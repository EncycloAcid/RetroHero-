package com.retrohero.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameMetadataTest {

    @Test
    fun parsesWikipediaInfoboxCreditsCleanly() {
        val wikitext = """
            {{Infobox video game
            | title = Super Mario Bros.
            | developer = [[Nintendo R&D4]]<ref name="dev" />
            | publisher = [[Nintendo]]
            | platforms = {{ubl|[[Nintendo Entertainment System]]|[[Famicom Disk System]]}}
            | released = {{vgrelease|JP|September 13, 1985|NA|October 18, 1985}}
            | genre = [[Platform game|Platform]]
            | modes = [[Single-player video game|Single-player]], [[Multiplayer video game|multiplayer]]
            }}
        """.trimIndent()

        assertEquals("Nintendo R&D4", GameMetadataRepository.parseInfoboxField(wikitext, listOf("developer")))
        assertEquals("Nintendo", GameMetadataRepository.parseInfoboxField(wikitext, listOf("publisher")))
        assertTrue(GameMetadataRepository.parseInfoboxField(wikitext, listOf("released")).contains("1985"))
        assertEquals("Platform", GameMetadataRepository.parseInfoboxField(wikitext, listOf("genre")))
        assertEquals("Single-player, multiplayer", GameMetadataRepository.parseInfoboxField(wikitext, listOf("modes")))
    }

    @Test
    fun rejectsSuperMarioBrosWonderForNesSuperMarioBros() {
        val nesClassic = GameMetadataRepository.WikiPageCandidate(
            pageTitle = "Super Mario Bros.",
            extract = "Super Mario Bros. (Japanese: スーパーマリオブラザーズ, Hepburn: Sūpā Mario Burazāzu) is a 1985 platform game developed and published by Nintendo for the Nintendo Entertainment System.",
            wikitext = """
                {{Infobox video game
                | title = Super Mario Bros.
                | developer = [[Nintendo R&D4]]
                | publisher = [[Nintendo]]
                | platforms = [[Nintendo Entertainment System]]
                | released = {{Video game release|JP|September 13, 1985|NA|October 1985}}
                | genre = [[Platform game|Platform]]
                }}
            """.trimIndent(),
            thumbUrl = null,
        )
        val switchWonder = GameMetadataRepository.WikiPageCandidate(
            pageTitle = "Super Mario Bros. Wonder",
            extract = "Super Mario Bros. Wonder is a 2023 platform game developed and published by Nintendo for the Nintendo Switch.",
            wikitext = """
                {{Infobox video game
                | title = Super Mario Bros. Wonder
                | developer = [[Nintendo EPD]]
                | publisher = [[Nintendo]]
                | platforms = [[Nintendo Switch]]
                | released = October 20, 2023
                | genre = [[Platform game|Platform]]
                }}
            """.trimIndent(),
            thumbUrl = null,
        )

        val classicScore = GameMetadataRepository.scoreCandidatePage(nesClassic, "Super Mario Bros.", "nes")
        val wonderScore = GameMetadataRepository.scoreCandidatePage(switchWonder, "Super Mario Bros.", "nes")
        assertTrue("Expected NES classic ($classicScore) to crush Switch Wonder ($wonderScore)", classicScore > 250)
        assertTrue("Expected Switch Wonder ($wonderScore) to be rejected (< 40)", wonderScore < 40)

        val game = Game("id1", "Super Mario Bros. (USA).nes", "nes", "tree", "doc", 40976L)
        val dossier = GameMetadataRepository.buildDossierFromWikiPage(nesClassic, game, GameDossier.localFallback(game))
        assertEquals("Nintendo R&D4", dossier.developer)
        assertEquals("1985", dossier.releaseYear)
        assertEquals("September 13, 1985", dossier.releaseDate)
        assertTrue("Tagline should be clean lead sentence without Japanese kana", !dossier.tagline.contains("Japanese:") && dossier.tagline.contains("1985 platform game"))
    }

    @Test
    fun invalidatesStaleSchemaV1CacheAndRoundTripsV2() {
        val game = Game("id1", "Super Mario Bros. (USA).nes", "nes", "tree", "doc", 40976L)
        val fallback = GameDossier.localFallback(game)
        val staleV1 = JSONObject(fallback.toJson().toString()).apply { remove("schemaVersion") }
        assertNull("Stale v1 cached dossier should be invalidated", GameDossier.fromJson(staleV1))

        val roundTripped = GameDossier.fromJson(fallback.toJson())
        assertNotNull(roundTripped)
        assertEquals(fallback.title, roundTripped!!.title)
        assertEquals(5, roundTripped.radarAxes.size)
    }

    @Test
    fun extractsEditorialSectionsAndBuiltInHeroBoxDossier() {
        val game = Game("id1", "Super Mario Bros. (USA).nes", "nes", "tree", "doc", 40976L)
        val fallback = GameDossier.localFallback(game)
        val extract = """
            Super Mario Bros. is a 1985 platform game developed and published by Nintendo for the NES.

            == Gameplay ==
            Players control Mario, or his brother Luigi in multiplayer mode, as they travel across the Mushroom Kingdom to rescue Princess Toadstool from Bowser.

            == Reception ==
            Critics hailed the game as a landmark achievement in console game design and one of the greatest video games of all time.
        """.trimIndent()

        val (summary, blocks) = GameMetadataRepository.parseExtractSections(extract, game.title, fallback)
        assertTrue(summary.contains("1985 platform game"))
        assertEquals(2, blocks.size)
        assertEquals("GAMEPLAY & MECHANICS", blocks[0].headline)
        assertEquals("CRITICAL ACCLAIM", blocks[1].headline)

        val hb = Game("hb1", "Starlight Vanguard.hbx", "herobox16", "local", "hb1", 64L)
        val hbDossier = GameDossier.localFallback(hb)
        assertEquals("RetroHero Studio", hbDossier.developer)
        assertEquals("HeroBox-16 Original", hbDossier.sourceLabel)
        assertTrue(hbDossier.isLive)
    }
}
