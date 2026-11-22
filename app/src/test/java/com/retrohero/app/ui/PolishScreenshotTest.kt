package com.retrohero.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.R
import com.retrohero.app.data.Systems
import com.retrohero.app.emulator.VirtualGamepad
import com.retrohero.app.ui.screens.GamesScreen
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the in-game gamepad, the scanning skeleton and the launcher icon to PNGs. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-night-xxhdpi")
class PolishScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun save(name: String) {
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun gamepad(systemId: String, name: String) {
        val system = Systems.get(systemId)!!
        compose.setContent {
            RetroHeroTheme(dynamicColor = false) {
                Column(Modifier.fillMaxSize().background(Color.Black)) {
                    // Stand-in for the game picture.
                    Box(
                        Modifier.fillMaxWidth().weight(1f).background(
                            Brush.verticalGradient(listOf(Color(0xFF3B6BD9), Color(0xFF79C1FF), Color(0xFF3E9A4C))),
                        ),
                    )
                    VirtualGamepad(
                        layout = system.layout,
                        landscape = false,
                        opacity = 0.85f,
                        onButton = { _, _ -> },
                        onMenu = {},
                        onFastForward = {},
                        onPress = {},
                        accent = system.color,
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                    )
                }
            }
        }
        save(name)
    }

    @Test fun gamepadSnes() = gamepad("snes", "pad_snes")
    @Test fun gamepadPsx() = gamepad("psx", "pad_psx")
    @Test fun gamepadGenesis() = gamepad("genesis", "pad_genesis")

    @Test
    fun scanningSkeleton() {
        compose.setContent {
            RetroHeroTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    GamesScreen(
                        games = emptyList(), recents = emptyList(), hasFolders = true, scanning = true,
                        onAddFolder = {}, onDownload = {}, onPlay = {}, onGameOptions = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(400)
        save("skeleton")
    }

    @Test
    fun launcherIcon() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = ctx.getDrawable(R.mipmap.ic_launcher)!!
        val bmp = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 432, 432)
        icon.draw(Canvas(bmp))
        File(outDir, "icon.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
