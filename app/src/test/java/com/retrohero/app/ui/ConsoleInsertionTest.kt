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
        assertEquals(ConsoleLoaderKind.CARTRIDGE_TOP_SLOT, ConsoleHardwareSpec.forSystem("genesis", PhysicalMediaSpec.forSystem("genesis")).loaderKind)
        assertEquals(ConsoleLoaderKind.HANDHELD_REAR_SLOT, ConsoleHardwareSpec.forSystem("nds", PhysicalMediaSpec.forSystem("nds")).loaderKind)
        assertEquals(ConsoleLoaderKind.HANDHELD_REAR_SLOT, ConsoleHardwareSpec.forSystem("3ds", PhysicalMediaSpec.forSystem("3ds")).loaderKind)
        assertEquals(ConsoleLoaderKind.HANDHELD_REAR_SLOT, ConsoleHardwareSpec.forSystem("gb", PhysicalMediaSpec.forSystem("gb")).loaderKind)
        assertEquals(ConsoleLoaderKind.HANDHELD_REAR_SLOT, ConsoleHardwareSpec.forSystem("gba", PhysicalMediaSpec.forSystem("gba")).loaderKind)
        assertEquals(ConsoleLoaderKind.SWITCH_TOP_FLAP, ConsoleHardwareSpec.forSystem("switch", PhysicalMediaSpec.forSystem("switch")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_TOP_LID, ConsoleHardwareSpec.forSystem("psx", PhysicalMediaSpec.forSystem("psx")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_TOP_LID, ConsoleHardwareSpec.forSystem("dc", PhysicalMediaSpec.forSystem("dc")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_TOP_LID, ConsoleHardwareSpec.forSystem("gc", PhysicalMediaSpec.forSystem("gc")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_SLIDE_TRAY, ConsoleHardwareSpec.forSystem("ps2", PhysicalMediaSpec.forSystem("ps2")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_REAR_UMD, ConsoleHardwareSpec.forSystem("psp", PhysicalMediaSpec.forSystem("psp")).loaderKind)
        assertEquals(ConsoleLoaderKind.DISC_SLOT_IN, ConsoleHardwareSpec.forSystem("wii", PhysicalMediaSpec.forSystem("wii")).loaderKind)
    }

    @Test
    fun evaluatesInsertionTimelineSmoothlyForCartridgeAndDiscConsoles() {
        val nesHw = ConsoleHardwareSpec.forSystem("nes", PhysicalMediaSpec.forSystem("nes"))
        val nesDoorOpen = ConsoleInsertionMath.evaluate(0.20f, nesHw)
        val nesSliding = ConsoleInsertionMath.evaluate(0.36f, nesHw)
        val nesZif = ConsoleInsertionMath.evaluate(0.64f, nesHw)
        val nesClosed = ConsoleInsertionMath.evaluate(0.92f, nesHw)
        assertTrue(nesDoorOpen.lidOpen > 0.9f)
        assertTrue(nesSliding.insertDepth in 0.3f..0.8f)
        assertTrue(nesZif.latchMotion > 0.5f)
        assertEquals(0f, nesClosed.lidOpen, 1e-2f)
        assertTrue(nesClosed.ledGlow > 0.8f)

        val snesHw = ConsoleHardwareSpec.forSystem("snes", PhysicalMediaSpec.forSystem("snes"))
        val start = ConsoleInsertionMath.evaluate(0.0f, snesHw)
        val mid = ConsoleInsertionMath.evaluate(0.50f, snesHw)
        val seated = ConsoleInsertionMath.evaluate(0.75f, snesHw)
        val end = ConsoleInsertionMath.evaluate(1.0f, snesHw)

        assertEquals(0f, start.insertDepth, 1e-3f)
        assertFalse(start.seated)
        assertEquals(0f, start.ledGlow, 1e-3f)
        assertTrue(mid.insertDepth in 0.25f..0.85f)
        assertTrue(seated.seated)
        assertTrue(seated.ledGlow > 0f)
        assertEquals(1f, end.insertDepth, 1e-3f)
        assertEquals(1f, end.ledGlow, 1e-3f)
        assertEquals(1f, end.bootFlash, 1e-3f)

        val ndsHw = ConsoleHardwareSpec.forSystem("nds", PhysicalMediaSpec.forSystem("nds"))
        val ndsSpring = ConsoleInsertionMath.evaluate(0.67f, ndsHw)
        assertTrue(ndsSpring.seated)
        assertTrue(ndsSpring.latchMotion > 0.5f)

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

        val ps2Hw = ConsoleHardwareSpec.forSystem("ps2", PhysicalMediaSpec.forSystem("ps2"))
        assertEquals(1f, ConsoleInsertionMath.evaluate(0.20f, ps2Hw).trayOut, 1e-3f)
        assertEquals(0f, ConsoleInsertionMath.evaluate(0.88f, ps2Hw).trayOut, 1e-3f)

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

        data class SampleCase(val file: String, val game: Game, val publisher: String, val progress: Float)

        val cases = listOf(
            SampleCase("insertion_nes_cart.png", Game("nes_smb3", "Super Mario Bros. 3 (USA).nes", "nes", "tree", "doc", 393216L), "Nintendo", 0.34f),
            SampleCase("insertion_snes_cart.png", Game("snes_ct", "Chrono Trigger (USA).sfc", "snes", "tree", "doc", 4194304L), "Square Soft", 0.56f),
            SampleCase("insertion_nds_card.png", Game("nds_plat", "Pokemon - Platinum Version (USA).nds", "nds", "tree", "doc", 134217728L), "Nintendo", 0.38f),
            SampleCase("insertion_n64_cart.png", Game("n64_oot", "The Legend of Zelda: Ocarina of Time (USA).z64", "n64", "tree", "doc", 33554432L), "Nintendo", 0.56f),
            SampleCase("insertion_genesis_cart.png", Game("gen_s3", "Sonic the Hedgehog 3 (USA).md", "genesis", "tree", "doc", 2097152L), "Sega", 0.54f),
            SampleCase("insertion_gb_cart.png", Game("gb_red", "Pokemon - Red Version (USA).gb", "gb", "tree", "doc", 1048576L), "Nintendo", 0.44f),
            SampleCase("insertion_gba_cart.png", Game("gba_em", "Pokemon - Emerald Version (USA).gba", "gba", "tree", "doc", 16777216L), "Nintendo", 0.42f),
            SampleCase("insertion_switch_card.png", Game("sw_botw", "The Legend of Zelda: Breath of the Wild.nsp", "switch", "tree", "doc", 14000000000L), "Nintendo", 0.36f),
            SampleCase("insertion_psx_disc.png", Game("psx_ff7", "Final Fantasy VII (USA).chd", "psx", "tree", "doc", 650000000L), "Sony Computer Entertainment", 0.48f),
            SampleCase("insertion_gc_disc.png", Game("gc_melee", "Super Smash Bros. Melee (USA).rvz", "gc", "tree", "doc", 1459978240L), "Nintendo", 0.48f),
            SampleCase("insertion_dc_disc.png", Game("dc_sa2", "Sonic Adventure 2 (USA).chd", "dc", "tree", "doc", 850000000L), "Sega", 0.48f),
            SampleCase("insertion_ps2_disc.png", Game("ps2_sotc", "Shadow of the Colossus (USA).chd", "ps2", "tree", "doc", 2200000000L), "Sony Computer Entertainment", 0.46f),
            SampleCase("insertion_psp_umd.png", Game("psp_cc", "Crisis Core: Final Fantasy VII (USA).chd", "psp", "tree", "doc", 1200000000L), "Square Enix", 0.42f),
            SampleCase("insertion_wii_disc.png", Game("wii_smg", "Super Mario Galaxy (USA).rvz", "wii", "tree", "doc", 3500000000L), "Nintendo", 0.46f),
        )

        val first = cases.first()
        val firstSpec = PhysicalMediaSpec.forSystem(first.game.systemId)
        val firstHw = ConsoleHardwareSpec.forSystem(first.game.systemId, firstSpec)
        val firstTex = sampleTextures(first.game, GameDossier.localFallback(first.game).copy(publisher = first.publisher), firstSpec)

        val activeGame = mutableStateOf(first.game)
        val activeSpec = mutableStateOf(firstSpec)
        val activeHw = mutableStateOf(firstHw)
        val activeTex = mutableStateOf(firstTex)
        val activeProgress = mutableStateOf(first.progress)

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

        cases.forEachIndexed { idx, sample ->
            if (idx > 0) {
                val spec = PhysicalMediaSpec.forSystem(sample.game.systemId)
                activeGame.value = sample.game
                activeSpec.value = spec
                activeHw.value = ConsoleHardwareSpec.forSystem(sample.game.systemId, spec)
                activeTex.value = sampleTextures(sample.game, GameDossier.localFallback(sample.game).copy(publisher = sample.publisher), spec)
                activeProgress.value = sample.progress
            }
            compose.waitForIdle()
            savePng(sample.file)
        }
    }

    private fun savePng(fileName: String) {
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, fileName).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
    }
}
