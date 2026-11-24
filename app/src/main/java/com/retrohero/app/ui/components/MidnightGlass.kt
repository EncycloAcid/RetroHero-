package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

/**
 * Midnight Glass & Console Aurora atmospheric backdrop:
 * deep midnight (or frost light) canvas bathed in two soft radial aurora glows
 * tinted dynamically by the highlighted game or console color.
 */
fun Modifier.auroraBackdrop(
    primary: Color = Salmon,
    secondary: Color = Color(0xFF7B5CFF),
): Modifier = drawBehind {
    val bg = Tone.Bg
    drawRect(bg)
    val dark = Tone.isDark
    val a1 = if (dark) 0.30f else 0.16f
    val a2 = if (dark) 0.22f else 0.11f
    val w = size.width
    val h = size.height
    drawCircle(
        brush = Brush.radialGradient(
            0f to primary.copy(alpha = a1),
            0.55f to primary.copy(alpha = a1 * 0.35f),
            1f to Color.Transparent,
            center = Offset(w * 0.20f, h * 0.06f),
            radius = w * 0.98f,
        ),
        radius = w * 0.98f,
        center = Offset(w * 0.20f, h * 0.06f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to secondary.copy(alpha = a2),
            0.55f to secondary.copy(alpha = a2 * 0.30f),
            1f to Color.Transparent,
            center = Offset(w * 0.88f, h * 0.22f),
            radius = w * 0.86f,
        ),
        radius = w * 0.86f,
        center = Offset(w * 0.88f, h * 0.22f),
    )
}

/**
 * Luminous frosted translucent glass surface with a soft specular top-to-bottom sheen
 * and razor-thin luminous rim border.
 */
fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color? = null,
    elevated: Boolean = false,
): Modifier {
    val dark = Tone.isDark
    val fillBrush = if (dark) {
        val top = tint?.copy(alpha = if (elevated) 0.24f else 0.16f)
            ?: Color.White.copy(alpha = if (elevated) 0.12f else 0.08f)
        val bot = tint?.copy(alpha = 0.06f)
            ?: Color.White.copy(alpha = if (elevated) 0.05f else 0.03f)
        Brush.verticalGradient(listOf(top, bot))
    } else {
        val top = tint?.copy(alpha = 0.14f) ?: Color.White.copy(alpha = if (elevated) 0.94f else 0.82f)
        val bot = tint?.copy(alpha = 0.05f) ?: Color.White.copy(alpha = if (elevated) 0.84f else 0.68f)
        Brush.verticalGradient(listOf(top, bot))
    }
    val rimBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (elevated) 0.26f else 0.18f),
                Color.White.copy(alpha = 0.05f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF1A2548).copy(alpha = 0.10f),
            ),
        )
    }
    return this
        .clip(shape)
        .background(if (dark) Color(0xFF0E1322).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.65f))
        .background(fillBrush)
        .border(1.dp, rimBrush, shape)
}

/** Circular frosted glass orb button for floating header actions. */
@Composable
fun GlassOrb(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    accent: Boolean = false,
) {
    val bgMod = if (accent) {
        Modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Ascend.accent, Ascend.accent.copy(alpha = 0.82f))))
            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
    } else {
        Modifier.glassPanel(CircleShape, elevated = true)
    }
    Box(
        modifier
            .size(size)
            .then(bgMod)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (accent) Color.White else Tone.Ink,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** Sleek pill-shaped glass action button (luminous accent or translucent glass). */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    height: Dp = 44.dp,
) {
    val shape = CircleShape
    val surfaceMod = if (primary) {
        Modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Ascend.accent, Color(0xFF7B68FF))))
            .border(1.dp, Color.White.copy(alpha = 0.32f), shape)
    } else {
        Modifier.glassPanel(shape, elevated = true)
    }
    Row(
        modifier
            .height(height)
            .then(surfaceMod)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(
                icon,
                null,
                tint = if (primary) Color.White else Tone.Ink,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = if (primary) Color.White else Tone.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
