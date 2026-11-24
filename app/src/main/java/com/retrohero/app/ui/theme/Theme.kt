package com.retrohero.app.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val accentState = mutableStateOf(Color(0xFF5B8CFF))
private val darkState = mutableStateOf(true)

object Tone {
    val isDark: Boolean get() = darkState.value
    fun setDarkMode(dark: Boolean) { darkState.value = dark }

    val Bg: Color get() = if (isDark) Color(0xFF070A12) else Color(0xFFEEF2FA)
    val Surface: Color get() = if (isDark) Color(0xFF121828) else Color(0xFFFFFFFF)
    val Raised: Color get() = if (isDark) Color(0xFF1B233A) else Color(0xFFF6F8FE)
    val Ink: Color get() = if (isDark) Color(0xFFF4F7FF) else Color(0xFF0F1426)
    val Muted: Color get() = Ink.copy(alpha = if (isDark) 0.66f else 0.60f)
    val Faint: Color get() = Ink.copy(alpha = if (isDark) 0.36f else 0.34f)
    val Line: Color get() = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF1A2548).copy(alpha = 0.10f)
}

/**
 * The accent colour (Aurora Cobalt by default) used across the Midnight Glass UI.
 * It follows the chosen look; reading it in a composable recomposes when the look changes.
 */
val Salmon: Color get() = accentState.value

fun setAccent(color: Color) {
    accentState.value = color
}

private val Tight = (-0.035).em
private val Text = (-0.01).em

/** Headings (display font) are tracked tighter; running text stays close to normal for readability. */
private fun style(size: TextUnit, line: TextUnit, weight: FontWeight = FontWeight.SemiBold, family: FontFamily = Inter) =
    TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size,
        lineHeight = line,
        letterSpacing = if (family == InterDisplay) Tight else Text,
    )

/** Inter, SemiBold throughout with tight tracking; Bold for emphasized styles. */
private val MonoTypography = Typography(
    displayLarge = style(57.sp, 62.sp, family = InterDisplay),
    displayMedium = style(45.sp, 50.sp, family = InterDisplay),
    displaySmall = style(36.sp, 42.sp, family = InterDisplay),
    headlineLarge = style(32.sp, 38.sp, family = InterDisplay),
    headlineMedium = style(28.sp, 34.sp, family = InterDisplay),
    headlineSmall = style(24.sp, 28.sp, family = InterDisplay),
    titleLarge = style(22.sp, 27.sp),
    titleMedium = style(16.sp, 21.sp),
    titleSmall = style(14.sp, 17.sp),
    bodyLarge = style(16.sp, 22.sp, FontWeight.Normal),
    bodyMedium = style(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = style(14.sp, 18.sp),
    labelMedium = style(12.sp, 16.sp),
    labelSmall = style(11.sp, 14.sp),
    displayLargeEmphasized = style(57.sp, 62.sp, FontWeight.Bold, InterDisplay),
    displayMediumEmphasized = style(45.sp, 50.sp, FontWeight.Bold, InterDisplay),
    displaySmallEmphasized = style(34.sp, 38.sp, FontWeight.SemiBold, Inter).copy(letterSpacing = (-0.02).em),
    headlineLargeEmphasized = style(32.sp, 38.sp, FontWeight.SemiBold, InterDisplay),
    headlineMediumEmphasized = style(28.sp, 34.sp, FontWeight.SemiBold, InterDisplay),
    headlineSmallEmphasized = style(24.sp, 28.sp, FontWeight.SemiBold, InterDisplay),
    titleLargeEmphasized = style(17.sp, 22.sp, FontWeight.SemiBold).copy(letterSpacing = (-0.01).em),
    titleMediumEmphasized = style(16.sp, 19.sp, FontWeight.SemiBold),
    titleSmallEmphasized = style(14.sp, 17.sp, FontWeight.SemiBold),
    bodyLargeEmphasized = style(16.sp, 22.sp, FontWeight.SemiBold),
    bodyMediumEmphasized = style(14.sp, 19.sp, FontWeight.SemiBold),
    bodySmallEmphasized = style(12.sp, 15.sp, FontWeight.SemiBold),
    labelLargeEmphasized = style(14.sp, 18.sp, FontWeight.SemiBold),
    labelMediumEmphasized = style(12.sp, 16.sp, FontWeight.SemiBold),
    labelSmallEmphasized = style(11.sp, 14.sp, FontWeight.SemiBold),
)

/** [dynamicColor] is accepted for compatibility but the Instrument palette is fixed. */
@Composable
fun RetroHeroTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = Salmon
    val drawnAccent = accent.compositeOver(Tone.Bg)
    val primary = when {
        Tone.isDark && drawnAccent.luminance() < 0.25f -> lerp(accent, Color.White, 0.5f)
        !Tone.isDark && drawnAccent.luminance() > 0.6f -> lerp(accent, Color.Black, 0.35f)
        else -> accent
    }
    val onPrimary = if (primary.luminance() > 0.18f) Color(0xFF111111) else Color.White
    val onTertiary = if (drawnAccent.luminance() > 0.18f) Color(0xFF111111) else Color.White
    val primaryContainer = primary.copy(alpha = 0.18f).compositeOver(Tone.Surface)
    val colorScheme = if (Tone.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Tone.Ink,
            secondary = Tone.Muted,
            onSecondary = Tone.Bg,
            secondaryContainer = Tone.Raised,
            onSecondaryContainer = Tone.Ink,
            tertiary = accent,
            onTertiary = onTertiary,
            tertiaryContainer = primaryContainer,
            onTertiaryContainer = Tone.Ink,
            background = Tone.Bg,
            onBackground = Tone.Ink,
            surface = Tone.Surface,
            onSurface = Tone.Ink,
            surfaceVariant = Tone.Raised,
            onSurfaceVariant = Tone.Muted,
            surfaceTint = Tone.Surface,
            surfaceContainerLowest = Tone.Bg,
            surfaceContainerLow = Tone.Surface,
            surfaceContainer = Tone.Surface,
            surfaceContainerHigh = Tone.Raised,
            surfaceContainerHighest = Tone.Raised,
            surfaceBright = Tone.Raised,
            outline = Tone.Muted,
            outlineVariant = Tone.Line,
            inverseSurface = Tone.Ink,
            inverseOnSurface = Tone.Bg,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Tone.Ink,
            secondary = Tone.Muted,
            onSecondary = Tone.Bg,
            secondaryContainer = Tone.Raised,
            onSecondaryContainer = Tone.Ink,
            tertiary = accent,
            onTertiary = onTertiary,
            tertiaryContainer = primaryContainer,
            onTertiaryContainer = Tone.Ink,
            background = Tone.Bg,
            onBackground = Tone.Ink,
            surface = Tone.Surface,
            onSurface = Tone.Ink,
            surfaceVariant = Tone.Raised,
            onSurfaceVariant = Tone.Muted,
            surfaceTint = Tone.Surface,
            surfaceContainerLowest = Tone.Bg,
            surfaceContainerLow = Tone.Surface,
            surfaceContainer = Tone.Surface,
            surfaceContainerHigh = Tone.Raised,
            surfaceContainerHighest = Tone.Raised,
            surfaceBright = Tone.Raised,
            outline = Tone.Muted,
            outlineVariant = Tone.Line,
            inverseSurface = Tone.Ink,
            inverseOnSurface = Tone.Bg,
        )
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = MonoTypography,
        content = content,
    )
}
