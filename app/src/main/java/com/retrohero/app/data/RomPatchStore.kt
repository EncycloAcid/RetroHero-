package com.retrohero.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * A fan translation or ROM hack patch attached cleanly under a parent [Game].
 *
 * Only the small patch file is stored in app-private storage; the user's original ROM is
 * never duplicated or modified on disk.
 */
data class RomPatch(
    val id: String,
    val name: String,
    val fileName: String,
    val format: PatchFormat,
    val size: Long,
    val addedAt: Long,
    /** When true, this hack/translation uses its own isolated battery save & save-state slots. */
    val separateSave: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("fileName", fileName)
        .put("format", format.name)
        .put("size", size)
        .put("addedAt", addedAt)
        .put("separateSave", separateSave)

    companion object {
        fun fromJson(o: JSONObject): RomPatch? {
            val fmt = runCatching { PatchFormat.valueOf(o.getString("format")) }.getOrNull() ?: return null
            return RomPatch(
                id = o.getString("id"),
                name = o.getString("name"),
                fileName = o.getString("fileName"),
                format = fmt,
                size = o.optLong("size", 0L),
                addedAt = o.optLong("addedAt", 0L),
                separateSave = o.optBoolean("separateSave", true),
            )
        }

        /** Human-readable display title derived from a patch file name. */
        fun prettyName(fileName: String): String =
            fileName
                .substringAfterLast('/')
                .substringBeforeLast('.')
                .replace('_', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifEmpty { "ROM Patch" }

        /**
         * Returns a virtual [Game] identity scoped to [patchId] so battery saves (`.srm`) and
         * save states (`.state1..4` / `.auto.state`) for ROM hacks like *Pokémon Unbound* stay
         * completely isolated from the vanilla parent game's save file.
         */
        fun saveScopedGame(parent: Game, patchId: String): Game {
            val stem = parent.fileName.substringBeforeLast('.')
            val ext = parent.fileName.substringAfterLast('.', "")
            val shortId = patchId.replace(Regex("[^a-zA-Z0-9]"), "").take(8).ifEmpty { "mod" }
            val scopedFileName = if (ext.isNotEmpty()) {
                "$stem [patch_$shortId].$ext"
            } else {
                "$stem [patch_$shortId]"
            }
            return parent.copy(
                id = "${parent.id}#patch=$shortId",
                fileName = scopedFileName,
            )
        }
    }
}

/** Manages per-game soft-patches stored under `files/patches/<systemId>/<game.baseName>/`. */
class RomPatchStore(private val paths: AppPaths) {

    fun list(game: Game): List<RomPatch> {
        val dir = paths.patchesDir(game)
        val meta = File(dir, META_FILE)
        if (!meta.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(meta.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val patch = RomPatch.fromJson(arr.getJSONObject(i)) ?: continue
                    if (patchFile(game, patch).exists()) add(patch)
                }
            }.sortedByDescending { it.addedAt }
        }.getOrDefault(emptyList())
    }

    fun patchFile(game: Game, patch: RomPatch): File =
        File(paths.patchesDir(game), patch.fileName)

    /**
     * Imports a `.ips` / `.bps` / `.ups` / `.xdelta` / `.vcdiff` (or a `.zip` archive containing
     * one) directly from SAF [uri] and attaches it under [game].
     */
    fun attach(context: Context, game: Game, uri: Uri): RomPatch {
        val rawName = queryDisplayName(context, uri) ?: "patch.bps"
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw PatchException("Couldn't read the selected patch file.")
        return attachBytes(game, rawName, rawBytes)
    }

    /** Pure file-system attach helper (also directly unit-testable without Android Context). */
    fun attachBytes(game: Game, originalFileName: String, rawBytes: ByteArray): RomPatch {
        if (rawBytes.isEmpty()) throw PatchException("The selected patch file is empty.")
        if (rawBytes.size > MAX_PATCH_BYTES) {
            throw PatchException("Patch file exceeds 64 MB soft-patch limit.")
        }

        val (effectiveName, patchBytes) = if (originalFileName.endsWith(".zip", ignoreCase = true) ||
            (rawBytes.size >= 4 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte())
        ) {
            RomPatcher.extractPatchFromZip(rawBytes)
                ?: throw PatchException("No .ips, .bps, .ups, or .xdelta patch found inside $originalFileName.")
        } else {
            originalFileName to rawBytes
        }

        val format = RomPatcher.detectFormat(patchBytes, effectiveName)
            ?: throw PatchException(
                "Unsupported file \"$effectiveName\". Choose a .ips, .bps, .ups, .xdelta, or .zip patch file.",
            )

        val dir = paths.patchesDir(game).apply { mkdirs() }
        val id = UUID.randomUUID().toString().replace("-", "").take(12)
        val ext = effectiveName.substringAfterLast('.', format.extensions.first()).lowercase()
            .takeIf { it in PatchFormat.allExtensions } ?: format.extensions.first()
        val storedFileName = "$id.$ext"
        File(dir, storedFileName).writeBytes(patchBytes)

        val patch = RomPatch(
            id = id,
            name = RomPatch.prettyName(effectiveName),
            fileName = storedFileName,
            format = format,
            size = patchBytes.size.toLong(),
            addedAt = System.currentTimeMillis(),
            separateSave = true,
        )
        val updated = list(game).filterNot { it.id == patch.id } + patch
        writeMeta(dir, updated)
        return patch
    }

    fun setSeparateSave(game: Game, patchId: String, separate: Boolean) {
        val dir = paths.patchesDir(game)
        val updated = list(game).map { if (it.id == patchId) it.copy(separateSave = separate) else it }
        writeMeta(dir, updated)
    }

    fun delete(game: Game, patchId: String) {
        val dir = paths.patchesDir(game)
        val current = list(game)
        val victim = current.firstOrNull { it.id == patchId } ?: return
        patchFile(game, victim).delete()
        val remaining = current.filterNot { it.id == patchId }
        if (remaining.isEmpty()) {
            File(dir, META_FILE).delete()
            dir.delete()
        } else {
            writeMeta(dir, remaining)
        }
    }

    private fun writeMeta(dir: File, patches: List<RomPatch>) {
        dir.mkdirs()
        val arr = JSONArray()
        patches.forEach { arr.put(it.toJson()) }
        File(dir, META_FILE).writeText(arr.toString())
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return c.getString(idx)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private companion object {
        const val META_FILE = "patches.json"
        const val MAX_PATCH_BYTES = 64 * 1024 * 1024
    }
}
