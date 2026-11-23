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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import com.retrohero.app.ui.theme.polygon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
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
import com.retrohero.app.ui.theme.Tone
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
                        systemId = systemId,
                        onRewind = {},
                    )
                }
            }
        }
        save(name)
    }

    @Test fun gamepadSnes() = gamepad("snes", "pad_snes")
    @Test fun gamepadPsx() = gamepad("psx", "pad_psx")
    @Test fun gamepadGenesis() = gamepad("genesis", "pad_genesis")
    @Test fun gamepadNes() = gamepad("nes", "pad_nes")
    @Test fun gamepadGb() = gamepad("gb", "pad_gb")
    @Test fun gamepadGba() = gamepad("gba", "pad_gba")

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
    fun searchAndQuickPlay() {
        val first = sample("Thwaite.nes", "nes")
        val second = sample("Micro City.gba", "gba")
        var launched: String? = null
        var opened: String? = null
        compose.setContent {
            RetroHeroTheme {
                GamesScreen(listOf(first, second), emptyList(), true, false, {}, {}, { opened = it.id }, {},
                    onQuickPlay = { launched = it.id })
            }
        }
        compose.onNodeWithText("Filters").performClick()
        compose.onNodeWithText("Not played yet").assertExists()
        compose.onNodeWithText("Filters").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Micro")
        compose.onNodeWithText(first.title).assertDoesNotExist()
        compose.onNodeWithContentDescription("Play ${second.title}").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(second.id, launched); org.junit.Assert.assertNull(opened) }
        compose.onNodeWithText(second.title).assertIsDisplayed().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(second.id, opened) }
    }

    @Test
    fun gameCardClickOpensGame() {
        val game = sample("Micro City.gba", "gba")
        var opened: String? = null
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.components.GameCard(game, onClick = { opened = game.id })
            }
        }
        compose.onNodeWithText(game.title).assertIsDisplayed().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(game.id, opened) }
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
        compose.onNodeWithText("1 / 4").performTouchInput { swipeLeft() }
        compose.mainClock.advanceTimeBy(1500)
        save("hero_page2")
        compose.onNodeWithText("Shuffle").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithText("Random pick").assertExists()
        save("shuffle")
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
        save("game_record")
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
        compose.onNodeWithText("Play stats").assertExists()
    }

    @Test
    fun shelvesPage() {
        val games = listOf("Thwaite (USA).nes" to "nes", "Libbet.gb" to "gb", "Micro City.gba" to "gba", "Sonic.md" to "genesis", "Mario.sfc" to "snes", "Zelda.nes" to "nes", "Tetris.gb" to "gb", "Metroid.gba" to "gba")
            .map { sample(it.first, it.second) }
        val stats = mapOf(games[0].id to com.retrohero.app.data.PlayStat(11_520, 6, 0), games[2].id to com.retrohero.app.data.PlayStat(4_200, 3, 0), games[4].id to com.retrohero.app.data.PlayStat(1_500, 2, 0))
        val couch = com.retrohero.app.data.GameCollection("c1", "Couch co-op", games.take(4).map { it.id })
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize()) {
                    androidx.compose.runtime.CompositionLocalProvider(com.retrohero.app.ui.components.LocalNavSpace provides com.retrohero.app.ui.components.FloatingNavHeight) {
                        com.retrohero.app.ui.screens.ShelvesScreen(
                            games = games, recents = listOf(games[0], games[2], games[4]), favorites = listOf(games[1], games[3]),
                            collections = listOf(couch to games.take(4)), stats = stats,
                            onOpenGame = {}, onOptions = {}, onOpenCollection = {}, onNewShelf = {}, onCoverFlow = {},
                        )
                    }
                    com.retrohero.app.ui.components.FloatingNavBar(
                        items = navigationItems,
                        selected = 1, onSelect = {}, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        save("shelves_page")
    }

    @Test
    fun instrumentScreenshots() {
        val games = listOf(
            "Thwaite (USA).nes" to "nes",
            "Libbet.gb" to "gb",
            "Micro City.gba" to "gba",
            "Sonic.md" to "genesis",
            "Mario.sfc" to "snes",
            "Zelda.nes" to "nes",
        ).map { sample(it.first, it.second) }
        val recents = games.take(2)
        val favorites = games.take(3)
        val stats = mapOf(games[0].id to com.retrohero.app.data.PlayStat(11_520, 6, 0))
        val collection = com.retrohero.app.data.GameCollection("instrument", "Couch co-op", games.take(3).map { it.id })
        val history = sampleHistory()
        val settings = com.retrohero.app.data.Settings(ApplicationProvider.getApplicationContext())
        settings.accent = "ember"
        val activeScreen = mutableStateOf("home")
        settings.darkMode = false
        Tone.setDarkMode(false)
        compose.setContent {
            RetroHeroTheme {
                when (activeScreen.value) {
                    "home" -> com.retrohero.app.ui.screens.HomeScreen(
                        games = games, recents = recents, onPlay = {}, onOpen = {}, onOptions = {},
                        favorites = favorites, onOpenSystem = {}, onSeeAllGames = {}, topActions = {},
                        bottomPadding = 100.dp, stats = stats, collections = listOf(collection to games.take(3)),
                    )
                    "shelves" -> Box(Modifier.fillMaxSize()) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.retrohero.app.ui.components.LocalNavSpace provides com.retrohero.app.ui.components.FloatingNavHeight,
                        ) {
                            com.retrohero.app.ui.screens.ShelvesScreen(
                                games = games, recents = recents, favorites = favorites,
                                collections = listOf(collection to games.take(3)), stats = stats,
                                onOpenGame = {}, onOptions = {}, onOpenCollection = {}, onNewShelf = {}, onCoverFlow = {},
                            )
                        }
                        com.retrohero.app.ui.components.FloatingNavBar(
                            navigationItems, selected = 1, onSelect = {},
                            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                        )
                    }
                    "search" -> GamesScreen(
                        games = games, recents = recents, hasFolders = true, scanning = false,
                        onAddFolder = {}, onDownload = {}, onPlay = {}, onGameOptions = {},
                    )
                    "library" -> com.retrohero.app.ui.screens.LibraryScreen(
                        games = games, favorites = favorites, collections = listOf(collection to games.take(3)),
                        shots = emptyList(), history = history, onOpenGame = {}, onOptions = {}, onOpenSystem = {},
                        onOpenCollection = {}, onNewCollection = {}, onOpenStats = {},
                    )
                    "detail" -> com.retrohero.app.ui.screens.GameDetailScreen(
                        game = games.first(), currentCore = games.first().system!!.cores.first(),
                        saveSlots = listOf(-1 to System.currentTimeMillis()), onPlay = {}, onSelectCore = {},
                        onDelete = null, onClose = {}, stat = stats[games.first().id] ?: com.retrohero.app.data.PlayStat.None,
                    )
                    "settings" -> Column(
                        Modifier.fillMaxSize().background(Tone.Bg).padding(20.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    ) {
                        com.retrohero.app.ui.screens.LooksSettings(settings, emptyList(), {}, {})
                    }
                    else -> com.retrohero.app.ui.screens.OnboardingScreen(
                        hasGames = false, onAddFolder = {}, onDownloadLink = {}, onHomebrew = {}, onImportBios = {}, onFinish = {},
                    )
                }
            }
        }
        listOf(false, true).forEach { dark ->
            val appearance = if (dark) "dark" else "light"
            settings.darkMode = dark
            Tone.setDarkMode(dark)
            listOf(
                "home" to "home",
                "shelves" to "shelves",
                "search" to "search",
                "library" to "library",
                "detail" to "game_detail",
                "settings" to "settings_looks",
                "onboarding" to "onboarding",
            ).forEach { (screen, name) ->
                compose.runOnIdle { activeScreen.value = screen }
                compose.mainClock.advanceTimeBy(800)
                if (screen == "search") compose.onNodeWithText("Filters").performClick()
                compose.mainClock.advanceTimeBy(250)
                save("instrument_${name}_$appearance")
            }
        }
    }

    @Test
    fun darkAppearancePersists() {
        val settings = com.retrohero.app.data.Settings(ApplicationProvider.getApplicationContext())
        settings.darkMode = false
        Tone.setDarkMode(false)
        compose.setContent {
            RetroHeroTheme {
                Column(Modifier.fillMaxSize().background(Tone.Bg).padding(20.dp)) {
                    com.retrohero.app.ui.screens.LooksSettings(settings, emptyList(), {}, {})
                }
            }
        }
        compose.onNodeWithText("Dark").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertTrue(Tone.isDark)
            org.junit.Assert.assertTrue(settings.darkMode)
        }
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

    private fun sampleHistory(): List<com.retrohero.app.data.HistoryEntry> {
        val games = listOf("Thwaite (USA).nes" to "nes", "Libbet.gb" to "gb", "Micro City.gba" to "gba", "Sonic.md" to "genesis", "Mario.sfc" to "snes").map { sample(it.first, it.second) }
        val today = java.time.LocalDate.now()
        val rnd = java.util.Random(7)
        return (0 until 40).flatMap { back ->
            val day = today.minusDays(back.toLong())
            if (rnd.nextInt(4) == 0) emptyList() else List(1 + rnd.nextInt(2)) {
                val g = games[minOf(rnd.nextInt(games.size), rnd.nextInt(games.size))]
                val end = day.atTime(17 + rnd.nextInt(6), rnd.nextInt(60)).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                com.retrohero.app.data.HistoryEntry(g, end, 600L + rnd.nextInt(4000))
            }
        }.sortedByDescending { it.end }
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-night-xxhdpi")
    fun statsScreen() {
        val h = sampleHistory()
        compose.setContent { RetroHeroTheme { com.retrohero.app.ui.screens.StatsScreen(h, onBack = {}, onRecap = { _, _ -> }) } }
        compose.mainClock.advanceTimeBy(1500)
        compose.onAllNodesWithText("SYSTEMS").onFirst().performClick()
        compose.onAllNodesWithText("GAMES").onFirst().performClick()
        compose.mainClock.advanceTimeBy(1500)
        save("stats")
    }

    private fun recapPage(taps: Int, name: String, settle: Long = 4000) {
        val h = sampleHistory()
        val s = com.retrohero.app.data.PlayHistory.summarise(h, java.time.LocalDate.now().withDayOfMonth(1))
        compose.setContent { RetroHeroTheme { com.retrohero.app.ui.screens.RecapScreen("November", s, onClose = {}) } }
        compose.mainClock.advanceTimeBy(1200)
        repeat(taps) {
            compose.onRoot().performClick()
            compose.mainClock.advanceTimeBy(1200)
        }
        compose.mainClock.advanceTimeBy(settle)
        save(name)
    }

    @Test fun recapTotal() = recapPage(3, "recap_total")
    @Test fun recapSlot() = recapPage(5, "recap_slot", 6000)
    @Test fun recapReceipt() = recapPage(7, "recap_receipt")

    @Test
    @Config(qualifiers = "w411dp-h1300dp-night-xxhdpi")
    fun libraryAll() {
        val games = listOf("Thwaite (USA).nes" to "nes", "Libbet.gb" to "gb", "Micro City.gba" to "gba", "Sonic.md" to "genesis", "Mario.sfc" to "snes", "Zelda.nes" to "nes").map { sample(it.first, it.second) }
        val h = sampleHistory()
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.LibraryScreen(
                    games = games, favorites = games.take(5),
                    collections = listOf(com.retrohero.app.data.GameCollection("c", "Couch co-op", games.take(3).map { it.id }) to games.take(3)),
                    shots = emptyList(), history = h,
                    onOpenGame = {}, onOptions = {}, onOpenSystem = {}, onOpenCollection = {}, onNewCollection = {}, onOpenStats = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        save("library_all")
        compose.onAllNodesWithText("History").onFirst().performClick()
        compose.mainClock.advanceTimeBy(800)
        save("library_history")
    }

    @Test
    fun miniPlayer() {
        val g = sample("Thwaite (USA).nes", "nes")
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A2430), Color.Black)))) {
                    com.retrohero.app.ui.components.MiniPlayer(
                        game = g, stat = com.retrohero.app.data.PlayStat(4000, 3, System.currentTimeMillis() - 3_600_000), glass = null,
                        onOpen = {}, onPlay = {}, onHide = {},
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 110.dp),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        save("mini_player")
    }

    @Test
    fun padNeon() {
        val system = Systems.get("snes")!!
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    com.retrohero.app.emulator.VirtualGamepad(
                        layout = system.layout, landscape = false, opacity = 0.9f,
                        onButton = { _, _ -> }, onMenu = {}, onFastForward = {}, onPress = {},
                        modifier = Modifier.fillMaxWidth().height(400.dp).align(androidx.compose.ui.Alignment.BottomCenter),
                        accent = system.color, skin = com.retrohero.app.data.Looks.Neon, onRewind = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        save("pad_neon")
    }

    private fun shelfGames() = listOf(
        "Thwaite (USA).nes" to "nes", "Crystal Quest (USA).sfc" to "snes", "Libbet.gb" to "gb",
        "Micro City (World).gba" to "gba", "Night Runner (USA).cue" to "psx", "Sonic Blast (USA).md" to "genesis",
    ).map { sample(it.first, it.second) }

    @Test
    fun shelf() {
        val games = shelfGames()
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.ShelfScreen(
                    games = games, stats = emptyMap(), recentIds = emptyList(), favorites = setOf(games[0].id),
                    tiltEnabled = false, reducedMotion = false, onPlay = {}, onDetails = {}, startWithSpines = false,
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        save("shelf_1")
        compose.onRoot().performTouchInput { swipe(androidx.compose.ui.geometry.Offset(right * 0.8f, centerY), androidx.compose.ui.geometry.Offset(right * 0.45f, centerY), 400) }
        compose.mainClock.advanceTimeBy(80)
        save("shelf_mid_swipe")
        compose.mainClock.advanceTimeBy(1500)
        save("shelf_2")
    }

    private fun openShelf(index: Int, name: String) {
        val games = shelfGames()
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.ShelfScreen(
                    games = listOf(games[index]), stats = emptyMap(), recentIds = emptyList(), favorites = emptySet(),
                    tiltEnabled = false, reducedMotion = false, onPlay = {}, onDetails = {}, startWithSpines = false,
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.onRoot().performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(500)
        save(name + "_opening")
        compose.mainClock.advanceTimeBy(3500)
        save(name)
    }

    @Test fun shelfOpenNes() = openShelf(0, "shelf_open_nes")
    @Test fun shelfOpenPsx() = openShelf(4, "shelf_open_psx")

    @Test
    @Config(qualifiers = "w720dp-h900dp-night-xxhdpi")
    fun caseStyles() {
        val games = shelfGames() + listOf(sample("Tactics (USA).nds", "nds"), sample("Racer (USA).cue", "segacd"), sample("Puzzle (USA).gbc", "gbc"), sample("Heroes (USA).sms", "sms"))
        compose.setContent {
            RetroHeroTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0D)).padding(top = 20.dp)) {
                    games.chunked(4).forEach { row ->
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().height(290.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            row.forEachIndexed { i, g ->
                                com.retrohero.app.ui.components.GameBox3D(g, 130.dp, rotY = if (i % 2 == 0) 24f else -20f, rotX = 8f)
                            }
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(600)
        save("case_styles")
    }

    @Test
    @Config(qualifiers = "w720dp-h500dp-night-xxhdpi")
    fun mediaStyles() {
        val games = shelfGames() + listOf(sample("Tactics (USA).nds", "nds"), sample("Racer (USA).cue", "segacd"), sample("Puzzle (USA).gbc", "gbc"))
        compose.setContent {
            RetroHeroTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0D)).padding(16.dp)) {
                    games.chunked(5).forEach { row ->
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().height(220.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) { row.forEach { g -> com.retrohero.app.ui.components.GameMedia(g, 110.dp) } }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(600)
        save("media_styles")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun spineWall() {
        val more = shelfGames() + listOf(
            "Tactics (USA).nds" to "nds", "Racer (USA).cue" to "segacd", "Puzzle (USA).gbc" to "gbc", "Heroes (USA).sms" to "sms",
            "Star Patrol (USA).nes" to "nes", "Blade Run (USA).sfc" to "snes", "Galaxy Siege (USA).cue" to "psx", "Dungeon Dash (USA).gba" to "gba",
            "Moonlight Run (USA).gb" to "gb", "Turbo Kart (USA).md" to "genesis", "Castle Climb (USA).nes" to "nes", "Neon Drift (USA).sfc" to "snes",
        ).map { sample(it.first, it.second) }
        compose.setContent {
            RetroHeroTheme {
                com.retrohero.app.ui.screens.ShelfScreen(
                    games = more, stats = emptyMap(), recentIds = emptyList(), favorites = emptySet(),
                    tiltEnabled = false, reducedMotion = false, onPlay = {}, onDetails = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        save("spine_wall")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun storeGrid() {
        val games = shelfGames()
        compose.setContent {
            RetroHeroTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    com.retrohero.app.ui.screens.GamesScreen(
                        games = games, recents = emptyList(), hasFolders = true, scanning = false,
                        onAddFolder = {}, onDownload = {}, onPlay = {}, onGameOptions = {},
                        onQuickPlay = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.onRoot().performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(800)
        save("store_grid")
    }
}
