package com.retrohero.app.data

import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupTest {
    private val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()
    private fun game(id: String, file: String, sys: String = "nes") = Game(id, file, sys, "content://tree", "primary:Roms", 1L)

    @Test
    fun roundTripRestoresFilesAndMatchesGamesByName() {
        val oldPhone = listOf(game("content://old/1", "Thwaite.nes"), game("content://old/2", "Libbet.gb", "gb"))
        val sram = app.paths.saveRam(oldPhone[0]).apply { writeText("save data") }
        val shot = File(app.paths.screenshotsDir(oldPhone[0]).apply { mkdirs() }, "1.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        Cheats.save(app.paths.cheatsFile(oldPhone[0]), listOf(Cheat("Lives", "SXIOPO")))
        PlayTime.add(app.paths.playtimeFile(oldPhone[0]), 3600, true)
        File(app.paths.system, "bios.bin").writeText("bios")
        app.settings.toggleFavorite("content://old/2")
        app.settings.createCollection("Couch co-op", "content://old/1")
        app.settings.fastForwardSpeed = 5f

        val bytes = ByteArrayOutputStream().also { Backup(app.filesDir, app.settings).write(it, oldPhone, includeBios = false, appVersion = "test") }.toByteArray()
        val names = ZipInputStream(ByteArrayInputStream(bytes)).use { z -> generateSequence { z.nextEntry?.name }.toList() }
        assertEquals("manifest.json", names.first())
        assertFalse("BIOS only when asked", names.any { it.startsWith("system/") })

        // "New phone": wipe progress and settings, same games under different ids.
        listOf("saves", "screenshots", "cheats", "playtime").forEach { File(app.filesDir, it).deleteRecursively() }
        app.settings.toggleFavorite("content://old/2")
        app.settings.setCollections(emptyList())
        app.settings.fastForwardSpeed = 2f
        val newPhone = listOf(game("content://new/a", "Thwaite.nes"), game("content://new/b", "Libbet.gb", "gb"))

        val r = Backup(app.filesDir, app.settings).restore(ByteArrayInputStream(bytes), newPhone)
        assertEquals("save data", sram.readText())
        assertTrue(shot.exists())
        assertEquals("SXIOPO", Cheats.load(app.paths.cheatsFile(newPhone[0])).single().code)
        assertEquals(3600, PlayTime.read(app.paths.playtimeFile(newPhone[0])).seconds)
        assertEquals(setOf("content://new/b"), app.settings.favorites.value)
        assertEquals(listOf("content://new/a"), app.settings.collections.value.single { it.name == "Couch co-op" }.gameIds)
        assertEquals(5f, app.settings.fastForwardSpeed)
        assertEquals(0, r.unmatched)
        assertEquals(1, r.favorites)
    }

    @Test
    fun restoreRejectsPathsOutsideTheBackupFolders() {
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z ->
                fun put(name: String, text: String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry() }
                put("manifest.json", """{"format":1}""")
                put("../evil.txt", "x")
                put("saves/../../evil2.txt", "x")
                put("/abs.txt", "x")
                put("random/file.txt", "x")
                put("saves/nes/ok.srm", "ok")
            }
        }.toByteArray()
        val r = Backup(app.filesDir, app.settings).restore(ByteArrayInputStream(zip), emptyList())
        assertEquals(1, r.files)
        assertTrue(File(app.filesDir, "saves/nes/ok.srm").exists())
        assertFalse(File(app.filesDir.parentFile, "evil.txt").exists())
        assertFalse(File(app.filesDir.parentFile!!.parentFile, "evil2.txt").exists())
        val b = Backup(app.filesDir, app.settings)
        assertNull(b.targetFor("states/./x"))
        assertNull(b.targetFor("states\\..\\x"))
    }

    @Test
    fun restoreRefusesFilesThatAreNotBackups() {
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z -> z.putNextEntry(ZipEntry("game.nes")); z.write(1); z.closeEntry() }
        }.toByteArray()
        assertThrows(Backup.BadBackupException::class.java) {
            Backup(app.filesDir, app.settings).restore(ByteArrayInputStream(zip), emptyList())
        }
    }
}
