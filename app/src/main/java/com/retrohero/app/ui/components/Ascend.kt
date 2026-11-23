package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon

/** Clean card surfaces: flat charcoal panels with a hairline border and medium rounding. */
object Ascend {
    val Surface = Color(0xFF17171A)
    val Border = Color.White.copy(alpha = 0.07f)
    val Muted = Color.White.copy(alpha = 0.55f)
    val CardShape = RoundedCornerShape(14.dp)
    /** The accent, lifted when it is too dark to read on charcoal. */
    val accent: Color get() = if (Salmon.luminance() < 0.25f) lerp(Salmon, Color.White, 0.5f) else Salmon
    /** Text/icon colour for content drawn on a filled accent. */
    val onAccent: Color get() = if (Salmon.luminance() > 0.4f) Color(0xFF1A0E08) else Color.White
}

fun Modifier.ascendCard(shape: androidx.compose.ui.graphics.Shape = Ascend.CardShape): Modifier =
    this.clip(shape).background(Ascend.Surface).border(1.dp, Ascend.Border, shape)

/** Section title: accent icon tile, bold title, a count badge, and an optional chevron. */
@Composable
fun SectionTitle(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Ascend.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Ascend.accent, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.02).em),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (count != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                "$count",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = Ascend.accent,
                modifier = Modifier.clip(CircleShape).background(Ascend.accent.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ascend.Muted)
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
) {
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .ascendCard()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.75f)) { GameArt(game, Modifier.fillMaxSize()) }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
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
        modifier.height(42.dp).clip(RoundedCornerShape(10.dp)).background(Salmon).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = Ascend.onAccent, modifier = Modifier.padding(end = 6.dp).size(18.dp))
        Text(text, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = Ascend.onAccent)
    }
}
