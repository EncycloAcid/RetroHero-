package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the measured retail case sizes the 3D cases and the Physical Shelf are built from. */
class RealCaseDimensionsTest {
    @Test
    fun jewelCaseMatchesTheStandard142x125x10() {
        val psx = PhysicalMediaSpec.forSystem("psx")
        assertEquals(142f / 125f, psx.defaultBoxAspect, 0.001f)
        assertEquals(10.4f / 142f, psx.depthRatio, 0.001f)
    }

    @Test
    fun keepCasesAndHandheldCasesUseTheirMeasuredDepth() {
        assertEquals(14f / 190f, PhysicalMediaSpec.forSystem("ps2").depthRatio, 0.001f)
        assertEquals(10f / 170f, PhysicalMediaSpec.forSystem("switch").depthRatio, 0.001f)
        assertEquals(104f / 170f, PhysicalMediaSpec.forSystem("switch").defaultBoxAspect, 0.001f)
        // Sega CD / Saturn NA cases are 144 x 206 mm, not the old 0.62 guess.
        assertEquals(144f / 206f, PhysicalMediaSpec.forSystem("saturn").defaultBoxAspect, 0.001f)
    }

    @Test
    fun wraparoundScanSpineShareFollowsTheRealBox() {
        // SNES: back 179 + spine 27 + front 179.
        assertEquals(27f / (179f * 2 + 27f), PhysicalMediaSpec.forSystem("snes").fullWrapSpineFraction, 0.0001f)
    }

    @Test
    fun relativeHeightsMatchRealCases() {
        val h = { id: String -> RealCaseDimensions.forSystem(id)!!.heightMm }
        assertTrue(h("psx") < h("snes"))      // jewel case is shorter than an SNES box
        assertTrue(h("nes") < h("genesis"))   // NES box is shorter than a Genesis clamshell
        assertTrue(h("ps2") < h("saturn"))    // Saturn's long case towers over a DVD keep case
        assertNull(RealCaseDimensions.forSystem("not-a-system"))
    }

    @Test
    fun northAmericanPs2CasesAreBlack() {
        val c = PhysicalMediaSpec.forSystem("ps2").casePlasticColor
        assertTrue(c.red < 0.15f && c.green < 0.15f && c.blue < 0.15f)
    }
}
