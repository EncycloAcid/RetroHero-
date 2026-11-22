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

    fun saveRam(game: Game): File =
        File(File(saves, game.systemId).apply { mkdirs() }, "${game.baseName}.srm")

    fun stateFile(game: Game, slot: Int): File {
        val name = if (slot == AUTO_SLOT) "${game.baseName}.auto.state" else "${game.baseName}.state$slot"
        return File(File(states, game.systemId).apply { mkdirs() }, name)
    }

    fun coreOptionsFile(coreId: String): File = File(coreOptions, "$coreId.opt")

    companion object {
        const val AUTO_SLOT = -1
    }
}
