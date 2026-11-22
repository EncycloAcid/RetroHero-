package com.retrohero.app.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import com.retrohero.app.ui.theme.InterDisplay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.PadButton
import com.retrohero.app.data.PadLayout
import com.retrohero.app.data.RetroPad
import kotlin.math.atan2
import kotlin.math.hypot

private const val ID_MENU = 100
private const val ID_FAST_FORWARD = 101

private sealed interface Region {
    val rect: Rect

    data class Buttons(override val rect: Rect, val ids: List<Int>) : Region
    data class Dpad(override val rect: Rect) : Region
}

/** Shared registry: each control reports its on-screen bounds; one touch handler hit-tests them. */
private class Regions {
    val map = HashMap<String, Region>()
}

/**
 * Multi-touch on-screen gamepad. All touches are handled by a single pointer handler so a
 * finger can slide between buttons and press D-pad diagonals.
 */
@Composable
fun VirtualGamepad(
    layout: PadLayout,
    landscape: Boolean,
    opacity: Float,
    onButton: (id: Int, pressed: Boolean) -> Unit,
    onMenu: () -> Unit,
    onFastForward: (Boolean) -> Unit,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFFF4F8B),
) {
    val regions = remember { Regions() }
    var pressed by remember { mutableStateOf(emptySet<Int>()) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(layout) {
                val slop = 10.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val now = HashSet<Int>()
                        for (change in event.changes) {
                            if (!change.pressed) continue
                            val p = change.position + origin
                            for (region in regions.map.values) {
                                when (region) {
                                    is Region.Buttons -> if (region.rect.inflate(slop).contains(p)) now += region.ids
                                    is Region.Dpad -> if (region.rect.inflate(slop * 2).contains(p)) now += dpadDirections(region.rect, p)
                                }
                            }
                            change.consume()
                        }
                        if (now != pressed) {
                            val old = pressed
                            for (id in now - old) {
                                when (id) {
                                    ID_MENU -> onMenu()
                                    ID_FAST_FORWARD -> onFastForward(true)
                                    else -> onButton(id, true)
                                }
                            }
                            for (id in old - now) {
                                when (id) {
                                    ID_MENU -> Unit
                                    ID_FAST_FORWARD -> onFastForward(false)
                                    else -> onButton(id, false)
                                }
                            }
                            if ((now - old).isNotEmpty()) onPress()
                            pressed = now
                        }
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalPadAccent provides accent) {
            Box(Modifier.fillMaxSize().alpha(opacity)) {
                if (landscape) LandscapeControls(layout, regions, pressed) else PortraitControls(layout, regions, pressed)
            }
        }
    }
}

private val LocalPadAccent = staticCompositionLocalOf { Color(0xFFFF4F8B) }

// Frosted-glass look shared by every control.
private val GlassFill = Color(0xFF1A1622).copy(alpha = 0.55f)
private val GlassEdge = Color.White.copy(alpha = 0.22f)
private val GlassPressed = Color.White.copy(alpha = 0.32f)

/** Classic face-button colours, recognisable at a glance. */
private fun faceColor(label: String, accent: Color): Color = when (label) {
    "A", "○" -> Color(0xFFE5484D)
    "B" -> Color(0xFFF5B83D)
    "X", "✕" -> Color(0xFF4C7DFF)
    "Y" -> Color(0xFF30C26B)
    "△" -> Color(0xFF2FC4A2)
    "□" -> Color(0xFFE76FB8)
    else -> accent
}

@Composable
private fun pressScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "padPress",
    )
    return scale
}

@Composable
private fun PortraitControls(layout: PadLayout, regions: Regions, pressed: Set<Int>) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Shoulders(layout.leftShoulders, regions, pressed, "l")
            Shoulders(layout.rightShoulders, regions, pressed, "r")
        }
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dpad(regions, pressed, 156.dp)
            FaceButtons(layout, regions, pressed)
        }
        // Utility row: fast-forward and pause flank SELECT / START.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconControl("ff", ID_FAST_FORWARD, regions, pressed, Icons.Rounded.FastForward, "Fast forward")
            SelectStart(layout, regions, pressed, Modifier)
            IconControl("menu", ID_MENU, regions, pressed, Icons.Rounded.Pause, "Menu")
        }
    }
}

@Composable
private fun LandscapeControls(layout: PadLayout, regions: Regions, pressed: Set<Int>) {
    Box(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 14.dp)) {
        Column(Modifier.align(Alignment.TopStart)) { Shoulders(layout.leftShoulders, regions, pressed, "l") }
        Column(Modifier.align(Alignment.TopEnd)) { Shoulders(layout.rightShoulders, regions, pressed, "r") }
        Row(Modifier.align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconControl("ff", ID_FAST_FORWARD, regions, pressed, Icons.Rounded.FastForward, "Fast forward")
            IconControl("menu", ID_MENU, regions, pressed, Icons.Rounded.Pause, "Menu")
        }
        Box(Modifier.align(Alignment.CenterStart).offset(y = 24.dp)) { Dpad(regions, pressed, 146.dp) }
        Box(Modifier.align(Alignment.CenterEnd).offset(y = 24.dp)) { FaceButtons(layout, regions, pressed) }
        SelectStart(layout, regions, pressed, Modifier.align(Alignment.BottomCenter))
    }
}

/** A single rounded cross; the pressed arm lights up in the system's accent colour. */
@Composable
private fun Dpad(regions: Regions, pressed: Set<Int>, size: Dp) {
    val accent = LocalPadAccent.current
    val anyPressed = pressed.any { it in DPAD_IDS }
    val scale = pressScale(anyPressed)
    Canvas(
        Modifier
            .size(size)
            .onGloballyPositioned { regions.map["dpad"] = Region.Dpad(it.boundsInRoot()) }
            .graphicsLayer {
                scaleX = 0.96f + 0.04f * scale
                scaleY = 0.96f + 0.04f * scale
            },
    ) {
        val w = this.size.width
        val arm = w / 3f
        val r = arm * 0.32f
        val cross = Path().apply {
            addRoundRect(RoundRect(arm, 0f, arm * 2, w, CornerRadius(r)))
            addRoundRect(RoundRect(0f, arm, w, arm * 2, CornerRadius(r)))
        }
        // Soft drop shadow, glass body, rim.
        translate(top = 3.dp.toPx()) { drawPath(cross, Color.Black.copy(alpha = 0.25f)) }
        drawPath(cross, GlassFill)
        fun lit(rect: Rect, id: Int) {
            if (id in pressed) drawRoundRect(accent.copy(alpha = 0.85f), rect.topLeft, rect.size, CornerRadius(r))
        }
        lit(Rect(arm, 0f, arm * 2, arm * 1.1f), RetroPad.UP)
        lit(Rect(arm, arm * 1.9f, arm * 2, w), RetroPad.DOWN)
        lit(Rect(0f, arm, arm * 1.1f, arm * 2), RetroPad.LEFT)
        lit(Rect(arm * 1.9f, arm, w, arm * 2), RetroPad.RIGHT)
        drawPath(cross, GlassEdge, style = Stroke(1.5.dp.toPx()))
        // Centre dimple and direction arrows.
        drawCircle(Color.White.copy(alpha = 0.10f), radius = arm * 0.28f, center = center)
        val c = w / 2f
        val t = arm * 0.17f
        val e = arm * 0.38f
        val arrow = Color.White.copy(alpha = 0.6f)
        fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) =
            drawPath(Path().apply { moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); close() }, arrow)
        tri(c, e, c - t, e + t, c + t, e + t)
        tri(c, w - e, c - t, w - e - t, c + t, w - e - t)
        tri(e, c, e + t, c - t, e + t, c + t)
        tri(w - e, c, w - e - t, c - t, w - e - t, c + t)
    }
}

private val DPAD_IDS = setOf(RetroPad.UP, RetroPad.DOWN, RetroPad.LEFT, RetroPad.RIGHT)

/** 8-way D-pad: returns the directions under point [p], with a small dead zone in the centre. */
private fun dpadDirections(rect: Rect, p: Offset): List<Int> {
    val dx = p.x - rect.center.x
    val dy = p.y - rect.center.y
    if (hypot(dx, dy) < rect.width * 0.12f) return emptyList()
    val angle = Math.toDegrees(atan2(dy, dx).toDouble()).let { if (it < 0) it + 360 else it }
    val sector = ((angle + 22.5) / 45.0).toInt() % 8
    return when (sector) {
        0 -> listOf(RetroPad.RIGHT)
        1 -> listOf(RetroPad.RIGHT, RetroPad.DOWN)
        2 -> listOf(RetroPad.DOWN)
        3 -> listOf(RetroPad.DOWN, RetroPad.LEFT)
        4 -> listOf(RetroPad.LEFT)
        5 -> listOf(RetroPad.LEFT, RetroPad.UP)
        6 -> listOf(RetroPad.UP)
        else -> listOf(RetroPad.UP, RetroPad.RIGHT)
    }
}

@Composable
private fun FaceButtons(layout: PadLayout, regions: Regions, pressed: Set<Int>) {
    val diamond = layout.diamond
    if (diamond != null && diamond.size == 4) {
        val s = 60.dp
        Box(Modifier.size(s * 2.9f)) {
            RoundButton(diamond[0], regions, pressed, s, Modifier.align(Alignment.TopCenter))
            RoundButton(diamond[1], regions, pressed, s, Modifier.align(Alignment.CenterStart))
            RoundButton(diamond[2], regions, pressed, s, Modifier.align(Alignment.CenterEnd))
            RoundButton(diamond[3], regions, pressed, s, Modifier.align(Alignment.BottomCenter))
        }
        return
    }
    val s = if ((layout.rows.maxOfOrNull { it.size } ?: 0) >= 3) 56.dp else 70.dp
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        layout.rows.forEach { row ->
            // Stagger buttons diagonally like a real pad: each button sits a little higher.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                row.forEachIndexed { i, b ->
                    val lift = if (row.size == 1) 0.dp else (s / 3) * i
                    RoundButton(b, regions, pressed, s, Modifier.padding(bottom = lift))
                }
            }
        }
    }
}

@Composable
private fun RoundButton(button: PadButton, regions: Regions, pressed: Set<Int>, size: Dp, modifier: Modifier = Modifier) {
    val isPressed = button.id in pressed
    val color = faceColor(button.label, LocalPadAccent.current)
    val scale = pressScale(isPressed)
    Box(
        modifier
            .size(size)
            .onGloballyPositioned { regions.map["btn_${button.id}"] = Region.Buttons(it.boundsInRoot(), listOf(button.id)) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (isPressed) 2.dp else 8.dp, CircleShape, ambientColor = color, spotColor = color)
            .background(
                Brush.radialGradient(
                    listOf(
                        lerp(color, Color.White, if (isPressed) 0.35f else 0.18f).copy(alpha = if (isPressed) 0.95f else 0.75f),
                        color.copy(alpha = if (isPressed) 0.95f else 0.55f),
                    ),
                ),
                CircleShape,
            )
            .border(1.5.dp, Color.White.copy(alpha = if (isPressed) 0.6f else 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            button.label,
            color = Color.White,
            style = TextStyle(
                fontFamily = InterDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (button.label.length > 2) 12.sp else 22.sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 4f),
            ),
        )
    }
}

@Composable
private fun Shoulders(buttons: List<PadButton>, regions: Regions, pressed: Set<Int>, side: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val ordered = if (side == "r") buttons.reversed() else buttons
        ordered.forEach { b ->
            GlassControl(
                key = "sh_${b.id}",
                id = b.id,
                regions = regions,
                isPressed = b.id in pressed,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.width(if (buttons.size > 1) 68.dp else 84.dp).height(42.dp),
            ) { Text(b.label, color = Color.White, style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp)) }
        }
    }
}

@Composable
private fun SelectStart(layout: PadLayout, regions: Regions, pressed: Set<Int>, modifier: Modifier) {
    Row(modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        layout.select?.let { Pill(it, RetroPad.SELECT, regions, pressed) }
        layout.start?.let { Pill(it, RetroPad.START, regions, pressed) }
    }
}

@Composable
private fun Pill(label: String, id: Int, regions: Regions, pressed: Set<Int>) {
    GlassControl(
        key = "pill_$id",
        id = id,
        regions = regions,
        isPressed = id in pressed,
        shape = CircleShape,
        modifier = Modifier.width(88.dp).height(32.dp),
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.9f),
            style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp),
        )
    }
}

@Composable
private fun IconControl(key: String, id: Int, regions: Regions, pressed: Set<Int>, icon: ImageVector, description: String) {
    GlassControl(
        key = key,
        id = id,
        regions = regions,
        isPressed = id in pressed,
        shape = CircleShape,
        modifier = Modifier.size(42.dp),
    ) { Icon(icon, description, tint = Color.White) }
}

@Composable
private fun GlassControl(
    key: String,
    id: Int,
    regions: Regions,
    isPressed: Boolean,
    shape: Shape,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val accent = LocalPadAccent.current
    val scale = pressScale(isPressed)
    Box(
        modifier
            .onGloballyPositioned { regions.map[key] = Region.Buttons(it.boundsInRoot(), listOf(id)) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(if (isPressed) accent.copy(alpha = 0.75f) else GlassFill, shape)
            .border(1.5.dp, if (isPressed) GlassPressed else GlassEdge, shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}
