#include "rollback_netplay.h"

#include <algorithm>
#include <cstring>

namespace rh {

namespace {

uint32_t fnv1a32(const uint8_t *data, size_t size) {
    uint32_t h = 2166136261u;
    // Sample stride keeps hashing <= 4 KB per check (< 15 microseconds) while catching RAM divergence
    size_t step = size > 4096 ? (size / 4096) : 1;
    for (size_t i = 0; i < size; i += step) {
        h ^= data[i];
        h *= 16777619u;
    }
    return h;
}

}  // namespace

void RollbackEngine::start(int localPort, int inputDelayFrames) {
    std::lock_guard<std::mutex> lk(mutex_);
    active_ = true;
    localPort_ = (localPort == 1) ? 1 : 0;
    inputDelay_ = static_cast<uint32_t>(std::clamp(inputDelayFrames, 0, 3));
    currentFrame_ = 0;
    lastLocalFrame_ = 0;
    lastLocalMask_ = 0;
    lastConfirmedRemoteMask_ = 0;
    hasPendingRollback_ = false;
    pendingRollbackFrame_ = 0;
    rollbackCount_ = 0;
    maxRollbackDepth_ = 0;
    stateChecksum_ = 0;
    std::memset(localInput_, 0, sizeof(localInput_));
    std::memset(localInputFrame_, 0xFF, sizeof(localInputFrame_));
    std::memset(remoteConfirmed_, 0, sizeof(remoteConfirmed_));
    std::memset(remoteConfirmedFrame_, 0xFF, sizeof(remoteConfirmedFrame_));
    std::memset(remoteHasConfirmed_, 0, sizeof(remoteHasConfirmed_));
    std::memset(remotePredicted_, 0, sizeof(remotePredicted_));
    std::memset(remotePredictedFrame_, 0xFF, sizeof(remotePredictedFrame_));
    for (auto &snap : snapshots_) {
        snap.valid = false;
        snap.frame = 0;
        snap.data.clear();
    }
}

void RollbackEngine::stop() {
    std::lock_guard<std::mutex> lk(mutex_);
    active_ = false;
    hasPendingRollback_ = false;
    for (auto &snap : snapshots_) {
        snap.valid = false;
        snap.data.clear();
    }
}

bool RollbackEngine::isActive() const {
    std::lock_guard<std::mutex> lk(mutex_);
    return active_;
}

void RollbackEngine::pushRemoteInput(uint32_t frame, uint32_t remoteMask) {
    std::lock_guard<std::mutex> lk(mutex_);
    if (!active_) return;
    uint32_t slot = frame % kInputHistory;
    bool alreadySame = remoteHasConfirmed_[slot] && remoteConfirmedFrame_[slot] == frame &&
                       remoteConfirmed_[slot] == remoteMask;
    remoteConfirmed_[slot] = remoteMask;
    remoteConfirmedFrame_[slot] = frame;
    remoteHasConfirmed_[slot] = true;
    lastConfirmedRemoteMask_ = remoteMask;

    if (!alreadySame && frame < currentFrame_) {
        // Only trigger rollback if this frame is within our snapshot ring window AND mismatched prediction
        if (currentFrame_ - frame < kRollbackMaxFrames) {
            uint32_t predSlot = frame % kInputHistory;
            bool predictedDiffers = (remotePredictedFrame_[predSlot] == frame) &&
                                    (remotePredicted_[predSlot] != remoteMask);
            if (predictedDiffers) {
                if (!hasPendingRollback_ || frame < pendingRollbackFrame_) {
                    pendingRollbackFrame_ = frame;
                    hasPendingRollback_ = true;
                }
            }
        }
    }
}

bool RollbackEngine::consumePendingRollback(uint32_t &outTargetFrame) {
    std::lock_guard<std::mutex> lk(mutex_);
    if (!active_ || !hasPendingRollback_) return false;
    hasPendingRollback_ = false;
    if (pendingRollbackFrame_ >= currentFrame_) return false;
    if (currentFrame_ - pendingRollbackFrame_ >= kRollbackMaxFrames) return false;
    outTargetFrame = pendingRollbackFrame_;
    return true;
}

void RollbackEngine::saveSnapshot(uint32_t frame, const uint8_t *data, size_t size) {
    std::lock_guard<std::mutex> lk(mutex_);
    if (!active_ || !data || size == 0 || size > kRollbackMaxStateBytes) return;
    auto &snap = snapshots_[frame % kRollbackMaxFrames];
    snap.frame = frame;
    snap.valid = true;
    snap.data.assign(data, data + size);
    if ((frame % 60u) == 0u) {
        stateChecksum_ = fnv1a32(data, size);
    }
}

const std::vector<uint8_t> *RollbackEngine::snapshotFor(uint32_t frame) const {
    std::lock_guard<std::mutex> lk(mutex_);
    const auto &snap = snapshots_[frame % kRollbackMaxFrames];
    if (!snap.valid || snap.frame != frame || snap.data.empty()) return nullptr;
    return &snap.data;
}

void RollbackEngine::beginFrame(uint32_t localMask, uint32_t &outP1, uint32_t &outP2) {
    std::lock_guard<std::mutex> lk(mutex_);
    // Schedule local input at currentFrame_ + inputDelay_
    uint32_t targetLocalFrame = currentFrame_ + inputDelay_;
    uint32_t lSlot = targetLocalFrame % kInputHistory;
    localInput_[lSlot] = localMask;
    localInputFrame_[lSlot] = targetLocalFrame;
    lastLocalFrame_ = targetLocalFrame;
    lastLocalMask_ = localMask;

    // Fetch local input scheduled for currentFrame_ (0 during initial inputDelay_ frames)
    uint32_t curSlot = currentFrame_ % kInputHistory;
    uint32_t curLocal = (localInputFrame_[curSlot] == currentFrame_) ? localInput_[curSlot] : 0u;

    // Fetch confirmed remote input or predict from last confirmed remote input
    uint32_t curRemote = lastConfirmedRemoteMask_;
    if (remoteHasConfirmed_[curSlot] && remoteConfirmedFrame_[curSlot] == currentFrame_) {
        curRemote = remoteConfirmed_[curSlot];
    }
    remotePredicted_[curSlot] = curRemote;
    remotePredictedFrame_[curSlot] = currentFrame_;

    if (localPort_ == 0) {
        outP1 = curLocal;
        outP2 = curRemote;
    } else {
        outP1 = curRemote;
        outP2 = curLocal;
    }
}

void RollbackEngine::inputsForReplay(uint32_t frame, uint32_t &outP1, uint32_t &outP2) {
    std::lock_guard<std::mutex> lk(mutex_);
    uint32_t slot = frame % kInputHistory;
    uint32_t curLocal = (localInputFrame_[slot] == frame) ? localInput_[slot] : 0u;
    uint32_t curRemote = lastConfirmedRemoteMask_;
    if (remoteHasConfirmed_[slot] && remoteConfirmedFrame_[slot] == frame) {
        curRemote = remoteConfirmed_[slot];
    } else if (remotePredictedFrame_[slot] == frame) {
        curRemote = remotePredicted_[slot];
    }
    remotePredicted_[slot] = curRemote;
    remotePredictedFrame_[slot] = frame;

    if (localPort_ == 0) {
        outP1 = curLocal;
        outP2 = curRemote;
    } else {
        outP1 = curRemote;
        outP2 = curLocal;
    }
}

void RollbackEngine::advanceFrame() {
    std::lock_guard<std::mutex> lk(mutex_);
    currentFrame_++;
}

void RollbackEngine::recordRollbackStats(uint32_t depth) {
    std::lock_guard<std::mutex> lk(mutex_);
    rollbackCount_++;
    if (depth > maxRollbackDepth_) maxRollbackDepth_ = depth;
}

uint32_t RollbackEngine::currentFrame() const {
    std::lock_guard<std::mutex> lk(mutex_);
    return currentFrame_;
}

int RollbackEngine::localPort() const {
    std::lock_guard<std::mutex> lk(mutex_);
    return localPort_;
}

RollbackTelemetry RollbackEngine::telemetry() const {
    std::lock_guard<std::mutex> lk(mutex_);
    RollbackTelemetry t;
    t.active = active_;
    t.currentFrame = currentFrame_;
    t.lastLocalFrame = lastLocalFrame_;
    t.lastLocalMask = lastLocalMask_;
    t.rollbackCount = rollbackCount_;
    t.maxRollbackDepth = maxRollbackDepth_;
    t.stateChecksum = stateChecksum_;
    return t;
}

}  // namespace rh
