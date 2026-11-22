package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.LocalNavSpace
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.graphicsLayer
import com.retrohero.app.ui.components.rememberPressFeedback
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.PixelFont
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.EmptyState
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.GameCard

@Composable
fun GamesScreen(
    games: List<Game>,
    recents: List<Game>,
    hasFolders: Boolean,
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onDownload: () -> Unit,
    onPlay: (Game) -> Unit,
    onGameOptions: (Game) -> Unit,
    showSystemFilter: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var systemFilter by rememberSaveable { mutableStateOf<String?>(null) }

    if (!hasFolders && games.isEmpty()) {
        EmptyState(
            icon = Icons.Default.SportsEsports,
            title = "Let's find your games",
            body = "Choose the folder where you keep your game files. RetroHero recognises 20+ systems " +
                "and fetches the right emulator when you need it.",
        ) {
            Button(
                onClick = onAddFolder,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.height(ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Default.CreateNewFolder, null)
                Spacer(Modifier.width(8.dp))
                Text("Choose folder", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
            OutlinedButton(onClick = onDownload, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Default.Download, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Download from a link")
            }
        }
        return
    }
    if (games.isEmpty() && scanning) {
        SkeletonGrid()
        return
    }
    if (games.isEmpty() && !scanning) {
        EmptyState(
            icon = Icons.Default.SearchOff,
            title = "No games yet",
            body = "Supported files include .nes .sfc .gba .md .cue .chd .zip and more. " +
                "Tip: name folders after the system, like \"psx\" or \"genesis\".",
        )
        return
    }

    val systemsPresent = remember(games) {
        val ids = games.mapTo(HashSet()) { it.systemId }
        Systems.all.filter { it.id in ids }
    }
    val filtered = remember(games, query, systemFilter) {
        games.filter {
            (systemFilter == null || it.systemId == systemFilter) &&
                (query.isBlank() || it.title.contains(query.trim(), ignoreCase = true))
        }
    }
    val browsing = query.isBlank() && systemFilter == null

    LazyVerticalGrid(
        columns = GridCells.Adaptive(124.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp + LocalNavSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth { SearchField(query, games.size) { query = it } }

        if (showSystemFilter && systemsPresent.size > 1) {
            fullWidth {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ToggleButton(checked = systemFilter == null, onCheckedChange = { systemFilter = null }) { Text("All") }
                    }
                    items(systemsPresent, key = { it.id }) { system ->
                        ToggleButton(
                            checked = systemFilter == system.id,
                            onCheckedChange = { systemFilter = if (it) system.id else null },
                        ) { Text(system.shortName) }
                    }
                }
            }
        }

        if (browsing && recents.isNotEmpty()) {
            fullWidth { SectionHeader("Jump back in") }
            fullWidth { RecentCarousel(recents, onPlay, onGameOptions) }
            fullWidth { SectionHeader("Library") }
        }

        if (filtered.isEmpty()) {
            fullWidth {
                Text(
                    "No matches",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(filtered, key = { it.id }) { game ->
            GameCard(game, onClick = { onPlay(game) }, onLongClick = { onGameOptions(game) }, modifier = Modifier.animateItem())
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun SearchField(query: String, total: Int, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search $total games") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "Clear") }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun RecentCarousel(recents: List<Game>, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit) {
    val state = rememberCarouselState { recents.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 240.dp,
        itemSpacing = 10.dp,
        modifier = Modifier.fillMaxWidth().height(300.dp),
    ) { index ->
        val game = recents[index]
        val info = carouselItemDrawInfo
        // Text fades out as an item shrinks toward the edge, so it never shows clipped.
        val reveal = {
            val f = ((info.size - info.minSize) / (info.maxSize - info.minSize).coerceAtLeast(1f)).coerceIn(0f, 1f)
            ((f - 0.75f) / 0.25f).coerceIn(0f, 1f) // only the focused (near full-size) item shows text
        }
        val interaction = remember { MutableInteractionSource() }
        val press = rememberPressFeedback(interaction, restCorner = 28.dp, pressedCorner = 28.dp, pressedScale = 0.96f)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = press.scale.value
                    scaleY = press.scale.value
                }
                .maskClip(MaterialTheme.shapes.extraLarge)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onPlay(game) },
                    onLongClick = { onOptions(game) },
                ),
        ) {
            GameArt(game, Modifier.fillMaxSize(), showTag = false)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f)),
                ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer { alpha = reveal() }
                    .padding(18.dp),
            ) {
                Text(
                    game.system?.shortName.orEmpty().uppercase(),
                    style = TextStyle(fontFamily = PixelFont, fontSize = 9.sp, color = game.system?.color ?: Color.White),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer { alpha = reveal() }
                    .padding(14.dp)
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
            }
        }
    }
}

/** Shimmering placeholder cards shown while the first scan runs. */
@Composable
private fun SkeletonGrid() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val tone = MaterialTheme.colorScheme.surfaceContainerHighest
    LazyVerticalGrid(
        columns = GridCells.Adaptive(124.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pulse },
    ) {
        items(12) {
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(0.74f).background(tone, MaterialTheme.shapes.extraLarge))
                Box(Modifier.padding(top = 10.dp).fillMaxWidth(0.8f).height(14.dp).background(tone, CircleShape))
                Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.4f).height(10.dp).background(tone, CircleShape))
            }
        }
    }
}
