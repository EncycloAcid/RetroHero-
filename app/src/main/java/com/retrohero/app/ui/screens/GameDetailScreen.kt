package com.retrohero.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.components.rememberArtColor
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Slot value meaning "start from the beginning, ignore any auto-save". */
const val FreshStart = -99

/**
 * A full page for one game, in the style of a music player's now-playing screen: the art
 * blurred behind everything, colour pulled from it, a big cover, play/resume, save states,
 * emulator choice and details.
 */
@Composable
fun GameDetailScreen(
    game: Game,
    currentCore: CoreInfo,
    saveSlots: List<Pair<Int, Long>>,
    onPlay: (slot: Int?) -> Unit,
    onSelectCore: (CoreInfo) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val system = game.system ?: return
    val art = rememberArtColor(game)
    val tint by animateColorAsState(art, tween(600), label = "detailTint")
    val hasAuto = saveSlots.any { it.first == AppPaths.AUTO_SLOT }
    val time = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val coverWidth = (maxWidth * 0.62f).coerceAtMost(300.dp)

        // The art, blurred, hanging behind the top of the page; then the art colour washing to black.
        GameArt(
            game,
            Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.62f)
                .graphicsLayer { alpha = 0.55f }
                .blur(60.dp),
            showTag = false,
        )
        // Fade the backdrop's lower edge so it melts into the page instead of ending in a line.
        Box(
            Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.62f)
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black)),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to tint.copy(alpha = 0.55f),
                    0.35f to tint.copy(alpha = 0.35f),
                    0.65f to tint.copy(alpha = 0.1f),
                    1f to Color.Black,
                ),
            ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            Box(
                Modifier
                    .padding(start = 16.dp, top = 8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }

            GameArt(
                game,
                Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(coverWidth)
                    .aspectRatio(0.75f)
                    .press3D()
                    .shadow(28.dp, RoundedCornerShape(24.dp), ambientColor = tint, spotColor = tint)
                    .clip(RoundedCornerShape(24.dp)),
                showTitle = true,
            )

            Column(Modifier.padding(horizontal = 24.dp).padding(top = 28.dp)) {
                Text(game.title, style = MaterialTheme.typography.headlineMediumEmphasized, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(system.name, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))

                // Primary action: a light pill, like the hero's.
                Row(
                    Modifier
                        .padding(top = 22.dp)
                        .fillMaxWidth()
                        .height(54.dp)
                        .press3D(maxTilt = 3f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { onPlay(null) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF1A1A1A), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasAuto) "Resume" else "Play", style = MaterialTheme.typography.titleMediumEmphasized, color = Color(0xFF1A1A1A))
                }
                if (hasAuto) {
                    Text(
                        "Start from the beginning",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp)
                            .clip(CircleShape)
                            .clickable { onPlay(FreshStart) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            if (saveSlots.isNotEmpty()) {
                SectionLabel("Save states")
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(saveSlots, key = { it.first }) { (slot, modified) ->
                        Column(
                            Modifier
                                .width(150.dp)
                                .press3D()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onPlay(slot) }
                                .padding(14.dp),
                        ) {
                            Icon(if (slot == AppPaths.AUTO_SLOT) Icons.Rounded.History else Icons.Rounded.Save, null, tint = Color.White.copy(alpha = 0.8f))
                            Text(
                                if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot",
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                color = Color.White,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Text(time.format(Date(modified)), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                        }
                    }
                }
            }

            if (system.cores.size > 1) {
                SectionLabel("Emulator")
                var selected by remember { mutableIntStateOf(system.cores.indexOf(currentCore).coerceAtLeast(0)) }
                Row(
                    Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    system.cores.forEachIndexed { i, core ->
                        ToggleButton(
                            checked = i == selected,
                            onCheckedChange = { selected = i; onSelectCore(core) },
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                system.cores.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            if (i == selected) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(ToggleButtonDefaults.IconSize))
                                Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                            }
                            Text(core.displayName, maxLines = 1)
                        }
                    }
                }
                Text(
                    "${system.cores[selected].license} license · used for every ${system.shortName} game",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                )
            }

            SectionLabel("Details")
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
            ) {
                InfoRow("System", system.name)
                InfoRow("File", game.fileName)
                if (game.size > 0) InfoRow("Size", formatBytes(game.size))
                InfoRow("Source", if (game.isLocal) "Downloaded" else "Game folder")
            }

            if (onDelete != null) {
                var confirm by remember { mutableStateOf(false) }
                Text(
                    if (confirm) "Tap again to delete this download" else "Delete download",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 20.dp)
                        .clip(CircleShape)
                        .clickable { if (confirm) onDelete() else confirm = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = Color.White.copy(alpha = 0.69f),
        modifier = Modifier.padding(start = 24.dp, top = 30.dp, bottom = 12.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.width(72.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    else -> "${(bytes / 1024).coerceAtLeast(1)} KB"
}
