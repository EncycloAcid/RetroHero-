package com.retrohero.app.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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

private val accentState = androidx.compose.runtime.mutableStateOf(Color(0xFFFF6B2C))

/**
 * The accent colour (ember orange by default) used for the gacha pointer and small highlights.
 * It follows the chosen look; reading it in a composable recomposes when the look changes.
 */
val Salmon: Color get() = accentState.value

fun setAccent(color: Color) {
    accentState.value = color
}

/**
 * Dark-only palette on warm charcoal with an ember-orange accent; colour otherwise comes from
 * the games themselves (cover art and each system's colour).
 */
private val Mono = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C2C),
    onSecondaryContainer = Color.White,
    tertiary = Salmon,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3D2116),
    onTertiaryContainer = Color(0xFFFFDBCC),
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFDADA),
    background = Color(0xFF0E0E10),
    onBackground = Color.White,
    surface = Color(0xFF0E0E10),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFF9A9AA0),
    surfaceTint = Color(0xFF0E0E10),
    surfaceContainerLowest = Color(0xFF0A0A0B),
    surfaceContainerLow = Color(0xFF131315),
    surfaceContainer = Color(0xFF18181B),
    surfaceContainerHigh = Color(0xFF1F1F23),
    surfaceContainerHighest = Color(0xFF26262A),
    surfaceBright = Color(0xFF2A2A2A),
    outline = Color(0xFF77777D),
    outlineVariant = Color(0xFF232326),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
)

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
    displaySmallEmphasized = style(36.sp, 42.sp, FontWeight.Bold, InterDisplay),
    headlineLargeEmphasized = style(32.sp, 38.sp, FontWeight.Bold, InterDisplay),
    headlineMediumEmphasized = style(28.sp, 34.sp, FontWeight.Bold, InterDisplay),
    headlineSmallEmphasized = style(24.sp, 28.sp, FontWeight.Bold, InterDisplay),
    titleLargeEmphasized = style(22.sp, 27.sp, FontWeight.Bold),
    titleMediumEmphasized = style(16.sp, 19.sp, FontWeight.Bold),
    titleSmallEmphasized = style(14.sp, 17.sp, FontWeight.Bold),
    bodyLargeEmphasized = style(16.sp, 22.sp, FontWeight.SemiBold),
    bodyMediumEmphasized = style(14.sp, 19.sp, FontWeight.SemiBold),
    bodySmallEmphasized = style(12.sp, 15.sp, FontWeight.SemiBold),
    labelLargeEmphasized = style(14.sp, 18.sp, FontWeight.Bold),
    labelMediumEmphasized = style(12.sp, 16.sp, FontWeight.Bold),
    labelSmallEmphasized = style(11.sp, 14.sp, FontWeight.Bold),
)

/** RetroHero is dark-only; [dynamicColor] is accepted for compatibility but not used. */
@Composable
fun RetroHeroTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val accent = Salmon
    MaterialExpressiveTheme(
        colorScheme = Mono.copy(
            primary = if (accent.compositeOver(Color(0xFF0E0E10)).luminance() < 0.25f) lerp(accent, Color.White, 0.5f) else accent,
            onPrimary = Color.Black,
            primaryContainer = lerp(Mono.surfaceContainer, accent, 0.18f),
            tertiary = accent,
        ),
        motionScheme = MotionScheme.expressive(),
        typography = MonoTypography,
        content = content,
    )
}
