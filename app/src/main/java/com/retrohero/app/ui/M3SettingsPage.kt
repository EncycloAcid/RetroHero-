package com.retrohero.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3ConnectedButtonGroup
import com.retrohero.app.ui.components.M3ExpressiveSpring
import com.retrohero.app.ui.components.M3SegmentedItem
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.M3WavyBar
import com.retrohero.app.ui.components.MorphShape
import com.retrohero.app.ui.components.m3SegmentedShape
import com.retrohero.app.ui.components.m3StaggeredEntrance
import com.retrohero.app.ui.components.rememberMorph
import com.retrohero.app.ui.theme.M3ExpressivePalette
import com.retrohero.app.ui.theme.M3ExpressivePalettes
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.currentPaletteId

private val M3SpeedOptions = listOf(2f to "2×", 3f to "3×", 4f to "4×", 8f to "8×")

@Composable
internal fun M3SettingsPage(
    gamesCount: Int,
    foldersCount: Int,
    scanning: Boolean,
    autoSave: Boolean,
    haptics: Boolean,
    fastForwardSpeed: Float,
    dynamicColor: Boolean,
    coreStates: Map<String, CoreState>,
    onSetAutoSave: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetFastForward: (Float) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSelectPalette: (String) -> Unit,
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

    AnimatedContent(
        targetState = coresOpen,
        transitionSpec = {
            val slideSpec = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 360f)
            if (targetState) {
                (slideInHorizontally(slideSpec) { it / 3 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { -it / 6 } + fadeOut(tween(140)))
            } else {
                (slideInHorizontally(slideSpec) { -it / 6 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { it / 3 } + fadeOut(tween(140)))
            }
        },
        label = "m3CoreManagerPush",
    ) { showCores ->
        if (showCores) {
            M3CoreManagerPage(
                coreStates = coreStates,
                onDownloadCore = onDownloadCore,
                onDeleteCore = onDeleteCore,
                onBack = { coresOpen = false },
            )
            return@AnimatedContent
        }

        val cs = MaterialTheme.colorScheme
        val inset = LocalToolbarInset.current
        val activePaletteId = currentPaletteId()
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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. M3 Expressive Display Header
            item(key = "m3_set_header") {
                Column(Modifier.m3StaggeredEntrance(0, slideDp = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(text = "Engine · $abi", style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer)
                    }
                    Text(text = "Settings", style = MaterialTheme.typography.displayMedium, color = cs.onBackground)
                }
            }

        // 2. Material You Expressive Theme Studio
        item(key = "m3_set_theme") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.surfaceContainerHigh)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    M3ShapeBadge(
                        polygon = MaterialShapes.Flower,
                        containerColor = cs.primaryContainer,
                        size = 40.dp,
                    ) {
                        Icon(Icons.Rounded.Palette, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Theme & color", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text("Material 3 Expressive tonal palettes", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }

                M3ConnectedButtonGroup(
                    options = listOf("Dark", "Light"),
                    selectedIndex = if (Tone.isDark) 0 else 1,
                    onSelect = { onSetDarkMode(it == 0) },
                    height = 42.dp,
                    useSecondaryTone = false,
                    unselectedContainerColor = cs.surfaceContainer,
                )

                // Tri-tone Material You Palette Swatches (selected morphs to Cookie9Sided!)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    M3ExpressivePalettes.forEach { pal ->
                        val selected = !dynamicColor && activePaletteId == pal.id
                        MaterialYouTriSwatch(
                            palette = pal,
                            selected = selected,
                            onClick = {
                                if (dynamicColor) onSetDynamicColor(false)
                                onSelectPalette(pal.id)
                            },
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cs.surfaceContainer)
                            .clickable { onSetDynamicColor(!dynamicColor) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Wallpaper, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Wallpaper dynamic color", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                            Text("Match your Android system palette", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        M3ExpressiveSwitch(checked = dynamicColor, onCheckedChange = onSetDynamicColor)
                    }
                }
            }
        }

        // 3. 2x2 Bold Tonal Color-Block Action Hub (primaryContainer, secondaryContainer, tertiaryContainer, surfaceContainerHighest)
        item(key = "m3_set_hub") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    M3TonalHubCard(
                        icon = Icons.Rounded.Memory,
                        polygon = MaterialShapes.Sunny,
                        containerColor = cs.primaryContainer,
                        contentColor = cs.onPrimaryContainer,
                        title = "$installedCores / ${allCores.size}",
                        subtitle = "Emulator cores",
                        onClick = { coresOpen = true },
                        modifier = Modifier.weight(1f),
                    )
                    M3TonalHubCard(
                        icon = Icons.Rounded.CreateNewFolder,
                        polygon = MaterialShapes.Clover4Leaf,
                        containerColor = cs.secondaryContainer,
                        contentColor = cs.onSecondaryContainer,
                        title = "ROM folders",
                        subtitle = "$foldersCount · $gamesCount ROMs",
                        onClick = onAddFolder,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    M3TonalHubCard(
                        icon = Icons.Rounded.AutoAwesome,
                        polygon = MaterialShapes.Cookie7Sided,
                        containerColor = cs.tertiaryContainer,
                        contentColor = cs.onTertiaryContainer,
                        title = "Homebrew",
                        subtitle = "Free classics",
                        onClick = onOpenHomebrew,
                        modifier = Modifier.weight(1f),
                    )
                    M3TonalHubCard(
                        icon = Icons.Rounded.SportsEsports,
                        polygon = MaterialShapes.Gem,
                        containerColor = cs.surfaceContainerHighest,
                        contentColor = cs.onSurface,
                        title = "Delta skins",
                        subtitle = "Import .deltaskin",
                        onClick = onImportSkin,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "m3_set_wave") {
            M3WavyBar(color = cs.outlineVariant.copy(alpha = 0.6f), amplitude = 2.dp, wavelength = 24.dp, strokeWidth = 2.dp)
        }

        // 4. Android 16 Segmented Settings Group — Gameplay (separate stacked cards, 3dp gaps!)
        item(key = "m3_set_gameplay") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Gameplay",
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    M3SegmentedItem(
                        index = 0,
                        count = 3,
                        icon = Icons.Rounded.Save,
                        badgePolygon = MaterialShapes.Cookie4Sided,
                        badgeContainer = cs.primaryContainer,
                        badgeContent = cs.onPrimaryContainer,
                        title = "Auto-save & instant resume",
                        subtitle = "Save state automatically on exit",
                        trailing = { M3ExpressiveSwitch(checked = autoSave, onCheckedChange = onSetAutoSave) },
                    )
                    M3SegmentedItem(
                        index = 1,
                        count = 3,
                        icon = Icons.Rounded.Vibration,
                        badgePolygon = MaterialShapes.Puffy,
                        badgeContainer = cs.tertiaryContainer,
                        badgeContent = cs.onTertiaryContainer,
                        title = "Tactile haptics",
                        subtitle = "Controller & UI button detents",
                        trailing = { M3ExpressiveSwitch(checked = haptics, onCheckedChange = onSetHaptics) },
                    )
                    // Bottom segmented card: Fast-forward Connected Button Group
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(m3SegmentedShape(2, 3))
                            .background(cs.surfaceContainerHigh)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            M3ShapeBadge(
                                polygon = MaterialShapes.SoftBurst,
                                containerColor = cs.secondaryContainer,
                                size = 40.dp,
                            ) {
                                Icon(Icons.Rounded.FastForward, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Text("Fast-forward speed", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.onSurface)
                        }
                        val selIdx = M3SpeedOptions.indexOfFirst { it.first == fastForwardSpeed }.coerceAtLeast(0)
                        M3ConnectedButtonGroup(
                            options = M3SpeedOptions.map { it.second },
                            selectedIndex = selIdx,
                            onSelect = { onSetFastForward(M3SpeedOptions[it].first) },
                            height = 40.dp,
                            unselectedContainerColor = cs.surfaceContainer,
                        )
                    }
                }
            }
        }

        // 5. Android 16 Segmented Settings Group — Library & Firmware
        item(key = "m3_set_firmware") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Library & firmware",
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    M3SegmentedItem(
                        index = 0,
                        count = 3,
                        icon = Icons.Rounded.Link,
                        badgePolygon = MaterialShapes.Cookie6Sided,
                        badgeContainer = cs.primaryContainer,
                        badgeContent = cs.onPrimaryContainer,
                        title = "Download via HTTPS link",
                        subtitle = "Fetch a ROM file or folder",
                        onClick = onOpenDownload,
                    )
                    M3SegmentedItem(
                        index = 1,
                        count = 3,
                        icon = Icons.Rounded.Memory,
                        badgePolygon = MaterialShapes.Pentagon,
                        badgeContainer = cs.secondaryContainer,
                        badgeContent = cs.onSecondaryContainer,
                        title = "Import BIOS files",
                        subtitle = "PlayStation, Sega CD & Lynx firmware",
                        onClick = onImportBios,
                    )
                    M3SegmentedItem(
                        index = 2,
                        count = 3,
                        icon = Icons.Rounded.Refresh,
                        badgePolygon = MaterialShapes.Clover4Leaf,
                        badgeContainer = cs.tertiaryContainer,
                        badgeContent = cs.onTertiaryContainer,
                        title = if (scanning) "Scanning folders…" else "Rescan library",
                        subtitle = "Refresh ROMs & cover artwork",
                        onClick = onRescan,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun MaterialYouTriSwatch(
    palette: M3ExpressivePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, M3ExpressiveSpring, label = "swatchPress")

    // Signature M3 Expressive real polygon morph: Circle <-> Cookie9Sided with bouncy overshoot!
    val morph = rememberMorph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided)
    val morphProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 360f),
        label = "swatchMorph",
    )
    val swatchSize by animateDpAsState(
        targetValue = if (selected) 54.dp else 46.dp,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 400f),
        label = "swatchSize",
    )
    val swatchRot by animateFloatAsState(
        targetValue = if (selected) 36f else 0f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 320f),
        label = "swatchRot",
    )
    val shape = MorphShape(morph, morphProgress.coerceIn(0f, 1f))
    val scheme = if (Tone.isDark) palette.dark else palette.light

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            },
    ) {
        Box(
            Modifier.size(54.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(swatchSize)
                    .graphicsLayer { rotationZ = swatchRot }
                    .clip(shape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(color = scheme.primary, topLeft = Offset.Zero, size = Size(w, h * 0.52f))
                    drawRect(color = scheme.secondaryContainer, topLeft = Offset(0f, h * 0.52f), size = Size(w * 0.5f, h * 0.48f))
                    drawRect(color = scheme.tertiaryContainer, topLeft = Offset(w * 0.5f, h * 0.52f), size = Size(w * 0.5f, h * 0.48f))
                }
            }
            val checkScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.46f, stiffness = 500f),
                label = "swatchCheckScale",
            )
            if (checkScale > 0.02f) {
                Box(
                    Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                            alpha = checkScale.coerceIn(0f, 1f)
                        }
                        .clip(CircleShape)
                        .background(scheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = palette.name, tint = scheme.primary, modifier = Modifier.size(15.dp))
                }
            }
        }
        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Black else FontWeight.Medium),
            color = if (selected) cs.primary else cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun M3TonalHubCard(
    icon: ImageVector,
    polygon: RoundedPolygon,
    containerColor: Color,
    contentColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(26.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, M3ExpressiveSpring, label = "hubScale")
    val badgeSpin by animateFloatAsState(if (pressed) -14f else 0f, M3ExpressiveSpring, label = "hubSpin")

    val polyShape = polygon.toShape()

    Box(
        modifier
            .height(126.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(containerColor)
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            },
    ) {
        // Oversized sculptural MaterialShapes watermark peeking from lower-right corner
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 22.dp, y = 22.dp)
                .size(86.dp)
                .graphicsLayer { rotationZ = badgeSpin * -0.8f }
                .clip(polyShape)
                .background(contentColor.copy(alpha = 0.08f)),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            M3ShapeBadge(
                polygon = polygon,
                containerColor = contentColor.copy(alpha = 0.16f),
                size = 42.dp,
                modifier = Modifier.graphicsLayer { rotationZ = badgeSpin },
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(21.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun M3ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun M3CoreManagerPage(
    coreStates: Map<String, CoreState>,
    onDownloadCore: (CoreInfo) -> Unit,
    onDeleteCore: (CoreInfo) -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    val allCores = remember { Systems.allCores() }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "m3_cores_top") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = cs.onSecondaryContainer, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Libretro engines", style = MaterialTheme.typography.labelSmall, color = cs.primary)
                    Text("Emulator cores", style = MaterialTheme.typography.headlineMedium, color = cs.onBackground)
                }
            }
        }

        item(key = "m3_cores_segmented") {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                allCores.forEachIndexed { idx, (core, systems) ->
                    val state = coreStates[core.id] ?: CoreState.NotInstalled
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(m3SegmentedShape(idx, allCores.size))
                            .background(cs.surfaceContainerHigh)
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(core.displayName, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.onSurface, maxLines = 1)
                            Text(
                                text = systems.joinToString(" · ") { it.shortName },
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when (state) {
                            is CoreState.Installed -> {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(cs.errorContainer)
                                        .clickable { onDeleteCore(core) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${core.displayName}", tint = cs.onErrorContainer, modifier = Modifier.size(19.dp))
                                }
                            }
                            is CoreState.Downloading -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.6.dp)
                            }
                            else -> {
                                Row(
                                    Modifier
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(cs.primaryContainer)
                                        .clickable { onDownloadCore(core) }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                    Text("Install", style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
