package com.retrohero.app.ui.theme

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems

/** Every system gets its own Material expressive shape, used for badges and placeholders. */
private val SystemPolygons: List<RoundedPolygon> = listOf(
    MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided, MaterialShapes.Gem, MaterialShapes.Flower,
    MaterialShapes.SoftBurst, MaterialShapes.Puffy, MaterialShapes.Pentagon,
    MaterialShapes.Cookie12Sided, MaterialShapes.Clover8Leaf, MaterialShapes.Arch,
    MaterialShapes.Ghostish, MaterialShapes.Bun, MaterialShapes.PixelCircle,
    MaterialShapes.Cookie4Sided, MaterialShapes.PuffyDiamond, MaterialShapes.VerySunny,
    MaterialShapes.Cookie7Sided, MaterialShapes.Burst, MaterialShapes.Heart,
)

val GameSystem.polygon: RoundedPolygon
    get() = SystemPolygons[Systems.all.indexOf(this).coerceAtLeast(0) % SystemPolygons.size]

@Composable
fun GameSystem.badgeShape(): Shape = polygon.toShape()
