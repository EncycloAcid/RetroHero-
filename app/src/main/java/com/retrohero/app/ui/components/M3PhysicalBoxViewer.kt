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
private data class ResolvedPhysicalTextures(
    val front: Bitmap,
    val back: Bitmap,
    val spine: Bitmap,
    val mediaFront: Bitmap,
    val mediaBack: Bitmap,
    val hasRealFront: Boolean,
    val hasRealBack: Boolean,
    val hasRealSpine: Boolean,
    val hasRealMedia: Boolean,
)

/**
 * LaunchBox-inspired Interactive 3D Retail Box, Spine & Cartridge/Disc Spin Viewer for the Game Page.
 *
 * - Drag horizontally (with momentum fling) or tilt your phone to rotate the full 6-face 3D game case.
 * - Flip 180° to read the real LaunchBox `Box - Back` blurb (or authentic reconstructed back-of-box sleeve),
 *   inspect the `Box - Spine`, and switch to the console-authentic 3D Cartridge / Game Card / Optical Disc!
 */
@Composable
fun M3PhysicalBoxViewerSection(
    game: Game,
    dossier: GameDossier,
    openInspectorTrigger: Int = 0,
    initialSubject: PhysicalViewerSubject = PhysicalViewerSubject.RETAIL_BOX,
    initialYawDeg: Float = 24f,
    overrideBitmaps: PhysicalViewerBitmaps? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()

    val sysAccent = game.system?.color ?: cs.primary
    val spec = remember(game.systemId, sysAccent) {
        PhysicalMediaSpec.forSystem(game.systemId, sysAccent)
    }

    val textures = rememberPhysicalTextures(game, dossier, spec, overrideBitmaps)

    var subject by remember(game.id) { mutableStateOf(initialSubject) }
    val yawAnim = remember(game.id) { Animatable(initialYawDeg) }
    var pitchDeg by remember(game.id) { mutableFloatStateOf(-5f) }
    var autoSpin by remember(game.id) { mutableStateOf(false) }
    var tiltEnabled by remember(game.id) { mutableStateOf(!reducedMotion) }
    var tiltResetNonce by remember(game.id) { mutableIntStateOf(0) }
    var showInspector by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(openInspectorTrigger) {
        if (openInspectorTrigger > 0) {
            showInspector = true
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
        textures.hasRealFront,
        textures.hasRealSpine,
        textures.hasRealBack,
        textures.hasRealMedia,
        spec.mediaKind.tabLabel,
    ) {
        val parts = buildList {
            if (textures.hasRealFront) add("Front")
            if (textures.hasRealSpine) add("Spine")
            if (textures.hasRealBack) add("Back")
            if (textures.hasRealMedia) add(spec.mediaKind.tabLabel)
        }
        if (parts.isNotEmpty()) "LaunchBox · ${parts.joinToString(" · ")}"
        else "${spec.caseStyle.label} · 3D Reconstruction"
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
                        "Drag to spin 360° · Tilt phone for live glint"
                    } else {
                        "Drag to spin 360° · Flip over to read back of box"
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

            // Top-left provenance pill (shows real LaunchBox faces loaded)
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
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

            // Top-right Fullscreen Zoom / Read Back button
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer.copy(alpha = 0.92f))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        showInspector = true
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

        // LaunchBox-style Quick Angle & Turntable Controls Strip
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    label = "Back Blurb",
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
                    label = if (autoSpin) "Spinning" else "Auto-Spin",
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
            onDismiss = { showInspector = false },
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
private fun M3PhysicalBoxInspectorDialog(
    game: Game,
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    initialSubject: PhysicalViewerSubject,
    initialYawDeg: Float,
    boxAspect: Float,
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

@Composable
private fun Physical3dStageCanvas(
    subject: PhysicalViewerSubject,
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    boxAspect: Float,
    yawDeg: Float,
    pitchDeg: Float,
    modifier: Modifier = Modifier,
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

    Canvas(modifier) {
        val stageW = size.width
        val stageH = size.height
        if (stageW <= 16f || stageH <= 16f) return@Canvas

        val cx = stageW * 0.5f
        val cy = stageH * 0.48f
        val focal = max(stageW, stageH) * 2.35f

        val isDisc = subject == PhysicalViewerSubject.GAME_MEDIA && spec.mediaKind.isOpticalDisc
        val targetAspect = when {
            subject == PhysicalViewerSubject.RETAIL_BOX -> boxAspect
            isDisc -> 1.0f
            else -> (textures.mediaFront.width.toFloat() / textures.mediaFront.height.toFloat().coerceAtLeast(1f))
                .coerceIn(0.62f, 1.65f)
        }
        val depthRatio = when {
            subject == PhysicalViewerSubject.RETAIL_BOX -> spec.caseStyle.depthRatio
            else -> spec.mediaKind.thicknessRatio
        }

        // Fit cleanly inside the stage regardless of portrait vs landscape box proportions
        val maxBoxH = stageH * if (isDisc) 0.78f else 0.80f
        val maxBoxW = stageW * 0.70f
        var halfH = maxBoxH * 0.5f
        var halfW = halfH * targetAspect
        if (halfW * 2f > maxBoxW) {
            halfW = maxBoxW * 0.5f
            halfH = halfW / targetAspect
        }
        val halfD = max(halfW, halfH) * depthRatio

        // Perspective-coupled contact shadow on the pedestal beneath the floating 3D object
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val projectedFootprintW = abs(halfW * cos(yawRad).toFloat()) + abs(halfD * sin(yawRad).toFloat())
        val shadowRadiusX = (projectedFootprintW * 1.18f).coerceAtLeast(halfD * 1.8f)
        val shadowY = cy + halfH * 1.10f
        drawOval(
            brush = Brush.radialGradient(
                0.0f to Color.Black.copy(alpha = 0.42f),
                0.62f to Color.Black.copy(alpha = 0.16f),
                1.0f to Color.Transparent,
                center = Offset(cx, shadowY),
                radius = shadowRadiusX.coerceAtLeast(12f),
            ),
            topLeft = Offset(cx - shadowRadiusX, shadowY - halfH * 0.11f),
            size = androidx.compose.ui.geometry.Size(shadowRadiusX * 2f, halfH * 0.22f),
        )

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
            } else {
                val caseColorArgb = if (subject == PhysicalViewerSubject.RETAIL_BOX) {
                    spec.casePlasticColor.toArgb()
                } else {
                    spec.mediaShellColor.toArgb()
                }

                for (q in quads) {
                    val d = q.dstPoints
                    clipPath.reset()
                    clipPath.moveTo(d[0], d[1])
                    clipPath.lineTo(d[2], d[3])
                    clipPath.lineTo(d[4], d[5])
                    clipPath.lineTo(d[6], d[7])
                    clipPath.close()

                    when (q.face) {
                        BoxFaceId.FRONT, BoxFaceId.BACK, BoxFaceId.LEFT_SPINE -> {
                            val faceBmp = when {
                                subject == PhysicalViewerSubject.GAME_MEDIA && q.face == BoxFaceId.FRONT -> textures.mediaFront
                                subject == PhysicalViewerSubject.GAME_MEDIA && q.face == BoxFaceId.BACK -> textures.mediaBack
                                subject == PhysicalViewerSubject.GAME_MEDIA -> null
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

                                    // Optional molded plastic keep-case / jewel-case outer rim around paper insert
                                    if (subject == PhysicalViewerSubject.RETAIL_BOX && spec.caseStyle.hasPlasticRim) {
                                        fillPaint.shader = null
                                        fillPaint.color = shadeArgb(caseColorArgb, q.shade)
                                        native.drawRect(0f, 0f, bw, bh, fillPaint)
                                        val padX = if (q.face == BoxFaceId.LEFT_SPINE) 0f else bw * 0.022f
                                        val padY = bh * 0.018f
                                        val dstRect = RectF(
                                            if (q.face == BoxFaceId.FRONT) 0f else padX,
                                            padY,
                                            if (q.face == BoxFaceId.BACK) bw else bw - padX,
                                            bh - padY,
                                        )
                                        native.drawBitmap(faceBmp, null, dstRect, bmpPaint)

                                        // Clear acrylic CD jewel case left hinge ribs
                                        if (spec.caseStyle == RetailCaseStyle.JEWEL_CASE_CD && q.face == BoxFaceId.FRONT) {
                                            val hingeW = bw * 0.068f
                                            fillPaint.color = AndroidColor.argb(95, 235, 242, 255)
                                            native.drawRect(0f, 0f, hingeW, bh, fillPaint)
                                            fillPaint.color = AndroidColor.argb(120, 20, 22, 28)
                                            native.drawRect(hingeW - 2f, 0f, hingeW, bh, fillPaint)
                                        }
                                    } else {
                                        native.drawBitmap(faceBmp, 0f, 0f, bmpPaint)
                                    }

                                    // Directional 3D shading + moving glossy sleeve specular highlight
                                    drawFaceLightingAndGloss(
                                        native = native,
                                        bw = bw,
                                        bh = bh,
                                        shade = q.shade,
                                        specular = q.specular,
                                        glossy = subject == PhysicalViewerSubject.RETAIL_BOX && spec.caseStyle.glossyWrap,
                                        yawDeg = yawDeg,
                                        pitchDeg = pitchDeg,
                                        paint = fillPaint,
                                    )
                                    native.restore()
                                }
                            } else {
                                // Cartridge side thickness edge
                                drawSolidExtrudedEdge(native, clipPath, d, caseColorArgb, q.shade * 0.88f, fillPaint)
                            }
                        }

                        BoxFaceId.RIGHT_OPENING, BoxFaceId.TOP, BoxFaceId.BOTTOM -> {
                            drawCaseOrCartSideEdge(
                                native = native,
                                clipPath = clipPath,
                                dst = d,
                                face = q.face,
                                subject = subject,
                                spec = spec,
                                baseArgb = caseColorArgb,
                                shade = q.shade,
                                polyMatrix = polyMatrix,
                                fillPaint = fillPaint,
                                srcPts = srcPts,
                            )
                        }
                    }

                    // Crisp anti-aliased bevel crease along every 3D edge
                    strokePaint.strokeWidth = 1.4f
                    strokePaint.color = AndroidColor.argb(75, 255, 255, 255)
                    native.drawPath(clipPath, strokePaint)
                }
            }
        }
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
        fillPaint.color = AndroidColor.argb(230, 210, 220, 235)
        native.drawPath(path, fillPaint)
        return
    }

    val isLabelSide = mainFace.face == BoxFaceId.FRONT
    val tex = if (isLabelSide) labelBmp else undersideBmp
    val bw = tex.width.toFloat()
    val bh = tex.height.toFloat()
    srcPts[0] = 0f; srcPts[1] = 0f
    srcPts[2] = bw; srcPts[3] = 0f
    srcPts[4] = bw; srcPts[5] = bh
    srcPts[6] = 0f; srcPts[7] = bh

    val d = mainFace.dstPoints
    // Subtle extruded polycarbonate thickness rim slightly offset behind the disc
    val rimShiftX = -sin(Math.toRadians(yawDeg.toDouble())).toFloat() * 5.5f
    val rimShiftY = sin(Math.toRadians(pitchDeg.toDouble())).toFloat() * 4.0f
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
        fillPaint.color = if (spec.blackDiscUnderside) AndroidColor.rgb(32, 34, 42) else AndroidColor.rgb(185, 196, 212)
        val discPath = AndroidPath().apply {
            fillType = AndroidPath.FillType.EVEN_ODD
            addCircle(bw * 0.5f, bh * 0.5f, bw * 0.495f, AndroidPath.Direction.CW)
            addCircle(bw * 0.5f, bh * 0.5f, bw * 0.082f, AndroidPath.Direction.CW)
        }
        native.drawPath(discPath, fillPaint)
        native.restore()
    }

    if (polyMatrix.setPolyToPoly(srcPts, 0, d, 0, 4)) {
        native.save()
        native.concat(polyMatrix)
        val discClip = AndroidPath().apply {
            fillType = AndroidPath.FillType.EVEN_ODD
            addCircle(bw * 0.5f, bh * 0.5f, bw * 0.495f, AndroidPath.Direction.CW)
            addCircle(bw * 0.5f, bh * 0.5f, bw * 0.082f, AndroidPath.Direction.CW)
        }
        native.clipPath(discClip)
        native.drawBitmap(tex, 0f, 0f, bmpPaint)

        // Live rainbow diffraction / specular reflection sweep on the optical disc!
        val sweepAngle = (yawDeg * 1.6f + pitchDeg * 1.2f)
        native.save()
        native.rotate(sweepAngle, bw * 0.5f, bh * 0.5f)
        fillPaint.shader = SweepGradient(
            bw * 0.5f,
            bh * 0.5f,
            if (isLabelSide) {
                intArrayOf(
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(58, 255, 255, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(42, 255, 255, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                )
            } else if (spec.blackDiscUnderside) {
                // PS1 iconic black polycarbonate sheen
                intArrayOf(
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(95, 190, 210, 255),
                    AndroidColor.argb(35, 170, 120, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(85, 210, 225, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                )
            } else {
                // Iridescent CD/DVD diffraction spectrum
                intArrayOf(
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(92, 0, 230, 255),
                    AndroidColor.argb(88, 120, 255, 150),
                    AndroidColor.argb(88, 255, 210, 70),
                    AndroidColor.argb(84, 255, 80, 200),
                    AndroidColor.argb(0, 255, 255, 255),
                    AndroidColor.argb(88, 80, 210, 255),
                    AndroidColor.argb(0, 255, 255, 255),
                )
            },
            null,
        )
        native.drawRect(0f, 0f, bw, bh, fillPaint)
        fillPaint.shader = null
        native.restore()

        // Translucent polycarbonate inner clamping hub ring
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = bw * 0.052f
            color = AndroidColor.argb(135, 232, 238, 248)
        }
        native.drawCircle(bw * 0.5f, bh * 0.5f, bw * 0.115f, hubPaint)
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
    yawDeg: Float,
    pitchDeg: Float,
    paint: Paint,
) {
    paint.shader = null
    if (shade < 0.98f) {
        val darkAlpha = ((1f - shade) * 190f).roundToInt().coerceIn(0, 135)
        if (darkAlpha > 2) {
            paint.color = AndroidColor.argb(darkAlpha, 6, 8, 14)
            native.drawRect(0f, 0f, bw, bh, paint)
        }
    }
    if (glossy || specular > 0.06f) {
        val sheenCenter = ((yawDeg % 90f) / 90f + 0.5f).coerceIn(0.05f, 0.95f)
        val bandW = bw * 0.38f
        val x0 = bw * sheenCenter - bandW + pitchDeg * 2.2f
        val x1 = x0 + bandW * 2f
        val peakAlpha = ((if (glossy) 48f else 24f) + specular * 54f).roundToInt().coerceIn(14, 96)
        paint.shader = LinearGradient(
            x0,
            0f,
            x1,
            bh * 0.55f,
            intArrayOf(
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(peakAlpha, 255, 255, 255),
                AndroidColor.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
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
        // Molded cartridge shell seam + bottom gold PCB edge-connector pins!
        fillPaint.color = shadeArgb(baseArgb, shade * 0.92f)
        native.drawRect(0f, 0f, ew, eh, fillPaint)
        // Center mold parting seam line
        fillPaint.color = AndroidColor.argb(95, 0, 0, 0)
        native.drawRect(ew * 0.47f, 0f, ew * 0.53f, eh, fillPaint)

        if (face == BoxFaceId.BOTTOM) {
            // Recessed cartridge slot + gleaming gold PCB edge connector pins!
            fillPaint.color = AndroidColor.rgb(18, 22, 20)
            native.drawRect(ew * 0.18f, eh * 0.08f, ew * 0.82f, eh * 0.92f, fillPaint)
            fillPaint.color = AndroidColor.rgb(42, 98, 56) // green PCB board
            native.drawRect(ew * 0.44f, eh * 0.11f, ew * 0.56f, eh * 0.89f, fillPaint)
            fillPaint.color = AndroidColor.rgb(228, 186, 68) // gold connector teeth
            val pins = 20
            val step = (eh * 0.76f) / pins
            for (i in 0 until pins) {
                val py = eh * 0.12f + i * step
                native.drawRect(ew * 0.38f, py, ew * 0.62f, py + step * 0.58f, fillPaint)
            }
        }
    } else if (spec.caseStyle.hasPlasticRim) {
        // Molded DVD / Clamshell / DS / Jewel plastic case opening edge with recessed insert & thumb clasp
        fillPaint.color = shadeArgb(baseArgb, shade * 0.95f)
        native.drawRect(0f, 0f, ew, eh, fillPaint)
        // Recessed interior seam groove
        fillPaint.color = AndroidColor.argb(115, 8, 10, 15)
        native.drawRect(ew * 0.38f, eh * 0.02f, ew * 0.62f, eh * 0.98f, fillPaint)
        if (face == BoxFaceId.RIGHT_OPENING) {
            // Authentic keep-case finger-clasp indent in the middle of the right opening edge!
            fillPaint.color = shadeArgb(baseArgb, shade * 0.70f)
            native.drawRoundRect(RectF(ew * 0.18f, eh * 0.40f, ew * 0.82f, eh * 0.60f), 14f, 14f, fillPaint)
        }
    } else {
        // Printed retail cardboard box flap with fold crease & tuck seam
        val cardColor = shadeArgb(AndroidColor.rgb(46, 48, 58), shade)
        fillPaint.color = cardColor
        native.drawRect(0f, 0f, ew, eh, fillPaint)
        // Accent stripe from console header
        fillPaint.color = shadeArgb(spec.spineHeaderColor.toArgb(), shade)
        native.drawRect(0f, 0f, ew, eh * 0.11f, fillPaint)
        // Cardboard tuck-flap seam line
        fillPaint.color = AndroidColor.argb(95, 0, 0, 0)
        native.drawRect(ew * 0.14f, eh * 0.03f, ew * 0.19f, eh * 0.97f, fillPaint)
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

// ---- Coil Async Texture Loader & Procedural Museum-Grade Fallbacks ------------------------------

@Composable
private fun rememberFirstLoadedBitmap(key: String, candidates: List<Any>): Bitmap? {
    val context = LocalContext.current
    var bmp by remember(key, candidates) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(key, candidates) {
        bmp = null
        for (src in candidates) {
            val loaded = runCatching {
                val req = ImageRequest.Builder(context)
                    .data(src)
                    .allowHardware(false)
                    .size(1024)
                    .build()
                val res = context.imageLoader.execute(req)
                (res as? SuccessResult)?.drawable?.toBitmap()
            }.getOrNull()
            if (loaded != null && loaded.width >= 16 && loaded.height >= 16) {
                bmp = loaded
                break
            }
        }
    }
    return bmp
}

@Composable
private fun rememberPhysicalTextures(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    override: PhysicalViewerBitmaps?,
): ResolvedPhysicalTextures {
    val frontCandidates = remember(game.id, dossier.boxArtUrl) {
        buildList<Any> {
            dossier.boxArtUrl?.let { add(it) }
            addAll(game.boxArtUrls.take(3))
        }
    }
    val backCandidates = remember(game.id, dossier.boxBackUrl) {
        listOfNotNull<Any>(dossier.boxBackUrl)
    }
    val spineCandidates = remember(game.id, dossier.boxSpineUrl) {
        listOfNotNull<Any>(dossier.boxSpineUrl)
    }
    val fullCandidates = remember(game.id, dossier.boxFullUrl) {
        listOfNotNull<Any>(dossier.boxFullUrl)
    }
    val mediaFrontCandidates = remember(game.id, dossier.cartFrontUrl, dossier.discUrl, spec.mediaKind.isOpticalDisc) {
        if (spec.mediaKind.isOpticalDisc) {
            listOfNotNull<Any>(dossier.discUrl, dossier.cartFrontUrl)
        } else {
            listOfNotNull<Any>(dossier.cartFrontUrl, dossier.discUrl)
        }
    }
    val mediaBackCandidates = remember(game.id, dossier.cartBackUrl) {
        listOfNotNull<Any>(dossier.cartBackUrl)
    }
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

    val loadedFront = override?.boxFront ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_f", frontCandidates)
    val loadedBack = override?.boxBack ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_b", backCandidates)
    val loadedSpineRaw = override?.boxSpine ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_s", spineCandidates)
    val loadedFull = override?.boxFull ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_full", fullCandidates)
    val loadedMediaFront = (if (spec.mediaKind.isOpticalDisc) override?.disc ?: override?.cartFront else override?.cartFront ?: override?.disc)
        ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_mf", mediaFrontCandidates)
    val loadedMediaBack = override?.cartBack ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_mb", mediaBackCandidates)
    val shot1 = override?.screenshot1 ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_s1", shot1Candidates)
    val shot2 = override?.screenshot2 ?: if (override != null) null else rememberFirstLoadedBitmap("${game.id}_s2", shot2Candidates)

    // Support LaunchBox `Box - Full` wraparound sleeves when individual Back/Spine/Front scans are absent!
    val slicedFull = remember(loadedFull, spec.caseStyle) {
        loadedFull?.let { PhysicalMediaSpec.sliceFullBoxCover(it, (spec.caseStyle.depthRatio * 0.55f).coerceIn(0.05f, 0.14f)) }
    }

    val realFront = loadedFront ?: slicedFull?.third
    val realBack = loadedBack ?: slicedFull?.first
    val realSpine = remember(loadedSpineRaw, slicedFull) {
        (loadedSpineRaw ?: slicedFull?.second)?.let { PhysicalMediaSpec.orientSpineVertical(it) }
    }

    val frontBmp = remember(game.id, realFront, dossier.title, dossier.publisher, dossier.esrbRating, spec) {
        realFront ?: buildProceduralFrontBitmap(game, dossier, spec)
    }
    val targetAspect = (frontBmp.width.toFloat() / frontBmp.height.toFloat().coerceAtLeast(1f)).coerceIn(0.56f, 1.48f)

    val backBmp = remember(
        game.id,
        realBack,
        targetAspect,
        dossier.title,
        dossier.summary,
        dossier.developer,
        dossier.publisher,
        dossier.esrbRating,
        shot1,
        shot2,
        frontBmp,
    ) {
        realBack ?: buildProceduralBackBitmap(game, dossier, spec, targetAspect, shot1 ?: frontBmp, shot2)
    }

    val spineBmp = remember(game.id, realSpine, dossier.title, dossier.publisher, spec) {
        realSpine ?: buildProceduralSpineBitmap(game, dossier, spec)
    }

    val mediaFrontBmp = remember(game.id, loadedMediaFront, frontBmp, dossier.title, spec) {
        loadedMediaFront ?: if (spec.mediaKind.isOpticalDisc) {
            buildProceduralDiscLabelBitmap(game, dossier, spec, frontBmp)
        } else {
            buildProceduralCartFrontBitmap(game, dossier, spec, frontBmp)
        }
    }

    val mediaBackBmp = remember(game.id, loadedMediaBack, spec) {
        loadedMediaBack ?: if (spec.mediaKind.isOpticalDisc) {
            buildProceduralDiscUndersideBitmap(spec)
        } else {
            buildProceduralCartBackBitmap(game, spec)
        }
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
        hasRealMedia = loadedMediaFront != null,
    )
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

    // Decorative retro grid / emblem backdrop
    paint.color = AndroidColor.argb(34, 255, 255, 255)
    c.drawCircle(w * 0.72f, h * 0.46f, min(w, h) * 0.28f, paint)
    paint.color = shadeArgb(headerArgb, 1.15f)
    paint.alpha = 55
    c.drawCircle(w * 0.68f, h * 0.48f, min(w, h) * 0.19f, paint)
    paint.alpha = 255

    // Bold retail cover title
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = min(w, h) * 0.095f
    }
    val titleLayout = StaticLayout.Builder
        .obtain(game.title, 0, game.title.length, titlePaint, (w * 0.84f).toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(3)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    c.save()
    c.translate(w * 0.08f, h * 0.34f)
    titleLayout.draw(c)
    c.restore()

    // Bottom ESRB + Publisher stamp
    drawEsrbStamp(c, dossier.esrbRating.ifBlank { "E" }, w * 0.07f, h * 0.81f, min(w, h) * 0.13f)
    val pubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(215, 235, 238, 248)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = min(w, h) * 0.036f
    }
    val pub = dossier.publisher.ifBlank { dossier.developer }.uppercase(Locale.US)
    val pubW = pubPaint.measureText(pub)
    c.drawText(pub, (w * 0.93f - pubW).coerceAtLeast(w * 0.28f), h * 0.90f, pubPaint)
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
): Bitmap {
    val w = 136
    val h = 760
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    val headerArgb = spec.spineHeaderColor.toArgb()
    p.color = AndroidColor.rgb(24, 26, 34)
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

    // Top console color cap on spine
    val capH = h * 0.16f
    p.color = headerArgb
    c.drawRect(0f, 0f, w.toFloat(), capH, p)

    val sysShort = (game.system?.shortName ?: game.systemId).uppercase(Locale.US)
    val capTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    c.drawText(
        TextUtils.ellipsize(sysShort, capTp, w * 0.88f, TextUtils.TruncateAt.END).toString(),
        w * 0.5f,
        capH * 0.60f,
        capTp,
    )

    // Rotated vertical game title along the spine
    c.save()
    c.translate(w * 0.36f, capH + 28f)
    c.rotate(90f)
    val spineTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 36f
    }
    val maxTitlePx = h * 0.68f
    c.drawText(
        TextUtils.ellipsize(game.title, spineTp, maxTitlePx, TextUtils.TruncateAt.END).toString(),
        0f,
        0f,
        spineTp,
    )
    c.restore()

    // Bottom catalog code stamp
    val catTp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(185, 210, 215, 228)
        typeface = Typeface.MONOSPACE
        textSize = 16f
        textAlign = Paint.Align.CENTER
    }
    val pubShort = dossier.publisher.take(6).uppercase(Locale.US).ifBlank { "RETRO" }
    c.drawText(pubShort, w * 0.5f, h * 0.95f, catTp)
    return bmp
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

    val shellArgb = spec.mediaShellColor.toArgb()
    p.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        shadeArgb(shellArgb, 1.10f),
        shadeArgb(shellArgb, 0.84f),
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(RectF(6f, 6f, w - 6f, h - 6f), 26f, 26f, p)
    p.shader = null

    // Console-specific molded cartridge details (grip ribs, Game Boy top-right notch, NES left rib column)
    when (spec.mediaKind) {
        PhysicalMediaKind.CART_GAMEBOY -> {
            // Top horizontal grip grooves + oval header recess
            p.color = shadeArgb(shellArgb, 0.76f)
            for (i in 0..3) {
                val gy = h * 0.055f + i * (h * 0.022f)
                c.drawRoundRect(RectF(w * 0.08f, gy, w * 0.92f, gy + h * 0.010f), 4f, 4f, p)
            }
            c.drawRoundRect(RectF(w * 0.16f, h * 0.045f, w * 0.84f, h * 0.155f), 40f, 40f, p)
        }
        PhysicalMediaKind.CART_NES -> {
            // Iconic NES left vertical grip rib column
            p.color = shadeArgb(shellArgb, 0.78f)
            for (i in 0..16) {
                val gy = h * 0.08f + i * (h * 0.048f)
                c.drawRect(w * 0.07f, gy, w * 0.21f, gy + h * 0.022f, p)
            }
        }
        else -> {
            p.color = shadeArgb(shellArgb, 0.78f)
            c.drawRoundRect(RectF(w * 0.18f, h * 0.04f, w * 0.82f, h * 0.10f), 12f, 12f, p)
        }
    }

    // Recessed sticker well with the game's cover artwork + console header band
    val labelRect = when (spec.mediaKind) {
        PhysicalMediaKind.CART_NES -> RectF(w * 0.26f, h * 0.06f, w * 0.88f, h * 0.84f)
        PhysicalMediaKind.CART_GAMEBOY -> RectF(w * 0.13f, h * 0.20f, w * 0.87f, h * 0.85f)
        PhysicalMediaKind.CART_GBA -> RectF(w * 0.12f, h * 0.18f, w * 0.88f, h * 0.84f)
        PhysicalMediaKind.CART_DS_SWITCH -> RectF(w * 0.10f, h * 0.12f, w * 0.90f, h * 0.88f)
        else -> RectF(w * 0.14f, h * 0.14f, w * 0.86f, h * 0.82f)
    }

    p.color = shadeArgb(shellArgb, 0.68f)
    c.drawRoundRect(
        RectF(labelRect.left - 5f, labelRect.top - 5f, labelRect.right + 5f, labelRect.bottom + 5f),
        16f, 16f, p,
    )
    drawFramedBackScreenshot(c, coverBmp, labelRect, spec.spineHeaderColor.toArgb(), p, bmpPaint)

    // Bottom center insertion arrow triangle
    p.color = shadeArgb(shellArgb, 0.65f)
    val arrow = AndroidPath().apply {
        moveTo(w * 0.46f, h * 0.90f)
        lineTo(w * 0.54f, h * 0.90f)
        lineTo(w * 0.50f, h * 0.955f)
        close()
    }
    c.drawPath(arrow, p)
    return bmp
}

internal fun buildProceduralCartBackBitmap(game: Game, spec: PhysicalMediaSpec): Bitmap {
    val w = 620
    val h = (w / spec.mediaKind.defaultAspect).roundToInt().coerceIn(380, 740)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val shellArgb = spec.mediaShellColor.toArgb()

    p.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        shadeArgb(shellArgb, 1.05f),
        shadeArgb(shellArgb, 0.80f),
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(RectF(6f, 6f, w - 6f, h - 6f), 26f, 26f, p)
    p.shader = null

    if (spec.mediaKind == PhysicalMediaKind.CART_DS_SWITCH || spec.mediaKind == PhysicalMediaKind.CART_HUCARD) {
        // Gold contact fingers exposed along the bottom rear of DS / Switch / Vita / HuCard cards!
        val pinZoneTop = h * 0.64f
        p.color = AndroidColor.rgb(34, 78, 48)
        c.drawRoundRect(RectF(w * 0.14f, pinZoneTop, w * 0.86f, h * 0.92f), 8f, 8f, p)
        p.color = AndroidColor.rgb(228, 186, 68)
        val pins = 14
        val step = (w * 0.68f) / pins
        for (i in 0 until pins) {
            val px = w * 0.16f + i * step
            c.drawRoundRect(RectF(px, pinZoneTop + 6f, px + step * 0.68f, h * 0.90f), 4f, 4f, p)
        }
    } else {
        // Security screw wells + recessed rear caution sticker
        p.color = shadeArgb(shellArgb, 0.60f)
        c.drawCircle(w * 0.14f, h * 0.82f, 13f, p)
        c.drawCircle(w * 0.86f, h * 0.82f, 13f, p)
        p.color = AndroidColor.rgb(155, 135, 78) // brass security bit screw
        c.drawCircle(w * 0.14f, h * 0.82f, 7f, p)
        c.drawCircle(w * 0.86f, h * 0.82f, 7f, p)
    }

    // Recessed caution plate
    val warnRect = RectF(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.56f)
    p.color = shadeArgb(shellArgb, 0.72f)
    c.drawRoundRect(warnRect, 12f, 12f, p)
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(190, 235, 238, 246)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }
    c.drawText((game.system?.name ?: game.systemId).uppercase(Locale.US), w * 0.5f, warnRect.top + 42f, tp)
    tp.textSize = 14f
    tp.typeface = Typeface.MONOSPACE
    c.drawText("CAUTION · DO NOT TOUCH EDGE PINS", w * 0.5f, warnRect.top + 78f, tp)
    c.drawText("STORE IN PROTECTIVE CASE AFTER PLAY", w * 0.5f, warnRect.top + 104f, tp)
    return bmp
}

internal fun buildProceduralDiscLabelBitmap(
    game: Game,
    dossier: GameDossier,
    spec: PhysicalMediaSpec,
    coverBmp: Bitmap,
): Bitmap {
    val s = 600
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val discClip = AndroidPath().apply {
        addCircle(s * 0.5f, s * 0.5f, s * 0.495f, AndroidPath.Direction.CW)
    }
    c.save()
    c.clipPath(discClip)

    // Printed cover artwork on top half / silkscreened disc label
    val scale = max(s.toFloat() / coverBmp.width, s.toFloat() / coverBmp.height)
    val dw = coverBmp.width * scale
    val dh = coverBmp.height * scale
    c.drawBitmap(coverBmp, null, RectF((s - dw) * 0.5f, (s - dh) * 0.5f, (s + dw) * 0.5f, (s + dh) * 0.5f), bmpPaint)

    // Bottom silkscreen title band
    p.color = shadeArgb(spec.spineHeaderColor.toArgb(), 0.88f)
    p.alpha = 215
    c.drawRect(0f, s * 0.66f, s.toFloat(), s.toFloat(), p)
    p.alpha = 255

    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    c.drawText(
        TextUtils.ellipsize(game.title, tp, s * 0.72f, TextUtils.TruncateAt.END).toString(),
        s * 0.5f,
        s * 0.76f,
        tp,
    )
    tp.textSize = 17f
    c.drawText(
        "${(game.system?.shortName ?: game.systemId).uppercase(Locale.US)} · ${dossier.publisher.ifBlank { "OPTICAL DISC" }.uppercase(Locale.US)}",
        s * 0.5f,
        s * 0.83f,
        tp,
    )
    c.restore()

    // Outer silver rim + punch out center hole
    p.style = Paint.Style.STROKE
    p.strokeWidth = 8f
    p.color = AndroidColor.rgb(220, 226, 236)
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.49f, p)
    p.style = Paint.Style.FILL
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.082f, p)
    p.xfermode = null
    return bmp
}

internal fun buildProceduralDiscUndersideBitmap(spec: PhysicalMediaSpec): Bitmap {
    val s = 600
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    p.color = if (spec.blackDiscUnderside) {
        AndroidColor.rgb(16, 17, 22) // Authentic PS1 jet-black polycarbonate!
    } else {
        AndroidColor.rgb(196, 204, 218) // Mirror silver CD/DVD data layer
    }
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.495f, p)

    // Fine concentric data-track grooves + inner matrix barcode ring
    p.style = Paint.Style.STROKE
    p.strokeWidth = 2f
    p.color = if (spec.blackDiscUnderside) {
        AndroidColor.argb(55, 160, 175, 220)
    } else {
        AndroidColor.argb(70, 255, 255, 255)
    }
    for (r in listOf(0.19f, 0.28f, 0.37f, 0.45f)) {
        c.drawCircle(s * 0.5f, s * 0.5f, s * r, p)
    }
    // Inner silver mirror clamping ring
    p.strokeWidth = s * 0.038f
    p.color = AndroidColor.rgb(218, 224, 234)
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.165f, p)

    p.style = Paint.Style.FILL
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.082f, p)
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
            y in -122f..-62f -> "Case Opening Edge"
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
