package com.retrohero.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Photorealistic 3/4-perspective industrial-design hardware renderers for the Console Media
 * Insertion Launch Ceremony. Each console has a back/interior layer (drawn behind the 3D media)
 * and a foreground/lip/lid layer (drawn over the media so cartridges, cards, and discs physically
 * vanish into real slots, trays, bays, and lids).
 */

internal fun DrawScope.drawConsoleShadow(cx: Float, bottomY: Float, width: Float, height: Float = 28.dp.toPx()) {
    drawOval(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.68f),
            0.65f to Color.Black.copy(alpha = 0.28f),
            1f to Color.Transparent,
            center = Offset(cx, bottomY),
            radius = (width * 0.56f).coerceAtLeast(16f),
        ),
        topLeft = Offset(cx - width * 0.56f, bottomY - height * 0.5f),
        size = Size(width * 1.12f, height),
    )
}

internal fun DrawScope.drawGlowingLed(
    center: Offset,
    color: Color,
    glow: Float,
    radiusPx: Float = 3.8.dp.toPx(),
    haloPx: Float = 18.dp.toPx(),
) {
    if (glow > 0.02f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = 0.90f * glow),
                0.45f to color.copy(alpha = 0.34f * glow),
                1f to Color.Transparent,
                center = center,
                radius = haloPx,
            ),
            radius = haloPx,
            center = center,
        )
    }
    drawCircle(
        color = if (glow > 0.05f) {
            color.copy(alpha = (0.35f + glow * 0.65f).coerceIn(0f, 1f))
        } else {
            Color(0xFF14161A)
        },
        radius = radiusPx,
        center = center,
    )
    if (glow > 0.15f) {
        drawCircle(
            color = Color.White.copy(alpha = 0.68f * glow),
            radius = radiusPx * 0.42f,
            center = Offset(center.x - radiusPx * 0.22f, center.y - radiusPx * 0.22f),
        )
    }
}

// =================================================================================================
// 1. NINTENDO ENTERTAINMENT SYSTEM (NES-001) — Front-Loading ZIF Deck with Flip-Up Door
// =================================================================================================

internal fun DrawScope.drawNesConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW

    val roofTop = h * 0.24f
    val frontTop = h * 0.38f
    val midBandY = h * 0.62f
    val bottomY = h * 0.82f

    drawConsoleShadow(cx, bottomY + 8.dp.toPx(), deckW * 1.06f, 32.dp.toPx())

    // Perspective light-grey upper roof of NES-001
    val roofPath = Path().apply {
        moveTo(deckL + 22.dp.toPx(), roofTop)
        lineTo(deckR - 22.dp.toPx(), roofTop)
        lineTo(deckR, frontTop)
        lineTo(deckL, frontTop)
        close()
    }
    drawPath(
        path = roofPath,
        brush = Brush.verticalGradient(
            0f to Color(0xFFDFDDD6),
            1f to Color(0xFFC9C7BF),
            startY = roofTop,
            endY = frontTop,
        ),
    )

    // Iconic full-depth matte-black cooling vent channel on the right side of the NES roof
    val ventL = deckL + deckW * 0.67f
    val ventW = deckW * 0.145f
    val ventRoofPath = Path().apply {
        moveTo(ventL + 6.dp.toPx(), roofTop)
        lineTo(ventL + ventW + 1.dp.toPx(), roofTop)
        lineTo(ventL + ventW, frontTop)
        lineTo(ventL, frontTop)
        close()
    }
    drawPath(ventRoofPath, color = Color(0xFF1A1B1F))
    for (i in 1..6) {
        val t = i / 7f
        val y = roofTop + (frontTop - roofTop) * t
        val xl = ventL + 6.dp.toPx() * (1f - t) + 3.dp.toPx()
        val xr = ventL + ventW + 1.dp.toPx() * (1f - t) - 3.dp.toPx()
        drawLine(Color(0xFF32353D), Offset(xl, y), Offset(xr, y), strokeWidth = 1.4.dp.toPx())
    }

    // Upper-front light-grey fascia
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFD5D3CC),
            1f to Color(0xFFBDBBB3),
            startY = frontTop,
            endY = midBandY,
        ),
        topLeft = Offset(deckL, frontTop),
        size = Size(deckW, midBandY - frontTop),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )

    // Lower charcoal-grey NES stepped base apron
    val baseInset = 6.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF6B6D73),
            0.3f to Color(0xFF52545A),
            1f to Color(0xFF3A3C42),
            startY = midBandY,
            endY = bottomY,
        ),
        topLeft = Offset(deckL + baseInset, midBandY),
        size = Size(deckW - baseInset * 2f, bottomY - midBandY),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )

    // Black vent channel continuing straight down the upper-front right side!
    drawRect(
        color = Color(0xFF18191D),
        topLeft = Offset(ventL, frontTop),
        size = Size(ventW, midBandY - frontTop),
    )

    // Deep dark recessed front cartridge bay (left-center, matching real NES-001!)
    val bayL = deckL + deckW * 0.06f
    val bayW = deckW * 0.56f
    val bayTop = frontTop + 8.dp.toPx()
    val bayH = (midBandY - frontTop) - 12.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF060709),
            0.6f to Color(0xFF13151B),
            1f to Color(0xFF1D2028),
            startY = bayTop,
            endY = bayTop + bayH,
        ),
        topLeft = Offset(bayL, bayTop),
        size = Size(bayW, bayH),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    // Internal metal ZIF sled guide rails inside the open bay
    val zifDrop = (frame.insertDepth - 0.65f).coerceIn(0f, 0.35f) / 0.35f
    val sledY = bayTop + bayH * (0.48f + zifDrop * 0.24f)
    drawLine(Color(0xFF474C59), Offset(bayL + 8.dp.toPx(), sledY), Offset(bayL + bayW - 8.dp.toPx(), sledY), strokeWidth = 2.dp.toPx())
    drawRect(
        color = Color(0xFF2A2E38),
        topLeft = Offset(bayL + 10.dp.toPx(), sledY),
        size = Size(bayW - 20.dp.toPx(), 6.dp.toPx()),
    )

    // Lower-left recessed charcoal button panel: POWER + RESET + red LED
    val panelL = deckL + 16.dp.toPx()
    val panelTop = midBandY + 12.dp.toPx()
    val panelH = (bottomY - midBandY) - 22.dp.toPx()
    drawRoundRect(
        color = Color(0xFF2A2C31),
        topLeft = Offset(panelL, panelTop),
        size = Size(deckW * 0.36f, panelH),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    // Red LED left of POWER
    drawGlowingLed(
        center = Offset(panelL + 11.dp.toPx(), panelTop + panelH * 0.5f),
        color = Color(0xFFFF2430),
        glow = frame.ledGlow,
        radiusPx = 3.2.dp.toPx(),
        haloPx = 15.dp.toPx(),
    )
    // Tactile grey square POWER and RESET push buttons
    val btnW = 26.dp.toPx()
    val btnH = (panelH - 12.dp.toPx()).coerceAtLeast(14.dp.toPx())
    val btnY = panelTop + (panelH - btnH) * 0.5f
    drawRoundRect(Color(0xFF9FA2AA), Offset(panelL + 22.dp.toPx(), btnY), Size(btnW, btnH), CornerRadius(3.dp.toPx()))
    drawRoundRect(Color(0xFF8C9099), Offset(panelL + 54.dp.toPx(), btnY), Size(btnW, btnH), CornerRadius(3.dp.toPx()))
    // Subtle red accent lines beneath POWER / RESET
    drawLine(Color(0xFFD62828), Offset(panelL + 24.dp.toPx(), btnY + btnH + 3.dp.toPx()), Offset(panelL + 46.dp.toPx(), btnY + btnH + 3.dp.toPx()), 1.4.dp.toPx())
    drawLine(Color(0xFFD62828), Offset(panelL + 56.dp.toPx(), btnY + btnH + 3.dp.toPx()), Offset(panelL + 78.dp.toPx(), btnY + btnH + 3.dp.toPx()), 1.4.dp.toPx())

    // Lower-right twin iconic 7-pin NES controller ports
    val portAreaL = deckL + deckW * 0.62f
    for (p in 0..1) {
        val px = portAreaL + p * 44.dp.toPx()
        drawRoundRect(Color(0xFF1E2026), Offset(px, panelTop + 2.dp.toPx()), Size(34.dp.toPx(), panelH - 4.dp.toPx()), CornerRadius(4.dp.toPx()))
        drawRoundRect(Color(0xFF101116), Offset(px + 5.dp.toPx(), panelTop + 7.dp.toPx()), Size(24.dp.toPx(), panelH - 14.dp.toPx()), CornerRadius(2.dp.toPx()))
        // Tiny golden pin holes inside port
        for (pin in 0..3) {
            drawCircle(Color(0xFF6B7080), 1.3.dp.toPx(), Offset(px + 9.dp.toPx() + pin * 5.dp.toPx(), panelTop + panelH * 0.42f))
        }
        for (pin in 0..2) {
            drawCircle(Color(0xFF6B7080), 1.3.dp.toPx(), Offset(px + 11.dp.toPx() + pin * 5.dp.toPx(), panelTop + panelH * 0.62f))
        }
    }
}

internal fun DrawScope.drawNesConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val frontTop = h * 0.38f
    val midBandY = h * 0.62f

    val bayL = deckL + deckW * 0.06f
    val bayW = deckW * 0.56f
    val bayTop = frontTop + 8.dp.toPx()
    val bayH = (midBandY - frontTop) - 12.dp.toPx()

    // Hinged NES front cartridge bay door — swings UP around bayTop when open, closes flush over the cart!
    val open = frame.lidOpen.coerceIn(0f, 1f)
    if (open > 0.02f) {
        // Swung upward in 3D perspective above bayTop, anchored cleanly at bayTop hinge!
        val doorVisibleH = bayH * (0.26f + 0.74f * (1f - open))
        val doorTop = bayTop - open * 16.dp.toPx()
        val inset = open * 7.dp.toPx()
        val doorPath = Path().apply {
            moveTo(bayL + inset, doorTop)
            lineTo(bayL + bayW - inset, doorTop)
            lineTo(bayL + bayW, doorTop + doorVisibleH)
            lineTo(bayL, doorTop + doorVisibleH)
            close()
        }
        drawPath(
            path = doorPath,
            brush = Brush.verticalGradient(
                0f to Color(0xFFE7E5DE),
                0.78f to Color(0xFFC8C6BE),
                1f to Color(0xFF9C9A92),
                startY = doorTop,
                endY = doorTop + doorVisibleH,
            ),
        )
        drawPath(doorPath, color = Color(0xFF6B6A64).copy(alpha = 0.55f), style = Stroke(1.2.dp.toPx()))
        // Underside shadow strip along bottom edge of raised door
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(bayL + 2.dp.toPx(), doorTop + doorVisibleH + 1.dp.toPx()),
            end = Offset(bayL + bayW - 2.dp.toPx(), doorTop + doorVisibleH + 1.dp.toPx()),
            strokeWidth = 2.4.dp.toPx(),
        )
        // Famous red "Nintendo ENTERTAINMENT SYSTEM" stripe along the lower lip of the door
        drawLine(
            color = Color(0xFFD62828),
            start = Offset(bayL + 8.dp.toPx(), doorTop + doorVisibleH - 5.dp.toPx()),
            end = Offset(bayL + bayW * 0.62f, doorTop + doorVisibleH - 5.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    } else {
        // Fully closed flush cartridge door
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFDAD8D1),
                1f to Color(0xFFBFBDB5),
                startY = bayTop,
                endY = bayTop + bayH,
            ),
            topLeft = Offset(bayL, bayTop),
            size = Size(bayW, bayH),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFF7C7B76).copy(alpha = 0.55f),
            topLeft = Offset(bayL, bayTop),
            size = Size(bayW, bayH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(1.2.dp.toPx()),
        )
        // Red NES front emblem accent bar
        drawLine(
            color = Color(0xFFD62828),
            start = Offset(bayL + 10.dp.toPx(), bayTop + bayH - 12.dp.toPx()),
            end = Offset(bayL + bayW * 0.64f, bayTop + bayH - 12.dp.toPx()),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Finger lift notch at bottom-left of door
        drawRoundRect(
            color = Color(0xFF8C8A84),
            topLeft = Offset(bayL + 10.dp.toPx(), bayTop + bayH - 5.dp.toPx()),
            size = Size(24.dp.toPx(), 3.dp.toPx()),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
        )
    }

    // Crisp front top bevel highlight across the whole NES lip
    drawLine(
        color = Color.White.copy(alpha = 0.42f),
        start = Offset(deckL + 2.dp.toPx(), frontTop + 1.dp.toPx()),
        end = Offset(deckL + deckW - 2.dp.toPx(), frontTop + 1.dp.toPx()),
        strokeWidth = 1.4.dp.toPx(),
    )
}

// =================================================================================================
// 2. SUPER NINTENDO (NA SNS-001) — Sculpted Two-Tone Grey Deck, Purple Switches & Eject Bar
// =================================================================================================

internal fun DrawScope.drawSnesConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.46f
    val slotLipY = h * 0.61f
    val bottomY = h * 0.93f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), deckW * 1.06f)

    // Sculpted light-grey upper deck in 3/4 perspective
    val topDeck = Path().apply {
        moveTo(deckL + 20.dp.toPx(), topY)
        lineTo(deckR - 20.dp.toPx(), topY)
        lineTo(deckR, slotLipY + 14.dp.toPx())
        lineTo(deckL, slotLipY + 14.dp.toPx())
        close()
    }
    drawPath(
        path = topDeck,
        brush = Brush.verticalGradient(
            0f to Color(0xFFE2E0E5),
            1f to Color(0xFFC5C2CC),
            startY = topY,
            endY = slotLipY + 14.dp.toPx(),
        ),
    )

    // Iconic darker-grey recessed center platform framing the SNES cartridge well
    val wellPadW = deckW * 0.66f
    val wellPadL = cx - wellPadW * 0.5f
    val wellDeck = Path().apply {
        moveTo(wellPadL + 8.dp.toPx(), topY + 6.dp.toPx())
        lineTo(wellPadL + wellPadW - 8.dp.toPx(), topY + 6.dp.toPx())
        lineTo(wellPadL + wellPadW, slotLipY + 8.dp.toPx())
        lineTo(wellPadL, slotLipY + 8.dp.toPx())
        close()
    }
    drawPath(wellDeck, color = Color(0xFF9E9BA8))

    // Deep dark recessed cartridge slot mouth
    val slotW = deckW * 0.58f
    val slotL = cx - slotW * 0.5f
    val slotH = 22.dp.toPx()
    drawRoundRect(
        color = Color(0xFF101116),
        topLeft = Offset(slotL, slotLipY - slotH * 0.72f),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )

    // Twin grey spring-loaded SNES dust flaps parting as the cartridge enters
    val flapGap = frame.insertDepth * 6.dp.toPx()
    val flapH = (6.dp.toPx() * (1f - frame.insertDepth * 0.6f)).coerceAtLeast(2.dp.toPx())
    drawRoundRect(
        color = Color(0xFF7B7888),
        topLeft = Offset(slotL + 4.dp.toPx(), slotLipY - slotH * 0.66f),
        size = Size(slotW - 8.dp.toPx(), flapH),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    if (flapGap < 5.dp.toPx()) {
        drawRoundRect(
            color = Color(0xFF6B6878),
            topLeft = Offset(slotL + 4.dp.toPx(), slotLipY - slotH * 0.25f),
            size = Size(slotW - 8.dp.toPx(), flapH * 0.85f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

internal fun DrawScope.drawSnesConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val slotLipY = h * 0.61f
    val upperFrontH = h * 0.165f
    val lowerApronY = slotLipY + upperFrontH
    val lowerApronH = h * 0.155f

    // Upper sculpted light-grey SNES front bezel overlapping the seated cartridge base
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFD5D2DC),
            1f to Color(0xFFB3AFBE),
            startY = slotLipY,
            endY = lowerApronY + 6.dp.toPx(),
        ),
        topLeft = Offset(deckL, slotLipY),
        size = Size(deckW, upperFrontH + 6.dp.toPx()),
        cornerRadius = CornerRadius(14.dp.toPx()),
    )

    // Crisp bevel catch-light along slot front lip
    drawLine(
        color = Color.White.copy(alpha = 0.52f),
        start = Offset(deckL + 12.dp.toPx(), slotLipY + 1.2.dp.toPx()),
        end = Offset(deckL + deckW - 12.dp.toPx(), slotLipY + 1.2.dp.toPx()),
        strokeWidth = 1.6.dp.toPx(),
    )

    // Iconic wide lavender EJECT bar centered directly in front of the cartridge slot!
    val ejectW = deckW * 0.34f
    val ejectH = 12.dp.toPx()
    val ejectY = slotLipY + 10.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF9C8CE8),
            1f to Color(0xFF7B69CE),
            startY = ejectY,
            endY = ejectY + ejectH,
        ),
        topLeft = Offset(cx - ejectW * 0.5f, ejectY),
        size = Size(ejectW, ejectH),
        cornerRadius = CornerRadius(5.dp.toPx()),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(cx - ejectW * 0.44f, ejectY + 1.5.dp.toPx()),
        end = Offset(cx + ejectW * 0.44f, ejectY + 1.5.dp.toPx()),
        strokeWidth = 1.1.dp.toPx(),
    )

    // Signature twin SNES purple sliding switches: POWER (left, slides UP/ON!) & RESET (right)
    val switchW = 28.dp.toPx()
    val switchTrackH = 26.dp.toPx()
    val switchY = slotLipY + 12.dp.toPx()

    // Left POWER switch track + sliding purple thumb
    val powerX = deckL + 22.dp.toPx()
    drawRoundRect(Color(0xFF6B6878), Offset(powerX, switchY), Size(switchW, switchTrackH), CornerRadius(5.dp.toPx()))
    val slideOffset = (1f - frame.ledGlow) * 9.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF836FD6), 1f to Color(0xFF5A48AD), startY = switchY, endY = switchY + 16.dp.toPx()),
        topLeft = Offset(powerX + 2.dp.toPx(), switchY + 2.dp.toPx() + slideOffset),
        size = Size(switchW - 4.dp.toPx(), 14.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )

    // Right RESET switch (lavender)
    val resetX = deckL + deckW - 22.dp.toPx() - switchW
    drawRoundRect(Color(0xFF6B6878), Offset(resetX, switchY), Size(switchW, switchTrackH), CornerRadius(5.dp.toPx()))
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFFA090EB), 1f to Color(0xFF7B69CE), startY = switchY + 8.dp.toPx(), endY = switchY + 24.dp.toPx()),
        topLeft = Offset(resetX + 2.dp.toPx(), switchY + 8.dp.toPx()),
        size = Size(switchW - 4.dp.toPx(), 14.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )

    // Signature SNES purple horizontal accent strip separating upper deck from lower dark-grey controller panel
    drawRoundRect(
        color = Color(0xFF6D5AC2),
        topLeft = Offset(deckL + 10.dp.toPx(), lowerApronY - 2.dp.toPx()),
        size = Size(deckW - 20.dp.toPx(), 4.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )

    // Lower dark-grey SNES front apron housing the red LED + twin rounded-D SNES controller ports!
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF7B7885),
            1f to Color(0xFF514F5B),
            startY = lowerApronY + 2.dp.toPx(),
            endY = lowerApronY + lowerApronH,
        ),
        topLeft = Offset(deckL + 8.dp.toPx(), lowerApronY + 2.dp.toPx()),
        size = Size(deckW - 16.dp.toPx(), lowerApronH - 2.dp.toPx()),
        cornerRadius = CornerRadius(10.dp.toPx()),
    )

    // Iconic red POWER LED on the far left of the front panel!
    drawGlowingLed(
        center = Offset(deckL + 24.dp.toPx(), lowerApronY + lowerApronH * 0.52f),
        color = Color(0xFFFF2A3A),
        glow = frame.ledGlow,
        radiusPx = 3.5.dp.toPx(),
        haloPx = 18.dp.toPx(),
    )

    // Twin authentic SNES 7-pin rounded-D controller ports
    val portW = 64.dp.toPx()
    val portH = 22.dp.toPx()
    val portY = lowerApronY + (lowerApronH - portH) * 0.54f
    val portPositions = listOf(cx - portW - 14.dp.toPx(), cx + 14.dp.toPx())
    portPositions.forEach { px ->
        drawRoundRect(Color(0xFF2B2A32), Offset(px, portY), Size(portW, portH), CornerRadius(11.dp.toPx()))
        drawRoundRect(Color(0xFF14131A), Offset(px + 4.dp.toPx(), portY + 4.dp.toPx()), Size(portW - 8.dp.toPx(), portH - 8.dp.toPx()), CornerRadius(7.dp.toPx()))
        for (pin in 0..5) {
            drawCircle(Color(0xFF8A8896), 1.4.dp.toPx(), Offset(px + 12.dp.toPx() + pin * 8.dp.toPx(), portY + portH * 0.5f))
        }
    }
}

// =================================================================================================
// 3. NINTENDO 64 (NUS-001) — Curved Dual-Arch Charcoal Shell + 4 Controller Ports + Red Jewel LED
// =================================================================================================

internal fun DrawScope.drawN64ConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.46f
    val slotLipY = h * 0.61f
    val bottomY = h * 0.92f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), deckW * 1.04f)

    // Curved N64 rounded dome top chassis
    val domePath = Path().apply {
        moveTo(deckL + 28.dp.toPx(), topY)
        cubicTo(cx - deckW * 0.18f, topY - 10.dp.toPx(), cx + deckW * 0.18f, topY - 10.dp.toPx(), deckR - 28.dp.toPx(), topY)
        quadraticTo(deckR - 4.dp.toPx(), topY + 18.dp.toPx(), deckR, slotLipY + 12.dp.toPx())
        lineTo(deckL, slotLipY + 12.dp.toPx())
        quadraticTo(deckL + 4.dp.toPx(), topY + 18.dp.toPx(), deckL + 28.dp.toPx(), topY)
        close()
    }
    drawPath(
        path = domePath,
        brush = Brush.verticalGradient(
            0f to Color(0xFF474B55),
            1f to Color(0xFF282B33),
            startY = topY - 10.dp.toPx(),
            endY = slotLipY + 12.dp.toPx(),
        ),
    )

    // Deep recessed oval-cornered N64 cartridge bay with twin parting dust flaps
    val slotW = deckW * 0.56f
    val slotL = cx - slotW * 0.5f
    val slotH = 22.dp.toPx()
    drawRoundRect(Color(0xFF0A0B0F), Offset(slotL, slotLipY - slotH * 0.72f), Size(slotW, slotH), CornerRadius(8.dp.toPx()))
    val flapH = (5.5.dp.toPx() * (1f - frame.insertDepth * 0.6f)).coerceAtLeast(2.dp.toPx())
    drawRoundRect(Color(0xFF393D47), Offset(slotL + 4.dp.toPx(), slotLipY - slotH * 0.64f), Size(slotW - 8.dp.toPx(), flapH), CornerRadius(3.dp.toPx()))
}

internal fun DrawScope.drawN64ConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val slotLipY = h * 0.61f
    val bottomY = h * 0.92f
    val frontH = bottomY - slotLipY

    // Iconic N64 sculpted dual-arch charcoal body (twin curved outer legs + stepped center brow!)
    val bodyPath = Path().apply {
        moveTo(deckL, slotLipY + 10.dp.toPx())
        quadraticTo(deckL, slotLipY, deckL + 18.dp.toPx(), slotLipY)
        lineTo(deckR - 18.dp.toPx(), slotLipY)
        quadraticTo(deckR, slotLipY, deckR, slotLipY + 10.dp.toPx())
        lineTo(deckR, bottomY - 14.dp.toPx())
        quadraticTo(deckR, bottomY, deckR - 22.dp.toPx(), bottomY)
        lineTo(deckL + 22.dp.toPx(), bottomY)
        quadraticTo(deckL, bottomY, deckL, bottomY - 14.dp.toPx())
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            0f to Color(0xFF3A3E48),
            0.48f to Color(0xFF24272F),
            1f to Color(0xFF14161B),
            startY = slotLipY,
            endY = bottomY,
        ),
    )

    // Subtle specular curved shoulder highlights (signature N64 "dolphin" ridges)
    drawLine(Color.White.copy(alpha = 0.24f), Offset(deckL + 16.dp.toPx(), slotLipY + 1.5.dp.toPx()), Offset(deckR - 16.dp.toPx(), slotLipY + 1.5.dp.toPx()), 1.4.dp.toPx())

    // Center Expansion Pak lid hatch directly in front of the cartridge slot
    val expW = deckW * 0.30f
    drawRoundRect(
        color = Color(0xFF20232B),
        topLeft = Offset(cx - expW * 0.5f, slotLipY + 8.dp.toPx()),
        size = Size(expW, 18.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = Offset(cx - expW * 0.5f, slotLipY + 8.dp.toPx()),
        size = Size(expW, 18.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(1.dp.toPx()),
    )

    // Left sliding POWER switch + Right circular RESET button
    val powerX = deckL + 26.dp.toPx()
    val btnY = slotLipY + 10.dp.toPx()
    drawRoundRect(Color(0xFF15171D), Offset(powerX, btnY), Size(22.dp.toPx(), 24.dp.toPx()), CornerRadius(6.dp.toPx()))
    val slideY = btnY + 3.dp.toPx() + (1f - frame.ledGlow) * 7.dp.toPx()
    drawRoundRect(Color(0xFF545864), Offset(powerX + 3.dp.toPx(), slideY), Size(16.dp.toPx(), 11.dp.toPx()), CornerRadius(4.dp.toPx()))

    val resetCenter = Offset(deckR - 36.dp.toPx(), btnY + 12.dp.toPx())
    drawCircle(Color(0xFF15171D), 12.dp.toPx(), resetCenter)
    drawCircle(Color(0xFF4A4E5B), 9.dp.toPx(), resetCenter)

    // Iconic glowing RED POWER JEWEL LED dead-center on the N64 nose!
    drawGlowingLed(
        center = Offset(cx, slotLipY + frontH * 0.42f),
        color = Color(0xFFFF2436),
        glow = frame.ledGlow,
        radiusPx = 4.2.dp.toPx(),
        haloPx = 22.dp.toPx(),
    )

    // Iconic 4 curved N64 Controller Ports lined across the lower front face!
    val portW = 46.dp.toPx()
    val portH = 22.dp.toPx()
    val portGap = 11.dp.toPx()
    val totalPortsW = portW * 4f + portGap * 3f
    val startX = cx - totalPortsW * 0.5f
    val portY = bottomY - portH - 12.dp.toPx()
    for (i in 0..3) {
        val px = startX + i * (portW + portGap)
        // Curved-top D-shape N64 port housing
        drawRoundRect(Color(0xFF12141A), Offset(px, portY), Size(portW, portH), CornerRadius(9.dp.toPx()))
        drawRoundRect(Color(0xFF2E323D), Offset(px, portY), Size(portW, portH), CornerRadius(9.dp.toPx()), style = Stroke(1.1.dp.toPx()))
        // 3 horizontal controller pin slots
        for (pin in 0..2) {
            drawCircle(Color(0xFF7B8090), 1.6.dp.toPx(), Offset(px + 13.dp.toPx() + pin * 10.dp.toPx(), portY + portH * 0.52f))
        }
    }
}

// =================================================================================================
// 4. SEGA GENESIS (Model 1 MK-1601) — Circular "16-BIT" Platter, Volume Slider, Power & Reset
// =================================================================================================

internal fun DrawScope.drawGenesisConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.46f
    val slotLipY = h * 0.61f
    val bottomY = h * 0.92f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), deckW * 1.05f)

    // Sleek matte jet-black Genesis top deck with right-side rear cooling louvers
    val topDeck = Path().apply {
        moveTo(deckL + 18.dp.toPx(), topY)
        lineTo(deckR - 18.dp.toPx(), topY)
        lineTo(deckR, slotLipY + 12.dp.toPx())
        lineTo(deckL, slotLipY + 12.dp.toPx())
        close()
    }
    drawPath(
        path = topDeck,
        brush = Brush.verticalGradient(0f to Color(0xFF2F323B), 1f to Color(0xFF1B1D23), startY = topY, endY = slotLipY + 12.dp.toPx()),
    )
    // Ribbed louvers on the right-rear corner
    for (i in 0..4) {
        val y = topY + 6.dp.toPx() + i * 5.dp.toPx()
        drawLine(Color(0xFF101216), Offset(deckR - 58.dp.toPx(), y), Offset(deckR - 20.dp.toPx(), y), 1.8.dp.toPx())
    }

    // Recessed beveled Genesis cartridge bay with parting dark dust flaps
    val slotW = deckW * 0.54f
    val slotL = cx - slotW * 0.5f
    val slotH = 22.dp.toPx()
    drawRoundRect(Color(0xFF07080B), Offset(slotL, slotLipY - slotH * 0.72f), Size(slotW, slotH), CornerRadius(5.dp.toPx()))
    val flapH = (5.dp.toPx() * (1f - frame.insertDepth * 0.65f)).coerceAtLeast(2.dp.toPx())
    drawRoundRect(Color(0xFF282B33), Offset(slotL + 4.dp.toPx(), slotLipY - slotH * 0.65f), Size(slotW - 8.dp.toPx(), flapH), CornerRadius(2.dp.toPx()))
}

internal fun DrawScope.drawGenesisConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(328.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val slotLipY = h * 0.61f
    val bottomY = h * 0.92f
    val frontH = bottomY - slotLipY

    // Main jet-black Model 1 fascia
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF282B33), 0.55f to Color(0xFF181A20), 1f to Color(0xFF0E1014), startY = slotLipY, endY = bottomY),
        topLeft = Offset(deckL, slotLipY),
        size = Size(deckW, frontH),
        cornerRadius = CornerRadius(14.dp.toPx()),
    )
    drawLine(Color.White.copy(alpha = 0.24f), Offset(deckL + 12.dp.toPx(), slotLipY + 1.2.dp.toPx()), Offset(deckL + deckW - 12.dp.toPx(), slotLipY + 1.2.dp.toPx()), 1.4.dp.toPx())

    // Iconic Genesis Model 1 circular sculpted platter ring + gold "16-BIT" accent arc!
    val ringCx = cx + deckW * 0.14f
    val ringCy = slotLipY + frontH * 0.44f
    drawOval(
        color = Color(0xFF121419),
        topLeft = Offset(ringCx - 58.dp.toPx(), ringCy - 24.dp.toPx()),
        size = Size(116.dp.toPx(), 48.dp.toPx()),
    )
    drawOval(
        color = Color(0xFF6B7080).copy(alpha = 0.55f),
        topLeft = Offset(ringCx - 58.dp.toPx(), ringCy - 24.dp.toPx()),
        size = Size(116.dp.toPx(), 48.dp.toPx()),
        style = Stroke(1.6.dp.toPx()),
    )
    // Gold 16-BIT emblem highlight strip inside the circular platter
    drawRoundRect(
        color = Color(0xFFD9A74A),
        topLeft = Offset(ringCx - 20.dp.toPx(), ringCy - 4.dp.toPx()),
        size = Size(40.dp.toPx(), 8.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )

    // Left-side iconic horizontal VOLUME slider + headphone jack
    val volL = deckL + 22.dp.toPx()
    val volY = slotLipY + frontH * 0.30f
    drawRoundRect(Color(0xFF0C0E12), Offset(volL, volY), Size(54.dp.toPx(), 8.dp.toPx()), CornerRadius(4.dp.toPx()))
    drawRoundRect(Color(0xFF5A5F6E), Offset(volL + 28.dp.toPx(), volY - 2.dp.toPx()), Size(12.dp.toPx(), 12.dp.toPx()), CornerRadius(3.dp.toPx()))
    // Headphone jack circle below volume slider
    drawCircle(Color(0xFF08090C), 4.5.dp.toPx(), Offset(volL + 8.dp.toPx(), volY + 24.dp.toPx()))
    drawCircle(Color(0xFF5A5F6E), 4.5.dp.toPx(), Offset(volL + 8.dp.toPx(), volY + 24.dp.toPx()), style = Stroke(1.dp.toPx()))

    // POWER slider + iconic white RESET button
    drawRoundRect(Color(0xFF3E424E), Offset(volL + 22.dp.toPx(), volY + 18.dp.toPx()), Size(26.dp.toPx(), 12.dp.toPx()), CornerRadius(4.dp.toPx()))
    drawRoundRect(Color(0xFFDADCE2), Offset(volL + 56.dp.toPx(), volY + 19.dp.toPx()), Size(18.dp.toPx(), 10.dp.toPx()), CornerRadius(4.dp.toPx()))

    // Bright red Genesis POWER indicator LED
    drawGlowingLed(
        center = Offset(ringCx - 66.dp.toPx(), slotLipY + 16.dp.toPx()),
        color = Color(0xFFFF2424),
        glow = frame.ledGlow,
        radiusPx = 4.dp.toPx(),
        haloPx = 20.dp.toPx(),
    )

    // Twin recessed DE-9 controller ports on lower-right edge
    for (p in 0..1) {
        val px = deckL + deckW - 104.dp.toPx() + p * 46.dp.toPx()
        val py = bottomY - 24.dp.toPx()
        drawRoundRect(Color(0xFF0A0B0E), Offset(px, py), Size(38.dp.toPx(), 15.dp.toPx()), CornerRadius(4.dp.toPx()))
        drawRoundRect(Color(0xFF3D414D), Offset(px, py), Size(38.dp.toPx(), 15.dp.toPx()), CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
    }
}

// =================================================================================================
// 5. NINTENDO DS LITE & NINTENDO 3DS — Real Top-Rear Clamshell Spine with Slot-1, L/R & Hinge!
// =================================================================================================

internal fun DrawScope.drawNdsOr3dsConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val is3ds = hwSpec.systemId == "3ds"
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.88f).coerceAtMost(324.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val shoulderTop = h * 0.455f
    val spineY = h * 0.535f
    val bottomY = h * 0.93f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), bodyW * 1.02f)

    // Real sculpted L and R corner shoulder triggers wrapping the upper-left and upper-right!
    val triggerColor = if (is3ds) Color(0xFF1D466E) else Color(0xFFD6DBE2)
    val triggerEdge = if (is3ds) Color(0xFF4281B8) else Color.White.copy(alpha = 0.65f)
    val triggerW = bodyW * 0.24f
    val triggerH = 26.dp.toPx()
    // L trigger (left rear shoulder)
    drawRoundRect(triggerColor, Offset(bodyL + 6.dp.toPx(), shoulderTop), Size(triggerW, triggerH), CornerRadius(14.dp.toPx()))
    drawRoundRect(triggerEdge, Offset(bodyL + 6.dp.toPx(), shoulderTop), Size(triggerW, triggerH), CornerRadius(14.dp.toPx()), style = Stroke(1.2.dp.toPx()))
    // R trigger (right rear shoulder)
    drawRoundRect(triggerColor, Offset(bodyR - 6.dp.toPx() - triggerW, shoulderTop), Size(triggerW, triggerH), CornerRadius(14.dp.toPx()))
    drawRoundRect(triggerEdge, Offset(bodyR - 6.dp.toPx() - triggerW, shoulderTop), Size(triggerW, triggerH), CornerRadius(14.dp.toPx()), style = Stroke(1.2.dp.toPx()))

    // Iconic cylindrical Nintendo DS Lite / 3DS clamshell dual-hinge spine behind the slot
    val hingeTop = shoulderTop + 8.dp.toPx()
    val hingeH = 28.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to hwSpec.bodyTop,
            0.45f to hwSpec.bodyMid,
            1f to hwSpec.bodyDark,
            startY = hingeTop,
            endY = hingeTop + hingeH,
        ),
        topLeft = Offset(bodyL + 16.dp.toPx(), hingeTop),
        size = Size(bodyW - 32.dp.toPx(), hingeH),
        cornerRadius = CornerRadius(12.dp.toPx()),
    )
    // Iconic DS Lite cylindrical barrel hinge segment seams separating left/center/right barrels
    val hingeSeamOffset = bodyW * 0.28f
    drawLine(Color.Black.copy(alpha = 0.24f), Offset(cx - hingeSeamOffset, hingeTop + 2.dp.toPx()), Offset(cx - hingeSeamOffset, hingeTop + hingeH - 2.dp.toPx()), 1.4.dp.toPx())
    drawLine(Color.Black.copy(alpha = 0.24f), Offset(cx + hingeSeamOffset, hingeTop + 2.dp.toPx()), Offset(cx + hingeSeamOffset, hingeTop + hingeH - 2.dp.toPx()), 1.4.dp.toPx())

    // Deep dark recessed Slot-1 (DS / 3DS Game Card bay) dead-center along the top-rear edge!
    val slotW = bodyW * 0.34f
    val slotL = cx - slotW * 0.5f
    val slotH = 18.dp.toPx()
    drawRoundRect(
        color = Color(0xFF0A0C10),
        topLeft = Offset(slotL, spineY - slotH * 0.72f),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    // Metallic spring contacts glinting deep inside Slot-1
    for (p in 0..7) {
        val px = slotL + 10.dp.toPx() + p * ((slotW - 20.dp.toPx()) / 7f)
        drawLine(Color(0xFFC9A64C).copy(alpha = 0.55f), Offset(px, spineY - 7.dp.toPx()), Offset(px, spineY - 2.dp.toPx()), 1.4.dp.toPx())
    }
}

internal fun DrawScope.drawNdsOr3dsConsoleFront(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val is3ds = hwSpec.systemId == "3ds"
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.88f).coerceAtMost(324.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val spineY = h * 0.535f
    val bottomY = h * 0.93f
    val shellH = bottomY - spineY

    // Main glossy DS Lite / layered metallic 3DS lower clamshell rear body
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to hwSpec.bodyTop,
            0.55f to hwSpec.bodyMid,
            1f to hwSpec.bodyDark,
            startY = spineY,
            endY = bottomY,
        ),
        topLeft = Offset(bodyL, spineY),
        size = Size(bodyW, shellH),
        cornerRadius = CornerRadius(24.dp.toPx()),
    )

    // Clamshell seam line dividing top lid & lower housing (signature DS Lite / 3DS silhouette!)
    drawLine(
        color = Color.Black.copy(alpha = 0.28f),
        start = Offset(bodyL + 8.dp.toPx(), spineY + 18.dp.toPx()),
        end = Offset(bodyR - 8.dp.toPx(), spineY + 18.dp.toPx()),
        strokeWidth = 1.6.dp.toPx(),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(bodyL + 16.dp.toPx(), spineY + 1.5.dp.toPx()),
        end = Offset(bodyR - 16.dp.toPx(), spineY + 1.5.dp.toPx()),
        strokeWidth = 1.5.dp.toPx(),
    )

    // Recessed Slot-1 notch cutout on the rear edge so you can SEE the Game Card click flush!
    val slotW = bodyW * 0.34f
    val slotL = cx - slotW * 0.5f
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = Offset(slotL - 3.dp.toPx(), spineY - 1.dp.toPx()),
        size = Size(slotW + 6.dp.toPx(), 7.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )

    // Metallic trapezoid AC charging port + dark IR window (3DS) beside Slot-1
    val portX = slotL + slotW + 14.dp.toPx()
    drawRoundRect(Color(0xFF1F232B), Offset(portX, spineY + 4.dp.toPx()), Size(22.dp.toPx(), 9.dp.toPx()), CornerRadius(2.5.dp.toPx()))
    drawRoundRect(Color(0xFFB5BCC8), Offset(portX + 2.dp.toPx(), spineY + 5.5.dp.toPx()), Size(18.dp.toPx(), 6.dp.toPx()), CornerRadius(1.5.dp.toPx()), style = Stroke(1.dp.toPx()))
    if (is3ds) {
        // Dark ruby IR transceiver window on the left of Slot-1
        drawRoundRect(Color(0xFF1C0C12), Offset(slotL - 36.dp.toPx(), spineY + 4.dp.toPx()), Size(22.dp.toPx(), 9.dp.toPx()), CornerRadius(3.dp.toPx()))
    } else {
        // DS Lite wrist-strap bridge on the left of Slot-1
        drawRoundRect(Color(0xFF8C92A0), Offset(slotL - 34.dp.toPx(), spineY + 5.dp.toPx()), Size(18.dp.toPx(), 7.dp.toPx()), CornerRadius(3.dp.toPx()))
    }

    // Stylus head tucked into the right-side DS/3DS silo
    drawRoundRect(
        color = if (is3ds) Color(0xFF9EA8B8) else Color(0xFFF6F7FA),
        topLeft = Offset(bodyR - 22.dp.toPx(), spineY + 26.dp.toPx()),
        size = Size(14.dp.toPx(), 24.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx()),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(bodyR - 22.dp.toPx(), spineY + 26.dp.toPx()),
        size = Size(14.dp.toPx(), 24.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx()),
        style = Stroke(1.dp.toPx()),
    )

    // Iconic rear DS/3DS battery compartment panel + screw + twin-screen DS emblem subtly debossed
    val panelW = bodyW * 0.56f
    val panelH = shellH * 0.52f
    val panelTop = spineY + 34.dp.toPx()
    drawRoundRect(
        color = Color.Black.copy(alpha = if (is3ds) 0.16f else 0.08f),
        topLeft = Offset(cx - panelW * 0.5f, panelTop),
        size = Size(panelW, panelH),
        cornerRadius = CornerRadius(12.dp.toPx()),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(cx - panelW * 0.5f, panelTop),
        size = Size(panelW, panelH),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(1.dp.toPx()),
    )
    // Tiny twin-square DS / 3DS rear embossed emblem
    val embCx = cx
    val embCy = panelTop + panelH * 0.45f
    drawRoundRect(Color.Black.copy(alpha = 0.16f), Offset(embCx - 16.dp.toPx(), embCy - 11.dp.toPx()), Size(16.dp.toPx(), 12.dp.toPx()), CornerRadius(2.dp.toPx()), style = Stroke(1.8.dp.toPx()))
    drawRoundRect(Color.Black.copy(alpha = 0.16f), Offset(embCx, embCy - 1.dp.toPx()), Size(16.dp.toPx(), 12.dp.toPx()), CornerRadius(2.dp.toPx()), style = Stroke(1.8.dp.toPx()))

    // Iconic glowing Power LED perched on the right hinge edge!
    drawGlowingLed(
        center = Offset(bodyR - 26.dp.toPx(), spineY + 9.dp.toPx()),
        color = hwSpec.ledColor,
        glow = frame.ledGlow,
        radiusPx = 3.6.dp.toPx(),
        haloPx = 20.dp.toPx(),
    )
}

// =================================================================================================
// 6. GAME BOY (DMG-01) & GAME BOY COLOR — Real Rear Shell with Cartridge Channel & ON/OFF Lock
// =================================================================================================

internal fun DrawScope.drawGameBoyConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val isGbc = hwSpec.systemId == "gbc"
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.64f).coerceAtMost(228.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val topY = h * 0.44f
    val bottomY = h * 0.94f

    drawConsoleShadow(cx, bottomY + 5.dp.toPx(), bodyW * 1.08f)

    // Rear Game Boy / GBC vertical silhouette
    drawRoundRect(
        brush = Brush.verticalGradient(0f to hwSpec.bodyTop, 0.55f to hwSpec.bodyMid, 1f to hwSpec.bodyDark, startY = topY, endY = bottomY),
        topLeft = Offset(bodyL, topY),
        size = Size(bodyW, bottomY - topY),
        cornerRadius = CornerRadius(22.dp.toPx()),
    )

    // GBC dark ruby IR transceiver port perched on the top-left edge
    if (isGbc) {
        drawRoundRect(Color(0xFF280814), Offset(bodyL + 22.dp.toPx(), topY - 2.dp.toPx()), Size(22.dp.toPx(), 8.dp.toPx()), CornerRadius(4.dp.toPx()))
    }

    // Recessed vertical rear Game Pak pocket channel (authentic Game Boy rear bay!)
    val bayW = bodyW * 0.68f
    val bayL = cx - bayW * 0.5f
    val bayH = (bottomY - topY) * 0.52f
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF1A1B20), 1f to Color(0xFF2B2D35), startY = topY, endY = topY + bayH),
        topLeft = Offset(bayL, topY),
        size = Size(bayW, bayH),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    // Internal rear pin slot connector bar at bottom of the bay
    drawRoundRect(Color(0xFF0E1014), Offset(bayL + 8.dp.toPx(), topY + bayH - 12.dp.toPx()), Size(bayW - 16.dp.toPx(), 8.dp.toPx()), CornerRadius(2.dp.toPx()))
}

internal fun DrawScope.drawGameBoyConsoleFront(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val isGbc = hwSpec.systemId == "gbc"
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.64f).coerceAtMost(228.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val topY = h * 0.44f
    val bottomY = h * 0.94f
    val bayBottomY = topY + (bottomY - topY) * 0.44f

    // Lower rear body & battery cover overlapping the lower connector edge of the cartridge!
    drawRoundRect(
        brush = Brush.verticalGradient(0f to hwSpec.bodyMid, 1f to hwSpec.bodyDark, startY = bayBottomY, endY = bottomY),
        topLeft = Offset(bodyL, bayBottomY),
        size = Size(bodyW, bottomY - bayBottomY),
        cornerRadius = CornerRadius(18.dp.toPx()),
    )
    // Iconic horizontal grip grooves flanking the DMG rear shell
    for (g in 0..4) {
        val gy = topY + 18.dp.toPx() + g * 8.dp.toPx()
        drawLine(Color.Black.copy(alpha = 0.22f), Offset(bodyL + 8.dp.toPx(), gy), Offset(bodyL + 22.dp.toPx(), gy), 2.dp.toPx(), StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = 0.22f), Offset(bodyR - 22.dp.toPx(), gy), Offset(bodyR - 8.dp.toPx(), gy), 2.dp.toPx(), StrokeCap.Round)
    }

    // Rear AA battery compartment cover + ribbed thumb latch
    val battW = bodyW * 0.58f
    val battH = (bottomY - bayBottomY) * 0.68f
    val battTop = bayBottomY + 14.dp.toPx()
    drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(cx - battW * 0.5f, battTop), Size(battW, battH), CornerRadius(10.dp.toPx()))
    drawRoundRect(Color.Black.copy(alpha = 0.24f), Offset(cx - battW * 0.5f, battTop), Size(battW, battH), CornerRadius(10.dp.toPx()), style = Stroke(1.2.dp.toPx()))
    // Thumb clip ridges
    for (r in 0..2) {
        val ry = battTop + 7.dp.toPx() + r * 4.dp.toPx()
        drawLine(Color.Black.copy(alpha = 0.28f), Offset(cx - 12.dp.toPx(), ry), Offset(cx + 12.dp.toPx(), ry), 1.4.dp.toPx())
    }

    // Iconic DMG-01 top-left sliding ON/OFF power tab that physically locks the cartridge corner notch!
    if (!isGbc) {
        val lockSlide = frame.ledGlow * 14.dp.toPx()
        drawRoundRect(
            color = Color(0xFF9F9B8E),
            topLeft = Offset(bodyL + 16.dp.toPx() + lockSlide, topY - 3.dp.toPx()),
            size = Size(26.dp.toPx(), 9.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
    }

    // Subtle red/green power glow peeking along the right edge
    drawGlowingLed(
        center = Offset(bodyR - 14.dp.toPx(), bayBottomY + 10.dp.toPx()),
        color = hwSpec.ledColor,
        glow = frame.ledGlow,
        radiusPx = 3.2.dp.toPx(),
        haloPx = 16.dp.toPx(),
    )
}

// =================================================================================================
// 7. GAME BOY ADVANCE (Indigo AGB-001) — Wide Sweeping Rear Wings, Grey L/R Triggers & Top Slot
// =================================================================================================

internal fun DrawScope.drawGbaConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.92f).coerceAtMost(336.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val topY = h * 0.49f
    val bottomY = h * 0.92f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), bodyW * 1.02f)

    // Iconic light-grey contoured L and R shoulder triggers sweeping the top-left & top-right corners!
    val lPath = Path().apply {
        moveTo(bodyL + 16.dp.toPx(), topY + 26.dp.toPx())
        quadraticTo(bodyL + 14.dp.toPx(), topY - 4.dp.toPx(), bodyL + 78.dp.toPx(), topY - 2.dp.toPx())
        lineTo(bodyL + 84.dp.toPx(), topY + 24.dp.toPx())
        close()
    }
    drawPath(lPath, brush = Brush.verticalGradient(0f to Color(0xFFDADEE8), 1f to Color(0xFFA9B0BF), startY = topY, endY = topY + 26.dp.toPx()))
    val rPath = Path().apply {
        moveTo(bodyR - 16.dp.toPx(), topY + 26.dp.toPx())
        quadraticTo(bodyR - 14.dp.toPx(), topY - 4.dp.toPx(), bodyR - 78.dp.toPx(), topY - 2.dp.toPx())
        lineTo(bodyR - 84.dp.toPx(), topY + 24.dp.toPx())
        close()
    }
    drawPath(rPath, brush = Brush.verticalGradient(0f to Color(0xFFDADEE8), 1f to Color(0xFFA9B0BF), startY = topY, endY = topY + 26.dp.toPx()))

    // Recessed center Game Pak slot mouth between L and R triggers
    val slotW = bodyW * 0.42f
    val slotL = cx - slotW * 0.5f
    drawRoundRect(Color(0xFF0C0E14), Offset(slotL, topY + 2.dp.toPx()), Size(slotW, 20.dp.toPx()), CornerRadius(5.dp.toPx()))
}

internal fun DrawScope.drawGbaConsoleFront(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.92f).coerceAtMost(336.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val topY = h * 0.54f
    val bottomY = h * 0.92f
    val bodyH = bottomY - topY

    // Sweeping ergonomic wide GBA Indigo rear body!
    val gbaPath = Path().apply {
        moveTo(bodyL + 36.dp.toPx(), topY)
        lineTo(bodyR - 36.dp.toPx(), topY)
        cubicTo(bodyR + 4.dp.toPx(), topY + bodyH * 0.12f, bodyR + 4.dp.toPx(), bottomY - bodyH * 0.14f, bodyR - 42.dp.toPx(), bottomY)
        lineTo(bodyL + 42.dp.toPx(), bottomY)
        cubicTo(bodyL - 4.dp.toPx(), bottomY - bodyH * 0.14f, bodyL - 4.dp.toPx(), topY + bodyH * 0.12f, bodyL + 36.dp.toPx(), topY)
        close()
    }
    drawPath(
        path = gbaPath,
        brush = Brush.verticalGradient(0f to hwSpec.bodyTop, 0.55f to hwSpec.bodyMid, 1f to hwSpec.bodyDark, startY = topY, endY = bottomY),
    )
    drawPath(gbaPath, color = Color.White.copy(alpha = 0.22f), style = Stroke(1.2.dp.toPx()))

    // Light-grey side grip inlays (iconic AGB-001 curved accents!)
    drawRoundRect(Color(0xFFCFD4E0).copy(alpha = 0.72f), Offset(bodyL + 6.dp.toPx(), topY + bodyH * 0.24f), Size(10.dp.toPx(), bodyH * 0.52f), CornerRadius(5.dp.toPx()))
    drawRoundRect(Color(0xFFCFD4E0).copy(alpha = 0.72f), Offset(bodyR - 16.dp.toPx(), topY + bodyH * 0.24f), Size(10.dp.toPx(), bodyH * 0.52f), CornerRadius(5.dp.toPx()))

    // Central recessed 2xAA battery cover + thumb latch
    val battW = bodyW * 0.36f
    val battH = bodyH * 0.56f
    val battTop = topY + bodyH * 0.26f
    drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(cx - battW * 0.5f, battTop), Size(battW, battH), CornerRadius(10.dp.toPx()))
    drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(cx - battW * 0.5f, battTop), Size(battW, battH), CornerRadius(10.dp.toPx()), style = Stroke(1.dp.toPx()))

    // EXT Link Cable port centered right above the battery door
    drawRoundRect(Color(0xFF141824), Offset(cx - 14.dp.toPx(), topY + 5.dp.toPx()), Size(28.dp.toPx(), 10.dp.toPx()), CornerRadius(3.dp.toPx()))

    // Green Power LED glow at upper-right
    drawGlowingLed(
        center = Offset(bodyR - 36.dp.toPx(), topY + 14.dp.toPx()),
        color = hwSpec.ledColor,
        glow = frame.ledGlow,
        radiusPx = 3.5.dp.toPx(),
        haloPx = 18.dp.toPx(),
    )
}

// =================================================================================================
// 8. NINTENDO SWITCH — Neon Blue & Red Joy-Con Shoulders + Top-Right Hinged Game Card Flap!
// =================================================================================================

internal fun DrawScope.drawSwitchConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val fullW = (w * 0.94f).coerceAtMost(344.dp.toPx())
    val left = cx - fullW * 0.5f
    val right = left + fullW
    val topY = h * 0.52f
    val bottomY = h * 0.92f
    val jcW = fullW * 0.19f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), fullW)

    // Left Neon Blue Joy-Con shoulder + ZL/L rail
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF15C8F5), 1f to Color(0xFF008AB8), startY = topY, endY = bottomY),
        topLeft = Offset(left, topY),
        size = Size(jcW + 10.dp.toPx(), bottomY - topY),
        cornerRadius = CornerRadius(24.dp.toPx()),
    )
    // Right Neon Red Joy-Con shoulder + ZR/R rail
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFFFF4E44), 1f to Color(0xFFC41E18), startY = topY, endY = bottomY),
        topLeft = Offset(right - jcW - 10.dp.toPx(), topY),
        size = Size(jcW + 10.dp.toPx(), bottomY - topY),
        cornerRadius = CornerRadius(24.dp.toPx()),
    )

    // Center matte-black Switch tablet upper rail
    val tabL = left + jcW
    val tabW = fullW - jcW * 2f
    drawRoundRect(Color(0xFF282B32), Offset(tabL, topY), Size(tabW, 24.dp.toPx()), CornerRadius(6.dp.toPx()))

    // Top-right recessed Switch Game Card slot (aligned with the right side of the tablet!)
    val slotCx = tabL + tabW * 0.78f
    val slotW = 36.dp.toPx()
    drawRoundRect(Color(0xFF07080B), Offset(slotCx - slotW * 0.5f, topY + 2.dp.toPx()), Size(slotW, 14.dp.toPx()), CornerRadius(3.dp.toPx()))
}

internal fun DrawScope.drawSwitchConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val fullW = (w * 0.94f).coerceAtMost(344.dp.toPx())
    val left = cx - fullW * 0.5f
    val jcW = fullW * 0.19f
    val tabL = left + jcW
    val tabW = fullW - jcW * 2f
    val lipY = h * 0.555f
    val bottomY = h * 0.92f

    // Matte-black Switch tablet body
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF2A2D35), 1f to Color(0xFF14161A), startY = lipY, endY = bottomY),
        topLeft = Offset(tabL, lipY),
        size = Size(tabW, bottomY - lipY),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    // Glossy glass screen bezel glow when powered on
    val screenInset = 10.dp.toPx()
    drawRoundRect(
        color = Color(0xFF0A0C10),
        topLeft = Offset(tabL + screenInset, lipY + 18.dp.toPx()),
        size = Size(tabW - screenInset * 2f, (bottomY - lipY) - 28.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    if (frame.ledGlow > 0.05f) {
        drawRoundRect(
            brush = Brush.radialGradient(
                0f to Color(0xFFE60012).copy(alpha = 0.55f * frame.ledGlow),
                1f to Color.Transparent,
                center = Offset(cx, lipY + (bottomY - lipY) * 0.54f),
                radius = tabW * 0.55f,
            ),
            topLeft = Offset(tabL + screenInset, lipY + 18.dp.toPx()),
            size = Size(tabW - screenInset * 2f, (bottomY - lipY) - 28.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
    }

    // Top-left Power & Volume +/- buttons + center cooling exhaust grille + headphone jack
    drawRoundRect(Color(0xFF424652), Offset(tabL + 10.dp.toPx(), lipY - 5.dp.toPx()), Size(8.dp.toPx(), 4.dp.toPx()), CornerRadius(2.dp.toPx()))
    drawRoundRect(Color(0xFF424652), Offset(tabL + 24.dp.toPx(), lipY - 5.dp.toPx()), Size(18.dp.toPx(), 4.dp.toPx()), CornerRadius(2.dp.toPx()))
    for (v in 0..4) {
        drawRoundRect(Color(0xFF0E1014), Offset(cx - 24.dp.toPx() + v * 10.dp.toPx(), lipY - 4.dp.toPx()), Size(6.dp.toPx(), 5.dp.toPx()), CornerRadius(1.5.dp.toPx()))
    }
    drawCircle(Color(0xFF0A0B0E), 3.5.dp.toPx(), Offset(tabL + tabW * 0.62f, lipY - 2.dp.toPx()))

    // Authentic Joy-Con controls on Left Neon Blue & Right Neon Red Joy-Cons!
    val leftJcCx = left + jcW * 0.5f
    val rightJcCx = left + fullW - jcW * 0.5f
    val stickR = 11.dp.toPx()
    val btnR = 4.2.dp.toPx()
    // Left Blue Joy-Con: (-) button, top Analog Stick, lower directional buttons
    drawLine(Color(0xFF1A1D24), Offset(leftJcCx + 8.dp.toPx(), lipY + 12.dp.toPx()), Offset(leftJcCx + 16.dp.toPx(), lipY + 12.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    drawCircle(Color(0xFF181B22), stickR, Offset(leftJcCx, lipY + 38.dp.toPx()))
    drawCircle(Color(0xFF2D313C), stickR * 0.72f, Offset(leftJcCx, lipY + 38.dp.toPx()))
    val dpadCy = lipY + 84.dp.toPx()
    drawCircle(Color(0xFF181B22), btnR, Offset(leftJcCx, dpadCy - 11.dp.toPx()))
    drawCircle(Color(0xFF181B22), btnR, Offset(leftJcCx, dpadCy + 11.dp.toPx()))
    drawCircle(Color(0xFF181B22), btnR, Offset(leftJcCx - 11.dp.toPx(), dpadCy))
    drawCircle(Color(0xFF181B22), btnR, Offset(leftJcCx + 11.dp.toPx(), dpadCy))

    // Right Red Joy-Con: (+) button, upper ABXY buttons, lower Analog Stick
    val plusCx = rightJcCx - 12.dp.toPx()
    val plusCy = lipY + 12.dp.toPx()
    drawLine(Color(0xFF1A1D24), Offset(plusCx - 4.dp.toPx(), plusCy), Offset(plusCx + 4.dp.toPx(), plusCy), 2.dp.toPx(), StrokeCap.Round)
    drawLine(Color(0xFF1A1D24), Offset(plusCx, plusCy - 4.dp.toPx()), Offset(plusCx, plusCy + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    val abxyCy = lipY + 38.dp.toPx()
    drawCircle(Color(0xFF181B22), btnR, Offset(rightJcCx, abxyCy - 11.dp.toPx()))
    drawCircle(Color(0xFF181B22), btnR, Offset(rightJcCx, abxyCy + 11.dp.toPx()))
    drawCircle(Color(0xFF181B22), btnR, Offset(rightJcCx - 11.dp.toPx(), abxyCy))
    drawCircle(Color(0xFF181B22), btnR, Offset(rightJcCx + 11.dp.toPx(), abxyCy))
    drawCircle(Color(0xFF181B22), stickR, Offset(rightJcCx, lipY + 84.dp.toPx()))
    drawCircle(Color(0xFF2D313C), stickR * 0.72f, Offset(rightJcCx, lipY + 84.dp.toPx()))

    // Hinged top-right Game Card flap — flips UPWARD cleanly above the top rail while inserting!
    val slotCx = tabL + tabW * 0.78f
    val slotW = 38.dp.toPx()
    val open = frame.lidOpen.coerceIn(0f, 1f)
    val flapLeft = slotCx - slotW * 0.5f
    rotate(degrees = open * 76f, pivot = Offset(flapLeft + slotW, lipY - 5.dp.toPx())) {
        drawRoundRect(
            color = Color(0xFF474C59),
            topLeft = Offset(flapLeft, lipY - 7.dp.toPx()),
            size = Size(slotW, 6.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

// =================================================================================================
// 9. TOP-LID OPTICAL DISC CONSOLES — PS1, GAMECUBE, DREAMCAST, SATURN (Real 3/4 Hardware!)
// =================================================================================================

internal fun DrawScope.drawTopLidConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    when (hwSpec.systemId) {
        "psx", "ps1" -> drawPs1ConsoleBack(frame)
        "gc", "gamecube" -> drawGameCubeConsoleBack(frame)
        "dc", "dreamcast" -> drawDreamcastConsoleBack(frame)
        else -> drawSaturnConsoleBack(hwSpec, frame)
    }
}

internal fun DrawScope.drawTopLidConsoleFront(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    when (hwSpec.systemId) {
        "psx", "ps1" -> drawPs1ConsoleFront(frame)
        "gc", "gamecube" -> drawGameCubeConsoleFront(frame)
        "dc", "dreamcast" -> drawDreamcastConsoleFront(frame)
        else -> drawSaturnConsoleFront(hwSpec, frame)
    }
}

/** Shared recessed optical bay + metallic 3-ball spindle hub + diagonal laser lens rail. */
private fun DrawScope.drawOpticalSpindleWell(cx: Float, bayCy: Float, rx: Float, ry: Float, lidOpen: Float, lidColor: Color) {
    // Open hinged circular lid anchored directly onto the rear rim of the disc bay (zero floating gap!)
    if (lidOpen > 0.06f) {
        val hingeY = bayCy - ry + 2.dp.toPx()
        val lidH = (ry * (0.52f + 1.05f * lidOpen)).coerceAtMost(rx * 1.15f)
        val lidTop = hingeY - lidH + 6.dp.toPx()
        // Twin physical rear hinge brackets anchoring the lid to the console deck
        drawRoundRect(Color(0xFF282C36), Offset(cx - 26.dp.toPx(), hingeY - 5.dp.toPx()), Size(12.dp.toPx(), 10.dp.toPx()), CornerRadius(3.dp.toPx()))
        drawRoundRect(Color(0xFF282C36), Offset(cx + 14.dp.toPx(), hingeY - 5.dp.toPx()), Size(12.dp.toPx(), 10.dp.toPx()), CornerRadius(3.dp.toPx()))
        // Lid outer shell (tilted upright showing underside & disc stabilizer puck!)
        drawOval(
            brush = Brush.verticalGradient(
                0f to lidColor.copy(alpha = 0.94f),
                0.78f to Color(0xFF2A2E38),
                1f to Color(0xFF181A22),
                startY = lidTop,
                endY = lidTop + lidH,
            ),
            topLeft = Offset(cx - rx * 0.96f, lidTop),
            size = Size(rx * 1.92f, lidH),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(cx - rx * 0.96f, lidTop),
            size = Size(rx * 1.92f, lidH),
            style = Stroke(1.6.dp.toPx()),
        )
        // Underside circular disc stabilizer puck
        drawOval(
            color = Color(0xFF3E4452),
            topLeft = Offset(cx - 18.dp.toPx(), lidTop + lidH * 0.44f - 8.dp.toPx()),
            size = Size(36.dp.toPx(), 16.dp.toPx()),
        )
        drawOval(
            color = Color(0xFF181B22),
            topLeft = Offset(cx - 8.dp.toPx(), lidTop + lidH * 0.44f - 3.5.dp.toPx()),
            size = Size(16.dp.toPx(), 7.dp.toPx()),
        )
    }

    // Recessed circular disc tray
    drawOval(
        brush = Brush.radialGradient(
            0f to Color(0xFF232630),
            0.78f to Color(0xFF14161D),
            1f to Color(0xFF0A0B0F),
            center = Offset(cx, bayCy),
            radius = rx,
        ),
        topLeft = Offset(cx - rx, bayCy - ry),
        size = Size(rx * 2f, ry * 2f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(cx - rx, bayCy - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(1.5.dp.toPx()),
    )

    // Diagonal laser optical pickup rail + bluish glass lens glint
    drawLine(
        color = Color(0xFF3B404E),
        start = Offset(cx + 12.dp.toPx(), bayCy + 6.dp.toPx()),
        end = Offset(cx + rx * 0.62f, bayCy + ry * 0.56f),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFF4BA3FF).copy(alpha = 0.72f), 3.2.dp.toPx(), Offset(cx + rx * 0.34f, bayCy + ry * 0.28f))

    // Center 3-ball spring spindle hub
    drawOval(Color(0xFF383C46), Offset(cx - 16.dp.toPx(), bayCy - 8.5.dp.toPx()), Size(32.dp.toPx(), 17.dp.toPx()))
    drawOval(Color(0xFFB5BCC9), Offset(cx - 7.dp.toPx(), bayCy - 4.dp.toPx()), Size(14.dp.toPx(), 8.dp.toPx()))
    // 3 tiny chrome ball-bearings on spindle
    drawCircle(Color.White, 1.5.dp.toPx(), Offset(cx - 6.dp.toPx(), bayCy))
    drawCircle(Color.White, 1.5.dp.toPx(), Offset(cx + 6.dp.toPx(), bayCy))
    drawCircle(Color.White, 1.5.dp.toPx(), Offset(cx, bayCy - 3.dp.toPx()))
}

/** Shared animated closing circular hinged lid sweeping down from rear hinge over the spinning disc. */
private fun DrawScope.drawClosingCircularLid(
    cx: Float,
    bayCy: Float,
    rx: Float,
    ry: Float,
    lidOpen: Float,
    topColor: Color,
    midColor: Color,
    emblemDraw: DrawScope.(center: Offset, lidH: Float, closed: Float) -> Unit,
) {
    if (lidOpen >= 0.96f) return
    val closed = (1f - lidOpen).coerceIn(0f, 1f)
    val lidTop = bayCy - ry - (1f - closed) * 58.dp.toPx()
    val lidH = ry * 2f * (0.28f + 0.72f * closed)
    drawOval(
        brush = Brush.verticalGradient(
            0f to topColor.copy(alpha = (0.35f + closed * 0.65f).coerceIn(0f, 1f)),
            1f to midColor.copy(alpha = (0.35f + closed * 0.65f).coerceIn(0f, 1f)),
            startY = lidTop,
            endY = lidTop + lidH,
        ),
        topLeft = Offset(cx - rx, lidTop),
        size = Size(rx * 2f, lidH),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.34f * closed),
        topLeft = Offset(cx - rx, lidTop),
        size = Size(rx * 2f, lidH),
        style = Stroke(1.6.dp.toPx()),
    )
    if (closed > 0.38f) {
        emblemDraw(Offset(cx, lidTop + lidH * 0.5f), lidH, closed)
    }
}

// --- PlayStation 1 (SCPH-1001) ---
private fun DrawScope.drawPs1ConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.90f).coerceAtMost(332.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.43f
    val frontEdgeY = h * 0.78f
    val bottomY = h * 0.93f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), deckW * 1.05f)

    // Stepped cool-grey PS1 top deck in crisp 3/4 trapezoid perspective
    val topDeck = Path().apply {
        moveTo(deckL + 22.dp.toPx(), topY)
        lineTo(deckR - 22.dp.toPx(), topY)
        lineTo(deckR, frontEdgeY)
        lineTo(deckL, frontEdgeY)
        close()
    }
    drawPath(
        path = topDeck,
        brush = Brush.verticalGradient(0f to Color(0xFFD0D3D8), 1f to Color(0xFFB1B5BC), startY = topY, endY = frontEdgeY),
    )

    // Iconic PS1 left & right parallel side cooling vent slats
    for (v in 0..5) {
        val vy = topY + 14.dp.toPx() + v * 7.dp.toPx()
        val inset = (1f - v / 6f) * 14.dp.toPx()
        drawLine(Color(0xFF7B7F88), Offset(deckL + inset + 6.dp.toPx(), vy), Offset(deckL + inset + 26.dp.toPx(), vy), 1.6.dp.toPx())
        drawLine(Color(0xFF7B7F88), Offset(deckR - inset - 26.dp.toPx(), vy), Offset(deckR - inset - 6.dp.toPx(), vy), 1.6.dp.toPx())
    }

    // Iconic large round PS1 POWER button (front-left) + smaller RESET button behind it + large round OPEN button (front-right)!
    val pwrCenter = Offset(deckL + 38.dp.toPx(), frontEdgeY - 22.dp.toPx())
    drawOval(Color(0xFF8B8F98), Offset(pwrCenter.x - 16.dp.toPx(), pwrCenter.y - 10.dp.toPx()), Size(32.dp.toPx(), 20.dp.toPx()))
    drawOval(Color(0xFFC2C5CC), Offset(pwrCenter.x - 13.dp.toPx(), pwrCenter.y - 8.dp.toPx()), Size(26.dp.toPx(), 16.dp.toPx()))
    // Smaller RESET oval behind POWER
    val rstCenter = Offset(deckL + 44.dp.toPx(), frontEdgeY - 42.dp.toPx())
    drawOval(Color(0xFF8B8F98), Offset(rstCenter.x - 10.dp.toPx(), rstCenter.y - 6.dp.toPx()), Size(20.dp.toPx(), 12.dp.toPx()))
    drawOval(Color(0xFFC2C5CC), Offset(rstCenter.x - 8.dp.toPx(), rstCenter.y - 4.5.dp.toPx()), Size(16.dp.toPx(), 9.dp.toPx()))

    // Large round OPEN button (front-right)
    val openCenter = Offset(deckR - 38.dp.toPx(), frontEdgeY - 22.dp.toPx())
    drawOval(Color(0xFF8B8F98), Offset(openCenter.x - 16.dp.toPx(), openCenter.y - 10.dp.toPx()), Size(32.dp.toPx(), 20.dp.toPx()))
    drawOval(Color(0xFFC2C5CC), Offset(openCenter.x - 13.dp.toPx(), openCenter.y - 8.dp.toPx()), Size(26.dp.toPx(), 16.dp.toPx()))

    // PS1 vertical front face with twin recessed Memory Card slots + Controller ports!
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF9EA2AA), 1f to Color(0xFF71757E), startY = frontEdgeY, endY = bottomY),
        topLeft = Offset(deckL, frontEdgeY),
        size = Size(deckW, bottomY - frontEdgeY),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    drawLine(Color.White.copy(alpha = 0.45f), Offset(deckL + 6.dp.toPx(), frontEdgeY + 1.dp.toPx()), Offset(deckR - 6.dp.toPx(), frontEdgeY + 1.dp.toPx()), 1.4.dp.toPx())

    // Twin front Memory Card bays + Controller ports (Slot 1 left, Slot 2 right)
    val bayW = 76.dp.toPx()
    val portCenters = listOf(cx - 56.dp.toPx(), cx + 56.dp.toPx())
    portCenters.forEach { pcx ->
        // Memory card slit on top
        drawRoundRect(Color(0xFF565A62), Offset(pcx - bayW * 0.44f, frontEdgeY + 7.dp.toPx()), Size(bayW * 0.88f, 9.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRect(Color(0xFF23262E), Offset(pcx - bayW * 0.36f, frontEdgeY + 10.dp.toPx()), Size(bayW * 0.72f, 3.dp.toPx()))
        // 9-pin PS1 Controller port directly beneath
        drawRoundRect(Color(0xFF282B33), Offset(pcx - bayW * 0.42f, frontEdgeY + 21.dp.toPx()), Size(bayW * 0.84f, 18.dp.toPx()), CornerRadius(4.dp.toPx()))
        for (g in 0..2) {
            drawRect(Color(0xFF101217), Offset(pcx - 24.dp.toPx() + g * 18.dp.toPx(), frontEdgeY + 25.dp.toPx()), Size(12.dp.toPx(), 10.dp.toPx()))
        }
    }

    // Center circular PS1 optical disc well
    val bayCy = h * 0.61f
    val rx = deckW * 0.305f
    val ry = rx * 0.50f
    drawOpticalSpindleWell(cx, bayCy, rx, ry, frame.lidOpen, Color(0xFFBFC3CA))
}

private fun DrawScope.drawPs1ConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.90f).coerceAtMost(332.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val frontEdgeY = h * 0.78f
    val bayCy = h * 0.61f
    val rx = deckW * 0.31f
    val ry = rx * 0.50f

    drawClosingCircularLid(cx, bayCy, rx, ry, frame.lidOpen, Color(0xFFD2D5DA), Color(0xFFB4B8BF)) { center, lidH, closed ->
        // Iconic PlayStation circular lid trim ring + multicolor emblem dot accent
        drawOval(
            color = Color(0xFF7D828C).copy(alpha = 0.65f * closed),
            topLeft = Offset(center.x - rx * 0.82f, center.y - lidH * 0.40f),
            size = Size(rx * 1.64f, lidH * 0.80f),
            style = Stroke(1.3.dp.toPx()),
        )
        drawOval(
            color = Color(0xFFDF2935).copy(alpha = 0.78f * closed),
            topLeft = Offset(center.x - 14.dp.toPx(), center.y - 6.dp.toPx()),
            size = Size(12.dp.toPx(), 12.dp.toPx()),
        )
        drawOval(
            color = Color(0xFF2D6AE0).copy(alpha = 0.78f * closed),
            topLeft = Offset(center.x - 2.dp.toPx(), center.y - 4.dp.toPx()),
            size = Size(16.dp.toPx(), 8.dp.toPx()),
        )
    }

    // Iconic bright green PS1 POWER LED tucked beside the round POWER button!
    drawGlowingLed(
        center = Offset(deckL + 20.dp.toPx(), frontEdgeY - 30.dp.toPx()),
        color = Color(0xFF30F06E),
        glow = frame.ledGlow,
        radiusPx = 3.4.dp.toPx(),
        haloPx = 18.dp.toPx(),
    )
}

// --- Nintendo GameCube (Indigo DOL-001 Cube!) ---
private fun DrawScope.drawGameCubeConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cubeW = (w * 0.72f).coerceAtMost(254.dp.toPx())
    val cubeL = cx - cubeW * 0.5f
    val cubeR = cubeL + cubeW
    val topY = h * 0.36f
    val frontEdgeY = h * 0.64f
    val bottomY = h * 0.94f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), cubeW * 1.08f)

    // Rear black carry handle arch peeking behind the cube!
    drawRoundRect(
        color = Color(0xFF28205A),
        topLeft = Offset(cx - cubeW * 0.28f, topY - 12.dp.toPx()),
        size = Size(cubeW * 0.56f, 16.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(5.dp.toPx()),
    )

    // Indigo GameCube top deck in 3/4 perspective
    val topDeck = Path().apply {
        moveTo(cubeL + 16.dp.toPx(), topY)
        lineTo(cubeR - 16.dp.toPx(), topY)
        lineTo(cubeR, frontEdgeY)
        lineTo(cubeL, frontEdgeY)
        close()
    }
    drawPath(
        path = topDeck,
        brush = Brush.verticalGradient(0f to Color(0xFF6B5AC4), 1f to Color(0xFF4E3EA6), startY = topY, endY = frontEdgeY),
    )

    // Authentic GameCube top buttons: round grey POWER (front-left), smaller RESET (back-left), OPEN (front-right)
    val pwr = Offset(cubeL + 30.dp.toPx(), frontEdgeY - 22.dp.toPx())
    drawOval(Color(0xFFD5D7E0), Offset(pwr.x - 11.dp.toPx(), pwr.y - 7.dp.toPx()), Size(22.dp.toPx(), 14.dp.toPx()))
    val rst = Offset(cubeL + 34.dp.toPx(), topY + 26.dp.toPx())
    drawOval(Color(0xFFC2C4CE), Offset(rst.x - 7.dp.toPx(), rst.y - 4.5.dp.toPx()), Size(14.dp.toPx(), 9.dp.toPx()))
    val opn = Offset(cubeR - 30.dp.toPx(), frontEdgeY - 22.dp.toPx())
    drawOval(Color(0xFFD5D7E0), Offset(opn.x - 11.dp.toPx(), opn.y - 7.dp.toPx()), Size(22.dp.toPx(), 14.dp.toPx()))

    // Tall Indigo cube front shell wrapping the iconic light-grey controller faceplate!
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF4F3FA8), 1f to Color(0xFF332678), startY = frontEdgeY, endY = bottomY),
        topLeft = Offset(cubeL, frontEdgeY),
        size = Size(cubeW, bottomY - frontEdgeY),
        cornerRadius = CornerRadius(10.dp.toPx()),
    )
    drawLine(Color.White.copy(alpha = 0.35f), Offset(cubeL + 8.dp.toPx(), frontEdgeY + 1.5.dp.toPx()), Offset(cubeR - 8.dp.toPx(), frontEdgeY + 1.5.dp.toPx()), 1.4.dp.toPx())

    // Iconic light-grey recessed GameCube front panel: 2 Memory Card flaps + 4 circular controller ports!
    val faceInset = 12.dp.toPx()
    val faceL = cubeL + faceInset
    val faceW = cubeW - faceInset * 2f
    val faceTop = frontEdgeY + 10.dp.toPx()
    val faceH = (bottomY - frontEdgeY) - 20.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFFDBDCE2), 1f to Color(0xFFB5B7C2), startY = faceTop, endY = faceTop + faceH),
        topLeft = Offset(faceL, faceTop),
        size = Size(faceW, faceH),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    // Slot A & Slot B Memory Card spring doors
    val mcW = faceW * 0.36f
    drawRoundRect(Color(0xFF8C8F9C), Offset(faceL + 16.dp.toPx(), faceTop + 10.dp.toPx()), Size(mcW, 13.dp.toPx()), CornerRadius(3.dp.toPx()))
    drawRoundRect(Color(0xFF8C8F9C), Offset(faceL + faceW - 16.dp.toPx() - mcW, faceTop + 10.dp.toPx()), Size(mcW, 13.dp.toPx()), CornerRadius(3.dp.toPx()))

    // 4 iconic circular GameCube Controller ports across the lower faceplate!
    val portCy = faceTop + faceH * 0.66f
    val portSpacing = faceW / 4.4f
    val firstPortX = cx - portSpacing * 1.5f
    for (i in 0..3) {
        val px = firstPortX + i * portSpacing
        drawCircle(Color(0xFF23252C), 12.dp.toPx(), Offset(px, portCy))
        drawCircle(Color(0xFF595D6B), 12.dp.toPx(), Offset(px, portCy), style = Stroke(1.2.dp.toPx()))
        drawCircle(Color(0xFF101218), 8.dp.toPx(), Offset(px, portCy))
        // Tiny gold pin cluster
        drawCircle(Color(0xFFC2A858), 1.4.dp.toPx(), Offset(px - 3.dp.toPx(), portCy - 2.dp.toPx()))
        drawCircle(Color(0xFFC2A858), 1.4.dp.toPx(), Offset(px + 3.dp.toPx(), portCy - 2.dp.toPx()))
        drawCircle(Color(0xFFC2A858), 1.4.dp.toPx(), Offset(px, portCy + 2.5.dp.toPx()))
    }

    // Compact 8cm Mini-DVD bay centered on the top deck
    val bayCy = h * 0.50f
    val rx = cubeW * 0.305f
    val ry = rx * 0.50f
    drawOpticalSpindleWell(cx, bayCy, rx, ry, frame.lidOpen, Color(0xFF5F4FB8))
}

private fun DrawScope.drawGameCubeConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cubeW = (w * 0.72f).coerceAtMost(254.dp.toPx())
    val frontEdgeY = h * 0.64f
    val bayCy = h * 0.50f
    val rx = cubeW * 0.31f
    val ry = rx * 0.50f

    drawClosingCircularLid(cx, bayCy, rx, ry, frame.lidOpen, Color(0xFF6F5EC9), Color(0xFF5141AB)) { center, lidH, closed ->
        // Iconic glossy black circular "NINTENDO GAMECUBE" center lid jewel!
        drawOval(
            color = Color(0xFF11131A).copy(alpha = 0.94f * closed),
            topLeft = Offset(center.x - 24.dp.toPx(), center.y - lidH * 0.24f),
            size = Size(48.dp.toPx(), lidH * 0.48f),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.32f * closed),
            topLeft = Offset(center.x - 24.dp.toPx(), center.y - lidH * 0.24f),
            size = Size(48.dp.toPx(), lidH * 0.48f),
            style = Stroke(1.2.dp.toPx()),
        )
    }

    // Iconic glowing AMBER-ORANGE GameCube Power LED dead-center on the top front rim!
    drawGlowingLed(
        center = Offset(cx, frontEdgeY - 8.dp.toPx()),
        color = Color(0xFFFF8C1A),
        glow = frame.ledGlow,
        radiusPx = 4.2.dp.toPx(),
        haloPx = 22.dp.toPx(),
    )
}

// --- Sega Dreamcast ---
private fun DrawScope.drawDreamcastConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.86f).coerceAtMost(316.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.43f
    val frontEdgeY = h * 0.77f
    val bottomY = h * 0.93f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), deckW * 1.04f)

    // Sculpted pearl-white Dreamcast top shell
    val topDeck = Path().apply {
        moveTo(deckL + 22.dp.toPx(), topY)
        lineTo(deckR - 22.dp.toPx(), topY)
        lineTo(deckR, frontEdgeY)
        lineTo(deckL, frontEdgeY)
        close()
    }
    drawPath(
        path = topDeck,
        brush = Brush.verticalGradient(0f to Color(0xFFF0F1F4), 1f to Color(0xFFD5D8E0), startY = topY, endY = frontEdgeY),
    )

    // Trapezoidal Dreamcast POWER (left) and OPEN (right) buttons
    drawRoundRect(Color(0xFFC2C6D0), Offset(deckL + 22.dp.toPx(), frontEdgeY - 26.dp.toPx()), Size(32.dp.toPx(), 16.dp.toPx()), CornerRadius(4.dp.toPx()))
    drawRoundRect(Color(0xFFC2C6D0), Offset(deckR - 54.dp.toPx(), frontEdgeY - 26.dp.toPx()), Size(32.dp.toPx(), 16.dp.toPx()), CornerRadius(4.dp.toPx()))

    // Sculpted white front bezel with iconic curved recessed 4-controller bay!
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFFD4D7DF), 1f to Color(0xFFA7ACB8), startY = frontEdgeY, endY = bottomY),
        topLeft = Offset(deckL, frontEdgeY),
        size = Size(deckW, bottomY - frontEdgeY),
        cornerRadius = CornerRadius(10.dp.toPx()),
    )
    // Recessed 4 semicircular Dreamcast controller ports (A, B, C, D)
    val portW = 46.dp.toPx()
    val portH = 22.dp.toPx()
    val gap = 12.dp.toPx()
    val startX = cx - (portW * 4f + gap * 3f) * 0.5f
    val portY = frontEdgeY + (bottomY - frontEdgeY - portH) * 0.54f
    for (i in 0..3) {
        val px = startX + i * (portW + gap)
        drawRoundRect(Color(0xFF424650), Offset(px, portY), Size(portW, portH), CornerRadius(10.dp.toPx()))
        drawRoundRect(Color(0xFF181A20), Offset(px + 5.dp.toPx(), portY + 5.dp.toPx()), Size(portW - 10.dp.toPx(), portH - 10.dp.toPx()), CornerRadius(6.dp.toPx()))
    }

    val bayCy = h * 0.60f
    val rx = deckW * 0.31f
    val ry = rx * 0.50f
    drawOpticalSpindleWell(cx, bayCy, rx, ry, frame.lidOpen, Color(0xFFE6E8EE))
}

private fun DrawScope.drawDreamcastConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.86f).coerceAtMost(316.dp.toPx())
    val frontEdgeY = h * 0.77f
    val bayCy = h * 0.60f
    val rx = deckW * 0.315f
    val ry = rx * 0.50f

    drawClosingCircularLid(cx, bayCy, rx, ry, frame.lidOpen, Color(0xFFF2F3F6), Color(0xFFD8DBE2)) { center, _, closed ->
        // Iconic Dreamcast orange swirl emblem on the center lid!
        drawCircle(Color(0xFFFF641E).copy(alpha = 0.88f * closed), 12.dp.toPx(), center, style = Stroke(2.4.dp.toPx()))
        drawCircle(Color(0xFFFF641E).copy(alpha = 0.88f * closed), 6.5.dp.toPx(), Offset(center.x + 1.5.dp.toPx(), center.y - 1.dp.toPx()), style = Stroke(2.dp.toPx()))
    }

    // Iconic glowing translucent ORANGE TRIANGLE LED in front of the lid!
    val triCy = frontEdgeY - 12.dp.toPx()
    val triPath = Path().apply {
        moveTo(cx, triCy - 7.dp.toPx())
        lineTo(cx + 8.dp.toPx(), triCy + 6.dp.toPx())
        lineTo(cx - 8.dp.toPx(), triCy + 6.dp.toPx())
        close()
    }
    if (frame.ledGlow > 0.04f) {
        drawCircle(
            brush = Brush.radialGradient(0f to Color(0xFFFF6D1F).copy(alpha = 0.85f * frame.ledGlow), 1f to Color.Transparent, center = Offset(cx, triCy), radius = 22.dp.toPx()),
            radius = 22.dp.toPx(),
            center = Offset(cx, triCy),
        )
    }
    drawPath(triPath, color = if (frame.ledGlow > 0.05f) Color(0xFFFF7A26) else Color(0xFF9C4D26).copy(alpha = 0.5f))
}

// --- Sega Saturn / Sega CD / 3DO / Neo Geo CD ---
private fun DrawScope.drawSaturnConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(324.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.44f
    val frontEdgeY = h * 0.78f
    val bottomY = h * 0.93f

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), deckW * 1.05f)

    val topDeck = Path().apply {
        moveTo(deckL + 22.dp.toPx(), topY)
        lineTo(deckR - 22.dp.toPx(), topY)
        lineTo(deckR, frontEdgeY)
        lineTo(deckL, frontEdgeY)
        close()
    }
    drawPath(topDeck, brush = Brush.verticalGradient(0f to hwSpec.bodyTop, 1f to hwSpec.bodyMid, startY = topY, endY = frontEdgeY))
    // Sculpted side wing steps (classic Saturn ridge)
    drawLine(Color.White.copy(alpha = 0.14f), Offset(deckL + 42.dp.toPx(), topY + 4.dp.toPx()), Offset(deckL + 30.dp.toPx(), frontEdgeY - 4.dp.toPx()), 2.dp.toPx())
    drawLine(Color.White.copy(alpha = 0.14f), Offset(deckR - 42.dp.toPx(), topY + 4.dp.toPx()), Offset(deckR - 30.dp.toPx(), frontEdgeY - 4.dp.toPx()), 2.dp.toPx())

    // Oval POWER & RESET buttons on front-left
    drawOval(Color(0xFF525866), Offset(deckL + 20.dp.toPx(), frontEdgeY - 24.dp.toPx()), Size(26.dp.toPx(), 13.dp.toPx()))
    drawOval(Color(0xFF424754), Offset(deckL + 50.dp.toPx(), frontEdgeY - 24.dp.toPx()), Size(22.dp.toPx(), 12.dp.toPx()))

    // Front face with dual controller ports
    drawRoundRect(
        brush = Brush.verticalGradient(0f to hwSpec.bodyMid, 1f to hwSpec.bodyDark, startY = frontEdgeY, endY = bottomY),
        topLeft = Offset(deckL, frontEdgeY),
        size = Size(deckW, bottomY - frontEdgeY),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    for (p in 0..1) {
        val px = cx - 76.dp.toPx() + p * 98.dp.toPx()
        drawRoundRect(Color(0xFF0A0B0F), Offset(px, frontEdgeY + 14.dp.toPx()), Size(54.dp.toPx(), 20.dp.toPx()), CornerRadius(4.dp.toPx()))
    }

    val bayCy = h * 0.61f
    val rx = deckW * 0.305f
    val ry = rx * 0.50f
    drawOpticalSpindleWell(cx, bayCy, rx, ry, frame.lidOpen, hwSpec.bodyTop)
}

private fun DrawScope.drawSaturnConsoleFront(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.88f).coerceAtMost(324.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val frontEdgeY = h * 0.78f
    val bayCy = h * 0.61f
    val rx = deckW * 0.31f
    val ry = rx * 0.50f

    drawClosingCircularLid(cx, bayCy, rx, ry, frame.lidOpen, hwSpec.bodyTop, hwSpec.bodyMid) { center, _, closed ->
        drawOval(hwSpec.accentColor.copy(alpha = 0.65f * closed), Offset(center.x - 22.dp.toPx(), center.y - 10.dp.toPx()), Size(44.dp.toPx(), 20.dp.toPx()), style = Stroke(2.dp.toPx()))
    }

    drawGlowingLed(Offset(deckL + deckW - 34.dp.toPx(), frontEdgeY - 14.dp.toPx()), hwSpec.ledColor, frame.ledGlow, 3.5.dp.toPx(), 18.dp.toPx())
}

// =================================================================================================
// 10. PLAYSTATION 2 (Fat SCPH-30001 Monolith) — Motorized Extending Front Disc Tray!
// =================================================================================================

internal fun DrawScope.drawPs2ConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.90f).coerceAtMost(332.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.34f
    val driveMouthY = h * 0.47f
    val midStepY = h * 0.63f
    val bottomY = h * 0.78f

    drawConsoleShadow(cx, bottomY + 14.dp.toPx(), deckW * 1.05f)

    // Upper ribbed PS2 monolith roof (steep crisp 3/4 slab!)
    val roofPath = Path().apply {
        moveTo(deckL + 18.dp.toPx(), topY)
        lineTo(deckR - 18.dp.toPx(), topY)
        lineTo(deckR, driveMouthY)
        lineTo(deckL, driveMouthY)
        close()
    }
    drawPath(roofPath, brush = Brush.verticalGradient(0f to Color(0xFF2B2E36), 1f to Color(0xFF1A1C22), startY = topY, endY = driveMouthY))
    // Subtle ribbed top grooves characteristic of the PS2!
    for (r in 0..4) {
        val x = deckL + 28.dp.toPx() + r * 20.dp.toPx()
        drawLine(Color(0xFF111318), Offset(x, topY + 2.dp.toPx()), Offset(x - 6.dp.toPx(), driveMouthY - 2.dp.toPx()), 1.4.dp.toPx())
    }

    // Upper front monolith body
    drawRect(
        brush = Brush.verticalGradient(0f to Color(0xFF21242C), 1f to Color(0xFF13151A), startY = driveMouthY, endY = midStepY),
        topLeft = Offset(deckL, driveMouthY),
        size = Size(deckW, midStepY - driveMouthY),
    )

    // Lower stepped narrower PS2 monolith base with twin Memory Card & Controller ports + vertical louvers!
    val baseL = deckL + 10.dp.toPx()
    val baseW = deckW - 20.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF17191F), 1f to Color(0xFF0C0E12), startY = midStepY, endY = bottomY),
        topLeft = Offset(baseL, midStepY),
        size = Size(baseW, bottomY - midStepY),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    // Iconic PS2 blue horizontal accent stripe between upper & lower tiers
    drawRect(Color(0xFF2A66FF).copy(alpha = 0.78f), Offset(baseL + 4.dp.toPx(), midStepY - 1.dp.toPx()), Size(baseW - 8.dp.toPx(), 2.5.dp.toPx()))

    // Twin left Controller/MC ports + right-side ribbed PS2 grille louvers
    for (p in 0..1) {
        val px = baseL + 14.dp.toPx() + p * 64.dp.toPx()
        drawRoundRect(Color(0xFF0A0B0E), Offset(px, midStepY + 8.dp.toPx()), Size(52.dp.toPx(), 10.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(Color(0xFF0A0B0E), Offset(px, midStepY + 22.dp.toPx()), Size(52.dp.toPx(), 16.dp.toPx()), CornerRadius(3.dp.toPx()))
    }
    for (l in 0..6) {
        val ly = midStepY + 9.dp.toPx() + l * 4.5.dp.toPx()
        drawLine(Color(0xFF282C36), Offset(cx + 12.dp.toPx(), ly), Offset(baseL + baseW - 14.dp.toPx(), ly), 1.6.dp.toPx())
    }

    // Dark recessed optical drive mouth on upper-left of PS2 front face
    val trayW = deckW * 0.56f
    val trayL = deckL + 14.dp.toPx()
    drawRect(Color(0xFF050608), Offset(trayL, driveMouthY + 6.dp.toPx()), Size(trayW, 24.dp.toPx()))

    // Motorized extending black PS2 disc tray sliding OUT forward from the upper-left drive mouth!
    val trayOut = frame.trayOut.coerceIn(0f, 1f)
    if (trayOut > 0.02f) {
        val trayCx = trayL + trayW * 0.5f
        val trayTipY = driveMouthY + 28.dp.toPx() + trayOut * 84.dp.toPx()
        val trayBackY = driveMouthY + 14.dp.toPx()
        // Sculpted black sliding tray bed
        val trayPath = Path().apply {
            moveTo(trayL + 6.dp.toPx(), trayBackY)
            lineTo(trayL + trayW - 6.dp.toPx(), trayBackY)
            lineTo(trayL + trayW + 2.dp.toPx(), trayTipY)
            lineTo(trayL - 2.dp.toPx(), trayTipY)
            close()
        }
        drawPath(trayPath, brush = Brush.verticalGradient(0f to Color(0xFF14161C), 1f to Color(0xFF282C36), startY = trayBackY, endY = trayTipY))
        drawPath(trayPath, color = Color.White.copy(alpha = 0.18f), style = Stroke(1.2.dp.toPx()))

        // Recessed circular disc ring on the extended tray
        val ovalCx = trayCx
        val ovalCy = trayBackY + (trayTipY - trayBackY) * 0.54f
        val rx = trayW * 0.44f
        val ry = (trayTipY - trayBackY) * 0.42f
        if (ry > 6.dp.toPx()) {
            drawOval(Color(0xFF0E1015), Offset(ovalCx - rx, ovalCy - ry), Size(rx * 2f, ry * 2f))
            drawOval(Color.White.copy(alpha = 0.14f), Offset(ovalCx - rx, ovalCy - ry), Size(rx * 2f, ry * 2f), style = Stroke(1.2.dp.toPx()))
            // Center spindle guide ring
            drawOval(Color(0xFF3E4350), Offset(ovalCx - 12.dp.toPx(), ovalCy - 5.dp.toPx()), Size(24.dp.toPx(), 10.dp.toPx()))
        }
        // Thick glossy black tray front lip with blue accent line
        drawRoundRect(Color(0xFF1F222A), Offset(trayL - 2.dp.toPx(), trayTipY - 4.dp.toPx()), Size(trayW + 4.dp.toPx(), 14.dp.toPx()), CornerRadius(3.dp.toPx()))
        drawLine(Color(0xFF3B7BFF), Offset(trayCx - 14.dp.toPx(), trayTipY + 3.dp.toPx()), Offset(trayCx + 14.dp.toPx(), trayTipY + 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}

internal fun DrawScope.drawPs2ConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.90f).coerceAtMost(332.dp.toPx())
    val deckL = cx - deckW * 0.5f
    val deckR = deckL + deckW
    val driveMouthY = h * 0.47f
    val trayW = deckW * 0.56f
    val trayL = deckL + 14.dp.toPx()

    // Upper roof brow crisp highlight line masking the top of the retracting disc!
    drawRect(Color(0xFF1E2128), Offset(deckL, driveMouthY - 4.dp.toPx()), Size(deckW, 11.dp.toPx()))
    drawLine(Color.White.copy(alpha = 0.28f), Offset(deckL + 4.dp.toPx(), driveMouthY), Offset(deckR - 4.dp.toPx(), driveMouthY), 1.3.dp.toPx())

    // Flush tray front door plate once retracted
    if (frame.trayOut <= 0.08f) {
        drawRoundRect(Color(0xFF1E212A), Offset(trayL, driveMouthY + 7.dp.toPx()), Size(trayW, 22.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(trayL, driveMouthY + 7.dp.toPx()), Size(trayW, 22.dp.toPx()), CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
        drawLine(Color(0xFF3B7BFF), Offset(trayL + trayW * 0.5f - 14.dp.toPx(), driveMouthY + 18.dp.toPx()), Offset(trayL + trayW * 0.5f + 14.dp.toPx(), driveMouthY + 18.dp.toPx()), 2.4.dp.toPx(), StrokeCap.Round)
    }

    // Iconic PS2 glowing GREEN RESET & ELECTRIC-BLUE EJECT LEDs on the right bezel!
    drawGlowingLed(Offset(deckR - 22.dp.toPx(), driveMouthY + 16.dp.toPx()), Color(0xFF2EE86C), frame.ledGlow, 3.6.dp.toPx(), 18.dp.toPx())
    drawGlowingLed(Offset(deckR - 22.dp.toPx(), driveMouthY + 34.dp.toPx()), Color(0xFF3282FF), (0.35f + frame.ledGlow * 0.65f), 3.6.dp.toPx(), 18.dp.toPx())
}

// =================================================================================================
// 11. PLAYSTATION PORTABLE (PSP) — Real Piano-Black Rear Shell + Hinged Chrome-Ring UMD Hatch!
// =================================================================================================

internal fun DrawScope.drawPspConsoleBack(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.92f).coerceAtMost(336.dp.toPx())
    val bodyL = cx - bodyW * 0.5f
    val bodyR = bodyL + bodyW
    val topY = h * 0.44f
    val bottomY = h * 0.90f
    val bodyH = bottomY - topY

    drawConsoleShadow(cx, bottomY + 6.dp.toPx(), bodyW * 1.02f)

    // Translucent silver-crystal L and R shoulder triggers on the upper-left and upper-right corners!
    drawRoundRect(Color(0xFFD0D5E0).copy(alpha = 0.85f), Offset(bodyL + 22.dp.toPx(), topY - 4.dp.toPx()), Size(58.dp.toPx(), 16.dp.toPx()), CornerRadius(10.dp.toPx()))
    drawRoundRect(Color(0xFFD0D5E0).copy(alpha = 0.85f), Offset(bodyR - 80.dp.toPx(), topY - 4.dp.toPx()), Size(58.dp.toPx(), 16.dp.toPx()), CornerRadius(10.dp.toPx()))

    // Iconic sleek pill-shaped glossy Piano Black PSP rear silhouette!
    drawRoundRect(
        brush = Brush.verticalGradient(0f to Color(0xFF2E313A), 0.45f to Color(0xFF17191F), 1f to Color(0xFF0C0D12), startY = topY, endY = bottomY),
        topLeft = Offset(bodyL, topY),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(bodyH * 0.46f),
    )
    // Chrome-silver edge trim halo around the pill chassis
    drawRoundRect(
        color = Color(0xFFB8C0D0).copy(alpha = 0.36f),
        topLeft = Offset(bodyL + 1.5.dp.toPx(), topY + 1.5.dp.toPx()),
        size = Size(bodyW - 3.dp.toPx(), bodyH - 3.dp.toPx()),
        cornerRadius = CornerRadius(bodyH * 0.46f),
        style = Stroke(2.dp.toPx()),
    )

    // Recessed circular internal UMD bay + brushed stainless-steel UMD basket rails!
    val bayR = bodyH * 0.42f
    val bayCy = topY + bodyH * 0.50f
    drawCircle(Color(0xFF0A0B0E), bayR, Offset(cx, bayCy))
    drawCircle(Color(0xFF3E4452), bayR, Offset(cx, bayCy), style = Stroke(1.6.dp.toPx()))
    // Brushed metallic UMD spindle + sled frame inside the open bay
    drawRect(Color(0xFF7A8294).copy(alpha = 0.45f), Offset(cx - bayR * 0.65f, bayCy - 18.dp.toPx()), Size(bayR * 1.3f, 36.dp.toPx()))
    drawCircle(Color(0xFFB8C0CE), 10.dp.toPx(), Offset(cx, bayCy))
    drawCircle(Color(0xFF282C36), 4.dp.toPx(), Offset(cx, bayCy))
}

internal fun DrawScope.drawPspConsoleFront(frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bodyW = (w * 0.92f).coerceAtMost(336.dp.toPx())
    val bodyR = cx + bodyW * 0.5f
    val topY = h * 0.44f
    val bottomY = h * 0.90f
    val bodyH = bottomY - topY
    val bayR = bodyH * 0.42f
    val bayCy = topY + bodyH * 0.50f

    // Hinged Piano-Black Rear UMD Door with iconic SILVER CHROME RING!
    // Hinged at bottom (bayCy + bayR): tilts open downward when lidOpen = 1, swings UP flush when lidOpen = 0!
    val closed = (1f - frame.lidOpen).coerceIn(0f, 1f)
    val doorH = (bayR * 2f) * (0.22f + 0.78f * closed)
    val hingeY = bayCy + bayR
    val doorTop = hingeY - doorH + (1f - closed) * 14.dp.toPx()

    drawOval(
        brush = Brush.verticalGradient(0f to Color(0xFF2C2F38), 1f to Color(0xFF14161C), startY = doorTop, endY = doorTop + doorH),
        topLeft = Offset(cx - bayR, doorTop),
        size = Size(bayR * 2f, doorH),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(cx - bayR, doorTop),
        size = Size(bayR * 2f, doorH),
        style = Stroke(1.4.dp.toPx()),
    )

    // Iconic metallic SILVER CHROME RING on the back of the PSP UMD door!
    val ringRx = bayR * 0.64f
    val ringRy = doorH * 0.32f
    val ringCy = doorTop + doorH * 0.50f
    drawOval(
        brush = Brush.linearGradient(
            0f to Color(0xFFEFF3FA),
            0.5f to Color(0xFF9EA6B6),
            1f to Color(0xFFEFF3FA),
            start = Offset(cx - ringRx, ringCy - ringRy),
            end = Offset(cx + ringRx, ringCy + ringRy),
        ),
        topLeft = Offset(cx - ringRx, ringCy - ringRy),
        size = Size(ringRx * 2f, ringRy * 2f),
        style = Stroke(width = 4.dp.toPx()),
    )

    // Green Power LED on the right pill grip
    drawGlowingLed(Offset(bodyR - 22.dp.toPx(), topY + bodyH * 0.52f), Color(0xFF34EB6E), frame.ledGlow, 3.4.dp.toPx(), 18.dp.toPx())
}

// =================================================================================================
// 12. NINTENDO WII / WII U — Upright Glossy White Slanted Tower + Vertical Electric-Blue Disc Slot!
// =================================================================================================

internal fun DrawScope.drawWiiConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val isWiiU = hwSpec.systemId == "wiiu"

    val towerW = 86.dp.toPx()
    val towerL = cx - 98.dp.toPx()
    val towerTop = h * 0.14f
    val towerBottom = h * 0.86f
    val slant = 16.dp.toPx()

    drawConsoleShadow(towerL + towerW * 0.55f, towerBottom + 14.dp.toPx(), 156.dp.toPx(), 24.dp.toPx())

    // Iconic angled metallic silver Wii stand base
    val standPath = Path().apply {
        moveTo(towerL - 18.dp.toPx(), towerBottom + 10.dp.toPx())
        lineTo(towerL + towerW + 30.dp.toPx(), towerBottom + 10.dp.toPx())
        lineTo(towerL + towerW + 18.dp.toPx(), towerBottom - 4.dp.toPx())
        lineTo(towerL - 6.dp.toPx(), towerBottom - 4.dp.toPx())
        close()
    }
    drawPath(standPath, brush = Brush.verticalGradient(0f to Color(0xFFD5DAE3), 1f to Color(0xFF7B8290), startY = towerBottom - 4.dp.toPx(), endY = towerBottom + 10.dp.toPx()))

    // Side body depth of the upright slanted Wii monolith
    val sideTop = if (isWiiU) Color(0xFF2B2F38) else Color(0xFFDCE1EA)
    val sideBot = if (isWiiU) Color(0xFF14171E) else Color(0xFFA7B0BE)
    val sidePath = Path().apply {
        moveTo(towerL - 22.dp.toPx() + slant, towerTop + 6.dp.toPx())
        lineTo(towerL + slant, towerTop)
        lineTo(towerL, towerBottom)
        lineTo(towerL - 22.dp.toPx(), towerBottom - 6.dp.toPx())
        close()
    }
    drawPath(sidePath, brush = Brush.verticalGradient(0f to sideTop, 1f to sideBot, startY = towerTop, endY = towerBottom))

    // Right-of-slot glossy front lip (drawn BEHIND the disc so the disc slides directly INTO the glowing blue slot!)
    val faceTop = if (isWiiU) Color(0xFF333842) else Color(0xFFF6F8FC)
    val faceMid = if (isWiiU) Color(0xFF20242C) else Color(0xFFE3E7F0)
    val faceBot = if (isWiiU) Color(0xFF13161C) else Color(0xFFC5CCD8)
    val slotOffsetFromRight = 13.dp.toPx()
    val rightStripPath = Path().apply {
        moveTo(towerL + towerW + slant - slotOffsetFromRight, towerTop)
        lineTo(towerL + towerW + slant, towerTop)
        lineTo(towerL + towerW, towerBottom)
        lineTo(towerL + towerW - slotOffsetFromRight, towerBottom)
        close()
    }
    drawPath(rightStripPath, brush = Brush.verticalGradient(0f to faceTop, 0.55f to faceMid, 1f to faceBot, startY = towerTop, endY = towerBottom))
    // Dark physical vertical slot backing behind the entering disc
    val slotTop = Offset(towerL + towerW + slant - slotOffsetFromRight, towerTop + 26.dp.toPx())
    val slotBot = Offset(towerL + towerW - slotOffsetFromRight + slant * 0.22f, towerBottom - 44.dp.toPx())
    drawLine(Color(0xFF0A0D14), slotTop, slotBot, strokeWidth = 5.5.dp.toPx(), cap = StrokeCap.Round)
}

internal fun DrawScope.drawWiiConsoleFront(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val isWiiU = hwSpec.systemId == "wiiu"

    val towerW = 86.dp.toPx()
    val towerL = cx - 98.dp.toPx()
    val towerTop = h * 0.14f
    val towerBottom = h * 0.86f
    val slant = 16.dp.toPx()
    val slotOffsetFromRight = 13.dp.toPx()

    // Left-of-slot glossy slanted front face of the Wii (masks the disc precisely at the vertical blue slot!)
    val faceTop = if (isWiiU) Color(0xFF333842) else Color(0xFFF6F8FC)
    val faceMid = if (isWiiU) Color(0xFF20242C) else Color(0xFFE3E7F0)
    val faceBot = if (isWiiU) Color(0xFF13161C) else Color(0xFFC5CCD8)
    val frontLeftPath = Path().apply {
        moveTo(towerL + slant, towerTop)
        lineTo(towerL + towerW + slant - slotOffsetFromRight, towerTop)
        lineTo(towerL + towerW - slotOffsetFromRight, towerBottom)
        lineTo(towerL, towerBottom)
        close()
    }
    drawPath(frontLeftPath, brush = Brush.verticalGradient(0f to faceTop, 0.55f to faceMid, 1f to faceBot, startY = towerTop, endY = towerBottom))
    drawPath(frontLeftPath, color = Color.White.copy(alpha = if (isWiiU) 0.18f else 0.72f), style = Stroke(1.2.dp.toPx()))

    // Tiny iconic Wii front buttons down the left edge: POWER (with green LED), RESET, EJECT + SD card flap!
    val btnXTop = towerL + slant + 18.dp.toPx()
    val pwrCenter = Offset(btnXTop - 2.dp.toPx(), towerTop + 34.dp.toPx())
    drawCircle(Color(0xFFB4BCC9), 6.5.dp.toPx(), pwrCenter)
    drawGlowingLed(Offset(pwrCenter.x - 10.dp.toPx(), pwrCenter.y), Color(0xFF2CE86C), frame.ledGlow, 2.5.dp.toPx(), 12.dp.toPx())
    drawCircle(Color(0xFFB4BCC9), 5.5.dp.toPx(), Offset(btnXTop - 4.dp.toPx(), towerTop + 58.dp.toPx()))
    // Recessed SD Card door
    drawRoundRect(
        color = Color.Black.copy(alpha = if (isWiiU) 0.25f else 0.08f),
        topLeft = Offset(towerL + slant * 0.42f + 12.dp.toPx(), towerTop + 88.dp.toPx()),
        size = Size(28.dp.toPx(), 96.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(towerL + slant * 0.42f + 12.dp.toPx(), towerTop + 88.dp.toPx()),
        size = Size(28.dp.toPx(), 96.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(1.dp.toPx()),
    )
    drawCircle(Color(0xFFB4BCC9), 5.5.dp.toPx(), Offset(towerL + 16.dp.toPx(), towerBottom - 36.dp.toPx()))

    // Iconic VERTICAL GLOWING ELECTRIC-BLUE OPTICAL DISC SLOT halo hugging the entering disc!
    val slotTop = Offset(towerL + towerW + slant - slotOffsetFromRight, towerTop + 26.dp.toPx())
    val slotBot = Offset(towerL + towerW - slotOffsetFromRight + slant * 0.22f, towerBottom - 44.dp.toPx())
    val blueGlow = (0.38f + frame.ledGlow * 0.62f).coerceIn(0f, 1f)
    drawLine(
        color = Color(0xFF1EC8FF).copy(alpha = 0.34f * blueGlow),
        start = slotTop,
        end = slotBot,
        strokeWidth = 22.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF42DCFF).copy(alpha = 0.72f * blueGlow),
        start = Offset(slotTop.x - 1.dp.toPx(), slotTop.y),
        end = Offset(slotBot.x - 1.dp.toPx(), slotBot.y),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFB6F4FF).copy(alpha = 0.90f * blueGlow),
        start = Offset(slotTop.x - 2.dp.toPx(), slotTop.y),
        end = Offset(slotBot.x - 2.dp.toPx(), slotBot.y),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}
