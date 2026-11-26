package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbilightTest {

    @Test
    fun modeFromIdResolvesPresetsAndDefaultsToVivid() {
        assertEquals(AmbilightMode.OFF, AmbilightMode.fromId("off"))
        assertEquals(AmbilightMode.SUBTLE, AmbilightMode.fromId("SUBTLE"))
        assertEquals(AmbilightMode.VIVID, AmbilightMode.fromId("vivid"))
        assertEquals(AmbilightMode.HYPER, AmbilightMode.fromId("hyper"))
        assertEquals(AmbilightMode.VIVID, AmbilightMode.fromId(null))
        assertEquals(AmbilightMode.VIVID, AmbilightMode.fromId("unknown"))

        assertFalse(AmbilightMode.OFF.isEnabled)
        assertTrue(AmbilightMode.SUBTLE.isEnabled)
        assertTrue(AmbilightMode.VIVID.isEnabled)
        assertTrue(AmbilightMode.HYPER.isEnabled)
        assertTrue(AmbilightMode.HYPER.multiplier > AmbilightMode.VIVID.multiplier)
        assertTrue(AmbilightMode.VIVID.multiplier > AmbilightMode.SUBTLE.multiplier)
    }

    @Test
    fun pitchBlackEdgesSuppressOledGlowWhileLitEdgesGlow() {
        val blackState = AmbilightState(
            enabled = true,
            top = Color.Black,
            bottom = Color.Black,
            left = Color.Black,
            right = Color.Black,
            flash = 0f,
            intensity = AmbilightMode.VIVID.multiplier,
        )
        assertFalse("Pitch-black fade-outs should leave OLED unlit", blackState.isVisiblyGlowing)
        assertEquals(0f, blackState.maxEnergy, 0.001f)

        val litState = blackState.copy(bottom = Color(0xFFFF5500))
        assertTrue("Active colored edge should trigger Ambilight halo", litState.isVisiblyGlowing)
        assertEquals(1f, litState.maxEnergy, 0.001f)

        val flashOnly = blackState.copy(flash = 0.50f)
        assertTrue("Transient lightning/explosion flash should trigger halo", flashOnly.isVisiblyGlowing)
    }

    @Test
    fun trackerLatchesInitialFrameAndUsesFastAttackOnExplosionSpikes() {
        val tracker = AmbilightTracker()

        // Frame 0: dim cavern blue across all edges
        val dimBlue = 0xFF102040.toInt()
        val s0 = tracker.step(intArrayOf(dimBlue, dimBlue, dimBlue, dimBlue, 0), AmbilightMode.VIVID)
        assertTrue(s0.enabled)
        assertEquals((0x40 / 255f), s0.top.blue, 0.005f)
        assertEquals(0f, s0.flash, 0.001f)

        // Frame 1: sudden fiery orange explosion at bottom edge + flash spike (200/255)
        val fireOrange = 0xFFFF6600.toInt()
        val s1 = tracker.step(intArrayOf(dimBlue, fireOrange, dimBlue, dimBlue, 200), AmbilightMode.VIVID)
        // Fast attack (alpha = 0.46f) should jump bottom red well past steady-decay level
        val expectedFastRed = (0x10 / 255f) + ((1f - (0x10 / 255f)) * 0.46f)
        assertEquals(expectedFastRed, s1.bottom.red, 0.01f)
        assertTrue("Explosion flash should rise immediately", s1.flash > 0.45f)

        // Steady subsequent frames without flash should smoothly decay flash toward zero
        var current = s1
        repeat(12) {
            current = tracker.step(intArrayOf(dimBlue, dimBlue, dimBlue, dimBlue, 0), AmbilightMode.VIVID)
        }
        assertTrue("Flash energy should decay smoothly back near 0", current.flash < 0.08f)
    }

    @Test
    fun trackerOffModeAndResetReturnCleanState() {
        val tracker = AmbilightTracker()
        val neonCyan = 0xFF00E5FF.toInt()
        val active = tracker.step(intArrayOf(neonCyan, neonCyan, neonCyan, neonCyan, 128), AmbilightMode.HYPER)
        assertTrue(active.isVisiblyGlowing)
        assertEquals(AmbilightMode.HYPER.multiplier, active.intensity, 0.001f)

        val off = tracker.step(intArrayOf(neonCyan, neonCyan, neonCyan, neonCyan, 128), AmbilightMode.OFF)
        assertFalse(off.enabled)
        assertFalse(off.isVisiblyGlowing)

        tracker.reset()
        val freshGreen = 0xFF00FF66.toInt()
        val postReset = tracker.step(intArrayOf(freshGreen, freshGreen, freshGreen, freshGreen, 0), AmbilightMode.SUBTLE)
        assertEquals(0f, postReset.top.red, 0.005f)
        assertEquals(1f, postReset.top.green, 0.005f)
        assertEquals(0x66 / 255f, postReset.top.blue, 0.005f)
    }
}
