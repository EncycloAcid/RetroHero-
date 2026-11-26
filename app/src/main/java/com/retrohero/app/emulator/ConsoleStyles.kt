package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color

enum class SpeakerStyle { None, Slotted, DotMatrix, TwinDots }

/**
 * Complete physical hardware appearance for a Delta-inspired full-device handheld shell:
 * the seamless body housing, screen display lens bezel, power LED, stamped bezel wordmark,
 * D-pad, face buttons, rubber Select/Start pills, shoulder triggers, and speaker grille.
 */
data class ConsoleStyle(
    val plateTop: Color,
    val plateBottom: Color,
    /** Recessed glass bezel surrounding the live game display. */
    val bezel: Color = Color(0xFF18191E),
    val bezelBorder: Color = Color(0xFF2D2F38),
    val bezelWordmark: String = "RETROHERO",
    val bezelWordmarkColor: Color = Color(0xFFB5B8C4),
    /** Optional twin top pinstripes inside the bezel (Game Boy DMG / NES classic touch). */
    val bezelStripeA: Color? = null,
    val bezelStripeB: Color? = null,
    val bezelSubtitle: String = "DOT MATRIX WITH STEREO SOUND",
    val ledColor: Color = Color(0xFF39D353),
    val speaker: SpeakerStyle = SpeakerStyle.DotMatrix,
    /** NES-style black inset panel the buttons sit on. */
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
    private val nes = ConsoleStyle(
        plateTop = Color(0xFFDAD9D4), plateBottom = Color(0xFFB7B6B0),
        bezel = Color(0xFF19191C), bezelBorder = Color(0xFF34343A),
        bezelWordmark = "NINTENDO", bezelWordmarkColor = Color(0xFFE03344),
        bezelStripeA = Color(0xFFD0203C), bezelStripeB = Color(0xFF424248),
        bezelSubtitle = "8-BIT ENTERTAINMENT SYSTEM",
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        band = Color(0xFF1D1D20),
        dpad = Color(0xFF252528), arrow = Color(0xFF727278),
        face = { Color(0xFFD0203C) },
        faceLabel = { null }, labelBelow = Color(0xFFD0203C),
        pill = Color(0xFF3A3A3E), pillLabel = Color(0xFFD0203C),
        iconFill = Color(0xFF3A3A3E), iconTint = Color(0xFFE0DFDA),
    )

    private val gameBoy = ConsoleStyle(
        plateTop = Color(0xFFE0DDD4), plateBottom = Color(0xFFC2BEB3),
        bezel = Color(0xFF5A5D67), bezelBorder = Color(0xFF43464F),
        bezelWordmark = "GAME BOY", bezelWordmarkColor = Color(0xFF25317C),
        bezelStripeA = Color(0xFF8B1E4F), bezelStripeB = Color(0xFF25317C),
        bezelSubtitle = "DOT MATRIX WITH STEREO SOUND",
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
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF222228), arrow = Color(0xFF6B6B76),
        face = { Color(0xFF25252C) },
        faceLabel = { Color(0xFFD8D6EC) },
        pill = Color(0xFF26262E), pillLabel = Color(0xFFC5C0E6), pillTilt = -22f,
        iconFill = Color(0xFF2C2942), iconTint = Color(0xFFE4E2F5),
    )

    private val gba = ConsoleStyle(
        plateTop = Color(0xFF5D52B2), plateBottom = Color(0xFF3D3388),
        bezel = Color(0xFF1D1D24), bezelBorder = Color(0xFF343344),
        bezelWordmark = "GAME BOY ADVANCE", bezelWordmarkColor = Color(0xFFB9B6D6),
        ledColor = Color(0xFF39D353), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF25242E), arrow = Color(0xFF6E6B84),
        face = { Color(0xFFDFDFE8) },
        faceLabel = { Color(0xFF3D3388) },
        pill = Color(0xFF26242E), pillLabel = Color(0xFFD7D4F2),
        shoulder = Color(0xFFC4C2D6), shoulderLabel = Color(0xFF3D3388),
        iconFill = Color(0xFF26242E), iconTint = Color(0xFFD7D4F2),
    )

    private val snes = ConsoleStyle(
        plateTop = Color(0xFFD9D8DF), plateBottom = Color(0xFFB7B6C0),
        bezel = Color(0xFF202027), bezelBorder = Color(0xFF3B3B46),
        bezelWordmark = "SUPER NINTENDO", bezelWordmarkColor = Color(0xFFC8C7D4),
        bezelStripeA = Color(0xFF6D5ACF), bezelStripeB = Color(0xFF9C90E4),
        bezelSubtitle = "16-BIT ENTERTAINMENT SYSTEM",
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
        bezelStripeA = Color(0xFF22A055), bezelStripeB = Color(0xFFE63946),
        bezelSubtitle = "64-BIT REALITY CO-PROCESSOR",
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

    private val nds = ConsoleStyle(
        plateTop = Color(0xFF2C2D33), plateBottom = Color(0xFF1B1C20),
        bezel = Color(0xFF101114), bezelBorder = Color(0xFF2C2D34),
        bezelWordmark = "NINTENDO DS", bezelWordmarkColor = Color(0xFFC0C3CE),
        ledColor = Color(0xFF34D399), speaker = SpeakerStyle.TwinDots,
        dpad = Color(0xFF3E404A), arrow = Color(0xFF8C909E),
        face = { Color(0xFF3E404A) },
        faceLabel = { Color(0xFFF0F1F6) },
        pill = Color(0xFF3A3C45), pillLabel = Color(0xFFA8ACB8),
        shoulder = Color(0xFF464852), shoulderLabel = Color(0xFFE6E8F0),
        iconFill = Color(0xFF34363E), iconTint = Color(0xFFE6E8F0),
    )

    private val sega = ConsoleStyle(
        plateTop = Color(0xFF28282C), plateBottom = Color(0xFF131316),
        bezel = Color(0xFF0E0E11), bezelBorder = Color(0xFF2C2C34),
        bezelWordmark = "SEGA GENESIS", bezelWordmarkColor = Color(0xFFD0D2DC),
        bezelStripeA = Color(0xFFE53935), bezelStripeB = Color(0xFF1E88E5),
        bezelSubtitle = "16-BIT HIGH DEFINITION",
        ledColor = Color(0xFFFF3B30), speaker = SpeakerStyle.Slotted,
        dpad = Color(0xFF3B3B40), arrow = Color(0xFF92929A),
        face = { Color(0xFF1C1C20) },
        faceLabel = { Color(0xFFD4D4DC) },
        pill = Color(0xFF3B3B40), pillLabel = Color(0xFFD4D4DC),
        shoulder = Color(0xFF3B3B40), shoulderLabel = Color(0xFFD4D4DC),
        iconFill = Color(0xFF35353B), iconTint = Color(0xFFDCDCE4),
    )

    private val playStation = ConsoleStyle(
        plateTop = Color(0xFFCECFD4), plateBottom = Color(0xFFAAAAB2),
        bezel = Color(0xFF1C1D22), bezelBorder = Color(0xFF383A44),
        bezelWordmark = "PLAYSTATION", bezelWordmarkColor = Color(0xFFD4D6E0),
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

    private val psp = ConsoleStyle(
        plateTop = Color(0xFF1D1E22), plateBottom = Color(0xFF0F1013),
        bezel = Color(0xFF0A0B0D), bezelBorder = Color(0xFF3A3C46),
        bezelWordmark = "PSP", bezelWordmarkColor = Color(0xFFDADCE4),
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

    private val dark = ConsoleStyle(
        plateTop = Color(0xFF2E2D34), plateBottom = Color(0xFF18171C),
        bezel = Color(0xFF111115), bezelBorder = Color(0xFF2E2E36),
        bezelWordmark = "RETROHERO DECK", bezelWordmarkColor = Color(0xFFC4C3CE),
        ledColor = Color(0xFFFF5416), speaker = SpeakerStyle.DotMatrix,
        dpad = Color(0xFF1C1B20), arrow = Color(0xFF77767E),
        face = { Color(0xFF2A2930) },
        faceLabel = { Color(0xFFE2E1E8) },
        pill = Color(0xFF3A3940), pillLabel = Color(0xFFBDBCC4),
        shoulder = Color(0xFF3A3940), shoulderLabel = Color(0xFFE2E1E8),
        iconFill = Color(0xFF3A3940), iconTint = Color(0xFFE2E1E8),
    )

    /** Tactile hardware deck matching RetroHero's Cream Light / Obsidian Dark + Ember theme. */
    fun instrument(dark: Boolean, accent: Color, onAccent: Color = Color.White, systemName: String = "RETROHERO"): ConsoleStyle {
        val isPrimary = { label: String -> label == "A" || label == "○" || label == "I" || label == "FIRE" }
        return if (dark) {
            ConsoleStyle(
                plateTop = Color(0xFF1C1C20), plateBottom = Color(0xFF111114),
                bezel = Color(0xFF0B0B0D), bezelBorder = Color(0xFF28282E),
                bezelWordmark = systemName.uppercase(), bezelWordmarkColor = Color(0xFFA0A0A8),
                ledColor = accent, speaker = SpeakerStyle.DotMatrix,
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
                bezelWordmark = systemName.uppercase(), bezelWordmarkColor = Color(0xFFD2D0CA),
                ledColor = accent, speaker = SpeakerStyle.DotMatrix,
                dpad = Color(0xFF1C1C1F), arrow = Color(0xFF8A8A90),
                face = { if (isPrimary(it)) accent else Color(0xFFF8F7F4) },
                faceLabel = { if (isPrimary(it)) onAccent else Color(0xFF141416) },
                pill = Color(0xFFD8D6CF), pillLabel = Color(0xFF26262A),
                shoulder = Color(0xFFD8D6CF), shoulderLabel = Color(0xFF26262A),
                iconFill = Color(0xFFD8D6CF), iconTint = Color(0xFF141416),
            )
        }
    }

    fun forSystem(systemId: String, accent: Color): ConsoleStyle = when (systemId) {
        "nes" -> nes
        "gb" -> gameBoy
        "gbc" -> gbc
        "gba", "vb", "pokemini" -> gba
        "snes" -> snes
        "n64" -> n64
        "nds", "3ds" -> nds
        "genesis", "segacd", "32x" -> sega
        "sms", "gg", "sg1000" -> sega.copy(bezelWordmark = if (systemId == "gg") "SEGA GAME GEAR" else "SEGA MASTER")
        "psx", "ps2" -> playStation
        "psp" -> psp
        "saturn", "dreamcast" -> sega.copy(bezelWordmark = if (systemId == "dreamcast") "DREAMCAST" else "SEGA SATURN")
        "gc", "wii" -> gba.copy(bezelWordmark = if (systemId == "gc") "GAMECUBE" else "NINTENDO WII")
        "arcade" -> dark.copy(bezelWordmark = "ARCADE CABINET", face = { if (it in setOf("A", "B", "X", "Y")) accent else Color(0xFF2A2930) })
        else -> dark.copy(bezelWordmark = systemId.uppercase(), face = { accent })
    }
}
