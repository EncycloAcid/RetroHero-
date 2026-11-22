package com.retrohero.app.ui.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.retrohero.app.ui.theme.IosColors

// ---- Headers ---------------------------------------------------------------------------------

/** iOS large title, scrolled with the content. */
@Composable
fun LargeTitle(text: String, subtitle: String? = null, modifier: Modifier = Modifier, horizontalPadding: Dp = 20.dp) {
    Column(modifier.padding(start = horizontalPadding, end = horizontalPadding, top = 6.dp, bottom = 10.dp)) {
        Text(text, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Content section header ("Title 2") with an optional tinted action like "See All". */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    horizontalPadding: Dp = 20.dp,
    onAction: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = horizontalPadding, end = horizontalPadding, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(horizontal = 4.dp),
            )
        }
    }
}

// ---- Grouped (inset) lists -------------------------------------------------------------------

class GroupBuilder {
    internal val rows = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) {
        rows += content
    }
}

/** iOS inset-grouped section: small caps header, rounded card of rows with hairlines, footer. */
@Composable
fun GroupedSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    dividerInset: Dp = 60.dp,
    build: GroupBuilder.() -> Unit,
) {
    val rows = GroupBuilder().apply(build).rows
    Column(modifier.padding(horizontal = 16.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 7.dp, top = 22.dp),
            )
        } else {
            Spacer(Modifier.height(22.dp))
        }
        if (rows.isNotEmpty()) {
            Column(Modifier.clip(Squircles.Medium).background(MaterialTheme.colorScheme.surfaceContainer)) {
                rows.forEachIndexed { i, row ->
                    row()
                    if (i < rows.lastIndex) Hairline(Modifier.padding(start = dividerInset))
                }
            }
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
            )
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** A list row with iOS press highlight. */
@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val highlight by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f) else Color.Transparent,
        label = "rowHighlight",
    )
    Row(
        modifier
            .fillMaxWidth()
            .background(highlight)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .heightIn(min = if (subtitle != null) 58.dp else 46.dp)
            .padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

@Composable
fun Chevron() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        null,
        tint = MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
fun TrailingValue(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Settings-style coloured squircle with a white glyph. */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 30.dp) {
    Box(Modifier.size(size).clip(Squircles.Tile).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.64f))
    }
}

// ---- Controls --------------------------------------------------------------------------------

/** iOS switch with an expressive spring and the thumb stretch on press. */
@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bouncy = spring<Dp>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
    val track by animateColorAsState(
        if (checked) IosColors.Green else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "switchTrack",
    )
    val thumbWidth by animateDpAsState(if (pressed) 33.dp else 27.dp, bouncy, label = "thumbWidth")
    val thumbX by animateDpAsState(if (checked) 51.dp - 2.dp - thumbWidth else 2.dp, bouncy, label = "thumbX")
    Box(
        modifier
            .size(51.dp, 31.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(interactionSource = interaction, indication = null) { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = thumbX, y = 2.dp)
                .size(thumbWidth, 27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/** iOS segmented control with a spring-driven sliding thumb. */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background == Color.Black
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(2.dp),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(
            segment * selected,
            spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "segmentX",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .shadow(if (dark) 0.dp else 2.dp, RoundedCornerShape(7.dp))
                .background(if (dark) Color(0xFF636366) else Color.White, RoundedCornerShape(7.dp)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

enum class CapsuleStyle { Filled, Tinted, Gray, White }

/**
 * Capsule button (iOS "GET" / "Play" style) with M3 Expressive feedback: the capsule
 * squashes into a rounded rectangle and springs back.
 */
@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CapsuleStyle = CapsuleStyle.Filled,
    icon: ImageVector? = null,
    large: Boolean = false,
    enabled: Boolean = true,
    contentColor: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val height = if (large) 50.dp else 30.dp
    val corner by animateDpAsState(
        if (pressed) height * 0.28f else height / 2,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "capsuleCorner",
    )
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.5f, Spring.StiffnessMedium), label = "capsuleScale")
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (style) {
        CapsuleStyle.Filled -> scheme.primary to scheme.onPrimary
        CapsuleStyle.Tinted -> scheme.primary.copy(alpha = 0.15f) to scheme.primary
        CapsuleStyle.Gray -> scheme.surfaceContainerHigh to scheme.primary
        CapsuleStyle.White -> Color.White to Color.Black
    }
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .height(height)
            .defaultMinSize(minWidth = if (large) 0.dp else 72.dp)
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (large) 22.dp else 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = contentColor ?: fg
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(if (large) 22.dp else 16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            color = tint,
            maxLines = 1,
        )
    }
}

/** iOS search field: gray rounded pill with magnifier and clear button. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Cancel,
                "Clear",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onQueryChange("") },
            )
        }
    }
}

/** Capsule filter chip with an animated selection fill. */
@Composable
fun FilterCapsule(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.onSurface else scheme.surfaceContainerHigh, label = "chipBg")
    val fg by animateColorAsState(if (selected) scheme.surface else scheme.onSurface, label = "chipFg")
    Box(
        Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}
