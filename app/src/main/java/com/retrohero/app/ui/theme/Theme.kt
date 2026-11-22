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

/** Bolder, tighter display and headline styles for an expressive look. */
private val ExpressiveTypography = Typography(
    displayLarge = Base.displayLarge.heavy(),
    displayMedium = Base.displayMedium.heavy(),
    displaySmall = Base.displaySmall.heavy(),
    headlineLarge = Base.headlineLarge.heavy(),
    headlineMedium = Base.headlineMedium.heavy(),
    headlineSmall = Base.headlineSmall.heavy(),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)

private fun TextStyle.heavy() = copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)

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
