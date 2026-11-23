package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone

object Ascend {
    object Radius {
        val Cover = 10.dp
        val Control = 12.dp
        val Card = 14.dp
        val Hero = 18.dp
    }

    val Surface: Color get() = Tone.Surface
    val Border: Color get() = Tone.Line
    val Muted: Color get() = Tone.Muted
    val CardShape = RoundedCornerShape(Radius.Card)
    private val drawnAccent: Color get() = Salmon.compositeOver(Tone.Bg)
    val accent: Color get() = when {
        Tone.isDark && drawnAccent.luminance() < 0.25f -> lerp(Salmon, Color.White, 0.5f)
        !Tone.isDark && drawnAccent.luminance() > 0.6f -> lerp(Salmon, Color.Black, 0.35f)
        else -> Salmon
    }
    val onAccent: Color get() = if (drawnAccent.luminance() > 0.18f) Color(0xFF111111) else Color.White
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
    Row(modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
        .padding(vertical = verticalPadding), verticalAlignment = Alignment.CenterVertically) {
        ArchiveLabel(index.toString().padStart(2, '0'), color = Tone.Faint)
        Text(
            title,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Tone.Ink,
        )
        if (count != null) ArchiveLabel(count.toString().padStart(2, '0'), Modifier.padding(start = 8.dp), color = Tone.Muted)
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted, modifier = Modifier.size(18.dp))
    }
}

/** Library card: cover, title, and a quiet info line. */
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "gameCardScale",
    )
    Box(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(
            Modifier.fillMaxWidth()
                .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick),
        ) {
            val coverShape = RoundedCornerShape(Ascend.Radius.Cover)
            Box(
                Modifier.fillMaxWidth().aspectRatio(0.73f)
                    .clip(coverShape)
                    .border(0.5.dp, Tone.Line, coverShape),
            ) {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
                color = Tone.Ink,
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
        if (onPlay != null) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.73f)) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Tone.Raised.copy(alpha = 0.94f))
                        .border(0.5.dp, Tone.Line, CircleShape)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = Tone.Ink, modifier = Modifier.size(16.dp)) }
            }
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
        if (pressed && enabled) 0.97f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "keyScale",
    )
    val translation by animateDpAsState(
        if (pressed && enabled) 1.dp else 0.dp,
        spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "keyTranslation",
    )
    val shape = RoundedCornerShape(Ascend.Radius.Control)
    val view = LocalView.current
    val depth = when {
        Tone.isDark || !enabled -> Modifier
        primary -> Modifier.shadow(4.dp, shape, spotColor = Ascend.accent.copy(alpha = 0.35f))
        else -> Modifier.shadow(
            2.dp,
            shape,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.10f),
        )
    }
    val contentAlpha = if (enabled) 1f else 0.4f
    Row(
        modifier
            .then(depth)
            .graphicsLayer { scaleX = scale; scaleY = scale; translationY = translation.toPx(); alpha = contentAlpha }
            .height(48.dp)
            .clip(shape)
            .background(if (primary) Ascend.accent else Tone.Raised)
            .border(1.dp, if (primary && !Tone.isDark) Color.Transparent else Tone.Line, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = if (primary) Ascend.onAccent else Tone.Ink, modifier = Modifier.padding(end = 6.dp).size(18.dp))
        Text(
            text,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp),
            color = if (primary) Ascend.onAccent else Tone.Ink,
        )
    }
}

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
            .height(48.dp)
            .then(
                if (Tone.isDark) Modifier else Modifier.shadow(
                    2.dp,
                    trackShape,
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.10f),
                ),
            )
            .clip(trackShape)
            .background(Tone.Raised)
            .border(1.dp, Tone.Line, trackShape)
            .padding(4.dp),
    ) {
        val optionWidth = maxWidth / options.size
        val indicatorX by animateDpAsState(
            (optionWidth.value * selectedIndex).dp,
            spring(dampingRatio = 0.7f, stiffness = 600f),
            label = "segmentIndicator",
        )
        val indicatorShape = RoundedCornerShape(Ascend.Radius.Control)
        Box(
            Modifier
                .offset(x = indicatorX)
                .width(optionWidth)
                .fillMaxHeight()
                .then(if (Tone.isDark) Modifier else Modifier.shadow(1.dp, indicatorShape))
                .clip(indicatorShape)
                .background(Tone.Ink),
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
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        color = if (selectedIndex == index) Tone.Bg else Tone.Ink,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
