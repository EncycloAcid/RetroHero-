package com.retrohero.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retrohero.app.RetroHeroApp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.emulator.EmulatorActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    sealed interface LaunchState {
        data class NeedsCore(val game: Game, val core: CoreInfo) : LaunchState
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

    fun play(game: Game) {
        val system = game.system ?: return
        val core = settings.coreFor(system)
        if (app.cores.isInstalled(core.id)) prepareAndLaunch(game, core)
        else _launch.value = LaunchState.NeedsCore(game, core)
    }

    fun confirmCoreDownload() {
        val state = _launch.value as? LaunchState.NeedsCore ?: return
        _launch.value = LaunchState.Downloading(state.game, state.core)
        viewModelScope.launch {
            val error = app.cores.download(state.core.id)
            if (error != null) _launch.value = LaunchState.Error("Couldn't download ${state.core.displayName}: $error")
            else prepareAndLaunch(state.game, state.core)
        }
    }

    fun dismissLaunch() {
        if (_launch.value !is LaunchState.Downloading && _launch.value !is LaunchState.Preparing) _launch.value = null
    }

    private fun prepareAndLaunch(game: Game, core: CoreInfo) {
        _launch.value = LaunchState.Preparing(game, game.fileName)
        viewModelScope.launch {
            try {
                val rom = withContext(Dispatchers.IO) {
                    app.romCache.prepare(game) { _launch.value = LaunchState.Preparing(game, it) }
                }
                val corePath = app.cores.coreFile(core.id) ?: error("Core not installed")
                settings.markPlayed(game.id)
                _launch.value = null
                _launchIntents.send(EmulatorActivity.intent(app, game, core.id, corePath.absolutePath, rom.absolutePath))
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
            bios.import(uris)
            _biosVersion.value++
        }
    }

    fun deleteBios(name: String) {
        bios.delete(name)
        _biosVersion.value++
    }
}
