package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import dev.chrisbanes.haze.HazeState

/** Space the bottom nav takes; scrolling screens pad their content by it. */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

/** Height of the floating dock. */
val FloatingNavHeight = 60.dp

/** Gap between the dock and the bottom of the screen (above the system navigation inset). */
val FloatingNavGap = 12.dp

private val DockFill = Color(0xFF18181B)
private val ItemSize = 44.dp

/**
 * A floating dock: a rounded glass pill of icon buttons. The selected tab sits on a filled
 * accent tile that slides between items.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glass: HazeState? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    val gap = 4.dp
    Box(
        modifier
            .padding(bottom = FloatingNavGap)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(FloatingNavHeight)
            .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .then(if (glass != null) Modifier.frostedGlass(glass, shape) else Modifier.clip(shape))
            .background(DockFill.copy(alpha = if (glass != null) 0.86f else 0.97f))
            .border(1.dp, Ascend.accent.copy(alpha = 0.22f), shape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val x by animateDpAsState(
            (ItemSize + gap) * selected,
            spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "dockTile",
        )
        Box(Modifier.offset(x = x).size(ItemSize).clip(RoundedCornerShape(14.dp)).background(Salmon))
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, item ->
                val active = i == selected
                val tint by animateColorAsState(if (active) Ascend.onAccent else Color.White.copy(alpha = 0.6f), label = "dockTint")
                Box(
                    Modifier
                        .size(ItemSize)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (active) item.selectedIcon else item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
