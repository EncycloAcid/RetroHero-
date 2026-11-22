package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.GameSystem
import com.retrohero.app.ui.ios.SquircleShape
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.tag

/** App-icon-like squircle for a system: gradient in its colour with its short name. */
@Composable
fun SystemTile(system: GameSystem, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(SquircleShape(size * 0.24f))
            .background(Brush.linearGradient(listOf(lerp(system.color, Color.White, 0.2f), lerp(system.color, Color.Black, 0.25f)))),
        contentAlignment = Alignment.Center,
    ) {
        val label = system.tag
        val style = if (label.length <= 3) {
            TextStyle(fontFamily = PixelFont, fontSize = (size.value * 0.6f / 3f).sp)
        } else {
            TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.92f / label.length).coerceAtMost(size.value / 3.2f).sp,
                letterSpacing = (-0.2).sp,
            )
        }
        Text(label, style = style, color = Color.White, maxLines = 1)
    }
}
