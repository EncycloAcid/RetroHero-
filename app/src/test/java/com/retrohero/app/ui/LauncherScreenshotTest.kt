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
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setDynamicColorEnabled
import com.retrohero.app.ui.theme.setExpressivePalette
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
        setDynamicColorEnabled(false)
        setExpressivePalette("berry")
        Tone.setDarkMode(true)
    }

    private fun save(name: String) {
        compose.mainClock.advanceTimeBy(950)
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
    fun pureM3ExpressiveScreensAndFlow() {
        val games = sampleGames()
        val now = System.currentTimeMillis()
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
        val sampleCoreStates = mapOf(
            "pcsx_rearmed" to CoreState.Installed(bundled = true, sizeBytes = 4_800_000L),
            "mgba" to CoreState.Installed(bundled = true, sizeBytes = 2_400_000L),
            "snes9x" to CoreState.Installed(bundled = true, sizeBytes = 3_100_000L),
            "gambatte" to CoreState.Installed(bundled = true, sizeBytes = 900_000L),
            "fceumm" to CoreState.Installed(bundled = true, sizeBytes = 1_100_000L),
            "genesis_plus_gx" to CoreState.Installed(bundled = true, sizeBytes = 2_200_000L),
            "dolphin" to CoreState.NotInstalled,
            "mupen64plus_next_gles3" to CoreState.NotInstalled,
            "ppsspp" to CoreState.NotInstalled,
            "melonds" to CoreState.NotInstalled,
        )

        compose.setContent {
            RetroHeroTheme(dynamicColor = false) {
                M3ExpressiveAppView(
                    games = games,
                    recents = games.take(5),
                    favoriteIds = favs,
                    stats = mapOf(
                        games[0].id to PlayStat(seconds = 9_400, sessions = 7, lastPlayed = now - 120_000L),
                        games[1].id to PlayStat(seconds = 4_800, sessions = 4, lastPlayed = now - 3_600_000L),
                        games[2].id to PlayStat(seconds = 3_100, sessions = 2, lastPlayed = now - 86_400_000L),
                    ),
                    shelfStatuses = shelfStatuses,
                    folders = setOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ARoms",
                        "content://com.android.externalstorage.documents/tree/primary%3ADownloads%2FClassics",
                    ),
                    coreStates = sampleCoreStates,
                    biosStatusOf = { file ->
                        when (file.fileName) {
                            "scph5501.bin" -> BiosManager.Status.Verified
                            "gba_bios.bin" -> BiosManager.Status.Unverified
                            else -> BiosManager.Status.Missing
                        }
                    },
                    saveSlotsFor = { g ->
                        if (g.id == games[0].id) {
                            listOf(
                                AppPaths.AUTO_SLOT to (now - 180_000L),
                                1 to (now - 86_400_000L),
                            )
                        } else emptyList()
                    },
                    onPlay = { g, _ -> played = g },
                    onToggleFavorite = { g -> favs = if (g.id in favs) favs - g.id else favs + g.id },
                    onSetShelfStatus = { g, st -> shelfStatuses = shelfStatuses + (g.id to st) },
                    onSelectPalette = { id -> setExpressivePalette(id) },
                    onSetDarkMode = { Tone.setDarkMode(it) },
                )
            }
        }

        // 1. Pure M3 Expressive Home (Berry Violet Dark) + Smart Core Readiness Banner
        compose.mainClock.advanceTimeBy(950)
        compose.onNodeWithText("CONTINUE PLAYING").assertIsDisplayed()
        compose.onNodeWithText("4 cores needed").assertIsDisplayed()
        save("m3e_home_dark")

        // 2. Open Pure M3E Game Detail Sheet (with Core Readiness badge & relative save timestamps!)
        compose.onNodeWithContentDescription("Details for Chrono Symphony").performClick()
        compose.mainClock.advanceTimeBy(950)
        compose.waitForIdle()
        compose.onNodeWithText("Shelf status").assertIsDisplayed()
        compose.onNodeWithText("Ready").assertIsDisplayed()
        save("m3e_detail_sheet")

        compose.onNodeWithText("Resume game").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(games[0].id, played?.id)

        // 3. Games Tab (1-Tap Sort Pill + 56dp M3 SearchBar + Console Polygons + Editorial Grid)
        compose.onNodeWithContentDescription("Games").performClick()
        compose.mainClock.advanceTimeBy(950)
        compose.waitForIdle()
        compose.onAllNodesWithText("Games").onFirst().assertIsDisplayed()
        compose.onNodeWithText("A–Z").assertIsDisplayed()
        save("m3e_games_dark")

        // 4. Settings Tab (Super-Emulator Control Deck)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.mainClock.advanceTimeBy(950)
        compose.waitForIdle()
        compose.onAllNodesWithText("Settings").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Theme & color").assertIsDisplayed()
        save("m3e_settings_dark")

        // 5. Open Core Manager sub-page and capture
        compose.onNodeWithText("Emulator cores").performClick()
        compose.mainClock.advanceTimeBy(950)
        compose.waitForIdle()
        compose.onNodeWithText("Emulator cores").assertIsDisplayed()
        save("m3e_cores_dark")

        // Navigate back to Settings root
        compose.onNodeWithContentDescription("Back").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()

        // 6. Switch to Pixel Sage + Light Mode & capture pure M3E Light Home
        compose.onNodeWithText("Sage").performClick()
        compose.onNodeWithText("Light").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithContentDescription("Home").performClick()
        compose.mainClock.advanceTimeBy(950)
        compose.waitForIdle()
        save("m3e_home_light_sage")
    }
}
