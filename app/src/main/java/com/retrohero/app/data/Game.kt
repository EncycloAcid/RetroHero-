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

    /**
     * Artwork to try, best first, from the libretro-thumbnails sets (served from their GitHub
     * mirror; the old thumbnails.libretro.com host is empty): box art, then the title screen,
     * then an in-game snapshot. Each is tried under the exact file name and, if that name
     * carries extra tags like "[!]" or "(Rev 1)", under its plain "(Region)" form too. A name
     * with no region at all also tries the common regions for box art.
     */
    val artUrls: List<String>
        get() {
            val sys = system ?: return emptyList()
            val raw = fileName.substringBeforeLast('.').trim()
            val names = linkedSetOf(raw)
            // "Game (USA) (Rev 1) [!]" -> "Game (USA)": the most common naming in the sets.
            Regex("""^(.*?\([^)]*\))""").find(raw)?.groupValues?.get(1)?.trim()?.let { names += it }
            // "Game [!]" -> "Game".
            raw.replace(Regex("""\s*\[[^]]*]"""), "").trim().takeIf { it.isNotEmpty() }?.let { names += it }
            val guesses = if ('(' !in raw) RegionGuesses.map { "${names.last()} ($it)" } else emptyList()
            val repo = sys.thumbnailDb.replace(' ', '_')
            fun url(set: String, n: String): String {
                val safe = n.replace(Regex("[&*/:`<>?\\\\|\"]"), "_")
                return "https://raw.githubusercontent.com/libretro-thumbnails/${enc(repo)}/HEAD/$set/${enc(safe)}.png"
            }
            return names.map { url("Named_Boxarts", it) } + guesses.map { url("Named_Boxarts", it) } +
                listOf("Named_Titles", "Named_Snaps").flatMap { set -> (names + guesses.take(2)).map { url(set, it) } }
        }

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
