package com.retrohero.app.emulator

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.pow
import kotlin.math.roundToInt

/** A single tactile impulse computed from game audio energy. */
data class HapticPulse(
    val durationMs: Long,
    val amplitude: Int,
    /** Normalized [0..1] scale for API 31+ HD haptic primitives. */
    val scale: Float,
)

/** Pure mapping from audio transient energy [0..1] to crisp tactile impulses. */
object AudioHapticsMapper {
    const val MIN_ENERGY = 0.14f
    const val COOLDOWN_MS = 45L

    fun computePulse(
        energy: Float,
        strength: Float,
        elapsedSinceLastMs: Long,
    ): HapticPulse? {
        if (elapsedSinceLastMs < COOLDOWN_MS) return null
        val clampedStrength = strength.coerceIn(0f, 1f)
        if (clampedStrength <= 0.01f) return null
        val clamped = energy.coerceIn(0f, 1f)
        if (clamped < MIN_ENERGY) return null

        // Normalize above threshold and apply a mild perceptual curve so light hits feel subtle
        // while big explosions punch clearly.
        val norm = ((clamped - MIN_ENERGY) / (1f - MIN_ENERGY)).coerceIn(0f, 1f)
        val curved = norm.toDouble().pow(1.15).toFloat()
        val scaled = (curved * clampedStrength).coerceIn(0f, 1f)
        if (scaled < 0.05f) return null

        val durationMs = (14L + (scaled * 32f).roundToInt()).coerceIn(14L, 48L)
        val amplitude = (32 + (scaled * 223f).roundToInt()).coerceIn(1, 255)
        return HapticPulse(durationMs = durationMs, amplitude = amplitude, scale = scaled)
    }
}

/** Drives Android's vibrator (with HD primitive composition on API 31+) from game audio transients. */
class AudioHapticsEngine(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()?.takeIf { it.hasVibrator() }

    private val supportsThud: Boolean = Build.VERSION.SDK_INT >= 31 && vibrator?.let {
        runCatching { it.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD) }.getOrDefault(false)
    } == true

    private var lastTriggerAt = 0L

    fun trigger(energy: Float, strength: Float) {
        val v = vibrator ?: return
        val now = SystemClock.elapsedRealtime()
        val pulse = AudioHapticsMapper.computePulse(energy, strength, now - lastTriggerAt) ?: return
        lastTriggerAt = now
        val effect = if (supportsThud && Build.VERSION.SDK_INT >= 31) {
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, pulse.scale.coerceIn(0.15f, 1f))
                .compose()
        } else {
            VibrationEffect.createOneShot(pulse.durationMs, pulse.amplitude)
        }
        runCatching { v.vibrate(effect) }
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
    }
}
