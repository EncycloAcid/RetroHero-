package com.retrohero.app.data

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.io.File

/** How the touch buttons are drawn. */
data class PadSkin(
    val id: String,
    val name: String,
    /** Body of D-pad, shoulders and small buttons. */
    val fill: Color,
    val edge: Color,
    /** Body colour while held; null uses the system/accent colour. */
    val pressed: Color?,
    val label: Color,
    /** Face buttons: "color" (classic colours), "mono" (like the body), or "accent". */
    val faces: String,
    /** 0 = no outline glow, 1 = soft glow in the edge colour. */
    val glow: Float = 0f,
)

/** An app look: an accent colour plus a touch-pad skin. */
data class Look(val id: String, val name: String, val accent: Color, val pad: PadSkin, val custom: Boolean = false)

object Looks {
    /** Looks like the real controller of the system being played (see ConsoleStyles). */
    val Console = PadSkin("console", "Console", Color(0xFF232325), Color(0xFFD6D6D6), Color(0xFFD0203C), Color(0xFFD0203C), "console")
    val Glass = PadSkin("glass", "Glass", Color(0xFF1A1622).copy(alpha = 0.55f), Color.White.copy(alpha = 0.22f), null, Color.White, "color")
    val Classic = PadSkin("classic", "Classic", Color(0xFFCFCFD6), Color(0xFF6E6E78), Color(0xFF9C9CA6), Color(0xFF2A2A30), "color")
    val Minimal = PadSkin("minimal", "Minimal", Color.Transparent, Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.85f), "mono")
    val Neon = PadSkin("neon", "Neon", Color.Black.copy(alpha = 0.55f), Color(0xFFF0908D), null, Color.White, "accent", glow = 1f)
    val Stealth = PadSkin("stealth", "Stealth", Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.55f), "mono")
    val skins = listOf(Console, Glass, Classic, Minimal, Neon, Stealth)

    val accents = listOf(
        "ember" to ("Ember" to Color(0xFFFF6B2C)),
        "salmon" to ("Salmon" to Color(0xFFF0908D)),
        "mint" to ("Mint" to Color(0xFF7FD8B5)),
        "sky" to ("Sky" to Color(0xFF7DB7F5)),
        "lavender" to ("Lavender" to Color(0xFFB4A2F2)),
        "butter" to ("Butter" to Color(0xFFF2D27A)),
        "snow" to ("Snow" to Color(0xFFE8E8EC)),
    )

    fun accent(id: String?): Color = accents.firstOrNull { it.first == id }?.second?.second ?: accents.first().second.second
    fun skin(id: String?): PadSkin = skins.firstOrNull { it.id == id } ?: Console

    // ---- Look files you can import ----------------------------------------------------------

    /**
     * Reads a look file:
     * `{"name": "Game Boy", "accent": "#9BBC0F", "pad": {"fill": "#CC306230", "edge": "#8BAC0F",
     *   "pressed": "#0F380F", "label": "#E0F8D0", "faces": "mono", "glow": 0}}`.
     */
    fun parse(text: String, id: String): Look {
        val o = JSONObject(text)
        val name = o.optString("name").ifBlank { throw IllegalArgumentException("A look needs a name") }.take(40)
        val accent = color(o.optString("accent")) ?: accent(null)
        val p = o.optJSONObject("pad") ?: JSONObject()
        val pad = PadSkin(
            id = "look:$id",
            name = name,
            fill = color(p.optString("fill")) ?: Glass.fill,
            edge = color(p.optString("edge")) ?: Glass.edge,
            pressed = color(p.optString("pressed")),
            label = color(p.optString("label")) ?: Color.White,
            faces = p.optString("faces").takeIf { it in setOf("color", "mono", "accent") } ?: "color",
            glow = p.optDouble("glow", 0.0).toFloat().coerceIn(0f, 1f),
        )
        return Look(id, name, accent, pad, custom = true)
    }

    /** Your imported looks in [dir]. Broken files are skipped. */
    fun custom(dir: File): List<Look> =
        dir.listFiles { f -> f.extension == "json" }?.sortedBy { it.nameWithoutExtension }?.mapNotNull { f ->
            runCatching { parse(f.readText(), f.nameWithoutExtension) }.getOrNull()
        }.orEmpty()

    /** Validates [text] and saves it into [dir]; returns the look. */
    fun import(dir: File, text: String): Look {
        val base = Regex("[^a-z0-9]+").replace(JSONObject(text).optString("name").lowercase(), "-").trim('-').ifEmpty { "look" }.take(32)
        var id = base
        var n = 2
        while (File(dir, "$id.json").exists()) id = "$base-${n++}"
        val look = parse(text, id)
        dir.mkdirs()
        writeAtomic(File(dir, "$id.json"), text)
        return look
    }

    /** "#RGB", "#RRGGBB" or "#AARRGGBB". */
    fun color(s: String?): Color? {
        val h = s?.trim()?.removePrefix("#") ?: return null
        val v = when (h.length) {
            3 -> h.map { "$it$it" }.joinToString("").toLongOrNull(16)?.or(0xFF000000)
            6 -> h.toLongOrNull(16)?.or(0xFF000000)
            8 -> h.toLongOrNull(16)
            else -> null
        } ?: return null
        return Color(v.toInt())
    }
}
