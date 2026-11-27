package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric because checkpoint metadata uses Android's org.json. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CheckpointsTest {
    @Test
    fun checkpointsRotateThroughThreeSlots() {
        val dir = kotlin.io.path.createTempDirectory().toFile()
        val cps = Checkpoints(dir, "game")
        repeat(5) { i ->
            val idx = cps.nextIndex()
            cps.stateFile(idx).writeText("state $i")
            cps.record(idx, "Level $i", 1_000L + i, "core")
        }
        val list = cps.list()
        assertEquals(Checkpoints.MAX, list.size)
        assertEquals(listOf("Level 4", "Level 3", "Level 2"), list.map { it.label })
        assertEquals("state 4", cps.stateFile(cps.latest()!!.index).readText())
        cps.clear()
        assertTrue(cps.list().isEmpty())
        dir.deleteRecursively()
    }
}
