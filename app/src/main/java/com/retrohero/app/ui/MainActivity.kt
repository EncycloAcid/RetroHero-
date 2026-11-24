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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.R
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                RomLauncherScreen(vm)
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
internal fun RomLauncherScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val games by vm.games.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }

    LaunchedEffect(Unit) {
        vm.launchIntents.collect {
            val options = ActivityOptions.makeCustomAnimation(context, R.anim.game_enter, R.anim.hold)
            context.startActivity(it, options.toBundle())
        }
    }

    RomListContent(
        games = games,
        scanning = scanning,
        onAddFolder = { folderPicker.launch(null) },
        onPlay = vm::play,
    )

    LaunchDialogs(launchState, coreStates, vm)
}

@Composable
internal fun RomListContent(
    games: List<Game>,
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onPlay: (Game) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Tone.Bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (scanning) "Scanning…" else "${games.size} ROMs",
                style = MaterialTheme.typography.bodyMedium,
                color = Tone.Muted,
            )
            OutlinedButton(onClick = onAddFolder) {
                Text("Add folder")
            }
        }
        HorizontalDivider(color = Tone.Line)

        if (games.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (scanning) "Scanning folder…" else "No ROMs loaded",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Tone.Muted,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(games, key = { it.id }) { game ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onPlay(game) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Tone.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                        )
                        Text(
                            text = game.system?.shortName ?: game.systemId.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Tone.Muted,
                        )
                    }
                    HorizontalDivider(color = Tone.Line)
                }
            }
        }
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
                Text("${state.game.system?.name ?: state.game.systemId} uses ${state.core.displayName}.")
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
