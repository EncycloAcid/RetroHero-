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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
    // Arcade style: a dark chunky bar on a ledge; the selected tab is a bright block that
    // slides over with a bounce, and its icon pops.
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(FloatingNavHeight + 5.dp),
    ) {
        val barH = FloatingNavHeight
        Box(Modifier.fillMaxWidth().height(barH).offset(y = 5.dp).background(Color(0xFF0B0A0E), RoundedCornerShape(26.dp)))
        Box(
            Modifier
                .fillMaxWidth()
                .height(barH)
                .background(Arcade.Panel, RoundedCornerShape(26.dp))
                .border(2.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(26.dp))
                .padding(7.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val itemWidth: Dp = maxWidth / items.size
                val x by animateDpAsState(itemWidth * selected, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "navBlock")
                val color by animateColorAsState(Arcade.tab(selected), label = "navColor")
                Box(Modifier.offset(x = x + 3.dp, y = 3.dp).width(itemWidth - 6.dp).fillMaxHeight().background(Arcade.ledge(color), RoundedCornerShape(18.dp)))
                Box(Modifier.offset(x = x + 3.dp).width(itemWidth - 6.dp).fillMaxHeight(0.92f).background(color, RoundedCornerShape(18.dp)))
                Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                    items.forEachIndexed { i, item ->
                        val active = i == selected
                        val pop = remember { Animatable(1f) }
                        LaunchedEffect(active) {
                            if (active) {
                                pop.snapTo(0.6f)
                                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                        val tint by animateColorAsState(if (active) Arcade.Ink else Color.White.copy(alpha = 0.6f), label = "navTint")
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (active) item.selectedIcon else item.icon,
                                item.label,
                                tint = tint,
                                modifier = Modifier.size(26.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                            )
                        }
                    }
                }
            }
        }
    }
}
