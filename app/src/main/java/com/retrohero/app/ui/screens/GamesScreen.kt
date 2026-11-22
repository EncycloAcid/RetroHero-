package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.LocalNavSpace
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.graphicsLayer
import com.retrohero.app.ui.components.rememberPressFeedback
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.PixelFont
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import com.retrohero.app.data.Game
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.EmptyState
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.PageHeader
import com.retrohero.app.ui.components.PillChip
import com.retrohero.app.ui.components.BoxTile
import com.retrohero.app.ui.components.glossCard
import com.retrohero.app.ui.components.GameCard

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
    stats: Map<String, com.retrohero.app.data.PlayStat> = emptyMap(),
    favorites: Set<String> = emptySet(),
    title: String = "Search",
    onQuickPlay: ((Game) -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var systemFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(com.retrohero.app.data.GameSort.Name) }
    var onlyFavourites by rememberSaveable { mutableStateOf(false) }
    var onlyUnplayed by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = rememberCoroutineScope()

    if (!hasFolders && games.isEmpty()) {
        EmptyState(
            icon = Icons.Default.SportsEsports,
            title = "Let's find your games",
            body = "Choose the folder where you keep your game files. RetroHero recognises 20+ systems " +
                "and fetches the right emulator when you need it.",
        ) {
            Button(
                onClick = onAddFolder,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.height(ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Default.CreateNewFolder, null)
                Spacer(Modifier.width(8.dp))
                Text("Choose folder", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
            OutlinedButton(onClick = onDownload, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Default.Download, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Download from a link")
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
            icon = Icons.Default.SearchOff,
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
        com.retrohero.app.data.GameSort.apply(
            games.filter {
                (systemFilter == null || it.systemId == systemFilter) &&
                    (q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.fileName.contains(q, ignoreCase = true) ||
                        it.system?.shortName?.equals(q, ignoreCase = true) == true) &&
                    (!onlyFavourites || it.id in favorites) &&
                    (!onlyUnplayed || (stats[it.id]?.sessions ?: 0) == 0)
            },
            sort, stats, recents.map { it.id },
        )
    }
    val browsing = query.isBlank() && systemFilter == null && !onlyFavourites && !onlyUnplayed

    Box(Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        state = grid,
        columns = GridCells.Adaptive(124.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 28.dp, bottom = 24.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth { PageHeader(title, if (games.size == 1) "1 game" else "${games.size} games across ${systemsPresent.size} ${if (systemsPresent.size == 1) "system" else "systems"}") }
        fullWidth { SearchField(query, games.size) { query = it } }

        // Sort and quick filters.
        fullWidth {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item {
                    Box {
                        PillChip(sort.label, selected = false, onClick = { sortMenu = true }, leading = {
                            Icon(Icons.Default.SwapVert, "Sort by", Modifier.padding(end = 6.dp).size(18.dp), tint = Color.White)
                        })
                        DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                            com.retrohero.app.data.GameSort.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.label) },
                                    leadingIcon = { if (s == sort) Icon(Icons.Default.Check, null) },
                                    onClick = { sort = s; sortMenu = false; scope.launch { grid.scrollToItem(0) } },
                                )
                            }
                        }
                    }
                }
                item {
                    PillChip("Favourites", onlyFavourites, { onlyFavourites = !onlyFavourites })
                }
                item {
                    PillChip("Not played yet", onlyUnplayed, { onlyUnplayed = !onlyUnplayed })
                }
            }
        }
        if (showSystemFilter && systemsPresent.size > 1) {
            fullWidth {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { PillChip("All", systemFilter == null, { systemFilter = null }) }
                    items(systemsPresent, key = { it.id }) { system ->
                        PillChip(system.shortName, systemFilter == system.id, { systemFilter = if (systemFilter == system.id) null else system.id }, color = system.color)
                    }
                }
            }
        }

        if (browsing && recents.isNotEmpty()) {
            fullWidth { SectionHeader("Jump back in") }
            fullWidth { RecentCarousel(recents, onPlay, onGameOptions) }
            fullWidth { SectionHeader("Library") }
        }

        if (filtered.isEmpty()) {
            fullWidth {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No games match", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Try a different search, or clear the filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { query = ""; systemFilter = null; onlyFavourites = false; onlyUnplayed = false }) { Text("Clear filters") }
                }
            }
        }
        items(filtered, key = { it.id }) { game ->
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.animateItem()) {
                BoxTile(
                    game, maxWidth * 0.86f,
                    onClick = { onPlay(game) },
                    onLongClick = { onGameOptions(game) },
                    meta = listOfNotNull(game.system?.shortName, stats[game.id]?.takeIf { it.seconds >= 60 }?.let { com.retrohero.app.data.PlayTime.format(it.seconds) }).joinToString(" · "),
                    onPlay = onQuickPlay?.let { { it(game) } },
                )
            }
        }
    }
    // A–Z fast scroller along the right edge when sorted by name.
    if (sort == com.retrohero.app.data.GameSort.Name && filtered.size > 12) {
        val headerItems = 3 + (if (showSystemFilter && systemsPresent.size > 1) 1 else 0) + (if (browsing && recents.isNotEmpty()) 3 else 0)
        AlphabetScroller(
            letters = filtered.map { it.title.firstOrNull()?.uppercaseChar()?.let { c -> if (c.isLetter()) c else '#' } ?: '#' },
            onJump = { index -> scope.launch { grid.scrollToItem(headerItems + index) } },
            modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = LocalNavSpace.current),
        )
    }
    }
}

/** Letters down the right edge: tap or drag to jump to games starting with that letter. */
@Composable
private fun AlphabetScroller(letters: List<Char>, onJump: (Int) -> Unit, modifier: Modifier = Modifier) {
    val present = remember(letters) { letters.distinct() }
    val firstIndex = remember(letters) { present.associateWith { c -> letters.indexOf(c) } }
    var active by remember { mutableStateOf<Char?>(null) }
    val view = androidx.compose.ui.platform.LocalView.current
    Column(
        modifier
            .padding(end = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (active != null) 0.1f else 0.04f))
            .pointerInput(present) {
                fun pick(y: Float) {
                    val i = ((y / size.height) * present.size).toInt().coerceIn(0, present.lastIndex)
                    val c = present[i]
                    if (c != active) {
                        active = c
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        onJump(firstIndex[c] ?: 0)
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        present.forEach { c ->
            Text(
                c.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (c == active) Color.White else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(vertical = 1.dp).semantics { contentDescription = "Jump to $c" },
            )
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun SearchField(query: String, total: Int, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search $total games") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "Clear") }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).glossCard(CircleShape),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun RecentCarousel(recents: List<Game>, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit) {
    val state = rememberCarouselState { recents.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 240.dp,
        itemSpacing = 10.dp,
        modifier = Modifier.fillMaxWidth().height(300.dp),
    ) { index ->
        val game = recents[index]
        val info = carouselItemDrawInfo
        // Text fades out as an item shrinks toward the edge, so it never shows clipped.
        val reveal = {
            val f = ((info.size - info.minSize) / (info.maxSize - info.minSize).coerceAtLeast(1f)).coerceIn(0f, 1f)
            ((f - 0.75f) / 0.25f).coerceIn(0f, 1f) // only the focused (near full-size) item shows text
        }
        val interaction = remember { MutableInteractionSource() }
        val press = rememberPressFeedback(interaction, restCorner = 28.dp, pressedCorner = 28.dp, pressedScale = 0.96f)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = press.scale.value
                    scaleY = press.scale.value
                }
                .maskClip(MaterialTheme.shapes.extraLarge)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onPlay(game) },
                    onLongClick = { onOptions(game) },
                ),
        ) {
            GameArt(game, Modifier.fillMaxSize(), showTag = false)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f)),
                ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer { alpha = reveal() }
                    .padding(18.dp),
            ) {
                Text(
                    game.system?.shortName.orEmpty().uppercase(),
                    style = TextStyle(fontFamily = PixelFont, fontSize = 9.sp, color = game.system?.color ?: Color.White),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer { alpha = reveal() }
                    .padding(14.dp)
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
            }
        }
    }
}

/** Shimmering placeholder cards shown while the first scan runs. */
@Composable
private fun SkeletonGrid() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val tone = MaterialTheme.colorScheme.surfaceContainerHighest
    LazyVerticalGrid(
        columns = GridCells.Adaptive(124.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pulse },
    ) {
        items(12) {
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(0.74f).background(tone, MaterialTheme.shapes.extraLarge))
                Box(Modifier.padding(top = 10.dp).fillMaxWidth(0.8f).height(14.dp).background(tone, CircleShape))
                Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.4f).height(10.dp).background(tone, CircleShape))
            }
        }
    }
}
