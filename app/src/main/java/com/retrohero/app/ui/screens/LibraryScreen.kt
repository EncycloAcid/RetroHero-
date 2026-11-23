package com.retrohero.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.emberBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.HistoryEntry
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.glossCard
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.theme.Inter
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


enum class LibraryTab(val label: String) { All("All"), Systems("Systems"), Favourites("Favourites"), Collections("Collections"), Screenshots("Screenshots"), History("History") }

/** A screenshot of a game, for the Screenshots tab. */
data class Shot(val game: Game, val file: File)

/** Archive index and category drilldowns. History lists every session by day. */
@Composable
fun LibraryScreen(
    games: List<Game>,
    favorites: List<Game>,
    collections: List<Pair<GameCollection, List<Game>>>,
    shots: List<Shot>,
    history: List<HistoryEntry>,
    onOpenGame: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onOpenSystem: (GameSystem) -> Unit,
    onOpenCollection: (GameCollection) -> Unit,
    onNewCollection: () -> Unit,
    onOpenStats: () -> Unit,
    onPlay: (Game) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.All) }
    BackHandler(enabled = tab != LibraryTab.All) { tab = LibraryTab.All }
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) { com.retrohero.app.data.Systems.all.filter { (counts[it.id] ?: 0) > 0 } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(94.dp),
        contentPadding = PaddingValues(start = Page.Gutter, end = Page.Gutter, bottom = 24.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().emberBackground(),
    ) {
        span {
            com.retrohero.app.ui.components.PageHeader(if (tab == LibraryTab.All) "Library" else tab.label,
                "${games.size} games · ${systems.size} systems") {
                if (tab != LibraryTab.All) TextButton(onClick = { tab = LibraryTab.All }) { Text("All library") }
            }
        }
        when (tab) {
            LibraryTab.All -> {
                span { StatsCard(history, onOpenStats) }
                span {
                    Column {
                        ArchiveLabel("Browse the archive", Modifier.padding(vertical = 12.dp))
                        LibraryTab.entries.filter { it != LibraryTab.All }.forEach { destination ->
                            val (count, art) = when (destination) {
                                LibraryTab.Systems -> systems.size to games.firstOrNull()
                                LibraryTab.Favourites -> favorites.size to favorites.firstOrNull()
                                LibraryTab.Collections -> collections.size to collections.firstNotNullOfOrNull { it.second.firstOrNull() }
                                LibraryTab.Screenshots -> shots.size to shots.firstOrNull()?.game
                                else -> history.size to history.firstOrNull()?.game
                            }
                            IndexRow(destination, count, art) { tab = destination }
                        }
                    }
                }
                if (favorites.isNotEmpty()) {
                    span { com.retrohero.app.ui.components.SectionTitle("Pinned", Modifier.padding(top = 12.dp), count = favorites.size) }
                    items(favorites.take(9), key = { "pin_" + it.id }) { g ->
                        com.retrohero.app.ui.components.GameCard(g, onClick = { onOpenGame(g) }, subtitle = g.system?.shortName, onLongClick = { onOptions(g) }, onPlay = { onPlay(g) })
                    }
                }
            }
            LibraryTab.Systems -> {
                items(systems, key = { "sys_" + it.id }, span = { GridItemSpan(maxLineSpan) }) { s ->
                    ListRow(
                        art = { SystemTile(s) },
                        title = s.name,
                        subtitle = "${s.manufacturer} · ${counts[s.id] ?: 0} ${if ((counts[s.id] ?: 0) == 1) "game" else "games"}",
                        onClick = { onOpenSystem(s) },
                    )
                }
            }
            LibraryTab.Favourites -> {
                if (favorites.isEmpty()) span { Empty("Long-press a game and pin it, or swipe left on it in a system list.") }
                items(favorites, key = { "fav_" + it.id }, span = { GridItemSpan(maxLineSpan) }) { g ->
                    ListRow(
                        art = { GameArt(g, Modifier.fillMaxSize(), showTag = false) },
                        title = g.title,
                        subtitle = g.system?.name.orEmpty(),
                        onClick = { onOpenGame(g) },
                        onLongClick = { onOptions(g) },
                    )
                }
            }
            LibraryTab.Collections -> {
                items(collections, key = { "col_" + it.first.id }) { (c, list) ->
                    Box(Modifier.fillMaxWidth()) { CollectionCard(c, list, onOpenCollection) }
                }
                item(key = "newcol") {
                    Column(Modifier.clickable(onClick = onNewCollection)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF1C1C1F)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Add, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(36.dp)) }
                        Text("New collection", style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.padding(top = 9.dp))
                    }
                }
            }
            LibraryTab.Screenshots -> {
                if (shots.isEmpty()) span { Empty("Take screenshots from the pause menu in a game.") }
                items(shots, key = { "shot_" + it.file.path }) { s ->
                    AsyncImage(
                        model = s.file,
                        contentDescription = "${s.game.title} screenshot",
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .press3D()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1C1C1F))
                            .clickable { onOpenGame(s.game) },
                    )
                }
            }
            LibraryTab.History -> historyItems(history, onOpenGame, onOptions)
        }
    }
}

private fun LazyGridScope.span(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

/** Sessions grouped under full dates, newest first. */
private fun LazyGridScope.historyItems(history: List<HistoryEntry>, onOpen: (Game) -> Unit, onOptions: (Game) -> Unit) {
    if (history.isEmpty()) {
        span { Empty("Nothing yet. Every time you play, it's added here.") }
        return
    }
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    val byDay = history.groupBy { it.day() }
    byDay.forEach { (day, list) ->
        span {
            Text(
                day.format(fmt),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp, top = 22.dp, bottom = 6.dp),
            )
        }
        items(list, key = { "h_${it.game.id}_${it.end}" }, span = { GridItemSpan(maxLineSpan) }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .combinedClickable(onClick = { onOpen(e.game) }, onLongClick = { onOptions(e.game) })
                    .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameArt(e.game, Modifier.size(39.dp).clip(RoundedCornerShape(9.dp)), showTag = false)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.game.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Color.White.copy(alpha = 0.94f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${e.game.system?.shortName.orEmpty()} • ${PlayTime.format(e.seconds)} • ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(e.end))}",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
    span {
        Text(
            "· end of the line ·",
            style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.35f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(vertical = 28.dp, horizontal = 4.dp))
}

@Composable
private fun IndexRow(tab: LibraryTab, count: Int, game: Game?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(Ascend.CardShape).background(Ascend.Surface), contentAlignment = Alignment.Center) {
            if (game != null) GameArt(game, Modifier.fillMaxSize(), showTag = false)
            else ArchiveLabel("–", color = Ascend.Muted)
        }
        Text(tab.label, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium, color = Color.White)
        ArchiveLabel(count.toString().padStart(2, '0'), color = Ascend.Muted)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ascend.Muted)
    }
    HorizontalDivider(color = Page.Divider)
}

@Composable
private fun StatsCard(history: List<HistoryEntry>, onClick: () -> Unit) {
    val week = remember(history) {
        val from = LocalDate.now().minusDays(6)
        history.filter { !it.day().isBefore(from) }.sumOf { it.seconds }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .press3D()
            .glossCard(Ascend.CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Insights, null, tint = Ascend.accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Play journal", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp), color = Color.White)
            Text(if (week > 0) "${PlayTime.format(week)} this week" else "Play a game to start your stats", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun ListRow(art: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))) { art() }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.35f))
    }
}

@Composable
private fun SystemTile(system: GameSystem) {
    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lerp(system.color, Color.White, 0.15f), lerp(system.color, Color.Black, 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(system.shortName.take(4), style = TextStyle(fontFamily = com.retrohero.app.ui.theme.PixelFont, fontSize = 9.sp), color = Color.White)
    }
}
