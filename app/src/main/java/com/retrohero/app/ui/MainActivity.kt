package com.retrohero.app.ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.R
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.ui.components.LocalArtPaths
import com.retrohero.app.ui.components.LocalArtVersion
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.resolveExpressiveAccent
import com.retrohero.app.ui.theme.setAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setAccent(resolveExpressiveAccent(vm.settings.accent))
        Tone.setDarkMode(vm.settings.darkMode)
        lifecycleScope.launch(Dispatchers.IO) {
            DeltaSkins.installBundledDefaults(this@MainActivity, vm.paths.deltaSkins, vm.settings)
        }
        if (savedInstanceState == null) handleIncomingDeltaSkin(intent)
        setContent {
            RetroHeroTheme {
                val view = LocalView.current
                val lightBars = !Tone.isDark
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                val artVersion by vm.artVersion.collectAsStateWithLifecycle()
                CompositionLocalProvider(
                    LocalArtPaths provides vm.paths,
                    LocalArtVersion provides artVersion,
                ) {
                    ExpressiveHost(vm)
                }
            }
        }
    }

    private fun handleIncomingDeltaSkin(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)!!.use { stream ->
                        DeltaSkins.import(vm.paths.deltaSkins, stream)
                    }
                }
            }
            result.onSuccess { skin ->
                skin.systemIds.forEach { vm.settings.setDeltaSkinFor(it, skin.identifier) }
                Toast.makeText(this@MainActivity, "Added skin “${skin.name}”", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, err.message ?: "Couldn't import .deltaskin file.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun ExpressiveHost(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val games by vm.games.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val shelfStatuses by vm.shelfStatuses.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()

    var autoSave by remember { mutableStateOf(vm.settings.autoSave) }
    var haptics by remember { mutableStateOf(vm.settings.haptics) }
    var fastForward by remember { mutableFloatStateOf(vm.settings.fastForwardSpeed) }
    var homebrewOpen by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        vm.refreshStats()
        onPauseOrDispose { }
    }
    LaunchedEffect(games.size) { vm.refreshStats() }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importBios(uris)
    }
    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            var count = 0
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)!!.use { stream ->
                            DeltaSkins.import(vm.paths.deltaSkins, stream)
                        }
                    }.onSuccess { skin ->
                        skin.systemIds.forEach { vm.settings.setDeltaSkinFor(it, skin.identifier) }
                        count++
                    }
                }
            }
            val msg = if (count > 0) "Imported $count controller skin${if (count == 1) "" else "s"}" else "Couldn't import .deltaskin"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        vm.launchIntents.collect {
            val options = ActivityOptions.makeCustomAnimation(context, R.anim.game_enter, R.anim.hold)
            context.startActivity(it, options.toBundle())
        }
    }

    ExpressiveAppView(
        games = games,
        recents = recents,
        favoriteIds = favoriteIds,
        stats = stats,
        shelfStatuses = shelfStatuses,
        scanning = scanning,
        foldersCount = folders.size,
        autoSave = autoSave,
        haptics = haptics,
        fastForwardSpeed = fastForward,
        coreStates = coreStates,
        saveSlotsFor = { g -> vm.saveSlots(g) },
        currentCoreFor = { g -> g.system?.let { vm.settings.coreFor(it) } },
        onPlay = { g, slot -> vm.play(g, slot) },
        onShufflePlay = { vm.playRandom() },
        onToggleFavorite = { g -> vm.toggleFavorite(g) },
        onSetShelfStatus = { g, status -> vm.setShelfStatus(g, status) },
        onSelectCore = { g, core -> g.system?.let { vm.setCore(it, core) } },
        onDeleteGame = { g -> vm.deleteGame(g) },
        onSetAutoSave = { v -> autoSave = v; vm.settings.autoSave = v },
        onSetHaptics = { v -> haptics = v; vm.settings.haptics = v },
        onSetFastForward = { v -> fastForward = v; vm.settings.fastForwardSpeed = v },
        onSelectAccent = { id, col -> vm.settings.accent = id; setAccent(col) },
        onSetDarkMode = { dark -> vm.settings.darkMode = dark; Tone.setDarkMode(dark) },
        onAddFolder = { folderPicker.launch(null) },
        onRescan = vm::rescan,
        onOpenHomebrew = { homebrewOpen = true },
        onOpenDownload = { vm.openDownload(AppViewModel.DownloadKind.Game) },
        onImportBios = { biosPicker.launch(arrayOf("*/*")) },
        onImportSkin = { skinPicker.launch(arrayOf("*/*")) },
        onDownloadCore = vm::downloadCore,
        onDeleteCore = vm::deleteCore,
    )

    if (homebrewOpen) {
        HomebrewCatalogSheet(
            installedUrls = vm.installedHomebrew(games),
            onInstall = { item ->
                homebrewOpen = false
                vm.installHomebrew(item)
            },
            onDismiss = { homebrewOpen = false },
        )
    }

    launchState?.let { state ->
        LaunchStateDialog(
            state = state,
            onConfirmDownload = vm::confirmCoreDownload,
            onDismiss = vm::dismissLaunch,
        )
    }

    download?.let { state ->
        DownloadStateDialog(
            state = state,
            onStart = vm::startDownload,
            onDownloadFolder = vm::downloadFolderEntries,
            onPlayGame = vm::playDownloaded,
            onCancel = vm::cancelDownload,
        )
    }
}
