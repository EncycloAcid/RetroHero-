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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    /** NES ZIF press-down drop (`0f..1f`) or DS/Switch spring-click over-travel bounce (`0f..1f`). */
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
                // 1) Flip door UP -> 2) Slide cart flat horizontally in -> 3) Press ZIF tray DOWN -> 4) Flip door SHUT!
                val doorOpenT = smoothStep(p / 0.16f)
                val slideInT = easeInOutCubic((p - 0.15f) / 0.37f)
                val zifDropT = easeInOutCubic((p - 0.52f) / 0.18f)
                val doorCloseT = smoothStep((p - 0.70f) / 0.16f)
                val seated = p >= 0.66f
                val bouncePhase = clamp01((p - 0.62f) / 0.14f)
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) sin(bouncePhase * PI).toFloat() * 5.0f else 0f
                val ledGlow = smoothStep((p - 0.78f) / 0.14f)
                val bootFlash = smoothStep((p - 0.90f) / 0.10f)
                val status = when {
                    p < 0.18f -> "Opening NES cartridge door…"
                    p < 0.52f -> "Sliding Game Pak in…"
                    p < 0.72f -> "Pressing down into ZIF deck…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = slideInT,
                    mediaYawDeg = 10f * (1f - easeOutCubic(p / 0.22f)),
                    mediaPitchDeg = -58f, // Paired with rotationZ = 180f so pins enter first & front label faces UP!
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
                    mediaYawDeg = 20f * (1f - alignT),
                    mediaPitchDeg = -5f + 15f * alignT,
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
                val alignT = easeOutCubic(p / 0.22f)
                val slideT = easeInOutCubic((p - 0.20f) / 0.42f)
                // Authentic DS/3DS spring-loaded click over-travel & rebound flush into Slot-1!
                val springPhase = clamp01((p - 0.60f) / 0.14f)
                val springClick = if (springPhase > 0f && springPhase < 1f) sin(springPhase * PI).toFloat() else 0f
                val seated = p >= 0.64f
                val bounceDp = springClick * 4.5f
                val ledGlow = smoothStep((p - 0.70f) / 0.18f)
                val bootFlash = smoothStep((p - 0.88f) / 0.12f)
                val status = when {
                    p < 0.25f -> spec.insertVerb + "…"
                    p < 0.70f -> "Clicking flush into ${spec.consoleLabel}…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = slideT,
                    mediaYawDeg = 16f * (1f - alignT),
                    mediaPitchDeg = -4f + 10f * alignT,
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
                val flapOpenT = smoothStep(p / 0.18f)
                val dropT = easeInOutCubic((p - 0.18f) / 0.42f)
                val springPhase = clamp01((p - 0.58f) / 0.12f)
                val springClick = if (springPhase > 0f && springPhase < 1f) sin(springPhase * PI).toFloat() else 0f
                val flapCloseT = smoothStep((p - 0.70f) / 0.15f)
                val seated = p >= 0.60f
                val ledGlow = smoothStep((p - 0.74f) / 0.16f)
                val bootFlash = smoothStep((p - 0.89f) / 0.11f)
                val status = when {
                    p < 0.20f -> "Opening Game Card flap…"
                    p < 0.68f -> "Clicking Switch Game Card…"
                    else -> "Powering on ${spec.consoleLabel}…"
                }
                ConsoleInsertionFrame(
                    insertDepth = dropT,
                    mediaYawDeg = 12f * (1f - easeOutCubic(p / 0.24f)),
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
                val bounceDp = if (bouncePhase > 0f && bouncePhase < 1f) sin(bouncePhase * PI).toFloat() * 4.2f else 0f
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
                    mediaYawDeg = 10f * (1f - easeOutCubic(p / 0.22f)),
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

/**
 * Tactile fullscreen Console Media Insertion Ceremony triggered when pressing Play from a 3D model!
 * Animates the game's actual 3D Cartridge / Game Card / Optical Disc inserting the exact physical
 * way into the real matching console hardware, clicks with haptics, illuminates the Power LED,
 * and launches the emulator.
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
                    0.0f to hwSpec.accentColor.copy(alpha = 0.20f + frame.ledGlow * 0.14f),
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

            // Main 3D Media + Authentic Console Hardware Insertion Stage (360dp tall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .offset(y = frame.consoleBounceDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (hwSpec.loaderKind) {
                    ConsoleLoaderKind.CARTRIDGE_FRONT_DECK -> NesFrontDeckInsertionStage(
                        spec = spec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.CARTRIDGE_TOP_SLOT -> CartridgeTopSlotInsertionStage(
                        spec = spec,
                        hwSpec = hwSpec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.HANDHELD_REAR_SLOT -> HandheldRearSlotInsertionStage(
                        spec = spec,
                        hwSpec = hwSpec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.SWITCH_TOP_FLAP -> SwitchTopFlapInsertionStage(
                        spec = spec,
                        textures = textures,
                        frame = frame,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ConsoleLoaderKind.DISC_TOP_LID -> DiscTopLidInsertionStage(
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
                    ConsoleLoaderKind.DISC_REAR_UMD -> PspRearUmdInsertionStage(
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
// 1. NES Front-Loading Deck: Door flips UP -> Cart slides flat horizontally -> ZIF clicks DOWN -> Door shuts!
// =================================================================================================

@Composable
private fun NesFrontDeckInsertionStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawNesConsoleBack(frame) }

        // Clipped cleanly at the top ceiling of the open NES front bay (145dp) so pins disappear inside!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 145.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val slideY = 48f + (-86f) * frame.insertDepth + frame.latchMotion * 14f
            val scale = 1.02f - frame.insertDepth * 0.28f
            val cartAlpha = if (frame.seated) (frame.lidOpen * 1.6f).coerceIn(0f, 1f) else 1f
            Box(
                modifier = Modifier
                    .width(172.dp)
                    .height(158.dp)
                    .offset(x = (-55).dp, y = slideY.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = 180f // Pairs with pitchDeg = -58f: pins go IN first, grip faces you, label faces UP!
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

        Canvas(Modifier.fillMaxSize()) { drawNesConsoleFront(frame) }
    }
}

// =================================================================================================
// 2. Classic Top-Loading Home Consoles: SNES, Nintendo 64, Sega Genesis
// =================================================================================================

@Composable
private fun CartridgeTopSlotInsertionStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val slotLipYDp = 220.dp
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            when (hwSpec.systemId) {
                "snes" -> drawSnesConsoleBack(frame)
                "n64" -> drawN64ConsoleBack(frame)
                else -> drawGenesisConsoleBack(frame)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(slotLipYDp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val startY = 6f
            val endY = 122f
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
                    .width(192.dp)
                    .height(172.dp)
                    .offset(y = yDp.dp),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            when (hwSpec.systemId) {
                "snes" -> drawSnesConsoleFront(frame)
                "n64" -> drawN64ConsoleFront(frame)
                else -> drawGenesisConsoleFront(frame)
            }
        }
    }
}

// =================================================================================================
// 3. Real Handheld Top-Rear Slot: Nintendo DS Lite, 3DS, Game Boy DMG, GBC, Game Boy Advance
// =================================================================================================

@Composable
private fun HandheldRearSlotInsertionStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val id = hwSpec.systemId
    val isDsFamily = id == "nds" || id == "3ds"
    val isGba = id == "gba"

    val clipHeightDp = when {
        isDsFamily -> 193.dp
        isGba -> 195.dp
        else -> 238.dp // DMG / GBC open-backed recessed rear pocket
    }
    val cartW = when {
        isDsFamily -> 116.dp
        isGba -> 150.dp
        else -> 144.dp
    }
    val cartH = when {
        isDsFamily -> 116.dp
        isGba -> 104.dp
        else -> 148.dp
    }
    val startY = when {
        isDsFamily -> 8f
        isGba -> 16f
        else -> 12f
    }
    val endY = when {
        isDsFamily -> 176f // Top edge rests cleanly flush in Slot-1 (spring-clicks down +5.5dp & rebounds!)
        isGba -> 160f
        else -> 148f // Flush with top of Game Boy / GBC rear bay
    }
    val springExtraY = if (isDsFamily) frame.latchMotion * 5.5f else 0f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            when {
                isDsFamily -> drawNdsOr3dsConsoleBack(hwSpec, frame)
                isGba -> drawGbaConsoleBack(hwSpec, frame)
                else -> drawGameBoyConsoleBack(hwSpec, frame)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(clipHeightDp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val yDp = startY + (endY - startY) * frame.insertDepth + springExtraY
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = spec.mediaKind.defaultAspect,
                yawDeg = frame.mediaYawDeg,
                pitchDeg = frame.mediaPitchDeg,
                floorShadow = false,
                modifier = Modifier
                    .width(cartW)
                    .height(cartH)
                    .offset(y = yDp.dp),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            when {
                isDsFamily -> drawNdsOr3dsConsoleFront(hwSpec, frame)
                isGba -> drawGbaConsoleFront(hwSpec, frame)
                else -> drawGameBoyConsoleFront(hwSpec, frame)
            }
        }
    }
}

// =================================================================================================
// 4. Nintendo Switch: Top-Right Hinged Game Card Flap flanked by Neon Blue & Red Joy-Cons
// =================================================================================================

@Composable
private fun SwitchTopFlapInsertionStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawSwitchConsoleBack(frame) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(198.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val yDp = 24f + (186f - 24f) * frame.insertDepth + frame.latchMotion * 4.5f
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.GAME_MEDIA,
                spec = spec,
                textures = textures,
                boxAspect = spec.mediaKind.defaultAspect,
                yawDeg = frame.mediaYawDeg,
                pitchDeg = frame.mediaPitchDeg,
                floorShadow = false,
                modifier = Modifier
                    .width(68.dp)
                    .height(86.dp)
                    .offset(x = 58.dp, y = yDp.dp),
            )
        }

        Canvas(Modifier.fillMaxSize()) { drawSwitchConsoleFront(frame) }
    }
}

// =================================================================================================
// 5. Top-Lid Optical Disc Consoles: PS1, GameCube (8cm Mini-DVD!), Dreamcast, Saturn
// =================================================================================================

@Composable
private fun DiscTopLidInsertionStage(
    spec: PhysicalMediaSpec,
    hwSpec: ConsoleHardwareSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val isGameCube = hwSpec.systemId == "gc" || hwSpec.systemId == "gamecube"
    val isDreamcast = hwSpec.systemId == "dc" || hwSpec.systemId == "dreamcast"

    val discSizeDp = when {
        isGameCube -> 178f // Authentic 8cm Nintendo GameCube Mini-DVD filling the compact GameCube bay!
        isDreamcast -> 218f
        else -> 226f
    }
    val bayCyDp = when {
        isGameCube -> 360f * 0.50f
        isDreamcast -> 360f * 0.60f
        else -> 360f * 0.61f
    }
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
// 6. PlayStation 2: Motorized Front Disc Tray extending out & retracting straight into the Monolith!
// =================================================================================================

@Composable
private fun Ps2SlideTrayInsertionStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val discSizeDp = 194f
    // Extended tray oval center tracks directly with drawPs2ConsoleBack's trayTipY!
    val trayBackY = 360f * 0.47f + 14f
    val trayTipY = 360f * 0.47f + 28f + frame.trayOut * 84f
    val trayCenterY = trayBackY + (trayTipY - trayBackY) * 0.54f
    val seatedTopY = trayCenterY - discSizeDp * 0.5f
    val yDp = 8f + (seatedTopY - 8f) * frame.insertDepth
    val foreshortenY = (0.80f - frame.insertDepth * 0.34f).coerceIn(0.42f, 0.88f)
    val discAlpha = (frame.trayOut * 1.4f).coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawPs2ConsoleBack(frame) }

        // Once the disc rests on the tray, clip strictly below the PS2 drive mouth (172dp) so it
        // physically disappears INSIDE the monolith as the motorized tray retracts!
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
// 7. PlayStation Portable (PSP): Rear Hinged Chrome-Ring UMD Clamshell Hatch!
// =================================================================================================

@Composable
private fun PspRearUmdInsertionStage(
    spec: PhysicalMediaSpec,
    textures: ResolvedPhysicalTextures,
    frame: ConsoleInsertionFrame,
    modifier: Modifier = Modifier,
) {
    val umdSizeDp = 148f
    val bayCyDp = 360f * 0.67f
    val startY = 14f
    val endY = bayCyDp - umdSizeDp * 0.5f
    val yDp = startY + (endY - startY) * frame.insertDepth
    val umdAlpha = if (frame.lidOpen < 0.35f) (frame.lidOpen / 0.35f).coerceIn(0f, 1f) else 1f

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawPspConsoleBack(frame) }

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

        Canvas(Modifier.fillMaxSize()) { drawPspConsoleFront(frame) }
    }
}

// =================================================================================================
// 8. Nintendo Wii / Wii U: Upright Glossy Tower with Horizontal Feed into Vertical Electric-Blue Slot!
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
    // Glides horizontally from right (+96dp) straight LEFT into the vertical glowing blue slot (-66dp)!
    val offsetX = 96f + (-162f) * frame.insertDepth
    val discAlpha = (1f - ((frame.insertDepth - 0.85f) / 0.15f).coerceIn(0f, 1f))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawWiiConsoleBack(hwSpec, frame) }

        // Clip cleanly at the vertical glowing blue slot (cx - 21dp) so the disc feeds DIRECTLY into the blue slit!
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
