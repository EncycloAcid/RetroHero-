package com.retrohero.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.Homebrew
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.CoverArt
import com.retrohero.app.ui.components.InteractiveGameCaseModal
import com.retrohero.app.ui.components.ShelfBucket
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import java.util.Locale

internal enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Library("Library", Icons.Rounded.Layers, Icons.Outlined.Layers),
    Shelves("Shelves", Icons.Rounded.Tune, Icons.Outlined.Tune),
    Stats("Stats", Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    Settings("Settings", Icons.Rounded.Person, Icons.Outlined.Person),
}

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
    // Smart initial shelf distribution when no play history exists yet
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
    initialTab: MainTab = MainTab.Shelves,
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
    onToggleTheme: (() -> Unit)? = null,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var openCaseGameId by rememberSaveable { mutableStateOf(initialOpenGameId) }
    var addMenuOpen by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize().background(Tone.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                when (tab) {
                    MainTab.Shelves -> ShelvesScreenContent(
                        games = games,
                        buckets = buckets,
                        favoriteGames = remember(games, favoriteIds) { games.filter { it.id in favoriteIds } },
                        scanning = scanning,
                        hasFolders = hasFolders,
                        addMenuOpen = addMenuOpen,
                        onAddMenuToggle = { addMenuOpen = it },
                        onAddFolder = onAddFolder,
                        onRescan = onRescan,
                        onOpenHomebrew = onOpenHomebrew,
                        onOpenDownload = onOpenDownload,
                        onOpenCase = { openCaseGameId = it.id },
                        onQuickPlay = { onPlay(it, null) },
                    )
                    MainTab.Library -> LibraryGridContent(
                        games = games,
                        scanning = scanning,
                        hasFolders = hasFolders,
                        addMenuOpen = addMenuOpen,
                        onAddMenuToggle = { addMenuOpen = it },
                        onAddFolder = onAddFolder,
                        onRescan = onRescan,
                        onOpenHomebrew = onOpenHomebrew,
                        onOpenDownload = onOpenDownload,
                        onOpenCase = { openCaseGameId = it.id },
                        onQuickPlay = { onPlay(it, null) },
                    )
                    MainTab.Stats -> StatsScreenContent(
                        games = games,
                        buckets = buckets,
                        stats = stats,
                        onOpenCase = { openCaseGameId = it.id },
                    )
                    MainTab.Settings -> SettingsScreenContent(
                        gamesCount = games.size,
                        scanning = scanning,
                        hasFolders = hasFolders,
                        onAddFolder = onAddFolder,
                        onRescan = onRescan,
                        onOpenHomebrew = onOpenHomebrew,
                        onOpenDownload = onOpenDownload,
                        onImportBios = onImportBios,
                        onImportSkin = onImportSkin,
                        onToggleTheme = onToggleTheme,
                    )
                }
            }

            // Bottom 4-Tab Bar with Glowing Warm Ember Top Indicator & Vertical Spotlight
            EmberBottomTabBar(
                selected = tab,
                onSelect = { tab = it },
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
                    autoOpenOnMount = autoOpenCaseOnMount,
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

@Composable
private fun ShelvesScreenContent(
    games: List<Game>,
    buckets: Map<ShelfBucket, List<Game>>,
    favoriteGames: List<Game>,
    scanning: Boolean,
    hasFolders: Boolean,
    addMenuOpen: Boolean,
    onAddMenuToggle: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onOpenCase: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Exactly 4 crisp covers fit across standard phone width (18dp outer padding, 9.5dp gaps)
        val coverWidth = ((maxWidth - 36.dp - 28.5.dp) / 4f).coerceIn(74.dp, 118.dp)

        LazyColumn(
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "shelves_header") {
                TopTitleRow(
                    title = "Shelves",
                    addMenuOpen = addMenuOpen,
                    scanning = scanning,
                    hasFolders = hasFolders,
                    onAddMenuToggle = onAddMenuToggle,
                    onAddFolder = onAddFolder,
                    onRescan = onRescan,
                    onOpenHomebrew = onOpenHomebrew,
                    onOpenDownload = onOpenDownload,
                )
            }

            if (games.isEmpty()) {
                item(key = "shelves_empty") {
                    MinimalEmptyState(scanning = scanning, onAddFolder = onAddFolder)
                }
            } else {
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
    val shape = RoundedCornerShape(7.dp)

    Box(
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .aspectRatio(0.68f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = rotY
            }
            .clip(shape)
            .background(Tone.Surface)
            .border(0.6.dp, Color.White.copy(alpha = 0.09f), shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        CoverArt(game, Modifier.fillMaxSize())
    }
}

@Composable
private fun LibraryGridContent(
    games: List<Game>,
    scanning: Boolean,
    hasFolders: Boolean,
    addMenuOpen: Boolean,
    onAddMenuToggle: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onOpenCase: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSystem by rememberSaveable { mutableStateOf<String?>(null) }

    val systems = remember(games) {
        val ids = games.mapTo(HashSet()) { it.systemId }
        Systems.all.filter { it.id in ids }
    }
    val filtered = remember(games, query, selectedSystem) {
        val q = query.trim().lowercase(Locale.getDefault())
        games.filter { g ->
            (selectedSystem == null || g.systemId == selectedSystem) &&
                (q.isEmpty() || g.title.lowercase(Locale.getDefault()).contains(q) || g.systemId.contains(q))
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.5.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "lib_header", span = { GridItemSpan(maxLineSpan) }) {
            TopTitleRow(
                title = "Library",
                addMenuOpen = addMenuOpen,
                scanning = scanning,
                hasFolders = hasFolders,
                onAddMenuToggle = onAddMenuToggle,
                onAddFolder = onAddFolder,
                onRescan = onRescan,
                onOpenHomebrew = onOpenHomebrew,
                onOpenDownload = onOpenDownload,
                horizontalPadding = 0.dp,
            )
        }

        if (games.isNotEmpty()) {
            item(key = "lib_search", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Tone.Surface)
                            .border(1.dp, Tone.Line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = Tone.Muted, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    "Search ${games.size} games…",
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

                    if (systems.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                SystemFilterPill("All", selectedSystem == null) { selectedSystem = null }
                            }
                            items(systems, key = { it.id }) { sys ->
                                SystemFilterPill(sys.shortName, selectedSystem == sys.id) {
                                    selectedSystem = if (selectedSystem == sys.id) null else sys.id
                                }
                            }
                        }
                    }
                }
            }
        }

        if (games.isEmpty()) {
            item(key = "lib_empty", span = { GridItemSpan(maxLineSpan) }) {
                MinimalEmptyState(scanning = scanning, onAddFolder = onAddFolder)
            }
        } else {
            items(filtered, key = { "grid_${it.id}" }) { game ->
                TactileCoverTile(
                    game = game,
                    width = null,
                    onClick = { onOpenCase(game) },
                    onLongClick = { onQuickPlay(game) },
                )
            }
        }
    }
}

@Composable
private fun SystemFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Ascend.accent.copy(alpha = 0.18f) else Tone.Surface)
            .border(1.dp, if (selected) Ascend.accent else Tone.Line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = Inter, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 12.sp),
            color = if (selected) Ascend.accent else Tone.Muted,
        )
    }
}

@Composable
private fun StatsScreenContent(
    games: List<Game>,
    buckets: Map<ShelfBucket, List<Game>>,
    stats: Map<String, PlayStat>,
    onOpenCase: (Game) -> Unit,
) {
    val totalSeconds = remember(stats) { stats.values.sumOf { it.seconds } }
    val totalSessions = remember(stats) { stats.values.sumOf { it.sessions } }
    val playingCount = buckets[ShelfBucket.Playing]?.size ?: 0
    val completedCount = buckets[ShelfBucket.Completed]?.size ?: 0
    val backlogCount = buckets[ShelfBucket.Backlog]?.size ?: 0
    val mostPlayed = remember(games, stats) {
        games.sortedByDescending { stats[it.id]?.seconds ?: 0 }.take(4)
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Stats",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.03).em),
                color = Tone.Ink,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMetricCard("Play Time", if (totalSeconds > 0) PlayTime.format(totalSeconds) else "0m", Modifier.weight(1f))
                StatMetricCard("Sessions", totalSessions.toString(), Modifier.weight(1f))
                StatMetricCard("Library", games.size.toString(), Modifier.weight(1f))
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Shelf Breakdown", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Tone.Ink)
                val total = (playingCount + completedCount + backlogCount).coerceAtLeast(1)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Tone.Raised),
                ) {
                    if (playingCount > 0) Box(Modifier.weight(playingCount.toFloat() / total).fillMaxHeight().background(Ascend.accent))
                    if (completedCount > 0) Box(Modifier.weight(completedCount.toFloat() / total).fillMaxHeight().background(Color(0xFF3FD8A4)))
                    if (backlogCount > 0) Box(Modifier.weight(backlogCount.toFloat() / total).fillMaxHeight().background(Color(0xFF4C5566)))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Playing · $playingCount", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Ascend.accent)
                    Text("Completed · $completedCount", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Color(0xFF3FD8A4))
                    Text("Backlog · $backlogCount", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                }
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item {
                Text("Top Covers", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.5.sp), color = Tone.Ink)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.5.dp)) {
                    mostPlayed.forEach { game ->
                        Box(Modifier.weight(1f)) {
                            TactileCoverTile(game = game, width = null, onClick = { onOpenCase(game) }, onLongClick = {})
                        }
                    }
                    repeat((4 - mostPlayed.size).coerceAtLeast(0)) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp), color = Tone.Ink, maxLines = 1)
        Text(label, style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = Tone.Muted, maxLines = 1)
    }
}

@Composable
private fun SettingsScreenContent(
    gamesCount: Int,
    scanning: Boolean,
    hasFolders: Boolean,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onImportBios: () -> Unit,
    onImportSkin: () -> Unit,
    onToggleTheme: (() -> Unit)?,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Settings",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.03).em),
                color = Tone.Ink,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        item {
            SettingsActionCard(
                icon = Icons.Outlined.CreateNewFolder,
                title = "Add game folder",
                subtitle = "$gamesCount games indexed across 46 supported consoles",
                onClick = onAddFolder,
            )
        }
        if (hasFolders) {
            item {
                SettingsActionCard(
                    icon = Icons.Outlined.Refresh,
                    title = if (scanning) "Scanning library…" else "Rescan folders",
                    subtitle = "Refresh ROMs from your selected folders",
                    onClick = { if (!scanning) onRescan() },
                )
            }
        }
        item {
            SettingsActionCard(
                icon = Icons.Rounded.CardGiftcard,
                title = "Free homebrew games",
                subtitle = "Install openly licensed retro games in one tap",
                onClick = onOpenHomebrew,
            )
        }
        item {
            SettingsActionCard(
                icon = Icons.Outlined.Download,
                title = "Download from link",
                subtitle = "Fetch a ROM or folder from your cloud or server",
                onClick = onOpenDownload,
            )
        }
        item {
            SettingsActionCard(
                icon = Icons.Outlined.Memory,
                title = "Import BIOS files",
                subtitle = "PlayStation, Sega CD, PC Engine CD & Lynx firmware",
                onClick = onImportBios,
            )
        }
        item {
            SettingsActionCard(
                icon = Icons.Outlined.SportsEsports,
                title = "Import controller skin (.deltaskin)",
                subtitle = "Custom portrait & landscape console shells",
                onClick = onImportSkin,
            )
        }
        if (onToggleTheme != null) {
            item {
                SettingsActionCard(
                    icon = Icons.Outlined.Tune,
                    title = if (Tone.isDark) "Appearance: Pitch Dark" else "Appearance: Light",
                    subtitle = "Tap to switch theme mode",
                    onClick = onToggleTheme,
                )
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(icon, null, tint = Ascend.accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp), color = Tone.Ink)
            Text(subtitle, style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
        }
    }
}

@Composable
private fun TopTitleRow(
    title: String,
    addMenuOpen: Boolean,
    scanning: Boolean,
    hasFolders: Boolean,
    onAddMenuToggle: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    horizontalPadding: Dp = 18.dp,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                letterSpacing = (-0.03).em,
            ),
            color = Tone.Ink,
        )

        Box {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, CircleShape)
                    .clickable { onAddMenuToggle(true) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add games", tint = Tone.Ink, modifier = Modifier.size(19.dp))
            }

            DropdownMenu(
                expanded = addMenuOpen,
                onDismissRequest = { onAddMenuToggle(false) },
                shape = RoundedCornerShape(12.dp),
                containerColor = Tone.Surface,
                border = BorderStroke(1.dp, Tone.Line),
            ) {
                DropdownMenuItem(
                    text = { Text("Add game folder", color = Tone.Ink) },
                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null, tint = Ascend.accent) },
                    onClick = { onAddMenuToggle(false); onAddFolder() },
                )
                DropdownMenuItem(
                    text = { Text("Free homebrew games", color = Tone.Ink) },
                    leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null, tint = Ascend.accent) },
                    onClick = { onAddMenuToggle(false); onOpenHomebrew() },
                )
                DropdownMenuItem(
                    text = { Text("Download from link", color = Tone.Ink) },
                    leadingIcon = { Icon(Icons.Outlined.Download, null, tint = Ascend.accent) },
                    onClick = { onAddMenuToggle(false); onOpenDownload() },
                )
                if (hasFolders) {
                    DropdownMenuItem(
                        text = { Text(if (scanning) "Scanning…" else "Rescan library", color = Tone.Ink) },
                        leadingIcon = { Icon(Icons.Outlined.Refresh, null, tint = Ascend.accent) },
                        enabled = !scanning,
                        onClick = { onAddMenuToggle(false); onRescan() },
                    )
                }
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
                    .clip(RoundedCornerShape(11.dp))
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

/**
 * Exact 4-tab bottom bar matching the reference screenshot:
 * Pitch-dark bar, thin top border, and on the active tab a 2.5dp glowing Warm Ember top bar
 * with a soft vertical warm-amber spotlight column behind the active icon & label.
 */
@Composable
private fun EmberBottomTabBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Tone.Surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(thickness = 1.dp, color = Tone.Line)
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            MainTab.entries.forEach { item ->
                val active = item == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (active) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        0f to Ascend.accent.copy(alpha = 0.19f),
                                        0.65f to Ascend.accent.copy(alpha = 0.06f),
                                        1f to Color.Transparent,
                                    ),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(item) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(0.76f)
                                .height(2.5.dp)
                                .background(Ascend.accent),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (active) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (active) Ascend.accent else Tone.Muted,
                            modifier = Modifier.size(21.dp),
                        )
                        Text(
                            text = item.label,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp,
                            ),
                            color = if (active) Ascend.accent else Tone.Muted,
                            maxLines = 1,
                        )
                    }
                }
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
