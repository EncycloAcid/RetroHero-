package com.retrohero.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.HistoryEntry
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.GameCard
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class LibraryTab(val label: String) {
    Hardware("Hardware"),
    Shelves("Shelves"),
    Favourites("Pinned"),
    Screenshots("Captures"),
    History("History"),
}

/** A screenshot of a game, for the Captures tab. */
data class Shot(val game: Game, val file: File)

/**
 * Handheld Console OS Consoles screen:
 * Slate hardware cards with fanned cover art, console switcher pills, shelves, pinned titles, captures, and session history.
 */
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
    pageIndex: Int = 2,
    recents: List<Game> = emptyList(),
    stats: Map<String, PlayStat> = emptyMap(),
    onCoverFlow: (() -> Unit)? = null,
) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.Hardware) }
    var showAllSupported by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = backEnabled && tab != LibraryTab.Hardware) { tab = LibraryTab.Hardware }

    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val gamesBySystem = remember(games) { games.groupBy { it.systemId } }
    val activeSystems = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }
    val unpopulated = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) == 0 }
    }
    val unplayed = remember(games, stats) {
        games.filter { (stats[it.id]?.sessions ?: 0) == 0 }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().background(Tone.Bg),
    ) {
        // Console OS header: hardware count eyebrow, bold title, round console actions.
        span {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "${activeSystems.size} active · ${Systems.all.size} supported",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, letterSpacing = 0.02.em),
                        color = Tone.Muted,
                    )
                    Text(
                        "Consoles",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.03).em),
                        color = Tone.Ink,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onCoverFlow != null && games.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Tone.Surface)
                                .border(1.dp, Tone.Line, CircleShape)
                                .clickable(onClick = onCoverFlow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.ViewCarousel, "3D Shelf", tint = Tone.Ink, modifier = Modifier.size(18.dp))
                        }
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Ascend.accent)
                            .clickable(onClick = onNewCollection),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, "New shelf", tint = Ascend.onAccent, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }

        // Console OS section chips.
        span {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            ) {
                items(LibraryTab.entries.size) { i ->
                    val item = LibraryTab.entries[i]
                    val selected = item == tab
                    Box(
                        Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(if (selected) Tone.Ink else Tone.Surface)
                            .then(if (selected) Modifier else Modifier.border(1.dp, Tone.Line, CircleShape))
                            .clickable { tab = item }
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.label,
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = if (selected) Tone.Bg else Tone.Ink,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        when (tab) {
            LibraryTab.Hardware -> {
                if (activeSystems.isEmpty()) {
                    span {
                        MonoEmptyMessage(
                            title = "No console games yet",
                            body = "Add a game folder or browse free homebrew from Play to populate your consoles.",
                        )
                    }
                } else {
                    // Steam Deck / Switch hardware cards: cover fan, console badge, ROM count.
                    activeSystems.forEach { sys ->
                        val sysGames = gamesBySystem[sys.id].orEmpty()
                        item(key = "hw_card_${sys.id}") {
                            ConsoleHardwareCard(
                                system = sys,
                                games = sysGames,
                                onClick = { onOpenSystem(sys) },
                            )
                        }
                    }
                }

                // Expandable All 46 Supported Consoles Directory
                if (unpopulated.isNotEmpty()) {
                    span {
                        Column(Modifier.padding(top = 6.dp)) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.Line))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showAllSupported = !showAllSupported }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        "All supported hardware",
                                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                                        color = Tone.Ink,
                                    )
                                    Text(
                                        "${unpopulated.size} more systems ready for ROMs",
                                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                                        color = Tone.Muted,
                                    )
                                }
                                Icon(
                                    if (showAllSupported) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    null,
                                    tint = Tone.Muted,
                                )
                            }
                        }
                    }
                    if (showAllSupported) {
                        items(unpopulated, key = { "supp_${it.id}" }) { sys ->
                            MonoSupportedTile(sys) { onOpenSystem(sys) }
                        }
                    }
                }
            }

            LibraryTab.Shelves -> {
                if (recents.isNotEmpty()) {
                    item(key = "smart_recent") {
                        MonoShelfTile(
                            title = "Recently Played",
                            count = recents.size,
                            games = recents,
                            onClick = { onOpenGame(recents.first()) },
                        )
                    }
                }
                if (unplayed.isNotEmpty()) {
                    item(key = "smart_backlog") {
                        MonoShelfTile(
                            title = "Backlog",
                            count = unplayed.size,
                            games = unplayed,
                            onClick = { onOpenGame(unplayed.first()) },
                        )
                    }
                }
                items(collections, key = { "col_${it.first.id}" }) { (col, colGames) ->
                    MonoShelfTile(
                        title = col.name,
                        count = colGames.size,
                        games = colGames,
                        onClick = { onOpenCollection(col) },
                    )
                }
                item(key = "create_shelf_tile") {
                    val shape = RoundedCornerShape(Ascend.Radius.Card)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.15f)
                            .clip(shape)
                            .background(Tone.Surface)
                            .border(1.dp, Tone.Line, shape)
                            .clickable(onClick = onNewCollection)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Tone.Ink, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "New Shelf",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                            color = Tone.Ink,
                        )
                    }
                }
            }

            LibraryTab.Favourites -> {
                if (favorites.isEmpty()) {
                    span {
                        MonoEmptyMessage(
                            title = "No pinned games yet",
                            body = "Pin classics from any game page for instant access here.",
                        )
                    }
                } else {
                    items(favorites, key = { "pin_${it.id}" }) { game ->
                        GameCard(
                            game = game,
                            onClick = { onOpenGame(game) },
                            subtitle = game.system?.shortName,
                            onLongClick = { onOptions(game) },
                            onPlay = { onPlay(game) },
                        )
                    }
                }
            }

            LibraryTab.Screenshots -> {
                if (shots.isEmpty()) {
                    span {
                        MonoEmptyMessage(
                            title = "No captures saved yet",
                            body = "Snap screenshots from the in-game menu to build your gallery.",
                        )
                    }
                } else {
                    items(shots, key = { it.file.absolutePath }) { shot ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenGame(shot.game) },
                        ) {
                            AsyncImage(
                                model = shot.file,
                                contentDescription = shot.game.title,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.None,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.75.dp, Tone.Line, RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                shot.game.title,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                color = Tone.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                shot.game.system?.shortName ?: shot.game.systemId,
                                style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                                color = Tone.Muted,
                            )
                        }
                    }
                }
            }

            LibraryTab.History -> {
                if (history.isEmpty()) {
                    span {
                        MonoEmptyMessage(
                            title = "No play history yet",
                            body = "Launch any game to start logging sessions and play time.",
                        )
                    }
                } else {
                    val byDay = history.groupBy {
                        java.time.Instant.ofEpochMilli(it.end)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    byDay.forEach { (day, entries) ->
                        span {
                            val label = when (day) {
                                LocalDate.now() -> "Today"
                                LocalDate.now().minusDays(1) -> "Yesterday"
                                else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            }
                            Text(
                                label,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                color = Tone.Muted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        entries.forEachIndexed { i, entry ->
                            span {
                                val game = entry.game
                                Column(Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenGame(game) }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        GameArt(
                                            game,
                                            Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(0.75.dp, Tone.Line, RoundedCornerShape(6.dp)),
                                            showTag = false,
                                        )
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                game.title,
                                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                                color = Tone.Ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "${game.system?.shortName ?: game.systemId} · ${PlayTime.format(entry.seconds)}",
                                                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                                                color = Tone.Muted,
                                            )
                                        }
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Faint, modifier = Modifier.size(18.dp))
                                    }
                                    if (i < entries.lastIndex) {
                                        Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(Tone.Line))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleHardwareCard(
    system: GameSystem,
    games: List<Game>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Ascend.Radius.Card)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .background(Tone.Raised),
        ) {
            val covers = games.take(3)
            if (covers.isNotEmpty()) {
                GameArt(covers.first(), Modifier.fillMaxSize().graphicsLayer { alpha = 0.35f }, showTag = false)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Tone.Surface))))
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy((-18).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    covers.forEachIndexed { i, g ->
                        val tileShape = RoundedCornerShape(8.dp)
                        GameArt(
                            g,
                            Modifier
                                .size(if (i == 0) 64.dp else 54.dp)
                                .graphicsLayer { rotationZ = (i - 1) * 6f }
                                .clip(tileShape)
                                .border(1.5.dp, Tone.Surface, tileShape),
                            showTag = false,
                        )
                    }
                }
            }
            Text(
                system.shortName,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 0.08.em),
                color = Ascend.onAccent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Ascend.accent)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                system.name,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${if (games.size == 1) "1 game" else "${games.size} games"} · ${system.manufacturer}",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                color = Tone.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonoConsoleRow(
    system: GameSystem,
    games: List<Game>,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            // Real game box art thumbnail carries the visual identity
            val artShape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .size(50.dp)
                    .clip(artShape)
                    .background(Tone.Surface)
                    .border(0.75.dp, Tone.Line, artShape),
                contentAlignment = Alignment.Center,
            ) {
                val lead = games.firstOrNull()
                if (lead != null) {
                    GameArt(lead, Modifier.fillMaxSize(), showTag = false)
                } else {
                    Text(
                        system.shortName,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Tone.Muted,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    system.name,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = Tone.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${system.shortName} · ${system.manufacturer}",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = Tone.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                games.size.toString(),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = Tone.Muted,
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Faint, modifier = Modifier.size(18.dp))
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 63.dp).height(1.dp).background(Tone.Line))
        }
    }
}

@Composable
private fun MonoSupportedTile(system: GameSystem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            system.name,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = Tone.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            system.shortName,
            style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
            color = Tone.Muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonoShelfTile(
    title: String,
    count: Int,
    games: List<Game>,
    onClick: () -> Unit,
) {
    val artShape = RoundedCornerShape(Ascend.Radius.Card)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(artShape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, artShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(artShape)
                .background(Tone.Surface)
                .border(0.75.dp, Tone.Line, artShape),
        ) {
            val covers = games.take(4)
            when {
                covers.size >= 4 -> Column(Modifier.fillMaxSize()) {
                    for (r in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..1) GameArt(covers[r * 2 + c], Modifier.weight(1f).fillMaxHeight(), showTag = false)
                    }
                }
                covers.isNotEmpty() -> GameArt(covers.first(), Modifier.fillMaxSize(), showTag = false)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
            color = Tone.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (count == 1) "1 game" else "$count games",
            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
            color = Tone.Muted,
        )
    }
}

@Composable
private fun MonoEmptyMessage(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.5.sp), color = Tone.Ink)
        Text(body, style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 20.sp), color = Tone.Muted)
    }
}

private fun LazyGridScope.span(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
