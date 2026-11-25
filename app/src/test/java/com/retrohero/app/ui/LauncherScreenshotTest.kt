package com.retrohero.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.retrohero.app.ui.theme.RetroHeroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class LauncherScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun blankSlateRendersCleanly() {
        compose.setContent {
            RetroHeroTheme {
                BlankCanvas()
            }
        }
        compose.waitForIdle()
    }
}
