package com.retrohero.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.GameArt
import com.retrohero.app.ui.components.Key
import com.retrohero.app.ui.components.Page
import com.retrohero.app.ui.components.Segmented
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.PixelFont
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.tag
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Slot value meaning "start from the beginning, ignore any auto-save". */
const val FreshStart = -99

private data class GameMilestone(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val unlocked: Boolean,
    val percent: Int,
)

/**
 * Pure Minimal Monochrome Game Detail screen:
 * - Full-bleed box art hero melting straight into flat OLED black or pure white
 * - Minimal floating circular Back / Pin / Shelf controls + crisp monochrome Play / Resume button
 * - Flat hairline telemetry strip, Media Stage, Save States, Specs Table, 5-Axis Game DNA, and Milestones
 */
@Composable
fun GameDetailScreen(
    game: Game,
    currentCore: CoreInfo,
    saveSlots: List<Pair<Int, Long>>,
    onPlay: (slot: Int?) -> Unit,
    onSelectCore: (CoreInfo) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
    stat: PlayStat = PlayStat.None,
    screenshots: List<File> = emptyList(),
    hasCustomCover: Boolean = false,
    onUseAsCover: (File) -> Unit = {},
    onClearCover: () -> Unit = {},
    onShareScreenshot: (File) -> Unit = {},
    pageIndex: Int = 4,
    onDeleteScreenshot: (File) -> Unit = {},
    isFavorite: Boolean = false,
    onShelf: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onAddToCollection: () -> Unit = {},
) {
    val system = game.system ?: return
    val hasAuto = saveSlots.any { it.first == AppPaths.AUTO_SLOT }
    val time = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()) }
    var viewingShot by remember { mutableStateOf<File?>(null) }
    var selectedStageShot by remember(screenshots) { mutableStateOf(screenshots.firstOrNull()) }

    val milestones = remember(game.id, stat, saveSlots.size, screenshots.size, isFavorite, onShelf, system.shortName) {
        buildMilestones(stat, saveSlots.size, screenshots.size, isFavorite, onShelf, system.shortName)
    }
    val unlockedCount = milestones.count { it.unlocked }
    val completionPct = ((unlockedCount * 100f) / milestones.size.coerceAtLeast(1)).roundToInt()
    val region = remember(game.fileName) { parseRegion(game.fileName) }
    val era = remember(system.id) { consoleEra(system.id) }
    val synopsis = remember(game.title, system.name, region, era) {
        buildSynopsis(game, system, region, era)
    }
    val radarValues = remember(game.id, system.id) { computeRadarProfile(game, system) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Tone.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Full-Bleed Box Art Hero Banner melting into flat Tone.Bg -----------------------
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(352.dp),
            ) {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.36f to Color.Transparent,
                                0.72f to Tone.Bg.copy(alpha = 0.88f),
                                1f to Tone.Bg,
                            ),
                        ),
                )

                // Console OS Top Floating Controls
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F131A).copy(alpha = 0.78f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isFavorite) Ascend.accent else Color(0xFF0F131A).copy(alpha = 0.78f))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable(onClick = onToggleFavorite),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Favorite,
                                if (isFavorite) "Pinned" else "Add to Library",
                                tint = if (isFavorite) Ascend.onAccent else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F131A).copy(alpha = 0.78f))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable(onClick = onAddToCollection),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, "Add to shelf", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                // Steam Deck Game Page Hero Dock: floating box-art badge + title + console pill CTA
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        GameArt(
                            game,
                            Modifier
                                .width(78.dp)
                                .height(102.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(2.dp, Ascend.focusRing, RoundedCornerShape(10.dp)),
                            showTag = false,
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text(
                                    "${system.shortName} · $region".uppercase(),
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.08.em),
                                    color = Ascend.accentText,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Ascend.accent.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                                if (stat.lastPlayed > 0) {
                                    Text(
                                        DateUtils.getRelativeTimeSpanString(
                                            stat.lastPlayed,
                                            System.currentTimeMillis(),
                                            DateUtils.MINUTE_IN_MILLIS,
                                        ).toString().lowercase(Locale.getDefault()),
                                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                                        color = Tone.Muted,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Text(
                                game.title,
                                style = TextStyle(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    letterSpacing = (-0.03).em,
                                ),
                                color = Tone.Ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                synopsis,
                                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 17.sp),
                                color = Tone.Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Key(
                            text = if (hasAuto) "Resume game" else "Play game",
                            icon = Icons.Rounded.PlayArrow,
                            primary = true,
                            onClick = { onPlay(null) },
                            modifier = Modifier.weight(1.35f).height(46.dp),
                        )
                        Key(
                            text = if (isFavorite) "Pinned" else "Pin",
                            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.Add,
                            primary = false,
                            onClick = onToggleFavorite,
                            modifier = Modifier.weight(0.85f).height(46.dp),
                        )
                    }

                    if (hasAuto) {
                        Key(
                            text = "Start from the beginning",
                            primary = false,
                            onClick = { onPlay(FreshStart) },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                        )
                    }
                }
            }

            // ---- 4-Cell Flat Hairline Telemetry Strip -------------------------------------------
            TelemetryBentoStrip(
                playTimeValue = if (stat.seconds >= 60) PlayTime.format(stat.seconds) else if (stat.sessions > 0) "< 1m" else estLength(system.id),
                playTimeLabel = if (stat.seconds > 0 || stat.sessions > 0) "PLAY TIME" else "EST. LENGTH",
                achValue = "$completionPct%",
                achLabel = "MILESTONES",
                sessionsValue = stat.sessions.toString(),
                sessionsLabel = "SESSIONS",
                savesValue = "${saveSlots.size + screenshots.size}",
                savesLabel = "SAVES & SHOTS",
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // ---- Media Showcase Stage + Thumbnail Strip -----------------------------------------
            MediaShowcaseSection(
                game = game,
                system = system,
                screenshots = screenshots,
                selectedShot = selectedStageShot,
                onSelectShot = { selectedStageShot = it },
                onInspectShot = { viewingShot = it },
                onPlay = { onPlay(null) },
                hasCustomCover = hasCustomCover,
                onClearCover = onClearCover,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // ---- Save States (if present) -------------------------------------------------------
            if (saveSlots.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Save states (${saveSlots.size})",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Tone.Ink,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(saveSlots, key = { it.first }) { (slot, modified) ->
                            val slotShape = RoundedCornerShape(10.dp)
                            Column(
                                Modifier
                                    .width(152.dp)
                                    .clip(slotShape)
                                    .background(Tone.Surface)
                                    .border(1.dp, Tone.Line, slotShape)
                                    .clickable { onPlay(slot) }
                                    .padding(12.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (slot == AppPaths.AUTO_SLOT) Icons.Rounded.History else Icons.Rounded.Save,
                                        null,
                                        tint = if (slot == AppPaths.AUTO_SLOT) Ascend.accent else Tone.Ink,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        if (slot == AppPaths.AUTO_SLOT) "Auto" else "Slot $slot",
                                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                        color = Tone.Muted,
                                    )
                                }
                                Text(
                                    if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                                    color = Tone.Ink,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                                Text(
                                    time.format(Date(modified)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Tone.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // ---- Metadata Table Card ------------------------------------------------------------
            MetadataSpecTable(
                game = game,
                system = system,
                region = region,
                era = era,
                currentCore = currentCore,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ---- 5-Axis Pentagon Radar Chart Card (GAME PROFILE) --------------------------------
            GameProfileRadarCard(
                values = radarValues,
                systemName = system.shortName,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ---- YOUR ACHIEVEMENTS Progression Card ---------------------------------------------
            AchievementsCard(
                milestones = milestones,
                unlockedCount = unlockedCount,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ---- Editorial Story Highlight Banner -----------------------------------------------
            EditorialFeatureBanner(
                game = game,
                system = system,
                screenshot = screenshots.lastOrNull(),
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ---- Emulator Core Selector (if multiple cores available) ---------------------------
            if (system.cores.size > 1) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Emulator engine",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = Tone.Ink,
                    )
                    var selected by remember { mutableIntStateOf(system.cores.indexOf(currentCore).coerceAtLeast(0)) }
                    Segmented(
                        options = system.cores.map { it.displayName },
                        selected = selected,
                        onSelect = { i ->
                            selected = i
                            onSelectCore(system.cores[i])
                        },
                    )
                    Text(
                        "${system.cores[selected].license} license · default engine for ${system.shortName} games",
                        style = MaterialTheme.typography.bodySmall,
                        color = Tone.Muted,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // ---- Delete Local Download (if applicable) ------------------------------------------
            if (onDelete != null) {
                var confirm by remember { mutableStateOf(false) }
                Text(
                    if (confirm) "Tap again to delete this download" else "Delete download",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .clip(CircleShape)
                        .clickable { if (confirm) onDelete() else confirm = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    viewingShot?.let { shot ->
        ScreenshotDialog(
            shot,
            onDismiss = { viewingShot = null },
            onUseAsCover = { onUseAsCover(shot); viewingShot = null },
            onShare = { onShareScreenshot(shot) },
            onDelete = { onDeleteScreenshot(shot); viewingShot = null },
        )
    }
}

// ---- 4-Cell Translucent Glass Telemetry Strip ---------------------------------------------------

@Composable
private fun TelemetryBentoStrip(
    playTimeValue: String,
    playTimeLabel: String,
    achValue: String,
    achLabel: String,
    sessionsValue: String,
    sessionsLabel: String,
    savesValue: String,
    savesLabel: String,
    modifier: Modifier = Modifier,
) {
    val stripShape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(stripShape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, stripShape)
            .height(IntrinsicSize.Min)
            .padding(vertical = 12.dp),
    ) {
        TelemetryCell(playTimeValue, Icons.Rounded.Schedule, playTimeLabel, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(Tone.Line))
        TelemetryCell(achValue, Icons.Rounded.Verified, achLabel, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(Tone.Line))
        TelemetryCell(sessionsValue, Icons.Rounded.SportsEsports, sessionsLabel, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(Tone.Line))
        TelemetryCell(savesValue, Icons.Rounded.Save, savesLabel, Modifier.weight(1f))
    }
}

@Composable
private fun TelemetryCell(
    value: String,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.02).em,
                ),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(icon, null, tint = Tone.Muted, modifier = Modifier.size(13.dp))
        }
        Text(
            label,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 8.5.sp,
                letterSpacing = 0.04.em,
            ),
            color = Tone.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- Widescreen Media Showcase Stage + Thumbnail Strip ------------------------------------------

@Composable
private fun MediaShowcaseSection(
    game: Game,
    system: GameSystem,
    screenshots: List<File>,
    selectedShot: File?,
    onSelectShot: (File?) -> Unit,
    onInspectShot: (File) -> Unit,
    onPlay: () -> Unit,
    hasCustomCover: Boolean,
    onClearCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stageShape = RoundedCornerShape(12.dp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9.2f)
                .clip(stageShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.Line, stageShape)
                .clickable {
                    if (selectedShot != null) onInspectShot(selectedShot) else onPlay()
                },
        ) {
            if (selectedShot != null) {
                AsyncImage(
                    model = selectedShot,
                    contentDescription = "Screenshot",
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Black.copy(alpha = 0.30f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    if (selectedShot != null) "CAPTURE" else "PREVIEW",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.08.em),
                    color = Color.White,
                )
            }

            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, "Launch", tint = Color(0xFF0A0A0C), modifier = Modifier.size(26.dp))
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${system.name} Classic",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${system.manufacturer} · ${system.shortName}",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "thumb_cover") {
                val active = selectedShot == null
                val thumbShape = RoundedCornerShape(8.dp)
                Box(
                    Modifier
                        .width(100.dp)
                        .height(60.dp)
                        .clip(thumbShape)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) Tone.Ink else Tone.Line,
                            shape = thumbShape,
                        )
                        .clickable { onSelectShot(null) },
                ) {
                    GameArt(game, Modifier.fillMaxSize(), showTag = false)
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text("Cover", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp), color = Color.White)
                    }
                }
            }
            items(screenshots, key = { it.path }) { shot ->
                val active = selectedShot?.path == shot.path
                val thumbShape = RoundedCornerShape(8.dp)
                Box(
                    Modifier
                        .width(100.dp)
                        .height(60.dp)
                        .clip(thumbShape)
                        .background(Tone.Surface)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) Tone.Ink else Tone.Line,
                            shape = thumbShape,
                        )
                        .clickable {
                            if (active) onInspectShot(shot) else onSelectShot(shot)
                        },
                ) {
                    AsyncImage(
                        model = shot,
                        contentDescription = "Screenshot",
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (hasCustomCover) {
            Text(
                "Restore original cover art",
                style = MaterialTheme.typography.labelLarge,
                color = Tone.Ink,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClearCover)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

// ---- Flat Hairline Metadata Spec Table ----------------------------------------------------------

@Composable
private fun MetadataSpecTable(
    game: Game,
    system: GameSystem,
    region: String,
    era: String,
    currentCore: CoreInfo,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape),
    ) {
        SpecRow("Console", system.name)
        HorizontalDivider(color = Tone.Line)
        SpecRow("Platform", system.manufacturer)
        HorizontalDivider(color = Tone.Line)
        SpecRow("Era & region", "$era · $region")
        HorizontalDivider(color = Tone.Line)
        SpecRow("Engine", "${currentCore.displayName} (${currentCore.license})")
        HorizontalDivider(color = Tone.Line)
        SpecRow(
            "File",
            if (game.size > 0) "${game.fileName} (${formatBytes(game.size)})" else game.fileName,
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = Tone.Muted,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            value,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = Tone.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---- 5-Axis Pentagon Game Profile Radar Chart Card ----------------------------------------------

@Composable
private fun GameProfileRadarCard(
    values: List<Float>,
    systemName: String,
    modifier: Modifier = Modifier,
) {
    val axes = listOf("ADVENTURE", "ACTION", "Reflex", "STRATEGY", "RPG")
    val lineColor = Tone.Line
    val accentColor = Ascend.accent
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Game DNA", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Tone.Ink)
            Text(systemName, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp), color = Tone.Muted)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(225.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(165.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension * 0.45f
                val count = 5

                fun vertex(i: Int, fraction: Float): Offset {
                    val angle = -PI / 2.0 + (2.0 * PI * i / count)
                    return Offset(
                        cx + (maxR * fraction * cos(angle)).toFloat(),
                        cy + (maxR * fraction * sin(angle)).toFloat(),
                    )
                }

                listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { ring ->
                    val p = Path()
                    for (i in 0 until count) {
                        val pt = vertex(i, ring)
                        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                    }
                    p.close()
                    drawPath(p, color = lineColor, style = Stroke(width = 1.dp.toPx()))
                }

                for (i in 0 until count) {
                    val outer = vertex(i, 1f)
                    drawLine(lineColor, Offset(cx, cy), outer, strokeWidth = 1.dp.toPx())
                }

                val dataPath = Path()
                for (i in 0 until count) {
                    val v = values.getOrElse(i) { 0.65f }.coerceIn(0.2f, 1f)
                    val pt = vertex(i, v)
                    if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
                }
                dataPath.close()
                drawPath(dataPath, color = accentColor.copy(alpha = 0.16f))
                drawPath(dataPath, color = accentColor, style = Stroke(width = 1.8.dp.toPx()))

                for (i in 0 until count) {
                    val v = values.getOrElse(i) { 0.65f }.coerceIn(0.2f, 1f)
                    val pt = vertex(i, v)
                    drawCircle(accentColor, radius = 3.dp.toPx(), center = pt)
                }
            }

            ArchiveLabel(axes[0], Modifier.align(Alignment.TopCenter), color = Tone.Muted)
            ArchiveLabel(axes[1], Modifier.align(Alignment.CenterEnd).padding(bottom = 28.dp), color = Tone.Muted)
            ArchiveLabel(axes[2].uppercase(), Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 4.dp), color = Tone.Muted)
            ArchiveLabel(axes[3], Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 4.dp), color = Tone.Muted)
            ArchiveLabel(axes[4], Modifier.align(Alignment.CenterStart).padding(bottom = 28.dp), color = Tone.Muted)
        }
    }
}

// ---- YOUR ACHIEVEMENTS Progression Card ---------------------------------------------------------

@Composable
private fun AchievementsCard(
    milestones: List<GameMilestone>,
    unlockedCount: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Milestones", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Tone.Ink)
            Text(
                "$unlockedCount / ${milestones.size}",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = Tone.Muted,
            )
        }
        HorizontalDivider(color = Tone.Line)

        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            milestones.forEachIndexed { idx, m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (m.unlocked) Ascend.accent else Tone.Raised)
                            .border(1.dp, if (m.unlocked) Color.Transparent else Tone.Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            m.icon,
                            null,
                            tint = if (m.unlocked) Ascend.onAccent else Tone.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.title,
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = Tone.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            m.subtitle,
                            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                            color = Tone.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "${m.percent}%",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = if (m.unlocked) Tone.Ink else Tone.Muted,
                        )
                        Icon(
                            if (m.unlocked) Icons.Rounded.Check else Icons.Rounded.EmojiEvents,
                            null,
                            tint = if (m.unlocked) Tone.Ink else Tone.Muted,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                if (idx < milestones.lastIndex) {
                    HorizontalDivider(color = Tone.Line)
                }
            }
        }
    }
}

// ---- Editorial Story Highlight Banner -----------------------------------------------------------

@Composable
private fun EditorialFeatureBanner(
    game: Game,
    system: GameSystem,
    screenshot: File?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 8.5f)
                .clip(RoundedCornerShape(8.dp))
                .border(0.75.dp, Tone.Line, RoundedCornerShape(8.dp)),
        ) {
            if (screenshot != null) {
                AsyncImage(
                    model = screenshot,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                GameArt(game, Modifier.fillMaxSize(), showTag = false)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF060912).copy(alpha = 0.72f)),
                        ),
                    ),
            )
            Text(
                "Enhanced on RetroHero",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
        Text(
            "Explore ${game.title} on ${system.shortName}",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
            color = Tone.Ink,
        )
        Text(
            "Experience ${game.title} with instant save-states, frame-accurate rewind, audio-reactive HD haptics, live on-device screen translation, and custom Delta controller skins.",
            style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
            color = Tone.Muted,
        )
    }
}

// ---- Helpers & Dialogs --------------------------------------------------------------------------

private fun buildMilestones(
    stat: PlayStat,
    saveCount: Int,
    shotCount: Int,
    isFavorite: Boolean,
    onShelf: Boolean,
    shortSystem: String,
): List<GameMilestone> {
    val firstBoot = stat.sessions >= 1 || stat.seconds > 0 || saveCount > 0
    val hasSave = saveCount > 0
    val hasShot = shotCount > 0
    val playPct = ((stat.seconds / 900f) * 100f).roundToInt().coerceIn(if (firstBoot) 10 else 0, 100)
    val sessionPct = ((stat.sessions / 5f) * 100f).roundToInt().coerceIn(0, 100)

    return listOf(
        GameMilestone(
            title = "First Boot",
            subtitle = "Launch and play your first session",
            icon = Icons.Rounded.SportsEsports,
            unlocked = firstBoot,
            percent = if (firstBoot) 100 else 0,
        ),
        GameMilestone(
            title = "Time Keeper",
            subtitle = "Create an auto-save or manual save state",
            icon = Icons.Rounded.Save,
            unlocked = hasSave,
            percent = if (hasSave) 100 else 0,
        ),
        GameMilestone(
            title = "The Photographer",
            subtitle = "Capture an in-game screenshot from the pause menu",
            icon = Icons.Rounded.CameraAlt,
            unlocked = hasShot,
            percent = if (hasShot) 100 else 0,
        ),
        GameMilestone(
            title = "The Curator",
            subtitle = "Pin this title to Favourites or a custom shelf",
            icon = Icons.Rounded.Favorite,
            unlocked = isFavorite || onShelf,
            percent = if (isFavorite || onShelf) 100 else 0,
        ),
        GameMilestone(
            title = "Arcade Regular",
            subtitle = "Play for at least 15 minutes across sessions",
            icon = Icons.Rounded.Schedule,
            unlocked = stat.seconds >= 900,
            percent = playPct,
        ),
        GameMilestone(
            title = "$shortSystem Veteran",
            subtitle = "Complete 5 play sessions on this cartridge",
            icon = Icons.Rounded.AutoAwesome,
            unlocked = stat.sessions >= 5,
            percent = sessionPct,
        ),
    )
}

private fun parseRegion(fileName: String): String {
    val lower = fileName.lowercase(Locale.US)
    return when {
        "(usa" in lower || "(u)" in lower -> "USA (NTSC)"
        "(europe" in lower || "(e)" in lower -> "Europe (PAL)"
        "(japan" in lower || "(j)" in lower -> "Japan (NTSC-J)"
        "(world" in lower || "(w)" in lower -> "World Release"
        else -> "Universal ROM"
    }
}

private fun consoleEra(systemId: String): String = when (systemId) {
    "nes", "sms", "atari2600" -> "8-Bit Era (1985)"
    "gb", "gbc", "gamegear", "lynx" -> "Handheld Classic (1990s)"
    "snes", "genesis", "pce", "segacd" -> "16-Bit Golden Era (1992)"
    "psx", "n64", "saturn", "sega32x" -> "32/64-Bit 3D Era (1996)"
    "gba", "nds", "psp" -> "Modern Portable (2002)"
    else -> "Arcade Classic"
}

private fun estLength(systemId: String): String = when (systemId) {
    "psx", "nds", "snes" -> "18hs"
    "gba", "gbc", "n64" -> "12hs"
    else -> "6hs"
}

private fun buildSynopsis(game: Game, system: GameSystem, region: String, era: String): String =
    "${game.title} for ${system.name} ($era · $region). Precision-emulated with hardware save states, custom controller skins, and low-latency audio."

private fun computeRadarProfile(game: Game, system: GameSystem): List<Float> {
    val h = (game.title.hashCode() and 0x7fffffff)
    val base = when (system.id) {
        "nes", "sms", "arcade", "gb" -> floatArrayOf(0.68f, 0.92f, 0.88f, 0.48f, 0.38f)
        "snes", "gba", "nds", "psx" -> floatArrayOf(0.88f, 0.78f, 0.64f, 0.74f, 0.85f)
        "genesis", "pce", "gamegear" -> floatArrayOf(0.72f, 0.94f, 0.90f, 0.55f, 0.45f)
        else -> floatArrayOf(0.76f, 0.82f, 0.75f, 0.65f, 0.62f)
    }
    return List(5) { idx ->
        val jitter = (((h shr (idx * 3)) and 0x7) - 3) * 0.03f
        (base[idx] + jitter).coerceIn(0.32f, 0.96f)
    }
}

@Composable
private fun ScreenshotDialog(shot: File, onDismiss: () -> Unit, onUseAsCover: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(Ascend.Radius.Card))
                .background(Tone.Surface)
                .border(1.dp, Tone.Line, RoundedCornerShape(Ascend.Radius.Card))
                .padding(16.dp),
        ) {
            AsyncImage(
                model = shot,
                contentDescription = "Screenshot",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Ascend.Radius.Cover))
                    .border(1.dp, Tone.Line, RoundedCornerShape(Ascend.Radius.Cover)),
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShotAction(Icons.Rounded.Image, "Use as cover", Modifier.weight(1f), onUseAsCover)
                ShotAction(Icons.Rounded.Share, "Share", Modifier.weight(1f), onShare)
                ShotAction(Icons.Rounded.Delete, "Delete", Modifier.weight(1f), onDelete)
            }
        }
    }
}

@Composable
private fun ShotAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(Ascend.Radius.Control))
            .background(Tone.Raised)
            .border(1.dp, Tone.Line, RoundedCornerShape(Ascend.Radius.Control))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Tone.Ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Tone.Ink, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    else -> "${(bytes / 1024).coerceAtLeast(1)} KB"
}
