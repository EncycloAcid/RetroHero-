package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color

/**
 * How a real console's controller looks, for the "Console" touch-button skin: the body plate,
 * D-pad, face buttons (colour, and whether the letter is printed on or below the button),
 * the rubber Select/Start pills and the shoulder buttons.
 */
data class ConsoleStyle(
    val plateTop: Color,
    val plateBottom: Color,
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
        plateTop = Color(0xFFD6D6D6), plateBottom = Color(0xFFAEAEAE),
        band = Color(0xFF1B1B1D),
        dpad = Color(0xFF232325), arrow = Color(0xFF6A6A6E),
        face = { Color(0xFFD0203C) },
        faceLabel = { null }, labelBelow = Color(0xFFD0203C),
        pill = Color(0xFF3A3A3D), pillLabel = Color(0xFFD0203C),
        iconFill = Color(0xFF3A3A3D), iconTint = Color(0xFFD6D6D6),
    )

    private val gameBoy = ConsoleStyle(
        plateTop = Color(0xFFDAD6CD), plateBottom = Color(0xFFBDB8AD),
        dpad = Color(0xFF2B2B30), arrow = Color(0xFF6E6E74),
        face = { Color(0xFFA2265B) },
        faceLabel = { null }, labelBelow = Color(0xFF25317C),
        pill = Color(0xFF8E8E96), pillLabel = Color(0xFF25317C), pillTilt = -25f,
        iconFill = Color(0xFF8E8E96), iconTint = Color(0xFF2B2B30),
    )

    private val gba = ConsoleStyle(
        plateTop = Color(0xFF5B50AB), plateBottom = Color(0xFF3B3180),
        dpad = Color(0xFF26242E), arrow = Color(0xFF6A6780),
        face = { Color(0xFFDCDCE4) },
        faceLabel = { Color(0xFF3B3180) },
        pill = Color(0xFF26242E), pillLabel = Color(0xFFD7D4F2),
        shoulder = Color(0xFFBDBBCE), shoulderLabel = Color(0xFF3B3180),
        iconFill = Color(0xFF26242E), iconTint = Color(0xFFD7D4F2),
    )

    private val snes = ConsoleStyle(
        plateTop = Color(0xFFD5D5DB), plateBottom = Color(0xFFB2B2BA),
        dpad = Color(0xFF2E2E33), arrow = Color(0xFF707076),
        face = {
            when (it) {
                "A" -> Color(0xFFD4262F)
                "B" -> Color(0xFFF0BF1A)
                "X" -> Color(0xFF2C52D6)
                "Y" -> Color(0xFF1F9C50)
                else -> Color(0xFF6A6A72)
            }
        },
        pill = Color(0xFF6A6A72), pillLabel = Color(0xFF3C3E92), pillTilt = -25f,
        shoulder = Color(0xFFA9A9B2), shoulderLabel = Color(0xFF45454D),
        iconFill = Color(0xFF8C8C95), iconTint = Color(0xFF2E2E33),
    )

    private val sega = ConsoleStyle(
        plateTop = Color(0xFF2C2C2F), plateBottom = Color(0xFF141416),
        dpad = Color(0xFF3B3B3F), arrow = Color(0xFF8C8C92),
        face = { Color(0xFF1C1C1F) },
        faceLabel = { Color(0xFFB8B8BE) },
        pill = Color(0xFF3B3B3F), pillLabel = Color(0xFFB8B8BE),
        shoulder = Color(0xFF3B3B3F), shoulderLabel = Color(0xFFB8B8BE),
        iconFill = Color(0xFF3B3B3F), iconTint = Color(0xFFB8B8BE),
    )

    private val playStation = ConsoleStyle(
        plateTop = Color(0xFFCBCCD0), plateBottom = Color(0xFFA6A7AC),
        dpad = Color(0xFFA2A3A8), arrow = Color(0xFF55565C),
        face = { Color(0xFF3A3A3F) },
        faceLabel = {
            when (it) {
                "△" -> Color(0xFF34C08A)
                "○" -> Color(0xFFEA4B51)
                "✕" -> Color(0xFF6F97EE)
                "□" -> Color(0xFFE584BF)
                else -> Color.White
            }
        },
        pill = Color(0xFF2B2B30), pillLabel = Color(0xFF45464C),
        shoulder = Color(0xFF9E9FA4), shoulderLabel = Color(0xFF34353A),
        iconFill = Color(0xFF8E8F95), iconTint = Color(0xFF2B2B30),
    )

    private val dark = ConsoleStyle(
        plateTop = Color(0xFF2E2D34), plateBottom = Color(0xFF18171C),
        dpad = Color(0xFF1C1B20), arrow = Color(0xFF77767E),
        face = { Color(0xFF2A2930) },
        faceLabel = { Color(0xFFE2E1E8) },
        pill = Color(0xFF3A3940), pillLabel = Color(0xFFBDBCC4),
        shoulder = Color(0xFF3A3940), shoulderLabel = Color(0xFFE2E1E8),
        iconFill = Color(0xFF3A3940), iconTint = Color(0xFFE2E1E8),
    )

    fun forSystem(systemId: String, accent: Color): ConsoleStyle = when (systemId) {
        "nes" -> nes
        "gb", "gbc" -> gameBoy
        "gba", "vb" -> gba
        "snes", "nds" -> snes
        "genesis", "segacd", "32x", "sms", "gg" -> sega
        "psx" -> playStation
        "arcade" -> dark.copy(face = { if (it in setOf("A", "B", "X", "Y")) accent else Color(0xFF2A2930) })
        else -> dark.copy(face = { accent })
    }
}
