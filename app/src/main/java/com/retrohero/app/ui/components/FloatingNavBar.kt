package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Space the floating nav takes at the bottom; scrolling screens pad their content by it. */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val FloatingNavHeight = 62.dp

/**
 * Detached pill navigation bar with icon-only items. The selection pill slides between items
 * on an expressive spring.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glass: HazeState? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var pressedAny by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(if (pressedAny) 0.985f else 1f, spring(0.6f, Spring.StiffnessMedium), label = "barPress")
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(FloatingNavHeight)
            .graphicsLayer {
                scaleX = barScale
                scaleY = barScale
            }
            .shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            // Frosted chrome over the blurred app; a plain graphite fill where there's nothing to blur.
            .then(
                if (glass != null) Modifier.frostedGlass(glass, CircleShape)
                else Modifier.background(Color(0xFF131313).copy(alpha = 0.94f)),
            )
            .padding(6.dp),
    ) {
        val itemWidth: Dp = maxWidth / items.size
        val x by animateDpAsState(
            itemWidth * selected,
            spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
            label = "navPill",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.11f)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEachIndexed { i, item ->
                val active = i == selected
                val tint by animateColorAsState(if (active) Color.White else Color.White.copy(alpha = 0.7f), label = "navTint")
                val interaction = remember { MutableInteractionSource() }
                val itemPressed by interaction.collectIsPressedAsState()
                LaunchedEffect(itemPressed) { pressedAny = itemPressed }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = interaction, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (active) item.selectedIcon else item.icon, item.label, tint = tint, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
