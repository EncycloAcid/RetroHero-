package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Smart checkpoints: save states written automatically when a game event says a new level or boss
 * fight started. They rotate through [MAX] files and never touch the player's own slots.
 */
class Checkpoints(private val dir: File, private val baseName: String) {
    data class Entry(val index: Int, val label: String, val atMs: Long, val coreId: String) {
        fun toJson(): JSONObject = JSONObject().put("index", index).put("label", label).put("at", atMs).put("core", coreId)
    }

    private val metaFile get() = File(dir, "$baseName.checkpoints.json")

    fun stateFile(index: Int): File = File(dir.apply { mkdirs() }, "$baseName.checkpoint$index.state")

    /** Newest first; entries whose state file is missing are dropped. */
    fun list(): List<Entry> {
        val arr = runCatching { JSONArray(metaFile.readText()) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Entry(o.optInt("index"), o.optString("label"), o.optLong("at"), o.optString("core"))
        }.filter { stateFile(it.index).exists() }.sortedByDescending { it.atMs }
    }

    fun latest(): Entry? = list().firstOrNull()

    /** The file to write the next checkpoint into: the oldest slot, or an unused one. */
    fun nextIndex(): Int {
        val used = list()
        return (0 until MAX).firstOrNull { idx -> used.none { it.index == idx } }
            ?: used.minBy { it.atMs }.index
    }

    /** Records that [index] now holds a checkpoint. Call after the state was written. */
    fun record(index: Int, label: String, atMs: Long, coreId: String) {
        val entries = list().filter { it.index != index } + Entry(index, label, atMs, coreId)
        dir.mkdirs()
        metaFile.writeText(JSONArray(entries.map { it.toJson() }).toString())
    }

    fun clear() {
        list().forEach { stateFile(it.index).delete() }
        metaFile.delete()
    }

    companion object {
        const val MAX = 3
    }
}
