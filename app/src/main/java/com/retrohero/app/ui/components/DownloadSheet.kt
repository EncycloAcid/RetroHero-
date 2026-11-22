package com.retrohero.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.retrohero.app.ui.ios.CapsuleButton
import com.retrohero.app.ui.ios.CapsuleStyle
import com.retrohero.app.ui.ios.GroupedSection
import com.retrohero.app.ui.ios.IconTile
import com.retrohero.app.ui.theme.IosColors
import java.util.Locale

/** iOS-style sheet that walks through downloading a game or BIOS from a link. */
@Composable
fun DownloadSheet(
    state: DownloadState,
    onStart: (DownloadKind, String) -> Unit,
    onRetry: (DownloadKind) -> Unit,
    onPlay: (Game) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { Grabber() },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(bottom = 20.dp)) {
            when (state) {
                is DownloadState.Input -> InputPage(state, onStart, onDismiss)
                is DownloadState.Running -> RunningPage(state, onDismiss)
                is DownloadState.Done -> ResultPage(
                    icon = Icons.Rounded.Done,
                    polygon = MaterialShapes.Cookie9Sided,
                    tint = IosColors.Green,
                    title = state.title,
                    detail = state.detail,
                ) {
                    val game = state.game
                    if (game != null) {
                        CapsuleButton("Play Now", { onPlay(game) }, icon = Icons.Rounded.PlayArrow, large = true, modifier = Modifier.fillMaxWidth())
                        CapsuleButton("Done", onDismiss, style = CapsuleStyle.Gray, large = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        CapsuleButton("Done", onDismiss, large = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                is DownloadState.Failed -> ResultPage(
                    icon = Icons.Rounded.ErrorOutline,
                    polygon = MaterialShapes.SoftBurst,
                    tint = IosColors.Red,
                    title = "Download Failed",
                    detail = state.message,
                ) {
                    CapsuleButton("Try Again", { onRetry(state.kind) }, large = true, modifier = Modifier.fillMaxWidth())
                    CapsuleButton("Close", onDismiss, style = CapsuleStyle.Gray, large = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Title(kind: DownloadKind) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconTile(if (kind == DownloadKind.Game) Icons.Rounded.SportsEsports else Icons.Rounded.Memory, if (kind == DownloadKind.Game) IosColors.Pink else IosColors.Indigo, 56.dp)
        Text(
            if (kind == DownloadKind.Game) "Download a Game" else "Download BIOS",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun InputPage(state: DownloadState.Input, onStart: (DownloadKind, String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var url by rememberSaveable { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    Title(state.kind)
    GroupedSection(
        header = "Link",
        footer = state.error ?: if (state.kind == DownloadKind.Game) {
            "Paste a direct link, or a Google Drive or Dropbox share link, to a game file. It's saved in RetroHero's " +
                "Games folder and added to your library. Only download games you own or that are free to share, such as homebrew."
        } else {
            "Paste a link to a BIOS file, or a .zip of them, dumped from your own console. Each file is checked " +
                "against known checksums and saved under the name the emulator expects."
        },
    ) {
        row {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (url.isEmpty()) Text("https://…", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                    BasicTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onStart(state.kind, url) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    "Paste",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.primary,
                    modifier = Modifier.clip(CircleShape).clickable { pasteInto(context)?.let { url = it } }.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CapsuleButton("Download", { onStart(state.kind, url) }, large = true, modifier = Modifier.fillMaxWidth())
        CapsuleButton("Cancel", onDismiss, style = CapsuleStyle.Gray, large = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RunningPage(state: DownloadState.Running, onCancel: () -> Unit) {
    Title(state.kind)
    val total = state.total
    Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (total != null) {
            LinearWavyProgressIndicator(progress = { (state.read.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearWavyProgressIndicator(Modifier.fillMaxWidth())
        }
        Text(
            if (total != null) "${size(state.read)} of ${size(total)}" else size(state.read),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    CapsuleButton("Cancel", onCancel, style = CapsuleStyle.Gray, large = true, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth())
}

@Composable
private fun ResultPage(
    icon: ImageVector,
    polygon: RoundedPolygon,
    tint: Color,
    title: String,
    detail: String,
    buttons: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).background(tint.copy(alpha = 0.16f), polygon.toShape()), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(40.dp), tint = tint)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { buttons() }
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
