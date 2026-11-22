package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/** Scans the user's ROM folders and keeps the game list persisted as JSON. */
class GameLibrary(private val context: Context, private val paths: AppPaths) {

    private val _games = MutableStateFlow(load())
    val games: StateFlow<List<Game>> = _games

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    private val _scanProgress = MutableStateFlow("")
    val scanProgress: StateFlow<String> = _scanProgress

    fun find(id: String): Game? = _games.value.firstOrNull { it.id == id }

    private fun load(): List<Game> =
        if (paths.library.exists()) runCatching { Game.listFromJson(paths.library.readText()) }.getOrDefault(emptyList())
        else emptyList()

    suspend fun scan(folders: Collection<String>) = withContext(Dispatchers.IO) {
        if (_scanning.value) return@withContext
        _scanning.value = true
        try {
            val found = ArrayList<Game>()
            for (folder in folders) {
                val tree = Uri.parse(folder)
                runCatching {
                    scanDir(tree, Saf.rootDocId(tree), listOf(Saf.folderName(tree).lowercase()), 0, found)
                }.onFailure { Log.w(TAG, "Failed to scan $folder", it) }
            }
            val sorted = found.distinctBy { it.id }.sortedBy { it.title.lowercase() }
            paths.library.writeText(Game.listToJson(sorted))
            _games.value = sorted
        } finally {
            _scanProgress.value = ""
            _scanning.value = false
        }
    }

    private fun scanDir(tree: Uri, docId: String, ancestors: List<String>, depth: Int, out: MutableList<Game>) {
        if (depth > MAX_DEPTH) return
        _scanProgress.value = ancestors.first()
        val children = Saf.children(context.contentResolver, tree, docId)
        val names = children.filter { !it.isDir }.map { it.name.lowercase() }.toSet()
        val hasCue = names.any { it.endsWith(".cue") }
        val hasM3u = names.any { it.endsWith(".m3u") }

        for (doc in children) {
            if (doc.name.startsWith(".")) continue
            if (doc.isDir) {
                scanDir(tree, doc.docId, listOf(doc.name.lowercase()) + ancestors, depth + 1, out)
                continue
            }
            val ext = doc.name.substringAfterLast('.', "").lowercase()
            // Track files referenced by a .cue / .m3u are not separate games.
            if (hasCue && ext in setOf("bin", "img", "wav", "iso")) continue
            if (hasM3u && ext in setOf("cue", "chd", "ccd")) continue
            val system = classify(tree, doc, ext, ancestors) ?: continue
            out += Game(
                id = Saf.documentUri(tree, doc.docId).toString(),
                fileName = doc.name,
                systemId = system.id,
                treeUri = tree.toString(),
                parentDocId = docId,
                size = doc.size,
            )
        }
    }

    private fun classify(tree: Uri, doc: Saf.Doc, ext: String, ancestors: List<String>): GameSystem? {
        val candidates = Systems.all.filter { ext in it.extensions }
        if (candidates.size == 1) return candidates.first()

        // Folder names are the strongest hint ("/roms/psx/game.cue").
        for (folder in ancestors) {
            candidates.firstOrNull { folder in it.folderHints }?.let { return it }
        }

        if (ext == "zip") {
            // A zipped console game? Look at the first entry's extension.
            zippedSystem(tree, doc)?.let { return it }
            return Systems.get("arcade")
        }
        if (candidates.isEmpty()) return null
        return when (ext) {
            "bin" -> if (doc.size > 16L * 1024 * 1024) Systems.get("psx") else Systems.get("genesis")
            "cue", "chd", "iso", "img" -> Systems.get("psx")
            else -> candidates.first()
        }
    }

    private fun zippedSystem(tree: Uri, doc: Saf.Doc): GameSystem? = runCatching {
        context.contentResolver.openInputStream(Saf.documentUri(tree, doc.docId))?.use { input ->
            ZipInputStream(input).use { zip ->
                val entry = zip.nextEntry ?: return null
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                Systems.all.filter { !it.keepZipped && ext in it.extensions }.singleOrNull()
            }
        }
    }.getOrNull()

    private companion object {
        const val TAG = "GameLibrary"
        const val MAX_DEPTH = 6
    }
}
