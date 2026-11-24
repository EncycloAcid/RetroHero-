package com.retrohero.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.emberBackground
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.tag
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
 * Hardware Consoles & Shelves Deck:
 * A 2-column hardware console shelf with color-coded system cards, featured #1 hardware banner,
 * expandable full 46-console catalogue, and unified segmented views for Shelves, Pinned, Captures, and History.
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
        contentPadding = PaddingValues(start = Page.Gutter, end = Page.Gutter, bottom = 28.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().emberBackground(),
    ) {
        // Minimal Hardware Deck Header (distinct from Home & Search)
        span {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Consoles",
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            letterSpacing = (-0.03).em,
                        ),
                        color = Tone.Ink,
                    )
                    ArchiveLabel(
                        "${activeSystems.size} ACTIVE · ${Systems.all.size} SUPPORTED · ${games.size} ROMS",
                        color = Tone.Muted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onCoverFlow != null && games.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Tone.Surface)
                                .border(1.dp, Tone.Line, RoundedCornerShape(11.dp))
                                .clickable(onClick = onCoverFlow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.ViewCarousel, "3D Shelf", tint = Tone.Ink, modifier = Modifier.size(19.dp))
                        }
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Tone.Ink)
                            .clickable(onClick = onNewCollection),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, "New shelf", tint = Tone.Bg, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Top Segmented Deck Switcher
        span {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                items(LibraryTab.entries) { item ->
                    val selected = item == tab
                    val count = when (item) {
                        LibraryTab.Hardware -> activeSystems.size
                        LibraryTab.Shelves -> collections.size
                        LibraryTab.Favourites -> favorites.size
                        LibraryTab.Screenshots -> shots.size
                        LibraryTab.History -> history.size
                    }
                    val shape = RoundedCornerShape(11.dp)
                    Row(
                        Modifier
                            .height(35.dp)
                            .clip(shape)
                            .background(if (selected) Tone.Ink else Tone.Surface)
                            .border(1.dp, if (selected) Tone.Ink else Tone.Line, shape)
                            .clickable { tab = item }
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (selected) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Ascend.accent))
                        }
                        Text(
                            item.label,
                            style = TextStyle(fontFamily = Inter, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp),
                            color = if (selected) Tone.Bg else Tone.Ink,
                        )
                        if (count > 0 && selected) {
                            Text(
                                count.toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Tone.Bg.copy(alpha = 0.72f),
                            )
                        }
                    }
                }
            }
        }

        when (tab) {
            LibraryTab.Hardware -> {
                if (activeSystems.isEmpty()) {
                    span {
                        DeckEmptyCard(
                            title = "No console ROMs loaded yet",
                            body = "Add a game folder or grab free homebrew from Play to populate your hardware deck.",
                        )
                    }
                } else {
                    // Featured #1 Active Hardware Banner
                    val lead = activeSystems.first()
                    val leadGames = games.filter { it.systemId == lead.id }
                    span {
                        FeaturedHardwareBanner(
                            system = lead,
                            games = leadGames,
                            onClick = { onOpenSystem(lead) },
                        )
                    }

                    // 2-Column Hardware Console Deck Grid
                    items(activeSystems, key = { "hw_${it.id}" }) { sys ->
                        HardwareConsoleCard(
                            system = sys,
                            count = counts[sys.id] ?: 0,
                            onClick = { onOpenSystem(sys) },
                        )
                    }
                }

                // Expandable Supported Hardware Catalogue (all remaining consoles)
                if (unpopulated.isNotEmpty()) {
                    span {
                        val toggleShape = RoundedCornerShape(14.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(toggleShape)
                                .background(Tone.Surface)
                                .border(1.dp, Tone.Line, toggleShape)
                                .clickable { showAllSupported = !showAllSupported }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "All Supported Hardware",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = Tone.Ink,
                                )
                                ArchiveLabel("${unpopulated.size} ADDITIONAL CONSOLES READY FOR ROMS", color = Tone.Muted)
                            }
                            Icon(
                                if (showAllSupported) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null,
                                tint = Tone.Ink,
                            )
                        }
                    }
                    if (showAllSupported) {
                        items(unpopulated, key = { "supp_${it.id}" }) { sys ->
                            SupportedHardwareChipCard(sys) { onOpenSystem(sys) }
                        }
                    }
                }
            }

            LibraryTab.Shelves -> {
                // Smart Shelves + Custom Collections as 2-Column Bento Cards
                if (recents.isNotEmpty()) {
                    item(key = "smart_recent") {
                        ShelfGridCard(
                            title = "Recently Played",
                            count = recents.size,
                            games = recents,
                            accent = Ascend.accent,
                            onClick = { onOpenGame(recents.first()) },
                        )
                    }
                }
                if (unplayed.isNotEmpty()) {
                    item(key = "smart_backlog") {
                        ShelfGridCard(
                            title = "Backlog",
                            count = unplayed.size,
                            games = unplayed,
                            accent = Color(0xFF4F6AF6),
                            onClick = { onOpenGame(unplayed.first()) },
                        )
                    }
                }
                items(collections, key = { "col_${it.first.id}" }) { (col, colGames) ->
                    ShelfGridCard(
                        title = col.name,
                        count = colGames.size,
                        games = colGames,
                        accent = Tone.Ink,
                        onClick = { onOpenCollection(col) },
                    )
                }
                item(key = "create_shelf_tile") {
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(shape)
                            .background(Tone.Surface)
                            .border(1.dp, Tone.Line, shape)
                            .clickable(onClick = onNewCollection)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Ascend.accent.copy(alpha = 0.14f))
                                .border(1.dp, Ascend.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Ascend.accent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "New Shelf",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = Tone.Ink,
                        )
                        ArchiveLabel("CURATE GAMES", color = Tone.Muted)
                    }
                }
            }

            LibraryTab.Favourites -> {
                if (favorites.isEmpty()) {
                    span {
                        DeckEmptyCard(
                            title = "No pinned games yet",
                            body = "Pin your favourite classics from any Game Record page for instant access here.",
                        )
                    }
                } else {
                    items(favorites, key = { "pin_${it.id}" }) { game ->
                        TwoColGameCard(game, onOpenGame, onPlay, onOptions)
                    }
                }
            }

            LibraryTab.Screenshots -> {
                if (shots.isEmpty()) {
                    span {
                        DeckEmptyCard(
                            title = "No captures saved yet",
                            body = "Snap screenshots from the in-game pause menu to build your gallery and custom covers.",
                        )
                    }
                } else {
                    items(shots, key = { it.file.absolutePath }) { shot ->
                        val shape = RoundedCornerShape(14.dp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(Tone.Surface)
                                .border(1.dp, Tone.Line, shape)
                                .clickable { onOpenGame(shot.game) },
                        ) {
                            AsyncImage(
                                model = shot.file,
                                contentDescription = shot.game.title,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.None,
                                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                            )
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    shot.game.title,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = Tone.Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                ArchiveLabel(shot.game.system?.shortName ?: shot.game.systemId, color = Tone.Muted)
                            }
                        }
                    }
                }
            }

            LibraryTab.History -> {
                if (history.isEmpty()) {
                    span {
                        DeckEmptyCard(
                            title = "No play history yet",
                            body = "Launch any game to start logging sessions, play time, and console stats.",
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
                                LocalDate.now() -> "TODAY"
                                LocalDate.now().minusDays(1) -> "YESTERDAY"
                                else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).uppercase()
                            }
                            ArchiveLabel(label, Modifier.padding(top = 6.dp, bottom = 2.dp), color = Tone.Muted)
                        }
                        entries.forEach { entry ->
                            span {
                                val game = entry.game
                                val rowShape = RoundedCornerShape(14.dp)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(rowShape)
                                        .background(Tone.Surface)
                                        .border(1.dp, Tone.Line, rowShape)
                                        .clickable { onOpenGame(game) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    GameArt(
                                        game,
                                        Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)).border(1.dp, Tone.Line, RoundedCornerShape(9.dp)),
                                        showTag = false,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            game.title,
                                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                            color = Tone.Ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        ArchiveLabel(
                                            "${game.system?.shortName ?: game.systemId} · ${PlayTime.format(entry.seconds)}",
                                            color = Tone.Muted,
                                        )
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Faint, modifier = Modifier.size(18.dp))
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
private fun FeaturedHardwareBanner(
    system: GameSystem,
    games: List<Game>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(114.dp)
            .clip(shape)
            .background(Tone.Ink)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(7.dp).fillMaxHeight().background(system.color))
        Column(
            Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Ascend.accent))
                ArchiveLabel("TOP CONSOLE · ${system.shortName}", color = Tone.Bg.copy(alpha = 0.72f))
            }
            Text(
                system.name,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 17.sp, lineHeight = 19.sp, letterSpacing = (-0.02).em),
                color = Tone.Bg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ArchiveLabel("${system.manufacturer} · ${games.size} TITLES READY", color = Ascend.accent)
        }
        // Mini 2-cover preview on the right
        Row(
            Modifier.padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy((-14).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            games.take(2).forEach { game ->
                GameArt(
                    game,
                    Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(1.5.dp, Tone.Ink, RoundedCornerShape(11.dp)),
                    showTag = false,
                )
            }
        }
    }
}

@Composable
private fun HardwareConsoleCard(
    system: GameSystem,
    count: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(122.dp)
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onClick),
    ) {
        // Signature console brand color top stripe
        Box(Modifier.fillMaxWidth().height(5.dp).background(system.color))
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Tone.Raised)
                        .border(1.dp, Tone.Line, RoundedCornerShape(7.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                    Text(
                        system.tag,
                        style = TextStyle(fontFamily = PixelFont, fontSize = 9.sp),
                        color = Tone.Ink,
                    )
                }
                Text(
                    count.toString().padStart(2, '0'),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    ),
                    color = Tone.Ink,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    system.name,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, lineHeight = 16.sp, letterSpacing = (-0.01).em),
                    color = Tone.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ArchiveLabel(system.manufacturer, color = Tone.Muted)
            }
        }
    }
}

@Composable
private fun SupportedHardwareChipCard(system: GameSystem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface.copy(alpha = 0.7f))
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(system.color))
        Column(Modifier.weight(1f)) {
            Text(
                system.name,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ArchiveLabel(system.shortName, color = Tone.Faint)
        }
    }
}

@Composable
private fun ShelfGridCard(
    title: String,
    count: Int,
    games: List<Game>,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Tone.Bg)) {
            val covers = games.take(4)
            when {
                covers.size >= 4 -> Column(Modifier.fillMaxSize()) {
                    for (r in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..1) GameArt(covers[r * 2 + c], Modifier.weight(1f).fillMaxHeight(), showTag = false)
                    }
                }
                covers.isNotEmpty() -> GameArt(covers.first(), Modifier.fillMaxSize(), showTag = false)
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                ArchiveLabel("$count", color = Color.White)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.Line))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TwoColGameCard(
    game: Game,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onOptions: (Game) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .combinedClickable(onClick = { onOpen(game) }, onLongClick = { onOptions(game) }),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.1f).background(Tone.Bg)) {
            GameArt(game, Modifier.fillMaxSize(), showTag = false)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Ascend.accent)
                    .clickable { onPlay(game) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = Ascend.onAccent, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ArchiveLabel(game.system?.shortName ?: game.systemId, color = Tone.Muted)
        }
    }
}

@Composable
private fun DeckEmptyCard(title: String, body: String) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Tone.Ink)
        Text(body, style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp), color = Tone.Muted)
    }
}

private fun LazyGridScope.span(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
