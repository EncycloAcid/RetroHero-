package com.retrohero.app.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.ExpressiveConnectedBar
import com.retrohero.app.ui.components.ExpressiveShapeBadge
import com.retrohero.app.ui.components.IosGroupedSection
import com.retrohero.app.ui.components.IosInsetRow
import com.retrohero.app.ui.components.LocalDockInset
import com.retrohero.app.ui.components.MonoSwitch
import com.retrohero.app.ui.theme.ExpressiveAccents
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

private val SpeedOptions = listOf(2f to "2×", 3f to "3×", 4f to "4×", 8f to "8×")

@Composable
internal fun SettingsDeckPage(
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
    onSetDarkMode: (Boolean) -> Unit,
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
        CoreManagerSheetPage(
            coreStates = coreStates,
            onDownloadCore = onDownloadCore,
            onDeleteCore = onDeleteCore,
            onBack = { coresOpen = false },
        )
        return
    }

    val view = LocalView.current
    val dockInset = LocalDockInset.current
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = dockInset + 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. iOS Large Title Header
        item(key = "set_header") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "RETROHERO ENGINE · $abi",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = "Settings",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-0.035).em),
                        color = Tone.Ink,
                    )
                }
            }
        }

        // 2. M3 Expressive Appearance & Theme Card
        item(key = "set_appearance") {
            val shape = RoundedCornerShape(26.dp)
            val cookieShape = MaterialShapes.Cookie6Sided.toShape()
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(Tone.tonalSurface(Ascend.accent), Tone.Surface),
                        ),
                    )
                    .border(1.dp, Tone.glassBorder, shape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveShapeBadge(
                            polygon = MaterialShapes.Flower,
                            containerColor = Ascend.accent.copy(alpha = 0.20f),
                            size = 34.dp,
                        ) {
                            Icon(Icons.Rounded.Palette, null, tint = Ascend.accent, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Appearance",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
                                color = Tone.Ink,
                            )
                            Text(
                                text = "M3 Expressive palette & mode",
                                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                                color = Tone.Muted,
                            )
                        }
                    }
                }

                ExpressiveConnectedBar(
                    options = listOf("Dark", "Light"),
                    selectedIndex = if (Tone.isDark) 0 else 1,
                    onSelect = { idx -> onSetDarkMode(idx == 0) },
                    height = 38.dp,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveAccents.forEach { swatch ->
                        val selected = Salmon == swatch.color
                        val swatchShape = if (selected) cookieShape else CircleShape
                        Box(
                            Modifier
                                .size(if (selected) 42.dp else 36.dp)
                                .clip(swatchShape)
                                .background(swatch.color)
                                .then(
                                    if (selected) Modifier.border(2.dp, Tone.Ink, swatchShape)
                                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.22f), swatchShape),
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onSelectAccent(swatch.id, swatch.color)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = swatch.name,
                                    tint = if (swatch.color.luminance() > 0.56f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 2x2 Expressive Tonal Bento Tiles
        item(key = "set_bento") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveBentoAction(
                        icon = Icons.Rounded.Memory,
                        polygon = MaterialShapes.Sunny,
                        tint = Color(0xFF6D67FF),
                        title = "$installedCores / ${allCores.size}",
                        subtitle = "Emulator Cores",
                        onClick = { coresOpen = true },
                        modifier = Modifier.weight(1f),
                    )
                    ExpressiveBentoAction(
                        icon = Icons.Rounded.CreateNewFolder,
                        polygon = MaterialShapes.Clover4Leaf,
                        tint = Color(0xFF0A84FF),
                        title = "ROM Folders",
                        subtitle = "$foldersCount ${if (foldersCount == 1) "folder" else "folders"} · $gamesCount ROMs",
                        onClick = onAddFolder,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveBentoAction(
                        icon = Icons.Rounded.AutoAwesome,
                        polygon = MaterialShapes.Cookie7Sided,
                        tint = Color(0xFF25C990),
                        title = "Free Homebrew",
                        subtitle = "One-tap classics",
                        onClick = onOpenHomebrew,
                        modifier = Modifier.weight(1f),
                    )
                    ExpressiveBentoAction(
                        icon = Icons.Rounded.SportsEsports,
                        polygon = MaterialShapes.Gem,
                        tint = Color(0xFFE052B8),
                        title = "Delta Skins",
                        subtitle = "Import .deltaskin",
                        onClick = onImportSkin,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 4. iOS Grouped Inset Card — Gameplay & Comfort
        item(key = "set_gameplay") {
            IosGroupedSection(title = "Gameplay & Comfort") {
                IosInsetRow(
                    icon = Icons.Rounded.Save,
                    badgePolygon = MaterialShapes.Cookie4Sided,
                    badgeTint = Color(0xFF0A84FF),
                    title = "Auto-save & instant resume",
                    subtitle = "Save automatically on exit",
                    trailing = { MonoSwitch(checked = autoSave, onCheckedChange = onSetAutoSave) },
                )
                IosInsetRow(
                    icon = Icons.Rounded.Vibration,
                    badgePolygon = MaterialShapes.Puffy,
                    badgeTint = Color(0xFFFF5A5F),
                    title = "Tactile haptics",
                    subtitle = "Button clicks & UI detents",
                    trailing = { MonoSwitch(checked = haptics, onCheckedChange = onSetHaptics) },
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExpressiveShapeBadge(
                            polygon = MaterialShapes.SoftBurst,
                            containerColor = Color(0xFFFF7A2F).copy(alpha = if (Tone.isDark) 0.20f else 0.14f),
                            size = 34.dp,
                        ) {
                            Icon(Icons.Rounded.FastForward, null, tint = Color(0xFFFF7A2F), modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Fast-forward speed",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                            color = Tone.Ink,
                        )
                    }
                    val selIdx = SpeedOptions.indexOfFirst { it.first == fastForwardSpeed }.coerceAtLeast(0)
                    ExpressiveConnectedBar(
                        options = SpeedOptions.map { it.second },
                        selectedIndex = selIdx,
                        onSelect = { onSetFastForward(SpeedOptions[it].first) },
                        height = 36.dp,
                    )
                }
            }
        }

        // 5. iOS Grouped Inset Card — Firmware & Data
        item(key = "set_data") {
            IosGroupedSection(title = "Library & Firmware") {
                IosInsetRow(
                    icon = Icons.Rounded.Link,
                    badgePolygon = MaterialShapes.Cookie6Sided,
                    badgeTint = Color(0xFF6D67FF),
                    title = "Download via HTTPS Link",
                    subtitle = "Fetch a ROM or folder directly",
                    onClick = onOpenDownload,
                )
                IosInsetRow(
                    icon = Icons.Rounded.Memory,
                    badgePolygon = MaterialShapes.Pentagon,
                    badgeTint = Color(0xFF25C990),
                    title = "Import BIOS Files",
                    subtitle = "PlayStation, Sega CD & Lynx firmware",
                    onClick = onImportBios,
                )
                IosInsetRow(
                    icon = Icons.Rounded.Refresh,
                    badgePolygon = MaterialShapes.Clover4Leaf,
                    badgeTint = Color(0xFFFF7A2F),
                    title = if (scanning) "Scanning folders…" else "Rescan Library",
                    subtitle = "Refresh indexed ROMs & artwork",
                    showDivider = false,
                    onClick = onRescan,
                )
            }
        }
    }
}

@Composable
private fun ExpressiveBentoAction(
    icon: ImageVector,
    polygon: RoundedPolygon,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .height(114.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = if (Tone.isDark) 0.16f else 0.11f), Tone.Surface),
                ),
            )
            .border(1.dp, Tone.glassBorder, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ExpressiveShapeBadge(
            polygon = polygon,
            containerColor = tint.copy(alpha = if (Tone.isDark) 0.24f else 0.16f),
            size = 38.dp,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = (-0.01).em),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                color = Tone.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CoreManagerSheetPage(
    coreStates: Map<String, CoreState>,
    onDownloadCore: (CoreInfo) -> Unit,
    onDeleteCore: (CoreInfo) -> Unit,
    onBack: () -> Unit,
) {
    val dockInset = LocalDockInset.current
    val allCores = remember { Systems.allCores() }
    val cardShape = RoundedCornerShape(24.dp)

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = dockInset + 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "cores_back") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.glassBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Tone.Ink, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "LIBRETRO ENGINES",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = "Emulator Cores",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = (-0.03).em),
                        color = Tone.Ink,
                    )
                }
            }
        }

        item(key = "cores_list") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.glassBorder, cardShape),
            ) {
                allCores.forEachIndexed { idx, (core, systems) ->
                    val state = coreStates[core.id] ?: CoreState.NotInstalled
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = core.displayName,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                                color = Tone.Ink,
                                maxLines = 1,
                            )
                            Text(
                                text = systems.joinToString(" · ") { it.shortName },
                                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                                color = Tone.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when (state) {
                            is CoreState.Installed -> {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5A5F).copy(alpha = 0.16f))
                                        .clickable { onDeleteCore(core) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${core.displayName}", tint = Color(0xFFFF5A5F), modifier = Modifier.size(18.dp))
                                }
                            }
                            is CoreState.Downloading -> {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.4.dp, color = Ascend.accent)
                            }
                            else -> {
                                Box(
                                    Modifier
                                        .height(34.dp)
                                        .clip(CircleShape)
                                        .background(Tone.tonalSurface(Ascend.accent))
                                        .clickable { onDownloadCore(core) }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(Icons.Rounded.CloudDownload, null, tint = Ascend.accent, modifier = Modifier.size(15.dp))
                                        Text("Get", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Ascend.accent)
                                    }
                                }
                            }
                        }
                    }
                    if (idx < allCores.lastIndex) {
                        HorizontalDivider(thickness = 0.75.dp, color = Tone.Line, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
