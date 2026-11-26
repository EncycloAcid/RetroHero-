package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheatFinderTest {

    @Test
    fun parsesLibretroChtFormatCleanlyAndDefaultsDisabled() {
        val sample = """
            cheats = 4

            cheat0_desc = "Enable Code (Must Be On)"
            cheat0_code = "0000317D+000A+1006C9E4+0007"
            cheat0_enable = false

            cheat1_desc = "Infinite Coins"
            cheat1_code = "8201227C+FFFF"
            cheat1_enable = true

            cheat2_desc = "Giant \"Super\" Mario"
            cheat2_code = "810F0890 3F90"
            cheat2_enable = false

            cheat3_desc = "Empty Code Ignored"
            cheat3_code = ""
            cheat3_enable = false
        """.trimIndent()

        val parsed = CheatFinder.parseCht(sample)
        assertEquals(3, parsed.size)
        assertEquals("Enable Code (Must Be On)", parsed[0].name)
        assertEquals("0000317D+000A+1006C9E4+0007", parsed[0].code)
        assertFalse(parsed[0].enabled)

        assertEquals("Infinite Coins", parsed[1].name)
        assertEquals("8201227C+FFFF", parsed[1].code)
        assertFalse("Auto-parsed cheats must default to disabled", parsed[1].enabled)

        assertEquals("""Giant "Super" Mario""", parsed[2].name)
        assertEquals("810F0890 3F90", parsed[2].code)
    }

    @Test
    fun normalizesNoIntroTitlesArticlesRomansAndAccents() {
        assertEquals(
            CheatFinder.normalizeTitle("The Legend of Zelda: Ocarina of Time"),
            CheatFinder.normalizeTitle("Legend of Zelda, The - Ocarina of Time (USA) (Rev 2).cht"),
        )
        assertEquals(
            CheatFinder.normalizeTitle("Final Fantasy VII"),
            CheatFinder.normalizeTitle("Final Fantasy 7 (USA) (GameShark).cht"),
        )
        assertEquals(
            CheatFinder.normalizeTitle("Pokémon FireRed Version"),
            CheatFinder.normalizeTitle("Pokemon - FireRed Version (USA) (Code Breaker).cht"),
        )
    }

    @Test
    fun ranksExactTitleAndPreferredRegionAboveSequels() {
        val catalog = listOf(
            "Advance Wars 2 - Black Hole Rising (USA, Europe) (Code Breaker).cht",
            "Advance Wars (Japan) (Code Breaker).cht",
            "Advance Wars (Europe) (Action Replay).cht",
            "Advance Wars (USA, Europe) (Code Breaker).cht",
            "Advance Guardian Heroes (World) (Code Breaker).cht",
        )
        val rankedUsa = CheatFinder.rankCandidates(
            gameTitle = "Advance Wars",
            romBaseName = "Advance Wars (USA)",
            folder = "Nintendo - Game Boy Advance",
            filenames = catalog,
        )
        assertTrue(rankedUsa.isNotEmpty())
        assertEquals("Advance Wars (USA, Europe) (Code Breaker).cht", rankedUsa.first().fileName)
        assertTrue(
            "Sequel Advance Wars 2 should not outrank Advance Wars",
            rankedUsa.none { it.fileName.startsWith("Advance Wars 2") && it.score >= rankedUsa.first().score },
        )

        val rankedEu = CheatFinder.rankCandidates(
            gameTitle = "Advance Wars",
            romBaseName = "Advance Wars (Europe)",
            folder = "Nintendo - Game Boy Advance",
            filenames = catalog,
        )
        assertEquals("Advance Wars (Europe) (Action Replay).cht", rankedEu.first().fileName)
    }

    @Test
    fun directProbeGeneratesNoIntroArticleReorderedCandidates() {
        val probes = CheatFinder.directProbeFilenames(
            gameTitle = "The Legend of Zelda: A Link to the Past",
            romBaseName = "Legend of Zelda, The - A Link to the Past (USA)",
        )
        assertTrue(probes.contains("Legend of Zelda, The - A Link to the Past (USA).cht"))
        assertTrue(probes.contains("Legend of Zelda, The - A Link to the Past (USA) (Game Genie).cht"))
    }

    @Test
    fun mergePreservesUserCheatsAndActiveTogglesWithoutDuplicates() {
        val existing = listOf(
            Cheat("My Custom Code", "AABBCCDD", enabled = true),
            Cheat("Infinite Lives", "7E0DBE63", enabled = true),
        )
        val discovered = listOf(
            Cheat("Infinite Lives", "7E0DBE63", enabled = false),
            Cheat("Infinite Coins", "7E0DBF63", enabled = false),
        )
        val merged = CheatFinder.merge(existing, discovered)
        assertEquals(3, merged.size)
        assertTrue(merged[0].enabled)
        assertTrue("Existing enabled toggle must be preserved", merged[1].enabled)
        assertEquals("Infinite Coins", merged[2].name)
        assertFalse(merged[2].enabled)
    }

    @Test
    fun classifiesPopularCheatsAccurately() {
        assertTrue(CheatFinder.isPopularCheat("Infinite 99 Lives"))
        assertTrue(CheatFinder.isPopularCheat("Enable Code (Must Be On)"))
        assertTrue(CheatFinder.isPopularCheat("Moon Jump (Press L)"))
        assertTrue(CheatFinder.isPopularCheat("Max Gold / Rupees"))
        assertFalse(CheatFinder.isPopularCheat("Disable Mosaic Effect"))
        assertFalse(CheatFinder.isPopularCheat("In-level music mod"))
    }

    @Test
    fun offlineClassicsFallbackMatchesIconicGames() {
        val smw = CheatFinder.findBuiltInClassics("snes", "Super Mario World", "Super Mario World (USA)")
        assertNotNull(smw)
        assertTrue(smw!!.second.any { it.name.contains("Lives") })

        val emerald = CheatFinder.findBuiltInClassics("gba", "Pokémon Emerald", "Pokemon - Emerald Version (USA)")
        assertNotNull(emerald)
        assertTrue(emerald!!.second.isNotEmpty())

        val sm64 = CheatFinder.findBuiltInClassics("n64", "Super Mario 64", "Super Mario 64 (USA)")
        assertNotNull(sm64)
        assertTrue(sm64!!.second.any { it.code.contains(" ") })
    }

    @Test
    fun mapsMajorSystemsToOfficialLibretroChtFolders() {
        assertTrue(CheatFinder.chtFoldersFor("nes").contains("Nintendo - Nintendo Entertainment System"))
        assertTrue(CheatFinder.chtFoldersFor("snes").contains("Nintendo - Super Nintendo Entertainment System"))
        assertTrue(CheatFinder.chtFoldersFor("gba").contains("Nintendo - Game Boy Advance"))
        assertTrue(CheatFinder.chtFoldersFor("n64").contains("Nintendo - Nintendo 64"))
        assertTrue(CheatFinder.chtFoldersFor("psx").contains("Sony - PlayStation"))
        assertTrue(CheatFinder.chtFoldersFor("genesis").contains("Sega - Mega Drive - Genesis"))
        assertTrue(CheatFinder.chtFoldersFor("nds").contains("Nintendo - Nintendo DS"))
    }
}
