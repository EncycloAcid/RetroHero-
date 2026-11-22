package com.retrohero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SystemTile
import com.retrohero.app.ui.components.rememberPressFeedback
import com.retrohero.app.ui.ios.Chevron
import com.retrohero.app.ui.ios.LargeTitle
import com.retrohero.app.ui.ios.SectionHeader
import com.retrohero.app.ui.ios.Squircles
import com.retrohero.app.ui.theme.badgeShape

@Composable
fun SystemsScreen(games: List<Game>, onOpen: (GameSystem) -> Unit, contentPadding: PaddingValues = PaddingValues()) {
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val (withGames, empty) = remember(counts) { Systems.all.partition { (counts[it.id] ?: 0) > 0 } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LargeTitle("Systems", subtitle = "${Systems.all.size} consoles supported", horizontalPadding = 4.dp)
        }
        if (withGames.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("In Your Library", horizontalPadding = 4.dp) }
            items(withGames, key = { it.id }) { SystemCard(it, counts[it.id] ?: 0, onOpen, Modifier.animateItem()) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(if (withGames.isEmpty()) "All Systems" else "More Systems", horizontalPadding = 4.dp)
        }
        items(empty, key = { it.id }) { SystemCard(it, 0, onOpen, Modifier.animateItem()) }
    }
}

@Composable
private fun SystemCard(system: GameSystem, count: Int, onOpen: (GameSystem) -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 0.dp, pressedCorner = 0.dp, pressedScale = 0.95f)
    val active = count > 0
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            }
            .clip(Squircles.Large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interactionSource = interaction, indication = null) { onOpen(system) },
    ) {
        // The system's expressive shape peeks in from the corner as a soft watermark.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 44.dp)
                .size(120.dp)
                .rotate(18f)
                .background(system.color.copy(alpha = if (active) 0.22f else 0.08f), system.badgeShape()),
        )
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SystemTile(system, 46.dp, Modifier.graphicsLayer { alpha = if (active) 1f else 0.5f })
                Spacer(Modifier.weight(1f))
                Chevron()
            }
            Spacer(Modifier.weight(1f))
            Text(system.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (active) "${if (count == 1) "1 game" else "$count games"}" else system.manufacturer,
                style = MaterialTheme.typography.bodySmall,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
