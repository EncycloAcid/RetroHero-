package com.retrohero.app.emulator

import androidx.compose.ui.geometry.Offset
import com.retrohero.app.data.PadButton
import com.retrohero.app.data.PadLayout
import com.retrohero.app.data.RetroPad
import com.retrohero.app.data.Systems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTouchTest {

    @Test
    fun thumbRollChordZoneHoldsBothAdjacentFaceButtonsSmoothly() {
        val btnY = SmartTouch.FaceHitCircle(RetroPad.Y, Offset(100f, 200f), 40f)
        val btnB = SmartTouch.FaceHitCircle(RetroPad.B, Offset(180f, 250f), 40f)
        val btnA = SmartTouch.FaceHitCircle(RetroPad.A, Offset(260f, 200f), 40f)
        val all = listOf(btnY, btnB, btnA)

        // Direct center hits single button only
        assertEquals(setOf(RetroPad.Y), SmartTouch.resolveFaceChord(Offset(100f, 200f), all, chordEnabled = true, slopPx = 10f))
        assertEquals(setOf(RetroPad.B), SmartTouch.resolveFaceChord(Offset(180f, 250f), all, chordEnabled = true, slopPx = 10f))

        // Midpoint bridge between Y and B activates BOTH simultaneously when chord is enabled
        val midpointYB = Offset(140f, 225f)
        assertEquals(
            setOf(RetroPad.Y, RetroPad.B),
            SmartTouch.resolveFaceChord(midpointYB, all, chordEnabled = true, slopPx = 8f),
        )

        // Midpoint bridge between B and A activates BOTH simultaneously
        val midpointBA = Offset(220f, 225f)
        assertEquals(
            setOf(RetroPad.B, RetroPad.A),
            SmartTouch.resolveFaceChord(midpointBA, all, chordEnabled = true, slopPx = 8f),
        )
    }

    @Test
    fun generatesTailoredComboMacrosForDiamondAnd2ButtonConsoles() {
        val snesMacros = SmartTouch.availableMacrosFor(Systems.get("snes")!!.layout)
        assertTrue(snesMacros.any { it.label == "Y+B" && it.buttonIds == listOf(RetroPad.Y, RetroPad.B) })
        assertTrue(snesMacros.any { it.label == "R+B" && it.buttonIds == listOf(RetroPad.R, RetroPad.B) })
        assertTrue(snesMacros.any { it.label == "L+R" && it.buttonIds == listOf(RetroPad.L, RetroPad.R) })

        val nesMacros = SmartTouch.availableMacrosFor(Systems.get("nes")!!.layout)
        assertEquals(1, nesMacros.size)
        assertEquals("B+A", nesMacros[0].label)
        assertEquals(listOf(RetroPad.B, RetroPad.A), nesMacros[0].buttonIds)

        val gbaMacros = SmartTouch.availableMacrosFor(Systems.get("gba")!!.layout)
        assertTrue(gbaMacros.any { it.label == "B+A" })
        assertTrue(gbaMacros.any { it.label == "R+A" })
    }

    @Test
    fun autoSwitchesBetweenPortraitOneHandedRpgAndLandscapeActionMode() {
        // Pokémon / Chrono Trigger in Portrait AUTO -> One-Handed RPG Mode!
        assertTrue(SmartTouch.shouldUseOneHandedPortrait(SmartTouch.PORTRAIT_AUTO, landscape = false, gameTitle = "Pokemon Emerald"))
        assertTrue(SmartTouch.shouldUseOneHandedPortrait(SmartTouch.PORTRAIT_AUTO, landscape = false, gameTitle = "Chrono Trigger"))
        // Rotate to Landscape -> ALWAYS switches to Landscape Action Mode!
        assertFalse(SmartTouch.shouldUseOneHandedPortrait(SmartTouch.PORTRAIT_AUTO, landscape = true, gameTitle = "Pokemon Emerald"))
        assertFalse(SmartTouch.shouldUseOneHandedPortrait(SmartTouch.PORTRAIT_ONE_HAND, landscape = true, gameTitle = "Super Metroid"))
        // Action games in Portrait AUTO stay standard two-handed unless user opts in
        assertFalse(SmartTouch.shouldUseOneHandedPortrait(SmartTouch.PORTRAIT_AUTO, landscape = false, gameTitle = "Super Metroid"))
        assertTrue(SmartTouch.shouldUseOneHandedPortrait(SmartTouch.PORTRAIT_ONE_HAND, landscape = false, gameTitle = "Super Metroid"))
    }

    @Test
    fun gyroSteeringCurveRespectsDeadzoneCenteringAndBounds() {
        // Inside deadzone -> exact 0
        assertEquals(0f, SmartTouch.computeGyroAxis(tiltDegrees = 2.0f, zeroOffsetDegrees = 0f), 0.0001f)
        // Centered at 10 deg holding angle -> exact 0
        assertEquals(0f, SmartTouch.computeGyroAxis(tiltDegrees = 11.5f, zeroOffsetDegrees = 10f), 0.0001f)
        // Full tilt right/left -> exact +1 / -1
        assertEquals(1f, SmartTouch.computeGyroAxis(tiltDegrees = 35f, zeroOffsetDegrees = 0f), 0.0001f)
        assertEquals(-1f, SmartTouch.computeGyroAxis(tiltDegrees = -35f, zeroOffsetDegrees = 0f), 0.0001f)
        // Half tilt is smooth and non-linear
        val half = SmartTouch.computeGyroAxis(tiltDegrees = 16f, zeroOffsetDegrees = 0f)
        assertTrue("Expected smooth positive steer ($half)", half in 0.35f..0.55f)
    }
}
