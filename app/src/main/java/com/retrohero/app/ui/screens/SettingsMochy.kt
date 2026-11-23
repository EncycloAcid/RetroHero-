package com.retrohero.app.ui.screens

import com.retrohero.app.ui.components.emberBackground
import com.retrohero.app.ui.components.ArchiveLabel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.R
import com.retrohero.app.data.DeltaSkin
import com.retrohero.app.data.Look
import com.retrohero.app.data.Looks
import com.retrohero.app.data.PadSkin
import com.retrohero.app.data.Settings
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.glossCard
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.components.Segmented
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setAccent

/** Mochy's emphasized easing. */
val MochyEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val IconTile get() = Tone.Raised
private val SearchFill get() = Tone.Surface
private val Placeholder get() = Tone.Muted

private class Category(val id: String, val title: String, val subtitle: String, val icon: ImageVector, val keywords: List<String>)

private val categories = listOf(
    Category("library", "Game folders", "Add ROM folders & rescan your library", Icons.Rounded.Folder, listOf("folders", "add folder", "rescan", "library", "scan")),
    Category("gameplay", "Gameplay & comfort", "Auto-save, fast-forward, shaders & haptics", Icons.Rounded.Tune, listOf("reduce motion", "animations", "tilt", "gyroscope", "accessibility", "sharp pixels", "haptic", "vibration", "opacity", "touch controls", "fast-forward speed", "auto save", "resume", "screenshots", "gallery", "stick", "d-pad")),
    Category("controls", "Controls", "Controller remapping & touch button layout", Icons.Rounded.SportsEsports, listOf("controller", "buttons", "remap", "rebind", "rewind", "gamepad")),
    Category("looks", "Customisation", "Themes, accents & Delta controller skins", Icons.Rounded.Palette, listOf("look", "theme", "accent", "colour", "color", "pad skin", "touch buttons", "import look", "delta", "deltaskin", "controller skin", "skin")),
    Category("bios", "BIOS & system files", "PS1, Sega CD & console BIOS status", Icons.Rounded.Memory, listOf("bios", "system files", "playstation", "import bios", "download bios")),
    Category("backup", "Backup & restore", "Export or restore saves, states & shelves", Icons.Rounded.Backup, listOf("backup", "restore", "export", "new phone", "saves")),
    Category("about", "About", "Version info & emulator core licenses", Icons.Rounded.Info, listOf("version", "about", "libretro", "licence")),
)

/** The settings home: big title, search pill, then a column of category bubbles. */
@Composable
fun SettingsHome(settings: Settings, onOpen: (String) -> Unit, onOpenStats: () -> Unit, pageIndex: Int = 6) {
    var query by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .emberBackground()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalNavSpace.current),
    ) {
        com.retrohero.app.ui.components.PageHeader("Settings", "Library, controls and appearance", index = pageIndex)
        Spacer(Modifier.height(14.dp))
        AppearanceSwitch(settings)
        Spacer(Modifier.height(16.dp))
        // Search pill.
        val focused = query.isNotEmpty()
        val fill by animateColorAsState(if (focused) Tone.Raised else SearchFill, tween(200), label = "searchFill")
        Row(
            Modifier.fillMaxWidth().height(48.dp).glossCard(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)).background(fill.copy(alpha = if (focused) 0.6f else 0f)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Placeholder, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search settings", style = TextStyle(fontFamily = Inter, fontSize = 15.sp), color = Placeholder)
                BasicTextField(
                    query, { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Tone.Ink),
                    cursorBrush = SolidColor(Salmon),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            val hits = categories.flatMap { c ->
                (listOf(c.title.lowercase()) + c.keywords).filter { it.contains(q) }.map { c to it }
            }.distinctBy { it.second }
            Text("Results", style = TextStyle(fontFamily = Inter, fontSize = 15.sp), color = Tone.Muted, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp))
            if (hits.isEmpty()) {
                Text("Nothing matches “${query.trim()}”.", style = MaterialTheme.typography.bodyMedium, color = Tone.Muted, modifier = Modifier.padding(4.dp))
            }
            hits.forEach { (c, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card)).clickable { onOpen(c.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(IconTile), contentAlignment = Alignment.Center) {
                        Icon(c.icon, null, tint = Tone.Ink, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(label.replaceFirstChar { it.uppercase() }, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp), color = Tone.Ink)
                        Text("Settings › ${c.title}", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                    }
                }
            }
            return@Column
        }

        Spacer(Modifier.height(18.dp))
        // Your stats card, like Mochy's profile entry.
        Row(
            Modifier
                .fillMaxWidth()
                .press3D()
                .glossCard(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
                .clickable(onClick = onOpenStats)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Salmon), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Insights, null, tint = com.retrohero.app.ui.components.Ascend.onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Your stats & recap", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp), color = Tone.Ink)
                Text("Play time, favourite systems, your month", style = MaterialTheme.typography.bodySmall, color = Tone.Muted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted)
        }
        Spacer(Modifier.height(16.dp))
        val groupShape = RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(groupShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.Line, groupShape),
        ) {
            categories.forEachIndexed { index, c ->
                CategoryBubble(index + 1, c.title, c.subtitle, c.icon) { onOpen(c.id) }
                if (index < categories.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = com.retrohero.app.ui.components.Page.Divider)
                }
            }
        }
    }
}

@Composable
private fun CategoryBubble(index: Int, title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArchiveLabel(index.toString().padStart(2, '0'), color = Tone.Faint)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Tone.Raised)
                .border(1.dp, Tone.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Tone.Ink, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp), color = Tone.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Tone.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted)
    }
}

/** A settings sub-page: round back button, mono breadcrumb, big title, divider, and scrolling content. */
@Composable
fun SubSettings(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .emberBackground()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp + LocalNavSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Tone.Raised)
                        .border(1.dp, Tone.Line, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBackIos,
                        "Back",
                        tint = Tone.Ink,
                        modifier = Modifier.size(16.dp).offset(x = 2.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                ArchiveLabel("06 — SETTINGS / ${title.uppercase()}")
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = Tone.Ink,
                modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(com.retrohero.app.ui.components.Page.Divider))
        }
        content()
    }
}

/** Customisation: preview-card pagers for the accent and the touch-pad skin, plus imported looks and Delta skins. */
@Composable
fun LooksSettings(
    settings: Settings,
    imported: List<Look>,
    onImport: () -> Unit,
    onChanged: () -> Unit,
    deltaSkins: List<DeltaSkin> = emptyList(),
    onImportDeltaSkin: () -> Unit = {},
    onDeleteDeltaSkin: (DeltaSkin) -> Unit = {},
) {
    var accentId by remember { mutableStateOf(settings.accent) }
    var skinId by remember { mutableStateOf(settings.padSkin) }
    var deltaRevision by remember { mutableIntStateOf(0) }

    fun apply(accent: String = accentId, skin: String = skinId) {
        accentId = accent
        skinId = skin
        settings.accent = accent
        settings.padSkin = skin
        setAccent(imported.firstOrNull { "look:" + it.id == accent }?.accent ?: Looks.accent(accent))
        onChanged()
    }

    AppearanceSwitch(settings)
    Spacer(Modifier.height(28.dp))
    SectionTitle("Accent", 2)
    Spacer(Modifier.height(10.dp))
    val accents = Looks.accents.map { (id, v) -> Triple(id, v.first, v.second) } + imported.map { Triple("look:" + it.id, it.name, it.accent) }
    StylePager(
        items = accents.map { it.first },
        selected = accentId,
        label = "Accent",
        name = { id -> accents.first { it.first == id }.second },
        onSelect = { apply(accent = it) },
    ) { id -> AccentPreview(accents.first { it.first == id }.third) }

    Spacer(Modifier.height(28.dp))
    SectionTitle("Touch buttons", 3)
    Spacer(Modifier.height(10.dp))
    val skins = Looks.skins.map { it.id to it } + imported.map { "look:" + it.id to it.pad }
    StylePager(
        items = skins.map { it.first },
        selected = skinId,
        label = "Pad skin",
        name = { id -> skins.first { it.first == id }.second.name },
        onSelect = { apply(skin = it) },
    ) { id -> PadPreview(skins.first { it.first == id }.second) }

    Spacer(Modifier.height(28.dp))
    SectionTitle("Your looks", 4)
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
            .clickable(onClick = onImport)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Tone.Raised), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FileOpen, null, tint = Tone.Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Import a look", style = MaterialTheme.typography.titleMedium, color = Tone.Ink)
            Text(
                if (imported.isEmpty()) "A small .json file with an accent and button colours" else "${imported.size} imported: ${imported.joinToString { it.name }}",
                style = MaterialTheme.typography.labelMedium,
                color = Tone.Muted,
                maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted)
    }

    Spacer(Modifier.height(28.dp))
    SectionTitle("Delta skins", 5)
    Spacer(Modifier.height(10.dp))
    val cardShape = RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Tone.Surface)
            .border(1.dp, Tone.Line, cardShape)
            .clickable(onClick = onImportDeltaSkin)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Tone.Raised), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.SportsEsports, null, tint = Tone.Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Import .deltaskin", style = MaterialTheme.typography.titleMedium, color = Tone.Ink)
            Text(
                if (deltaSkins.isEmpty()) "Use custom Delta controller skins (.deltaskin)" else "${deltaSkins.size} installed · tap a skin below to toggle it",
                style = MaterialTheme.typography.labelMedium,
                color = Tone.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Tone.Muted)
    }

    if (deltaSkins.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            deltaSkins.forEach { skin ->
                val active = remember(deltaRevision, skin.identifier, skin.systemIds) {
                    skin.systemIds.any { settings.deltaSkinFor(it) == skin.identifier }
                }
                val sysLabel = skin.systemIds
                    .mapNotNull { id -> Systems.all.firstOrNull { it.id == id }?.shortName }
                    .distinct()
                    .joinToString(" / ")
                    .ifEmpty { skin.gameTypeIdentifier.substringAfterLast('.').uppercase() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(Tone.Surface)
                        .border(
                            if (active) 1.5.dp else 1.dp,
                            if (active) com.retrohero.app.ui.components.Ascend.accent else Tone.Line,
                            cardShape,
                        )
                        .clickable {
                            val next = if (active) null else skin.identifier
                            skin.systemIds.forEach { settings.setDeltaSkinFor(it, next) }
                            deltaRevision++
                            onChanged()
                        }
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (active) com.retrohero.app.ui.components.Ascend.accent else Tone.Raised),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) {
                            Icon(Icons.Rounded.Check, "Active", tint = com.retrohero.app.ui.components.Ascend.onAccent, modifier = Modifier.size(18.dp))
                        } else {
                            ArchiveLabel(sysLabel.take(4), color = Tone.Muted)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(skin.name, style = MaterialTheme.typography.titleMedium, color = Tone.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (active) "$sysLabel · Active" else "$sysLabel · Tap to use",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) com.retrohero.app.ui.components.Ascend.accent else Tone.Muted,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = {
                        skin.systemIds.forEach { sysId ->
                            if (settings.deltaSkinFor(sysId) == skin.identifier) settings.setDeltaSkinFor(sysId, null)
                        }
                        deltaRevision++
                        onDeleteDeltaSkin(skin)
                    }) {
                        Icon(Icons.Rounded.DeleteOutline, "Remove ${skin.name}", tint = Tone.Muted)
                    }
                }
            }
        }
    }
    Text(
        "Button skins and Delta skins apply when you start a game (or switch live from the pause menu).",
        style = MaterialTheme.typography.bodySmall,
        color = Tone.Muted,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun AppearanceSwitch(settings: Settings) {
    SectionTitle("Appearance")
    Spacer(Modifier.height(10.dp))
    Segmented(
        options = listOf("Light", "Dark"),
        selected = if (Tone.isDark) 1 else 0,
        onSelect = { index ->
            val dark = index == 1
            settings.darkMode = dark
            Tone.setDarkMode(dark)
        },
    )
}

@Composable
private fun SectionTitle(text: String, index: Int = 1) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        ArchiveLabel(index.toString().padStart(2, '0'), color = Tone.Faint)
        Text(
            text,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.01).em),
            color = Tone.Ink,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Mochy-style preview cards: 187 dp tall, name bottom-left, a mini phone peeking up bottom-right. */
@Composable
private fun StylePager(
    items: List<String>,
    selected: String,
    label: String,
    name: (String) -> String,
    onSelect: (String) -> Unit,
    preview: @Composable (String) -> Unit,
) {
    val pager = rememberPagerState(initialPage = items.indexOf(selected).coerceAtLeast(0)) { items.size }
    Column {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(end = 40.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { i ->
            val id = items[i]
            val isSel = id == selected
            val ring by animateFloatAsState(if (isSel) 1f else 0f, tween(220), label = "ring")
            val cardShape = RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(187.dp)
                    .clip(cardShape)
                    .background(Tone.Surface)
                    .border(1.dp, Tone.Line, cardShape)
                    .then(
                        if (isSel) Modifier.border(
                            1.5.dp,
                            com.retrohero.app.ui.components.Ascend.accent.copy(alpha = ring),
                            cardShape,
                        ) else Modifier,
                    )
                    .clickable { onSelect(id) },
            ) {
                if (isSel) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(6.dp)
                            .background(com.retrohero.app.ui.components.Ascend.accent, CircleShape),
                    )
                }
                // The mini phone.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp)
                        .offset(y = 30.dp)
                        .width(133.dp)
                        .fillMaxHeight(0.86f)
                        .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
                        .background(Color.Black)
                        .border(1.dp, Tone.Line, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card)),
                ) { preview(id) }
                Box(Modifier.align(Alignment.BottomEnd).fillMaxWidth().height(40.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Tone.Surface))))
                Text(label, style = MaterialTheme.typography.labelMedium, color = Tone.Muted, modifier = Modifier.padding(start = 22.dp, top = 18.dp))
                Text(
                    name(id),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.03).em),
                    color = Tone.Ink,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 24.dp),
                )
            }
        }
        Text(
            if (pager.currentPage < items.lastIndex) "Swipe for more · tap to use" else "Tap to use",
            style = MaterialTheme.typography.bodySmall,
            color = Tone.Muted,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
    }
}

@Composable
private fun AccentPreview(color: Color) {
    Column(Modifier.fillMaxSize().padding(10.dp)) {
    Box(Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Cover)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, color.copy(alpha = 0.35f)))))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(60.dp).height(6.dp).clip(CircleShape).background(Tone.Ink.copy(alpha = 0.7f)))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(Tone.Ink.copy(alpha = 0.3f)))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(54.dp).height(18.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun PadPreview(skin: PadSkin) {
    val faces = listOf(Color(0xFF4C7DFF), Color(0xFF30C26B), Color(0xFFE5484D), Color(0xFFF5B83D))
    val isInstrument = skin.faces == "instrument"
    val dpadFill = if (isInstrument) (if (Tone.isDark) Color(0xFF2B2B30) else Color(0xFF1C1C1F)) else skin.fill
    val dpadEdge = if (isInstrument) Tone.Line else skin.edge
    Box(Modifier.fillMaxSize().background(if (isInstrument) Tone.Bg else Tone.Surface).border(1.dp, Tone.Line, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card)).padding(10.dp)) {
        // D-pad.
        Box(Modifier.align(Alignment.CenterStart).offset(y = (-10).dp).size(44.dp)) {
            Box(Modifier.align(Alignment.Center).size(15.dp, 44.dp).clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)).background(dpadFill).border(1.dp, dpadEdge, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)))
            Box(Modifier.align(Alignment.Center).size(44.dp, 15.dp).clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)).background(dpadFill).border(1.dp, dpadEdge, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)))
        }
        // Face buttons.
        Box(Modifier.align(Alignment.CenterEnd).offset(y = (-10).dp).size(48.dp)) {
            val pos = listOf(Alignment.TopCenter, Alignment.CenterStart, Alignment.CenterEnd, Alignment.BottomCenter)
            pos.forEachIndexed { i, a ->
                val c = when (skin.faces) {
                    "instrument" -> if (i == 2) com.retrohero.app.ui.components.Ascend.accent else Tone.Raised
                    "mono" -> skin.pressed ?: skin.edge
                    "accent" -> skin.edge
                    else -> faces[i]
                }
                Box(Modifier.align(a).size(16.dp).clip(CircleShape).background(c.copy(alpha = if (skin.faces == "color") 0.8f else c.alpha)).border(1.dp, Tone.Line, CircleShape))
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { Box(Modifier.size(26.dp, 8.dp).clip(CircleShape).background(if (isInstrument) Tone.Raised else skin.fill).border(1.dp, dpadEdge, CircleShape)) }
        }
    }
}
