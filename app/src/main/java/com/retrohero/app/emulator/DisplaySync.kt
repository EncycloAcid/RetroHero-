package com.retrohero.app.emulator

import android.app.Activity
import android.os.Build
import android.view.Surface
import android.view.Window
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hardware display refresh-rate lock planner for 60 / 90 / 120 Hz / LTPO Android panels.
 *
 * Selects exact integer multiples of the console's native refresh rate (e.g., 60.0988 Hz NES,
 * 59.7275 Hz GBA, 59.94 Hz SNES, 50.0 Hz PAL) so the surface locks cleanly at 60 Hz or 120 Hz
 * instead of juddering at 90 Hz or dropping to 10–30 Hz on LTPO displays.
 */
object DisplaySync {

    data class SyncPlan(
        val targetRefreshHz: Float,
        val divider: Int,
        val exactLock: Boolean,
        val bfiCapable: Boolean,
        val skewRatio: Double,
    )

    private const val MAX_LOCK_SKEW = 0.035 // 3.5% tolerance covers 59.73–60.10 Hz on 60/120 Hz panels

    /**
     * Computes the optimal display refresh rate for [coreFps] from the screen's [supportedModesHz].
     * When [bfiRequested] is true, strongly prefers a 2× integer mode (e.g., 120 Hz for 60 Hz games)
     * so OLED Black Frame Insertion can alternate real & black frames cleanly.
     */
    fun chooseTargetRefreshRate(
        coreFps: Double,
        supportedModesHz: List<Float>,
        bfiRequested: Boolean = false,
    ): SyncPlan {
        val safeCore = if (coreFps in 25.0..120.0) coreFps else 60.0
        val modes = supportedModesHz
            .filter { it in 45f..245f }
            .distinctBy { (it * 10f).roundToInt() }
            .sorted()
            .ifEmpty { listOf(60f) }

        data class Candidate(val hz: Float, val div: Int, val skew: Double) {
            val err: Double get() = abs(skew - 1.0)
        }

        val exactMatches = modes.mapNotNull { hz ->
            val div = (hz / safeCore).roundToInt().coerceIn(1, 4)
            val eff = hz.toDouble() / div.toDouble()
            val skew = eff / safeCore
            if (abs(skew - 1.0) <= MAX_LOCK_SKEW) Candidate(hz, div, skew) else null
        }

        val bfiCandidate = exactMatches
            .filter { it.div >= 2 }
            .minWithOrNull(compareBy<Candidate> { it.div }.thenBy { it.err })

        val bfiCapable = bfiCandidate != null

        val chosen = when {
            bfiRequested && bfiCandidate != null -> bfiCandidate
            exactMatches.isNotEmpty() -> {
                // Prefer exact 1× (60 Hz) for lowest power / heat when available; otherwise 2× (120 Hz)
                exactMatches.minWithOrNull(compareBy<Candidate> { it.div }.thenBy { it.err })!!
            }
            else -> {
                // No integer multiple available (e.g. 50 Hz PAL on a 60 Hz-only screen): pick closest/highest
                val fallbackHz = modes.maxOrNull() ?: 60f
                return SyncPlan(
                    targetRefreshHz = fallbackHz,
                    divider = 1,
                    exactLock = false,
                    bfiCapable = false,
                    skewRatio = 1.0,
                )
            }
        }

        return SyncPlan(
            targetRefreshHz = chosen.hz,
            divider = chosen.div,
            exactLock = true,
            bfiCapable = bfiCapable,
            skewRatio = chosen.skew.coerceIn(0.985, 1.015),
        )
    }

    /** Queries every refresh rate supported by the activity's current display. */
    fun supportedRefreshRates(activity: Activity): List<Float> = runCatching {
        val display = if (Build.VERSION.SDK_INT >= 30) activity.display else @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
        val modes = display?.supportedModes?.map { it.refreshRate }?.filter { it in 45f..245f }.orEmpty()
        if (modes.isNotEmpty()) modes else listOf(display?.refreshRate?.takeIf { it in 45f..245f } ?: 60f)
    }.getOrDefault(listOf(60f))

    /** Current active display refresh rate in Hz. */
    fun currentRefreshRate(activity: Activity): Float = runCatching {
        val display = if (Build.VERSION.SDK_INT >= 30) activity.display else @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
        display?.refreshRate?.takeIf { it in 45f..245f } ?: 60f
    }.getOrDefault(60f)

    /**
     * Requests a hardware refresh-rate lock on both the Activity [Window] (preventing LTPO drop-downs)
     * and the game [Surface] (`Surface.setFrameRate` on API 30+).
     */
    fun applyHardwareLock(surface: Surface?, window: Window?, targetHz: Float) {
        if (targetHz < 45f || targetHz > 245f) return
        if (window != null) {
            runCatching {
                val lp = window.attributes
                if (abs(lp.preferredRefreshRate - targetHz) > 0.25f) {
                    lp.preferredRefreshRate = targetHz
                    window.attributes = lp
                }
            }
        }
        if (surface != null && surface.isValid) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 31) {
                    surface.setFrameRate(
                        targetHz,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                        Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS,
                    )
                } else if (Build.VERSION.SDK_INT >= 30) {
                    surface.setFrameRate(
                        targetHz,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    )
                }
            }
        }
    }
}
