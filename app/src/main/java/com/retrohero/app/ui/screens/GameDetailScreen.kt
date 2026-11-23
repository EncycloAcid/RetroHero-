package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.AccentButton
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.emberBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import android.text.format.DateUtils
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.press3D
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
    stat: PlayStat = PlayStat.None,
    screenshots: List<File> = emptyList(),
    hasCustomCover: Boolean = false,
    onUseAsCover: (File) -> Unit = {},
    onClearCover: () -> Unit = {},
    onShareScreenshot: (File) -> Unit = {},
    pageIndex: Int = 4,
    onDeleteScreenshot: (File) -> Unit = {},
) {
    val system = game.system ?: return
    val hasAuto = saveSlots.any { it.first == AppPaths.AUTO_SLOT }
    val time = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()) }

    BoxWithConstraints(Modifier.fillMaxSize().emberBackground()) {
        val coverWidth = (maxWidth * 0.35f).coerceAtMost(190.dp)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = com.retrohero.app.ui.theme.Tone.Ink) }
                ArchiveLabel("${pageIndex.toString().padStart(2, '0')} — Game record")
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = Page.Gutter, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
                GameArt(game, Modifier.width(coverWidth).aspectRatio(0.73f).clip(Ascend.CardShape), showTag = false)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    ArchiveLabel(system.shortName)
                    Text(game.title, style = MaterialTheme.typography.displaySmallEmphasized, color = com.retrohero.app.ui.theme.Tone.Ink,
                        modifier = Modifier.padding(top = 6.dp))
                    Text(system.name, style = MaterialTheme.typography.bodySmall, color = Ascend.Muted)
                    if (stat.sessions > 0 || stat.seconds > 0) Text("${PlayTime.format(stat.seconds)} played · ${stat.sessions} sessions",
                        style = MaterialTheme.typography.bodySmall, color = Ascend.Muted, modifier = Modifier.padding(top = 10.dp))
                    if (stat.lastPlayed > 0) Text("Last played " + DateUtils.getRelativeTimeSpanString(stat.lastPlayed,
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().lowercase(Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall, color = Ascend.Muted)
                }
            }
            Column(Modifier.padding(horizontal = Page.Gutter, vertical = 12.dp)) {
                AccentButton(if (hasAuto) "Resume game" else "Play game", { onPlay(null) }, Modifier.fillMaxWidth(), Icons.Rounded.PlayArrow)
                if (hasAuto) {
                    Text(
                        "Start from the beginning",
                        style = MaterialTheme.typography.labelLarge,
                        color = com.retrohero.app.ui.theme.Tone.Muted,
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
                SectionLabel("Save states", 1)
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(saveSlots, key = { it.first }) { (slot, modified) ->
                        Column(
                            Modifier
                                .width(150.dp)
                                .press3D()
                                .clip(Ascend.CardShape)
                                .background(com.retrohero.app.ui.theme.Tone.Surface)
                                .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, Ascend.CardShape)
                                .clickable { onPlay(slot) }
                                .padding(14.dp),
                        ) {
                            Icon(if (slot == AppPaths.AUTO_SLOT) Icons.Rounded.History else Icons.Rounded.Save, null, tint = com.retrohero.app.ui.theme.Tone.Muted)
                            Text(
                                if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot",
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                color = com.retrohero.app.ui.theme.Tone.Ink,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Text(time.format(Date(modified)), style = MaterialTheme.typography.bodySmall, color = com.retrohero.app.ui.theme.Tone.Muted, maxLines = 1)
                        }
                    }
                }
            }

            if (screenshots.isNotEmpty()) {
                var viewing by remember { mutableStateOf<File?>(null) }
                SectionLabel("Screenshots", 2)
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(screenshots, key = { it.path }) { shot ->
                        AsyncImage(
                            model = shot,
                            contentDescription = "Screenshot",
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier
                                .height(110.dp)
                                .aspectRatio(4f / 3f)
                                .press3D()
                                .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Cover))
                                .background(com.retrohero.app.ui.theme.Tone.Surface)
                                .clickable { viewing = shot },
                        )
                    }
                }
                if (hasCustomCover) {
                    Text(
                        "Go back to the original cover",
                        style = MaterialTheme.typography.labelLarge,
                        color = com.retrohero.app.ui.theme.Tone.Muted,
                        modifier = Modifier
                            .padding(start = 14.dp, top = 6.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearCover)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                viewing?.let { shot ->
                    ScreenshotDialog(
                        shot,
                        onDismiss = { viewing = null },
                        onUseAsCover = { onUseAsCover(shot); viewing = null },
                        onShare = { onShareScreenshot(shot) },
                        onDelete = { onDeleteScreenshot(shot); viewing = null },
                    )
                }
            }

            if (system.cores.size > 1) {
                SectionLabel("Emulator", 3)
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
                    color = com.retrohero.app.ui.theme.Tone.Muted,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                )
            }

            SectionLabel("Details", 4)
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(Ascend.CardShape)
                    .background(com.retrohero.app.ui.theme.Tone.Surface)
                    .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, Ascend.CardShape),
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
private fun ScreenshotDialog(shot: File, onDismiss: () -> Unit, onUseAsCover: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
                .background(Color(0xFF161618))
                .padding(16.dp),
        ) {
            AsyncImage(
                model = shot,
                contentDescription = "Screenshot",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Cover)),
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShotAction(Icons.Rounded.Image, "Use as cover", Modifier.weight(1f), onUseAsCover)
                ShotAction(Icons.Rounded.Share, "Share", Modifier.weight(1f), onShare)
                ShotAction(Icons.Rounded.Delete, "Delete", Modifier.weight(1f), onDelete)
            }
        }
    }
}

@Composable
private fun ShotAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String, index: Int) {
    com.retrohero.app.ui.components.SectionTitle(
        text,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 12.dp),
        index = index,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = com.retrohero.app.ui.theme.Tone.Muted, modifier = Modifier.width(72.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    else -> "${(bytes / 1024).coerceAtLeast(1)} KB"
}
