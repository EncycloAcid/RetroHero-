package com.retrohero.app.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/** Height of the Console OS quick-resume capsule plus bottom clearance. */
val MiniPlayerSpace = 62.dp + 12.dp

/**
 * Handheld Console OS quick-resume capsule docked near the bottom edge (no tab bar beneath it).
 * Tap or swipe up to open the game page, press Resume to jump straight in, swipe down to dismiss.
 */
@Composable
fun MiniPlayer(
    game: Game,
    stat: PlayStat?,
    glass: HazeState?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 500.dp)
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                translationY = drag.value
                alpha = 1f - (drag.value / 120.dp.toPx()).coerceIn(0f, 0.8f)
            }
            .shadow(
                elevation = if (Tone.isDark) 12.dp else 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (Tone.isDark) 0.55f else 0.14f),
                spotColor = Ascend.accent.copy(alpha = if (Tone.isDark) 0.35f else 0.12f),
            )
            .background(Tone.Surface, shape)
            .border(1.25.dp, Ascend.accent.copy(alpha = 0.45f), shape)
            .clip(shape)
            .then(if (glass != null) Modifier.frostedGlass(glass, shape) { 1f } else Modifier)
            .pointerInput(game.id) {
                val tracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDragEnd = {
                        val v = tracker.calculateVelocity().y / density
                        val d = drag.value / density
                        scope.launch {
                            when {
                                d < -36f || v < -300f -> { onOpen(); drag.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f)) }
                                d > 48f || v > 900f -> { drag.animateTo(140.dp.toPx(), spring(dampingRatio = 1f, stiffness = 420f)); onHide(); drag.snapTo(0f) }
                                else -> drag.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f))
                            }
                        }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(0f) } },
                ) { change, dy ->
                    change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val next = drag.value + if (drag.value + dy < 0) dy * 0.45f else dy
                    scope.launch { drag.snapTo(next) }
                }
            }
            .clickable(onClick = onOpen)
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt(
            game,
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.5.dp, Ascend.accent, CircleShape),
            showTag = false,
        )
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.horizontalGradient(0.85f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Ascend.accent))
                Text(
                    "QUICK RESUME · ${game.system?.shortName ?: game.systemId.uppercase()}",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 0.08.em),
                    color = Ascend.accentText,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            val sub = buildString {
                append(game.title)
                if (stat != null && stat.lastPlayed > 0) {
                    append(" · ")
                    append(DateUtils.getRelativeTimeSpanString(stat.lastPlayed, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
                }
            }
            Text(
                sub,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = Tone.Ink,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .height(42.dp)
                .clip(CircleShape)
                .background(Ascend.accent)
                .clickable(onClick = onPlay)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                "Resume ${game.title}",
                tint = Ascend.onAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Play",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                color = Ascend.onAccent,
            )
        }
    }
}
