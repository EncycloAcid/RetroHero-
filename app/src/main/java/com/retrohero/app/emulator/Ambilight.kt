package com.retrohero.app.emulator

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.max

/** User-selectable intensity presets for Live Framebuffer OLED Ambilight Bezel Glow. */
enum class AmbilightMode(val id: String, val label: String, val multiplier: Float, val description: String) {
    OFF("off", "Off", 0f, "Static console bezel with zero ambient halo"),
    SUBTLE("subtle", "Subtle", 0.55f, "Gentle ambient tint along bezel & grip edges"),
    VIVID("vivid", "Vivid", 0.90f, "Balanced OLED Ambilight halo + explosion flare"),
    HYPER("hyper", "Hyper", 1.25f, "Deep full-shell cinema glow & dramatic flash bursts");

    val isEnabled: Boolean get() = multiplier > 0f

    companion object {
        fun fromId(id: String?): AmbilightMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: VIVID
    }
}

/**
 * Immutable snapshot of the 4 directional framebuffer edge colors + transient flash energy
 * rendered onto RetroHero's portrait bezels, controller decks, and landscape side grips.
 */
@Immutable
data class AmbilightState(
    val enabled: Boolean = false,
    val top: Color = Color.Black,
    val bottom: Color = Color.Black,
    val left: Color = Color.Black,
    val right: Color = Color.Black,
    /** Normalized [0f..1f] sudden luminance jump from explosions, lightning, or spell flashes. */
    val flash: Float = 0f,
    /** Mode intensity multiplier (0.55 Subtle, 0.90 Vivid, 1.25 Hyper). */
    val intensity: Float = 0.90f,
) {
    /** Overall dominant accent blended across all 4 framebuffer edges (for bezel rim reflection). */
    val rimColor: Color
        get() = Color(
            red = ((top.red + bottom.red + left.red + right.red) * 0.25f).coerceIn(0f, 1f),
            green = ((top.green + bottom.green + left.green + right.green) * 0.25f).coerceIn(0f, 1f),
            blue = ((top.blue + bottom.blue + left.blue + right.blue) * 0.25f).coerceIn(0f, 1f),
            alpha = 1f,
        )

    /** Peak brightness across all 4 edges [0f..1f]; 0 on pitch-black fade-outs so OLED stays black. */
    val maxEnergy: Float
        get() = maxOf(edgeEnergy(top), edgeEnergy(bottom), edgeEnergy(left), edgeEnergy(right))

    val isVisiblyGlowing: Boolean
        get() = enabled && intensity > 0f && (maxEnergy > 0.03f || flash > 0.04f)

    companion object {
        val Off = AmbilightState(enabled = false)

        fun edgeEnergy(c: Color): Float = maxOf(c.red, c.green, c.blue).coerceIn(0f, 1f)

        fun argbToColor(argb: Int): Color {
            val r = ((argb ushr 16) and 0xFF) / 255f
            val g = ((argb ushr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            return Color(r, g, b, 1f)
        }
    }
}

/**
 * Zero-allocation temporal smoother for C++ framebuffer Ambilight samples.
 *
 * Uses asymmetric exponential smoothing:
 * - Fast attack (`0.46f`) when brightness spikes so explosions, lightning, and gunfire react immediately.
 * - Silky decay (`0.18f`) during steady play so 60 Hz sprite flicker never strobes the bezel.
 */
class AmbilightTracker {
    val rawBuffer = IntArray(5)

    private var initialized = false
    private var topR = 0f; private var topG = 0f; private var topB = 0f
    private var botR = 0f; private var botG = 0f; private var botB = 0f
    private var leftR = 0f; private var leftG = 0f; private var leftB = 0f
    private var rightR = 0f; private var rightG = 0f; private var rightB = 0f
    private var smoothFlash = 0f

    fun reset() {
        initialized = false
        topR = 0f; topG = 0f; topB = 0f
        botR = 0f; botG = 0f; botB = 0f
        leftR = 0f; leftG = 0f; leftB = 0f
        rightR = 0f; rightG = 0f; rightB = 0f
        smoothFlash = 0f
    }

    /** Steps the smoother from `[topArgb, bottomArgb, leftArgb, rightArgb, flash0to255]` and returns [AmbilightState]. */
    fun step(raw: IntArray, mode: AmbilightMode): AmbilightState {
        if (!mode.isEnabled || raw.size < 5) {
            return AmbilightState.Off
        }
        val tR = ((raw[0] ushr 16) and 0xFF) / 255f
        val tG = ((raw[0] ushr 8) and 0xFF) / 255f
        val tB = (raw[0] and 0xFF) / 255f

        val bR = ((raw[1] ushr 16) and 0xFF) / 255f
        val bG = ((raw[1] ushr 8) and 0xFF) / 255f
        val bB = (raw[1] and 0xFF) / 255f

        val lR = ((raw[2] ushr 16) and 0xFF) / 255f
        val lG = ((raw[2] ushr 8) and 0xFF) / 255f
        val lB = (raw[2] and 0xFF) / 255f

        val rR = ((raw[3] ushr 16) and 0xFF) / 255f
        val rG = ((raw[3] ushr 8) and 0xFF) / 255f
        val rB = (raw[3] and 0xFF) / 255f

        val rawFlash = (raw[4].coerceIn(0, 255)) / 255f

        if (!initialized) {
            topR = tR; topG = tG; topB = tB
            botR = bR; botG = bG; botB = bB
            leftR = lR; leftG = lG; leftB = lB
            rightR = rR; rightG = rG; rightB = rB
            smoothFlash = rawFlash
            initialized = true
        } else {
            val prevPeak = maxOf(topR, topG, topB, botR, botG, botB, leftR, leftG, leftB, rightR, rightG, rightB)
            val nextPeak = maxOf(tR, tG, tB, bR, bG, bB, lR, lG, lB, rR, rG, rB)
            // Fast attack on flashes & bright transitions; smooth cinema decay otherwise
            val alpha = if (rawFlash > 0.20f || nextPeak > prevPeak + 0.12f) 0.46f else 0.18f

            topR += (tR - topR) * alpha
            topG += (tG - topG) * alpha
            topB += (tB - topB) * alpha

            botR += (bR - botR) * alpha
            botG += (bG - botG) * alpha
            botB += (bB - botB) * alpha

            leftR += (lR - leftR) * alpha
            leftG += (lG - leftG) * alpha
            leftB += (lB - leftB) * alpha

            rightR += (rR - rightR) * alpha
            rightG += (rG - rightG) * alpha
            rightB += (rB - rightB) * alpha

            smoothFlash = if (rawFlash > smoothFlash) {
                smoothFlash + (rawFlash - smoothFlash) * 0.65f
            } else {
                max(0f, smoothFlash * 0.80f - 0.01f)
            }
        }

        return AmbilightState(
            enabled = true,
            top = Color(topR.coerceIn(0f, 1f), topG.coerceIn(0f, 1f), topB.coerceIn(0f, 1f), 1f),
            bottom = Color(botR.coerceIn(0f, 1f), botG.coerceIn(0f, 1f), botB.coerceIn(0f, 1f), 1f),
            left = Color(leftR.coerceIn(0f, 1f), leftG.coerceIn(0f, 1f), leftB.coerceIn(0f, 1f), 1f),
            right = Color(rightR.coerceIn(0f, 1f), rightG.coerceIn(0f, 1f), rightB.coerceIn(0f, 1f), 1f),
            flash = smoothFlash.coerceIn(0f, 1f),
            intensity = mode.multiplier,
        )
    }
}
