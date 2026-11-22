package com.retrohero.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.components.CaseStyle
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.boxSpec
import com.retrohero.app.ui.components.caseStyle
import com.retrohero.app.ui.components.rememberArtColor
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import kotlinx.coroutines.delay

/**
 * Your games standing spine-out on shelves, like a wall of tapes in a video store. Each spine
 * takes its colour from the game's art and sets the title in its own bold lettering. Tap one:
 * it slides out of the shelf, then opens.
 */
@Composable
fun SpineShelf(games: List<Game>, onPick: (Game) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val gutter = 18.dp
        val spineGap = 3.dp
        // Fit as many standard spines across as the width allows.
        val perRow = ((maxWidth - gutter * 2 + spineGap) / (44.dp + spineGap)).toInt().coerceAtLeast(4)
        val rows = remember(games, perRow) { games.chunked(perRow) }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 24.dp + LocalNavSpace.current),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(rows, key = { i, _ -> "shelf_$i" }) { _, row ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(232.dp).padding(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(spineGap),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        row.forEach { g -> Spine(g, onPick) }
                    }
                    Plank()
                }
            }
        }
    }
}

/** How a spine's title is lettered: picked per game so the shelf looks varied, like real tapes. */
private fun spineType(seed: Int): TextStyle {
    val styles = listOf(
        TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 0.04.em),
        TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 18.sp, letterSpacing = (-0.02).em),
        TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.06.em),
        TextStyle(fontFamily = PixelFont, fontSize = 11.sp, letterSpacing = 0.02.em),
        TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.12.em),
        TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 18.sp, letterSpacing = 0.22.em),
    )
    return styles[(seed and 0x7fffffff) % styles.size]
}

@Composable
private fun Spine(game: Game, onPick: (Game) -> Unit) {
    val spec = boxSpec(game.systemId)
    val style = caseStyle(game.systemId)
    val art = rememberArtColor(game)
    // Spine width follows the box's thickness; height follows how tall the box stands.
    val width: Dp = when (style) {
        CaseStyle.Jewel -> 30.dp
        CaseStyle.Keepcase -> 36.dp
        else -> if (spec.depth >= 0.19f) 46.dp else 40.dp
    }
    val height: Dp = when {
        spec.aspect > 1.2f -> 168.dp   // wide boxes (SNES) lie lower
        style == CaseStyle.Jewel -> 176.dp
        spec.aspect > 0.8f -> 196.dp
        else -> 224.dp
    }
    val base = when (style) {
        CaseStyle.Clamshell -> lerp(art, Color.Black, 0.55f)
        CaseStyle.Jewel -> Color(0xFF101012)
        CaseStyle.Keepcase -> Color(0xFFDCDCE2)
        else -> art
    }
    val ink = if (base.luminance() > 0.45f) Color(0xFF141414) else Color.White
    val accent = if (style == CaseStyle.Jewel) art else lerp(base, if (base.luminance() > 0.45f) Color.Black else Color.White, 0.35f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var pulled by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (pulled) 1f else if (pressed) 0.25f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "pull")
    val view = LocalView.current
    LaunchedEffect(pulled) {
        if (pulled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            delay(260)
            onPick(game)
            delay(400)
            pulled = false
        }
    }
    Box(
        Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                translationY = -lift * 28.dp.toPx()
                shadowElevation = 6f + 18f * lift
                shape = RoundedCornerShape(3.dp)
                clip = true
            }
            .background(base)
            .clickable(interactionSource = interaction, indication = null) { pulled = true },
    ) {
        // Round, worn plastic: dark edges, a soft highlight a third of the way across.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    0.18f to Color.Transparent,
                    0.32f to Color.White.copy(alpha = 0.16f),
                    0.5f to Color.Transparent,
                    0.85f to Color.Black.copy(alpha = 0.15f),
                    1f to Color.Black.copy(alpha = 0.5f),
                ),
            )
            val r = java.util.Random(game.id.hashCode().toLong())
            repeat(4) {
                val y = r.nextFloat() * size.height
                drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width, y + r.nextFloat() * 6f), 1f)
            }
            if (style == CaseStyle.Jewel) {
                // Ridged hinge texture.
                var y = 0f
                while (y < size.height) { drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 1f); y += 3.dp.toPx() }
            }
        }
        // System band at the top, like the brand strip on a real spine.
        Box(
            Modifier.fillMaxWidth().height(22.dp).background(if (style == CaseStyle.Keepcase) Color.White else Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                game.system?.shortName?.take(4)?.uppercase().orEmpty(),
                style = TextStyle(fontFamily = PixelFont, fontSize = 6.sp),
                color = if (style == CaseStyle.Keepcase) Color(0xFF2A2A30) else Color.White.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
        // The title, running up the spine.
        Box(Modifier.fillMaxSize().padding(top = 26.dp, bottom = 20.dp), contentAlignment = Alignment.Center) {
            Text(
                game.title.uppercase(),
                style = spineType(game.title.hashCode()).let { t -> if (width < 40.dp) t.copy(fontSize = t.fontSize * 0.78f) else t },
                color = if (style == CaseStyle.Jewel) Color.White else ink,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    // Lay the text out along the spine's length, then turn it to run upward.
                    .requiredWidth(height - 50.dp)
                    .graphicsLayer { rotationZ = -90f },
            )
        }
        // A colour stripe at the foot.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp).offset(y = (-8).dp).background(accent))
    }
}

/** A wooden shelf board with a lit front edge and a shadow beneath. */
@Composable
private fun Plank() {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF6B4A2F), Color(0xFF4A321F), Color(0xFF2E1F13)))),
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFA77B52).copy(alpha = 0.7f)))
            Canvas(Modifier.fillMaxSize()) {
                // Wood grain.
                val r = java.util.Random(7)
                repeat(10) {
                    val y = r.nextFloat() * size.height
                    drawLine(Color.Black.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y + r.nextFloat() * 3f - 1.5f), 1f)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(14.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
    }
}
