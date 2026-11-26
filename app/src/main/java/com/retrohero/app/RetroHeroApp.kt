package com.retrohero.app

import android.app.Application
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.BiosManager
import com.retrohero.app.data.CoreManager
import com.retrohero.app.data.GameLibrary
import com.retrohero.app.data.RomCache
import com.retrohero.app.data.RomPatchStore
import com.retrohero.app.data.Settings

class RetroHeroApp : Application() {
    lateinit var paths: AppPaths private set
    lateinit var settings: Settings private set
    val cores by lazy { CoreManager(this, paths) }
    val library by lazy { GameLibrary(this, paths) }
    val romCache by lazy { RomCache(this, paths) }
    val bios by lazy { BiosManager(this, paths) }
    val patches by lazy { RomPatchStore(paths) }

    override fun onCreate() {
        super.onCreate()
        paths = AppPaths(this)
        settings = Settings(this)
    }
}
