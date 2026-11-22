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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.em
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
            Wear(game.id.hashCode())
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

/** The printed front of the case, dressed like that system's real packaging. */
@Composable
private fun CaseFront(game: Game, style: CaseStyle, tint: Color, height: Dp) {
    val id = game.systemId
    when (style) {
        CaseStyle.Cardboard -> Box(Modifier.fillMaxSize()) {
            when (id) {
                // NES: art in a black frame, a black band along the bottom with the name in red.
                "nes" -> Column(Modifier.fillMaxSize().background(Color(0xFF0E0E0E)).padding(5.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(2.dp))) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
                    Row(Modifier.fillMaxWidth().height(height * 0.1f).padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("NES", style = heavy(11), color = Color(0xFFE3262E))
                        Box(Modifier.padding(start = 5.dp).weight(1f).height(2.dp).background(Color(0xFFE3262E)))
                        Seal(height)
                    }
                }
                // SNES: silver band across the top with the four-colour dots.
                "snes" -> Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().height(height * 0.14f).background(Brush.verticalGradient(listOf(Color(0xFFE4E4EA), Color(0xFFB4B4BC)))).padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(0xFFD4262F, 0xFFF0BF1A, 0xFF1F9C50, 0xFF2C52D6).forEach { c ->
                            Box(Modifier.padding(end = 2.dp).size(height * 0.045f).clip(CircleShape).background(Color(c)))
                        }
                        Text("SUPER NES", style = heavy(9), color = Color(0xFF3A3A44), modifier = Modifier.padding(start = 4.dp).weight(1f))
                        Seal(height)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
                }
                // Game Boy family: a light band at the top with the name, italic, like the logo.
                "gb", "gbc", "gba" -> Column(Modifier.fillMaxSize()) {
                    val band = if (id == "gba") Color(0xFF15151C) else Color(0xFFF4F4F2)
                    Row(
                        Modifier.fillMaxWidth().height(height * 0.12f).background(band).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (id) {
                            "gb" -> Text("GAME BOY", style = heavy(10, italic = true), color = Color(0xFF1B1B5E))
                            "gbc" -> Row {
                                "GAME BOY".forEach { Text(it.toString(), style = heavy(10, italic = true), color = Color(0xFF1B1B5E)) }
                                Spacer(Modifier.width(3.dp))
                                "COLOR".forEachIndexed { i, ch ->
                                    Text(ch.toString(), style = heavy(10, italic = true), color = listOf(Color(0xFFD62A2A), Color(0xFF7A2DBD), Color(0xFF1C8C3B), Color(0xFFF0B31A), Color(0xFF2257C7))[i])
                                }
                            }
                            else -> Column {
                                Text("GAME BOY", style = heavy(9, italic = true), color = Color.White)
                                Box(Modifier.width(height * 0.28f).height(2.dp).background(Color(0xFF3F64E0)))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Seal(height)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
                }
                else -> Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().height(height * 0.09f).background(lerp(tint, Color.Black, 0.3f)).padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(game.system?.shortName?.uppercase().orEmpty(), style = heavy(9), color = Color.White, modifier = Modifier.weight(1f))
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
                }
            }
            // Printed card: slightly worn edges.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)), radius = size.maxDimension * 0.8f))
                drawRect(Color.White.copy(alpha = 0.18f), style = Stroke(1.2.dp.toPx()))
            }
        }
        CaseStyle.Clamshell -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A2A2E), Color(0xFF0D0D0F))))) {
            Column(Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(3.dp))) {
                when (id) {
                    // Genesis: black top band with a red grid and the name in red.
                    "genesis", "32x" -> Box(Modifier.fillMaxWidth().height(height * 0.12f).background(Color(0xFF0A0A0A))) {
                        Canvas(Modifier.fillMaxSize()) {
                            val step = size.height / 3f
                            var x = 0f
                            while (x < size.width) { drawLine(Color(0xFFB3141C).copy(alpha = 0.55f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                            var y = 0f
                            while (y < size.height) { drawLine(Color(0xFFB3141C).copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
                        }
                        Text(if (id == "32x") "GENESIS 32X" else "GENESIS", style = heavy(11), color = Color(0xFFE5232B), modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                    }
                    // Master System: the white grid border.
                    "sms" -> Box(Modifier.fillMaxWidth().height(height * 0.12f).background(Color.White)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val step = size.height / 2.5f
                            var x = 0f
                            while (x < size.width) { drawLine(Color.Black.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                            drawLine(Color.Black.copy(alpha = 0.6f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
                        }
                        Text("MASTER SYSTEM", style = heavy(8), color = Color(0xFFD4202A), modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp).background(Color.White).padding(horizontal = 2.dp))
                    }
                    "gg" -> Box(Modifier.fillMaxWidth().height(height * 0.12f).background(Brush.horizontalGradient(listOf(Color(0xFF0E0E10), Color(0xFF2846B8))))) {
                        Text("GAME GEAR", style = heavy(10), color = Color.White, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                    }
                    else -> Box(Modifier.fillMaxWidth().height(height * 0.1f).background(Color.Black)) {
                        Text(game.system?.shortName?.uppercase().orEmpty(), style = heavy(9), color = Color.White, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false)
                    // The clear sleeve over the printed insert.
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent, Color.White.copy(alpha = 0.05f)))))
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color.White.copy(alpha = 0.14f), style = Stroke(1.5.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
            }
        }
        CaseStyle.Jewel -> Row(Modifier.fillMaxSize().background(Color(0xFF101012))) {
            // The hinge: a dark ridged strip down the left.
            Canvas(Modifier.fillMaxHeight().fillMaxWidth(0.08f)) {
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF1C1C20), Color(0xFF050506))))
                val step = 4.dp.toPx()
                var y = step
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(size.width * 0.2f, y), Offset(size.width * 0.8f, y), 1f)
                    y += step
                }
            }
            Box(Modifier.fillMaxSize().padding(3.dp)) {
                Row(Modifier.fillMaxSize()) {
                    if (id == "psx") {
                        // The black strip down the left of a PlayStation insert, name running up it.
                        Box(Modifier.fillMaxHeight().fillMaxWidth(0.13f).background(Color.Black), contentAlignment = Alignment.Center) {
                            Column(Modifier.graphicsLayer { rotationZ = -90f }.width(height * 0.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Four little colour blocks, then the name.
                                    listOf(0xFFE5232B, 0xFFF0BF1A, 0xFF1F9C50, 0xFF2C52D6).forEach { c ->
                                        Box(Modifier.padding(end = 1.dp).size(5.dp).background(Color(c)))
                                    }
                                    Text("  PlayStation", style = heavy(8), color = Color.White, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxSize()) {
                        if (id != "psx") {
                            Box(Modifier.fillMaxWidth().height(height * 0.09f).background(Color.Black)) {
                                Text(
                                    when (id) { "segacd" -> "SEGA CD"; "pcecd" -> "CD-ROM²"; else -> game.system?.shortName?.uppercase().orEmpty() },
                                    style = heavy(9),
                                    color = if (id == "segacd") Color(0xFFE5232B) else Color.White,
                                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                                )
                            }
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
                    }
                }
                // Clear plastic: a cool tint.
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x22BFD8FF), Color.Transparent, Color(0x14FFFFFF)))))
            }
            Unit
        }
        CaseStyle.Keepcase -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFDCDCE2), Color(0xFFB4B4BC))))) {
            Column(Modifier.fillMaxSize().padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 7.dp).clip(RoundedCornerShape(4.dp))) {
                // DS inserts: a white band with the name, a thin grey rule under it.
                Box(Modifier.fillMaxWidth().height(height * 0.11f).background(Color.White)) {
                    Text("NINTENDO DS", style = heavy(9), color = Color(0xFF3A3A44), modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF9A9AA4)))
                Box(Modifier.weight(1f).fillMaxWidth()) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
            }
            // Clear outer sleeve.
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent))))
        }
    }
}

/** Handled-box wear: a few faint scuffs and a crease, different for every game. */
@Composable
private fun Wear(seed: Int) {
    Canvas(Modifier.fillMaxSize()) {
        val r = java.util.Random(seed.toLong())
        repeat(5) {
            val x = r.nextFloat() * size.width
            val y = r.nextFloat() * size.height
            val len = size.minDimension * (0.08f + r.nextFloat() * 0.18f)
            val ang = r.nextFloat() * 6.28f
            drawLine(
                Color.White.copy(alpha = 0.06f + r.nextFloat() * 0.05f),
                Offset(x, y),
                Offset(x + len * kotlin.math.cos(ang), y + len * kotlin.math.sin(ang)),
                strokeWidth = 1f,
            )
        }
        // A soft crease across one corner.
        val cx = if (r.nextBoolean()) 0f else size.width
        drawLine(Color.White.copy(alpha = 0.07f), Offset(cx, size.height * 0.08f), Offset(if (cx == 0f) size.width * 0.12f else size.width * 0.88f, 0f), 1.5f)
        // Rubbed corners.
        listOf(Offset(0f, 0f), Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height)).forEach { c ->
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent), center = c, radius = size.minDimension * 0.1f), size.minDimension * 0.1f, c)
        }
    }
}

/** A gold oval seal, like the one on Nintendo boxes. */
@Composable
private fun Seal(height: Dp) {
    Box(
        Modifier
            .size(width = height * 0.09f, height = height * 0.055f)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFF6DE8A), Color(0xFFC79A34))))
            .border(1.dp, Color(0xFF8A6A1E), CircleShape),
    )
}

private fun heavy(size: Int, italic: Boolean = false) = TextStyle(
    fontFamily = com.retrohero.app.ui.theme.Inter,
    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
    fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
    fontSize = size.sp,
    letterSpacing = 0.02.em,
)

/** Game Boy carts have a corner cut off at the top right. */
private val GameBoyCartShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    val cut = size.width * 0.14f
    val r = size.width * 0.05f
    moveTo(r, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - r)
    quadraticTo(size.width, size.height, size.width - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, 0f, size.height - r)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

/** The cartridge, disc or card that comes out of the box, shaped like the real thing. */
@Composable
fun GameMedia(game: Game, width: Dp, modifier: Modifier = Modifier, spin: Float = 0f) {
    val spec = boxSpec(game.systemId)
    val id = game.systemId
    when (spec.media) {
        Media.Disc -> Box(modifier.size(width).graphicsLayer { rotationZ = spin }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                if (id == "psx") {
                    // PlayStation discs are black underneath, with a faint purple sheen.
                    drawCircle(Brush.sweepGradient(listOf(Color(0xFF111114), Color(0xFF2A1F3A), Color(0xFF111114), Color(0xFF1E2A3A), Color(0xFF111114))))
                } else {
                    drawCircle(Brush.sweepGradient(listOf(Color(0xFFE8E8EE), Color(0xFFB9D4F2), Color(0xFFF2D8F0), Color(0xFFF5F0C8), Color(0xFFC8F0DA), Color(0xFFE8E8EE))))
                }
                drawCircle(Color.Black.copy(alpha = 0.2f), style = Stroke(2.dp.toPx()))
            }
            // Printed label side.
            Box(Modifier.fillMaxSize(0.9f).clip(CircleShape), contentAlignment = Alignment.Center) {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
                if (id == "psx") {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.75f to Color.Transparent, 0.76f to Color.Black.copy(alpha = 0.85f))))
                    Text("PlayStation", style = heavy(8), color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = width * 0.07f))
                }
            }
            Box(Modifier.fillMaxSize(0.32f).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFE6E6EA), Color(0xFFB8B8C0)))))
            Box(Modifier.fillMaxSize(0.12f).clip(CircleShape).background(Color(0xFF0A0A0C)))
        }
        else -> {
            val h = width / spec.mediaAspect
            val shape = when {
                spec.media == Media.Card -> RoundedCornerShape(8.dp)
                id == "gb" || id == "gbc" -> GameBoyCartShape
                id == "snes" -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                else -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            }
            Box(
                modifier
                    .size(width, h)
                    .graphicsLayer { shadowElevation = 24f; this.shape = shape; clip = true }
                    .background(Brush.verticalGradient(listOf(lerp(spec.shell, Color.White, 0.18f), spec.shell, lerp(spec.shell, Color.Black, 0.25f)))),
            ) {
                when {
                    // NES: label up top, a block of grip grooves along the bottom.
                    id == "nes" -> Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(h * 0.22f).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        val n = 6
                        for (i in 0 until n) {
                            val y = size.height * i / (n - 1)
                            drawLine(Color.Black.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                        }
                    }
                    // SNES: two grooves down each side.
                    id == "snes" -> Canvas(Modifier.fillMaxSize()) {
                        listOf(0.06f, 0.1f, 0.9f, 0.94f).forEach { fx ->
                            drawLine(Color.Black.copy(alpha = 0.22f), Offset(size.width * fx, size.height * 0.25f), Offset(size.width * fx, size.height * 0.85f), 2.dp.toPx())
                        }
                    }
                    // Game Boy: a little embossed arrow under the label.
                    id == "gb" || id == "gbc" -> Text("▲", color = Color.Black.copy(alpha = 0.25f), style = TextStyle(fontSize = 12.sp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                    // Sega and others: ribbed sides.
                    spec.media == Media.Cartridge -> Canvas(Modifier.fillMaxSize()) {
                        val n = 8
                        for (i in 0 until n) {
                            val y = size.height * (0.35f + 0.5f * i / (n - 1))
                            drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width * 0.07f, y), 1.5.dp.toPx())
                            drawLine(Color.White.copy(alpha = 0.07f), Offset(size.width * 0.93f, y), Offset(size.width, y), 1.5.dp.toPx())
                        }
                    }
                    else -> Unit
                }
                // The label.
                val labelAlign = when {
                    spec.media == Media.Card -> Alignment.Center
                    id == "nes" -> Alignment.TopCenter
                    else -> Alignment.TopCenter
                }
                Box(
                    Modifier
                        .align(labelAlign)
                        .padding(top = if (spec.media == Media.Card) 0.dp else h * 0.08f)
                        .fillMaxWidth(if (id == "snes") 0.74f else 0.8f)
                        .fillMaxHeight(when { spec.media == Media.Card -> 0.74f; id == "nes" -> 0.6f; id == "gb" || id == "gbc" -> 0.62f; else -> 0.66f })
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                ) {
                    GameArt(game, Modifier.fillMaxSize(), showTag = false)
                    // Title printed small along the bottom of the label, like a real cart.
                    Text(
                        game.title,
                        style = TextStyle(fontFamily = com.retrohero.app.ui.theme.Inter, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, fontSize = 10.sp, lineHeight = 11.sp),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}
