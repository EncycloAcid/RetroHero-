package com.retrohero.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.CoverArt
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.ShelfBucket
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.InterDisplay
import com.retrohero.app.ui.theme.Tone
import kotlin.math.roundToInt

/**
 * Distinct Stats Dashboard Page:
 * 1) Segmented Donut Arc Gauge + Tabular Play-Time Hero Card
 * 2) Staggered 3-Pedestal "Hall of Fame" Podium for top-played titles
 * 3) 2x2 Bento Insights Grid
 * 4) Proportional Console Share Bar Chart
 */
@Composable
internal fun StatsDashboardPage(
    games: List<Game>,
    buckets: Map<ShelfBucket, List<Game>>,
    favoriteCount: Int,
    stats: Map<String, PlayStat>,
    onOpenCase: (Game) -> Unit,
) {
    val navSpace = LocalNavSpace.current
    val totalSeconds = remember(stats) { stats.values.sumOf { it.seconds } }
    val totalSessions = remember(stats) { stats.values.sumOf { it.sessions } }
    val playingCount = buckets[ShelfBucket.Playing]?.size ?: 0
    val completedCount = buckets[ShelfBucket.Completed]?.size ?: 0
    val backlogCount = buckets[ShelfBucket.Backlog]?.size ?: 0
    val totalGames = games.size
    val completionPct = if (totalGames > 0) ((completedCount * 100f) / totalGames).roundToInt() else 0

    val ranked = remember(games, stats) {
        games.sortedByDescending { stats[it.id]?.seconds ?: 0 }
    }
    val podium = ranked.take(3)

    val bySystem = remember(games) {
        games.groupingBy { it.systemId }.eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }
    val topConsoleName = bySystem.firstOrNull()?.let { Systems.get(it.key)?.shortName ?: it.key.uppercase() } ?: "—"

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = navSpace + 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Editorial Telemetry Header
        item(key = "stats_header") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "PLAY JOURNAL",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.14.em),
                    color = Ascend.accent,
                )
                Text(
                    text = "Stats & Insights",
                    style = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.04).em),
                    color = Tone.Ink,
                )
            }
        }

        // 1. Segmented Circular Donut Ring + Playtime Readout Hero
        item(key = "stats_ring_hero") {
            val heroShape = RoundedCornerShape(20.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Tone.Surface, Tone.Raised.copy(alpha = 0.65f)),
                        ),
                    )
                    .border(1.dp, Tone.Line, heroShape)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    val accent = Ascend.accent
                    val mint = Color(0xFF3FD8A4)
                    val slate = Color(0xFF3E4554)
                    val track = Tone.Line
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 11.dp.toPx()
                        val inset = stroke / 2f
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        val topLeft = Offset(inset, inset)
                        drawArc(
                            color = track,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke),
                        )
                        val sum = (playingCount + completedCount + backlogCount).coerceAtLeast(1).toFloat()
                        var cursor = -90f
                        val segments = listOf(
                            playingCount / sum to accent,
                            completedCount / sum to mint,
                            backlogCount / sum to slate,
                        ).filter { it.first > 0f }
                        segments.forEach { (frac, col) ->
                            val sweep = (frac * 360f - 5f).coerceAtLeast(4f)
                            drawArc(
                                color = col,
                                startAngle = cursor,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                            cursor += frac * 360f
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$completionPct%",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, letterSpacing = (-0.02).em),
                            color = Tone.Ink,
                        )
                        Text(
                            text = "CLEARED",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, letterSpacing = 0.12.em),
                            color = Tone.Muted,
                        )
                    }
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (totalSeconds > 0) PlayTime.format(totalSeconds) else "0m",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.03).em),
                            color = Tone.Ink,
                        )
                        Text(
                            text = "Total time logged · $totalSessions ${if (totalSessions == 1) "session" else "sessions"}",
                            style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                            color = Tone.Muted,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        LegendDotRow("Playing", playingCount, Ascend.accent)
                        LegendDotRow("Completed", completedCount, Color(0xFF3FD8A4))
                        LegendDotRow("Backlog", backlogCount, Color(0xFF646E82))
                    }
                }
            }
        }

        // 2. Staggered 3-Pedestal Podium (when at least 2 games exist)
        if (podium.size >= 2) {
            item(key = "stats_podium") {
                val shape = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, shape)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Hall of Fame", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp), color = Tone.Ink)
                        Text("Tap cover to open case", style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = Tone.Muted)
                    }

                    val ordered = listOfNotNull(
                        podium.getOrNull(1)?.let { Triple(it, 2, 16.dp) },
                        podium.getOrNull(0)?.let { Triple(it, 1, 0.dp) },
                        podium.getOrNull(2)?.let { Triple(it, 3, 26.dp) },
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ordered.forEach { (game, rank, topOffset) ->
                            val sec = stats[game.id]?.seconds ?: 0
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(top = topOffset)
                                    .clickable { onOpenCase(game) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    Modifier
                                        .width(if (rank == 1) 88.dp else 74.dp)
                                        .height(if (rank == 1) 124.dp else 104.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (rank == 1) 1.5.dp else 1.dp,
                                            color = if (rank == 1) Ascend.accent else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp),
                                        ),
                                ) {
                                    CoverArt(game, Modifier.fillMaxSize())
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(5.dp)
                                            .size(21.dp)
                                            .clip(CircleShape)
                                            .background(if (rank == 1) Ascend.accent else Color.Black.copy(alpha = 0.78f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "#$rank",
                                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                                            color = Color.White,
                                        )
                                    }
                                }
                                Text(
                                    text = game.title,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                    color = Tone.Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (sec > 0) PlayTime.format(sec) else (game.system?.shortName ?: game.systemId.uppercase()),
                                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                                    color = if (rank == 1) Ascend.accent else Tone.Muted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 2x2 Bento Insights Grid
        item(key = "stats_bento") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoStatTile(
                        icon = Icons.Rounded.Layers,
                        value = totalGames.toString(),
                        label = "Indexed ROMs",
                        sub = "${bySystem.size} active consoles",
                        modifier = Modifier.weight(1f),
                    )
                    BentoStatTile(
                        icon = Icons.Rounded.Bolt,
                        value = topConsoleName,
                        label = "Top Platform",
                        sub = "${bySystem.firstOrNull()?.value ?: 0} titles in vault",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoStatTile(
                        icon = Icons.Rounded.EmojiEvents,
                        value = "$completedCount",
                        label = "Completed",
                        sub = "$playingCount currently in rotation",
                        modifier = Modifier.weight(1f),
                    )
                    BentoStatTile(
                        icon = Icons.Rounded.Favorite,
                        value = favoriteCount.toString(),
                        label = "Favourites",
                        sub = "Hearted classics",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 4. Console Distribution Bars
        if (bySystem.isNotEmpty()) {
            item(key = "stats_consoles") {
                val maxCount = (bySystem.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, shape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Platform Share", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.5.sp), color = Tone.Ink)
                    bySystem.forEachIndexed { idx, (sysId, count) ->
                        val sysName = Systems.get(sysId)?.name ?: sysId.uppercase()
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sysName, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp), color = Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$count", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp), color = Tone.Muted)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(Tone.Raised),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((count.toFloat() / maxCount).coerceIn(0.08f, 1f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(if (idx == 0) Ascend.accent else Ascend.accent.copy(alpha = 0.55f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDotRow(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
            color = Tone.Muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = Tone.Ink,
        )
    }
}

@Composable
private fun BentoStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    sub: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Ascend.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Ascend.accent, modifier = Modifier.size(17.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.02).em), color = Tone.Ink, maxLines = 1)
            Text(label, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = Tone.Ink, maxLines = 1)
            Text(sub, style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = Tone.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
