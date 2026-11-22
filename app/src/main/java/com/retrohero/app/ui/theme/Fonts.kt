package com.retrohero.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.retrohero.app.R

private fun bricolage(weight: Int) = Font(
    R.font.bricolage_grotesque,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", 36f)),
)

/** Expressive display face for titles and headlines (variable font, SIL OFL). */
val Bricolage = FontFamily(bricolage(500), bricolage(600), bricolage(700), bricolage(800))

/** 8-bit pixel face for small retro accents such as system tags (SIL OFL). */
val PixelFont = FontFamily(Font(R.font.press_start_2p))
