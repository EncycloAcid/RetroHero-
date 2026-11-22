package com.retrohero.app.emulator

import android.view.Surface

/** JNI bindings to the native libretro frontend (app/src/main/cpp). */
object NativeBridge {
    init {
        System.loadLibrary("retrohero")
    }

    external fun loadCore(corePath: String, systemDir: String, saveDir: String): Boolean
    external fun loadGame(romPath: String, sramPath: String): Boolean
    external fun start()
    external fun setPaused(paused: Boolean)
    external fun setFastForward(enabled: Boolean, speed: Float)
    external fun reset()
    external fun saveState(path: String): Boolean
    external fun loadState(path: String): Boolean
    external fun saveSram(path: String): Boolean
    external fun shutdown(sramPath: String)

    external fun setSurface(surface: Surface?)
    external fun setSharpScale(scale: Int)

    external fun setButton(port: Int, id: Int, pressed: Boolean)
    external fun setAnalog(port: Int, index: Int, axis: Int, value: Float)
    /** Normalised pointer position, -1..1 on both axes across the game image. */
    external fun setPointer(x: Float, y: Float, down: Boolean)

    external fun setOption(key: String, value: String)
    external fun getOptions(): Array<String>
    external fun getAspectRatio(): Float
    external fun getLastError(): String

    data class CoreOption(val key: String, val description: String, val current: String, val values: List<String>)

    fun options(): List<CoreOption> = getOptions().mapNotNull { enc ->
        val parts = enc.split('\u001F')
        if (parts.size < 4) null
        else CoreOption(parts[0], parts[1], parts[2], parts[3].split('\u001E').filter { it.isNotEmpty() })
    }
}
