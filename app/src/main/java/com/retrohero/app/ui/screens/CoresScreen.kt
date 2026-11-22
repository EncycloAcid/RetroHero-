package com.retrohero.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SystemTile
import com.retrohero.app.ui.ios.CapsuleButton
import com.retrohero.app.ui.ios.CapsuleStyle
import com.retrohero.app.ui.ios.GroupedSection
import com.retrohero.app.ui.ios.IosRow
import com.retrohero.app.ui.ios.LargeTitle

@Composable
fun CoresScreen(
    states: Map<String, CoreState>,
    onDownload: (CoreInfo) -> Unit,
    onDelete: (CoreInfo) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val cores = Systems.allCores()
    val (installed, available) = cores.partition { states[it.first.id] is CoreState.Installed }
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { LargeTitle("Emulators", subtitle = "${installed.size} of ${cores.size} installed") }
        if (installed.isNotEmpty()) {
            item { CoreGroup("Installed", null, installed, states, onDownload, onDelete) }
        }
        if (available.isNotEmpty()) {
            item {
                CoreGroup(
                    "Available",
                    "Emulators download automatically the first time you play a system. Built for ${CoreManager.abi()}. " +
                        "Each is an independent open-source project with its own license.",
                    available, states, onDownload, onDelete,
                )
            }
        }
    }
}

@Composable
private fun CoreGroup(
    title: String,
    footer: String?,
    cores: List<Pair<CoreInfo, List<GameSystem>>>,
    states: Map<String, CoreState>,
    onDownload: (CoreInfo) -> Unit,
    onDelete: (CoreInfo) -> Unit,
) {
    GroupedSection(header = title, footer = footer, dividerInset = 74.dp) {
        cores.forEach { (core, systems) ->
            row {
                val state = states[core.id] ?: CoreState.NotInstalled
                IosRow(
                    title = core.displayName,
                    subtitle = buildString {
                        append(systems.joinToString { it.shortName })
                        append(" · ")
                        append(
                            when (state) {
                                is CoreState.Installed -> if (state.bundled) "Built in" else "${state.sizeBytes / (1024 * 1024)} MB"
                                is CoreState.Downloading -> "Downloading…"
                                is CoreState.Failed -> "Failed: ${state.message}"
                                CoreState.NotInstalled -> core.license
                            },
                        )
                    },
                    icon = { SystemTile(systems.first(), 44.dp) },
                ) {
                    when (state) {
                        is CoreState.Downloading -> CircularWavyProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.size(32.dp),
                        )
                        is CoreState.Installed -> if (!state.bundled) {
                            CapsuleButton("Remove", { onDelete(core) }, style = CapsuleStyle.Gray, contentColor = MaterialTheme.colorScheme.error)
                        }
                        else -> CapsuleButton("Get", { onDownload(core) }, style = CapsuleStyle.Gray)
                    }
                }
            }
        }
    }
}
