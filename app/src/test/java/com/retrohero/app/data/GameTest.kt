package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    private fun game(name: String, system: String = "snes") = Game(
        id = "content://x/$name", fileName = name, systemId = system,
        treeUri = "content://x", parentDocId = "root", size = 0,
    )

    @Test
    fun titleStripsRegionAndRevisionTags() {
        assertEquals("Super Hero Quest", game("Super Hero Quest (USA) (Rev 1).sfc").title)
        assertEquals("Space Run", game("Space Run [!].nes", "nes").title)
        assertEquals("my game", game("my_game.gba", "gba").title)
    }

    @Test
    fun baseNameIsFileSystemSafe() {
        assertEquals("A_B (USA)", game("A:B (USA).md", "genesis").baseName)
    }

    @Test
    fun everySystemHasACoreOrStandaloneEngineAndUniqueId() {
        assertTrue(Systems.all.size >= 64)
        assertTrue(
            Systems.all.all {
                it.cores.isNotEmpty() || ExternalEmulators.forSystem(it.id).isNotEmpty()
            },
        )
        assertEquals(Systems.all.size, Systems.all.map { it.id }.toSet().size)
    }

    @Test
    fun unambiguousExtensionsMapToOneSystem() {
        val uniqueExts = listOf(
            "hbx", "ch8",
            "nes", "sfc", "n64", "z64", "v64", "gba", "gb", "gbc", "nds", "3ds",
            "gcm", "rvz", "wbfs", "wua", "nsp", "xci", "vpk", "min", "mgw", "sg",
            "32x", "cdi", "gdi", "cso", "elf", "pce", "sgx", "lnx", "a26", "a52",
            "a78", "j64", "col", "int", "vec", "o2", "sv", "chf", "tzx", "cdt",
            "st", "hdm", "fdi", "jar", "mx1", "d64", "adf", "dosz", "scummvm", "p8", "tic",
        )
        for (ext in uniqueExts) {
            assertEquals(ext, 1, Systems.all.count { ext in it.extensions })
        }
    }

    @Test
    fun builtInCoresAndEmbeddedCartridgesAreValid() {
        val hb16 = Systems.all.firstOrNull { it.id == "herobox16" }
        val chip8 = Systems.all.firstOrNull { it.id == "chip8" }
        val nes = Systems.all.firstOrNull { it.id == "nes" }
        assertTrue(hb16 != null && hb16.cores.firstOrNull()?.builtIn == true)
        assertTrue(chip8 != null && chip8.cores.firstOrNull()?.builtIn == true)
        assertTrue(nes != null && nes.cores.firstOrNull()?.id == "retrohero_nes" && nes.cores.firstOrNull()?.builtIn == true)
        val embedded = Homebrew.catalog.filter { it.embeddedBytes != null }
        assertTrue("Expected at least 4 instant built-in games", embedded.size >= 4)
        val hbxGames = embedded.filter { it.systemId == "herobox16" }
        assertTrue("Expected 3 HeroBox-16 launch cartridges", hbxGames.size >= 3)
        for (cart in hbxGames) {
            val bytes = cart.embeddedBytes!!
            assertTrue(bytes.size >= 16)
            assertEquals("HBX1", String(bytes.copyOfRange(0, 4), Charsets.US_ASCII))
        }
        val ch8Games = embedded.filter { it.systemId == "chip8" }
        assertTrue("Expected at least 1 built-in CHIP-8 cartridge", ch8Games.isNotEmpty())
        assertTrue(ch8Games.first().embeddedBytes!!.size >= 16)
    }

}
