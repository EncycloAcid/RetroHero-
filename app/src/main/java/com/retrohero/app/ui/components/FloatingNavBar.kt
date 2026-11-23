package com.retrohero.app.ui.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter

/** Space the bottom nav takes; scrolling screens pad their content by it. */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val FloatingNavHeight = 64.dp

@Composable
fun FloatingNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Ascend.Surface).windowInsetsPadding(WindowInsets.navigationBars)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Page.Divider))
        Row(Modifier.fillMaxWidth().height(FloatingNavHeight).selectableGroup()) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val tint = if (active) Ascend.accent else Ascend.Muted
                Box(Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center) {
                    if (active) Box(Modifier.align(Alignment.TopCenter).width(28.dp).height(2.dp).background(tint))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (active) item.selectedIcon else item.icon, null, tint = tint, modifier = Modifier.size(21.dp))
                        Text(item.label, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 10.sp))
                    }
                }
            }
        }
    }
}
