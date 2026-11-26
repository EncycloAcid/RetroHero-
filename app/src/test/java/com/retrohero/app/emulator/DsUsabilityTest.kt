package com.retrohero.app.emulator

import com.retrohero.app.data.RetroPad
import com.retrohero.app.data.Systems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DsUsabilityTest {

    @Test
    fun defaultsPickStackedInPortraitAndHybridInLandscape() {
        assertEquals(DsScreenLayout.STACKED, DsDualScreen.defaultLayout(landscape = false))
        assertEquals(DsScreenLayout.HYBRID, DsDualScreen.defaultLayout(landscape = true))
        for (layout in DsScreenLayout.entries) {
            assertEquals(layout, DsScreenLayout.fromId(layout.id))
        }
        assertNull(DsScreenLayout.fromId("unknown"))
        assertEquals(DsScreenLayout.HYBRID, DsScreenLayout.STACKED.next())
        assertEquals(DsScreenLayout.STACKED, DsScreenLayout.BOTTOM_ONLY.next())
    }

    @Test
    fun melonDsDsCoreOptionsMapEveryLayoutAndEnableStylusAndBlowMic() {
        val stacked = DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.STACKED, swapped = false)
        assertEquals("1", stacked["melonds_number_of_screen_layouts"])
        assertEquals("top-bottom", stacked["melonds_screen_layout1"])
        assertEquals("touch", stacked["melonds_touch_mode"])
        assertEquals("blow", stacked["melonds_mic_input"])
        assertEquals("hold", stacked["melonds_mic_input_active"])

        assertEquals(
            "bottom-top",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.STACKED, swapped = true)["melonds_screen_layout1"],
        )
        assertEquals(
            "hybrid-top",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.HYBRID, swapped = false)["melonds_screen_layout1"],
        )
        assertEquals(
            "hybrid-bottom",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.HYBRID, swapped = true)["melonds_screen_layout1"],
        )
        assertEquals(
            "left-right",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.SIDE_BY_SIDE, swapped = false)["melonds_screen_layout1"],
        )
        assertEquals(
            "right-left",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.SIDE_BY_SIDE, swapped = true)["melonds_screen_layout1"],
        )
        assertEquals(
            "top",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.TOP_ONLY, swapped = false)["melonds_screen_layout1"],
        )
        assertEquals(
            "bottom",
            DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_DS, DsScreenLayout.BOTTOM_ONLY, swapped = false)["melonds_screen_layout1"],
        )
    }

    @Test
    fun desmumeAndLegacyMelonDsCoreOptionsStayConsistent() {
        val desmumeHybrid = DsDualScreen.coreOptions(DsDualScreen.CORE_DESMUME, DsScreenLayout.HYBRID, swapped = false)
        assertEquals("hybrid/top", desmumeHybrid["desmume_screens_layout"])
        assertEquals("enabled", desmumeHybrid["desmume_pointer_mouse"])
        assertEquals("touch", desmumeHybrid["desmume_pointer_type"])
        assertEquals("pattern", desmumeHybrid["desmume_mic_mode"])

        val desmumeSwapped = DsDualScreen.coreOptions(DsDualScreen.CORE_DESMUME, DsScreenLayout.TOP_ONLY, swapped = true)
        assertEquals("bottom only", desmumeSwapped["desmume_screens_layout"])

        val legacy = DsDualScreen.coreOptions(DsDualScreen.CORE_MELONDS_LEGACY, DsScreenLayout.HYBRID, swapped = true)
        assertEquals("Hybrid Bottom", legacy["melonds_screen_layout"])
        assertEquals("Touch", legacy["melonds_touch_mode"])
        assertEquals("Blow Noise", legacy["melonds_mic_input"])
    }

    @Test
    fun hardwareButtonsMatchCoreDescriptors() {
        val melonDsDs = DsDualScreen.hardwareButtons(DsDualScreen.CORE_MELONDS_DS)
        assertEquals(RetroPad.L3, melonDsDs.mic)
        assertEquals(listOf(RetroPad.L2, RetroPad.Y), melonDsDs.lid)
        assertTrue(melonDsDs.lidIsToggle)

        val desmume = DsDualScreen.hardwareButtons(DsDualScreen.CORE_DESMUME)
        assertEquals(RetroPad.L3, desmume.mic)
        assertEquals(listOf(RetroPad.L2), desmume.lid)
        assertFalse(desmume.lidIsToggle)

        val legacy = DsDualScreen.hardwareButtons(DsDualScreen.CORE_MELONDS_LEGACY)
        assertEquals(RetroPad.L2, legacy.mic)
        assertEquals(listOf(RetroPad.L3), legacy.lid)
        assertFalse(legacy.lidIsToggle)
    }

    @Test
    fun stylusPointerMappingHandlesBoundsClampingAndOverscanZoom() {
        // Center maps to (0, 0)
        val center = DsDualScreen.toPointer(150f, 200f, left = 50f, top = 100f, width = 200f, height = 200f)
        assertNotNull(center)
        assertEquals(0f, center!!.first, 1e-4f)
        assertEquals(0f, center.second, 1e-4f)

        // Top-left & bottom-right corners map to (-1, -1) and (+1, +1)
        val tl = DsDualScreen.toPointer(50f, 100f, left = 50f, top = 100f, width = 200f, height = 200f)!!
        assertEquals(-1f, tl.first, 1e-4f)
        assertEquals(-1f, tl.second, 1e-4f)

        val br = DsDualScreen.toPointer(250f, 300f, left = 50f, top = 100f, width = 200f, height = 200f)!!
        assertEquals(1f, br.first, 1e-4f)
        assertEquals(1f, br.second, 1e-4f)

        // Outside rectangle returns null for hit-testing, clamps cleanly for active drags
        assertNull(DsDualScreen.toPointer(40f, 150f, left = 50f, top = 100f, width = 200f, height = 200f))
        val clamped = DsDualScreen.toPointerClamped(0f, 999f, left = 50f, top = 100f, width = 200f, height = 200f)
        assertEquals(-1f, clamped.first, 1e-4f)
        assertEquals(1f, clamped.second, 1e-4f)

        // Overscan zoom (1.06x) pulls edge pointer slightly inward toward the actually visible pixels
        val zoomed = DsDualScreen.toPointer(250f, 200f, left = 50f, top = 100f, width = 200f, height = 200f, contentScale = 1.06f)!!
        assertTrue(zoomed.first in 0.93f..0.95f)
        assertEquals(0f, zoomed.second, 1e-4f)
    }

    @Test
    fun ndsSystemDefinitionAndErgonomicsAreComplete() {
        val nds = Systems.all.firstOrNull { it.id == "nds" }
        assertNotNull(nds)
        assertTrue(nds!!.touchScreen)
        assertEquals(DsDualScreen.CORE_MELONDS_DS, nds.cores.first().id)

        val faceIds = nds.layout.diamond.orEmpty().map { it.id }.toSet()
        assertEquals(setOf(RetroPad.A, RetroPad.B, RetroPad.X, RetroPad.Y), faceIds)
        assertEquals(listOf(RetroPad.L), nds.layout.leftShoulders.map { it.id })
        assertEquals(listOf(RetroPad.R), nds.layout.rightShoulders.map { it.id })

        assertEquals(0f, DsDualScreen.landscapeSideInsetFraction(DsScreenLayout.STACKED), 1e-4f)
        assertTrue(DsDualScreen.landscapeSideInsetFraction(DsScreenLayout.HYBRID) > 0.1f)
        assertTrue(DsDualScreen.PORTRAIT_PAD_FRACTION < 0.42f)
    }
}
