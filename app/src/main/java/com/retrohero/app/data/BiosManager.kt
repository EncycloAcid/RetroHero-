package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Installs and verifies BIOS files. Files are identified by MD5 checksum against known good
 * dumps, so they are saved under the exact name each core expects regardless of how the
 * downloaded or picked file was named.
 */
class BiosManager(private val context: Context, private val paths: AppPaths) {

    enum class Status { Missing, Verified, Unverified }

    data class InstallResult(val name: String, val verified: Boolean)

    private val known: List<BiosFile> = Systems.all.flatMap { it.bios }
    private val byMd5: Map<String, BiosFile> = known.filter { it.md5 != null }.associateBy { it.md5!! }

    fun status(file: BiosFile): Status {
        val f = File(paths.system, file.fileName)
        if (!f.exists()) return Status.Missing
        if (file.md5 == null) return Status.Verified
        return if (md5(f) == file.md5) Status.Verified else Status.Unverified
    }

    fun isPresent(file: BiosFile): Boolean = File(paths.system, file.fileName).exists()

    /** Copies picked files into the system directory. */
    fun import(uris: List<Uri>): List<InstallResult> = uris.flatMap { uri ->
        val name = displayName(uri) ?: return@flatMap emptyList()
        val tmp = File(context.cacheDir, "bios_import_${System.nanoTime()}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                ?: return@flatMap emptyList()
            install(tmp, name)
        } finally {
            tmp.delete()
        }
    }

    /**
     * Installs a downloaded or imported file. Zips that aren't arcade BIOS sets are unpacked
     * and every recognised BIOS inside is installed.
     */
    fun install(file: File, originalName: String): List<InstallResult> {
        val isArcadeSet = known.any { it.fileName.equals(originalName, ignoreCase = true) && it.fileName.endsWith(".zip") }
        if (originalName.endsWith(".zip", ignoreCase = true) && !isArcadeSet) {
            val results = ArrayList<InstallResult>()
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name.substringAfterLast('/')
                        val tmp = File(context.cacheDir, "bios_entry_${System.nanoTime()}")
                        try {
                            tmp.outputStream().use { zip.copyTo(it) }
                            identify(tmp, entryName)?.let { target ->
                                results += place(tmp, target)
                            }
                        } finally {
                            tmp.delete()
                        }
                    }
                    entry = zip.nextEntry
                }
            }
            return results
        }
        val target = identify(file, originalName) ?: BiosFile(sanitize(originalName), "", false)
        return listOf(place(file, target))
    }

    /** Matches by checksum first, then by file name. */
    private fun identify(file: File, name: String): BiosFile? =
        byMd5[md5(file)] ?: known.firstOrNull { it.fileName.equals(name, ignoreCase = true) }

    private fun place(src: File, target: BiosFile): InstallResult {
        val dest = File(paths.system, target.fileName)
        src.copyTo(dest, overwrite = true)
        // FinalBurn Neo also looks for BIOS romsets (e.g. neogeo.zip) in system/fbneo.
        if (target.fileName.endsWith(".zip", ignoreCase = true)) {
            src.copyTo(File(File(paths.system, "fbneo").apply { mkdirs() }, target.fileName), overwrite = true)
        }
        val verified = target.md5 != null && md5(dest) == target.md5
        return InstallResult(target.fileName, verified)
    }

    fun delete(name: String) {
        File(paths.system, name).delete()
        File(File(paths.system, "fbneo"), name).delete()
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun sanitize(name: String) = name.replace(Regex("[\\\\/]"), "_")

    companion object {
        fun md5(file: File): String = file.inputStream().use { md5(it) }

        fun md5(input: InputStream): String {
            val digest = MessageDigest.getInstance("MD5")
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
