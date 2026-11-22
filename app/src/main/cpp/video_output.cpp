#include "video_output.h"

#include <cstring>

#include "libretro.h"
#include "log.h"

namespace rh {

namespace {

inline uint16_t rgb1555To565(uint16_t p) {
    uint16_t r = (p >> 10) & 0x1F, g = (p >> 5) & 0x1F, b = p & 0x1F;
    return static_cast<uint16_t>((r << 11) | (((g << 1) | (g >> 4)) << 5) | b);
}

inline uint32_t xrgb8888ToRgbx(uint32_t p) {
    return ((p >> 16) & 0xFF) | (p & 0x0000FF00) | ((p & 0xFF) << 16) | 0xFF000000u;
}

// Multiply a pixel's brightness by k/256.
inline uint16_t shade565(uint16_t p, unsigned k) {
    unsigned r = ((p >> 11) & 0x1F) * k >> 8, g = ((p >> 5) & 0x3F) * k >> 8, b = (p & 0x1F) * k >> 8;
    return static_cast<uint16_t>((r << 11) | (g << 5) | b);
}

inline uint32_t shadeRgbx(uint32_t p, unsigned k) {
    unsigned r = (p & 0xFF) * k >> 8, g = ((p >> 8) & 0xFF) * k >> 8, b = ((p >> 16) & 0xFF) * k >> 8;
    return r | (g << 8) | (b << 16) | 0xFF000000u;
}

inline uint32_t toArgb(const uint8_t *row, unsigned x, int fmt) {
    if (fmt == RETRO_PIXEL_FORMAT_XRGB8888) return reinterpret_cast<const uint32_t *>(row)[x] | 0xFF000000u;
    uint16_t p = reinterpret_cast<const uint16_t *>(row)[x];
    unsigned r, g, b;
    if (fmt == RETRO_PIXEL_FORMAT_0RGB1555) {
        r = (p >> 10) & 0x1F; g = (p >> 5) & 0x1F; b = p & 0x1F;
        r = (r << 3) | (r >> 2); g = (g << 3) | (g >> 2); b = (b << 3) | (b >> 2);
    } else {
        r = (p >> 11) & 0x1F; g = (p >> 5) & 0x3F; b = p & 0x1F;
        r = (r << 3) | (r >> 2); g = (g << 2) | (g >> 4); b = (b << 3) | (b >> 2);
    }
    return 0xFF000000u | (r << 16) | (g << 8) | b;
}

constexpr unsigned kScanline = 140;  // ~55%
constexpr unsigned kGrid = 180;      // ~70%

}  // namespace

VideoOutput::~VideoOutput() { setWindow(nullptr); }

void VideoOutput::setWindow(ANativeWindow *window) {
    std::lock_guard<std::mutex> lk(mutex_);
    if (window_) ANativeWindow_release(window_);
    window_ = window;
    bufW_ = bufH_ = bufFmt_ = 0;
    if (window_ && !last_.empty())
        blitLocked(last_.data(), lastW_, lastH_, lastPitch_, lastFmt_);
}

void VideoOutput::setFilter(int filter) {
    std::lock_guard<std::mutex> lk(mutex_);
    filter_ = filter < Sharp || filter > Lcd ? Sharp : filter;
    sharpScale_ = filter_ == Smooth ? 1 : 3;
    bufW_ = bufH_ = 0;
    if (window_ && !last_.empty())
        blitLocked(last_.data(), lastW_, lastH_, lastPitch_, lastFmt_);
}

void VideoOutput::redraw() {
    std::lock_guard<std::mutex> lk(mutex_);
    if (window_ && !last_.empty())
        blitLocked(last_.data(), lastW_, lastH_, lastPitch_, lastFmt_);
}

bool VideoOutput::grabArgb(std::vector<uint32_t> &out, unsigned &width, unsigned &height) {
    std::lock_guard<std::mutex> lk(mutex_);
    if (last_.empty() || lastW_ == 0 || lastH_ == 0) return false;
    width = lastW_;
    height = lastH_;
    out.resize(static_cast<size_t>(lastW_) * lastH_);
    for (unsigned y = 0; y < lastH_; y++) {
        const uint8_t *row = last_.data() + y * lastPitch_;
        for (unsigned x = 0; x < lastW_; x++) out[static_cast<size_t>(y) * lastW_ + x] = toArgb(row, x, lastFmt_);
    }
    return true;
}

void VideoOutput::present(const void *data, unsigned width, unsigned height, size_t pitch,
                          int pixelFormat) {
    if (!data || width == 0 || height == 0) return;
    std::lock_guard<std::mutex> lk(mutex_);
    size_t bytes = pitch * height;
    if (last_.size() != bytes) last_.resize(bytes);
    memcpy(last_.data(), data, bytes);
    lastW_ = width;
    lastH_ = height;
    lastPitch_ = pitch;
    lastFmt_ = pixelFormat;
    if (window_) blitLocked(last_.data(), width, height, pitch, pixelFormat);
}

void VideoOutput::blitLocked(const uint8_t *data, unsigned width, unsigned height, size_t pitch,
                             int fmt) {
    const bool is32 = fmt == RETRO_PIXEL_FORMAT_XRGB8888;
    const int winFmt = is32 ? WINDOW_FORMAT_RGBX_8888 : WINDOW_FORMAT_RGB_565;
    // Cap the pre-scale so large frames (e.g. 640x480) don't cost too much CPU.
    int s = sharpScale_;
    const int cap = height > 0 ? static_cast<int>(800 / height) : 1;
    if (s > cap) s = cap < 1 ? 1 : cap;
    const int outW = static_cast<int>(width) * s;
    const int outH = static_cast<int>(height) * s;

    if (outW != bufW_ || outH != bufH_ || winFmt != bufFmt_) {
        if (ANativeWindow_setBuffersGeometry(window_, outW, outH, winFmt) != 0) {
            LOGE("setBuffersGeometry failed");
            return;
        }
        bufW_ = outW;
        bufH_ = outH;
        bufFmt_ = winFmt;
    }

    ANativeWindow_Buffer buf;
    if (ANativeWindow_lock(window_, &buf, nullptr) != 0) return;
    const int rows = outH < buf.height ? outH : buf.height;
    const int cols = outW < buf.width ? outW : buf.width;

    // Effects need at least 2 output pixels per source pixel to have anywhere to go.
    const bool crt = filter_ == Crt && s >= 2;
    const bool lcd = filter_ == Lcd && s >= 2;

    if (is32) {
        auto *dst = static_cast<uint32_t *>(buf.bits);
        for (int y = 0; y < rows; y++) {
            auto *src = reinterpret_cast<const uint32_t *>(data + (y / s) * pitch);
            uint32_t *d = dst + static_cast<size_t>(y) * buf.stride;
            const bool edgeRow = y % s == s - 1;
            for (int x = 0; x < cols; x++) d[x] = xrgb8888ToRgbx(src[x / s]);
            if (crt && edgeRow) {
                for (int x = 0; x < cols; x++) d[x] = shadeRgbx(d[x], kScanline);
            } else if (lcd) {
                for (int x = 0; x < cols; x++)
                    if (edgeRow || x % s == s - 1) d[x] = shadeRgbx(d[x], kGrid);
            }
        }
    } else {
        auto *dst = static_cast<uint16_t *>(buf.bits);
        const bool is1555 = fmt == RETRO_PIXEL_FORMAT_0RGB1555;
        for (int y = 0; y < rows; y++) {
            auto *src = reinterpret_cast<const uint16_t *>(data + (y / s) * pitch);
            uint16_t *d = dst + static_cast<size_t>(y) * buf.stride;
            if (s == 1 && !is1555) {
                memcpy(d, src, cols * sizeof(uint16_t));
            } else if (is1555) {
                for (int x = 0; x < cols; x++) d[x] = rgb1555To565(src[x / s]);
            } else {
                for (int x = 0; x < cols; x++) d[x] = src[x / s];
            }
            const bool edgeRow = y % s == s - 1;
            if (crt && edgeRow) {
                for (int x = 0; x < cols; x++) d[x] = shade565(d[x], kScanline);
            } else if (lcd) {
                for (int x = 0; x < cols; x++)
                    if (edgeRow || x % s == s - 1) d[x] = shade565(d[x], kGrid);
            }
        }
    }
    ANativeWindow_unlockAndPost(window_);
}

}  // namespace rh
