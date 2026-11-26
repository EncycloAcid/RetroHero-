package com.retrohero.app.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.BiosFile
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Game
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalToolbarInset
import com.retrohero.app.ui.components.M3ConnectedButtonGroup
import com.retrohero.app.ui.components.M3ShapeBadge
import com.retrohero.app.ui.components.m3SegmentedShape
import com.retrohero.app.ui.theme.polygon
import java.util.Locale

private val CoreFilterTabs = listOf("Needed", "Installed", "All")

@Composable
internal fun M3CoreManagerPage(
    games: List<Game>,
    coreStates: Map<String, CoreState>,
    neededCores: List<CoreInfo>,
    onDownloadCore: (CoreInfo) -> Unit,
    onDownloadAllNeeded: () -> Unit,
    onDeleteCore: (CoreInfo) -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    val allCores = remember { Systems.allCores() }
    val neededIds = remember(neededCores) { neededCores.map { it.id }.toSet() }
    val missingNeeded = remember(neededCores, coreStates) {
        neededCores.filter { coreStates[it.id] !is CoreState.Installed }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(if (neededIds.isNotEmpty()) 0 else 2) }

    val filtered = remember(allCores, query, tab, neededIds, coreStates) {
        allCores.filter { (core, systems) ->
            val matchTab = when (tab) {
                0 -> core.id in neededIds
                1 -> coreStates[core.id] is CoreState.Installed
                else -> true
            }
            val matchQuery = query.isBlank() ||
                core.displayName.contains(query, ignoreCase = true) ||
                systems.any { it.name.contains(query, ignoreCase = true) || it.shortName.contains(query, ignoreCase = true) }
            matchTab && matchQuery
        }.let { list ->
            if (tab == 0) {
                list.sortedBy { (core, _) ->
                    when (coreStates[core.id]) {
                        is CoreState.Downloading -> 0
                        is CoreState.Failed -> 1
                        is CoreState.NotInstalled, null -> 2
                        is CoreState.Installed -> 3
                    }
                }
            } else list
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "cores_top") {
            SubPageHeader(
                eyebrow = "${allCores.count { coreStates[it.first.id] is CoreState.Installed }} OF ${allCores.size} INSTALLED",
                title = "Emulator cores",
                onBack = onBack,
            )
        }

        // One-tap batch install banner when games need cores!
        if (missingNeeded.isNotEmpty()) {
            item(key = "cores_batch") {
                val anyBusy = missingNeeded.any { coreStates[it.id] is CoreState.Downloading }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(cs.primaryContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    M3ShapeBadge(polygon = MaterialShapes.Sunny, containerColor = cs.primary, size = 44.dp) {
                        Icon(Icons.Rounded.DownloadForOffline, null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${missingNeeded.size} ${if (missingNeeded.size == 1) "core" else "cores"} needed",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = cs.onPrimaryContainer,
                        )
                        Text(
                            text = "Install engines for your ${games.size} library games in one tap",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onPrimaryContainer.copy(alpha = 0.80f),
                        )
                    }
                    Row(
                        Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(cs.primary)
                            .clickable(enabled = !anyBusy, onClick = onDownloadAllNeeded)
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (anyBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                            Text("Installing…", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
                        } else {
                            Text("Install all", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
                        }
                    }
                }
            }
        }

        // Search pill
        item(key = "cores_search") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHighest)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search SNES, PS1, mGBA…", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).clickable { query = "" },
                    )
                }
            }
        }

        // Connected filter bar
        item(key = "cores_tabs") {
            M3ConnectedButtonGroup(
                options = CoreFilterTabs,
                selectedIndex = tab,
                onSelect = { tab = it },
                height = 40.dp,
            )
        }

        if (filtered.isEmpty()) {
            item(key = "cores_empty") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(vertical = 30.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (tab == 0) "No games need cores yet" else "No matching cores",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                    )
                    Text(
                        text = if (tab == 0) "Add a ROM folder or switch to All to browse all 50 engines." else "Try a different search term.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        } else {
            item(key = "cores_list") {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    filtered.forEachIndexed { idx, (core, systems) ->
                        val state = coreStates[core.id] ?: CoreState.NotInstalled
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(m3SegmentedShape(idx, filtered.size))
                                .background(cs.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(core.displayName, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.onSurface, maxLines = 1)
                                        if (core.id in neededIds && state !is CoreState.Installed) {
                                            Box(
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(cs.tertiaryContainer)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                            ) {
                                                Text("Needed", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = cs.onTertiaryContainer)
                                            }
                                        }
                                    }
                                    val sub = buildString {
                                        append(systems.joinToString(" · ") { it.shortName })
                                        if (state is CoreState.Installed && state.sizeBytes > 0) {
                                            append(" · ")
                                            append(String.format(Locale.US, "%.1f MB", state.sizeBytes / (1024f * 1024f)))
                                        } else if (state is CoreState.Failed) {
                                            append(" · ")
                                            append(state.message)
                                        }
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state is CoreState.Failed) cs.error else cs.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                when (state) {
                                    is CoreState.Installed -> {
                                        Box(
                                            Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(cs.errorContainer)
                                                .clickable { onDeleteCore(core) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${core.displayName}", tint = cs.onErrorContainer, modifier = Modifier.size(19.dp))
                                        }
                                    }
                                    is CoreState.Downloading -> {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.6.dp)
                                    }
                                    else -> {
                                        Row(
                                            Modifier
                                                .height(36.dp)
                                                .clip(CircleShape)
                                                .background(cs.primaryContainer)
                                                .clickable { onDownloadCore(core) }
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Icon(Icons.Rounded.CloudDownload, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                            Text("Install", style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
                                        }
                                    }
                                }
                            }

                            if (state is CoreState.Downloading) {
                                if (state.progress > 0f) {
                                    LinearProgressIndicator(progress = { state.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun M3BiosManagerPage(
    biosStatusOf: (BiosFile) -> BiosManager.Status,
    onImportBios: () -> Unit,
    onDownloadBiosLink: () -> Unit,
    onDeleteBios: (String) -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    val systemsWithBios = remember { Systems.all.filter { it.bios.isNotEmpty() } }
    val allBios = remember(systemsWithBios) { systemsWithBios.flatMap { it.bios } }
    val presentCount = allBios.count { biosStatusOf(it) != BiosManager.Status.Missing }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "bios_top") {
            SubPageHeader(
                eyebrow = "$presentCount OF ${allBios.size} FILES INSTALLED",
                title = "BIOS firmware",
                onBack = onBack,
            )
        }

        // Quick import & download pill bar
        item(key = "bios_actions") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                        .clickable(onClick = onImportBios)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.UploadFile, null, tint = cs.onPrimary, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import file", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .clickable(onClick = onDownloadBiosLink)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Link, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("From link", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
                }
            }
        }

        systemsWithBios.forEach { sys ->
            item(key = "bios_sys_${sys.id}") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
                        M3ShapeBadge(polygon = sys.polygon, containerColor = cs.secondaryContainer, size = 26.dp) {
                            Icon(Icons.Rounded.Memory, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(14.dp))
                        }
                        Text(sys.name, style = MaterialTheme.typography.titleSmall, color = cs.primary)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        sys.bios.forEachIndexed { idx, file ->
                            val st = biosStatusOf(file)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(m3SegmentedShape(idx, sys.bios.size))
                                    .background(cs.surfaceContainerHigh)
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(file.fileName, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val (pillBg, pillFg, pillText) = when (st) {
                                            BiosManager.Status.Verified -> Triple(cs.primaryContainer, cs.onPrimaryContainer, "Verified")
                                            BiosManager.Status.Unverified -> Triple(cs.tertiaryContainer, cs.onTertiaryContainer, "Present")
                                            BiosManager.Status.Missing -> if (file.required) {
                                                Triple(cs.errorContainer, cs.onErrorContainer, "Required")
                                            } else {
                                                Triple(cs.surfaceContainerHighest, cs.onSurfaceVariant, "Optional")
                                            }
                                        }
                                        Box(
                                            Modifier
                                                .clip(CircleShape)
                                                .background(pillBg)
                                                .padding(horizontal = 8.dp, vertical = 2.dp),
                                        ) {
                                            Text(pillText, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = pillFg)
                                        }
                                    }
                                    Text(file.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2)
                                }

                                if (st != BiosManager.Status.Missing) {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(cs.errorContainer)
                                            .clickable { onDeleteBios(file.fileName) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${file.fileName}", tint = cs.onErrorContainer, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Icon(
                                        if (file.required) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (file.required) cs.error else cs.outlineVariant,
                                        modifier = Modifier.size(20.dp),
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

@Composable
internal fun M3FoldersManagerPage(
    folders: Set<String>,
    games: List<Game>,
    scanning: Boolean,
    scanProgress: String,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inset = LocalToolbarInset.current
    val folderList = remember(folders) { folders.sorted() }
    val countByFolder = remember(games) { games.groupingBy { it.treeUri }.eachCount() }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = inset + 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "folders_top") {
            SubPageHeader(
                eyebrow = "${folderList.size} ${if (folderList.size == 1) "FOLDER" else "FOLDERS"} · ${games.size} ROMS",
                title = "ROM folders",
                onBack = onBack,
            )
        }

        if (scanning) {
            item(key = "folders_scan") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cs.primaryContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = scanProgress.ifBlank { "Scanning folders for games…" },
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onPrimaryContainer,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item(key = "folders_actions") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                        .clickable(onClick = onAddFolder)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, null, tint = cs.onPrimary, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add folder", style = MaterialTheme.typography.labelLarge, color = cs.onPrimary)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .clickable(enabled = !scanning, onClick = onRescan)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rescan all", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
                }
            }
        }

        if (folderList.isEmpty()) {
            item(key = "folders_empty") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    M3ShapeBadge(polygon = MaterialShapes.Clover4Leaf, containerColor = cs.secondaryContainer, size = 56.dp) {
                        Icon(Icons.Rounded.Folder, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(26.dp))
                    }
                    Text("No ROM folders added yet", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Text(
                        text = "Pick any folder on your phone or SD card. RetroHero auto-sorts all 46 consoles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        } else {
            item(key = "folders_list") {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    folderList.forEachIndexed { idx, uri ->
                        val label = remember(uri) {
                            Uri.decode(uri).substringAfterLast("tree/").substringAfter(':').ifBlank { "Storage folder" }
                        }
                        val romCount = countByFolder[uri] ?: 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(m3SegmentedShape(idx, folderList.size))
                                .background(cs.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            M3ShapeBadge(polygon = MaterialShapes.Clover4Leaf, containerColor = cs.secondaryContainer, size = 40.dp) {
                                Icon(Icons.Rounded.Folder, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(label, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$romCount ${if (romCount == 1) "game" else "games"} indexed", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(cs.errorContainer)
                                    .clickable { onRemoveFolder(uri) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove $label", tint = cs.onErrorContainer, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubPageHeader(
    eyebrow: String,
    title: String,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(cs.secondaryContainer)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = cs.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = cs.primary)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = cs.onBackground)
        }
    }
}
