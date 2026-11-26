package com.retrohero.app.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySyncTest {

    @Test
    fun locksNesGbaSnesToExactIntegerRefreshAvoiding90HzJudder() {
        val ltpoModes = listOf(10f, 24f, 30f, 60f, 90f, 120f)

        // NES (60.0988 Hz) -> picks 60 Hz (1x exact lock, skew ~0.9983) and skips 90 Hz
        val nes = DisplaySync.chooseTargetRefreshRate(60.0988, ltpoModes, bfiRequested = false)
        assertEquals(60f, nes.targetRefreshHz, 0.01f)
        assertEquals(1, nes.divider)
        assertTrue(nes.exactLock)
        assertTrue(nes.bfiCapable)
        assertTrue(nes.skewRatio in 0.995..1.005)

        // GBA (59.7275 Hz) with 120 Hz OLED BFI ON -> locks to 120 Hz (2x exact lock)
        val gbaBfi = DisplaySync.chooseTargetRefreshRate(59.7275, ltpoModes, bfiRequested = true)
        assertEquals(120f, gbaBfi.targetRefreshHz, 0.01f)
        assertEquals(2, gbaBfi.divider)
        assertTrue(gbaBfi.exactLock)
        assertTrue(gbaBfi.bfiCapable)
        assertTrue(gbaBfi.skewRatio in 0.995..1.006)
    }

    @Test
    fun locks120HzOnlyLtpoScreenAt2xDividerAndHandles90HzOnlyGracefully() {
        // 120 Hz-only panel: SNES 59.94 Hz locks cleanly at 120 Hz with divider 2
        val snes120 = DisplaySync.chooseTargetRefreshRate(59.94, listOf(120f), bfiRequested = false)
        assertEquals(120f, snes120.targetRefreshHz, 0.01f)
        assertEquals(2, snes120.divider)
        assertTrue(snes120.exactLock)
        assertTrue(snes120.bfiCapable)

        // 90 Hz-only panel: 60 Hz game does NOT force a broken integer lock (exactLock = false)
        val ninetyOnly = DisplaySync.chooseTargetRefreshRate(60.0, listOf(90f), bfiRequested = true)
        assertFalse(ninetyOnly.exactLock)
        assertFalse(ninetyOnly.bfiCapable)

        // 60 Hz-only panel: BFI is cleanly marked unavailable (never flickers at 30 Hz!)
        val sixtyOnly = DisplaySync.chooseTargetRefreshRate(60.0, listOf(60f), bfiRequested = true)
        assertEquals(60f, sixtyOnly.targetRefreshHz, 0.01f)
        assertEquals(1, sixtyOnly.divider)
        assertTrue(sixtyOnly.exactLock)
        assertFalse(sixtyOnly.bfiCapable)
    }

    @Test
    fun supportsPal50HzOn100HzDisplays() {
        val pal = DisplaySync.chooseTargetRefreshRate(50.0, listOf(60f, 100f, 120f), bfiRequested = false)
        assertEquals(100f, pal.targetRefreshHz, 0.01f)
        assertEquals(2, pal.divider)
        assertTrue(pal.exactLock)
        assertTrue(pal.bfiCapable)
    }
}
