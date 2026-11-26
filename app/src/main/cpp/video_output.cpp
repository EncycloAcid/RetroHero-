#include "video_output.h"

#include <algorithm>
#include <cmath>
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

inline uint16_t shade565(uint16_t p, unsigned k) {
    unsigned r = ((p >> 11) & 0x1F) * k >> 8, g = ((p >> 5) & 0x3F) * k >> 8, b = (p & 0x1F) * k >> 8;
    return static_cast<uint16_t>((r << 11) | (g << 5) | b);
}

inline uint32_t shadeRgbx(uint32_t p, unsigned k) {
    unsigned r = (p & 0xFF) * k >> 8, g = ((p >> 8) & 0xFF) * k >> 8, b = ((p >> 16) & 0xFF) * k >> 8;
    return r | (g << 8) | (b << 16) | 0xFF000000u;
}

// Fast integer 3-tap horizontal NTSC composite blend (0.25*L + 0.5*C + 0.25*R) for dither transparency
inline uint16_t blend565(uint16_t a, uint16_t b, uint16_t c) {
    unsigned r = (((a >> 11) & 0x1F) + (((b >> 11) & 0x1F) << 1) + ((c >> 11) & 0x1F) + 2) >> 2;
    unsigned g = (((a >> 5) & 0x3F) + (((b >> 5) & 0x3F) << 1) + ((c >> 5) & 0x3F) + 2) >> 2;
    unsigned bl = ((a & 0x1F) + ((b & 0x1F) << 1) + (c & 0x1F) + 2) >> 2;
    return static_cast<uint16_t>((r << 11) | (g << 5) | bl);
}

inline uint32_t blendRgbx(uint32_t a, uint32_t b, uint32_t c) {
    unsigned r = ((a & 0xFF) + ((b & 0xFF) << 1) + (c & 0xFF) + 2) >> 2;
    unsigned g = (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) << 1) + ((c >> 8) & 0xFF) + 2) >> 2;
    unsigned bl = (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) << 1) + ((c >> 16) & 0xFF) + 2) >> 2;
    return r | (g << 8) | (bl << 16) | 0xFF000000u;
}

inline uint32_t applyLcdMatrixRgbx(uint32_t p, int profile) {
    float r = static_cast<float>(p & 0xFF) / 255.0f;
    float g = static_cast<float>((p >> 8) & 0xFF) / 255.0f;
    float b = static_cast<float>((p >> 16) & 0xFF) / 255.0f;
    // Slight gamma lift matches reflective handheld TFT tone response
    r = std::pow(r, 1.18f);
    g = std::pow(g, 1.18f);
    b = std::pow(b, 1.18f);
    float nr, ng, nb;
    if (profile == VideoOutput::ColorGbaLcd) {
        nr = 0.85f * r + 0.12f * g + 0.03f * b;
        ng = 0.08f * r + 0.78f * g + 0.14f * b;
        nb = 0.06f * r + 0.22f * g + 0.72f * b;
    } else {
        // Authentic GBC reflective LCD color matrix
        nr = 0.82f * r + 0.14f * g + 0.04f * b;
        ng = 0.10f * r + 0.74f * g + 0.16f * b;
        nb = 0.08f * r + 0.18f * g + 0.74f * b;
    }
    auto clamp8 = [](float v) -> uint32_t {
        int i = static_cast<int>(v * 255.0f + 0.5f);
        if (i < 0) i = 0;
        if (i > 255) i = 255;
        return static_cast<uint32_t>(i);
    };
    return clamp8(nr) | (clamp8(ng) << 8) | (clamp8(nb) << 16) | 0xFF000000u;
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
    filter_ = (filter < Sharp || filter > SharpBilinear) ? Sharp : filter;
    sharpScale_ = (filter_ == Smooth) ? 1 : (filter_ == Scale2x ? 2 : upscaleFactor_);
    bufW_ = bufH_ = 0;
    if (window_ && !last_.empty())
        blitLocked(last_.data(), lastW_, lastH_, lastPitch_, lastFmt_);
}

void VideoOutput::setUpscaleFactor(int factor) {
    std::lock_guard<std::mutex> lk(mutex_);
    int clamped = std::clamp(factor, 1, 4);
    if (clamped == upscaleFactor_) return;
    upscaleFactor_ = clamped;
    sharpScale_ = (filter_ == Smooth) ? 1 : (filter_ == Scale2x ? 2 : upscaleFactor_);
    bufW_ = bufH_ = 0;
    if (window_ && !last_.empty())
        blitLocked(last_.data(), lastW_, lastH_, lastPitch_, lastFmt_);
}

void VideoOutput::setColorProfile(int profile) {
    std::lock_guard<std::mutex> lk(mutex_);
    int clamped = (profile < ColorNone || profile > ColorCrtComposite) ? ColorNone : profile;
    if (clamped == colorProfile_) return;
    colorProfile_ = clamped;
    rebuildColorLutLocked();
    if (window_ && !last_.empty())
        blitLocked(last_.data(), lastW_, lastH_, lastPitch_, lastFmt_);
}

void VideoOutput::rebuildColorLutLocked() {
    if (colorProfile_ != ColorGbaLcd && colorProfile_ != ColorGbcLcd) {
        lut565_.clear();
        return;
    }
    lut565_.resize(65536);
    for (uint32_t p = 0; p < 65536; p++) {
        uint32_t r = ((p >> 11) & 0x1F); r = (r << 3) | (r >> 2);
        uint32_t g = ((p >> 5) & 0x3F); g = (g << 2) | (g >> 4);
        uint32_t b = (p & 0x1F); b = (b << 3) | (b >> 2);
        uint32_t rgbx = r | (g << 8) | (b << 16) | 0xFF000000u;
        uint32_t out = applyLcdMatrixRgbx(rgbx, colorProfile_);
        uint16_t nr = static_cast<uint16_t>(((out & 0xFF) >> 3) & 0x1F);
        uint16_t ng = static_cast<uint16_t>((((out >> 8) & 0xFF) >> 2) & 0x3F);
        uint16_t nb = static_cast<uint16_t>((((out >> 16) & 0xFF) >> 3) & 0x1F);
        lut565_[p] = static_cast<uint16_t>((nr << 11) | (ng << 5) | nb);
    }
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

void VideoOutput::presentBlack() {
    std::lock_guard<std::mutex> lk(mutex_);
    if (!window_ || bufW_ <= 0 || bufH_ <= 0) return;
    ANativeWindow_Buffer buf;
    if (ANativeWindow_lock(window_, &buf, nullptr) != 0) return;
    const size_t bpp = (bufFmt_ == WINDOW_FORMAT_RGBX_8888) ? 4 : 2;
    const size_t totalBytes = static_cast<size_t>(buf.stride) * static_cast<size_t>(buf.height) * bpp;
    memset(buf.bits, 0, totalBytes);
    ANativeWindow_unlockAndPost(window_);
}

// Fast division-free unrolled software blitter with O(1) GBA/GBC LCD color LUT & CRT composite blend.
void VideoOutput::blitLocked(const uint8_t *data, unsigned width, unsigned height, size_t pitch,
                             int fmt) {
    const bool is32 = fmt == RETRO_PIXEL_FORMAT_XRGB8888;
    const int winFmt = is32 ? WINDOW_FORMAT_RGBX_8888 : WINDOW_FORMAT_RGB_565;
    int s = sharpScale_;
    const int cap = height > 0 ? static_cast<int>(1080 / height) : 1;
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
    const int srcRows = std::min(static_cast<int>(height), buf.height / s);
    const int srcCols = std::min(static_cast<int>(width), buf.width / s);
    const int outCols = srcCols * s;

    const bool crt = filter_ == Crt && s >= 2;
    const bool lcd = filter_ == Lcd && s >= 2;
    const bool scale2x = filter_ == Scale2x && s == 2;
    const bool useLcdLut = (colorProfile_ == ColorGbaLcd || colorProfile_ == ColorGbcLcd) && lut565_.size() == 65536;
    const bool useComposite = colorProfile_ == ColorCrtComposite;
    const uint16_t *lut = useLcdLut ? lut565_.data() : nullptr;

    if (is32) {
        auto *dst = static_cast<uint32_t *>(buf.bits);
        const size_t rowBytes = static_cast<size_t>(outCols) * sizeof(uint32_t);
        auto fetch32 = [&](const uint32_t *src, int sx) -> uint32_t {
            uint32_t px = xrgb8888ToRgbx(src[sx]);
            if (useLcdLut) px = applyLcdMatrixRgbx(px, colorProfile_);
            return px;
        };
        if (scale2x) {
            for (int sy = 0; sy < srcRows; sy++) {
                const auto *srcC = reinterpret_cast<const uint32_t *>(data + static_cast<size_t>(sy) * pitch);
                const auto *srcU = reinterpret_cast<const uint32_t *>(data + static_cast<size_t>(sy > 0 ? sy - 1 : 0) * pitch);
                const auto *srcD = reinterpret_cast<const uint32_t *>(data + static_cast<size_t>(sy + 1 < srcRows ? sy + 1 : sy) * pitch);
                uint32_t *row0 = dst + static_cast<size_t>(sy * 2) * buf.stride;
                uint32_t *row1 = row0 + buf.stride;
                for (int sx = 0; sx < srcCols; sx++) {
                    const int xl = sx > 0 ? sx - 1 : 0;
                    const int xr = sx + 1 < srcCols ? sx + 1 : sx;
                    const uint32_t E = fetch32(srcC, sx);
                    const uint32_t B = fetch32(srcU, sx);
                    const uint32_t H = fetch32(srcD, sx);
                    const uint32_t D = fetch32(srcC, xl);
                    const uint32_t F = fetch32(srcC, xr);
                    uint32_t e0 = E, e1 = E, e2 = E, e3 = E;
                    if (B != H && D != F) {
                        if (D == B) e0 = D;
                        if (B == F) e1 = F;
                        if (D == H) e2 = D;
                        if (H == F) e3 = F;
                    }
                    row0[sx * 2] = e0;
                    row0[sx * 2 + 1] = e1;
                    row1[sx * 2] = e2;
                    row1[sx * 2 + 1] = e3;
                }
            }
        } else {
            for (int sy = 0; sy < srcRows; sy++) {
                const auto *src = reinterpret_cast<const uint32_t *>(data + static_cast<size_t>(sy) * pitch);
                uint32_t *firstRow = dst + static_cast<size_t>(sy * s) * buf.stride;
                uint32_t prevPx = srcCols > 0 ? fetch32(src, 0) : 0;
                uint32_t curPx = prevPx;
                for (int sx = 0; sx < srcCols; sx++) {
                    uint32_t nextPx = (sx + 1 < srcCols) ? fetch32(src, sx + 1) : curPx;
                    uint32_t px = useComposite ? blendRgbx(prevPx, curPx, nextPx) : curPx;
                    prevPx = curPx;
                    curPx = nextPx;
                    if (s == 1) {
                        firstRow[sx] = px;
                    } else if (s == 2) {
                        uint32_t *d = firstRow + sx * 2;
                        d[0] = px;
                        d[1] = lcd ? shadeRgbx(px, kGrid) : px;
                    } else if (s == 3) {
                        uint32_t *d = firstRow + sx * 3;
                        d[0] = px;
                        d[1] = px;
                        d[2] = lcd ? shadeRgbx(px, kGrid) : px;
                    } else {
                        uint32_t *d = firstRow + sx * 4;
                        d[0] = px;
                        d[1] = px;
                        d[2] = px;
                        d[3] = lcd ? shadeRgbx(px, kGrid) : px;
                    }
                }
                for (int r = 1; r < s; r++) {
                    uint32_t *subRow = firstRow + static_cast<size_t>(r) * buf.stride;
                    if (r == s - 1 && (crt || lcd)) {
                        const unsigned k = crt ? kScanline : kGrid;
                        for (int x = 0; x < outCols; x++) subRow[x] = shadeRgbx(firstRow[x], k);
                    } else {
                        memcpy(subRow, firstRow, rowBytes);
                    }
                }
            }
        }
    } else {
        auto *dst = static_cast<uint16_t *>(buf.bits);
        const bool is1555 = fmt == RETRO_PIXEL_FORMAT_0RGB1555;
        const size_t rowBytes = static_cast<size_t>(outCols) * sizeof(uint16_t);
        auto fetch16 = [&](const uint16_t *src, int sx) -> uint16_t {
            uint16_t px = is1555 ? rgb1555To565(src[sx]) : src[sx];
            if (lut) px = lut[px];
            return px;
        };
        if (scale2x) {
            for (int sy = 0; sy < srcRows; sy++) {
                const auto *srcC = reinterpret_cast<const uint16_t *>(data + static_cast<size_t>(sy) * pitch);
                const auto *srcU = reinterpret_cast<const uint16_t *>(data + static_cast<size_t>(sy > 0 ? sy - 1 : 0) * pitch);
                const auto *srcD = reinterpret_cast<const uint16_t *>(data + static_cast<size_t>(sy + 1 < srcRows ? sy + 1 : sy) * pitch);
                uint16_t *row0 = dst + static_cast<size_t>(sy * 2) * buf.stride;
                uint16_t *row1 = row0 + buf.stride;
                for (int sx = 0; sx < srcCols; sx++) {
                    const int xl = sx > 0 ? sx - 1 : 0;
                    const int xr = sx + 1 < srcCols ? sx + 1 : sx;
                    const uint16_t E = fetch16(srcC, sx);
                    const uint16_t B = fetch16(srcU, sx);
                    const uint16_t H = fetch16(srcD, sx);
                    const uint16_t D = fetch16(srcC, xl);
                    const uint16_t F = fetch16(srcC, xr);
                    uint16_t e0 = E, e1 = E, e2 = E, e3 = E;
                    if (B != H && D != F) {
                        if (D == B) e0 = D;
                        if (B == F) e1 = F;
                        if (D == H) e2 = D;
                        if (H == F) e3 = F;
                    }
                    row0[sx * 2] = e0;
                    row0[sx * 2 + 1] = e1;
                    row1[sx * 2] = e2;
                    row1[sx * 2 + 1] = e3;
                }
            }
        } else {
            for (int sy = 0; sy < srcRows; sy++) {
                const auto *src = reinterpret_cast<const uint16_t *>(data + static_cast<size_t>(sy) * pitch);
                uint16_t *firstRow = dst + static_cast<size_t>(sy * s) * buf.stride;
                if (s == 1 && !is1555 && !lut && !useComposite) {
                    memcpy(firstRow, src, rowBytes);
                } else {
                    uint16_t prevPx = srcCols > 0 ? fetch16(src, 0) : 0;
                    uint16_t curPx = prevPx;
                    for (int sx = 0; sx < srcCols; sx++) {
                        uint16_t nextPx = (sx + 1 < srcCols) ? fetch16(src, sx + 1) : curPx;
                        uint16_t px = useComposite ? blend565(prevPx, curPx, nextPx) : curPx;
                        prevPx = curPx;
                        curPx = nextPx;
                        if (s == 1) {
                            firstRow[sx] = px;
                        } else if (s == 2) {
                            uint16_t *d = firstRow + sx * 2;
                            d[0] = px;
                            d[1] = lcd ? shade565(px, kGrid) : px;
                        } else if (s == 3) {
                            uint16_t *d = firstRow + sx * 3;
                            d[0] = px;
                            d[1] = px;
                            d[2] = lcd ? shade565(px, kGrid) : px;
                        } else {
                            uint16_t *d = firstRow + sx * 4;
                            d[0] = px;
                            d[1] = px;
                            d[2] = px;
                            d[3] = lcd ? shade565(px, kGrid) : px;
                        }
                    }
                }
                for (int r = 1; r < s; r++) {
                    uint16_t *subRow = firstRow + static_cast<size_t>(r) * buf.stride;
                    if (r == s - 1 && (crt || lcd)) {
                        const unsigned k = crt ? kScanline : kGrid;
                        for (int x = 0; x < outCols; x++) subRow[x] = shade565(firstRow[x], k);
                    } else {
                        memcpy(subRow, firstRow, rowBytes);
                    }
                }
            }
        }
    }
    ANativeWindow_unlockAndPost(window_);
}

}  // namespace rh
