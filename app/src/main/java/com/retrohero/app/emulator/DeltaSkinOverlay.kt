package com.retrohero.app.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.retrohero.app.data.DeltaAction
import com.retrohero.app.data.DeltaInputs
import com.retrohero.app.data.DeltaItem
import com.retrohero.app.data.DeltaMappedInput
import com.retrohero.app.data.DeltaRect
import com.retrohero.app.data.DeltaRepresentation
import com.retrohero.app.data.DeltaSkin
import com.retrohero.app.data.DeltaSkins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Computed pixel rects for a [DeltaRepresentation] fitted inside a screen container. */
data class DeltaSkinLayout(
    val scale: Float,
    val skinRect: Rect,
    /** Where the emulator's game surface should be placed, in container pixels. */
    val screenRect: Rect,
    /** True when the skin explicitly defines screen output frames (the game fills [screenRect] directly). */
    val explicitScreen: Boolean,
) {
    fun toScreenRect(r: DeltaRect): Rect = Rect(
        left = skinRect.left + r.x * scale,
        top = skinRect.top + r.y * scale,
        right = skinRect.left + r.right * scale,
        bottom = skinRect.top + r.bottom * scale,
    )

    fun toMappingPoint(p: Offset): Offset = Offset(
        x = (p.x - skinRect.left) / scale,
        y = (p.y - skinRect.top) / scale,
    )
}

/**
 * Fits [rep] into a container of [containerW] × [containerH] pixels and determines where both
 * the skin artwork and the game screen should be placed.
 */
fun computeDeltaLayout(
    containerW: Float,
    containerH: Float,
    rep: DeltaRepresentation,
    landscape: Boolean,
): DeltaSkinLayout {
    val mapW = rep.mappingSize.width.coerceAtLeast(1f)
    val mapH = rep.mappingSize.height.coerceAtLeast(1f)
    val scale = minOf(containerW / mapW, containerH / mapH)
    val skinW = mapW * scale
    val skinH = mapH * scale
    val offsetX = (containerW - skinW) / 2f
    val explicitScreen = rep.screens.isNotEmpty()
    val offsetY = if (!explicitScreen && !landscape) {
        (containerH - skinH).coerceAtLeast(0f)
    } else {
        (containerH - skinH) / 2f
    }
    val skinRect = Rect(offsetX, offsetY, offsetX + skinW, offsetY + skinH)
    val screenRect = if (explicitScreen) {
        var minL = Float.POSITIVE_INFINITY
        var minT = Float.POSITIVE_INFINITY
        var maxR = Float.NEGATIVE_INFINITY
        var maxB = Float.NEGATIVE_INFINITY
        for (s in rep.screens) {
            val f = s.outputFrame
            minL = minOf(minL, offsetX + f.x * scale)
            minT = minOf(minT, offsetY + f.y * scale)
            maxR = maxOf(maxR, offsetX + f.right * scale)
            maxB = maxOf(maxB, offsetY + f.bottom * scale)
        }
        Rect(minL, minT, maxR, maxB)
    } else if (!landscape) {
        Rect(0f, 0f, containerW, offsetY.coerceAtLeast(1f))
    } else {
        Rect(0f, 0f, containerW, containerH)
    }
    return DeltaSkinLayout(
        scale = scale,
        skinRect = skinRect,
        screenRect = screenRect,
        explicitScreen = explicitScreen,
    )
}

/**
 * Evaluates a Delta D-pad item at mapping-point coordinate [p] using the official 3×3 grid split
 * on [item.frame] (with hit-testing against [item.hitRect]).
 */
fun dpadInputsForPoint(item: DeltaItem, p: Offset): List<String> {
    val dir = item.inputs as? DeltaInputs.Directional ?: return emptyList()
    val hit = item.hitRect
    if (p.x < hit.x || p.x > hit.right || p.y < hit.y || p.y > hit.bottom) return emptyList()
    val f = item.frame
    val leftBound = f.x + f.width / 3f
    val rightBound = f.x + f.width * 2f / 3f
    val topBound = f.y + f.height / 3f
    val bottomBound = f.y + f.height * 2f / 3f
    val out = ArrayList<String>(2)
    if (p.x < leftBound) dir.left?.let(out::add)
    else if (p.x > rightBound) dir.right?.let(out::add)
    if (p.y < topBound) dir.up?.let(out::add)
    else if (p.y > bottomBound) dir.down?.let(out::add)
    return out
}

/** Evaluates a Delta thumbstick item at mapping-point coordinate [p], returning clamped (-1..1) offset. */
fun thumbstickOffsetForPoint(item: DeltaItem, p: Offset): Offset? {
    val hit = item.hitRect
    if (p.x < hit.x || p.x > hit.right || p.y < hit.y || p.y > hit.bottom) return null
    val f = item.frame
    val cx = f.x + f.width / 2f
    val cy = f.y + f.height / 2f
    val radius = (minOf(f.width, f.height) / 2f).coerceAtLeast(1f)
    var nx = (p.x - cx) / radius
    var ny = (p.y - cy) / radius
    val dist = hypot(nx.toDouble(), ny.toDouble()).toFloat()
    if (dist > 1f) {
        nx /= dist
        ny /= dist
    }
    return Offset(nx, ny)
}

/**
 * Renders a Delta skin overlay (PNG or vector PDF artwork + optional thumbsticks + debug hitboxes)
 * and handles multi-touch input for buttons, D-pads, thumbsticks, and DS touch screens.
 */
@Composable
fun DeltaSkinOverlay(
    skin: DeltaSkin,
    representation: DeltaRepresentation,
    layout: DeltaSkinLayout,
    systemId: String,
    opacity: Float,
    onButton: (id: Int, pressed: Boolean) -> Unit,
    onAnalog: (x: Float, y: Float) -> Unit,
    onPointer: (x: Float, y: Float, down: Boolean) -> Unit,
    onAction: (action: DeltaAction, pressed: Boolean) -> Unit,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val skinW = layout.skinRect.width.roundToInt().coerceAtLeast(1)
    val skinH = layout.skinRect.height.roundToInt().coerceAtLeast(1)

    val skinBitmap by produceState<ImageBitmap?>(null, skin.dir, representation.assets, skinW, skinH) {
        value = withContext(Dispatchers.IO) {
            val assetName = representation.assets.bestAsset() ?: return@withContext null
            DeltaSkinAssets.loadBitmap(skin.assetFile(assetName), skinW, skinH)?.asImageBitmap()
        }
    }

    val thumbAssets = remember(representation) {
        representation.items.mapNotNull { it.thumbstick }.distinctBy { it.name }
    }
    val thumbBitmaps by produceState<Map<String, ImageBitmap>>(emptyMap(), skin.dir, thumbAssets, layout.scale) {
        value = withContext(Dispatchers.IO) {
            buildMap {
                for (t in thumbAssets) {
                    val tw = (t.width * layout.scale).roundToInt().coerceAtLeast(1)
                    val th = (t.height * layout.scale).roundToInt().coerceAtLeast(1)
                    DeltaSkinAssets.loadBitmap(skin.assetFile(t.name), tw, th)?.asImageBitmap()?.let {
                        put(t.name, it)
                    }
                }
            }
        }
    }

    var pressedButtons by remember(skin.identifier, representation) { mutableStateOf(emptySet<Int>()) }
    var pressedActions by remember(skin.identifier, representation) { mutableStateOf(emptySet<DeltaAction>()) }
    var thumbOffsets by remember(skin.identifier, representation) { mutableStateOf(emptyMap<Int, Offset>()) }
    var pointerActive by remember(skin.identifier, representation) { mutableStateOf(false) }

    // Detect if this is a DS skin with a split bottom touch screen so pointer Y maps to [0..1].
    val dsBottomOnly = remember(systemId, representation) {
        systemId == "nds" && representation.screens.any { s ->
            val inf = s.inputFrame
            inf != null && inf.y >= 100f && inf.height <= 220f
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(skin.identifier, representation, layout) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val nextButtons = HashSet<Int>()
                        val nextActions = HashSet<DeltaAction>()
                        val nextThumbs = HashMap<Int, Offset>()
                        var nextAnalog: Offset? = null
                        var nextPointer: Offset? = null

                        for (change in event.changes) {
                            if (!change.pressed) continue
                            val mp = layout.toMappingPoint(change.position)
                            var consumed = false

                            representation.items.forEachIndexed { idx, item ->
                                when (val inp = item.inputs) {
                                    is DeltaInputs.TouchScreen -> {
                                        val hit = item.hitRect
                                        if (mp.x >= hit.x && mp.x <= hit.right && mp.y >= hit.y && mp.y <= hit.bottom) {
                                            val u = ((mp.x - item.frame.x) / item.frame.width).coerceIn(0f, 1f)
                                            val v = ((mp.y - item.frame.y) / item.frame.height).coerceIn(0f, 1f)
                                            val px = u * 2f - 1f
                                            val py = if (dsBottomOnly) v else (v * 2f - 1f)
                                            nextPointer = Offset(px, py)
                                            consumed = true
                                        }
                                    }
                                    is DeltaInputs.Directional -> {
                                        if (item.thumbstick != null) {
                                            val off = thumbstickOffsetForPoint(item, mp)
                                            if (off != null) {
                                                nextThumbs[idx] = off
                                                val isAnalog = listOf(inp.up, inp.down, inp.left, inp.right)
                                                    .any { it?.startsWith("analogStick", ignoreCase = true) == true }
                                                if (isAnalog) nextAnalog = off
                                                if (off.x < -0.3f) inp.left?.let { addMapped(it, systemId, nextButtons, nextActions) }
                                                if (off.x > 0.3f) inp.right?.let { addMapped(it, systemId, nextButtons, nextActions) }
                                                if (off.y < -0.3f) inp.up?.let { addMapped(it, systemId, nextButtons, nextActions) }
                                                if (off.y > 0.3f) inp.down?.let { addMapped(it, systemId, nextButtons, nextActions) }
                                                consumed = true
                                            }
                                        } else {
                                            val hit = item.hitRect
                                            if (mp.x >= hit.x && mp.x <= hit.right && mp.y >= hit.y && mp.y <= hit.bottom) {
                                                for (name in dpadInputsForPoint(item, mp)) {
                                                    addMapped(name, systemId, nextButtons, nextActions)
                                                }
                                                consumed = true
                                            }
                                        }
                                    }
                                    is DeltaInputs.Buttons -> {
                                        val hit = item.hitRect
                                        if (mp.x >= hit.x && mp.x <= hit.right && mp.y >= hit.y && mp.y <= hit.bottom) {
                                            for (name in inp.ids) {
                                                addMapped(name, systemId, nextButtons, nextActions)
                                            }
                                            consumed = true
                                        }
                                    }
                                }
                            }
                            if (consumed) change.consume()
                        }

                        // Dispatch button diffs
                        if (nextButtons != pressedButtons) {
                            val newlyPressed = nextButtons - pressedButtons
                            for (id in newlyPressed) onButton(id, true)
                            for (id in pressedButtons - nextButtons) onButton(id, false)
                            if (newlyPressed.isNotEmpty()) onPress()
                            pressedButtons = nextButtons
                        }

                        // Dispatch special action diffs
                        if (nextActions != pressedActions) {
                            val newlyPressed = nextActions - pressedActions
                            for (act in newlyPressed) onAction(act, true)
                            for (act in pressedActions - nextActions) onAction(act, false)
                            if (newlyPressed.isNotEmpty()) onPress()
                            pressedActions = nextActions
                        }

                        // Dispatch thumbstick analog
                        if (nextThumbs != thumbOffsets) {
                            thumbOffsets = nextThumbs
                        }
                        val analog = nextAnalog
                        if (analog != null) {
                            onAnalog(analog.x, analog.y)
                        } else if (thumbAssets.isNotEmpty()) {
                            onAnalog(0f, 0f)
                        }

                        // Dispatch DS touch screen pointer
                        val ptr = nextPointer
                        if (ptr != null) {
                            pointerActive = true
                            onPointer(ptr.x, ptr.y, true)
                        } else if (pointerActive) {
                            pointerActive = false
                            onPointer(0f, 0f, false)
                        }
                    }
                }
            },
    ) {
        val drawAlpha = if (representation.translucent) opacity else 1f

        // Skin artwork layer
        skinBitmap?.let { bmp ->
            with(density) {
                Image(
                    bitmap = bmp,
                    contentDescription = skin.name,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(layout.skinRect.left.roundToInt(), layout.skinRect.top.roundToInt()) }
                        .size(layout.skinRect.width.toDp(), layout.skinRect.height.toDp())
                        .alpha(drawAlpha),
                )
            }
        }

        // Movable thumbstick graphics
        representation.items.forEachIndexed { idx, item ->
            val ts = item.thumbstick ?: return@forEachIndexed
            val bmp = thumbBitmaps[ts.name] ?: return@forEachIndexed
            val norm = thumbOffsets[idx] ?: Offset.Zero
            val itemRect = layout.toScreenRect(item.frame)
            val tw = ts.width * layout.scale
            val th = ts.height * layout.scale
            val maxShift = minOf(itemRect.width, itemRect.height) * 0.3f
            val left = itemRect.center.x - tw / 2f + norm.x * maxShift
            val top = itemRect.center.y - th / 2f + norm.y * maxShift
            with(density) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                        .size(tw.toDp(), th.toDp())
                        .alpha(drawAlpha),
                )
            }
        }

        // Debug hitbox overlay when info.json has "debug": true
        if (skin.debug) {
            Canvas(Modifier.fillMaxSize()) {
                for (item in representation.items) {
                    val hr = layout.toScreenRect(item.hitRect)
                    drawRect(
                        color = Color(0x33FF2D2D),
                        topLeft = hr.topLeft,
                        size = Size(hr.width, hr.height),
                    )
                    val fr = layout.toScreenRect(item.frame)
                    drawRect(
                        color = Color(0xCCFF2D2D),
                        topLeft = fr.topLeft,
                        size = Size(fr.width, fr.height),
                        style = Stroke(width = 2f),
                    )
                }
            }
        }
    }
}

private fun addMapped(
    name: String,
    systemId: String,
    buttons: MutableSet<Int>,
    actions: MutableSet<DeltaAction>,
) {
    when (val mapped = DeltaSkins.mapInput(name, systemId)) {
        is DeltaMappedInput.Button -> buttons += mapped.retroPadId
        is DeltaMappedInput.Action -> actions += mapped.action
        null -> Unit
    }
}
