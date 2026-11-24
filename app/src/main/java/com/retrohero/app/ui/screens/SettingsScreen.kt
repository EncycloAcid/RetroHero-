package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.LocalNavSpace
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import com.retrohero.app.ui.components.MonoSwitch
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
    onOpenControllers: () -> Unit = {},
    remapCount: Int = 0,
    onBackUp: (includeBios: Boolean) -> Unit = {},
    onRestore: () -> Unit = {},
    looks: List<com.retrohero.app.data.Look> = emptyList(),
    onImportLook: () -> Unit = {},
    onLookChanged: () -> Unit = {},
    deltaSkins: List<com.retrohero.app.data.DeltaSkin> = emptyList(),
    onImportDeltaSkin: () -> Unit = {},
    onDeleteDeltaSkin: (com.retrohero.app.data.DeltaSkin) -> Unit = {},
    pageIndex: Int = 4,
    onOpenStats: () -> Unit = {},
    coreStates: Map<String, CoreManager.CoreState> = emptyMap(),
    onDownloadCore: (com.retrohero.app.data.CoreInfo) -> Unit = {},
    onDeleteCore: (com.retrohero.app.data.CoreInfo) -> Unit = {},
) {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }
    val installedCoresCount = coreStates.values.count { it is CoreManager.CoreState.Installed }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState != null) (slideInHorizontally(tween(420, easing = MochyEase)) { it / 3 } + fadeIn(tween(260))) togetherWith fadeOut(tween(160))
            else fadeIn(tween(260)) togetherWith (slideOutHorizontally(tween(320, easing = MochyEase)) { it / 3 } + fadeOut(tween(200)))
        },
        label = "settingsPage",
    ) { p ->
        when (p) {
            null -> SettingsHome(
                settings = settings,
                onOpen = { page = it },
                onOpenStats = onOpenStats,
                pageIndex = pageIndex,
                installedCoresCount = installedCoresCount,
                totalCoresCount = Systems.allCores().size,
            )
            "cores" -> Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(onClick = { page = null }) {
                        Text("← System")
                    }
                }
                CoresScreen(coreStates, onDownloadCore, onDeleteCore, pageIndex = pageIndex)
            }
            "library" -> SubSettings("Game folders", onBack = { page = null }) {
        // ---- Library -------------------------------------------------------------------
        Column {
            val folderList = folders.toList()
            SegmentedGroup(null, folderList.size + if (scanning) 1 else 0) {
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
                com.retrohero.app.ui.components.AccentButton("Add folder", onAddFolder, icon = Icons.Outlined.Add)
                com.retrohero.app.ui.components.Key("Rescan", onRescan, enabled = !scanning && folders.isNotEmpty(), icon = Icons.Outlined.Refresh)
            }
        }

            }
            "gameplay" -> SubSettings("Gameplay & comfort", onBack = { page = null }) {
        // ---- Appearance & controls ------------------------------------------------------
        SegmentedGroup("Comfort", 2) {
            ToggleRow(shapes(0), Icons.Outlined.Animation, "Reduce motion", "Calmer animations; no tilting or spinning", settings.reducedMotion.value) { settings.setReducedMotion(it) }
            ToggleRow(shapes(1), Icons.Outlined.ScreenRotation, "Tilt effects", "Game boxes on the Shelf move as you tilt your phone", settings.tiltEffects.value) { settings.setTiltEffects(it) }
        }
        SegmentedGroup("Playing", 7) {
            var i = 0
            ToggleRow(shapes(i++), Icons.Outlined.GridOn, "Sharp pixels", "Default look; pick CRT or LCD per system in-game", settings.sharpPixels) { settings.sharpPixels = it }
            ToggleRow(shapes(i++), Icons.Outlined.Vibration, "Haptic feedback", "Buzz when touch buttons are pressed", settings.haptics) { settings.haptics = it }
            ToggleRow(shapes(i++), Icons.Outlined.Gamepad, "Stick moves D-pad", "Left stick acts as D-pad on controllers", settings.stickAsDpad) { settings.stickAsDpad = it }
            SliderRow(shapes(i++), Icons.Outlined.Opacity, "Touch controls opacity", settings.overlayOpacity, 0.2f..1f, { "${(it * 100).toInt()}%" }) {
                settings.overlayOpacity = it
            }
            SliderRow(shapes(i++), Icons.Outlined.FastForward, "Fast-forward speed", settings.fastForwardSpeed, 2f..8f, { "${it.toInt()}×" }, steps = 5) {
                settings.fastForwardSpeed = it
            }
            ToggleRow(shapes(i++), Icons.Outlined.Save, "Auto save & resume", "Pick up exactly where you left off", settings.autoSave) { settings.autoSave = it }
            ToggleRow(shapes(i), Icons.Outlined.PhotoLibrary, "Screenshots to Gallery", "Also save in-game screenshots to Pictures", settings.screenshotsToGallery) { settings.screenshotsToGallery = it }
        }

            }
            "controls" -> SubSettings("Controls", onBack = { page = null }) {
        // ---- Controllers ------------------------------------------------------------------
        SegmentedGroup(null, 1) {
            SegmentedListItem(
                onClick = onOpenControllers,
                shapes = shapes(0),
                colors = segmentColors(),
                leadingContent = { ShapeIcon(Icons.Outlined.SportsEsports) },
                supportingContent = {
                    Text(if (remapCount == 0) "Rebind buttons, rewind and fast forward" else "$remapCount ${if (remapCount == 1) "button" else "buttons"} changed")
                },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                verticalAlignment = Alignment.CenterVertically,
            ) { Text("Controller buttons") }
        }
        Text(
            "To move or resize the touch buttons, open the pause menu in a game and choose Edit controls.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

            }
            "looks" -> SubSettings("Customisation", onBack = { page = null }) {
                LooksSettings(
                    settings = settings,
                    imported = looks,
                    onImport = onImportLook,
                    onChanged = onLookChanged,
                    deltaSkins = deltaSkins,
                    onImportDeltaSkin = onImportDeltaSkin,
                    onDeleteDeltaSkin = onDeleteDeltaSkin,
                )
            }
            "backup" -> SubSettings("Backup & restore", onBack = { page = null }) {
        // ---- Backup -----------------------------------------------------------------------
        Column {
            var includeBios by remember { mutableStateOf(false) }
            SegmentedGroup(null, 1) {
                ToggleRow(shapes(0), Icons.Outlined.Memory, "Include BIOS files", "Bigger backup; handy when moving to a new phone", includeBios) { includeBios = it }
            }
            Text(
                "Saves your progress, save states, screenshots, cheats, play time, collections and settings to one file. Games and emulators aren't included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.retrohero.app.ui.components.AccentButton("Back up now", { onBackUp(includeBios) }, icon = Icons.Outlined.Backup)
                com.retrohero.app.ui.components.Key("Restore", onRestore, icon = Icons.Outlined.Restore)
            }
        }

            }
            "bios" -> SubSettings("BIOS & system files", onBack = { page = null }) {
        // ---- BIOS -------------------------------------------------------------------------
        Column {
            key(biosVersion) {
                val files = Systems.all.flatMap { s -> s.bios.map { s to it } }
                SegmentedGroup(null, files.size) {
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
                com.retrohero.app.ui.components.AccentButton("Download", onDownloadBios, icon = Icons.Outlined.Download)
                com.retrohero.app.ui.components.Key("Import files", onImportBios, icon = Icons.Outlined.Memory)
            }
        }

            }
            else -> SubSettings("About", onBack = { page = null }) {
        // ---- About ------------------------------------------------------------------------
        SegmentedGroup(null, 2) {
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
            MonoSwitch(checked, { checked = it; onChange(it) })
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
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = com.retrohero.app.ui.components.Ascend.accent,
                    activeTrackColor = com.retrohero.app.ui.components.Ascend.accent,
                    inactiveTrackColor = com.retrohero.app.ui.theme.Tone.Ink.copy(alpha = 0.14f),
                    activeTickColor = com.retrohero.app.ui.components.Ascend.onAccent.copy(alpha = 0.7f),
                    inactiveTickColor = com.retrohero.app.ui.theme.Tone.Muted.copy(alpha = 0.55f),
                ),
            )
        },
    ) { Text(title) }
}
