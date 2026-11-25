package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.polygon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

val LocalToolbarInset = compositionLocalOf { 104.dp }

private val ExpressiveBouncySpring = spring<Float>(dampingRatio = 0.52f, stiffness = 480f)

data class M3ToolbarDestination(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Android 16 Material 3 Expressive Segmented List corner geometry:
 * outer edges are deeply rounded (24dp), inner stacked edges stay snug (6dp) with a 3dp gap.
 */
fun m3SegmentedShape(index: Int, count: Int): RoundedCornerShape {
    if (count <= 1) return RoundedCornerShape(24.dp)
    val outer = 24.dp
    val inner = 6.dp
    return when (index) {
        0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

/** Organic Material 3 Expressive polygon badge (Cookie, Clover, Sunny, SoftBurst, Flower, Gem…). */
@Composable
fun M3ShapeBadge(
    polygon: RoundedPolygon,
    containerColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = polygon.toShape()
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Iconic Material 3 Expressive sine-wave squiggly divider / accent track. */
@Composable
fun M3WavyBar(
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    progressColor: Color? = null,
    progress: Float = 0f,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 20.dp,
    strokeWidth: Dp = 3.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxWidth().height(amplitude * 2 + strokeWidth + 2.dp)) {
        val midY = size.height / 2f
        val amp = amplitude.toPx()
        val wl = wavelength.toPx().coerceAtLeast(8f)
        val stroke = strokeWidth.toPx()

        fun buildWave(startX: Float, endX: Float): Path {
            val path = Path()
            val step = 3f
            var x = startX
            var first = true
            while (x <= endX) {
                val y = midY + sin((x / wl) * 2.0 * PI).toFloat() * amp
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }
            val endY = midY + sin((endX / wl) * 2.0 * PI).toFloat() * amp
            if (first) path.moveTo(endX, endY) else path.lineTo(endX, endY)
            return path
        }

        if (progressColor != null && progress > 0f) {
            val splitX = size.width * progress.coerceIn(0f, 1f)
            if (splitX > 2f) {
                drawPath(
                    path = buildWave(0f, splitX),
                    color = progressColor,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            if (splitX < size.width - 2f) {
                drawLine(
                    color = color,
                    start = Offset(splitX + 6f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        } else {
            drawPath(
                path = buildWave(0f, size.width),
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Pure Material 3 Expressive Connected Button Group:
 * Outer ends are full pill caps (24dp), adjacent seams stay snug (8dp),
 * and the selected item morphs smoothly into a full pill (CircleShape) in `primary` / `onPrimary`.
 */
@Composable
fun M3ConnectedButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    useSecondaryTone: Boolean = false,
) {
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme
    val activeBg = if (useSecondaryTone) cs.secondaryContainer else cs.primary
    val activeFg = if (useSecondaryTone) cs.onSecondaryContainer else cs.onPrimary

    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            val startR by animateDpAsState(
                when {
                    selected -> 24.dp
                    idx == 0 -> 24.dp
                    else -> 8.dp
                },
                spring(dampingRatio = 0.60f, stiffness = 500f),
                label = "m3ConnStart",
            )
            val endR by animateDpAsState(
                when {
                    selected -> 24.dp
                    idx == options.lastIndex -> 24.dp
                    else -> 8.dp
                },
                spring(dampingRatio = 0.60f, stiffness = 500f),
                label = "m3ConnEnd",
            )
            val bg by animateColorAsState(
                if (selected) activeBg else cs.surfaceContainerHigh,
                tween(170),
                label = "m3ConnBg",
            )
            val fg by animateColorAsState(
                if (selected) activeFg else cs.onSurfaceVariant,
                tween(150),
                label = "m3ConnFg",
            )
            val shape = RoundedCornerShape(topStart = startR, bottomStart = startR, topEnd = endR, bottomEnd = endR)

            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(shape)
                    .background(bg)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = {
                            if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelect(idx)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Pure Material 3 Expressive Split Button:
 * Bold pill primary action paired tightly across a 3dp seam with a trailing tonal icon button.
 */
@Composable
fun M3SplitButton(
    label: String,
    icon: ImageVector,
    onPrimaryClick: () -> Unit,
    trailingIcon: ImageVector,
    trailingDescription: String,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    trailingActive: Boolean = false,
) {
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme

    val pInt = remember { MutableInteractionSource() }
    val pDown by pInt.collectIsPressedAsState()
    val pScale by animateFloatAsState(if (pDown) 0.96f else 1f, ExpressiveBouncySpring, label = "m3SplitP")

    val tInt = remember { MutableInteractionSource() }
    val tDown by tInt.collectIsPressedAsState()
    val tScale by animateFloatAsState(if (tDown) 0.91f else 1f, ExpressiveBouncySpring, label = "m3SplitT")

    val leadingShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    val trailingShape = RoundedCornerShape(
        topStart = if (trailingActive) 28.dp else 8.dp,
        bottomStart = if (trailingActive) 28.dp else 8.dp,
        topEnd = 28.dp,
        bottomEnd = 28.dp,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(height)
                .graphicsLayer { scaleX = pScale; scaleY = pScale }
                .clip(leadingShape)
                .background(cs.primary)
                .clickable(interactionSource = pInt, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onPrimaryClick()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = cs.onPrimary,
                maxLines = 1,
            )
        }

        Box(
            Modifier
                .size(width = 56.dp, height = height)
                .graphicsLayer { scaleX = tScale; scaleY = tScale }
                .clip(trailingShape)
                .background(if (trailingActive) cs.tertiaryContainer else cs.primaryContainer)
                .clickable(interactionSource = tInt, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onTrailingClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingDescription,
                tint = if (trailingActive) cs.onTertiaryContainer else cs.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Pure Material 3 Expressive Floating Toolbar + Detached Expressive FAB:
 * Matte tonal `surfaceContainerHighest` pill bar paired with a `tertiaryContainer` Cookie7Sided `+` FAB.
 */
@Composable
fun M3FloatingToolbarDock(
    destinations: List<M3ToolbarDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme
    val fabInt = remember { MutableInteractionSource() }
    val fabDown by fabInt.collectIsPressedAsState()
    val fabScale by animateFloatAsState(if (fabDown) 0.89f else 1f, ExpressiveBouncySpring, label = "m3Fab")
    val fabShape = MaterialShapes.Cookie7Sided.toShape()

    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 410.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Official Material 3 Expressive Horizontal Floating Toolbar
            Surface(
                shape = CircleShape,
                color = cs.surfaceContainerHighest,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    destinations.forEachIndexed { idx, dest ->
                        val active = idx == selected
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val scale by animateFloatAsState(if (pressed) 0.92f else 1f, ExpressiveBouncySpring, label = "m3TabScale")
                        val pillBg by animateColorAsState(
                            if (active) cs.primaryContainer else Color.Transparent,
                            tween(180),
                            label = "m3TabBg",
                        )
                        val fg by animateColorAsState(
                            if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                            tween(160),
                            label = "m3TabFg",
                        )

                        Box(
                            Modifier
                                .weight(if (active) 1.48f else 0.94f)
                                .fillMaxHeight()
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(CircleShape)
                                .background(pillBg)
                                .selectable(
                                    selected = active,
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (!active) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onSelect(idx)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    Icon(dest.selectedIcon, contentDescription = dest.label, tint = fg, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                                        color = fg,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            } else {
                                Icon(dest.unselectedIcon, contentDescription = dest.label, tint = fg, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // Detached M3 Expressive Cookie7Sided FAB in vibrant tertiaryContainer
            Box(
                Modifier
                    .size(62.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .clip(fabShape)
                    .background(cs.tertiaryContainer)
                    .semantics { contentDescription = "Quick add menu" }
                    .clickable(interactionSource = fabInt, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onFabClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add", tint = cs.onTertiaryContainer, modifier = Modifier.size(28.dp))
            }
        }
    }
}

/**
 * Pure Android 16 Material 3 Expressive Segmented List Tile:
 * Individual solid `surfaceContainerHigh` card with morphing outer/inner radii and organic badge.
 */
@Composable
fun M3SegmentedItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    badgePolygon: RoundedPolygon,
    badgeContainer: Color,
    badgeContent: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val shape = m3SegmentedShape(index, count)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        M3ShapeBadge(polygon = badgePolygon, containerColor = badgeContainer, size = 42.dp) {
            Icon(icon, null, tint = badgeContent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private data class M3PosterSwatch(
    val bg: Color,
    val shapeFill: Color,
    val accent: Color,
    val ink: Color,
)

private val M3PosterSwatches = listOf(
    M3PosterSwatch(Color(0xFF4F378B), Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFFF6EDFF)), // deep berry
    M3PosterSwatch(Color(0xFF25500D), Color(0xFF386A20), Color(0xFFB7F397), Color(0xFFECFFE0)), // lush sage
    M3PosterSwatch(Color(0xFF723522), Color(0xFF8F4B37), Color(0xFFFFDBD0), Color(0xFFFFF0EB)), // warm clay
    M3PosterSwatch(Color(0xFF1F4588), Color(0xFF355CA8), Color(0xFFD3E3FD), Color(0xFFEDF4FF)), // expressive cobalt
    M3PosterSwatch(Color(0xFF70334C), Color(0xFF8C4A64), Color(0xFFFFD9E3), Color(0xFFFFEEF3)), // berry rose
    M3PosterSwatch(Color(0xFF1E4E4F), Color(0xFF386667), Color(0xFFBBEBEC), Color(0xFFE6FCFD)), // teal eucalyptus
    M3PosterSwatch(Color(0xFF52461A), Color(0xFF6B5D2F), Color(0xFFF4E1A7), Color(0xFFFFF8DF)), // golden ochre
    M3PosterSwatch(Color(0xFF3D3460), Color(0xFF584B8C), Color(0xFFD8D0FF), Color(0xFFF2EEFF)), // dusk plum
)

/**
 * Loads Libretro / custom cover art with Coil, falling back to a bold flat-tonal Material 3 Expressive
 * geometric poster with the game's console polygon silhouette and clean word-safe display typography.
 */
@Composable
fun M3GameCover(
    game: Game,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    val sources = rememberArtSources(game)
    var index by remember(game.id, sources) { mutableIntStateOf(0) }
    var loaded by remember(game.id, sources) { mutableStateOf(false) }

    Box(modifier) {
        if (!loaded) {
            M3ExpressivePosterFallback(game, showTitle, Modifier.fillMaxSize())
        }
        val model = sources.getOrNull(index)
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = { if (index < sources.lastIndex) index++ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun M3ExpressivePosterFallback(
    game: Game,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    val hash = remember(game.id) { abs(game.id.hashCode()) }
    val sw = M3PosterSwatches[hash % M3PosterSwatches.size]
    val sysPoly = game.system?.polygon ?: MaterialShapes.Cookie6Sided
    val polyShape = sysPoly.toShape()

    BoxWithConstraints(modifier.background(sw.bg)) {
        val compact = maxWidth < 120.dp
        val longestWord = remember(game.title) {
            game.title.split(" ", "-", ":").maxOfOrNull { it.length } ?: 6
        }
        val titleSp = when {
            maxWidth < 86.dp -> if (longestWord >= 8) 8.5f else 10f
            maxWidth < 135.dp -> if (longestWord >= 8) 11f else 12.5f
            maxWidth < 190.dp -> if (longestWord >= 9) 14f else 16f
            else -> if (longestWord >= 9) 19f else 22f
        }

        // Bold flat Material 3 Expressive color-blocked organic polygon background motif
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (compact) 8.dp else 14.dp, end = if (compact) 8.dp else 14.dp)
                .size(if (compact) 68.dp else 124.dp)
                .clip(polyShape)
                .background(sw.shapeFill),
        )
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = sw.accent.copy(alpha = 0.16f),
                radius = size.width * 0.36f,
                center = Offset(size.width * 0.14f, size.height * 0.88f),
            )
        }

        if (showTitle) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 10.dp else 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(sw.accent)
                        .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = if (compact) 2.5.dp else 4.dp),
                ) {
                    Text(
                        text = (game.system?.shortName ?: game.systemId).uppercase(),
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Black,
                            fontSize = if (compact) 7.5.sp else 9.sp,
                            letterSpacing = 0.08.em,
                        ),
                        color = sw.bg,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                Text(
                    text = game.title,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Black,
                        fontSize = titleSp.sp,
                        lineHeight = (titleSp * 1.12f).sp,
                        letterSpacing = (-0.025).em,
                        textAlign = TextAlign.Start,
                    ),
                    color = sw.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
