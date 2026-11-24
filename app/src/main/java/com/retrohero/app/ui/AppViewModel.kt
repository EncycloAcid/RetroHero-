package com.retrohero.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retrohero.app.RetroHeroApp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.GameCollection
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.emulator.EmulatorActivity
import com.retrohero.app.data.Downloader
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.data.Systems
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class Shot(val game: Game, val file: File)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RetroHeroApp
    val settings = app.settings
    val bios = app.bios

    val games = app.library.games
    val scanning = app.library.scanning
    val scanProgress = app.library.scanProgress
    val coreStates = app.cores.states
    val folders = settings.romFolders
    val dynamicColor = settings.dynamicColor

    val recents: StateFlow<List<Game>> = combine(games, settings.recents) { all, ids ->
        val byId = all.associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteIds = settings.favorites
    val shelfStatuses = settings.shelfStatuses
    fun setShelfStatus(game: Game, status: String) = settings.setShelfStatus(game.id, status)

    val favorites: StateFlow<List<Game>> = combine(games, settings.favorites) { all, ids ->
        all.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** A short message, optionally with Undo. */
    class Message(val text: String, val undo: (() -> Unit)? = null)

    private val _messages = Channel<Message>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()
    fun say(text: String, undo: (() -> Unit)? = null) { _messages.trySend(Message(text, undo)) }

    fun toggleFavorite(game: Game): Boolean {
        val pinned = settings.toggleFavorite(game.id)
        say(if (pinned) "Pinned ${game.title}" else "Unpinned ${game.title}") { settings.toggleFavorite(game.id) }
        return pinned
    }

    val paths: AppPaths get() = app.paths

    // ---- Collections ----
    val collections = settings.collections

    fun createCollection(name: String, first: Game? = null) = settings.createCollection(name, first?.id)
    fun renameCollection(c: GameCollection, name: String) = settings.renameCollection(c.id, name)
    fun deleteCollection(c: GameCollection) {
        val before = settings.collections.value
        settings.deleteCollection(c.id)
        say("Deleted “${c.name}”") { settings.setCollections(before) }
    }

    fun setInCollection(c: GameCollection, game: Game, inIt: Boolean) {
        settings.setInCollection(c.id, game.id, inIt)
        say(if (inIt) "Added to “${c.name}”" else "Removed from “${c.name}”") { settings.setInCollection(c.id, game.id, !inIt) }
    }

    // ---- Controller remaps (written here, read by the game process at launch) ----
    private val _remaps = MutableStateFlow(com.retrohero.app.data.Remap.load(app.paths.remapFile))
    val remaps: StateFlow<Map<String, Map<Int, Int>>> = _remaps

    fun setRemaps(profiles: Map<String, Map<Int, Int>>) {
        _remaps.value = profiles
        viewModelScope.launch(Dispatchers.IO) { com.retrohero.app.data.Remap.save(app.paths.remapFile, profiles) }
    }

    // ---- Backup & restore ----
    sealed interface BackupState {
        data object Working : BackupState
        data class Done(val message: String) : BackupState
        data class Failed(val message: String) : BackupState
    }

    private val _backup = MutableStateFlow<BackupState?>(null)
    val backupState: StateFlow<BackupState?> = _backup
    fun dismissBackup() { _backup.value = null }

    private val backup by lazy { com.retrohero.app.data.Backup(app.filesDir, settings) }

    fun backUp(uri: Uri, includeBios: Boolean) {
        _backup.value = BackupState.Working
        viewModelScope.launch(Dispatchers.IO) {
            _backup.value = runCatching {
                val version = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
                app.contentResolver.openOutputStream(uri, "wt")!!.use { backup.write(it, games.value, includeBios, version) }
                BackupState.Done("Backup saved. Keep it somewhere safe, like Drive or your computer.")
            }.getOrElse { BackupState.Failed("Couldn't save the backup: ${it.message ?: "unknown error"}") }
        }
    }

    fun restore(uri: Uri) {
        _backup.value = BackupState.Working
        viewModelScope.launch(Dispatchers.IO) {
            _backup.value = runCatching {
                val r = app.contentResolver.openInputStream(uri)!!.use { backup.restore(it, games.value) }
                _remaps.value = com.retrohero.app.data.Remap.load(app.paths.remapFile)
                _biosVersion.value++
                refreshStats()
                BackupState.Done(
                    buildString {
                        append("Restored ${r.files} files")
                        if (r.favorites > 0) append(", ${r.favorites} favourites")
                        if (r.collections > 0) append(", ${r.collections} collections")
                        append(".")
                        if (r.unmatched > 0) append(" ${r.unmatched} games from the backup aren't in your library yet; add their folder and restore again to link them.")
                    },
                )
            }.getOrElse {
                BackupState.Failed(
                    (it as? com.retrohero.app.data.Backup.BadBackupException)?.message ?: "Couldn't restore: ${it.message ?: "unknown error"}",
                )
            }
        }
    }

    // ---- Play time ----
    private val _stats = MutableStateFlow<Map<String, PlayStat>>(emptyMap())
    /** Play time per game id (only games that have been played). */
    val stats: StateFlow<Map<String, PlayStat>> = _stats

    private val _shots = MutableStateFlow<List<Shot>>(emptyList())
    /** Every screenshot you've taken, newest first. */
    val shots: StateFlow<List<Shot>> = _shots

    private val _history = MutableStateFlow<List<com.retrohero.app.data.HistoryEntry>>(emptyList())
    /** Every play session, newest first. */
    val history: StateFlow<List<com.retrohero.app.data.HistoryEntry>> = _history

    /** Re-reads play time; the game process writes it, so call this when returning to the app. */
    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _stats.value = games.value.mapNotNull { g ->
                val f = app.paths.playtimeFile(g)
                if (f.exists()) g.id to PlayTime.read(f) else null
            }.toMap()
            _history.value = com.retrohero.app.data.PlayHistory.entries(games.value, app.paths)
            _shots.value = games.value.flatMap { g -> app.paths.screenshotsOf(g).map { Shot(g, it) } }
                .sortedByDescending { it.file.lastModified() }
            _artVersion.value++  // new screenshots may have arrived too
        }
    }

    // ---- Screenshots and covers ----
    private val _artVersion = MutableStateFlow(0)
    /** Changes whenever covers or screenshots change, so artwork reloads. */
    val artVersion: StateFlow<Int> = _artVersion

    fun screenshots(game: Game): List<File> = app.paths.screenshotsOf(game)

    fun useAsCover(game: Game, shot: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val cover = app.paths.coverFile(game)
            cover.parentFile?.mkdirs()
            shot.copyTo(cover, overwrite = true)
            com.retrohero.app.ui.components.forgetArtColor(game.id)
            _artVersion.value++
        }
    }

    fun clearCover(game: Game) {
        viewModelScope.launch(Dispatchers.IO) {
            app.paths.coverFile(game).delete()
            com.retrohero.app.ui.components.forgetArtColor(game.id)
            _artVersion.value++
        }
    }

    fun deleteScreenshot(game: Game, shot: File) {
        viewModelScope.launch(Dispatchers.IO) {
            // Keep it aside so Undo can put it back; the cache is cleared by the system later.
            val trash = File(app.cacheDir, "trash").apply { mkdirs() }
            val kept = File(trash, "${System.nanoTime()}_${shot.name}")
            if (!shot.renameTo(kept)) shot.delete()
            _shots.value = _shots.value.filter { it.file != shot }
            _artVersion.value++
            say("Screenshot deleted") {
                viewModelScope.launch(Dispatchers.IO) {
                    if (kept.exists()) kept.renameTo(shot)
                    _artVersion.value++
                    refreshStats()
                }
            }
        }
    }

    sealed interface LaunchState {
        data class NeedsCore(val game: Game, val core: CoreInfo, val slot: Int? = null) : LaunchState
        data class Downloading(val game: Game, val core: CoreInfo) : LaunchState
        data class Preparing(val game: Game, val file: String) : LaunchState
        data class Error(val message: String) : LaunchState
    }

    private val _launch = MutableStateFlow<LaunchState?>(null)
    val launchState: StateFlow<LaunchState?> = _launch

    private val _launchIntents = Channel<Intent>(Channel.BUFFERED)
    val launchIntents = _launchIntents.receiveAsFlow()

    private val _biosVersion = MutableStateFlow(0)
    val biosVersion: StateFlow<Int> = _biosVersion

    // ---- Link downloads (games & BIOS) -------------------------------------------------

    enum class DownloadKind { Game, Bios }

    sealed interface DownloadState {
        val kind: DownloadKind

        data class Input(override val kind: DownloadKind, val error: String? = null) : DownloadState
        data class Running(override val kind: DownloadKind, val read: Long, val total: Long?, val label: String? = null) : DownloadState
        /** The link was a folder listing; the user picks which game files to fetch. */
        data class Folder(override val kind: DownloadKind, val url: String, val entries: List<Downloader.FolderEntry>) : DownloadState
        data class Done(override val kind: DownloadKind, val title: String, val detail: String, val game: Game? = null) : DownloadState
        data class Failed(override val kind: DownloadKind, val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState?>(null)
    val download: StateFlow<DownloadState?> = _download
    private var downloadJob: Job? = null

    fun openDownload(kind: DownloadKind) {
        _download.value = DownloadState.Input(kind)
    }

    fun startDownload(kind: DownloadKind, url: String) {
        Downloader.validate(url)?.let {
            _download.value = DownloadState.Input(kind, it)
            return
        }
        _download.value = DownloadState.Running(kind, 0, null)
        downloadJob = viewModelScope.launch {
            try {
                val file = Downloader.fetch(url, app.paths.downloadTmp) { read, total ->
                    _download.value = DownloadState.Running(kind, read, total)
                }
                _download.value = when (kind) {
                    DownloadKind.Game -> finishGame(file)
                    DownloadKind.Bios -> finishBios(file)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Downloader.PageNotFileException) {
                // Probably a folder on your own server: list the games in it.
                _download.value = if (kind == DownloadKind.Game) {
                    runCatching { Downloader.listFolder(e.url, gameExtensions) }.fold(
                        onSuccess = { entries ->
                            if (entries.isEmpty()) DownloadState.Failed(kind, "That page doesn't list any game files RetroHero can play.")
                            else DownloadState.Folder(kind, e.url, entries)
                        },
                        onFailure = { DownloadState.Failed(kind, it.message ?: "Couldn't read that folder") },
                    )
                } else {
                    DownloadState.Failed(kind, e.message ?: "Download failed")
                }
            } catch (e: Exception) {
                _download.value = DownloadState.Failed(kind, e.message ?: "Download failed")
            }
        }
    }

    private val gameExtensions: Set<String> by lazy { Systems.all.flatMap { it.extensions }.toSet() + setOf("zip", "7z") }

    /** Downloads the chosen files from a folder listing, one after another. */
    fun downloadFolderEntries(entries: List<Downloader.FolderEntry>) {
        if (entries.isEmpty()) return
        downloadJob = viewModelScope.launch {
            val added = ArrayList<String>()
            val failed = ArrayList<String>()
            entries.forEachIndexed { i, entry ->
                val label = "${i + 1} of ${entries.size} · ${entry.name}"
                _download.value = DownloadState.Running(DownloadKind.Game, 0, null, label)
                try {
                    val file = Downloader.fetch(entry.url, app.paths.downloadTmp) { read, total ->
                        _download.value = DownloadState.Running(DownloadKind.Game, read, total, label)
                    }
                    when (val result = finishGame(file)) {
                        is DownloadState.Done -> added += result.title
                        else -> failed += entry.name
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed += entry.name
                }
            }
            _download.value = if (added.isEmpty()) {
                DownloadState.Failed(DownloadKind.Game, "None of the files could be added.")
            } else {
                DownloadState.Done(
                    DownloadKind.Game,
                    if (added.size == 1) added.first() else "${added.size} games added",
                    buildString {
                        append(added.joinToString(", "))
                        if (failed.isNotEmpty()) append("\nSkipped: ").append(failed.joinToString(", "))
                    },
                )
            }
        }
    }

    // ---- Free homebrew --------------------------------------------------------------------

    fun installedHomebrew(games: List<Game>): Set<String> =
        games.filter { it.isLocal }.mapTo(HashSet()) { it.fileName }

    fun installHomebrew(entry: HomebrewGame) {
        _download.value = DownloadState.Running(DownloadKind.Game, 0, null, entry.title)
        downloadJob = viewModelScope.launch {
            try {
                val file = Downloader.fetch(entry.url, app.paths.downloadTmp) { read, total ->
                    _download.value = DownloadState.Running(DownloadKind.Game, read, total, entry.title)
                }
                val expected = entry.sha256
                if (expected != null && withContext(Dispatchers.IO) { Downloader.sha256(file) } != expected) {
                    file.delete()
                    _download.value = DownloadState.Failed(DownloadKind.Game, "The download didn't match the author's checksum, so it was discarded.")
                    return@launch
                }
                _download.value = finishGame(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _download.value = DownloadState.Failed(DownloadKind.Game, e.message ?: "Download failed")
            }
        }
    }

    private suspend fun finishGame(file: File): DownloadState = withContext(Dispatchers.IO) {
        val dest = File(app.paths.downloads, file.name)
        dest.delete()
        if (!file.renameTo(dest)) file.copyTo(dest, overwrite = true).also { file.delete() }
        val game = app.library.addDownloaded(dest)
        if (game == null) {
            dest.delete()
            DownloadState.Failed(DownloadKind.Game, "RetroHero doesn't recognise \"${file.name}\" as a game for any supported system.")
        } else {
            DownloadState.Done(DownloadKind.Game, game.title, "Added to ${game.system?.name}", game)
        }
    }

    private suspend fun finishBios(file: File): DownloadState = withContext(Dispatchers.IO) {
        try {
            val results = bios.install(file, file.name)
            _biosVersion.value++
            when {
                results.isEmpty() -> DownloadState.Failed(DownloadKind.Bios, "No recognised BIOS files were found in ${file.name}.")
                else -> DownloadState.Done(
                    DownloadKind.Bios,
                    if (results.size == 1) results[0].name else "${results.size} BIOS files installed",
                    results.joinToString("\n") { "${it.name} · ${if (it.verified) "verified ✓" else "unverified (checksum unknown or different)"}" },
                )
            }
        } finally {
            file.delete()
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _download.value = null
    }

    fun playDownloaded(game: Game) {
        _download.value = null
        play(game)
    }

    fun deleteGame(game: Game) {
        viewModelScope.launch { app.library.deleteLocal(game) }
    }

    fun addFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { app.contentResolver.takePersistableUriPermission(uri, flags) }
        settings.addRomFolder(uri.toString())
        rescan()
    }

    fun removeFolder(uri: String) {
        runCatching {
            app.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        settings.removeRomFolder(uri)
        rescan()
    }

    fun rescan() {
        viewModelScope.launch { app.library.scan(settings.romFolders.value) }
    }

    fun gamesFor(system: GameSystem) = games.value.filter { it.systemId == system.id }

    /** Starts [game]; with [slot], resumes from that save-state slot instead of the auto-save. */
    fun play(game: Game, slot: Int? = null) {
        val system = game.system ?: return
        val core = settings.coreFor(system)
        if (app.cores.isInstalled(core.id)) prepareAndLaunch(game, core, slot)
        else _launch.value = LaunchState.NeedsCore(game, core, slot)
    }

    /** Save-state slots that exist for [game], newest first: slot to last-modified time. */
    fun saveSlots(game: Game): List<Pair<Int, Long>> =
        (listOf(AppPaths.AUTO_SLOT) + (1..4)).mapNotNull { slot ->
            app.paths.stateFile(game, slot).takeIf { it.exists() }?.let { slot to it.lastModified() }
        }.sortedByDescending { it.second }

    /** "Gacha" pick: play a random game (optionally from one system), avoiding the last one played. */
    fun playRandom(systemId: String? = null) {
        val all = games.value.filter { systemId == null || it.systemId == systemId }
        if (all.isEmpty()) return
        val last = settings.recents.value.firstOrNull()
        val pool = all.filter { it.id != last }.ifEmpty { all }
        play(pool.random())
    }

    fun confirmCoreDownload() {
        val state = _launch.value as? LaunchState.NeedsCore ?: return
        _launch.value = LaunchState.Downloading(state.game, state.core)
        viewModelScope.launch {
            val error = app.cores.download(state.core.id)
            if (error != null) _launch.value = LaunchState.Error("Couldn't download ${state.core.displayName}: $error")
            else prepareAndLaunch(state.game, state.core, state.slot)
        }
    }

    fun dismissLaunch() {
        if (_launch.value !is LaunchState.Downloading && _launch.value !is LaunchState.Preparing) _launch.value = null
    }

    private fun prepareAndLaunch(game: Game, core: CoreInfo, slot: Int? = null) {
        _launch.value = LaunchState.Preparing(game, game.fileName)
        viewModelScope.launch {
            try {
                val rom = withContext(Dispatchers.IO) {
                    app.romCache.prepare(game) { _launch.value = LaunchState.Preparing(game, it) }
                }
                val corePath = app.cores.coreFile(core.id) ?: error("Core not installed")
                settings.markPlayed(game.id)
                _launch.value = null
                _launchIntents.send(EmulatorActivity.intent(app, game, core.id, corePath.absolutePath, rom.absolutePath, slot))
            } catch (e: Exception) {
                _launch.value = LaunchState.Error(e.message ?: "Couldn't open this game.")
            }
        }
    }

    fun downloadCore(core: CoreInfo) {
        viewModelScope.launch { app.cores.download(core.id) }
    }

    fun deleteCore(core: CoreInfo) = app.cores.delete(core.id)

    fun setCore(system: GameSystem, core: CoreInfo) = settings.setCoreFor(system, core)

    fun importBios(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = bios.import(uris)
            _biosVersion.value++
            if (results.isNotEmpty()) {
                _download.value = DownloadState.Done(
                    DownloadKind.Bios,
                    if (results.size == 1) results[0].name else "${results.size} BIOS files installed",
                    results.joinToString("\n") { "${it.name} · ${if (it.verified) "verified ✓" else "unverified"}" },
                )
            }
        }
    }

    fun deleteBios(name: String) {
        bios.delete(name)
        _biosVersion.value++
    }
}
