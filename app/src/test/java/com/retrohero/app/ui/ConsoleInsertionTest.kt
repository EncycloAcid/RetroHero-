package com.retrohero.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameDossier
import com.retrohero.app.data.PhysicalMediaSpec
import com.retrohero.app.ui.components.ConsoleHardwareSpec
import com.retrohero.app.ui.components.ConsoleInsertionMath
import com.retrohero.app.ui.components.ConsoleInsertionStageContent
import com.retrohero.app.ui.components.ConsoleLoaderKind
import com.retrohero.app.ui.components.ResolvedPhysicalTextures
import com.retrohero.app.ui.components.buildProceduralBackBitmap
import com.retrohero.app.ui.components.buildProceduralCartBackBitmap
import com.retrohero.app.ui.components.buildProceduralCartFrontBitmap
import com.retrohero.app.ui.components.buildProceduralDiscLabelBitmap
import com.retrohero.app.ui.components.buildProceduralDiscUndersideBitmap
import com.retrohero.app.ui.components.buildProceduralFrontBitmap
import com.retrohero.app.ui.components.buildProceduralSpineBitmap
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ConsoleInsertionTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    @Test
    fun mapsConsolesToAuthenticHardwareLoaderMechanisms() {
        assertEquals(ConsoleLoaderKind.CARTRIDGE_FRONT_DECK, ConsoleHardwareSpec.forSystem("nes", PhysicalMediaSpec.forSystem("nes")).loaderKind)
        assertEquals(ConsoleLoaderKind.CARTRIDGE_TOP_SLOT, ConsoleHardwareSpec.forSystem("snes", PhysicalMediaSpec.forSystem("snes")).loaderKind)
        assertEquals(ConsoleLoaderKind.CARTRIDGE_TOP_SLOT, ConsoleHardwareSpec.forSystem("n64", PhysicalMediaSpec.forSystem("n64")).loaderKind)
        assertEquals(ConsoleLoaderKind.CARTRIDGE_TOP_SLOT, ConsoleHardwareSpec.forSystem("nds", PhysicalMediaSpec.forSystem("nds")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_TOP_LID, ConsoleHardwareSpec.forSystem("psx", PhysicalMediaSpec.forSystem("psx")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_TOP_LID, ConsoleHardwareSpec.forSystem("dc", PhysicalMediaSpec.forSystem("dc")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_TOP_LID, ConsoleHardwareSpec.forSystem("gc", PhysicalMediaSpec.forSystem("gc")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_SLIDE_TRAY, ConsoleHardwareSpec.forSystem("ps2", PhysicalMediaSpec.forSystem("ps2")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_SLOT_IN, ConsoleHardwareSpec.forSystem("wii", PhysicalMediaSpec.forSystem("wii")).loaderKind)
    }

    @Test
    fun evaluatesInsertionTimelineSmoothlyForCartridgeAndDiscConsoles() {
        val snesHw = ConsoleHardwareSpec.forSystem("snes", PhysicalMediaSpec.forSystem("snes"))
        val start = ConsoleInsertionMath.evaluate(0.0f, snesHw)
        val mid = ConsoleInsertionMath.evaluate(0.50f, snesHw)
        val seated = ConsoleInsertionMath.evaluate(0.75f, snesHw)
        val end = ConsoleInsertionMath.evaluate(1.0f, snesHw)

        assertEquals(0f, start.insertDepth, 1e-3f)
        assertFalse(start.seated)
        assertEquals(0f, start.ledGlow, 1e-3f)
        assertTrue(mid.insertDepth in 0.25f..0.80f)
        assertTrue(seated.seated)
        assertTrue(seated.ledGlow > 0f)
        assertEquals(1f, end.insertDepth, 1e-3f)
        assertEquals(1f, end.ledGlow, 1e-3f)
        assertEquals(1f, end.bootFlash, 1e-3f)

        val psxHw = ConsoleHardwareSpec.forSystem("psx", PhysicalMediaSpec.forSystem("psx"))
        val psxStart = ConsoleInsertionMath.evaluate(0.05f, psxHw)
        val psxMid = ConsoleInsertionMath.evaluate(0.60f, psxHw)
        val psxEnd = ConsoleInsertionMath.evaluate(0.95f, psxHw)
        assertEquals(1f, psxStart.lidOpen, 1e-3f)
        assertTrue(psxMid.seated)
        assertTrue(psxMid.discSpinDeg > 20f)
        assertTrue(psxMid.lidOpen < 0.9f)
        assertEquals(0f, psxEnd.lidOpen, 1e-3f)
        assertTrue(psxEnd.discSpinDeg > 600f)

        val wiiHw = ConsoleHardwareSpec.forSystem("wii", PhysicalMediaSpec.forSystem("wii"))
        assertTrue(ConsoleInsertionMath.evaluate(0.85f, wiiHw).ledGlow > 0.8f)
    }

    @Test
    fun rendersCartridgeAndDiscConsoleInsertionScreenshots() {
        fun sampleTextures(game: Game, dossier: GameDossier, spec: PhysicalMediaSpec): ResolvedPhysicalTextures {
            val front = buildProceduralFrontBitmap(game, dossier, spec)
            val aspect = front.width.toFloat() / front.height.coerceAtLeast(1)
            val back = buildProceduralBackBitmap(game, dossier, spec, aspect, front, null)
            val spine = buildProceduralSpineBitmap(game, dossier, spec, front, aspect)
            val mediaFront = if (spec.mediaKind.isOpticalDisc) {
                buildProceduralDiscLabelBitmap(game, dossier, spec, front)
            } else {
                buildProceduralCartFrontBitmap(game, dossier, spec, front)
            }
            val mediaBack = if (spec.mediaKind.isOpticalDisc) {
                buildProceduralDiscUndersideBitmap(spec)
            } else {
                buildProceduralCartBackBitmap(game, spec)
            }
            return ResolvedPhysicalTextures(
                front = front,
                back = back,
                spine = spine,
                mediaFront = mediaFront,
                mediaBack = mediaBack,
                hasRealFront = false,
                hasRealBack = false,
                hasRealSpine = false,
                hasRealMedia = false,
            )
        }

        val snesGame = Game("snes_ct", "Chrono Trigger (USA).sfc", "snes", "tree", "doc", 4194304L)
        val snesDossier = GameDossier.localFallback(snesGame).copy(publisher = "Square Soft", esrbRating = "K-A")
        val snesSpec = PhysicalMediaSpec.forSystem("snes")
        val snesHw = ConsoleHardwareSpec.forSystem("snes", snesSpec)
        val snesTex = sampleTextures(snesGame, snesDossier, snesSpec)

        val psxGame = Game("psx_ff7", "Final Fantasy VII (USA).chd", "psx", "tree", "doc", 650000000L)
        val psxDossier = GameDossier.localFallback(psxGame).copy(publisher = "Sony Computer Entertainment", esrbRating = "T")
        val psxSpec = PhysicalMediaSpec.forSystem("psx")
        val psxHw = ConsoleHardwareSpec.forSystem("psx", psxSpec)
        val psxTex = sampleTextures(psxGame, psxDossier, psxSpec)

        val activeGame = mutableStateOf(snesGame)
        val activeSpec = mutableStateOf(snesSpec)
        val activeHw = mutableStateOf(snesHw)
        val activeTex = mutableStateOf(snesTex)
        val activeProgress = mutableStateOf(0.54f)

        compose.setContent {
            RetroHeroTheme {
                ConsoleInsertionStageContent(
                    game = activeGame.value,
                    spec = activeSpec.value,
                    hwSpec = activeHw.value,
                    textures = activeTex.value,
                    frame = ConsoleInsertionMath.evaluate(activeProgress.value, activeHw.value),
                    onSkip = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        compose.waitForIdle()
        savePng("insertion_snes_cart.png")

        activeGame.value = psxGame
        activeSpec.value = psxSpec
        activeHw.value = psxHw
        activeTex.value = psxTex
        activeProgress.value = 0.56f
        compose.waitForIdle()
        savePng("insertion_psx_disc.png")
    }

    private fun savePng(fileName: String) {
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, fileName).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
    }
}
