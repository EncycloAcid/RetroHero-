package com.retrohero.app.emulator

import com.retrohero.app.data.QrCodeMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class RollbackNetplayTest {

    @Test
    fun roomInviteParsesBothQrStringAndBareSixDigitCode() {
        val orig = NetplayRoomInvite(
            roomCode = "482917",
            publicHost = "203.0.113.55",
            publicPort = 51922,
            lanHost = "192.168.1.42",
            lanPort = 47123,
        )
        assertEquals("482-917", orig.formattedCode)
        val encoded = orig.encode()
        val parsed = NetplayRoomInvite.parse(encoded)
        assertEquals(orig, parsed)

        val bare = NetplayRoomInvite.parse("482-917")
        assertNotNull(bare)
        assertEquals("482917", bare!!.roomCode)
    }

    @Test
    fun stunClientDecodesRfc5389XorMappedAddress() {
        val magicCookie = 0x2112A442
        val targetIp = (203 shl 24) or (0 shl 16) or (113 shl 8) or 77
        val targetPort = 54321
        val xPort = targetPort xor (magicCookie ushr 16)
        val xIp = targetIp xor magicCookie

        val buf = ByteBuffer.allocate(32)
        buf.putShort(0x0101) // Binding Success Response
        buf.putShort(12)     // Attribute length = 4 header + 8 value
        buf.putInt(magicCookie)
        buf.put(ByteArray(12) { 0x11 })
        buf.putShort(0x0020) // XOR-MAPPED-ADDRESS
        buf.putShort(8)
        buf.put(0)
        buf.put(1)           // IPv4
        buf.putShort(xPort.toShort())
        buf.putInt(xIp)

        val mapped = StunClient.parseMappedAddress(buf.array())
        assertEquals("203.0.113.77" to 54321, mapped)
    }

    @Test
    fun netplayPacketRoundTripsRedundant8FrameHistory() {
        val history = (100..107).map { f -> NetplayPacket.InputFrame(f, (f * 13) and 0xFFFF) }
        val encoded = NetplayPacket.encodeInputs(history)
        val decoded = NetplayPacket.decodeInputs(encoded)
        assertEquals(history, decoded)
    }

    @Test
    fun qrCodeMatrixGeneratesValidFinderPatternsAndSize() {
        val invite = "RHNP:482917:203.0.113.55:51922:192.168.1.42:47123"
        val matrix = QrCodeMatrix.encode(invite)
        assertTrue(matrix.size in 25..37)
        // Verify top-left, top-right, and bottom-left 3x3 finder cores are solid dark
        for (dy in 2..4) {
            for (dx in 2..4) {
                assertTrue(matrix.get(dx, dy))
                assertTrue(matrix.get(matrix.size - 7 + dx, dy))
                assertTrue(matrix.get(dx, matrix.size - 7 + dy))
            }
        }
    }
}
