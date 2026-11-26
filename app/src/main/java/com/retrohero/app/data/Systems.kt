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
    /** Override buildbot archive filename when it differs from "<id>_libretro_android.so.zip". */
    val archiveName: String? = null,
    /** Compiled directly into libretrohero.so (0ms instant launch, zero download). */
    val builtIn: Boolean = false,
) {
    fun archiveFileName(): String = archiveName ?: "${id}_libretro_android.so.zip"
}

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
    val bsnes = CoreInfo("bsnes", "bsnes", "GPLv3")
    val mupen64 = CoreInfo("mupen64plus_next_gles3", "Mupen64Plus-Next (GLES3)", "GPLv2")
    val parallelN64 = CoreInfo("parallel_n64", "ParaLLEl N64", "GPLv2")
    val gambatte = CoreInfo("gambatte", "Gambatte", "GPLv2")
    val sameboy = CoreInfo("sameboy", "SameBoy", "MIT")
    val mgba = CoreInfo("mgba", "mGBA", "MPLv2.0")
    val gpsp = CoreInfo("gpsp", "gpSP (Fast ARM JIT)", "GPLv2")
    val vbaNext = CoreInfo("vba_next", "VBA Next", "GPLv2")
    val melondsDs = CoreInfo("melondsds", "melonDS DS", "GPLv3")
    val desmume = CoreInfo("desmume", "DeSmuME", "GPLv2")
    val melonds = CoreInfo("melonds", "melonDS", "GPLv3")
    val azahar = CoreInfo("azahar", "Azahar 3DS (GLES3)", "GPLv2+", archiveName = "azahar_libretro.so.zip")
    val citra = CoreInfo("citra", "Citra (GLES3)", "GPLv2")
    val dolphin = CoreInfo("dolphin", "Dolphin (GLES3)", "GPLv2+")
    val pokemini = CoreInfo("pokemini", "PokeMini", "GPLv3")
    val vb = CoreInfo("mednafen_vb", "Beetle VB", "GPLv2")
    val gw = CoreInfo("gw", "GW (Game & Watch)", "Zlib")
    val gpgx = CoreInfo("genesis_plus_gx", "Genesis Plus GX", "Non-commercial")
    val gpgxWide = CoreInfo("genesis_plus_gx_wide", "Genesis Plus GX Wide", "Non-commercial")
    val picodrive = CoreInfo("picodrive", "PicoDrive", "Non-commercial")
    val gearsystem = CoreInfo("gearsystem", "Gearsystem", "GPLv3")
    val yabasanshiro = CoreInfo("yabasanshiro", "YabaSanshiro (GLES3)", "GPLv2")
    val ymir = CoreInfo("ymir", "Ymir Saturn", "GPLv3")
    val beetleSaturn = CoreInfo("mednafen_saturn", "Beetle Saturn", "GPLv2")
    val flycast = CoreInfo("flycast", "Flycast (GLES3)", "GPLv2")
    val parallelDc = CoreInfo("parallel_dc", "ParaLLEl DC", "GPLv2")
    val pcsx = CoreInfo("pcsx_rearmed", "PCSX ReARMed", "GPLv2")
    val swanstation = CoreInfo("swanstation", "SwanStation (HD GLES3)", "GPLv3")
    val beetlePsxHw = CoreInfo("mednafen_psx_hw", "Beetle PSX HW (GLES3)", "GPLv2")
    val ppsspp = CoreInfo("ppsspp", "PPSSPP (GLES3)", "GPLv2+")
    val lrps2 = CoreInfo("pcsx2", "LRPS2 / PCSX2 (GLES3)", "GPLv3")
    val playPs2 = CoreInfo("play", "Play! (HLE · No BIOS)", "BSD-2-Clause")
    val pce = CoreInfo("mednafen_pce_fast", "Beetle PCE Fast", "GPLv2")
    val supergrafx = CoreInfo("mednafen_supergrafx", "Beetle SuperGrafx", "GPLv2")
    val pcfx = CoreInfo("mednafen_pcfx", "Beetle PC-FX", "GPLv2")
    val ngp = CoreInfo("mednafen_ngp", "Beetle NeoPop", "GPLv2")
    val neocd = CoreInfo("neocd", "NeoCD", "GPLv3")
    val wswan = CoreInfo("mednafen_wswan", "Beetle WonderSwan", "GPLv2")
    val handy = CoreInfo("handy", "Handy", "Zlib")
    val stella = CoreInfo("stella", "Stella", "GPLv2")
    val a5200 = CoreInfo("a5200", "a5200", "GPLv2")
    val prosystem = CoreInfo("prosystem", "ProSystem", "GPLv2")
    val virtualjaguar = CoreInfo("virtualjaguar", "Virtual Jaguar", "GPLv3")
    val gearcoleco = CoreInfo("gearcoleco", "Gearcoleco", "GPLv3")
    val freeintv = CoreInfo("freeintv", "FreeIntv", "GPLv3")
    val vecx = CoreInfo("vecx", "vecx", "GPLv3")
    val o2em = CoreInfo("o2em", "O2EM", "Artistic")
    val opera = CoreInfo("opera", "Opera (3DO)", "GPLv2+")
    val sameCdi = CoreInfo("same_cdi", "SAME CDi", "BSD-3-Clause")
    val potator = CoreInfo("potator", "Potator (Supervision)", "MIT")
    val freechaf = CoreInfo("freechaf", "FreeChaF (Channel F)", "GPLv3")
    val fmsx = CoreInfo("fmsx", "fMSX", "Non-commercial")
    val bluemsx = CoreInfo("bluemsx", "blueMSX", "GPLv2")
    val viceC64 = CoreInfo("vice_x64", "VICE x64", "GPLv2")
    val puae = CoreInfo("puae", "PUAE (Amiga)", "GPLv2")
    val fuse = CoreInfo("fuse", "Fuse (ZX Spectrum)", "GPLv2")
    val cap32 = CoreInfo("cap32", "Caprice32 (Amstrad CPC)", "GPLv2")
    val hatari = CoreInfo("hatari", "Hatari (Atari ST)", "GPLv2")
    val px68k = CoreInfo("px68k", "PX68k (Sharp X68000)", "GPLv2")
    val np2kai = CoreInfo("np2kai", "Neko Project II Kai (PC-98)", "MIT")
    val dosboxPure = CoreInfo("dosbox_pure", "DOSBox-Pure", "GPLv2")
    val scummvm = CoreInfo("scummvm", "ScummVM", "GPLv3+")
    val squirreljme = CoreInfo("squirreljme", "SquirrelJME (Java J2ME)", "GPLv3")
    val fake08 = CoreInfo("fake08", "Fake-08 (PICO-8)", "MIT")
    val tic80 = CoreInfo("tic80", "TIC-80", "MIT")
    val fbneo = CoreInfo("fbneo", "FinalBurn Neo", "Non-commercial")
    val mame2003 = CoreInfo("mame2003_plus", "MAME 2003-Plus", "MAME (non-commercial)")
    val mame2016 = CoreInfo("mame2016", "MAME 2016", "GPLv2")
    val herobox16 = CoreInfo("herobox16", "HeroBox-16 Native", "MIT", builtIn = true)
    val retroheroChip8 = CoreInfo("retrohero_chip8", "RetroHero CHIP-8 Native", "MIT", builtIn = true)
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
    val n64 = PadLayout(
        rows = listOf(
            listOf(PadButton("C▲", RetroPad.X), PadButton("C◄", RetroPad.Y), PadButton("C►", RetroPad.R2)),
            listOf(PadButton("B", RetroPad.B), PadButton("A", RetroPad.A), PadButton("C▼", RetroPad.L2)),
        ),
        leftShoulders = listOf(PadButton("L", RetroPad.L), PadButton("Z", RetroPad.L2)),
        rightShoulders = listOf(PadButton("R", RetroPad.R)),
        select = null,
        start = "START",
    )
    val gc = PadLayout(
        diamond = listOf(
            PadButton("Y", RetroPad.Y), PadButton("X", RetroPad.X),
            PadButton("A", RetroPad.A), PadButton("B", RetroPad.B),
        ),
        leftShoulders = listOf(PadButton("L", RetroPad.L)),
        rightShoulders = listOf(PadButton("R", RetroPad.R), PadButton("Z", RetroPad.R2)),
        select = null,
        start = "START",
    )
    val gba = nes.copy(
        leftShoulders = listOf(PadButton("L", RetroPad.L)),
        rightShoulders = listOf(PadButton("R", RetroPad.R)),
    )
    val nds = snes
    val n3ds = snes.copy(
        leftShoulders = listOf(PadButton("L", RetroPad.L), PadButton("ZL", RetroPad.L2)),
        rightShoulders = listOf(PadButton("R", RetroPad.R), PadButton("ZR", RetroPad.R2)),
    )
    val vb = gba
    val genesis = PadLayout(
        rows = listOf(listOf(PadButton("A", RetroPad.Y), PadButton("B", RetroPad.B), PadButton("C", RetroPad.A))),
        select = "MODE",
    )
    val saturn = PadLayout(
        rows = listOf(
            listOf(PadButton("X", RetroPad.L), PadButton("Y", RetroPad.X), PadButton("Z", RetroPad.R)),
            listOf(PadButton("A", RetroPad.Y), PadButton("B", RetroPad.B), PadButton("C", RetroPad.A)),
        ),
        leftShoulders = listOf(PadButton("L", RetroPad.L2)),
        rightShoulders = listOf(PadButton("R", RetroPad.R2)),
        select = null,
        start = "START",
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
    val psp = PadLayout(
        diamond = listOf(
            PadButton("△", RetroPad.X), PadButton("□", RetroPad.Y),
            PadButton("○", RetroPad.A), PadButton("✕", RetroPad.B),
        ),
        leftShoulders = listOf(PadButton("L", RetroPad.L)),
        rightShoulders = listOf(PadButton("R", RetroPad.R)),
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
        // ---- Nintendo ---------------------------------------------------------------------------
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
            cores = listOf(Cores.snes9x, Cores.bsnes),
            thumbnailDb = "Nintendo - Super Nintendo Entertainment System",
            layout = Layouts.snes, color = Color(0xFF7E57C2),
            folderHints = setOf("snes", "sfc", "superfamicom", "super nintendo"),
        ),
        GameSystem(
            id = "n64", name = "Nintendo 64", shortName = "N64", manufacturer = "Nintendo",
            extensions = setOf("n64", "z64", "v64", "ndd"),
            cores = listOf(Cores.mupen64, Cores.parallelN64),
            thumbnailDb = "Nintendo - Nintendo 64",
            layout = Layouts.n64, color = Color(0xFF43A047),
            folderHints = setOf("n64", "nintendo64", "nintendo 64"),
        ),
        GameSystem(
            id = "gb", name = "Game Boy", shortName = "GB", manufacturer = "Nintendo",
            extensions = setOf("gb"),
            cores = listOf(Cores.gambatte, Cores.sameboy, Cores.mgba),
            thumbnailDb = "Nintendo - Game Boy",
            layout = Layouts.gb, color = Color(0xFF8BC34A),
            folderHints = setOf("gb", "gameboy"),
        ),
        GameSystem(
            id = "gbc", name = "Game Boy Color", shortName = "GBC", manufacturer = "Nintendo",
            extensions = setOf("gbc"),
            cores = listOf(Cores.gambatte, Cores.sameboy, Cores.mgba),
            thumbnailDb = "Nintendo - Game Boy Color",
            layout = Layouts.gb, color = Color(0xFF26C6DA),
            folderHints = setOf("gbc", "gameboycolor"),
        ),
        GameSystem(
            id = "gba", name = "Game Boy Advance", shortName = "GBA", manufacturer = "Nintendo",
            extensions = setOf("gba"),
            cores = listOf(Cores.mgba, Cores.gpsp, Cores.vbaNext),
            thumbnailDb = "Nintendo - Game Boy Advance",
            layout = Layouts.gba, color = Color(0xFF5C6BC0),
            folderHints = setOf("gba", "gameboyadvance"),
            bios = listOf(BiosFile("gba_bios.bin", "GBA BIOS (optional, improves accuracy)", false, md5 = "a860e8c0b6d573d191e4ec7db1b1e4f6")),
        ),
        GameSystem(
            id = "nds", name = "Nintendo DS", shortName = "NDS", manufacturer = "Nintendo",
            extensions = setOf("nds"),
            cores = listOf(Cores.melondsDs, Cores.desmume, Cores.melonds),
            thumbnailDb = "Nintendo - Nintendo DS",
            layout = Layouts.nds, color = Color(0xFF90A4AE),
            folderHints = setOf("nds", "ds", "nintendods"),
            bios = listOf(
                BiosFile("bios7.bin", "DS ARM7 BIOS (melonDS)", false, md5 = "df692a80a5b1bc90728bc3dfc76cd948"),
                BiosFile("bios9.bin", "DS ARM9 BIOS (melonDS)", false, md5 = "a392174eb3e572fed6447e956bde4b25"),
                BiosFile("firmware.bin", "DS firmware (melonDS)", false),
            ),
            touchScreen = true,
        ),
        GameSystem(
            id = "3ds", name = "Nintendo 3DS", shortName = "3DS", manufacturer = "Nintendo",
            extensions = setOf("3ds", "3dsx", "cci", "cia", "cxi"),
            cores = listOf(Cores.azahar, Cores.citra),
            thumbnailDb = "Nintendo - Nintendo 3DS",
            layout = Layouts.n3ds, color = Color(0xFFE53935),
            folderHints = setOf("3ds", "n3ds", "nintendo 3ds", "nintendo3ds"),
            touchScreen = true,
        ),
        GameSystem(
            id = "gc", name = "Nintendo GameCube", shortName = "GameCube", manufacturer = "Nintendo",
            extensions = setOf("gcm", "gcz", "rvz", "ciso", "dol"),
            cores = listOf(Cores.dolphin),
            thumbnailDb = "Nintendo - GameCube",
            layout = Layouts.gc, color = Color(0xFF5E35B1),
            folderHints = setOf("gc", "ngc", "gamecube", "nintendo gamecube"),
        ),
        GameSystem(
            id = "wii", name = "Nintendo Wii", shortName = "Wii", manufacturer = "Nintendo",
            extensions = setOf("wbfs", "wia", "wad"),
            cores = listOf(Cores.dolphin),
            thumbnailDb = "Nintendo - Wii",
            layout = Layouts.snes, color = Color(0xFF039BE5),
            folderHints = setOf("wii", "nintendo wii"),
        ),
        GameSystem(
            id = "wiiu", name = "Nintendo Wii U", shortName = "Wii U", manufacturer = "Nintendo",
            extensions = setOf("wua", "wud", "wux", "rpx"),
            cores = emptyList(),
            thumbnailDb = "Nintendo - Wii U",
            layout = Layouts.n3ds, color = Color(0xFF009AC7),
            folderHints = setOf("wiiu", "wii u", "nintendo wii u"),
            touchScreen = true,
        ),
        GameSystem(
            id = "switch", name = "Nintendo Switch", shortName = "Switch", manufacturer = "Nintendo",
            extensions = setOf("nsp", "xci", "nca"),
            cores = emptyList(),
            thumbnailDb = "Nintendo - Switch",
            layout = Layouts.n3ds, color = Color(0xFFE60012),
            folderHints = setOf("switch", "nsw", "nintendo switch"),
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
            id = "pokemini", name = "Nintendo Pokémon Mini", shortName = "PokéMini", manufacturer = "Nintendo",
            extensions = setOf("min"),
            cores = listOf(Cores.pokemini),
            thumbnailDb = "Nintendo - Pokemon Mini",
            layout = Layouts.gb, color = Color(0xFFFBC02D),
            folderHints = setOf("pokemini", "pokemon mini", "pokemonmini"),
        ),
        GameSystem(
            id = "gw", name = "Nintendo Game & Watch", shortName = "G&W", manufacturer = "Nintendo",
            extensions = setOf("mgw"),
            cores = listOf(Cores.gw),
            thumbnailDb = "Handheld Electronic Game",
            layout = Layouts.gb, color = Color(0xFFB71C1C),
            folderHints = setOf("gw", "gameandwatch", "game & watch"),
        ),

        // ---- Sega -------------------------------------------------------------------------------
        GameSystem(
            id = "genesis", name = "Sega Genesis / Mega Drive", shortName = "Genesis", manufacturer = "Sega",
            extensions = setOf("md", "gen", "smd", "68k", "sgd", "bin"),
            cores = listOf(Cores.gpgx, Cores.gpgxWide, Cores.picodrive),
            thumbnailDb = "Sega - Mega Drive - Genesis",
            layout = Layouts.genesis, color = Color(0xFF1E88E5),
            folderHints = setOf("genesis", "megadrive", "mega drive", "md"),
        ),
        GameSystem(
            id = "sms", name = "Sega Master System", shortName = "SMS", manufacturer = "Sega",
            extensions = setOf("sms"),
            cores = listOf(Cores.gpgx, Cores.picodrive, Cores.gearsystem),
            thumbnailDb = "Sega - Master System - Mark III",
            layout = Layouts.sms, color = Color(0xFF00897B),
            folderHints = setOf("sms", "mastersystem"),
        ),
        GameSystem(
            id = "gg", name = "Sega Game Gear", shortName = "GG", manufacturer = "Sega",
            extensions = setOf("gg"),
            cores = listOf(Cores.gpgx, Cores.gearsystem),
            thumbnailDb = "Sega - Game Gear",
            layout = Layouts.gg, color = Color(0xFF424242),
            folderHints = setOf("gg", "gamegear"),
        ),
        GameSystem(
            id = "sg1000", name = "Sega SG-1000", shortName = "SG-1000", manufacturer = "Sega",
            extensions = setOf("sg", "sc"),
            cores = listOf(Cores.gpgx, Cores.gearsystem),
            thumbnailDb = "Sega - SG-1000",
            layout = Layouts.sms, color = Color(0xFF00ACC1),
            folderHints = setOf("sg1000", "sg-1000"),
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
            id = "saturn", name = "Sega Saturn", shortName = "Saturn", manufacturer = "Sega",
            extensions = setOf("mds", "ccd", "cue", "chd", "iso"),
            cores = listOf(Cores.yabasanshiro, Cores.ymir, Cores.beetleSaturn),
            thumbnailDb = "Sega - Saturn",
            layout = Layouts.saturn, color = Color(0xFF283593),
            folderHints = setOf("saturn", "sega saturn", "ss"),
            bios = listOf(
                BiosFile("sega_101.bin", "Sega Saturn BIOS (Japan)", false, md5 = "85ec9ca47d8f6807718151cbcca8b964"),
                BiosFile("mpr-17933.bin", "Sega Saturn BIOS (USA/Europe)", false, md5 = "3240872c70984b6cbfda1586cab68dbe"),
            ),
        ),
        GameSystem(
            id = "dreamcast", name = "Sega Dreamcast", shortName = "Dreamcast", manufacturer = "Sega",
            extensions = setOf("cdi", "gdi", "chd", "cue", "m3u"),
            cores = listOf(Cores.flycast, Cores.parallelDc),
            thumbnailDb = "Sega - Dreamcast",
            layout = Layouts.snes, color = Color(0xFFFF6D00),
            folderHints = setOf("dreamcast", "dc", "sega dreamcast"),
            bios = listOf(BiosFile("dc_boot.bin", "Dreamcast BIOS (optional, Flycast includes HLE)", false, md5 = "e10c53c2f8b90bab96ead2d368858623")),
        ),
        GameSystem(
            id = "naomi", name = "Sega NAOMI / Atomiswave", shortName = "NAOMI", manufacturer = "Sega",
            extensions = setOf("lst", "dat", "zip", "chd"),
            cores = listOf(Cores.flycast),
            thumbnailDb = "Sega - Naomi",
            layout = Layouts.arcade, color = Color(0xFFFF8F00),
            folderHints = setOf("naomi", "atomiswave", "sega naomi"),
            bios = listOf(BiosFile("naomi.zip", "Sega NAOMI Arcade BIOS set", false)),
            keepZipped = true,
        ),

        // ---- Sony -------------------------------------------------------------------------------
        GameSystem(
            id = "psx", name = "Sony PlayStation", shortName = "PS1", manufacturer = "Sony",
            extensions = setOf("cue", "chd", "pbp", "m3u", "bin", "img", "iso"),
            cores = listOf(Cores.pcsx, Cores.swanstation, Cores.beetlePsxHw),
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
            id = "psp", name = "Sony PlayStation Portable", shortName = "PSP", manufacturer = "Sony",
            extensions = setOf("cso", "iso", "pbp", "prx"),
            cores = listOf(Cores.ppsspp),
            thumbnailDb = "Sony - PlayStation Portable",
            layout = Layouts.psp, color = Color(0xFF546E7A),
            folderHints = setOf("psp", "playstation portable", "sony psp"),
        ),
        GameSystem(
            id = "ps2", name = "Sony PlayStation 2", shortName = "PS2", manufacturer = "Sony",
            extensions = setOf("iso", "chd", "gz", "bin", "elf"),
            cores = listOf(Cores.lrps2, Cores.playPs2),
            thumbnailDb = "Sony - PlayStation 2",
            layout = Layouts.psx, color = Color(0xFF1565C0),
            folderHints = setOf("ps2", "playstation 2", "playstation2"),
            bios = listOf(
                BiosFile("scph39001.bin", "PS2 BIOS (USA v7.0) — recommended for LRPS2/PCSX2", false, md5 = "d5ce2c7d119f563ce04bc04dbc3a323e"),
                BiosFile("scph70012.bin", "PS2 Slim BIOS (USA)", false),
                BiosFile("scph50004.bin", "PS2 BIOS (Europe)", false),
            ),
        ),
        GameSystem(
            id = "psvita", name = "Sony PlayStation Vita", shortName = "PS Vita", manufacturer = "Sony",
            extensions = setOf("vpk"),
            cores = emptyList(),
            thumbnailDb = "Sony - PlayStation Vita",
            layout = Layouts.psx, color = Color(0xFF003791),
            folderHints = setOf("psvita", "vita", "playstation vita"),
            touchScreen = true,
        ),

        // ---- NEC --------------------------------------------------------------------------------
        GameSystem(
            id = "pce", name = "PC Engine / TurboGrafx-16", shortName = "PCE", manufacturer = "NEC",
            extensions = setOf("pce"),
            cores = listOf(Cores.pce),
            thumbnailDb = "NEC - PC Engine - TurboGrafx 16",
            layout = Layouts.pce, color = Color(0xFFF4511E),
            folderHints = setOf("pce", "pcengine", "tg16", "turbografx"),
        ),
        GameSystem(
            id = "supergrafx", name = "PC Engine SuperGrafx", shortName = "SuperGrafx", manufacturer = "NEC",
            extensions = setOf("sgx"),
            cores = listOf(Cores.supergrafx, Cores.pce),
            thumbnailDb = "NEC - PC Engine SuperGrafx",
            layout = Layouts.pce, color = Color(0xFFD84315),
            folderHints = setOf("supergrafx", "sgx"),
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
            id = "pcfx", name = "NEC PC-FX", shortName = "PC-FX", manufacturer = "NEC",
            extensions = setOf("cue", "chd", "ccd"),
            cores = listOf(Cores.pcfx),
            thumbnailDb = "NEC - PC-FX",
            layout = Layouts.snes, color = Color(0xFF6A1B9A),
            folderHints = setOf("pcfx", "pc-fx", "nec pc-fx"),
            bios = listOf(BiosFile("pcfx.rom", "NEC PC-FX BIOS", true, md5 = "08e36edbea28a017f79f8d4f7ff9b6d7")),
        ),

        // ---- SNK & Bandai -----------------------------------------------------------------------
        GameSystem(
            id = "ngp", name = "Neo Geo Pocket (Color)", shortName = "NGP", manufacturer = "SNK",
            extensions = setOf("ngp", "ngc"),
            cores = listOf(Cores.ngp),
            thumbnailDb = "SNK - Neo Geo Pocket Color",
            layout = Layouts.ngp, color = Color(0xFF6D4C41),
            folderHints = setOf("ngp", "ngpc"),
        ),
        GameSystem(
            id = "neocd", name = "SNK Neo Geo CD", shortName = "Neo CD", manufacturer = "SNK",
            extensions = setOf("cue", "chd"),
            cores = listOf(Cores.neocd, Cores.fbneo),
            thumbnailDb = "SNK - Neo Geo CD",
            layout = Layouts.snes, color = Color(0xFFD84315),
            folderHints = setOf("neocd", "neogeocd", "neo geo cd"),
        ),
        GameSystem(
            id = "ws", name = "WonderSwan (Color)", shortName = "WS", manufacturer = "Bandai",
            extensions = setOf("ws", "wsc"),
            cores = listOf(Cores.wswan),
            thumbnailDb = "Bandai - WonderSwan Color",
            layout = Layouts.wswan, color = Color(0xFF00ACC1),
            folderHints = setOf("ws", "wsc", "wonderswan"),
        ),

        // ---- Atari ------------------------------------------------------------------------------
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
            id = "atari5200", name = "Atari 5200", shortName = "5200", manufacturer = "Atari",
            extensions = setOf("a52", "xfd", "atr", "atx"),
            cores = listOf(Cores.a5200),
            thumbnailDb = "Atari - 5200",
            layout = Layouts.atari7800, color = Color(0xFF795548),
            folderHints = setOf("atari5200", "5200"),
            bios = listOf(BiosFile("5200.rom", "Atari 5200 BIOS (optional)", false, md5 = "281f20ea4320404ec820fb7ec0693b38")),
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
            id = "jaguar", name = "Atari Jaguar", shortName = "Jaguar", manufacturer = "Atari",
            extensions = setOf("j64", "jag"),
            cores = listOf(Cores.virtualjaguar),
            thumbnailDb = "Atari - Jaguar",
            layout = Layouts.genesis, color = Color(0xFFC62828),
            folderHints = setOf("jaguar", "atari jaguar", "atarijaguar"),
        ),

        // ---- Classic Consoles, Microcomputers & Fantasy Consoles --------------------------------
        GameSystem(
            id = "coleco", name = "ColecoVision", shortName = "Coleco", manufacturer = "Coleco",
            extensions = setOf("col", "cv"),
            cores = listOf(Cores.gearcoleco, Cores.bluemsx),
            thumbnailDb = "Coleco - ColecoVision",
            layout = Layouts.atari7800, color = Color(0xFFFB8C00),
            folderHints = setOf("coleco", "colecovision"),
            bios = listOf(BiosFile("colecovision.rom", "ColecoVision BIOS", false, md5 = "2c66f5911e5b42b8ebe113403548eee7")),
        ),
        GameSystem(
            id = "intellivision", name = "Mattel Intellivision", shortName = "Intv", manufacturer = "Mattel",
            extensions = setOf("int", "itv"),
            cores = listOf(Cores.freeintv),
            thumbnailDb = "Mattel - Intellivision",
            layout = Layouts.atari7800, color = Color(0xFF6D4C41),
            folderHints = setOf("intellivision", "intv"),
            bios = listOf(
                BiosFile("exec.bin", "Intellivision Executive BIOS", false, md5 = "62e761035cb657903761800f4437b8af"),
                BiosFile("grom.bin", "Intellivision Graphics ROM", false, md5 = "0cd5946c6473e42e8e4c2137785e427f"),
            ),
        ),
        GameSystem(
            id = "vectrex", name = "GCE Vectrex", shortName = "Vectrex", manufacturer = "GCE",
            extensions = setOf("vec"),
            cores = listOf(Cores.vecx),
            thumbnailDb = "GCE - Vectrex",
            layout = Layouts.genesis, color = Color(0xFF26A69A),
            folderHints = setOf("vectrex", "vecx"),
        ),
        GameSystem(
            id = "odyssey2", name = "Magnavox Odyssey² / Videopac", shortName = "Odyssey2", manufacturer = "Magnavox",
            extensions = setOf("o2"),
            cores = listOf(Cores.o2em),
            thumbnailDb = "Magnavox - Odyssey2",
            layout = Layouts.atari2600, color = Color(0xFF8E24AA),
            folderHints = setOf("odyssey2", "videopac"),
            bios = listOf(BiosFile("o2rom.bin", "Odyssey² / Videopac BIOS", false, md5 = "562d5ebf9e030a40d6fabfc2f33139fd")),
        ),
        GameSystem(
            id = "3do", name = "3DO Interactive Multiplayer", shortName = "3DO", manufacturer = "The 3DO Company",
            extensions = setOf("iso", "chd", "cue"),
            cores = listOf(Cores.opera),
            thumbnailDb = "The 3DO Company - 3DO",
            layout = Layouts.genesis, color = Color(0xFFD81B60),
            folderHints = setOf("3do", "panasonic 3do"),
            bios = listOf(BiosFile("panafz10.bin", "Panasonic FZ-10 3DO BIOS", false, md5 = "51f2f43ae2f3508a14d9f56597e2d3ce")),
        ),
        GameSystem(
            id = "msx", name = "Microsoft MSX / MSX2", shortName = "MSX", manufacturer = "Microsoft",
            extensions = setOf("mx1", "mx2", "dsk", "cas"),
            cores = listOf(Cores.fmsx, Cores.bluemsx),
            thumbnailDb = "Microsoft - MSX",
            layout = Layouts.nes, color = Color(0xFF1976D2),
            folderHints = setOf("msx", "msx1", "msx2"),
        ),
        GameSystem(
            id = "c64", name = "Commodore 64", shortName = "C64", manufacturer = "Commodore",
            extensions = setOf("d64", "t64", "prg", "crt", "tap", "g64"),
            cores = listOf(Cores.viceC64),
            thumbnailDb = "Commodore - 64",
            layout = Layouts.atari2600, color = Color(0xFF5D4037),
            folderHints = setOf("c64", "commodore64", "commodore 64"),
        ),
        GameSystem(
            id = "amiga", name = "Commodore Amiga", shortName = "Amiga", manufacturer = "Commodore",
            extensions = setOf("adf", "ipf", "dms", "hdf", "lha"),
            cores = listOf(Cores.puae),
            thumbnailDb = "Commodore - Amiga",
            layout = Layouts.nes, color = Color(0xFF00838F),
            folderHints = setOf("amiga", "commodore amiga", "puae"),
        ),
        GameSystem(
            id = "dos", name = "MS-DOS (DOSBox Pure)", shortName = "DOS", manufacturer = "IBM / Microsoft",
            extensions = setOf("dosz", "exe", "com", "bat", "conf"),
            cores = listOf(Cores.dosboxPure),
            thumbnailDb = "DOS",
            layout = Layouts.snes, color = Color(0xFF455A64),
            folderHints = setOf("dos", "msdos", "dosbox"),
        ),
        GameSystem(
            id = "scummvm", name = "ScummVM Classic Adventures", shortName = "ScummVM", manufacturer = "LucasArts / Various",
            extensions = setOf("scummvm"),
            cores = listOf(Cores.scummvm),
            thumbnailDb = "ScummVM",
            layout = Layouts.nes, color = Color(0xFF7CB342),
            folderHints = setOf("scummvm"),
            touchScreen = true,
        ),
        GameSystem(
            id = "pico8", name = "PICO-8 Fantasy Console", shortName = "PICO-8", manufacturer = "Lexaloffle",
            extensions = setOf("p8"),
            cores = listOf(Cores.fake08),
            thumbnailDb = "PICO-8",
            layout = Layouts.gb, color = Color(0xFFFF004D),
            folderHints = setOf("pico8", "pico-8"),
        ),
        GameSystem(
            id = "tic80", name = "TIC-80 Tiny Computer", shortName = "TIC-80", manufacturer = "Nesbox",
            extensions = setOf("tic"),
            cores = listOf(Cores.tic80),
            thumbnailDb = "TIC-80",
            layout = Layouts.snes, color = Color(0xFF29ADFF),
            folderHints = setOf("tic80", "tic-80"),
        ),
        GameSystem(
            id = "neogeo", name = "SNK Neo Geo AES / MVS", shortName = "Neo Geo", manufacturer = "SNK",
            extensions = setOf("zip", "7z"),
            cores = listOf(Cores.fbneo, Cores.mame2003),
            thumbnailDb = "SNK - Neo Geo",
            layout = Layouts.arcade, color = Color(0xFFFFB300),
            folderHints = setOf("neogeo", "neo geo", "aes", "mvs"),
            bios = listOf(BiosFile("neogeo.zip", "Neo Geo AES/MVS BIOS set", false)),
            keepZipped = true,
        ),
        GameSystem(
            id = "cdi", name = "Philips CD-i", shortName = "CD-i", manufacturer = "Philips",
            extensions = setOf("chd", "cue", "iso"),
            cores = listOf(Cores.sameCdi),
            thumbnailDb = "Philips - CD-i",
            layout = Layouts.nes, color = Color(0xFF1E88E5),
            folderHints = setOf("cdi", "cd-i", "philips cdi"),
            bios = listOf(BiosFile("cdimono1.zip", "Philips CD-i Mono-I BIOS set", true)),
        ),
        GameSystem(
            id = "supervision", name = "Watara Supervision", shortName = "Supervision", manufacturer = "Watara",
            extensions = setOf("sv"),
            cores = listOf(Cores.potator),
            thumbnailDb = "Watara - Supervision",
            layout = Layouts.gb, color = Color(0xFF546E7A),
            folderHints = setOf("supervision", "watara"),
        ),
        GameSystem(
            id = "channelf", name = "Fairchild Channel F", shortName = "Channel F", manufacturer = "Fairchild",
            extensions = setOf("chf"),
            cores = listOf(Cores.freechaf),
            thumbnailDb = "Fairchild - Channel F",
            layout = Layouts.atari2600, color = Color(0xFF6D4C41),
            folderHints = setOf("channelf", "channel f", "fairchild"),
        ),
        GameSystem(
            id = "zxspectrum", name = "Sinclair ZX Spectrum", shortName = "ZX Spectrum", manufacturer = "Sinclair",
            extensions = setOf("tzx", "tap", "z80", "rzx", "sna"),
            cores = listOf(Cores.fuse),
            thumbnailDb = "Sinclair - ZX Spectrum",
            layout = Layouts.atari2600, color = Color(0xFFE53935),
            folderHints = setOf("zxspectrum", "zx", "spectrum"),
        ),
        GameSystem(
            id = "amstradcpc", name = "Amstrad CPC", shortName = "CPC", manufacturer = "Amstrad",
            extensions = setOf("cdt", "cpr", "voc", "dsk"),
            cores = listOf(Cores.cap32),
            thumbnailDb = "Amstrad - CPC",
            layout = Layouts.nes, color = Color(0xFF43A047),
            folderHints = setOf("amstradcpc", "amstrad", "cpc"),
        ),
        GameSystem(
            id = "atarist", name = "Atari ST", shortName = "Atari ST", manufacturer = "Atari",
            extensions = setOf("st", "msa", "stx", "dim"),
            cores = listOf(Cores.hatari),
            thumbnailDb = "Atari - ST",
            layout = Layouts.nes, color = Color(0xFF78909C),
            folderHints = setOf("atarist", "atari st"),
        ),
        GameSystem(
            id = "x68000", name = "Sharp X68000", shortName = "X68000", manufacturer = "Sharp",
            extensions = setOf("hdm", "2hd", "xdf", "dim", "hdf"),
            cores = listOf(Cores.px68k),
            thumbnailDb = "Sharp - X68000",
            layout = Layouts.nes, color = Color(0xFF37474F),
            folderHints = setOf("x68000", "x68k", "sharp x68000"),
        ),
        GameSystem(
            id = "pc98", name = "NEC PC-98", shortName = "PC-98", manufacturer = "NEC",
            extensions = setOf("fdi", "hdi", "nhd", "d88", "d98", "thd"),
            cores = listOf(Cores.np2kai),
            thumbnailDb = "NEC - PC-98",
            layout = Layouts.nes, color = Color(0xFF0277BD),
            folderHints = setOf("pc98", "pc-98", "nec pc-98"),
        ),
        GameSystem(
            id = "j2me", name = "Java ME (J2ME Mobile)", shortName = "J2ME", manufacturer = "Sun / Oracle",
            extensions = setOf("jar"),
            cores = listOf(Cores.squirreljme),
            thumbnailDb = "Java - J2ME",
            layout = Layouts.snes, color = Color(0xFFEF6C00),
            folderHints = setOf("j2me", "java", "javame"),
            touchScreen = true,
        ),
        GameSystem(
            id = "herobox16", name = "HeroBox-16 Original Console", shortName = "HB-16", manufacturer = "RetroHero",
            extensions = setOf("hbx"),
            cores = listOf(Cores.herobox16),
            thumbnailDb = "PICO-8",
            layout = Layouts.snes, color = Color(0xFFD0BCFF),
            folderHints = setOf("herobox16", "herobox", "hb16"),
        ),
        GameSystem(
            id = "chip8", name = "CHIP-8 / SUPER-CHIP", shortName = "CHIP-8", manufacturer = "COSMAC / RetroHero",
            extensions = setOf("ch8", "sc8", "c8"),
            cores = listOf(Cores.retroheroChip8),
            thumbnailDb = "CHIP-8",
            layout = Layouts.snes, color = Color(0xFF9CD67D),
            folderHints = setOf("chip8", "chip-8", "schip", "superchip"),
        ),
        GameSystem(
            id = "arcade", name = "Arcade", shortName = "Arcade", manufacturer = "Various",
            extensions = setOf("zip", "7z"),
            cores = listOf(Cores.fbneo, Cores.mame2003, Cores.mame2016),
            thumbnailDb = "FBNeo - Arcade Games",
            layout = Layouts.arcade, color = Color(0xFFFFD600),
            folderHints = setOf("arcade", "mame", "fbneo", "fba", "cps1", "cps2", "cps3"),
            bios = listOf(BiosFile("neogeo.zip", "Neo Geo BIOS set (for Neo Geo games)", false)),
            keepZipped = true,
        ),
    )

    private val byId = all.associateBy { it.id }
    private val coresById = all.flatMap { it.cores }.distinctBy { it.id }.associateBy { it.id }

    fun get(id: String): GameSystem? = byId[id]

    fun coreById(id: String): CoreInfo? = coresById[id]

    fun allCores(): List<Pair<CoreInfo, List<GameSystem>>> =
        all.flatMap { s -> s.cores.map { it to s } }
            .groupBy({ it.first.id }, { it.second })
            .map { (id, systems) -> coresById[id]!! to systems }
}
