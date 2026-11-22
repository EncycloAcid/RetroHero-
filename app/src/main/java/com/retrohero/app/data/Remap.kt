package com.retrohero.app.data

import android.view.KeyEvent
import org.json.JSONObject
import java.io.File

/**
 * Controller button remaps. Each profile maps an Android key code to an action: a RetroPad
 * button id (0..15) or one of the special actions below. Profiles are keyed by the
 * controller's descriptor, or [ANY] for every controller; a key not in any profile keeps its
 * default meaning.
 */
object Remap {
    const val ANY = "any"
    const val REWIND = 100
    const val FAST_FORWARD = 101
    const val MENU = 102
    /** The key does nothing. */
    const val NONE = -1

    /** Everything you can assign, in the order the remap page shows them. */
    val actions: List<Pair<Int, String>> = listOf(
        RetroPad.B to "B (bottom)",
        RetroPad.A to "A (right)",
        RetroPad.Y to "Y (left)",
        RetroPad.X to "X (top)",
        RetroPad.UP to "Up",
        RetroPad.DOWN to "Down",
        RetroPad.LEFT to "Left",
        RetroPad.RIGHT to "Right",
        RetroPad.L to "L",
        RetroPad.R to "R",
        RetroPad.L2 to "L2",
        RetroPad.R2 to "R2",
        RetroPad.L3 to "L3 (left stick press)",
        RetroPad.R3 to "R3 (right stick press)",
        RetroPad.START to "Start",
        RetroPad.SELECT to "Select",
        REWIND to "Rewind (hold)",
        FAST_FORWARD to "Fast forward (hold)",
        MENU to "Pause menu",
    )

    /** Built-in meaning of a key, before any remap. */
    fun default(keyCode: Int, rewindOnL3: Boolean = true): Int? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_Z -> RetroPad.B
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_X -> RetroPad.A
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_A -> RetroPad.Y
        KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_S -> RetroPad.X
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q -> RetroPad.L
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_W -> RetroPad.R
        KeyEvent.KEYCODE_BUTTON_L2 -> RetroPad.L2
        KeyEvent.KEYCODE_BUTTON_R2 -> RetroPad.R2
        KeyEvent.KEYCODE_BUTTON_THUMBL -> if (rewindOnL3) REWIND else RetroPad.L3
        KeyEvent.KEYCODE_BUTTON_THUMBR -> RetroPad.R3
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> RetroPad.START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_DEL -> RetroPad.SELECT
        KeyEvent.KEYCODE_DPAD_UP -> RetroPad.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> RetroPad.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> RetroPad.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> RetroPad.RIGHT
        KeyEvent.KEYCODE_BUTTON_MODE -> MENU
        else -> null
    }

    /** What [keyCode] does for a controller with [descriptor], after remaps. */
    fun resolve(profiles: Map<String, Map<Int, Int>>, descriptor: String?, keyCode: Int, rewindOnL3: Boolean = true): Int? {
        descriptor?.let { profiles[it]?.get(keyCode) }?.let { return it.takeIf { a -> a != NONE } }
        profiles[ANY]?.get(keyCode)?.let { return it.takeIf { a -> a != NONE } }
        return default(keyCode, rewindOnL3)
    }

    /** Keys that currently trigger [action] in [profile] (falling back to "any" and defaults). */
    fun keysFor(profiles: Map<String, Map<Int, Int>>, profile: String, action: Int): List<Int> {
        val candidates = (profiles[profile].orEmpty().keys + profiles[ANY].orEmpty().keys + DefaultKeys).toSortedSet()
        return candidates.filter { resolve(profiles, profile.takeIf { it != ANY }, it) == action }
    }

    private val DefaultKeys = (0..KeyEvent.getMaxKeyCode()).filter { default(it) != null }

    /** "A", "L1", "D-pad up", "Enter"… for showing a key to a person. */
    fun keyName(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> "D-pad up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "D-pad down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "D-pad left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "D-pad right"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "Left stick"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "Right stick"
        KeyEvent.KEYCODE_BUTTON_A -> "A"
        KeyEvent.KEYCODE_BUTTON_B -> "B"
        KeyEvent.KEYCODE_BUTTON_X -> "X"
        KeyEvent.KEYCODE_BUTTON_Y -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
        KeyEvent.KEYCODE_BUTTON_START -> "Start"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
        KeyEvent.KEYCODE_BUTTON_MODE -> "Home/Mode"
        KeyEvent.KEYCODE_ENTER -> "Enter"
        KeyEvent.KEYCODE_DEL -> "Backspace"
        KeyEvent.KEYCODE_SHIFT_RIGHT -> "Right Shift"
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> ('A' + (keyCode - KeyEvent.KEYCODE_A)).toString()
        else -> KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .removePrefix("BUTTON_")
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }
            .let { if (it.length <= 2) it.uppercase() else it }
            .let { if (it.all(Char::isDigit)) "Key $it" else it }
    }

    fun load(file: File): Map<String, Map<Int, Int>> {
        val root = readJson(file) ?: return emptyMap()
        return root.keys().asSequence().associateWith { profile ->
            val o = root.getJSONObject(profile)
            o.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to o.getInt(k) } }.toMap()
        }.filterValues { it.isNotEmpty() }
    }

    fun save(file: File, profiles: Map<String, Map<Int, Int>>) {
        val root = JSONObject()
        profiles.filterValues { it.isNotEmpty() }.forEach { (p, m) ->
            root.put(p, JSONObject().apply { m.forEach { (k, v) -> put(k.toString(), v) } })
        }
        writeAtomic(file, root.toString())
    }
}
