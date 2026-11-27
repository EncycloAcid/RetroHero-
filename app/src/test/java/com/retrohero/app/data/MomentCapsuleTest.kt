package com.retrohero.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MomentCapsuleTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun exportsAndExtractsRoundTrip() {
        val stateBytes = byteArrayOf(10, 20, 30, 40, 50)
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2)
        val stateIn = File(tmp.root, "in.state").apply { writeBytes(stateBytes) }
        val pngIn = File(tmp.root, "in.png").apply { writeBytes(pngBytes) }

        val manifest = MomentManifest(
            title = "Beat Ridley with 1 E-Tank",
            note = "No saves allowed!",
            systemId = "snes",
            coreId = "snes9x",
            gameTitle = "Super Metroid",
            gameBaseName = "Super Metroid (USA)",
            createdAt = 123456789L,
        )

        val baos = ByteArrayOutputStream()
        MomentCapsule.export(baos, manifest, stateIn, pngIn)

        val outDir = tmp.newFolder("extracted")
        val extracted = MomentCapsule.extract(ByteArrayInputStream(baos.toByteArray()), outDir)

        assertEquals(manifest, extracted.manifest)
        assertArrayEquals(stateBytes, extracted.stateFile.readBytes())
        assertNotNull(extracted.previewFile)
        assertArrayEquals(pngBytes, extracted.previewFile!!.readBytes())
    }

    @Test
    fun matchesGamesByBaseNameOrTitleWithinSameSystem() {
        val snesGame = Game(
            id = "snes:sm",
            fileName = "Super Metroid (USA).sfc",
            systemId = "snes",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 1024L,
        )
        val gbaGame = Game(
            id = "gba:sm",
            fileName = "Super Metroid.gba",
            systemId = "gba",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 1024L,
        )

        val exact = MomentManifest("Boss", "", "snes", "snes9x", "Super Metroid", "Super Metroid (USA)")
        assertEquals(snesGame, MomentCapsule.findMatchingGame(exact, listOf(gbaGame, snesGame)))

        val regionDiff = MomentManifest("Boss", "", "snes", "snes9x", "Super Metroid", "Super Metroid (Europe)")
        assertEquals(snesGame, MomentCapsule.findMatchingGame(regionDiff, listOf(gbaGame, snesGame)))

        val wrongSystem = MomentManifest("Boss", "", "nes", "fceumm", "Super Metroid", "Super Metroid")
        assertNull(MomentCapsule.findMatchingGame(wrongSystem, listOf(gbaGame, snesGame)))
    }
}
