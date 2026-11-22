package com.retrohero.app.data

/** Ways to order a list of games. */
enum class GameSort(val label: String) {
    Recent("Recently played"),
    Name("Name A–Z"),
    MostPlayed("Most played"),
    System("System");

    companion object {
        fun apply(games: List<Game>, sort: GameSort, stats: Map<String, PlayStat>, recentIds: List<String>): List<Game> = when (sort) {
            Name -> games.sortedBy { it.title.lowercase() }
            MostPlayed -> games.sortedWith(compareByDescending<Game> { stats[it.id]?.seconds ?: 0 }.thenBy { it.title.lowercase() })
            System -> {
                val order = Systems.all.mapIndexed { i, s -> s.id to i }.toMap()
                games.sortedWith(compareBy<Game> { order[it.systemId] ?: 99 }.thenBy { it.title.lowercase() })
            }
            Recent -> {
                // Most recently played first (by last-played time, then the recents list), then A–Z.
                val rank = recentIds.withIndex().associate { (i, id) -> id to i }
                games.sortedWith(
                    compareByDescending<Game> { stats[it.id]?.lastPlayed ?: 0L }
                        .thenBy { rank[it.id] ?: Int.MAX_VALUE }
                        .thenBy { it.title.lowercase() },
                )
            }
        }
    }
}
