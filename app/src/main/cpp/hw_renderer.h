#pragma once

#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>

#include <atomic>
#include <cstdint>
#include <mutex>
#include <vector>

namespace rh {

/**
 * Native EGL + OpenGL ES 3.0 (with ES 2.0 fallback) hardware rendering backend for
 * libretro cores requesting RETRO_ENVIRONMENT_SET_HW_RENDER (PS2 LRPS2/Play!, GameCube/Wii
 * Dolphin, 3DS Azahar/Citra, PSP PPSSPP, Dreamcast Flycast, N64 Mupen64Plus-Next,
 * Saturn YabaSanshiro, PS1 SwanStation/Beetle HW).
 *
 * All EGL/GL calls run strictly on the emulation thread. Surface attachment from the UI
 * thread is queued via [setWindow] and applied safely on the emulation thread.
 */
class HwRenderer {
public:
    ~HwRenderer();

    bool initContext(unsigned requestedMajor, unsigned requestedMinor, bool needDepth, bool needStencil);
    void destroyContext();
    bool isReady() const { return context_ != EGL_NO_CONTEXT; }

    /** Queues a new ANativeWindow (takes ownership of one reference). Safe from any thread. */
    void setWindow(ANativeWindow *window);

    /** Sets presentation filter (0 sharp, 1 smooth, 2 CRT, 3 LCD). Safe from any thread. */
    void setFilter(int filter) { filter_.store(filter); }

    /** Sets authentic hardware color matrix (0 none, 1 GBA LCD, 2 GBC LCD, 3 CRT composite). */
    void setColorProfile(int profile) { colorProfile_.store(profile); }

    /** Enables or disables near-zero-cost 16x16 OpenGL Ambilight edge color sampling. */
    void setAmbilightEnabled(bool enabled) { ambilightEnabled_.store(enabled, std::memory_order_relaxed); }

    /** Reads the latest 4 directional Ambilight ARGB colors + flash [0..255]. */
    void pollAmbilight(uint32_t out[5]) const;

    /** Called on the emulation thread before core_.run() to sync window & bind FBO. */
    void beginFrame(unsigned maxWidth, unsigned maxHeight);

    /** Called on the emulation thread from videoCb(RETRO_HW_FRAME_BUFFER_VALID, w, h). */
    void presentFrame(unsigned width, unsigned height, bool bottomLeftOrigin);

    /** Inserts a single pure black frame for 120 Hz OLED Black Frame Insertion (BFI). */
    void presentBlack();

    /** Returns the offscreen FBO id for libretro's get_current_framebuffer callback. */
    uintptr_t currentFramebuffer() const { return static_cast<uintptr_t>(fbo_); }

    /** Returns last presented frame dimensions. */
    void frameSize(unsigned &width, unsigned &height);

    /** Captures the last presented hardware frame as ARGB_8888. */
    bool grabArgb(std::vector<uint32_t> &out, unsigned &width, unsigned &height);

private:
    bool ensureFbo(unsigned width, unsigned height);
    bool ensureAmbilightFbo();
    void destroyAmbilightFbo();
    bool initQuadProgram();
    void applyPendingWindow();
    void destroyWindowSurface();
    void destroyFbo();
    void captureSnapshotLocked(unsigned width, unsigned height, bool bottomLeftOrigin);

    EGLDisplay display_ = EGL_NO_DISPLAY;
    EGLConfig config_ = nullptr;
    EGLContext context_ = EGL_NO_CONTEXT;
    EGLSurface pbuffer_ = EGL_NO_SURFACE;
    EGLSurface winSurface_ = EGL_NO_SURFACE;
    ANativeWindow *activeWindow_ = nullptr;

    bool isGles3_ = false;
    bool needDepth_ = true;
    bool needStencil_ = false;

    GLuint fbo_ = 0;
    GLuint colorTex_ = 0;
    GLuint depthRb_ = 0;
    unsigned fboW_ = 0;
    unsigned fboH_ = 0;

    GLuint ambiFbo_ = 0;
    GLuint ambiTex_ = 0;
    uint32_t ambiTick_ = 0;
    std::atomic<bool> ambilightEnabled_{true};
    std::atomic<uint32_t> ambilight_[5]{0xFF000000u, 0xFF000000u, 0xFF000000u, 0xFF000000u, 0u};
    float ambilightAvgLuma_ = -1.0f;

    GLuint program_ = 0;
    GLint locPos_ = -1;
    GLint locUv_ = -1;
    GLint locTex_ = -1;
    GLint locFilter_ = -1;
    GLint locColorProfile_ = -1;
    GLint locSrcSize_ = -1;
    GLint locUvScale_ = -1;
    GLint locFlipY_ = -1;

    std::atomic<int> filter_{0};
    std::atomic<int> colorProfile_{0};

    std::mutex winMutex_;
    ANativeWindow *pendingWindow_ = nullptr;
    bool windowDirty_ = false;

    std::mutex snapMutex_;
    std::vector<uint32_t> lastArgb_;
    unsigned lastW_ = 0;
    unsigned lastH_ = 0;
    std::atomic<bool> grabRequested_{false};
};

}  // namespace rh
