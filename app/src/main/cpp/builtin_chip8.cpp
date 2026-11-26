#include "builtin_cores.h"

#include <cmath>
#include <cstdint>
#include <cstring>

namespace rh::builtin {

namespace {

constexpr unsigned kW = 128;
constexpr unsigned kH = 64;
constexpr unsigned kSampleRate = 44100;
constexpr unsigned kAudioFramesPerTick = kSampleRate / 60;  // 735 stereo frames @ 60 Hz

struct Chip8State {
    uint8_t mem[4096];
    uint8_t v[16];
    uint16_t i;
    uint16_t pc;
    uint16_t stack[16];
    uint8_t sp;
    uint8_t dt;
    uint8_t st;
    uint8_t rpl[8];
    uint8_t fb[kW * kH];  // 0 or 1
    uint8_t hires;
    uint32_t rng;
    float buzzPhase;
    uint8_t prevKeys[16];
    uint8_t waitKeyReg;  // 0xFF if not waiting
};

static const uint8_t kFont4x5[80] = {
    0xF0, 0x90, 0x90, 0x90, 0xF0, 0x20, 0x60, 0x20, 0x20, 0x70,
    0xF0, 0x10, 0xF0, 0x80, 0xF0, 0xF0, 0x10, 0xF0, 0x10, 0xF0,
    0x90, 0x90, 0xF0, 0x10, 0x10, 0xF0, 0x80, 0xF0, 0x10, 0xF0,
    0xF0, 0x80, 0xF0, 0x90, 0xF0, 0xF0, 0x10, 0x20, 0x40, 0x40,
    0xF0, 0x90, 0xF0, 0x90, 0xF0, 0xF0, 0x90, 0xF0, 0x10, 0xF0,
    0xF0, 0x90, 0xF0, 0x90, 0x90, 0xE0, 0x90, 0xE0, 0x90, 0xE0,
    0xF0, 0x80, 0x80, 0x80, 0xF0, 0xE0, 0x90, 0x90, 0x90, 0xE0,
    0xF0, 0x80, 0xF0, 0x80, 0xF0, 0xF0, 0x80, 0xF0, 0x80, 0x80,
};

struct Chip8Runtime {
    retro_environment_t env = nullptr;
    retro_video_refresh_t video = nullptr;
    retro_audio_sample_batch_t audioBatch = nullptr;
    retro_input_poll_t inputPoll = nullptr;
    retro_input_state_t inputState = nullptr;

    Chip8State s{};
    uint8_t rom[3584]{};
    size_t romSize = 0;
    uint32_t pixels[kW * kH]{};
    int16_t audioBuf[kAudioFramesPerTick * 2]{};
};

Chip8Runtime gC8;

uint8_t nextRand() {
    gC8.s.rng = gC8.s.rng * 1664525u + 1013904223u;
    return static_cast<uint8_t>((gC8.s.rng >> 16) & 0xFF);
}

void resetMachine() {
    memset(&gC8.s, 0, sizeof(gC8.s));
    memcpy(gC8.s.mem + 0x050, kFont4x5, sizeof(kFont4x5));
    // Also duplicate scaled 8x10 font at 0x0A0 for SUPER-CHIP FX30
    for (int ch = 0; ch < 16; ch++) {
        for (int row = 0; row < 5; row++) {
            uint8_t b = kFont4x5[ch * 5 + row];
            gC8.s.mem[0x0A0 + ch * 10 + row * 2] = b;
            gC8.s.mem[0x0A0 + ch * 10 + row * 2 + 1] = b;
        }
    }
    if (gC8.romSize > 0) {
        size_t n = gC8.romSize > (4096 - 0x200) ? (4096 - 0x200) : gC8.romSize;
        memcpy(gC8.s.mem + 0x200, gC8.rom, n);
    }
    gC8.s.pc = 0x200;
    gC8.s.rng = 0xC816BEEFu;
    gC8.s.waitKeyReg = 0xFF;
}

void pollKeypad(uint8_t keys[16]) {
    memset(keys, 0, 16);
    if (!gC8.inputState) return;
    auto btn = [&](unsigned id) -> bool {
        return gC8.inputState(0, RETRO_DEVICE_JOYPAD, 0, id) != 0;
    };
    // Intuitive RetroPad -> CHIP-8 hex keypad mapping for classic games (Pong, Breakout, Tetris, Invaders)
    if (btn(RETRO_DEVICE_ID_JOYPAD_UP))     { keys[0x2] = 1; keys[0x1] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_DOWN))   { keys[0x8] = 1; keys[0x4] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_LEFT))   { keys[0x4] = 1; keys[0x7] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_RIGHT))  { keys[0x6] = 1; keys[0x9] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_A))      { keys[0x5] = 1; keys[0x6] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_B))      { keys[0x0] = 1; keys[0x5] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_X))      { keys[0xC] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_Y))      { keys[0xD] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_L))      { keys[0xA] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_R))      { keys[0xB] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_START))  { keys[0x5] = 1; keys[0xE] = 1; }
    if (btn(RETRO_DEVICE_ID_JOYPAD_SELECT)) { keys[0xF] = 1; }
}

void stepCpu(const uint8_t keys[16]) {
    auto &s = gC8.s;
    if (s.waitKeyReg != 0xFF) {
        for (uint8_t k = 0; k < 16; k++) {
            if (keys[k] && !s.prevKeys[k]) {
                s.v[s.waitKeyReg] = k;
                s.waitKeyReg = 0xFF;
                break;
            }
        }
        return;
    }

    uint16_t op = (static_cast<uint16_t>(s.mem[s.pc & 0xFFF]) << 8) | s.mem[(s.pc + 1) & 0xFFF];
    s.pc = (s.pc + 2) & 0xFFF;

    uint16_t nnn = op & 0x0FFF;
    uint8_t nn = static_cast<uint8_t>(op & 0x00FF);
    uint8_t n = static_cast<uint8_t>(op & 0x000F);
    uint8_t x = static_cast<uint8_t>((op >> 8) & 0x0F);
    uint8_t y = static_cast<uint8_t>((op >> 4) & 0x0F);

    const unsigned w = s.hires ? 128 : 64;
    const unsigned h = s.hires ? 64 : 32;

    switch (op & 0xF000) {
        case 0x0000:
            if ((op & 0xFFF0) == 0x00C0) {
                // Scroll down N lines
                if (n > 0 && n < h) {
                    memmove(s.fb + n * kW, s.fb, (h - n) * kW);
                    memset(s.fb, 0, n * kW);
                }
            } else if (op == 0x00E0) {
                memset(s.fb, 0, sizeof(s.fb));
            } else if (op == 0x00EE) {
                if (s.sp > 0) s.pc = s.stack[--s.sp];
            } else if (op == 0x00FB) {
                for (unsigned row = 0; row < h; row++) {
                    memmove(s.fb + row * kW + 4, s.fb + row * kW, w - 4);
                    memset(s.fb + row * kW, 0, 4);
                }
            } else if (op == 0x00FC) {
                for (unsigned row = 0; row < h; row++) {
                    memmove(s.fb + row * kW, s.fb + row * kW + 4, w - 4);
                    memset(s.fb + row * kW + w - 4, 0, 4);
                }
            } else if (op == 0x00FE) {
                s.hires = 0;
            } else if (op == 0x00FF) {
                s.hires = 1;
            }
            break;
        case 0x1000: s.pc = nnn; break;
        case 0x2000:
            if (s.sp < 16) s.stack[s.sp++] = s.pc;
            s.pc = nnn;
            break;
        case 0x3000: if (s.v[x] == nn) s.pc = (s.pc + 2) & 0xFFF; break;
        case 0x4000: if (s.v[x] != nn) s.pc = (s.pc + 2) & 0xFFF; break;
        case 0x5000: if (s.v[x] == s.v[y]) s.pc = (s.pc + 2) & 0xFFF; break;
        case 0x6000: s.v[x] = nn; break;
        case 0x7000: s.v[x] = static_cast<uint8_t>(s.v[x] + nn); break;
        case 0x8000:
            switch (n) {
                case 0x0: s.v[x] = s.v[y]; break;
                case 0x1: s.v[x] |= s.v[y]; break;
                case 0x2: s.v[x] &= s.v[y]; break;
                case 0x3: s.v[x] ^= s.v[y]; break;
                case 0x4: {
                    uint16_t sum = static_cast<uint16_t>(s.v[x]) + s.v[y];
                    s.v[x] = static_cast<uint8_t>(sum);
                    s.v[0xF] = sum > 0xFF ? 1 : 0;
                    break;
                }
                case 0x5: {
                    uint8_t cf = s.v[x] >= s.v[y] ? 1 : 0;
                    s.v[x] = static_cast<uint8_t>(s.v[x] - s.v[y]);
                    s.v[0xF] = cf;
                    break;
                }
                case 0x6: {
                    uint8_t cf = s.v[x] & 1;
                    s.v[x] >>= 1;
                    s.v[0xF] = cf;
                    break;
                }
                case 0x7: {
                    uint8_t cf = s.v[y] >= s.v[x] ? 1 : 0;
                    s.v[x] = static_cast<uint8_t>(s.v[y] - s.v[x]);
                    s.v[0xF] = cf;
                    break;
                }
                case 0xE: {
                    uint8_t cf = (s.v[x] >> 7) & 1;
                    s.v[x] <<= 1;
                    s.v[0xF] = cf;
                    break;
                }
            }
            break;
        case 0x9000: if (s.v[x] != s.v[y]) s.pc = (s.pc + 2) & 0xFFF; break;
        case 0xA000: s.i = nnn; break;
        case 0xB000: s.pc = static_cast<uint16_t>((nnn + s.v[0]) & 0xFFF); break;
        case 0xC000: s.v[x] = nextRand() & nn; break;
        case 0xD000: {
            unsigned sx = s.v[x] % w;
            unsigned sy = s.v[y] % h;
            s.v[0xF] = 0;
            if (n == 0 && s.hires) {
                for (unsigned row = 0; row < 16; row++) {
                    unsigned py = sy + row;
                    if (py >= h) break;
                    uint16_t bits = (static_cast<uint16_t>(s.mem[(s.i + row * 2) & 0xFFF]) << 8) |
                                    s.mem[(s.i + row * 2 + 1) & 0xFFF];
                    for (unsigned col = 0; col < 16; col++) {
                        unsigned px = sx + col;
                        if (px >= w) break;
                        if (bits & (0x8000u >> col)) {
                            uint8_t &dst = s.fb[py * kW + px];
                            if (dst) s.v[0xF] = 1;
                            dst ^= 1;
                        }
                    }
                }
            } else {
                unsigned rows = n == 0 ? 16 : n;
                for (unsigned row = 0; row < rows; row++) {
                    unsigned py = sy + row;
                    if (py >= h) break;
                    uint8_t bits = s.mem[(s.i + row) & 0xFFF];
                    for (unsigned col = 0; col < 8; col++) {
                        unsigned px = sx + col;
                        if (px >= w) break;
                        if (bits & (0x80u >> col)) {
                            uint8_t &dst = s.fb[py * kW + px];
                            if (dst) s.v[0xF] = 1;
                            dst ^= 1;
                        }
                    }
                }
            }
            break;
        }
        case 0xE000:
            if (nn == 0x9E && keys[s.v[x] & 0xF]) s.pc = (s.pc + 2) & 0xFFF;
            else if (nn == 0xA1 && !keys[s.v[x] & 0xF]) s.pc = (s.pc + 2) & 0xFFF;
            break;
        case 0xF000:
            switch (nn) {
                case 0x07: s.v[x] = s.dt; break;
                case 0x0A: s.waitKeyReg = x; break;
                case 0x15: s.dt = s.v[x]; break;
                case 0x18: s.st = s.v[x]; break;
                case 0x1E: s.i = static_cast<uint16_t>((s.i + s.v[x]) & 0xFFF); break;
                case 0x29: s.i = static_cast<uint16_t>(0x050 + (s.v[x] & 0xF) * 5); break;
                case 0x30: s.i = static_cast<uint16_t>(0x0A0 + (s.v[x] & 0xF) * 10); break;
                case 0x33:
                    s.mem[s.i & 0xFFF] = s.v[x] / 100;
                    s.mem[(s.i + 1) & 0xFFF] = (s.v[x] / 10) % 10;
                    s.mem[(s.i + 2) & 0xFFF] = s.v[x] % 10;
                    break;
                case 0x55:
                    for (uint8_t r = 0; r <= x; r++) s.mem[(s.i + r) & 0xFFF] = s.v[r];
                    break;
                case 0x65:
                    for (uint8_t r = 0; r <= x; r++) s.v[r] = s.mem[(s.i + r) & 0xFFF];
                    break;
                case 0x75:
                    for (uint8_t r = 0; r <= (x & 7); r++) s.rpl[r] = s.v[r];
                    break;
                case 0x85:
                    for (uint8_t r = 0; r <= (x & 7); r++) s.v[r] = s.rpl[r];
                    break;
            }
            break;
    }
}

void c8Init() {}
void c8Deinit() {}
unsigned c8ApiVersion() { return RETRO_API_VERSION; }

void c8GetSystemInfo(retro_system_info *info) {
    info->library_name = "RetroHero CHIP-8";
    info->library_version = "1.0";
    info->valid_extensions = "ch8|sc8|c8";
    info->need_fullpath = false;
    info->block_extract = false;
}

void c8GetSystemAvInfo(retro_system_av_info *av) {
    av->geometry.base_width = kW;
    av->geometry.base_height = kH;
    av->geometry.max_width = kW;
    av->geometry.max_height = kH;
    av->geometry.aspect_ratio = 2.0f;
    av->timing.fps = 60.0;
    av->timing.sample_rate = static_cast<double>(kSampleRate);
}

void c8SetEnv(retro_environment_t cb) { gC8.env = cb; }
void c8SetVideo(retro_video_refresh_t cb) { gC8.video = cb; }
void c8SetAudioSample(retro_audio_sample_t) {}
void c8SetAudioBatch(retro_audio_sample_batch_t cb) { gC8.audioBatch = cb; }
void c8SetInputPoll(retro_input_poll_t cb) { gC8.inputPoll = cb; }
void c8SetInputState(retro_input_state_t cb) { gC8.inputState = cb; }

bool c8LoadGame(const retro_game_info *game) {
    if (!game || !game->data || game->size == 0) return false;
    if (gC8.env) {
        enum retro_pixel_format fmt = RETRO_PIXEL_FORMAT_XRGB8888;
        gC8.env(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt);
    }
    gC8.romSize = game->size > sizeof(gC8.rom) ? sizeof(gC8.rom) : game->size;
    memcpy(gC8.rom, game->data, gC8.romSize);
    resetMachine();
    return true;
}

void c8UnloadGame() { gC8.romSize = 0; }
void c8Reset() { resetMachine(); }

void c8Run() {
    if (gC8.inputPoll) gC8.inputPoll();
    uint8_t keys[16];
    pollKeypad(keys);

    const int cycles = gC8.s.hires ? 24 : 12;
    for (int i = 0; i < cycles; i++) stepCpu(keys);
    memcpy(gC8.s.prevKeys, keys, 16);

    if (gC8.s.dt > 0) gC8.s.dt--;
    const bool buzz = gC8.s.st > 0;
    if (gC8.s.st > 0) gC8.s.st--;

    // Render warm amber-mint phosphor pixels into 128x64 buffer
    constexpr uint32_t kBg = 0xFF141820u;
    constexpr uint32_t kFg = 0xFFD0BCFFu;
    if (gC8.s.hires) {
        for (unsigned i = 0; i < kW * kH; i++) {
            gC8.pixels[i] = gC8.s.fb[i] ? kFg : kBg;
        }
    } else {
        for (unsigned y = 0; y < 32; y++) {
            for (unsigned x = 0; x < 64; x++) {
                uint32_t c = gC8.s.fb[y * kW + x] ? kFg : kBg;
                unsigned dy = y * 2;
                unsigned dx = x * 2;
                gC8.pixels[dy * kW + dx] = c;
                gC8.pixels[dy * kW + dx + 1] = c;
                gC8.pixels[(dy + 1) * kW + dx] = c;
                gC8.pixels[(dy + 1) * kW + dx + 1] = c;
            }
        }
    }
    if (gC8.video) gC8.video(gC8.pixels, kW, kH, kW * sizeof(uint32_t));

    // Synthesize crisp 440 Hz triangle-square buzz when sound timer > 0
    for (unsigned i = 0; i < kAudioFramesPerTick; i++) {
        int16_t sample = 0;
        if (buzz) {
            gC8.s.buzzPhase += 440.0f / static_cast<float>(kSampleRate);
            if (gC8.s.buzzPhase >= 1.0f) gC8.s.buzzPhase -= 1.0f;
            sample = (gC8.s.buzzPhase < 0.5f) ? 4800 : -4800;
        }
        gC8.audioBuf[i * 2] = sample;
        gC8.audioBuf[i * 2 + 1] = sample;
    }
    if (gC8.audioBatch) gC8.audioBatch(gC8.audioBuf, kAudioFramesPerTick);
}

size_t c8SerializeSize() { return sizeof(Chip8State); }
bool c8Serialize(void *data, size_t size) {
    if (size < sizeof(Chip8State)) return false;
    memcpy(data, &gC8.s, sizeof(Chip8State));
    return true;
}
bool c8Unserialize(const void *data, size_t size) {
    if (size < sizeof(Chip8State)) return false;
    memcpy(&gC8.s, data, sizeof(Chip8State));
    return true;
}
void *c8MemData(unsigned) { return nullptr; }
size_t c8MemSize(unsigned) { return 0; }

}  // namespace

CoreApi chip8Api() {
    CoreApi api{};
    api.init = &c8Init;
    api.deinit = &c8Deinit;
    api.api_version = &c8ApiVersion;
    api.get_system_info = &c8GetSystemInfo;
    api.get_system_av_info = &c8GetSystemAvInfo;
    api.set_environment = &c8SetEnv;
    api.set_video_refresh = &c8SetVideo;
    api.set_audio_sample = &c8SetAudioSample;
    api.set_audio_sample_batch = &c8SetAudioBatch;
    api.set_input_poll = &c8SetInputPoll;
    api.set_input_state = &c8SetInputState;
    api.reset = &c8Reset;
    api.run = &c8Run;
    api.serialize_size = &c8SerializeSize;
    api.serialize = &c8Serialize;
    api.unserialize = &c8Unserialize;
    api.load_game = &c8LoadGame;
    api.unload_game = &c8UnloadGame;
    api.get_memory_data = &c8MemData;
    api.get_memory_size = &c8MemSize;
    return api;
}

}  // namespace rh::builtin
