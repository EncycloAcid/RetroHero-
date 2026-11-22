package com.retrohero.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.retrohero.app.R

private fun inter(weight: Int, opticalSize: Float) = Font(
    R.font.inter,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", opticalSize)),
)

/** Inter (SIL OFL): a neutral grotesque close to the iOS system face. Text sizes use the small optical size. */
val Inter = FontFamily(inter(400, 14f), inter(500, 14f), inter(600, 14f), inter(700, 14f))

/** Inter at its display optical size, for large titles. */
val InterDisplay = FontFamily(inter(600, 32f), inter(700, 32f), inter(800, 32f))

/** 8-bit pixel face for small retro accents such as system tags (SIL OFL). */
val PixelFont = FontFamily(Font(R.font.press_start_2p))
