package com.retrohero.app.emulator

import android.annotation.SuppressLint
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

    private var phase by mutableStateOf<Phase>(Phase.Loading)
    private var menuOpen by mutableStateOf(false)
    private var showPad by mutableStateOf(true)
    private var fastForwardToggle by mutableStateOf(false)
    private var aspect by mutableFloatStateOf(4f / 3f)

    private var started = false
    private var shutDown = false
    private var fastForwardHeld = false

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
        game = Game.fromJson(JSONObject(gameJson))
        system = game.system ?: run { finish(); return }
        paths = AppPaths(this)
        settings = Settings(this)
        input = InputMapper(settings.stickAsDpad)
        optionsStore = CoreOptionsStore(paths.coreOptionsFile(coreId))

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (phase is Phase.Running) setMenu(!menuOpen) else finish()
            }
        })

        setContent { RetroHeroTheme(dynamicColor = settings.dynamicColor.value) { EmulatorScreen() } }

        lifecycleScope.launch { boot(corePath, romPath) }
    }

    private suspend fun boot(corePath: String, romPath: String) {
        val error = withContext(Dispatchers.IO) {
            optionsStore.load().forEach { (k, v) -> NativeBridge.setOption(k, v) }
            if (!NativeBridge.loadCore(corePath, paths.system.absolutePath, paths.saves.absolutePath)) {
                return@withContext NativeBridge.getLastError()
            }
            if (!NativeBridge.loadGame(romPath, paths.saveRam(game).absolutePath)) {
                return@withContext NativeBridge.getLastError()
            }
            val requested = if (intent.hasExtra(EXTRA_LOAD_SLOT)) intent.getIntExtra(EXTRA_LOAD_SLOT, AppPaths.AUTO_SLOT) else null
            val resume = when {
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
        NativeBridge.setSharpScale(if (settings.sharpPixels) 3 else 1)
        aspect = NativeBridge.getAspectRatio()
        NativeBridge.start()
        started = true
        phase = Phase.Running
    }

    private fun setMenu(open: Boolean) {
        menuOpen = open
        if (started) NativeBridge.setPaused(open)
    }

    private fun applyFastForward() {
        NativeBridge.setFastForward(fastForwardToggle || fastForwardHeld, settings.fastForwardSpeed)
    }

    override fun onPause() {
        super.onPause()
        if (started && !shutDown) {
            NativeBridge.setPaused(true)
            NativeBridge.saveSram(paths.saveRam(game).absolutePath)
        }
    }

    override fun onResume() {
        super.onResume()
        if (started && !shutDown && !menuOpen) NativeBridge.setPaused(false)
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
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE && event.action == KeyEvent.ACTION_UP && phase is Phase.Running) {
            setMenu(!menuOpen)
            return true
        }
        if (!menuOpen && phase is Phase.Running && event.keyCode != KeyEvent.KEYCODE_BACK) {
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

    // ---- UI ---------------------------------------------------------------------------------

    @Composable
    private fun EmulatorScreen() {
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        LaunchedEffect(phase) {
            while (phase is Phase.Running) {
                aspect = NativeBridge.getAspectRatio().takeIf { it > 0.1f } ?: aspect
                delay(500)
            }
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (landscape) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GameView() }
                if (showPad && phase is Phase.Running) Pad(landscape = true, Modifier.fillMaxSize().safeDrawingPadding())
            } else {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { GameView() }
                    if (showPad && phase is Phase.Running) Pad(landscape = false, Modifier.fillMaxWidth().fillMaxHeight(0.45f))
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
        }
    }

    @Composable
    private fun Pad(landscape: Boolean, modifier: Modifier) {
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
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    private fun GameView() {
        AndroidView(
            modifier = Modifier.aspectRatio(aspect),
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
                ActionTile(Icons.Default.Tune, "Options", Modifier.weight(1f)) { navigate("options") }
                ActionTile(Icons.Default.RestartAlt, "Reset", Modifier.weight(1f)) {
                    NativeBridge.reset()
                    setMenu(false)
                }
            }
            SegmentedGroup(null, 2) {
                MenuSwitch(shapes(0), Icons.Default.FastForward, "Fast forward", "${settings.fastForwardSpeed.toInt()}× speed", fastForwardToggle) {
                    fastForwardToggle = it
                    applyFastForward()
                }
                MenuSwitch(shapes(1), Icons.Default.Gamepad, "Touch controls", "Show the on-screen gamepad", showPad) { showPad = it }
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

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val EXTRA_GAME = "game"
        private const val EXTRA_CORE_PATH = "core_path"
        private const val EXTRA_CORE_ID = "core_id"
        private const val EXTRA_ROM_PATH = "rom_path"

        private const val EXTRA_LOAD_SLOT = "load_slot"

        /** [slot]: a save-state slot to resume from (AppPaths.AUTO_SLOT or 1..4); null uses the usual auto-resume. */
        fun intent(context: Context, game: Game, coreId: String, corePath: String, romPath: String, slot: Int? = null) =
            Intent(context, EmulatorActivity::class.java)
                .putExtra(EXTRA_GAME, game.toJson().toString())
                .putExtra(EXTRA_CORE_ID, coreId)
                .putExtra(EXTRA_CORE_PATH, corePath)
                .putExtra(EXTRA_ROM_PATH, romPath)
                .apply { if (slot != null) putExtra(EXTRA_LOAD_SLOT, slot) }
    }
}
