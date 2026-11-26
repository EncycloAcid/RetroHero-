package com.retrohero.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Installs libretro cores. Cores are resolved in this order:
 *  1. bundled with the APK (src/main/jniLibs/<abi>/lib<core>_libretro_android.so)
 *  2. downloaded from the libretro buildbot into app storage
 */
class CoreManager(private val context: Context, private val paths: AppPaths) {

    sealed interface CoreState {
        data object NotInstalled : CoreState
        data class Downloading(val progress: Float) : CoreState
        data class Installed(val bundled: Boolean, val sizeBytes: Long) : CoreState
        data class Failed(val message: String) : CoreState
    }

    private val _states = MutableStateFlow<Map<String, CoreState>>(emptyMap())
    val states: StateFlow<Map<String, CoreState>> = _states

    init {
        refresh()
    }

    fun refresh() {
        _states.value = Systems.all.flatMap { it.cores }.distinctBy { it.id }.associate { core ->
            core.id to installedState(core.id)
        }
    }

    private fun installedState(coreId: String): CoreState {
        bundledFile(coreId)?.let { return CoreState.Installed(true, it.length()) }
        val f = downloadedFile(coreId)
        return if (f.exists()) CoreState.Installed(false, f.length()) else CoreState.NotInstalled
    }

    fun coreFile(coreId: String): File? = bundledFile(coreId) ?: downloadedFile(coreId).takeIf { it.exists() }

    fun isInstalled(coreId: String) = coreFile(coreId) != null

    private fun bundledFile(coreId: String): File? =
        File(context.applicationInfo.nativeLibraryDir, "lib${coreId}_libretro_android.so").takeIf { it.exists() }

    private fun downloadedFile(coreId: String) = File(paths.cores, "${coreId}_libretro_android.so")

    fun delete(coreId: String) {
        downloadedFile(coreId).delete()
        refresh()
    }

    /** Downloads and installs a core. Returns null on success or an error message. */
    suspend fun download(coreId: String): String? = withContext(Dispatchers.IO) {
        setState(coreId, CoreState.Downloading(0f))
        val target = downloadedFile(coreId)
        val tmp = File(paths.cores, "$coreId.download")
        try {
            val archive = Systems.coreById(coreId)?.archiveFileName() ?: "${coreId}_libretro_android.so.zip"
            val url = URL("$BUILDBOT/${abi()}/$archive")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "RetroHero")
            }
            if (conn.responseCode != 200) error("Server returned HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 }
            var read = 0L
            ZipInputStream(conn.inputStream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && !entry.name.endsWith(".so")) entry = zip.nextEntry
                if (entry == null) error("Archive did not contain a core")
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var lastReport = 0L
                    while (true) {
                        val n = zip.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (read - lastReport > 256 * 1024) {
                            lastReport = read
                            // Uncompressed bytes vs compressed length: an estimate, capped below 100%.
                            val p = total?.let { (read.toFloat() / (it * 2.2f)).coerceAtMost(0.97f) } ?: 0f
                            setState(coreId, CoreState.Downloading(p))
                        }
                    }
                }
            }
            if (!tmp.renameTo(target)) error("Could not install core")
            setState(coreId, installedState(coreId))
            null
        } catch (e: Exception) {
            tmp.delete()
            val msg = e.message ?: e.javaClass.simpleName
            setState(coreId, CoreState.Failed(msg))
            msg
        }
    }

    private fun setState(coreId: String, state: CoreState) = _states.update { it + (coreId to state) }

    companion object {
        private const val BUILDBOT = "https://buildbot.libretro.com/nightly/android/latest"

        /** The ABI of this (possibly 32-bit) process, which downloaded cores must match. */
        fun abi(): String = when (System.getProperty("os.arch")) {
            "aarch64", "arm64" -> "arm64-v8a"
            "x86_64", "amd64" -> "x86_64"
            "i686", "x86" -> "x86"
            else -> "armeabi-v7a"
        }
    }
}
