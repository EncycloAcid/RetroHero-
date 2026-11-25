package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val LocalToolbarInset = compositionLocalOf { 104.dp }

val M3ExpressiveSpring = spring<Float>(dampingRatio = 0.56f, stiffness = 460f)
val M3SpatialSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

data class M3ToolbarDestination(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Choreographed Material 3 Expressive staggered entrance:
 * silky upward float + gentle scale settle + fade-in with natural spring physics.
 */
fun Modifier.m3StaggeredEntrance(
    order: Int,
    slideDp: Dp = 22.dp,
): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduced) {
            if (order > 0) delay((order * 38L).coerceAtMost(220L))
            progress.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 310f))
        }
    }

    val p = progress.value
    val yPx = with(density) { slideDp.toPx() } * (1f - p)
    val s = 0.955f + 0.045f * p
    this.graphicsLayer {
        alpha = p.coerceIn(0f, 1f)
        translationY = yPx
        scaleX = s
        scaleY = s
    }
}

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

/**
 * Iconic Material 3 Expressive sine-wave squiggly divider / progress track.
 * Smoothly unfurls with a gentle traveling wave phase on mount and whenever [progress] updates.
 */
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
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 180f),
        )
    }

    val p = animatedProgress.value
    val phase = p * (2.0 * PI) * 1.4

    Canvas(modifier.fillMaxWidth().height(amplitude * 2 + strokeWidth + 2.dp)) {
        val midY = size.height / 2f
        val amp = amplitude.toPx()
        val wl = wavelength.toPx().coerceAtLeast(8f)
        val stroke = strokeWidth.toPx()

        fun buildWave(startX: Float, endX: Float, wavePhase: Double): Path {
            val path = Path()
            val step = 3f
            var x = startX
            var first = true
            while (x <= endX) {
                val y = midY + sin((x / wl) * 2.0 * PI - wavePhase).toFloat() * amp
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }
            val endY = midY + sin((endX / wl) * 2.0 * PI - wavePhase).toFloat() * amp
            if (first) path.moveTo(endX, endY) else path.lineTo(endX, endY)
            return path
        }

        if (progressColor != null && p > 0f) {
            val splitX = size.width * p
            if (splitX > 2f) {
                drawPath(
                    path = buildWave(0f, splitX, phase),
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
                path = buildWave(0f, size.width, 0.0),
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Avant-garde Material 3 Expressive Circular Wavy Progress Ring:
 * Draws a sculpted sinusoidal orbital wave arc that smoothly sweeps from 0 to [progress].
 */
@Composable
fun M3CircularWavyGauge(
    progress: Float,
    activeColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 2.dp,
    lobes: Int = 10,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        anim.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = spring(dampingRatio = 0.70f, stiffness = 170f),
        )
    }
    val p = anim.value

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val amp = waveAmplitude.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = (minOf(size.width, size.height) / 2f) - stroke - amp - 1f

            // Quiet matte circular track
            drawCircle(
                color = trackColor,
                radius = baseR,
                center = Offset(cx, cy),
                style = Stroke(width = stroke),
            )

            // Sculpted wavy orbital arc
            val sweepRad = (p.coerceAtLeast(0.06f)) * 2.0 * PI
            val steps = (sweepRad * 34.0).toInt().coerceAtLeast(16)
            val path = Path()
            for (i in 0..steps) {
                val frac = i.toDouble() / steps
                val angle = -PI / 2.0 + frac * sweepRad
                val r = baseR + sin(angle * lobes + p * PI * 2.0).toFloat() * amp
                val x = cx + (cos(angle) * r).toFloat()
                val y = cy + (sin(angle) * r).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/**
 * Pure Material 3 Expressive Connected Button Group:
 * Outer ends are full pill caps (24dp), adjacent seams stay snug (8dp),
 * and the selected item morphs smoothly into a full pill (CircleShape) with bouncy spring press!
 */
@Composable
fun M3ConnectedButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    useSecondaryTone: Boolean = false,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (pressed) 0.95f else 1f,
                M3ExpressiveSpring,
                label = "m3ConnPress",
            )
            val startR by animateDpAsState(
                when {
                    selected -> 24.dp
                    idx == 0 -> 24.dp
                    else -> 8.dp
                },
                spring(dampingRatio = 0.58f, stiffness = 460f),
                label = "m3ConnStart",
            )
            val endR by animateDpAsState(
                when {
                    selected -> 24.dp
                    idx == options.lastIndex -> 24.dp
                    else -> 8.dp
                },
                spring(dampingRatio = 0.58f, stiffness = 460f),
                label = "m3ConnEnd",
            )
            val bg by animateColorAsState(
                if (selected) activeBg else unselectedContainerColor,
                tween(200),
                label = "m3ConnBg",
            )
            val fg by animateColorAsState(
                if (selected) activeFg else cs.onSurfaceVariant,
                tween(180),
                label = "m3ConnFg",
            )
            val shape = RoundedCornerShape(
                topStart = startR.coerceAtLeast(0.dp),
                bottomStart = startR.coerceAtLeast(0.dp),
                topEnd = endR.coerceAtLeast(0.dp),
                bottomEnd = endR.coerceAtLeast(0.dp),
            )

            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(shape)
                    .background(bg)
                    .selectable(
                        selected = selected,
                        interactionSource = interaction,
                        indication = null,
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
 * Bold pill primary action paired tightly across a 3dp seam with a trailing morphing icon button
 * that pops with a joyful heartbeat spring when toggled!
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
    val pScale by animateFloatAsState(if (pDown) 0.96f else 1f, M3ExpressiveSpring, label = "m3SplitP")

    val tInt = remember { MutableInteractionSource() }
    val tDown by tInt.collectIsPressedAsState()
    val tScale by animateFloatAsState(if (tDown) 0.88f else 1f, M3ExpressiveSpring, label = "m3SplitT")

    // Joyful heartbeat pop on the trailing icon when favorite/active state flips
    val heartPop = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(trailingActive) {
        if (initialized) {
            heartPop.animateTo(1.26f, spring(dampingRatio = 0.38f, stiffness = 700f))
            heartPop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
        }
        initialized = true
    }

    val trailingStartCorner by animateDpAsState(
        if (trailingActive) 28.dp else 8.dp,
        spring(dampingRatio = 0.54f, stiffness = 420f),
        label = "splitTrailCorner",
    )
    val trailingBg by animateColorAsState(
        if (trailingActive) cs.tertiaryContainer else cs.primaryContainer,
        tween(220),
        label = "splitTrailBg",
    )
    val trailingFg by animateColorAsState(
        if (trailingActive) cs.onTertiaryContainer else cs.onPrimaryContainer,
        tween(200),
        label = "splitTrailFg",
    )

    val leadingShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    val clampedTrail = trailingStartCorner.coerceAtLeast(0.dp)
    val trailingShape = RoundedCornerShape(
        topStart = clampedTrail,
        bottomStart = clampedTrail,
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
                .background(trailingBg)
                .clickable(interactionSource = tInt, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onTrailingClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingDescription,
                tint = trailingFg,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = heartPop.value
                        scaleY = heartPop.value
                    },
            )
        }
    }
}

/**
 * Pure Material 3 Expressive Floating Toolbar + Morphing Detached Expressive FAB:
 * - Tab widths smoothly spring-expand/compress with `animateFloatAsState`
 * - Active tab label expands smoothly via `AnimatedVisibility`
 * - Detached FAB smoothly morphs `Cookie7Sided ↔ Circle` and rotates `+` → `×` (45°) when Quick Add opens!
 */
@Composable
fun M3FloatingToolbarDock(
    destinations: List<M3ToolbarDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onFabClick: () -> Unit,
    isFabOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme
    val fabInt = remember { MutableInteractionSource() }
    val fabDown by fabInt.collectIsPressedAsState()
    val fabScale by animateFloatAsState(if (fabDown) 0.88f else 1f, M3ExpressiveSpring, label = "m3FabScale")

    // Morph Cookie7Sided -> Circle and rotate 45° (+ becomes ×) when Quick Add is open!
    val fabMorph = rememberMorph(MaterialShapes.Cookie7Sided, MaterialShapes.Circle)
    val fabMorphProgress by animateFloatAsState(
        if (isFabOpen) 1f else 0f,
        spring(dampingRatio = 0.56f, stiffness = 380f),
        label = "m3FabMorph",
    )
    val fabRotation by animateFloatAsState(
        if (isFabOpen) 45f else 0f,
        spring(dampingRatio = 0.52f, stiffness = 360f),
        label = "m3FabRot",
    )
    val fabBg by animateColorAsState(
        if (isFabOpen) cs.primary else cs.tertiaryContainer,
        tween(220),
        label = "m3FabBg",
    )
    val fabFg by animateColorAsState(
        if (isFabOpen) cs.onPrimary else cs.onTertiaryContainer,
        tween(200),
        label = "m3FabFg",
    )
    val fabShape = MorphShape(fabMorph, fabMorphProgress.coerceIn(0f, 1f))

    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.48f to cs.background.copy(alpha = 0.84f),
                    1f to cs.background.copy(alpha = 0.98f),
                ),
            )
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
                shadowElevation = 8.dp,
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
                        val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, M3ExpressiveSpring, label = "m3TabScale")
                        val tabWeight by animateFloatAsState(
                            if (active) 1.54f else 0.92f,
                            spring(dampingRatio = 0.64f, stiffness = 400f),
                            label = "m3TabWeight",
                        )
                        val iconPop by animateFloatAsState(
                            if (active) 1.06f else 1f,
                            spring(dampingRatio = 0.45f, stiffness = 500f),
                            label = "m3TabIconPop",
                        )
                        val pillBg by animateColorAsState(
                            if (active) cs.primaryContainer else Color.Transparent,
                            tween(220),
                            label = "m3TabBg",
                        )
                        val fg by animateColorAsState(
                            if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                            tween(180),
                            label = "m3TabFg",
                        )

                        Box(
                            Modifier
                                .weight(tabWeight)
                                .fillMaxHeight()
                                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 10.dp),
                            ) {
                                Icon(
                                    imageVector = if (active) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label,
                                    tint = fg,
                                    modifier = Modifier
                                        .size(if (active) 20.dp else 22.dp)
                                        .graphicsLayer { scaleX = iconPop; scaleY = iconPop },
                                )
                                AnimatedVisibility(
                                    visible = active,
                                    enter = expandHorizontally(spring(dampingRatio = 0.70f, stiffness = 420f)) + fadeIn(tween(160)),
                                    exit = shrinkHorizontally(tween(140)) + fadeOut(tween(100)),
                                ) {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                                        color = fg,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(start = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detached M3 Expressive Morphing Cookie7Sided <-> Circle FAB!
            Box(
                Modifier
                    .size(66.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .clip(fabShape)
                    .background(cs.background)
                    .padding(3.dp)
                    .clip(fabShape)
                    .background(fabBg)
                    .semantics { contentDescription = if (isFabOpen) "Close quick add menu" else "Quick add menu" }
                    .clickable(interactionSource = fabInt, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onFabClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add",
                    tint = fabFg,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = fabRotation },
                )
            }
        }
    }
}

/**
 * Pure Android 16 Material 3 Expressive Segmented List Tile:
 * Individual solid `surfaceContainerHigh` card with morphing outer/inner radii, organic badge, and tactile bounce.
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && onClick != null) 0.978f else 1f,
        M3ExpressiveSpring,
        label = "m3SegScale",
    )
    val badgeTilt by animateFloatAsState(
        if (pressed && onClick != null) -10f else 0f,
        M3ExpressiveSpring,
        label = "m3SegTilt",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(cs.surfaceContainerHigh)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        M3ShapeBadge(
            polygon = badgePolygon,
            containerColor = badgeContainer,
            size = 42.dp,
            modifier = Modifier.graphicsLayer { rotationZ = badgeTilt },
        ) {
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
    val monogram = remember(game.title) {
        val words = game.title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "RH"
        }
    }

    BoxWithConstraints(modifier.background(sw.bg)) {
        val compact = maxWidth < 120.dp
        val isWide = maxWidth > maxHeight * 1.15f
        val longestWord = remember(game.title) {
            game.title.split(" ", "-", ":").maxOfOrNull { it.length } ?: 6
        }
        val titleSp = when {
            maxWidth < 86.dp -> if (longestWord >= 8) 8.5f else 10f
            maxWidth < 135.dp -> if (longestWord >= 8) 11f else 12.5f
            maxWidth < 190.dp -> if (longestWord >= 9) 14f else 16f
            else -> if (longestWord >= 9) 19f else 22f
        }

        val motif = hash % 4

        // 4 distinct Bauhaus × Material 3 Expressive Collector Art Print compositions
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (motif) {
                0 -> {
                    // Bauhaus bold diagonal color block + orbital hairline
                    val diag = Path().apply {
                        moveTo(0f, h * 0.62f)
                        lineTo(w, h * 0.24f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(diag, color = sw.shapeFill.copy(alpha = 0.52f))
                    drawCircle(sw.accent.copy(alpha = 0.26f), w * 0.34f, Offset(w * 0.16f, h * 0.86f), style = Stroke(2.4f))
                    val step = (w * 0.085f).coerceIn(9f, 18f)
                    for (r in 0..3) for (c in 0..3) {
                        drawCircle(sw.accent.copy(alpha = 0.20f), 2f, Offset(w * 0.10f + c * step, h * 0.14f + r * step))
                    }
                }
                1 -> {
                    // Concentric editorial radar rings + floating pill orb
                    val center = Offset(w * 0.50f, h * 0.50f)
                    for (i in 1..3) {
                        drawCircle(sw.accent.copy(alpha = 0.22f - i * 0.04f), w * (0.20f + i * 0.13f), center, style = Stroke(2f))
                    }
                    drawCircle(sw.shapeFill.copy(alpha = 0.65f), w * 0.24f, Offset(w * 0.88f, h * 0.14f))
                }
                2 -> {
                    // Sculptural architectural arch backdrop + geometric stripes
                    drawCircle(sw.shapeFill.copy(alpha = 0.72f), w * 0.46f, Offset(w * 0.50f, h * 0.96f))
                    for (i in 0..2) {
                        val y = h * (0.14f + i * 0.09f)
                        drawLine(sw.accent.copy(alpha = 0.20f), Offset(w * 0.08f, y), Offset(w * 0.34f, y), 3f, cap = StrokeCap.Round)
                    }
                }
                else -> {
                    // Kinetic corner burst + dual duotone circles
                    drawCircle(sw.shapeFill.copy(alpha = 0.68f), w * 0.42f, Offset(w * 0.12f, h * 0.92f))
                    drawCircle(sw.accent.copy(alpha = 0.18f), w * 0.32f, Offset(w * 0.88f, h * 0.18f))
                    drawCircle(sw.accent.copy(alpha = 0.28f), w * 0.16f, Offset(w * 0.88f, h * 0.18f), style = Stroke(2.2f))
                }
            }
        }

        val emblemSize = when {
            isWide -> 112.dp
            compact -> 64.dp
            else -> 92.dp
        }
        Box(
            Modifier
                .align(if (showTitle) Alignment.TopEnd else Alignment.Center)
                .padding(
                    top = if (showTitle) (if (compact) 8.dp else 14.dp) else 0.dp,
                    end = if (showTitle) (if (compact) 8.dp else 14.dp) else 0.dp,
                )
                .size(emblemSize)
                .clip(polyShape)
                .background(sw.shapeFill),
            contentAlignment = Alignment.Center,
        ) {
            if (!showTitle) {
                Box(
                    Modifier
                        .size(emblemSize * 0.72f)
                        .clip(polyShape)
                        .background(sw.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = monogram,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isWide) 32.sp else if (compact) 18.sp else 26.sp,
                            letterSpacing = (-0.04).em,
                        ),
                        color = sw.ink,
                        maxLines = 1,
                    )
                }
            }
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
