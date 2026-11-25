package com.retrohero.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Downloader
import com.retrohero.app.data.Game
import com.retrohero.app.data.Homebrew
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.M3ConnectedButtonGroup
import com.retrohero.app.ui.components.M3GameCover
import com.retrohero.app.ui.components.M3SegmentedItem
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.M3SplitButton
import com.retrohero.app.ui.components.M3WavyBar
import com.retrohero.app.ui.components.m3SegmentedShape
import com.retrohero.app.ui.theme.polygon

private val M3ShelfIds = listOf("playing", "completed", "backlog")
private val M3ShelfLabels = listOf("Playing", "Completed", "Backlog")

@Composable
internal fun M3GameDetailSheet(
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
    val cs = MaterialTheme.colorScheme
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val cores = game.system?.cores.orEmpty()
    val hasAutoSave = saveSlots.any { it.first == AppPaths.AUTO_SLOT }
    val sysPoly = game.system?.polygon ?: MaterialShapes.Cookie6Sided

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.scrim.copy(alpha = 0.56f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(sheetShape)
                .background(cs.surfaceContainerLow)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // M3 Modal Bottom Sheet drag handle + close tonal circle
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(cs.outlineVariant),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest)
                        .semantics { contentDescription = "Close details" }
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            // Cover + Title + Expressive System Shape Pill
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                M3GameCover(
                    game = game,
                    showTitle = false,
                    modifier = Modifier
                        .width(104.dp)
                        .aspectRatio(0.84f)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.secondaryContainer)
                            .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        M3ShapeBadge(polygon = sysPoly, containerColor = cs.primary, size = 20.dp) {
                            Icon(Icons.Rounded.SportsEsports, null, tint = cs.onPrimary, modifier = Modifier.size(11.dp))
                        }
                        Text(
                            text = (game.system?.shortName ?: game.systemId).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = cs.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = if (stat != null && stat.seconds > 0) {
                        "${PlayTime.format(stat.seconds)} · ${stat.sessions} ${if (stat.sessions == 1) "session" else "sessions"}"
                    } else {
                        game.system?.name ?: "Ready to play"
                    }
                    Text(text = meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                }
            }

            // M3 Expressive Split Play + Favorite Button
            M3SplitButton(
                label = if (hasAutoSave) "Resume game" else "Play game",
                icon = Icons.Rounded.PlayArrow,
                onPrimaryClick = { onPlay(null) },
                trailingIcon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                trailingDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                onTrailingClick = onToggleFavorite,
                trailingActive = isFavorite,
                height = 54.dp,
            )

            M3WavyBar(color = cs.outlineVariant.copy(alpha = 0.6f), amplitude = 2.dp, wavelength = 22.dp, strokeWidth = 2.dp)

            if (saveSlots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save states", style = MaterialTheme.typography.titleSmall, color = cs.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(saveSlots, key = { it.first }) { (slot, _) ->
                            Row(
                                Modifier
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .background(cs.secondaryContainer)
                                    .clickable { onPlay(slot) }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Save, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                    color = cs.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Shelf status", style = MaterialTheme.typography.titleSmall, color = cs.primary)
                val selIdx = M3ShelfIds.indexOf(shelfStatus).coerceAtLeast(0)
                M3ConnectedButtonGroup(
                    options = M3ShelfLabels,
                    selectedIndex = selIdx,
                    onSelect = { onSetShelfStatus(M3ShelfIds[it]) },
                    height = 44.dp,
                )
            }

            if (cores.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Emulator core", style = MaterialTheme.typography.titleSmall, color = cs.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        cores.forEachIndexed { idx, core ->
                            val selected = core.id == currentCore?.id
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(m3SegmentedShape(idx, cores.size))
                                    .background(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
                                    .clickable { onSelectCore(core) }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = core.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                                )
                                if (selected) {
                                    Icon(Icons.Rounded.Check, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (onDelete != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun M3QuickAddSheet(
    hasGames: Boolean,
    onAddFolder: () -> Unit,
    onOpenHomebrew: () -> Unit,
    onOpenDownload: () -> Unit,
    onImportBios: () -> Unit,
    onImportSkin: () -> Unit,
    onShufflePlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val total = if (hasGames) 6 else 5

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.scrim.copy(alpha = 0.56f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(cs.surfaceContainerLow)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(cs.outlineVariant),
            )
            Text("Add & play", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                M3SegmentedItem(
                    index = 0, count = total,
                    icon = Icons.Rounded.CreateNewFolder,
                    badgePolygon = MaterialShapes.Clover4Leaf,
                    badgeContainer = cs.primaryContainer, badgeContent = cs.onPrimaryContainer,
                    title = "Add ROM folder", subtitle = "Scan games from device storage",
                    onClick = { onDismiss(); onAddFolder() },
                )
                M3SegmentedItem(
                    index = 1, count = total,
                    icon = Icons.Rounded.AutoAwesome,
                    badgePolygon = MaterialShapes.Sunny,
                    badgeContainer = cs.tertiaryContainer, badgeContent = cs.onTertiaryContainer,
                    title = "Free homebrew catalog", subtitle = "Install open-source classics in one tap",
                    onClick = { onDismiss(); onOpenHomebrew() },
                )
                M3SegmentedItem(
                    index = 2, count = total,
                    icon = Icons.Rounded.Link,
                    badgePolygon = MaterialShapes.Cookie6Sided,
                    badgeContainer = cs.secondaryContainer, badgeContent = cs.onSecondaryContainer,
                    title = "Download via HTTPS link", subtitle = "Fetch a ROM or folder directly",
                    onClick = { onDismiss(); onOpenDownload() },
                )
                M3SegmentedItem(
                    index = 3, count = total,
                    icon = Icons.Rounded.Memory,
                    badgePolygon = MaterialShapes.Pentagon,
                    badgeContainer = cs.primaryContainer, badgeContent = cs.onPrimaryContainer,
                    title = "Import BIOS files", subtitle = "PlayStation, Sega CD & Lynx firmware",
                    onClick = { onDismiss(); onImportBios() },
                )
                M3SegmentedItem(
                    index = 4, count = total,
                    icon = Icons.Rounded.SportsEsports,
                    badgePolygon = MaterialShapes.Gem,
                    badgeContainer = cs.secondaryContainer, badgeContent = cs.onSecondaryContainer,
                    title = "Import Delta skin (.deltaskin)", subtitle = "Custom handheld controller shells",
                    onClick = { onDismiss(); onImportSkin() },
                )
                if (hasGames) {
                    M3SegmentedItem(
                        index = 5, count = total,
                        icon = Icons.Rounded.Casino,
                        badgePolygon = MaterialShapes.Cookie7Sided,
                        badgeContainer = cs.tertiaryContainer, badgeContent = cs.onTertiaryContainer,
                        title = "Surprise me", subtitle = "Launch a random game from your library",
                        onClick = { onDismiss(); onShufflePlay() },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun M3HomebrewSheet(
    installedUrls: Set<String>,
    onInstall: (HomebrewGame) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val itemsList = Homebrew.catalog

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.scrim.copy(alpha = 0.56f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .clip(sheetShape)
                .background(cs.surfaceContainerLow)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Open-source classics", style = MaterialTheme.typography.labelSmall, color = cs.primary)
                    Text("Free homebrew", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close homebrew", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(itemsList.size, key = { itemsList[it].url }) { idx ->
                    val item = itemsList[idx]
                    val installed = item.url in installedUrls
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(m3SegmentedShape(idx, itemsList.size))
                            .background(cs.surfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        M3ShapeBadge(
                            polygon = item.system?.polygon ?: MaterialShapes.Cookie6Sided,
                            containerColor = cs.secondaryContainer,
                            size = 44.dp,
                        ) {
                            Text(
                                text = (item.system?.shortName ?: item.systemId).take(3).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = cs.onSecondaryContainer,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.onSurface, maxLines = 1)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Row(
                            Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (installed) cs.surfaceContainerHighest else cs.primary)
                                .clickable(enabled = !installed) { onInstall(item) }
                                .padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                if (installed) Icons.Rounded.Check else Icons.Rounded.CloudDownload,
                                null,
                                tint = if (installed) cs.onSurfaceVariant else cs.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (installed) "Added" else "Get",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (installed) cs.onSurfaceVariant else cs.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun M3LaunchDialog(
    state: AppViewModel.LaunchState,
    onConfirmDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is AppViewModel.LaunchState.NeedsCore -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Install ${state.core.displayName}?") },
            text = { Text("${state.game.title} needs the ${state.core.displayName} core to run.") },
            confirmButton = { TextButton(onClick = onConfirmDownload) { Text("Install & play") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
        is AppViewModel.LaunchState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Installing ${state.core.displayName}") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.6.dp)
                    Text("Downloading emulator core…")
                }
            },
            confirmButton = {},
        )
        is AppViewModel.LaunchState.Preparing -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Launching ${state.game.title}") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.6.dp)
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
internal fun M3DownloadDialog(
    state: AppViewModel.DownloadState,
    onStart: (AppViewModel.DownloadKind, String) -> Unit,
    onDownloadFolder: (List<Downloader.FolderEntry>) -> Unit,
    onPlayGame: (Game) -> Unit,
    onCancel: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    when (state) {
        is AppViewModel.DownloadState.Input -> {
            var url by rememberSaveable { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text(if (state.kind == AppViewModel.DownloadKind.Bios) "Download BIOS" else "Download ROM link") },
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
                            Text(state.error, color = cs.error, style = MaterialTheme.typography.bodySmall)
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
                        Text("${state.read / 1024} KB downloaded", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
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
                                Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
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
                    TextButton(onClick = { onPlayGame(state.game) }) { Text("Play now") }
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
