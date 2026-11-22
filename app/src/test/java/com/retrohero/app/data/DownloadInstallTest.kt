package com.retrohero.app.data

import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadInstallTest {
    private val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()

    @Test
    fun biosZipIsUnpackedAndMatchedByName() {
        val zip = File(app.cacheDir, "my bios pack.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("BIOS/SCPH5501.BIN")); out.write(ByteArray(512) { 1 }); out.closeEntry()
            out.putNextEntry(ZipEntry("readme.txt")); out.write("hi".toByteArray()); out.closeEntry()
        }
        val results = app.bios.install(zip, zip.name)
        assertEquals(listOf("scph5501.bin"), results.map { it.name })
        assertFalse("a fake file must not verify", results.single().verified)
        assertTrue(File(app.paths.system, "scph5501.bin").exists())
        val psxBios = Systems.get("psx")!!.bios.first { it.fileName == "scph5501.bin" }
        assertEquals(BiosManager.Status.Unverified, app.bios.status(psxBios))
    }

    @Test
    fun arcadeBiosZipIsKeptWhole() {
        val zip = File(app.cacheDir, "neogeo.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("sp-s2.sp1")); out.write(ByteArray(64)); out.closeEntry()
        }
        val results = app.bios.install(zip, zip.name)
        assertEquals("neogeo.zip", results.single().name)
        assertTrue(File(File(app.paths.system, "fbneo"), "neogeo.zip").exists())
    }

    @Test
    fun downloadedGamesAreClassifiedAndDeletable() = runBlocking {
        val gba = File(app.paths.downloads, "Homebrew Quest.gba").apply { writeBytes(ByteArray(1024)) }
        val game = app.library.addDownloaded(gba)
        assertNotNull(game)
        assertEquals("gba", game!!.systemId)
        assertTrue(game.isLocal)
        assertTrue(app.library.games.value.any { it.id == game.id })

        // The ROM cache hands local files straight to the core.
        assertEquals(gba.absolutePath, app.romCache.prepare(game).absolutePath)

        app.library.deleteLocal(game)
        assertFalse(gba.exists())
        assertFalse(app.library.games.value.any { it.id == game.id })

        val unknown = File(app.paths.downloads, "notes.txt").apply { writeText("x") }
        assertNull(app.library.addDownloaded(unknown))
    }

    @Test
    fun rescanFindsDownloadsWithoutAnyFolders() = runBlocking {
        File(app.paths.downloads, "Space Shooter.nes").writeBytes(ByteArray(16))
        app.library.scan(emptyList())
        assertEquals(listOf("nes"), app.library.games.value.map { it.systemId })
    }
}
