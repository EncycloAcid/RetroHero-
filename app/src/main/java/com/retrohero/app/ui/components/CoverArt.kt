package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private val PosterPalettes = listOf(
    listOf(Color(0xFF566B37), Color(0xFF2B381E), Color(0xFF12180D)), // mossy colossus green
    listOf(Color(0xFFC29E59), Color(0xFF7D6230), Color(0xFF2C210F)), // warm wind-waker parchment gold
    listOf(Color(0xFF355C7D), Color(0xFF1F354D), Color(0xFF0E1824)), // twilight ocean slate
    listOf(Color(0xFF7A3E3E), Color(0xFF432022), Color(0xFF190C0D)), // crimson dusk
    listOf(Color(0xFF5C4B7D), Color(0xFF312647), Color(0xFF130F1D)), // midnight violet
    listOf(Color(0xFF3E6B63), Color(0xFF1F3B36), Color(0xFF0C1816)), // deep emerald
)

/**
 * Loads real box art / custom cover / snapshot from [rememberArtSources], falling back smoothly
 * through candidate URLs and rendering a moody editorial poster backdrop until artwork arrives.
 */
@Composable
fun CoverArt(game: Game, modifier: Modifier = Modifier) {
    val sources = rememberArtSources(game)
    var index by remember(game.id, sources) { mutableIntStateOf(0) }
    var loaded by remember(game.id, sources) { mutableStateOf(false) }

    Box(modifier) {
        if (!loaded) {
            EditorialPosterFallback(game, Modifier.fillMaxSize())
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
private fun EditorialPosterFallback(game: Game, modifier: Modifier = Modifier) {
    val palette = remember(game.id) {
        PosterPalettes[abs(game.id.hashCode()) % PosterPalettes.size]
    }
    Box(
        modifier.background(
            Brush.verticalGradient(
                0f to palette[0],
                0.58f to palette[1],
                1f to palette[2],
            ),
        ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.28f),
                    radius = size.width * 0.72f,
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.50f, size.height * 0.28f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.88f),
                    radius = size.width * 0.75f,
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.50f, size.height * 0.88f),
            )
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = (game.system?.shortName ?: game.systemId).uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.16.em,
                ),
                color = Color.White.copy(alpha = 0.62f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = game.title.uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.04.em,
                    textAlign = TextAlign.Center,
                ),
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Moody night-desk retro CRT + handheld scene for the right side of the Continue Playing hero card,
 * softly blending any loaded game art into the warm desk / phosphor blue CRT glow.
 */
@Composable
fun HeroCrtBackdrop(game: Game, modifier: Modifier = Modifier) {
    val sources = rememberArtSources(game)
    var index by remember(game.id, sources) { mutableIntStateOf(0) }
    val bgBase = Color(0xFF0D0F0D)

    Box(modifier.background(bgBase)) {
        // Subtle real game art reflection on the right if available
        val model = sources.getOrNull(index)
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { if (index < sources.lastIndex) index++ },
                alpha = 0.28f,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.58f)
                    .align(Alignment.CenterEnd),
            )
        }

        // Illustrated low-key CRT monitor + handheld silhouette + warm wooden desk reflection
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Warm amber desk reflection at bottom-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8A4B16).copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(w * 0.86f, h * 0.92f),
                    radius = w * 0.42f,
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.86f, h * 0.92f),
            )

            // Cool phosphor-blue CRT ambient bloom on upper-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3E8BFF).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(w * 0.92f, h * 0.42f),
                    radius = w * 0.46f,
                ),
                radius = w * 0.46f,
                center = Offset(w * 0.92f, h * 0.42f),
            )

            // CRT monitor bezel on the right edge
            val crtLeft = w * 0.77f
            val crtTop = h * 0.11f
            val crtW = w * 0.28f
            val crtH = h * 0.62f
            drawRoundRect(
                color = Color(0xFF171A1E),
                topLeft = Offset(crtLeft, crtTop),
                size = Size(crtW, crtH),
                cornerRadius = CornerRadius(18f, 18f),
            )
            drawRoundRect(
                color = Color(0xFF2B313A),
                topLeft = Offset(crtLeft, crtTop),
                size = Size(crtW, crtH),
                cornerRadius = CornerRadius(18f, 18f),
                style = Stroke(width = 2f),
            )

            // Glowing CRT phosphor glass tube
            val glassLeft = crtLeft + 14f
            val glassTop = crtTop + 14f
            val glassW = crtW - 22f
            val glassH = crtH - 28f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF2E63B8),
                    0.55f to Color(0xFF7CB8FF),
                    0.72f to Color(0xFF1E355E),
                    1f to Color(0xFF3B6EA8),
                    startY = glassTop,
                    endY = glassTop + glassH,
                ),
                topLeft = Offset(glassLeft, glassTop),
                size = Size(glassW, glassH),
                cornerRadius = CornerRadius(14f, 14f),
            )

            // Subtle CRT scanlines
            var sy = glassTop + 4f
            while (sy < glassTop + glassH) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.22f),
                    start = Offset(glassLeft, sy),
                    end = Offset(glassLeft + glassW, sy),
                    strokeWidth = 1.5f,
                )
                sy += 5f
            }

            // Leaning retro pocket handheld silhouette in front of the CRT
            val gbLeft = w * 0.62f
            val gbTop = h * 0.40f
            val gbW = w * 0.16f
            val gbH = h * 0.42f
            drawRoundRect(
                color = Color(0xFF1B1E24),
                topLeft = Offset(gbLeft, gbTop),
                size = Size(gbW, gbH),
                cornerRadius = CornerRadius(14f, 14f),
            )
            drawRoundRect(
                color = Color(0xFF3B4554).copy(alpha = 0.65f),
                topLeft = Offset(gbLeft, gbTop),
                size = Size(gbW, gbH),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 1.5f),
            )
            // Handheld screen inset
            drawRoundRect(
                color = Color(0xFF0E131A),
                topLeft = Offset(gbLeft + gbW * 0.14f, gbTop + gbH * 0.10f),
                size = Size(gbW * 0.72f, gbH * 0.38f),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }

        // Smooth left-to-right obsidian fade so typography on the left is 100% legible
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to bgBase,
                        0.46f to bgBase.copy(alpha = 0.96f),
                        0.68f to bgBase.copy(alpha = 0.55f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to bgBase.copy(alpha = 0.35f),
                        0.5f to Color.Transparent,
                        1f to bgBase.copy(alpha = 0.65f),
                    ),
                ),
        )
    }
}
