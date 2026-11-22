package com.retrohero.app.emulator

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.retrohero.app.data.Remap
import com.retrohero.app.data.RetroPad
import kotlin.math.abs

/**
 * Maps physical controllers and keyboards to RetroPad ports. Android's gamepad layout
 * (A bottom, B right) is mapped positionally to the SNES-style RetroPad (B bottom, A right).
 */
class InputMapper(
    private val stickAsDpad: Boolean,
    /** Remap profiles from [Remap.load]. */
    private val profiles: Map<String, Map<Int, Int>> = emptyMap(),
) {
    private val ports = LinkedHashMap<Int, Int>()
    private val motionDpad = HashMap<Int, Int>() // port -> bitmask of dpad buttons driven by axes
    private val triggers = HashMap<Int, Int>()

    /** Called with true/false while the rewind button is held. */
    var onRewind: ((Boolean) -> Unit)? = null
    var onFastForward: ((Boolean) -> Unit)? = null
    var onMenu: (() -> Unit)? = null
    /** L3 rewinds by default on systems that don't use the stick buttons. */
    var rewindKeyEnabled = true

    fun portFor(deviceId: Int): Int = ports.getOrPut(deviceId) { ports.size.coerceAtMost(3) }

    fun isGamepad(event: KeyEvent): Boolean {
        val s = event.source
        return s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
            s and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD && event.device?.isVirtual == false
    }

    fun keyToRetro(keyCode: Int): Int? = Remap.default(keyCode, rewindKeyEnabled)?.takeIf { it < Remap.REWIND }

    /** Returns true if the event was consumed. */
    fun onKey(event: KeyEvent): Boolean {
        val action = Remap.resolve(profiles, event.device?.descriptor, event.keyCode, rewindKeyEnabled) ?: return false
        val down = event.action == KeyEvent.ACTION_DOWN
        if (down && event.repeatCount > 0) return true
        when (action) {
            Remap.REWIND -> onRewind?.invoke(down)
            Remap.FAST_FORWARD -> onFastForward?.invoke(down)
            Remap.MENU -> if (!down) onMenu?.invoke()
            else -> NativeBridge.setButton(portFor(event.deviceId), action, down)
        }
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
