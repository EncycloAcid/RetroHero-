package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.PixelFont
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class Media { Cartridge, Disc, Card }

/** The physical shape of a game: its box, how thick it is, and what's inside. */
data class BoxSpec(
    /** Box width / height. */
    val aspect: Float,
    /** Box thickness as a fraction of its width. */
    val depth: Float,
    val media: Media,
    val shell: Color,
    /** Cartridge or card width / height. */
    val mediaAspect: Float,
)

fun boxSpec(systemId: String): BoxSpec = when (systemId) {
    "nes" -> BoxSpec(0.72f, 0.2f, Media.Cartridge, Color(0xFF9C9CA0), 0.9f)
    "snes" -> BoxSpec(1.36f, 0.15f, Media.Cartridge, Color(0xFFBDBDC4), 1.3f)
    "gb" -> BoxSpec(0.82f, 0.18f, Media.Cartridge, Color(0xFFB9B9BE), 0.88f)
    "gbc" -> BoxSpec(0.82f, 0.18f, Media.Cartridge, Color(0xFF4A4A52), 0.88f)
    "gba" -> BoxSpec(0.8f, 0.16f, Media.Cartridge, Color(0xFF34343E), 1.6f)
    "nds" -> BoxSpec(0.88f, 0.13f, Media.Card, Color(0xFF2A2A2E), 1.06f)
    "vb" -> BoxSpec(0.8f, 0.18f, Media.Cartridge, Color(0xFF2A2A2E), 1.1f)
    "genesis", "32x", "sms" -> BoxSpec(0.71f, 0.2f, Media.Cartridge, Color(0xFF1C1C1E), 0.82f)
    "gg" -> BoxSpec(0.8f, 0.16f, Media.Cartridge, Color(0xFF1C1C1E), 1.25f)
    "psx", "segacd", "pcecd" -> BoxSpec(1.12f, 0.07f, Media.Disc, Color(0xFFCFCFD4), 1f)
    "pce" -> BoxSpec(0.8f, 0.12f, Media.Card, Color(0xFFE2E2E6), 0.64f)
    "ngp", "ws", "lynx" -> BoxSpec(0.8f, 0.16f, Media.Cartridge, Color(0xFF2F2F36), 1.15f)
    "atari2600", "atari7800" -> BoxSpec(0.74f, 0.2f, Media.Cartridge, Color(0xFF1C1C1E), 1.0f)
    else -> BoxSpec(0.72f, 0.18f, Media.Cartridge, Color(0xFF3A3A40), 1f)
}

/**
 * A game box in 3D: the cover on the front, a spine that comes into view as it turns, and a
 * glare that slides across like a foil card. [rotY]/[rotX] are degrees; [lid] 0..1 swings the
 * front open like a case to show the tray inside.
 */
@Composable
fun GameBox3D(game: Game, width: Dp, rotY: Float, rotX: Float, modifier: Modifier = Modifier, lid: Float = 0f) {
    val spec = boxSpec(game.systemId)
    val height = width / spec.aspect
    val density = LocalDensity.current
    val wPx = with(density) { width.toPx() }
    val tint = game.system?.color ?: Color(0xFF6E6A86)
    val spineColor = lerp(tint, Color.Black, 0.45f)
    val rad = Math.toRadians(rotY.toDouble())
    val frontHalf = (wPx / 2f) * cos(rad).toFloat()
    val spineW = wPx * spec.depth * abs(sin(rad).toFloat())
    val cam = 14f * density.density
    Box(modifier.size(width, height), contentAlignment = Alignment.Center) {
        // Soft shadow on the floor.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 22.dp)
                .width(width * 0.9f)
                .height(26.dp)
                .graphicsLayer { translationX = -rotY * 1.4f; scaleX = 1f - abs(rotY) / 140f }
                .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)), CircleShape),
        )
        // The spine, on whichever side has turned toward you.
        if (spineW > 0.5f) {
            val side = if (rotY > 0) -1f else 1f
            Box(
                Modifier
                    .width(with(density) { spineW.toDp() })
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = side * (frontHalf + spineW / 2f)
                        rotationX = rotX
                        cameraDistance = cam
                        scaleY = 1f - spineW / wPx * 0.08f
                    }
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(if (side < 0) listOf(lerp(spineColor, Color.Black, 0.4f), spineColor) else listOf(spineColor, lerp(spineColor, Color.Black, 0.4f)))),
                contentAlignment = Alignment.Center,
            ) {
                if (spineW > 18f) {
                    Text(
                        game.title.uppercase(),
                        style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.graphicsLayer { rotationZ = 90f }.width(height * 0.8f),
                    )
                }
            }
        }
        // Inside of the case, seen once the lid swings open.
        if (lid > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = rotY; rotationX = rotX; cameraDistance = cam }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(lerp(spec.shell, Color.Black, 0.55f), lerp(spec.shell, Color.Black, 0.75f))))
                    .border(2.dp, lerp(spec.shell, Color.Black, 0.3f), RoundedCornerShape(6.dp)),
            ) {
                // The tray the cartridge or disc sits in.
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(if (spec.media == Media.Disc) 0.72f else 0.6f)
                        .clip(if (spec.media == Media.Disc) CircleShape else RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                )
            }
        }
        // The front cover.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    cameraDistance = cam
                    rotationX = rotX
                    if (lid > 0f) {
                        // Swing open on the left edge.
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = rotY - 112f * lid
                    } else {
                        rotationY = rotY
                    }
                    shadowElevation = 18f
                    shape = RoundedCornerShape(6.dp)
                    clip = true
                },
        ) {
            // Past 90° we're looking at the inside of the lid, not the cover.
            if (rotY - 112f * lid < -90f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(lerp(spec.shell, Color.Black, 0.6f), lerp(spec.shell, Color.Black, 0.4f))))
                        .border(2.dp, lerp(spec.shell, Color.Black, 0.3f), RoundedCornerShape(6.dp)),
                )
                return@Box
            }
            GameArt(game, Modifier.fillMaxSize(), showTitle = true)
            // Plastic edge and system band along the top, like a real box.
            Box(Modifier.fillMaxWidth().height(height * 0.075f).background(lerp(tint, Color.Black, 0.2f).copy(alpha = 0.92f)), contentAlignment = Alignment.CenterStart) {
                Text(
                    game.system?.shortName?.uppercase().orEmpty(),
                    style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp),
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            // Glare: a soft bright band that slides with the tilt.
            Canvas(Modifier.fillMaxSize()) {
                val shift = (rotY / 40f + rotX / 60f).coerceIn(-1f, 1f)
                val cx = size.width * (0.5f - shift)
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.45f to Color.White.copy(alpha = 0.0f),
                        0.5f to Color.White.copy(alpha = 0.22f + 0.1f * abs(shift)),
                        0.55f to Color.White.copy(alpha = 0.0f),
                        1f to Color.Transparent,
                        start = Offset(cx - size.width, 0f),
                        end = Offset(cx + size.width, size.height),
                    ),
                )
                drawRoundRect(Color.White.copy(alpha = 0.18f), style = Stroke(1.5.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }
        }
    }
}

/** The cartridge, disc or card that comes out of the box, with the game's art as its label. */
@Composable
fun GameMedia(game: Game, width: Dp, modifier: Modifier = Modifier, spin: Float = 0f) {
    val spec = boxSpec(game.systemId)
    when (spec.media) {
        Media.Disc -> Box(modifier.size(width).graphicsLayer { rotationZ = spin }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                // Silver with a rainbow sheen, like the data side of a CD.
                drawCircle(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFE8E8EE), Color(0xFFB9D4F2), Color(0xFFF2D8F0), Color(0xFFF5F0C8),
                            Color(0xFFC8F0DA), Color(0xFFE8E8EE),
                        ),
                    ),
                )
                drawCircle(Color.Black.copy(alpha = 0.15f), style = Stroke(2.dp.toPx()))
            }
            BoxWithConstraints(Modifier.fillMaxSize(0.88f).clip(CircleShape), contentAlignment = Alignment.Center) {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
            }
            // Hub and hole.
            Box(Modifier.fillMaxSize(0.3f).clip(CircleShape).background(Color(0xFFD9D9DE)))
            Box(Modifier.fillMaxSize(0.12f).clip(CircleShape).background(Color(0xFF0A0A0C)))
        }
        else -> {
            val h = width / spec.mediaAspect
            val shape = if (spec.media == Media.Card) RoundedCornerShape(8.dp) else RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            Box(
                modifier
                    .size(width, h)
                    .graphicsLayer { shadowElevation = 24f; this.shape = shape; clip = true }
                    .background(Brush.verticalGradient(listOf(lerp(spec.shell, Color.White, 0.18f), spec.shell, lerp(spec.shell, Color.Black, 0.25f)))),
            ) {
                // Grip ridges along the top of a cartridge.
                if (spec.media == Media.Cartridge) {
                    Canvas(Modifier.fillMaxWidth().height(h * 0.14f).padding(horizontal = 14.dp, vertical = 5.dp)) {
                        val n = 7
                        for (i in 0 until n) {
                            val y = size.height * i / (n - 1)
                            drawLine(Color.Black.copy(alpha = 0.22f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx())
                        }
                    }
                }
                // The label.
                Box(
                    Modifier
                        .align(if (spec.media == Media.Card) Alignment.Center else Alignment.BottomCenter)
                        .fillMaxWidth(0.82f)
                        .fillMaxHeight(if (spec.media == Media.Card) 0.78f else 0.7f)
                        .offset(y = if (spec.media == Media.Card) 0.dp else (-8).dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(5.dp)),
                ) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
            }
        }
    }
}
