package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class RomPatchLabTest {

    private fun fakeGame(title: String, systemId: String) = Game(
        id = "$systemId:$title",
        fileName = "$title (Japan).rom",
        systemId = systemId,
        treeUri = "tree",
        parentDocId = "doc",
        size = 1024L,
    )

    private fun buildSimpleIpsPatch(offset: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("PATCH".toByteArray(Charsets.US_ASCII))
        out.write((offset shr 16) and 0xFF)
        out.write((offset shr 8) and 0xFF)
        out.write(offset and 0xFF)
        out.write((payload.size shr 8) and 0xFF)
        out.write(payload.size and 0xFF)
        out.write(payload)
        out.write("EOF".toByteArray(Charsets.US_ASCII))
        return out.toByteArray()
    }

    @Test
    fun inspectBaseRomDetectsSnesCopierHeaderAndNesInesHeader() {
        val headerlessSnes = ByteArray(0x8000) { 0x11 }
        val fpClean = RomPatchLab.inspectBaseRom(headerlessSnes, "snes")
        assertFalse(fpClean.hasCopierHeader)
        assertTrue(fpClean.headerStatus.contains("Headerless SNES"))
        assertEquals(8, fpClean.crc32Hex.length)

        val headeredSnes = ByteArray(0x8000 + 512) { 0x22 }
        val fpHeadered = RomPatchLab.inspectBaseRom(headeredSnes, "snes")
        assertTrue(fpHeadered.hasCopierHeader)
        assertTrue(fpHeadered.headerStatus.contains("512B copier header"))

        val inesRom = ByteArray(1024) { 0x00 }.apply {
            this[0] = 'N'.code.toByte()
            this[1] = 'E'.code.toByte()
            this[2] = 'S'.code.toByte()
            this[3] = 0x1A.toByte()
        }
        val fpNes = RomPatchLab.inspectBaseRom(inesRom, "nes")
        assertTrue(fpNes.hasInesHeader)
        assertTrue(fpNes.headerStatus.contains("iNES 16B"))
    }

    @Test
    fun verifyPatchDetectsValidIpsAndRejectsInvalidPatch() {
        val rom = ByteArray(4096) { 0x00 }
        val ips = buildSimpleIpsPatch(offset = 16, payload = byteArrayOf(0x42, 0x43, 0x44))

        val ok = RomPatchLab.verifyPatch(rom, ips, "gba", "translation.ips")
        assertEquals(PatchCompatStatus.COMPATIBLE, ok.status)
        assertEquals(PatchFormat.IPS, ok.format)
        assertNotNull(ok.patchedCrc32Hex)
        assertTrue(ok.patchedCrc32Hex != ok.sourceCrc32Hex)

        val bad = RomPatchLab.verifyPatch(rom, "NOT_A_PATCH".toByteArray(), "gba", "broken.bin")
        assertEquals(PatchCompatStatus.INCOMPATIBLE, bad.status)
        assertNull(bad.patchedCrc32Hex)
        assertTrue(bad.message.isNotBlank())
    }

    @Test
    fun notablePatchCatalogMatchesClassicTranslationsAndQolHacks() {
        val mother3 = NotablePatchCatalog.forGame(fakeGame("Mother 3", "gba"))
        assertTrue(mother3.any { "English" in it.title })

        val fe6 = NotablePatchCatalog.forGame(fakeGame("Fire Emblem - Fuuin no Tsurugi", "gba"))
        assertTrue(fe6.any { "Binding Blade" in it.title })

        val ct = NotablePatchCatalog.forGame(fakeGame("Chrono Trigger", "snes"))
        assertTrue(ct.any { "Chrono Trigger+" in it.title })

        val sm = NotablePatchCatalog.forGame(fakeGame("Super Metroid", "snes"))
        assertTrue(sm.any { "Redux" in it.title })

        val poke = NotablePatchCatalog.forGame(fakeGame("Pokémon Emerald", "gba"))
        assertTrue(poke.isNotEmpty())
    }
}
