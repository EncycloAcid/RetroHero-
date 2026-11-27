package com.retrohero.app.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.RetroPad
import java.util.Locale
import kotlin.math.abs

/** Timestamped RetroPad bitmask sample recorded during a Practice Loop run for PB Ghost replay. */
data class GhostSample(
    val ms: Long,
    val buttonsMask: Int,
)

/** Complete state of an active Boss-Fight / Hard-Spot Practice Loop session. */
data class PracticeLoopState(
    val active: Boolean = false,
    val attempts: Int = 1,
    val clears: Int = 0,
    val elapsedMs: Long = 0L,
    val bestSplitMs: Long? = null,
    val lastClearMs: Long? = null,
    val lastDeltaMs: Long? = null,
    val justHitPb: Boolean = false,
    /** 0 = Manual Retry; positive = auto-reload anchor after N seconds (10 / 15 / 30 / 60). */
    val autoResetSeconds: Int = 0,
    val ghostEnabled: Boolean = true,
    val pbGhostFrames: List<GhostSample> = emptyList(),
)

object PracticeLoopMath {

    val AUTO_RESET_OPTIONS = listOf(0, 10, 15, 30, 60)

    fun formatSplit(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val totalSec = clamped / 1000L
        val centis = (clamped % 1000L) / 10L
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return if (mins > 0L) {
            String.format(Locale.US, "%d:%02d.%02d", mins, secs, centis)
        } else {
            String.format(Locale.US, "%d.%02ds", secs, centis)
        }
    }

    fun formatDelta(deltaMs: Long): String {
        val sign = if (deltaMs <= 0L) "-" else "+"
        val absMs = abs(deltaMs)
        val secs = absMs / 1000L
        val centis = (absMs % 1000L) / 10L
        return String.format(Locale.US, "%s%d.%02ds", sign, secs, centis)
    }

    fun shouldAutoReset(elapsedMs: Long, autoResetSeconds: Int): Boolean =
        autoResetSeconds > 0 && elapsedMs >= autoResetSeconds * 1000L

    /** Binary-search the latest recorded PB ghost button mask at or before [elapsedMs]. */
    fun sampleGhostMask(frames: List<GhostSample>, elapsedMs: Long): Int {
        if (frames.isEmpty() || elapsedMs < frames.first().ms) return 0
        var lo = 0
        var hi = frames.lastIndex
        var best = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (frames[mid].ms <= elapsedMs) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return frames[best].buttonsMask
    }

    /**
     * Records a successful clear of the practiced boss/section, updating attempt/clear stats,
     * split delta vs. previous PB, and promoting [runGhost] to the new PB ghost when faster.
     */
    fun recordClear(state: PracticeLoopState, runGhost: List<GhostSample>): PracticeLoopState {
        val split = state.elapsedMs.coerceAtLeast(1L)
        val prevPb = state.bestSplitMs
        val delta = prevPb?.let { split - it }
        val isNewPb = prevPb == null || split < prevPb
        return state.copy(
            clears = state.clears + 1,
            bestSplitMs = if (isNewPb) split else prevPb,
            lastClearMs = split,
            lastDeltaMs = delta,
            justHitPb = isNewPb,
            pbGhostFrames = if (isNewPb && runGhost.isNotEmpty()) runGhost else state.pbGhostFrames,
        )
    }

    fun startRetry(state: PracticeLoopState): PracticeLoopState = state.copy(
        attempts = state.attempts + 1,
        elapsedMs = 0L,
        justHitPb = false,
    )
}

/**
 * Compact floating Boss-Fight / Hard-Spot Practice Loop HUD.
 * Gives 1-tap Retry, 1-tap Clear/Split, live millisecond timer, Attempt counter, PB split delta,
 * optional Auto-Reset countdown bar, and an optional live PB Input Ghost indicator.
 */
@Composable
fun PracticeLoopHud(
    state: PracticeLoopState,
    ghostMask: Int,
    onRetry: () -> Unit,
    onClearSplit: () -> Unit,
    onOpenSettings: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.Black.copy(alpha = 0.78f),
            modifier = Modifier.border(
                width = 1.dp,
                color = if (state.justHitPb) Color(0xFF69F0AE).copy(alpha = 0.75f) else cs.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp),
            ),
        ) {
            Column(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        Icons.Rounded.Loop,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = PracticeLoopMath.formatSplit(state.elapsedMs),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = "#${state.attempts}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    val pb = state.bestSplitMs
                    if (pb != null) {
                        val delta = state.lastDeltaMs
                        val badgeBg = when {
                            state.justHitPb -> Color(0xFF00C853).copy(alpha = 0.28f)
                            delta != null && delta <= 0L -> Color(0xFF00C853).copy(alpha = 0.22f)
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                        val badgeFg = when {
                            state.justHitPb -> Color(0xFF69F0AE)
                            delta != null && delta <= 0L -> Color(0xFF69F0AE)
                            else -> Color(0xFFFFD54F)
                        }
                        val badgeText = when {
                            state.justHitPb && delta != null -> "PB ${PracticeLoopMath.formatDelta(delta)}"
                            state.justHitPb -> "NEW PB!"
                            else -> "PB ${PracticeLoopMath.formatSplit(pb)}"
                        }
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(badgeBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                ),
                                color = badgeFg,
                                maxLines = 1,
                            )
                        }
                    }

                    // 1-tap instant Retry button
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.primary)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(Icons.Rounded.Replay, "Retry loop", tint = cs.onPrimary, modifier = Modifier.size(13.dp))
                        Text(
                            "Retry",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = cs.onPrimary,
                        )
                    }

                    // 1-tap Clear / Record PB Split button
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF00C853).copy(alpha = 0.85f))
                            .clickable(onClick = onClearSplit)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Flag, "Mark clear & split", tint = Color.Black, modifier = Modifier.size(13.dp))
                    }

                    // Settings & Close
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(onClick = onOpenSettings)
                            .padding(4.dp),
                    ) {
                        Icon(Icons.Rounded.Tune, "Practice settings", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(onClick = onStop)
                            .padding(4.dp),
                    ) {
                        Icon(Icons.Rounded.Close, "Exit practice loop", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }

                // Auto-reset countdown bar if enabled
                if (state.autoResetSeconds > 0) {
                    val progress = (state.elapsedMs.toFloat() / (state.autoResetSeconds * 1000f)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.width(210.dp).height(3.dp).clip(CircleShape),
                        color = if (progress > 0.8f) Color(0xFFFF6E40) else cs.primary,
                        trackColor = Color.White.copy(alpha = 0.16f),
                    )
                }
            }
        }

        // Optional live PB Input Ghost replay pill
        if (state.ghostEnabled && state.pbGhostFrames.isNotEmpty()) {
            PracticeGhostPill(buttonsMask = ghostMask)
        }
    }
}

@Composable
private fun PracticeGhostPill(buttonsMask: Int) {
    val active = buildList {
        if ((buttonsMask and (1 shl RetroPad.LEFT)) != 0) add("◄")
        if ((buttonsMask and (1 shl RetroPad.UP)) != 0) add("▲")
        if ((buttonsMask and (1 shl RetroPad.DOWN)) != 0) add("▼")
        if ((buttonsMask and (1 shl RetroPad.RIGHT)) != 0) add("►")
        if ((buttonsMask and (1 shl RetroPad.B)) != 0) add("B")
        if ((buttonsMask and (1 shl RetroPad.A)) != 0) add("A")
        if ((buttonsMask and (1 shl RetroPad.Y)) != 0) add("Y")
        if ((buttonsMask and (1 shl RetroPad.X)) != 0) add("X")
        if ((buttonsMask and (1 shl RetroPad.L)) != 0) add("L")
        if ((buttonsMask and (1 shl RetroPad.R)) != 0) add("R")
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, Color(0xFF80D8FF).copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            "PB GHOST",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
            color = Color(0xFF80D8FF),
        )
        Text(
            text = if (active.isEmpty()) "·" else active.joinToString(" "),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** Material 3 configuration sheet for Practice Loop Mode (Anchor, Auto-Reset Timer, Ghost Replay). */
@Composable
fun PracticeLoopSheet(
    state: PracticeLoopState,
    onDropAnchor: () -> Unit,
    onSetAutoReset: (Int) -> Unit,
    onToggleGhost: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onStopLoop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = cs.surfaceContainer,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .clickable(interactionSource = null, indication = null) {},
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(cs.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Loop, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Practice Loop Mode", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Drop an anchor right before a boss or tough jump · Attempts: ${state.attempts} · Clears: ${state.clears}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                // Auto-reset timer chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Auto-Reset Timer",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = cs.onSurface,
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PracticeLoopMath.AUTO_RESET_OPTIONS.forEach { sec ->
                            FilterChip(
                                selected = state.autoResetSeconds == sec,
                                onClick = { onSetAutoReset(sec) },
                                label = { Text(if (sec == 0) "Manual" else "${sec}s") },
                            )
                        }
                    }
                }

                // PB Ghost Overlay toggle
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("PB Input Ghost Overlay", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Replays your Personal Best button presses live beside the timer",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(checked = state.ghostEnabled, onCheckedChange = onToggleGhost)
                }

                Button(
                    onClick = {
                        onDropAnchor()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Loop, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.active) "Move Loop Anchor Here" else "Drop Loop Anchor & Start")
                }

                if (state.active) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onResetStats,
                            modifier = Modifier.weight(1f),
                        ) { Text("Reset PB") }
                        FilledTonalButton(
                            onClick = {
                                onStopLoop()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Stop Loop") }
                    }
                }
            }
        }
    }
}
