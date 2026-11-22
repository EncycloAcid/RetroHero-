package com.retrohero.app.ui.ios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.CupertinoMaterials

val NavBarHeight = 48.dp
val TabBarHeight = 54.dp

/**
 * Translucent top bar. Fully clear over the large title; as content scrolls under it the
 * frosted material, hairline and compact centred title fade in ([collapse] 0 → 1).
 */
@Composable
fun IosNavBar(
    hazeState: HazeState,
    title: String,
    collapse: () -> Float,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .hazeEffect(hazeState, CupertinoMaterials.regular()) { alpha = collapse() },
        )
        Column {
            Row(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .height(NavBarHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { leading() }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2f).graphicsLayer { alpha = collapse() },
                    textAlign = TextAlign.Center,
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
                }
            }
            Hairline(Modifier.graphicsLayer { alpha = collapse() })
        }
    }
}

class TabItem(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

/** Frosted tab bar; the selected icon does an expressive spring "pop". */
@Composable
fun IosTabBar(
    hazeState: HazeState,
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().hazeEffect(hazeState, CupertinoMaterials.regular())) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(TabBarHeight),
        ) {
            items.forEachIndexed { i, item ->
                val active = i == selected
                val pop = remember { Animatable(1f) }
                LaunchedEffect(active) {
                    if (active) {
                        pop.snapTo(0.82f)
                        pop.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMedium))
                    }
                }
                val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier
                        .weight(1f)
                        .height(TabBarHeight)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(7.dp))
                    Icon(
                        if (active) item.selectedIcon else item.icon,
                        item.label,
                        tint = tint,
                        modifier = Modifier.size(26.dp).graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, color = tint, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** A back button in iOS style: chevron plus the previous screen's title, tinted. */
@Composable
fun BackButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBackIos,
            "Back",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
    }
}

