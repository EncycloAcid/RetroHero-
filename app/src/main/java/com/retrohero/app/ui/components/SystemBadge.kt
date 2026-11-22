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
import com.retrohero.app.ui.theme.Bricolage
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.badgeShape

/** A system's short name inside its signature Material shape. */
@Composable
fun SystemBadge(system: GameSystem, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(system.color, system.badgeShape()),
        contentAlignment = Alignment.Center,
    ) {
        val label = system.shortName.uppercase()
        // Pixel font for short tags; longer names fall back to the display face so they fit.
        val style = if (label.length <= 4) {
            TextStyle(fontFamily = PixelFont, fontSize = (size.value * 0.5f / label.length.coerceAtLeast(3)).sp)
        } else {
            TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = (size.value / 5f).sp)
        }
        Text(label, style = style, color = Color.Black.copy(alpha = 0.8f), maxLines = 1)
    }
}
