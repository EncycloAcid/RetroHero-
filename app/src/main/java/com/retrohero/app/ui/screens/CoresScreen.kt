package com.retrohero.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems

@Composable
fun CoresScreen(
    states: Map<String, CoreState>,
    onDownload: (CoreInfo) -> Unit,
    onDelete: (CoreInfo) -> Unit,
) {
    val cores = Systems.allCores()
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Emulator cores are downloaded on demand from the libretro buildbot (${CoreManager.abi()}). " +
                    "Each core is a separate open-source project with its own license.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp, 0.dp, 4.dp, 8.dp),
            )
        }
        items(cores, key = { it.first.id }) { (core, systems) ->
            val state = states[core.id] ?: CoreState.NotInstalled
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                ListItem(
                    headlineContent = { Text(core.displayName) },
                    supportingContent = {
                        Column {
                            Text(systems.joinToString { it.shortName })
                            Text(
                                when (state) {
                                    is CoreState.Installed -> if (state.bundled) "Bundled · ${core.license}"
                                    else "Installed · ${state.sizeBytes / (1024 * 1024)} MB · ${core.license}"
                                    is CoreState.Downloading -> "Downloading…"
                                    is CoreState.Failed -> "Failed: ${state.message}"
                                    CoreState.NotInstalled -> "Not installed · ${core.license}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state is CoreState.Failed) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingContent = {
                        when (state) {
                            is CoreState.Installed -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                            is CoreState.Failed -> Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            else -> Icon(Icons.Default.CloudDownload, null)
                        }
                    },
                    trailingContent = {
                        when (state) {
                            is CoreState.Downloading -> CircularProgressIndicator(
                                progress = { state.progress }, modifier = Modifier.size(28.dp),
                            )
                            is CoreState.Installed -> if (!state.bundled) {
                                IconButton(onClick = { onDelete(core) }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                            else -> IconButton(onClick = { onDownload(core) }) { Icon(Icons.Default.CloudDownload, "Download") }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
