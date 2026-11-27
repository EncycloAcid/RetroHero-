package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Scans the user's ROM folders (via the Storage Access Framework) plus the app's own
 * Downloads folder, and keeps the game list persisted as JSON.
 */
class GameLibrary(private val context: Context, private val paths: AppPaths) {

    /** Games the player removed from their folders' listing. Their files are left alone. */
    private val hiddenFile = File(paths.library.parentFile, "hidden_games.json")
    private val hidden: MutableSet<String> = loadHidden()

    private val _games = MutableStateFlow(load())
    val games: StateFlow<List<Game>> = _games

    private val _hiddenCount = MutableStateFlow(hidden.size)
    val hiddenCount: StateFlow<Int> = _hiddenCount

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    private val _scanProgress = MutableStateFlow("")
    val scanProgress: StateFlow<String> = _scanProgress

    fun find(id: String): Game? = _games.value.firstOrNull { it.id == id }

    private fun load(): List<Game> =
        if (paths.library.exists()) {
            runCatching { MultiDisc.collapseMultiDiscGames(Game.listFromJson(paths.library.readText())) }
                .getOrDefault(emptyList())
        } else emptyList()

    private fun loadHidden(): MutableSet<String> = runCatching {
        val arr = org.json.JSONArray(hiddenFile.readText())
        (0 until arr.length()).map { arr.getString(it) }.toMutableSet()
    }.getOrDefault(mutableSetOf())

    private fun saveHidden() {
        hiddenFile.writeText(org.json.JSONArray(hidden.toList()).toString())
        _hiddenCount.value = hidden.size
    }

    private fun publish(list: List<Game>) {
        val grouped = MultiDisc.collapseMultiDiscGames(list).filter { it.id !in hidden }
        val sorted = grouped.distinctBy { it.id }.sortedBy { it.title.lowercase() }
        paths.library.writeText(Game.listToJson(sorted))
        _games.value = sorted
    }

    suspend fun scan(folders: Collection<String>) = withContext(Dispatchers.IO) {
        if (_scanning.value) return@withContext
        _scanning.value = true
        try {
            val found = ArrayList<Game>()
            for (folder in folders) {
                val tree = Uri.parse(folder)
                runCatching {
                    scanTree(tree, Saf.rootDocId(tree), listOf(Saf.folderName(tree).lowercase()), 0, found)
                }.onFailure { Log.w(TAG, "Failed to scan $folder", it) }
            }
            scanLocal(paths.downloads, emptyList(), 0, found)
            publish(found)
        } finally {
            _scanProgress.value = ""
            _scanning.value = false
        }
    }

    /** Adds one freshly downloaded file. Returns the game, or null if no system recognises it. */
    suspend fun addDownloaded(file: File): Game? = withContext(Dispatchers.IO) {
        val system = classify(file.name, file.length(), emptyList()) { file.inputStream() } ?: return@withContext null
        val game = localGame(file, system)
        publish(_games.value.filter { it.id != game.id } + game)
        _games.value.firstOrNull { it.id == game.id || it.discs.any { d -> d.id == game.id } } ?: game
    }

    /**
     * Takes [game] out of the library. A game from the player's own folders is hidden (its file
     * stays, and rescans skip it) because RetroHero only has read access there; a game downloaded
     * into the app is deleted from disk.
     */
    suspend fun remove(game: Game) = withContext(Dispatchers.IO) {
        if (game.isLocal) {
            deleteLocal(game)
        } else {
            hidden += game.id
            saveHidden()
            publish(_games.value)
        }
    }

    /** Undoes [remove] for a hidden folder game. */
    suspend fun unhide(game: Game) = withContext(Dispatchers.IO) {
        if (hidden.remove(game.id)) {
            saveHidden()
            publish(_games.value + game)
        }
    }

    /** Brings back every hidden game; they reappear on the next scan. */
    fun unhideAll() {
        hidden.clear()
        saveHidden()
    }

    private fun deleteLocal(game: Game) {
        if (!game.isLocal) return
        if (game.isMultiDisc) {
            game.discs.forEach { d -> Uri.parse(d.id).path?.let { File(it).delete() } }
        } else {
            File(Uri.parse(game.id).path ?: return).delete()
        }
        publish(_games.value.filter { it.id != game.id })
    }

    private fun localGame(file: File, system: GameSystem) = Game(
        id = Uri.fromFile(file).toString(),
        fileName = file.name,
        systemId = system.id,
        treeUri = Game.LOCAL,
        parentDocId = file.parentFile?.absolutePath.orEmpty(),
        size = file.length(),
    )

    private fun scanLocal(dir: File, ancestors: List<String>, depth: Int, out: MutableList<Game>) {
        if (depth > MAX_DEPTH) return
        val children = dir.listFiles() ?: return
        val names = children.filter { it.isFile }.map { it.name.lowercase() }.toSet()
        for (f in children) {
            if (f.name.startsWith(".") || f.name.endsWith(".part")) continue
            if (f.isDirectory) {
                scanLocal(f, listOf(f.name.lowercase()) + ancestors, depth + 1, out)
                continue
            }
            if (isTrackFile(f.name, names)) continue
            val system = classify(f.name, f.length(), ancestors) { f.inputStream() } ?: continue
            out += localGame(f, system)
        }
    }

    private fun scanTree(tree: Uri, docId: String, ancestors: List<String>, depth: Int, out: MutableList<Game>) {
        if (depth > MAX_DEPTH) return
        _scanProgress.value = ancestors.first()
        val children = Saf.children(context.contentResolver, tree, docId)
        val names = children.filter { !it.isDir }.map { it.name.lowercase() }.toSet()
        for (doc in children) {
            if (doc.name.startsWith(".")) continue
            if (doc.isDir) {
                scanTree(tree, doc.docId, listOf(doc.name.lowercase()) + ancestors, depth + 1, out)
                continue
            }
            if (isTrackFile(doc.name, names)) continue
            val system = classify(doc.name, doc.size, ancestors) {
                context.contentResolver.openInputStream(Saf.documentUri(tree, doc.docId))
            } ?: continue
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

    /** Track files referenced by a .cue / .m3u in the same folder are not separate games. */
    private fun isTrackFile(name: String, siblings: Set<String>): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (siblings.any { it.endsWith(".cue") } && ext in setOf("bin", "img", "wav", "iso")) return true
        if (siblings.any { it.endsWith(".m3u") } && ext in setOf("cue", "chd", "ccd")) return true
        return false
    }

    private fun classify(name: String, size: Long, ancestors: List<String>, open: () -> InputStream?): GameSystem? {
        val ext = name.substringAfterLast('.', "").lowercase()
        val candidates = Systems.all.filter { ext in it.extensions }
        if (candidates.size == 1) return candidates.first()

        // Folder names are the strongest hint ("/roms/psx/game.cue").
        for (folder in ancestors) {
            candidates.firstOrNull { folder in it.folderHints }?.let { return it }
        }
        if (ext == "zip") {
            // A zipped console game? Look at the first entry's extension.
            zippedSystem(open)?.let { return it }
            return Systems.get("arcade")
        }
        if (candidates.isEmpty()) return null
        return when (ext) {
            "m3u" -> Systems.get("psx")
            "bin" -> if (size > 16L * 1024 * 1024) Systems.get("psx") else Systems.get("genesis")
            "cue", "chd", "iso", "img" -> Systems.get("psx")
            else -> candidates.first()
        }
    }

    private fun zippedSystem(open: () -> InputStream?): GameSystem? = runCatching {
        open()?.use { input ->
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
