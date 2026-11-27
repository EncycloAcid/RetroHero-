package com.retrohero.app.emulator

import android.view.Surface

/** JNI bindings to the native libretro frontend (app/src/main/cpp). */
object NativeBridge {
    init {
        System.loadLibrary("retrohero")
    }

    external fun loadCore(corePath: String, systemDir: String, saveDir: String): Boolean
    external fun loadGame(romPath: String, sramPath: String): Boolean
    external fun loadGameMemory(
        virtualPath: String,
        romBytes: ByteArray,
        sramPath: String,
        scratchDir: String,
    ): Boolean
    external fun start()
    external fun setPaused(paused: Boolean)
    external fun setFastForward(enabled: Boolean, speed: Float)
    external fun setSlowMotion(enabled: Boolean)
    external fun setRunAhead(frames: Int)
    external fun onVsync(frameTimeNanos: Long, displayHz: Float)
    external fun setVsyncConfig(vsyncLock: Boolean, bfi120Hz: Boolean)
    external fun getCoreFps(): Float
    external fun isVsyncLocked(): Boolean
    external fun isBfiActive(): Boolean
    /** Returns [actualFps, frameTimeMs, activeRunAheadFrames]. */
    external fun getPerfStats(): FloatArray
    external fun reset()
    external fun saveState(path: String): Boolean
    external fun loadState(path: String): Boolean
    external fun saveSram(path: String): Boolean
    /** Snapshots a libretro memory region (0 = SAVE_RAM, 2 = SYSTEM_RAM) for RetroAchievements. */
    external fun readMemory(memoryType: Int, maxBytes: Int = 8 * 1024 * 1024): ByteArray?
    external fun shutdown(sramPath: String)

    external fun setInitialDisc(index: Int)
    external fun getDiscCount(): Int
    external fun getCurrentDisc(): Int
    external fun getDiscLabels(): Array<String>
    external fun swapDisc(index: Int): Boolean

    external fun setSurface(surface: Surface?)
    /** See [ScreenFilter]. */
    external fun setFilter(filter: Int)
    /** 0 = Vivid/None, 1 = Authentic GBA LCD, 2 = Authentic GBC LCD, 3 = CRT Composite Blend. */
    external fun setColorProfile(profile: Int)
    /** Software pixel prescale multiplier (1..4) for 2D crisp pixel rendering. */
    external fun setUpscaleFactor(factor: Int)
    /** Enables or disables near-zero-cost C++ framebuffer edge color sampling for OLED Ambilight. */
    external fun setAmbilightEnabled(enabled: Boolean)
    /** Writes [topArgb, bottomArgb, leftArgb, rightArgb, flash0to255] into [out5] with zero allocation. */
    external fun pollAmbilight(out5: IntArray)
    /** Width and height of the last frame the core drew (0, 0 before the first). */
    external fun getFrameSize(): IntArray
    /** [width, height, ARGB pixels…] of the last frame, or null before the first. */
    external fun grabFrame(): IntArray?

    external fun setRewindEnabled(enabled: Boolean)
    external fun setRewinding(rewinding: Boolean)

    external fun supportsCheats(): Boolean
    /** Replaces the active cheats; false if the core has no cheat support. */
    external fun setCheats(codes: Array<String>): Boolean

    external fun setButton(port: Int, id: Int, pressed: Boolean)
    external fun getButtonsMask(port: Int): Int
    external fun setAnalog(port: Int, index: Int, axis: Int, value: Float)
    /** Normalised pointer position, -1..1 on both axes across the game image. */
    external fun setPointer(x: Float, y: Float, down: Boolean)

    /** 3-Second Mobile Rollback Netplay (GGPO-style native frame rollback). */
    external fun netplayStart(localPort: Int, inputDelayFrames: Int): Boolean
    external fun netplayStop()
    external fun netplayPushRemoteInput(frame: Int, buttonsMask: Int)
    /** Returns [active, currentFrame, lastLocalFrame, lastLocalMask, rollbackCount, maxDepth, stateChecksum]. */
    external fun netplayPollTelemetry(): IntArray

    external fun setOption(key: String, value: String)
    external fun getOptions(): Array<String>
    external fun getAspectRatio(): Float
    /** Peak audio-driven haptic energy [0..1] detected since the previous poll. */
    external fun consumeAudioHaptic(): Float
    external fun getLastError(): String

    data class CoreOption(val key: String, val description: String, val current: String, val values: List<String>)

    fun options(): List<CoreOption> = getOptions().mapNotNull { enc ->
        val parts = enc.split('\u001F')
        if (parts.size < 4) null
        else CoreOption(parts[0], parts[1], parts[2], parts[3].split('\u001E').filter { it.isNotEmpty() })
    }
}
