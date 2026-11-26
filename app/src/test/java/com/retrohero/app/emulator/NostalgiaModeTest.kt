package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NostalgiaModeTest {

    @Test
    fun consoleFamilyClassificationSeparatesHandheldDiscAndCrt() {
        assertTrue(NostalgiaMath.isHandheld("gba"))
        assertTrue(NostalgiaMath.isHandheld("nds"))
        assertFalse(NostalgiaMath.isCrtConsole("gb"))

        assertTrue(NostalgiaMath.isCrtConsole("snes"))
        assertTrue(NostalgiaMath.isCrtConsole("psx"))
        assertTrue(NostalgiaMath.isDiscConsole("psx"))
        assertFalse(NostalgiaMath.isDiscConsole("snes"))
    }

    @Test
    fun batteryLedDimmingShiftsAmberThenFlickersRedAtLowBattery() {
        val healthy = Color(0xFF00E676)
        assertEquals(healthy, NostalgiaMath.batteryLedColor(80, healthy))
        assertEquals(healthy, NostalgiaMath.batteryLedColor(31, healthy))

        val amber = NostalgiaMath.batteryLedColor(22, healthy)
        assertNotEquals(healthy, amber)
        assertTrue(amber.red > 0.9f && amber.green > 0.4f)

        val lowBright = NostalgiaMath.batteryLedColor(10, healthy, pulsePhase = 0.25f)
        val lowDim = NostalgiaMath.batteryLedColor(10, healthy, pulsePhase = 0.75f)
        assertTrue(lowBright.red > lowDim.red)
    }

    @Test
    fun crtWarmupCurvesStartThinAndBloomToFullScreen() {
        val h0 = NostalgiaMath.warmupBeamHeightFraction(0f)
        val hMid = NostalgiaMath.warmupBeamHeightFraction(0.5f)
        val hEnd = NostalgiaMath.warmupBeamHeightFraction(1f)
        assertTrue(h0 in 0.01f..0.05f)
        assertTrue(hMid > h0 && hMid < hEnd)
        assertEquals(1f, hEnd, 1e-4f)

        assertEquals(0f, NostalgiaMath.warmupGlowAlpha(0f), 1e-4f)
        assertTrue(NostalgiaMath.warmupGlowAlpha(0.25f) > 0.8f)
        assertEquals(0f, NostalgiaMath.warmupGlowAlpha(1f), 1e-4f)
    }

    @Test
    fun proceduralAudioSynthesizerProducesNonSilentValidPcm() {
        val click = NostalgiaAudioSynth.synthesizeCartridgeClick()
        assertTrue(click.size > 1000)
        assertTrue(click.maxOf { abs(it.toInt()) } > 5000)

        val gbChime = NostalgiaAudioSynth.synthesizeBootChime("gba")
        val psxChime = NostalgiaAudioSynth.synthesizeBootChime("psx")
        val snesChime = NostalgiaAudioSynth.synthesizeBootChime("snes")
        assertTrue(psxChime.size > gbChime.size)
        assertTrue(gbChime.maxOf { abs(it.toInt()) } > 3000)
        assertTrue(psxChime.maxOf { abs(it.toInt()) } > 3000)
        assertTrue(snesChime.maxOf { abs(it.toInt()) } > 3000)

        val blow = NostalgiaAudioSynth.synthesizeCartridgeBlow()
        assertTrue(blow.size > click.size + 5000)
        assertTrue(blow.maxOf { abs(it.toInt()) } > 3000)
    }
}
