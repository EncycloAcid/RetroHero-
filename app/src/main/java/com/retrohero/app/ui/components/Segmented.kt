package com.retrohero.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.retrohero.app.ui.theme.Tone

/** Scope handed to a segmented group so each row gets the right first/middle/last shape. */
class SegmentScope(private val count: Int) {
    @Composable
    fun shapes(index: Int): ListItemShapes = ListItemDefaults.segmentedShapes(index, count)
}

/** An expressive grouped list: a header plus tightly spaced rows with rounded outer corners. */
@Composable
fun SegmentedGroup(
    title: String?,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable SegmentScope.() -> Unit,
) {
    Column(modifier) {
        if (title != null) {
            androidx.compose.material3.Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = Tone.Muted,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp),
            )
        }
        val depth = if (Tone.isDark) Modifier else Modifier.shadow(
            2.dp,
            Ascend.CardShape,
            ambientColor = Color.Black.copy(alpha = 0.05f),
            spotColor = Color.Black.copy(alpha = 0.08f),
        )
        // One surface card with crisp Instrument hairline border.
        Column(
            Modifier
                .then(depth)
                .clip(Ascend.CardShape)
                .background(GroupFill)
                .border(1.dp, Tone.Line, Ascend.CardShape)
                .padding(vertical = 6.dp),
        ) {
            SegmentScope(count).content()
        }
    }
}

/** Segmented rows sit on a raised container so the group reads clearly against the page. */
@Composable
fun segmentColors() = ListItemDefaults.segmentedColors(containerColor = Color.Transparent)

private val GroupFill get() = Tone.Surface
