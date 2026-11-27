package com.retrohero.app.data

/**
 * Automatic mobile-first performance tuning defaults for heavy 3D libretro cores.
 * Applied right before user CoreOptionsStore overrides so every heavy console boots
 * with hardware OpenGL ES 3.0 rendering, ARM64 JIT/dynarec, and mobile-friendly
 * threading out of the box — while preserving any custom tweak the user sets later.
 */
object CoreTuning {

    fun defaultsFor(coreId: String): Map<String, String> = when (coreId) {
        // PlayStation 2 — LRPS2 (PCSX2 ARM64 libretro core)
        "pcsx2" -> mapOf(
            "pcsx2_renderer" to "OpenGL",
            "pcsx2_upscale_multiplier" to "1",
            "pcsx2_speedhacks_presets" to "2",        // Balanced mobile speedhacks
            "pcsx2_enable_cheats" to "disabled",
            "pcsx2_fastboot" to "enabled",
            "pcsx2_fast_cdvd" to "enabled",
            "pcsx2_vu_clamping_mode" to "none",
            "pcsx2_mtvu" to "enabled",                // Multi-threaded VU1 for huge ARM64 speedup
            "pcsx2_frames_to_draw" to "1",
            "pcsx2_frames_to_skip" to "0",
        )
        // PlayStation 2 — Play! (HLE BIOS-free fallback)
        "play" -> mapOf(
            "play_res_factor" to "1",
            "play_widescreen_mode" to "disabled",
        )
        // Nintendo GameCube / Wii — Dolphin
        "dolphin" -> mapOf(
            "dolphin_renderer" to "Hardware",
            "dolphin_efb_scale" to "x1 (640 x 528)",
            "dolphin_cpu_core" to "JIT64",
            "dolphin_dual_core" to "enabled",
            "dolphin_fastmem" to "enabled",
            "dolphin_shader_compilation_mode" to "a-sync Skip Rendering",
            "dolphin_dsp_hle" to "enabled",
            "dolphin_dsp_jit" to "enabled",
        )
        // Nintendo 3DS — Azahar & Citra
        "azahar", "citra" -> mapOf(
            "citra_use_cpu_jit" to "enabled",
            "citra_use_hw_renderer" to "enabled",
            "citra_use_hw_shaders" to "enabled",
            "citra_use_acc_mul" to "disabled",
            "citra_use_shader_jit" to "enabled",
            "citra_resolution_factor" to "1x (Native)",
            "citra_is_new_3ds" to "New 3DS",
        )
        // Sony PSP — PPSSPP
        "ppsspp" -> mapOf(
            "ppsspp_cpu_core" to "jit",
            "ppsspp_internal_resolution" to "960x544",
            "ppsspp_gpu_hardware_transform" to "enabled",
            "ppsspp_vertex_cache" to "enabled",
            "ppsspp_fast_memory" to "enabled",
            "ppsspp_auto_frameskip" to "disabled",
            "ppsspp_rendering_mode" to "buffered",
            "ppsspp_threaded_input" to "enabled",
        )
        // Sega Dreamcast / NAOMI — Flycast
        "flycast" -> mapOf(
            "reicast_threaded_rendering" to "enabled",
            "reicast_synchronous_rendering" to "disabled",
            "reicast_internal_resolution" to "640x480",
            "reicast_alpha_sorting" to "per-triangle (normal)",
            "reicast_div_matching" to "auto",
            "reicast_enable_dsp" to "enabled",
        )
        // Nintendo 64 — Mupen64Plus-Next GLES3
        "mupen64plus_next_gles3" -> mapOf(
            "mupen64plus-cpucore" to "dynamic_recompiler",
            "mupen64plus-rdp-plugin" to "gliden64",
            "mupen64plus-rsp-plugin" to "hle",
            "mupen64plus-43screensize" to "640x480",
            "mupen64plus-ThreadedRenderer" to "True",
            "mupen64plus-HybridFilter" to "False",
        )
        // Sega Saturn — YabaSanshiro
        "yabasanshiro" -> mapOf(
            "yabasanshiro_resolution_mode" to "original",
            "yabasanshiro_sh2coretype" to "dynarec",
            "yabasanshiro_multithread_sh2" to "enabled",
            "yabasanshiro_frameskip" to "disabled",
        )
        // Sony PS1 HD — SwanStation & Beetle PSX HW
        "swanstation" -> mapOf(
            "duckstation_GPU.Renderer" to "OpenGL",
            "duckstation_GPU.ResolutionScale" to "2",
            "duckstation_GPU.PGXPEnable" to "true",
            "duckstation_CPU.ExecutionMode" to "Recompiler",
        )
        "mednafen_psx_hw" -> mapOf(
            "beetle_psx_hw_renderer" to "hardware_gl",
            "beetle_psx_hw_internal_resolution" to "2x",
            "beetle_psx_hw_cpu_dynarec" to "execute",
            "beetle_psx_hw_pgxp_mode" to "memory only",
        )
        // Nintendo DS — melonDS DS (direct boot FreeBIOS, reliable software renderer + ARM64 JIT, no PC cursor)
        "melondsds" -> mapOf(
            "melonds_render_mode" to "software",
            "melonds_jit_enable" to "enabled",
            "melonds_boot_mode" to "direct",
            "melonds_console_mode" to "ds",
            "melonds_cursor_mode" to "never",
            "melonds_screen_gap" to "0",
        )
        // Nintendo DS — DeSmuME
        "desmume" -> mapOf(
            "desmume_cpu_mode" to "jit",
            "desmume_internal_resolution" to "256x192",
            "desmume_pointer_colour" to "transparent",
            "desmume_frameskip" to "0",
        )
        // Nintendo DS — legacy melonDS
        "melonds" -> mapOf(
            "melonds_boot_directly" to "enabled",
            "melonds_jit_enable" to "enabled",
        )
        else -> emptyMap()
    }
}
