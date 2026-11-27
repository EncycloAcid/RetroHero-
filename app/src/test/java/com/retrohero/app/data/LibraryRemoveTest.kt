package com.retrohero.app.data

import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryRemoveTest {
    private val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()

    @Test
    fun folderGameIsHiddenNotDeletedAndUndoBringsItBack() = runBlocking {
        val lib = app.library
        val game = Game("content://tree/doc/Thwaite.nes", "Thwaite.nes", "nes", "content://tree", "primary:Roms", 1L)
        lib.remove(game)
        assertFalse(lib.games.value.any { it.id == game.id })
        assertEquals(1, lib.hiddenCount.value)

        // A fresh library instance (app restart) still hides it.
        assertFalse(GameLibrary(app, app.paths).games.value.any { it.id == game.id })

        lib.unhide(game)
        assertTrue(lib.games.value.any { it.id == game.id })
        assertEquals(0, lib.hiddenCount.value)
    }
}
