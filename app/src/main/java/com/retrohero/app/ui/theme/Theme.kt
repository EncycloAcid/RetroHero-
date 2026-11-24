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
import androidx.compose.ui.unit.sp

val MonoAccentColor = Color(0xFFEAEAEA)
private val accentState = mutableStateOf(MonoAccentColor)
private val darkState = mutableStateOf(true)

object Tone {
    val isDark: Boolean get() = darkState.value
    fun setDarkMode(dark: Boolean) { darkState.value = dark }

    val Bg: Color get() = if (isDark) Color(0xFF121212) else Color(0xFFFAFAFA)
    val Surface: Color get() = if (isDark) Color(0xFF1C1C1C) else Color(0xFFFFFFFF)
    val Raised: Color get() = if (isDark) Color(0xFF242424) else Color(0xFFF0F0F0)
    val Ink: Color get() = if (isDark) Color(0xFFEEEEEE) else Color(0xFF141414)
    val Muted: Color get() = if (isDark) Color(0xFF8E8E8E) else Color(0xFF666666)
    val Faint: Color get() = if (isDark) Color(0xFF555555) else Color(0xFF999999)
    val Line: Color get() = if (isDark) Color(0xFF262626) else Color(0xFFE4E4E4)
}

val Salmon: Color get() = accentState.value

fun setAccent(color: Color) {
    accentState.value = color
}

private fun style(size: TextUnit, line: TextUnit, weight: FontWeight = FontWeight.Normal, family: FontFamily = Inter) =
    TextStyle(fontFamily = family, fontWeight = weight, fontSize = size, lineHeight = line)

private val BaseTypography = Typography(
    titleLarge = style(20.sp, 26.sp, FontWeight.Medium),
    titleMedium = style(16.sp, 22.sp, FontWeight.Medium),
    titleSmall = style(14.sp, 19.sp, FontWeight.Medium),
    bodyLarge = style(16.sp, 22.sp, FontWeight.Normal),
    bodyMedium = style(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = style(14.sp, 18.sp, FontWeight.Medium),
    labelMedium = style(12.sp, 16.sp, FontWeight.Medium),
    labelSmall = style(11.sp, 14.sp, FontWeight.Medium),
)

@Composable
fun RetroHeroTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val primary = Salmon
    val onPrimary = if (primary.luminance() > 0.4f) Color(0xFF121212) else Color.White
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
