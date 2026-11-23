package com.retrohero.app.data

import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Metadata for a shareable `.rhclip` playable moment / challenge capsule.
 * Capsules never contain ROM data — only a save state, optional screenshot, and game metadata.
 */
data class MomentManifest(
    val title: String,
    val note: String,
    val systemId: String,
    val coreId: String,
    val gameTitle: String,
    val gameBaseName: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("format", FORMAT_VERSION)
        .put("title", title)
        .put("note", note)
        .put("systemId", systemId)
        .put("coreId", coreId)
        .put("gameTitle", gameTitle)
        .put("gameBaseName", gameBaseName)
        .put("createdAt", createdAt)

    companion object {
        const val FORMAT_VERSION = 1

        fun fromJson(text: String): MomentManifest {
            val o = JSONObject(text)
            val format = o.optInt("format", 0)
            if (format != FORMAT_VERSION) {
                throw IllegalArgumentException("Unsupported .rhclip format version: $format")
            }
            val systemId = o.optString("systemId").trim()
            val gameTitle = o.optString("gameTitle").trim()
            if (systemId.isEmpty() || gameTitle.isEmpty()) {
                throw IllegalArgumentException("Invalid .rhclip manifest")
            }
            return MomentManifest(
                title = o.optString("title").trim().ifEmpty { gameTitle }.take(80),
                note = o.optString("note").trim().take(240),
                systemId = systemId,
                coreId = o.optString("coreId").trim(),
                gameTitle = gameTitle,
                gameBaseName = o.optString("gameBaseName").trim().ifEmpty { gameTitle },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }
}

/** An extracted `.rhclip` capsule ready to preview and launch. */
data class ExtractedMoment(
    val manifest: MomentManifest,
    val stateFile: File,
    val previewFile: File?,
)

object MomentCapsule {
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val STATE_ENTRY = "state.bin"
    private const val PREVIEW_ENTRY = "preview.png"
    private const val MAX_BYTES = 24L * 1024 * 1024

    /** Writes a `.rhclip` ZIP archive containing [manifest], [stateFile], and optional [previewFile]. */
    fun export(
        out: OutputStream,
        manifest: MomentManifest,
        stateFile: File,
        previewFile: File? = null,
    ) {
        require(stateFile.isFile) { "Save state file does not exist" }
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toJson().toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(STATE_ENTRY))
            stateFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            if (previewFile != null && previewFile.isFile) {
                zip.putNextEntry(ZipEntry(PREVIEW_ENTRY))
                previewFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Extracts a `.rhclip` archive from [input] into [destDir] and validates its contents. */
    fun extract(input: InputStream, destDir: File): ExtractedMoment {
        destDir.mkdirs()
        val destCanonical = destDir.canonicalPath + File.separator
        var totalBytes = 0L
        val buf = ByteArray(16 * 1024)
        ZipInputStream(input).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.isDirectory) {
                    zis.closeEntry()
                    continue
                }
                val out = File(destDir, entry.name)
                if (!out.canonicalPath.startsWith(destCanonical)) {
                    throw IllegalArgumentException("Invalid entry path in .rhclip")
                }
                out.parentFile?.mkdirs()
                out.outputStream().use { fos ->
                    while (true) {
                        val n = zis.read(buf)
                        if (n <= 0) break
                        totalBytes += n
                        if (totalBytes > MAX_BYTES) throw IllegalArgumentException(".rhclip file is too large")
                        fos.write(buf, 0, n)
                    }
                }
                zis.closeEntry()
            }
        }
        val manifestFile = File(destDir, MANIFEST_ENTRY)
        val stateFile = File(destDir, STATE_ENTRY)
        if (!manifestFile.isFile) throw IllegalArgumentException("Missing manifest.json in .rhclip")
        if (!stateFile.isFile || stateFile.length() == 0L) throw IllegalArgumentException("Missing save state in .rhclip")
        val manifest = MomentManifest.fromJson(manifestFile.readText())
        val preview = File(destDir, PREVIEW_ENTRY).takeIf { it.isFile && it.length() > 0L }
        return ExtractedMoment(manifest = manifest, stateFile = stateFile, previewFile = preview)
    }

    /** Finds the best matching local game in [library] for [manifest]. */
    fun findMatchingGame(manifest: MomentManifest, library: List<Game>): Game? {
        val sameSystem = library.filter { it.systemId == manifest.systemId }
        if (sameSystem.isEmpty()) return null
        return sameSystem.firstOrNull { it.baseName.equals(manifest.gameBaseName, ignoreCase = true) }
            ?: sameSystem.firstOrNull { it.title.equals(manifest.gameTitle, ignoreCase = true) }
            ?: sameSystem.firstOrNull { normalizeName(it.title) == normalizeName(manifest.gameTitle) }
    }

    private fun normalizeName(s: String): String =
        Regex("[^a-z0-9]+").replace(s.lowercase(), "")
}
