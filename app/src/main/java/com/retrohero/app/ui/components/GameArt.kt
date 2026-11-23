package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.retrohero.app.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.retrohero.app.data.Game

/**
 * Box art from libretro-thumbnails, falling back to an illustrated jacket when no artwork loads.
 * [showTitle] prints the game title on generated covers (off where the title is shown nearby).
 */
@Composable
fun GameArt(game: Game, modifier: Modifier = Modifier, showTitle: Boolean = false, showTag: Boolean = true) {
    // Walk the artwork sources (box art -> title screen -> snapshot) until one loads.
    val urls = rememberArtSources(game)
    var attempt by remember(urls) { mutableIntStateOf(0) }
    var loaded by remember(urls) { mutableStateOf(false) }
    Box(modifier) {
        // The generated cover shows while artwork loads and stays if none is found.
        if (!loaded) GeneratedCover(game, showTitle, showTag)
        val url = urls.getOrNull(attempt)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { attempt++ },
                onSuccess = { loaded = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val jackets = intArrayOf(
    R.drawable.archive_ascent, R.drawable.archive_summit, R.drawable.archive_orbit,
    R.drawable.archive_gate, R.drawable.archive_flight, R.drawable.archive_grove,
)

@Composable
private fun GeneratedCover(game: Game, showTitle: Boolean, showTag: Boolean) {
    val jacket = jackets[game.title.hashCode().mod(jackets.size)]
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(painterResource(jacket), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (showTag && maxWidth >= 72.dp) {
            Text(
                game.system?.shortName?.uppercase().orEmpty(),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(6.dp).background(Color.Black.copy(alpha = 0.78f)).padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        if (showTitle) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))))
            Text(game.title, color = Color.White, style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
