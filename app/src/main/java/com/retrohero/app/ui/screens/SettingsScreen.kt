package com.retrohero.app.ui.screens

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.retrohero.app.BuildConfig
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.Saf
import com.retrohero.app.data.Settings
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.ios.GroupBuilder
import com.retrohero.app.ui.ios.GroupedSection
import com.retrohero.app.ui.ios.IconTile
import com.retrohero.app.ui.ios.IosRow
import com.retrohero.app.ui.ios.IosSwitch
import com.retrohero.app.ui.ios.LargeTitle
import com.retrohero.app.ui.ios.TrailingValue
import com.retrohero.app.ui.theme.IosColors

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
    contentPadding: PaddingValues = PaddingValues(),
) {
    val biosFiles = remember { Systems.all.flatMap { s -> s.bios.map { s to it } } }
    val biosStatus = remember(biosVersion) { biosFiles.associate { (_, f) -> f.fileName to bios.status(f) } }
    val verified = biosStatus.values.count { it == BiosManager.Status.Verified }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { LargeTitle("Settings") }

        item {
            GroupedSection(header = "Library") {
                folders.forEach { uri ->
                    row {
                        val name = remember(uri) { runCatching { Saf.folderName(Uri.parse(uri)) }.getOrDefault(uri) }
                        IosRow(name, icon = { IconTile(Icons.Rounded.Folder, IosColors.Blue) }) {
                            Icon(
                                Icons.Rounded.Close,
                                "Remove $name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp).clickableCircle { onRemoveFolder(uri) }.padding(6.dp),
                            )
                        }
                    }
                }
                if (scanning) {
                    row {
                        Column(Modifier.padding(16.dp)) {
                            Text("Scanning $scanProgress…", style = MaterialTheme.typography.bodyLarge)
                            LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
                        }
                    }
                }
                row {
                    IosRow("Add Folder", titleColor = MaterialTheme.colorScheme.primary, icon = { IconTile(Icons.Rounded.Add, IosColors.Green) }, onClick = onAddFolder)
                }
                if (folders.isNotEmpty()) {
                    row {
                        IosRow(
                            "Rescan Library",
                            titleColor = if (scanning) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            icon = { IconTile(Icons.Rounded.Refresh, IosColors.Indigo) },
                            onClick = if (scanning) null else onRescan,
                        )
                    }
                }
            }
        }

        item {
            GroupedSection(header = "Look & Feel") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    toggle(Icons.Rounded.Palette, IosColors.Purple, "Wallpaper Accent", dynamicColor) { settings.setDynamicColor(it) }
                }
                toggle(Icons.Rounded.GridOn, IosColors.Orange, "Sharp Pixels", settings.sharpPixels) { settings.sharpPixels = it }
            }
        }

        item {
            GroupedSection(header = "Controls", footer = "The left stick option lets games without analog support be played with a stick.") {
                toggle(Icons.Rounded.Vibration, IosColors.Pink, "Haptic Feedback", settings.haptics) { settings.haptics = it }
                toggle(Icons.Rounded.Gamepad, IosColors.Teal, "Left Stick Moves D-pad", settings.stickAsDpad) { settings.stickAsDpad = it }
                row {
                    SliderRow(Icons.Rounded.Opacity, IosColors.Gray, "Touch Controls", settings.overlayOpacity, 0.2f..1f, { "${(it * 100).toInt()}%" }) {
                        settings.overlayOpacity = it
                    }
                }
                row {
                    SliderRow(Icons.Rounded.FastForward, IosColors.Red, "Fast-Forward", settings.fastForwardSpeed, 2f..8f, { "${it.toInt()}×" }, steps = 5) {
                        settings.fastForwardSpeed = it
                    }
                }
            }
        }

        item {
            GroupedSection(header = "Saves", footer = "Saves your place when you quit and resumes it next time.") {
                toggle(Icons.Rounded.Save, IosColors.Green, "Auto Save & Resume", settings.autoSave) { settings.autoSave = it }
            }
        }

        item {
            GroupedSection(
                header = "BIOS · $verified verified",
                footer = "BIOS files come from your own consoles. Each file is identified by its checksum, so any file name works.",
            ) {
                biosFiles.forEach { (system, file) ->
                    row {
                        val status = biosStatus[file.fileName] ?: BiosManager.Status.Missing
                        val (icon, color) = when {
                            status == BiosManager.Status.Verified -> Icons.Rounded.CheckCircle to IosColors.Green
                            status == BiosManager.Status.Unverified -> Icons.Rounded.HelpOutline to IosColors.Orange
                            file.required -> Icons.Rounded.ErrorOutline to IosColors.Red
                            else -> Icons.Rounded.Memory to IosColors.Gray
                        }
                        val note = when (status) {
                            BiosManager.Status.Verified -> "Verified"
                            BiosManager.Status.Unverified -> "Checksum doesn't match"
                            BiosManager.Status.Missing -> if (file.required) "Required" else "Optional"
                        }
                        IosRow(
                            file.fileName,
                            subtitle = "${system.shortName} · ${file.description}",
                            icon = { IconTile(icon, color) },
                        ) {
                            if (status != BiosManager.Status.Missing) {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Delete ${file.fileName}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp).clickableCircle { onDeleteBios(file.fileName) }.padding(6.dp),
                                )
                            } else {
                                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                row {
                    IosRow("Download BIOS…", titleColor = MaterialTheme.colorScheme.primary, icon = { IconTile(Icons.Rounded.Download, IosColors.Blue) }, onClick = onDownloadBios)
                }
                row {
                    IosRow("Import from Files…", titleColor = MaterialTheme.colorScheme.primary, icon = { IconTile(Icons.Rounded.FolderOpen, IosColors.Cyan) }, onClick = onImportBios)
                }
            }
        }

        item {
            GroupedSection(header = "About", footer = "RetroHero includes no games or BIOS files. Play only games you own.") {
                row {
                    IosRow("Version", icon = { IconTile(Icons.Rounded.Info, IosColors.Gray) }) { TrailingValue(BuildConfig.VERSION_NAME) }
                }
                row {
                    IosRow("Emulator Architecture", icon = { IconTile(Icons.Rounded.Memory, IosColors.Indigo) }) { TrailingValue(CoreManager.abi()) }
                }
                row {
                    IosRow("Powered by libretro", icon = { IconTile(Icons.Rounded.AutoAwesome, IosColors.Pink) })
                }
            }
        }
    }
}

private fun GroupBuilder.toggle(icon: ImageVector, color: Color, title: String, initial: Boolean, onChange: (Boolean) -> Unit) = row {
    var checked by remember { mutableStateOf(initial) }
    IosRow(title, icon = { IconTile(icon, color) }, onClick = { checked = !checked; onChange(checked) }) {
        IosSwitch(checked, { checked = it; onChange(it) })
    }
}

@Composable
private fun SliderRow(
    icon: ImageVector,
    color: Color,
    title: String,
    initial: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(initial) }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, color)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TrailingValue(format(value))
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onChange(value) },
            valueRange = range,
            steps = steps,
            modifier = Modifier.padding(start = 44.dp),
        )
    }
}

private fun Modifier.clickableCircle(onClick: () -> Unit): Modifier =
    this.then(Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onClick))
