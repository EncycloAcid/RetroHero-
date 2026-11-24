package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import kotlin.math.abs

private data class PosterTheme(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val glow: Color,
    val ink: Color = Color.White,
)

private val PosterThemes = listOf(
    PosterTheme(Color(0xFF192428), Color(0xFF0E171B), Color(0xFF070A0D), Color(0xFFE5A93B)), // dark golden-ring fantasy
    PosterTheme(Color(0xFFF6DC24), Color(0xFFD9B211), Color(0xFF282008), Color(0xFFFFF394), Color(0xFF141104)), // cyber yellow
    PosterTheme(Color(0xFF1A6FA8), Color(0xFF123F6E), Color(0xFF0A1729), Color(0xFF56C2FF)), // cobalt adventure sky
    PosterTheme(Color(0xFFD1281E), Color(0xFF7D1410), Color(0xFF180707), Color(0xFFFF8C38)), // western crimson sunset
    PosterTheme(Color(0xFF8C3B2B), Color(0xFF451A14), Color(0xFF140908), Color(0xFFFF6E40)), // underworld ember
    PosterTheme(Color(0xFF4E3D7A), Color(0xFF29214A), Color(0xFF111022), Color(0xFF7BE0D6)), // celestial mountain
    PosterTheme(Color(0xFF293845), Color(0xFF161F29), Color(0xFF090D12), Color(0xFFB5D2E8)), // hollow twilight
    PosterTheme(Color(0xFF23473B), Color(0xFF132922), Color(0xFF07110E), Color(0xFFFF9E3D)), // campfire cosmos
)

fun posterAccentColor(game: Game): Color =
    PosterThemes[abs(game.id.hashCode()) % PosterThemes.size].glow

/**
 * Renders a game's cover art from Libretro thumbnails or custom covers, with a vibrant
 * illustrated box-art poster fallback so shelves look rich and authentic even offline.
 */
@Composable
fun CoverArt(game: Game, modifier: Modifier = Modifier) {
    val sources = rememberArtSources(game)
    var index by remember(game.id, sources) { mutableIntStateOf(0) }
    var loaded by remember(game.id, sources) { mutableStateOf(false) }

    Box(modifier) {
        if (!loaded) {
            VibrantBoxArtFallback(game, Modifier.fillMaxSize())
        }
        val model = sources.getOrNull(index)
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = {
                    if (index < sources.lastIndex) index++
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun VibrantBoxArtFallback(game: Game, modifier: Modifier = Modifier) {
    val theme = remember(game.id) {
        PosterThemes[abs(game.id.hashCode()) % PosterThemes.size]
    }
    Box(
        modifier.background(
            Brush.verticalGradient(
                0f to theme.top,
                0.56f to theme.mid,
                1f to theme.bottom,
            ),
        ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Atmospheric central emblem / sun flare
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.glow.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.40f),
                    radius = w * 0.62f,
                ),
                radius = w * 0.62f,
                center = Offset(w * 0.50f, h * 0.40f),
            )
            // Subtle emblem ring
            drawCircle(
                color = theme.glow.copy(alpha = 0.28f),
                radius = w * 0.26f,
                center = Offset(w * 0.50f, h * 0.38f),
                style = Stroke(width = 2.2f),
            )
            // Subtle glossy top-left light catch
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(w * 0.7f, h * 0.5f),
                ),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = (game.system?.shortName ?: game.systemId).uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.5.sp,
                    letterSpacing = 0.14.em,
                ),
                color = theme.ink.copy(alpha = 0.68f),
                maxLines = 1,
            )

            Text(
                text = game.title.uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    lineHeight = 12.5.sp,
                    letterSpacing = 0.01.em,
                    textAlign = TextAlign.Center,
                ),
                color = theme.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Box(Modifier.padding(bottom = 1.dp))
        }
    }
}
