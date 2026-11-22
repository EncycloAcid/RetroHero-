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
    val boxArtUrl: String?
        get() {
            val sys = system ?: return null
            val name = fileName.substringBeforeLast('.').replace(Regex("[&*/:`<>?\\\\|\"]"), "_")
            return "https://thumbnails.libretro.com/${enc(sys.thumbnailDb)}/Named_Boxarts/${enc(name)}.png"
        }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("fileName", fileName).put("systemId", systemId)
        .put("treeUri", treeUri).put("parentDocId", parentDocId).put("size", size)

    companion object {
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
