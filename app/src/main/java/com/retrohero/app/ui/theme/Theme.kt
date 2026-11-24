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

val ElectricLimeColor = Color(0xFFD7FA3C)
val MonoAccentColor = Color(0xFFEAEAEA)
private val accentState = mutableStateOf(ElectricLimeColor)
private val darkState = mutableStateOf(true)

object Tone {
    val isDark: Boolean get() = darkState.value
    fun setDarkMode(dark: Boolean) { darkState.value = dark }

    val Bg: Color get() = if (isDark) Color(0xFF0B0C0B) else Color(0xFFF6F7F4)
    val Surface: Color get() = if (isDark) Color(0xFF141614) else Color(0xFFFFFFFF)
    val Raised: Color get() = if (isDark) Color(0xFF1C1F1C) else Color(0xFFECEEE9)
    val Ink: Color get() = if (isDark) Color(0xFFF3F5F1) else Color(0xFF111311)
    val Muted: Color get() = if (isDark) Color(0xFF787D76) else Color(0xFF646962)
    val Faint: Color get() = if (isDark) Color(0xFF535852) else Color(0xFF9BA099)
    val Line: Color get() = if (isDark) Color(0xFF1D201D) else Color(0xFFE1E3DD)
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
        letterSpacing = (-0.01).em,
    )

private val BaseTypography = Typography(
    headlineMedium = style(28.sp, 33.sp, FontWeight.SemiBold),
    titleLarge = style(20.sp, 25.sp, FontWeight.SemiBold),
    titleMedium = style(15.sp, 20.sp, FontWeight.Medium),
    titleSmall = style(14.sp, 18.sp, FontWeight.Medium),
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
    val onPrimary = if (primary.luminance() > 0.35f) Color(0xFF0B0C0B) else Color.White
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
