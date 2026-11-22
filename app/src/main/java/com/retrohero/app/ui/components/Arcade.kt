package com.retrohero.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont

/** The bolder, playful "arcade" style: bright colours, chunky press-down cards, pixel labels. */
object Arcade {
    val Pink = Color(0xFFFF7A9C)
    val Sky = Color(0xFF6EC1FF)
    val Mint = Color(0xFF5FE3A1)
    val Butter = Color(0xFFFFD25E)
    val Grape = Color(0xFFB18CFF)
    val Tangerine = Color(0xFFFF9F55)
    /** Dark text on bright faces. */
    val Ink = Color(0xFF15131A)
    /** Dark card face. */
    val Panel = Color(0xFF1B1A20)

    val palette = listOf(Pink, Sky, Mint, Butter, Grape, Tangerine)

    /** Colour for nav tab [index]: Home pink, Search sky, Library mint, Cores butter, Settings grape. */
    fun tab(index: Int): Color = palette[index % palette.size]

    /** The darker ledge under a chunky face. */
    fun ledge(face: Color): Color = lerp(face, Color.Black, if (face == Panel) 0.45f else 0.42f)
}

/**
 * A chunky card: a solid ledge [depth] below the face, and the face sinks onto it when pressed
 * (with a little bounce on release), like an arcade button.
 */
@Composable
fun Chunky(
    modifier: Modifier = Modifier,
    face: Color = Arcade.Panel,
    ledge: Color = Arcade.ledge(face),
    shape: Shape = RoundedCornerShape(24.dp),
    depth: Dp = 5.dp,
    outline: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateFloatAsState(
        if (pressed) 1f else 0f,
        if (pressed) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "chunkySink",
    )
    val depthPx = with(LocalDensity.current) { depth.toPx() }
    Box(modifier.padding(bottom = depth)) {
        // The ledge sits behind the face, peeking out below it.
        Box(Modifier.matchParentSize().graphicsLayer { translationY = depthPx }.background(ledge, shape))
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { translationY = sink * depthPx * 0.85f }
                .background(face, shape)
                .then(if (outline != null) Modifier.border(2.dp, outline, shape) else Modifier),
        )
        Box(
            Modifier
                .graphicsLayer { translationY = sink * depthPx * 0.85f }
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.combinedClickable(interactionSource = interaction, indication = null, onClick = onClick ?: {}, onLongClick = onLongClick)
                    } else Modifier,
                ),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/** Small caps label in the pixel font. */
@Composable
fun PixelLabel(text: String, color: Color = Color.White.copy(alpha = 0.7f), modifier: Modifier = Modifier, size: Int = 9) {
    Text(text.uppercase(), style = TextStyle(fontFamily = PixelFont, fontSize = size.sp, lineHeight = (size * 1.6).sp), color = color, modifier = modifier)
}

/** Big playful page header: a coloured pixel tag, then a heavy title. */
@Composable
fun ArcadeHeader(title: String, tag: String, color: Color, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(8.dp))
                PixelLabel(tag, color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = (-0.045).em),
                color = Color.White,
            )
        }
        trailing()
    }
}

/** A chunky pill chip: filled with [color] when selected. */
@Composable
fun ArcadeChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chunky(
        modifier = modifier,
        face = if (selected) color else Arcade.Panel,
        shape = CircleShape,
        depth = 3.dp,
        onClick = onClick,
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = if (selected) Arcade.Ink else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
