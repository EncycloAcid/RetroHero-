package com.retrohero.app.ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.R
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.ui.components.LocalArtPaths
import com.retrohero.app.ui.components.LocalArtVersion
import com.retrohero.app.ui.theme.RetroHeroTheme
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setDynamicColorEnabled
import com.retrohero.app.ui.theme.setExpressivePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setExpressivePalette(vm.settings.accent)
        setDynamicColorEnabled(vm.settings.dynamicColor.value)
        Tone.setDarkMode(vm.settings.darkMode)
        lifecycleScope.launch(Dispatchers.IO) {
            DeltaSkins.installBundledDefaults(this@MainActivity, vm.paths.deltaSkins, vm.settings)
        }
        if (savedInstanceState == null) handleIncomingDeltaSkin(intent)
        setContent {
            val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()
            RetroHeroTheme(dynamicColor = dynamicColor) {
                val view = LocalView.current
                val lightBars = !Tone.isDark
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                val artVersion by vm.artVersion.collectAsStateWithLifecycle()
                CompositionLocalProvider(
                    LocalArtPaths provides vm.paths,
                    LocalArtVersion provides artVersion,
                ) {
                    M3ExpressiveHost(vm)
                }
            }
        }
    }

    private fun handleIncomingDeltaSkin(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)!!.use { stream ->
                        DeltaSkins.import(vm.paths.deltaSkins, stream)
                    }
                }
            }
            result.onSuccess { skin ->
                skin.systemIds.forEach { vm.settings.setDeltaSkinFor(it, skin.identifier) }
                Toast.makeText(this@MainActivity, "Added skin “${skin.name}”", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, err.message ?: "Couldn't import .deltaskin file.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun M3ExpressiveHost(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val games by vm.games.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val shelfStatuses by vm.shelfStatuses.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val launchState by vm.launchState.collectAsStateWithLifecycle()
    val coreStates by vm.coreStates.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()
    val backupState by vm.backupState.collectAsStateWithLifecycle()
    val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()
    val reducedMotion by vm.settings.reducedMotion.collectAsStateWithLifecycle()
    val biosVersion by vm.biosVersion.collectAsStateWithLifecycle()
    val artVersion by vm.artVersion.collectAsStateWithLifecycle()
    val raProfile by vm.raProfile.collectAsStateWithLifecycle()
    val raHardcore by vm.raHardcore.collectAsStateWithLifecycle()
    val raUnofficial by vm.raUnofficial.collectAsStateWithLifecycle()
    val raNotifications by vm.raNotifications.collectAsStateWithLifecycle()
    val raAuthState by vm.raAuth.collectAsStateWithLifecycle()

    var autoSave by remember { mutableStateOf(vm.settings.autoSave) }
    var haptics by remember { mutableStateOf(vm.settings.haptics) }
    var sharpPixels by remember { mutableStateOf(vm.settings.sharpPixels) }
    var overlayOpacity by remember { mutableFloatStateOf(vm.settings.overlayOpacity) }
    var stickAsDpad by remember { mutableStateOf(vm.settings.stickAsDpad) }
    var screenshotsToGallery by remember { mutableStateOf(vm.settings.screenshotsToGallery) }
    var fastForward by remember { mutableFloatStateOf(vm.settings.fastForwardSpeed) }
    var runAheadFrames by remember { mutableIntStateOf(vm.settings.runAheadFrames) }
    var showPerfHud by remember { mutableStateOf(vm.settings.showPerfHud) }
    var homebrewOpen by rememberSaveable { mutableStateOf(false) }
    var raSignInOpen by rememberSaveable { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<AppViewModel.Message?>(null) }
    var engineVersion by remember { mutableIntStateOf(0) }
    var patchesVersion by remember { mutableIntStateOf(0) }
    var pendingPatchGame by remember { mutableStateOf<com.retrohero.app.data.Game?>(null) }

    LifecycleResumeEffect(Unit) {
        vm.refreshStats()
        onPauseOrDispose { }
    }
    LaunchedEffect(games.size) { vm.refreshStats() }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            toastMessage = msg
        }
    }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3600)
            toastMessage = null
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importBios(uris)
    }
    val backupCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.backUp(uri, includeBios = true)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restore(uri)
    }
    val patchPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = pendingPatchGame
        pendingPatchGame = null
        if (uri != null && target != null) {
            vm.attachPatch(target, uri) {
                patchesVersion++
            }
        }
    }
    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            var count = 0
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)!!.use { stream ->
                            DeltaSkins.import(vm.paths.deltaSkins, stream)
                        }
                    }.onSuccess { skin ->
                        skin.systemIds.forEach { vm.settings.setDeltaSkinFor(it, skin.identifier) }
                        count++
                    }
                }
            }
            val msg = if (count > 0) "Imported $count controller skin${if (count == 1) "" else "s"}" else "Couldn't import .deltaskin"
            vm.say(msg)
        }
    }

    LaunchedEffect(Unit) {
        vm.launchIntents.collect {
            val options = ActivityOptions.makeCustomAnimation(context, R.anim.game_enter, R.anim.hold)
            context.startActivity(it, options.toBundle())
        }
    }

    M3ExpressiveAppView(
        games = games,
        recents = recents,
        favoriteIds = favoriteIds,
        stats = stats,
        shelfStatuses = shelfStatuses,
        folders = folders,
        scanning = scanning,
        scanProgress = scanProgress,
        autoSave = autoSave,
        haptics = haptics,
        sharpPixels = sharpPixels,
        overlayOpacity = overlayOpacity,
        stickAsDpad = stickAsDpad,
        screenshotsToGallery = screenshotsToGallery,
        reducedMotion = reducedMotion,
        fastForwardSpeed = fastForward,
        dynamicColor = dynamicColor,
        coreStates = coreStates,
        biosStatusOf = { file ->
            // Read biosVersion so BIOS status pills refresh live on import/delete
            @Suppress("UNUSED_EXPRESSION")
            biosVersion
            vm.bios.status(file)
        },
        saveSlotsFor = { g -> vm.saveSlots(g) },
        screenshotsFor = { g ->
            @Suppress("UNUSED_EXPRESSION")
            artVersion
            vm.screenshots(g)
        },
        currentCoreFor = { g ->
            @Suppress("UNUSED_EXPRESSION")
            engineVersion
            g.system?.let { vm.settings.coreFor(it) }
        },
        installedExternalEmulatorsFor = { g -> vm.installedExternalEmulators(g) },
        selectedExternalEmulatorFor = { g ->
            @Suppress("UNUSED_EXPRESSION")
            engineVersion
            vm.selectedExternalEmulator(g)
        },
        cachedDossierFor = { g -> vm.cachedDossier(g) },
        loadDossierFor = { g, force -> vm.loadDossier(g, force) },
        toastMessage = toastMessage,
        onDismissToast = { toastMessage = null },
        onPlay = { g, slot -> vm.play(g, slot) },
        onShufflePlay = { vm.playRandom() },
        onToggleFavorite = { g -> vm.toggleFavorite(g) },
        onSetShelfStatus = { g, status -> vm.setShelfStatus(g, status) },
        onSelectCore = { g, core ->
            g.system?.let { vm.setCore(it, core) }
            engineVersion++
        },
        onSelectExternalEmulator = { g, ext ->
            g.system?.let { vm.setExternalEmulator(it, ext) }
            engineVersion++
        },
        onUseScreenshotAsCover = { g, shot -> vm.useAsCover(g, shot) },
        onClearCover = { g -> vm.clearCover(g) },
        onDeleteGame = { g -> vm.deleteGame(g) },
        onSetAutoSave = { v -> autoSave = v; vm.settings.autoSave = v },
        onSetHaptics = { v -> haptics = v; vm.settings.haptics = v },
        onSetSharpPixels = { v -> sharpPixels = v; vm.settings.sharpPixels = v },
        onSetOverlayOpacity = { v -> overlayOpacity = v; vm.settings.overlayOpacity = v },
        onSetStickAsDpad = { v -> stickAsDpad = v; vm.settings.stickAsDpad = v },
        onSetScreenshotsToGallery = { v -> screenshotsToGallery = v; vm.settings.screenshotsToGallery = v },
        onSetReducedMotion = { v -> vm.settings.setReducedMotion(v) },
        onSetFastForward = { v -> fastForward = v; vm.settings.fastForwardSpeed = v },
        onSetDynamicColor = { enabled -> vm.settings.setDynamicColor(enabled); setDynamicColorEnabled(enabled) },
        onSelectPalette = { id -> vm.settings.accent = id; setExpressivePalette(id) },
        onSetDarkMode = { dark -> vm.settings.darkMode = dark; Tone.setDarkMode(dark) },
        onAddFolder = { folderPicker.launch(null) },
        onRemoveFolder = { uri ->
            vm.removeFolder(uri)
            vm.say("Folder removed")
        },
        onRescan = vm::rescan,
        onOpenHomebrew = { homebrewOpen = true },
        onOpenDownload = { vm.openDownload(AppViewModel.DownloadKind.Game) },
        onOpenDownloadBios = { vm.openDownload(AppViewModel.DownloadKind.Bios) },
        onImportBios = { biosPicker.launch(arrayOf("*/*")) },
        onDeleteBios = { name ->
            vm.deleteBios(name)
            vm.say("Removed $name")
        },
        onImportSkin = { skinPicker.launch(arrayOf("*/*")) },
        onBackup = {
            val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            backupCreator.launch("retrohero-backup-$stamp.zip")
        },
        onRestore = { restorePicker.launch(arrayOf("application/zip", "*/*")) },
        onDownloadCore = vm::downloadCore,
        onDownloadAllNeededCores = vm::downloadCores,
        onDeleteCore = vm::deleteCore,
        raProfile = raProfile,
        raHardcore = raHardcore,
        raUnofficial = raUnofficial,
        raNotifications = raNotifications,
        cachedRaFor = { g -> vm.cachedRaGame(g) },
        loadRaFor = { g, force -> vm.loadRaGame(g, force) },
        onOpenRaSignIn = {
            vm.resetRaAuthError()
            raSignInOpen = true
        },
        onSignOutRa = vm::signOutRetroAchievements,
        onSetRaHardcore = { vm.settings.setRaHardcore(it) },
        onSetRaUnofficial = { vm.settings.setRaUnofficial(it) },
        onSetRaNotifications = { vm.settings.setRaNotifications(it) },
        runAheadFrames = runAheadFrames,
        showPerfHud = showPerfHud,
        onSetRunAhead = { f -> runAheadFrames = f; vm.settings.runAheadFrames = f },
        onSetShowPerfHud = { on -> showPerfHud = on; vm.settings.showPerfHud = on },
        patchesFor = { g ->
            @Suppress("UNUSED_EXPRESSION")
            patchesVersion
            vm.patchesFor(g)
        },
        onAttachPatch = { g ->
            pendingPatchGame = g
            patchPicker.launch(arrayOf("*/*"))
        },
        onPlayWithPatch = { g, patch -> vm.play(g, slot = null, patch = patch) },
        onTogglePatchSeparateSave = { g, patch, sep ->
            vm.setPatchSeparateSave(g, patch.id, sep)
            patchesVersion++
        },
        onDeletePatch = { g, patch ->
            vm.deletePatch(g, patch.id)
            patchesVersion++
        },
    )

    if (raSignInOpen) {
        M3RetroAchievementsSignInDialog(
            authState = raAuthState,
            onSignIn = { u, p ->
                vm.signInRetroAchievements(u, p) { raSignInOpen = false }
            },
            onDismiss = {
                vm.resetRaAuthError()
                raSignInOpen = false
            },
        )
    }

    if (homebrewOpen) {
        M3HomebrewSheet(
            installedUrls = vm.installedHomebrew(games),
            onInstall = { item ->
                homebrewOpen = false
                vm.installHomebrew(item)
            },
            onDismiss = { homebrewOpen = false },
        )
    }

    launchState?.let { state ->
        M3LaunchDialog(
            state = state,
            onConfirmDownload = vm::confirmCoreDownload,
            onDismiss = vm::dismissLaunch,
        )
    }

    download?.let { state ->
        M3DownloadDialog(
            state = state,
            onStart = vm::startDownload,
            onDownloadFolder = vm::downloadFolderEntries,
            onPlayGame = vm::playDownloaded,
            onCancel = vm::cancelDownload,
        )
    }

    backupState?.let { state ->
        M3BackupDialog(
            state = state,
            onDismiss = vm::dismissBackup,
        )
    }
}
