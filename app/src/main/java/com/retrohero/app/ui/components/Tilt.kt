package com.retrohero.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

/**
 * How far the phone is tilted from where you were holding it, in degrees (x = left/right,
 * y = toward/away), smoothed. Re-centres slowly, so it reacts to movement rather than to how
 * you happen to hold the phone. Returns zero when [enabled] is false or there's no sensor.
 */
@Composable
fun rememberDeviceTilt(enabled: Boolean, maxDegrees: Float = 14f): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sm == null || sensor == null) {
            tilt.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        var baseX = Float.NaN
        var baseY = Float.NaN
        var sx = 0f
        var sy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val gx = e.values[0] / SensorManager.GRAVITY_EARTH
                val gy = e.values[1] / SensorManager.GRAVITY_EARTH
                if (baseX.isNaN()) { baseX = gx; baseY = gy }
                // Drift the neutral point toward the current hold over a few seconds.
                baseX += (gx - baseX) * 0.01f
                baseY += (gy - baseY) * 0.01f
                val tx = ((gx - baseX) * 60f).coerceIn(-maxDegrees, maxDegrees)
                val ty = ((gy - baseY) * 60f).coerceIn(-maxDegrees, maxDegrees)
                sx += (tx - sx) * 0.2f
                sy += (ty - sy) * 0.2f
                tilt.value = Offset(-sx, sy)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return tilt
}
