package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Homebrew
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.tag

/** One-tap downloads of free, openly licensed homebrew games. */
@Composable
fun HomebrewSheet(installed: Set<String>, onGet: (HomebrewGame) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Free homebrew", style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(
                "New games made for classic consoles by independent developers, free to download and share.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Homebrew.catalog.forEach { game ->
                    val system = game.system ?: return@forEach
                    val have = game.fileName in installed
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control))
                                .background(com.retrohero.app.ui.theme.Tone.Raised)
                                .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(system.tag, style = TextStyle(fontFamily = PixelFont, fontSize = 10.sp), color = com.retrohero.app.ui.theme.Tone.Ink)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleMediumEmphasized)
                            Text(
                                game.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                            Text(
                                "${game.author} · ${game.license}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        if (have) {
                            Icon(Icons.Rounded.Check, "In your library", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(com.retrohero.app.ui.components.Ascend.accent)
                                    .clickableNoRipple { onGet(game) }
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                            ) { Text("Get", style = MaterialTheme.typography.labelLargeEmphasized, color = com.retrohero.app.ui.components.Ascend.onAccent) }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
