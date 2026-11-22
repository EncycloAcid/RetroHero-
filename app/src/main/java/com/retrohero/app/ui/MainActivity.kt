package com.retrohero.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.GameSheet
import com.retrohero.app.ui.components.SystemBadge
import com.retrohero.app.ui.screens.CoresScreen
import com.retrohero.app.ui.screens.GamesScreen
import com.retrohero.app.ui.screens.SettingsScreen
import com.retrohero.app.ui.screens.SystemsScreen
import com.retrohero.app.ui.theme.RetroHeroTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dynamic by vm.dynamicColor.collectAsStateWithLifecycle()
            RetroHeroTheme(dynamicColor = dynamic) { App(vm) }
        }
    }
}

private enum class Tab(val label: String, val title: String, val selected: ImageVector, val unselected: ImageVector) {
    Games("Games", "RetroHero", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    Systems("Systems", "Systems", Icons.Filled.ViewModule, Icons.Outlined.ViewModule),
    Cores("Cores", "Emulators", Icons.Filled.Extension, Icons.Outlined.Extension),
    Settings("Settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
private fun App(vm: AppViewModel) {
    val context = LocalContext.current
    val games by vm.games.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val biosVersion by vm.biosVersion.collectAsStateWithLifecycle()
    val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Games) }
    var openSystemId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importBios(uris)
    }
    LaunchedEffect(Unit) { vm.launchIntents.collect { context.startActivity(it) } }

    val openSystem = openSystemId?.let { Systems.get(it) }
    BackHandler(enabled = openSystem != null) { openSystemId = null }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val systemCount = games.mapTo(HashSet()) { it.systemId }.size

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(openSystem?.name ?: tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    subtitle = {
                        val text = when {
                            openSystem != null -> {
                                val n = games.count { it.systemId == openSystem.id }
                                "${openSystem.manufacturer} · $n ${if (n == 1) "game" else "games"}"
                            }
                            tab == Tab.Games && games.isNotEmpty() -> "${games.size} games · $systemCount systems"
                            tab == Tab.Games -> "Your retro game library"
                            tab == Tab.Systems -> "${Systems.all.size} systems supported"
                            tab == Tab.Cores -> "Emulator engines"
                            else -> "Library, controls & BIOS"
                        }
                        Text(text)
                    },
                    navigationIcon = {
                        if (openSystem != null) {
                            FilledTonalIconButton(onClick = { openSystemId = null }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        if (openSystem != null) SystemBadge(openSystem, 40.dp, Modifier.padding(end = 12.dp))
                        if (openSystem == null && tab == Tab.Games && folders.isNotEmpty()) {
                            FilledTonalIconButton(onClick = vm::rescan, enabled = !scanning, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Outlined.Refresh, "Rescan library")
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                if (scanning) LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
        },
        bottomBar = {
            if (openSystem == null) {
                ShortNavigationBar {
                    Tab.entries.forEach { t ->
                        ShortNavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(if (tab == t) t.selected else t.unselected, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val screen = openSystem?.id ?: tab.name
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        AnimatedContent(
            targetState = screen,
            transitionSpec = { (fadeIn(effects) + scaleIn(spatial, initialScale = 0.96f)) togetherWith fadeOut(effects) },
            label = "screen",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { key ->
            val system = Systems.get(key)
            when {
                system != null -> GamesScreen(
                    games = games.filter { it.systemId == system.id },
                    recents = emptyList(),
                    hasFolders = folders.isNotEmpty(),
                    scanning = scanning,
                    onAddFolder = { folderPicker.launch(null) },
                    onPlay = vm::play,
                    onGameOptions = { sheetFor = it.id },
                    showSystemFilter = false,
                )
                key == Tab.Games.name -> GamesScreen(
                    games = games,
                    recents = recents,
                    hasFolders = folders.isNotEmpty(),
                    scanning = scanning,
                    onAddFolder = { folderPicker.launch(null) },
                    onPlay = vm::play,
                    onGameOptions = { sheetFor = it.id },
                )
                key == Tab.Systems.name -> SystemsScreen(games) { openSystemId = it.id }
                key == Tab.Cores.name -> CoresScreen(coreStates, vm::downloadCore, vm::deleteCore)
                else -> SettingsScreen(
                    settings = vm.settings,
                    dynamicColor = dynamicColor,
                    bios = vm.bios,
                    biosVersion = biosVersion,
                    folders = folders,
                    scanning = scanning,
                    scanProgress = scanProgress,
                    onAddFolder = { folderPicker.launch(null) },
                    onRemoveFolder = vm::removeFolder,
                    onRescan = vm::rescan,
                    onImportBios = { biosPicker.launch(arrayOf("*/*")) },
                    onDeleteBios = vm::deleteBios,
                )
            }
        }
    }

    sheetFor?.let { id -> games.firstOrNull { it.id == id } }?.let { game ->
        val system = game.system ?: return@let
        GameSheet(
            game = game,
            currentCore = vm.settings.coreFor(system),
            onSelectCore = { vm.setCore(system, it) },
            onPlay = { sheetFor = null; vm.play(game) },
            onDismiss = { sheetFor = null },
        )
    }

    LaunchDialogs(launchState, coreStates, vm)
}

@Composable
private fun LaunchDialogs(state: AppViewModel.LaunchState?, coreStates: Map<String, CoreState>, vm: AppViewModel) {
    when (state) {
        null -> Unit
        is AppViewModel.LaunchState.NeedsCore -> AlertDialog(
            onDismissRequest = vm::dismissLaunch,
            icon = { state.game.system?.let { SystemBadge(it, 56.dp) } },
            title = { Text("Get ${state.core.displayName}?", textAlign = TextAlign.Center) },
            text = {
                Text(
                    "${state.game.system?.name} games run on the ${state.core.displayName} emulator " +
                        "(${state.core.license}). It downloads once and is ready forever.",
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmCoreDownload) { Text("Download & play") } },
            dismissButton = { TextButton(onClick = vm::dismissLaunch) { Text("Not now") } },
        )
        is AppViewModel.LaunchState.Downloading -> {
            val progress = (coreStates[state.core.id] as? CoreState.Downloading)?.progress ?: 0f
            BusyDialog("Downloading ${state.core.displayName}") {
                LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
        is AppViewModel.LaunchState.Preparing -> BusyDialog("Loading ${state.game.title}", state.file) {
            ContainedLoadingIndicator(Modifier.size(72.dp))
        }
        is AppViewModel.LaunchState.Error -> AlertDialog(
            onDismissRequest = vm::dismissLaunch,
            title = { Text("Couldn't start") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = vm::dismissLaunch) { Text("OK") } },
        )
    }
}

@Composable
private fun BusyDialog(title: String, detail: String? = null, indicator: @Composable () -> Unit) {
    BasicAlertDialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(bottom = 20.dp), contentAlignment = Alignment.Center) { indicator() }
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
