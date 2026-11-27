package com.retrohero.app.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SpokenTextFilterTest {
    @Test
    fun readsNewDialogueAndSkipsHudThatStaysOnScreen() {
        val f = SpokenTextFilter()
        assertEquals(listOf("SCORE", "Welcome to the village!"), f.newText(listOf("SCORE", "001200", "Welcome to the village!")))
        // HUD label still there, dialogue replaced: only the new line is read.
        assertEquals(listOf("Have you seen the princess?"), f.newText(listOf("SCORE", "001300", "Have you seen the princess?")))
    }

    @Test
    fun skipsNumbersAndSingleLetters() {
        val f = SpokenTextFilter()
        assertTrue(f.newText(listOf("x03", "12:45", "A", "  ")).isEmpty())
    }

    @Test
    fun growingDialogueBoxOnlyReadsTheNewPart() {
        val f = SpokenTextFilter()
        assertEquals(listOf("Hello"), f.newText(listOf("Hello")))
        assertEquals(listOf("traveler. Rest here."), f.newText(listOf("Hello, traveler. Rest here.")))
    }

    @Test
    fun doesNotRepeatRecentLinesAfterAMenu() {
        val f = SpokenTextFilter()
        f.newText(listOf("The door is locked."))
        f.newText(listOf("ITEMS", "Potion"))
        assertTrue(f.newText(listOf("The door is locked.")).isEmpty())
        f.reset()
        assertEquals(listOf("The door is locked."), f.newText(listOf("The door is locked.")))
    }

    @Test
    fun normalizesWhitespace() {
        val f = SpokenTextFilter()
        assertEquals(listOf("Two lines of text"), f.newText(listOf("Two  lines\n of text ")))
    }

    @Test
    fun guessesSpeechLanguageFromScript() {
        assertEquals(Locale.JAPANESE, SpokenTextFilter.localeFor("ようこそ"))
        assertEquals(Locale.KOREAN, SpokenTextFilter.localeFor("안녕하세요"))
        assertEquals(Locale.CHINESE, SpokenTextFilter.localeFor("你好"))
        assertNull(SpokenTextFilter.localeFor("Hello"))
    }
}
