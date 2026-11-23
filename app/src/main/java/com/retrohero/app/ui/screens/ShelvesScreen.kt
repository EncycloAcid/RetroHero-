package com.retrohero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.PageHeader
import com.retrohero.app.ui.components.emberBackground
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon

/** One row on the Shelves page. */
private class ShelfRow(val key: String, val title: String, val games: List<Game>, val count: Int = games.size, val onOpen: (() -> Unit)? = null)

private const val RowLimit = 20

/**
 * Your games grouped into shelves, one horizontal row of covers each: what you're playing,
 * favourites, your own shelves (collections), most played and the backlog of games not started.
 */
@Composable
fun ShelvesScreen(
    games: List<Game>,
    recents: List<Game>,
    favorites: List<Game>,
    collections: List<Pair<GameCollection, List<Game>>>,
    stats: Map<String, PlayStat>,
    onOpenGame: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onOpenCollection: (GameCollection) -> Unit,
    onNewShelf: () -> Unit,
    onCoverFlow: () -> Unit,
) {
    val rows = remember(games, recents, favorites, collections, stats) {
        val played = games.filter { (stats[it.id]?.seconds ?: 0) > 0 }
        val backlog = games.filter { (stats[it.id]?.sessions ?: 0) == 0 && (stats[it.id]?.seconds ?: 0) == 0L }
        buildList {
            add(ShelfRow("playing", "Currently Playing", recents.take(RowLimit), recents.size))
            add(ShelfRow("favorites", "Favorites", favorites.take(RowLimit), favorites.size))
            collections.forEach { (c, list) -> add(ShelfRow("col:" + c.id, c.name, list.take(RowLimit), list.size) { onOpenCollection(c) }) }
            add(ShelfRow("most", "Most Played", played.sortedByDescending { stats[it.id]?.seconds ?: 0 }.take(RowLimit), played.size))
            add(ShelfRow("backlog", "Backlog", backlog.take(RowLimit), backlog.size))
        }.filter { it.count > 0 || it.key.startsWith("col:") }
    }

    LazyColumn(
        Modifier.fillMaxSize().emberBackground(),
        contentPadding = PaddingValues(bottom = 24.dp + LocalNavSpace.current),
    ) {
        item(key = "header") {
            PageHeader(
                "Shelves",
                "${rows.size} ${if (rows.size == 1) "shelf" else "shelves"} · ${games.size} ${if (games.size == 1) "game" else "games"}",
                Modifier.padding(start = Page.Gutter, end = 14.dp),
            ) {
                HeaderButton(Icons.Rounded.ViewCarousel, "Cover flow", Color.White.copy(alpha = 0.08f), Color.White, onCoverFlow)
                HeaderButton(Icons.Rounded.Add, "New shelf", Salmon, Color(0xFF1A0E08), onNewShelf, Modifier.padding(start = 8.dp))
            }
        }
        if (rows.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Add some games and they'll line up here.",
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = Page.Gutter, vertical = 40.dp),
                )
            }
        }
        rows.forEachIndexed { i, row ->
            item(key = row.key) {
                Shelf(row, onOpenGame, onOptions, divider = i < rows.lastIndex)
            }
        }
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, fill: Color, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(fill).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint) }
}

@Composable
private fun Shelf(row: ShelfRow, onOpenGame: (Game) -> Unit, onOptions: (Game) -> Unit, divider: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (row.onOpen != null) Modifier.clickable(onClick = row.onOpen) else Modifier)
                .padding(horizontal = Page.Gutter),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                row.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.02).em),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${row.count} ${if (row.count == 1) "game" else "games"}",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.55f),
            )
        }
        if (row.games.isEmpty()) {
            Box(
                Modifier
                    .padding(start = Page.Gutter, end = Page.Gutter, top = 12.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Long-press a game and choose Add to put it here", style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = Color.White.copy(alpha = 0.45f))
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Page.Gutter),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(row.games, key = { it.id }) { game -> Cover(game, { onOpenGame(game) }, { onOptions(game) }) }
            }
        }
        if (divider) {
            Box(Modifier.padding(start = Page.Gutter, end = Page.Gutter, top = 20.dp).fillMaxWidth().height(1.dp).background(Page.Divider))
        }
    }
}

@Composable
private fun Cover(game: Game, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .width(84.dp)
            .height(116.dp)
            .shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
    ) {
        GameArt(game, Modifier.fillMaxSize())
    }
}
