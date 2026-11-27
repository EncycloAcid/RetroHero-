package com.retrohero.app.emulator

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.M3ExpressivePalettes
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w384dp-h854dp-night-xxhdpi")
class PocketGuideDrawerScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    @Test
    fun rendersPolishedPocketGuideDrawerInWarmPlumThemeWithCyanPrimaryOverride() {
        val rosePalette = M3ExpressivePalettes.last() // Warm plum/rose palette matching user's wallpaper
        val schemeWithCyanOverride = rosePalette.dark.copy(
            primary = Color(0xFF29B6F6),
            onPrimary = Color.White,
        )

        val game = Game(
            id = "nds:platinum",
            fileName = "Pokemon - Platinum Version (USA).nds",
            systemId = "nds",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 134_217_728L,
        )
        val manual = PocketGuides.forGame(game)

        compose.setContent {
            RetroHeroTheme(dynamicColor = false) {
                MaterialTheme(colorScheme = schemeWithCyanOverride) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0D12)),
                    ) {
                        PocketGuideDrawer(
                            visible = true,
                            game = game,
                            manual = manual,
                            initialNotes = "",
                            pinnedNote = null,
                            raData = null,
                            raHardcore = true,
                            onSaveNotes = {},
                            onPinNote = {},
                            onOpenWebBrowser = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        // Verify all 4 tab labels and top Sinnoh hints exist intact
        compose.onNodeWithText("Trainer's Guide").assertExists()
        compose.onNodeWithText("Hints").assertExists()
        compose.onNodeWithText("Lists").assertExists()
        compose.onNodeWithText("Notes").assertExists()
        compose.onNodeWithText("Trophies").assertExists()
        compose.onNodeWithText("Psyduck Blocking Route 210 (Sinnoh)").assertExists()

        val collapsedPng = File(outDir, "pocket_guide_polished_collapsed.png")
        val collapsedBmp = compose.onRoot().captureToImage().asAndroidBitmap()
        collapsedPng.outputStream().use { collapsedBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(collapsedPng.length() > 1024)

        // Expand the first hint to verify animated capsule dots and editorial Nudge card
        compose.onAllNodesWithText("Reveal gentle nudge").onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("1 · NUDGE").assertExists()
        compose.onNodeWithText("Hide all").assertExists()

        val expandedPng = File(outDir, "pocket_guide_polished_expanded.png")
        val expandedBmp = compose.onRoot().captureToImage().asAndroidBitmap()
        expandedPng.outputStream().use { expandedBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(expandedPng.length() > 1024)
    }
}
