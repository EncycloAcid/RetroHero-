package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.GameSystem
import com.retrohero.app.ui.theme.InterDisplay
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.tag
import com.retrohero.app.ui.theme.badgeShape

/** A system's short name inside its signature Material shape. */
@Composable
fun SystemBadge(system: GameSystem, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(system.color, system.badgeShape()),
        contentAlignment = Alignment.Center,
    ) {
        val label = system.tag
        // Pixel font for short tags; longer names fall back to the display face so they fit.
        val style = if (label.length <= 3) {
            TextStyle(fontFamily = PixelFont, fontSize = (size.value * 0.58f / 3f).sp)
        } else {
            TextStyle(
                fontFamily = InterDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.95f / label.length).coerceAtMost(size.value / 3.4f).sp,
                letterSpacing = (-0.3).sp,
            )
        }
        Text(label, style = style, color = Color.Black.copy(alpha = 0.8f), maxLines = 1)
    }
}
