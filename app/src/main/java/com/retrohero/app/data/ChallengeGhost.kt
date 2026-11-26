package com.retrohero.app.data

import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Supported win conditions for a Playable Challenge Snapshot (`.rhclip` v2). */
enum class ChallengeWinType(val id: String, val label: String) {
    NONE("none", "Free play snapshot"),
    TIME_TRIAL("time_trial", "Beat ghost time"),
    SURVIVE("survive", "Survive timer"),
    RAM_GOAL("ram_goal", "RAM memory goal");

    companion object {
        fun fromId(id: String?): ChallengeWinType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
    }
}

enum class ChallengeRamOp(val id: String, val symbol: String) {
    EQ("eq", "=="),
    NEQ("neq", "!="),
    LTE("lte", "<="),
    GTE("gte", ">="),
    LT("lt", "<"),
    GT("gt", ">");

    fun test(actual: Int, target: Int): Boolean = when (this) {
        EQ -> actual == target
        NEQ -> actual != target
        LTE -> actual <= target
        GTE -> actual >= target
        LT -> actual < target
        GT -> actual > target
    }

    companion object {
        fun fromId(id: String?): ChallengeRamOp =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EQ
    }
}

/**
 * Optional deterministic win condition attached to a `.rhclip` Playable Challenge Snapshot.
 * Supports time trials against a recorded input ghost, survival countdowns, or RetroAchievements-style
 * live System RAM memory watches (e.g. Boss HP == 0 or Stage Clear flag == 1).
 */
data class ChallengeWinCondition(
    val type: ChallengeWinType = ChallengeWinType.NONE,
    val targetSeconds: Int = 0,
    val ramOffset: Int = 0,
    val ramSize: Int = 1, // 1 = 8-bit, 2 = 16-bit LE
    val op: ChallengeRamOp = ChallengeRamOp.EQ,
    val targetValue: Int = 0,
    val label: String = "",
) {
    fun evaluateRam(systemRam: ByteArray?): Boolean {
        if (type != ChallengeWinType.RAM_GOAL || systemRam == null) return false
        if (ramOffset < 0 || ramOffset + ramSize > systemRam.size) return false
        val actual = when (ramSize) {
            2 -> (systemRam[ramOffset].toInt() and 0xFF) or
                ((systemRam[ramOffset + 1].toInt() and 0xFF) shl 8)
            else -> systemRam[ramOffset].toInt() and 0xFF
        }
        return op.test(actual, targetValue)
    }

    fun summary(): String = when (type) {
        ChallengeWinType.NONE -> ""
        ChallengeWinType.TIME_TRIAL -> label.ifBlank { "Beat the creator's ghost run" }
        ChallengeWinType.SURVIVE -> label.ifBlank { "Survive ${targetSeconds}s without resetting" }
        ChallengeWinType.RAM_GOAL -> label.ifBlank {
            "RAM[0x%X] %s %d".format(ramOffset, op.symbol, targetValue)
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("type", type.id)
        .put("targetSeconds", targetSeconds)
        .put("ramOffset", ramOffset)
        .put("ramSize", ramSize)
        .put("op", op.id)
        .put("targetValue", targetValue)
        .put("label", label)

    companion object {
        fun fromJson(o: JSONObject?): ChallengeWinCondition? {
            if (o == null) return null
            val type = ChallengeWinType.fromId(o.optString("type"))
            if (type == ChallengeWinType.NONE) return null
            return ChallengeWinCondition(
                type = type,
                targetSeconds = o.optInt("targetSeconds", 0).coerceIn(0, 600),
                ramOffset = o.optInt("ramOffset", 0).coerceAtLeast(0),
                ramSize = o.optInt("ramSize", 1).coerceIn(1, 2),
                op = ChallengeRamOp.fromId(o.optString("op")),
                targetValue = o.optInt("targetValue", 0),
                label = o.optString("label").trim().take(80),
            )
        }
    }
}

/** One frame of recorded Player 1 controller input for Challenge Ghost playback. */
data class GhostFrame(
    val buttons: Int,
    val lx: Byte = 0,
    val ly: Byte = 0,
)

/**
 * Compact RLE binary codec for deterministic frame-accurate input ghosts inside `.rhclip` capsules.
 * Typical 60-second gameplay (3,600 frames) compresses to ~1–3 KB.
 */
object ChallengeGhost {
    private val MAGIC = byteArrayOf('R'.code.toByte(), 'H'.code.toByte(), 'G'.code.toByte(), '1'.code.toByte())
    const val MAX_RECORD_FRAMES = 3600 // 60 seconds @ 60fps

    fun encode(frames: List<GhostFrame>): ByteArray {
        val out = ByteArrayOutputStream(maxOf(64, frames.size / 2))
        out.write(MAGIC)
        writeU32LE(out, frames.size)
        var i = 0
        while (i < frames.size) {
            val cur = frames[i]
            var run = 1
            while (i + run < frames.size && run < 65535 && frames[i + run] == cur) {
                run++
            }
            // RLE chunk: u16 runLen, u16 buttons, i8 lx, i8 ly
            out.write(run and 0xFF)
            out.write((run ushr 8) and 0xFF)
            out.write(cur.buttons and 0xFF)
            out.write((cur.buttons ushr 8) and 0xFF)
            out.write(cur.lx.toInt() and 0xFF)
            out.write(cur.ly.toInt() and 0xFF)
            i += run
        }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): List<GhostFrame> {
        if (bytes.size < 8) return emptyList()
        for (k in 0..3) {
            if (bytes[k] != MAGIC[k]) return emptyList()
        }
        val total = readU32LE(bytes, 4).coerceIn(0, 36000)
        val out = ArrayList<GhostFrame>(total)
        var pos = 8
        while (pos + 6 <= bytes.size && out.size < total) {
            val run = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
            val buttons = (bytes[pos + 2].toInt() and 0xFF) or ((bytes[pos + 3].toInt() and 0xFF) shl 8)
            val lx = bytes[pos + 4]
            val ly = bytes[pos + 5]
            pos += 6
            val frame = GhostFrame(buttons, lx, ly)
            val count = minOf(run, total - out.size)
            repeat(count) { out += frame }
        }
        return out
    }

    /** Formats active RetroPad bitmask into concise readable button labels for the Ghost HUD. */
    fun activeLabels(mask: Int): List<String> {
        if (mask == 0) return emptyList()
        val labels = ArrayList<String>(6)
        if ((mask and (1 shl RetroPad.UP)) != 0) labels += "▲"
        if ((mask and (1 shl RetroPad.DOWN)) != 0) labels += "▼"
        if ((mask and (1 shl RetroPad.LEFT)) != 0) labels += "◀"
        if ((mask and (1 shl RetroPad.RIGHT)) != 0) labels += "▶"
        if ((mask and (1 shl RetroPad.B)) != 0) labels += "B"
        if ((mask and (1 shl RetroPad.A)) != 0) labels += "A"
        if ((mask and (1 shl RetroPad.Y)) != 0) labels += "Y"
        if ((mask and (1 shl RetroPad.X)) != 0) labels += "X"
        if ((mask and (1 shl RetroPad.L)) != 0) labels += "L"
        if ((mask and (1 shl RetroPad.R)) != 0) labels += "R"
        return labels
    }

    private fun writeU32LE(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v ushr 8) and 0xFF)
        out.write((v ushr 16) and 0xFF)
        out.write((v ushr 24) and 0xFF)
    }

    private fun readU32LE(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)
}
