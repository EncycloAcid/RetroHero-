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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems

@Composable
fun SystemsScreen(games: List<Game>, onOpen: (GameSystem) -> Unit) {
    val counts = games.groupingBy { it.systemId }.eachCount()
    val systems = Systems.all.sortedWith(compareByDescending<GameSystem> { (counts[it.id] ?: 0) > 0 })
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(systems, key = { it.id }) { system ->
            val count = counts[system.id] ?: 0
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().alpha(if (count > 0) 1f else 0.5f).clickable { onOpen(system) },
            ) {
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(72.dp).background(
                            Brush.linearGradient(listOf(system.color, system.color.copy(alpha = 0.35f))),
                        ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            system.shortName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                    Column(Modifier.padding(12.dp)) {
                        Text(system.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            "${system.manufacturer} · ${if (count == 1) "1 game" else "$count games"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
