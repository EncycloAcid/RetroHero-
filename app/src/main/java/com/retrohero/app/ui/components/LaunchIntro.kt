package com.retrohero.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.retrohero.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/*
 * Launch intro: the system splash shows the white shield on black; the app takes over with the
 * same mark, flies it into Home's logo and lets Home rise in underneath, piece by piece.
 */

private const val FlightMillis = 660
private val MoveEasing = CubicBezierEasing(0.55f, 0f, 0.1f, 1f)
private val ScaleEasing = CubicBezierEasing(0.45f, 0f, 0.1f, 1f)
private const val RiseDelayMillis = 80
private const val RiseMillis = 520
private const val RiseStaggerMillis = 50
private const val RiseOrders = 4
private const val RiseTotalMillis = RiseMillis + (RiseOrders - 1) * RiseStaggerMillis
private val RiseEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val RiseDistance = 56.dp
/** The system splash draws an icon without a background at 288dp; the mark starts at that size. */
private val SplashIconSize = 288.dp

@Stable
class LaunchIntroState(val plays: Boolean) {
    internal val flight = Animatable(if (plays) 0f else 1f)
    internal val rise = Animatable(if (plays) 0f else 1f)
    internal var markBounds by mutableStateOf<Rect?>(null)
    internal var landed by mutableStateOf(!plays)
    var done by mutableStateOf(!plays)
        internal set

    internal fun riseAt(order: Int): Float {
        val millis = rise.value * RiseTotalMillis - order * RiseStaggerMillis
        return RiseEasing.transform((millis / RiseMillis).coerceIn(0f, 1f))
    }
}

val LocalLaunchIntro = staticCompositionLocalOf<LaunchIntroState?> { null }

/** A piece of Home that rises into place during the intro; [order] staggers it after the pieces above. */
fun Modifier.launchRise(order: Int): Modifier = composed {
    val state = LocalLaunchIntro.current
    if (state == null || state.done) this
    else graphicsLayer {
        val e = state.riseAt(order)
        alpha = e
        translationY = (1f - e) * RiseDistance.toPx()
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/** Home's logo: where the flying mark lands. Hidden until the mark gets there. */
fun Modifier.launchMarkTarget(): Modifier = composed {
    val state = LocalLaunchIntro.current
    if (state == null || state.done) this
    else this
        .onGloballyPositioned { state.markBounds = it.boundsInRoot() }
        .graphicsLayer { alpha = if (state.landed) 1f else 0f }
}

@Composable
fun LaunchIntroOverlay(state: LaunchIntroState) {
    if (state.done) return
    val mark = painterResource(R.drawable.ic_launcher_monochrome)
    LaunchedEffect(state) {
        // Wait for Home's logo (or give up and land in place), then let its first heavy frame draw.
        withTimeoutOrNull(2_500) { snapshotFlow { state.markBounds != null }.first { it } }
        withFrameNanos { }
        withFrameNanos { }
        delay(120)
        coroutineScope {
            launch { state.rise.animateTo(1f, tween(RiseTotalMillis, RiseDelayMillis, LinearEasing)) }
            state.flight.animateTo(1f, tween(FlightMillis, easing = LinearEasing))
            state.landed = true
        }
        state.rise.snapTo(1f)
        state.done = true
    }
    Canvas(
        Modifier
            .fillMaxSize()
            // swallow touches until the intro is over
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            },
    ) {
        val f = state.flight.value
        val backdrop = 1f - smoothstep(0.18f, 0.7f, f)
        if (backdrop > 0f) drawRect(Color.Black.copy(alpha = backdrop))
        if (state.landed) return@Canvas
        val startSize = SplashIconSize.toPx()
        val start = Rect(Offset(size.width / 2f - startSize / 2f, size.height / 2f - startSize / 2f), Size(startSize, startSize))
        val target = state.markBounds ?: Rect(start.center - Offset(startSize / 8f, startSize / 8f), Size(startSize / 4f, startSize / 4f))
        val m = MoveEasing.transform(f)
        val s = ScaleEasing.transform(f)
        val side = start.width + (target.width - start.width) * s
        val cx = start.center.x + (target.center.x - start.center.x) * m
        val cy = start.center.y + (target.center.y - start.center.y) * m
        translate(cx - side / 2f, cy - side / 2f) {
            with(mark) { draw(Size(side, side), colorFilter = ColorFilter.tint(Color.White)) }
        }
    }
}

private fun smoothstep(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
