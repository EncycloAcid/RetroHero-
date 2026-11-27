package com.retrohero.app.emulator

import com.retrohero.app.data.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InGameBrowserTest {

    @Test
    fun quickLinksSeedExactGameTitleAndSystem() {
        val game = Game(
            id = "local://oot",
            fileName = "Legend of Zelda, The - Ocarina of Time (USA).z64",
            systemId = "n64",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 33_554_432L,
        )
        val links = InGameBrowserUrls.quickLinksFor(game)
        assertTrue(links.size >= 6)
        assertTrue(links.any { it.label == "Walkthrough" && it.url.contains("Ocarina+of+Time") && it.url.contains("N64") })
        assertTrue(links.any { it.label == "GameFAQs" && it.url.startsWith("https://gamefaqs.gamespot.com/") })
        assertTrue(links.any { it.label == "Maps" && it.url.contains("images") })
        assertTrue(links.any { it.label == "YouTube" && it.url.startsWith("https://m.youtube.com/") })
    }

    @Test
    fun resolveInputHandlesUrlsDomainsAndSearchQueries() {
        assertEquals("https://strategywiki.org/wiki/Super_Metroid", InGameBrowserUrls.resolveInput("https://strategywiki.org/wiki/Super_Metroid"))
        assertEquals("https://gamefaqs.gamespot.com", InGameBrowserUrls.resolveInput("gamefaqs.gamespot.com"))
        val query = InGameBrowserUrls.resolveInput("water temple boss key")
        assertTrue(query.startsWith("https://duckduckgo.com/?q="))
        assertTrue(query.contains("water+temple+boss+key"))
    }

    @Test
    fun isAllowedSchemeBlocksUnsafeRedirects() {
        assertTrue(InGameBrowserUrls.isAllowedScheme("https://strategywiki.org"))
        assertTrue(InGameBrowserUrls.isAllowedScheme("http://example.com/map.png"))
        assertFalse(InGameBrowserUrls.isAllowedScheme("javascript:alert(1)"))
        assertFalse(InGameBrowserUrls.isAllowedScheme("intent://scan/#Intent;end"))
        assertFalse(InGameBrowserUrls.isAllowedScheme("file:///etc/passwd"))
        assertFalse(InGameBrowserUrls.isAllowedScheme(""))
    }
}
