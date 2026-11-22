package com.retrohero.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.retrohero.app.R

/*
 * Google Sans Flex (SIL OFL), subset to Latin and limited to the axes the app uses:
 * weight 400–800, optical size 12–48 and ROND (roundness) 0–100.
 */
private fun sans(weight: Int, opsz: Float, round: Float) = Font(
    R.font.google_sans_flex,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
        FontVariation.Setting("ROND", round),
    ),
)

/** Text face: crisp, slightly rounded, small optical size. */
val GoogleSans = FontFamily(sans(400, 16f, 20f), sans(500, 16f, 20f), sans(600, 16f, 30f), sans(700, 16f, 30f))

/** Display face: large optical size with full ROND for soft, expressive headlines. */
val GoogleSansDisplay = FontFamily(sans(500, 40f, 100f), sans(600, 40f, 100f), sans(700, 40f, 100f), sans(800, 40f, 100f))

/** 8-bit pixel face for small retro accents such as system tags (SIL OFL). */
val PixelFont = FontFamily(Font(R.font.press_start_2p))
