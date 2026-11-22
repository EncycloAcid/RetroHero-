package com.retrohero.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.retrohero.app.data.Downloader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.retrohero.app.data.Game
import com.retrohero.app.ui.AppViewModel.DownloadKind
import com.retrohero.app.ui.AppViewModel.DownloadState
import java.util.Locale

/** Bottom sheet that walks through downloading a game or BIOS from a link. */
@Composable
fun DownloadSheet(
    state: DownloadState,
    onStart: (DownloadKind, String) -> Unit,
    onDownloadEntries: (List<Downloader.FolderEntry>) -> Unit,
    onRetry: (DownloadKind) -> Unit,
    onPlay: (Game) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp),
        ) {
            when (state) {
                is DownloadState.Input -> InputPage(state, onStart, onDismiss)
                is DownloadState.Running -> RunningPage(state, onDismiss)
                is DownloadState.Folder -> FolderPage(state, onDownloadEntries, onDismiss)
                is DownloadState.Done -> ResultPage(
                    icon = Icons.Outlined.Done,
                    polygon = MaterialShapes.Cookie9Sided,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = state.title,
                    detail = state.detail,
                ) {
                    val game = state.game
                    if (game != null) {
                        OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Done") }
                        Button(onClick = { onPlay(game) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Play now")
                        }
                    } else {
                        Button(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Done") }
                    }
                }
                is DownloadState.Failed -> ResultPage(
                    icon = Icons.Outlined.ErrorOutline,
                    polygon = MaterialShapes.SoftBurst,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    title = "Download failed",
                    detail = state.message,
                ) {
                    OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Close") }
                    Button(onClick = { onRetry(state.kind) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Try again") }
                }
            }
        }
    }
}

@Composable
private fun Header(kind: DownloadKind, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie6Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (kind == DownloadKind.Game) Icons.Outlined.SportsEsports else Icons.Outlined.Memory,
                null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.size(16.dp))
        Column {
            Text(if (kind == DownloadKind.Game) "Download a game" else "Download BIOS", style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InputPage(state: DownloadState.Input, onStart: (DownloadKind, String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var url by rememberSaveable { mutableStateOf("") }
    Header(state.kind, "From a link to your own file")
    OutlinedTextField(
        value = url,
        onValueChange = { url = it },
        label = { Text("Link") },
        placeholder = { Text("https://…") },
        leadingIcon = { Icon(Icons.Outlined.Link, null) },
        trailingIcon = {
            IconButton(onClick = { pasteInto(context)?.let { url = it } }, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.ContentPaste, "Paste")
            }
        },
        isError = state.error != null,
        supportingText = state.error?.let { { Text(it) } },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onStart(state.kind, url) }),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    )
    Text(
        if (state.kind == DownloadKind.Game) {
            "Paste a direct link, or a Google Drive or Dropbox share link, to a game file (.zip, .gba, .chd…). " +
                "A link to a folder on your own computer or NAS (http:// works on your home network) lets you pick several games. " +
                "It's saved in RetroHero's Games folder and added to your library. " +
                "Only download games you own or that are free to share, such as homebrew."
        } else {
            "Paste a link to a BIOS file or a .zip of them, dumped from your own console. RetroHero checks each file's " +
                "checksum and saves it under the name the emulator expects."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(onClick = { onStart(state.kind, url) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Download") }
    }
}

@Composable
private fun RunningPage(state: DownloadState.Running, onCancel: () -> Unit) {
    Header(state.kind, state.label ?: "Downloading…")
    val total = state.total
    if (total != null) {
        LinearWavyProgressIndicator(
            progress = { (state.read.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        )
    } else {
        LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(top = 32.dp))
    }
    Text(
        if (total != null) "${size(state.read)} of ${size(total)}" else size(state.read),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    OutlinedButton(onClick = onCancel, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text("Cancel")
    }
}

@Composable
private fun ResultPage(
    icon: ImageVector,
    polygon: RoundedPolygon,
    container: Color,
    content: Color,
    title: String,
    detail: String,
    buttons: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).background(container, polygon.toShape()), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(40.dp), tint = content)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = buttons)
    }
}

private fun pasteInto(context: Context): String? {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
    return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
}

private fun size(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    else -> String.format(Locale.US, "%d KB", bytes / 1024)
}

@Composable
private fun FolderPage(state: DownloadState.Folder, onDownload: (List<Downloader.FolderEntry>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember(state) { mutableStateOf(state.entries.map { it.url }.toSet()) }
    Text("Pick games", style = MaterialTheme.typography.headlineSmall)
    Text(
        "${state.entries.size} game files in this folder",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        val all = selected.size == state.entries.size
        Text(
            if (all) "Select none" else "Select all",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { selected = if (all) emptySet() else state.entries.map { it.url }.toSet() }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        state.entries.forEach { entry ->
            val checked = entry.url in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { selected = if (checked) selected - entry.url else selected + entry.url }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(entry.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(
            onClick = { onDownload(state.entries.filter { it.url in selected }) },
            enabled = selected.isNotEmpty(),
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f),
        ) { Text(if (selected.size == 1) "Download 1 game" else "Download ${selected.size} games") }
    }
}
