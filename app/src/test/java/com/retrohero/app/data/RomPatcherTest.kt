package com.retrohero.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RomPatcherTest {

    // -------------------------------------------------------------------------
    // Helpers to construct valid binary IPS / BPS / UPS / VCDIFF patches
    // -------------------------------------------------------------------------

    private fun writeVarInt(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (true) {
            val x = (v and 0x7FL).toInt()
            v = v ushr 7
            if (v == 0L) {
                out.write(x or 0x80)
                break
            }
            out.write(x)
            v--
        }
    }

    private fun writeU32LE(out: ByteArrayOutputStream, value: Long) {
        out.write((value and 0xFF).toInt())
        out.write(((value ushr 8) and 0xFF).toInt())
        out.write(((value ushr 16) and 0xFF).toInt())
        out.write(((value ushr 24) and 0xFF).toInt())
    }

    private fun write7BitInt(out: ByteArrayOutputStream, value: Int) {
        val bytes = ArrayList<Int>()
        var v = value
        bytes += (v and 0x7F)
        v = v ushr 7
        while (v > 0) {
            bytes += (v and 0x7F) or 0x80
            v = v ushr 7
        }
        for (i in bytes.lastIndex downTo 0) out.write(bytes[i])
    }

    private fun buildSimpleBps(source: ByteArray, target: ByteArray, metadata: String = ""): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("BPS1".toByteArray(Charsets.US_ASCII))
        writeVarInt(body, source.size.toLong())
        writeVarInt(body, target.size.toLong())
        val metaBytes = metadata.toByteArray(Charsets.UTF_8)
        writeVarInt(body, metaBytes.size.toLong())
        body.write(metaBytes)

        // Emit SourceRead for matching prefix, then TargetRead for the rest
        var sameLen = 0
        while (sameLen < source.size && sameLen < target.size && source[sameLen] == target[sameLen]) {
            sameLen++
        }
        if (sameLen > 0) {
            writeVarInt(body, ((sameLen - 1).toLong() shl 2) or 0L) // SourceRead
        }
        val rem = target.size - sameLen
        if (rem > 0) {
            writeVarInt(body, ((rem - 1).toLong() shl 2) or 1L) // TargetRead
            body.write(target, sameLen, rem)
        }
        writeU32LE(body, RomPatcher.crc32(source))
        writeU32LE(body, RomPatcher.crc32(target))
        val preFooter = body.toByteArray()
        val full = ByteArrayOutputStream()
        full.write(preFooter)
        writeU32LE(full, RomPatcher.crc32(preFooter))
        return full.toByteArray()
    }

    private fun buildSimpleUps(source: ByteArray, target: ByteArray): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("UPS1".toByteArray(Charsets.US_ASCII))
        writeVarInt(body, source.size.toLong())
        writeVarInt(body, target.size.toLong())

        var pos = 0
        val maxLen = maxOf(source.size, target.size)
        while (pos < maxLen) {
            val start = pos
            while (pos < maxLen) {
                val sb = if (pos < source.size) source[pos].toInt() and 0xFF else 0
                val tb = if (pos < target.size) target[pos].toInt() and 0xFF else 0
                if (sb != tb) break
                pos++
            }
            if (pos >= maxLen) break
            writeVarInt(body, (pos - start).toLong())
            while (pos < maxLen) {
                val sb = if (pos < source.size) source[pos].toInt() and 0xFF else 0
                val tb = if (pos < target.size) target[pos].toInt() and 0xFF else 0
                val x = sb xor tb
                if (x == 0) break
                body.write(x)
                pos++
            }
            body.write(0)
            pos++
        }

        writeU32LE(body, RomPatcher.crc32(source))
        writeU32LE(body, RomPatcher.crc32(target))
        val pre = body.toByteArray()
        val full = ByteArrayOutputStream()
        full.write(pre)
        writeU32LE(full, RomPatcher.crc32(pre))
        return full.toByteArray()
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun ipsLiteralRleAndExpansionWorkInMemory() {
        val source = ByteArray(16) { it.toByte() }
        val patch = ByteArrayOutputStream().apply {
            write("PATCH".toByteArray(Charsets.US_ASCII))
            // Offset 2, size 3 -> [0xAA, 0xBB, 0xCC]
            write(byteArrayOf(0x00, 0x00, 0x02, 0x00, 0x03, 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()))
            // Offset 14, RLE size 6, byte 0x77 (expands ROM to 20 bytes)
            write(byteArrayOf(0x00, 0x00, 0x0E, 0x00, 0x00, 0x00, 0x06, 0x77))
            write("EOF".toByteArray(Charsets.US_ASCII))
        }.toByteArray()

        val result = RomPatcher.applyInMemory(source, patch, systemId = "gba", fileName = "hack.ips")
        assertEquals(PatchFormat.IPS, result.format)
        assertEquals(20, result.data.size)
        assertEquals(0xAA.toByte(), result.data[2])
        assertEquals(0xBB.toByte(), result.data[3])
        assertEquals(0xCC.toByte(), result.data[4])
        for (i in 14 until 20) assertEquals(0x77.toByte(), result.data[i])
        // Original source array untouched!
        assertEquals(2.toByte(), source[2])
    }

    @Test
    fun bpsPatchesCleanlyAndVerifiesCrc32AndMetadata() {
        val source = "MOTHER 3 JAPANESE ROM DATA 1234567890".toByteArray(Charsets.US_ASCII)
        val target = "MOTHER 3 ENGLISH v1.3 TRANSLATION!!!!".toByteArray(Charsets.US_ASCII)
        val patch = buildSimpleBps(source, target, metadata = "Mother 3 Fan Translation")

        val result = RomPatcher.applyInMemory(source, patch, systemId = "gba", fileName = "mother3_en.bps")
        assertEquals(PatchFormat.BPS, result.format)
        assertArrayEquals(target, result.data)
        assertEquals("Mother 3 Fan Translation", result.metadata)
        assertEquals(RomPatcher.crc32Hex(target), result.targetCrc32Hex)
    }

    @Test
    fun bpsAutoStrips512ByteSnesCopierHeader() {
        val unheadered = ByteArray(0x8000) { (it * 7 and 0xFF).toByte() }
        val target = unheadered.copyOf().also { it[0x100] = 0x42; it[0x101] = 0x99.toByte() }
        // Patch expects unheadered No-Intro SNES ROM
        val patch = buildSimpleBps(unheadered, target)

        // User supplies a 512-byte SMC-headered SNES ROM
        val headered = ByteArray(unheadered.size + 512).also {
            System.arraycopy(unheadered, 0, it, 512, unheadered.size)
        }

        val result = RomPatcher.applyInMemory(headered, patch, systemId = "snes", fileName = "kaizo.bps")
        assertArrayEquals(target, result.data)
        assertNotNull(result.headerNote)
        assertTrue(result.headerNote!!.contains("512-byte SNES"))
    }

    @Test
    fun bpsAutoAlignsHeaderedPatchOntoUnheaderedSnesRom() {
        val unheadered = ByteArray(0x8000) { (it * 3 and 0xFF).toByte() }
        val headeredSource = ByteArray(unheadered.size + 512).also {
            System.arraycopy(unheadered, 0, it, 512, unheadered.size)
        }
        val headeredTarget = headeredSource.copyOf().also {
            it[512 + 0x200] = 0x55
            it[512 + 0x201] = 0x66
        }
        // Patch expects a 512-byte headered SNES ROM
        val patch = buildSimpleBps(headeredSource, headeredTarget)

        // User has a clean unheadered No-Intro SNES ROM
        val result = RomPatcher.applyInMemory(unheadered, patch, systemId = "snes", fileName = "smw_hack.bps")
        assertEquals(unheadered.size, result.data.size)
        assertEquals(0x55.toByte(), result.data[0x200])
        assertEquals(0x66.toByte(), result.data[0x201])
        assertNotNull(result.headerNote)
    }

    @Test
    fun upsPatchesAndAutoHandlesSnesHeader() {
        val unheadered = ByteArray(0x8000) { (it and 0xFF).toByte() }
        val target = ByteArray(0x8010) { idx ->
            when {
                idx < unheadered.size && idx in 10..15 -> 0xFE.toByte()
                idx < unheadered.size -> unheadered[idx]
                else -> 0xAB.toByte()
            }
        }
        val patch = buildSimpleUps(unheadered, target)
        val headered = ByteArray(unheadered.size + 512).also {
            System.arraycopy(unheadered, 0, it, 512, unheadered.size)
        }

        val result = RomPatcher.applyInMemory(headered, patch, systemId = "snes", fileName = "unbound.ups")
        assertEquals(PatchFormat.UPS, result.format)
        assertArrayEquals(target, result.data)
        assertNotNull(result.headerNote)
    }

    @Test
    fun xdeltaVcdiffDecodesCopyAddRunAndVerifiesAdler32() {
        val source = "POKEMON FIRERED ORIGINAL ROM BYTES".toByteArray(Charsets.US_ASCII)
        // Expected target: "POKEMON UNBOUND!" + "!!!!" (RUN 4 '!') + " ORIGINAL ROM BYTES"
        val expected = "POKEMON UNBOUND!!!!! ORIGINAL ROM BYTES".toByteArray(Charsets.US_ASCII)

        val dataSec = ByteArrayOutputStream().apply {
            write("UNBOUND!".toByteArray(Charsets.US_ASCII)) // 8 bytes for ADD
            write('!'.code) // 1 byte for RUN
        }.toByteArray()

        val instSec = ByteArrayOutputStream().apply {
            // 1) COPY 8 bytes ("POKEMON ") from source offset 0 (mode 0 = VCD_SELF, size 8 => opcode 19 + (8-3) = 24)
            write(24)
            // 2) ADD 8 bytes ("UNBOUND!") => opcode 1 + 8 = 9
            write(9)
            // 3) RUN 4 bytes ('!') => opcode 0, size 4
            write(0)
            write7BitInt(this, 4)
            // 4) COPY 19 bytes (" ORIGINAL ROM BYTES") from source offset 15 (mode 0, size 0 => opcode 19, then size 19)
            write(19)
            write7BitInt(this, 19)
        }.toByteArray()

        val addrSec = ByteArrayOutputStream().apply {
            write7BitInt(this, 0) // source offset 0
            write7BitInt(this, 15) // source offset 15 (" ORIGINAL ROM BYTES")
        }.toByteArray()

        val adler = Adler32().apply { update(expected) }.value

        val windowPayload = ByteArrayOutputStream().apply {
            write7BitInt(this, expected.size) // target window len
            write(0) // delta indicator = 0 (uncompressed)
            write7BitInt(this, dataSec.size)
            write7BitInt(this, instSec.size)
            write7BitInt(this, addrSec.size)
            // Adler-32 big-endian
            write(((adler ushr 24) and 0xFF).toInt())
            write(((adler ushr 16) and 0xFF).toInt())
            write(((adler ushr 8) and 0xFF).toInt())
            write((adler and 0xFF).toInt())
            write(dataSec)
            write(instSec)
            write(addrSec)
        }.toByteArray()

        val patch = ByteArrayOutputStream().apply {
            write(byteArrayOf(0xD6.toByte(), 0xC3.toByte(), 0xC4.toByte(), 0x00)) // VCDIFF magic
            write(0x00) // hdrIndicator
            write(0x01 or 0x04) // VCD_SOURCE | VCD_ADLER32
            write7BitInt(this, source.size) // sourceSegmentLength
            write7BitInt(this, 0) // sourceSegmentPosition
            write7BitInt(this, windowPayload.size) // deltaLength
            write(windowPayload)
        }.toByteArray()

        val result = RomPatcher.applyInMemory(source, patch, systemId = "gba", fileName = "unbound.xdelta")
        assertEquals(PatchFormat.XDELTA, result.format)
        assertArrayEquals(expected, result.data)
    }

    @Test
    fun zipPatchArchiveAutoExtractsNestedPatch() {
        val source = "HELLO WORLD".toByteArray(Charsets.US_ASCII)
        val target = "HELLO RETRO".toByteArray(Charsets.US_ASCII)
        val bps = buildSimpleBps(source, target)

        val zipBytes = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("readme.txt"))
                zos.write("Apply to clean ROM".toByteArray())
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("patch/retro_mod.bps"))
                zos.write(bps)
                zos.closeEntry()
            }
        }.toByteArray()

        val extracted = RomPatcher.extractPatchFromZip(zipBytes)
        assertNotNull(extracted)
        assertEquals("retro_mod.bps", extracted!!.first)
        val res = RomPatcher.applyInMemory(source, extracted.second, systemId = "gba", fileName = extracted.first)
        assertArrayEquals(target, res.data)
    }

    @Test
    fun saveScopedGameIsolatesHackBatterySaveFromParent() {
        val parent = Game(
            id = "content://roms/Pokemon_FireRed.gba",
            fileName = "Pokemon - FireRed Version (USA).gba",
            systemId = "gba",
            treeUri = Game.LOCAL,
            parentDocId = "",
            size = 16_777_216L,
        )
        val scoped = RomPatch.saveScopedGame(parent, "a1b2c3d4e5f6")
        assertNotEquals(parent.id, scoped.id)
        assertNotEquals(parent.baseName, scoped.baseName)
        assertTrue(scoped.baseName.contains("patch_a1b2c3d4"))
        assertEquals(parent.title, scoped.title)
    }
}
