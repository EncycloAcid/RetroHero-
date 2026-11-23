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

fun Modifier.press3D(maxTilt: Float = 7f, pressedScale: Float = 0.97f): Modifier = composed {
    if (LocalReducedMotion.current) return@composed this
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    var normalized by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed } ?: continue
                    val bounds = this.size
                    if (bounds.width > 0 && bounds.height > 0) {
                        normalized = Offset(
                            ((down.position.x / bounds.width) * 2f - 1f).coerceIn(-1f, 1f),
                            ((down.position.y / bounds.height) * 2f - 1f).coerceIn(-1f, 1f),
                        )
                        size = bounds
                    }
                    scope.launch { press.animateTo(1f, TiltSpring) }
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) break
                    }
                    scope.launch { press.animateTo(0f, TiltSpring) }
                }
            }
        }
        .graphicsLayer {
            val t = press.value
            if (t > 0f) {
                val s = 1f - (1f - pressedScale) * t
                scaleX = s
                scaleY = s
                rotationX = -normalized.y * maxTilt * t
                rotationY = normalized.x * maxTilt * t
                cameraDistance = (size.width.takeIf { it > 0 }?.toFloat() ?: 1000f) * 3f
                transformOrigin = TransformOrigin.Center
            }
        }
}

/** Monochrome switch: an outlined track when off, grey track and white thumb when on. */
@Composable
fun MonoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val alpha = if (enabled) 1f else 0.38f
    val track by animateColorAsState(if (checked) Tone.Ink.copy(alpha = 0.24f * alpha) else Color.Transparent, tween(150), label = "track")
    val border by animateColorAsState(if (checked) Color.Transparent else Tone.Line.copy(alpha = alpha), tween(150), label = "border")
    val thumb by animateColorAsState(if (checked) Tone.Ink.copy(alpha = alpha) else Tone.Raised.copy(alpha = alpha), tween(150), label = "thumb")
    val x by animateDpAsState(if (checked) 52.dp - 24.dp - 4.dp else 4.dp, spring(dampingRatio = 0.7f, stiffness = 700f), label = "thumbX")
    Box(
        modifier
            .size(52.dp, 32.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
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
                tint = if (checked) Tone.Bg else Tone.Ink,
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
