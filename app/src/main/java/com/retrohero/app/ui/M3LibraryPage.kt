package com.retrohero.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3ConnectedButtonGroup
import com.retrohero.app.ui.components.M3ExpressiveSpring
import com.retrohero.app.ui.components.M3GameCover
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.m3SegmentedShape
import com.retrohero.app.ui.components.m3StaggeredEntrance
import com.retrohero.app.ui.theme.polygon
import com.retrohero.app.ui.theme.tag

private val M3LibraryFilters = listOf("All", "Playing", "Favorites", "Cleared")

@Composable
internal fun M3LibraryPage(
    games: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    selectedSystem: String?,
    onSelectSystem: (String?) -> Unit,
    onOpenDetail: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    var query by rememberSaveable { mutableStateOf("") }
    var filterIdx by rememberSaveable { mutableIntStateOf(0) }
    var listMode by rememberSaveable { mutableStateOf(false) }

    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }
    val activeSystemObj = remember(selectedSystem) { selectedSystem?.let { Systems.get(it) } }

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
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = inset + 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. M3 Expressive Display Header
        item(key = "m3_lib_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().m3StaggeredEntrance(0, slideDp = 12.dp),
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

        // 2. Official M3 Pill SearchBar (54dp surfaceContainerHighest + animated layout switcher)
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
                    AnimatedContent(
                        targetState = listMode,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = 0.56f, stiffness = 460f), 0.55f) + fadeIn(tween(140))) togetherWith
                                (scaleOut(tween(110), 0.55f) + fadeOut(tween(100)))
                        },
                        label = "libViewModeIcon",
                    ) { isList ->
                        Icon(
                            imageVector = if (isList) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                            contentDescription = null,
                            tint = cs.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // 3. Organic Console Shape Filter Strip
        if (systems.isNotEmpty()) {
            item(key = "m3_lib_consoles", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().m3StaggeredEntrance(1, slideDp = 14.dp),
                ) {
                    item(key = "m3_sys_all") {
                        M3ConsoleChip(
                            label = "All",
                            badgeText = "${games.size}",
                            polygon = MaterialShapes.Cookie6Sided,
                            selected = selectedSystem == null,
                            onClick = { onSelectSystem(null) },
                        )
                    }
                    items(systems, key = { "m3_sys_${it.id}" }) { sys ->
                        M3ConsoleChip(
                            label = sys.shortName,
                            badgeText = sys.tag,
                            polygon = sys.polygon,
                            selected = selectedSystem == sys.id,
                            onClick = { onSelectSystem(if (selectedSystem == sys.id) null else sys.id) },
                        )
                    }
                }
            }
        }

        // 4. Dynamic Console Spotlight Banner when filtering a specific hardware platform!
        item(key = "m3_lib_sys_banner", span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                visible = activeSystemObj != null,
                enter = expandVertically(spring(dampingRatio = 0.72f, stiffness = 360f)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
            ) {
                val sys = activeSystemObj ?: return@AnimatedVisibility
                M3ConsoleSpotlightBanner(
                    system = sys,
                    count = filtered.size,
                    onShuffle = { filtered.randomOrNull()?.let(onQuickPlay) },
                    onClear = { onSelectSystem(null) },
                )
            }
        }

        // 5. M3 Expressive Connected Button Group
        item(key = "m3_lib_filters", span = { GridItemSpan(maxLineSpan) }) {
            M3ConnectedButtonGroup(
                options = M3LibraryFilters,
                selectedIndex = filterIdx,
                onSelect = { filterIdx = it },
                height = 42.dp,
                modifier = Modifier.m3StaggeredEntrance(2, slideDp = 14.dp),
            )
        }

        // 6. Feature-First Asymmetric Magazine Grid or Android 16 Segmented List
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
            // Lead editorial full-width spotlight card for #1 filtered title!
            val lead = filtered.first()
            item(key = "m3_lead_${lead.id}", span = { GridItemSpan(maxLineSpan) }) {
                M3LeadEditorialRowCard(
                    game = lead,
                    stat = stats[lead.id],
                    isFavorite = lead.id in favoriteIds,
                    onOpen = { onOpenDetail(lead) },
                    onPlay = { onQuickPlay(lead) },
                )
            }

            // Remaining titles flow in alternating sculptural 2-column magazine grid with smooth animateItem()
            val rest = filtered.drop(1)
            itemsIndexed(rest, key = { _, it -> "m3_grid_${it.id}" }) { idx, game ->
                val sec = stats[game.id]?.seconds ?: 0
                Box(Modifier.animateItem(placementSpec = spring(dampingRatio = 0.72f, stiffness = 360f))) {
                    M3ExpressiveGameCard(
                        game = game,
                        width = null,
                        isFavorite = game.id in favoriteIds,
                        playTimeLabel = if (sec > 0) PlayTime.format(sec) else null,
                        silhouetteVariant = (idx + 1) % 3,
                        onOpen = { onOpenDetail(game) },
                        onPlay = { onQuickPlay(game) },
                    )
                }
            }
        }
    }
}

@Composable
private fun M3ConsoleSpotlightBanner(
    system: GameSystem,
    count: Int,
    onShuffle: () -> Unit,
    onClear: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val polyShape = system.polygon.toShape()

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.primaryContainer)
            .padding(16.dp),
    ) {
        // Oversized sculptural console watermark peeking from right side
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp)
                .size(104.dp)
                .clip(polyShape)
                .background(cs.onPrimaryContainer.copy(alpha = 0.10f)),
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            M3ShapeBadge(polygon = system.polygon, containerColor = cs.primary, size = 48.dp) {
                Text(
                    text = system.tag,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = cs.onPrimary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = system.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = cs.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$count ${if (count == 1) "game" else "games"} in library",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (count > 0) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(cs.primary)
                            .clickable(onClick = onShuffle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = "Shuffle ${system.shortName}", tint = cs.onPrimary, modifier = Modifier.size(19.dp))
                    }
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cs.onPrimaryContainer.copy(alpha = 0.14f))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear console filter", tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun M3LeadEditorialRowCard(
    game: Game,
    stat: PlayStat?,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, M3ExpressiveSpring, label = "leadScale")
    val cardShape = RoundedCornerShape(30.dp)
    val artShape = RoundedCornerShape(topStart = 22.dp, topEnd = 42.dp, bottomStart = 22.dp, bottomEnd = 22.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(cardShape)
            .background(cs.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onOpen()
            }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(114.dp)
                .aspectRatio(0.92f)
                .clip(artShape),
        ) {
            M3GameCover(game = game, showTitle = false, modifier = Modifier.fillMaxSize())
            if (isFavorite) {
                M3ShapeBadge(
                    polygon = MaterialShapes.Heart,
                    containerColor = cs.tertiaryContainer,
                    size = 28.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(13.dp))
                }
            }
        }

        Column(
            Modifier.weight(1f).padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = (game.system?.shortName ?: game.systemId).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = cs.onSecondaryContainer,
                    )
                }
                if (stat != null && stat.seconds > 0) {
                    Text(
                        text = PlayTime.format(stat.seconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(cs.primary)
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp))
                Text("Play", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
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
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, M3ExpressiveSpring, label = "chipScale")
    val badgeRot by animateFloatAsState(
        if (selected) 16f else 0f,
        spring(dampingRatio = 0.48f, stiffness = 420f),
        label = "chipRot",
    )
    val chipBg by animateColorAsState(if (selected) cs.primaryContainer else cs.surfaceContainerHigh, tween(200), label = "chipBg")
    val badgeBg by animateColorAsState(if (selected) cs.primary else cs.secondaryContainer, tween(200), label = "chipBadgeBg")
    val badgeFg by animateColorAsState(if (selected) cs.onPrimary else cs.onSecondaryContainer, tween(180), label = "chipBadgeFg")
    val textFg by animateColorAsState(if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant, tween(180), label = "chipTextFg")

    Row(
        Modifier
            .height(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(chipBg)
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        M3ShapeBadge(
            polygon = polygon,
            containerColor = badgeBg,
            size = 32.dp,
            modifier = Modifier.graphicsLayer { rotationZ = badgeRot },
        ) {
            Text(
                text = badgeText.take(4),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (badgeText.length >= 4) 7.sp else 8.5.sp,
                ),
                color = badgeFg,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer { rotationZ = -badgeRot },
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = textFg,
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
