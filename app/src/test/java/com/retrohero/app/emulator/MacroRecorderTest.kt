package com.retrohero.app.emulator

import com.retrohero.app.data.RetroPad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MacroRecorderTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun sampleMaskAtWrapsSeamlesslyAcrossMultipleLoops() {
        val left = 1 shl RetroPad.LEFT
        val a = 1 shl RetroPad.A
        val macro = RecordedMacro(
            id = "m1",
            name = "Test",
            durationMs = 1_000L,
            steps = listOf(
                MacroStep(0L, left),
                MacroStep(400L, left or a),
                MacroStep(800L, 0),
            ),
        )

        assertEquals(left, MacroEngine.sampleMaskAt(macro, 0L))
        assertEquals(left, MacroEngine.sampleMaskAt(macro, 399L))
        assertEquals(left or a, MacroEngine.sampleMaskAt(macro, 400L))
        assertEquals(0, MacroEngine.sampleMaskAt(macro, 900L))

        // Loop #2 and #5 wrap cleanly via modulo
        assertEquals(left, MacroEngine.sampleMaskAt(macro, 1_150L))
        assertEquals(left or a, MacroEngine.sampleMaskAt(macro, 4_550L))
        assertEquals(0, MacroEngine.sampleMaskAt(macro, 4_950L))
    }

    @Test
    fun diffButtonChangesEmitsExactPressAndReleaseTransitions() {
        val left = 1 shl RetroPad.LEFT
        val right = 1 shl RetroPad.RIGHT
        val a = 1 shl RetroPad.A

        val delta = MacroEngine.diffButtonChanges(left or a, right or a)
        assertEquals(
            setOf(RetroPad.LEFT to false, RetroPad.RIGHT to true),
            delta.toSet(),
        )

        // Stopping releases every held button cleanly
        val releaseAll = MacroEngine.diffButtonChanges(right or a, 0)
        assertEquals(
            setOf(RetroPad.RIGHT to false, RetroPad.A to false),
            releaseAll.toSet(),
        )
    }

    @Test
    fun summarizeAndBuiltInPresetsAreValid() {
        val presets = MacroEngine.builtInPresets()
        assertEquals(2, presets.size)
        val grassSummary = MacroEngine.summarize(presets[0])
        assertTrue(grassSummary.contains("◄"))
        assertTrue(grassSummary.contains("►"))
        assertTrue(grassSummary.contains("A"))
        assertTrue(grassSummary.contains("2.4s"))
    }

    @Test
    fun macroStoreRoundTripsCustomMacrosAndSkipsBuiltIns() {
        val file = tempDir.newFile("pokemon_emerald.json")
        val custom = MacroEngine.createRecorded(
            name = "Route 101 Grind",
            durationMs = 3_200L,
            steps = listOf(
                MacroStep(0L, 1 shl RetroPad.LEFT),
                MacroStep(1_600L, (1 shl RetroPad.RIGHT) or (1 shl RetroPad.A)),
            ),
            fastForward = true,
        )
        MacroStore.save(file, listOf(custom) + MacroEngine.builtInPresets())

        val loaded = MacroStore.load(file)
        assertEquals(1, loaded.size)
        assertEquals("Route 101 Grind", loaded.first().name)
        assertEquals(3_200L, loaded.first().durationMs)
        assertEquals(2, loaded.first().steps.size)
        assertTrue(loaded.first().fastForward)
    }
}
