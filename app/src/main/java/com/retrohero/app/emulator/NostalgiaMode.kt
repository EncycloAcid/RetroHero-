package com.retrohero.app.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** User settings for "Cartridge Blow" & Real-Time Hardware Aging / Nostalgia Mode. */
data class NostalgiaConfig(
    val enabled: Boolean = false,
    val tactileStartup: Boolean = true,
    val crtWarmup: Boolean = true,
    /** 0f = crisp modern display, 1f = strong vintage LCD persistence / CRT phosphor aging. */
    val displayAging: Float = 0.25f,
    val batteryLedDimming: Boolean = true,
)

object NostalgiaMath {

    private val HANDHELD_IDS = setOf("gb", "gbc", "gba", "nds", "3ds", "psp", "gg", "wonderswan", "ngp", "lynx", "pokemini", "vb")
    private val DISC_IDS = setOf("psx", "ps2", "saturn", "dreamcast", "segacd", "gc", "wii", "psp", "3do", "pcfx")

    fun isHandheld(systemId: String): Boolean = systemId.lowercase() in HANDHELD_IDS
    fun isDiscConsole(systemId: String): Boolean = systemId.lowercase() in DISC_IDS
    fun isCrtConsole(systemId: String): Boolean = !isHandheld(systemId)

    /**
     * Authentic handheld/console power LED color based on real battery percentage:
     * - > 30%: healthy default shell LED
     * - 16%..30%: warm amber transition (classic GBA / DS low-power warning)
     * - <= 15%: vintage red flickering/dimming LED (classic Game Boy dying AA batteries!)
     */
    fun batteryLedColor(batteryPercent: Int, defaultLed: Color, pulsePhase: Float = 0f): Color {
        val pct = batteryPercent.coerceIn(0, 100)
        return when {
            pct > 30 -> defaultLed
            pct > 15 -> {
                val t = (30 - pct) / 15f
                lerp(Color(0xFFFFB300), Color(0xFFFF5722), t)
            }
            else -> {
                val flicker = 0.55f + 0.45f * ((sin(pulsePhase * 2f * PI.toFloat()) + 1f) * 0.5f)
                lerp(Color(0xFF4A0A0A), Color(0xFFFF1744), flicker)
            }
        }
    }

    /** Vertical beam expansion curve for CRT warm-up (0 = thin horizontal line, 1 = full screen). */
    fun warmupBeamHeightFraction(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when {
            p < 0.22f -> 0.015f + (p / 0.22f) * 0.035f
            else -> {
                val t = ((p - 0.22f) / 0.78f).coerceIn(0f, 1f)
                // Smooth cubic ease-out bloom
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                0.05f + 0.95f * eased
            }
        }
    }

    /** Warm phosphor bloom alpha peaking right as the CRT tube ignites and fading cleanly to 0. */
    fun warmupGlowAlpha(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when {
            p < 0.25f -> (p / 0.25f) * 0.88f
            else -> ((1f - (p - 0.25f) / 0.75f)).coerceIn(0f, 1f) * 0.88f
        }
    }
}

/**
 * Zero-asset procedural PCM audio synthesizer for tactile cartridge clicks, authentic
 * console-family boot chimes, and the iconic "Cartridge Blow" puff + reseat snap.
 */
object NostalgiaAudioSynth {
    const val SAMPLE_RATE = 22050

    /** Crisp 60ms dual-transient mechanical cartridge-seat snap. */
    fun synthesizeCartridgeClick(sampleRate: Int = SAMPLE_RATE): ShortArray {
        val count = (sampleRate * 0.065f).roundToInt()
        val out = ShortArray(count)
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            // Initial plastic contact click + slightly delayed latch snap at 22ms
            val env1 = exp(-t * 140f)
            val env2 = if (t >= 0.022f) exp(-(t - 0.022f) * 165f) else 0f
            val noise = (rng.nextFloat() * 2f - 1f)
            val body1 = sin(2.0 * PI * 210.0 * t).toFloat() * 0.55f + sin(2.0 * PI * 980.0 * t).toFloat() * 0.45f
            val body2 = sin(2.0 * PI * 320.0 * (t - 0.022f)).toFloat() * 0.6f + noise * 0.4f
            val sample = (body1 * 0.55f + noise * 0.30f) * env1 + body2 * env2 * 0.75f
            out[i] = (sample.coerceIn(-1f, 1f) * 24000f).roundToInt().toShort()
        }
        return out
    }

    /**
     * Original, copyright-safe retro console-family boot chime:
     * - Handhelds (GB/GBA/DS): crisp 2-tone square/bell chime
     * - Disc consoles (PS1/Saturn/DC/PSP): warm shimmering glass chord swell
     * - Cartridge TV consoles (NES/SNES/Genesis/N64): click + warm power chime
     */
    fun synthesizeBootChime(systemId: String, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val click = synthesizeCartridgeClick(sampleRate)
        val chimeDur = if (NostalgiaMath.isDiscConsole(systemId)) 0.62f else 0.42f
        val chimeCount = (sampleRate * chimeDur).roundToInt()
        val gap = (sampleRate * 0.055f).roundToInt()
        val out = ShortArray(click.size + gap + chimeCount)
        click.copyInto(out)

        val offset = click.size + gap
        for (i in 0 until chimeCount) {
            val t = i.toFloat() / sampleRate
            val s = when {
                NostalgiaMath.isHandheld(systemId) -> {
                    // Crisp two-note retro handheld ping (987.8 Hz -> 1975.5 Hz)
                    val freq = if (t < 0.095f) 987.77 else 1975.53
                    val localT = if (t < 0.095f) t else t - 0.095f
                    val env = exp(-localT * 9.5f) * (1f - exp(-localT * 260f))
                    val fundamental = sin(2.0 * PI * freq * t).toFloat()
                    val harmonic = sin(2.0 * PI * freq * 2.0 * t).toFloat() * 0.22f
                    (fundamental + harmonic) * env * 0.52f
                }
                NostalgiaMath.isDiscConsole(systemId) -> {
                    // Lush shimmering warm major-9th glass swell
                    val env = (1f - exp(-t * 14f)) * exp(-t * 4.2f)
                    val c1 = sin(2.0 * PI * 261.63 * t).toFloat()
                    val c2 = sin(2.0 * PI * 329.63 * t).toFloat() * 0.7f
                    val c3 = sin(2.0 * PI * 392.00 * t).toFloat() * 0.65f
                    val c4 = sin(2.0 * PI * 587.33 * t).toFloat() * 0.45f
                    (c1 + c2 + c3 + c4) * 0.25f * env
                }
                else -> {
                    // Warm 16-bit cartridge power-on dyad (523.25 Hz + 783.99 Hz)
                    val env = (1f - exp(-t * 40f)) * exp(-t * 7.5f)
                    val f1 = sin(2.0 * PI * 523.25 * t).toFloat()
                    val f2 = sin(2.0 * PI * 783.99 * t).toFloat() * 0.65f
                    val f3 = sin(2.0 * PI * 1046.5 * t).toFloat() * 0.28f
                    (f1 + f2 + f3) * 0.34f * env
                }
            }
            out[offset + i] = (s.coerceIn(-1f, 1f) * 23000f).roundToInt().toShort()
        }
        return out
    }

    /** Realistic 460ms airy "cartridge blow" puff followed by a crisp reseat click. */
    fun synthesizeCartridgeBlow(sampleRate: Int = SAMPLE_RATE): ShortArray {
        val blowCount = (sampleRate * 0.38f).roundToInt()
        val gap = (sampleRate * 0.05f).roundToInt()
        val click = synthesizeCartridgeClick(sampleRate)
        val out = ShortArray(blowCount + gap + click.size)
        val rng = Random(1337)
        var lp = 0f
        var hp = 0f
        var prev = 0f
        for (i in 0 until blowCount) {
            val t = i.toFloat() / sampleRate
            val env = sin(PI * (t / 0.38f).coerceIn(0f, 1f)).toFloat() * 0.9f
            val white = rng.nextFloat() * 2f - 1f
            // Simple band-pass shaping for warm breath airflow
            lp += 0.18f * (white - lp)
            hp = 0.85f * (hp + lp - prev)
            prev = lp
            out[i] = ((hp * 1.8f * env).coerceIn(-1f, 1f) * 22000f).roundToInt().toShort()
        }
        click.copyInto(out, destinationOffset = blowCount + gap)
        return out
    }

    /** Plays synthesized PCM audio asynchronously without blocking UI or failing in unit tests. */
    fun playAsync(samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        if (samples.isEmpty()) return
        Thread {
            runCatching {
                val minBytes = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ).coerceAtLeast(samples.size * 2)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(minBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                val durationMs = (samples.size * 1000L / sampleRate) + 80L
                Thread.sleep(durationMs)
                track.stop()
                track.release()
            }
        }.start()
    }
}

/**
 * Authentic CRT phosphor warm-up beam bloom (or handheld LCD contrast sweep) overlay played
 * briefly on boot or after a "Blow Cartridge & Reseat".
 */
@Composable
fun NostalgiaWarmupOverlay(
    progress: Float,
    isCrt: Boolean,
    modifier: Modifier = Modifier,
) {
    if (progress >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (isCrt) {
            val beamH = (h * NostalgiaMath.warmupBeamHeightFraction(progress)).coerceIn(2f, h)
            val topMaskH = ((h - beamH) * 0.5f).coerceAtLeast(0f)
            // Unlit CRT glass above & below the expanding scan beam
            if (topMaskH > 0.5f) {
                drawRect(Color.Black.copy(alpha = 0.96f), Offset.Zero, Size(w, topMaskH))
                drawRect(Color.Black.copy(alpha = 0.96f), Offset(0f, h - topMaskH), Size(w, topMaskH))
            }
            val glowAlpha = NostalgiaMath.warmupGlowAlpha(progress)
            if (glowAlpha > 0.01f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color(0xFFB3E5FC).copy(alpha = glowAlpha * 0.55f),
                        0.5f to Color.White.copy(alpha = glowAlpha),
                        0.58f to Color(0xFFFFE0B2).copy(alpha = glowAlpha * 0.45f),
                        1f to Color.Transparent,
                    ),
                    topLeft = Offset(0f, topMaskH),
                    size = Size(w, beamH),
                )
            }
        } else {
            // Classic handheld LCD power-on contrast sweep
            val veil = (1f - progress.coerceIn(0f, 1f))
            drawRect(Color(0xFF1D2418).copy(alpha = veil * 0.88f))
        }
    }
}

/**
 * Subtle real-time vintage display aging overlay:
 * - Handhelds: gentle warm LCD glass reflection & soft edge falloff
 * - TV Consoles: subtle CRT phosphor corner vignette & warm glass tint
 */
@Composable
fun NostalgiaAgingOverlay(
    aging: Float,
    isHandheld: Boolean,
    modifier: Modifier = Modifier,
) {
    val strength = aging.coerceIn(0f, 1f)
    if (strength <= 0.01f) return
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (isHandheld) {
            // Subtle vintage reflective LCD glass sheen + warm olive-amber cast
            drawRect(Color(0xFF8B9A6B).copy(alpha = strength * 0.11f))
            drawRect(
                brush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = strength * 0.08f),
                    0.35f to Color.Transparent,
                    1f to Color.Black.copy(alpha = strength * 0.16f),
                ),
            )
        } else {
            // Warm CRT phosphor edge vignette
            drawRect(
                brush = Brush.radialGradient(
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = strength * 0.34f),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = maxOf(w, h) * 0.72f,
                ),
            )
            drawRect(Color(0xFFFFB74D).copy(alpha = strength * 0.05f))
        }
    }
}

/** Material 3 Expressive configuration sheet for Nostalgia Mode & Cartridge Blow. */
@Composable
fun NostalgiaModeSheet(
    config: NostalgiaConfig,
    systemId: String,
    batteryPercent: Int,
    onUpdate: (NostalgiaConfig) -> Unit,
    onPreviewChime: () -> Unit,
    onBlowCartridge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isHandheld = NostalgiaMath.isHandheld(systemId)
    val previewLed = NostalgiaMath.batteryLedColor(batteryPercent, Color(0xFF00E676), 0.25f)

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
                .widthIn(max = 420.dp)
                .clickable(interactionSource = null, indication = null) {},
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(cs.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Hardware Nostalgia Mode", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Tactile cartridge audio, tube warm-up & vintage display aging",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                    )
                }

                // Blow Cartridge & Reseat fun tactile button!
                Button(
                    onClick = {
                        onBlowCartridge()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Air, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Blow Cartridge & Reseat")
                }

                // Tactile Startup toggle + Preview Chime
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tactile Cartridge Startup", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Subtle cartridge-seat click + console boot chime",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = config.tactileStartup,
                        enabled = config.enabled,
                        onCheckedChange = { onUpdate(config.copy(tactileStartup = it)) },
                    )
                }

                // CRT / LCD Warm-Up toggle
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (isHandheld) "LCD Power-On Sweep" else "CRT Phosphor Warm-Up", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (isHandheld) "Authentic contrast sweep on boot" else "Brief tube beam bloom & warm glow on start",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = config.crtWarmup,
                        enabled = config.enabled,
                        onCheckedChange = { onUpdate(config.copy(crtWarmup = it)) },
                    )
                }

                // Vintage Display Aging Slider
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (isHandheld) "Handheld LCD Aging" else "CRT Glass Vignette",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "${(config.displayAging * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = cs.primary,
                        )
                    }
                    Slider(
                        value = config.displayAging,
                        onValueChange = { onUpdate(config.copy(displayAging = it)) },
                        enabled = config.enabled,
                        valueRange = 0f..1f,
                    )
                }

                // Battery LED Dimming
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
                        Box(Modifier.size(10.dp).clip(CircleShape).background(previewLed))
                        Column {
                            Text("Authentic Battery LED Dimming", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Shell LED shifts amber/red as battery drops (now $batteryPercent%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = config.batteryLedDimming,
                        enabled = config.enabled,
                        onCheckedChange = { onUpdate(config.copy(batteryLedDimming = it)) },
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onPreviewChime, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.VolumeUp, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Test Chime")
                    }
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
