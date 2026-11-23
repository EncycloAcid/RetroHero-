package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon

/** Clean card surfaces: flat charcoal panels with a hairline border and medium rounding. */
object Ascend {
    val Surface = Color(0xFF19191C)
    val Border = Color.White.copy(alpha = 0.07f)
    val Muted = Color.White.copy(alpha = 0.65f)
    val CardShape = RoundedCornerShape(8.dp)
    /** The accent, lifted when it is too dark to read on charcoal. */
    val accent: Color get() = if (Salmon.luminance() < 0.25f) lerp(Salmon, Color.White, 0.5f) else Salmon
    /** Text/icon colour for content drawn on a filled accent. */
    val onAccent: Color get() = if (Salmon.luminance() > 0.18f) Color(0xFF1A0E08) else Color.White
}

fun Modifier.ascendCard(shape: androidx.compose.ui.graphics.Shape = Ascend.CardShape): Modifier =
    this.clip(shape).background(Ascend.Surface).border(1.dp, Ascend.Border, shape)

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, count: Int? = null, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
        .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.White)
        if (count != null) ArchiveLabel(count.toString().padStart(2, '0'), Modifier.padding(start = 8.dp), color = Ascend.Muted)
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ascend.Muted, modifier = Modifier.size(18.dp))
    }
}

/** Library card: cover on top, title and a quiet info line on a flat panel underneath. */
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    subtitle: String? = game.system?.name,
    onLongClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
) {
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.73f).clip(RoundedCornerShape(6.dp))) {
            GameArt(game, Modifier.fillMaxSize())
            if (onPlay != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.78f))
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = Ascend.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Solid accent call-to-action, e.g. "Play" or "View Game". */
@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.height(48.dp).clip(Ascend.CardShape).background(Salmon).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        if (icon != null) Icon(icon, null, tint = Ascend.onAccent, modifier = Modifier.padding(end = 6.dp).size(18.dp))
        Text(text, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Ascend.onAccent)
    }
}
