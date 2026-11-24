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
import com.retrohero.app.ui.components.GlassOrb
import com.retrohero.app.ui.components.GlassPillButton
import com.retrohero.app.ui.components.SectionTitle
import com.retrohero.app.ui.components.PageHeader
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.auroraBackdrop
import com.retrohero.app.ui.components.emberBackground
import com.retrohero.app.ui.components.glassPanel
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

/** Midnight Glass & Console Aurora Play screen: dynamic console-tinted aurora glow, cinematic poster carousel, glass console ribbons, and poster shelves. */
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
        games.filter { (stats[it.id]?.seconds ?: 0) >= 60 }.sortedByDescending { stats[it.id]?.seconds ?: 0 }.take(6)
    }
    val counts = remember(games) { games.groupingBy { it.systemId }.eachCount() }
    val systems = remember(counts) {
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }
    val candidates = remember(games, recents) {
        (recents + games.shuffled()).distinctBy { it.id }.take(MaxCandidates)
    }
    var shuffleOpen by rememberSaveable { mutableStateOf(false) }
    if (shuffleOpen) Dialog(onDismissRequest = { shuffleOpen = false }) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
            GachaHero(candidates) { shuffleOpen = false; onPlay(it) }
            IconButton({ shuffleOpen = false }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
        }
    }
    val featured = remember(games, recents, favorites) {
        (recents.take(3) + favorites.take(2) + games.take(4)).distinctBy { it.id }.take(4)
    }
    val pager = rememberPagerState { featured.size.coerceAtLeast(1) }
    val focusedGame = featured.getOrNull(pager.currentPage)
    val auroraPrimary by animateColorAsState(
        focusedGame?.system?.color ?: Ascend.accent,
        tween(550),
        label = "auroraPrimary",
    )
    val auroraSecondary by animateColorAsState(
        lerp(auroraPrimary, Color(0xFF7B5CFF), 0.55f),
        tween(550),
        label = "auroraSecondary",
    )
    val shelfGames = remember(games, recents) {
        (recents + games).distinctBy { it.id }.take(10)
    }

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .auroraBackdrop(auroraPrimary, auroraSecondary),
    ) {
        // Floating Minimal Glass Header
        item(key = "aurora_header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .glassPanel(CircleShape, elevated = true),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            androidx.compose.ui.res.painterResource(com.retrohero.app.R.drawable.ic_logo_mark),
                            null,
                            Modifier.size(18.dp).launchMarkTarget(),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Tone.Ink),
                        )
                    }
                    Column {
                        Text(
                            "RetroHero",
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                letterSpacing = (-0.025).em,
                            ),
                            color = Tone.Ink,
                        )
                        Text(
                            "${games.size} games · ${systems.size} consoles",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                            color = Tone.Muted,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassOrb(
                        icon = Icons.Rounded.Insights,
                        contentDescription = "Play stats",
                        onClick = onOpenStats,
                        size = 38.dp,
                    )
                    GlassPillButton(
                        text = "Shuffle",
                        icon = Icons.Rounded.Shuffle,
                        onClick = {
                            if (candidates.size > 1) shuffleOpen = true else candidates.firstOrNull()?.let(onPlay)
                        },
                        height = 38.dp,
                    )
                    topActions()
                }
            }
        }

        // PS5 / Apple TV Cinematic Cover Carousel
        if (featured.isNotEmpty()) {
            item(key = "cinematic_carousel") {
                CinematicAuroraCarousel(
                    pager = pager,
                    featured = featured,
                    recent = recents.firstOrNull(),
                    onOpen = onOpen,
                    onPlay = onPlay,
                    onShuffle = {
                        if (candidates.size > 1) shuffleOpen = true else candidates.firstOrNull()?.let(onPlay)
                    },
                )
            }
        }

        // Floating Console Worlds Glass Ribbon
        if (systems.isNotEmpty()) {
            item(key = "console_worlds") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Consoles",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.02).em),
                            color = Tone.Ink,
                        )
                        Text(
                            "${systems.size} active",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
                            color = Tone.Muted,
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(systems, key = { "world_${it.id}" }) { sys ->
                            ConsoleGlassChip(sys, counts[sys.id] ?: 0) { onOpenSystem(sys) }
                        }
                    }
                }
            }
        }

        // Continue Playing / Jump Back In Horizontal Poster Shelf
        item(key = "poster_shelf") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (recents.isNotEmpty()) "Continue Playing" else "Your Library",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                    )
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onSeeAllGames)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "See all",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = Ascend.accent,
                        )
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ascend.accent, modifier = Modifier.size(16.dp))
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(shelfGames, key = { "poster_${it.id}" }) { game ->
                        val sub = stats[game.id]?.takeIf { it.seconds >= 60 }?.let { PlayTime.format(it.seconds) }
                            ?: game.system?.name
                        GameCard(
                            game = game,
                            onClick = { onOpen(game) },
                            width = 148.dp,
                            subtitle = sub,
                            onLongClick = { onOptions(game) },
                            onPlay = { onPlay(game) },
                        )
                    }
                }
            }
        }

        // Pinned Favourites & Custom Collections Glass Cards
        if (favorites.isNotEmpty() || collections.isNotEmpty()) {
            item(key = "glass_shelves") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Collections",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (favorites.isNotEmpty()) {
                            item(key = "fav_glass") {
                                GlassCollectionBanner(
                                    title = "Favourites",
                                    subtitle = "${favorites.size} pinned games",
                                    games = favorites,
                                    tint = Ascend.accent,
                                    onClick = { onOpen(favorites.first()) },
                                )
                            }
                        }
                        items(collections, key = { "gcol_${it.first.id}" }) { (col, items) ->
                            GlassCollectionBanner(
                                title = col.name,
                                subtitle = if (items.size == 1) "1 game" else "${items.size} games",
                                games = items,
                                tint = items.firstOrNull()?.system?.color ?: Color(0xFF7B5CFF),
                                onClick = { onOpenCollection(col) },
                            )
                        }
                    }
                }
            }
        }

        // Most Played Glass Row
        if (mostPlayed.isNotEmpty()) {
            item(key = "most_played_glass") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Most Played",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.02).em),
                        color = Tone.Ink,
                    )
                    mostPlayed.take(3).forEach { game ->
                        GlassGameStripRow(
                            game = game,
                            detail = "${game.system?.name.orEmpty()} · ${PlayTime.format(stats[game.id]?.seconds ?: 0)}",
                            onOpen = { onOpen(game) },
                            onPlay = { onPlay(game) },
                            onLongClick = { onOptions(game) },
                        )
                    }
                }
            }
        }

        // Explore All Games Pill CTA
        item(key = "explore_all_cta") {
            com.retrohero.app.ui.components.GlassPillButton(
                text = "Explore all ${games.size} games",
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onSeeAllGames,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                height = 48.dp,
            )
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

// ---- Midnight Glass & Console Aurora Play Components -------------------------------------------

@Composable
private fun CinematicAuroraCarousel(
    pager: PagerState,
    featured: List<Game>,
    recent: Game?,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onShuffle: () -> Unit,
) {
    val cardShape = RoundedCornerShape(26.dp)
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = 20.dp),
        pageSpacing = 12.dp,
        modifier = Modifier.fillMaxWidth().height(312.dp),
    ) { page ->
        val game = featured[page]
        val sysColor = game.system?.color ?: Ascend.accent
        val isRecent = game.id == recent?.id
        val pageOffset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
        val scale = 1f - 0.055f * abs(pageOffset)
        val alpha = 1f - 0.22f * abs(pageOffset)

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(cardShape)
                .background(Color(0xFF0C1120))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.30f),
                            sysColor.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = cardShape,
                )
                .clickable { onOpen(game) },
        ) {
            GameArt(game, Modifier.fillMaxSize(), showTag = false)

            // Atmospheric midnight vignette + console-colored edge aura
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF060912).copy(alpha = 0.30f),
                            0.38f to Color.Transparent,
                            0.68f to Color(0xFF070A14).copy(alpha = 0.80f),
                            1f to Color(0xFF050811).copy(alpha = 0.96f),
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0f to sysColor.copy(alpha = 0.28f),
                            0.75f to Color.Transparent,
                            center = Offset(0f, 900f),
                            radius = 700f,
                        ),
                    ),
            )

            // Top floating glass badges
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0A0F1E).copy(alpha = 0.68f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(sysColor))
                    Text(
                        if (isRecent) "Resume" else "Spotlight",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = Color.White,
                    )
                }

                if (featured.size > 1) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF0A0F1E).copy(alpha = 0.68f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "${page + 1} / ${featured.size}",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.92f),
                        )
                    }
                }
            }

            // Bottom cinematic title + luminous Play Now CTA
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(sysColor.copy(alpha = 0.24f))
                                .border(0.75.dp, sysColor.copy(alpha = 0.55f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                game.system?.shortName ?: game.systemId,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                color = Color.White,
                            )
                        }
                        Text(
                            game.system?.name.orEmpty(),
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        game.title,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            lineHeight = 26.sp,
                            letterSpacing = (-0.025).em,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Ascend.accent, Color(0xFF7B68FF))))
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                            .clickable { onPlay(game) }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            "Play ${game.title}",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isRecent) "Resume Game" else "Play Now",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = Color.White,
                        )
                    }

                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                            .clickable { onOpen(game) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            "Game details",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleGlassChip(system: GameSystem, count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .height(54.dp)
            .glassPanel(shape, tint = system.color)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(system.color.copy(alpha = 0.22f))
                .border(1.dp, system.color.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(system.color))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                system.shortName,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = Tone.Ink,
                maxLines = 1,
            )
            Text(
                if (count == 1) "1 title" else "$count titles",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = Tone.Muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GlassCollectionBanner(
    title: String,
    subtitle: String,
    games: List<Game>,
    tint: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .width(248.dp)
            .height(92.dp)
            .glassPanel(shape, tint = tint, elevated = true)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Tone.Bg)
                .border(1.dp, Color.White.copy(alpha = if (Tone.isDark) 0.16f else 0.55f), RoundedCornerShape(14.dp)),
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                color = Tone.Muted,
                maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GlassGameStripRow(
    game: Game,
    detail: String,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .glassPanel(shape)
            .combinedClickable(onClick = onOpen, onLongClick = onLongClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameArt(
            game,
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, Color.White.copy(alpha = if (Tone.isDark) 0.16f else 0.40f), RoundedCornerShape(13.dp)),
            showTag = false,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                game.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                color = Tone.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Ascend.accent, Color(0xFF7B68FF))))
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, "Play ${game.title}", tint = Color.White, modifier = Modifier.size(19.dp))
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
