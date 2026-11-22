package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.badgeShape

/** Box art from libretro-thumbnails, falling back to a generated cover in the system's style. */
@Composable
fun GameArt(game: Game, modifier: Modifier = Modifier) {
    var failed by remember(game.id) { mutableStateOf(false) }
    Box(modifier) {
        if (failed) {
            GeneratedCover(game)
        } else {
            AsyncImage(
                model = game.boxArtUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
    }
}

@Composable
private fun GeneratedCover(game: Game) {
    val system = game.system
    val color = system?.color ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.35f))),
        ),
    ) {
        if (system != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 36.dp, y = 36.dp)
                    .size(150.dp)
                    .rotate(18f)
                    .background(Color.White.copy(alpha = 0.18f), system.badgeShape()),
            )
        }
        Text(
            system?.shortName.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black.copy(alpha = 0.7f),
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
        )
    }
}
