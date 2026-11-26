package com.retrohero.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.BiosFile
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.ExternalEmulator
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameDossier
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.components.LocalReducedMotion
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3FloatingToolbarDock
import com.retrohero.app.ui.components.M3ToolbarDestination
import com.retrohero.app.ui.theme.Tone
import java.io.File

private val M3Destinations = listOf(
    M3ToolbarDestination("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    M3ToolbarDestination("games", "Games", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    M3ToolbarDestination("settings", "Settings", Icons.Rounded.Tune, Icons.Outlined.Tune),
)

/**
 * Pure Material 3 Expressive application shell with directional spatial tab choreography,
 * full-screen Hydra-inspired Game Showcase Page, physical depth pullback behind modal sheets,
 * floating snackbar feedback, and morphing toolbar FAB.
 */
@Composable
fun M3ExpressiveAppView(
    games: List<Game>,
    recents: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    folders: Set<String> = emptySet(),
    scanning: Boolean = false,
    scanProgress: String = "",
    autoSave: Boolean = true,
    haptics: Boolean = true,
    sharpPixels: Boolean = false,
    overlayOpacity: Float = 0.72f,
    stickAsDpad: Boolean = true,
    screenshotsToGallery: Boolean = true,
    reducedMotion: Boolean = false,
    fastForwardSpeed: Float = 3f,
    dynamicColor: Boolean = false,
    coreStates: Map<String, CoreState> = emptyMap(),
    biosStatusOf: (BiosFile) -> BiosManager.Status = { BiosManager.Status.Missing },
    saveSlotsFor: (Game) -> List<Pair<Int, Long>> = { emptyList() },
    screenshotsFor: (Game) -> List<File> = { emptyList() },
    currentCoreFor: (Game) -> CoreInfo? = { it.system?.cores?.firstOrNull() },
    installedExternalEmulatorsFor: (Game) -> List<ExternalEmulator> = { emptyList() },
    selectedExternalEmulatorFor: (Game) -> ExternalEmulator? = { null },
    cachedDossierFor: (Game) -> GameDossier? = { null },
    loadDossierFor: (suspend (Game, Boolean) -> GameDossier)? = null,
    toastMessage: AppViewModel.Message? = null,
    onDismissToast: () -> Unit = {},
    onPlay: (Game, Int?) -> Unit = { _, _ -> },
    onShufflePlay: () -> Unit = {},
    onToggleFavorite: (Game) -> Unit = {},
    onSetShelfStatus: (Game, String) -> Unit = { _, _ -> },
    onSelectCore: (Game, CoreInfo) -> Unit = { _, _ -> },
    onSelectExternalEmulator: (Game, ExternalEmulator) -> Unit = { _, _ -> },
    onUseScreenshotAsCover: (Game, File) -> Unit = { _, _ -> },
    onClearCover: (Game) -> Unit = {},
    onDeleteGame: ((Game) -> Unit)? = null,
    onSetAutoSave: (Boolean) -> Unit = {},
    onSetHaptics: (Boolean) -> Unit = {},
    onSetSharpPixels: (Boolean) -> Unit = {},
    onSetOverlayOpacity: (Float) -> Unit = {},
    onSetStickAsDpad: (Boolean) -> Unit = {},
    onSetScreenshotsToGallery: (Boolean) -> Unit = {},
    onSetReducedMotion: (Boolean) -> Unit = {},
    onSetFastForward: (Float) -> Unit = {},
    onSetDynamicColor: (Boolean) -> Unit = {},
    onSelectPalette: (String) -> Unit = {},
    onSetDarkMode: (Boolean) -> Unit = { Tone.setDarkMode(it) },
    onAddFolder: () -> Unit = {},
    onRemoveFolder: (String) -> Unit = {},
    onRescan: () -> Unit = {},
    onOpenHomebrew: () -> Unit = {},
    onOpenDownload: () -> Unit = {},
    onOpenDownloadBios: () -> Unit = {},
    onImportBios: () -> Unit = {},
    onDeleteBios: (String) -> Unit = {},
    onImportSkin: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onDownloadCore: (CoreInfo) -> Unit = {},
    onDownloadAllNeededCores: (List<CoreInfo>) -> Unit = {},
    onDeleteCore: (CoreInfo) -> Unit = {},
    raProfile: com.retrohero.app.data.RaProfile? = null,
    raHardcore: Boolean = false,
    raUnofficial: Boolean = false,
    raNotifications: Boolean = true,
    cachedRaFor: (Game) -> com.retrohero.app.data.RaGameData? = { null },
    loadRaFor: (suspend (Game, Boolean) -> com.retrohero.app.data.RaGameData?)? = null,
    onOpenRaSignIn: () -> Unit = {},
    onSignOutRa: () -> Unit = {},
    onSetRaHardcore: (Boolean) -> Unit = {},
    onSetRaUnofficial: (Boolean) -> Unit = {},
    onSetRaNotifications: (Boolean) -> Unit = {},
    runAheadFrames: Int = 0,
    showPerfHud: Boolean = false,
    onSetRunAhead: (Int) -> Unit = {},
    onSetShowPerfHud: (Boolean) -> Unit = {},
    initialTab: Int = 0,
) {
    val cs = MaterialTheme.colorScheme
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var librarySystemFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var detailGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAddOpen by rememberSaveable { mutableStateOf(false) }

    val detailGame = remember(detailGameId, games) {
        detailGameId?.let { id -> games.firstOrNull { it.id == id } }
    }
    val anySheetOpen = detailGame != null || quickAddOpen

    // Cores actively needed by the user's current library
    val neededCores = remember(games) {
        games.mapNotNull { g -> currentCoreFor(g) }.distinctBy { it.id }
    }
    val missingCores = remember(neededCores, coreStates) {
        neededCores.filter { c -> !c.builtIn && coreStates[c.id] !is CoreState.Installed }
    }
    val installingCores = remember(neededCores, coreStates) {
        neededCores.any { c -> coreStates[c.id] is CoreState.Downloading }
    }

    // Signature spatial depth pullback: main canvas smoothly recedes & rounds corners behind sheets
    val backdropScale by animateFloatAsState(
        targetValue = if (anySheetOpen && !reducedMotion) 0.945f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 360f),
        label = "sheetBackdropScale",
    )
    val backdropCorner by animateDpAsState(
        targetValue = if (anySheetOpen && !reducedMotion) 28.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 360f),
        label = "sheetBackdropCorner",
    )

    CompositionLocalProvider(
        LocalToolbarInset provides 104.dp,
        LocalReducedMotion provides reducedMotion,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(cs.surfaceContainerLowest),
        ) {
            // Main interactive app stage that physically recedes when a modal sheet opens
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = backdropScale
                        scaleY = backdropScale
                    }
                    .clip(RoundedCornerShape(backdropCorner.coerceAtLeast(0.dp)))
                    .background(cs.background),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(tween(120)) togetherWith fadeOut(tween(100))
                            } else {
                                val forward = targetState > initialState
                                val slideSpring = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 360f)
                                val enter = slideInHorizontally(slideSpring) { w -> if (forward) w / 7 else -w / 7 } +
                                    fadeIn(tween(220)) +
                                    scaleIn(spring(dampingRatio = 0.82f, stiffness = 380f), initialScale = 0.97f)
                                val exit = slideOutHorizontally(tween(180)) { w -> if (forward) -w / 10 else w / 10 } +
                                    fadeOut(tween(140))
                                enter togetherWith exit
                            }
                        },
                        label = "m3SpatialTabSwitch",
                    ) { tab ->
                        when (tab) {
                            0 -> M3HomePage(
                                games = games,
                                recents = recents,
                                favoriteIds = favoriteIds,
                                stats = stats,
                                shelfStatuses = shelfStatuses,
                                scanning = scanning,
                                scanProgress = scanProgress,
                                missingCoresCount = missingCores.size,
                                installingCores = installingCores,
                                onInstallMissingCores = { onDownloadAllNeededCores(missingCores) },
                                onPlay = { g -> onPlay(g, null) },
                                onOpenDetail = { g -> detailGameId = g.id },
                                onSelectConsole = { sysId ->
                                    librarySystemFilter = sysId
                                    selectedTab = 1
                                },
                                onShufflePlay = onShufflePlay,
                                onToggleTheme = { onSetDarkMode(!Tone.isDark) },
                                onAddFolder = onAddFolder,
                                onOpenHomebrew = onOpenHomebrew,
                            )
                            1 -> M3LibraryPage(
                                games = games,
                                favoriteIds = favoriteIds,
                                stats = stats,
                                shelfStatuses = shelfStatuses,
                                selectedSystem = librarySystemFilter,
                                onSelectSystem = { librarySystemFilter = it },
                                onOpenDetail = { g -> detailGameId = g.id },
                                onQuickPlay = { g -> onPlay(g, null) },
                            )
                            else -> M3SettingsPage(
                                games = games,
                                folders = folders,
                                scanning = scanning,
                                scanProgress = scanProgress,
                                autoSave = autoSave,
                                haptics = haptics,
                                sharpPixels = sharpPixels,
                                overlayOpacity = overlayOpacity,
                                stickAsDpad = stickAsDpad,
                                screenshotsToGallery = screenshotsToGallery,
                                reducedMotion = reducedMotion,
                                fastForwardSpeed = fastForwardSpeed,
                                dynamicColor = dynamicColor,
                                coreStates = coreStates,
                                neededCores = neededCores,
                                biosStatusOf = biosStatusOf,
                                onSetAutoSave = onSetAutoSave,
                                onSetHaptics = onSetHaptics,
                                onSetSharpPixels = onSetSharpPixels,
                                onSetOverlayOpacity = onSetOverlayOpacity,
                                onSetStickAsDpad = onSetStickAsDpad,
                                onSetScreenshotsToGallery = onSetScreenshotsToGallery,
                                onSetReducedMotion = onSetReducedMotion,
                                onSetFastForward = onSetFastForward,
                                onSetDynamicColor = onSetDynamicColor,
                                onSelectPalette = onSelectPalette,
                                onSetDarkMode = onSetDarkMode,
                                onAddFolder = onAddFolder,
                                onRemoveFolder = onRemoveFolder,
                                onRescan = onRescan,
                                onOpenHomebrew = onOpenHomebrew,
                                onOpenDownloadGame = onOpenDownload,
                                onOpenDownloadBios = onOpenDownloadBios,
                                onImportBios = onImportBios,
                                onDeleteBios = onDeleteBios,
                                onImportSkin = onImportSkin,
                                onBackup = onBackup,
                                onRestore = onRestore,
                                onDownloadCore = onDownloadCore,
                                onDownloadAllNeededCores = { onDownloadAllNeededCores(missingCores) },
                                onDeleteCore = onDeleteCore,
                                raProfile = raProfile,
                                raHardcore = raHardcore,
                                raUnofficial = raUnofficial,
                                raNotifications = raNotifications,
                                onOpenRaSignIn = onOpenRaSignIn,
                                onSignOutRa = onSignOutRa,
                                onSetRaHardcore = onSetRaHardcore,
                                onSetRaUnofficial = onSetRaUnofficial,
                                onSetRaNotifications = onSetRaNotifications,
                                runAheadFrames = runAheadFrames,
                                showPerfHud = showPerfHud,
                                onSetRunAhead = onSetRunAhead,
                                onSetShowPerfHud = onSetShowPerfHud,
                            )
                        }
                    }
                }

                // Pure Material 3 Expressive Floating Pill Snackbar above the toolbar dock
                AnimatedVisibility(
                    visible = toastMessage != null && !anySheetOpen,
                    enter = slideInVertically(spring(dampingRatio = 0.78f, stiffness = 420f)) { it / 2 } + fadeIn(tween(160)),
                    exit = slideOutVertically(tween(150)) { it / 2 } + fadeOut(tween(120)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 18.dp, end = 18.dp, bottom = 92.dp),
                ) {
                    val msg = toastMessage ?: return@AnimatedVisibility
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(cs.inverseSurface)
                            .clickable(onClick = onDismissToast)
                            .padding(start = 18.dp, end = if (msg.undo != null) 8.dp else 18.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.inverseOnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (msg.undo != null) {
                            Box(
                                Modifier
                                    .padding(start = 10.dp)
                                    .clip(CircleShape)
                                    .background(cs.inversePrimary)
                                    .clickable {
                                        msg.undo.invoke()
                                        onDismissToast()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "Undo",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = cs.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                // Pure Material 3 Expressive Floating Toolbar + Morphing Cookie7Sided FAB
                M3FloatingToolbarDock(
                    destinations = M3Destinations,
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    onFabClick = { quickAddOpen = !quickAddOpen },
                    isFabOpen = quickAddOpen,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            BackHandler(enabled = quickAddOpen) { quickAddOpen = false }
            BackHandler(enabled = detailGame != null) { detailGameId = null }

            // Independent fading backdrop scrim behind Quick Add sheet
            AnimatedVisibility(
                visible = quickAddOpen,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(cs.scrim.copy(alpha = 0.56f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            quickAddOpen = false
                        },
                )
            }

            // Full-Screen Hydra-Inspired Mobile Material 3 Expressive Game Showcase Page
            AnimatedVisibility(
                visible = detailGame != null,
                enter = if (reducedMotion) fadeIn(tween(150)) else {
                    slideInVertically(spring(dampingRatio = 0.80f, stiffness = 360f)) { it / 6 } +
                        scaleIn(spring(dampingRatio = 0.82f, stiffness = 380f), initialScale = 0.94f) +
                        fadeIn(tween(200))
                },
                exit = if (reducedMotion) fadeOut(tween(120)) else {
                    slideOutVertically(tween(180)) { it / 8 } +
                        scaleOut(tween(160), targetScale = 0.96f) +
                        fadeOut(tween(150))
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                val game = detailGame ?: return@AnimatedVisibility
                val core = currentCoreFor(game)
                val extSelected = selectedExternalEmulatorFor(game)
                val coreReady = extSelected != null || core == null || core.builtIn || coreStates[core.id] is CoreState.Installed
                M3GameDetailPage(
                    game = game,
                    stat = stats[game.id],
                    isFavorite = game.id in favoriteIds,
                    shelfStatus = shelfStatuses[game.id] ?: "playing",
                    saveSlots = saveSlotsFor(game),
                    currentCore = core,
                    coreInstalled = coreReady,
                    installedExternalEmulators = installedExternalEmulatorsFor(game),
                    selectedExternalEmulator = extSelected,
                    screenshots = screenshotsFor(game),
                    initialDossier = cachedDossierFor(game),
                    loadDossier = loadDossierFor,
                    raProfile = raProfile,
                    initialRaData = cachedRaFor(game),
                    loadRaData = loadRaFor,
                    onOpenRaSignIn = onOpenRaSignIn,
                    onSetShelfStatus = { st -> onSetShelfStatus(game, st) },
                    onToggleFavorite = { onToggleFavorite(game) },
                    onPlay = { slot ->
                        detailGameId = null
                        onPlay(game, slot)
                    },
                    onSelectCore = { c -> onSelectCore(game, c) },
                    onSelectExternalEmulator = { ext -> onSelectExternalEmulator(game, ext) },
                    onUseScreenshotAsCover = { shot -> onUseScreenshotAsCover(game, shot) },
                    onClearCover = { onClearCover(game) },
                    onDelete = onDeleteGame?.let { del ->
                        {
                            detailGameId = null
                            del(game)
                        }
                    },
                    allGames = games,
                    onOpenGame = { target -> detailGameId = target.id },
                    onDismiss = { detailGameId = null },
                )
            }

            // Expressive Spring Quick Add Sheet
            AnimatedVisibility(
                visible = quickAddOpen,
                enter = slideInVertically(spring(dampingRatio = 0.74f, stiffness = 340f)) { it } + fadeIn(tween(160)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(140)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                M3QuickAddSheet(
                    hasGames = games.isNotEmpty(),
                    onAddFolder = onAddFolder,
                    onOpenHomebrew = onOpenHomebrew,
                    onOpenDownload = onOpenDownload,
                    onImportBios = onImportBios,
                    onImportSkin = onImportSkin,
                    onShufflePlay = onShufflePlay,
                    onDismiss = { quickAddOpen = false },
                )
            }
        }
    }
}
