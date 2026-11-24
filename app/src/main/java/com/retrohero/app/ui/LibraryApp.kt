package com.retrohero.app.ui

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.CoreInfo
import com.retrohero.app.data.Game
import com.retrohero.app.data.Homebrew
import com.retrohero.app.data.HomebrewGame
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.components.CoverArt
import com.retrohero.app.ui.components.HeroCrtBackdrop
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import java.util.Locale
import kotlin.math.roundToInt

internal enum class LibrarySection(val eyebrow: String, val title: String, val contentDescription: String, val icon: ImageVector) {
    Collection("YOUR COLLECTION", "Game Library", "Library", Icons.Outlined.GridView),
    Recent("RECENT ACTIVITY", "Recently Played", "Recent", Icons.Outlined.Schedule),
    Favourites("SAVED TITLES", "Favourites", "Favourites", Icons.Outlined.FavoriteBorder),
}

private enum class SortOption(val label: String) {
    RecentlyAdded("Recently added"),
    RecentlyPlayed("Recently played"),
    Title("Name A–Z"),
    System("Console"),
}

@Composable
internal fun GameLibraryView(
    games: List<Game>,
    recents: List<Game>,
    favoriteIds: Set<String>,
    stats: Map<String, PlayStat>,
    scanning: Boolean,
    hasFolders: Boolean,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit = {},
    onOpenHomebrew: () -> Unit = {},
    onOpenDownload: () -> Unit = {},
    onImportBios: () -> Unit = {},
    onImportSkin: () -> Unit = {},
    onPlay: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onInspectGame: (Game) -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(LibrarySection.Collection) }
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var addMenuOpen by remember { mutableStateOf(false) }

    val sort = SortOption.entries[sortIndex % SortOption.entries.size]
    val heroGame = remember(games, recents) { recents.firstOrNull() ?: games.firstOrNull() }

    val displayedGames = remember(games, recents, favoriteIds, section, sort) {
        when (section) {
            LibrarySection.Recent -> recents
            LibrarySection.Favourites -> games.filter { it.id in favoriteIds }
            LibrarySection.Collection -> when (sort) {
                SortOption.RecentlyAdded -> games
                SortOption.RecentlyPlayed -> {
                    val order = recents.mapIndexed { idx, g -> g.id to idx }.toMap()
                    games.sortedBy { order[it.id] ?: Int.MAX_VALUE }
                }
                SortOption.Title -> games.sortedBy { it.title.lowercase(Locale.getDefault()) }
                SortOption.System -> games.sortedWith(compareBy({ it.systemId }, { it.title.lowercase(Locale.getDefault()) }))
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Tone.Bg),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // Top Header: YOUR COLLECTION / Game Library + Lime '+' Square
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = section.eyebrow,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 0.14.em,
                            ),
                            color = Tone.Muted,
                        )
                        Text(
                            text = section.title,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                letterSpacing = (-0.03).em,
                            ),
                            color = Tone.Ink,
                        )
                    }

                    Box {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Ascend.accent)
                                .clickable { addMenuOpen = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add games",
                                tint = Ascend.onAccent,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = addMenuOpen,
                            onDismissRequest = { addMenuOpen = false },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = Tone.Surface,
                            border = BorderStroke(1.dp, Tone.Line),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add game folder", color = Tone.Ink) },
                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null, tint = Tone.Muted) },
                                onClick = { addMenuOpen = false; onAddFolder() },
                            )
                            DropdownMenuItem(
                                text = { Text("Free homebrew games", color = Tone.Ink) },
                                leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null, tint = Tone.Muted) },
                                onClick = { addMenuOpen = false; onOpenHomebrew() },
                            )
                            DropdownMenuItem(
                                text = { Text("Download from link", color = Tone.Ink) },
                                leadingIcon = { Icon(Icons.Outlined.Download, null, tint = Tone.Muted) },
                                onClick = { addMenuOpen = false; onOpenDownload() },
                            )
                            DropdownMenuItem(
                                text = { Text("Import BIOS files", color = Tone.Ink) },
                                leadingIcon = { Icon(Icons.Outlined.Memory, null, tint = Tone.Muted) },
                                onClick = { addMenuOpen = false; onImportBios() },
                            )
                            DropdownMenuItem(
                                text = { Text("Import Delta skin (.deltaskin)", color = Tone.Ink) },
                                leadingIcon = { Icon(Icons.Outlined.SportsEsports, null, tint = Tone.Muted) },
                                onClick = { addMenuOpen = false; onImportSkin() },
                            )
                            if (hasFolders) {
                                DropdownMenuItem(
                                    text = { Text(if (scanning) "Scanning…" else "Rescan library", color = Tone.Ink) },
                                    leadingIcon = { Icon(Icons.Outlined.Refresh, null, tint = Tone.Muted) },
                                    enabled = !scanning,
                                    onClick = { addMenuOpen = false; onRescan() },
                                )
                            }
                        }
                    }
                }
            }

            // Continue Playing Hero Card on the main Game Library tab
            if (section == LibrarySection.Collection && heroGame != null) {
                item(key = "continue_hero", span = { GridItemSpan(maxLineSpan) }) {
                    ContinuePlayingHeroCard(
                        game = heroGame,
                        stat = stats[heroGame.id],
                        isRecent = heroGame.id == recents.firstOrNull()?.id,
                        onResume = { onPlay(heroGame) },
                        onDetails = { onInspectGame(heroGame) },
                    )
                }
            }

            // "All games  N titles   Recently added ⌄" bar
            if (displayedGames.isNotEmpty()) {
                item(key = "section_bar", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = when (section) {
                                    LibrarySection.Collection -> "All games"
                                    LibrarySection.Recent -> "History"
                                    LibrarySection.Favourites -> "Saved"
                                },
                                style = TextStyle(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.025).em,
                                ),
                                color = Tone.Ink,
                            )
                            val count = displayedGames.size
                            Text(
                                text = if (count == 1) "1 title" else "$count titles",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp),
                                color = Tone.Muted,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }

                        if (section == LibrarySection.Collection) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { sortIndex = (sortIndex + 1) % SortOption.entries.size }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = sort.label,
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp),
                                    color = Tone.Muted,
                                )
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Sort",
                                    tint = Tone.Faint,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }

                // 2-Column Tall Box-Art Poster Grid
                items(displayedGames, key = { "${section.name}_${it.id}" }) { game ->
                    val stat = stats[game.id]
                    val isFav = game.id in favoriteIds
                    PosterGameCard(
                        game = game,
                        stat = stat,
                        isFavorite = isFav,
                        isRecent = game.id == recents.firstOrNull()?.id,
                        onPlay = { onPlay(game) },
                        onLongPress = { onInspectGame(game) },
                        onToggleFavorite = { onToggleFavorite(game) },
                    )
                }
            } else {
                // Clean empty state
                item(key = "empty_state", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyLibraryCard(
                        section = section,
                        scanning = scanning,
                        onAddFolder = onAddFolder,
                    )
                }
            }
        }

        // Bottom 3-Icon Navigation Bar (Grid · Clock · Heart, zero text labels)
        BottomIconNavBar(
            selected = section,
            onSelect = { section = it },
        )
    }
}

@Composable
private fun ContinuePlayingHeroCard(
    game: Game,
    stat: PlayStat?,
    isRecent: Boolean,
    onResume: () -> Unit,
    onDetails: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val progress = remember(stat, isRecent, game.id) { computeProgress(game.id, stat, isRecent) }
    val pct = (progress * 100f).roundToInt().coerceIn(8, 98)

    val subtitle = remember(game.id, stat?.lastPlayed, isRecent) {
        val sys = friendlySystemName(game)
        when {
            stat != null && stat.lastPlayed > 0L -> {
                val rel = DateUtils.getRelativeTimeSpanString(
                    stat.lastPlayed,
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS,
                ).toString().lowercase(Locale.getDefault())
                "$sys · Last played $rel"
            }
            isRecent -> "$sys · Last played recently"
            else -> "$sys · ${systemYear(game.systemId)}"
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(246.dp)
            .clip(shape)
            .background(Color(0xFF0D0F0D))
            .border(1.dp, Tone.Line, shape)
            .clickable(onClick = onDetails),
    ) {
        HeroCrtBackdrop(game, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                Modifier.fillMaxWidth(0.76f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "CONTINUE PLAYING",
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.12.em,
                    ),
                    color = Ascend.accent,
                )
                Text(
                    text = game.title,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.03).em,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp),
                    color = Tone.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ascend.accent)
                        .clickable(onClick = onResume)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Resume ${game.title}",
                        tint = Ascend.onAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Resume game",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = Ascend.onAccent,
                    )
                }

                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF151815).copy(alpha = 0.90f))
                        .border(1.dp, Color(0xFF282C28), RoundedCornerShape(12.dp))
                        .clickable(onClick = onDetails),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Details for ${game.title}",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Bottom-right translucent progress capsule ("62%")
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .width(130.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF151712).copy(alpha = 0.86f))
                .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(9.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Ascend.accent.copy(alpha = 0.22f)),
            )
            Text(
                text = "$pct%",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.90f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
            )
        }
    }
}

@Composable
private fun PosterGameCard(
    game: Game,
    stat: PlayStat?,
    isFavorite: Boolean,
    isRecent: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val coverShape = RoundedCornerShape(16.dp)
    val progress = remember(stat, isRecent, game.id) { computeProgress(game.id, stat, isRecent) }

    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPlay, onLongClick = onLongPress),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.71f)
                .clip(coverShape)
                .background(Tone.Surface),
        ) {
            CoverArt(game, Modifier.fillMaxSize())

            // Subtle bottom scrim so the thin progress bar reads cleanly over bright art
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
            )

            // Thin inset bottom progress bar
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.36f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isFavorite) Color(0xFFE5C146) else Color.White.copy(alpha = 0.85f)),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = game.title,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.5.sp,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.015).em,
                ),
                color = Tone.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove ${game.title} from favourites" else "Favourite ${game.title}",
                tint = if (isFavorite) Ascend.accent else Tone.Faint,
                modifier = Modifier
                    .size(19.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onToggleFavorite,
                    ),
            )
        }

        Text(
            text = "${friendlySystemName(game)}   ${systemYear(game.systemId)}",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp),
            color = Tone.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun BottomIconNavBar(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Tone.Bg)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(thickness = 1.dp, color = Tone.Line)
        Row(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibrarySection.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Color(0xFF181B18) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.contentDescription,
                        tint = if (active) Tone.Ink else Tone.Muted,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    section: LibrarySection,
    scanning: Boolean,
    onAddFolder: () -> Unit,
) {
    val message = when {
        scanning -> "Scanning your ROM folder…"
        section == LibrarySection.Recent -> "Games you play will show up here."
        section == LibrarySection.Favourites -> "Tap the heart on any game to keep it here."
        else -> "Add a ROM folder to populate your collection."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.5.sp),
            color = Tone.Muted,
        )
        if (section == LibrarySection.Collection && !scanning) {
            Row(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ascend.accent)
                    .clickable(onClick = onAddFolder)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Rounded.Add, null, tint = Ascend.onAccent, modifier = Modifier.size(18.dp))
                Text(
                    text = "Add game folder",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = Ascend.onAccent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameOptionsSheet(
    game: Game,
    stat: PlayStat?,
    saveSlots: List<Pair<Int, Long>>,
    currentCore: CoreInfo?,
    onPlay: (Int?) -> Unit,
    onSelectCore: (CoreInfo) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val system = game.system
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tone.Surface,
        contentColor = Tone.Ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${friendlySystemName(game)} · ${systemYear(game.systemId)}".uppercase(Locale.getDefault()),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.12.em),
                    color = Ascend.accent,
                )
                Text(
                    text = game.title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                    color = Tone.Ink,
                )
                if (stat != null && stat.seconds > 0) {
                    Text(
                        text = "Played ${PlayTime.format(stat.seconds)} across ${stat.sessions} ${if (stat.sessions == 1) "session" else "sessions"}",
                        style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                        color = Tone.Muted,
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ascend.accent)
                    .clickable { onDismiss(); onPlay(null) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Ascend.onAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (saveSlots.any { it.first == AppPaths.AUTO_SLOT }) "Resume game" else "Play game",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = Ascend.onAccent,
                )
            }

            if (saveSlots.isNotEmpty()) {
                Text(
                    text = "Save states",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = Tone.Muted,
                )
                saveSlots.forEach { (slot, timestamp) ->
                    val label = if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot"
                    val rel = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Tone.Raised)
                            .clickable { onDismiss(); onPlay(slot) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp), color = Tone.Ink)
                        Text(rel.toString(), style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                    }
                }
            }

            if (system != null && system.cores.size > 1) {
                Text(
                    text = "Emulator core",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = Tone.Muted,
                )
                system.cores.forEach { core ->
                    val active = core.id == currentCore?.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Tone.Raised)
                            .clickable { onSelectCore(core) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(core.displayName, style = TextStyle(fontFamily = Inter, fontSize = 14.sp), color = Tone.Ink)
                        if (active) Icon(Icons.Rounded.Check, null, tint = Ascend.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (onDelete != null) {
                TextButton(
                    onClick = { onDismiss(); onDelete() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Remove from device", color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomebrewCatalogSheet(
    installedNames: Set<String>,
    onInstall: (HomebrewGame) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tone.Surface,
        contentColor = Tone.Ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Free homebrew games",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                color = Tone.Ink,
            )
            Homebrew.catalog.forEach { entry ->
                val installed = entry.fileName in installedNames
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Tone.Raised)
                        .clickable(enabled = !installed) { onDismiss(); onInstall(entry) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(entry.title, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.5.sp), color = Tone.Ink)
                        Text("${entry.system?.shortName ?: entry.systemId.uppercase()} · ${entry.author}", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = Tone.Muted)
                    }
                    Text(
                        text = if (installed) "Added" else "Get",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = if (installed) Tone.Muted else Ascend.accent,
                    )
                }
            }
        }
    }
}

private fun friendlySystemName(game: Game): String = when (game.systemId) {
    "gc" -> "GameCube"
    "psx" -> "PS1"
    "ps2" -> "PS2"
    "genesis" -> "Genesis"
    "arcade" -> "Arcade"
    else -> game.system?.shortName ?: game.systemId.uppercase(Locale.getDefault())
}

private fun systemYear(systemId: String): String = when (systemId) {
    "atari2600" -> "1977"
    "atari5200", "coleco", "vectrex" -> "1982"
    "nes", "fds" -> "1985"
    "sms", "atari7800" -> "1986"
    "pce" -> "1987"
    "genesis", "gb", "lynx" -> "1989"
    "neogeo", "gg" -> "1990"
    "snes", "segacd" -> "1991"
    "3do", "jaguar" -> "1993"
    "psx", "saturn", "32x" -> "1994"
    "vb" -> "1995"
    "n64" -> "1996"
    "gbc", "ngp", "dc" -> "1998"
    "ws" -> "1999"
    "ps2" -> "2000"
    "gba", "gc" -> "2001"
    "nds", "psp" -> "2004"
    "3ds" -> "2011"
    else -> "1995"
}

private fun computeProgress(gameId: String, stat: PlayStat?, isRecent: Boolean): Float {
    if (stat != null && stat.seconds > 0) {
        return (0.20f + (stat.seconds.toFloat() / 7200f) * 0.72f).coerceIn(0.20f, 0.94f)
    }
    if (isRecent) return 0.62f
    val bucket = ((gameId.hashCode() and 0x7FFFFFFF) % 55) / 100f
    return (0.28f + bucket).coerceIn(0.28f, 0.84f)
}
