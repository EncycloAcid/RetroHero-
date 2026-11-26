#pragma once

#include <aaudio/AAudio.h>
#include <atomic>
#include <cstdint>
#include <mutex>
#include <vector>

namespace rh {

// Lock-free single-producer/single-consumer power-of-two ring buffer built on AAudio.
// Batches atomic index updates once per frame and uses 2-chunk memcpy in the audio callback.
class AudioOutput {
public:
    ~AudioOutput();

    bool start();
    void stop();
    void setInputRate(double rate);
    void write(const int16_t *frames, size_t count);
    void clear();

private:
    static aaudio_data_callback_result_t dataCallback(AAudioStream *stream, void *user,
                                                      void *audioData, int32_t numFrames);
    static void errorCallback(AAudioStream *stream, void *user, aaudio_result_t error);
    bool openStream();
    void closeStream();

    std::mutex streamMutex_;
    AAudioStream *stream_ = nullptr;
    std::atomic<bool> wantRunning_{false};

    int32_t deviceRate_ = 48000;
    double inputRate_ = 44100.0;

    std::vector<int16_t> ring_;
    size_t capacity_ = 0;  // power of two, in stereo frames
    size_t mask_ = 0;
    std::atomic<uint64_t> readPos_{0};
    std::atomic<uint64_t> writePos_{0};

    // Resampler state (producer side only).
    double t_ = 0.0;
    int16_t prevL_ = 0, prevR_ = 0;
};

}  // namespace rh
