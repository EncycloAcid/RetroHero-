package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

/** Tick the collections [game] should be in, or start a new one. */
@Composable
fun CollectionPickerSheet(
    game: Game,
    collections: List<GameCollection>,
    onToggle: (GameCollection, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var naming by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tone.Surface,
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("Add to collection", style = MaterialTheme.typography.titleLargeEmphasized, color = Tone.Ink)
            Text(game.title, style = MaterialTheme.typography.bodyMedium, color = Tone.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(14.dp))
            collections.forEach { c ->
                val inIt = game.id in c.gameIds
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(Ascend.Radius.Card))
                        .background(Tone.Raised)
                        .border(1.dp, Tone.Line, RoundedCornerShape(Ascend.Radius.Card))
                        .clickable { onToggle(c, !inIt) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium, color = Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (c.gameIds.size == 1) "1 game" else "${c.gameIds.size} games", style = MaterialTheme.typography.bodySmall, color = Tone.Muted)
                    }
                    Icon(
                        if (inIt) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        if (inIt) "In ${c.name}" else "Not in ${c.name}",
                        tint = if (inIt) Salmon else Tone.Faint,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Ascend.Radius.Card))
                    .clickable { naming = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(Ascend.Radius.Control)).background(Tone.Raised), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, tint = Tone.Ink)
                }
                Spacer(Modifier.width(12.dp))
                Text("New collection", style = MaterialTheme.typography.titleMedium, color = Tone.Ink)
            }
        }
    }
    if (naming) {
        NameDialog(
            title = "New collection",
            initial = "",
            confirm = "Create",
            onDismiss = { naming = false },
        ) { onCreate(it); naming = false }
    }
}

/** A one-field dialog for naming (or renaming) something. */
@Composable
fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
