package com.retrohero.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HeroPink = Color(0xFFFF4F8B)
val HeroViolet = Color(0xFF7C5CFF)
val HeroCyan = Color(0xFF35D0FF)

private val Colors = darkColorScheme(
    primary = HeroPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A1430),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = HeroViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E2266),
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = HeroCyan,
    background = Color(0xFF0E0E16),
    onBackground = Color(0xFFE8E6F0),
    surface = Color(0xFF14141F),
    onSurface = Color(0xFFE8E6F0),
    surfaceVariant = Color(0xFF24243A),
    onSurfaceVariant = Color(0xFFB9B6CC),
    surfaceContainer = Color(0xFF1A1A28),
    surfaceContainerHigh = Color(0xFF212133),
    surfaceContainerHighest = Color(0xFF29293E),
    outline = Color(0xFF4A4862),
)

@Composable
fun RetroHeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Typography(), content = content)
}
