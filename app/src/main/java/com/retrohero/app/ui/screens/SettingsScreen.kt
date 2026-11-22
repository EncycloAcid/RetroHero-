package com.retrohero.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrohero.app.BuildConfig
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.Saf
import com.retrohero.app.data.Settings
import com.retrohero.app.data.Systems

@Composable
fun SettingsScreen(
    settings: Settings,
    bios: BiosManager,
    biosVersion: Int,
    folders: Set<String>,
    scanning: Boolean,
    scanProgress: String,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onImportBios: () -> Unit,
    onDeleteBios: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("Game folders") {
            folders.forEach { uri ->
                val name = remember(uri) { runCatching { Saf.folderName(Uri.parse(uri)) }.getOrDefault(uri) }
                Item(name, leading = { Icon(Icons.Default.Folder, null) }) {
                    IconButton(onClick = { onRemoveFolder(uri) }) { Icon(Icons.Default.Close, "Remove") }
                }
            }
            if (scanning) Item("Scanning… $scanProgress")
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAddFolder) { Icon(Icons.Default.Add, null); Text(" Add folder") }
                FilledTonalButton(onClick = onRescan, enabled = !scanning && folders.isNotEmpty()) {
                    Icon(Icons.Default.Refresh, null); Text(" Rescan")
                }
            }
        }

        Section("BIOS & system files") {
            Text(
                "Some systems need BIOS files dumped from your own console. Import them here — " +
                    "file names must match exactly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // biosVersion forces recomposition after imports/deletes.
            key(biosVersion) {
                Systems.all.filter { it.bios.isNotEmpty() }.forEach { system ->
                    system.bios.forEach { file ->
                        val present = bios.isPresent(file)
                        Item(
                            title = file.fileName,
                            subtitle = "${system.shortName} · ${file.description}${if (file.required) " · required" else ""}",
                            leading = {
                                if (present) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.tertiary)
                                else Icon(Icons.Default.Close, null, tint = if (file.required) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                            },
                        ) {
                            if (present) IconButton(onClick = { onDeleteBios(file.fileName) }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    }
                }
            }
            FilledTonalButton(onClick = onImportBios, modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Default.Memory, null); Text(" Import BIOS files")
            }
        }

        Section("Display & controls") {
            ToggleItem("Sharp pixels", "Crisp nearest-neighbour scaling for pixel art", settings.sharpPixels) { settings.sharpPixels = it }
            ToggleItem("Haptic feedback", "Vibrate when touch buttons are pressed", settings.haptics) { settings.haptics = it }
            ToggleItem("Left stick drives D-pad", "For controllers, in games without analog support", settings.stickAsDpad) { settings.stickAsDpad = it }
            SliderItem("Touch controls opacity", settings.overlayOpacity, 0.2f..1f, { "${(it * 100).toInt()}%" }) { settings.overlayOpacity = it }
            SliderItem("Fast-forward speed", settings.fastForwardSpeed, 2f..8f, { "${it.toInt()}×" }, steps = 5) { settings.fastForwardSpeed = it }
        }

        Section("Saves") {
            ToggleItem("Auto save & resume", "Save state when quitting and resume where you left off", settings.autoSave) { settings.autoSave = it }
        }

        Section("About") {
            Item("RetroHero ${BuildConfig.VERSION_NAME}", "Core architecture: ${CoreManager.abi()}")
            Text(
                "RetroHero is a libretro frontend. It does not include any games or BIOS files. " +
                    "Only play games you legally own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 12.dp),
            )
        }
    }
}

@Composable
private fun key(value: Int, content: @Composable () -> Unit) = androidx.compose.runtime.key(value) { content() }

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun Item(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = leading,
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ToggleItem(title: String, subtitle: String, initial: Boolean, onChange: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(initial) }
    Item(title, subtitle) {
        Switch(checked = checked, onCheckedChange = { checked = it; onChange(it) })
    }
}

@Composable
private fun SliderItem(
    title: String,
    initial: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(initial) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f))
            Text(format(value), color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = { value = it }, onValueChangeFinished = { onChange(value) }, valueRange = range, steps = steps)
    }
}
