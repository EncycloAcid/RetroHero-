package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.retrohero.app.data.GameSystem
import java.net.URLEncoder

/**
 * Resolves real, high-resolution console hardware illustrations from the official open-source
 * Libretro RetroArch Assets repository (systematic & automatic hardware PNG sets), plus bespoke
 * hardware silhouettes for offline use and RetroHero's original HeroBox-16 / CHIP-8 consoles.
 */
object ConsoleArtwork {
    private const val BASE_SYSTEMATIC =
        "https://raw.githubusercontent.com/libretro/retroarch-assets/master/xmb/systematic/png/"
    private const val BASE_AUTOMATIC =
        "https://raw.githubusercontent.com/libretro/retroarch-assets/master/xmb/automatic/png/"

    /** Official RetroArch asset stem overrides when different from [GameSystem.thumbnailDb]. */
    private val assetOverrides = mapOf(
        "nes" to "Nintendo - Nintendo Entertainment System",
        "fds" to "Nintendo - Family Computer Disk System",
        "snes" to "Nintendo - Super Nintendo Entertainment System",
        "n64" to "Nintendo - Nintendo 64",
        "gc" to "Nintendo - GameCube",
        "wii" to "Nintendo - Wii",
        "wiiu" to "Nintendo - Wii U",
        "switch" to "Nintendo - Nintendo Switch",
        "gb" to "Nintendo - Game Boy",
        "gbc" to "Nintendo - Game Boy Color",
        "gba" to "Nintendo - Game Boy Advance",
        "nds" to "Nintendo - Nintendo DS",
        "3ds" to "Nintendo - Nintendo 3DS",
        "virtualboy" to "Nintendo - Virtual Boy",
        "pokemini" to "Nintendo - Pokemon Mini",
        "ps1" to "Sony - PlayStation",
        "ps2" to "Sony - PlayStation 2",
        "psp" to "Sony - PlayStation Portable",
        "vita" to "Sony - PlayStation Vita",
        "genesis" to "Sega - Mega Drive - Genesis",
        "segacd" to "Sega - Mega-CD - Sega CD",
        "32x" to "Sega - 32X",
        "sms" to "Sega - Master System - Mark III",
        "gg" to "Sega - Game Gear",
        "saturn" to "Sega - Saturn",
        "dreamcast" to "Sega - Dreamcast",
        "sg1000" to "Sega - SG-1000",
        "pce" to "NEC - PC Engine - TurboGrafx 16",
        "pcecd" to "NEC - PC Engine CD - TurboGrafx-CD",
        "supergrafx" to "NEC - PC Engine SuperGrafx",
        "pcfx" to "NEC - PC-FX",
        "neogeo" to "SNK - Neo Geo",
        "neocd" to "SNK - Neo Geo CD",
        "ngp" to "SNK - Neo Geo Pocket Color",
        "wonderswan" to "Bandai - WonderSwan Color",
        "atari2600" to "Atari - 2600",
        "atari5200" to "Atari - 5200",
        "atari7800" to "Atari - 7800",
        "lynx" to "Atari - Lynx",
        "jaguar" to "Atari - Jaguar",
        "coleco" to "Coleco - ColecoVision",
        "intellivision" to "Mattel - Intellivision",
        "vectrex" to "GCE - Vectrex",
        "odyssey2" to "Magnavox - Odyssey2",
        "3do" to "The 3DO Company - 3DO",
        "c64" to "Commodore - 64",
        "amiga" to "Commodore - Amiga",
        "msx" to "Microsoft - MSX",
        "zxspectrum" to "Sinclair - ZX Spectrum",
        "dos" to "DOS",
        "scummvm" to "ScummVM",
        "arcade" to "FBNeo - Arcade Games",
        "pico8" to "PICO-8",
        "tic80" to "TIC-80",
    )

    private fun enc(name: String): String =
        URLEncoder.encode(name, "UTF-8").replace("+", "%20")

    /** Returns ordered hardware artwork URLs for [system] (empty for original built-in consoles). */
    fun urlsFor(system: GameSystem): List<String> {
        if (system.id == "herobox16" || system.id == "chip8") return emptyList()
        val stem = assetOverrides[system.id] ?: system.thumbnailDb.takeIf { it.isNotBlank() } ?: return emptyList()
        val encoded = enc(stem)
        return listOf(
            "$BASE_SYSTEMATIC$encoded.png",
            "$BASE_AUTOMATIC$encoded.png",
        )
    }

    enum class FormFactor { HANDHELD, DUAL_SCREEN, DISC_CONSOLE, HOME_CONSOLE, ARCADE, COMPUTER, HEROBOX16 }

    fun formFactorFor(systemId: String): FormFactor = when (systemId) {
        "herobox16" -> FormFactor.HEROBOX16
        "nds", "3ds" -> FormFactor.DUAL_SCREEN
        "gb", "gbc", "gba", "gg", "psp", "vita", "lynx", "ngp", "wonderswan", "pokemini", "supervision", "gw", "switch", "virtualboy" -> FormFactor.HANDHELD
        "ps1", "ps2", "gc", "wii", "wiiu", "saturn", "dreamcast", "segacd", "pcecd", "pcfx", "neocd", "3do", "cdi" -> FormFactor.DISC_CONSOLE
        "arcade", "neogeo" -> FormFactor.ARCADE
        "c64", "amiga", "msx", "zxspectrum", "amstrad", "atarist", "x68000", "pc98", "dos", "scummvm", "pico8", "tic80", "chip8", "j2me" -> FormFactor.COMPUTER
        else -> FormFactor.HOME_CONSOLE
    }
}

/**
 * Renders the real console hardware illustration (loaded & cached via Coil) with a crisp bespoke
 * vector hardware silhouette shown immediately while loading or when offline.
 */
@Composable
fun M3ConsoleImage(
    system: GameSystem,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val urls = remember(system.id) { ConsoleArtwork.urlsFor(system) }
    var urlIdx by remember(system.id) { mutableIntStateOf(0) }
    var loaded by remember(system.id) { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!loaded || urlIdx >= urls.size) {
            M3ConsoleVectorSilhouette(
                formFactor = ConsoleArtwork.formFactorFor(system.id),
                tint = tint,
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        }
        if (urlIdx < urls.size) {
            AsyncImage(
                model = urls[urlIdx],
                contentDescription = "${system.name} hardware",
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                onError = {
                    loaded = false
                    urlIdx++
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun M3ConsoleVectorSilhouette(
    formFactor: ConsoleArtwork.FormFactor,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = (size.minDimension * 0.065f).coerceAtLeast(1.5.dp.toPx())
        val soft = tint.copy(alpha = 0.24f)

        when (formFactor) {
            ConsoleArtwork.FormFactor.HEROBOX16 -> {
                // RetroHero's signature 16-bit fantasy micro-console
                drawRoundRect(soft, Offset(w * 0.12f, h * 0.20f), Size(w * 0.76f, h * 0.60f), CornerRadius(w * 0.14f))
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.20f), Size(w * 0.76f, h * 0.60f), CornerRadius(w * 0.14f), style = Stroke(stroke))
                // Glowing cartridge slot + 16-bit screen visor
                drawRoundRect(tint, Offset(w * 0.26f, h * 0.12f), Size(w * 0.48f, h * 0.12f), CornerRadius(w * 0.04f))
                drawRoundRect(tint.copy(alpha = 0.35f), Offset(w * 0.24f, h * 0.32f), Size(w * 0.52f, h * 0.26f), CornerRadius(w * 0.06f))
                // Action buttons + D-pad
                drawCircle(tint, radius = w * 0.055f, center = Offset(w * 0.70f, h * 0.68f))
                drawCircle(tint, radius = w * 0.055f, center = Offset(w * 0.58f, h * 0.68f))
                drawLine(tint, Offset(w * 0.24f, h * 0.68f), Offset(w * 0.38f, h * 0.68f), strokeWidth = stroke)
                drawLine(tint, Offset(w * 0.31f, h * 0.61f), Offset(w * 0.31f, h * 0.75f), strokeWidth = stroke)
            }
            ConsoleArtwork.FormFactor.HANDHELD -> {
                drawRoundRect(soft, Offset(w * 0.10f, h * 0.24f), Size(w * 0.80f, h * 0.52f), CornerRadius(w * 0.16f))
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.24f), Size(w * 0.80f, h * 0.52f), CornerRadius(w * 0.16f), style = Stroke(stroke))
                drawRoundRect(tint.copy(alpha = 0.35f), Offset(w * 0.30f, h * 0.32f), Size(w * 0.40f, h * 0.36f), CornerRadius(w * 0.05f))
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.79f, h * 0.45f))
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.79f, h * 0.57f))
                drawLine(tint, Offset(w * 0.16f, h * 0.50f), Offset(w * 0.25f, h * 0.50f), strokeWidth = stroke)
                drawLine(tint, Offset(w * 0.205f, h * 0.45f), Offset(w * 0.205f, h * 0.55f), strokeWidth = stroke)
            }
            ConsoleArtwork.FormFactor.DUAL_SCREEN -> {
                drawRoundRect(soft, Offset(w * 0.18f, h * 0.10f), Size(w * 0.64f, h * 0.36f), CornerRadius(w * 0.08f))
                drawRoundRect(tint, Offset(w * 0.18f, h * 0.10f), Size(w * 0.64f, h * 0.36f), CornerRadius(w * 0.08f), style = Stroke(stroke))
                drawRoundRect(soft, Offset(w * 0.18f, h * 0.52f), Size(w * 0.64f, h * 0.38f), CornerRadius(w * 0.08f))
                drawRoundRect(tint, Offset(w * 0.18f, h * 0.52f), Size(w * 0.64f, h * 0.38f), CornerRadius(w * 0.08f), style = Stroke(stroke))
                drawRoundRect(tint.copy(alpha = 0.35f), Offset(w * 0.28f, h * 0.16f), Size(w * 0.44f, h * 0.24f), CornerRadius(w * 0.03f))
                drawRoundRect(tint.copy(alpha = 0.35f), Offset(w * 0.30f, h * 0.58f), Size(w * 0.40f, h * 0.24f), CornerRadius(w * 0.03f))
            }
            ConsoleArtwork.FormFactor.DISC_CONSOLE -> {
                drawRoundRect(soft, Offset(w * 0.10f, h * 0.22f), Size(w * 0.80f, h * 0.56f), CornerRadius(w * 0.12f))
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.22f), Size(w * 0.80f, h * 0.56f), CornerRadius(w * 0.12f), style = Stroke(stroke))
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(stroke))
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.50f))
            }
            ConsoleArtwork.FormFactor.ARCADE -> {
                drawRoundRect(soft, Offset(w * 0.22f, h * 0.10f), Size(w * 0.56f, h * 0.80f), CornerRadius(w * 0.08f))
                drawRoundRect(tint, Offset(w * 0.22f, h * 0.10f), Size(w * 0.56f, h * 0.80f), CornerRadius(w * 0.08f), style = Stroke(stroke))
                drawRoundRect(tint.copy(alpha = 0.38f), Offset(w * 0.30f, h * 0.22f), Size(w * 0.40f, h * 0.30f), CornerRadius(w * 0.04f))
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.38f, h * 0.62f))
                drawCircle(tint, radius = w * 0.04f, center = Offset(w * 0.58f, h * 0.62f))
                drawCircle(tint, radius = w * 0.04f, center = Offset(w * 0.66f, h * 0.62f))
            }
            ConsoleArtwork.FormFactor.COMPUTER -> {
                drawRoundRect(soft, Offset(w * 0.14f, h * 0.14f), Size(w * 0.72f, h * 0.46f), CornerRadius(w * 0.08f))
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.14f), Size(w * 0.72f, h * 0.46f), CornerRadius(w * 0.08f), style = Stroke(stroke))
                drawRoundRect(tint.copy(alpha = 0.35f), Offset(w * 0.22f, h * 0.22f), Size(w * 0.56f, h * 0.30f), CornerRadius(w * 0.04f))
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.68f), Size(w * 0.80f, h * 0.18f), CornerRadius(w * 0.06f), style = Stroke(stroke))
            }
            ConsoleArtwork.FormFactor.HOME_CONSOLE -> {
                drawRoundRect(soft, Offset(w * 0.10f, h * 0.26f), Size(w * 0.80f, h * 0.48f), CornerRadius(w * 0.10f))
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.26f), Size(w * 0.80f, h * 0.48f), CornerRadius(w * 0.10f), style = Stroke(stroke))
                drawLine(tint, Offset(w * 0.14f, h * 0.48f), Offset(w * 0.86f, h * 0.48f), strokeWidth = stroke)
                drawRoundRect(tint, Offset(w * 0.22f, h * 0.34f), Size(w * 0.32f, h * 0.08f), CornerRadius(w * 0.03f))
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.72f, h * 0.62f))
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.58f, h * 0.62f))
            }
        }
    }
}
