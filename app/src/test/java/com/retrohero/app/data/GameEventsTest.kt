package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain JVM tests: the Rich Presence interpreter and event detector against a fake NES RAM. */
class GameEventsTest {
    // Modelled on a typical NES platformer script.
    private val script = """
        Lookup:World
        0=1
        1=2
        2-7=Later

        Lookup:Mode
        0x01=Title
        *=Playing

        Format:Lives
        FormatType=VALUE

        Display:
        ?0xH0770=0?Title screen
        ?0xH0010=1?Boss fight in World @World(0xH075f)
        World @World(0xH075f)-@Number(0xH075c*1_v1) · @Lives(0xH075a) lives · HP @Number(0xH0020) // trailing comment
    """.trimIndent()

    private val ram = ByteArray(0x800)
    private fun set(addr: Int, v: Int) { ram[addr] = v.toByte() }

    private fun presence() = RichPresence.parse(script)!!

    @Test
    fun rendersConditionalAndDefaultDisplayLines() {
        val rp = presence()
        set(0x770, 0)
        rp.evaluate("nes", ram, null) // first frame initializes condition baselines
        assertEquals("Title screen", rp.evaluate("nes", ram, null)!!.text)
        set(0x770, 1); set(0x75f, 1); set(0x75c, 2); set(0x75a, 3)
        val snap = rp.evaluate("nes", ram, null)!!
        assertEquals("World 2-3 · 3 lives · HP 0", snap.text)
        assertEquals(listOf("World", "Number", "Lives", "Number"), snap.values.map { it.name })
        // Generic @Number macros are labelled by the word beside them.
        assertEquals(listOf("World", "Number", "Lives", "HP"), snap.values.map { it.label })
        set(0x75f, 5)
        assertTrue(rp.evaluate("nes", ram, null)!!.text.startsWith("World Later-"))
    }

    @Test
    fun valueExpressionsAddMultiplyAndTakeMax() {
        val expr = RichPresence.ValueExpr.parse("0xH0001*10_0xH0002$0xH0003")!!
        val mem = ByteArray(8).also { it[1] = 4; it[2] = 5; it[3] = 30 }
        assertEquals(45L, expr.eval("nes", mem, null))
        mem[3] = 99
        assertEquals(99L, expr.eval("nes", mem, null))
        assertNull(RichPresence.ValueExpr.parse("M:0xH0001=5"))
        assertEquals(-3L, RichPresence.ValueExpr.parse("A:0xH0001_B:0xH0002*1.4")!!.eval("nes", mem, null))
    }

    @Test
    fun noDisplaySectionMeansNoPresence() {
        assertNull(RichPresence.parse(""))
        assertNull(RichPresence.parse("Lookup:X\n0=A\n"))
    }

    private fun gameplayDetector(): GameEventDetector {
        set(0x770, 1); set(0x75f, 0); set(0x75c, 0); set(0x75a, 3); set(0x20, 5); set(0x10, 0)
        val det = GameEventDetector(presence(), "nes")
        det.update(ram, null, 0) // condition baselines
        det.update(ram, null, 100)
        return det
    }

    @Test
    fun losingALifeIsADeathButGainingOneIsNot() {
        val det = gameplayDetector()
        set(0x75a, 2)
        val events = det.update(ram, null, 200)
        assertTrue(events.single() is GameEvent.Died)
        set(0x75a, 3)
        assertTrue(det.update(ram, null, 5_000).none { it is GameEvent.Died })
    }

    @Test
    fun healthHittingZeroIsADeathWithCooldown() {
        val det = gameplayDetector()
        set(0x20, 0)
        assertTrue(det.update(ram, null, 200).any { it is GameEvent.Died })
        set(0x20, 5); det.update(ram, null, 300)
        set(0x20, 0)
        assertTrue("within cooldown", det.update(ram, null, 400).none { it is GameEvent.Died })
    }

    @Test
    fun levelChangeMustSettleBeforeItCounts() {
        val det = gameplayDetector()
        set(0x75f, 1)
        assertTrue(det.update(ram, null, 200).none { it is GameEvent.LevelChanged })
        assertTrue(det.update(ram, null, 1_000).none { it is GameEvent.LevelChanged })
        val change = det.update(ram, null, 1_800).filterIsInstance<GameEvent.LevelChanged>().single()
        assertEquals("1", change.from)
        assertEquals("2", change.to)
    }

    @Test
    fun flickeringLevelValueIsIgnored() {
        val det = gameplayDetector()
        for (t in 0 until 10) {
            set(0x75f, t % 2)
            assertTrue(det.update(ram, null, 200L + t * 400).none { it is GameEvent.LevelChanged })
        }
    }

    @Test
    fun bossFightStartsAfterItHolds() {
        val det = gameplayDetector()
        set(0x10, 1)
        assertTrue(det.update(ram, null, 200).none { it is GameEvent.BossStarted })
        assertNotNull(det.update(ram, null, 1_300).filterIsInstance<GameEvent.BossStarted>().singleOrNull())
        set(0x10, 0)
        det.update(ram, null, 1_400)
        assertTrue(det.update(ram, null, 2_500).any { it is GameEvent.BossEnded })
    }

    @Test
    fun quietSuppressesEventsAfterAStateLoad() {
        val det = gameplayDetector()
        det.quiet(200)
        set(0x75a, 1); set(0x20, 0)
        assertTrue(det.update(ram, null, 300).isEmpty())
        // After the quiet window the new values are the baseline, not a death.
        assertTrue(det.update(ram, null, 3_000).none { it is GameEvent.Died })
    }

    @Test
    fun switchingDisplayLinesResetsBaselines() {
        val det = gameplayDetector()
        set(0x770, 0) // back to title screen: lives may read as anything
        set(0x75a, 0)
        det.update(ram, null, 200)
        set(0x770, 1); set(0x75a, 3)
        assertTrue(det.update(ram, null, 300).none { it is GameEvent.Died })
    }

    @Test
    fun classifiesValueNames() {
        assertEquals(GameEventDetector.Kind.LIVES, GameEventDetector.kindOf("Lives"))
        assertEquals(GameEventDetector.Kind.HEALTH, GameEventDetector.kindOf("HP"))
        assertEquals(GameEventDetector.Kind.HEALTH, GameEventDetector.kindOf("Hearts"))
        assertEquals(GameEventDetector.Kind.LEVEL, GameEventDetector.kindOf("Stage"))
        assertEquals(GameEventDetector.Kind.LEVEL, GameEventDetector.kindOf("World"))
        assertEquals(GameEventDetector.Kind.OTHER, GameEventDetector.kindOf("Score"))
    }
}
