package com.retrohero.app.ui.screens

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.retrohero.app.BuildConfig
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.Saf
import com.retrohero.app.data.Settings
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SegmentedGroup
import com.retrohero.app.ui.components.ShapeIcon
import com.retrohero.app.ui.components.segmentColors

@Composable
fun SettingsScreen(
    settings: Settings,
    dynamicColor: Boolean,
    bios: BiosManager,
    biosVersion: Int,
    folders: Set<String>,
    scanning: Boolean,
    scanProgress: String,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onImportBios: () -> Unit,
    onDownloadBios: () -> Unit,
    onDeleteBios: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ---- Library -------------------------------------------------------------------
        Column {
            val folderList = folders.toList()
            SegmentedGroup("Game folders", folderList.size + if (scanning) 1 else 0) {
                folderList.forEachIndexed { i, uri ->
                    val name = remember(uri) { runCatching { Saf.folderName(Uri.parse(uri)) }.getOrDefault(uri) }
                    SegmentedListItem(
                        shapes = shapes(i),
                        colors = segmentColors(),
                        leadingContent = { ShapeIcon(Icons.Outlined.Folder) },
                        trailingContent = {
                            IconButton(onClick = { onRemoveFolder(uri) }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Outlined.Close, "Remove $name")
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(name) }
                }
                if (scanning) {
                    SegmentedListItem(
                        shapes = shapes(folderList.size),
                        colors = segmentColors(),
                        supportingContent = { LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp)) },
                    ) { Text("Scanning $scanProgress…") }
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddFolder, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Add folder")
                }
                FilledTonalButton(onClick = onRescan, enabled = !scanning && folders.isNotEmpty(), shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Rescan")
                }
            }
        }

        // ---- Appearance & controls ------------------------------------------------------
        val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        SegmentedGroup("Look & feel", if (dynamicAvailable) 7 else 6) {
            var i = 0
            if (dynamicAvailable) {
                ToggleRow(shapes(i++), Icons.Outlined.Palette, "Wallpaper colours", "Match the app to your device theme", dynamicColor) {
                    settings.setDynamicColor(it)
                }
            }
            ToggleRow(shapes(i++), Icons.Outlined.GridOn, "Sharp pixels", "Crisp scaling for pixel art", settings.sharpPixels) { settings.sharpPixels = it }
            ToggleRow(shapes(i++), Icons.Outlined.Vibration, "Haptic feedback", "Buzz when touch buttons are pressed", settings.haptics) { settings.haptics = it }
            ToggleRow(shapes(i++), Icons.Outlined.Gamepad, "Stick moves D-pad", "Left stick acts as D-pad on controllers", settings.stickAsDpad) { settings.stickAsDpad = it }
            SliderRow(shapes(i++), Icons.Outlined.Opacity, "Touch controls opacity", settings.overlayOpacity, 0.2f..1f, { "${(it * 100).toInt()}%" }) {
                settings.overlayOpacity = it
            }
            SliderRow(shapes(i++), Icons.Outlined.FastForward, "Fast-forward speed", settings.fastForwardSpeed, 2f..8f, { "${it.toInt()}×" }, steps = 5) {
                settings.fastForwardSpeed = it
            }
            ToggleRow(shapes(i), Icons.Outlined.Save, "Auto save & resume", "Pick up exactly where you left off", settings.autoSave) { settings.autoSave = it }
        }

        // ---- BIOS -------------------------------------------------------------------------
        Column {
            key(biosVersion) {
                val files = Systems.all.flatMap { s -> s.bios.map { s to it } }
                SegmentedGroup("BIOS & system files", files.size) {
                    files.forEachIndexed { i, (system, file) ->
                        val status = remember(biosVersion, file) { bios.status(file) }
                        val present = status != BiosManager.Status.Missing
                        SegmentedListItem(
                            shapes = shapes(i),
                            colors = segmentColors(),
                            leadingContent = {
                                when {
                                    status == BiosManager.Status.Verified -> Icon(Icons.Outlined.Verified, "Verified", tint = MaterialTheme.colorScheme.primary)
                                    status == BiosManager.Status.Unverified -> Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Unverified", tint = MaterialTheme.colorScheme.tertiary)
                                    file.required -> Icon(Icons.Outlined.ErrorOutline, "Missing", tint = MaterialTheme.colorScheme.error)
                                    else -> Icon(Icons.Outlined.Memory, "Optional", tint = MaterialTheme.colorScheme.outline)
                                }
                            },
                            supportingContent = {
                                val tag = when (status) {
                                    BiosManager.Status.Verified -> " · verified"
                                    BiosManager.Status.Unverified -> " · checksum doesn't match"
                                    BiosManager.Status.Missing -> if (file.required) " · required" else ""
                                }
                                Text("${system.shortName} · ${file.description}$tag")
                            },
                            trailingContent = {
                                if (present) {
                                    IconButton(onClick = { onDeleteBios(file.fileName) }, shapes = IconButtonDefaults.shapes()) {
                                        Icon(Icons.Outlined.Delete, "Delete ${file.fileName}")
                                    }
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Text(file.fileName) }
                    }
                }
            }
            Text(
                "BIOS files come from your own consoles. Each file is identified by checksum, so any file name works.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp),
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDownloadBios, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Download")
                }
                FilledTonalButton(onClick = onImportBios, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.Memory, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Import files")
                }
            }
        }

        // ---- About ------------------------------------------------------------------------
        SegmentedGroup("About", 2) {
            SegmentedListItem(
                shapes = shapes(0),
                colors = segmentColors(),
                leadingContent = { ShapeIcon(Icons.Outlined.AutoAwesome) },
                supportingContent = { Text("Version ${BuildConfig.VERSION_NAME} · ${CoreManager.abi()}") },
            ) { Text("RetroHero") }
            SegmentedListItem(
                shapes = shapes(1),
                colors = segmentColors(),
                leadingContent = { ShapeIcon(Icons.Outlined.Info) },
                supportingContent = { Text("No games or BIOS files are included. Play games you own.") },
            ) { Text("Powered by libretro") }
        }
    }
}

@Composable
private fun ToggleRow(
    shapes: ListItemShapes,
    icon: ImageVector,
    title: String,
    subtitle: String,
    initial: Boolean,
    onChange: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(initial) }
    SegmentedListItem(
        onClick = { checked = !checked; onChange(checked) },
        shapes = shapes,
        colors = segmentColors(),
        leadingContent = { ShapeIcon(icon) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { checked = it; onChange(it) },
                thumbContent = if (checked) {
                    { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
            )
        },
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(title) }
}

@Composable
private fun SliderRow(
    shapes: ListItemShapes,
    icon: ImageVector,
    title: String,
    initial: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(initial) }
    SegmentedListItem(
        shapes = shapes,
        colors = segmentColors(),
        leadingContent = { ShapeIcon(icon) },
        trailingContent = { Text(format(value), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onChange(value) },
                valueRange = range,
                steps = steps,
            )
        },
    ) { Text(title) }
}
