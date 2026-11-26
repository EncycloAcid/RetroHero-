package com.retrohero.app.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SavePortabilityTest {

    @Test
    fun convertsN64StandaloneEepSraAndFlaToCanonicalContainer() {
        // 1. 2 KB EEPROM (.eep)
        val eep = ByteArray(2048) { (it and 0xFF).toByte() }
        val eepRes = SavePortability.canonicalizeBytes(eep, "n64", "eep")
        assertTrue(eepRes.converted)
        assertEquals(SavePortability.N64_SRM_SIZE, eepRes.bytes.size)
        assertArrayEquals(eep, eepRes.bytes.copyOfRange(SavePortability.N64_EEP_OFFSET, SavePortability.N64_EEP_OFFSET + 2048))

        // 2. 32 KB SRAM (.sra) with 32-bit word byte-swapping
        val sra = ByteArray(SavePortability.N64_SRAM_SIZE)
        sra[0] = 0x11; sra[1] = 0x22; sra[2] = 0x33; sra[3] = 0x44
        val sraRes = SavePortability.canonicalizeBytes(sra, "n64", "sra")
        assertTrue(sraRes.converted)
        assertEquals(SavePortability.N64_SRM_SIZE, sraRes.bytes.size)
        assertEquals(0x44.toByte(), sraRes.bytes[SavePortability.N64_SRAM_OFFSET])
        assertEquals(0x33.toByte(), sraRes.bytes[SavePortability.N64_SRAM_OFFSET + 1])
        assertEquals(0x22.toByte(), sraRes.bytes[SavePortability.N64_SRAM_OFFSET + 2])
        assertEquals(0x11.toByte(), sraRes.bytes[SavePortability.N64_SRAM_OFFSET + 3])

        // 3. 128 KB FlashRAM (.fla) with 32-bit word byte-swapping
        val fla = ByteArray(SavePortability.N64_FLASH_SIZE)
        fla[0] = 0xAA.toByte(); fla[1] = 0xBB.toByte(); fla[2] = 0xCC.toByte(); fla[3] = 0xDD.toByte()
        val flaRes = SavePortability.canonicalizeBytes(fla, "n64", "fla")
        assertTrue(flaRes.converted)
        assertEquals(SavePortability.N64_SRM_SIZE, flaRes.bytes.size)
        assertEquals(0xDD.toByte(), flaRes.bytes[SavePortability.N64_FLASH_OFFSET])
        assertEquals(0xAA.toByte(), flaRes.bytes[SavePortability.N64_FLASH_OFFSET + 3])

        // 4. Already canonical 296,960 B .srm is untouched
        val already = SavePortability.canonicalizeBytes(eepRes.bytes, "n64", "srm")
        assertFalse(already.converted)
    }

    @Test
    fun normalizesGbaVba136KbPaddingAndAdaptsMgbaRtcForGpsp() {
        // VBA Next 136 KB (139,264 B) padded 64 KB Flash -> trims to exact 64 KB
        val vba64 = ByteArray(139_264) { 0xFF.toByte() }
        vba64[0] = 0x5A; vba64[100] = 0x3C
        val res64 = SavePortability.canonicalizeBytes(vba64, "gba", "srm", "vba_next")
        assertTrue(res64.converted)
        assertEquals(65_536, res64.bytes.size)
        assertEquals(0x5A.toByte(), res64.bytes[0])

        // VBA Next 136 KB with active data in upper 64 KB -> trims to exact 128 KB
        val vba128 = ByteArray(139_264) { 0x00.toByte() }
        vba128[70_000] = 0x7E
        val res128 = SavePortability.canonicalizeBytes(vba128, "gba", "srm", "vba_next")
        assertTrue(res128.converted)
        assertEquals(131_072, res128.bytes.size)
        assertEquals(0x7E.toByte(), res128.bytes[70_000])

        // mGBA 128 KB + 16 B RTC trailer -> adapts cleanly to 128 KB for gpSP
        val mgbaRtc = ByteArray(131_072 + 16) { 0x12 }
        val adaptedGpsp = SavePortability.adaptForCore(mgbaRtc, "gba", "gpsp")
        assertTrue(adaptedGpsp.converted)
        assertEquals(131_072, adaptedGpsp.bytes.size)
        val adaptedMgba = SavePortability.adaptForCore(mgbaRtc, "gba", "mgba")
        assertFalse(adaptedMgba.converted)
    }

    @Test
    fun stripsSnesCopierHeaderDesmumeFooterAndAdaptsGambatteForSameBoy() {
        // SNES 512 B copier header on 8 KB SRAM (8,704 B -> 8,192 B)
        val snesCopier = ByteArray(8_704)
        snesCopier[512] = 0x42
        val snesRes = SavePortability.canonicalizeBytes(snesCopier, "snes", "sav")
        assertTrue(snesRes.converted)
        assertEquals(8_192, snesRes.bytes.size)
        assertEquals(0x42.toByte(), snesRes.bytes[0])

        // NDS DeSmuME .dsv 122-byte ASCII footer -> raw 64 KB SRAM
        val rawNds = ByteArray(65_536) { 0x2A }
        val footer = "|<--Snip above here to create a raw sav by excluding this DeSmuME savedata footer:|dummy".toByteArray(Charsets.US_ASCII)
        val dsv = rawNds + footer
        val ndsRes = SavePortability.canonicalizeBytes(dsv, "nds", "dsv")
        assertTrue(ndsRes.converted)
        assertEquals(65_536, ndsRes.bytes.size)
        assertArrayEquals(rawNds, ndsRes.bytes)

        // GB/GBC Gambatte 32 KB + 48 B RTC -> adapted to 32 KB for SameBoy
        val gbRtc = ByteArray(32_768 + 48) { 0x33 }
        val sameBoyRes = SavePortability.adaptForCore(gbRtc, "gbc", "sameboy")
        assertTrue(sameBoyRes.converted)
        assertEquals(32_768, sameBoyRes.bytes.size)
    }

    @Test
    fun prepareForCoreImportsExternalSavAndCommitsCanonicalBack() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val paths = AppPaths(ctx)
        val game = Game("id-emerald", "Pokemon Emerald (USA).gba", "gba", "local", "/tmp", 16_777_216L)
        val gbaDir = File(paths.saves, "gba").apply { mkdirs() }
        // Clean slate
        paths.saveRam(game).delete()
        val externalSav = File(gbaDir, "${game.baseName}.sav")
        val padded = ByteArray(139_264) { 0xFF.toByte() }.also { it[0] = 0x77; it[80_000] = 0x99.toByte() }
        externalSav.writeBytes(padded)

        val prepared = SavePortability.prepareForCore(game, "gpsp", paths)
        assertTrue("Should import and convert external .sav", prepared.converted)
        assertTrue(prepared.canonicalFile.exists())
        assertEquals(131_072L, prepared.canonicalFile.length())
        assertEquals(0x77.toByte(), prepared.canonicalFile.readBytes()[0])

        // Simulate core updating SRAM and committing back
        val updated = prepared.activeFile.readBytes().also { it[1] = 0x55 }
        prepared.activeFile.writeBytes(updated)
        assertTrue(SavePortability.commitFromCore(prepared.activeFile, prepared.canonicalFile, "gba", "gpsp", cleanupTemp = true))
        assertEquals(0x55.toByte(), prepared.canonicalFile.readBytes()[1])

        // Cleanup
        externalSav.delete()
        prepared.canonicalFile.delete()
    }
}
