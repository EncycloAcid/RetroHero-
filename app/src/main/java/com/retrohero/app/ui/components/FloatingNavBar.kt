package com.retrohero.app.ui.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Tone

/** Space the bottom nav takes; scrolling screens pad their content by it. */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val FloatingNavHeight = 66.dp

@Composable
fun FloatingNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Ascend.Surface).windowInsetsPadding(WindowInsets.navigationBars)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Page.Divider))
        BoxWithConstraints(Modifier.fillMaxWidth().height(FloatingNavHeight).padding(horizontal = 6.dp)) {
            val itemWidth = maxWidth / items.size.coerceAtLeast(1)
            val clamped = selected.coerceIn(0, items.lastIndex.coerceAtLeast(0))
            val indicatorX by animateDpAsState(
                (itemWidth.value * clamped + (itemWidth.value - 5f) / 2f).dp,
                spring(dampingRatio = 0.72f, stiffness = 620f),
                label = "dockIndicator",
            )
            Box(
                Modifier
                    .offset(x = indicatorX, y = 6.dp)
                    .size(5.dp)
                    .background(Ascend.accent, CircleShape),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
                items.forEachIndexed { index, item ->
                    val active = index == selected
                    val tint by animateColorAsState(if (active) Tone.Ink else Tone.Muted, tween(160), label = "navTint")
                    val pill by animateColorAsState(
                        if (active) Tone.Ink.copy(alpha = if (Tone.isDark) 0.10f else 0.06f) else Color.Transparent,
                        tween(160),
                        label = "navPill",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pill)
                            .selectable(active, role = Role.Tab, onClick = { onSelect(index) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(if (active) item.selectedIcon else item.icon, null, tint = tint, modifier = Modifier.size(21.dp))
                            Text(
                                item.label.uppercase(),
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.05.em,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
