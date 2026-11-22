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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SegmentedGroup
import com.retrohero.app.ui.components.segmentColors
import com.retrohero.app.ui.components.SystemBadge

@Composable
fun CoresScreen(
    states: Map<String, CoreState>,
    onDownload: (CoreInfo) -> Unit,
    onDelete: (CoreInfo) -> Unit,
) {
    val cores = Systems.allCores()
    val (installed, available) = cores.partition { states[it.first.id] is CoreState.Installed }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp + LocalNavSpace.current),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.padding(20.dp)) {
                    Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "${installed.size} of ${cores.size} emulators installed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        "Cores download automatically the first time you play a system. " +
                            "Built for ${CoreManager.abi()}. Each one is an independent open-source project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (installed.isNotEmpty()) item { CoreGroup("Installed", installed, states, onDownload, onDelete) }
        if (available.isNotEmpty()) item { CoreGroup("Available", available, states, onDownload, onDelete) }
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
    SegmentedGroup(title, cores.size) {
        cores.forEachIndexed { i, (core, systems) ->
            val state = states[core.id] ?: CoreState.NotInstalled
            SegmentedListItem(
                shapes = shapes(i),
                colors = segmentColors(),
                leadingContent = { SystemBadge(systems.first(), 44.dp) },
                supportingContent = {
                    Text(
                        buildString {
                            append(systems.joinToString { it.shortName })
                            append(" · ")
                            append(
                                when (state) {
                                    is CoreState.Installed -> if (state.bundled) "Bundled" else "${state.sizeBytes / (1024 * 1024)} MB"
                                    is CoreState.Downloading -> "Downloading…"
                                    is CoreState.Failed -> "Failed: ${state.message}"
                                    CoreState.NotInstalled -> core.license
                                },
                            )
                        },
                        color = if (state is CoreState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    when (state) {
                        is CoreState.Downloading -> CircularWavyProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.size(40.dp),
                        )
                        is CoreState.Installed -> if (!state.bundled) {
                            IconButton(onClick = { onDelete(core) }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Outlined.Delete, "Delete ${core.displayName}")
                            }
                        }
                        else -> FilledTonalIconButton(onClick = { onDownload(core) }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Outlined.CloudDownload, "Download ${core.displayName}")
                        }
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) { Text(core.displayName, style = MaterialTheme.typography.titleMedium) }
        }
    }
}
