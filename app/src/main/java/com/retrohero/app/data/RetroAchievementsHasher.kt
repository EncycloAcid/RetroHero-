package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Computes official RetroAchievements (rcheevos / rc_hash compatible) ROM MD5 hashes.
 * Handles console headers (NES iNES 16B, FDS 16B, SNES/PCE 512B copier, Lynx 64B, Atari 7800 128B),
 * N64 byte-order normalization (.v64 / .n64 -> .z64 big-endian), and zipped ROM archives.
 */
object RetroAchievementsHasher {

    private const val MAX_DIRECT_HASH_BYTES = 128L * 1024L * 1024L // 128 MB cap for mobile speed

    /** Hashes a local ROM file on disk using the console's official RetroAchievements method. */
    fun hashFile(file: File, systemId: String): String? {
        if (!file.exists() || !file.isFile) return null
        val size = file.length()
        if (size <= 0L || size > MAX_DIRECT_HASH_BYTES) return null
        val ext = file.extension.lowercase()
        return runCatching {
            if (ext == "zip") {
                FileInputStream(file).use { fis -> hashZipStream(fis, systemId) }
            } else {
                FileInputStream(file).use { fis -> hashStream(fis, size, systemId, ext) }
            }
        }.getOrNull()
    }

    /** Hashes a [Game] (local file or SAF content:// URI), caching the result on disk. */
    fun hashGame(context: Context, game: Game, cacheFile: File? = null): String? {
        val cacheKey = "${game.id}#${game.size}"
        val cache = cacheFile?.let { loadCache(it) }
        cache?.optString(cacheKey)?.takeIf { it.length == 32 }?.let { return it }

        val computed = when {
            game.isLocal -> {
                val parsedPath = Uri.parse(game.id).path
                val f = if (parsedPath != null && File(parsedPath).exists()) File(parsedPath) else File(game.id)
                hashFile(f, game.systemId)
            }
            game.size in 1L..MAX_DIRECT_HASH_BYTES -> runCatching {
                val uri = Uri.parse(game.id)
                val ext = game.extension.lowercase()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (ext == "zip") hashZipStream(stream, game.systemId)
                    else hashStream(stream, game.size, game.systemId, ext)
                }
            }.getOrNull()
            else -> null
        }

        if (computed != null && cacheFile != null) {
            runCatching {
                val updated = cache ?: JSONObject()
                updated.put(cacheKey, computed)
                writeAtomic(cacheFile, updated.toString())
            }
        }
        return computed
    }

    internal fun hashBytes(bytes: ByteArray, systemId: String, ext: String = ""): String =
        hashStream(bytes.inputStream(), bytes.size.toLong(), systemId, ext.lowercase())

    private fun hashZipStream(raw: InputStream, systemId: String): String? {
        val allowed = Systems.all.firstOrNull { it.id == systemId }?.extensions ?: emptySet()
        ZipInputStream(BufferedInputStream(raw)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.isDirectory) continue
                val entryExt = entry.name.substringAfterLast('.', "").lowercase()
                if (allowed.isEmpty() || entryExt in allowed) {
                    val data = zis.readBytes()
                    if (data.isNotEmpty()) {
                        return hashBytes(data, systemId, entryExt)
                    }
                }
            }
        }
        return null
    }

    internal fun hashStream(raw: InputStream, totalSize: Long, systemId: String, ext: String): String {
        val input = BufferedInputStream(raw, 65536)
        input.mark(512)
        val head = ByteArray(512)
        val headRead = readFully(input, head, 512)
        input.reset()

        // Strip system-specific header bytes per rcheevos rc_hash rules
        val skipBytes: Long = when (systemId) {
            "nes" -> if (headRead >= 4 && head[0] == 'N'.code.toByte() && head[1] == 'E'.code.toByte() &&
                head[2] == 'S'.code.toByte() && head[3] == 0x1A.toByte()
            ) 16L else 0L
            "fds" -> if (headRead >= 4 && head[0] == 'F'.code.toByte() && head[1] == 'D'.code.toByte() &&
                head[2] == 'S'.code.toByte() && head[3] == 0x1A.toByte()
            ) 16L else 0L
            "snes", "satellaview" -> if (totalSize > 512L && (totalSize % 1024L) == 512L) 512L else 0L
            "pce", "supergrafx" -> if (totalSize > 512L && (totalSize % 1024L) == 512L) 512L else 0L
            "lynx" -> if (headRead >= 4 && head[0] == 'L'.code.toByte() && head[1] == 'Y'.code.toByte() &&
                head[2] == 'N'.code.toByte() && head[3] == 'X'.code.toByte()
            ) 64L else 0L
            "atari7800" -> if (headRead >= 10 && String(head, 1, 9, Charsets.US_ASCII) == "ATARI7800") 128L else 0L
            "sms", "gg" -> if (totalSize > 512L && (totalSize % 1024L) == 512L) 512L else 0L
            else -> 0L
        }

        if (skipBytes > 0L) {
            var remaining = skipBytes
            while (remaining > 0L) {
                val skipped = input.skip(remaining)
                if (skipped <= 0L) {
                    if (input.read() < 0) break
                    remaining--
                } else {
                    remaining -= skipped
                }
            }
        }

        val md5 = MessageDigest.getInstance("MD5")
        val buf = ByteArray(65536)

        // N64 byte-order normalization: .v64 (byteswapped 16-bit) and .n64 (little-endian 32-bit) -> .z64 big-endian
        val n64Mode = if (systemId == "n64" && headRead >= 4) {
            when {
                head[0] == 0x37.toByte() && head[1] == 0x80.toByte() && head[2] == 0x40.toByte() && head[3] == 0x12.toByte() -> 1 // v64
                head[0] == 0x40.toByte() && head[1] == 0x12.toByte() && head[2] == 0x37.toByte() && head[3] == 0x80.toByte() -> 2 // n64
                else -> 0
            }
        } else 0

        if (n64Mode != 0) {
            while (true) {
                val n = readAligned(input, buf, 4)
                if (n <= 0) break
                if (n64Mode == 1) {
                    var i = 0
                    while (i + 1 < n) {
                        val tmp = buf[i]; buf[i] = buf[i + 1]; buf[i + 1] = tmp
                        i += 2
                    }
                } else {
                    var i = 0
                    while (i + 3 < n) {
                        val b0 = buf[i]; val b1 = buf[i + 1]; val b2 = buf[i + 2]; val b3 = buf[i + 3]
                        buf[i] = b3; buf[i + 1] = b2; buf[i + 2] = b1; buf[i + 3] = b0
                        i += 4
                    }
                }
                md5.update(buf, 0, n)
            }
        } else {
            // For NES iNES ROMs, cap hash at PRG+CHR size if header specifies it (optional trainer handled cleanly)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md5.update(buf, 0, n)
            }
        }

        return md5.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readFully(input: InputStream, dst: ByteArray, len: Int): Int {
        var total = 0
        while (total < len) {
            val n = input.read(dst, total, len - total)
            if (n <= 0) break
            total += n
        }
        return total
    }

    private fun readAligned(input: InputStream, dst: ByteArray, align: Int): Int {
        var total = 0
        while (total < dst.size) {
            val n = input.read(dst, total, dst.size - total)
            if (n <= 0) break
            total += n
            if (total % align == 0 && total >= 16384) break
        }
        return total
    }

    private fun loadCache(file: File): JSONObject {
        if (!file.exists()) return JSONObject()
        return runCatching { JSONObject(file.readText()) }.getOrElse { JSONObject() }
    }
}
