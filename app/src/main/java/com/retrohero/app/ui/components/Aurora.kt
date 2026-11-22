package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max

/**
 * Soft, slowly drifting colour glows behind the app. [accent] eases between values, so the
 * glow follows whatever system or game is in focus.
 */
@Composable
fun AuroraBackground(accent: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val glow = animateColorAsState(accent, tween(1200), label = "auroraAccent")
    val drift = rememberInfiniteTransition(label = "aurora").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "auroraDrift",
    )
    val background = scheme.background
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary
    Box(
        modifier.drawBehind {
            drawRect(background)
            val t = drift.value
            val w = size.width
            val h = size.height
            blob(glow.value.copy(alpha = 0.30f), Offset(w * (0.10f + 0.25f * t), h * 0.05f), w * 0.95f)
            blob(secondary.copy(alpha = 0.20f), Offset(w * (1.0f - 0.3f * t), h * (0.22f + 0.05f * t)), w * 0.85f)
            blob(tertiary.copy(alpha = 0.10f), Offset(w * 0.4f, h * (0.75f - 0.1f * t)), w * 0.9f)
        },
    ) { content() }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    val r = max(radius, 1f)
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = r), radius = r, center = center)
}
