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
        if (url.startsWith("http://")) {
            val host = runCatching { URL(url).host }.getOrNull() ?: return "That doesn't look like a web link"
            return if (isLocalHost(host)) null
            else "Plain http:// only works for computers on your own network. Use an https:// link."
        }
        if (!url.startsWith("https://")) return "That doesn't look like a web link"
        return null
    }

    /**
     * True for addresses on your own network: private IPv4 ranges, loopback, link-local,
     * and local hostnames (myserver, nas.local, pc.lan, pc.home).
     */
    fun isLocalHost(host: String): Boolean {
        val h = host.lowercase().trim('[', ']')
        if (h == "localhost" || h.endsWith(".local") || h.endsWith(".lan") || h.endsWith(".home") || h.endsWith(".internal")) return true
        val parts = h.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size == 4 && h.count { it == '.' } == 3) {
            val (a, b) = parts
            return a == 10 || a == 127 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
        }
        if (h.startsWith("fe80:") || h.startsWith("fd") || h == "::1") return true
        // A bare machine name (no dots) can only resolve on the local network.
        return !h.contains('.') && !h.contains(':')
    }

    /** The link points at a web page (most likely a folder listing), not a file. */
    class PageNotFileException(val url: String) : IOException("That link opens a web page, not a file. Use a direct download link.")

    data class FolderEntry(val name: String, val url: String)

    /**
     * Reads a directory listing (Apache, nginx, Caddy, `python -m http.server`, most NAS web
     * shares) and returns the game files in it.
     */
    suspend fun listFolder(rawUrl: String, extensions: Set<String>): List<FolderEntry> = withContext(Dispatchers.IO) {
        val base = URL(normalize(rawUrl))
        val conn = (base.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "RetroHero")
        }
        try {
            if (conn.responseCode != 200) throw IOException("Server returned HTTP ${conn.responseCode}")
            val html = conn.inputStream.use { input ->
                val bytes = input.readNBytesCompat(4 * 1024 * 1024)
                String(bytes, Charsets.UTF_8)
            }
            Regex("""href\s*=\s*["']([^"'#?]+)["']""", RegexOption.IGNORE_CASE).findAll(html)
                .mapNotNull { m ->
                    val target = runCatching { URL(conn.url, m.groupValues[1]) }.getOrNull() ?: return@mapNotNull null
                    if (target.host != conn.url.host) return@mapNotNull null
                    val name = URLDecoder.decode(target.path.substringAfterLast('/'), "UTF-8")
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (name.isBlank() || ext !in extensions) null else FolderEntry(name, target.toString())
                }
                .distinctBy { it.url }
                .sortedBy { it.name.lowercase() }
                .toList()
        } finally {
            conn.disconnect()
        }
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (out.size() < limit) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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
                    if (next.protocol != "https" && !(next.protocol == "http" && isLocalHost(next.host))) {
                        throw IOException("Link redirected to an insecure address")
                    }
                    url = next
                    if (++redirects > 8) throw IOException("Too many redirects")
                    continue
                }
                if (code != 200) throw IOException("Server returned HTTP $code")
                break
            }
            val type = conn.contentType.orEmpty()
            if (type.startsWith("text/html")) {
                val pageUrl = conn.url.toString()
                conn.disconnect()
                throw PageNotFileException(pageUrl)
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
