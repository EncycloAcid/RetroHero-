package com.retrohero.app.ui

import android.app.ActivityOptions
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retrohero.app.R
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.DownloadSheet
import com.retrohero.app.ui.components.GameSheet
import com.retrohero.app.ui.components.SystemBadge
import com.retrohero.app.ui.ios.BackButton
import com.retrohero.app.ui.ios.IosNavBar
import com.retrohero.app.ui.ios.IosTabBar
import com.retrohero.app.ui.ios.NavBarHeight
import com.retrohero.app.ui.ios.Squircles
import com.retrohero.app.ui.ios.TabBarHeight
import com.retrohero.app.ui.ios.TabItem
import com.retrohero.app.ui.screens.CoresScreen
import com.retrohero.app.ui.screens.GamesScreen
import com.retrohero.app.ui.screens.SettingsScreen
import com.retrohero.app.ui.screens.SystemsScreen
import com.retrohero.app.ui.theme.RetroHeroTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
    Games("Library", "Library", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    Systems("Systems", "Systems", Icons.Rounded.GridView, Icons.Outlined.GridView),
    Cores("Emulators", "Emulators", Icons.Rounded.Extension, Icons.Outlined.Extension),
    Settings("Settings", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
}

@Composable
private fun App(vm: AppViewModel) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val games by vm.games.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val biosVersion by vm.biosVersion.collectAsStateWithLifecycle()
    val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Games) }
    var openSystemId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    var addMenu by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importBios(uris)
    }
    LaunchedEffect(Unit) {
        vm.launchIntents.collect {
            val options = ActivityOptions.makeCustomAnimation(context, R.anim.game_enter, R.anim.hold)
            context.startActivity(it, options.toBundle())
        }
    }

    val openSystem = openSystemId?.let { Systems.get(it) }
    BackHandler(enabled = openSystem != null) { openSystemId = null }

    val screen = openSystem?.id ?: tab.name
    // Track how far the current screen has scrolled so the nav bar can fade in its material + title.
    var scrolled by remember(screen) { mutableFloatStateOf(0f) }
    val collapseDistance = with(density) { 56.dp.toPx() }
    val scrollTracker = remember(screen) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                scrolled = (scrolled - consumed.y).coerceAtLeast(0f)
                return Offset.Zero
            }
        }
    }
    val collapse = { (scrolled / collapseDistance).coerceIn(0f, 1f) }

    val hazeState = rememberHazeState()
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(
        top = statusBar + NavBarHeight,
        bottom = navBar + if (openSystem == null) TabBarHeight else 0.dp,
    )

    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
    Box(Modifier.fillMaxSize()) {
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        AnimatedContent(
            targetState = screen,
            transitionSpec = { (fadeIn(effects) + scaleIn(spatial, initialScale = 0.97f)) togetherWith fadeOut(effects) },
            label = "screen",
            modifier = Modifier.fillMaxSize().hazeSource(hazeState).nestedScroll(scrollTracker),
        ) { key ->
            val system = Systems.get(key)
            when {
                system != null -> GamesScreen(
                    games = games.filter { it.systemId == system.id },
                    recents = emptyList(),
                    hasFolders = folders.isNotEmpty(),
                    scanning = scanning,
                    onAddFolder = { folderPicker.launch(null) },
                    onDownload = { vm.openDownload(AppViewModel.DownloadKind.Game) },
                    onPlay = vm::play,
                    onGameOptions = { sheetFor = it.id },
                    contentPadding = contentPadding,
                    title = system.name,
                    showSystemFilter = false,
                )
                key == Tab.Games.name -> {
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = scanning,
                        onRefresh = vm::rescan,
                        state = pullState,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = pullState,
                                isRefreshing = scanning,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = contentPadding.calculateTopPadding()),
                            )
                        },
                    ) {
                        GamesScreen(
                            games = games,
                            recents = recents,
                            hasFolders = folders.isNotEmpty(),
                            scanning = scanning,
                            onAddFolder = { folderPicker.launch(null) },
                            onDownload = { vm.openDownload(AppViewModel.DownloadKind.Game) },
                            onPlay = vm::play,
                            onGameOptions = { sheetFor = it.id },
                            contentPadding = contentPadding,
                        )
                    }
                }
                key == Tab.Systems.name -> SystemsScreen(games, { openSystemId = it.id }, contentPadding)
                key == Tab.Cores.name -> CoresScreen(coreStates, vm::downloadCore, vm::deleteCore, contentPadding)
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
                    onDownloadBios = { vm.openDownload(AppViewModel.DownloadKind.Bios) },
                    onDeleteBios = vm::deleteBios,
                    contentPadding = contentPadding,
                )
            }
        }

        IosNavBar(
            hazeState = hazeState,
            title = openSystem?.name ?: tab.title,
            collapse = collapse,
            modifier = Modifier.align(Alignment.TopCenter),
            leading = {
                if (openSystem != null) BackButton("Systems") { openSystemId = null }
            },
            trailing = {
                if (openSystem == null && tab == Tab.Games) {
                    Box {
                        IconButton(onClick = { addMenu = true }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.AddCircle, "Add games", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        DropdownMenu(
                            expanded = addMenu,
                            onDismissRequest = { addMenu = false },
                            shape = Squircles.Medium,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download from Link") },
                                trailingIcon = { Icon(Icons.Rounded.Download, null) },
                                onClick = { addMenu = false; vm.openDownload(AppViewModel.DownloadKind.Game) },
                            )
                            DropdownMenuItem(
                                text = { Text("Add Game Folder") },
                                trailingIcon = { Icon(Icons.Rounded.CreateNewFolder, null) },
                                onClick = { addMenu = false; folderPicker.launch(null) },
                            )
                        }
                    }
                }
                if (openSystem != null) SystemBadge(openSystem, 30.dp, Modifier.padding(end = 10.dp))
            },
        )

        if (openSystem == null) {
            IosTabBar(
                hazeState = hazeState,
                items = Tab.entries.map { TabItem(it.label, it.selected, it.unselected) },
                selected = tab.ordinal,
                onSelect = { tab = Tab.entries[it] },
                modifier = Modifier.align(Alignment.BottomCenter),
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
            onDelete = if (game.isLocal) {
                { sheetFor = null; vm.deleteGame(game) }
            } else null,
            onDismiss = { sheetFor = null },
        )
    }

    download?.let { state ->
        DownloadSheet(
            state = state,
            onStart = vm::startDownload,
            onRetry = vm::openDownload,
            onPlay = vm::playDownloaded,
            onDismiss = vm::cancelDownload,
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
        Surface(shape = Squircles.Card, color = MaterialTheme.colorScheme.surfaceContainer) {
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
