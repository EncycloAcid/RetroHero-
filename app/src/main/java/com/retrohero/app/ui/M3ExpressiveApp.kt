package com.retrohero.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3FloatingToolbarDock
import com.retrohero.app.ui.components.M3ToolbarDestination
import com.retrohero.app.ui.theme.Tone

private val M3Destinations = listOf(
    M3ToolbarDestination("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    M3ToolbarDestination("games", "Games", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    M3ToolbarDestination("settings", "Settings", Icons.Rounded.Tune, Icons.Outlined.Tune),
)

/**
 * Pure Material 3 Expressive application shell with directional spatial tab choreography,
 * physical background depth pullback when modal sheets open, and morphing toolbar FAB.
 */
@Composable
fun M3ExpressiveAppView(
    games: List<Game>,
    recents: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    scanning: Boolean = false,
    foldersCount: Int = 0,
    autoSave: Boolean = true,
    haptics: Boolean = true,
    fastForwardSpeed: Float = 3f,
    dynamicColor: Boolean = false,
    coreStates: Map<String, CoreState> = emptyMap(),
    saveSlotsFor: (Game) -> List<Pair<Int, Long>> = { emptyList() },
    currentCoreFor: (Game) -> CoreInfo? = { it.system?.cores?.firstOrNull() },
    onPlay: (Game, Int?) -> Unit = { _, _ -> },
    onShufflePlay: () -> Unit = {},
    onToggleFavorite: (Game) -> Unit = {},
    onSetShelfStatus: (Game, String) -> Unit = { _, _ -> },
    onSelectCore: (Game, CoreInfo) -> Unit = { _, _ -> },
    onDeleteGame: ((Game) -> Unit)? = null,
    onSetAutoSave: (Boolean) -> Unit = {},
    onSetHaptics: (Boolean) -> Unit = {},
    onSetFastForward: (Float) -> Unit = {},
    onSetDynamicColor: (Boolean) -> Unit = {},
    onSelectPalette: (String) -> Unit = {},
    onSetDarkMode: (Boolean) -> Unit = { Tone.setDarkMode(it) },
    onAddFolder: () -> Unit = {},
    onRescan: () -> Unit = {},
    onOpenHomebrew: () -> Unit = {},
    onOpenDownload: () -> Unit = {},
    onImportBios: () -> Unit = {},
    onImportSkin: () -> Unit = {},
    onDownloadCore: (CoreInfo) -> Unit = {},
    onDeleteCore: (CoreInfo) -> Unit = {},
    initialTab: Int = 0,
) {
    val cs = MaterialTheme.colorScheme
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var detailGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAddOpen by rememberSaveable { mutableStateOf(false) }

    val detailGame = remember(detailGameId, games) {
        detailGameId?.let { id -> games.firstOrNull { it.id == id } }
    }
    val anySheetOpen = detailGame != null || quickAddOpen

    // Signature spatial depth pullback: main canvas smoothly recedes & rounds corners behind sheets
    val backdropScale by animateFloatAsState(
        targetValue = if (anySheetOpen) 0.945f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 360f),
        label = "sheetBackdropScale",
    )
    val backdropCorner by animateDpAsState(
        targetValue = if (anySheetOpen) 28.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 360f),
        label = "sheetBackdropCorner",
    )

    CompositionLocalProvider(LocalToolbarInset provides 104.dp) {
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
                            val forward = targetState > initialState
                            val slideSpring = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 360f)
                            val enter = slideInHorizontally(slideSpring) { w -> if (forward) w / 7 else -w / 7 } +
                                fadeIn(tween(220)) +
                                scaleIn(spring(dampingRatio = 0.82f, stiffness = 380f), initialScale = 0.97f)
                            val exit = slideOutHorizontally(tween(180)) { w -> if (forward) -w / 10 else w / 10 } +
                                fadeOut(tween(140))
                            enter togetherWith exit
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
                                onPlay = { g -> onPlay(g, null) },
                                onOpenDetail = { g -> detailGameId = g.id },
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
                                onOpenDetail = { g -> detailGameId = g.id },
                                onQuickPlay = { g -> onPlay(g, null) },
                            )
                            else -> M3SettingsPage(
                                gamesCount = games.size,
                                foldersCount = foldersCount,
                                scanning = scanning,
                                autoSave = autoSave,
                                haptics = haptics,
                                fastForwardSpeed = fastForwardSpeed,
                                dynamicColor = dynamicColor,
                                coreStates = coreStates,
                                onSetAutoSave = onSetAutoSave,
                                onSetHaptics = onSetHaptics,
                                onSetFastForward = onSetFastForward,
                                onSetDynamicColor = onSetDynamicColor,
                                onSelectPalette = onSelectPalette,
                                onSetDarkMode = onSetDarkMode,
                                onAddFolder = onAddFolder,
                                onRescan = onRescan,
                                onOpenHomebrew = onOpenHomebrew,
                                onOpenDownload = onOpenDownload,
                                onImportBios = onImportBios,
                                onImportSkin = onImportSkin,
                                onDownloadCore = onDownloadCore,
                                onDeleteCore = onDeleteCore,
                            )
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

            // Independent fading backdrop scrim (never slides vertically with the sheet!)
            AnimatedVisibility(
                visible = anySheetOpen,
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
                            detailGameId = null
                            quickAddOpen = false
                        },
                )
            }

            // Expressive Spring Modal Game Detail Sheet
            AnimatedVisibility(
                visible = detailGame != null,
                enter = slideInVertically(spring(dampingRatio = 0.74f, stiffness = 340f)) { it } + fadeIn(tween(160)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(140)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val game = detailGame ?: return@AnimatedVisibility
                M3GameDetailSheet(
                    game = game,
                    stat = stats[game.id],
                    isFavorite = game.id in favoriteIds,
                    shelfStatus = shelfStatuses[game.id] ?: "playing",
                    saveSlots = saveSlotsFor(game),
                    currentCore = currentCoreFor(game),
                    onSetShelfStatus = { st -> onSetShelfStatus(game, st) },
                    onToggleFavorite = { onToggleFavorite(game) },
                    onPlay = { slot ->
                        detailGameId = null
                        onPlay(game, slot)
                    },
                    onSelectCore = { core -> onSelectCore(game, core) },
                    onDelete = onDeleteGame?.let { del ->
                        {
                            detailGameId = null
                            del(game)
                        }
                    },
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
