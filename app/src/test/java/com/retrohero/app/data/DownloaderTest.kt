package com.retrohero.app.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloaderTest {
    @get:Rule val tmp = TemporaryFolder()
    private val server = MockWebServer()

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    @Test
    fun rewritesShareLinks() {
        assertEquals(
            "https://drive.google.com/uc?export=download&id=abc123",
            Downloader.normalize("https://drive.google.com/file/d/abc123/view?usp=sharing"),
        )
        assertEquals("https://www.dropbox.com/s/x/game.zip?dl=1", Downloader.normalize("https://www.dropbox.com/s/x/game.zip?dl=0"))
        assertEquals("https://www.dropbox.com/s/x/game.zip?dl=1", Downloader.normalize("https://www.dropbox.com/s/x/game.zip"))
        assertEquals("https://example.com/a.gba", Downloader.normalize("  https://example.com/a.gba "))
    }

    @Test
    fun validatesLinks() {
        assertNotNull(Downloader.validate(""))
        assertNotNull(Downloader.validate("http://example.com/a.zip"))
        assertNotNull(Downloader.validate("ftp://example.com/a.zip"))
        assertNull(Downloader.validate("https://example.com/a.zip"))
    }

    @Test
    fun namesFileFromContentDispositionAndFollowsRedirects() = runBlocking {
        val body = ByteArray(300_000) { (it % 251).toByte() }
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/real"))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setHeader("Content-Disposition", "attachment; filename=\"My Homebrew (World).gba\"")
                .setBody(Buffer().write(body)),
        )
        var lastProgress = 0L
        val file = Downloader.fetch(server.url("/share/123").toString(), tmp.root) { read, _ -> lastProgress = read }
        assertEquals("My Homebrew (World).gba", file.name)
        assertTrue(file.readBytes().contentEquals(body))
        assertEquals(body.size.toLong(), lastProgress)
        assertFalse(tmp.root.listFiles()!!.any { it.name.endsWith(".part") })
    }

    @Test
    fun namesFileFromUrlPath() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/zip").setBody("PK"))
        val file = Downloader.fetch(server.url("/files/Space%20Game.zip").toString(), tmp.root) { _, _ -> }
        assertEquals("Space Game.zip", file.name)
    }

    @Test
    fun rejectsHtmlPagesAndHttpErrors() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html; charset=utf-8").setBody("<html>"))
        server.enqueue(MockResponse().setResponseCode(404))
        for (path in listOf("/page", "/missing")) {
            try {
                Downloader.fetch(server.url(path).toString(), tmp.root) { _, _ -> }
                fail("expected failure for $path")
            } catch (e: java.io.IOException) {
                // expected
            }
        }
        assertTrue(tmp.root.listFiles()!!.isEmpty())
    }
}
