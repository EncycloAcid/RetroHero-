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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.R
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.ui.components.LocalArtPaths
import com.retrohero.app.ui.components.LocalArtVersion
import com.retrohero.app.ui.theme.EmberOrangeColor
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setAccent(EmberOrangeColor)
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
                    ShelvesHost(vm)
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
private fun ShelvesHost(vm: AppViewModel) {
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

    ShelvesAppView(
        games = games,
        recents = recents,
        favoriteIds = favoriteIds,
        stats = stats,
        shelfStatuses = shelfStatuses,
        scanning = scanning,
        hasFolders = folders.isNotEmpty(),
        saveSlotsFor = { g -> vm.saveSlots(g) },
        currentCoreFor = { g -> g.system?.let { vm.settings.coreFor(it) } },
        onAddFolder = { folderPicker.launch(null) },
        onRescan = vm::rescan,
        onOpenHomebrew = { homebrewOpen = true },
        onOpenDownload = { vm.openDownload(AppViewModel.DownloadKind.Game) },
        onImportBios = { biosPicker.launch(arrayOf("*/*")) },
        onImportSkin = { skinPicker.launch(arrayOf("*/*")) },
        onSetShelfBucket = { g, bucket -> vm.setShelfStatus(g, bucket.id) },
        onToggleFavorite = { vm.toggleFavorite(it) },
        onPlay = { g, slot -> vm.play(g, slot) },
        onSelectCore = { g, core -> g.system?.let { vm.setCore(it, core) } },
        onDeleteGame = { g -> vm.deleteGame(g) },
        onToggleTheme = {
            val next = !Tone.isDark
            vm.settings.darkMode = next
            Tone.setDarkMode(next)
        },
    )

    if (homebrewOpen) {
        HomebrewCatalogSheet(
            installedNames = vm.installedHomebrew(games),
            onInstall = vm::installHomebrew,
            onDismiss = { homebrewOpen = false },
        )
    }

    DownloadDialogs(download, vm)
    LaunchDialogs(launchState, coreStates, vm)
}

@Composable
private fun DownloadDialogs(state: AppViewModel.DownloadState?, vm: AppViewModel) {
    when (state) {
        null -> Unit
        is AppViewModel.DownloadState.Input -> {
            var url by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = vm::cancelDownload,
                title = { Text("Download ROM from link") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = { Text("https://…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.error != null) {
                            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { vm.startDownload(state.kind, url) }) { Text("Download") } },
                dismissButton = { TextButton(onClick = vm::cancelDownload) { Text("Cancel") } },
            )
        }
        is AppViewModel.DownloadState.Running -> BusyDialog(state.label ?: "Downloading…") {
            val total = state.total
            if (total != null && total > 0L) {
                LinearProgressIndicator(progress = { (state.read.toFloat() / total.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        is AppViewModel.DownloadState.Folder -> AlertDialog(
            onDismissRequest = vm::cancelDownload,
            title = { Text("Download ${state.entries.size} files?") },
            text = { Text(state.entries.take(6).joinToString("\n") { it.name }) },
            confirmButton = { TextButton(onClick = { vm.downloadFolderEntries(state.entries) }) { Text("Download all") } },
            dismissButton = { TextButton(onClick = vm::cancelDownload) { Text("Cancel") } },
        )
        is AppViewModel.DownloadState.Done -> AlertDialog(
            onDismissRequest = vm::cancelDownload,
            title = { Text(state.title) },
            text = { Text(state.detail) },
            confirmButton = {
                if (state.game != null) {
                    TextButton(onClick = { vm.playDownloaded(state.game) }) { Text("Play now") }
                } else {
                    TextButton(onClick = vm::cancelDownload) { Text("OK") }
                }
            },
            dismissButton = if (state.game != null) ({ TextButton(onClick = vm::cancelDownload) { Text("Done") } }) else null,
        )
        is AppViewModel.DownloadState.Failed -> AlertDialog(
            onDismissRequest = vm::cancelDownload,
            title = { Text("Download failed") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = vm::cancelDownload) { Text("OK") } },
        )
    }
}

@Composable
private fun LaunchDialogs(state: AppViewModel.LaunchState?, coreStates: Map<String, CoreState>, vm: AppViewModel) {
    when (state) {
        null -> Unit
        is AppViewModel.LaunchState.NeedsCore -> AlertDialog(
            onDismissRequest = vm::dismissLaunch,
            title = { Text("Download ${state.core.displayName}?") },
            text = {
                Text("${state.game.system?.name ?: state.game.systemId} runs on ${state.core.displayName} (${state.core.license}).")
            },
            confirmButton = { TextButton(onClick = vm::confirmCoreDownload) { Text("Download & play") } },
            dismissButton = { TextButton(onClick = vm::dismissLaunch) { Text("Cancel") } },
        )
        is AppViewModel.LaunchState.Downloading -> {
            val progress = (coreStates[state.core.id] as? CoreState.Downloading)?.progress ?: 0f
            BusyDialog("Downloading ${state.core.displayName}") {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
        is AppViewModel.LaunchState.Preparing -> BusyDialog("Loading ${state.game.title}", state.file) {
            CircularProgressIndicator(Modifier.size(36.dp))
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
        Surface(shape = MaterialTheme.shapes.medium, color = Tone.Raised) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(bottom = 16.dp), contentAlignment = Alignment.Center) { indicator() }
                Text(title, style = MaterialTheme.typography.titleMedium, color = Tone.Ink, textAlign = TextAlign.Center)
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Tone.Muted,
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
