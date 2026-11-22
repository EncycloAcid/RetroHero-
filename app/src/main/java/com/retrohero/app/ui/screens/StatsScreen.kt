package com.retrohero.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.HistoryEntry
import com.retrohero.app.data.PlayHistory
import com.retrohero.app.data.PlaySummary
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.theme.Inter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// Mochy's stats palette.
val StatsAccent = Color(0xFF88FF76)
private val Ink2 = Color(0xFFB8B8B8)
private val Muted = Color(0xFF7A7A7A)
private val Grid = Color(0xFF242424)
private val BarRest = Color(0xFF3A3A3A)
private val StatsCardFill = Color(0xFF0F0F0F)
private val Donut = listOf(Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500), Color(0xFFD55181))
private val DonutOther = Color(0xFF5A5A5A)
private val ChartEase = MochyEase

/** A range of days to summarise. */
data class Period(val name: String, val from: LocalDate?, val to: LocalDate, val month: LocalDate? = null)

fun statsPeriods(today: LocalDate = LocalDate.now()): List<Period> = buildList {
    add(Period("This week", today.minusDays(6), today))
    for (back in 0..5) {
        val m = today.withDayOfMonth(1).minusMonths(back.toLong())
        val end = if (back == 0) today else m.plusMonths(1).minusDays(1)
        add(Period(m.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()), m, end, month = m))
    }
    add(Period(today.year.toString(), today.withDayOfYear(1), today))
    add(Period("All time", null, today))
}

/** Play-time stats, styled like the music app's: a period ruler, then expandable cards. */
@Composable
fun StatsScreen(history: List<HistoryEntry>, onBack: () -> Unit, onRecap: (Period, PlaySummary) -> Unit) {
    val periods = remember { statsPeriods() }
    var index by rememberSaveable { mutableIntStateOf(1) }
    val period = periods[index]
    val summary = remember(history, index) { PlayHistory.summarise(history, period.from, period.to) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item {
            Row(
                Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2C2C2E)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = Color.White, modifier = Modifier.size(18.dp).padding(start = 3.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (summary.totalSeconds > 0) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                        .clickable(enabled = summary.totalSeconds > 0) { onRecap(period, summary) }
                        .padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Re'cap", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color.White)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { PeriodRuler(periods, index, history) { index = it } }
        item {
            StatCard("Play time", if (summary.totalSeconds > 0) PlayTime.format(summary.totalSeconds) else "Nothing yet", initiallyOpen = true) {
                LineChart(summary.days.map { it.second.toFloat() }, Modifier.fillMaxWidth().height(150.dp))
                Text("${summary.sessions} sessions · best streak ${summary.bestStreak} ${if (summary.bestStreak == 1) "day" else "days"}", style = axis, color = Ink2, modifier = Modifier.padding(top = 10.dp))
            }
        }
        if (summary.systems.isNotEmpty()) item {
            StatCard("Systems", "Mostly ${summary.topSystem!!.first.name}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(summary.systems.map { it.second.toFloat() }, Modifier.size(92.dp))
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        summary.systems.take(5).forEachIndexed { i, (s, secs) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Donut[i]))
                                Spacer(Modifier.width(8.dp))
                                Text("${s.shortName}  ${PlayTime.format(secs)}", style = axis, color = Ink2)
                            }
                        }
                    }
                }
            }
        }
        if (summary.games.isNotEmpty()) item {
            StatCard("Games", "Top: ${summary.topGame!!.first.title}") {
                val max = summary.games.first().second.toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.games.take(5).forEachIndexed { i, (g, secs) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Muted, modifier = Modifier.width(28.dp))
                            GameArt(g, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), showTag = false)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(PlayTime.format(secs), style = axis, color = Muted)
                                Box(Modifier.padding(top = 5.dp).fillMaxWidth(secs / max).height(3.dp).clip(CircleShape).background(if (i == 0) StatsAccent else Ink2.copy(alpha = 0.55f)))
                            }
                        }
                    }
                }
            }
        }
        if (summary.totalSeconds > 0) {
            val byWeekday = LongArray(7).also { w -> summary.days.forEach { (d, s) -> w[d.dayOfWeek.value - 1] += s } }.toList()
            val fav = DayOfWeek.of(byWeekday.indices.maxBy { byWeekday[it] } + 1)
            item {
                StatCard("Weekdays", "You play most on ${fav.getDisplayName(JTextStyle.FULL, Locale.getDefault())}s") {
                    BarChart(byWeekday.map { it.toFloat() }, DayOfWeek.entries.map { it.getDisplayName(JTextStyle.NARROW, Locale.getDefault()) }, Modifier.fillMaxWidth().height(130.dp))
                }
            }
            item {
                val h = summary.busiestHour ?: 0
                StatCard("Time of day", "Usually around ${hourLabel(h)}") {
                    LineChart(summary.hours.map { it.toFloat() }, Modifier.fillMaxWidth().height(150.dp), labels = listOf("12am", "6am", "12pm", "6pm", ""))
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Habit("Sessions", "${summary.sessions}", Modifier.weight(1f))
                    Habit("Longest", summary.longest?.let { PlayTime.format(it.seconds) } ?: "–", Modifier.weight(1f))
                    Habit("Streak", "${summary.bestStreak}d", Modifier.weight(1f))
                }
            }
        }
    }
}

private val axis = TextStyle(fontFamily = Inter, fontSize = 11.sp)

private fun hourLabel(h: Int) = when {
    h == 0 -> "midnight"
    h < 12 -> "${h}am"
    h == 12 -> "noon"
    else -> "${h - 12}pm"
}

/** Period names in a sliding row: the chosen one big on the left, the rest small and grey. */
@Composable
private fun PeriodRuler(periods: List<Period>, selected: Int, history: List<HistoryEntry>, onSelect: (Int) -> Unit) {
    val playedDays = remember(history) { history.map { it.day() }.toSet() }
    Column(Modifier.padding(top = 18.dp, bottom = 10.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            itemsIndexed(periods) { i, p ->
                val on = i == selected
                val has = history.any { val d = it.day(); (p.from == null || !d.isBefore(p.from)) && !d.isAfter(p.to) }
                val size by animateFloatAsState(if (on) 30f else 14.4f, tween(320, easing = ChartEase), label = "periodSize")
                Text(
                    p.name,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = size.sp, letterSpacing = (-0.035).em),
                    color = when {
                        on -> Color.White
                        has -> Color(0xFF6E6E6E)
                        else -> Color(0xFF474747)
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSelect(i) }.padding(bottom = 2.dp),
                )
            }
        }
        // Ticks: one per day of the last 8 weeks, height = whether you played.
        Canvas(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 20.dp).padding(top = 10.dp)) {
            val days = 56
            val step = size.width / days
            val today = LocalDate.now()
            for (i in 0 until days) {
                val d = today.minusDays((days - 1 - i).toLong())
                val played = d in playedDays
                val h = if (played) size.height else size.height * 0.45f
                drawLine(
                    if (played) Color.White.copy(alpha = 0.8f) else Color(0xFF474747),
                    Offset(i * step + step / 2, size.height),
                    Offset(i * step + step / 2, size.height - h),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, headline: String, initiallyOpen: Boolean = false, content: @Composable () -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(320, easing = ChartEase), label = "chev")
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StatsCardFill)
            .clickable { open = !open }
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp), color = Muted)
                Text(headline, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.03).em), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, if (open) "Collapse" else "Expand", tint = Ink2, modifier = Modifier.rotate(turn))
            }
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(420, easing = ChartEase)) + fadeIn(tween(300, delayMillis = 120)),
            exit = shrinkVertically(tween(320, easing = ChartEase)) + fadeOut(tween(160)),
        ) { Column(Modifier.padding(top = 16.dp)) { content() } }
    }
}

@Composable
private fun Habit(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(StatsCardFill).padding(16.dp)) {
        Text(label, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = Muted)
        Text(value, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.03).em), color = Color.White, maxLines = 1)
    }
}

/** Grows in from zero over 900 ms, like the music app's charts. */
@Composable
private fun rememberGrow(key: Any): Float {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(900, easing = ChartEase)) }
    return a.value
}

@Composable
fun LineChart(values: List<Float>, modifier: Modifier, labels: List<String> = emptyList()) {
    val grow = rememberGrow(values)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val n = values.size
            // Baseline and a dashed midline.
            drawLine(Grid, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            drawLine(Grid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            if (n < 2) return@Canvas
            val pts = values.mapIndexed { i, v -> Offset(i * size.width / (n - 1), size.height - (v / max) * size.height * 0.92f * grow) }
            val line = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until n) {
                    val a = pts[i - 1]
                    val b = pts[i]
                    val mx = (a.x + b.x) / 2
                    cubicTo(mx, a.y, mx, b.y, b.x, b.y)
                }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(StatsAccent.copy(alpha = 0.22f), StatsAccent.copy(alpha = 0f))))
            drawPath(line, StatsAccent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val peak = values.indices.maxBy { values[it] }
            if (values[peak] > 0) {
                drawCircle(Color(0xFF111111), 6.dp.toPx(), pts[peak])
                drawCircle(StatsAccent, 4.dp.toPx(), pts[peak])
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { Text(it, style = axis, color = Muted) }
            }
        }
    }
}

@Composable
private fun BarChart(values: List<Float>, labels: List<String>, modifier: Modifier) {
    val grow = rememberGrow(values)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val slot = size.width / values.size
            val w = minOf(slot * 0.46f, 22.dp.toPx())
            val peak = values.indices.maxBy { values[it] }
            values.forEachIndexed { i, v ->
                val h = (v / max) * size.height * grow
                val x = i * slot + (slot - w) / 2
                if (h > 0) {
                    drawRoundRect(if (i == peak) StatsAccent else BarRest, Offset(x, size.height - h), Size(w, h), CornerRadius(4.dp.toPx()))
                    // Square off the bottom corners.
                    val r = minOf(4.dp.toPx(), h)
                    drawRect(if (i == peak) StatsAccent else BarRest, Offset(x, size.height - r), Size(w, r))
                }
            }
            drawLine(Grid, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            labels.forEach { Text(it, style = axis, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DonutChart(values: List<Float>, modifier: Modifier) {
    val grow = rememberGrow(values)
    Canvas(modifier) {
        val total = values.sum().coerceAtLeast(1f)
        val parts = values.take(5) + listOfNotNull(values.drop(5).sum().takeIf { it > 0 })
        val stroke = 13.dp.toPx()
        val gap = if (parts.size > 1) 2.dp.toPx() / (size.minDimension / 2) * 57.3f else 0f
        var start = -90f
        parts.forEachIndexed { i, v ->
            val sweep = v / total * 360f * grow
            drawArc(
                Donut.getOrElse(i) { DonutOther },
                start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
            start += sweep
        }
    }
}
