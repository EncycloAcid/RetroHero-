#pragma once

#include <cstddef>
#include <cstdint>
#include <mutex>
#include <vector>

namespace rh {

constexpr uint32_t kRollbackMaxFrames = 16;  // ~266 ms rollback window @ 60 fps
constexpr size_t kRollbackMaxStateBytes = 2u << 20;  // 2 MB max per snapshot

struct RollbackSnapshot {
    uint32_t frame = 0;
    bool valid = false;
    std::vector<uint8_t> data;
};

struct RollbackTelemetry {
    bool active = false;
    uint32_t currentFrame = 0;
    uint32_t lastLocalFrame = 0;
    uint32_t lastLocalMask = 0;
    uint32_t rollbackCount = 0;
    uint32_t maxRollbackDepth = 0;
    uint32_t stateChecksum = 0;
};

/**
 * Deterministic GGPO-style Rollback Netplay manager for RetroHero native cores
 * (HeroNES, HeroBox-16, and any deterministic libretro core with fast serialization).
 */
class RollbackEngine {
public:
    void start(int localPort, int inputDelayFrames);
    void stop();
    bool isActive() const;

    /** Called by network thread when a confirmed remote input arrives for [frame]. */
    void pushRemoteInput(uint32_t frame, uint32_t remoteMask);

    /** Returns true and sets [outTargetFrame] when a late remote input invalidated predictions. */
    bool consumePendingRollback(uint32_t &outTargetFrame);

    /** Stores serialized core state for [frame] and refreshes periodic FNV-1a desync checksum. */
    void saveSnapshot(uint32_t frame, const uint8_t *data, size_t size);

    /** Fetches serialized snapshot bytes for [frame], or nullptr if evicted/unavailable. */
    const std::vector<uint8_t> *snapshotFor(uint32_t frame) const;

    /**
     * Records local input for (currentFrame + inputDelay), predicts or uses confirmed remote input
     * for [currentFrame], and outputs effective P1/P2 button bitmasks.
     */
    void beginFrame(uint32_t localMask, uint32_t &outP1, uint32_t &outP2);

    /** Returns P1/P2 masks for re-simulating a past frame [frame] during rollback recovery. */
    void inputsForReplay(uint32_t frame, uint32_t &outP1, uint32_t &outP2);

    /** Advances deterministic frame counter after core_.run() completes. */
    void advanceFrame();

    void recordRollbackStats(uint32_t depth);

    uint32_t currentFrame() const;
    int localPort() const;
    RollbackTelemetry telemetry() const;

private:
    static constexpr uint32_t kInputHistory = 256;

    mutable std::mutex mutex_;
    bool active_ = false;
    int localPort_ = 0;       // 0 = P1 (Host), 1 = P2 (Guest)
    uint32_t inputDelay_ = 1; // 0..3 frames

    uint32_t currentFrame_ = 0;
    uint32_t lastLocalFrame_ = 0;
    uint32_t lastLocalMask_ = 0;

    uint32_t localInput_[kInputHistory]{};
    uint32_t localInputFrame_[kInputHistory]{};

    uint32_t remoteConfirmed_[kInputHistory]{};
    uint32_t remoteConfirmedFrame_[kInputHistory]{};
    bool remoteHasConfirmed_[kInputHistory]{};

    uint32_t remotePredicted_[kInputHistory]{};
    uint32_t remotePredictedFrame_[kInputHistory]{};
    uint32_t lastConfirmedRemoteMask_ = 0;

    bool hasPendingRollback_ = false;
    uint32_t pendingRollbackFrame_ = 0;

    RollbackSnapshot snapshots_[kRollbackMaxFrames]{};

    uint32_t rollbackCount_ = 0;
    uint32_t maxRollbackDepth_ = 0;
    uint32_t stateChecksum_ = 0;
};

}  // namespace rh
