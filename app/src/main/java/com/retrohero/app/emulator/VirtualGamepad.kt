package com.retrohero.app.emulator

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Menu
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
        Box(Modifier.fillMaxSize().alpha(opacity)) {
            if (landscape) LandscapeControls(layout, regions, pressed) else PortraitControls(layout, regions, pressed)
        }
    }
}

@Composable
private fun PortraitControls(layout: PadLayout, regions: Regions, pressed: Set<Int>) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Shoulders(layout.leftShoulders, regions, pressed, "l")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconControl("ff", ID_FAST_FORWARD, regions, pressed) { Icon(Icons.Default.FastForward, "Fast forward", tint = Color.White) }
                IconControl("menu", ID_MENU, regions, pressed) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
            }
            Shoulders(layout.rightShoulders, regions, pressed, "r")
        }
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dpad(regions, pressed, 150.dp)
            FaceButtons(layout, regions, pressed)
        }
        SelectStart(layout, regions, pressed, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun LandscapeControls(layout: PadLayout, regions: Regions, pressed: Set<Int>) {
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Column(Modifier.align(Alignment.TopStart)) { Shoulders(layout.leftShoulders, regions, pressed, "l") }
        Column(Modifier.align(Alignment.TopEnd)) { Shoulders(layout.rightShoulders, regions, pressed, "r") }
        Row(Modifier.align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconControl("ff", ID_FAST_FORWARD, regions, pressed) { Icon(Icons.Default.FastForward, "Fast forward", tint = Color.White) }
            IconControl("menu", ID_MENU, regions, pressed) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
        }
        Box(Modifier.align(Alignment.CenterStart).offset(y = 24.dp)) { Dpad(regions, pressed, 140.dp) }
        Box(Modifier.align(Alignment.CenterEnd).offset(y = 24.dp)) { FaceButtons(layout, regions, pressed) }
        SelectStart(layout, regions, pressed, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Dpad(regions: Regions, pressed: Set<Int>, size: Dp) {
    Canvas(
        Modifier
            .size(size)
            .onGloballyPositioned { regions.map["dpad"] = Region.Dpad(it.boundsInRoot()) },
    ) {
        val arm = this.size.width / 3f
        val base = Color.White.copy(alpha = 0.25f)
        val active = Color.White.copy(alpha = 0.7f)
        fun armRect(x: Float, y: Float, id: Int) = drawRoundRect(
            color = if (id in pressed) active else base,
            topLeft = Offset(x, y),
            size = Size(arm, arm),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(arm * 0.2f),
        )
        armRect(arm, 0f, RetroPad.UP)
        armRect(arm, arm * 2, RetroPad.DOWN)
        armRect(0f, arm, RetroPad.LEFT)
        armRect(arm * 2, arm, RetroPad.RIGHT)
        drawRect(base, topLeft = Offset(arm, arm), size = Size(arm, arm))
        // Direction arrows.
        val c = this.size.width / 2f
        val t = arm * 0.22f
        val arrow = Color.Black.copy(alpha = 0.45f)
        fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) =
            drawPath(Path().apply { moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); close() }, arrow)
        tri(c, arm * 0.3f, c - t, arm * 0.3f + t, c + t, arm * 0.3f + t)
        tri(c, this.size.height - arm * 0.3f, c - t, this.size.height - arm * 0.3f - t, c + t, this.size.height - arm * 0.3f - t)
        tri(arm * 0.3f, c, arm * 0.3f + t, c - t, arm * 0.3f + t, c + t)
        tri(this.size.width - arm * 0.3f, c, this.size.width - arm * 0.3f - t, c - t, this.size.width - arm * 0.3f - t, c + t)
    }
}

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
        val s = 58.dp
        Box(Modifier.size(s * 3)) {
            RoundButton(diamond[0], regions, pressed, s, Modifier.align(Alignment.TopCenter))
            RoundButton(diamond[1], regions, pressed, s, Modifier.align(Alignment.CenterStart))
            RoundButton(diamond[2], regions, pressed, s, Modifier.align(Alignment.CenterEnd))
            RoundButton(diamond[3], regions, pressed, s, Modifier.align(Alignment.BottomCenter))
        }
        return
    }
    val s = if ((layout.rows.maxOfOrNull { it.size } ?: 0) >= 3) 54.dp else 66.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        layout.rows.forEach { row ->
            // Stagger buttons diagonally like a real pad: each button sits a little higher.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
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
    ControlSurface(
        key = "btn_${button.id}",
        ids = listOf(button.id),
        regions = regions,
        isPressed = button.id in pressed,
        shape = CircleShape,
        modifier = modifier.size(size),
    ) {
        Text(button.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (button.label.length > 2) 12.sp else 20.sp)
    }
}

@Composable
private fun Shoulders(buttons: List<PadButton>, regions: Regions, pressed: Set<Int>, side: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val ordered = if (side == "r") buttons.reversed() else buttons
        ordered.forEach { b ->
            ControlSurface(
                key = "sh_${b.id}",
                ids = listOf(b.id),
                regions = regions,
                isPressed = b.id in pressed,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(76.dp).height(40.dp),
            ) { Text(b.label, color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SelectStart(layout: PadLayout, regions: Regions, pressed: Set<Int>, modifier: Modifier) {
    Row(modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        layout.select?.let { Pill(it, RetroPad.SELECT, regions, pressed) }
        layout.start?.let { Pill(it, RetroPad.START, regions, pressed) }
    }
}

@Composable
private fun Pill(label: String, id: Int, regions: Regions, pressed: Set<Int>) {
    ControlSurface(
        key = "pill_$id",
        ids = listOf(id),
        regions = regions,
        isPressed = id in pressed,
        shape = RoundedCornerShape(50),
        modifier = Modifier.width(84.dp).height(30.dp),
    ) { Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun IconControl(key: String, id: Int, regions: Regions, pressed: Set<Int>, content: @Composable () -> Unit) {
    ControlSurface(
        key = key,
        ids = listOf(id),
        regions = regions,
        isPressed = id in pressed,
        shape = CircleShape,
        modifier = Modifier.size(40.dp),
        content = content,
    )
}

@Composable
private fun ControlSurface(
    key: String,
    ids: List<Int>,
    regions: Regions,
    isPressed: Boolean,
    shape: Shape,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .onGloballyPositioned { regions.map[key] = Region.Buttons(it.boundsInRoot(), ids) }
            .background(if (isPressed) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f), shape)
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}
