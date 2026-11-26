package com.retrohero.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import com.retrohero.app.BuildConfig
import com.retrohero.app.data.BiosFile
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
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
import kotlin.math.roundToInt

private val M3SpeedOptions = listOf(2f to "2×", 3f to "3×", 4f to "4×", 8f to "8×")

private enum class SettingsSubPage { Root, Cores, Bios, Folders }

@Composable
internal fun M3SettingsPage(
    games: List<Game>,
    folders: Set<String>,
    scanning: Boolean,
    scanProgress: String,
    autoSave: Boolean,
    haptics: Boolean,
    sharpPixels: Boolean,
    overlayOpacity: Float,
    stickAsDpad: Boolean,
    screenshotsToGallery: Boolean,
    reducedMotion: Boolean,
    fastForwardSpeed: Float,
    dynamicColor: Boolean,
    coreStates: Map<String, CoreState>,
    neededCores: List<CoreInfo>,
    biosStatusOf: (BiosFile) -> BiosManager.Status,
    onSetAutoSave: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetSharpPixels: (Boolean) -> Unit,
    onSetOverlayOpacity: (Float) -> Unit,
    onSetStickAsDpad: (Boolean) -> Unit,
    onSetScreenshotsToGallery: (Boolean) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetFastForward: (Float) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSelectPalette: (String) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownloadGame: () -> Unit,
    onOpenDownloadBios: () -> Unit,
    onImportBios: () -> Unit,
    onDeleteBios: (String) -> Unit,
    onImportSkin: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDownloadCore: (CoreInfo) -> Unit,
    onDownloadAllNeededCores: () -> Unit,
    onDeleteCore: (CoreInfo) -> Unit,
) {
    var subPage by rememberSaveable { mutableStateOf(SettingsSubPage.Root) }

    AnimatedContent(
        targetState = subPage,
        transitionSpec = {
            val slideSpec = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 360f)
            val pushing = targetState != SettingsSubPage.Root
            if (pushing) {
                (slideInHorizontally(slideSpec) { it / 3 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { -it / 6 } + fadeOut(tween(140)))
            } else {
                (slideInHorizontally(slideSpec) { -it / 6 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { it / 3 } + fadeOut(tween(140)))
            }
        },
        label = "m3SettingsSubPage",
    ) { page ->
        when (page) {
            SettingsSubPage.Cores -> {
                M3CoreManagerPage(
                    games = games,
                    coreStates = coreStates,
                    neededCores = neededCores,
                    onDownloadCore = onDownloadCore,
                    onDownloadAllNeeded = onDownloadAllNeededCores,
                    onDeleteCore = onDeleteCore,
                    onBack = { subPage = SettingsSubPage.Root },
                )
                return@AnimatedContent
            }
            SettingsSubPage.Bios -> {
                M3BiosManagerPage(
                    biosStatusOf = biosStatusOf,
                    onImportBios = onImportBios,
                    onDownloadBiosLink = onOpenDownloadBios,
                    onDeleteBios = onDeleteBios,
                    onBack = { subPage = SettingsSubPage.Root },
                )
                return@AnimatedContent
            }
            SettingsSubPage.Folders -> {
                M3FoldersManagerPage(
                    folders = folders,
                    games = games,
                    scanning = scanning,
                    scanProgress = scanProgress,
                    onAddFolder = onAddFolder,
                    onRemoveFolder = onRemoveFolder,
                    onRescan = onRescan,
                    onBack = { subPage = SettingsSubPage.Root },
                )
                return@AnimatedContent
            }
            SettingsSubPage.Root -> Unit
        }

        val cs = MaterialTheme.colorScheme
        val inset = LocalToolbarInset.current
        val activePaletteId = currentPaletteId()
        val allCores = remember { Systems.allCores() }
        val installedCores = remember(coreStates, allCores) {
            allCores.count { coreStates[it.first.id] is CoreState.Installed }
        }
        val allBios = remember { Systems.all.flatMap { it.bios } }
        val installedBios = allBios.count { biosStatusOf(it) != BiosManager.Status.Missing }
        val abi = remember {
            when (val raw = CoreManager.abi().lowercase()) {
                "arm64-v8a" -> "ARM64"
                "armeabi-v7a" -> "ARM32"
                else -> raw.uppercase()
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Display Header
            item(key = "m3_set_header") {
                Column(Modifier.m3StaggeredEntrance(0, slideDp = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(text = "v${BuildConfig.VERSION_NAME} · $abi", style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer)
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

            // 3. 2x2 Sculptural Tonal Hub (Cores, ROM Folders, BIOS Health, Free Homebrew)
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
                            onClick = { subPage = SettingsSubPage.Cores },
                            modifier = Modifier.weight(1f),
                        )
                        M3TonalHubCard(
                            icon = Icons.Rounded.CreateNewFolder,
                            polygon = MaterialShapes.Clover4Leaf,
                            containerColor = cs.secondaryContainer,
                            contentColor = cs.onSecondaryContainer,
                            title = "ROM folders",
                            subtitle = "${folders.size} · ${games.size} ROMs",
                            onClick = { subPage = SettingsSubPage.Folders },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        M3TonalHubCard(
                            icon = Icons.Rounded.Memory,
                            polygon = MaterialShapes.Pentagon,
                            containerColor = cs.tertiaryContainer,
                            contentColor = cs.onTertiaryContainer,
                            title = "$installedBios / ${allBios.size} BIOS",
                            subtitle = "Firmware manager",
                            onClick = { subPage = SettingsSubPage.Bios },
                            modifier = Modifier.weight(1f),
                        )
                        M3TonalHubCard(
                            icon = Icons.Rounded.AutoAwesome,
                            polygon = MaterialShapes.Cookie7Sided,
                            containerColor = cs.surfaceContainerHighest,
                            contentColor = cs.onSurface,
                            title = "Homebrew",
                            subtitle = "Free classics",
                            onClick = onOpenHomebrew,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item(key = "m3_set_wave") {
                M3WavyBar(color = cs.outlineVariant.copy(alpha = 0.6f), amplitude = 2.dp, wavelength = 24.dp, strokeWidth = 2.dp)
            }

            // 4. Display & Controls (Exposes ALL real emulator video/input powers!)
            item(key = "m3_set_display") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Display & controls",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.primary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        M3SegmentedItem(
                            index = 0,
                            count = 4,
                            icon = Icons.Rounded.GridOn,
                            badgePolygon = MaterialShapes.Cookie6Sided,
                            badgeContainer = cs.primaryContainer,
                            badgeContent = cs.onPrimaryContainer,
                            title = "Sharp pixel scaling",
                            subtitle = "Crisp pixel edges instead of soft blur",
                            trailing = { M3ExpressiveSwitch(checked = sharpPixels, onCheckedChange = onSetSharpPixels) },
                        )
                        // Interactive Touch Pad Opacity Slider
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(m3SegmentedShape(1, 4))
                                .background(cs.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                M3ShapeBadge(polygon = MaterialShapes.Clover4Leaf, containerColor = cs.secondaryContainer, size = 40.dp) {
                                    Icon(Icons.Rounded.Opacity, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Touch pad opacity", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.onSurface)
                                    Text("Visibility of on-screen buttons", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                }
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(cs.primaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text("${(overlayOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = cs.onPrimaryContainer)
                                }
                            }
                            Slider(
                                value = overlayOpacity.coerceIn(0.2f, 1f),
                                onValueChange = onSetOverlayOpacity,
                                valueRange = 0.2f..1f,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        M3SegmentedItem(
                            index = 2,
                            count = 4,
                            icon = Icons.Rounded.Gamepad,
                            badgePolygon = MaterialShapes.Sunny,
                            badgeContainer = cs.tertiaryContainer,
                            badgeContent = cs.onTertiaryContainer,
                            title = "Analog stick as D-Pad",
                            subtitle = "Use left stick on Bluetooth controllers for D-pad games",
                            trailing = { M3ExpressiveSwitch(checked = stickAsDpad, onCheckedChange = onSetStickAsDpad) },
                        )
                        M3SegmentedItem(
                            index = 3,
                            count = 4,
                            icon = Icons.Rounded.SportsEsports,
                            badgePolygon = MaterialShapes.Gem,
                            badgeContainer = cs.secondaryContainer,
                            badgeContent = cs.onSecondaryContainer,
                            title = "Import Delta skin (.deltaskin)",
                            subtitle = "Custom handheld controller shells",
                            onClick = onImportSkin,
                        )
                    }
                }
            }

            // 5. Gameplay & Comfort
            item(key = "m3_set_gameplay") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gameplay & comfort",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.primary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        M3SegmentedItem(
                            index = 0,
                            count = 5,
                            icon = Icons.Rounded.Save,
                            badgePolygon = MaterialShapes.Cookie4Sided,
                            badgeContainer = cs.primaryContainer,
                            badgeContent = cs.onPrimaryContainer,
                            title = "Auto-save & instant resume",
                            subtitle = "Pick up right where you left off every time",
                            trailing = { M3ExpressiveSwitch(checked = autoSave, onCheckedChange = onSetAutoSave) },
                        )
                        M3SegmentedItem(
                            index = 1,
                            count = 5,
                            icon = Icons.Rounded.Vibration,
                            badgePolygon = MaterialShapes.Puffy,
                            badgeContainer = cs.tertiaryContainer,
                            badgeContent = cs.onTertiaryContainer,
                            title = "Tactile haptics",
                            subtitle = "Controller & UI button detents",
                            trailing = { M3ExpressiveSwitch(checked = haptics, onCheckedChange = onSetHaptics) },
                        )
                        M3SegmentedItem(
                            index = 2,
                            count = 5,
                            icon = Icons.Rounded.PhotoLibrary,
                            badgePolygon = MaterialShapes.Flower,
                            badgeContainer = cs.secondaryContainer,
                            badgeContent = cs.onSecondaryContainer,
                            title = "Save screenshots to Photos",
                            subtitle = "Copy in-game captures to Android gallery",
                            trailing = { M3ExpressiveSwitch(checked = screenshotsToGallery, onCheckedChange = onSetScreenshotsToGallery) },
                        )
                        M3SegmentedItem(
                            index = 3,
                            count = 5,
                            icon = Icons.Rounded.AccessibilityNew,
                            badgePolygon = MaterialShapes.Arch,
                            badgeContainer = cs.primaryContainer,
                            badgeContent = cs.onPrimaryContainer,
                            title = "Reduced motion",
                            subtitle = "Calm UI transitions and fewer animations",
                            trailing = { M3ExpressiveSwitch(checked = reducedMotion, onCheckedChange = onSetReducedMotion) },
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(m3SegmentedShape(4, 5))
                                .background(cs.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                M3ShapeBadge(polygon = MaterialShapes.SoftBurst, containerColor = cs.secondaryContainer, size = 40.dp) {
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

            // 6. Backup, Restore & Direct Downloads
            item(key = "m3_set_backup") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Backup & downloads",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.primary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        M3SegmentedItem(
                            index = 0,
                            count = 3,
                            icon = Icons.Rounded.Backup,
                            badgePolygon = MaterialShapes.Cookie9Sided,
                            badgeContainer = cs.primaryContainer,
                            badgeContent = cs.onPrimaryContainer,
                            title = "Back up saves & data",
                            subtitle = "Export saves, states, cheats & favorites to .zip",
                            onClick = onBackup,
                        )
                        M3SegmentedItem(
                            index = 1,
                            count = 3,
                            icon = Icons.Rounded.Restore,
                            badgePolygon = MaterialShapes.Clover4Leaf,
                            badgeContainer = cs.secondaryContainer,
                            badgeContent = cs.onSecondaryContainer,
                            title = "Restore from backup",
                            subtitle = "Import a RetroHero backup archive",
                            onClick = onRestore,
                        )
                        M3SegmentedItem(
                            index = 2,
                            count = 3,
                            icon = Icons.Rounded.Link,
                            badgePolygon = MaterialShapes.Cookie6Sided,
                            badgeContainer = cs.tertiaryContainer,
                            badgeContent = cs.onTertiaryContainer,
                            title = "Download ROM via HTTPS link",
                            subtitle = "Fetch a game file or folder directly",
                            onClick = onOpenDownloadGame,
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
                    .size(swatchSize.coerceAtLeast(24.dp))
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
