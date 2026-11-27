package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color
import com.retrohero.app.data.Systems

enum class SpeakerStyle { None, Slotted, DotMatrix, TwinDots }

/** Physical bezel silhouette matching the real console's hardware form factor. */
enum class BezelFormFactor {
    /** Classic Game Boy DMG / GBC lens with asymmetric curved bottom-right corner. */
    GameBoyChamfer,
    /** Symmetrical wide handheld lens (GBA, PSP, PS Vita, Game Gear, Lynx, NGP, WonderSwan). */
    HandheldWide,
    /** Dual-screen clamshell bezel with hinge accents (Nintendo DS, Nintendo 3DS). */
    Clamshell,
    /** Living-room home console / CRT display frame (NES, SNES, N64, GC, Wii, PS1, PS2, Sega, Atari). */
    HomeConsole,
    /** Classic home computer / vector / fantasy console monitor bezel (C64, Amiga, MSX, DOS, PICO-8). */
    ComputerMonitor,
    /** Arcade cabinet monitor bezel with marquee header stripes (Arcade, Neo Geo, NAOMI). */
    ArcadeCabinet,
}

/**
 * Complete physical hardware appearance for a console-authentic display & controller shell:
 * seamless body housing, screen display lens bezel, power LED, stamped console wordmark & subtitle,
 * form-factor geometry, D-pad, face buttons, rubber Select/Start pills, shoulder triggers, and speaker grille.
 */
data class ConsoleStyle(
    val plateTop: Color,
    val plateBottom: Color,
    /** Recessed glass bezel surrounding the live game display. */
    val bezel: Color = Color(0xFF18191E),
    val bezelBorder: Color = Color(0xFF2D2F38),
    val bezelWordmark: String = "RETROHERO",
    val bezelWordmarkColor: Color = Color(0xFFB5B8C4),
    /** Optional top accent pinstripes inside the bezel (2-stripe or 3-stripe hardware branding). */
    val bezelStripeA: Color? = null,
    val bezelStripeB: Color? = null,
    val bezelStripeC: Color? = null,
    val bezelSubtitle: String = "",
    val formFactor: BezelFormFactor = BezelFormFactor.HomeConsole,
    /** Subtle horizontal walnut woodgrain finish for classic 1970s/80s consoles (Atari 2600, Intellivision, Channel F). */
    val woodgrain: Boolean = false,
    val ledColor: Color = Color(0xFF39D353),
    val speaker: SpeakerStyle = SpeakerStyle.DotMatrix,
    /** NES-style dark inset controller panel the buttons sit on. */
    val band: Color? = null,
    val dpad: Color,
    val arrow: Color,
    val face: (String) -> Color,
    /** Letter colour on the button; null = letters are printed below it in [labelBelow]. */
    val faceLabel: (String) -> Color? = { Color.White },
    val labelBelow: Color? = null,
    val pill: Color,
    val pillLabel: Color,
    /** Game Boy and SNES Select/Start pills sit at an angle. */
    val pillTilt: Float = 0f,
    val shoulder: Color = Color(0xFF9C9CA4),
    val shoulderLabel: Color = Color(0xFF2E2E34),
    val iconFill: Color,
    val iconTint: Color,
)

object ConsoleStyles {

    // ---- Nintendo -------------------------------------------------------------------------------

    private val nes = ConsoleStyle(
        plateTop = Color(0xFFDAD9D4), plateBottom = Color(0xFFB7B6B0),
        bezel = Color(0xFF19191C), bezelBorder = Color(0xFF34343A),
        bezelWordmark = "NINTENDO NES", bezelWordmarkColor = Color(0xFFE03344),
        bezelStripeA = Color(0xFFD0203C), bezelStripeB = Color(0xFF424248),
        bezelSubtitle = "8-BIT ENTERTAINMENT SYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        band = Color(0xFF1D1D20),
        dpad = Color(0xFF252528), arrow = Color(0xFF727278),
        face = { Color(0xFFD0203C) },
        faceLabel = { null }, labelBelow = Color(0xFFD0203C),
        pill = Color(0xFF3A3A3E), pillLabel = Color(0xFFD0203C),
        iconFill = Color(0xFF3A3A3E), iconTint = Color(0xFFE0DFDA),
    )

    private val snes = ConsoleStyle(
        plateTop = Color(0xFFD9D8DF), plateBottom = Color(0xFFB7B6C0),
        bezel = Color(0xFF202027), bezelBorder = Color(0xFF3B3B46),
        bezelWordmark = "SUPER NINTENDO", bezelWordmarkColor = Color(0xFFC8C7D4),
        bezelStripeA = Color(0xFF6D5ACF), bezelStripeB = Color(0xFF9C90E4),
        bezelSubtitle = "16-BIT ENTERTAINMENT SYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF2E2E34), arrow = Color(0xFF72727A),
        face = {
            when (it) {
                "A" -> Color(0xFFD4262F)
                "B" -> Color(0xFFF0BF1A)
                "X" -> Color(0xFF2C52D6)
                "Y" -> Color(0xFF1F9C50)
                else -> Color(0xFF6A6A72)
            }
        },
        pill = Color(0xFF6A6A72), pillLabel = Color(0xFF433F8C), pillTilt = -25f,
        shoulder = Color(0xFFACACB6), shoulderLabel = Color(0xFF45454E),
        iconFill = Color(0xFF8C8C96), iconTint = Color(0xFF24242A),
    )

    private val n64 = ConsoleStyle(
        plateTop = Color(0xFF34353E), plateBottom = Color(0xFF1F2026),
        bezel = Color(0xFF131418), bezelBorder = Color(0xFF2D2E38),
        bezelWordmark = "NINTENDO 64", bezelWordmarkColor = Color(0xFFCFD1DC),
        bezelStripeA = Color(0xFF22A055), bezelStripeB = Color(0xFFE63946), bezelStripeC = Color(0xFF1E6CE0),
        bezelSubtitle = "64-BIT REALITY CO-PROCESSOR",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF464854), arrow = Color(0xFF9497A6),
        face = {
            when (it) {
                "A" -> Color(0xFF1E6CE0)
                "B" -> Color(0xFF1BA351)
                "X", "Y", "L2", "R2" -> Color(0xFFF2BE22)
                else -> Color(0xFF464854)
            }
        },
        faceLabel = { if (it in setOf("X", "Y", "L2", "R2")) Color(0xFF222226) else Color.White },
        pill = Color(0xFFD92B3A), pillLabel = Color(0xFFE4E5EE),
        shoulder = Color(0xFF4A4C58), shoulderLabel = Color(0xFFE4E5EE),
        iconFill = Color(0xFF292A32), iconTint = Color(0xFFE4E5EE),
    )

    private val gameBoy = ConsoleStyle(
        plateTop = Color(0xFFE0DDD4), plateBottom = Color(0xFFC2BEB3),
        bezel = Color(0xFF5A5D67), bezelBorder = Color(0xFF43464F),
        bezelWordmark = "GAME BOY", bezelWordmarkColor = Color(0xFF25317C),
        bezelStripeA = Color(0xFF8B1E4F), bezelStripeB = Color(0xFF25317C),
        bezelSubtitle = "DOT MATRIX WITH STEREO SOUND",
        formFactor = BezelFormFactor.GameBoyChamfer,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF26262B), arrow = Color(0xFF6B6B74),
        face = { Color(0xFF9E1F54) },
        faceLabel = { null }, labelBelow = Color(0xFF25317C),
        pill = Color(0xFF8C8C94), pillLabel = Color(0xFF25317C), pillTilt = -25f,
        iconFill = Color(0xFF8C8C94), iconTint = Color(0xFF26262B),
    )

    private val gbc = ConsoleStyle(
        plateTop = Color(0xFF6A58B8), plateBottom = Color(0xFF483892),
        bezel = Color(0xFF1B1B22), bezelBorder = Color(0xFF30303B),
        bezelWordmark = "GAME BOY COLOR", bezelWordmarkColor = Color(0xFFD4D2E8),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFF22C55E), bezelStripeC = Color(0xFFFACC15),
        bezelSubtitle = "COLOR LCD · 8-BIT HANDHELD",
        formFactor = BezelFormFactor.GameBoyChamfer,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF222228), arrow = Color(0xFF6B6B76),
        face = { Color(0xFF25252C) },
        faceLabel = { Color(0xFFD8D6EC) },
        pill = Color(0xFF26262E), pillLabel = Color(0xFFC5C0E6), pillTilt = -22f,
        iconFill = Color(0xFF2C2942), iconTint = Color(0xFFE4E2F5),
    )

    private val gba = ConsoleStyle(
        plateTop = Color(0xFF5D52B2), plateBottom = Color(0xFF3D3388),
        bezel = Color(0xFF1D1D24), bezelBorder = Color(0xFF38374A),
        bezelWordmark = "GAME BOY ADVANCE", bezelWordmarkColor = Color(0xFFC5C2E2),
        bezelStripeA = Color(0xFF7D72D6), bezelStripeB = Color(0xFF493FA2),
        bezelSubtitle = "32-BIT PORTABLE SYSTEM",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF39D353), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF25242E), arrow = Color(0xFF6E6B84),
        face = { Color(0xFFDFDFE8) },
        faceLabel = { Color(0xFF3D3388) },
        pill = Color(0xFF26242E), pillLabel = Color(0xFFD7D4F2),
        shoulder = Color(0xFFC4C2D6), shoulderLabel = Color(0xFF3D3388),
        iconFill = Color(0xFF26242E), iconTint = Color(0xFFD7D4F2),
    )

    private val nds = ConsoleStyle(
        plateTop = Color(0xFF2C2D33), plateBottom = Color(0xFF1B1C20),
        bezel = Color(0xFF101114), bezelBorder = Color(0xFF2E3038),
        bezelWordmark = "NINTENDO DS", bezelWordmarkColor = Color(0xFFC8CBD6),
        bezelStripeA = Color(0xFF64748B), bezelStripeB = Color(0xFF334155),
        bezelSubtitle = "DUAL SCREEN TOUCH SYSTEM",
        formFactor = BezelFormFactor.Clamshell,
        ledColor = Color(0xFF34D399), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF3E404A), arrow = Color(0xFF8C909E),
        face = { Color(0xFF3E404A) },
        faceLabel = { Color(0xFFF0F1F6) },
        pill = Color(0xFF3A3C45), pillLabel = Color(0xFFA8ACB8),
        shoulder = Color(0xFF464852), shoulderLabel = Color(0xFFE6E8F0),
        iconFill = Color(0xFF34363E), iconTint = Color(0xFFE6E8F0),
    )

    private val n3ds = ConsoleStyle(
        plateTop = Color(0xFF1B647D), plateBottom = Color(0xFF0E3A4D),
        bezel = Color(0xFF0D0F14), bezelBorder = Color(0xFF244C5E),
        bezelWordmark = "NINTENDO 3DS", bezelWordmarkColor = Color(0xFFD8F3FF),
        bezelStripeA = Color(0xFF00C2FF), bezelStripeB = Color(0xFFE53935),
        bezelSubtitle = "STEREOSCOPIC 3D SYSTEM",
        formFactor = BezelFormFactor.Clamshell,
        ledColor = Color(0xFF00D4FF), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF243440), arrow = Color(0xFF8AC4DC),
        face = { Color(0xFF243440) },
        faceLabel = { Color(0xFFE6F7FF) },
        pill = Color(0xFF1C2C38), pillLabel = Color(0xFFAAD8EC),
        shoulder = Color(0xFF2A4A5C), shoulderLabel = Color(0xFFE6F7FF),
        iconFill = Color(0xFF182A36), iconTint = Color(0xFFD8F3FF),
    )

    private val gameCube = ConsoleStyle(
        plateTop = Color(0xFF5A4BA8), plateBottom = Color(0xFF3B2F7E),
        bezel = Color(0xFF16161E), bezelBorder = Color(0xFF3B3560),
        bezelWordmark = "NINTENDO GAMECUBE", bezelWordmarkColor = Color(0xFFDCD8F8),
        bezelStripeA = Color(0xFF7B6CE0), bezelStripeB = Color(0xFFFF7A1A),
        bezelSubtitle = "128-BIT DOLPHIN",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF7A1A), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF9EA0AC), arrow = Color(0xFF42434C),
        face = {
            when (it) {
                "A" -> Color(0xFF1DB36E)
                "B" -> Color(0xFFE02F3C)
                "X", "Y" -> Color(0xFFA8AAB6)
                else -> Color(0xFF6B5CC2)
            }
        },
        faceLabel = { if (it in setOf("X", "Y")) Color(0xFF24242C) else Color.White },
        pill = Color(0xFFA8AAB6), pillLabel = Color(0xFF24242C),
        shoulder = Color(0xFF9A9CA8), shoulderLabel = Color(0xFF24242C),
        iconFill = Color(0xFF2A2448), iconTint = Color(0xFFE4E0FA),
    )

    private val wii = ConsoleStyle(
        plateTop = Color(0xFFF3F4F8), plateBottom = Color(0xFFDADCE5),
        bezel = Color(0xFF181A20), bezelBorder = Color(0xFFB6BCCB),
        bezelWordmark = "NINTENDO WII", bezelWordmarkColor = Color(0xFFE8F6FF),
        bezelStripeA = Color(0xFF00B4FF), bezelStripeB = Color(0xFF7EDBFF),
        bezelSubtitle = "MOTION INTERACTIVE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF00B8FF), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFFE2E4EC), arrow = Color(0xFF6E7382),
        face = {
            when (it) {
                "A" -> Color(0xFF00A6ED)
                else -> Color(0xFFECEEF4)
            }
        },
        faceLabel = { if (it == "A") Color.White else Color(0xFF2C303C) },
        pill = Color(0xFFD5D8E2), pillLabel = Color(0xFF3A4050),
        shoulder = Color(0xFFCFD3DE), shoulderLabel = Color(0xFF2C303C),
        iconFill = Color(0xFFD5D8E2), iconTint = Color(0xFF242834),
    )

    private val wiiU = ConsoleStyle(
        plateTop = Color(0xFF23262F), plateBottom = Color(0xFF12141A),
        bezel = Color(0xFF0B0C10), bezelBorder = Color(0xFF2B3544),
        bezelWordmark = "WII U GAMEPAD", bezelWordmarkColor = Color(0xFFD2EFFF),
        bezelStripeA = Color(0xFF009AC7), bezelStripeB = Color(0xFF38BDF8),
        bezelSubtitle = "HD DUAL-SCREEN SYSTEM",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF00B4F0), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF323642), arrow = Color(0xFF8C92A4),
        face = { Color(0xFF2D313D) },
        faceLabel = { Color(0xFFE6F4FA) },
        pill = Color(0xFF2A2E3A), pillLabel = Color(0xFF9AC8DC),
        shoulder = Color(0xFF353946), shoulderLabel = Color(0xFFE6F4FA),
        iconFill = Color(0xFF282C38), iconTint = Color(0xFFE6F4FA),
    )

    private val nintendoSwitch = ConsoleStyle(
        plateTop = Color(0xFF26282F), plateBottom = Color(0xFF15161B),
        bezel = Color(0xFF0C0D10), bezelBorder = Color(0xFF32353E),
        bezelWordmark = "NINTENDO SWITCH", bezelWordmarkColor = Color(0xFFEAECEF),
        bezelStripeA = Color(0xFF00C3E3), bezelStripeB = Color(0xFFFF4554),
        bezelSubtitle = "HYBRID CONSOLE · JOY-CON",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF38E86C), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF1E2026), arrow = Color(0xFF9094A0),
        face = { Color(0xFF1E2026) },
        faceLabel = { Color(0xFFF2F3F7) },
        pill = Color(0xFF32353E), pillLabel = Color(0xFFD2D5DE),
        shoulder = Color(0xFF2E313A), shoulderLabel = Color(0xFFF2F3F7),
        iconFill = Color(0xFF2A2D36), iconTint = Color(0xFFF2F3F7),
    )

    private val virtualBoy = ConsoleStyle(
        plateTop = Color(0xFFB81B26), plateBottom = Color(0xFF740E16),
        bezel = Color(0xFF100A0C), bezelBorder = Color(0xFF421418),
        bezelWordmark = "VIRTUAL BOY", bezelWordmarkColor = Color(0xFFFF4040),
        bezelStripeA = Color(0xFFFF1A1A), bezelStripeB = Color(0xFF541014),
        bezelSubtitle = "32-BIT 3D RED LED",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF1A1A), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF201D20), arrow = Color(0xFF964046),
        face = { Color(0xFF1F1C1E) },
        faceLabel = { Color(0xFFFF5252) },
        pill = Color(0xFF282224), pillLabel = Color(0xFFFF6666),
        shoulder = Color(0xFF2C2426), shoulderLabel = Color(0xFFFF6666),
        iconFill = Color(0xFF2B1C1E), iconTint = Color(0xFFFFB2B2),
    )

    private val pokeMini = ConsoleStyle(
        plateTop = Color(0xFF46A2E0), plateBottom = Color(0xFF2674AE),
        bezel = Color(0xFF1A222B), bezelBorder = Color(0xFF38526B),
        bezelWordmark = "POKÉMON MINI", bezelWordmarkColor = Color(0xFFFFDF4A),
        bezelStripeA = Color(0xFFFACC15), bezelStripeB = Color(0xFFEF4444),
        bezelSubtitle = "POCKET CARTRIDGE SYSTEM",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFFFACC15), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF24303C), arrow = Color(0xFF96B8D4),
        face = {
            when (it) {
                "A" -> Color(0xFFFACC15)
                "B" -> Color(0xFFEF4444)
                else -> Color(0xFF263442)
            }
        },
        faceLabel = { if (it == "A") Color(0xFF20242A) else Color.White },
        pill = Color(0xFF1E3A52), pillLabel = Color(0xFFFFE57F),
        shoulder = Color(0xFF2C5A82), shoulderLabel = Color(0xFFFFF4C2),
        iconFill = Color(0xFF1E3A52), iconTint = Color(0xFFEAF6FF),
    )

    private val gameAndWatch = ConsoleStyle(
        plateTop = Color(0xFFC9A34E), plateBottom = Color(0xFF947028),
        bezel = Color(0xFF5E1720), bezelBorder = Color(0xFFDFC178),
        bezelWordmark = "GAME & WATCH", bezelWordmarkColor = Color(0xFFF4DC9E),
        bezelStripeA = Color(0xFFDFC178), bezelStripeB = Color(0xFF8C2430),
        bezelSubtitle = "WIDE SCREEN LCD",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFFFF4D4D), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF262220), arrow = Color(0xFFC2A466),
        face = { Color(0xFFB71C28) },
        faceLabel = { Color(0xFFFFEBC2) },
        pill = Color(0xFFD6BE84), pillLabel = Color(0xFF421A1A),
        shoulder = Color(0xFFB59244), shoulderLabel = Color(0xFF2E1E10),
        iconFill = Color(0xFF6B1E28), iconTint = Color(0xFFF8E5B4),
    )

    // ---- Sega -----------------------------------------------------------------------------------

    private val genesis = ConsoleStyle(
        plateTop = Color(0xFF26262B), plateBottom = Color(0xFF121216),
        bezel = Color(0xFF0D0D10), bezelBorder = Color(0xFF2C2C34),
        bezelWordmark = "SEGA GENESIS", bezelWordmarkColor = Color(0xFFD0D2DC),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFF1E88E5),
        bezelSubtitle = "16-BIT HIGH DEFINITION",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF3B3B40), arrow = Color(0xFF92929A),
        face = { Color(0xFF1C1C20) },
        faceLabel = { Color(0xFFD4D4DC) },
        pill = Color(0xFF3B3B40), pillLabel = Color(0xFFD4D4DC),
        shoulder = Color(0xFF3B3B40), shoulderLabel = Color(0xFFD4D4DC),
        iconFill = Color(0xFF35353B), iconTint = Color(0xFFDCDCE4),
    )

    private val segaCd = ConsoleStyle(
        plateTop = Color(0xFF282A32), plateBottom = Color(0xFF14151B),
        bezel = Color(0xFF0E0F14), bezelBorder = Color(0xFF4D5262),
        bezelWordmark = "SEGA CD", bezelWordmarkColor = Color(0xFFDCE0EC),
        bezelStripeA = Color(0xFF22C55E), bezelStripeB = Color(0xFFE53935),
        bezelSubtitle = "MEGA-CD · 16-BIT OPTICAL",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF3A3D48), arrow = Color(0xFF9499A8),
        face = { Color(0xFF1D1F26) },
        faceLabel = { Color(0xFFDCE0EC) },
        pill = Color(0xFF3A3D48), pillLabel = Color(0xFFDCE0EC),
        shoulder = Color(0xFF3A3D48), shoulderLabel = Color(0xFFDCE0EC),
        iconFill = Color(0xFF323540), iconTint = Color(0xFFE2E6F2),
    )

    private val sega32x = ConsoleStyle(
        plateTop = Color(0xFF242428), plateBottom = Color(0xFF111114),
        bezel = Color(0xFF0D0D10), bezelBorder = Color(0xFF4A3D20),
        bezelWordmark = "SEGA 32X", bezelWordmarkColor = Color(0xFFF6D068),
        bezelStripeA = Color(0xFFF5B82E), bezelStripeB = Color(0xFFE53935),
        bezelSubtitle = "SUPER 32X · 32-BIT ADD-ON",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFF5B82E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF38383E), arrow = Color(0xFF9696A0),
        face = { Color(0xFF1E1E22) },
        faceLabel = { Color(0xFFF6D068) },
        pill = Color(0xFF38383E), pillLabel = Color(0xFFF6D068),
        shoulder = Color(0xFF38383E), shoulderLabel = Color(0xFFF6D068),
        iconFill = Color(0xFF323238), iconTint = Color(0xFFF2E2B6),
    )

    private val masterSystem = ConsoleStyle(
        plateTop = Color(0xFF232429), plateBottom = Color(0xFF121317),
        bezel = Color(0xFF0D0E12), bezelBorder = Color(0xFF422226),
        bezelWordmark = "SEGA MASTER SYSTEM", bezelWordmarkColor = Color(0xFFF2F2F5),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFFD4D6DE),
        bezelSubtitle = "8-BIT POWER BASE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF34363E), arrow = Color(0xFF9698A4),
        face = { Color(0xFFD62828) },
        faceLabel = { Color.White },
        pill = Color(0xFF34363E), pillLabel = Color(0xFFE4E6EE),
        shoulder = Color(0xFF34363E), shoulderLabel = Color(0xFFE4E6EE),
        iconFill = Color(0xFF2E3038), iconTint = Color(0xFFEAECEF),
    )

    private val gameGear = ConsoleStyle(
        plateTop = Color(0xFF30333C), plateBottom = Color(0xFF1B1D24),
        bezel = Color(0xFF111318), bezelBorder = Color(0xFF3A3E4A),
        bezelWordmark = "SEGA GAME GEAR", bezelWordmarkColor = Color(0xFFE0E2EC),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFF22C55E), bezelStripeC = Color(0xFF2563EB),
        bezelSubtitle = "8-BIT COLOR HANDHELD",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF20222A), arrow = Color(0xFF828798),
        face = { Color(0xFF1C1E26) },
        faceLabel = { Color(0xFFDCE0EC) },
        pill = Color(0xFF2563EB), pillLabel = Color.White,
        shoulder = Color(0xFF383C48), shoulderLabel = Color(0xFFE0E2EC),
        iconFill = Color(0xFF282B34), iconTint = Color(0xFFE0E2EC),
    )

    private val sg1000 = ConsoleStyle(
        plateTop = Color(0xFFE6E1D5), plateBottom = Color(0xFFCBC4B4),
        bezel = Color(0xFF1A1918), bezelBorder = Color(0xFF423E38),
        bezelWordmark = "SEGA SG-1000", bezelWordmarkColor = Color(0xFFF0EBE0),
        bezelStripeA = Color(0xFFD62828), bezelStripeB = Color(0xFF1D4ED8),
        bezelSubtitle = "8-BIT HOME CONSOLE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFE53935), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2A2926), arrow = Color(0xFF8A867C),
        face = { Color(0xFFD62828) },
        faceLabel = { Color.White },
        pill = Color(0xFF3A3834), pillLabel = Color(0xFFF0EBE0),
        shoulder = Color(0xFF8C877C), shoulderLabel = Color(0xFF1E1D1B),
        iconFill = Color(0xFF3A3834), iconTint = Color(0xFFF0EBE0),
    )

    private val saturn = ConsoleStyle(
        plateTop = Color(0xFF252834), plateBottom = Color(0xFF13151D),
        bezel = Color(0xFF0D0F16), bezelBorder = Color(0xFF323E58),
        bezelWordmark = "SEGA SATURN", bezelWordmarkColor = Color(0xFFD6E2FA),
        bezelStripeA = Color(0xFF2563EB), bezelStripeB = Color(0xFF94A3B8),
        bezelSubtitle = "32-BIT DUAL-CPU",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF363B4C), arrow = Color(0xFF96A0B8),
        face = {
            when (it) {
                "A", "B", "C" -> Color(0xFF1F2430)
                else -> Color(0xFF2C3448)
            }
        },
        faceLabel = { Color(0xFFBFD4FA) },
        pill = Color(0xFF2563EB), pillLabel = Color.White,
        shoulder = Color(0xFF32384A), shoulderLabel = Color(0xFFD6E2FA),
        iconFill = Color(0xFF2B3040), iconTint = Color(0xFFD6E2FA),
    )

    private val dreamcast = ConsoleStyle(
        plateTop = Color(0xFFEEEFF4), plateBottom = Color(0xFFD2D5DF),
        bezel = Color(0xFF1A1C22), bezelBorder = Color(0xFF9EA4B5),
        bezelWordmark = "SEGA DREAMCAST", bezelWordmarkColor = Color(0xFFFF8533),
        bezelStripeA = Color(0xFFFF6B1A), bezelStripeB = Color(0xFF1E88E5),
        bezelSubtitle = "128-BIT · IT'S THINKING",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF6B1A), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFFBDC0CC), arrow = Color(0xFF5A5E6C),
        face = {
            when (it) {
                "A" -> Color(0xFFE53935)
                "B" -> Color(0xFF1E88E5)
                "X" -> Color(0xFFF5B82E)
                "Y" -> Color(0xFF22A055)
                else -> Color(0xFFB5B8C4)
            }
        },
        faceLabel = { if (it == "X") Color(0xFF222226) else Color.White },
        pill = Color(0xFFFF6B1A), pillLabel = Color.White,
        shoulder = Color(0xFFC2C6D2), shoulderLabel = Color(0xFF2E313C),
        iconFill = Color(0xFFC8CCD8), iconTint = Color(0xFF282B36),
    )

    private val naomi = ConsoleStyle(
        plateTop = Color(0xFFE8E9EE), plateBottom = Color(0xFFCACCD6),
        bezel = Color(0xFF16171C), bezelBorder = Color(0xFF525462),
        bezelWordmark = "SEGA NAOMI", bezelWordmarkColor = Color(0xFFFFA726),
        bezelStripeA = Color(0xFFFF8F00), bezelStripeB = Color(0xFF1E88E5),
        bezelSubtitle = "ARCADE SYSTEM BOARD",
        formFactor = BezelFormFactor.ArcadeCabinet,
        ledColor = Color(0xFFFF9800), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2D303A), arrow = Color(0xFF8C90A0),
        face = {
            when (it) {
                "A", "Y" -> Color(0xFFFF8F00)
                "B", "X" -> Color(0xFF1E88E5)
                else -> Color(0xFF343844)
            }
        },
        pill = Color(0xFF2D303A), pillLabel = Color(0xFFFFCA7A),
        shoulder = Color(0xFFB8BCC8), shoulderLabel = Color(0xFF24262E),
        iconFill = Color(0xFFC2C5D0), iconTint = Color(0xFF22242C),
    )

    // ---- Sony -----------------------------------------------------------------------------------

    private val playStation = ConsoleStyle(
        plateTop = Color(0xFFCECFD4), plateBottom = Color(0xFFAAAAB2),
        bezel = Color(0xFF1C1D22), bezelBorder = Color(0xFF383A44),
        bezelWordmark = "PLAYSTATION", bezelWordmarkColor = Color(0xFFD4D6E0),
        bezelStripeA = Color(0xFF34C08A), bezelStripeB = Color(0xFFEA4B51), bezelStripeC = Color(0xFF6F97EE),
        bezelSubtitle = "32-BIT CD-ROM SYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF39D353), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF9E9FA6), arrow = Color(0xFF4F5058),
        face = { Color(0xFF36363C) },
        faceLabel = {
            when (it) {
                "△" -> Color(0xFF34C08A)
                "○" -> Color(0xFFEA4B51)
                "✕" -> Color(0xFF6F97EE)
                "□" -> Color(0xFFE584BF)
                else -> Color.White
            }
        },
        pill = Color(0xFF3A3A40), pillLabel = Color(0xFF46464E),
        shoulder = Color(0xFF9899A0), shoulderLabel = Color(0xFF2E2E34),
        iconFill = Color(0xFF8E8F96), iconTint = Color(0xFF26262B),
    )

    private val playStation2 = ConsoleStyle(
        plateTop = Color(0xFF1A1D27), plateBottom = Color(0xFF0C0E15),
        bezel = Color(0xFF08090E), bezelBorder = Color(0xFF1E4C9A),
        bezelWordmark = "PLAYSTATION 2", bezelWordmarkColor = Color(0xFF5FA8FF),
        bezelStripeA = Color(0xFF1E78FF), bezelStripeB = Color(0xFF0B3B91),
        bezelSubtitle = "EMOTION ENGINE · 128-BIT",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF1E90FF), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF282C3A), arrow = Color(0xFF7D89A8),
        face = { Color(0xFF232734) },
        faceLabel = {
            when (it) {
                "△" -> Color(0xFF34C08A)
                "○" -> Color(0xFFEA4B51)
                "✕" -> Color(0xFF6F97EE)
                "□" -> Color(0xFFE584BF)
                else -> Color.White
            }
        },
        pill = Color(0xFF262A38), pillLabel = Color(0xFF92B8F8),
        shoulder = Color(0xFF2B3040), shoulderLabel = Color(0xFFD8E4FA),
        iconFill = Color(0xFF232734), iconTint = Color(0xFFD8E4FA),
    )

    private val psp = ConsoleStyle(
        plateTop = Color(0xFF1D1E22), plateBottom = Color(0xFF0F1013),
        bezel = Color(0xFF0A0B0D), bezelBorder = Color(0xFF545764),
        bezelWordmark = "PSP", bezelWordmarkColor = Color(0xFFDADCE4),
        bezelStripeA = Color(0xFF94A3B8), bezelStripeB = Color(0xFF475569),
        bezelSubtitle = "PLAYSTATION PORTABLE",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF39D353), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF2E3038), arrow = Color(0xFF8E919E),
        face = { Color(0xFF282A31) },
        faceLabel = {
            when (it) {
                "△" -> Color(0xFF34C08A)
                "○" -> Color(0xFFEA4B51)
                "✕" -> Color(0xFF6F97EE)
                "□" -> Color(0xFFE584BF)
                else -> Color.White
            }
        },
        pill = Color(0xFF2D2F36), pillLabel = Color(0xFFB4B7C2),
        shoulder = Color(0xFF3A3C46), shoulderLabel = Color(0xFFE6E8F0),
        iconFill = Color(0xFF282A31), iconTint = Color(0xFFE6E8F0),
    )

    private val psVita = ConsoleStyle(
        plateTop = Color(0xFF181B24), plateBottom = Color(0xFF0B0D14),
        bezel = Color(0xFF080A0F), bezelBorder = Color(0xFF485A78),
        bezelWordmark = "PS VITA", bezelWordmarkColor = Color(0xFFD6E8FF),
        bezelStripeA = Color(0xFF0070D1), bezelStripeB = Color(0xFF60A5FA),
        bezelSubtitle = "OLED TOUCH SYSTEM",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF0088FF), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF262B38), arrow = Color(0xFF8694B2),
        face = { Color(0xFF222734) },
        faceLabel = {
            when (it) {
                "△" -> Color(0xFF34C08A)
                "○" -> Color(0xFFEA4B51)
                "✕" -> Color(0xFF6F97EE)
                "□" -> Color(0xFFE584BF)
                else -> Color.White
            }
        },
        pill = Color(0xFF262B38), pillLabel = Color(0xFFA8C4F0),
        shoulder = Color(0xFF343C50), shoulderLabel = Color(0xFFE2EEFF),
        iconFill = Color(0xFF222734), iconTint = Color(0xFFE2EEFF),
    )

    // ---- NEC ------------------------------------------------------------------------------------

    private val pcEngine = ConsoleStyle(
        plateTop = Color(0xFF242429), plateBottom = Color(0xFF131317),
        bezel = Color(0xFF0D0D10), bezelBorder = Color(0xFF4A2B20),
        bezelWordmark = "TURBOGRAFX-16", bezelWordmarkColor = Color(0xFFFF8A50),
        bezelStripeA = Color(0xFFFF5722), bezelStripeB = Color(0xFF78350F),
        bezelSubtitle = "PC ENGINE · 16-BIT HUCARD",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF5722), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF32323A), arrow = Color(0xFF9494A0),
        face = { Color(0xFFFF5722) },
        faceLabel = { Color.White },
        pill = Color(0xFF36363E), pillLabel = Color(0xFFFFAB80),
        shoulder = Color(0xFF36363E), shoulderLabel = Color(0xFFFFAB80),
        iconFill = Color(0xFF2E2E36), iconTint = Color(0xFFFFD6C2),
    )

    private val superGrafx = ConsoleStyle(
        plateTop = Color(0xFF2B2D36), plateBottom = Color(0xFF161820),
        bezel = Color(0xFF0E0F14), bezelBorder = Color(0xFF563228),
        bezelWordmark = "PC ENGINE SUPERGRAFX", bezelWordmarkColor = Color(0xFFFF9066),
        bezelStripeA = Color(0xFFD84315), bezelStripeB = Color(0xFF94A3B8),
        bezelSubtitle = "DUAL VDC 16-BIT",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF6D3A), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF383B48), arrow = Color(0xFF9AA0B2),
        face = { Color(0xFFD84315) },
        faceLabel = { Color.White },
        pill = Color(0xFF383B48), pillLabel = Color(0xFFFFB296),
        shoulder = Color(0xFF383B48), shoulderLabel = Color(0xFFFFB296),
        iconFill = Color(0xFF303340), iconTint = Color(0xFFF0E0DA),
    )

    private val pceCd = ConsoleStyle(
        plateTop = Color(0xFF222226), plateBottom = Color(0xFF111114),
        bezel = Color(0xFF0C0C0E), bezelBorder = Color(0xFF5A4224),
        bezelWordmark = "TURBOGRAFX-CD", bezelWordmarkColor = Color(0xFFFFB74D),
        bezelStripeA = Color(0xFFF59E0B), bezelStripeB = Color(0xFFEA580C),
        bezelSubtitle = "SUPER CD-ROM² SYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFF59E0B), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF34343B), arrow = Color(0xFF9696A2),
        face = { Color(0xFFEA580C) },
        faceLabel = { Color.White },
        pill = Color(0xFF34343B), pillLabel = Color(0xFFFCD34D),
        shoulder = Color(0xFF34343B), shoulderLabel = Color(0xFFFCD34D),
        iconFill = Color(0xFF2C2C33), iconTint = Color(0xFFFEEAC2),
    )

    private val pcFx = ConsoleStyle(
        plateTop = Color(0xFFEBEAF2), plateBottom = Color(0xFFCFCEE0),
        bezel = Color(0xFF1A1626), bezelBorder = Color(0xFF6D4CB8),
        bezelWordmark = "NEC PC-FX", bezelWordmarkColor = Color(0xFFD8C4FF),
        bezelStripeA = Color(0xFF7C3AED), bezelStripeB = Color(0xFF06B6D4),
        bezelSubtitle = "32-BIT ANIMATION ENGINE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF9333EA), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF423C58), arrow = Color(0xFFA8A0C4),
        face = { Color(0xFF6D28D9) },
        faceLabel = { Color.White },
        pill = Color(0xFF4C4666), pillLabel = Color(0xFFEAE4FF),
        shoulder = Color(0xFFB5B2CC), shoulderLabel = Color(0xFF2B2440),
        iconFill = Color(0xFFC2BFE0), iconTint = Color(0xFF241E38),
    )

    // ---- SNK & Bandai ---------------------------------------------------------------------------

    private val neoGeo = ConsoleStyle(
        plateTop = Color(0xFF1B1A1D), plateBottom = Color(0xFF0C0B0E),
        bezel = Color(0xFF09090B), bezelBorder = Color(0xFF8C7228),
        bezelWordmark = "NEO GEO", bezelWordmarkColor = Color(0xFFEAC55A),
        bezelStripeA = Color(0xFFD4AF37), bezelStripeB = Color(0xFF785E18),
        bezelSubtitle = "MAX 330 MEGA · PRO-GEAR",
        formFactor = BezelFormFactor.ArcadeCabinet,
        ledColor = Color(0xFFFFD154), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2A282E), arrow = Color(0xFFB89E4C),
        face = {
            when (it) {
                "B" -> Color(0xFFE53935)
                "A" -> Color(0xFFF5B82E)
                "Y" -> Color(0xFF22A055)
                "X" -> Color(0xFF1E88E5)
                else -> Color(0xFF302E36)
            }
        },
        faceLabel = { if (it == "A") Color(0xFF1C1A16) else Color.White },
        pill = Color(0xFF2E2B32), pillLabel = Color(0xFFEAC55A),
        shoulder = Color(0xFF2E2B32), shoulderLabel = Color(0xFFEAC55A),
        iconFill = Color(0xFF2A272E), iconTint = Color(0xFFF4DCA0),
    )

    private val neoGeoCd = ConsoleStyle(
        plateTop = Color(0xFF201F24), plateBottom = Color(0xFF0E0D11),
        bezel = Color(0xFF0A090C), bezelBorder = Color(0xFF7A5624),
        bezelWordmark = "NEO GEO CD", bezelWordmarkColor = Color(0xFFF4C858),
        bezelStripeA = Color(0xFFD4AF37), bezelStripeB = Color(0xFFD84315),
        bezelSubtitle = "SNK 16-BIT OPTICAL",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF9100), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2E2C34), arrow = Color(0xFFB09858),
        face = {
            when (it) {
                "B" -> Color(0xFFE53935)
                "A" -> Color(0xFFF5B82E)
                "Y" -> Color(0xFF22A055)
                "X" -> Color(0xFF1E88E5)
                else -> Color(0xFF32303A)
            }
        },
        faceLabel = { if (it == "A") Color(0xFF1C1A16) else Color.White },
        pill = Color(0xFF302E36), pillLabel = Color(0xFFF4C858),
        shoulder = Color(0xFF302E36), shoulderLabel = Color(0xFFF4C858),
        iconFill = Color(0xFF2A2830), iconTint = Color(0xFFF6DF9E),
    )

    private val neoGeoPocket = ConsoleStyle(
        plateTop = Color(0xFF364258), plateBottom = Color(0xFF1E2636),
        bezel = Color(0xFF11151E), bezelBorder = Color(0xFF4B5C78),
        bezelWordmark = "NEO GEO POCKET COLOR", bezelWordmarkColor = Color(0xFFD8E6FF),
        bezelStripeA = Color(0xFF00B4D8), bezelStripeB = Color(0xFFFF7A1A),
        bezelSubtitle = "SNK 16-BIT HANDHELD",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFFFF5252), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF1D2330), arrow = Color(0xFF8AA0C0),
        face = {
            when (it) {
                "A" -> Color(0xFFE53935)
                "B" -> Color(0xFF2563EB)
                else -> Color(0xFF283244)
            }
        },
        pill = Color(0xFF242D3E), pillLabel = Color(0xFFC8DCFA),
        shoulder = Color(0xFF34425A), shoulderLabel = Color(0xFFE0ECFF),
        iconFill = Color(0xFF242D3E), iconTint = Color(0xFFE0ECFF),
    )

    private val wonderSwan = ConsoleStyle(
        plateTop = Color(0xFF246A8A), plateBottom = Color(0xFF144258),
        bezel = Color(0xFF0F1A22), bezelBorder = Color(0xFF3A7B99),
        bezelWordmark = "WONDERSWAN COLOR", bezelWordmarkColor = Color(0xFFD4F3FF),
        bezelStripeA = Color(0xFF00ACC1), bezelStripeB = Color(0xFF38BDF8),
        bezelSubtitle = "BANDAI 16-BIT HANDHELD",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF26C6DA), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF1A3A4A), arrow = Color(0xFF88CFE8),
        face = { Color(0xFF163544) },
        faceLabel = { Color(0xFFD4F3FF) },
        pill = Color(0xFF163544), pillLabel = Color(0xFFA8E4F8),
        shoulder = Color(0xFF1E4C62), shoulderLabel = Color(0xFFD4F3FF),
        iconFill = Color(0xFF163544), iconTint = Color(0xFFD4F3FF),
    )

    // ---- Atari ----------------------------------------------------------------------------------

    private val atari2600 = ConsoleStyle(
        plateTop = Color(0xFF6B4224), plateBottom = Color(0xFF422510),
        bezel = Color(0xFF151312), bezelBorder = Color(0xFF8C5A32),
        bezelWordmark = "ATARI 2600", bezelWordmarkColor = Color(0xFFF6C278),
        bezelStripeA = Color(0xFFEA6A1F), bezelStripeB = Color(0xFFD9A441),
        bezelSubtitle = "VIDEO COMPUTER SYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        woodgrain = true,
        ledColor = Color(0xFFFF6D1F), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF1C1A19), arrow = Color(0xFFA88868),
        face = { Color(0xFFE53935) },
        faceLabel = { Color.White },
        pill = Color(0xFFC29B56), pillLabel = Color(0xFF28180C),
        shoulder = Color(0xFF3B2A1E), shoulderLabel = Color(0xFFF6D29E),
        iconFill = Color(0xFF2B1C12), iconTint = Color(0xFFF4D4A8),
    )

    private val atari5200 = ConsoleStyle(
        plateTop = Color(0xFF2A2B30), plateBottom = Color(0xFF16171B),
        bezel = Color(0xFF0E0F12), bezelBorder = Color(0xFF8E929E),
        bezelWordmark = "ATARI 5200", bezelWordmarkColor = Color(0xFFE4E6EE),
        bezelStripeA = Color(0xFFB8BCC8), bezelStripeB = Color(0xFFE53935),
        bezelSubtitle = "SUPERSYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF1F2026), arrow = Color(0xFF9A9EA8),
        face = { Color(0xFFD62828) },
        faceLabel = { Color.White },
        pill = Color(0xFF3A3C44), pillLabel = Color(0xFFE4E6EE),
        shoulder = Color(0xFF3A3C44), shoulderLabel = Color(0xFFE4E6EE),
        iconFill = Color(0xFF32343C), iconTint = Color(0xFFE4E6EE),
    )

    private val atari7800 = ConsoleStyle(
        plateTop = Color(0xFF36383E), plateBottom = Color(0xFF1B1C20),
        bezel = Color(0xFF101114), bezelBorder = Color(0xFFA6AAB6),
        bezelWordmark = "ATARI 7800", bezelWordmarkColor = Color(0xFFECEEF4),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFFFACC15), bezelStripeC = Color(0xFF2563EB),
        bezelSubtitle = "PROSYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF5252), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF222328), arrow = Color(0xFF9EA2AE),
        face = { Color(0xFFC0C4D0) },
        faceLabel = { Color(0xFF1C1D22) },
        pill = Color(0xFF42454E), pillLabel = Color(0xFFECEEF4),
        shoulder = Color(0xFF42454E), shoulderLabel = Color(0xFFECEEF4),
        iconFill = Color(0xFF34363E), iconTint = Color(0xFFECEEF4),
    )

    private val jaguar = ConsoleStyle(
        plateTop = Color(0xFF222228), plateBottom = Color(0xFF101014),
        bezel = Color(0xFF0B0B0E), bezelBorder = Color(0xFF6E2024),
        bezelWordmark = "ATARI JAGUAR", bezelWordmarkColor = Color(0xFFFF6B6B),
        bezelStripeA = Color(0xFFD62828), bezelStripeB = Color(0xFF94A3B8),
        bezelSubtitle = "64-BIT MULTIMEDIA",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF2E2E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF34343C), arrow = Color(0xFF9898A4),
        face = { Color(0xFFC62828) },
        faceLabel = { Color.White },
        pill = Color(0xFF34343C), pillLabel = Color(0xFFF0D4D4),
        shoulder = Color(0xFF34343C), shoulderLabel = Color(0xFFF0D4D4),
        iconFill = Color(0xFF2C2C34), iconTint = Color(0xFFF0E4E4),
    )

    private val lynx = ConsoleStyle(
        plateTop = Color(0xFF363942), plateBottom = Color(0xFF1E2027),
        bezel = Color(0xFF121318), bezelBorder = Color(0xFF565A68),
        bezelWordmark = "ATARI LYNX", bezelWordmarkColor = Color(0xFFFF9654),
        bezelStripeA = Color(0xFFFF6D24), bezelStripeB = Color(0xFF94A3B8),
        bezelSubtitle = "16-BIT COLOR HANDHELD",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFFFF6D24), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF24262E), arrow = Color(0xFF9296A4),
        face = { Color(0xFFFF6D24) },
        faceLabel = { Color.White },
        pill = Color(0xFF282A32), pillLabel = Color(0xFFFFB88A),
        shoulder = Color(0xFF424652), shoulderLabel = Color(0xFFECEEF4),
        iconFill = Color(0xFF2C2F38), iconTint = Color(0xFFECEEF4),
    )

    private val atariSt = ConsoleStyle(
        plateTop = Color(0xFFC8C9CF), plateBottom = Color(0xFFA2A4AC),
        bezel = Color(0xFF1D1E22), bezelBorder = Color(0xFF5C5F68),
        bezelWordmark = "ATARI ST", bezelWordmarkColor = Color(0xFFE6E8F0),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFF22C55E), bezelStripeC = Color(0xFF2563EB),
        bezelSubtitle = "16/32-BIT PERSONAL COMPUTER",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF5C5F6A), arrow = Color(0xFFD8DAE2),
        face = { Color(0xFF4C4F5A) },
        faceLabel = { Color(0xFFF2F3F7) },
        pill = Color(0xFF5C5F6A), pillLabel = Color(0xFFF2F3F7),
        shoulder = Color(0xFF8A8D98), shoulderLabel = Color(0xFF1E2026),
        iconFill = Color(0xFF7E818C), iconTint = Color(0xFF1E2026),
    )

    // ---- Classic Consoles, Microcomputers & Fantasy Consoles ------------------------------------

    private val coleco = ConsoleStyle(
        plateTop = Color(0xFF262322), plateBottom = Color(0xFF141211),
        bezel = Color(0xFF0E0D0D), bezelBorder = Color(0xFF5C3E24),
        bezelWordmark = "COLECOVISION", bezelWordmarkColor = Color(0xFFFFB74D),
        bezelStripeA = Color(0xFFFB8C00), bezelStripeB = Color(0xFFFACC15), bezelStripeC = Color(0xFFE53935),
        bezelSubtitle = "ARCADE QUALITY AT HOME",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFB8C00), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF363230), arrow = Color(0xFFA89C92),
        face = { Color(0xFFFB8C00) },
        faceLabel = { Color(0xFF1E140A) },
        pill = Color(0xFF383330), pillLabel = Color(0xFFFFCC80),
        shoulder = Color(0xFF383330), shoulderLabel = Color(0xFFFFCC80),
        iconFill = Color(0xFF302B28), iconTint = Color(0xFFF6E2C8),
    )

    private val intellivision = ConsoleStyle(
        plateTop = Color(0xFF5E3C22), plateBottom = Color(0xFF3B2413),
        bezel = Color(0xFF181410), bezelBorder = Color(0xFFC9A248),
        bezelWordmark = "INTELLIVISION", bezelWordmarkColor = Color(0xFFEAD082),
        bezelStripeA = Color(0xFFD4AF37), bezelStripeB = Color(0xFF8C6828),
        bezelSubtitle = "INTELLIGENT TELEVISION",
        formFactor = BezelFormFactor.HomeConsole,
        woodgrain = true,
        ledColor = Color(0xFFEAC55A), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFFC29B44), arrow = Color(0xFF3A2810),
        face = { Color(0xFFC29B44) },
        faceLabel = { Color(0xFF22160A) },
        pill = Color(0xFF2E2218), pillLabel = Color(0xFFEAD082),
        shoulder = Color(0xFF423020), shoulderLabel = Color(0xFFEAD082),
        iconFill = Color(0xFF2E2218), iconTint = Color(0xFFF0DCA2),
    )

    private val vectrex = ConsoleStyle(
        plateTop = Color(0xFF1E2026), plateBottom = Color(0xFF0E1014),
        bezel = Color(0xFF090B0E), bezelBorder = Color(0xFF2B5248),
        bezelWordmark = "GCE VECTREX", bezelWordmarkColor = Color(0xFF66FFD1),
        bezelStripeA = Color(0xFF26A69A), bezelStripeB = Color(0xFF66FFD1),
        bezelSubtitle = "VECTOR GRAPHICS DISPLAY",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF4FFFB0), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF2C303A), arrow = Color(0xFF66FFD1),
        face = { Color(0xFF1E403A) },
        faceLabel = { Color(0xFF8CFFDA) },
        pill = Color(0xFF2A2E38), pillLabel = Color(0xFF8CFFDA),
        shoulder = Color(0xFF2A2E38), shoulderLabel = Color(0xFF8CFFDA),
        iconFill = Color(0xFF222A2E), iconTint = Color(0xFF8CFFDA),
    )

    private val odyssey2 = ConsoleStyle(
        plateTop = Color(0xFFB8B9C0), plateBottom = Color(0xFF8E909A),
        bezel = Color(0xFF18181E), bezelBorder = Color(0xFF564870),
        bezelWordmark = "MAGNAVOX ODYSSEY²", bezelWordmarkColor = Color(0xFFE0C8FF),
        bezelStripeA = Color(0xFF8E24AA), bezelStripeB = Color(0xFFE53935),
        bezelSubtitle = "VIDEOPAC G7000",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFAB47BC), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2A2A32), arrow = Color(0xFF9C9CAA),
        face = { Color(0xFF8E24AA) },
        faceLabel = { Color.White },
        pill = Color(0xFF3A3A44), pillLabel = Color(0xFFE6DCEB),
        shoulder = Color(0xFF7A7C88), shoulderLabel = Color(0xFF1A1A20),
        iconFill = Color(0xFF6E707C), iconTint = Color(0xFFF2F2F8),
    )

    private val threeDo = ConsoleStyle(
        plateTop = Color(0xFF24252C), plateBottom = Color(0xFF121318),
        bezel = Color(0xFF0C0D11), bezelBorder = Color(0xFF543044),
        bezelWordmark = "3DO INTERACTIVE", bezelWordmarkColor = Color(0xFFF4B6CE),
        bezelStripeA = Color(0xFFD81B60), bezelStripeB = Color(0xFFFACC15), bezelStripeC = Color(0xFF1E88E5),
        bezelSubtitle = "32-BIT MULTIMEDIA",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFD81B60), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF343642), arrow = Color(0xFF989CAC),
        face = { Color(0xFFD81B60) },
        faceLabel = { Color.White },
        pill = Color(0xFF343642), pillLabel = Color(0xFFF4C2D6),
        shoulder = Color(0xFF343642), shoulderLabel = Color(0xFFF4C2D6),
        iconFill = Color(0xFF2C2E38), iconTint = Color(0xFFF4D8E4),
    )

    private val cdi = ConsoleStyle(
        plateTop = Color(0xFF2A2D36), plateBottom = Color(0xFF161820),
        bezel = Color(0xFF0E1016), bezelBorder = Color(0xFF3B5074),
        bezelWordmark = "PHILIPS CD-I", bezelWordmarkColor = Color(0xFFC8E0FF),
        bezelStripeA = Color(0xFF1E88E5), bezelStripeB = Color(0xFF94A3B8),
        bezelSubtitle = "COMPACT DISC INTERACTIVE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFF38BDF8), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF383D4C), arrow = Color(0xFF98A4BC),
        face = { Color(0xFF1E88E5) },
        faceLabel = { Color.White },
        pill = Color(0xFF383D4C), pillLabel = Color(0xFFC8E0FF),
        shoulder = Color(0xFF383D4C), shoulderLabel = Color(0xFFC8E0FF),
        iconFill = Color(0xFF2F3442), iconTint = Color(0xFFDCEBFF),
    )

    private val supervision = ConsoleStyle(
        plateTop = Color(0xFFD0D1D6), plateBottom = Color(0xFFAAACB4),
        bezel = Color(0xFF3E444E), bezelBorder = Color(0xFF2B3038),
        bezelWordmark = "WATARA SUPERVISION", bezelWordmarkColor = Color(0xFFD2E2EC),
        bezelStripeA = Color(0xFF546E7A), bezelStripeB = Color(0xFF26A69A),
        bezelSubtitle = "8-BIT HANDHELD",
        formFactor = BezelFormFactor.GameBoyChamfer,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2A2D34), arrow = Color(0xFF828894),
        face = { Color(0xFF37474F) },
        faceLabel = { Color(0xFFE0ECEF) },
        pill = Color(0xFF546E7A), pillLabel = Color(0xFFECEFF2), pillTilt = -20f,
        shoulder = Color(0xFF888C96), shoulderLabel = Color(0xFF24262C),
        iconFill = Color(0xFF7E828C), iconTint = Color(0xFF1E2026),
    )

    private val channelF = ConsoleStyle(
        plateTop = Color(0xFF5A402D), plateBottom = Color(0xFF38261A),
        bezel = Color(0xFF161311), bezelBorder = Color(0xFF7D5C40),
        bezelWordmark = "FAIRCHILD CHANNEL F", bezelWordmarkColor = Color(0xFFF2D4A8),
        bezelStripeA = Color(0xFFD97706), bezelStripeB = Color(0xFF92400E),
        bezelSubtitle = "VIDEO ENTERTAINMENT SYSTEM",
        formFactor = BezelFormFactor.HomeConsole,
        woodgrain = true,
        ledColor = Color(0xFFF59E0B), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF261E18), arrow = Color(0xFFB4987E),
        face = { Color(0xFFD97706) },
        faceLabel = { Color.White },
        pill = Color(0xFF342820), pillLabel = Color(0xFFF2D4A8),
        shoulder = Color(0xFF3E3026), shoulderLabel = Color(0xFFF2D4A8),
        iconFill = Color(0xFF2E221A), iconTint = Color(0xFFF2D4A8),
    )

    private val msx = ConsoleStyle(
        plateTop = Color(0xFF272C3A), plateBottom = Color(0xFF151822),
        bezel = Color(0xFF0E1017), bezelBorder = Color(0xFF34486E),
        bezelWordmark = "MSX / MSX2", bezelWordmarkColor = Color(0xFFD2E4FF),
        bezelStripeA = Color(0xFF1976D2), bezelStripeB = Color(0xFF64B5F6),
        bezelSubtitle = "HOME COMPUTER STANDARD",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF38BDF8), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF363D50), arrow = Color(0xFF94A4C4),
        face = { Color(0xFF1976D2) },
        faceLabel = { Color.White },
        pill = Color(0xFF363D50), pillLabel = Color(0xFFD2E4FF),
        shoulder = Color(0xFF363D50), shoulderLabel = Color(0xFFD2E4FF),
        iconFill = Color(0xFF2E3444), iconTint = Color(0xFFDCEBFF),
    )

    private val c64 = ConsoleStyle(
        plateTop = Color(0xFF9E9282), plateBottom = Color(0xFF766B5D),
        bezel = Color(0xFF1E1B18), bezelBorder = Color(0xFF564D43),
        bezelWordmark = "COMMODORE 64", bezelWordmarkColor = Color(0xFFEBE4D8),
        bezelStripeA = Color(0xFF1E88E5), bezelStripeB = Color(0xFFE53935),
        bezelSubtitle = "64K PERSONAL COMPUTER",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF3B322C), arrow = Color(0xFFB8ACA0),
        face = { Color(0xFF4A3D34) },
        faceLabel = { Color(0xFFF4ECE0) },
        pill = Color(0xFF4A3D34), pillLabel = Color(0xFFF0E6D8),
        shoulder = Color(0xFF5E5146), shoulderLabel = Color(0xFFF0E6D8),
        iconFill = Color(0xFF4A3D34), iconTint = Color(0xFFF4ECE0),
    )

    private val amiga = ConsoleStyle(
        plateTop = Color(0xFFE4E0D4), plateBottom = Color(0xFFC4BFAF),
        bezel = Color(0xFF1B1B1E), bezelBorder = Color(0xFF5A5852),
        bezelWordmark = "COMMODORE AMIGA", bezelWordmarkColor = Color(0xFFF2EFE6),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFFF5F5F5),
        bezelSubtitle = "16/32-BIT MULTIMEDIA",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF4C4A46), arrow = Color(0xFFD2CEC4),
        face = { Color(0xFFE53935) },
        faceLabel = { Color.White },
        pill = Color(0xFF6E6B64), pillLabel = Color(0xFFF6F4EE),
        shoulder = Color(0xFF9C988E), shoulderLabel = Color(0xFF22211E),
        iconFill = Color(0xFF7A7770), iconTint = Color(0xFFF6F4EE),
    )

    private val zxSpectrum = ConsoleStyle(
        plateTop = Color(0xFF1B1C20), plateBottom = Color(0xFF0D0E12),
        bezel = Color(0xFF090A0D), bezelBorder = Color(0xFF363842),
        bezelWordmark = "ZX SPECTRUM", bezelWordmarkColor = Color(0xFFF0F2F7),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFFFACC15), bezelStripeC = Color(0xFF06B6D4),
        bezelSubtitle = "SINCLAIR 48K / 128K",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFFE53935), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF343844), arrow = Color(0xFF9AA2B4),
        face = { Color(0xFF3E4454) },
        faceLabel = { Color(0xFFF4F6FA) },
        pill = Color(0xFF343844), pillLabel = Color(0xFFDCE0EC),
        shoulder = Color(0xFF343844), shoulderLabel = Color(0xFFDCE0EC),
        iconFill = Color(0xFF2C303A), iconTint = Color(0xFFEAECEF),
    )

    private val amstradCpc = ConsoleStyle(
        plateTop = Color(0xFF25272E), plateBottom = Color(0xFF14161B),
        bezel = Color(0xFF0E0F14), bezelBorder = Color(0xFF3C424E),
        bezelWordmark = "AMSTRAD CPC", bezelWordmarkColor = Color(0xFFE0F2E4),
        bezelStripeA = Color(0xFF43A047), bezelStripeB = Color(0xFFE53935), bezelStripeC = Color(0xFF1E88E5),
        bezelSubtitle = "COLOUR PERSONAL COMPUTER",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF43A047), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF343842), arrow = Color(0xFF969EA8),
        face = {
            when (it) {
                "A" -> Color(0xFF43A047)
                "B" -> Color(0xFFE53935)
                else -> Color(0xFF1E88E5)
            }
        },
        pill = Color(0xFF343842), pillLabel = Color(0xFFDCE6E0),
        shoulder = Color(0xFF343842), shoulderLabel = Color(0xFFDCE6E0),
        iconFill = Color(0xFF2D303A), iconTint = Color(0xFFE4ECE6),
    )

    private val sharpX68000 = ConsoleStyle(
        plateTop = Color(0xFF23262E), plateBottom = Color(0xFF111318),
        bezel = Color(0xFF0B0C10), bezelBorder = Color(0xFF3C4452),
        bezelWordmark = "SHARP X68000", bezelWordmarkColor = Color(0xFFD4E0EA),
        bezelStripeA = Color(0xFF22C55E), bezelStripeB = Color(0xFF607D8B),
        bezelSubtitle = "16-BIT WORKSTATION",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF323640), arrow = Color(0xFF929AA8),
        face = { Color(0xFF37474F) },
        faceLabel = { Color(0xFFE6F0F4) },
        pill = Color(0xFF323640), pillLabel = Color(0xFFD4E0EA),
        shoulder = Color(0xFF323640), shoulderLabel = Color(0xFFD4E0EA),
        iconFill = Color(0xFF2A2E38), iconTint = Color(0xFFDCE8F0),
    )

    private val pc98 = ConsoleStyle(
        plateTop = Color(0xFFE6E3D8), plateBottom = Color(0xFFC8C4B6),
        bezel = Color(0xFF1A1C22), bezelBorder = Color(0xFF465C7A),
        bezelWordmark = "NEC PC-98", bezelWordmarkColor = Color(0xFFD2E8FF),
        bezelStripeA = Color(0xFF0277BD), bezelStripeB = Color(0xFF90A4AE),
        bezelSubtitle = "16/32-BIT PERSONAL COMPUTER",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF4C505C), arrow = Color(0xFFD2D6E0),
        face = { Color(0xFF0277BD) },
        faceLabel = { Color.White },
        pill = Color(0xFF646874), pillLabel = Color(0xFFF0F2F6),
        shoulder = Color(0xFF9C9A90), shoulderLabel = Color(0xFF1E2026),
        iconFill = Color(0xFF7A7C84), iconTint = Color(0xFFF0F2F6),
    )

    private val dos = ConsoleStyle(
        plateTop = Color(0xFFDDD8C8), plateBottom = Color(0xFFBEB8A6),
        bezel = Color(0xFF1A1B1E), bezelBorder = Color(0xFF5A5850),
        bezelWordmark = "MS-DOS", bezelWordmarkColor = Color(0xFFDAE6F2),
        bezelStripeA = Color(0xFF2563EB), bezelStripeB = Color(0xFF22C55E),
        bezelSubtitle = "IBM PC COMPATIBLE",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF22C55E), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF4A4B50), arrow = Color(0xFFD0D0D6),
        face = { Color(0xFF455A64) },
        faceLabel = { Color(0xFFF2F4F6) },
        pill = Color(0xFF6C6A64), pillLabel = Color(0xFFF6F4EE),
        shoulder = Color(0xFF9E9A90), shoulderLabel = Color(0xFF222224),
        iconFill = Color(0xFF7A7870), iconTint = Color(0xFFF6F4EE),
    )

    private val scummVm = ConsoleStyle(
        plateTop = Color(0xFF262A3E), plateBottom = Color(0xFF141624),
        bezel = Color(0xFF0D0E18), bezelBorder = Color(0xFF4E5678),
        bezelWordmark = "SCUMMVM", bezelWordmarkColor = Color(0xFFE8E4B8),
        bezelStripeA = Color(0xFF7CB342), bezelStripeB = Color(0xFFF5B82E),
        bezelSubtitle = "POINT & CLICK ADVENTURE",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF9CCC65), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF363C56), arrow = Color(0xFFA2ACC8),
        face = { Color(0xFF7CB342) },
        faceLabel = { Color(0xFF121C0A) },
        pill = Color(0xFF363C56), pillLabel = Color(0xFFE8E4B8),
        shoulder = Color(0xFF363C56), shoulderLabel = Color(0xFFE8E4B8),
        iconFill = Color(0xFF2E334A), iconTint = Color(0xFFECEAD0),
    )

    private val pico8 = ConsoleStyle(
        plateTop = Color(0xFF1D2B53), plateBottom = Color(0xFF111A36),
        bezel = Color(0xFF0B0E1A), bezelBorder = Color(0xFF4B3D6B),
        bezelWordmark = "PICO-8", bezelWordmarkColor = Color(0xFFFF77A8),
        bezelStripeA = Color(0xFFFF004D), bezelStripeB = Color(0xFF29ADFF), bezelStripeC = Color(0xFF00E436),
        bezelSubtitle = "FANTASY CONSOLE · 128×128",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFFFF004D), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF293B6E), arrow = Color(0xFF88A4E0),
        face = {
            when (it) {
                "A" -> Color(0xFFFF004D)
                "B" -> Color(0xFF29ADFF)
                else -> Color(0xFF293B6E)
            }
        },
        pill = Color(0xFF263664), pillLabel = Color(0xFFFFEC27),
        shoulder = Color(0xFF2B3E72), shoulderLabel = Color(0xFFFFF1E8),
        iconFill = Color(0xFF24345E), iconTint = Color(0xFFFFF1E8),
    )

    private val tic80 = ConsoleStyle(
        plateTop = Color(0xFF24324B), plateBottom = Color(0xFF141D2E),
        bezel = Color(0xFF0C111C), bezelBorder = Color(0xFF3B5A7A),
        bezelWordmark = "TIC-80", bezelWordmarkColor = Color(0xFF73EFF7),
        bezelStripeA = Color(0xFF41A6F6), bezelStripeB = Color(0xFF38B764),
        bezelSubtitle = "TINY COMPUTER",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF38B764), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF324464), arrow = Color(0xFF94B4D8),
        face = {
            when (it) {
                "A" -> Color(0xFF41A6F6)
                "B" -> Color(0xFF38B764)
                "X" -> Color(0xFFEF7D57)
                "Y" -> Color(0xFFFFCD75)
                else -> Color(0xFF324464)
            }
        },
        faceLabel = { if (it == "Y") Color(0xFF1A1C2C) else Color.White },
        pill = Color(0xFF2D3E5C), pillLabel = Color(0xFFA7F070),
        shoulder = Color(0xFF324464), shoulderLabel = Color(0xFFF4F4F4),
        iconFill = Color(0xFF2B3B56), iconTint = Color(0xFFF4F4F4),
    )

    private val j2me = ConsoleStyle(
        plateTop = Color(0xFF2D3648), plateBottom = Color(0xFF181E2B),
        bezel = Color(0xFF0E121A), bezelBorder = Color(0xFF566480),
        bezelWordmark = "JAVA ME", bezelWordmarkColor = Color(0xFFFFB770),
        bezelStripeA = Color(0xFFEF6C00), bezelStripeB = Color(0xFF38BDF8),
        bezelSubtitle = "MOBILE EDITION",
        formFactor = BezelFormFactor.HandheldWide,
        ledColor = Color(0xFF38BDF8), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF3C475E), arrow = Color(0xFFA4B2CC),
        face = { Color(0xFFEF6C00) },
        faceLabel = { Color.White },
        pill = Color(0xFF3C475E), pillLabel = Color(0xFFE0E8F6),
        shoulder = Color(0xFF424E66), shoulderLabel = Color(0xFFE0E8F6),
        iconFill = Color(0xFF343E52), iconTint = Color(0xFFE0E8F6),
    )

    private val heroBox16 = ConsoleStyle(
        plateTop = Color(0xFF231F36), plateBottom = Color(0xFF12101F),
        bezel = Color(0xFF0B0A12), bezelBorder = Color(0xFF56478A),
        bezelWordmark = "HEROBOX-16", bezelWordmarkColor = Color(0xFFDCCEFF),
        bezelStripeA = Color(0xFFFF5A1F), bezelStripeB = Color(0xFFD0BCFF),
        bezelSubtitle = "RETROHERO ORIGINAL CONSOLE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF5A1F), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF332E4D), arrow = Color(0xFFA89ED4),
        face = {
            when (it) {
                "A" -> Color(0xFFFF5A1F)
                "B" -> Color(0xFFD0BCFF)
                "X" -> Color(0xFF38BDF8)
                "Y" -> Color(0xFF34D399)
                else -> Color(0xFF332E4D)
            }
        },
        faceLabel = { if (it == "B") Color(0xFF1E1832) else Color.White },
        pill = Color(0xFF322C4A), pillLabel = Color(0xFFDCCEFF),
        shoulder = Color(0xFF383254), shoulderLabel = Color(0xFFEDE6FF),
        iconFill = Color(0xFF2D2844), iconTint = Color(0xFFEDE6FF),
    )

    private val chip8 = ConsoleStyle(
        plateTop = Color(0xFF1C2620), plateBottom = Color(0xFF0E1410),
        bezel = Color(0xFF080D0A), bezelBorder = Color(0xFF345E42),
        bezelWordmark = "CHIP-8", bezelWordmarkColor = Color(0xFF9CD67D),
        bezelStripeA = Color(0xFF57E389), bezelStripeB = Color(0xFF2E7D32),
        bezelSubtitle = "COSMAC VIP · SUPER-CHIP",
        formFactor = BezelFormFactor.ComputerMonitor,
        ledColor = Color(0xFF57E389), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF28382E), arrow = Color(0xFF86C494),
        face = { Color(0xFF2E5E3E) },
        faceLabel = { Color(0xFFC8F6D2) },
        pill = Color(0xFF28382E), pillLabel = Color(0xFF9CD67D),
        shoulder = Color(0xFF28382E), shoulderLabel = Color(0xFF9CD67D),
        iconFill = Color(0xFF223028), iconTint = Color(0xFFC8F6D2),
    )

    private val arcade = ConsoleStyle(
        plateTop = Color(0xFF211E28), plateBottom = Color(0xFF100E15),
        bezel = Color(0xFF0A090E), bezelBorder = Color(0xFF5C4B24),
        bezelWordmark = "ARCADE CABINET", bezelWordmarkColor = Color(0xFFFFDF5D),
        bezelStripeA = Color(0xFFFFD600), bezelStripeB = Color(0xFFFF3B4B),
        bezelSubtitle = "INSERT COIN · 6-BUTTON DECK",
        formFactor = BezelFormFactor.ArcadeCabinet,
        ledColor = Color(0xFFFFD600), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF2E2A38), arrow = Color(0xFFB4A478),
        face = {
            when (it) {
                "A", "Y" -> Color(0xFFFF3B4B)
                "B", "X" -> Color(0xFFFFD600)
                "L", "R" -> Color(0xFF29B6F6)
                else -> Color(0xFF302C3A)
            }
        },
        faceLabel = { if (it in setOf("B", "X")) Color(0xFF1C1810) else Color.White },
        pill = Color(0xFF342E40), pillLabel = Color(0xFFFFDF5D),
        shoulder = Color(0xFF342E40), shoulderLabel = Color(0xFFFFDF5D),
        iconFill = Color(0xFF2C2836), iconTint = Color(0xFFFFECB3),
    )

    private val darkFallback = ConsoleStyle(
        plateTop = Color(0xFF2E2D34), plateBottom = Color(0xFF18171C),
        bezel = Color(0xFF111115), bezelBorder = Color(0xFF2E2E36),
        bezelWordmark = "RETROHERO", bezelWordmarkColor = Color(0xFFC4C3CE),
        bezelSubtitle = "CLASSIC CONSOLE",
        formFactor = BezelFormFactor.HomeConsole,
        ledColor = Color(0xFFFF5416), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF1C1B20), arrow = Color(0xFF77767E),
        face = { Color(0xFF2A2930) },
        faceLabel = { Color(0xFFE2E1E8) },
        pill = Color(0xFF3A3940), pillLabel = Color(0xFFBDBCC4),
        shoulder = Color(0xFF3A3940), shoulderLabel = Color(0xFFE2E1E8),
        iconFill = Color(0xFF3A3940), iconTint = Color(0xFFE2E1E8),
    )

    /**
     * Tactile hardware deck matching RetroHero's Cream Light / Obsidian Dark + Ember theme,
     * while preserving the exact console's wordmark, subtitle, and bezel silhouette.
     */
    fun instrument(
        dark: Boolean,
        accent: Color,
        onAccent: Color = Color.White,
        systemName: String = "RETROHERO",
        systemId: String = "",
    ): ConsoleStyle {
        val consoleRef = if (systemId.isNotBlank()) forSystem(systemId, accent) else null
        val resolvedWordmark = consoleRef?.bezelWordmark
            ?: Systems.get(systemId)?.name?.uppercase()
            ?: systemName.uppercase()
        val resolvedSubtitle = consoleRef?.bezelSubtitle ?: ""
        val resolvedForm = consoleRef?.formFactor ?: BezelFormFactor.HomeConsole
        val isPrimary = { label: String -> label == "A" || label == "○" || label == "I" || label == "FIRE" }
        return if (dark) {
            ConsoleStyle(
                plateTop = Color(0xFF1C1C20), plateBottom = Color(0xFF111114),
                bezel = Color(0xFF0B0B0D), bezelBorder = Color(0xFF28282E),
                bezelWordmark = resolvedWordmark, bezelWordmarkColor = Color(0xFFA8A8B2),
                bezelStripeA = accent.copy(alpha = 0.72f), bezelStripeB = Color(0xFF36363E),
                bezelSubtitle = resolvedSubtitle,
                formFactor = resolvedForm,
                ledColor = accent, speaker = consoleRef?.speaker ?: SpeakerStyle.DotMatrix,
                dpad = Color(0xFF2B2B30), arrow = Color(0xFF8E8E96),
                face = { if (isPrimary(it)) accent else Color(0xFF2A2A30) },
                faceLabel = { if (isPrimary(it)) onAccent else Color(0xFFF2F1EE) },
                pill = Color(0xFF26262B), pillLabel = Color(0xFFD0CFCB),
                shoulder = Color(0xFF26262B), shoulderLabel = Color(0xFFD0CFCB),
                iconFill = Color(0xFF26262B), iconTint = Color(0xFFF2F1EE),
            )
        } else {
            ConsoleStyle(
                plateTop = Color(0xFFEFEEE9), plateBottom = Color(0xFFE0DED7),
                bezel = Color(0xFF17171A), bezelBorder = Color(0xFF2D2D32),
                bezelWordmark = resolvedWordmark, bezelWordmarkColor = Color(0xFFD6D4CE),
                bezelStripeA = accent.copy(alpha = 0.75f), bezelStripeB = Color(0xFF46464E),
                bezelSubtitle = resolvedSubtitle,
                formFactor = resolvedForm,
                ledColor = accent, speaker = consoleRef?.speaker ?: SpeakerStyle.DotMatrix,
                dpad = Color(0xFF1C1C1F), arrow = Color(0xFF8A8A90),
                face = { if (isPrimary(it)) accent else Color(0xFFF8F7F4) },
                faceLabel = { if (isPrimary(it)) onAccent else Color(0xFF141416) },
                pill = Color(0xFFD8D6CF), pillLabel = Color(0xFF26262A),
                shoulder = Color(0xFFD8D6CF), shoulderLabel = Color(0xFF26262A),
                iconFill = Color(0xFFD8D6CF), iconTint = Color(0xFF141416),
            )
        }
    }

    /** Returns the authentic console-matched display & controller style for every supported system. */
    fun forSystem(systemId: String, accent: Color): ConsoleStyle = when (systemId) {
        // Nintendo
        "nes" -> nes
        "snes" -> snes
        "n64" -> n64
        "gb" -> gameBoy
        "gbc" -> gbc
        "gba" -> gba
        "nds" -> nds
        "3ds" -> n3ds
        "gc" -> gameCube
        "wii" -> wii
        "wiiu" -> wiiU
        "switch" -> nintendoSwitch
        "vb" -> virtualBoy
        "pokemini" -> pokeMini
        "gw" -> gameAndWatch

        // Sega
        "genesis" -> genesis
        "sms" -> masterSystem
        "gg" -> gameGear
        "sg1000" -> sg1000
        "segacd" -> segaCd
        "32x" -> sega32x
        "saturn" -> saturn
        "dreamcast" -> dreamcast
        "naomi" -> naomi

        // Sony
        "psx" -> playStation
        "psp" -> psp
        "ps2" -> playStation2
        "psvita" -> psVita

        // NEC
        "pce" -> pcEngine
        "supergrafx" -> superGrafx
        "pcecd" -> pceCd
        "pcfx" -> pcFx

        // SNK & Bandai
        "ngp" -> neoGeoPocket
        "neogeo" -> neoGeo
        "neocd" -> neoGeoCd
        "ws" -> wonderSwan

        // Atari
        "lynx" -> lynx
        "atari2600" -> atari2600
        "atari5200" -> atari5200
        "atari7800" -> atari7800
        "jaguar" -> jaguar
        "atarist" -> atariSt

        // Classic Consoles, Home Computers, Fantasy Consoles & Arcade
        "coleco" -> coleco
        "intellivision" -> intellivision
        "vectrex" -> vectrex
        "odyssey2" -> odyssey2
        "3do" -> threeDo
        "cdi" -> cdi
        "supervision" -> supervision
        "channelf" -> channelF
        "msx" -> msx
        "c64" -> c64
        "amiga" -> amiga
        "zxspectrum" -> zxSpectrum
        "amstradcpc" -> amstradCpc
        "x68000" -> sharpX68000
        "pc98" -> pc98
        "dos" -> dos
        "scummvm" -> scummVm
        "pico8" -> pico8
        "tic80" -> tic80
        "j2me" -> j2me
        "herobox16" -> heroBox16
        "chip8" -> chip8
        "arcade" -> arcade

        else -> {
            val sys = Systems.get(systemId)
            darkFallback.copy(
                bezelWordmark = (sys?.name ?: systemId).uppercase(),
                bezelSubtitle = sys?.manufacturer?.uppercase() ?: "CLASSIC CONSOLE",
                bezelStripeA = accent,
                bezelStripeB = accent.copy(alpha = 0.45f),
                ledColor = accent,
                face = { accent },
            )
        }
    }
}
