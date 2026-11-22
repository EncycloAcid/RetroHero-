package com.retrohero.app.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
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
import androidx.compose.ui.draw.clip
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
import com.retrohero.app.data.Looks
import com.retrohero.app.data.PadButton
import com.retrohero.app.data.PadSkin
import com.retrohero.app.data.PadLayout
import com.retrohero.app.data.RetroPad
import kotlin.math.atan2
import kotlin.math.hypot

private const val ID_MENU = 100
private const val ID_FAST_FORWARD = 101
private const val ID_REWIND = 102

/** Editing state for moving and resizing groups of controls. */
class PadEditState(
    val adjust: Map<String, PadAdjust>,
    val selected: String?,
    val onSelect: (String) -> Unit,
    val onAdjust: (String, PadAdjust) -> Unit,
)

private val LocalPadEdit = staticCompositionLocalOf<PadEditState?> { null }
private val LocalPadAdjust = staticCompositionLocalOf<Map<String, PadAdjust>> { emptyMap() }

/** Applies a group's saved offset and size; while editing, also makes it draggable. */
@Composable
private fun Modifier.padGroup(name: String): Modifier {
    val edit = LocalPadEdit.current
    val adj = (edit?.adjust ?: LocalPadAdjust.current)[name] ?: PadAdjust()
    val density = androidx.compose.ui.platform.LocalDensity.current
    var m = this
        .offset(adj.dx.dp, adj.dy.dp)
        .graphicsLayer {
            scaleX = adj.scale
            scaleY = adj.scale
        }
    if (edit != null) {
        val selected = edit.selected == name
        val latest = androidx.compose.runtime.rememberUpdatedState(adj)
        m = m
            .border(
                if (selected) 2.dp else 1.dp,
                Color.White.copy(alpha = if (selected) 0.9f else 0.35f),
                RoundedCornerShape(18.dp),
            )
            .background(Color.White.copy(alpha = if (selected) 0.12f else 0.04f), RoundedCornerShape(18.dp))
            .pointerInput(name) {
                detectDragGestures(
                    onDragStart = { edit.onSelect(name) },
                ) { change, drag ->
                    change.consume()
                    val cur = latest.value
                    // Drag happens in the scaled layer, so scale it back to screen space.
                    with(density) {
                        edit.onAdjust(name, cur.copy(dx = cur.dx + (drag.x * cur.scale).toDp().value, dy = cur.dy + (drag.y * cur.scale).toDp().value))
                    }
                }
            }
            .pointerInput("tap_$name") { detectTapGestures { edit.onSelect(name) } }
    }
    return m
}

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
    skin: PadSkin = Looks.Console,
    onRewind: ((Boolean) -> Unit)? = null,
    systemId: String = "",
    adjust: Map<String, PadAdjust> = emptyMap(),
    edit: PadEditState? = null,
) {
    val regions = remember { Regions() }
    var pressed by remember { mutableStateOf(emptySet<Int>()) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(layout, edit != null) {
                if (edit != null) return@pointerInput
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
                                    ID_REWIND -> onRewind?.invoke(true)
                                    else -> onButton(id, true)
                                }
                            }
                            for (id in old - now) {
                                when (id) {
                                    ID_MENU -> Unit
                                    ID_FAST_FORWARD -> onFastForward(false)
                                    ID_REWIND -> onRewind?.invoke(false)
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
        CompositionLocalProvider(
            LocalPadAccent provides accent,
            LocalPadSkin provides skin,
            LocalConsole provides (if (skin.faces == "console") ConsoleStyles.forSystem(systemId, accent) else null),
            LocalPadEdit provides edit,
            LocalPadAdjust provides adjust,
            LocalShowRewind provides (onRewind != null),
        ) {
            // The console skin in portrait is a solid controller under the game, so it ignores opacity.
            val solid = skin.faces == "console" && !landscape
            Box(Modifier.fillMaxSize().alpha(if (solid) 1f else if (edit != null) maxOf(opacity, 0.85f) else opacity)) {
                if (landscape) LandscapeControls(layout, regions, pressed) else PortraitControls(layout, regions, pressed)
            }
        }
    }
}

private val LocalPadAccent = staticCompositionLocalOf { Color(0xFFFF4F8B) }
private val LocalShowRewind = staticCompositionLocalOf { false }

/** Fast-forward, plus rewind beside it when rewind is on. */
@Composable
private fun SpeedControls(regions: Regions, pressed: Set<Int>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (LocalShowRewind.current) {
            IconControl("rw", ID_REWIND, regions, pressed, Icons.Rounded.FastRewind, "Rewind")
        }
        IconControl("ff", ID_FAST_FORWARD, regions, pressed, Icons.Rounded.FastForward, "Fast forward")
    }
}

// Frosted-glass look shared by every control.
private val LocalPadSkin = staticCompositionLocalOf { Looks.Console }
private val LocalConsole = staticCompositionLocalOf<ConsoleStyle?> { null }

/** The controller body behind the buttons: a plate, plus the NES's black inset panel. */
@Composable
private fun ControllerPlate(style: ConsoleStyle) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(Brush.verticalGradient(listOf(style.plateTop, style.plateBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)),
    ) {
        if (style.band != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 56.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(style.band, lerp(style.band, Color.Black, 0.4f))))
                    .border(2.dp, Color.Black.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            )
        }
    }
}

// The current skin's colours.
private val GlassFill: Color @Composable get() = LocalPadSkin.current.fill
private val GlassEdge: Color @Composable get() = LocalPadSkin.current.edge
private val GlassPressed = Color.White.copy(alpha = 0.32f)
private val PadLabel: Color @Composable get() = LocalPadSkin.current.label

/** Face-button colour for the current skin: classic colours, the body colour, or the accent. */
@Composable
private fun skinFace(label: String, accent: Color): Color {
    val skin = LocalPadSkin.current
    return when (skin.faces) {
        "mono" -> skin.pressed ?: skin.edge
        "accent" -> skin.edge
        else -> faceColor(label, accent)
    }
}

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
    LocalConsole.current?.let { ControllerPlate(it) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padGroup("l")) { Shoulders(layout.leftShoulders, regions, pressed, "l") }
            Box(Modifier.padGroup("r")) { Shoulders(layout.rightShoulders, regions, pressed, "r") }
        }
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.padGroup("dpad")) { Dpad(regions, pressed, 156.dp) }
            Box(Modifier.padGroup("face")) { FaceButtons(layout, regions, pressed) }
        }
        // Utility row: speed controls and pause flank SELECT / START.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SpeedControls(regions, pressed, Modifier.padGroup("speed"))
            SelectStart(layout, regions, pressed, Modifier.padGroup("selstart"))
            Box(Modifier.padGroup("menu")) { IconControl("menu", ID_MENU, regions, pressed, Icons.Rounded.Pause, "Menu") }
        }
    }
}

@Composable
private fun LandscapeControls(layout: PadLayout, regions: Regions, pressed: Set<Int>) {
    Box(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 14.dp)) {
        Column(Modifier.align(Alignment.TopStart).padGroup("l")) { Shoulders(layout.leftShoulders, regions, pressed, "l") }
        Column(Modifier.align(Alignment.TopEnd).padGroup("r")) { Shoulders(layout.rightShoulders, regions, pressed, "r") }
        Row(Modifier.align(Alignment.TopCenter).padGroup("speed"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeedControls(regions, pressed)
            IconControl("menu", ID_MENU, regions, pressed, Icons.Rounded.Pause, "Menu")
        }
        Box(Modifier.align(Alignment.CenterStart).offset(y = 24.dp).padGroup("dpad")) { Dpad(regions, pressed, 146.dp) }
        Box(Modifier.align(Alignment.CenterEnd).offset(y = 24.dp).padGroup("face")) { FaceButtons(layout, regions, pressed) }
        SelectStart(layout, regions, pressed, Modifier.align(Alignment.BottomCenter).padGroup("selstart"))
    }
}

/** A single rounded cross; the pressed arm lights up in the system's accent colour. */
@Composable
private fun Dpad(regions: Regions, pressed: Set<Int>, size: Dp) {
    val accent = LocalPadAccent.current
    val anyPressed = pressed.any { it in DPAD_IDS }
    val scale = pressScale(anyPressed)
    val fill = GlassFill
    val edge = GlassEdge
    val label = PadLabel
    val glow = LocalPadSkin.current.glow
    val console = LocalConsole.current
    if (console != null) {
        ConsoleDpad(console, regions, pressed, size, scale)
        return
    }
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
        drawPath(cross, fill)
        fun lit(rect: Rect, id: Int) {
            if (id in pressed) drawRoundRect(accent.copy(alpha = 0.85f), rect.topLeft, rect.size, CornerRadius(r))
        }
        lit(Rect(arm, 0f, arm * 2, arm * 1.1f), RetroPad.UP)
        lit(Rect(arm, arm * 1.9f, arm * 2, w), RetroPad.DOWN)
        lit(Rect(0f, arm, arm * 1.1f, arm * 2), RetroPad.LEFT)
        lit(Rect(arm * 1.9f, arm, w, arm * 2), RetroPad.RIGHT)
        if (glow > 0f) drawPath(cross, edge.copy(alpha = 0.25f * glow), style = Stroke(7.dp.toPx()))
        drawPath(cross, edge, style = Stroke(1.5.dp.toPx()))
        // Centre dimple and direction arrows.
        drawCircle(Color.White.copy(alpha = 0.10f), radius = arm * 0.28f, center = center)
        val c = w / 2f
        val t = arm * 0.17f
        val e = arm * 0.38f
        val arrow = label.copy(alpha = label.alpha * 0.65f)
        fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) =
            drawPath(Path().apply { moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); close() }, arrow)
        tri(c, e, c - t, e + t, c + t, e + t)
        tri(c, w - e, c - t, w - e - t, c + t, w - e - t)
        tri(e, c, e + t, c - t, e + t, c + t)
        tri(w - e, c, w - e - t, c - t, w - e - t, c + t)
    }
}

/** A moulded plastic cross: lit from above, dark rim, the held arm pressed in. */
@Composable
private fun ConsoleDpad(style: ConsoleStyle, regions: Regions, pressed: Set<Int>, size: Dp, scale: Float) {
    Canvas(
        Modifier
            .size(size)
            .onGloballyPositioned { regions.map["dpad"] = Region.Dpad(it.boundsInRoot()) }
            .graphicsLayer {
                scaleX = 0.97f + 0.03f * scale
                scaleY = 0.97f + 0.03f * scale
            },
    ) {
        val w = this.size.width
        val arm = w / 3f
        val r = arm * 0.16f
        val cross = Path().apply {
            addRoundRect(RoundRect(arm, 0f, arm * 2, w, CornerRadius(r)))
            addRoundRect(RoundRect(0f, arm, w, arm * 2, CornerRadius(r)))
        }
        // Recess in the body, drop shadow, then the cross itself.
        translate(top = 4.dp.toPx()) { drawPath(cross, Color.Black.copy(alpha = 0.45f)) }
        drawPath(cross, Brush.verticalGradient(listOf(lerp(style.dpad, Color.White, 0.18f), style.dpad, lerp(style.dpad, Color.Black, 0.35f))))
        fun held(rect: Rect, id: Int) {
            if (id in pressed) drawRoundRect(Color.Black.copy(alpha = 0.35f), rect.topLeft, rect.size, CornerRadius(r))
        }
        held(Rect(arm, 0f, arm * 2, arm * 1.1f), RetroPad.UP)
        held(Rect(arm, arm * 1.9f, arm * 2, w), RetroPad.DOWN)
        held(Rect(0f, arm, arm * 1.1f, arm * 2), RetroPad.LEFT)
        held(Rect(arm * 1.9f, arm, w, arm * 2), RetroPad.RIGHT)
        drawPath(cross, lerp(style.dpad, Color.Black, 0.55f), style = Stroke(1.5.dp.toPx()))
        // Shallow dimple in the middle and moulded arrows.
        drawCircle(Brush.radialGradient(listOf(lerp(style.dpad, Color.Black, 0.25f), style.dpad), center = center, radius = arm * 0.34f), arm * 0.34f, center)
        val c = w / 2f
        val t = arm * 0.16f
        val e = arm * 0.36f
        fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) =
            drawPath(Path().apply { moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); close() }, style.arrow)
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
    LocalConsole.current?.let {
        ConsoleButton(it, button, regions, button.id in pressed, size, modifier)
        return
    }
    val isPressed = button.id in pressed
    val color = skinFace(button.label, LocalPadAccent.current)
    val labelColor = if (LocalPadSkin.current.faces == "color") Color.White else PadLabel
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
            color = labelColor,
            style = TextStyle(
                fontFamily = InterDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (button.label.length > 2) 12.sp else 22.sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 4f),
            ),
        )
    }
}

/** A convex plastic button with a highlight; the letter printed on it or below it, like the real pad. */
@Composable
private fun ConsoleButton(style: ConsoleStyle, button: PadButton, regions: Regions, isPressed: Boolean, size: Dp, modifier: Modifier) {
    val color = style.face(button.label)
    val scale = pressScale(isPressed)
    Box(modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { regions.map["btn_${button.id}"] = Region.Buttons(it.boundsInRoot(), listOf(button.id)) }
                .graphicsLayer {
                    scaleX = 0.94f + 0.06f * scale
                    scaleY = 0.94f + 0.06f * scale
                    translationY = (1f - scale) * 18f
                }
                .shadow(if (isPressed) 1.dp else 6.dp, CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            lerp(color, Color.White, if (isPressed) 0.05f else 0.22f),
                            color,
                            lerp(color, Color.Black, 0.38f),
                        ),
                        center = Offset.Unspecified,
                        radius = Float.POSITIVE_INFINITY,
                    ),
                    CircleShape,
                )
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (isPressed) 0.05f else 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.25f))), CircleShape)
                .border(1.5.dp, lerp(color, Color.Black, 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val onButton = style.faceLabel(button.label)
            if (onButton != null) {
                val symbol = button.label in setOf("△", "○", "✕", "□")
                Text(
                    button.label,
                    color = onButton,
                    style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.ExtraBold, fontSize = if (symbol) 28.sp else if (button.label.length > 2) 12.sp else 21.sp),
                )
            }
        }
        if (style.labelBelow != null) {
            Text(
                button.label,
                color = style.labelBelow,
                style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 24.dp),
            )
        }
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
                kind = "shoulder",
            ) { Text(b.label, color = LocalConsole.current?.shoulderLabel ?: PadLabel, style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp)) }
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
    val console = LocalConsole.current
    if (console != null) {
        // A rubber pill with its name printed on the body beside it.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlassControl(
                key = "pill_$id",
                id = id,
                regions = regions,
                isPressed = id in pressed,
                shape = CircleShape,
                modifier = Modifier.graphicsLayer { rotationZ = console.pillTilt }.width(64.dp).height(20.dp),
                kind = "pill",
            ) {}
            Text(
                label,
                color = console.pillLabel,
                style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.2.sp),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        return
    }
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
            color = PadLabel.copy(alpha = 0.9f),
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
        kind = "icon",
    ) { Icon(icon, description, tint = LocalConsole.current?.iconTint ?: PadLabel) }
}

@Composable
private fun GlassControl(
    key: String,
    id: Int,
    regions: Regions,
    isPressed: Boolean,
    shape: Shape,
    modifier: Modifier,
    kind: String = "",
    content: @Composable () -> Unit,
) {
    val accent = LocalPadAccent.current
    val skin = LocalPadSkin.current
    val scale = pressScale(isPressed)
    val console = LocalConsole.current
    if (console != null) {
        val base = when (kind) {
            "shoulder" -> console.shoulder
            "pill" -> console.pill
            else -> console.iconFill
        }
        Box(
            modifier
                .onGloballyPositioned { regions.map[key] = Region.Buttons(it.boundsInRoot(), listOf(id)) }
                .graphicsLayer {
                    scaleX = 0.95f + 0.05f * scale
                    scaleY = 0.95f + 0.05f * scale
                }
                .shadow(if (isPressed) 1.dp else 4.dp, shape)
                .background(Brush.verticalGradient(listOf(lerp(base, Color.White, if (isPressed) 0.02f else 0.16f), lerp(base, Color.Black, if (isPressed) 0.3f else 0.15f))), shape)
                .border(1.dp, lerp(base, Color.Black, 0.45f), shape),
            contentAlignment = Alignment.Center,
        ) { content() }
        return
    }
    Box(
        modifier
            .onGloballyPositioned { regions.map[key] = Region.Buttons(it.boundsInRoot(), listOf(id)) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(if (isPressed) (skin.pressed ?: accent.copy(alpha = 0.75f)) else GlassFill, shape)
            .border(1.5.dp, if (isPressed) GlassPressed else GlassEdge, shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}
