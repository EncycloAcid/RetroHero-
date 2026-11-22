package com.retrohero.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.PlaySummary
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SlotEase = CubicBezierEasing(0.35f, 0.1f, 0.15f, 1f)
private const val PAGES = 9

/**
 * Your recap, told as a story you tap through, like the music app's: a dot grid, giant words
 * drifting behind, and one beat per page. Tap or swipe left for the next page, right to go back.
 */
@Composable
fun RecapScreen(periodName: String, summary: PlaySummary, onClose: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var shownAt by remember { mutableLongStateOf(0L) }
    val lean = remember { Animatable(0f) }
    var drag by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(page) { shownAt = System.currentTimeMillis() }
    fun next() {
        if (System.currentTimeMillis() - shownAt < 350) return
        if (page < PAGES - 1) page++ else onClose()
    }
    fun back() { if (page > 0) page-- }

    val tone = summary.topGame?.first?.system?.color ?: Color(0xFF6E6A86)
    val giant = when (page) {
        4 -> summary.days.groupBy { it.first.dayOfWeek }.maxByOrNull { (_, v) -> v.sumOf { it.second } }?.key?.getDisplayName(JTextStyle.FULL, Locale.getDefault())?.uppercase() ?: "PLAY"
        5 -> summary.topSystem?.first?.shortName?.uppercase() ?: "RETRO"
        else -> "RETROHERO"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(page) { detectTapGestures { next() } }
            .pointerInput(page) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            drag < -56.dp.toPx() -> next()
                            drag > 56.dp.toPx() -> back()
                        }
                        drag = 0f
                    },
                ) { _, d -> drag += d }
            },
    ) {
        MeshBackdrop(tone)
        DotGrid()
        GiantWords(giant)
        Box(Modifier.fillMaxSize().graphicsLayer { translationX = (drag * 0.28f).coerceIn(-70.dp.toPx(), 14.dp.toPx()) }) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(700, easing = Emphasized)) { it / 3 } + fadeIn(tween(400))) togetherWith
                            (slideOutHorizontally(tween(500, easing = Emphasized)) { -it / 4 } + fadeOut(tween(300)) + scaleOut(tween(500), targetScale = 0.92f))
                    } else {
                        (slideInHorizontally(tween(600, easing = Emphasized)) { -it / 3 } + fadeIn(tween(400))) togetherWith
                            (slideOutHorizontally(tween(450, easing = Emphasized)) { it / 4 } + fadeOut(tween(250)))
                    }
                },
                label = "recapPage",
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
            ) { p ->
                Box(Modifier.fillMaxSize().padding(horizontal = 36.dp)) {
                    when (p) {
                        0 -> IntroPage(periodName)
                        1 -> HeyPage()
                        2 -> JourneyPage(summary)
                        3 -> TotalPage(periodName, summary)
                        4 -> WeekdayPage(summary)
                        5 -> SlotPage(summary)
                        6 -> GamePage(summary)
                        7 -> ReceiptPage(periodName, summary)
                        else -> EndPage(periodName)
                    }
                }
            }
        }
    }
}

// ---- Shared visuals --------------------------------------------------------------------------

/** Four dark tones from the top game's colour, drifting slowly. */
@Composable
private fun MeshBackdrop(tone: Color) {
    val t = rememberInfiniteTransition(label = "mesh")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    val a = lerp(tone, Color.Black, 0.55f)
    val b = lerp(tone, Color(0xFF1B1430), 0.7f)
    val c = lerp(tone, Color.Black, 0.82f)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(c)
        drawCircle(Brush.radialGradient(listOf(a, Color.Transparent), center = Offset(size.width * (0.2f + 0.3f * drift), size.height * 0.25f), radius = size.width), size.width, Offset(size.width * (0.2f + 0.3f * drift), size.height * 0.25f))
        drawCircle(Brush.radialGradient(listOf(b, Color.Transparent), center = Offset(size.width * (0.9f - 0.3f * drift), size.height * 0.8f), radius = size.width * 1.1f), size.width * 1.1f, Offset(size.width * (0.9f - 0.3f * drift), size.height * 0.8f))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
    }
}

@Composable
private fun DotGrid() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 18.dp.toPx()
        val r = 1.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(Color.White.copy(alpha = 0.06f), r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/** A giant word in rows behind everything, sliding slowly. */
@Composable
private fun GiantWords(word: String) {
    val t = rememberInfiniteTransition(label = "giant")
    val slide by t.animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "giantSlide")
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val rowH = 150.dp
        val rows = (maxHeight / rowH).toInt() + 2
        Column(Modifier.offset(y = -rowH * slide)) {
            repeat(rows + 1) { i ->
                Text(
                    "$word $word",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 178.sp, lineHeight = 150.sp, letterSpacing = (-0.06).em),
                    color = Color.White.copy(alpha = 0.035f),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = if (i % 2 == 0) (-60).dp else (-160).dp),
                )
            }
        }
    }
}

/** Counts up to [target] (like an odometer settling). */
@Composable
private fun rollingNumber(target: Long, durationMs: Int = 1400, delayMs: Int = 250): Long {
    val a = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        delay(delayMs.toLong())
        a.animateTo(target.toFloat(), tween(durationMs, easing = Emphasized))
    }
    return a.value.roundToInt().toLong()
}

/** Rises in from below after [delayMs]. */
@Composable
private fun Modifier.rise(delayMs: Int): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        a.animateTo(1f, tween(700, easing = Emphasized))
    }
    return graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 40.dp.toPx()
    }
}

private fun big(size: TextUnit, weight: FontWeight = FontWeight.Bold, tracking: Float = -0.03f) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size, lineHeight = size * 1.05f, letterSpacing = tracking.em)

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { content() }
}

@Composable
private fun TapHint(text: String = "tap to continue") {
    Box(Modifier.fillMaxSize()) {
        Text(text, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp), color = Color.White.copy(alpha = 0.45f), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).rise(1600))
    }
}

// ---- Pages -----------------------------------------------------------------------------------

@Composable
private fun IntroPage(period: String) {
    Centered {
        Text("RETROHERO", style = TextStyle(fontFamily = PixelFont, fontSize = 13.sp), color = Color.White.copy(alpha = 0.7f), modifier = Modifier.rise(100))
        Spacer(Modifier.height(14.dp))
        Text("Your", style = big(44.sp), color = Color.White, modifier = Modifier.rise(250))
        Text(period, style = big(56.sp, FontWeight.ExtraBold, -0.05f), color = Color.White, modifier = Modifier.rise(400))
        Text("in games", style = big(44.sp), color = Color.White.copy(alpha = 0.72f), modifier = Modifier.rise(550))
    }
    TapHint("tap to start")
}

@Composable
private fun HeyPage() {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(127.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(Brush.linearGradient(listOf(StatsAccent, Color(0xFF46E68F)))),
            contentAlignment = Alignment.Center,
        ) { Text("▶", style = big(54.sp), color = Color(0xFF0E1A0C)) }
        Spacer(Modifier.height(22.dp))
        Text("Hey, player", style = big(26.sp), color = Color.White, modifier = Modifier.rise(400))
        Text("Let's look back.", style = big(18.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.6f), modifier = Modifier.rise(600))
    }
}

@Composable
private fun JourneyPage(s: PlaySummary) {
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    Centered {
        Text("your journey started", style = big(22.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.72f), modifier = Modifier.rise(100))
        Spacer(Modifier.height(10.dp))
        Text(s.firstDay?.format(fmt) ?: "today", style = big(44.sp), color = Color.White, modifier = Modifier.rise(300))
        Spacer(Modifier.height(28.dp))
        Text("and you came back", style = big(22.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.72f), modifier = Modifier.rise(800))
        val n = rollingNumber(s.sessions.toLong(), delayMs = 900)
        Text("$n ${if (s.sessions == 1) "time" else "times"}", style = big(44.sp), color = Color.White, modifier = Modifier.rise(900))
    }
}

@Composable
private fun TotalPage(period: String, s: PlaySummary) {
    val hours = s.totalSeconds / 3600
    val minutes = s.totalSeconds / 60
    val playedDays = s.days.count { it.second > 0 }
    val share = if (s.days.isEmpty()) 0f else playedDays.toFloat() / s.days.size
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(600); fill.animateTo(share, tween(1600, easing = Emphasized)) }
    Centered {
        Text("$period was", style = big(44.sp), color = Color.White, modifier = Modifier.rise(100))
        val shown = rollingNumber(if (hours >= 2) hours else minutes, durationMs = 1800, delayMs = 400)
        Text("$shown", style = big(104.sp, FontWeight.ExtraBold, -0.04f), color = Color.White, modifier = Modifier.rise(300))
        Text(if (hours >= 2) "hours of play" else "minutes of play", style = big(18.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.7f), modifier = Modifier.rise(400))
        Spacer(Modifier.height(36.dp))
        // Days played bar: white track, green gradient fill with a glowing edge.
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).rise(700)) {
            Box(
                Modifier.fillMaxWidth(fill.value.coerceAtLeast(0.02f)).height(14.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF46E68F), Color(0xFF88FF76), Color(0xFFD4FF73)))),
            )
        }
        Text("$playedDays out of ${s.days.size} days", style = big(15.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 10.dp).rise(900))
    }
}

@Composable
private fun WeekdayPage(s: PlaySummary) {
    val day = s.days.groupBy { it.first.dayOfWeek }.maxByOrNull { (_, v) -> v.sumOf { it.second } }?.key
    Centered {
        Text("you played most on", style = big(30.sp), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.rise(100))
        Text(day?.getDisplayName(JTextStyle.FULL, Locale.getDefault())?.uppercase() ?: "—", style = big(62.sp, FontWeight.ExtraBold, -0.04f), color = Color.White, maxLines = 1, modifier = Modifier.rise(300))
        Text("your favourite day", style = big(22.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.6f), modifier = Modifier.rise(500))
        s.busiestHour?.let { h ->
            Spacer(Modifier.height(24.dp))
            Text("usually around ${if (h == 0) "midnight" else if (h < 12) "${h}am" else if (h == 12) "noon" else "${h - 12}pm"}", style = big(20.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.6f), modifier = Modifier.rise(800))
        }
    }
}

/** Your systems spin like a slot machine and land on the one you played most. */
@Composable
private fun SlotPage(s: PlaySummary) {
    val systems = s.systems.map { it.first }.ifEmpty { return }
    val laps = 3
    val reel = remember { Animatable(0f) }
    val target = (laps * systems.size).toFloat()
    LaunchedEffect(Unit) { delay(300); reel.animateTo(target, tween(3600, easing = SlotEase)) }
    val landed = reel.value >= target - 0.01f
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("your system was", style = big(26.sp), color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(24.dp))
        val tile = 120.dp
        Box(Modifier.size(tile * 1.2f, tile * 2.2f).clipToBounds(), contentAlignment = Alignment.Center) {
            for (k in -2..2) {
                val pos = reel.value
                val i = Math.floorMod(pos.toInt() + k, systems.size)
                val frac = pos - pos.toInt()
                val dy = (k - frac) * tile.value * 1.05f
                val sys = systems[i]
                val near = 1f - (abs(k - frac) / 2f).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .size(tile)
                        .offset(y = dy.dp)
                        .graphicsLayer { scaleX = 0.7f + 0.3f * near; scaleY = 0.7f + 0.3f * near; alpha = 0.3f + 0.7f * near }
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(lerp(sys.color, Color.White, 0.15f), lerp(sys.color, Color.Black, 0.5f)))),
                    contentAlignment = Alignment.Center,
                ) { Text(sys.shortName, style = TextStyle(fontFamily = PixelFont, fontSize = 13.sp), color = Color.White) }
            }
        }
        Spacer(Modifier.height(24.dp))
        val top = systems.first()
        Text(if (landed) top.name else " ", style = big(30.sp), color = Color.White, textAlign = TextAlign.Center)
        Text(if (landed) PlayTime.format(s.systems.first().second) else " ", style = big(18.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.6f))
    }
}

/** The top game: its cover swings in tilted and straightens. */
@Composable
private fun GamePage(s: PlaySummary) {
    val (game, secs) = s.topGame ?: return
    val tilt = remember { Animatable(-30f) }
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        slide.animateTo(0f, tween(900, easing = Emphasized))
        tilt.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("your game", style = big(26.sp), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.rise(100))
        Spacer(Modifier.height(26.dp))
        GameArt(
            game,
            Modifier
                .size(230.dp)
                .graphicsLayer { rotationZ = tilt.value; translationX = slide.value * size.width; shadowElevation = 30f }
                .clip(RoundedCornerShape(30.dp)),
            showTitle = true,
        )
        Spacer(Modifier.height(26.dp))
        Text(game.title, style = big(30.sp), color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.rise(900))
        Text("${PlayTime.format(secs)} · ${game.system?.name.orEmpty()}", style = big(16.sp, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.6f), modifier = Modifier.rise(1100))
    }
}

/** A paper bill of your top games. */
@Composable
private fun ReceiptPage(period: String, s: PlaySummary) {
    val paper = Color(0xFFEEECE6)
    val ink = Color(0xFF1B1B1B)
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ink)
    val drop = remember { Animatable(-1f) }
    LaunchedEffect(Unit) { drop.animateTo(0f, tween(900, easing = Emphasized)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().graphicsLayer { translationY = drop.value * size.height }) {
            Zigzag(paper, top = true)
            Column(Modifier.fillMaxWidth().background(paper).padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text("RETROHERO ARCADE", style = mono.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(period.uppercase(), style = mono, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("-".repeat(34), style = mono, maxLines = 1)
                s.games.take(7).forEach { (g, secs) ->
                    val count = s.days.size.coerceAtLeast(1)
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        GameArt(g, Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)), showTag = false)
                        Spacer(Modifier.width(8.dp))
                        Text(g.title, style = mono, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(" ${"%.1f".format(secs / 3600f)}h", style = mono)
                    }
                }
                Text("-".repeat(34), style = mono, maxLines = 1)
                Row {
                    Text("TOTAL", style = mono.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    Text(PlayTime.format(s.totalSeconds).uppercase(), style = mono.copy(fontWeight = FontWeight.Bold))
                }
                Row {
                    Text("SESSIONS", style = mono, modifier = Modifier.weight(1f))
                    Text("${s.sessions}", style = mono)
                }
                Text("THANK YOU FOR PLAYING", style = mono, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
            Zigzag(paper, top = false)
        }
    }
}

@Composable
private fun Zigzag(color: Color, top: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val tooth = 10.dp.toPx()
        val path = Path().apply {
            if (top) {
                moveTo(0f, size.height)
                var x = 0f
                while (x < size.width) { lineTo(x + tooth / 2, 0f); lineTo(x + tooth, size.height); x += tooth }
                close()
            } else {
                moveTo(0f, 0f)
                var x = 0f
                while (x < size.width) { lineTo(x + tooth / 2, size.height); lineTo(x + tooth, 0f); x += tooth }
                close()
            }
        }
        drawPath(path, color)
    }
}

@Composable
private fun EndPage(period: String) {
    Centered {
        Text("There you go", style = big(34.sp), color = Color.White.copy(alpha = 0.72f), modifier = Modifier.rise(100))
        Text("That's your $period", style = big(44.sp), color = Color.White, modifier = Modifier.rise(300))
    }
    TapHint("tap to finish")
}
