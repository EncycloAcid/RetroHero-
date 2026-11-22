package com.retrohero.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSort
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.GameBox3D
import com.retrohero.app.ui.components.GameMedia
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.Media
import com.retrohero.app.ui.components.boxSpec
import com.retrohero.app.ui.components.rememberDeviceTilt
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import kotlin.math.abs

/**
 * Your collection as 3D game boxes. Swipe through them (they turn as they pass, like a cover
 * flow), tilt your phone or hold a finger on one to rock it, and tap to open it: the lid swings
 * open, the cartridge or disc slides out, and you press Play.
 */
@Composable
fun ShelfScreen(
    games: List<Game>,
    stats: Map<String, PlayStat>,
    recentIds: List<String>,
    favorites: Set<String>,
    tiltEnabled: Boolean,
    reducedMotion: Boolean,
    onPlay: (Game) -> Unit,
    onDetails: (Game) -> Unit,
    startWithSpines: Boolean = true,
) {
    var system by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(GameSort.Recent) }
    var sortMenu by remember { mutableStateOf(false) }
    // Spines (a video-store wall) or the 3D cover flow.
    var spines by rememberSaveable { mutableStateOf(startWithSpines) }
    var opened by remember { mutableStateOf<Game?>(null) }
    val systems = remember(games) { val ids = games.mapTo(HashSet()) { it.systemId }; Systems.all.filter { it.id in ids } }
    val shown = remember(games, system, sort, stats, recentIds) {
        GameSort.apply(games.filter { system == null || it.systemId == system }, sort, stats, recentIds)
    }
    val tilt by rememberDeviceTilt(enabled = tiltEnabled && !reducedMotion && opened == null)
    val view = LocalView.current

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val focus = shown.getOrNull(0)
        Column(Modifier.fillMaxSize()) {
            com.retrohero.app.ui.components.PageHeader(
                "Shelf",
                "${shown.size} ${if (shown.size == 1) "game" else "games"} · ${sort.label}",
                Modifier.padding(start = 22.dp, end = 12.dp),
            ) {
                Box(
                    Modifier.padding(end = 8.dp).size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { spines = !spines },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (spines) Icons.Rounded.ViewCarousel else Icons.Rounded.ViewWeek, if (spines) "Show as cover flow" else "Show as spines", tint = Color.White) }
                Box {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { sortMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.SwapVert, "Sort", tint = Color.White) }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }, shape = RoundedCornerShape(20.dp)) {
                        GameSort.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label, fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sort = s; sortMenu = false },
                            )
                        }
                    }
                }
            }
            if (systems.size > 1) {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    item { ShelfChip("All", system == null) { system = null } }
                    items(systems, key = { it.id }) { s -> ShelfChip(s.shortName, system == s.id, s.color) { system = if (system == s.id) null else s.id } }
                }
            }
            if (shown.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No games here yet.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.55f))
                }
                return@Column
            }
            if (spines) {
                SpineShelf(shown, onPick = { opened = it }, modifier = Modifier.weight(1f))
                return@Column
            }
            val pager = rememberPagerState { shown.size }
            LaunchedEffect(system, sort) { pager.scrollToPage(0) }
            LaunchedEffect(pager) {
                snapshotFlow { pager.currentPage }.collect { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val boxW = (maxWidth * 0.56f).coerceAtMost(260.dp)
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = (maxWidth - boxW) / 2),
                    pageSpacing = (-28).dp,
                    beyondViewportPageCount = 2,
                    key = { shown[it].id },
                    modifier = Modifier.fillMaxSize(),
                ) { i ->
                    val game = shown[i]
                    val offset = (pager.currentPage - i) + pager.currentPageOffsetFraction
                    var finger by remember { mutableStateOf(Offset.Zero) }
                    val fx by animateFloatAsState(finger.x, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow), label = "fx")
                    val fy by animateFloatAsState(finger.y, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow), label = "fy")
                    val turn = if (reducedMotion) 0f else (offset * 42f).coerceIn(-60f, 60f)
                    val near = 1f - abs(offset).coerceAtMost(1f)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(near)
                            .graphicsLayer {
                                val s = 1f - 0.16f * abs(offset).coerceAtMost(1.5f)
                                scaleX = s
                                scaleY = s
                                alpha = 1f - 0.35f * abs(offset).coerceAtMost(1.5f)
                            }
                            // Hold a finger on the box to rock it toward your finger, like a trading card.
                            .pointerInput(game.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val e = awaitPointerEvent(PointerEventPass.Initial)
                                        val c = e.changes.firstOrNull() ?: continue
                                        finger = if (c.pressed) {
                                            Offset(
                                                ((c.position.x / size.width) - 0.5f) * 36f,
                                                -((c.position.y / size.height) - 0.5f) * 30f,
                                            )
                                        } else Offset.Zero
                                    }
                                }
                            }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                if (abs(offset) < 0.3f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    opened = game
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        GameBox3D(
                            game,
                            width = if (boxSpec(game.systemId).aspect > 1.2f) boxW * 1.15f else boxW,
                            rotY = turn + (tilt.x + fx) * near,
                            rotX = (tilt.y + fy) * near,
                        )
                    }
                }
            }
            // What's in front.
            val current = shown.getOrNull(pager.currentPage)
            if (current != null) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 12.dp + LocalNavSpace.current),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(current.title, style = MaterialTheme.typography.headlineSmallEmphasized, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    val stat = stats[current.id]
                    Text(
                        listOfNotNull(
                            current.system?.name,
                            stat?.takeIf { it.seconds > 0 }?.let { PlayTime.format(it.seconds) + " played" },
                            "♥".takeIf { current.id in favorites },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${pager.currentPage + 1} / ${shown.size} · tap the box to open it",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        opened?.let { g ->
            OpenBox(
                game = g,
                reducedMotion = reducedMotion,
                onPlay = { onPlay(g); opened = null },
                onDetails = { onDetails(g); opened = null },
                onClose = { opened = null },
            )
        }
    }
}

@Composable
private fun ShelfChip(text: String, selected: Boolean, color: Color = Color.White, onClick: () -> Unit) =
    com.retrohero.app.ui.components.PillChip(text, selected, onClick, color = color)

/**
 * The box comes forward, its lid swings open and the cartridge or disc slides out below it;
 * then Play. Tap outside or press back to put it away.
 */
@Composable
private fun OpenBox(game: Game, reducedMotion: Boolean, onPlay: () -> Unit, onDetails: () -> Unit, onClose: () -> Unit) {
    val spec = boxSpec(game.systemId)
    val enter = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val lid = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val out = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val spin = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val view = LocalView.current
    LaunchedEffect(Unit) {
        if (reducedMotion) return@LaunchedEffect
        enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
        lid.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        out.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    if (spec.media == Media.Disc && !reducedMotion) {
        LaunchedEffect(Unit) {
            spin.animateTo(360f * 40, infiniteRepeatable(tween(1600 * 40, easing = LinearEasing)))
        }
    }
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        if (!reducedMotion) {
            out.animateTo(0f, tween(220))
            lid.animateTo(0f, tween(240))
            enter.animateTo(0f, tween(200))
        }
        onClose()
    }
    BackHandler { closing = true }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.97f), lerp(game.system?.color ?: Color.Black, Color.Black, 0.78f))))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { closing = true },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).navigationBarsPadding()) {
            val boxW = (maxWidth * 0.4f).coerceAtMost(210.dp) * (if (spec.aspect > 1.2f) 1.25f else 1f)
            val mediaW = if (spec.media == Media.Disc) boxW * 0.82f else boxW * 0.78f
            // Media slides out from inside the box down below it.
            val mediaY = (maxHeight * 0.18f) * out.value
            val boxLift = maxHeight * 0.12f
            Box(Modifier.fillMaxSize().padding(bottom = 150.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .zIndex(if (out.value > 0.5f) 2f else 0f)
                        .graphicsLayer {
                            translationY = mediaY.toPx() + (1f - out.value) * -40f
                            alpha = (out.value * 3f).coerceIn(0f, 1f)
                            rotationZ = (1f - out.value) * -8f
                            scaleX = 0.85f + 0.15f * out.value
                            scaleY = 0.85f + 0.15f * out.value
                        },
                ) { GameMedia(game, mediaW, spin = spin.value % 360f) }
                Box(
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            translationY = -boxLift.toPx() * out.value
                            scaleX = 0.8f + 0.2f * enter.value
                            scaleY = 0.8f + 0.2f * enter.value
                        },
                ) { GameBox3D(game, boxW, rotY = -14f * lid.value, rotX = 6f * lid.value, lid = lid.value) }
            }
            // Title and actions.
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp).graphicsLayer { alpha = out.value; translationY = (1f - out.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(game.title, style = MaterialTheme.typography.headlineSmallEmphasized, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(game.system?.name.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(Icons.Rounded.Close, "Put it back") { closing = true }
                    Row(
                        Modifier
                            .height(58.dp)
                            .width(190.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f))
                            .clickable(onClick = onPlay),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF1A1A1A), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play", style = MaterialTheme.typography.titleMediumEmphasized, color = Color(0xFF1A1A1A))
                    }
                    RoundAction(Icons.Rounded.Info, "Game details", onDetails)
                }
            }
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White) }
}
