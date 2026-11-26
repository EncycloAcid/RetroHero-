#pragma once

#include "frontend.h"

namespace rh::builtin {

/** RetroHero's original 16-bit fantasy console emulator (HeroBox-16: 240x160 PPU, HB16-CPU VM, 4-ch chiptune APU). */
CoreApi heroBox16Api();

/** Native built-in CHIP-8 / SUPER-CHIP-8 emulator (64x32 & 128x64, full keypad mapping, synth buzzer). */
CoreApi chip8Api();

/** RetroHero's own from-scratch native C++ NES / Famicom core (6502 CPU, 2C02 PPU, 2A03 APU, Mappers 0/1/2/3/4/7/66). */
CoreApi heroNesApi();

}  // namespace rh::builtin
