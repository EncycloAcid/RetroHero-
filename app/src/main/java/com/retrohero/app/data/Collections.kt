package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A list of games you made yourself, shown like a playlist. */
data class GameCollection(val id: String, val name: String, val gameIds: List<String>) {
    companion object {
        fun new(name: String) = GameCollection(UUID.randomUUID().toString(), name.trim(), emptyList())

        fun listToJson(list: List<GameCollection>): String = JSONArray().apply {
            list.forEach { c -> put(JSONObject().put("id", c.id).put("name", c.name).put("games", JSONArray(c.gameIds))) }
        }.toString()

        fun listFromJson(json: String?): List<GameCollection> {
            if (json.isNullOrEmpty()) return emptyList()
            return runCatching {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    val games = o.optJSONArray("games") ?: JSONArray()
                    GameCollection(o.getString("id"), o.getString("name"), (0 until games.length()).map { games.getString(it) })
                }
            }.getOrDefault(emptyList())
        }
    }
}
