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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.roundToInt

private val DiscSystems = setOf(
    "psx", "ps2", "psp", "gc", "wii", "dc", "dreamcast", "saturn", "segacd", "pcecd", "neocd", "3do", "fds",
)

enum class ShelfBucket(val id: String, val label: String) {
    Playing("playing", "Currently Playing"),
    Completed("completed", "Completed"),
    Backlog("backlog", "Backlog"),
}

/**
 * Interactive 3D Physical Game Case stage:
 * - Closed case tilts in 3D with a dynamic plastic-sleeve glare.
 * - Tap the cover or drag horizontally across the case to unlatch & swing the front door open (-132°),
 *   revealing the printed instruction booklet on the left inside clips and the spinning optical disc
 *   or sculpted ROM cartridge nestled in the molded right tray.
 * - Tap the disc/cartridge inside the open tray to eject it upward with a haptic snap and launch the game!
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
    val isDisc = game.systemId in DiscSystems
    val ambientGlow = remember(game.id) { posterAccentColor(game) }

    // 0f = snapped shut, 1f = swung wide open (-132 deg)
    val lid = remember(game.id) { Animatable(if (autoOpenOnMount) 1f else 0f) }
    val eject = remember(game.id) { Animatable(0f) }
    var tiltX by remember(game.id) { mutableFloatStateOf(4f) }
    var tiltY by remember(game.id) { mutableFloatStateOf(-12f) }

    // Subtle entrance settle
    LaunchedEffect(game.id) {
        if (!autoOpenOnMount) {
            delay(160)
            // Tiny tactile peek wiggle so the user immediately feels it's a 3D hinged case
            lid.animateTo(0.08f, tween(240, easing = FastOutSlowInEasing))
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
        // Ambient cover-tinted spotlight behind the physical case
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ambientGlow.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    radius = size.width * 0.78f,
                ),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.5f, size.height * 0.34f),
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
            // Top bar: Close & Case Open/Close toggle + Favourite heart
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

            Spacer(Modifier.height(18.dp))

            // ---- Interactive 3D Physical Case Stage --------------------------------------------
            val caseW = 160.dp
            val caseH = 228.dp
            val shiftPx = with(density) { (caseW * 0.47f).toPx() } * lid.value
            val cam = 22f * density.density

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(266.dp)
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
                                tiltX = 4f
                                tiltY = -12f
                            },
                        ) { change, drag ->
                            change.consume()
                            // Dragging left swings the cover open; dragging right closes it; vertical drag tilts in 3D!
                            val nextLid = (lid.value - drag.x / 230f).coerceIn(0f, 1f)
                            if ((nextLid > 0.5f) != crossedHalf) {
                                crossedHalf = nextLid > 0.5f
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            scope.launch { lid.snapTo(nextLid) }
                            tiltX = (tiltX - drag.y * 0.06f).coerceIn(-12f, 14f)
                            tiltY = (-12f + (1f - nextLid) * (drag.x * 0.08f)).coerceIn(-22f, 16f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Soft floor shadow underneath the case
                Canvas(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width(if (lid.value > 0.3f) 280.dp else 170.dp)
                        .height(22.dp),
                ) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                        ),
                    )
                }

                // 1. Right / Back Molded Media Tray (shifts right as lid opens so both halves stay centered)
                val effectiveRotY = tiltY * (1f - lid.value)
                val effectiveRotX = tiltX * (1f - lid.value * 0.5f)

                Box(
                    Modifier
                        .size(caseW, caseH)
                        .offset { IntOffset(shiftPx.roundToInt(), 0) }
                        .graphicsLayer {
                            cameraDistance = cam
                            rotationY = effectiveRotY
                            rotationX = effectiveRotX
                        }
                        .shadow(20.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1D1D24), Color(0xFF121217)),
                            ),
                        )
                        .border(2.dp, Color(0xFF2B2B35), RoundedCornerShape(10.dp)),
                ) {
                    // Plastic clasp lip along right edge
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(5.dp)
                            .fillMaxHeight(0.75f)
                            .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                            .background(Color(0xFF292933)),
                    )

                    // Inner recessed media well + interactive Disc or Cartridge!
                    if (isDisc) {
                        DiscTrayInterior(
                            game = game,
                            spinDegrees = spin * lid.value,
                            ejectProgress = eject.value,
                            enabled = lid.value > 0.55f,
                            onEjectAndPlay = { ejectAndLaunch(null) },
                        )
                    } else {
                        CartridgeTrayInterior(
                            game = game,
                            ejectProgress = eject.value,
                            enabled = lid.value > 0.55f,
                            onEjectAndPlay = { ejectAndLaunch(null) },
                        )
                    }
                }

                // 2. Hinged Front Cover Door (pivots around left spine TransformOrigin(0f, 0.5f))
                val doorAngle = effectiveRotY - 134f * lid.value
                val showingInside = doorAngle < -90f

                Box(
                    Modifier
                        .size(caseW, caseH)
                        .offset { IntOffset(shiftPx.roundToInt(), 0) }
                        .graphicsLayer {
                            cameraDistance = cam
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = doorAngle
                            rotationX = effectiveRotX
                        }
                        .shadow(if (showingInside) 12.dp else 22.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { toggleCase() },
                ) {
                    if (showingInside) {
                        // Mirror horizontally so the inside cover & manual booklet read left-to-right!
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { scaleX = -1f }
                                .background(Color(0xFF17171D))
                                .border(2.dp, Color(0xFF2A2A34), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                        ) {
                            InsideBookletInsert(game = game, stat = stat)
                        }
                    } else {
                        // Closed / opening exterior front cover with spine & glossy plastic sheen
                        Box(Modifier.fillMaxSize()) {
                            CoverArt(game, Modifier.fillMaxSize())

                            // Left plastic hinge spine ridge
                            Box(
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .width(9.dp)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.White.copy(alpha = 0.14f),
                                                Color.Black.copy(alpha = 0.35f),
                                            ),
                                        ),
                                    ),
                            )

                            // Dynamic clear-sleeve specular light sweep
                            val sheenShift = lid.value * 220f
                            Canvas(Modifier.fillMaxSize()) {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.16f),
                                            Color.Transparent,
                                        ),
                                        start = Offset(-60f + sheenShift, 0f),
                                        end = Offset(size.width * 0.65f + sheenShift, size.height * 0.75f),
                                    ),
                                )
                            }

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                            )
                        }
                    }
                }
            }

            // Interactive gesture cue directly beneath the 3D case
            Text(
                text = if (lid.value > 0.6f) {
                    "Tap ${if (isDisc) "disc" else "cartridge"} to insert & play · Drag case to close"
                } else {
                    "Tap or drag cover to open physical case"
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

            // Primary Warm Ember Play / Resume CTA
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ascend.accent)
                    .clickable { ejectAndLaunch(null) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (saveSlots.any { it.first == AppPaths.AUTO_SLOT }) "Resume game" else "Play game",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Shelf Bucket Selector (Currently Playing · Completed · Backlog)
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

            // Save States (if present)
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

            // Core Selector (when multiple cores exist for this system)
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

@Composable
private fun InsideBookletInsert(game: Game, stat: PlayStat?) {
    Box(Modifier.fillMaxSize()) {
        // Booklet paper card
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF202028))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            ) {
                CoverArt(game, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF202028)),
                            ),
                        ),
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "INSTRUCTION BOOKLET",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 7.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = game.title,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 14.sp),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = game.system?.shortName ?: game.systemId.uppercase(),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Text(
                        text = if (stat != null && stat.seconds > 0) PlayTime.format(stat.seconds) else "Unplayed",
                        style = TextStyle(fontFamily = Inter, fontSize = 9.sp),
                        color = Tone.Muted,
                    )
                }
            }
        }

        // Two molded plastic booklet holder tabs on the right edge of the inside lid
        listOf(0.26f, 0.74f).forEach { frac ->
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (208 * frac).dp)
                    .size(width = 12.dp, height = 22.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    .background(Color(0xFF2A2A35))
                    .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)),
            )
        }
    }
}

@Composable
private fun DiscTrayInterior(
    game: Game,
    spinDegrees: Float,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Recessed circular tray well
        Canvas(Modifier.size(138.dp)) {
            drawCircle(color = Color(0xFF0C0C10))
            drawCircle(color = Color.White.copy(alpha = 0.06f), style = Stroke(width = 2f))
        }

        // Optical CD / DVD disc with artwork + holographic rainbow sheen + spindle hole
        Box(
            Modifier
                .size(128.dp)
                .offset { IntOffset(0, (-34f * ejectProgress).roundToInt()) }
                .graphicsLayer {
                    val s = 1f + 0.12f * ejectProgress
                    scaleX = s
                    scaleY = s
                    rotationZ = spinDegrees
                }
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .then(
                    if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier,
                )
                .semantics { contentDescription = "Optical disc for ${game.title}" },
            contentAlignment = Alignment.Center,
        ) {
            CoverArt(game, Modifier.fillMaxSize())

            // Iridescent optical data-track foil sweep
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to Color.White.copy(alpha = 0.24f),
                        0.18f to Color(0xFF5CE1E6).copy(alpha = 0.18f),
                        0.34f to Color.Transparent,
                        0.52f to Color(0xFFFF85C0).copy(alpha = 0.20f),
                        0.68f to Color.White.copy(alpha = 0.22f),
                        0.85f to Color.Transparent,
                        1.00f to Color.White.copy(alpha = 0.24f),
                    ),
                )
                drawCircle(color = Color.White.copy(alpha = 0.30f), style = Stroke(width = 2f))
                // Clear inner polycarbonate ring + center spindle hub
                drawCircle(color = Color(0xFFD8D8E0).copy(alpha = 0.55f), radius = size.minDimension * 0.20f)
                drawCircle(color = Color(0xFF131318), radius = size.minDimension * 0.11f)
                drawCircle(color = Color.White.copy(alpha = 0.22f), radius = size.minDimension * 0.11f, style = Stroke(width = 1.5f))
            }
        }
    }
}

@Composable
private fun CartridgeTrayInterior(
    game: Game,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Molded rectangular cartridge tray cavity
        Box(
            Modifier
                .size(width = 118.dp, height = 154.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0C0C10))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
        )

        // Sculpted ROM Cartridge
        Column(
            Modifier
                .size(width = 104.dp, height = 138.dp)
                .offset { IntOffset(0, (-34f * ejectProgress).roundToInt()) }
                .graphicsLayer {
                    val s = 1f + 0.10f * ejectProgress
                    scaleX = s
                    scaleY = s
                }
                .shadow(10.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF34343E), Color(0xFF23232B)),
                    ),
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                .then(
                    if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier,
                )
                .semantics { contentDescription = "ROM cartridge for ${game.title}" }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top grip ribs
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f)),
                    )
                }
            }

            // Recessed glossy cartridge label sticker
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
            ) {
                CoverArt(game, Modifier.fillMaxSize())
            }

            // Gold PCB connector pins hint at bottom edge
            Canvas(Modifier.width(66.dp).height(6.dp)) {
                val count = 10
                val step = size.width / count
                for (i in 0 until count) {
                    drawRoundRect(
                        color = Color(0xFFD4AF37).copy(alpha = 0.85f),
                        topLeft = Offset(i * step + 1.5f, 0f),
                        size = Size(step - 3f, size.height),
                        cornerRadius = CornerRadius(1.5f, 1.5f),
                    )
                }
            }
        }
    }
}
