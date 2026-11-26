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

    fun graphicsConfig(systemId: String, is3D: Boolean): com.retrohero.app.data.GraphicsConfig {
        val defaultUpscale = if (is3D) 2 else 3
        return com.retrohero.app.data.GraphicsConfig(
            upscale = prefs.getInt("gfx_upscale_$systemId", defaultUpscale).coerceIn(1, 4),
            aspectRatio = com.retrohero.app.data.AspectRatioMode.fromId(prefs.getString("gfx_aspect_$systemId", null)),
            textureFilter = com.retrohero.app.data.TextureFilterMode.fromId(prefs.getString("gfx_tex_$systemId", null)),
            widescreenHack = prefs.getBoolean("gfx_wide_$systemId", false),
            pgxpEnabled = prefs.getBoolean("gfx_pgxp_$systemId", true),
            cropOverscan = prefs.getBoolean("gfx_overscan_$systemId", false),
        )
    }

    fun setGraphicsConfig(systemId: String, cfg: com.retrohero.app.data.GraphicsConfig) = prefs.edit {
        putInt("gfx_upscale_$systemId", cfg.upscale.coerceIn(1, 4))
        putString("gfx_aspect_$systemId", cfg.aspectRatio.id)
        putString("gfx_tex_$systemId", cfg.textureFilter.id)
        putBoolean("gfx_wide_$systemId", cfg.widescreenHack)
        putBoolean("gfx_pgxp_$systemId", cfg.pgxpEnabled)
        putBoolean("gfx_overscan_$systemId", cfg.cropOverscan)
    }

    var rewind: Boolean
        get() = prefs.getBoolean("rewind", true)
        set(v) = prefs.edit { putBoolean("rewind", v) }

    var vsyncLock: Boolean
        get() = prefs.getBoolean("vsync_lock", true)
        set(v) = prefs.edit { putBoolean("vsync_lock", v) }

    var bfi120Hz: Boolean
        get() = prefs.getBoolean("bfi_120hz", false)
        set(v) = prefs.edit { putBoolean("bfi_120hz", v) }

    var ambilightMode: AmbilightMode
        get() = AmbilightMode.fromId(prefs.getString("ambilight_mode", AmbilightMode.VIVID.id))
        set(v) = prefs.edit {
            putString("ambilight_mode", v.id)
            if (v.isEnabled) putString("ambilight_last_active", v.id)
        }

    val lastActiveAmbilightMode: AmbilightMode
        get() = AmbilightMode.fromId(prefs.getString("ambilight_last_active", AmbilightMode.VIVID.id))
            .takeIf { it.isEnabled } ?: AmbilightMode.VIVID

    var thumbChordZones: Boolean
        get() = prefs.getBoolean("smart_thumb_chord", true)
        set(v) = prefs.edit { putBoolean("smart_thumb_chord", v) }

    var shoulderLatch: Boolean
        get() = prefs.getBoolean("smart_shoulder_latch", true)
        set(v) = prefs.edit { putBoolean("smart_shoulder_latch", v) }

    var comboMacroEnabled: Boolean
        get() = prefs.getBoolean("smart_combo_macro", true)
        set(v) = prefs.edit { putBoolean("smart_combo_macro", v) }

    fun macroIndex(systemId: String): Int = prefs.getInt("smart_macro_$systemId", 0).coerceAtLeast(0)
    fun setMacroIndex(systemId: String, idx: Int) = prefs.edit { putInt("smart_macro_$systemId", idx.coerceAtLeast(0)) }

    var gyroSteering: Boolean
        get() = prefs.getBoolean("smart_gyro_steer", false)
        set(v) = prefs.edit { putBoolean("smart_gyro_steer", v) }

    var portraitTouchMode: Int
        get() = prefs.getInt("smart_portrait_mode", SmartTouch.PORTRAIT_AUTO)
        set(v) = prefs.edit { putInt("smart_portrait_mode", v) }

    var leftHandedPortrait: Boolean
        get() = prefs.getBoolean("smart_left_handed", false)
        set(v) = prefs.edit { putBoolean("smart_left_handed", v) }

    fun visualMode(gameId: String): String? =
        prefs.getString("visual_mode_${gameId.hashCode()}", null)?.takeIf { it.isNotBlank() }

    fun setVisualMode(gameId: String, modeId: String) =
        prefs.edit { putString("visual_mode_${gameId.hashCode()}", modeId) }

    fun colorProfileOverride(gameId: String): Int? {
        val key = "color_prof_${gameId.hashCode()}"
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    fun setColorProfileOverride(gameId: String, profile: Int?) = prefs.edit {
        val key = "color_prof_${gameId.hashCode()}"
        if (profile == null) remove(key) else putInt(key, profile)
    }

    fun hasCustomFilter(systemId: String): Boolean = prefs.contains("filter_$systemId")

    fun lastDisc(gameId: String): Int = prefs.getInt("disc_${gameId.hashCode()}", 0).coerceAtLeast(0)
    fun setLastDisc(gameId: String, index: Int) = prefs.edit { putInt("disc_${gameId.hashCode()}", index.coerceAtLeast(0)) }

    fun stateCore(gameId: String, slot: Int): String? =
        prefs.getString("state_core_${gameId.hashCode()}_$slot", null)?.takeIf { it.isNotBlank() }

    fun setStateCore(gameId: String, slot: Int, coreId: String) =
        prefs.edit { putString("state_core_${gameId.hashCode()}_$slot", coreId) }

    /** Nintendo DS screen layout remembered per game + orientation, falling back to the last DS choice. */
    fun dsLayout(gameId: String, landscape: Boolean): DsScreenLayout {
        val o = if (landscape) "land" else "port"
        return DsScreenLayout.fromId(prefs.getString("ds_layout_${gameId.hashCode()}_$o", null))
            ?: DsScreenLayout.fromId(prefs.getString("ds_layout_last_$o", null))
            ?: DsDualScreen.defaultLayout(landscape)
    }

    fun setDsLayout(gameId: String, landscape: Boolean, layout: DsScreenLayout) = prefs.edit {
        val o = if (landscape) "land" else "port"
        putString("ds_layout_${gameId.hashCode()}_$o", layout.id)
        putString("ds_layout_last_$o", layout.id)
    }

    fun dsSwapped(gameId: String): Boolean = prefs.getBoolean("ds_swap_${gameId.hashCode()}", false)
    fun setDsSwapped(gameId: String, swapped: Boolean) = prefs.edit { putBoolean("ds_swap_${gameId.hashCode()}", swapped) }

    fun pinnedNote(gameId: String): String? =
        prefs.getString("guide_pin_${gameId.hashCode()}", null)?.takeIf { it.isNotBlank() }

    fun setPinnedNote(gameId: String, note: String?) = prefs.edit {
        val key = "guide_pin_${gameId.hashCode()}"
        if (note.isNullOrBlank()) remove(key) else putString(key, note.trim())
    }

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
