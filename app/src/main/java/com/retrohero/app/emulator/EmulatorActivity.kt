package com.retrohero.app.emulator

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import com.retrohero.app.data.Cheat
import com.retrohero.app.data.CheatFinder
import com.retrohero.app.data.CheatPackMatch
import com.retrohero.app.data.Cheats
import com.retrohero.app.data.DeltaAction
import com.retrohero.app.data.DeltaSkin
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.data.PlayTime
import com.retrohero.app.data.Remap
import com.retrohero.app.data.Looks
import com.retrohero.app.data.PadSkin
import com.retrohero.app.ui.theme.Tone
import com.retrohero.app.ui.theme.setAccent
import com.retrohero.app.data.ScreenFilter
import java.io.File
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import com.retrohero.app.R
import android.os.Process
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import com.retrohero.app.ui.components.SegmentedGroup
import com.retrohero.app.ui.components.MonoSwitch
import com.retrohero.app.ui.components.ShapeIcon
import com.retrohero.app.ui.components.segmentColors
import com.retrohero.app.ui.components.SystemBadge
import com.retrohero.app.ui.components.rememberPressFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.Game
import com.retrohero.app.data.GameSystem
import com.retrohero.app.data.Settings
import com.retrohero.app.ui.theme.RetroHeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/**
 * Runs one game. Declared in its own process (":game") so every launch starts with a fresh
 * core: many libretro cores keep global state and cannot be cleanly unloaded and reloaded.
 */
class EmulatorActivity : ComponentActivity() {

    private sealed interface Phase {
        data object Loading : Phase
        data object Running : Phase
        data class Failed(val message: String) : Phase
    }

    private lateinit var game: Game
    private lateinit var system: GameSystem
    private lateinit var paths: AppPaths
    private lateinit var settings: Settings
    private lateinit var input: InputMapper
    private lateinit var optionsStore: CoreOptionsStore
    private lateinit var gamePrefs: GamePrefs
    private lateinit var padLayouts: PadLayouts
    private lateinit var audioHaptics: AudioHapticsEngine
    private var activeCoreId: String = ""
    private var preparedSave: com.retrohero.app.data.SavePortability.PreparedSave? = null
    private var padSkin by mutableStateOf(Looks.Console)
    private var currentCoreId: String = ""
    private var challengeBanner by mutableStateOf<String?>(null)
    private var deltaSkinsForSystem: List<DeltaSkin> = emptyList()
    private var activeDeltaSkin by mutableStateOf<DeltaSkin?>(null)

    private var phase by mutableStateOf<Phase>(Phase.Loading)
    private var menuOpen by mutableStateOf(false)
    private var showPad by mutableStateOf(true)
    private var audioHapticsOn by mutableStateOf(false)
    private var fastForwardToggle by mutableStateOf(false)
    private var slowMotion by mutableStateOf(false)
    private var runAheadFrames by mutableIntStateOf(0)
    private var showPerfHud by mutableStateOf(false)
    private var perfFps by mutableFloatStateOf(60f)
    private var perfMs by mutableFloatStateOf(0f)
    private var perfActiveRa by mutableIntStateOf(0)
    private var aspect by mutableFloatStateOf(4f / 3f)
    private var frameSize by mutableStateOf(IntSize.Zero)
    private var filter by mutableIntStateOf(0)
    private var integerScale by mutableStateOf(false)
    private var rewindOn by mutableStateOf(false)
    private var cheats by mutableStateOf<List<Cheat>>(emptyList())
    private var cheatsSupported by mutableStateOf(false)
    private var cheatSearching by mutableStateOf(false)
    private var cheatSearchSource by mutableStateOf<String?>(null)
    private var cheatSearchError by mutableStateOf<String?>(null)
    private var cheatAlternatePacks by mutableStateOf<List<CheatPackMatch>>(emptyList())
    private var activeCheatPack by mutableStateOf<CheatPackMatch?>(null)
    private var cheatAutoTried by mutableStateOf(false)
    private var flash by mutableIntStateOf(0)
    /** Touch-control editing: null when not editing. */
    private var editingPad by mutableStateOf(false)
    private var padAdjust by mutableStateOf<Map<String, PadAdjust>>(emptyMap())
    private var padSelected by mutableStateOf<String?>(null)
    private var padLandscape = false
    private var translateState by mutableStateOf<TranslateState>(TranslateState.Idle)
    private var translateLang by mutableStateOf(TranslateSourceLang.JAPANESE)

    // Zero-config multi-disc runtime state
    private var discCount by mutableIntStateOf(0)
    private var currentDisc by mutableIntStateOf(0)
    private var discLabels by mutableStateOf<List<String>>(emptyList())
    private var discPickerOpen by mutableStateOf(false)
    private var swappingDisc by mutableStateOf(false)

    // Tear-free 60/90/120 Hz/LTPO display sync & 120 Hz OLED BFI
    private var activeSurface: Surface? = null
    private var vsyncLock by mutableStateOf(true)
    private var bfi120Hz by mutableStateOf(false)
    private var displayHz by mutableFloatStateOf(60f)
    private var bfiCapable by mutableStateOf(false)
    private var perfVsyncLocked by mutableStateOf(false)
    private var perfBfiActive by mutableStateOf(false)
    private var choreographerActive = false
    private val vsyncCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!choreographerActive || shutDown) return
            val liveHz = DisplaySync.currentRefreshRate(this@EmulatorActivity)
            if (kotlin.math.abs(liveHz - displayHz) >= 0.5f) displayHz = liveHz
            NativeBridge.onVsync(frameTimeNanos, liveHz)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // Smart Touch Ergonomics & Gyro-to-Analog steering
    private var smartTouch by mutableStateOf(SmartTouchConfig())
    private var gyroController: GyroSteeringController? = null
    private var gyroDigitalLeft = false
    private var gyroDigitalRight = false

    // Automatic Per-Game Hardware & Shader Intelligence ("Authentic / Enhanced")
    private var visualMode by mutableStateOf(com.retrohero.app.data.VisualMode.ENHANCED)
    private var activeProfile by mutableStateOf<com.retrohero.app.data.HardwareProfile?>(null)
    private var colorProfile by mutableIntStateOf(com.retrohero.app.data.ColorProfile.NONE)
    private var graphicsConfig by mutableStateOf(com.retrohero.app.data.GraphicsConfig())
    private var ambilightMode by mutableStateOf(AmbilightMode.VIVID)
    private var ambilightState by mutableStateOf(AmbilightState.Off)
    private val ambilightTracker = AmbilightTracker()
    private var activePatchName by mutableStateOf<String?>(null)
    private var activePatchFormat by mutableStateOf<String?>(null)
    private var activePatchHeaderNote by mutableStateOf<String?>(null)
    private var activeRomCrc: String? = null

    // #8 Real-Time Overlay Translation & Spoiler-Free Pocket Guide
    private var liveTranslateOn by mutableStateOf(false)
    private var liveTranslateBlocks by mutableStateOf<List<TranslatedBlock>>(emptyList())
    private var liveTranslateScanning by mutableStateOf(false)
    private var liveTranslateJob: kotlinx.coroutines.Job? = null
    private var pocketGuideOpen by mutableStateOf(false)
    private var pocketNotes by mutableStateOf("")
    private var browserOpen by mutableStateOf(false)
    private var browserLastUrl by mutableStateOf<String?>(null)
    private var browserBookmarkedUrl by mutableStateOf<String?>(null)
    private var browserWebView: android.webkit.WebView? = null

    // #9 Playable Challenge Snapshots (Evolving Moment Capsules + Input Ghost + Win Conditions)
    private var challengeRecording by mutableStateOf(false)
    private var challengeRecStartMs = 0L
    private var challengeRecElapsedMs by mutableStateOf(0L)
    private var challengeRecStartState: File? = null
    private var challengeRecStartPng: File? = null
    private val challengeRecFrames = ArrayList<com.retrohero.app.data.GhostFrame>()
    private var challengeRecTitle = ""
    private var challengeRecNote = ""
    private var challengeRecWin: com.retrohero.app.data.ChallengeWinCondition? = null

    private var activeChallengeStateFile: File? = null
    private var activeChallengeTitle by mutableStateOf<String?>(null)
    private var activeChallengeGhost by mutableStateOf<List<com.retrohero.app.data.GhostFrame>>(emptyList())
    private var activeChallengeTargetMs by mutableStateOf(0L)
    private var activeChallengeWin by mutableStateOf<com.retrohero.app.data.ChallengeWinCondition?>(null)
    private var challengeElapsedMs by mutableStateOf(0L)
    private var challengeGhostMask by mutableIntStateOf(0)
    private var challengeCleared by mutableStateOf(false)
    private var challengeClearedTimeMs by mutableStateOf<Long?>(null)
    private var challengeStartRealtime = 0L

    private val challengePicker = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) loadChallengeCapsuleFromUri(uri)
    }

    // #10 3-Second Mobile Rollback Netplay
    private val netplaySession by lazy { RollbackNetplaySession(lifecycleScope) }

    // Play time: counted only while the game is actually running on screen.
    private var runningSince = 0L
    private var pendingSession = true

    private var started = false
    private var shutDown = false
    private var fastForwardHeld = false

    private data class RaBanner(
        val title: String,
        val subtitle: String,
        val badgeUrl: String?,
        val points: Int?,
        val hardcore: Boolean,
    )

    private var raBanner by mutableStateOf<RaBanner?>(null)
    private var raSessionHardcore by mutableStateOf(false)
    private var raSessionGame by mutableStateOf<com.retrohero.app.data.RaGameData?>(null)
    private var raJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameJson = intent.getStringExtra(EXTRA_GAME)
        val corePath = intent.getStringExtra(EXTRA_CORE_PATH)
        val coreId = intent.getStringExtra(EXTRA_CORE_ID)
        val romPath = intent.getStringExtra(EXTRA_ROM_PATH)
        if (gameJson == null || corePath == null || coreId == null || romPath == null) {
            finish()
            return
        }
        currentCoreId = coreId
        challengeBanner = intent.getStringExtra(EXTRA_CHALLENGE_TITLE)?.takeIf { it.isNotBlank() }
        game = Game.fromJson(JSONObject(gameJson))
        system = game.system ?: run { finish(); return }
        paths = AppPaths(this)
        settings = Settings(this)
        audioHaptics = AudioHapticsEngine(this)
        audioHapticsOn = settings.audioHaptics
        translateLang = TranslateSourceLang.fromCode(settings.translateSourceLang)
        Tone.setDarkMode(settings.darkMode)
        input = InputMapper(settings.stickAsDpad, Remap.load(paths.remapFile))
        optionsStore = CoreOptionsStore(paths.coreOptionsFile(coreId))
        gamePrefs = GamePrefs(this, settings.sharpPixels)
        // The chosen look: an accent and a pad skin (imported looks carry both).
        val custom = Looks.custom(paths.looks)
        padSkin = settings.padSkin.let { id -> custom.firstOrNull { "look:" + it.id == id }?.pad ?: Looks.skin(id) }
        setAccent(settings.accent.let { id -> custom.firstOrNull { "look:" + it.id == id }?.accent ?: Looks.accent(id) })
        deltaSkinsForSystem = DeltaSkins.forSystem(paths.deltaSkins, system.id)
        val selectedDeltaId = settings.deltaSkinFor(system.id)
        activeDeltaSkin = deltaSkinsForSystem.firstOrNull { it.identifier == selectedDeltaId }
        padLayouts = PadLayouts(paths.padLayoutFile)
        filter = gamePrefs.filter(system.id)
        integerScale = gamePrefs.integerScale(system.id)
        vsyncLock = gamePrefs.vsyncLock
        bfi120Hz = gamePrefs.bfi120Hz
        ambilightMode = gamePrefs.ambilightMode
        displayHz = DisplaySync.currentRefreshRate(this)
        smartTouch = SmartTouchConfig(
            thumbChordZones = gamePrefs.thumbChordZones,
            shoulderLatch = gamePrefs.shoulderLatch,
            comboMacroEnabled = gamePrefs.comboMacroEnabled,
            macroIndex = gamePrefs.macroIndex(system.id),
            gyroSteering = gamePrefs.gyroSteering,
            portraitMode = gamePrefs.portraitTouchMode,
            leftHandedPortrait = gamePrefs.leftHandedPortrait,
        )
        gyroController = GyroSteeringController(
            context = this,
            displayRotationProvider = {
                runCatching {
                    if (Build.VERSION.SDK_INT >= 30) display?.rotation ?: Surface.ROTATION_0
                    else @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
                }.getOrDefault(Surface.ROTATION_0)
            },
            onSteer = { axisX, digLeft, digRight ->
                if (started && !shutDown && !menuOpen && !editingPad) {
                    NativeBridge.setAnalog(0, 0, 0, axisX)
                    val digitalSystem = system.id in setOf("nes", "snes", "gb", "gbc", "gba", "genesis", "sms", "gg", "pce")
                    if (digitalSystem) {
                        if (digLeft != gyroDigitalLeft) {
                            gyroDigitalLeft = digLeft
                            NativeBridge.setButton(0, com.retrohero.app.data.RetroPad.LEFT, digLeft)
                        }
                        if (digRight != gyroDigitalRight) {
                            gyroDigitalRight = digRight
                            NativeBridge.setButton(0, com.retrohero.app.data.RetroPad.RIGHT, digRight)
                        }
                    }
                }
            },
        )
        cheats = Cheats.load(paths.cheatsFile(game))
        pocketNotes = runCatching { paths.notesFile(game).takeIf { it.exists() }?.readText().orEmpty() }.getOrDefault("")
        browserBookmarkedUrl = runCatching {
            paths.browserBookmarkFile(game).takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
        input.onRewind = { held -> if (rewindOn) NativeBridge.setRewinding(held) }
        input.rewindKeyEnabled = system.id != "psx"
        input.onFastForward = { held -> fastForwardHeld = held; applyFastForward() }
        input.onMenu = { if (phase is Phase.Running) setMenu(!menuOpen) }

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    browserOpen -> {
                        if (browserWebView?.canGoBack() == true) browserWebView?.goBack()
                        else closeInGameBrowser()
                    }
                    pocketGuideOpen -> closePocketGuide()
                    discPickerOpen -> discPickerOpen = false
                    translateState !is TranslateState.Idle -> dismissTranslate()
                    editingPad -> finishPadEdit(save = true)
                    phase is Phase.Running -> setMenu(!menuOpen)
                    else -> finish()
                }
            }
        })

        setContent { RetroHeroTheme(dynamicColor = settings.dynamicColor.value) { EmulatorScreen() } }

        lifecycleScope.launch { boot(coreId, corePath, romPath) }
    }

    private suspend fun boot(coreId: String, corePath: String, romPath: String) {
        activeCoreId = coreId
        val externalState = intent.getStringExtra(EXTRA_STATE_PATH)?.let(::File)?.takeIf { it.isFile }
        val requested = if (intent.hasExtra(EXTRA_LOAD_SLOT)) intent.getIntExtra(EXTRA_LOAD_SLOT, AppPaths.AUTO_SLOT) else null
        val patchPath = intent.getStringExtra(EXTRA_PATCH_PATH)
        val patchName = intent.getStringExtra(EXTRA_PATCH_NAME)
        val patchFormatLabel = intent.getStringExtra(EXTRA_PATCH_FORMAT)
        raSessionHardcore = settings.raProfile.value != null && settings.raHardcore.value &&
            externalState == null && requested == null && cheats.none { it.enabled }
        var bootNotice: String? = null
        var resolvedProfile: com.retrohero.app.data.HardwareProfile? = null
        var resolvedMode = com.retrohero.app.data.VisualMode.ENHANCED
        var resolvedPatchFormat = patchFormatLabel
        var resolvedHeaderNote: String? = null
        val error = withContext(Dispatchers.IO) {
            val prep = com.retrohero.app.data.SavePortability.prepareForCore(game, coreId, paths)
            preparedSave = prep
            if (prep.converted && prep.note.isNotBlank()) bootNotice = prep.note

            var patchedBytes: ByteArray? = null
            var crcHex: String? = null
            if (!patchPath.isNullOrBlank()) {
                val pFile = File(patchPath)
                if (!pFile.exists()) {
                    return@withContext "Attached patch file is missing."
                }
                val romFile = File(romPath)
                if (!romFile.exists()) {
                    return@withContext "Original ROM file could not be opened."
                }
                if (romFile.length() > 128L * 1024L * 1024L) {
                    return@withContext "In-memory soft-patching supports ROMs up to 128 MB."
                }
                val patchRes = try {
                    com.retrohero.app.data.RomPatcher.applyInMemory(
                        sourceRom = romFile.readBytes(),
                        patchBytes = pFile.readBytes(),
                        systemId = system.id,
                        fileName = pFile.name,
                    )
                } catch (e: Exception) {
                    return@withContext e.message ?: "Failed to apply ROM patch."
                }
                patchedBytes = patchRes.data
                crcHex = patchRes.targetCrc32Hex
                resolvedPatchFormat = patchRes.format.label
                resolvedHeaderNote = patchRes.headerNote
                bootNotice = buildString {
                    append("Soft-patched in memory · ")
                    append(patchName ?: pFile.nameWithoutExtension)
                    append(" (")
                    append(patchRes.format.label)
                    append(')')
                    if (!patchRes.headerNote.isNullOrBlank()) {
                        append(" · ")
                        append(patchRes.headerNote)
                    }
                }
            } else {
                crcHex = com.retrohero.app.data.HardwareProfiles.quickCrc32Hex(File(romPath))
            }

            activeRomCrc = crcHex
            resolvedMode = com.retrohero.app.data.VisualMode.fromId(gamePrefs.visualMode(game.id))
                ?: com.retrohero.app.data.HardwareProfiles.defaultModeFor(game, crcHex)
            val prof = com.retrohero.app.data.HardwareProfiles.resolve(game, coreId, resolvedMode, crcHex)
            resolvedProfile = prof

            val savedDisc = gamePrefs.lastDisc(game.id)
            if (savedDisc > 0) NativeBridge.setInitialDisc(savedDisc)
            val gfx = gamePrefs.graphicsConfig(system.id, com.retrohero.app.data.GraphicsCoreOptions.is3DCore(coreId))
            graphicsConfig = gfx
            com.retrohero.app.data.CoreTuning.defaultsFor(coreId).forEach { (k, v) -> NativeBridge.setOption(k, v) }
            prof.coreOptions.forEach { (k, v) -> NativeBridge.setOption(k, v) }
            com.retrohero.app.data.GraphicsCoreOptions.optionsFor(coreId, gfx).forEach { (k, v) -> NativeBridge.setOption(k, v) }
            optionsStore.load().forEach { (k, v) -> NativeBridge.setOption(k, v) }
            if (!NativeBridge.loadCore(corePath, paths.system.absolutePath, paths.saves.absolutePath)) {
                return@withContext NativeBridge.getLastError()
            }
            val loaded = if (patchedBytes != null) {
                NativeBridge.loadGameMemory(
                    virtualPath = romPath,
                    romBytes = patchedBytes,
                    sramPath = prep.activeFile.absolutePath,
                    scratchDir = paths.romCache.absolutePath,
                )
            } else {
                NativeBridge.loadGame(romPath, prep.activeFile.absolutePath)
            }
            if (!loaded) {
                return@withContext NativeBridge.getLastError()
            }
            if (externalState != null) {
                NativeBridge.loadState(externalState.absolutePath)
            } else {
                val resumeSlot = when {
                    raSessionHardcore -> null // Hardcore mode starts cleanly from boot
                    requested != null -> requested
                    settings.autoSave -> AppPaths.AUTO_SLOT
                    else -> null
                }
                val resume = resumeSlot?.let { paths.stateFile(game, it) }
                if (resumeSlot != null && resume != null && resume.exists()) {
                    val savedByCore = gamePrefs.stateCore(game.id, resumeSlot)
                    if (savedByCore != null && savedByCore != coreId) {
                        val coreName = system.cores.firstOrNull { it.id == coreId }?.displayName ?: coreId
                        bootNotice = "Switched to $coreName · Battery save carried over cleanly"
                    } else {
                        NativeBridge.loadState(resume.absolutePath)
                    }
                }
            }
            null
        }
        if (error != null) {
            phase = Phase.Failed(error.ifEmpty { "Failed to start the game." })
            return
        }
        activePatchName = patchName
        activePatchFormat = resolvedPatchFormat
        activePatchHeaderNote = resolvedHeaderNote
        visualMode = resolvedMode
        activeProfile = resolvedProfile
        val prof = resolvedProfile
        if (prof != null) {
            if (!gamePrefs.hasCustomFilter(system.id) || gamePrefs.visualMode(game.id) != null) {
                filter = prof.filter
            }
            colorProfile = gamePrefs.colorProfileOverride(game.id) ?: prof.colorProfile
        }
        NativeBridge.setUpscaleFactor(graphicsConfig.upscale)
        NativeBridge.setAmbilightEnabled(ambilightMode.isEnabled)
        NativeBridge.setFilter(filter)
        NativeBridge.setColorProfile(colorProfile)
        runAheadFrames = settings.runAheadFrames
        showPerfHud = settings.showPerfHud
        NativeBridge.setRunAhead(runAheadFrames)
        rewindOn = gamePrefs.rewind && !raSessionHardcore
        NativeBridge.setRewindEnabled(rewindOn)
        cheatsSupported = NativeBridge.supportsCheats()
        if (!raSessionHardcore) applyCheats()
        aspect = NativeBridge.getAspectRatio()
        refreshDiscState()
        applyDisplaySyncLock()
        startVsyncFeed()
        NativeBridge.start()
        started = true
        phase = Phase.Running
        syncGyroSteering()
        bootNotice?.let { toast(it) }
        startClock()
        startRetroAchievements(romPath)
    }

    private fun applyVisualMode(newMode: com.retrohero.app.data.VisualMode) {
        visualMode = newMode
        gamePrefs.setVisualMode(game.id, newMode.id)
        gamePrefs.setColorProfileOverride(game.id, null)
        val prof = com.retrohero.app.data.HardwareProfiles.resolve(game, activeCoreId, newMode, activeRomCrc)
        activeProfile = prof
        filter = prof.filter
        colorProfile = prof.colorProfile
        gamePrefs.setFilter(system.id, filter)
        NativeBridge.setFilter(filter)
        NativeBridge.setColorProfile(colorProfile)
        prof.coreOptions.forEach { (k, v) -> NativeBridge.setOption(k, v) }
    }

    private fun setColorMatrix(profile: Int) {
        colorProfile = profile
        gamePrefs.setColorProfileOverride(game.id, profile)
        NativeBridge.setColorProfile(profile)
    }

    private fun updateSmartTouch(transform: (SmartTouchConfig) -> SmartTouchConfig) {
        val next = transform(smartTouch)
        smartTouch = next
        gamePrefs.thumbChordZones = next.thumbChordZones
        gamePrefs.shoulderLatch = next.shoulderLatch
        gamePrefs.comboMacroEnabled = next.comboMacroEnabled
        gamePrefs.setMacroIndex(system.id, next.macroIndex)
        gamePrefs.gyroSteering = next.gyroSteering
        gamePrefs.portraitTouchMode = next.portraitMode
        gamePrefs.leftHandedPortrait = next.leftHandedPortrait
        syncGyroSteering()
    }

    private fun syncGyroSteering() {
        val wantGyro = smartTouch.gyroSteering && started && !shutDown && !menuOpen && !editingPad
        if (wantGyro) {
            gyroController?.start()
        } else {
            gyroController?.stop()
            if (gyroDigitalLeft) {
                gyroDigitalLeft = false
                if (started && !shutDown) NativeBridge.setButton(0, com.retrohero.app.data.RetroPad.LEFT, false)
            }
            if (gyroDigitalRight) {
                gyroDigitalRight = false
                if (started && !shutDown) NativeBridge.setButton(0, com.retrohero.app.data.RetroPad.RIGHT, false)
            }
        }
    }

    private fun startVsyncFeed() {
        if (choreographerActive || shutDown) return
        choreographerActive = true
        Choreographer.getInstance().postFrameCallback(vsyncCallback)
    }

    private fun stopVsyncFeed() {
        if (!choreographerActive) return
        choreographerActive = false
        Choreographer.getInstance().removeFrameCallback(vsyncCallback)
    }

    private fun applyDisplaySyncLock() {
        val coreFps = if (started || phase !is Phase.Failed) {
            runCatching { NativeBridge.getCoreFps().toDouble() }.getOrDefault(60.0)
        } else 60.0
        val modes = DisplaySync.supportedRefreshRates(this)
        val plan = DisplaySync.chooseTargetRefreshRate(coreFps, modes, bfiRequested = bfi120Hz)
        bfiCapable = plan.bfiCapable
        val targetHz = if (vsyncLock) plan.targetRefreshHz else DisplaySync.currentRefreshRate(this)
        DisplaySync.applyHardwareLock(activeSurface, window, targetHz)
        displayHz = DisplaySync.currentRefreshRate(this)
        NativeBridge.setVsyncConfig(vsyncLock, bfi120Hz && plan.bfiCapable)
    }

    private fun setDisplaySync(vsync: Boolean = vsyncLock, bfi: Boolean = bfi120Hz) {
        vsyncLock = vsync
        bfi120Hz = bfi
        gamePrefs.vsyncLock = vsync
        gamePrefs.bfi120Hz = bfi
        applyDisplaySyncLock()
    }

    /** Atomically flushes the core's SRAM and commits canonicalized bytes into the universal .srm save. */
    private fun flushCanonicalSram(cleanupTemp: Boolean = false) {
        val prep = preparedSave ?: return
        NativeBridge.saveSram(prep.activeFile.absolutePath)
        com.retrohero.app.data.SavePortability.commitFromCore(
            activeFile = prep.activeFile,
            canonicalFile = prep.canonicalFile,
            systemId = system.id,
            coreId = activeCoreId,
            cleanupTemp = cleanupTemp,
        )
    }

    private fun refreshDiscState() {
        val count = NativeBridge.getDiscCount()
        discCount = count
        if (count > 1) {
            currentDisc = NativeBridge.getCurrentDisc().coerceIn(0, count - 1)
            val nativeLabels = NativeBridge.getDiscLabels().toList()
            discLabels = (0 until count).map { idx ->
                val grouped = game.discs.getOrNull(idx)
                val raw = nativeLabels.getOrNull(idx).orEmpty()
                when {
                    grouped != null -> grouped.label
                    raw.isNotBlank() -> raw
                    else -> "Disc ${idx + 1}"
                }
            }
        } else {
            currentDisc = 0
            discLabels = emptyList()
        }
    }

    private fun performDiscSwap(targetIndex: Int, closeMenuAfter: Boolean = false) {
        if (swappingDisc || discCount <= 1 || targetIndex !in 0 until discCount) return
        swappingDisc = true
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                flushCanonicalSram()
                NativeBridge.swapDisc(targetIndex)
            }
            swappingDisc = false
            if (ok) {
                refreshDiscState()
                gamePrefs.setLastDisc(game.id, currentDisc)
                discPickerOpen = false
                if (closeMenuAfter) setMenu(false)
                val label = discLabels.getOrElse(currentDisc) { "Disc ${currentDisc + 1}" }
                toast("Swapped to $label · Unified save active")
            } else {
                toast(NativeBridge.getLastError().ifEmpty { "Couldn't swap disc" })
            }
        }
    }

    private fun demoteRaHardcore(reason: String) {
        if (!raSessionHardcore) return
        raSessionHardcore = false
        toast("RetroAchievements: switched to Softcore ($reason)")
    }

    private fun startRetroAchievements(romPath: String) {
        val profile = settings.raProfile.value ?: return
        val notifications = settings.raNotifications.value
        val includeUnofficial = settings.raUnofficial.value
        val raRepo = com.retrohero.app.data.RetroAchievementsRepository(this, File(filesDir, "ra_cache"))
        raJob?.cancel()
        raJob = lifecycleScope.launch(Dispatchers.IO) {
            val hash = com.retrohero.app.data.RetroAchievementsHasher.hashFile(File(romPath), system.id)
                ?: return@launch
            val gameId = com.retrohero.app.data.RetroAchievementsApi.lookupGameId(hash)
                ?: return@launch
            val data = com.retrohero.app.data.RetroAchievementsApi.fetchPatchAndUnlocks(
                profile = profile,
                gameId = gameId,
                romHash = hash,
                hardcore = raSessionHardcore,
                includeUnofficial = includeUnofficial,
            ) ?: return@launch

            com.retrohero.app.data.RetroAchievementsApi.startSession(profile, gameId, raSessionHardcore, hash)
            withContext(Dispatchers.Main) {
                raSessionGame = data
                if (notifications) {
                    val mode = if (raSessionHardcore) "Hardcore" else "Softcore"
                    showRaBanner(
                        RaBanner(
                            title = data.title.ifBlank { game.title },
                            subtitle = "${data.unlockedCount}/${data.totalCount} trophies · $mode",
                            badgeUrl = data.imageIconUrl,
                            points = null,
                            hardcore = raSessionHardcore,
                        ),
                    )
                }
            }

            val triggers = data.achievements
                .filter { !it.unlocked }
                .mapNotNull { com.retrohero.app.data.RaLogicEvaluator.compile(it) }
            if (triggers.isEmpty()) return@launch

            var lastPingMs = SystemClock.elapsedRealtime()
            while (started && !shutDown) {
                if (!menuOpen && !editingPad) {
                    val sysRam = NativeBridge.readMemory(2, 8 * 1024 * 1024) // RETRO_MEMORY_SYSTEM_RAM
                    val saveRam = NativeBridge.readMemory(0, 512 * 1024)     // RETRO_MEMORY_SAVE_RAM
                    if (sysRam != null || saveRam != null) {
                        for (t in triggers) {
                            if (com.retrohero.app.data.RaLogicEvaluator.step(t, system.id, sysRam, saveRam)) {
                                val ach = t.achievement
                                val hc = raSessionHardcore
                                val awarded = com.retrohero.app.data.RetroAchievementsApi.awardAchievement(
                                    profile = profile,
                                    achievementId = ach.id,
                                    hardcore = hc,
                                    romHash = hash,
                                )
                                awarded.onSuccess { (score, soft) ->
                                    settings.updateRaScores(score, soft)
                                }
                                raRepo.markUnlockedLocally(game, ach.id, hc)
                                withContext(Dispatchers.Main) {
                                    raSessionGame = raSessionGame?.let { cur ->
                                        cur.copy(
                                            achievements = cur.achievements.map { item ->
                                                if (item.id == ach.id) item.copy(unlocked = true, unlockedHardcore = item.unlockedHardcore || hc)
                                                else item
                                            },
                                        )
                                    }
                                    if (notifications) {
                                        showRaBanner(
                                            RaBanner(
                                                title = ach.title,
                                                subtitle = ach.description,
                                                badgeUrl = ach.badgeUrl,
                                                points = ach.points,
                                                hardcore = hc,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastPingMs >= 120_000L) {
                        lastPingMs = now
                        com.retrohero.app.data.RetroAchievementsApi.ping(profile, gameId, "Playing ${game.title} on RetroHero")
                    }
                }
                delay(66L) // ~15 Hz RAM evaluation
            }
        }
    }

    private fun showRaBanner(banner: RaBanner) {
        raBanner = banner
        lifecycleScope.launch {
            delay(4400L)
            if (raBanner === banner) raBanner = null
        }
    }

    private fun setMenu(open: Boolean) {
        menuOpen = open
        if (started) NativeBridge.setPaused(open)
        if (open) stopClock() else startClock()
        syncGyroSteering()
    }

    private fun startClock() {
        if (started && !shutDown && runningSince == 0L && !menuOpen) runningSince = SystemClock.elapsedRealtime()
    }

    /** Stops counting and writes the time played so far. */
    private fun stopClock() {
        if (runningSince == 0L && !pendingSession) return
        val secs = if (runningSince == 0L) 0L else (SystemClock.elapsedRealtime() - runningSince) / 1000
        runningSince = 0L
        if (secs <= 0 && !pendingSession) return
        val newSession = pendingSession
        pendingSession = false
        runCatching { PlayTime.add(paths.playtimeFile(game), secs, newSession) }
    }

    private fun applyCheats() {
        if (!cheatsSupported) return
        NativeBridge.setCheats(cheats.filter { it.enabled }.map { it.code }.toTypedArray())
    }

    private fun updateCheats(list: List<Cheat>) {
        if (list.any { it.enabled }) demoteRaHardcore("cheats enabled")
        cheats = list
        Cheats.save(paths.cheatsFile(game), list)
        applyCheats()
    }

    private fun autoFindCheats(silentIfNone: Boolean = false) {
        if (cheatSearching) return
        cheatAutoTried = true
        cheatSearching = true
        cheatSearchError = null
        lifecycleScope.launch {
            val result = CheatFinder.autoFind(game, paths.cheats)
            cheatSearching = false
            cheatAlternatePacks = result.alternatePacks
            if (result.cheats.isNotEmpty()) {
                val merged = CheatFinder.merge(cheats, result.cheats)
                val added = (merged.size - cheats.size).coerceAtLeast(0)
                cheats = merged
                Cheats.save(paths.cheatsFile(game), merged)
                activeCheatPack = result.activePack
                cheatSearchSource = result.sourceLabel
                cheatSearchError = null
                val msg = if (added > 0) "Found $added cheat${if (added == 1) "" else "s"} for ${game.title}"
                else "All ${result.cheats.size} cheats already loaded"
                toast(msg)
            } else {
                cheatSearchError = result.error
                if (!silentIfNone && result.error != null) {
                    toast("No cheat pack found for ${game.title}")
                }
            }
        }
    }

    private fun selectCheatPack(pack: CheatPackMatch) {
        if (cheatSearching || activeCheatPack?.fileName == pack.fileName) return
        cheatSearching = true
        cheatSearchError = null
        lifecycleScope.launch {
            val loaded = CheatFinder.loadPack(pack)
            cheatSearching = false
            if (loaded.isNotEmpty()) {
                val keepEnabled = cheats.filter { it.enabled }
                val merged = CheatFinder.merge(keepEnabled, loaded)
                cheats = merged
                Cheats.save(paths.cheatsFile(game), merged)
                activeCheatPack = pack
                cheatSearchSource = "${pack.fileName.removeSuffix(".cht")} · libretro-database"
                toast("Loaded ${loaded.size} cheats (${pack.displayLabel})")
            } else {
                toast("Couldn't load ${pack.displayLabel}")
            }
        }
    }

    private fun setScreen(newFilter: Int = filter, integer: Boolean = integerScale) {
        filter = newFilter
        integerScale = integer
        gamePrefs.setFilter(system.id, newFilter)
        gamePrefs.setIntegerScale(system.id, integer)
        NativeBridge.setFilter(newFilter)
    }

    private fun updateGraphics(transform: (com.retrohero.app.data.GraphicsConfig) -> com.retrohero.app.data.GraphicsConfig) {
        val next = transform(graphicsConfig)
        graphicsConfig = next
        gamePrefs.setGraphicsConfig(system.id, next)
        NativeBridge.setUpscaleFactor(next.upscale)
        com.retrohero.app.data.GraphicsCoreOptions.optionsFor(activeCoreId, next).forEach { (k, v) ->
            NativeBridge.setOption(k, v)
        }
    }

    private fun setAmbilight(mode: AmbilightMode) {
        ambilightMode = mode
        gamePrefs.ambilightMode = mode
        NativeBridge.setAmbilightEnabled(mode.isEnabled)
        if (!mode.isEnabled) {
            ambilightTracker.reset()
            ambilightState = AmbilightState.Off
        } else if (started) {
            NativeBridge.pollAmbilight(ambilightTracker.rawBuffer)
            ambilightState = ambilightTracker.step(ambilightTracker.rawBuffer, mode)
        }
    }

    private fun setRewind(on: Boolean) {
        if (on) demoteRaHardcore("rewind enabled")
        rewindOn = on
        gamePrefs.rewind = on
        NativeBridge.setRewindEnabled(on)
    }

    /** Saves the current frame as a PNG (scaled up with hard pixel edges). */
    private fun takeScreenshot() {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                val raw = NativeBridge.grabFrame() ?: return@withContext null
                val w = raw[0]
                val h = raw[1]
                if (w <= 0 || h <= 0) return@withContext null
                val bmp = Bitmap.createBitmap(raw, 2, w, w, h, Bitmap.Config.ARGB_8888)
                val k = (720 / maxOf(w, h)).coerceIn(1, 4)
                val out = if (k > 1) Bitmap.createScaledBitmap(bmp, w * k, h * k, false) else bmp
                val dir = paths.screenshotsDir(game).apply { mkdirs() }
                val f = File(dir, "${System.currentTimeMillis()}.png")
                f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                if (settings.screenshotsToGallery) runCatching { copyToGallery(f) }
                f
            }
            if (file != null) {
                flash++
                toast("Screenshot saved")
            } else {
                toast("Nothing to capture yet")
            }
        }
    }

    private fun copyToGallery(f: File) {
        if (Build.VERSION.SDK_INT < 29) return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${game.baseName}-${f.nameWithoutExtension}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RetroHero")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
    }

    /** Packages the current state + screenshot (+ optional recorded input ghost & win condition) into a `.rhclip`. */
    private fun shareMoment(
        title: String,
        note: String,
        preCapturedState: File? = null,
        preCapturedPng: File? = null,
        ghostFrames: List<com.retrohero.app.data.GhostFrame> = emptyList(),
        targetDurationMs: Long = 0L,
        winCondition: com.retrohero.app.data.ChallengeWinCondition? = null,
    ) {
        lifecycleScope.launch {
            val clipFile = withContext(Dispatchers.IO) {
                runCatching {
                    val tmpState = preCapturedState ?: File(paths.clipTmp, "moment-${System.nanoTime()}.state").also { f ->
                        if (!NativeBridge.saveState(f.absolutePath)) error("Couldn't save state")
                    }
                    val tmpPng = preCapturedPng ?: NativeBridge.grabFrame()?.let { raw ->
                        val w = raw[0]
                        val h = raw[1]
                        if (w > 0 && h > 0) {
                            val bmp = Bitmap.createBitmap(raw, 2, w, w, h, Bitmap.Config.ARGB_8888)
                            val k = (720 / maxOf(w, h)).coerceIn(1, 4)
                            val out = if (k > 1) Bitmap.createScaledBitmap(bmp, w * k, h * k, false) else bmp
                            File(paths.clipTmp, "moment-${System.nanoTime()}.png").also { f ->
                                f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            }
                        } else null
                    }
                    val tmpGhost = if (ghostFrames.isNotEmpty()) {
                        File(paths.clipTmp, "ghost-${System.nanoTime()}.bin").also { f ->
                            f.writeBytes(com.retrohero.app.data.ChallengeGhost.encode(ghostFrames))
                        }
                    } else null
                    val manifest = com.retrohero.app.data.MomentManifest(
                        title = title.trim().ifEmpty { game.title },
                        note = note.trim(),
                        systemId = system.id,
                        coreId = currentCoreId,
                        gameTitle = game.title,
                        gameBaseName = game.baseName,
                        ghostFrames = ghostFrames.size,
                        targetDurationMs = targetDurationMs,
                        winCondition = winCondition,
                    )
                    val safeName = Regex("[^a-zA-Z0-9._-]+").replace(manifest.title, "_").trim('_').ifEmpty { "moment" }.take(40)
                    val dest = File(paths.clips, "$safeName-${System.currentTimeMillis()}.rhclip")
                    dest.outputStream().use {
                        com.retrohero.app.data.MomentCapsule.export(it, manifest, tmpState, tmpPng, tmpGhost)
                    }
                    tmpState.delete()
                    tmpPng?.delete()
                    tmpGhost?.delete()
                    dest
                }.getOrNull()
            }
            if (clipFile == null) {
                toast("Couldn't create .rhclip capsule")
                return@launch
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(this@EmulatorActivity, "$packageName.files", clipFile)
            val send = Intent(Intent.ACTION_SEND)
                .setType("application/octet-stream")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(
                    Intent.EXTRA_SUBJECT,
                    if (ghostFrames.isNotEmpty()) "RetroHero Challenge: ${title.ifBlank { game.title }}"
                    else "RetroHero Moment: ${title.ifBlank { game.title }}",
                )
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, "Share .rhclip challenge"))
        }
    }

    private fun startChallengeRecording(
        title: String,
        note: String,
        winCondition: com.retrohero.app.data.ChallengeWinCondition?,
    ) {
        lifecycleScope.launch {
            val captured = withContext(Dispatchers.IO) {
                runCatching {
                    val stateF = File(paths.clipTmp, "chal-start-${System.nanoTime()}.state")
                    if (!NativeBridge.saveState(stateF.absolutePath)) error("Save state failed")
                    val pngF = NativeBridge.grabFrame()?.let { raw ->
                        val w = raw[0]
                        val h = raw[1]
                        if (w > 0 && h > 0) {
                            val bmp = Bitmap.createBitmap(raw, 2, w, w, h, Bitmap.Config.ARGB_8888)
                            File(paths.clipTmp, "chal-start-${System.nanoTime()}.png").also { f ->
                                f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            }
                        } else null
                    }
                    stateF to pngF
                }.getOrNull()
            }
            if (captured == null) {
                toast("Couldn't snapshot starting frame for challenge")
                return@launch
            }
            challengeRecStartState?.delete()
            challengeRecStartPng?.delete()
            challengeRecStartState = captured.first
            challengeRecStartPng = captured.second
            challengeRecFrames.clear()
            challengeRecTitle = title.trim().ifEmpty { "${game.title} Challenge" }
            challengeRecNote = note.trim()
            challengeRecWin = winCondition
            challengeRecStartMs = SystemClock.elapsedRealtime()
            challengeRecElapsedMs = 0L
            challengeRecording = true
            setMenu(false)
            toast("Recording Challenge Ghost — play your run, then tap Finish!")
        }
    }

    private fun finishChallengeRecordingAndShare() {
        if (!challengeRecording) return
        challengeRecording = false
        val duration = (SystemClock.elapsedRealtime() - challengeRecStartMs).coerceAtLeast(100L)
        val stateF = challengeRecStartState
        val pngF = challengeRecStartPng
        val framesCopy = challengeRecFrames.toList()
        challengeRecStartState = null
        challengeRecStartPng = null
        challengeRecFrames.clear()
        val effectiveWin = challengeRecWin ?: com.retrohero.app.data.ChallengeWinCondition(
            type = com.retrohero.app.data.ChallengeWinType.TIME_TRIAL,
            label = "Beat ${formatChallengeClock(duration)}",
        )
        setMenu(true)
        shareMoment(
            title = challengeRecTitle,
            note = challengeRecNote,
            preCapturedState = stateF,
            preCapturedPng = pngF,
            ghostFrames = framesCopy,
            targetDurationMs = duration,
            winCondition = effectiveWin,
        )
    }

    private fun cancelChallengeRecording() {
        challengeRecording = false
        challengeRecStartState?.delete()
        challengeRecStartPng?.delete()
        challengeRecStartState = null
        challengeRecStartPng = null
        challengeRecFrames.clear()
        toast("Challenge recording canceled")
    }

    private fun loadChallengeCapsuleFromUri(uri: android.net.Uri) {
        lifecycleScope.launch {
            val extracted = withContext(Dispatchers.IO) {
                runCatching {
                    val destDir = File(paths.clipTmp, "play-${System.nanoTime()}")
                    contentResolver.openInputStream(uri)?.use { input ->
                        com.retrohero.app.data.MomentCapsule.extract(input, destDir)
                    } ?: error("Could not read .rhclip file")
                }
            }
            extracted.onFailure { err ->
                toast(err.message ?: "Couldn't load .rhclip challenge")
                return@launch
            }
            val moment = extracted.getOrNull() ?: return@launch
            val loaded = withContext(Dispatchers.IO) {
                NativeBridge.loadState(moment.stateFile.absolutePath)
            }
            if (!loaded) {
                toast("This .rhclip belongs to a different core or game revision")
                return@launch
            }
            val ghost = withContext(Dispatchers.IO) {
                moment.ghostFile?.takeIf { it.isFile }?.let {
                    com.retrohero.app.data.ChallengeGhost.decode(it.readBytes())
                }.orEmpty()
            }
            activeChallengeStateFile = moment.stateFile
            activeChallengeTitle = moment.manifest.title
            activeChallengeGhost = ghost
            activeChallengeTargetMs = moment.manifest.targetDurationMs
            activeChallengeWin = moment.manifest.winCondition
            challengeCleared = false
            challengeClearedTimeMs = null
            challengeElapsedMs = 0L
            challengeGhostMask = 0
            challengeStartRealtime = SystemClock.elapsedRealtime()
            setMenu(false)
            toast("Challenge loaded: ${moment.manifest.title}")
        }
    }

    private fun retryActiveChallenge() {
        val stateF = activeChallengeStateFile ?: return
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { NativeBridge.loadState(stateF.absolutePath) }
            if (ok) {
                challengeCleared = false
                challengeClearedTimeMs = null
                challengeElapsedMs = 0L
                challengeGhostMask = 0
                challengeStartRealtime = SystemClock.elapsedRealtime()
                toast("Retry! Go!")
            }
        }
    }

    private fun openPocketGuide() {
        menuOpen = false
        browserOpen = false
        if (started) NativeBridge.setPaused(true)
        stopClock()
        pocketGuideOpen = true
    }

    private fun closePocketGuide() {
        pocketGuideOpen = false
        if (started && !shutDown && !menuOpen && !editingPad && !browserOpen && translateState is TranslateState.Idle) {
            NativeBridge.setPaused(false)
            startClock()
        }
    }

    private fun openInGameBrowser() {
        menuOpen = false
        pocketGuideOpen = false
        if (started) NativeBridge.setPaused(true)
        stopClock()
        browserOpen = true
    }

    private fun closeInGameBrowser() {
        browserOpen = false
        if (started && !shutDown && !menuOpen && !editingPad && !pocketGuideOpen && translateState is TranslateState.Idle) {
            NativeBridge.setPaused(false)
            startClock()
        }
    }

    private fun saveBrowserBookmark(url: String?) {
        browserBookmarkedUrl = url
        lifecycleScope.launch(Dispatchers.IO) {
            val f = paths.browserBookmarkFile(game)
            runCatching {
                if (url.isNullOrBlank()) f.delete() else f.writeText(url)
            }
        }
        toast(if (url.isNullOrBlank()) "Removed pinned guide" else "Pinned walkthrough for ${game.title}")
    }

    private fun savePocketNotes(text: String) {
        pocketNotes = text
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { paths.notesFile(game).writeText(text) }
        }
    }

    private fun toggleLiveTranslate(enable: Boolean) {
        liveTranslateOn = enable
        liveTranslateJob?.cancel()
        liveTranslateJob = null
        if (!enable) {
            liveTranslateBlocks = emptyList()
            liveTranslateScanning = false
            return
        }
        // Resume gameplay cleanly when activating Live In-Place Overlay!
        translateState = TranslateState.Idle
        menuOpen = false
        pocketGuideOpen = false
        browserOpen = false
        if (started && !shutDown) {
            NativeBridge.setPaused(false)
            startClock()
        }
        toast("Live translate ON (${translateLang.label} → EN) — replacing dialogue in place")
        liveTranslateJob = lifecycleScope.launch(Dispatchers.Default) {
            var lastSig = 0L
            while (liveTranslateOn && !shutDown) {
                if (!menuOpen && !pocketGuideOpen && !browserOpen && translateState is TranslateState.Idle) {
                    val raw = NativeBridge.grabFrame()
                    val sig = frameDialogueSignature(raw)
                    if (sig != 0L && sig != lastSig) {
                        lastSig = sig
                        withContext(Dispatchers.Main) { liveTranslateScanning = true }
                        val res = ScreenTranslator.translateRawFrame(raw, translateLang) {}
                        withContext(Dispatchers.Main) {
                            liveTranslateScanning = false
                            if (res is TranslateState.Ready) {
                                liveTranslateBlocks = res.blocks
                            }
                        }
                    }
                }
                delay(700L)
            }
        }
    }

    private fun startPadEdit() {
        padAdjust = padLayouts.get(system.id, padLandscape)
        padSelected = null
        showPad = true
        editingPad = true
        menuOpen = false  // stay paused while editing
    }

    private fun finishPadEdit(save: Boolean) {
        if (save) padLayouts.set(system.id, padLandscape, padAdjust)
        editingPad = false
        padSelected = null
        setMenu(false)
    }

    private fun runScreenTranslate(lang: TranslateSourceLang = translateLang) {
        translateLang = lang
        settings.translateSourceLang = lang.code
        menuOpen = false
        if (started) NativeBridge.setPaused(true)
        stopClock()
        translateState = TranslateState.Working("Capturing frame…")
        lifecycleScope.launch {
            val raw = withContext(Dispatchers.IO) { NativeBridge.grabFrame() }
            translateState = ScreenTranslator.translateRawFrame(raw, lang) { status ->
                lifecycleScope.launch(Dispatchers.Main) {
                    if (translateState is TranslateState.Working) translateState = TranslateState.Working(status)
                }
            }
        }
    }

    private fun dismissTranslate() {
        translateState = TranslateState.Idle
        if (started && !shutDown && !menuOpen && !editingPad && !pocketGuideOpen && !browserOpen) {
            NativeBridge.setPaused(false)
            startClock()
        }
    }

    private fun applyFastForward() {
        NativeBridge.setFastForward(fastForwardToggle || fastForwardHeld, settings.fastForwardSpeed)
    }

    override fun onPause() {
        super.onPause()
        stopVsyncFeed()
        gyroController?.stop()
        if (started && !shutDown) {
            audioHaptics.cancel()
            NativeBridge.setPaused(true)
            NativeBridge.setRewinding(false)
            flushCanonicalSram()
            stopClock()
        }
    }

    override fun onResume() {
        super.onResume()
        if (started && !shutDown && !menuOpen && !editingPad && !pocketGuideOpen && !browserOpen && translateState is TranslateState.Idle) {
            applyDisplaySyncLock()
            startVsyncFeed()
            syncGyroSteering()
            NativeBridge.setPaused(false)
            startClock()
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.hold, R.anim.game_exit)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.hold, R.anim.game_exit)
        }
    }

    private fun quit() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { shutdownCore() }
            finish()
        }
    }

    private fun shutdownCore() {
        if (!started || shutDown) return
        liveTranslateOn = false
        liveTranslateJob?.cancel()
        liveTranslateJob = null
        netplaySession.stop()
        stopVsyncFeed()
        gyroController?.stop()
        raJob?.cancel()
        raJob = null
        stopClock()
        shutDown = true
        NativeBridge.setPaused(true)
        if (settings.autoSave) {
            if (NativeBridge.saveState(paths.stateFile(game, AppPaths.AUTO_SLOT).absolutePath)) {
                gamePrefs.setStateCore(game.id, AppPaths.AUTO_SLOT, activeCoreId)
            }
        }
        val prep = preparedSave
        val rawPath = prep?.activeFile?.absolutePath ?: paths.saveRam(game).absolutePath
        NativeBridge.shutdown(rawPath)
        if (prep != null) {
            com.retrohero.app.data.SavePortability.commitFromCore(
                activeFile = prep.activeFile,
                canonicalFile = prep.canonicalFile,
                systemId = system.id,
                coreId = activeCoreId,
                cleanupTemp = true,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            shutdownCore()
            // Give the core a clean slate next time; see class comment.
            Process.killProcess(Process.myPid())
        }
    }

    // ---- Physical controllers -------------------------------------------------------------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // The controller's menu button (and anything remapped to it) still works with the menu open.
        if (menuOpen && event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE && event.action == KeyEvent.ACTION_UP) {
            setMenu(false)
            return true
        }
        if (!menuOpen && !editingPad && phase is Phase.Running && event.keyCode != KeyEvent.KEYCODE_BACK) {
            if (input.isGamepad(event)) showPad = false
            if (input.onKey(event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!menuOpen && phase is Phase.Running && input.onMotion(event)) {
            showPad = false
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun handleDeltaAction(action: DeltaAction, pressed: Boolean) {
        when (action) {
            DeltaAction.MENU -> if (pressed) setMenu(true)
            DeltaAction.FAST_FORWARD_HOLD -> {
                fastForwardHeld = pressed
                applyFastForward()
            }
            DeltaAction.FAST_FORWARD_TOGGLE -> if (pressed) {
                fastForwardToggle = !fastForwardToggle
                applyFastForward()
            }
            DeltaAction.REWIND_HOLD -> if (rewindOn) NativeBridge.setRewinding(pressed)
            DeltaAction.QUICK_SAVE -> if (pressed) {
                val file = paths.stateFile(game, AppPaths.QUICK_SLOT)
                val ok = NativeBridge.saveState(file.absolutePath)
                if (ok) {
                    gamePrefs.setStateCore(game.id, AppPaths.QUICK_SLOT, activeCoreId)
                    flushCanonicalSram()
                }
                toast(if (ok) "Quick saved · Battery save synced" else NativeBridge.getLastError().ifEmpty { "Quick save failed" })
            }
            DeltaAction.QUICK_LOAD -> if (pressed) {
                val file = paths.stateFile(game, AppPaths.QUICK_SLOT)
                val savedBy = gamePrefs.stateCore(game.id, AppPaths.QUICK_SLOT)
                if (!file.exists()) {
                    toast("No quick save yet")
                } else if (savedBy != null && savedBy != activeCoreId) {
                    val name = system.cores.firstOrNull { it.id == savedBy }?.displayName ?: savedBy
                    toast("Quick save was created on $name — use in-game battery save or switch back")
                } else {
                    val ok = NativeBridge.loadState(file.absolutePath)
                    if (ok) demoteRaHardcore("save state loaded")
                    toast(if (ok) "Quick loaded" else NativeBridge.getLastError().ifEmpty { "Quick load failed" })
                }
            }
            DeltaAction.SCREENSHOT -> if (pressed) takeScreenshot()
        }
    }

    // ---- UI ---------------------------------------------------------------------------------

    @Composable
    private fun EmulatorScreen() {
        val config = LocalConfiguration.current
        val landscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val tablet = config.smallestScreenWidthDp >= 600
        var sustainedCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(phase) {
            while (phase is Phase.Running) {
                aspect = NativeBridge.getAspectRatio().takeIf { it > 0.1f } ?: aspect
                delay(500)
            }
        }

        LaunchedEffect(phase, menuOpen, editingPad, audioHapticsOn) {
            if (phase !is Phase.Running || menuOpen || editingPad || !audioHapticsOn) {
                audioHaptics.cancel()
                return@LaunchedEffect
            }
            while (true) {
                if (!fastForwardToggle && !fastForwardHeld) {
                    val energy = NativeBridge.consumeAudioHaptic()
                    if (energy > 0f) audioHaptics.trigger(energy, settings.audioHapticsStrength)
                } else {
                    NativeBridge.consumeAudioHaptic()
                }
                delay(32)
            }
        }

        LaunchedEffect(phase, menuOpen, ambilightMode) {
            if (!ambilightMode.isEnabled) {
                ambilightState = AmbilightState.Off
                return@LaunchedEffect
            }
            if (phase !is Phase.Running || menuOpen) return@LaunchedEffect
            while (true) {
                NativeBridge.pollAmbilight(ambilightTracker.rawBuffer)
                ambilightState = ambilightTracker.step(ambilightTracker.rawBuffer, ambilightMode)
                delay(33L)
            }
        }

        padLandscape = landscape
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val density = LocalDensity.current
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }
            val longEdge = maxOf(containerW, containerH)
            val shortEdge = minOf(containerW, containerH).coerceAtLeast(1f)
            val tallPhone = !tablet && (longEdge / shortEdge) >= 1.85f
            val skin = activeDeltaSkin
            val deltaRep = if (showPad && !editingPad && skin != null) {
                skin.representations.pick(landscape = landscape, tablet = tablet, tallPhone = tallPhone)
            } else null

            val shellStyle = remember(padSkin, system.id, system.color, Tone.isDark) {
                when (padSkin.faces) {
                    "console" -> ConsoleStyles.forSystem(system.id, system.color)
                    "instrument" -> ConsoleStyles.instrument(
                        dark = Tone.isDark,
                        accent = com.retrohero.app.ui.components.Ascend.accent,
                        onAccent = com.retrohero.app.ui.components.Ascend.onAccent,
                        systemName = system.name,
                        systemId = system.id,
                    )
                    else -> null
                }
            }

            if (skin != null && deltaRep != null) {
                val deltaLayout = remember(containerW, containerH, deltaRep, landscape) {
                    computeDeltaLayout(containerW, containerH, deltaRep, landscape)
                }
                val sr = deltaLayout.screenRect
                Box(
                    Modifier
                        .offset { IntOffset(sr.left.roundToInt(), sr.top.roundToInt()) }
                        .size(with(density) { sr.width.toDp() }, with(density) { sr.height.toDp() }),
                    contentAlignment = Alignment.Center,
                ) {
                    if (deltaLayout.explicitScreen) GameSurface(Modifier.fillMaxSize()) else GameView()
                }
                if (phase is Phase.Running) {
                    val view = androidx.compose.ui.platform.LocalView.current
                    DeltaSkinOverlay(
                        skin = skin,
                        representation = deltaRep,
                        layout = deltaLayout,
                        systemId = system.id,
                        opacity = settings.overlayOpacity,
                        onButton = { id, pressed -> NativeBridge.setButton(0, id, pressed) },
                        onAnalog = { x, y ->
                            NativeBridge.setAnalog(0, 0, 0, x)
                            NativeBridge.setAnalog(0, 0, 1, y)
                        },
                        onPointer = { x, y, down -> NativeBridge.setPointer(x, y, down) },
                        onAction = ::handleDeltaAction,
                        onPress = { if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
                    )
                }
            } else if (landscape && shellStyle != null && showPad) {
                LandscapeConsoleShell(
                    style = shellStyle,
                    fastForwardActive = fastForwardToggle || fastForwardHeld,
                    sustainedCount = sustainedCount,
                    ambilight = ambilightState,
                    gameContent = { GameView() },
                    padContent = {
                        if (phase is Phase.Running) {
                            Pad(
                                landscape = true,
                                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                                onSustainedChanged = { sustainedCount = it.size },
                            )
                        }
                    },
                )
            } else if (landscape) {
                AmbilightBackdropHalo(ambilight = ambilightState, landscape = true)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GameView() }
                if (showPad && phase is Phase.Running) {
                    Pad(
                        landscape = true,
                        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        onSustainedChanged = { sustainedCount = it.size },
                    )
                }
            } else if (shellStyle != null && showPad) {
                // Full-device Delta-inspired physical handheld shell wrapping display bezel + control deck
                HandheldShell(
                    style = shellStyle,
                    fastForwardActive = fastForwardToggle || fastForwardHeld,
                    sustainedCount = sustainedCount,
                    ambilight = ambilightState,
                    gameContent = { GameView() },
                    padContent = {
                        if (phase is Phase.Running) {
                            Pad(
                                landscape = false,
                                modifier = Modifier.fillMaxSize(),
                                inShell = true,
                                onSustainedChanged = { sustainedCount = it.size },
                            )
                        }
                    },
                )
            } else {
                AmbilightBackdropHalo(ambilight = ambilightState, landscape = false)
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { GameView() }
                    if (showPad && phase is Phase.Running) {
                        Pad(
                            landscape = false,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f),
                            onSustainedChanged = { sustainedCount = it.size },
                        )
                    }
                }
            }

            when (val p = phase) {
                Phase.Loading -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ContainedLoadingIndicator(Modifier.size(88.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Starting ${game.title}…", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                is Phase.Failed -> AlertDialog(
                    onDismissRequest = { finish() },
                    confirmButton = { TextButton(onClick = { finish() }) { Text("Close") } },
                    title = { Text("Couldn't start game") },
                    text = { Text(p.message) },
                )
                Phase.Running -> Unit
            }

            if (showPerfHud && phase is Phase.Running && !menuOpen) {
                val hudText = buildString {
                    append(String.format(java.util.Locale.US, "%.1f FPS", perfFps))
                    if (perfMs > 0.05f) append(String.format(java.util.Locale.US, " · %.1f ms", perfMs))
                    if (perfVsyncLocked) append(" · ${displayHz.roundToInt()}Hz")
                    if (perfBfiActive) append(" · BFI")
                    if (perfActiveRa > 0) append(" · RA-${perfActiveRa}f")
                    if (slowMotion) append(" · 0.5×")
                }
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .safeDrawingPadding()
                        .padding(top = 10.dp, start = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = hudText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB9F6CA),
                        maxLines = 1,
                    )
                }
            }
            if (discCount > 1 && phase is Phase.Running && !menuOpen && !editingPad) {
                val activeLabel = discLabels.getOrElse(currentDisc) { "Disc ${currentDisc + 1}" }
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(top = 10.dp, end = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .clickable(enabled = !swappingDisc) { discPickerOpen = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.Album,
                        contentDescription = "Swap disc",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "$activeLabel/$discCount · Swap",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
            if (discPickerOpen && discCount > 1 && !menuOpen) QuickDiscSwapSheet()
            val npState by netplaySession.state.collectAsState()
            if (!menuOpen && !pocketGuideOpen && translateState is TranslateState.Idle && phase is Phase.Running) {
                Column(
                    Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (npState.phase != NetplayPhase.IDLE && npState.phase != NetplayPhase.ERROR) {
                        NetplayHudPill(state = npState, onClick = { setMenu(true) })
                    }
                    if (challengeRecording) {
                        ChallengeRecordingPill(
                            elapsedMs = challengeRecElapsedMs,
                            onFinishAndShare = ::finishChallengeRecordingAndShare,
                            onCancel = ::cancelChallengeRecording,
                        )
                    } else if (activeChallengeTitle != null) {
                        ChallengePlayHud(
                            title = activeChallengeTitle.orEmpty(),
                            elapsedMs = challengeElapsedMs,
                            targetMs = activeChallengeTargetMs,
                            ghostButtonsMask = challengeGhostMask,
                            hasGhost = activeChallengeGhost.isNotEmpty(),
                            winGoalText = activeChallengeWin?.summary().orEmpty(),
                            cleared = challengeCleared,
                            clearedTimeMs = challengeClearedTimeMs,
                            onInstantRetry = ::retryActiveChallenge,
                        )
                    }
                    if (liveTranslateOn) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            contentColor = Color.White,
                            onClick = { toggleLiveTranslate(false) },
                        ) {
                            Text(
                                "LIVE ${translateLang.label}→EN · Tap to stop",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
            if (menuOpen) PauseMenu()
            if (pocketGuideOpen) {
                val manual = remember(game.id) { PocketGuides.forGame(game) }
                PocketGuideDrawer(
                    game = game,
                    manual = manual,
                    initialNotes = pocketNotes,
                    raData = raSessionGame,
                    raHardcore = raSessionHardcore,
                    onSaveNotes = ::savePocketNotes,
                    onOpenWebBrowser = ::openInGameBrowser,
                    onDismiss = ::closePocketGuide,
                )
            }
            if (browserOpen) {
                val startUrl = browserLastUrl
                    ?: browserBookmarkedUrl
                    ?: InGameBrowserUrls.defaultUrlFor(game)
                InGameBrowserOverlay(
                    game = game,
                    initialUrl = startUrl,
                    bookmarkedUrl = browserBookmarkedUrl,
                    onUrlChanged = { browserLastUrl = it },
                    onBookmarkUrl = ::saveBrowserBookmark,
                    onWebViewReady = { browserWebView = it },
                    onDismiss = ::closeInGameBrowser,
                )
            }
            if (translateState !is TranslateState.Idle) {
                TranslateOverlay(
                    state = translateState,
                    sourceLang = translateLang,
                    liveActive = liveTranslateOn,
                    onToggleLive = ::toggleLiveTranslate,
                    onSelectLang = { runScreenTranslate(it) },
                    onRescan = { runScreenTranslate(translateLang) },
                    onDismiss = ::dismissTranslate,
                )
            }
            challengeBanner?.let { banner ->
                LaunchedEffect(banner) {
                    delay(4500)
                    challengeBanner = null
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.82f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                ) {
                    Text(
                        "Challenge: $banner",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            if (editingPad) PadEditBar(Modifier.align(Alignment.TopCenter))
            raBanner?.let { banner ->
                RaUnlockBanner(
                    banner = banner,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                )
            }
            ScreenshotFlash(flash)
        }
        // 60 Hz Playable Challenge Ghost recorder & live playback / RAM win-condition loop
        LaunchedEffect(phase, challengeRecording, activeChallengeTitle, menuOpen, pocketGuideOpen, challengeCleared) {
            if (phase !is Phase.Running || menuOpen || pocketGuideOpen) return@LaunchedEffect
            var tick = 0
            while (challengeRecording || (activeChallengeTitle != null && !challengeCleared)) {
                if (challengeRecording) {
                    val elapsed = SystemClock.elapsedRealtime() - challengeRecStartMs
                    challengeRecElapsedMs = elapsed
                    val mask = NativeBridge.getButtonsMask(0)
                    if (challengeRecFrames.size < com.retrohero.app.data.ChallengeGhost.MAX_RECORD_FRAMES) {
                        challengeRecFrames += com.retrohero.app.data.GhostFrame(mask)
                    } else {
                        finishChallengeRecordingAndShare()
                        break
                    }
                }
                if (activeChallengeTitle != null && !challengeCleared) {
                    val elapsed = SystemClock.elapsedRealtime() - challengeStartRealtime
                    challengeElapsedMs = elapsed
                    val ghost = activeChallengeGhost
                    if (ghost.isNotEmpty()) {
                        val idx = ((elapsed * 60L) / 1000L).toInt()
                        challengeGhostMask = if (idx in ghost.indices) ghost[idx].buttons else 0
                    }
                    val win = activeChallengeWin
                    if (win != null) {
                        val won = when (win.type) {
                            com.retrohero.app.data.ChallengeWinType.SURVIVE ->
                                win.targetSeconds > 0 && elapsed >= win.targetSeconds * 1000L
                            com.retrohero.app.data.ChallengeWinType.RAM_GOAL ->
                                (tick % 4 == 0) && win.evaluateRam(
                                    withContext(Dispatchers.IO) { NativeBridge.readMemory(2, 512 * 1024) },
                                )
                            else -> false
                        }
                        if (won) {
                            challengeCleared = true
                            challengeClearedTimeMs = elapsed
                            toast("Challenge Cleared in ${formatChallengeClock(elapsed)}!")
                        }
                    }
                }
                tick++
                delay(16L)
            }
        }
        LaunchedEffect(phase, showPerfHud) {
            while (phase is Phase.Running) {
                val sz = NativeBridge.getFrameSize()
                if (sz[0] > 0 && sz[1] > 0) frameSize = IntSize(sz[0], sz[1])
                if (showPerfHud) {
                    val stats = NativeBridge.getPerfStats()
                    if (stats.size >= 3) {
                        perfFps = stats[0]
                        perfMs = stats[1]
                        perfActiveRa = stats[2].toInt()
                    }
                    perfVsyncLocked = NativeBridge.isVsyncLocked()
                    perfBfiActive = NativeBridge.isBfiActive()
                    delay(450)
                } else {
                    delay(1000)
                }
            }
        }
    }

    @Composable
    private fun RaUnlockBanner(banner: RaBanner, modifier: Modifier = Modifier) {
        val cs = MaterialTheme.colorScheme
        Row(
            modifier = modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (banner.hardcore) cs.tertiaryContainer else cs.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (banner.hardcore) cs.tertiary else cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (banner.badgeUrl != null) {
                    coil.compose.AsyncImage(
                        model = banner.badgeUrl,
                        contentDescription = banner.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (banner.hardcore) cs.onTertiary else cs.onPrimary,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (banner.hardcore) cs.onTertiaryContainer else cs.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = banner.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (banner.hardcore) cs.onTertiaryContainer else cs.onPrimaryContainer).copy(alpha = 0.84f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (banner.points != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (banner.hardcore) cs.tertiary else cs.primary)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "+${banner.points}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (banner.hardcore) cs.onTertiary else cs.onPrimary,
                    )
                }
            }
        }
    }

    @Composable
    private fun Pad(
        landscape: Boolean,
        modifier: Modifier,
        inShell: Boolean = false,
        onSustainedChanged: (Set<Int>) -> Unit = {},
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val oneHanded = remember(smartTouch.portraitMode, landscape, game.title) {
            SmartTouch.shouldUseOneHandedPortrait(smartTouch.portraitMode, landscape, game.title)
        }
        VirtualGamepad(
            layout = system.layout,
            landscape = landscape,
            opacity = settings.overlayOpacity,
            onButton = { id, pressed -> NativeBridge.setButton(0, id, pressed) },
            onMenu = { setMenu(true) },
            onFastForward = { held ->
                fastForwardHeld = held
                applyFastForward()
            },
            onPress = { if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
            modifier = modifier,
            accent = system.color,
            skin = padSkin,
            systemId = system.id,
            onRewind = if (rewindOn) { held -> NativeBridge.setRewinding(held) } else null,
            adjust = remember(editingPad, landscape) { padLayouts.get(system.id, landscape) },
            edit = if (editingPad) {
                PadEditState(
                    adjust = padAdjust,
                    selected = padSelected,
                    onSelect = { padSelected = it },
                    onAdjust = { name, a -> padAdjust = padAdjust + (name to a) },
                )
            } else null,
            inShell = inShell,
            smartTouch = smartTouch,
            oneHandedPortrait = oneHanded,
            onSustainedChanged = onSustainedChanged,
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    private fun GameView() {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val ratioOverride = graphicsConfig.aspectRatio.ratio
            val isFill = ratioOverride != null && ratioOverride < 0f
            val effectiveAspect = if (ratioOverride != null && ratioOverride > 0f) ratioOverride else aspect
            val size = if (!isFill && integerScale && frameSize.height > 0) {
                integerFit(
                    with(density) { maxWidth.toPx() },
                    with(density) { maxHeight.toPx() },
                    frameSize.height,
                    effectiveAspect,
                )?.let { (w, h) -> with(density) { Modifier.size(w.toDp(), h.toDp()) } }
            } else null
            val surfaceMod = when {
                isFill -> Modifier.fillMaxSize()
                size != null -> size
                else -> Modifier.aspectRatio(effectiveAspect)
            }
            Box(surfaceMod.clip(RoundedCornerShape(0.dp))) {
                GameSurface(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (graphicsConfig.cropOverscan) {
                                scaleX = 1.06f
                                scaleY = 1.07f
                            }
                        },
                )
                if (liveTranslateOn && !menuOpen && !pocketGuideOpen && !browserOpen && translateState is TranslateState.Idle) {
                    LiveTranslateOverlay(
                        blocks = liveTranslateBlocks,
                        sourceLang = translateLang,
                        scanning = liveTranslateScanning,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    private fun GameSurface(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            activeSurface = holder.surface
                            NativeBridge.setSurface(holder.surface)
                            applyDisplaySyncLock()
                        }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            activeSurface = null
                            NativeBridge.setSurface(null)
                        }
                    })
                    if (system.touchScreen) setOnTouchListener(::onScreenTouch)
                }
            },
        )
    }

    private fun onScreenTouch(v: View, e: MotionEvent): Boolean {
        val x = e.x / v.width * 2f - 1f
        val y = e.y / v.height * 2f - 1f
        val down = e.actionMasked != MotionEvent.ACTION_UP && e.actionMasked != MotionEvent.ACTION_CANCEL
        NativeBridge.setPointer(x, y, down)
        return true
    }

    @Composable
    private fun PauseMenu() {
        var page by remember { mutableStateOf("main") }
        val cs = MaterialTheme.colorScheme
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.64f))
                .clickable(interactionSource = null, indication = null) { if (page == "main") setMenu(false) else page = "main" }
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = cs.surfaceContainer,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null) {},
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SystemBadge(system, 46.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                game.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                system.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            onClick = { page = "screen" },
                            shape = RoundedCornerShape(999.dp),
                            color = cs.secondaryContainer,
                            contentColor = cs.onSecondaryContainer,
                        ) {
                            Text(
                                text = visualMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(120))
                        },
                        label = "menuPage",
                    ) { p ->
                        when (p) {
                            "discs" -> DiscsPage { page = "main" }
                            "smart_touch" -> SmartTouchPage { page = "main" }
                            "save" -> SlotPage(saving = true) { page = "main" }
                            "load" -> SlotPage(saving = false) { page = "main" }
                            "options" -> OptionsPage { page = "main" }
                            "cheats" -> CheatsPage { page = "main" }
                            "screen" -> ScreenPage { page = "main" }
                            "skin" -> DeltaSkinPage { page = "main" }
                            "share" -> ShareMomentPage { page = "main" }
                            "netplay" -> NetplayPage { page = "main" }
                            else -> MainMenuPage { page = it }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun QuickDiscSwapSheet() {
        val nextIdx = (currentDisc + 1) % discCount
        val nextLabel = discLabels.getOrElse(nextIdx) { "Disc ${nextIdx + 1}" }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(interactionSource = null, indication = null) { discPickerOpen = false }
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 420.dp)
                    .clickable(interactionSource = null, indication = null) {},
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ShapeIcon(Icons.Default.Album)
                        Column(Modifier.weight(1f)) {
                            Text("Swap disc", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Unified battery save & memory card active across all $discCount discs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Button(
                        onClick = { performDiscSwap(nextIdx, closeMenuAfter = false) },
                        enabled = !swappingDisc,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
                    ) {
                        Icon(Icons.Default.Album, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (swappingDisc) "Swapping disc…" else "Quick swap → $nextLabel")
                    }
                    SegmentedGroup(null, discCount) {
                        for (i in 0 until discCount) {
                            val label = discLabels.getOrElse(i) { "Disc ${i + 1}" }
                            val sub = game.discs.getOrNull(i)?.fileName
                                ?: if (i == currentDisc) "Currently inserted" else "Tap to insert"
                            SegmentedListItem(
                                onClick = { performDiscSwap(i, closeMenuAfter = false) },
                                shapes = shapes(i),
                                colors = segmentColors(),
                                leadingContent = { RadioButton(selected = i == currentDisc, onClick = null) },
                                supportingContent = { Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) { Text(label) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { discPickerOpen = false }) { Text("Close") }
                    }
                }
            }
        }
    }

    @Composable
    private fun DiscsPage(back: () -> Unit) {
        val nextIdx = (currentDisc + 1) % discCount.coerceAtLeast(1)
        val nextLabel = discLabels.getOrElse(nextIdx) { "Disc ${nextIdx + 1}" }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("Swap disc", back)
            Text(
                "RetroHero automatically maintains one shared save (.srm) and virtual .m3u playlist across every disc in this set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (discCount > 1) {
                Button(
                    onClick = { performDiscSwap(nextIdx, closeMenuAfter = true) },
                    enabled = !swappingDisc,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Default.Album, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (swappingDisc) "Swapping disc…" else "Quick swap → $nextLabel")
                }
                SegmentedGroup(null, discCount) {
                    for (i in 0 until discCount) {
                        val label = discLabels.getOrElse(i) { "Disc ${i + 1}" }
                        val sub = game.discs.getOrNull(i)?.fileName
                            ?: if (i == currentDisc) "Currently inserted" else "Tap to insert"
                        SegmentedListItem(
                            onClick = { performDiscSwap(i, closeMenuAfter = true) },
                            shapes = shapes(i),
                            colors = segmentColors(),
                            leadingContent = { RadioButton(selected = i == currentDisc, onClick = null) },
                            supportingContent = { Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Text(label) }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainMenuPage(navigate: (String) -> Unit) {
        val cs = MaterialTheme.colorScheme
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val pName = activePatchName
            if (!pName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cs.tertiaryContainer,
                    contentColor = cs.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Soft-patched in memory · ${activePatchFormat ?: "ROM Patch"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onTertiaryContainer.copy(alpha = 0.85f),
                        )
                        Text(
                            text = pName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val hdrNote = activePatchHeaderNote
                        if (!hdrNote.isNullOrBlank()) {
                            Text(
                                text = hdrNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onTertiaryContainer.copy(alpha = 0.82f),
                            )
                        }
                    }
                }
            }

            // Primary Controls: Resume + Reset + Quit in one crisp row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { setMenu(false) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Resume", style = MaterialTheme.typography.titleSmall)
                }
                FilledTonalIconButton(
                    onClick = {
                        NativeBridge.reset()
                        setMenu(false)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(52.dp),
                ) { Icon(Icons.Default.RestartAlt, "Reset game") }
                FilledTonalIconButton(
                    onClick = { quit() },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(52.dp),
                ) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Quit game") }
            }

            if (discCount > 1) {
                val activeLabel = discLabels.getOrElse(currentDisc) { "Disc ${currentDisc + 1}" }
                Surface(
                    onClick = { navigate("discs") },
                    shape = RoundedCornerShape(18.dp),
                    color = cs.secondaryContainer,
                    contentColor = cs.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Album, null, Modifier.size(18.dp))
                        Text(
                            "Swap disc · $activeLabel of $discCount",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Quick Actions 4-Column Dock (Save, Load, Snap, Screen)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactMenuTile(Icons.Default.Save, "Save", Modifier.weight(1f)) { navigate("save") }
                CompactMenuTile(Icons.Default.Download, "Load", Modifier.weight(1f)) { navigate("load") }
                CompactMenuTile(Icons.Default.PhotoCamera, "Snap", Modifier.weight(1f)) { takeScreenshot() }
                CompactMenuTile(Icons.Default.Tv, "Screen", Modifier.weight(1f)) { navigate("screen") }
            }

            // Tools & Guides 3x3 Expressive Bento Grid (distinct icons, zero bloat)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactMenuTile(
                        Icons.AutoMirrored.Rounded.MenuBook,
                        "Guide",
                        Modifier.weight(1f),
                        accent = true,
                    ) { openPocketGuide() }
                    CompactMenuTile(
                        Icons.Rounded.Public,
                        "Browser",
                        Modifier.weight(1f),
                        accent = true,
                    ) { openInGameBrowser() }
                    CompactMenuTile(
                        Icons.Rounded.Translate,
                        if (liveTranslateOn) "Live ON" else "Translate",
                        Modifier.weight(1f),
                        active = liveTranslateOn,
                        accent = true,
                    ) { runScreenTranslate(translateLang) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactMenuTile(
                        Icons.Rounded.EmojiEvents,
                        "Challenges",
                        Modifier.weight(1f),
                        active = challengeRecording || activeChallengeTitle != null,
                    ) { navigate("share") }
                    CompactMenuTile(
                        Icons.Rounded.Wifi,
                        "Netplay",
                        Modifier.weight(1f),
                    ) { navigate("netplay") }
                    CompactMenuTile(
                        Icons.Default.Code,
                        if (cheats.any { it.enabled }) "Cheats (${cheats.count { it.enabled }})" else "Cheats",
                        Modifier.weight(1f),
                        active = cheats.any { it.enabled },
                    ) { navigate("cheats") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactMenuTile(Icons.Default.OpenWith, "Edit pad", Modifier.weight(1f)) { startPadEdit() }
                    CompactMenuTile(Icons.Rounded.TouchApp, "Smart Touch", Modifier.weight(1f)) { navigate("smart_touch") }
                    CompactMenuTile(Icons.Default.Tune, "Options", Modifier.weight(1f)) { navigate("options") }
                }
            }

            // Live Gameplay Toggles (harmonized with Material 3 Expressive surfaces)
            val consoleSkinSummary = remember(activeDeltaSkin, padSkin, system.id, system.shortName, system.color) {
                activeDeltaSkin?.let { "${it.name} · Delta skin" }
                    ?: when (padSkin.id) {
                        Looks.Console.id -> "Authentic ${system.shortName} · ${ConsoleStyles.forSystem(system.id, system.color).bezelWordmark}"
                        Looks.Instrument.id -> "RetroHero Deck · ${system.shortName}"
                        else -> "${padSkin.name} · ${system.shortName}"
                    }
            }
            SegmentedGroup(null, 9) {
                MenuSwitch(shapes(0), Icons.Default.FastForward, "Fast forward", "${settings.fastForwardSpeed.toInt()}× speed", fastForwardToggle) {
                    fastForwardToggle = it
                    if (it && slowMotion) {
                        slowMotion = false
                        NativeBridge.setSlowMotion(false)
                    }
                    applyFastForward()
                }
                MenuSwitch(shapes(1), Icons.Default.History, "Slow motion", "0.5× precision speed", slowMotion) {
                    slowMotion = it
                    if (it && fastForwardToggle) {
                        fastForwardToggle = false
                        applyFastForward()
                    }
                    NativeBridge.setSlowMotion(it)
                }
                MenuSwitch(
                    shapes(2), Icons.Default.FastRewind, "Rewind",
                    if (system.id == "psx") "Hold the rewind button" else "Hold the rewind button or L3",
                    rewindOn,
                ) { setRewind(it) }
                SegmentedListItem(
                    onClick = {
                        val next = (runAheadFrames + 1) % 3
                        runAheadFrames = next
                        settings.runAheadFrames = next
                        NativeBridge.setRunAhead(next)
                    },
                    shapes = shapes(3),
                    colors = segmentColors(),
                    leadingContent = { ShapeIcon(Icons.Default.Tune) },
                    supportingContent = {
                        Text(
                            when (runAheadFrames) {
                                1 -> "1 frame · -16.7 ms input lag"
                                2 -> "2 frames · -33.3 ms input lag"
                                else -> "Off · tap to enable on 2D/retro cores"
                            },
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Run-Ahead latency") }
                MenuSwitch(shapes(4), Icons.Default.Tv, "Live FPS HUD", "FPS, frame time (ms) & Vsync pill", showPerfHud) {
                    showPerfHud = it
                    settings.showPerfHud = it
                }
                MenuSwitch(shapes(5), Icons.Default.Gamepad, "Touch controls", "Show the on-screen gamepad", showPad) { showPad = it }
                MenuSwitch(
                    shapes(6),
                    Icons.Rounded.AutoAwesome,
                    "Ambilight bezel glow",
                    if (ambilightMode.isEnabled) "${ambilightMode.label} · Live OLED edge halo & flash"
                    else "Off · Static bezel",
                    ambilightMode.isEnabled,
                ) { on ->
                    setAmbilight(if (on) gamePrefs.lastActiveAmbilightMode else AmbilightMode.OFF)
                }
                MenuSwitch(shapes(7), Icons.Default.Tune, "Audio rumble", "Feel bass impacts from game audio", audioHapticsOn) {
                    audioHapticsOn = it
                    settings.audioHaptics = it
                    if (!it) audioHaptics.cancel()
                }
                SegmentedListItem(
                    onClick = { navigate("skin") },
                    shapes = shapes(8),
                    colors = segmentColors(),
                    leadingContent = { ShapeIcon(Icons.Default.AspectRatio) },
                    supportingContent = { Text(consoleSkinSummary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Display & shell skin") }
            }
        }
    }

    @Composable
    private fun SmartTouchPage(back: () -> Unit) {
        val macros = remember(system.id) { SmartTouch.availableMacrosFor(system.layout) }
        val activeMacro = macros.getOrNull(smartTouch.macroIndex % macros.size.coerceAtLeast(1))
        val portraitLabel = when (smartTouch.portraitMode) {
            SmartTouch.PORTRAIT_ONE_HAND -> "Always One-Handed (thumb arc)"
            SmartTouch.PORTRAIT_STANDARD -> "Standard Two-Handed"
            else -> if (SmartTouch.isRpgFriendly(game.title)) "Auto · One-Handed RPG active" else "Auto · One-Handed for RPGs"
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("Smart Touch", back)
            SegmentedGroup(null, 6) {
                MenuSwitch(
                    shapes(0),
                    Icons.Default.Gamepad,
                    "Thumb-roll chord zones",
                    "Slide across B→A or Y→B to hold both cleanly",
                    smartTouch.thumbChordZones,
                ) { on -> updateSmartTouch { it.copy(thumbChordZones = on) } }

                MenuSwitch(
                    shapes(1),
                    Icons.Default.OpenWith,
                    "Tap-to-latch L / R shoulders",
                    "Tap once to lock aim or drift · zero claw grip",
                    smartTouch.shoulderLatch,
                ) { on -> updateSmartTouch { it.copy(shoulderLatch = on) } }

                MenuSwitch(
                    shapes(2),
                    Icons.Default.FastForward,
                    "1-thumb Combo Macro pill",
                    activeMacro?.let { "${it.label} (${it.subtitle})" } ?: "Simultaneous chord button",
                    smartTouch.comboMacroEnabled,
                ) { on -> updateSmartTouch { it.copy(comboMacroEnabled = on) } }

                SegmentedListItem(
                    onClick = {
                        if (macros.size > 1) {
                            updateSmartTouch { it.copy(macroIndex = (it.macroIndex + 1) % macros.size, comboMacroEnabled = true) }
                        }
                    },
                    shapes = shapes(3),
                    colors = segmentColors(),
                    leadingContent = { ShapeIcon(Icons.Default.Tune) },
                    supportingContent = {
                        Text(
                            if (activeMacro != null) "${activeMacro.label} · tap to cycle (${macros.size} presets)"
                            else "No multi-button macro needed for ${system.shortName}",
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Combo Macro preset") }

                MenuSwitch(
                    shapes(4),
                    Icons.Default.Tv,
                    "Gyro-to-Analog steering",
                    if (gyroController?.isAvailable == true) "Tilt phone to steer racing & flight games"
                    else "Motion sensor unavailable on this device",
                    smartTouch.gyroSteering,
                ) { on ->
                    if (on && gyroController?.isAvailable != true) {
                        toast("Motion sensor unavailable on this device")
                    } else {
                        updateSmartTouch { it.copy(gyroSteering = on) }
                        if (on) {
                            gyroController?.recenter()
                            toast("Gyro steering centered to current angle")
                        }
                    }
                }

                SegmentedListItem(
                    onClick = {
                        val nextMode = (smartTouch.portraitMode + 1) % 3
                        updateSmartTouch { it.copy(portraitMode = nextMode) }
                    },
                    shapes = shapes(5),
                    colors = segmentColors(),
                    leadingContent = { ShapeIcon(Icons.Default.AspectRatio) },
                    supportingContent = { Text(portraitLabel) },
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Portrait layout mode") }
            }

            if (smartTouch.portraitMode != SmartTouch.PORTRAIT_STANDARD) {
                SegmentedGroup(null, 1) {
                    MenuSwitch(
                        shapes(0),
                        Icons.Default.OpenWith,
                        "Left-handed One-Hand mode",
                        "Anchors portrait thumb arc to the left side",
                        smartTouch.leftHandedPortrait,
                    ) { on -> updateSmartTouch { it.copy(leftHandedPortrait = on) } }
                }
            }

            if (smartTouch.gyroSteering) {
                Button(
                    onClick = {
                        gyroController?.recenter()
                        toast("Gyro center reset")
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Re-center gyro angle")
                }
            }
        }
    }

    @Composable
    private fun DeltaSkinPage(back: () -> Unit) {
        val authenticStyle = remember(system.id, system.color) { ConsoleStyles.forSystem(system.id, system.color) }
        val builtInSkins = Looks.skins
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("${system.shortName} display skin", back)
            SegmentedGroup("Built-in shells for ${system.name}", builtInSkins.size) {
                builtInSkins.forEachIndexed { i, skin ->
                    val selected = activeDeltaSkin == null && padSkin.id == skin.id
                    val title = when (skin.id) {
                        Looks.Console.id -> "Authentic ${system.shortName}"
                        Looks.Instrument.id -> "RetroHero Deck (${system.shortName})"
                        else -> skin.name
                    }
                    val subtitle = when (skin.id) {
                        Looks.Console.id -> "${authenticStyle.bezelWordmark} · ${authenticStyle.bezelSubtitle}".trimEnd(' ', '·')
                        Looks.Instrument.id -> "Tactile hardware shell branded for ${system.shortName}"
                        else -> "Translucent ${skin.name.lowercase()} touch overlay"
                    }
                    SegmentedListItem(
                        onClick = {
                            activeDeltaSkin = null
                            settings.setDeltaSkinFor(system.id, null)
                            padSkin = skin
                            settings.padSkin = skin.id
                            setMenu(false)
                            back()
                        },
                        shapes = shapes(i),
                        colors = segmentColors(),
                        leadingContent = { RadioButton(selected = selected, onClick = null) },
                        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(title) }
                }
            }
            if (deltaSkinsForSystem.isNotEmpty()) {
                SegmentedGroup("Imported ${system.shortName} Delta skins", deltaSkinsForSystem.size) {
                    deltaSkinsForSystem.forEachIndexed { i, skin ->
                        val selected = activeDeltaSkin?.identifier == skin.identifier
                        SegmentedListItem(
                            onClick = {
                                activeDeltaSkin = skin
                                settings.setDeltaSkinFor(system.id, skin.identifier)
                                setMenu(false)
                                back()
                            },
                            shapes = shapes(i),
                            colors = segmentColors(),
                            leadingContent = { RadioButton(selected = selected, onClick = null) },
                            supportingContent = { Text(skin.identifier, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Text(skin.name) }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompactMenuTile(
        icon: ImageVector,
        label: String,
        modifier: Modifier = Modifier,
        active: Boolean = false,
        accent: Boolean = false,
        onClick: () -> Unit,
    ) {
        val cs = MaterialTheme.colorScheme
        val interaction = remember { MutableInteractionSource() }
        val press = rememberPressFeedback(interaction, restCorner = 20.dp, pressedCorner = 12.dp, pressedScale = 0.95f)
        val bg = when {
            active -> cs.primaryContainer
            accent -> cs.secondaryContainer
            else -> cs.surfaceContainerHigh
        }
        val fg = when {
            active -> cs.onPrimaryContainer
            accent -> cs.onSecondaryContainer
            else -> cs.onSurface
        }
        Surface(
            shape = RoundedCornerShape(press.corner.value),
            color = bg,
            contentColor = fg,
            modifier = modifier
                .height(62.dp)
                .graphicsLayer {
                    scaleX = press.scale.value
                    scaleY = press.scale.value
                }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, null, Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    @Composable
    private fun MenuSwitch(
        shapes: ListItemShapes,
        icon: ImageVector,
        title: String,
        subtitle: String,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ) {
        SegmentedListItem(
            onClick = { onChange(!checked) },
            shapes = shapes,
            colors = segmentColors(),
            leadingContent = { ShapeIcon(icon) },
            supportingContent = { Text(subtitle) },
            trailingContent = {
                MonoSwitch(checked, onChange)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) { Text(title) }
    }

    @Composable
    private fun PageHeader(title: String, back: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            FilledTonalIconButton(onClick = back, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    }

    @Composable
    private fun SlotPage(saving: Boolean, back: () -> Unit) {
        val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
        val slots = buildList {
            if (!saving) add(AppPaths.AUTO_SLOT)
            addAll(1..4)
        }
        Column {
            PageHeader(if (saving) "Save state" else "Load state", back)
            SegmentedGroup(null, slots.size) {
                slots.forEachIndexed { i, slot ->
                    val file = paths.stateFile(game, slot)
                    val exists = file.exists()
                    val label = if (slot == AppPaths.AUTO_SLOT) "Auto-save" else "Slot $slot"
                    val savedCoreId = if (exists) gamePrefs.stateCore(game.id, slot) else null
                    val coreMismatch = !saving && exists && savedCoreId != null && savedCoreId != activeCoreId
                    val savedCoreName = savedCoreId?.let { cid ->
                        system.cores.firstOrNull { it.id == cid }?.displayName ?: cid
                    }
                    val subtitle = when {
                        !exists -> "Empty"
                        coreMismatch -> "Saved on $savedCoreName · switch core to load"
                        else -> fmt.format(Date(file.lastModified()))
                    }
                    SegmentedListItem(
                        onClick = {
                            if (coreMismatch) {
                                toast("Created on $savedCoreName — switch back to load this state, or use your portable battery save")
                                return@SegmentedListItem
                            }
                            val ok = if (saving) {
                                val saved = NativeBridge.saveState(file.absolutePath)
                                if (saved) {
                                    gamePrefs.setStateCore(game.id, slot, activeCoreId)
                                    flushCanonicalSram()
                                }
                                saved
                            } else {
                                val loaded = NativeBridge.loadState(file.absolutePath)
                                if (loaded) demoteRaHardcore("save state loaded")
                                loaded
                            }
                            toast(
                                when {
                                    ok && saving -> "Saved to $label · Battery save synced"
                                    ok -> "Loaded $label"
                                    else -> NativeBridge.getLastError().ifEmpty { "Failed" }
                                },
                            )
                            if (ok) setMenu(false)
                            back()
                        },
                        enabled = saving || exists,
                        shapes = shapes(i),
                        colors = segmentColors(),
                        leadingContent = {
                            Icon(
                                if (slot == AppPaths.AUTO_SLOT) Icons.Default.History else if (exists) Icons.Default.Save else Icons.Default.Add,
                                null,
                            )
                        },
                        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(label) }
                }
            }
        }
    }

    @Composable
    private fun OptionsPage(back: () -> Unit) {
        var options by remember { mutableStateOf(NativeBridge.options()) }
        var editing by remember { mutableStateOf<NativeBridge.CoreOption?>(null) }
        Column {
            PageHeader("Core options", back)
            if (options.isEmpty()) {
                Text(
                    "This emulator has no options.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                SegmentedGroup(null, options.size) {
                    options.forEachIndexed { i, opt ->
                        SegmentedListItem(
                            onClick = { editing = opt },
                            shapes = shapes(i),
                            colors = segmentColors(),
                            supportingContent = { Text(opt.current, color = MaterialTheme.colorScheme.primary) },
                        ) { Text(opt.description) }
                    }
                }
            }
        }
        editing?.let { opt ->
            AlertDialog(
                onDismissRequest = { editing = null },
                confirmButton = { TextButton(onClick = { editing = null }) { Text("Close") } },
                title = { Text(opt.description) },
                text = {
                    LazyColumn {
                        items(opt.values) { value ->
                            Row(
                                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).selectable(selected = value == opt.current) {
                                    NativeBridge.setOption(opt.key, value)
                                    optionsStore.save(opt.key, value)
                                    options = NativeBridge.options()
                                    editing = null
                                }.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = value == opt.current, onClick = null)
                                Spacer(Modifier.size(12.dp))
                                Text(value)
                            }
                        }
                    }
                },
            )
        }
    }

    @Composable
    private fun ScreenPage(back: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        val prof = activeProfile
        val gfx = graphicsConfig
        val is3D = com.retrohero.app.data.GraphicsCoreOptions.is3DCore(activeCoreId)
        val hasPgxp = com.retrohero.app.data.GraphicsCoreOptions.supportsPgxp(activeCoreId)
        val hasWideHack = com.retrohero.app.data.GraphicsCoreOptions.supportsWidescreenHack(activeCoreId)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("Graphics & screen", back)

            // 1. Hardware Intelligence Profile (Authentic vs Enhanced)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Hardware profile", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                        if (prof?.isGameSpecific == true) {
                            Surface(shape = RoundedCornerShape(999.dp), color = cs.tertiaryContainer) {
                                Text(
                                    "Auto-tuned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.retrohero.app.data.VisualMode.entries.forEach { mode ->
                            val selected = visualMode == mode
                            Surface(
                                onClick = {
                                    applyVisualMode(mode)
                                    toast("${mode.label}: ${activeProfile?.title ?: mode.tagline}")
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) cs.primaryContainer else cs.surfaceContainerHigh,
                                contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, null, Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                    }
                                    Text(mode.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                            }
                        }
                    }
                    if (prof != null) {
                        Text(
                            "${prof.title} — ${prof.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }

            // 1b. Live Framebuffer OLED Ambilight Bezel Glow Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                            Text("OLED Ambilight bezel glow", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = if (ambilightMode.isEnabled) cs.primaryContainer else cs.surfaceContainerHigh) {
                            Text(
                                ambilightMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ambilightMode.isEnabled) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AmbilightMode.entries.forEach { mode ->
                            val sel = ambilightMode == mode
                            Surface(
                                onClick = {
                                    setAmbilight(mode)
                                    toast("Ambilight: ${mode.label}")
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (sel) cs.primaryContainer else cs.surfaceContainerHigh,
                                contentColor = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                    if (ambilightState.isVisiblyGlowing) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(ambilightState.left, ambilightState.top, ambilightState.bottom, ambilightState.right),
                                    ),
                                ),
                        )
                    }
                    Text(
                        text = ambilightMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            // 2. Resolution & Upscaling Card (1x..4x 3D Internal Res or 2D Crisp Prescale + 3D Enhancements)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (is3D) "Internal resolution upscale" else "Pixel upscaling multiplier",
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface,
                        )
                        Surface(shape = RoundedCornerShape(999.dp), color = cs.secondaryContainer) {
                            Text(
                                "${gfx.upscale}×",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "1× Native", 2 to "2× HD", 3 to "3× FHD", 4 to "4× QHD").forEach { (scale, label) ->
                            val sel = gfx.upscale == scale
                            Surface(
                                onClick = {
                                    updateGraphics { it.copy(upscale = scale) }
                                    toast(com.retrohero.app.data.GraphicsCoreOptions.upscaleDescription(activeCoreId, scale))
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (sel) cs.primaryContainer else cs.surfaceContainerHigh,
                                contentColor = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = if (is3D) label else "${scale}×",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = com.retrohero.app.data.GraphicsCoreOptions.upscaleDescription(activeCoreId, gfx.upscale),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )

                    if (is3D) {
                        Text("Texture filtering", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.retrohero.app.data.TextureFilterMode.entries.forEach { tf ->
                                val sel = gfx.textureFilter == tf
                                Surface(
                                    onClick = { updateGraphics { it.copy(textureFilter = tf) } },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (sel) cs.secondaryContainer else cs.surfaceContainerHigh,
                                    contentColor = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        tf.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Aspect Ratio Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aspect ratio", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.retrohero.app.data.AspectRatioMode.entries.forEach { mode ->
                            val sel = gfx.aspectRatio == mode
                            Surface(
                                onClick = { updateGraphics { it.copy(aspectRatio = mode) } },
                                shape = RoundedCornerShape(999.dp),
                                color = if (sel) cs.primaryContainer else cs.surfaceContainerHigh,
                                contentColor = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 4. Shaders & Pixel Upscalers
            SegmentedGroup("Shader & pixel upscaler", ScreenFilter.labels.size) {
                ScreenFilter.labels.forEachIndexed { i, label ->
                    SegmentedListItem(
                        onClick = { setScreen(newFilter = i) },
                        shapes = shapes(i),
                        colors = segmentColors(),
                        leadingContent = { RadioButton(selected = filter == i, onClick = null) },
                        supportingContent = { Text(ScreenFilter.descriptions[i]) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(label) }
                }
            }

            // 5. Color Matrix, 3D Geometry Fixes, Overscan & Display Sync
            val extra3dSwitches = (if (hasPgxp) 1 else 0) + (if (hasWideHack) 1 else 0)
            val totalRows = 5 + extra3dSwitches
            SegmentedGroup("Color, geometry & display sync", totalRows) {
                var row = 0
                val safeColorIdx = colorProfile.coerceIn(0, com.retrohero.app.data.ColorProfile.labels.lastIndex)
                SegmentedListItem(
                    onClick = {
                        val next = (safeColorIdx + 1) % com.retrohero.app.data.ColorProfile.labels.size
                        setColorMatrix(next)
                    },
                    shapes = shapes(row++),
                    colors = segmentColors(),
                    leadingContent = { ShapeIcon(Icons.Default.Tune) },
                    supportingContent = {
                        Text("${com.retrohero.app.data.ColorProfile.labels[safeColorIdx]} · ${com.retrohero.app.data.ColorProfile.descriptions[safeColorIdx]}")
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Hardware color matrix") }

                if (hasPgxp) {
                    MenuSwitch(
                        shapes(row++),
                        Icons.Default.Tv,
                        "PGXP anti-wobble 3D",
                        "Eliminates PS1 polygon jitter & warped textures",
                        gfx.pgxpEnabled,
                    ) { on -> updateGraphics { it.copy(pgxpEnabled = on) } }
                }

                if (hasWideHack) {
                    MenuSwitch(
                        shapes(row++),
                        Icons.Default.AspectRatio,
                        "3D Widescreen hack",
                        "Extends 3D camera frustum for true 16:9 gameplay",
                        gfx.widescreenHack,
                    ) { on ->
                        updateGraphics {
                            it.copy(
                                widescreenHack = on,
                                aspectRatio = if (on) com.retrohero.app.data.AspectRatioMode.WIDESCREEN_16_9
                                else com.retrohero.app.data.AspectRatioMode.AUTO,
                            )
                        }
                    }
                }

                MenuSwitch(
                    shapes(row++),
                    Icons.Default.AspectRatio,
                    "Crop TV overscan",
                    "Trims noisy CRT edge borders on NES/SNES/Genesis/PS1",
                    gfx.cropOverscan,
                ) { on -> updateGraphics { it.copy(cropOverscan = on) } }

                MenuSwitch(
                    shapes(row++),
                    Icons.Default.AspectRatio,
                    "Integer scaling",
                    "Every game pixel the exact same size, with a clean border",
                    integerScale,
                ) { setScreen(integer = it) }

                MenuSwitch(
                    shapes(row++),
                    Icons.Default.Tv,
                    "Tear-free Vsync lock",
                    "Choreographer vsync + micro-pitch audio · ${displayHz.roundToInt()} Hz screen",
                    vsyncLock,
                ) { setDisplaySync(vsync = it) }

                MenuSwitch(
                    shapes(row),
                    Icons.Default.History,
                    "120 Hz OLED Black Frame Insertion",
                    if (bfiCapable) "CRT-sharp motion clarity on 120 Hz OLED"
                    else "Requires a 120 Hz display (detected ${displayHz.roundToInt()} Hz)",
                    bfi120Hz && bfiCapable,
                ) { on ->
                    if (!bfiCapable && on) {
                        toast("Black Frame Insertion requires a 120 Hz display")
                    } else {
                        setDisplaySync(vsync = if (on) true else vsyncLock, bfi = on)
                    }
                }
            }

            Text(
                "Graphics & upscaling settings are saved per ${system.shortName}; hardware profile is saved per game.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    @Composable
    private fun CheatsPage(back: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        var adding by remember { mutableStateOf(false) }
        var deleting by remember { mutableStateOf<Cheat?>(null) }
        var query by remember { mutableStateOf("") }
        var filterTab by remember { mutableStateOf("all") } // "all", "popular", "active"
        var visibleLimit by remember { mutableIntStateOf(50) }

        LaunchedEffect(cheatsSupported) {
            if (cheatsSupported && cheats.isEmpty() && !cheatAutoTried && !cheatSearching) {
                autoFindCheats(silentIfNone = true)
            }
        }

        val indexedCheats = remember(cheats, query, filterTab) {
            val q = query.trim().lowercase()
            cheats.mapIndexed { idx, c -> idx to c }
                .filter { (_, c) ->
                    val tabOk = when (filterTab) {
                        "active" -> c.enabled
                        "popular" -> CheatFinder.isPopularCheat(c.name) || c.enabled
                        else -> true
                    }
                    val queryOk = q.isEmpty() ||
                        c.name.lowercase().contains(q) ||
                        c.code.lowercase().contains(q)
                    tabOk && queryOk
                }
                .sortedByDescending { (_, c) -> c.enabled }
        }
        val activeCount = remember(cheats) { cheats.count { it.enabled } }
        val popularCount = remember(cheats) { cheats.count { CheatFinder.isPopularCheat(it.name) } }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PageHeader("Cheats", back)
            when {
                !cheatsSupported -> Text(
                    "This emulator core doesn't support cheat codes.",
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                )
                else -> {
                    // Primary actions: Auto-find cheats + Add manual code
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { autoFindCheats(silentIfNone = false) },
                            enabled = !cheatSearching,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (cheatSearching) "Searching…" else "Auto-find")
                        }
                        FilledTonalButton(
                            onClick = { adding = true },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add code")
                        }
                    }

                    if (cheatSearching) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cs.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ContainedLoadingIndicator(Modifier.size(34.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Finding cheats for ${game.title}…", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Checking libretro-database & offline classics",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    cheatSearchSource?.let { src ->
                        Text(
                            text = "Matched: $src",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    cheatSearchError?.let { err ->
                        if (cheats.isEmpty() && !cheatSearching) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // Alternate .cht packs (e.g., Game Genie / Action Replay / GameShark / Code Breaker)
                    if (cheatAlternatePacks.size > 1) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            cheatAlternatePacks.forEach { pack ->
                                val selected = activeCheatPack?.fileName == pack.fileName
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectCheatPack(pack) },
                                    label = { Text(pack.displayLabel, maxLines = 1) },
                                )
                            }
                        }
                    }

                    if (cheats.isEmpty() && !cheatSearching) {
                        Text(
                            "Tap Auto-find to discover Game Genie, Action Replay, GameShark or Code Breaker codes for this game.",
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else if (cheats.isNotEmpty()) {
                        // Search + filter bar when a cheat pack has more than a handful of entries
                        if (cheats.size > 5) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    visibleLimit = 50
                                },
                                placeholder = { Text("Search ${cheats.size} cheats (lives, health, coins…)") },
                                leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = if (query.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilterChip(
                                    selected = filterTab == "all",
                                    onClick = { filterTab = "all"; visibleLimit = 50 },
                                    label = { Text("All (${cheats.size})") },
                                )
                                if (popularCount > 0) {
                                    FilterChip(
                                        selected = filterTab == "popular",
                                        onClick = { filterTab = "popular"; visibleLimit = 50 },
                                        label = { Text("Popular ($popularCount)") },
                                    )
                                }
                                FilterChip(
                                    selected = filterTab == "active",
                                    onClick = { filterTab = "active"; visibleLimit = 50 },
                                    label = { Text("Active ($activeCount)") },
                                )
                                if (activeCount > 0) {
                                    TextButton(onClick = {
                                        updateCheats(cheats.map { it.copy(enabled = false) })
                                        toast("All cheats disabled")
                                    }) { Text("Disable all") }
                                }
                            }
                        }

                        val shown = indexedCheats.take(visibleLimit)
                        if (shown.isEmpty()) {
                            Text(
                                if (filterTab == "active") "No active cheats yet. Flip any switch to enable a code."
                                else "No cheats match “$query”.",
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            SegmentedGroup(null, shown.size) {
                                shown.forEachIndexed { rowIdx, (origIndex, cheat) ->
                                    SegmentedListItem(
                                        onClick = {
                                            updateCheats(cheats.toMutableList().also {
                                                it[origIndex] = cheat.copy(enabled = !cheat.enabled)
                                            })
                                        },
                                        onLongClick = { deleting = cheat },
                                        shapes = shapes(rowIdx),
                                        colors = segmentColors(),
                                        supportingContent = { Text(cheat.code, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        trailingContent = {
                                            MonoSwitch(cheat.enabled, { on ->
                                                updateCheats(cheats.toMutableList().also {
                                                    it[origIndex] = cheat.copy(enabled = on)
                                                })
                                            })
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) { Text(cheat.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                            if (indexedCheats.size > shown.size) {
                                val remaining = indexedCheats.size - shown.size
                                FilledTonalButton(
                                    onClick = { visibleLimit += 50 },
                                    shapes = ButtonDefaults.shapes(),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Show more (${remaining} remaining)")
                                }
                            }
                            Text(
                                "Hold any code to delete it · Auto-found cheats start off so Hardcore achievements stay safe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        if (adding) AddCheatDialog(onDismiss = { adding = false }) { name, code ->
            updateCheats(cheats + Cheat(name, code))
            adding = false
        }
        deleting?.let { c ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("Delete “${c.name}”?") },
                confirmButton = { TextButton(onClick = { updateCheats(cheats - c); deleting = null }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
            )
        }
    }

    @Composable
    private fun AddCheatDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        val normalised = Cheats.normalise(code)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add cheat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        code, { code = it },
                        label = { Text("Code") },
                        supportingText = { Text("One code per line for multi-part cheats") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = normalised.isNotEmpty(), onClick = { onAdd(name.trim().ifEmpty { "Cheat ${cheats.size + 1}" }, normalised) }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }

    /** Shown over the game while moving touch controls. */
    @Composable
    private fun PadEditBar(modifier: Modifier) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = modifier.safeDrawingPadding().padding(12.dp).widthIn(max = 520.dp),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text("Edit controls", style = MaterialTheme.typography.titleMedium)
                val sel = padSelected
                if (sel == null) {
                    Text(
                        "Drag a group of buttons to move it. Tap one to resize it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val a = padAdjust[sel] ?: PadAdjust()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
                        Slider(
                            value = a.scale,
                            onValueChange = { padAdjust = padAdjust + (sel to a.copy(scale = it)) },
                            valueRange = 0.6f..1.6f,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${(a.scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { padAdjust = emptyMap(); padSelected = null }) { Text("Reset all") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { finishPadEdit(save = false) }) { Text("Cancel") }
                    Button(onClick = { finishPadEdit(save = true) }, shapes = ButtonDefaults.shapes()) { Text("Done") }
                }
            }
        }
    }

    @Composable
    private fun ScreenshotFlash(trigger: Int) {
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(trigger) {
            if (trigger == 0) return@LaunchedEffect
            alpha.snapTo(0.7f)
            alpha.animateTo(0f, tween(350))
        }
        if (alpha.value > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha.value)))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        /**
         * The largest whole multiple of the frame height that fits [maxW] x [maxH] at [aspect];
         * null when even 1x does not fit.
         */
        fun integerFit(maxW: Float, maxH: Float, frameH: Int, aspect: Float): Pair<Float, Float>? {
            var k = (maxH / frameH).toInt()
            while (k >= 1) {
                val h = frameH * k.toFloat()
                val w = h * aspect
                if (w <= maxW) return w to h
                k--
            }
            return null
        }

        private const val EXTRA_GAME = "game"
        private const val EXTRA_CORE_PATH = "core_path"
        private const val EXTRA_CORE_ID = "core_id"
        private const val EXTRA_ROM_PATH = "rom_path"

        private const val EXTRA_LOAD_SLOT = "load_slot"
        private const val EXTRA_STATE_PATH = "state_path"
        private const val EXTRA_CHALLENGE_TITLE = "challenge_title"
        private const val EXTRA_PATCH_PATH = "patch_path"
        private const val EXTRA_PATCH_NAME = "patch_name"
        private const val EXTRA_PATCH_FORMAT = "patch_format"

        /** [slot] or [statePath]: resume from a save state instead of the usual auto-resume. */
        fun intent(
            context: Context,
            game: Game,
            coreId: String,
            corePath: String,
            romPath: String,
            slot: Int? = null,
            statePath: String? = null,
            challengeTitle: String? = null,
            patchPath: String? = null,
            patchName: String? = null,
            patchFormat: String? = null,
        ) =
            Intent(context, EmulatorActivity::class.java)
                .putExtra(EXTRA_GAME, game.toJson().toString())
                .putExtra(EXTRA_CORE_ID, coreId)
                .putExtra(EXTRA_CORE_PATH, corePath)
                .putExtra(EXTRA_ROM_PATH, romPath)
                .apply {
                    if (slot != null) putExtra(EXTRA_LOAD_SLOT, slot)
                    if (!statePath.isNullOrBlank()) putExtra(EXTRA_STATE_PATH, statePath)
                    if (!challengeTitle.isNullOrBlank()) putExtra(EXTRA_CHALLENGE_TITLE, challengeTitle)
                    if (!patchPath.isNullOrBlank()) putExtra(EXTRA_PATCH_PATH, patchPath)
                    if (!patchName.isNullOrBlank()) putExtra(EXTRA_PATCH_NAME, patchName)
                    if (!patchFormat.isNullOrBlank()) putExtra(EXTRA_PATCH_FORMAT, patchFormat)
                }
    }

    @Composable
    private fun ShareMomentPage(back: () -> Unit) {
        var title by remember { mutableStateOf("${game.title} Challenge") }
        var note by remember { mutableStateOf("") }
        var winTypeIdx by remember { mutableIntStateOf(0) } // 0=Time Trial, 1=Survive 30s, 2=RAM Goal, 3=Free Snapshot
        var surviveSecs by remember { mutableStateOf("30") }
        var ramAddrHex by remember { mutableStateOf("0000") }
        var ramTargetVal by remember { mutableStateOf("0") }

        fun buildWinCondition(): com.retrohero.app.data.ChallengeWinCondition? = when (winTypeIdx) {
            0 -> com.retrohero.app.data.ChallengeWinCondition(
                type = com.retrohero.app.data.ChallengeWinType.TIME_TRIAL,
                label = "Beat the ghost run",
            )
            1 -> com.retrohero.app.data.ChallengeWinCondition(
                type = com.retrohero.app.data.ChallengeWinType.SURVIVE,
                targetSeconds = (surviveSecs.toIntOrNull() ?: 30).coerceIn(5, 300),
            )
            2 -> {
                val off = ramAddrHex.trim().removePrefix("0x").removePrefix("0X").toIntOrNull(16) ?: 0
                val tv = ramTargetVal.trim().toIntOrNull() ?: 0
                com.retrohero.app.data.ChallengeWinCondition(
                    type = com.retrohero.app.data.ChallengeWinType.RAM_GOAL,
                    ramOffset = off,
                    ramSize = 1,
                    op = com.retrohero.app.data.ChallengeRamOp.EQ,
                    targetValue = tv,
                )
            }
            else -> null
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("Challenge snapshots", back)
            Text(
                "Record a deterministic save state + input ghost + optional win condition as a tiny .rhclip file (no ROM included) so a friend drops straight into this exact frame.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Challenge title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(240) },
                label = { Text("Rule or trash-talk note (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Ghost Race", "Survive", "RAM Goal", "Snapshot").forEachIndexed { idx, label ->
                    val sel = winTypeIdx == idx
                    Surface(
                        onClick = { winTypeIdx = idx },
                        shape = RoundedCornerShape(999.dp),
                        color = if (sel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (sel) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            if (winTypeIdx == 1) {
                OutlinedTextField(
                    value = surviveSecs,
                    onValueChange = { surviveSecs = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Survive duration (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (winTypeIdx == 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ramAddrHex,
                        onValueChange = { ramAddrHex = it.take(8) },
                        label = { Text("RAM offset (hex)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = ramTargetVal,
                        onValueChange = { ramTargetVal = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Target == value") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Button(
                onClick = { startChallengeRecording(title, note, buildWinCondition()) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Record Live Challenge Ghost (up to 60s)") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { shareMoment(title, note, winCondition = buildWinCondition()) },
                    modifier = Modifier.weight(1f),
                ) { Text("Instant snapshot") }
                TextButton(
                    onClick = { challengePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("Load .rhclip") }
            }
        }
    }

    @Composable
    private fun NetplayPage(back: () -> Unit) {
        val npState by netplaySession.state.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("Rollback netplay", back)
            NetplayPanel(
                coreId = activeCoreId,
                state = npState,
                onHost = { delayFrames ->
                    netplaySession.hostRoom(delayFrames)
                },
                onJoin = { codeOrInvite, delayFrames ->
                    netplaySession.joinRoom(codeOrInvite, delayFrames)
                },
                onStop = {
                    netplaySession.stop()
                    toast("Rollback Netplay disconnected")
                },
                onCopyInvite = { invite ->
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("RetroHero Netplay", invite))
                    toast("Room invite copied!")
                },
                onShareInvite = { invite ->
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "Join my RetroHero Rollback Netplay room for ${game.title}:\n$invite")
                    startActivity(Intent.createChooser(send, "Share Netplay room"))
                },
            )
        }
    }
}
