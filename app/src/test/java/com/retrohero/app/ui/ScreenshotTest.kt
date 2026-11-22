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
// The full walk-through runs first: compose test roots from an earlier test in the same
// class can linger and confuse its node lookups.
@org.junit.FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun seed() {
        val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()
        app.settings.lastTab = null
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

    private fun saveNow(name: String) {
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
            tab("Library"); shot("light_2_library")
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
    @Config(qualifiers = "w360dp-h740dp-night-xhdpi")
    fun captureSmallPhone() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            shot("small_1_home")
        }
    }

    @Test
    fun a0_captureScreens() {
        seed()
        compose.mainClock.autoAdvance = false
        ActivityScenario.launch(MainActivity::class.java).use {
            // Launch intro: the splash mark flying into Home's logo while Home rises in.
            compose.mainClock.advanceTimeBy(250)
            saveNow("0a_intro_start")
            compose.mainClock.advanceTimeBy(380)
            saveNow("0b_intro_mid")
            compose.mainClock.advanceTimeBy(260)
            saveNow("0c_intro_late")
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            shot("1_home")
            // Roll the gacha: mid-spin, then after it lands.
            compose.onAllNodesWithText("roll!!").onFirst().performClick()
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(1400)
            saveNow("1b_rolling")
            compose.mainClock.advanceTimeBy(9500)
            saveNow("1c_landed")
            compose.mainClock.autoAdvance = true
            tab("Search"); shot("2_search")
            tab("Library"); shot("3_library")
            compose.onAllNodesWithText("Systems").onFirst().performClick()
            shot("3a_library_systems")
            compose.onAllNodesWithText("Game Boy Advance").onFirst().performClick()
            shot("3b_system_page")
            compose.onAllNodesWithText("Moonlight Run").onFirst().performClick()
            shot("3c_game_page")
            compose.onAllNodesWithContentDescription("Back").onFirst().performClick()
            compose.waitForIdle()
            compose.onAllNodesWithContentDescription("Back").onFirst().performClick()
            compose.waitForIdle()
            tab("Cores"); shot("4_cores")
            tab("Settings"); shot("5_settings")
            compose.onAllNodesWithText("Customisation").onFirst().performClick()
            shot("5b_customisation")
            compose.onAllNodesWithContentDescription("Back").onFirst().performClick()
            compose.waitForIdle()
            compose.onAllNodesWithText("Gameplay & comfort").onFirst().performClick()
            shot("5c_gameplay")
            compose.onAllNodesWithContentDescription("Back").onFirst().performClick()
            compose.waitForIdle()
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
