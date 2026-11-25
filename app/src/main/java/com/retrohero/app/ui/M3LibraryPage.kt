package com.retrohero.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3ConnectedButtonGroup
import com.retrohero.app.ui.components.M3GameCover
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.m3SegmentedShape
import com.retrohero.app.ui.theme.polygon
import com.retrohero.app.ui.theme.tag

private val M3LibraryFilters = listOf("All", "Playing", "Favorites", "Cleared")

@Composable
internal fun M3LibraryPage(
    games: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    onOpenDetail: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
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
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. M3 Expressive Display Header
        item(key = "m3_lib_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.tertiaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${systems.size} ${if (systems.size == 1) "console" else "consoles"} · ${filtered.size} shown",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onTertiaryContainer,
                        )
                    }
                    Text(
                        text = "Games",
                        style = MaterialTheme.typography.displayMedium,
                        color = cs.onBackground,
                    )
                }
            }
        }

        // 2. Official M3 Pill SearchBar (56dp surfaceContainerHighest + integrated layout switcher)
        item(key = "m3_lib_search", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHighest)
                    .padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search ${games.size} titles…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(cs.surfaceContainer)
                            .clickable { query = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .semantics { contentDescription = if (listMode) "Switch to grid view" else "Switch to list view" }
                        .clickable { listMode = !listMode },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (listMode) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                        contentDescription = null,
                        tint = cs.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // 3. Organic Console Shape Filter Strip
        if (systems.isNotEmpty()) {
            item(key = "m3_lib_consoles", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item(key = "m3_sys_all") {
                        M3ConsoleChip(
                            label = "All",
                            badgeText = "${games.size}",
                            polygon = MaterialShapes.Cookie6Sided,
                            selected = selectedSystem == null,
                            onClick = { selectedSystem = null },
                        )
                    }
                    items(systems, key = { "m3_sys_${it.id}" }) { sys ->
                        M3ConsoleChip(
                            label = sys.shortName,
                            badgeText = sys.tag,
                            polygon = sys.polygon,
                            selected = selectedSystem == sys.id,
                            onClick = { selectedSystem = if (selectedSystem == sys.id) null else sys.id },
                        )
                    }
                }
            }
        }

        // 4. M3 Expressive Connected Button Group
        item(key = "m3_lib_filters", span = { GridItemSpan(maxLineSpan) }) {
            M3ConnectedButtonGroup(
                options = M3LibraryFilters,
                selectedIndex = filterIdx,
                onSelect = { filterIdx = it },
                height = 42.dp,
            )
        }

        // 5. Games Grid or Android 16 Segmented List
        if (filtered.isEmpty()) {
            item(key = "m3_lib_empty", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(vertical = 36.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    M3ShapeBadge(
                        polygon = MaterialShapes.Clover4Leaf,
                        containerColor = cs.secondaryContainer,
                        size = 56.dp,
                    ) {
                        Icon(Icons.Rounded.Apps, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(26.dp))
                    }
                    Text("No matching games", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Text("Try clearing your search or tap + to add more ROMs.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
        } else if (listMode) {
            item(key = "m3_lib_segmented", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    filtered.forEachIndexed { idx, game ->
                        M3SegmentedGameRow(
                            game = game,
                            stat = stats[game.id],
                            index = idx,
                            count = filtered.size,
                            onOpen = { onOpenDetail(game) },
                            onPlay = { onQuickPlay(game) },
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { "m3_grid_${it.id}" }) { game ->
                val sec = stats[game.id]?.seconds ?: 0
                M3ExpressiveGameCard(
                    game = game,
                    width = null,
                    isFavorite = game.id in favoriteIds,
                    playTimeLabel = if (sec > 0) PlayTime.format(sec) else null,
                    onOpen = { onOpenDetail(game) },
                    onPlay = { onQuickPlay(game) },
                )
            }
        }
    }
}

@Composable
private fun M3ConsoleChip(
    label: String,
    badgeText: String,
    polygon: RoundedPolygon,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        M3ShapeBadge(
            polygon = polygon,
            containerColor = if (selected) cs.primary else cs.secondaryContainer,
            size = 32.dp,
        ) {
            Text(
                text = badgeText.take(4),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (badgeText.length >= 4) 7.sp else 8.5.sp,
                ),
                color = if (selected) cs.onPrimary else cs.onSecondaryContainer,
                maxLines = 1,
                softWrap = false,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun M3SegmentedGameRow(
    game: Game,
    stat: PlayStat?,
    index: Int,
    count: Int,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val shape = m3SegmentedShape(index, count)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        M3GameCover(
            game = game,
            showTitle = false,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = cs.onSurface,
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
            Text(text = sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cs.primaryContainer)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${game.title}", tint = cs.onPrimaryContainer, modifier = Modifier.size(22.dp))
        }
    }
}
