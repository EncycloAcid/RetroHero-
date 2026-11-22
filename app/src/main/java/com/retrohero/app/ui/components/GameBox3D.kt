package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.drawWithContent
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

/** What the packaging is made of, which decides how the front, spine and edges look. */
enum class CaseStyle { Cardboard, Clamshell, Jewel, Keepcase }

fun caseStyle(systemId: String): CaseStyle = when (systemId) {
    "psx", "segacd", "pcecd" -> CaseStyle.Jewel
    "genesis", "32x", "sms", "gg", "atari2600", "atari7800" -> CaseStyle.Clamshell
    "nds" -> CaseStyle.Keepcase
    else -> CaseStyle.Cardboard
}

/**
 * A game box in 3D, made to look like the real packaging: a printed cardboard box, a black
 * plastic clamshell with the art under a clear sleeve, a CD jewel case with its ridged hinge,
 * or a grey keep case. It has a spine and a top edge that come into view as it turns, a glare
 * that slides across like a foil card, and a soft reflection on the floor. [rotY]/[rotX] are
 * degrees; [lid] 0..1 swings the front open to show the tray inside.
 */
@Composable
fun GameBox3D(game: Game, width: Dp, rotY: Float, rotX: Float, modifier: Modifier = Modifier, lid: Float = 0f, reflection: Boolean = true) {
    val spec = boxSpec(game.systemId)
    val style = caseStyle(game.systemId)
    val height = width / spec.aspect
    val density = LocalDensity.current
    val wPx = with(density) { width.toPx() }
    val hPx = with(density) { height.toPx() }
    val tint = game.system?.color ?: Color(0xFF6E6A86)
    val radY = Math.toRadians(rotY.toDouble())
    val radX = Math.toRadians(rotX.toDouble())
    val frontHalfW = (wPx / 2f) * cos(radY).toFloat()
    val frontHalfH = (hPx / 2f) * cos(radX).toFloat()
    val depthPx = wPx * spec.depth
    val spineW = depthPx * abs(sin(radY).toFloat())
    val cam = 14f * density.density
    val corner = when (style) {
        CaseStyle.Cardboard -> 4.dp
        CaseStyle.Clamshell -> 7.dp
        CaseStyle.Jewel -> 3.dp
        CaseStyle.Keepcase -> 10.dp
    }
    val spineFill = when (style) {
        CaseStyle.Cardboard -> lerp(tint, Color.Black, 0.35f)
        CaseStyle.Clamshell -> Color(0xFF151517)
        CaseStyle.Jewel -> Color(0xFF0E0E10)
        CaseStyle.Keepcase -> Color(0xFFD6D6DA)
    }
    val spineInk = if (style == CaseStyle.Keepcase) Color(0xFF2A2A30) else Color.White.copy(alpha = 0.85f)

    Box(modifier.size(width, height), contentAlignment = Alignment.Center) {
        // Reflection on a glossy floor, fading out.
        if (reflection && lid == 0f) {
            Box(
                Modifier
                    .size(width, height)
                    .graphicsLayer {
                        translationY = hPx + 6.dp.toPx()
                        rotationY = rotY
                        scaleY = -1f
                        cameraDistance = cam
                        alpha = 0.18f
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    }
                    .clip(RoundedCornerShape(corner))
                    // Only the part nearest the box shows, fading to nothing (flipped, so the
                    // bottom of this layer is the edge touching the box).
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(0f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                        )
                    },
            ) {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
            }
        }
        // Contact shadow.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 16.dp)
                .width(width * 0.95f)
                .height(22.dp)
                .graphicsLayer { translationX = -rotY * 1.4f; scaleX = 1f - abs(rotY) / 140f }
                .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)), CircleShape),
        )
        // Spine, on whichever side has turned toward you.
        if (spineW > 0.5f) {
            val side = if (rotY > 0) -1f else 1f
            Box(
                Modifier
                    .width(with(density) { spineW.toDp() })
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = side * (frontHalfW + spineW / 2f)
                        rotationX = rotX
                        cameraDistance = cam
                        scaleY = (1f - spineW / wPx * 0.08f) * cos(radX).toFloat()
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(if (side < 0) listOf(lerp(spineFill, Color.Black, 0.45f), spineFill) else listOf(spineFill, lerp(spineFill, Color.Black, 0.45f)))),
                contentAlignment = Alignment.Center,
            ) {
                when (style) {
                    // A jewel case's spine is a strip of ridges (the hinge).
                    CaseStyle.Jewel -> Canvas(Modifier.fillMaxSize()) {
                        val step = 3.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                    else -> Unit
                }
                if (spineW > 64f) {
                    Column(
                        Modifier.graphicsLayer { rotationZ = 90f }.width(height * 0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            game.title.uppercase(),
                            style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp),
                            color = spineInk,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        }
        // Inside of the case, seen once the lid swings open.
        if (lid > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = rotY; rotationX = rotX; cameraDistance = cam }
                    .clip(RoundedCornerShape(corner))
                    .background(Brush.verticalGradient(listOf(lerp(spec.shell, Color.Black, 0.55f), lerp(spec.shell, Color.Black, 0.78f))))
                    .border(2.dp, lerp(spec.shell, Color.Black, 0.3f), RoundedCornerShape(corner)),
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(if (spec.media == Media.Disc) 0.72f else 0.6f)
                        .clip(if (spec.media == Media.Disc) CircleShape else RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), if (spec.media == Media.Disc) CircleShape else RoundedCornerShape(8.dp)),
                )
            }
        }
        // The front.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    cameraDistance = cam
                    rotationX = rotX
                    if (lid > 0f) {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = rotY - 112f * lid
                    } else {
                        rotationY = rotY
                    }
                    shadowElevation = 22f
                    shape = RoundedCornerShape(corner)
                    clip = true
                },
        ) {
            if (rotY - 112f * lid < -90f) {
                // The inside of the lid.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(lerp(spec.shell, Color.Black, 0.6f), lerp(spec.shell, Color.Black, 0.4f))))
                        .border(2.dp, lerp(spec.shell, Color.Black, 0.3f), RoundedCornerShape(corner)),
                )
                return@Box
            }
            CaseFront(game, style, tint, height)
            // Glare: a soft bright band that slides with the tilt (stronger on clear plastic).
            val shine = if (style == CaseStyle.Cardboard) 0.16f else 0.3f
            Canvas(Modifier.fillMaxSize()) {
                val shift = (rotY / 40f + rotX / 60f).coerceIn(-1f, 1f)
                val cx = size.width * (0.5f - shift)
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.42f to Color.White.copy(alpha = 0f),
                        0.5f to Color.White.copy(alpha = shine + 0.08f * abs(shift)),
                        0.58f to Color.White.copy(alpha = 0f),
                        1f to Color.Transparent,
                        start = Offset(cx - size.width, 0f),
                        end = Offset(cx + size.width, size.height),
                    ),
                )
                // Light catching the top edge, shadow along the bottom.
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.18f), 0.04f to Color.Transparent, 0.9f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.3f)))
            }
        }
    }
}

/** The printed front of the case, by packaging type. */
@Composable
private fun CaseFront(game: Game, style: CaseStyle, tint: Color, height: Dp) {
    val label = game.system?.shortName?.uppercase().orEmpty()
    when (style) {
        CaseStyle.Cardboard -> Box(Modifier.fillMaxSize()) {
            GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false)
            // Printed system band across the top, with a little seal.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(height * 0.085f)
                    .background(Brush.horizontalGradient(listOf(lerp(tint, Color.Black, 0.15f), lerp(tint, Color.Black, 0.45f))))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp), color = Color.White, modifier = Modifier.weight(1f))
                Box(Modifier.size(height * 0.05f).clip(CircleShape).background(Color(0xFFF2D27A)).border(1.dp, Color(0xFFB08A2E), CircleShape))
            }
            // Worn corners and edges of printed card.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)), radius = size.maxDimension * 0.78f))
                drawRect(Color.White.copy(alpha = 0.22f), style = Stroke(1.2.dp.toPx()))
            }
        }
        CaseStyle.Clamshell -> Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A2A2E), Color(0xFF0F0F11)))),
        ) {
            // Art under a clear sleeve, inset in the black plastic.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 7.dp)
                    .clip(RoundedCornerShape(3.dp)),
            ) {
                GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false)
                Box(Modifier.fillMaxWidth().height(height * 0.07f).background(Color.Black.copy(alpha = 0.82f)), contentAlignment = Alignment.CenterStart) {
                    Text(label, style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp), color = Color.White, modifier = Modifier.padding(start = 6.dp))
                }
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent, Color.White.copy(alpha = 0.05f)))))
            }
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color.White.copy(alpha = 0.14f), style = Stroke(1.5.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
            }
        }
        CaseStyle.Jewel -> Row(Modifier.fillMaxSize().background(Color(0xFF101012))) {
            // The hinge: a dark ridged strip down the left.
            Canvas(Modifier.fillMaxHeight().fillMaxWidth(0.085f)) {
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF1C1C20), Color(0xFF050506))))
                val step = 4.dp.toPx()
                var y = step
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(size.width * 0.2f, y), Offset(size.width * 0.8f, y), 1f)
                    y += step
                }
            }
            Box(Modifier.fillMaxSize().padding(3.dp)) {
                GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false)
                // Black band like a PlayStation insert.
                Box(Modifier.fillMaxWidth().height(height * 0.075f).background(Color.Black.copy(alpha = 0.88f)), contentAlignment = Alignment.CenterStart) {
                    Text(label, style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp), color = Color.White, modifier = Modifier.padding(start = 6.dp))
                }
                // Clear plastic: a cool tint and bright edges.
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x22BFD8FF), Color.Transparent, Color(0x14FFFFFF)))))
            }
            Unit
        }
        CaseStyle.Keepcase -> Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE6E6EA), Color(0xFFBDBDC4)))),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = height * 0.1f, bottom = 8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            ) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
            Text(
                label,
                style = TextStyle(fontFamily = PixelFont, fontSize = 7.sp),
                color = Color(0xFF2A2A30),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = height * 0.035f),
            )
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
