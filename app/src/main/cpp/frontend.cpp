#include "frontend.h"

#include <dlfcn.h>

#include <chrono>
#include <cstdarg>
#include <cstdio>
#include <cstring>
#include <fstream>

#include "log.h"

namespace rh {

namespace {

bool readFile(const std::string &path, std::vector<uint8_t> &out) {
    std::ifstream f(path, std::ios::binary | std::ios::ate);
    if (!f) return false;
    std::streamsize size = f.tellg();
    if (size < 0) return false;
    f.seekg(0);
    out.resize(static_cast<size_t>(size));
    return size == 0 || static_cast<bool>(f.read(reinterpret_cast<char *>(out.data()), size));
}

bool writeFile(const std::string &path, const void *data, size_t size) {
    std::string tmp = path + ".tmp";
    {
        std::ofstream f(tmp, std::ios::binary | std::ios::trunc);
        if (!f) return false;
        f.write(static_cast<const char *>(data), static_cast<std::streamsize>(size));
        if (!f) return false;
    }
    return std::rename(tmp.c_str(), path.c_str()) == 0;
}

std::string trim(const std::string &s) {
    size_t a = s.find_first_not_of(" \t");
    if (a == std::string::npos) return "";
    size_t b = s.find_last_not_of(" \t");
    return s.substr(a, b - a + 1);
}

template <typename T>
bool loadSym(void *handle, const char *name, T &out) {
    out = reinterpret_cast<T>(dlsym(handle, name));
    return out != nullptr;
}

}  // namespace

Frontend &Frontend::get() {
    static Frontend instance;
    return instance;
}

void Frontend::setError(const std::string &msg) {
    LOGE("%s", msg.c_str());
    std::lock_guard<std::mutex> lk(errorMutex_);
    lastError_ = msg;
}

std::string Frontend::lastError() {
    std::lock_guard<std::mutex> lk(errorMutex_);
    return lastError_;
}

bool Frontend::loadCore(const std::string &corePath, const std::string &systemDir,
                        const std::string &saveDir) {
    systemDir_ = systemDir;
    saveDir_ = saveDir;

    void *h = dlopen(corePath.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (!h) {
        setError(std::string("Could not load core: ") + dlerror());
        return false;
    }
    core_.handle = h;
    bool ok = loadSym(h, "retro_init", core_.init) && loadSym(h, "retro_deinit", core_.deinit) &&
              loadSym(h, "retro_api_version", core_.api_version) &&
              loadSym(h, "retro_get_system_info", core_.get_system_info) &&
              loadSym(h, "retro_get_system_av_info", core_.get_system_av_info) &&
              loadSym(h, "retro_set_environment", core_.set_environment) &&
              loadSym(h, "retro_set_video_refresh", core_.set_video_refresh) &&
              loadSym(h, "retro_set_audio_sample", core_.set_audio_sample) &&
              loadSym(h, "retro_set_audio_sample_batch", core_.set_audio_sample_batch) &&
              loadSym(h, "retro_set_input_poll", core_.set_input_poll) &&
              loadSym(h, "retro_set_input_state", core_.set_input_state) &&
              loadSym(h, "retro_reset", core_.reset) && loadSym(h, "retro_run", core_.run) &&
              loadSym(h, "retro_serialize_size", core_.serialize_size) &&
              loadSym(h, "retro_serialize", core_.serialize) &&
              loadSym(h, "retro_unserialize", core_.unserialize) &&
              loadSym(h, "retro_load_game", core_.load_game) &&
              loadSym(h, "retro_unload_game", core_.unload_game) &&
              loadSym(h, "retro_get_memory_data", core_.get_memory_data) &&
              loadSym(h, "retro_get_memory_size", core_.get_memory_size);
    // Optional symbols.
    loadSym(h, "retro_cheat_reset", core_.cheat_reset);
    loadSym(h, "retro_cheat_set", core_.cheat_set);
    if (!ok) {
        setError("Core is missing required libretro symbols");
        dlclose(h);
        core_ = CoreApi{};
        return false;
    }
    if (core_.api_version() != RETRO_API_VERSION) {
        setError("Core uses an incompatible libretro API version");
        dlclose(h);
        core_ = CoreApi{};
        return false;
    }

    std::lock_guard<std::mutex> lk(coreMutex_);
    core_.set_environment(&Frontend::envCb);
    core_.init();
    coreInitialized_ = true;
    core_.set_video_refresh(&Frontend::videoCb);
    core_.set_audio_sample(&Frontend::audioSampleCb);
    core_.set_audio_sample_batch(&Frontend::audioBatchCb);
    core_.set_input_poll(&Frontend::inputPollCb);
    core_.set_input_state(&Frontend::inputStateCb);

    retro_system_info info{};
    core_.get_system_info(&info);
    LOGI("Loaded core %s %s", info.library_name ? info.library_name : "?",
         info.library_version ? info.library_version : "");
    return true;
}

bool Frontend::loadGame(const std::string &romPath, const std::string &sramPath) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!coreInitialized_) {
        setError("No core loaded");
        return false;
    }
    retro_system_info sys{};
    core_.get_system_info(&sys);

    romPath_ = romPath;
    retro_game_info game{};
    game.path = romPath_.c_str();
    if (!sys.need_fullpath) {
        if (!readFile(romPath, romData_)) {
            setError("Could not read game file");
            return false;
        }
        game.data = romData_.data();
        game.size = romData_.size();
    }
    if (!core_.load_game(&game)) {
        setError("The core could not load this game. It may be unsupported, corrupt, or need a BIOS file.");
        return false;
    }
    gameLoaded_ = true;

    retro_system_av_info av{};
    core_.get_system_av_info(&av);
    updateAvInfo(av);

    if (!sramPath.empty()) {
        size_t size = core_.get_memory_size(RETRO_MEMORY_SAVE_RAM);
        void *mem = core_.get_memory_data(RETRO_MEMORY_SAVE_RAM);
        std::vector<uint8_t> sram;
        if (size > 0 && mem && readFile(sramPath, sram)) {
            memcpy(mem, sram.data(), sram.size() < size ? sram.size() : size);
            LOGI("Loaded %zu bytes of save RAM", sram.size());
        }
    }
    return true;
}

void Frontend::updateAvInfo(const retro_system_av_info &av) {
    if (av.timing.fps > 1.0) fps_ = av.timing.fps;
    if (av.timing.sample_rate > 1.0) audio_.setInputRate(av.timing.sample_rate);
    float ar = av.geometry.aspect_ratio;
    if (ar <= 0.0f && av.geometry.base_height > 0)
        ar = static_cast<float>(av.geometry.base_width) / av.geometry.base_height;
    if (ar > 0.0f) aspect_ = ar;
    LOGI("AV info: %ux%u fps=%.3f rate=%.1f aspect=%.3f", av.geometry.base_width,
         av.geometry.base_height, av.timing.fps, av.timing.sample_rate, ar);
}

void Frontend::start() {
    if (running_ || !gameLoaded_) return;
    running_ = true;
    audio_.start();
    thread_ = std::thread(&Frontend::runLoop, this);
}

void Frontend::runLoop() {
    using clock = std::chrono::steady_clock;
    auto next = clock::now();
    auto lastFrame = clock::now();
    while (running_) {
        if (paused_) {
            std::this_thread::sleep_for(std::chrono::milliseconds(16));
            next = clock::now();
            lastFrame = next;
            continue;
        }
        {
            std::lock_guard<std::mutex> lk(coreMutex_);
            if (hasFrameTimeCb_ && frameTimeCb_.callback) {
                auto now = clock::now();
                auto usec = std::chrono::duration_cast<std::chrono::microseconds>(now - lastFrame).count();
                lastFrame = now;
                frameTimeCb_.callback(static_cast<retro_usec_t>(usec));
            }
            runFrameLocked();
        }
        if (!audioStaging_.empty()) {
            if (!fastForward_ && !rewinding_) audio_.write(audioStaging_.data(), audioStaging_.size() / 2);
            audioStaging_.clear();
        }

        double speed = fastForward_ ? ffSpeed_.load() : 1.0;
        auto frame = std::chrono::duration_cast<clock::duration>(
                std::chrono::duration<double>(1.0 / (fps_.load() * speed)));
        next += frame;
        auto now = clock::now();
        if (next < now - std::chrono::milliseconds(100)) {
            next = now;  // fell far behind (e.g. a slow frame); don't try to catch up
        } else if (next > now) {
            std::this_thread::sleep_until(next);
        }
    }
}

void Frontend::runFrameLocked() {
    if (rewinding_ && rewindEnabled_) {
        // Step back one snapshot and run a single frame from it so the screen updates.
        // With nothing left to rewind to, hold on the current frame.
        if (popRewindLocked()) core_.run();
        else video_.redraw();
        return;
    }
    core_.run();
    if (rewindEnabled_ && !rewindTooBig_ && ++rewindTick_ % kRewindInterval == 0) pushRewindLocked();
}

void Frontend::pushRewindLocked() {
    size_t size = core_.serialize_size();
    if (size == 0 || size > kRewindMaxState) {
        rewindTooBig_ = true;
        clearRewindLocked();
        ring_.clear();
        ring_.shrink_to_fit();
        return;
    }
    if (size != ringStateSize_ || ring_.empty()) {
        // (Re)allocate the ring for this state size.
        size_t slots = kRewindBudget / size;
        if (slots > kRewindMaxSlots) slots = kRewindMaxSlots;
        if (slots < 2) {
            rewindTooBig_ = true;
            return;
        }
        ring_.assign(slots, std::vector<uint8_t>());
        ringStateSize_ = size;
        ringHead_ = ringCount_ = 0;
    }
    auto &slot = ring_[ringHead_];
    if (slot.size() != size) slot.resize(size);
    if (!core_.serialize(slot.data(), size)) return;
    ringHead_ = (ringHead_ + 1) % ring_.size();
    if (ringCount_ < ring_.size()) ringCount_++;
}

bool Frontend::popRewindLocked() {
    if (ringCount_ == 0 || ring_.empty()) return false;
    ringHead_ = (ringHead_ + ring_.size() - 1) % ring_.size();
    ringCount_--;
    auto &slot = ring_[ringHead_];
    return core_.unserialize(slot.data(), slot.size());
}

void Frontend::clearRewindLocked() {
    ringHead_ = ringCount_ = 0;
    rewindTick_ = 0;
}

void Frontend::setRewindEnabled(bool enabled) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    rewindEnabled_ = enabled;
    if (!enabled) {
        rewinding_ = false;
        clearRewindLocked();
        ring_.clear();
        ring_.shrink_to_fit();
        ringStateSize_ = 0;
    }
}

void Frontend::setRewinding(bool rewinding) { rewinding_ = rewinding && rewindEnabled_; }

bool Frontend::setCheats(const std::vector<std::string> &codes) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_ || !core_.cheat_reset || !core_.cheat_set) return false;
    core_.cheat_reset();
    for (size_t i = 0; i < codes.size(); i++) core_.cheat_set(static_cast<unsigned>(i), true, codes[i].c_str());
    return true;
}

void Frontend::setPaused(bool paused) {
    paused_ = paused;
    if (!running_) return;
    if (paused) {
        audio_.stop();
    } else {
        audio_.clear();
        audio_.start();
    }
}

void Frontend::setFastForward(bool enabled, float speed) {
    ffSpeed_ = speed < 1.0f ? 1.0f : speed;
    fastForward_ = enabled;
}

void Frontend::reset() {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (gameLoaded_) core_.reset();
    clearRewindLocked();
}

bool Frontend::saveState(const std::string &path) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_) return false;
    size_t size = core_.serialize_size();
    if (size == 0) {
        setError("This core does not support save states");
        return false;
    }
    std::vector<uint8_t> buf(size);
    if (!core_.serialize(buf.data(), size)) {
        setError("Saving state failed");
        return false;
    }
    return writeFile(path, buf.data(), size);
}

bool Frontend::loadState(const std::string &path) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_) return false;
    std::vector<uint8_t> buf;
    if (!readFile(path, buf)) return false;
    if (!core_.unserialize(buf.data(), buf.size())) {
        setError("Loading state failed");
        return false;
    }
    clearRewindLocked();
    return true;
}

bool Frontend::saveSram(const std::string &path) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_ || path.empty()) return false;
    size_t size = core_.get_memory_size(RETRO_MEMORY_SAVE_RAM);
    void *mem = core_.get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (size == 0 || !mem) return false;
    return writeFile(path, mem, size);
}

void Frontend::shutdown(const std::string &sramPath) {
    running_ = false;
    if (thread_.joinable()) thread_.join();
    audio_.stop();
    saveSram(sramPath);
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (gameLoaded_) core_.unload_game();
    gameLoaded_ = false;
    if (coreInitialized_) core_.deinit();
    coreInitialized_ = false;
    // The core library is intentionally not dlclose()d: many cores do not
    // tolerate being unloaded, and the game process exits right after.
}

void Frontend::setOptionOverride(const std::string &key, const std::string &value) {
    std::lock_guard<std::mutex> lk(optionsMutex_);
    overrides_[key] = value;
    for (auto &opt : options_) {
        if (opt.key == key) opt.current = value;
    }
    optionsDirty_ = true;
}

std::vector<CoreOption> Frontend::options() {
    std::lock_guard<std::mutex> lk(optionsMutex_);
    return options_;
}

void Frontend::setVariables(const retro_variable *vars) {
    std::lock_guard<std::mutex> lk(optionsMutex_);
    options_.clear();
    for (const retro_variable *v = vars; v && v->key; v++) {
        CoreOption opt;
        opt.key = v->key;
        std::string spec = v->value ? v->value : "";
        size_t semi = spec.find(';');
        opt.description = trim(semi == std::string::npos ? opt.key : spec.substr(0, semi));
        std::string list = semi == std::string::npos ? spec : spec.substr(semi + 1);
        size_t start = 0;
        while (start <= list.size()) {
            size_t bar = list.find('|', start);
            std::string val = trim(list.substr(start, bar == std::string::npos ? std::string::npos : bar - start));
            if (!val.empty()) opt.values.push_back(val);
            if (bar == std::string::npos) break;
            start = bar + 1;
        }
        auto it = overrides_.find(opt.key);
        if (it != overrides_.end()) {
            opt.current = it->second;
        } else {
            opt.current = opt.values.empty() ? "" : opt.values.front();
        }
        options_.push_back(std::move(opt));
    }
}

void Frontend::setButton(unsigned port, unsigned id, bool pressed) {
    if (port >= 4 || id >= 32) return;
    if (pressed)
        buttons_[port].fetch_or(1u << id);
    else
        buttons_[port].fetch_and(~(1u << id));
}

void Frontend::setAnalog(unsigned port, unsigned index, unsigned axis, int16_t value) {
    if (port >= 4 || index >= 2 || axis >= 2) return;
    analog_[port][index][axis] = value;
}

void Frontend::setPointer(int16_t x, int16_t y, bool pressed) {
    pointerX_ = x;
    pointerY_ = y;
    pointerDown_ = pressed;
}

// ---- libretro callbacks -------------------------------------------------

bool Frontend::envCb(unsigned cmd, void *data) { return get().environment(cmd, data); }

bool Frontend::environment(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            static_cast<retro_log_callback *>(data)->log = &Frontend::logCb;
            return true;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
            *static_cast<const char **>(data) = systemDir_.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            *static_cast<const char **>(data) = saveDir_.c_str();
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            auto fmt = *static_cast<const retro_pixel_format *>(data);
            if (fmt > RETRO_PIXEL_FORMAT_RGB565) return false;
            pixelFormat_ = fmt;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *static_cast<bool *>(data) = true;
            return true;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return true;
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            // Version 0: cores fall back to the simple SET_VARIABLES interface.
            *static_cast<unsigned *>(data) = 0;
            return true;
        case RETRO_ENVIRONMENT_SET_VARIABLES:
            setVariables(static_cast<const retro_variable *>(data));
            return true;
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            auto *var = static_cast<retro_variable *>(data);
            if (!var || !var->key) return false;
            std::lock_guard<std::mutex> lk(optionsMutex_);
            for (const auto &opt : options_) {
                if (opt.key == var->key) {
                    var->value = opt.current.c_str();
                    return true;
                }
            }
            var->value = nullptr;
            return false;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *static_cast<bool *>(data) = optionsDirty_.exchange(false);
            return true;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
            return true;
        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            *static_cast<unsigned *>(data) = RETRO_LANGUAGE_ENGLISH;
            return true;
        case RETRO_ENVIRONMENT_GET_USERNAME:
            *static_cast<const char **>(data) = "RetroHero";
            return true;
        case RETRO_ENVIRONMENT_SET_GEOMETRY: {
            auto *geom = static_cast<const retro_game_geometry *>(data);
            float ar = geom->aspect_ratio;
            if (ar <= 0.0f && geom->base_height > 0)
                ar = static_cast<float>(geom->base_width) / geom->base_height;
            if (ar > 0.0f) aspect_ = ar;
            return true;
        }
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
            updateAvInfo(*static_cast<const retro_system_av_info *>(data));
            return true;
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            *static_cast<int *>(data) = 1 | 2;
            return true;
        case RETRO_ENVIRONMENT_GET_FASTFORWARDING:
            *static_cast<bool *>(data) = fastForward_.load();
            return true;
        case RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK:
            frameTimeCb_ = *static_cast<const retro_frame_time_callback *>(data);
            hasFrameTimeCb_ = true;
            return true;
        case RETRO_ENVIRONMENT_SET_MESSAGE: {
            auto *msg = static_cast<const retro_message *>(data);
            if (msg && msg->msg) LOGI("Core message: %s", msg->msg);
            return true;
        }
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
        case RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO:
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
        case RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE:
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
            return true;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            // Only software-rendered cores are supported for now.
            LOGW("Core requested hardware rendering, which is not supported yet");
            return false;
        default:
            return false;
    }
}

void Frontend::videoCb(const void *data, unsigned w, unsigned h, size_t pitch) {
    if (!data || data == RETRO_HW_FRAME_BUFFER_VALID) return;
    Frontend &f = get();
    f.video_.present(data, w, h, pitch, f.pixelFormat_);
}

void Frontend::audioSampleCb(int16_t l, int16_t r) {
    auto &st = get().audioStaging_;
    st.push_back(l);
    st.push_back(r);
}

size_t Frontend::audioBatchCb(const int16_t *data, size_t frames) {
    auto &st = get().audioStaging_;
    st.insert(st.end(), data, data + frames * 2);
    return frames;
}

void Frontend::inputPollCb() {}

int16_t Frontend::inputStateCb(unsigned port, unsigned device, unsigned index, unsigned id) {
    Frontend &f = get();
    if (port >= 4) return 0;
    switch (device & RETRO_DEVICE_MASK) {
        case RETRO_DEVICE_JOYPAD: {
            uint32_t b = f.buttons_[port].load();
            if (id == RETRO_DEVICE_ID_JOYPAD_MASK) return static_cast<int16_t>(b & 0xFFFF);
            return id < 16 ? static_cast<int16_t>((b >> id) & 1) : 0;
        }
        case RETRO_DEVICE_ANALOG:
            if (index == RETRO_DEVICE_INDEX_ANALOG_BUTTON)
                return id < 16 && ((f.buttons_[port].load() >> id) & 1) ? 0x7FFF : 0;
            if (index < 2 && id < 2) return f.analog_[port][index][id].load();
            return 0;
        case RETRO_DEVICE_POINTER:
            if (port != 0 || index != 0) return 0;
            switch (id) {
                case RETRO_DEVICE_ID_POINTER_X: return f.pointerX_.load();
                case RETRO_DEVICE_ID_POINTER_Y: return f.pointerY_.load();
                case RETRO_DEVICE_ID_POINTER_PRESSED: return f.pointerDown_.load() ? 1 : 0;
                case RETRO_DEVICE_ID_POINTER_COUNT: return f.pointerDown_.load() ? 1 : 0;
                default: return 0;
            }
        default:
            return 0;
    }
}

void Frontend::logCb(enum retro_log_level level, const char *fmt, ...) {
    int prio = ANDROID_LOG_DEBUG;
    switch (level) {
        case RETRO_LOG_INFO: prio = ANDROID_LOG_INFO; break;
        case RETRO_LOG_WARN: prio = ANDROID_LOG_WARN; break;
        case RETRO_LOG_ERROR: prio = ANDROID_LOG_ERROR; break;
        default: break;
    }
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(prio, "RetroHeroCore", fmt, args);
    va_end(args);
}

}  // namespace rh
