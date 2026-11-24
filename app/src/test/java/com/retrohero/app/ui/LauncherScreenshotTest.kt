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
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.theme.EmberOrangeColor
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
        setAccent(EmberOrangeColor)
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
            "Symphony Ring (USA).chd" to "psx",
            "Cyber Runner (USA).gba" to "gba",
            "Zelda Wind Waker (USA).rvz" to "gc",
            "Hades Quest (USA).sfc" to "snes",
            "Red Outlaw (USA).chd" to "psx",
            "Wild Hunt (USA).rvz" to "gc",
            "Celeste Peak (USA).gbc" to "gbc",
            "Hollow Cavern (USA).nes" to "nes",
            "Doom Fortress (USA).md" to "genesis",
            "Factory Core (USA).gba" to "gba",
            "Outer Cosmos (USA).n64" to "n64",
        ).mapIndexed { idx, (name, sys) ->
            Game("$tree/document/primary%3ARoms%2F$name", name, sys, tree, "primary:Roms", (20 - idx) * 1000L)
        }
    }

    @Test
    fun shelvesAndInteractiveCase() {
        val games = sampleGames()
        var shelfStatuses by mutableStateOf(emptyMap<String, String>())
        var played: Game? = null

        compose.setContent {
            RetroHeroTheme {
                ShelvesAppView(
                    games = games,
                    recents = games.take(3),
                    favoriteIds = emptySet(),
                    stats = mapOf(games[0].id to PlayStat(seconds = 5400, sessions = 4, lastPlayed = 1000L)),
                    shelfStatuses = shelfStatuses,
                    hasFolders = true,
                    onAddFolder = {},
                    onSetShelfBucket = { g, b -> shelfStatuses = shelfStatuses + (g.id to b.id) },
                    onPlay = { g, _ -> played = g },
                )
            }
        }

        compose.onAllNodesWithText("Shelves").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Currently Playing").assertIsDisplayed()
        compose.onNodeWithText("3 games").assertIsDisplayed()
        compose.onNodeWithText("Completed").assertIsDisplayed()
        compose.onAllNodesWithText("4 games").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Backlog").assertIsDisplayed()
        save("shelves_main")

        // Tap a disc game cover ("Symphony Ring") to open the 3D physical case modal
        compose.onAllNodesWithText("SYMPHONY RING").onFirst().performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        save("case_closed")

        // Tap "Open case" to swing the 3D front cover open and reveal the booklet + optical disc
        compose.onNodeWithText("Open case").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        compose.onNodeWithText("INSTRUCTION BOOKLET").assertIsDisplayed()
        save("case_open_disc")

        // Tap the optical disc inside the open tray to eject & launch gameplay!
        compose.onNodeWithContentDescription("Optical disc for Symphony Ring").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(games[0].id, played?.id)

        // Close case, switch to Library grid tab
        compose.onNodeWithContentDescription("Close case").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Library").performClick()
        compose.waitForIdle()
        save("library_grid")
    }

    @Test
    fun openCartridgeCase() {
        val games = sampleGames()
        var played: Game? = null
        compose.setContent {
            RetroHeroTheme {
                ShelvesAppView(
                    games = games,
                    recents = games.take(3),
                    favoriteIds = setOf(games[1].id),
                    stats = emptyMap(),
                    initialOpenGameId = games[1].id, // GBA cartridge game
                    autoOpenCaseOnMount = true,
                    onAddFolder = {},
                    onPlay = { g, _ -> played = g },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("INSTRUCTION BOOKLET").assertIsDisplayed()
        save("case_open_cartridge")

        compose.onNodeWithContentDescription("ROM cartridge for Cyber Runner").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(games[1].id, played?.id)
    }
}
