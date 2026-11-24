package com.retrohero.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

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

    /** Accent colour id from [Looks.accents], or "look:<id>" for an imported look. */
    var accent: String
        get() = prefs.getString("accent", "deck")!!
        set(v) = prefs.edit { putString("accent", v) }

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(v) = prefs.edit { putBoolean("dark_mode", v) }

    /** Touch-pad skin id from [Looks.skins], or "look:<id>" for an imported look. */
    var padSkin: String
        get() = prefs.getString("pad_skin", "console")!!
        set(v) = prefs.edit { putString("pad_skin", v) }

    /** Selected Delta skin identifier for [systemId], or null to use the built-in touch pad. */
    fun deltaSkinFor(systemId: String): String? =
        prefs.getString("delta_skin_$systemId", null)?.takeIf { it.isNotBlank() }

    fun setDeltaSkinFor(systemId: String, identifier: String?) = prefs.edit {
        if (identifier.isNullOrBlank()) remove("delta_skin_$systemId")
        else putString("delta_skin_$systemId", identifier)
    }

    private val _reducedMotion = MutableStateFlow(prefs.getBoolean("reduced_motion", false))
    /** Fewer and calmer animations: no tilt, no spinning, instant transitions where possible. */
    val reducedMotion: StateFlow<Boolean> = _reducedMotion
    fun setReducedMotion(on: Boolean) {
        prefs.edit { putBoolean("reduced_motion", on) }
        _reducedMotion.value = on
    }

    private val _tiltEffects = MutableStateFlow(prefs.getBoolean("tilt_effects", true))
    /** Game boxes on the Shelf move when you tilt your phone. */
    val tiltEffects: StateFlow<Boolean> = _tiltEffects
    fun setTiltEffects(on: Boolean) {
        prefs.edit { putBoolean("tilt_effects", on) }
        _tiltEffects.value = on
    }

    /** The tab you were on, so the app reopens there. */
    var lastTab: String?
        get() = prefs.getString("last_tab", null)
        set(v) = prefs.edit { putString("last_tab", v) }

    /** Also copy in-game screenshots to Pictures/RetroHero. */
    var screenshotsToGallery: Boolean
        get() = prefs.getBoolean("screenshots_to_gallery", false)
        set(v) = prefs.edit { putBoolean("screenshots_to_gallery", v) }

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

    private val _collections = MutableStateFlow(GameCollection.listFromJson(prefs.getString(KEY_COLLECTIONS, null)))
    /** Your own game lists. */
    val collections: StateFlow<List<GameCollection>> = _collections

    fun setCollections(list: List<GameCollection>) {
        prefs.edit { putString(KEY_COLLECTIONS, GameCollection.listToJson(list)) }
        _collections.value = list
    }

    fun createCollection(name: String, firstGame: String? = null): GameCollection {
        val c = GameCollection.new(name).let { if (firstGame != null) it.copy(gameIds = listOf(firstGame)) else it }
        setCollections(_collections.value + c)
        return c
    }

    fun renameCollection(id: String, name: String) =
        setCollections(_collections.value.map { if (it.id == id) it.copy(name = name.trim()) else it })

    fun deleteCollection(id: String) = setCollections(_collections.value.filter { it.id != id })

    /** Adds or removes [gameId] from collection [id]. */
    fun setInCollection(id: String, gameId: String, inIt: Boolean) = setCollections(
        _collections.value.map { c ->
            when {
                c.id != id -> c
                inIt && gameId !in c.gameIds -> c.copy(gameIds = c.gameIds + gameId)
                !inIt -> c.copy(gameIds = c.gameIds - gameId)
                else -> c
            }
        },
    )

    /** Adds several favourites at once (used by restore). */
    fun addFavorites(ids: Collection<String>) {
        val updated = _favorites.value + ids
        prefs.edit { putStringSet(KEY_FAVORITES, updated) }
        _favorites.value = updated
    }

    /** Simple preferences (not folders, lists or favourites) for backups. */
    fun exportPrefs(): JSONObject {
        val o = JSONObject()
        prefs.all.forEach { (k, v) ->
            if (k in BackupSkip) return@forEach
            when (v) {
                is Boolean, is Int, is Long, is String -> o.put(k, v)
                is Float -> o.put(k, v.toDouble()).also { o.put("$k#f", true) }
            }
        }
        return o
    }

    fun importPrefs(o: JSONObject) {
        prefs.edit {
            o.keys().forEach { k ->
                if (k.endsWith("#f") || k in BackupSkip) return@forEach
                when (val v = o.get(k)) {
                    is Boolean -> putBoolean(k, v)
                    is String -> putString(k, v)
                    is Number -> when {
                        o.optBoolean("$k#f") -> putFloat(k, v.toFloat())
                        v is Long -> putLong(k, v)
                        else -> putInt(k, v.toInt())
                    }
                }
            }
        }
        _dynamicColor.value = prefs.getBoolean("dynamic_color", true)
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
        const val KEY_COLLECTIONS = "collections"
        const val MAX_RECENTS = 20
        /** Not portable between phones (folder grants), or saved separately in a backup. */
        val BackupSkip = setOf(KEY_FOLDERS, KEY_RECENTS, KEY_FAVORITES, KEY_COLLECTIONS, "onboarding_done")
    }
}
