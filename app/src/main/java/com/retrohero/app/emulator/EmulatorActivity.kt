package com.retrohero.app.emulator

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.retrohero.app.data.Cheat
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
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
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
    private var padSkin: PadSkin = Looks.Console
    private var currentCoreId: String = ""
    private var challengeBanner by mutableStateOf<String?>(null)
    private var deltaSkinsForSystem: List<DeltaSkin> = emptyList()
    private var activeDeltaSkin by mutableStateOf<DeltaSkin?>(null)

    private var phase by mutableStateOf<Phase>(Phase.Loading)
    private var menuOpen by mutableStateOf(false)
    private var showPad by mutableStateOf(true)
    private var audioHapticsOn by mutableStateOf(false)
    private var fastForwardToggle by mutableStateOf(false)
    private var aspect by mutableFloatStateOf(4f / 3f)
    private var frameSize by mutableStateOf(IntSize.Zero)
    private var filter by mutableIntStateOf(0)
    private var integerScale by mutableStateOf(false)
    private var rewindOn by mutableStateOf(false)
    private var cheats by mutableStateOf<List<Cheat>>(emptyList())
    private var cheatsSupported by mutableStateOf(false)
    private var flash by mutableIntStateOf(0)
    /** Touch-control editing: null when not editing. */
    private var editingPad by mutableStateOf(false)
    private var padAdjust by mutableStateOf<Map<String, PadAdjust>>(emptyMap())
    private var padSelected by mutableStateOf<String?>(null)
    private var padLandscape = false
    private var translateState by mutableStateOf<TranslateState>(TranslateState.Idle)
    private var translateLang by mutableStateOf(TranslateSourceLang.JAPANESE)

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
        cheats = Cheats.load(paths.cheatsFile(game))
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
        val externalState = intent.getStringExtra(EXTRA_STATE_PATH)?.let(::File)?.takeIf { it.isFile }
        val requested = if (intent.hasExtra(EXTRA_LOAD_SLOT)) intent.getIntExtra(EXTRA_LOAD_SLOT, AppPaths.AUTO_SLOT) else null
        raSessionHardcore = settings.raProfile.value != null && settings.raHardcore.value &&
            externalState == null && requested == null && cheats.none { it.enabled }
        val error = withContext(Dispatchers.IO) {
            com.retrohero.app.data.CoreTuning.defaultsFor(coreId).forEach { (k, v) -> NativeBridge.setOption(k, v) }
            optionsStore.load().forEach { (k, v) -> NativeBridge.setOption(k, v) }
            if (!NativeBridge.loadCore(corePath, paths.system.absolutePath, paths.saves.absolutePath)) {
                return@withContext NativeBridge.getLastError()
            }
            if (!NativeBridge.loadGame(romPath, paths.saveRam(game).absolutePath)) {
                return@withContext NativeBridge.getLastError()
            }
            val resume = when {
                externalState != null -> externalState
                raSessionHardcore -> null // Hardcore mode starts cleanly from boot
                requested != null -> paths.stateFile(game, requested)
                settings.autoSave -> paths.stateFile(game, AppPaths.AUTO_SLOT)
                else -> null
            }
            if (resume != null && resume.exists()) NativeBridge.loadState(resume.absolutePath)
            null
        }
        if (error != null) {
            phase = Phase.Failed(error.ifEmpty { "Failed to start the game." })
            return
        }
        NativeBridge.setFilter(filter)
        rewindOn = gamePrefs.rewind && !raSessionHardcore
        NativeBridge.setRewindEnabled(rewindOn)
        cheatsSupported = NativeBridge.supportsCheats()
        if (!raSessionHardcore) applyCheats()
        aspect = NativeBridge.getAspectRatio()
        NativeBridge.start()
        started = true
        phase = Phase.Running
        startClock()
        startRetroAchievements(romPath)
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

    private fun setScreen(newFilter: Int = filter, integer: Boolean = integerScale) {
        filter = newFilter
        integerScale = integer
        gamePrefs.setFilter(system.id, newFilter)
        gamePrefs.setIntegerScale(system.id, integer)
        NativeBridge.setFilter(newFilter)
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

    /** Packages the current state + screenshot into a shareable `.rhclip` capsule and opens the share sheet. */
    private fun shareMoment(title: String, note: String) {
        lifecycleScope.launch {
            val clipFile = withContext(Dispatchers.IO) {
                runCatching {
                    val tmpState = File(paths.clipTmp, "moment-${System.nanoTime()}.state")
                    if (!NativeBridge.saveState(tmpState.absolutePath)) error("Couldn't save state")
                    val tmpPng = NativeBridge.grabFrame()?.let { raw ->
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
                    val manifest = com.retrohero.app.data.MomentManifest(
                        title = title.trim().ifEmpty { game.title },
                        note = note.trim(),
                        systemId = system.id,
                        coreId = currentCoreId,
                        gameTitle = game.title,
                        gameBaseName = game.baseName,
                    )
                    val safeName = Regex("[^a-zA-Z0-9._-]+").replace(manifest.title, "_").trim('_').ifEmpty { "moment" }.take(40)
                    val dest = File(paths.clips, "$safeName-${System.currentTimeMillis()}.rhclip")
                    dest.outputStream().use { com.retrohero.app.data.MomentCapsule.export(it, manifest, tmpState, tmpPng) }
                    tmpState.delete()
                    tmpPng?.delete()
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
                .putExtra(Intent.EXTRA_SUBJECT, "RetroHero Moment: ${title.ifBlank { game.title }}")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, "Share .rhclip moment"))
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
        if (started && !shutDown && !menuOpen && !editingPad) {
            NativeBridge.setPaused(false)
            startClock()
        }
    }

    private fun applyFastForward() {
        NativeBridge.setFastForward(fastForwardToggle || fastForwardHeld, settings.fastForwardSpeed)
    }

    override fun onPause() {
        super.onPause()
        if (started && !shutDown) {
            audioHaptics.cancel()
            NativeBridge.setPaused(true)
            NativeBridge.setRewinding(false)
            NativeBridge.saveSram(paths.saveRam(game).absolutePath)
            stopClock()
        }
    }

    override fun onResume() {
        super.onResume()
        if (started && !shutDown && !menuOpen && !editingPad && translateState is TranslateState.Idle) {
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
        raJob?.cancel()
        raJob = null
        stopClock()
        shutDown = true
        NativeBridge.setPaused(true)
        if (settings.autoSave) NativeBridge.saveState(paths.stateFile(game, AppPaths.AUTO_SLOT).absolutePath)
        NativeBridge.shutdown(paths.saveRam(game).absolutePath)
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
                toast(if (ok) "Quick saved" else NativeBridge.getLastError().ifEmpty { "Quick save failed" })
            }
            DeltaAction.QUICK_LOAD -> if (pressed) {
                val file = paths.stateFile(game, AppPaths.QUICK_SLOT)
                if (!file.exists()) {
                    toast("No quick save yet")
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
            } else if (landscape) {
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

            if (menuOpen) PauseMenu()
            if (translateState !is TranslateState.Idle) {
                TranslateOverlay(
                    state = translateState,
                    sourceLang = translateLang,
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
        LaunchedEffect(phase) {
            while (phase is Phase.Running) {
                val sz = NativeBridge.getFrameSize()
                if (sz[0] > 0 && sz[1] > 0) frameSize = IntSize(sz[0], sz[1])
                delay(1000)
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
            onSustainedChanged = onSustainedChanged,
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    private fun GameView() {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val size = if (integerScale && frameSize.height > 0) {
                integerFit(
                    with(density) { maxWidth.toPx() },
                    with(density) { maxHeight.toPx() },
                    frameSize.height,
                    aspect,
                )?.let { (w, h) -> with(density) { Modifier.size(w.toDp(), h.toDp()) } }
            } else null
            GameSurface(size ?: Modifier.aspectRatio(aspect))
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
                        override fun surfaceCreated(holder: SurfaceHolder) = NativeBridge.setSurface(holder.surface)
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
                        override fun surfaceDestroyed(holder: SurfaceHolder) = NativeBridge.setSurface(null)
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
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = null, indication = null) { if (page == "main") setMenu(false) else page = "main" }
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 460.dp)
                    .clickable(interactionSource = null, indication = null) {},
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SystemBadge(system, 52.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(system.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(120))
                        },
                        label = "menuPage",
                    ) { p ->
                        when (p) {
                            "save" -> SlotPage(saving = true) { page = "main" }
                            "load" -> SlotPage(saving = false) { page = "main" }
                            "options" -> OptionsPage { page = "main" }
                            "cheats" -> CheatsPage { page = "main" }
                            "screen" -> ScreenPage { page = "main" }
                            "skin" -> DeltaSkinPage { page = "main" }
                            "share" -> ShareMomentPage { page = "main" }
                            else -> MainMenuPage { page = it }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainMenuPage(navigate: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { setMenu(false) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
                    Spacer(Modifier.width(8.dp))
                    Text("Resume", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
                }
                FilledTonalIconButton(
                    onClick = { quit() },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(ButtonDefaults.MediumContainerHeight),
                ) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Quit game") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(Icons.Default.Save, "Save", Modifier.weight(1f)) { navigate("save") }
                ActionTile(Icons.Default.Download, "Load", Modifier.weight(1f)) { navigate("load") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(Icons.Default.PhotoCamera, "Screenshot", Modifier.weight(1f)) { takeScreenshot() }
                ActionTile(Icons.Default.Tv, "Screen", Modifier.weight(1f)) { navigate("screen") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(Icons.Default.Code, if (cheats.any { it.enabled }) "Cheats · ${cheats.count { it.enabled }}" else "Cheats", Modifier.weight(1f)) { navigate("cheats") }
                ActionTile(Icons.Default.OpenWith, "Edit controls", Modifier.weight(1f)) { startPadEdit() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(Icons.Default.Tune, "Options", Modifier.weight(1f)) { navigate("options") }
                ActionTile(Icons.Default.RestartAlt, "Reset", Modifier.weight(1f)) {
                    NativeBridge.reset()
                    setMenu(false)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(Icons.Default.AspectRatio, "Translate (${translateLang.label})", Modifier.weight(1f)) {
                    runScreenTranslate(translateLang)
                }
                ActionTile(Icons.Default.Save, "Share moment", Modifier.weight(1f)) { navigate("share") }
            }
            val switchCount = if (deltaSkinsForSystem.isNotEmpty()) 5 else 4
            SegmentedGroup(null, switchCount) {
                MenuSwitch(shapes(0), Icons.Default.FastForward, "Fast forward", "${settings.fastForwardSpeed.toInt()}× speed", fastForwardToggle) {
                    fastForwardToggle = it
                    applyFastForward()
                }
                MenuSwitch(
                    shapes(1), Icons.Default.FastRewind, "Rewind",
                    if (system.id == "psx") "Hold the rewind button" else "Hold the rewind button or L3",
                    rewindOn,
                ) { setRewind(it) }
                MenuSwitch(shapes(2), Icons.Default.Gamepad, "Touch controls", "Show the on-screen gamepad", showPad) { showPad = it }
                MenuSwitch(shapes(3), Icons.Default.Tune, "Audio rumble", "Feel bass impacts from game audio", audioHapticsOn) {
                    audioHapticsOn = it
                    settings.audioHaptics = it
                    if (!it) audioHaptics.cancel()
                }
                if (deltaSkinsForSystem.isNotEmpty()) {
                    SegmentedListItem(
                        onClick = { navigate("skin") },
                        shapes = shapes(4),
                        colors = segmentColors(),
                        leadingContent = { ShapeIcon(Icons.Default.AspectRatio) },
                        supportingContent = { Text(activeDeltaSkin?.name ?: "Built-in RetroHero pad") },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text("Delta skin") }
                }
            }
        }
    }

    @Composable
    private fun DeltaSkinPage(back: () -> Unit) {
        val options: List<DeltaSkin?> = listOf(null) + deltaSkinsForSystem
        Column {
            PageHeader("Delta skin", back)
            SegmentedGroup(null, options.size) {
                options.forEachIndexed { i, skin ->
                    val selected = activeDeltaSkin?.identifier == skin?.identifier
                    SegmentedListItem(
                        onClick = {
                            activeDeltaSkin = skin
                            settings.setDeltaSkinFor(system.id, skin?.identifier)
                            setMenu(false)
                            back()
                        },
                        shapes = shapes(i),
                        colors = segmentColors(),
                        leadingContent = {
                            RadioButton(selected = selected, onClick = null)
                        },
                        supportingContent = {
                            Text(if (skin == null) "Use RetroHero's built-in touch pad" else skin.identifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(skin?.name ?: "Built-in controls") }
                }
            }
        }
    }

    @Composable
    private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
        val interaction = remember { MutableInteractionSource() }
        val press = rememberPressFeedback(interaction, restCorner = 28.dp, pressedCorner = 14.dp, pressedScale = 0.96f)
        Surface(
            shape = RoundedCornerShape(press.corner.value),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = modifier
                .height(84.dp)
                .graphicsLayer {
                    scaleX = press.scale.value
                    scaleY = press.scale.value
                }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null)
                Text(label, style = MaterialTheme.typography.titleMedium)
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
                    SegmentedListItem(
                        onClick = {
                            val ok = if (saving) NativeBridge.saveState(file.absolutePath) else NativeBridge.loadState(file.absolutePath)
                            toast(
                                when {
                                    ok && saving -> "Saved to $label"
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
                        supportingContent = { Text(if (exists) fmt.format(Date(file.lastModified())) else "Empty") },
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
        Column {
            PageHeader("Screen", back)
            SegmentedGroup(null, ScreenFilter.labels.size) {
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
            Spacer(Modifier.height(12.dp))
            SegmentedGroup(null, 1) {
                MenuSwitch(shapes(0), Icons.Default.AspectRatio, "Integer scaling", "Every game pixel the same size, with a border", integerScale) {
                    setScreen(integer = it)
                }
            }
            Text(
                "Saved for every ${system.shortName} game.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp),
            )
        }
    }

    @Composable
    private fun CheatsPage(back: () -> Unit) {
        var adding by remember { mutableStateOf(false) }
        var deleting by remember { mutableStateOf<Cheat?>(null) }
        Column {
            PageHeader("Cheats", back)
            when {
                !cheatsSupported -> Text(
                    "This emulator doesn't support cheat codes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                )
                else -> {
                    if (cheats.isEmpty()) {
                        Text(
                            "No codes yet. Add Game Genie, Action Replay or GameShark codes for this game.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        SegmentedGroup(null, cheats.size) {
                            cheats.forEachIndexed { i, cheat ->
                                SegmentedListItem(
                                    onClick = { updateCheats(cheats.toMutableList().also { it[i] = cheat.copy(enabled = !cheat.enabled) }) },
                                    onLongClick = { deleting = cheat },
                                    shapes = shapes(i),
                                    colors = segmentColors(),
                                    supportingContent = { Text(cheat.code, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    trailingContent = {
                                        MonoSwitch(cheat.enabled, { on -> updateCheats(cheats.toMutableList().also { it[i] = cheat.copy(enabled = on) }) })
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) { Text(cheat.name) }
                            }
                        }
                        Text(
                            "Hold a code to delete it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { adding = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add code")
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
                }
    }

    @Composable
    private fun ShareMomentPage(back: () -> Unit) {
        var title by remember { mutableStateOf(game.title) }
        var note by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader("Share moment", back)
            Text(
                "Exports your current save state + screenshot as a tiny .rhclip file (no ROM included) so a friend can jump right into this exact moment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Moment or challenge title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(240) },
                label = { Text("Note for your friend (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { shareMoment(title, note) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create & share .rhclip") }
        }
    }
}
