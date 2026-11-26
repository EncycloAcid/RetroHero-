package com.retrohero.app.emulator

import android.content.Context
import androidx.core.content.edit
import com.retrohero.app.data.ScreenFilter
import com.retrohero.app.data.readJson
import com.retrohero.app.data.writeAtomic
import org.json.JSONObject
import java.io.File

/**
 * In-game preferences. Only the game process writes these (the main app's Settings are
 * read-only in here), so they live in their own preferences file.
 */
class GamePrefs(context: Context, sharpDefault: Boolean) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val defaultFilter = if (sharpDefault) ScreenFilter.SHARP else ScreenFilter.SMOOTH

    fun filter(systemId: String): Int = prefs.getInt("filter_$systemId", defaultFilter)
    fun setFilter(systemId: String, filter: Int) = prefs.edit { putInt("filter_$systemId", filter) }

    fun integerScale(systemId: String): Boolean = prefs.getBoolean("integer_$systemId", false)
    fun setIntegerScale(systemId: String, on: Boolean) = prefs.edit { putBoolean("integer_$systemId", on) }

    var rewind: Boolean
        get() = prefs.getBoolean("rewind", true)
        set(v) = prefs.edit { putBoolean("rewind", v) }

    fun lastDisc(gameId: String): Int = prefs.getInt("disc_${gameId.hashCode()}", 0).coerceAtLeast(0)
    fun setLastDisc(gameId: String, index: Int) = prefs.edit { putInt("disc_${gameId.hashCode()}", index.coerceAtLeast(0)) }

    fun stateCore(gameId: String, slot: Int): String? =
        prefs.getString("state_core_${gameId.hashCode()}_$slot", null)?.takeIf { it.isNotBlank() }

    fun setStateCore(gameId: String, slot: Int, coreId: String) =
        prefs.edit { putString("state_core_${gameId.hashCode()}_$slot", coreId) }

    companion object {
        const val FILE = "game_prefs"
    }
}

/** Where one group of touch controls has been moved to, and how big it is. */
data class PadAdjust(val dx: Float = 0f, val dy: Float = 0f, val scale: Float = 1f) {
    val isDefault get() = dx == 0f && dy == 0f && scale == 1f
}

/** Touch-control layouts, per system and orientation, in one small JSON file. */
class PadLayouts(private val file: File) {
    private var root: JSONObject = readJson(file) ?: JSONObject()

    fun get(systemId: String, landscape: Boolean): Map<String, PadAdjust> {
        val o = root.optJSONObject(key(systemId, landscape)) ?: return emptyMap()
        return o.keys().asSequence().associateWith { k ->
            val a = o.getJSONArray(k)
            PadAdjust(a.getDouble(0).toFloat(), a.getDouble(1).toFloat(), a.getDouble(2).toFloat())
        }
    }

    fun set(systemId: String, landscape: Boolean, groups: Map<String, PadAdjust>) {
        val o = JSONObject()
        groups.filterValues { !it.isDefault }.forEach { (k, v) -> o.put(k, org.json.JSONArray(listOf(v.dx, v.dy, v.scale))) }
        if (o.length() == 0) root.remove(key(systemId, landscape)) else root.put(key(systemId, landscape), o)
        writeAtomic(file, root.toString())
    }

    private fun key(systemId: String, landscape: Boolean) = "${systemId}_${if (landscape) "land" else "port"}"
}
