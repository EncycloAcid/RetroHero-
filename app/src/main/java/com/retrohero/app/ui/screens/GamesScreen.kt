package com.retrohero.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.EmptyState
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.GameCard
import com.retrohero.app.ui.components.rememberPressFeedback
import com.retrohero.app.ui.ios.CapsuleButton
import com.retrohero.app.ui.ios.CapsuleStyle
import com.retrohero.app.ui.ios.FilterCapsule
import com.retrohero.app.ui.ios.LargeTitle
import com.retrohero.app.ui.ios.SearchField
import com.retrohero.app.ui.ios.SectionHeader
import com.retrohero.app.ui.ios.Squircles

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
    contentPadding: PaddingValues = PaddingValues(),
    title: String = "Library",
    showSystemFilter: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var systemFilter by rememberSaveable { mutableStateOf<String?>(null) }

    if (!hasFolders && games.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            EmptyState(
                icon = Icons.Rounded.SportsEsports,
                title = "Let's find your games",
                body = "Choose the folder where you keep your game files, or download one from a link. " +
                    "RetroHero recognises 20+ systems and fetches the right emulator for you.",
            ) {
                CapsuleButton("Choose Folder", onAddFolder, icon = Icons.Rounded.CreateNewFolder, large = true)
                Spacer(Modifier.height(12.dp))
                CapsuleButton("Download from a Link", onDownload, style = CapsuleStyle.Gray, icon = Icons.Rounded.Download, large = true)
            }
        }
        return
    }
    if (games.isEmpty() && scanning) {
        SkeletonGrid(contentPadding)
        return
    }
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No games yet",
                body = "Supported files include .nes .sfc .gba .md .cue .chd .zip and more. " +
                    "Tip: name folders after the system, like \"psx\" or \"genesis\".",
            )
        }
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
    val systemCount = systemsPresent.size

    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            LargeTitle(
                title,
                subtitle = "${games.size} ${if (games.size == 1) "game" else "games"}" +
                    if (showSystemFilter) " · $systemCount ${if (systemCount == 1) "system" else "systems"}" else "",
                horizontalPadding = 4.dp,
            )
        }
        fullWidth { SearchField(query, { query = it }, "Search ${games.size} games") }

        if (showSystemFilter && systemsPresent.size > 1) {
            fullWidth {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterCapsule("All", systemFilter == null) { systemFilter = null } }
                    items(systemsPresent, key = { it.id }) { system ->
                        FilterCapsule(system.shortName, systemFilter == system.id) {
                            systemFilter = if (systemFilter == system.id) null else system.id
                        }
                    }
                }
            }
        }

        if (browsing && recents.isNotEmpty()) {
            fullWidth { FeaturedCard(recents.first(), onPlay, onGameOptions, Modifier.padding(top = 6.dp)) }
            if (recents.size > 1) {
                fullWidth { SectionHeader("Recently Played", horizontalPadding = 4.dp) }
                fullWidth {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recents.drop(1), key = { "recent_" + it.id }) { game ->
                            GameCard(game, { onPlay(game) }, { onGameOptions(game) }, Modifier.width(112.dp))
                        }
                    }
                }
            }
            fullWidth { SectionHeader("All Games", horizontalPadding = 4.dp) }
        }

        if (filtered.isEmpty()) {
            fullWidth {
                Text(
                    "No results for \"$query\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 40.dp),
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

/** App Store "Today"-style hero card for the game you played last. */
@Composable
private fun FeaturedCard(game: Game, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 0.dp, pressedCorner = 0.dp, pressedScale = 0.97f)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            }
            .shadow(20.dp, Squircles.Card, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(Squircles.Card)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = { onPlay(game) }, onLongClick = { onOptions(game) }),
    ) {
        GameArt(game, Modifier.fillMaxSize(), showTag = false)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    0.32f to Color.Transparent,
                    0.6f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f),
                ),
            ),
        )
        Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
            Text(
                "CONTINUE PLAYING",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.75f),
            )
            Text(
                game.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameArt(
                game,
                Modifier
                    .size(50.dp)
                    .clip(Squircles.Small)
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), Squircles.Small),
                showTag = false,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.system?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            CapsuleButton("Play", { onPlay(game) }, style = CapsuleStyle.White, icon = Icons.Rounded.PlayArrow)
        }
    }
}

/** Shimmering placeholder cards shown while the first scan runs. */
@Composable
private fun SkeletonGrid(contentPadding: PaddingValues) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val tone = MaterialTheme.colorScheme.surfaceContainerHigh
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth { LargeTitle("Library", subtitle = "Looking for games…", horizontalPadding = 4.dp) }
        items(15) {
            Column(Modifier.graphicsLayer { alpha = pulse }) {
                Box(Modifier.fillMaxWidth().aspectRatio(0.75f).clip(Squircles.Medium).background(tone))
                Box(Modifier.padding(top = 10.dp).fillMaxWidth(0.8f).height(13.dp).clip(CircleShape).background(tone))
                Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.4f).height(10.dp).clip(CircleShape).background(tone))
            }
        }
    }
}
