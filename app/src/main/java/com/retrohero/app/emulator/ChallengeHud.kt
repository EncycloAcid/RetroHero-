package com.retrohero.app.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.ChallengeGhost

fun formatChallengeClock(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val mins = (safe / 60_000L).toInt()
    val secs = ((safe % 60_000L) / 1000L).toInt()
    val tenths = ((safe % 1000L) / 100L).toInt()
    return if (mins > 0) "%d:%02d.%d".format(mins, secs, tenths) else "%d.%ds".format(secs, tenths)
}

/** Floating pill shown while recording a Live Challenge Snapshot input ghost. */
@Composable
fun ChallengeRecordingPill(
    elapsedMs: Long,
    onFinishAndShare: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xE6B3261E))
            .border(1.dp, Color(0x66FFFFFF), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(12.dp))
        Text(
            "REC GHOST · ${formatChallengeClock(elapsedMs)}",
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
        )
        Row(
            Modifier
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onFinishAndShare)
                .padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(Icons.Rounded.Stop, null, tint = Color(0xFFB3261E), modifier = Modifier.size(12.dp))
            Text("Finish", color = Color(0xFFB3261E), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Icon(
            Icons.Rounded.Close,
            "Cancel recording",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp).clickable(onClick = onCancel),
        )
    }
}

/**
 * Floating HUD shown while playing an imported Playable Challenge Snapshot (.rhclip v2).
 * Displays live split clock, creator ghost input visualizer, instant retry, and victory banner.
 */
@Composable
fun ChallengePlayHud(
    title: String,
    elapsedMs: Long,
    targetMs: Long,
    ghostButtonsMask: Int,
    hasGhost: Boolean,
    winGoalText: String,
    cleared: Boolean,
    clearedTimeMs: Long?,
    onInstantRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val ghostKeys = ChallengeGhost.activeLabels(ghostButtonsMask)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(if (cleared) Color(0xE81E6E3E) else Color(0xD912151E))
                .border(1.dp, if (cleared) Color(0xAA7CF2A5) else Color(0x4DFFFFFF), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (cleared) Icons.Rounded.CheckCircle else Icons.Rounded.EmojiEvents,
                null,
                tint = if (cleared) Color(0xFF8CFFAF) else cs.primary,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = if (cleared) {
                    "CLEARED! ${formatChallengeClock(clearedTimeMs ?: elapsedMs)}"
                } else if (targetMs > 0L) {
                    "$title · ${formatChallengeClock(elapsedMs)} / ${formatChallengeClock(targetMs)}"
                } else {
                    "$title · ${formatChallengeClock(elapsedMs)}"
                },
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onInstantRetry)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(Icons.Rounded.RestartAlt, "Instant retry", tint = Color.White, modifier = Modifier.size(13.dp))
                Text("Retry", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if ((hasGhost && !cleared) || winGoalText.isNotBlank()) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xB812151E))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (hasGhost && !cleared) {
                    Text(
                        "GHOST:",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (ghostKeys.isEmpty()) "·" else ghostKeys.joinToString(" "),
                        color = Color(0xFF90CAF9),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                if (winGoalText.isNotBlank() && !cleared) {
                    Text(
                        text = winGoalText,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
