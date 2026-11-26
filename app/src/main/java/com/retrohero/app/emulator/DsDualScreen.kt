package com.retrohero.app.emulator

import com.retrohero.app.data.RetroPad

/**
 * Nintendo DS dual-screen presets surfaced in the in-game DS quick bar.
 *
 * Each preset is translated into the native screen-layout option of whichever DS core is running
 * (melonDS DS, legacy melonDS 2021, DeSmuME), so the core itself composes the frame and keeps its
 * own stylus → touch-screen mapping in sync with what is drawn.
 */
enum class DsScreenLayout(val id: String, val label: String, val shortLabel: String) {
    /** Classic DS: top screen above the touch screen. Best in portrait. */
    STACKED("stacked", "Stacked (like a real DS)", "Stacked"),
    /** One big focus screen + the other small beside it. Best in landscape. */
    HYBRID("hybrid", "Big screen + mini touch", "Hybrid"),
    /** Both screens next to each other. */
    SIDE_BY_SIDE("side", "Side by side", "Side"),
    /** Only the top (main action) screen, full size. */
    TOP_ONLY("top", "Top screen only", "Top"),
    /** Only the touch screen, full size — great for stylus-heavy games. */
    BOTTOM_ONLY("bottom", "Touch screen only", "Touch");

    fun next(): DsScreenLayout = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): DsScreenLayout? = entries.firstOrNull { it.id == id }
    }
}

/** RetroPad ids that trigger DS-only hardware (mic, hinge) on a given core. Null = unsupported. */
data class DsHardwareButtons(
    /** Hold to blow into the microphone. */
    val mic: Int?,
    /** Buttons that close the lid; [lidIsToggle] decides hold-vs-tap semantics. */
    val lid: List<Int>,
    /** True when a short press toggles the lid (melonDS DS); false when the lid stays shut while held. */
    val lidIsToggle: Boolean,
)

object DsDualScreen {
    const val SYSTEM_ID = "nds"

    const val CORE_MELONDS_DS = "melondsds"
    const val CORE_MELONDS_LEGACY = "melonds"
    const val CORE_DESMUME = "desmume"

    fun isDs(systemId: String): Boolean = systemId == SYSTEM_ID

    /** Portrait has the height for a real stacked DS; landscape is far better with one big screen. */
    fun defaultLayout(landscape: Boolean): DsScreenLayout =
        if (landscape) DsScreenLayout.HYBRID else DsScreenLayout.STACKED

    /**
     * Core options that put [coreId] into [layout] with stylus touch input enabled and the
     * microphone wired to a hold-to-blow button. [swapped] mirrors which screen is primary
     * (e.g. bottom-over-top, or a big touch screen in hybrid).
     */
    fun coreOptions(coreId: String, layout: DsScreenLayout, swapped: Boolean): Map<String, String> =
        when (coreId) {
            CORE_MELONDS_DS -> linkedMapOf(
                "melonds_number_of_screen_layouts" to "1",
                "melonds_screen_layout1" to melonDsDsLayout(layout, swapped),
                "melonds_touch_mode" to "touch",
                "melonds_cursor_mode" to "never",
                "melonds_mic_input" to "blow",
                "melonds_mic_input_active" to "hold",
                "melonds_hybrid_ratio" to "3",
            )
            CORE_MELONDS_LEGACY -> linkedMapOf(
                "melonds_screen_layout" to melonDsLegacyLayout(layout, swapped),
                "melonds_touch_mode" to "Touch",
                "melonds_mic_input" to "Blow Noise",
            )
            CORE_DESMUME -> linkedMapOf(
                "desmume_screens_layout" to desmumeLayout(layout, swapped),
                "desmume_pointer_mouse" to "enabled",
                "desmume_pointer_type" to "touch",
                "desmume_hybrid_showboth_screens" to "enabled",
                "desmume_hybrid_cursor_always_smallscreen" to "enabled",
                "desmume_mic_mode" to "pattern",
            )
            else -> emptyMap()
        }

    private fun melonDsDsLayout(layout: DsScreenLayout, swapped: Boolean): String = when (layout) {
        DsScreenLayout.STACKED -> if (swapped) "bottom-top" else "top-bottom"
        DsScreenLayout.HYBRID -> if (swapped) "hybrid-bottom" else "hybrid-top"
        DsScreenLayout.SIDE_BY_SIDE -> if (swapped) "right-left" else "left-right"
        DsScreenLayout.TOP_ONLY -> if (swapped) "bottom" else "top"
        DsScreenLayout.BOTTOM_ONLY -> if (swapped) "top" else "bottom"
    }

    private fun melonDsLegacyLayout(layout: DsScreenLayout, swapped: Boolean): String = when (layout) {
        DsScreenLayout.STACKED -> if (swapped) "Bottom/Top" else "Top/Bottom"
        DsScreenLayout.HYBRID -> if (swapped) "Hybrid Bottom" else "Hybrid Top"
        DsScreenLayout.SIDE_BY_SIDE -> if (swapped) "Right/Left" else "Left/Right"
        DsScreenLayout.TOP_ONLY -> if (swapped) "Bottom Only" else "Top Only"
        DsScreenLayout.BOTTOM_ONLY -> if (swapped) "Top Only" else "Bottom Only"
    }

    private fun desmumeLayout(layout: DsScreenLayout, swapped: Boolean): String = when (layout) {
        DsScreenLayout.STACKED -> if (swapped) "bottom/top" else "top/bottom"
        DsScreenLayout.HYBRID -> if (swapped) "hybrid/bottom" else "hybrid/top"
        DsScreenLayout.SIDE_BY_SIDE -> if (swapped) "right/left" else "left/right"
        DsScreenLayout.TOP_ONLY -> if (swapped) "bottom only" else "top only"
        DsScreenLayout.BOTTOM_ONLY -> if (swapped) "top only" else "bottom only"
    }

    /** Whether the stylus has anything to touch in this layout (touch screen visible). */
    fun touchScreenVisible(layout: DsScreenLayout, swapped: Boolean): Boolean = when (layout) {
        DsScreenLayout.TOP_ONLY -> swapped
        DsScreenLayout.BOTTOM_ONLY -> !swapped
        else -> true
    }

    /** DS-only hardware buttons, taken from each core's own input descriptors. */
    fun hardwareButtons(coreId: String): DsHardwareButtons = when (coreId) {
        // melonDS DS joypad.cpp: L3 = microphone, L2 + Y = toggle lid.
        CORE_MELONDS_DS -> DsHardwareButtons(mic = RetroPad.L3, lid = listOf(RetroPad.L2, RetroPad.Y), lidIsToggle = true)
        // DeSmuME: L2 = lid close/open (held), L3 = make microphone noise.
        CORE_DESMUME -> DsHardwareButtons(mic = RetroPad.L3, lid = listOf(RetroPad.L2), lidIsToggle = false)
        // melonDS 2021: L2 = microphone noise, L3 = close lid.
        CORE_MELONDS_LEGACY -> DsHardwareButtons(mic = RetroPad.L2, lid = listOf(RetroPad.L3), lidIsToggle = false)
        else -> DsHardwareButtons(mic = null, lid = emptyList(), lidIsToggle = false)
    }

    /**
     * Maps a touch at ([px], [py]) inside the on-screen game rectangle ([left], [top], [width], [height])
     * to libretro pointer space in [-1, 1] (the JNI bridge scales to ±0x7fff).
     *
     * [contentScale] undoes a centred zoom applied to the picture (e.g. overscan crop), so the stylus
     * lands exactly where the finger is on the visible image. Returns null when the touch is outside.
     */
    fun toPointer(
        px: Float,
        py: Float,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        contentScale: Float = 1f,
    ): Pair<Float, Float>? {
        if (width <= 0f || height <= 0f) return null
        val u = (px - left) / width
        val v = (py - top) / height
        if (u < 0f || u > 1f || v < 0f || v > 1f) return null
        val s = if (contentScale > 0f) contentScale else 1f
        val cu = 0.5f + (u - 0.5f) / s
        val cv = 0.5f + (v - 0.5f) / s
        return (cu * 2f - 1f).coerceIn(-1f, 1f) to (cv * 2f - 1f).coerceIn(-1f, 1f)
    }

    /** Same as [toPointer] but clamps to the edge — used while a stylus drag strays off the screen. */
    fun toPointerClamped(
        px: Float,
        py: Float,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        contentScale: Float = 1f,
    ): Pair<Float, Float> {
        val cx = px.coerceIn(left, left + width.coerceAtLeast(0f))
        val cy = py.coerceIn(top, top + height.coerceAtLeast(0f))
        return toPointer(cx, cy, left, top, width, height, contentScale) ?: (0f to 0f)
    }

    /**
     * Fraction of the screen width kept clear on each side of a DS picture in landscape when the
     * overlay gamepad is shown, so wide DS layouts never sit underneath the D-pad or face buttons.
     */
    fun landscapeSideInsetFraction(layout: DsScreenLayout): Float = when (layout) {
        DsScreenLayout.STACKED -> 0f // already a narrow column in the middle
        else -> 0.16f
    }

    /** Expected aspect ratio of each DS layout so the viewport snaps immediately before the next core frame. */
    fun expectedAspect(layout: DsScreenLayout): Float = when (layout) {
        DsScreenLayout.STACKED -> 256f / 384f
        DsScreenLayout.HYBRID -> 1.7778f
        DsScreenLayout.SIDE_BY_SIDE -> 512f / 192f
        DsScreenLayout.TOP_ONLY, DsScreenLayout.BOTTOM_ONLY -> 256f / 192f
    }

    /** Portrait share of the screen the gamepad gets for DS (shorter than other systems: two screens need height). */
    const val PORTRAIT_PAD_FRACTION = 0.38f
}
