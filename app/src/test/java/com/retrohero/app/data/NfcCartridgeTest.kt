package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NfcCartridgeTest {
    private fun game(file: String, sys: String = "nes", id: String = "content://tree/$file") =
        Game(id, file, sys, "content://tree", "primary:Roms", 1L)

    @Test
    fun keyIsStableAcrossLibraryIdsAndCase() {
        val a = game("Thwaite.nes", id = "content://old/1")
        val b = game("thwaite.NES", id = "content://new/9")
        assertEquals(NfcCartridge.keyOf(a), NfcCartridge.keyOf(b))
        assertEquals(12, NfcCartridge.keyOf(a).length)
        assertNotEquals(NfcCartridge.keyOf(a), NfcCartridge.keyOf(game("Thwaite.nes", sys = "fds")))
    }

    @Test
    fun linkRoundTripsAndFindsTheGame() {
        val target = game("Libbet and the Magic Floor.gb", sys = "gb")
        val library = listOf(game("Other.nes"), target)
        val uri = NfcCartridge.uriFor(target)
        assertTrue(uri.startsWith("retrohero://play/"))
        val link = NfcCartridge.parse(uri)!!
        assertEquals(target.title.take(40), link.title)
        assertEquals(target, NfcCartridge.find(library, link))
        assertTrue("fits a 144-byte NTAG213", NfcCartridge.messageFor(target).byteArrayLength <= 137)
    }

    @Test
    fun rejectsOtherLinks() {
        assertNull(NfcCartridge.parse(null))
        assertNull(NfcCartridge.parse("https://example.com/play/abcdef"))
        assertNull(NfcCartridge.parse("retrohero://other/abcdef"))
        assertNull(NfcCartridge.parse("retrohero://play/not-hex!"))
        assertNull(NfcCartridge.find(listOf(game("A.nes")), NfcCartridge.Link("ffffffffffff", null)))
    }
}
