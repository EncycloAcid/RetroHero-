package com.retrohero.app.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.AccentButton
import com.retrohero.app.ui.components.SectionTitle
import com.retrohero.app.ui.components.PageHeader
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.launchMarkTarget
import androidx.compose.foundation.pager.PagerState
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import androidx.compose.foundation.layout.fillMaxHeight
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.GameCard
import com.retrohero.app.ui.components.MorphShape
import com.retrohero.app.ui.components.rememberMorph
import com.retrohero.app.ui.components.launchRise
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.polygon
import com.retrohero.app.ui.theme.tag
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ---- Layout & type ------------------------------------------------------------------------------

/** Shared page gutter. */
private val ContentInset: Dp
    @Composable @ReadOnlyComposable get() = Page.Gutter

/** Three covers and a peek at the next one on phones. */
private val RowCardWidth: Dp
    @Composable @ReadOnlyComposable get() {
        val w = LocalConfiguration.current.screenWidthDp.dp
        return ((w - ContentInset * 2 - CardSpacing * 2) / 3.15f).coerceIn(94.dp, 146.dp)
    }

/** The gacha scales with the screen height so short phones still see the rows below it. */
private val HeroHeight: Dp
    @Composable @ReadOnlyComposable get() =
        (LocalConfiguration.current.screenHeightDp * 0.6f).coerceIn(430f, GachaHeight).dp

private val CardSpacing = 12.dp
private val Tracking = (-0.04).em

private val LabelStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = Tracking)
private val HeroTitleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = Tracking)
private val CardTitleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = (-0.03).em)
private val CardSubtitleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = (-0.02).em)
private val LabelColor = Color.White.copy(alpha = 0.69f)
private val SubtleColor = Color.White.copy(alpha = 0.55f)

// ---- Gacha reel geometry (in hero units: the hero is GachaHeight units tall) --------------------

private const val GachaHeight = 527f
private const val ItemSize = 187f
private const val FocusSize = 221f
private const val Pitch = 207f
private const val FocusGap = 17f
private const val CenterY = 230.5f
private const val Overhang = 28f
private const val PointerX = 120f
private const val PointerY = 236f
private const val PointerSize = 72f
private const val MaxCandidates = 10
private const val MinRollMillis = 3500
private const val MaxRollMillis = 9000
private val RollEasing = CubicBezierEasing(0.15f, 0.45f, 0.2f, 1f)
private val PipColor = Color(0xFF3C3C3C)

private enum class Phase { Idle, Rolling, Landed }

@Stable
private class GachaState {
    val reel = Animatable(0f)
    val landPop = Animatable(1f)
    val burst = Animatable(0f)
    val pointerKick = Animatable(0f)
    var phase by mutableStateOf(Phase.Idle)
    var result by mutableStateOf<Game?>(null)
}

/** Signed distance of cover [index] from the focus slot, wrapping around the reel. */
private fun reelDistance(index: Int, reel: Float, count: Int): Float {
    var d = ((index - reel) % count + count) % count
    if (d > count / 2f) d -= count
    return d
}

/** Pure Minimal Monochrome Play screen: flat OLED/white canvas, editorial Hero Landing, live console filter strip, and immediate 3-column box-art wall. */
@Composable
fun HomeScreen(
    games: List<Game>,
    recents: List<Game>,
    onPlay: (Game) -> Unit,
    onOpen: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    favorites: List<Game> = emptyList(),
    onOpenSystem: (GameSystem) -> Unit,
    onSeeAllGames: () -> Unit,
    topActions: @Composable () -> Unit,
    bottomPadding: Dp,
    stats: Map<String, PlayStat> = emptyMap(),
    collections: List<Pair<GameCollection, List<Game>>> = emptyList(),
    onOpenCollection: (GameCollection) -> Unit = {},
    pageIndex: Int = 1,
    onOpenStats: () -> Unit = {},
) {
    val mostPlayed = remember(games, stats) {
        games.filter { (stats[it.id]?.seconds ?: 0) >= 60 }.sortedByDescending { stats[it.id]?.seconds ?: 0 }.take(5)
    }
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }
    val candidates = remember(games, recents) {
        (recents + games.shuffled()).distinctBy { it.id }.take(MaxCandidates)
    }
    var shuffleOpen by rememberSaveable { mutableStateOf(false) }
    var activeSystem by rememberSaveable { mutableStateOf<String?>(null) }

    if (shuffleOpen) Dialog(onDismissRequest = { shuffleOpen = false }) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
            GachaHero(candidates) { shuffleOpen = false; onPlay(it) }
            IconButton({ shuffleOpen = false }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
        }
    }

    val featured = remember(games, recents, favorites) {
        (recents.take(3) + favorites.take(2) + games.take(4)).distinctBy { it.id }.take(4)
    }
    val pager = rememberPagerState { featured.size.coerceAtLeast(1) }
    val gridGames = remember(games, recents, activeSystem) {
        val ordered = (recents + games).distinctBy { it.id }
        val filtered = if (activeSystem == null) ordered else ordered.filter { it.systemId == activeSystem }
        filtered.take(15)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().background(Tone.Bg),
    ) {
        // Minimal Flat Monochrome Top Bar
        item(key = "mono_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    androidx.compose.foundation.Image(
                        androidx.compose.ui.res.painterResource(com.retrohero.app.R.drawable.ic_logo_mark),
                        null,
                        Modifier.size(20.dp).launchMarkTarget(),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Tone.Ink),
                    )
                    Text(
                        "RetroHero",
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            letterSpacing = (-0.03).em,
                        ),
                        color = Tone.Ink,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Tone.Surface)
                            .border(1.dp, Tone.Line, CircleShape)
                            .clickable(onClick = onOpenStats),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Insights, "Play stats", tint = Tone.Ink, modifier = Modifier.size(18.dp))
                    }
                    Row(
                        Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(Tone.Surface)
                            .border(1.dp, Tone.Line, CircleShape)
                            .clickable {
                                if (candidates.size > 1) shuffleOpen = true else candidates.firstOrNull()?.let(onPlay)
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, tint = Tone.Ink, modifier = Modifier.size(15.dp))
                        Text(
                            "Shuffle",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                            color = Tone.Ink,
                        )
                    }
                    topActions()
                }
            }
        }

        // Clean Editorial Hero Landing (Box Art First)
        if (featured.isNotEmpty()) {
            item(key = "mono_hero", span = { GridItemSpan(maxLineSpan) }) {
                MonoHeroLanding(
                    pager = pager,
                    featured = featured,
                    recent = recents.firstOrNull(),
                    onOpen = onOpen,
                    onPlay = onPlay,
                    onOptions = onOptions,
                )
            }
        }

        // Flat Monochrome Console Filter Bar + Section Title
        item(key = "grid_controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (systems.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "filter_all") {
                            MonoConsolePill(
                                label = "All",
                                count = games.size,
                                selected = activeSystem == null,
                                onClick = { activeSystem = null },
                            )
                        }
                        items(systems, key = { "sys_filter_${it.id}" }) { sys ->
                            MonoConsolePill(
                                label = sys.shortName,
                                count = counts[sys.id],
                                selected = activeSystem == sys.id,
                                onClick = { activeSystem = if (activeSystem == sys.id) null else sys.id },
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val heading = if (activeSystem == null) {
                        if (recents.isNotEmpty()) "Library" else "Games"
                    } else {
                        systems.firstOrNull { it.id == activeSystem }?.name ?: "Games"
                    }
                    Text(
                        heading,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                    )
                    Text(
                        "See all (${games.size})",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        color = Tone.Muted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onSeeAllGames)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }

        // Dense 3-Column Pure Box-Art Wall (9+ covers visible right on Play!)
        items(gridGames, key = { "wall_${it.id}" }) { game ->
            val sub = stats[game.id]?.takeIf { it.seconds >= 60 }?.let { PlayTime.format(it.seconds) }
                ?: game.system?.shortName
            GameCard(
                game = game,
                onClick = { onOpen(game) },
                subtitle = sub,
                onLongClick = { onOptions(game) },
                onPlay = { onPlay(game) },
            )
        }

        // Flat Collections Strip (if present)
        if (favorites.isNotEmpty() || collections.isNotEmpty()) {
            item(key = "mono_collections", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.Line))
                    Text(
                        "Collections",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (favorites.isNotEmpty()) {
                            item(key = "fav_mono") {
                                MonoCollectionTile(
                                    title = "Favourites",
                                    count = favorites.size,
                                    games = favorites,
                                    onClick = { onOpen(favorites.first()) },
                                )
                            }
                        }
                        items(collections, key = { "mcol_${it.first.id}" }) { (col, items) ->
                            MonoCollectionTile(
                                title = col.name,
                                count = items.size,
                                games = items,
                                onClick = { onOpenCollection(col) },
                            )
                        }
                    }
                }
            }
        }

        // Ultra-Clean Flat Most Played List (hairline dividers, no boxy cards)
        if (mostPlayed.isNotEmpty()) {
            item(key = "mono_most_played", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.Line))
                    Text(
                        "Most played",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                    mostPlayed.forEachIndexed { i, game ->
                        MonoFlatGameRow(
                            game = game,
                            detail = "${game.system?.shortName ?: game.systemId} · ${PlayTime.format(stats[game.id]?.seconds ?: 0)}",
                            onOpen = { onOpen(game) },
                            onPlay = { onPlay(game) },
                            onLongClick = { onOptions(game) },
                            showDivider = i < mostPlayed.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

// ---- Gacha hero -----------------------------------------------------------------------------------

@Composable
private fun GachaHero(candidates: List<Game>, onPlay: (Game) -> Unit) {
    val state = remember { GachaState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val count = candidates.size

    // A free-running clock for the drifting light.
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                if (last >= 0) clock += (now - last) / 1000f
                last = now
            }
        }
    }

    // Every cover passing the pointer ticks: a light haptic and the pointer knocked back.
    LaunchedEffect(state) {
        var lastBuzz = 0L
        snapshotFlow { state.reel.value.roundToInt() }.drop(1).collect {
            if (state.phase != Phase.Rolling) return@collect
            val now = System.currentTimeMillis()
            if (now - lastBuzz >= 45L) {
                lastBuzz = now
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            launch {
                state.pointerKick.snapTo(1f)
                state.pointerKick.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 900f))
            }
        }
    }

    fun roll() {
        if (state.phase == Phase.Rolling || count < 2) return
        scope.launch {
            state.phase = Phase.Rolling
            state.result = null
            try {
                val winner = Random.nextInt(count)
                val duration = Random.nextInt(MinRollMillis, MaxRollMillis + 1)
                val start = state.reel.value.roundToInt()
                state.reel.snapTo(start.toFloat())
                // whole turns first (more on a longer roll), then on to the winner
                val spins = count * (2 + duration / 2500)
                val landing = start + spins + (winner - (start + spins)).mod(count)
                state.reel.animateTo(landing.toFloat(), tween(duration, easing = RollEasing))
                state.result = candidates[winner]
                state.phase = Phase.Landed
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                launch {
                    state.landPop.snapTo(1f)
                    state.landPop.animateTo(1.1f, tween(120))
                    state.landPop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 320f))
                }
                launch {
                    state.burst.snapTo(0.001f)
                    state.burst.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)))
                    state.burst.snapTo(0f)
                }
            } finally {
                if (state.phase == Phase.Rolling) state.phase = Phase.Idle
            }
        }
    }

    val focused = candidates.getOrNull(state.reel.value.roundToInt().mod(count.coerceAtLeast(1)))
    val base = focused?.system?.color ?: Color(0xFF7C82B4)
    val light by animateColorAsState(lerp(base, Color.White, 0.5f), tween(700), label = "washLight")
    val deep by animateColorAsState(lerp(lerp(base, Color(0xFF7C82B4), 0.35f), Color.Black, 0.25f), tween(700), label = "washDeep")
    val rush by animateFloatAsState(if (state.phase == Phase.Rolling) 1f else 0f, tween(600), label = "rush")

    BoxWithConstraints(Modifier.fillMaxWidth().height(HeroHeight).clipToBounds()) {
        val unit = maxHeight / GachaHeight
        val focus = unit * FocusSize
        val coverShape = RoundedCornerShape(Ascend.Radius.Hero)

        Box(Modifier.fillMaxSize().launchRise(0)) {

        // The colour wash, a wandering light and the glow behind the big cover.
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = size.width
                val h = size.height
                val t = clock
                drawRect(deep)
                val sway = sin(t * 0.35f) * w * 0.12f
                drawRect(
                    Brush.linearGradient(
                        listOf(deep, lerp(deep, light, 0.55f), light),
                        start = Offset(w * 0.15f - sway, h),
                        end = Offset(w * 0.85f + sway, 0f),
                    ),
                )
                val wander = Offset(w * (0.18f + 0.08f * sin(t * 0.23f)), h * (0.55f + 0.1f * sin(t * 0.31f + 1.3f)))
                drawCircle(Brush.radialGradient(listOf(light.copy(alpha = 0.35f), Color.Transparent), wander, w * 0.7f), w * 0.7f, wander)
                val focusPx = focus.toPx()
                val glowCenter = Offset(w + (unit * Overhang).toPx() - focusPx / 2f, (unit * CenterY).toPx() + sin(t * 0.8f) * focusPx * 0.04f)
                val win = state.burst.value
                val flare = if (win > 0f) sin(win * PI.toFloat()) * 0.55f else 0f
                val glowR = focusPx * (1.05f + 0.05f * sin(t * 1.4f) + rush * 0.35f + flare)
                drawCircle(
                    Brush.radialGradient(
                        listOf(light.copy(alpha = 0.75f + rush * 0.2f), light.copy(alpha = 0.32f), Color.Transparent),
                        glowCenter,
                        glowR,
                    ),
                    glowR,
                    glowCenter,
                )
            },
        )

        // The reel: covers anchored on the right edge, tipping away like a drum and fading out.
        candidates.forEachIndexed { index, game ->
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = unit * Overhang, y = unit * CenterY - focus / 2)
                    .size(focus)
                    .graphicsLayer {
                        val d = reelDistance(index, state.reel.value, count)
                        val near = (1f - abs(d)).coerceIn(0f, 1f)
                        val landed = if (near > 0.5f) state.landPop.value else 1f
                        val scale = (ItemSize + (FocusSize - ItemSize) * near) / FocusSize * landed
                        scaleX = scale
                        scaleY = scale * (1f + 0.08f * rush)
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        translationY = (d * Pitch + FocusGap * d.coerceIn(-1f, 1f)) * unit.toPx()
                        rotationX = (-d * 18f).coerceIn(-50f, 50f)
                        cameraDistance = 14f * density
                        alpha = (2.7f - abs(d)).coerceIn(0f, 1f)
                        shape = coverShape
                        clip = true
                    },
            ) {
                GameArt(game, Modifier.fillMaxSize(), showTag = true)
            }
        }

        // Top and bottom shading over the reel; the bottom melts into the black page.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.85f),
                    0.22f to Color.Black.copy(alpha = 0.3f),
                    0.44f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color.Black,
                ),
            ),
        )

        // The pointer: a rounded play shape, knocked back by every cover that ticks past.
        val pointerShape = MaterialShapes.Triangle.toShape()
        Box(
            Modifier
                .offset(x = unit * (PointerX - PointerSize / 2), y = unit * (PointerY - PointerSize / 2))
                .size(unit * PointerSize)
                .graphicsLayer {
                    val kick = state.pointerKick.value
                    translationX = -kick * 7.dp.toPx()
                    rotationZ = 90f - kick * 9f
                    val win = state.burst.value
                    val pop = if (win > 0f) 1f + 0.18f * sin(win * PI.toFloat()) else 1f
                    scaleX = pop
                    scaleY = pop
                }
                .background(Salmon, pointerShape),
        )

        // The big cover is a button too: roll, or play the result once landed.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = unit * Overhang, y = unit * CenterY - focus / 2)
                .size(focus)
                .clickable(interactionSource = null, indication = null, enabled = state.phase != Phase.Rolling) {
                    val r = state.result
                    if (state.phase == Phase.Landed && r != null) onPlay(r) else roll()
                },
        )

        }

        HeroText(
            label = when (state.phase) {
                Phase.Idle -> "Shuffle"
                Phase.Rolling -> "Rolling…"
                Phase.Landed -> "You got"
            },
            title = state.result?.title ?: "Random pick",
            modifier = Modifier.align(Alignment.BottomStart).launchRise(1),
        ) {
            when (state.phase) {
                Phase.Landed -> {
                    HeroPill(onClick = { state.result?.let(onPlay) }) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = PipColor, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("play", style = LabelStyle, color = PipColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    HeroPill(onClick = ::roll, compact = true) { Die(spinning = false) }
                }
                Phase.Rolling -> HeroPill(onClick = {}, enabled = false) {
                    Die(spinning = true)
                    Spacer(Modifier.width(8.dp))
                    Text("rolling", style = LabelStyle, color = PipColor)
                }
                Phase.Idle -> HeroPill(onClick = ::roll, enabled = count >= 2) {
                    Die(spinning = false)
                    Spacer(Modifier.width(8.dp))
                    Text("roll!!", style = LabelStyle, color = PipColor)
                }
            }
        }
    }
}

// ---- Pure Minimal Monochrome (Box Art First) Play Components -----------------------------------

@Composable
private fun MonoHeroLanding(
    pager: PagerState,
    featured: List<Game>,
    recent: Game?,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onOptions: (Game) -> Unit,
) {
    val heroShape = RoundedCornerShape(12.dp)
    HorizontalPager(
        state = pager,
        pageSpacing = 10.dp,
        modifier = Modifier.fillMaxWidth().height(212.dp),
    ) { page ->
        val game = featured[page]
        val isRecent = game.id == recent?.id
        Box(
            Modifier
                .fillMaxSize()
                .clip(heroShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.Line, heroShape)
                .combinedClickable(
                    onClick = { onOpen(game) },
                    onLongClick = { onOptions(game) },
                ),
        ) {
            GameArt(game, Modifier.fillMaxSize(), showTag = false)
            // Minimal dark bottom readability scrim so the full box art stays crisp
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.42f to Color.Black.copy(alpha = 0.12f),
                            0.74f to Color.Black.copy(alpha = 0.72f),
                            1f to Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
            )

            // Top-left minimal label & top-right page index
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (isRecent) "CONTINUE" else "FEATURED",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.08.em),
                        color = Color.White,
                    )
                }
                if (featured.size > 1) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "${page + 1} / ${featured.size}",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
                            color = Color.White.copy(alpha = 0.90f),
                        )
                    }
                }
            }

            // Bottom title + high-contrast flat monochrome Play button
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        game.system?.name.orEmpty(),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        game.title,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            lineHeight = 23.sp,
                            letterSpacing = (-0.025).em,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onPlay(game) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = Color(0xFF0A0A0C), modifier = Modifier.size(18.dp))
                    Text(
                        if (isRecent) "Resume" else "Play",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = Color(0xFF0A0A0C),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonoConsolePill(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) Ascend.accent else Tone.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, Tone.Line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
            ),
            color = if (selected) Ascend.onAccent else Tone.Ink,
        )
        if (count != null) {
            Text(
                count.toString(),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = if (selected) Ascend.onAccent.copy(alpha = 0.72f) else Tone.Muted,
            )
        }
    }
}

@Composable
private fun MonoCollectionTile(
    title: String,
    count: Int,
    games: List<Game>,
    onClick: () -> Unit,
) {
    val artShape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .width(124.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artShape)
                .background(Tone.Surface)
                .border(0.75.dp, Tone.Line, artShape),
        ) {
            val covers = games.take(4)
            when {
                covers.size >= 4 -> Column(Modifier.fillMaxSize()) {
                    for (r in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..1) GameArt(covers[r * 2 + c], Modifier.weight(1f).fillMaxHeight(), showTag = false)
                    }
                }
                covers.isNotEmpty() -> GameArt(covers.first(), Modifier.fillMaxSize(), showTag = false)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = Tone.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (count == 1) "1 game" else "$count games",
            style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
            color = Tone.Muted,
        )
    }
}

@Composable
private fun MonoFlatGameRow(
    game: Game,
    detail: String,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpen, onLongClick = onLongClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameArt(
                game,
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.75.dp, Tone.Line, RoundedCornerShape(6.dp)),
                showTag = false,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    game.title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = Tone.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = Tone.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, CircleShape)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = Tone.Ink, modifier = Modifier.size(18.dp))
            }
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(Tone.Line))
        }
    }
}

@Composable
private fun HeroText(label: String, title: String, modifier: Modifier, pills: @Composable () -> Unit) {
    Column(modifier.padding(start = ContentInset, end = ContentInset, bottom = 48.dp)) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(tween(160)) { -it / 3 } + fadeOut(tween(140)))
            },
            label = "heroLabel",
        ) { Text(it, style = LabelStyle, color = LabelColor, maxLines = 1) }
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(tween(160)) { -it / 3 } + fadeOut(tween(140)))
            },
            label = "heroTitle",
        ) { Text(it, style = HeroTitleStyle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.6f)) }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { pills() }
    }
}

/** The light pill: dark glyph and label on white at 64%; squishes under the finger. */
@Composable
private fun HeroPill(onClick: () -> Unit, enabled: Boolean = true, compact: Boolean = false, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squish by animateFloatAsState(if (pressed) 0.92f else 1f, spring(0.5f, Spring.StiffnessMedium), label = "pillSquish")
    Row(
        Modifier
            .graphicsLayer {
                scaleX = squish
                scaleY = squish
            }
            .height(33.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.64f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** A white die showing five; it tumbles while the reel spins and settles on a face. */
@Composable
private fun Die(spinning: Boolean) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) rotation.animateTo(rotation.value + 360f, tween(620, easing = LinearEasing))
        } else {
            val rest = (rotation.value / 90f).roundToInt() * 90f
            rotation.animateTo(rest, spring(dampingRatio = 0.45f, stiffness = 300f))
        }
    }
    Canvas(Modifier.size(21.dp).graphicsLayer { rotationZ = rotation.value }) {
        drawRoundRect(Color.White, cornerRadius = CornerRadius(4.5.dp.toPx()))
        val pip = 2.1.dp.toPx()
        val near = size.width * 0.28f
        val far = size.width * 0.72f
        val mid = size.width / 2f
        listOf(Offset(near, near), Offset(far, near), Offset(mid, mid), Offset(near, far), Offset(far, far))
            .forEach { drawCircle(PipColor, pip, it) }
    }
}

// ---- Rows -----------------------------------------------------------------------------------------

@Composable
private fun SectionHeader(text: String, count: Int, index: Int, onClick: (() -> Unit)? = null) {
    com.retrohero.app.ui.components.SectionTitle(
        text,
        Modifier.padding(start = ContentInset, top = 20.dp, end = ContentInset),
        count = count,
        index = index,
        onClick = onClick,
        verticalPadding = 0.dp,
    )
}

@Composable
internal fun CollectionCard(collection: GameCollection, games: List<Game>, onOpen: (GameCollection) -> Unit) {
    Column(
        Modifier
            .width(RowCardWidth)
            .clip(RoundedCornerShape(Ascend.Radius.Card))
            .background(com.retrohero.app.ui.theme.Tone.Surface)
            .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(Ascend.Radius.Card))
            .press3D()
            .clickable { onOpen(collection) }
            .padding(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(Ascend.Radius.Cover)).background(com.retrohero.app.ui.theme.Tone.Bg)) {
            val covers = games.take(4)
            when {
                covers.size >= 4 -> Column(Modifier.fillMaxSize()) {
                    for (r in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..1) GameArt(covers[r * 2 + c], Modifier.weight(1f).fillMaxHeight(), showTag = false)
                    }
                }
                covers.isNotEmpty() -> GameArt(covers.first(), Modifier.fillMaxSize(), showTag = false)
                else -> Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic, null,
                    tint = com.retrohero.app.ui.theme.Tone.Faint,
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
        }
        Text(collection.name, style = CardTitleStyle, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp, start = 2.dp, end = 2.dp))
        Text(if (games.size == 1) "1 game" else "${games.size} games", style = CardSubtitleStyle, color = com.retrohero.app.ui.theme.Tone.Muted, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun RowCard(game: Game, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit, subtitle: String? = null, onQuickPlay: ((Game) -> Unit)? = null) {
    GameCard(game, { onPlay(game) }, width = RowCardWidth, subtitle = subtitle ?: game.system?.shortName, onLongClick = { onOptions(game) }, onPlay = onQuickPlay?.let { { it(game) } })
}

@Composable
private fun SystemCard(system: GameSystem, count: Int, onOpen: (GameSystem) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, spring(0.5f, Spring.StiffnessMediumLow), label = "sysMorph")
    val morph = rememberMorph(system.polygon, MaterialShapes.Circle)
    Column(
        Modifier
            .width(RowCardWidth)
            .clip(RoundedCornerShape(Ascend.Radius.Card))
            .background(com.retrohero.app.ui.theme.Tone.Surface)
            .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(Ascend.Radius.Card))
            .press3D()
            .clickable(interactionSource = interaction, indication = null) { onOpen(system) }
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Ascend.Radius.Cover))
                .background(com.retrohero.app.ui.theme.Tone.Surface)
                .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(Ascend.Radius.Cover)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(88.dp).graphicsLayer { rotationZ = 14f + 60f * p }.background(com.retrohero.app.ui.theme.Tone.Ink.copy(alpha = 0.08f), MorphShape(morph, p)))
            Text(system.tag, style = TextStyle(fontFamily = PixelFont, fontSize = 15.sp), color = com.retrohero.app.ui.theme.Tone.Ink)
        }
        Text(system.name, style = CardTitleStyle, color = com.retrohero.app.ui.theme.Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(if (count == 1) "1 game" else "$count games", style = CardSubtitleStyle, color = com.retrohero.app.ui.theme.Tone.Muted)
    }
}
