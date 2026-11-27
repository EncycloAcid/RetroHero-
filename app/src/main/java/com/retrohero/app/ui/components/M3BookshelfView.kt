package com.retrohero.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.retrohero.app.data.GameDossier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PhysicalMediaSpec
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.RetailCaseStyle

/** Tactile material theme for the physical Bookshelf ledges. */
enum class ShelfMaterial(val label: String) {
    WALNUT("Walnut Wood"),
    SMOKED_GLASS("Smoked Glass"),
}

/** Pure, unit-testable dimensions for each physical game spine standing on the shelf. */
data class SpineDimensions(
    val widthDp: Float,
    val heightDp: Float,
    val topBevelDp: Float,
)

object ShelfLayoutMath {
    /**
     * Spine-fit framing for the shared 3D renderer, as fractions of the slot height: the case fills
     * 94% of its slot and its base sits on the slot's bottom edge (the shelf), with headroom above
     * for the top face seen from slightly above.
     */
    const val SPINE_FIT_HALF_HEIGHT = 0.47f
    const val SPINE_FIT_CENTER = 0.525f

    /** Slot height that makes a case of [caseHeightDp] stand exactly that tall on the shelf. */
    fun slotHeightDp(caseHeightDp: Float): Float = caseHeightDp / (2f * SPINE_FIT_HALF_HEIGHT)

    /** Shelf cases face the room spine-first, the same angle as the game page's "Spine" view. */
    const val SPINE_YAW = 88f
    const val SPINE_PITCH = -4f
    /** A pulled case turns a little toward you, showing a sliver of its front. */
    const val PULLED_YAW = 78f

    /** Width / height of a case's spine face in the 3D model (depth relative to the case's long side). */
    fun spineFaceRatio(spec: PhysicalMediaSpec, boxAspect: Float): Float =
        (spec.caseStyle.depthRatio * maxOf(boxAspect, 1f)).coerceIn(0.05f, 0.4f)

    /** Aspect of the generated box front, which the 3D viewer uses when there's no real cover. */
    fun proceduralFrontAspect(spec: PhysicalMediaSpec): Float {
        val w = if (spec.defaultBoxAspect > 1.12f) 740 else 540
        val h = Math.round(w / spec.defaultBoxAspect).coerceIn(480, 860)
        return w.toFloat() / h
    }

    /** How wide a shelf slot must be for a case [caseHeightDp] tall standing at [yawDeg], with 1 dp of air either side. */
    fun spineSlotWidthDp(spec: PhysicalMediaSpec, boxAspect: Float, caseHeightDp: Float, yawDeg: Float = SPINE_YAW): Float {
        val halfH = caseHeightDp / 2f
        val halfW = halfH * boxAspect
        val halfD = maxOf(halfW, halfH) * spec.caseStyle.depthRatio
        val rad = Math.toRadians(yawDeg.toDouble())
        val projected = 2f * (halfD * kotlin.math.abs(kotlin.math.sin(rad)).toFloat() + halfW * kotlin.math.abs(kotlin.math.cos(rad)).toFloat())
        return projected + 2f
    }

    /** Real retail case heights in millimetres (North American releases), standing upright. */
    fun caseHeightMm(systemId: String): Float = when (systemId.lowercase()) {
        "nes" -> 180f
        "snes", "vb" -> 133f
        "n64" -> 131f
        "gb", "gbc" -> 116f
        "gba" -> 125f
        "nds", "3ds" -> 136f
        "switch" -> 170f
        "psp" -> 178f
        "gc", "wii", "wiiu", "ps2" -> 190f
        "genesis", "32x", "sms" -> 190f
        "sg1000" -> 180f
        "gg" -> 142f
        "segacd", "saturn" -> 220f
        "psx", "dreamcast" -> 125f
        else -> when (PhysicalMediaSpec.forSystem(systemId).caseStyle) {
            RetailCaseStyle.CARDBOARD_VERTICAL -> 180f
            RetailCaseStyle.CARDBOARD_LANDSCAPE -> 133f
            RetailCaseStyle.CARDBOARD_SQUARE -> 120f
            RetailCaseStyle.CLAMSHELL_PLASTIC -> 190f
            RetailCaseStyle.JEWEL_CASE_CD -> 125f
            RetailCaseStyle.KEEP_CASE_DVD -> 190f
            RetailCaseStyle.HANDHELD_SNAP_CASE -> 136f
            RetailCaseStyle.ARCADE_SHOCKBOX -> 205f
        }
    }

    /** One scale for every case, so they stand at their true relative sizes (220 mm fills the row). */
    const val DP_PER_MM = 0.80f

    /**
     * Each console's case as it stands on the shelf: its true height, and the thickness of its spine
     * in the same 3D model the game page uses (depth relative to the case's long side).
     */
    fun spineDimensions(systemId: String): SpineDimensions {
        val spec = PhysicalMediaSpec.forSystem(systemId)
        val heightDp = caseHeightMm(systemId) * DP_PER_MM
        val aspect = proceduralFrontAspect(spec)
        val depthDp = heightDp * maxOf(aspect, 1f) * spec.caseStyle.depthRatio
        return SpineDimensions(widthDp = depthDp, heightDp = heightDp, topBevelDp = 5f)
    }

    /** Chunks a game list into clean physical shelf rows (default 8 cases per shelf). */
    fun chunkIntoShelves(games: List<Game>, perShelf: Int = 8): List<List<Game>> {
        if (games.isEmpty()) return emptyList()
        return games.chunked(perShelf.coerceAtLeast(1))
    }

    /** Deterministic rich spine base color per game so every spine on the shelf is immediately recognizable. */
    fun spineBaseColor(game: Game, spec: PhysicalMediaSpec): Color {
        val accent = game.system?.color ?: spec.spineHeaderColor
        val hash = (game.title.hashCode() and 0x7FFFFFFF)
        val tintMix = (hash % 5) * 0.09f
        val darkTone = lerp(Color(0xFF1A1C22), accent, 0.30f + tintMix)
        return when (spec.caseStyle) {
            RetailCaseStyle.HANDHELD_SNAP_CASE -> when (game.systemId.lowercase()) {
                "nds", "3ds" -> lerp(Color(0xFFECEFF4), accent, 0.14f)
                "switch" -> Color(0xFFD5001C)
                else -> darkTone
            }
            RetailCaseStyle.JEWEL_CASE_CD -> lerp(Color(0xFF14161B), accent, 0.22f + tintMix)
            else -> darkTone
        }
    }

    fun isLightSpine(color: Color): Boolean {
        val luma = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return luma > 0.58f
    }
}

/**
 * Interactive 3D Bookshelf / Spine-View Library section rendered inside the Games tab.
 * Lines games up by their console-authentic 3D spines resting on sculpted walnut wood or smoked
 * glass ledges. Tapping any spine slides the physical case forward off the shelf with a live 3/4
 * case spotlight and 1-tap Play / Inspect 3D Case actions.
 */
@Composable
fun M3BookshelfSection(
    games: List<Game>,
    stats: Map<String, PlayStat>,
    onOpenDetail: (Game) -> Unit,
    onQuickPlay: (Game) -> Unit,
    modifier: Modifier = Modifier,
    /** Cached LaunchBox data for a game (may read disk; called off the main thread). */
    dossierFor: (Game) -> GameDossier? = { null },
    /** Fetches LaunchBox data for the pulled-out case, as the game page does. */
    loadDossier: (suspend (Game) -> GameDossier)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val view = LocalView.current
    var material by remember { mutableStateOf(ShelfMaterial.WALNUT) }
    var pulledGameId by remember(games.size) { mutableStateOf<String?>(null) }
    val shelves = remember(games) { ShelfLayoutMath.chunkIntoShelves(games, perShelf = 8) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Shelf material switcher header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Physical Shelf",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurface,
                )
                Text(
                    text = "Tap any spine to slide the case off the shelf",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ShelfMaterial.entries.forEach { mat ->
                    val active = material == mat
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (active) cs.primaryContainer else Color.Transparent)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                material = mat
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = if (mat == ShelfMaterial.WALNUT) "Wood" else "Glass",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Physical Shelf Rows
        shelves.forEachIndexed { shelfIdx, rowGames ->
            val pulledInThisRow = rowGames.firstOrNull { it.id == pulledGameId }
            Column(Modifier.fillMaxWidth()) {
                // Horizontally scrollable row of physical spines resting flush on the ledge
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp)
                        // Tall enough for this shelf's tallest case at its true size, plus room to lift one out.
                        .height(
                            (rowGames.maxOf { ShelfLayoutMath.slotHeightDp(ShelfLayoutMath.spineDimensions(it.systemId).heightDp) } + 18f).dp,
                        ),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    rowGames.forEach { game ->
                        val isPulled = game.id == pulledGameId
                        ShelfSpineCase(
                            game = game,
                            pulled = isPulled,
                            dossierFor = dossierFor,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                pulledGameId = if (isPulled) null else game.id
                            },
                        )
                    }
                }

                // Sculpted Walnut Wood or Smoked Glass Shelf Ledge + contact shadow
                ShelfLedgePlank(material = material)

                // Pulled-out 3/4 Case Spotlight Panel right beneath the active shelf
                AnimatedVisibility(
                    visible = pulledInThisRow != null,
                    enter = expandVertically(spring(dampingRatio = 0.78f, stiffness = 360f)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    pulledInThisRow?.let { g ->
                        PulledCaseSpotlight(
                            game = g,
                            stat = stats[g.id],
                            dossierFor = dossierFor,
                            loadDossier = loadDossier,
                            onPlay = { onQuickPlay(g) },
                            onInspect = { onOpenDetail(g) },
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfSpineCase(
    game: Game,
    pulled: Boolean,
    onClick: () -> Unit,
    dossierFor: (Game) -> GameDossier?,
) {
    val dims = remember(game.systemId) { ShelfLayoutMath.spineDimensions(game.systemId) }
    // Same spec (and accent) as the game page's 3D viewer, so the case is the same model.
    val viewerAccent = game.system?.color ?: MaterialTheme.colorScheme.primary
    val viewerSpec = remember(game.systemId, viewerAccent) { PhysicalMediaSpec.forSystem(game.systemId, viewerAccent) }
    val art = rememberShelfCaseArt(game, viewerSpec, dossierFor)

    val yaw by animateFloatAsState(
        targetValue = if (pulled) ShelfLayoutMath.PULLED_YAW else ShelfLayoutMath.SPINE_YAW,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 300f),
        label = "spineYaw",
    )
    val liftY by animateDpAsState(
        targetValue = if (pulled) (-16).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "spineLift",
    )
    val aspect = art?.boxAspect ?: ShelfLayoutMath.proceduralFrontAspect(viewerSpec)
    // The slot follows the case as it turns, so neighbours make room for a pulled case.
    val slotW = ShelfLayoutMath.spineSlotWidthDp(viewerSpec, aspect, dims.heightDp, yaw)

    Box(
        Modifier
            .offset(y = liftY)
            .width(slotW.dp)
            .height(ShelfLayoutMath.slotHeightDp(dims.heightDp).dp)
            .semantics {
                contentDescription = game.title
                selected = pulled
            }
            .clickable(onClick = onClick),
    ) {
        if (art != null) {
            Physical3dStageCanvas(
                subject = PhysicalViewerSubject.RETAIL_BOX,
                spec = viewerSpec,
                textures = art.textures,
                boxAspect = art.boxAspect,
                yawDeg = yaw,
                pitchDeg = ShelfLayoutMath.SPINE_PITCH,
                spineFit = true,
                floorShadow = false,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FlatSpinePlaceholder(game, viewerSpec, Modifier.fillMaxSize())
        }
    }
}

/** A plain printed spine shown for the moment before the case's 3D textures are ready. */
@Composable
private fun FlatSpinePlaceholder(game: Game, spec: PhysicalMediaSpec, modifier: Modifier) {
    val dims = remember(game.systemId) { ShelfLayoutMath.spineDimensions(game.systemId) }
    val baseColor = remember(game.id) { ShelfLayoutMath.spineBaseColor(game, spec) }
    val isLight = remember(baseColor) { ShelfLayoutMath.isLightSpine(baseColor) }
    val textColor = if (isLight) Color(0xFF14171D) else Color(0xFFF5F6FA)
    val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
    Box(
        modifier
            .padding(top = (ShelfLayoutMath.slotHeightDp(dims.heightDp) - dims.heightDp).dp, start = 2.dp, end = 2.dp)
            .clip(shape)
            .background(baseColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = game.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .requiredWidth((dims.heightDp - 24f).dp)
                .rotate(90f),
        )
    }
}

@Composable
private fun ShelfLedgePlank(material: ShelfMaterial) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(22.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val plankH = h * 0.62f
                // Soft cast shadow beneath the shelf plank
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.46f),
                        1f to Color.Transparent,
                        startY = plankH,
                        endY = h,
                    ),
                    topLeft = Offset(8.dp.toPx(), plankH - 1f),
                    size = Size(w - 16.dp.toPx(), h - plankH + 1f),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                if (material == ShelfMaterial.WALNUT) {
                    // Rich warm walnut wood plank
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0xFF795548),
                            0.22f to Color(0xFF5D4037),
                            0.85f to Color(0xFF3E2723),
                            1f to Color(0xFF281814),
                        ),
                        topLeft = Offset.Zero,
                        size = Size(w, plankH),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                    )
                    // Subtle horizontal woodgrain fibers
                    for (line in 1..3) {
                        val y = plankH * (line * 0.24f)
                        drawLine(
                            color = Color(0xFF2B1914).copy(alpha = 0.28f),
                            start = Offset(10f, y),
                            end = Offset(w - 10f, y),
                            strokeWidth = 1.2f,
                        )
                    }
                    // Top bevel highlight
                    drawLine(
                        color = Color(0xFFD7CCC8).copy(alpha = 0.35f),
                        start = Offset(6f, 1.2f),
                        end = Offset(w - 6f, 1.2f),
                        strokeWidth = 1.4f,
                    )
                } else {
                    // Sleek smoked OLED glass ledge
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0xFF455A64).copy(alpha = 0.65f),
                            0.35f to Color(0xFF263238).copy(alpha = 0.78f),
                            1f to Color(0xFF10161B).copy(alpha = 0.88f),
                        ),
                        topLeft = Offset.Zero,
                        size = Size(w, plankH),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                    )
                    // Crisp glass specular top rim
                    drawLine(
                        color = Color(0xFFB0BEC5).copy(alpha = 0.58f),
                        start = Offset(4f, 1.2f),
                        end = Offset(w - 4f, 1.2f),
                        strokeWidth = 1.8f,
                    )
                }
            },
    )
}

@Composable
private fun PulledCaseSpotlight(
    game: Game,
    stat: PlayStat?,
    onPlay: () -> Unit,
    onInspect: () -> Unit,
    dossierFor: (Game) -> GameDossier?,
    loadDossier: (suspend (Game) -> GameDossier)?,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    // Same LaunchBox data the game page loads, so its 3D viewer shows the same case and disc.
    var dossier by remember(game.id) { mutableStateOf(GameDossier.localFallback(game)) }
    LaunchedEffect(game.id) {
        withContext(Dispatchers.IO) { dossierFor(game) }?.let { dossier = it }
        loadDossier?.let { load -> runCatching { load(game) }.getOrNull()?.let { dossier = it } }
    }
    val spec = remember(game.systemId) { PhysicalMediaSpec.forSystem(game.systemId) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // The game page's own 3D case & disc viewer: drag 360°, double-tap to flip, Case / Disc,
        // Zoom. The case turns out of the shelf from its spine to the 3/4 view.
        M3PhysicalBoxViewerSection(
            game = game,
            dossier = dossier,
            introFromYawDeg = ShelfLayoutMath.SPINE_YAW,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildString {
                    append(game.system?.shortName ?: game.systemId.uppercase())
                    append(" · ")
                    append(spec.caseStyle.label)
                    if (stat != null && stat.seconds > 0) {
                        append(" · ")
                        append(PlayTime.format(stat.seconds).replace(' ', '\u00A0')) // keep "2 h 3 min" together
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = onInspect,
                modifier = Modifier.height(40.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            ) {
                Text("Details", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            }
            Button(
                onClick = onPlay,
                modifier = Modifier.height(40.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Play", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            }
        }
    }
}
