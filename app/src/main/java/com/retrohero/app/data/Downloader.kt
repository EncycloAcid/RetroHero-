package com.retrohero.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/**
 * Downloads a file from a user-supplied link (your own cloud backup, server, or a homebrew
 * site). Common share links are rewritten to their direct-download form.
 */
object Downloader {

    /** Turns Dropbox / Google Drive share links into direct links. */
    fun normalize(raw: String): String {
        val url = raw.trim()
        Regex("""drive\.google\.com/file/d/([^/?#]+)""").find(url)?.let {
            return "https://drive.google.com/uc?export=download&id=${it.groupValues[1]}"
        }
        Regex("""drive\.google\.com/open\?id=([^&#]+)""").find(url)?.let {
            return "https://drive.google.com/uc?export=download&id=${it.groupValues[1]}"
        }
        if ("dropbox.com" in url) {
            return when {
                Regex("""[?&]dl=0""").containsMatchIn(url) -> url.replace(Regex("""([?&])dl=0"""), "$1dl=1")
                "dl=1" in url -> url
                "?" in url -> "$url&dl=1"
                else -> "$url?dl=1"
            }
        }
        return url
    }

    fun validate(raw: String): String? {
        val url = raw.trim()
        if (url.isEmpty()) return "Paste a link first"
        if (url.startsWith("http://")) return "Only secure https:// links are supported"
        if (!url.startsWith("https://")) return "That doesn't look like a web link"
        return null
    }

    /**
     * Downloads [rawUrl] into [destDir]. Returns the downloaded file, named from the server's
     * Content-Disposition header or the URL path.
     */
    suspend fun fetch(rawUrl: String, destDir: File, onProgress: (read: Long, total: Long?) -> Unit): File =
        withContext(Dispatchers.IO) {
            destDir.mkdirs()
            var url = URL(normalize(rawUrl))
            var conn: HttpURLConnection
            var redirects = 0
            while (true) {
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "RetroHero")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location") ?: throw IOException("Broken redirect")
                    conn.disconnect()
                    val next = URL(url, location)
                    if (url.protocol == "https" && next.protocol != "https") throw IOException("Link redirected to an insecure address")
                    url = next
                    if (++redirects > 8) throw IOException("Too many redirects")
                    continue
                }
                if (code != 200) throw IOException("Server returned HTTP $code")
                break
            }
            val type = conn.contentType.orEmpty()
            if (type.startsWith("text/html")) {
                conn.disconnect()
                throw IOException("That link opens a web page, not a file. Use a direct download link.")
            }
            val name = sanitize(fileNameFrom(conn.getHeaderField("Content-Disposition")) ?: fileNameFrom(url))
            val total = conn.contentLengthLong.takeIf { it > 0 }
            val part = File(destDir, "$name.part")
            try {
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(128 * 1024)
                        var read = 0L
                        var last = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (read - last > 128 * 1024) {
                                last = read
                                onProgress(read, total)
                            }
                        }
                        onProgress(read, total)
                    }
                }
                val dest = File(destDir, name)
                dest.delete()
                if (!part.renameTo(dest)) throw IOException("Couldn't save $name")
                dest
            } catch (e: Exception) {
                part.delete()
                throw e
            } finally {
                conn.disconnect()
            }
        }

    private fun fileNameFrom(disposition: String?): String? {
        if (disposition == null) return null
        Regex("""filename\*=UTF-8''([^;]+)""", RegexOption.IGNORE_CASE).find(disposition)?.let {
            return URLDecoder.decode(it.groupValues[1], "UTF-8")
        }
        return Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.get(1)
    }

    private fun fileNameFrom(url: URL): String =
        URLDecoder.decode(url.path.substringAfterLast('/'), "UTF-8").ifBlank { "download.bin" }

    private fun sanitize(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_").trim().trimStart('.').ifEmpty { "download.bin" }
}
