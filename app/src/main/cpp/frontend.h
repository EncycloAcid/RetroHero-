#pragma once

#include <atomic>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "audio_output.h"
#include "libretro.h"
#include "video_output.h"

namespace rh {

struct CoreOption {
    std::string key;
    std::string description;
    std::vector<std::string> values;
    std::string current;
};

struct CoreApi {
    void *handle = nullptr;
    void (*init)() = nullptr;
    void (*deinit)() = nullptr;
    unsigned (*api_version)() = nullptr;
    void (*get_system_info)(retro_system_info *) = nullptr;
    void (*get_system_av_info)(retro_system_av_info *) = nullptr;
    void (*set_environment)(retro_environment_t) = nullptr;
    void (*set_video_refresh)(retro_video_refresh_t) = nullptr;
    void (*set_audio_sample)(retro_audio_sample_t) = nullptr;
    void (*set_audio_sample_batch)(retro_audio_sample_batch_t) = nullptr;
    void (*set_input_poll)(retro_input_poll_t) = nullptr;
    void (*set_input_state)(retro_input_state_t) = nullptr;
    void (*reset)() = nullptr;
    void (*run)() = nullptr;
    size_t (*serialize_size)() = nullptr;
    bool (*serialize)(void *, size_t) = nullptr;
    bool (*unserialize)(const void *, size_t) = nullptr;
    bool (*load_game)(const retro_game_info *) = nullptr;
    void (*unload_game)() = nullptr;
    void *(*get_memory_data)(unsigned) = nullptr;
    size_t (*get_memory_size)(unsigned) = nullptr;
};

// A single libretro frontend instance. RetroHero runs each game in its own
// process, so there is exactly one of these per process.
class Frontend {
public:
    static Frontend &get();

    bool loadCore(const std::string &corePath, const std::string &systemDir,
                  const std::string &saveDir);
    bool loadGame(const std::string &romPath, const std::string &sramPath);
    void start();
    void setPaused(bool paused);
    void setFastForward(bool enabled, float speed);
    void reset();
    bool saveState(const std::string &path);
    bool loadState(const std::string &path);
    bool saveSram(const std::string &path);
    void shutdown(const std::string &sramPath);

    void setOptionOverride(const std::string &key, const std::string &value);
    std::vector<CoreOption> options();

    void setButton(unsigned port, unsigned id, bool pressed);
    void setAnalog(unsigned port, unsigned index, unsigned axis, int16_t value);
    void setPointer(int16_t x, int16_t y, bool pressed);

    float aspectRatio() const { return aspect_.load(); }
    std::string lastError();

    VideoOutput &video() { return video_; }

private:
    Frontend() = default;
    void runLoop();
    void setError(const std::string &msg);
    bool environment(unsigned cmd, void *data);
    void setVariables(const retro_variable *vars);
    void updateAvInfo(const retro_system_av_info &info);

    static bool envCb(unsigned cmd, void *data);
    static void videoCb(const void *data, unsigned w, unsigned h, size_t pitch);
    static void audioSampleCb(int16_t l, int16_t r);
    static size_t audioBatchCb(const int16_t *data, size_t frames);
    static void inputPollCb();
    static int16_t inputStateCb(unsigned port, unsigned device, unsigned index, unsigned id);
    static void logCb(enum retro_log_level level, const char *fmt, ...);

    CoreApi core_;
    std::mutex coreMutex_;  // guards every call into the core
    bool coreInitialized_ = false;
    bool gameLoaded_ = false;
    std::vector<uint8_t> romData_;
    std::string romPath_;

    std::string systemDir_, saveDir_;
    int pixelFormat_ = RETRO_PIXEL_FORMAT_0RGB1555;

    std::mutex optionsMutex_;
    std::vector<CoreOption> options_;
    std::map<std::string, std::string> overrides_;
    std::atomic<bool> optionsDirty_{false};

    retro_frame_time_callback frameTimeCb_{};
    bool hasFrameTimeCb_ = false;

    std::atomic<double> fps_{60.0};
    std::atomic<float> aspect_{4.0f / 3.0f};
    std::atomic<bool> running_{false};
    std::atomic<bool> paused_{false};
    std::atomic<bool> fastForward_{false};
    std::atomic<float> ffSpeed_{3.0f};
    std::thread thread_;

    std::atomic<uint32_t> buttons_[4]{};
    std::atomic<int16_t> analog_[4][2][2]{};
    std::atomic<int16_t> pointerX_{0}, pointerY_{0};
    std::atomic<bool> pointerDown_{false};

    std::vector<int16_t> audioStaging_;
    AudioOutput audio_;
    VideoOutput video_;

    std::mutex errorMutex_;
    std::string lastError_;
};

}  // namespace rh
