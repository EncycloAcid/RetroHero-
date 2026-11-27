package com.retrohero.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PhysicalMediaSpec
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.components.M3BookshelfSection
import com.retrohero.app.ui.components.ShelfLayoutMath
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h891dp-xxhdpi")
class BookshelfViewTest {

    @get:Rule
    val compose = createComposeRule()

    private fun fakeGame(id: String, title: String, sys: String) = Game(
        id = id,
        fileName = "$title (USA).rom",
        systemId = sys,
        treeUri = "tree",
        parentDocId = "doc",
        size = 2048L,
    )

    @Test
    fun spineDimensionsAndLightDarkContrastAreConsoleAuthentic() {
        val psx = ShelfLayoutMath.spineDimensions("psx")
        val snes = ShelfLayoutMath.spineDimensions("snes")
        val genesis = ShelfLayoutMath.spineDimensions("genesis")
        val nds = ShelfLayoutMath.spineDimensions("nds")
        val ps2 = ShelfLayoutMath.spineDimensions("ps2")

        // Distinct physical case heights: DS/PS1 shorter than Genesis clamshell & PS2 DVD keep case
        assertTrue(psx.heightDp < genesis.heightDp)
        assertTrue(nds.heightDp < ps2.heightDp)
        assertTrue(snes.widthDp > psx.widthDp)

        // DS white snap case gets dark text; PS1 dark jewel case gets light text
        val dsGame = fakeGame("ds1", "Pokémon Platinum", "nds")
        val psxGame = fakeGame("ps1", "Metal Gear Solid", "psx")
        assertTrue(ShelfLayoutMath.isLightSpine(ShelfLayoutMath.spineBaseColor(dsGame, PhysicalMediaSpec.forSystem("nds"))))
        assertFalse(ShelfLayoutMath.isLightSpine(ShelfLayoutMath.spineBaseColor(psxGame, PhysicalMediaSpec.forSystem("psx"))))

        val chunked = ShelfLayoutMath.chunkIntoShelves(List(10) { fakeGame("g$it", "Game $it", "snes") }, perShelf = 8)
        assertEquals(listOf(8, 2), chunked.map { it.size })
    }

    @Test
    fun bookshelfRendersSpinesAndSlidesCaseForwardOnTap() {
        val games = listOf(
            fakeGame("g1", "Chrono Trigger", "snes"),
            fakeGame("g2", "Metal Gear Solid", "psx"),
            fakeGame("g3", "Sonic the Hedgehog 2", "genesis"),
            fakeGame("g4", "Pokémon Platinum", "nds"),
            fakeGame("g5", "Super Mario 64", "n64"),
            fakeGame("g6", "Super Metroid", "snes"),
            fakeGame("g7", "Castlevania SotN", "psx"),
        )
        val stats = mapOf("g1" to PlayStat(seconds = 7400, lastPlayed = 1L, sessions = 4))

        compose.setContent {
            RetroHeroTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 24.dp),
                ) {
                    M3BookshelfSection(
                        games = games,
                        stats = stats,
                        onOpenDetail = {},
                        onQuickPlay = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Physical Shelf").assertIsDisplayed()
        // Spines show the 3D viewer's spine art; the title is their accessibility label.
        compose.onNodeWithContentDescription("Chrono Trigger").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("3D Case").assertIsDisplayed()
        compose.onNodeWithText("Play").assertIsDisplayed()

        // Spine and case art are built off the main thread; give them a moment before the capture.
        Thread.sleep(1500)
        compose.waitForIdle()
        // Capture visual screenshot for inspection
        val outDir = File("build/screenshots").apply { mkdirs() }
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outDir, "bookshelf_3d_pulled.png")).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
