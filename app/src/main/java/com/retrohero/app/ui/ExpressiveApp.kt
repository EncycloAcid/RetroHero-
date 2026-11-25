package com.retrohero.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.DockTab
import com.retrohero.app.ui.components.ExpressiveIosDock
import com.retrohero.app.ui.components.LocalDockInset
import com.retrohero.app.ui.theme.Tone

private val MainDockTabs = listOf(
    DockTab("play", "Play", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle),
    DockTab("library", "Library", Icons.Rounded.GridView, Icons.Outlined.GridView),
    DockTab("settings", "Settings", Icons.Rounded.Tune, Icons.Outlined.Tune),
)

/**
 * Pure stateless shell for the M3 Expressive × Little iOS launcher UI,
 * powering both [MainActivity] and deterministic Compose screenshot tests.
 */
@Composable
fun ExpressiveAppView(
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
    onSelectAccent: (String, Color) -> Unit = { _, _ -> },
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
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var detailGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAddOpen by rememberSaveable { mutableStateOf(false) }

    val detailGame = remember(detailGameId, games) {
        detailGameId?.let { id -> games.firstOrNull { it.id == id } }
    }

    CompositionLocalProvider(LocalDockInset provides 104.dp) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Tone.Bg),
        ) {
            // Soft M3 Expressive atmospheric mesh glow hanging from top edge
            val glowColor = Ascend.accent
            Canvas(Modifier.fillMaxWidth().height(260.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            glowColor.copy(alpha = if (Tone.isDark) 0.16f else 0.10f),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * 0.18f, -size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFF0A84FF).copy(alpha = if (Tone.isDark) 0.10f else 0.07f),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.90f, size.height * 0.05f),
                )
            }

            // Main active tab content
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "expressiveTabSwitch",
                ) { tab ->
                    when (tab) {
                        0 -> PlayHomePage(
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
                        1 -> LibraryGridPage(
                            games = games,
                            favoriteIds = favoriteIds,
                            stats = stats,
                            shelfStatuses = shelfStatuses,
                            onOpenDetail = { g -> detailGameId = g.id },
                            onQuickPlay = { g -> onPlay(g, null) },
                        )
                        else -> SettingsDeckPage(
                            gamesCount = games.size,
                            foldersCount = foldersCount,
                            scanning = scanning,
                            autoSave = autoSave,
                            haptics = haptics,
                            fastForwardSpeed = fastForwardSpeed,
                            coreStates = coreStates,
                            onSetAutoSave = onSetAutoSave,
                            onSetHaptics = onSetHaptics,
                            onSetFastForward = onSetFastForward,
                            onSelectAccent = onSelectAccent,
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

            // Signature M3 Expressive Floating Toolbar × iOS Liquid Glass Dock + Detached Cookie FAB
            ExpressiveIosDock(
                tabs = MainDockTabs,
                selected = selectedTab,
                onSelect = { selectedTab = it },
                onFabClick = { quickAddOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // Frosted iOS Game Detail Sheet
            AnimatedVisibility(
                visible = detailGame != null,
                enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 3 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 3 },
            ) {
                val game = detailGame ?: return@AnimatedVisibility
                GameDetailSheet(
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

            // Frosted iOS Quick Add Action Sheet (triggered by detached Cookie + FAB)
            AnimatedVisibility(
                visible = quickAddOpen,
                enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 3 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 3 },
            ) {
                QuickAddSheet(
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
