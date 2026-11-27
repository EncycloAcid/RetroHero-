package com.retrohero.app.data

import java.util.Locale

/*
 * Game-aware events.
 *
 * RetroAchievements publishes, for thousands of games, a "Rich Presence" script: memory addresses
 * for things like lives, health, stage and boss fights, plus the text the game is showing
 * ("World 1-2 · 3 lives"). RetroHero already downloads it with the achievement set. This file
 * runs that script against live RAM and turns changes into events (died, new level, boss fight)
 * that other features react to: smart checkpoints today, and later highlight clips, splits and
 * maps.
 */

/** Something meaningful that just happened in the game. */
sealed interface GameEvent {
    val atMs: Long
    val label: String

    data class Died(override val atMs: Long, val cause: String) : GameEvent {
        override val label get() = "Died ($cause)"
    }
    data class LevelChanged(override val atMs: Long, val name: String, val from: String, val to: String) : GameEvent {
        override val label get() = "$name: $from → $to"
    }
    data class BossStarted(override val atMs: Long, val scene: String) : GameEvent {
        override val label get() = "Boss fight · $scene"
    }
    data class BossEnded(override val atMs: Long, val scene: String) : GameEvent {
        override val label get() = "Boss fight over"
    }
    data class SceneChanged(override val atMs: Long, val scene: String) : GameEvent {
        override val label get() = scene
    }
    data class AchievementUnlocked(override val atMs: Long, val title: String, val milestone: Boolean) : GameEvent {
        override val label get() = "Achievement: $title"
    }
}

/** A parsed Rich Presence script that renders the current game text and named values from RAM. */
class RichPresence private constructor(
    private val lookups: Map<String, Lookup>,
    private val formats: Map<String, String>,
    private val displays: List<Display>,
) {
    class Lookup(private val exact: Map<Long, String>, private val ranges: List<Pair<LongRange, String>>, private val fallback: String?) {
        fun get(v: Long): String? = exact[v] ?: ranges.firstOrNull { v in it.first }?.second ?: fallback
    }

    /**
     * One `@Name(expr)` in a display line. [label] is what the value means: the macro name, or for
     * generic macros like `@Number` the word next to it ("HP @Number(…)", "@Number(…) lives").
     */
    class Macro(val name: String, val expr: ValueExpr, val label: String)

    /** Literal text and macros, in order. */
    class Display(val condition: RaLogicEvaluator.Trigger?, val parts: List<Any>)

    /** One evaluated macro: its raw number and how it was shown. */
    data class Value(val name: String, val label: String, val raw: Long, val text: String)

    data class Snapshot(val displayIndex: Int, val text: String, val values: List<Value>)

    /** Sum of terms like `0xH075a*10_0xH075b`; `$` separates alternatives and takes the max. */
    class ValueExpr(private val alternatives: List<List<Term>>) {
        class Term(val operand: RaLogicEvaluator.Operand, val multiplier: Double, val negate: Boolean)

        fun eval(systemId: String, sysRam: ByteArray?, saveRam: ByteArray?): Long =
            alternatives.maxOf { terms ->
                terms.sumOf { t ->
                    val v = RaLogicEvaluator.readOperand(t.operand, 0L, systemId, sysRam, saveRam).toDouble() * t.multiplier
                    (if (t.negate) -v else v)
                }.toLong()
            }

        companion object {
            fun parse(raw: String): ValueExpr? {
                val alts = raw.split('$').map { alt ->
                    alt.split('_').map { it.trim() }.filter { it.isNotEmpty() }.map { tok ->
                        var t = tok
                        var negate = false
                        // Newer scripts write value terms with condition flags: A: add, B: subtract, M: measured.
                        if (t.length > 2 && t[1] == ':') {
                            when (t[0].uppercaseChar()) {
                                'A', 'M' -> Unit
                                'B' -> negate = true
                                else -> return null
                            }
                            t = t.substring(2)
                        }
                        // Comparisons make it a hit-count measure, which this simple evaluator doesn't model.
                        if (Regex("[=<>!]").containsMatchIn(t)) return null
                        val mulIdx = t.indexOfFirst { it == '*' || it == '/' }
                        val (opText, mul) = if (mulIdx < 0) t to 1.0 else {
                            val factor = t.substring(mulIdx + 1).toDoubleOrNull() ?: return null
                            if (t[mulIdx] == '/' && factor == 0.0) return null
                            t.substring(0, mulIdx) to (if (t[mulIdx] == '*') factor else 1.0 / factor)
                        }
                        val op = RaLogicEvaluator.parseOperand(opText) ?: return null
                        Term(op, mul, negate)
                    }
                }
                if (alts.any { it.isEmpty() }) return null
                return ValueExpr(alts)
            }
        }
    }

    fun evaluate(systemId: String, sysRam: ByteArray?, saveRam: ByteArray?): Snapshot? {
        if (displays.isEmpty() || (sysRam == null && saveRam == null)) return null
        // Conditional lines are tested every frame so their delta/prior operands stay current.
        var chosen = -1
        for ((i, d) in displays.withIndex()) {
            val cond = d.condition ?: continue
            if (RaLogicEvaluator.test(cond, systemId, sysRam, saveRam) && chosen < 0) chosen = i
        }
        if (chosen < 0) chosen = displays.indexOfFirst { it.condition == null }.takeIf { it >= 0 } ?: return null
        val values = mutableListOf<Value>()
        val text = buildString {
            for (part in displays[chosen].parts) {
                when (part) {
                    is String -> append(part)
                    is Macro -> {
                        val raw = part.expr.eval(systemId, sysRam, saveRam)
                        val shown = render(part.name, raw)
                        values += Value(part.name, part.label, raw, shown)
                        append(shown)
                    }
                }
            }
        }.trim()
        return Snapshot(chosen, text, values)
    }

    private fun render(name: String, raw: Long): String {
        lookups[name]?.let { return it.get(raw) ?: "" }
        return when ((formats[name] ?: BUILTIN_FORMATS[name] ?: "VALUE").uppercase(Locale.US)) {
            "SCORE", "POINTS" -> String.format(Locale.US, "%06d", raw)
            "FRAMES" -> time(raw * 1000 / 60)
            "SECS", "SECS_AS_MINS" -> time(raw * 1000)
            "MINUTES" -> "${raw}m"
            "MILLISECS" -> time(raw * 10)
            "TENS" -> (raw * 10).toString()
            "HUNDREDS" -> (raw * 100).toString()
            "THOUSANDS" -> (raw * 1000).toString()
            "FIXED1" -> String.format(Locale.US, "%.1f", raw / 10.0)
            "FIXED2" -> String.format(Locale.US, "%.2f", raw / 100.0)
            "FIXED3" -> String.format(Locale.US, "%.3f", raw / 1000.0)
            "ASCIICHAR" -> raw.toInt().toChar().takeIf { it in ' '..'~' }?.toString() ?: ""
            else -> raw.toString()
        }
    }

    private fun time(ms: Long): String {
        val s = ms / 1000
        return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
        else String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }

    companion object {
        private val BUILTIN_FORMATS = mapOf(
            "Number" to "VALUE", "Unsigned" to "VALUE", "Score" to "SCORE", "Centiseconds" to "MILLISECS",
            "Seconds" to "SECS", "Minutes" to "MINUTES", "SecondsAsMinutes" to "SECS_AS_MINS",
            "ASCIIChar" to "ASCIICHAR", "Fixed1" to "FIXED1", "Fixed2" to "FIXED2", "Fixed3" to "FIXED3",
            "Float1" to "FIXED1", "Float2" to "FIXED2",
        )
        private val MACRO = Regex("@([A-Za-z0-9_ ]+)\\(([^)]*)\\)")

        /** Returns null when the script has no usable Display section. */
        fun parse(script: String): RichPresence? {
            if (script.isBlank()) return null
            val lines = script.replace("\r", "").lines().map { it.substringBefore("//").trimEnd() }
            val lookups = mutableMapOf<String, Lookup>()
            val formats = mutableMapOf<String, String>()
            val displays = mutableListOf<Display>()
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                when {
                    line.startsWith("Lookup:", ignoreCase = true) -> {
                        val name = line.substringAfter(':').trim()
                        val exact = mutableMapOf<Long, String>()
                        val ranges = mutableListOf<Pair<LongRange, String>>()
                        var fallback: String? = null
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            val entry = lines[i].trim()
                            val eq = entry.indexOf('=')
                            if (eq > 0) {
                                val keys = entry.substring(0, eq).trim()
                                val value = entry.substring(eq + 1)
                                for (k in keys.split(',').map { it.trim() }) {
                                    when {
                                        k == "*" -> fallback = value
                                        k.contains('-') && !k.startsWith("-") -> {
                                            val a = number(k.substringBefore('-'))
                                            val b = number(k.substringAfter('-'))
                                            if (a != null && b != null) ranges += (a..b) to value
                                        }
                                        else -> number(k)?.let { exact[it] = value }
                                    }
                                }
                            }
                            i++
                        }
                        lookups[name] = Lookup(exact, ranges, fallback)
                    }
                    line.startsWith("Format:", ignoreCase = true) -> {
                        val name = line.substringAfter(':').trim()
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            val f = lines[i].trim()
                            if (f.startsWith("FormatType=", ignoreCase = true)) formats[name] = f.substringAfter('=').trim()
                            i++
                        }
                    }
                    line.startsWith("Display:", ignoreCase = true) -> {
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            parseDisplay(lines[i].trim())?.let { displays += it }
                            i++
                        }
                    }
                    else -> i++
                }
            }
            if (displays.isEmpty()) return null
            return RichPresence(lookups, formats, displays)
        }

        private fun parseDisplay(line: String): Display? {
            var cond: RaLogicEvaluator.Trigger? = null
            var body = line
            if (line.startsWith("?")) {
                val end = line.indexOf('?', 1)
                if (end < 0) return null
                val condText = line.substring(1, end)
                cond = RaLogicEvaluator.compile(RaAchievement(0, "", "", 0, "", condText)) ?: return null
                body = line.substring(end + 1)
            }
            val parts = mutableListOf<Any>()
            val matches = MACRO.findAll(body).toList()
            var last = 0
            for ((i, m) in matches.withIndex()) {
                val before = body.substring(last, m.range.first)
                if (before.isNotEmpty()) parts += before
                val expr = ValueExpr.parse(m.groupValues[2])
                if (expr != null) {
                    val name = m.groupValues[1].trim()
                    val after = body.substring(m.range.last + 1, matches.getOrNull(i + 1)?.range?.first ?: body.length)
                    parts += Macro(name, expr, labelFor(name, before, after))
                }
                last = m.range.last + 1
            }
            if (last < body.length) parts += body.substring(last)
            return Display(cond, parts)
        }

        private val WORD_BEFORE = Regex("([A-Za-z][A-Za-z ]{0,15}?)\\s*[:=]?\\s*$")
        private val WORD_AFTER = Regex("^\\s*([A-Za-z][A-Za-z]{0,15})")

        /** Generic macros take their meaning from the text right next to them. */
        private fun labelFor(name: String, before: String, after: String): String {
            if (name !in BUILTIN_FORMATS && !name.equals("Value", ignoreCase = true)) return name
            WORD_BEFORE.find(before)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            WORD_AFTER.find(after)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }?.let { return it }
            return name
        }

        private fun number(s: String): Long? {
            val t = s.trim()
            return if (t.startsWith("0x", ignoreCase = true)) t.substring(2).toLongOrNull(16) else t.toLongOrNull()
        }
    }
}

/**
 * Watches Rich Presence snapshots over time and reports [GameEvent]s. Values are classified by the
 * name the script gives them (Lives, HP, Stage…) and changes must hold steady for a moment before
 * they count, so flicker during screen transitions doesn't produce events.
 */
class GameEventDetector(private val presence: RichPresence, private val systemId: String) {
    enum class Kind { LIVES, HEALTH, LEVEL, OTHER }

    private class Tracked(var value: Long, var text: String, var pending: Long? = null, var pendingText: String = "", var pendingSince: Long = 0L)

    private val tracked = mutableMapOf<String, Tracked>()
    private var lastDisplay = -1
    private var quietUntil = 0L
    private var lastDeathAt = Long.MIN_VALUE / 2
    private var inBoss = false
    private var bossCandidate: Boolean? = null
    private var bossCandidateSince = 0L
    private var scene = ""
    private var sceneCandidate = ""
    private var sceneCandidateSince = 0L

    /** The most recent presence text, e.g. "World 1-2 · 3 lives". */
    var currentText: String = ""
        private set
    var currentValues: List<RichPresence.Value> = emptyList()
        private set

    /** Call after a state load, reset or while rewinding: values jump and must not count as events. */
    fun quiet(nowMs: Long, forMs: Long = 2500L) {
        quietUntil = maxOf(quietUntil, nowMs + forMs)
        tracked.clear()
        lastDisplay = -1
        bossCandidate = null
    }

    fun update(sysRam: ByteArray?, saveRam: ByteArray?, nowMs: Long): List<GameEvent> {
        val snap = presence.evaluate(systemId, sysRam, saveRam) ?: return emptyList()
        currentText = snap.text
        currentValues = snap.values
        val quiet = nowMs < quietUntil
        if (snap.displayIndex != lastDisplay || quiet) {
            // A different display line means a different game mode (title, map, battle): restart baselines.
            lastDisplay = if (quiet) -1 else snap.displayIndex
            tracked.clear()
            snap.values.forEachIndexed { i, v -> tracked[key(i, v)] = Tracked(v.raw, v.text) }
            if (quiet) return emptyList()
        }
        val events = mutableListOf<GameEvent>()

        for ((i, v) in snap.values.withIndex()) {
            val t = tracked.getOrPut(key(i, v)) { Tracked(v.raw, v.text) }
            when (kindOf(v.label)) {
                Kind.LIVES -> {
                    val drop = t.value - v.raw
                    if (drop in 1..3 && nowMs - lastDeathAt > DEATH_COOLDOWN_MS) {
                        lastDeathAt = nowMs
                        events += GameEvent.Died(nowMs, "lost a life")
                    }
                    t.value = v.raw; t.text = v.text
                }
                Kind.HEALTH -> {
                    if (t.value > 0 && v.raw <= 0 && nowMs - lastDeathAt > DEATH_COOLDOWN_MS) {
                        lastDeathAt = nowMs
                        events += GameEvent.Died(nowMs, "${v.label} ran out")
                    }
                    t.value = v.raw; t.text = v.text
                }
                Kind.LEVEL -> {
                    if (v.raw == t.value) {
                        t.pending = null
                    } else if (t.pending != v.raw) {
                        t.pending = v.raw; t.pendingText = v.text; t.pendingSince = nowMs
                    } else if (nowMs - t.pendingSince >= SETTLE_MS) {
                        val from = t.text.ifBlank { t.value.toString() }
                        val to = v.text.ifBlank { v.raw.toString() }
                        t.value = v.raw; t.text = v.text; t.pending = null
                        if (from != to) events += GameEvent.LevelChanged(nowMs, v.label, from, to)
                    }
                }
                Kind.OTHER -> { t.value = v.raw; t.text = v.text }
            }
        }

        val bossNow = BOSS.containsMatchIn(snap.text)
        if (bossNow == inBoss) {
            bossCandidate = null
        } else if (bossCandidate != bossNow) {
            bossCandidate = bossNow; bossCandidateSince = nowMs
        } else if (nowMs - bossCandidateSince >= BOSS_SETTLE_MS) {
            inBoss = bossNow; bossCandidate = null
            events += if (bossNow) GameEvent.BossStarted(nowMs, snap.text) else GameEvent.BossEnded(nowMs, snap.text)
        }

        // Scene text without numbers, so a ticking score or timer isn't a new scene.
        val sceneKey = snap.text.replace(Regex("[0-9:,.]+"), "#")
        if (sceneKey == scene) {
            sceneCandidate = sceneKey
        } else if (sceneCandidate != sceneKey) {
            sceneCandidate = sceneKey; sceneCandidateSince = nowMs
        } else if (nowMs - sceneCandidateSince >= SCENE_SETTLE_MS) {
            scene = sceneKey
            if (snap.text.isNotBlank()) events += GameEvent.SceneChanged(nowMs, snap.text)
        }
        return events
    }

    private fun key(index: Int, v: RichPresence.Value) = "$index:${v.label}"

    companion object {
        const val SETTLE_MS = 1500L
        const val BOSS_SETTLE_MS = 1000L
        const val SCENE_SETTLE_MS = 2000L
        const val DEATH_COOLDOWN_MS = 3000L
        private val BOSS = Regex("\\bboss\\b|\\bvs\\.? ", RegexOption.IGNORE_CASE)
        private val LIVES = Regex("lives|\\blife ?count|\\bstock|\\b1-?ups?\\b|\\btries|\\bcontinues|\\brest\\b", RegexOption.IGNORE_CASE)
        private val HEALTH = Regex("\\bhp\\b|health|\\blife\\b|energy|hearts?\\b|vitality", RegexOption.IGNORE_CASE)
        private val LEVEL = Regex("level|stage|world|area|zone|\\bmap\\b|room|chapter|\\bact\\b|location|dungeon|mission|course|round|floor", RegexOption.IGNORE_CASE)

        fun kindOf(name: String): Kind = when {
            LIVES.containsMatchIn(name) -> Kind.LIVES
            HEALTH.containsMatchIn(name) -> Kind.HEALTH
            LEVEL.containsMatchIn(name) -> Kind.LEVEL
            else -> Kind.OTHER
        }
    }
}
