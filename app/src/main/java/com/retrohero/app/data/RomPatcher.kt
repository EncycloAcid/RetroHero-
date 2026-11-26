package com.retrohero.app.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Adler32
import java.util.zip.CRC32
import java.util.zip.ZipInputStream

/** Supported ROM soft-patching binary formats. */
enum class PatchFormat(val label: String, val extensions: Set<String>) {
    IPS("IPS", setOf("ips", "ips32")),
    BPS("BPS", setOf("bps")),
    UPS("UPS", setOf("ups")),
    XDELTA("xDelta", setOf("xdelta", "xdelta3", "vcdiff"));

    companion object {
        val allExtensions: Set<String> = entries.flatMap { it.extensions }.toSet()

        fun fromExtension(ext: String): PatchFormat? {
            val clean = ext.lowercase().trimStart('.')
            return entries.firstOrNull { clean in it.extensions }
        }
    }
}

/** Exception thrown when a patch file is invalid, unsupported, or incompatible with the base ROM. */
class PatchException(message: String) : IOException(message)

/** Output of an in-memory ROM soft-patch operation. */
data class PatchResult(
    val data: ByteArray,
    val format: PatchFormat,
    val headerNote: String? = null,
    val targetCrc32Hex: String,
    val metadata: String = "",
)

/**
 * Pure-Kotlin, zero-dependency, in-memory ROM soft-patching engine supporting
 * **IPS / IPS32**, **BPS**, **UPS**, and **xDelta (RFC 3284 VCDIFF)**.
 *
 * Automatically resolves headered vs. unheadered SNES (512-byte copier header) and
 * NES (16-byte iNES header) mismatches so players never hit cryptic checksum errors
 * or have to duplicate/mutate their original ROM files.
 */
object RomPatcher {

    private val MAGIC_IPS = "PATCH".toByteArray(Charsets.US_ASCII)
    private val MAGIC_IPS32 = "IPS32".toByteArray(Charsets.US_ASCII)
    private val MAGIC_BPS = "BPS1".toByteArray(Charsets.US_ASCII)
    private val MAGIC_UPS = "UPS1".toByteArray(Charsets.US_ASCII)
    private val MAGIC_VCDIFF = byteArrayOf(0xD6.toByte(), 0xC3.toByte(), 0xC4.toByte(), 0x00)

    /** Detects patch format by binary magic bytes first, falling back to filename extension. */
    fun detectFormat(patchBytes: ByteArray, fileName: String = ""): PatchFormat? {
        if (startsWith(patchBytes, MAGIC_BPS)) return PatchFormat.BPS
        if (startsWith(patchBytes, MAGIC_UPS)) return PatchFormat.UPS
        if (startsWith(patchBytes, MAGIC_IPS) || startsWith(patchBytes, MAGIC_IPS32)) return PatchFormat.IPS
        if (startsWith(patchBytes, MAGIC_VCDIFF)) return PatchFormat.XDELTA
        val ext = fileName.substringAfterLast('.', "")
        return PatchFormat.fromExtension(ext)
    }

    /** If [bytes] is a ZIP archive containing a patch file, extracts the first valid patch entry. */
    fun extractPatchFromZip(bytes: ByteArray): Pair<String, ByteArray>? {
        if (bytes.size < 4 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4B.toByte()) return null
        return runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/')
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && !name.startsWith("._") && ext in PatchFormat.allExtensions) {
                        val out = ByteArrayOutputStream()
                        zis.copyTo(out)
                        val data = out.toByteArray()
                        if (detectFormat(data, name) != null) {
                            return name to data
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            null
        }.getOrNull()
    }

    /** Applies [patchBytes] to [sourceRom] completely in memory without touching any files on disk. */
    fun applyInMemory(
        sourceRom: ByteArray,
        patchBytes: ByteArray,
        systemId: String = "",
        fileName: String = "",
    ): PatchResult {
        val format = detectFormat(patchBytes, fileName)
            ?: throw PatchException("Unsupported or unrecognized patch format (${fileName.ifBlank { "unknown" }}).")
        return when (format) {
            PatchFormat.IPS -> applyIps(sourceRom, patchBytes, systemId)
            PatchFormat.BPS -> applyBps(sourceRom, patchBytes, systemId)
            PatchFormat.UPS -> applyUps(sourceRom, patchBytes, systemId)
            PatchFormat.XDELTA -> applyVcdiff(sourceRom, patchBytes, systemId)
        }
    }

    fun crc32(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): Long {
        val crc = CRC32()
        crc.update(bytes, offset, length)
        return crc.value and 0xFFFFFFFFL
    }

    fun crc32Hex(bytes: ByteArray): String = "%08x".format(crc32(bytes))

    // -------------------------------------------------------------------------
    // Header adaptation (SNES 512-byte copier header & NES 16-byte iNES header)
    // -------------------------------------------------------------------------

    private data class HeaderVariant(
        val bytes: ByteArray,
        val note: String?,
        val addedDummyPrefixBytes: Int = 0,
    )

    private fun headerVariantsFor(sourceRom: ByteArray, systemId: String): List<HeaderVariant> {
        val list = mutableListOf(HeaderVariant(sourceRom, note = null))
        val sys = systemId.lowercase()
        // SNES SMC/SWC 512-byte copier header
        if (sys == "snes" || sys.isEmpty()) {
            if (sourceRom.size > 512 && (sourceRom.size % 1024) == 512) {
                list += HeaderVariant(
                    bytes = sourceRom.copyOfRange(512, sourceRom.size),
                    note = "Auto-stripped 512-byte SNES copier header",
                )
            } else if (sourceRom.size >= 0x8000 && (sourceRom.size % 1024) == 0) {
                val padded = ByteArray(sourceRom.size + 512)
                System.arraycopy(sourceRom, 0, padded, 512, sourceRom.size)
                list += HeaderVariant(
                    bytes = padded,
                    note = "Auto-aligned 512-byte SNES copier header",
                    addedDummyPrefixBytes = 512,
                )
            }
        }
        // NES 16-byte iNES header
        if (sys == "nes" || sys.isEmpty()) {
            val hasInes = sourceRom.size > 16 &&
                sourceRom[0] == 'N'.code.toByte() &&
                sourceRom[1] == 'E'.code.toByte() &&
                sourceRom[2] == 'S'.code.toByte() &&
                sourceRom[3] == 0x1A.toByte()
            if (hasInes) {
                list += HeaderVariant(
                    bytes = sourceRom.copyOfRange(16, sourceRom.size),
                    note = "Auto-stripped 16-byte iNES header",
                )
            }
        }
        return list
    }

    // -------------------------------------------------------------------------
    // IPS & IPS32
    // -------------------------------------------------------------------------

    private fun applyIps(sourceRom: ByteArray, patch: ByteArray, systemId: String): PatchResult {
        val isIps32 = startsWith(patch, MAGIC_IPS32)
        if (!isIps32 && !startsWith(patch, MAGIC_IPS)) {
            throw PatchException("Invalid IPS patch header (missing PATCH/IPS32 magic).")
        }

        // IPS carries no source CRC, so for SNES ROMs we check whether the ROM has a 512B header
        // and inspect whether the patch targets standard SNES internal header locations.
        var base = sourceRom
        var headerNote: String? = null
        if (systemId.equals("snes", ignoreCase = true) && sourceRom.size > 512 && (sourceRom.size % 1024) == 512) {
            // Check if applying as-is vs stripped yields a better SNES internal header score
            val rawOut = runIpsRaw(sourceRom, patch, isIps32)
            val strippedOut = runIpsRaw(sourceRom.copyOfRange(512, sourceRom.size), patch, isIps32)
            val rawScore = snesHeaderScore(rawOut, offset = 512)
            val strippedScore = snesHeaderScore(strippedOut, offset = 0)
            if (strippedScore > rawScore + 2) {
                return PatchResult(
                    data = strippedOut,
                    format = PatchFormat.IPS,
                    headerNote = "Auto-stripped 512-byte SNES copier header",
                    targetCrc32Hex = crc32Hex(strippedOut),
                )
            }
            return PatchResult(
                data = rawOut,
                format = PatchFormat.IPS,
                headerNote = null,
                targetCrc32Hex = crc32Hex(rawOut),
            )
        }

        val out = runIpsRaw(base, patch, isIps32)
        return PatchResult(
            data = out,
            format = PatchFormat.IPS,
            headerNote = headerNote,
            targetCrc32Hex = crc32Hex(out),
        )
    }

    private fun runIpsRaw(source: ByteArray, patch: ByteArray, isIps32: Boolean): ByteArray {
        val offsetBytes = if (isIps32) 4 else 3
        val eofTag = if (isIps32) 0x45454F46L else 0x454F46L // "EEOF" or "EOF"
        var pos = 5
        var out = source.copyOf()

        fun ensureSize(needed: Int) {
            if (needed > out.size) {
                out = out.copyOf(needed)
            }
        }

        while (pos < patch.size) {
            if (pos + offsetBytes > patch.size) {
                throw PatchException("Unexpected end of IPS patch at byte $pos.")
            }
            var offset = 0L
            for (i in 0 until offsetBytes) {
                offset = (offset shl 8) or (patch[pos + i].toInt() and 0xFF).toLong()
            }
            pos += offsetBytes

            if (offset == eofTag) {
                // Optional Lunar IPS truncation size right after EOF
                if (!isIps32 && pos + 3 == patch.size) {
                    val truncSize = ((patch[pos].toInt() and 0xFF) shl 16) or
                        ((patch[pos + 1].toInt() and 0xFF) shl 8) or
                        (patch[pos + 2].toInt() and 0xFF)
                    if (truncSize in 0 until out.size) {
                        out = out.copyOf(truncSize)
                    }
                }
                return out
            }

            if (pos + 2 > patch.size) throw PatchException("Truncated IPS hunk size at byte $pos.")
            val size = ((patch[pos].toInt() and 0xFF) shl 8) or (patch[pos + 1].toInt() and 0xFF)
            pos += 2

            if (size == 0) {
                // RLE hunk
                if (pos + 3 > patch.size) throw PatchException("Truncated IPS RLE hunk at byte $pos.")
                val rleLen = ((patch[pos].toInt() and 0xFF) shl 8) or (patch[pos + 1].toInt() and 0xFF)
                val fill = patch[pos + 2]
                pos += 3
                val end = (offset + rleLen).toInt()
                ensureSize(end)
                out.fill(fill, offset.toInt(), end)
            } else {
                if (pos + size > patch.size) throw PatchException("Truncated IPS hunk payload at byte $pos.")
                val end = (offset + size).toInt()
                ensureSize(end)
                System.arraycopy(patch, pos, out, offset.toInt(), size)
                pos += size
            }
        }
        return out
    }

    /** Heuristic score for valid SNES internal ROM header checksum complement at LoROM/HiROM/ExHiROM. */
    private fun snesHeaderScore(rom: ByteArray, offset: Int): Int {
        var best = 0
        for (base in intArrayOf(0x7FC0, 0xFFC0, 0x40FFC0)) {
            val h = offset + base
            if (h + 0x20 > rom.size) continue
            var score = 0
            val comp = ((rom[h + 0x1C].toInt() and 0xFF) or ((rom[h + 0x1D].toInt() and 0xFF) shl 8))
            val csum = ((rom[h + 0x1E].toInt() and 0xFF) or ((rom[h + 0x1F].toInt() and 0xFF) shl 8))
            if ((comp xor csum) == 0xFFFF) score += 4
            val printableTitle = (0 until 21).count { (rom[h + it].toInt() and 0xFF) in 0x20..0x7E }
            if (printableTitle >= 16) score += 2
            if (score > best) best = score
        }
        return best
    }

    // -------------------------------------------------------------------------
    // BPS (Beat Patch System v1)
    // -------------------------------------------------------------------------

    private fun applyBps(sourceRom: ByteArray, patch: ByteArray, systemId: String): PatchResult {
        if (patch.size < 16 || !startsWith(patch, MAGIC_BPS)) {
            throw PatchException("Invalid BPS patch header.")
        }
        val expectedPatchCrc = readU32LE(patch, patch.size - 4)
        val actualPatchCrc = crc32(patch, 0, patch.size - 4)
        if (expectedPatchCrc != actualPatchCrc) {
            throw PatchException("Patch file is corrupted (BPS patch CRC32 mismatch).")
        }
        val expectedSourceCrc = readU32LE(patch, patch.size - 12)
        val expectedTargetCrc = readU32LE(patch, patch.size - 8)

        val variants = headerVariantsFor(sourceRom, systemId)
        val matched = variants.firstOrNull { crc32(it.bytes) == expectedSourceCrc }
        if (matched == null) {
            if (variants.any { crc32(it.bytes) == expectedTargetCrc }) {
                throw PatchException("This ROM already matches the patched output.")
            }
            val gotHex = crc32Hex(sourceRom)
            val expHex = "%08x".format(expectedSourceCrc)
            throw PatchException(
                "ROM revision mismatch for BPS patch (expected CRC32 $expHex, found $gotHex). " +
                    "Check that you have the right region/revision base ROM.",
            )
        }

        val src = matched.bytes
        val reader = VarIntReader(patch, startOffset = 4, endOffset = patch.size - 12)
        val sourceSize = reader.readNumber().toInt()
        val targetSize = reader.readNumber().toInt()
        val metaSize = reader.readNumber().toInt()
        val metadata = if (metaSize > 0) reader.readBytes(metaSize).toString(Charsets.UTF_8) else ""

        if (src.size != sourceSize) {
            throw PatchException("BPS source size mismatch (expected $sourceSize B, got ${src.size} B).")
        }
        if (targetSize < 0 || targetSize > 256 * 1024 * 1024) {
            throw PatchException("BPS target ROM size is out of supported range ($targetSize bytes).")
        }

        val out = ByteArray(targetSize)
        var outOffset = 0
        var sourceRelOffset = 0
        var targetRelOffset = 0

        while (reader.hasRemaining()) {
            val data = reader.readNumber()
            val command = (data and 3L).toInt()
            val length = ((data ushr 2) + 1L).toInt()
            when (command) {
                0 -> { // SourceRead
                    if (outOffset + length > out.size || outOffset + length > src.size) {
                        throw PatchException("BPS SourceRead out of bounds.")
                    }
                    System.arraycopy(src, outOffset, out, outOffset, length)
                    outOffset += length
                }
                1 -> { // TargetRead
                    if (outOffset + length > out.size) throw PatchException("BPS TargetRead out of bounds.")
                    reader.readInto(out, outOffset, length)
                    outOffset += length
                }
                2 -> { // SourceCopy
                    val delta = reader.readNumber()
                    val signed = (delta ushr 1).toInt()
                    sourceRelOffset += if ((delta and 1L) != 0L) -signed else signed
                    if (sourceRelOffset < 0 || sourceRelOffset + length > src.size || outOffset + length > out.size) {
                        throw PatchException("BPS SourceCopy out of bounds.")
                    }
                    System.arraycopy(src, sourceRelOffset, out, outOffset, length)
                    sourceRelOffset += length
                    outOffset += length
                }
                3 -> { // TargetCopy (must copy byte-by-byte to support overlapping RLE)
                    val delta = reader.readNumber()
                    val signed = (delta ushr 1).toInt()
                    targetRelOffset += if ((delta and 1L) != 0L) -signed else signed
                    if (targetRelOffset < 0 || outOffset + length > out.size) {
                        throw PatchException("BPS TargetCopy out of bounds.")
                    }
                    for (i in 0 until length) {
                        out[outOffset++] = out[targetRelOffset++]
                    }
                }
            }
        }

        val actualTargetCrc = crc32(out)
        if (actualTargetCrc != expectedTargetCrc) {
            throw PatchException(
                "BPS output CRC32 verification failed (expected %08x, got %08x).".format(expectedTargetCrc, actualTargetCrc),
            )
        }

        // If we prepended a dummy 512-byte SNES header solely to satisfy a headered patch and the
        // patched output still has an empty 512-byte copier header, strip it back so it matches
        // clean No-Intro layout; otherwise keep it (snes9x supports both cleanly).
        val finalBytes = if (matched.addedDummyPrefixBytes == 512 && out.size > 512 && (out.size % 1024) == 512) {
            val prefixAllZero = (0 until 512).all { out[it] == 0.toByte() }
            if (prefixAllZero) out.copyOfRange(512, out.size) else out
        } else {
            out
        }

        return PatchResult(
            data = finalBytes,
            format = PatchFormat.BPS,
            headerNote = matched.note,
            targetCrc32Hex = crc32Hex(finalBytes),
            metadata = metadata,
        )
    }

    // -------------------------------------------------------------------------
    // UPS (Universal Patching System v1)
    // -------------------------------------------------------------------------

    private fun applyUps(sourceRom: ByteArray, patch: ByteArray, systemId: String): PatchResult {
        if (patch.size < 16 || !startsWith(patch, MAGIC_UPS)) {
            throw PatchException("Invalid UPS patch header.")
        }
        val expectedPatchCrc = readU32LE(patch, patch.size - 4)
        val actualPatchCrc = crc32(patch, 0, patch.size - 4)
        if (expectedPatchCrc != actualPatchCrc) {
            throw PatchException("Patch file is corrupted (UPS patch CRC32 mismatch).")
        }
        val expectedSourceCrc = readU32LE(patch, patch.size - 12)
        val expectedTargetCrc = readU32LE(patch, patch.size - 8)

        val variants = headerVariantsFor(sourceRom, systemId)
        val matched = variants.firstOrNull { crc32(it.bytes) == expectedSourceCrc }
        if (matched == null) {
            if (variants.any { crc32(it.bytes) == expectedTargetCrc }) {
                throw PatchException("This ROM already matches the patched output.")
            }
            val gotHex = crc32Hex(sourceRom)
            val expHex = "%08x".format(expectedSourceCrc)
            throw PatchException(
                "ROM revision mismatch for UPS patch (expected CRC32 $expHex, found $gotHex).",
            )
        }

        val src = matched.bytes
        val reader = VarIntReader(patch, startOffset = 4, endOffset = patch.size - 12)
        val sourceSize = reader.readNumber().toInt()
        val targetSize = reader.readNumber().toInt()
        if (src.size != sourceSize) {
            throw PatchException("UPS source size mismatch (expected $sourceSize B, got ${src.size} B).")
        }
        if (targetSize < 0 || targetSize > 256 * 1024 * 1024) {
            throw PatchException("UPS target ROM size is out of supported range ($targetSize bytes).")
        }

        val out = ByteArray(targetSize)
        System.arraycopy(src, 0, out, 0, minOf(src.size, targetSize))

        var outOffset = 0
        while (reader.hasRemaining()) {
            val skip = reader.readNumber().toInt()
            outOffset += skip
            while (reader.hasRemaining()) {
                val x = reader.readByte()
                if (x == 0) {
                    outOffset++
                    break
                }
                if (outOffset >= out.size) throw PatchException("UPS XOR write out of bounds at $outOffset.")
                val srcByte = if (outOffset < src.size) src[outOffset].toInt() and 0xFF else 0
                out[outOffset] = (srcByte xor x).toByte()
                outOffset++
            }
        }

        val actualTargetCrc = crc32(out)
        if (actualTargetCrc != expectedTargetCrc) {
            throw PatchException(
                "UPS output CRC32 verification failed (expected %08x, got %08x).".format(expectedTargetCrc, actualTargetCrc),
            )
        }

        val finalBytes = if (matched.addedDummyPrefixBytes == 512 && out.size > 512 && (out.size % 1024) == 512) {
            val prefixAllZero = (0 until 512).all { out[it] == 0.toByte() }
            if (prefixAllZero) out.copyOfRange(512, out.size) else out
        } else {
            out
        }

        return PatchResult(
            data = finalBytes,
            format = PatchFormat.UPS,
            headerNote = matched.note,
            targetCrc32Hex = crc32Hex(finalBytes),
        )
    }

    // -------------------------------------------------------------------------
    // xDelta / VCDIFF (RFC 3284)
    // -------------------------------------------------------------------------

    private const val VCD_DECOMPRESS = 0x01
    private const val VCD_CODETABLE = 0x02
    private const val VCD_APPHEADER = 0x04

    private const val VCD_SOURCE = 0x01
    private const val VCD_TARGET = 0x02
    private const val VCD_ADLER32 = 0x04

    private const val INST_NOOP = 0
    private const val INST_ADD = 1
    private const val INST_RUN = 2
    private const val INST_COPY = 3

    private data class VcdOp(val type: Int, val size: Int, val mode: Int)
    private data class VcdEntry(val first: VcdOp, val second: VcdOp)

    /** RFC 3284 Section 5.6 default 256-entry VCDIFF code table. */
    private val DEFAULT_VCDIFF_TABLE: Array<VcdEntry> by lazy {
        val noop = VcdOp(INST_NOOP, 0, 0)
        val tbl = ArrayList<VcdEntry>(256)
        // 0: RUN 0
        tbl += VcdEntry(VcdOp(INST_RUN, 0, 0), noop)
        // 1..18: ADD 0, 1..17
        for (size in 0..17) {
            tbl += VcdEntry(VcdOp(INST_ADD, size, 0), noop)
        }
        // 19..162: COPY mode 0..8, sizes 0, 4..18 (9 * 16 = 144)
        for (mode in 0..8) {
            tbl += VcdEntry(VcdOp(INST_COPY, 0, mode), noop)
            for (size in 4..18) {
                tbl += VcdEntry(VcdOp(INST_COPY, size, mode), noop)
            }
        }
        // 163..234: ADD(1..4) + COPY(4..6, mode 0..5) (6 * 4 * 3 = 72)
        for (mode in 0..5) {
            for (addSize in 1..4) {
                for (copySize in 4..6) {
                    tbl += VcdEntry(VcdOp(INST_ADD, addSize, 0), VcdOp(INST_COPY, copySize, mode))
                }
            }
        }
        // 235..246: ADD(1..4) + COPY(4, mode 6..8) (3 * 4 = 12)
        for (mode in 6..8) {
            for (addSize in 1..4) {
                tbl += VcdEntry(VcdOp(INST_ADD, addSize, 0), VcdOp(INST_COPY, 4, mode))
            }
        }
        // 247..255: COPY(4, mode 0..8) + ADD(1) (9)
        for (mode in 0..8) {
            tbl += VcdEntry(VcdOp(INST_COPY, 4, mode), VcdOp(INST_ADD, 1, 0))
        }
        check(tbl.size == 256) { "Invalid VCDIFF table size: ${tbl.size}" }
        tbl.toTypedArray()
    }

    private fun applyVcdiff(sourceRom: ByteArray, patch: ByteArray, systemId: String): PatchResult {
        if (patch.size < 8 || !startsWith(patch, MAGIC_VCDIFF)) {
            throw PatchException("Invalid xDelta/VCDIFF patch header.")
        }
        // Try exact ROM first, then auto-header variants if Adler-32 checksum or bounds fail!
        val variants = headerVariantsFor(sourceRom, systemId)
        var lastError: PatchException? = null
        for (variant in variants) {
            try {
                val out = decodeVcdiff(variant.bytes, patch)
                val finalBytes = if (variant.addedDummyPrefixBytes == 512 && out.size > 512 && (out.size % 1024) == 512) {
                    val prefixAllZero = (0 until 512).all { out[it] == 0.toByte() }
                    if (prefixAllZero) out.copyOfRange(512, out.size) else out
                } else {
                    out
                }
                return PatchResult(
                    data = finalBytes,
                    format = PatchFormat.XDELTA,
                    headerNote = variant.note,
                    targetCrc32Hex = crc32Hex(finalBytes),
                )
            } catch (e: PatchException) {
                lastError = e
            }
        }
        throw lastError ?: PatchException("Failed to apply xDelta/VCDIFF patch.")
    }

    private fun decodeVcdiff(source: ByteArray, patch: ByteArray): ByteArray {
        var pos = 4
        val hdrIndicator = patch[pos++].toInt() and 0xFF

        fun read7BitInt(): Int {
            var value = 0L
            while (true) {
                if (pos >= patch.size) throw PatchException("Unexpected EOF reading VCDIFF integer.")
                val b = patch[pos++].toInt() and 0xFF
                value = (value shl 7) or (b and 0x7F).toLong()
                if ((b and 0x80) == 0) break
            }
            if (value > Int.MAX_VALUE) throw PatchException("VCDIFF integer overflow.")
            return value.toInt()
        }

        if ((hdrIndicator and VCD_DECOMPRESS) != 0) {
            val compId = patch[pos++].toInt() and 0xFF
            throw PatchException(
                "This xDelta patch uses secondary compression (id=$compId). " +
                    "Re-export with `xdelta3 -S none` or use a BPS/IPS/uncompressed xDelta patch.",
            )
        }
        if ((hdrIndicator and VCD_CODETABLE) != 0) {
            val len = read7BitInt()
            if (len > 0) {
                throw PatchException("Custom VCDIFF code tables are not supported.")
            }
        }
        if ((hdrIndicator and VCD_APPHEADER) != 0) {
            val appLen = read7BitInt()
            pos += appLen
            if (pos > patch.size) throw PatchException("Invalid VCDIFF app header length.")
        }

        val targetOut = ByteArrayOutputStream(maxOf(source.size, 64 * 1024))

        while (pos < patch.size) {
            val winIndicator = patch[pos++].toInt() and 0xFF
            val hasSource = (winIndicator and VCD_SOURCE) != 0
            val hasTarget = (winIndicator and VCD_TARGET) != 0
            val hasAdler = (winIndicator and VCD_ADLER32) != 0
            if (hasSource && hasTarget) {
                throw PatchException("Invalid VCDIFF window: both VCD_SOURCE and VCD_TARGET set.")
            }

            val srcSegLen = if (hasSource || hasTarget) read7BitInt() else 0
            val srcSegPos = if (hasSource || hasTarget) read7BitInt() else 0

            val deltaLength = read7BitInt()
            val windowEnd = pos + deltaLength
            if (windowEnd > patch.size) throw PatchException("Truncated VCDIFF window.")

            val targetWinLen = read7BitInt()
            val deltaIndicator = patch[pos++].toInt() and 0xFF
            if (deltaIndicator != 0) {
                throw PatchException("Compressed VCDIFF delta sections (indicator=$deltaIndicator) are not supported.")
            }

            val dataLen = read7BitInt()
            val instLen = read7BitInt()
            val addrLen = read7BitInt()
            val expectedAdler = if (hasAdler) {
                if (pos + 4 > windowEnd) throw PatchException("Truncated VCDIFF Adler-32 checksum.")
                val v = ((patch[pos].toLong() and 0xFF) shl 24) or
                    ((patch[pos + 1].toLong() and 0xFF) shl 16) or
                    ((patch[pos + 2].toLong() and 0xFF) shl 8) or
                    (patch[pos + 3].toLong() and 0xFF)
                pos += 4
                v
            } else -1L

            val dataStart = pos
            val instStart = dataStart + dataLen
            val addrStart = instStart + instLen
            if (addrStart + addrLen > windowEnd) {
                throw PatchException("Invalid VCDIFF section lengths.")
            }

            val srcBuf: ByteArray = when {
                hasSource -> {
                    if (srcSegPos < 0 || srcSegPos + srcSegLen > source.size) {
                        throw PatchException("VCDIFF source segment out of bounds ($srcSegPos+$srcSegLen > ${source.size}).")
                    }
                    source
                }
                hasTarget -> {
                    val prev = targetOut.toByteArray()
                    if (srcSegPos < 0 || srcSegPos + srcSegLen > prev.size) {
                        throw PatchException("VCDIFF target segment out of bounds.")
                    }
                    prev
                }
                else -> ByteArray(0)
            }

            val winOut = ByteArray(targetWinLen)
            var dataPtr = dataStart
            var instPtr = instStart
            var addrPtr = addrStart
            var outPtr = 0

            val near = IntArray(4)
            var nextNear = 0
            val same = IntArray(3 * 256)

            fun updateCache(addr: Int) {
                near[nextNear] = addr
                nextNear = (nextNear + 1) and 3
                same[addr % (3 * 256)] = addr
            }

            fun readInstInt(): Int {
                var value = 0L
                while (true) {
                    if (instPtr >= instStart + instLen) throw PatchException("VCDIFF instruction stream EOF.")
                    val b = patch[instPtr++].toInt() and 0xFF
                    value = (value shl 7) or (b and 0x7F).toLong()
                    if ((b and 0x80) == 0) break
                }
                return value.toInt()
            }

            fun readAddrInt(): Int {
                var value = 0L
                while (true) {
                    if (addrPtr >= addrStart + addrLen) throw PatchException("VCDIFF address stream EOF.")
                    val b = patch[addrPtr++].toInt() and 0xFF
                    value = (value shl 7) or (b and 0x7F).toLong()
                    if ((b and 0x80) == 0) break
                }
                return value.toInt()
            }

            fun decodeAddr(here: Int, mode: Int): Int {
                val addr = when {
                    mode == 0 -> readAddrInt() // VCD_SELF
                    mode == 1 -> here - readAddrInt() // VCD_HERE
                    mode in 2..5 -> near[mode - 2] + readAddrInt() // NEAR
                    mode in 6..8 -> { // SAME
                        if (addrPtr >= addrStart + addrLen) throw PatchException("VCDIFF SAME address EOF.")
                        val b = patch[addrPtr++].toInt() and 0xFF
                        same[(mode - 6) * 256 + b]
                    }
                    else -> throw PatchException("Invalid VCDIFF COPY address mode: $mode")
                }
                updateCache(addr)
                return addr
            }

            fun execOp(op: VcdOp) {
                when (op.type) {
                    INST_NOOP -> return
                    INST_ADD -> {
                        val size = if (op.size == 0) readInstInt() else op.size
                        if (dataPtr + size > dataStart + dataLen || outPtr + size > targetWinLen) {
                            throw PatchException("VCDIFF ADD out of bounds.")
                        }
                        System.arraycopy(patch, dataPtr, winOut, outPtr, size)
                        dataPtr += size
                        outPtr += size
                    }
                    INST_RUN -> {
                        val size = if (op.size == 0) readInstInt() else op.size
                        if (dataPtr >= dataStart + dataLen || outPtr + size > targetWinLen) {
                            throw PatchException("VCDIFF RUN out of bounds.")
                        }
                        val fill = patch[dataPtr++]
                        winOut.fill(fill, outPtr, outPtr + size)
                        outPtr += size
                    }
                    INST_COPY -> {
                        val size = if (op.size == 0) readInstInt() else op.size
                        val here = srcSegLen + outPtr
                        var addr = decodeAddr(here, op.mode)
                        if (addr < 0 || outPtr + size > targetWinLen) {
                            throw PatchException("VCDIFF COPY out of bounds (addr=$addr, size=$size).")
                        }
                        for (i in 0 until size) {
                            val b = if (addr < srcSegLen) {
                                srcBuf[srcSegPos + addr]
                            } else {
                                val tIdx = addr - srcSegLen
                                if (tIdx >= outPtr) throw PatchException("VCDIFF COPY references unwritten target byte.")
                                winOut[tIdx]
                            }
                            winOut[outPtr++] = b
                            addr++
                        }
                    }
                }
            }

            while (instPtr < instStart + instLen) {
                val opcode = patch[instPtr++].toInt() and 0xFF
                val entry = DEFAULT_VCDIFF_TABLE[opcode]
                execOp(entry.first)
                execOp(entry.second)
            }

            if (outPtr != targetWinLen) {
                throw PatchException("VCDIFF window size mismatch (wrote $outPtr, expected $targetWinLen).")
            }
            if (hasAdler) {
                val adler = Adler32()
                adler.update(winOut)
                if ((adler.value and 0xFFFFFFFFL) != expectedAdler) {
                    throw PatchException(
                        "xDelta window Adler-32 verification failed (wrong base ROM revision or header).",
                    )
                }
            }
            targetOut.write(winOut)
            pos = windowEnd
        }

        return targetOut.toByteArray()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private class VarIntReader(
        private val buf: ByteArray,
        startOffset: Int,
        private val endOffset: Int,
    ) {
        private var pos = startOffset

        fun hasRemaining(): Boolean = pos < endOffset

        fun readByte(): Int {
            if (pos >= endOffset) throw PatchException("Unexpected EOF in patch stream.")
            return buf[pos++].toInt() and 0xFF
        }

        fun readNumber(): Long {
            var data = 0L
            var shift = 1L
            while (true) {
                val x = readByte()
                data += (x and 0x7F).toLong() * shift
                if ((x and 0x80) != 0) break
                shift = shift shl 7
                data += shift
            }
            return data
        }

        fun readBytes(len: Int): ByteArray {
            if (pos + len > endOffset) throw PatchException("Unexpected EOF reading $len bytes from patch.")
            val out = buf.copyOfRange(pos, pos + len)
            pos += len
            return out
        }

        fun readInto(dst: ByteArray, dstOffset: Int, len: Int) {
            if (pos + len > endOffset) throw PatchException("Unexpected EOF reading $len bytes from patch.")
            System.arraycopy(buf, pos, dst, dstOffset, len)
            pos += len
        }
    }

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) {
            if (data[i] != prefix[i]) return false
        }
        return true
    }

    private fun readU32LE(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }
}
