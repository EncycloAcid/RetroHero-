package com.retrohero.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One stretch of play of [game]. */
data class HistoryEntry(val game: Game, val end: Long, val seconds: Long) {
    fun day(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
    fun hour(zone: ZoneId = ZoneId.systemDefault()): Int = Instant.ofEpochMilli(end - seconds * 1000).atZone(zone).hour
}

/** Totals over a slice of history, for the Stats screen and Recap. */
data class PlaySummary(
    val totalSeconds: Long,
    val sessions: Int,
    val games: List<Pair<Game, Long>>,
    val systems: List<Pair<GameSystem, Long>>,
    /** Seconds per day, oldest first, covering every day in the range. */
    val days: List<Pair<LocalDate, Long>>,
    /** Seconds per hour of the day (24 entries). */
    val hours: List<Long>,
    val longest: HistoryEntry?,
    val bestStreak: Int,
    val firstDay: LocalDate?,
) {
    val topGame get() = games.firstOrNull()
    val topSystem get() = systems.firstOrNull()
    val busiestDay get() = days.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val busiestHour get() = hours.indices.maxByOrNull { hours[it] }?.takeIf { hours[it] > 0 }
}

object PlayHistory {
    /** Every logged session across [games], newest first. */
    fun entries(games: List<Game>, paths: AppPaths): List<HistoryEntry> =
        games.flatMap { g ->
            val f = paths.playtimeFile(g)
            if (!f.exists()) emptyList() else PlayTime.log(f).map { HistoryEntry(g, it.end, it.seconds) }
        }.sortedByDescending { it.end }

    /** Summary of [entries] played on or after [from] (null = all time) up to [to]. */
    fun summarise(entries: List<HistoryEntry>, from: LocalDate?, to: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): PlaySummary {
        val inRange = entries.filter { val d = it.day(zone); (from == null || !d.isBefore(from)) && !d.isAfter(to) }
        val start = from ?: inRange.minOfOrNull { it.day(zone) } ?: to
        val perDay = inRange.groupBy { it.day(zone) }.mapValues { (_, v) -> v.sumOf { it.seconds } }
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { it to (perDay[it] ?: 0L) }.toList()
        val hours = LongArray(24).also { h -> inRange.forEach { h[it.hour(zone)] += it.seconds } }.toList()
        var best = 0
        var run = 0
        days.forEach { (_, s) -> if (s > 0) { run++; best = maxOf(best, run) } else run = 0 }
        return PlaySummary(
            totalSeconds = inRange.sumOf { it.seconds },
            sessions = inRange.size,
            games = inRange.groupBy { it.game }.map { (g, v) -> g to v.sumOf { it.seconds } }.sortedByDescending { it.second },
            systems = inRange.mapNotNull { e -> e.game.system?.let { it to e.seconds } }
                .groupBy({ it.first }, { it.second }).map { (s, v) -> s to v.sum() }.sortedByDescending { it.second },
            days = days,
            hours = hours,
            longest = inRange.maxByOrNull { it.seconds },
            bestStreak = best,
            firstDay = inRange.minOfOrNull { it.day(zone) },
        )
    }
}
