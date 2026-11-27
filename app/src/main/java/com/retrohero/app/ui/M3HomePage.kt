package com.retrohero.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameDossier
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalReducedMotion
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3CircularWavyGauge
import com.retrohero.app.ui.components.M3ConsoleImage
import com.retrohero.app.ui.components.M3ExpressiveSpring
import com.retrohero.app.ui.components.M3GameCover
import com.retrohero.app.ui.components.M3PhysicalCaseShowcase
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.M3SplitButton
import com.retrohero.app.ui.components.M3WavyBar
import com.retrohero.app.ui.components.MorphShape
import com.retrohero.app.ui.components.m3StaggeredEntrance
import com.retrohero.app.ui.components.rememberMorph
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.polygon
import com.retrohero.app.ui.theme.tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun M3HomePage(
    games: List<Game>,
    recents: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    shelfStatuses: Map<String, String>,
    scanning: Boolean,
    scanProgress: String,
    missingCoresCount: Int,
    installingCores: Boolean,
    onInstallMissingCores: () -> Unit,
    onPlay: (Game) -> Unit,
    onOpenDetail: (Game) -> Unit,
    onSelectConsole: (String?) -> Unit,
    onShufflePlay: () -> Unit,
    onToggleTheme: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
    dossierFor: (Game) -> GameDossier? = { null },
    loadDossier: (suspend (Game) -> GameDossier)? = null,
    showCasesEverywhere: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    val featuredDeck = remember(recents, games) {
        (recents + games).distinctBy { it.id }.take(5)
    }
    val jumpBack = remember(recents, games, featuredDeck) {
        val heroId = featuredDeck.firstOrNull()?.id
        val base = if (recents.size > 1) recents.drop(1) else games.filter { it.id != heroId }
        base.take(8)
    }
    val favorites = remember(games, favoriteIds) { games.filter { it.id in favoriteIds } }
    val activeSystems = remember(games) {
        val counts = games.groupingBy { it.systemId }.eachCount()
        Systems.all.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
    }
    val totalSeconds = remember(stats) { stats.values.sumOf { it.seconds } }
    val clearedCount = remember(games, shelfStatuses) { games.count { shelfStatuses[it.id] == "completed" } }
    val clearedFrac = if (games.isNotEmpty()) clearedCount.toFloat() / games.size else 0f

    LazyColumn(
        contentPadding = PaddingValues(top = 14.dp, bottom = inset + 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. Editorial Material 3 Expressive Header + Morphing Sunny<->Cookie4Sided Theme Control
        item(key = "m3_home_header") {
            val view = LocalView.current
            val themeMorph = rememberMorph(MaterialShapes.Sunny, MaterialShapes.Cookie4Sided)
            val themeProgress by animateFloatAsState(
                if (Tone.isDark) 0f else 1f,
                spring(dampingRatio = 0.54f, stiffness = 360f),
                label = "themeMorph",
            )
            val themeSpin by animateFloatAsState(
                if (Tone.isDark) 0f else 180f,
                spring(dampingRatio = 0.58f, stiffness = 320f),
                label = "themeSpin",
            )
            val themeShape = MorphShape(themeMorph, themeProgress.coerceIn(0f, 1f))

            Row(
                Modifier
                    .fillMaxWidth()
                    .m3StaggeredEntrance(0, slideDp = 12.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(cs.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (scanning) "Scanning…" else "${games.size} titles",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onPrimaryContainer,
                            )
                        }
                        if (activeSystems.isNotEmpty()) {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(cs.tertiaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "${activeSystems.size} consoles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onTertiaryContainer,
                                )
                            }
                        }
                    }
                    Text(
                        text = "RetroHero",
                        style = MaterialTheme.typography.displayMedium,
                        color = cs.onBackground,
                    )
                }

                Box(
                    Modifier
                        .size(50.dp)
                        .graphicsLayer { rotationZ = themeSpin }
                        .clip(themeShape)
                        .background(cs.secondaryContainer)
                        .semantics { contentDescription = "Toggle theme" }
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onToggleTheme()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = Tone.isDark,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = 0.55f, stiffness = 460f), 0.5f) + fadeIn(tween(150))) togetherWith
                                (scaleOut(tween(120), 0.5f) + fadeOut(tween(100)))
                        },
                        label = "themeIconSwap",
                    ) { dark ->
                        Icon(
                            imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = cs.onSecondaryContainer,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer { rotationZ = -themeSpin },
                        )
                    }
                }
            }
        }

        if (scanning) {
            item(key = "m3_home_scanning") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(cs.secondaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = scanProgress.ifBlank { "Scanning ROM folders…" },
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (games.isNotEmpty() && missingCoresCount > 0) {
            item(key = "m3_home_cores_ready") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(cs.secondaryContainer)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    M3ShapeBadge(polygon = MaterialShapes.Sunny, containerColor = cs.primary, size = 36.dp) {
                        Icon(Icons.Rounded.DownloadForOffline, null, tint = cs.onPrimary, modifier = Modifier.size(19.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "$missingCoresCount ${if (missingCoresCount == 1) "core" else "cores"} needed",
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSecondaryContainer,
                        )
                        Text(
                            text = "One-tap setup for instant launch",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSecondaryContainer.copy(alpha = 0.78f),
                            maxLines = 1,
                        )
                    }
                    Row(
                        Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(cs.primary)
                            .clickable(enabled = !installingCores, onClick = onInstallMissingCores)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (installingCores) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                            Text("Installing…", style = MaterialTheme.typography.labelMedium, color = cs.onPrimary)
                        } else {
                            Text("Install all", style = MaterialTheme.typography.labelMedium, color = cs.onPrimary)
                        }
                    }
                }
            }
        }

        if (featuredDeck.isEmpty()) {
            item(key = "m3_home_empty") {
                M3EmptyStateCard(
                    scanning = scanning,
                    onAddFolder = onAddFolder,
                    onOpenHomebrew = onOpenHomebrew,
                    modifier = Modifier.m3StaggeredEntrance(1).padding(horizontal = 20.dp),
                )
            }
        } else {
            // 2. Kinetic Swipeable Parallax Hero Deck (HorizontalPager + Sculpted Scoop Artwork + Pill Indicator)
            item(key = "m3_home_pager") {
                val pagerState = rememberPagerState(pageCount = { featuredDeck.size })
                Column(
                    Modifier.m3StaggeredEntrance(1),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        val game = featuredDeck[page]
                        val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                        val pageScale = 1f - abs(offset) * 0.06f
                        val pageTilt = offset * -2.6f
                        val pageAlpha = 1f - abs(offset) * 0.22f

                        M3SculpturalHeroCard(
                            game = game,
                            stat = stats[game.id],
                            isRecent = page == 0 && recents.isNotEmpty(),
                            isFavorite = game.id in favoriteIds,
                            completionProgress = clearedFrac.coerceIn(0.24f, 1f),
                            onPlay = { onPlay(game) },
                            onDetails = { onOpenDetail(game) },
                            dossierFor = dossierFor,
                            loadDossier = loadDossier,
                            show3dCase = showCasesEverywhere,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pageScale
                                scaleY = pageScale
                                rotationZ = pageTilt
                                alpha = pageAlpha
                            },
                        )
                    }

                    if (featuredDeck.size > 1) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(featuredDeck.size) { idx ->
                                val active = idx == pagerState.currentPage
                                val w by animateDpAsState(
                                    if (active) 28.dp else 7.dp,
                                    spring(dampingRatio = 0.62f, stiffness = 420f),
                                    label = "pagerDotW",
                                )
                                val col by animateColorAsState(
                                    if (active) cs.primary else cs.outlineVariant,
                                    tween(200),
                                    label = "pagerDotCol",
                                )
                                Box(
                                    Modifier
                                        .width(w.coerceAtLeast(6.dp))
                                        .height(7.dp)
                                        .clip(CircleShape)
                                        .background(col),
                                )
                            }
                        }
                    }
                }
            }

            // 3. Avant-Garde Asymmetric Sculptural Bento Mosaic (Giant Arch Circular Wavy Gauge + Stacked Kinetic Pair!)
            item(key = "m3_home_mosaic") {
                M3AsymmetricSculpturalMosaic(
                    totalSeconds = totalSeconds,
                    clearedCount = clearedCount,
                    totalGames = games.size,
                    activeSystems = activeSystems,
                    onShufflePlay = onShufflePlay,
                    onOpenConsoles = { onSelectConsole(null) },
                    modifier = Modifier.m3StaggeredEntrance(2).padding(horizontal = 20.dp),
                )
            }

            // 4. Sculpted Console Hardware Medallion Ribbon (unique MaterialShapes per console!)
            if (activeSystems.isNotEmpty()) {
                item(key = "m3_home_consoles") {
                    Column(Modifier.m3StaggeredEntrance(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        M3ShelfHeader(
                            title = "Consoles",
                            badgeText = "${activeSystems.size}",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(activeSystems, key = { _, sys -> "orb_${sys.id}" }) { idx, sys ->
                                M3ConsoleMedallion(
                                    system = sys,
                                    toneIndex = idx,
                                    onClick = { onSelectConsole(sys.id) },
                                )
                            }
                        }
                    }
                }
            }

            // 5. Expressive Wavy Divider
            item(key = "m3_home_wave") {
                M3WavyBar(
                    color = cs.outlineVariant.copy(alpha = 0.65f),
                    amplitude = 2.5.dp,
                    wavelength = 24.dp,
                    strokeWidth = 2.dp,
                    modifier = Modifier.m3StaggeredEntrance(3).padding(horizontal = 20.dp),
                )
            }

            // 6. Staggered Editorial "Jump back in" Showcase
            if (jumpBack.isNotEmpty()) {
                item(key = "m3_home_continue") {
                    Column(Modifier.m3StaggeredEntrance(4)) {
                        M3ShelfHeader(
                            title = "Jump back in",
                            badgeText = "${jumpBack.size}",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(jumpBack, key = { _, g -> "m3_jump_${g.id}" }) { idx, game ->
                                val sec = stats[game.id]?.seconds ?: 0
                                M3ExpressiveGameCard(
                                    game = game,
                                    width = 152.dp,
                                    isFavorite = game.id in favoriteIds,
                                    playTimeLabel = if (sec > 0) PlayTime.format(sec) else null,
                                    silhouetteVariant = idx % 3,
                                    onOpen = { onOpenDetail(game) },
                                    onPlay = { onPlay(game) },
                                    dossierFor = dossierFor,
                                    loadDossier = loadDossier,
                                    show3dCase = showCasesEverywhere,
                                )
                            }
                        }
                    }
                }
            }

            // 7. Favorites Showcase
            if (favorites.isNotEmpty()) {
                item(key = "m3_home_favs") {
                    Column(Modifier.m3StaggeredEntrance(5)) {
                        M3ShelfHeader(
                            title = "Favorites",
                            badgeText = "${favorites.size}",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(favorites, key = { _, g -> "m3_fav_${g.id}" }) { idx, game ->
                                val sec = stats[game.id]?.seconds ?: 0
                                M3ExpressiveGameCard(
                                    game = game,
                                    width = 136.dp,
                                    isFavorite = true,
                                    playTimeLabel = if (sec > 0) PlayTime.format(sec) else null,
                                    silhouetteVariant = (idx + 1) % 3,
                                    onOpen = { onOpenDetail(game) },
                                    onPlay = { onPlay(game) },
                                    dossierFor = dossierFor,
                                    loadDossier = loadDossier,
                                    show3dCase = showCasesEverywhere,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3HeroLandingPhoto(
    game: Game,
    dossierFor: (Game) -> GameDossier?,
    loadDossier: (suspend (Game) -> GameDossier)?,
    modifier: Modifier = Modifier,
) {
    var heroUrl by remember(game.id) { mutableStateOf<String?>(null) }
    var heroFailed by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(game.id) {
        val cached = withContext(Dispatchers.IO) { dossierFor(game) }
        val initialUrl = cached?.heroImageUrl?.takeIf { it.isNotBlank() }
        if (initialUrl != null) {
            heroUrl = initialUrl
        } else if (cached == null && loadDossier != null) {
            val loaded = runCatching { loadDossier(game) }.getOrNull()
            heroUrl = loaded?.heroImageUrl?.takeIf { it.isNotBlank() }
        }
    }

    Box(modifier) {
        // Immediate full-bleed cover/key-art photo with zero wait
        M3GameCover(
            game = game,
            showTitle = false,
            modifier = Modifier.fillMaxSize(),
        )
        // Upgrade smoothly to wide 16:9 LaunchBox hero/fanart landing photo when available
        val wideUrl = heroUrl
        if (wideUrl != null && !heroFailed) {
            AsyncImage(
                model = wideUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                onError = { heroFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Subtle cinematic vignette scrim so top & bottom pills stay crisp over bright artwork
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.24f),
                        0.32f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.34f),
                    ),
                ),
        )
    }
}

@Composable
private fun M3SculpturalHeroCard(
    game: Game,
    stat: PlayStat?,
    isRecent: Boolean,
    isFavorite: Boolean,
    completionProgress: Float,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    dossierFor: (Game) -> GameDossier? = { null },
    loadDossier: (suspend (Game) -> GameDossier)? = null,
    show3dCase: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val outerShape = RoundedCornerShape(34.dp)
    // Dramatic asymmetric sculptural scoop window — unmistakable Material 3 Expressive silhouette!
    val artShape = RoundedCornerShape(topStart = 26.dp, topEnd = 84.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    Column(
        modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(cs.surfaceContainerHigh)
            .clickable(onClick = onDetails)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(172.dp),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(artShape),
            ) {
                if (show3dCase) {
                    M3PhysicalCaseShowcase(
                        game = game,
                        dossierFor = dossierFor,
                        loadDossier = loadDossier,
                        yawDeg = 22f,
                        pitchDeg = -6f,
                        stagePaddingDp = 10.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    M3HeroLandingPhoto(
                        game = game,
                        dossierFor = dossierFor,
                        loadDossier = loadDossier,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(cs.tertiaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.SportsEsports, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (isRecent) "CONTINUE PLAYING" else "FEATURED",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onTertiaryContainer,
                    )
                }

                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = (game.system?.shortName ?: game.systemId).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSecondaryContainer,
                    )
                }
            }

            // Sculpted Heart medallion perched in the negative space of the top-right arch scoop!
            if (isFavorite) {
                M3ShapeBadge(
                    polygon = MaterialShapes.Heart,
                    containerColor = cs.tertiaryContainer,
                    size = 40.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildString {
                    append(game.system?.name ?: game.systemId.uppercase())
                    if (stat != null && stat.seconds > 0) {
                        append(" · ")
                        append(PlayTime.format(stat.seconds))
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            M3WavyBar(
                color = cs.surfaceContainerHighest,
                progressColor = cs.primary,
                progress = completionProgress,
                amplitude = 2.8.dp,
                wavelength = 18.dp,
                strokeWidth = 3.2.dp,
            )

            M3SplitButton(
                label = if (isRecent) "Resume" else "Play now",
                icon = Icons.Rounded.PlayArrow,
                onPrimaryClick = onPlay,
                trailingIcon = Icons.Rounded.Info,
                trailingDescription = "Details for ${game.title}",
                onTrailingClick = onDetails,
                height = 50.dp,
            )
        }
    }
}

/**
 * Avant-garde Asymmetric Sculptural Bento Mosaic:
 * Left: Architectural Arch card with Circular Wavy Orbital Ring Gauge + giant Playtime readout.
 * Right: Full-pill Lucky Spin dice capsule + Console Vault tonal card with overlapping system polygons.
 */
@Composable
private fun M3AsymmetricSculpturalMosaic(
    totalSeconds: Long,
    clearedCount: Int,
    totalGames: Int,
    activeSystems: List<GameSystem>,
    onShufflePlay: () -> Unit,
    onOpenConsoles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val pct = if (totalGames > 0) ((clearedCount * 100f) / totalGames).roundToInt() else 0
    val frac = if (totalGames > 0) clearedCount.toFloat() / totalGames else 0f

    val archShape = RoundedCornerShape(topStart = 96.dp, topEnd = 96.dp, bottomStart = 28.dp, bottomEnd = 28.dp)

    // Lucky spin interaction
    val spinInt = remember { MutableInteractionSource() }
    val spinPressed by spinInt.collectIsPressedAsState()
    val spinScale by animateFloatAsState(if (spinPressed) 0.93f else 1f, M3ExpressiveSpring, label = "luckyScale")
    var diceRotTarget by remember { mutableFloatStateOf(0f) }
    val diceRot by animateFloatAsState(diceRotTarget, spring(dampingRatio = 0.52f, stiffness = 280f), label = "luckyDice")

    Row(
        modifier.fillMaxWidth().height(190.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left Architectural Arch Card: Circular Wavy Progress Ring + Playtime
        Column(
            Modifier
                .weight(0.96f)
                .fillMaxHeight()
                .clip(archShape)
                .background(cs.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            M3CircularWavyGauge(
                progress = frac.coerceIn(0.14f, 1f),
                activeColor = cs.primary,
                trackColor = cs.onPrimaryContainer.copy(alpha = 0.16f),
                strokeWidth = 4.5.dp,
                waveAmplitude = 2.dp,
                lobes = 10,
                modifier = Modifier.size(90.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        text = "$clearedCount/$totalGames",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = cs.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (totalSeconds > 0) PlayTime.format(totalSeconds) else "0m",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 19.sp),
                    color = cs.onPrimaryContainer,
                    maxLines = 1,
                )
                Text(
                    text = "PLAY JOURNAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = cs.onPrimaryContainer.copy(alpha = 0.74f),
                    maxLines = 1,
                )
            }
        }

        // Right Stacked Kinetic Pair
        Column(
            Modifier.weight(1.04f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Full-Pill Lucky Spin Launcher
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { scaleX = spinScale; scaleY = spinScale }
                    .clip(CircleShape)
                    .background(cs.tertiaryContainer)
                    .semantics { contentDescription = "Surprise me" }
                    .clickable(interactionSource = spinInt, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        diceRotTarget += 180f
                        onShufflePlay()
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                M3ShapeBadge(
                    polygon = MaterialShapes.Sunny,
                    containerColor = cs.tertiary,
                    size = 50.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = diceRot },
                ) {
                    Icon(
                        Icons.Rounded.Casino,
                        null,
                        tint = cs.onTertiary,
                        modifier = Modifier.size(23.dp).graphicsLayer { rotationZ = -diceRot },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = cs.onTertiaryContainer,
                        maxLines = 1,
                    )
                    Text(
                        text = "Lucky pick",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onTertiaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                    )
                }
            }

            // Bottom Sculpted Consoles Preview Card
            val vaultInt = remember { MutableInteractionSource() }
            val vaultPressed by vaultInt.collectIsPressedAsState()
            val vaultScale by animateFloatAsState(if (vaultPressed) 0.94f else 1f, M3ExpressiveSpring, label = "vaultScale")
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { scaleX = vaultScale; scaleY = vaultScale }
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.secondaryContainer)
                    .clickable(interactionSource = vaultInt, indication = null) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onOpenConsoles()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${activeSystems.size} consoles",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onSecondaryContainer,
                    )
                    Text(
                        text = "All →",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSecondaryContainer.copy(alpha = 0.72f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeSystems.take(4).forEach { sys ->
                        M3ShapeBadge(
                            polygon = sys.polygon,
                            containerColor = cs.onSecondaryContainer.copy(alpha = 0.14f),
                            size = 32.dp,
                        ) {
                            Text(
                                text = sys.tag.take(4),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (sys.tag.length >= 4) 6.5.sp else 8.sp,
                                ),
                                color = cs.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3ConsoleMedallion(
    system: GameSystem,
    toneIndex: Int,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, M3ExpressiveSpring, label = "orbScale")
    val twist by animateFloatAsState(if (pressed) -14f else 0f, M3ExpressiveSpring, label = "orbTwist")

    val (bg, fg) = when (toneIndex % 3) {
        0 -> cs.primaryContainer to cs.onPrimaryContainer
        1 -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.tertiaryContainer to cs.onTertiaryContainer
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(78.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            },
    ) {
        M3ShapeBadge(
            polygon = system.polygon,
            containerColor = bg,
            size = 70.dp,
            modifier = Modifier.graphicsLayer { rotationZ = twist },
        ) {
            M3ConsoleImage(
                system = system,
                tint = fg,
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer { rotationZ = -twist },
            )
        }
        Text(
            text = system.shortName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun M3ShelfHeader(
    title: String,
    badgeText: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = cs.onBackground,
        )
        Box(
            Modifier
                .clip(CircleShape)
                .background(cs.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(text = badgeText, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
internal fun M3ExpressiveGameCard(
    game: Game,
    width: Dp?,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    playTimeLabel: String? = null,
    silhouetteVariant: Int = 0,
    dossierFor: (Game) -> GameDossier? = { null },
    loadDossier: (suspend (Game) -> GameDossier)? = null,
    show3dCase: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, M3ExpressiveSpring, label = "m3CardScale")

    // Alternating sculptural M3 Expressive card silhouettes (Arch-top, Asymmetric Scoop, Soft Squircle)
    val cardShape = when (silhouetteVariant % 3) {
        1 -> RoundedCornerShape(topStart = 26.dp, topEnd = 46.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
        2 -> RoundedCornerShape(topStart = 46.dp, topEnd = 26.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
        else -> RoundedCornerShape(28.dp)
    }
    val artShape = when (silhouetteVariant % 3) {
        1 -> RoundedCornerShape(topStart = 20.dp, topEnd = 38.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        2 -> RoundedCornerShape(topStart = 38.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        else -> RoundedCornerShape(22.dp)
    }

    Column(
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(cardShape)
            .background(cs.surfaceContainerHigh)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onPlay()
                },
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.88f)
                .clip(artShape),
        ) {
            if (show3dCase) {
                M3PhysicalCaseShowcase(
                    game = game,
                    dossierFor = dossierFor,
                    loadDossier = loadDossier,
                    yawDeg = 24f,
                    pitchDeg = -6f,
                    stagePaddingDp = 8.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                M3GameCover(
                    game = game,
                    showTitle = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (playTimeLabel != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = playTimeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = cs.onSurface,
                        maxLines = 1,
                    )
                }
            }

            if (isFavorite) {
                M3ShapeBadge(
                    polygon = MaterialShapes.Heart,
                    containerColor = cs.tertiaryContainer,
                    size = 28.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(13.dp))
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp, lineHeight = 16.sp),
                    color = cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = game.system?.shortName ?: game.systemId.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${game.title}", tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun M3EmptyStateCard(
    scanning: Boolean,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val reduced = LocalReducedMotion.current
    val spin by rememberInfiniteTransition(label = "m3EmptySpin").animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "m3EmptyAngle",
    )
    val shape = RoundedCornerShape(34.dp)
    val flowerShape = MaterialShapes.SoftBurst.toShape()

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainerHigh)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(92.dp)
                    .graphicsLayer { rotationZ = spin }
                    .clip(flowerShape)
                    .background(cs.primaryContainer),
            )
            Icon(Icons.Rounded.SportsEsports, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(38.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (scanning) "Scanning folders…" else "Ready to play",
                style = MaterialTheme.typography.headlineMedium,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Add a ROM folder from storage or grab free open-source homebrew classics.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Beginner-friendly 3-step quick-start strip
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cs.surfaceContainerHighest)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("1. Add ROMs", "2. Auto cores", "3. Play").forEachIndexed { idx, step ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (idx == 0) cs.primaryContainer else cs.surfaceContainerHigh)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (idx == 0) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(cs.primary)
                    .clickable(onClick = onAddFolder),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.CreateNewFolder, null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add ROM Folder", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(cs.secondaryContainer)
                    .clickable(onClick = onOpenHomebrew),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Free Homebrew", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
            }
        }
    }
}
