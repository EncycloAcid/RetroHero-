#include "builtin_cores.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <vector>

#include "libretro.h"
#include "log.h"

namespace rh::builtin {
namespace {

// ============================================================================
// HeroNES — RetroHero's From-Scratch Native C++ NES / Famicom Emulator Core
//   - 2A03 6502 CPU (all 151 official + common unofficial opcodes, OAM DMA)
//   - 2C02 PPU (256x240 @ 60.0988 Hz, Loopy scrolling, 8x8/8x16 sprites,
//               accurate Sprite 0 Hit, authentic 64-color NTSC palette)
//   - 2A03 APU (Pulse 1/2 + Sweep, Triangle, Noise LFSR, DMC, nonlinear mixer)
//   - Mappers: 0 (NROM), 1 (MMC1), 2 (UxROM), 3 (CNROM), 4 (MMC3 + IRQ),
//              7 (AxROM), 66 (GxROM)
//   - Full Save States, Rewind, Battery SRAM, and RetroAchievements RAM export
// ============================================================================

constexpr unsigned kNesW = 256;
constexpr unsigned kNesH = 240;
constexpr double kNesFps = 60.0988138974405;
constexpr double kCpuHz = 1789772.7272727;
constexpr double kSampleRate = 44100.0;
constexpr uint32_t kStateMagic = 0x53454E48u; // "HNES"
constexpr uint32_t kStateVersion = 1u;

// Authentic 64-color NES NTSC composite palette (XRGB8888)
constexpr uint32_t kNesPal[64] = {
    0xFF626262, 0xFF001FB2, 0xFF2404C8, 0xFF5200B2, 0xFF730076, 0xFF800024, 0xFF730B00, 0xFF522800,
    0xFF244400, 0xFF005700, 0xFF005C00, 0xFF005324, 0xFF003C76, 0xFF000000, 0xFF000000, 0xFF000000,
    0xFFABABAB, 0xFF0D57FF, 0xFF4B30FF, 0xFF8A13FF, 0xFFBC08D6, 0xFFD21269, 0xFFC72E00, 0xFF9D5400,
    0xFF607B00, 0xFF209800, 0xFF00A300, 0xFF009942, 0xFF007DB4, 0xFF000000, 0xFF000000, 0xFF000000,
    0xFFFFFFFF, 0xFF53AEFF, 0xFF9085FF, 0xFFD365FF, 0xFFFF57FF, 0xFFFF5DCF, 0xFFFF7757, 0xFFFA9E00,
    0xFFBDC700, 0xFF7AE700, 0xFF43F611, 0xFF26EF7E, 0xFF2CD5F6, 0xFF4E4E4E, 0xFF000000, 0xFF000000,
    0xFFFFFFFF, 0xFFB6E1FF, 0xFFCED1FF, 0xFFE9C3FF, 0xFFFFBCFF, 0xFFFFBDF4, 0xFFFFC6C3, 0xFFFFD59A,
    0xFFE9E681, 0xFFCEF481, 0xFFB6FB9A, 0xFFA9FAC3, 0xFFA9F0F4, 0xFFB8B8B8, 0xFF000000, 0xFF000000,
};

constexpr uint8_t kDutyTable[4][8] = {
    {0, 1, 0, 0, 0, 0, 0, 0},
    {0, 1, 1, 0, 0, 0, 0, 0},
    {0, 1, 1, 1, 1, 0, 0, 0},
    {1, 0, 0, 1, 1, 1, 1, 1},
};

constexpr uint8_t kTriSeq[32] = {
    15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
     0,  1,  2,  3,  4,  5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
};

constexpr uint8_t kLengthTable[32] = {
    10, 254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
    12,  16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30,
};

constexpr uint16_t kNoisePeriod[16] = {
    4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068,
};

constexpr uint16_t kDmcRate[16] = {
    428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54,
};

enum MirrorMode : uint8_t {
    MIRROR_HORIZONTAL = 0,
    MIRROR_VERTICAL = 1,
    MIRROR_SINGLE0 = 2,
    MIRROR_SINGLE1 = 3,
    MIRROR_FOUR = 4,
};

struct PulseCh {
    bool enabled;
    uint8_t duty, dutyPos;
    bool envLoop, constVol, envStart;
    uint8_t envPeriod, envDivider, envDecay;
    bool sweepEnable, sweepNegate, sweepReload;
    uint8_t sweepPeriod, sweepShift, sweepDivider;
    uint16_t timerPeriod, timerVal;
    uint8_t length;
};

struct TriCh {
    bool enabled;
    bool ctrlFlag, linearReload;
    uint8_t linearPeriod, linearCounter;
    uint16_t timerPeriod, timerVal;
    uint8_t seqPos, length;
};

struct NoiseCh {
    bool enabled;
    bool envLoop, constVol, envStart, mode;
    uint8_t envPeriod, envDivider, envDecay;
    uint16_t timerPeriod, timerVal, lfsr;
    uint8_t length;
};

struct DmcCh {
    bool enabled, irqEnable, loop, silence;
    uint16_t ratePeriod, rateVal;
    uint16_t sampleAddr, sampleLen, curAddr, bytesRemaining;
    uint8_t output, shiftReg, bitsRemaining, sampleBuf;
    bool hasBuf;
};

// Serializable machine state (POD)
struct NesState {
    uint32_t magic;
    uint32_t version;

    // 2A03 CPU
    uint8_t a, x, y, sp, p;
    uint16_t pc;
    uint64_t cpuCycles;
    int32_t stallCycles;
    bool nmiPending;
    bool irqMapper;
    bool irqApu;

    // Memory
    uint8_t ram[2048];
    uint8_t wram[8192];
    uint8_t vram[4096]; // Up to 4 nametables (supports 4-screen mirroring)
    uint8_t palette[32];
    uint8_t oam[256];
    uint8_t chrRam[8192];

    // 2C02 PPU
    uint8_t ctrl, mask, status, oamAddr, readBuf;
    uint16_t v, t;
    uint8_t fineX;
    bool writeLatch;
    bool oddFrame;

    // Controllers
    uint8_t joyStrobe;
    uint8_t joyShift[2];
    uint8_t joyState[2];

    // Mapper state
    uint8_t mapper;
    uint8_t mirror;
    // MMC1
    uint8_t mmc1Shift, mmc1Count, mmc1Ctrl, mmc1Chr0, mmc1Chr1, mmc1Prg;
    // UxROM / CNROM / AxROM / GxROM
    uint8_t prgBank, chrBank;
    // MMC3
    uint8_t mmc3BankSelect;
    uint8_t mmc3Regs[8];
    uint8_t mmc3IrqLatch, mmc3IrqCounter;
    bool mmc3IrqReload, mmc3IrqEnable;

    // 2A03 APU
    PulseCh pulse[2];
    TriCh tri;
    NoiseCh noise;
    DmcCh dmc;
    uint8_t frameMode;
    bool frameIrqInhibit;
    uint32_t frameCycle;
    double sampleAccum;
    float hpPrevIn, hpPrevOut, lpPrevOut;

    uint32_t frameCounter;
};

struct NesRuntime {
    retro_environment_t env = nullptr;
    retro_video_refresh_t video = nullptr;
    retro_audio_sample_batch_t audioBatch = nullptr;
    retro_input_poll_t inputPoll = nullptr;
    retro_input_state_t inputState = nullptr;

    NesState s{};
    std::vector<uint8_t> prgRom;
    std::vector<uint8_t> chrRom;
    bool hasChrRam = false;
    bool hasBattery = false;
    uint32_t prgBankCount16k = 1;
    uint32_t prgBankCount8k = 2;
    uint32_t chrBankCount8k = 1;
    uint32_t chrBankCount1k = 8;

    uint32_t fb[kNesW * kNesH]{};
    int16_t audioBuf[2048 * 2]{};
    size_t audioFrames = 0;

    // Pre-computed mixer tables
    float pulseTable[31]{};
    float tndTable[203]{};
    bool tablesReady = false;
};

NesRuntime gNes;

void initMixerTables() {
    if (gNes.tablesReady) return;
    gNes.pulseTable[0] = 0.0f;
    for (int i = 1; i < 31; i++) {
        gNes.pulseTable[i] = 95.52f / (8128.0f / static_cast<float>(i) + 100.0f);
    }
    gNes.tndTable[0] = 0.0f;
    for (int i = 1; i < 203; i++) {
        gNes.tndTable[i] = 163.67f / (24329.0f / static_cast<float>(i) + 100.0f);
    }
    gNes.tablesReady = true;
}

// ============================================================================
// Mapper PRG / CHR / Nametable Addressing
// ============================================================================

inline uint8_t readPrg8k(uint32_t bank, uint16_t offset) {
    if (gNes.prgRom.empty()) return 0;
    uint32_t b = bank % gNes.prgBankCount8k;
    return gNes.prgRom[(b << 13) | (offset & 0x1FFF)];
}

inline uint8_t readPrg16k(uint32_t bank, uint16_t offset) {
    if (gNes.prgRom.empty()) return 0;
    uint32_t b = bank % gNes.prgBankCount16k;
    return gNes.prgRom[(b << 14) | (offset & 0x3FFF)];
}

uint8_t mapperCpuRead(uint16_t addr) {
    const auto &s = gNes.s;
    if (addr < 0x6000) return 0;
    if (addr < 0x8000) return s.wram[addr & 0x1FFF];

    switch (s.mapper) {
        case 0: // NROM
        case 3: // CNROM
            if (gNes.prgBankCount16k == 1) return readPrg16k(0, addr & 0x3FFF);
            return (addr < 0xC000) ? readPrg16k(0, addr & 0x3FFF) : readPrg16k(1, addr & 0x3FFF);

        case 1: { // MMC1
            uint8_t mode = (s.mmc1Ctrl >> 2) & 3;
            uint32_t bank = s.mmc1Prg & 0x0F;
            if (mode == 0 || mode == 1) {
                uint32_t b32 = bank & 0x0E;
                return (addr < 0xC000) ? readPrg16k(b32, addr & 0x3FFF) : readPrg16k(b32 + 1, addr & 0x3FFF);
            } else if (mode == 2) {
                return (addr < 0xC000) ? readPrg16k(0, addr & 0x3FFF) : readPrg16k(bank, addr & 0x3FFF);
            } else {
                return (addr < 0xC000) ? readPrg16k(bank, addr & 0x3FFF)
                                       : readPrg16k(gNes.prgBankCount16k - 1, addr & 0x3FFF);
            }
        }

        case 2: // UxROM
            return (addr < 0xC000) ? readPrg16k(s.prgBank, addr & 0x3FFF)
                                   : readPrg16k(gNes.prgBankCount16k - 1, addr & 0x3FFF);

        case 4: { // MMC3
            bool prgSwap = (s.mmc3BankSelect & 0x40) != 0;
            uint32_t last = gNes.prgBankCount8k - 1;
            uint32_t secondLast = (gNes.prgBankCount8k >= 2) ? (gNes.prgBankCount8k - 2) : 0;
            uint32_t r6 = s.mmc3Regs[6] & 0x3F;
            uint32_t r7 = s.mmc3Regs[7] & 0x3F;
            switch ((addr >> 13) & 3) {
                case 0: return readPrg8k(prgSwap ? secondLast : r6, addr);
                case 1: return readPrg8k(r7, addr);
                case 2: return readPrg8k(prgSwap ? r6 : secondLast, addr);
                default: return readPrg8k(last, addr);
            }
        }

        case 7: // AxROM (32KB switchable)
        case 66: { // GxROM (32KB switchable)
            uint32_t b = static_cast<uint32_t>(s.prgBank) * 2u;
            return (addr < 0xC000) ? readPrg16k(b, addr & 0x3FFF) : readPrg16k(b + 1, addr & 0x3FFF);
        }

        default:
            return readPrg16k(0, addr & 0x3FFF);
    }
}

void mapperCpuWrite(uint16_t addr, uint8_t val) {
    auto &s = gNes.s;
    if (addr < 0x6000) return;
    if (addr < 0x8000) {
        s.wram[addr & 0x1FFF] = val;
        return;
    }

    switch (s.mapper) {
        case 1: { // MMC1
            if (val & 0x80) {
                s.mmc1Shift = 0;
                s.mmc1Count = 0;
                s.mmc1Ctrl |= 0x0C;
            } else {
                s.mmc1Shift = static_cast<uint8_t>((s.mmc1Shift >> 1) | ((val & 1) << 4));
                s.mmc1Count++;
                if (s.mmc1Count == 5) {
                    uint8_t reg = (addr >> 13) & 3;
                    uint8_t data = s.mmc1Shift & 0x1F;
                    if (reg == 0) {
                        s.mmc1Ctrl = data;
                        switch (data & 3) {
                            case 0: s.mirror = MIRROR_SINGLE0; break;
                            case 1: s.mirror = MIRROR_SINGLE1; break;
                            case 2: s.mirror = MIRROR_VERTICAL; break;
                            case 3: s.mirror = MIRROR_HORIZONTAL; break;
                        }
                    } else if (reg == 1) {
                        s.mmc1Chr0 = data;
                    } else if (reg == 2) {
                        s.mmc1Chr1 = data;
                    } else {
                        s.mmc1Prg = data & 0x0F;
                    }
                    s.mmc1Shift = 0;
                    s.mmc1Count = 0;
                }
            }
            break;
        }
        case 2: // UxROM
            s.prgBank = val;
            break;
        case 3: // CNROM
            s.chrBank = val;
            break;
        case 4: { // MMC3
            bool even = (addr & 1) == 0;
            if (addr < 0xA000) {
                if (even) s.mmc3BankSelect = val;
                else s.mmc3Regs[s.mmc3BankSelect & 7] = val;
            } else if (addr < 0xC000) {
                if (even && s.mirror != MIRROR_FOUR) {
                    s.mirror = (val & 1) ? MIRROR_HORIZONTAL : MIRROR_VERTICAL;
                }
            } else if (addr < 0xE000) {
                if (even) s.mmc3IrqLatch = val;
                else s.mmc3IrqReload = true;
            } else {
                if (even) {
                    s.mmc3IrqEnable = false;
                    s.irqMapper = false;
                } else {
                    s.mmc3IrqEnable = true;
                }
            }
            break;
        }
        case 7: // AxROM
            s.prgBank = val & 0x07;
            s.mirror = (val & 0x10) ? MIRROR_SINGLE1 : MIRROR_SINGLE0;
            break;
        case 66: // GxROM
            s.chrBank = val & 0x03;
            s.prgBank = (val >> 4) & 0x03;
            break;
        default:
            break;
    }
}

void clockMmc3ScanlineIrq() {
    auto &s = gNes.s;
    if (s.mapper != 4) return;
    if (s.mmc3IrqCounter == 0 || s.mmc3IrqReload) {
        s.mmc3IrqCounter = s.mmc3IrqLatch;
        s.mmc3IrqReload = false;
    } else {
        s.mmc3IrqCounter--;
    }
    if (s.mmc3IrqCounter == 0 && s.mmc3IrqEnable) {
        s.irqMapper = true;
    }
}

uint8_t chrRead(uint16_t addr) {
    addr &= 0x1FFF;
    if (gNes.hasChrRam) return gNes.s.chrRam[addr];
    if (gNes.chrRom.empty()) return 0;
    const auto &s = gNes.s;

    switch (s.mapper) {
        case 0:
        case 2:
        case 7:
            return gNes.chrRom[addr % gNes.chrRom.size()];

        case 1: { // MMC1
            bool mode4k = (s.mmc1Ctrl & 0x10) != 0;
            if (mode4k) {
                uint32_t bank = (addr < 0x1000) ? s.mmc1Chr0 : s.mmc1Chr1;
                uint32_t banks4k = std::max<uint32_t>(1u, static_cast<uint32_t>(gNes.chrRom.size() >> 12));
                return gNes.chrRom[((bank % banks4k) << 12) | (addr & 0x0FFF)];
            } else {
                uint32_t bank8k = (s.mmc1Chr0 >> 1) % gNes.chrBankCount8k;
                return gNes.chrRom[(bank8k << 13) | addr];
            }
        }

        case 3: // CNROM
        case 66: { // GxROM
            uint32_t bank8k = s.chrBank % gNes.chrBankCount8k;
            return gNes.chrRom[(bank8k << 13) | addr];
        }

        case 4: { // MMC3
            bool invert = (s.mmc3BankSelect & 0x80) != 0;
            uint16_t eff = invert ? (addr ^ 0x1000) : addr;
            uint32_t bank1k = 0;
            if (eff < 0x0800) bank1k = (s.mmc3Regs[0] & 0xFE) + ((eff >> 10) & 1);
            else if (eff < 0x1000) bank1k = (s.mmc3Regs[1] & 0xFE) + ((eff >> 10) & 1);
            else if (eff < 0x1400) bank1k = s.mmc3Regs[2];
            else if (eff < 0x1800) bank1k = s.mmc3Regs[3];
            else if (eff < 0x1C00) bank1k = s.mmc3Regs[4];
            else bank1k = s.mmc3Regs[5];
            return gNes.chrRom[((bank1k % gNes.chrBankCount1k) << 10) | (eff & 0x03FF)];
        }

        default:
            return gNes.chrRom[addr % gNes.chrRom.size()];
    }
}

void chrWrite(uint16_t addr, uint8_t val) {
    if (gNes.hasChrRam) gNes.s.chrRam[addr & 0x1FFF] = val;
}

inline uint16_t mirrorNametable(uint16_t addr) {
    addr &= 0x0FFF;
    uint16_t nt = addr >> 10;
    uint16_t off = addr & 0x03FF;
    switch (gNes.s.mirror) {
        case MIRROR_HORIZONTAL: return ((nt >> 1) << 10) | off;
        case MIRROR_VERTICAL:   return ((nt & 1) << 10) | off;
        case MIRROR_SINGLE0:    return off;
        case MIRROR_SINGLE1:    return 0x0400 | off;
        case MIRROR_FOUR:       return addr;
        default:                return off;
    }
}

inline uint8_t paletteMirror(uint16_t addr) {
    uint8_t idx = static_cast<uint8_t>(addr & 0x1F);
    if (idx >= 16 && (idx & 3) == 0) idx -= 16;
    return idx;
}

uint8_t ppuMemRead(uint16_t addr) {
    addr &= 0x3FFF;
    if (addr < 0x2000) return chrRead(addr);
    if (addr < 0x3F00) return gNes.s.vram[mirrorNametable(addr)];
    uint8_t val = gNes.s.palette[paletteMirror(addr)] & 0x3F;
    if (gNes.s.mask & 0x01) val &= 0x30;
    return val;
}

void ppuMemWrite(uint16_t addr, uint8_t val) {
    addr &= 0x3FFF;
    if (addr < 0x2000) { chrWrite(addr, val); return; }
    if (addr < 0x3F00) { gNes.s.vram[mirrorNametable(addr)] = val; return; }
    gNes.s.palette[paletteMirror(addr)] = val & 0x3F;
}

// ============================================================================
// 2A03 APU — Pulse 1/2, Triangle, Noise, DMC + Nonlinear Mixer
// ============================================================================

uint8_t cpuRead(uint16_t addr); // forward decl for DMC sample DMA

void clockPulseEnv(PulseCh &p) {
    if (p.envStart) {
        p.envStart = false;
        p.envDecay = 15;
        p.envDivider = p.envPeriod;
    } else if (p.envDivider == 0) {
        p.envDivider = p.envPeriod;
        if (p.envDecay > 0) p.envDecay--;
        else if (p.envLoop) p.envDecay = 15;
    } else {
        p.envDivider--;
    }
}

void clockNoiseEnv(NoiseCh &n) {
    if (n.envStart) {
        n.envStart = false;
        n.envDecay = 15;
        n.envDivider = n.envPeriod;
    } else if (n.envDivider == 0) {
        n.envDivider = n.envPeriod;
        if (n.envDecay > 0) n.envDecay--;
        else if (n.envLoop) n.envDecay = 15;
    } else {
        n.envDivider--;
    }
}

void clockPulseSweep(PulseCh &p, int idx) {
    uint16_t change = p.timerPeriod >> p.sweepShift;
    int32_t target = p.sweepNegate
        ? static_cast<int32_t>(p.timerPeriod) - static_cast<int32_t>(change) - (idx == 0 ? 1 : 0)
        : static_cast<int32_t>(p.timerPeriod) + static_cast<int32_t>(change);
    bool mute = (p.timerPeriod < 8) || (target > 0x7FF);

    if (p.sweepDivider == 0 && p.sweepEnable && p.sweepShift > 0 && !mute) {
        p.timerPeriod = static_cast<uint16_t>(std::max<int32_t>(0, target));
    }
    if (p.sweepDivider == 0 || p.sweepReload) {
        p.sweepDivider = p.sweepPeriod;
        p.sweepReload = false;
    } else {
        p.sweepDivider--;
    }
}

void apuQuarterFrame() {
    auto &s = gNes.s;
    clockPulseEnv(s.pulse[0]);
    clockPulseEnv(s.pulse[1]);
    clockNoiseEnv(s.noise);
    if (s.tri.linearReload) s.tri.linearCounter = s.tri.linearPeriod;
    else if (s.tri.linearCounter > 0) s.tri.linearCounter--;
    if (!s.tri.ctrlFlag) s.tri.linearReload = false;
}

void apuHalfFrame() {
    auto &s = gNes.s;
    for (int i = 0; i < 2; i++) {
        if (!s.pulse[i].envLoop && s.pulse[i].length > 0) s.pulse[i].length--;
        clockPulseSweep(s.pulse[i], i);
    }
    if (!s.tri.ctrlFlag && s.tri.length > 0) s.tri.length--;
    if (!s.noise.envLoop && s.noise.length > 0) s.noise.length--;
}

void apuStepCpuCycles(int cycles) {
    auto &s = gNes.s;
    for (int c = 0; c < cycles; c++) {
        // Triangle clocks every CPU cycle
        if (s.tri.timerVal == 0) {
            s.tri.timerVal = s.tri.timerPeriod;
            if (s.tri.length > 0 && s.tri.linearCounter > 0 && s.tri.timerPeriod >= 2) {
                s.tri.seqPos = (s.tri.seqPos + 1) & 31;
            }
        } else {
            s.tri.timerVal--;
        }

        // Pulse, Noise, DMC clock every 2 CPU cycles
        if ((s.frameCycle & 1u) == 0u) {
            for (int i = 0; i < 2; i++) {
                auto &p = s.pulse[i];
                if (p.timerVal == 0) {
                    p.timerVal = p.timerPeriod;
                    p.dutyPos = (p.dutyPos + 1) & 7;
                } else {
                    p.timerVal--;
                }
            }
            if (s.noise.timerVal == 0) {
                s.noise.timerVal = s.noise.timerPeriod;
                uint16_t fb = (s.noise.lfsr & 1u) ^ ((s.noise.lfsr >> (s.noise.mode ? 6 : 1)) & 1u);
                s.noise.lfsr = static_cast<uint16_t>((s.noise.lfsr >> 1) | (fb << 14));
            } else {
                s.noise.timerVal--;
            }
        }

        // DMC timer (rates table is in CPU cycles)
        if (s.dmc.ratePeriod > 0) {
            if (s.dmc.rateVal == 0) {
                s.dmc.rateVal = s.dmc.ratePeriod;
                if (!s.dmc.silence) {
                    if (s.dmc.shiftReg & 1) {
                        if (s.dmc.output <= 125) s.dmc.output += 2;
                    } else {
                        if (s.dmc.output >= 2) s.dmc.output -= 2;
                    }
                }
                s.dmc.shiftReg >>= 1;
                if (s.dmc.bitsRemaining > 0) s.dmc.bitsRemaining--;
                if (s.dmc.bitsRemaining == 0) {
                    s.dmc.bitsRemaining = 8;
                    if (!s.dmc.hasBuf && s.dmc.bytesRemaining > 0) {
                        s.dmc.sampleBuf = mapperCpuRead(s.dmc.curAddr);
                        s.dmc.hasBuf = true;
                        s.dmc.curAddr = (s.dmc.curAddr == 0xFFFF) ? 0x8000 : static_cast<uint16_t>(s.dmc.curAddr + 1);
                        s.dmc.bytesRemaining--;
                        if (s.dmc.bytesRemaining == 0 && s.dmc.loop) {
                            s.dmc.curAddr = s.dmc.sampleAddr;
                            s.dmc.bytesRemaining = s.dmc.sampleLen;
                        }
                    }
                    if (s.dmc.hasBuf) {
                        s.dmc.silence = false;
                        s.dmc.shiftReg = s.dmc.sampleBuf;
                        s.dmc.hasBuf = false;
                    } else {
                        s.dmc.silence = true;
                    }
                }
            } else {
                s.dmc.rateVal--;
            }
        }

        // Frame sequencer
        s.frameCycle++;
        if (s.frameMode == 0) {
            if (s.frameCycle == 7457 || s.frameCycle == 22371) {
                apuQuarterFrame();
            } else if (s.frameCycle == 14913) {
                apuQuarterFrame();
                apuHalfFrame();
            } else if (s.frameCycle >= 29829) {
                apuQuarterFrame();
                apuHalfFrame();
                if (!s.frameIrqInhibit) s.irqApu = true;
                s.frameCycle = 0;
            }
        } else {
            if (s.frameCycle == 7457 || s.frameCycle == 22371) {
                apuQuarterFrame();
            } else if (s.frameCycle == 14913 || s.frameCycle >= 37281) {
                apuQuarterFrame();
                apuHalfFrame();
                if (s.frameCycle >= 37281) s.frameCycle = 0;
            }
        }

        // Downsample to 44.1 kHz
        s.sampleAccum += kSampleRate;
        if (s.sampleAccum >= kCpuHz) {
            s.sampleAccum -= kCpuHz;
            int pOut = 0;
            for (int i = 0; i < 2; i++) {
                const auto &p = s.pulse[i];
                if (p.enabled && p.length > 0 && p.timerPeriod >= 8 && p.timerPeriod <= 0x7FF &&
                    kDutyTable[p.duty][p.dutyPos]) {
                    pOut += p.constVol ? p.envPeriod : p.envDecay;
                }
            }
            int tOut = (s.tri.enabled && s.tri.length > 0 && s.tri.linearCounter > 0) ? kTriSeq[s.tri.seqPos] : 0;
            int nOut = (s.noise.enabled && s.noise.length > 0 && ((s.noise.lfsr & 1u) == 0u))
                ? (s.noise.constVol ? s.noise.envPeriod : s.noise.envDecay) : 0;
            int dOut = s.dmc.output;

            float mix = gNes.pulseTable[std::min(30, pOut)] +
                        gNes.tndTable[std::min(202, 3 * tOut + 2 * nOut + dOut)];

            // Gentle DC-blocking high-pass + smoothing low-pass
            float hp = 0.996f * (s.hpPrevOut + mix - s.hpPrevIn);
            s.hpPrevIn = mix;
            s.hpPrevOut = hp;
            float lp = s.lpPrevOut + 0.65f * (hp - s.lpPrevOut);
            s.lpPrevOut = lp;

            int32_t pcm = static_cast<int32_t>(std::clamp(lp * 28000.0f, -32767.0f, 32767.0f));
            if (gNes.audioFrames < 2048) {
                gNes.audioBuf[gNes.audioFrames * 2] = static_cast<int16_t>(pcm);
                gNes.audioBuf[gNes.audioFrames * 2 + 1] = static_cast<int16_t>(pcm);
                gNes.audioFrames++;
            }
        }
    }
}

void apuWrite(uint16_t addr, uint8_t val) {
    auto &s = gNes.s;
    switch (addr) {
        case 0x4000: case 0x4004: {
            auto &p = s.pulse[(addr >> 2) & 1];
            p.duty = (val >> 6) & 3;
            p.envLoop = (val & 0x20) != 0;
            p.constVol = (val & 0x10) != 0;
            p.envPeriod = val & 0x0F;
            break;
        }
        case 0x4001: case 0x4005: {
            auto &p = s.pulse[(addr >> 2) & 1];
            p.sweepEnable = (val & 0x80) != 0;
            p.sweepPeriod = (val >> 4) & 7;
            p.sweepNegate = (val & 0x08) != 0;
            p.sweepShift = val & 7;
            p.sweepReload = true;
            break;
        }
        case 0x4002: case 0x4006: {
            auto &p = s.pulse[(addr >> 2) & 1];
            p.timerPeriod = (p.timerPeriod & 0x0700) | val;
            break;
        }
        case 0x4003: case 0x4007: {
            auto &p = s.pulse[(addr >> 2) & 1];
            p.timerPeriod = (p.timerPeriod & 0x00FF) | (static_cast<uint16_t>(val & 7) << 8);
            if (p.enabled) p.length = kLengthTable[val >> 3];
            p.dutyPos = 0;
            p.envStart = true;
            break;
        }
        case 0x4008:
            s.tri.ctrlFlag = (val & 0x80) != 0;
            s.tri.linearPeriod = val & 0x7F;
            break;
        case 0x400A:
            s.tri.timerPeriod = (s.tri.timerPeriod & 0x0700) | val;
            break;
        case 0x400B:
            s.tri.timerPeriod = (s.tri.timerPeriod & 0x00FF) | (static_cast<uint16_t>(val & 7) << 8);
            if (s.tri.enabled) s.tri.length = kLengthTable[val >> 3];
            s.tri.linearReload = true;
            break;
        case 0x400C:
            s.noise.envLoop = (val & 0x20) != 0;
            s.noise.constVol = (val & 0x10) != 0;
            s.noise.envPeriod = val & 0x0F;
            break;
        case 0x400E:
            s.noise.mode = (val & 0x80) != 0;
            s.noise.timerPeriod = kNoisePeriod[val & 0x0F];
            break;
        case 0x400F:
            if (s.noise.enabled) s.noise.length = kLengthTable[val >> 3];
            s.noise.envStart = true;
            break;
        case 0x4010:
            s.dmc.irqEnable = (val & 0x80) != 0;
            s.dmc.loop = (val & 0x40) != 0;
            s.dmc.ratePeriod = kDmcRate[val & 0x0F];
            break;
        case 0x4011:
            s.dmc.output = val & 0x7F;
            break;
        case 0x4012:
            s.dmc.sampleAddr = static_cast<uint16_t>(0xC000u | (static_cast<uint16_t>(val) << 6));
            break;
        case 0x4013:
            s.dmc.sampleLen = static_cast<uint16_t>((static_cast<uint16_t>(val) << 4) + 1u);
            break;
        case 0x4015:
            s.pulse[0].enabled = (val & 0x01) != 0; if (!s.pulse[0].enabled) s.pulse[0].length = 0;
            s.pulse[1].enabled = (val & 0x02) != 0; if (!s.pulse[1].enabled) s.pulse[1].length = 0;
            s.tri.enabled      = (val & 0x04) != 0; if (!s.tri.enabled) s.tri.length = 0;
            s.noise.enabled    = (val & 0x08) != 0; if (!s.noise.enabled) s.noise.length = 0;
            s.dmc.enabled      = (val & 0x10) != 0;
            if (!s.dmc.enabled) {
                s.dmc.bytesRemaining = 0;
            } else if (s.dmc.bytesRemaining == 0) {
                s.dmc.curAddr = s.dmc.sampleAddr;
                s.dmc.bytesRemaining = s.dmc.sampleLen;
            }
            break;
        case 0x4017:
            s.frameMode = (val >> 7) & 1;
            s.frameIrqInhibit = (val & 0x40) != 0;
            if (s.frameIrqInhibit) s.irqApu = false;
            s.frameCycle = 0;
            if (s.frameMode == 1) {
                apuQuarterFrame();
                apuHalfFrame();
            }
            break;
        default:
            break;
    }
}

uint8_t apuReadStatus() {
    auto &s = gNes.s;
    uint8_t r = 0;
    if (s.pulse[0].length > 0) r |= 0x01;
    if (s.pulse[1].length > 0) r |= 0x02;
    if (s.tri.length > 0)      r |= 0x04;
    if (s.noise.length > 0)    r |= 0x08;
    if (s.dmc.bytesRemaining > 0) r |= 0x10;
    if (s.irqApu) r |= 0x40;
    s.irqApu = false;
    return r;
}

// ============================================================================
// 2C02 PPU Register I/O & Scanline Renderer
// ============================================================================

uint8_t ppuRegRead(uint16_t reg) {
    auto &s = gNes.s;
    switch (reg & 7) {
        case 2: { // PPUSTATUS
            uint8_t r = s.status & 0xE0;
            s.status &= 0x7F; // clear VBlank
            s.writeLatch = false;
            return r;
        }
        case 4: // OAMDATA
            return s.oam[s.oamAddr];
        case 7: { // PPUDATA
            uint16_t addr = s.v & 0x3FFF;
            uint8_t val = ppuMemRead(addr);
            uint8_t out;
            if (addr < 0x3F00) {
                out = s.readBuf;
                s.readBuf = val;
            } else {
                out = val;
                s.readBuf = s.vram[mirrorNametable(addr)];
            }
            s.v = (s.v + ((s.ctrl & 0x04) ? 32 : 1)) & 0x7FFF;
            return out;
        }
        default:
            return 0;
    }
}

void ppuRegWrite(uint16_t reg, uint8_t val) {
    auto &s = gNes.s;
    switch (reg & 7) {
        case 0: { // PPUCTRL
            bool prevNmi = (s.ctrl & 0x80) != 0;
            s.ctrl = val;
            s.t = (s.t & ~0x0C00u) | (static_cast<uint16_t>(val & 3) << 10);
            if (!prevNmi && (val & 0x80) && (s.status & 0x80)) {
                s.nmiPending = true;
            }
            break;
        }
        case 1: // PPUMASK
            s.mask = val;
            break;
        case 3: // OAMADDR
            s.oamAddr = val;
            break;
        case 4: // OAMDATA
            s.oam[s.oamAddr++] = val;
            break;
        case 5: // PPUSCROLL
            if (!s.writeLatch) {
                s.fineX = val & 7;
                s.t = (s.t & ~0x001Fu) | (val >> 3);
                s.writeLatch = true;
            } else {
                s.t = (s.t & ~0x73E0u) |
                      (static_cast<uint16_t>(val & 7) << 12) |
                      (static_cast<uint16_t>(val >> 3) << 5);
                s.writeLatch = false;
            }
            break;
        case 6: // PPUADDR
            if (!s.writeLatch) {
                s.t = (s.t & 0x00FFu) | (static_cast<uint16_t>(val & 0x3F) << 8);
                s.writeLatch = true;
            } else {
                s.t = (s.t & 0x7F00u) | val;
                s.v = s.t;
                s.writeLatch = false;
            }
            break;
        case 7: // PPUDATA
            ppuMemWrite(s.v, val);
            s.v = (s.v + ((s.ctrl & 0x04) ? 32 : 1)) & 0x7FFF;
            break;
        default:
            break;
    }
}

inline void ppuIncX(uint16_t &v) {
    if ((v & 0x001F) == 31) {
        v &= ~0x001Fu;
        v ^= 0x0400u;
    } else {
        v++;
    }
}

inline void ppuIncY(uint16_t &v) {
    if ((v & 0x7000) != 0x7000) {
        v += 0x1000;
    } else {
        v &= ~0x7000u;
        uint16_t y = (v & 0x03E0) >> 5;
        if (y == 29) {
            y = 0;
            v ^= 0x0800u;
        } else if (y == 31) {
            y = 0;
        } else {
            y++;
        }
        v = (v & ~0x03E0u) | (y << 5);
    }
}

// Renders one visible scanline (0..239) into gNes.fb and returns the X coordinate
// where Sprite 0 Hit occurs (-1 if no Sprite 0 Hit on this scanline).
int renderScanline(int y) {
    auto &s = gNes.s;
    uint32_t *row = gNes.fb + y * kNesW;
    const bool showBg = (s.mask & 0x08) != 0;
    const bool showSpr = (s.mask & 0x10) != 0;
    const bool showBgLeft = (s.mask & 0x02) != 0;
    const bool showSprLeft = (s.mask & 0x04) != 0;
    const uint8_t grayMask = (s.mask & 0x01) ? 0x30 : 0x3F;
    const uint32_t bgColor = kNesPal[s.palette[0] & grayMask];

    uint8_t bgOpaque[kNesW]{};

    if (showBg) {
        uint16_t v = s.v;
        const uint16_t bgBase = (s.ctrl & 0x10) ? 0x1000 : 0x0000;
        const int fineX = s.fineX;
        const int fineY = (v >> 12) & 7;

        int x = 0;
        while (x < static_cast<int>(kNesW)) {
            uint16_t ntAddr = 0x2000 | (v & 0x0FFF);
            uint8_t tile = s.vram[mirrorNametable(ntAddr)];
            uint16_t attrAddr = 0x23C0 | (v & 0x0C00) | ((v >> 4) & 0x38) | ((v >> 2) & 0x07);
            uint8_t attr = s.vram[mirrorNametable(attrAddr)];
            int shift = ((v & 0x0040) ? 4 : 0) | ((v & 0x0002) ? 2 : 0);
            uint8_t palHi = ((attr >> shift) & 3) << 2;

            uint16_t patAddr = bgBase + (static_cast<uint16_t>(tile) << 4) + fineY;
            uint8_t lo = chrRead(patAddr);
            uint8_t hi = chrRead(patAddr + 8);

            int startBit = (x == 0) ? fineX : 0;
            for (int bit = startBit; bit < 8 && x < static_cast<int>(kNesW); bit++, x++) {
                int sh = 7 - bit;
                uint8_t px = ((lo >> sh) & 1) | (((hi >> sh) & 1) << 1);
                if (px != 0 && (x >= 8 || showBgLeft)) {
                    bgOpaque[x] = 1;
                    row[x] = kNesPal[s.palette[palHi | px] & grayMask];
                } else {
                    row[x] = bgColor;
                }
            }
            ppuIncX(v);
        }
    } else {
        for (unsigned x = 0; x < kNesW; x++) row[x] = bgColor;
    }

    int sprite0HitX = -1;
    if (showSpr) {
        const bool tall = (s.ctrl & 0x20) != 0;
        const int sprH = tall ? 16 : 8;
        const uint16_t sprBase = (s.ctrl & 0x08) ? 0x1000 : 0x0000;

        int found[8];
        int count = 0;
        for (int i = 0; i < 64; i++) {
            int sy = static_cast<int>(s.oam[i * 4]) + 1;
            if (y >= sy && y < sy + sprH) {
                if (count < 8) found[count++] = i;
                else { s.status |= 0x20; break; }
            }
        }

        // Draw back-to-front so lower OAM index has priority
        for (int idx = count - 1; idx >= 0; idx--) {
            int i = found[idx];
            int sy = static_cast<int>(s.oam[i * 4]) + 1;
            uint8_t tile = s.oam[i * 4 + 1];
            uint8_t attr = s.oam[i * 4 + 2];
            int sx = s.oam[i * 4 + 3];

            bool flipH = (attr & 0x40) != 0;
            bool flipV = (attr & 0x80) != 0;
            bool behindBg = (attr & 0x20) != 0;
            uint8_t palHi = 0x10 | ((attr & 3) << 2);

            int rowInSpr = y - sy;
            if (flipV) rowInSpr = sprH - 1 - rowInSpr;

            uint16_t patAddr;
            if (tall) {
                uint16_t bank = (tile & 1) ? 0x1000 : 0x0000;
                uint8_t tIdx = (tile & 0xFE) + (rowInSpr >= 8 ? 1 : 0);
                patAddr = bank + (static_cast<uint16_t>(tIdx) << 4) + (rowInSpr & 7);
            } else {
                patAddr = sprBase + (static_cast<uint16_t>(tile) << 4) + rowInSpr;
            }

            uint8_t lo = chrRead(patAddr);
            uint8_t hi = chrRead(patAddr + 8);

            for (int col = 0; col < 8; col++) {
                int x = sx + col;
                if (x < 0 || x >= static_cast<int>(kNesW)) continue;
                if (x < 8 && !showSprLeft) continue;
                int bit = flipH ? col : (7 - col);
                uint8_t px = ((lo >> bit) & 1) | (((hi >> bit) & 1) << 1);
                if (px == 0) continue;

                if (i == 0 && showBg && bgOpaque[x] && x < 255 && sprite0HitX < 0) {
                    sprite0HitX = x;
                }
                if (!behindBg || !bgOpaque[x]) {
                    row[x] = kNesPal[s.palette[palHi | px] & grayMask];
                }
            }
        }
    }

    return sprite0HitX;
}

// ============================================================================
// CPU Memory Bus
// ============================================================================

uint8_t cpuRead(uint16_t addr) {
    auto &s = gNes.s;
    if (addr < 0x2000) return s.ram[addr & 0x07FF];
    if (addr < 0x4000) return ppuRegRead(addr);
    if (addr == 0x4015) return apuReadStatus();
    if (addr == 0x4016 || addr == 0x4017) {
        int port = addr & 1;
        if (s.joyStrobe & 1) return (s.joyState[port] & 1) | 0x40;
        uint8_t bit = s.joyShift[port] & 1;
        s.joyShift[port] = static_cast<uint8_t>((s.joyShift[port] >> 1) | 0x80);
        return bit | 0x40;
    }
    if (addr < 0x4020) return 0;
    return mapperCpuRead(addr);
}

void cpuWrite(uint16_t addr, uint8_t val) {
    auto &s = gNes.s;
    if (addr < 0x2000) { s.ram[addr & 0x07FF] = val; return; }
    if (addr < 0x4000) { ppuRegWrite(addr, val); return; }
    if (addr == 0x4014) { // OAM DMA
        uint16_t base = static_cast<uint16_t>(val) << 8;
        for (int i = 0; i < 256; i++) {
            s.oam[s.oamAddr++] = cpuRead(base + i);
        }
        s.stallCycles += 513 + static_cast<int32_t>(s.cpuCycles & 1u);
        return;
    }
    if (addr == 0x4016) {
        s.joyStrobe = val & 1;
        if (s.joyStrobe) {
            s.joyShift[0] = s.joyState[0];
            s.joyShift[1] = s.joyState[1];
        }
        return;
    }
    if (addr <= 0x4017) { apuWrite(addr, val); return; }
    if (addr >= 0x4020) mapperCpuWrite(addr, val);
}

inline uint16_t cpuRead16(uint16_t addr) {
    return static_cast<uint16_t>(cpuRead(addr) | (static_cast<uint16_t>(cpuRead(addr + 1)) << 8));
}

// ============================================================================
// 2A03 6502 CPU Core
// ============================================================================

constexpr uint8_t F_C = 0x01;
constexpr uint8_t F_Z = 0x02;
constexpr uint8_t F_I = 0x04;
constexpr uint8_t F_D = 0x08;
constexpr uint8_t F_B = 0x10;
constexpr uint8_t F_U = 0x20;
constexpr uint8_t F_V = 0x40;
constexpr uint8_t F_N = 0x80;

inline void setZN(uint8_t v) {
    auto &p = gNes.s.p;
    p = static_cast<uint8_t>((p & ~(F_Z | F_N)) | (v == 0 ? F_Z : 0) | (v & F_N));
}

inline void push(uint8_t v) {
    gNes.s.ram[0x0100 | gNes.s.sp--] = v;
}

inline uint8_t pop() {
    return gNes.s.ram[0x0100 | ++gNes.s.sp];
}

inline void push16(uint16_t v) {
    push(static_cast<uint8_t>(v >> 8));
    push(static_cast<uint8_t>(v & 0xFF));
}

inline uint16_t pop16() {
    uint8_t lo = pop();
    uint8_t hi = pop();
    return static_cast<uint16_t>(lo | (static_cast<uint16_t>(hi) << 8));
}

void doAdc(uint8_t v) {
    auto &s = gNes.s;
    uint16_t sum = static_cast<uint16_t>(s.a) + v + (s.p & F_C);
    uint8_t res = static_cast<uint8_t>(sum & 0xFF);
    s.p = static_cast<uint8_t>((s.p & ~(F_C | F_V)) |
                               (sum > 0xFF ? F_C : 0) |
                               ((~(s.a ^ v) & (s.a ^ res) & 0x80) ? F_V : 0));
    s.a = res;
    setZN(s.a);
}

inline void doCmp(uint8_t reg, uint8_t v) {
    uint16_t diff = static_cast<uint16_t>(reg) - v;
    gNes.s.p = static_cast<uint8_t>((gNes.s.p & ~F_C) | (reg >= v ? F_C : 0));
    setZN(static_cast<uint8_t>(diff & 0xFF));
}

int branch(bool cond) {
    auto &s = gNes.s;
    int8_t off = static_cast<int8_t>(cpuRead(s.pc++));
    if (!cond) return 2;
    uint16_t oldPc = s.pc;
    s.pc = static_cast<uint16_t>(s.pc + off);
    return ((oldPc & 0xFF00) != (s.pc & 0xFF00)) ? 4 : 3;
}

int cpuStep() {
    auto &s = gNes.s;
    if (s.stallCycles > 0) {
        s.stallCycles--;
        s.cpuCycles++;
        return 1;
    }

    if (s.nmiPending) {
        s.nmiPending = false;
        push16(s.pc);
        push(static_cast<uint8_t>((s.p & ~F_B) | F_U));
        s.p |= F_I;
        s.pc = cpuRead16(0xFFFA);
        s.cpuCycles += 7;
        return 7;
    }
    if ((s.irqMapper || s.irqApu) && !(s.p & F_I)) {
        push16(s.pc);
        push(static_cast<uint8_t>((s.p & ~F_B) | F_U));
        s.p |= F_I;
        s.pc = cpuRead16(0xFFFE);
        s.cpuCycles += 7;
        return 7;
    }

    auto imm = [&]() -> uint16_t { return s.pc++; };
    auto zp  = [&]() -> uint16_t { return cpuRead(s.pc++); };
    auto zpx = [&]() -> uint16_t { return static_cast<uint8_t>(cpuRead(s.pc++) + s.x); };
    auto zpy = [&]() -> uint16_t { return static_cast<uint8_t>(cpuRead(s.pc++) + s.y); };
    auto absA = [&]() -> uint16_t { uint16_t a = cpuRead16(s.pc); s.pc += 2; return a; };
    auto absX = [&](int &extra) -> uint16_t {
        uint16_t base = cpuRead16(s.pc); s.pc += 2;
        uint16_t a = static_cast<uint16_t>(base + s.x);
        if ((base & 0xFF00) != (a & 0xFF00)) extra = 1;
        return a;
    };
    auto absY = [&](int &extra) -> uint16_t {
        uint16_t base = cpuRead16(s.pc); s.pc += 2;
        uint16_t a = static_cast<uint16_t>(base + s.y);
        if ((base & 0xFF00) != (a & 0xFF00)) extra = 1;
        return a;
    };
    auto indX = [&]() -> uint16_t {
        uint8_t ptr = static_cast<uint8_t>(cpuRead(s.pc++) + s.x);
        return static_cast<uint16_t>(cpuRead(ptr) | (static_cast<uint16_t>(cpuRead(static_cast<uint8_t>(ptr + 1))) << 8));
    };
    auto indY = [&](int &extra) -> uint16_t {
        uint8_t ptr = cpuRead(s.pc++);
        uint16_t base = static_cast<uint16_t>(cpuRead(ptr) | (static_cast<uint16_t>(cpuRead(static_cast<uint8_t>(ptr + 1))) << 8));
        uint16_t a = static_cast<uint16_t>(base + s.y);
        if ((base & 0xFF00) != (a & 0xFF00)) extra = 1;
        return a;
    };

    uint8_t op = cpuRead(s.pc++);
    int cyc = 2;
    int extra = 0;

    switch (op) {
        // ---- Loads & Stores ----
        case 0xA9: s.a = cpuRead(imm()); setZN(s.a); cyc = 2; break;
        case 0xA5: s.a = cpuRead(zp()); setZN(s.a); cyc = 3; break;
        case 0xB5: s.a = cpuRead(zpx()); setZN(s.a); cyc = 4; break;
        case 0xAD: s.a = cpuRead(absA()); setZN(s.a); cyc = 4; break;
        case 0xBD: s.a = cpuRead(absX(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0xB9: s.a = cpuRead(absY(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0xA1: s.a = cpuRead(indX()); setZN(s.a); cyc = 6; break;
        case 0xB1: s.a = cpuRead(indY(extra)); setZN(s.a); cyc = 5 + extra; break;

        case 0xA2: s.x = cpuRead(imm()); setZN(s.x); cyc = 2; break;
        case 0xA6: s.x = cpuRead(zp()); setZN(s.x); cyc = 3; break;
        case 0xB6: s.x = cpuRead(zpy()); setZN(s.x); cyc = 4; break;
        case 0xAE: s.x = cpuRead(absA()); setZN(s.x); cyc = 4; break;
        case 0xBE: s.x = cpuRead(absY(extra)); setZN(s.x); cyc = 4 + extra; break;

        case 0xA0: s.y = cpuRead(imm()); setZN(s.y); cyc = 2; break;
        case 0xA4: s.y = cpuRead(zp()); setZN(s.y); cyc = 3; break;
        case 0xB4: s.y = cpuRead(zpx()); setZN(s.y); cyc = 4; break;
        case 0xAC: s.y = cpuRead(absA()); setZN(s.y); cyc = 4; break;
        case 0xBC: s.y = cpuRead(absX(extra)); setZN(s.y); cyc = 4 + extra; break;

        case 0x85: cpuWrite(zp(), s.a); cyc = 3; break;
        case 0x95: cpuWrite(zpx(), s.a); cyc = 4; break;
        case 0x8D: cpuWrite(absA(), s.a); cyc = 4; break;
        case 0x9D: cpuWrite(absX(extra), s.a); cyc = 5; break;
        case 0x99: cpuWrite(absY(extra), s.a); cyc = 5; break;
        case 0x81: cpuWrite(indX(), s.a); cyc = 6; break;
        case 0x91: cpuWrite(indY(extra), s.a); cyc = 6; break;

        case 0x86: cpuWrite(zp(), s.x); cyc = 3; break;
        case 0x96: cpuWrite(zpy(), s.x); cyc = 4; break;
        case 0x8E: cpuWrite(absA(), s.x); cyc = 4; break;

        case 0x84: cpuWrite(zp(), s.y); cyc = 3; break;
        case 0x94: cpuWrite(zpx(), s.y); cyc = 4; break;
        case 0x8C: cpuWrite(absA(), s.y); cyc = 4; break;

        // ---- Transfers & Stack ----
        case 0xAA: s.x = s.a; setZN(s.x); cyc = 2; break;
        case 0xA8: s.y = s.a; setZN(s.y); cyc = 2; break;
        case 0xBA: s.x = s.sp; setZN(s.x); cyc = 2; break;
        case 0x8A: s.a = s.x; setZN(s.a); cyc = 2; break;
        case 0x9A: s.sp = s.x; cyc = 2; break;
        case 0x98: s.a = s.y; setZN(s.a); cyc = 2; break;

        case 0x48: push(s.a); cyc = 3; break;
        case 0x08: push(static_cast<uint8_t>(s.p | F_B | F_U)); cyc = 3; break;
        case 0x68: s.a = pop(); setZN(s.a); cyc = 4; break;
        case 0x28: s.p = static_cast<uint8_t>((pop() & ~F_B) | F_U); cyc = 4; break;

        // ---- Logic ----
        case 0x29: s.a &= cpuRead(imm()); setZN(s.a); cyc = 2; break;
        case 0x25: s.a &= cpuRead(zp()); setZN(s.a); cyc = 3; break;
        case 0x35: s.a &= cpuRead(zpx()); setZN(s.a); cyc = 4; break;
        case 0x2D: s.a &= cpuRead(absA()); setZN(s.a); cyc = 4; break;
        case 0x3D: s.a &= cpuRead(absX(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0x39: s.a &= cpuRead(absY(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0x21: s.a &= cpuRead(indX()); setZN(s.a); cyc = 6; break;
        case 0x31: s.a &= cpuRead(indY(extra)); setZN(s.a); cyc = 5 + extra; break;

        case 0x09: s.a |= cpuRead(imm()); setZN(s.a); cyc = 2; break;
        case 0x05: s.a |= cpuRead(zp()); setZN(s.a); cyc = 3; break;
        case 0x15: s.a |= cpuRead(zpx()); setZN(s.a); cyc = 4; break;
        case 0x0D: s.a |= cpuRead(absA()); setZN(s.a); cyc = 4; break;
        case 0x1D: s.a |= cpuRead(absX(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0x19: s.a |= cpuRead(absY(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0x01: s.a |= cpuRead(indX()); setZN(s.a); cyc = 6; break;
        case 0x11: s.a |= cpuRead(indY(extra)); setZN(s.a); cyc = 5 + extra; break;

        case 0x49: s.a ^= cpuRead(imm()); setZN(s.a); cyc = 2; break;
        case 0x45: s.a ^= cpuRead(zp()); setZN(s.a); cyc = 3; break;
        case 0x55: s.a ^= cpuRead(zpx()); setZN(s.a); cyc = 4; break;
        case 0x4D: s.a ^= cpuRead(absA()); setZN(s.a); cyc = 4; break;
        case 0x5D: s.a ^= cpuRead(absX(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0x59: s.a ^= cpuRead(absY(extra)); setZN(s.a); cyc = 4 + extra; break;
        case 0x41: s.a ^= cpuRead(indX()); setZN(s.a); cyc = 6; break;
        case 0x51: s.a ^= cpuRead(indY(extra)); setZN(s.a); cyc = 5 + extra; break;

        case 0x24: case 0x2C: {
            uint8_t v = cpuRead(op == 0x24 ? zp() : absA());
            s.p = static_cast<uint8_t>((s.p & ~(F_Z | F_V | F_N)) |
                                       ((s.a & v) == 0 ? F_Z : 0) | (v & (F_V | F_N)));
            cyc = (op == 0x24) ? 3 : 4;
            break;
        }

        // ---- Arithmetic & Compares ----
        case 0x69: doAdc(cpuRead(imm())); cyc = 2; break;
        case 0x65: doAdc(cpuRead(zp())); cyc = 3; break;
        case 0x75: doAdc(cpuRead(zpx())); cyc = 4; break;
        case 0x6D: doAdc(cpuRead(absA())); cyc = 4; break;
        case 0x7D: doAdc(cpuRead(absX(extra))); cyc = 4 + extra; break;
        case 0x79: doAdc(cpuRead(absY(extra))); cyc = 4 + extra; break;
        case 0x61: doAdc(cpuRead(indX())); cyc = 6; break;
        case 0x71: doAdc(cpuRead(indY(extra))); cyc = 5 + extra; break;

        case 0xE9: case 0xEB: doAdc(static_cast<uint8_t>(~cpuRead(imm()))); cyc = 2; break;
        case 0xE5: doAdc(static_cast<uint8_t>(~cpuRead(zp()))); cyc = 3; break;
        case 0xF5: doAdc(static_cast<uint8_t>(~cpuRead(zpx()))); cyc = 4; break;
        case 0xED: doAdc(static_cast<uint8_t>(~cpuRead(absA()))); cyc = 4; break;
        case 0xFD: doAdc(static_cast<uint8_t>(~cpuRead(absX(extra)))); cyc = 4 + extra; break;
        case 0xF9: doAdc(static_cast<uint8_t>(~cpuRead(absY(extra)))); cyc = 4 + extra; break;
        case 0xE1: doAdc(static_cast<uint8_t>(~cpuRead(indX()))); cyc = 6; break;
        case 0xF1: doAdc(static_cast<uint8_t>(~cpuRead(indY(extra)))); cyc = 5 + extra; break;

        case 0xC9: doCmp(s.a, cpuRead(imm())); cyc = 2; break;
        case 0xC5: doCmp(s.a, cpuRead(zp())); cyc = 3; break;
        case 0xD5: doCmp(s.a, cpuRead(zpx())); cyc = 4; break;
        case 0xCD: doCmp(s.a, cpuRead(absA())); cyc = 4; break;
        case 0xDD: doCmp(s.a, cpuRead(absX(extra))); cyc = 4 + extra; break;
        case 0xD9: doCmp(s.a, cpuRead(absY(extra))); cyc = 4 + extra; break;
        case 0xC1: doCmp(s.a, cpuRead(indX())); cyc = 6; break;
        case 0xD1: doCmp(s.a, cpuRead(indY(extra))); cyc = 5 + extra; break;

        case 0xE0: doCmp(s.x, cpuRead(imm())); cyc = 2; break;
        case 0xE4: doCmp(s.x, cpuRead(zp())); cyc = 3; break;
        case 0xEC: doCmp(s.x, cpuRead(absA())); cyc = 4; break;

        case 0xC0: doCmp(s.y, cpuRead(imm())); cyc = 2; break;
        case 0xC4: doCmp(s.y, cpuRead(zp())); cyc = 3; break;
        case 0xCC: doCmp(s.y, cpuRead(absA())); cyc = 4; break;

        // ---- Inc / Dec ----
        case 0xE6: case 0xF6: case 0xEE: case 0xFE: {
            uint16_t a = (op == 0xE6) ? zp() : (op == 0xF6) ? zpx() : (op == 0xEE) ? absA() : absX(extra);
            uint8_t v = static_cast<uint8_t>(cpuRead(a) + 1);
            cpuWrite(a, v); setZN(v);
            cyc = (op == 0xE6) ? 5 : (op == 0xFE) ? 7 : 6;
            break;
        }
        case 0xC6: case 0xD6: case 0xCE: case 0xDE: {
            uint16_t a = (op == 0xC6) ? zp() : (op == 0xD6) ? zpx() : (op == 0xCE) ? absA() : absX(extra);
            uint8_t v = static_cast<uint8_t>(cpuRead(a) - 1);
            cpuWrite(a, v); setZN(v);
            cyc = (op == 0xC6) ? 5 : (op == 0xDE) ? 7 : 6;
            break;
        }
        case 0xE8: s.x++; setZN(s.x); cyc = 2; break;
        case 0xC8: s.y++; setZN(s.y); cyc = 2; break;
        case 0xCA: s.x--; setZN(s.x); cyc = 2; break;
        case 0x88: s.y--; setZN(s.y); cyc = 2; break;

        // ---- Shifts & Rotates ----
        case 0x0A:
            s.p = static_cast<uint8_t>((s.p & ~F_C) | ((s.a >> 7) & 1));
            s.a <<= 1; setZN(s.a); cyc = 2; break;
        case 0x06: case 0x16: case 0x0E: case 0x1E: {
            uint16_t a = (op == 0x06) ? zp() : (op == 0x16) ? zpx() : (op == 0x0E) ? absA() : absX(extra);
            uint8_t v = cpuRead(a);
            s.p = static_cast<uint8_t>((s.p & ~F_C) | ((v >> 7) & 1));
            v <<= 1; cpuWrite(a, v); setZN(v);
            cyc = (op == 0x06) ? 5 : (op == 0x1E) ? 7 : 6;
            break;
        }
        case 0x4A:
            s.p = static_cast<uint8_t>((s.p & ~F_C) | (s.a & 1));
            s.a >>= 1; setZN(s.a); cyc = 2; break;
        case 0x46: case 0x56: case 0x4E: case 0x5E: {
            uint16_t a = (op == 0x46) ? zp() : (op == 0x56) ? zpx() : (op == 0x4E) ? absA() : absX(extra);
            uint8_t v = cpuRead(a);
            s.p = static_cast<uint8_t>((s.p & ~F_C) | (v & 1));
            v >>= 1; cpuWrite(a, v); setZN(v);
            cyc = (op == 0x46) ? 5 : (op == 0x5E) ? 7 : 6;
            break;
        }
        case 0x2A: {
            uint8_t c = s.p & F_C;
            s.p = static_cast<uint8_t>((s.p & ~F_C) | ((s.a >> 7) & 1));
            s.a = static_cast<uint8_t>((s.a << 1) | c); setZN(s.a); cyc = 2; break;
        }
        case 0x26: case 0x36: case 0x2E: case 0x3E: {
            uint16_t a = (op == 0x26) ? zp() : (op == 0x36) ? zpx() : (op == 0x2E) ? absA() : absX(extra);
            uint8_t v = cpuRead(a), c = s.p & F_C;
            s.p = static_cast<uint8_t>((s.p & ~F_C) | ((v >> 7) & 1));
            v = static_cast<uint8_t>((v << 1) | c); cpuWrite(a, v); setZN(v);
            cyc = (op == 0x26) ? 5 : (op == 0x3E) ? 7 : 6;
            break;
        }
        case 0x6A: {
            uint8_t c = static_cast<uint8_t>((s.p & F_C) << 7);
            s.p = static_cast<uint8_t>((s.p & ~F_C) | (s.a & 1));
            s.a = static_cast<uint8_t>((s.a >> 1) | c); setZN(s.a); cyc = 2; break;
        }
        case 0x66: case 0x76: case 0x6E: case 0x7E: {
            uint16_t a = (op == 0x66) ? zp() : (op == 0x76) ? zpx() : (op == 0x6E) ? absA() : absX(extra);
            uint8_t v = cpuRead(a), c = static_cast<uint8_t>((s.p & F_C) << 7);
            s.p = static_cast<uint8_t>((s.p & ~F_C) | (v & 1));
            v = static_cast<uint8_t>((v >> 1) | c); cpuWrite(a, v); setZN(v);
            cyc = (op == 0x66) ? 5 : (op == 0x7E) ? 7 : 6;
            break;
        }

        // ---- Branches ----
        case 0x10: cyc = branch((s.p & F_N) == 0); break;
        case 0x30: cyc = branch((s.p & F_N) != 0); break;
        case 0x50: cyc = branch((s.p & F_V) == 0); break;
        case 0x70: cyc = branch((s.p & F_V) != 0); break;
        case 0x90: cyc = branch((s.p & F_C) == 0); break;
        case 0xB0: cyc = branch((s.p & F_C) != 0); break;
        case 0xD0: cyc = branch((s.p & F_Z) == 0); break;
        case 0xF0: cyc = branch((s.p & F_Z) != 0); break;

        // ---- Jumps, Subroutines, Interrupts ----
        case 0x4C: s.pc = absA(); cyc = 3; break;
        case 0x6C: { // Authentic 6502 JMP ($xxFF) page wrap quirk
            uint16_t ptr = absA();
            uint16_t hiAddr = (ptr & 0xFF00) | ((ptr + 1) & 0x00FF);
            s.pc = static_cast<uint16_t>(cpuRead(ptr) | (static_cast<uint16_t>(cpuRead(hiAddr)) << 8));
            cyc = 5;
            break;
        }
        case 0x20: {
            uint16_t target = absA();
            push16(static_cast<uint16_t>(s.pc - 1));
            s.pc = target;
            cyc = 6;
            break;
        }
        case 0x60: s.pc = static_cast<uint16_t>(pop16() + 1); cyc = 6; break;
        case 0x40:
            s.p = static_cast<uint8_t>((pop() & ~F_B) | F_U);
            s.pc = pop16();
            cyc = 6;
            break;
        case 0x00: // BRK
            s.pc++;
            push16(s.pc);
            push(static_cast<uint8_t>(s.p | F_B | F_U));
            s.p |= F_I;
            s.pc = cpuRead16(0xFFFE);
            cyc = 7;
            break;

        // ---- Status Flags ----
        case 0x18: s.p &= ~F_C; cyc = 2; break;
        case 0x38: s.p |= F_C;  cyc = 2; break;
        case 0x58: s.p &= ~F_I; cyc = 2; break;
        case 0x78: s.p |= F_I;  cyc = 2; break;
        case 0xB8: s.p &= ~F_V; cyc = 2; break;
        case 0xD8: s.p &= ~F_D; cyc = 2; break;
        case 0xF8: s.p |= F_D;  cyc = 2; break;

        // ---- Official & Common Unofficial NOPs / LAX / SAX / DCP / ISB ----
        case 0xEA: case 0x1A: case 0x3A: case 0x5A: case 0x7A: case 0xDA: case 0xFA:
            cyc = 2; break;
        case 0x80: case 0x82: case 0x89: case 0xC2: case 0xE2:
            s.pc++; cyc = 2; break;
        case 0x04: case 0x44: case 0x64:
            s.pc++; cyc = 3; break;
        case 0x14: case 0x34: case 0x54: case 0x74: case 0xD4: case 0xF4:
            s.pc++; cyc = 4; break;
        case 0x0C:
            s.pc += 2; cyc = 4; break;
        case 0x1C: case 0x3C: case 0x5C: case 0x7C: case 0xDC: case 0xFC:
            absX(extra); cyc = 4 + extra; break;
        case 0xA7: case 0xB7: case 0xAF: case 0xBF: case 0xA3: case 0xB3: { // LAX
            uint16_t a = (op == 0xA7) ? zp() : (op == 0xB7) ? zpy() : (op == 0xAF) ? absA() :
                         (op == 0xBF) ? absY(extra) : (op == 0xA3) ? indX() : indY(extra);
            s.a = s.x = cpuRead(a); setZN(s.a); cyc = 4 + extra; break;
        }
        case 0x87: case 0x97: case 0x8F: case 0x83: { // SAX
            uint16_t a = (op == 0x87) ? zp() : (op == 0x97) ? zpy() : (op == 0x8F) ? absA() : indX();
            cpuWrite(a, static_cast<uint8_t>(s.a & s.x)); cyc = 4; break;
        }
        case 0xC7: case 0xD7: case 0xCF: case 0xDF: case 0xDB: case 0xC3: case 0xD3: { // DCP
            uint16_t a = (op == 0xC7) ? zp() : (op == 0xD7) ? zpx() : (op == 0xCF) ? absA() :
                         (op == 0xDF) ? absX(extra) : (op == 0xDB) ? absY(extra) : (op == 0xC3) ? indX() : indY(extra);
            uint8_t v = static_cast<uint8_t>(cpuRead(a) - 1);
            cpuWrite(a, v); doCmp(s.a, v); cyc = 6; break;
        }
        case 0xE7: case 0xF7: case 0xEF: case 0xFF: case 0xFB: case 0xE3: case 0xF3: { // ISB
            uint16_t a = (op == 0xE7) ? zp() : (op == 0xF7) ? zpx() : (op == 0xEF) ? absA() :
                         (op == 0xFF) ? absX(extra) : (op == 0xFB) ? absY(extra) : (op == 0xE3) ? indX() : indY(extra);
            uint8_t v = static_cast<uint8_t>(cpuRead(a) + 1);
            cpuWrite(a, v); doAdc(static_cast<uint8_t>(~v)); cyc = 6; break;
        }
        default:
            cyc = 2;
            break;
    }

    s.cpuCycles += static_cast<uint64_t>(cyc);
    return cyc;
}

void runCpuCycles(int targetCycles) {
    int spent = 0;
    while (spent < targetCycles) {
        int c = cpuStep();
        apuStepCpuCycles(c);
        spent += c;
    }
}

// ============================================================================
// Frame Execution (262 NTSC Scanlines)
// ============================================================================

uint8_t pollNesPad(unsigned port) {
    if (!gNes.inputState) return 0;
    uint8_t m = 0;
    bool turboOn = ((gNes.s.frameCounter / 3u) & 1u) == 0u;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A) ||
        (turboOn && gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_X))) m |= 0x01;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_B) ||
        (turboOn && gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_Y))) m |= 0x02;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_SELECT)) m |= 0x04;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_START))  m |= 0x08;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_UP))     m |= 0x10;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_DOWN))   m |= 0x20;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_LEFT))   m |= 0x40;
    if (gNes.inputState(port, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_RIGHT))  m |= 0x80;
    // Prevent physically impossible simultaneous opposite directions
    if ((m & 0x30) == 0x30) m &= ~0x30;
    if ((m & 0xC0) == 0xC0) m &= ~0xC0;
    return m;
}

void nesReset() {
    auto &s = gNes.s;
    uint8_t savedWram[8192];
    memcpy(savedWram, s.wram, sizeof(savedWram));
    uint8_t mapper = s.mapper;
    uint8_t mirror = s.mirror;

    memset(&s, 0, sizeof(s));
    memcpy(s.wram, savedWram, sizeof(savedWram));
    s.magic = kStateMagic;
    s.version = kStateVersion;
    s.mapper = mapper;
    s.mirror = mirror;
    s.mmc1Ctrl = 0x0C;
    s.noise.lfsr = 1;
    s.dmc.bitsRemaining = 8;
    s.dmc.silence = true;

    s.p = F_I | F_U;
    s.sp = 0xFD;
    s.pc = cpuRead16(0xFFFC);
}

void runNesFrame() {
    auto &s = gNes.s;
    gNes.audioFrames = 0;

    if (gNes.inputPoll) gNes.inputPoll();
    s.joyState[0] = pollNesPad(0);
    s.joyState[1] = pollNesPad(1);
    if (s.joyStrobe & 1) {
        s.joyShift[0] = s.joyState[0];
        s.joyShift[1] = s.joyState[1];
    }

    int fracDots = 0;
    auto scanlineCycles = [&]() -> int {
        fracDots += 341;
        int cyc = fracDots / 3;
        fracDots -= cyc * 3;
        return cyc;
    };

    // Visible scanlines 0..239
    for (int y = 0; y < 240; y++) {
        const bool rendering = (s.mask & 0x18) != 0;
        const int lineCyc = scanlineCycles();
        int hitX = renderScanline(y);

        if (hitX >= 0 && !(s.status & 0x40)) {
            int preHitCyc = std::clamp(hitX / 3, 1, lineCyc - 28);
            runCpuCycles(preHitCyc);
            s.status |= 0x40; // Sprite 0 Hit at exact dot!
            int toHblank = std::max(0, 86 - preHitCyc);
            runCpuCycles(toHblank);
            if (rendering) {
                ppuIncY(s.v);
                s.v = (s.v & ~0x041Fu) | (s.t & 0x041Fu);
                clockMmc3ScanlineIrq();
            }
            runCpuCycles(std::max(0, lineCyc - preHitCyc - toHblank));
        } else {
            runCpuCycles(86);
            if (rendering) {
                ppuIncY(s.v);
                s.v = (s.v & ~0x041Fu) | (s.t & 0x041Fu);
                clockMmc3ScanlineIrq();
            }
            runCpuCycles(lineCyc - 86);
        }
    }

    // Scanline 240: post-render idle
    runCpuCycles(scanlineCycles());

    // Scanline 241: VBlank starts at dot 1 -> NMI
    s.status |= 0x80;
    if (s.ctrl & 0x80) s.nmiPending = true;
    runCpuCycles(scanlineCycles());

    // Scanlines 242..260: VBlank
    for (int y = 242; y <= 260; y++) {
        runCpuCycles(scanlineCycles());
    }

    // Scanline 261: pre-render (clear VBlank, Sprite 0 Hit, Overflow; copy vertical scroll)
    s.status &= 0x1F;
    runCpuCycles(93);
    if (s.mask & 0x18) {
        s.v = (s.v & ~0x7BE0u) | (s.t & 0x7BE0u);
        s.v = (s.v & ~0x041Fu) | (s.t & 0x041Fu);
    }
    runCpuCycles(scanlineCycles() - 93);

    s.oddFrame = !s.oddFrame;
    s.frameCounter++;

    if (gNes.video) gNes.video(gNes.fb, kNesW, kNesH, kNesW * sizeof(uint32_t));
    if (gNes.audioBatch && gNes.audioFrames > 0) gNes.audioBatch(gNes.audioBuf, gNes.audioFrames);
}

// ============================================================================
// Libretro Core API Entry Points
// ============================================================================

void nesInit() {
    initMixerTables();
    memset(&gNes.s, 0, sizeof(gNes.s));
    gNes.prgRom.clear();
    gNes.chrRom.clear();
}

void nesDeinit() {
    gNes.prgRom.clear();
    gNes.chrRom.clear();
}

unsigned nesApiVersion() { return RETRO_API_VERSION; }

void nesGetSystemInfo(retro_system_info *info) {
    memset(info, 0, sizeof(*info));
    info->library_name = "HeroNES";
    info->library_version = "1.0";
    info->valid_extensions = "nes";
    info->need_fullpath = false;
    info->block_extract = false;
}

void nesGetSystemAvInfo(retro_system_av_info *info) {
    memset(info, 0, sizeof(*info));
    info->geometry.base_width = kNesW;
    info->geometry.base_height = kNesH;
    info->geometry.max_width = kNesW;
    info->geometry.max_height = kNesH;
    info->geometry.aspect_ratio = 4.0f / 3.0f;
    info->timing.fps = kNesFps;
    info->timing.sample_rate = kSampleRate;
}

void nesSetEnv(retro_environment_t cb) { gNes.env = cb; }
void nesSetVideo(retro_video_refresh_t cb) { gNes.video = cb; }
void nesSetAudioSample(retro_audio_sample_t) {}
void nesSetAudioBatch(retro_audio_sample_batch_t cb) { gNes.audioBatch = cb; }
void nesSetInputPoll(retro_input_poll_t cb) { gNes.inputPoll = cb; }
void nesSetInputState(retro_input_state_t cb) { gNes.inputState = cb; }

bool nesLoadGame(const retro_game_info *info) {
    if (!info) return false;
    initMixerTables();
    if (gNes.env) {
        int fmt = RETRO_PIXEL_FORMAT_XRGB8888;
        gNes.env(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt);
    }

    std::vector<uint8_t> raw;
    if (info->data && info->size >= 16) {
        const auto *p = static_cast<const uint8_t *>(info->data);
        raw.assign(p, p + info->size);
    } else if (info->path) {
        std::ifstream f(info->path, std::ios::binary | std::ios::ate);
        if (!f) return false;
        auto sz = f.tellg();
        if (sz < 16) return false;
        raw.resize(static_cast<size_t>(sz));
        f.seekg(0);
        f.read(reinterpret_cast<char *>(raw.data()), sz);
    } else {
        return false;
    }

    if (raw.size() < 16 || raw[0] != 'N' || raw[1] != 'E' || raw[2] != 'S' || raw[3] != 0x1A) {
        LOGE("HeroNES: not a valid iNES (.nes) ROM");
        return false;
    }

    uint32_t prg16k = raw[4];
    uint32_t chr8k = raw[5];
    uint8_t f6 = raw[6];
    uint8_t f7 = raw[7];
    bool nes2 = ((f7 & 0x0C) == 0x08);
    bool dirtyHeader = !nes2 && (raw[12] != 0 || raw[13] != 0 || raw[14] != 0 || raw[15] != 0);
    uint8_t mapper = dirtyHeader ? static_cast<uint8_t>(f6 >> 4)
                                 : static_cast<uint8_t>((f6 >> 4) | (f7 & 0xF0));

    // Verify mapper support
    if (mapper != 0 && mapper != 1 && mapper != 2 && mapper != 3 && mapper != 4 && mapper != 7 && mapper != 66) {
        LOGE("HeroNES: unsupported iNES mapper %u (switch to FCEUmm/Nestopia for this ROM)", mapper);
        return false;
    }

    size_t offset = 16 + ((f6 & 0x04) ? 512 : 0);
    size_t prgBytes = static_cast<size_t>(prg16k) * 16384u;
    size_t chrBytes = static_cast<size_t>(chr8k) * 8192u;
    if (prg16k == 0 || offset + prgBytes > raw.size()) {
        LOGE("HeroNES: truncated iNES ROM (need %zu bytes, got %zu)", offset + prgBytes, raw.size());
        return false;
    }

    gNes.prgRom.assign(raw.begin() + offset, raw.begin() + offset + prgBytes);
    gNes.prgBankCount16k = prg16k;
    gNes.prgBankCount8k = prg16k * 2u;

    if (chr8k > 0 && offset + prgBytes + chrBytes <= raw.size()) {
        gNes.chrRom.assign(raw.begin() + offset + prgBytes, raw.begin() + offset + prgBytes + chrBytes);
        gNes.hasChrRam = false;
        gNes.chrBankCount8k = chr8k;
        gNes.chrBankCount1k = chr8k * 8u;
    } else {
        gNes.chrRom.clear();
        gNes.hasChrRam = true;
        gNes.chrBankCount8k = 1;
        gNes.chrBankCount1k = 8;
    }

    gNes.hasBattery = (f6 & 0x02) != 0;
    memset(gNes.s.wram, 0, sizeof(gNes.s.wram));
    gNes.s.mapper = mapper;
    gNes.s.mirror = (f6 & 0x08) ? MIRROR_FOUR : ((f6 & 0x01) ? MIRROR_VERTICAL : MIRROR_HORIZONTAL);
    nesReset();
    LOGI("HeroNES loaded ROM: mapper=%u PRG=%uKB CHR=%uKB mirror=%u",
         mapper, prg16k * 16u, chr8k * 8u, gNes.s.mirror);
    return true;
}

void nesUnloadGame() {
    gNes.prgRom.clear();
    gNes.chrRom.clear();
}

size_t nesSerializeSize() { return sizeof(NesState); }

bool nesSerialize(void *data, size_t size) {
    if (!data || size < sizeof(NesState)) return false;
    gNes.s.magic = kStateMagic;
    gNes.s.version = kStateVersion;
    memcpy(data, &gNes.s, sizeof(NesState));
    return true;
}

bool nesUnserialize(const void *data, size_t size) {
    if (!data || size < sizeof(NesState)) return false;
    NesState tmp{};
    memcpy(&tmp, data, sizeof(NesState));
    if (tmp.magic != kStateMagic || tmp.version != kStateVersion) return false;
    gNes.s = tmp;
    return true;
}

void *nesGetMemoryData(unsigned id) {
    if (id == RETRO_MEMORY_SYSTEM_RAM) return gNes.s.ram;
    if (id == RETRO_MEMORY_SAVE_RAM) return gNes.s.wram;
    return nullptr;
}

size_t nesGetMemorySize(unsigned id) {
    if (id == RETRO_MEMORY_SYSTEM_RAM) return sizeof(gNes.s.ram);
    if (id == RETRO_MEMORY_SAVE_RAM) return sizeof(gNes.s.wram);
    return 0;
}

}  // namespace

CoreApi heroNesApi() {
    CoreApi api{};
    api.init = nesInit;
    api.deinit = nesDeinit;
    api.api_version = nesApiVersion;
    api.get_system_info = nesGetSystemInfo;
    api.get_system_av_info = nesGetSystemAvInfo;
    api.set_environment = nesSetEnv;
    api.set_video_refresh = nesSetVideo;
    api.set_audio_sample = nesSetAudioSample;
    api.set_audio_sample_batch = nesSetAudioBatch;
    api.set_input_poll = nesSetInputPoll;
    api.set_input_state = nesSetInputState;
    api.reset = nesReset;
    api.run = runNesFrame;
    api.serialize_size = nesSerializeSize;
    api.serialize = nesSerialize;
    api.unserialize = nesUnserialize;
    api.load_game = nesLoadGame;
    api.unload_game = nesUnloadGame;
    api.get_memory_data = nesGetMemoryData;
    api.get_memory_size = nesGetMemorySize;
    return api;
}

}  // namespace rh::builtin
