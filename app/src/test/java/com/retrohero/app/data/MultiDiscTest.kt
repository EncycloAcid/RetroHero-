package com.retrohero.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultiDiscTest {

    @Test
    fun collapsesMultiDiscSetsAndUnifiesSaveBaseName() {
        val d1 = Game("uri://ff7-1", "Final Fantasy VII (USA) (Disc 1).chd", "psx", "tree", "dir1", 400_000_000L)
        val d2 = Game("uri://ff7-2", "Final Fantasy VII (USA) (Disc 2).chd", "psx", "tree", "dir1", 420_000_000L)
        val d3 = Game("uri://ff7-3", "Final Fantasy VII (USA) (Disc 3).chd", "psx", "tree", "dir1", 410_000_000L)
        val single = Game("uri://sotn", "Castlevania - Symphony of the Night (USA).chd", "psx", "tree", "dir1", 350_000_000L)

        // Pass out of order to verify automatic chronological disc sorting
        val collapsed = MultiDisc.collapseMultiDiscGames(listOf(d3, single, d1, d2))
        assertEquals(2, collapsed.size)

        val ff7 = collapsed.first { it.title == "Final Fantasy VII" }
        assertTrue(ff7.isMultiDisc)
        assertEquals(3, ff7.discCount)
        assertEquals(listOf("Disc 1", "Disc 2", "Disc 3"), ff7.discs.map { it.label })
        assertEquals(listOf(1, 2, 3), ff7.discs.map { it.discNumber })
        assertEquals(1_230_000_000L, ff7.size)
        assertEquals("Final Fantasy VII (USA)", ff7.baseName)

        // JSON round-trip preserves discs cleanly
        val rt = Game.fromJson(JSONObject(ff7.toJson().toString()))
        assertTrue(rt.isMultiDisc)
        assertEquals(3, rt.discCount)
        assertEquals("Final Fantasy VII (USA)", rt.baseName)
    }

    @Test
    fun parsesAllCommonDiscConventionsWithoutFalsePositives() {
        val mgs1 = MultiDisc.parseDiscInfo("Metal Gear Solid (USA) (Disk 1 of 2).cue")
        val mgs2 = MultiDisc.parseDiscInfo("Metal Gear Solid (USA) (Disk 2 of 2).cue")
        assertNotNull(mgs1)
        assertNotNull(mgs2)
        assertEquals(mgs1!!.groupKey, mgs2!!.groupKey)
        assertEquals(1, mgs1.discNumber)
        assertEquals(2, mgs2.discNumber)

        val xeno1 = MultiDisc.parseDiscInfo("Xenogears [CD1].bin")
        val xeno2 = MultiDisc.parseDiscInfo("Xenogears [CD2].bin")
        assertNotNull(xeno1)
        assertNotNull(xeno2)
        assertEquals(xeno1!!.groupKey, xeno2!!.groupKey)

        val pds1 = MultiDisc.parseDiscInfo("Panzer Dragoon Saga - Disc 1.chd")
        val pds2 = MultiDisc.parseDiscInfo("Panzer Dragoon Saga - Disc 2.chd")
        assertNotNull(pds1)
        assertNotNull(pds2)
        assertEquals(pds1!!.groupKey, pds2!!.groupKey)

        val fdsA = MultiDisc.parseDiscInfo("Metroid (Japan) (Side A).fds")
        val fdsB = MultiDisc.parseDiscInfo("Metroid (Japan) (Side B).fds")
        assertNotNull(fdsA)
        assertNotNull(fdsB)
        assertEquals(fdsA!!.groupKey, fdsB!!.groupKey)
        assertEquals("Side A", fdsA.label)
        assertEquals("Side B", fdsB.label)

        // Never mistake numbered sequels or existing .m3u playlists for discs!
        assertNull(MultiDisc.parseDiscInfo("Crash Bandicoot 2 - Cortex Strikes Back (USA).chd"))
        assertNull(MultiDisc.parseDiscInfo("Final Fantasy VIII (USA).m3u"))
        assertNull(MultiDisc.parseDiscInfo("Super Mario Bros. 3 (USA).nes"))
    }

    @Test
    fun neverMergesDifferentRegionsOrNumberedSequels() {
        val usa1 = Game("u1", "Resident Evil (USA) (Disc 1).chd", "psx", "tree", "dir1", 300L)
        val jpn2 = Game("j2", "Resident Evil (Japan) (Disc 2).chd", "psx", "tree", "dir1", 300L)
        val cb1 = Game("c1", "Crash Bandicoot (USA).chd", "psx", "tree", "dir1", 300L)
        val cb2 = Game("c2", "Crash Bandicoot 2 (USA).chd", "psx", "tree", "dir1", 300L)

        val result = MultiDisc.collapseMultiDiscGames(listOf(usa1, jpn2, cb1, cb2))
        assertEquals(4, result.size)
        assertTrue(result.none { it.isMultiDisc })
        assertFalse(cb2.isMultiDisc)
    }
}
