package com.retrohero.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.getValue
import com.retrohero.app.ui.components.MorphShape
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.components.rememberMorph
import com.retrohero.app.ui.theme.polygon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.rememberPressFeedback
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.badgeShape

@Composable
fun SystemsScreen(games: List<Game>, onOpen: (GameSystem) -> Unit) {
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val (withGames, empty) = remember(counts) { Systems.all.partition { (counts[it.id] ?: 0) > 0 } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (withGames.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header("In your library") }
            items(withGames, key = { it.id }) { SystemCard(it, counts[it.id] ?: 0, onOpen, Modifier.animateItem()) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Header(if (withGames.isEmpty()) "Supported systems" else "Also supported") }
        items(empty, key = { it.id }) { SystemCard(it, 0, onOpen, Modifier.animateItem()) }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun SystemCard(system: GameSystem, count: Int, onOpen: (GameSystem) -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 28.dp, pressedCorner = 44.dp)
    // Pressing morphs the system's signature shape into a circle and spins it a little.
    val pressed by interaction.collectIsPressedAsState()
    val morphProgress by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "systemMorph",
    )
    val morph = rememberMorph(system.polygon, MaterialShapes.Circle)
    val active = count > 0
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val tint = system.color.copy(alpha = if (active) 0.30f else 0.10f).compositeOver(surface)
    val deep = system.color.copy(alpha = if (active) 0.08f else 0.03f).compositeOver(surface)
    Box(
        modifier
            .fillMaxWidth()
            .height(164.dp)
            .press3D()
            .clip(RoundedCornerShape(press.corner.value))
            .background(Brush.linearGradient(listOf(tint, deep), start = Offset.Zero, end = Offset.Infinite))
            .clickable(interactionSource = interaction, indication = null) { onOpen(system) },
    ) {
        // Oversized signature shape, cropped by the card.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = (-30).dp)
                .size(130.dp)
                .rotate(16f + 60f * morphProgress)
                .background(system.color.copy(alpha = if (active) 0.85f else 0.3f), MorphShape(morph, morphProgress)),
        )
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                system.shortName.uppercase(),
                style = TextStyle(fontFamily = PixelFont, fontSize = 10.sp),
                color = Color.White.copy(alpha = if (active) 1f else 0.7f),
                modifier = Modifier
                    .background(lerp(system.color, Color.Black, 0.5f).copy(alpha = if (active) 0.9f else 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                system.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (active) "${if (count == 1) "1 game" else "$count games"} · ${system.manufacturer}" else system.manufacturer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
