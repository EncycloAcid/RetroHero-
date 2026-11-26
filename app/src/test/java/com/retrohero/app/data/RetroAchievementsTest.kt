package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RetroAchievementsTest {

    @Test
    fun nesHashStripsInes16ByteHeader() {
        val prg = ByteArray(16384) { (it * 31).toByte() }
        val inesHeader = ByteArray(16).apply {
            this[0] = 'N'.code.toByte()
            this[1] = 'E'.code.toByte()
            this[2] = 'S'.code.toByte()
            this[3] = 0x1A.toByte()
            this[4] = 1
        }
        val headered = inesHeader + prg
        val h1 = RetroAchievementsHasher.hashBytes(prg, "nes", "nes")
        val h2 = RetroAchievementsHasher.hashBytes(headered, "nes", "nes")
        assertEquals(32, h1.length)
        assertEquals("Headered and unheadered NES ROMs must produce identical RA hash", h1, h2)
    }

    @Test
    fun snesHashStrips512ByteCopierHeader() {
        val rom = ByteArray(32768) { (it * 17).toByte() }
        val copierHeader = ByteArray(512) { 0x55 }
        val h1 = RetroAchievementsHasher.hashBytes(rom, "snes", "sfc")
        val h2 = RetroAchievementsHasher.hashBytes(copierHeader + rom, "snes", "smc")
        assertEquals("Headered SMC and clean SFC ROMs must produce identical RA hash", h1, h2)
    }

    @Test
    fun n64HashNormalizesV64AndN64ToZ64BigEndian() {
        val z64 = ByteArray(256)
        z64[0] = 0x80.toByte(); z64[1] = 0x37.toByte(); z64[2] = 0x12.toByte(); z64[3] = 0x40.toByte()
        for (i in 4 until z64.size) z64[i] = (i * 13).toByte()

        // Byteswapped 16-bit (.v64)
        val v64 = z64.copyOf()
        var i = 0
        while (i < v64.size) {
            val t = v64[i]; v64[i] = v64[i + 1]; v64[i + 1] = t
            i += 2
        }

        // Little-endian 32-bit (.n64)
        val n64 = z64.copyOf()
        i = 0
        while (i < n64.size) {
            val b0 = n64[i]; val b1 = n64[i + 1]; val b2 = n64[i + 2]; val b3 = n64[i + 3]
            n64[i] = b3; n64[i + 1] = b2; n64[i + 2] = b1; n64[i + 3] = b0
            i += 4
        }

        val hz = RetroAchievementsHasher.hashBytes(z64, "n64", "z64")
        val hv = RetroAchievementsHasher.hashBytes(v64, "n64", "v64")
        val hn = RetroAchievementsHasher.hashBytes(n64, "n64", "n64")
        assertEquals(hz, hv)
        assertEquals(hz, hn)
    }

    @Test
    fun logicEvaluatorTriggersOnRealNesDeltaAndHitConditions() {
        // Super Mario Bros.-style trigger: World (0x075F) == 0 AND Level transitions 0 -> 1 (d0xH0760=0_0xH0760=1)
        val ach = RaAchievement(
            id = 101,
            title = "Underground Bound",
            description = "Reach World 1-2",
            points = 5,
            badgeName = "00101",
            memAddr = "0xH075f=0_d0xH0760=0_0xH0760=1",
        )
        val trigger = RaLogicEvaluator.compile(ach)
        assertNotNull(trigger)

        val ram = ByteArray(2048)
        ram[0x075F] = 0
        ram[0x0760] = 0

        // Frame 0 initializes baselines
        assertFalse(RaLogicEvaluator.step(trigger!!, "nes", ram, null))
        // Frame 1 still on 1-1
        assertFalse(RaLogicEvaluator.step(trigger, "nes", ram, null))
        // Frame 2 transitions to 1-2 -> UNLOCK!
        ram[0x0760] = 1
        assertTrue(RaLogicEvaluator.step(trigger, "nes", ram, null))
        // Frame 3 does not re-trigger
        assertFalse(RaLogicEvaluator.step(trigger, "nes", ram, null))
    }

    @Test
    fun logicEvaluatorSupportsResetIfAndHitCountersAndAltGroups() {
        // Core: 0xH0010=5 for 2 hits, ResetIf 0xH0011=1; Alt1: 0xH0020=9 OR Alt2: 0xH0021=9
        val ach = RaAchievement(
            id = 202,
            title = "Combo Chain",
            description = "Hold value 5 for 2 ticks while either alt flag is set",
            points = 10,
            badgeName = "00202",
            memAddr = "0xH0010=5(2)_R:0xH0011=1S0xH0020=9S0xH0021=9",
        )
        val trigger = RaLogicEvaluator.compile(ach)!!
        val ram = ByteArray(2048)
        ram[0x0021] = 9 // Alt2 satisfied

        // Baseline init
        assertFalse(RaLogicEvaluator.step(trigger, "nes", ram, null))
        // Hit 1
        ram[0x0010] = 5
        assertFalse(RaLogicEvaluator.step(trigger, "nes", ram, null))
        // ResetIf fires -> clears hits!
        ram[0x0011] = 1
        assertFalse(RaLogicEvaluator.step(trigger, "nes", ram, null))
        // Clear reset, hit 1 again
        ram[0x0011] = 0
        assertFalse(RaLogicEvaluator.step(trigger, "nes", ram, null))
        // Hit 2 -> UNLOCK!
        assertTrue(RaLogicEvaluator.step(trigger, "nes", ram, null))
    }

    @Test
    fun raGameDataRoundTripsJsonAndComputesStats() {
        val data = RaGameData(
            gameId = 1446,
            romHash = "811b027eaf99c2def7b933c5208636de",
            title = "Super Mario Bros.",
            imageIconUrl = "https://media.retroachievements.org/Images/012345.png",
            richPresencePatch = "",
            hardcoreEnabled = true,
            achievements = listOf(
                RaAchievement(1, "Mushroom Kingdom", "Beat 1-1", 5, "00001", "0xH0760=1", flags = 3, type = "progression", unlocked = true, unlockedHardcore = true),
                RaAchievement(2, "Save the Princess", "Beat 8-4", 25, "00002", "0xH075f=7", flags = 3, type = "win_condition", unlocked = false, unlockedHardcore = false),
            ),
        )
        val restored = RaGameData.fromJson(data.toJson())
        assertEquals(1446, restored.gameId)
        assertEquals(1, restored.unlockedCount)
        assertEquals(2, restored.totalCount)
        assertEquals(5, restored.earnedPoints)
        assertEquals(30, restored.totalPoints)
        assertEquals(50, restored.completionPct)
    }
}
