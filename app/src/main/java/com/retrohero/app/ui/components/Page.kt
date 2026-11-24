package com.retrohero.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone

/** Shared look for every tab, matching the Game Record Bento & Telemetry aesthetic. */
object Page {
    /** Side padding for page content. */
    val Gutter = 20.dp
    val CardTop: Color get() = Tone.Surface
    val CardBottom: Color get() = Tone.Bg
    val Bg: Color get() = Tone.Bg
    val Divider: Color get() = Tone.Line
}

fun Modifier.emberBackground(): Modifier = background(Tone.Bg)

@Composable
fun ArchiveLabel(text: String, modifier: Modifier = Modifier, color: Color = Tone.Muted) {
    Text(
        text.uppercase(),
        modifier,
        color = color,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.12.em,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    index: Int = 1,
    trailing: @Composable () -> Unit = {},
) {
    Column(modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(top = 10.dp)) {
        // Top telemetry breadcrumb bar (matches Game Record header)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Tone.Surface)
                        .border(1.dp, Tone.Line, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        androidx.compose.ui.res.painterResource(com.retrohero.app.R.drawable.ic_logo_mark),
                        null,
                        Modifier.size(14.dp).launchMarkTarget(),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Tone.Ink),
                    )
                }
                ArchiveLabel("${index.toString().padStart(2, '0')} — ${title.uppercase()} RECORD")
            }
            if (subtitle != null) {
                Box(
                    Modifier
                        .background(Tone.Surface, RoundedCornerShape(7.dp))
                        .border(1.dp, Tone.Line, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    ArchiveLabel(subtitle, color = Tone.Ink)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.03).em,
                ),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Page.Divider))
    }
}

/** One cell in a 4-column Game Record Bento Telemetry Strip. */
data class BentoMetric(
    val value: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * 4-Cell Bento Telemetry Strip (exact style from GameDetailScreen):
 * Bold value + icon in top cell, horizontal hairline divider, and recessed monospace uppercase footer label.
 */
@Composable
fun BentoTelemetryStrip(
    metrics: List<BentoMetric>,
    modifier: Modifier = Modifier,
) {
    if (metrics.isEmpty()) return
    val shape = RoundedCornerShape(Ascend.Radius.Card)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            metrics.forEachIndexed { idx, m ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            m.value,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = (-0.02).em,
                            ),
                            color = Tone.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(m.icon, null, tint = Tone.Muted, modifier = Modifier.size(13.dp))
                    }
                    HorizontalDivider(color = Page.Divider)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Tone.Bg.copy(alpha = 0.42f))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            m.label.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 7.5.sp,
                                letterSpacing = 0.06.em,
                            ),
                            color = Tone.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (idx < metrics.lastIndex) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Page.Divider))
                }
            }
        }
    }
}

/**
 * Boxed Action Button (exact style from GameDetailScreen):
 * Label on the left, framed 32.dp rounded-square icon box on the right edge.
 */
@Composable
fun BoxedButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(11.dp)
    val bg = if (primary) Ascend.accent else Tone.Surface
    val fg = if (primary) Ascend.onAccent else Tone.Ink
    val borderCol = if (primary) Color.Transparent else Tone.Line
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .height(44.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderCol, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 13.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (primary) Color.Black.copy(alpha = 0.18f) else Tone.Raised)
                .border(1.dp, if (primary) Color.White.copy(alpha = 0.22f) else Tone.Line, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Game Record Bento Panel Card (matches "YOUR ACHIEVEMENTS" panel from GameDetailScreen):
 * Top monospace header bar with right-side count + info icon, 1.dp hairline divider, and 10.dp padded interior.
 */
@Composable
fun BentoPanel(
    header: String,
    badge: String,
    modifier: Modifier = Modifier,
    onHeaderClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Ascend.Radius.Card)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onHeaderClick != null) Modifier.clickable(onClick = onHeaderClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchiveLabel(header, color = Tone.Muted)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    badge,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = Tone.Ink,
                )
                Icon(
                    if (onHeaderClick != null) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.Info,
                    null,
                    tint = Tone.Muted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        HorizontalDivider(color = Page.Divider)
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Recessed sub-row inside a BentoPanel (exact style of achievement rows in GameDetailScreen):
 * 40.dp rounded-square badge on left, title & subtitle in center, monospace tag/percentage on right.
 */
@Composable
fun BentoItemRow(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    rightText: String? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val rowShape = RoundedCornerShape(11.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(Tone.Bg.copy(alpha = 0.55f))
            .border(1.dp, if (highlighted) Ascend.accent.copy(alpha = 0.55f) else Tone.Line, rowShape)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
                } else Modifier,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (highlighted) Ascend.accent else Tone.Raised)
                .border(1.dp, if (highlighted) Color.Transparent else Tone.Line, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            leading()
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                color = Tone.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (rightText != null) {
            Text(
                rightText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = if (highlighted) Ascend.accent else Tone.Muted,
            )
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted, modifier = Modifier.size(16.dp))
        }
    }
}

/** A rounded pill used for filters everywhere; tinted with [color] when selected. */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    leading: (@Composable () -> Unit)? = null,
    selectedStyle: Boolean = false,
) {
    val fill by animateColorAsState(
        if (!selected) Tone.Raised else if (selectedStyle) Tone.Ink else lerp(Tone.Raised, if (color == Color.White) Ascend.accent else color, 0.20f),
        label = "pill",
    )
    val shape = RoundedCornerShape(Ascend.Radius.Control)
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (selected && selectedStyle) Tone.Ink else Tone.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected && selectedStyle) Tone.Bg else if (selected) Tone.Ink else Tone.Muted,
            maxLines = 1,
        )
    }
}

fun Modifier.glossCard(shape: Shape = Ascend.CardShape): Modifier =
    clip(shape).background(Tone.Surface).border(1.dp, Page.Divider, shape)

/** Video-store yellow, for the little Play buttons. */
val StoreYellow = Color(0xFFF2C94C)

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
        Box(Modifier.fillMaxWidth().height(width * 1.42f), contentAlignment = Alignment.BottomCenter) {
            val spec = boxSpec(game.systemId)
            val w = if (spec.aspect > 1.1f) width else width * 0.94f
            GameBox3D(game, w, rotY = 0f, rotX = 0f, reflection = false)
        }
        Text(
            game.title.uppercase(),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.02.em),
            color = Tone.Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (meta != null) {
            Text(meta, style = MaterialTheme.typography.labelSmall, color = Tone.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onPlay != null) {
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(Ascend.Radius.Control))
                    .background(StoreYellow)
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF1A1A1A), modifier = Modifier.padding(end = 4.dp).height(16.dp))
                Text("PLAY", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.08.em), color = Color(0xFF1A1A1A))
            }
        }
    }
}
