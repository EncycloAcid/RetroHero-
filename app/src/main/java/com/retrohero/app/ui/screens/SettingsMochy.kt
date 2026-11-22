package com.retrohero.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.FileOpen
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.R
import com.retrohero.app.data.Look
import com.retrohero.app.data.Looks
import com.retrohero.app.data.PadSkin
import com.retrohero.app.data.Settings
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.components.Arcade
import com.retrohero.app.ui.components.ArcadeHeader
import com.retrohero.app.ui.components.Chunky
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Salmon
import com.retrohero.app.ui.theme.setAccent

/** Mochy's emphasized easing. */
val MochyEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val BubbleFill = Color(0xFF1C1C1E)
private val IconTile = Color(0xFF333338)
private val SearchFill = Color(0xFF141416)
private val Placeholder = Color(0xFF77777D)

private class Category(val id: String, val title: String, val icon: ImageVector, val keywords: List<String>)

private val categories = listOf(
    Category("library", "Game folders", Icons.Rounded.Folder, listOf("folders", "add folder", "rescan", "library", "scan")),
    Category("gameplay", "Gameplay", Icons.Rounded.Tune, listOf("sharp pixels", "haptic", "vibration", "opacity", "touch controls", "fast-forward speed", "auto save", "resume", "screenshots", "gallery", "stick", "d-pad")),
    Category("controls", "Controls", Icons.Rounded.SportsEsports, listOf("controller", "buttons", "remap", "rebind", "rewind", "gamepad")),
    Category("looks", "Customisation", Icons.Rounded.Palette, listOf("look", "theme", "accent", "colour", "color", "pad skin", "touch buttons", "import look")),
    Category("bios", "BIOS & system files", Icons.Rounded.Memory, listOf("bios", "system files", "playstation", "import bios", "download bios")),
    Category("backup", "Backup & restore", Icons.Rounded.Backup, listOf("backup", "restore", "export", "new phone", "saves")),
    Category("about", "About", Icons.Rounded.Info, listOf("version", "about", "libretro", "licence")),
)

/** The settings home: big title, search pill, then a column of category bubbles. */
@Composable
fun SettingsHome(onOpen: (String) -> Unit, onOpenStats: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalNavSpace.current),
    ) {
        ArcadeHeader("Settings", "Options", Arcade.Grape, trailing = {
            Image(painterResource(R.drawable.ic_logo_mark), null, Modifier.padding(bottom = 10.dp).size(width = 44.dp, height = 30.dp))
        })
        Spacer(Modifier.height(16.dp))
        // Search pill.
        val focused = query.isNotEmpty()
        val fill by animateColorAsState(if (focused) Color(0xFF1E1E21) else SearchFill, tween(200), label = "searchFill")
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(fill).border(2.dp, if (focused) Arcade.Grape else Color.White.copy(alpha = 0.08f), CircleShape).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Placeholder, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search settings", style = TextStyle(fontFamily = Inter, fontSize = 15.sp), color = Placeholder)
                BasicTextField(
                    query, { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Color.White),
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
            Text("Results", style = TextStyle(fontFamily = Inter, fontSize = 15.sp), color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp))
            if (hits.isEmpty()) {
                Text("Nothing matches “${query.trim()}”.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(4.dp))
            }
            hits.forEach { (c, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onOpen(c.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(IconTile), contentAlignment = Alignment.Center) {
                        Icon(c.icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(label.replaceFirstChar { it.uppercase() }, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp), color = Color.White)
                        Text("Settings › ${c.title}", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Color.White.copy(alpha = 0.55f))
                    }
                }
            }
            return@Column
        }

        Spacer(Modifier.height(18.dp))
        // Your stats card: a bright chunky block.
        Chunky(Modifier.fillMaxWidth(), face = Arcade.Pink, shape = RoundedCornerShape(26.dp), onClick = onOpenStats) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Arcade.Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Insights, null, tint = Arcade.Pink, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your stats & recap", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 19.sp), color = Arcade.Ink)
                    Text("Play time, favourite systems, your month", style = MaterialTheme.typography.bodySmall, color = Arcade.Ink.copy(alpha = 0.7f))
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Arcade.Ink)
            }
        }
        Spacer(Modifier.height(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEachIndexed { i, c -> CategoryBubble(c.title, c.icon, Arcade.palette[(i + 1) % Arcade.palette.size]) { onOpen(c.id) } }
        }
    }
}

@Composable
private fun CategoryBubble(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    // Dark face on a ledge in the category's colour, with a bright icon tile.
    Chunky(Modifier.fillMaxWidth(), face = BubbleFill, ledge = androidx.compose.ui.graphics.lerp(color, Color.Black, 0.35f), shape = RoundedCornerShape(22.dp), depth = 4.dp, onClick = onClick) {
        Row(Modifier.height(66.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Arcade.Ink, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp), color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = color)
        }
    }
}

/** A settings sub-page: round back button, big title, scrolling content. */
@Composable
fun SubSettings(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp + LocalNavSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Chunky(face = Color(0xFF2C2C2E), shape = CircleShape, depth = 3.dp, onClick = onBack, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = Color.White, modifier = Modifier.padding(11.dp).size(18.dp).offset(x = 3.dp))
            }
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-0.045).em),
                color = Color.White,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            )
        }
        content()
    }
}

/** Customisation: preview-card pagers for the accent and the touch-pad skin, plus imported looks. */
@Composable
fun LooksSettings(settings: Settings, imported: List<Look>, onImport: () -> Unit, onChanged: () -> Unit) {
    var accentId by remember { mutableStateOf(settings.accent) }
    var skinId by remember { mutableStateOf(settings.padSkin) }

    fun apply(accent: String = accentId, skin: String = skinId) {
        accentId = accent
        skinId = skin
        settings.accent = accent
        settings.padSkin = skin
        setAccent(imported.firstOrNull { "look:" + it.id == accent }?.accent ?: Looks.accent(accent))
        onChanged()
    }

    SectionTitle("Accent")
    val accents = Looks.accents.map { (id, v) -> Triple(id, v.first, v.second) } + imported.map { Triple("look:" + it.id, it.name, it.accent) }
    StylePager(
        items = accents.map { it.first },
        selected = accentId,
        label = "Accent",
        name = { id -> accents.first { it.first == id }.second },
        onSelect = { apply(accent = it) },
    ) { id -> AccentPreview(accents.first { it.first == id }.third) }

    SectionTitle("Touch buttons")
    val skins = Looks.skins.map { it.id to it } + imported.map { "look:" + it.id to it.pad }
    StylePager(
        items = skins.map { it.first },
        selected = skinId,
        label = "Pad skin",
        name = { id -> skins.first { it.first == id }.second.name },
        onSelect = { apply(skin = it) },
    ) { id -> PadPreview(skins.first { it.first == id }.second) }

    SectionTitle("Your looks")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141416))
            .clickable(onClick = onImport)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE5E5EA)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FileOpen, null, tint = Color(0xFF1C1C1E), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Import a look", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                if (imported.isEmpty()) "A small .json file with an accent and button colours" else "${imported.size} imported: ${imported.joinToString { it.name }}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.5f))
    }
    Text(
        "Button skins show in games you start after changing them.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Color.White, modifier = Modifier.padding(start = 4.dp))
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
            Chunky(Modifier.fillMaxWidth(), face = Color(0xFF1C1C1E), ledge = androidx.compose.ui.graphics.lerp(Arcade.Grape, Color.Black, 0.3f + 0.4f * (1 - ring)), outline = Arcade.Grape.copy(alpha = ring), shape = RoundedCornerShape(24.dp), onClick = { onSelect(id) }) {
            Box(Modifier.fillMaxWidth().height(187.dp).clip(RoundedCornerShape(24.dp))) {
                // The mini phone.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp)
                        .offset(y = 30.dp)
                        .width(133.dp)
                        .fillMaxHeight(0.86f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(15.dp)),
                ) { preview(id) }
                Box(Modifier.align(Alignment.BottomEnd).fillMaxWidth().height(40.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1C1C1E)))))
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(start = 22.dp, top = 18.dp))
                Text(
                    name(id),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.03).em),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 24.dp),
                )
            }
            }
        }
        Text(
            if (pager.currentPage < items.lastIndex) "Swipe for more · tap to use" else "Tap to use",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
    }
}

@Composable
private fun AccentPreview(color: Color) {
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(10.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, color.copy(alpha = 0.35f)))))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(60.dp).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(54.dp).height(18.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun PadPreview(skin: PadSkin) {
    val faces = listOf(Color(0xFF4C7DFF), Color(0xFF30C26B), Color(0xFFE5484D), Color(0xFFF5B83D))
    Box(Modifier.fillMaxSize().background(Color(0xFF14121A)).padding(10.dp)) {
        // D-pad.
        Box(Modifier.align(Alignment.CenterStart).offset(y = (-10).dp).size(44.dp)) {
            Box(Modifier.align(Alignment.Center).size(15.dp, 44.dp).clip(RoundedCornerShape(4.dp)).background(skin.fill).border(1.dp, skin.edge, RoundedCornerShape(4.dp)))
            Box(Modifier.align(Alignment.Center).size(44.dp, 15.dp).clip(RoundedCornerShape(4.dp)).background(skin.fill).border(1.dp, skin.edge, RoundedCornerShape(4.dp)))
        }
        // Face buttons.
        Box(Modifier.align(Alignment.CenterEnd).offset(y = (-10).dp).size(48.dp)) {
            val pos = listOf(Alignment.TopCenter, Alignment.CenterStart, Alignment.CenterEnd, Alignment.BottomCenter)
            pos.forEachIndexed { i, a ->
                val c = when (skin.faces) {
                    "mono" -> skin.pressed ?: skin.edge
                    "accent" -> skin.edge
                    else -> faces[i]
                }
                Box(Modifier.align(a).size(16.dp).clip(CircleShape).background(c.copy(alpha = if (skin.faces == "color") 0.8f else c.alpha)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape))
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { Box(Modifier.size(26.dp, 8.dp).clip(CircleShape).background(skin.fill).border(1.dp, skin.edge, CircleShape)) }
        }
    }
}
