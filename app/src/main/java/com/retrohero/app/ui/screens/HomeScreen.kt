package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.emberBackground
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.R
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameCollection
import androidx.compose.ui.text.style.TextAlign
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
import com.retrohero.app.ui.components.launchMarkTarget
import com.retrohero.app.ui.components.launchRise
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.components.rememberPressFeedback
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.Salmon
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

/** Side margin that suits the screen: tighter on phones, roomier on large screens. */
private val ContentInset: Dp
    @Composable @ReadOnlyComposable get() {
        val w = LocalConfiguration.current.screenWidthDp
        return when {
            w < 380 -> 18.dp
            w < 600 -> 22.dp
            else -> 36.dp
        }
    }

/** Row cards show about two and a half per screen, hinting that the row scrolls. */
private val RowCardWidth: Dp
    @Composable @ReadOnlyComposable get() {
        val w = LocalConfiguration.current.screenWidthDp.dp
        return ((w - ContentInset * 2 - CardSpacing * 2) / 2.45f).coerceIn(112.dp, 168.dp)
    }

/** The gacha scales with the screen height so short phones still see the rows below it. */
private val HeroHeight: Dp
    @Composable @ReadOnlyComposable get() =
        (LocalConfiguration.current.screenHeightDp * 0.6f).coerceIn(430f, GachaHeight).dp

private val CardSpacing = 12.dp
private val Tracking = (-0.04).em

private val LabelStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = Tracking)
private val TitleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = Tracking)
private val HeroTitleStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = Tracking)
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
private const val CoverCorner = 25f
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

/**
 * Home: a gacha hero (roll the reel, it lands on a random game), then rows of recently played
 * games and systems, then the whole library.
 */
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
    onOpenStats: () -> Unit = {},
) {
    val mostPlayed = remember(games, stats) {
        games.filter { (stats[it.id]?.seconds ?: 0) >= 60 }.sortedByDescending { stats[it.id]?.seconds ?: 0 }.take(10)
    }
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) { Systems.all.filter { (counts[it.id] ?: 0) > 0 } }
    // The reel: recent games first, then a shuffle of the rest.
    val candidates = remember(games) {
        (recents + games.shuffled()).distinctBy { it.id }.take(MaxCandidates)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        contentPadding = PaddingValues(start = ContentInset, end = ContentInset, bottom = bottomPadding + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(CardSpacing),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().emberBackground(),
    ) {
        full { HeroCarousel(candidates, games, recents, favorites, onPlay, onOpen, topActions, stats, onOpenStats) }

        if (recents.isNotEmpty()) {
            full { Box(Modifier.launchRise(2)) { SectionHeader("Recently Played") } }
            full {
                LazyRow(modifier = Modifier.launchRise(3), contentPadding = PaddingValues(horizontal = ContentInset), horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
                    items(recents, key = { "r_" + it.id }) { game -> RowCard(game, onOpen, onOptions) }
                }
            }
        }
        if (mostPlayed.size >= 2) {
            full { SectionHeader("Most Played") }
            full {
                LazyRow(contentPadding = PaddingValues(horizontal = ContentInset), horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
                    items(mostPlayed, key = { "m_" + it.id }) { game ->
                        RowCard(game, onOpen, onOptions, subtitle = PlayTime.format(stats[game.id]?.seconds ?: 0))
                    }
                }
            }
        }
        if (collections.isNotEmpty()) {
            full { SectionHeader("Your Collections") }
            full {
                LazyRow(contentPadding = PaddingValues(horizontal = ContentInset), horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
                    items(collections, key = { "c_" + it.first.id }) { (c, list) -> CollectionCard(c, list, onOpenCollection) }
                }
            }
        }
        if (systems.size > 1) {
            full { SectionHeader("Your Systems") }
            full {
                LazyRow(contentPadding = PaddingValues(horizontal = ContentInset), horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
                    items(systems, key = { "s_" + it.id }) { system -> SystemCard(system, counts[system.id] ?: 0, onOpenSystem) }
                }
            }
        }
        full { SectionHeader("All Games", onSeeAllGames) }
        items(games, key = { it.id }) { game ->
            GameCard(game, onClick = { onOpen(game) }, onLongClick = { onOptions(game) }, modifier = Modifier.animateItem())
        }
    }
}

/** Full-width row that bleeds through the grid's side padding to the screen edges. */
private fun LazyGridScope.full(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.bleed(ContentInset)) { content() } }

private fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = (horizontal * 2).roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
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
        val coverShape = RoundedCornerShape(unit * CoverCorner)

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
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
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
                Phase.Idle -> "Gacha here!"
                Phase.Rolling -> "Rolling…"
                Phase.Landed -> "You got"
            },
            title = state.result?.title ?: "Random plays",
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

// ---- Hero carousel: Gacha / Speed dial / Because you played -----------------------------------

/**
 * The top of Home, as a swipeable set of pages: the gacha, a speed dial of favourites, and picks
 * based on the last game played. The logo row stays put while the pages move under it.
 */
@Composable
private fun HeroCarousel(
    candidates: List<Game>,
    games: List<Game>,
    recents: List<Game>,
    favorites: List<Game>,
    onPlay: (Game) -> Unit,
    onOpen: (Game) -> Unit,
    topActions: @Composable () -> Unit,
    stats: Map<String, PlayStat> = emptyMap(),
    onOpenStats: () -> Unit = {},
) {
    val last = recents.firstOrNull()
    val totalSeconds = stats.values.sumOf { it.seconds }
    val picks = remember(games, recents) {
        if (last == null) emptyList()
        else {
            val played = recents.mapTo(HashSet()) { it.id }
            val same = games.filter { it.systemId == last.systemId && it.id !in played }
            (same + games.filter { it.id !in played && it.systemId != last.systemId }).take(3)
        }
    }
    val pages = buildList {
        add(0)
        add(1)
        if (last != null && picks.isNotEmpty()) add(2)
        if (totalSeconds >= 60) add(3)
    }
    val pager = rememberPagerState { pages.size }
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
            // Pages ease in from the side, like a card being dealt.
            val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    alpha = 1f - (abs(offset) * 0.6f).coerceAtMost(0.6f)
                    // Neighbours trail behind (pushed outward), never pulled into view.
                    translationX = -offset * size.width * 0.25f
                },
            ) {
                when (pages[index]) {
                    0 -> GachaHero(candidates, onPlay)
                    1 -> SpeedDialPage(favorites, onPlay, onOpen)
                    2 -> BecausePage(last!!, picks, onPlay, onOpen)
                    else -> StatsPage(games, stats, onOpen, onOpenStats)
                }
            }
        }
        Row(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(start = ContentInset, end = ContentInset - 4.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Glyph (keeps its own colours) and wordmark.
            androidx.compose.foundation.Image(
                painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(width = 38.dp, height = 26.dp).launchMarkTarget(),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "RetroHero",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.03).em),
                color = Color.White,
                modifier = Modifier.launchRise(0),
            )
            Spacer(Modifier.weight(1f))
            topActions()
        }

        // Page dots
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = ContentInset, bottom = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(pages.size) { i ->
                val active = pager.currentPage == i
                val w by androidx.compose.animation.core.animateDpAsState(if (active) 18.dp else 6.dp, spring(dampingRatio = 0.7f), label = "heroDot")
                Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(Color.White.copy(alpha = if (active) 0.9f else 0.35f)))
            }
        }
    }
}

/** A soft colour wash used behind the non-gacha pages. */
@Composable
private fun PageWash(base: Color) {
    val light = lerp(base, Color.White, 0.45f)
    val deep = lerp(lerp(base, Color(0xFF7C82B4), 0.3f), Color.Black, 0.35f)
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(deep, lerp(deep, light, 0.5f), light), start = Offset(0f, Float.POSITIVE_INFINITY), end = Offset(Float.POSITIVE_INFINITY, 0f)),
        ),
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.85f), 0.22f to Color.Black.copy(alpha = 0.3f), 0.44f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black),
        ),
    )
}

@Composable
private fun SpeedDialPage(favorites: List<Game>, onPlay: (Game) -> Unit, onOpen: (Game) -> Unit) {
    PageWash(favorites.firstOrNull()?.system?.color ?: Color(0xFF6E6A86))
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(top = 72.dp)) {
        val tiles = favorites.take(6)
        Column(Modifier.padding(horizontal = ContentInset), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0..2) {
                    val game = tiles.getOrNull(row * 3 + col)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (game != null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .press3D()
                                    .clip(RoundedCornerShape(22.dp))
                                    .combinedClickable(onClick = { onPlay(game) }, onLongClick = { onOpen(game) }),
                            ) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
                        } else {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.07f)))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(start = ContentInset, end = ContentInset, bottom = 48.dp)) {
            Text("Your favourites", style = LabelStyle, color = LabelColor)
            Text("Speed dial", style = HeroTitleStyle, color = Color.White)
            Text(
                if (favorites.isEmpty()) "Swipe a game left in a system's list, or long-press it, to pin it here."
                else "Tap to play, hold for details.",
                style = CardSubtitleStyle,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.7f),
            )
        }
    }
}

@Composable
private fun BecausePage(last: Game, picks: List<Game>, onPlay: (Game) -> Unit, onOpen: (Game) -> Unit) {
    PageWash(last.system?.color ?: Color(0xFF6E6A86))
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val card = (maxWidth * 0.42f).coerceAtMost(180.dp)
        // A fanned hand of covers on the right.
        picks.take(3).forEachIndexed { i, game ->
            val angle = (i - 1) * 9f
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (ContentInset + card * (0.42f * (2 - i))) * -1f, y = if (i == 1) 96.dp else 110.dp)
                    .size(card, card * 1.3f)
                    .graphicsLayer { rotationZ = angle }
                    .press3D()
                    .shadow(18.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .combinedClickable(onClick = { onOpen(game) }, onLongClick = { onOpen(game) }),
            ) { GameArt(game, Modifier.fillMaxSize(), showTitle = true, showTag = false) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = ContentInset, end = ContentInset, bottom = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Because you played", style = LabelStyle, color = LabelColor)
                Spacer(Modifier.width(6.dp))
                GameArt(last, Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)), showTag = false)
                Spacer(Modifier.width(6.dp))
                Text(last.title, style = LabelStyle, color = Color.White.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (picks.all { it.systemId == last.systemId }) "More ${last.system?.shortName} games" else "Try something new",
                style = HeroTitleStyle,
                color = Color.White,
            )
            Spacer(Modifier.height(11.dp))
            HeroPill(onClick = { picks.firstOrNull()?.let(onPlay) }) {
                Icon(Icons.Rounded.PlayArrow, null, tint = PipColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text("play", style = LabelStyle, color = PipColor)
            }
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
private fun SectionHeader(text: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .padding(start = ContentInset - 4.dp, top = 14.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = TitleStyle, color = LabelColor)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = LabelColor, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun StatsPage(games: List<Game>, stats: Map<String, PlayStat>, onOpen: (Game) -> Unit, onOpenStats: () -> Unit) {
    val ranked = remember(games, stats) {
        games.filter { (stats[it.id]?.seconds ?: 0) > 0 }.sortedByDescending { stats[it.id]!!.seconds }
    }
    val total = stats.values.sumOf { it.seconds }
    PageWash(ranked.firstOrNull()?.system?.color ?: Color(0xFF6E6A86))
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(top = 76.dp, start = ContentInset, end = ContentInset)) {
        ranked.take(3).forEachIndexed { i, game ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .press3D()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable { onOpen(game) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}", style = TitleStyle, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                GameArt(game, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), showTag = false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(game.title, style = CardTitleStyle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(PlayTime.format(stats[game.id]?.seconds ?: 0), style = CardSubtitleStyle, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Your play time  ›", style = LabelStyle, color = LabelColor, modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenStats))
        Text(PlayTime.format(total).replaceFirstChar { it.uppercase() }, style = HeroTitleStyle, color = Color.White)
        Text(
            if (ranked.size == 1) "on 1 game" else "across ${ranked.size} games",
            style = CardSubtitleStyle,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 58.dp),
        )
    }
}

@Composable
internal fun CollectionCard(collection: GameCollection, games: List<Game>, onOpen: (GameCollection) -> Unit) {
    Column(Modifier.width(RowCardWidth).press3D().clickable { onOpen(collection) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF1C1C1F))) {
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
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
        }
        Text(collection.name, style = CardTitleStyle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(if (games.size == 1) "1 game" else "${games.size} games", style = CardSubtitleStyle, color = SubtleColor)
    }
}

@Composable
private fun RowCard(game: Game, onPlay: (Game) -> Unit, onOptions: (Game) -> Unit, subtitle: String? = null) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressFeedback(interaction, restCorner = 28.dp, pressedCorner = 40.dp, pressedScale = 0.95f)
    Column(
        Modifier
            .width(RowCardWidth)
            .press3D()
            .combinedClickable(interactionSource = interaction, indication = null, onClick = { onPlay(game) }, onLongClick = { onOptions(game) }),
    ) {
        GameArt(game, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(press.corner.value)))
        Text(game.title, style = CardTitleStyle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(subtitle ?: game.system?.name.orEmpty(), style = CardSubtitleStyle, color = SubtleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SystemCard(system: GameSystem, count: Int, onOpen: (GameSystem) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, spring(0.5f, Spring.StiffnessMediumLow), label = "sysMorph")
    val morph = rememberMorph(system.polygon, MaterialShapes.Circle)
    Column(Modifier.width(RowCardWidth).press3D().clickable(interactionSource = interaction, indication = null) { onOpen(system) }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(lerp(system.color, Color.White, 0.15f), lerp(system.color, Color.Black, 0.55f)))),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(88.dp).graphicsLayer { rotationZ = 14f + 60f * p }.background(Color.White.copy(alpha = 0.2f), MorphShape(morph, p)))
            Text(system.tag, style = TextStyle(fontFamily = PixelFont, fontSize = 15.sp), color = Color.White)
        }
        Text(system.name, style = CardTitleStyle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(if (count == 1) "1 game" else "$count games", style = CardSubtitleStyle, color = SubtleColor)
    }
}
