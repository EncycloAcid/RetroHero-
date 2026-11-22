package com.retrohero.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.retrohero.app.ui.theme.polygon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipe
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
    fun onboarding() {
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.OnboardingScreen(
                    hasGames = false, onAddFolder = {}, onDownloadLink = {}, onHomebrew = {}, onImportBios = {}, onFinish = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(600)
        save("onboarding_1")
        compose.onAllNodesWithText("Get started").onFirst().performClick()
        compose.mainClock.advanceTimeBy(800)
        save("onboarding_2")
    }

    @Test
    fun homebrewSheet() {
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    com.retrohero.app.ui.components.HomebrewSheet(installed = setOf("libbet.gb"), onGet = {}, onDismiss = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        save("homebrew")
    }

    @Test
    fun systemMorph() {
        val systems = listOf("nes", "snes", "nds", "psx").map { Systems.get(it)!! }
        compose.setContent {
            RetroHeroTheme(dynamicColor = false) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    systems.forEach { system ->
                        androidx.compose.foundation.layout.Row {
                            val morph = com.retrohero.app.ui.components.rememberMorph(
                                system.polygon,
                                androidx.compose.material3.MaterialShapes.Circle,
                            )
                            listOf(0f, 0.33f, 0.66f, 1f).forEach { p ->
                                Box(
                                    Modifier
                                        .padding(12.dp)
                                        .size(80.dp)
                                        .background(system.color, com.retrohero.app.ui.components.MorphShape(morph, p)),
                                )
                            }
                        }
                    }
                }
            }
        }
        save("morph")
    }

    @Test
    fun launcherIconShapes() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fg = ctx.getDrawable(R.drawable.ic_launcher_foreground)!!
        val bg = ctx.getDrawable(R.drawable.ic_launcher_background)!!
        val mono = ctx.getDrawable(R.drawable.ic_launcher_monochrome)!!
        val px = 432
        val out = Bitmap.createBitmap(px * 4 + 90, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.rgb(36, 36, 40))
        // Launcher masks: circle, squircle-ish rounded square, teardrop-free square, themed (mono on tonal).
        val masks = listOf<(android.graphics.Path) -> Unit>(
            { it.addCircle(px / 2f, px / 2f, px / 2f * 0.92f, android.graphics.Path.Direction.CW) },
            { it.addRoundRect(px * 0.04f, px * 0.04f, px * 0.96f, px * 0.96f, px * 0.24f, px * 0.24f, android.graphics.Path.Direction.CW) },
            { it.addRoundRect(px * 0.04f, px * 0.04f, px * 0.96f, px * 0.96f, px * 0.08f, px * 0.08f, android.graphics.Path.Direction.CW) },
        )
        // Adaptive icons render a 108dp canvas scaled so the 72dp viewport fills the mask.
        val full = (px * 1.5f).toInt()
        val inset = (full - px) / 2
        masks.forEachIndexed { i, mask ->
            canvas.save()
            canvas.translate(i * (px + 30f), 0f)
            val path = android.graphics.Path().also(mask)
            canvas.clipPath(path)
            bg.setBounds(-inset, -inset, px + inset, px + inset); bg.draw(canvas)
            fg.setBounds(-inset, -inset, px + inset, px + inset); fg.draw(canvas)
            canvas.restore()
        }
        canvas.save()
        canvas.translate(3 * (px + 30f), 0f)
        canvas.clipPath(android.graphics.Path().also(masks[0]))
        canvas.drawColor(android.graphics.Color.rgb(222, 214, 255))
        mono.setTint(android.graphics.Color.rgb(60, 40, 150))
        mono.setBounds(-inset, -inset, px + inset, px + inset); mono.draw(canvas)
        canvas.restore()
        File(outDir, "icon_shapes.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun logoMark() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mark = ctx.getDrawable(R.drawable.ic_logo_mark)!!
        val bmp = Bitmap.createBitmap(900, 260, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(android.graphics.Color.BLACK)
        var x = 30
        for (h in listOf(30, 60, 120, 200)) {
            val w = h * 56 / 38
            mark.setBounds(x, 20, x + w, 20 + h)
            mark.draw(canvas)
            x += w + 30
        }
        File(outDir, "logo_mark.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
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

    private fun sample(name: String, sys: String): com.retrohero.app.data.Game {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
        return com.retrohero.app.data.Game("$tree/document/primary%3ARoms%2F$name", name, sys, tree, "primary:Roms", 1L)
    }

    @Test
    fun gameMenu() {
        val g = sample("Thwaite (USA).nes", "nes")
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    com.retrohero.app.ui.components.GameMenuSheet(
                        game = g, hasResume = true, isFavorite = false,
                        onPlay = {}, onResume = {}, onFresh = {}, onToggleFavorite = {}, onDetails = {}, onOpenSystem = {},
                        onDelete = {}, onDismiss = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        save("game_menu")
    }

    @Test
    fun heroPages() {
        val games = listOf("Thwaite (USA).nes" to "nes", "Libbet.gb" to "gb", "Micro City.gba" to "gba", "Sonic.md" to "genesis", "Mario.sfc" to "snes", "Zelda.nes" to "nes").map { sample(it.first, it.second) }
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.HomeScreen(
                    games = games, recents = listOf(games[0]), onPlay = {}, onOpen = {}, onOptions = {}, onOpenSystem = {}, onSeeAllGames = {},
                    topActions = {}, favorites = games.take(4), bottomPadding = 100.dp,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        save("hero_page1")
        compose.onRoot().performTouchInput { swipeLeft() }
        compose.mainClock.advanceTimeBy(1500)
        save("hero_page2")
        compose.onRoot().performTouchInput { swipeLeft() }
        compose.mainClock.advanceTimeBy(1500)
        save("hero_page3")
    }

    @Test
    fun systemSwipeRows() {
        val games = listOf("Thwaite (USA).nes", "Zelda.nes", "Metroid.nes", "Kirby.nes", "Tetris.nes").map { sample(it, "nes") }
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.SystemDetailScreen(
                    system = Systems.get("nes")!!, games = games, onBack = {}, onOpenGame = {}, onPlay = {}, onPlayRandom = {},
                    favorites = setOf(games[1].id),
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        save("system_rows")
    }

    private fun pngFile(name: String, color: Int): java.io.File {
        val app = ApplicationProvider.getApplicationContext<com.retrohero.app.RetroHeroApp>()
        val bmp = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(color)
        val paint = android.graphics.Paint().apply { this.color = android.graphics.Color.WHITE }
        c.drawRect(8f, 30f, 56f, 40f, paint)
        c.drawCircle(20f, 16f, 6f, paint)
        return java.io.File(app.cacheDir, name).apply { outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    @Test
    fun padEditing() {
        val system = Systems.get("snes")!!
        compose.setContent {
            RetroHeroTheme {
                Column(Modifier.fillMaxSize().background(Color.Black)) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(Color(0xFF2B2440), Color(0xFF0E0C14)))))
                    com.retrohero.app.emulator.VirtualGamepad(
                        layout = system.layout, landscape = false, opacity = 0.7f,
                        onButton = { _, _ -> }, onMenu = {}, onFastForward = {}, onPress = {},
                        modifier = Modifier.fillMaxWidth().height(400.dp), accent = system.color,
                        onRewind = {},
                        edit = com.retrohero.app.emulator.PadEditState(
                            adjust = mapOf("dpad" to com.retrohero.app.emulator.PadAdjust(12f, -20f, 1.15f), "face" to com.retrohero.app.emulator.PadAdjust(-6f, 0f, 0.9f)),
                            selected = "dpad", onSelect = {}, onAdjust = { _, _ -> },
                        ),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        save("pad_edit")
    }

    @Test
    fun gameDetailWithStats() {
        val g = sample("Thwaite (USA).nes", "nes")
        val shots = listOf(pngFile("s1.png", 0xFF3A6EA5.toInt()), pngFile("s2.png", 0xFFB5523B.toInt()), pngFile("s3.png", 0xFF3B8F5A.toInt()))
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.GameDetailScreen(
                    game = g, currentCore = g.system!!.cores.first(), saveSlots = listOf(-1 to 1_700_000_000_000L),
                    onPlay = {}, onSelectCore = {}, onDelete = null, onClose = {},
                    stat = com.retrohero.app.data.PlayStat(3 * 3600 + 12 * 60, 9, System.currentTimeMillis() - 86_400_000L),
                    screenshots = shots,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.onRoot().performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(1000)
        save("detail_stats")
    }

    @Test
    @Config(qualifiers = "w411dp-h1700dp-night-xxhdpi")
    fun homeWithCollectionsAndStats() {
        val games = listOf("Thwaite (USA).nes" to "nes", "Libbet.gb" to "gb", "Micro City.gba" to "gba", "Sonic.md" to "genesis", "Mario.sfc" to "snes", "Zelda.nes" to "nes")
            .map { sample(it.first, it.second) }
        val stats = mapOf(games[0].id to com.retrohero.app.data.PlayStat(11_520, 6, 0), games[2].id to com.retrohero.app.data.PlayStat(4_200, 3, 0), games[4].id to com.retrohero.app.data.PlayStat(1_500, 2, 0))
        val couch = com.retrohero.app.data.GameCollection("c1", "Couch co-op", games.take(4).map { it.id })
        val rpg = com.retrohero.app.data.GameCollection("c2", "Weekend RPGs", listOf(games[4].id))
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.HomeScreen(
                    games = games, recents = listOf(games[0], games[2]), onPlay = {}, onOpen = {}, onOptions = {}, onOpenSystem = {}, onSeeAllGames = {},
                    topActions = {}, bottomPadding = 100.dp, stats = stats,
                    collections = listOf(couch to games.take(4), rpg to listOf(games[4])),
                )
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        save("home_collections")
        repeat(3) {
            compose.onRoot().performTouchInput {
                swipe(androidx.compose.ui.geometry.Offset(right * 0.9f, 900f), androidx.compose.ui.geometry.Offset(left + 20f, 900f), 200)
            }
            compose.mainClock.advanceTimeBy(2000)
        }
        save("home_stats_page")
    }

    @Test
    fun collectionPage() {
        val games = listOf("Thwaite (USA).nes" to "nes", "Libbet.gb" to "gb", "Micro City.gba" to "gba").map { sample(it.first, it.second) }
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.CollectionScreen(
                    collection = com.retrohero.app.data.GameCollection("c1", "Couch co-op", games.map { it.id }),
                    games = games, onBack = {}, onOpenGame = {}, onPlay = {}, onPlayRandom = {}, onRemove = {}, onOptions = {}, onRename = {}, onDelete = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        save("collection_page")
    }

    @Test
    fun collectionPicker() {
        val g = sample("Thwaite (USA).nes", "nes")
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    com.retrohero.app.ui.components.CollectionPickerSheet(
                        game = g,
                        collections = listOf(
                            com.retrohero.app.data.GameCollection("c1", "Couch co-op", listOf(g.id, "x", "y")),
                            com.retrohero.app.data.GameCollection("c2", "Weekend RPGs", listOf("z")),
                        ),
                        onToggle = { _, _ -> }, onCreate = {}, onDismiss = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        save("collection_picker")
    }

    @Test
    fun remapPage() {
        val profiles = mapOf(com.retrohero.app.data.Remap.ANY to mapOf(android.view.KeyEvent.KEYCODE_BUTTON_R1 to com.retrohero.app.data.Remap.FAST_FORWARD))
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.RemapScreen(
                    profiles = profiles,
                    controllers = listOf(com.retrohero.app.ui.screens.ControllerProfile("d1", "Xbox Wireless Controller")),
                    onChange = {}, onBack = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(600)
        save("remap")
    }
}
