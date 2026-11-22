package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.badgeShape

/**
 * Box art from libretro-thumbnails, falling back to a generated cover in the system's colours.
 * [showTitle] prints the game title on generated covers (off where the title is shown nearby).
 */
@Composable
fun GameArt(game: Game, modifier: Modifier = Modifier, showTitle: Boolean = false, showTag: Boolean = true) {
    var failed by remember(game.id) { mutableStateOf(false) }
    Box(modifier) {
        if (failed) {
            GeneratedCover(game, showTitle, showTag)
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
private fun GeneratedCover(game: Game, showTitle: Boolean, showTag: Boolean) {
    val system = game.system
    val base = system?.color ?: MaterialTheme.colorScheme.primary
    val light = lerp(base, Color.White, 0.18f)
    val deep = lerp(base, Color.Black, 0.55f)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(light, base, deep), start = Offset.Zero, end = Offset.Infinite))
            .drawWithContent {
                drawContent()
                // CRT scanlines.
                val step = 3.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.Black.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            },
    ) {
        val unit = maxWidth
        if (system != null) {
            val shape = system.badgeShape()
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = unit * 0.35f, y = -unit * 0.18f)
                    .size(unit * 1.05f)
                    .rotate(14f)
                    .background(Color.White.copy(alpha = 0.16f), shape),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = -unit * 0.25f, y = unit * 0.25f)
                    .size(unit * 0.7f)
                    .rotate(-20f)
                    .border(2.dp, Color.White.copy(alpha = 0.18f), shape),
            )
        }
        if (showTitle) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))),
            )
            Text(
                game.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 8f),
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            )
        }
        if (showTag) Text(
            system?.shortName.orEmpty().uppercase(),
            style = TextStyle(fontFamily = PixelFont, fontSize = (unit.value / 13f).coerceIn(7f, 14f).sp, color = Color.White),
            maxLines = 1,
            modifier = Modifier
                .align(if (showTitle) Alignment.TopStart else Alignment.BottomStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.28f), MaterialTheme.shapes.small)
                .padding(horizontal = 7.dp, vertical = 5.dp),
        )
    }
}
