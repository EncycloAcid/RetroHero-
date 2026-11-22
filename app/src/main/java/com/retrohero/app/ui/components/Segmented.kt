package com.retrohero.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            SegmentScope(count).content()
        }
    }
}

/** Segmented rows sit on a raised container so the group reads clearly against the page. */
@Composable
fun segmentColors() = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
