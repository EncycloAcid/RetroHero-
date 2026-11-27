package com.retrohero.app.data

/**
 * Lightweight rcheevos-compatible RetroAchievements condition logic parser & runtime evaluator.
 * Evaluates official RA `MemAddr` triggers against live libretro `SYSTEM_RAM` + `SAVE_RAM` snapshots
 * with support for Core + Alt groups, Delta/Prior memory, bit/BCD/16/32-bit sizes, hit counters,
 * ResetIf, PauseIf, AddSource/SubSource, AddAddress, AndNext/OrNext, AddHits/SubHits, and Measured.
 */
object RaLogicEvaluator {

    enum class Flag { NONE, RESET_IF, PAUSE_IF, ADD_SOURCE, SUB_SOURCE, ADD_HITS, SUB_HITS, AND_NEXT, OR_NEXT, ADD_ADDRESS, MEASURED, MEASURED_IF, RESET_NEXT_IF, TRIGGER }
    enum class OpType { MEM, DELTA, PRIOR, CONST }
    enum class Size { BIT0, BIT1, BIT2, BIT3, BIT4, BIT5, BIT6, BIT7, LOWER4, UPPER4, BIT_COUNT, U8, U16_LE, U16_BE, U24_LE, U32_LE, U32_BE, BCD8 }
    enum class Cmp { EQ, NE, LT, LE, GT, GE, NONE }

    data class Operand(
        val type: OpType,
        val size: Size,
        val value: Long,
        var current: Long = 0L,
        var delta: Long = 0L,
        var prior: Long = 0L,
    )

    data class Condition(
        val flag: Flag,
        val left: Operand,
        val cmp: Cmp,
        val right: Operand?,
        val requiredHits: Int,
        var hits: Int = 0,
    )

    data class TriggerGroup(val conditions: List<Condition>)

    class Trigger(val achievement: RaAchievement, val groups: List<TriggerGroup>) {
        var initialized: Boolean = false
        var triggered: Boolean = achievement.unlocked

        fun resetHits() {
            for (g in groups) for (c in g.conditions) c.hits = 0
        }
    }

    /** Translates a RetroAchievements flat console address into (isSaveRam, byteOffset). */
    fun readConsoleByte(systemId: String, addr: Long, sysRam: ByteArray?, saveRam: ByteArray?): Int {
        val a = addr and 0xFFFFFFFFL
        return when (systemId) {
            "nes", "fds" -> when {
                a < 0x2000L -> byteAt(sysRam, (a and 0x07FFL).toInt())
                a in 0x6000L..0x7FFFL -> byteAt(saveRam, (a - 0x6000L).toInt())
                else -> byteAt(sysRam, a.toInt())
            }
            "snes", "satellaview" -> when {
                a < 0x20000L -> byteAt(sysRam, a.toInt())
                a >= 0x20000L -> byteAt(saveRam, (a - 0x20000L).toInt())
                else -> 0
            }
            "gb", "gbc" -> when {
                // RetroAchievements GB map: 0xC000-0xDFFF = 8KB WRAM, 0xA000-0xBFFF = SRAM, 0xFF80-0xFFFF = HRAM
                a in 0xC000L..0xDFFFL -> byteAt(sysRam, (a - 0xC000L).toInt())
                a in 0xA000L..0xBFFFL -> byteAt(saveRam, (a - 0xA000L).toInt())
                else -> byteAt(sysRam, a.toInt())
            }
            "gba" -> when {
                // RA GBA map: 0x000000-0x007FFF = IWRAM (32KB), 0x008000-0x047FFF = EWRAM (256KB)
                a < 0x48000L -> byteAt(sysRam, a.toInt())
                else -> byteAt(saveRam, (a - 0x48000L).toInt())
            }
            "genesis", "segacd", "32x" -> when {
                a < 0x10000L -> byteAt(sysRam, a.toInt())
                else -> byteAt(saveRam, (a - 0x10000L).toInt())
            }
            "ps1" -> byteAt(sysRam, (a and 0x1FFFFFL).toInt())
            "n64" -> byteAt(sysRam, (a and 0x7FFFFFL).toInt())
            else -> byteAt(sysRam, a.toInt())
        }
    }

    private fun byteAt(buf: ByteArray?, idx: Int): Int =
        if (buf != null && idx in buf.indices) buf[idx].toInt() and 0xFF else 0

    fun readSized(systemId: String, addr: Long, size: Size, sysRam: ByteArray?, saveRam: ByteArray?): Long {
        val b0 = readConsoleByte(systemId, addr, sysRam, saveRam).toLong()
        return when (size) {
            Size.BIT0 -> (b0 shr 0) and 1L
            Size.BIT1 -> (b0 shr 1) and 1L
            Size.BIT2 -> (b0 shr 2) and 1L
            Size.BIT3 -> (b0 shr 3) and 1L
            Size.BIT4 -> (b0 shr 4) and 1L
            Size.BIT5 -> (b0 shr 5) and 1L
            Size.BIT6 -> (b0 shr 6) and 1L
            Size.BIT7 -> (b0 shr 7) and 1L
            Size.LOWER4 -> b0 and 0x0FL
            Size.UPPER4 -> (b0 shr 4) and 0x0FL
            Size.BIT_COUNT -> java.lang.Long.bitCount(b0).toLong()
            Size.U8 -> b0
            Size.BCD8 -> ((b0 shr 4) * 10L) + (b0 and 0x0FL)
            Size.U16_LE -> {
                val b1 = readConsoleByte(systemId, addr + 1, sysRam, saveRam).toLong()
                b0 or (b1 shl 8)
            }
            Size.U16_BE -> {
                val b1 = readConsoleByte(systemId, addr + 1, sysRam, saveRam).toLong()
                (b0 shl 8) or b1
            }
            Size.U24_LE -> {
                val b1 = readConsoleByte(systemId, addr + 1, sysRam, saveRam).toLong()
                val b2 = readConsoleByte(systemId, addr + 2, sysRam, saveRam).toLong()
                b0 or (b1 shl 8) or (b2 shl 16)
            }
            Size.U32_LE -> {
                val b1 = readConsoleByte(systemId, addr + 1, sysRam, saveRam).toLong()
                val b2 = readConsoleByte(systemId, addr + 2, sysRam, saveRam).toLong()
                val b3 = readConsoleByte(systemId, addr + 3, sysRam, saveRam).toLong()
                b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            }
            Size.U32_BE -> {
                val b1 = readConsoleByte(systemId, addr + 1, sysRam, saveRam).toLong()
                val b2 = readConsoleByte(systemId, addr + 2, sysRam, saveRam).toLong()
                val b3 = readConsoleByte(systemId, addr + 3, sysRam, saveRam).toLong()
                (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
            }
        }
    }

    /** Parses a RetroAchievements `MemAddr` string into a runnable [Trigger]. Returns null if unsupported/empty. */
    fun compile(achievement: RaAchievement): Trigger? {
        val raw = achievement.memAddr.trim()
        if (raw.isEmpty()) return null
        val groupStrings = raw.split('S')
        val groups = groupStrings.map { gs ->
            if (gs.isBlank()) TriggerGroup(emptyList())
            else {
                val conds = gs.split('_').mapNotNull { parseCondition(it.trim()) }
                TriggerGroup(conds)
            }
        }
        if (groups.all { it.conditions.isEmpty() }) return null
        return Trigger(achievement, groups)
    }

    private fun parseCondition(token: String): Condition? {
        if (token.isEmpty()) return null
        var rest = token
        var flag = Flag.NONE
        if (rest.length >= 2 && rest[1] == ':') {
            flag = when (rest[0]) {
                'R' -> Flag.RESET_IF
                'P' -> Flag.PAUSE_IF
                'A' -> Flag.ADD_SOURCE
                'B' -> Flag.SUB_SOURCE
                'C' -> Flag.ADD_HITS
                'D' -> Flag.SUB_HITS
                'N' -> Flag.AND_NEXT
                'O' -> Flag.OR_NEXT
                'I' -> Flag.ADD_ADDRESS
                'M' -> Flag.MEASURED
                'Q' -> Flag.MEASURED_IF
                'Z' -> Flag.RESET_NEXT_IF
                'T' -> Flag.TRIGGER
                else -> Flag.NONE
            }
            rest = rest.substring(2)
        }

        // Trailing hit count `(N)` or `.N.`
        var reqHits = 0
        val hitMatch = Regex("""(?:\((\d+)\)|\.(\d+)\.)$""").find(rest)
        if (hitMatch != null) {
            reqHits = (hitMatch.groupValues[1].ifEmpty { hitMatch.groupValues[2] }).toIntOrNull() ?: 0
            rest = rest.substring(0, hitMatch.range.first)
        }

        // Find comparison operator
        val ops = listOf("<=", ">=", "!=", "=", "<", ">")
        var foundOp: String? = null
        var opIdx = -1
        for (op in ops) {
            val idx = rest.indexOf(op)
            if (idx > 0 && (opIdx == -1 || idx < opIdx)) {
                opIdx = idx
                foundOp = op
            }
        }

        return if (foundOp == null) {
            // Unary combining flag (e.g. A:0xH0010, I:0xX0020)
            val left = parseOperand(rest) ?: return null
            Condition(flag, left, Cmp.NONE, null, reqHits)
        } else {
            val leftStr = rest.substring(0, opIdx)
            val rightStr = rest.substring(opIdx + foundOp.length)
            val left = parseOperand(leftStr) ?: return null
            val right = parseOperand(rightStr) ?: return null
            val cmp = when (foundOp) {
                "=" -> Cmp.EQ
                "!=" -> Cmp.NE
                "<" -> Cmp.LT
                "<=" -> Cmp.LE
                ">" -> Cmp.GT
                ">=" -> Cmp.GE
                else -> Cmp.NONE
            }
            Condition(flag, left, cmp, right, reqHits)
        }
    }

    internal fun parseOperand(raw: String): Operand? {
        var s = raw.trim()
        if (s.isEmpty()) return null
        var type = OpType.MEM
        when {
            s.startsWith("d0x", ignoreCase = true) -> { type = OpType.DELTA; s = s.substring(1) }
            s.startsWith("p0x", ignoreCase = true) -> { type = OpType.PRIOR; s = s.substring(1) }
            s.startsWith("0x", ignoreCase = true) -> { type = OpType.MEM }
            s.startsWith("h", ignoreCase = true) -> {
                val v = s.substring(1).toLongOrNull(16) ?: return null
                return Operand(OpType.CONST, Size.U32_LE, v)
            }
            s.startsWith("v", ignoreCase = true) -> {
                val v = s.substring(1).toLongOrNull() ?: return null
                return Operand(OpType.CONST, Size.U32_LE, v)
            }
            else -> {
                val v = s.toLongOrNull() ?: return null
                return Operand(OpType.CONST, Size.U32_LE, v)
            }
        }

        // Now s starts with "0x"
        val after = s.substring(2)
        if (after.isEmpty()) return null
        val (size, hexDigits) = when (after[0]) {
            'H', 'h' -> Size.U8 to after.substring(1)
            'L', 'l' -> Size.LOWER4 to after.substring(1)
            'U', 'u' -> Size.UPPER4 to after.substring(1)
            'M', 'm' -> Size.BIT0 to after.substring(1)
            'N', 'n' -> Size.BIT1 to after.substring(1)
            'O', 'o' -> Size.BIT2 to after.substring(1)
            'P', 'p' -> Size.BIT3 to after.substring(1)
            'Q', 'q' -> Size.BIT4 to after.substring(1)
            'R', 'r' -> Size.BIT5 to after.substring(1)
            'S', 's' -> Size.BIT6 to after.substring(1)
            'T', 't' -> Size.BIT7 to after.substring(1)
            'K', 'k' -> Size.BIT_COUNT to after.substring(1)
            'W', 'w' -> Size.U24_LE to after.substring(1)
            'X', 'x' -> Size.U32_LE to after.substring(1)
            'I', 'i' -> Size.U16_BE to after.substring(1)
            'G', 'g' -> Size.U32_BE to after.substring(1)
            'B', 'b' -> Size.BCD8 to after.substring(1)
            ' ' -> Size.U16_LE to after.substring(1)
            else -> Size.U16_LE to after
        }
        val addr = hexDigits.trim().toLongOrNull(16) ?: return null
        return Operand(type, size, addr)
    }

    /** Steps a single [Trigger] one frame against live RAM. Returns true the moment it unlocks! */
    fun step(trigger: Trigger, systemId: String, sysRam: ByteArray?, saveRam: ByteArray?): Boolean {
        if (trigger.triggered) return false
        if (test(trigger, systemId, sysRam, saveRam)) {
            trigger.triggered = true
            return true
        }
        return false
    }

    /**
     * Evaluates [trigger] for this frame without latching, so it can be true one frame and false
     * the next. Used for Rich Presence display conditions and game-event detection.
     */
    fun test(trigger: Trigger, systemId: String, sysRam: ByteArray?, saveRam: ByteArray?): Boolean {
        if (sysRam == null && saveRam == null) return false

        // First frame initializes delta/prior baselines without firing spurious delta triggers
        if (!trigger.initialized) {
            for (g in trigger.groups) {
                for (c in g.conditions) {
                    initOperand(c.left, systemId, sysRam, saveRam)
                    c.right?.let { initOperand(it, systemId, sysRam, saveRam) }
                }
            }
            trigger.initialized = true
            return false
        }

        var anyReset = false
        val coreResult = evalGroup(trigger.groups[0], systemId, sysRam, saveRam)
        if (coreResult.reset) anyReset = true

        var altPassed = trigger.groups.size <= 1
        for (i in 1 until trigger.groups.size) {
            val r = evalGroup(trigger.groups[i], systemId, sysRam, saveRam)
            if (r.reset) anyReset = true
            if (r.passed && !r.paused) altPassed = true
        }

        if (anyReset) {
            trigger.resetHits()
            return false
        }

        return coreResult.passed && !coreResult.paused && altPassed
    }

    private data class GroupResult(val passed: Boolean, val paused: Boolean, val reset: Boolean)

    private fun evalGroup(group: TriggerGroup, systemId: String, sysRam: ByteArray?, saveRam: ByteArray?): GroupResult {
        if (group.conditions.isEmpty()) return GroupResult(passed = true, paused = false, reset = false)
        var groupPassed = true
        var paused = false
        var reset = false
        var addSubAccum = 0L
        var addrOffset = 0L
        var chainActive = false
        var chainVal = true
        var resetNext = false
        var extraHits = 0

        for (c in group.conditions) {
            val lVal = readOperand(c.left, addrOffset, systemId, sysRam, saveRam)
            val rVal = c.right?.let { readOperand(it, addrOffset, systemId, sysRam, saveRam) } ?: 0L
            addrOffset = 0L

            when (c.flag) {
                Flag.ADD_ADDRESS -> {
                    addrOffset = lVal
                    continue
                }
                Flag.ADD_SOURCE -> {
                    addSubAccum += lVal
                    continue
                }
                Flag.SUB_SOURCE -> {
                    addSubAccum -= lVal
                    continue
                }
                else -> Unit
            }

            val effLeft = lVal + addSubAccum
            addSubAccum = 0L

            var condTrue = compare(effLeft, c.cmp, rVal)
            if (chainActive) {
                condTrue = chainVal && condTrue
                chainActive = false
                chainVal = true
            }

            if (resetNext) {
                if (condTrue) c.hits = 0
                resetNext = false
            }

            if (condTrue) {
                if (c.requiredHits == 0 || c.hits < c.requiredHits) c.hits++
            }

            val effectiveHits = (c.hits + extraHits).coerceAtLeast(0)
            val met = if (c.requiredHits > 0) effectiveHits >= c.requiredHits else condTrue

            when (c.flag) {
                Flag.AND_NEXT -> {
                    chainActive = true
                    chainVal = met
                }
                Flag.OR_NEXT -> {
                    if (met) {
                        chainActive = true
                        chainVal = true
                    } else {
                        chainActive = false
                    }
                }
                Flag.ADD_HITS -> extraHits += c.hits
                Flag.SUB_HITS -> extraHits -= c.hits
                Flag.RESET_NEXT_IF -> resetNext = met
                Flag.PAUSE_IF -> {
                    extraHits = 0
                    if (met) paused = true
                }
                Flag.RESET_IF -> {
                    extraHits = 0
                    if (met) reset = true
                }
                else -> {
                    extraHits = 0
                    if (!met) groupPassed = false
                }
            }
        }
        return GroupResult(passed = groupPassed, paused = paused, reset = reset)
    }

    private fun initOperand(op: Operand, systemId: String, sysRam: ByteArray?, saveRam: ByteArray?) {
        if (op.type == OpType.CONST) {
            op.current = op.value; op.delta = op.value; op.prior = op.value
            return
        }
        val v = readSized(systemId, op.value, op.size, sysRam, saveRam)
        op.current = v; op.delta = v; op.prior = v
    }

    internal fun readOperand(op: Operand, addrOffset: Long, systemId: String, sysRam: ByteArray?, saveRam: ByteArray?): Long {
        if (op.type == OpType.CONST) return op.value
        val now = readSized(systemId, op.value + addrOffset, op.size, sysRam, saveRam)
        val prev = op.current
        if (now != prev) op.prior = prev
        op.delta = prev
        op.current = now
        return when (op.type) {
            OpType.MEM -> now
            OpType.DELTA -> op.delta
            OpType.PRIOR -> op.prior
            OpType.CONST -> op.value
        }
    }

    private fun compare(a: Long, cmp: Cmp, b: Long): Boolean = when (cmp) {
        Cmp.EQ -> a == b
        Cmp.NE -> a != b
        Cmp.LT -> a < b
        Cmp.LE -> a <= b
        Cmp.GT -> a > b
        Cmp.GE -> a >= b
        Cmp.NONE -> a != 0L
    }
}
