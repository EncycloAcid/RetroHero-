package com.retrohero.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Downloader
import com.retrohero.app.data.Game
import com.retrohero.app.data.Homebrew
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.ExpressiveConnectedBar
import com.retrohero.app.ui.components.ExpressivePosterArt
import com.retrohero.app.ui.components.ExpressiveShapeBadge
import com.retrohero.app.ui.components.ExpressiveSplitButton
import com.retrohero.app.ui.components.IosInsetRow
import com.retrohero.app.ui.components.gameAccentColor
import com.retrohero.app.ui.components.systemTintColor
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.polygon

private val ShelfStatusIds = listOf("playing", "completed", "backlog")
private val ShelfStatusLabels = listOf("Playing", "Completed", "Backlog")

@Composable
internal fun GameDetailSheet(
    game: Game,
    stat: PlayStat?,
    isFavorite: Boolean,
    shelfStatus: String,
    saveSlots: List<Pair<Int, Long>>,
    currentCore: CoreInfo?,
    onSetShelfStatus: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onPlay: (Int?) -> Unit,
    onSelectCore: (CoreInfo) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val glow = remember(game.id) { gameAccentColor(game) }
    val sysTint = remember(game.systemId) { systemTintColor(game.systemId) }
    val sysPoly = game.system?.polygon ?: MaterialShapes.Cookie6Sided
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val cores = game.system?.cores.orEmpty()
    val hasAutoSave = saveSlots.any { it.first == AppPaths.AUTO_SLOT }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(sheetShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.glassBorder, sheetShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            // Soft ambient artwork halo bleeding across top of sheet
            Canvas(Modifier.fillMaxWidth().height(190.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(glow.copy(alpha = if (Tone.isDark) 0.28f else 0.16f), Color.Transparent)),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.28f, size.height * 0.2f),
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // iOS grabber handle + close button row
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .width(38.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(Tone.Faint.copy(alpha = 0.55f)),
                    )
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Tone.Raised)
                            .semantics { contentDescription = "Close details" }
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = Tone.Muted, modifier = Modifier.size(17.dp))
                    }
                }

                // Header: Squircle Poster + Editorial Title & Console Organic Pill
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val artShape = RoundedCornerShape(22.dp)
                    ExpressivePosterArt(
                        game = game,
                        modifier = Modifier
                            .width(102.dp)
                            .aspectRatio(0.78f)
                            .shadow(14.dp, artShape, ambientColor = glow.copy(alpha = 0.4f), spotColor = glow.copy(alpha = 0.5f))
                            .clip(artShape)
                            .border(1.dp, Tone.glassBorder, artShape),
                    )

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(sysTint.copy(alpha = if (Tone.isDark) 0.20f else 0.14f))
                                .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ExpressiveShapeBadge(polygon = sysPoly, containerColor = sysTint, size = 18.dp) {
                                Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                            Text(
                                text = (game.system?.shortName ?: game.systemId).uppercase(),
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.08.em),
                                color = sysTint,
                                maxLines = 1,
                            )
                        }

                        Text(
                            text = game.title,
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 25.sp, letterSpacing = (-0.025).em),
                            color = Tone.Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val statText = if (stat != null && stat.seconds > 0) {
                            "${PlayTime.format(stat.seconds)} played · ${stat.sessions} ${if (stat.sessions == 1) "session" else "sessions"}"
                        } else {
                            game.system?.name ?: "Ready to launch"
                        }
                        Text(
                            text = statText,
                            style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                            color = Tone.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // M3 Expressive Split Play CTA + Morphing Favorite Button
                ExpressiveSplitButton(
                    label = if (hasAutoSave) "Resume Game" else "Play Game",
                    icon = Icons.Rounded.PlayArrow,
                    onPrimaryClick = { onPlay(null) },
                    secondaryIcon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    secondaryDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    onSecondaryClick = onToggleFavorite,
                    secondaryHighlighted = isFavorite,
                    height = 50.dp,
                )

                // Save States quick-load pills
                if (saveSlots.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SAVE STATES",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.12.em),
                            color = Tone.Muted,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(saveSlots, key = { it.first }) { (slot, _) ->
                                Row(
                                    Modifier
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(Tone.Raised)
                                        .border(1.dp, Tone.Line, CircleShape)
                                        .clickable { onPlay(slot) }
                                        .padding(horizontal = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Icons.Rounded.Save, null, tint = Ascend.accent, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot",
                                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                        color = Tone.Ink,
                                    )
                                }
                            }
                        }
                    }
                }

                // Shelf Status Connected Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SHELF STATUS",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.12.em),
                        color = Tone.Muted,
                    )
                    val selIdx = ShelfStatusIds.indexOf(shelfStatus).coerceAtLeast(0)
                    ExpressiveConnectedBar(
                        options = ShelfStatusLabels,
                        selectedIndex = selIdx,
                        onSelect = { onSetShelfStatus(ShelfStatusIds[it]) },
                        height = 40.dp,
                    )
                }

                // Core Engine Picker (if system supports >1 core)
                if (cores.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "EMULATOR CORE",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.12.em),
                            color = Tone.Muted,
                        )
                        val boxShape = RoundedCornerShape(18.dp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(boxShape)
                                .background(Tone.Raised)
                                .border(1.dp, Tone.Line, boxShape),
                        ) {
                            cores.forEachIndexed { idx, core ->
                                val selected = core.id == currentCore?.id
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCore(core) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = core.displayName,
                                        style = TextStyle(
                                            fontFamily = Inter,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                        ),
                                        color = if (selected) Ascend.accent else Tone.Ink,
                                    )
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, null, tint = Ascend.accent, modifier = Modifier.size(17.dp))
                                    }
                                }
                                if (idx < cores.lastIndex) {
                                    HorizontalDivider(thickness = 0.75.dp, color = Tone.Line, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                            }
                        }
                    }
                }

                if (onDelete != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5A5F).copy(alpha = 0.14f))
                            .clickable(onClick = onDelete),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFFF5A5F), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove from Library", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp), color = Color(0xFFFF5A5F))
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
internal fun QuickAddSheet(
    hasGames: Boolean,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onImportBios: () -> Unit,
    onImportSkin: () -> Unit,
    onShufflePlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.glassBorder, sheetShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(38.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(Tone.Faint.copy(alpha = 0.55f)),
            )
            Text(
                text = "Quick Actions",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.025).em),
                color = Tone.Ink,
            )

            val groupShape = RoundedCornerShape(22.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(groupShape)
                    .background(Tone.Raised)
                    .border(1.dp, Tone.Line, groupShape),
            ) {
                IosInsetRow(
                    icon = Icons.Rounded.CreateNewFolder,
                    badgePolygon = MaterialShapes.Clover4Leaf,
                    badgeTint = Color(0xFF0A84FF),
                    title = "Add ROM Folder",
                    subtitle = "Scan games from your device storage",
                    onClick = { onDismiss(); onAddFolder() },
                )
                IosInsetRow(
                    icon = Icons.Rounded.AutoAwesome,
                    badgePolygon = MaterialShapes.Sunny,
                    badgeTint = Color(0xFF25C990),
                    title = "Free Homebrew Catalog",
                    subtitle = "Install openly licensed retro games in one tap",
                    onClick = { onDismiss(); onOpenHomebrew() },
                )
                IosInsetRow(
                    icon = Icons.Rounded.Link,
                    badgePolygon = MaterialShapes.Cookie6Sided,
                    badgeTint = Color(0xFF6D67FF),
                    title = "Download via HTTPS Link",
                    subtitle = "Fetch a ROM file or folder directly",
                    onClick = { onDismiss(); onOpenDownload() },
                )
                IosInsetRow(
                    icon = Icons.Rounded.Memory,
                    badgePolygon = MaterialShapes.Pentagon,
                    badgeTint = Color(0xFFFF7A2F),
                    title = "Import BIOS Files",
                    subtitle = "PlayStation, Sega CD & Lynx firmware",
                    onClick = { onDismiss(); onImportBios() },
                )
                IosInsetRow(
                    icon = Icons.Rounded.SportsEsports,
                    badgePolygon = MaterialShapes.Gem,
                    badgeTint = Color(0xFFE052B8),
                    title = "Import Delta Skin (.deltaskin)",
                    subtitle = "Custom handheld controller shells",
                    showDivider = hasGames,
                    onClick = { onDismiss(); onImportSkin() },
                )
                if (hasGames) {
                    IosInsetRow(
                        icon = Icons.Rounded.Casino,
                        badgePolygon = MaterialShapes.Cookie7Sided,
                        badgeTint = Color(0xFFFF5A5F),
                        title = "Surprise Me",
                        subtitle = "Launch a random game from your library",
                        showDivider = false,
                        onClick = { onDismiss(); onShufflePlay() },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun HomebrewCatalogSheet(
    installedUrls: Set<String>,
    onInstall: (HomebrewGame) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .clip(sheetShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.glassBorder, sheetShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "OPEN SOURCE CLASSICS",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.14.em),
                        color = Ascend.accent,
                    )
                    Text(
                        text = "Free Homebrew",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-0.03).em),
                        color = Tone.Ink,
                    )
                }
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Tone.Raised)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close homebrew", tint = Tone.Muted, modifier = Modifier.size(18.dp))
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(Homebrew.catalog, key = { it.url }) { item ->
                    val installed = item.url in installedUrls
                    val cardShape = RoundedCornerShape(20.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(Tone.Raised)
                            .border(1.dp, Tone.Line, cardShape)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExpressiveShapeBadge(
                            polygon = item.system?.polygon ?: MaterialShapes.Cookie6Sided,
                            containerColor = systemTintColor(item.systemId).copy(alpha = 0.22f),
                            size = 42.dp,
                        ) {
                            Text(
                                text = (item.system?.shortName ?: item.systemId).take(3).uppercase(),
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 9.5.sp),
                                color = systemTintColor(item.systemId),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.5.sp), color = Tone.Ink, maxLines = 1)
                            Text(item.description, style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (installed) Tone.Surface else Ascend.accent)
                                .clickable(enabled = !installed) { onInstall(item) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    if (installed) Icons.Rounded.Check else Icons.Rounded.CloudDownload,
                                    null,
                                    tint = if (installed) Tone.Muted else Ascend.onAccent,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = if (installed) "Added" else "Get",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = if (installed) Tone.Muted else Ascend.onAccent,
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
internal fun LaunchStateDialog(
    state: AppViewModel.LaunchState,
    onConfirmDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is AppViewModel.LaunchState.NeedsCore -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Download ${state.core.displayName}?") },
            text = { Text("${state.game.title} needs the ${state.core.displayName} core to run.") },
            confirmButton = { TextButton(onClick = onConfirmDownload) { Text("Download & Play") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
        is AppViewModel.LaunchState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Downloading ${state.core.displayName}") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text("Preparing engine…")
                }
            },
            confirmButton = {},
        )
        is AppViewModel.LaunchState.Preparing -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Launching ${state.game.title}") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text(state.file, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            confirmButton = {},
        )
        is AppViewModel.LaunchState.Error -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Couldn't start game") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
    }
}

@Composable
internal fun DownloadStateDialog(
    state: AppViewModel.DownloadState,
    onStart: (AppViewModel.DownloadKind, String) -> Unit,
    onDownloadFolder: (List<Downloader.FolderEntry>) -> Unit,
    onPlayGame: (Game) -> Unit,
    onCancel: () -> Unit,
) {
    when (state) {
        is AppViewModel.DownloadState.Input -> {
            var url by rememberSaveable { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text(if (state.kind == AppViewModel.DownloadKind.Bios) "Download BIOS" else "Download ROM Link") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            singleLine = true,
                            label = { Text("https://…") },
                            isError = state.error != null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.error != null) {
                            Text(state.error, color = Color(0xFFFF5A5F), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { onStart(state.kind, url) }) { Text("Download") } },
                dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        }
        is AppViewModel.DownloadState.Running -> {
            val pct = state.total?.takeIf { it > 0 }?.let { (state.read.toFloat() / it).coerceIn(0f, 1f) }
            AlertDialog(
                onDismissRequest = {},
                title = { Text(state.label ?: "Downloading…") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (pct != null) LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("${state.read / 1024} KB downloaded", fontSize = 12.sp, color = Tone.Muted)
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        }
        is AppViewModel.DownloadState.Folder -> {
            val chosen = remember(state.entries) { mutableStateListOf<Downloader.FolderEntry>() }
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text("Pick files (${state.entries.size})") },
                text = {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(state.entries, key = { it.url }) { entry ->
                            val checked = entry in chosen
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { if (checked) chosen.remove(entry) else chosen.add(entry) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked, onCheckedChange = { if (it) chosen.add(entry) else chosen.remove(entry) })
                                Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = chosen.isNotEmpty(), onClick = { onDownloadFolder(chosen.toList()) }) {
                        Text("Download (${chosen.size})")
                    }
                },
                dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        }
        is AppViewModel.DownloadState.Done -> AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(state.title) },
            text = { Text(state.detail) },
            confirmButton = {
                if (state.game != null) {
                    TextButton(onClick = { onPlayGame(state.game) }) { Text("Play Now") }
                } else {
                    TextButton(onClick = onCancel) { Text("Done") }
                }
            },
            dismissButton = { if (state.game != null) TextButton(onClick = onCancel) { Text("Close") } },
        )
        is AppViewModel.DownloadState.Failed -> AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Download failed") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onCancel) { Text("OK") } },
        )
    }
}
