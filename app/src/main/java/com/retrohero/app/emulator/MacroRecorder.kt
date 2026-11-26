package com.retrohero.app.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.RetroPad
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

/** Single button-mask transition recorded during a Grind Helper macro. */
data class MacroStep(
    val ms: Long,
    val buttonsMask: Int,
)

/** Saved or built-in loopable button macro for RPG grinding & repetitive sequences. */
data class RecordedMacro(
    val id: String,
    val name: String,
    val durationMs: Long,
    val steps: List<MacroStep>,
    val fastForward: Boolean = true,
    val builtIn: Boolean = false,
)

enum class MacroPhase { IDLE, RECORDING, PLAYING }

object MacroEngine {
    const val MIN_DURATION_MS = 500L
    const val MAX_DURATION_MS = 10_000L
    const val MAX_SAVED_MACROS = 12

    /** Returns the active RetroPad bitmask at [elapsedMs], wrapping cleanly for infinite loops. */
    fun sampleMaskAt(macro: RecordedMacro, elapsedMs: Long): Int {
        if (macro.steps.isEmpty() || macro.durationMs <= 0L) return 0
        val pos = ((elapsedMs % macro.durationMs) + macro.durationMs) % macro.durationMs
        if (pos < macro.steps.first().ms) return 0
        var lo = 0
        var hi = macro.steps.lastIndex
        var best = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (macro.steps[mid].ms <= pos) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return macro.steps[best].buttonsMask
    }

    /** Returns exact `(retroPadButtonId, pressed)` transitions between [prevMask] and [nextMask]. */
    fun diffButtonChanges(prevMask: Int, nextMask: Int): List<Pair<Int, Boolean>> {
        val diff = prevMask xor nextMask
        if (diff == 0) return emptyList()
        val changes = ArrayList<Pair<Int, Boolean>>(4)
        for (bit in 0..15) {
            val flag = 1 shl bit
            if ((diff and flag) != 0) {
                changes += bit to ((nextMask and flag) != 0)
            }
        }
        return changes
    }

    /** Compact human-readable badge summarizing buttons used and duration (e.g. "◄ ► A · 3.6s"). */
    fun summarize(macro: RecordedMacro): String {
        var union = 0
        for (s in macro.steps) union = union or s.buttonsMask
        val labels = buildList {
            if ((union and (1 shl RetroPad.LEFT)) != 0) add("◄")
            if ((union and (1 shl RetroPad.RIGHT)) != 0) add("►")
            if ((union and (1 shl RetroPad.UP)) != 0) add("▲")
            if ((union and (1 shl RetroPad.DOWN)) != 0) add("▼")
            if ((union and (1 shl RetroPad.A)) != 0) add("A")
            if ((union and (1 shl RetroPad.B)) != 0) add("B")
            if ((union and (1 shl RetroPad.X)) != 0) add("X")
            if ((union and (1 shl RetroPad.Y)) != 0) add("Y")
            if ((union and (1 shl RetroPad.L)) != 0) add("L")
            if ((union and (1 shl RetroPad.R)) != 0) add("R")
        }
        val btns = if (labels.isEmpty()) "No inputs" else labels.joinToString(" ")
        val secs = String.format(Locale.US, "%.1fs", macro.durationMs / 1000f)
        return "$btns · $secs"
    }

    /** Ready-to-use RPG grind presets so players can loop immediately even before recording. */
    fun builtInPresets(): List<RecordedMacro> {
        val left = 1 shl RetroPad.LEFT
        val right = 1 shl RetroPad.RIGHT
        val a = 1 shl RetroPad.A
        return listOf(
            RecordedMacro(
                id = "preset_grass_grind",
                name = "Grass Grind (◄ ► + Mash A)",
                durationMs = 2_400L,
                steps = listOf(
                    MacroStep(0L, left or a),
                    MacroStep(250L, left),
                    MacroStep(500L, left or a),
                    MacroStep(750L, left),
                    MacroStep(1_000L, a),
                    MacroStep(1_200L, right or a),
                    MacroStep(1_450L, right),
                    MacroStep(1_700L, right or a),
                    MacroStep(1_950L, right),
                    MacroStep(2_200L, 0),
                ),
                fastForward = true,
                builtIn = true,
            ),
            RecordedMacro(
                id = "preset_turbo_a",
                name = "Turbo Mash A (Battles / Dialogue)",
                durationMs = 600L,
                steps = listOf(
                    MacroStep(0L, a),
                    MacroStep(140L, 0),
                    MacroStep(300L, a),
                    MacroStep(440L, 0),
                ),
                fastForward = true,
                builtIn = true,
            ),
        )
    }

    fun createRecorded(
        name: String,
        durationMs: Long,
        steps: List<MacroStep>,
        fastForward: Boolean,
    ): RecordedMacro = RecordedMacro(
        id = UUID.randomUUID().toString(),
        name = name.ifBlank { "Grind Loop" },
        durationMs = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS),
        steps = steps,
        fastForward = fastForward,
        builtIn = false,
    )
}

/** Per-game JSON persistence for custom recorded Grind Helper macros. */
object MacroStore {
    fun load(file: File): List<RecordedMacro> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val stepsArr = o.optJSONArray("steps") ?: JSONArray()
                    val steps = buildList {
                        for (j in 0 until stepsArr.length()) {
                            val s = stepsArr.getJSONObject(j)
                            add(MacroStep(s.optLong("t", 0L), s.optInt("m", 0)))
                        }
                    }
                    val dur = o.optLong("durationMs", 0L)
                    if (dur >= MacroEngine.MIN_DURATION_MS && steps.isNotEmpty()) {
                        add(
                            RecordedMacro(
                                id = o.optString("id", UUID.randomUUID().toString()),
                                name = o.optString("name", "Grind Loop"),
                                durationMs = dur.coerceAtMost(MacroEngine.MAX_DURATION_MS),
                                steps = steps,
                                fastForward = o.optBoolean("ff", true),
                                builtIn = false,
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(file: File, macros: List<RecordedMacro>) {
        runCatching {
            val custom = macros.filterNot { it.builtIn }.take(MacroEngine.MAX_SAVED_MACROS)
            val arr = JSONArray()
            for (m in custom) {
                val stepsArr = JSONArray()
                for (s in m.steps) {
                    stepsArr.put(JSONObject().put("t", s.ms).put("m", s.buttonsMask))
                }
                arr.put(
                    JSONObject()
                        .put("id", m.id)
                        .put("name", m.name)
                        .put("durationMs", m.durationMs)
                        .put("ff", m.fastForward)
                        .put("steps", stepsArr),
                )
            }
            file.parentFile?.mkdirs()
            file.writeText(arr.toString())
        }
    }
}

/** Compact floating HUD pill shown while recording or looping an Auto-Play / Grind Helper macro. */
@Composable
fun MacroHudPill(
    phase: MacroPhase,
    activeMacroName: String,
    elapsedMs: Long,
    loopCount: Int,
    fastForward: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (phase == MacroPhase.IDLE) return
    val isRec = phase == MacroPhase.RECORDING
    val accent = if (isRec) Color(0xFFFF5252) else Color(0xFF69F0AE)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.80f),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
    ) {
        Column(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    if (isRec) Icons.Rounded.FiberManualRecord else Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
                if (isRec) {
                    Text(
                        text = String.format(Locale.US, "REC %.1fs / 10s", elapsedMs / 1000f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                        ),
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = "$activeMacroName · ×$loopCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 170.dp),
                    )
                    if (fastForward) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFAB40).copy(alpha = 0.25f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "FF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = Color(0xFFFFAB40),
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onStop)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Rounded.Stop, "Stop macro", tint = Color.Black, modifier = Modifier.size(13.dp))
                    Text(
                        text = if (isRec) "Save" else "Stop",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.Black,
                    )
                }
            }
            if (isRec) {
                LinearProgressIndicator(
                    progress = { (elapsedMs.toFloat() / MacroEngine.MAX_DURATION_MS).coerceIn(0f, 1f) },
                    modifier = Modifier.width(190.dp).height(3.dp).clip(CircleShape),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.16f),
                )
            } else {
                Text(
                    "Tap any button or Stop to resume manual control",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

/** Material 3 sheet for recording, launching, and managing per-game Auto-Play / Grind Helper macros. */
@Composable
fun MacroManagerSheet(
    savedMacros: List<RecordedMacro>,
    fastForwardDefault: Boolean,
    onToggleFastForward: (Boolean) -> Unit,
    onStartRecording: () -> Unit,
    onPlayMacro: (RecordedMacro) -> Unit,
    onDeleteMacro: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val presets = MacroEngine.builtInPresets()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = cs.surfaceContainer,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 430.dp)
                .clickable(interactionSource = null, indication = null) {},
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(cs.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Repeat, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Auto-Play / Grind Helper", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Record a 3–10s button loop on the pad · stops automatically on your tap",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                // Pair with Fast-Forward switch
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.FastForward, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Loop with Fast-Forward", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Auto-boost speed while grinding, restore normal speed on stop",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = fastForwardDefault, onCheckedChange = onToggleFastForward)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onStartRecording()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.FiberManualRecord, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Record New Macro (up to 10s)")
                }

                if (savedMacros.isNotEmpty()) {
                    Text(
                        "Saved for This Game",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.primary,
                    )
                    savedMacros.forEach { macro ->
                        MacroRowCard(
                            macro = macro,
                            onPlay = {
                                onDismiss()
                                onPlayMacro(macro)
                            },
                            onDelete = { onDeleteMacro(macro.id) },
                        )
                    }
                }

                Text(
                    "Instant RPG Starter Loops",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurfaceVariant,
                )
                presets.forEach { preset ->
                    MacroRowCard(
                        macro = preset,
                        onPlay = {
                            onDismiss()
                            onPlayMacro(preset)
                        },
                        onDelete = null,
                    )
                }

                FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun MacroRowCard(
    macro: RecordedMacro,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    macro.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    MacroEngine.summarize(macro),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Delete, "Delete macro", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(cs.primary)
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, "Loop macro", tint = cs.onPrimary, modifier = Modifier.size(15.dp))
                Text(
                    "Loop",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onPrimary,
                )
            }
        }
    }
}
