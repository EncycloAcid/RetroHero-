package com.retrohero.app.data

/** Aspect ratio override modes applied live in EmulatorActivity.GameView(). */
enum class AspectRatioMode(val id: String, val label: String, val ratio: Float?) {
    AUTO("auto", "Auto", null),
    CLASSIC_4_3("4_3", "4:3", 4f / 3f),
    WIDESCREEN_16_9("16_9", "16:9", 16f / 9f),
    FILL("fill", "Stretch", -1f);

    companion object {
        fun fromId(id: String?): AspectRatioMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}

/** Texture filtering preset mapped to 3D libretro cores (PS1/N64/PSP/DC/GC/PS2). */
enum class TextureFilterMode(val id: String, val label: String) {
    AUTO("auto", "Auto"),
    NEAREST("nearest", "Sharp"),
    BILINEAR("bilinear", "Smooth"),
    ENHANCED("enhanced", "xBR / 16× AF");

    companion object {
        fun fromId(id: String?): TextureFilterMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}

/** Per-system graphics settings (upscaling, aspect ratio, texture filtering, widescreen, PGXP, overscan). */
data class GraphicsConfig(
    val upscale: Int = 2, // 1..4 (3D internal resolution multiplier OR 2D crisp pixel prescale)
    val aspectRatio: AspectRatioMode = AspectRatioMode.AUTO,
    val textureFilter: TextureFilterMode = TextureFilterMode.AUTO,
    val widescreenHack: Boolean = false,
    val pgxpEnabled: Boolean = true,
    val cropOverscan: Boolean = false,
)

/**
 * Translates unified RetroHero Graphics Settings into the exact libretro core option keys
 * across every supported 3D and 2D core — no cryptic core submenus required.
 */
object GraphicsCoreOptions {

    private val THREE_D_CORES = setOf(
        "swanstation",
        "mednafen_psx_hw",
        "mupen64plus_next_gles3",
        "ppsspp",
        "flycast",
        "dolphin",
        "pcsx2",
        "play",
        "azahar",
        "citra",
        "yabasanshiro",
        "melonds",
    )

    fun is3DCore(coreId: String): Boolean = coreId in THREE_D_CORES

    fun supportsPgxp(coreId: String): Boolean = coreId == "swanstation" || coreId == "mednafen_psx_hw"

    fun supportsWidescreenHack(coreId: String): Boolean = coreId in setOf(
        "swanstation",
        "mednafen_psx_hw",
        "mupen64plus_next_gles3",
        "dolphin",
        "flycast",
        "play",
    )

    fun upscaleDescription(coreId: String, upscale: Int): String {
        val u = upscale.coerceIn(1, 4)
        return if (is3DCore(coreId)) {
            when (u) {
                1 -> "1× Native console resolution (fastest battery)"
                2 -> "2× HD (~480p–720p internal 3D rendering)"
                3 -> "3× Full HD (~720p–1080p internal 3D rendering)"
                else -> "4× Ultra HD (~1080p–1440p internal 3D rendering)"
            }
        } else {
            when (u) {
                1 -> "1× Native framebuffer (softest scaling)"
                2 -> "2× Crisp pixel prescale"
                3 -> "3× Ultra-crisp pixel prescale (recommended)"
                else -> "4× Maximum pixel sharpness (up to 1080p)"
            }
        }
    }

    /** Returns the exact libretro core option overrides corresponding to [config] for [coreId]. */
    fun optionsFor(coreId: String, config: GraphicsConfig): Map<String, String> {
        val u = config.upscale.coerceIn(1, 4)
        val out = linkedMapOf<String, String>()
        when (coreId) {
            "swanstation" -> {
                out["duckstation_GPU.ResolutionScale"] = u.toString()
                out["duckstation_GPU.PGXPEnable"] = if (config.pgxpEnabled) "true" else "false"
                out["duckstation_GPU.WidescreenHack"] = if (config.widescreenHack) "true" else "false"
                out["duckstation_Display.CropMode"] = if (config.cropOverscan) "Overscan" else "None"
                when (config.textureFilter) {
                    TextureFilterMode.NEAREST -> out["duckstation_GPU.TextureFilter"] = "Nearest"
                    TextureFilterMode.BILINEAR -> out["duckstation_GPU.TextureFilter"] = "Bilinear"
                    TextureFilterMode.ENHANCED -> out["duckstation_GPU.TextureFilter"] = "xBR"
                    TextureFilterMode.AUTO -> Unit
                }
            }
            "mednafen_psx_hw" -> {
                out["beetle_psx_hw_internal_resolution"] = when (u) {
                    1 -> "1x(native)"
                    2 -> "2x"
                    else -> "4x"
                }
                out["beetle_psx_hw_pgxp_mode"] = if (config.pgxpEnabled) "memory only" else "disabled"
                out["beetle_psx_hw_widescreen_mode"] = if (config.widescreenHack) "enabled" else "disabled"
                when (config.textureFilter) {
                    TextureFilterMode.NEAREST -> out["beetle_psx_hw_filter"] = "nearest"
                    TextureFilterMode.BILINEAR -> out["beetle_psx_hw_filter"] = "bilinear"
                    TextureFilterMode.ENHANCED -> out["beetle_psx_hw_filter"] = "xBR"
                    TextureFilterMode.AUTO -> Unit
                }
            }
            "mupen64plus_next_gles3" -> {
                out["mupen64plus-43screensize"] = when (u) {
                    1 -> "320x240"
                    2 -> "640x480"
                    3 -> "960x720"
                    else -> "1280x960"
                }
                out["mupen64plus-169screensize"] = when (u) {
                    1 -> "640x360"
                    2 -> "960x540"
                    3 -> "1280x720"
                    else -> "1920x1080"
                }
                out["mupen64plus-aspect"] = if (config.widescreenHack) "16:9 adjusted" else "4:3"
                when (config.textureFilter) {
                    TextureFilterMode.NEAREST -> out["mupen64plus-txFilterMode"] = "None"
                    TextureFilterMode.BILINEAR -> out["mupen64plus-txFilterMode"] = "Smooth filtering 1"
                    TextureFilterMode.ENHANCED -> out["mupen64plus-txFilterMode"] = "xBRZ"
                    TextureFilterMode.AUTO -> Unit
                }
            }
            "ppsspp" -> {
                out["ppsspp_internal_resolution"] = when (u) {
                    1 -> "480x272"
                    2 -> "960x544"
                    3 -> "1440x816"
                    else -> "1920x1088"
                }
                when (config.textureFilter) {
                    TextureFilterMode.NEAREST -> {
                        out["ppsspp_texture_filtering"] = "nearest"
                        out["ppsspp_texture_anisotropic_filtering"] = "off"
                    }
                    TextureFilterMode.BILINEAR -> {
                        out["ppsspp_texture_filtering"] = "linear"
                        out["ppsspp_texture_anisotropic_filtering"] = "4x"
                    }
                    TextureFilterMode.ENHANCED -> {
                        out["ppsspp_texture_filtering"] = "auto"
                        out["ppsspp_texture_anisotropic_filtering"] = "16x"
                    }
                    TextureFilterMode.AUTO -> Unit
                }
            }
            "flycast" -> {
                out["reicast_internal_resolution"] = when (u) {
                    1 -> "640x480"
                    2 -> "1280x960"
                    else -> "1920x1440"
                }
                out["reicast_widescreen_hack"] = if (config.widescreenHack) "enabled" else "disabled"
                when (config.textureFilter) {
                    TextureFilterMode.NEAREST -> out["reicast_anisotropic_filtering"] = "off"
                    TextureFilterMode.BILINEAR -> out["reicast_anisotropic_filtering"] = "4"
                    TextureFilterMode.ENHANCED -> out["reicast_anisotropic_filtering"] = "16"
                    TextureFilterMode.AUTO -> Unit
                }
            }
            "dolphin" -> {
                out["dolphin_efb_scale"] = when (u) {
                    1 -> "x1 (640 x 528)"
                    2 -> "x2 (1280 x 1056)"
                    3 -> "x3 (1920 x 1584)"
                    else -> "x4 (2560 x 2112)"
                }
                out["dolphin_widescreen_hack"] = if (config.widescreenHack) "enabled" else "disabled"
                when (config.textureFilter) {
                    TextureFilterMode.NEAREST -> out["dolphin_anisotropy"] = "1x"
                    TextureFilterMode.BILINEAR -> out["dolphin_anisotropy"] = "4x"
                    TextureFilterMode.ENHANCED -> out["dolphin_anisotropy"] = "16x"
                    TextureFilterMode.AUTO -> Unit
                }
            }
            "pcsx2" -> {
                out["pcsx2_upscale_multiplier"] = u.toString()
            }
            "play" -> {
                out["play_res_factor"] = u.toString()
                out["play_widescreen_mode"] = if (config.widescreenHack) "enabled" else "disabled"
            }
            "azahar", "citra" -> {
                out["citra_resolution_factor"] = if (u == 1) "1x (Native)" else "${u}x"
            }
            "yabasanshiro" -> {
                out["yabasanshiro_resolution_mode"] = when (u) {
                    1 -> "original"
                    2 -> "2x"
                    else -> "4x"
                }
            }
            "melonds" -> {
                out["melonds_opengl_resolution"] = if (u == 1) "1x native (256x192)" else "${u}x"
            }
            "fceumm" -> {
                out["fceumm_overscan_v"] = if (config.cropOverscan) "enabled" else "disabled"
            }
            "snes9x" -> {
                out["snes9x_overscan"] = if (config.cropOverscan) "enabled" else "disabled"
            }
            "genesis_plus_gx" -> {
                out["genesis_plus_gx_overscan"] = if (config.cropOverscan) "disabled" else "top/bottom"
            }
        }
        return out
    }
}
