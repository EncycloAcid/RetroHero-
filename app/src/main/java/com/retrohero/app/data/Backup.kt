package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backs up your progress (saves, save states, screenshots, covers, cheats, play time,
 * emulator options, control layouts) and settings to one zip, and restores it. Games and
 * downloaded emulators are never included; BIOS files only when asked.
 */
class Backup(private val filesDir: File, private val settings: Settings) {

    /** What a restore did, for showing to the person. */
    data class Result(val files: Int, val favorites: Int, val collections: Int, val unmatched: Int)

    class BadBackupException(message: String) : Exception(message)

    private val dirs = listOf("saves", "states", "screenshots", "covers", "cheats", "playtime", "core_options", "looks")
    private val files = listOf("pad_layout.json", "remap.json")
    private val gamePrefs get() = File(filesDir.parentFile, "shared_prefs/game_prefs.xml")

    fun write(out: OutputStream, games: List<Game>, includeBios: Boolean, appVersion: String) {
        ZipOutputStream(out.buffered()).use { zip ->
            // The manifest goes first so a restore can check it before touching anything.
            zip.text(MANIFEST, JSONObject().put("format", FORMAT).put("app", appVersion).put("created", System.currentTimeMillis()).toString(1))
            zip.text(SETTINGS, settingsJson(games).toString(1))
            val roots = if (includeBios) dirs + "system" else dirs
            roots.forEach { d -> File(filesDir, d).walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.forEach { zip.file(it, it.relativeTo(filesDir).invariantSeparatorsPath) } }
            files.map { File(filesDir, it) }.filter { it.isFile }.forEach { zip.file(it, it.name) }
            if (gamePrefs.isFile) zip.file(gamePrefs, GAME_PREFS)
        }
    }

    /** Restores [input] over the current data; settings are merged into what's there. */
    fun restore(input: InputStream, games: List<Game>): Result {
        var restored = 0
        var settingsText: String? = null
        ZipInputStream(input.buffered()).use { zip ->
            val first = zip.nextEntry ?: throw BadBackupException("This file is empty.")
            if (first.name != MANIFEST) throw BadBackupException("This isn't a RetroHero backup.")
            val manifest = runCatching { JSONObject(zip.readBytes().decodeToString()) }.getOrNull()
                ?: throw BadBackupException("This backup is damaged.")
            if (manifest.optInt("format", 0) !in 1..FORMAT) throw BadBackupException("This backup is from a newer version of RetroHero.")
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.isDirectory) continue
                val target = targetFor(e.name) ?: continue  // unknown or unsafe: skip it
                if (e.name == SETTINGS) {
                    settingsText = zip.readBytes().decodeToString()
                    continue
                }
                target.parentFile?.mkdirs()
                val tmp = File(target.parentFile, target.name + ".restore")
                tmp.outputStream().use { zip.copyTo(it) }
                if (!tmp.renameTo(target)) {
                    target.delete()
                    tmp.renameTo(target)
                }
                restored++
            }
        }
        val s = settingsText?.let { runCatching { JSONObject(it) }.getOrNull() }
        return if (s != null) applySettings(s, games, restored) else Result(restored, 0, 0, 0)
    }

    /** Where a zip entry may go, or null if it isn't something a backup contains. */
    internal fun targetFor(name: String): File? {
        if (name.isEmpty() || name.startsWith("/") || name.contains('\\') || name.split('/').any { it == ".." || it == "." || it.isEmpty() }) return null
        return when {
            name == SETTINGS -> File(filesDir, SETTINGS)
            name == GAME_PREFS -> gamePrefs
            name in files -> File(filesDir, name)
            (dirs + "system").any { name.startsWith("$it/") } -> File(filesDir, name).takeIf { it.canonicalPath.startsWith(filesDir.canonicalPath + File.separator) }
            else -> null
        }
    }

    private fun settingsJson(games: List<Game>): JSONObject {
        val byId = games.associateBy { it.id }
        fun ref(id: String) = byId[id]?.let { JSONObject().put("system", it.systemId).put("file", it.fileName) }
        return JSONObject()
            .put("prefs", settings.exportPrefs())
            .put("favorites", JSONArray(settings.favorites.value.mapNotNull { ref(it) }))
            .put("recents", JSONArray(settings.recents.value.mapNotNull { ref(it) }))
            .put(
                "collections",
                JSONArray(settings.collections.value.map { c -> JSONObject().put("name", c.name).put("games", JSONArray(c.gameIds.mapNotNull { ref(it) })) }),
            )
    }

    private fun applySettings(o: JSONObject, games: List<Game>, files: Int): Result {
        val byKey = games.associateBy { it.systemId to it.fileName }
        var unmatched = 0
        fun resolve(arr: JSONArray?): List<String> = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
            val r = arr!!.getJSONObject(i)
            byKey[r.optString("system") to r.optString("file")]?.id ?: run { unmatched++; null }
        }
        o.optJSONObject("prefs")?.let { settings.importPrefs(it) }
        val favs = resolve(o.optJSONArray("favorites"))
        settings.addFavorites(favs)
        // Collections merge by name.
        val existing = settings.collections.value.toMutableList()
        val arr = o.optJSONArray("collections") ?: JSONArray()
        var collections = 0
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val name = c.optString("name")
            if (name.isBlank()) continue
            val ids = resolve(c.optJSONArray("games"))
            val at = existing.indexOfFirst { it.name == name }
            if (at >= 0) existing[at] = existing[at].copy(gameIds = (existing[at].gameIds + ids).distinct())
            else existing += GameCollection.new(name).copy(gameIds = ids)
            collections++
        }
        settings.setCollections(existing)
        return Result(files, favs.size, collections, unmatched)
    }

    private fun ZipOutputStream.text(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray())
        closeEntry()
    }

    private fun ZipOutputStream.file(f: File, name: String) {
        putNextEntry(ZipEntry(name).apply { time = f.lastModified() })
        f.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    companion object {
        const val FORMAT = 1
        const val MANIFEST = "manifest.json"
        const val SETTINGS = "settings.json"
        const val GAME_PREFS = "game_prefs.xml"
    }
}
