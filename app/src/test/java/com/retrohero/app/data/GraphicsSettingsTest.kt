package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphicsSettingsTest {

    @Test
    fun swanStationMapsUpscalePgxpWidescreenAndTextureFilter() {
        val cfg = GraphicsConfig(
            upscale = 3,
            textureFilter = TextureFilterMode.ENHANCED,
            widescreenHack = true,
            pgxpEnabled = true,
            cropOverscan = true,
        )
        val opts = GraphicsCoreOptions.optionsFor("swanstation", cfg)
        assertEquals("3", opts["duckstation_GPU.ResolutionScale"])
        assertEquals("xBR", opts["duckstation_GPU.TextureFilter"])
        assertEquals("true", opts["duckstation_GPU.WidescreenHack"])
        assertEquals("true", opts["duckstation_GPU.PGXPEnable"])
        assertEquals("Overscan", opts["duckstation_Display.CropMode"])
    }

    @Test
    fun ppssppAndDolphinMapUpscalingAndAnisotropyCleanly() {
        val cfg4x = GraphicsConfig(upscale = 4, textureFilter = TextureFilterMode.ENHANCED, widescreenHack = true)
        val psp = GraphicsCoreOptions.optionsFor("ppsspp", cfg4x)
        assertEquals("1920x1088", psp["ppsspp_internal_resolution"])
        assertEquals("16x", psp["ppsspp_texture_anisotropic_filtering"])

        val gc = GraphicsCoreOptions.optionsFor("dolphin", cfg4x)
        assertEquals("x4 (2560 x 2112)", gc["dolphin_efb_scale"])
        assertEquals("enabled", gc["dolphin_widescreen_hack"])
        assertEquals("16x", gc["dolphin_anisotropy"])
    }

    @Test
    fun n64And2DCoresMapCapabilitiesAccurately() {
        assertTrue(GraphicsCoreOptions.is3DCore("mupen64plus_next_gles3"))
        assertTrue(GraphicsCoreOptions.supportsWidescreenHack("mupen64plus_next_gles3"))
        assertFalse(GraphicsCoreOptions.is3DCore("snes9x"))
        assertFalse(GraphicsCoreOptions.supportsPgxp("mupen64plus_next_gles3"))

        val snes = GraphicsCoreOptions.optionsFor("snes9x", GraphicsConfig(cropOverscan = true))
        assertEquals("enabled", snes["snes9x_overscan"])
    }
}
