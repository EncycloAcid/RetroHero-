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
     * Maps each console's [RetailCaseStyle] to distinct, authentic physical spine proportions
     * so PS1 jewel cases, SNES cardboard boxes, Genesis clamshells, and DS snap cases look
     * unmistakably distinct standing side-by-side on the shelf.
     */
    fun spineDimensions(systemId: String): SpineDimensions {
        val spec = PhysicalMediaSpec.forSystem(systemId)
        return when (spec.caseStyle) {
            RetailCaseStyle.JEWEL_CASE_CD -> SpineDimensions(widthDp = 30f, heightDp = 128f, topBevelDp = 5f)
            RetailCaseStyle.HANDHELD_SNAP_CASE -> SpineDimensions(widthDp = 32f, heightDp = 124f, topBevelDp = 5f)
            RetailCaseStyle.CARDBOARD_SQUARE -> SpineDimensions(widthDp = 34f, heightDp = 126f, topBevelDp = 6f)
            RetailCaseStyle.CARDBOARD_LANDSCAPE -> SpineDimensions(widthDp = 38f, heightDp = 136f, topBevelDp = 6f)
            RetailCaseStyle.CARDBOARD_VERTICAL -> SpineDimensions(widthDp = 36f, heightDp = 152f, topBevelDp = 6f)
            RetailCaseStyle.CLAMSHELL_PLASTIC -> SpineDimensions(widthDp = 40f, heightDp = 158f, topBevelDp = 7f)
            RetailCaseStyle.KEEP_CASE_DVD -> SpineDimensions(widthDp = 34f, heightDp = 164f, topBevelDp = 6f)
            RetailCaseStyle.ARCADE_SHOCKBOX -> SpineDimensions(widthDp = 44f, heightDp = 166f, topBevelDp = 7f)
        }
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
                        .height(186.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
    val spec = remember(game.systemId) { PhysicalMediaSpec.forSystem(game.systemId) }
    // Same spec (and accent) as the game page's 3D viewer, so the spine art matches it.
    val viewerAccent = game.system?.color ?: MaterialTheme.colorScheme.primary
    val viewerSpec = remember(game.systemId, viewerAccent) { PhysicalMediaSpec.forSystem(game.systemId, viewerAccent) }
    val spineArt = rememberShelfSpineArt(game, viewerSpec, dossierFor)
    val dims = remember(game.systemId) { ShelfLayoutMath.spineDimensions(game.systemId) }
    val baseColor = remember(game.id) { ShelfLayoutMath.spineBaseColor(game, spec) }
    val isLight = remember(baseColor) { ShelfLayoutMath.isLightSpine(baseColor) }
    val textColor = if (isLight) Color(0xFF14171D) else Color(0xFFF5F6FA)
    val accent = game.system?.color ?: spec.spineHeaderColor
    val shortSys = when (game.systemId.lowercase()) {
        "genesis" -> "GEN"
        "dreamcast" -> "DC"
        "saturn" -> "SAT"
        "wonderswan" -> "WS"
        else -> (game.system?.shortName ?: game.systemId).uppercase().take(4)
    }

    val liftY by animateDpAsState(
        targetValue = if (pulled) (-16).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "spineLift",
    )
    val scale by animateFloatAsState(
        targetValue = if (pulled) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "spineScale",
    )

    val corner = when (spec.caseStyle) {
        RetailCaseStyle.JEWEL_CASE_CD -> 3.dp
        RetailCaseStyle.CARDBOARD_LANDSCAPE,
        RetailCaseStyle.CARDBOARD_VERTICAL,
        RetailCaseStyle.CARDBOARD_SQUARE -> 4.dp
        else -> 6.dp
    }

    Box(
        Modifier
            .offset(y = liftY)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = if (pulled) -2.5f else 0f
            }
            .width(dims.widthDp.dp)
            .height(dims.heightDp.dp)
            .shadow(
                elevation = if (pulled) 14.dp else 4.dp,
                shape = RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 2.dp, bottomEnd = 2.dp),
            )
            .clip(RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(baseColor)
            .border(
                width = if (pulled) 1.5.dp else 0.75.dp,
                color = if (pulled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = if (isLight) 0.55f else 0.16f),
                shape = RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 2.dp, bottomEnd = 2.dp),
            )
            .semantics { contentDescription = game.title }
            .clickable(onClick = onClick),
    ) {
        if (spineArt != null) {
            Image(
                bitmap = spineArt,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 3D cylindrical spine bevel shading + subtle plastic/cardboard top edge highlight
        Canvas(Modifier.fillMaxSize()) {
            // Left/right spine curvature lighting
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = if (isLight) 0.35f else 0.20f),
                    0.18f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.42f),
                ),
            )
            // Top edge thickness glint
            drawRect(
                color = Color.White.copy(alpha = if (isLight) 0.45f else 0.18f),
                topLeft = Offset.Zero,
                size = Size(size.width, dims.topBevelDp.dp.toPx() * 0.45f),
            )
            // CD Jewel case clear plastic hinge ribs at top/bottom
            if (spec.caseStyle == RetailCaseStyle.JEWEL_CASE_CD) {
                for (i in 0..3) {
                    val y = 6.dp.toPx() + i * 2.5.dp.toPx()
                    drawLine(Color.White.copy(alpha = 0.24f), Offset(2f, y), Offset(size.width - 2f, y), strokeWidth = 1f)
                }
            }
            // Bottom contact shadow where case meets the shelf
            drawRect(
                brush = Brush.verticalGradient(
                    0.82f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.45f),
                ),
            )
        }

        // Plain printed spine while the real art loads (the art already carries title and logo).
        if (spineArt == null) Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Console color band / logo header at top of spine
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = if (isLight) 0.90f else 0.82f))
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shortSys.take(4),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.3.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            // Vertical rotated game title along the spine
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.2.sp,
                    ),
                    color = textColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .requiredWidth((dims.heightDp - 44f).dp)
                        .rotate(-90f),
                )
            }

            // Tiny bottom console stripe accent
            Box(
                Modifier
                    .width(12.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.78f)),
            )
        }
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
    val spec = remember(game.systemId) { PhysicalMediaSpec.forSystem(game.systemId) }
    // The same box front and disc/cartridge textures the game page's 3D viewer shows.
    var dossier by remember(game.id) { mutableStateOf(GameDossier.localFallback(game)) }
    LaunchedEffect(game.id) {
        withContext(Dispatchers.IO) { dossierFor(game) }?.let { dossier = it }
        loadDossier?.let { load -> runCatching { load(game) }.getOrNull()?.let { dossier = it } }
    }
    val viewerAccent = game.system?.color ?: cs.primary
    val viewerSpec = remember(game.systemId, viewerAccent) { PhysicalMediaSpec.forSystem(game.systemId, viewerAccent) }
    val textures = rememberPhysicalTextures(game, dossier, viewerSpec, null)
    val frontAspect = (textures.front.width.toFloat() / textures.front.height.coerceAtLeast(1)).coerceIn(0.6f, 1.45f)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cs.surfaceContainerHigh,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, cs.primary.copy(alpha = 0.32f), RoundedCornerShape(24.dp)),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Angled 3/4 case with its disc or cartridge sliding out from behind it
            // Wide boxes (SNES, N64) get shorter rather than wider, so the text and buttons keep their room.
            val caseW = (92f * frontAspect).coerceAtMost(96f).dp
            val caseH = (caseW.value / frontAspect).dp
            Box(
                Modifier
                    .width(caseW + 28.dp)
                    .height(96.dp)
                    .clickable(onClickLabel = "Open the 3D case", onClick = onInspect),
            ) {
                Image(
                    bitmap = textures.mediaFront.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(if (viewerSpec.mediaKind.isOpticalDisc) 64.dp else 52.dp)
                        .graphicsLayer { rotationZ = if (viewerSpec.mediaKind.isOpticalDisc) 0f else 7f },
                )
                Image(
                    bitmap = textures.front.asImageBitmap(),
                    contentDescription = "${game.title} case",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(caseW)
                        .height(caseH)
                        .graphicsLayer {
                            rotationY = -14f
                            cameraDistance = 14f * density
                        }
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp)),
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = onInspect,
                        modifier = Modifier.height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Rounded.ViewInAr, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("3D Case", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
