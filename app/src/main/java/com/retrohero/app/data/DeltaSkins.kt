package com.retrohero.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** A rectangle in Delta skin mapping-point coordinates. */
data class DeltaRect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
}

data class DeltaSize(val width: Float, val height: Float)

/** Touch-target padding in mapping points. */
data class DeltaEdges(
    val top: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
    val right: Float = 0f,
)

/** Image assets for one representation: either a resizable PDF or PNGs at small/medium/large. */
data class DeltaAssets(
    val resizable: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
) {
    /** Picks the best available asset file name (preferring resizable PDF, then large → medium → small PNG). */
    fun bestAsset(): String? = resizable ?: large ?: medium ?: small
}

data class DeltaThumbstick(val name: String, val width: Float, val height: Float)

sealed interface DeltaInputs {
    /** One or more button identifiers activated together when tapped (e.g. `["a"]` or `["a", "b", "start", "select"]`). */
    data class Buttons(val ids: List<String>) : DeltaInputs

    /** 4-way D-pad or analog thumbstick mapping. */
    data class Directional(
        val up: String? = null,
        val down: String? = null,
        val left: String? = null,
        val right: String? = null,
    ) : DeltaInputs

    /** Nintendo DS touch screen region (`{"x": "touchScreenX", "y": "touchScreenY"}`). */
    data object TouchScreen : DeltaInputs
}

data class DeltaItem(
    val inputs: DeltaInputs,
    val frame: DeltaRect,
    val extendedEdges: DeltaEdges,
    val thumbstick: DeltaThumbstick? = null,
    /** Optional per-button graphic asset name (e.g. `iphone_a.pdf` in Delta's Standard GBA skin). */
    val asset: String? = null,
) {
    /** Hit-test rectangle with [extendedEdges] applied around [frame]. */
    val hitRect: DeltaRect
        get() = DeltaRect(
            x = frame.x - extendedEdges.left,
            y = frame.y - extendedEdges.top,
            width = frame.width + extendedEdges.left + extendedEdges.right,
            height = frame.height + extendedEdges.top + extendedEdges.bottom,
        )
}

data class DeltaScreen(
    val inputFrame: DeltaRect?,
    val outputFrame: DeltaRect,
)

data class DeltaRepresentation(
    val assets: DeltaAssets,
    val mappingSize: DeltaSize,
    val extendedEdges: DeltaEdges,
    val translucent: Boolean,
    val items: List<DeltaItem>,
    val screens: List<DeltaScreen>,
)

data class DeltaOrientationPair(
    val portrait: DeltaRepresentation? = null,
    val landscape: DeltaRepresentation? = null,
) {
    fun forOrientation(landscape: Boolean): DeltaRepresentation? =
        if (landscape) this.landscape else this.portrait
}

data class DeltaRepresentations(
    val iphoneStandard: DeltaOrientationPair? = null,
    val iphoneEdgeToEdge: DeltaOrientationPair? = null,
    val ipadStandard: DeltaOrientationPair? = null,
    val ipadSplitView: DeltaOrientationPair? = null,
) {
    /**
     * Picks the closest representation for the current device class and orientation.
     * Returns null if the skin does not define any layout for [landscape].
     */
    fun pick(landscape: Boolean, tablet: Boolean, tallPhone: Boolean): DeltaRepresentation? {
        val order = when {
            tablet -> listOf(ipadStandard, iphoneEdgeToEdge, iphoneStandard, ipadSplitView)
            tallPhone -> listOf(iphoneEdgeToEdge, iphoneStandard, ipadStandard)
            else -> listOf(iphoneStandard, iphoneEdgeToEdge, ipadStandard)
        }
        return order.firstNotNullOfOrNull { it?.forOrientation(landscape) }
    }

    /** Any available representation, preferring portrait for previews. */
    fun previewRepresentation(): DeltaRepresentation? =
        pick(landscape = false, tablet = false, tallPhone = true)
            ?: pick(landscape = true, tablet = false, tallPhone = true)
}

/** A parsed `.deltaskin` bundle extracted on disk at [dir]. */
data class DeltaSkin(
    val name: String,
    val identifier: String,
    val gameTypeIdentifier: String,
    val systemIds: List<String>,
    val debug: Boolean,
    val representations: DeltaRepresentations,
    val dir: File,
) {
    val primarySystemId: String? get() = systemIds.firstOrNull()

    fun assetFile(name: String): File = File(dir, name)
}

/** Special (non-RetroPad) actions supported by Delta skins. */
enum class DeltaAction {
    MENU,
    QUICK_SAVE,
    QUICK_LOAD,
    FAST_FORWARD_HOLD,
    FAST_FORWARD_TOGGLE,
    REWIND_HOLD,
    SCREENSHOT,
}

/** Resolved target of a single Delta input string. */
sealed interface DeltaMappedInput {
    data class Button(val retroPadId: Int) : DeltaMappedInput
    data class Action(val action: DeltaAction) : DeltaMappedInput
}

object DeltaSkins {
    private const val MAX_ENTRIES = 256
    private const val MAX_BYTES = 64L * 1024 * 1024

    /** Maps a Delta / Ignited / Manic `gameTypeIdentifier` to RetroHero system IDs. */
    fun systemIdsFor(gameTypeIdentifier: String): List<String> {
        val raw = gameTypeIdentifier.trim().lowercase()
        val suffix = raw.substringAfterLast('.')
        return when (suffix) {
            "nes" -> listOf("nes")
            "snes" -> listOf("snes")
            "gbc" -> listOf("gbc", "gb")
            "gb" -> listOf("gb", "gbc")
            "gba" -> listOf("gba")
            "ds", "nds" -> listOf("nds")
            "3ds", "n3ds" -> listOf("3ds")
            "genesis", "md", "megadrive" -> listOf("genesis", "segacd", "32x")
            "gg", "gamegear" -> listOf("gg")
            "ms", "sms", "mastersystem" -> listOf("sms")
            "sg1000" -> listOf("sg1000")
            "n64" -> listOf("n64")
            "gc", "ngc", "gamecube" -> listOf("gc")
            "wii" -> listOf("wii")
            "saturn", "ss" -> listOf("saturn")
            "dc", "dreamcast" -> listOf("dreamcast")
            "psx", "ps1" -> listOf("psx")
            "psp" -> listOf("psp")
            "ps2" -> listOf("ps2")
            "pce", "tg16" -> listOf("pce", "pcecd")
            else -> emptyList()
        }
    }

    /** Maps a Delta button identifier string to a RetroPad button or special action. */
    fun mapInput(name: String, systemId: String): DeltaMappedInput? {
        val key = name.trim()
        when (key.lowercase()) {
            "menu" -> return DeltaMappedInput.Action(DeltaAction.MENU)
            "quicksave" -> return DeltaMappedInput.Action(DeltaAction.QUICK_SAVE)
            "quickload" -> return DeltaMappedInput.Action(DeltaAction.QUICK_LOAD)
            "fastforward" -> return DeltaMappedInput.Action(DeltaAction.FAST_FORWARD_HOLD)
            "togglefastforward" -> return DeltaMappedInput.Action(DeltaAction.FAST_FORWARD_TOGGLE)
            "rewind", "togglerewind" -> return DeltaMappedInput.Action(DeltaAction.REWIND_HOLD)
            "screenshot" -> return DeltaMappedInput.Action(DeltaAction.SCREENSHOT)
        }
        // Genesis maps A/B/C/X/Y/Z/Mode onto RetroPad differently from Nintendo systems.
        if (systemId == "genesis" || systemId == "segacd" || systemId == "32x" || systemId == "saturn") {
            return when (key.lowercase()) {
                "a" -> DeltaMappedInput.Button(RetroPad.Y)
                "b" -> DeltaMappedInput.Button(RetroPad.B)
                "c" -> DeltaMappedInput.Button(RetroPad.A)
                "x" -> DeltaMappedInput.Button(RetroPad.L)
                "y" -> DeltaMappedInput.Button(RetroPad.X)
                "z" -> DeltaMappedInput.Button(RetroPad.R)
                "mode", "select" -> DeltaMappedInput.Button(RetroPad.SELECT)
                "start" -> DeltaMappedInput.Button(RetroPad.START)
                "up", "analogstickup" -> DeltaMappedInput.Button(RetroPad.UP)
                "down", "analogstickdown" -> DeltaMappedInput.Button(RetroPad.DOWN)
                "left", "analogstickleft" -> DeltaMappedInput.Button(RetroPad.LEFT)
                "right", "analogstickright" -> DeltaMappedInput.Button(RetroPad.RIGHT)
                else -> null
            }
        }
        return when (key.lowercase()) {
            "a" -> DeltaMappedInput.Button(RetroPad.A)
            "b" -> DeltaMappedInput.Button(RetroPad.B)
            "x", "cup" -> DeltaMappedInput.Button(RetroPad.X)
            "y", "cleft" -> DeltaMappedInput.Button(RetroPad.Y)
            "cdown" -> DeltaMappedInput.Button(RetroPad.L2)
            "cright" -> DeltaMappedInput.Button(RetroPad.R2)
            "l", "l1" -> DeltaMappedInput.Button(RetroPad.L)
            "r", "r1" -> DeltaMappedInput.Button(RetroPad.R)
            "l2", "z", "zl" -> DeltaMappedInput.Button(RetroPad.L2)
            "r2", "zr" -> DeltaMappedInput.Button(RetroPad.R2)
            "l3" -> DeltaMappedInput.Button(RetroPad.L3)
            "r3" -> DeltaMappedInput.Button(RetroPad.R3)
            "select", "mode" -> DeltaMappedInput.Button(RetroPad.SELECT)
            "start" -> DeltaMappedInput.Button(RetroPad.START)
            "up", "analogstickup" -> DeltaMappedInput.Button(RetroPad.UP)
            "down", "analogstickdown" -> DeltaMappedInput.Button(RetroPad.DOWN)
            "left", "analogstickleft" -> DeltaMappedInput.Button(RetroPad.LEFT)
            "right", "analogstickright" -> DeltaMappedInput.Button(RetroPad.RIGHT)
            else -> null
        }
    }

    /** Parses `info.json` text for a skin living in [dir]. */
    fun parseInfo(text: String, dir: File): DeltaSkin {
        val o = JSONObject(text)
        val name = o.optString("name").trim().ifEmpty {
            throw IllegalArgumentException("Skin info.json is missing a name")
        }.take(60)
        val rawId = o.optString("identifier").trim().ifEmpty {
            "custom." + Regex("[^a-z0-9]+").replace(name.lowercase(), ".").trim('.')
        }
        val gameType = o.optString("gameTypeIdentifier").trim().ifEmpty {
            throw IllegalArgumentException("Skin info.json is missing gameTypeIdentifier")
        }
        val systems = systemIdsFor(gameType)
        val debug = o.optBoolean("debug", false)
        val repsObj = o.optJSONObject("representations")
            ?: throw IllegalArgumentException("Skin info.json is missing representations")

        val iphone = repsObj.optJSONObject("iphone")
        val ipad = repsObj.optJSONObject("ipad")
        val representations = DeltaRepresentations(
            iphoneStandard = iphone?.optJSONObject("standard")?.let { parseOrientationPair(it, 414f, 736f) },
            iphoneEdgeToEdge = iphone?.optJSONObject("edgeToEdge")?.let { parseOrientationPair(it, 414f, 896f) },
            ipadStandard = ipad?.optJSONObject("standard")?.let { parseOrientationPair(it, 768f, 1024f) },
            ipadSplitView = ipad?.optJSONObject("splitView")?.let { parseOrientationPair(it, 768f, 400f) },
        )
        if (representations.previewRepresentation() == null) {
            throw IllegalArgumentException("Skin has no usable portrait or landscape representations")
        }
        return DeltaSkin(
            name = name,
            identifier = rawId,
            gameTypeIdentifier = gameType,
            systemIds = systems,
            debug = debug,
            representations = representations,
            dir = dir,
        )
    }

    private fun parseOrientationPair(o: JSONObject, defaultW: Float, defaultH: Float): DeltaOrientationPair {
        val portrait = o.optJSONObject("portrait")?.let { parseRepresentation(it, defaultW, defaultH) }
        val landscape = o.optJSONObject("landscape")?.let { parseRepresentation(it, defaultH, defaultW) }
        return DeltaOrientationPair(portrait = portrait, landscape = landscape)
    }

    private fun parseRepresentation(o: JSONObject, defaultW: Float, defaultH: Float): DeltaRepresentation? {
        val assetsObj = o.optJSONObject("assets") ?: return null
        val assets = DeltaAssets(
            resizable = assetsObj.optString("resizable").takeIf { it.isNotBlank() },
            small = assetsObj.optString("small").takeIf { it.isNotBlank() },
            medium = assetsObj.optString("medium").takeIf { it.isNotBlank() },
            large = assetsObj.optString("large").takeIf { it.isNotBlank() },
        )
        if (assets.bestAsset() == null) return null

        val mapObj = o.optJSONObject("mappingSize")
        val mappingSize = DeltaSize(
            width = mapObj?.optDouble("width", defaultW.toDouble())?.toFloat()?.takeIf { it > 0f } ?: defaultW,
            height = mapObj?.optDouble("height", defaultH.toDouble())?.toFloat()?.takeIf { it > 0f } ?: defaultH,
        )
        val baseEdges = parseEdges(o.optJSONObject("extendedEdges"), DeltaEdges())
        val translucent = o.optBoolean("translucent", false)

        val items = mutableListOf<DeltaItem>()
        val itemsArr = o.optJSONArray("items")
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val itemObj = itemsArr.optJSONObject(i) ?: continue
                parseItem(itemObj, baseEdges)?.let(items::add)
            }
        }

        val screens = mutableListOf<DeltaScreen>()
        val screensArr = o.optJSONArray("screens")
        if (screensArr != null) {
            for (i in 0 until screensArr.length()) {
                val sObj = screensArr.optJSONObject(i) ?: continue
                val outFrame = parseRect(sObj.optJSONObject("outputFrame")) ?: continue
                val inFrame = parseRect(sObj.optJSONObject("inputFrame"))
                screens += DeltaScreen(inputFrame = inFrame, outputFrame = outFrame)
            }
        } else {
            // Legacy single-screen property used by some older Delta skins.
            parseRect(o.optJSONObject("gameScreenFrame"))?.let {
                screens += DeltaScreen(inputFrame = null, outputFrame = it)
            }
        }

        return DeltaRepresentation(
            assets = assets,
            mappingSize = mappingSize,
            extendedEdges = baseEdges,
            translucent = translucent,
            items = items,
            screens = screens,
        )
    }

    private fun parseItem(o: JSONObject, baseEdges: DeltaEdges): DeltaItem? {
        val frame = parseRect(o.optJSONObject("frame")) ?: return null
        val edges = parseEdges(o.optJSONObject("extendedEdges"), baseEdges)
        val thumb = o.optJSONObject("thumbstick")?.let { t ->
            val name = t.optString("name").takeIf { it.isNotBlank() } ?: return@let null
            DeltaThumbstick(
                name = name,
                width = t.optDouble("width", frame.width.toDouble()).toFloat(),
                height = t.optDouble("height", frame.height.toDouble()).toFloat(),
            )
        }
        val rawInputs = o.opt("inputs") ?: return null
        val inputs: DeltaInputs = when (rawInputs) {
            is JSONArray -> {
                val list = (0 until rawInputs.length())
                    .mapNotNull { rawInputs.optString(it).takeIf { s -> s.isNotBlank() } }
                if (list.isEmpty()) return null
                DeltaInputs.Buttons(list)
            }
            is JSONObject -> {
                val x = rawInputs.optString("x")
                val y = rawInputs.optString("y")
                if (x.equals("touchScreenX", ignoreCase = true) || y.equals("touchScreenY", ignoreCase = true)) {
                    DeltaInputs.TouchScreen
                } else {
                    val up = rawInputs.optString("up").takeIf { it.isNotBlank() }
                    val down = rawInputs.optString("down").takeIf { it.isNotBlank() }
                    val left = rawInputs.optString("left").takeIf { it.isNotBlank() }
                    val right = rawInputs.optString("right").takeIf { it.isNotBlank() }
                    if (up == null && down == null && left == null && right == null) return null
                    DeltaInputs.Directional(up = up, down = down, left = left, right = right)
                }
            }
            else -> return null
        }
        val itemAsset = o.optJSONObject("asset")?.optString("name")?.takeIf { it.isNotBlank() }
        return DeltaItem(inputs = inputs, frame = frame, extendedEdges = edges, thumbstick = thumb, asset = itemAsset)
    }

    private fun parseRect(o: JSONObject?): DeltaRect? {
        if (o == null) return null
        val w = o.optDouble("width", -1.0).toFloat()
        val h = o.optDouble("height", -1.0).toFloat()
        if (w <= 0f || h <= 0f) return null
        return DeltaRect(
            x = o.optDouble("x", 0.0).toFloat(),
            y = o.optDouble("y", 0.0).toFloat(),
            width = w,
            height = h,
        )
    }

    private fun parseEdges(o: JSONObject?, fallback: DeltaEdges): DeltaEdges {
        if (o == null) return fallback
        return DeltaEdges(
            top = if (o.has("top")) o.optDouble("top", 0.0).toFloat() else fallback.top,
            bottom = if (o.has("bottom")) o.optDouble("bottom", 0.0).toFloat() else fallback.bottom,
            left = if (o.has("left")) o.optDouble("left", 0.0).toFloat() else fallback.left,
            right = if (o.has("right")) o.optDouble("right", 0.0).toFloat() else fallback.right,
        )
    }

    // ---- On-disk store ---------------------------------------------------------------------

    /** Lists all valid installed Delta skins inside [rootDir]. */
    fun list(rootDir: File): List<DeltaSkin> =
        rootDir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            ?.mapNotNull { dir ->
                val info = File(dir, "info.json")
                if (!info.isFile) null else runCatching { parseInfo(info.readText(), dir) }.getOrNull()
            }
            .orEmpty()

    /** Installed Delta skins compatible with RetroHero [systemId]. */
    fun forSystem(rootDir: File, systemId: String): List<DeltaSkin> =
        list(rootDir).filter { systemId in it.systemIds }

    /** Finds a single installed skin by its [identifier]. */
    fun find(rootDir: File, identifier: String?): DeltaSkin? =
        if (identifier.isNullOrBlank()) null else list(rootDir).firstOrNull { it.identifier == identifier }

    /** Deletes an installed skin directory. */
    fun delete(skin: DeltaSkin) {
        skin.dir.deleteRecursively()
    }

    /**
     * Extracts any `.deltaskin` files bundled under `assets/default_skins/` into [rootDir] once,
     * and sets each as the default skin for its compatible system(s) if no skin is selected yet.
     */
    fun installBundledDefaults(context: android.content.Context, rootDir: File, settings: Settings) {
        val marker = File(rootDir, ".defaults_v1")
        if (marker.isFile) return
        val names = runCatching { context.assets.list("default_skins") }.getOrNull().orEmpty()
            .filter { it.endsWith(".deltaskin", ignoreCase = true) || it.endsWith(".zip", ignoreCase = true) }
            .sorted()
        if (names.isEmpty()) return
        rootDir.mkdirs()
        for (name in names) {
            runCatching {
                context.assets.open("default_skins/$name").use { stream ->
                    val skin = import(rootDir, stream)
                    for (sysId in skin.systemIds) {
                        if (settings.deltaSkinFor(sysId) == null) {
                            settings.setDeltaSkinFor(sysId, skin.identifier)
                        }
                    }
                }
            }
        }
        runCatching { marker.writeText(names.joinToString("\n")) }
    }

    /**
     * Imports a `.deltaskin` (ZIP) stream into [rootDir].
     * Re-importing a skin with the same `identifier` replaces the previous copy.
     */
    fun import(rootDir: File, stream: InputStream): DeltaSkin {
        rootDir.mkdirs()
        val tmp = File(rootDir, ".import-${System.nanoTime()}").apply { mkdirs() }
        try {
            extractZip(stream, tmp)
            val contentRoot = findSkinRoot(tmp)
                ?: throw IllegalArgumentException("No info.json found inside .deltaskin archive")
            val infoFile = File(contentRoot, "info.json")
            val parsed = parseInfo(infoFile.readText(), contentRoot)
            val previewAsset = parsed.representations.previewRepresentation()?.assets?.bestAsset()
            if (previewAsset == null || !File(contentRoot, previewAsset).isFile) {
                throw IllegalArgumentException("Skin asset “${previewAsset ?: "?"}” is missing from the archive")
            }

            val folderName = "${sanitizeFolderName(parsed.identifier).take(64)}-${sha1Prefix(parsed.identifier)}"
            val target = File(rootDir, folderName)
            val rootCanonical = rootDir.canonicalFile
            require(target.canonicalFile.parentFile == rootCanonical) {
                "Invalid skin import directory"
            }
            val tmpCanonical = tmp.canonicalFile
            val existingDir = list(rootDir)
                .firstOrNull { it.identifier == parsed.identifier && it.dir.canonicalFile != tmpCanonical }
                ?.dir
            if (existingDir != null) {
                require(existingDir.canonicalFile.parentFile == rootCanonical) {
                    "Existing skin directory is outside the skin folder"
                }
                existingDir.deleteRecursively()
            }
            if (target.exists()) {
                throw IllegalArgumentException("Skin import directory already exists")
            }
            require(target.canonicalFile.parentFile == rootCanonical) {
                "Invalid skin import directory"
            }
            if (contentRoot == tmp) {
                tmp.copyRecursively(target, overwrite = false)
                tmp.deleteRecursively()
            } else {
                contentRoot.copyRecursively(target, overwrite = false)
                tmp.deleteRecursively()
            }
            return parsed.copy(dir = target)
        } catch (t: Throwable) {
            tmp.deleteRecursively()
            throw t
        }
    }

    private fun extractZip(stream: InputStream, destDir: File) {
        val destCanonical = destDir.canonicalPath + File.separator
        var entries = 0
        var totalBytes = 0L
        val buf = ByteArray(16 * 1024)
        ZipInputStream(stream).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                if (name.startsWith("__MACOSX/") || name.endsWith("/.DS_Store") || name == ".DS_Store") {
                    zis.closeEntry()
                    continue
                }
                if (++entries > MAX_ENTRIES) throw IllegalArgumentException("Skin archive has too many files")
                val out = File(destDir, name)
                val outCanonical = out.canonicalPath
                if (!outCanonical.startsWith(destCanonical)) {
                    throw IllegalArgumentException("Invalid path in skin archive")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { fos ->
                        while (true) {
                            val n = zis.read(buf)
                            if (n <= 0) break
                            totalBytes += n
                            if (totalBytes > MAX_BYTES) throw IllegalArgumentException("Skin archive is too large")
                            fos.write(buf, 0, n)
                        }
                    }
                }
                zis.closeEntry()
            }
        }
    }

    /** Finds `info.json` at [dir] root or inside a single top-level wrapper folder. */
    private fun findSkinRoot(dir: File): File? {
        if (File(dir, "info.json").isFile) return dir
        val subdirs = dir.listFiles { f -> f.isDirectory && f.name != "__MACOSX" }.orEmpty()
        if (subdirs.size == 1 && File(subdirs[0], "info.json").isFile) return subdirs[0]
        return null
    }

    private fun sanitizeFolderName(identifier: String): String =
        Regex("[^a-zA-Z0-9._-]+").replace(identifier, "_").trim('_').ifEmpty { "skin" }

    private fun sha1Prefix(identifier: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(identifier.toByteArray(Charsets.UTF_8))
            .take(4)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
