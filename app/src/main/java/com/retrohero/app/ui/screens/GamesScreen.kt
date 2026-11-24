package com.retrohero.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSort
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.EmptyState
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.GameCard
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.PillChip
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import kotlinx.coroutines.launch

/**
 * Pure Minimal Monochrome Search screen:
 * Flat OLED/white canvas, crisp search bar, flat monochrome console chips,
 * default dense 3-column pure box-art wall (toggleable to flat hairline list), and A–Z scrubber.
 */
@Composable
fun GamesScreen(
    games: List<Game>,
    recents: List<Game>,
    hasFolders: Boolean,
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onDownload: () -> Unit,
    onPlay: (Game) -> Unit,
    onGameOptions: (Game) -> Unit,
    showSystemFilter: Boolean = true,
    stats: Map<String, PlayStat> = emptyMap(),
    favorites: Set<String> = emptySet(),
    title: String = "Search",
    onQuickPlay: ((Game) -> Unit)? = null,
    pageIndex: Int = 3,
    onHomebrew: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var systemFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(GameSort.Name) }
    var onlyFavourites by rememberSaveable { mutableStateOf(false) }
    var onlyUnplayed by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var gridMode by rememberSaveable { mutableStateOf(true) }
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = rememberCoroutineScope()

    if (!hasFolders && games.isEmpty()) {
        EmptyState(
            title = "Start your library",
            body = "Add a folder of games to build your first shelf. RetroHero finds the system and sets up its emulator when you play.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                com.retrohero.app.ui.components.AccentButton("Choose game folder", onAddFolder, Modifier.fillMaxWidth(), icon = Icons.Default.CreateNewFolder)
                if (onHomebrew != null) {
                    com.retrohero.app.ui.components.Key("Browse free homebrew", onHomebrew, Modifier.fillMaxWidth())
                }
                com.retrohero.app.ui.components.Key("Download from a link", onDownload, Modifier.fillMaxWidth(), icon = Icons.Default.Download)
            }
        }
        return
    }
    if (games.isEmpty() && scanning) {
        SkeletonGrid()
        return
    }
    if (games.isEmpty() && !scanning) {
        EmptyState(
            title = "No games yet",
            body = "Supported files include .nes .sfc .gba .md .cue .chd .zip and more. " +
                "Tip: name folders after the system, like \"psx\" or \"genesis\".",
        )
        return
    }

    val systemsPresent = remember(games) {
        val ids = games.mapTo(HashSet()) { it.systemId }
        Systems.all.filter { it.id in ids }
    }
    val filtered = remember(games, query, systemFilter, sort, onlyFavourites, onlyUnplayed, stats, favorites, recents) {
        val q = query.trim()
        GameSort.apply(
            games.filter {
                (systemFilter == null || it.systemId == systemFilter) &&
                    (q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.fileName.contains(q, ignoreCase = true) ||
                        it.system?.shortName?.equals(q, ignoreCase = true) == true ||
                        it.system?.name?.contains(q, ignoreCase = true) == true) &&
                    (!onlyFavourites || it.id in favorites) &&
                    (!onlyUnplayed || (stats[it.id]?.sessions ?: 0) == 0)
            },
            sort, stats, recents.map { it.id },
        )
    }
    val filtersActive = systemFilter != null || onlyFavourites || onlyUnplayed || sort != GameSort.Name
    val showScroller = sort == GameSort.Name && filtered.size > 12

    Box(Modifier.fillMaxSize().background(Tone.Bg)) {
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = if (showScroller) 28.dp else 16.dp, bottom = 28.dp + LocalNavSpace.current),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidth {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 12.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                title,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.03).em),
                                color = Tone.Ink,
                            )
                            Text(
                                "${filtered.size} of ${games.size} titles · ${sort.label}",
                                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                                color = Tone.Muted,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Tone.Surface)
                                    .border(1.dp, Tone.Line, CircleShape)
                                    .clickable { gridMode = !gridMode },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (gridMode) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                                    if (gridMode) "Switch to list view" else "Switch to grid view",
                                    tint = Tone.Ink,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            val active = filtersOpen || filtersActive
                            Row(
                                Modifier
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Ascend.accent else Tone.Surface)
                                    .then(if (active) Modifier else Modifier.border(1.dp, Tone.Line, CircleShape))
                                    .clickable { filtersOpen = !filtersOpen }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    null,
                                    tint = if (active) Ascend.onAccent else Tone.Ink,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    "Filters",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                    color = if (active) Ascend.onAccent else Tone.Ink,
                                )
                            }
                        }
                    }

                    MonoSearchField(query, games.size) { query = it }

                    if (showSystemFilter && systemsPresent.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                MonoFilterChip(
                                    label = "All",
                                    count = games.size,
                                    selected = systemFilter == null,
                                    onClick = { systemFilter = null },
                                )
                            }
                            items(systemsPresent, key = { it.id }) { sys ->
                                MonoFilterChip(
                                    label = sys.shortName,
                                    count = null,
                                    selected = systemFilter == sys.id,
                                    onClick = { systemFilter = if (systemFilter == sys.id) null else sys.id },
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = filtersOpen) {
                        val panelShape = RoundedCornerShape(10.dp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(panelShape)
                                .background(Tone.Surface)
                                .border(1.dp, Tone.Line, panelShape)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Sort",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                color = Tone.Muted,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(GameSort.entries) { option ->
                                    PillChip(option.label, sort == option, { sort = option }, selectedStyle = true)
                                }
                            }
                            Text(
                                "Filter",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                color = Tone.Muted,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillChip("Favourites", onlyFavourites, { onlyFavourites = !onlyFavourites }, selectedStyle = true)
                                PillChip("Not played yet", onlyUnplayed, { onlyUnplayed = !onlyUnplayed }, selectedStyle = true)
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                fullWidth {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "No matching games",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = Tone.Ink,
                        )
                        Text(
                            "Try another search term or clear active filters.",
                            style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                            color = Tone.Muted,
                        )
                    }
                }
            } else if (gridMode) {
                // Default: Dense 3-Column Pure Box-Art Wall
                items(filtered, key = { "grid_${it.id}" }) { game ->
                    GameCard(
                        game = game,
                        onClick = { onPlay(game) },
                        subtitle = game.system?.shortName,
                        onLongClick = { onGameOptions(game) },
                        onPlay = onQuickPlay?.let { qp -> { qp(game) } } ?: { onPlay(game) },
                    )
                }
            } else {
                // Alternate: Ultra-Clean Flat Hairline List (no cards around rows)
                itemsIndexed(
                    items = filtered,
                    key = { _, it -> "row_${it.id}" },
                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                ) { idx, game ->
                    MonoCatalogueRow(
                        game = game,
                        isFavorite = game.id in favorites,
                        stat = stats[game.id],
                        showDivider = idx < filtered.lastIndex,
                        onOpen = { onPlay(game) },
                        onQuickPlay = { (onQuickPlay ?: onPlay)(game) },
                        onLongClick = { onGameOptions(game) },
                    )
                }
            }
        }

        if (showScroller) {
            AlphaScroller(
                games = filtered,
                modifier = Modifier.align(Alignment.CenterEnd),
                onJump = { idx -> scope.launch { grid.scrollToItem(idx + 1) } },
            )
        }
    }
}

@Composable
private fun MonoSearchField(query: String, total: Int, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = {
            Text(
                "Search $total games or consoles…",
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                color = Tone.Muted,
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Tone.Muted, modifier = Modifier.size(19.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, "Clear", tint = Tone.Muted)
                }
            }
        },
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Tone.Surface,
            unfocusedContainerColor = Tone.Surface,
            focusedTextColor = Tone.Ink,
            unfocusedTextColor = Tone.Ink,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Tone.Line, shape),
    )
}

@Composable
private fun MonoFilterChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) Ascend.accent else Tone.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, Tone.Line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp),
            color = if (selected) Ascend.onAccent else Tone.Ink,
        )
        if (count != null) {
            Text(
                count.toString(),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = if (selected) Ascend.onAccent.copy(alpha = 0.72f) else Tone.Muted,
            )
        }
    }
}

@Composable
private fun MonoCatalogueRow(
    game: Game,
    isFavorite: Boolean,
    stat: PlayStat?,
    showDivider: Boolean,
    onOpen: () -> Unit,
    onQuickPlay: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpen, onLongClick = onLongClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameArt(
                game,
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.75.dp, Tone.Line, RoundedCornerShape(6.dp)),
                showTag = false,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        game.title,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                        color = Tone.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isFavorite) {
                        Icon(Icons.Rounded.Favorite, "Pinned", tint = Tone.Ink, modifier = Modifier.size(12.dp))
                    }
                }
                val detail = buildString {
                    append(game.system?.shortName ?: game.systemId)
                    if (stat != null && stat.seconds >= 60) {
                        append(" · ")
                        append(PlayTime.format(stat.seconds))
                    } else {
                        append(" · ")
                        append(game.fileName.substringAfterLast('.').uppercase())
                    }
                }
                Text(detail, style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, CircleShape)
                    .clickable(onClick = onQuickPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    "Play ${game.title}",
                    tint = Tone.Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 58.dp).height(1.dp).background(Tone.Line))
        }
    }
}

@Composable
private fun AlphaScroller(games: List<Game>, modifier: Modifier = Modifier, onJump: (Int) -> Unit) {
    val firstIndex = remember(games) {
        buildMap<Char, Int> {
            games.forEachIndexed { i, g ->
                val c = g.title.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
                putIfAbsent(c, i)
            }
        }
    }
    val present = remember(firstIndex) { firstIndex.keys.sortedBy { if (it == '#') ' ' else it } }
    if (present.size < 4) return
    val jump by rememberUpdatedState(onJump)
    var active by remember { mutableStateOf<Char?>(null) }
    val view = androidx.compose.ui.platform.LocalView.current
    Column(
        modifier
            .padding(end = 3.dp)
            .clip(CircleShape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, CircleShape)
            .pointerInput(present) {
                fun pick(y: Float) {
                    val i = ((y / size.height) * present.size).toInt().coerceIn(0, present.lastIndex)
                    val c = present[i]
                    if (c != active) {
                        active = c
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        jump(firstIndex[c] ?: 0)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pick(down.position.y)
                    do {
                        val e = awaitPointerEvent()
                        e.changes.forEach { if (it.pressed) { pick(it.position.y); it.consume() } }
                    } while (e.changes.any { it.pressed })
                    active = null
                }
            }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        present.forEach { c ->
            Text(
                c.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (c == active) Tone.Ink else Tone.Muted,
                modifier = Modifier.padding(vertical = 0.5.dp).semantics { contentDescription = "Jump to $c" },
            )
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

/** Shimmering placeholder cards shown while the first scan runs. */
@Composable
private fun SkeletonGrid() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 88.dp, bottom = 24.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().background(Tone.Bg),
    ) {
        items(9) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.76f)
                    .graphicsLayer { alpha = pulse }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Tone.Surface),
            )
        }
    }
}
