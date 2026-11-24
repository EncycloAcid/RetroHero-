package com.retrohero.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

/** Minimal shared tokens retained for EmulatorActivity and VirtualGamepad. */
object Ascend {
    object Radius {
        val Cover = 8.dp
        val Control = 8.dp
        val Card = 10.dp
        val Hero = 12.dp
    }

    val Surface: Color get() = Tone.Surface
    val Border: Color get() = Tone.Line
    val Muted: Color get() = Tone.Muted
    val CardShape = RoundedCornerShape(Radius.Card)
    val accent: Color get() = Salmon
    val onAccent: Color get() = if (accent.luminance() > 0.35f) Color(0xFF0B0C0B) else Color.White
}
