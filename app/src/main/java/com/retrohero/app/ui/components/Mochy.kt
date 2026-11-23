package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.retrohero.app.ui.theme.Tone
import kotlinx.coroutines.launch

/*
 * Small pieces of the dark monochrome design language: the 3D press, the grey switch and the
 * corner glow that hangs from the top of a page.
 */

private val TiltSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f)

/**
 * Press feedback that tips the surface toward the finger (up to [maxTilt] degrees) while it
 * shrinks a touch. Observes touches without consuming them, so clicks still work.
 */
/** True when the person asked for less motion (Settings → Gameplay & comfort). */
val LocalReducedMotion = androidx.compose.runtime.staticCompositionLocalOf { false }

fun Modifier.press3D(maxTilt: Float = 7f, pressedScale: Float = 0.97f): Modifier = this

/** Tactile Instrument switch: recessed track when off, Ember accent track and crisp thumb when on. */
@Composable
fun MonoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val alpha = if (enabled) 1f else 0.38f
    val view = androidx.compose.ui.platform.LocalView.current
    val track by animateColorAsState(if (checked) Ascend.accent.copy(alpha = alpha) else Tone.Bg.copy(alpha = alpha), tween(160), label = "track")
    val border by animateColorAsState(if (checked) Color.Transparent else Tone.Line.copy(alpha = alpha), tween(160), label = "border")
    val thumb by animateColorAsState(if (checked) Ascend.onAccent.copy(alpha = alpha) else Tone.Muted.copy(alpha = 0.55f * alpha), tween(160), label = "thumb")
    val x by animateDpAsState(if (checked) 52.dp - 24.dp - 4.dp else 4.dp, spring(dampingRatio = 0.7f, stiffness = 700f), label = "thumbX")
    Box(
        modifier
            .size(52.dp, 32.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    onCheckedChange(it)
                },
            )
            .clip(CircleShape)
            .background(track)
            .border(1.dp, border, CircleShape),
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = x).size(24.dp).background(thumb, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                null,
                tint = if (checked) Ascend.accent else Tone.Surface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Two soft oval glows hanging from the top corners (left from [left], right from [right]),
 * eased to nothing so there's no edge. [visibility] fades it, e.g. as the page scrolls.
 */
fun Modifier.topGlow(left: Color, right: Color, depth: Dp, visibility: () -> Float): Modifier = drawBehind {
    val v = visibility().coerceIn(0f, 1f)
    if (v <= 0f) return@drawBehind
    val h = depth.toPx()
    val w = size.width
    fun glow(color: Color, cx: Float) {
        val stops = (0..12).map { i ->
            val r = 1f - i / 12f
            val falloff = r * r * (3f - 2f * r)
            (i / 12f) to color.copy(alpha = 0.9f * falloff * v)
        }.toTypedArray()
        // an oval: draw a circle of radius h, stretched sideways around its centre
        scale(scaleX = w * 0.95f / h, scaleY = 1f, pivot = Offset(cx, 0f)) {
            drawRect(Brush.radialGradient(*stops, center = Offset(cx, 0f), radius = h), topLeft = Offset(cx - h, 0f), size = androidx.compose.ui.geometry.Size(h * 2, h))
        }
    }
    glow(left, w * 0.08f)
    glow(right, w * 0.92f)
}
