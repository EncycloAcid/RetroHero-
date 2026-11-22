package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/** A shape partway through a [Morph] between two normalised Material shapes. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

@Composable
fun rememberMorph(from: RoundedPolygon, to: RoundedPolygon): Morph = remember(from, to) { Morph(from, to) }

private val IconShapes = listOf(
    MaterialShapes.Cookie4Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided, MaterialShapes.Pentagon, MaterialShapes.Gem,
    MaterialShapes.Cookie7Sided, MaterialShapes.Puffy,
)

/** An icon on a tonal Material shape; the shape and tone vary per icon for an expressive rhythm. */
@Composable
fun ShapeIcon(icon: ImageVector, size: Dp = 32.dp, tint: Color? = null) {
    // Mochy's row icon: an iOS-style light circle with a dark glyph, on a dark card.
    Box(
        Modifier.size(size).background(Color(0xFFE5E5EA), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint ?: Color(0xFF1C1C1E), modifier = Modifier.size(size * 0.62f))
    }
}
