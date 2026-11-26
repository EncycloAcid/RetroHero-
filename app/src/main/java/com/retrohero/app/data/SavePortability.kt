package com.retrohero.app.data

import java.io.File

/**
 * Core-independent canonical battery save engine.
 *
 * Solves save fragmentation across emulator cores by:
 * 1. Discovering & unifying alternate save extensions (`.srm`, `.sav`, `.dsv`, `.eep`, `.sra`, `.fla`, `.mpk`).
 * 2. Canonicalizing per-system layouts (N64 296,960 B container + 32-bit word byte-swapping,
 *    GBA VBA 136 KB padding / mGBA 16 B RTC trailer, SNES 512 B copier header, NDS DeSmuME 122 B footer,
 *    GB/GBC Gambatte vs SameBoy RTC trailer, NES HeroNES/FCEUmm/Nestopia 8 KB WRAM).
 * 3. Adapting the canonical save on-the-fly for the active core and committing canonical bytes back on
 *    every save state, quick save, disc swap, pause, and exit.
 */
object SavePortability {

    const val N64_SRM_SIZE = 296_960        // 0x48800 libretro unified N64 save container
    const val N64_EEP_OFFSET = 0x00000
    const val N64_EEP_MAX = 0x00800         // 2 KB
    const val N64_MPK_OFFSET = 0x00800
    const val N64_MPK_SIZE = 0x20000        // 128 KB (4 x 32 KB Controller Paks)
    const val N64_SRAM_OFFSET = 0x20800
    const val N64_SRAM_SIZE = 0x08000       // 32 KB
    const val N64_FLASH_OFFSET = 0x28800
    const val N64_FLASH_SIZE = 0x20000      // 128 KB

    private const val GBA_VBA_PADDED_SIZE = 139_264 // 136 KB (VBA Next / VBA-M padded Flash+RTC)
    private const val GBA_FLASH_128K = 131_072
    private const val GBA_FLASH_64K = 65_536
    private const val GBA_RTC_FOOTER = 16

    private val SNES_COPIER_SIZES = setOf(2_560, 8_704, 16_896, 33_280, 66_048)
    private val DESMUME_MARKER = "DeSmuME savedata footer".toByteArray(Charsets.US_ASCII)
    private val DESMUME_SNIP = "|<--Snip".toByteArray(Charsets.US_ASCII)

    private val ALT_EXTENSIONS = listOf("srm", "sav", "dsv", "eep", "sra", "fla", "mpk", "mcr", "mcd")

    data class ConversionResult(
        val bytes: ByteArray,
        val converted: Boolean,
        val note: String = "",
    )

    data class PreparedSave(
        /** File path passed directly into NativeBridge.loadGame() / saveSram(). */
        val activeFile: File,
        /** Canonical unified battery save path for this game (`saves/<sys>/<baseName>.srm`). */
        val canonicalFile: File,
        /** True if an alternate save was imported or converted during preparation. */
        val converted: Boolean,
        val note: String = "",
    )

    /** 32-bit word byte-swap (ABCD <-> DCBA), required between standalone N64 .sra/.fla and libretro .srm. */
    fun swap32(input: ByteArray): ByteArray {
        val out = input.copyOf()
        var i = 0
        while (i + 3 < out.size) {
            val b0 = out[i]
            val b1 = out[i + 1]
            out[i] = out[i + 3]
            out[i + 1] = out[i + 2]
            out[i + 2] = b1
            out[i + 3] = b0
            i += 4
        }
        return out
    }

    /** 16-bit halfword byte-swap (AB <-> BA). */
    fun swap16(input: ByteArray): ByteArray {
        val out = input.copyOf()
        var i = 0
        while (i + 1 < out.size) {
            val b0 = out[i]
            out[i] = out[i + 1]
            out[i + 1] = b0
            i += 2
        }
        return out
    }

    /** Allocates a blank libretro N64 296,960-byte container initialized identically to Mupen64Plus-Next. */
    fun blankN64Container(): ByteArray {
        val buf = ByteArray(N64_SRM_SIZE) { 0xFF.toByte() }
        // Controller Paks (0x00800 .. 0x207FF) default to 0x00
        buf.fill(0x00.toByte(), N64_MPK_OFFSET, N64_MPK_OFFSET + N64_MPK_SIZE)
        return buf
    }

    /**
     * Converts raw battery save bytes from any supported emulator/extension into RetroHero's
     * canonical per-system `.srm` representation.
     */
    fun canonicalizeBytes(
        raw: ByteArray,
        systemId: String,
        sourceExt: String = "srm",
        sourceCoreId: String = "",
    ): ConversionResult {
        if (raw.isEmpty()) return ConversionResult(raw, false)
        val ext = sourceExt.lowercase()
        return when (systemId.lowercase()) {
            "n64" -> canonicalizeN64(raw, ext)
            "gba" -> canonicalizeGba(raw)
            "snes" -> canonicalizeSnes(raw)
            "nds" -> canonicalizeNds(raw)
            "gb", "gbc" -> canonicalizeGameBoy(raw)
            "nes", "fds" -> canonicalizeNes(raw)
            else -> ConversionResult(raw, false)
        }
    }

    /**
     * Adapts canonical `.srm` bytes into the exact layout expected by [targetCoreId] when cores
     * disagree on trailing RTC footers or container sizing.
     */
    fun adaptForCore(canonical: ByteArray, systemId: String, targetCoreId: String): ConversionResult {
        if (canonical.isEmpty()) return ConversionResult(canonical, false)
        val sys = systemId.lowercase()
        val core = targetCoreId.lowercase()

        // GBA: gpSP & VBA Next expect clean power-of-two raw Flash without mGBA's 16-byte RTC footer
        if (sys == "gba" && (core == "gpsp" || core == "vba_next")) {
            if (canonical.size == GBA_FLASH_128K + GBA_RTC_FOOTER || canonical.size == GBA_FLASH_64K + GBA_RTC_FOOTER) {
                val stripped = canonical.copyOf(canonical.size - GBA_RTC_FOOTER)
                return ConversionResult(stripped, true, "Stripped 16 B GBA RTC trailer for $targetCoreId")
            }
        }

        // GB/GBC: SameBoy expects clean power-of-two SRAM without Gambatte's 44/48-byte RTC footer
        if ((sys == "gb" || sys == "gbc") && core == "sameboy") {
            val base = stripTrailingRtcIfKnown(canonical, listOf(44, 48))
            if (base != null) {
                return ConversionResult(base, true, "Normalized GB/GBC RTC footer for SameBoy")
            }
        }

        // N64: guarantee 296,960-byte container for Mupen64Plus-Next / ParaLLEl N64
        if (sys == "n64" && canonical.size != N64_SRM_SIZE) {
            return canonicalizeN64(canonical, "sav")
        }

        return ConversionResult(canonical, false)
    }

    /**
     * Prepares the active save file for [game] on [targetCoreId]:
     * 1. Migrates legacy per-disc `.srm` saves into the unified multi-disc save file.
     * 2. Discovers and imports any external/alternate save (`.sav`, `.dsv`, `.eep`, `.sra`, `.fla`, etc.)
     *    if the canonical `.srm` does not exist yet.
     * 3. Canonicalizes the save and adapts it if [targetCoreId] needs a core-specific layout.
     */
    fun prepareForCore(game: Game, targetCoreId: String, paths: AppPaths): PreparedSave {
        val canonical = paths.migrateLegacyDiscSave(game)
        var converted = false
        var note = ""

        // If canonical .srm doesn't exist yet, hunt for alternate extensions in both system & root save dirs
        if (!canonical.exists() || canonical.length() == 0L) {
            val discovered = discoverAlternateSave(game, paths)
            if (discovered != null) {
                val raw = runCatching { discovered.readBytes() }.getOrNull()
                if (raw != null && raw.isNotEmpty()) {
                    val res = canonicalizeBytes(raw, game.systemId, discovered.extension, targetCoreId)
                    writeAtomicBytes(canonical, res.bytes)
                    converted = true
                    note = res.note.ifBlank { "Imported ${discovered.extension.uppercase()} save into unified .srm" }
                }
            }
        } else {
            // Canonical exists — make sure it is actually normalized (e.g. from older app versions or external copy)
            val raw = runCatching { canonical.readBytes() }.getOrNull()
            if (raw != null && raw.isNotEmpty()) {
                val res = canonicalizeBytes(raw, game.systemId, "srm", targetCoreId)
                if (res.converted) {
                    backupOnce(canonical, raw)
                    writeAtomicBytes(canonical, res.bytes)
                    converted = true
                    note = res.note
                }
            }
        }

        // Now adapt for target core if needed
        if (canonical.exists() && canonical.length() > 0L) {
            val canonicalBytes = runCatching { canonical.readBytes() }.getOrNull()
            if (canonicalBytes != null && canonicalBytes.isNotEmpty()) {
                val adapted = adaptForCore(canonicalBytes, game.systemId, targetCoreId)
                if (adapted.converted) {
                    val coreTemp = File(canonical.parentFile, ".${game.baseName}.$targetCoreId.srm")
                    writeAtomicBytes(coreTemp, adapted.bytes)
                    return PreparedSave(
                        activeFile = coreTemp,
                        canonicalFile = canonical,
                        converted = true,
                        note = adapted.note,
                    )
                }
            }
        }

        return PreparedSave(
            activeFile = canonical,
            canonicalFile = canonical,
            converted = converted,
            note = note,
        )
    }

    /**
     * Commits the core's written SRAM file back into the canonical `.srm` save atomically,
     * normalizing any core-specific padding/headers so other cores can load it immediately.
     */
    fun commitFromCore(
        activeFile: File,
        canonicalFile: File,
        systemId: String,
        coreId: String,
        cleanupTemp: Boolean = false,
    ): Boolean {
        if (!activeFile.exists() || activeFile.length() == 0L) {
            if (cleanupTemp && activeFile != canonicalFile) runCatching { activeFile.delete() }
            return false
        }
        val raw = runCatching { activeFile.readBytes() }.getOrNull() ?: return false
        if (raw.isEmpty()) return false

        val normalized = canonicalizeBytes(raw, systemId, "srm", coreId)
        val ok = if (activeFile == canonicalFile && !normalized.converted) {
            true // already written cleanly in place
        } else {
            writeAtomicBytes(canonicalFile, normalized.bytes)
        }
        if (cleanupTemp && activeFile != canonicalFile) {
            runCatching { activeFile.delete() }
        }
        return ok
    }

    private fun discoverAlternateSave(game: Game, paths: AppPaths): File? {
        val sysDir = File(paths.saves, game.systemId)
        val dirs = listOf(sysDir, paths.saves).distinct()
        val stems = linkedSetOf(game.baseName)
        val rawStem = game.fileName.substringBeforeLast('.').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        stems += rawStem
        game.discs.forEach { d ->
            stems += d.fileName.substringBeforeLast('.').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        }

        val matches = mutableListOf<File>()
        for (dir in dirs) {
            if (!dir.isDirectory) continue
            for (stem in stems) {
                for (ext in ALT_EXTENSIONS) {
                    val f = File(dir, "$stem.$ext")
                    if (f.exists() && f.isFile && f.length() > 0L) matches += f
                }
            }
        }
        return matches.maxByOrNull { it.lastModified() }
    }

    private fun canonicalizeN64(raw: ByteArray, ext: String): ConversionResult {
        if (raw.size == N64_SRM_SIZE) return ConversionResult(raw, false)
        val container = blankN64Container()
        return when {
            raw.size == 512 || raw.size == N64_EEP_MAX -> {
                raw.copyInto(container, destinationOffset = N64_EEP_OFFSET)
                ConversionResult(container, true, "Packed N64 ${raw.size} B EEPROM into canonical .srm")
            }
            raw.size == N64_SRAM_SIZE && ext == "mpk" -> {
                raw.copyInto(container, destinationOffset = N64_MPK_OFFSET)
                ConversionResult(container, true, "Packed N64 Controller Pak (.mpk) into canonical .srm")
            }
            raw.size == N64_SRAM_SIZE -> {
                // Standalone N64 .sra / .sav SRAM is 32-bit big-endian; libretro Mupen64Plus expects word-swapped
                val swapped = swap32(raw)
                swapped.copyInto(container, destinationOffset = N64_SRAM_OFFSET)
                ConversionResult(container, true, "Word-swapped N64 32 KB SRAM into canonical .srm")
            }
            raw.size == N64_FLASH_SIZE -> {
                val swapped = swap32(raw)
                swapped.copyInto(container, destinationOffset = N64_FLASH_OFFSET)
                ConversionResult(container, true, "Word-swapped N64 128 KB FlashRAM into canonical .srm")
            }
            else -> ConversionResult(raw, false)
        }
    }

    private fun canonicalizeGba(raw: ByteArray): ConversionResult {
        // VBA Next / VBA-M often writes 136 KB (139,264 B) saves padded with 0x00 or 0xFF
        if (raw.size == GBA_VBA_PADDED_SIZE) {
            val trimmed = if (isUniformRange(raw, GBA_FLASH_64K, GBA_FLASH_128K)) {
                raw.copyOf(GBA_FLASH_64K)
            } else {
                raw.copyOf(GBA_FLASH_128K)
            }
            return ConversionResult(trimmed, true, "Trimmed VBA 136 KB padding to canonical ${trimmed.size / 1024} KB GBA save")
        }
        // Trim accidental trailing zero/0xFF padding on 128 KB + non-RTC extras
        if (raw.size > GBA_FLASH_128K + GBA_RTC_FOOTER && isUniformRange(raw, GBA_FLASH_128K, raw.size)) {
            return ConversionResult(raw.copyOf(GBA_FLASH_128K), true, "Normalized padded GBA Flash save to 128 KB")
        }
        return ConversionResult(raw, false)
    }

    private fun canonicalizeSnes(raw: ByteArray): ConversionResult {
        if (raw.size in SNES_COPIER_SIZES) {
            return ConversionResult(
                raw.copyOfRange(512, raw.size),
                true,
                "Stripped 512 B copier header from SNES save",
            )
        }
        return ConversionResult(raw, false)
    }

    private fun canonicalizeNds(raw: ByteArray): ConversionResult {
        // DeSmuME .dsv files append a ~122-byte ASCII footer containing "DeSmuME savedata footer"
        val searchStart = (raw.size - 256).coerceAtLeast(0)
        if (indexOfBytes(raw, DESMUME_MARKER, searchStart) >= 0) {
            val snipIdx = indexOfBytes(raw, DESMUME_SNIP, searchStart)
            if (snipIdx > 0) {
                return ConversionResult(
                    raw.copyOf(snipIdx),
                    true,
                    "Stripped DeSmuME .dsv footer to canonical raw NDS save",
                )
            }
        }
        return ConversionResult(raw, false)
    }

    private fun canonicalizeGameBoy(raw: ByteArray): ConversionResult {
        // Already clean power-of-two or valid Gambatte/SameBoy RTC save
        return ConversionResult(raw, false)
    }

    private fun canonicalizeNes(raw: ByteArray): ConversionResult {
        // Standardize accidental oversized trailing zero padding above 8 KB / 32 KB
        if (raw.size > 8192 && raw.size < 32768 && isUniformRange(raw, 8192, raw.size)) {
            return ConversionResult(raw.copyOf(8192), true, "Normalized NES WRAM to canonical 8 KB")
        }
        return ConversionResult(raw, false)
    }

    private fun stripTrailingRtcIfKnown(data: ByteArray, footerSizes: List<Int>): ByteArray? {
        for (footer in footerSizes) {
            val base = data.size - footer
            if (base >= 512 && (base and (base - 1)) == 0) {
                return data.copyOf(base)
            }
        }
        return null
    }

    private fun isUniformRange(data: ByteArray, from: Int, to: Int): Boolean {
        if (from >= to || from < 0 || to > data.size) return true
        val first = data[from]
        if (first != 0x00.toByte() && first != 0xFF.toByte()) return false
        for (i in (from + 1) until to) {
            if (data[i] != first) return false
        }
        return true
    }

    private fun indexOfBytes(haystack: ByteArray, needle: ByteArray, start: Int): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        outer@ for (i in start..(haystack.size - needle.size)) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun backupOnce(file: File, originalBytes: ByteArray) {
        val bak = File(file.parentFile, "${file.name}.bak")
        if (!bak.exists()) {
            runCatching { bak.writeBytes(originalBytes) }
        }
    }

    private fun writeAtomicBytes(dest: File, bytes: ByteArray): Boolean = runCatching {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "${dest.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(dest)) {
            dest.writeBytes(bytes)
            tmp.delete()
        }
        true
    }.getOrDefault(false)
}
