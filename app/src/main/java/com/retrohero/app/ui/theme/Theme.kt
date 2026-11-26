package com.retrohero.app.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Curated Material 3 Expressive tonal seed palettes (each with full primary/secondary/tertiary & tinted surfaces). */
data class M3ExpressivePalette(
    val id: String,
    val name: String,
    val seedPrimary: Color,
    val seedSecondary: Color,
    val seedTertiary: Color,
    val dark: ColorScheme,
    val light: ColorScheme,
)

private val BerryVioletPalette = M3ExpressivePalette(
    id = "berry",
    name = "Berry",
    seedPrimary = Color(0xFFD0BCFF),
    seedSecondary = Color(0xFFCCC2DC),
    seedTertiary = Color(0xFFEFB8C8),
    dark = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE6E0E9),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceContainerLowest = Color(0xFF0F0D13),
        surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
    ),
    light = lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        background = Color(0xFFFEF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
    ),
)

private val PixelSagePalette = M3ExpressivePalette(
    id = "sage",
    name = "Sage",
    seedPrimary = Color(0xFF9CD67D),
    seedSecondary = Color(0xFFBCCBB0),
    seedTertiary = Color(0xFFA0CFD0),
    dark = darkColorScheme(
        primary = Color(0xFF9CD67D),
        onPrimary = Color(0xFF153800),
        primaryContainer = Color(0xFF25500D),
        onPrimaryContainer = Color(0xFFB7F397),
        secondary = Color(0xFFBCCBB0),
        onSecondary = Color(0xFF273420),
        secondaryContainer = Color(0xFF3D4B35),
        onSecondaryContainer = Color(0xFFD8E7CB),
        tertiary = Color(0xFFA0CFD0),
        onTertiary = Color(0xFF003738),
        tertiaryContainer = Color(0xFF1E4E4F),
        onTertiaryContainer = Color(0xFFBBEBEC),
        background = Color(0xFF11140F),
        onBackground = Color(0xFFE1E4DA),
        surface = Color(0xFF11140F),
        onSurface = Color(0xFFE1E4DA),
        surfaceVariant = Color(0xFF43483E),
        onSurfaceVariant = Color(0xFFC3C8BC),
        surfaceContainerLowest = Color(0xFF0C0F0A),
        surfaceContainerLow = Color(0xFF191D16),
        surfaceContainer = Color(0xFF1D211A),
        surfaceContainerHigh = Color(0xFF272B24),
        surfaceContainerHighest = Color(0xFF32362E),
        outline = Color(0xFF8D9287),
        outlineVariant = Color(0xFF43483E),
    ),
    light = lightColorScheme(
        primary = Color(0xFF386A20),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB7F397),
        onPrimaryContainer = Color(0xFF072100),
        secondary = Color(0xFF55624C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E7CB),
        onSecondaryContainer = Color(0xFF131F0D),
        tertiary = Color(0xFF386667),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBBEBEC),
        onTertiaryContainer = Color(0xFF002021),
        background = Color(0xFFF8FAF0),
        onBackground = Color(0xFF191D16),
        surface = Color(0xFFF8FAF0),
        onSurface = Color(0xFF191D16),
        surfaceVariant = Color(0xFFE0E4D7),
        onSurfaceVariant = Color(0xFF43483E),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF2F5EA),
        surfaceContainer = Color(0xFFECEFE4),
        surfaceContainerHigh = Color(0xFFE6E9DF),
        surfaceContainerHighest = Color(0xFFE1E4DA),
        outline = Color(0xFF74796D),
        outlineVariant = Color(0xFFC3C8BC),
    ),
)

private val TerracottaPalette = M3ExpressivePalette(
    id = "terracotta",
    name = "Clay",
    seedPrimary = Color(0xFFFFB59D),
    seedSecondary = Color(0xFFE7BDB0),
    seedTertiary = Color(0xFFD7C58D),
    dark = darkColorScheme(
        primary = Color(0xFFFFB59D),
        onPrimary = Color(0xFF551F0E),
        primaryContainer = Color(0xFF723522),
        onPrimaryContainer = Color(0xFFFFDBD0),
        secondary = Color(0xFFE7BDB0),
        onSecondary = Color(0xFF442A21),
        secondaryContainer = Color(0xFF5D4036),
        onSecondaryContainer = Color(0xFFFFDBD0),
        tertiary = Color(0xFFD7C58D),
        onTertiary = Color(0xFF3A2F05),
        tertiaryContainer = Color(0xFF52461A),
        onTertiaryContainer = Color(0xFFF4E1A7),
        background = Color(0xFF1A110F),
        onBackground = Color(0xFFF1DFDA),
        surface = Color(0xFF1A110F),
        onSurface = Color(0xFFF1DFDA),
        surfaceVariant = Color(0xFF53433F),
        onSurfaceVariant = Color(0xFFD8C2BC),
        surfaceContainerLowest = Color(0xFF140C0A),
        surfaceContainerLow = Color(0xFF231917),
        surfaceContainer = Color(0xFF271D1A),
        surfaceContainerHigh = Color(0xFF322825),
        surfaceContainerHighest = Color(0xFF3D322F),
        outline = Color(0xFFA08D87),
        outlineVariant = Color(0xFF53433F),
    ),
    light = lightColorScheme(
        primary = Color(0xFF8F4B37),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBD0),
        onPrimaryContainer = Color(0xFF3A0B01),
        secondary = Color(0xFF77574D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDBD0),
        onSecondaryContainer = Color(0xFF2C150E),
        tertiary = Color(0xFF6B5D2F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF4E1A7),
        onTertiaryContainer = Color(0xFF231B00),
        background = Color(0xFFFFF8F6),
        onBackground = Color(0xFF231917),
        surface = Color(0xFFFFF8F6),
        onSurface = Color(0xFF231917),
        surfaceVariant = Color(0xFFF5DED7),
        onSurfaceVariant = Color(0xFF53433F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFF0EC),
        surfaceContainer = Color(0xFFFCEAE4),
        surfaceContainerHigh = Color(0xFFF6E4DF),
        surfaceContainerHighest = Color(0xFFF1DFDA),
        outline = Color(0xFF85736E),
        outlineVariant = Color(0xFFD8C2BC),
    ),
)

private val CobaltSkyPalette = M3ExpressivePalette(
    id = "cobalt",
    name = "Cobalt",
    seedPrimary = Color(0xFFA8C7FA),
    seedSecondary = Color(0xFFBDC7DC),
    seedTertiary = Color(0xFFDCBCE1),
    dark = darkColorScheme(
        primary = Color(0xFFA8C7FA),
        onPrimary = Color(0xFF062E6F),
        primaryContainer = Color(0xFF1F4588),
        onPrimaryContainer = Color(0xFFD3E3FD),
        secondary = Color(0xFFBDC7DC),
        onSecondary = Color(0xFF283141),
        secondaryContainer = Color(0xFF3E4758),
        onSecondaryContainer = Color(0xFFD9E3F8),
        tertiary = Color(0xFFDCBCE1),
        onTertiary = Color(0xFF3E2845),
        tertiaryContainer = Color(0xFF563E5D),
        onTertiaryContainer = Color(0xFFFAD8FE),
        background = Color(0xFF111318),
        onBackground = Color(0xFFE2E2E9),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFE2E2E9),
        surfaceVariant = Color(0xFF44474F),
        onSurfaceVariant = Color(0xFFC4C6D0),
        surfaceContainerLowest = Color(0xFF0C0E13),
        surfaceContainerLow = Color(0xFF191C20),
        surfaceContainer = Color(0xFF1D2024),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474F),
    ),
    light = lightColorScheme(
        primary = Color(0xFF355CA8),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3E3FD),
        onPrimaryContainer = Color(0xFF001A41),
        secondary = Color(0xFF565F71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD9E3F8),
        onSecondaryContainer = Color(0xFF131C2B),
        tertiary = Color(0xFF6F5575),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFAD8FE),
        onTertiaryContainer = Color(0xFF28132F),
        background = Color(0xFFF9F9FF),
        onBackground = Color(0xFF191C20),
        surface = Color(0xFFF9F9FF),
        onSurface = Color(0xFF191C20),
        surfaceVariant = Color(0xFFE0E2EC),
        onSurfaceVariant = Color(0xFF44474F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF3F3FA),
        surfaceContainer = Color(0xFFEDEDF4),
        surfaceContainerHigh = Color(0xFFE7E8EE),
        surfaceContainerHighest = Color(0xFFE2E2E9),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C6D0),
    ),
)

private val RoseBerryPalette = M3ExpressivePalette(
    id = "rose",
    name = "Rose",
    seedPrimary = Color(0xFFFFB0CA),
    seedSecondary = Color(0xFFE3BDC6),
    seedTertiary = Color(0xFFEFBD94),
    dark = darkColorScheme(
        primary = Color(0xFFFFB0CA),
        onPrimary = Color(0xFF541D35),
        primaryContainer = Color(0xFF70334C),
        onPrimaryContainer = Color(0xFFFFD9E3),
        secondary = Color(0xFFE3BDC6),
        onSecondary = Color(0xFF422931),
        secondaryContainer = Color(0xFF5B3F47),
        onSecondaryContainer = Color(0xFFFFD9E3),
        tertiary = Color(0xFFEFBD94),
        onTertiary = Color(0xFF48290C),
        tertiaryContainer = Color(0xFF623F20),
        onTertiaryContainer = Color(0xFFFFDCC1),
        background = Color(0xFF191114),
        onBackground = Color(0xFFEFDFE2),
        surface = Color(0xFF191114),
        onSurface = Color(0xFFEFDFE2),
        surfaceVariant = Color(0xFF514347),
        onSurfaceVariant = Color(0xFFD5C2C6),
        surfaceContainerLowest = Color(0xFF140C0E),
        surfaceContainerLow = Color(0xFF22191C),
        surfaceContainer = Color(0xFF261D20),
        surfaceContainerHigh = Color(0xFF31282A),
        surfaceContainerHighest = Color(0xFF3C3235),
        outline = Color(0xFF9E8C90),
        outlineVariant = Color(0xFF514347),
    ),
    light = lightColorScheme(
        primary = Color(0xFF8C4A64),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E3),
        onPrimaryContainer = Color(0xFF3A0720),
        secondary = Color(0xFF74565F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD9E3),
        onSecondaryContainer = Color(0xFF2B151C),
        tertiary = Color(0xFF7C5635),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCC1),
        onTertiaryContainer = Color(0xFF2E1500),
        background = Color(0xFFFFF8F8),
        onBackground = Color(0xFF22191C),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF22191C),
        surfaceVariant = Color(0xFFF2DDE2),
        onSurfaceVariant = Color(0xFF514347),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFCF0F3),
        surfaceContainer = Color(0xFFF6EAED),
        surfaceContainerHigh = Color(0xFFF1E5E8),
        surfaceContainerHighest = Color(0xFFEFDFE2),
        outline = Color(0xFF837377),
        outlineVariant = Color(0xFFD5C2C6),
    ),
)

val M3ExpressivePalettes = listOf(
    BerryVioletPalette,
    PixelSagePalette,
    TerracottaPalette,
    CobaltSkyPalette,
    RoseBerryPalette,
)

private val paletteState = mutableStateOf(BerryVioletPalette)
private val darkState = mutableStateOf(true)
private val dynamicColorState = mutableStateOf(false)

fun setExpressivePalette(id: String) {
    paletteState.value = M3ExpressivePalettes.firstOrNull { it.id == id } ?: BerryVioletPalette
}

fun currentPaletteId(): String = paletteState.value.id

fun setDynamicColorEnabled(enabled: Boolean) {
    dynamicColorState.value = enabled
}

// Legacy compatibility bridge for EmulatorActivity / VirtualGamepad / HandheldShell
val EmberOrangeColor = BerryVioletPalette.seedPrimary
val ElectricLimeColor = PixelSagePalette.seedPrimary
val MonoAccentColor = Color(0xFFEAEAEA)

private val accentOverrideState = mutableStateOf<Color?>(null)

val Salmon: Color
    get() = accentOverrideState.value ?: (if (Tone.isDark) paletteState.value.dark.primary else paletteState.value.light.primary)

fun setAccent(color: Color) {
    val matched = M3ExpressivePalettes.firstOrNull { it.seedPrimary == color || it.dark.primary == color || it.light.primary == color }
    if (matched != null) {
        paletteState.value = matched
        accentOverrideState.value = null
    } else {
        accentOverrideState.value = color
    }
}

object Tone {
    val isDark: Boolean get() = darkState.value
    fun setDarkMode(dark: Boolean) { darkState.value = dark }

    private val activeScheme: ColorScheme
        get() = if (isDark) paletteState.value.dark else paletteState.value.light

    val Bg: Color get() = activeScheme.background
    val Surface: Color get() = activeScheme.surfaceContainerLow
    val Raised: Color get() = activeScheme.surfaceContainerHigh
    val Ink: Color get() = activeScheme.onSurface
    val Muted: Color get() = activeScheme.onSurfaceVariant
    val Faint: Color get() = activeScheme.outline
    val Line: Color get() = activeScheme.outlineVariant
}

private fun style(
    size: TextUnit,
    line: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    tracking: Float = -0.02f,
    family: FontFamily = Inter,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = tracking.em,
)

private val ExpressiveTypography = Typography(
    displayLarge = style(44.sp, 48.sp, FontWeight.Black, -0.04f),
    displayMedium = style(36.sp, 40.sp, FontWeight.Black, -0.035f),
    displaySmall = style(32.sp, 36.sp, FontWeight.ExtraBold, -0.03f),
    headlineMedium = style(26.sp, 31.sp, FontWeight.ExtraBold, -0.025f),
    headlineSmall = style(22.sp, 27.sp, FontWeight.Bold, -0.02f),
    titleLarge = style(20.sp, 25.sp, FontWeight.Bold, -0.015f),
    titleMedium = style(16.sp, 21.sp, FontWeight.Bold, -0.01f),
    titleSmall = style(14.sp, 18.sp, FontWeight.SemiBold, -0.005f),
    bodyLarge = style(15.sp, 22.sp, FontWeight.Normal, 0f),
    bodyMedium = style(13.5.sp, 19.sp, FontWeight.Normal, 0f),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal, 0.01f),
    labelLarge = style(14.sp, 18.sp, FontWeight.Bold, 0.01f),
    labelMedium = style(12.sp, 16.sp, FontWeight.SemiBold, 0.03f),
    labelSmall = style(11.sp, 14.sp, FontWeight.Bold, 0.06f),
)

@Composable
private fun animateColor(target: Color, label: String): Color =
    animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 280),
        label = label,
    ).value

@Composable
private fun ColorScheme.animated(): ColorScheme = copy(
    primary = animateColor(primary, "cs_p"),
    onPrimary = animateColor(onPrimary, "cs_op"),
    primaryContainer = animateColor(primaryContainer, "cs_pc"),
    onPrimaryContainer = animateColor(onPrimaryContainer, "cs_opc"),
    secondary = animateColor(secondary, "cs_s"),
    onSecondary = animateColor(onSecondary, "cs_os"),
    secondaryContainer = animateColor(secondaryContainer, "cs_sc"),
    onSecondaryContainer = animateColor(onSecondaryContainer, "cs_osc"),
    tertiary = animateColor(tertiary, "cs_t"),
    onTertiary = animateColor(onTertiary, "cs_ot"),
    tertiaryContainer = animateColor(tertiaryContainer, "cs_tc"),
    onTertiaryContainer = animateColor(onTertiaryContainer, "cs_otc"),
    background = animateColor(background, "cs_bg"),
    onBackground = animateColor(onBackground, "cs_obg"),
    surface = animateColor(surface, "cs_sf"),
    onSurface = animateColor(onSurface, "cs_osf"),
    surfaceVariant = animateColor(surfaceVariant, "cs_sv"),
    onSurfaceVariant = animateColor(onSurfaceVariant, "cs_osv"),
    surfaceContainerLowest = animateColor(surfaceContainerLowest, "cs_scl0"),
    surfaceContainerLow = animateColor(surfaceContainerLow, "cs_scl1"),
    surfaceContainer = animateColor(surfaceContainer, "cs_sc2"),
    surfaceContainerHigh = animateColor(surfaceContainerHigh, "cs_sch"),
    surfaceContainerHighest = animateColor(surfaceContainerHighest, "cs_sch2"),
    outline = animateColor(outline, "cs_out"),
    outlineVariant = animateColor(outlineVariant, "cs_outv"),
)

@Composable
fun RetroHeroTheme(
    dynamicColor: Boolean = dynamicColorState.value,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (Tone.isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (Tone.isDark) paletteState.value.dark else paletteState.value.light
    }
    val override = accentOverrideState.value
    val targetScheme = if (override != null) {
        val onP = if (override.luminance() > 0.56f) Color(0xFF141218) else Color.White
        base.copy(primary = override, onPrimary = onP)
    } else {
        base
    }
    MaterialExpressiveTheme(
        colorScheme = targetScheme.animated(),
        motionScheme = MotionScheme.expressive(),
        typography = ExpressiveTypography,
        content = content,
    )
}
