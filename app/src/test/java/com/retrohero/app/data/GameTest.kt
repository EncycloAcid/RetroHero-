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
    fun everySystemHasACoreAndUniqueId() {
        assertTrue(Systems.all.all { it.cores.isNotEmpty() })
        assertEquals(Systems.all.size, Systems.all.map { it.id }.toSet().size)
    }

    @Test
    fun unambiguousExtensionsMapToOneSystem() {
        for (ext in listOf("nes", "sfc", "gba", "gb", "gbc", "nds", "pce", "lnx", "a26", "32x")) {
            assertEquals(ext, 1, Systems.all.count { ext in it.extensions })
        }
    }
}
