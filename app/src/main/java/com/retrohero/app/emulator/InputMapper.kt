package com.retrohero.app.emulator

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.retrohero.app.data.RetroPad
import kotlin.math.abs

/**
 * Maps physical controllers and keyboards to RetroPad ports. Android's gamepad layout
 * (A bottom, B right) is mapped positionally to the SNES-style RetroPad (B bottom, A right).
 */
class InputMapper(private val stickAsDpad: Boolean) {
    private val ports = LinkedHashMap<Int, Int>()
    private val motionDpad = HashMap<Int, Int>() // port -> bitmask of dpad buttons driven by axes
    private val triggers = HashMap<Int, Int>()

    /** Called with true/false while the rewind button is held. */
    var onRewind: ((Boolean) -> Unit)? = null
    /** L3 rewinds on systems that don't use the stick buttons. */
    var rewindKeyEnabled = true

    fun portFor(deviceId: Int): Int = ports.getOrPut(deviceId) { ports.size.coerceAtMost(3) }

    fun isGamepad(event: KeyEvent): Boolean {
        val s = event.source
        return s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
            s and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD && event.device?.isVirtual == false
    }

    fun keyToRetro(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_Z -> RetroPad.B
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_X -> RetroPad.A
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_A -> RetroPad.Y
        KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_S -> RetroPad.X
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q -> RetroPad.L
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_W -> RetroPad.R
        KeyEvent.KEYCODE_BUTTON_L2 -> RetroPad.L2
        KeyEvent.KEYCODE_BUTTON_R2 -> RetroPad.R2
        KeyEvent.KEYCODE_BUTTON_THUMBL -> RetroPad.L3
        KeyEvent.KEYCODE_BUTTON_THUMBR -> RetroPad.R3
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> RetroPad.START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_DEL -> RetroPad.SELECT
        KeyEvent.KEYCODE_DPAD_UP -> RetroPad.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> RetroPad.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> RetroPad.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> RetroPad.RIGHT
        else -> null
    }

    /** Returns true if the event was consumed. */
    fun onKey(event: KeyEvent): Boolean {
        if (rewindKeyEnabled && event.keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL && onRewind != null) {
            if (event.repeatCount == 0) onRewind?.invoke(event.action == KeyEvent.ACTION_DOWN)
            return true
        }
        val id = keyToRetro(event.keyCode) ?: return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) return true
        NativeBridge.setButton(portFor(event.deviceId), id, event.action == KeyEvent.ACTION_DOWN)
        return true
    }

    fun onMotion(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK ||
            event.action != MotionEvent.ACTION_MOVE
        ) return false
        val port = portFor(event.deviceId)
        val device = event.device

        fun axis(a: Int): Float {
            val v = event.getAxisValue(a)
            val flat = device?.getMotionRange(a, event.source)?.flat ?: 0f
            return if (abs(v) > maxOf(flat, 0.1f)) v else 0f
        }

        val lx = axis(MotionEvent.AXIS_X)
        val ly = axis(MotionEvent.AXIS_Y)
        NativeBridge.setAnalog(port, 0, 0, lx)
        NativeBridge.setAnalog(port, 0, 1, ly)
        NativeBridge.setAnalog(port, 1, 0, axis(MotionEvent.AXIS_Z))
        NativeBridge.setAnalog(port, 1, 1, axis(MotionEvent.AXIS_RZ))

        // D-pad from the hat (and optionally the left stick).
        val hx = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val sx = if (stickAsDpad) lx else 0f
        val sy = if (stickAsDpad) ly else 0f
        var mask = 0
        if (hx < -0.5f || sx < -0.5f) mask = mask or (1 shl RetroPad.LEFT)
        if (hx > 0.5f || sx > 0.5f) mask = mask or (1 shl RetroPad.RIGHT)
        if (hy < -0.5f || sy < -0.5f) mask = mask or (1 shl RetroPad.UP)
        if (hy > 0.5f || sy > 0.5f) mask = mask or (1 shl RetroPad.DOWN)
        val prev = motionDpad[port] ?: 0
        if (mask != prev) {
            for (id in intArrayOf(RetroPad.UP, RetroPad.DOWN, RetroPad.LEFT, RetroPad.RIGHT)) {
                val bit = 1 shl id
                if ((mask xor prev) and bit != 0) NativeBridge.setButton(port, id, mask and bit != 0)
            }
            motionDpad[port] = mask
        }

        // Analog triggers -> L2 / R2.
        val lt = maxOf(axis(MotionEvent.AXIS_LTRIGGER), axis(MotionEvent.AXIS_BRAKE))
        val rt = maxOf(axis(MotionEvent.AXIS_RTRIGGER), axis(MotionEvent.AXIS_GAS))
        var tmask = 0
        if (lt > 0.5f) tmask = tmask or (1 shl RetroPad.L2)
        if (rt > 0.5f) tmask = tmask or (1 shl RetroPad.R2)
        val tprev = triggers[port] ?: 0
        if (tmask != tprev) {
            for (id in intArrayOf(RetroPad.L2, RetroPad.R2)) {
                val bit = 1 shl id
                if ((tmask xor tprev) and bit != 0) NativeBridge.setButton(port, id, tmask and bit != 0)
            }
            triggers[port] = tmask
        }
        return true
    }
}
