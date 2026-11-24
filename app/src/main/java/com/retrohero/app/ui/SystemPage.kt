package com.retrohero.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.InterDisplay
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

private val AccentPresets = listOf(
    "ember" to Color(0xFFFF6B24),
    "lime" to Color(0xFFD7FA3C),
    "deck" to Color(0xFF1AA8FF),
    "crimson" to Color(0xFFFF385C),
    "mint" to Color(0xFF3FD8A4),
)

/**
 * Distinct Hardware & Emulation Control Deck Page:
 * 1) Dual Top Quick-Deck Tiles: Live Theme + 5-Swatch Accent Picker & Libretro Core Engine Tile (opens full Core Manager)
 * 2) Live Emulation Comfort Panel: Auto-save switch, Haptics switch, and 2x-8x Fast-Forward speed pills
 * 3) 2x2 Storage, Firmware & Skins Bento Matrix (ROM Folders, BIOS, Delta Skins, Free Homebrew & Link Download)
 */
@Composable
internal fun SystemControlDeckPage(
    gamesCount: Int,
    foldersCount: Int,
    scanning: Boolean,
    autoSave: Boolean,
    haptics: Boolean,
    fastForwardSpeed: Float,
    coreStates: Map<String, CoreState>,
    onSetAutoSave: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetFastForward: (Float) -> Unit,
    onSelectAccent: (String, Color) -> Unit,
    onToggleTheme: () -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onImportBios: () -> Unit,
    onImportSkin: () -> Unit,
    onDownloadCore: (CoreInfo) -> Unit,
    onDeleteCore: (CoreInfo) -> Unit,
) {
    var coresOpen by rememberSaveable { mutableStateOf(false) }
    if (coresOpen) {
        CoreManagerSubPage(
            coreStates = coreStates,
            onDownloadCore = onDownloadCore,
            onDeleteCore = onDeleteCore,
            onBack = { coresOpen = false },
        )
        return
    }

    val navSpace = LocalNavSpace.current
    val allCores = remember { Systems.allCores() }
    val installedCores = remember(coreStates, allCores) {
        allCores.count { coreStates[it.first.id] is CoreState.Installed }
    }
    val abi = remember {
        when (val raw = CoreManager.abi().lowercase()) {
            "arm64-v8a" -> "ARM64"
            "armeabi-v7a" -> "ARM32"
            else -> raw.uppercase()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = navSpace + 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Control Center Header
        item(key = "sys_header") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CONTROL DECK · $abi",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                    color = Ascend.accent,
                )
                Text(
                    text = "System & Engine",
                    style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.04).em),
                    color = Tone.Ink,
                )
            }
        }

        // 1. Top Dual Quick-Deck Tiles (Live Appearance Swatches + Core Engine Status)
        item(key = "sys_dual_tiles") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tile A: Theme & Accent Swatches
                val tileShape = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .height(138.dp)
                        .clip(tileShape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, tileShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Palette, null, tint = Ascend.accent, modifier = Modifier.size(19.dp))
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Tone.Raised)
                                .border(1.dp, Tone.Line, CircleShape)
                                .clickable(onClick = onToggleTheme)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (Tone.isDark) "Dark" else "Light",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                color = Tone.Ink,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Accent", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp), color = Tone.Ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            AccentPresets.forEach { (id, col) ->
                                val selected = Salmon == col
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .then(
                                            if (selected) Modifier.border(2.dp, Tone.Ink, CircleShape)
                                            else Modifier,
                                        )
                                        .clickable { onSelectAccent(id, col) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = id,
                                            tint = if (id == "lime") Color(0xFF0A0A0C) else Color.White,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tile B: Libretro Core Engine Manager Trigger
                Column(
                    Modifier
                        .weight(1f)
                        .height(138.dp)
                        .clip(tileShape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, tileShape)
                        .clickable { coresOpen = true }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Ascend.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Memory, null, tint = Ascend.accent, modifier = Modifier.size(18.dp))
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted, modifier = Modifier.size(18.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$installedCores / ${allCores.size}",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 21.sp, letterSpacing = (-0.02).em),
                            color = Tone.Ink,
                        )
                        Text(
                            text = "Emulator Cores",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                            color = Tone.Ink,
                        )
                        Text(
                            text = "Manage engines",
                            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                            color = Tone.Muted,
                        )
                    }
                }
            }
        }

        // 2. Live Emulation Comfort Controls (Switches + Fast-Forward Multiplier Pills)
        item(key = "sys_comfort") {
            val panelShape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, panelShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Gameplay & Comfort",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
                    color = Tone.Ink,
                )

                ToggleControlRow(
                    icon = Icons.Rounded.Save,
                    title = "Auto-save & instant resume",
                    subtitle = "Save state automatically on exit and resume next time",
                    checked = autoSave,
                    onCheckedChange = onSetAutoSave,
                )

                HorizontalDivider(color = Tone.Line)

                ToggleControlRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Tactile haptics",
                    subtitle = "Controller button clicks and 3D case latch feedback",
                    checked = haptics,
                    onCheckedChange = onSetHaptics,
                )

                HorizontalDivider(color = Tone.Line)

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.FastForward, null, tint = Ascend.accent, modifier = Modifier.size(19.dp))
                        Text("Fast-forward speed", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Tone.Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2f, 3f, 4f, 8f).forEach { speed ->
                            val selected = fastForwardSpeed.toInt() == speed.toInt()
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) Ascend.accent.copy(alpha = 0.18f) else Tone.Raised)
                                    .border(1.dp, if (selected) Ascend.accent else Tone.Line, RoundedCornerShape(9.dp))
                                    .clickable { onSetFastForward(speed) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${speed.toInt()}×",
                                    style = TextStyle(fontFamily = Inter, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp),
                                    color = if (selected) Ascend.accent else Tone.Ink,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 2x2 Hardware, Storage & Skins Bento Matrix
        item(key = "sys_matrix") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Library, Firmware & Skins",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
                    color = Tone.Ink,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionMatrixTile(
                        icon = Icons.Outlined.CreateNewFolder,
                        title = "ROM Folders",
                        subtitle = if (foldersCount > 0) "$foldersCount folder${if (foldersCount == 1) "" else "s"} · $gamesCount ROMs" else "Select directory",
                        onClick = onAddFolder,
                        modifier = Modifier.weight(1f),
                    )
                    ActionMatrixTile(
                        icon = Icons.Outlined.Memory,
                        title = "BIOS Files",
                        subtitle = "PS1, Sega CD & Lynx",
                        onClick = onImportBios,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionMatrixTile(
                        icon = Icons.Outlined.SportsEsports,
                        title = "Delta Skins",
                        subtitle = "Import .deltaskin shells",
                        onClick = onImportSkin,
                        modifier = Modifier.weight(1f),
                    )
                    ActionMatrixTile(
                        icon = Icons.Rounded.CardGiftcard,
                        title = "Free Homebrew",
                        subtitle = "One-tap retro games",
                        onClick = onOpenHomebrew,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionMatrixTile(
                        icon = Icons.Outlined.Download,
                        title = "Direct Link",
                        subtitle = "Fetch ROM via HTTPS",
                        onClick = onOpenDownload,
                        modifier = Modifier.weight(1f),
                    )
                    ActionMatrixTile(
                        icon = Icons.Outlined.Refresh,
                        title = if (scanning) "Scanning…" else "Rescan Vault",
                        subtitle = "Refresh indexed titles",
                        onClick = { if (!scanning) onRescan() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = Ascend.accent, modifier = Modifier.size(19.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Tone.Ink)
            Text(subtitle, style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = Tone.Muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Ascend.accent,
                uncheckedThumbColor = Tone.Muted,
                uncheckedTrackColor = Tone.Raised,
                uncheckedBorderColor = Tone.Line,
            ),
        )
    }
}

@Composable
private fun ActionMatrixTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Tone.Raised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Ascend.accent, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Tone.Ink, maxLines = 1)
            Text(subtitle, style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = Tone.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CoreManagerSubPage(
    coreStates: Map<String, CoreState>,
    onDownloadCore: (CoreInfo) -> Unit,
    onDeleteCore: (CoreInfo) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val navSpace = LocalNavSpace.current
    val cores = remember { Systems.allCores() }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = navSpace + 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "cores_back") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Tone.Ink, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Emulator Cores", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp), color = Tone.Ink)
                    Text("Cores also auto-download on first game launch", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                }
            }
        }

        items(cores, key = { it.first.id }) { (core, systems) ->
            val state = coreStates[core.id] ?: CoreState.NotInstalled
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, shape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(core.displayName, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp), color = Tone.Ink)
                    Text(
                        systems.joinToString { it.shortName },
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = Tone.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (state) {
                    is CoreState.Downloading -> CircularProgressIndicator(progress = { state.progress }, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    is CoreState.Installed -> if (!state.bundled) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Tone.Raised)
                                .clickable { onDeleteCore(core) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete ${core.displayName}", tint = Tone.Muted, modifier = Modifier.size(17.dp))
                        }
                    } else {
                        Text("Bundled", style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = Ascend.accent)
                    }
                    else -> Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Ascend.accent.copy(alpha = 0.16f))
                            .clickable { onDownloadCore(core) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = "Download ${core.displayName}", tint = Ascend.accent, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}
