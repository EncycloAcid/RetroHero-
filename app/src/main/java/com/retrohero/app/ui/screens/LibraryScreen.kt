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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.lerp
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
import com.retrohero.app.ui.components.GlassOrb
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.auroraBackdrop
import com.retrohero.app.ui.components.glassPanel
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
 * Midnight Glass & Console Aurora Consoles screen:
 * Full-width luminous Console World banners with console-colored glass auras and overlapping poster fans,
 * plus glass pill switchers for Shelves, Pinned posters, Captures, and Play History.
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

    val auraPrimary = activeSystems.firstOrNull()?.color ?: Ascend.accent
    val auraSecondary = lerp(auraPrimary, Color(0xFF7B5CFF), 0.55f)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .auroraBackdrop(auraPrimary, auraSecondary),
    ) {
        // Floating Midnight Glass Header
        span {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 14.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Consoles",
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.03).em,
                        ),
                        color = Tone.Ink,
                    )
                    Text(
                        "${activeSystems.size} active · ${Systems.all.size} supported · ${games.size} games",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                        color = Tone.Muted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onCoverFlow != null && games.isNotEmpty()) {
                        GlassOrb(
                            icon = Icons.Rounded.ViewCarousel,
                            contentDescription = "3D Shelf",
                            onClick = onCoverFlow,
                            size = 40.dp,
                        )
                    }
                    GlassOrb(
                        icon = Icons.Rounded.Add,
                        contentDescription = "New shelf",
                        onClick = onNewCollection,
                        size = 40.dp,
                        accent = true,
                    )
                }
            }
        }

        // Translucent Glass Pill Tab Switcher (all 5 tabs fit comfortably on 411dp)
        span {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                items(LibraryTab.entries) { item ->
                    val selected = item == tab
                    val pillMod = if (selected) {
                        Modifier
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Ascend.accent, Color(0xFF7B68FF))))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    } else {
                        Modifier.glassPanel(CircleShape)
                    }
                    Box(
                        Modifier
                            .height(34.dp)
                            .then(pillMod)
                            .clickable { tab = item }
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.label,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            color = if (selected) Color.White else Tone.Ink,
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
                        GlassEmptyCard(
                            title = "No console games loaded yet",
                            body = "Add a game folder or grab free homebrew from Play to illuminate your console worlds.",
                        )
                    }
                } else {
                    // Sweeping 1-column Console World Banner Cards
                    activeSystems.forEach { sys ->
                        val sysGames = gamesBySystem[sys.id].orEmpty()
                        item(key = "world_banner_${sys.id}", span = { GridItemSpan(maxLineSpan) }) {
                            ConsoleWorldBanner(
                                system = sys,
                                games = sysGames,
                                onClick = { onOpenSystem(sys) },
                            )
                        }
                    }
                }

                // Collapsible 46-System Supported Hardware Directory
                if (unpopulated.isNotEmpty()) {
                    span {
                        val toggleShape = RoundedCornerShape(20.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .glassPanel(toggleShape)
                                .clickable { showAllSupported = !showAllSupported }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "All Supported Hardware",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                                    color = Tone.Ink,
                                )
                                Text(
                                    "${unpopulated.size} additional consoles ready for ROMs",
                                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                                    color = Tone.Muted,
                                )
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
                            SupportedConsoleGlassChip(sys) { onOpenSystem(sys) }
                        }
                    }
                }
            }

            LibraryTab.Shelves -> {
                if (recents.isNotEmpty()) {
                    item(key = "smart_recent") {
                        GlassShelfTile(
                            title = "Recently Played",
                            count = recents.size,
                            games = recents,
                            tint = Ascend.accent,
                            onClick = { onOpenGame(recents.first()) },
                        )
                    }
                }
                if (unplayed.isNotEmpty()) {
                    item(key = "smart_backlog") {
                        GlassShelfTile(
                            title = "Backlog",
                            count = unplayed.size,
                            games = unplayed,
                            tint = Color(0xFF7B68FF),
                            onClick = { onOpenGame(unplayed.first()) },
                        )
                    }
                }
                items(collections, key = { "col_${it.first.id}" }) { (col, colGames) ->
                    GlassShelfTile(
                        title = col.name,
                        count = colGames.size,
                        games = colGames,
                        tint = colGames.firstOrNull()?.system?.color ?: Ascend.accent,
                        onClick = { onOpenCollection(col) },
                    )
                }
                item(key = "create_shelf_tile") {
                    val shape = RoundedCornerShape(22.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(182.dp)
                            .glassPanel(shape, elevated = true)
                            .clickable(onClick = onNewCollection)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Ascend.accent, Color(0xFF7B68FF))))
                                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "New Shelf",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                            color = Tone.Ink,
                        )
                        Text(
                            "Curate games",
                            style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                            color = Tone.Muted,
                        )
                    }
                }
            }

            LibraryTab.Favourites -> {
                if (favorites.isEmpty()) {
                    span {
                        GlassEmptyCard(
                            title = "No pinned games yet",
                            body = "Pin your favourite classics from any game page for instant access here.",
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
                        GlassEmptyCard(
                            title = "No captures saved yet",
                            body = "Snap screenshots from the in-game pause menu to build your gallery and custom covers.",
                        )
                    }
                } else {
                    items(shots, key = { it.file.absolutePath }) { shot ->
                        val shape = RoundedCornerShape(20.dp)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .glassPanel(shape)
                                .clickable { onOpenGame(shot.game) },
                        ) {
                            AsyncImage(
                                model = shot.file,
                                contentDescription = shot.game.title,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.None,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.45f to Color.Transparent,
                                            1f to Color(0xFF060912).copy(alpha = 0.88f),
                                        ),
                                    ),
                            )
                            Column(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    shot.game.title,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    shot.game.system?.shortName ?: shot.game.systemId,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.History -> {
                if (history.isEmpty()) {
                    span {
                        GlassEmptyCard(
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
                                LocalDate.now() -> "Today"
                                LocalDate.now().minusDays(1) -> "Yesterday"
                                else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            }
                            Text(
                                label,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = Tone.Muted,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            )
                        }
                        entries.forEach { entry ->
                            span {
                                val game = entry.game
                                val rowShape = RoundedCornerShape(20.dp)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .glassPanel(rowShape)
                                        .clickable { onOpenGame(game) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    GameArt(
                                        game,
                                        Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = if (Tone.isDark) 0.16f else 0.40f), RoundedCornerShape(12.dp)),
                                        showTag = false,
                                    )
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            game.title,
                                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
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
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted, modifier = Modifier.size(18.dp))
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
private fun ConsoleWorldBanner(
    system: GameSystem,
    games: List<Game>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .glassPanel(shape, tint = system.color, elevated = true)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to system.color.copy(alpha = if (Tone.isDark) 0.34f else 0.18f),
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.12f, size.height * 0.5f),
                        radius = size.width * 0.52f,
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.12f, size.height * 0.5f),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(system.color),
                )
                Text(
                    system.shortName,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                    color = if (Tone.isDark) Color.White.copy(alpha = 0.85f) else Tone.Ink,
                )
                Text(
                    "· ${system.manufacturer}",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                    color = Tone.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                system.name,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    lineHeight = 19.sp,
                    letterSpacing = (-0.02).em,
                ),
                color = Tone.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(system.color.copy(alpha = if (Tone.isDark) 0.24f else 0.14f))
                    .border(0.75.dp, system.color.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    if (games.size == 1) "1 game ready" else "${games.size} games ready",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = Tone.Ink,
                )
            }
        }

        // Overlapping 3-poster cover fan
        val preview = games.take(3)
        if (preview.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-18).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                preview.forEachIndexed { idx, game ->
                    val sz = if (idx == 0) 64.dp else 56.dp
                    GameArt(
                        game,
                        Modifier
                            .size(sz)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, if (Tone.isDark) Color(0xFF0E1424) else Color.White, RoundedCornerShape(14.dp)),
                        showTag = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportedConsoleGlassChip(system: GameSystem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .glassPanel(shape, tint = system.color)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(system.color),
        )
        Column(Modifier.weight(1f)) {
            Text(
                system.name,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
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
}

@Composable
private fun GlassShelfTile(
    title: String,
    count: Int,
    games: List<Game>,
    tint: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(182.dp)
            .glassPanel(shape, tint = tint, elevated = true)
            .clickable(onClick = onClick),
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
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.30f to Color.Transparent,
                        0.72f to Color(0xFF070A14).copy(alpha = 0.78f),
                        1f to Color(0xFF050811).copy(alpha = 0.94f),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0F1E).copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                "$count",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = Color.White,
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (count == 1) "1 game" else "$count games",
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun GlassEmptyCard(title: String, body: String) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glassPanel(shape, elevated = true)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp), color = Tone.Ink)
        Text(body, style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 20.sp), color = Tone.Muted)
    }
}

private fun LazyGridScope.span(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
