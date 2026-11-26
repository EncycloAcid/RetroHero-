package com.retrohero.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class LaunchBoxMetadataTest {

    @Test
    fun streamsLaunchBoxMetadataZipAndResolvesGameDossierAndStorefrontMedia() {
        val xml = """
            <?xml version="1.0" standalone="yes"?>
            <LaunchBox>
              <Game>
                <Name>Pokémon Platinum Version</Name>
                <ReleaseDate>2008-09-13T00:00:00-05:00</ReleaseDate>
                <ReleaseYear>2008</ReleaseYear>
                <Overview>Pokémon Platinum Version is an enhanced remake of Pokémon Diamond and Pearl featuring Giratina and the Distortion World.</Overview>
                <MaxPlayers>2</MaxPlayers>
                <Cooperative>false</Cooperative>
                <DatabaseID>12440</DatabaseID>
                <CommunityRating>4.62</CommunityRating>
                <Platform>Nintendo DS</Platform>
                <ESRB>E - Everyone</ESRB>
                <Genres>Role-Playing; Adventure</Genres>
                <Developer>Game Freak</Developer>
                <Publisher>Nintendo</Publisher>
              </Game>
              <Game>
                <Name>Unrelated Windows Game</Name>
                <DatabaseID>99999</DatabaseID>
                <Platform>Windows</Platform>
              </Game>
              <GameImage>
                <DatabaseID>12440</DatabaseID>
                <FileName>fanart-platinum-1080p.jpg</FileName>
                <Type>Fanart - Background</Type>
                <Region>World</Region>
              </GameImage>
              <GameImage>
                <DatabaseID>12440</DatabaseID>
                <FileName>box-platinum-jp.jpg</FileName>
                <Type>Box - Front</Type>
                <Region>Japan</Region>
              </GameImage>
              <GameImage>
                <DatabaseID>12440</DatabaseID>
                <FileName>box-platinum-na.jpg</FileName>
                <Type>Box - Front</Type>
                <Region>North America</Region>
              </GameImage>
              <GameImage>
                <DatabaseID>12440</DatabaseID>
                <FileName>title-platinum.png</FileName>
                <Type>Screenshot - Game Title</Type>
                <Region>North America</Region>
              </GameImage>
              <GameImage>
                <DatabaseID>12440</DatabaseID>
                <FileName>shot1-platinum.png</FileName>
                <Type>Screenshot - Gameplay</Type>
                <Region>North America</Region>
              </GameImage>
            </LaunchBox>
        """.trimIndent()

        val tmpDir = kotlin.io.path.createTempDirectory("lb_test").toFile()
        try {
            val zipFile = File(tmpDir, "Metadata.zip")
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("Metadata.xml"))
                zos.write(xml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val dbDir = File(tmpDir, "launchbox_db")
            val status = LaunchBoxMetadata.indexMetadataZipFile(zipFile, dbDir)
            assertTrue(status.synced)
            assertEquals("Only supported retro platforms should be indexed", 1, status.gameCount)

            val game = Game("nds_plat", "Pokemon - Platinum Version (USA).nds", "nds", "tree", "doc", 134217728L)
            val record = LaunchBoxMetadata.lookupLocalIndex(game.title, LaunchBoxMetadata.platformNamesFor("nds"), dbDir)
            assertNotNull(record)
            assertEquals("12440", record!!.databaseId)
            assertEquals("Game Freak", record.developer)
            assertEquals("Nintendo", record.publisher)

            val dossier = LaunchBoxMetadata.toGameDossier(game, record, GameDossier.localFallback(game))
            assertEquals("LaunchBox", dossier.sourceLabel)
            assertTrue(dossier.isLive)
            assertEquals("September 13, 2008", dossier.releaseDate)
            assertEquals("2008", dossier.releaseYear)
            assertEquals("Role-Playing · Adventure", dossier.genre)
            assertEquals("1–2 players", dossier.modes)
            assertEquals("E - Everyone", dossier.esrbRating)
            assertEquals(4.62f, dossier.communityRating!!, 0.01f)
            assertEquals("https://images.launchbox-app.com/fanart-platinum-1080p.jpg", dossier.heroImageUrl)
            assertEquals(
                "North America Box - Front should beat Japan Box - Front for a (USA) ROM",
                "https://images.launchbox-app.com/box-platinum-na.jpg",
                dossier.boxArtUrl,
            )
            assertEquals("https://images.launchbox-app.com/title-platinum.png", dossier.titleScreenUrl)
            assertEquals(listOf("https://images.launchbox-app.com/shot1-platinum.png"), dossier.screenshotUrls)

            // Verify schema v5 round-trip & v4 invalidation
            val staleV4 = JSONObject(dossier.toJson().toString()).put("schemaVersion", 4)
            assertNull("v4 cached Wikipedia dossier must invalidate so LaunchBox v5 loads", GameDossier.fromJson(staleV4))
            val rt = GameDossier.fromJson(dossier.toJson())
            assertNotNull(rt)
            assertEquals("LaunchBox", rt!!.sourceLabel)
            assertEquals(dossier.boxArtUrl, rt.boxArtUrl)
            assertEquals(dossier.screenshotUrls, rt.screenshotUrls)
            assertEquals("E - Everyone", rt.esrbRating)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun parsesLaunchBoxGamesDbSearchAndDetailHtmlCleanly() {
        val searchHtml = """
            <div class="list-item-wrapper">
              <a class="list-item" href="/games/details/152-super-mario-world">
                <h3>Super Mario World</h3>
                <h4>Super Nintendo Entertainment System</h4>
              </a>
              <a class="list-item" href="/games/details/999-super-mario-world-gba">
                <h3>Super Mario Advance 2: Super Mario World</h3>
                <h4>Nintendo Game Boy Advance</h4>
              </a>
            </div>
        """.trimIndent()

        val hits = LaunchBoxMetadata.parseSearchResultsHtml(searchHtml)
        assertEquals(2, hits.size)
        val picked = LaunchBoxMetadata.pickBestSearchHit(
            "Super Mario World",
            LaunchBoxMetadata.platformNamesFor("snes"),
            hits,
        )
        assertNotNull(picked)
        assertEquals("152", picked!!.databaseId)

        val detailHtml = """
            <html><body>
              <h2>Overview</h2>
              <p>Mario's off on his biggest adventure ever through Dinosaur Land to rescue Princess Toadstool from Bowser!</p>
              <table>
                <tr><td>Release Date</td><td>August 23, 1991</td></tr>
                <tr><td>Developers</td><td>Nintendo EAD</td></tr>
                <tr><td>Publishers</td><td>Nintendo</td></tr>
                <tr><td>Genre</td><td>Platform</td></tr>
                <tr><td>Max Players</td><td>2</td></tr>
                <tr><td>Cooperative</td><td>No</td></tr>
                <tr><td>ESRB</td><td>E - Everyone</td></tr>
              </table>
              <div>
                <a href="https://images.launchbox-app.com/smw-box-front.jpg" title="Super Mario World - Box - Front (North America)">Box</a>
                <img src="https://images.launchbox-app.com/smw-fanart-bg.jpg" alt="Super Mario World - Fanart - Background" />
                <img src="https://images.launchbox-app.com/smw-gameplay-1.png" alt="Super Mario World - Screenshot - Gameplay" />
              </div>
            </body></html>
        """.trimIndent()

        val rec = LaunchBoxMetadata.parseDetailsHtml(detailHtml, picked)
        assertEquals("Nintendo EAD", rec.developer)
        assertEquals("Nintendo", rec.publisher)
        assertEquals("August 23, 1991", rec.releaseDate)
        assertEquals("1991", rec.releaseYear)
        assertEquals(listOf("Platform"), rec.genres)
        assertEquals(2, rec.maxPlayers)
        assertEquals("E - Everyone", rec.esrb)
        assertEquals(3, rec.images.size)

        val media = LaunchBoxMetadata.selectStorefrontMedia(rec.images, "Super Mario World (USA)")
        assertEquals("https://images.launchbox-app.com/smw-fanart-bg.jpg", media.heroBannerUrl)
        assertEquals("https://images.launchbox-app.com/smw-box-front.jpg", media.boxArtUrl)
        assertEquals(listOf("https://images.launchbox-app.com/smw-gameplay-1.png"), media.screenshotUrls)
    }

    @Test
    fun mapsAllCoreRetroConsolesToOfficialLaunchBoxPlatforms() {
        val required = listOf(
            "nes", "snes", "n64", "gb", "gbc", "gba", "nds", "3ds", "gc", "wii",
            "genesis", "sms", "gg", "saturn", "dreamcast",
            "psx", "ps2", "psp", "psvita",
            "pce", "ngp", "neogeo", "ws", "lynx", "atari2600", "arcade",
        )
        for (sysId in required) {
            assertTrue(
                "System $sysId must map to an official LaunchBox platform",
                LaunchBoxMetadata.platformNamesFor(sysId).isNotEmpty(),
            )
        }
    }
}
