package com.retrohero.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.retrohero.app.data.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val artColorCache = ConcurrentHashMap<String, Int>()

/**
 * A colour taken from the game's box art (vibrant, else dominant swatch), darkened if it's too
 * light to sit behind white text. Generated covers use the system's colour.
 */
@Composable
fun rememberArtColor(game: Game?): Color {
    val fallback = game?.system?.color?.let { lerp(it, Color.Black, 0.35f) } ?: Color(0xFF2A2A2E)
    val context = LocalContext.current
    var color by remember(game?.id) { mutableStateOf(artColorCache[game?.id]?.let { Color(it) } ?: fallback) }
    LaunchedEffect(game?.id) {
        val g = game ?: return@LaunchedEffect
        if (artColorCache.containsKey(g.id)) return@LaunchedEffect
        // First artwork source that actually loads.
        val result = g.artUrls.firstNotNullOfOrNull { url ->
            runCatching {
                context.imageLoader.execute(
                    ImageRequest.Builder(context).data(url).allowHardware(false).size(160).build(),
                )
            }.getOrNull() as? SuccessResult
        } ?: return@LaunchedEffect
        val bitmap: Bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val picked = withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            (palette.vibrantSwatch ?: palette.darkVibrantSwatch ?: palette.dominantSwatch)?.rgb
        } ?: return@LaunchedEffect
        var c = Color(picked)
        if (c.luminance() > 0.45f) c = lerp(c, Color.Black, 0.45f)
        artColorCache[g.id] = android.graphics.Color.argb(255, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
        color = c
    }
    return color
}
