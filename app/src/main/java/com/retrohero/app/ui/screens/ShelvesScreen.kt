package com.retrohero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.graphics.vector.ImageVector
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.GameCard
import com.retrohero.app.ui.components.SectionTitle
import com.retrohero.app.ui.components.ascendCard
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.PageHeader
import com.retrohero.app.ui.components.emberBackground
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon

/** One row on the Shelves page. */
private class ShelfRow(val key: String, val title: String, val games: List<Game>, val count: Int = games.size, val onOpen: (() -> Unit)? = null)

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
    pageIndex: Int = 2,
    onCoverFlow: () -> Unit,
) {
    val rows = remember(games, recents, favorites, collections, stats) {
        val played = games.filter { (stats[it.id]?.seconds ?: 0) > 0 }
        val backlog = games.filter { (stats[it.id]?.sessions ?: 0) == 0 && (stats[it.id]?.seconds ?: 0) == 0L }
        buildList {
            add(ShelfRow("playing", "Currently Playing", recents))
            add(ShelfRow("favorites", "Favorites", favorites))
            collections.forEach { (c, list) -> add(ShelfRow("col:" + c.id, c.name, list) { onOpenCollection(c) }) }
            add(ShelfRow("most", "Most Played", played.sortedByDescending { stats[it.id]?.seconds ?: 0 }))
            add(ShelfRow("backlog", "Backlog", backlog))
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
                index = pageIndex,
            ) {
                HeaderButton(Icons.Rounded.ViewCarousel, "Cover flow", com.retrohero.app.ui.theme.Tone.Raised, com.retrohero.app.ui.theme.Tone.Ink, onCoverFlow)
                HeaderButton(Icons.Rounded.Add, "New shelf", Salmon, Ascend.onAccent, onNewShelf, Modifier.padding(start = 8.dp))
            }
        }
        if (rows.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Your shelves start here. Add games to your library, or tap + to create a collection.",
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                    color = com.retrohero.app.ui.theme.Tone.Muted,
                    modifier = Modifier.padding(horizontal = Page.Gutter, vertical = 40.dp),
                )
            }
        }
        rows.forEachIndexed { index, row ->
            item(key = row.key) {
                Shelf(row, index + 1, onOpenGame, onOptions) { g ->
                    stats[g.id]?.seconds?.takeIf { it >= 60 }?.let { com.retrohero.app.data.PlayTime.format(it) } ?: g.system?.shortName
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, fill: Color, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(fill).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint) }
}

@Composable
private fun Shelf(row: ShelfRow, index: Int, onOpenGame: (Game) -> Unit, onOptions: (Game) -> Unit, subtitle: (Game) -> String?) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        SectionTitle(row.title, Modifier.padding(horizontal = Page.Gutter), count = row.count, index = index, onClick = row.onOpen)
        if (row.games.isEmpty()) {
            Box(
                Modifier
                    .padding(start = Page.Gutter, end = Page.Gutter, top = 12.dp)
                    .fillMaxWidth()
                    .height(72.dp)
                    .ascendCard(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Long-press a game and choose Add to put it here", style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = Ascend.Muted)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Page.Gutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(row.games, key = { it.id }) { game ->
                    GameCard(game, onClick = { onOpenGame(game) }, width = 96.dp, subtitle = subtitle(game), onLongClick = { onOptions(game) })
                }
            }
        }
    }
}
