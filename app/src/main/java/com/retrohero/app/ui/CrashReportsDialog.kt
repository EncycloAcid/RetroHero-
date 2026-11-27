package com.retrohero.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import com.retrohero.app.data.CrashReporter

/**
 * Shown after a crash (afterCrash = true) or from Settings. Lets the player send the local
 * reports through any share target (email, Discord, Drive…) or delete them.
 */
@Composable
internal fun CrashReportsDialog(
    count: Int,
    afterCrash: Boolean,
    onShare: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
        title = { Text(if (afterCrash) "RetroHero closed unexpectedly" else "Crash reports") },
        text = {
            Text(
                when {
                    count == 0 -> "No crash reports. If RetroHero or a game ever closes unexpectedly, a report will show up here."
                    afterCrash -> "A crash report was saved on this device. Sharing it helps get the problem fixed. " +
                        "It has the app version, device model, Android version, the game and core that were running, and the error. " +
                        "Nothing is sent unless you share it."
                    else -> "$count report${if (count == 1) "" else "s"} saved on this device. " +
                        "They include the app version, device model, Android version, the game and core that were running, and the error."
                },
            )
        },
        confirmButton = {
            if (count > 0) TextButton(onClick = onShare) { Text("Share") } else TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = {
            if (count > 0) {
                if (afterCrash) TextButton(onClick = onDismiss) { Text("Not now") }
                else TextButton(onClick = onClear) { Text("Delete all") }
            }
        },
    )
}

internal fun shareCrashReports(context: Context) {
    val file = CrashReporter.bundleForSharing(context)
    if (file == null) {
        Toast.makeText(context, "No crash reports to share", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "RetroHero crash report")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share crash report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
