package com.retrohero.app.data

import java.io.File
import java.io.FileInputStream
import java.util.zip.CRC32

/** Native hardware color-matrix / composite blending mode matching C++ VideoOutput & HwRenderer. */
object ColorProfile {
    const val NONE = 0
    const val GBA_LCD = 1
    const val GBC_LCD = 2
    const val CRT_COMPOSITE = 3

    val labels = listOf("Off (Vivid)", "GBA LCD Matrix", "GBC LCD Matrix", "CRT Composite Blend")
    val descriptions = listOf(
        "Unfiltered native RGB pixels",
        "Authentic AGB-001 reflective TFT gamut",
        "Authentic Game Boy Color LCD tone curve",
        "Blends 1-px dither patterns (Sonic waterfalls & PS1 fog)",
    )
}

/** Simple top-level visual & hardware accuracy switch surfaced to the user. */
enum class VisualMode(val id: String, val label: String, val tagline: String) {
    AUTHENTIC("authentic", "Authentic", "Original CRT/LCD color matrix, composite blending & hardware timing"),
    ENHANCED("enhanced", "Enhanced", "Crisp pixels, PGXP anti-wobble 3D, SuperFX overclock & sprite fixes");

    companion object {
        fun fromId(id: String?): VisualMode? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

/**
 * Resolved per-game hardware & shader profile automatically applied by RetroHero.
 * Combines verified ROM CRC32 hash lookup, curated game rules, and per-system hardware profiles.
 */
data class HardwareProfile(
    val mode: VisualMode,
    val title: String,
    val summary: String,
    val filter: Int,
    val colorProfile: Int,
    val coreOptions: Map<String, String>,
    val preferredCoreId: String? = null,
    val isGameSpecific: Boolean = false,
)

/**
 * Built-in ROM-hash + title hardware intelligence database.
 *
 * Silently applies the exact optimal LCD color matrix, CRT composite dither blending,
 * PGXP 3D geometry correction, SuperFX overclocking, sprite-limit removal, and core fixes
 * per game — surfaced cleanly to the user as a 1-tap "Authentic / Enhanced" toggle.
 */
object HardwareProfiles {

    /** Known No-Intro / Redump CRC32 hashes -> canonical game profile tag (survives file renaming). */
    private val ROM_CRC_TAGS: Map<String, String> = mapOf(
        // Super Mario World 2: Yoshi's Island (USA 1.0 / 1.1 / Europe)
        "d2491220" to "superfx",
        "dc9bb451" to "superfx",
        "0f281414" to "superfx",
        // Star Fox (USA 1.0 / 1.2)
        "5897074d" to "superfx",
        "84511258" to "superfx",
        // Sonic the Hedgehog (W) (REV00 / REV01)
        "f9394e97" to "genesis_dither",
        "b519e833" to "genesis_dither",
        // Sonic the Hedgehog 2 (W) (REV01)
        "e548a201" to "genesis_dither",
        // Sonic the Hedgehog 3 (USA)
        "a8d2c66e" to "genesis_dither",
        // Castlevania: Harmony of Dissonance (USA)
        "88c6821c" to "gba_neon",
        // Golden Sun (USA)
        "d3667527" to "gba_neon",
        // Pokemon Emerald (USA, Europe)
        "1f1c08fb" to "gba_neon",
        // Metroid Fusion (USA)
        "6c75479c" to "gba_neon",
        // The Legend of Zelda: Link's Awakening DX (USA, Europe)
        "9f0a8a3e" to "gbc_lcd",
    )

    /** Fast CRC32 hash for cartridge ROMs (caps at 64 MB so massive disc ISOs never stall launch). */
    fun quickCrc32Hex(file: File): String? {
        if (!file.exists() || !file.isFile) return null
        val len = file.length()
        if (len <= 0L || len > 64L * 1024L * 1024L) return null
        return runCatching {
            val crc = CRC32()
            val buf = ByteArray(65_536)
            FileInputStream(file).use { fis ->
                while (true) {
                    val n = fis.read(buf)
                    if (n <= 0) break
                    crc.update(buf, 0, n)
                }
            }
            "%08x".format(crc.value)
        }.getOrNull()
    }

    /**
     * Picks the smartest default [VisualMode] for a game when the user hasn't manually toggled yet:
     * - Handhelds (GBA/GBC) & Genesis dither classics default to Authentic so colors & waterfalls look right immediately!
     * - 3D consoles (PS1/N64/PSP/GC/PS2) & SuperFX games default to Enhanced for PGXP anti-wobble & smooth FPS!
     */
    fun defaultModeFor(game: Game, romCrcHex: String? = null): VisualMode {
        val tag = detectSpecialTag(game, romCrcHex)
        return when {
            tag == "superfx" -> VisualMode.ENHANCED
            tag == "genesis_dither" -> VisualMode.AUTHENTIC
            tag == "gba_neon" || tag == "gbc_lcd" -> VisualMode.AUTHENTIC
            game.systemId in setOf("gba", "gbc") -> VisualMode.AUTHENTIC
            else -> VisualMode.ENHANCED
        }
    }

    /** Resolves the complete hardware, shader, color matrix, and libretro core options profile. */
    fun resolve(
        game: Game,
        coreId: String = "",
        mode: VisualMode = defaultModeFor(game),
        romCrcHex: String? = null,
    ): HardwareProfile {
        val sys = game.systemId.lowercase()
        val tag = detectSpecialTag(game, romCrcHex)

        // 1. High-impact game-specific intelligence profiles
        when (tag) {
            "superfx" -> return if (mode == VisualMode.ENHANCED) {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · SuperFX Boost",
                    summary = "SuperFX overclocked to 150% for smooth 60 FPS + crisp pixels",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf(
                        "snes9x_overclock_superfx" to "150%",
                        "snes9x_overclock_cycles" to "compatible",
                        "bsnes_coprocessor_delayed_sync" to "OFF",
                    ),
                    preferredCoreId = "snes9x",
                    isGameSpecific = true,
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · Authentic SuperFX",
                    summary = "Original 100% SuperFX timing + CRT scanlines & composite blend",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf(
                        "snes9x_overclock_superfx" to "100%",
                        "snes9x_overclock_cycles" to "disabled",
                    ),
                    preferredCoreId = "snes9x",
                    isGameSpecific = true,
                )
            }

            "genesis_dither" -> return if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · CRT Composite",
                    summary = "NTSC composite blend turns checkerboard waterfalls into true transparency + CRT scanlines",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf(
                        "genesis_plus_gx_lcd_filter" to "disabled",
                        "genesis_plus_gx_no_sprite_limit" to "disabled",
                    ),
                    preferredCoreId = "genesis_plus_gx",
                    isGameSpecific = true,
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · Crisp Enhanced",
                    summary = "Sharp pixels + sprite-flicker limit removed",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf(
                        "genesis_plus_gx_no_sprite_limit" to "enabled",
                    ),
                    preferredCoreId = "genesis_plus_gx",
                    isGameSpecific = true,
                )
            }

            "n64_factor5" -> return if (mode == VisualMode.ENHANCED) {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · GLideN64 Precision",
                    summary = "Framebuffer emulation + HD 960×720 for Factor 5 microcode compatibility",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf(
                        "mupen64plus-rdp-plugin" to "gliden64",
                        "mupen64plus-EnableFBEmulation" to "True",
                        "mupen64plus-43screensize" to "960x720",
                        "mupen64plus-EnableLODEmulation" to "True",
                    ),
                    preferredCoreId = "mupen64plus_next_gles3",
                    isGameSpecific = true,
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · Authentic N64 CRT",
                    summary = "Framebuffer emulation + native 640×480 CRT scanlines",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf(
                        "mupen64plus-rdp-plugin" to "gliden64",
                        "mupen64plus-EnableFBEmulation" to "True",
                        "mupen64plus-43screensize" to "640x480",
                    ),
                    preferredCoreId = "mupen64plus_next_gles3",
                    isGameSpecific = true,
                )
            }

            "ps1_dither" -> return if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · Authentic PS1 Fog",
                    summary = "Native 1× PS1 dithering smoothed by CRT composite blend & scanlines",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = ps1Options(authentic = true),
                    isGameSpecific = true,
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "${game.title} · PGXP 2× HD",
                    summary = "PGXP geometry fix removes wobbly 3D polygons + 2× HD resolution",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = ps1Options(authentic = false),
                    isGameSpecific = true,
                )
            }
        }

        // 2. Per-system hardware & shader intelligence
        return when (sys) {
            "gba" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "GBA · Authentic AGB-001 LCD",
                    summary = "Hardware AGB-001 color matrix fixes neon oversaturation + subtle LCD pixel grid",
                    filter = ScreenFilter.LCD,
                    colorProfile = ColorProfile.GBA_LCD,
                    coreOptions = mapOf(
                        // Handled at 0 ms in RetroHero's native LUT across mGBA, gpSP, and VBA Next
                        "mgba_color_correction" to "OFF",
                        "mgba_interframe_blending" to "lcd_ghosting",
                    ),
                    preferredCoreId = "mgba",
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "GBA · Vivid Sharp",
                    summary = "Unfiltered crisp RGB pixels with zero LCD ghosting",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf(
                        "mgba_color_correction" to "OFF",
                        "mgba_interframe_blending" to "OFF",
                    ),
                )
            }

            "gbc" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "GBC · Authentic Reflective LCD",
                    summary = "Authentic Game Boy Color tone matrix + fine handheld LCD grid",
                    filter = ScreenFilter.LCD,
                    colorProfile = ColorProfile.GBC_LCD,
                    coreOptions = mapOf("gambatte_gbc_color_correction" to "disabled"),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "GBC · Vivid Sharp",
                    summary = "Crisp unfiltered pixels for OLED punch",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf("gambatte_gbc_color_correction" to "disabled"),
                )
            }

            "gb" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "Game Boy · Authentic DMG LCD",
                    summary = "Classic handheld LCD pixel grid + original dot-matrix feel",
                    filter = ScreenFilter.LCD,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf("gambatte_gb_colorization" to "internal"),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "Game Boy · Crisp Sharp",
                    summary = "Razor-sharp pixels with clean high-contrast palette",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf("gambatte_gb_colorization" to "auto"),
                )
            }

            "psx", "ps1" -> if (mode == VisualMode.ENHANCED) {
                HardwareProfile(
                    mode = mode,
                    title = "PS1 · PGXP Anti-Wobble + 2× HD",
                    summary = "Eliminates PS1 polygon wobble with PGXP + crisp 2× internal resolution",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = ps1Options(authentic = false),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "PS1 · Authentic 240p CRT",
                    summary = "Native 1× PS1 output with CRT composite dither blending & scanlines",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = ps1Options(authentic = true),
                )
            }

            "snes" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "SNES · Authentic NTSC CRT",
                    summary = "CRT scanlines + composite dither blending for authentic 16-bit warmth",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf("snes9x_overclock_cycles" to "disabled"),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "SNES · Crisp + Smooth Timing",
                    summary = "Sharp pixels + compatible CPU overclock reduces heavy boss slowdown",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf("snes9x_overclock_cycles" to "compatible"),
                )
            }

            "nes", "fds" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "NES · Authentic 240p CRT",
                    summary = "Classic CRT scanlines + NTSC composite color bleed",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf(
                        "fceumm_nospritelimit" to "disabled",
                        "nestopia_nospritelimit" to "disabled",
                    ),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "NES · Crisp + No Sprite Flicker",
                    summary = "Sharp pixels + 8-sprite-per-scanline flicker limit lifted",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf(
                        "fceumm_nospritelimit" to "enabled",
                        "nestopia_nospritelimit" to "enabled",
                    ),
                )
            }

            "genesis", "segacd", "32x", "sms" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "Sega · Authentic CRT Composite",
                    summary = "Composite horizontal blending resolves Mega Drive dithering + CRT scanlines",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf("genesis_plus_gx_no_sprite_limit" to "disabled"),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "Sega · Crisp + No Sprite Limit",
                    summary = "Razor-sharp pixels + sprite flicker removed",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf("genesis_plus_gx_no_sprite_limit" to "enabled"),
                )
            }

            "n64" -> if (mode == VisualMode.ENHANCED) {
                HardwareProfile(
                    mode = mode,
                    title = "N64 · GLideN64 HD",
                    summary = "Crisp 960×720 rendering + accurate LOD mipmaps",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = mapOf(
                        "mupen64plus-43screensize" to "960x720",
                        "mupen64plus-EnableLODEmulation" to "True",
                    ),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "N64 · Authentic CRT",
                    summary = "Native 640×480 + CRT scanlines & composite softening",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = mapOf("mupen64plus-43screensize" to "640x480"),
                )
            }

            "nds", "3ds", "psp", "gg", "wonderswan", "ngp", "lynx", "pokemini" -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "${game.system?.shortName ?: sys.uppercase()} · Authentic Handheld LCD",
                    summary = "Subtle handheld pixel grid for authentic portable texture",
                    filter = ScreenFilter.LCD,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = emptyMap(),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "${game.system?.shortName ?: sys.uppercase()} · Crisp Enhanced",
                    summary = "Sharp unfiltered pixels for maximum handheld clarity",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = emptyMap(),
                )
            }

            else -> if (mode == VisualMode.AUTHENTIC) {
                HardwareProfile(
                    mode = mode,
                    title = "${game.system?.shortName ?: sys.uppercase()} · Authentic CRT",
                    summary = "Classic CRT scanlines & warm composite blending",
                    filter = ScreenFilter.CRT,
                    colorProfile = ColorProfile.CRT_COMPOSITE,
                    coreOptions = emptyMap(),
                )
            } else {
                HardwareProfile(
                    mode = mode,
                    title = "${game.system?.shortName ?: sys.uppercase()} · Crisp Enhanced",
                    summary = "Sharp pixels & balanced mobile performance",
                    filter = ScreenFilter.SHARP,
                    colorProfile = ColorProfile.NONE,
                    coreOptions = emptyMap(),
                )
            }
        }
    }

    private fun ps1Options(authentic: Boolean): Map<String, String> = if (authentic) {
        mapOf(
            "duckstation_GPU.ResolutionScale" to "1",
            "duckstation_GPU.PGXPEnable" to "false",
            "beetle_psx_hw_internal_resolution" to "1x",
            "beetle_psx_hw_pgxp_mode" to "disabled",
            "pcsx_rearmed_neon_enhancement_enable" to "disabled",
        )
    } else {
        mapOf(
            "duckstation_GPU.ResolutionScale" to "2",
            "duckstation_GPU.PGXPEnable" to "true",
            "duckstation_GPU.PGXPVertexCache" to "true",
            "duckstation_GPU.PGXPCulling" to "true",
            "beetle_psx_hw_internal_resolution" to "2x",
            "beetle_psx_hw_pgxp_mode" to "memory only",
            "beetle_psx_hw_pgxp_vertex" to "enabled",
            "pcsx_rearmed_neon_enhancement_enable" to "enabled",
        )
    }

    private fun detectSpecialTag(game: Game, romCrcHex: String?): String? {
        if (!romCrcHex.isNullOrBlank()) {
            ROM_CRC_TAGS[romCrcHex.lowercase()]?.let { return it }
        }
        val t = game.title.lowercase()
        val sys = game.systemId.lowercase()
        return when {
            sys == "snes" && listOf("yoshi's island", "yoshis island", "star fox", "starfox", "starwing", "stunt race fx", "doom", "vortex", "dirt trax").any { it in t } -> "superfx"
            sys in setOf("genesis", "segacd") && listOf("sonic", "streets of rage", "comix zone", "earthworm jim", "vectorman", "ecco", "gunstar", "shinobi", "castlevania", "aladdin").any { it in t } -> "genesis_dither"
            sys == "n64" && listOf("rogue squadron", "battle for naboo", "indiana jones").any { it in t } -> "n64_factor5"
            sys in setOf("psx", "ps1") && listOf("silent hill", "resident evil", "metal gear solid", "vagrant story", "dino crisis").any { it in t } -> "ps1_dither"
            sys == "gba" && listOf("harmony of dissonance", "golden sun", "metroid fusion", "emerald", "ruby", "sapphire", "firered", "leafgreen", "mario & luigi", "minish cap").any { it in t } -> "gba_neon"
            sys == "gbc" -> "gbc_lcd"
            else -> null
        }
    }
}
