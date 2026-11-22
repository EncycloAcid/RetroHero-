package com.retrohero.app.data

/**
 * Free, openly licensed homebrew games that can be downloaded in one tap. Every URL is the
 * author's own GitHub release asset; licenses allow redistribution.
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
) {
    val fileName: String get() = url.substringAfterLast('/')
    val system: GameSystem? get() = Systems.get(systemId)
}

object Homebrew {
    val catalog = listOf(
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
