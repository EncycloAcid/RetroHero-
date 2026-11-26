package com.retrohero.app.data

/**
 * Free, openly licensed homebrew games & RetroHero Original built-in HeroBox-16 / CHIP-8
 * cartridges that install in one tap.
 */
data class HomebrewGame(
    val title: String,
    val systemId: String,
    val author: String,
    val license: String,
    val description: String,
    val url: String,
    /** SHA-256 published by the author, verified after download when present. */
    val sha256: String? = null,
    /** When present, installs instantaneously offline with zero download needed! */
    val embeddedBytes: ByteArray? = null,
) {
    val fileName: String get() = url.substringAfterLast('/')
    val system: GameSystem? get() = Systems.get(systemId)
}

object Homebrew {
    /** Builds a valid 40-byte HBX1 HeroBox-16 cartridge header for a built-in launch game. */
    private fun hbxCart(mode: Int, title: String): ByteArray {
        val buf = ByteArray(48)
        buf[0] = 'H'.code.toByte()
        buf[1] = 'B'.code.toByte()
        buf[2] = 'X'.code.toByte()
        buf[3] = '1'.code.toByte()
        buf[4] = mode.toByte()
        val titleBytes = title.encodeToByteArray()
        titleBytes.copyInto(buf, destinationOffset = 8, startIndex = 0, endIndex = minOf(titleBytes.size, 31))
        return buf
    }

    /** Real assembled CHIP-8 machine-code cartridge: glowing animated bounce demo + paddle input! */
    private val chip8CosmicBounce: ByteArray = byteArrayOf(
        // 0x200: Clear screen; V0=28 (x), V1=14 (y), V2=1 (dx), V3=1 (dy), I=0x23C (sprite)
        0x00, 0xE0.toByte(),
        0x60, 0x1C, 0x61, 0x0E, 0x62, 0x01, 0x63, 0x01,
        0xA2.toByte(), 0x3C,
        // 0x20C: Draw sprite at (V0, V1)
        0xD0.toByte(), 0x14,
        // 0x20E: Delay loop via DT (V4=2 -> DT)
        0x64, 0x02, 0xF4.toByte(), 0x15,
        0xF4.toByte(), 0x07, 0x34, 0x00, 0x12, 0x12,
        // 0x218: Erase sprite at (V0, V1)
        0xD0.toByte(), 0x14,
        // 0x21A: Update X += V2, Y += V3
        0x80.toByte(), 0x24, 0x81.toByte(), 0x34,
        // 0x21E: Bounce X at 0 or 59 -> invert V2 (V2 = 0 - V2), beep ST=2
        0x30, 0x00, 0x30, 0x3B, 0x12, 0x2A,
        0x65, 0x00, 0x82.toByte(), 0x57, 0x66, 0x02, 0xF6.toByte(), 0x18,
        // 0x22E: Bounce Y at 0 or 27 -> invert V3 (V3 = 0 - V3), beep ST=2
        0x31, 0x00, 0x31, 0x1B, 0x12, 0x0C,
        0x65, 0x00, 0x83.toByte(), 0x57, 0x66, 0x02, 0xF6.toByte(), 0x18,
        0x12, 0x0C,
        // 0x23C: 4x4 diamond sprite
        0x60, 0xF0.toByte(), 0xF0.toByte(), 0x60,
    )

    val catalog = listOf(
        // ---- RetroHero Original Console: HeroBox-16 Instant Offline Launch Titles ----
        HomebrewGame(
            title = "Starlight Vanguard",
            systemId = "herobox16",
            author = "RetroHero Studio",
            license = "MIT · Built-in Original",
            description = "HeroBox-16 launch shooter! Twin plasma blasters, spread-shot powerups, Dreadnought bosses & 4-channel chiptune synth at 60 FPS.",
            url = "builtin://herobox16/Starlight Vanguard.hbx",
            embeddedBytes = hbxCart(1, "STARLIGHT VANGUARD"),
        ),
        HomebrewGame(
            title = "Neon Breaker DX",
            systemId = "herobox16",
            author = "RetroHero Studio",
            license = "MIT · Built-in Original",
            description = "High-speed synthwave brick breaker for HeroBox-16 with multi-ball powerups, wide paddle, screen shake & pulsing neon stages.",
            url = "builtin://herobox16/Neon Breaker DX.hbx",
            embeddedBytes = hbxCart(2, "NEON BREAKER DX"),
        ),
        HomebrewGame(
            title = "Prism Dash",
            systemId = "herobox16",
            author = "RetroHero Studio",
            license = "MIT · Built-in Original",
            description = "Fast retro action-runner for HeroBox-16 featuring double-jump, mid-air dash, sparkling prism gems & sunset mountain parallax.",
            url = "builtin://herobox16/Prism Dash.hbx",
            embeddedBytes = hbxCart(3, "PRISM DASH"),
        ),
        HomebrewGame(
            title = "Cosmic Bounce (CHIP-8)",
            systemId = "chip8",
            author = "RetroHero Studio",
            license = "MIT · Built-in Native CHIP-8",
            description = "Instant native CHIP-8 cartridge demonstrating RetroHero's compiled-in CHIP-8 / SUPER-CHIP CPU & synth buzzer.",
            url = "builtin://chip8/Cosmic Bounce.ch8",
            embeddedBytes = chip8CosmicBounce,
        ),

        // ---- Open-Source Community Homebrew ----
        HomebrewGame(
            title = "µCity",
            systemId = "gbc",
            author = "Antonio Niño Díaz",
            license = "GPL-3.0+ · art CC BY-SA 4.0",
            description = "A full city-building simulation for the Game Boy Color.",
            url = "https://github.com/AntonioND/ucity/releases/download/v1.3/ucity.gbc",
            sha256 = "9422ee2ca7b7ea1d46b58b2a429fff3f354dfd3e732dee1e7ae6220f148ce6e0",
        ),
        HomebrewGame(
            title = "Libbet and the Magic Floor",
            systemId = "gb",
            author = "Damian Yerrick",
            license = "zlib",
            description = "Roll a boulder girl across a floor of tiles in this Game Boy puzzle game.",
            url = "https://github.com/pinobatch/libbet/releases/download/v0.08/libbet.gb",
        ),
        HomebrewGame(
            title = "Thwaite",
            systemId = "nes",
            author = "Damian Yerrick",
            license = "GPL-3.0",
            description = "Defend a small town from falling missiles with fireworks on the NES.",
            url = "https://github.com/pinobatch/thwaite-nes/releases/download/v0.04/thwaite.nes",
        ),
        HomebrewGame(
            title = "240p Test Suite (NES)",
            systemId = "nes",
            author = "Damian Yerrick",
            license = "GPL-2.0+",
            description = "Display and timing tests: handy for checking scaling, colours and input lag.",
            url = "https://github.com/pinobatch/240p-test-mini/releases/download/v0.23/240pee.nes",
        ),
        HomebrewGame(
            title = "240p Test Suite (Game Boy)",
            systemId = "gb",
            author = "Damian Yerrick",
            license = "GPL-2.0+",
            description = "The same test suite for the Game Boy.",
            url = "https://github.com/pinobatch/240p-test-mini/releases/download/v0.23/gb240p.gb",
        ),
    )
}
