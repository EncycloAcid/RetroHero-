package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.retrohero.app.data.Game
import com.retrohero.app.data.PhysicalMediaSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Physical loading mechanism for each console family. */
enum class ConsoleLoaderKind {
    /** Classic top-loading cartridge slot (SNES, N64, Genesis, Game Boy, GBA, DS, 3DS, Switch). */
    CARTRIDGE_TOP_SLOT,
    /** Iconic NES front-loading zero-insertion-force cartridge deck with flip door. */
    CARTRIDGE_FRONT_DECK,
    /** Classic hinged top-lid optical disc bay (PS1, Dreamcast, GameCube, Saturn, Sega CD). */
    DISC_TOP_LID,
    /** Motorized sliding disc tray / UMD carrier (PS2, Xbox, PSP). */
    DISC_SLIDE_TRAY,
    /** Glowing front slot-in optical drive (Wii, Wii U). */
    DISC_SLOT_IN,
}

/** Console-authentic hardware styling for the 3D media insertion ceremony. */
data class ConsoleHardwareSpec(
    val systemId: String,
    val consoleLabel: String,
    val loaderKind: ConsoleLoaderKind,
    val bodyTop: Color,
    val bodyMid: Color,
    val bodyDark: Color,
    val trimColor: Color,
    val accentColor: Color,
    val ledColor: Color,
    val insertVerb: String,
) {
    companion object {
        fun forSystem(systemId: String, mediaSpec: PhysicalMediaSpec): ConsoleHardwareSpec {
            val id = systemId.lowercase(Locale.US)
            return when (id) {
                "nes" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo Entertainment System",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_FRONT_DECK,
                    bodyTop = Color(0xFFD6D4CE),
                    bodyMid = Color(0xFFB5B3AD),
                    bodyDark = Color(0xFF424349),
                    trimColor = Color(0xFF1D1E22),
                    accentColor = Color(0xFFD62828),
                    ledColor = Color(0xFFFF2E3A),
                    insertVerb = "Sliding Game Pak into NES",
                )
                "snes" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Super Nintendo",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFFD9D8DC),
                    bodyMid = Color(0xFFB3B1BC),
                    bodyDark = Color(0xFF777487),
                    trimColor = Color(0xFF8C7AE6),
                    accentColor = Color(0xFF5D4EA8),
                    ledColor = Color(0xFFFF3647),
                    insertVerb = "Inserting SNES Game Pak",
                )
                "n64" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo 64",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF43464F),
                    bodyMid = Color(0xFF2B2E36),
                    bodyDark = Color(0xFF17191E),
                    trimColor = Color(0xFF585C68),
                    accentColor = Color(0xFFE53935),
                    ledColor = Color(0xFFFF3344),
                    insertVerb = "Inserting N64 Game Pak",
                )
                "gb" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Game Boy",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFFDAD7CC),
                    bodyMid = Color(0xFFC0BCB0),
                    bodyDark = Color(0xFF8B877C),
                    trimColor = Color(0xFF7B1F5B),
                    accentColor = Color(0xFF9C2766),
                    ledColor = Color(0xFFFF3038),
                    insertVerb = "Sliding Game Boy Pak",
                )
                "gbc" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Game Boy Color",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF6B4FC7),
                    bodyMid = Color(0xFF4F379E),
                    bodyDark = Color(0xFF33226B),
                    trimColor = Color(0xFF232530),
                    accentColor = Color(0xFF8F74F0),
                    ledColor = Color(0xFFFF3846),
                    insertVerb = "Inserting GBC Cartridge",
                )
                "gba" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Game Boy Advance",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF5664B8),
                    bodyMid = Color(0xFF3E4A94),
                    bodyDark = Color(0xFF283066),
                    trimColor = Color(0xFFC8CCD8),
                    accentColor = Color(0xFF7986CB),
                    ledColor = Color(0xFF38E56B),
                    insertVerb = "Inserting GBA Game Pak",
                )
                "nds" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo DS",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFFEAECEF),
                    bodyMid = Color(0xFFCFD4DC),
                    bodyDark = Color(0xFF939AA8),
                    trimColor = Color(0xFF3D434F),
                    accentColor = Color(0xFF5294FF),
                    ledColor = Color(0xFF2EE87A),
                    insertVerb = "Clicking DS Game Card",
                )
                "3ds" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo 3DS",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF275B8C),
                    bodyMid = Color(0xFF1A3F66),
                    bodyDark = Color(0xFF102844),
                    trimColor = Color(0xFF0E131B),
                    accentColor = Color(0xFF35B8FF),
                    ledColor = Color(0xFF2EE0FF),
                    insertVerb = "Clicking 3DS Game Card",
                )
                "switch" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo Switch",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF363A42),
                    bodyMid = Color(0xFF23262C),
                    bodyDark = Color(0xFF14161A),
                    trimColor = Color(0xFFE60012),
                    accentColor = Color(0xFF0AB9E6),
                    ledColor = Color(0xFF38E86C),
                    insertVerb = "Clicking Switch Game Card",
                )
                "genesis", "md", "megadrive", "sms", "gg", "32x" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Sega Genesis",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF33363E),
                    bodyMid = Color(0xFF1F2127),
                    bodyDark = Color(0xFF101216),
                    trimColor = Color(0xFF505562),
                    accentColor = Color(0xFFD62828),
                    ledColor = Color(0xFFFF3636),
                    insertVerb = "Inserting Sega Cartridge",
                )
                "psx", "ps1" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "PlayStation",
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFFC8CBD0),
                    bodyMid = Color(0xFFA7ABB2),
                    bodyDark = Color(0xFF7B7F88),
                    trimColor = Color(0xFF343840),
                    accentColor = Color(0xFF3D68D8),
                    ledColor = Color(0xFF34E86C),
                    insertVerb = "Loading PlayStation Disc",
                )
                "ps2" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "PlayStation 2",
                    loaderKind = ConsoleLoaderKind.DISC_SLIDE_TRAY,
                    bodyTop = Color(0xFF2B2E36),
                    bodyMid = Color(0xFF1A1C22),
                    bodyDark = Color(0xFF0E1014),
                    trimColor = Color(0xFF3B6EFF),
                    accentColor = Color(0xFF3B82F6),
                    ledColor = Color(0xFF34E86C),
                    insertVerb = "Loading PS2 Disc Tray",
                )
                "psp" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "PlayStation Portable",
                    loaderKind = ConsoleLoaderKind.DISC_SLIDE_TRAY,
                    bodyTop = Color(0xFF2C2E35),
                    bodyMid = Color(0xFF1A1C21),
                    bodyDark = Color(0xFF0D0E12),
                    trimColor = Color(0xFF9EA4B0),
                    accentColor = Color(0xFF8AA4FF),
                    ledColor = Color(0xFF36E86E),
                    insertVerb = "Closing PSP UMD Drive",
                )
                "dc", "dreamcast" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Sega Dreamcast",
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFFE7E8EC),
                    bodyMid = Color(0xFFCBCED6),
                    bodyDark = Color(0xFF9EA2AE),
                    trimColor = Color(0xFF8A909E),
                    accentColor = Color(0xFFFF6D28),
                    ledColor = Color(0xFFFF6D28),
                    insertVerb = "Loading Dreamcast GD-ROM",
                )
                "gc", "gamecube" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo GameCube",
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFF6B5CC2),
                    bodyMid = Color(0xFF4D3F9E),
                    bodyDark = Color(0xFF332870),
                    trimColor = Color(0xFF24242C),
                    accentColor = Color(0xFF8B7CE8),
                    ledColor = Color(0xFFFF8C24),
                    insertVerb = "Loading GameCube Disc",
                )
                "wii", "wiiu" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = if (id == "wiiu") "Nintendo Wii U" else "Nintendo Wii",
                    loaderKind = ConsoleLoaderKind.DISC_SLOT_IN,
                    bodyTop = Color(0xFFECEFF4),
                    bodyMid = Color(0xFFD5DAE2),
                    bodyDark = Color(0xFF9DA5B2),
                    trimColor = Color(0xFF282D36),
                    accentColor = Color(0xFF26B4FF),
                    ledColor = Color(0xFF26C6FF),
                    insertVerb = "Feeding Optical Disc",
                )
                "saturn", "segacd", "3do", "neogeocd" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = when (id) {
                        "saturn" -> "Sega Saturn"
                        "segacd" -> "Sega CD"
                        "3do" -> "3DO Interactive"
                        else -> "Neo Geo CD"
                    },
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFF363A44),
                    bodyMid = Color(0xFF22252D),
                    bodyDark = Color(0xFF13151B),
                    trimColor = Color(0xFF565C6B),
                    accentColor = Color(0xFF3B82F6),
                    ledColor = Color(0xFF34E86C),
                    insertVerb = "Loading Optical Disc",
                )
                else -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = systemId.uppercase(Locale.US),
                    loaderKind = if (mediaSpec.mediaKind.isOpticalDisc) {
                        ConsoleLoaderKind.DISC_TOP_LID
                    } else {
                        ConsoleLoaderKind.CARTRIDGE_TOP_SLOT
                    },
                    bodyTop = Color(0xFF3A3D47),
                    bodyMid = Color(0xFF242730),
                    bodyDark = Color(0xFF14161C),
                    trimColor = Color(0xFF545866),
                    accentColor = mediaSpec.spineHeaderColor,
                    ledColor = Color(0xFF38E86C),
                    insertVerb = if (mediaSpec.mediaKind.isOpticalDisc) "Loading Disc" else "Inserting Cartridge",
                )
            }
        }
    }
}

/** Deterministic frame state computed from normalized timeline progress `0f..1f`. */
data class ConsoleInsertionFrame(
    /** Normalized vertical progress (`0f` hovering above console, `1f` fully seated inside slot/spindle). */
    val insertDepth: Float,
    /** Media 3D yaw in degrees. */
    val mediaYawDeg: Float,
    /** Media 3D pitch in degrees (tilts flat for optical disc trays/lids). */
    val mediaPitchDeg: Float,
    /** Continuous optical disc spin rotation in degrees once seated. */
    val discSpinDeg: Float,
    /** Console lid/flap open fraction (`1f` fully open, `0f` shut). */
    val lidOpen: Float,
    /** Tray extension fraction for sliding trays (`1f` extended out, `0f` retracted). */
    val trayOut: Float,
    /** Subtle downward console bounce in dp when cartridge/disc clicks into place. */
    val consoleBounceDp: Float,
    /** Console power LED glow intensity (`0f` off, `1f` bright powered on). */
    val ledGlow: Float,
    /** Brief CRT/LCD power-on white bloom at the end of the sequence (`0f..1f`). */
    val bootFlash: Float,
    /** True at the moment media physically seats in the slot/spindle (for haptic click). */
    val seated: Boolean,
    /** Human-readable status caption for the current phase. */
    val statusText: String,
)

object ConsoleInsertionMath {
    private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
    private fun smoothStep(t: Float): Float {
        val x = clamp01(t)
        return x * x * (3f - 2f * x)
    }
    private fun easeOutCubic(t: Float): Float {
        val x = clamp01(t)
        val inv = 1f - x
        return 1f - inv * inv * inv
    }
    private fun easeInOutCubic(t: Float): Float {
        val x = clamp01(t)
        return if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f) * (-2f * x + 2f) * (-2f * x + 2f) / 2f
    }

    fun evaluate(progress: Float, spec: ConsoleHardwareSpec): ConsoleInsertionFrame {
        val p = clamp01(progress)
        return when (spec.loaderKind) {
            ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
            ConsoleLoaderKind.CARTRIDGE_FRONT_DECK -> {
                val alignT = easeOutCubic(p / 0.28f)
                val plungeT = easeInOutCubic((p - 0.26f) / 0.44f)
                val seated = p >= 0.68f
                val bouncePhase = clamp01((p - 0.66f) / 0.16f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) {
                    sin(bouncePhase * PI).toFloat() * 5.5f
                } else 0f
                val ledGlow = smoothStep((p - 0.72f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val flapOpen = if (p < 0.18f) smoothStep(p / 0.18f) else 1f - smoothStep((p - 0.72f) / 0.16f) * 0.45f
                val status = when {
                    p < 0.30f -> spec.insertVerb + "…"
                    p < 0.72f -> "Seating into ${spec.consoleLabel}…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = plungeT,
                    mediaYawDeg = 22f * (1f - alignT),
                    mediaPitchDeg = -6f + 16f * alignT,
                    discSpinDeg = 0f,
                    lidOpen = flapOpen,
                    trayOut = 0f,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }
            ConsoleLoaderKind.DISC_TOP_LID -> {
                val dropT = easeInOutCubic((p - 0.10f) / 0.38f)
                val seated = p >= 0.48f
                val lidCloseT = smoothStep((p - 0.54f) / 0.24f)
                val spinT = clamp01((p - 0.46f) / 0.54f)
                val spinDeg = spinT * spinT * 980f
                val bouncePhase = clamp01((p - 0.46f) / 0.14f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) {
                    sin(bouncePhase * PI).toFloat() * 4.2f
                } else 0f
                val ledGlow = smoothStep((p - 0.72f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.48f -> spec.insertVerb + "…"
                    p < 0.76f -> "Closing lid & spinning up…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = dropT,
                    mediaYawDeg = 18f * (1f - easeOutCubic(p / 0.25f)),
                    mediaPitchDeg = -8f + 56f * easeOutCubic(p / 0.36f),
                    discSpinDeg = spinDeg,
                    lidOpen = 1f - lidCloseT,
                    trayOut = 0f,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }
            ConsoleLoaderKind.DISC_SLIDE_TRAY -> {
                val dropT = easeInOutCubic((p - 0.08f) / 0.34f)
                val seated = p >= 0.42f
                val retractT = easeInOutCubic((p - 0.46f) / 0.30f)
                val spinT = clamp01((p - 0.42f) / 0.58f)
                val ledGlow = smoothStep((p - 0.72f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.44f -> spec.insertVerb + "…"
                    p < 0.76f -> "Retracting disc drive…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = dropT,
                    mediaYawDeg = 14f * (1f - easeOutCubic(p / 0.24f)),
                    mediaPitchDeg = -6f + 52f * easeOutCubic(p / 0.32f),
                    discSpinDeg = spinT * spinT * 840f,
                    lidOpen = 0f,
                    trayOut = 1f - retractT,
                    consoleBounceDp = 0f,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }
            ConsoleLoaderKind.DISC_SLOT_IN -> {
                val alignT = easeOutCubic(p / 0.24f)
                val feedT = easeInOutCubic((p - 0.22f) / 0.48f)
                val seated = p >= 0.68f
                val ledGlow = smoothStep((p - 0.20f) / 0.55f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.68f -> spec.insertVerb + "…"
                    else -> "Reading disc · ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = feedT,
                    mediaYawDeg = 16f * (1f - alignT),
                    mediaPitchDeg = -4f + 22f * alignT,
                    discSpinDeg = feedT * 260f,
                    lidOpen = 1f,
                    trayOut = 0f,
                    consoleBounceDp = 0f,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }
        }
    }
}

/**
 * Tactile fullscreen Console Media Insertion Ceremony triggered when pressing Play from a 3D model!
 * Animates the game's actual 3D Cartridge / Game Card / Optical Disc inserting into the matching
 * console hardware, clicks with haptics, lights the console power LED, and launches the emulator.
 */
@Composable
internal fun M3ConsoleInsertionDialog(
    game: Game,
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    onLaunchNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val reducedMotion = LocalReducedMotion.current
    val hwSpec = remember(game.systemId, spec) { ConsoleHardwareSpec.forSystem(game.systemId, spec) }
    val progress = remember { Animatable(0f) }
    var launched by remember { mutableStateOf(false) }
    var hapticFired by remember { mutableStateOf(false) }

    fun finishAndLaunch() {
        if (launched) return
        launched = true
        onDismiss()
        onLaunchNow()
    }

    LaunchedEffect(game.id, reducedMotion) {
        if (reducedMotion) {
            finishAndLaunch()
            return@LaunchedEffect
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1280, easing = LinearEasing),
        )
        finishAndLaunch()
    }

    val frame = ConsoleInsertionMath.evaluate(progress.value, hwSpec)

    LaunchedEffect(frame.seated) {
        if (frame.seated && !hapticFired) {
            hapticFired = true
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    Dialog(
        onDismissRequest = { finishAndLaunch() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ConsoleInsertionStageContent(
            game = game,
            spec = spec,
            hwSpec = hwSpec,
            textures = textures,
            frame = frame,
            onSkip = { finishAndLaunch() },
        )
    }
}

/** Pure stateless renderer for the Console Media Insertion stage (shared by dialog & screenshot tests). */
@Composable
internal fun ConsoleInsertionStageContent(
    game: Game,
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val isDisc = spec.mediaKind.isOpticalDisc

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to hwSpec.accentColor.copy(alpha = 0.22f + frame.ledGlow * 0.14f),
                    0.55f to Color(0xF511121A),
                    1.0f to Color(0xFA06070B),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkip,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Top badge showing the hardware + game title
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = if (isDisc) Icons.Rounded.Album else Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = if (frame.ledGlow > 0.2f) hwSpec.ledColor else hwSpec.accentColor,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = hwSpec.consoleLabel.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                )
            }

            // Main 3D Media + Console Insertion Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .offset(y = frame.consoleBounceDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isDisc) {
                    DiscConsoleInsertionStage(
                        spec = spec,
                        hwSpec = hwSpec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CartridgeConsoleInsertionStage(
                        spec = spec,
                        hwSpec = hwSpec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Title & live phase status readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (frame.ledGlow > 0.1f) {
                                    hwSpec.ledColor.copy(alpha = (0.35f + frame.ledGlow * 0.65f).coerceIn(0f, 1f))
                                } else {
                                    Color.White.copy(alpha = 0.28f)
                                },
                            ),
                    )
                    Text(
                        text = frame.statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.84f),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap anywhere to skip",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.42f),
                )
            }
        }

        // Crisp CRT/LCD boot flash right as the console powers on
        if (frame.bootFlash > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(frame.bootFlash * 0.68f)
                    .background(
                        Brush.radialGradient(
                            0.0f to Color.White,
                            0.65f to cs.primaryContainer.copy(alpha = 0.65f),
                            1.0f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun CartridgeConsoleInsertionStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val slotLipYDp = 216.dp
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        // Layer 1: Console back chassis + open dark cartridge slot interior
        Canvas(Modifier.fillMaxSize()) {
            drawCartridgeConsoleBack(hwSpec, frame)
        }

        // Layer 2: The actual 3D Cartridge clipped cleanly at the console's front slot lip!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(slotLipYDp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val startY = 8f
            val endY = 128f
            val yDp = startY + (endY - startY) * frame.insertDepth
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = spec.mediaKind.defaultAspect,
                yawDeg = frame.mediaYawDeg,
                pitchDeg = frame.mediaPitchDeg,
                floorShadow = false,
                modifier = Modifier
                    .width(210.dp)
                    .height(186.dp)
                    .offset(y = yDp.dp),
            )
        }

        // Layer 3: Console front bezel overlapping the lower half of the cartridge + glowing Power LED!
        Canvas(Modifier.fillMaxSize()) {
            drawCartridgeConsoleFrontLip(hwSpec, frame)
        }
    }
}

@Composable
private fun DiscConsoleInsertionStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val isSlotIn = hwSpec.loaderKind == ConsoleLoaderKind.DISC_SLOT_IN
    val isSlideTray = hwSpec.loaderKind == ConsoleLoaderKind.DISC_SLIDE_TRAY

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        // Layer 1: Console disc bay / sliding tray / open spindle hub
        Canvas(Modifier.fillMaxSize()) {
            drawDiscConsoleBase(hwSpec, frame)
        }

        // Layer 2: The actual 3D Optical Disc dropping onto the spindle or feeding into the slot!
        val clipHeight = if (isSlotIn) 210.dp else 340.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(clipHeight)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val discSizeDp = 210f
            // Bay center is at 340 * 0.67 = 227.8dp -> top offset to center a 210dp disc = 122.8dp!
            val startY = if (isSlotIn) 8f else 6f
            val endY = when {
                isSlotIn -> 178f
                isSlideTray -> 123f - (1f - frame.trayOut) * 28f
                else -> 123f
            }
            val yDp = startY + (endY - startY) * frame.insertDepth
            val discScaleX = when {
                isSlotIn -> 0.86f
                else -> 0.96f - frame.insertDepth * 0.04f
            }
            // Foreshorten vertically in the OUTER layer so the inner disc spins cleanly inside a flat horizontal oval!
            val foreshortenY = when {
                isSlotIn -> 1f
                else -> (0.84f - frame.insertDepth * 0.31f).coerceIn(0.51f, 0.95f)
            }
            val discAlpha = when {
                isSlideTray -> (0.25f + frame.trayOut * 0.75f).coerceIn(0f, 1f)
                else -> 1f
            }
            Box(
                modifier = Modifier
                    .size(discSizeDp.dp)
                    .offset(y = yDp.dp)
                    .graphicsLayer {
                        scaleX = discScaleX
                        scaleY = discScaleX * foreshortenY
                        alpha = discAlpha
                    },
                contentAlignment = Alignment.Center,
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = 1.0f,
                    yawDeg = if (isSlotIn) frame.mediaYawDeg else 0f,
                    pitchDeg = if (isSlotIn) frame.mediaPitchDeg else 0f,
                    floorShadow = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = frame.discSpinDeg
                        },
                )
            }
        }

        // Layer 3: Closing top lid / front slot-in chassis lip + glowing Power LED!
        Canvas(Modifier.fillMaxSize()) {
            drawDiscConsoleForeground(hwSpec, frame)
        }
    }
}

private fun DrawScope.drawCartridgeConsoleBack(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val deckW = (w * 0.84f).coerceAtMost(320.dp.toPx())
    val deckL = (w - deckW) * 0.5f
    val deckR = deckL + deckW
    val topY = h * 0.53f
    val slotY = h * 0.635f
    val bottomY = h * 0.91f

    // Grounded contact shadow beneath the console
    drawOval(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.65f),
            1f to Color.Transparent,
            center = Offset(w * 0.5f, bottomY + 6.dp.toPx()),
            radius = deckW * 0.60f,
        ),
        topLeft = Offset(deckL - 14.dp.toPx(), bottomY - 10.dp.toPx()),
        size = Size(deckW + 28.dp.toPx(), 32.dp.toPx()),
    )

    // Upper sculpted perspective top deck of the console
    val topDeckPath = Path().apply {
        moveTo(deckL + 18.dp.toPx(), topY)
        lineTo(deckR - 18.dp.toPx(), topY)
        lineTo(deckR, slotY + 12.dp.toPx())
        lineTo(deckL, slotY + 12.dp.toPx())
        close()
    }
    drawPath(
        path = topDeckPath,
        brush = Brush.verticalGradient(
            0f to hwSpec.bodyTop,
            1f to hwSpec.bodyMid,
            startY = topY,
            endY = slotY + 14.dp.toPx(),
        ),
    )

    // Deep dark recessed cartridge slot mouth
    val slotW = deckW * 0.68f
    val slotL = (w - slotW) * 0.5f
    val slotH = 22.dp.toPx()
    drawRoundRect(
        color = Color(0xFF0A0B0E),
        topLeft = Offset(slotL, slotY - slotH * 0.62f),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    // Inner dust-flap highlight
    val flapH = (slotH * 0.36f * (1f - frame.lidOpen * 0.65f)).coerceAtLeast(2.dp.toPx())
    drawRoundRect(
        color = hwSpec.bodyDark.copy(alpha = 0.85f),
        topLeft = Offset(slotL + 4.dp.toPx(), slotY - slotH * 0.56f),
        size = Size(slotW - 8.dp.toPx(), flapH),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
}

private fun DrawScope.drawCartridgeConsoleFrontLip(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val deckW = (w * 0.84f).coerceAtMost(320.dp.toPx())
    val deckL = (w - deckW) * 0.5f
    val slotLipY = h * 0.635f
    val frontH = h * 0.275f

    // Front console fascia that masks the bottom of the cartridge as it plunges into the slot!
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to hwSpec.bodyMid,
            0.42f to hwSpec.bodyDark,
            1f to Color(0xFF101218),
            startY = slotLipY,
            endY = slotLipY + frontH,
        ),
        topLeft = Offset(deckL, slotLipY),
        size = Size(deckW, frontH),
        cornerRadius = CornerRadius(18.dp.toPx()),
    )

    // Crisp catch-light bevel along the front slot lip
    drawLine(
        color = Color.White.copy(alpha = 0.34f),
        start = Offset(deckL + 12.dp.toPx(), slotLipY + 1.2.dp.toPx()),
        end = Offset(deckL + deckW - 12.dp.toPx(), slotLipY + 1.2.dp.toPx()),
        strokeWidth = 1.6.dp.toPx(),
    )

    // Console signature trim stripe (NES black stripe, SNES purple switches, Switch red/blue accent)
    drawRoundRect(
        color = hwSpec.trimColor.copy(alpha = 0.82f),
        topLeft = Offset(deckL + 18.dp.toPx(), slotLipY + frontH * 0.28f),
        size = Size(deckW - 36.dp.toPx(), 6.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )

    // Power & Reset tactile buttons on the console fascia
    val btnY = slotLipY + frontH * 0.58f
    drawRoundRect(
        color = hwSpec.accentColor.copy(alpha = 0.88f),
        topLeft = Offset(deckL + 22.dp.toPx(), btnY),
        size = Size(34.dp.toPx(), 13.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx()),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(deckL + 64.dp.toPx(), btnY),
        size = Size(28.dp.toPx(), 13.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx()),
    )

    // Glowing Power LED on the right side of the console front panel!
    val ledCenter = Offset(deckL + deckW - 28.dp.toPx(), btnY + 6.5.dp.toPx())
    if (frame.ledGlow > 0.02f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to hwSpec.ledColor.copy(alpha = 0.85f * frame.ledGlow),
                0.5f to hwSpec.ledColor.copy(alpha = 0.28f * frame.ledGlow),
                1f to Color.Transparent,
                center = ledCenter,
                radius = 24.dp.toPx(),
            ),
            radius = 24.dp.toPx(),
            center = ledCenter,
        )
    }
    drawCircle(
        color = if (frame.ledGlow > 0.05f) {
            hwSpec.ledColor.copy(alpha = (0.35f + frame.ledGlow * 0.65f).coerceIn(0f, 1f))
        } else {
            Color.Black.copy(alpha = 0.55f)
        },
        radius = 4.5.dp.toPx(),
        center = ledCenter,
    )
}

private fun DrawScope.drawDiscConsoleBase(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.84f).coerceAtMost(310.dp.toPx())
    val deckL = cx - deckW * 0.5f

    if (hwSpec.loaderKind == ConsoleLoaderKind.DISC_SLOT_IN) {
        // Sleek glossy Wii / Wii U front drive bezel with glowing slot mouth
        val bodyTop = h * 0.56f
        val bodyH = h * 0.34f
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to hwSpec.bodyTop,
                0.55f to hwSpec.bodyMid,
                1f to hwSpec.bodyDark,
                startY = bodyTop,
                endY = bodyTop + bodyH,
            ),
            topLeft = Offset(deckL, bodyTop),
            size = Size(deckW, bodyH),
            cornerRadius = CornerRadius(18.dp.toPx()),
        )
        // Glowing electric-blue optical disc slot
        val slotW = deckW * 0.72f
        val slotL = cx - slotW * 0.5f
        val slotY = h * 0.61f
        if (frame.ledGlow > 0.05f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.2f to hwSpec.ledColor.copy(alpha = 0.65f * frame.ledGlow),
                    0.5f to hwSpec.ledColor.copy(alpha = 0.95f * frame.ledGlow),
                    0.8f to hwSpec.ledColor.copy(alpha = 0.65f * frame.ledGlow),
                    1f to Color.Transparent,
                    startX = slotL - 12.dp.toPx(),
                    endX = slotL + slotW + 12.dp.toPx(),
                ),
                topLeft = Offset(slotL - 8.dp.toPx(), slotY - 8.dp.toPx()),
                size = Size(slotW + 16.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx()),
            )
        }
        drawRoundRect(
            color = Color(0xFF0A0C10),
            topLeft = Offset(slotL, slotY),
            size = Size(slotW, 9.dp.toPx()),
            cornerRadius = CornerRadius(4.5.dp.toPx()),
        )
        return
    }

    // Classic Top-Lid (PS1, Dreamcast, GameCube, Saturn) or Sliding Tray (PS2, PSP)
    val bayCy = h * 0.67f
    val bodyTop = h * 0.48f
    val bodyH = h * 0.42f

    // Grounded contact shadow
    drawOval(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.62f),
            1f to Color.Transparent,
            center = Offset(cx, bodyTop + bodyH + 6.dp.toPx()),
            radius = deckW * 0.58f,
        ),
        topLeft = Offset(deckL - 10.dp.toPx(), bodyTop + bodyH - 8.dp.toPx()),
        size = Size(deckW + 20.dp.toPx(), 28.dp.toPx()),
    )

    // Console top deck body
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to hwSpec.bodyTop,
            0.65f to hwSpec.bodyMid,
            1f to hwSpec.bodyDark,
            startY = bodyTop,
            endY = bodyTop + bodyH,
        ),
        topLeft = Offset(deckL, bodyTop),
        size = Size(deckW, bodyH),
        cornerRadius = CornerRadius(22.dp.toPx()),
    )

    // Recessed circular disc bay + center spindle hub (in 3D oval perspective!)
    val trayShiftY = if (hwSpec.loaderKind == ConsoleLoaderKind.DISC_SLIDE_TRAY) {
        (frame.trayOut - 1f) * 28.dp.toPx()
    } else 0f
    val ovalRx = deckW * 0.34f
    val ovalRy = ovalRx * 0.52f
    val wellCenter = Offset(cx, bayCy + trayShiftY)
    drawOval(
        color = Color(0xFF14161D),
        topLeft = Offset(wellCenter.x - ovalRx, wellCenter.y - ovalRy),
        size = Size(ovalRx * 2f, ovalRy * 2f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = Offset(wellCenter.x - ovalRx, wellCenter.y - ovalRy),
        size = Size(ovalRx * 2f, ovalRy * 2f),
        style = Stroke(width = 1.6.dp.toPx()),
    )
    // Center spindle hub + laser sled glint
    drawOval(
        color = Color(0xFF3E434E),
        topLeft = Offset(wellCenter.x - 14.dp.toPx(), wellCenter.y - 8.dp.toPx()),
        size = Size(28.dp.toPx(), 16.dp.toPx()),
    )
    drawOval(
        color = Color(0xFF9EA6B6),
        topLeft = Offset(wellCenter.x - 6.dp.toPx(), wellCenter.y - 3.5.dp.toPx()),
        size = Size(12.dp.toPx(), 7.dp.toPx()),
    )
}

private fun DrawScope.drawDiscConsoleForeground(hwSpec: ConsoleHardwareSpec, frame: ConsoleInsertionFrame) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val deckW = (w * 0.84f).coerceAtMost(310.dp.toPx())
    val deckL = cx - deckW * 0.5f

    if (hwSpec.loaderKind == ConsoleLoaderKind.DISC_SLOT_IN) {
        // Lower lip masking the disc once it slides inside the glowing slot
        val lipY = h * 0.625f
        val lipH = h * 0.28f
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to hwSpec.bodyMid,
                1f to hwSpec.bodyDark,
                startY = lipY,
                endY = lipY + lipH,
            ),
            topLeft = Offset(deckL, lipY),
            size = Size(deckW, lipH),
            cornerRadius = CornerRadius(18.dp.toPx()),
        )
        drawLine(
            color = hwSpec.ledColor.copy(alpha = 0.35f + frame.ledGlow * 0.60f),
            start = Offset(cx - deckW * 0.34f, lipY + 1.dp.toPx()),
            end = Offset(cx + deckW * 0.34f, lipY + 1.dp.toPx()),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        return
    }

    val bayCy = h * 0.67f
    val ovalRx = deckW * 0.35f
    val ovalRy = ovalRx * 0.53f

    // Closing hinged circular console lid sweeping down over the spinning disc!
    if (hwSpec.loaderKind == ConsoleLoaderKind.DISC_TOP_LID && frame.lidOpen < 0.98f) {
        val closedFraction = (1f - frame.lidOpen).coerceIn(0f, 1f)
        val lidTop = bayCy - ovalRy - (1f - closedFraction) * 86.dp.toPx()
        val lidH = ovalRy * 2f * (0.35f + 0.65f * closedFraction)
        drawOval(
            brush = Brush.verticalGradient(
                0f to hwSpec.bodyTop.copy(alpha = 0.25f + closedFraction * 0.73f),
                1f to hwSpec.bodyMid.copy(alpha = 0.25f + closedFraction * 0.73f),
                startY = lidTop,
                endY = lidTop + lidH,
            ),
            topLeft = Offset(cx - ovalRx, lidTop),
            size = Size(ovalRx * 2f, lidH),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.32f * closedFraction),
            topLeft = Offset(cx - ovalRx, lidTop),
            size = Size(ovalRx * 2f, lidH),
            style = Stroke(width = 1.6.dp.toPx()),
        )
        // Center lid emblem badge ring
        if (closedFraction > 0.45f) {
            drawOval(
                color = hwSpec.accentColor.copy(alpha = 0.45f * closedFraction),
                topLeft = Offset(cx - 22.dp.toPx(), lidTop + lidH * 0.5f - 11.dp.toPx()),
                size = Size(44.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }

    // Power LED in the front-right corner of the disc console
    val ledCenter = Offset(deckL + deckW - 26.dp.toPx(), h * 0.82f)
    if (frame.ledGlow > 0.02f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to hwSpec.ledColor.copy(alpha = 0.85f * frame.ledGlow),
                0.55f to hwSpec.ledColor.copy(alpha = 0.28f * frame.ledGlow),
                1f to Color.Transparent,
                center = ledCenter,
                radius = 22.dp.toPx(),
            ),
            radius = 22.dp.toPx(),
            center = ledCenter,
        )
    }
    drawCircle(
        color = if (frame.ledGlow > 0.05f) {
            hwSpec.ledColor.copy(alpha = (0.35f + frame.ledGlow * 0.65f).coerceIn(0f, 1f))
        } else {
            Color.Black.copy(alpha = 0.5f)
        },
        radius = 4.5.dp.toPx(),
        center = ledCenter,
    )
}
