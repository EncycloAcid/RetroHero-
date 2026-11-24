package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Tone

/** Space the bottom nav takes; scrolling screens pad their content by it. */
val LocalNavSpace = compositionLocalOf { 0.dp }

class NavItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val FloatingNavHeight = 72.dp

@Composable
fun FloatingNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dockShape = RoundedCornerShape(20.dp)
    val dark = Tone.isDark
    val dockBg = if (dark) Color(0xFF0E0E10) else Color(0xFFFFFFFF)
    val dockBorder = Tone.Line
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = if (dark) 12.dp else 6.dp,
                shape = dockShape,
                ambientColor = Color.Black.copy(alpha = if (dark) 0.70f else 0.10f),
                spotColor = Color.Black.copy(alpha = if (dark) 0.80f else 0.14f),
            )
            .clip(dockShape)
            .background(dockBg)
            .border(1.dp, dockBorder, dockShape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    if (pressed) 0.94f else 1f,
                    spring(dampingRatio = 0.7f, stiffness = 650f),
                    label = "navScale",
                )
                val tint by animateColorAsState(
                    if (active) Ascend.onAccent else Tone.Muted,
                    tween(160),
                    label = "navTint",
                )
                val pill by animateColorAsState(
                    if (active) Ascend.accent else Color.Transparent,
                    tween(180),
                    label = "navPill",
                )
                val keyShape = RoundedCornerShape(14.dp)
                Box(
                    Modifier
                        .weight(if (active) 1.48f else 0.92f)
                        .fillMaxHeight()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(keyShape)
                        .background(pill)
                        .selectable(
                            selected = active,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp),
                        ) {
                            Icon(item.selectedIcon, null, tint = tint, modifier = Modifier.size(18.dp))
                            Text(
                                item.label,
                                color = tint,
                                maxLines = 1,
                                softWrap = false,
                                style = TextStyle(
                                    fontFamily = com.retrohero.app.ui.theme.Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                ),
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Icon(item.icon, null, tint = tint, modifier = Modifier.size(19.dp))
                            Text(
                                item.label,
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontFamily = com.retrohero.app.ui.theme.Inter,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Instrument vertical navigation rail for landscape and wide tablet screens. */
@Composable
fun InstrumentNavRail(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxHeight().background(Ascend.Surface)) {
        Column(
            Modifier
                .width(76.dp)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 6.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    if (pressed) 0.94f else 1f,
                    spring(dampingRatio = 0.7f, stiffness = 650f),
                    label = "railScale",
                )
                val tint by animateColorAsState(if (active) Tone.Ink else Tone.Muted, tween(160), label = "railTint")
                val pill by animateColorAsState(
                    if (active) Tone.Raised else Color.Transparent,
                    tween(160),
                    label = "railPill",
                )
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(shape)
                        .background(pill)
                        .then(if (active) Modifier.border(1.dp, Tone.Line, shape) else Modifier)
                        .selectable(
                            selected = active,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 3.dp)
                                .size(width = 2.5.dp, height = 16.dp)
                                .background(Ascend.accent, CircleShape),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(if (active) item.selectedIcon else item.icon, null, tint = tint, modifier = Modifier.size(20.dp))
                        Text(
                            item.label.uppercase(),
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 8.sp,
                                letterSpacing = 0.04.em,
                            ),
                        )
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Page.Divider))
    }
}
