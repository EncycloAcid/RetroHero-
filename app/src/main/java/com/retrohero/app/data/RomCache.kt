package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * libretro cores open games by file path, but user ROMs live behind the Storage Access
 * Framework. This copies a game (and any files it references, e.g. .cue -> .bin) into a
 * private cache directory, extracting single-game zips when the core can't read them.
 */
class RomCache(private val context: Context, private val paths: AppPaths) {

    fun prepare(game: Game, onProgress: (String) -> Unit = {}): File {
        val system = game.system ?: throw IOException("Unknown system")
        val dir = File(paths.romCache, hash(game.id)).apply { mkdirs() }
        dir.setLastModified(System.currentTimeMillis())
        val tree = Uri.parse(game.treeUri)

        val result = if (game.extension == "zip" && !system.keepZipped) {
            extractZip(game, system, dir)
        } else {
            copyWithReferences(tree, game.parentDocId, game.fileName, dir, onProgress, depth = 0)
            File(dir, game.fileName)
        }

        // Arcade emulators look for BIOS sets like neogeo.zip next to the game.
        if (system.keepZipped) {
            paths.system.listFiles { f -> f.name.endsWith(".zip", true) }?.forEach { bios ->
                val dest = File(dir, bios.name)
                if (!dest.exists() || dest.length() != bios.length()) bios.copyTo(dest, overwrite = true)
            }
        }
        trim(keep = dir)
        return result
    }

    private fun copyWithReferences(
        tree: Uri, parentDocId: String, fileName: String, dir: File,
        onProgress: (String) -> Unit, depth: Int,
    ) {
        val siblings = Saf.children(context.contentResolver, tree, parentDocId)
        val doc = siblings.firstOrNull { it.name == fileName }
            ?: siblings.firstOrNull { it.name.equals(fileName, ignoreCase = true) }
            ?: throw IOException("Missing file: $fileName")
        val dest = File(dir, doc.name)
        if (!dest.exists() || dest.length() != doc.size) {
            onProgress(doc.name)
            val tmp = File(dir, doc.name + ".part")
            context.contentResolver.openInputStream(Saf.documentUri(tree, doc.docId))?.use { input ->
                tmp.outputStream().use { input.copyTo(it, 256 * 1024) }
            } ?: throw IOException("Could not open ${doc.name}")
            if (!tmp.renameTo(dest)) throw IOException("Could not cache ${doc.name}")
        }
        if (depth >= 2) return
        val ext = doc.name.substringAfterLast('.', "").lowercase()
        val refs = when (ext) {
            "cue" -> CUE_FILE.findAll(dest.readText()).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.toList()
            "m3u" -> dest.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            else -> emptyList()
        }
        for (ref in refs) {
            val name = ref.substringAfterLast('/').substringAfterLast('\\')
            copyWithReferences(tree, parentDocId, name, dir, onProgress, depth + 1)
        }
    }

    private fun extractZip(game: Game, system: GameSystem, dir: File): File {
        val uri = Uri.parse(game.id)
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/')
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && ext in system.extensions) {
                        val dest = File(dir, name)
                        if (dest.exists() && dest.length() == entry.size) return dest
                        dest.outputStream().use { zip.copyTo(it) }
                        return dest
                    }
                    entry = zip.nextEntry
                }
            }
        }
        throw IOException("No ${system.shortName} game found inside ${game.fileName}")
    }

    /** Keeps the cache under [MAX_CACHE_BYTES], evicting least recently played games first. */
    private fun trim(keep: File) {
        val dirs = paths.romCache.listFiles()?.filter { it.isDirectory } ?: return
        var total = dirs.sumOf { d -> d.walk().filter { it.isFile }.sumOf { it.length() } }
        for (d in dirs.sortedBy { it.lastModified() }) {
            if (total <= MAX_CACHE_BYTES) break
            if (d == keep) continue
            total -= d.walk().filter { it.isFile }.sumOf { it.length() }
            d.deleteRecursively()
        }
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)

    private companion object {
        const val MAX_CACHE_BYTES = 2L * 1024 * 1024 * 1024
        val CUE_FILE = Regex("""FILE\s+(?:"([^"]+)"|(\S+))""", RegexOption.IGNORE_CASE)
    }
}
