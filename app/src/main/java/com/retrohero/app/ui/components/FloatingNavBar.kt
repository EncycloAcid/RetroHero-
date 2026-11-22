package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
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

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val FloatingNavHeight = 68.dp

/**
 * Detached pill navigation bar with icon-only items. The selection pill slides between items
 * on an expressive spring.
 */
@Composable
fun FloatingNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(FloatingNavHeight)
            .shadow(18.dp, CircleShape)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.96f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
            .padding(8.dp),
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
                .background(scheme.onSurface.copy(alpha = 0.12f)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEachIndexed { i, item ->
                val active = i == selected
                val tint by animateColorAsState(if (active) scheme.onSurface else scheme.onSurfaceVariant, label = "navTint")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (active) item.selectedIcon else item.icon, item.label, tint = tint, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
