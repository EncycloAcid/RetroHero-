package com.retrohero.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Casino
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.R
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.GameCard
import com.retrohero.app.ui.components.MorphShape
import com.retrohero.app.ui.components.rememberMorph
import com.retrohero.app.ui.components.rememberPressFeedback
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.polygon
import com.retrohero.app.ui.theme.tag

/**
 * Immersive home: a hero with a collage of covers and a big play shape, a "roll" button for a
 * random game, then rows of recently played games and systems, then the whole library.
 */
@Composable
fun HomeScreen(
    games: List<Game>,
    recents: List<Game>,
    onPlay: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onRandom: () -> Unit,
    onOpenSystem: (GameSystem) -> Unit,
    onSeeAllGames: () -> Unit,
    topActions: @Composable () -> Unit,
    bottomPadding: Dp,
) {
    val featured = recents.firstOrNull() ?: games.firstOrNull()
    val collage = remember(recents, games) { (recents + games).distinctBy { it.id }.take(3) }
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) { Systems.all.filter { (counts[it.id] ?: 0) > 0 } }
    val tint = featured?.system?.color ?: MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    LazyVerticalGrid(
        columns = GridCells.Adaptive(118.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = bottomPadding + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().background(background),
    ) {
        full {
            Hero(
                tint = tint,
                collage = collage,
                featured = featured,
                onPlay = onPlay,
                onOptions = onOptions,
                onRandom = onRandom,
                topActions = topActions,
            )
        }

        if (recents.isNotEmpty()) {
            full { RowHeader("Recently Played") }
            full {
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recents, key = { "r_" + it.id }) { game -> RowCard(game, onPlay, onOptions) }
                }
            }
        }

        if (systems.size > 1) {
            full { RowHeader("Your Systems") }
            full {
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(systems, key = { "s_" + it.id }) { system -> SystemRowCard(system, counts[system.id] ?: 0, onOpenSystem) }
                }
            }
        }

        full { RowHeader("All Games", onClick = onSeeAllGames) }
        items(games, key = { it.id }) { game ->
            GameCard(
                game,
                onClick = { onPlay(game) },
                onLongClick = { onOptions(game) },
                modifier = Modifier.padding(horizontal = 0.dp).animateItem(),
            )
        }
    }
}

/** Full-width row that bleeds through the grid's 24dp side padding to the screen edges. */
private fun LazyGridScope.full(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.bleed(24.dp)) { content() } }

private fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = (horizontal * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

@Composable
private fun Hero(
    tint: Color,
    collage: List<Game>,
    featured: Game?,
    onPlay: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onRandom: () -> Unit,
    topActions: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val glow = lerp(tint, scheme.background, 0.45f)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(540.dp)
            .background(Brush.verticalGradient(0f to glow, 0.62f to lerp(glow, scheme.background, 0.6f), 1f to scheme.background)),
    ) {
        val w = maxWidth
        // Collage of covers bleeding off the right edge.
        val cardWidth = w * 0.56f
        collage.getOrNull(0)?.let { CollageCard(it, onPlay, onOptions, Modifier.offset(x = w * 0.62f, y = (-44).dp).size(cardWidth, 140.dp), -2f) }
        collage.getOrNull(1)?.let { CollageCard(it, onPlay, onOptions, Modifier.offset(x = w * 0.54f, y = 112.dp).size(cardWidth, 222.dp), 1.5f) }
        collage.getOrNull(2)?.let { CollageCard(it, onPlay, onOptions, Modifier.offset(x = w * 0.64f, y = 356.dp).size(cardWidth, 170.dp), -1f) }

        // Fade the collage into the page at the bottom.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, scheme.background))),
        )

        Row(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_launcher_monochrome),
                "RetroHero",
                tint = scheme.onBackground,
                modifier = Modifier.size(56.dp).offset(x = (-12).dp),
            )
            Spacer(Modifier.weight(1f))
            topActions()
        }

        PlayBlob(featured, onPlay, Modifier.offset(x = 36.dp, y = 170.dp))

        Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 36.dp)) {
            Text(
                "Feeling lucky?",
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = scheme.onBackground.copy(alpha = 0.6f),
            )
            Text(
                "Random play",
                style = MaterialTheme.typography.displaySmallEmphasized.copy(fontSize = 34.sp),
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            RollButton(onRandom)
        }
    }
}

@Composable
private fun CollageCard(game: Game, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit, modifier: Modifier, tilt: Float) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 30.dp, pressedCorner = 44.dp, pressedScale = 0.97f)
    Box(
        modifier
            .rotate(tilt)
            .graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            }
            .shadow(16.dp, RoundedCornerShape(press.corner.value))
            .clip(RoundedCornerShape(press.corner.value))
            .combinedClickable(interactionSource = interaction, indication = null, onClick = { onPlay(game) }, onLongClick = { onOptions(game) }),
    ) {
        GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false)
    }
}

/** Big rounded play shape; it morphs toward a circle and grows while pressed. */
@Composable
private fun PlayBlob(featured: Game?, onPlay: (Game) -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val progress by animateFloatAsState(if (pressed) 1f else 0f, spring(0.45f, Spring.StiffnessMediumLow), label = "playMorph")
    val morph = rememberMorph(MaterialShapes.Triangle, MaterialShapes.Cookie9Sided)
    val color = lerp(MaterialTheme.colorScheme.primary, Color(0xFFFF8A8A), 0.5f)
    Box(
        modifier
            .size(118.dp)
            .graphicsLayer {
                rotationZ = 90f + 40f * progress
                scaleX = 1f + 0.08f * progress
                scaleY = 1f + 0.08f * progress
            }
            .background(color, MorphShape(morph, progress))
            .clickable(interactionSource = interaction, indication = null, enabled = featured != null) { featured?.let(onPlay) },
    )
}

@Composable
private fun RollButton(onRoll: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spin by animateFloatAsState(if (pressed) 180f else 0f, spring(0.4f, Spring.StiffnessMedium), label = "dice")
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(scheme.onBackground.copy(alpha = 0.16f))
            .clickable(interactionSource = interaction, indication = null, onClick = onRoll)
            .padding(start = 10.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(scheme.onBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Casino, null, tint = scheme.background, modifier = Modifier.size(24.dp).rotate(spin))
        }
        Spacer(Modifier.width(10.dp))
        Text("Roll!", style = MaterialTheme.typography.titleLargeEmphasized, color = scheme.onBackground)
    }
}

@Composable
private fun RowHeader(text: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .padding(start = 24.dp, top = 10.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.headlineSmallEmphasized)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun RowCard(game: Game, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 36.dp, pressedCorner = 52.dp, pressedScale = 0.95f)
    Column(
        Modifier
            .width(150.dp)
            .graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            }
            .combinedClickable(interactionSource = interaction, indication = null, onClick = { onPlay(game) }, onLongClick = { onOptions(game) }),
    ) {
        GameArt(game, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(press.corner.value)))
        Text(
            game.title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            game.system?.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SystemRowCard(system: GameSystem, count: Int, onOpen: (GameSystem) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, spring(0.5f, Spring.StiffnessMediumLow), label = "sysMorph")
    val morph = rememberMorph(system.polygon, MaterialShapes.Circle)
    Column(
        Modifier
            .width(150.dp)
            .clickable(interactionSource = interaction, indication = null) { onOpen(system) },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.linearGradient(listOf(lerp(system.color, Color.White, 0.15f), lerp(system.color, Color.Black, 0.5f)))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .rotate(14f + 60f * p)
                    .background(Color.White.copy(alpha = 0.22f), MorphShape(morph, p)),
            )
            Text(
                system.tag,
                style = androidx.compose.ui.text.TextStyle(fontFamily = PixelFont, fontSize = 16.sp),
                color = Color.White,
            )
        }
        Text(system.name, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
        Text(
            if (count == 1) "1 game" else "$count games",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
