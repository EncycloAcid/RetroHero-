package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
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
    val accent2: Color,
    val ink: Color = Color.White,
)

private val PosterThemes = listOf(
    PosterTheme(Color(0xFF1A2830), Color(0xFF0E181E), Color(0xFF060A0D), Color(0xFFF0B43C), Color(0xFF3FD8A4)), // obsidian gold
    PosterTheme(Color(0xFFF4D026), Color(0xFFC9980B), Color(0xFF211805), Color(0xFFFFF49C), Color(0xFFFF6B24), Color(0xFF120E03)), // cyber yellow
    PosterTheme(Color(0xFF196AA8), Color(0xFF0F3B68), Color(0xFF071324), Color(0xFF60CBFF), Color(0xFF9E7BFF)), // cobalt sky
    PosterTheme(Color(0xFFC92820), Color(0xFF701310), Color(0xFF140606), Color(0xFFFF8C38), Color(0xFFFFD166)), // crimson dusk
    PosterTheme(Color(0xFF7D3428), Color(0xFF3E1612), Color(0xFF100707), Color(0xFFFF6E40), Color(0xFFF4A261)), // ember bronze
    PosterTheme(Color(0xFF4C3A78), Color(0xFF261E46), Color(0xFF0E0D1E), Color(0xFF7AE4DA), Color(0xFFFF7EB6)), // twilight celeste
    PosterTheme(Color(0xFF263848), Color(0xFF131E29), Color(0xFF070C12), Color(0xFF9EC9E8), Color(0xFF56C2FF)), // midnight slate
    PosterTheme(Color(0xFF1E483C), Color(0xFF102822), Color(0xFF060F0D), Color(0xFFFFA040), Color(0xFF4BE3AC)), // emerald cosmos
)

fun posterAccentColor(game: Game): Color =
    PosterThemes[abs(game.id.hashCode()) % PosterThemes.size].glow

/**
 * Renders a game's cover art from Libretro thumbnails or custom covers, with a rich
 * editorial poster fallback so physical cases look crisp and authentic even offline.
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
    val hash = remember(game.id) { abs(game.id.hashCode()) }
    val theme = PosterThemes[hash % PosterThemes.size]
    val motif = hash % 4
    val isLightBg = theme.ink != Color.White

    BoxWithConstraints(
        modifier.background(
            Brush.verticalGradient(
                0f to theme.top,
                0.55f to theme.mid,
                1f to theme.bottom,
            ),
        ),
    ) {
        val compact = maxWidth < 108.dp
        val shortestWord = remember(game.title) {
            game.title.split(" ", "-", ":").maxOfOrNull { it.length } ?: 6
        }
        // Smart responsive font sizing so words NEVER wrap mid-word (no "SYMPHON / Y RING")
        val titleSp = when {
            maxWidth < 76.dp -> if (shortestWord >= 8) 7.5f else 8.5f
            maxWidth < 104.dp -> if (shortestWord >= 8) 8.6f else 9.8f
            maxWidth < 140.dp -> if (shortestWord >= 9) 10.5f else 12f
            else -> if (shortestWord >= 9) 13f else 15f
        }

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft atmospheric key light behind the artwork
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.glow.copy(alpha = 0.44f), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.36f),
                    radius = w * 0.72f,
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.50f, h * 0.36f),
            )

            // Secondary counter-light at lower right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.accent2.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.82f),
                    radius = w * 0.55f,
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.85f, h * 0.82f),
            )

            // Editorial retro poster geometric motif (4 distinct styles per title)
            when (motif) {
                0 -> {
                    // Orbital concentric rings + sun core
                    val center = Offset(w * 0.50f, h * 0.38f)
                    drawCircle(theme.glow.copy(alpha = 0.24f), w * 0.30f, center, style = Stroke(width = 1.6f))
                    drawCircle(theme.glow.copy(alpha = 0.14f), w * 0.43f, center, style = Stroke(width = 1.1f))
                    drawCircle(theme.accent2.copy(alpha = 0.22f), w * 0.08f, Offset(w * 0.72f, h * 0.25f))
                }
                1 -> {
                    // Swiss diagonal racing ribbons
                    val ribbon = Path().apply {
                        moveTo(-w * 0.1f, h * 0.72f)
                        lineTo(w * 1.1f, h * 0.22f)
                        lineTo(w * 1.1f, h * 0.36f)
                        lineTo(-w * 0.1f, h * 0.86f)
                        close()
                    }
                    drawPath(ribbon, color = theme.glow.copy(alpha = 0.15f))
                    drawLine(
                        color = theme.accent2.copy(alpha = 0.28f),
                        start = Offset(0f, h * 0.68f),
                        end = Offset(w, h * 0.18f),
                        strokeWidth = 2f,
                    )
                }
                2 -> {
                    // Retro synthwave horizon grid at bottom third
                    val horizonY = h * 0.62f
                    drawCircle(
                        brush = Brush.verticalGradient(
                            listOf(theme.glow.copy(alpha = 0.32f), Color.Transparent),
                            startY = horizonY - w * 0.26f,
                            endY = horizonY,
                        ),
                        radius = w * 0.24f,
                        center = Offset(w * 0.50f, horizonY - w * 0.04f),
                    )
                    for (i in 0..4) {
                        val y = horizonY + (h - horizonY) * (i / 4f)
                        drawLine(theme.glow.copy(alpha = 0.16f), Offset(0f, y), Offset(w, y), 1f)
                    }
                }
                else -> {
                    // Architectural chevron crest
                    val crest = Path().apply {
                        moveTo(w * 0.50f, h * 0.18f)
                        lineTo(w * 0.78f, h * 0.44f)
                        lineTo(w * 0.50f, h * 0.60f)
                        lineTo(w * 0.22f, h * 0.44f)
                        close()
                    }
                    drawPath(crest, color = theme.glow.copy(alpha = 0.14f))
                    drawPath(crest, color = theme.glow.copy(alpha = 0.32f), style = Stroke(width = 1.5f))
                }
            }

            // Subtle lower scrim so typography always pops cleanly
            if (!isLightBg) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.42f),
                    ),
                )
            }

            // Crisp top-left specular print sheen
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.13f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(w * 0.65f, h * 0.45f),
                ),
            )
            // Hairline inner print frame
            val pad = (w * 0.055f).coerceIn(3f, 8f)
            drawRect(
                color = theme.ink.copy(alpha = 0.10f),
                topLeft = Offset(pad, pad),
                size = Size(w - pad * 2f, h - pad * 2f),
                style = Stroke(width = 1f),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 7.dp else 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = (game.system?.shortName ?: game.systemId).uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 6.5.sp else 7.5.sp,
                    letterSpacing = 0.14.em,
                ),
                color = theme.ink.copy(alpha = 0.66f),
                maxLines = 1,
                softWrap = false,
            )

            Text(
                text = game.title.uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Black,
                    fontSize = titleSp.sp,
                    lineHeight = (titleSp * 1.18f).sp,
                    letterSpacing = 0.02.em,
                    textAlign = TextAlign.Center,
                    shadow = if (!isLightBg) {
                        Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 1.5f), 4f)
                    } else {
                        null
                    },
                ),
                color = theme.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(Modifier.padding(bottom = 1.dp))
        }
    }
}
