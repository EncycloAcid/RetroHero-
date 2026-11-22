package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Salmon

/** One tile in the grid menu. */
class MenuTile(val icon: ImageVector, val label: String, val tint: Color = Color.White, val onClick: () -> Unit)

/**
 * Long-press menu: the game at the top, then a grid of large action tiles (108 dp tall, like
 * the music player's), three to a row.
 */
@Composable
fun GameMenuSheet(
    game: Game,
    hasResume: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onFresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDetails: () -> Unit,
    onOpenSystem: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val tiles = buildList {
        if (hasResume) {
            add(MenuTile(Icons.Rounded.History, "Resume") { onResume() })
            add(MenuTile(Icons.Rounded.RestartAlt, "Start fresh") { onFresh() })
        } else {
            add(MenuTile(Icons.Rounded.PlayArrow, "Play") { onPlay() })
        }
        add(MenuTile(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isFavorite) "Unpin" else "Pin", Salmon) { onToggleFavorite() })
        add(MenuTile(Icons.Rounded.Info, "Details") { onDetails() })
        add(MenuTile(Icons.Rounded.ViewModule, game.system?.shortName ?: "System") { onOpenSystem() })
        if (onDelete != null) {
            add(MenuTile(Icons.Rounded.Delete, if (confirmDelete) "Tap to confirm" else "Delete", MaterialTheme.colorScheme.error) {
                if (confirmDelete) onDelete() else confirmDelete = true
            })
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF131313),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameArt(game, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), showTag = false)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(game.title, style = MaterialTheme.typography.titleMediumEmphasized, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(game.system?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(18.dp))
            tiles.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { tile ->
                        Column(
                            Modifier
                                .weight(1f)
                                .height(108.dp)
                                .press3D()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable(onClick = tile.onClick)
                                .padding(12.dp),
                        ) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Icon(tile.icon, null, tint = tile.tint, modifier = Modifier.size(28.dp))
                            }
                            Text(
                                tile.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
