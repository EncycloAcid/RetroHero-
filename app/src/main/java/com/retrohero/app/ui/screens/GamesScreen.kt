package com.retrohero.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.components.EmptyState
import com.retrohero.app.ui.components.GameCard

@Composable
fun GamesScreen(
    games: List<Game>,
    recents: List<Game>,
    hasFolders: Boolean,
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onPlay: (Game) -> Unit,
    onGameOptions: (Game) -> Unit,
    title: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }

    if (!hasFolders && title == null) {
        EmptyState(
            icon = Icons.Default.SportsEsports,
            title = "Welcome to RetroHero",
            body = "Pick the folder where you keep your game files. RetroHero finds games for 20+ systems " +
                "and downloads the right emulator automatically.",
        ) {
            Button(onClick = onAddFolder) {
                Icon(Icons.Default.CreateNewFolder, null)
                Text("  Add ROM folder")
            }
        }
        return
    }
    if (games.isEmpty() && !scanning) {
        EmptyState(
            icon = Icons.Default.Search,
            title = "No games found",
            body = "Supported files include .nes .sfc .gba .md .cue .chd .zip and more. " +
                "Tip: put games in folders named after the system (e.g. \"psx\", \"genesis\").",
        )
        return
    }

    val filtered = remember(games, query) {
        if (query.isBlank()) games else games.filter { it.title.contains(query.trim(), ignoreCase = true) }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(118.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search ${games.size} games") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        if (query.isBlank() && recents.isNotEmpty() && title == null) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Continue playing") }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recents, key = { "recent_" + it.id }) { game ->
                        GameCard(game, onClick = { onPlay(game) }, onLongClick = { onGameOptions(game) }, width = 130.dp)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("All games") }
        }
        items(filtered, key = { it.id }) { game ->
            GameCard(game, onClick = { onPlay(game) }, onLongClick = { onGameOptions(game) })
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
    )
}
