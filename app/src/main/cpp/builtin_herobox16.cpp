#include "builtin_cores.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>

namespace rh::builtin {

namespace {

constexpr int kW = 240;
constexpr int kH = 160;
constexpr int kSampleRate = 44100;
constexpr int kAudioFrames = kSampleRate / 60;  // 735 stereo frames @ 60 Hz

// Curated 16-bit HeroBox-16 Hardware Palette (32 rich arcade colors)
constexpr uint32_t kPal[32] = {
    0xFF0C0E18u, 0xFF171B2Eu, 0xFF252B48u, 0xFF3A4268u,
    0xFF5A6590u, 0xFF939EC4u, 0xFFD9E0F7u, 0xFFFFFFFFu,
    0xFFFF3B6Bu, 0xFFFF7A9Cu, 0xFFFF9F43u, 0xFFFFD166u,
    0xFF4ADE80u, 0xFF22C55Eu, 0xFF38BDF8u, 0xFF0284C7u,
    0xFFA78BFAu, 0xFF7C3AEDu, 0xFFF472B6u, 0xFFEC4899u,
    0xFF2DD4BFu, 0xFF14B8A6u, 0xFFFB7185u, 0xFFE11D48u,
    0xFFFACC15u, 0xFFF59E0Bu, 0xFF60A5FAu, 0xFF3B82F6u,
    0xFF1E1B4Bu, 0xFF312E81u, 0xFF4C1D95u, 0xFF831843u,
};

// Compact 5x7 uppercase + digit pixel font for crisp HUD & title screens
struct Glyph { char ch; uint8_t rows[7]; };
static const Glyph kFont[] = {
    {'0', {0x0E,0x11,0x13,0x15,0x19,0x11,0x0E}}, {'1', {0x04,0x0C,0x04,0x04,0x04,0x04,0x0E}},
    {'2', {0x0E,0x11,0x01,0x02,0x04,0x08,0x1F}}, {'3', {0x1F,0x02,0x04,0x02,0x01,0x11,0x0E}},
    {'4', {0x02,0x06,0x0A,0x12,0x1F,0x02,0x02}}, {'5', {0x1F,0x10,0x1E,0x01,0x01,0x11,0x0E}},
    {'6', {0x06,0x08,0x10,0x1E,0x11,0x11,0x0E}}, {'7', {0x1F,0x01,0x02,0x04,0x08,0x08,0x08}},
    {'8', {0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E}}, {'9', {0x0E,0x11,0x11,0x0F,0x01,0x02,0x0C}},
    {'A', {0x0E,0x11,0x11,0x1F,0x11,0x11,0x11}}, {'B', {0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E}},
    {'C', {0x0E,0x11,0x10,0x10,0x10,0x11,0x0E}}, {'D', {0x1C,0x12,0x11,0x11,0x11,0x12,0x1C}},
    {'E', {0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F}}, {'F', {0x1F,0x10,0x10,0x1E,0x10,0x10,0x10}},
    {'G', {0x0E,0x11,0x10,0x17,0x11,0x11,0x0F}}, {'H', {0x11,0x11,0x11,0x1F,0x11,0x11,0x11}},
    {'I', {0x0E,0x04,0x04,0x04,0x04,0x04,0x0E}}, {'J', {0x07,0x02,0x02,0x02,0x02,0x12,0x0C}},
    {'K', {0x11,0x12,0x14,0x18,0x14,0x12,0x11}}, {'L', {0x10,0x10,0x10,0x10,0x10,0x10,0x1F}},
    {'M', {0x11,0x1B,0x15,0x15,0x11,0x11,0x11}}, {'N', {0x11,0x19,0x15,0x13,0x11,0x11,0x11}},
    {'O', {0x0E,0x11,0x11,0x11,0x11,0x11,0x0E}}, {'P', {0x1E,0x11,0x11,0x1E,0x10,0x10,0x10}},
    {'Q', {0x0E,0x11,0x11,0x11,0x15,0x12,0x0D}}, {'R', {0x1E,0x11,0x11,0x1E,0x14,0x12,0x11}},
    {'S', {0x0F,0x10,0x10,0x0E,0x01,0x01,0x1E}}, {'T', {0x1F,0x04,0x04,0x04,0x04,0x04,0x04}},
    {'U', {0x11,0x11,0x11,0x11,0x11,0x11,0x0E}}, {'V', {0x11,0x11,0x11,0x11,0x11,0x0A,0x04}},
    {'W', {0x11,0x11,0x11,0x15,0x15,0x1B,0x11}}, {'X', {0x11,0x11,0x0A,0x04,0x0A,0x11,0x11}},
    {'Y', {0x11,0x11,0x0A,0x04,0x04,0x04,0x04}}, {'Z', {0x1F,0x01,0x02,0x04,0x08,0x10,0x1F}},
    {'-', {0x00,0x00,0x00,0x1F,0x00,0x00,0x00}}, {'!', {0x04,0x04,0x04,0x04,0x04,0x00,0x04}},
    {':', {0x00,0x04,0x04,0x00,0x04,0x04,0x00}}, {'.', {0x00,0x00,0x00,0x00,0x00,0x0C,0x0C}},
    {'+', {0x00,0x04,0x04,0x1F,0x04,0x04,0x00}}, {'/', {0x01,0x01,0x02,0x04,0x08,0x10,0x10}},
    {'x', {0x00,0x00,0x11,0x0A,0x04,0x0A,0x11}},
};

// ---- 4-Channel Real-Time Chiptune APU ("HB16-APU") ----
struct ApuChannel {
    float freq;
    float targetFreq;
    float phase;
    float vol;
    float decay;
    uint8_t wave;  // 0=pulse, 1=triangle, 2=saw, 3=noise
};

struct Bullet { float x, y, vx, vy; uint8_t active, friendly, color; };
struct Enemy { float x, y, vx, vy, hp, maxHp, phase; uint8_t active, kind; };
struct Particle { float x, y, vx, vy; uint8_t life, maxLife, color; };
struct Pickup { float x, y; uint8_t active, kind; };
struct Ball { float x, y, vx, vy; uint8_t active; };
struct Platform { float x, y, w; uint8_t active, hasGem, hasSpike; };

// Complete deterministic HeroBox-16 console + cartridge state (100% serializable for Save States & Rewind!)
struct HeroBoxState {
    uint8_t cartMode;   // 0=custom HB16 bytecode VM, 1=Starlight Vanguard, 2=Neon Breaker DX, 3=Prism Dash
    uint8_t screen;     // 0=title, 1=playing, 2=gameover
    uint32_t tick;
    uint32_t rng;
    uint32_t score;
    uint32_t hiScore;
    uint16_t combo;
    uint16_t comboTimer;
    uint8_t lives;
    uint8_t stage;
    uint8_t shake;
    uint16_t prevButtons;

    // Player state
    float px, py, pvx, pvy;
    uint8_t jumpCount;
    uint8_t dashTimer;
    uint8_t shootCd;
    uint16_t spreadTimer;
    uint8_t shieldHits;
    uint8_t invuln;
    float paddleW;
    uint16_t laserTimer;

    // Entities
    Bullet bullets[40];
    Enemy enemies[24];
    Particle particles[64];
    Pickup pickups[8];
    Ball balls[6];
    uint8_t bricks[6 * 12];
    Platform platforms[12];
    float scrollSpeed;

    // HB16-CPU Bytecode VM state (for user-written .hbx cartridges)
    int32_t reg[16];
    uint32_t pc;
    uint32_t callStack[32];
    uint8_t sp;
    uint8_t ram[4096];

    // HB16-APU synthesizer state
    ApuChannel ch[4];
    uint32_t lfsr;
    uint16_t musicStep;
};

struct HeroBoxRuntime {
    retro_environment_t env = nullptr;
    retro_video_refresh_t video = nullptr;
    retro_audio_sample_batch_t audioBatch = nullptr;
    retro_input_poll_t inputPoll = nullptr;
    retro_input_state_t inputState = nullptr;

    HeroBoxState s{};
    uint8_t rom[65536]{};
    size_t romSize = 0;
    char cartTitle[32]{};
    uint32_t fb[kW * kH]{};
    int16_t audioBuf[kAudioFrames * 2]{};
};

HeroBoxRuntime gHb;

uint32_t rnd() {
    gHb.s.rng = gHb.s.rng * 1664525u + 1013904223u;
    return gHb.s.rng;
}

float rndF(float lo, float hi) {
    return lo + (hi - lo) * static_cast<float>(rnd() & 0xFFFFu) / 65535.0f;
}

// ---- HB16-APU Sound Synthesis ----
void triggerNote(int idx, uint8_t wave, float freq, float targetFreq, float vol, float decay) {
    if (idx < 0 || idx >= 4) return;
    auto &c = gHb.s.ch[idx];
    c.wave = wave;
    c.freq = freq;
    c.targetFreq = targetFreq;
    c.vol = vol;
    c.decay = decay;
}

void sfxLaser()   { triggerNote(0, 0, 880.f, 220.f, 0.24f, 0.00018f); }
void sfxHit()     { triggerNote(3, 3, 900.f, 300.f, 0.26f, 0.00024f); }
void sfxExplode() { triggerNote(3, 3, 420.f, 65.f, 0.36f, 0.00009f); }
void sfxCoin()    { triggerNote(0, 0, 987.f, 1318.f, 0.22f, 0.00014f); }
void sfxJump()    { triggerNote(0, 1, 260.f, 660.f, 0.25f, 0.00016f); }
void sfxPowerup() { triggerNote(2, 2, 440.f, 1180.f, 0.24f, 0.00008f); }
void sfxBounce()  { triggerNote(1, 1, 520.f, 640.f, 0.22f, 0.00025f); }

void tickChiptuneMusic() {
    if (gHb.s.screen != 1) return;
    if (gHb.s.tick % 9 != 0) return;  // ~166 BPM 16th-note pulse
    static const float kBass[16] = {
        110.f, 110.f, 130.8f, 110.f, 146.8f, 146.8f, 164.8f, 146.8f,
        130.8f, 130.8f, 146.8f, 130.8f, 98.0f, 98.0f, 123.5f, 98.0f,
    };
    static const float kArp[16] = {
        440.f, 523.2f, 659.2f, 523.2f, 587.3f, 659.2f, 784.0f, 659.2f,
        523.2f, 659.2f, 784.0f, 659.2f, 392.0f, 493.9f, 587.3f, 659.2f,
    };
    int step = gHb.s.musicStep++ & 15;
    float shift = gHb.s.cartMode == 2 ? 1.122f : (gHb.s.cartMode == 3 ? 1.26f : 1.0f);
    if (gHb.s.ch[1].vol < 0.08f) {
        triggerNote(1, 1, kBass[step] * shift, kBass[step] * shift, 0.14f, 0.00007f);
    }
    if ((step % 2 == 0) && gHb.s.ch[2].vol < 0.06f) {
        triggerNote(2, 2, kArp[step] * shift, kArp[step] * shift, 0.08f, 0.00010f);
    }
}

void renderAudioFrame() {
    auto &s = gHb.s;
    if (s.lfsr == 0) s.lfsr = 0x7FFFu;
    for (int i = 0; i < kAudioFrames; i++) {
        float mix = 0.0f;
        for (int c = 0; c < 4; c++) {
            auto &ch = s.ch[c];
            if (ch.vol <= 0.001f) continue;
            ch.freq += (ch.targetFreq - ch.freq) * 0.0025f;
            ch.phase += ch.freq / static_cast<float>(kSampleRate);
            if (ch.phase >= 1.0f) {
                ch.phase -= 1.0f;
                if (ch.wave == 3) {
                    uint32_t bit = ((s.lfsr >> 0) ^ (s.lfsr >> 1)) & 1u;
                    s.lfsr = (s.lfsr >> 1) | (bit << 14);
                }
            }
            float sample = 0.0f;
            switch (ch.wave) {
                case 0: sample = (ch.phase < 0.35f) ? 1.0f : -1.0f; break;
                case 1: sample = 4.0f * std::fabs(ch.phase - 0.5f) - 1.0f; break;
                case 2: sample = 2.0f * ch.phase - 1.0f; break;
                default: sample = (s.lfsr & 1u) ? 1.0f : -1.0f; break;
            }
            mix += sample * ch.vol;
            ch.vol = std::max(0.0f, ch.vol - ch.decay);
        }
        int32_t out = static_cast<int32_t>(std::clamp(mix, -0.92f, 0.92f) * 26000.0f);
        gHb.audioBuf[i * 2] = static_cast<int16_t>(out);
        gHb.audioBuf[i * 2 + 1] = static_cast<int16_t>(out);
    }
    if (gHb.audioBatch) gHb.audioBatch(gHb.audioBuf, kAudioFrames);
}

// ---- HB16-PPU Graphics Primitives ----
inline void putPx(int x, int y, uint32_t col) {
    if (x >= 0 && x < kW && y >= 0 && y < kH) gHb.fb[y * kW + x] = col;
}

void clearBg(uint32_t topCol, uint32_t botCol) {
    int tr = (topCol >> 16) & 0xFF, tg = (topCol >> 8) & 0xFF, tb = topCol & 0xFF;
    int br = (botCol >> 16) & 0xFF, bg = (botCol >> 8) & 0xFF, bb = botCol & 0xFF;
    for (int y = 0; y < kH; y++) {
        uint32_t r = static_cast<uint32_t>(tr + ((br - tr) * y) / kH);
        uint32_t g = static_cast<uint32_t>(tg + ((bg - tg) * y) / kH);
        uint32_t b = static_cast<uint32_t>(tb + ((bb - tb) * y) / kH);
        uint32_t c = 0xFF000000u | (r << 16) | (g << 8) | b;
        uint32_t *row = gHb.fb + y * kW;
        for (int x = 0; x < kW; x++) row[x] = c;
    }
}

void fillRect(int x, int y, int w, int h, uint32_t col) {
    int x0 = std::max(0, x), y0 = std::max(0, y);
    int x1 = std::min(kW, x + w), y1 = std::min(kH, y + h);
    for (int py = y0; py < y1; py++) {
        uint32_t *row = gHb.fb + py * kW;
        for (int px = x0; px < x1; px++) row[px] = col;
    }
}

void fillCircle(int cx, int cy, int r, uint32_t col) {
    int r2 = r * r;
    for (int dy = -r; dy <= r; dy++) {
        for (int dx = -r; dx <= r; dx++) {
            if (dx * dx + dy * dy <= r2) putPx(cx + dx, cy + dy, col);
        }
    }
}

void drawText(int x, int y, const char *txt, uint32_t col, int scale = 1) {
    int cx = x;
    for (const char *p = txt; *p; p++) {
        char ch = *p;
        if (ch >= 'a' && ch <= 'z' && ch != 'x') ch = static_cast<char>(ch - 'a' + 'A');
        if (ch == ' ') { cx += 5 * scale; continue; }
        const Glyph *g = nullptr;
        for (const auto &entry : kFont) {
            if (entry.ch == ch) { g = &entry; break; }
        }
        if (g) {
            for (int row = 0; row < 7; row++) {
                uint8_t bits = g->rows[row];
                for (int colBit = 0; colBit < 5; colBit++) {
                    if (bits & (0x10 >> colBit)) {
                        fillRect(cx + colBit * scale, y + row * scale, scale, scale, col);
                    }
                }
            }
        }
        cx += 6 * scale;
    }
}

void drawTextCentered(int y, const char *txt, uint32_t col, int scale = 1) {
    int len = static_cast<int>(strlen(txt));
    int totalW = len * 6 * scale - scale;
    drawText((kW - totalW) / 2, y, txt, col, scale);
}

void spawnBurst(float x, float y, uint8_t color, int count = 10) {
    for (int i = 0; i < count; i++) {
        for (auto &pt : gHb.s.particles) {
            if (pt.life == 0) {
                float a = rndF(0.f, 6.283f);
                float sp = rndF(0.6f, 2.8f);
                pt.x = x; pt.y = y;
                pt.vx = std::cos(a) * sp;
                pt.vy = std::sin(a) * sp;
                pt.maxLife = static_cast<uint8_t>(12 + (rnd() % 14));
                pt.life = pt.maxLife;
                pt.color = color;
                break;
            }
        }
    }
}

void updateAndDrawParticles(int ox, int oy) {
    for (auto &pt : gHb.s.particles) {
        if (pt.life == 0) continue;
        pt.x += pt.vx;
        pt.y += pt.vy;
        pt.vx *= 0.94f;
        pt.vy *= 0.94f;
        pt.life--;
        int sz = pt.life > pt.maxLife / 2 ? 2 : 1;
        fillRect(static_cast<int>(pt.x) + ox, static_cast<int>(pt.y) + oy, sz, sz, kPal[pt.color & 31]);
    }
}

uint16_t pollButtons() {
    if (!gHb.inputState) return 0;
    uint16_t mask = 0;
    for (unsigned id = 0; id < 16; id++) {
        if (gHb.inputState(0, RETRO_DEVICE_JOYPAD, 0, id)) mask |= static_cast<uint16_t>(1u << id);
    }
    return mask;
}

inline bool held(uint16_t m, unsigned id) { return (m & (1u << id)) != 0; }
inline bool pressed(uint16_t m, unsigned id) { return ((m & ~gHb.s.prevButtons) & (1u << id)) != 0; }

// ---- GAME 1: STARLIGHT VANGUARD (Arcade Vertical Space Shooter) ----
void resetStarlight(bool toPlaying) {
    uint32_t hi = gHb.s.hiScore;
    uint8_t mode = gHb.s.cartMode;
    memset(&gHb.s, 0, sizeof(gHb.s));
    gHb.s.cartMode = mode;
    gHb.s.hiScore = hi;
    gHb.s.screen = toPlaying ? 1 : 0;
    gHb.s.rng = 0x57A1197u;
    gHb.s.lfsr = 0x7FFFu;
    gHb.s.px = kW * 0.5f;
    gHb.s.py = kH - 26.0f;
    gHb.s.lives = 3;
    gHb.s.stage = 1;
    gHb.s.shieldHits = 1;
}

void stepStarlight(uint16_t btn) {
    auto &s = gHb.s;
    clearBg(kPal[0], kPal[28]);

    // 3-layer parallax starfield
    for (int i = 0; i < 42; i++) {
        int sx = (i * 53 + 17) % kW;
        int spd = (i % 3) + 1;
        int sy = static_cast<int>((i * 29 + s.tick * spd) % kH);
        uint32_t col = spd == 3 ? kPal[7] : (spd == 2 ? kPal[14] : kPal[4]);
        fillRect(sx, sy, spd == 3 ? 2 : 1, spd == 3 ? 2 : 1, col);
    }

    if (s.screen == 0) {
        drawTextCentered(36, "HEROBOX-16", kPal[16], 1);
        drawTextCentered(52, "STARLIGHT", kPal[14], 2);
        drawTextCentered(72, "VANGUARD", kPal[11], 2);
        if ((s.tick / 25) % 2 == 0) drawTextCentered(108, "PRESS START OR A", kPal[7], 1);
        drawTextCentered(132, "D-PAD MOVE  A FIRE", kPal[5], 1);
        if (pressed(btn, RETRO_DEVICE_ID_JOYPAD_START) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_A) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_B)) {
            sfxPowerup();
            resetStarlight(true);
        }
        return;
    }

    int ox = s.shake > 0 ? static_cast<int>(rnd() % 5) - 2 : 0;
    int oy = s.shake > 0 ? static_cast<int>(rnd() % 5) - 2 : 0;
    if (s.shake > 0) s.shake--;

    if (s.screen == 1) {
        float spd = 2.6f;
        if (held(btn, RETRO_DEVICE_ID_JOYPAD_LEFT))  s.px -= spd;
        if (held(btn, RETRO_DEVICE_ID_JOYPAD_RIGHT)) s.px += spd;
        if (held(btn, RETRO_DEVICE_ID_JOYPAD_UP))    s.py -= spd;
        if (held(btn, RETRO_DEVICE_ID_JOYPAD_DOWN))  s.py += spd;
        s.px = std::clamp(s.px, 12.f, static_cast<float>(kW - 12));
        s.py = std::clamp(s.py, 24.f, static_cast<float>(kH - 14));

        if (s.shootCd > 0) s.shootCd--;
        if (s.spreadTimer > 0) s.spreadTimer--;
        if (s.invuln > 0) s.invuln--;
        if (s.comboTimer > 0) { if (--s.comboTimer == 0) s.combo = 0; }

        if ((held(btn, RETRO_DEVICE_ID_JOYPAD_A) || held(btn, RETRO_DEVICE_ID_JOYPAD_B) || held(btn, RETRO_DEVICE_ID_JOYPAD_Y)) && s.shootCd == 0) {
            s.shootCd = 7;
            sfxLaser();
            auto fire = [&](float x, float y, float vx, float vy, uint8_t col) {
                for (auto &b : s.bullets) {
                    if (!b.active) { b = {x, y, vx, vy, 1, 1, col}; break; }
                }
            };
            fire(s.px - 4, s.py - 6, 0.f, -4.6f, 14);
            fire(s.px + 4, s.py - 6, 0.f, -4.6f, 14);
            if (s.spreadTimer > 0) {
                fire(s.px - 6, s.py - 4, -1.4f, -4.2f, 11);
                fire(s.px + 6, s.py - 4,  1.4f, -4.2f, 11);
            }
        }

        // Spawn enemy waves + periodic Dreadnought boss
        int spawnEvery = std::max(18, 42 - static_cast<int>(s.stage) * 3);
        if (s.tick % spawnEvery == 0) {
            bool hasBoss = false;
            for (const auto &e : s.enemies) if (e.active && e.kind == 3) hasBoss = true;
            if (!hasBoss && s.score > 0 && (s.score / 800) >= s.stage) {
                s.stage++;
                for (auto &e : s.enemies) {
                    if (!e.active) {
                        e = {kW * 0.5f, -18.f, 1.1f, 0.45f, 18.f + s.stage * 6.f, 18.f + s.stage * 6.f, 0.f, 1, 3};
                        break;
                    }
                }
            } else if (!hasBoss) {
                for (auto &e : s.enemies) {
                    if (!e.active) {
                        uint8_t kind = static_cast<uint8_t>(rnd() % 3);
                        float hp = kind == 2 ? 4.f : (kind == 1 ? 2.f : 1.f);
                        e = {rndF(18.f, kW - 18.f), -10.f, 0.f, 0.85f + kind * 0.22f + s.stage * 0.08f, hp, hp, rndF(0.f, 6.28f), 1, kind};
                        break;
                    }
                }
            }
        }

        // Update bullets
        for (auto &b : s.bullets) {
            if (!b.active) continue;
            b.x += b.vx; b.y += b.vy;
            if (b.x < -8 || b.x > kW + 8 || b.y < -8 || b.y > kH + 8) { b.active = 0; continue; }
            if (b.friendly) {
                for (auto &e : s.enemies) {
                    if (!e.active) continue;
                    float rad = e.kind == 3 ? 16.f : 8.f;
                    if (std::fabs(b.x - e.x) < rad && std::fabs(b.y - e.y) < rad) {
                        b.active = 0;
                        e.hp -= 1.f;
                        sfxHit();
                        spawnBurst(b.x, b.y, 11, 4);
                        if (e.hp <= 0.f) {
                            e.active = 0;
                            sfxExplode();
                            s.shake = e.kind == 3 ? 12 : 4;
                            spawnBurst(e.x, e.y, e.kind == 3 ? 8 : 10, e.kind == 3 ? 22 : 10);
                            s.combo++;
                            s.comboTimer = 110;
                            uint32_t pts = (e.kind == 3 ? 500u : (e.kind == 2 ? 120u : 60u)) * std::min<uint32_t>(s.combo, 8u);
                            s.score += pts;
                            if (s.score > s.hiScore) s.hiScore = s.score;
                            if (e.kind == 3 || (rnd() % 6 == 0)) {
                                for (auto &pk : s.pickups) {
                                    if (!pk.active) { pk = {e.x, e.y, 1, static_cast<uint8_t>(rnd() % 2)}; break; }
                                }
                            }
                        }
                        break;
                    }
                }
            } else if (s.invuln == 0 && std::fabs(b.x - s.px) < 7.f && std::fabs(b.y - s.py) < 7.f) {
                b.active = 0;
                if (s.shieldHits > 0) { s.shieldHits--; s.invuln = 45; sfxHit(); spawnBurst(s.px, s.py, 20, 10); }
                else {
                    s.invuln = 70; s.shake = 10; sfxExplode(); spawnBurst(s.px, s.py, 8, 16);
                    if (s.lives > 0) s.lives--;
                    if (s.lives == 0) s.screen = 2;
                }
            }
        }

        // Update enemies
        for (auto &e : s.enemies) {
            if (!e.active) continue;
            e.phase += 0.08f;
            if (e.kind == 3) {
                if (e.y < 34.f) e.y += e.vy;
                else {
                    e.x += e.vx;
                    if (e.x < 28.f || e.x > kW - 28.f) e.vx = -e.vx;
                }
                if (s.tick % 38 == 0) {
                    for (int k = -1; k <= 1; k++) {
                        for (auto &b : s.bullets) {
                            if (!b.active) { b = {e.x + k * 8.f, e.y + 12.f, k * 0.9f, 2.2f, 1, 0, 8}; break; }
                        }
                    }
                }
            } else {
                e.y += e.vy;
                if (e.kind == 1) e.x += std::sin(e.phase) * 1.4f;
                if (e.kind == 2 && (s.tick % 65 == 0)) {
                    float dx = s.px - e.x, dy = s.py - e.y;
                    float d = std::max(1.f, std::sqrt(dx * dx + dy * dy));
                    for (auto &b : s.bullets) {
                        if (!b.active) { b = {e.x, e.y + 6.f, dx / d * 2.1f, dy / d * 2.1f, 1, 0, 18}; break; }
                    }
                }
                if (e.y > kH + 14) { e.active = 0; continue; }
            }
            if (s.invuln == 0 && std::fabs(e.x - s.px) < 10.f && std::fabs(e.y - s.py) < 10.f) {
                if (e.kind != 3) e.active = 0;
                if (s.shieldHits > 0) { s.shieldHits--; s.invuln = 45; sfxHit(); }
                else {
                    s.invuln = 70; s.shake = 10; sfxExplode(); spawnBurst(s.px, s.py, 8, 16);
                    if (s.lives > 0) s.lives--;
                    if (s.lives == 0) s.screen = 2;
                }
            }
        }

        // Update pickups
        for (auto &pk : s.pickups) {
            if (!pk.active) continue;
            pk.y += 1.1f;
            if (pk.y > kH + 8) { pk.active = 0; continue; }
            if (std::fabs(pk.x - s.px) < 12.f && std::fabs(pk.y - s.py) < 12.f) {
                pk.active = 0;
                sfxPowerup();
                if (pk.kind == 0) s.spreadTimer = 360;
                else s.shieldHits = std::min<uint8_t>(2, s.shieldHits + 1);
                spawnBurst(s.px, s.py, 12, 10);
            }
        }
    }

    // Draw pickups
    for (const auto &pk : s.pickups) {
        if (!pk.active) continue;
        fillCircle(static_cast<int>(pk.x) + ox, static_cast<int>(pk.y) + oy, 4, pk.kind == 0 ? kPal[11] : kPal[20]);
        fillRect(static_cast<int>(pk.x) - 1 + ox, static_cast<int>(pk.y) - 1 + oy, 3, 3, kPal[7]);
    }
    // Draw enemies
    for (const auto &e : s.enemies) {
        if (!e.active) continue;
        int ex = static_cast<int>(e.x) + ox, ey = static_cast<int>(e.y) + oy;
        if (e.kind == 3) {
            fillRect(ex - 16, ey - 8, 32, 14, kPal[23]);
            fillRect(ex - 10, ey - 12, 20, 18, kPal[8]);
            fillRect(ex - 5, ey - 3, 10, 6, kPal[11]);
            int barW = static_cast<int>((e.hp / std::max(1.f, e.maxHp)) * 36.f);
            fillRect(ex - 18, ey - 16, 36, 3, kPal[2]);
            fillRect(ex - 18, ey - 16, barW, 3, kPal[12]);
        } else {
            uint32_t c = e.kind == 2 ? kPal[18] : (e.kind == 1 ? kPal[10] : kPal[8]);
            fillRect(ex - 6, ey - 4, 12, 7, c);
            fillRect(ex - 3, ey + 2, 6, 4, kPal[11]);
            fillRect(ex - 4, ey - 2, 2, 2, kPal[7]);
            fillRect(ex + 2, ey - 2, 2, 2, kPal[7]);
        }
    }
    // Draw bullets
    for (const auto &b : s.bullets) {
        if (!b.active) continue;
        fillRect(static_cast<int>(b.x) - 1 + ox, static_cast<int>(b.y) - 3 + oy, 3, 6, kPal[b.color & 31]);
    }
    // Draw player starfighter
    if (s.screen == 1 && (s.invuln == 0 || (s.tick / 3) % 2 == 0)) {
        int x = static_cast<int>(s.px) + ox, y = static_cast<int>(s.py) + oy;
        if (s.shieldHits > 0) fillCircle(x, y, 10, kPal[20]);
        fillRect(x - 8, y + 1, 16, 5, kPal[15]);
        fillRect(x - 4, y - 5, 8, 12, kPal[14]);
        fillRect(x - 2, y - 9, 4, 6, kPal[7]);
        fillRect(x - 2, y + 7, 4, 3 + (s.tick % 3), kPal[11]);
    }
    updateAndDrawParticles(ox, oy);

    // HUD
    char hud[40];
    snprintf(hud, sizeof(hud), "SCORE %u", s.score);
    drawText(6, 5, hud, kPal[7], 1);
    if (s.combo > 1) {
        snprintf(hud, sizeof(hud), "x%u", s.combo);
        drawText(104, 5, hud, kPal[11], 1);
    }
    for (int i = 0; i < s.lives; i++) fillRect(kW - 10 - i * 9, 6, 6, 5, kPal[8]);

    if (s.screen == 2) {
        fillRect(36, 48, kW - 72, 64, kPal[1]);
        drawTextCentered(58, "GAME OVER", kPal[8], 2);
        snprintf(hud, sizeof(hud), "BEST %u", s.hiScore);
        drawTextCentered(80, hud, kPal[11], 1);
        if ((s.tick / 24) % 2 == 0) drawTextCentered(96, "PRESS START", kPal[7], 1);
        if (pressed(btn, RETRO_DEVICE_ID_JOYPAD_START) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_A)) {
            resetStarlight(true);
        }
    }
}

// ---- GAME 2: NEON BREAKER DX (Synthwave Brick Breaker) ----
void loadBreakerStage(uint8_t stage) {
    auto &s = gHb.s;
    for (int r = 0; r < 6; r++) {
        for (int c = 0; c < 12; c++) {
            bool solid = ((stage % 2 == 1) || ((r + c) % 2 == 0) || r < 2);
            s.bricks[r * 12 + c] = solid ? static_cast<uint8_t>(1 + (r == 0 && stage > 1 ? 1 : 0)) : 0;
        }
    }
    memset(s.balls, 0, sizeof(s.balls));
    s.balls[0] = {kW * 0.5f, kH - 28.f, 1.8f, -2.2f, 1};
    s.paddleW = 42.f;
}

void resetBreaker(bool toPlaying) {
    uint32_t hi = gHb.s.hiScore;
    uint8_t mode = gHb.s.cartMode;
    memset(&gHb.s, 0, sizeof(gHb.s));
    gHb.s.cartMode = mode;
    gHb.s.hiScore = hi;
    gHb.s.screen = toPlaying ? 1 : 0;
    gHb.s.rng = 0xB8EA4E8u;
    gHb.s.lfsr = 0x7FFFu;
    gHb.s.px = kW * 0.5f;
    gHb.s.lives = 3;
    gHb.s.stage = 1;
    loadBreakerStage(1);
}

void stepBreaker(uint16_t btn) {
    auto &s = gHb.s;
    clearBg(kPal[29], kPal[0]);

    if (s.screen == 0) {
        drawTextCentered(34, "HEROBOX-16", kPal[18], 1);
        drawTextCentered(52, "NEON", kPal[20], 2);
        drawTextCentered(72, "BREAKER DX", kPal[11], 2);
        if ((s.tick / 25) % 2 == 0) drawTextCentered(108, "PRESS START OR A", kPal[7], 1);
        if (pressed(btn, RETRO_DEVICE_ID_JOYPAD_START) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_A)) {
            sfxPowerup();
            resetBreaker(true);
        }
        return;
    }

    int ox = s.shake > 0 ? static_cast<int>(rnd() % 5) - 2 : 0;
    int oy = s.shake > 0 ? static_cast<int>(rnd() % 5) - 2 : 0;
    if (s.shake > 0) s.shake--;

    if (s.screen == 1) {
        float spd = 3.4f;
        if (held(btn, RETRO_DEVICE_ID_JOYPAD_LEFT))  s.px -= spd;
        if (held(btn, RETRO_DEVICE_ID_JOYPAD_RIGHT)) s.px += spd;
        s.px = std::clamp(s.px, s.paddleW * 0.5f + 4.f, kW - s.paddleW * 0.5f - 4.f);

        int activeBalls = 0;
        for (auto &bl : s.balls) {
            if (!bl.active) continue;
            activeBalls++;
            bl.x += bl.vx; bl.y += bl.vy;
            if (bl.x < 5.f) { bl.x = 5.f; bl.vx = std::fabs(bl.vx); sfxBounce(); }
            if (bl.x > kW - 5.f) { bl.x = kW - 5.f; bl.vx = -std::fabs(bl.vx); sfxBounce(); }
            if (bl.y < 16.f) { bl.y = 16.f; bl.vy = std::fabs(bl.vy); sfxBounce(); }

            // Paddle collision
            if (bl.vy > 0 && bl.y >= kH - 18.f && bl.y <= kH - 10.f && std::fabs(bl.x - s.px) <= s.paddleW * 0.5f + 3.f) {
                float rel = (bl.x - s.px) / (s.paddleW * 0.5f);
                bl.vx = std::clamp(rel * 2.8f, -3.2f, 3.2f);
                bl.vy = -std::max(2.1f, std::fabs(bl.vy));
                s.combo = 0;
                sfxBounce();
            }

            // Brick collision
            int col = static_cast<int>((bl.x - 6.f) / 19.f);
            int row = static_cast<int>((bl.y - 24.f) / 10.f);
            if (col >= 0 && col < 12 && row >= 0 && row < 6 && s.bricks[row * 12 + col] > 0) {
                s.bricks[row * 12 + col]--;
                bl.vy = -bl.vy;
                s.combo++;
                s.score += 50u * std::min<uint32_t>(s.combo, 8u);
                if (s.score > s.hiScore) s.hiScore = s.score;
                s.shake = 3;
                sfxCoin();
                spawnBurst(bl.x, bl.y, static_cast<uint8_t>(8 + row * 2), 8);
                if (rnd() % 7 == 0) {
                    for (auto &pk : s.pickups) {
                        if (!pk.active) { pk = {bl.x, bl.y, 1, static_cast<uint8_t>(rnd() % 2)}; break; }
                    }
                }
            }

            if (bl.y > kH + 8.f) { bl.active = 0; activeBalls--; }
        }

        if (activeBalls == 0) {
            sfxExplode();
            s.shake = 8;
            if (s.lives > 0) s.lives--;
            if (s.lives == 0) s.screen = 2;
            else s.balls[0] = {s.px, kH - 26.f, 1.7f, -2.2f, 1};
        }

        for (auto &pk : s.pickups) {
            if (!pk.active) continue;
            pk.y += 1.3f;
            if (pk.y > kH + 6) { pk.active = 0; continue; }
            if (pk.y >= kH - 20.f && std::fabs(pk.x - s.px) < s.paddleW * 0.5f + 6.f) {
                pk.active = 0;
                sfxPowerup();
                if (pk.kind == 0) {
                    s.paddleW = std::min(68.f, s.paddleW + 12.f);
                } else {
                    for (auto &bl : s.balls) {
                        if (!bl.active) { bl = {s.px, kH - 26.f, rndF(-2.2f, 2.2f), -2.4f, 1}; break; }
                    }
                }
            }
        }

        bool anyBrick = false;
        for (uint8_t b : s.bricks) if (b > 0) anyBrick = true;
        if (!anyBrick) { s.stage++; sfxPowerup(); loadBreakerStage(s.stage); }
    }

    // Draw bricks
    static const uint8_t kRowCol[6] = {8, 10, 11, 12, 14, 16};
    for (int r = 0; r < 6; r++) {
        for (int c = 0; c < 12; c++) {
            uint8_t hp = s.bricks[r * 12 + c];
            if (hp == 0) continue;
            int bx = 6 + c * 19 + ox, by = 24 + r * 10 + oy;
            fillRect(bx, by, 17, 8, hp > 1 ? kPal[7] : kPal[kRowCol[r]]);
            fillRect(bx + 1, by + 1, 15, 2, kPal[7]);
        }
    }
    // Draw pickups, balls & paddle
    for (const auto &pk : s.pickups) {
        if (pk.active) fillCircle(static_cast<int>(pk.x) + ox, static_cast<int>(pk.y) + oy, 4, pk.kind == 0 ? kPal[12] : kPal[11]);
    }
    for (const auto &bl : s.balls) {
        if (bl.active) fillCircle(static_cast<int>(bl.x) + ox, static_cast<int>(bl.y) + oy, 3, kPal[7]);
    }
    fillRect(static_cast<int>(s.px - s.paddleW * 0.5f) + ox, kH - 15 + oy, static_cast<int>(s.paddleW), 6, kPal[20]);
    fillRect(static_cast<int>(s.px - s.paddleW * 0.5f + 3) + ox, kH - 14 + oy, static_cast<int>(s.paddleW - 6), 2, kPal[7]);
    updateAndDrawParticles(ox, oy);

    char hud[40];
    snprintf(hud, sizeof(hud), "SCORE %u  STG %u", s.score, s.stage);
    drawText(6, 5, hud, kPal[7], 1);
    for (int i = 0; i < s.lives; i++) fillCircle(kW - 10 - i * 9, 8, 3, kPal[18]);

    if (s.screen == 2) {
        fillRect(36, 48, kW - 72, 64, kPal[1]);
        drawTextCentered(58, "GAME OVER", kPal[8], 2);
        snprintf(hud, sizeof(hud), "BEST %u", s.hiScore);
        drawTextCentered(80, hud, kPal[11], 1);
        if ((s.tick / 24) % 2 == 0) drawTextCentered(96, "PRESS START", kPal[7], 1);
        if (pressed(btn, RETRO_DEVICE_ID_JOYPAD_START) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_A)) {
            resetBreaker(true);
        }
    }
}

// ---- GAME 3: PRISM DASH (Retro Action Platformer Runner) ----
void resetPrism(bool toPlaying) {
    uint32_t hi = gHb.s.hiScore;
    uint8_t mode = gHb.s.cartMode;
    memset(&gHb.s, 0, sizeof(gHb.s));
    gHb.s.cartMode = mode;
    gHb.s.hiScore = hi;
    gHb.s.screen = toPlaying ? 1 : 0;
    gHb.s.rng = 0x9153DA5u;
    gHb.s.lfsr = 0x7FFFu;
    gHb.s.px = 46.f;
    gHb.s.py = 90.f;
    gHb.s.scrollSpeed = 2.0f;
    gHb.s.platforms[0] = {0.f, 128.f, 160.f, 1, 0, 0};
    gHb.s.platforms[1] = {180.f, 114.f, 90.f, 1, 1, 0};
    gHb.s.platforms[2] = {292.f, 100.f, 80.f, 1, 1, 1};
}

void stepPrism(uint16_t btn) {
    auto &s = gHb.s;
    clearBg(kPal[30], kPal[1]);
    // Synthwave sunset disc + horizon
    fillCircle(186, 56, 22, kPal[10]);
    fillCircle(186, 56, 17, kPal[11]);
    for (int i = 0; i < 6; i++) {
        int mx = static_cast<int>((i * 60 - (s.tick / 2) % 60));
        fillRect(mx, 116 + (i % 2) * 8, 44, 44, kPal[2]);
    }

    if (s.screen == 0) {
        drawTextCentered(36, "HEROBOX-16", kPal[11], 1);
        drawTextCentered(54, "PRISM DASH", kPal[12], 2);
        if ((s.tick / 25) % 2 == 0) drawTextCentered(104, "PRESS START OR A", kPal[7], 1);
        drawTextCentered(128, "A JUMP   B AIR DASH", kPal[5], 1);
        if (pressed(btn, RETRO_DEVICE_ID_JOYPAD_START) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_A)) {
            sfxJump();
            resetPrism(true);
        }
        return;
    }

    if (s.screen == 1) {
        s.scrollSpeed = std::min(4.0f, 2.0f + s.score * 0.00045f);
        if ((pressed(btn, RETRO_DEVICE_ID_JOYPAD_A) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_UP)) && s.jumpCount < 2) {
            s.pvy = -4.8f;
            s.jumpCount++;
            sfxJump();
            spawnBurst(s.px, s.py + 8, 20, 6);
        }
        if ((pressed(btn, RETRO_DEVICE_ID_JOYPAD_B) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_X)) && s.dashTimer == 0) {
            s.dashTimer = 14;
            s.pvy = 0.f;
            sfxLaser();
            spawnBurst(s.px, s.py, 11, 8);
        }
        float moveMul = s.dashTimer > 0 ? 1.85f : 1.0f;
        if (s.dashTimer > 0) s.dashTimer--;
        else s.pvy += 0.28f;
        float prevY = s.py;
        s.py += s.pvy;

        float maxRight = 0.f;
        for (auto &pl : s.platforms) {
            if (!pl.active) continue;
            pl.x -= s.scrollSpeed * moveMul;
            if (pl.x + pl.w < -10.f) { pl.active = 0; continue; }
            maxRight = std::max(maxRight, pl.x + pl.w);

            // Land on platform
            if (s.pvy >= 0 && s.px + 5.f > pl.x && s.px - 5.f < pl.x + pl.w && prevY + 8.f <= pl.y + 4.f && s.py + 8.f >= pl.y) {
                s.py = pl.y - 8.f;
                s.pvy = 0.f;
                s.jumpCount = 0;
            }
            // Collect prism gem
            if (pl.hasGem) {
                float gx = pl.x + pl.w * 0.5f, gy = pl.y - 14.f;
                if (std::fabs(s.px - gx) < 12.f && std::fabs(s.py - gy) < 12.f) {
                    pl.hasGem = 0;
                    s.score += 100;
                    if (s.score > s.hiScore) s.hiScore = s.score;
                    sfxCoin();
                    spawnBurst(gx, gy, 11, 10);
                }
            }
            // Spike hazard
            if (pl.hasSpike && s.dashTimer == 0) {
                float sx = pl.x + pl.w * 0.72f, sy = pl.y - 5.f;
                if (std::fabs(s.px - sx) < 7.f && std::fabs(s.py + 4.f - sy) < 7.f) {
                    sfxExplode();
                    s.screen = 2;
                }
            }
        }

        // Spawn next platform seamlessly
        if (maxRight < kW + 20.f) {
            for (auto &pl : s.platforms) {
                if (!pl.active) {
                    float gap = rndF(24.f, 48.f);
                    float w = rndF(56.f, 108.f);
                    float y = rndF(74.f, 134.f);
                    pl = {std::max(static_cast<float>(kW), maxRight) + gap, y, w, 1, 1, static_cast<uint8_t>((rnd() % 3 == 0 && w > 72.f) ? 1 : 0)};
                    break;
                }
            }
        }

        if (s.tick % 6 == 0) {
            s.score++;
            if (s.score > s.hiScore) s.hiScore = s.score;
        }
        if (s.py > kH + 16.f) { sfxExplode(); s.screen = 2; }
    }

    for (const auto &pl : s.platforms) {
        if (!pl.active) continue;
        fillRect(static_cast<int>(pl.x), static_cast<int>(pl.y), static_cast<int>(pl.w), 8, kPal[17]);
        fillRect(static_cast<int>(pl.x), static_cast<int>(pl.y), static_cast<int>(pl.w), 2, kPal[16]);
        if (pl.hasGem) {
            int gx = static_cast<int>(pl.x + pl.w * 0.5f);
            int gy = static_cast<int>(pl.y - 14.f + std::sin(s.tick * 0.14f) * 2.5f);
            fillCircle(gx, gy, 4, kPal[11]);
            fillRect(gx - 1, gy - 1, 2, 2, kPal[7]);
        }
        if (pl.hasSpike) {
            int sx = static_cast<int>(pl.x + pl.w * 0.72f);
            fillRect(sx - 4, static_cast<int>(pl.y) - 6, 8, 6, kPal[8]);
            fillRect(sx - 2, static_cast<int>(pl.y) - 9, 4, 3, kPal[9]);
        }
    }

    // Draw player prism runner
    if (s.screen == 1) {
        int px = static_cast<int>(s.px), py = static_cast<int>(s.py);
        uint32_t body = s.dashTimer > 0 ? kPal[11] : kPal[20];
        fillRect(px - 5, py - 6, 10, 14, body);
        fillRect(px - 2, py - 4, 6, 4, kPal[7]);
        fillRect(px - 4, py + 6, 3, 2, kPal[14]);
        fillRect(px + 1, py + 6, 3, 2, kPal[14]);
    }
    updateAndDrawParticles(0, 0);

    char hud[40];
    snprintf(hud, sizeof(hud), "SCORE %u", s.score);
    drawText(6, 5, hud, kPal[7], 1);
    snprintf(hud, sizeof(hud), "BEST %u", s.hiScore);
    drawText(kW - 74, 5, hud, kPal[11], 1);

    if (s.screen == 2) {
        fillRect(36, 48, kW - 72, 64, kPal[1]);
        drawTextCentered(58, "GAME OVER", kPal[8], 2);
        snprintf(hud, sizeof(hud), "SCORE %u", s.score);
        drawTextCentered(80, hud, kPal[11], 1);
        if ((s.tick / 24) % 2 == 0) drawTextCentered(96, "PRESS START", kPal[7], 1);
        if (pressed(btn, RETRO_DEVICE_ID_JOYPAD_START) || pressed(btn, RETRO_DEVICE_ID_JOYPAD_A)) {
            resetPrism(true);
        }
    }
}

// ---- CUSTOM HB16-CPU BYTECODE VM (Mode 0: User-authored .hbx ROMs!) ----
// 32-bit fixed instructions: [op:8][a:4][b:4][imm16:16]
void stepCustomVm(uint16_t btn) {
    auto &s = gHb.s;
    const size_t codeOff = 40;  // 4-byte "HBX1" + 1 mode + 3 reserved + 32 title
    if (gHb.romSize <= codeOff) {
        clearBg(kPal[0], kPal[28]);
        drawTextCentered(70, "HEROBOX-16 READY", kPal[14], 1);
        return;
    }
    const size_t codeLen = gHb.romSize - codeOff;
    for (int budget = 0; budget < 4096; budget++) {
        if (s.pc * 4 + 4 > codeLen) { s.pc = 0; break; }
        const uint8_t *p = gHb.rom + codeOff + s.pc * 4;
        uint8_t op = p[0];
        uint8_t a = (p[1] >> 4) & 0xF;
        uint8_t b = p[1] & 0xF;
        int16_t imm = static_cast<int16_t>((static_cast<uint16_t>(p[2]) << 8) | p[3]);
        s.pc++;
        switch (op) {
            case 0x00: break;                                            // NOP
            case 0x01: s.reg[a] = imm; break;                            // LDI Ra, imm
            case 0x02: s.reg[a] = s.reg[b]; break;                       // MOV Ra, Rb
            case 0x03: s.reg[a] += s.reg[b]; break;                      // ADD
            case 0x04: s.reg[a] -= s.reg[b]; break;                      // SUB
            case 0x05: s.reg[a] *= s.reg[b]; break;                      // MUL
            case 0x06: if (s.reg[b]) s.reg[a] /= s.reg[b]; break;        // DIV
            case 0x07: s.reg[a] += imm; break;                           // ADDI
            case 0x08: s.reg[a] = btn; break;                            // INPUT Ra
            case 0x09: s.reg[a] = static_cast<int32_t>(rnd() & 0x7FFF); break; // RAND Ra
            case 0x10: s.pc = static_cast<uint32_t>(imm); break;         // JMP
            case 0x11: if (s.reg[a] == s.reg[b]) s.pc = static_cast<uint32_t>(imm); break; // JEQ
            case 0x12: if (s.reg[a] <  s.reg[b]) s.pc = static_cast<uint32_t>(imm); break; // JLT
            case 0x20: clearBg(kPal[a & 31], kPal[b & 31]); break;       // GPU_CLEAR
            case 0x21: fillRect(s.reg[a], s.reg[b], s.reg[(a + 1) & 15], s.reg[(b + 1) & 15], kPal[imm & 31]); break; // GPU_RECT
            case 0x22: fillCircle(s.reg[a], s.reg[b], std::max(1, static_cast<int>(imm & 0xFF)), kPal[(imm >> 8) & 31]); break; // GPU_CIRC
            case 0x30: triggerNote(a & 3, b & 3, static_cast<float>(imm), static_cast<float>(imm), 0.22f, 0.00012f); break; // APU_NOTE
            case 0xFF: return;                                           // WAIT_VBLANK
            default: break;
        }
    }
}

void resetCart(bool toPlaying) {
    switch (gHb.s.cartMode) {
        case 1: resetStarlight(toPlaying); break;
        case 2: resetBreaker(toPlaying); break;
        case 3: resetPrism(toPlaying); break;
        default: {
            uint8_t m = gHb.s.cartMode;
            memset(&gHb.s, 0, sizeof(gHb.s));
            gHb.s.cartMode = m;
            gHb.s.rng = 0x16B016u;
            gHb.s.lfsr = 0x7FFFu;
            break;
        }
    }
}

void hbInit() {}
void hbDeinit() {}
unsigned hbApiVersion() { return RETRO_API_VERSION; }

void hbGetSystemInfo(retro_system_info *info) {
    info->library_name = "HeroBox-16";
    info->library_version = "1.0";
    info->valid_extensions = "hbx";
    info->need_fullpath = false;
    info->block_extract = false;
}

void hbGetSystemAvInfo(retro_system_av_info *av) {
    av->geometry.base_width = kW;
    av->geometry.base_height = kH;
    av->geometry.max_width = kW;
    av->geometry.max_height = kH;
    av->geometry.aspect_ratio = static_cast<float>(kW) / static_cast<float>(kH);
    av->timing.fps = 60.0;
    av->timing.sample_rate = static_cast<double>(kSampleRate);
}

void hbSetEnv(retro_environment_t cb) { gHb.env = cb; }
void hbSetVideo(retro_video_refresh_t cb) { gHb.video = cb; }
void hbSetAudioSample(retro_audio_sample_t) {}
void hbSetAudioBatch(retro_audio_sample_batch_t cb) { gHb.audioBatch = cb; }
void hbSetInputPoll(retro_input_poll_t cb) { gHb.inputPoll = cb; }
void hbSetInputState(retro_input_state_t cb) { gHb.inputState = cb; }

bool hbLoadGame(const retro_game_info *game) {
    if (!game || !game->data || game->size < 8) return false;
    if (gHb.env) {
        enum retro_pixel_format fmt = RETRO_PIXEL_FORMAT_XRGB8888;
        gHb.env(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt);
    }
    gHb.romSize = std::min(game->size, sizeof(gHb.rom));
    memcpy(gHb.rom, game->data, gHb.romSize);

    // Parse "HBX1" cartridge header: [0..3]="HBX1", [4]=cartMode (0..3), [8..39]=title
    if (memcmp(gHb.rom, "HBX1", 4) != 0) return false;
    gHb.s.cartMode = gHb.rom[4];
    memset(gHb.cartTitle, 0, sizeof(gHb.cartTitle));
    if (gHb.romSize >= 40) memcpy(gHb.cartTitle, gHb.rom + 8, 31);
    resetCart(false);
    return true;
}

void hbUnloadGame() { gHb.romSize = 0; }
void hbReset() { resetCart(false); }

void hbRun() {
    if (gHb.inputPoll) gHb.inputPoll();
    uint16_t btn = pollButtons();
    gHb.s.tick++;

    switch (gHb.s.cartMode) {
        case 1: stepStarlight(btn); break;
        case 2: stepBreaker(btn); break;
        case 3: stepPrism(btn); break;
        default: stepCustomVm(btn); break;
    }
    gHb.s.prevButtons = btn;

    tickChiptuneMusic();
    if (gHb.video) gHb.video(gHb.fb, kW, kH, kW * sizeof(uint32_t));
    renderAudioFrame();
}

size_t hbSerializeSize() { return sizeof(HeroBoxState); }
bool hbSerialize(void *data, size_t size) {
    if (size < sizeof(HeroBoxState)) return false;
    memcpy(data, &gHb.s, sizeof(HeroBoxState));
    return true;
}
bool hbUnserialize(const void *data, size_t size) {
    if (size < sizeof(HeroBoxState)) return false;
    memcpy(&gHb.s, data, sizeof(HeroBoxState));
    return true;
}
void *hbMemData(unsigned) { return nullptr; }
size_t hbMemSize(unsigned) { return 0; }

}  // namespace

CoreApi heroBox16Api() {
    CoreApi api{};
    api.init = &hbInit;
    api.deinit = &hbDeinit;
    api.api_version = &hbApiVersion;
    api.get_system_info = &hbGetSystemInfo;
    api.get_system_av_info = &hbGetSystemAvInfo;
    api.set_environment = &hbSetEnv;
    api.set_video_refresh = &hbSetVideo;
    api.set_audio_sample = &hbSetAudioSample;
    api.set_audio_sample_batch = &hbSetAudioBatch;
    api.set_input_poll = &hbSetInputPoll;
    api.set_input_state = &hbSetInputState;
    api.reset = &hbReset;
    api.run = &hbRun;
    api.serialize_size = &hbSerializeSize;
    api.serialize = &hbSerialize;
    api.unserialize = &hbUnserialize;
    api.load_game = &hbLoadGame;
    api.unload_game = &hbUnloadGame;
    api.get_memory_data = &hbMemData;
    api.get_memory_size = &hbMemSize;
    return api;
}

}  // namespace rh::builtin
