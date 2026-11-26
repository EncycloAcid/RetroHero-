package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Game(
    /** SAF document URI of the file; unique id. */
    val id: String,
    val fileName: String,
    val systemId: String,
    val treeUri: String,
    val parentDocId: String,
    val size: Long,
) {
    /** Downloaded into the app (a plain file) rather than found in a user folder via SAF. */
    val isLocal: Boolean get() = treeUri == LOCAL

    val extension: String get() = fileName.substringAfterLast('.', "").lowercase()

    /** File name without extension, safe to use for save files. */
    val baseName: String get() = fileName.substringBeforeLast('.').replace(Regex("[\\\\/:*?\"<>|]"), "_")

    /** Display title with region / revision tags stripped: "Game (USA) (Rev 1)" -> "Game". */
    val title: String
        get() = fileName.substringBeforeLast('.')
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

    companion object {
        /** Regions to guess, most likely first, when a file name has none. */
        private val RegionGuesses = listOf("USA", "World", "USA, Europe", "Europe", "Japan")

        const val LOCAL = "local"

        private fun enc(s: String) = android.net.Uri.encode(s)

        fun fromJson(o: JSONObject) = Game(
            id = o.getString("id"),
            fileName = o.getString("fileName"),
            systemId = o.getString("systemId"),
            treeUri = o.getString("treeUri"),
            parentDocId = o.getString("parentDocId"),
            size = o.optLong("size"),
        )

        fun listToJson(games: List<Game>): String =
            JSONArray().apply { games.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(json: String): List<Game> {
            val arr = JSONArray(json)
            return (0 until arr.length()).mapNotNull { runCatching { fromJson(arr.getJSONObject(it)) }.getOrNull() }
        }
    }
}
