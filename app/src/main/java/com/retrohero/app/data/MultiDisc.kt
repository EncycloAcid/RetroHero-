package com.retrohero.app.data

/**
 * Automatic Zero-Config Multi-Disc detection, grouping, and canonical naming.
 *
 * Collapses multi-disc sets like `(Disc 1)`, `(Disc 2)`, `[CD1]`, `(Disk 1 of 3)`,
 * `- Disc 1`, or `(Side A)` / `(Side B)` into a single unified [Game] entry with one
 * shared battery save (`.srm`) and an auto-generated virtual `.m3u` playlist.
 */
object MultiDisc {

    data class DiscParseResult(
        /** Canonical grouping key (same across all discs of the same release/region). */
        val groupKey: String,
        /** Clean canonical base filename with the disc tag removed (used for unified .srm / .m3u). */
        val canonicalBaseName: String,
        /** 1-based disc/side number for ordering. */
        val discNumber: Int,
        /** Human-readable disc label, e.g. "Disc 1", "Side A". */
        val label: String,
    )

    // Matches bracketed/parenthesized disc tags: (Disc 1), (Disc 2 of 3), [CD1], (Disk 2), (Side A)
    private val BRACKETED_DISC = Regex(
        """\s*[\(\[]\s*(?:(disc|disk|cd)\s*0?(\d{1,2})(?:\s*(?:of|/)\s*\d{1,2})?|(side)\s*([a-d1-4]))\s*[\)\]]""",
        RegexOption.IGNORE_CASE,
    )

    // Matches bare trailing disc suffixes: " - Disc 1", " Disc 2", "_CD1", ".CD2", " Disk 1 of 2"
    private val TRAILING_DISC = Regex(
        """(?:\s*[-_.\s]\s*)(?:(disc|disk|cd)\s*[-_.]?\s*0?(\d{1,2})(?:\s*(?:of|/)\s*\d{1,2})?|(side)\s*[-_.]?\s*([a-d1-4]))\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val NON_ALNUM = Regex("""[^a-z0-9]+""")
    private val MULTI_SPACE = Regex("""\s+""")

    /** Parses disc metadata from a ROM filename, or returns null if this is not a multi-disc file. */
    fun parseDiscInfo(fileName: String): DiscParseResult? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        // Never treat an existing .m3u playlist itself as one disc of another set
        if (ext == "m3u") return null

        val stem = fileName.substringBeforeLast('.')
        val match = BRACKETED_DISC.find(stem) ?: TRAILING_DISC.find(stem) ?: return null

        val discKind = match.groupValues[1]
        val discNumStr = match.groupValues[2]
        val sideKind = match.groupValues[3]
        val sideVal = match.groupValues[4]

        val (discNumber, label) = if (discKind.isNotEmpty() && discNumStr.isNotEmpty()) {
            val n = discNumStr.toIntOrNull() ?: return null
            if (n !in 1..20) return null
            n to "Disc $n"
        } else if (sideKind.isNotEmpty() && sideVal.isNotEmpty()) {
            val upper = sideVal.uppercase()
            val n = when (upper) {
                "A", "1" -> 1
                "B", "2" -> 2
                "C", "3" -> 3
                "D", "4" -> 4
                else -> return null
            }
            val displaySide = if (upper[0] in 'A'..'D') upper else ('A' + (n - 1)).toString()
            n to "Side $displaySide"
        } else {
            return null
        }

        val stripped = (stem.removeRange(match.range))
            .replace(MULTI_SPACE, " ")
            .trim(' ', '-', '_', '.')
            .ifEmpty { return null }

        val groupKey = stripped.lowercase().replace(NON_ALNUM, " ").trim()
        if (groupKey.isEmpty()) return null

        val safeCanonical = stripped.replace(Regex("""[\\/:*?"<>|]"""), "_")
        return DiscParseResult(
            groupKey = groupKey,
            canonicalBaseName = safeCanonical,
            discNumber = discNumber,
            label = label,
        )
    }

    /** Strips any disc tag from a filename stem so all discs map to one canonical save name. */
    fun stripDiscTag(stem: String): String {
        val m = BRACKETED_DISC.find(stem) ?: TRAILING_DISC.find(stem) ?: return stem
        return stem.removeRange(m.range)
            .replace(MULTI_SPACE, " ")
            .trim(' ', '-', '_', '.')
            .ifEmpty { stem }
    }

    /**
     * Collapses multi-disc games within the same system, storage tree, and folder into a single
     * [Game] entry holding all sorted [GameDisc]s. Single-disc games and already-grouped entries
     * pass through untouched.
     */
    fun collapseMultiDiscGames(rawGames: List<Game>): List<Game> {
        if (rawGames.size < 2) return rawGames

        data class GroupId(
            val systemId: String,
            val treeUri: String,
            val parentDocId: String,
            val groupKey: String,
        )

        val buckets = LinkedHashMap<GroupId, MutableList<Pair<Game, DiscParseResult>>>()
        val standalone = ArrayList<Game>(rawGames.size)

        for (g in rawGames) {
            if (g.discs.size > 1) {
                standalone += g
                continue
            }
            val parsed = parseDiscInfo(g.fileName)
            if (parsed == null) {
                standalone += g
            } else {
                val key = GroupId(g.systemId, g.treeUri, g.parentDocId, parsed.groupKey)
                buckets.getOrPut(key) { mutableListOf() } += g to parsed
            }
        }

        val result = ArrayList<Game>(standalone.size + buckets.size)
        result.addAll(standalone)

        for ((_, entries) in buckets) {
            // Only collapse when 2+ distinct discs exist for the same game
            val distinctSorted = entries
                .distinctBy { it.first.id }
                .sortedWith(compareBy<Pair<Game, DiscParseResult>> { it.second.discNumber }.thenBy { it.first.fileName.lowercase() })

            if (distinctSorted.size < 2) {
                result += distinctSorted.first().first
                continue
            }

            val firstGame = distinctSorted.first().first
            val discs = distinctSorted.map { (g, info) ->
                GameDisc(
                    id = g.id,
                    fileName = g.fileName,
                    parentDocId = g.parentDocId,
                    size = g.size,
                    discNumber = info.discNumber,
                    label = info.label,
                )
            }
            val combinedSize = discs.sumOf { it.size }
            result += firstGame.copy(
                size = combinedSize,
                discs = discs,
            )
        }

        return result
    }
}
