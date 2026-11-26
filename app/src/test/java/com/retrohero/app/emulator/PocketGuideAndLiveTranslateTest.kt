package com.retrohero.app.emulator

import com.retrohero.app.data.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketGuideAndLiveTranslateTest {

    @Test
    fun frameDialogueSignatureDetectsTextChangeAndIgnoresNull() {
        assertEquals(0L, frameDialogueSignature(null))
        val w = 64
        val h = 32
        val frameA = IntArray(2 + w * h) { 0xFF000000.toInt() }.apply { this[0] = w; this[1] = h }
        val frameB = frameA.copyOf()
        assertEquals(frameDialogueSignature(frameA), frameDialogueSignature(frameB))

        // Draw a white dialogue box at the bottom of frameB
        for (y in 20 until 30) {
            for (x in 8 until 56) {
                frameB[2 + y * w + x] = 0xFFFFFFFF.toInt()
            }
        }
        assertNotEquals(frameDialogueSignature(frameA), frameDialogueSignature(frameB))
    }

    @Test
    fun pocketGuidesReturnsCuratedThreeTierSpoilerFreeHints() {
        val metroid = Game(
            id = "local://sm",
            fileName = "Super Metroid (Japan, USA).sfc",
            systemId = "snes",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 3_145_728L,
        )
        val manual = PocketGuides.forGame(metroid)
        assertTrue(manual.badge.contains("Curated"))
        assertTrue(manual.hints.size >= 3)
        assertTrue(manual.hints.all { it.nudge.isNotBlank() && it.clue.isNotBlank() && it.solution.isNotBlank() })
    }

    @Test
    fun pocketGuidesFallsBackCleanlyForAnyGame() {
        val unknown = Game(
            id = "local://custom",
            fileName = "Obscure 1991 RPG (Japan).nes",
            systemId = "nes",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 262_144L,
        )
        val manual = PocketGuides.forGame(unknown)
        assertTrue(manual.controls.isNotEmpty())
        assertTrue(manual.hints.isNotEmpty())
    }
}
