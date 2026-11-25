package com.retrohero.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3GameCover
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.M3SplitButton
import com.retrohero.app.ui.components.M3WavyBar
import com.retrohero.app.ui.theme.Tone

@Composable
internal fun M3HomePage(
    games: List<Game>,
    recents: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    scanning: Boolean,
    onPlay: (Game) -> Unit,
    onOpenDetail: (Game) -> Unit,
    onShufflePlay: () -> Unit,
    onToggleTheme: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    val heroGame = recents.firstOrNull() ?: games.firstOrNull()
    val jumpBack = remember(recents, games, heroGame) {
        val base = if (recents.size > 1) recents.drop(1) else games.filter { it.id != heroGame?.id }
        base.take(8)
    }
    val favorites = remember(games, favoriteIds) { games.filter { it.id in favoriteIds } }
    val totalSeconds = remember(stats) { stats.values.sumOf { it.seconds } }
    val clearedCount = remember(games, shelfStatuses) { games.count { shelfStatuses[it.id] == "completed" } }
    val clearedFrac = if (games.isNotEmpty()) clearedCount.toFloat() / games.size else 0f

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = inset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. Pure M3 Expressive Display Top Header
        item(key = "m3_home_header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (scanning) "Scanning library…" else "${games.size} ${if (games.size == 1) "title" else "titles"} ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = "RetroHero",
                        style = MaterialTheme.typography.displayMedium,
                        color = cs.onBackground,
                    )
                }

                M3ShapeBadge(
                    polygon = MaterialShapes.Sunny,
                    containerColor = cs.secondaryContainer,
                    size = 48.dp,
                    modifier = Modifier
                        .semantics { contentDescription = "Toggle theme" }
                        .clickable(onClick = onToggleTheme),
                ) {
                    Icon(
                        imageVector = if (Tone.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = cs.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (heroGame == null) {
            item(key = "m3_home_empty") {
                M3EmptyStateCard(
                    scanning = scanning,
                    onAddFolder = onAddFolder,
                    onOpenHomebrew = onOpenHomebrew,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            // 2. Pure M3 Expressive Sculpted Hero Card (solid surfaceContainerHigh + Split Button + Wavy Bar)
            item(key = "m3_home_hero") {
                M3HeroGameCard(
                    game = heroGame,
                    stat = stats[heroGame.id],
                    isRecent = recents.firstOrNull()?.id == heroGame.id,
                    isFavorite = heroGame.id in favoriteIds,
                    completionProgress = clearedFrac.coerceIn(0.24f, 1f),
                    onPlay = { onPlay(heroGame) },
                    onDetails = { onOpenDetail(heroGame) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 3. Playful Organic Shape Stat Trio (Pixel Weather / M3 Expressive Widgets!)
            item(key = "m3_home_shapes") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    M3SculptedStatTile(
                        polygon = MaterialShapes.Cookie9Sided,
                        containerColor = cs.secondaryContainer,
                        contentColor = cs.onSecondaryContainer,
                        icon = Icons.Rounded.Schedule,
                        value = if (totalSeconds > 0) PlayTime.format(totalSeconds) else "0m",
                        label = "Played",
                        modifier = Modifier.weight(1f),
                    )
                    M3SculptedStatTile(
                        polygon = MaterialShapes.Clover4Leaf,
                        containerColor = cs.tertiaryContainer,
                        contentColor = cs.onTertiaryContainer,
                        icon = Icons.Rounded.EmojiEvents,
                        value = "$clearedCount/${games.size}",
                        label = "Cleared",
                        modifier = Modifier.weight(1f),
                    )
                    M3SculptedActionTile(
                        polygon = MaterialShapes.Sunny,
                        containerColor = cs.primaryContainer,
                        contentColor = cs.onPrimaryContainer,
                        label = "Shuffle",
                        onClick = onShufflePlay,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 4. Expressive Wavy Section Divider
            item(key = "m3_home_wave") {
                M3WavyBar(
                    color = cs.outlineVariant.copy(alpha = 0.65f),
                    amplitude = 2.5.dp,
                    wavelength = 24.dp,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 5. Continue Playing Expressive Carousel
            if (jumpBack.isNotEmpty()) {
                item(key = "m3_home_continue") {
                    M3ShelfHeader(
                        title = "Jump back in",
                        badgeText = "${jumpBack.size}",
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(jumpBack, key = { "m3_jump_${it.id}" }) { game ->
                            val sec = stats[game.id]?.seconds ?: 0
                            M3ExpressiveGameCard(
                                game = game,
                                width = 148.dp,
                                isFavorite = game.id in favoriteIds,
                                playTimeLabel = if (sec > 0) PlayTime.format(sec) else null,
                                onOpen = { onOpenDetail(game) },
                                onPlay = { onPlay(game) },
                            )
                        }
                    }
                }
            }

            // 6. Favorites Expressive Shelf
            if (favorites.isNotEmpty()) {
                item(key = "m3_home_favs") {
                    M3ShelfHeader(
                        title = "Favorites",
                        badgeText = "${favorites.size}",
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(favorites, key = { "m3_fav_${it.id}" }) { game ->
                            val sec = stats[game.id]?.seconds ?: 0
                            M3ExpressiveGameCard(
                                game = game,
                                width = 132.dp,
                                isFavorite = true,
                                playTimeLabel = if (sec > 0) PlayTime.format(sec) else null,
                                onOpen = { onOpenDetail(game) },
                                onPlay = { onPlay(game) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3HeroGameCard(
    game: Game,
    stat: PlayStat?,
    isRecent: Boolean,
    isFavorite: Boolean,
    completionProgress: Float,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val outerShape = RoundedCornerShape(32.dp)
    val artShape = RoundedCornerShape(24.dp)

    Column(
        modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(cs.surfaceContainerHigh)
            .clickable(onClick = onDetails)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Bold rounded artwork frame with tonal status badge
        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(artShape),
        ) {
            M3GameCover(game = game, showTitle = false, modifier = Modifier.fillMaxSize())

            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(cs.tertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.SportsEsports, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(14.dp))
                Text(
                    text = if (isRecent) "CONTINUE PLAYING" else "FEATURED",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onTertiaryContainer,
                )
            }

            if (isFavorite) {
                M3ShapeBadge(
                    polygon = MaterialShapes.Heart,
                    containerColor = cs.tertiaryContainer,
                    size = 34.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(16.dp))
                }
            }

            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(cs.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = (game.system?.shortName ?: game.systemId).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSecondaryContainer,
                )
            }
        }

        // Title + Wavy Progress Accent + Split Action Button
        Column(
            Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildString {
                    append(game.system?.name ?: game.systemId.uppercase())
                    if (stat != null && stat.seconds > 0) {
                        append(" · ")
                        append(PlayTime.format(stat.seconds))
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            M3WavyBar(
                color = cs.surfaceContainerHighest,
                progressColor = cs.primary,
                progress = completionProgress,
                amplitude = 2.8.dp,
                wavelength = 18.dp,
                strokeWidth = 3.2.dp,
            )

            M3SplitButton(
                label = if (isRecent) "Resume" else "Play now",
                icon = Icons.Rounded.PlayArrow,
                onPrimaryClick = onPlay,
                trailingIcon = Icons.Rounded.Info,
                trailingDescription = "Details for ${game.title}",
                onTrailingClick = onDetails,
                height = 50.dp,
            )
        }
    }
}

@Composable
private fun M3SculptedStatTile(
    polygon: RoundedPolygon,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = polygon.toShape()
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(containerColor)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, tint = contentColor.copy(alpha = 0.88f), modifier = Modifier.size(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.5.sp),
                color = contentColor,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.80f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun M3SculptedActionTile(
    polygon: RoundedPolygon,
    containerColor: Color,
    contentColor: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val shape = polygon.toShape()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.91f else 1f, spring(dampingRatio = 0.52f, stiffness = 480f), label = "shuffleScale")

    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(containerColor)
            .semantics { contentDescription = "Surprise me" }
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Rounded.Casino, null, tint = contentColor, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun M3ShelfHeader(
    title: String,
    badgeText: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = cs.onBackground,
        )
        Box(
            Modifier
                .clip(CircleShape)
                .background(cs.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(text = badgeText, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
internal fun M3ExpressiveGameCard(
    game: Game,
    width: Dp?,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    playTimeLabel: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.54f, stiffness = 520f), label = "m3CardScale")
    val cardShape = RoundedCornerShape(26.dp)
    val artShape = RoundedCornerShape(20.dp)

    Column(
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(cardShape)
            .background(cs.surfaceContainerHigh)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onPlay()
                },
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.88f)
                .clip(artShape),
        ) {
            // Graphic sculpted monogram/art (no redundant text duplicating the footer below!)
            M3GameCover(game = game, showTitle = false, modifier = Modifier.fillMaxSize())

            if (playTimeLabel != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = playTimeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = cs.onSurface,
                        maxLines = 1,
                    )
                }
            }

            if (isFavorite) {
                M3ShapeBadge(
                    polygon = MaterialShapes.Heart,
                    containerColor = cs.tertiaryContainer,
                    size = 28.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(13.dp))
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = game.system?.shortName ?: game.systemId.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${game.title}", tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun M3EmptyStateCard(
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val spin by rememberInfiniteTransition(label = "m3EmptySpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "m3EmptyAngle",
    )
    val shape = RoundedCornerShape(32.dp)
    val flowerShape = MaterialShapes.SoftBurst.toShape()

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainerHigh)
            .padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(88.dp)
                    .graphicsLayer { rotationZ = spin }
                    .clip(flowerShape)
                    .background(cs.primaryContainer),
            )
            Icon(Icons.Rounded.SportsEsports, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(38.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (scanning) "Scanning folders…" else "Ready to play",
                style = MaterialTheme.typography.headlineMedium,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Add a ROM folder from storage or grab free open-source homebrew classics.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(cs.primary)
                    .clickable(onClick = onAddFolder),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.CreateNewFolder, null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add ROM Folder", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(cs.secondaryContainer)
                    .clickable(onClick = onOpenHomebrew),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Free Homebrew", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
            }
        }
    }
}
