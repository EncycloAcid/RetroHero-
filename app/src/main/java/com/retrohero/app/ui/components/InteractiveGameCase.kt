package com.retrohero.app.ui.components

import android.text.format.DateUtils
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ShelfBucket(val id: String, val label: String) {
    Playing("playing", "Currently Playing"),
    Completed("completed", "Completed"),
    Backlog("backlog", "Backlog"),
}

/**
 * Interactive 3D Physical Game Case stage using authentic console-specific packaging
 * (CD Jewel Case, DVD/GameCube/DS Keep Case, PSP UMD Case, Sega Clamshell, SNES/N64 Landscape Box,
 * and GBA/GB/NES Retail Cardboard Box).
 */
@Composable
fun InteractiveGameCaseModal(
    game: Game,
    stat: PlayStat?,
    isFavorite: Boolean,
    shelfBucket: ShelfBucket,
    saveSlots: List<Pair<Int, Long>>,
    currentCore: CoreInfo?,
    autoOpenOnMount: Boolean = false,
    onSetShelfBucket: (ShelfBucket) -> Unit,
    onToggleFavorite: () -> Unit,
    onPlay: (Int?) -> Unit,
    onSelectCore: (CoreInfo) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val kind = remember(game.systemId) { packagingFor(game.systemId) }
    val shellColor = remember(game.systemId) { caseShellColor(game.systemId) }
    val ambientGlow = remember(game.id) { posterAccentColor(game) }

    // 0f = snapped shut, 1f = swung wide open (-134 deg)
    val lid = remember(game.id) { Animatable(if (autoOpenOnMount) 1f else 0f) }
    val eject = remember(game.id) { Animatable(0f) }
    var tiltX by remember(game.id) { mutableFloatStateOf(5f) }
    var tiltY by remember(game.id) { mutableFloatStateOf(-15f) }

    LaunchedEffect(game.id) {
        if (!autoOpenOnMount) {
            delay(150)
            lid.animateTo(0.08f, tween(230, easing = FastOutSlowInEasing))
            lid.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 380f))
        }
    }

    val spin by rememberInfiniteTransition(label = "discSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "discDeg",
    )

    fun toggleCase() {
        val target = if (lid.value < 0.5f) 1f else 0f
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        scope.launch {
            lid.animateTo(target, spring(dampingRatio = 0.72f, stiffness = 210f))
        }
    }

    fun ejectAndLaunch(slot: Int? = null) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        scope.launch {
            if (lid.value < 0.75f) {
                lid.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 280f))
            }
            eject.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 340f))
            onPlay(slot)
            eject.snapTo(0f)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070709).copy(alpha = 0.96f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ambientGlow.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.33f),
                    radius = size.width * 0.80f,
                ),
                radius = size.width * 0.80f,
                center = Offset(size.width * 0.5f, size.height * 0.33f),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: Close · Open/Close Case · Favourite
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Tone.Raised)
                        .border(1.dp, Tone.Line, CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close case", tint = Tone.Ink, modifier = Modifier.size(18.dp))
                }

                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Tone.Raised)
                        .border(1.dp, Tone.Line, CircleShape)
                        .clickable { toggleCase() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = if (lid.value > 0.5f) "Close case" else "Open case",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = Tone.Ink,
                    )
                }

                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Tone.Raised)
                        .border(1.dp, Tone.Line, CircleShape)
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favourite" else "Add favourite",
                        tint = if (isFavorite) Ascend.accent else Tone.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- Authentic 3D Physical Case Stage -----------------------------------------------
            val caseW = kind.closedWidth
            val caseH = kind.closedHeight
            val stageScale = 1f - (1f - kind.openScale) * lid.value
            val shiftPx = with(density) { (caseW * 0.46f * stageScale).toPx() } * lid.value
            val cam = 22f * density.density
            val caseShape = RoundedCornerShape(kind.cornerRadius)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(258.dp)
                    .semantics { contentDescription = "3D game case for ${game.title}" }
                    .pointerInput(game.id) {
                        var crossedHalf = lid.value > 0.5f
                        detectDragGestures(
                            onDragEnd = {
                                val snap = if (lid.value > 0.36f) 1f else 0f
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                scope.launch {
                                    lid.animateTo(snap, spring(dampingRatio = 0.72f, stiffness = 230f))
                                }
                                tiltX = 5f
                                tiltY = -15f
                            },
                        ) { change, drag ->
                            change.consume()
                            val nextLid = (lid.value - drag.x / 230f).coerceIn(0f, 1f)
                            if ((nextLid > 0.5f) != crossedHalf) {
                                crossedHalf = nextLid > 0.5f
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            scope.launch { lid.snapTo(nextLid) }
                            tiltX = (tiltX - drag.y * 0.06f).coerceIn(-12f, 14f)
                            tiltY = (-15f + (1f - nextLid) * (drag.x * 0.08f)).coerceIn(-24f, 16f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Wide elliptical ground contact shadow that expands smoothly as the case opens
                val shadowW = caseW + 30.dp + (120.dp * lid.value)
                Canvas(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width(shadowW)
                        .height(26.dp),
                ) {
                    val rx = size.width * 0.50f
                    drawOval(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.72f),
                                0.52f to Color.Black.copy(alpha = 0.36f),
                                1f to Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = rx,
                        ),
                    )
                }

                val effectiveRotY = tiltY * (1f - lid.value)
                val effectiveRotX = tiltX * (1f - lid.value * 0.5f)

                // Subtle 3D extruded left spine depth visible when the closed box is angled
                if (lid.value < 0.15f && abs(effectiveRotY) > 3f) {
                    val depthBase = when (kind) {
                        PackagingKind.JewelCase -> 10.dp
                        PackagingKind.LandscapeCardboard,
                        PackagingKind.PortraitCardboard -> 20.dp
                        PackagingKind.KeepCaseDs,
                        PackagingKind.SegaClamshell -> 18.dp
                        else -> 14.dp
                    }
                    val projSpine = depthBase * abs(sin(Math.toRadians(effectiveRotY.toDouble()))).toFloat().coerceAtLeast(0.28f)
                    val halfCasePx = with(density) { (caseW * stageScale / 2f).toPx() }
                    val sideSign = if (effectiveRotY < 0f) -1 else 1
                    Box(
                        Modifier
                            .width(projSpine)
                            .height(caseH * stageScale * 0.98f)
                            .offset { IntOffset((sideSign * halfCasePx).roundToInt(), 0) }
                            .graphicsLayer {
                                rotationX = effectiveRotX
                                cameraDistance = cam
                            }
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        shellColor.copy(alpha = 0.95f),
                                        Color.Black.copy(alpha = 0.65f),
                                    ),
                                ),
                            )
                            .border(0.6.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(2.dp)),
                    )
                }

                // 1. Right / Back Authentic Media Tray (Jewel CD rosette, PS2/GC DVD tray, UMD, DS card, Clamshell, or Die-cut Cardboard Tray)
                RealisticMediaTray(
                    game = game,
                    spinDegrees = spin * lid.value,
                    ejectProgress = eject.value,
                    enabled = lid.value > 0.55f,
                    onEjectAndPlay = { ejectAndLaunch(null) },
                    modifier = Modifier
                        .size(caseW, caseH)
                        .offset { IntOffset(shiftPx.roundToInt(), 0) }
                        .graphicsLayer {
                            scaleX = stageScale
                            scaleY = stageScale
                            cameraDistance = cam
                            rotationY = effectiveRotY
                            rotationX = effectiveRotX
                        }
                        .shadow(20.dp, caseShape),
                )

                // 2. Hinged Front Cover Door (pivots around left spine TransformOrigin(0f, 0.5f))
                val doorAngle = effectiveRotY - 134f * lid.value
                val showingInside = doorAngle < -90f

                Box(
                    Modifier
                        .size(caseW, caseH)
                        .offset { IntOffset(shiftPx.roundToInt(), 0) }
                        .graphicsLayer {
                            scaleX = stageScale
                            scaleY = stageScale
                            cameraDistance = cam
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = doorAngle
                            rotationX = effectiveRotX
                        }
                        .shadow(if (showingInside) 12.dp else 22.dp, caseShape)
                        .clip(caseShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { toggleCase() },
                ) {
                    if (showingInside) {
                        RealisticInsideLid(
                            game = game,
                            stat = stat,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { scaleX = -1f },
                        )
                    } else {
                        RealisticCaseFront(
                            game = game,
                            sheenOffset = lid.value * 210f + (tiltY + 15f) * 4f,
                            compact = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            //Packaging-aware interactive gesture cue
            Text(
                text = if (lid.value > 0.6f) {
                    "Tap ${kind.mediaNoun} to insert & play · Drag case to close"
                } else {
                    "Tap or drag cover to open ${packagingDisplayName(kind)}"
                },
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                color = Tone.Muted,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
            )

            // Title & System Metadata
            Text(
                text = game.title,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 27.sp,
                    letterSpacing = (-0.025).em,
                    textAlign = TextAlign.Center,
                ),
                color = Tone.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val metaText = buildString {
                append(game.system?.name ?: game.systemId.uppercase())
                if (stat != null && stat.seconds > 0) {
                    append(" · ")
                    append(PlayTime.format(stat.seconds))
                }
            }
            Text(
                text = metaText,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                color = Tone.Muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            // Primary Play / Resume CTA with warm glow & subtle top highlight
            val ctaShape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = ctaShape,
                        ambientColor = Ascend.accent.copy(alpha = 0.55f),
                        spotColor = Ascend.accent.copy(alpha = 0.65f),
                    )
                    .clip(ctaShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Ascend.accent, Ascend.accent.copy(alpha = 0.88f)),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                        ),
                        shape = ctaShape,
                    )
                    .clickable { ejectAndLaunch(null) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Ascend.onAccent, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (saveSlots.any { it.first == AppPaths.AUTO_SLOT }) "Resume game" else "Play game",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = Ascend.onAccent,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Shelf Bucket Selector
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Shelf",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = Tone.Muted,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShelfBucket.entries.forEach { bucket ->
                        val selected = bucket == shelfBucket
                        val shortLabel = when (bucket) {
                            ShelfBucket.Playing -> "Playing"
                            ShelfBucket.Completed -> "Completed"
                            ShelfBucket.Backlog -> "Backlog"
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) Ascend.accent.copy(alpha = 0.18f) else Tone.Raised)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Ascend.accent else Tone.Line,
                                    shape = RoundedCornerShape(9.dp),
                                )
                                .clickable { onSetShelfBucket(bucket) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = shortLabel,
                                style = TextStyle(
                                    fontFamily = Inter,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.5.sp,
                                ),
                                color = if (selected) Ascend.accent else Tone.Ink,
                            )
                        }
                    }
                }
            }

            // Save States
            if (saveSlots.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Save states",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = Tone.Muted,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp),
                    ) {
                        items(saveSlots, key = { it.first }) { (slot, ts) ->
                            val label = if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot"
                            val rel = DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                            Column(
                                Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Tone.Raised)
                                    .border(1.dp, Tone.Line, RoundedCornerShape(9.dp))
                                    .clickable { ejectAndLaunch(slot) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(label, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp), color = Tone.Ink)
                                Text(rel.toString(), style = TextStyle(fontFamily = Inter, fontSize = 11.sp), color = Tone.Muted)
                            }
                        }
                    }
                }
            }

            // Core Selector
            val system = game.system
            if (system != null && system.cores.size > 1) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Emulator engine",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = Tone.Muted,
                    )
                    system.cores.forEach { core ->
                        val active = core.id == currentCore?.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Tone.Raised)
                                .clickable { onSelectCore(core) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(core.displayName, style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = Tone.Ink)
                            if (active) Icon(Icons.Rounded.Check, null, tint = Ascend.accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (onDelete != null) {
                TextButton(
                    onClick = { onClose(); onDelete() },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Remove from device", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                }
            }
        }
    }
}

private fun packagingDisplayName(kind: PackagingKind): String = when (kind) {
    PackagingKind.JewelCase -> "jewel case"
    PackagingKind.KeepCaseDvd,
    PackagingKind.KeepCaseMiniDisc,
    PackagingKind.KeepCaseDs -> "keep case"
    PackagingKind.PspUmdCase -> "PSP case"
    PackagingKind.SegaClamshell -> "clamshell case"
    PackagingKind.LandscapeCardboard,
    PackagingKind.PortraitCardboard -> "cardboard box"
}
