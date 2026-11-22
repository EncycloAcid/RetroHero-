package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.GameSystem
import com.retrohero.app.ui.theme.badgeShape

/** A system's short name inside its signature Material shape. */
@Composable
fun SystemBadge(system: GameSystem, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(system.color, system.badgeShape()),
        contentAlignment = Alignment.Center,
    ) {
        val label = system.shortName
        val fontSize = (size.value / (if (label.length > 4) 5.2f else if (label.length > 3) 4.2f else 3.3f)).sp
        Text(label, color = Color.Black.copy(alpha = 0.82f), fontWeight = FontWeight.Black, fontSize = fontSize, maxLines = 1)
    }
}
