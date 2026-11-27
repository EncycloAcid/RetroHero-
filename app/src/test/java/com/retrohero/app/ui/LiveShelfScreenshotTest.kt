package com.retrohero.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameMetadataRepository
import com.retrohero.app.ui.components.M3BookshelfSection
import com.retrohero.app.ui.theme.RetroHeroTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Renders the Physical Shelf from real LaunchBox Games Database scans (Box - Spine, Box - Front,
 * Box - Full) for well-known games, exactly as the app fetches them. Needs the network, so it is
 * skipped (not failed) when LaunchBox can't be reached. Writes build/screenshots/bookshelf_launchbox.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h891dp-xxhdpi")
class LiveShelfScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun game(file: String, sys: String) = Game("live:$file", file, sys, "tree", "doc", 4096L)

    private fun launchBoxReachable(): Boolean = runCatching {
        (URL("https://gamesdb.launchbox-app.com/").openConnection() as HttpURLConnection).run {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            responseCode in 200..399
        }
    }.getOrDefault(false)

    @Test
    fun shelfWithRealLaunchBoxArt() {
        assumeTrue("LaunchBox is not reachable", launchBoxReachable())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = GameMetadataRepository(File(context.cacheDir, "live_meta"))
        val games = listOf(
            game("Super Metroid (Japan, USA) (En).sfc", "snes"),
            game("Sonic The Hedgehog 2 (World).md", "genesis"),
            game("Metal Gear Solid (USA) (Disc 1).cue", "psx"),
            game("Super Mario 64 (USA).z64", "n64"),
            game("Castlevania - Symphony of the Night (USA).cue", "psx"),
            game("Pokemon - Platinum Version (USA).nds", "nds"),
            game("Super Mario World (USA).sfc", "snes"),
        )
        // Same lookups the app makes; logged so CI shows which real scans LaunchBox returned.
        val dossiers = games.associate { g -> g.id to runBlocking { repo.load(g) } }
        dossiers.forEach { (id, d) ->
            println("LaunchBox ${id.removePrefix("live:")}: front=${d.boxArtUrl != null} spine=${d.boxSpineUrl != null} full=${d.boxFullUrl != null} back=${d.boxBackUrl != null} disc=${d.discUrl != null} cart=${d.cartFrontUrl != null}")
        }

        compose.setContent {
            RetroHeroTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 24.dp),
                ) {
                    M3BookshelfSection(
                        games = games,
                        stats = emptyMap(),
                        onOpenDetail = {},
                        onQuickPlay = {},
                        dossierFor = { repo.peekCached(it) ?: dossiers[it.id] },
                        loadDossier = { repo.load(it) },
                    )
                }
            }
        }
        // Scans download on real threads; let them land and recompose.
        repeat(40) {
            Thread.sleep(500)
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Super Metroid").performClick()
        repeat(16) {
            Thread.sleep(500)
            compose.waitForIdle()
        }
        val outDir = File("build/screenshots").apply { mkdirs() }
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outDir, "bookshelf_launchbox.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
