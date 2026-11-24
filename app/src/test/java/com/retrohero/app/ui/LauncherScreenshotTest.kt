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
    fun allFourDistinctPagesAndFloatingNav() {
        val games = sampleGames()
        var shelfStatuses by mutableStateOf(emptyMap<String, String>())
        var played: Game? = null

        compose.setContent {
            RetroHeroTheme {
                ShelvesAppView(
                    games = games,
                    recents = games.take(3),
                    favoriteIds = setOf(games[1].id, games[3].id),
                    stats = mapOf(
                        games[0].id to PlayStat(seconds = 7_800, sessions = 6, lastPlayed = 2000L),
                        games[1].id to PlayStat(seconds = 4_200, sessions = 3, lastPlayed = 1500L),
                        games[2].id to PlayStat(seconds = 2_400, sessions = 2, lastPlayed = 1000L),
                    ),
                    shelfStatuses = shelfStatuses,
                    hasFolders = true,
                    foldersCount = 2,
                    onAddFolder = {},
                    onSetShelfBucket = { g, b -> shelfStatuses = shelfStatuses + (g.id to b.id) },
                    onSelectAccent = { _, col -> setAccent(col) },
                    onPlay = { g, _ -> played = g },
                )
            }
        }

        // 1. Shelves Page + Desk Spotlight + FloatingNavBar
        compose.onNodeWithText("ON YOUR DESK").assertIsDisplayed()
        compose.onNodeWithText("Currently Playing").assertIsDisplayed()
        save("page1_shelves")

        // Open 3D Case directly from Desk Spotlight "Open case" button (swings open immediately!)
        compose.onNodeWithText("Open case").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("INSTRUCTION BOOKLET").assertIsDisplayed()
        compose.onNodeWithContentDescription("Optical disc for Symphony Ring").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(games[0].id, played?.id)
        compose.onNodeWithContentDescription("Close case").performClick()
        compose.waitForIdle()

        // 2. Vault Page: Console Hardware Deck + 3D Boxes + 3D Spine Bookshelf
        compose.onNodeWithContentDescription("Vault").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onAllNodesWithText("Vault").onFirst().assertIsDisplayed()
        save("page2_vault_boxes")

        compose.onNodeWithContentDescription("Spines").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        save("page2_vault_spines")

        // 3. Stats Dashboard Page: Donut Arc Gauge + Hall of Fame Podium + Bento Grid
        compose.onNodeWithContentDescription("Stats").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithText("Stats & Insights").assertIsDisplayed()
        compose.onNodeWithText("Hall of Fame").assertIsDisplayed()
        save("page3_stats")

        // 4. System Control Deck Page: Dual Quick-Tiles + Live Switches + Firmware Matrix
        compose.onNodeWithContentDescription("System").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithText("System & Engine").assertIsDisplayed()
        compose.onNodeWithText("Gameplay & Comfort").assertIsDisplayed()
        save("page4_system")
    }
}
