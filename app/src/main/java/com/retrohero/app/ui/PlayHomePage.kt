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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.ExpressivePosterArt
import com.retrohero.app.ui.components.ExpressiveShapeBadge
import com.retrohero.app.ui.components.ExpressiveSplitButton
import com.retrohero.app.ui.components.LocalDockInset
import com.retrohero.app.ui.components.gameAccentColor
import com.retrohero.app.ui.components.systemTintColor
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.polygon

@Composable
internal fun PlayHomePage(
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
    val dockInset = LocalDockInset.current
    val heroGame = recents.firstOrNull() ?: games.firstOrNull()
    val jumpBackIn = remember(recents, games, heroGame) {
        val base = if (recents.size > 1) recents.drop(1) else games.filter { it.id != heroGame?.id }
        base.take(8)
    }
    val favorites = remember(games, favoriteIds) {
        games.filter { it.id in favoriteIds }
    }
    val recentlyAdded = remember(games) {
        games.take(10)
    }
    val totalSeconds = remember(stats) { stats.values.sumOf { it.seconds } }
    val consoleCount = remember(games) { games.map { it.systemId }.distinct().size }
    val clearedCount = remember(games, shelfStatuses) {
        games.count { shelfStatuses[it.id] == "completed" }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 14.dp, bottom = dockInset + 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. iOS Large Title + Expressive Quick Controls Header
        item(key = "home_header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (scanning) "SCANNING LIBRARY…" else "RETROHERO · ${games.size} ${if (games.size == 1) "GAME" else "GAMES"}",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = "Play",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-0.035).em),
                        color = Tone.Ink,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (games.isNotEmpty()) {
                        ExpressiveShapeBadge(
                            polygon = MaterialShapes.Clover4Leaf,
                            containerColor = Tone.tonalSurface(Ascend.accent),
                            size = 42.dp,
                            modifier = Modifier
                                .semantics { contentDescription = "Surprise me" }
                                .clickable(onClick = onShufflePlay),
                        ) {
                            Icon(Icons.Rounded.Casino, contentDescription = null, tint = Ascend.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Tone.Surface)
                            .border(1.dp, Tone.glassBorder, CircleShape)
                            .semantics { contentDescription = "Toggle theme" }
                            .clickable(onClick = onToggleTheme),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (Tone.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = Tone.Ink,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }

        if (heroGame == null) {
            item(key = "home_empty") {
                ExpressiveEmptyHero(
                    scanning = scanning,
                    onAddFolder = onAddFolder,
                    onOpenHomebrew = onOpenHomebrew,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            // 2. Apple Arcade × M3 Expressive Featured Hero Spotlight Card
            item(key = "home_hero") {
                FeaturedHeroCard(
                    game = heroGame,
                    stat = stats[heroGame.id],
                    isRecent = recents.firstOrNull()?.id == heroGame.id,
                    onPlay = { onPlay(heroGame) },
                    onDetails = { onOpenDetail(heroGame) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 3. Asymmetric M3 Expressive Tonal Bento Strip
            item(key = "home_bento") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Left Bento: Playtime & Cleared Tonal Card
                    val leftShape = RoundedCornerShape(26.dp)
                    Box(
                        Modifier
                            .weight(1.18f)
                            .height(106.dp)
                            .clip(leftShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Ascend.accent.copy(alpha = if (Tone.isDark) 0.24f else 0.16f),
                                        Tone.Surface,
                                    ),
                                ),
                            )
                            .border(1.dp, Tone.glassBorder, leftShape)
                            .padding(14.dp),
                    ) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "PLAY JOURNAL",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 0.12.em),
                                    color = Ascend.accent,
                                )
                                ExpressiveShapeBadge(
                                    polygon = MaterialShapes.Sunny,
                                    containerColor = Ascend.accent.copy(alpha = 0.22f),
                                    size = 28.dp,
                                ) {
                                    Icon(Icons.Rounded.Schedule, null, tint = Ascend.accent, modifier = Modifier.size(14.dp))
                                }
                            }
                            Column {
                                Text(
                                    text = if (totalSeconds > 0) PlayTime.format(totalSeconds) else "Ready",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.025).em),
                                    color = Tone.Ink,
                                    maxLines = 1,
                                )
                                Text(
                                    text = "$clearedCount cleared · $consoleCount ${if (consoleCount == 1) "console" else "consoles"}",
                                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                                    color = Tone.Muted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // Right Bento: Quick Random Play Card
                    val rightShape = RoundedCornerShape(26.dp)
                    val mint = Color(0xFF25C990)
                    Box(
                        Modifier
                            .weight(0.82f)
                            .height(106.dp)
                            .clip(rightShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        mint.copy(alpha = if (Tone.isDark) 0.22f else 0.16f),
                                        Tone.Surface,
                                    ),
                                ),
                            )
                            .border(1.dp, Tone.glassBorder, rightShape)
                            .clickable(onClick = onShufflePlay)
                            .padding(14.dp),
                    ) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ExpressiveShapeBadge(
                                polygon = MaterialShapes.Cookie6Sided,
                                containerColor = mint.copy(alpha = 0.22f),
                                size = 32.dp,
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = mint, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(
                                    text = "Surprise",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = (-0.02).em),
                                    color = Tone.Ink,
                                )
                                Text(
                                    text = "Pick random",
                                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                                    color = Tone.Muted,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Jump Back In Carousel (Large iOS Squircle Cards)
            if (jumpBackIn.isNotEmpty()) {
                item(key = "home_jump_back") {
                    CarouselSection(
                        title = "Jump Back In",
                        subtitle = "${jumpBackIn.size} ready",
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(jumpBackIn, key = { "jump_${it.id}" }) { game ->
                                SquircleGameCard(
                                    game = game,
                                    width = 138.dp,
                                    isFavorite = game.id in favoriteIds,
                                    onClick = { onOpenDetail(game) },
                                    onLongClick = { onPlay(game) },
                                )
                            }
                        }
                    }
                }
            }

            // 5. Favorites Shelf
            if (favorites.isNotEmpty()) {
                item(key = "home_favorites") {
                    CarouselSection(
                        title = "Favorites",
                        subtitle = "${favorites.size} loved",
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(favorites, key = { "fav_${it.id}" }) { game ->
                                SquircleGameCard(
                                    game = game,
                                    width = 116.dp,
                                    isFavorite = true,
                                    onClick = { onOpenDetail(game) },
                                    onLongClick = { onPlay(game) },
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recently Added Shelf
            if (recentlyAdded.isNotEmpty()) {
                item(key = "home_recent_added") {
                    CarouselSection(
                        title = "Recently Added",
                        subtitle = "Fresh cartridges",
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(recentlyAdded, key = { "added_${it.id}" }) { game ->
                                SquircleGameCard(
                                    game = game,
                                    width = 116.dp,
                                    isFavorite = game.id in favoriteIds,
                                    onClick = { onOpenDetail(game) },
                                    onLongClick = { onPlay(game) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeroCard(
    game: Game,
    stat: PlayStat?,
    isRecent: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(30.dp)
    val glow = remember(game.id) { gameAccentColor(game) }
    val sysColor = remember(game.systemId) { systemTintColor(game.systemId) }
    val sysPoly = game.system?.polygon ?: MaterialShapes.Cookie6Sided

    Box(
        modifier
            .fillMaxWidth()
            .height(292.dp)
            .shadow(
                elevation = 22.dp,
                shape = cardShape,
                ambientColor = glow.copy(alpha = 0.34f),
                spotColor = glow.copy(alpha = 0.45f),
            )
            .clip(cardShape)
            .border(1.dp, Tone.glassBorder, cardShape)
            .clickable(onClick = onDetails),
    ) {
        // Full-bleed expressive artwork backdrop
        ExpressivePosterArt(
            game = game,
            showTypography = false,
            modifier = Modifier.fillMaxSize(),
        )

        // Subtle atmospheric bottom vignette so iOS frosted panel pops
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.14f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )

        // Top-left iOS Liquid Glass Eyebrow Pill with console expressive shape
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ExpressiveShapeBadge(
                polygon = sysPoly,
                containerColor = sysColor,
                size = 20.dp,
            ) {
                Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(11.dp))
            }
            Text(
                text = if (isRecent) "CONTINUE PLAYING" else "FEATURED PICK",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.10.em),
                color = Color.White,
            )
        }

        // Bottom Frosted iOS Liquid Glass Control Panel
        val glassShape = RoundedCornerShape(22.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(glassShape)
                .background(Color(0xFF10121C).copy(alpha = 0.80f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f)),
                    ),
                    shape = glassShape,
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = game.title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = (-0.025).em),
                    color = Color.White,
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
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }

            ExpressiveSplitButton(
                label = if (isRecent) "Resume" else "Play Now",
                icon = Icons.Rounded.PlayArrow,
                onPrimaryClick = onPlay,
                secondaryIcon = Icons.Rounded.Info,
                secondaryDescription = "Details for ${game.title}",
                onSecondaryClick = onDetails,
                height = 44.dp,
            )
        }
    }
}

@Composable
private fun CarouselSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, letterSpacing = (-0.02).em),
                color = Tone.Ink,
            )
            Text(
                text = subtitle,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                color = Tone.Muted,
            )
        }
        content()
    }
}

@Composable
internal fun SquircleGameCard(
    game: Game,
    width: Dp?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = 0.56f, stiffness = 540f),
        label = "squircleScale",
    )
    val shape = RoundedCornerShape(22.dp)
    val glow = remember(game.id) { gameAccentColor(game) }

    Column(
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                },
            ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .shadow(
                    elevation = 10.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (Tone.isDark) 0.7f else 0.16f),
                    spotColor = glow.copy(alpha = if (Tone.isDark) 0.30f else 0.18f),
                )
                .clip(shape)
                .border(1.dp, Tone.glassBorder, shape),
        ) {
            ExpressivePosterArt(game, Modifier.fillMaxSize())

            if (isFavorite) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.46f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFFF5A7A), modifier = Modifier.size(13.dp))
                }
            }
        }

        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = (-0.01).em),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = game.system?.shortName ?: game.systemId.uppercase(),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = Tone.Muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ExpressiveEmptyHero(
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(30.dp)
    val spin by rememberInfiniteTransition(label = "emptySpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "emptyAngle",
    )
    val burstShape = MaterialShapes.SoftBurst.toShape()

    Column(
        modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(Tone.tonalSurface(Ascend.accent), Tone.Surface),
                ),
            )
            .border(1.dp, Tone.glassBorder, cardShape)
            .padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Ascend.accent.copy(alpha = 0.35f), Color.Transparent)),
                    radius = size.width * 0.65f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
            Box(
                Modifier
                    .size(76.dp)
                    .graphicsLayer { rotationZ = spin }
                    .clip(burstShape)
                    .background(Ascend.accent),
            )
            Icon(Icons.Rounded.SportsEsports, null, tint = Ascend.onAccent, modifier = Modifier.size(34.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (scanning) "Scanning your folders…" else "Your pocket arcade awaits",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.025).em),
                color = Tone.Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Point RetroHero to a ROM folder or grab a free open-source homebrew title in one tap.",
                style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 19.sp),
                color = Tone.Muted,
                textAlign = TextAlign.Center,
            )
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Ascend.accent)
                    .clickable(onClick = onAddFolder),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.CreateNewFolder, null, tint = Ascend.onAccent, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add ROM Folder", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp), color = Ascend.onAccent)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(Tone.Raised)
                    .border(1.dp, Tone.glassBorder, CircleShape)
                    .clickable(onClick = onOpenHomebrew),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Ascend.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse Free Homebrew", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Tone.Ink)
            }
        }
    }
}
