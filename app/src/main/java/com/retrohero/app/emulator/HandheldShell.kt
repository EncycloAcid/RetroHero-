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
import androidx.compose.ui.graphics.BlendMode
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

private fun shellLuminance(c: Color): Float =
    (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue).coerceIn(0f, 1f)

private fun DrawScope.drawPortraitAmbilight(ambilight: AmbilightState, surfaceLuma: Float = 0f) {
    if (!ambilight.isVisiblyGlowing) return
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return

    val flashBoost = 1f + ambilight.flash * 0.70f
    val gain = ambilight.intensity * flashBoost
    // Light matte shells (SNES/NES/DMG/Dreamcast) need SrcOver to tint bright ABS plastic with colored light;
    // dark shells & pure-black OLED backdrops shine brightest with additive Screen blending.
    val lightShell = surfaceLuma > 0.38f
    val mode = if (lightShell) BlendMode.SrcOver else BlendMode.Screen
    val modeScale = if (lightShell) 0.78f else 1.0f

    fun bloom(color: Color, center: Offset, radius: Float, baseAlpha: Float) {
        val energy = AmbilightState.edgeEnergy(color)
        val alpha = (baseAlpha * modeScale * energy * gain).coerceIn(0f, if (lightShell) 0.52f else 0.74f)
        if (alpha <= 0.015f || radius <= 2f) return
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to color.copy(alpha = alpha),
                0.52f to color.copy(alpha = alpha * 0.44f),
                1.0f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            blendMode = mode,
        )
    }

    // Upper bezel crown & side flank halos
    bloom(ambilight.top, Offset(w * 0.50f, h * 0.07f), w * (0.74f + 0.16f * ambilight.flash), 0.38f)
    bloom(ambilight.left, Offset(w * 0.04f, h * 0.27f), w * (0.52f + 0.12f * ambilight.flash), 0.36f)
    bloom(ambilight.right, Offset(w * 0.96f, h * 0.27f), w * (0.52f + 0.12f * ambilight.flash), 0.36f)

    // Downward OLED Ambilight wash across the physical controller deck & left/right button zones
    bloom(ambilight.bottom, Offset(w * 0.50f, h * 0.51f), w * (0.90f + 0.22f * ambilight.flash), 0.44f)
    bloom(ambilight.left, Offset(w * 0.18f, h * 0.60f), w * 0.54f, 0.28f)
    bloom(ambilight.right, Offset(w * 0.82f, h * 0.60f), w * 0.54f, 0.28f)
}

private fun DrawScope.drawLandscapeAmbilight(ambilight: AmbilightState, surfaceLuma: Float = 0f) {
    if (!ambilight.isVisiblyGlowing) return
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return

    val flashBoost = 1f + ambilight.flash * 0.72f
    val gain = ambilight.intensity * flashBoost
    val sideRadius = maxOf(w * 0.42f, h * 0.82f) * (1f + 0.16f * ambilight.flash)
    val lightShell = surfaceLuma > 0.38f
    val mode = if (lightShell) BlendMode.SrcOver else BlendMode.Screen
    val modeScale = if (lightShell) 0.78f else 1.0f

    fun bloom(color: Color, center: Offset, radius: Float, baseAlpha: Float) {
        val energy = AmbilightState.edgeEnergy(color)
        val alpha = (baseAlpha * modeScale * energy * gain).coerceIn(0f, if (lightShell) 0.54f else 0.78f)
        if (alpha <= 0.015f || radius <= 2f) return
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to color.copy(alpha = alpha),
                0.50f to color.copy(alpha = alpha * 0.46f),
                1.0f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            blendMode = mode,
        )
    }

    bloom(ambilight.left, Offset(w * 0.16f, h * 0.50f), sideRadius, 0.46f)
    bloom(ambilight.right, Offset(w * 0.84f, h * 0.50f), sideRadius, 0.46f)
    bloom(ambilight.top, Offset(w * 0.50f, h * 0.04f), w * 0.48f, 0.30f)
    bloom(ambilight.bottom, Offset(w * 0.50f, h * 0.96f), w * 0.48f, 0.30f)
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
 * Standalone OLED Ambilight cinema halo for translucent / minimal touch skins or controller-only play
 * on pure black backgrounds (both landscape and portrait).
 */
@Composable
fun AmbilightBackdropHalo(
    ambilight: AmbilightState,
    landscape: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!ambilight.isVisiblyGlowing) return
    Canvas(modifier.fillMaxSize()) {
        if (landscape) drawLandscapeAmbilight(ambilight, 0f) else drawPortraitAmbilight(ambilight, 0f)
    }
}

/**
 * Full-device console-authentic physical shell for portrait gameplay:
 * wraps both the live game display inside a console-specific molded bezel (with form-factor silhouette,
 * glowing Power/Status LED, hardware accent pinstripes, console subtitle, live OLED Ambilight halo,
 * and stamped wordmark) and the sculpted tactile controller deck.
 */
@Composable
fun HandheldShell(
    style: ConsoleStyle,
    fastForwardActive: Boolean = false,
    sustainedCount: Int = 0,
    ambilight: AmbilightState = AmbilightState.Off,
    padHeightFraction: Float = 0.47f,
    ledColorOverride: Color? = null,
    modifier: Modifier = Modifier,
    gameContent: @Composable () -> Unit,
    padContent: @Composable () -> Unit,
) {
    val activeLed = when {
        fastForwardActive || sustainedCount > 0 -> Color(0xFFFF6A1A)
        ledColorOverride != null -> ledColorOverride
        else -> style.ledColor
    }
    val statusText = when {
        sustainedCount > 0 -> "HOLD ×$sustainedCount"
        fastForwardActive -> "FAST FWD"
        else -> "POWER"
    }

    val rimTintFraction = if (ambilight.isVisiblyGlowing) {
        (0.45f * ambilight.maxEnergy * ambilight.intensity + ambilight.flash * 0.30f).coerceIn(0f, 0.82f)
    } else 0f
    val dynamicBezelBorder = if (rimTintFraction > 0f) {
        lerp(style.bezelBorder, ambilight.rimColor, rimTintFraction)
    } else style.bezelBorder
    val portraitLuma = shellLuminance(lerp(style.plateTop, style.plateBottom, 0.5f))

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(style.plateTop, lerp(style.plateTop, style.plateBottom, 0.52f), style.plateBottom),
                ),
            ),
    ) {
        // Subtle casing highlights, optional vintage woodgrain, live OLED Ambilight halo, and speaker grille
        Canvas(Modifier.fillMaxSize()) {
            if (style.woodgrain) drawWoodgrainOverlay()

            // Live Framebuffer OLED Ambilight halo across casing & controller deck
            drawPortraitAmbilight(ambilight, portraitLuma)

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
                val bezelTopGlass = if (rimTintFraction > 0f) {
                    lerp(lerp(style.bezel, Color.White, 0.05f), ambilight.top, rimTintFraction * 0.24f)
                } else lerp(style.bezel, Color.White, 0.05f)
                val bezelBottomGlass = if (rimTintFraction > 0f) {
                    lerp(style.bezel, ambilight.bottom, rimTintFraction * 0.24f)
                } else style.bezel
                val innerCutoutBorder = if (rimTintFraction > 0f) {
                    lerp(Color.Black.copy(alpha = 0.85f), ambilight.rimColor, (rimTintFraction * 0.78f).coerceAtMost(0.75f))
                } else Color.Black.copy(alpha = 0.85f)

                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(bezelShape)
                        .background(Brush.verticalGradient(listOf(bezelTopGlass, bezelBottomGlass)))
                        .border(1.5.dp, dynamicBezelBorder, bezelShape)
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
                            .border(1.dp, innerCutoutBorder, RoundedCornerShape(screenCorner)),
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
                val seamGlow = if (rimTintFraction > 0f) {
                    ambilight.bottom.copy(alpha = (rimTintFraction * 0.36f).coerceAtMost(0.42f))
                } else Color.White.copy(alpha = 0.16f)
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.22f)))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(seamGlow))
                }
            }

            // ---- Lower Half: Sculpted Tactile Control Deck -------------------------------------
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(padHeightFraction.coerceIn(0.32f, 0.55f)),
            ) {
                padContent()
            }
        }
    }
}

/**
 * Console-authentic landscape shell: dresses the left and right ergonomic grip margins in the
 * exact console's hardware housing (casing gradient, optional woodgrain, live directional OLED
 * Ambilight grip halos, power LED & subtle console wordmark) while keeping the centered live game
 * display at full unobstructed height.
 */
@Composable
fun LandscapeConsoleShell(
    style: ConsoleStyle,
    fastForwardActive: Boolean = false,
    sustainedCount: Int = 0,
    ambilight: AmbilightState = AmbilightState.Off,
    ledColorOverride: Color? = null,
    modifier: Modifier = Modifier,
    gameContent: @Composable () -> Unit,
    padContent: @Composable () -> Unit,
) {
    val activeLed = when {
        fastForwardActive || sustainedCount > 0 -> Color(0xFFFF6A1A)
        ledColorOverride != null -> ledColorOverride
        else -> style.ledColor
    }
    val pillBorder = if (ambilight.isVisiblyGlowing) {
        val f = (0.45f * ambilight.maxEnergy * ambilight.intensity + ambilight.flash * 0.30f).coerceIn(0f, 0.80f)
        lerp(style.bezelBorder.copy(alpha = 0.75f), ambilight.bottom, f)
    } else style.bezelBorder.copy(alpha = 0.75f)

    val gripTop = lerp(style.plateTop, Color.Black, 0.12f)
    val gripBottom = lerp(style.plateBottom, Color.Black, 0.20f)
    val landscapeLuma = shellLuminance(lerp(gripTop, gripBottom, 0.5f))

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(gripTop, gripBottom),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (style.woodgrain) drawWoodgrainOverlay()
            // Live directional OLED Ambilight glow across left & right landscape grips
            drawLandscapeAmbilight(ambilight, landscapeLuma)

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
                .border(0.75.dp, pillBorder, RoundedCornerShape(50))
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
