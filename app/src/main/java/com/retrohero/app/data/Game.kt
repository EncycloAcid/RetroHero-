package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject

/** One physical disc belonging to a grouped multi-disc [Game]. */
data class GameDisc(
    val id: String,
    val fileName: String,
    val parentDocId: String,
    val size: Long,
    val discNumber: Int,
    val label: String = "Disc $discNumber",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("fileName", fileName)
        .put("parentDocId", parentDocId)
        .put("size", size)
        .put("discNumber", discNumber)
        .put("label", label)

    companion object {
        fun fromJson(o: JSONObject) = GameDisc(
            id = o.getString("id"),
            fileName = o.getString("fileName"),
            parentDocId = o.optString("parentDocId"),
            size = o.optLong("size"),
            discNumber = o.optInt("discNumber", 1),
            label = o.optString("label", "Disc ${o.optInt("discNumber", 1)}"),
        )
    }
}

data class Game(
    /** SAF document URI of the file (or Disc 1 for grouped multi-disc sets); unique id. */
    val id: String,
    val fileName: String,
    val systemId: String,
    val treeUri: String,
    val parentDocId: String,
    val size: Long,
    /** Sorted discs when this entry represents an auto-grouped multi-disc game. */
    val discs: List<GameDisc> = emptyList(),
) {
    /** Downloaded into the app (a plain file) rather than found in a user folder via SAF. */
    val isLocal: Boolean get() = treeUri == LOCAL

    /** True when RetroHero automatically grouped 2+ discs into this single game entry. */
    val isMultiDisc: Boolean get() = discs.size > 1

    /** Total number of grouped discs (1 for single-disc / cartridge games). */
    val discCount: Int get() = if (discs.size > 1) discs.size else 1

    val extension: String get() = fileName.substringAfterLast('.', "").lowercase()

    /**
     * File name without extension, safe to use for save files.
     * For auto-grouped multi-disc games, strips the disc suffix ("(Disc 1)", "CD1", etc.)
     * so all discs share one unified battery save (.srm), memory card, and save-state slot family.
     */
    val baseName: String
        get() {
            val stem = fileName.substringBeforeLast('.')
            val canonical = if (isMultiDisc) MultiDisc.stripDiscTag(stem) else stem
            return canonical.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        }

    /** Display title with region / revision / disc tags stripped: "Game (USA) (Disc 1)" -> "Game". */
    val title: String
        get() = MultiDisc.stripDiscTag(fileName.substringBeforeLast('.'))
            .replace(Regex("\\s*[(\\[][^)\\]]*[)\\]]"), "")
            .replace('_', ' ')
            .trim()
            .ifEmpty { fileName }

    val system: GameSystem? get() = Systems.get(systemId)

    /** libretro-thumbnails box art URL (named after the No-Intro/Redump file name). */
    val boxArtUrl: String? get() = artUrls.firstOrNull()

    private fun namedSetUrls(set: String, maxGuesses: Int = RegionGuesses.size): List<String> {
        val sys = system ?: return emptyList()
        val raw = fileName.substringBeforeLast('.').trim()
        val names = linkedSetOf(raw)
        if (isMultiDisc) {
            MultiDisc.stripDiscTag(raw).takeIf { it.isNotEmpty() }?.let { names += it }
        }
        // "Game (USA) (Rev 1) [!]" -> "Game (USA)": the most common naming in the sets.
        Regex("""^(.*?\([^)]*\))""").find(raw)?.groupValues?.get(1)?.trim()?.let { names += it }
        // "Game [!]" -> "Game".
        raw.replace(Regex("""\s*\[[^]]*]"""), "").trim().takeIf { it.isNotEmpty() }?.let { names += it }
        val guesses = if ('(' !in raw) RegionGuesses.take(maxGuesses).map { "${names.last()} ($it)" } else emptyList()
        val repo = sys.thumbnailDb.replace(' ', '_')
        fun url(n: String): String {
            val safe = n.replace(Regex("[&*/:`<>?\\\\|\"]"), "_")
            return "https://raw.githubusercontent.com/libretro-thumbnails/${enc(repo)}/HEAD/$set/${enc(safe)}.png"
        }
        return (names + guesses).map(::url)
    }

    /** Official retail box art URLs from libretro-thumbnails. */
    val boxArtUrls: List<String> get() = namedSetUrls("Named_Boxarts")

    /** Official title screen URLs from libretro-thumbnails. */
    val titleScreenUrls: List<String> get() = namedSetUrls("Named_Titles", maxGuesses = 2)

    /** Official in-game gameplay screenshot URLs from libretro-thumbnails. */
    val gameplaySnapUrls: List<String> get() = namedSetUrls("Named_Snaps", maxGuesses = 2)

    /**
     * Artwork to try, best first, from the libretro-thumbnails sets (served from their GitHub
     * mirror; the old thumbnails.libretro.com host is empty): box art, then the title screen,
     * then an in-game snapshot.
     */
    val artUrls: List<String>
        get() = boxArtUrls + titleScreenUrls + gameplaySnapUrls

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("fileName", fileName).put("systemId", systemId)
        .put("treeUri", treeUri).put("parentDocId", parentDocId).put("size", size)
        .apply {
            if (discs.isNotEmpty()) {
                put("discs", JSONArray().apply { discs.forEach { put(it.toJson()) } })
            }
        }

    companion object {
        /** Regions to guess, most likely first, when a file name has none. */
        private val RegionGuesses = listOf("USA", "World", "USA, Europe", "Europe", "Japan")

        const val LOCAL = "local"

        private fun enc(s: String) = android.net.Uri.encode(s)

        fun fromJson(o: JSONObject): Game {
            val discsArr = o.optJSONArray("discs")
            val parsedDiscs = if (discsArr != null) {
                (0 until discsArr.length()).mapNotNull { runCatching { GameDisc.fromJson(discsArr.getJSONObject(it)) }.getOrNull() }
            } else emptyList()
            return Game(
                id = o.getString("id"),
                fileName = o.getString("fileName"),
                systemId = o.getString("systemId"),
                treeUri = o.getString("treeUri"),
                parentDocId = o.getString("parentDocId"),
                size = o.optLong("size"),
                discs = parsedDiscs,
            )
        }

        fun listToJson(games: List<Game>): String =
            JSONArray().apply { games.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(json: String): List<Game> {
            val arr = JSONArray(json)
            return (0 until arr.length()).mapNotNull { runCatching { fromJson(arr.getJSONObject(it)) }.getOrNull() }
        }
    }
}
