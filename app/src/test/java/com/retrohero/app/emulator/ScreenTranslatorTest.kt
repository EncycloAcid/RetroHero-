package com.retrohero.app.emulator

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTranslatorTest {

    @Test
    fun normalizesPixelBounds() {
        val r = NormalizedRect.fromPixelBounds(left = 100, top = 50, right = 300, bottom = 150, imgW = 400, imgH = 200)
        assertEquals(0.25f, r.left, 0.001f)
        assertEquals(0.25f, r.top, 0.001f)
        assertEquals(0.75f, r.right, 0.001f)
        assertEquals(0.75f, r.bottom, 0.001f)
        assertEquals(0.5f, r.width, 0.001f)
        assertEquals(0.5f, r.height, 0.001f)
    }

    @Test
    fun mergesMultiLineDialogueBlocksButKeepsDistantHudSeparate() {
        val hud = RecognizedBlock("HP 99", NormalizedRect(0.05f, 0.05f, 0.25f, 0.10f))
        val line1 = RecognizedBlock("こんにちは", NormalizedRect(0.10f, 0.72f, 0.85f, 0.78f))
        val line2 = RecognizedBlock("勇者よ！", NormalizedRect(0.10f, 0.80f, 0.70f, 0.86f))

        val merged = mergeDialogueBlocks(listOf(line2, hud, line1))
        assertEquals(2, merged.size)
        assertEquals("HP 99", merged[0].text)
        assertEquals("こんにちは 勇者よ！", merged[1].text)
        assertEquals(0.72f, merged[1].bounds.top, 0.001f)
        assertEquals(0.86f, merged[1].bounds.bottom, 0.001f)
    }

    @Test
    fun resolvesLanguageCodesWithFallback() {
        assertEquals(TranslateSourceLang.JAPANESE, TranslateSourceLang.fromCode(null))
        assertEquals(TranslateSourceLang.JAPANESE, TranslateSourceLang.fromCode("unknown"))
        assertEquals(TranslateSourceLang.SPANISH, TranslateSourceLang.fromCode("es"))
        assertEquals(TranslateSourceLang.KOREAN, TranslateSourceLang.fromCode("KO"))
    }
}
