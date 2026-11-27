package com.retrohero.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/** Guides writing a game to an NFC sticker or card: hold the tag, then done or try again. */
@Composable
internal fun NfcWriteDialog(state: AppViewModel.NfcWrite, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val title = state.game.title
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                when (state) {
                    is AppViewModel.NfcWrite.Waiting -> Icons.Rounded.Nfc
                    is AppViewModel.NfcWrite.Done -> Icons.Rounded.CheckCircle
                    is AppViewModel.NfcWrite.Failed -> Icons.Rounded.ErrorOutline
                },
                contentDescription = null,
            )
        },
        title = {
            Text(
                when (state) {
                    is AppViewModel.NfcWrite.Waiting -> "Hold a tag to your phone"
                    is AppViewModel.NfcWrite.Done -> "Cartridge ready"
                    is AppViewModel.NfcWrite.Failed -> "Couldn't write the tag"
                },
            )
        },
        text = {
            Text(
                when (state) {
                    is AppViewModel.NfcWrite.Waiting ->
                        "Hold an NFC sticker or card flat against the back of your phone to turn it into a cartridge for $title. " +
                            "NTAG213, 215 and 216 stickers work, and anything already on the tag is replaced."
                    is AppViewModel.NfcWrite.Done ->
                        "Tap this tag to your phone any time to start $title. Stick it on a case or card for your shelf."
                    is AppViewModel.NfcWrite.Failed -> state.message
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
        confirmButton = {
            when (state) {
                is AppViewModel.NfcWrite.Waiting -> TextButton(onClick = onDismiss) { Text("Cancel") }
                is AppViewModel.NfcWrite.Done -> TextButton(onClick = onDismiss) { Text("Done") }
                is AppViewModel.NfcWrite.Failed -> TextButton(onClick = onRetry) { Text("Try again") }
            }
        },
        dismissButton = {
            if (state is AppViewModel.NfcWrite.Failed) TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
