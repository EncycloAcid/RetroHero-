package com.retrohero.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import com.retrohero.app.data.Game
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens headlessly and writes PNGs to app/build/screenshots/.
 * Uses made-up sample games; box art falls back to generated covers.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun seed() {
        val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()
        val tree = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
        val samples = listOf(
            "Star Courier (USA).nes" to "nes", "Castle Climb (Europe).nes" to "nes",
            "Neon Drift (USA).sfc" to "snes", "Meadow Quest (Japan).sfc" to "snes",
            "Pixel Knight (World).gb" to "gb", "Lost Lagoon (USA).gbc" to "gbc",
            "Moonlight Run (USA).gba" to "gba", "Dungeon Dash (Europe).gba" to "gba",
            "Crystal Tactics (USA).nds" to "nds", "Turbo Comet (USA).md" to "genesis",
            "Retro Racer (Europe).md" to "genesis", "Robo Rally (Europe).sms" to "sms",
            "Galaxy Siege (USA).chd" to "psx", "Velocity X (Europe).chd" to "psx",
            "Thunder Fist.zip" to "arcade", "Sky Pirates (Japan).pce" to "pce",
        )
        val games = samples.map { (name, sys) ->
            Game("$tree/document/primary%3ARoms%2F$name", name, sys, tree, "primary:Roms", 1L)
        }.sortedBy { it.title }
        app.paths.library.writeText(Game.listToJson(games))
        app.settings.addRomFolder(tree)
        app.settings.setDynamicColor(false)
        listOf(9, 6, 12, 2).forEach { app.settings.markPlayed(games[it].id) }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(1500) // let image loads fail over to generated covers
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tab(label: String) {
        // Icon-only floating bar exposes labels as content descriptions; the rail shows text.
        val byDescription = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes()
        if (byDescription.isNotEmpty()) compose.onAllNodesWithContentDescription(label).onFirst().performClick()
        else compose.onAllNodesWithText(label).onFirst().performClick()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun captureLightTheme() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            shot("light_1_home")
            tab("Systems"); shot("light_2_systems")
            tab("Settings"); shot("light_3_settings")
        }
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-night-xxhdpi")
    fun captureLandscape() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            shot("land_1_home")
            tab("Settings"); shot("land_2_settings")
        }
    }

    @Test
    fun captureScreens() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            shot("1_home")
            tab("Search"); shot("2_search")
            tab("Systems"); shot("3_systems")
            tab("Cores"); shot("4_cores")
            tab("Settings"); shot("5_settings")
            tab("Home")
            compose.onAllNodesWithContentDescription("Add games").onFirst().performClick()
            shot("6_add_menu")
            compose.onAllNodesWithText("Download from link").onFirst().performClick()
            shot("7_download_sheet")
            compose.onAllNodesWithText("Cancel").onFirst().performClick()
            compose.waitForIdle()
            compose.onAllNodesWithText("Dungeon Dash").onFirst().performTouchInput { longClick() }
            shot("8_game_sheet")
        }
    }
}
