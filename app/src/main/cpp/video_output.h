#pragma once

#include <android/native_window.h>
#include <cstddef>
#include <cstdint>
#include <mutex>
#include <vector>

namespace rh {

// Software video output: blits core frames straight into the SurfaceView's
// ANativeWindow. The compositor scales the buffer to the view; an optional
// integer nearest-neighbour pre-scale keeps pixel art sharp.
class VideoOutput {
public:
    ~VideoOutput();

    void setWindow(ANativeWindow *window);  // takes ownership of one reference
    // 0 sharp, 1 smooth, 2 CRT scanlines, 3 LCD grid, 4 Pixel Art 2x (EPX/Scale2x), 5 Sharp Bilinear.
    enum Filter { Sharp = 0, Smooth = 1, Crt = 2, Lcd = 3, Scale2x = 4, SharpBilinear = 5 };
    // 0 none (vivid), 1 GBA AGB-001 LCD matrix, 2 GBC LCD matrix, 3 CRT composite horizontal blend.
    enum ColorProfile { ColorNone = 0, ColorGbaLcd = 1, ColorGbcLcd = 2, ColorCrtComposite = 3 };
    void setFilter(int filter);
    void setColorProfile(int profile);
    void setUpscaleFactor(int factor);
    // Copies the last frame as ARGB_8888 into [out]; false if there is none yet.
    void frameSize(unsigned &width, unsigned &height) {
        std::lock_guard<std::mutex> lk(mutex_);
        width = lastW_;
        height = lastH_;
    }
    bool grabArgb(std::vector<uint32_t> &out, unsigned &width, unsigned &height);
    void present(const void *data, unsigned width, unsigned height, size_t pitch, int pixelFormat);
    void presentBlack();
    void redraw();

private:
    void rebuildColorLutLocked();
    void blitLocked(const uint8_t *data, unsigned width, unsigned height, size_t pitch, int fmt);

    std::mutex mutex_;
    ANativeWindow *window_ = nullptr;
    int bufW_ = 0, bufH_ = 0, bufFmt_ = 0;
    int filter_ = Sharp;
    int colorProfile_ = ColorNone;
    int upscaleFactor_ = 3;
    int sharpScale_ = 3;
    std::vector<uint16_t> lut565_;

    std::vector<uint8_t> last_;
    unsigned lastW_ = 0, lastH_ = 0;
    size_t lastPitch_ = 0;
    int lastFmt_ = 0;
};

}  // namespace rh
