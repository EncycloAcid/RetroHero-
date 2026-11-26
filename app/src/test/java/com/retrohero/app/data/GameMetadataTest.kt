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

    @Test
    fun gameDnaProducesAccurateProfilesWithoutSubstringCorruption() {
        fun axisMap(axes: List<GameDossier.RadarAxis>) = axes.associate { it.label to it.value }

        // Super Mario Bros.: Wikipedia summary intentionally includes trap words ("simple", "similar", "arcade", "beating")
        val mario = axisMap(
            GameDossier.computeGameDna(
                genre = "Platform",
                modes = "Single-player, multiplayer",
                title = "Super Mario Bros.",
                summary = "Super Mario Bros. is a simple yet iconic arcade-inspired platform game about beating Bowser, similar to earlier classics.",
                systemId = "nes",
            ),
        )
        assertTrue("Mario ACTION should be high (${mario["ACTION"]})", mario["ACTION"]!! >= 0.80f)
        assertTrue("Mario SKILL should be high (${mario["SKILL"]})", mario["SKILL"]!! >= 0.85f)
        assertTrue("Mario REPLAY should be high (${mario["REPLAY"]})", mario["REPLAY"]!! >= 0.90f)
        assertTrue("Mario RPG must stay low (${mario["RPG"]})", mario["RPG"]!! <= 0.15f)

        // Chrono Trigger (JRPG)
        val chrono = axisMap(
            GameDossier.computeGameDna(
                genre = "Role-playing",
                modes = "Single-player",
                title = "Chrono Trigger",
                summary = "A turn-based role-playing game with party members, quests, and experience points.",
                systemId = "snes",
            ),
        )
        assertTrue("Chrono RPG should be >= 0.95 (${chrono["RPG"]})", chrono["RPG"]!! >= 0.95f)
        assertTrue("Chrono ADVENTURE should be >= 0.90 (${chrono["ADVENTURE"]})", chrono["ADVENTURE"]!! >= 0.90f)
        assertTrue("Chrono ACTION should stay moderate/low (${chrono["ACTION"]})", chrono["ACTION"]!! <= 0.45f)

        // Tetris (Puzzle)
        val tetris = axisMap(
            GameDossier.computeGameDna(
                genre = "Puzzle",
                modes = "Single-player, multiplayer",
                title = "Tetris",
                summary = "A falling-block tile-matching puzzle game focused on high score mastery.",
                systemId = "gb",
            ),
        )
        assertTrue("Tetris SKILL should be >= 0.94", tetris["SKILL"]!! >= 0.94f)
        assertTrue("Tetris REPLAY should be >= 0.95", tetris["REPLAY"]!! >= 0.95f)
        assertTrue("Tetris RPG should be <= 0.12", tetris["RPG"]!! <= 0.12f)
        assertTrue("Tetris ADVENTURE should be <= 0.15", tetris["ADVENTURE"]!! <= 0.15f)

        // Sub-franchise disambiguation in local fallback
        assertEquals("Racing · Arcade", GameDossier.guessGenreFromTitle("Mario Kart 64", "n64"))
        assertEquals("Puzzle · Strategy", GameDossier.guessGenreFromTitle("Dr. Mario", "nes"))
        assertEquals("Role-Playing", GameDossier.guessGenreFromTitle("Super Mario RPG", "snes"))
        assertEquals("Platformer", GameDossier.guessGenreFromTitle("Super Mario Bros.", "nes"))

        // Castlevania: Symphony of the Night hybrid Metroidvania RPG
        val sotn = axisMap(
            GameDossier.computeGameDna(
                genre = "Action-adventure",
                modes = "Single-player",
                title = "Castlevania: Symphony of the Night",
                summary = "An interconnected non-linear exploration game with experience points and inventory.",
                systemId = "ps1",
            ),
        )
        assertTrue("SotN ADVENTURE >= 0.90", sotn["ADVENTURE"]!! >= 0.90f)
        assertTrue("SotN RPG >= 0.80", sotn["RPG"]!! >= 0.80f)

        // Verify schema v2 & v3 caches are invalidated by v4
        val baseJson = GameDossier.localFallback(Game("id1", "Super Mario Bros. (USA).nes", "nes", "tree", "doc", 40976L)).toJson().toString()
        assertNull("Stale v2 cache should be invalidated", GameDossier.fromJson(JSONObject(baseJson).put("schemaVersion", 2)))
        assertNull("Stale v3 cache should be invalidated by v4", GameDossier.fromJson(JSONObject(baseJson).put("schemaVersion", 3)))
    }

    @Test
    fun resolvesPrequelsSequelsAndMatchesOwnedLibraryGames() {
        // 1. Curated franchise timeline: Super Mario Bros. (1985)
        val smb = GameSeries.resolve("Super Mario Bros. (USA)", "nes")
        assertEquals("Super Mario", smb.seriesName)
        assertTrue("Expected Mario Bros. as prequel", smb.entries.any { it.title == "Mario Bros." && it.relation == "Prequel" })
        assertTrue("Expected Super Mario Bros. 2 as sequel", smb.entries.any { it.title == "Super Mario Bros. 2" && it.relation == "Sequel" })
        assertTrue("Expected Super Mario Bros. 3 as sequel", smb.entries.any { it.title == "Super Mario Bros. 3" && it.relation == "Sequel" })

        // 2. Curated franchise with subtitle alias: Ocarina of Time
        val oot = GameSeries.resolve("The Legend of Zelda - Ocarina of Time (USA)", "n64")
        assertEquals("The Legend of Zelda", oot.seriesName)
        assertTrue("Expected A Link to the Past as prequel", oot.entries.any { it.title.contains("A Link to the Past") && it.relation == "Prequel" })
        assertTrue("Expected Majora's Mask as sequel", oot.entries.any { it.title.contains("Majora's Mask") && it.relation == "Sequel" })

        // 3. Wikipedia infobox fallback for non-curated series
        val wiki = GameSeries.resolve(
            gameTitle = "Banjo-Tooie",
            systemId = "n64",
            wikiSeries = "Banjo-Kazooie",
            wikiPrecededBy = "Banjo-Kazooie",
            wikiFollowedBy = "Banjo-Kazooie: Grunty's Revenge",
        )
        assertEquals("Banjo-Kazooie", wiki.seriesName)
        assertEquals(2, wiki.entries.size)
        assertEquals("Prequel", wiki.entries[0].relation)
        assertEquals("Sequel", wiki.entries[1].relation)

        // 4. Library cross-matching + automatic numbered sequel discovery
        val g1 = Game("id1", "Super Mario Bros. (USA).nes", "nes", "t", "d1", 40976L)
        val g2 = Game("id2", "Super Mario Bros. 3 (USA).nes", "nes", "t", "d2", 393232L)
        val aw1 = Game("aw1", "Advance Wars (USA).gba", "gba", "t", "d3", 8388608L)
        val aw2 = Game("aw2", "Advance Wars 2 (USA).gba", "gba", "t", "d4", 8388608L)

        val smbPaired = GameSeries.resolveWithLibrary(g1, smb.entries, listOf(g1, g2, aw1, aw2))
        val ownedSmb3 = smbPaired.firstOrNull { it.first.title == "Super Mario Bros. 3" }
        assertNotNull("Super Mario Bros. 3 should match owned library ROM", ownedSmb3?.second)
        assertEquals("id2", ownedSmb3!!.second!!.id)

        val awPaired = GameSeries.resolveWithLibrary(aw1, emptyList(), listOf(g1, g2, aw1, aw2))
        assertEquals(1, awPaired.size)
        assertEquals("Sequel", awPaired[0].first.relation)
        assertEquals("aw2", awPaired[0].second?.id)

        // 5. Built-in HeroBox-16 launch titles link to each other & round-trip JSON cleanly
        val hb = Game("hb1", "Starlight Vanguard.hbx", "herobox16", "local", "hb1", 64L)
        val hbDossier = GameDossier.localFallback(hb)
        assertEquals("HeroBox-16 Originals", hbDossier.seriesName)
        assertEquals(2, hbDossier.relatedGames.size)
        val rt = GameDossier.fromJson(hbDossier.toJson())
        assertNotNull(rt)
        assertEquals("HeroBox-16 Originals", rt!!.seriesName)
        assertEquals(2, rt.relatedGames.size)
    }
}
