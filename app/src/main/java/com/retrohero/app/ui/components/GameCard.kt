package com.retrohero.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.ios.Squircles

/** App Store–style tile: squircle artwork with a hairline edge, title and system below. */
@Composable
fun GameCard(game: Game, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 0.dp, pressedCorner = 0.dp, pressedScale = 0.94f)
    Column(
        modifier
            .graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            }
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick),
    ) {
        GameArt(
            game,
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .shadow(6.dp, Squircles.Medium, clip = false)
                .clip(Squircles.Medium)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), Squircles.Medium),
        )
        Text(
            game.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
        Text(
            game.system?.shortName.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
