#include "audio_output.h"

#include <algorithm>
#include <cstring>
#include <thread>

#include "log.h"

namespace rh {

namespace {

size_t nextPow2(size_t v) {
    size_t p = 1024;
    while (p < v) p <<= 1;
    return p;
}

}  // namespace

AudioOutput::~AudioOutput() { stop(); }

bool AudioOutput::openStream() {
    AAudioStreamBuilder *builder = nullptr;
    if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK) return false;
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setChannelCount(builder, 2);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setDataCallback(builder, &AudioOutput::dataCallback, this);
    AAudioStreamBuilder_setErrorCallback(builder, &AudioOutput::errorCallback, this);

    aaudio_result_t r = AAudioStreamBuilder_openStream(builder, &stream_);
    AAudioStreamBuilder_delete(builder);
    if (r != AAUDIO_OK) {
        LOGE("AAudio open failed: %s", AAudio_convertResultToText(r));
        stream_ = nullptr;
        return false;
    }
    deviceRate_ = AAudioStream_getSampleRate(stream_);
    if (deviceRate_ <= 0) deviceRate_ = 48000;

    // Power-of-two ring buffer (~170 ms at 48 kHz = 8192 frames) for branchless bitmask indexing.
    size_t cap = nextPow2(static_cast<size_t>(deviceRate_ * 0.12));
    if (cap != capacity_) {
        capacity_ = cap;
        mask_ = cap - 1;
        ring_.assign(capacity_ * 2, 0);
        readPos_.store(0, std::memory_order_relaxed);
        writePos_.store(0, std::memory_order_relaxed);
    }
    int32_t burst = AAudioStream_getFramesPerBurst(stream_);
    if (burst > 0) AAudioStream_setBufferSizeInFrames(stream_, burst * 2);

    r = AAudioStream_requestStart(stream_);
    if (r != AAUDIO_OK) {
        LOGE("AAudio start failed: %s", AAudio_convertResultToText(r));
        AAudioStream_close(stream_);
        stream_ = nullptr;
        return false;
    }
    LOGI("Audio started at %d Hz (ring=%zu frames)", deviceRate_, capacity_);
    return true;
}

void AudioOutput::closeStream() {
    if (stream_) {
        AAudioStream_requestStop(stream_);
        AAudioStream_close(stream_);
        stream_ = nullptr;
    }
}

bool AudioOutput::start() {
    std::lock_guard<std::mutex> lk(streamMutex_);
    wantRunning_ = true;
    if (stream_) return true;
    return openStream();
}

void AudioOutput::stop() {
    std::lock_guard<std::mutex> lk(streamMutex_);
    wantRunning_ = false;
    closeStream();
}

void AudioOutput::setInputRate(double rate) {
    if (rate > 0) inputRate_ = rate;
}

void AudioOutput::clear() {
    readPos_.store(writePos_.load(std::memory_order_acquire), std::memory_order_release);
}

// Batched lock-free write: performs ONE atomic load/store per frame instead of per sample.
void AudioOutput::write(const int16_t *frames, size_t count) {
    if (capacity_ == 0 || !stream_ || count == 0) return;
    uint64_t w = writePos_.load(std::memory_order_relaxed);
    const uint64_t rd = readPos_.load(std::memory_order_acquire);
    const uint64_t fill = w - rd;
    const double fillRatio = static_cast<double>(fill) / static_cast<double>(capacity_);
    // Dynamic rate control: gently nudge resampling ratio to keep ring buffer centered.
    const double step = (inputRate_ / deviceRate_) * (1.0 + 0.005 * (2.0 * fillRatio - 1.0));
    const size_t mask = mask_;
    int16_t *ring = ring_.data();

    for (size_t i = 0; i < count; i++) {
        const int16_t l = frames[i * 2];
        const int16_t r = frames[i * 2 + 1];
        const double dl = static_cast<double>(l - prevL_);
        const double dr = static_cast<double>(r - prevR_);
        while (t_ < 1.0) {
            if (w - rd < capacity_) {
                const size_t idx = (w & mask) * 2;
                ring[idx] = static_cast<int16_t>(prevL_ + dl * t_);
                ring[idx + 1] = static_cast<int16_t>(prevR_ + dr * t_);
                w++;
            }
            t_ += step;
        }
        t_ -= 1.0;
        prevL_ = l;
        prevR_ = r;
    }
    writePos_.store(w, std::memory_order_release);
}

aaudio_data_callback_result_t AudioOutput::dataCallback(AAudioStream *, void *user,
                                                        void *audioData, int32_t numFrames) {
    auto *self = static_cast<AudioOutput *>(user);
    auto *out = static_cast<int16_t *>(audioData);
    const uint64_t rd = self->readPos_.load(std::memory_order_relaxed);
    const uint64_t w = self->writePos_.load(std::memory_order_acquire);
    const uint64_t avail = w - rd;
    const int32_t n = static_cast<int32_t>(std::min<uint64_t>(avail, static_cast<uint64_t>(numFrames)));
    if (n > 0) {
        const size_t start = static_cast<size_t>(rd & self->mask_);
        const size_t firstChunk = std::min<size_t>(static_cast<size_t>(n), self->capacity_ - start);
        memcpy(out, self->ring_.data() + start * 2, firstChunk * 2 * sizeof(int16_t));
        const size_t secondChunk = static_cast<size_t>(n) - firstChunk;
        if (secondChunk > 0) {
            memcpy(out + firstChunk * 2, self->ring_.data(), secondChunk * 2 * sizeof(int16_t));
        }
    }
    if (n < numFrames) {
        memset(out + static_cast<size_t>(n) * 2, 0, static_cast<size_t>(numFrames - n) * 2 * sizeof(int16_t));
    }
    self->readPos_.store(rd + static_cast<uint64_t>(n), std::memory_order_release);
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AudioOutput::errorCallback(AAudioStream *, void *user, aaudio_result_t error) {
    auto *self = static_cast<AudioOutput *>(user);
    LOGW("AAudio error: %s, restarting stream", AAudio_convertResultToText(error));
    std::thread([self]() {
        std::lock_guard<std::mutex> lk(self->streamMutex_);
        self->closeStream();
        if (self->wantRunning_) self->openStream();
    }).detach();
}

}  // namespace rh
