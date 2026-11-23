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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Shared look for every tab, taken from the Shelf: same header, same chips, same glossy cards. */
object Page {
    /** Side padding for page content. */
    val Gutter = 20.dp
    val CardTop = Color(0xFF1E1E22)
    val CardBottom = Color(0xFF141417)
    /** Warm charcoal behind every page. */
    val Bg = Color(0xFF0E0E10)
    val Divider = Color.White.copy(alpha = 0.07f)
}

fun Modifier.emberBackground(): Modifier = background(Page.Bg)

@Composable
fun ArchiveLabel(text: String, modifier: Modifier = Modifier, color: Color = Ascend.accent) {
    Text(text.uppercase(), modifier, color = color,
        style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.1.em), maxLines = 1,
        overflow = TextOverflow.Ellipsis)
}

@Composable
fun PageHeader(title: String, subtitle: String?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Column(modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.retrohero.app.R.drawable.ic_logo_mark), null,
                Modifier.size(width = 18.dp, height = 13.dp).launchMarkTarget())
            ArchiveLabel("RetroHero  /  Archive", Modifier.padding(start = 7.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.displaySmallEmphasized, color = Color.White)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ascend.Muted)
            }
            trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Page.Divider))
    }
}

/** A rounded pill used for filters everywhere; tinted with [color] when selected. */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.White, leading: (@Composable () -> Unit)? = null) {
    val fill by animateColorAsState(
        if (!selected) Ascend.Surface else lerp(Ascend.Surface, if (color == Color.White) Ascend.accent else color, 0.20f),
        label = "pill",
    )
    Row(
        modifier.height(40.dp).clip(Ascend.CardShape).background(fill).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = if (selected) 1f else 0.78f), maxLines = 1)
    }
}

fun Modifier.glossCard(shape: Shape = Ascend.CardShape, tint: Color? = null): Modifier =
    clip(shape).background(tint?.let { lerp(Ascend.Surface, it, 0.08f) } ?: Ascend.Surface).border(1.dp, Page.Divider, shape)

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
