package com.retrohero.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
import com.retrohero.app.data.Homebrew
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.CoverArt
import com.retrohero.app.ui.components.FloatingNavBar
import com.retrohero.app.ui.components.FloatingNavBarHeight
import com.retrohero.app.ui.components.FloatingNavRail
import com.retrohero.app.ui.components.InteractiveGameCaseModal
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.NavItem
import com.retrohero.app.ui.components.PackagingKind
import com.retrohero.app.ui.components.RealisticCaseFront
import com.retrohero.app.ui.components.RealisticCaseSpine
import com.retrohero.app.ui.components.packagingFor
import com.retrohero.app.ui.components.ShelfBucket
import com.retrohero.app.ui.components.posterAccentColor
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import java.util.Locale

internal enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Shelves("Shelves", Icons.Rounded.Tune, Icons.Outlined.Tune),
    Library("Vault", Icons.Rounded.Layers, Icons.Outlined.Layers),
    Stats("Stats", Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    System("System", Icons.Rounded.Settings, Icons.Outlined.Settings),
}

internal enum class VaultViewMode(val label: String, val icon: ImageVector) {
    Boxes("Boxes", Icons.Outlined.GridView),
    Spines("Spines", Icons.Outlined.ViewWeek),
    Index("Index", Icons.Outlined.ViewAgenda),
}

private val NavItems = MainTab.entries.map { NavItem(it.label, it.selectedIcon, it.unselectedIcon) }

internal fun resolveShelfBucket(
    game: Game,
    index: Int,
    recentIds: Set<String>,
    stat: PlayStat?,
    explicit: Map<String, String>,
): ShelfBucket {
    when (explicit[game.id]) {
        ShelfBucket.Playing.id -> return ShelfBucket.Playing
        ShelfBucket.Completed.id -> return ShelfBucket.Completed
        ShelfBucket.Backlog.id -> return ShelfBucket.Backlog
    }
    if (game.id in recentIds) return ShelfBucket.Playing
    if ((stat?.seconds ?: 0) >= 3_600) return ShelfBucket.Completed
    if ((stat?.seconds ?: 0) > 0) return ShelfBucket.Playing
    return when {
        index < 3 -> ShelfBucket.Playing
        index < 7 -> ShelfBucket.Completed
        else -> ShelfBucket.Backlog
    }
}

@Composable
internal fun ShelvesAppView(
    games: List<Game>,
    recents: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String> = emptyMap(),
    scanning: Boolean = false,
    hasFolders: Boolean = false,
    foldersCount: Int = if (hasFolders) 1 else 0,
    autoSave: Boolean = true,
    haptics: Boolean = true,
    fastForwardSpeed: Float = 3f,
    coreStates: Map<String, CoreState> = emptyMap(),
    initialTab: MainTab = MainTab.Shelves,
    initialVaultMode: VaultViewMode = VaultViewMode.Boxes,
    initialOpenGameId: String? = null,
    autoOpenCaseOnMount: Boolean = false,
    saveSlotsFor: (Game) -> List<Pair<Int, Long>> = { emptyList() },
    currentCoreFor: (Game) -> CoreInfo? = { it.system?.cores?.firstOrNull() },
    onAddFolder: () -> Unit,
    onRescan: () -> Unit = {},
    onOpenHomebrew: () -> Unit = {},
    onOpenDownload: () -> Unit = {},
    onImportBios: () -> Unit = {},
    onImportSkin: () -> Unit = {},
    onSetShelfBucket: (Game, ShelfBucket) -> Unit = { _, _ -> },
    onToggleFavorite: (Game) -> Unit = {},
    onPlay: (Game, Int?) -> Unit,
    onSelectCore: (Game, CoreInfo) -> Unit = { _, _ -> },
    onDeleteGame: ((Game) -> Unit)? = null,
    onSetAutoSave: (Boolean) -> Unit = {},
    onSetHaptics: (Boolean) -> Unit = {},
    onSetFastForward: (Float) -> Unit = {},
    onSelectAccent: (String, Color) -> Unit = { _, _ -> },
    onToggleTheme: () -> Unit = {},
    onDownloadCore: (CoreInfo) -> Unit = {},
    onDeleteCore: (CoreInfo) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var openCaseGameId by rememberSaveable { mutableStateOf(initialOpenGameId) }
    var autoSwingOpen by rememberSaveable { mutableStateOf(autoOpenCaseOnMount) }
    var addMenuOpen by remember { mutableStateOf(false) }

    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomNavSpace = if (wide) 16.dp else (FloatingNavBarHeight + 28.dp + navInset)

    val recentIds = remember(recents) { recents.mapTo(HashSet()) { it.id } }
    val buckets = remember(games, recentIds, stats, shelfStatuses) {
        val map = LinkedHashMap<ShelfBucket, MutableList<Game>>().apply {
            ShelfBucket.entries.forEach { put(it, mutableListOf()) }
        }
        games.forEachIndexed { idx, g ->
            val b = resolveShelfBucket(g, idx, recentIds, stats[g.id], shelfStatuses)
            map.getValue(b).add(g)
        }
        map
    }

    CompositionLocalProvider(LocalNavSpace provides bottomNavSpace) {
        Box(Modifier.fillMaxSize().background(Tone.Bg)) {
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    FloatingNavRail(
                        items = NavItems,
                        selected = tab.ordinal,
                        onSelect = { tab = MainTab.entries[it] },
                    )
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            fadeIn(tween(200, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(150, easing = FastOutSlowInEasing))
                        },
                        label = "mainPageTransition",
                    ) { currentTab ->
                        when (currentTab) {
                            MainTab.Shelves -> ShelvesPage(
                                games = games,
                                recents = recents,
                                buckets = buckets,
                                favoriteGames = remember(games, favoriteIds) { games.filter { it.id in favoriteIds } },
                                stats = stats,
                                scanning = scanning,
                                hasFolders = hasFolders,
                                addMenuOpen = addMenuOpen,
                                onAddMenuToggle = { addMenuOpen = it },
                                onAddFolder = onAddFolder,
                                onRescan = onRescan,
                                onOpenHomebrew = onOpenHomebrew,
                                onOpenDownload = onOpenDownload,
                                onOpenCase = { autoSwingOpen = false; openCaseGameId = it.id },
                                onOpenCaseOpened = { autoSwingOpen = true; openCaseGameId = it.id },
                                onQuickPlay = { onPlay(it, null) },
                            )
                            MainTab.Library -> LibraryVaultPage(
                                games = games,
                                buckets = buckets,
                                stats = stats,
                                initialMode = initialVaultMode,
                                scanning = scanning,
                                onAddFolder = onAddFolder,
                                onOpenCase = { autoSwingOpen = false; openCaseGameId = it.id },
                                onQuickPlay = { onPlay(it, null) },
                            )
                            MainTab.Stats -> StatsDashboardPage(
                                games = games,
                                buckets = buckets,
                                favoriteCount = favoriteIds.size,
                                stats = stats,
                                onOpenCase = { autoSwingOpen = false; openCaseGameId = it.id },
                            )
                            MainTab.System -> SystemControlDeckPage(
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
                                onToggleTheme = onToggleTheme,
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
            }

            // Floating Capsule Navigation Bar suspended over the bottom edge
            if (!wide) {
                FloatingNavBar(
                    items = NavItems,
                    selected = tab.ordinal,
                    onSelect = { tab = MainTab.entries[it] },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Interactive 3D Physical Game Case Overlay
            val activeCaseGame = openCaseGameId?.let { id -> games.firstOrNull { it.id == id } }
            var retainedGame by remember { mutableStateOf(activeCaseGame) }
            if (activeCaseGame != null) retainedGame = activeCaseGame

            AnimatedVisibility(
                visible = activeCaseGame != null,
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.94f),
                exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.95f),
            ) {
                val g = retainedGame
                if (g != null) {
                    val gIdx = games.indexOfFirst { it.id == g.id }.coerceAtLeast(0)
                    val bucket = resolveShelfBucket(g, gIdx, recentIds, stats[g.id], shelfStatuses)
                    InteractiveGameCaseModal(
                        game = g,
                        stat = stats[g.id],
                        isFavorite = g.id in favoriteIds,
                        shelfBucket = bucket,
                        saveSlots = saveSlotsFor(g),
                        currentCore = currentCoreFor(g),
                        autoOpenOnMount = autoSwingOpen,
                        onSetShelfBucket = { onSetShelfBucket(g, it) },
                        onToggleFavorite = { onToggleFavorite(g) },
                        onPlay = { slot -> onPlay(g, slot) },
                        onSelectCore = { core -> onSelectCore(g, core) },
                        onDelete = if (g.isLocal && onDeleteGame != null) ({ onDeleteGame(g) }) else null,
                        onClose = { openCaseGameId = null },
                    )
                }
            }
        }
    }
}

// =================================================================================================
// PAGE 1: SHELVES — "On Your Desk" 3D Case Spotlight + Curated Horizontal Pure-Cover Shelves
// =================================================================================================

@Composable
private fun ShelvesPage(
    games: List<Game>,
    recents: List<Game>,
    buckets: Map<ShelfBucket, List<Game>>,
    favoriteGames: List<Game>,
    stats: Map<String, PlayStat>,
    scanning: Boolean,
    hasFolders: Boolean,
    addMenuOpen: Boolean,
    onAddMenuToggle: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onOpenCase: (Game) -> Unit,
    onOpenCaseOpened: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    val navSpace = LocalNavSpace.current
    val spotlightGame = remember(recents, buckets, games) {
        recents.firstOrNull() ?: buckets[ShelfBucket.Playing]?.firstOrNull() ?: games.firstOrNull()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val coverWidth = ((maxWidth - 36.dp - 28.5.dp) / 4f).coerceIn(74.dp, 118.dp)

        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, bottom = navSpace + 18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "shelves_header") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "COLLECTION · ${games.size} TITLES",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                            color = Ascend.accent,
                        )
                        Text(
                            text = "Shelves",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.03).em),
                            color = Tone.Ink,
                        )
                    }

                    AddGamesMenuButton(
                        expanded = addMenuOpen,
                        scanning = scanning,
                        hasFolders = hasFolders,
                        onToggle = onAddMenuToggle,
                        onAddFolder = onAddFolder,
                        onRescan = onRescan,
                        onOpenHomebrew = onOpenHomebrew,
                        onOpenDownload = onOpenDownload,
                    )
                }
            }

            if (games.isEmpty()) {
                item(key = "shelves_empty") {
                    MinimalEmptyState(scanning = scanning, onAddFolder = onAddFolder)
                }
            } else {
                // Featured "On Your Desk" 3D Case Spotlight Card
                if (spotlightGame != null) {
                    item(key = "shelves_spotlight") {
                        DeskSpotlightCard(
                            game = spotlightGame,
                            stat = stats[spotlightGame.id],
                            onResume = { onQuickPlay(spotlightGame) },
                            onOpenCase = { onOpenCaseOpened(spotlightGame) },
                            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                }

                ShelfBucket.entries.forEach { bucket ->
                    val list = buckets[bucket].orEmpty()
                    if (list.isNotEmpty()) {
                        item(key = "shelf_${bucket.id}") {
                            ShelfCoverSection(
                                title = bucket.label,
                                games = list,
                                coverWidth = coverWidth,
                                onOpenCase = onOpenCase,
                                onQuickPlay = onQuickPlay,
                            )
                        }
                    }
                }

                if (favoriteGames.isNotEmpty()) {
                    item(key = "shelf_favourites") {
                        ShelfCoverSection(
                            title = "Favourites",
                            games = favoriteGames,
                            coverWidth = coverWidth,
                            onOpenCase = onOpenCase,
                            onQuickPlay = onQuickPlay,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeskSpotlightCard(
    game: Game,
    stat: PlayStat?,
    onResume: () -> Unit,
    onOpenCase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = remember(game.id) { posterAccentColor(game) }
    val density = LocalDensity.current
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onOpenCase),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.5f),
                    radius = size.width * 0.55f,
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.18f, size.height * 0.5f),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Mini angled 3D authentic retail case (jewel case, keepcase, cardboard box...)
            val kind = remember(game.systemId) { packagingFor(game.systemId) }
            val caseRatio = (kind.closedWidth.value / kind.closedHeight.value).coerceIn(0.62f, 1.40f)
            Box(
                Modifier.size(width = 108.dp, height = 120.dp),
                contentAlignment = Alignment.Center,
            ) {
                val caseModifier = if (caseRatio >= 0.9f) {
                    Modifier.width(108.dp).aspectRatio(caseRatio)
                } else {
                    Modifier.height(120.dp).aspectRatio(caseRatio)
                }
                RealisticCaseFront(
                    game = game,
                    compact = true,
                    modifier = caseModifier
                        .graphicsLayer {
                            cameraDistance = 16f * density.density
                            rotationY = -16f
                            rotationX = 4f
                        }
                        .shadow(14.dp, RoundedCornerShape(6.dp)),
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "ON YOUR DESK",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = game.title,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 22.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = buildString {
                        append(game.system?.shortName ?: game.systemId.uppercase())
                        if (stat != null && stat.seconds > 0) {
                            append(" · ")
                            append(PlayTime.format(stat.seconds))
                        }
                    }
                    Text(meta, style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Ascend.accent)
                            .clickable(onClick = onResume)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Resume", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp), color = Color.White)
                    }

                    Box(
                        Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Tone.Raised)
                            .border(1.dp, Tone.Line, RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenCase)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Open case", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp), color = Tone.Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfCoverSection(
    title: String,
    games: List<Game>,
    coverWidth: Dp,
    onOpenCase: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    letterSpacing = (-0.015).em,
                ),
                color = Tone.Ink,
            )
            Text(
                text = if (games.size == 1) "1 game" else "${games.size} games",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.5.sp),
                color = Tone.Muted,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(9.5.dp),
        ) {
            items(games, key = { "${title}_${it.id}" }) { game ->
                TactileCoverTile(
                    game = game,
                    width = coverWidth,
                    onClick = { onOpenCase(game) },
                    onLongClick = { onQuickPlay(game) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = Tone.Line,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

@Composable
private fun TactileCoverTile(
    game: Game,
    width: Dp?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = 0.65f, stiffness = 550f),
        label = "coverPressScale",
    )
    val rotY by animateFloatAsState(
        if (pressed) -9f else 0f,
        spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "coverPressTilt",
    )
    val kind = packagingFor(game.systemId)
    val caseRatio = (kind.closedWidth.value / kind.closedHeight.value).coerceIn(0.62f, 1.40f)

    // Fixed 0.68 slot; the real case shape stands on the slot's bottom edge.
    Box(
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .aspectRatio(0.68f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        RealisticCaseFront(
            game = game,
            compact = true,
            modifier = (if (caseRatio >= 0.68f) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
                .aspectRatio(caseRatio)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationY = rotY
                }
                .shadow(6.dp, RoundedCornerShape(6.dp)),
        )
    }
}

// =================================================================================================
// PAGE 2: VAULT — Console Hardware Deck + 3-Mode Switcher (Boxes · 3D Spine Bookshelf · Index)
// =================================================================================================

@Composable
private fun LibraryVaultPage(
    games: List<Game>,
    buckets: Map<ShelfBucket, List<Game>>,
    stats: Map<String, PlayStat>,
    initialMode: VaultViewMode,
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onOpenCase: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    val navSpace = LocalNavSpace.current
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSystem by rememberSaveable { mutableStateOf<String?>(null) }

    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }
    val bucketOf = remember(buckets) {
        buildMap {
            buckets.forEach { (b, list) -> list.forEach { put(it.id, b) } }
        }
    }
    val filtered = remember(games, query, selectedSystem) {
        val q = query.trim().lowercase(Locale.getDefault())
        games.filter { g ->
            (selectedSystem == null || g.systemId == selectedSystem) &&
                (q.isEmpty() || g.title.lowercase(Locale.getDefault()).contains(q) || g.systemId.contains(q))
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = navSpace + 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Vault Header + 3-Mode Layout Switcher Pill
        item(key = "vault_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ARCHIVE · ${systems.size} CONSOLES",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = "Vault",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.03).em),
                        color = Tone.Ink,
                    )
                }

                // Segmented 3-Mode Switcher (Boxes · Spines · Index)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    VaultViewMode.entries.forEach { m ->
                        val active = m == mode
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) Tone.Raised else Color.Transparent)
                                .clickable { mode = m }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                m.icon,
                                contentDescription = m.label,
                                tint = if (active) Ascend.accent else Tone.Muted,
                                modifier = Modifier.size(15.dp),
                            )
                            if (active) {
                                Text(
                                    m.label,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                                    color = Tone.Ink,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tactile Console Hardware Cards Carousel
        if (systems.isNotEmpty()) {
            item(key = "vault_consoles", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "console_all") {
                        ConsoleDeckCard(
                            shortName = "ALL",
                            fullName = "All Systems",
                            count = games.size,
                            selected = selectedSystem == null,
                            onClick = { selectedSystem = null },
                        )
                    }
                    items(systems, key = { "console_${it.id}" }) { sys ->
                        ConsoleDeckCard(
                            shortName = sys.shortName,
                            fullName = sys.name,
                            count = counts[sys.id] ?: 0,
                            selected = selectedSystem == sys.id,
                            onClick = { selectedSystem = if (selectedSystem == sys.id) null else sys.id },
                        )
                    }
                }
            }
        }

        // Search Capsule
        if (games.isNotEmpty()) {
            item(key = "vault_search", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, null, tint = Ascend.accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Filter ${filtered.size} cases…",
                                style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp),
                                color = Tone.Muted,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, color = Tone.Ink),
                            cursorBrush = SolidColor(Ascend.accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Tone.Muted,
                            modifier = Modifier.size(16.dp).clickable { query = "" },
                        )
                    }
                }
            }
        }

        if (games.isEmpty()) {
            item(key = "vault_empty", span = { GridItemSpan(maxLineSpan) }) {
                MinimalEmptyState(scanning = scanning, onAddFolder = onAddFolder)
            }
        } else {
            when (mode) {
                // Mode A: 3-Column Physical Box-Cases with Spine Edge & System Tag
                VaultViewMode.Boxes -> {
                    items(filtered, key = { "box_${it.id}" }) { game ->
                        VaultPhysicalBoxCard(
                            game = game,
                            onClick = { onOpenCase(game) },
                            onLongClick = { onQuickPlay(game) },
                        )
                    }
                }

                // Mode B: Physical Collector's Spine Bookshelf Ledges!
                VaultViewMode.Spines -> {
                    val shelves = filtered.chunked(8)
                    shelves.forEachIndexed { shelfIdx, rowGames ->
                        item(key = "spine_row_$shelfIdx", span = { GridItemSpan(maxLineSpan) }) {
                            SpineBookshelfRow(
                                games = rowGames,
                                onSelectSpine = onOpenCase,
                            )
                        }
                    }
                }

                // Mode C: Detailed Cartridge / Disc Collector Index Cards
                VaultViewMode.Index -> {
                    items(filtered, key = { "idx_${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { game ->
                        VaultIndexRow(
                            game = game,
                            bucket = bucketOf[game.id] ?: ShelfBucket.Backlog,
                            stat = stats[game.id],
                            onOpenCase = { onOpenCase(game) },
                            onPlay = { onQuickPlay(game) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleDeckCard(
    shortName: String,
    fullName: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .width(108.dp)
            .clip(shape)
            .background(if (selected) Ascend.accent.copy(alpha = 0.16f) else Tone.Surface)
            .border(1.dp, if (selected) Ascend.accent else Tone.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shortName,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.04.em),
                color = if (selected) Ascend.accent else Tone.Ink,
                maxLines = 1,
            )
            Text(
                text = count.toString(),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
                color = Tone.Muted,
            )
        }
        Text(
            text = fullName,
            style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
            color = Tone.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VaultPhysicalBoxCard(
    game: Game,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val kind = packagingFor(game.systemId)
    val caseRatio = (kind.closedWidth.value / kind.closedHeight.value).coerceIn(0.62f, 1.40f)
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Fixed-height slot so mixed case shapes (square jewel, wide SNES box, tall keepcase)
        // all stand on the same bottom baseline like a real shelf.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val boxModifier = if (caseRatio >= 0.70f) {
                Modifier.fillMaxWidth().aspectRatio(caseRatio)
            } else {
                Modifier.fillMaxHeight().aspectRatio(caseRatio)
            }
            RealisticCaseFront(
                game = game,
                compact = true,
                modifier = boxModifier.shadow(8.dp, RoundedCornerShape(6.dp)),
            )
        }
        Text(
            text = game.title,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
            color = Tone.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SpineBookshelfRow(
    games: List<Game>,
    onSelectSpine: (Game) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
            verticalAlignment = Alignment.Bottom,
        ) {
            games.forEach { game ->
                // Authentic per-console spine: slim clear-ribbed jewel case, chunky Sega clamshell,
                // gloss-white Wii/3DS keepcase, creased cardboard box, etc.
                val spineHeight = when (packagingFor(game.systemId)) {
                    PackagingKind.JewelCase -> 164.dp // spine shortens jewel cases internally
                    PackagingKind.LandscapeCardboard -> 128.dp
                    PackagingKind.KeepCaseDs -> 142.dp
                    PackagingKind.PortraitCardboard -> 152.dp
                    else -> 164.dp
                }
                RealisticCaseSpine(
                    game = game,
                    height = spineHeight,
                    onClick = { onSelectSpine(game) },
                )
            }
        }
        // Dark anodized shelf ledge beneath the standing spines
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF282833), Color(0xFF14141A)),
                    ),
                )
                .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun VaultIndexRow(
    game: Game,
    bucket: ShelfBucket,
    stat: PlayStat?,
    onOpenCase: () -> Unit,
    onPlay: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onOpenCase)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverArt(
            game,
            Modifier
                .width(44.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildString {
                append(game.system?.shortName ?: game.systemId.uppercase())
                append(" · ")
                append(bucket.label)
                if (stat != null && stat.seconds > 0) {
                    append(" · ")
                    append(PlayTime.format(stat.seconds))
                }
            }
            Text(sub, style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted, maxLines = 1)
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Ascend.accent.copy(alpha = 0.16f))
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${game.title}", tint = Ascend.accent, modifier = Modifier.size(18.dp))
        }
    }
}

// =================================================================================================
// Shared Add Menu, Empty State & Homebrew Catalog
// =================================================================================================

@Composable
private fun AddGamesMenuButton(
    expanded: Boolean,
    scanning: Boolean,
    hasFolders: Boolean,
    onToggle: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
) {
    Box {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Ascend.accent)
                .clickable { onToggle(true) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add games", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onToggle(false) },
            shape = RoundedCornerShape(14.dp),
            containerColor = Tone.Surface,
            border = BorderStroke(1.dp, Tone.Line),
        ) {
            DropdownMenuItem(
                text = { Text("Add game folder", color = Tone.Ink) },
                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null, tint = Ascend.accent) },
                onClick = { onToggle(false); onAddFolder() },
            )
            DropdownMenuItem(
                text = { Text("Free homebrew games", color = Tone.Ink) },
                leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null, tint = Ascend.accent) },
                onClick = { onToggle(false); onOpenHomebrew() },
            )
            DropdownMenuItem(
                text = { Text("Download from link", color = Tone.Ink) },
                leadingIcon = { Icon(Icons.Outlined.Download, null, tint = Ascend.accent) },
                onClick = { onToggle(false); onOpenDownload() },
            )
            if (hasFolders) {
                DropdownMenuItem(
                    text = { Text(if (scanning) "Scanning…" else "Rescan library", color = Tone.Ink) },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, null, tint = Ascend.accent) },
                    enabled = !scanning,
                    onClick = { onToggle(false); onRescan() },
                )
            }
        }
    }
}

@Composable
private fun MinimalEmptyState(scanning: Boolean, onAddFolder: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (scanning) "Scanning your ROM folder…" else "Your shelves are empty",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = Tone.Ink,
        )
        if (!scanning) {
            Row(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ascend.accent)
                    .clickable(onClick = onAddFolder)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    text = "Add game folder",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = Color.White,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomebrewCatalogSheet(
    installedNames: Set<String>,
    onInstall: (HomebrewGame) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tone.Surface,
        contentColor = Tone.Ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Free homebrew games",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = Tone.Ink,
            )
            Homebrew.catalog.forEach { entry ->
                val installed = entry.fileName in installedNames
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Tone.Raised)
                        .clickable(enabled = !installed) { onDismiss(); onInstall(entry) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(entry.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp), color = Tone.Ink)
                        Text("${entry.system?.shortName ?: entry.systemId.uppercase()} · ${entry.author}", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                    }
                    Text(
                        text = if (installed) "Added" else "Get",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = if (installed) Tone.Muted else Ascend.accent,
                    )
                }
            }
        }
    }
}
