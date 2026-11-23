package com.retrohero.app.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.ui.theme.Tone
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/** Height of the mini player pill, plus the gap above the nav bar. */
val MiniPlayerSpace = 64.dp + 10.dp

/**
 * The last game you played as a floating frosted pill above the nav bar, like the music app's
 * mini player. Tap or drag up to open the game page, play to jump back in, drag down to hide it.
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
    val shape = Ascend.CardShape
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                // Follows the finger: up freely (with resistance), down to dismiss.
                translationY = drag.value
                alpha = 1f - (drag.value / 120.dp.toPx()).coerceIn(0f, 0.8f)
            }
            .background(Ascend.Surface, shape)
            .border(1.dp, Tone.Line, shape)
            .clip(shape)
            .then(if (glass != null) Modifier.frostedGlass(glass, shape) { 1f } else Modifier)
            .pointerInput(game.id) {
                val tracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDragEnd = {
                        val v = tracker.calculateVelocity().y / density  // dp/s
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
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt(game, Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)), showTag = false)
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                // Text fades out at the right edge instead of being cut.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.horizontalGradient(0.82f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
        ) {
            Text(game.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Tone.Ink, maxLines = 1, softWrap = false)
            val sub = buildString {
                append(game.system?.name.orEmpty())
                if (stat != null && stat.lastPlayed > 0) {
                    append(" — ")
                    append(DateUtils.getRelativeTimeSpanString(stat.lastPlayed, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
                }
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Tone.Muted, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.PlayArrow, "Resume ${game.title}", tint = Tone.Ink, modifier = Modifier.size(26.dp)) }
    }
}
