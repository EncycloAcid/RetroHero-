#include "audio_output.h"

#include <cstring>
#include <thread>

#include "log.h"

namespace rh {

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

    // ~120 ms of buffering; resampler targets half of that.
    size_t cap = static_cast<size_t>(deviceRate_ * 0.12);
    if (cap != capacity_) {
        capacity_ = cap;
        ring_.assign(capacity_ * 2, 0);
        readPos_ = 0;
        writePos_ = 0;
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
    LOGI("Audio started at %d Hz", deviceRate_);
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

void AudioOutput::clear() { readPos_.store(writePos_.load()); }

void AudioOutput::push(int16_t l, int16_t r) {
    uint64_t w = writePos_.load(std::memory_order_relaxed);
    uint64_t rd = readPos_.load(std::memory_order_acquire);
    if (w - rd >= capacity_) return;  // overflow: drop
    size_t idx = (w % capacity_) * 2;
    ring_[idx] = l;
    ring_[idx + 1] = r;
    writePos_.store(w + 1, std::memory_order_release);
}

void AudioOutput::write(const int16_t *frames, size_t count) {
    if (capacity_ == 0 || !stream_) return;
    uint64_t fill = writePos_.load() - readPos_.load();
    double fillRatio = static_cast<double>(fill) / static_cast<double>(capacity_);
    // Dynamic rate control: nudge the ratio by up to 0.5% to keep the buffer half full.
    double step = (inputRate_ / deviceRate_) * (1.0 + 0.005 * (2.0 * fillRatio - 1.0));

    for (size_t i = 0; i < count; i++) {
        int16_t l = frames[i * 2];
        int16_t r = frames[i * 2 + 1];
        while (t_ < 1.0) {
            push(static_cast<int16_t>(prevL_ + (l - prevL_) * t_),
                 static_cast<int16_t>(prevR_ + (r - prevR_) * t_));
            t_ += step;
        }
        t_ -= 1.0;
        prevL_ = l;
        prevR_ = r;
    }
}

aaudio_data_callback_result_t AudioOutput::dataCallback(AAudioStream *, void *user,
                                                        void *audioData, int32_t numFrames) {
    auto *self = static_cast<AudioOutput *>(user);
    auto *out = static_cast<int16_t *>(audioData);
    uint64_t rd = self->readPos_.load(std::memory_order_relaxed);
    uint64_t w = self->writePos_.load(std::memory_order_acquire);
    uint64_t avail = w - rd;
    int32_t n = static_cast<int32_t>(avail < static_cast<uint64_t>(numFrames) ? avail : numFrames);
    for (int32_t i = 0; i < n; i++) {
        size_t idx = ((rd + i) % self->capacity_) * 2;
        out[i * 2] = self->ring_[idx];
        out[i * 2 + 1] = self->ring_[idx + 1];
    }
    if (n < numFrames) memset(out + n * 2, 0, (numFrames - n) * 2 * sizeof(int16_t));
    self->readPos_.store(rd + n, std::memory_order_release);
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AudioOutput::errorCallback(AAudioStream *, void *user, aaudio_result_t error) {
    auto *self = static_cast<AudioOutput *>(user);
    LOGW("AAudio error: %s, restarting stream", AAudio_convertResultToText(error));
    // Streams can't be reopened from the callback thread.
    std::thread([self]() {
        std::lock_guard<std::mutex> lk(self->streamMutex_);
        self->closeStream();
        if (self->wantRunning_) self->openStream();
    }).detach();
}

}  // namespace rh
