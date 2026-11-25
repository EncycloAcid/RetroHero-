package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone

val LocalNavSpace = compositionLocalOf { 88.dp }

val FloatingNavBarHeight = 64.dp

class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Floating capsule navigation dock suspended above the bottom edge.
 * The active tab expands into an accent-lit pill with icon + label while inactive tabs stay compact.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val dark = Tone.isDark
    val dockShape = RoundedCornerShape(26.dp)
    val dockBg = if (dark) Color(0xFF14141A).copy(alpha = 0.96f) else Color(0xFFFFFFFF).copy(alpha = 0.97f)
    val dockBorderBrush = Brush.verticalGradient(
        if (dark) {
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))
        } else {
            listOf(Color(0xFFDADAE4), Color(0xFFE8E8F0))
        },
    )

    // Soft bottom fade scrim so scrolling content dissolves cleanly under the floating dock
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to Tone.Bg.copy(alpha = 0.78f),
                    1f to Tone.Bg.copy(alpha = 0.96f),
                ),
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (dark) 24.dp else 12.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = if (dark) 0.85f else 0.16f),
                    spotColor = Color.Black.copy(alpha = if (dark) 0.92f else 0.20f),
                )
                .clip(dockShape)
                .background(dockBg)
                .border(1.dp, dockBorderBrush, dockShape),
        ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(FloatingNavBarHeight)
                .padding(horizontal = 7.dp, vertical = 7.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    if (pressed) 0.92f else 1f,
                    spring(dampingRatio = 0.65f, stiffness = 620f),
                    label = "floatingNavScale",
                )
                val pillColor by animateColorAsState(
                    if (active) Ascend.accent.copy(alpha = if (dark) 0.17f else 0.14f) else Color.Transparent,
                    tween(180),
                    label = "floatingNavPill",
                )
                val fgColor by animateColorAsState(
                    if (active) Ascend.accent else Tone.Muted,
                    tween(160),
                    label = "floatingNavFg",
                )
                val itemShape = RoundedCornerShape(17.dp)

                Box(
                    Modifier
                        .weight(if (active) 1.55f else 0.92f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(itemShape)
                        .background(pillColor)
                        .then(
                            if (active) Modifier.border(1.dp, Ascend.accent.copy(alpha = 0.42f), itemShape)
                            else Modifier,
                        )
                        .selectable(
                            selected = active,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (!active) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelect(index)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = item.selectedIcon,
                                contentDescription = item.label,
                                tint = fgColor,
                                modifier = Modifier.size(19.dp),
                            )
                            Text(
                                text = item.label,
                                style = TextStyle(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.01).em,
                                ),
                                color = if (dark) Tone.Ink else fgColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.unselectedIcon,
                            contentDescription = item.label,
                            tint = fgColor,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
        }
        }
    }
}

/** Floating vertical pill rail for landscape / wide displays. */
@Composable
fun FloatingNavRail(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val dark = Tone.isDark
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(64.dp)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(if (dark) Color(0xFF141419).copy(alpha = 0.95f) else Color.White)
                .border(1.dp, Tone.Line, shape)
                .padding(vertical = 8.dp, horizontal = 7.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (active) Ascend.accent.copy(alpha = 0.18f) else Color.Transparent)
                        .then(if (active) Modifier.border(1.dp, Ascend.accent.copy(alpha = 0.45f), CircleShape) else Modifier)
                        .selectable(
                            selected = active,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (!active) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelect(index)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (active) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (active) Ascend.accent else Tone.Muted,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}
