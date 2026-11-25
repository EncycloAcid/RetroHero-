package com.retrohero.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.ExpressiveConnectedBar
import com.retrohero.app.ui.components.ExpressivePosterArt
import com.retrohero.app.ui.components.ExpressiveShapeBadge
import com.retrohero.app.ui.components.LocalDockInset
import com.retrohero.app.ui.components.systemTintColor
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.polygon
import com.retrohero.app.ui.theme.tag

private val LibraryFilters = listOf("All", "Playing", "Favorites", "Cleared")

@Composable
internal fun LibraryGridPage(
    games: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    onOpenDetail: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    val dockInset = LocalDockInset.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSystem by rememberSaveable { mutableStateOf<String?>(null) }
    var filterIdx by rememberSaveable { mutableIntStateOf(0) }
    var listMode by rememberSaveable { mutableStateOf(false) }

    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }

    val filtered = remember(games, query, selectedSystem, filterIdx, favoriteIds, shelfStatuses) {
        games.filter { g ->
            val matchSys = selectedSystem == null || g.systemId == selectedSystem
            val matchFilter = when (filterIdx) {
                1 -> shelfStatuses[g.id] == "playing"
                2 -> g.id in favoriteIds
                3 -> shelfStatuses[g.id] == "completed"
                else -> true
            }
            val matchQuery = query.isBlank() ||
                g.title.contains(query, ignoreCase = true) ||
                (g.system?.name?.contains(query, ignoreCase = true) == true) ||
                (g.system?.shortName?.contains(query, ignoreCase = true) == true)
            matchSys && matchFilter && matchQuery
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = dockInset + 22.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. iOS Large Title Header + Grid/List Toggle
        item(key = "lib_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${systems.size} ${if (systems.size == 1) "CONSOLE" else "CONSOLES"} · ${filtered.size} SHOWN",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = "Library",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-0.035).em),
                        color = Tone.Ink,
                    )
                }

                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.glassBorder, CircleShape)
                        .semantics { contentDescription = if (listMode) "Switch to grid view" else "Switch to list view" }
                        .clickable { listMode = !listMode },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (listMode) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                        contentDescription = null,
                        tint = Tone.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // 2. iOS Frosted Capsule Search Bar
        item(key = "lib_search", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.glassBorder, CircleShape)
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Tone.Muted, modifier = Modifier.size(19.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search ${games.size} games or consoles…",
                            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                            color = Tone.Muted,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Tone.Ink),
                        cursorBrush = SolidColor(Ascend.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Tone.Raised)
                            .clickable { query = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = Tone.Muted, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // 3. M3 Expressive Console Scallop Chip Strip
        if (systems.isNotEmpty()) {
            item(key = "lib_consoles", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item(key = "sys_all") {
                        ConsoleScallopChip(
                            label = "All",
                            tagText = "${games.size}",
                            polygon = MaterialShapes.Cookie6Sided,
                            tint = Ascend.accent,
                            selected = selectedSystem == null,
                            onClick = { selectedSystem = null },
                        )
                    }
                    items(systems, key = { "sys_${it.id}" }) { sys ->
                        ConsoleScallopChip(
                            label = sys.shortName,
                            tagText = sys.tag,
                            polygon = sys.polygon,
                            tint = systemTintColor(sys.id),
                            selected = selectedSystem == sys.id,
                            onClick = { selectedSystem = if (selectedSystem == sys.id) null else sys.id },
                        )
                    }
                }
            }
        }

        // 4. M3 Expressive Connected Filter Bar
        item(key = "lib_filters", span = { GridItemSpan(maxLineSpan) }) {
            ExpressiveConnectedBar(
                options = LibraryFilters,
                selectedIndex = filterIdx,
                onSelect = { filterIdx = it },
                height = 38.dp,
            )
        }

        // 5. Game Grid or iOS Grouped List
        if (filtered.isEmpty()) {
            item(key = "lib_empty", span = { GridItemSpan(maxLineSpan) }) {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.glassBorder, shape)
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExpressiveShapeBadge(
                        polygon = MaterialShapes.Clover4Leaf,
                        containerColor = Tone.tonalSurface(Ascend.accent),
                        size = 52.dp,
                    ) {
                        Icon(Icons.Rounded.Apps, null, tint = Ascend.accent, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        text = "No matching titles",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Tone.Ink,
                    )
                    Text(
                        text = "Try clearing your filter or adding more ROMs from the + button.",
                        style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                        color = Tone.Muted,
                    )
                }
            }
        } else if (listMode) {
            item(key = "lib_list_card", span = { GridItemSpan(maxLineSpan) }) {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.glassBorder, shape),
                ) {
                    filtered.forEachIndexed { idx, game ->
                        IosLibraryListRow(
                            game = game,
                            stat = stats[game.id],
                            showDivider = idx < filtered.lastIndex,
                            onDetails = { onOpenDetail(game) },
                            onPlay = { onQuickPlay(game) },
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { "grid_${it.id}" }) { game ->
                SquircleGameCard(
                    game = game,
                    width = null,
                    isFavorite = game.id in favoriteIds,
                    onClick = { onOpenDetail(game) },
                    onLongClick = { onQuickPlay(game) },
                )
            }
        }
    }
}

@Composable
private fun ConsoleScallopChip(
    label: String,
    tagText: String,
    polygon: RoundedPolygon,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Row(
        Modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) tint.copy(alpha = if (Tone.isDark) 0.24f else 0.16f) else Tone.Surface)
            .border(
                width = 1.dp,
                color = if (selected) tint.copy(alpha = 0.72f) else Tone.Line,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpressiveShapeBadge(
            polygon = polygon,
            containerColor = if (selected) tint else tint.copy(alpha = if (Tone.isDark) 0.22f else 0.14f),
            size = 30.dp,
        ) {
            Text(
                text = tagText.take(4),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = if (tagText.length >= 4) 6.5.sp else 8.sp, letterSpacing = 0.01.em),
                color = if (selected) Color.White else tint,
                maxLines = 1,
                softWrap = false,
            )
        }
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = if (selected) Tone.Ink else Tone.Muted,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun IosLibraryListRow(
    game: Game,
    stat: PlayStat?,
    showDivider: Boolean,
    onDetails: () -> Unit,
    onPlay: () -> Unit,
) {
    val thumbShape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onDetails)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpressivePosterArt(
                game = game,
                showTypography = false,
                modifier = Modifier
                    .size(48.dp)
                    .clip(thumbShape)
                    .border(0.8.dp, Tone.glassBorder, thumbShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = game.title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = Tone.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = buildString {
                    append(game.system?.shortName ?: game.systemId.uppercase())
                    if (stat != null && stat.seconds > 0) {
                        append(" · ")
                        append(PlayTime.format(stat.seconds))
                    }
                }
                Text(
                    text = sub,
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = Tone.Muted,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(Tone.tonalSurface(Ascend.accent))
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Ascend.accent, modifier = Modifier.size(16.dp))
                    Text("Play", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Ascend.accent)
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = 0.75.dp, color = Tone.Line, modifier = Modifier.padding(start = 74.dp, end = 14.dp))
        }
    }
}
