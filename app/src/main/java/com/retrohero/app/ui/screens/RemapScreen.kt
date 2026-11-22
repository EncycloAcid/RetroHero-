package com.retrohero.app.ui.screens

import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.retrohero.app.data.Remap
import com.retrohero.app.ui.theme.Salmon

/** A controller you can make a profile for. [descriptor] is [Remap.ANY] for every controller. */
data class ControllerProfile(val descriptor: String, val name: String)

/** Lists connected game controllers, updating as they come and go. */
@Composable
fun rememberControllers(): List<ControllerProfile> {
    val context = LocalContext.current
    var list by remember { mutableStateOf(connectedControllers()) }
    DisposableEffect(Unit) {
        val im = context.getSystemService(InputManager::class.java)
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(id: Int) { list = connectedControllers() }
            override fun onInputDeviceRemoved(id: Int) { list = connectedControllers() }
            override fun onInputDeviceChanged(id: Int) { list = connectedControllers() }
        }
        im?.registerInputDeviceListener(listener, null)
        onDispose { im?.unregisterInputDeviceListener(listener) }
    }
    return list
}

private fun connectedControllers(): List<ControllerProfile> =
    InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }
        .filter { !it.isVirtual && (it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || it.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) }
        .distinctBy { it.descriptor }
        .map { ControllerProfile(it.descriptor, it.name) }

/**
 * Rebind controller buttons. Pick "All controllers" or one you have connected, tap an action,
 * then press the button you want for it.
 */
@Composable
fun RemapScreen(
    profiles: Map<String, Map<Int, Int>>,
    controllers: List<ControllerProfile>,
    onChange: (Map<String, Map<Int, Int>>) -> Unit,
    onBack: () -> Unit,
) {
    val known = profiles.keys.filter { it != Remap.ANY }.map { d -> controllers.firstOrNull { it.descriptor == d } ?: ControllerProfile(d, "Saved controller") }
    val choices = listOf(ControllerProfile(Remap.ANY, "All controllers")) + (controllers + known).distinctBy { it.descriptor }
    var profile by remember { mutableStateOf(Remap.ANY) }
    var capturing by remember { mutableStateOf<Pair<Int, String>?>(null) }

    Column(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Controller buttons", style = MaterialTheme.typography.titleLargeEmphasized, color = Color.White, modifier = Modifier.weight(1f))
            if (profiles[profile].orEmpty().isNotEmpty()) {
                TextButton(onClick = { onChange(profiles - profile) }) { Text("Reset") }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices, key = { it.descriptor }) { c ->
                val sel = c.descriptor == profile
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (sel) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f))
                        .clickable { profile = c.descriptor }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (c.descriptor != Remap.ANY) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = if (sel) Color(0xFF1A1A1A) else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(c.name, style = MaterialTheme.typography.labelLarge, color = if (sel) Color(0xFF1A1A1A) else Color.White, maxLines = 1)
                }
            }
        }
        Text(
            if (controllers.isEmpty()) "No controller connected. Changes to “All controllers” apply to any you connect later."
            else "Tap an action, then press the button you want for it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(Remap.actions, key = { it.first }) { (action, label) ->
                val keys = Remap.keysFor(profiles, profile, action)
                val changed = profiles[profile].orEmpty().any { it.value == action }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { capturing = action to label }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Text(
                        keys.joinToString(", ") { Remap.keyName(it) }.ifEmpty { "Not set" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (changed) Salmon else Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp).width(150.dp),
                    )
                }
            }
        }
    }

    capturing?.let { (action, label) ->
        CaptureDialog(
            label = label,
            onKey = { keyCode ->
                val current = profiles[profile].orEmpty().toMutableMap()
                current[keyCode] = action
                onChange(profiles + (profile to current))
                capturing = null
            },
            onClear = {
                // Stop every key that does this from doing it, for this profile.
                val current = profiles[profile].orEmpty().toMutableMap()
                Remap.keysFor(profiles, profile, action).forEach { current[it] = Remap.NONE }
                onChange(profiles + (profile to current))
                capturing = null
            },
            onDismiss = { capturing = null },
        )
    }
}

@Composable
private fun CaptureDialog(label: String, onKey: (Int) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161618))
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    val native = e.nativeKeyEvent
                    if (native.keyCode == KeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                    if (native.action == KeyEvent.ACTION_UP) onKey(native.keyCode)
                    true
                }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(40.dp))
            Text("Press a button for", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 14.dp))
            Text(label, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) { Text("Turn off") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}
