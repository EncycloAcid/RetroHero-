package com.retrohero.app.data

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local crash reporting. Nothing leaves the device: reports are plain text files in
 * `files/crashes/` that the player can share from Settings.
 *
 * Three sources are covered:
 *  - Kotlin/Java crashes in any process (main UI and the `:game` emulator process), caught by a
 *    default uncaught-exception handler that writes the report before the process dies.
 *  - Native crashes (a core segfaulting) and ANRs, which no in-process handler can catch reliably.
 *    On Android 11+ these are read back from [ActivityManager.getHistoricalProcessExitReasons]
 *    the next time the main process starts.
 *  - A breadcrumb with the game and core that were running, so a report from the `:game` process
 *    says what was being played.
 */
object CrashReporter {
    private const val DIR = "crashes"
    private const val BREADCRUMB = "last_session.txt"
    private const val PREFS = "crash_reporter"
    private const val KEY_LAST_EXIT_SEEN = "last_exit_seen"
    private const val KEY_LAST_ACK = "last_ack"
    const val MAX_REPORTS = 20

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    /** Call from Application.onCreate in every process. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeReport(app, kind = "crash", body = describeThrowable(thread, error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Records what is about to run in the `:game` process. Shown in that process's reports. */
    fun setBreadcrumb(context: Context, text: String) {
        runCatching { File(dir(context), BREADCRUMB).writeText(text) }
    }

    fun clearBreadcrumb(context: Context) {
        runCatching { File(dir(context), BREADCRUMB).delete() }
    }

    /**
     * Turns native crashes and ANRs recorded by the system since the last check into reports.
     * Java crashes are skipped here because the uncaught handler already wrote them. Call off the
     * main thread from the main process only.
     */
    fun collectSystemExits(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong(KEY_LAST_EXIT_SEEN, -1L)
        val am = context.getSystemService(ActivityManager::class.java) ?: return
        val exits = runCatching { am.getHistoricalProcessExitReasons(context.packageName, 0, 16) }
            .getOrNull().orEmpty()
        if (lastSeen < 0L) {
            // First run with the reporter: don't flood the list with exits from older versions.
            prefs.edit().putLong(KEY_LAST_EXIT_SEEN, exits.maxOfOrNull { it.timestamp } ?: 0L).apply()
            return
        }
        var newest = lastSeen
        for (exit in exits.sortedBy { it.timestamp }) {
            if (exit.timestamp <= lastSeen) continue
            newest = maxOf(newest, exit.timestamp)
            val kind = when (exit.reason) {
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "native-crash"
                ApplicationExitInfo.REASON_ANR -> "anr"
                else -> null
            } ?: continue
            val body = buildString {
                appendLine("Process: ${exit.processName}")
                appendLine("Reason: $kind (${exit.description ?: "no description"})")
                appendLine("Status/signal: ${exit.status}")
                appendLine("Occurred: ${formatTime(exit.timestamp)}")
                appendLine("Memory at exit: pss=${exit.pss}kB rss=${exit.rss}kB")
                if (exit.processName.endsWith(":game")) readBreadcrumb(context)?.let {
                    appendLine()
                    appendLine("Last game session:")
                    appendLine(it)
                }
                traceExcerpt(exit)?.let {
                    appendLine()
                    appendLine("System trace (excerpt):")
                    appendLine(it)
                }
            }
            writeReport(context, kind, body, includeBreadcrumb = false, timestamp = exit.timestamp)
        }
        if (newest != lastSeen) prefs.edit().putLong(KEY_LAST_EXIT_SEEN, newest).apply()
    }

    fun reports(context: Context): List<File> =
        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".txt") && f.name != BREADCRUMB }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    /** Reports written since the player last opened or dismissed them. */
    fun unseenReports(context: Context): List<File> {
        val ack = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_ACK, 0L)
        return reports(context).filter { it.lastModified() > ack }
    }

    fun acknowledge(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_ACK, System.currentTimeMillis()).apply()
    }

    fun clear(context: Context) {
        reports(context).forEach { it.delete() }
        acknowledge(context)
    }

    /** All reports joined newest-first into one shareable file under the FileProvider path. */
    fun bundleForSharing(context: Context): File? {
        val all = reports(context)
        if (all.isEmpty()) return null
        val out = File(dir(context), "share/retrohero-crash-reports.txt").apply { parentFile?.mkdirs() }
        out.writeText(all.joinToString("\n\n" + "=".repeat(60) + "\n\n") { it.readText() })
        return out
    }

    internal fun writeReport(
        context: Context,
        kind: String,
        body: String,
        includeBreadcrumb: Boolean = true,
        timestamp: Long = System.currentTimeMillis(),
    ): File {
        val dir = dir(context)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(timestamp))
        val file = File(dir, "$stamp-$kind-${processSuffix()}.txt")
        val text = buildString {
            appendLine("RetroHero crash report")
            appendLine(header(context))
            appendLine()
            append(body.trimEnd())
            appendLine()
            if (includeBreadcrumb && processSuffix() == "game") readBreadcrumb(context)?.let {
                appendLine()
                appendLine("Game session:")
                appendLine(it)
            }
        }
        file.writeText(text)
        file.setLastModified(timestamp)
        prune(context)
        return file
    }

    internal fun describeThrowable(thread: Thread, error: Throwable): String {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        return "Thread: ${thread.name}\n\n$sw"
    }

    private fun prune(context: Context) {
        reports(context).drop(MAX_REPORTS).forEach { it.delete() }
    }

    private fun header(context: Context): String {
        val version = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("unknown")
        return buildString {
            appendLine("App: $version")
            appendLine("Process: ${currentProcessName()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            append("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        }
    }

    private fun readBreadcrumb(context: Context): String? =
        runCatching { File(dir(context), BREADCRUMB).takeIf { it.exists() }?.readText() }.getOrNull()

    private fun traceExcerpt(exit: ApplicationExitInfo): String? {
        // ANR traces are text. Native tombstones (API 31+) are protobuf, so only keep printable runs.
        val bytes = runCatching { exit.traceInputStream?.use { it.readNBytes(64 * 1024) } }.getOrNull()
            ?: return null
        val text = if (exit.reason == ApplicationExitInfo.REASON_ANR) {
            String(bytes)
        } else {
            Regex("[\\x20-\\x7E]{6,}").findAll(String(bytes, Charsets.ISO_8859_1))
                .joinToString("\n") { it.value }
        }
        return text.lineSequence().take(120).joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun currentProcessName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) android.app.Application.getProcessName()
        else runCatching { File("/proc/self/cmdline").readText().trim('\u0000', ' ') }.getOrDefault("?")

    private fun processSuffix(): String =
        if (currentProcessName().endsWith(":game")) "game" else "app"

    private fun formatTime(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(ms))
}
