package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.retrohero.app.ui.theme.Salmon
import kotlinx.coroutines.launch

/**
 * A game row you can swipe: right to play it straight away, left to pin it to (or unpin it from)
 * Favourites. The row snaps back after the action; a tick of haptics marks the threshold.
 */
@Composable
fun SwipeRow(
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    /** Swiping left removes the game from a list instead of pinning it. */
    removeLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    // Buzz once when the swipe passes the point where letting go will act.
    LaunchedEffect(state.targetValue) {
        if (state.targetValue != SwipeToDismissBoxValue.Settled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    SwipeToDismissBox(
        state = state,
        // Screen readers get the swipe actions as plain actions.
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Play") { onPlay(); true },
                CustomAccessibilityAction(removeLabel ?: if (isFavorite) "Unpin" else "Pin") { onToggleFavorite(); true },
            )
        },
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onPlay()
                SwipeToDismissBoxValue.EndToStart -> onToggleFavorite()
                else -> Unit
            }
            scope.launch { state.reset() }
        },
        backgroundContent = {
            val dir = state.dismissDirection
            if (dir == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val armed = state.targetValue != SwipeToDismissBoxValue.Settled
            val pop by animateFloatAsState(if (armed) 1.15f else 0.85f, spring(dampingRatio = 0.45f), label = "swipeIcon")
            val (color, icon, label) = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(com.retrohero.app.ui.theme.Tone.Raised, Icons.Rounded.PlayArrow, "Play")
                else -> if (removeLabel != null) Triple(Color(0xFFE5484D), Icons.Rounded.RemoveCircle, removeLabel)
                else Triple(Salmon, if (isFavorite) Icons.Rounded.HeartBroken else Icons.Rounded.Favorite, if (isFavorite) "Unpin" else "Pin")
            }
            val onColor = when {
                dir == SwipeToDismissBoxValue.StartToEnd -> com.retrohero.app.ui.theme.Tone.Ink
                removeLabel != null -> Color(0xFF111111)
                else -> com.retrohero.app.ui.components.Ascend.onAccent
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = if (armed) 1f else 0.6f)),
            ) {
                Row(
                    Modifier
                        .align(if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = onColor, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = pop; scaleY = pop })
                    Text(label, style = MaterialTheme.typography.labelLargeEmphasized, color = onColor)
                }
            }
        },
    ) {
        // Opaque only while swiping, so the page's colour wash shows through at rest.
        val moving = state.dismissDirection != SwipeToDismissBoxValue.Settled
        Box(if (moving) Modifier.background(com.retrohero.app.ui.theme.Tone.Bg) else Modifier) { content() }
    }
}
