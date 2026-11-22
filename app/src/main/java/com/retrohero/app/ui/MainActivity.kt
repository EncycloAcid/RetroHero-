package com.retrohero.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.GameOptionsDialog
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
        setContent { RetroHeroTheme { App(vm) } }
    }
}

private enum class Tab(val label: String) { Games("Games"), Systems("Systems"), Cores("Cores"), Settings("Settings") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: AppViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val games by vm.games.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val biosVersion by vm.biosVersion.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Games) }
    var openSystemId by rememberSaveable { mutableStateOf<String?>(null) }
    var optionsFor by rememberSaveable { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importBios(uris)
    }

    LaunchedEffect(Unit) {
        vm.launchIntents.collect { context.startActivity(it) }
    }

    val openSystem = openSystemId?.let { Systems.get(it) }
    BackHandler(enabled = openSystem != null) { openSystemId = null }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            openSystem?.name ?: "RetroHero",
                            fontWeight = FontWeight.Black,
                            color = if (openSystem == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        if (openSystem != null) IconButton(onClick = { openSystemId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (tab == Tab.Games && folders.isNotEmpty()) {
                            IconButton(onClick = vm::rescan, enabled = !scanning) { Icon(Icons.Default.Refresh, "Rescan") }
                        }
                    },
                )
                if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            if (openSystem == null) NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.Games -> Icons.Default.SportsEsports
                                    Tab.Systems -> Icons.Default.ViewModule
                                    Tab.Cores -> Icons.Default.Extension
                                    Tab.Settings -> Icons.Default.Settings
                                },
                                null,
                            )
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val onOptions: (Game) -> Unit = { optionsFor = it.id }
            if (openSystem != null) {
                GamesScreen(
                    games = games.filter { it.systemId == openSystem.id },
                    recents = emptyList(),
                    hasFolders = folders.isNotEmpty(),
                    scanning = scanning,
                    onAddFolder = { folderPicker.launch(null) },
                    onPlay = vm::play,
                    onGameOptions = onOptions,
                    title = openSystem.name,
                )
            } else when (tab) {
                Tab.Games -> GamesScreen(
                    games = games,
                    recents = recents,
                    hasFolders = folders.isNotEmpty(),
                    scanning = scanning,
                    onAddFolder = { folderPicker.launch(null) },
                    onPlay = vm::play,
                    onGameOptions = onOptions,
                )
                Tab.Systems -> SystemsScreen(games) { openSystemId = it.id }
                Tab.Cores -> CoresScreen(coreStates, vm::downloadCore, vm::deleteCore)
                Tab.Settings -> SettingsScreen(
                    settings = vm.settings,
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

    optionsFor?.let { id -> games.firstOrNull { it.id == id } }?.let { game ->
        val system = game.system!!
        GameOptionsDialog(
            game = game,
            currentCore = vm.settings.coreFor(system),
            onSelectCore = { vm.setCore(system, it) },
            onPlay = { optionsFor = null; vm.play(game) },
            onDismiss = { optionsFor = null },
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
            title = { Text("Download emulator?") },
            text = {
                Text(
                    "${state.game.system?.name} games run on the ${state.core.displayName} core " +
                        "(${state.core.license} license). Download it now? This only happens once.",
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmCoreDownload) { Text("Download") } },
            dismissButton = { TextButton(onClick = vm::dismissLaunch) { Text("Cancel") } },
        )
        is AppViewModel.LaunchState.Downloading -> {
            val progress = (coreStates[state.core.id] as? CoreState.Downloading)?.progress ?: 0f
            ProgressDialog("Downloading ${state.core.displayName}…", progress)
        }
        is AppViewModel.LaunchState.Preparing -> ProgressDialog("Loading ${state.file}…", null)
        is AppViewModel.LaunchState.Error -> AlertDialog(
            onDismissRequest = vm::dismissLaunch,
            title = { Text("Something went wrong") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = vm::dismissLaunch) { Text("OK") } },
        )
    }
}

@Composable
private fun ProgressDialog(message: String, progress: Float?) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (progress != null && progress > 0f) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    CircularProgressIndicator()
                }
                Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
            }
        },
    )
}
