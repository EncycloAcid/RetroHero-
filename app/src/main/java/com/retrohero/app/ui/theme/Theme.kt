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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkBrand = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7C2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFC9BFFF),
    onSecondary = Color(0xFF31236F),
    secondaryContainer = Color(0xFF483B87),
    onSecondaryContainer = Color(0xFFE6DEFF),
    tertiary = Color(0xFF7FD0FF),
    onTertiary = Color(0xFF00344B),
    tertiaryContainer = Color(0xFF004C6B),
    onTertiaryContainer = Color(0xFFC6E7FF),
    background = Color(0xFF15111A),
    onBackground = Color(0xFFE9E0E8),
    surface = Color(0xFF15111A),
    onSurface = Color(0xFFE9E0E8),
    surfaceVariant = Color(0xFF4E4350),
    onSurfaceVariant = Color(0xFFD2C2D0),
    surfaceContainerLowest = Color(0xFF100C14),
    surfaceContainerLow = Color(0xFF1D1922),
    surfaceContainer = Color(0xFF221D26),
    surfaceContainerHigh = Color(0xFF2C2731),
    surfaceContainerHighest = Color(0xFF37323C),
    outline = Color(0xFF9B8D9A),
    outlineVariant = Color(0xFF4E4350),
)

private val LightBrand = lightColorScheme(
    primary = Color(0xFFA0305A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E0020),
    secondary = Color(0xFF5F52A0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF1B0C59),
    tertiary = Color(0xFF00658E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC6E7FF),
    onTertiaryContainer = Color(0xFF001E2E),
    background = Color(0xFFFFF8F9),
    onBackground = Color(0xFF1F1A1F),
    surface = Color(0xFFFFF8F9),
    onSurface = Color(0xFF1F1A1F),
    surfaceVariant = Color(0xFFEFDEEB),
    onSurfaceVariant = Color(0xFF4E4350),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF1F6),
    surfaceContainer = Color(0xFFF5EBF0),
    surfaceContainerHigh = Color(0xFFEFE5EA),
    surfaceContainerHighest = Color(0xFFE9DFE5),
    outline = Color(0xFF7F7381),
    outlineVariant = Color(0xFFD2C2D0),
)

private val Base = Typography()

private fun TextStyle.text(weight: FontWeight = fontWeight ?: FontWeight.Normal) =
    copy(fontFamily = GoogleSans, fontWeight = weight)

private fun TextStyle.display(weight: FontWeight, tracking: Double) =
    copy(fontFamily = GoogleSansDisplay, fontWeight = weight, letterSpacing = tracking.sp)

/**
 * Material 3 Expressive type scale in Google Sans Flex. Display/headline styles use the fully
 * rounded display cut; the *Emphasized* variants are heavier for hero moments.
 */
private val ExpressiveTypography = Typography(
    displayLarge = Base.displayLarge.display(FontWeight.Medium, -0.5),
    displayMedium = Base.displayMedium.display(FontWeight.Medium, -0.25),
    displaySmall = Base.displaySmall.display(FontWeight.Medium, 0.0),
    headlineLarge = Base.headlineLarge.display(FontWeight.Medium, 0.0),
    headlineMedium = Base.headlineMedium.display(FontWeight.Medium, 0.0),
    headlineSmall = Base.headlineSmall.display(FontWeight.Medium, 0.0),
    titleLarge = Base.titleLarge.display(FontWeight.Medium, 0.0),
    titleMedium = Base.titleMedium.text(FontWeight.SemiBold),
    titleSmall = Base.titleSmall.text(FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.text(),
    bodyMedium = Base.bodyMedium.text(),
    bodySmall = Base.bodySmall.text(),
    labelLarge = Base.labelLarge.text(FontWeight.SemiBold),
    labelMedium = Base.labelMedium.text(FontWeight.SemiBold),
    labelSmall = Base.labelSmall.text(FontWeight.SemiBold),
    displayLargeEmphasized = Base.displayLarge.display(FontWeight.ExtraBold, -0.5),
    displayMediumEmphasized = Base.displayMedium.display(FontWeight.ExtraBold, -0.25),
    displaySmallEmphasized = Base.displaySmall.display(FontWeight.ExtraBold, 0.0),
    headlineLargeEmphasized = Base.headlineLarge.display(FontWeight.Bold, 0.0),
    headlineMediumEmphasized = Base.headlineMedium.display(FontWeight.Bold, 0.0),
    headlineSmallEmphasized = Base.headlineSmall.display(FontWeight.Bold, 0.0),
    titleLargeEmphasized = Base.titleLarge.display(FontWeight.Bold, 0.0),
    titleMediumEmphasized = Base.titleMedium.text(FontWeight.Bold),
    titleSmallEmphasized = Base.titleSmall.text(FontWeight.Bold),
    bodyLargeEmphasized = Base.bodyLarge.text(FontWeight.SemiBold),
    bodyMediumEmphasized = Base.bodyMedium.text(FontWeight.SemiBold),
    bodySmallEmphasized = Base.bodySmall.text(FontWeight.SemiBold),
    labelLargeEmphasized = Base.labelLarge.text(FontWeight.Bold),
    labelMediumEmphasized = Base.labelMedium.text(FontWeight.Bold),
    labelSmallEmphasized = Base.labelSmall.text(FontWeight.Bold),
)

@Composable
fun RetroHeroTheme(
    dynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkBrand
        else -> LightBrand
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = ExpressiveTypography,
        content = content,
    )
}
