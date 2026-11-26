package com.retrohero.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Seamless high-speed standalone emulator bridge for demanding modern consoles
 * (PS2 via NetherSX2/AetherSX2, Switch via Eden/Citron/Sudachi/Yuzu, PS Vita via Vita3K,
 * Wii U via Cemu, plus optional standalone acceleration for 3DS, GameCube/Wii, PSP & Dreamcast).
 */
data class ExternalEmulator(
    val id: String,
    val name: String,
    val packageName: String,
    val activityName: String? = null,
    val systemIds: Set<String>,
    val extraRomKey: String? = null,
    val badge: String = "Standalone · Full Speed",
)

object ExternalEmulators {

    val all: List<ExternalEmulator> = listOf(
        // PlayStation 2 — NetherSX2 / AetherSX2 (gold standard 60 FPS Android PS2)
        ExternalEmulator(
            id = "nethersx2",
            name = "NetherSX2 / AetherSX2",
            packageName = "xyz.aethersx2.android",
            activityName = "xyz.aethersx2.android.EmulationActivity",
            systemIds = setOf("ps2"),
            extraRomKey = "bootPath",
            badge = "Best for PS2 · 60 FPS",
        ),
        // Nintendo Switch
        ExternalEmulator(
            id = "eden",
            name = "Eden (Switch)",
            packageName = "dev.eden.eden_emulator",
            activityName = "org.yuzu.yuzu_emu.activities.EmulationActivity",
            systemIds = setOf("switch"),
            badge = "Switch · ARM64",
        ),
        ExternalEmulator(
            id = "citron",
            name = "Citron (Switch)",
            packageName = "org.citron.citron_emu",
            activityName = "org.citron.citron_emu.activities.EmulationActivity",
            systemIds = setOf("switch"),
            badge = "Switch · ARM64",
        ),
        ExternalEmulator(
            id = "sudachi",
            name = "Sudachi (Switch)",
            packageName = "org.sudachi.sudachi_emu",
            activityName = "org.sudachi.sudachi_emu.activities.EmulationActivity",
            systemIds = setOf("switch"),
            badge = "Switch · ARM64",
        ),
        ExternalEmulator(
            id = "yuzu",
            name = "Yuzu (Switch)",
            packageName = "org.yuzu.yuzu_emu",
            activityName = "org.yuzu.yuzu_emu.activities.EmulationActivity",
            systemIds = setOf("switch"),
            badge = "Switch · ARM64",
        ),
        // Sony PlayStation Vita
        ExternalEmulator(
            id = "vita3k",
            name = "Vita3K",
            packageName = "org.vita3k.emulator",
            systemIds = setOf("psvita"),
            badge = "PS Vita · ARM64",
        ),
        // Nintendo Wii U
        ExternalEmulator(
            id = "cemu",
            name = "Cemu Android",
            packageName = "info.cemu.cemu",
            systemIds = setOf("wiiu"),
            badge = "Wii U · ARM64",
        ),
        // Nintendo 3DS
        ExternalEmulator(
            id = "azahar_app",
            name = "Azahar Standalone",
            packageName = "io.github.lime3ds.android",
            activityName = "org.citra.citra_emu.activities.EmulationActivity",
            systemIds = setOf("3ds"),
            extraRomKey = "SelectedGame",
            badge = "Fast 3DS · Vulkan/GLES",
        ),
        ExternalEmulator(
            id = "citra_app",
            name = "Citra Standalone",
            packageName = "org.citra.citra_emu",
            activityName = "org.citra.citra_emu.activities.EmulationActivity",
            systemIds = setOf("3ds"),
            extraRomKey = "SelectedGame",
            badge = "Fast 3DS",
        ),
        // Nintendo GameCube / Wii
        ExternalEmulator(
            id = "dolphin_app",
            name = "Dolphin Standalone",
            packageName = "org.dolphinemu.dolphinemu",
            activityName = "org.dolphinemu.dolphinemu.ui.main.MainActivity",
            systemIds = setOf("gc", "wii"),
            extraRomKey = "AutoStartFile",
            badge = "Fast GC/Wii · Vulkan",
        ),
        // Sony PSP
        ExternalEmulator(
            id = "ppsspp_gold",
            name = "PPSSPP Gold",
            packageName = "org.ppsspp.ppssppgold",
            activityName = "org.ppsspp.ppsspp.PpssppActivity",
            systemIds = setOf("psp"),
            badge = "Fast PSP · Vulkan",
        ),
        ExternalEmulator(
            id = "ppsspp_app",
            name = "PPSSPP Standalone",
            packageName = "org.ppsspp.ppsspp",
            activityName = "org.ppsspp.ppsspp.PpssppActivity",
            systemIds = setOf("psp"),
            badge = "Fast PSP · Vulkan",
        ),
        // Sega Dreamcast
        ExternalEmulator(
            id = "redream",
            name = "Redream",
            packageName = "io.recompiled.redream",
            activityName = "io.recompiled.redream.MainActivity",
            systemIds = setOf("dreamcast"),
            badge = "Fast Dreamcast",
        ),
    )

    fun forSystem(systemId: String): List<ExternalEmulator> =
        all.filter { systemId in it.systemIds }

    fun installedForSystem(context: Context, systemId: String): List<ExternalEmulator> =
        forSystem(systemId).filter { isInstalled(context, it.packageName) }

    fun isInstalled(context: Context, packageName: String): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        true
    }.getOrDefault(false)

    /** Builds an Android launch Intent that hands the ROM URI cleanly to the standalone emulator. */
    fun launchIntent(context: Context, emulator: ExternalEmulator, game: Game): Intent? {
        if (!isInstalled(context, emulator.packageName)) return null
        val uri = if (game.isLocal) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                java.io.File(game.id),
            )
        } else {
            Uri.parse(game.id)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (emulator.activityName != null) {
                component = ComponentName(emulator.packageName, emulator.activityName)
            } else {
                setPackage(emulator.packageName)
            }
            if (emulator.extraRomKey != null) {
                putExtra(emulator.extraRomKey, uri.toString())
            }
        }
        val canResolve = context.packageManager.resolveActivity(intent, 0) != null
        return if (canResolve) intent else context.packageManager.getLaunchIntentForPackage(emulator.packageName)
    }
}
