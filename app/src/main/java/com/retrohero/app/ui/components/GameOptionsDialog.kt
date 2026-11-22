package com.retrohero.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game

/** Long-press dialog: game details and the emulator core used for its system. */
@Composable
fun GameOptionsDialog(
    game: Game,
    currentCore: CoreInfo,
    onSelectCore: (CoreInfo) -> Unit,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val system = game.system ?: return
    var selected by remember { mutableStateOf(currentCore) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(game.title) },
        text = {
            Column {
                Text(game.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(system.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                Text("Emulator core for ${system.shortName}", style = MaterialTheme.typography.titleSmall)
                system.cores.forEach { core ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = core == selected) {
                            selected = core
                            onSelectCore(core)
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = core == selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(core.displayName)
                            Text(core.license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onPlay) { Text("Play") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
