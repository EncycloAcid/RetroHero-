#pragma once

#include <aaudio/AAudio.h>
#include <atomic>
#include <cstdint>
#include <mutex>
#include <vector>

namespace rh {

// Lock-free single-producer/single-consumer audio output built on AAudio.
// The emulation thread pushes interleaved stereo int16 frames at the core's
// sample rate; they are resampled (linear + dynamic rate control) to the
// device rate so the ring buffer hovers around half full.
class AudioOutput {
public:
    ~AudioOutput();

    bool start();
    void stop();
    void setInputRate(double rate);
    void write(const int16_t *frames, size_t count);
    void clear();
    /** Returns and resets the peak audio-haptic intensity [0..1] detected since the last call. */
    float consumeHapticEnergy();

private:
    static aaudio_data_callback_result_t dataCallback(AAudioStream *stream, void *user,
                                                      void *audioData, int32_t numFrames);
    static void errorCallback(AAudioStream *stream, void *user, aaudio_result_t error);
    bool openStream();
    void closeStream();
    void push(int16_t l, int16_t r);
    void analyzeHaptics(const int16_t *frames, size_t count);

    std::mutex streamMutex_;
    AAudioStream *stream_ = nullptr;
    std::atomic<bool> wantRunning_{false};

    int32_t deviceRate_ = 48000;
    double inputRate_ = 44100.0;

    std::vector<int16_t> ring_;
    size_t capacity_ = 0;  // in frames
    std::atomic<uint64_t> readPos_{0};
    std::atomic<uint64_t> writePos_{0};

    // Resampler state (producer side only).
    double t_ = 0.0;
    int16_t prevL_ = 0, prevR_ = 0;

    // Audio-driven haptic analyzer state (producer side only, lock-free output).
    float lpState_ = 0.0f;
    float envFast_ = 0.0f;
    float envFloor_ = 0.02f;
    std::atomic<float> hapticPeak_{0.0f};
};

}  // namespace rh
