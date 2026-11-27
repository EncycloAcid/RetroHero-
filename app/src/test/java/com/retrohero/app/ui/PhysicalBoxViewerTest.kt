package com.retrohero.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Box3dMath
import com.retrohero.app.data.BoxFaceId
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameDossier
import com.retrohero.app.data.PhysicalMediaKind
import com.retrohero.app.data.PhysicalMediaSpec
import com.retrohero.app.data.RetailCaseStyle
import com.retrohero.app.ui.components.M3PhysicalBoxViewerSection
import com.retrohero.app.ui.components.PhysicalViewerBitmaps
import com.retrohero.app.ui.components.PhysicalViewerSubject
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class PhysicalBoxViewerTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    @Test
    fun mapsConsolesToAuthenticPhysicalCasesAndMedia() {
        val snes = PhysicalMediaSpec.forSystem("snes")
        assertEquals(RetailCaseStyle.CARDBOARD_LANDSCAPE, snes.caseStyle)
        assertEquals(PhysicalMediaKind.CART_SNES, snes.mediaKind)

        val psx = PhysicalMediaSpec.forSystem("psx")
        assertEquals(RetailCaseStyle.JEWEL_CASE_CD, psx.caseStyle)
        assertEquals(PhysicalMediaKind.OPTICAL_DISC, psx.mediaKind)
        assertTrue("PS1 discs have iconic black polycarbonate undersides", psx.blackDiscUnderside)

        val ps2 = PhysicalMediaSpec.forSystem("ps2")
        assertEquals(RetailCaseStyle.KEEP_CASE_DVD, ps2.caseStyle)
        assertEquals(PhysicalMediaKind.OPTICAL_DISC, ps2.mediaKind)
        assertFalse(ps2.blackDiscUnderside)

        val psp = PhysicalMediaSpec.forSystem("psp")
        assertEquals(PhysicalMediaKind.UMD_DISC, psp.mediaKind)

        val gc = PhysicalMediaSpec.forSystem("gc")
        assertEquals(PhysicalMediaKind.MINI_DISC_GC, gc.mediaKind)

        val nds = PhysicalMediaSpec.forSystem("nds")
        assertEquals(RetailCaseStyle.HANDHELD_SNAP_CASE, nds.caseStyle)
        assertEquals(PhysicalMediaKind.CART_DS_SWITCH, nds.mediaKind)

        val genesis = PhysicalMediaSpec.forSystem("genesis")
        assertEquals(RetailCaseStyle.CLAMSHELL_PLASTIC, genesis.caseStyle)
        assertEquals(PhysicalMediaKind.CART_SEGA, genesis.mediaKind)
    }

    @Test
    fun box3dProjectionCullsBackFacesAndOrientsFrontSpineAndBackUnmirrored() {
        // 0° yaw: straight-on Front Cover
        val frontPose = Box3dMath.projectBox(100f, 140f, 20f, yawDeg = 0f, pitchDeg = 0f, centerX = 200f, centerY = 200f, focalPx = 800f)
        assertEquals(listOf(BoxFaceId.FRONT), frontPose.map { it.face })

        // +24° LaunchBox 3/4 pose: both Front Cover and Left Spine visible simultaneously!
        val threeQuarter = Box3dMath.projectBox(100f, 140f, 24f, yawDeg = 24f, pitchDeg = -14f, centerX = 200f, centerY = 200f, focalPx = 800f)
        val faces24 = threeQuarter.map { it.face }.toSet()
        assertTrue(BoxFaceId.FRONT in faces24)
        assertTrue(BoxFaceId.LEFT_SPINE in faces24)
        assertTrue(BoxFaceId.TOP in faces24)
        assertFalse(BoxFaceId.BACK in faces24)

        // +90° yaw: Left Spine centered
        val spinePose = Box3dMath.projectBox(100f, 140f, 24f, yawDeg = 90f, pitchDeg = 0f, centerX = 200f, centerY = 200f, focalPx = 800f)
        assertEquals(listOf(BoxFaceId.LEFT_SPINE), spinePose.map { it.face })

        // +180° yaw: Back of Box centered and un-mirrored (TL.x < TR.x and TL.y < BL.y)
        val backPose = Box3dMath.projectBox(100f, 140f, 24f, yawDeg = 180f, pitchDeg = 0f, centerX = 200f, centerY = 200f, focalPx = 800f)
        assertEquals(listOf(BoxFaceId.BACK), backPose.map { it.face })
        val pts = backPose.single().dstPoints
        assertTrue("Back cover must not be horizontally mirrored", pts[0] < pts[2])
        assertTrue("Back cover must be right-side up", pts[1] < pts[7])
    }

    @Test
    fun slicesLaunchBoxFullWraparoundCoverAndRotatesHorizontalSpines() {
        val full = Bitmap.createBitmap(1000, 680, Bitmap.Config.ARGB_8888)
        val c = Canvas(full)
        val p = Paint()
        p.color = AndroidColor.RED
        c.drawRect(0f, 0f, 460f, 680f, p)
        p.color = AndroidColor.GREEN
        c.drawRect(460f, 0f, 540f, 680f, p)
        p.color = AndroidColor.BLUE
        c.drawRect(540f, 0f, 1000f, 680f, p)

        val sliced = PhysicalMediaSpec.sliceFullBoxCover(full, spineFraction = 0.08f)
        assertNotNull(sliced)
        assertEquals(AndroidColor.RED, sliced!!.first.getPixel(20, 340))
        assertEquals(AndroidColor.GREEN, sliced.second.getPixel(sliced.second.width / 2, 340))
        assertEquals(AndroidColor.BLUE, sliced.third.getPixel(sliced.third.width - 20, 340))

        val horizSpine = Bitmap.createBitmap(800, 120, Bitmap.Config.ARGB_8888)
        val vertical = PhysicalMediaSpec.orientSpineVertical(horizSpine)
        assertTrue("Horizontal spine scan should auto-rotate to vertical", vertical.height > vertical.width)
    }

    @Test
    fun renders3dBoxFrontBackCartridgeAndPs1BlackDiscScreenshots() {
        val snesGame = Game("snes_ct", "Chrono Trigger (USA).sfc", "snes", "tree", "doc", 4194304L)
        val snesDossier = GameDossier.localFallback(snesGame).copy(
            title = "Chrono Trigger",
            developer = "Square",
            publisher = "Square Soft",
            releaseDate = "August 11, 1995",
            releaseYear = "1995",
            genre = "Role-Playing · Time Travel",
            modes = "Single-player · Battery Save",
            esrbRating = "K-A",
            promoQuote = "JOURNEY ACROSS TIME TO SAVE THE FUTURE · 1995",
            summary = "When a newly-invented teleporter malfunctions at the Millennial Fair, young Crono is thrust through a dimensional gate across prehistoric eras, dark ages, and a ruined future to stop Lavos before the planet's destruction.",
            sourceLabel = "LaunchBox",
            isLive = true,
        )

        val psxGame = Game("psx_ff7", "Final Fantasy VII (USA).chd", "psx", "tree", "doc", 650000000L)
        val psxDossier = GameDossier.localFallback(psxGame).copy(
            title = "Final Fantasy VII",
            developer = "Square",
            publisher = "Sony Computer Entertainment",
            releaseYear = "1997",
            esrbRating = "T",
            sourceLabel = "LaunchBox",
            isLive = true,
        )

        val shotA = Bitmap.createBitmap(320, 224, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawColor(AndroidColor.rgb(24, 58, 128))
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 190, 60) }
                drawCircle(160f, 112f, 64f, paint)
            }
        }
        val shotB = Bitmap.createBitmap(320, 224, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawColor(AndroidColor.rgb(68, 22, 92))
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(0, 229, 255) }
                drawRect(70f, 60f, 250f, 170f, paint)
            }
        }

        val activeSystem = mutableStateOf("snes")
        val activeSubject = mutableStateOf(PhysicalViewerSubject.RETAIL_BOX)
        val activeYaw = mutableFloatStateOf(26f)

        compose.setContent {
            RetroHeroTheme {
                val isPsx = activeSystem.value == "psx"
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 12.dp),
                ) {
                    // Key by pose so each test snapshot initializes cleanly
                    androidx.compose.runtime.key(activeSystem.value, activeSubject.value, activeYaw.floatValue) {
                        M3PhysicalBoxViewerSection(
                            game = if (isPsx) psxGame else snesGame,
                            dossier = if (isPsx) psxDossier else snesDossier,
                            initialSubject = activeSubject.value,
                            initialYawDeg = activeYaw.floatValue,
                            overrideBitmaps = PhysicalViewerBitmaps(screenshot1 = shotA, screenshot2 = shotB),
                        )
                    }
                }
            }
        }

        fun saveShot(name: String) {
            compose.waitForIdle()
            val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
            File(outDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        // 1. SNES 3/4 3D Retail Box + Spine
        saveShot("box3d_snes_three_quarter.png")

        // 2. SNES Flipped 180° Back-of-the-Box Blurb + Screenshots + ESRB + Barcode
        compose.runOnIdle { activeYaw.floatValue = 172f }
        saveShot("box3d_snes_back_blurb.png")

        // 3. SNES 3D Physical Cartridge
        compose.runOnIdle {
            activeSubject.value = PhysicalViewerSubject.GAME_MEDIA
            activeYaw.floatValue = 22f
        }
        saveShot("box3d_snes_cartridge.png")

        // 4. PS1 3D Black Polycarbonate Data Underside Disc
        compose.runOnIdle {
            activeSystem.value = "psx"
            activeSubject.value = PhysicalViewerSubject.GAME_MEDIA
            activeYaw.floatValue = 162f
        }
        saveShot("box3d_psx_black_disc.png")
    }
}
