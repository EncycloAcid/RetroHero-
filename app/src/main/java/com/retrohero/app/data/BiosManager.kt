package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

class BiosManager(private val context: Context, private val paths: AppPaths) {

    fun isPresent(file: BiosFile): Boolean = File(paths.system, file.fileName).exists()

    fun installedFiles(): List<File> = paths.system.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    /** Copies the picked files into the system directory. Returns the imported file names. */
    fun import(uris: List<Uri>): List<String> = uris.mapNotNull { uri ->
        val name = displayName(uri) ?: return@mapNotNull null
        val safe = name.replace(Regex("[\\\\/]"), "_")
        val dest = File(paths.system, safe)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: return@mapNotNull null
        // FinalBurn Neo also looks for BIOS romsets (e.g. neogeo.zip) in system/fbneo.
        if (safe.endsWith(".zip", ignoreCase = true)) {
            val fbneo = File(paths.system, "fbneo").apply { mkdirs() }
            dest.copyTo(File(fbneo, safe), overwrite = true)
        }
        safe
    }

    fun delete(name: String) {
        File(paths.system, name).delete()
        File(File(paths.system, "fbneo"), name).delete()
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
}
