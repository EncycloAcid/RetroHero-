package com.retrohero.app.emulator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.ui.geometry.Offset
import com.retrohero.app.data.PadButton
import com.retrohero.app.data.PadLayout
import com.retrohero.app.data.RetroPad
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sign

/** A 1-thumb simultaneous multi-button combo macro (e.g., Dash + Jump, Run + Jump, Drift). */
data class ComboMacro(
    val id: String,
    val label: String,
    val subtitle: String,
    val buttonIds: List<Int>,
)

/** Per-session / persisted Smart Touch Ergonomics configuration. */
data class SmartTouchConfig(
    /** Slide your thumb across the bridge between adjacent face buttons (B→A, Y→B) to hold both cleanly. */
    val thumbChordZones: Boolean = true,
    /** Tap L/R/L2/R2 once to latch held; tap again to release (zero claw grip!). */
    val shoulderLatch: Boolean = true,
    /** Show a dedicated 1-thumb Combo Macro button beside the face cluster. */
    val comboMacroEnabled: Boolean = true,
    /** Index into [SmartTouch.availableMacrosFor] for the active console. */
    val macroIndex: Int = 0,
    /** Tilt phone to steer analog stick X (plus gentle digital assist for racing classics). */
    val gyroSteering: Boolean = false,
    /** Portrait layout mode: AUTO (one-handed for RPGs), ALWAYS_ONE_HAND, or STANDARD. */
    val portraitMode: Int = SmartTouch.PORTRAIT_AUTO,
    /** Mirrors One-Handed Portrait Mode for left-handed thumb play. */
    val leftHandedPortrait: Boolean = false,
)

/** Pure geometric & ergonomics logic for RetroHero's Smart Touch Controls. */
object SmartTouch {

    const val PORTRAIT_AUTO = 0
    const val PORTRAIT_ONE_HAND = 1
    const val PORTRAIT_STANDARD = 2

    val SHOULDER_IDS = setOf(RetroPad.L, RetroPad.R, RetroPad.L2, RetroPad.R2)

    data class FaceHitCircle(
        val id: Int,
        val center: Offset,
        val radius: Float,
    )

    /**
     * Hit-tests a touch [point] against face button circles. When [chordEnabled] is true,
     * touching the ergonomic bridge between two adjacent face buttons simultaneously presses
     * BOTH buttons so rolling your thumb (e.g. Y→B or B→A) never drops the chord.
     */
    fun resolveFaceChord(
        point: Offset,
        buttons: List<FaceHitCircle>,
        chordEnabled: Boolean,
        slopPx: Float,
    ): Set<Int> {
        val hit = HashSet<Int>()
        for (b in buttons) {
            if (hypot(point.x - b.center.x, point.y - b.center.y) <= b.radius + slopPx) {
                hit += b.id
            }
        }
        if (!chordEnabled || buttons.size < 2) return hit

        // Check saddle/bridge zones between adjacent face button pairs
        for (i in buttons.indices) {
            val a = buttons[i]
            for (j in (i + 1) until buttons.size) {
                val b = buttons[j]
                val dx = b.center.x - a.center.x
                val dy = b.center.y - a.center.y
                val dist = hypot(dx, dy)
                val maxPairDist = (a.radius + b.radius) * 1.55f
                if (dist <= 1f || dist > maxPairDist) continue

                val px = point.x - a.center.x
                val py = point.y - a.center.y
                val t = (px * dx + py * dy) / (dist * dist)
                if (t in 0.16f..0.84f) {
                    val projX = a.center.x + t * dx
                    val projY = a.center.y + t * dy
                    val perpDist = hypot(point.x - projX, point.y - projY)
                    val bridgeHalfWidth = minOf(a.radius, b.radius) * 0.72f
                    if (perpDist <= bridgeHalfWidth) {
                        hit += a.id
                        hit += b.id
                    }
                }
            }
        }
        return hit
    }

    /** Generates console-tailored 1-thumb Combo Macro presets from the console's [PadLayout]. */
    fun availableMacrosFor(layout: PadLayout): List<ComboMacro> {
        val list = mutableListOf<ComboMacro>()
        val diamond = layout.diamond
        val rShoulder = layout.rightShoulders.firstOrNull()
        val lShoulder = layout.leftShoulders.firstOrNull()

        if (diamond != null && diamond.size == 4) {
            // diamond order: [top, left, right, bottom] (e.g. X, Y, A, B or △, □, ○, ✕)
            val top = diamond[0]
            val left = diamond[1]
            val right = diamond[2]
            val bottom = diamond[3]
            list += ComboMacro(
                id = "left_bottom",
                label = "${left.label}+${bottom.label}",
                subtitle = "Run / Dash + Jump",
                buttonIds = listOf(left.id, bottom.id),
            )
            list += ComboMacro(
                id = "bottom_right",
                label = "${bottom.label}+${right.label}",
                subtitle = "Confirm / Action chord",
                buttonIds = listOf(bottom.id, right.id),
            )
            if (rShoulder != null) {
                list += ComboMacro(
                    id = "r_bottom",
                    label = "${rShoulder.label}+${bottom.label}",
                    subtitle = "Drift / Aim + Action",
                    buttonIds = listOf(rShoulder.id, bottom.id),
                )
            }
            if (lShoulder != null && rShoulder != null) {
                list += ComboMacro(
                    id = "l_r",
                    label = "${lShoulder.label}+${rShoulder.label}",
                    subtitle = "Dual shoulder chord",
                    buttonIds = listOf(lShoulder.id, rShoulder.id),
                )
            }
            list += ComboMacro(
                id = "top_right",
                label = "${top.label}+${right.label}",
                subtitle = "Special attack chord",
                buttonIds = listOf(top.id, right.id),
            )
        } else {
            val flat: List<PadButton> = layout.rows.flatten()
            if (flat.size >= 2) {
                val b0 = flat[0]
                val b1 = flat[1]
                list += ComboMacro(
                    id = "face_01",
                    label = "${b0.label}+${b1.label}",
                    subtitle = "Run + Jump chord",
                    buttonIds = listOf(b0.id, b1.id),
                )
                if (rShoulder != null) {
                    list += ComboMacro(
                        id = "r_face1",
                        label = "${rShoulder.label}+${b1.label}",
                        subtitle = "Dash / Drift + Jump",
                        buttonIds = listOf(rShoulder.id, b1.id),
                    )
                }
                if (lShoulder != null && rShoulder != null) {
                    list += ComboMacro(
                        id = "l_r",
                        label = "${lShoulder.label}+${rShoulder.label}",
                        subtitle = "Dual shoulder chord",
                        buttonIds = listOf(lShoulder.id, rShoulder.id),
                    )
                }
            }
        }
        return list
    }

    private val RPG_KEYWORDS = listOf(
        "pokemon", "pokémon", "final fantasy", "chrono", "dragon quest", "dragon warrior",
        "earthbound", "mother", "golden sun", "fire emblem", "advance wars", "tactics",
        "persona", "shin megami", "breath of fire", "phantasy star", "suikoden",
        "paper mario", "mario & luigi", "mario and luigi", "mario rpg", "xenogears",
        "lufia", "shining force", "ogre battle", "wild arms", "grandia", "lunar",
        "role-playing", "rpg", "strategy", "turn-based", "picross", "puzzle",
    )

    /** True when the game is well suited to effortless Portrait One-Handed thumb play. */
    fun isRpgFriendly(gameTitle: String, genreHint: String = ""): Boolean {
        val haystack = "${gameTitle.lowercase()} ${genreHint.lowercase()}"
        return RPG_KEYWORDS.any { it in haystack }
    }

    /** Automatic Portrait One-Handed RPG Mode vs. Landscape Action Mode resolver. */
    fun shouldUseOneHandedPortrait(
        portraitMode: Int,
        landscape: Boolean,
        gameTitle: String,
        genreHint: String = "",
    ): Boolean {
        if (landscape) return false
        return when (portraitMode) {
            PORTRAIT_ONE_HAND -> true
            PORTRAIT_STANDARD -> false
            else -> isRpgFriendly(gameTitle, genreHint)
        }
    }

    /**
     * Converts phone roll tilt (degrees) into a smooth analog steering value in `[-1f, 1f]`
     * with a crisp deadzone and gentle progressive curve.
     */
    fun computeGyroAxis(
        tiltDegrees: Float,
        zeroOffsetDegrees: Float = 0f,
        maxTiltDegrees: Float = 28f,
        deadzoneDegrees: Float = 3.5f,
    ): Float {
        val rel = tiltDegrees - zeroOffsetDegrees
        val mag = abs(rel)
        if (mag <= deadzoneDegrees) return 0f
        val span = (maxTiltDegrees - deadzoneDegrees).coerceAtLeast(1f)
        val linear = ((mag - deadzoneDegrees) / span).coerceIn(0f, 1f)
        val curved = linear.toDouble().pow(1.25).toFloat()
        return sign(rel) * curved
    }
}

/**
 * Hardware accelerometer/gravity tilt steering controller for racing, N64, PS1, and flight games.
 * Outputs smooth analog X (`-1..1`) plus optional digital Left/Right thresholds for classic 2D racers.
 */
class GyroSteeringController(
    context: Context,
    private val displayRotationProvider: () -> Int,
    private val onSteer: (analogX: Float, digitalLeft: Boolean, digitalRight: Boolean) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var active = false
    private var zeroTiltDeg = 0f
    private var needsCenter = true
    private var smoothedTilt = 0f

    val isAvailable: Boolean get() = sensor != null

    fun start() {
        val sm = sensorManager ?: return
        val s = sensor ?: return
        if (active) return
        active = true
        needsCenter = true
        smoothedTilt = 0f
        sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!active) return
        active = false
        sensorManager?.unregisterListener(this)
        onSteer(0f, false, false)
    }

    fun recenter() {
        needsCenter = true
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!active || event.values.size < 3) return
        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]
        // Compute intuitive steering wheel roll angle for the current screen orientation
        val rawTiltDeg = when (displayRotationProvider()) {
            Surface.ROTATION_90 -> Math.toDegrees(atan2(gy.toDouble(), hypot(gx.toDouble(), gz.toDouble()))).toFloat()
            Surface.ROTATION_270 -> Math.toDegrees(atan2(-gy.toDouble(), hypot(gx.toDouble(), gz.toDouble()))).toFloat()
            else -> Math.toDegrees(atan2(-gx.toDouble(), hypot(gy.toDouble(), gz.toDouble()))).toFloat()
        }
        if (needsCenter) {
            zeroTiltDeg = rawTiltDeg
            smoothedTilt = rawTiltDeg
            needsCenter = false
        } else {
            smoothedTilt = smoothedTilt * 0.72f + rawTiltDeg * 0.28f
        }
        val axis = SmartTouch.computeGyroAxis(smoothedTilt, zeroTiltDeg)
        onSteer(axis, axis <= -0.35f, axis >= 0.35f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
