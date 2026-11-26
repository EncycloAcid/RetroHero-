package com.retrohero.app.emulator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * Compact 6-Digit Room Code & QR Invite for 3-Second Mobile Rollback Netplay.
 * Works both on the same Wi-Fi (bare 6-digit code via LAN broadcast discovery) and across 5G / NAT
 * networks (QR/invite string carrying the STUN reflexive UDP endpoint for zero-port-forwarding hole-punching).
 */
data class NetplayRoomInvite(
    val roomCode: String,
    val publicHost: String = "",
    val publicPort: Int = 0,
    val lanHost: String = "",
    val lanPort: Int = 0,
) {
    val formattedCode: String
        get() = if (roomCode.length == 6) "${roomCode.substring(0, 3)}-${roomCode.substring(3)}" else roomCode

    fun encode(): String = buildString {
        append("RHNP:")
        append(roomCode)
        append(':')
        append(publicHost)
        append(':')
        append(publicPort)
        if (lanHost.isNotBlank() && lanPort > 0) {
            append(':')
            append(lanHost)
            append(':')
            append(lanPort)
        }
    }

    companion object {
        private val RNG = SecureRandom()

        fun generateRoomCode(): String = "%06d".format(RNG.nextInt(1_000_000))

        /** Parses either a full QR/invite string (`RHNP:...`) or a bare 6-digit room code (`482-917`). */
        fun parse(raw: String): NetplayRoomInvite? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.startsWith("RHNP:", ignoreCase = true)) {
                val parts = trimmed.split(':')
                if (parts.size < 4) return null
                val code = parts[1].filter { it.isDigit() }
                if (code.length != 6) return null
                return NetplayRoomInvite(
                    roomCode = code,
                    publicHost = parts[2].trim(),
                    publicPort = parts[3].trim().toIntOrNull() ?: 0,
                    lanHost = parts.getOrNull(4)?.trim().orEmpty(),
                    lanPort = parts.getOrNull(5)?.trim()?.toIntOrNull() ?: 0,
                )
            }
            val digits = trimmed.filter { it.isDigit() }
            if (digits.length == 6) {
                return NetplayRoomInvite(roomCode = digits)
            }
            return null
        }
    }
}

/**
 * Pure-Kotlin RFC 5389 STUN Binding Request client for discovering the device's reflexive
 * public UDP IP:port over 5G / Wi-Fi NAT without any manual router port forwarding.
 */
object StunClient {
    private const val STUN_MAGIC_COOKIE = 0x2112A442
    private const val ATTR_MAPPED_ADDRESS = 0x0001
    private const val ATTR_XOR_MAPPED_ADDRESS = 0x0020

    private val SERVERS = listOf(
        "stun.l.google.com" to 19302,
        "stun.cloudflare.com" to 3478,
    )

    fun buildBindingRequest(txId: ByteArray): ByteArray {
        require(txId.size == 12)
        val buf = ByteBuffer.allocate(20)
        buf.putShort(0x0001) // Binding Request
        buf.putShort(0x0000) // Message Length = 0
        buf.putInt(STUN_MAGIC_COOKIE)
        buf.put(txId)
        return buf.array()
    }

    /** Parses an IPv4 endpoint from a STUN Binding Success Response (XOR-MAPPED-ADDRESS or MAPPED-ADDRESS). */
    fun parseMappedAddress(packet: ByteArray, length: Int = packet.size): Pair<String, Int>? {
        if (length < 20) return null
        val buf = ByteBuffer.wrap(packet, 0, length)
        val msgType = buf.short.toInt() and 0xFFFF
        val msgLen = buf.short.toInt() and 0xFFFF
        val cookie = buf.int
        if (msgType != 0x0101 || cookie != STUN_MAGIC_COOKIE) return null
        val end = minOf(length, 20 + msgLen)
        buf.position(20)
        var fallback: Pair<String, Int>? = null
        while (buf.position() + 4 <= end) {
            val attrType = buf.short.toInt() and 0xFFFF
            val attrLen = buf.short.toInt() and 0xFFFF
            val valStart = buf.position()
            if (valStart + attrLen > end) break
            if ((attrType == ATTR_XOR_MAPPED_ADDRESS || attrType == ATTR_MAPPED_ADDRESS) && attrLen >= 8) {
                buf.get() // reserved
                val family = buf.get().toInt() and 0xFF
                if (family == 0x01) { // IPv4
                    var port = buf.short.toInt() and 0xFFFF
                    var ip = buf.int
                    if (attrType == ATTR_XOR_MAPPED_ADDRESS) {
                        port = port xor (STUN_MAGIC_COOKIE ushr 16)
                        ip = ip xor STUN_MAGIC_COOKIE
                    }
                    val ipStr = "${(ip ushr 24) and 0xFF}.${(ip ushr 16) and 0xFF}.${(ip ushr 8) and 0xFF}.${ip and 0xFF}"
                    if (attrType == ATTR_XOR_MAPPED_ADDRESS) return ipStr to port
                    fallback = ipStr to port
                }
            }
            val padded = (attrLen + 3) and 3.inv()
            buf.position(minOf(end, valStart + padded))
        }
        return fallback
    }

    fun discoverReflexiveAddress(socket: DatagramSocket): Pair<String, Int>? {
        val prevTimeout = runCatching { socket.soTimeout }.getOrDefault(0)
        val txId = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val req = buildBindingRequest(txId)
        val recvBuf = ByteArray(512)
        try {
            socket.soTimeout = 900
            for ((host, port) in SERVERS) {
                val res = runCatching {
                    val addr = InetAddress.getByName(host)
                    socket.send(DatagramPacket(req, req.size, addr, port))
                    val pkt = DatagramPacket(recvBuf, recvBuf.size)
                    socket.receive(pkt)
                    parseMappedAddress(pkt.data, pkt.length)
                }.getOrNull()
                if (res != null) return res
            }
        } finally {
            runCatching { socket.soTimeout = prevTimeout }
        }
        return null
    }
}

/** Redundant binary UDP protocol for 3-Second Rollback Netplay (zero stall on packet loss). */
object NetplayPacket {
    const val MAGIC_0: Byte = 'R'.code.toByte()
    const val MAGIC_1: Byte = 'H'.code.toByte()
    const val TYPE_HELLO: Byte = 1
    const val TYPE_INPUT: Byte = 2
    const val TYPE_PING: Byte = 3
    const val TYPE_PONG: Byte = 4
    const val TYPE_DISCONNECT: Byte = 5
    const val TYPE_DISCOVER: Byte = 6
    const val TYPE_DISCOVER_REPLY: Byte = 7

    data class InputFrame(val frame: Int, val buttonsMask: Int)

    fun encodeHello(roomCode: String, localPort: Int): ByteArray {
        val codeBytes = roomCode.take(6).toByteArray(Charsets.US_ASCII)
        val buf = ByteBuffer.allocate(3 + 1 + 6)
        buf.put(MAGIC_0); buf.put(MAGIC_1); buf.put(TYPE_HELLO)
        buf.put(localPort.toByte())
        buf.put(codeBytes)
        return buf.array()
    }

    fun encodeInputs(frames: List<InputFrame>): ByteArray {
        val count = frames.size.coerceAtMost(8)
        val buf = ByteBuffer.allocate(4 + count * 8)
        buf.put(MAGIC_0); buf.put(MAGIC_1); buf.put(TYPE_INPUT)
        buf.put(count.toByte())
        for (i in 0 until count) {
            buf.putInt(frames[i].frame)
            buf.putInt(frames[i].buttonsMask)
        }
        return buf.array()
    }

    fun decodeInputs(data: ByteArray, length: Int = data.size): List<InputFrame> {
        if (length < 4 || data[0] != MAGIC_0 || data[1] != MAGIC_1 || data[2] != TYPE_INPUT) return emptyList()
        val count = (data[3].toInt() and 0xFF).coerceAtMost(8)
        if (length < 4 + count * 8) return emptyList()
        val buf = ByteBuffer.wrap(data, 4, count * 8)
        return List(count) { InputFrame(buf.int, buf.int) }
    }

    fun encodePing(type: Byte, sentAtMs: Long): ByteArray {
        val buf = ByteBuffer.allocate(11)
        buf.put(MAGIC_0); buf.put(MAGIC_1); buf.put(type)
        buf.putLong(sentAtMs)
        return buf.array()
    }

    fun decodePingTime(data: ByteArray, length: Int = data.size): Long? {
        if (length < 11 || data[0] != MAGIC_0 || data[1] != MAGIC_1) return null
        return ByteBuffer.wrap(data, 3, 8).long
    }

    fun encodeDiscover(roomCode: String): ByteArray {
        val out = ByteArrayOutputStream(10)
        out.write(MAGIC_0.toInt()); out.write(MAGIC_1.toInt()); out.write(TYPE_DISCOVER.toInt())
        out.write(roomCode.take(6).toByteArray(Charsets.US_ASCII))
        return out.toByteArray()
    }

    fun encodeDiscoverReply(roomCode: String, gameUdpPort: Int): ByteArray {
        val buf = ByteBuffer.allocate(3 + 6 + 4)
        buf.put(MAGIC_0); buf.put(MAGIC_1); buf.put(TYPE_DISCOVER_REPLY)
        buf.put(roomCode.take(6).toByteArray(Charsets.US_ASCII))
        buf.putInt(gameUdpPort)
        return buf.array()
    }
}

enum class NetplayPhase { IDLE, HOSTING, JOINING, CONNECTED, ERROR }

data class NetplayUiState(
    val phase: NetplayPhase = NetplayPhase.IDLE,
    val localPort: Int = 0, // 0 = P1, 1 = P2
    val inputDelay: Int = 1,
    val invite: NetplayRoomInvite? = null,
    val pingMs: Int = 0,
    val rollbackCount: Int = 0,
    val maxRollbackDepth: Int = 0,
    val frame: Int = 0,
    val statusMessage: String = "",
)

/**
 * Live 3-Second Mobile Rollback Netplay session managing UDP hole-punching, LAN discovery,
 * redundant 8-frame input streaming, and deterministic native C++ rollback telemetry.
 */
class RollbackNetplaySession(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(NetplayUiState())
    val state: StateFlow<NetplayUiState> = _state

    private var socket: DatagramSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private var jobs = mutableListOf<Job>()
    @Volatile private var peerAddress: InetSocketAddress? = null

    fun hostRoom(inputDelay: Int = 1) {
        stop()
        val roomCode = NetplayRoomInvite.generateRoomCode()
        _state.value = NetplayUiState(
            phase = NetplayPhase.HOSTING,
            localPort = 0,
            inputDelay = inputDelay.coerceIn(0, 3),
            invite = NetplayRoomInvite(roomCode = roomCode),
            statusMessage = "Opening UDP socket & STUN punch…",
        )
        jobs += scope.launch(Dispatchers.IO) {
            try {
                val sock = DatagramSocket().apply { soTimeout = 100 }
                socket = sock
                val lanIp = localLanIpv4().orEmpty()
                val stun = StunClient.discoverReflexiveAddress(sock)
                val invite = NetplayRoomInvite(
                    roomCode = roomCode,
                    publicHost = stun?.first ?: lanIp,
                    publicPort = stun?.second ?: sock.localPort,
                    lanHost = lanIp,
                    lanPort = sock.localPort,
                )
                _state.value = _state.value.copy(
                    invite = invite,
                    statusMessage = if (stun != null) "Ready on 5G & Wi-Fi · Waiting for Player 2…"
                    else "Ready on Wi-Fi LAN · Waiting for Player 2…",
                )
                startLanResponder(roomCode, sock.localPort)
                runSessionLoop(sock, localPort = 0, inputDelay = inputDelay, invite = invite)
            } catch (e: Exception) {
                _state.value = NetplayUiState(
                    phase = NetplayPhase.ERROR,
                    statusMessage = e.message ?: "Failed to host Netplay room.",
                )
                stopSocketOnly()
            }
        }
    }

    fun joinRoom(rawCodeOrInvite: String, inputDelay: Int = 1) {
        val parsed = NetplayRoomInvite.parse(rawCodeOrInvite)
        if (parsed == null) {
            _state.value = NetplayUiState(
                phase = NetplayPhase.ERROR,
                statusMessage = "Enter a valid 6-digit room code or QR invite.",
            )
            return
        }
        stop()
        _state.value = NetplayUiState(
            phase = NetplayPhase.JOINING,
            localPort = 1,
            inputDelay = inputDelay.coerceIn(0, 3),
            invite = parsed,
            statusMessage = "Punching NAT & finding room ${parsed.formattedCode}…",
        )
        jobs += scope.launch(Dispatchers.IO) {
            try {
                val sock = DatagramSocket().apply {
                    soTimeout = 100
                    broadcast = true
                }
                socket = sock
                // Send simultaneous LAN discovery + STUN hole-punch packets
                val hello = NetplayPacket.encodeHello(parsed.roomCode, 1)
                val discover = NetplayPacket.encodeDiscover(parsed.roomCode)
                val targets = buildList {
                    if (parsed.lanHost.isNotBlank() && parsed.lanPort > 0) {
                        runCatching { add(InetSocketAddress(InetAddress.getByName(parsed.lanHost), parsed.lanPort)) }
                    }
                    if (parsed.publicHost.isNotBlank() && parsed.publicPort > 0) {
                        runCatching { add(InetSocketAddress(InetAddress.getByName(parsed.publicHost), parsed.publicPort)) }
                    }
                }
                // Kick off periodic punch/discovery until peer replies
                jobs += scope.launch(Dispatchers.IO) {
                    val bcast = runCatching { InetSocketAddress(InetAddress.getByName("255.255.255.255"), LAN_DISCOVERY_PORT) }.getOrNull()
                    repeat(30) {
                        if (!isActive || peerAddress != null) return@launch
                        if (bcast != null) {
                            runCatching { sock.send(DatagramPacket(discover, discover.size, bcast)) }
                        }
                        for (t in targets) {
                            runCatching { sock.send(DatagramPacket(hello, hello.size, t)) }
                        }
                        delay(250)
                    }
                }
                runSessionLoop(sock, localPort = 1, inputDelay = inputDelay, invite = parsed)
            } catch (e: Exception) {
                _state.value = NetplayUiState(
                    phase = NetplayPhase.ERROR,
                    statusMessage = e.message ?: "Could not connect to room.",
                )
                stopSocketOnly()
            }
        }
    }

    fun stop() {
        val sock = socket
        val peer = peerAddress
        if (sock != null && peer != null) {
            runCatching {
                val disc = byteArrayOf(NetplayPacket.MAGIC_0, NetplayPacket.MAGIC_1, NetplayPacket.TYPE_DISCONNECT)
                sock.send(DatagramPacket(disc, disc.size, peer))
            }
        }
        NativeBridge.netplayStop()
        jobs.forEach { it.cancel() }
        jobs.clear()
        peerAddress = null
        stopSocketOnly()
        _state.value = NetplayUiState()
    }

    private fun stopSocketOnly() {
        runCatching { socket?.close() }
        runCatching { discoverySocket?.close() }
        socket = null
        discoverySocket = null
    }

    private fun startLanResponder(roomCode: String, gameUdpPort: Int) {
        jobs += scope.launch(Dispatchers.IO) {
            val dSock = runCatching { DatagramSocket(LAN_DISCOVERY_PORT).apply { soTimeout = 250; broadcast = true } }.getOrNull()
                ?: return@launch
            discoverySocket = dSock
            val buf = ByteArray(64)
            while (isActive && peerAddress == null) {
                val pkt = DatagramPacket(buf, buf.size)
                val ok = runCatching { dSock.receive(pkt); true }.getOrDefault(false)
                if (ok && pkt.length >= 9 &&
                    buf[0] == NetplayPacket.MAGIC_0 && buf[1] == NetplayPacket.MAGIC_1 &&
                    buf[2] == NetplayPacket.TYPE_DISCOVER
                ) {
                    val asked = String(buf, 3, 6, Charsets.US_ASCII)
                    if (asked == roomCode) {
                        val reply = NetplayPacket.encodeDiscoverReply(roomCode, gameUdpPort)
                        runCatching { dSock.send(DatagramPacket(reply, reply.size, pkt.socketAddress)) }
                    }
                }
            }
            runCatching { dSock.close() }
        }
    }

    private suspend fun runSessionLoop(
        sock: DatagramSocket,
        localPort: Int,
        inputDelay: Int,
        invite: NetplayRoomInvite,
    ) {
        val recentLocal = ArrayDeque<NetplayPacket.InputFrame>(8)
        var lastSentFrame = -1
        var lastPingAt = 0L
        var rollbackStarted = false

        // Receive coroutine
        val recvJob = scope.launch(Dispatchers.IO) {
            val buf = ByteArray(256)
            while (isActive) {
                val pkt = DatagramPacket(buf, buf.size)
                val got = runCatching { sock.receive(pkt); true }.getOrDefault(false)
                if (!got || pkt.length < 3 || buf[0] != NetplayPacket.MAGIC_0 || buf[1] != NetplayPacket.MAGIC_1) {
                    continue
                }
                when (buf[2]) {
                    NetplayPacket.TYPE_DISCOVER_REPLY -> {
                        if (pkt.length >= 13) {
                            val code = String(buf, 3, 6, Charsets.US_ASCII)
                            val port = ByteBuffer.wrap(buf, 9, 4).int
                            if (code == invite.roomCode && port in 1..65535) {
                                val target = InetSocketAddress(pkt.address, port)
                                val hello = NetplayPacket.encodeHello(invite.roomCode, localPort)
                                runCatching { sock.send(DatagramPacket(hello, hello.size, target)) }
                            }
                        }
                    }
                    NetplayPacket.TYPE_HELLO -> {
                        val sender = pkt.socketAddress as? InetSocketAddress ?: continue
                        if (peerAddress == null) {
                            peerAddress = sender
                            // Reply HELLO so both peers lock in immediately
                            val reply = NetplayPacket.encodeHello(invite.roomCode, localPort)
                            runCatching { sock.send(DatagramPacket(reply, reply.size, sender)) }
                            if (!rollbackStarted) {
                                val started = NativeBridge.netplayStart(localPort, inputDelay)
                                if (started) {
                                    rollbackStarted = true
                                    _state.value = _state.value.copy(
                                        phase = NetplayPhase.CONNECTED,
                                        statusMessage = "Connected · Rollback active!",
                                    )
                                } else {
                                    _state.value = _state.value.copy(
                                        phase = NetplayPhase.ERROR,
                                        statusMessage = NativeBridge.getLastError().ifEmpty { "Core doesn't support rollback." },
                                    )
                                }
                            }
                        }
                    }
                    NetplayPacket.TYPE_INPUT -> {
                        val frames = NetplayPacket.decodeInputs(buf, pkt.length)
                        for (f in frames) {
                            NativeBridge.netplayPushRemoteInput(f.frame, f.buttonsMask)
                        }
                    }
                    NetplayPacket.TYPE_PING -> {
                        val ts = NetplayPacket.decodePingTime(buf, pkt.length) ?: continue
                        val pong = NetplayPacket.encodePing(NetplayPacket.TYPE_PONG, ts)
                        runCatching { sock.send(DatagramPacket(pong, pong.size, pkt.socketAddress)) }
                    }
                    NetplayPacket.TYPE_PONG -> {
                        val ts = NetplayPacket.decodePingTime(buf, pkt.length) ?: continue
                        val rtt = (System.currentTimeMillis() - ts).toInt().coerceIn(1, 999)
                        _state.value = _state.value.copy(pingMs = rtt)
                    }
                    NetplayPacket.TYPE_DISCONNECT -> {
                        _state.value = NetplayUiState(
                            phase = NetplayPhase.IDLE,
                            statusMessage = "Peer disconnected",
                        )
                        NativeBridge.netplayStop()
                        peerAddress = null
                        return@launch
                    }
                }
            }
        }
        jobs += recvJob

        // 60 Hz deterministic local input transmit + telemetry loop
        while (scope.isActive && !sock.isClosed) {
            val peer = peerAddress
            if (peer != null && rollbackStarted) {
                val tel = NativeBridge.netplayPollTelemetry()
                if (tel.size >= 7 && tel[0] == 1) {
                    val curFrame = tel[1]
                    val localFrame = tel[2]
                    val localMask = tel[3]
                    if (localFrame != lastSentFrame) {
                        lastSentFrame = localFrame
                        if (recentLocal.size >= 8) recentLocal.removeFirst()
                        recentLocal.addLast(NetplayPacket.InputFrame(localFrame, localMask))
                        val payload = NetplayPacket.encodeInputs(recentLocal.toList())
                        runCatching { sock.send(DatagramPacket(payload, payload.size, peer)) }
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastPingAt >= 500L) {
                        lastPingAt = now
                        val ping = NetplayPacket.encodePing(NetplayPacket.TYPE_PING, now)
                        runCatching { sock.send(DatagramPacket(ping, ping.size, peer)) }
                    }
                    _state.value = _state.value.copy(
                        frame = curFrame,
                        rollbackCount = tel[4],
                        maxRollbackDepth = tel[5],
                    )
                }
            }
            delay(8L)
        }
    }

    private fun localLanIpv4(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    companion object {
        const val LAN_DISCOVERY_PORT = 47719
    }
}
