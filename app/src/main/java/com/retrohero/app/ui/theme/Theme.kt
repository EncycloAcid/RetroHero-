package com.retrohero.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/*
 * Colour roles follow iOS's grouped-content palette, mapped onto Material roles:
 *   background             → systemGroupedBackground
 *   surfaceContainer       → secondarySystemGroupedBackground (cards, list rows)
 *   surfaceContainerHigh   → tertiary fill (search field, segmented track, gray buttons)
 *   surfaceContainerHighest→ quaternary fill / selected segment (dark)
 *   onSurfaceVariant       → secondaryLabel
 *   outlineVariant         → separator
 */
private val Dark = darkColorScheme(
    primary = Color(0xFFFF375F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1322),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF64D2FF),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF0B3A4D),
    onTertiaryContainer = Color(0xFFCDEFFF),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0F0F11),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    surfaceBright = Color(0xFF3A3A3C),
    outline = Color(0xFF545458),
    outlineVariant = Color(0xFF38383A),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
)

private val Light = lightColorScheme(
    primary = Color(0xFFFF2D55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF3F0014),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF32ADE6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDEFFF),
    onTertiaryContainer = Color(0xFF00344B),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEEEF0),
    surfaceContainerHighest = Color(0xFFE3E3E8),
    surfaceBright = Color.White,
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
)

private fun ui(size: TextUnit, weight: FontWeight, tracking: Double, line: TextUnit, family: FontFamily = Inter) =
    TextStyle(fontFamily = family, fontSize = size, fontWeight = weight, letterSpacing = tracking.sp, lineHeight = line)

/** iOS Dynamic Type sizes (Large Title → Caption 2) set in Inter. */
private val IosTypography = Typography(
    displayLarge = ui(56.sp, FontWeight.ExtraBold, -1.6, 60.sp, InterDisplay),
    displayMedium = ui(44.sp, FontWeight.ExtraBold, -1.2, 48.sp, InterDisplay),
    displaySmall = ui(34.sp, FontWeight.Bold, -0.9, 41.sp, InterDisplay), // Large Title
    headlineLarge = ui(28.sp, FontWeight.Bold, -0.7, 34.sp, InterDisplay), // Title 1
    headlineMedium = ui(22.sp, FontWeight.Bold, -0.5, 28.sp, InterDisplay), // Title 2
    headlineSmall = ui(20.sp, FontWeight.SemiBold, -0.4, 25.sp, InterDisplay), // Title 3
    titleLarge = ui(20.sp, FontWeight.SemiBold, -0.4, 25.sp),
    titleMedium = ui(17.sp, FontWeight.SemiBold, -0.4, 22.sp), // Headline
    titleSmall = ui(15.sp, FontWeight.SemiBold, -0.2, 20.sp),
    bodyLarge = ui(17.sp, FontWeight.Normal, -0.4, 22.sp), // Body
    bodyMedium = ui(15.sp, FontWeight.Normal, -0.2, 20.sp), // Subheadline
    bodySmall = ui(13.sp, FontWeight.Normal, -0.1, 18.sp), // Footnote
    labelLarge = ui(15.sp, FontWeight.SemiBold, -0.2, 20.sp),
    labelMedium = ui(12.sp, FontWeight.Medium, 0.0, 16.sp), // Caption 1
    labelSmall = ui(11.sp, FontWeight.Medium, 0.05, 13.sp), // Caption 2
)

@Composable
fun RetroHeroTheme(
    dynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) Dark else Light
    val context = LocalContext.current
    // Wallpaper colours only replace the accent roles; iOS-style neutrals stay put.
    val scheme: ColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val d = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        base.copy(
            primary = d.primary, onPrimary = d.onPrimary,
            primaryContainer = d.primaryContainer, onPrimaryContainer = d.onPrimaryContainer,
            secondary = d.secondary, onSecondary = d.onSecondary,
            tertiary = d.tertiary, onTertiary = d.onTertiary,
        )
    } else {
        base
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = IosTypography,
        content = content,
    )
}
