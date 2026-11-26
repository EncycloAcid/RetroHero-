package com.retrohero.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import com.retrohero.app.data.Cheat
import com.retrohero.app.data.CheatFinder
import com.retrohero.app.data.CheatPackMatch
import com.retrohero.app.data.Cheats
import kotlinx.coroutines.launch
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.ExternalEmulator
import com.retrohero.app.data.ExternalEmulators
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameDossier
import com.retrohero.app.data.GameSeries
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.RaGameData
import com.retrohero.app.data.RaProfile
import com.retrohero.app.data.RelatedGame
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalReducedMotion
import com.retrohero.app.ui.components.M3ConnectedButtonGroup
import com.retrohero.app.ui.components.M3ConsoleImage
import com.retrohero.app.ui.components.M3ExpressiveSpring
import com.retrohero.app.ui.components.M3GameCover
import com.retrohero.app.ui.components.M3PhysicalBoxViewerSection
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.M3SplitButton
import com.retrohero.app.ui.components.M3WavyBar
import com.retrohero.app.ui.components.m3SegmentedShape
import com.retrohero.app.ui.theme.polygon
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DetailShelfIds = listOf("playing", "completed", "backlog")
private val DetailShelfLabels = listOf("Playing", "Completed", "Backlog")

private data class GallerySlide(
    val id: String,
    val label: String,
    val sources: List<Any>,
    val userShotFile: File? = null,
)

private data class GameMilestone(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val polygon: RoundedPolygon,
    val unlocked: Boolean,
    val progressPct: Int,
)

private fun formatFileSizeShort(bytes: Long): String {
    if (bytes <= 0L) return "ROM"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "${kb.toInt()} KB"
        else -> "$bytes B"
    }
}

private fun formatCompactPlayTime(seconds: Long): String {
    if (seconds <= 0L) return "New"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        else -> "<1m"
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val diffMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L)
    val mins = diffMs / 60_000L
    val hours = mins / 60L
    val days = hours / 24L
    return when {
        mins < 1L -> "just now"
        mins < 60L -> "${mins}m ago"
        hours < 24L -> "${hours}h ago"
        days == 1L -> "yesterday"
        days < 30L -> "${days}d ago"
        else -> "${days / 30L}mo ago"
    }
}

private fun buildGameMilestones(
    stat: PlayStat?,
    saveSlots: List<Pair<Int, Long>>,
    screenshots: List<File>,
    isFavorite: Boolean,
    shelfStatus: String,
): List<GameMilestone> {
    val secs = stat?.seconds ?: 0L
    val sessions = stat?.sessions ?: 0
    return listOf(
        GameMilestone(
            title = "First Boot",
            subtitle = "Launch this classic for the first time",
            icon = Icons.Rounded.PlayArrow,
            polygon = MaterialShapes.Sunny,
            unlocked = sessions >= 1,
            progressPct = if (sessions >= 1) 100 else 0,
        ),
        GameMilestone(
            title = "Quick Saver",
            subtitle = "Store a save state or auto-save snapshot",
            icon = Icons.Rounded.Save,
            polygon = MaterialShapes.Cookie6Sided,
            unlocked = saveSlots.isNotEmpty(),
            progressPct = if (saveSlots.isNotEmpty()) 100 else 0,
        ),
        GameMilestone(
            title = "Warming Up",
            subtitle = "Log at least 15 minutes of playtime",
            icon = Icons.Rounded.Schedule,
            polygon = MaterialShapes.Clover4Leaf,
            unlocked = secs >= 900L,
            progressPct = ((secs * 100L) / 900L).toInt().coerceIn(0, 100),
        ),
        GameMilestone(
            title = "Regular Return",
            subtitle = "Return across 3 separate play sessions",
            icon = Icons.Rounded.LocalFireDepartment,
            polygon = MaterialShapes.Pentagon,
            unlocked = sessions >= 3,
            progressPct = ((sessions * 100) / 3).coerceIn(0, 100),
        ),
        GameMilestone(
            title = "Deep Dive",
            subtitle = "Reach 2 hours of total playtime",
            icon = Icons.Rounded.AutoAwesome,
            polygon = MaterialShapes.Gem,
            unlocked = secs >= 7200L,
            progressPct = ((secs * 100L) / 7200L).toInt().coerceIn(0, 100),
        ),
        GameMilestone(
            title = "Shutterbug",
            subtitle = "Capture an in-game screenshot moment",
            icon = Icons.Rounded.CameraAlt,
            polygon = MaterialShapes.SoftBurst,
            unlocked = screenshots.isNotEmpty(),
            progressPct = if (screenshots.isNotEmpty()) 100 else 0,
        ),
        GameMilestone(
            title = "Hall of Fame",
            subtitle = "Pin this title to your favorites",
            icon = Icons.Rounded.Favorite,
            polygon = MaterialShapes.Heart,
            unlocked = isFavorite,
            progressPct = if (isFavorite) 100 else 0,
        ),
        GameMilestone(
            title = "Credits Rolled",
            subtitle = "Mark this game as Completed on your shelf",
            icon = Icons.Rounded.EmojiEvents,
            polygon = MaterialShapes.Cookie7Sided,
            unlocked = shelfStatus == "completed",
            progressPct = if (shelfStatus == "completed") 100 else 0,
        ),
    )
}

/**
 * Full-screen mobile-first Material 3 Expressive Game Showcase Page inspired by
 * modern game store/launcher dossiers: cinematic hero banner, real stats grid,
 * interactive screenshot gallery, live game credits, pentagonal genre radar,
 * magazine-style editorial feature spotlights, real milestones, and emulator controls.
 */
@Composable
internal fun M3GameDetailPage(
    game: Game,
    stat: PlayStat?,
    isFavorite: Boolean,
    shelfStatus: String,
    saveSlots: List<Pair<Int, Long>>,
    currentCore: CoreInfo?,
    coreInstalled: Boolean = true,
    installedExternalEmulators: List<ExternalEmulator> = emptyList(),
    selectedExternalEmulator: ExternalEmulator? = null,
    screenshots: List<File> = emptyList(),
    initialDossier: GameDossier? = null,
    loadDossier: (suspend (Game, Boolean) -> GameDossier)? = null,
    raProfile: RaProfile? = null,
    initialRaData: RaGameData? = null,
    loadRaData: (suspend (Game, Boolean) -> RaGameData?)? = null,
    onOpenRaSignIn: () -> Unit = {},
    onSetShelfStatus: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onPlay: (Int?) -> Unit,
    onSelectCore: (CoreInfo) -> Unit,
    onSelectExternalEmulator: (ExternalEmulator) -> Unit = {},
    onUseScreenshotAsCover: (File) -> Unit = {},
    onClearCover: () -> Unit = {},
    onDelete: (() -> Unit)?,
    patches: List<com.retrohero.app.data.RomPatch> = emptyList(),
    onAttachPatch: () -> Unit = {},
    onPlayWithPatch: (com.retrohero.app.data.RomPatch) -> Unit = {},
    onTogglePatchSeparateSave: (com.retrohero.app.data.RomPatch, Boolean) -> Unit = { _, _ -> },
    onDeletePatch: (com.retrohero.app.data.RomPatch) -> Unit = {},
    allGames: List<Game> = emptyList(),
    onOpenGame: (Game) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPaths = remember(context) { AppPaths(context) }
    val cores = game.system?.cores.orEmpty()
    val hasAutoSave = saveSlots.any { it.first == AppPaths.AUTO_SLOT }
    val sysPoly = game.system?.polygon ?: MaterialShapes.Cookie6Sided

    var savedCheats by remember(game.id) { mutableStateOf(Cheats.load(appPaths.cheatsFile(game))) }
    var findingCheats by remember(game.id) { mutableStateOf(false) }
    var cheatSource by remember(game.id) { mutableStateOf<String?>(null) }
    var cheatError by remember(game.id) { mutableStateOf<String?>(null) }
    var cheatPacks by remember(game.id) { mutableStateOf<List<CheatPackMatch>>(emptyList()) }
    var activeCheatPack by remember(game.id) { mutableStateOf<CheatPackMatch?>(null) }
    var showCheatsSheet by remember(game.id) { mutableStateOf(false) }

    fun persistCheats(updated: List<Cheat>) {
        savedCheats = updated
        Cheats.save(appPaths.cheatsFile(game), updated)
    }

    fun triggerAutoFindCheats(openSheetOnSuccess: Boolean = true) {
        if (findingCheats) return
        findingCheats = true
        cheatError = null
        scope.launch {
            val res = CheatFinder.autoFind(game, appPaths.cheats)
            findingCheats = false
            cheatPacks = res.alternatePacks
            if (res.cheats.isNotEmpty()) {
                val merged = CheatFinder.merge(savedCheats, res.cheats)
                persistCheats(merged)
                activeCheatPack = res.activePack
                cheatSource = res.sourceLabel
                cheatError = null
                if (openSheetOnSuccess) showCheatsSheet = true
            } else {
                cheatError = res.error
                if (openSheetOnSuccess) showCheatsSheet = true
            }
        }
    }

    var dossier by remember(game.id) {
        mutableStateOf(initialDossier ?: GameDossier.localFallback(game))
    }
    var loadingMeta by remember(game.id) { mutableStateOf(loadDossier != null && initialDossier == null) }
    var refreshNonce by remember(game.id) { mutableIntStateOf(0) }

    LaunchedEffect(game.id, refreshNonce) {
        val loader = loadDossier ?: return@LaunchedEffect
        loadingMeta = true
        dossier = runCatching { loader(game, refreshNonce > 0) }.getOrElse { GameDossier.localFallback(game) }
        loadingMeta = false
    }

    var raData by remember(game.id) { mutableStateOf(initialRaData) }
    var loadingRa by remember(game.id, raProfile?.username) { mutableStateOf(raProfile != null && loadRaData != null && initialRaData == null) }
    var raRefreshNonce by remember(game.id) { mutableIntStateOf(0) }
    var showAllRa by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(game.id, raProfile?.username, raRefreshNonce) {
        val loader = loadRaData ?: return@LaunchedEffect
        if (raProfile == null) {
            raData = null
            loadingRa = false
            return@LaunchedEffect
        }
        loadingRa = true
        raData = runCatching { loader(game, raRefreshNonce > 0) }.getOrNull()
        loadingRa = false
    }

    val seriesCards = remember(game.id, dossier.relatedGames, allGames) {
        GameSeries.resolveWithLibrary(game, dossier.relatedGames, allGames)
    }

    val milestones = remember(stat, saveSlots, screenshots, isFavorite, shelfStatus) {
        buildGameMilestones(stat, saveSlots, screenshots, isFavorite, shelfStatus)
    }
    val unlockedCount = remember(milestones) { milestones.count { it.unlocked } }
    val milestonePct = remember(unlockedCount, milestones) {
        if (milestones.isEmpty()) 0 else (unlockedCount * 100) / milestones.size
    }

    val slides = remember(
        game.id,
        screenshots,
        dossier.heroImageUrl,
        dossier.boxArtUrl,
        dossier.boxBackUrl,
        dossier.cartFrontUrl,
        dossier.discUrl,
        dossier.titleScreenUrl,
        dossier.screenshotUrls,
    ) {
        buildList {
            if (dossier.screenshotUrls.isNotEmpty()) {
                dossier.screenshotUrls.forEachIndexed { i, url ->
                    val label = if (dossier.screenshotUrls.size == 1) "Gameplay" else "Gameplay ${i + 1}"
                    add(GallerySlide("lb_shot_$i", label, listOf(url) + game.gameplaySnapUrls + game.boxArtUrls))
                }
            } else if (game.gameplaySnapUrls.isNotEmpty()) {
                add(GallerySlide("snap", "Gameplay", game.gameplaySnapUrls + game.titleScreenUrls + game.boxArtUrls))
            }
            val titleSources = buildList<Any> {
                dossier.titleScreenUrl?.let { add(it) }
                addAll(game.titleScreenUrls)
                dossier.boxArtUrl?.let { add(it) }
                addAll(game.boxArtUrls)
            }
            if (titleSources.isNotEmpty()) {
                add(GallerySlide("title", "Title screen", titleSources))
            }
            val boxSources = buildList<Any> {
                dossier.boxArtUrl?.let { add(it) }
                addAll(game.boxArtUrls)
            }
            if (boxSources.isNotEmpty()) {
                add(GallerySlide("box", "Box front", boxSources))
            }
            dossier.boxBackUrl?.let { backUrl ->
                add(GallerySlide("box_back", "Box back", listOf(backUrl)))
            }
            dossier.cartFrontUrl?.let { cartUrl ->
                add(GallerySlide("cart_front", "Cartridge", listOf(cartUrl)))
            }
            dossier.discUrl?.let { discUrl ->
                add(GallerySlide("disc_art", "Disc", listOf(discUrl)))
            }
            val heroUrl = dossier.heroImageUrl
            if (heroUrl != null && heroUrl != dossier.boxArtUrl) {
                add(GallerySlide("hero_art", "Key art", listOf(heroUrl) + game.artUrls))
            }
            screenshots.forEachIndexed { i, shot ->
                add(GallerySlide("shot_${shot.absolutePath}", "Capture ${i + 1}", listOf(shot), userShotFile = shot))
            }
        }
    }
    var selectedSlideIdx by remember(game.id) { mutableIntStateOf(0) }
    var lightboxSlideIdx by remember(game.id) { mutableStateOf<Int?>(null) }
    var open3dInspectorNonce by remember(game.id) { mutableIntStateOf(0) }
    val activeSlide = slides.getOrNull(selectedSlideIdx) ?: slides.firstOrNull()

    val listState = rememberLazyListState()
    LaunchedEffect(game.id) {
        listState.scrollToItem(0)
    }
    val scrolledPastHero by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (scrolledPastHero) 0.96f else 0f,
        animationSpec = tween(220),
        label = "detailTopBarAlpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. STOREFRONT HERO BANNER
            item(key = "detail_hero") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(336.dp)
                        .clipToBounds(),
                ) {
                    // Storefront key-art backdrop (LaunchBox 1920x1080 Fanart/Banner -> LaunchBox Box -> libretro)
                    val heroSources = remember(game.id, dossier.heroImageUrl, dossier.boxArtUrl) {
                        buildList<Any> {
                            dossier.heroImageUrl?.let { add(it) }
                            dossier.boxArtUrl?.let { add(it) }
                            addAll(game.boxArtUrls.take(2))
                            addAll(game.titleScreenUrls.take(2))
                            addAll(game.gameplaySnapUrls.take(2))
                        }
                    }
                    M3FallbackArtFrame(
                        game = game,
                        sources = heroSources,
                        modifier = Modifier.fillMaxSize().clipToBounds(),
                    )
                    // Lush tonal gradient melting cleanly into cs.background
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to cs.background.copy(alpha = 0.50f),
                                    0.38f to cs.background.copy(alpha = 0.36f),
                                    0.72f to cs.background.copy(alpha = 0.88f),
                                    1.0f to cs.background,
                                ),
                            ),
                    )

                    // Bottom hero lockup: Uncropped Retail Box Art (tap to launch 3D Box & Media Inspector!) + Title
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val coverMod = Modifier
                            .width(96.dp)
                            .aspectRatio(0.86f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cs.surfaceContainerHighest)
                            .border(1.5.dp, cs.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                open3dInspectorNonce++
                            }
                        val lbBox = dossier.boxArtUrl
                        if (lbBox != null) {
                            M3FallbackArtFrame(
                                game = game,
                                sources = listOf(lbBox) + game.boxArtUrls,
                                storefrontFit = true,
                                modifier = coverMod,
                            )
                        } else {
                            M3GameCover(
                                game = game,
                                showTitle = false,
                                fitCover = true,
                                modifier = coverMod,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 30.sp,
                                ),
                                color = cs.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOf(dossier.developer, dossier.releaseYear)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = dossier.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // 2. PRIMARY PLAY SPLIT CTA + SAVE STATES STRIP
            item(key = "detail_play_cta") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    M3SplitButton(
                        label = if (hasAutoSave) "Resume game" else "Play game",
                        icon = Icons.Rounded.PlayArrow,
                        onPrimaryClick = { onPlay(null) },
                        trailingIcon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        trailingDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                        onTrailingClick = onToggleFavorite,
                        trailingActive = isFavorite,
                        height = 56.dp,
                    )

                    if (game.isMultiDisc) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(cs.secondaryContainer.copy(alpha = 0.65f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(cs.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Album, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "${game.discCount}-Disc Set · Auto-grouped",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = cs.onSecondaryContainer,
                                    )
                                    Text(
                                        text = "Virtual .m3u + shared battery save · 1-tap Swap Disc in-game",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSecondaryContainer.copy(alpha = 0.80f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(game.discs, key = { it.id }) { disc ->
                                    Box(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(cs.surfaceContainerHighest.copy(alpha = 0.88f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "${disc.label} · ${formatFileSizeShort(disc.size)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = cs.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (saveSlots.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(saveSlots, key = { it.first }) { (slot, timestamp) ->
                                val rel = remember(timestamp) { formatRelativeTime(timestamp) }
                                val baseLabel = if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot"
                                Row(
                                    Modifier
                                        .height(40.dp)
                                        .clip(CircleShape)
                                        .background(if (slot == AppPaths.AUTO_SLOT) cs.primaryContainer else cs.secondaryContainer)
                                        .clickable { onPlay(slot) }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Save,
                                        null,
                                        tint = if (slot == AppPaths.AUTO_SLOT) cs.onPrimaryContainer else cs.onSecondaryContainer,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = if (rel.isNotEmpty()) "$baseLabel · $rel" else baseLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                                        color = if (slot == AppPaths.AUTO_SLOT) cs.onPrimaryContainer else cs.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2B. 1-TAP IN-MEMORY ROM SOFT-PATCHING (IPS / BPS / UPS / xDelta)
            if (!game.isMultiDisc && game.system?.keepZipped != true) {
                item(key = "detail_patches_section") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (patches.isEmpty()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cs.surfaceContainerLow)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onAttachPatch()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(cs.tertiaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = cs.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "Soft-patch ROM (IPS · BPS · UPS · xDelta)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = cs.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Attach fan translations or ROM hacks in memory — original ROM untouched",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(cs.primaryContainer)
                                        .padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = cs.onPrimaryContainer,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = "Attach",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = cs.onPrimaryContainer,
                                    )
                                }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "Translations & ROM hacks",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = cs.onSurface,
                                        )
                                        Box(
                                            Modifier
                                                .clip(CircleShape)
                                                .background(cs.tertiaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = "${patches.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = cs.onTertiaryContainer,
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Patched cleanly in memory on boot · Auto header alignment · Original ROM untouched",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(cs.primaryContainer)
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            onAttachPatch()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = cs.onPrimaryContainer,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = "Attach",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = cs.onPrimaryContainer,
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                patches.forEachIndexed { idx, patch ->
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(m3SegmentedShape(idx, patches.size))
                                            .background(cs.surfaceContainerHigh)
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onPlayWithPatch(patch)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(cs.tertiaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            ) {
                                                Text(
                                                    text = patch.format.label.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 10.sp,
                                                    ),
                                                    color = cs.onTertiaryContainer,
                                                )
                                            }
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(
                                                    text = patch.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = cs.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = "${formatFileSizeShort(patch.size)} · Grouped under ${game.title}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cs.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Row(
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(cs.primary)
                                                    .clickable {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                        onPlayWithPatch(patch)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.PlayArrow,
                                                    contentDescription = null,
                                                    tint = cs.onPrimary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    text = "Play",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = cs.onPrimary,
                                                )
                                            }
                                        }

                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (patch.separateSave) cs.secondaryContainer
                                                        else cs.surfaceContainerHighest,
                                                    )
                                                    .clickable {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                        onTogglePatchSeparateSave(patch, !patch.separateSave)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Save,
                                                    contentDescription = null,
                                                    tint = if (patch.separateSave) cs.onSecondaryContainer else cs.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Text(
                                                    text = if (patch.separateSave) "Own save slot" else "Shares parent save",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (patch.separateSave) cs.onSecondaryContainer else cs.onSurfaceVariant,
                                                )
                                            }
                                            Row(
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(cs.surfaceContainerHighest.copy(alpha = 0.7f))
                                                    .clickable {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                        onDeletePatch(patch)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Remove patch",
                                                    tint = cs.error,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                                Text(
                                                    text = "Remove",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = cs.error,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2e. AUTO-FIND CHEATS STRIP
            item(key = "detail_cheats_strip") {
                val activeCheats = remember(savedCheats) { savedCheats.count { it.enabled } }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(cs.surfaceContainerLow)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (savedCheats.isEmpty() && !findingCheats) {
                                triggerAutoFindCheats(openSheetOnSuccess = true)
                            } else {
                                showCheatsSheet = true
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (activeCheats > 0) cs.primaryContainer else cs.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (findingCheats) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = cs.onSecondaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Code,
                                contentDescription = null,
                                tint = if (activeCheats > 0) cs.onPrimaryContainer else cs.onSecondaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = when {
                                findingCheats -> "Finding cheats for ${game.title}…"
                                savedCheats.isNotEmpty() -> "Cheats (${savedCheats.size} ready${if (activeCheats > 0) " · $activeCheats active" else ""})"
                                else -> "Auto-find cheats"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when {
                                cheatSource != null -> cheatSource!!
                                savedCheats.isNotEmpty() -> "Tap to search & toggle codes before launching"
                                else -> "Game Genie, Action Replay, GameShark & Code Breaker codes"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.primaryContainer)
                            .clickable(enabled = !findingCheats) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                triggerAutoFindCheats(openSheetOnSuccess = true)
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (savedCheats.isEmpty()) "Find" else "Browse",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = cs.onPrimaryContainer,
                        )
                    }
                }
            }

            // 3. REAL STATS 4-COLUMN HYDRA-STYLE BENTO STRIP
            item(key = "detail_stats_strip") {
                val raSet = raData
                val thirdStat = if (raSet != null && raSet.totalCount > 0) {
                    "${raSet.unlockedCount}/${raSet.totalCount}" to "TROPHIES"
                } else {
                    "$milestonePct%" to "MILESTONES"
                }
                val statsCells = listOf(
                    formatCompactPlayTime(stat?.seconds ?: 0L) to "PLAY TIME",
                    "${stat?.sessions ?: 0}" to "SESSIONS",
                    thirdStat,
                    (dossier.releaseYear.takeIf { it.length == 4 } ?: formatFileSizeShort(game.size)) to
                        (if (dossier.releaseYear.length == 4) "RELEASED" else "ROM SIZE"),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    statsCells.forEachIndexed { idx, (value, label) ->
                        val shape = when (idx) {
                            0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 10.dp, bottomEnd = 10.dp)
                            statsCells.lastIndex -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                            else -> RoundedCornerShape(10.dp)
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(shape)
                                .background(cs.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            // 4. CLEAN STOREFRONT SCREENSHOTS & MEDIA GALLERY
            if (activeSlide != null) {
                item(key = "detail_gallery") {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Screenshots & media",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = cs.onSurface,
                            )
                            if (screenshots.isNotEmpty()) {
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(cs.surfaceContainerHighest)
                                        .clickable(onClick = onClearCover)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                    Text("Reset cover", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                            }
                        }

                        // Clean full-frame storefront screenshot viewport (uncropped, unobstructed — tap to expand)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .aspectRatio(16f / 10.5f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(cs.surfaceContainerHigh)
                                .border(1.dp, cs.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    lightboxSlideIdx = selectedSlideIdx
                                },
                        ) {
                            AnimatedContent(
                                targetState = activeSlide,
                                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                                label = "galleryShowcase",
                            ) { slide ->
                                M3FallbackArtFrame(
                                    game = game,
                                    sources = slide.sources,
                                    storefrontFit = true,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        // Subtle caption row below the photo (never covering the screenshot)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${activeSlide.label} · ${game.system?.name ?: game.systemId.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (activeSlide.userShotFile != null) {
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(cs.primaryContainer)
                                        .clickable { onUseScreenshotAsCover(activeSlide.userShotFile) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Rounded.Wallpaper, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(13.dp))
                                    Text("Use as cover", style = MaterialTheme.typography.labelSmall, color = cs.onPrimaryContainer)
                                }
                            } else {
                                Text(
                                    text = "Tap to expand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.primary,
                                )
                            }
                        }

                        // Clean uncropped horizontal thumbnail strip with labels below each image
                        if (slides.size > 1) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                itemsIndexed(slides, key = { _, s -> s.id }) { idx, slide ->
                                    val selected = idx == selectedSlideIdx
                                    val thumbScale by animateFloatAsState(
                                        if (selected) 1f else 0.96f,
                                        M3ExpressiveSpring,
                                        label = "thumbScale",
                                    )
                                    Column(
                                        Modifier
                                            .width(104.dp)
                                            .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                selectedSlideIdx = idx
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 10.5f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(cs.surfaceContainerHigh)
                                                .border(
                                                    width = if (selected) 2.dp else 1.dp,
                                                    color = if (selected) cs.primary else cs.outlineVariant.copy(alpha = 0.30f),
                                                    shape = RoundedCornerShape(14.dp),
                                                ),
                                        ) {
                                            M3FallbackArtFrame(
                                                game = game,
                                                sources = slide.sources,
                                                storefrontFit = true,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                        Text(
                                            text = slide.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            color = if (selected) cs.primary else cs.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4B. LAUNCHBOX-STYLE INTERACTIVE 3D RETAIL BOX, SPINE & CARTRIDGE/DISC SPIN VIEWER
            item(key = "detail_3d_box_viewer") {
                M3PhysicalBoxViewerSection(
                    game = game,
                    dossier = dossier,
                    openInspectorTrigger = open3dInspectorNonce,
                )
            }

            // 5. GAME INFO & CREDITS DOSSIER (Hydra-style key-value rows)
            item(key = "detail_credits") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Game info",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = cs.onSurface,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (loadingMeta) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = cs.primary)
                            }
                            Row(
                                Modifier
                                    .clip(CircleShape)
                                    .background(if (dossier.isLive) cs.primaryContainer else cs.surfaceContainerHighest)
                                    .clickable(enabled = loadDossier != null && !loadingMeta) { refreshNonce++ }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    if (dossier.isLive) Icons.Rounded.Verified else Icons.Rounded.Refresh,
                                    null,
                                    tint = if (dossier.isLive) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = dossier.sourceLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (dossier.isLive) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    val infoRows = buildList {
                        if (dossier.seriesName.isNotBlank()) add("SERIES" to dossier.seriesName)
                        if (game.isMultiDisc) add("DISCS" to "${game.discCount} discs · Unified save & virtual .m3u")
                        add("DEVELOPED BY" to dossier.developer)
                        add("PUBLISHED BY" to dossier.publisher)
                        add("RELEASE DATE" to dossier.releaseDate)
                        add("GENRE" to dossier.genre)
                        add("MODES" to dossier.modes)
                        if (dossier.esrbRating.isNotBlank()) add("ESRB RATING" to dossier.esrbRating)
                        dossier.communityRating?.let { rating ->
                            add("COMMUNITY RATING" to String.format(java.util.Locale.US, "★ %.1f / 5.0", rating))
                        }
                        add("PLATFORM" to (game.system?.name ?: game.systemId.uppercase()))
                        add("ROM FILE" to "${game.extension.uppercase()} · ${formatFileSizeShort(game.size)}")
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        infoRows.forEachIndexed { idx, (key, value) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(m3SegmentedShape(idx, infoRows.size))
                                    .background(cs.surfaceContainerHigh)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        letterSpacing = 0.7.sp,
                                    ),
                                    color = cs.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = cs.onSurface,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // 6. PENTAGONAL GAME DNA RADAR CHART (inspired by Screenshot 2)
            item(key = "detail_radar") {
                M3GenreRadarCard(
                    axes = dossier.radarAxes,
                    genreLabel = dossier.genre,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 6b. PREQUELS & SEQUELS FRANCHISE TIMELINE
            if (seriesCards.isNotEmpty()) {
                item(key = "detail_series") {
                    M3SeriesTimelineSection(
                        seriesName = dossier.seriesName,
                        cards = seriesCards,
                        onOpenGame = { target ->
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenGame(target)
                        },
                    )
                }
            }

            // 7. MAGAZINE-STYLE GAME ADVERTISEMENT & EDITORIAL SHOWCASE (Screenshots 3 & 4)
            item(key = "detail_editorial") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    M3WavyBar(color = cs.outlineVariant.copy(alpha = 0.55f), amplitude = 2.dp, wavelength = 24.dp, strokeWidth = 2.dp)

                    // About card
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(cs.surfaceContainerHigh)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            M3ShapeBadge(polygon = MaterialShapes.Flower, containerColor = cs.primaryContainer, size = 30.dp) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "ABOUT ${game.title.uppercase()}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = cs.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = dossier.summary,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = cs.onSurface,
                        )
                    }

                    // Alternating editorial spotlight / game advertisement sections
                    dossier.featureSections.forEachIndexed { idx, block ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (idx % 2 == 0) cs.surfaceContainer else cs.surfaceContainerHigh)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val showcaseSlide = slides.getOrNull(idx % slides.size.coerceAtLeast(1))
                            if (showcaseSlide != null) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(cs.surfaceContainerHighest),
                                ) {
                                    M3FallbackArtFrame(
                                        game = game,
                                        sources = showcaseSlide.sources,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(10.dp)
                                            .clip(CircleShape)
                                            .background(cs.primaryContainer.copy(alpha = 0.94f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = if (idx == 0) "FEATURED SPOTLIGHT" else "PLAY ON RETROHERO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.7.sp,
                                            ),
                                            color = cs.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = block.headline,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp,
                                ),
                                color = cs.onSurface,
                            )
                            Text(
                                text = block.body,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 7b. RETROACHIEVEMENTS LIVE TROPHY SHOWCASE
            item(key = "detail_retroachievements") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val activeRa = raData
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "RetroAchievements",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = cs.onSurface,
                            )
                            if (activeRa != null && activeRa.totalCount > 0) {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(if (activeRa.hardcoreEnabled) cs.tertiaryContainer else cs.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = if (activeRa.hardcoreEnabled) "Hardcore" else "Softcore",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                        color = if (activeRa.hardcoreEnabled) cs.onTertiaryContainer else cs.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        if (raProfile != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (loadingRa) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = cs.primary)
                                } else {
                                    Row(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(cs.surfaceContainerHighest)
                                            .clickable { raRefreshNonce++ }
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.Refresh, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = if (activeRa != null && activeRa.totalCount > 0) "${activeRa.earnedPoints}/${activeRa.totalPoints} pts" else "Sync",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when {
                        raProfile == null -> {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(cs.primaryContainer)
                                    .clickable(onClick = onOpenRaSignIn)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                M3ShapeBadge(polygon = MaterialShapes.Sunny, containerColor = cs.primary, size = 42.dp) {
                                    Icon(Icons.Rounded.EmojiEvents, null, tint = cs.onPrimary, modifier = Modifier.size(21.dp))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Connect RetroAchievements",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = cs.onPrimaryContainer,
                                    )
                                    Text(
                                        text = "Sign in to load official trophies and earn real points as you play",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onPrimaryContainer.copy(alpha = 0.82f),
                                    )
                                }
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(cs.primary)
                                        .padding(horizontal = 13.dp, vertical = 7.dp),
                                ) {
                                    Text("Sign in", style = MaterialTheme.typography.labelMedium, color = cs.onPrimary)
                                }
                            }
                        }
                        loadingRa && activeRa == null -> {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cs.surfaceContainerHigh)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.2.dp, color = cs.primary)
                                Text(
                                    text = "Matching ROM hash with RetroAchievements…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                        activeRa == null || activeRa.achievements.isEmpty() -> {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cs.surfaceContainerHigh)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                M3ShapeBadge(polygon = MaterialShapes.Cookie6Sided, containerColor = cs.surfaceContainerHighest, size = 38.dp) {
                                    Icon(Icons.Rounded.EmojiEvents, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "No RetroAchievements set matched",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = cs.onSurface,
                                    )
                                    Text(
                                        text = "Signed in as ${raProfile.username}. Use a supported No-Intro/Redump ROM to unlock official RA trophies.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        else -> {
                            M3WavyBar(
                                progress = (activeRa.unlockedCount.toFloat() / activeRa.totalCount.coerceAtLeast(1)).coerceIn(0f, 1f),
                                color = cs.surfaceContainerHighest,
                                progressColor = cs.tertiary,
                                amplitude = 2.5.dp,
                                wavelength = 20.dp,
                                strokeWidth = 3.dp,
                            )

                            val ordered = remember(activeRa) {
                                activeRa.achievements.sortedWith(
                                    compareByDescending<com.retrohero.app.data.RaAchievement> { it.unlocked }
                                        .thenByDescending { it.type == "win_condition" || it.type == "progression" }
                                        .thenByDescending { it.points },
                                )
                            }
                            val visibleList = if (showAllRa) ordered else ordered.take(8)

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                visibleList.forEachIndexed { idx, ach ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(m3SegmentedShape(idx, visibleList.size))
                                            .background(if (ach.unlocked) cs.surfaceContainerHigh else cs.surfaceContainer)
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (ach.unlocked) cs.primaryContainer else cs.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            AsyncImage(
                                                model = ach.displayBadgeUrl,
                                                contentDescription = ach.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = ach.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                                    color = if (ach.unlocked) cs.onSurface else cs.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                if (ach.type.isNotBlank()) {
                                                    val tag = when (ach.type) {
                                                        "win_condition" -> "Win"
                                                        "progression" -> "Story"
                                                        "missable" -> "Missable"
                                                        else -> ach.type
                                                    }
                                                    Box(
                                                        Modifier
                                                            .clip(CircleShape)
                                                            .background(cs.secondaryContainer)
                                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                            color = cs.onSecondaryContainer,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = ach.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = cs.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Box(
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(if (ach.unlocked) cs.primaryContainer else cs.surfaceContainerHighest)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                            ) {
                                                Text(
                                                    text = "${ach.points} pts",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                                    color = if (ach.unlocked) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                )
                                            }
                                            if (ach.unlocked) {
                                                Icon(
                                                    Icons.Rounded.CheckCircle,
                                                    contentDescription = "Unlocked",
                                                    tint = cs.primary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (ordered.size > 8) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .background(cs.surfaceContainerHighest)
                                        .clickable { showAllRa = !showAllRa }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (showAllRa) "Show fewer achievements" else "Show all ${ordered.size} achievements",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = cs.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8. YOUR MILESTONES / ACHIEVEMENTS CARD (Screenshots 3 & 4)
            item(key = "detail_milestones") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Your milestones",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = cs.onSurface,
                            )
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(cs.primaryContainer)
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "$unlockedCount / ${milestones.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = cs.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            text = "$milestonePct%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = cs.primary,
                        )
                    }

                    M3WavyBar(
                        progress = (unlockedCount.toFloat() / milestones.size.coerceAtLeast(1)).coerceIn(0f, 1f),
                        color = cs.surfaceContainerHighest,
                        progressColor = cs.primary,
                        amplitude = 2.5.dp,
                        wavelength = 20.dp,
                        strokeWidth = 3.dp,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        milestones.forEachIndexed { idx, m ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(m3SegmentedShape(idx, milestones.size))
                                    .background(if (m.unlocked) cs.surfaceContainerHigh else cs.surfaceContainer)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                M3ShapeBadge(
                                    polygon = m.polygon,
                                    containerColor = if (m.unlocked) cs.primaryContainer else cs.surfaceContainerHighest,
                                    size = 42.dp,
                                ) {
                                    Icon(
                                        m.icon,
                                        null,
                                        tint = if (m.unlocked) cs.onPrimaryContainer else cs.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = m.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (m.unlocked) cs.onSurface else cs.onSurfaceVariant,
                                    )
                                    Text(
                                        text = m.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant.copy(alpha = if (m.unlocked) 0.85f else 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(if (m.unlocked) cs.primary else cs.surfaceContainerHighest)
                                        .padding(horizontal = 9.dp, vertical = 4.dp),
                                ) {
                                    if (m.unlocked) {
                                        Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text(
                                            text = "${m.progressPct}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. SHELF STATUS, EMULATOR CORE SWITCHER & LIBRARY MANAGEMENT
            item(key = "detail_controls") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Shelf status", style = MaterialTheme.typography.titleSmall, color = cs.primary)
                        val selIdx = DetailShelfIds.indexOf(shelfStatus).coerceAtLeast(0)
                        M3ConnectedButtonGroup(
                            options = DetailShelfLabels,
                            selectedIndex = selIdx,
                            onSelect = { onSetShelfStatus(DetailShelfIds[it]) },
                            height = 46.dp,
                        )
                    }

                    val supportedExternal = remember(game.systemId) { ExternalEmulators.forSystem(game.systemId) }
                    val totalEngines = cores.size + installedExternalEmulators.size
                    if (totalEngines > 1 || supportedExternal.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Emulator engine", style = MaterialTheme.typography.titleSmall, color = cs.primary)
                                if (cores.size > 1) {
                                    Row(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(cs.primaryContainer.copy(alpha = 0.72f))
                                            .padding(horizontal = 9.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.Verified, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "Universal Save",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = cs.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                            if (cores.size > 1) {
                                Text(
                                    text = "Battery saves auto-convert & stay portable when switching cores.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                            val autoProf = remember(game.id, currentCore?.id) {
                                val defMode = com.retrohero.app.data.HardwareProfiles.defaultModeFor(game)
                                com.retrohero.app.data.HardwareProfiles.resolve(game, currentCore?.id.orEmpty(), defMode)
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cs.secondaryContainer.copy(alpha = 0.52f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${autoProf.mode.label} · ${autoProf.title}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = cs.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = autoProf.summary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSecondaryContainer.copy(alpha = 0.80f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (totalEngines > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    cores.forEachIndexed { idx, core ->
                                        val selected = selectedExternalEmulator == null && core.id == currentCore?.id
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(m3SegmentedShape(idx, totalEngines))
                                                .background(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
                                                .clickable { onSelectCore(core) }
                                                .padding(horizontal = 16.dp, vertical = 13.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = core.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                                                )
                                                Text(
                                                    text = "Built-in RetroHero core",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = (if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant).copy(alpha = 0.78f),
                                                )
                                            }
                                            if (selected) {
                                                Icon(Icons.Rounded.Check, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    installedExternalEmulators.forEachIndexed { eIdx, ext ->
                                        val rowIdx = cores.size + eIdx
                                        val selected = selectedExternalEmulator?.id == ext.id
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(m3SegmentedShape(rowIdx, totalEngines))
                                                .background(if (selected) cs.tertiaryContainer else cs.surfaceContainerHigh)
                                                .clickable { onSelectExternalEmulator(ext) }
                                                .padding(horizontal = 16.dp, vertical = 13.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = ext.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (selected) cs.onTertiaryContainer else cs.onSurface,
                                                )
                                                Text(
                                                    text = ext.badge,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = (if (selected) cs.onTertiaryContainer else cs.onSurfaceVariant).copy(alpha = 0.80f),
                                                )
                                            }
                                            if (selected) {
                                                Icon(Icons.Rounded.Check, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            if (installedExternalEmulators.isEmpty() && supportedExternal.isNotEmpty()) {
                                val names = supportedExternal.take(3).joinToString(" · ") { it.name }
                                Text(
                                    text = if (cores.isEmpty()) {
                                        "Standalone mobile engine required: install $names to launch directly."
                                    } else {
                                        "Tip: Also supports $names for native standalone performance if installed."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (onDelete != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(CircleShape)
                                .background(cs.errorContainer)
                                .clickable(onClick = onDelete),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = cs.onErrorContainer, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Remove from library", style = MaterialTheme.typography.labelLarge, color = cs.onErrorContainer)
                        }
                    }
                }
            }
        }

        // Pinned floating top bar (transparent over hero, clean solid tonal bar once scrolled!)
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(cs.background.copy(alpha = topBarAlpha))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHighest.copy(alpha = 0.92f))
                    .semantics { contentDescription = "Close details" }
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest.copy(alpha = 0.92f))
                        .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    M3ShapeBadge(polygon = sysPoly, containerColor = cs.primary, size = 24.dp) {
                        val sysObj = game.system
                        if (sysObj != null) {
                            M3ConsoleImage(system = sysObj, tint = cs.onPrimary, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Rounded.SportsEsports, null, tint = cs.onPrimary, modifier = Modifier.size(12.dp))
                        }
                    }
                    Text(
                        text = (game.system?.shortName ?: game.systemId).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onSurface,
                    )
                }
                if (currentCore != null) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(if (coreInstalled) cs.primaryContainer else cs.tertiaryContainer)
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            if (coreInstalled) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                            null,
                            tint = if (coreInstalled) cs.onPrimaryContainer else cs.onTertiaryContainer,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = if (coreInstalled) "Ready" else "Auto-core",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (coreInstalled) cs.onPrimaryContainer else cs.onTertiaryContainer,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isFavorite) cs.tertiaryContainer else cs.surfaceContainerHighest.copy(alpha = 0.92f))
                    .semantics { contentDescription = if (isFavorite) "Remove favorite" else "Add favorite" }
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onToggleFavorite()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                    tint = if (isFavorite) cs.onTertiaryContainer else cs.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    lightboxSlideIdx?.let { startIdx ->
        val currentIdx = startIdx.coerceIn(0, (slides.size - 1).coerceAtLeast(0))
        val slide = slides.getOrNull(currentIdx)
        if (slide != null) {
            Dialog(
                onDismissRequest = { lightboxSlideIdx = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f))
                        .clickable { lightboxSlideIdx = null },
                ) {
                    M3FallbackArtFrame(
                        game = game,
                        sources = slide.sources,
                        storefrontFit = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 72.dp),
                    )
                    // Top close bar
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${slide.label} (${currentIdx + 1}/${slides.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { lightboxSlideIdx = null },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    // Bottom prev/next controls when multiple slides exist
                    if (slides.size > 1) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .clickable {
                                        val prev = (currentIdx - 1 + slides.size) % slides.size
                                        lightboxSlideIdx = prev
                                        selectedSlideIdx = prev
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous", tint = Color.White)
                            }
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.80f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
                                textAlign = TextAlign.Center,
                            )
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .clickable {
                                        val next = (currentIdx + 1) % slides.size
                                        lightboxSlideIdx = next
                                        selectedSlideIdx = next
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheatsSheet) {
        M3GameCheatsSheet(
            game = game,
            cheats = savedCheats,
            searching = findingCheats,
            sourceLabel = cheatSource,
            errorMessage = cheatError,
            packs = cheatPacks,
            activePack = activeCheatPack,
            onAutoFind = { triggerAutoFindCheats(openSheetOnSuccess = true) },
            onSelectPack = { pack ->
                if (!findingCheats && activeCheatPack?.fileName != pack.fileName) {
                    findingCheats = true
                    scope.launch {
                        val loaded = CheatFinder.loadPack(pack)
                        findingCheats = false
                        if (loaded.isNotEmpty()) {
                            val merged = CheatFinder.merge(savedCheats.filter { it.enabled }, loaded)
                            persistCheats(merged)
                            activeCheatPack = pack
                            cheatSource = "${pack.fileName.removeSuffix(".cht")} · libretro-database"
                        }
                    }
                }
            },
            onToggleCheat = { index, enabled ->
                if (index in savedCheats.indices) {
                    persistCheats(savedCheats.toMutableList().also { it[index] = it[index].copy(enabled = enabled) })
                }
            },
            onDisableAll = {
                persistCheats(savedCheats.map { it.copy(enabled = false) })
            },
            onDismiss = { showCheatsSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3GameCheatsSheet(
    game: Game,
    cheats: List<Cheat>,
    searching: Boolean,
    sourceLabel: String?,
    errorMessage: String?,
    packs: List<CheatPackMatch>,
    activePack: CheatPackMatch?,
    onAutoFind: () -> Unit,
    onSelectPack: (CheatPackMatch) -> Unit,
    onToggleCheat: (Int, Boolean) -> Unit,
    onDisableAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("all") }
    val filtered = remember(cheats, query, tab) {
        val q = query.trim().lowercase()
        cheats.mapIndexed { idx, c -> idx to c }
            .filter { (_, c) ->
                val tabOk = when (tab) {
                    "active" -> c.enabled
                    "popular" -> CheatFinder.isPopularCheat(c.name) || c.enabled
                    else -> true
                }
                val qOk = q.isEmpty() || c.name.lowercase().contains(q) || c.code.lowercase().contains(q)
                tabOk && qOk
            }
            .sortedByDescending { (_, c) -> c.enabled }
    }
    val activeCount = remember(cheats) { cheats.count { it.enabled } }
    val popularCount = remember(cheats) { cheats.count { CheatFinder.isPopularCheat(it.name) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${game.title} Cheats",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (sourceLabel != null) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.primaryContainer)
                        .clickable(enabled = !searching) { onAutoFind() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (searching) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = cs.onPrimaryContainer, modifier = Modifier.size(14.dp))
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        text = if (searching) "Searching…" else "Auto-find",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = cs.onPrimaryContainer,
                    )
                }
            }

            if (packs.size > 1) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    packs.forEach { pack ->
                        FilterChip(
                            selected = activePack?.fileName == pack.fileName,
                            onClick = { onSelectPack(pack) },
                            label = { Text(pack.displayLabel, maxLines = 1) },
                        )
                    }
                }
            }

            if (cheats.size > 5) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search ${cheats.size} cheats…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.size(18.dp)) } }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(selected = tab == "all", onClick = { tab = "all" }, label = { Text("All (${cheats.size})") })
                    if (popularCount > 0) {
                        FilterChip(selected = tab == "popular", onClick = { tab = "popular" }, label = { Text("Popular ($popularCount)") })
                    }
                    FilterChip(selected = tab == "active", onClick = { tab = "active" }, label = { Text("Active ($activeCount)") })
                    if (activeCount > 0) {
                        TextButton(onClick = onDisableAll) { Text("Disable all") }
                    }
                }
            }

            when {
                searching && cheats.isEmpty() -> {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Searching libretro-database…", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
                filtered.isEmpty() -> {
                    Text(
                        text = errorMessage ?: if (tab == "active") "No active cheats yet." else "No cheats found matching your filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        itemsIndexed(filtered, key = { _, pair -> "${pair.first}_${pair.second.code}" }) { rowIdx, (origIdx, cheat) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(m3SegmentedShape(rowIdx, filtered.size))
                                    .background(if (cheat.enabled) cs.primaryContainer.copy(alpha = 0.55f) else cs.surfaceContainerHigh)
                                    .clickable { onToggleCheat(origIdx, !cheat.enabled) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = cheat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = cs.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = cheat.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Switch(
                                    checked = cheat.enabled,
                                    onCheckedChange = { onToggleCheat(origIdx, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pentagonal 5-Axis Game DNA Radar Chart drawn natively on Compose Canvas with spring unfurl.
 */
@Composable
private fun M3GenreRadarCard(
    axes: List<GameDossier.RadarAxis>,
    genreLabel: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val reduced = LocalReducedMotion.current
    var revealed by remember(axes) { mutableStateOf(reduced) }
    LaunchedEffect(axes) { revealed = true }
    val animProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 240f),
        label = "radarUnfurl",
    )

    val safeAxes = if (axes.size >= 3) axes.take(5) else GameDossier.defaultRadar("")

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHigh)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3ShapeBadge(polygon = MaterialShapes.Pentagon, containerColor = cs.secondaryContainer, size = 30.dp) {
                    Icon(Icons.Rounded.Flag, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(15.dp))
                }
                Text(
                    text = "Game DNA",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurface,
                )
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(cs.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = genreLabel.split(',', '·').first().trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSecondaryContainer,
                    maxLines = 1,
                )
            }
        }

        // Radar chart + 5 crisp labeled vertices with exact percentage readouts
        Box(
            Modifier
                .fillMaxWidth()
                .height(254.dp),
            contentAlignment = Alignment.Center,
        ) {
            val webColor = cs.outlineVariant.copy(alpha = 0.55f)
            val fillColor = cs.primary.copy(alpha = 0.26f)
            val strokeColor = cs.primary
            val dotBg = cs.surfaceContainerHigh

            Canvas(Modifier.size(168.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension * 0.48f
                val n = safeAxes.size
                val startAngle = -PI / 2.0

                // 4 concentric web rings
                for (ring in 1..4) {
                    val r = maxR * (ring / 4f)
                    val path = Path()
                    for (i in 0 until n) {
                        val a = startAngle + (2.0 * PI * i / n)
                        val px = cx + (r * cos(a)).toFloat()
                        val py = cy + (r * sin(a)).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, color = webColor, style = Stroke(width = 1.2.dp.toPx()))
                }

                // Spokes
                for (i in 0 until n) {
                    val a = startAngle + (2.0 * PI * i / n)
                    drawLine(
                        color = webColor,
                        start = Offset(cx, cy),
                        end = Offset(cx + (maxR * cos(a)).toFloat(), cy + (maxR * sin(a)).toFloat()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Data polygon
                val dataPath = Path()
                val vertices = mutableListOf<Offset>()
                for (i in 0 until n) {
                    val a = startAngle + (2.0 * PI * i / n)
                    val v = (safeAxes[i].value * animProgress).coerceIn(0.05f, 1f)
                    val px = cx + (maxR * v * cos(a)).toFloat()
                    val py = cy + (maxR * v * sin(a)).toFloat()
                    vertices += Offset(px, py)
                    if (i == 0) dataPath.moveTo(px, py) else dataPath.lineTo(px, py)
                }
                dataPath.close()
                drawPath(dataPath, color = fillColor)
                drawPath(
                    dataPath,
                    color = strokeColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                vertices.forEach { pt ->
                    drawCircle(color = dotBg, radius = 4.8.dp.toPx(), center = pt)
                    drawCircle(color = strokeColor, radius = 3.2.dp.toPx(), center = pt)
                }
            }

            // Positioned axis labels + live percentage values around all 5 vertices
            if (safeAxes.size == 5) {
                @Composable
                fun VertexLabel(axis: GameDossier.RadarAxis, align: Alignment.Horizontal, mod: Modifier) {
                    val pct = (axis.value * 100f).toInt().coerceIn(0, 100)
                    Column(modifier = mod, horizontalAlignment = align) {
                        Text(
                            text = axis.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            ),
                            color = if (pct >= 70) cs.primary else cs.onSurface,
                            maxLines = 1,
                        )
                    }
                }

                VertexLabel(safeAxes[0], Alignment.CenterHorizontally, Modifier.align(Alignment.TopCenter))
                VertexLabel(safeAxes[1], Alignment.End, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp, bottom = 28.dp))
                VertexLabel(safeAxes[2], Alignment.End, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 2.dp))
                VertexLabel(safeAxes[3], Alignment.Start, Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 2.dp))
                VertexLabel(safeAxes[4], Alignment.Start, Modifier.align(Alignment.CenterStart).padding(start = 4.dp, bottom = 28.dp))
            }
        }
    }
}

/**
 * Resilient media image frame that cascades through candidate URLs / Files and falls back
 * seamlessly to [M3GameCover] (Bauhaus collector print) so no slot ever renders blank.
 */
@Composable
private fun M3FallbackArtFrame(
    game: Game,
    sources: List<Any>,
    modifier: Modifier = Modifier,
    storefrontFit: Boolean = false,
) {
    var idx by remember(game.id, sources) { mutableIntStateOf(0) }
    var loaded by remember(game.id, sources) { mutableStateOf(false) }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (!loaded || idx >= sources.size) {
            M3GameCover(game = game, showTitle = false, fitCover = storefrontFit, modifier = Modifier.fillMaxSize())
        }
        if (idx < sources.size) {
            if (storefrontFit) {
                AsyncImage(
                    model = sources[idx],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.24f,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(18.dp),
                )
            }
            AsyncImage(
                model = sources[idx],
                contentDescription = null,
                contentScale = if (storefrontFit) ContentScale.Fit else ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = {
                    loaded = false
                    idx++
                },
                modifier = if (storefrontFit) Modifier.fillMaxSize().padding(4.dp) else Modifier.fillMaxSize(),
            )
        }
    }
}

private fun resolveHintSystem(hint: String): GameSystem? {
    val key = hint.trim().lowercase()
    if (key.isBlank()) return null
    val mappedId = when (key) {
        "nes", "famicom" -> "nes"
        "fds" -> "fds"
        "snes", "super nintendo", "sfc" -> "snes"
        "n64", "nintendo 64" -> "n64"
        "gc", "gamecube" -> "gc"
        "wii" -> "wii"
        "wii u", "wiiu" -> "wiiu"
        "switch" -> "switch"
        "gb", "game boy" -> "gb"
        "gbc", "game boy color" -> "gbc"
        "gba", "game boy advance" -> "gba"
        "nds", "ds" -> "nds"
        "3ds" -> "3ds"
        "ps1", "psx", "playstation" -> "ps1"
        "ps2", "playstation 2" -> "ps2"
        "psp" -> "psp"
        "vita", "ps vita" -> "vita"
        "genesis", "mega drive", "md" -> "genesis"
        "sega cd", "segacd", "mega cd" -> "segacd"
        "saturn" -> "saturn"
        "dreamcast", "dc" -> "dreamcast"
        "sms", "master system" -> "sms"
        "gg", "game gear" -> "gg"
        "neo geo", "neogeo" -> "neogeo"
        "hb16", "herobox-16", "herobox16" -> "herobox16"
        "arcade" -> "arcade"
        else -> null
    }
    return mappedId?.let { Systems.get(it) }
        ?: Systems.all.firstOrNull {
            it.shortName.equals(key, ignoreCase = true) ||
                it.id.equals(key, ignoreCase = true) ||
                it.name.equals(key, ignoreCase = true)
        }
}

/**
 * Horizontal Material 3 Expressive franchise rail showing chronological prequels, sequels,
 * and companion titles. Owned entries display live box art + "In library" badge and open
 * directly on tap; non-owned entries show console hardware art + release era.
 */
@Composable
private fun M3SeriesTimelineSection(
    seriesName: String,
    cards: List<Pair<RelatedGame, Game?>>,
    onOpenGame: (Game) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Prequels & sequels",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = cs.onSurface,
            )
            if (seriesName.isNotBlank()) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = cs.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards, key = { (rel, owned) -> "${rel.relation}:${rel.title}:${owned?.id ?: ""}" }) { (entry, ownedGame) ->
                val isPrequel = entry.relation.equals("Prequel", ignoreCase = true)
                val isSequel = entry.relation.equals("Sequel", ignoreCase = true)
                val pillBg = when {
                    isSequel -> cs.primaryContainer
                    isPrequel -> cs.tertiaryContainer
                    else -> cs.secondaryContainer
                }
                val pillFg = when {
                    isSequel -> cs.onPrimaryContainer
                    isPrequel -> cs.onTertiaryContainer
                    else -> cs.onSecondaryContainer
                }
                val relIcon = when {
                    isPrequel -> Icons.AutoMirrored.Rounded.ArrowBack
                    isSequel -> Icons.AutoMirrored.Rounded.ArrowForward
                    else -> Icons.Rounded.AutoAwesome
                }
                val hintSystem = remember(entry.systemHint, ownedGame?.systemId) {
                    ownedGame?.system ?: resolveHintSystem(entry.systemHint)
                }

                Column(
                    Modifier
                        .width(166.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (ownedGame != null) cs.surfaceContainerHighest else cs.surfaceContainerHigh)
                        .then(
                            if (ownedGame != null) {
                                Modifier
                                    .border(1.2.dp, cs.primary.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                                    .clickable { onOpenGame(ownedGame) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    // Top visual showcase area (box art when owned, console hardware art when not owned)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                            .background(pillBg.copy(alpha = 0.36f))
                            .clipToBounds(),
                    ) {
                        if (ownedGame != null) {
                            M3GameCover(
                                game = ownedGame,
                                showTitle = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to cs.scrim.copy(alpha = 0.30f),
                                            0.55f to Color.Transparent,
                                            1f to cs.scrim.copy(alpha = 0.62f),
                                        ),
                                    ),
                            )
                        } else if (hintSystem != null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = 24.dp, bottom = 10.dp, start = 14.dp, end = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                M3ConsoleImage(
                                    system = hintSystem,
                                    tint = pillFg,
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                M3ShapeBadge(
                                    polygon = if (isSequel) MaterialShapes.Sunny else MaterialShapes.Cookie6Sided,
                                    containerColor = pillBg,
                                    size = 46.dp,
                                ) {
                                    Icon(Icons.Rounded.SportsEsports, null, tint = pillFg, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        // Top-left relation badge (PREQUEL / SEQUEL / SERIES)
                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(pillBg.copy(alpha = 0.94f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(relIcon, null, tint = pillFg, modifier = Modifier.size(11.dp))
                            Text(
                                text = entry.relation.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = pillFg,
                            )
                        }

                        // Bottom-right badge: "In library" when owned, or release year
                        if (ownedGame != null) {
                            Row(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(cs.primary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = cs.onPrimary, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "In library",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = cs.onPrimary,
                                )
                            }
                        } else if (entry.year.isNotBlank()) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(cs.surfaceContainerHighest.copy(alpha = 0.88f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = entry.year,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = cs.onSurface,
                                )
                            }
                        }
                    }

                    // Bottom title & platform footer
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 16.sp,
                            ),
                            color = cs.onSurface,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val meta = buildList {
                                if (ownedGame != null && entry.year.isNotBlank()) add(entry.year)
                                if (entry.systemHint.isNotBlank()) add(entry.systemHint)
                                else if (ownedGame?.system?.shortName != null) add(ownedGame.system!!.shortName)
                            }.joinToString(" · ").ifBlank {
                                if (ownedGame != null) "Tap to open" else entry.relation
                            }
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = if (ownedGame != null) cs.primary else cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (ownedGame != null) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open ${entry.title}",
                                    tint = cs.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
