package com.retrohero.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** Long-press sheet: cover, details, emulator core choice and a big Play button. */
@Composable
fun GameSheet(
    game: Game,
    currentCore: CoreInfo,
    onSelectCore: (CoreInfo) -> Unit,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val system = game.system ?: return
    var selected by remember { mutableStateOf(currentCore) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameArt(game, Modifier.width(96.dp).height(128.dp).clip(MaterialTheme.shapes.large))
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    SystemBadge(system, 40.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(game.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(system.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                game.fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text("Emulator core", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                val cores = system.cores
                cores.forEachIndexed { i, core ->
                    ToggleButton(
                        checked = core == selected,
                        onCheckedChange = {
                            selected = core
                            onSelectCore(core)
                        },
                        shapes = when {
                            cores.size == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                            i == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            i == cores.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        if (core == selected) {
                            Icon(Icons.Default.Check, null, Modifier.size(ToggleButtonDefaults.IconSize))
                            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                        }
                        Text(core.displayName, maxLines = 1)
                    }
                }
            }
            Text(
                "License: ${selected.license}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Button(
                onClick = onPlay,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
                Text("Play", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
    }
}
