package com.retrohero.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.theme.ExpressiveIrisColor
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setAccent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class LauncherScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    @Before
    fun setupTheme() {
        setAccent(ExpressiveIrisColor)
        Tone.setDarkMode(true)
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun sampleGames(): List<Game> {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
        return listOf(
            "Chrono Symphony (USA).chd" to "psx",
            "Metroid Zero (USA).gba" to "gba",
            "Wind Waker (USA).rvz" to "gc",
            "Super World (USA).sfc" to "snes",
            "Star Fox (USA).n64" to "n64",
            "Sonic Mania (USA).md" to "genesis",
            "Celeste Classic (USA).gbc" to "gbc",
            "Castlevania (USA).nes" to "nes",
            "Persona Portable (USA).iso" to "psp",
            "Mario Kart DS (USA).nds" to "nds",
        ).mapIndexed { idx, (name, sys) ->
            Game("$tree/document/primary%3ARoms%2F$name", name, sys, tree, "primary:Roms", (20 - idx) * 1000L)
        }
    }

    @Test
    fun expressiveIosFlowAndScreenshots() {
        val games = sampleGames()
        var shelfStatuses by mutableStateOf(
            mapOf(
                games[0].id to "playing",
                games[1].id to "completed",
                games[2].id to "completed",
                games[3].id to "playing",
            ),
        )
        var favs by mutableStateOf(setOf(games[0].id, games[1].id, games[3].id))
        var played: Game? = null

        compose.setContent {
            RetroHeroTheme {
                ExpressiveAppView(
                    games = games,
                    recents = games.take(5),
                    favoriteIds = favs,
                    stats = mapOf(
                        games[0].id to PlayStat(seconds = 9_400, sessions = 7, lastPlayed = 2000L),
                        games[1].id to PlayStat(seconds = 4_800, sessions = 4, lastPlayed = 1500L),
                        games[2].id to PlayStat(seconds = 3_100, sessions = 2, lastPlayed = 1000L),
                    ),
                    shelfStatuses = shelfStatuses,
                    foldersCount = 2,
                    saveSlotsFor = { g ->
                        if (g.id == games[0].id) listOf(AppPaths.AUTO_SLOT to 2000L, 1 to 1500L) else emptyList()
                    },
                    onPlay = { g, _ -> played = g },
                    onToggleFavorite = { g -> favs = if (g.id in favs) favs - g.id else favs + g.id },
                    onSetShelfStatus = { g, st -> shelfStatuses = shelfStatuses + (g.id to st) },
                    onSelectAccent = { _, col -> setAccent(col) },
                    onSetDarkMode = { Tone.setDarkMode(it) },
                )
            }
        }

        // 1. Play Home (Dark Mode)
        compose.onNodeWithText("CONTINUE PLAYING").assertIsDisplayed()
        compose.onNodeWithText("Jump Back In").assertIsDisplayed()
        save("m3ios_play_dark")

        // 2. Open frosted iOS Game Detail Sheet via Split Button Info icon
        compose.onNodeWithContentDescription("Details for Chrono Symphony").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithText("SHELF STATUS").assertIsDisplayed()
        save("m3ios_detail_sheet")

        compose.onNodeWithText("Resume Game").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        assertEquals(games[0].id, played?.id)

        // 3. Library Tab (Console Scallop Strip + M3 Connected Filters + Squircle Grid)
        compose.onNodeWithContentDescription("Library").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onAllNodesWithText("Library").onFirst().assertIsDisplayed()
        save("m3ios_library_dark")

        // 4. Settings Tab (iOS Grouped Inset + M3 Expressive Swatches & Bento)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onAllNodesWithText("Settings").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        save("m3ios_settings_dark")

        // 5. Switch to Light Mode & capture crisp iOS Light Mode Home
        compose.onNodeWithText("Light").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithContentDescription("Play").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        save("m3ios_play_light")
    }
}
