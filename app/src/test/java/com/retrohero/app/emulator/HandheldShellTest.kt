package com.retrohero.app.emulator

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.retrohero.app.data.DeltaAction
import com.retrohero.app.data.DeltaMappedInput
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.data.Looks
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi")
class HandheldShellTest {
    @get:Rule
    val compose = createComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    @Test
    fun parsesDeltaPerItemAssetAndScreenshotAction() {
        val json = """
            {
              "name": "Standard GBA",
              "identifier": "com.delta.gba.standard",
              "gameTypeIdentifier": "com.rileytestut.delta.game.gba",
              "representations": {
                "iphone": {
                  "standard": {
                    "portrait": {
                      "assets": { "resizable": "iphone_portrait.pdf" },
                      "mappingSize": { "width": 320, "height": 568 },
                      "items": [
                        {
                          "asset": { "name": "iphone_a.pdf" },
                          "inputs": ["a"],
                          "frame": { "x": 249, "y": 74, "width": 56, "height": 56 }
                        },
                        {
                          "inputs": ["screenshot"],
                          "frame": { "x": 10, "y": 10, "width": 30, "height": 30 }
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val skin = DeltaSkins.parseInfo(json, File("/tmp"))
        val rep = skin.representations.iphoneStandard!!.portrait!!
        assertEquals("iphone_a.pdf", rep.items[0].asset)
        assertEquals(
            DeltaMappedInput.Action(DeltaAction.SCREENSHOT),
            DeltaSkins.mapInput("screenshot", "gba"),
        )
    }

    @Test
    fun renderHandheldShells() {
        val cases = listOf(
            "gba" to "shell_gba",
            "gbc" to "shell_gbc",
            "snes" to "shell_snes",
            "nes" to "shell_nes",
            "psx" to "shell_psx",
        )
        val activeSys = androidx.compose.runtime.mutableStateOf("gba")
        compose.setContent {
            val sys = Systems.get(activeSys.value)!!
            val style = ConsoleStyles.forSystem(sys.id, sys.color)
            RetroHeroTheme {
                HandheldShell(
                    style = style,
                    fastForwardActive = sys.id == "gbc",
                    sustainedCount = if (sys.id == "snes") 1 else 0,
                    gameContent = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1A2A6C), Color(0xFFB21F1F), Color(0xFFFDBB2D)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {}
                    },
                    padContent = {
                        VirtualGamepad(
                            layout = sys.layout,
                            landscape = false,
                            opacity = 1f,
                            onButton = { _, _ -> },
                            onMenu = {},
                            onFastForward = {},
                            onPress = {},
                            modifier = Modifier.fillMaxSize(),
                            accent = sys.color,
                            skin = Looks.Console,
                            systemId = sys.id,
                            inShell = true,
                        )
                    },
                )
            }
        }
        for ((sysId, name) in cases) {
            compose.runOnIdle { activeSys.value = sysId }
            compose.waitForIdle()
            val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
            File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun renderAmbilightShellAndLandscapeGrips() {
        val vividAmbilight = AmbilightState(
            enabled = true,
            top = Color(0xFF00E5FF),
            bottom = Color(0xFFFF5500),
            left = Color(0xFFFF2A85),
            right = Color(0xFFFFC400),
            flash = 0.65f,
            intensity = AmbilightMode.HYPER.multiplier,
        )
        val landscapeMode = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            val sys = Systems.get(if (landscapeMode.value) "psp" else "snes")!!
            val style = ConsoleStyles.forSystem(sys.id, sys.color)
            RetroHeroTheme {
                if (!landscapeMode.value) {
                    HandheldShell(
                        style = style,
                        ambilight = vividAmbilight,
                        gameContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF00B8D4), Color(0xFF311B92), Color(0xFFFF5500)),
                                        ),
                                    ),
                            )
                        },
                        padContent = {
                            VirtualGamepad(
                                layout = sys.layout,
                                landscape = false,
                                opacity = 1f,
                                onButton = { _, _ -> },
                                onMenu = {},
                                onFastForward = {},
                                onPress = {},
                                modifier = Modifier.fillMaxSize(),
                                accent = sys.color,
                                skin = Looks.Console,
                                systemId = sys.id,
                                inShell = true,
                            )
                        },
                    )
                } else {
                    LandscapeConsoleShell(
                        style = style,
                        ambilight = vividAmbilight,
                        gameContent = {
                            Box(
                                Modifier
                                    .fillMaxSize(0.62f)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFF2A85), Color(0xFF1A103C), Color(0xFFFFC400)),
                                        ),
                                    ),
                            )
                        },
                        padContent = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        val portraitBmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "shell_snes_ambilight.png").outputStream().use {
            portraitBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        compose.runOnIdle { landscapeMode.value = true }
        compose.waitForIdle()
        val landscapeBmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "shell_psp_landscape_ambilight.png").outputStream().use {
            landscapeBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
