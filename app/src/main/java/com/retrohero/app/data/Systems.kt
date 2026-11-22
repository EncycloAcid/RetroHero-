package com.retrohero.app.data

import androidx.compose.ui.graphics.Color

/** libretro RetroPad button ids (RETRO_DEVICE_ID_JOYPAD_*). */
object RetroPad {
    const val B = 0
    const val Y = 1
    const val SELECT = 2
    const val START = 3
    const val UP = 4
    const val DOWN = 5
    const val LEFT = 6
    const val RIGHT = 7
    const val A = 8
    const val X = 9
    const val L = 10
    const val R = 11
    const val L2 = 12
    const val R2 = 13
    const val L3 = 14
    const val R3 = 15
}

data class PadButton(val label: String, val id: Int)

/**
 * Virtual gamepad description. [diamond] holds face buttons in top/left/right/bottom order;
 * otherwise [rows] are drawn as staggered rows (bottom row last).
 */
data class PadLayout(
    val diamond: List<PadButton>? = null,
    val rows: List<List<PadButton>> = emptyList(),
    val leftShoulders: List<PadButton> = emptyList(),
    val rightShoulders: List<PadButton> = emptyList(),
    val select: String? = "SELECT",
    val start: String? = "START",
)

data class CoreInfo(
    val id: String,          // libretro core name, e.g. "snes9x"
    val displayName: String,
    val license: String,
)

/** A BIOS file a core may need. [md5] identifies the known good dump (null = any file accepted). */
data class BiosFile(val fileName: String, val description: String, val required: Boolean, val md5: String? = null)

data class GameSystem(
    val id: String,
    val name: String,
    val shortName: String,
    val manufacturer: String,
    val extensions: Set<String>,
    val cores: List<CoreInfo>,
    /** libretro-thumbnails / database playlist name, used for box art. */
    val thumbnailDb: String,
    val layout: PadLayout,
    val color: Color,
    /** Folder names that hint a file belongs to this system (lowercase). */
    val folderHints: Set<String> = emptySet(),
    val bios: List<BiosFile> = emptyList(),
    /** Touch on the game screen is sent to the core as a pointer (e.g. DS touch screen). */
    val touchScreen: Boolean = false,
    /** Extensions that should be passed to the core still zipped (arcade romsets). */
    val keepZipped: Boolean = false,
)

private object Cores {
    val fceumm = CoreInfo("fceumm", "FCEUmm", "GPLv2")
    val nestopia = CoreInfo("nestopia", "Nestopia UE", "GPLv2")
    val snes9x = CoreInfo("snes9x", "Snes9x", "Non-commercial")
    val gambatte = CoreInfo("gambatte", "Gambatte", "GPLv2")
    val mgba = CoreInfo("mgba", "mGBA", "MPLv2.0")
    val vbaNext = CoreInfo("vba_next", "VBA Next", "GPLv2")
    val desmume = CoreInfo("desmume", "DeSmuME", "GPLv2")
    val melonds = CoreInfo("melonds", "melonDS", "GPLv3")
    val vb = CoreInfo("mednafen_vb", "Beetle VB", "GPLv2")
    val gpgx = CoreInfo("genesis_plus_gx", "Genesis Plus GX", "Non-commercial")
    val picodrive = CoreInfo("picodrive", "PicoDrive", "Non-commercial")
    val pcsx = CoreInfo("pcsx_rearmed", "PCSX ReARMed", "GPLv2")
    val swanstation = CoreInfo("swanstation", "SwanStation", "GPLv3")
    val pce = CoreInfo("mednafen_pce_fast", "Beetle PCE Fast", "GPLv2")
    val ngp = CoreInfo("mednafen_ngp", "Beetle NeoPop", "GPLv2")
    val wswan = CoreInfo("mednafen_wswan", "Beetle WonderSwan", "GPLv2")
    val handy = CoreInfo("handy", "Handy", "Zlib")
    val stella = CoreInfo("stella", "Stella", "GPLv2")
    val prosystem = CoreInfo("prosystem", "ProSystem", "GPLv2")
    val fbneo = CoreInfo("fbneo", "FinalBurn Neo", "Non-commercial")
    val mame2003 = CoreInfo("mame2003_plus", "MAME 2003-Plus", "MAME (non-commercial)")
}

private object Layouts {
    val nes = PadLayout(rows = listOf(listOf(PadButton("B", RetroPad.B), PadButton("A", RetroPad.A))))
    val gb = nes
    val snes = PadLayout(
        diamond = listOf(
            PadButton("X", RetroPad.X), PadButton("Y", RetroPad.Y),
            PadButton("A", RetroPad.A), PadButton("B", RetroPad.B),
        ),
        leftShoulders = listOf(PadButton("L", RetroPad.L)),
        rightShoulders = listOf(PadButton("R", RetroPad.R)),
    )
    val gba = nes.copy(
        leftShoulders = listOf(PadButton("L", RetroPad.L)),
        rightShoulders = listOf(PadButton("R", RetroPad.R)),
    )
    val nds = snes
    val vb = gba
    val genesis = PadLayout(
        rows = listOf(listOf(PadButton("A", RetroPad.Y), PadButton("B", RetroPad.B), PadButton("C", RetroPad.A))),
        select = "MODE",
    )
    val sms = PadLayout(
        rows = listOf(listOf(PadButton("1", RetroPad.B), PadButton("2", RetroPad.A))),
        select = null, start = "PAUSE",
    )
    val gg = PadLayout(rows = listOf(listOf(PadButton("1", RetroPad.B), PadButton("2", RetroPad.A))), select = null)
    val psx = PadLayout(
        diamond = listOf(
            PadButton("△", RetroPad.X), PadButton("□", RetroPad.Y),
            PadButton("○", RetroPad.A), PadButton("✕", RetroPad.B),
        ),
        leftShoulders = listOf(PadButton("L1", RetroPad.L), PadButton("L2", RetroPad.L2)),
        rightShoulders = listOf(PadButton("R1", RetroPad.R), PadButton("R2", RetroPad.R2)),
    )
    val pce = PadLayout(
        rows = listOf(listOf(PadButton("II", RetroPad.B), PadButton("I", RetroPad.A))),
        start = "RUN",
    )
    val ngp = PadLayout(
        rows = listOf(listOf(PadButton("A", RetroPad.B), PadButton("B", RetroPad.A))),
        select = null, start = "OPTION",
    )
    val wswan = PadLayout(
        rows = listOf(listOf(PadButton("B", RetroPad.B), PadButton("A", RetroPad.A))),
        select = null,
    )
    val lynx = PadLayout(
        rows = listOf(listOf(PadButton("B", RetroPad.B), PadButton("A", RetroPad.A))),
        leftShoulders = listOf(PadButton("OPT1", RetroPad.L)),
        rightShoulders = listOf(PadButton("OPT2", RetroPad.R)),
        select = null, start = "PAUSE",
    )
    val atari2600 = PadLayout(
        rows = listOf(listOf(PadButton("FIRE", RetroPad.B))),
        select = "SELECT", start = "RESET",
    )
    val atari7800 = PadLayout(
        rows = listOf(listOf(PadButton("2", RetroPad.A), PadButton("1", RetroPad.B))),
        select = "SELECT", start = "PAUSE",
    )
    val arcade = PadLayout(
        rows = listOf(
            listOf(PadButton("Y", RetroPad.Y), PadButton("X", RetroPad.X), PadButton("L", RetroPad.L)),
            listOf(PadButton("B", RetroPad.B), PadButton("A", RetroPad.A), PadButton("R", RetroPad.R)),
        ),
        select = "COIN", start = "START",
    )
}

object Systems {
    val all: List<GameSystem> = listOf(
        GameSystem(
            id = "nes", name = "Nintendo Entertainment System", shortName = "NES", manufacturer = "Nintendo",
            extensions = setOf("nes", "fds", "unf", "unif"),
            cores = listOf(Cores.fceumm, Cores.nestopia),
            thumbnailDb = "Nintendo - Nintendo Entertainment System",
            layout = Layouts.nes, color = Color(0xFFE53935),
            folderHints = setOf("nes", "famicom", "fds"),
            bios = listOf(BiosFile("disksys.rom", "Famicom Disk System BIOS", false, md5 = "ca30b50f880eb660a320674ed365ef7a")),
        ),
        GameSystem(
            id = "snes", name = "Super Nintendo", shortName = "SNES", manufacturer = "Nintendo",
            extensions = setOf("sfc", "smc", "fig", "swc", "bs"),
            cores = listOf(Cores.snes9x),
            thumbnailDb = "Nintendo - Super Nintendo Entertainment System",
            layout = Layouts.snes, color = Color(0xFF7E57C2),
            folderHints = setOf("snes", "sfc", "superfamicom", "super nintendo"),
        ),
        GameSystem(
            id = "gb", name = "Game Boy", shortName = "GB", manufacturer = "Nintendo",
            extensions = setOf("gb"),
            cores = listOf(Cores.gambatte, Cores.mgba),
            thumbnailDb = "Nintendo - Game Boy",
            layout = Layouts.gb, color = Color(0xFF8BC34A),
            folderHints = setOf("gb", "gameboy"),
        ),
        GameSystem(
            id = "gbc", name = "Game Boy Color", shortName = "GBC", manufacturer = "Nintendo",
            extensions = setOf("gbc"),
            cores = listOf(Cores.gambatte, Cores.mgba),
            thumbnailDb = "Nintendo - Game Boy Color",
            layout = Layouts.gb, color = Color(0xFF26C6DA),
            folderHints = setOf("gbc"),
        ),
        GameSystem(
            id = "gba", name = "Game Boy Advance", shortName = "GBA", manufacturer = "Nintendo",
            extensions = setOf("gba"),
            cores = listOf(Cores.mgba, Cores.vbaNext),
            thumbnailDb = "Nintendo - Game Boy Advance",
            layout = Layouts.gba, color = Color(0xFF5C6BC0),
            folderHints = setOf("gba"),
            bios = listOf(BiosFile("gba_bios.bin", "GBA BIOS (optional, improves accuracy)", false, md5 = "a860e8c0b6d573d191e4ec7db1b1e4f6")),
        ),
        GameSystem(
            id = "nds", name = "Nintendo DS", shortName = "NDS", manufacturer = "Nintendo",
            extensions = setOf("nds"),
            cores = listOf(Cores.desmume, Cores.melonds),
            thumbnailDb = "Nintendo - Nintendo DS",
            layout = Layouts.nds, color = Color(0xFF90A4AE),
            folderHints = setOf("nds", "ds"),
            bios = listOf(
                BiosFile("bios7.bin", "DS ARM7 BIOS (melonDS)", false, md5 = "df692a80a5b1bc90728bc3dfc76cd948"),
                BiosFile("bios9.bin", "DS ARM9 BIOS (melonDS)", false, md5 = "a392174eb3e572fed6447e956bde4b25"),
                BiosFile("firmware.bin", "DS firmware (melonDS)", false),
            ),
            touchScreen = true,
        ),
        GameSystem(
            id = "vb", name = "Virtual Boy", shortName = "VB", manufacturer = "Nintendo",
            extensions = setOf("vb", "vboy"),
            cores = listOf(Cores.vb),
            thumbnailDb = "Nintendo - Virtual Boy",
            layout = Layouts.vb, color = Color(0xFFD32F2F),
            folderHints = setOf("vb", "virtualboy"),
        ),
        GameSystem(
            id = "genesis", name = "Sega Genesis / Mega Drive", shortName = "Genesis", manufacturer = "Sega",
            extensions = setOf("md", "gen", "smd", "68k", "sgd", "bin"),
            cores = listOf(Cores.gpgx, Cores.picodrive),
            thumbnailDb = "Sega - Mega Drive - Genesis",
            layout = Layouts.genesis, color = Color(0xFF1E88E5),
            folderHints = setOf("genesis", "megadrive", "mega drive", "md"),
        ),
        GameSystem(
            id = "sms", name = "Sega Master System", shortName = "SMS", manufacturer = "Sega",
            extensions = setOf("sms"),
            cores = listOf(Cores.gpgx, Cores.picodrive),
            thumbnailDb = "Sega - Master System - Mark III",
            layout = Layouts.sms, color = Color(0xFF00897B),
            folderHints = setOf("sms", "mastersystem"),
        ),
        GameSystem(
            id = "gg", name = "Sega Game Gear", shortName = "GG", manufacturer = "Sega",
            extensions = setOf("gg"),
            cores = listOf(Cores.gpgx),
            thumbnailDb = "Sega - Game Gear",
            layout = Layouts.gg, color = Color(0xFF424242),
            folderHints = setOf("gg", "gamegear"),
        ),
        GameSystem(
            id = "segacd", name = "Sega CD / Mega-CD", shortName = "Sega CD", manufacturer = "Sega",
            extensions = setOf("cue", "chd", "iso"),
            cores = listOf(Cores.gpgx, Cores.picodrive),
            thumbnailDb = "Sega - Mega-CD - Sega CD",
            layout = Layouts.genesis, color = Color(0xFF3949AB),
            folderHints = setOf("segacd", "megacd", "sega cd", "mega cd"),
            bios = listOf(
                BiosFile("bios_CD_U.bin", "Sega CD BIOS (USA)", true, md5 = "2efd74e3232ff260e371b99f84024f7f"),
                BiosFile("bios_CD_E.bin", "Mega-CD BIOS (Europe)", false, md5 = "e66fa1dc5820d254611fdcdba0662372"),
                BiosFile("bios_CD_J.bin", "Mega-CD BIOS (Japan)", false, md5 = "278a9397d192149e84e820ac621a8edd"),
            ),
        ),
        GameSystem(
            id = "32x", name = "Sega 32X", shortName = "32X", manufacturer = "Sega",
            extensions = setOf("32x"),
            cores = listOf(Cores.picodrive),
            thumbnailDb = "Sega - 32X",
            layout = Layouts.genesis, color = Color(0xFFFFB300),
            folderHints = setOf("32x", "sega32x"),
        ),
        GameSystem(
            id = "psx", name = "Sony PlayStation", shortName = "PS1", manufacturer = "Sony",
            extensions = setOf("cue", "chd", "pbp", "m3u", "bin", "img", "iso"),
            cores = listOf(Cores.pcsx, Cores.swanstation),
            thumbnailDb = "Sony - PlayStation",
            layout = Layouts.psx, color = Color(0xFF9E9E9E),
            folderHints = setOf("psx", "ps1", "playstation"),
            bios = listOf(
                BiosFile("scph5501.bin", "PS1 BIOS (USA) — optional for PCSX ReARMed", false, md5 = "490f666e1afb15b7362b406ed1cea246"),
                BiosFile("scph5500.bin", "PS1 BIOS (Japan)", false, md5 = "8dd7d5296a650fac7319bce665a6a53c"),
                BiosFile("scph5502.bin", "PS1 BIOS (Europe)", false, md5 = "32736f17079d0b2b7024407c39bd3050"),
            ),
        ),
        GameSystem(
            id = "pce", name = "PC Engine / TurboGrafx-16", shortName = "PCE", manufacturer = "NEC",
            extensions = setOf("pce", "sgx"),
            cores = listOf(Cores.pce),
            thumbnailDb = "NEC - PC Engine - TurboGrafx 16",
            layout = Layouts.pce, color = Color(0xFFF4511E),
            folderHints = setOf("pce", "pcengine", "tg16", "turbografx"),
        ),
        GameSystem(
            id = "pcecd", name = "PC Engine CD / TurboGrafx-CD", shortName = "PCE-CD", manufacturer = "NEC",
            extensions = setOf("cue", "chd"),
            cores = listOf(Cores.pce),
            thumbnailDb = "NEC - PC Engine CD - TurboGrafx-CD",
            layout = Layouts.pce, color = Color(0xFFBF360C),
            folderHints = setOf("pcecd", "pce-cd", "tgcd", "turbografxcd", "turbografx-cd"),
            bios = listOf(BiosFile("syscard3.pce", "Super CD-ROM² System Card 3", true, md5 = "38179df8f4ac870017db21ebcbf53114")),
        ),
        GameSystem(
            id = "ngp", name = "Neo Geo Pocket (Color)", shortName = "NGP", manufacturer = "SNK",
            extensions = setOf("ngp", "ngc"),
            cores = listOf(Cores.ngp),
            thumbnailDb = "SNK - Neo Geo Pocket Color",
            layout = Layouts.ngp, color = Color(0xFF6D4C41),
            folderHints = setOf("ngp", "ngpc"),
        ),
        GameSystem(
            id = "ws", name = "WonderSwan (Color)", shortName = "WS", manufacturer = "Bandai",
            extensions = setOf("ws", "wsc"),
            cores = listOf(Cores.wswan),
            thumbnailDb = "Bandai - WonderSwan Color",
            layout = Layouts.wswan, color = Color(0xFF00ACC1),
            folderHints = setOf("ws", "wsc", "wonderswan"),
        ),
        GameSystem(
            id = "lynx", name = "Atari Lynx", shortName = "Lynx", manufacturer = "Atari",
            extensions = setOf("lnx"),
            cores = listOf(Cores.handy),
            thumbnailDb = "Atari - Lynx",
            layout = Layouts.lynx, color = Color(0xFFFF7043),
            folderHints = setOf("lynx"),
            bios = listOf(BiosFile("lynxboot.img", "Lynx boot ROM", true, md5 = "fcd403db69f54290b51035d82f835e7b")),
        ),
        GameSystem(
            id = "atari2600", name = "Atari 2600", shortName = "2600", manufacturer = "Atari",
            extensions = setOf("a26"),
            cores = listOf(Cores.stella),
            thumbnailDb = "Atari - 2600",
            layout = Layouts.atari2600, color = Color(0xFF8D6E63),
            folderHints = setOf("atari2600", "2600", "atari"),
        ),
        GameSystem(
            id = "atari7800", name = "Atari 7800", shortName = "7800", manufacturer = "Atari",
            extensions = setOf("a78"),
            cores = listOf(Cores.prosystem),
            thumbnailDb = "Atari - 7800",
            layout = Layouts.atari7800, color = Color(0xFFA1887F),
            folderHints = setOf("atari7800", "7800"),
            bios = listOf(BiosFile("7800 BIOS (U).rom", "Atari 7800 BIOS (optional)", false, md5 = "0763f1ffb006ddbe32e52d497ee848ae")),
        ),
        GameSystem(
            id = "arcade", name = "Arcade", shortName = "Arcade", manufacturer = "Various",
            extensions = setOf("zip", "7z"),
            cores = listOf(Cores.fbneo, Cores.mame2003),
            thumbnailDb = "FBNeo - Arcade Games",
            layout = Layouts.arcade, color = Color(0xFFFFD600),
            folderHints = setOf("arcade", "mame", "fbneo", "fba", "neogeo", "cps1", "cps2", "cps3"),
            bios = listOf(BiosFile("neogeo.zip", "Neo Geo BIOS set (for Neo Geo games)", false)),
            keepZipped = true,
        ),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): GameSystem? = byId[id]

    fun allCores(): List<Pair<CoreInfo, List<GameSystem>>> =
        all.flatMap { s -> s.cores.map { it to s } }
            .groupBy({ it.first.id }, { it.second })
            .map { (id, systems) -> all.flatMap { it.cores }.first { it.id == id } to systems }
}
