package com.retrohero.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SystemBadge
import com.retrohero.app.ui.components.rememberPressFeedback

@Composable
fun SystemsScreen(games: List<Game>, onOpen: (GameSystem) -> Unit) {
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val (withGames, empty) = remember(counts) { Systems.all.partition { (counts[it.id] ?: 0) > 0 } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (withGames.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header("In your library") }
            items(withGames, key = { it.id }) { SystemCard(it, counts[it.id] ?: 0, onOpen) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Header(if (withGames.isEmpty()) "Supported systems" else "Also supported") }
        items(empty, key = { it.id }) { SystemCard(it, 0, onOpen) }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun SystemCard(system: GameSystem, count: Int, onOpen: (GameSystem) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 28.dp, pressedCorner = 44.dp)
    Surface(
        shape = RoundedCornerShape(press.corner.value),
        color = if (count > 0) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            }
            .clickable(interactionSource = interaction, indication = null) { onOpen(system) },
    ) {
        Column(Modifier.padding(16.dp)) {
            SystemBadge(system, size = 64.dp, modifier = Modifier.graphicsLayer { alpha = if (count > 0) 1f else 0.55f })
            Spacer(Modifier.height(16.dp))
            Text(system.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Text(
                if (count > 0) "${if (count == 1) "1 game" else "$count games"} · ${system.manufacturer}" else system.manufacturer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
