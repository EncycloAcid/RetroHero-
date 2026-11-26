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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter

private fun bezelShapeFor(formFactor: BezelFormFactor): RoundedCornerShape = when (formFactor) {
    BezelFormFactor.GameBoyChamfer -> RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 34.dp,
    )
    BezelFormFactor.HandheldWide -> RoundedCornerShape(22.dp)
    BezelFormFactor.Clamshell -> RoundedCornerShape(16.dp)
    BezelFormFactor.HomeConsole -> RoundedCornerShape(14.dp)
    BezelFormFactor.ComputerMonitor -> RoundedCornerShape(10.dp)
    BezelFormFactor.ArcadeCabinet -> RoundedCornerShape(8.dp)
}

private fun DrawScope.drawWoodgrainOverlay() {
    val lines = 28
    val step = size.height / lines
    for (i in 0 until lines) {
        val y = i * step
        val alpha = if (i % 3 == 0) 0.14f else if (i % 2 == 0) 0.08f else 0.05f
        val tone = if (i % 4 == 0) Color(0xFF1A0D04) else Color(0xFF9C6638)
        drawLine(
            color = tone.copy(alpha = alpha),
            start = Offset(0f, y),
            end = Offset(size.width, y + ((i % 3) - 1) * 3f),
            strokeWidth = if (i % 3 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
        )
    }
}

@Composable
private fun BezelStripeStack(
    stripeA: Color,
    stripeB: Color,
    stripeC: Color?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(stripeA))
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(stripeB))
        if (stripeC != null) {
            Box(Modifier.fillMaxWidth().height(1.5.dp).background(stripeC))
        }
    }
}

/**
 * Full-device console-authentic physical shell for portrait gameplay:
 * wraps both the live game display inside a console-specific molded bezel (with form-factor silhouette,
 * glowing Power/Status LED, hardware accent pinstripes, console subtitle, and stamped wordmark) and
 * the sculpted tactile controller deck (with speaker grille and seamless hardware casing).
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
            .background(
                Brush.verticalGradient(
                    listOf(style.plateTop, lerp(style.plateTop, style.plateBottom, 0.52f), style.plateBottom),
                ),
            ),
    ) {
        // Subtle casing highlights, optional vintage woodgrain, and etched console speaker grille
        Canvas(Modifier.fillMaxSize()) {
            if (style.woodgrain) drawWoodgrainOverlay()

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
            // ---- Upper Half: Console-Authentic Screen Bezel ------------------------------------
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {
                val bezelShape = bezelShapeFor(style.formFactor)
                val screenCorner = when (style.formFactor) {
                    BezelFormFactor.ArcadeCabinet, BezelFormFactor.ComputerMonitor -> 6.dp
                    BezelFormFactor.HandheldWide -> 10.dp
                    else -> 8.dp
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(bezelShape)
                        .background(Brush.verticalGradient(listOf(lerp(style.bezel, Color.White, 0.05f), style.bezel)))
                        .border(1.5.dp, style.bezelBorder, bezelShape)
                        .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 9.dp),
                ) {
                    // Console-specific top accent pinstripes & hardware subtitle
                    if (style.bezelStripeA != null && style.bezelStripeB != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 7.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BezelStripeStack(
                                stripeA = style.bezelStripeA,
                                stripeB = style.bezelStripeB,
                                stripeC = style.bezelStripeC,
                                modifier = Modifier.weight(1f),
                            )
                            if (style.bezelSubtitle.isNotBlank()) {
                                Text(
                                    style.bezelSubtitle,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 7.5.sp,
                                        letterSpacing = 0.11.em,
                                    ),
                                    color = style.bezelWordmarkColor.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BezelStripeStack(
                                stripeA = style.bezelStripeA,
                                stripeB = style.bezelStripeB,
                                stripeC = style.bezelStripeC,
                                modifier = Modifier.weight(0.45f),
                            )
                        }
                    }

                    // Recessed Glass Display Cutout
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(screenCorner))
                            .background(Color.Black)
                            .border(1.dp, Color.Black.copy(alpha = 0.85f), RoundedCornerShape(screenCorner)),
                        contentAlignment = Alignment.Center,
                    ) {
                        gameContent()
                    }

                    // Bezel Bottom Lip: Glowing Power/Status LED + Stamped Console Wordmark
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp,
                                start = 4.dp,
                                end = if (style.formFactor == BezelFormFactor.GameBoyChamfer) 16.dp else 8.dp,
                            ),
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
                                maxLines = 1,
                            )
                        }
                        Text(
                            style.bezelWordmark,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.sp,
                                letterSpacing = 0.09.em,
                            ),
                            color = style.bezelWordmarkColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                        Spacer(Modifier.width(if (style.formFactor == BezelFormFactor.GameBoyChamfer) 18.dp else 10.dp))
                    }
                }
            }

            // ---- Molded Housing Groove Seam (with Clamshell hinge barrels for DS / 3DS) --------
            if (style.formFactor == BezelFormFactor.Clamshell) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(3.dp)),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .height(1.5.dp)
                            .background(Color.Black.copy(alpha = 0.25f)),
                    )
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(3.dp)),
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.22f)))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                }
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

/**
 * Console-authentic landscape shell: dresses the left and right ergonomic grip margins in the
 * exact console's hardware housing (casing gradient, optional woodgrain, power LED & subtle
 * console wordmark) while keeping the centered live game display at full unobstructed height.
 */
@Composable
fun LandscapeConsoleShell(
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
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(style.plateTop, Color.Black, 0.12f),
                        lerp(style.plateBottom, Color.Black, 0.20f),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (style.woodgrain) drawWoodgrainOverlay()
            // Subtle side grip bevel shading
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.22f),
                    0.06f to Color.White.copy(alpha = 0.06f),
                    0.22f to Color.Transparent,
                    0.78f to Color.Transparent,
                    0.94f to Color.White.copy(alpha = 0.06f),
                    1f to Color.Black.copy(alpha = 0.24f),
                ),
            )
            // Subtle accent pinstripe bar along the very top edge
            if (style.bezelStripeA != null) {
                drawRect(
                    color = style.bezelStripeA.copy(alpha = 0.65f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 2.dp.toPx()),
                )
            }
            if (style.bezelStripeB != null) {
                drawRect(
                    color = style.bezelStripeB.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 2.dp.toPx()),
                    size = Size(size.width, 1.5.dp.toPx()),
                )
            }
        }

        // Full-height recessed display viewport
        Box(
            Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            gameContent()
        }

        // Subtle console wordmark + status LED tucked cleanly at bottom-center safe margin
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(style.bezel.copy(alpha = 0.72f))
                .border(0.75.dp, style.bezelBorder.copy(alpha = 0.75f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(activeLed),
            )
            Text(
                style.bezelWordmark,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 9.sp,
                    letterSpacing = 0.10.em,
                ),
                color = style.bezelWordmarkColor.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        padContent()
    }
}
