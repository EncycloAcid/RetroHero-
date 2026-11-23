package com.retrohero.app.data

import android.content.Context
import java.io.File

/** All on-disk locations used by RetroHero. Everything lives in app-private storage. */
class AppPaths(context: Context) {
    /** Games downloaded inside the app. Visible to file managers under Android/data. */
    val downloads: File = (context.getExternalFilesDir("Games") ?: File(context.filesDir, "Games")).apply { mkdirs() }
    val downloadTmp = File(context.cacheDir, "downloads").apply { mkdirs() }
    val cores = File(context.filesDir, "cores").apply { mkdirs() }
    val system = File(context.filesDir, "system").apply { mkdirs() }
    val saves = File(context.filesDir, "saves").apply { mkdirs() }
    val states = File(context.filesDir, "states").apply { mkdirs() }
    val coreOptions = File(context.filesDir, "core_options").apply { mkdirs() }
    val romCache = File(context.cacheDir, "roms").apply { mkdirs() }
    val library = File(context.filesDir, "library.json")
    val screenshots = File(context.filesDir, "screenshots")
    val covers = File(context.filesDir, "covers")
    val cheats = File(context.filesDir, "cheats")
    val playtime = File(context.filesDir, "playtime")
    /** Imported look files. */
    val looks = File(context.filesDir, "looks")
    /** Imported .deltaskin bundles (extracted one per subfolder). */
    val deltaSkins = File(context.filesDir, "delta_skins")
    /** Controller remaps; written by the main app, read when a game starts. */
    val remapFile = File(context.filesDir, "remap.json")
    /** Touch-control positions; written in-game. */
    val padLayoutFile = File(context.filesDir, "pad_layout.json")

    fun saveRam(game: Game): File =
        File(File(saves, game.systemId).apply { mkdirs() }, "${game.baseName}.srm")

    fun stateFile(game: Game, slot: Int): File {
        val name = if (slot == AUTO_SLOT) "${game.baseName}.auto.state" else "${game.baseName}.state$slot"
        return File(File(states, game.systemId).apply { mkdirs() }, name)
    }

    fun screenshotsDir(game: Game): File = File(File(screenshots, game.systemId), game.baseName)

    /** Your screenshots for [game], newest first. */
    fun screenshotsOf(game: Game): List<File> =
        screenshotsDir(game).listFiles { f -> f.extension == "png" }?.sortedByDescending { it.lastModified() }.orEmpty()

    fun coverFile(game: Game): File = File(File(covers, game.systemId), "${game.baseName}.png")
    fun cheatsFile(game: Game): File = File(File(cheats, game.systemId), "${game.baseName}.json")
    fun playtimeFile(game: Game): File = File(File(playtime, game.systemId), "${game.baseName}.json")

    fun coreOptionsFile(coreId: String): File = File(coreOptions, "$coreId.opt")

    companion object {
        const val AUTO_SLOT = -1
    }
}
