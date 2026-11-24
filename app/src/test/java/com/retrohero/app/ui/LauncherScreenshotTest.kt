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
import com.retrohero.app.ui.theme.ElectricLimeColor
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        setAccent(ElectricLimeColor)
        Tone.setDarkMode(true)
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun emptyLibrary() {
        var added = 0
        compose.setContent {
            RetroHeroTheme {
                GameLibraryView(
                    games = emptyList(),
                    recents = emptyList(),
                    favoriteIds = emptySet(),
                    stats = emptyMap(),
                    scanning = false,
                    hasFolders = false,
                    onAddFolder = { added++ },
                    onPlay = {},
                    onToggleFavorite = {},
                )
            }
        }
        compose.onNodeWithText("YOUR COLLECTION").assertIsDisplayed()
        compose.onNodeWithText("Game Library").assertIsDisplayed()
        compose.onNodeWithText("Add game folder").assertIsDisplayed().performClick()
        assertEquals(1, added)
        save("game_library_empty")
    }

    @Test
    fun populatedGameLibrary() {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
        val samples = listOf(
            "Shadow of the Colossus (USA).chd" to "psx",
            "The Wind Waker (USA).rvz" to "gc",
            "Moonlight Run (USA).gba" to "gba",
            "Chrono Trigger (USA).sfc" to "snes",
            "Castlevania Symphony of the Night (USA).chd" to "psx",
        ).mapIndexed { i, (name, sys) ->
            Game("$tree/document/primary%3ARoms%2F$name", name, sys, tree, "primary:Roms", (5 - i) * 1000L)
        }
        val yesterday = System.currentTimeMillis() - 86_400_000L
        val stats = mapOf(
            samples[0].id to PlayStat(seconds = 4_250, sessions = 4, lastPlayed = yesterday),
            samples[1].id to PlayStat(seconds = 5_600, sessions = 5, lastPlayed = yesterday - 86_400_000L),
        )
        var favorites by mutableStateOf(setOf(samples[1].id))
        var played: Game? = null

        compose.setContent {
            RetroHeroTheme {
                GameLibraryView(
                    games = samples,
                    recents = listOf(samples[0], samples[1]),
                    favoriteIds = favorites,
                    stats = stats,
                    scanning = false,
                    hasFolders = true,
                    onAddFolder = {},
                    onPlay = { played = it },
                    onToggleFavorite = { g ->
                        favorites = if (g.id in favorites) favorites - g.id else favorites + g.id
                    },
                )
            }
        }

        compose.onNodeWithText("YOUR COLLECTION").assertIsDisplayed()
        compose.onNodeWithText("Game Library").assertIsDisplayed()
        compose.onNodeWithText("CONTINUE PLAYING").assertIsDisplayed()
        compose.onNodeWithText("Resume game").assertIsDisplayed().performClick()
        assertEquals(samples[0].id, played?.id)

        compose.onNodeWithText("All games").assertIsDisplayed()
        compose.onNodeWithText("5 titles").assertIsDisplayed()
        save("game_library_main")

        // Switch to Favourites tab via bottom icon bar
        compose.onNodeWithContentDescription("Favourites").performClick()
        compose.onNodeWithText("Favourites").assertIsDisplayed()
        compose.onAllNodesWithText("The Wind Waker").onFirst().assertIsDisplayed()
        save("game_library_favourites")

        // Switch to Recent tab via bottom icon bar
        compose.onNodeWithContentDescription("Recent").performClick()
        compose.onNodeWithText("Recently Played").assertIsDisplayed()
        assertTrue(favorites.contains(samples[1].id))
        save("game_library_recent")
    }
}
