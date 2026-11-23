package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import dev.chrisbanes.haze.HazeState

/** Space the bottom nav takes; scrolling screens pad their content by it. */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

/** Height of the bar's tab row, above the system navigation inset. */
val FloatingNavHeight = 64.dp

private val BarFill = Color(0xFF151517)

/**
 * Bottom tab bar docked to the screen edge: icon over label, with the selected tab lit in the
 * accent colour, a faint tile behind it and an accent strip along its top edge.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glass: HazeState? = null,
) {
    val accent = Salmon
    Column(
        modifier
            .fillMaxWidth()
            .then(if (glass != null) Modifier.frostedGlass(glass, RectangleShape) else Modifier)
            .background(BarFill.copy(alpha = if (glass != null) 0.9f else 0.98f)),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        BoxWithConstraints(Modifier.fillMaxWidth().height(FloatingNavHeight)) {
            val itemWidth: Dp = maxWidth / items.size
            val x by animateDpAsState(
                itemWidth * selected,
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "navTile",
            )
            Box(Modifier.offset(x = x).width(itemWidth).fillMaxHeight().background(Color.White.copy(alpha = 0.04f)))
            Box(
                Modifier
                    .offset(x = x + itemWidth * 0.18f)
                    .width(itemWidth * 0.64f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(accent),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { i, item ->
                    val active = i == selected
                    val tint by animateColorAsState(if (active) accent else Color.White.copy(alpha = 0.62f), label = "navTint")
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(if (active) item.selectedIcon else item.icon, null, tint = tint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            style = TextStyle(fontFamily = Inter, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, fontSize = 11.sp),
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
