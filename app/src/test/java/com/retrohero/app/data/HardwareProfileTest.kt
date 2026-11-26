package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareProfileTest {

    @Test
    fun resolvesSuperFxOverclockForYoshisIslandEvenByRomHashWhenRenamed() {
        // Even if the ROM is renamed to "mystery.sfc", the verified CRC32 identifies Yoshi's Island!
        val renamed = Game("id1", "mystery.sfc", "snes", "local", "/tmp", 2_097_152L)
        assertEquals(VisualMode.ENHANCED, HardwareProfiles.defaultModeFor(renamed, romCrcHex = "d2491220"))

        val enhanced = HardwareProfiles.resolve(renamed, "snes9x", VisualMode.ENHANCED, romCrcHex = "d2491220")
        assertTrue(enhanced.isGameSpecific)
        assertEquals("150%", enhanced.coreOptions["snes9x_overclock_superfx"])
        assertEquals(ScreenFilter.SHARP, enhanced.filter)
        assertEquals(ColorProfile.NONE, enhanced.colorProfile)

        val authentic = HardwareProfiles.resolve(renamed, "snes9x", VisualMode.AUTHENTIC, romCrcHex = "d2491220")
        assertEquals("100%", authentic.coreOptions["snes9x_overclock_superfx"])
        assertEquals(ScreenFilter.CRT, authentic.filter)
        assertEquals(ColorProfile.CRT_COMPOSITE, authentic.colorProfile)
    }

    @Test
    fun resolvesAuthenticGbaLcdColorMatrixAndGenesisSonicCompositeWaterfalls() {
        val emerald = Game("id2", "Pokemon Emerald (USA).gba", "gba", "local", "/tmp", 16_777_216L)
        assertEquals(VisualMode.AUTHENTIC, HardwareProfiles.defaultModeFor(emerald))
        val gbaAuth = HardwareProfiles.resolve(emerald, "mgba", VisualMode.AUTHENTIC)
        assertEquals(ColorProfile.GBA_LCD, gbaAuth.colorProfile)
        assertEquals(ScreenFilter.LCD, gbaAuth.filter)

        val gbaEnh = HardwareProfiles.resolve(emerald, "mgba", VisualMode.ENHANCED)
        assertEquals(ColorProfile.NONE, gbaEnh.colorProfile)
        assertEquals(ScreenFilter.SHARP, gbaEnh.filter)

        val sonic = Game("id3", "Sonic the Hedgehog (USA).md", "genesis", "local", "/tmp", 524_288L)
        assertEquals(VisualMode.AUTHENTIC, HardwareProfiles.defaultModeFor(sonic))
        val sonicAuth = HardwareProfiles.resolve(sonic, "genesis_plus_gx", VisualMode.AUTHENTIC)
        assertTrue(sonicAuth.isGameSpecific)
        assertEquals(ColorProfile.CRT_COMPOSITE, sonicAuth.colorProfile)
        assertEquals(ScreenFilter.CRT, sonicAuth.filter)
    }

    @Test
    fun resolvesPs1PgxpEnhancedVsAuthenticCompositeAndN64RogueSquadronFixes() {
        val mgs = Game("id4", "Metal Gear Solid (USA) (Disc 1).chd", "psx", "local", "/tmp", 400_000_000L)
        val ps1Enh = HardwareProfiles.resolve(mgs, "swanstation", VisualMode.ENHANCED)
        assertEquals("true", ps1Enh.coreOptions["duckstation_GPU.PGXPEnable"])
        assertEquals("2", ps1Enh.coreOptions["duckstation_GPU.ResolutionScale"])
        assertEquals(ColorProfile.NONE, ps1Enh.colorProfile)

        val ps1Auth = HardwareProfiles.resolve(mgs, "swanstation", VisualMode.AUTHENTIC)
        assertEquals("false", ps1Auth.coreOptions["duckstation_GPU.PGXPEnable"])
        assertEquals("1", ps1Auth.coreOptions["duckstation_GPU.ResolutionScale"])
        assertEquals(ColorProfile.CRT_COMPOSITE, ps1Auth.colorProfile)

        val rogue = Game("id5", "Star Wars - Rogue Squadron (USA).z64", "n64", "local", "/tmp", 16_777_216L)
        val rogueEnh = HardwareProfiles.resolve(rogue, "mupen64plus_next_gles3", VisualMode.ENHANCED)
        assertTrue(rogueEnh.isGameSpecific)
        assertEquals("True", rogueEnh.coreOptions["mupen64plus-EnableFBEmulation"])
        assertEquals("960x720", rogueEnh.coreOptions["mupen64plus-43screensize"])
    }
}
