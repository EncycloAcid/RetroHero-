package com.retrohero.app.data

import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import com.retrohero.app.emulator.EmulatorActivity
import com.retrohero.app.emulator.PadAdjust
import com.retrohero.app.emulator.PadLayouts
import com.retrohero.app.ui.components.artSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeaturesTest {
    private val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()
    private val game = Game("content://x/Thwaite (USA).nes", "Thwaite (USA).nes", "nes", "content://x", "p", 1L)

    @Test
    fun cheatCodesAreNormalised() {
        assertEquals("SXIOPO+AAAA-BBBB", Cheats.normalise(" sxiopo \n aaaa-bbbb,"))
        val f = File(app.filesDir, "c.json")
        Cheats.save(f, listOf(Cheat("A", "X", false)))
        assertEquals(listOf(Cheat("A", "X", false)), Cheats.load(f))
    }

    @Test
    fun playTimeAddsUp() {
        val f = File(app.filesDir, "pt.json").apply { delete() }
        PlayTime.add(f, 90, newSession = true, now = 1000)
        PlayTime.add(f, 30, newSession = false, now = 2000)
        PlayTime.add(f, 0, newSession = true, now = 3000)
        assertEquals(PlayStat(120, 2, 3000), PlayTime.read(f))
        assertEquals("2 h 5 min", PlayTime.format(7500))
        assertEquals("under a minute", PlayTime.format(20))
    }

    @Test
    fun remapsOverrideDefaultsPerController() {
        val profiles = mapOf(
            Remap.ANY to mapOf(KeyEvent.KEYCODE_BUTTON_A to RetroPad.A),
            "pad1" to mapOf(KeyEvent.KEYCODE_BUTTON_A to Remap.REWIND, KeyEvent.KEYCODE_BUTTON_B to Remap.NONE),
        )
        assertEquals(RetroPad.A, Remap.resolve(profiles, "other", KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(Remap.REWIND, Remap.resolve(profiles, "pad1", KeyEvent.KEYCODE_BUTTON_A))
        assertNull(Remap.resolve(profiles, "pad1", KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(RetroPad.START, Remap.resolve(profiles, "pad1", KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(RetroPad.L3, Remap.resolve(emptyMap(), null, KeyEvent.KEYCODE_BUTTON_THUMBL, rewindOnL3 = false))
        assertTrue(KeyEvent.KEYCODE_BUTTON_A in Remap.keysFor(profiles, Remap.ANY, RetroPad.A))
        val f = File(app.filesDir, "remap_test.json")
        Remap.save(f, profiles)
        assertEquals(profiles, Remap.load(f))
        assertEquals("L1", Remap.keyName(KeyEvent.KEYCODE_BUTTON_L1))
    }

    @Test
    fun integerScalingPicksTheLargestFit() {
        // 256x224 at 4:3 into a 1080x900 area: 4x is 896 tall but 1195 wide, so 3x.
        val (w, h) = EmulatorActivity.integerFit(1080f, 900f, 224, 4f / 3f)!!
        assertEquals(672f, h)
        assertEquals(896f, w)
        assertNull(EmulatorActivity.integerFit(100f, 100f, 224, 1f))
    }

    @Test
    fun padLayoutsAreSavedPerSystemAndOrientation() {
        val f = File(app.filesDir, "pad_test.json").apply { delete() }
        PadLayouts(f).set("snes", true, mapOf("dpad" to PadAdjust(10f, -5f, 1.2f), "face" to PadAdjust()))
        val again = PadLayouts(f)
        assertEquals(mapOf("dpad" to PadAdjust(10f, -5f, 1.2f)), again.get("snes", true))
        assertTrue(again.get("snes", false).isEmpty())
    }

    @Test
    fun artPrefersYourCoverThenOnlineThenScreenshots() {
        val paths = app.paths
        paths.coverFile(game).delete()
        assertEquals(game.artUrls, artSources(game, paths))
        val shot = File(paths.screenshotsDir(game).apply { mkdirs() }, "1.png").apply { writeBytes(byteArrayOf(1)) }
        val cover = paths.coverFile(game).apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(2)) }
        val sources = artSources(game, paths)
        assertEquals(cover, sources.first())
        assertEquals(shot, sources.last())
        assertEquals(game.artUrls, sources.subList(1, sources.size - 1))
    }

    @Test
    fun artComesFromTheLibretroThumbnailsMirror() {
        val g = Game("content://x/smb", "Super Mario Bros. (World).nes", "nes", "t", "p", 1)
        org.junit.Assert.assertEquals(
            "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Nintendo_Entertainment_System/HEAD/Named_Boxarts/Super%20Mario%20Bros.%20(World).png",
            g.artUrls.first(),
        )
        // No region in the name: common regions are guessed for box art.
        val plain = Game("content://x/t", "Tetris.gb", "gb", "t", "p", 1)
        org.junit.Assert.assertTrue(plain.artUrls.any { it.endsWith("/Named_Boxarts/Tetris%20(World).png") })
    }
}
