package com.retrohero.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Assert.assertEquals
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

    private fun save(name: String) {
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun emptyLauncher() {
        var added = 0
        compose.setContent {
            RetroHeroTheme {
                RomListContent(
                    games = emptyList(),
                    scanning = false,
                    onAddFolder = { added++ },
                    onPlay = {},
                )
            }
        }
        compose.onNodeWithText("No ROMs loaded").assertIsDisplayed()
        compose.onNodeWithText("Add folder").assertIsDisplayed().performClick()
        assertEquals(1, added)
        save("barebones_empty")
    }

    @Test
    fun populatedRomList() {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
        val samples = listOf(
            "Castle Climb (Europe).nes" to "nes",
            "Crystal Tactics (USA).nds" to "nds",
            "Dungeon Dash (Europe).gba" to "gba",
            "Galaxy Siege (USA).chd" to "psx",
            "Meadow Quest (Japan).sfc" to "snes",
            "Moonlight Run (USA).gba" to "gba",
            "Turbo Comet (USA).md" to "genesis",
        ).map { (name, sys) ->
            Game("$tree/document/primary%3ARoms%2F$name", name, sys, tree, "primary:Roms", 1L)
        }
        var played: Game? = null
        compose.setContent {
            RetroHeroTheme {
                RomListContent(
                    games = samples,
                    scanning = false,
                    onAddFolder = {},
                    onPlay = { played = it },
                )
            }
        }
        compose.onNodeWithText("7 ROMs").assertIsDisplayed()
        compose.onNodeWithText("Add folder").assertIsDisplayed()
        compose.onNodeWithText("Moonlight Run").assertIsDisplayed().performClick()
        assertEquals("Moonlight Run", played?.title)
        save("barebones_list")
    }
}
