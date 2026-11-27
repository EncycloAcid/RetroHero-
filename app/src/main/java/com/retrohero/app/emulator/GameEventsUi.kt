package com.retrohero.app.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Checkpoints
import com.retrohero.app.data.GameEvent
import com.retrohero.app.data.GameEventDetector
import com.retrohero.app.data.RichPresence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the events engine can do for the current game. */
enum class GameEventsStatus { NOT_SIGNED_IN, LOADING, NO_DATA, ACTIVE, HARDCORE }

/** Shown for a few seconds after a death: one tap goes back to the last smart checkpoint. */
@Composable
fun DeathRetryChip(checkpoint: Checkpoints.Entry, onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.85f),
        contentColor = Color.White,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text("You died", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Checkpoint: ${checkpoint.label}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            TextButton(onClick = onRetry) { Text("Retry") }
            TextButton(onClick = onDismiss) { Text("✕", color = Color.White.copy(alpha = 0.7f)) }
        }
    }
}

@Composable
fun GameEventsSheet(
    status: GameEventsStatus,
    presenceText: String,
    values: List<RichPresence.Value>,
    events: List<GameEvent>,
    checkpoints: List<Checkpoints.Entry>,
    smartCheckpoints: Boolean,
    onSetSmartCheckpoints: (Boolean) -> Unit,
    onLoadCheckpoint: (Checkpoints.Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
        title = { Text("Game events") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    when (status) {
                        GameEventsStatus.NOT_SIGNED_IN -> "Sign in to RetroAchievements (free) in Settings. RetroHero uses its game data to tell when you die, reach a new level or start a boss fight."
                        GameEventsStatus.LOADING -> "Loading game data from RetroAchievements…"
                        GameEventsStatus.NO_DATA -> "RetroAchievements has no Rich Presence data for this game, so events aren't available. Achievements still work."
                        GameEventsStatus.ACTIVE -> "Watching this game. Checkpoints are saved when a new level or boss fight starts, and after a death you can jump back with one tap."
                        GameEventsStatus.HARDCORE -> "Hardcore mode doesn't allow loading states, so events are logged but no checkpoints are made."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Smart checkpoints", style = MaterialTheme.typography.titleSmall)
                        Text("Up to ${Checkpoints.MAX}, separate from your save slots", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = smartCheckpoints, onCheckedChange = onSetSmartCheckpoints)
                }
                if (presenceText.isNotBlank() || values.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Now", style = MaterialTheme.typography.titleSmall)
                    if (presenceText.isNotBlank()) Text(presenceText, style = MaterialTheme.typography.bodyMedium)
                    values.filter { GameEventDetector.kindOf(it.label) != GameEventDetector.Kind.OTHER }.forEach { v ->
                        val kind = when (GameEventDetector.kindOf(v.label)) {
                            GameEventDetector.Kind.LIVES -> "lives"
                            GameEventDetector.Kind.HEALTH -> "health"
                            GameEventDetector.Kind.LEVEL -> "level"
                            GameEventDetector.Kind.OTHER -> ""
                        }
                        Text("${v.label}: ${v.text.ifBlank { v.raw.toString() }} · tracked as $kind", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (checkpoints.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Checkpoints", style = MaterialTheme.typography.titleSmall)
                    checkpoints.forEach { cp ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(cp.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(time.format(Date(cp.atMs)), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onLoadCheckpoint(cp) }, enabled = status != GameEventsStatus.HARDCORE) { Text("Load") }
                        }
                    }
                }
                if (events.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Recent events", style = MaterialTheme.typography.titleSmall)
                    events.asReversed().take(30).forEach { e ->
                        Text("${time.format(Date(e.wallMs()))}  ${e.label}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
    )
}

/** Events carry elapsedRealtime timestamps; convert for display. */
private fun GameEvent.wallMs(): Long =
    System.currentTimeMillis() - (android.os.SystemClock.elapsedRealtime() - atMs)
