package com.retrohero.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import com.retrohero.app.data.Box3dMath
import com.retrohero.app.data.BoxFaceId
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameDossier
import com.retrohero.app.data.PhysicalMediaKind
import com.retrohero.app.data.PhysicalMediaSpec
import com.retrohero.app.data.RetailCaseStyle
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Which 3D physical artifact is currently displayed on the LaunchBox-style turntable stage. */
enum class PhysicalViewerSubject { RETAIL_BOX, GAME_MEDIA }

/** Optional pre-decoded bitmaps (used by unit/screenshot tests; null in production where Coil loads live). */
@Immutable
data class PhysicalViewerBitmaps(
    val boxFront: Bitmap? = null,
    val boxBack: Bitmap? = null,
    val boxSpine: Bitmap? = null,
    val boxFull: Bitmap? = null,
    val cartFront: Bitmap? = null,
    val cartBack: Bitmap? = null,
    val disc: Bitmap? = null,
    val screenshot1: Bitmap? = null,
    val screenshot2: Bitmap? = null,
)

@Immutable
internal data class ResolvedPhysicalTextures(
    val front: Bitmap,
    val back: Bitmap,
    val spine: Bitmap,
    val mediaFront: Bitmap,
    val mediaBack: Bitmap,
    val hasRealFront: Boolean,
    val hasRealBack: Boolean,
    val hasRealSpine: Boolean,
    val hasRealMedia: Boolean,
    val maskedMediaBack: Bitmap = mediaBack,
)

/**
 * LaunchBox-inspired Interactive 3D Retail Box, Spine & Cartridge/Disc Spin Viewer for the Game Page.
 *
 * - Drag horizontally (with momentum fling) or tilt your phone to rotate the full 6-face 3D game case.
 * - Flip 180° to read the real LaunchBox `Box - Back` blurb (or authentic reconstructed back-of-box sleeve),
 *   inspect the `Box - Spine`, and switch to the console-authentic 3D Cartridge / Game Card / Optical Disc!
 */
@Composable
internal fun M3PhysicalBoxViewerSection(
    game: Game,
    dossier: GameDossier,
    openInspectorTrigger: Int = 0,
    initialSubject: PhysicalViewerSubject = PhysicalViewerSubject.RETAIL_BOX,
    initialYawDeg: Float = 24f,
    overrideBitmaps: PhysicalViewerBitmaps? = null,
    modifier: Modifier = Modifier,
    /** If set, the case starts at this angle and turns to [initialYawDeg] (e.g. spine-on, pulled off a shelf). */
    introFromYawDeg: Float? = null,
    /** When provided, shows a tactile Play action on the 3D viewer that animates inserting the media into the console. */
    onPlay: (() -> Unit)? = null,
    /** Increment to trigger the 3D cartridge/disc console insertion ceremony externally. */
    insertLaunchTrigger: Int = 0,
    /** Optional hoisted textures so Game Info doesn't duplicate texture work between hero inspector & inline stage. */
    preloadedTextures: ResolvedPhysicalTextures? = null,
    /** Optional hoisted inspector handler so Game Info opens fullscreen 3D inspector instantly from anywhere. */
    onOpenInspector: ((PhysicalViewerSubject, Float) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()

    val sysAccent = game.system?.color ?: cs.primary
    val spec = remember(game.systemId, sysAccent) {
        PhysicalMediaSpec.forSystem(game.systemId, sysAccent)
    }

    val textures = preloadedTextures ?: rememberPhysicalTextures(game, dossier, spec, overrideBitmaps)

    var subject by remember(game.id) { mutableStateOf(initialSubject) }
    val yawAnim = remember(game.id) { Animatable(introFromYawDeg ?: initialYawDeg) }
    LaunchedEffect(game.id) {
        if (introFromYawDeg != null) {
            if (reducedMotion) {
                yawAnim.snapTo(initialYawDeg)
            } else {
                yawAnim.animateTo(initialYawDeg, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
            }
        }
    }
    var pitchDeg by remember(game.id) { mutableFloatStateOf(-5f) }
    var autoSpin by remember(game.id) { mutableStateOf(false) }
    var tiltEnabled by remember(game.id) { mutableStateOf(!reducedMotion) }
    var tiltResetNonce by remember(game.id) { mutableIntStateOf(0) }
    var showInspector by remember(game.id) { mutableStateOf(false) }
    var showInsertCeremony by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(openInspectorTrigger) {
        if (openInspectorTrigger > 0) {
            showInspector = true
        }
    }
    LaunchedEffect(insertLaunchTrigger) {
        if (insertLaunchTrigger > 0 && onPlay != null) {
            showInsertCeremony = true
        }
    }

    val (tiltAvailable, tiltOffset) = rememberDeviceTiltAngles(
        enabled = tiltEnabled && !reducedMotion && !autoSpin,
        recenterNonce = tiltResetNonce,
    )

    // Smooth continuous turntable auto-spin when toggled
    LaunchedEffect(autoSpin, reducedMotion) {
        if (!autoSpin || reducedMotion) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        while (isActive && autoSpin) {
            val now = withFrameNanos { it }
            val dtSec = ((now - lastNanos).coerceIn(0L, 64_000_000L)) / 1_000_000_000f
            lastNanos = now
            yawAnim.snapTo(normalizeSignedDegrees(yawAnim.value + dtSec * 38f))
        }
    }

    val effectiveYaw = yawAnim.value + if (tiltAvailable && tiltEnabled && !autoSpin) tiltOffset.value.x else 0f
    val effectivePitch = (pitchDeg + if (tiltAvailable && tiltEnabled && !autoSpin) tiltOffset.value.y else 0f)
        .coerceIn(-28f, 28f)

    val boxAspectTarget = remember(textures.front, spec) {
        (textures.front.width.toFloat() / textures.front.height.toFloat().coerceAtLeast(1f))
            .coerceIn(0.54f, 1.50f)
    }
    val animatedBoxAspect by animateFloatAsState(
        targetValue = boxAspectTarget,
        animationSpec = tween(240),
        label = "box3dAspect",
    )

    fun snapToAngle(targetYaw: Float, targetPitch: Float = -4f) {
        autoSpin = false
        tiltResetNonce++
        pitchDeg = targetPitch
        scope.launch {
            val current = normalizeSignedDegrees(yawAnim.value)
            yawAnim.snapTo(current)
            // Shortest angular path around the 360° circle
            var delta = normalizeSignedDegrees(targetYaw - current)
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            yawAnim.animateTo(
                targetValue = current + delta,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    val activeFaceLabel = remember(subject, effectiveYaw, spec) {
        describeVisibleAngle(subject, effectiveYaw, spec)
    }
    val assetProvenance = remember(
        subject,
        textures.hasRealFront,
        textures.hasRealSpine,
        textures.hasRealBack,
        textures.hasRealMedia,
        spec.mediaKind.tabLabel,
        spec.mediaKind.isOpticalDisc,
        spec.caseStyle.label,
    ) {
        val parts = buildList {
            if (textures.hasRealFront) add("Front")
            if (textures.hasRealSpine) add("Spine")
            if (textures.hasRealBack) add("Back")
            if (textures.hasRealMedia) add(spec.mediaKind.tabLabel)
        }
        when {
            parts.isNotEmpty() -> "LaunchBox · ${parts.joinToString(" · ")}"
            subject == PhysicalViewerSubject.GAME_MEDIA -> if (spec.mediaKind.isOpticalDisc) {
                "${spec.mediaKind.tabLabel} · Optical Media"
            } else {
                "${spec.mediaKind.tabLabel} · 3D Shell"
            }
            else -> "${spec.caseStyle.label} · 3D Case"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Section Header + Subject Switcher (3D Case <-> Cartridge / Disc)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "3D box & ${spec.mediaKind.tabLabel.lowercase(Locale.US)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurface,
                )
                Text(
                    text = if (tiltAvailable && tiltEnabled) {
                        "Drag 360° · Tilt for live glint"
                    } else {
                        "Drag 360° · Double-tap to flip"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Segmented subject switcher: [ 3D Box | Cartridge/Disc ]
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val boxSelected = subject == PhysicalViewerSubject.RETAIL_BOX
                SubjectChip(
                    label = "Case",
                    icon = Icons.Rounded.SportsEsports,
                    selected = boxSelected,
                    onClick = {
                        if (!boxSelected) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            subject = PhysicalViewerSubject.RETAIL_BOX
                            snapToAngle(24f, -5f)
                        }
                    },
                )
                val mediaSelected = subject == PhysicalViewerSubject.GAME_MEDIA
                SubjectChip(
                    label = spec.mediaKind.tabLabel,
                    icon = if (spec.mediaKind.isOpticalDisc) Icons.Rounded.Album else Icons.Rounded.Memory,
                    selected = mediaSelected,
                    onClick = {
                        if (!mediaSelected) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            subject = PhysicalViewerSubject.GAME_MEDIA
                            snapToAngle(20f, -6f)
                        }
                    },
                )
            }
        }

        // Interactive 3D Pedestal Stage Card
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(312.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.surfaceContainerHigh,
                            cs.surfaceContainer,
                            cs.surfaceContainerLowest,
                        ),
                    ),
                )
                .border(1.dp, cs.outlineVariant.copy(alpha = 0.40f), RoundedCornerShape(28.dp))
                .pointerInput(game.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            val norm = abs(normalizeSignedDegrees(yawAnim.value))
                            // Double-tap flips between Front (24°) and Back (180°)!
                            if (norm > 95f) snapToAngle(24f, -5f) else snapToAngle(180f, 0f)
                        },
                    )
                }
                .horizontalSpinGesture(
                    onDragStart = { autoSpin = false },
                    onDragDelta = { dx, dy ->
                        scope.launch {
                            yawAnim.snapTo(normalizeSignedDegrees(yawAnim.value + dx * 0.48f))
                        }
                        pitchDeg = (pitchDeg - dy * 0.16f).coerceIn(-24f, 24f)
                    },
                    onFlingVelocityX = { vx ->
                        if (abs(vx) > 220f) {
                            scope.launch {
                                yawAnim.animateDecay(
                                    initialVelocity = vx * 0.18f,
                                    animationSpec = exponentialDecay(frictionMultiplier = 1.9f),
                                )
                            }
                        }
                    },
                ),
        ) {
            // Subtle ambient studio spotlight tinted by the console's accent color
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to sysAccent.copy(alpha = 0.18f),
                        0.55f to cs.primary.copy(alpha = 0.06f),
                        1.0f to Color.Transparent,
                        center = Offset(size.width * 0.50f, size.height * 0.46f),
                        radius = size.minDimension * 0.68f,
                    ),
                    radius = size.minDimension * 0.68f,
                    center = Offset(size.width * 0.50f, size.height * 0.46f),
                )
            }

            // Real-time 3D Projected Box / Cartridge / Optical Disc
            Physical3dStageCanvas(
                subject = subject,
                spec = spec,
                textures = textures,
                boxAspect = animatedBoxAspect,
                yawDeg = effectiveYaw,
                pitchDeg = effectivePitch,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp, bottom = 34.dp, start = 16.dp, end = 16.dp),
            )

            // Top bar: Provenance pill (left, ellipsizes cleanly) + Fullscreen Zoom button (right, never overlaps!)
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier
                        .weight(1f, fill = false)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest.copy(alpha = 0.86f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = if (textures.hasRealFront || textures.hasRealBack || textures.hasRealMedia) {
                            Icons.Rounded.Verified
                        } else {
                            Icons.Rounded.AutoAwesome
                        },
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = assetProvenance,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.primaryContainer.copy(alpha = 0.92f))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            val hoisted = onOpenInspector
                            if (hoisted != null) hoisted(subject, yawAnim.value) else showInspector = true
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Inspect & zoom 3D box",
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Zoom",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = cs.onPrimaryContainer,
                    )
                }
            }

            // Bottom-center live angle readout pill
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHighest.copy(alpha = 0.86f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = activeFaceLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = cs.onSurface,
                )
                Text(
                    text = "· Double-tap to flip",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = cs.onSurfaceVariant,
                )
            }
        }

        // LaunchBox-style Quick Angle, Insert & Play & Turntable Controls Strip
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onPlay != null) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.primary)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            showInsertCeremony = true
                        }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (spec.mediaKind.isOpticalDisc) "Load & Play" else "Insert & Play",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                        ),
                        color = cs.onPrimary,
                        maxLines = 1,
                    )
                }
            }

            val normYaw = normalizeSignedDegrees(effectiveYaw)
            if (subject == PhysicalViewerSubject.RETAIL_BOX) {
                AnglePresetChip(
                    label = "3/4 Box",
                    active = !autoSpin && normYaw in 10f..55f,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        snapToAngle(24f, -5f)
                    },
                )
                AnglePresetChip(
                    label = "Front",
                    active = !autoSpin && abs(normYaw) < 10f,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        snapToAngle(0f, 0f)
                    },
                )
                AnglePresetChip(
                    label = "Spine",
                    active = !autoSpin && normYaw in 65f..115f,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        snapToAngle(88f, 0f)
                    },
                )
                AnglePresetChip(
                    label = "Back",
                    active = !autoSpin && abs(normYaw) > 145f,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        snapToAngle(180f, 0f)
                    },
                )
            } else {
                AnglePresetChip(
                    label = "${spec.mediaKind.tabLabel} Front",
                    active = !autoSpin && abs(normYaw) < 55f,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        snapToAngle(16f, -5f)
                    },
                )
                AnglePresetChip(
                    label = if (spec.mediaKind.isOpticalDisc) {
                        if (spec.blackDiscUnderside) "Black PS1 Side" else "Data Side"
                    } else {
                        "Rear Shell"
                    },
                    active = !autoSpin && abs(normYaw) > 135f,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        snapToAngle(180f, 4f)
                    },
                )
            }

            if (!reducedMotion) {
                AnglePresetChip(
                    label = if (autoSpin) "Stop spin" else "Spin",
                    active = autoSpin,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        autoSpin = !autoSpin
                    },
                )
            }

            if (tiltAvailable && !reducedMotion) {
                AnglePresetChip(
                    label = if (tiltEnabled) "Tilt: On" else "Tilt: Off",
                    active = tiltEnabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        tiltEnabled = !tiltEnabled
                        tiltResetNonce++
                    },
                )
            }
        }
    }

    if (showInspector) {
        M3PhysicalBoxInspectorDialog(
            game = game,
            spec = spec,
            textures = textures,
            initialSubject = subject,
            initialYawDeg = yawAnim.value,
            boxAspect = animatedBoxAspect,
            onPlayFromModel = if (onPlay != null) {
                {
                    showInspector = false
                    showInsertCeremony = true
                }
            } else null,
            onDismiss = { showInspector = false },
        )
    }

    if (showInsertCeremony && onPlay != null) {
        M3ConsoleInsertionDialog(
            game = game,
            spec = spec,
            textures = textures,
            onLaunchNow = onPlay,
            onDismiss = { showInsertCeremony = false },
        )
    }
}

@Composable
private fun SubjectChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) cs.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) cs.onPrimary else cs.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnglePresetChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (active) cs.secondaryContainer else cs.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (active) cs.primary.copy(alpha = 0.65f) else cs.outlineVariant.copy(alpha = 0.30f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 11.5.sp,
            ),
            color = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
        )
    }
}

/**
 * Fullscreen 3D Spin + Flat Hi-Res Pinch-to-Zoom Inspector so users can comfortably read fine print
 * on LaunchBox `Box - Back` blurbs, `Box - Spine` strips, and `Cart - Front` / `Disc` labels.
 */
@Composable
internal fun M3PhysicalBoxInspectorDialog(
    game: Game,
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    initialSubject: PhysicalViewerSubject,
    initialYawDeg: Float,
    boxAspect: Float,
    onPlayFromModel: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf(initialSubject) }
    var flatReadMode by remember { mutableStateOf(false) }
    var flatFaceIdx by remember { mutableIntStateOf(1) } // 0=Front, 1=Back, 2=Spine, 3=Media
    val yawAnim = remember { Animatable(initialYawDeg) }
    var pitchDeg by remember { mutableFloatStateOf(-4f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    fun snapYaw(target: Float) {
        zoom = 1f
        panOffset = Offset.Zero
        pitchDeg = 0f
        scope.launch {
            val cur = normalizeSignedDegrees(yawAnim.value)
            yawAnim.snapTo(cur)
            var d = normalizeSignedDegrees(target - cur)
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
            yawAnim.animateTo(cur + d, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xF50B0C10))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            // Top bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (flatReadMode) {
                            "Pinch to zoom up to 3.5× to read back-of-box fine print"
                        } else {
                            "Drag anywhere to rotate 3D · Pinch to zoom (${String.format(Locale.US, "%.1f×", zoom)})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(if (flatReadMode) cs.primary else Color.White.copy(alpha = 0.14f))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                flatReadMode = !flatReadMode
                                zoom = 1f
                                panOffset = Offset.Zero
                                if (flatReadMode) {
                                    val y = abs(normalizeSignedDegrees(yawAnim.value))
                                    flatFaceIdx = when {
                                        subject == PhysicalViewerSubject.GAME_MEDIA -> 3
                                        y > 130f -> 1
                                        y in 60f..130f -> 2
                                        else -> 0
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (flatReadMode) "3D View" else "Read Flat",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (flatReadMode) cs.onPrimary else Color.White,
                        )
                    }

                    if (zoom > 1.05f) {
                        IconButton(
                            onClick = {
                                zoom = 1f
                                panOffset = Offset.Zero
                            },
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset zoom", tint = Color.White)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Center Interactive Viewport
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pointerInput(flatReadMode) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val nextZoom = (zoom * gestureZoom).coerceIn(1f, 3.6f)
                            zoom = nextZoom
                            if (flatReadMode || nextZoom > 1.12f) {
                                val maxPan = 420f * (nextZoom - 1f).coerceAtLeast(0.15f)
                                panOffset = Offset(
                                    (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                    (panOffset.y + pan.y).coerceIn(-maxPan, maxPan),
                                )
                            } else {
                                scope.launch {
                                    yawAnim.snapTo(normalizeSignedDegrees(yawAnim.value + pan.x * 0.45f))
                                }
                                pitchDeg = (pitchDeg - pan.y * 0.22f).coerceIn(-34f, 34f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (flatReadMode) {
                    val flatBitmap = when (flatFaceIdx) {
                        0 -> textures.front
                        1 -> textures.back
                        2 -> textures.spine
                        else -> textures.mediaFront
                    }
                    androidx.compose.foundation.Image(
                        bitmap = remember(flatBitmap) { flatBitmap.asImageBitmap() },
                        contentDescription = game.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                    )
                } else {
                    Physical3dStageCanvas(
                        subject = subject,
                        spec = spec,
                        textures = textures,
                        boxAspect = boxAspect,
                        yawDeg = yawAnim.value,
                        pitchDeg = pitchDeg,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                    )
                }
            }

            // Bottom quick face bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (flatReadMode) {
                    listOf(
                        0 to "Front Cover",
                        1 to "Back of Box",
                        2 to "Spine",
                        3 to spec.mediaKind.tabLabel,
                    ).forEach { (idx, label) ->
                        InspectorPill(
                            label = label,
                            selected = flatFaceIdx == idx,
                            onClick = {
                                flatFaceIdx = idx
                                zoom = 1f
                                panOffset = Offset.Zero
                            },
                        )
                    }
                } else {
                    InspectorPill(
                        label = "3D Case",
                        selected = subject == PhysicalViewerSubject.RETAIL_BOX && abs(normalizeSignedDegrees(yawAnim.value)) <= 120f,
                        onClick = {
                            subject = PhysicalViewerSubject.RETAIL_BOX
                            snapYaw(24f)
                        },
                    )
                    InspectorPill(
                        label = "Flip to Back",
                        selected = subject == PhysicalViewerSubject.RETAIL_BOX && abs(normalizeSignedDegrees(yawAnim.value)) > 120f,
                        onClick = {
                            subject = PhysicalViewerSubject.RETAIL_BOX
                            snapYaw(180f)
                        },
                    )
                    InspectorPill(
                        label = "Spine",
                        selected = false,
                        onClick = {
                            subject = PhysicalViewerSubject.RETAIL_BOX
                            snapYaw(88f)
                        },
                    )
                    InspectorPill(
                        label = spec.mediaKind.tabLabel,
                        selected = subject == PhysicalViewerSubject.GAME_MEDIA,
                        onClick = {
                            subject = PhysicalViewerSubject.GAME_MEDIA
                            snapYaw(18f)
                        },
                    )
                }

                if (onPlayFromModel != null) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.primary)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onPlayFromModel()
                            }
                            .padding(horizontal = 15.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = cs.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (spec.mediaKind.isOpticalDisc) "Load & Play" else "Insert & Play",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = cs.onPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) cs.primary else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) cs.onPrimary else Color.White,
        )
    }
}

// ---- Core 3D Canvas Renderer (Retail Box + Cartridge + Optical Disc) ----------------------------

/**
 * The 3D renderer for cases, cartridges and discs, shared by the game page and the Physical Shelf.
 * [spineFit] sizes the case by height alone (for a narrow shelf slot showing the spine) instead of
 * fitting the whole case in view; [floorShadow] draws the studio contact shadow under it.
 */
@Composable
internal fun Physical3dStageCanvas(
    subject: PhysicalViewerSubject,
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    boxAspect: Float,
    yawDeg: Float,
    pitchDeg: Float,
    modifier: Modifier = Modifier,
    spineFit: Boolean = false,
    floorShadow: Boolean = true,
) {
    val polyMatrix = remember { Matrix() }
    val clipPath = remember { AndroidPath() }
    val bmpPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val fillPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
    }
    val srcPts = remember { FloatArray(8) }
    val cardboardTopColor = remember(textures.front) { sampleBitmapEdgeArgb(textures.front, topEdge = true) }
    val cardboardBottomColor = remember(textures.front) { sampleBitmapEdgeArgb(textures.front, topEdge = false) }
    val realCartShellColor = remember(textures.mediaFront, textures.hasRealMedia, spec.mediaShellColor) {
        if (textures.hasRealMedia) {
            sampleRealCartShellArgb(textures.mediaFront, spec.mediaShellColor.toArgb())
        } else {
            spec.mediaShellColor.toArgb()
        }
    }
    val maskedCartBackBmp = textures.maskedMediaBack

    Canvas(modifier) {
        val stageW = size.width
        val stageH = size.height
        if (stageW <= 16f || stageH <= 16f) return@Canvas

        val cx = stageW * 0.5f
        val cy = if (spineFit) stageH * ShelfLayoutMath.SPINE_FIT_CENTER else stageH * 0.48f
        val focal = max(stageW, stageH) * 2.35f

        val isDisc = subject == PhysicalViewerSubject.GAME_MEDIA && spec.mediaKind.isOpticalDisc
        val targetAspect = when {
            subject == PhysicalViewerSubject.RETAIL_BOX -> boxAspect
            isDisc -> 1.0f
            else -> (textures.mediaFront.width.toFloat() / textures.mediaFront.height.toFloat().coerceAtLeast(1f))
                .coerceIn(0.62f, 1.65f)
        }
        val depthRatio = when {
            subject == PhysicalViewerSubject.RETAIL_BOX -> spec.depthRatio
            else -> spec.mediaKind.thicknessRatio
        }

        // Fit cleanly inside the stage regardless of portrait vs landscape box proportions
        val maxBoxH = stageH * if (isDisc) 0.78f else 0.80f
        val maxBoxW = stageW * 0.70f
        // Spine-on, the spine sits halfW nearer the camera and perspective enlarges it by
        // focal / (focal - halfW). Solve for the half-height that lands it exactly at the target,
        // so wide and narrow cases stand the same height on the shelf.
        val spineTarget = stageH * ShelfLayoutMath.SPINE_FIT_HALF_HEIGHT
        var halfH = if (spineFit) spineTarget * focal / (focal + spineTarget * targetAspect) else maxBoxH * 0.5f
        var halfW = halfH * targetAspect
        if (!spineFit && halfW * 2f > maxBoxW) {
            halfW = maxBoxW * 0.5f
            halfH = halfW / targetAspect
        }
        val halfD = max(halfW, halfH) * depthRatio

        val yawRad = Math.toRadians(yawDeg.toDouble())
        if (floorShadow) {
            // Dual-layer perspective-coupled studio contact shadow (soft wide penumbra + grounded core umbra)
            val projectedFootprintW = abs(halfW * cos(yawRad).toFloat()) + abs(halfD * sin(yawRad).toFloat())
            val shadowRadiusX = (projectedFootprintW * 1.24f).coerceAtLeast(halfD * 1.9f)
            val shadowY = cy + halfH * 1.09f
            drawOval(
                brush = Brush.radialGradient(
                    0.0f to Color.Black.copy(alpha = 0.28f),
                    0.55f to Color.Black.copy(alpha = 0.12f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, shadowY + halfH * 0.02f),
                    radius = shadowRadiusX.coerceAtLeast(12f),
                ),
                topLeft = Offset(cx - shadowRadiusX, shadowY - halfH * 0.12f),
                size = androidx.compose.ui.geometry.Size(shadowRadiusX * 2f, halfH * 0.26f),
            )
            val coreShadowX = (projectedFootprintW * 0.88f).coerceAtLeast(halfD * 1.15f)
            drawOval(
                brush = Brush.radialGradient(
                    0.0f to Color.Black.copy(alpha = 0.56f),
                    0.65f to Color.Black.copy(alpha = 0.22f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, shadowY),
                    radius = coreShadowX.coerceAtLeast(10f),
                ),
                topLeft = Offset(cx - coreShadowX, shadowY - halfH * 0.065f),
                size = androidx.compose.ui.geometry.Size(coreShadowX * 2f, halfH * 0.13f),
            )

        }

        val quads = Box3dMath.projectBox(
            halfW = halfW,
            halfH = halfH,
            halfD = halfD,
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            centerX = cx,
            centerY = cy,
            focalPx = focal,
        )

        drawIntoCanvas { composeCanvas ->
            val native = composeCanvas.nativeCanvas
            val isCartridge = subject == PhysicalViewerSubject.GAME_MEDIA && !isDisc
            if (isDisc) {
                draw3dOpticalDisc(
                    native = native,
                    quads = quads,
                    spec = spec,
                    labelBmp = textures.mediaFront,
                    undersideBmp = textures.mediaBack,
                    yawDeg = yawDeg,
                    pitchDeg = pitchDeg,
                    polyMatrix = polyMatrix,
                    bmpPaint = bmpPaint,
                    fillPaint = fillPaint,
                    srcPts = srcPts,
                )
            } else if (isCartridge) {
                // True alpha-silhouette 3D extrusion for both real LaunchBox scans AND console-sculpted
                // procedural cartridges (3DS lockout tab, GBA flared wings, NES stepped neck, GB notch, N64 arch!)
                draw3dRealCartridgeScan(
                    native = native,
                    quads = quads,
                    shellArgb = realCartShellColor,
                    frontScanBmp = textures.mediaFront,
                    backShellBmp = maskedCartBackBmp,
                    halfD = halfD,
                    yawDeg = yawDeg,
                    pitchDeg = pitchDeg,
                    polyMatrix = polyMatrix,
                    bmpPaint = bmpPaint,
                    fillPaint = fillPaint,
                    srcPts = srcPts,
                    useTexSilhouette = !textures.hasRealMedia,
                )
            } else {
                val caseColorArgb = if (subject == PhysicalViewerSubject.RETAIL_BOX) {
                    spec.casePlasticColor.toArgb()
                } else {
                    spec.mediaShellColor.toArgb()
                }
                val isCardboardBox = subject == PhysicalViewerSubject.RETAIL_BOX && !spec.caseStyle.hasPlasticRim

                for (q in quads) {
                    val d = q.dstPoints
                    clipPath.reset()
                    clipPath.moveTo(d[0], d[1])
                    clipPath.lineTo(d[2], d[3])
                    clipPath.lineTo(d[4], d[5])
                    clipPath.lineTo(d[6], d[7])
                    clipPath.close()

                    val useSpineTextureOnRight = isCardboardBox && q.face == BoxFaceId.RIGHT_OPENING
                    when {
                        q.face == BoxFaceId.FRONT ||
                            q.face == BoxFaceId.BACK ||
                            q.face == BoxFaceId.LEFT_SPINE ||
                            useSpineTextureOnRight -> {
                            val faceBmp = when {
                                isCartridge && q.face == BoxFaceId.FRONT -> textures.mediaFront
                                isCartridge && q.face == BoxFaceId.BACK -> textures.mediaBack
                                isCartridge -> null
                                q.face == BoxFaceId.FRONT -> textures.front
                                q.face == BoxFaceId.BACK -> textures.back
                                else -> textures.spine
                            }
                            if (faceBmp != null) {
                                val bw = faceBmp.width.toFloat()
                                val bh = faceBmp.height.toFloat()
                                srcPts[0] = 0f; srcPts[1] = 0f
                                srcPts[2] = bw; srcPts[3] = 0f
                                srcPts[4] = bw; srcPts[5] = bh
                                srcPts[6] = 0f; srcPts[7] = bh
                                if (polyMatrix.setPolyToPoly(srcPts, 0, d, 0, 4)) {
                                    native.save()
                                    native.clipPath(clipPath)
                                    native.concat(polyMatrix)

                                    // For procedural rounded cartridges, clip shading & gloss strictly inside the molded shell
                                    // silhouette so no dark rectangular corner ever bleeds past the rounded shell!
                                    if (isCartridge) {
                                        val cartShellClip = AndroidPath().apply {
                                            addRoundRect(
                                                RectF(6f, 6f, bw - 6f, bh - 6f),
                                                28f,
                                                28f,
                                                AndroidPath.Direction.CW,
                                            )
                                        }
                                        native.clipPath(cartShellClip)
                                    }

                                    // Authentic physical retail case housing around the printed cover insert
                                    if (subject == PhysicalViewerSubject.RETAIL_BOX && spec.caseStyle.hasPlasticRim) {
                                        fillPaint.shader = null
                                        fillPaint.color = shadeArgb(caseColorArgb, q.shade)
                                        native.drawRect(0f, 0f, bw, bh, fillPaint)

                                        // Molded outer plastic rim bevel catch-light (top/left highlight, bottom/right shadow)
                                        fillPaint.shader = LinearGradient(
                                            0f, 0f, bw, bh,
                                            AndroidColor.argb(46, 255, 255, 255),
                                            AndroidColor.argb(64, 0, 0, 0),
                                            Shader.TileMode.CLAMP,
                                        )
                                        native.drawRect(0f, 0f, bw, bh, fillPaint)
                                        fillPaint.shader = null

                                        val isJewel = spec.caseStyle == RetailCaseStyle.JEWEL_CASE_CD
                                        val hingeW = if (isJewel && q.face == BoxFaceId.FRONT) bw * 0.074f else 0f
                                        val padX = if (q.face == BoxFaceId.LEFT_SPINE) 0f else bw * 0.024f
                                        val padY = bh * 0.020f
                                        val dstRect = RectF(
                                            when {
                                                isJewel && q.face == BoxFaceId.FRONT -> hingeW
                                                q.face == BoxFaceId.FRONT -> 0f
                                                else -> padX
                                            },
                                            padY,
                                            if (q.face == BoxFaceId.BACK) bw else bw - padX,
                                            bh - padY,
                                        )
                                        // Subtle recessed pocket shadow around the paper insert
                                        fillPaint.color = AndroidColor.argb(95, 0, 0, 0)
                                        native.drawRect(
                                            dstRect.left - 1.5f,
                                            dstRect.top - 1.5f,
                                            dstRect.right + 1.5f,
                                            dstRect.bottom + 1.5f,
                                            fillPaint,
                                        )
                                        native.drawBitmap(faceBmp, null, dstRect, bmpPaint)

                                        if (isJewel && q.face == BoxFaceId.FRONT) {
                                            // Authentic PS1 / Dreamcast / Saturn clear ribbed acrylic left tray strip!
                                            fillPaint.color = AndroidColor.rgb(20, 22, 28)
                                            native.drawRect(0f, 0f, hingeW, bh, fillPaint)
                                            val ribCount = 30
                                            val ribStep = bh / ribCount
                                            for (r in 0 until ribCount) {
                                                val ry = r * ribStep
                                                fillPaint.color = AndroidColor.argb(62, 210, 224, 248)
                                                native.drawRect(bw * 0.010f, ry + ribStep * 0.18f, hingeW - 3f, ry + ribStep * 0.44f, fillPaint)
                                                fillPaint.color = AndroidColor.argb(85, 0, 0, 0)
                                                native.drawRect(bw * 0.010f, ry + ribStep * 0.44f, hingeW - 3f, ry + ribStep * 0.64f, fillPaint)
                                            }
                                            // Clear acrylic lid hinge bar + top/bottom molded hinge pins
                                            fillPaint.shader = LinearGradient(
                                                0f, 0f, hingeW, 0f,
                                                intArrayOf(
                                                    AndroidColor.argb(82, 255, 255, 255),
                                                    AndroidColor.argb(24, 225, 238, 255),
                                                    AndroidColor.argb(95, 255, 255, 255),
                                                    AndroidColor.argb(145, 10, 12, 18),
                                                ),
                                                floatArrayOf(0f, 0.45f, 0.86f, 1f),
                                                Shader.TileMode.CLAMP,
                                            )
                                            native.drawRect(0f, 0f, hingeW, bh, fillPaint)
                                            fillPaint.shader = null

                                            // Tiny clear acrylic booklet retainer tabs on top & bottom of the jewel case lid
                                            fillPaint.color = AndroidColor.argb(95, 235, 244, 255)
                                            val tabW = bw * 0.042f
                                            val tabH = bh * 0.014f
                                            native.drawRoundRect(RectF(bw * 0.34f, padY, bw * 0.34f + tabW, padY + tabH), 4f, 4f, fillPaint)
                                            native.drawRoundRect(RectF(bw * 0.72f, padY, bw * 0.72f + tabW, padY + tabH), 4f, 4f, fillPaint)
                                            native.drawRoundRect(RectF(bw * 0.34f, bh - padY - tabH, bw * 0.34f + tabW, bh - padY), 4f, 4f, fillPaint)
                                            native.drawRoundRect(RectF(bw * 0.72f, bh - padY - tabH, bw * 0.72f + tabW, bh - padY), 4f, 4f, fillPaint)
                                        } else if (q.face == BoxFaceId.FRONT || q.face == BoxFaceId.BACK) {
                                            // Authentic clear polypropylene outer jacket sleeve heat-seal lip lines (DS / PS2 / GC / Wii / Switch)
                                            fillPaint.color = AndroidColor.argb(58, 255, 255, 255)
                                            native.drawRect(dstRect.left, dstRect.top, dstRect.right, dstRect.top + 1.8f, fillPaint)
                                            fillPaint.color = AndroidColor.argb(46, 255, 255, 255)
                                            native.drawRect(dstRect.left, dstRect.bottom - 1.8f, dstRect.right, dstRect.bottom, fillPaint)
                                        }
                                    } else {
                                        native.drawBitmap(faceBmp, 0f, 0f, bmpPaint)
                                        // Subtle printed cardboard top-flap fold crease & warm cardstock edge wear on cardboard boxes
                                        if (isCardboardBox && (q.face == BoxFaceId.FRONT || q.face == BoxFaceId.BACK)) {
                                            fillPaint.shader = LinearGradient(
                                                0f, 0f, 0f, bh * 0.022f,
                                                intArrayOf(
                                                    AndroidColor.argb(48, 255, 248, 236),
                                                    AndroidColor.argb(42, 0, 0, 0),
                                                    AndroidColor.TRANSPARENT,
                                                ),
                                                floatArrayOf(0f, 0.38f, 1f),
                                                Shader.TileMode.CLAMP,
                                            )
                                            native.drawRect(0f, 0f, bw, bh * 0.022f, fillPaint)
                                            fillPaint.shader = null
                                        }
                                    }

                                    // Realistic spine-fold crease shadow + edge catch-light on 3D retail boxes
                                    if (subject == PhysicalViewerSubject.RETAIL_BOX) {
                                        if (q.face == BoxFaceId.FRONT) {
                                            val creaseW = bw * 0.036f
                                            fillPaint.shader = LinearGradient(
                                                0f, 0f, creaseW, 0f,
                                                intArrayOf(
                                                    AndroidColor.argb(82, 255, 252, 246),
                                                    AndroidColor.argb(54, 0, 0, 0),
                                                    AndroidColor.TRANSPARENT,
                                                ),
                                                floatArrayOf(0f, 0.24f, 1f),
                                                Shader.TileMode.CLAMP,
                                            )
                                            native.drawRect(0f, 0f, creaseW, bh, fillPaint)
                                            fillPaint.shader = null
                                        } else if (q.face == BoxFaceId.BACK) {
                                            val creaseW = bw * 0.036f
                                            fillPaint.shader = LinearGradient(
                                                bw - creaseW, 0f, bw, 0f,
                                                intArrayOf(
                                                    AndroidColor.TRANSPARENT,
                                                    AndroidColor.argb(54, 0, 0, 0),
                                                    AndroidColor.argb(70, 255, 252, 246),
                                                ),
                                                floatArrayOf(0f, 0.76f, 1f),
                                                Shader.TileMode.CLAMP,
                                            )
                                            native.drawRect(bw - creaseW, 0f, bw, bh, fillPaint)
                                            fillPaint.shader = null
                                        } else if (q.face == BoxFaceId.LEFT_SPINE || useSpineTextureOnRight) {
                                            // Subtle rounded cardstock/wrap highlight along spine edges
                                            fillPaint.shader = LinearGradient(
                                                0f, 0f, bw, 0f,
                                                intArrayOf(
                                                    AndroidColor.argb(64, 0, 0, 0),
                                                    AndroidColor.TRANSPARENT,
                                                    AndroidColor.TRANSPARENT,
                                                    AndroidColor.argb(74, 255, 252, 246),
                                                ),
                                                floatArrayOf(0f, 0.16f, 0.84f, 1f),
                                                Shader.TileMode.CLAMP,
                                            )
                                            native.drawRect(0f, 0f, bw, bh, fillPaint)
                                            fillPaint.shader = null
                                        }
                                    }

                                    // Directional 3D shading + moving studio softbox specular highlight
                                    drawFaceLightingAndGloss(
                                        native = native,
                                        bw = bw,
                                        bh = bh,
                                        shade = q.shade,
                                        specular = q.specular,
                                        glossy = if (subject == PhysicalViewerSubject.RETAIL_BOX) {
                                            spec.caseStyle.glossyWrap
                                        } else {
                                            true
                                        },
                                        isCartridge = isCartridge,
                                        yawDeg = yawDeg,
                                        pitchDeg = pitchDeg,
                                        paint = fillPaint,
                                    )
                                    native.restore()
                                }
                            } else {
                                // Cartridge extruded left side wall (inset smoothly so it never pokes past rounded corners)
                                drawCaseOrCartSideEdge(
                                    native = native,
                                    clipPath = clipPath,
                                    dst = d,
                                    face = q.face,
                                    subject = subject,
                                    spec = spec,
                                    baseArgb = caseColorArgb,
                                    shade = q.shade * 0.90f,
                                    polyMatrix = polyMatrix,
                                    fillPaint = fillPaint,
                                    srcPts = srcPts,
                                )
                            }
                        }

                        else -> {
                            val edgeBaseColor = when {
                                isCardboardBox && q.face == BoxFaceId.TOP -> cardboardTopColor
                                isCardboardBox && q.face == BoxFaceId.BOTTOM -> cardboardBottomColor
                                else -> caseColorArgb
                            }
                            drawCaseOrCartSideEdge(
                                native = native,
                                clipPath = clipPath,
                                dst = d,
                                face = q.face,
                                subject = subject,
                                spec = spec,
                                baseArgb = edgeBaseColor,
                                shade = q.shade,
                                polyMatrix = polyMatrix,
                                fillPaint = fillPaint,
                                srcPts = srcPts,
                            )
                        }
                    }

                    // Subtle angle-aware edge bevel on retail boxes only (never draw a sharp rectangular wireframe around rounded cartridges!)
                    if (!isCartridge) {
                        val edgeAlpha = (22 + (q.shade * 26f).roundToInt()).coerceIn(18, 48)
                        strokePaint.strokeWidth = 1.15f
                        strokePaint.color = AndroidColor.argb(edgeAlpha, 255, 255, 255)
                        native.drawPath(clipPath, strokePaint)
                    }
                }
            }
        }
    }
}

/**
 * Renders a real LaunchBox transparent-PNG cartridge scan with true alpha-extruded 3D thickness!
 * Because each depth slice is stamped directly from the real scan's alpha silhouette, notched NES
 * corners, curved SNES shoulders, and sculpted GB/N64 profiles extrude cleanly in 3D with zero
 * rectangular bounding-box outline or ghost shadow over transparent cutouts.
 */
private fun draw3dRealCartridgeScan(
    native: AndroidCanvas,
    quads: List<com.retrohero.app.data.ProjectedQuad>,
    shellArgb: Int,
    frontScanBmp: Bitmap,
    backShellBmp: Bitmap,
    halfD: Float,
    yawDeg: Float,
    pitchDeg: Float,
    polyMatrix: Matrix,
    bmpPaint: Paint,
    fillPaint: Paint,
    srcPts: FloatArray,
    useTexSilhouette: Boolean = false,
) {
    val mainFace = quads.firstOrNull { it.face == BoxFaceId.FRONT || it.face == BoxFaceId.BACK }

    if (mainFace == null) {
        // Pure edge-on cartridge profile
        val edge = quads.firstOrNull() ?: return
        val path = AndroidPath().apply {
            moveTo(edge.dstPoints[0], edge.dstPoints[1])
            lineTo(edge.dstPoints[2], edge.dstPoints[3])
            lineTo(edge.dstPoints[4], edge.dstPoints[5])
            lineTo(edge.dstPoints[6], edge.dstPoints[7])
            close()
        }
        fillPaint.shader = null
        fillPaint.color = shadeArgb(shellArgb, 0.84f)
        native.drawPath(path, fillPaint)
        return
    }

    val isFront = mainFace.face == BoxFaceId.FRONT
    val tex = if (isFront) frontScanBmp else backShellBmp
    val silhouetteBmp = if (useTexSilhouette) tex else frontScanBmp
    val bw = tex.width.toFloat()
    val bh = tex.height.toFloat()
    val sw = silhouetteBmp.width.toFloat()
    val sh = silhouetteBmp.height.toFloat()
    val d = mainFace.dstPoints

    // Compute 3D extrusion vector pointing away from the viewer along the cartridge thickness
    val yawRad = Math.toRadians(yawDeg.toDouble())
    val pitchRad = Math.toRadians(pitchDeg.toDouble())
    val totalShiftX = (-sin(yawRad).toFloat() * halfD * 1.40f) * if (isFront) 1f else -1f
    val totalShiftY = (sin(pitchRad).toFloat() * halfD * 1.10f) * if (isFront) 1f else -1f

    // Draw 7 shaded alpha-silhouette depth slices from back to front for genuine 3D molded thickness
    val slices = 7
    val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val sliceDst = FloatArray(8)
    srcPts[0] = 0f; srcPts[1] = 0f
    srcPts[2] = sw; srcPts[3] = 0f
    srcPts[4] = sw; srcPts[5] = sh
    srcPts[6] = 0f; srcPts[7] = sh

    for (i in slices downTo 1) {
        val t = i.toFloat() / slices.toFloat()
        val ox = totalShiftX * t
        val oy = totalShiftY * t
        for (pIdx in 0..3) {
            sliceDst[pIdx * 2] = d[pIdx * 2] + ox
            sliceDst[pIdx * 2 + 1] = d[pIdx * 2 + 1] + oy
        }
        if (polyMatrix.setPolyToPoly(srcPts, 0, sliceDst, 0, 4)) {
            // Subtle molded clamshell parting seam at the middle slice
            val isSeamSlice = i == (slices / 2 + 1)
            val sliceShade = if (isSeamSlice) {
                (0.54f * mainFace.shade).coerceIn(0.32f, 0.72f)
            } else {
                ((0.88f - t * 0.22f) * mainFace.shade).coerceIn(0.45f, 0.94f)
            }
            slicePaint.colorFilter = android.graphics.PorterDuffColorFilter(
                shadeArgb(shellArgb, sliceShade),
                PorterDuff.Mode.SRC_IN,
            )
            native.save()
            native.concat(polyMatrix)
            native.drawBitmap(silhouetteBmp, 0f, 0f, slicePaint)
            native.restore()
        }
    }
    slicePaint.colorFilter = null

    // Draw the crisp front scan (or pre-masked rear shell) directly with GPU-native LightingColorFilter
    // (NO saveLayer inside a perspective matrix, which can clip/corrupt on hardware-accelerated Android GPUs!)
    srcPts[0] = 0f; srcPts[1] = 0f
    srcPts[2] = bw; srcPts[3] = 0f
    srcPts[4] = bw; srcPts[5] = bh
    srcPts[6] = 0f; srcPts[7] = bh
    if (polyMatrix.setPolyToPoly(srcPts, 0, d, 0, 4)) {
        val mul = (mainFace.shade.coerceIn(0.68f, 1.0f) * 255f).roundToInt().coerceIn(170, 255)
        val add = (mainFace.specular * 22f).roundToInt().coerceIn(0, 24)
        bmpPaint.colorFilter = android.graphics.LightingColorFilter(
            AndroidColor.rgb(mul, mul, mul),
            AndroidColor.rgb(add, add, add),
        )
        native.save()
        native.concat(polyMatrix)
        native.drawBitmap(tex, 0f, 0f, bmpPaint)
        native.restore()
        bmpPaint.colorFilter = null
    }
}

/** Pre-masks a procedural cartridge rear shell to the exact alpha silhouette of a real LaunchBox scan once. */
private fun maskBitmapToAlphaSilhouette(shellBmp: Bitmap, silhouetteBmp: Bitmap): Bitmap {
    val w = shellBmp.width.coerceAtLeast(16)
    val h = shellBmp.height.coerceAtLeast(16)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(out)
    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    c.drawBitmap(silhouetteBmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), p)
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    c.drawBitmap(shellBmp, 0f, 0f, p)
    p.xfermode = null
    return out
}

/** Samples the real molded plastic shell color from the outer edges of a LaunchBox cartridge PNG. */
private fun sampleRealCartShellArgb(bmp: Bitmap, fallbackArgb: Int): Int {
    val w = bmp.width
    val h = bmp.height
    if (w <= 8 || h <= 8) return fallbackArgb
    // Sample left & right outer plastic shoulder strips (away from center art sticker)
    val sampleCoords = arrayOf(
        0.08f to 0.20f, 0.08f to 0.45f, 0.08f to 0.70f,
        0.92f to 0.20f, 0.92f to 0.45f, 0.92f to 0.70f,
        0.50f to 0.05f, 0.50f to 0.95f,
    )
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0
    for ((fx, fy) in sampleCoords) {
        val px = bmp.getPixel((w * fx).roundToInt().coerceIn(0, w - 1), (h * fy).roundToInt().coerceIn(0, h - 1))
        if (AndroidColor.alpha(px) > 200) {
            rSum += AndroidColor.red(px)
            gSum += AndroidColor.green(px)
            bSum += AndroidColor.blue(px)
            count++
        }
    }
    if (count < 2) return fallbackArgb
    return AndroidColor.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
}

/** Samples the average cardstock color along the top or bottom edge of a box cover bitmap. */
private fun sampleBitmapEdgeArgb(bmp: Bitmap, topEdge: Boolean): Int {
    val w = bmp.width
    val h = bmp.height
    if (w <= 2 || h <= 2) return AndroidColor.rgb(32, 34, 42)
    val y = if (topEdge) (h * 0.03f).roundToInt().coerceIn(0, h - 1) else (h * 0.97f).roundToInt().coerceIn(0, h - 1)
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0
    val samples = 12
    for (i in 1..samples) {
        val x = (w * i / (samples + 1)).coerceIn(0, w - 1)
        val px = bmp.getPixel(x, y)
        if (AndroidColor.alpha(px) > 128) {
            rSum += AndroidColor.red(px)
            gSum += AndroidColor.green(px)
            bSum += AndroidColor.blue(px)
            count++
        }
    }
    if (count == 0) return AndroidColor.rgb(32, 34, 42)
    return AndroidColor.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
}

private fun buildUmdShellPath(bw: Float, bh: Float): AndroidPath {
    // Authentic Sony PSP UMD protective shell silhouette: semicircular top dome + straight sides + squared beveled bottom!
    val r = bw * 0.485f
    val cx = bw * 0.50f
    val domeCy = bh * 0.495f
    val left = cx - r
    val right = cx + r
    val bottom = bh * 0.965f
    val corner = bw * 0.055f
    return AndroidPath().apply {
        // Upper semicircular arc from right (0°) counter-clockwise to left (180°)
        arcTo(RectF(left, domeCy - r, right, domeCy + r), 0f, -180f, true)
        lineTo(left, bottom - corner)
        quadTo(left, bottom, left + corner, bottom)
        lineTo(right - corner, bottom)
        quadTo(right, bottom, right, bottom - corner)
        close()
    }
}

private fun draw3dOpticalDisc(
    native: AndroidCanvas,
    quads: List<com.retrohero.app.data.ProjectedQuad>,
    spec: PhysicalMediaSpec,
    labelBmp: Bitmap,
    undersideBmp: Bitmap,
    yawDeg: Float,
    pitchDeg: Float,
    polyMatrix: Matrix,
    bmpPaint: Paint,
    fillPaint: Paint,
    srcPts: FloatArray,
) {
    // Draw main visible disc face (FRONT = printed label, BACK = polycarbonate data underside)
    val mainFace = quads.firstOrNull { it.face == BoxFaceId.FRONT || it.face == BoxFaceId.BACK }
    if (mainFace == null) {
        // Pure edge-on polycarbonate rim
        val edge = quads.firstOrNull() ?: return
        val path = AndroidPath().apply {
            moveTo(edge.dstPoints[0], edge.dstPoints[1])
            lineTo(edge.dstPoints[2], edge.dstPoints[3])
            lineTo(edge.dstPoints[4], edge.dstPoints[5])
            lineTo(edge.dstPoints[6], edge.dstPoints[7])
            close()
        }
        fillPaint.shader = null
        fillPaint.color = if (spec.blackDiscUnderside) {
            AndroidColor.rgb(28, 30, 36)
        } else {
            AndroidColor.argb(235, 224, 232, 244)
        }
        native.drawPath(path, fillPaint)
        return
    }

    val isUmd = spec.mediaKind == PhysicalMediaKind.UMD_DISC
    val isMiniGc = spec.mediaKind == PhysicalMediaKind.MINI_DISC_GC
    val isLabelSide = mainFace.face == BoxFaceId.FRONT
    val tex = if (isLabelSide) labelBmp else undersideBmp
    val bw = tex.width.toFloat()
    val bh = tex.height.toFloat()
    srcPts[0] = 0f; srcPts[1] = 0f
    srcPts[2] = bw; srcPts[3] = 0f
    srcPts[4] = bw; srcPts[5] = bh
    srcPts[6] = 0f; srcPts[7] = bh

    val d = mainFace.dstPoints

    if (isUmd) {
        // ---- AUTHENTIC ENCASED SONY PSP UMD CARTRIDGE (Clear-White Shell + 60mm Inner Disc + Steel Magnetic Hub) ----
        val shellPath = buildUmdShellPath(bw, bh)
        val rimShiftX = -sin(Math.toRadians(yawDeg.toDouble())).toFloat() * 8.5f
        val rimShiftY = sin(Math.toRadians(pitchDeg.toDouble())).toFloat() * 6.0f
        for (slice in 3 downTo 1) {
            val t = slice / 3f
            val rimDst = floatArrayOf(
                d[0] + rimShiftX * t, d[1] + rimShiftY * t,
                d[2] + rimShiftX * t, d[3] + rimShiftY * t,
                d[4] + rimShiftX * t, d[5] + rimShiftY * t,
                d[6] + rimShiftX * t, d[7] + rimShiftY * t,
            )
            if (polyMatrix.setPolyToPoly(srcPts, 0, rimDst, 0, 4)) {
                native.save()
                native.concat(polyMatrix)
                fillPaint.shader = null
                fillPaint.color = if (slice == 2) AndroidColor.rgb(186, 194, 208) else AndroidColor.rgb(228, 234, 244)
                native.drawPath(shellPath, fillPaint)
                native.restore()
            }
        }

        if (polyMatrix.setPolyToPoly(srcPts, 0, d, 0, 4)) {
            native.save()
            native.concat(polyMatrix)
            native.clipPath(shellPath)

            // Outer translucent-pearl UMD polycarbonate shell body
            fillPaint.shader = LinearGradient(
                0f, 0f, bw, bh,
                intArrayOf(
                    AndroidColor.rgb(246, 249, 254),
                    AndroidColor.rgb(218, 224, 235),
                    AndroidColor.rgb(196, 204, 218),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            native.drawPath(shellPath, fillPaint)
            fillPaint.shader = null

            // Inner circular cavity recess holding the 60mm optical mini-disc
            val cx = bw * 0.50f
            val cy = bh * 0.50f
            val innerR = bw * 0.432f
            fillPaint.color = AndroidColor.rgb(22, 25, 32)
            native.drawCircle(cx, cy, innerR + bw * 0.012f, fillPaint)

            // Draw the 60mm optical mini-disc inside the clear protective UMD window!
            val innerDiscClip = AndroidPath().apply {
                addCircle(cx, cy, innerR, AndroidPath.Direction.CW)
            }
            native.save()
            native.clipPath(innerDiscClip)
            val discRect = RectF(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
            native.drawBitmap(tex, null, discRect, bmpPaint)

            // Subtle specular gloss sweep across the enclosed UMD disc
            val sweepAngle = (yawDeg * 1.5f + pitchDeg * 1.1f)
            native.save()
            native.rotate(sweepAngle, cx, cy)
            fillPaint.shader = SweepGradient(
                cx, cy,
                if (isLabelSide) {
                    intArrayOf(
                        AndroidColor.argb(0, 255, 255, 255),
                        AndroidColor.argb(66, 255, 255, 255),
                        AndroidColor.argb(0, 255, 255, 255),
                        AndroidColor.argb(46, 255, 255, 255),
                        AndroidColor.argb(0, 255, 255, 255),
                    )
                } else {
                    intArrayOf(
                        AndroidColor.argb(0, 255, 255, 255),
                        AndroidColor.argb(95, 70, 225, 255),
                        AndroidColor.argb(85, 255, 110, 210),
                        AndroidColor.argb(85, 255, 225, 95),
                        AndroidColor.argb(0, 255, 255, 255),
                    )
                },
                null,
            )
            native.drawRect(discRect, fillPaint)
            fillPaint.shader = null
            native.restore()
            native.restore()

            if (isLabelSide) {
                // Translucent acrylic top UMD shell overlay sheen + white outer frame ring
                val shellStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = bw * 0.018f
                    color = AndroidColor.argb(215, 245, 248, 255)
                }
                native.drawCircle(cx, cy, innerR + bw * 0.008f, shellStroke)

                // Iconic brushed stainless-steel magnetic clamping puck in the center of a real PSP UMD!
                val hubR = bw * 0.125f
                fillPaint.color = AndroidColor.rgb(34, 38, 46)
                native.drawCircle(cx, cy, hubR + bw * 0.018f, fillPaint)
                fillPaint.shader = android.graphics.RadialGradient(
                    cx - hubR * 0.25f, cy - hubR * 0.25f, hubR * 1.35f,
                    intArrayOf(
                        AndroidColor.rgb(248, 251, 255),
                        AndroidColor.rgb(188, 196, 210),
                        AndroidColor.rgb(122, 130, 146),
                        AndroidColor.rgb(218, 224, 236),
                    ),
                    floatArrayOf(0f, 0.45f, 0.78f, 1f),
                    Shader.TileMode.CLAMP,
                )
                native.drawCircle(cx, cy, hubR, fillPaint)
                fillPaint.shader = null
                // Center steel spindle dimple + concentric machined rings
                val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = bw * 0.007f
                    color = AndroidColor.argb(140, 68, 76, 92)
                }
                native.drawCircle(cx, cy, hubR * 0.68f, ringP)
                fillPaint.color = AndroidColor.rgb(72, 78, 90)
                native.drawCircle(cx, cy, hubR * 0.28f, fillPaint)
                fillPaint.color = AndroidColor.rgb(225, 232, 242)
                native.drawCircle(cx - hubR * 0.08f, cy - hubR * 0.08f, hubR * 0.10f, fillPaint)

                // Molded white bottom corner bevel panels & subtle "UMD" mark on lower-left lip
                val umdTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.rgb(88, 96, 112)
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = bw * 0.042f
                    letterSpacing = 0.10f
                    textAlign = Paint.Align.CENTER
                }
                native.drawText("UMD", bw * 0.145f, bh * 0.935f, umdTp)
                native.drawText("PSP", bw * 0.855f, bh * 0.935f, umdTp)
            } else {
                // Underside UMD shutter window framing the lower optical read slot
                fillPaint.color = AndroidColor.argb(215, 228, 233, 242)
                val maskRect = RectF(0f, 0f, bw, bh * 0.46f)
                native.drawRect(maskRect, fillPaint)
                // Brushed steel center clamp puck also visible on rear!
                val hubR = bw * 0.130f
                fillPaint.shader = android.graphics.RadialGradient(
                    cx - hubR * 0.25f, cy - hubR * 0.25f, hubR * 1.3f,
                    intArrayOf(AndroidColor.rgb(245, 248, 255), AndroidColor.rgb(156, 164, 180), AndroidColor.rgb(112, 120, 136)),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP,
                )
                native.drawCircle(cx, cy, hubR, fillPaint)
                fillPaint.shader = null
            }

            // Crisp glossy acrylic diagonal reflection across the whole clear UMD case!
            fillPaint.shader = LinearGradient(
                0f, 0f, bw, bh * 0.85f,
                intArrayOf(
                    AndroidColor.argb(72, 255, 255, 255),
                    AndroidColor.argb(14, 255, 255, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(40, 255, 255, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                ),
                floatArrayOf(0f, 0.24f, 0.48f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
            native.drawPath(shellPath, fillPaint)
            fillPaint.shader = null

            // Outer clear-white shell edge rim highlight
            val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = bw * 0.016f
                color = AndroidColor.argb(190, 255, 255, 255)
            }
            native.drawPath(shellPath, outerStroke)
            native.restore()
        }
        return
    }

    // ---- STANDARD 12cm CD/DVD & 8cm GAMECUBE MINI-DVD ----
    val holeFrac = if (isMiniGc) 0.118f else 0.084f
    val hubRingInner = if (isMiniGc) 0.145f else 0.110f
    val hubRingOuter = if (isMiniGc) 0.235f else 0.188f

    // Multi-slice extruded polycarbonate thickness rim behind the disc
    val rimShiftX = -sin(Math.toRadians(yawDeg.toDouble())).toFloat() * 6.0f
    val rimShiftY = sin(Math.toRadians(pitchDeg.toDouble())).toFloat() * 4.5f
    val rimDst = floatArrayOf(
        d[0] + rimShiftX, d[1] + rimShiftY,
        d[2] + rimShiftX, d[3] + rimShiftY,
        d[4] + rimShiftX, d[5] + rimShiftY,
        d[6] + rimShiftX, d[7] + rimShiftY,
    )
    if (polyMatrix.setPolyToPoly(srcPts, 0, rimDst, 0, 4)) {
        native.save()
        native.concat(polyMatrix)
        fillPaint.shader = null
        fillPaint.color = if (spec.blackDiscUnderside) AndroidColor.rgb(24, 26, 34) else AndroidColor.rgb(198, 208, 224)
        val discPath = AndroidPath().apply {
            fillType = AndroidPath.FillType.EVEN_ODD
            addCircle(bw * 0.5f, bh * 0.5f, bw * 0.495f, AndroidPath.Direction.CW)
            addCircle(bw * 0.5f, bh * 0.5f, bw * holeFrac, AndroidPath.Direction.CW)
        }
        native.drawPath(discPath, fillPaint)
        native.restore()
    }

    if (polyMatrix.setPolyToPoly(srcPts, 0, d, 0, 4)) {
        native.save()
        native.concat(polyMatrix)
        val cx = bw * 0.5f
        val cy = bh * 0.5f
        val discClip = AndroidPath().apply {
            fillType = AndroidPath.FillType.EVEN_ODD
            addCircle(cx, cy, bw * 0.495f, AndroidPath.Direction.CW)
            addCircle(cx, cy, bw * holeFrac, AndroidPath.Direction.CW)
        }
        native.clipPath(discClip)
        native.drawBitmap(tex, 0f, 0f, bmpPaint)

        // Live rainbow diffraction / metallic specular reflection sweep on the optical disc!
        val sweepAngle = (yawDeg * 1.6f + pitchDeg * 1.2f)
        native.save()
        native.rotate(sweepAngle, cx, cy)
        fillPaint.shader = SweepGradient(
            cx,
            cy,
            if (isLabelSide) {
                intArrayOf(
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(72, 255, 255, 255),
                    AndroidColor.argb(18, 210, 232, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(56, 255, 255, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                )
            } else if (spec.blackDiscUnderside) {
                // PS1 iconic jet-black polycarbonate indigo sheen
                intArrayOf(
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(105, 195, 215, 255),
                    AndroidColor.argb(42, 175, 125, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(92, 215, 230, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                )
            } else {
                // Iridescent CD/DVD diffraction spectrum
                intArrayOf(
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(96, 0, 230, 255),
                    AndroidColor.argb(90, 120, 255, 150),
                    AndroidColor.argb(90, 255, 210, 70),
                    AndroidColor.argb(86, 255, 80, 200),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(92, 80, 210, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                )
            },
            null,
        )
        native.drawRect(0f, 0f, bw, bh, fillPaint)
        fillPaint.shader = null
        native.restore()

        // Authentic multi-ring polycarbonate center clamping hub + mirror-silver IFPI matrix band!
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        // 1. Outer translucent stacking ridge ring
        hubPaint.strokeWidth = bw * 0.016f
        hubPaint.color = AndroidColor.argb(150, 240, 246, 255)
        native.drawCircle(cx, cy, bw * hubRingOuter, hubPaint)
        hubPaint.strokeWidth = bw * 0.008f
        hubPaint.color = AndroidColor.argb(110, 16, 20, 28)
        native.drawCircle(cx, cy, bw * (hubRingOuter + 0.010f), hubPaint)

        // 2. Mirror-silver inner metallic clamping band (with subtle IFPI matrix barcode ticks)
        val bandMidR = bw * ((hubRingInner + hubRingOuter) * 0.5f)
        val bandW = bw * (hubRingOuter - hubRingInner)
        hubPaint.strokeWidth = bandW
        hubPaint.color = if (spec.blackDiscUnderside && !isLabelSide) {
            AndroidColor.argb(165, 42, 46, 58)
        } else {
            AndroidColor.argb(165, 214, 222, 236)
        }
        native.drawCircle(cx, cy, bandMidR, hubPaint)

        // Subtle radial IFPI micro-etched matrix ticks around the mirror clamping ring
        hubPaint.strokeWidth = bw * 0.004f
        hubPaint.color = AndroidColor.argb(85, 65, 76, 96)
        for (i in 0 until 28) {
            val ang = Math.toRadians(i * (360.0 / 28.0))
            val r0 = bw * (hubRingInner + 0.012f)
            val r1 = bw * (hubRingOuter - 0.016f)
            native.drawLine(
                cx + cos(ang).toFloat() * r0,
                cy + sin(ang).toFloat() * r0,
                cx + cos(ang).toFloat() * r1,
                cy + sin(ang).toFloat() * r1,
                hubPaint,
            )
        }

        // 3. Clear polycarbonate inner collar + crisp beveled spindle hole catch-light
        hubPaint.strokeWidth = bw * (hubRingInner - holeFrac)
        hubPaint.color = AndroidColor.argb(135, 236, 242, 252)
        native.drawCircle(cx, cy, bw * ((holeFrac + hubRingInner) * 0.5f), hubPaint)

        hubPaint.strokeWidth = bw * 0.007f
        hubPaint.color = AndroidColor.argb(170, 255, 255, 255)
        native.drawCircle(cx, cy, bw * (holeFrac + 0.004f), hubPaint)

        // 4. Crisp outer beveled clear polycarbonate disc edge catch-light
        hubPaint.strokeWidth = bw * 0.011f
        hubPaint.color = if (spec.blackDiscUnderside) {
            AndroidColor.argb(145, 190, 200, 218)
        } else {
            AndroidColor.argb(190, 242, 248, 255)
        }
        native.drawCircle(cx, cy, bw * 0.490f, hubPaint)

        native.restore()
    }
}

private fun drawFaceLightingAndGloss(
    native: AndroidCanvas,
    bw: Float,
    bh: Float,
    shade: Float,
    specular: Float,
    glossy: Boolean,
    isCartridge: Boolean = false,
    yawDeg: Float,
    pitchDeg: Float,
    paint: Paint,
) {
    paint.shader = null
    if (shade < 0.98f) {
        val darkAlpha = ((1f - shade) * 185f).roundToInt().coerceIn(0, 130)
        if (darkAlpha > 2) {
            paint.color = AndroidColor.argb(darkAlpha, 6, 8, 14)
            native.drawRect(0f, 0f, bw, bh, paint)
        }
    }

    // Subtle overhead studio softbox falloff (warm top catch-light -> gentle bottom grounding)
    paint.shader = LinearGradient(
        0f, 0f, 0f, bh,
        intArrayOf(
            AndroidColor.argb(if (isCartridge) 20 else 16, 255, 252, 245),
            AndroidColor.TRANSPARENT,
            AndroidColor.argb(if (isCartridge) 26 else 22, 4, 6, 12),
        ),
        floatArrayOf(0f, 0.32f, 1f),
        Shader.TileMode.CLAMP,
    )
    native.drawRect(0f, 0f, bw, bh, paint)
    paint.shader = null

    // Dynamic moving studio specular reflection band coupled to yaw/pitch
    if (glossy || specular > 0.05f) {
        val sheenCenter = ((yawDeg % 90f) / 90f + 0.5f).coerceIn(0.06f, 0.94f)
        val bandW = bw * if (isCartridge) 0.44f else 0.36f
        val x0 = bw * sheenCenter - bandW + pitchDeg * 2.2f
        val x1 = x0 + bandW * 2f
        val peakAlpha = when {
            isCartridge -> (22f + specular * 38f).roundToInt().coerceIn(14, 58)
            glossy -> (44f + specular * 54f).roundToInt().coerceIn(22, 94)
            else -> (18f + specular * 32f).roundToInt().coerceIn(10, 48)
        }
        paint.shader = LinearGradient(
            x0,
            0f,
            x1,
            bh * 0.58f,
            intArrayOf(
                AndroidColor.TRANSPARENT,
                AndroidColor.argb((peakAlpha * 0.45f).roundToInt(), 255, 255, 255),
                AndroidColor.argb(peakAlpha, 255, 255, 255),
                AndroidColor.argb((peakAlpha * 0.35f).roundToInt(), 255, 255, 255),
                AndroidColor.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.34f, 0.50f, 0.66f, 1f),
            Shader.TileMode.CLAMP,
        )
        native.drawRect(0f, 0f, bw, bh, paint)
        paint.shader = null
    }
}

private fun drawSolidExtrudedEdge(
    native: AndroidCanvas,
    clipPath: AndroidPath,
    dst: FloatArray,
    baseArgb: Int,
    shade: Float,
    fillPaint: Paint,
) {
    fillPaint.shader = LinearGradient(
        dst[0], dst[1], dst[4], dst[5],
        shadeArgb(baseArgb, shade * 1.06f),
        shadeArgb(baseArgb, shade * 0.80f),
        Shader.TileMode.CLAMP,
    )
    native.drawPath(clipPath, fillPaint)
    fillPaint.shader = null
}

private fun drawCaseOrCartSideEdge(
    native: AndroidCanvas,
    clipPath: AndroidPath,
    dst: FloatArray,
    face: BoxFaceId,
    subject: PhysicalViewerSubject,
    spec: PhysicalMediaSpec,
    baseArgb: Int,
    shade: Float,
    polyMatrix: Matrix,
    fillPaint: Paint,
    srcPts: FloatArray,
) {
    val ew = 120f
    val eh = 600f
    srcPts[0] = 0f; srcPts[1] = 0f
    srcPts[2] = ew; srcPts[3] = 0f
    srcPts[4] = ew; srcPts[5] = eh
    srcPts[6] = 0f; srcPts[7] = eh

    if (!polyMatrix.setPolyToPoly(srcPts, 0, dst, 0, 4)) {
        drawSolidExtrudedEdge(native, clipPath, dst, baseArgb, shade, fillPaint)
        return
    }

    native.save()
    native.clipPath(clipPath)
    native.concat(polyMatrix)
    fillPaint.shader = null

    if (subject == PhysicalViewerSubject.GAME_MEDIA) {
        // Inset slightly at both ends so extruded cartridge walls blend cleanly into the rounded shell corners
        val edgeInsetY = eh * 0.032f
        val edgeRect = RectF(0f, edgeInsetY, ew, eh - edgeInsetY)
        fillPaint.shader = LinearGradient(
            0f, 0f, ew, 0f,
            intArrayOf(
                shadeArgb(baseArgb, shade * 1.04f),
                shadeArgb(baseArgb, shade * 0.90f),
                shadeArgb(baseArgb, shade * 0.80f),
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP,
        )
        native.drawRoundRect(edgeRect, 10f, 10f, fillPaint)
        fillPaint.shader = null

        // Crisp two-piece clamshell mold parting seam + subtle catch-light
        fillPaint.color = AndroidColor.argb(95, 0, 0, 0)
        native.drawRect(ew * 0.47f, edgeInsetY + 4f, ew * 0.52f, eh - edgeInsetY - 4f, fillPaint)
        fillPaint.color = AndroidColor.argb(45, 255, 255, 255)
        native.drawRect(ew * 0.52f, edgeInsetY + 4f, ew * 0.55f, eh - edgeInsetY - 4f, fillPaint)

        if (face == BoxFaceId.BOTTOM) {
            // Recessed cartridge slot + gleaming gold PCB edge connector pins!
            fillPaint.color = AndroidColor.rgb(18, 22, 20)
            native.drawRoundRect(RectF(ew * 0.18f, eh * 0.08f, ew * 0.82f, eh * 0.92f), 8f, 8f, fillPaint)
            fillPaint.color = AndroidColor.rgb(38, 92, 52) // green PCB board
            native.drawRect(ew * 0.44f, eh * 0.11f, ew * 0.56f, eh * 0.89f, fillPaint)
            fillPaint.color = AndroidColor.rgb(236, 194, 76) // gleaming gold connector teeth
            val pins = 20
            val step = (eh * 0.76f) / pins
            for (i in 0 until pins) {
                val py = eh * 0.12f + i * step
                native.drawRect(ew * 0.38f, py, ew * 0.62f, py + step * 0.58f, fillPaint)
            }
        }
    } else if (spec.caseStyle.hasPlasticRim) {
        if (spec.caseStyle == RetailCaseStyle.JEWEL_CASE_CD) {
            // Clear polystyrene acrylic outer lid + dark ribbed inner CD tray sandwiched inside!
            fillPaint.shader = LinearGradient(
                0f, 0f, ew, 0f,
                intArrayOf(
                    AndroidColor.argb(230, 210, 222, 240),
                    AndroidColor.rgb(26, 28, 36),
                    AndroidColor.rgb(18, 20, 26),
                    AndroidColor.argb(225, 200, 214, 236),
                ),
                floatArrayOf(0f, 0.22f, 0.78f, 1f),
                Shader.TileMode.CLAMP,
            )
            native.drawRect(0f, 0f, ew, eh, fillPaint)
            fillPaint.shader = null
            // Fine acrylic edge highlights
            fillPaint.color = AndroidColor.argb(90, 255, 255, 255)
            native.drawRect(ew * 0.16f, 0f, ew * 0.20f, eh, fillPaint)
            native.drawRect(ew * 0.80f, 0f, ew * 0.84f, eh, fillPaint)
        } else {
            // Molded DVD / Clamshell / DS / Switch keep-case opening edge with 3D thumb-grip cutout & dual-shell seam
            fillPaint.shader = LinearGradient(
                0f, 0f, ew, 0f,
                intArrayOf(
                    shadeArgb(baseArgb, shade * 1.06f),
                    shadeArgb(baseArgb, shade * 0.92f),
                    shadeArgb(baseArgb, shade * 0.82f),
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP,
            )
            native.drawRect(0f, 0f, ew, eh, fillPaint)
            fillPaint.shader = null
            // Recessed interior dual-shell parting groove + edge catch-light
            fillPaint.color = AndroidColor.argb(135, 6, 8, 12)
            native.drawRect(ew * 0.40f, eh * 0.015f, ew * 0.60f, eh * 0.985f, fillPaint)
            fillPaint.color = AndroidColor.argb(46, 255, 255, 255)
            native.drawRect(ew * 0.60f, eh * 0.015f, ew * 0.64f, eh * 0.985f, fillPaint)
            if (face == BoxFaceId.RIGHT_OPENING) {
                // Sculpted 3D keep-case thumb-grip recess in the center of the right opening edge!
                val gripRect = RectF(ew * 0.16f, eh * 0.39f, ew * 0.84f, eh * 0.61f)
                fillPaint.color = shadeArgb(baseArgb, shade * 0.58f)
                native.drawRoundRect(gripRect, 16f, 16f, fillPaint)
                fillPaint.color = AndroidColor.argb(55, 255, 255, 255)
                native.drawRoundRect(RectF(ew * 0.20f, eh * 0.595f, ew * 0.80f, eh * 0.612f), 6f, 6f, fillPaint)
            }
        }
    } else {
        // Printed retail cardboard box top/bottom tuck flap matched to the real box cover cardstock color!
        fillPaint.shader = LinearGradient(
            0f, 0f, ew, 0f,
            intArrayOf(
                shadeArgb(baseArgb, shade * 1.08f),
                shadeArgb(baseArgb, shade * 0.92f),
                shadeArgb(baseArgb, shade * 0.78f),
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP,
        )
        native.drawRect(0f, 0f, ew, eh, fillPaint)
        fillPaint.shader = null
        // Authentic cardboard tuck-flap seam, inner side-ear seams & center thumb-tab notch
        fillPaint.color = AndroidColor.argb(115, 0, 0, 0)
        native.drawRect(ew * 0.14f, eh * 0.02f, ew * 0.18f, eh * 0.98f, fillPaint)
        fillPaint.color = AndroidColor.argb(42, 255, 250, 240)
        native.drawRect(ew * 0.18f, eh * 0.02f, ew * 0.21f, eh * 0.98f, fillPaint)
        // Left & right inner cardboard ear-flap fold seams
        fillPaint.color = AndroidColor.argb(72, 0, 0, 0)
        native.drawRect(ew * 0.18f, eh * 0.06f, ew * 0.92f, eh * 0.072f, fillPaint)
        native.drawRect(ew * 0.18f, eh * 0.928f, ew * 0.92f, eh * 0.94f, fillPaint)
        // Subtle semi-circular thumb cutout on the tuck flap
        fillPaint.color = AndroidColor.argb(92, 0, 0, 0)
        native.drawOval(RectF(ew * 0.06f, eh * 0.44f, ew * 0.24f, eh * 0.56f), fillPaint)
    }

    native.restore()
}

private fun shadeArgb(argb: Int, factor: Float): Int {
    val a = AndroidColor.alpha(argb)
    val r = (AndroidColor.red(argb) * factor).roundToInt().coerceIn(0, 255)
    val g = (AndroidColor.green(argb) * factor).roundToInt().coerceIn(0, 255)
    val b = (AndroidColor.blue(argb) * factor).roundToInt().coerceIn(0, 255)
    return AndroidColor.argb(a, r, g, b)
}

// ---- Cached Parallel Texture Loader & Procedural Museum-Grade Fallbacks -------------------------

/**
 * Bounded in-memory RAM cache for full 3D Case + Cartridge/Disc textures so reopening Game Info,
 * tapping the 3D Case Inspector, or pulling a case from the 3D Bookshelf is instant (0 ms).
 */
private object PhysicalTexturesCache {
    private val cache = object : android.util.LruCache<String, ResolvedPhysicalTextures>(48 * 1024) {
        override fun sizeOf(key: String, value: ResolvedPhysicalTextures): Int {
            val seen = java.util.IdentityHashMap<Bitmap, Boolean>(6)
            var bytes = 0
            fun add(b: Bitmap) {
                if (seen.put(b, true) == null) bytes += b.byteCount
            }
            add(value.front)
            add(value.back)
            add(value.spine)
            add(value.mediaFront)
            add(value.mediaBack)
            add(value.maskedMediaBack)
            return (bytes / 1024).coerceAtLeast(1)
        }
    }

    fun get(key: String): ResolvedPhysicalTextures? = cache.get(key)
    fun put(key: String, value: ResolvedPhysicalTextures) { cache.put(key, value) }
}

private val instantStubCache = java.util.concurrent.ConcurrentHashMap<String, ResolvedPhysicalTextures>()

/** Tiny (< 8 KB, < 0.05 ms) first-frame stub so clicking a cold 3D case never stalls the UI thread. */
private fun buildInstantStubTextures(spec: PhysicalMediaSpec): ResolvedPhysicalTextures {
    val key = "${spec.systemId}|${spec.spineHeaderColor.toArgb()}|${spec.mediaShellColor.toArgb()}"
    return instantStubCache.getOrPut(key) {
        val w = 32
        val h = (w / spec.defaultBoxAspect).roundToInt().coerceIn(22, 48)
        val headerArgb = spec.spineHeaderColor.toArgb()
        val bodyArgb = shadeArgb(headerArgb, 0.34f)
        val front = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            val c = AndroidCanvas(this)
            c.drawColor(bodyArgb)
            val p = Paint().apply { color = headerArgb }
            c.drawRect(0f, 0f, w.toFloat(), h * 0.14f, p)
        }
        val spine = Bitmap.createBitmap(12, h, Bitmap.Config.ARGB_8888).apply {
            val c = AndroidCanvas(this)
            c.drawColor(bodyArgb)
            val p = Paint().apply { color = headerArgb }
            c.drawRect(0f, 0f, 12f, h * 0.18f, p)
        }
        val media = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(spec.mediaShellColor.toArgb())
        }
        ResolvedPhysicalTextures(
            front = front,
            back = front,
            spine = spine,
            mediaFront = media,
            mediaBack = media,
            hasRealFront = false,
            hasRealBack = false,
            hasRealSpine = false,
            hasRealMedia = false,
            maskedMediaBack = media,
        )
    }
}

private fun ShelfCaseArt.toSeedTextures(spec: PhysicalMediaSpec): ResolvedPhysicalTextures {
    val stub = buildInstantStubTextures(spec)
    return ResolvedPhysicalTextures(
        front = textures.front,
        back = stub.back,
        spine = textures.spine,
        mediaFront = stub.mediaFront,
        mediaBack = stub.mediaBack,
        hasRealFront = textures.hasRealFront,
        hasRealBack = false,
        hasRealSpine = textures.hasRealSpine,
        hasRealMedia = false,
        maskedMediaBack = stub.maskedMediaBack,
    )
}

private suspend fun loadFirstPhysicalBitmap(
    context: Context,
    candidates: List<Any>,
    maxPx: Int,
): Bitmap? {
    if (candidates.isEmpty()) return null
    for (src in candidates) {
        if (src is java.io.File) {
            val exists = withContext(Dispatchers.IO) { src.exists() && src.length() > 0L }
            if (!exists) continue
        }
        val loaded = runCatching {
            val req = ImageRequest.Builder(context)
                .data(src)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .size(maxPx, maxPx)
                .scale(Scale.FIT)
                .build()
            val res = context.imageLoader.execute(req)
            val raw = (res as? SuccessResult)?.drawable?.toBitmap(config = Bitmap.Config.ARGB_8888)
            if (raw != null && raw.width >= 16 && raw.height >= 16) {
                withContext(Dispatchers.Default) { trimTransparentBounds(raw) }
            } else {
                null
            }
        }.getOrNull()
        if (loaded != null && loaded.width >= 16 && loaded.height >= 16) {
            return loaded
        }
    }
    return null
}

/**
 * Trims invisible all-transparent outer padding from LaunchBox PNG scans (common on Box - Front,
 * Box - Spine, and Cart - Front assets) so artwork fills 100% of the 3D box face and cartridge
 * silhouette edge-to-edge without floating borders or shrunk textures.
 */
private fun trimTransparentBounds(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    if (w < 24 || h < 24 || !src.hasAlpha()) return src
    val alphaThreshold = 18

    // O(1) perimeter fast-path: full-bleed JPEGs & unpadded retail box scans have opaque outer edges,
    // letting us skip allocating & scanning a multi-megabyte IntArray altogether!
    val topOpaque = (src.getPixel(w / 4, 1) ushr 24) > alphaThreshold ||
        (src.getPixel(w / 2, 1) ushr 24) > alphaThreshold ||
        (src.getPixel((w * 3) / 4, 1) ushr 24) > alphaThreshold
    val botOpaque = (src.getPixel(w / 4, h - 2) ushr 24) > alphaThreshold ||
        (src.getPixel(w / 2, h - 2) ushr 24) > alphaThreshold ||
        (src.getPixel((w * 3) / 4, h - 2) ushr 24) > alphaThreshold
    val leftOpaque = (src.getPixel(1, h / 4) ushr 24) > alphaThreshold ||
        (src.getPixel(1, h / 2) ushr 24) > alphaThreshold ||
        (src.getPixel(1, (h * 3) / 4) ushr 24) > alphaThreshold
    val rightOpaque = (src.getPixel(w - 2, h / 4) ushr 24) > alphaThreshold ||
        (src.getPixel(w - 2, h / 2) ushr 24) > alphaThreshold ||
        (src.getPixel(w - 2, (h * 3) / 4) ushr 24) > alphaThreshold
    if (topOpaque && botOpaque && leftOpaque && rightOpaque) return src

    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1
    val step = if (max(w, h) > 512) 4 else 2
    for (y in 0 until h step step) {
        val rowOffset = y * w
        for (x in 0 until w step step) {
            if ((pixels[rowOffset + x] ushr 24) > alphaThreshold) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX <= minX || maxY <= minY) return src
    val pad = step
    val left = (minX - pad).coerceAtLeast(0)
    val top = (minY - pad).coerceAtLeast(0)
    val right = (maxX + pad + 1).coerceAtMost(w)
    val bottom = (maxY + pad + 1).coerceAtMost(h)
    val cropW = right - left
    val cropH = bottom - top
    // Only crop if there is meaningful transparent margin (> 2%) and resulting image is valid
    if (cropW >= (w * 0.98f).toInt() && cropH >= (h * 0.98f).toInt()) return src
    if (cropW < 16 || cropH < 16) return src
    return runCatching { Bitmap.createBitmap(src, left, top, cropW, cropH) }.getOrDefault(src)
}

private fun assembleResolvedTextures(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    loadedFront: Bitmap?,
    loadedBack: Bitmap?,
    loadedSpineRaw: Bitmap?,
    loadedFull: Bitmap?,
    loadedMediaFront: Bitmap?,
    loadedMediaBack: Bitmap?,
    shot1: Bitmap?,
    shot2: Bitmap?,
): ResolvedPhysicalTextures {
    val slicedFull = loadedFull?.let { PhysicalMediaSpec.sliceFullBoxCover(it, spec.fullWrapSpineFraction) }
    val realFront = loadedFront ?: slicedFull?.third
    val realBack = loadedBack ?: slicedFull?.first
    val realSpine = (loadedSpineRaw ?: slicedFull?.second)?.let { PhysicalMediaSpec.orientSpineVertical(it) }

    val frontBmp = realFront ?: buildProceduralFrontBitmap(game, dossier, spec)
    val targetAspect = (frontBmp.width.toFloat() / frontBmp.height.toFloat().coerceAtLeast(1f)).coerceIn(0.56f, 1.48f)
    val backBmp = realBack ?: buildProceduralBackBitmap(game, dossier, spec, targetAspect, shot1 ?: frontBmp, shot2)
    val spineBmp = realSpine ?: buildProceduralSpineBitmap(game, dossier, spec, realFront, targetAspect)
    val mediaFrontBmp = loadedMediaFront ?: if (spec.mediaKind.isOpticalDisc) {
        buildProceduralDiscLabelBitmap(game, dossier, spec, frontBmp)
    } else {
        buildProceduralCartFrontBitmap(game, dossier, spec, frontBmp)
    }
    val mediaBackBmp = loadedMediaBack ?: if (spec.mediaKind.isOpticalDisc) {
        buildProceduralDiscUndersideBitmap(spec)
    } else {
        buildProceduralCartBackBitmap(game, spec)
    }
    val hasRealMedia = loadedMediaFront != null
    val maskedBack = if (hasRealMedia && !spec.mediaKind.isOpticalDisc) {
        maskBitmapToAlphaSilhouette(mediaBackBmp, mediaFrontBmp)
    } else {
        mediaBackBmp
    }
    return ResolvedPhysicalTextures(
        front = frontBmp,
        back = backBmp,
        spine = spineBmp,
        mediaFront = mediaFrontBmp,
        mediaBack = mediaBackBmp,
        hasRealFront = realFront != null,
        hasRealBack = realBack != null,
        hasRealSpine = realSpine != null,
        hasRealMedia = hasRealMedia,
        maskedMediaBack = maskedBack,
    )
}

@Composable
internal fun rememberPhysicalTextures(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    override: PhysicalViewerBitmaps?,
): ResolvedPhysicalTextures {
    // Deterministic synchronous path for unit/screenshot tests supplying override bitmaps.
    if (override != null) {
        return remember(game.id, dossier, spec, override) {
            val loadedMediaFront = if (spec.mediaKind.isOpticalDisc) {
                override.disc ?: override.cartFront
            } else {
                override.cartFront ?: override.disc
            }
            assembleResolvedTextures(
                game = game,
                dossier = dossier,
                spec = spec,
                loadedFront = override.boxFront,
                loadedBack = override.boxBack,
                loadedSpineRaw = override.boxSpine,
                loadedFull = override.boxFull,
                loadedMediaFront = loadedMediaFront,
                loadedMediaBack = override.cartBack,
                shot1 = override.screenshot1,
                shot2 = override.screenshot2,
            )
        }
    }

    val context = LocalContext.current
    val artVersion = LocalArtVersion.current
    val artSources = rememberArtSources(game)

    val baseKey = "${game.id}|${spec.systemId}|${spec.spineHeaderColor.toArgb()}|$artVersion"
    val fullKey = "$baseKey|${dossier.boxArtUrl}|${dossier.boxBackUrl}|${dossier.boxSpineUrl}|${dossier.boxFullUrl}|${dossier.cartFrontUrl}|${dossier.cartBackUrl}|${dossier.discUrl}|${dossier.title}|${dossier.publisher}|${dossier.esrbRating}"

    // Instant first frame (0 ms, zero UI-thread Canvas synthesis):
    // 1) Exact RAM cache hit -> 2) Last cached version for this game -> 3) Already-decoded Shelf cover & spine -> 4) 1 KB stub
    val initialTextures = remember(fullKey, baseKey) {
        PhysicalTexturesCache.get(fullKey)
            ?: PhysicalTexturesCache.get(baseKey)
            ?: ShelfCaseCache.peekGame("${game.id}|$artVersion")?.toSeedTextures(spec)
            ?: buildInstantStubTextures(spec)
    }

    val frontCandidates = remember(game.id, dossier.boxArtUrl, artSources) {
        buildList<Any> {
            artSources.filterIsInstance<java.io.File>().forEach { add(it) }
            dossier.boxArtUrl?.let { add(it) }
            artSources.forEach { src -> if (src !in this) add(src) }
        }
    }
    val backCandidates = remember(game.id, dossier.boxBackUrl) { listOfNotNull<Any>(dossier.boxBackUrl) }
    val spineCandidates = remember(game.id, dossier.boxSpineUrl) { listOfNotNull<Any>(dossier.boxSpineUrl) }
    val fullCandidates = remember(game.id, dossier.boxFullUrl) { listOfNotNull<Any>(dossier.boxFullUrl) }
    val mediaFrontCandidates = remember(game.id, dossier.cartFrontUrl, dossier.discUrl, spec.mediaKind.isOpticalDisc) {
        if (spec.mediaKind.isOpticalDisc) {
            listOfNotNull<Any>(dossier.discUrl, dossier.cartFrontUrl)
        } else {
            listOfNotNull<Any>(dossier.cartFrontUrl, dossier.discUrl)
        }
    }
    val mediaBackCandidates = remember(game.id, dossier.cartBackUrl) { listOfNotNull<Any>(dossier.cartBackUrl) }
    val shot1Candidates = remember(game.id, dossier.screenshotUrls, dossier.titleScreenUrl) {
        buildList<Any> {
            dossier.screenshotUrls.getOrNull(0)?.let { add(it) }
            dossier.titleScreenUrl?.let { add(it) }
            addAll(game.gameplaySnapUrls.take(1))
        }
    }
    val shot2Candidates = remember(game.id, dossier.screenshotUrls) {
        buildList<Any> {
            dossier.screenshotUrls.getOrNull(1)?.let { add(it) }
            addAll(game.titleScreenUrls.take(1))
        }
    }

    val state = androidx.compose.runtime.produceState(initialValue = initialTextures, fullKey) {
        PhysicalTexturesCache.get(fullKey)?.let { cached ->
            value = cached
            return@produceState
        }

        // Launch primary face & media loads in parallel at crisp mobile resolution (768px -> 2.5x faster than 1200px)
        coroutineScope {
            val frontDef = async { loadFirstPhysicalBitmap(context, frontCandidates, 768) }
            val spineDef = async { loadFirstPhysicalBitmap(context, spineCandidates, 768) }
            val backDef = async { loadFirstPhysicalBitmap(context, backCandidates, 768) }
            val mediaFrontDef = async { loadFirstPhysicalBitmap(context, mediaFrontCandidates, 768) }
            val mediaBackDef = async { loadFirstPhysicalBitmap(context, mediaBackCandidates, 768) }

            // Progressive fast-publish: the moment front art resolves, immediately show crisp 3D front + spine
            // without waiting on back-of-box, full-wrap, or cartridge scans!
            val loadedFront = frontDef.await()
            if (loadedFront != null && (!value.hasRealFront || loadedFront.width > value.front.width)) {
                val quickSpine = if (value.hasRealSpine) {
                    value.spine
                } else {
                    withContext(Dispatchers.Default) {
                        val asp = (loadedFront.width.toFloat() / loadedFront.height.toFloat().coerceAtLeast(1f)).coerceIn(0.56f, 1.48f)
                        buildProceduralSpineBitmap(game, dossier, spec, loadedFront, asp)
                    }
                }
                value = value.copy(
                    front = loadedFront,
                    spine = quickSpine,
                    hasRealFront = true,
                )
            }

            val loadedSpineRaw = spineDef.await()
            val loadedBack = backDef.await()
            val loadedMediaFront = mediaFrontDef.await()
            val loadedMediaBack = mediaBackDef.await()

            // Skip downloading massive Box - Full wraparound scan when front, back & spine are already present
            val needFull = (loadedFront == null || loadedBack == null || loadedSpineRaw == null) && fullCandidates.isNotEmpty()
            val loadedFull = if (needFull) loadFirstPhysicalBitmap(context, fullCandidates, 960) else null

            // Skip downloading back-of-box screenshots altogether when a real back scan (or full wrap) is present
            val hasRealBackAlready = loadedBack != null || loadedFull != null
            val (shot1, shot2) = if (!hasRealBackAlready) {
                val s1 = async { loadFirstPhysicalBitmap(context, shot1Candidates, 480) }
                val s2 = async { loadFirstPhysicalBitmap(context, shot2Candidates, 480) }
                s1.await() to s2.await()
            } else {
                null to null
            }

            val resolved = withContext(Dispatchers.Default) {
                assembleResolvedTextures(
                    game = game,
                    dossier = dossier,
                    spec = spec,
                    loadedFront = loadedFront,
                    loadedBack = loadedBack,
                    loadedSpineRaw = loadedSpineRaw,
                    loadedFull = loadedFull,
                    loadedMediaFront = loadedMediaFront,
                    loadedMediaBack = loadedMediaBack,
                    shot1 = shot1,
                    shot2 = shot2,
                )
            }
            PhysicalTexturesCache.put(fullKey, resolved)
            PhysicalTexturesCache.put(baseKey, resolved)
            value = resolved
        }
    }

    return state.value
}

internal fun buildProceduralFrontBitmap(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
): Bitmap {
    val landscape = spec.defaultBoxAspect > 1.12f
    val w = if (landscape) 740 else 540
    val h = (w / spec.defaultBoxAspect).roundToInt().coerceIn(480, 860)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val headerArgb = spec.spineHeaderColor.toArgb()
    paint.shader = LinearGradient(
        0f, 0f, w.toFloat(), h.toFloat(),
        intArrayOf(
            shadeArgb(headerArgb, 0.55f),
            AndroidColor.rgb(20, 22, 32),
            AndroidColor.rgb(12, 13, 20),
        ),
        floatArrayOf(0f, 0.58f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    paint.shader = null

    // Console top header band
    val bandH = h * 0.11f
    paint.color = headerArgb
    c.drawRect(0f, 0f, w.toFloat(), bandH, paint)

    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = bandH * 0.38f
        letterSpacing = 0.08f
    }
    val sysName = (game.system?.name ?: game.systemId).uppercase(Locale.US)
    c.drawText(
        TextUtils.ellipsize(sysName, tp, w * 0.88f, TextUtils.TruncateAt.END).toString(),
        w * 0.06f,
        bandH * 0.66f,
        tp,
    )

    // Subtle vintage printed retail box diagonal grid & framed key-art window
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.2f
    paint.color = AndroidColor.argb(18, 255, 255, 255)
    val gridStep = min(w, h) * 0.065f
    var gx = 0f
    while (gx < w) {
        c.drawLine(gx, bandH, gx, h.toFloat(), paint)
        gx += gridStep
    }
    var gy = bandH
    while (gy < h) {
        c.drawLine(0f, gy, w.toFloat(), gy, paint)
        gy += gridStep
    }
    paint.style = Paint.Style.FILL

    // Framed key-art emblem card
    val artFrame = RectF(w * 0.06f, bandH + h * 0.055f, w * 0.94f, h * 0.76f)
    paint.shader = LinearGradient(
        artFrame.left, artFrame.top, artFrame.right, artFrame.bottom,
        intArrayOf(
            shadeArgb(headerArgb, 0.72f),
            AndroidColor.rgb(24, 26, 40),
            AndroidColor.rgb(12, 13, 22),
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(artFrame, 12f, 12f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.4f
    paint.color = AndroidColor.argb(72, 255, 255, 255)
    c.drawRoundRect(artFrame, 12f, 12f, paint)
    paint.style = Paint.Style.FILL

    // Decorative retro emblem backdrop inside key-art frame
    paint.color = AndroidColor.argb(28, 255, 255, 255)
    c.drawCircle(w * 0.72f, h * 0.45f, min(w, h) * 0.24f, paint)
    paint.color = shadeArgb(headerArgb, 1.15f)
    paint.alpha = 65
    c.drawCircle(w * 0.68f, h * 0.47f, min(w, h) * 0.16f, paint)
    paint.alpha = 255

    // Bold drop-shadowed retail cover title
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = min(w, h) * 0.092f
        setShadowLayer(8f, 0f, 4f, AndroidColor.argb(180, 0, 0, 0))
    }
    val titleLayout = StaticLayout.Builder
        .obtain(game.title, 0, game.title.length, titlePaint, (w * 0.80f).toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(3)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    c.save()
    c.translate(w * 0.10f, h * 0.34f)
    titleLayout.draw(c)
    c.restore()

    // Bottom ESRB + Publisher stamp + metallic gold Official Seal of Quality on Nintendo boxes
    drawEsrbStamp(c, dossier.esrbRating.ifBlank { "E" }, w * 0.07f, h * 0.80f, min(w, h) * 0.13f)
    val isNintendoBox = game.systemId in setOf("nes", "snes", "n64", "gb", "gbc", "gba", "nds", "3ds", "gc", "wii", "switch")
    if (isNintendoBox) {
        val sealW = min(w, h) * 0.17f
        val sealH = sealW * 0.62f
        val sealRect = RectF(w * 0.93f - sealW, h * 0.80f, w * 0.93f, h * 0.80f + sealH)
        paint.shader = LinearGradient(
            sealRect.left, sealRect.top, sealRect.right, sealRect.bottom,
            intArrayOf(
                AndroidColor.rgb(252, 224, 126),
                AndroidColor.rgb(206, 162, 54),
                AndroidColor.rgb(154, 112, 28),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawOval(sealRect, paint)
        paint.shader = null
        paint.color = AndroidColor.rgb(252, 248, 236)
        c.drawOval(
            RectF(sealRect.left + 4f, sealRect.top + 4f, sealRect.right - 4f, sealRect.bottom - 4f),
            paint,
        )
        val sealTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(138, 98, 22)
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = sealH * 0.25f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("OFFICIAL", sealRect.centerX(), sealRect.centerY() - sealH * 0.06f, sealTp)
        c.drawText("SEAL", sealRect.centerX(), sealRect.centerY() + sealH * 0.24f, sealTp)
    }

    val pubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(225, 235, 238, 248)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = min(w, h) * 0.036f
    }
    val pub = dossier.publisher.ifBlank { dossier.developer }.uppercase(Locale.US)
    val pubW = pubPaint.measureText(pub)
    val pubX = if (isNintendoBox) {
        (w * 0.52f - pubW * 0.5f).coerceAtLeast(w * 0.26f)
    } else {
        (w * 0.93f - pubW).coerceAtLeast(w * 0.28f)
    }
    c.drawText(pub, pubX, h * 0.90f, pubPaint)

    // Physical case finishing details (cardboard flap seam vs. acrylic jewel-case inner bevel + studio sheen)
    when (spec.caseStyle) {
        RetailCaseStyle.CARDBOARD_VERTICAL,
        RetailCaseStyle.CARDBOARD_LANDSCAPE,
        RetailCaseStyle.CARDBOARD_SQUARE -> {
            // Subtle cardboard edge wear & top-flap crease
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = AndroidColor.argb(38, 255, 255, 255)
            c.drawRect(3f, 3f, w - 3f, h - 3f, paint)
            paint.strokeWidth = 1.6f
            paint.color = AndroidColor.argb(55, 0, 0, 0)
            c.drawLine(0f, bandH + 1.5f, w.toFloat(), bandH + 1.5f, paint)
            paint.color = AndroidColor.argb(26, 255, 255, 255)
            c.drawLine(0f, bandH + 3.2f, w.toFloat(), bandH + 3.2f, paint)
            paint.style = Paint.Style.FILL
        }
        RetailCaseStyle.JEWEL_CASE_CD -> {
            // Acrylic jewel-case inner tray hinge line & crisp clear edge bevel
            paint.color = AndroidColor.argb(42, 0, 0, 0)
            c.drawRect(0f, 0f, w * 0.068f, h.toFloat(), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = AndroidColor.argb(90, 255, 255, 255)
            c.drawRect(2f, 2f, w - 2f, h - 2f, paint)
            paint.strokeWidth = 1.8f
            paint.color = AndroidColor.argb(60, 255, 255, 255)
            c.drawLine(w * 0.068f, 0f, w * 0.068f, h.toFloat(), paint)
            paint.style = Paint.Style.FILL
        }
        else -> {
            // Molded plastic clamshell / DVD / Blu-ray outer lip highlight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = AndroidColor.argb(52, 255, 255, 255)
            c.drawRect(2.5f, 2.5f, w - 2.5f, h - 2.5f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // Diagonal studio softbox cover sheen
    val sheenAlpha = if (spec.caseStyle.glossyWrap) 44 else 22
    paint.shader = LinearGradient(
        0f, 0f, w * 0.85f, h * 0.72f,
        intArrayOf(
            AndroidColor.argb(sheenAlpha, 255, 255, 255),
            AndroidColor.argb(sheenAlpha / 4, 255, 255, 255),
            AndroidColor.TRANSPARENT,
        ),
        floatArrayOf(0f, 0.36f, 0.72f),
        Shader.TileMode.CLAMP,
    )
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    paint.shader = null
    return bmp
}

/**
 * Museum-grade Back-of-the-Box sleeve generator used when LaunchBox does not yet have a `Box - Back`
 * scan for a ROM—so flipping ANY game 180° lets you read the real LaunchBox back-of-box blurb,
 * inspect framed gameplay callouts, ESRB rating, player count, publisher credits, and UPC barcode!
 */
internal fun buildProceduralBackBitmap(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    aspect: Float,
    shot1: Bitmap?,
    shot2: Bitmap?,
): Bitmap {
    val landscape = aspect > 1.12f
    val w = if (landscape) 860 else 640
    val h = (w / aspect).roundToInt().coerceIn(560, 960)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val headerArgb = spec.spineHeaderColor.toArgb()
    // Rich dark editorial back-cover stock
    paint.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        intArrayOf(
            AndroidColor.rgb(22, 25, 36),
            AndroidColor.rgb(14, 16, 24),
            AndroidColor.rgb(10, 11, 17),
        ),
        floatArrayOf(0f, 0.65f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    paint.shader = null

    // Top console strip
    val topBarH = h * 0.085f
    paint.color = headerArgb
    c.drawRect(0f, 0f, w.toFloat(), topBarH, paint)

    val topTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = topBarH * 0.40f
        letterSpacing = 0.06f
    }
    val headerLabel = "${(game.system?.shortName ?: game.systemId).uppercase(Locale.US)} · ${game.title.uppercase(Locale.US)}"
    c.drawText(
        TextUtils.ellipsize(headerLabel, topTextPaint, w * 0.90f, TextUtils.TruncateAt.END).toString(),
        w * 0.05f,
        topBarH * 0.66f,
        topTextPaint,
    )

    val padX = w * 0.055f
    var cursorY = topBarH + h * 0.032f

    // Catchy back-of-box hook headline
    val hookPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(255, 214, 102)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        textSize = min(w, h) * 0.036f
    }
    val hookText = dossier.promoQuote.ifBlank { dossier.genre.uppercase(Locale.US) }
    c.drawText(
        TextUtils.ellipsize(hookText, hookPaint, w - padX * 2f, TextUtils.TruncateAt.END).toString(),
        padX,
        cursorY + hookPaint.textSize,
        hookPaint,
    )
    cursorY += hookPaint.textSize + h * 0.022f

    // Framed Back-of-the-Box Gameplay Screenshot Callout Boxes
    if (shot1 != null) {
        val shotRowH = h * if (landscape) 0.30f else 0.23f
        val gap = w * 0.03f
        if (shot2 != null) {
            val boxW = (w - padX * 2f - gap) * 0.5f
            drawFramedBackScreenshot(c, shot1, RectF(padX, cursorY, padX + boxW, cursorY + shotRowH), headerArgb, paint, bmpPaint)
            drawFramedBackScreenshot(c, shot2, RectF(padX + boxW + gap, cursorY, w - padX, cursorY + shotRowH), headerArgb, paint, bmpPaint)
        } else {
            val boxW = (w - padX * 2f) * 0.68f
            drawFramedBackScreenshot(c, shot1, RectF(padX, cursorY, padX + boxW, cursorY + shotRowH), headerArgb, paint, bmpPaint)
        }
        cursorY += shotRowH + h * 0.028f
    }

    // Main Back-of-the-Box Overview Blurb (real LaunchBox overview!)
    val footerTop = h * 0.79f
    val availTextH = (footerTop - cursorY - h * 0.02f).coerceAtLeast(80f)
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(234, 237, 245)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textSize = min(w, h) * 0.029f
    }
    val maxLines = (availTextH / (bodyPaint.textSize * 1.32f)).toInt().coerceIn(4, 14)
    val blurb = dossier.summary.ifBlank { dossier.tagline }
    val bodyLayout = StaticLayout.Builder
        .obtain(blurb, 0, blurb.length, bodyPaint, (w - padX * 2f).toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.24f)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    c.save()
    c.translate(padX, cursorY)
    bodyLayout.draw(c)
    c.restore()

    // Bottom retail spec strip: ESRB stamp + Players/Genre/Publisher + Printed UPC Barcode
    paint.color = AndroidColor.rgb(24, 27, 38)
    c.drawRect(0f, footerTop, w.toFloat(), h.toFloat(), paint)
    paint.color = AndroidColor.argb(90, 255, 255, 255)
    c.drawRect(0f, footerTop, w.toFloat(), footerTop + 2f, paint)

    val stampH = h * 0.14f
    val stampW = drawEsrbStamp(c, dossier.esrbRating.ifBlank { "E" }, padX, footerTop + h * 0.035f, stampH)

    // Retail UPC Barcode on the bottom-right
    val barcodeW = w * 0.22f
    val barcodeH = h * 0.12f
    val barcodeX = w - padX - barcodeW
    val barcodeY = footerTop + h * 0.045f
    paint.color = AndroidColor.WHITE
    c.drawRoundRect(RectF(barcodeX, barcodeY, barcodeX + barcodeW, barcodeY + barcodeH), 6f, 6f, paint)
    paint.color = AndroidColor.BLACK
    val seed = game.title.hashCode()
    var bx = barcodeX + barcodeW * 0.08f
    val bxEnd = barcodeX + barcodeW * 0.92f
    var barIdx = 0
    while (bx < bxEnd) {
        val barW = if (((seed ushr (barIdx % 24)) and 1) == 1) 3.4f else 1.7f
        if (bx + barW < bxEnd) {
            c.drawRect(bx, barcodeY + barcodeH * 0.12f, bx + barW, barcodeY + barcodeH * 0.82f, paint)
        }
        bx += barW + if (barIdx % 3 == 0) 3.2f else 2.0f
        barIdx++
    }

    // Spec lines between ESRB and Barcode
    val specX = padX + stampW + w * 0.035f
    val specMaxW = (barcodeX - specX - w * 0.025f).coerceAtLeast(80f)
    val specPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(210, 216, 230)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = min(w, h) * 0.024f
    }
    val specs = listOfNotNull(
        dossier.modes.takeIf { it.isNotBlank() },
        dossier.genre.takeIf { it.isNotBlank() },
        listOf(dossier.developer, dossier.releaseYear).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() },
        "© ${dossier.publisher.ifBlank { dossier.developer }}".takeIf { it.length > 2 },
    )
    specs.take(4).forEachIndexed { idx, line ->
        c.drawText(
            TextUtils.ellipsize(line, specPaint, specMaxW, TextUtils.TruncateAt.END).toString(),
            specX,
            footerTop + h * 0.062f + idx * (specPaint.textSize * 1.38f),
            specPaint,
        )
    }

    return bmp
}

private fun drawFramedBackScreenshot(
    c: AndroidCanvas,
    bmp: Bitmap,
    rect: RectF,
    borderArgb: Int,
    paint: Paint,
    bmpPaint: Paint,
) {
    c.save()
    val clip = AndroidPath().apply { addRoundRect(rect, 10f, 10f, AndroidPath.Direction.CW) }
    c.clipPath(clip)
    // Center-crop screenshot inside callout frame
    val scale = max(rect.width() / bmp.width.toFloat(), rect.height() / bmp.height.toFloat())
    val sw = bmp.width * scale
    val sh = bmp.height * scale
    val dx = rect.centerX() - sw * 0.5f
    val dy = rect.centerY() - sh * 0.5f
    c.drawBitmap(bmp, null, RectF(dx, dy, dx + sw, dy + sh), bmpPaint)
    c.restore()

    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = shadeArgb(borderArgb, 1.15f)
    }
    c.drawRoundRect(rect, 10f, 10f, stroke)
    paint.color = AndroidColor.TRANSPARENT
}

private fun drawEsrbStamp(c: AndroidCanvas, rawRating: String, x: Float, y: Float, h: Float): Float {
    val w = h * 0.72f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = AndroidColor.WHITE
    c.drawRect(x, y, x + w, y + h, p)
    p.color = AndroidColor.BLACK
    p.style = Paint.Style.STROKE
    p.strokeWidth = 3f
    c.drawRect(x, y, x + w, y + h, p)
    p.style = Paint.Style.FILL
    c.drawRect(x, y, x + w, y + h * 0.22f, p)
    c.drawRect(x, y + h * 0.80f, x + w, y + h, p)

    val letter = when {
        rawRating.startsWith("M", ignoreCase = true) -> "M"
        rawRating.startsWith("T", ignoreCase = true) -> "T"
        rawRating.startsWith("E10", ignoreCase = true) || "10+" in rawRating -> "E10+"
        rawRating.startsWith("K-A", ignoreCase = true) -> "K-A"
        else -> "E"
    }
    val topTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = h * 0.12f
        textAlign = Paint.Align.CENTER
    }
    c.drawText("CONTENT RATED", x + w * 0.5f, y + h * 0.16f, topTp)
    c.drawText("ESRB", x + w * 0.5f, y + h * 0.94f, topTp)

    val bigTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = if (letter.length > 2) h * 0.28f else h * 0.44f
        textAlign = Paint.Align.CENTER
    }
    c.drawText(letter, x + w * 0.5f, y + h * 0.65f, bigTp)
    return w
}

internal fun buildProceduralSpineBitmap(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    cover: Bitmap? = null,
    /** Front aspect of the case this spine belongs to (sets the spine face's shape in 3D). */
    boxAspect: Float = ShelfLayoutMath.proceduralFrontAspect(spec),
): Bitmap {
    // Exactly the spine face's shape in the 3D model, so the renderer never squashes or stretches it.
    val w = 160
    val h = (w / ShelfLayoutMath.spineFaceRatio(spec, boxAspect)).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun luminance(argb: Int): Float =
        (0.2126f * AndroidColor.red(argb) + 0.7152f * AndroidColor.green(argb) + 0.0722f * AndroidColor.blue(argb)) / 255f
    fun inkOn(argb: Int) = if (luminance(argb) > 0.6f) AndroidColor.rgb(20, 22, 28) else AndroidColor.WHITE

    // Body: the cover's own colors when there's real art, else the shelf's per-game color.
    // White/clear cases (3DS, Wii, PSP) get a white spine; the NA DS case is black (Wikipedia, Keep case).
    val whiteCase = luminance(spec.casePlasticColor.toArgb()) > 0.75f
    val coverArgb = cover?.let { art ->
        runCatching {
            val small = Bitmap.createScaledBitmap(art, 96, (96f * art.height / art.width).toInt().coerceAtLeast(16), true)
            val palette = androidx.palette.graphics.Palette.from(small).generate()
            (palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch)?.rgb
        }.getOrNull()
    }
    val bodyArgb = when {
        whiteCase -> AndroidColor.rgb(236, 239, 244)
        coverArgb != null -> {
            // Keep it dark enough for white text.
            val hsv = FloatArray(3).also { AndroidColor.colorToHSV(coverArgb, it) }
            hsv[2] = hsv[2].coerceAtMost(0.42f)
            AndroidColor.HSVToColor(hsv)
        }
        else -> ShelfLayoutMath.spineBaseColor(game, spec).toArgb()
    }
    val ink = inkOn(bodyArgb)
    p.color = bodyArgb
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

    // White DS-style cases get a stripe of the cover's color down the edge.
    if (whiteCase && coverArgb != null) {
        p.color = coverArgb
        c.drawRect(0f, 0f, w * 0.07f, h.toFloat(), p)
        c.drawRect(w * 0.93f, 0f, w.toFloat(), h.toFloat(), p)
    }

    // Console band at the top, with a label that always contrasts with it.
    val headerArgb = spec.spineHeaderColor.toArgb()
    val capH = minOf(h * 0.12f, w * 0.8f)
    p.color = headerArgb
    c.drawRect(0f, 0f, w.toFloat(), capH, p)
    val sysLabel = (game.system?.shortName ?: game.systemId).uppercase(Locale.US)
    val capTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkOn(headerArgb)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
        textSize = 34f
    }
    while (capTp.measureText(sysLabel) > w * 0.84f && capTp.textSize > 16f) capTp.textSize -= 1f
    c.drawText(sysLabel, w * 0.5f, capH * 0.5f - (capTp.descent() + capTp.ascent()) / 2f, capTp)

    // A window of the cover art near the bottom ties the spine to the box (when there's art).
    val footH = h * 0.07f
    val artSize = if (cover != null) w * 0.78f else 0f
    val artBottom = h - footH - w * 0.08f
    if (cover != null) {
        val artTop = artBottom - artSize
        val left = (w - artSize) / 2f
        val side = minOf(cover.width, cover.height)
        val src = android.graphics.Rect((cover.width - side) / 2, (cover.height - side) / 2, (cover.width + side) / 2, (cover.height + side) / 2)
        c.save()
        val clip = android.graphics.Path().apply {
            addRoundRect(left, artTop, left + artSize, artTop + artSize, 10f, 10f, android.graphics.Path.Direction.CW)
        }
        c.clipPath(clip)
        c.drawBitmap(cover, src, android.graphics.RectF(left, artTop, left + artSize, artTop + artSize), Paint(Paint.FILTER_BITMAP_FLAG))
        c.restore()
    }

    // Title runs down the spine, sized to fill it and only shortened if it really can't fit.
    val titleTop = capH + w * 0.12f
    val titleBottom = (if (cover != null) artBottom - artSize else h - footH) - w * 0.12f
    val titleLen = (titleBottom - titleTop).coerceAtLeast(40f)
    val titleTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = w * 0.40f
    }
    while (titleTp.measureText(game.title) > titleLen && titleTp.textSize > w * 0.22f) titleTp.textSize -= 1f
    val title = TextUtils.ellipsize(game.title, titleTp, titleLen, TextUtils.TruncateAt.END).toString()
    c.save()
    // Rotated so it reads top to bottom, centered across the spine's width.
    c.translate(w * 0.5f - (titleTp.descent() + titleTp.ascent()) / 2f, titleTop + (titleLen - titleTp.measureText(title)) / 2f)
    c.rotate(90f)
    c.drawText(title, 0f, 0f, titleTp)
    c.restore()

    // Publisher at the foot: its first word, sized to fit (no cut-off names, nothing invented).
    val publisher = dossier.publisher.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        .trim(',', '.').uppercase(Locale.US)
    if (publisher.isNotEmpty()) {
        val pubTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(200, AndroidColor.red(ink), AndroidColor.green(ink), AndroidColor.blue(ink))
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 22f
            letterSpacing = 0.04f
        }
        while (pubTp.measureText(publisher) > w * 0.86f && pubTp.textSize > 11f) pubTp.textSize -= 1f
        c.drawText(publisher, w * 0.5f, h - footH * 0.45f, pubTp)
    }
    return bmp
}

/**
 * Builds the authentic physical silhouette outline for each console's cartridge / Game Card
 * (3DS protruding top-right lockout tab, DS chamfered corner, Switch slim vertical card,
 * Game Boy top-right power notch, GBC raised bubble crown, GBA flared top grip wings,
 * NES stepped narrower lower pin neck, N64/Genesis curved dome arch, SNES sculpted shoulders).
 */
private fun buildCartridgeSilhouettePath(
    spec: PhysicalMediaSpec,
    w: Float,
    h: Float,
    mirrorX: Boolean = false,
): AndroidPath {
    val path = AndroidPath()
    val id = spec.systemId.lowercase(Locale.US)
    when {
        id == "3ds" -> {
            // Iconic Nintendo 3DS Game Card: protruding top-right lockout tab + chamfered bottom-left corner!
            val l = w * 0.055f
            val r = w * 0.885f
            val tabR = w * 0.965f
            val t = h * 0.035f
            val tabBot = h * 0.195f
            val b = h * 0.965f
            val cham = w * 0.095f
            val rad = w * 0.042f
            path.moveTo(l + rad, t)
            path.lineTo(tabR - rad * 0.7f, t)
            path.quadTo(tabR, t, tabR, t + rad * 0.7f)
            path.lineTo(tabR, tabBot - rad * 0.6f)
            path.quadTo(tabR, tabBot, tabR - rad * 0.6f, tabBot)
            path.lineTo(r + rad * 0.35f, tabBot)
            path.quadTo(r, tabBot, r, tabBot + rad * 0.45f)
            path.lineTo(r, b - rad)
            path.quadTo(r, b, r - rad, b)
            path.lineTo(l + cham, b)
            path.lineTo(l, b - cham)
            path.lineTo(l, t + rad)
            path.quadTo(l, t, l + rad, t)
            path.close()
        }
        id == "nds" -> {
            // Iconic Nintendo DS Game Card: square charcoal shell with chamfered bottom-left corner
            val l = w * 0.06f
            val r = w * 0.94f
            val t = h * 0.04f
            val b = h * 0.96f
            val cham = w * 0.095f
            val rad = w * 0.045f
            path.moveTo(l + rad, t)
            path.lineTo(r - rad, t)
            path.quadTo(r, t, r, t + rad)
            path.lineTo(r, b - rad)
            path.quadTo(r, b, r - rad, b)
            path.lineTo(l + cham, b)
            path.lineTo(l, b - cham)
            path.lineTo(l, t + rad)
            path.quadTo(l, t, l + rad, t)
            path.close()
        }
        id == "switch" -> {
            // Iconic Nintendo Switch Game Card: sleek narrow vertical matte-black card + chamfered bottom-right
            val l = w * 0.145f
            val r = w * 0.855f
            val t = h * 0.03f
            val b = h * 0.97f
            val cham = w * 0.075f
            val rad = w * 0.042f
            path.moveTo(l + rad, t)
            path.lineTo(r - rad, t)
            path.quadTo(r, t, r, t + rad)
            path.lineTo(r, b - cham)
            path.lineTo(r - cham, b)
            path.lineTo(l + rad, b)
            path.quadTo(l, b, l, b - rad)
            path.lineTo(l, t + rad)
            path.quadTo(l, t, l + rad, t)
            path.close()
        }
        id == "gb" -> {
            // Classic 1989 Game Boy DMG Game Pak: iconic top-right power-switch lockout notch!
            val l = w * 0.045f
            val r = w * 0.955f
            val t = h * 0.035f
            val b = h * 0.965f
            val notchX = w * 0.825f
            val notchY = h * 0.155f
            val rad = w * 0.040f
            path.moveTo(l + rad, t)
            path.lineTo(notchX - rad * 0.4f, t)
            path.quadTo(notchX, t, notchX, t + rad * 0.4f)
            path.lineTo(notchX, notchY - rad * 0.35f)
            path.quadTo(notchX, notchY, notchX + rad * 0.4f, notchY)
            path.lineTo(r - rad * 0.4f, notchY)
            path.quadTo(r, notchY, r, notchY + rad * 0.5f)
            path.lineTo(r, b - rad)
            path.quadTo(r, b, r - rad, b)
            path.lineTo(l + rad, b)
            path.quadTo(l, b, l, b - rad)
            path.lineTo(l, t + rad)
            path.quadTo(l, t, l + rad, t)
            path.close()
        }
        id == "gbc" -> {
            // Classic Game Boy Color cartridge: smooth convex arched top bubble crown (no corner notch!)
            val l = w * 0.045f
            val r = w * 0.955f
            val shoulderY = h * 0.085f
            val b = h * 0.965f
            val rad = w * 0.040f
            path.moveTo(l, shoulderY)
            path.quadTo(w * 0.50f, -h * 0.015f, r, shoulderY)
            path.lineTo(r, b - rad)
            path.quadTo(r, b, r - rad, b)
            path.lineTo(l + rad, b)
            path.quadTo(l, b, l, b - rad)
            path.close()
        }
        spec.mediaKind == PhysicalMediaKind.CART_GBA -> {
            // Iconic Game Boy Advance Game Pak: wide flared top grip wings stepping into narrower slot body!
            val wingL = w * 0.025f
            val wingR = w * 0.975f
            val bodyL = w * 0.068f
            val bodyR = w * 0.932f
            val t = h * 0.035f
            val wingBot = h * 0.195f
            val b = h * 0.965f
            val rad = w * 0.042f
            path.moveTo(wingL + rad, t)
            path.lineTo(wingR - rad, t)
            path.quadTo(wingR, t, wingR, t + rad)
            path.lineTo(wingR, wingBot - rad * 0.4f)
            path.quadTo(wingR, wingBot, wingR - rad * 0.45f, wingBot)
            path.lineTo(bodyR + rad * 0.25f, wingBot)
            path.quadTo(bodyR, wingBot, bodyR, wingBot + rad * 0.4f)
            path.lineTo(bodyR, b - rad)
            path.quadTo(bodyR, b, bodyR - rad, b)
            path.lineTo(bodyL + rad, b)
            path.quadTo(bodyL, b, bodyL, b - rad)
            path.lineTo(bodyL, wingBot + rad * 0.4f)
            path.quadTo(bodyL, wingBot, bodyL - rad * 0.25f, wingBot)
            path.lineTo(wingL + rad * 0.45f, wingBot)
            path.quadTo(wingL, wingBot, wingL, wingBot - rad * 0.4f)
            path.lineTo(wingL, t + rad)
            path.quadTo(wingL, t, wingL + rad, t)
            path.close()
        }
        spec.mediaKind == PhysicalMediaKind.CART_NES -> {
            // Iconic 1985 NES Game Pak: wide upper body stepping inward into narrower lower 72-pin connector neck!
            val l = w * 0.035f
            val r = w * 0.965f
            val neckL = w * 0.105f
            val neckR = w * 0.895f
            val t = h * 0.025f
            val stepY = h * 0.805f
            val b = h * 0.975f
            val rad = w * 0.026f
            path.moveTo(l + rad, t)
            path.lineTo(r - rad, t)
            path.quadTo(r, t, r, t + rad)
            path.lineTo(r, stepY - rad)
            path.quadTo(r, stepY, r - rad, stepY)
            path.lineTo(neckR + rad * 0.4f, stepY)
            path.lineTo(neckR, stepY + rad * 0.6f)
            path.lineTo(neckR, b - rad)
            path.quadTo(neckR, b, neckR - rad, b)
            path.lineTo(neckL + rad, b)
            path.quadTo(neckL, b, neckL, b - rad)
            path.lineTo(neckL, stepY + rad * 0.6f)
            path.lineTo(neckL - rad * 0.4f, stepY)
            path.lineTo(l + rad, stepY)
            path.quadTo(l, stepY, l, stepY - rad)
            path.lineTo(l, t + rad)
            path.quadTo(l, t, l + rad, t)
            path.close()
        }
        spec.mediaKind == PhysicalMediaKind.CART_N64 -> {
            // Iconic Nintendo 64 cartridge: smooth arched dome crown + sculpted vertical sides
            val l = w * 0.035f
            val r = w * 0.965f
            val archY = h * 0.135f
            val b = h * 0.965f
            val rad = w * 0.036f
            path.moveTo(l, archY)
            path.quadTo(w * 0.50f, -h * 0.03f, r, archY)
            path.lineTo(r, b - rad)
            path.quadTo(r, b, r - rad, b)
            path.lineTo(l + rad, b)
            path.quadTo(l, b, l, b - rad)
            path.close()
        }
        spec.mediaKind == PhysicalMediaKind.CART_SEGA -> {
            // Iconic Sega Genesis / Mega Drive cartridge: gentle arched dome top + sleek rounded body
            val l = w * 0.04f
            val r = w * 0.96f
            val archY = h * 0.115f
            val b = h * 0.965f
            val rad = w * 0.036f
            path.moveTo(l, archY)
            path.quadTo(w * 0.50f, -h * 0.02f, r, archY)
            path.lineTo(r, b - rad)
            path.quadTo(r, b, r - rad, b)
            path.lineTo(l + rad, b)
            path.quadTo(l, b, l, b - rad)
            path.close()
        }
        else -> {
            // SNES / TurboGrafx / generic sculpted cartridge
            path.addRoundRect(RectF(w * 0.025f, h * 0.025f, w * 0.975f, h * 0.975f), 26f, 26f, AndroidPath.Direction.CW)
        }
    }
    if (mirrorX) {
        val m = Matrix().apply { setScale(-1f, 1f, w * 0.5f, h * 0.5f) }
        path.transform(m)
    }
    return path
}

private fun drawEmbossedCartridgeText(
    c: AndroidCanvas,
    text: String,
    cx: Float,
    cy: Float,
    textSizePx: Float,
    shellArgb: Int,
    italic: Boolean = false,
) {
    val style = if (italic) Typeface.BOLD_ITALIC else Typeface.BOLD
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
        textSize = textSizePx
        letterSpacing = 0.08f
        textAlign = Paint.Align.CENTER
    }
    val fm = tp.fontMetrics
    val baseY = cy - (fm.ascent + fm.descent) * 0.5f
    // Bottom highlight + dark debossed shadow for authentic injection-molded plastic relief!
    tp.color = shadeArgb(shellArgb, 1.24f)
    c.drawText(text, cx, baseY + 1.8f, tp)
    tp.color = shadeArgb(shellArgb, 0.56f)
    c.drawText(text, cx, baseY, tp)
}

internal fun buildProceduralCartFrontBitmap(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    coverBmp: Bitmap,
): Bitmap {
    val w = 620
    val h = (w / spec.mediaKind.defaultAspect).roundToInt().coerceIn(380, 740)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val id = spec.systemId.lowercase(Locale.US)
    val shellArgb = spec.mediaShellColor.toArgb()
    val silhouette = buildCartridgeSilhouettePath(spec, w.toFloat(), h.toFloat(), mirrorX = false)

    c.save()
    c.clipPath(silhouette)

    // Injection-molded ABS plastic shell body gradient with subtle studio top-left key light
    p.shader = LinearGradient(
        0f, 0f, w * 0.30f, h.toFloat(),
        intArrayOf(
            shadeArgb(shellArgb, 1.15f),
            shellArgb,
            shadeArgb(shellArgb, 0.78f),
        ),
        floatArrayOf(0f, 0.48f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawPath(silhouette, p)
    p.shader = null

    // Console-specific sculpted hardware molding & embossed typography
    when {
        spec.mediaKind == PhysicalMediaKind.CART_SNES -> {
            // Iconic SNES dual side grip columns + center recessed bay + top lock trough + embossed SUPER NINTENDO
            p.color = shadeArgb(shellArgb, 0.91f)
            c.drawRoundRect(RectF(w * 0.145f, h * 0.025f, w * 0.855f, h * 0.96f), 14f, 14f, p)
            for (side in 0..1) {
                val x0 = if (side == 0) w * 0.042f else w * 0.875f
                val x1 = if (side == 0) w * 0.125f else w * 0.958f
                for (i in 0..7) {
                    val gy = h * 0.14f + i * (h * 0.088f)
                    p.color = shadeArgb(shellArgb, 0.66f)
                    c.drawRoundRect(RectF(x0, gy, x1, gy + h * 0.030f), 4f, 4f, p)
                    p.color = shadeArgb(shellArgb, 1.18f)
                    c.drawRoundRect(RectF(x0, gy + h * 0.030f, x1, gy + h * 0.044f), 3f, 3f, p)
                }
            }
            p.color = shadeArgb(shellArgb, 0.74f)
            c.drawRoundRect(RectF(w * 0.21f, h * 0.042f, w * 0.79f, h * 0.118f), 14f, 14f, p)
            p.color = shadeArgb(shellArgb, 1.04f)
            c.drawRoundRect(RectF(w * 0.22f, h * 0.050f, w * 0.78f, h * 0.110f), 11f, 11f, p)
            drawEmbossedCartridgeText(c, "SUPER NINTENDO", w * 0.50f, h * 0.080f, 19f, shellArgb, italic = true)
            // Classic SNES front center lock-tab cutout notch near bottom
            p.color = shadeArgb(shellArgb, 0.58f)
            c.drawRoundRect(RectF(w * 0.34f, h * 0.848f, w * 0.66f, h * 0.888f), 6f, 6f, p)
        }
        spec.mediaKind == PhysicalMediaKind.CART_GAMEBOY -> {
            // Horizontal top grip grooves + oval GAME BOY / GAME BOY COLOR recessed header pill
            for (i in 0..3) {
                val gy = h * 0.058f + i * (h * 0.023f)
                val xEnd = if (id == "gb" && gy < h * 0.155f) w * 0.79f else w * 0.92f
                p.color = shadeArgb(shellArgb, 0.68f)
                c.drawRoundRect(RectF(w * 0.08f, gy, xEnd, gy + h * 0.011f), 4f, 4f, p)
                p.color = shadeArgb(shellArgb, 1.16f)
                c.drawRoundRect(RectF(w * 0.08f, gy + h * 0.011f, xEnd, gy + h * 0.016f), 2f, 2f, p)
            }
            p.color = shadeArgb(shellArgb, 0.72f)
            c.drawRoundRect(RectF(w * 0.16f, h * 0.048f, w * 0.81f, h * 0.168f), 46f, 46f, p)
            p.color = shadeArgb(shellArgb, if (id == "gbc") 1.10f else 0.96f)
            c.drawRoundRect(RectF(w * 0.178f, h * 0.058f, w * 0.792f, h * 0.158f), 40f, 40f, p)
            drawEmbossedCartridgeText(
                c,
                if (id == "gbc") "GAME BOY COLOR" else "Nintendo GAME BOY",
                w * 0.485f,
                h * 0.108f,
                21f,
                shellArgb,
                italic = true,
            )
        }
        spec.mediaKind == PhysicalMediaKind.CART_GBA -> {
            // Flared top shoulder lip + curved embossed GAME BOY ADVANCE trough
            p.color = shadeArgb(shellArgb, 1.14f)
            c.drawRoundRect(RectF(w * 0.04f, h * 0.045f, w * 0.96f, h * 0.175f), 24f, 24f, p)
            p.color = shadeArgb(shellArgb, 0.74f)
            c.drawRoundRect(RectF(w * 0.14f, h * 0.068f, w * 0.86f, h * 0.162f), 34f, 34f, p)
            p.color = shadeArgb(shellArgb, 0.94f)
            c.drawRoundRect(RectF(w * 0.155f, h * 0.078f, w * 0.845f, h * 0.152f), 28f, 28f, p)
            drawEmbossedCartridgeText(c, "GAME BOY ADVANCE", w * 0.50f, h * 0.115f, 19f, shellArgb, italic = true)
        }
        spec.mediaKind == PhysicalMediaKind.CART_NES -> {
            // Iconic NES left vertical 17-rib grip column + molded top-left Nintendo oval + stepped neck seam
            p.color = shadeArgb(shellArgb, 0.72f)
            c.drawRoundRect(RectF(w * 0.062f, h * 0.042f, w * 0.220f, h * 0.088f), 14f, 14f, p)
            drawEmbossedCartridgeText(c, "Nintendo", w * 0.141f, h * 0.065f, 15f, shellArgb)
            for (i in 0..14) {
                val gy = h * 0.108f + i * (h * 0.045f)
                p.color = shadeArgb(shellArgb, 0.66f)
                c.drawRect(w * 0.065f, gy, w * 0.218f, gy + h * 0.020f, p)
                p.color = shadeArgb(shellArgb, 1.18f)
                c.drawRect(w * 0.065f, gy + h * 0.020f, w * 0.218f, gy + h * 0.028f, p)
            }
            // Subtle horizontal molded step crease where the lower 72-pin neck narrows
            p.color = shadeArgb(shellArgb, 0.64f)
            c.drawRect(w * 0.105f, h * 0.802f, w * 0.895f, h * 0.809f, p)
            // Twin lower molded guide rails on the narrow neck
            p.color = shadeArgb(shellArgb, 0.78f)
            c.drawRect(w * 0.14f, h * 0.818f, w * 0.175f, h * 0.96f, p)
            c.drawRect(w * 0.825f, h * 0.818f, w * 0.86f, h * 0.96f, p)
        }
        spec.mediaKind == PhysicalMediaKind.CART_N64 -> {
            // Sculpted N64 side pillars + arched embossed NINTENDO 64 trough
            p.color = shadeArgb(shellArgb, 0.84f)
            c.drawRoundRect(RectF(w * 0.045f, h * 0.12f, w * 0.105f, h * 0.92f), 8f, 8f, p)
            c.drawRoundRect(RectF(w * 0.895f, h * 0.12f, w * 0.955f, h * 0.92f), 8f, 8f, p)
            p.color = shadeArgb(shellArgb, 0.74f)
            c.drawRoundRect(RectF(w * 0.22f, h * 0.068f, w * 0.78f, h * 0.132f), 18f, 18f, p)
            drawEmbossedCartridgeText(c, "NINTENDO 64", w * 0.50f, h * 0.100f, 18f, shellArgb)
        }
        spec.mediaKind == PhysicalMediaKind.CART_SEGA -> {
            // Jet-black Sega Genesis sculpted upper grip ridge + side notches
            p.color = shadeArgb(shellArgb, 1.24f)
            c.drawRoundRect(RectF(w * 0.18f, h * 0.055f, w * 0.82f, h * 0.118f), 16f, 16f, p)
            drawEmbossedCartridgeText(c, "SEGA", w * 0.50f, h * 0.086f, 18f, shellArgb)
            for (side in 0..1) {
                val x0 = if (side == 0) w * 0.052f else w * 0.912f
                val x1 = if (side == 0) w * 0.088f else w * 0.948f
                for (i in 0..4) {
                    val gy = h * 0.22f + i * (h * 0.055f)
                    p.color = shadeArgb(shellArgb, 0.62f)
                    c.drawRoundRect(RectF(x0, gy, x1, gy + h * 0.022f), 3f, 3f, p)
                }
            }
        }
        id == "3ds" || id == "nds" || id == "switch" -> {
            // Tactile molded top pull lip groove on DS / 3DS / Switch Game Cards
            val grooveL = if (id == "switch") w * 0.20f else w * 0.12f
            val grooveR = when (id) {
                "switch" -> w * 0.80f
                "3ds" -> w * 0.82f
                else -> w * 0.88f
            }
            p.color = shadeArgb(shellArgb, 0.74f)
            c.drawRoundRect(RectF(grooveL, h * 0.068f, grooveR, h * 0.104f), 8f, 8f, p)
            p.color = shadeArgb(shellArgb, 1.18f)
            c.drawRect(grooveL + 8f, h * 0.104f, grooveR - 8f, h * 0.110f, p)
        }
        else -> {
            p.color = shadeArgb(shellArgb, 0.74f)
            c.drawRoundRect(RectF(w * 0.18f, h * 0.05f, w * 0.82f, h * 0.11f), 12f, 12f, p)
        }
    }

    // Recessed sticker well sized & aligned for each exact console silhouette
    val labelRect = when {
        id == "3ds" -> RectF(w * 0.12f, h * 0.145f, w * 0.82f, h * 0.845f)
        id == "nds" -> RectF(w * 0.13f, h * 0.145f, w * 0.87f, h * 0.855f)
        id == "switch" -> RectF(w * 0.20f, h * 0.145f, w * 0.80f, h * 0.865f)
        spec.mediaKind == PhysicalMediaKind.CART_NES -> RectF(w * 0.265f, h * 0.058f, w * 0.895f, h * 0.785f)
        spec.mediaKind == PhysicalMediaKind.CART_GAMEBOY -> RectF(w * 0.13f, h * 0.205f, w * 0.87f, h * 0.855f)
        spec.mediaKind == PhysicalMediaKind.CART_GBA -> RectF(w * 0.135f, h * 0.215f, w * 0.865f, h * 0.855f)
        spec.mediaKind == PhysicalMediaKind.CART_SNES -> RectF(w * 0.175f, h * 0.150f, w * 0.825f, h * 0.815f)
        spec.mediaKind == PhysicalMediaKind.CART_N64 -> RectF(w * 0.155f, h * 0.175f, w * 0.845f, h * 0.835f)
        spec.mediaKind == PhysicalMediaKind.CART_SEGA -> RectF(w * 0.145f, h * 0.165f, w * 0.855f, h * 0.845f)
        else -> RectF(w * 0.14f, h * 0.14f, w * 0.86f, h * 0.82f)
    }

    // Deep recessed sticker well bevel shadow + crisp bottom rim catch-light
    p.color = shadeArgb(shellArgb, 0.54f)
    c.drawRoundRect(
        RectF(labelRect.left - 7f, labelRect.top - 7f, labelRect.right + 7f, labelRect.bottom + 7f),
        18f, 18f, p,
    )
    p.color = shadeArgb(shellArgb, 1.20f)
    c.drawRoundRect(
        RectF(labelRect.left - 5f, labelRect.top - 3f, labelRect.right + 5f, labelRect.bottom + 5f),
        16f, 16f, p,
    )
    drawFramedBackScreenshot(c, coverBmp, labelRect, spec.spineHeaderColor.toArgb(), p, bmpPaint)

    // Authentic console sticker branding band along bottom/top edge of DS, 3DS & Switch Game Cards!
    if (id == "3ds" || id == "nds" || id == "switch") {
        val bandH = labelRect.height() * 0.17f
        val bandRect = if (id == "switch") {
            RectF(labelRect.left, labelRect.top, labelRect.right, labelRect.top + bandH)
        } else {
            RectF(labelRect.left, labelRect.bottom - bandH, labelRect.right, labelRect.bottom)
        }
        p.color = when (id) {
            "switch" -> AndroidColor.rgb(228, 0, 18)
            "3ds" -> AndroidColor.rgb(245, 246, 250)
            else -> AndroidColor.rgb(236, 238, 244)
        }
        c.drawRect(bandRect, p)
        if (id == "3ds") {
            p.color = AndroidColor.rgb(218, 20, 32)
            c.drawRect(bandRect.right - bandRect.width() * 0.18f, bandRect.top, bandRect.right, bandRect.bottom, p)
        }
        val tagTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (id == "switch") AndroidColor.WHITE else AndroidColor.rgb(34, 38, 46)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = bandH * 0.44f
            letterSpacing = 0.06f
            textAlign = Paint.Align.CENTER
        }
        val cardBadge = when (id) {
            "switch" -> "SWITCH"
            "3ds" -> "NINTENDO 3DS"
            else -> "NINTENDO DS"
        }
        val fm = tagTp.fontMetrics
        c.drawText(
            cardBadge,
            bandRect.centerX() - (if (id == "3ds") bandRect.width() * 0.06f else 0f),
            bandRect.centerY() - (fm.ascent + fm.descent) * 0.5f,
            tagTp,
        )
    }

    // Subtle inner top-lip cast shadow inside the recessed sticker well
    p.shader = LinearGradient(
        labelRect.left, labelRect.top, labelRect.left, labelRect.top + labelRect.height() * 0.15f,
        intArrayOf(AndroidColor.argb(78, 0, 0, 0), AndroidColor.TRANSPARENT),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(labelRect, 12f, 12f, p)
    p.shader = null

    // Glossy vinyl sticker diagonal specular sheen
    p.shader = LinearGradient(
        labelRect.left, labelRect.top, labelRect.right, labelRect.bottom,
        intArrayOf(
            AndroidColor.argb(52, 255, 255, 255),
            AndroidColor.argb(12, 255, 255, 255),
            AndroidColor.argb(0, 255, 255, 255),
            AndroidColor.argb(34, 0, 0, 0),
        ),
        floatArrayOf(0f, 0.34f, 0.62f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(labelRect, 12f, 12f, p)
    p.shader = null

    // Bottom center molded insertion arrow triangle (shifted slightly left on 3DS to center on main body!)
    val arrowCx = if (id == "3ds") w * 0.470f else w * 0.500f
    val arrowTop = if (spec.mediaKind == PhysicalMediaKind.CART_NES) h * 0.852f else h * 0.892f
    val arrowBot = if (spec.mediaKind == PhysicalMediaKind.CART_NES) h * 0.916f else h * 0.946f
    p.color = shadeArgb(shellArgb, 0.54f)
    val arrow = AndroidPath().apply {
        moveTo(arrowCx - w * 0.044f, arrowTop)
        lineTo(arrowCx + w * 0.044f, arrowTop)
        lineTo(arrowCx, arrowBot)
        close()
    }
    c.drawPath(arrow, p)

    // Tactile molded outer rim bevel highlight + shadow along the EXACT console silhouette!
    p.style = Paint.Style.STROKE
    p.strokeWidth = 7f
    p.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        AndroidColor.argb(135, 255, 255, 255),
        AndroidColor.argb(125, 0, 0, 0),
        Shader.TileMode.CLAMP,
    )
    c.drawPath(silhouette, p)
    p.shader = null
    p.style = Paint.Style.FILL
    c.restore()
    return bmp
}

internal fun buildProceduralCartBackBitmap(game: Game, spec: PhysicalMediaSpec): Bitmap {
    val w = 620
    val h = (w / spec.mediaKind.defaultAspect).roundToInt().coerceIn(380, 740)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val shellArgb = spec.mediaShellColor.toArgb()
    val silhouette = buildCartridgeSilhouettePath(spec, w.toFloat(), h.toFloat(), mirrorX = true)

    c.save()
    c.clipPath(silhouette)

    // Injection-molded rear shell body with subtle top-to-bottom shading
    p.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        intArrayOf(
            shadeArgb(shellArgb, 1.08f),
            shellArgb,
            shadeArgb(shellArgb, 0.78f),
        ),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawPath(silhouette, p)
    p.shader = null

    // Console-authentic rear side grip channels for SNES/N64/NES shells
    if (spec.mediaKind == PhysicalMediaKind.CART_SNES || spec.mediaKind == PhysicalMediaKind.CART_N64) {
        p.color = shadeArgb(shellArgb, 0.86f)
        c.drawRoundRect(RectF(18f, h * 0.12f, w * 0.13f, h * 0.72f), 10f, 10f, p)
        c.drawRoundRect(RectF(w * 0.87f, h * 0.12f, w - 18f, h * 0.72f), 10f, 10f, p)
    }

    if (spec.mediaKind == PhysicalMediaKind.CART_DS_SWITCH || spec.mediaKind == PhysicalMediaKind.CART_HUCARD) {
        // Gleaming gold contact fingers exposed along the bottom rear of DS / 3DS / Switch / Vita / HuCard cards!
        val isSwitch = spec.systemId.lowercase(Locale.US) == "switch"
        val pinL = if (isSwitch) w * 0.21f else w * 0.15f
        val pinR = if (isSwitch) w * 0.79f else w * 0.85f
        val pinZoneTop = h * 0.62f
        val pinZoneBottom = h * 0.92f
        p.color = AndroidColor.rgb(22, 62, 38)
        c.drawRoundRect(RectF(pinL, pinZoneTop, pinR, pinZoneBottom), 10f, 10f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = AndroidColor.argb(110, 0, 0, 0)
        c.drawRoundRect(RectF(pinL, pinZoneTop, pinR, pinZoneBottom), 10f, 10f, p)
        p.style = Paint.Style.FILL

        val pins = if (isSwitch) 10 else 14
        val pinSpan = (pinR - pinL) * 0.92f
        val step = pinSpan / pins
        p.shader = LinearGradient(
            0f, pinZoneTop, 0f, pinZoneBottom,
            intArrayOf(
                AndroidColor.rgb(252, 224, 118),
                AndroidColor.rgb(218, 172, 54),
                AndroidColor.rgb(176, 132, 34),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        for (i in 0 until pins) {
            val px = pinL + (pinR - pinL) * 0.04f + i * step
            c.drawRoundRect(RectF(px, pinZoneTop + 7f, px + step * 0.66f, pinZoneBottom - 6f), 4f, 4f, p)
        }
        p.shader = null
        // Plastic divider ribs between contact fingers
        p.color = shadeArgb(shellArgb, 0.72f)
        for (i in 0..pins) {
            val rx = pinL + (pinR - pinL) * 0.032f + i * step
            c.drawRect(rx, pinZoneTop + 2f, rx + 3f, pinZoneBottom - 2f, p)
        }
    } else {
        // Deep recessed metallic brass GameBit security screw wells
        val screwPositions = listOf(w * 0.16f to h * 0.80f, w * 0.84f to h * 0.80f)
        for ((sx, sy) in screwPositions) {
            p.color = shadeArgb(shellArgb, 0.52f)
            c.drawCircle(sx, sy, 15.5f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.6f
            p.color = AndroidColor.argb(55, 255, 255, 255)
            c.drawCircle(sx, sy + 1f, 15.5f, p)
            p.style = Paint.Style.FILL
            p.shader = android.graphics.RadialGradient(
                sx - 3f, sy - 3f, 10f,
                intArrayOf(
                    AndroidColor.rgb(236, 212, 138),
                    AndroidColor.rgb(174, 146, 78),
                    AndroidColor.rgb(96, 78, 38),
                ),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
            c.drawCircle(sx, sy, 9.2f, p)
            p.shader = null
            p.color = AndroidColor.rgb(72, 58, 28)
            c.drawCircle(sx, sy, 3.2f, p)
        }

        // Bottom PCB connector slot mouth recess
        val slotRect = RectF(w * 0.22f, h * 0.87f, w * 0.78f, h * 0.945f)
        p.color = shadeArgb(shellArgb, 0.55f)
        c.drawRoundRect(slotRect, 6f, 6f, p)
    }

    // Recessed rear matte caution sticker plate with hairline catch-light
    val isSwitch = spec.systemId.lowercase(Locale.US) == "switch"
    val warnRect = if (isSwitch) {
        RectF(w * 0.22f, h * 0.16f, w * 0.78f, h * 0.54f)
    } else {
        RectF(w * 0.18f, h * 0.16f, w * 0.82f, h * 0.56f)
    }
    p.color = shadeArgb(shellArgb, 0.68f)
    c.drawRoundRect(warnRect, 14f, 14f, p)
    val labelInset = RectF(warnRect.left + 6f, warnRect.top + 6f, warnRect.right - 6f, warnRect.bottom - 6f)
    p.shader = LinearGradient(
        labelInset.left, labelInset.top, labelInset.left, labelInset.bottom,
        AndroidColor.rgb(218, 220, 224),
        AndroidColor.rgb(192, 196, 202),
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(labelInset, 9f, 9f, p)
    p.shader = null
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1.6f
    p.color = AndroidColor.argb(60, 255, 255, 255)
    c.drawRoundRect(warnRect, 14f, 14f, p)
    p.style = Paint.Style.FILL

    // Crisp printed retro caution typography
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(42, 46, 54)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = if (isSwitch) 14f else 17f
        textAlign = Paint.Align.CENTER
    }
    c.drawText((game.system?.name ?: game.systemId).uppercase(Locale.US), w * 0.5f, labelInset.top + 36f, tp)
    p.color = AndroidColor.argb(70, 42, 46, 54)
    c.drawRect(labelInset.left + 22f, labelInset.top + 48f, labelInset.right - 22f, labelInset.top + 50f, p)

    tp.textSize = if (isSwitch) 11f else 13.5f
    tp.typeface = Typeface.MONOSPACE
    c.drawText("CAUTION · DO NOT TOUCH EDGE PINS", w * 0.5f, labelInset.top + 78f, tp)
    c.drawText("STORE IN PROTECTIVE CASE AFTER PLAY", w * 0.5f, labelInset.top + 102f, tp)
    tp.textSize = if (isSwitch) 9.5f else 11.5f
    tp.color = AndroidColor.rgb(88, 94, 106)
    c.drawText("PAT. PEND. · RETROHERO ARCHIVE", w * 0.5f, labelInset.bottom - 18f, tp)

    // Perimeter molded bevel catch-light along the mirrored silhouette
    p.style = Paint.Style.STROKE
    p.strokeWidth = 6f
    p.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        AndroidColor.argb(95, 255, 255, 255),
        AndroidColor.argb(115, 0, 0, 0),
        Shader.TileMode.CLAMP,
    )
    c.drawPath(silhouette, p)
    p.shader = null
    p.style = Paint.Style.FILL
    c.restore()
    return bmp
}

private data class DiscSilkscreenStyle(
    val crescentTop: Int,
    val crescentBot: Int,
    val primaryTextCol: Int,
    val subTextCol: Int,
    val formatBadge: String,
)

internal fun buildProceduralDiscLabelBitmap(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    coverBmp: Bitmap,
): Bitmap {
    val s = 600
    val cx = s * 0.5f
    val cy = s * 0.5f
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val id = spec.systemId.lowercase(Locale.US)
    val isMiniGc = spec.mediaKind == PhysicalMediaKind.MINI_DISC_GC
    val isUmd = spec.mediaKind == PhysicalMediaKind.UMD_DISC

    val discClip = AndroidPath().apply {
        addCircle(cx, cy, s * 0.495f, AndroidPath.Direction.CW)
    }
    c.save()
    c.clipPath(discClip)

    // Printed cover artwork filling the upper & full circular disc label
    val scale = max(s.toFloat() / coverBmp.width, s.toFloat() / coverBmp.height)
    val dw = coverBmp.width * scale
    val dh = coverBmp.height * scale
    c.drawBitmap(coverBmp, null, RectF((s - dw) * 0.5f, (s - dh) * 0.5f, (s + dw) * 0.5f, (s + dh) * 0.5f), bmpPaint)

    // Subtle radial silkscreen vignette so disc typography & center hub read with real physical contrast
    p.shader = android.graphics.RadialGradient(
        cx, cy, s * 0.50f,
        intArrayOf(
            AndroidColor.argb(40, 0, 0, 0),
            AndroidColor.argb(0, 0, 0, 0),
            AndroidColor.argb(82, 0, 0, 0),
        ),
        floatArrayOf(0.18f, 0.62f, 1f),
        Shader.TileMode.CLAMP,
    )
    c.drawCircle(cx, cy, s * 0.495f, p)
    p.shader = null

    // Console-authentic curved lower silkscreen crescent arc (replacing boxy horizontal stripe!)
    val (crescentTop, crescentBot, primaryTextCol, subTextCol, formatBadge) = when (id) {
        "psx", "ps1" -> DiscSilkscreenStyle(AndroidColor.rgb(18, 20, 26), AndroidColor.rgb(8, 9, 13), AndroidColor.WHITE, AndroidColor.rgb(196, 204, 218), "PlayStation · COMPACT DISC")
        "ps2" -> DiscSilkscreenStyle(AndroidColor.rgb(16, 62, 168), AndroidColor.rgb(9, 34, 98), AndroidColor.WHITE, AndroidColor.rgb(196, 218, 255), "PlayStation 2 · DVD-ROM")
        "gc", "gamecube" -> DiscSilkscreenStyle(AndroidColor.rgb(74, 54, 158), AndroidColor.rgb(44, 30, 104), AndroidColor.WHITE, AndroidColor.rgb(218, 208, 255), "NINTENDO GAMECUBE")
        "dreamcast", "dc" -> DiscSilkscreenStyle(AndroidColor.rgb(244, 246, 250), AndroidColor.rgb(218, 222, 232), AndroidColor.rgb(28, 32, 40), AndroidColor.rgb(96, 102, 114), "SEGA · GD-ROM")
        "wii", "wiiu" -> DiscSilkscreenStyle(AndroidColor.rgb(244, 248, 252), AndroidColor.rgb(212, 224, 238), AndroidColor.rgb(46, 54, 68), AndroidColor.rgb(0, 138, 214), if (id == "wiiu") "Wii U · OPTICAL DISC" else "Wii · OPTICAL DISC")
        "psp" -> DiscSilkscreenStyle(AndroidColor.rgb(24, 28, 36), AndroidColor.rgb(12, 14, 20), AndroidColor.WHITE, AndroidColor.rgb(196, 204, 218), "PSP · UMD")
        else -> DiscSilkscreenStyle(shadeArgb(spec.spineHeaderColor.toArgb(), 0.78f), shadeArgb(spec.spineHeaderColor.toArgb(), 0.48f), AndroidColor.WHITE, AndroidColor.rgb(220, 226, 238), "${(game.system?.shortName ?: game.systemId).uppercase(Locale.US)} · OPTICAL DISC")
    }

    val crescentPath = AndroidPath().apply {
        moveTo(0f, s * 0.655f)
        quadTo(cx, s * 0.725f, s.toFloat(), s * 0.655f)
        lineTo(s.toFloat(), s.toFloat())
        lineTo(0f, s.toFloat())
        close()
    }
    p.shader = LinearGradient(
        0f, s * 0.655f, 0f, s.toFloat(),
        crescentTop,
        crescentBot,
        Shader.TileMode.CLAMP,
    )
    p.alpha = 238
    c.drawPath(crescentPath, p)
    p.shader = null
    p.alpha = 255

    // Crisp metallic silver trim arc separating cover artwork from the lower silkscreen badge
    p.style = Paint.Style.STROKE
    p.strokeWidth = 3.2f
    p.color = AndroidColor.argb(190, 236, 242, 252)
    val arcLine = AndroidPath().apply {
        moveTo(0f, s * 0.655f)
        quadTo(cx, s * 0.725f, s.toFloat(), s * 0.655f)
    }
    c.drawPath(arcLine, p)
    p.style = Paint.Style.FILL

    // Dreamcast signature orange swirl emblem dot on the pearl-white arc
    if (id == "dreamcast" || id == "dc") {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        p.color = AndroidColor.rgb(255, 98, 24)
        c.drawCircle(cx, s * 0.722f, 11f, p)
        p.style = Paint.Style.FILL
    }

    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextCol
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 25f
        textAlign = Paint.Align.CENTER
    }
    c.drawText(
        TextUtils.ellipsize(game.title, tp, s * 0.64f, TextUtils.TruncateAt.END).toString(),
        cx,
        s * 0.785f,
        tp,
    )
    tp.color = subTextCol
    tp.textSize = 15.5f
    tp.letterSpacing = 0.08f
    c.drawText(formatBadge, cx, s * 0.848f, tp)

    val pubText = dossier.publisher.ifBlank { dossier.developer }.uppercase(Locale.US)
    if (pubText.isNotBlank()) {
        tp.textSize = 12f
        tp.alpha = 185
        c.drawText(TextUtils.ellipsize(pubText, tp, s * 0.48f, TextUtils.TruncateAt.END).toString(), cx, s * 0.896f, tp)
        tp.alpha = 255
    }

    // Subtle concentric silkscreen rim copyright ring near outer edge
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1.6f
    p.color = AndroidColor.argb(95, 240, 246, 255)
    c.drawCircle(cx, cy, s * 0.456f, p)
    c.restore()

    // Outer mirror-silver beveled disc rim + punch out authentic center spindle hole (except UMD which has solid steel hub)
    p.style = Paint.Style.STROKE
    p.strokeWidth = 8.5f
    p.color = if (spec.blackDiscUnderside) AndroidColor.rgb(172, 180, 196) else AndroidColor.rgb(228, 234, 244)
    c.drawCircle(cx, cy, s * 0.489f, p)
    p.style = Paint.Style.FILL
    if (!isUmd) {
        val holeFrac = if (isMiniGc) 0.118f else 0.084f
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        c.drawCircle(cx, cy, s * holeFrac, p)
        p.xfermode = null
    }
    return bmp
}

internal fun buildProceduralDiscUndersideBitmap(spec: PhysicalMediaSpec): Bitmap {
    val s = 600
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = s * 0.5f
    val cy = s * 0.5f

    p.color = if (spec.blackDiscUnderside) {
        AndroidColor.rgb(14, 15, 20) // Authentic PS1 jet-black polycarbonate!
    } else {
        AndroidColor.rgb(196, 204, 218) // Mirror silver CD/DVD data layer
    }
    c.drawCircle(cx, cy, s * 0.495f, p)

    // Iridescent diffraction sweep (subtle violet/indigo sheen on PS1 black polycarbonate, luminous rainbow on silver CD/DVD)
    p.shader = android.graphics.SweepGradient(
        cx, cy,
        if (spec.blackDiscUnderside) {
            intArrayOf(
                AndroidColor.argb(12, 120, 140, 255),
                AndroidColor.argb(65, 130, 100, 255),
                AndroidColor.argb(42, 80, 190, 255),
                AndroidColor.argb(10, 40, 44, 60),
                AndroidColor.argb(58, 160, 110, 255),
                AndroidColor.argb(12, 120, 140, 255),
            )
        } else {
            intArrayOf(
                AndroidColor.argb(30, 255, 255, 255),
                AndroidColor.argb(95, 80, 235, 255),
                AndroidColor.argb(90, 255, 110, 220),
                AndroidColor.argb(85, 255, 230, 100),
                AndroidColor.argb(85, 110, 255, 160),
                AndroidColor.argb(30, 255, 255, 255),
            )
        },
        null,
    )
    c.drawCircle(cx, cy, s * 0.488f, p)
    p.shader = null

    // Fine concentric data-track grooves + outer beveled edge rim
    p.style = Paint.Style.STROKE
    p.strokeWidth = 2f
    p.color = if (spec.blackDiscUnderside) {
        AndroidColor.argb(55, 170, 185, 235)
    } else {
        AndroidColor.argb(85, 255, 255, 255)
    }
    for (r in listOf(0.20f, 0.29f, 0.38f, 0.45f, 0.49f)) {
        c.drawCircle(cx, cy, s * r, p)
    }

    // Console-specific features, placed at their real radius (12 cm discs: 60 mm; GameCube: 40 mm).
    val discRadiusMm = if (spec.mediaKind == PhysicalMediaKind.MINI_DISC_GC) 40f else 60f
    fun mm(r: Float) = s * 0.495f * r / discRadiusMm
    when (spec.systemId) {
        "dreamcast" -> {
            // GD-ROM: dark grey inner low-density area, a visible separator ring, then the light grey
            // high-density area toward the edge (Redump wiki; dreamcast.wiki).
            p.style = Paint.Style.FILL
            p.color = AndroidColor.argb(70, 236, 238, 244)
            c.drawCircle(cx, cy, mm(58.5f), p)
            p.color = AndroidColor.argb(150, 70, 74, 84)
            c.drawCircle(cx, cy, mm(33f), p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = mm(1.6f)
            p.color = AndroidColor.argb(200, 205, 210, 220)
            c.drawCircle(cx, cy, mm(34f), p)
        }
        "gc", "wii", "wiiu" -> {
            // Burst cutting area: the laser-cut barcode ring near the hole on every Nintendo disc
            // (Wikipedia, Nintendo optical discs). Same physical radius on 8 cm and 12 cm discs.
            p.style = Paint.Style.STROKE
            val inner = mm(22.3f)
            val outer = mm(23.5f)
            p.strokeWidth = outer - inner
            val rnd = java.util.Random(spec.systemId.hashCode().toLong())
            var angle = 0f
            while (angle < 360f) {
                val bar = 0.6f + rnd.nextFloat() * 1.8f
                p.color = AndroidColor.argb(150 + rnd.nextInt(80), 40, 42, 50)
                c.drawArc(cx - (inner + outer) / 2f, cy - (inner + outer) / 2f, cx + (inner + outer) / 2f, cy + (inner + outer) / 2f, angle, bar, false, p)
                angle += bar + 0.6f + rnd.nextFloat() * 2.2f
            }
        }
    }
    p.style = Paint.Style.STROKE

    // Inner silver mirror clamping ring + translucent acrylic hub bevel
    p.strokeWidth = s * 0.036f
    p.color = if (spec.blackDiscUnderside) AndroidColor.rgb(188, 196, 214) else AndroidColor.rgb(228, 233, 242)
    c.drawCircle(cx, cy, s * 0.165f, p)
    p.strokeWidth = 4f
    p.color = AndroidColor.argb(140, 255, 255, 255)
    c.drawCircle(cx, cy, s * 0.115f, p)

    p.style = Paint.Style.FILL
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    c.drawCircle(cx, cy, s * 0.082f, p)
    p.xfermode = null
    return bmp
}

// ---- Gesture, Tilt Sensor & Angle Formatting Helpers --------------------------------------------

private fun normalizeSignedDegrees(deg: Float): Float {
    var d = deg % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

private fun describeVisibleAngle(
    subject: PhysicalViewerSubject,
    yawDeg: Float,
    spec: PhysicalMediaSpec,
): String {
    val y = normalizeSignedDegrees(yawDeg)
    val ay = abs(y)
    return if (subject == PhysicalViewerSubject.RETAIL_BOX) {
        when {
            ay < 18f -> "Front Cover"
            y in 18f..62f -> "Front & Spine"
            y in 62f..122f -> "Box Spine"
            ay > 142f -> "Back of Box"
            y in -122f..-62f -> if (spec.caseStyle.hasPlasticRim) "Case Opening Edge" else "Right Spine"
            else -> "3D Retail Case"
        }
    } else if (spec.mediaKind.isOpticalDisc) {
        when {
            ay < 88f -> "${spec.mediaKind.tabLabel} Artwork"
            spec.blackDiscUnderside -> "PlayStation Black Data Side"
            else -> "Iridescent Data Side"
        }
    } else {
        if (ay < 92f) "${spec.mediaKind.tabLabel} Front Label" else "${spec.mediaKind.tabLabel} Rear Shell"
    }
}

/**
 * Captures horizontal spin drags cleanly without fighting vertical `LazyColumn` scrolling:
 * if the user's initial movement is horizontal (`|dx| > |dy|`), locks into smooth 360° 3D spin + fling;
 * if vertical, yields immediately so the parent page scrolls naturally.
 */
private fun Modifier.horizontalSpinGesture(
    onDragStart: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onFlingVelocityX: (vx: Float) -> Unit,
): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lockedHorizontal: Boolean? = null
        var accX = 0f
        var accY = 0f
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                if (lockedHorizontal == true) {
                    onFlingVelocityX(tracker.calculateVelocity().x)
                }
                break
            }
            val delta = change.positionChange()
            if (lockedHorizontal == null) {
                accX += delta.x
                accY += delta.y
                if (abs(accX) > touchSlop || abs(accY) > touchSlop) {
                    lockedHorizontal = abs(accX) >= abs(accY) * 0.85f
                    if (lockedHorizontal == true) {
                        onDragStart()
                        change.consume()
                        onDragDelta(accX, accY)
                    } else {
                        break // Let LazyColumn scroll vertically
                    }
                }
            } else if (lockedHorizontal == true) {
                tracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
                onDragDelta(delta.x, delta.y)
            }
        }
    }
}

/**
 * Zero-permission phone tilt reader (`TYPE_GAME_ROTATION_VECTOR` -> `TYPE_ROTATION_VECTOR` -> `TYPE_ACCELEROMETER`)
 * returning `(sensorPresent, State<Offset(yawOffsetDeg, pitchOffsetDeg)>)` relative to the user's natural grip angle.
 */
@Composable
private fun rememberDeviceTiltAngles(
    enabled: Boolean,
    recenterNonce: Int,
): Pair<Boolean, State<Offset>> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    val offsetState = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(sensor, enabled, recenterNonce, lifecycleOwner) {
        if (!enabled || sensor == null || sensorManager == null) {
            offsetState.value = Offset.Zero
            return@DisposableEffect onDispose {}
        }

        var hasBaseline = false
        var baseRoll = 0f
        var basePitch = 0f
        var smoothYaw = 0f
        var smoothPitch = 0f
        val rotMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (rawRollDeg, rawPitchDeg) = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val ax = event.values.getOrElse(0) { 0f }
                    val ay = event.values.getOrElse(1) { 0f }
                    (-ax * 3.8f) to ((ay - 5.5f) * 2.4f)
                } else {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    roll to pitch
                }

                if (!hasBaseline) {
                    baseRoll = rawRollDeg
                    basePitch = rawPitchDeg
                    hasBaseline = true
                }
                val targetYaw = (rawRollDeg - baseRoll).coerceIn(-16f, 16f)
                val targetPitch = (rawPitchDeg - basePitch).coerceIn(-11f, 11f)
                smoothYaw += (targetYaw - smoothYaw) * 0.16f
                smoothPitch += (targetPitch - smoothPitch) * 0.16f
                offsetState.value = Offset(smoothYaw, smoothPitch)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        var registered = false
        fun registerIfNeeded() {
            if (!registered) {
                registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        fun unregisterIfNeeded() {
            if (registered) {
                sensorManager.unregisterListener(listener)
                registered = false
            }
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registerIfNeeded()
        }
        val observer = LifecycleEventObserver { _, ev ->
            when (ev) {
                Lifecycle.Event.ON_RESUME -> {
                    hasBaseline = false
                    registerIfNeeded()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> unregisterIfNeeded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregisterIfNeeded()
        }
    }

    return (sensor != null) to offsetState
}

// ---- Physical Shelf: the same 3D cases as the game page, at shelf size ----------------------------

/** Textures for one case standing on the shelf, plus the front aspect that shapes it in 3D. */
internal class ShelfCaseArt(val textures: ResolvedPhysicalTextures, val boxAspect: Float) {
    val bytes: Int get() = textures.spine.byteCount + (if (textures.front !== textures.spine) textures.front.byteCount else 0)
}

/** Shelf cases kept within a fixed memory budget (full-size viewer textures are several MB each). */
private object ShelfCaseCache {
    private val cache = object : android.util.LruCache<String, ShelfCaseArt>(24 * 1024) {
        override fun sizeOf(key: String, value: ShelfCaseArt) = (value.bytes / 1024).coerceAtLeast(1)
    }
    private val byGame = android.util.LruCache<String, ShelfCaseArt>(64)
    fun get(key: String): ShelfCaseArt? = cache.get(key)
    fun put(key: String, value: ShelfCaseArt) { cache.put(key, value) }
    fun peekGame(gameKey: String): ShelfCaseArt? = byGame.get(gameKey)
    fun putGame(gameKey: String, value: ShelfCaseArt) {
        val prev = byGame.get(gameKey)
        if (prev == null || value.textures.front.width >= prev.textures.front.width || value.textures.hasRealSpine) {
            byGame.put(gameKey, value)
        }
    }
}

private const val SHELF_SPINE_WIDTH_PX = 64

/** At most two LaunchBox lookups at a time from the shelf, so a big library doesn't flood it. */
private val shelfLookupGate = kotlinx.coroutines.sync.Semaphore(2)
private const val SHELF_COVER_PX = 220

/**
 * Builds what the shared 3D renderer needs to draw [game]'s case on the shelf, from the same
 * sources and in the same order as the game page's 3D viewer: the LaunchBox `Box - Spine` scan,
 * else the spine slice of `Box - Full`, else the same generated spine; the cover from your own
 * cover, the LaunchBox front, then online art. Textures are shelf-sized and built off the main
 * thread. [dossierFor] is called off the main thread too. Null until ready.
 */
@Composable
internal fun rememberShelfCaseArt(
    game: Game,
    spec: PhysicalMediaSpec,
    dossierFor: (Game) -> GameDossier?,
    /** Looks the game up on LaunchBox (as its game page does) when nothing is cached yet. */
    loadDossier: (suspend (Game) -> GameDossier)? = null,
    frontWidthPx: Int = SHELF_COVER_PX,
    proceduralFrontFallback: Boolean = false,
): ShelfCaseArt? {
    val context = LocalContext.current
    val paths = LocalArtPaths.current
    val artVersion = LocalArtVersion.current
    val gameKey = "${game.id}|$artVersion"
    val baseKey = "${game.id}|${spec.spineHeaderColor.toArgb()}|$artVersion|$frontWidthPx|$proceduralFrontFallback"
    val state = androidx.compose.runtime.produceState(initialValue = ShelfCaseCache.get(baseKey), baseKey) {
        // Shows the last case at once, then re-checks: the game page may have fetched real scans since.
        val cached = withContext(Dispatchers.IO) { dossierFor(game) }
        var dossier = cached ?: GameDossier.localFallback(game)
        fun keyFor(d: GameDossier) = "$baseKey|${d.boxSpineUrl}|${d.boxFullUrl}|${d.boxArtUrl}|${d.title}|${d.publisher}"
        ShelfCaseCache.get(keyFor(dossier))?.let { value = it }

        suspend fun load(src: Any?, max: Int): Bitmap? {
            if (src == null) return null
            if (src is java.io.File) {
                val exists = withContext(Dispatchers.IO) { src.exists() && src.length() > 0L }
                if (!exists) return null
            }
            return runCatching {
                val req = ImageRequest.Builder(context)
                    .data(src)
                    .allowHardware(false)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .size(max, max)
                    .scale(Scale.FIT)
                    .build()
                (context.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap(config = Bitmap.Config.ARGB_8888)
            }.getOrNull()?.takeIf { it.width >= 16 && it.height >= 16 }
        }

        suspend fun build(cover: Bitmap?, loadedSpine: Bitmap?, loadedFull: Bitmap?): ShelfCaseArt = withContext(Dispatchers.Default) {
            val sliced = loadedFull?.let {
                PhysicalMediaSpec.sliceFullBoxCover(it, spec.fullWrapSpineFraction)
            }
            val front = (cover?.let { trimTransparentBounds(it) }) ?: sliced?.third
            // Same aspect rules as the 3D viewer: the real front, else the generated front's shape.
            val aspect = (front?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: ShelfLayoutMath.proceduralFrontAspect(spec))
                .coerceIn(0.56f, 1.48f)
            val realSpine = (loadedSpine?.let { trimTransparentBounds(it) } ?: sliced?.second)
                ?.let { PhysicalMediaSpec.orientSpineVertical(it) }
            val spineFull = realSpine ?: buildProceduralSpineBitmap(game, dossier, spec, front, aspect)
            val spineH = (SHELF_SPINE_WIDTH_PX * spineFull.height.toFloat() / spineFull.width).toInt().coerceIn(SHELF_SPINE_WIDTH_PX, 900)
            val spineSmall = Bitmap.createScaledBitmap(spineFull, SHELF_SPINE_WIDTH_PX, spineH, true)
            val frontSmall = if (front != null) {
                Bitmap.createScaledBitmap(front, frontWidthPx, (frontWidthPx / aspect).toInt().coerceAtLeast(16), true)
            } else if (proceduralFrontFallback) {
                val proc = buildProceduralFrontBitmap(game, dossier, spec)
                Bitmap.createScaledBitmap(proc, frontWidthPx, (frontWidthPx / aspect).toInt().coerceAtLeast(16), true)
            } else {
                // No cover: a plain face in the spine's body color (only a sliver ever shows on the shelf).
                val body = spineSmall.getPixel(spineSmall.width / 2, spineSmall.height / 2)
                Bitmap.createBitmap(32, (32 / aspect).toInt().coerceAtLeast(8), Bitmap.Config.ARGB_8888).apply { eraseColor(body) }
            }
            ShelfCaseArt(
                textures = ResolvedPhysicalTextures(
                    front = frontSmall,
                    back = frontSmall,
                    spine = spineSmall,
                    mediaFront = frontSmall,
                    mediaBack = frontSmall,
                    hasRealFront = front != null,
                    hasRealBack = false,
                    hasRealSpine = realSpine != null,
                    hasRealMedia = false,
                ),
                boxAspect = aspect,
            )
        }

        val sources = artSources(game, paths)
        val localCovers = sources.filterIsInstance<java.io.File>()

        // 1. Right away, from what's on the phone: a 3D case with your own cover (if any) and the
        //    same generated spine the game page would show. No waiting on the network.
        var cover: Bitmap? = null
        for (src in localCovers) {
            cover = load(src, 720)
            if (cover != null) break
        }
        if (value == null) {
            val built = build(cover, null, null)
            ShelfCaseCache.put(baseKey, built)
            ShelfCaseCache.putGame(gameKey, built)
            value = built
        }

        // 2. Never looked up: fetch the game's LaunchBox record, as its game page would, to get the
        //    real Box - Spine / Box - Full / Box - Front scans.
        if (cached == null && loadDossier != null) {
            shelfLookupGate.withPermit { runCatching { loadDossier(game) }.getOrNull() }?.let { dossier = it }
        }
        val key = keyFor(dossier)
        ShelfCaseCache.get(key)?.let {
            ShelfCaseCache.putGame(gameKey, it)
            value = it
            return@produceState
        }

        // 3. Then online art in parallel: cover (if none yet) + LaunchBox spine, then full wrap only if needed.
        val (onlineCover, loadedSpine) = coroutineScope {
            val coverDef = async {
                if (cover != null) {
                    cover
                } else {
                    val onlineCovers = buildList<Any> {
                        dossier.boxArtUrl?.let { add(it) }
                        sources.forEach { if (it !is java.io.File && it !in this) add(it) }
                    }
                    var found: Bitmap? = null
                    for (src in onlineCovers) {
                        found = load(src, 640)
                        if (found != null) break
                    }
                    found
                }
            }
            val spineDef = async { load(dossier.boxSpineUrl, 768) }
            coverDef.await() to spineDef.await()
        }
        cover = onlineCover
        val loadedFull = if (loadedSpine == null || cover == null) load(dossier.boxFullUrl, 960) else null
        val art = build(cover, loadedSpine, loadedFull)
        ShelfCaseCache.put(key, art)
        ShelfCaseCache.put(baseKey, art)
        ShelfCaseCache.putGame(gameKey, art)
        value = art
    }
    return state.value
}

private const val SHOWCASE_COVER_PX = 380

/**
 * Renders a game's physical retail case in crisp 3/4 3D view using the exact same [Physical3dStageCanvas]
 * engine, LaunchBox front & spine scans, acrylic jewel-case ribs, keep-case wrap seams, cardboard
 * creases, and grounded studio shadow as the Game Information screen's 3D Box Viewer.
 */
@Composable
fun M3PhysicalCaseShowcase(
    game: Game,
    modifier: Modifier = Modifier,
    dossierFor: (Game) -> GameDossier? = { null },
    loadDossier: (suspend (Game) -> GameDossier)? = null,
    yawDeg: Float = 24f,
    pitchDeg: Float = -6f,
    stagePaddingDp: androidx.compose.ui.unit.Dp = 8.dp,
    transparentStage: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val viewerAccent = game.system?.color ?: cs.primary
    val spec = remember(game.systemId, viewerAccent) { PhysicalMediaSpec.forSystem(game.systemId, viewerAccent) }
    val art = rememberShelfCaseArt(
        game = game,
        spec = spec,
        dossierFor = dossierFor,
        loadDossier = loadDossier,
        frontWidthPx = SHOWCASE_COVER_PX,
        proceduralFrontFallback = true,
    )

    val stageModifier = if (transparentStage) {
        modifier
    } else {
        modifier.background(
            Brush.radialGradient(
                0.0f to cs.primaryContainer.copy(alpha = 0.24f),
                0.62f to Color(0xFF14131B),
                1.0f to Color(0xFF0A0A0F),
            ),
        )
    }

    Box(
        modifier = stageModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.RETAIL_BOX,
                spec = spec,
                textures = art.textures,
                boxAspect = art.boxAspect,
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                spineFit = false,
                floorShadow = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(stagePaddingDp),
            )
        } else {
            M3GameCover(game = game, showTitle = false, modifier = Modifier.fillMaxSize())
        }
    }
}
