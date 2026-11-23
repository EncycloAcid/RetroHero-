package com.retrohero.app.ui

import android.app.ActivityOptions
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.retrohero.app.ui.components.LocalNavSpace
import com.retrohero.app.ui.screens.GameDetailScreen
import com.retrohero.app.ui.screens.OnboardingScreen
import com.retrohero.app.ui.screens.SystemDetailScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.retrohero.app.ui.components.FloatingNavBar
import com.retrohero.app.ui.components.GameMenuSheet
import com.retrohero.app.ui.components.MiniPlayer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.retrohero.app.ui.components.MiniPlayerSpace
import com.retrohero.app.ui.screens.LibraryScreen
import com.retrohero.app.ui.screens.ShelfScreen
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.ViewAgenda
import com.retrohero.app.ui.screens.StatsScreen
import com.retrohero.app.ui.screens.RecapScreen
import com.retrohero.app.data.Looks
import com.retrohero.app.ui.components.CollectionPickerSheet
import com.retrohero.app.ui.components.NameDialog
import com.retrohero.app.ui.components.LocalArtPaths
import com.retrohero.app.ui.components.LocalArtVersion
import com.retrohero.app.ui.screens.CollectionScreen
import com.retrohero.app.ui.screens.RemapScreen
import com.retrohero.app.ui.screens.rememberControllers
import com.retrohero.app.data.GameCollection
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.retrohero.app.ui.components.FloatingNavHeight
import com.retrohero.app.ui.components.NavItem
import com.retrohero.app.ui.screens.HomeScreen
import com.retrohero.app.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.remember
import com.retrohero.app.ui.components.LaunchIntroOverlay
import com.retrohero.app.ui.components.LaunchIntroState
import com.retrohero.app.ui.components.LocalLaunchIntro
import com.retrohero.app.ui.components.emberBackground
import com.retrohero.app.ui.screens.ShelvesScreen
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import com.retrohero.app.ui.components.DownloadSheet
import com.retrohero.app.ui.components.HomebrewSheet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.data.CoreManager.CoreState
import com.retrohero.app.data.Systems
import com.retrohero.app.ui.components.SystemBadge
import com.retrohero.app.ui.screens.CoresScreen
import com.retrohero.app.ui.screens.GamesScreen
import com.retrohero.app.ui.screens.SettingsScreen
import com.retrohero.app.ui.theme.RetroHeroTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The launch intro plays on a cold start only, not when the activity is recreated.
        // The intro lands the logo on Home's header, so it only plays when the app opens on Home.
        val opensOnHome = vm.settings.lastTab.let { it == null || it == "Home" }
        val intro = LaunchIntroState(plays = savedInstanceState == null && !vm.settings.reducedMotion.value && opensOnHome)
        vm.settings.accent.let { id ->
            com.retrohero.app.ui.theme.setAccent(
                Looks.custom(vm.paths.looks).firstOrNull { "look:" + it.id == id }?.accent ?: Looks.accent(id),
            )
        }
        com.retrohero.app.ui.theme.Tone.setDarkMode(vm.settings.darkMode)
        handleIncomingDeltaSkin(intent)
        setContent {
            RetroHeroTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                val lightSystemBars = !com.retrohero.app.ui.theme.Tone.isDark
                androidx.compose.runtime.SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = lightSystemBars
                        isAppearanceLightNavigationBars = lightSystemBars
                    }
                }
                val artVersion by vm.artVersion.collectAsStateWithLifecycle()
                CompositionLocalProvider(
                    LocalLaunchIntro provides intro,
                    LocalArtPaths provides vm.paths,
                    LocalArtVersion provides artVersion,
                    com.retrohero.app.ui.components.LocalReducedMotion provides vm.settings.reducedMotion.collectAsStateWithLifecycle().value,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        App(vm)
                        LaunchIntroOverlay(intro)
                    }
                }
            }
        }
    }

    private fun handleIncomingDeltaSkin(intent: android.content.Intent?) {
        if (intent?.action != android.content.Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)!!.use { stream ->
                        com.retrohero.app.data.DeltaSkins.import(vm.paths.deltaSkins, stream)
                    }
                }
            }
            result.onSuccess { skin ->
                skin.systemIds.forEach { vm.settings.setDeltaSkinFor(it, skin.identifier) }
                Toast.makeText(this@MainActivity, "Added Delta skin “${skin.name}”", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, err.message ?: "Couldn't import .deltaskin file.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

/** Shares a screenshot through the system share sheet. */
private fun shareScreenshot(context: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(android.content.Intent.EXTRA_STREAM, uri)
        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(android.content.Intent.createChooser(send, "Share screenshot"))
}

internal enum class Tab(val label: String, val title: String, val selected: ImageVector, val unselected: ImageVector) {
    Home("Home", "RetroHero", Icons.Filled.Home, Icons.Outlined.Home),
    Shelf("Shelves", "Shelves", Icons.Filled.ViewAgenda, Icons.Outlined.ViewAgenda),
    Games("Search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    Library("Library", "Library", Icons.Filled.Layers, Icons.Outlined.Layers),
    Cores("Cores", "Emulators", Icons.Filled.Extension, Icons.Outlined.Extension),
    Settings("Settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

internal val navigationItems = Tab.entries.map { NavItem(it.label, it.selected, it.unselected) }

@Composable
private fun App(vm: AppViewModel) {
    val context = LocalContext.current
    val games by vm.games.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val biosVersion by vm.biosVersion.collectAsStateWithLifecycle()
    val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val short = LocalConfiguration.current.screenHeightDp < 500
    var addMenu by remember { mutableStateOf(false) }
    var homebrewOpen by rememberSaveable { mutableStateOf(false) }

    var tab by rememberSaveable { mutableStateOf(Tab.entries.firstOrNull { it.name == vm.settings.lastTab } ?: Tab.Home) }
    LaunchedEffect(tab) { vm.settings.lastTab = tab.name }
    // Shelves tab: rows of shelves, or the 3D cover flow / spine wall.
    var shelfFlow by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = shelfFlow && tab == Tab.Shelf) { shelfFlow = false }
    var openSystemId by rememberSaveable { mutableStateOf<String?>(null) }
    var openCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickCollectionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<GameCollection?>(null) }
    var remapOpen by rememberSaveable { mutableStateOf(false) }
    var statsOpen by rememberSaveable { mutableStateOf(false) }
    var recap by remember { mutableStateOf<Pair<com.retrohero.app.ui.screens.Period, com.retrohero.app.data.PlaySummary>?>(null) }
    var naming by remember { mutableStateOf(false) }
    val shots by vm.shots.collectAsStateWithLifecycle()
    val reducedMotion by vm.settings.reducedMotion.collectAsStateWithLifecycle()
    val tiltEffects by vm.settings.tiltEffects.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    // Dragging the mini player down hides it until you play something else.
    var miniHidden by rememberSaveable(recents.firstOrNull()?.id) { mutableStateOf(false) }
    var lookVersion by remember { mutableIntStateOf(0) }
    val customLooks = remember(lookVersion) { Looks.custom(vm.paths.looks) }
    val lookScope = rememberCoroutineScope()
    val lookPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lookScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    Looks.import(vm.paths.looks, text)
                }
            }
            result.onSuccess { lookVersion++; Toast.makeText(context, "Added “${it.name}”", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "That file isn't a RetroHero look.", Toast.LENGTH_LONG).show() }
        }
    }
    var deltaSkinVersion by remember { mutableIntStateOf(0) }
    val installedDeltaSkins = remember(deltaSkinVersion) { com.retrohero.app.data.DeltaSkins.list(vm.paths.deltaSkins) }
    val deltaSkinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lookScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { stream ->
                        com.retrohero.app.data.DeltaSkins.import(vm.paths.deltaSkins, stream)
                    }
                }
            }
            result.onSuccess { skin ->
                skin.systemIds.forEach { vm.settings.setDeltaSkinFor(it, skin.identifier) }
                deltaSkinVersion++
                Toast.makeText(context, "Added Delta skin “${skin.name}”", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Couldn't import .deltaskin file.", Toast.LENGTH_LONG).show()
            }
        }
    }
    var pendingBios by remember { mutableStateOf(false) }
    val remaps by vm.remaps.collectAsStateWithLifecycle()
    val backupState by vm.backupState.collectAsStateWithLifecycle()
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.backUp(uri, pendingBios)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restore(uri)
    }
    val collections by vm.collections.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val artVersion by vm.artVersion.collectAsStateWithLifecycle()
    // Play time and screenshots are written by the game process; pick them up on return.
    LifecycleResumeEffect(Unit) {
        vm.refreshStats()
        onPauseOrDispose { }
    }
    LaunchedEffect(games.size) { vm.refreshStats() }
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importBios(uris)
    }
    LaunchedEffect(Unit) {
        vm.launchIntents.collect {
            val options = ActivityOptions.makeCustomAnimation(context, R.anim.game_enter, R.anim.hold)
            context.startActivity(it, options.toBundle())
        }
    }

    val openSystem = openSystemId?.let { Systems.get(it) }
    val openCollection = openCollectionId?.let { id -> collections.firstOrNull { it.id == id } }
    val albumOpen = openSystem != null || openCollection != null
    BackHandler(enabled = albumOpen) { openSystemId = null; openCollectionId = null }
    val byId = remember(games) { games.associateBy { it.id } }
    val collectionGames = remember(collections, byId) { collections.map { c -> c to c.gameIds.mapNotNull { byId[it] } } }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Home is immersive: no top app bar, content runs under the status bar.
    val isHome = !albumOpen && tab == Tab.Home && games.isNotEmpty()
    // Pages that run under the status bar with their own big titles, like the music app's.
    // Every tab draws its own big header, like the Shelf.
    val immersive = !albumOpen
    val miniGame = recents.firstOrNull()?.takeIf { !isHome && !miniHidden && !albumOpen && tab != Tab.Settings && !(tab == Tab.Shelf && shelfFlow) && sheetFor == null && !statsOpen && recap == null }
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val glass = rememberHazeState()
    val floatingNavSpace = (if (wide || albumOpen) 0.dp else FloatingNavHeight + 1.dp + navBarInset) +
        (if (miniGame != null && !wide) MiniPlayerSpace else 0.dp)
    val systemCount = games.mapTo(HashSet()) { it.systemId }.size

    val accent = when {
        openSystem != null -> openSystem.color
        tab == Tab.Games || tab == Tab.Home -> recents.firstOrNull()?.system?.color ?: MaterialTheme.colorScheme.primary
        tab == Tab.Library -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(Modifier.fillMaxSize().emberBackground()) {
    Row(Modifier.fillMaxSize()) {
        if (wide && !albumOpen) {
            // Landscape / large screens: an expressive navigation rail instead of the bottom bar.
            WideNavigationRail(modifier = Modifier.fillMaxHeight(), colors = WideNavigationRailDefaults.colors(containerColor = Color.Transparent)) {
                Tab.entries.forEach { t ->
                    WideNavigationRailItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(if (tab == t) t.selected else t.unselected, null) },
                        label = { Text(t.label) },
                        railExpanded = false,
                    )
                }
            }
        }
    Box(Modifier.weight(1f).fillMaxHeight()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!immersive && !albumOpen) Column {
                val barTitle: @Composable () -> Unit = {
                        Text(openSystem?.name ?: tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val barSubtitle: @Composable () -> Unit = {
                        val text = when {
                            openSystem != null -> {
                                val n = games.count { it.systemId == openSystem.id }
                                "${openSystem.manufacturer} · $n ${if (n == 1) "game" else "games"}"
                            }
                            tab == Tab.Games && games.isNotEmpty() -> "Find games across ${systemCount} systems"
                            tab == Tab.Games -> "Your retro game library"
                            tab == Tab.Library -> "Your games"
                            tab == Tab.Cores -> "Emulator engines"
                            else -> "Library, controls & BIOS"
                        }
                        Text(text)
                }
                val barNav: @Composable () -> Unit = {
                        if (openSystem != null) {
                            FilledTonalIconButton(onClick = { openSystemId = null }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                }
                val barActions: @Composable RowScope.() -> Unit = {
                        if (openSystem != null) SystemBadge(openSystem, 40.dp, Modifier.padding(end = 12.dp))
                        if (openSystem == null && (tab == Tab.Games || tab == Tab.Home) && folders.isNotEmpty()) {
                            FilledTonalIconButton(onClick = vm::rescan, enabled = !scanning, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Outlined.Refresh, "Rescan library")
                            }
                        }
                }
                val barColors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                )
                if (short) {
                    MediumFlexibleTopAppBar(
                        title = barTitle, subtitle = barSubtitle, navigationIcon = barNav, actions = barActions,
                        scrollBehavior = scrollBehavior, colors = barColors,
                    )
                } else {
                    LargeFlexibleTopAppBar(
                        title = barTitle, subtitle = barSubtitle, navigationIcon = barNav, actions = barActions,
                        scrollBehavior = scrollBehavior, colors = barColors,
                    )
                }
                if (scanning) LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
        },
    ) { padding ->
      Row(
          (if (immersive || albumOpen) Modifier else Modifier.padding(top = padding.calculateTopPadding()))
              .fillMaxSize(),
      ) {
        val screen = openSystem?.id ?: openCollection?.let { "col:" + it.id } ?: tab.name
        val density = LocalDensity.current
        CompositionLocalProvider(LocalNavSpace provides floatingNavSpace) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (
                    fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(220, easing = FastOutSlowInEasing)) { with(density) { 8.dp.roundToPx() } }
                    ) togetherWith (
                    fadeOut(tween(220, easing = FastOutSlowInEasing)) +
                        slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { with(density) { (-8).dp.roundToPx() } }
                    )
            },
            label = "screen",
            modifier = Modifier.weight(1f).fillMaxHeight().hazeSource(glass),
        ) { key ->
            val system = Systems.get(key)
            val collection = if (key.startsWith("col:")) collections.firstOrNull { "col:" + it.id == key } else null
            when {
                collection != null -> CollectionScreen(
                    collection = collection,
                    games = collection.gameIds.mapNotNull { byId[it] },
                    onBack = { openCollectionId = null },
                    onOpenGame = { sheetFor = it.id },
                    onPlay = { vm.play(it) },
                    onPlayRandom = {
                        collection.gameIds.mapNotNull { byId[it] }.randomOrNull()?.let { vm.play(it) }
                    },
                    onRemove = { vm.setInCollection(collection, it, false) },
                    onOptions = { menuFor = it.id },
                    onRename = { renaming = collection },
                    onDelete = { openCollectionId = null; vm.deleteCollection(collection) },
                    favorites = favoriteIds,
                )
                system != null -> SystemDetailScreen(
                    system = system,
                    games = games.filter { it.systemId == system.id },
                    onBack = { openSystemId = null },
                    onOpenGame = { sheetFor = it.id },
                    onPlay = { vm.play(it) },
                    onPlayRandom = { vm.playRandom(system.id) },
                    favorites = favoriteIds,
                    onToggleFavorite = { vm.toggleFavorite(it) },
                    onOptions = { menuFor = it.id },
                )
                key == Tab.Home.name && games.isNotEmpty() -> HomeScreen(
                    games = games,
                    recents = recents,
                    onPlay = vm::play,
                    onOpen = { sheetFor = it.id },
                    onOptions = { menuFor = it.id },
                    favorites = favorites,
                    stats = stats,
                    collections = collectionGames,
                    onOpenCollection = { openCollectionId = it.id },
                    pageIndex = tab.ordinal + 1,
                    onOpenStats = { statsOpen = true },
                    onOpenSystem = { openSystemId = it.id },
                    onSeeAllGames = { tab = Tab.Games },
                    topActions = {
                        Box {
                            FilledIconButton(
                                onClick = { addMenu = true },
                                shapes = IconButtonDefaults.shapes(),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = com.retrohero.app.ui.theme.Tone.Raised,
                                    contentColor = com.retrohero.app.ui.theme.Tone.Ink,
                                ),
                                modifier = Modifier.size(46.dp),
                            ) { Icon(Icons.Rounded.Add, "Add games") }
                            DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }, shape = MaterialTheme.shapes.large) {
                                DropdownMenuItem(
                                    text = { Text("Download from link") },
                                    leadingIcon = { Icon(Icons.Outlined.Download, null) },
                                    onClick = { addMenu = false; vm.openDownload(AppViewModel.DownloadKind.Game) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Free homebrew games") },
                                    leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null) },
                                    onClick = { addMenu = false; homebrewOpen = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Add game folder") },
                                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                                    onClick = { addMenu = false; folderPicker.launch(null) },
                                )
                                if (folders.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Rescan library") },
                                        leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                                        enabled = !scanning,
                                        onClick = { addMenu = false; vm.rescan() },
                                    )
                                }
                            }
                        }
                    },
                    bottomPadding = floatingNavSpace,
                )
                key == Tab.Games.name || key == Tab.Home.name -> GamesScreen(
                    games = games,
                    recents = if (key == Tab.Home.name) recents else emptyList(),
                    hasFolders = folders.isNotEmpty(),
                    scanning = scanning,
                    onAddFolder = { folderPicker.launch(null) },
                    onDownload = { vm.openDownload(AppViewModel.DownloadKind.Game) },
                    onPlay = { sheetFor = it.id },
                    onGameOptions = { menuFor = it.id },
                    onQuickPlay = { vm.play(it) },
                    onHomebrew = { homebrewOpen = true },
                    stats = stats,
                    favorites = favoriteIds,
                    pageIndex = tab.ordinal + 1,
                )
                key == Tab.Shelf.name && !shelfFlow -> ShelvesScreen(
                    games = games,
                    recents = recents,
                    favorites = favorites,
                    collections = collectionGames,
                    stats = stats,
                    onOpenGame = { sheetFor = it.id },
                    onOptions = { menuFor = it.id },
                    onOpenCollection = { openCollectionId = it.id },
                    onNewShelf = { naming = true },
                    pageIndex = tab.ordinal + 1,
                    onCoverFlow = { shelfFlow = true },
                )
                key == Tab.Shelf.name -> ShelfScreen(
                    games = games,
                    stats = stats,
                    recentIds = recents.map { it.id },
                    favorites = favoriteIds,
                    tiltEnabled = tiltEffects,
                    reducedMotion = reducedMotion,
                    onPlay = { vm.play(it) },
                    onDetails = { sheetFor = it.id },
                    onShowShelves = { shelfFlow = false },
                    pageIndex = tab.ordinal + 1,
                )
                key == Tab.Library.name -> LibraryScreen(
                    backEnabled = sheetFor == null && menuFor == null && !statsOpen && recap == null && pickCollectionsFor == null,
                    games = games,
                    favorites = favorites,
                    collections = collectionGames,
                    shots = shots,
                    history = history,
                    onOpenGame = { sheetFor = it.id },
                    onOptions = { menuFor = it.id },
                    onOpenSystem = { openSystemId = it.id },
                    onOpenCollection = { openCollectionId = it.id },
                    onNewCollection = { naming = true },
                    onOpenStats = { statsOpen = true },
                    onPlay = { vm.play(it) },
                    pageIndex = tab.ordinal + 1,
                )
                key == Tab.Cores.name -> CoresScreen(coreStates, vm::downloadCore, vm::deleteCore, pageIndex = tab.ordinal + 1)
                else -> SettingsScreen(
                    settings = vm.settings,
                    dynamicColor = dynamicColor,
                    bios = vm.bios,
                    biosVersion = biosVersion,
                    folders = folders,
                    scanning = scanning,
                    scanProgress = scanProgress,
                    onAddFolder = { folderPicker.launch(null) },
                    onRemoveFolder = vm::removeFolder,
                    onRescan = vm::rescan,
                    onImportBios = { biosPicker.launch(arrayOf("*/*")) },
                    onDownloadBios = { vm.openDownload(AppViewModel.DownloadKind.Bios) },
                    onDeleteBios = vm::deleteBios,
                    onOpenControllers = { remapOpen = true },
                    remapCount = remaps.values.sumOf { it.size },
                    onBackUp = { bios ->
                        pendingBios = bios
                        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                        backupPicker.launch("RetroHero-backup-$date.zip")
                    },
                    onRestore = { restorePicker.launch(arrayOf("application/zip", "application/octet-stream")) },
                    looks = customLooks,
                    onImportLook = { lookPicker.launch(arrayOf("application/json", "text/*", "*/*")) },
                    deltaSkins = installedDeltaSkins,
                    onImportDeltaSkin = { deltaSkinPicker.launch(arrayOf("*/*")) },
                    onDeleteDeltaSkin = { skin ->
                        com.retrohero.app.data.DeltaSkins.delete(skin)
                        deltaSkinVersion++
                        Toast.makeText(context, "Removed “${skin.name}”", Toast.LENGTH_SHORT).show()
                    },
                    onOpenStats = { statsOpen = true },
                    pageIndex = tab.ordinal + 1,
                )
            }
        }
        }
      }
    }
        if (miniGame != null && !wide) {
            MiniPlayer(
                game = miniGame,
                stat = stats[miniGame.id],
                glass = glass,
                onOpen = { sheetFor = miniGame.id },
                onPlay = { vm.play(miniGame) },
                onHide = { miniHidden = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navBarInset + FloatingNavHeight + 11.dp),
            )
        }
        if (!wide && !albumOpen) {
            FloatingNavBar(
                items = navigationItems,
                selected = tab.ordinal,
                onSelect = { tab = Tab.entries[it] },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    }

    }

    // The game page slides up over everything; back closes it.
    val detailGame = sheetFor?.let { id -> games.firstOrNull { it.id == id } }
    var lastDetail by remember { mutableStateOf<com.retrohero.app.data.Game?>(null) }
    if (detailGame != null) lastDetail = detailGame
    BackHandler(enabled = detailGame != null) { sheetFor = null }
    AnimatedVisibility(
        visible = detailGame != null,
        enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 5 } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { it / 5 } + fadeOut(tween(180)),
    ) {
        val game = lastDetail
        val system = game?.system
        if (game != null && system != null) {
            GameDetailScreen(
                game = game,
                currentCore = vm.settings.coreFor(system),
                saveSlots = remember(game.id, launchState) { vm.saveSlots(game) },
                onPlay = { slot -> vm.play(game, slot) },
                onSelectCore = { vm.setCore(system, it) },
                onDelete = if (game.isLocal) {
                    { sheetFor = null; vm.deleteGame(game) }
                } else null,
                onClose = { sheetFor = null },
                stat = stats[game.id] ?: com.retrohero.app.data.PlayStat.None,
                screenshots = remember(game.id, artVersion) { vm.screenshots(game) },
                hasCustomCover = remember(game.id, artVersion) { vm.paths.coverFile(game).exists() },
                onUseAsCover = { vm.useAsCover(game, it) },
                onClearCover = { vm.clearCover(game) },
                onShareScreenshot = { shareScreenshot(context, it) },
                pageIndex = tab.ordinal + 1,
                onDeleteScreenshot = { vm.deleteScreenshot(game, it) },
            )
        }
    }

    // Messages with Undo, floating above the nav bar.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val r = snackbar.showSnackbar(m.text, actionLabel = if (m.undo != null) "Undo" else null, withDismissAction = m.undo == null, duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) m.undo?.invoke()
            }
        }
    }
    Box(Modifier.fillMaxSize().padding(bottom = floatingNavSpace + 8.dp).navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(snackbar) { data ->
            Snackbar(
                data,
                shape = RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card),
                containerColor = com.retrohero.app.ui.theme.Tone.Surface,
                contentColor = com.retrohero.app.ui.theme.Tone.Ink,
                actionColor = com.retrohero.app.ui.theme.Salmon,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    // Long-press menu: a grid of big action tiles.
    val menuGame = menuFor?.let { id -> games.firstOrNull { it.id == id } }
    if (menuGame != null) {
        val close = { menuFor = null }
        GameMenuSheet(
            game = menuGame,
            hasResume = remember(menuGame.id, launchState) { vm.saveSlots(menuGame).any { it.first == com.retrohero.app.data.AppPaths.AUTO_SLOT } },
            isFavorite = menuGame.id in favoriteIds,
            onPlay = { close(); vm.play(menuGame) },
            onResume = { close(); vm.play(menuGame) },
            onFresh = { close(); vm.play(menuGame, com.retrohero.app.ui.screens.FreshStart) },
            onToggleFavorite = { vm.toggleFavorite(menuGame) },
            onDetails = { close(); sheetFor = menuGame.id },
            onOpenSystem = { close(); sheetFor = null; menuGame.system?.let { openSystemId = it.id } },
            onDelete = if (menuGame.isLocal) { { close(); vm.deleteGame(menuGame) } } else null,
            onDismiss = close,
            onAddTo = { close(); pickCollectionsFor = menuGame.id },
        )
    }

    // Stats and the recap story, full screen.
    BackHandler(enabled = statsOpen && recap == null) { statsOpen = false }
    AnimatedVisibility(
        visible = statsOpen,
        enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 5 } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { it / 5 } + fadeOut(tween(180)),
    ) {
        StatsScreen(history, onBack = { statsOpen = false }, onRecap = { p, s -> recap = p to s })
    }
    BackHandler(enabled = recap != null) { recap = null }
    var lastRecap by remember { mutableStateOf(recap) }
    if (recap != null) lastRecap = recap
    AnimatedVisibility(visible = recap != null, enter = fadeIn(tween(400)) + scaleIn(tween(500), initialScale = 0.92f), exit = fadeOut(tween(300))) {
        lastRecap?.let { (p, s) -> RecapScreen(p.name, s, onClose = { recap = null }) }
    }
    if (naming) {
        NameDialog("New collection", "", "Create", onDismiss = { naming = false }) {
            vm.createCollection(it)
            naming = false
        }
    }

    // Controller remapping, full screen over Settings.
    BackHandler(enabled = remapOpen) { remapOpen = false }
    AnimatedVisibility(
        visible = remapOpen,
        enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 5 } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { it / 5 } + fadeOut(tween(180)),
    ) {
        RemapScreen(
            profiles = remaps,
            controllers = rememberControllers(),
            onChange = vm::setRemaps,
            onBack = { remapOpen = false },
        )
    }

    when (val b = backupState) {
        AppViewModel.BackupState.Working -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Working…") },
            text = { LinearWavyProgressIndicator(Modifier.fillMaxWidth()) },
        )
        is AppViewModel.BackupState.Done -> AlertDialog(
            onDismissRequest = vm::dismissBackup,
            confirmButton = { TextButton(onClick = vm::dismissBackup) { Text("OK") } },
            title = { Text("Done") },
            text = { Text(b.message) },
        )
        is AppViewModel.BackupState.Failed -> AlertDialog(
            onDismissRequest = vm::dismissBackup,
            confirmButton = { TextButton(onClick = vm::dismissBackup) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(b.message) },
        )
        null -> Unit
    }

    val pickGame = pickCollectionsFor?.let { id -> games.firstOrNull { it.id == id } }
    if (pickGame != null) {
        CollectionPickerSheet(
            game = pickGame,
            collections = collections,
            onToggle = { c, inIt -> vm.setInCollection(c, pickGame, inIt) },
            onCreate = { name -> vm.createCollection(name, pickGame) },
            onDismiss = { pickCollectionsFor = null },
        )
    }
    renaming?.let { c ->
        NameDialog("Rename collection", c.name, "Rename", onDismiss = { renaming = null }) {
            vm.renameCollection(c, it)
            renaming = null
        }
    }

    // First run: a short setup, only for a fresh install with nothing added yet.
    var onboarding by rememberSaveable { mutableStateOf(!vm.settings.onboardingDone && games.isEmpty() && folders.isEmpty()) }
    AnimatedVisibility(visible = onboarding, enter = fadeIn(), exit = fadeOut(tween(350))) {
        OnboardingScreen(
            hasGames = games.isNotEmpty() || folders.isNotEmpty(),
            onAddFolder = { folderPicker.launch(null) },
            onDownloadLink = { vm.openDownload(AppViewModel.DownloadKind.Game) },
            onHomebrew = { homebrewOpen = true },
            onImportBios = { biosPicker.launch(arrayOf("*/*")) },
            onFinish = {
                vm.settings.onboardingDone = true
                onboarding = false
            },
        )
    }

    if (homebrewOpen) {
        HomebrewSheet(
            installed = vm.installedHomebrew(games),
            onGet = { vm.installHomebrew(it) },
            onDismiss = { homebrewOpen = false },
        )
    }

    download?.let { state ->
        DownloadSheet(
            state = state,
            onStart = vm::startDownload,
            onDownloadEntries = vm::downloadFolderEntries,
            onRetry = vm::openDownload,
            onPlay = vm::playDownloaded,
            onDismiss = vm::cancelDownload,
        )
    }

    LaunchDialogs(launchState, coreStates, vm)
}

@Composable
private fun LaunchDialogs(state: AppViewModel.LaunchState?, coreStates: Map<String, CoreState>, vm: AppViewModel) {
    when (state) {
        null -> Unit
        is AppViewModel.LaunchState.NeedsCore -> AlertDialog(
            onDismissRequest = vm::dismissLaunch,
            icon = { state.game.system?.let { SystemBadge(it, 56.dp) } },
            title = { Text("Get ${state.core.displayName}?", textAlign = TextAlign.Center) },
            text = {
                Text(
                    "${state.game.system?.name} games run on the ${state.core.displayName} emulator " +
                        "(${state.core.license}). It downloads once and is ready forever.",
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmCoreDownload) { Text("Download & play") } },
            dismissButton = { TextButton(onClick = vm::dismissLaunch) { Text("Not now") } },
        )
        is AppViewModel.LaunchState.Downloading -> {
            val progress = (coreStates[state.core.id] as? CoreState.Downloading)?.progress ?: 0f
            BusyDialog("Downloading ${state.core.displayName}") {
                LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
        is AppViewModel.LaunchState.Preparing -> BusyDialog("Loading ${state.game.title}", state.file) {
            ContainedLoadingIndicator(Modifier.size(72.dp))
        }
        is AppViewModel.LaunchState.Error -> AlertDialog(
            onDismissRequest = vm::dismissLaunch,
            title = { Text("Couldn't start") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = vm::dismissLaunch) { Text("OK") } },
        )
    }
}

@Composable
private fun BusyDialog(title: String, detail: String? = null, indicator: @Composable () -> Unit) {
    BasicAlertDialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(bottom = 20.dp), contentAlignment = Alignment.Center) { indicator() }
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
