package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import kotlin.math.abs

val LocalDockInset = compositionLocalOf { 104.dp }

private val ExpressiveSpring = spring<Float>(dampingRatio = 0.56f, stiffness = 520f)

data class DockTab(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private data class PosterPalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val glow: Color,
    val pop: Color,
    val ink: Color = Color.White,
)

private val ExpressivePosters = listOf(
    PosterPalette(Color(0xFF39328E), Color(0xFF1F1C57), Color(0xFF0E0D28), Color(0xFF9F9BFF), Color(0xFFFF7EB6)), // iris pop
    PosterPalette(Color(0xFF0D72C8), Color(0xFF08427D), Color(0xFF061A36), Color(0xFF66D1FF), Color(0xFF7BF1D8)), // ocean liquid
    PosterPalette(Color(0xFFD63B5A), Color(0xFF831D3B), Color(0xFF230816), Color(0xFFFF90A8), Color(0xFFFFCA62)), // berry coral
    PosterPalette(Color(0xFF1A8465), Color(0xFF0F4D3F), Color(0xFF071D19), Color(0xFF5CF2BC), Color(0xFF8BE2FF)), // emerald spring
    PosterPalette(Color(0xFFE06424), Color(0xFF8C3114), Color(0xFF240B07), Color(0xFFFFBA68), Color(0xFFFF6E8D)), // tangerine glow
    PosterPalette(Color(0xFF7B38B8), Color(0xFF441A6D), Color(0xFF160826), Color(0xFFE79BFF), Color(0xFF6CE5FF)), // orchid dream
    PosterPalette(Color(0xFFF6CF38), Color(0xFFD99A14), Color(0xFF311F06), Color(0xFFFFF49E), Color(0xFFFF6B4A), Color(0xFF161105)), // citrus burst
    PosterPalette(Color(0xFF2A4259), Color(0xFF152536), Color(0xFF091018), Color(0xFF94C8F7), Color(0xFF84F0C8)), // arctic slate
)

fun gameAccentColor(game: Game): Color =
    ExpressivePosters[abs(game.id.hashCode()) % ExpressivePosters.size].glow

fun systemTintColor(systemId: String): Color = when (systemId) {
    "psx", "ps2", "psp" -> Color(0xFF5B8CFF)
    "snes", "n64", "gc", "wii" -> Color(0xFF8C6EFF)
    "gba", "gbc", "gb" -> Color(0xFF2CD39A)
    "nds", "3ds" -> Color(0xFF38BDF8)
    "nes", "genesis", "sms" -> Color(0xFFFF5E6C)
    "dc", "dreamcast" -> Color(0xFFFF8838)
    else -> ExpressivePosters[abs(systemId.hashCode()) % ExpressivePosters.size].glow
}

/**
 * Loads Libretro / custom box art with Coil and renders an M3 Expressive editorial poster
 * fallback when offline, using responsive typography that never splits words mid-word.
 */
@Composable
fun ExpressivePosterArt(
    game: Game,
    modifier: Modifier = Modifier,
    showTypography: Boolean = true,
) {
    val sources = rememberArtSources(game)
    var index by remember(game.id, sources) { mutableIntStateOf(0) }
    var loaded by remember(game.id, sources) { mutableStateOf(false) }

    Box(modifier) {
        if (!loaded) {
            ExpressiveFallbackPoster(game, showTypography, Modifier.fillMaxSize())
        }
        val model = sources.getOrNull(index)
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = { if (index < sources.lastIndex) index++ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ExpressiveFallbackPoster(
    game: Game,
    showTypography: Boolean,
    modifier: Modifier = Modifier,
) {
    val hash = remember(game.id) { abs(game.id.hashCode()) }
    val p = ExpressivePosters[hash % ExpressivePosters.size]
    val motif = hash % 4
    val isLight = p.ink != Color.White

    BoxWithConstraints(
        modifier.background(
            Brush.linearGradient(
                0f to p.top,
                0.55f to p.mid,
                1f to p.bottom,
            ),
        ),
    ) {
        val compact = maxWidth < 112.dp
        val longestWord = remember(game.title) {
            game.title.split(" ", "-", ":").maxOfOrNull { it.length } ?: 6
        }
        val titleSp = when {
            maxWidth < 80.dp -> if (longestWord >= 8) 7.5f else 9f
            maxWidth < 116.dp -> if (longestWord >= 8) 9.5f else 11f
            maxWidth < 170.dp -> if (longestWord >= 9) 12.5f else 14.5f
            else -> if (longestWord >= 9) 18f else 21f
        }

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft iOS mesh-like glowing orbs
            drawCircle(
                brush = Brush.radialGradient(listOf(p.glow.copy(alpha = 0.55f), Color.Transparent)),
                radius = w * 0.78f,
                center = Offset(w * 0.28f, h * 0.24f),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(p.pop.copy(alpha = 0.36f), Color.Transparent)),
                radius = w * 0.65f,
                center = Offset(w * 0.86f, h * 0.78f),
            )

            // Playful M3 Expressive geometric artwork layers
            when (motif) {
                0 -> {
                    val c = Offset(w * 0.52f, h * 0.42f)
                    drawCircle(p.glow.copy(alpha = 0.22f), w * 0.36f, c)
                    drawCircle(Color.White.copy(alpha = 0.22f), w * 0.24f, c, style = Stroke(width = 2.2f))
                    drawCircle(p.pop.copy(alpha = 0.42f), w * 0.11f, Offset(w * 0.74f, h * 0.24f))
                }
                1 -> {
                    val wave = Path().apply {
                        moveTo(-w * 0.1f, h * 0.68f)
                        cubicTo(w * 0.25f, h * 0.38f, w * 0.68f, h * 0.82f, w * 1.1f, h * 0.34f)
                        lineTo(w * 1.1f, h * 0.56f)
                        cubicTo(w * 0.68f, h * 1.02f, w * 0.25f, h * 0.58f, -w * 0.1f, h * 0.88f)
                        close()
                    }
                    drawPath(wave, color = p.glow.copy(alpha = 0.24f))
                    drawCircle(p.pop.copy(alpha = 0.30f), w * 0.18f, Offset(w * 0.26f, h * 0.28f))
                }
                2 -> {
                    val diamond = Path().apply {
                        moveTo(w * 0.50f, h * 0.14f)
                        lineTo(w * 0.84f, h * 0.42f)
                        lineTo(w * 0.50f, h * 0.70f)
                        lineTo(w * 0.16f, h * 0.42f)
                        close()
                    }
                    drawPath(diamond, color = Color.White.copy(alpha = 0.12f))
                    drawPath(diamond, color = p.glow.copy(alpha = 0.40f), style = Stroke(width = 2f))
                }
                else -> {
                    for (i in 0..3) {
                        val r = w * (0.16f + i * 0.14f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.16f - i * 0.03f),
                            radius = r,
                            center = Offset(w * 0.50f, h * 0.42f),
                            style = Stroke(width = 1.8f),
                        )
                    }
                }
            }

            if (!isLight) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.46f),
                    ),
                )
            }

            // Liquid glass top-left specular gloss
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(w * 0.72f, h * 0.48f),
                ),
            )
            // Subtle inner glass rim
            val inset = 2f
            drawRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(inset, inset),
                size = Size(w - inset * 2f, h - inset * 2f),
                style = Stroke(width = 1f),
            )
        }

        if (showTypography) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 9.dp else 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = (game.system?.shortName ?: game.systemId).uppercase(),
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 7.5.sp else 9.sp,
                        letterSpacing = 0.14.em,
                    ),
                    color = p.ink.copy(alpha = 0.74f),
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = game.title,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Black,
                        fontSize = titleSp.sp,
                        lineHeight = (titleSp * 1.15f).sp,
                        letterSpacing = (-0.02).em,
                        textAlign = TextAlign.Center,
                        shadow = if (!isLight) Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), 6f) else null,
                    ),
                    color = p.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

/** Signature Material 3 Expressive organic polygon badge (Cookie, Clover, Sunny, SoftBurst…). */
@Composable
fun ExpressiveShapeBadge(
    polygon: RoundedPolygon,
    containerColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = polygon.toShape()
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Signature Material 3 Expressive Connected Button Group:
 * outer ends stay rounded, inner corners stay softly squared, while the selected pill
 * morphs smoothly into a full pill (CircleShape) with bouncy spring motion!
 */
@Composable
fun ExpressiveConnectedBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            val startR by animateDpAsState(
                when {
                    selected -> 24.dp
                    idx == 0 -> 20.dp
                    else -> 8.dp
                },
                spring(dampingRatio = 0.65f, stiffness = 500f),
                label = "connStart",
            )
            val endR by animateDpAsState(
                when {
                    selected -> 24.dp
                    idx == options.lastIndex -> 20.dp
                    else -> 8.dp
                },
                spring(dampingRatio = 0.65f, stiffness = 500f),
                label = "connEnd",
            )
            val bg by animateColorAsState(
                if (selected) Ascend.accent else Tone.Raised,
                tween(170),
                label = "connBg",
            )
            val fg by animateColorAsState(
                if (selected) Ascend.onAccent else Tone.Muted,
                tween(150),
                label = "connFg",
            )
            val shape = RoundedCornerShape(topStart = startR, bottomStart = startR, topEnd = endR, bottomEnd = endR)

            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(shape)
                    .background(bg)
                    .border(
                        width = if (selected) 0.dp else 0.75.dp,
                        color = if (selected) Color.Transparent else Tone.Line,
                        shape = shape,
                    )
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = {
                            if (!selected) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelect(idx)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        letterSpacing = (-0.01).em,
                    ),
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Signature Material 3 Expressive Split Action Button:
 * Bold pill primary CTA paired tightly with a companion icon button (4dp split seam).
 */
@Composable
fun ExpressiveSplitButton(
    label: String,
    icon: ImageVector,
    onPrimaryClick: () -> Unit,
    secondaryIcon: ImageVector,
    secondaryDescription: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    secondaryHighlighted: Boolean = false,
) {
    val view = LocalView.current
    val primaryInteraction = remember { MutableInteractionSource() }
    val primaryPressed by primaryInteraction.collectIsPressedAsState()
    val primaryScale by animateFloatAsState(if (primaryPressed) 0.96f else 1f, ExpressiveSpring, label = "splitPrim")

    val secInteraction = remember { MutableInteractionSource() }
    val secPressed by secInteraction.collectIsPressedAsState()
    val secScale by animateFloatAsState(if (secPressed) 0.92f else 1f, ExpressiveSpring, label = "splitSec")

    val primaryShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 10.dp, bottomEnd = 10.dp)
    val secondaryShape = RoundedCornerShape(
        topStart = if (secondaryHighlighted) 24.dp else 10.dp,
        bottomStart = if (secondaryHighlighted) 24.dp else 10.dp,
        topEnd = 24.dp,
        bottomEnd = 24.dp,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(height)
                .graphicsLayer { scaleX = primaryScale; scaleY = primaryScale }
                .shadow(
                    elevation = 14.dp,
                    shape = primaryShape,
                    ambientColor = Ascend.accent.copy(alpha = 0.45f),
                    spotColor = Ascend.accent.copy(alpha = 0.55f),
                )
                .clip(primaryShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Ascend.accent, Ascend.accent.copy(alpha = 0.88f)),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.Transparent)),
                    shape = primaryShape,
                )
                .clickable(interactionSource = primaryInteraction, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onPrimaryClick()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = Ascend.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = (-0.01).em),
                color = Ascend.onAccent,
                maxLines = 1,
            )
        }

        Box(
            Modifier
                .size(width = 54.dp, height = height)
                .graphicsLayer { scaleX = secScale; scaleY = secScale }
                .clip(secondaryShape)
                .background(if (secondaryHighlighted) Color(0xFFFF4D6D) else Tone.tonalSurface(Ascend.accent))
                .border(
                    width = 1.dp,
                    color = if (secondaryHighlighted) Color.White.copy(alpha = 0.25f) else Ascend.accent.copy(alpha = 0.35f),
                    shape = secondaryShape,
                )
                .clickable(interactionSource = secInteraction, indication = null) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onSecondaryClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = secondaryIcon,
                contentDescription = secondaryDescription,
                tint = if (secondaryHighlighted) Color.White else Ascend.accent,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/**
 * Signature M3 Expressive Floating Toolbar × iOS Liquid Glass Dock:
 * Frosted glass navigation capsule paired with a detached organic Cookie7Sided `+` FAB!
 */
@Composable
fun ExpressiveIosDock(
    tabs: List<DockTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val dark = Tone.isDark
    val capsuleShape = RoundedCornerShape(32.dp)
    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val fabScale by animateFloatAsState(if (fabPressed) 0.90f else 1f, ExpressiveSpring, label = "fabScale")
    val fabShape = MaterialShapes.Cookie7Sided.toShape()

    // Soft bottom dissolve scrim like iOS 26 Liquid Glass dock
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.46f to Tone.Bg.copy(alpha = 0.80f),
                    1f to Tone.Bg.copy(alpha = 0.97f),
                ),
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Frosted iOS Liquid-Glass Floating Capsule
            Row(
                Modifier
                    .weight(1f)
                    .height(62.dp)
                    .shadow(
                        elevation = if (dark) 24.dp else 14.dp,
                        shape = capsuleShape,
                        ambientColor = Color.Black.copy(alpha = if (dark) 0.85f else 0.18f),
                        spotColor = Color.Black.copy(alpha = if (dark) 0.90f else 0.22f),
                    )
                    .clip(capsuleShape)
                    .background(Tone.Glass)
                    .border(1.dp, Tone.glassBorder, capsuleShape)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { idx, tab ->
                    val active = idx == selected
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, ExpressiveSpring, label = "dockScale")
                    val pillBg by animateColorAsState(
                        if (active) Ascend.accent.copy(alpha = if (dark) 0.24f else 0.16f) else Color.Transparent,
                        tween(180),
                        label = "dockBg",
                    )
                    val fg by animateColorAsState(
                        if (active) Ascend.accent else Tone.Muted,
                        tween(160),
                        label = "dockFg",
                    )

                    Box(
                        Modifier
                            .weight(if (active) 1.48f else 0.95f)
                            .fillMaxHeight()
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(pillBg)
                            .then(
                                if (active) Modifier.border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Ascend.accent.copy(alpha = 0.55f), Ascend.accent.copy(alpha = 0.18f)),
                                    ),
                                    shape = CircleShape,
                                ) else Modifier,
                            )
                            .selectable(
                                selected = active,
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                                onClick = {
                                    if (!active) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onSelect(idx)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) {
                                Icon(tab.selectedIcon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(19.dp))
                                Text(
                                    text = tab.label,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = (-0.01).em),
                                    color = if (dark) Tone.Ink else fg,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        } else {
                            Icon(tab.unselectedIcon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }

            // Detached M3 Expressive Organic Scallop/Cookie `+` Action FAB with soft radial aura
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .semantics { contentDescription = "Quick add menu" }
                    .clickable(interactionSource = fabInteraction, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onFabClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                val aura = Ascend.accent
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(aura.copy(alpha = 0.52f), aura.copy(alpha = 0.16f), Color.Transparent),
                        ),
                        radius = size.width * 0.54f,
                    )
                }
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(fabShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Ascend.accent, Ascend.accent.copy(alpha = 0.84f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Ascend.onAccent, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

/** iOS Settings-style grouped inset surface with frosted glass rim highlight. */
@Composable
fun IosGroupedSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.10.em),
            color = Tone.Muted,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Tone.Surface)
                .border(1.dp, Tone.glassBorder, shape),
            content = content,
        )
    }
}

@Composable
fun IosInsetRow(
    icon: ImageVector,
    badgePolygon: RoundedPolygon,
    badgeTint: Color,
    title: String,
    subtitle: String? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            ExpressiveShapeBadge(
                polygon = badgePolygon,
                containerColor = badgeTint.copy(alpha = if (Tone.isDark) 0.20f else 0.14f),
                size = 38.dp,
            ) {
                Icon(icon, null, tint = badgeTint, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = Tone.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = Tone.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Tone.Faint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.75.dp,
                color = Tone.Line,
                modifier = Modifier.padding(start = 66.dp, end = 16.dp),
            )
        }
    }
}
