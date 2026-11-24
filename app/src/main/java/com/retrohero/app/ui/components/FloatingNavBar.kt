package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone

/**
 * Space reserved at the bottom of scrolling screens. With the Handheld Console OS layout there is
 * no bottom tab bar any more, so this is only the gesture inset plus the mini player when shown.
 */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

/** Height of the top Console OS bar (below the status bar). */
val FloatingNavHeight = 52.dp

/**
 * The Handheld Console OS bar (Switch / Steam Deck): docked at the top under the status bar with
 * text tabs and a glowing accent underline on the active section, plus a console-style clock and
 * battery readout on the right. Kept under its old name so existing call sites keep compiling.
 */
@Composable
fun FloatingNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    ConsoleTopBar(items, selected, onSelect, modifier)
}

@Composable
fun ConsoleTopBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Tone.Bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(FloatingNavHeight)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.fillMaxHeight().selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    ConsoleTab(item, active = index == selected, onClick = { onSelect(index) })
                }
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(0.dp)), contentAlignment = Alignment.CenterEnd) { ConsoleStatus() }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.Line))
    }
}

@Composable
private fun ConsoleTab(item: NavItem, active: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (active) Tone.Ink else Tone.Muted, tween(160), label = "tabTint")
    val bar by animateDpAsState(if (active) 18.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 600f), label = "tabBar")
    Box(
        Modifier
            .fillMaxHeight()
            .selectable(
                selected = active,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (active) {
                Icon(item.selectedIcon, null, tint = com.retrohero.app.ui.components.Ascend.accent, modifier = Modifier.size(16.dp))
            }
            Text(
                item.label,
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    letterSpacing = (-0.005).em,
                ),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .width(bar)
                .height(3.dp)
                .shadow(if (active) 6.dp else 0.dp, CircleShape, ambientColor = Ascend.accent, spotColor = Ascend.accent)
                .clip(CircleShape)
                .background(Ascend.accent),
        )
    }
}

/** Console-OS status cluster: clock plus a tiny battery glyph. */
@Composable
private fun ConsoleStatus() {
    val time = remember {
        java.text.SimpleDateFormat("H:mm", java.util.Locale.getDefault()).format(java.util.Date())
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            time,
            color = Tone.Ink,
            maxLines = 1,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 20.dp, height = 10.dp)
                    .border(1.25.dp, Tone.Muted, RoundedCornerShape(3.dp))
                    .padding(2.dp),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(0.72f).clip(RoundedCornerShape(1.dp)).background(Tone.Ink))
            }
            Box(Modifier.padding(start = 1.dp).size(width = 2.dp, height = 4.dp).background(Tone.Muted, RoundedCornerShape(1.dp)))
        }
    }
}

/** Kept for API compatibility; wide screens now also use the top Console OS bar. */
@Composable
fun InstrumentNavRail(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    ConsoleTopBar(items, selected, onSelect, modifier)
}
