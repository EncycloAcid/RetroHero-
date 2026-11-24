package com.retrohero.app.data

import androidx.compose.ui.geometry.Offset
import com.retrohero.app.emulator.computeDeltaLayout
import com.retrohero.app.emulator.dpadInputsForPoint
import com.retrohero.app.emulator.thumbstickOffsetForPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class DeltaSkinsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val sampleGbaInfo = """
        {
          "name": "Instrument GBA",
          "identifier": "com.example.gba.instrument",
          "gameTypeIdentifier": "com.rileytestut.delta.game.gba",
          "debug": true,
          "representations": {
            "iphone": {
              "edgeToEdge": {
                "portrait": {
                  "assets": { "resizable": "portrait.pdf" },
                  "mappingSize": { "width": 414, "height": 896 },
                  "extendedEdges": { "top": 10, "bottom": 10, "left": 10, "right": 10 },
                  "translucent": false,
                  "items": [
                    {
                      "inputs": { "up": "up", "down": "down", "left": "left", "right": "right" },
                      "frame": { "x": 30, "y": 500, "width": 120, "height": 120 }
                    },
                    {
                      "inputs": ["a"],
                      "frame": { "x": 310, "y": 540, "width": 50, "height": 50 },
                      "extendedEdges": { "right": 25 }
                    },
                    {
                      "inputs": ["a", "b", "start", "select"],
                      "frame": { "x": 180, "y": 760, "width": 54, "height": 30 }
                    },
                    {
                      "inputs": ["toggleFastForward"],
                      "frame": { "x": 20, "y": 760, "width": 40, "height": 30 }
                    },
                    {
                      "inputs": ["menu"],
                      "frame": { "x": 350, "y": 760, "width": 40, "height": 30 }
                    }
                  ],
                  "screens": [
                    {
                      "inputFrame": { "x": 0, "y": 0, "width": 240, "height": 160 },
                      "outputFrame": { "x": 12, "y": 60, "width": 390, "height": 260 }
                    }
                  ]
                },
                "landscape": {
                  "assets": {
                    "small": "land_s.png",
                    "medium": "land_m.png",
                    "large": "land_l.png"
                  },
                  "mappingSize": { "width": 896, "height": 414 },
                  "translucent": true,
                  "items": [
                    {
                      "thumbstick": { "name": "stick.png", "width": 80, "height": 80 },
                      "inputs": {
                        "up": "analogStickUp",
                        "down": "analogStickDown",
                        "left": "analogStickLeft",
                        "right": "analogStickRight"
                      },
                      "frame": { "x": 40, "y": 200, "width": 100, "height": 100 }
                    }
                  ]
                }
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun parsesFullDeltaInfoJsonAndExtendedEdges() {
        val skin = DeltaSkins.parseInfo(sampleGbaInfo, tmp.root)
        assertEquals("Instrument GBA", skin.name)
        assertEquals("com.example.gba.instrument", skin.identifier)
        assertEquals(listOf("gba"), skin.systemIds)
        assertTrue(skin.debug)

        val portrait = skin.representations.pick(landscape = false, tablet = false, tallPhone = true)
        assertNotNull(portrait)
        portrait!!
        assertEquals("portrait.pdf", portrait.assets.bestAsset())
        assertEquals(414f, portrait.mappingSize.width, 0.01f)
        assertEquals(896f, portrait.mappingSize.height, 0.01f)
        assertFalse(portrait.translucent)
        assertEquals(1, portrait.screens.size)
        assertEquals(390f, portrait.screens[0].outputFrame.width, 0.01f)

        // Button A overrides only `right` to 25 and inherits 10 for top/bottom/left.
        val btnA = portrait.items[1]
        assertEquals(DeltaRect(300f, 530f, 85f, 70f), btnA.hitRect)

        // Combo button has all 4 inputs.
        val combo = portrait.items[2].inputs as DeltaInputs.Buttons
        assertEquals(listOf("a", "b", "start", "select"), combo.ids)

        // Landscape selects `large` PNG when no PDF is present and respects `translucent`.
        val landscape = skin.representations.pick(landscape = true, tablet = false, tallPhone = true)
        assertNotNull(landscape)
        landscape!!
        assertEquals("land_l.png", landscape.assets.bestAsset())
        assertTrue(landscape.translucent)
        assertNotNull(landscape.items[0].thumbstick)
    }

    @Test
    fun importsZippedDeltaSkinAndHandlesWrapperFolder() {
        val storeDir = tmp.newFolder("delta_skins")
        val bytes = buildZip(
            "__MACOSX/._info.json" to byteArrayOf(1, 2, 3),
            "WrappedSkin/info.json" to sampleGbaInfo.toByteArray(),
            "WrappedSkin/portrait.pdf" to "%PDF-1.4".toByteArray(),
            "WrappedSkin/land_l.png" to byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
        )

        val imported = DeltaSkins.import(storeDir, ByteArrayInputStream(bytes))
        assertEquals("Instrument GBA", imported.name)
        assertTrue(imported.assetFile("portrait.pdf").isFile)
        assertEquals(1, DeltaSkins.forSystem(storeDir, "gba").size)
        assertEquals(0, DeltaSkins.forSystem(storeDir, "snes").size)

        // Re-importing the same identifier replaces in-place without duplicates.
        DeltaSkins.import(storeDir, ByteArrayInputStream(bytes))
        assertEquals(1, DeltaSkins.list(storeDir).size)

        DeltaSkins.delete(imported)
        assertEquals(0, DeltaSkins.list(storeDir).size)
    }

    @Test
    fun importsDotDotIdentifierInsideSkinRootWithoutDeletingSiblingFiles() {
        val storeDir = tmp.newFolder("delta_skins_dotdot")
        val outside = File(tmp.root, "unrelated-save.state").apply { writeText("keep") }
        val info = sampleGbaInfo.replace("com.example.gba.instrument", "..")
        val archive = buildZip(
            "info.json" to info.toByteArray(),
            "portrait.pdf" to "%PDF-1.4".toByteArray(),
        )

        val imported = DeltaSkins.import(storeDir, ByteArrayInputStream(archive))

        assertEquals(storeDir.canonicalFile, imported.dir.canonicalFile.parentFile)
        assertEquals("keep", outside.readText())
    }

    @Test
    fun sanitizedIdentifierCollisionsGetDistinctSkinDirectories() {
        val storeDir = tmp.newFolder("delta_skins_collisions")
        fun archive(identifier: String) = buildZip(
            "info.json" to sampleGbaInfo.replace("com.example.gba.instrument", identifier).toByteArray(),
            "portrait.pdf" to "%PDF-1.4".toByteArray(),
        )

        val first = DeltaSkins.import(storeDir, ByteArrayInputStream(archive("com.example/a")))
        val second = DeltaSkins.import(storeDir, ByteArrayInputStream(archive("com.example?a")))

        assertTrue(first.dir != second.dir)
        assertEquals(2, DeltaSkins.list(storeDir).size)
    }

    @Test
    fun gbcSkinMatchesBothGbAndGbc() {
        assertEquals(listOf("gbc", "gb"), DeltaSkins.systemIdsFor("com.rileytestut.delta.game.gbc"))
        assertEquals(listOf("gba"), DeltaSkins.systemIdsFor("com.rileytestut.Delta.game.gba"))
        assertEquals(listOf("nds"), DeltaSkins.systemIdsFor("com.rileytestut.delta.game.ds"))
        assertEquals(listOf("genesis", "segacd", "32x"), DeltaSkins.systemIdsFor("com.rileytestut.delta.game.genesis"))
        assertEquals(listOf("gg"), DeltaSkins.systemIdsFor("com.rileytestut.delta.game.gg"))
        assertEquals(listOf("sms"), DeltaSkins.systemIdsFor("com.ignited.game.sms"))
    }

    @Test
    fun mapsStandardGenesisAndSpecialInputs() {
        assertEquals(DeltaMappedInput.Button(RetroPad.A), DeltaSkins.mapInput("a", "gba"))
        assertEquals(DeltaMappedInput.Button(RetroPad.B), DeltaSkins.mapInput("b", "gba"))
        assertEquals(DeltaMappedInput.Action(DeltaAction.MENU), DeltaSkins.mapInput("menu", "gba"))
        assertEquals(DeltaMappedInput.Action(DeltaAction.QUICK_SAVE), DeltaSkins.mapInput("quickSave", "gba"))
        assertEquals(DeltaMappedInput.Action(DeltaAction.QUICK_LOAD), DeltaSkins.mapInput("quickLoad", "gba"))
        assertEquals(DeltaMappedInput.Action(DeltaAction.FAST_FORWARD_HOLD), DeltaSkins.mapInput("fastForward", "gba"))
        assertEquals(DeltaMappedInput.Action(DeltaAction.FAST_FORWARD_TOGGLE), DeltaSkins.mapInput("toggleFastForward", "gba"))

        // Genesis 6-button mapping
        assertEquals(DeltaMappedInput.Button(RetroPad.Y), DeltaSkins.mapInput("a", "genesis"))
        assertEquals(DeltaMappedInput.Button(RetroPad.B), DeltaSkins.mapInput("b", "genesis"))
        assertEquals(DeltaMappedInput.Button(RetroPad.A), DeltaSkins.mapInput("c", "genesis"))
        assertEquals(DeltaMappedInput.Button(RetroPad.SELECT), DeltaSkins.mapInput("mode", "genesis"))
    }

    @Test
    fun computesDpad3x3GridAndThumbstickClamping() {
        val dpad = DeltaItem(
            inputs = DeltaInputs.Directional(up = "up", down = "down", left = "left", right = "right"),
            frame = DeltaRect(0f, 0f, 90f, 90f),
            extendedEdges = DeltaEdges(10f, 10f, 10f, 10f),
        )
        // Center cell is neutral
        assertEquals(emptyList<String>(), dpadInputsForPoint(dpad, Offset(45f, 45f)))
        // Top-left corner activates both left and up
        assertEquals(listOf("left", "up"), dpadInputsForPoint(dpad, Offset(10f, 10f)))
        // Extended edge outside frame but inside hitRect still registers
        assertEquals(listOf("right"), dpadInputsForPoint(dpad, Offset(95f, 45f)))
        // Outside hitRect registers nothing
        assertEquals(emptyList<String>(), dpadInputsForPoint(dpad, Offset(105f, 45f)))

        val stick = DeltaItem(
            inputs = DeltaInputs.Directional(up = "analogStickUp", down = "analogStickDown", left = "analogStickLeft", right = "analogStickRight"),
            frame = DeltaRect(0f, 0f, 100f, 100f),
            extendedEdges = DeltaEdges(20f, 20f, 20f, 20f),
            thumbstick = DeltaThumbstick("stick.png", 60f, 60f),
        )
        // Center is (0, 0)
        assertEquals(Offset(0f, 0f), thumbstickOffsetForPoint(stick, Offset(50f, 50f)))
        // Beyond radius clamps to unit length
        val clamped = thumbstickOffsetForPoint(stick, Offset(115f, 50f))!!
        assertEquals(1f, clamped.x, 0.001f)
        assertEquals(0f, clamped.y, 0.001f)
        // Outside hitRect returns null
        assertNull(thumbstickOffsetForPoint(stick, Offset(130f, 50f)))
    }

    @Test
    fun computesScreenLayoutForExplicitAndImplicitScreens() {
        val skin = DeltaSkins.parseInfo(sampleGbaInfo, tmp.root)
        val portrait = skin.representations.pick(landscape = false, tablet = false, tallPhone = true)!!
        val layout = computeDeltaLayout(1080f, 2340f, portrait, landscape = false)
        assertTrue(layout.explicitScreen)
        assertTrue(layout.screenRect.width > 0f)
        assertTrue(layout.screenRect.height > 0f)
        assertTrue(layout.screenRect.left >= layout.skinRect.left)
        assertTrue(layout.screenRect.right <= layout.skinRect.right + 0.5f)

        val landscape = skin.representations.pick(landscape = true, tablet = false, tallPhone = true)!!
        val landLayout = computeDeltaLayout(2340f, 1080f, landscape, landscape = true)
        assertFalse(landLayout.explicitScreen)
        assertEquals(2340f, landLayout.screenRect.width, 0.1f)
        assertEquals(1080f, landLayout.screenRect.height, 0.1f)
    }

    private fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
