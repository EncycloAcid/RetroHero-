package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.emberBackground
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.GameCollection
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.SwipeRow
import com.retrohero.app.ui.components.frostedGlass
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.badgeShape
import com.retrohero.app.ui.theme.tag
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val CollapseEasing = CubicBezierEasing(0.59f, 0.53f, 0f, 1f)

/**
 * A system shown like an album: a big cover collage that fades into the system's colour,
 * a "Play random" pill, then the games as rows. A frosted bar takes over once the header
 * scrolls away.
 */
@Composable
fun SystemDetailScreen(
    system: GameSystem,
    games: List<Game>,
    onBack: () -> Unit,
    onOpenGame: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onPlayRandom: () -> Unit,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (Game) -> Unit = {},
    onOptions: (Game) -> Unit = {},
) = AlbumScreen(
    title = system.name,
    tag = system.tag,
    subtitle = "${system.manufacturer} · ${if (games.size == 1) "1 game" else "${games.size} games"}",
    color = system.color,
    emptyShape = { system.badgeShape() },
    games = games,
    onBack = onBack,
    onOpenGame = onOpenGame,
    onPlay = onPlay,
    onPlayRandom = onPlayRandom,
    favorites = favorites,
    onSwipeLeft = onToggleFavorite,
    onOptions = onOptions,
)

/** Your own collection, shown the same way as a system. */
@Composable
fun CollectionScreen(
    collection: GameCollection,
    games: List<Game>,
    onBack: () -> Unit,
    onOpenGame: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onPlayRandom: () -> Unit,
    onRemove: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    favorites: Set<String> = emptySet(),
) = AlbumScreen(
    title = collection.name,
    tag = "LIST",
    subtitle = "Your collection · ${if (games.size == 1) "1 game" else "${games.size} games"}",
    color = games.firstOrNull()?.system?.color ?: Color(0xFF6E6A86),
    emptyShape = { CircleShape },
    games = games,
    onBack = onBack,
    onOpenGame = onOpenGame,
    onPlay = onPlay,
    onPlayRandom = onPlayRandom,
    favorites = favorites,
    onSwipeLeft = onRemove,
    removeLabel = "Remove",
    onOptions = onOptions,
    emptyText = "Nothing here yet. Long-press any game and choose “Add to…”.",
    barActions = { progress ->
        var menu by remember { mutableStateOf(false) }
        Box {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)).clickable { menu = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MoreVert, "Collection options", tint = lerp(Color.White, com.retrohero.app.ui.theme.Tone.Ink, progress)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Delete collection") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; onDelete() })
            }
        }
    },
)

@Composable
private fun AlbumScreen(
    title: String,
    tag: String,
    subtitle: String,
    color: Color,
    emptyShape: @Composable () -> Shape,
    games: List<Game>,
    onBack: () -> Unit,
    onOpenGame: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onPlayRandom: () -> Unit,
    favorites: Set<String>,
    onSwipeLeft: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    removeLabel: String? = null,
    emptyText: String? = null,
    barActions: @Composable (Float) -> Unit = {},
) {
    val density = LocalDensity.current
    val headerHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
    val headerPx = with(density) { headerHeight.toPx() }
    val tint = lerp(color, Color.Black, 0.45f)
    val list = rememberLazyListState()
    val glass = rememberHazeState()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Latch the collapse once the header has mostly scrolled away, then ease all the way.
    val collapsed by remember {
        derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > headerPx * 0.72f }
    }
    val progress by animateFloatAsState(if (collapsed) 1f else 0f, tween(700, easing = CollapseEasing), label = "collapse")

    Box(Modifier.fillMaxSize().emberBackground()) {
        LazyColumn(
            state = list,
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(glass)
                .drawBehind {
                    // The system colour continues below the header and fades to black.
                    val top = if (list.firstVisibleItemIndex == 0) headerPx - list.firstVisibleItemScrollOffset else -1f
                    if (top > -headerPx) {
                        drawRect(
                            Brush.verticalGradient(
                                0f to tint,
                                0.4f to tint.copy(alpha = 0.35f),
                                1f to Color.Transparent,
                                startY = top,
                                endY = top + 420.dp.toPx(),
                            ),
                            topLeft = Offset(0f, top),
                        )
                    }
                },
        ) {
            item(key = "header") {
                Header(title, tag, subtitle, color, emptyShape(), games, tint, headerHeight, onPlayRandom)
            }
            if (games.isEmpty() && emptyText != null) item(key = "empty") {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = com.retrohero.app.ui.theme.Tone.Muted,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                )
            }
            items(games, key = { it.id }) { game ->
                SwipeRow(
                    isFavorite = game.id in favorites,
                    onPlay = { onPlay(game) },
                    onToggleFavorite = { onSwipeLeft(game) },
                    removeLabel = removeLabel,
                ) {
                    GameRow(game, game.id in favorites, onOpen = { onOpenGame(game) }, onPlay = { onPlay(game) }, onLongPress = { onOptions(game) })
                }
            }
        }

        // The bar: clear over the header, frosted with a title once the header has gone.
        Row(
            Modifier
                .fillMaxWidth()
                .height(statusTop + 60.dp)
                .frostedGlass(glass, RectangleShape) { progress }
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f * (1f - 0.5f * progress)))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = lerp(Color.White, com.retrohero.app.ui.theme.Tone.Ink, progress)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).graphicsLayer { alpha = progress }) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${games.size} games", style = MaterialTheme.typography.bodySmall, color = com.retrohero.app.ui.theme.Tone.Muted)
            }
            barActions(progress)
        }
    }
}

@Composable
private fun Header(
    title: String,
    tag: String,
    subtitle: String,
    color: Color,
    emptyShape: Shape,
    games: List<Game>,
    tint: Color,
    height: androidx.compose.ui.unit.Dp,
    onPlayRandom: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        val covers = games.take(4)
        when {
            covers.size >= 4 -> Column(Modifier.fillMaxSize()) {
                for (r in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0..1) GameArt(covers[r * 2 + c], Modifier.weight(1f).fillMaxHeight(), showTag = false)
                }
            }
            covers.isNotEmpty() -> GameArt(covers.first(), Modifier.fillMaxSize(), showTag = false)
            else -> Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lerp(color, Color.White, 0.15f), tint))),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(220.dp).offset(x = 60.dp, y = (-30).dp).rotate(14f).background(Color.White.copy(alpha = 0.16f), emptyShape))
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.2f to Color.Transparent,
                    0.4f to Color.Transparent,
                    0.85f to Color.Black.copy(alpha = 0.8f),
                    1f to com.retrohero.app.ui.components.Page.Bg,
                ),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 22.dp)) {
            Text(
                tag,
                style = TextStyle(fontFamily = PixelFont, fontSize = 10.sp),
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .height(40.dp)
                        .press3D(maxTilt = 4f)
                        .clip(com.retrohero.app.ui.components.Ascend.CardShape)
                        .background(com.retrohero.app.ui.theme.Salmon)
                        .clickable(enabled = games.isNotEmpty(), onClick = onPlayRandom)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Casino, null, tint = com.retrohero.app.ui.components.Ascend.onAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play random", style = MaterialTheme.typography.labelLargeEmphasized, color = com.retrohero.app.ui.components.Ascend.onAccent)
                }
            }
        }
    }
}

@Composable
private fun GameRow(game: Game, isFavorite: Boolean, onOpen: () -> Unit, onPlay: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt(game, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), showTag = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isFavorite) Icon(Icons.Rounded.Favorite, "Pinned", tint = Salmon, modifier = Modifier.padding(start = 6.dp).size(14.dp))
            }
            Text(
                game.fileName.substringAfterLast('.').uppercase() + if (game.isLocal) " · Downloaded" else "",
                style = MaterialTheme.typography.bodySmall,
                color = com.retrohero.app.ui.theme.Tone.Muted,
            )
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = com.retrohero.app.ui.theme.Tone.Ink) }
    }
}
