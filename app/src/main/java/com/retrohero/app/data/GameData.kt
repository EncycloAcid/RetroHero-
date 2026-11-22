package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Writes [text] to [file] atomically (temp file, then rename), creating parent folders. */
internal fun writeAtomic(file: File, text: String) {
    file.parentFile?.mkdirs()
    val tmp = File(file.parentFile, file.name + ".tmp")
    tmp.writeText(text)
    if (!tmp.renameTo(file)) {
        file.writeText(text)
        tmp.delete()
    }
}

internal fun readJson(file: File): JSONObject? =
    if (file.exists()) runCatching { JSONObject(file.readText()) }.getOrNull() else null

/** A cheat code. [code] may hold several codes joined with '+', as libretro expects. */
data class Cheat(val name: String, val code: String, val enabled: Boolean = true)

object Cheats {
    fun load(file: File): List<Cheat> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Cheat(o.getString("name"), o.getString("code"), o.optBoolean("enabled", true))
            }
        }.getOrDefault(emptyList())
    }

    fun save(file: File, cheats: List<Cheat>) {
        val arr = JSONArray()
        cheats.forEach { arr.put(JSONObject().put("name", it.name).put("code", it.code).put("enabled", it.enabled)) }
        writeAtomic(file, arr.toString(1))
    }

    /** Normalises what someone typed: trims, upper-cases, and joins lines/commas with '+'. */
    fun normalise(code: String): String =
        code.split('\n', ',', '+', ';').map { it.trim().uppercase() }.filter { it.isNotEmpty() }.joinToString("+")
}

/** Play time for one game. */
data class PlayStat(val seconds: Long, val sessions: Int, val lastPlayed: Long) {
    companion object {
        val None = PlayStat(0, 0, 0)
    }
}

/** One stretch of play: ended at [end] (epoch ms), lasted [seconds]. */
data class PlaySession(val end: Long, val seconds: Long)

object PlayTime {
    /** How many stretches of play to keep per game. */
    private const val MAX_LOG = 1500

    fun read(file: File): PlayStat {
        val o = readJson(file) ?: return PlayStat.None
        return PlayStat(o.optLong("seconds"), o.optInt("sessions"), o.optLong("lastPlayed"))
    }

    /** Every recorded stretch of play, oldest first. */
    fun log(file: File): List<PlaySession> {
        val arr = readJson(file)?.optJSONArray("log") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val e = arr.getJSONArray(i)
            PlaySession(e.getLong(0), e.getLong(1))
        }
    }

    /** Adds [seconds] of play; [newSession] counts a fresh launch. */
    fun add(file: File, seconds: Long, newSession: Boolean, now: Long = System.currentTimeMillis()) {
        if (seconds <= 0 && !newSession) return
        val old = readJson(file)
        val log = old?.optJSONArray("log") ?: JSONArray()
        if (seconds > 0) log.put(JSONArray(listOf(now, seconds)))
        while (log.length() > MAX_LOG) log.remove(0)
        val o = JSONObject()
            .put("seconds", (old?.optLong("seconds") ?: 0) + seconds.coerceAtLeast(0))
            .put("sessions", (old?.optInt("sessions") ?: 0) + if (newSession) 1 else 0)
            .put("lastPlayed", now)
            .put("log", log)
        writeAtomic(file, o.toString())
    }

    /** "3 h 12 min", "45 min", "under a minute". */
    fun format(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 && m > 0 -> "$h h $m min"
            h > 0 -> "$h h"
            m > 0 -> "$m min"
            else -> "under a minute"
        }
    }
}

/** Screen look per system. */
object ScreenFilter {
    const val SHARP = 0
    const val SMOOTH = 1
    const val CRT = 2
    const val LCD = 3
    val labels = listOf("Sharp pixels", "Smooth", "CRT scanlines", "LCD grid")
    val descriptions = listOf(
        "Crisp, square pixels",
        "Softened by the phone's scaler",
        "Dark lines like an old TV",
        "A fine grid like a handheld screen",
    )
}
