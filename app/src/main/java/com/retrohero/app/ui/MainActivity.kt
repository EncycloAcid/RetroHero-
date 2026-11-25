package com.retrohero.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.retrohero.app.data.DeltaSkins
import com.retrohero.app.ui.theme.RetroHeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) {
            DeltaSkins.installBundledDefaults(this@MainActivity, vm.paths.deltaSkins, vm.settings)
        }
        if (savedInstanceState == null) handleIncomingDeltaSkin(intent)
        setContent {
            RetroHeroTheme {
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = false
                    }
                }
                BlankCanvas()
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

/** Clean blank slate ready for a brand-new UI from scratch. */
@Composable
fun BlankCanvas() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    )
}
