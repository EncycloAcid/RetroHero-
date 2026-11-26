package com.retrohero.app.data

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code matrix generator (Byte mode, Error Correction Level L,
 * Versions 2..5 supporting up to 106 UTF-8 bytes) with zero third-party dependencies.
 * Used to render scannable 3-Second Rollback Netplay room invites natively on Compose Canvas.
 */
object QrCodeMatrix {

    data class Matrix(val size: Int, val modules: BooleanArray) {
        fun get(x: Int, y: Int): Boolean = modules[y * size + x]
    }

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val dataCodewords: Int,
        val ecCodewords: Int,
        val alignmentCenter: Int,
    )

    // ISO/IEC 18004 Level L single-block specs for Versions 2..5
    private val SPECS = listOf(
        VersionSpec(2, 25, 34, 10, 18),
        VersionSpec(3, 29, 55, 15, 22),
        VersionSpec(4, 33, 80, 20, 26),
        VersionSpec(5, 37, 108, 26, 30),
    )

    // Precomputed 15-bit BCH format string for EC Level L (01) + Mask 0 (000) XOR 0x5412
    private const val FORMAT_L_MASK0 = 0x77C4

    fun encode(text: String): Matrix {
        val payload = text.toByteArray(Charsets.UTF_8)
        val spec = SPECS.firstOrNull { payload.size + 2 <= it.dataCodewords }
            ?: SPECS.last()
        val trimmed = if (payload.size + 2 > spec.dataCodewords) {
            payload.copyOf(spec.dataCodewords - 2)
        } else {
            payload
        }

        val dataCw = buildDataCodewords(trimmed, spec.dataCodewords)
        val ecCw = reedSolomon(dataCw, spec.ecCodewords)
        val allCw = ByteArray(dataCw.size + ecCw.size)
        System.arraycopy(dataCw, 0, allCw, 0, dataCw.size)
        System.arraycopy(ecCw, 0, allCw, dataCw.size, ecCw.size)

        val n = spec.size
        val modules = BooleanArray(n * n)
        val isFunction = BooleanArray(n * n)

        fun setFunc(x: Int, y: Int, dark: Boolean) {
            if (x in 0 until n && y in 0 until n) {
                modules[y * n + x] = dark
                isFunction[y * n + x] = true
            }
        }

        // 1. Finder patterns + separators (top-left, top-right, bottom-left)
        fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    val dark = dist != 2 && dist != 4
                    setFunc(cx + dx, cy + dy, dark)
                }
            }
        }
        drawFinder(3, 3)
        drawFinder(n - 4, 3)
        drawFinder(3, n - 4)

        // 2. Alignment pattern
        val ac = spec.alignmentCenter
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunc(ac + dx, ac + dy, dist != 1)
            }
        }

        // 3. Timing patterns
        for (i in 8 until n - 8) {
            setFunc(i, 6, (i and 1) == 0)
            setFunc(6, i, (i and 1) == 0)
        }

        // 4. Reserve format information areas + dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in n - 8 until n) {
            setFunc(8, i, false)
            setFunc(i, 8, false)
        }
        setFunc(8, n - 8, true) // Dark module

        // 5. Place data + EC bits in standard 2-column zigzag with Mask 0 ((x + y) % 2 == 0)
        var bitIdx = 0
        val totalBits = allCw.size * 8
        var upward = true
        var right = n - 1
        while (right >= 1) {
            if (right == 6) right-- // Skip vertical timing column
            for (step in 0 until n) {
                val y = if (upward) n - 1 - step else step
                for (dx in 0..1) {
                    val x = right - dx
                    if (!isFunction[y * n + x]) {
                        val bit = if (bitIdx < totalBits) {
                            ((allCw[bitIdx ushr 3].toInt() ushr (7 - (bitIdx and 7))) and 1) == 1
                        } else {
                            false
                        }
                        bitIdx++
                        val masked = if (((x + y) and 1) == 0) !bit else bit
                        modules[y * n + x] = masked
                    }
                }
            }
            upward = !upward
            right -= 2
        }

        // 6. Write format bits (Level L, Mask 0)
        for (i in 0..14) {
            val dark = ((FORMAT_L_MASK0 ushr i) and 1) == 1
            // Around top-left finder
            when {
                i < 6 -> modules[i * n + 8] = dark
                i == 6 -> modules[7 * n + 8] = dark
                i == 7 -> modules[8 * n + 8] = dark
                i == 8 -> modules[8 * n + 7] = dark
                else -> modules[8 * n + (14 - i)] = dark
            }
            // Top-right & bottom-left copies
            if (i < 8) {
                modules[8 * n + (n - 1 - i)] = dark
            } else {
                modules[(n - 15 + i) * n + 8] = dark
            }
        }

        return Matrix(size = n, modules = modules)
    }

    private fun buildDataCodewords(payload: ByteArray, capacity: Int): ByteArray {
        val bits = ArrayList<Int>(capacity * 8)
        fun pushBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) bits += (value ushr i) and 1
        }
        // Byte mode indicator (0100) + 8-bit length + UTF-8 bytes
        pushBits(0b0100, 4)
        pushBits(payload.size, 8)
        for (b in payload) pushBits(b.toInt() and 0xFF, 8)
        // Terminator (up to 4 zero bits)
        val capBits = capacity * 8
        repeat(minOf(4, capBits - bits.size)) { bits += 0 }
        while (bits.size % 8 != 0) bits += 0

        val out = ByteArray(capacity)
        var idx = 0
        var i = 0
        while (i < bits.size && idx < capacity) {
            var v = 0
            for (b in 0..7) v = (v shl 1) or bits[i + b]
            out[idx++] = v.toByte()
            i += 8
        }
        var padToggle = true
        while (idx < capacity) {
            out[idx++] = (if (padToggle) 0xEC else 0x11).toByte()
            padToggle = !padToggle
        }
        return out
    }

    private fun reedSolomon(data: ByteArray, ecCount: Int): ByteArray {
        var gen = IntArray(1) { 1 }
        var root = 1
        for (i in 0 until ecCount) {
            val next = IntArray(gen.size + 1)
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
            }
            gen = next
            root = gfMul(root, 2)
        }
        val rem = IntArray(ecCount)
        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecCount - 1)
            rem[ecCount - 1] = 0
            for (j in 0 until ecCount) {
                rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
            }
        }
        return ByteArray(ecCount) { rem[it].toByte() }
    }

    private fun gfMul(a: Int, b: Int): Int {
        var x = a
        var y = b
        var res = 0
        repeat(8) {
            if ((y and 1) != 0) res = res xor x
            val hi = (x and 0x80) != 0
            x = (x shl 1) and 0xFF
            if (hi) x = x xor 0x1D
            y = y ushr 1
        }
        return res
    }
}
