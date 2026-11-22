package com.retrohero.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.ui.ios.CapsuleButton
import com.retrohero.app.ui.ios.CapsuleStyle
import com.retrohero.app.ui.ios.GroupedSection
import com.retrohero.app.ui.ios.IosRow
import com.retrohero.app.ui.ios.SegmentedControl
import com.retrohero.app.ui.ios.Squircles
import com.retrohero.app.ui.ios.TrailingValue
import java.util.Locale

/** Long-press sheet laid out like an App Store product page. */
@Composable
fun GameSheet(
    game: Game,
    currentCore: CoreInfo,
    onSelectCore: (CoreInfo) -> Unit,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val system = game.system ?: return
    var selected by remember { mutableIntStateOf(system.cores.indexOf(currentCore).coerceAtLeast(0)) }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { Grabber() },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 20.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
                GameArt(
                    game,
                    Modifier
                        .size(width = 104.dp, height = 138.dp)
                        .clip(Squircles.Medium)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, Squircles.Medium),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(game.title, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(system.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    CapsuleButton("Play", onPlay, icon = Icons.Rounded.PlayArrow)
                }
            }

            if (system.cores.size > 1) {
                GroupedSection(
                    header = "Emulator",
                    footer = "${system.cores[selected].license} license. Applies to every ${system.shortName} game.",
                ) {
                    row {
                        SegmentedControl(
                            options = system.cores.map { it.displayName },
                            selected = selected,
                            onSelect = {
                                selected = it
                                onSelectCore(system.cores[it])
                            },
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }

            GroupedSection(header = "Information", dividerInset = 16.dp) {
                row { IosRow("System") { TrailingValue(system.shortName) } }
                row { IosRow("File", subtitle = game.fileName) }
                if (game.size > 0) row { IosRow("Size") { TrailingValue(formatSize(game.size)) } }
                row { IosRow("Source") { TrailingValue(if (game.isLocal) "Downloaded" else "Game folder") } }
            }

            if (onDelete != null) {
                GroupedSection(footer = if (confirmDelete) "Tap again to permanently delete this file." else null) {
                    row {
                        IosRow(
                            if (confirmDelete) "Confirm Delete" else "Delete Download",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            CapsuleButton(
                "Done",
                onDismiss,
                style = CapsuleStyle.Gray,
                large = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    else -> "${(bytes / 1024).coerceAtLeast(1)} KB"
}
