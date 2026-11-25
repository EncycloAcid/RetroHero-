package com.retrohero.app.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val ExpressiveIrisColor = Color(0xFF6D67FF)
val IosBlueColor = Color(0xFF0A84FF)
val ExpressiveCoralColor = Color(0xFFFF5A5F)
val ExpressiveMintColor = Color(0xFF25C990)
val EmberOrangeColor = Color(0xFFFF7A2F)
val ExpressiveOrchidColor = Color(0xFFE052B8)

// Compatibility aliases kept for emulator components
val ElectricLimeColor = ExpressiveIrisColor
val MonoAccentColor = Color(0xFFEAEAEA)

private val accentState = mutableStateOf(ExpressiveIrisColor)
private val darkState = mutableStateOf(true)

data class AccentSwatch(val id: String, val name: String, val color: Color)

val ExpressiveAccents = listOf(
    AccentSwatch("iris", "Iris", ExpressiveIrisColor),
    AccentSwatch("blue", "iOS Blue", IosBlueColor),
    AccentSwatch("coral", "Coral", ExpressiveCoralColor),
    AccentSwatch("mint", "Mint", ExpressiveMintColor),
    AccentSwatch("ember", "Tangerine", EmberOrangeColor),
    AccentSwatch("orchid", "Orchid", ExpressiveOrchidColor),
)

fun resolveExpressiveAccent(id: String): Color =
    ExpressiveAccents.firstOrNull { it.id == id }?.color ?: ExpressiveIrisColor

object Tone {
    val isDark: Boolean get() = darkState.value
    fun setDarkMode(dark: Boolean) { darkState.value = dark }

    // Lush M3 Expressive × iOS surface ladder
    val Bg: Color get() = if (isDark) Color(0xFF0A0B11) else Color(0xFFF2F3F8)
    val Surface: Color get() = if (isDark) Color(0xFF141621) else Color(0xFFFFFFFF)
    val Raised: Color get() = if (isDark) Color(0xFF1D2030) else Color(0xFFE9ECF5)
    val Glass: Color get() = if (isDark) Color(0xFF161824).copy(alpha = 0.90f) else Color(0xFFFFFFFF).copy(alpha = 0.92f)
    val Ink: Color get() = if (isDark) Color(0xFFF6F7FC) else Color(0xFF11131C)
    val Muted: Color get() = if (isDark) Color(0xFF8C92A8) else Color(0xFF63687D)
    val Faint: Color get() = if (isDark) Color(0xFF565C72) else Color(0xFF9AA0B4)
    val Line: Color get() = if (isDark) Color(0xFF24283B) else Color(0xFFE0E3EF)

    /** Top-lit iOS Liquid Glass specular rim gradient. */
    val glassBorder: Brush
        get() = Brush.verticalGradient(
            if (isDark) {
                listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f))
            } else {
                listOf(Color.White.copy(alpha = 0.95f), Color(0xFFD5DAE8), Color(0xFFE4E7F2))
            },
        )

    /** Soft M3 Expressive tonal container wash derived from an accent color. */
    fun tonalSurface(accent: Color): Color =
        accent.copy(alpha = if (isDark) 0.16f else 0.12f)
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
        letterSpacing = (-0.02).em,
    )

private val ExpressiveTypography = Typography(
    displaySmall = style(34.sp, 38.sp, FontWeight.Black),
    headlineMedium = style(28.sp, 33.sp, FontWeight.ExtraBold),
    titleLarge = style(20.sp, 25.sp, FontWeight.Bold),
    titleMedium = style(16.sp, 21.sp, FontWeight.SemiBold),
    titleSmall = style(14.sp, 18.sp, FontWeight.SemiBold),
    bodyLarge = style(15.sp, 21.sp, FontWeight.Normal),
    bodyMedium = style(13.5.sp, 19.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = style(14.sp, 18.sp, FontWeight.SemiBold),
    labelMedium = style(12.sp, 16.sp, FontWeight.SemiBold),
    labelSmall = style(11.sp, 14.sp, FontWeight.SemiBold),
)

@Composable
fun RetroHeroTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val primary = Salmon
    val onPrimary = if (primary.luminance() > 0.56f) Color(0xFF0A0B11) else Color.White
    val scheme = if (Tone.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
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
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF11131C),
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
        motionScheme = MotionScheme.expressive(),
        typography = ExpressiveTypography,
        content = content,
    )
}
