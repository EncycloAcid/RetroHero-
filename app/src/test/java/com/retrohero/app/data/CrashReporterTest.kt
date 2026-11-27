package com.retrohero.app.data

import androidx.test.core.app.ApplicationProvider
import com.retrohero.app.RetroHeroApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrashReporterTest {
    private val app = ApplicationProvider.getApplicationContext<RetroHeroApp>()

    @Before
    fun clean() = CrashReporter.clear(app)

    @Test
    fun reportHasDeviceHeaderAndStackTrace() {
        val body = CrashReporter.describeThrowable(Thread.currentThread(), IllegalStateException("core exploded"))
        val file = CrashReporter.writeReport(app, "crash", body, timestamp = System.currentTimeMillis() + 1000)
        val text = file.readText()
        assertTrue(text.contains("App: "))
        assertTrue(text.contains("Android: "))
        assertTrue(text.contains("IllegalStateException: core exploded"))
        assertTrue(text.contains("CrashReporterTest"))
        assertEquals(listOf(file), CrashReporter.reports(app))
    }

    @Test
    fun unseenUntilAcknowledgedThenClearRemovesAll() {
        CrashReporter.writeReport(app, "crash", "boom", timestamp = System.currentTimeMillis() + 1000)
        assertEquals(1, CrashReporter.unseenReports(app).size)
        // A report older than the last acknowledgement no longer counts as unseen.
        CrashReporter.clear(app)
        CrashReporter.writeReport(app, "crash", "old", timestamp = System.currentTimeMillis() - 60_000)
        CrashReporter.acknowledge(app)
        assertEquals(0, CrashReporter.unseenReports(app).size)
        assertEquals(1, CrashReporter.reports(app).size)
        CrashReporter.clear(app)
        assertTrue(CrashReporter.reports(app).isEmpty())
    }

    @Test
    fun keepsOnlyTheNewestReports() {
        val now = System.currentTimeMillis()
        repeat(CrashReporter.MAX_REPORTS + 5) { i ->
            CrashReporter.writeReport(app, "crash", "n$i", timestamp = now - (100 - i) * 60_000L)
        }
        val kept = CrashReporter.reports(app)
        assertEquals(CrashReporter.MAX_REPORTS, kept.size)
        assertTrue(kept.first().readText().contains("n${CrashReporter.MAX_REPORTS + 4}"))
    }

    @Test
    fun bundleJoinsReportsAndBreadcrumbIsNotAReport() {
        assertNull(CrashReporter.bundleForSharing(app))
        CrashReporter.setBreadcrumb(app, "Game: Test (test.nes)")
        CrashReporter.writeReport(app, "crash", "first", timestamp = System.currentTimeMillis() - 2000)
        CrashReporter.writeReport(app, "anr", "second", timestamp = System.currentTimeMillis() - 1000)
        assertEquals(2, CrashReporter.reports(app).size)
        val bundle = CrashReporter.bundleForSharing(app)
        assertNotNull(bundle)
        val text = bundle!!.readText()
        assertTrue(text.indexOf("second") < text.indexOf("first"))
        CrashReporter.clearBreadcrumb(app)
    }
}
