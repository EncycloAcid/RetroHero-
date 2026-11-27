package com.retrohero.app.ui.components

import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Memory
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.retrohero.app.R
import com.retrohero.app.data.Game
import com.retrohero.app.data.PhysicalMediaSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Authentic physical loading mechanism for each console family. */
enum class ConsoleLoaderKind {
    /** Classic top-loading home console cartridge slot (SNES, N64, Genesis). */
    CARTRIDGE_TOP_SLOT,
    /** Iconic NES front-loading flip-door + horizontal slide-in + ZIF press-down deck. */
    CARTRIDGE_FRONT_DECK,
    /** Real handheld top-rear Slot-1 / Game Pak bay (NDS, 3DS, GB, GBC, GBA). */
    HANDHELD_REAR_SLOT,
    /** Nintendo Switch top-right hinged Game Card flap with Neon Blue/Red Joy-Cons. */
    SWITCH_TOP_FLAP,
    /** Classic hinged top-lid optical disc console (PS1, Dreamcast, GameCube, Saturn, Sega CD). */
    DISC_TOP_LID,
    /** PlayStation 2 motorized extending front disc tray. */
    DISC_SLIDE_TRAY,
    /** PlayStation Portable rear hinged UMD clamshell hatch with silver chrome ring. */
    DISC_REAR_UMD,
    /** Upright Nintendo Wii / Wii U vertical glowing electric-blue slot-in optical drive. */
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
                    bodyTop = Color(0xFFDFDDD6),
                    bodyMid = Color(0xFFBDBBB3),
                    bodyDark = Color(0xFF424349),
                    trimColor = Color(0xFF1A1B1F),
                    accentColor = Color(0xFFD62828),
                    ledColor = Color(0xFFFF2430),
                    insertVerb = "Sliding Game Pak into NES",
                )
                "snes" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Super Nintendo",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFFE2E0E5),
                    bodyMid = Color(0xFFB3AFBE),
                    bodyDark = Color(0xFF6B6878),
                    trimColor = Color(0xFF8C7AE6),
                    accentColor = Color(0xFF6D5AC2),
                    ledColor = Color(0xFFFF2A3A),
                    insertVerb = "Inserting SNES Game Pak",
                )
                "n64" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo 64",
                    loaderKind = ConsoleLoaderKind.CARTRIDGE_TOP_SLOT,
                    bodyTop = Color(0xFF474B55),
                    bodyMid = Color(0xFF282B33),
                    bodyDark = Color(0xFF14161B),
                    trimColor = Color(0xFF545864),
                    accentColor = Color(0xFFE53935),
                    ledColor = Color(0xFFFF2436),
                    insertVerb = "Inserting N64 Game Pak",
                )
                "gb" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Game Boy",
                    loaderKind = ConsoleLoaderKind.HANDHELD_REAR_SLOT,
                    bodyTop = Color(0xFFDDDAD0),
                    bodyMid = Color(0xFFC2BEB2),
                    bodyDark = Color(0xFF8F8B80),
                    trimColor = Color(0xFF7B1F5B),
                    accentColor = Color(0xFF9C2766),
                    ledColor = Color(0xFFFF3038),
                    insertVerb = "Sliding Game Boy Pak",
                )
                "gbc" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Game Boy Color",
                    loaderKind = ConsoleLoaderKind.HANDHELD_REAR_SLOT,
                    bodyTop = Color(0xFF7456D4),
                    bodyMid = Color(0xFF5339A8),
                    bodyDark = Color(0xFF342270),
                    trimColor = Color(0xFF232530),
                    accentColor = Color(0xFF8F74F0),
                    ledColor = Color(0xFFFF3846),
                    insertVerb = "Inserting GBC Cartridge",
                )
                "gba" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Game Boy Advance",
                    loaderKind = ConsoleLoaderKind.HANDHELD_REAR_SLOT,
                    bodyTop = Color(0xFF5D6BC4),
                    bodyMid = Color(0xFF414E9E),
                    bodyDark = Color(0xFF2A326B),
                    trimColor = Color(0xFFD0D4E0),
                    accentColor = Color(0xFF7986CB),
                    ledColor = Color(0xFF38E56B),
                    insertVerb = "Inserting GBA Game Pak",
                )
                "nds" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo DS Lite",
                    loaderKind = ConsoleLoaderKind.HANDHELD_REAR_SLOT,
                    bodyTop = Color(0xFFF0F2F6),
                    bodyMid = Color(0xFFD6DBE4),
                    bodyDark = Color(0xFF9EA5B4),
                    trimColor = Color(0xFF3D434F),
                    accentColor = Color(0xFF5294FF),
                    ledColor = Color(0xFF2EE87A),
                    insertVerb = "Clicking DS Card into Slot-1",
                )
                "3ds" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo 3DS",
                    loaderKind = ConsoleLoaderKind.HANDHELD_REAR_SLOT,
                    bodyTop = Color(0xFF2D6CA3),
                    bodyMid = Color(0xFF1D4975),
                    bodyDark = Color(0xFF102A48),
                    trimColor = Color(0xFF0E131B),
                    accentColor = Color(0xFF35B8FF),
                    ledColor = Color(0xFF2EE0FF),
                    insertVerb = "Clicking 3DS Card into Slot-1",
                )
                "switch" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo Switch",
                    loaderKind = ConsoleLoaderKind.SWITCH_TOP_FLAP,
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
                    bodyTop = Color(0xFF2F323B),
                    bodyMid = Color(0xFF1B1D23),
                    bodyDark = Color(0xFF0E1014),
                    trimColor = Color(0xFF505562),
                    accentColor = Color(0xFFD62828),
                    ledColor = Color(0xFFFF2424),
                    insertVerb = "Inserting Sega Cartridge",
                )
                "psx", "ps1" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "PlayStation",
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFFD0D3D8),
                    bodyMid = Color(0xFFB1B5BC),
                    bodyDark = Color(0xFF71757E),
                    trimColor = Color(0xFF343840),
                    accentColor = Color(0xFF3D68D8),
                    ledColor = Color(0xFF30F06E),
                    insertVerb = "Placing PlayStation Disc",
                )
                "ps2" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "PlayStation 2",
                    loaderKind = ConsoleLoaderKind.DISC_SLIDE_TRAY,
                    bodyTop = Color(0xFF2B2E36),
                    bodyMid = Color(0xFF1A1C22),
                    bodyDark = Color(0xFF0C0E12),
                    trimColor = Color(0xFF2A66FF),
                    accentColor = Color(0xFF3B82F6),
                    ledColor = Color(0xFF2EE86C),
                    insertVerb = "Loading PS2 Disc Tray",
                )
                "psp" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "PlayStation Portable",
                    loaderKind = ConsoleLoaderKind.DISC_REAR_UMD,
                    bodyTop = Color(0xFF2E313A),
                    bodyMid = Color(0xFF17191F),
                    bodyDark = Color(0xFF0C0D12),
                    trimColor = Color(0xFFB8C0D0),
                    accentColor = Color(0xFF8AA4FF),
                    ledColor = Color(0xFF34EB6E),
                    insertVerb = "Sliding UMD into Rear Hatch",
                )
                "dc", "dreamcast" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Sega Dreamcast",
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFFF0F1F4),
                    bodyMid = Color(0xFFD5D8E0),
                    bodyDark = Color(0xFFA7ACB8),
                    trimColor = Color(0xFF8A909E),
                    accentColor = Color(0xFFFF6D28),
                    ledColor = Color(0xFFFF6D1F),
                    insertVerb = "Placing Dreamcast GD-ROM",
                )
                "gc", "gamecube" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = "Nintendo GameCube",
                    loaderKind = ConsoleLoaderKind.DISC_TOP_LID,
                    bodyTop = Color(0xFF6B5AC4),
                    bodyMid = Color(0xFF4E3EA6),
                    bodyDark = Color(0xFF332678),
                    trimColor = Color(0xFFDBDCE2),
                    accentColor = Color(0xFF8B7CE8),
                    ledColor = Color(0xFFFF8C1A),
                    insertVerb = "Placing GameCube Disc",
                )
                "wii", "wiiu" -> ConsoleHardwareSpec(
                    systemId = id,
                    consoleLabel = if (id == "wiiu") "Nintendo Wii U" else "Nintendo Wii",
                    loaderKind = ConsoleLoaderKind.DISC_SLOT_IN,
                    bodyTop = Color(0xFFF6F8FC),
                    bodyMid = Color(0xFFE3E7F0),
                    bodyDark = Color(0xFFC5CCD8),
                    trimColor = Color(0xFF282D36),
                    accentColor = Color(0xFF1EC8FF),
                    ledColor = Color(0xFF26C6FF),
                    insertVerb = "Feeding Disc into Optical Slot",
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
                    insertVerb = "Placing Optical Disc",
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
    /** Normalized insertion progress (`0f` hovering outside console, `1f` fully seated inside). */
    val insertDepth: Float,
    /** Media 3D yaw in degrees. */
    val mediaYawDeg: Float,
    /** Media 3D pitch in degrees. */
    val mediaPitchDeg: Float,
    /** Continuous optical disc spin rotation in degrees once seated. */
    val discSpinDeg: Float,
    /** Console lid/door/flap open fraction (`1f` fully open, `0f` shut). */
    val lidOpen: Float,
    /** Motorized sliding tray extension fraction (`1f` extended out, `0f` retracted flush). */
    val trayOut: Float,
    /** NES ZIF press-down drop (`0f..1f`) or DS/3DS/Switch spring-click over-travel bounce (`0f..1f`). */
    val latchMotion: Float,
    /** Subtle downward console recoil in dp when cartridge/disc clicks into place. */
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
            ConsoleLoaderKind.CARTRIDGE_FRONT_DECK -> {
                // Authentic 4-phase NES front-loader:
                // 1) Door open -> 2) Slide cart flat horizontally in -> 3) Press ZIF tray DOWN -> 4) Close door!
                val doorOpenT = smoothStep(p / 0.14f)
                val slideInT = easeInOutCubic((p - 0.14f) / 0.38f)
                val zifDropT = easeInOutCubic((p - 0.52f) / 0.18f)
                val doorCloseT = smoothStep((p - 0.70f) / 0.16f)
                val seated = p >= 0.66f
                val bouncePhase = clamp01((p - 0.62f) / 0.14f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) sin(bouncePhase * PI).toFloat() * 5.0f else 0f
                val ledGlow = smoothStep((p - 0.78f) / 0.14f)
                val bootFlash = smoothStep((p - 0.90f) / 0.10f)
                val status = when {
                    p < 0.16f -> "Opening NES cartridge door…"
                    p < 0.52f -> "Sliding Game Pak in…"
                    p < 0.72f -> "Pressing down into ZIF deck…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = slideInT,
                    mediaYawDeg = 8f * (1f - easeOutCubic(p / 0.22f)),
                    mediaPitchDeg = -58f, // Paired with rotationZ = 180f so pins enter first & label faces UP!
                    discSpinDeg = 0f,
                    lidOpen = (doorOpenT * (1f - doorCloseT)).coerceIn(0f, 1f),
                    trayOut = 0f,
                    latchMotion = zifDropT,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.CARTRIDGE_TOP_SLOT -> {
                val alignT = easeOutCubic(p / 0.24f)
                val plungeT = easeInOutCubic((p - 0.22f) / 0.44f)
                val seated = p >= 0.65f
                val bouncePhase = clamp01((p - 0.63f) / 0.15f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) sin(bouncePhase * PI).toFloat() * 5.5f else 0f
                val ledGlow = smoothStep((p - 0.70f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.26f -> spec.insertVerb + "…"
                    p < 0.70f -> "Seating into ${spec.consoleLabel}…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = plungeT,
                    mediaYawDeg = 16f * (1f - alignT),
                    mediaPitchDeg = -4f + 12f * alignT,
                    discSpinDeg = 0f,
                    lidOpen = plungeT,
                    trayOut = 0f,
                    latchMotion = 0f,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.HANDHELD_REAR_SLOT -> {
                val isForwardHorizontalSlot = spec.systemId == "nds" || spec.systemId == "3ds"
                val alignT = easeOutCubic(p / 0.20f)
                val slideT = easeInOutCubic((p - 0.18f) / 0.44f)
                // Authentic DS/3DS spring-loaded click over-travel & rebound flush into Slot-1!
                val springPhase = clamp01((p - 0.60f) / 0.14f)
                val springClick = if (springPhase > 0f && springPhase < 1f) sin(springPhase * PI).toFloat() else 0f
                val seated = p >= 0.64f
                val bounceDp = springClick * 4.2f
                val ledGlow = smoothStep((p - 0.70f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.24f -> spec.insertVerb + "…"
                    p < 0.70f -> "Clicking flush into ${spec.consoleLabel}…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = slideT,
                    mediaYawDeg = if (isForwardHorizontalSlot) 8f * (1f - alignT) else 14f * (1f - alignT),
                    mediaPitchDeg = if (isForwardHorizontalSlot) -58f else -4f + 10f * alignT,
                    discSpinDeg = 0f,
                    lidOpen = 1f,
                    trayOut = 0f,
                    latchMotion = springClick,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.SWITCH_TOP_FLAP -> {
                val flapOpenT = smoothStep(p / 0.16f)
                val dropT = easeInOutCubic((p - 0.16f) / 0.44f)
                val springPhase = clamp01((p - 0.58f) / 0.12f)
                val springClick = if (springPhase > 0f && springPhase < 1f) sin(springPhase * PI).toFloat() else 0f
                val flapCloseT = smoothStep((p - 0.70f) / 0.15f)
                val seated = p >= 0.60f
                val ledGlow = smoothStep((p - 0.74f) / 0.16f)
                val bootFlash = smoothStep((p - 0.89f) / 0.11f)
                val status = when {
                    p < 0.18f -> "Opening Game Card flap…"
                    p < 0.68f -> "Clicking Switch Game Card…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = dropT,
                    mediaYawDeg = 10f * (1f - easeOutCubic(p / 0.24f)),
                    mediaPitchDeg = 6f,
                    discSpinDeg = 0f,
                    lidOpen = (flapOpenT * (1f - flapCloseT)).coerceIn(0f, 1f),
                    trayOut = 0f,
                    latchMotion = springClick,
                    consoleBounceDp = springClick * 3.5f,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.DISC_TOP_LID -> {
                val dropT = easeInOutCubic((p - 0.08f) / 0.38f)
                val seated = p >= 0.46f
                val lidCloseT = smoothStep((p - 0.52f) / 0.26f)
                val spinT = clamp01((p - 0.44f) / 0.56f)
                val spinDeg = spinT * spinT * 980f
                val bouncePhase = clamp01((p - 0.44f) / 0.14f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) sin(bouncePhase * PI).toFloat() * 4.0f else 0f
                val ledGlow = smoothStep((p - 0.72f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.46f -> spec.insertVerb + "…"
                    p < 0.76f -> "Closing lid & spinning up…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = dropT,
                    mediaYawDeg = 0f,
                    mediaPitchDeg = 0f,
                    discSpinDeg = spinDeg,
                    lidOpen = 1f - lidCloseT,
                    trayOut = 0f,
                    latchMotion = 0f,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.DISC_SLIDE_TRAY -> {
                val dropT = easeInOutCubic((p - 0.06f) / 0.34f)
                val seated = p >= 0.40f
                val retractT = easeInOutCubic((p - 0.44f) / 0.34f)
                val spinT = clamp01((p - 0.40f) / 0.60f)
                val ledGlow = smoothStep((p - 0.72f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.42f -> spec.insertVerb + "…"
                    p < 0.78f -> "Retracting motorized disc tray…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = dropT,
                    mediaYawDeg = 0f,
                    mediaPitchDeg = 0f,
                    discSpinDeg = spinT * spinT * 760f,
                    lidOpen = 0f,
                    trayOut = 1f - retractT,
                    latchMotion = 0f,
                    consoleBounceDp = 0f,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.DISC_REAR_UMD -> {
                val slideT = easeInOutCubic((p - 0.10f) / 0.38f)
                val seated = p >= 0.48f
                val hatchCloseT = smoothStep((p - 0.52f) / 0.26f)
                val bouncePhase = clamp01((p - 0.74f) / 0.12f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) sin(bouncePhase * PI).toFloat() * 3.5f else 0f
                val ledGlow = smoothStep((p - 0.74f) / 0.16f)
                val bootFlash = smoothStep((p - 0.89f) / 0.11f)
                val status = when {
                    p < 0.48f -> spec.insertVerb + "…"
                    p < 0.78f -> "Closing UMD hatch…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = slideT,
                    mediaYawDeg = 8f * (1f - easeOutCubic(p / 0.22f)),
                    mediaPitchDeg = 4f,
                    discSpinDeg = 0f,
                    lidOpen = 1f - hatchCloseT,
                    trayOut = 0f,
                    latchMotion = 0f,
                    consoleBounceDp = bounceDp,
                    ledGlow = ledGlow,
                    bootFlash = bootFlash,
                    seated = seated,
                    statusText = status,
                )
            }

            ConsoleLoaderKind.DISC_SLOT_IN -> {
                val alignT = easeOutCubic(p / 0.18f)
                val feedT = easeInOutCubic((p - 0.16f) / 0.56f)
                val seated = p >= 0.70f
                val ledGlow = smoothStep((p - 0.12f) / 0.55f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.70f -> spec.insertVerb + "…"
                    else -> "Reading disc · ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = feedT,
                    mediaYawDeg = -34f * alignT,
                    mediaPitchDeg = 6f,
                    discSpinDeg = feedT * 220f,
                    lidOpen = 1f,
                    trayOut = 0f,
                    latchMotion = 0f,
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

/** Decode & memoize a photorealistic console studio WebP drawable as a Compose [ImageBitmap]. */
@Composable
private fun rememberConsolePhoto(@DrawableRes resId: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(resId) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
        }.getOrNull()
    }
}

private fun DrawScope.drawPhotoCentered(
    photo: ImageBitmap,
    destLeft: Float,
    destTop: Float,
    destW: Float,
    destH: Float,
    alpha: Float = 1f,
) {
    drawImage(
        image = photo,
        dstOffset = IntOffset(destLeft.roundToInt(), destTop.roundToInt()),
        dstSize = IntSize(destW.roundToInt().coerceAtLeast(1), destH.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
    )
}

/** Additive glowing LED bloom for real photographed consoles (never draws a fake black dot when off). */
private fun DrawScope.drawPhotoLedBloom(
    center: Offset,
    color: Color,
    glow: Float,
    radiusPx: Float = 3.4.dp.toPx(),
    haloPx: Float = 20.dp.toPx(),
) {
    if (glow <= 0.04f) return
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = 0.92f * glow),
            0.42f to color.copy(alpha = 0.36f * glow),
            1f to Color.Transparent,
            center = center,
            radius = haloPx,
        ),
        radius = haloPx,
        center = center,
    )
    drawCircle(
        color = color.copy(alpha = (0.45f + glow * 0.55f).coerceIn(0f, 1f)),
        radius = radiusPx,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.72f * glow),
        radius = radiusPx * 0.44f,
        center = center,
    )
}

/**
 * Tactile fullscreen Console Media Insertion Ceremony triggered when pressing Play from a 3D model!
 * Animates the game's actual 3D Cartridge / Game Card / UMD / Optical Disc inserting the exact
 * physical way into the real matching console hardware, clicks with haptics, illuminates the Power
 * LED, and launches the emulator.
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
            animationSpec = tween(durationMillis = 1380, easing = LinearEasing),
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
                    0.0f to hwSpec.accentColor.copy(alpha = 0.18f + frame.ledGlow * 0.14f),
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top badge showing the hardware system
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

            // Main 3D Media + Real Photorealistic Console Hardware Stage (360dp tall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .offset(y = frame.consoleBounceDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (hwSpec.loaderKind) {
                    ConsoleLoaderKind.CARTRIDGE_FRONT_DECK -> NesPhotoFrontDeckStage(
                        spec = spec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.CARTRIDGE_TOP_SLOT -> HomeConsolePhotoTopSlotStage(
                        spec = spec,
                        hwSpec = hwSpec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.HANDHELD_REAR_SLOT -> {
                        if (hwSpec.systemId == "nds" || hwSpec.systemId == "3ds") {
                            Ds3dsPhotoSlot1Stage(
                                spec = spec,
                                hwSpec = hwSpec,
                                textures = textures,
                                frame = frame,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            GameBoyGbaPhotoRearSlotStage(
                                spec = spec,
                                hwSpec = hwSpec,
                                textures = textures,
                                frame = frame,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    ConsoleLoaderKind.SWITCH_TOP_FLAP -> SwitchPhotoTopFlapStage(
                        spec = spec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.DISC_TOP_LID -> DiscTopLidStage(
                        spec = spec,
                        hwSpec = hwSpec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.DISC_SLIDE_TRAY -> Ps2SlideTrayInsertionStage(
                        spec = spec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.DISC_REAR_UMD -> PspPhotoRearUmdStage(
                        spec = spec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.DISC_SLOT_IN -> WiiVerticalSlotInsertionStage(
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

// =================================================================================================
// 1. REAL NINTENDO DS LITE & NINTENDO 3DS — Photorealistic Rear-Top Clamshell + Horizontal Slot-1!
// =================================================================================================

@Composable
private fun Ds3dsPhotoSlot1Stage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val is3ds = hwSpec.systemId == "3ds"
    val photo = rememberConsolePhoto(if (is3ds) R.drawable.console_3ds else R.drawable.console_nds)

    // Measured exact normalized coordinates from console_grid_sheet_1.png
    val consoleW = 336f
    val consoleH = if (is3ds) consoleW * (327f / 720f) else consoleW * (288f / 720f) // 152.6dp / 134.4dp
    val consoleTopY = if (is3ds) 76f else 84f
    val slotTopFrac = if (is3ds) 0.435f else 0.755f
    val slotTopY = consoleTopY + consoleH * slotTopFrac
    val ledXFrac = if (is3ds) 0.90f else 0.078f
    val ledYFrac = if (is3ds) 0.53f else 0.515f

    // DS/3DS Game Card lies flat (pins first, label facing UP!) & slides straight forward into Slot-1!
    val cardSizeDp = if (is3ds) 128f else 120f
    val startY = slotTopY + 34f
    val endY = slotTopY - cardSizeDp * 0.56f
    val yDp = startY + (endY - startY) * frame.insertDepth - frame.latchMotion * 7.5f
    val cardScale = 1.05f - frame.insertDepth * 0.15f
    val cardAlpha = if (frame.insertDepth > 0.92f) {
        (1f - ((frame.insertDepth - 0.92f) / 0.08f) * 0.55f).coerceIn(0.45f, 1f)
    } else 1f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        // Layer 1: Full real photorealistic Nintendo DS Lite / 3DS + soft grounded shadow
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            drawConsoleShadow(size.width * 0.5f, top + ch + 10.dp.toPx(), cw * 1.02f, 28.dp.toPx())
            if (photo != null) {
                drawPhotoCentered(photo, left, top, cw, ch)
            } else {
                drawNdsOr3dsConsoleBack(hwSpec, frame)
            }
        }

        // Layer 2: Real 3D DS / 3DS Game Card sliding flat horizontally into Slot-1!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(top = slotTopY.dp.toPx()) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(cardSizeDp.dp)
                    .offset(y = yDp.dp)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        rotationZ = 180f // Pairs with pitchDeg = -58f: gold pins enter Slot-1 FIRST, label faces UP!
                        alpha = cardAlpha
                    },
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = spec.mediaKind.defaultAspect,
                    yawDeg = frame.mediaYawDeg,
                    pitchDeg = frame.mediaPitchDeg,
                    floorShadow = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Layer 3: Upper lip of the real DS/3DS photo + subtle slot AO shadow + additive glowing hinge LED!
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            val clipY = slotTopY.dp.toPx()
            if (photo != null) {
                clipRect(bottom = clipY) {
                    drawPhotoCentered(photo, left, top, cw, ch)
                }
            } else {
                drawNdsOr3dsConsoleFront(hwSpec, frame)
            }
            if (frame.insertDepth > 0.25f) {
                val slotW = cw * (if (is3ds) 0.26f else 0.24f)
                drawLine(
                    color = Color.Black.copy(alpha = 0.52f * frame.insertDepth),
                    start = Offset(size.width * 0.5f - slotW * 0.5f, clipY + 1.dp.toPx()),
                    end = Offset(size.width * 0.5f + slotW * 0.5f, clipY + 1.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawPhotoLedBloom(
                center = Offset(left + cw * ledXFrac, top + ch * ledYFrac),
                color = hwSpec.ledColor,
                glow = frame.ledGlow,
                radiusPx = 3.4.dp.toPx(),
                haloPx = 20.dp.toPx(),
            )
        }
    }
}

// =================================================================================================
// 2. REAL 1985 NES-001 — Photorealistic Open Flip-Door + Horizontal Slide + ZIF Click + Door Shut!
// =================================================================================================

@Composable
private fun NesPhotoFrontDeckStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val photo = rememberConsolePhoto(R.drawable.console_nes)

    // Exact normalized measurements from console_grid_sheet_1.png (720x463)
    val consoleW = 336f
    val consoleH = consoleW * (463f / 720f) // 216.1dp
    val consoleTopY = 52f
    val slotCxFrac = 0.42f
    val slotTopY = consoleTopY + consoleH * 0.465f // 152.5dp (bottom rim of open flip-up door!)
    val slotOffsetX = (slotCxFrac - 0.5f) * consoleW // -26.9dp

    val cartW = 188f
    val cartH = 166f
    val startY = slotTopY + 52f
    val endY = slotTopY - cartH * 0.46f
    val yDp = startY + (endY - startY) * frame.insertDepth + frame.latchMotion * 13f
    val scale = 1.04f - frame.insertDepth * 0.24f
    val cartAlpha = if (frame.seated) (frame.lidOpen * 1.55f).coerceIn(0f, 1f) else 1f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            drawConsoleShadow(size.width * 0.5f, top + ch + 8.dp.toPx(), cw * 1.04f, 30.dp.toPx())
            if (photo != null) {
                drawPhotoCentered(photo, left, top, cw, ch)
            } else {
                drawNesConsoleBack(frame)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(top = slotTopY.dp.toPx()) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(cartW.dp)
                    .height(cartH.dp)
                    .offset(x = slotOffsetX.dp, y = yDp.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = 180f // Pairs with pitchDeg = -58f: pins go IN first, label faces UP!
                        alpha = cartAlpha
                    },
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = spec.mediaKind.defaultAspect,
                    yawDeg = frame.mediaYawDeg,
                    pitchDeg = frame.mediaPitchDeg,
                    floorShadow = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            val clipY = slotTopY.dp.toPx()
            if (photo != null) {
                clipRect(bottom = clipY) {
                    drawPhotoCentered(photo, left, top, cw, ch)
                }
                if (frame.seated && frame.lidOpen < 0.96f) {
                    val closed = (1f - frame.lidOpen).coerceIn(0f, 1f)
                    val bayL = left + cw * 0.195f
                    val bayW = cw * 0.455f
                    val bayTop = top + ch * 0.460f
                    val bayH = ch * 0.225f * closed
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0xFFDCDAD3),
                            1f to Color(0xFFBDBBB3),
                            startY = bayTop,
                            endY = bayTop + bayH,
                        ),
                        topLeft = Offset(bayL, bayTop),
                        size = Size(bayW, bayH),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color(0xFF706E68).copy(alpha = 0.55f),
                        topLeft = Offset(bayL, bayTop),
                        size = Size(bayW, bayH),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(1.2.dp.toPx()),
                    )
                    if (closed > 0.35f) {
                        drawLine(
                            color = Color(0xFFD62828),
                            start = Offset(bayL + 10.dp.toPx(), bayTop + bayH - 10.dp.toPx()),
                            end = Offset(bayL + bayW * 0.65f, bayTop + bayH - 10.dp.toPx()),
                            strokeWidth = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
                drawPhotoLedBloom(
                    center = Offset(left + cw * 0.165f, top + ch * 0.845f),
                    color = Color(0xFFFF2430),
                    glow = frame.ledGlow,
                    radiusPx = 3.2.dp.toPx(),
                    haloPx = 18.dp.toPx(),
                )
            } else {
                drawNesConsoleFront(frame)
            }
        }
    }
}

// =================================================================================================
// 3. REAL HOME TOP-LOADING CONSOLES: SNES, NINTENDO 64, SEGA GENESIS (Photorealistic Studio Photos!)
// =================================================================================================

@Composable
private fun HomeConsolePhotoTopSlotStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val id = hwSpec.systemId
    val resId = when (id) {
        "snes" -> R.drawable.console_snes
        "n64" -> R.drawable.console_n64
        else -> R.drawable.console_genesis
    }
    val photo = rememberConsolePhoto(resId)

    val consoleW = when (id) {
        "snes" -> 332f
        "n64" -> 332f
        else -> 322f
    }
    val consoleH = when (id) {
        "snes" -> consoleW * (453f / 720f)
        "n64" -> consoleW * (438f / 720f)
        else -> consoleW * (514f / 720f)
    }
    val consoleTopY = when (id) {
        "snes" -> 134f
        "n64" -> 138f
        else -> 114f
    }
    val slotCxFrac = if (id == "snes" || id == "n64") 0.50f else 0.60f
    val slotLipFrac = when (id) {
        "snes" -> 0.265f
        "n64" -> 0.255f
        else -> 0.305f
    }
    val slotLipY = consoleTopY + consoleH * slotLipFrac
    val slotOffsetX = (slotCxFrac - 0.5f) * consoleW

    val cartW = when (id) {
        "snes" -> 200f
        "n64" -> 164f
        else -> 160f
    }
    val cartH = when (id) {
        "snes" -> 166f
        "n64" -> 148f
        else -> 144f
    }
    val startY = 4f
    val endY = slotLipY - cartH * 0.56f
    val yDp = startY + (endY - startY) * frame.insertDepth

    val (ledXFrac, ledYFrac) = when (id) {
        "snes" -> 0.175f to 0.425f
        "n64" -> 0.500f to 0.780f
        else -> 0.605f to 0.705f
    }

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            drawConsoleShadow(size.width * 0.5f, top + ch + 6.dp.toPx(), cw * 1.04f, 28.dp.toPx())
            if (photo != null) {
                drawPhotoCentered(photo, left, top, cw, ch)
            } else {
                when (id) {
                    "snes" -> drawSnesConsoleBack(frame)
                    "n64" -> drawN64ConsoleBack(frame)
                    else -> drawGenesisConsoleBack(frame)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(slotLipY.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = spec.mediaKind.defaultAspect,
                yawDeg = frame.mediaYawDeg,
                pitchDeg = frame.mediaPitchDeg,
                floorShadow = false,
                modifier = Modifier
                    .width(cartW.dp)
                    .height(cartH.dp)
                    .offset(x = slotOffsetX.dp, y = yDp.dp),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            val clipY = slotLipY.dp.toPx()
            if (photo != null) {
                clipRect(top = clipY) {
                    drawPhotoCentered(photo, left, top, cw, ch)
                }
                if (frame.insertDepth > 0.35f) {
                    val slotW = cartW.dp.toPx() * 0.72f
                    val scx = size.width * 0.5f + slotOffsetX.dp.toPx()
                    drawLine(
                        color = Color.Black.copy(alpha = 0.45f * frame.insertDepth),
                        start = Offset(scx - slotW * 0.5f, clipY),
                        end = Offset(scx + slotW * 0.5f, clipY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                drawPhotoLedBloom(
                    center = Offset(left + cw * ledXFrac, top + ch * ledYFrac),
                    color = hwSpec.ledColor,
                    glow = frame.ledGlow,
                    radiusPx = 3.6.dp.toPx(),
                    haloPx = 20.dp.toPx(),
                )
            } else {
                when (id) {
                    "snes" -> drawSnesConsoleFront(frame)
                    "n64" -> drawN64ConsoleFront(frame)
                    else -> drawGenesisConsoleFront(frame)
                }
            }
        }
    }
}

// =================================================================================================
// 4. REAL GAME BOY (DMG-01), GAME BOY COLOR, GAME BOY ADVANCE — Photorealistic Rear-Top Shells!
// =================================================================================================

@Composable
private fun GameBoyGbaPhotoRearSlotStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val id = hwSpec.systemId
    val isGba = id == "gba"
    val resId = when (id) {
        "gba" -> R.drawable.console_gba
        "gbc" -> R.drawable.console_gbc
        else -> R.drawable.console_gb
    }
    val photo = rememberConsolePhoto(resId)

    val consoleW = if (isGba) 334f else 308f
    val consoleH = if (isGba) consoleW * (431f / 720f) else consoleW * (469f / 720f)
    val consoleTopY = 142f
    val slotLipFrac = if (isGba) 0.275f else 0.295f
    val slotLipY = consoleTopY + consoleH * slotLipFrac

    val cartW = if (isGba) 152f else 172f
    val cartH = if (isGba) 104f else 160f
    val startY = 8f
    val endY = if (isGba) slotLipY - cartH * 0.44f else slotLipY - cartH * 0.56f
    val yDp = startY + (endY - startY) * frame.insertDepth

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            drawConsoleShadow(size.width * 0.5f, top + ch + 6.dp.toPx(), cw * 1.02f, 26.dp.toPx())
            if (photo != null) {
                drawPhotoCentered(photo, left, top, cw, ch)
            } else if (isGba) {
                drawGbaConsoleBack(hwSpec, frame)
            } else {
                drawGameBoyConsoleBack(hwSpec, frame)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(slotLipY.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = spec.mediaKind.defaultAspect,
                yawDeg = frame.mediaYawDeg,
                pitchDeg = frame.mediaPitchDeg,
                floorShadow = false,
                modifier = Modifier
                    .width(cartW.dp)
                    .height(cartH.dp)
                    .offset(y = yDp.dp),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            val clipY = slotLipY.dp.toPx()
            if (photo != null) {
                clipRect(top = clipY) {
                    drawPhotoCentered(photo, left, top, cw, ch)
                }
                val ledPt = if (isGba) {
                    Offset(left + cw * 0.87f, top + ch * 0.22f)
                } else {
                    Offset(left + cw * 0.145f, top + ch * 0.72f)
                }
                drawPhotoLedBloom(ledPt, hwSpec.ledColor, frame.ledGlow, 3.4.dp.toPx(), 18.dp.toPx())
            } else if (isGba) {
                drawGbaConsoleFront(hwSpec, frame)
            } else {
                drawGameBoyConsoleFront(hwSpec, frame)
            }
        }
    }
}

// =================================================================================================
// 5. REAL NINTENDO SWITCH — Photorealistic Joy-Cons + Real Top-Right Open Game Card Slot!
// =================================================================================================

@Composable
private fun SwitchPhotoTopFlapStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val photo = rememberConsolePhoto(R.drawable.console_switch)

    // Exact normalized measurements from console_grid_sheet_1.png (720x349): open flap spans x = 0.655..0.755 -> center 0.705!
    val consoleW = 344f
    val consoleH = consoleW * (349f / 720f) // 166.7dp
    val consoleTopY = 154f
    val slotCxFrac = 0.705f
    val slotLipFrac = 0.295f
    val slotLipY = consoleTopY + consoleH * slotLipFrac // 203.2dp
    val slotOffsetX = (slotCxFrac - 0.5f) * consoleW // +70.5dp

    val cardW = 50f
    val cardH = 66f
    val startY = 30f
    val endY = slotLipY - 8f
    val yDp = startY + (endY - startY) * frame.insertDepth + frame.latchMotion * 5f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            drawConsoleShadow(size.width * 0.5f, top + ch + 6.dp.toPx(), cw * 0.98f, 24.dp.toPx())
            if (photo != null) {
                drawPhotoCentered(photo, left, top, cw, ch)
            } else {
                drawSwitchConsoleBack(frame)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(slotLipY.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = spec.mediaKind.defaultAspect,
                yawDeg = frame.mediaYawDeg,
                pitchDeg = frame.mediaPitchDeg,
                floorShadow = false,
                modifier = Modifier
                    .width(cardW.dp)
                    .height(cardH.dp)
                    .offset(x = slotOffsetX.dp, y = yDp.dp),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            val clipY = slotLipY.dp.toPx()
            if (photo != null) {
                clipRect(top = clipY) {
                    drawPhotoCentered(photo, left, top, cw, ch)
                }
                if (frame.ledGlow > 0.04f) {
                    val scrL = left + cw * 0.22f
                    val scrT = top + ch * 0.36f
                    val scrW = cw * 0.56f
                    val scrH = ch * 0.50f
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            0f to Color(0xFFE60012).copy(alpha = 0.55f * frame.ledGlow),
                            1f to Color.Transparent,
                            center = Offset(scrL + scrW * 0.5f, scrT + scrH * 0.5f),
                            radius = scrW * 0.55f,
                        ),
                        topLeft = Offset(scrL, scrT),
                        size = Size(scrW, scrH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
            } else {
                drawSwitchConsoleFront(frame)
            }
        }
    }
}

// =================================================================================================
// 6. REAL PLAYSTATION 1 & GAMECUBE (Photorealistic Open-Lid Photos!) + DREAMCAST / SATURN
// =================================================================================================

@Composable
private fun DiscTopLidStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val id = hwSpec.systemId
    val isPs1 = id == "psx" || id == "ps1"
    val isGc = id == "gc" || id == "gamecube"

    if (isPs1 || isGc) {
        val photo = rememberConsolePhoto(if (isPs1) R.drawable.console_ps1 else R.drawable.console_gc)
        // Exact spindle coordinates verified directly on console_grid_sheet_1.png (PS1: 0.555) & sheet_2.png (GC: 0.438)
        val consoleW = if (isPs1) 334f else 232f
        val consoleH = if (isPs1) consoleW * (537f / 720f) else consoleW * (918f / 720f)
        val consoleTopY = if (isPs1) 70f else 42f
        val bayCyFrac = if (isPs1) 0.555f else 0.438f
        val bayRxFrac = if (isPs1) 0.278f else 0.285f
        val bayRyFrac = if (isPs1) 0.205f else 0.125f
        val bayCyDp = consoleTopY + consoleH * bayCyFrac
        val bayRxDp = consoleW * bayRxFrac
        val bayRyDp = consoleH * bayRyFrac
        val targetForeshorten = (bayRyDp / bayRxDp).coerceIn(0.48f, 0.65f)

        // Disc fills the real circular bay snugly; Physical3dStageCanvas centers disc at 0.48 * size!
        val discSizeDp = (bayRxDp * 2f * 0.94f) / 0.78f
        val startY = 4f
        val endY = bayCyDp - discSizeDp * 0.48f
        val yDp = startY + (endY - startY) * frame.insertDepth
        val foreshortenY = (0.82f - frame.insertDepth * (0.82f - targetForeshorten)).coerceIn(targetForeshorten, 0.90f)
        val discAlpha = if (frame.lidOpen < 0.40f) (frame.lidOpen / 0.40f).coerceIn(0f, 1f) else 1f

        Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
            Canvas(Modifier.fillMaxSize()) {
                val left = (size.width - consoleW.dp.toPx()) * 0.5f
                val top = consoleTopY.dp.toPx()
                val cw = consoleW.dp.toPx()
                val ch = consoleH.dp.toPx()
                drawConsoleShadow(size.width * 0.5f, top + ch + 6.dp.toPx(), cw * 1.04f, 28.dp.toPx())
                if (photo != null) {
                    drawPhotoCentered(photo, left, top, cw, ch)
                } else {
                    drawTopLidConsoleBack(hwSpec, frame)
                }
            }

            Box(
                modifier = Modifier
                    .size(discSizeDp.dp)
                    .offset(y = yDp.dp)
                    .graphicsLayer {
                        scaleX = 0.98f
                        scaleY = 0.98f * foreshortenY
                        alpha = discAlpha
                    },
                contentAlignment = Alignment.Center,
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = 1.0f,
                    yawDeg = 0f,
                    pitchDeg = 0f,
                    floorShadow = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = frame.discSpinDeg },
                )
            }

            Canvas(Modifier.fillMaxSize()) {
                val left = (size.width - consoleW.dp.toPx()) * 0.5f
                val top = consoleTopY.dp.toPx()
                val cw = consoleW.dp.toPx()
                val ch = consoleH.dp.toPx()
                val cx = size.width * 0.5f
                val cy = bayCyDp.dp.toPx()
                val rx = bayRxDp.dp.toPx()
                val ry = bayRyDp.dp.toPx()

                // Real metallic 3-ball spindle hub snapping UP through the disc's center hole when seated!
                if (frame.insertDepth > 0.86f && frame.lidOpen > 0.35f) {
                    val hubAlpha = ((frame.insertDepth - 0.86f) / 0.14f).coerceIn(0f, 1f) * discAlpha
                    drawOval(
                        color = Color(0xFF1E2128).copy(alpha = 0.92f * hubAlpha),
                        topLeft = Offset(cx - 8.dp.toPx(), cy - 5.dp.toPx()),
                        size = Size(16.dp.toPx(), 10.dp.toPx()),
                    )
                    drawOval(
                        color = Color(0xFFD5DCE6).copy(alpha = 0.95f * hubAlpha),
                        topLeft = Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx()),
                        size = Size(10.dp.toPx(), 6.dp.toPx()),
                    )
                    drawCircle(Color.White.copy(alpha = hubAlpha), 1.3.dp.toPx(), Offset(cx - 3.5.dp.toPx(), cy))
                    drawCircle(Color.White.copy(alpha = hubAlpha), 1.3.dp.toPx(), Offset(cx + 3.5.dp.toPx(), cy))
                    drawCircle(Color.White.copy(alpha = hubAlpha), 1.3.dp.toPx(), Offset(cx, cy - 2.dp.toPx()))
                }

                // Closing matching circular lid sweeping down from the real rear hinge over the spinning disc!
                if (frame.lidOpen < 0.96f) {
                    val closed = (1f - frame.lidOpen).coerceIn(0f, 1f)
                    val lidTop = cy - ry - (1f - closed) * 56.dp.toPx()
                    val lidH = ry * 2f * (0.30f + 0.70f * closed)
                    val lidTopCol = if (isPs1) Color(0xFFB5B8BE) else Color(0xFF5D50B0)
                    val lidMidCol = if (isPs1) Color(0xFF9699A0) else Color(0xFF42368C)
                    drawOval(
                        brush = Brush.verticalGradient(
                            0f to lidTopCol.copy(alpha = (0.38f + closed * 0.62f).coerceIn(0f, 1f)),
                            1f to lidMidCol.copy(alpha = (0.38f + closed * 0.62f).coerceIn(0f, 1f)),
                            startY = lidTop,
                            endY = lidTop + lidH,
                        ),
                        topLeft = Offset(cx - rx * 1.03f, lidTop),
                        size = Size(rx * 2.06f, lidH),
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.28f * closed),
                        topLeft = Offset(cx - rx * 1.03f, lidTop),
                        size = Size(rx * 2.06f, lidH),
                        style = Stroke(1.4.dp.toPx()),
                    )
                    if (closed > 0.40f) {
                        val lidCy = lidTop + lidH * 0.5f
                        if (isPs1) {
                            drawOval(
                                color = Color(0xFF6B6E75).copy(alpha = 0.55f * closed),
                                topLeft = Offset(cx - rx * 0.80f, lidCy - lidH * 0.38f),
                                size = Size(rx * 1.60f, lidH * 0.76f),
                                style = Stroke(1.3.dp.toPx()),
                            )
                        } else {
                            drawOval(
                                color = Color(0xFF101218).copy(alpha = 0.94f * closed),
                                topLeft = Offset(cx - 22.dp.toPx(), lidCy - lidH * 0.24f),
                                size = Size(44.dp.toPx(), lidH * 0.48f),
                            )
                        }
                    }
                }

                // Additive bloom strictly on the real photographed LED lens when powered on!
                val ledPt = if (isPs1) {
                    Offset(left + cw * 0.179f, top + ch * 0.770f)
                } else {
                    Offset(left + cw * 0.50f, top + ch * 0.545f)
                }
                drawPhotoLedBloom(ledPt, hwSpec.ledColor, frame.ledGlow, 4.dp.toPx(), 22.dp.toPx())
            }
        }
        return
    }

    // Dreamcast / Saturn / Sega CD high-craft studio hardware stage
    val isDreamcast = id == "dc" || id == "dreamcast"
    val discSizeDp = if (isDreamcast) 218f else 226f
    val bayCyDp = if (isDreamcast) 360f * 0.60f else 360f * 0.61f
    val startY = 4f
    val endY = bayCyDp - discSizeDp * 0.5f
    val yDp = startY + (endY - startY) * frame.insertDepth
    val foreshortenY = (0.82f - frame.insertDepth * 0.32f).coerceIn(0.50f, 0.90f)
    val discAlpha = if (frame.lidOpen < 0.42f) (frame.lidOpen / 0.42f).coerceIn(0f, 1f) else 1f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawTopLidConsoleBack(hwSpec, frame) }

        Box(
            modifier = Modifier
                .size(discSizeDp.dp)
                .offset(y = yDp.dp)
                .graphicsLayer {
                    scaleX = 0.96f - frame.insertDepth * 0.03f
                    scaleY = (0.96f - frame.insertDepth * 0.03f) * foreshortenY
                    alpha = discAlpha
                },
            contentAlignment = Alignment.Center,
        ) {
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = 1.0f,
                yawDeg = 0f,
                pitchDeg = 0f,
                floorShadow = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = frame.discSpinDeg },
            )
        }

        Canvas(Modifier.fillMaxSize()) { drawTopLidConsoleFront(hwSpec, frame) }
    }
}

// =================================================================================================
// 7. REAL SONY PSP-1000 — Photorealistic Rear Shell + Sliding UMD + Real Closing Chrome-Ring Door!
// =================================================================================================

@Composable
private fun PspPhotoRearUmdStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val photo = rememberConsolePhoto(R.drawable.console_psp)

    // Exact measurements from console_grid_sheet_1.png (720x426)
    val consoleW = 340f
    val consoleH = consoleW * (426f / 720f) // 201.2dp
    val consoleTopY = 122f
    val bayCyDp = consoleTopY + consoleH * 0.385f // 199.5dp (dead center of brushed stainless-steel UMD spindle!)
    val doorTopFrac = 0.555f
    val doorTopY = consoleTopY + consoleH * doorTopFrac // 233.7dp (top rim of the real open chrome-ring door!)

    val umdSizeDp = 132f
    val startY = 14f
    val endY = bayCyDp - umdSizeDp * 0.5f + 10f
    val yDp = startY + (endY - startY) * frame.insertDepth
    val umdAlpha = if (frame.lidOpen < 0.38f) (frame.lidOpen / 0.38f).coerceIn(0f, 1f) else 1f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        // Layer 1: Full real photorealistic Sony PSP-1000 back showing stainless-steel internal UMD drive!
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            drawConsoleShadow(size.width * 0.5f, top + ch + 4.dp.toPx(), cw * 0.96f, 26.dp.toPx())
            if (photo != null) {
                drawPhotoCentered(photo, left, top, cw, ch)
            } else {
                drawPspConsoleBack(frame)
            }
        }

        // Layer 2: Real 3D UMD sliding straight DOWN into the brushed stainless-steel basket BEHIND the open chrome door!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((doorTopY + 30f).dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(umdSizeDp.dp)
                    .offset(y = yDp.dp)
                    .graphicsLayer { alpha = umdAlpha },
                contentAlignment = Alignment.Center,
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = 1.0f,
                    yawDeg = frame.mediaYawDeg,
                    pitchDeg = frame.mediaPitchDeg,
                    floorShadow = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Layer 3: THE REAL PHOTOGRAPHED `SONY PSP` CHROME-RING DOOR sliced straight from console_psp.webp!
        // While open, it masks the bottom of the sliding UMD; when closing, it smoothly lifts & snaps flush over the bay!
        Canvas(Modifier.fillMaxSize()) {
            val left = (size.width - consoleW.dp.toPx()) * 0.5f
            val top = consoleTopY.dp.toPx()
            val cw = consoleW.dp.toPx()
            val ch = consoleH.dp.toPx()
            if (photo != null) {
                val closed = (1f - frame.lidOpen).coerceIn(0f, 1f)
                // Slice source rect of the real chrome-ring SONY PSP door (x: 26%..74%, y: 55.5%..98%)
                val srcX = (photo.width * 0.26f).roundToInt()
                val srcY = (photo.height * doorTopFrac).roundToInt()
                val srcW = (photo.width * 0.48f).roundToInt()
                val srcH = (photo.height * (0.98f - doorTopFrac)).roundToInt()

                // Interpolate door position smoothly from open downward (55.5%..98%) UP flush over the UMD bay (11%..64%)!
                val dstL = left + cw * 0.26f
                val dstW = cw * 0.48f
                val openTop = top + ch * doorTopFrac
                val openH = ch * (0.98f - doorTopFrac)
                val closedTop = top + ch * 0.11f
                val closedH = ch * 0.54f
                val curTop = openTop + (closedTop - openTop) * closed
                val curH = openH + (closedH - openH) * closed

                drawImage(
                    image = photo,
                    srcOffset = IntOffset(srcX, srcY),
                    srcSize = IntSize(srcW, srcH),
                    dstOffset = IntOffset(dstL.roundToInt(), curTop.roundToInt()),
                    dstSize = IntSize(dstW.roundToInt(), curH.roundToInt().coerceAtLeast(1)),
                )
                drawPhotoLedBloom(
                    center = Offset(left + cw * 0.91f, top + ch * 0.52f),
                    color = Color(0xFF34EB6E),
                    glow = frame.ledGlow,
                    radiusPx = 3.5.dp.toPx(),
                    haloPx = 18.dp.toPx(),
                )
            } else {
                drawPspConsoleFront(frame)
            }
        }
    }
}

// =================================================================================================
// 8. PlayStation 2: Motorized Front Disc Tray extending out & retracting straight into the Monolith!
// =================================================================================================

@Composable
private fun Ps2SlideTrayInsertionStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val discSizeDp = 194f
    val trayBackY = 360f * 0.47f + 14f
    val trayTipY = 360f * 0.47f + 28f + frame.trayOut * 84f
    val trayCenterY = trayBackY + (trayTipY - trayBackY) * 0.54f
    val seatedTopY = trayCenterY - discSizeDp * 0.5f
    val yDp = 8f + (seatedTopY - 8f) * frame.insertDepth
    val foreshortenY = (0.80f - frame.insertDepth * 0.34f).coerceIn(0.42f, 0.88f)
    val discAlpha = (frame.trayOut * 1.4f).coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawPs2ConsoleBack(frame) }

        val clipModifier = if (frame.seated) {
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(top = size.height * 0.475f) {
                        this@drawWithContent.drawContent()
                    }
                }
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = clipModifier, contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .size(discSizeDp.dp)
                    .offset(x = (-56).dp, y = yDp.dp)
                    .graphicsLayer {
                        scaleX = 0.94f
                        scaleY = 0.94f * foreshortenY
                        alpha = discAlpha
                    },
                contentAlignment = Alignment.Center,
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = 1.0f,
                    yawDeg = 0f,
                    pitchDeg = 0f,
                    floorShadow = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = frame.discSpinDeg },
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) { drawPs2ConsoleFront(frame) }
    }
}

// =================================================================================================
// 9. Nintendo Wii / Wii U: Upright Glossy Tower with Horizontal Feed into Vertical Electric-Blue Slot!
// =================================================================================================

@Composable
private fun WiiVerticalSlotInsertionStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val discSizeDp = 178f
    val offsetX = 96f + (-162f) * frame.insertDepth
    val discAlpha = (1f - ((frame.insertDepth - 0.85f) / 0.15f).coerceIn(0f, 1f))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawWiiConsoleBack(hwSpec, frame) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(left = size.width * 0.5f - 21.dp.toPx()) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(discSizeDp.dp)
                    .offset(x = offsetX.dp, y = (-6).dp)
                    .graphicsLayer { alpha = discAlpha },
                contentAlignment = Alignment.Center,
            ) {
                Physical3dStageCanvas(
                    subject = PhysicalViewerSubject.GAME_MEDIA,
                    spec = spec,
                    textures = textures,
                    boxAspect = 1.0f,
                    yawDeg = frame.mediaYawDeg,
                    pitchDeg = frame.mediaPitchDeg,
                    floorShadow = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = frame.discSpinDeg },
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) { drawWiiConsoleFront(hwSpec, frame) }
    }
}
