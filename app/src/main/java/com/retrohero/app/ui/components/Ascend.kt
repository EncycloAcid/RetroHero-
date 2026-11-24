package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

object Ascend {
    object Radius {
        val Cover = 8.dp
        val Control = 10.dp
        val Card = 12.dp
        val Hero = 16.dp
    }

    val Surface: Color get() = Tone.Surface
    val Border: Color get() = Tone.Line
    val Muted: Color get() = Tone.Muted
    val CardShape = RoundedCornerShape(Radius.Card)
    private val isMono: Boolean get() = Salmon == com.retrohero.app.ui.theme.MonoAccentColor
    private val isLime: Boolean get() = Salmon == com.retrohero.app.ui.theme.AcidLimeColor
    private val isDeck: Boolean get() = Salmon == com.retrohero.app.ui.theme.DeckCyanColor
    private val drawnAccent: Color get() = Salmon.compositeOver(Tone.Bg)
    val accent: Color get() = when {
        isMono -> Tone.Ink
        isDeck -> if (Tone.isDark) Color(0xFF1AA8FF) else Color(0xFF0A8FE6)
        isLime -> if (Tone.isDark) Color(0xFFD8FF3E) else Color(0xFFC8F526)
        Tone.isDark && drawnAccent.luminance() < 0.25f -> lerp(Salmon, Color.White, 0.5f)
        !Tone.isDark && drawnAccent.luminance() > 0.6f -> lerp(Salmon, Color.Black, 0.35f)
        else -> Salmon
    }
    /** Readable accent colour for small inline text labels on Bg/Surface. */
    val accentText: Color get() = when {
        isLime && !Tone.isDark -> Color(0xFF557200)
        isDeck && !Tone.isDark -> Color(0xFF0773BA)
        else -> accent
    }
    val onAccent: Color get() = when {
        isMono -> Tone.Bg
        isDeck -> Color.White
        isLime -> Color(0xFF0A0B0A)
        accent.luminance() > 0.35f -> Color(0xFF0A0B0A)
        else -> Color.White
    }
    /** The glowing console focus ring drawn around the selected tile. */
    val focusRing: Color get() = if (Tone.isDark) Color(0xFFE9F6FF) else accent
}

fun Modifier.ascendCard(shape: androidx.compose.ui.graphics.Shape = Ascend.CardShape): Modifier =
    this.clip(shape).background(Ascend.Surface).border(1.dp, Ascend.Border, shape)

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    index: Int = 1,
    onClick: (() -> Unit)? = null,
    verticalPadding: Dp = 6.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 3.dp, height = 14.dp).clip(CircleShape).background(Ascend.accent))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.01).em,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Tone.Ink,
        )
        if (count != null) {
            Text(
                "$count titles",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.04.em),
                color = Tone.Muted,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Tone.Raised)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = Tone.Muted,
                modifier = Modifier.padding(start = 4.dp).size(18.dp),
            )
        }
    }
}

/**
 * Handheld Console OS game tile (Switch / Steam Deck): square-cornered box art on a slate plate,
 * a crisp focus ring while pressed, a play-time bar along the bottom edge and a compact title row.
 */
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    subtitle: String? = game.system?.shortName,
    onLongClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    progress: Float? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 1.04f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "gameCardScale",
    )
    val artShape = RoundedCornerShape(Ascend.Radius.Card)
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(
                    if (pressed) Modifier.border(2.5.dp, Ascend.focusRing, artShape).padding(4.dp)
                    else Modifier,
                )
                .clip(if (pressed) RoundedCornerShape(Ascend.Radius.Cover) else artShape)
                .background(Tone.Raised),
        ) {
            GameArt(game, Modifier.fillMaxSize(), showTag = false)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0F131A).copy(alpha = 0.78f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    game.system?.shortName ?: game.systemId.uppercase(),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.06.em),
                    color = Color.White,
                    maxLines = 1,
                )
            }
            if (onPlay != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Ascend.accent)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        "Play ${game.title}",
                        tint = Ascend.onAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            val barProgress = progress?.coerceIn(0f, 1f)
            if (barProgress != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    if (barProgress > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .background(Ascend.accent),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onToggleFavorite != null) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unpin ${game.title}" else "Pin ${game.title}",
                    tint = if (isFavorite) Ascend.accent else Tone.Faint,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable(onClick = onToggleFavorite),
                )
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = Tone.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Key(text, onClick, modifier, icon, primary = true)
}

@Composable
fun Key(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.96f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "keyScale",
    )
    val shape = CircleShape
    val view = LocalView.current
    val contentAlpha = if (enabled) 1f else 0.4f
    val fg = if (primary) Ascend.onAccent else Tone.Ink
    val bg = if (primary) Ascend.accent else Tone.Surface
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = contentAlpha }
            .height(46.dp)
            .clip(shape)
            .background(bg)
            .then(if (primary) Modifier else Modifier.border(1.dp, Tone.Line, shape))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(horizontal = 20.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Console OS tab strip: plain labels with a glowing accent bar under the active one. */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val selectedIndex = selected.coerceIn(options.indices)
    val trackShape = RoundedCornerShape(Ascend.Radius.Control)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(trackShape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, trackShape),
    ) {
        val optionWidth = maxWidth / options.size
        val indicatorX by animateDpAsState(
            (optionWidth.value * selectedIndex).dp,
            spring(dampingRatio = 0.75f, stiffness = 600f),
            label = "segmentIndicator",
        )
        Box(
            Modifier
                .offset(x = indicatorX)
                .width(optionWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(Ascend.Radius.Control - 3.dp))
                .background(Tone.Raised),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = indicatorX + optionWidth * 0.3f)
                .padding(bottom = 6.dp)
                .width(optionWidth * 0.4f)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(Ascend.accent),
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selectedIndex == index, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = if (selectedIndex == index) Tone.Ink else Tone.Muted,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
        }
    }
}
