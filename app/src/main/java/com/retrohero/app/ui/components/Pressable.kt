package com.retrohero.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp

/** Spring-driven press feedback (scale + corner morph) using the expressive motion scheme. */
class PressFeedback(val scale: State<Float>, val corner: State<Dp>)

@Composable
fun rememberPressFeedback(
    interaction: MutableInteractionSource,
    restCorner: Dp,
    pressedCorner: Dp,
    pressedScale: Float = 0.95f,
): PressFeedback {
    val pressed by interaction.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val dpSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    return PressFeedback(
        scale = animateFloatAsState(if (pressed) pressedScale else 1f, spec, label = "pressScale"),
        corner = animateDpAsState(if (pressed) pressedCorner else restCorner, dpSpec, label = "pressCorner"),
    )
}
