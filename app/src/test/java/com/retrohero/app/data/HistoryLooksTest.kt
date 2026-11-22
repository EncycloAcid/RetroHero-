package com.retrohero.app.data

import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoryLooksTest {
    private val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()
    private val nes = Game("content://x/a.nes", "a.nes", "nes", "t", "p", 1)
    private val gb = Game("content://x/b.gb", "b.gb", "gb", "t", "p", 1)
    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun summariesAddUpByDaySystemAndStreak() {
        val d = LocalDate.of(2026, 11, 10)
        val entries = listOf(
            HistoryEntry(nes, at(d, 20), 1800),
            HistoryEntry(nes, at(d.plusDays(1), 20), 3600),
            HistoryEntry(gb, at(d.plusDays(2), 9), 600),
            HistoryEntry(gb, at(d.plusDays(5), 9), 600),
        )
        val s = PlayHistory.summarise(entries, d, d.plusDays(6), ZoneOffset.UTC)
        assertEquals(6600, s.totalSeconds)
        assertEquals(7, s.days.size)
        assertEquals(3, s.bestStreak)
        assertEquals(nes, s.topGame!!.first)
        assertEquals("nes", s.topSystem!!.first.id)
        assertEquals(19, s.busiestHour)  // 20:30 minus an hour of play starts at 19:30
        assertEquals(4, s.sessions)
    }

    @Test
    fun playLogIsCapped() {
        val f = File(app.filesDir, "cap.json").apply { delete() }
        repeat(1510) { PlayTime.add(f, 1, newSession = false, now = it.toLong()) }
        assertEquals(1500, PlayTime.log(f).size)
        assertEquals(1510, PlayTime.read(f).seconds)
    }

    @Test
    fun looksParseAndValidate() {
        val look = Looks.parse("""{"name":"Game Boy","accent":"#9BBC0F","pad":{"fill":"#CC306230","faces":"mono","glow":3}}""", "gb")
        assertEquals("Game Boy", look.name)
        assertEquals(Color(0xFF9BBC0F), look.accent)
        assertEquals("mono", look.pad.faces)
        assertEquals(1f, look.pad.glow)
        assertEquals(Looks.Glass.edge, look.pad.edge)
        assertThrows(IllegalArgumentException::class.java) { Looks.parse("""{"accent":"#fff"}""", "x") }
        assertNull(Looks.color("#12"))
        assertEquals(Color(0xFFFFFFFF), Looks.color("#fff"))
        val dir = File(app.filesDir, "looks_test").apply { deleteRecursively() }
        Looks.import(dir, """{"name":"Game Boy"}""")
        Looks.import(dir, """{"name":"Game Boy"}""")
        assertEquals(listOf("game-boy", "game-boy-2"), Looks.custom(dir).map { it.id })
    }
}
