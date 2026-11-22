package com.retrohero.app.ui.ios

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import kotlin.math.min

/**
 * Rounded rectangle with continuous ("squircle") corners, like iOS app icons and cards:
 * the curvature eases in instead of starting abruptly where a circular arc would.
 */
class SquircleShape(private val radius: Dp, private val smoothing: Float = 0.6f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        val r = with(density) { radius.toPx() }.coerceAtMost(min(size.width, size.height) / 2f)
        val polygon = RoundedPolygon.rectangle(
            width = size.width,
            height = size.height,
            rounding = CornerRounding(r, smoothing),
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )
        return Outline.Generic(polygon.toPath().asComposePath())
    }

    override fun equals(other: Any?) = other is SquircleShape && other.radius == radius && other.smoothing == smoothing
    override fun hashCode() = radius.hashCode() * 31 + smoothing.hashCode()
}

object Squircles {
    val Tile = SquircleShape(7.dp)
    val Small = SquircleShape(10.dp)
    val Medium = SquircleShape(14.dp)
    val Large = SquircleShape(22.dp)
    val Card = SquircleShape(26.dp)
}
