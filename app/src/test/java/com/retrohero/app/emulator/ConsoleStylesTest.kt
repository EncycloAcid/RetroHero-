package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.data.Systems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleStylesTest {

    @Test
    fun everySupportedSystemHasDistinctRelevantConsoleSkin() {
        val wordmarks = mutableMapOf<String, MutableList<String>>()
        for (sys in Systems.all) {
            val style = ConsoleStyles.forSystem(sys.id, sys.color)
            assertTrue("System ${sys.id} must have a non-blank bezelWordmark", style.bezelWordmark.isNotBlank())
            assertTrue("System ${sys.id} must have a non-blank bezelSubtitle", style.bezelSubtitle.isNotBlank())
            assertNotEquals("System ${sys.id} must not fall back to generic RETROHERO wordmark", "RETROHERO", style.bezelWordmark)
            if (sys.id != "gb") {
                assertFalse(
                    "Non-Game Boy system ${sys.id} must not inherit DMG subtitle",
                    style.bezelSubtitle.contains("DOT MATRIX", ignoreCase = true),
                )
            }
            wordmarks.getOrPut(style.bezelWordmark) { mutableListOf() }.add(sys.id)
        }
        // Every console must have its own unique wordmark (no two consoles sharing another console's branding)
        val collisions = wordmarks.filterValues { it.size > 1 }
        assertTrue("Distinct consoles must not share identical bezelWordmark: $collisions", collisions.isEmpty())
    }

    @Test
    fun previouslyMismatchedConsolesHaveAuthenticBrandingAndHardwareColors() {
        val ps1 = ConsoleStyles.forSystem("psx", Color.Gray)
        val ps2 = ConsoleStyles.forSystem("ps2", Color.Blue)
        assertEquals("PLAYSTATION", ps1.bezelWordmark)
        assertEquals("PLAYSTATION 2", ps2.bezelWordmark)
        assertNotEquals("PS2 should not reuse PS1 light grey plate", ps1.plateTop, ps2.plateTop)
        assertTrue("PS2 subtitle must reference 128-BIT / Emotion Engine", ps2.bezelSubtitle.contains("128-BIT"))

        val gba = ConsoleStyles.forSystem("gba", Color.Blue)
        val gc = ConsoleStyles.forSystem("gc", Color.Blue)
        val wii = ConsoleStyles.forSystem("wii", Color.Cyan)
        val vb = ConsoleStyles.forSystem("vb", Color.Red)
        val pokeMini = ConsoleStyles.forSystem("pokemini", Color.Yellow)
        assertEquals("GAME BOY ADVANCE", gba.bezelWordmark)
        assertEquals("NINTENDO GAMECUBE", gc.bezelWordmark)
        assertEquals("NINTENDO WII", wii.bezelWordmark)
        assertEquals("VIRTUAL BOY", vb.bezelWordmark)
        assertEquals("POKÉMON MINI", pokeMini.bezelWordmark)
        assertNotEquals("Wii must not reuse purple GBA shell", gba.plateTop, wii.plateTop)
        assertNotEquals("Virtual Boy must not reuse purple GBA shell", gba.plateTop, vb.plateTop)
        assertNotEquals("GameCube A and B buttons must be distinct green & red", gc.face("A"), gc.face("B"))

        val nds = ConsoleStyles.forSystem("nds", Color.Gray)
        val n3ds = ConsoleStyles.forSystem("3ds", Color.Red)
        assertEquals("NINTENDO DS", nds.bezelWordmark)
        assertEquals("NINTENDO 3DS", n3ds.bezelWordmark)
        assertEquals(BezelFormFactor.Clamshell, nds.formFactor)
        assertEquals(BezelFormFactor.Clamshell, n3ds.formFactor)
        assertNotEquals("3DS should be visually distinct from original DS", nds.plateTop, n3ds.plateTop)

        val genesis = ConsoleStyles.forSystem("genesis", Color.Blue)
        val sms = ConsoleStyles.forSystem("sms", Color(0xFF00897B))
        val gg = ConsoleStyles.forSystem("gg", Color.DarkGray)
        val saturn = ConsoleStyles.forSystem("saturn", Color.Blue)
        val dreamcast = ConsoleStyles.forSystem("dreamcast", Color(0xFFFF6D00))
        assertTrue("Master System must be 8-BIT, not Genesis 16-BIT", sms.bezelSubtitle.contains("8-BIT"))
        assertTrue("Game Gear must be 8-BIT, not Genesis 16-BIT", gg.bezelSubtitle.contains("8-BIT"))
        assertTrue("Saturn must be 32-BIT, not Genesis 16-BIT", saturn.bezelSubtitle.contains("32-BIT"))
        assertTrue("Dreamcast must be 128-BIT, not Genesis 16-BIT", dreamcast.bezelSubtitle.contains("128-BIT"))
        assertNotEquals("Dreamcast should be pearl white, not black Genesis", genesis.plateTop, dreamcast.plateTop)
    }

    @Test
    fun formFactorsAndSpecialFinishesMatchRealHardware() {
        assertEquals(BezelFormFactor.GameBoyChamfer, ConsoleStyles.forSystem("gb", Color.Green).formFactor)
        assertEquals(BezelFormFactor.GameBoyChamfer, ConsoleStyles.forSystem("gbc", Color.Cyan).formFactor)
        assertEquals(BezelFormFactor.HandheldWide, ConsoleStyles.forSystem("gba", Color.Blue).formFactor)
        assertEquals(BezelFormFactor.HandheldWide, ConsoleStyles.forSystem("psp", Color.Gray).formFactor)
        assertEquals(BezelFormFactor.HandheldWide, ConsoleStyles.forSystem("gg", Color.Gray).formFactor)
        assertEquals(BezelFormFactor.HomeConsole, ConsoleStyles.forSystem("nes", Color.Red).formFactor)
        assertEquals(BezelFormFactor.HomeConsole, ConsoleStyles.forSystem("snes", Color.Magenta).formFactor)
        assertEquals(BezelFormFactor.HomeConsole, ConsoleStyles.forSystem("ps2", Color.Blue).formFactor)
        assertEquals(BezelFormFactor.ComputerMonitor, ConsoleStyles.forSystem("c64", Color.Gray).formFactor)
        assertEquals(BezelFormFactor.ArcadeCabinet, ConsoleStyles.forSystem("arcade", Color.Yellow).formFactor)
        assertEquals(BezelFormFactor.ArcadeCabinet, ConsoleStyles.forSystem("neogeo", Color.Yellow).formFactor)

        assertTrue("Atari 2600 must feature classic walnut woodgrain", ConsoleStyles.forSystem("atari2600", Color.Gray).woodgrain)
        assertTrue("Intellivision must feature woodgrain finish", ConsoleStyles.forSystem("intellivision", Color.Gray).woodgrain)
        assertFalse("PS2 must not use woodgrain", ConsoleStyles.forSystem("ps2", Color.Blue).woodgrain)
    }

    @Test
    fun instrumentDeckPreservesConsoleIdentityAndFormFactor() {
        val dcDeck = ConsoleStyles.instrument(dark = true, accent = Color(0xFFFF5A1F), systemId = "dreamcast")
        assertEquals("SEGA DREAMCAST", dcDeck.bezelWordmark)
        assertEquals("128-BIT · IT'S THINKING", dcDeck.bezelSubtitle)
        assertEquals(BezelFormFactor.HomeConsole, dcDeck.formFactor)

        val gbDeck = ConsoleStyles.instrument(dark = false, accent = Color(0xFFFF5A1F), systemId = "gb")
        assertEquals("GAME BOY", gbDeck.bezelWordmark)
        assertEquals(BezelFormFactor.GameBoyChamfer, gbDeck.formFactor)
    }

    @Test
    fun deltaSkinSystemMappingsResolveAccurately() {
        assertEquals(listOf("ps2"), DeltaSkins.systemIdsFor("com.retrohero.game.ps2"))
        assertEquals(listOf("gc"), DeltaSkins.systemIdsFor("com.retrohero.game.gamecube"))
        assertEquals(listOf("dreamcast"), DeltaSkins.systemIdsFor("com.retrohero.game.dreamcast"))
        assertEquals(listOf("neogeo"), DeltaSkins.systemIdsFor("com.retrohero.game.neogeo"))
        assertEquals(listOf("atari2600"), DeltaSkins.systemIdsFor("com.retrohero.game.atari2600"))
        assertEquals(listOf("vb"), DeltaSkins.systemIdsFor("com.retrohero.game.vb"))
    }
}
