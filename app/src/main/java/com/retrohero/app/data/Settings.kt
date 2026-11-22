package com.retrohero.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * User preferences. Note: the emulator runs in its own process, which reads these once at
 * start-up; only the main process writes them.
 */
class Settings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _romFolders = MutableStateFlow(prefs.getStringSet(KEY_FOLDERS, emptySet())!!.toSet())
    val romFolders: StateFlow<Set<String>> = _romFolders

    private val _recents = MutableStateFlow(loadRecents())
    val recents: StateFlow<List<String>> = _recents

    fun addRomFolder(uri: String) {
        val updated = _romFolders.value + uri
        prefs.edit { putStringSet(KEY_FOLDERS, updated) }
        _romFolders.value = updated
    }

    fun removeRomFolder(uri: String) {
        val updated = _romFolders.value - uri
        prefs.edit { putStringSet(KEY_FOLDERS, updated) }
        _romFolders.value = updated
    }

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", true))
    /** Use Material You wallpaper colours (Android 12+). */
    val dynamicColor: StateFlow<Boolean> = _dynamicColor

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean("dynamic_color", enabled) }
        _dynamicColor.value = enabled
    }

    fun coreFor(system: GameSystem): CoreInfo {
        val id = prefs.getString("core_${system.id}", null)
        return system.cores.firstOrNull { it.id == id } ?: system.cores.first()
    }

    fun setCoreFor(system: GameSystem, core: CoreInfo) = prefs.edit { putString("core_${system.id}", core.id) }

    /** First-run onboarding has been finished or skipped. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) = prefs.edit { putBoolean("onboarding_done", v) }

    var overlayOpacity: Float
        get() = prefs.getFloat("overlay_opacity", 0.7f)
        set(v) = prefs.edit { putFloat("overlay_opacity", v) }

    var sharpPixels: Boolean
        get() = prefs.getBoolean("sharp_pixels", true)
        set(v) = prefs.edit { putBoolean("sharp_pixels", v) }

    var haptics: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(v) = prefs.edit { putBoolean("haptics", v) }

    var autoSave: Boolean
        get() = prefs.getBoolean("auto_save", true)
        set(v) = prefs.edit { putBoolean("auto_save", v) }

    var fastForwardSpeed: Float
        get() = prefs.getFloat("ff_speed", 3f)
        set(v) = prefs.edit { putFloat("ff_speed", v) }

    var stickAsDpad: Boolean
        get() = prefs.getBoolean("stick_as_dpad", true)
        set(v) = prefs.edit { putBoolean("stick_as_dpad", v) }

    private val _favorites = MutableStateFlow(prefs.getStringSet(KEY_FAVORITES, emptySet())!!.toSet())
    /** Pinned games, shown in the home "Speed dial". */
    val favorites: StateFlow<Set<String>> = _favorites

    /** Toggles [gameId] in Favourites; returns true if it is now a favourite. */
    fun toggleFavorite(gameId: String): Boolean {
        val now = gameId !in _favorites.value
        val updated = if (now) _favorites.value + gameId else _favorites.value - gameId
        prefs.edit { putStringSet(KEY_FAVORITES, updated) }
        _favorites.value = updated
        return now
    }

    fun markPlayed(gameId: String) {
        val updated = (listOf(gameId) + _recents.value.filter { it != gameId }).take(MAX_RECENTS)
        prefs.edit { putString(KEY_RECENTS, JSONArray(updated).toString()) }
        _recents.value = updated
    }

    private fun loadRecents(): List<String> {
        val json = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_FOLDERS = "rom_folders"
        const val KEY_RECENTS = "recents"
        const val KEY_FAVORITES = "favorites"
        const val MAX_RECENTS = 20
    }
}
