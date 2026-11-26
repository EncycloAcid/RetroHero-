#include "frontend.h"

#include <dlfcn.h>
#include <pthread.h>
#include <sys/resource.h>

#include <chrono>
#include <cmath>
#include <cstdarg>
#include <cstdio>
#include <cstring>
#include <fstream>

#include "builtin_cores.h"
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
    size_t a = s.find_first_not_of(" \t\r\n");
    if (a == std::string::npos) return "";
    size_t b = s.find_last_not_of(" \t\r\n");
    return s.substr(a, b - a + 1);
}

bool endsWithIgnoreCase(const std::string &s, const char *suffix) {
    size_t slen = std::strlen(suffix);
    if (s.size() < slen) return false;
    for (size_t i = 0; i < slen; i++) {
        char a = s[s.size() - slen + i];
        char b = suffix[i];
        if (a >= 'A' && a <= 'Z') a = static_cast<char>(a - 'A' + 'a');
        if (b >= 'A' && b <= 'Z') b = static_cast<char>(b - 'A' + 'a');
        if (a != b) return false;
    }
    return true;
}

std::string baseFileName(const std::string &path) {
    size_t slash = path.find_last_of("/\\");
    std::string name = (slash == std::string::npos) ? path : path.substr(slash + 1);
    size_t dot = name.find_last_of('.');
    return (dot == std::string::npos) ? name : name.substr(0, dot);
}

std::vector<std::string> parseM3uEntries(const std::string &m3uPath) {
    std::vector<std::string> entries;
    if (!endsWithIgnoreCase(m3uPath, ".m3u")) return entries;
    std::ifstream f(m3uPath);
    if (!f) return entries;
    size_t slash = m3uPath.find_last_of('/');
    std::string dir = (slash == std::string::npos) ? "" : m3uPath.substr(0, slash + 1);
    std::string line;
    while (std::getline(f, line)) {
        std::string t = trim(line);
        if (t.empty() || t[0] == '#') continue;
        if (t[0] == '/') entries.push_back(t);
        else entries.push_back(dir + t);
    }
    return entries;
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
    audioStaging_.reserve(4096);

    if (corePath == "builtin://herobox16" || corePath.find("herobox16") != std::string::npos) {
        core_ = builtin::heroBox16Api();
    } else if (corePath == "builtin://retrohero_chip8" || corePath.find("retrohero_chip8") != std::string::npos) {
        core_ = builtin::chip8Api();
    } else if (corePath == "builtin://retrohero_nes" || corePath.find("retrohero_nes") != std::string::npos) {
        core_ = builtin::heroNesApi();
    } else {
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
    }

    std::lock_guard<std::mutex> lk(coreMutex_);
    diskControl_ = {};
    hasDiskControl_ = false;
    hasDiskControlExt_ = false;
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
    playlistPaths_ = parseM3uEntries(romPath_);
    if (initialDiscIndex_ > 0 && hasDiskControlExt_ && diskControl_.set_initial_image &&
        initialDiscIndex_ < playlistPaths_.size()) {
        diskControl_.set_initial_image(initialDiscIndex_, playlistPaths_[initialDiscIndex_].c_str());
    }

    auto tryLoad = [&](const std::string &path) -> bool {
        romPath_ = path;
        retro_game_info game{};
        game.path = romPath_.c_str();
        if (!sys.need_fullpath) {
            if (!readFile(romPath_, romData_)) return false;
            game.data = romData_.data();
            game.size = romData_.size();
        }
        return core_.load_game(&game);
    };

    bool loaded = tryLoad(romPath);
    if (!loaded && !playlistPaths_.empty()) {
        // Fallback for cores that don't parse .m3u playlists directly: load the initial disc
        // and register the remaining discs through libretro Disk Control if supported.
        unsigned startIdx = (initialDiscIndex_ < playlistPaths_.size()) ? initialDiscIndex_ : 0;
        LOGW("Core rejected .m3u playlist directly; falling back to disc %u (%s)",
             startIdx + 1, playlistPaths_[startIdx].c_str());
        loaded = tryLoad(playlistPaths_[startIdx]);
        if (loaded && hasDiskControl_ && diskControl_.get_num_images &&
            diskControl_.add_image_index && diskControl_.replace_image_index &&
            diskControl_.get_num_images() <= 1) {
            for (size_t i = 0; i < playlistPaths_.size(); i++) {
                if (i == startIdx) continue;
                if (diskControl_.add_image_index()) {
                    unsigned idx = diskControl_.get_num_images() - 1;
                    retro_game_info info{};
                    info.path = playlistPaths_[i].c_str();
                    diskControl_.replace_image_index(idx, &info);
                }
            }
        }
    }

    if (!loaded) {
        setError("The core could not load this game. It may be unsupported, corrupt, or need a BIOS file.");
        return false;
    }
    gameLoaded_ = true;

    // If an initial disc > 0 was requested and the core only exposes v0 disk control (no set_initial_image),
    // switch immediately before the first frame runs.
    if (initialDiscIndex_ > 0 && hasDiskControl_ && diskControl_.get_num_images &&
        diskControl_.get_image_index && diskControl_.set_eject_state && diskControl_.set_image_index) {
        unsigned total = diskControl_.get_num_images();
        if (initialDiscIndex_ < total && diskControl_.get_image_index() != initialDiscIndex_) {
            diskControl_.set_eject_state(true);
            diskControl_.set_image_index(initialDiscIndex_);
            diskControl_.set_eject_state(false);
        }
    }

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
    if (av.geometry.max_width > 0) maxVideoW_ = av.geometry.max_width;
    else if (av.geometry.base_width > 0) maxVideoW_ = av.geometry.base_width;
    if (av.geometry.max_height > 0) maxVideoH_ = av.geometry.max_height;
    else if (av.geometry.base_height > 0) maxVideoH_ = av.geometry.base_height;
    float ar = av.geometry.aspect_ratio;
    if (ar <= 0.0f && av.geometry.base_height > 0)
        ar = static_cast<float>(av.geometry.base_width) / av.geometry.base_height;
    if (ar > 0.0f) aspect_ = ar;
    LOGI("AV info: %ux%u (max %ux%u) fps=%.3f rate=%.1f aspect=%.3f", av.geometry.base_width,
         av.geometry.base_height, av.geometry.max_width, av.geometry.max_height,
         av.timing.fps, av.timing.sample_rate, ar);
}

void Frontend::start() {
    if (running_ || !gameLoaded_) return;
    running_ = true;
    audio_.start();
    thread_ = std::thread(&Frontend::runLoop, this);
}

void Frontend::runLoop() {
    pthread_setname_np(pthread_self(), "RetroHeroEmu");
    setpriority(PRIO_PROCESS, 0, -8);

    if (hwRenderEnabled_.load()) {
        unsigned reqMaj = hwRenderCb_.version_major;
        unsigned reqMin = hwRenderCb_.version_minor;
        if (hwRenderCb_.context_type == RETRO_HW_CONTEXT_OPENGLES2) {
            reqMaj = 2;
            reqMin = 0;
        } else if (reqMaj == 0) {
            reqMaj = 3;
            reqMin = 0;
        }
        if (hw_.initContext(reqMaj, reqMin, hwRenderCb_.depth, hwRenderCb_.stencil)) {
            std::lock_guard<std::mutex> lk(coreMutex_);
            hw_.beginFrame(maxVideoW_.load(), maxVideoH_.load());
            hwContextReady_ = true;
            if (hwRenderCb_.context_reset) {
                LOGI("Calling core HW context_reset()");
                hwRenderCb_.context_reset();
            }
            if (!pendingStatePath_.empty()) {
                std::vector<uint8_t> buf;
                if (readFile(pendingStatePath_, buf)) {
                    core_.unserialize(buf.data(), buf.size());
                }
                pendingStatePath_.clear();
            }
        } else {
            LOGE("HwRenderer initContext failed; hardware core cannot render");
        }
    }

    using clock = std::chrono::steady_clock;
    auto next = clock::now();
    auto lastFrame = clock::now();
    auto fpsWindowStart = next;
    int fpsFrames = 0;
    uint64_t lastSyncedSeq = 0;
    while (running_) {
        if (paused_) {
            vsyncActive_.store(false, std::memory_order_relaxed);
            bfiActive_.store(false, std::memory_order_relaxed);
            audio_.setSyncSkew(1.0);
            std::this_thread::sleep_for(std::chrono::milliseconds(16));
            next = clock::now();
            lastFrame = next;
            fpsWindowStart = next;
            fpsFrames = 0;
            {
                std::lock_guard<std::mutex> vlk(vsyncMutex_);
                lastSyncedSeq = vsyncSeq_;
            }
            continue;
        }
        auto workStart = clock::now();
        {
            std::lock_guard<std::mutex> lk(coreMutex_);
            if (hwContextReady_) {
                hw_.beginFrame(maxVideoW_.load(), maxVideoH_.load());
            }
            if (hasFrameTimeCb_ && frameTimeCb_.callback) {
                auto now = clock::now();
                auto usec = std::chrono::duration_cast<std::chrono::microseconds>(now - lastFrame).count();
                lastFrame = now;
                frameTimeCb_.callback(static_cast<retro_usec_t>(usec));
            }
            runFrameLocked();
        }
        auto workEnd = clock::now();
        float workMs = std::chrono::duration<float, std::milli>(workEnd - workStart).count();
        float prevMs = frameTimeMs_.load(std::memory_order_relaxed);
        frameTimeMs_.store(prevMs <= 0.01f ? workMs : (prevMs * 0.88f + workMs * 0.12f), std::memory_order_relaxed);

        if (!audioStaging_.empty()) {
            if (!fastForward_ && !rewinding_ && !slowMotion_) {
                audio_.write(audioStaging_.data(), audioStaging_.size() / 2);
            }
            audioStaging_.clear();
        }

        fpsFrames++;
        auto elapsedFps = std::chrono::duration<float>(workEnd - fpsWindowStart).count();
        if (elapsedFps >= 0.4f) {
            actualFps_.store(static_cast<float>(fpsFrames) / elapsedFps, std::memory_order_relaxed);
            fpsFrames = 0;
            fpsWindowStart = workEnd;
        }

        const bool normalSpeed = !fastForward_ && !slowMotion_ && !rewinding_;
        const double coreRate = fps_.load(std::memory_order_relaxed);
        const double dispHz = static_cast<double>(displayHz_.load(std::memory_order_relaxed));
        int div = (coreRate > 1.0 && dispHz > 1.0) ? static_cast<int>(std::lround(dispHz / coreRate)) : 1;
        if (div < 1) div = 1;
        if (div > 4) div = 4;
        const double effFps = dispHz / static_cast<double>(div);
        const double skew = (coreRate > 1.0) ? (effFps / coreRate) : 1.0;
        const bool canLockVsync = vsyncLockEnabled_.load(std::memory_order_relaxed) &&
                                  normalSpeed && std::abs(skew - 1.0) <= 0.035;

        bool usedVsync = false;
        if (canLockVsync) {
            std::unique_lock<std::mutex> vlk(vsyncMutex_);
            if (vsyncSeq_ > 0) {
                if (lastSyncedSeq == 0 || vsyncSeq_ > lastSyncedSeq + static_cast<uint64_t>(div) + 2) {
                    lastSyncedSeq = vsyncSeq_;
                }
                const bool doBfi = bfiEnabled_.load(std::memory_order_relaxed) && div >= 2;
                vsyncActive_.store(true, std::memory_order_relaxed);
                bfiActive_.store(doBfi, std::memory_order_relaxed);
                audio_.setSyncSkew(skew);

                if (doBfi) {
                    // 120 Hz OLED Black Frame Insertion: wait 1 hardware refresh (~8.3 ms), insert black frame,
                    // then wait remaining refresh(es) for the next real game frame.
                    uint64_t blackTick = lastSyncedSeq + 1;
                    bool gotBlack = vsyncCv_.wait_for(vlk, std::chrono::milliseconds(24), [&]() {
                        return !running_ || paused_ || vsyncSeq_ >= blackTick;
                    });
                    if (gotBlack && running_ && !paused_) {
                        vlk.unlock();
                        {
                            std::lock_guard<std::mutex> clk(coreMutex_);
                            if (hwContextReady_) hw_.presentBlack();
                            else video_.presentBlack();
                        }
                        vlk.lock();
                    }
                    uint64_t nextGameTick = lastSyncedSeq + static_cast<uint64_t>(div);
                    bool gotNext = vsyncCv_.wait_for(vlk, std::chrono::milliseconds(24), [&]() {
                        return !running_ || paused_ || vsyncSeq_ >= nextGameTick;
                    });
                    if (gotNext) {
                        lastSyncedSeq = vsyncSeq_;
                        usedVsync = true;
                    }
                } else {
                    uint64_t targetTick = lastSyncedSeq + static_cast<uint64_t>(div);
                    bool gotTick = vsyncCv_.wait_for(vlk, std::chrono::milliseconds(30), [&]() {
                        return !running_ || paused_ || vsyncSeq_ >= targetTick;
                    });
                    if (gotTick) {
                        lastSyncedSeq = vsyncSeq_;
                        usedVsync = true;
                    }
                }
            }
        }

        if (usedVsync) {
            next = clock::now();
        } else {
            vsyncActive_.store(false, std::memory_order_relaxed);
            bfiActive_.store(false, std::memory_order_relaxed);
            audio_.setSyncSkew(1.0);
            double speed = fastForward_ ? ffSpeed_.load() : (slowMotion_ ? 0.5 : 1.0);
            auto frame = std::chrono::duration_cast<clock::duration>(
                    std::chrono::duration<double>(1.0 / (coreRate * speed)));
            next += frame;
            auto now = clock::now();
            if (next < now - std::chrono::milliseconds(100)) {
                next = now;  // fell far behind (e.g. a slow frame); don't try to catch up
            } else if (next > now) {
                std::this_thread::sleep_until(next);
            }
        }
    }

    if (hwContextReady_) {
        std::lock_guard<std::mutex> lk(coreMutex_);
        if (hwRenderCb_.context_destroy) {
            hwRenderCb_.context_destroy();
        }
        hwContextReady_ = false;
        hw_.destroyContext();
    }
}

void Frontend::runFrameLocked() {
    if (rewinding_ && rewindEnabled_) {
        activeRunAhead_.store(0, std::memory_order_relaxed);
        if (popRewindLocked()) core_.run();
        else video_.redraw();
        return;
    }

    const int ra = runAheadFrames_.load(std::memory_order_relaxed);
    constexpr size_t kMaxRunAheadState = 2u << 20; // 2 MB cap keeps Run-Ahead ultra-fast on mobile
    if (ra > 0 && !fastForward_ && !hwRenderEnabled_ && core_.serialize_size && core_.serialize && core_.unserialize) {
        size_t sz = core_.serialize_size();
        if (sz > 0 && sz <= kMaxRunAheadState) {
            if (runAheadBuf_.size() != sz) runAheadBuf_.resize(sz);
            // Step 1: Advance real timeline 1 frame (keep audio, suppress video) and snapshot
            suppressVideo_ = true;
            suppressAudio_ = false;
            core_.run();
            if (core_.serialize(runAheadBuf_.data(), sz)) {
                // Step 2: Speculate N frames ahead (suppress audio, render final speculative frame!)
                suppressAudio_ = true;
                for (int i = 0; i < ra; i++) {
                    suppressVideo_ = (i + 1 < ra);
                    core_.run();
                }
                // Step 3: Restore real timeline seamlessly
                core_.unserialize(runAheadBuf_.data(), sz);
                suppressVideo_ = false;
                suppressAudio_ = false;
                activeRunAhead_.store(ra, std::memory_order_relaxed);
                if (rewindEnabled_ && !rewindTooBig_ && ++rewindTick_ % kRewindInterval == 0) pushRewindLocked();
                return;
            }
            suppressVideo_ = false;
            suppressAudio_ = false;
        }
    }

    activeRunAhead_.store(0, std::memory_order_relaxed);
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
    vsyncCv_.notify_all();
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
    vsyncCv_.notify_all();
}

void Frontend::setSlowMotion(bool enabled) {
    slowMotion_ = enabled;
    vsyncCv_.notify_all();
}

void Frontend::setRunAhead(int frames) {
    if (frames < 0) frames = 0;
    if (frames > 2) frames = 2;
    runAheadFrames_.store(frames, std::memory_order_relaxed);
}

void Frontend::onVsync(int64_t frameTimeNanos, float displayHz) {
    if (displayHz >= 45.0f && displayHz <= 245.0f) {
        displayHz_.store(displayHz, std::memory_order_relaxed);
    }
    {
        std::lock_guard<std::mutex> lk(vsyncMutex_);
        lastVsyncNs_ = frameTimeNanos;
        vsyncSeq_++;
    }
    vsyncCv_.notify_one();
}

void Frontend::setVsyncConfig(bool vsyncLock, bool bfi120Hz) {
    vsyncLockEnabled_.store(vsyncLock, std::memory_order_relaxed);
    bfiEnabled_.store(bfi120Hz, std::memory_order_relaxed);
    vsyncCv_.notify_all();
}

void Frontend::perfStats(float &fps, float &frameMs, int &activeRunAhead) const {
    fps = actualFps_.load(std::memory_order_relaxed);
    frameMs = frameTimeMs_.load(std::memory_order_relaxed);
    activeRunAhead = activeRunAhead_.load(std::memory_order_relaxed);
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
    if (hwRenderEnabled_.load() && !hwContextReady_) {
        pendingStatePath_ = path;
        return true;
    }
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

bool Frontend::readMemory(unsigned memoryType, std::vector<uint8_t> &out, size_t maxBytes) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_ || !core_.get_memory_size || !core_.get_memory_data) return false;
    size_t size = core_.get_memory_size(memoryType);
    const void *mem = core_.get_memory_data(memoryType);
    if (size == 0 || !mem) return false;
    if (maxBytes > 0 && size > maxBytes) size = maxBytes;
    out.resize(size);
    memcpy(out.data(), mem, size);
    return true;
}

void Frontend::shutdown(const std::string &sramPath) {
    running_ = false;
    vsyncCv_.notify_all();
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

void Frontend::setInitialDisc(unsigned index) {
    std::lock_guard<std::mutex> lk(coreMutex_);
    initialDiscIndex_ = index;
}

int Frontend::discCount() {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_ || !hasDiskControl_ || !diskControl_.get_num_images) return 0;
    return static_cast<int>(diskControl_.get_num_images());
}

int Frontend::currentDiscIndex() {
    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_ || !hasDiskControl_ || !diskControl_.get_image_index) return -1;
    return static_cast<int>(diskControl_.get_image_index());
}

std::vector<std::string> Frontend::discLabels() {
    std::lock_guard<std::mutex> lk(coreMutex_);
    std::vector<std::string> out;
    if (!gameLoaded_ || !hasDiskControl_ || !diskControl_.get_num_images) return out;
    unsigned count = diskControl_.get_num_images();
    out.reserve(count);
    char buf[512];
    for (unsigned i = 0; i < count; i++) {
        std::string label;
        if (hasDiskControlExt_ && diskControl_.get_image_label &&
            diskControl_.get_image_label(i, buf, sizeof(buf)) && buf[0] != '\0') {
            label = buf;
        } else if (hasDiskControlExt_ && diskControl_.get_image_path &&
                   diskControl_.get_image_path(i, buf, sizeof(buf)) && buf[0] != '\0') {
            label = baseFileName(buf);
        } else if (i < playlistPaths_.size() && !playlistPaths_[i].empty()) {
            label = baseFileName(playlistPaths_[i]);
        } else {
            label = "Disc " + std::to_string(i + 1);
        }
        out.push_back(std::move(label));
    }
    return out;
}

bool Frontend::swapDisc(unsigned index) {
    bool shouldBriefSpinDown = false;
    {
        std::lock_guard<std::mutex> lk(coreMutex_);
        if (!gameLoaded_ || !hasDiskControl_ || !diskControl_.get_num_images ||
            !diskControl_.set_eject_state || !diskControl_.set_image_index) {
            setError("This core does not support runtime disc swapping");
            return false;
        }
        unsigned count = diskControl_.get_num_images();
        if (index >= count) {
            setError("Requested disc index out of range");
            return false;
        }
        bool ejected = diskControl_.get_eject_state ? diskControl_.get_eject_state() : false;
        if (!ejected && diskControl_.get_image_index && diskControl_.get_image_index() == index) {
            return true;
        }
        if (!ejected) {
            if (!diskControl_.set_eject_state(true)) {
                setError("Could not open virtual disc tray");
                return false;
            }
        }
        // Let running cores observe the lid-open state for a few live frames on the emulation thread
        // (essential for PS1/Saturn titles like Final Fantasy VII, Metal Gear Solid, and Chrono Cross).
        shouldBriefSpinDown = running_.load() && !paused_.load();
    }

    if (shouldBriefSpinDown) {
        std::this_thread::sleep_for(std::chrono::milliseconds(80));
    }

    std::lock_guard<std::mutex> lk(coreMutex_);
    if (!gameLoaded_ || !hasDiskControl_) return false;
    bool okIdx = diskControl_.set_image_index(index);
    bool okClose = diskControl_.set_eject_state(false);
    clearRewindLocked();
    LOGI("Swapped disc to index %u (set_index=%d close=%d)", index, okIdx ? 1 : 0, okClose ? 1 : 0);
    return okIdx && okClose;
}

void Frontend::setWindow(ANativeWindow *window) {
    if (hwRenderEnabled_.load()) {
        video_.setWindow(nullptr);
        hw_.setWindow(window);
    } else if (!gameLoaded_) {
        // Surface often attaches before loadGame() reveals whether the core uses HW rendering.
        if (window) ANativeWindow_acquire(window);
        hw_.setWindow(window);
        video_.setWindow(window);
    } else {
        hw_.setWindow(nullptr);
        video_.setWindow(window);
    }
}

void Frontend::setFilter(int filter) {
    hw_.setFilter(filter);
    video_.setFilter(filter);
}

void Frontend::frameSize(unsigned &width, unsigned &height) {
    if (hwRenderEnabled_.load()) hw_.frameSize(width, height);
    else video_.frameSize(width, height);
}

bool Frontend::grabArgb(std::vector<uint32_t> &out, unsigned &width, unsigned &height) {
    if (hwRenderEnabled_.load()) return hw_.grabArgb(out, width, height);
    return video_.grabArgb(out, width, height);
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

void Frontend::setCoreOptionsV1(const retro_core_option_definition *defs) {
    std::lock_guard<std::mutex> lk(optionsMutex_);
    options_.clear();
    for (const retro_core_option_definition *d = defs; d && d->key; d++) {
        CoreOption opt;
        opt.key = d->key;
        opt.description = d->desc ? d->desc : d->key;
        for (size_t i = 0; i < RETRO_NUM_CORE_OPTION_VALUES_MAX && d->values[i].value; i++) {
            opt.values.emplace_back(d->values[i].value);
        }
        auto it = overrides_.find(opt.key);
        if (it != overrides_.end()) {
            opt.current = it->second;
        } else if (d->default_value && d->default_value[0]) {
            opt.current = d->default_value;
        } else {
            opt.current = opt.values.empty() ? "" : opt.values.front();
        }
        options_.push_back(std::move(opt));
    }
}

void Frontend::setCoreOptionsV2(const retro_core_option_v2_definition *defs) {
    std::lock_guard<std::mutex> lk(optionsMutex_);
    options_.clear();
    for (const retro_core_option_v2_definition *d = defs; d && d->key; d++) {
        CoreOption opt;
        opt.key = d->key;
        opt.description = d->desc ? d->desc : d->key;
        for (size_t i = 0; i < RETRO_NUM_CORE_OPTION_VALUES_MAX && d->values[i].value; i++) {
            opt.values.emplace_back(d->values[i].value);
        }
        auto it = overrides_.find(opt.key);
        if (it != overrides_.end()) {
            opt.current = it->second;
        } else if (d->default_value && d->default_value[0]) {
            opt.current = d->default_value;
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

uintptr_t Frontend::hwGetFramebufferCb() { return get().hw_.currentFramebuffer(); }

retro_proc_address_t Frontend::hwGetProcAddressCb(const char *sym) {
    return reinterpret_cast<retro_proc_address_t>(eglGetProcAddress(sym));
}

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
            *static_cast<unsigned *>(data) = 2;
            return true;
        case RETRO_ENVIRONMENT_SET_VARIABLES:
            setVariables(static_cast<const retro_variable *>(data));
            return true;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS:
            setCoreOptionsV1(static_cast<const retro_core_option_definition *>(data));
            return true;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL: {
            auto *intl = static_cast<const retro_core_options_intl *>(data);
            if (intl && intl->us) setCoreOptionsV1(intl->us);
            return true;
        }
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2: {
            auto *v2 = static_cast<const retro_core_options_v2 *>(data);
            if (v2 && v2->definitions) setCoreOptionsV2(v2->definitions);
            return true;
        }
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL: {
            auto *intl = static_cast<const retro_core_options_v2_intl *>(data);
            if (intl && intl->us && intl->us->definitions) setCoreOptionsV2(intl->us->definitions);
            return true;
        }
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
            auto it = overrides_.find(var->key);
            if (it != overrides_.end()) {
                var->value = it->second.c_str();
                return true;
            }
            var->value = nullptr;
            return false;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *static_cast<bool *>(data) = optionsDirty_.exchange(false);
            return true;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK:
            return true;
        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            *static_cast<unsigned *>(data) = RETRO_LANGUAGE_ENGLISH;
            return true;
        case RETRO_ENVIRONMENT_GET_USERNAME:
            *static_cast<const char **>(data) = "RetroHero";
            return true;
        case RETRO_ENVIRONMENT_SET_GEOMETRY: {
            auto *geom = static_cast<const retro_game_geometry *>(data);
            if (geom->max_width > 0) maxVideoW_ = geom->max_width;
            if (geom->max_height > 0) maxVideoH_ = geom->max_height;
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
        case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER:
            *static_cast<unsigned *>(data) = RETRO_HW_CONTEXT_OPENGLES3;
            return true;
        case RETRO_ENVIRONMENT_SET_HW_RENDER: {
            auto *cb = static_cast<retro_hw_render_callback *>(data);
            if (!cb) return false;
            switch (cb->context_type) {
                case RETRO_HW_CONTEXT_OPENGLES2:
                case RETRO_HW_CONTEXT_OPENGLES3:
                case RETRO_HW_CONTEXT_OPENGLES_VERSION:
                case RETRO_HW_CONTEXT_OPENGL:
                case RETRO_HW_CONTEXT_OPENGL_CORE:
                    cb->get_current_framebuffer = &Frontend::hwGetFramebufferCb;
                    cb->get_proc_address = &Frontend::hwGetProcAddressCb;
                    hwRenderCb_ = *cb;
                    hwRenderEnabled_ = true;
                    LOGI("Enabled EGL/GLES HW rendering (type=%d ver=%u.%u depth=%d stencil=%d)",
                         cb->context_type, cb->version_major, cb->version_minor,
                         cb->depth ? 1 : 0, cb->stencil ? 1 : 0);
                    return true;
                default:
                    LOGW("Unsupported HW render context_type=%d", cb->context_type);
                    return false;
            }
        }
        case RETRO_ENVIRONMENT_GET_DISK_CONTROL_INTERFACE_VERSION:
            *static_cast<unsigned *>(data) = 1;
            return true;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE: {
            diskControl_ = {};
            hasDiskControlExt_ = false;
            if (!data) {
                hasDiskControl_ = false;
                return true;
            }
            auto *cb = static_cast<const retro_disk_control_callback *>(data);
            diskControl_.set_eject_state = cb->set_eject_state;
            diskControl_.get_eject_state = cb->get_eject_state;
            diskControl_.get_image_index = cb->get_image_index;
            diskControl_.set_image_index = cb->set_image_index;
            diskControl_.get_num_images = cb->get_num_images;
            diskControl_.replace_image_index = cb->replace_image_index;
            diskControl_.add_image_index = cb->add_image_index;
            hasDiskControl_ = true;
            LOGI("Registered libretro Disk Control v0 interface");
            return true;
        }
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE: {
            diskControl_ = {};
            if (!data) {
                hasDiskControl_ = false;
                hasDiskControlExt_ = false;
                return true;
            }
            diskControl_ = *static_cast<const retro_disk_control_ext_callback *>(data);
            hasDiskControl_ = true;
            hasDiskControlExt_ = true;
            LOGI("Registered libretro Disk Control v1 (Ext) interface");
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
        default:
            return false;
    }
}

void Frontend::videoCb(const void *data, unsigned w, unsigned h, size_t pitch) {
    if (!data) return;
    Frontend &f = get();
    if (f.suppressVideo_) return;
    static uint32_t ffSkip = 0;
    if (f.fastForward_.load(std::memory_order_relaxed) && (++ffSkip % 3 != 0)) {
        return;
    }
    if (data == RETRO_HW_FRAME_BUFFER_VALID) {
        if (f.hwRenderEnabled_.load()) {
            f.hw_.presentFrame(w, h, f.hwRenderCb_.bottom_left_origin);
        }
        return;
    }
    f.video_.present(data, w, h, pitch, f.pixelFormat_);
}

void Frontend::audioSampleCb(int16_t l, int16_t r) {
    Frontend &f = get();
    if (f.suppressAudio_) return;
    auto &st = f.audioStaging_;
    st.push_back(l);
    st.push_back(r);
}

size_t Frontend::audioBatchCb(const int16_t *data, size_t frames) {
    Frontend &f = get();
    if (f.suppressAudio_) return frames;
    auto &st = f.audioStaging_;
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
