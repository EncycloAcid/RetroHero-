package com.retrohero.app.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter

/**
 * Full-device Delta-inspired physical handheld shell for portrait gameplay:
 * wraps both the live game display inside a molded console bezel (with glowing Power LED,
 * optional classic accent pinstripes, and stamped wordmark) and the sculpted tactile
 * controller deck (with speaker grille and seamless hardware casing) as one cohesive device.
 */
@Composable
fun HandheldShell(
    style: ConsoleStyle,
    fastForwardActive: Boolean = false,
    sustainedCount: Int = 0,
    modifier: Modifier = Modifier,
    gameContent: @Composable () -> Unit,
    padContent: @Composable () -> Unit,
) {
    val activeLed = when {
        fastForwardActive || sustainedCount > 0 -> Color(0xFFFF6A1A)
        else -> style.ledColor
    }
    val statusText = when {
        sustainedCount > 0 -> "HOLD ×$sustainedCount"
        fastForwardActive -> "FAST FWD"
        else -> "POWER"
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(style.plateTop, lerp(style.plateTop, style.plateBottom, 0.52f), style.plateBottom))),
    ) {
        // Subtle top-edge specular casing highlight + etched speaker grille in lower deck
        Canvas(Modifier.fillMaxSize()) {
            // Subtle molded casing bevel around screen edges
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.14f),
                    0.04f to Color.White.copy(alpha = 0.08f),
                    0.5f to Color.Transparent,
                    0.96f to Color.White.copy(alpha = 0.08f),
                    1f to Color.Black.copy(alpha = 0.16f),
                ),
            )
            // Etched console speaker grille in the lower-right corner of the controller deck
            when (style.speaker) {
                SpeakerStyle.DotMatrix -> {
                    val cols = 5
                    val rows = 4
                    val spacing = 7.dp.toPx()
                    val radius = 1.7.dp.toPx()
                    val startX = size.width - 28.dp.toPx() - (cols - 1) * spacing
                    val startY = size.height - 86.dp.toPx() - (rows - 1) * spacing
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            if ((r == 0 || r == rows - 1) && (c == 0 || c == cols - 1)) continue
                            val cx = startX + c * spacing
                            val cy = startY + r * spacing
                            drawCircle(Color.Black.copy(alpha = 0.34f), radius, Offset(cx, cy))
                            drawCircle(Color.White.copy(alpha = 0.10f), radius * 0.55f, Offset(cx, cy + 0.8f))
                        }
                    }
                }
                SpeakerStyle.Slotted -> {
                    val slots = 5
                    val slotW = 3.2.dp.toPx()
                    val slotH = 26.dp.toPx()
                    val gap = 7.dp.toPx()
                    val pivot = Offset(size.width - 44.dp.toPx(), size.height - 96.dp.toPx())
                    rotate(-26f, pivot = pivot) {
                        for (i in 0 until slots) {
                            val x = pivot.x - ((slots - 1) * gap) / 2f + i * gap
                            val y = pivot.y - slotH / 2f
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.32f),
                                topLeft = Offset(x, y),
                                size = Size(slotW, slotH),
                                cornerRadius = CornerRadius(slotW / 2f, slotW / 2f),
                            )
                        }
                    }
                }
                SpeakerStyle.TwinDots -> {
                    val spacing = 6.dp.toPx()
                    val radius = 1.6.dp.toPx()
                    val yBase = size.height * 0.56f
                    val centerX = size.width / 2f
                    listOf(centerX - 24.dp.toPx(), centerX + 12.dp.toPx()).forEach { xBase ->
                        for (r in 0..1) {
                            for (c in 0..2) {
                                drawCircle(
                                    Color.Black.copy(alpha = 0.28f),
                                    radius,
                                    Offset(xBase + c * spacing, yBase + r * spacing),
                                )
                            }
                        }
                    }
                }
                SpeakerStyle.None -> Unit
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            // ---- Upper Half: Molded Handheld Screen Bezel -------------------------------------
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {
                val bezelShape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 34.dp,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(bezelShape)
                        .background(Brush.verticalGradient(listOf(lerp(style.bezel, Color.White, 0.05f), style.bezel)))
                        .border(1.5.dp, style.bezelBorder, bezelShape)
                        .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 9.dp),
                ) {
                    // Optional classic twin top accent pinstripes (Game Boy / NES / SNES / Genesis)
                    if (style.bezelStripeA != null && style.bezelStripeB != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 7.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(Modifier.fillMaxWidth().height(1.5.dp).background(style.bezelStripeA))
                                Box(Modifier.fillMaxWidth().height(1.5.dp).background(style.bezelStripeB))
                            }
                            Text(
                                style.bezelSubtitle,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 7.5.sp,
                                    letterSpacing = 0.12.em,
                                ),
                                color = style.bezelWordmarkColor.copy(alpha = 0.72f),
                            )
                            Column(Modifier.weight(0.45f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(Modifier.fillMaxWidth().height(1.5.dp).background(style.bezelStripeA))
                                Box(Modifier.fillMaxWidth().height(1.5.dp).background(style.bezelStripeB))
                            }
                        }
                    }

                    // Recessed Glass Display Cutout
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        gameContent()
                    }

                    // Bezel Bottom Lip: Glowing Power/Status LED + Stamped Console Wordmark
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(activeLed.copy(alpha = 0.75f), Color.Transparent),
                                        ),
                                        radius = size.minDimension / 2f,
                                    )
                                }
                                Box(
                                    Modifier
                                        .size(5.5.dp)
                                        .clip(CircleShape)
                                        .background(activeLed),
                                )
                            }
                            Text(
                                statusText,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.10.em,
                                ),
                                color = if (fastForwardActive || sustainedCount > 0) activeLed else style.bezelWordmarkColor.copy(alpha = 0.72f),
                            )
                        }
                        Text(
                            style.bezelWordmark,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.sp,
                                letterSpacing = 0.10.em,
                            ),
                            color = style.bezelWordmarkColor,
                        )
                        Spacer(Modifier.width(24.dp))
                    }
                }
            }

            // ---- Molded Housing Groove Seam ----------------------------------------------------
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.22f)))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
            }

            // ---- Lower Half: Sculpted Tactile Control Deck -------------------------------------
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.47f),
            ) {
                padContent()
            }
        }
    }
}
