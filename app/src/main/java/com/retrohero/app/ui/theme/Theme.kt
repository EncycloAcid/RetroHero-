package com.retrohero.app.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val EmberOrangeColor = Color(0xFFFF6B24)
val ElectricLimeColor = EmberOrangeColor
val MonoAccentColor = Color(0xFFEAEAEA)
private val accentState = mutableStateOf(EmberOrangeColor)
private val darkState = mutableStateOf(true)

object Tone {
    val isDark: Boolean get() = darkState.value
    fun setDarkMode(dark: Boolean) { darkState.value = dark }

    val Bg: Color get() = if (isDark) Color(0xFF0A0A0C) else Color(0xFFF8F8FA)
    val Surface: Color get() = if (isDark) Color(0xFF121215) else Color(0xFFFFFFFF)
    val Raised: Color get() = if (isDark) Color(0xFF1A1A1F) else Color(0xFFEEEEF2)
    val Ink: Color get() = if (isDark) Color(0xFFF7F7F8) else Color(0xFF111114)
    val Muted: Color get() = if (isDark) Color(0xFF7D7D87) else Color(0xFF666670)
    val Faint: Color get() = if (isDark) Color(0xFF4E4E57) else Color(0xFF9C9CA6)
    val Line: Color get() = if (isDark) Color(0xFF19191F) else Color(0xFFE4E4EA)
}

val Salmon: Color get() = accentState.value

fun setAccent(color: Color) {
    accentState.value = color
}

private fun style(size: TextUnit, line: TextUnit, weight: FontWeight = FontWeight.Normal, family: FontFamily = Inter) =
    TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size,
        lineHeight = line,
        letterSpacing = (-0.015).em,
    )

private val BaseTypography = Typography(
    headlineMedium = style(30.sp, 35.sp, FontWeight.Bold),
    titleLarge = style(18.sp, 23.sp, FontWeight.Bold),
    titleMedium = style(15.sp, 20.sp, FontWeight.SemiBold),
    titleSmall = style(14.sp, 18.sp, FontWeight.SemiBold),
    bodyLarge = style(15.sp, 21.sp, FontWeight.Normal),
    bodyMedium = style(13.5.sp, 19.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = style(14.sp, 18.sp, FontWeight.SemiBold),
    labelMedium = style(12.sp, 16.sp, FontWeight.Medium),
    labelSmall = style(11.sp, 14.sp, FontWeight.Medium),
)

@Composable
fun RetroHeroTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val primary = Salmon
    val onPrimary = if (primary.luminance() > 0.52f) Color(0xFF0A0A0C) else Color.White
    val scheme = if (Tone.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            background = Tone.Bg,
            onBackground = Tone.Ink,
            surface = Tone.Surface,
            onSurface = Tone.Ink,
            surfaceVariant = Tone.Raised,
            onSurfaceVariant = Tone.Muted,
            surfaceContainerHigh = Tone.Raised,
            outline = Tone.Muted,
            outlineVariant = Tone.Line,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            background = Tone.Bg,
            onBackground = Tone.Ink,
            surface = Tone.Surface,
            onSurface = Tone.Ink,
            surfaceVariant = Tone.Raised,
            onSurfaceVariant = Tone.Muted,
            surfaceContainerHigh = Tone.Raised,
            outline = Tone.Muted,
            outlineVariant = Tone.Line,
        )
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.standard(),
        typography = BaseTypography,
        content = content,
    )
}
