package com.retrohero.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.retrohero.app.ui.theme.Tone
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** The app content behind floating chrome; screens draw into it, bars blur it. */
val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

/** Frosted chrome over a heavy blur, with a little grain against banding. */
val FrostStyle: HazeStyle
    get() = HazeStyle(
    backgroundColor = Tone.Bg,
    tint = HazeTint(Tone.Surface.copy(alpha = 0.78f)),
    blurRadius = 28.dp,
    noiseFactor = 0.035f,
)

/**
 * Draws [state]'s content blurred behind this element, with a 2 px hairline just inside [shape].
 * [visibility] fades the whole glass (e.g. a top bar that only frosts once content scrolls under it).
 */
fun Modifier.frostedGlass(state: HazeState?, shape: Shape, visibility: () -> Float = { 1f }): Modifier {
    if (state == null) return this
    return this
        .hazeEffect(state, FrostStyle) { alpha = visibility().coerceIn(0f, 1f) }
        .drawWithContent {
            drawContent()
            val v = visibility().coerceIn(0f, 1f)
            if (v <= 0f) return@drawWithContent
            val outline = shape.createOutline(size, layoutDirection, this)
            val inset = 1f
            val path = when (outline) {
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                is Outline.Generic -> outline.path
            }
            drawPath(path, Tone.Ink.copy(alpha = 0.05f * v), style = Stroke(width = 2f + inset))
        }
}
