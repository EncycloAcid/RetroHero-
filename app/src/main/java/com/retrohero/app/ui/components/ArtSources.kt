package com.retrohero.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.Game

/** App storage, so artwork can include your own covers and screenshots. Null in previews/tests. */
val LocalArtPaths = staticCompositionLocalOf<AppPaths?> { null }

/** Bumped whenever a cover is set or cleared, so artwork reloads. */
val LocalArtVersion = compositionLocalOf { 0 }

/**
 * Where a game's picture can come from, best first: a cover you chose, then online box art,
 * title screen and snapshot, then your newest screenshot.
 */
fun artSources(game: Game, paths: AppPaths?): List<Any> {
    if (paths == null) return game.artUrls
    val cover = paths.coverFile(game).takeIf { it.exists() }
    val shot = paths.screenshotsOf(game).firstOrNull()
    return listOfNotNull(cover) + game.artUrls + listOfNotNull(shot)
}

@Composable
fun rememberArtSources(game: Game): List<Any> {
    val paths = LocalArtPaths.current
    val version = LocalArtVersion.current
    return remember(game.id, paths, version) { artSources(game, paths) }
}
