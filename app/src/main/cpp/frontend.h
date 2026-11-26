#pragma once

#include <atomic>
#include <condition_variable>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "audio_output.h"
#include "hw_renderer.h"
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
    // Optional: not every core supports cheats.
    void (*cheat_reset)() = nullptr;
    void (*cheat_set)(unsigned, bool, const char *) = nullptr;
};

// A single libretro frontend instance. RetroHero runs each game in its own
// process, so there is exactly one of these per process.
// Rewind tuning: snapshot every N frames, cap the buffer's memory, and skip cores whose
// states are too big to snapshot cheaply.
constexpr unsigned kRewindInterval = 2;
constexpr size_t kRewindBudget = 48u << 20;
constexpr size_t kRewindMaxState = 4u << 20;
constexpr size_t kRewindMaxSlots = 1800;

class Frontend {
public:
    static Frontend &get();

    bool loadCore(const std::string &corePath, const std::string &systemDir,
                  const std::string &saveDir);
    bool loadGame(const std::string &romPath, const std::string &sramPath);
    bool loadGameFromMemory(const std::string &virtualPath, const uint8_t *data, size_t size,
                            const std::string &sramPath, const std::string &scratchDir);
    void start();
    void setPaused(bool paused);
    void setFastForward(bool enabled, float speed);
    void setSlowMotion(bool enabled);
    void setRunAhead(int frames);
    void onVsync(int64_t frameTimeNanos, float displayHz);
    void setVsyncConfig(bool vsyncLock, bool bfi120Hz);
    float coreFps() const { return static_cast<float>(fps_.load(std::memory_order_relaxed)); }
    bool isVsyncLocked() const { return vsyncActive_.load(std::memory_order_relaxed); }
    bool isBfiActive() const { return bfiActive_.load(std::memory_order_relaxed); }
    void perfStats(float &fps, float &frameMs, int &activeRunAhead) const;
    // Rewind keeps recent states in memory; holding rewind steps back through them.
    void setRewindEnabled(bool enabled);
    void setRewinding(bool rewinding);
    bool rewindAvailable() const { return rewindEnabled_.load() && !rewindTooBig_.load(); }
    bool setCheats(const std::vector<std::string> &codes);
    bool supportsCheats() const { return core_.cheat_set != nullptr && core_.cheat_reset != nullptr; }
    void reset();
    bool saveState(const std::string &path);
    bool loadState(const std::string &path);
    bool saveSram(const std::string &path);
    bool readMemory(unsigned memoryType, std::vector<uint8_t> &out, size_t maxBytes);
    void shutdown(const std::string &sramPath);

    // Multi-disc control (RETRO_ENVIRONMENT_SET_DISK_CONTROL[_EXT]_INTERFACE)
    void setInitialDisc(unsigned index);
    int discCount();
    int currentDiscIndex();
    std::vector<std::string> discLabels();
    bool swapDisc(unsigned index);

    void setOptionOverride(const std::string &key, const std::string &value);
    std::vector<CoreOption> options();

    void setButton(unsigned port, unsigned id, bool pressed);
    void setAnalog(unsigned port, unsigned index, unsigned axis, int16_t value);
    void setPointer(int16_t x, int16_t y, bool pressed);

    void setWindow(ANativeWindow *window);
    void setFilter(int filter);
    void setColorProfile(int profile);
    void frameSize(unsigned &width, unsigned &height);
    bool grabArgb(std::vector<uint32_t> &out, unsigned &width, unsigned &height);

    float aspectRatio() const { return aspect_.load(); }
    std::string lastError();

    VideoOutput &video() { return video_; }

private:
    Frontend() = default;
    void runLoop();
    void setError(const std::string &msg);
    bool environment(unsigned cmd, void *data);
    void setVariables(const retro_variable *vars);
    void setCoreOptionsV1(const retro_core_option_definition *defs);
    void setCoreOptionsV2(const retro_core_option_v2_definition *defs);
    void updateAvInfo(const retro_system_av_info &info);
    void runFrameLocked();
    void pushRewindLocked();
    bool popRewindLocked();
    void clearRewindLocked();

    static bool envCb(unsigned cmd, void *data);
    static void videoCb(const void *data, unsigned w, unsigned h, size_t pitch);
    static void audioSampleCb(int16_t l, int16_t r);
    static size_t audioBatchCb(const int16_t *data, size_t frames);
    static void inputPollCb();
    static int16_t inputStateCb(unsigned port, unsigned device, unsigned index, unsigned id);
    static void logCb(enum retro_log_level level, const char *fmt, ...);
    static uintptr_t hwGetFramebufferCb();
    static retro_proc_address_t hwGetProcAddressCb(const char *sym);

    CoreApi core_;
    std::mutex coreMutex_;  // guards every call into the core
    bool coreInitialized_ = false;
    bool gameLoaded_ = false;
    std::vector<uint8_t> romData_;
    std::string romPath_;
    std::string ephemeralRomPath_;
    std::vector<std::string> playlistPaths_;
    unsigned initialDiscIndex_ = 0;

    // Multi-disc control interface registered by the core (v0 or v1 ext).
    retro_disk_control_ext_callback diskControl_{};
    bool hasDiskControl_ = false;
    bool hasDiskControlExt_ = false;

    std::string systemDir_, saveDir_;
    int pixelFormat_ = RETRO_PIXEL_FORMAT_0RGB1555;

    std::mutex optionsMutex_;
    std::vector<CoreOption> options_;
    std::map<std::string, std::string> overrides_;
    std::atomic<bool> optionsDirty_{false};

    retro_frame_time_callback frameTimeCb_{};
    bool hasFrameTimeCb_ = false;

    // Hardware rendering state (EGL/GLES3)
    std::atomic<bool> hwRenderEnabled_{false};
    bool hwContextReady_ = false;
    retro_hw_render_callback hwRenderCb_{};
    std::atomic<unsigned> maxVideoW_{640};
    std::atomic<unsigned> maxVideoH_{480};
    std::string pendingStatePath_;
    HwRenderer hw_;

    std::atomic<double> fps_{60.0};
    std::atomic<float> aspect_{4.0f / 3.0f};
    std::atomic<bool> running_{false};
    std::atomic<bool> paused_{false};
    std::atomic<bool> fastForward_{false};
    std::atomic<float> ffSpeed_{3.0f};
    std::atomic<bool> slowMotion_{false};
    std::atomic<int> runAheadFrames_{0};
    std::atomic<int> activeRunAhead_{0};
    std::atomic<float> actualFps_{60.0f};
    std::atomic<float> frameTimeMs_{0.0f};
    bool suppressVideo_ = false;
    bool suppressAudio_ = false;
    std::vector<uint8_t> runAheadBuf_;
    std::thread thread_;

    // Hardware Choreographer Vsync pacing & 120 Hz OLED BFI
    std::mutex vsyncMutex_;
    std::condition_variable vsyncCv_;
    uint64_t vsyncSeq_ = 0;
    int64_t lastVsyncNs_ = 0;
    std::atomic<float> displayHz_{60.0f};
    std::atomic<bool> vsyncLockEnabled_{true};
    std::atomic<bool> bfiEnabled_{false};
    std::atomic<bool> vsyncActive_{false};
    std::atomic<bool> bfiActive_{false};

    // Rewind ring buffer (only touched with coreMutex_ held).
    std::atomic<bool> rewindEnabled_{false};
    std::atomic<bool> rewinding_{false};
    std::atomic<bool> rewindTooBig_{false};
    std::vector<std::vector<uint8_t>> ring_;
    size_t ringHead_ = 0, ringCount_ = 0, ringStateSize_ = 0;
    unsigned rewindTick_ = 0;

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
