package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
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

/** Shared look for every tab, taken from the Shelf: same header, same chips, same glossy cards. */
object Page {
    /** Side padding for page content. */
    val Gutter = 22.dp
    val CardTop = Color(0xFF1E1E22)
    val CardBottom = Color(0xFF141417)
    /** Warm charcoal behind every page. */
    val Bg = Color(0xFF0E0E10)
    val Divider = Color.White.copy(alpha = 0.07f)
}

/** Warm charcoal with a soft glow of the accent colour rising from the lower left. */
fun Modifier.emberBackground(): Modifier = drawBehind {
    drawRect(Page.Bg)
    val accent = Salmon
    val w = size.width
    val h = size.height
    val r1 = w * 1.05f
    val c1 = Offset(-w * 0.2f, h * 0.62f)
    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.2f), Color.Transparent), center = c1, radius = r1), radius = r1, center = c1)
    val r2 = w * 0.8f
    val c2 = Offset(w * 1.1f, h * 0.02f)
    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.07f), Color.Transparent), center = c2, radius = r2), radius = r2, center = c2)
}

/** The page title block every tab uses: big title, a quiet subtitle, optional actions on the right. */
@Composable
fun PageHeader(title: String, subtitle: String?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em), color = Color.White)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** A rounded pill used for filters everywhere; tinted with [color] when selected. */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.White, leading: (@Composable () -> Unit)? = null) {
    val fill by animateColorAsState(
        if (!selected) Color.White.copy(alpha = 0.07f) else if (color == Color.White) lerp(Salmon, Color.Black, 0.25f) else lerp(color, Color.Black, 0.3f),
        label = "pill",
    )
    Row(
        modifier.height(40.dp).clip(CircleShape).background(fill).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = if (selected) 1f else 0.78f), maxLines = 1)
    }
}

/**
 * The glossy panel used for cards: a soft top-to-bottom gradient, a thin highlight along the top
 * edge like the lip of a plastic case, and a soft shadow.
 */
fun Modifier.glossCard(shape: Shape = RoundedCornerShape(16.dp), tint: Color? = null): Modifier = this
    .shadow(6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                tint?.let { lerp(Page.CardTop, it, 0.22f) } ?: Page.CardTop,
                tint?.let { lerp(Page.CardBottom, it, 0.1f) } ?: Page.CardBottom,
            ),
        ),
    )
    .drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.09f), 0.03f to Color.Transparent))
    }
    .border(1.dp, Color.White.copy(alpha = 0.06f), shape)

/** Video-store yellow, for the little Play buttons. */
val StoreYellow = Color(0xFFF2C94C)

/**
 * A game as it would sit in a video store: its (slightly worn) box, the title in bold capitals,
 * a small info line, and a yellow Play button that starts it straight away.
 */
@Composable
fun BoxTile(
    game: Game,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    meta: String? = game.system?.name,
    onPlay: (() -> Unit)? = null,
) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick), horizontalAlignment = Alignment.CenterHorizontally) {
        // Boxes differ in shape; give them all the same footprint so rows line up.
        Box(Modifier.fillMaxWidth().height(width * 1.42f), contentAlignment = Alignment.BottomCenter) {
            val spec = boxSpec(game.systemId)
            val w = if (spec.aspect > 1.1f) width else width * 0.94f
            GameBox3D(game, w, rotY = 0f, rotX = 0f, reflection = false)
        }
        Text(
            game.title.uppercase(),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.02.em),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (meta != null) {
            Text(meta, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onPlay != null) {
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StoreYellow)
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.PlayArrow, null, tint = Color(0xFF1A1A1A), modifier = Modifier.padding(end = 4.dp).height(16.dp))
                Text("PLAY", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.08.em), color = Color(0xFF1A1A1A))
            }
        }
    }
}
