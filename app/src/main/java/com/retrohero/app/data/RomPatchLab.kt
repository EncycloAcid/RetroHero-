package com.retrohero.app.data

/** Base ROM fingerprint computed non-destructively in memory by Patch Lab. */
data class RomFingerprint(
    val crc32Hex: String,
    val sizeBytes: Long,
    val headerStatus: String,
    val hasCopierHeader: Boolean = false,
    val hasInesHeader: Boolean = false,
)

enum class PatchCompatStatus {
    COMPATIBLE,
    COMPATIBLE_WITH_HEADER_FIX,
    INCOMPATIBLE,
}

/** Result of dry-running a patch against the user's base ROM in memory. */
data class PatchVerification(
    val status: PatchCompatStatus,
    val format: PatchFormat?,
    val sourceCrc32Hex: String,
    val patchedCrc32Hex: String?,
    val headerNote: String?,
    val message: String,
)

/** Curated spotlight entry for famous fan translations & quality-of-life hacks. */
data class NotablePatchInfo(
    val title: String,
    val category: String,
    val summary: String,
    val baseRomTip: String,
    val formats: String = "IPS / BPS / UPS / xDelta",
)

/**
 * Pure, zero-side-effect Patch Lab engine:
 * - Fingerprints any base ROM (CRC32 + SNES 512-byte copier / NES 16-byte iNES header detection)
 * - Dry-runs `.ips`, `.bps`, `.ups`, and `.xdelta` patches in memory to verify compatibility
 *   and report automatic header alignment BEFORE the user boots the game.
 */
object RomPatchLab {

    /** Safety cap for in-memory verification (cartridge ROMs; disc ISOs are skipped). */
    const val MAX_VERIFY_ROM_BYTES = 64L * 1024L * 1024L

    fun inspectBaseRom(romBytes: ByteArray, systemId: String): RomFingerprint {
        val sys = systemId.lowercase()
        val hasCopier = (sys == "snes" || sys.isEmpty()) &&
            romBytes.size > 512 && (romBytes.size % 1024) == 512
        val hasInes = (sys == "nes" || sys.isEmpty()) &&
            romBytes.size > 16 &&
            romBytes[0] == 'N'.code.toByte() &&
            romBytes[1] == 'E'.code.toByte() &&
            romBytes[2] == 'S'.code.toByte() &&
            romBytes[3] == 0x1A.toByte()

        val status = when {
            hasCopier -> "SNES 512B copier header detected (auto-aligned on patch)"
            sys == "snes" -> "Headerless SNES ROM (No-Intro standard)"
            hasInes -> "NES iNES 16B header detected"
            else -> "Clean unheadered ROM"
        }
        return RomFingerprint(
            crc32Hex = RomPatcher.crc32Hex(romBytes).uppercase(),
            sizeBytes = romBytes.size.toLong(),
            headerStatus = status,
            hasCopierHeader = hasCopier,
            hasInesHeader = hasInes,
        )
    }

    fun verifyPatch(
        sourceRom: ByteArray,
        patchBytes: ByteArray,
        systemId: String,
        fileName: String,
    ): PatchVerification {
        val baseCrc = RomPatcher.crc32Hex(sourceRom).uppercase()
        val detected = RomPatcher.detectFormat(patchBytes, fileName)
        return try {
            val result = RomPatcher.applyInMemory(
                sourceRom = sourceRom,
                patchBytes = patchBytes,
                systemId = systemId,
                fileName = fileName,
            )
            val status = if (result.headerNote != null) {
                PatchCompatStatus.COMPATIBLE_WITH_HEADER_FIX
            } else {
                PatchCompatStatus.COMPATIBLE
            }
            val msg = if (result.headerNote != null) {
                "${result.headerNote} · Patched CRC32 ${result.targetCrc32Hex.uppercase()}"
            } else {
                "Verified ${result.format.label} match · Patched CRC32 ${result.targetCrc32Hex.uppercase()}"
            }
            PatchVerification(
                status = status,
                format = result.format,
                sourceCrc32Hex = baseCrc,
                patchedCrc32Hex = result.targetCrc32Hex.uppercase(),
                headerNote = result.headerNote,
                message = msg,
            )
        } catch (e: PatchException) {
            PatchVerification(
                status = PatchCompatStatus.INCOMPATIBLE,
                format = detected,
                sourceCrc32Hex = baseCrc,
                patchedCrc32Hex = null,
                headerNote = null,
                message = e.message ?: "Patch does not match this ROM dump.",
            )
        } catch (e: Exception) {
            PatchVerification(
                status = PatchCompatStatus.INCOMPATIBLE,
                format = detected,
                sourceCrc32Hex = baseCrc,
                patchedCrc32Hex = null,
                headerNote = null,
                message = e.message ?: "Could not verify patch.",
            )
        }
    }
}

/**
 * Curated spotlight of famous fan translations & Quality-of-Life ROM patches so players
 * immediately know what classic patches pair with their games and which base ROM region they expect.
 */
object NotablePatchCatalog {

    fun forGame(game: Game): List<NotablePatchInfo> {
        val t = game.title.lowercase()
        return when {
            "mother 3" in t -> listOf(
                NotablePatchInfo(
                    title = "Mother 3 English Translation (v1.3)",
                    category = "Translation",
                    summary = "Complete English fan localization with proportional font and preserved musical rhythm timing.",
                    baseRomTip = "Requires Japanese GBA ROM (Mother 3 (Japan).gba) · UPS/BPS patch",
                ),
            )
            "fire emblem" in t && ("fuuin" in t || "binding" in t || "sword of seals" in t) -> listOf(
                NotablePatchInfo(
                    title = "Fire Emblem: The Binding Blade (English)",
                    category = "Translation",
                    summary = "Full English script, localized item/class names, and updated support conversations.",
                    baseRomTip = "Requires Japanese GBA ROM (Fire Emblem - Fuuin no Tsurugi) · UPS/IPS",
                ),
            )
            "fire emblem" in t && ("seisen" in t || "genealogy" in t) -> listOf(
                NotablePatchInfo(
                    title = "Project Naga (English Translation)",
                    category = "Translation",
                    summary = "Complete modern English translation of Genealogy of the Holy War with clean menus.",
                    baseRomTip = "Requires Japanese SNES ROM (headered or headerless — auto-aligned!)",
                ),
            )
            "chrono trigger" in t -> listOf(
                NotablePatchInfo(
                    title = "Chrono Trigger+ / MSU-1 & Bugfix Patches",
                    category = "QoL & Expansion",
                    summary = "Restores cut areas, adds dash-by-default QoL, and supports orchestrated MSU-1 audio patches.",
                    baseRomTip = "Requires USA SNES ROM (No-Intro headerless — 512B header auto-handled!)",
                ),
            )
            "final fantasy vi" in t || "final fantasy 6" in t || "final fantasy iii" in t && game.systemId == "snes" -> listOf(
                NotablePatchInfo(
                    title = "Ted Woolsey Uncensored / Sound Restoration",
                    category = "QoL & Restoration",
                    summary = "Fixes the classic Evade bug, restores original sprites/script, or adds SNES audio to GBA.",
                    baseRomTip = "SNES USA v1.0 / v1.1 (header auto-aligned) or GBA USA ROM",
                ),
            )
            "super metroid" in t -> listOf(
                NotablePatchInfo(
                    title = "Super Metroid Redux",
                    category = "Quality of Life",
                    summary = "Adds GBA-style single-button weapon cycling, faster door transitions, and optional heavy physics.",
                    baseRomTip = "Requires Super Metroid (Japan, USA) SNES ROM · IPS/BPS",
                ),
            )
            "link to the past" in t -> listOf(
                NotablePatchInfo(
                    title = "ALttP Redux & Item Randomizer",
                    category = "QoL & Randomizer",
                    summary = "Adds L/R quick item swap, faster text, and full support for ALttP Randomizer BPS seeds.",
                    baseRomTip = "Requires USA SNES ROM · 512B copier header auto-stripped/aligned",
                ),
            )
            "seiken densetsu 3" in t || "trials of mana" in t -> listOf(
                NotablePatchInfo(
                    title = "Seiken Densetsu 3 English Translation",
                    category = "Translation",
                    summary = "Classic English translation for the 3-hero SNES action RPG.",
                    baseRomTip = "Requires Japanese SNES ROM · IPS/BPS",
                ),
            )
            "live a live" in t -> listOf(
                NotablePatchInfo(
                    title = "Live A Live English Translation",
                    category = "Translation",
                    summary = "Complete English translation of Square's 7-era cult-classic SNES RPG.",
                    baseRomTip = "Requires Japanese SNES ROM · IPS/BPS",
                ),
            )
            "bahamut lagoon" in t -> listOf(
                NotablePatchInfo(
                    title = "Bahamut Lagoon (Near's English Translation)",
                    category = "Translation",
                    summary = "High-precision English localization with crisp variable-width font.",
                    baseRomTip = "Requires Japanese SNES ROM · BPS",
                ),
            )
            "pokemon" in t || "pokémon" in t -> listOf(
                NotablePatchInfo(
                    title = "QoL & Enhancement Hacks (Modern Emerald / Radical Red / Renegade Platinum)",
                    category = "QoL & Hacks",
                    summary = "Adds Physical/Special split, reusable TMs, national Dex, and trade-free evolutions.",
                    baseRomTip = "Match exact base ROM (e.g. Emerald USA / FireRed 1.0 Squirrels / Platinum USA)",
                ),
            )
            "castlevania" in t -> listOf(
                NotablePatchInfo(
                    title = "QoL & Recolor / Definitive Patches",
                    category = "Quality of Life",
                    summary = "Removes GBA washed-out brightness, fixes DS touch-seal requirements (Dawn of Sorrow), or restores SotN extras.",
                    baseRomTip = "Works with IPS, BPS, and xDelta (for NDS ROMs) non-destructively",
                ),
            )
            else -> emptyList()
        }
    }
}
