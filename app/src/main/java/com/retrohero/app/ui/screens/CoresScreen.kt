package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.LocalNavSpace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import com.retrohero.app.ui.components.glossCard
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SegmentedGroup
import com.retrohero.app.ui.components.segmentColors
import com.retrohero.app.ui.components.SystemBadge
import com.retrohero.app.ui.components.auroraBackdrop

@Composable
fun CoresScreen(
    states: Map<String, CoreState>,
    onDownload: (CoreInfo) -> Unit,
    onDelete: (CoreInfo) -> Unit,
    pageIndex: Int = 5,
) {
    val cores = Systems.allCores()
    val (installed, available) = cores.partition { states[it.first.id] is CoreState.Installed }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalNavSpace.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
    ) {
        item { com.retrohero.app.ui.components.PageHeader("Emulators", "${installed.size} of ${cores.size} installed", index = pageIndex) }
        item {
            val abiLabel = when (val raw = CoreManager.abi().lowercase()) {
                "arm64-v8a" -> "ARM64"
                "armeabi-v7a" -> "ARM32"
                else -> raw.uppercase()
            }
            com.retrohero.app.ui.components.BentoTelemetryStrip(
                metrics = listOf(
                    com.retrohero.app.ui.components.BentoMetric(installed.size.toString(), "INSTALLED", Icons.Outlined.CloudDownload),
                    com.retrohero.app.ui.components.BentoMetric(cores.size.toString(), "ENGINES", Icons.Outlined.CloudDownload),
                    com.retrohero.app.ui.components.BentoMetric(Systems.all.size.toString(), "CONSOLES", Icons.Outlined.CloudDownload),
                    com.retrohero.app.ui.components.BentoMetric(abiLabel, "TARGET ABI", Icons.Outlined.CloudDownload),
                ),
            )
        }
        item {
            com.retrohero.app.ui.components.BentoPanel(
                header = "LIBRETRO ENGINE ARCHITECTURE",
                badge = "AUTO-FETCH",
            ) {
                Column(Modifier.padding(6.dp)) {
                    Text(
                        "Ready on first play",
                        style = MaterialTheme.typography.titleMedium,
                        color = com.retrohero.app.ui.theme.Tone.Ink,
                    )
                    Text(
                        "Emulator engines download automatically the first time you launch a console, or pre-install any engine below. Built for ${CoreManager.abi()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.retrohero.app.ui.theme.Tone.Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (installed.isNotEmpty()) item { CoreGroup("INSTALLED ENGINES", installed, states, onDownload, onDelete) }
        if (available.isNotEmpty()) item { CoreGroup("AVAILABLE ENGINES", available, states, onDownload, onDelete) }
    }
}

@Composable
private fun CoreGroup(
    title: String,
    cores: List<Pair<CoreInfo, List<GameSystem>>>,
    states: Map<String, CoreState>,
    onDownload: (CoreInfo) -> Unit,
    onDelete: (CoreInfo) -> Unit,
) {
    com.retrohero.app.ui.components.BentoPanel(
        header = title,
        badge = "${cores.size} CORES",
    ) {
        cores.forEach { (core, systems) ->
            val state = states[core.id] ?: CoreState.NotInstalled
            val statusText = when (state) {
                is CoreState.Installed -> if (state.bundled) "BUNDLED" else "${(state.sizeBytes / (1024 * 1024)).coerceAtLeast(1)} MB"
                is CoreState.Downloading -> "${(state.progress * 100).toInt()}%"
                is CoreState.Failed -> "RETRY"
                CoreState.NotInstalled -> core.license.uppercase()
            }
            com.retrohero.app.ui.components.BentoItemRow(
                title = core.displayName,
                subtitle = systems.joinToString { it.shortName },
                rightText = statusText,
                highlighted = state is CoreState.Installed,
                leading = {
                    com.retrohero.app.ui.components.ArchiveLabel(
                        systems.first().shortName.take(4),
                        color = if (state is CoreState.Installed) com.retrohero.app.ui.components.Ascend.onAccent else com.retrohero.app.ui.theme.Tone.Ink,
                    )
                },
                trailing = {
                    when (state) {
                        is CoreState.Downloading -> CircularWavyProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.size(32.dp),
                        )
                        is CoreState.Installed -> if (!state.bundled) {
                            IconButton(
                                onClick = { onDelete(core) },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(com.retrohero.app.ui.theme.Tone.Raised, CircleShape)
                                    .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, CircleShape),
                            ) {
                                Icon(Icons.Outlined.Delete, "Delete ${core.displayName}", tint = com.retrohero.app.ui.theme.Tone.Ink, modifier = Modifier.size(16.dp))
                            }
                        }
                        else -> IconButton(
                            onClick = { onDownload(core) },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier
                                .size(34.dp)
                                .background(com.retrohero.app.ui.theme.Tone.Raised, CircleShape)
                                .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, CircleShape),
                        ) {
                            Icon(Icons.Outlined.CloudDownload, "Download ${core.displayName}", tint = com.retrohero.app.ui.theme.Tone.Ink, modifier = Modifier.size(16.dp))
                        }
                    }
                },
            )
        }
    }
}
