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
    void setSharpScale(int scale);
    void present(const void *data, unsigned width, unsigned height, size_t pitch, int pixelFormat);
    void redraw();

private:
    void blitLocked(const uint8_t *data, unsigned width, unsigned height, size_t pitch, int fmt);

    std::mutex mutex_;
    ANativeWindow *window_ = nullptr;
    int bufW_ = 0, bufH_ = 0, bufFmt_ = 0;
    int sharpScale_ = 1;

    std::vector<uint8_t> last_;
    unsigned lastW_ = 0, lastH_ = 0;
    size_t lastPitch_ = 0;
    int lastFmt_ = 0;
};

}  // namespace rh
