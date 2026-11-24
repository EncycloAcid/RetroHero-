package com.retrohero.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.emberBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
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
import com.retrohero.app.ui.theme.tag
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
    backEnabled: Boolean = true,
    pageIndex: Int = 4,
) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.All) }
    BackHandler(enabled = backEnabled && tab != LibraryTab.All) { tab = LibraryTab.All }
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) { com.retrohero.app.data.Systems.all.filter { (counts[it.id] ?: 0) > 0 } }
    val unpopulated = remember(counts) { com.retrohero.app.data.Systems.all.filter { (counts[it.id] ?: 0) == 0 } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(94.dp),
        contentPadding = PaddingValues(start = Page.Gutter, end = Page.Gutter, bottom = 24.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().emberBackground(),
    ) {
        span {
            com.retrohero.app.ui.components.PageHeader(if (tab == LibraryTab.All) "Library" else tab.label,
                "${games.size} games · ${systems.size}/${com.retrohero.app.data.Systems.all.size} systems", index = pageIndex) {
                if (tab != LibraryTab.All) com.retrohero.app.ui.components.Key("All library", { tab = LibraryTab.All })
            }
        }
        span {
            com.retrohero.app.ui.components.BentoTelemetryStrip(
                metrics = listOf(
                    com.retrohero.app.ui.components.BentoMetric("${systems.size}/${com.retrohero.app.data.Systems.all.size}", "SYSTEMS", Icons.Rounded.Insights),
                    com.retrohero.app.ui.components.BentoMetric(games.size.toString(), "GAMES", Icons.Rounded.Insights),
                    com.retrohero.app.ui.components.BentoMetric(favorites.size.toString(), "FAVOURITES", Icons.Rounded.Insights),
                    com.retrohero.app.ui.components.BentoMetric(shots.size.toString(), "CAPTURES", Icons.Rounded.Image),
                ),
            )
        }
        span {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LibraryTab.entries.size) { i ->
                    val entry = LibraryTab.entries[i]
                    com.retrohero.app.ui.components.PillChip(
                        text = entry.label,
                        selected = tab == entry,
                        onClick = { tab = entry },
                        selectedStyle = true,
                    )
                }
            }
        }
        when (tab) {
            LibraryTab.All -> {
                span { StatsCard(history, onOpenStats) }
                span {
                    com.retrohero.app.ui.components.BentoPanel(
                        header = "ARCHIVE DIRECTORY",
                        badge = "05 SECTIONS",
                    ) {
                        val entries = LibraryTab.entries.filter { it != LibraryTab.All }
                        entries.forEach { destination ->
                            val (count, subtitleText) = when (destination) {
                                LibraryTab.Systems -> com.retrohero.app.data.Systems.all.size to "${systems.size} active consoles · ${com.retrohero.app.data.Systems.all.size} supported"
                                LibraryTab.Favourites -> favorites.size to "Pinned games for instant access"
                                LibraryTab.Collections -> collections.size to "Custom shelves & curated lists"
                                LibraryTab.Screenshots -> shots.size to "In-game captures & custom covers"
                                else -> history.size to "Chronological play session log"
                            }
                            com.retrohero.app.ui.components.BentoItemRow(
                                title = destination.label,
                                subtitle = subtitleText,
                                rightText = count.toString().padStart(2, '0'),
                                highlighted = destination == LibraryTab.Systems,
                                onClick = { tab = destination },
                                leading = {
                                    ArchiveLabel(
                                        destination.label.take(3).uppercase(),
                                        color = if (destination == LibraryTab.Systems) Ascend.onAccent else com.retrohero.app.ui.theme.Tone.Ink,
                                    )
                                },
                            )
                        }
                    }
                }
                if (favorites.isNotEmpty()) {
                    span {
                        com.retrohero.app.ui.components.SectionTitle(
                            "Pinned",
                            Modifier.padding(top = 8.dp),
                            count = favorites.size,
                            index = 1,
                            verticalPadding = 0.dp,
                        )
                    }
                    items(favorites.take(9), key = { "pin_" + it.id }) { g ->
                        com.retrohero.app.ui.components.GameCard(g, onClick = { onOpenGame(g) }, subtitle = g.system?.shortName, onLongClick = { onOptions(g) }, onPlay = { onPlay(g) })
                    }
                }
                if (systems.isNotEmpty()) {
                    span {
                        com.retrohero.app.ui.components.SectionTitle(
                            "Your Consoles",
                            Modifier.padding(top = 8.dp),
                            count = systems.size,
                            index = 2,
                            onClick = { tab = LibraryTab.Systems },
                            verticalPadding = 0.dp,
                        )
                    }
                    itemsIndexed(systems, key = { _, s -> "all_sys_" + s.id }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, s ->
                        ListRow(
                            index = index + 1,
                            art = { SystemTile(s) },
                            title = s.name,
                            subtitle = "${s.manufacturer} · ${counts[s.id] ?: 0} ${if ((counts[s.id] ?: 0) == 1) "game" else "games"}",
                            onClick = { onOpenSystem(s) },
                        )
                    }
                }
                if (unpopulated.isNotEmpty()) {
                    span {
                        com.retrohero.app.ui.components.SectionTitle(
                            "Supported Systems",
                            Modifier.padding(top = 8.dp),
                            count = com.retrohero.app.data.Systems.all.size,
                            index = 3,
                            onClick = { tab = LibraryTab.Systems },
                            verticalPadding = 0.dp,
                        )
                    }
                    itemsIndexed(unpopulated, key = { _, s -> "cat_sys_" + s.id }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, s ->
                        val exts = s.extensions.take(4).joinToString(" ") { ".$it" }
                        ListRow(
                            index = systems.size + index + 1,
                            art = { SystemTile(s) },
                            title = s.name,
                            subtitle = "${s.manufacturer} · ${s.cores.first().displayName} · $exts",
                            onClick = { onOpenSystem(s) },
                        )
                    }
                }
            }
            LibraryTab.Systems -> {
                if (systems.isNotEmpty()) {
                    span {
                        com.retrohero.app.ui.components.SectionTitle(
                            "In Your Library",
                            Modifier.padding(top = 4.dp),
                            count = systems.size,
                            index = 1,
                            verticalPadding = 0.dp,
                        )
                    }
                    itemsIndexed(systems, key = { _, s -> "sys_" + s.id }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, s ->
                        ListRow(
                            index = index + 1,
                            art = { SystemTile(s) },
                            title = s.name,
                            subtitle = "${s.manufacturer} · ${counts[s.id] ?: 0} ${if ((counts[s.id] ?: 0) == 1) "game" else "games"}",
                            onClick = { onOpenSystem(s) },
                        )
                    }
                }
                if (unpopulated.isNotEmpty()) {
                    span {
                        com.retrohero.app.ui.components.SectionTitle(
                            "All Supported Consoles",
                            Modifier.padding(top = 12.dp),
                            count = unpopulated.size,
                            index = 2,
                            verticalPadding = 0.dp,
                        )
                    }
                    itemsIndexed(unpopulated, key = { _, s -> "sys_avail_" + s.id }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, s ->
                        val exts = s.extensions.take(4).joinToString(" ") { ".$it" }
                        ListRow(
                            index = systems.size + index + 1,
                            art = { SystemTile(s) },
                            title = s.name,
                            subtitle = "${s.manufacturer} · ${s.cores.first().displayName} · $exts",
                            onClick = { onOpenSystem(s) },
                        )
                    }
                }
            }
            LibraryTab.Favourites -> {
                if (favorites.isEmpty()) span { Empty("Long-press a game and pin it, or swipe left on it in a system list.") }
                itemsIndexed(favorites, key = { _, g -> "fav_" + g.id }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, g ->
                    ListRow(
                        index = index + 1,
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
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(Ascend.Radius.Card)).background(com.retrohero.app.ui.theme.Tone.Surface).border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(Ascend.Radius.Card)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Add, null, tint = com.retrohero.app.ui.theme.Tone.Muted, modifier = Modifier.size(36.dp)) }
                        Text("New collection", style = MaterialTheme.typography.titleSmall, color = com.retrohero.app.ui.theme.Tone.Ink, modifier = Modifier.padding(top = 9.dp))
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
                            .clip(RoundedCornerShape(Ascend.Radius.Cover))
                            .background(com.retrohero.app.ui.theme.Tone.Surface)
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
    byDay.entries.forEachIndexed { sectionIndex, (day, list) ->
        span {
            Row(Modifier.padding(start = 12.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                ArchiveLabel((sectionIndex + 1).toString().padStart(2, '0'), color = com.retrohero.app.ui.theme.Tone.Faint)
                Text(
                    day.format(fmt),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                    color = com.retrohero.app.ui.theme.Tone.Ink,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        items(list, key = { "h_${it.game.id}_${it.end}" }, span = { GridItemSpan(maxLineSpan) }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Ascend.Radius.Card))
                    .combinedClickable(onClick = { onOpen(e.game) }, onLongClick = { onOptions(e.game) })
                    .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameArt(e.game, Modifier.size(39.dp).clip(RoundedCornerShape(Ascend.Radius.Cover)), showTag = false)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.game.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${e.game.system?.shortName.orEmpty()} • ${PlayTime.format(e.seconds)} • ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(e.end))}",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                        color = com.retrohero.app.ui.theme.Tone.Muted,
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
            color = com.retrohero.app.ui.theme.Tone.Faint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = com.retrohero.app.ui.theme.Tone.Muted, modifier = Modifier.padding(vertical = 28.dp, horizontal = 4.dp))
}

@Composable
private fun IndexRow(tab: LibraryTab, count: Int, game: Game?, showDivider: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        ArchiveLabel((tab.ordinal + 1).toString().padStart(2, '0'), Modifier.width(24.dp), color = com.retrohero.app.ui.theme.Tone.Faint)
        Spacer(Modifier.width(12.dp))
        val thumbnailShape = RoundedCornerShape(Ascend.Radius.Cover)
        Box(
            Modifier.size(42.dp).clip(thumbnailShape).background(com.retrohero.app.ui.theme.Tone.Raised)
                .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, thumbnailShape),
            contentAlignment = Alignment.Center,
        ) {
            if (game != null) GameArt(game, Modifier.fillMaxSize(), showTag = false)
            else if (tab == LibraryTab.Screenshots && count == 0) {
                Icon(Icons.Rounded.Image, null, tint = com.retrohero.app.ui.theme.Tone.Faint, modifier = Modifier.size(18.dp))
            } else {
                ArchiveLabel("–", color = com.retrohero.app.ui.theme.Tone.Faint)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(tab.label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ArchiveLabel(count.toString().padStart(2, '0'), color = com.retrohero.app.ui.theme.Tone.Muted)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = com.retrohero.app.ui.theme.Tone.Faint, modifier = Modifier.size(18.dp))
    }
    if (showDivider) HorizontalDivider(color = Page.Divider)
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
        ArchiveLabel("01", Modifier.padding(end = 12.dp), color = com.retrohero.app.ui.theme.Tone.Faint)
        Icon(Icons.Rounded.Insights, null, tint = Ascend.accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Play journal", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp), color = com.retrohero.app.ui.theme.Tone.Ink)
            Text(if (week > 0) "${PlayTime.format(week)} this week" else "Play a game to start your stats", style = MaterialTheme.typography.bodySmall, color = com.retrohero.app.ui.theme.Tone.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = com.retrohero.app.ui.theme.Tone.Muted)
    }
}

@Composable
private fun ListRow(index: Int, art: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val cardShape = RoundedCornerShape(Ascend.Radius.Card)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(com.retrohero.app.ui.theme.Tone.Surface)
            .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, cardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbnailShape = RoundedCornerShape(Ascend.Radius.Cover)
        Box(
            Modifier.size(44.dp).clip(thumbnailShape).background(com.retrohero.app.ui.theme.Tone.Raised)
                .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, thumbnailShape),
        ) { art() }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = com.retrohero.app.ui.theme.Tone.Muted, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = com.retrohero.app.ui.theme.Tone.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SystemTile(system: GameSystem) {
    Box(
        Modifier.fillMaxSize().background(com.retrohero.app.ui.theme.Tone.Surface).border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(Ascend.Radius.Card)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            system.tag,
            style = TextStyle(fontFamily = com.retrohero.app.ui.theme.PixelFont, fontSize = 9.sp),
            color = com.retrohero.app.ui.theme.Tone.Ink,
        )
    }
}
