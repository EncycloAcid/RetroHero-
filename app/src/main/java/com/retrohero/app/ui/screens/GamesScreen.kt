package com.retrohero.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
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
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
        preferredItemWidth = 220.dp,
        itemSpacing = 8.dp,
        modifier = Modifier.fillMaxWidth().height(280.dp),
    ) { index ->
        val game = recents[index]
        Box(
            Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .combinedClickable(onClick = { onPlay(game) }, onLongClick = { onOptions(game) }),
        ) {
            GameArt(game, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(game.system?.name.orEmpty(), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
