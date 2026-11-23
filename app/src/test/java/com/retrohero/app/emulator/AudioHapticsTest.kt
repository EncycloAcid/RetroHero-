package com.retrohero.app.emulator

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioHapticsTest {

    @Test
    fun suppressesSilenceAndCooldown() {
        assertNull(AudioHapticsMapper.computePulse(0.05f, 1f, 100L))
        assertNull(AudioHapticsMapper.computePulse(0.9f, 0f, 100L))
        assertNull(AudioHapticsMapper.computePulse(0.9f, 1f, 20L))
    }

    @Test
    fun scalesAmplitudeAndDurationWithEnergyAndStrength() {
        val soft = AudioHapticsMapper.computePulse(0.35f, 0.5f, 100L)
        val hard = AudioHapticsMapper.computePulse(0.95f, 1.0f, 100L)
        assertNotNull(soft)
        assertNotNull(hard)
        assertTrue(hard!!.amplitude > soft!!.amplitude)
        assertTrue(hard.durationMs > soft.durationMs)
        assertTrue(hard.amplitude in 1..255)
        assertTrue(hard.durationMs in 14L..48L)
    }
}
