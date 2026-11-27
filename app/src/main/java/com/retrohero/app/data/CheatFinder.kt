package com.retrohero.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** A matched `.cht` cheat pack in the remote libretro-database or built-in offline classics catalog. */
data class CheatPackMatch(
    val folder: String,
    val fileName: String,
    val displayLabel: String,
    val deviceTag: String?,
    val score: Int,
    val isBuiltIn: Boolean = false,
) {
    fun rawUrl(): String =
        "https://raw.githubusercontent.com/libretro/libretro-database/master/cht/${CheatFinder.encodePathSegment(folder)}/${CheatFinder.encodePathSegment(fileName)}"
}

/** Result of an auto-find cheat lookup for a [Game]. */
data class CheatSearchResult(
    val cheats: List<Cheat>,
    val activePack: CheatPackMatch?,
    val alternatePacks: List<CheatPackMatch> = emptyList(),
    val sourceLabel: String = "",
    val error: String? = null,
)

/**
 * Automatic game cheat code discovery engine.
 *
 * Uses a resilient 3-tier pipeline:
 * 1. Full system catalog fuzzy matching against `libretro/libretro-database/cht` via GitHub Trees API
 *    (cached per console on disk for 7 days, downloading `.cht` files directly from `raw.githubusercontent.com`).
 * 2. Direct `raw.githubusercontent.com` smart candidate probing (zero GitHub API rate limit).
 * 3. Built-in verified offline classics catalog so iconic games work even without internet access.
 */
object CheatFinder {
    private const val INDEX_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 12_000
    private const val USER_AGENT = "RetroHero-Android/1.0"

    /** Maps each RetroHero system ID to its official `libretro-database/cht/` folder(s), best first. */
    fun chtFoldersFor(systemId: String): List<String> = when (systemId) {
        "nes" -> listOf("Nintendo - Nintendo Entertainment System", "Nintendo - Family Computer Disk System")
        "snes" -> listOf("Nintendo - Super Nintendo Entertainment System", "Nintendo - Satellaview")
        "n64" -> listOf("Nintendo - Nintendo 64")
        "gb" -> listOf("Nintendo - Game Boy")
        "gbc" -> listOf("Nintendo - Game Boy Color", "Nintendo - Game Boy")
        "gba" -> listOf("Nintendo - Game Boy Advance")
        "nds" -> listOf("Nintendo - Nintendo DS")
        "vb" -> listOf("Nintendo - Virtual Boy")
        "genesis" -> listOf("Sega - Mega Drive - Genesis")
        "sms" -> listOf("Sega - Master System - Mark III")
        "gg" -> listOf("Sega - Game Gear")
        "sg1000" -> listOf("Sega - SG-1000")
        "segacd" -> listOf("Sega - Mega-CD - Sega CD")
        "32x" -> listOf("Sega - 32X")
        "saturn" -> listOf("Sega - Saturn")
        "dreamcast" -> listOf("Sega - Dreamcast")
        "psx" -> listOf("Sony - PlayStation")
        "psp" -> listOf("Sony - PlayStation Portable")
        "pce" -> listOf("NEC - PC Engine - TurboGrafx 16")
        "supergrafx" -> listOf("NEC - PC Engine SuperGrafx", "NEC - PC Engine - TurboGrafx 16")
        "pcecd" -> listOf("NEC - PC Engine CD - TurboGrafx-CD")
        "pcfx" -> listOf("NEC - PC-FX")
        "ngp" -> listOf("SNK - Neo Geo Pocket Color", "SNK - Neo Geo Pocket")
        "ws" -> listOf("Bandai - WonderSwan Color", "Bandai - WonderSwan")
        "lynx" -> listOf("Atari - Lynx")
        "atari2600" -> listOf("Atari - 2600")
        "atari5200" -> listOf("Atari - 5200")
        "atari7800" -> listOf("Atari - 7800")
        "jaguar" -> listOf("Atari - Jaguar")
        "coleco" -> listOf("Coleco - ColecoVision")
        "intellivision" -> listOf("Mattel - Intellivision")
        "msx" -> listOf("Microsoft - MSX2", "Microsoft - MSX")
        "dos" -> listOf("DOS")
        "tic80" -> listOf("TIC-80")
        "zxspectrum" -> listOf("Sinclair - ZX Spectrum +3")
        "arcade", "neogeo" -> listOf("FBNeo - Arcade Games")
        else -> emptyList()
    }

    /**
     * Parses a standard libretro `.cht` file into a list of [Cheat] entries (all disabled by default
     * so auto-importing never unexpectedly alters gameplay or demotes RetroAchievements Hardcore mode).
     */
    fun parseCht(text: String): List<Cheat> {
        val descs = HashMap<Int, String>()
        val codes = HashMap<Int, String>()
        val lineRegex = Regex("""^\s*cheat(\d+)_(desc|code)\s*=\s*(.+?)\s*$""")
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue
            val m = lineRegex.matchEntire(line) ?: continue
            val idx = m.groupValues[1].toIntOrNull() ?: continue
            val field = m.groupValues[2]
            val value = unquote(m.groupValues[3])
            when (field) {
                "desc" -> if (value.isNotBlank()) descs[idx] = value
                "code" -> if (value.isNotBlank()) codes[idx] = value
            }
        }
        val seen = HashSet<String>()
        val result = ArrayList<Cheat>(codes.size)
        for (idx in codes.keys.sorted()) {
            val rawCode = codes[idx]?.trim().orEmpty()
            val normalizedCode = normalizeChtCode(rawCode)
            if (normalizedCode.isEmpty()) continue
            val name = descs[idx]?.trim()?.ifBlank { "Cheat ${idx + 1}" } ?: "Cheat ${idx + 1}"
            val dedupKey = "${name.lowercase(Locale.US)}||$normalizedCode"
            if (seen.add(dedupKey)) {
                result += Cheat(name = name, code = normalizedCode, enabled = false)
            }
        }
        return result
    }

    private fun unquote(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed.substring(1, trimmed.length - 1).replace("\\\"", "\"")
        } else {
            trimmed
        }
    }

    private fun normalizeChtCode(raw: String): String =
        raw.split('\n', ',', '+', ';')
            .map { it.trim().replace(Regex("\\s+"), " ").uppercase(Locale.US) }
            .filter { it.isNotEmpty() }
            .joinToString("+")

    /**
     * Merges newly discovered cheats into an existing list without losing any user-added codes
     * or changing enabled toggles on existing cheats.
     */
    fun merge(existing: List<Cheat>, discovered: List<Cheat>): List<Cheat> {
        if (existing.isEmpty()) return discovered
        if (discovered.isEmpty()) return existing
        val existingCodes = existing.map { it.code.uppercase(Locale.US) }.toHashSet()
        val existingNames = existing.map { it.name.lowercase(Locale.US) }.toHashSet()
        val additions = discovered.filter { c ->
            c.code.uppercase(Locale.US) !in existingCodes && c.name.lowercase(Locale.US) !in existingNames
        }
        return existing + additions
    }

    /** True for high-value cheats players look for first (lives, health, invincibility, money, unlocks, master code). */
    fun isPopularCheat(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return POPULAR_KEYWORDS.any { n.contains(it) }
    }

    private val POPULAR_KEYWORDS = listOf(
        "infinite", "invincib", "lives", "health", "hp", "energy", "heart",
        "ammo", "money", "gold", "coin", "gil", "rupee", "ring", "score",
        "max ", "all items", "all weapons", "all levels", "all characters", "unlock",
        "moon jump", "super jump", "high jump", "walk through", "no clip",
        "master code", "enable code", "(m)", "must be on", "rare candy",
        "master ball", "shiny", "exp", "level 99", "level 100", "time", "mp", "magic",
        "one hit", "1 hit", "instant win", "always", "star", "mushroom", "fire flower",
    )

    // ---- Title Normalization & Fuzzy Ranking ----------------------------------------------------

    private val TAG_PAREN_REGEX = Regex("""\([^)]*\)""")
    private val TAG_BRACKET_REGEX = Regex("""\[[^\]]*\]""")
    private val NON_ALNUM_REGEX = Regex("""[^a-z0-9\s]""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    private val ROMAN_MAP = mapOf(
        "ii" to "2", "iii" to "3", "iv" to "4", "vi" to "6",
        "vii" to "7", "viii" to "8", "ix" to "9", "xi" to "11", "xii" to "12",
    )

    private val DEVICE_TAGS = listOf(
        "Game Genie", "Action Replay", "Pro Action Replay", "GameShark",
        "Code Breaker", "CodeBreaker", "Xploder", "Raw", "PAR", "AR",
    )

    /** Extracts a canonical comparable title stripped of region/revision/device tags and articles. */
    fun normalizeTitle(raw: String): String {
        var s = raw
            .removeSuffix(".cht")
            .removeSuffix(".CHT")
            .replace("é", "e", ignoreCase = true)
            .replace("É", "e")
            .replace("’", "'")
            .replace("&", " and ")
            .replace("+", " ")
        s = TAG_PAREN_REGEX.replace(s, " ")
        s = TAG_BRACKET_REGEX.replace(s, " ")
        // Handle No-Intro article suffix: "Legend of Zelda, The - Ocarina of Time" -> "The Legend of Zelda Ocarina of Time"
        s = s.replace(Regex("""(?i),\s*(the|a|an)(?=\s*[-:]|\s*$)"""), "")
        s = s.lowercase(Locale.US)
        s = s.replace(Regex("""^(the|a|an)\s+"""), "")
        s = NON_ALNUM_REGEX.replace(s, " ")
        val tokens = s.trim().split(WHITESPACE_REGEX)
            .filter { it.isNotEmpty() }
            .map { ROMAN_MAP[it] ?: it }
        return tokens.joinToString(" ")
    }

    /** Extracts the primary region from a ROM or `.cht` filename (e.g. "USA", "Europe", "Japan", "World"). */
    fun extractRegion(raw: String): String? {
        val lower = raw.lowercase(Locale.US)
        return when {
            "(usa" in lower || ", usa" in lower || "(u)" in lower -> "USA"
            "(world)" in lower || "(w)" in lower -> "World"
            "(europe" in lower || ", europe" in lower || "(e)" in lower -> "Europe"
            "(japan" in lower || ", japan" in lower || "(j)" in lower -> "Japan"
            else -> null
        }
    }

    /** Extracts the cheat device badge from a `.cht` filename (e.g. "Game Genie", "GameShark"). */
    fun extractCheatDevice(fileName: String): String? =
        DEVICE_TAGS.firstOrNull { fileName.contains("($it)", ignoreCase = true) }

    /** Builds a clean short label for a `.cht` filename (e.g. "USA · Game Genie"). */
    fun formatPackLabel(fileName: String): String {
        val region = extractRegion(fileName)
        val device = extractCheatDevice(fileName)
        return listOfNotNull(region, device).joinToString(" · ").ifEmpty {
            fileName.removeSuffix(".cht").take(32)
        }
    }

    private fun extractNumbers(normalized: String): Set<String> =
        normalized.split(' ').filter { it.isNotEmpty() && it.all(Char::isDigit) }.toSet()

    /**
     * Ranks candidate `.cht` filenames for [gameTitle] / [romBaseName] and returns the best matches
     * (sorted descending by relevance score).
     */
    fun rankCandidates(
        gameTitle: String,
        romBaseName: String,
        folder: String,
        filenames: List<String>,
        maxResults: Int = 6,
    ): List<CheatPackMatch> {
        val targetNorm = normalizeTitle(gameTitle).ifEmpty { normalizeTitle(romBaseName) }
        val baseNorm = normalizeTitle(romBaseName)
        if (targetNorm.isEmpty()) return emptyList()

        val targetTokens = targetNorm.split(' ').filter { it.isNotEmpty() }.toSet()
        val targetNums = extractNumbers(targetNorm)
        val preferredRegion = extractRegion(romBaseName) ?: "USA"

        val scored = ArrayList<CheatPackMatch>()
        for (file in filenames) {
            if (!file.endsWith(".cht", ignoreCase = true)) continue
            val candNorm = normalizeTitle(file)
            if (candNorm.isEmpty()) continue
            val candTokens = candNorm.split(' ').filter { it.isNotEmpty() }.toSet()
            val candNums = extractNumbers(candNorm)

            var score = 0
            when {
                candNorm == targetNorm || candNorm == baseNorm -> score += 120
                candNorm.startsWith("$targetNorm ") || targetNorm.startsWith("$candNorm ") -> {
                    val overlap = targetTokens.intersect(candTokens).size
                    val union = targetTokens.union(candTokens).size
                    score += (85 * overlap) / union.coerceAtLeast(1)
                }
                else -> {
                    val overlap = targetTokens.intersect(candTokens).size
                    if (overlap == 0) continue
                    val precision = overlap.toDouble() / candTokens.size.coerceAtLeast(1)
                    val recall = overlap.toDouble() / targetTokens.size.coerceAtLeast(1)
                    val f1 = (2.0 * precision * recall) / (precision + recall)
                    if (f1 < 0.62) continue
                    score += (f1 * 90).toInt()
                }
            }

            // Strongly penalize sequel/installment mismatches (e.g. "Mario 2" vs "Mario 3" or "Advance Wars" vs "Advance Wars 2")
            if (targetNums != candNums) {
                score -= 48
            }

            // Region preference bonus
            val candRegion = extractRegion(file)
            if (candRegion != null) {
                when {
                    candRegion.equals(preferredRegion, ignoreCase = true) -> score += 14
                    candRegion == "World" -> score += 10
                    candRegion == "USA" -> score += 8
                    candRegion == "Europe" -> score += 4
                }
            }

            // Slight preference for clean standard / popular cheat device formats
            val device = extractCheatDevice(file)
            if (device == null) score += 4
            else if (device in setOf("Game Genie", "GameShark", "Action Replay", "Code Breaker")) score += 3

            if (score >= 58) {
                scored += CheatPackMatch(
                    folder = folder,
                    fileName = file,
                    displayLabel = formatPackLabel(file),
                    deviceTag = device,
                    score = score,
                )
            }
        }
        return scored
            .sortedWith(compareByDescending<CheatPackMatch> { it.score }.thenBy { it.fileName.length })
            .take(maxResults)
    }

    /** Generates direct `raw.githubusercontent.com` candidate filenames when the full catalog index isn't available. */
    fun directProbeFilenames(gameTitle: String, romBaseName: String): List<String> {
        val out = LinkedHashSet<String>()
        val cleanBase = romBaseName.trim()
        val cleanTitle = gameTitle.trim()

        val titleVariants = linkedSetOf(cleanTitle)
        // Add No-Intro article-reordered variant: "The Legend of Zelda: Ocarina of Time" -> "Legend of Zelda, The - Ocarina of Time"
        val colonNormalized = cleanTitle.replace(": ", " - ").replace(":", " -")
        titleVariants += colonNormalized
        if (colonNormalized.startsWith("The ", ignoreCase = true)) {
            val rest = colonNormalized.substring(4)
            val dashIdx = rest.indexOf(" - ")
            if (dashIdx > 0) {
                titleVariants += "${rest.substring(0, dashIdx)}, The${rest.substring(dashIdx)}"
            } else {
                titleVariants += "$rest, The"
            }
        }

        val devices = listOf("", " (Game Genie)", " (GameShark)", " (Action Replay)", " (Code Breaker)")
        if (cleanBase.isNotEmpty()) {
            for (d in devices) out += "$cleanBase$d.cht"
        }
        val regions = listOf("(USA)", "(USA, Europe)", "(World)", "(Europe)", "(Japan)")
        for (tv in titleVariants) {
            if (tv.isBlank()) continue
            for (r in regions) {
                out += "$tv $r.cht"
                out += "$tv $r (Game Genie).cht"
                out += "$tv $r (Code Breaker).cht"
                out += "$tv $r (Action Replay).cht"
                out += "$tv $r (GameShark).cht"
            }
        }
        return out.toList()
    }

    // ---- Network + Disk-Cached Discovery --------------------------------------------------------

    /**
     * Auto-finds cheat codes for [game] using the 3-tier pipeline:
     * 1) Cached / live libretro-database system catalog fuzzy match
     * 2) Direct raw GitHub filename probing (zero API rate limit)
     * 3) Built-in verified offline classics pack
     */
    suspend fun autoFind(game: Game, cacheRoot: File): CheatSearchResult = withContext(Dispatchers.IO) {
        val folders = chtFoldersFor(game.systemId)
        val indexDir = File(cacheRoot, "_index").apply { mkdirs() }

        // Tier 1: Full catalog fuzzy match (cached 7 days on disk per system)
        val allMatches = ArrayList<CheatPackMatch>()
        var networkFailed = false
        for (folder in folders) {
            val filenames = loadOrFetchSystemIndex(folder, indexDir)
            if (filenames == null) {
                networkFailed = true
                continue
            }
            allMatches += rankCandidates(game.title, game.baseName, folder, filenames)
        }
        val ranked = allMatches
            .sortedWith(compareByDescending<CheatPackMatch> { it.score }.thenBy { it.fileName.length })
            .distinctBy { "${it.folder}/${it.fileName}" }
            .take(6)

        for (match in ranked) {
            val raw = fetchText(match.rawUrl())
            if (raw != null) {
                val parsed = parseCht(raw)
                if (parsed.isNotEmpty()) {
                    return@withContext CheatSearchResult(
                        cheats = parsed,
                        activePack = match,
                        alternatePacks = ranked,
                        sourceLabel = "${match.fileName.removeSuffix(".cht")} · libretro-database",
                    )
                }
            }
        }

        // Tier 2: Direct raw.githubusercontent.com probing (no GitHub API rate limit!)
        if (folders.isNotEmpty() && (ranked.isEmpty() || networkFailed)) {
            val probes = directProbeFilenames(game.title, game.baseName).take(14)
            val primaryFolder = folders.first()
            for (candidate in probes) {
                val match = CheatPackMatch(
                    folder = primaryFolder,
                    fileName = candidate,
                    displayLabel = formatPackLabel(candidate),
                    deviceTag = extractCheatDevice(candidate),
                    score = 100,
                )
                val raw = fetchText(match.rawUrl())
                if (raw != null) {
                    val parsed = parseCht(raw)
                    if (parsed.isNotEmpty()) {
                        return@withContext CheatSearchResult(
                            cheats = parsed,
                            activePack = match,
                            alternatePacks = listOf(match),
                            sourceLabel = "${candidate.removeSuffix(".cht")} · libretro-database",
                        )
                    }
                }
            }
        }

        // Tier 3: Built-in verified offline classics pack
        val offline = findBuiltInClassics(game.systemId, game.title, game.baseName)
        if (offline != null) {
            val pack = CheatPackMatch(
                folder = "offline",
                fileName = "${offline.first}.cht",
                displayLabel = "Built-in Classics",
                deviceTag = null,
                score = 100,
                isBuiltIn = true,
            )
            return@withContext CheatSearchResult(
                cheats = offline.second,
                activePack = pack,
                alternatePacks = listOf(pack),
                sourceLabel = "${offline.first} · Built-in offline classics",
            )
        }

        CheatSearchResult(
            cheats = emptyList(),
            activePack = null,
            alternatePacks = ranked,
            error = if (folders.isEmpty()) {
                "No public cheat database is available for this console yet. You can still add codes manually."
            } else {
                "No matching cheat pack found for “${game.title}”. Check your connection or add codes manually."
            },
        )
    }

    /** Downloads and parses a specific alternate [CheatPackMatch] chosen by the user. */
    suspend fun loadPack(match: CheatPackMatch): List<Cheat> = withContext(Dispatchers.IO) {
        if (match.isBuiltIn) return@withContext emptyList()
        val raw = fetchText(match.rawUrl()) ?: return@withContext emptyList()
        parseCht(raw)
    }

    private fun loadOrFetchSystemIndex(folder: String, indexDir: File): List<String>? {
        val safeName = folder.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val cacheFile = File(indexDir, "$safeName.txt")
        if (cacheFile.isFile && (System.currentTimeMillis() - cacheFile.lastModified()) < INDEX_TTL_MS) {
            val lines = runCatching { cacheFile.readLines().filter { it.isNotBlank() } }.getOrNull()
            if (!lines.isNullOrEmpty()) return lines
        }

        val rootShas = loadOrFetchChtRootShas(indexDir) ?: return null
        val treeSha = rootShas[folder] ?: return emptyList()
        val treeJson = fetchText("https://api.github.com/repos/libretro/libretro-database/git/trees/$treeSha")
            ?: return null
        val files = runCatching {
            val tree = JSONObject(treeJson).optJSONArray("tree") ?: JSONArray()
            buildList {
                for (i in 0 until tree.length()) {
                    val item = tree.optJSONObject(i) ?: continue
                    val path = item.optString("path")
                    if (path.endsWith(".cht", ignoreCase = true)) add(path)
                }
            }
        }.getOrNull() ?: return null

        if (files.isNotEmpty()) {
            runCatching { writeAtomic(cacheFile, files.joinToString("\n")) }
        }
        return files
    }

    private fun loadOrFetchChtRootShas(indexDir: File): Map<String, String>? {
        val rootCache = File(indexDir, "_cht_root_shas.json")
        if (rootCache.isFile && (System.currentTimeMillis() - rootCache.lastModified()) < INDEX_TTL_MS) {
            val cached = runCatching {
                val obj = JSONObject(rootCache.readText())
                buildMap {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        put(k, obj.getString(k))
                    }
                }
            }.getOrNull()
            if (!cached.isNullOrEmpty()) return cached
        }

        val json = fetchText("https://api.github.com/repos/libretro/libretro-database/contents/cht")
            ?: return null
        val map = runCatching {
            val arr = JSONArray(json)
            buildMap {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    if (item.optString("type") == "dir") {
                        val name = item.optString("name")
                        val sha = item.optString("sha")
                        if (name.isNotBlank() && sha.isNotBlank()) put(name, sha)
                    }
                }
            }
        }.getOrNull() ?: return null

        if (map.isNotEmpty()) {
            runCatching {
                val obj = JSONObject()
                map.forEach { (k, v) -> obj.put(k, v) }
                writeAtomic(rootCache, obj.toString())
            }
        }
        return map
    }

    private fun fetchText(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github+json, text/plain, */*")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    internal fun encodePathSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    // ---- Tier 3: Verified Built-In Offline Classics Pack ----------------------------------------

    private data class BuiltInEntry(
        val systemId: String,
        val displayTitle: String,
        val matchKeys: List<String>,
        val cheats: List<Cheat>,
    )

    internal fun findBuiltInClassics(systemId: String, title: String, baseName: String): Pair<String, List<Cheat>>? {
        val normTitle = normalizeTitle(title)
        val normBase = normalizeTitle(baseName)
        for (entry in BUILT_IN_CLASSICS) {
            if (entry.systemId != systemId) continue
            val matched = entry.matchKeys.any { key ->
                normTitle == key || normBase == key ||
                    normTitle.startsWith("$key ") || normBase.startsWith("$key ")
            }
            if (matched) return entry.displayTitle to entry.cheats
        }
        return null
    }

    private val BUILT_IN_CLASSICS: List<BuiltInEntry> = listOf(
        BuiltInEntry(
            systemId = "nes",
            displayTitle = "Super Mario Bros.",
            matchKeys = listOf("super mario bros", "super mario brothers"),
            cheats = listOf(
                Cheat("Infinite Lives", "SXIOPO", enabled = false),
                Cheat("Super Jump (Moon Jump)", "APZLGK", enabled = false),
                Cheat("Always Stay Big Mario", "OZTLLX+AATLGZ+SZLIVO", enabled = false),
                Cheat("Start with 9 Lives", "AATOZA", enabled = false),
                Cheat("Infinite Time", "SXXPOZ", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "nes",
            displayTitle = "Super Mario Bros. 3",
            matchKeys = listOf("super mario bros 3", "super mario brothers 3"),
            cheats = listOf(
                Cheat("Infinite Lives", "SLXPLOVS", enabled = false),
                Cheat("Super Jump (Moon Jump)", "AEKPTZGA", enabled = false),
                Cheat("Start and Stay as Super Mario", "XUKXGLIE", enabled = false),
                Cheat("Start and Stay as Raccoon Mario", "UXKXGLIE", enabled = false),
                Cheat("Infinite Time", "SZLGEZVG", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "nes",
            displayTitle = "The Legend of Zelda",
            matchKeys = listOf("legend of zelda"),
            cheats = listOf(
                Cheat("Infinite Health (Invincible)", "AVVLAUSZ", enabled = false),
                Cheat("Infinite Rupees (Everything Free)", "SZVXYLSA", enabled = false),
                Cheat("Infinite Bombs", "SZKZOZVG", enabled = false),
                Cheat("Infinite Keys", "SZNZLAVG", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "nes",
            displayTitle = "Contra",
            matchKeys = listOf("contra", "probotector"),
            cheats = listOf(
                Cheat("Infinite Lives", "SLAIUZ", enabled = false),
                Cheat("Start with 30 Lives", "GZLAAI", enabled = false),
                Cheat("Keep Weapon After Losing a Life", "GXIIAA", enabled = false),
                Cheat("Start with Spread Gun", "LAAXZG", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "nes",
            displayTitle = "Mega Man 2",
            matchKeys = listOf("mega man 2", "megaman 2", "rockman 2"),
            cheats = listOf(
                Cheat("Infinite Energy", "SXKAAZVG", enabled = false),
                Cheat("Infinite Lives", "SXXGAAVG", enabled = false),
                Cheat("Infinite Weapon Energy", "GXXAIOVG", enabled = false),
                Cheat("Mega Jump", "NYUGKUGE", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "snes",
            displayTitle = "Super Mario World",
            matchKeys = listOf("super mario world"),
            cheats = listOf(
                Cheat("Infinite 99 Lives", "7E0DBE63", enabled = false),
                Cheat("Infinite 99 Coins", "7E0DBF63", enabled = false),
                Cheat("Always 999 Time", "7E0F3109+7E0F3209+7E0F3309", enabled = false),
                Cheat("Always Cape Mario", "7E001902", enabled = false),
                Cheat("Always Fire Mario", "7E001903", enabled = false),
                Cheat("Moon Jump (Hold B)", "7E007D00", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "snes",
            displayTitle = "The Legend of Zelda: A Link to the Past",
            matchKeys = listOf("legend of zelda a link to the past", "zelda a link to the past"),
            cheats = listOf(
                Cheat("Infinite Health", "7EF36DA0", enabled = false),
                Cheat("Infinite Magic", "7EF36E80", enabled = false),
                Cheat("Infinite 999 Rupees", "7EF360E7+7EF36103", enabled = false),
                Cheat("Infinite Bombs", "7EF34363", enabled = false),
                Cheat("Infinite Arrows", "7EF37763", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "snes",
            displayTitle = "Super Metroid",
            matchKeys = listOf("super metroid"),
            cheats = listOf(
                Cheat("Infinite Energy", "7E09C2E7+7E09C303", enabled = false),
                Cheat("Infinite Missiles", "7E09C663", enabled = false),
                Cheat("Infinite Super Missiles", "7E09CA32", enabled = false),
                Cheat("Infinite Power Bombs", "7E09CE32", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "snes",
            displayTitle = "Chrono Trigger",
            matchKeys = listOf("chrono trigger"),
            cheats = listOf(
                Cheat("Infinite Gold", "7E2C53FF+7E2C54FF+7E2C550F", enabled = false),
                Cheat("Crono Infinite HP", "7E2603E7+7E260403", enabled = false),
                Cheat("Crono Infinite MP", "7E260763", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "snes",
            displayTitle = "Donkey Kong Country",
            matchKeys = listOf("donkey kong country"),
            cheats = listOf(
                Cheat("Infinite Lives", "7E057563", enabled = false),
                Cheat("Infinite Bananas", "7E052963", enabled = false),
                Cheat("Invincibility", "7E16C501", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "snes",
            displayTitle = "Mega Man X",
            matchKeys = listOf("mega man x", "megaman x", "rockman x"),
            cheats = listOf(
                Cheat("Infinite Health", "7E0BCF20", enabled = false),
                Cheat("Infinite Lives", "7E1F8009", enabled = false),
                Cheat("All Armor Upgrades", "7E1F99FF", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "gb",
            displayTitle = "Pokémon Red / Blue",
            matchKeys = listOf("pokemon red", "pokemon blue", "pokemon red version", "pokemon blue version"),
            cheats = listOf(
                Cheat("Infinite Money", "019947D3+019948D3+019949D3", enabled = false),
                Cheat("Infinite Master Balls (1st Slot)", "01011ED3+01631FD3", enabled = false),
                Cheat("Infinite Rare Candies (1st Slot)", "01281ED3+01631FD3", enabled = false),
                Cheat("Walk Through Walls", "010138CD", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "gbc",
            displayTitle = "Pokémon Gold / Silver / Crystal",
            matchKeys = listOf("pokemon gold", "pokemon silver", "pokemon crystal", "pokemon crystal version"),
            cheats = listOf(
                Cheat("Infinite Money (Crystal)", "010F4ED8+01424FD8+013F50D8", enabled = false),
                Cheat("Infinite Money (Gold/Silver)", "010F73D5+014274D5+013F75D5", enabled = false),
                Cheat("Max Rare Candies (Gold/Silver)", "0120B8D5+0163B9D5", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "gba",
            displayTitle = "Pokémon FireRed / LeafGreen",
            matchKeys = listOf("pokemon firered", "pokemon fire red", "pokemon leafgreen", "pokemon leaf green"),
            cheats = listOf(
                Cheat("Infinite Money", "820257BC+423F+820257BE+000F", enabled = false),
                Cheat("Rare Candies in PC", "82025840+0044", enabled = false),
                Cheat("Master Balls in PC", "82025840+0001", enabled = false),
                Cheat("Walk Through Walls", "509197D3+542975F4+78DA95DF+44018CB4", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "gba",
            displayTitle = "Pokémon Emerald",
            matchKeys = listOf("pokemon emerald", "pokemon emerald version"),
            cheats = listOf(
                Cheat("Master Code (Must Be On)", "D8BAE4D9+4864DCE5+A86CDBA5+19BA49B3", enabled = false),
                Cheat("Infinite Money", "C051CCF6+975E8DA1", enabled = false),
                Cheat("Rare Candies in PC", "BFF956FA+2F9EC50D", enabled = false),
                Cheat("Walk Through Walls", "7881A409+E2026E0C+8E883EFF+92E9660D", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "gba",
            displayTitle = "Metroid Fusion",
            matchKeys = listOf("metroid fusion"),
            cheats = listOf(
                Cheat("Infinite Health", "83001310+0833", enabled = false),
                Cheat("Infinite Missiles", "83001314+00FA", enabled = false),
                Cheat("Infinite Power Bombs", "33001318+004A", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "n64",
            displayTitle = "Super Mario 64",
            matchKeys = listOf("super mario 64"),
            cheats = listOf(
                Cheat("Infinite Lives", "8033B21D 0063", enabled = false),
                Cheat("Infinite Health", "8133B21E 08FF", enabled = false),
                Cheat("All 120 Stars", "8133B218 0078", enabled = false),
                Cheat("Moon Jump (Press L)", "D033AFA1 0020+8133B1BC 4220", enabled = false),
                Cheat("Giant Mario", "810F0890 3F90", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "n64",
            displayTitle = "The Legend of Zelda: Ocarina of Time",
            matchKeys = listOf("legend of zelda ocarina of time", "zelda ocarina of time"),
            cheats = listOf(
                Cheat("Infinite Health", "8111A600 0140", enabled = false),
                Cheat("Infinite Magic", "8011A603 0060", enabled = false),
                Cheat("Infinite 500 Rupees", "8111A604 01F4", enabled = false),
                Cheat("Moon Jump (Hold L)", "D01C84B5 0020+811DAA90 40CB", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "genesis",
            displayTitle = "Sonic the Hedgehog",
            matchKeys = listOf("sonic the hedgehog"),
            cheats = listOf(
                Cheat("Infinite Lives", "AJVA-AA4R", enabled = false),
                Cheat("Keep Rings When Hit", "ACLT-AA64", enabled = false),
                Cheat("Super Jump", "ACNA-CA54", enabled = false),
                Cheat("Level Select Enabled", "AAEA-AA7Y", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "genesis",
            displayTitle = "Sonic the Hedgehog 2",
            matchKeys = listOf("sonic the hedgehog 2", "sonic 2"),
            cheats = listOf(
                Cheat("Infinite Lives", "AJ4T-AA5T", enabled = false),
                Cheat("Invincibility", "RHCA-A6Z4", enabled = false),
                Cheat("Infinite Rings", "ATBT-AA32", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "psx",
            displayTitle = "Castlevania: Symphony of the Night",
            matchKeys = listOf("castlevania symphony of the night"),
            cheats = listOf(
                Cheat("Infinite Health", "80097BA0 03E7", enabled = false),
                Cheat("Infinite Hearts", "80097BA8 03E7", enabled = false),
                Cheat("Infinite Magic", "80097BB0 03E7", enabled = false),
                Cheat("Max Gold", "80097BF0 FFFF", enabled = false),
            ),
        ),
        BuiltInEntry(
            systemId = "psx",
            displayTitle = "Final Fantasy VII",
            matchKeys = listOf("final fantasy 7", "final fantasy vii"),
            cheats = listOf(
                Cheat("Max Gil", "8009D260 967F+8009D262 0098", enabled = false),
                Cheat("Cloud Infinite HP in Battle", "800F83FC 270F", enabled = false),
                Cheat("Cloud Infinite MP in Battle", "800F8400 03E7", enabled = false),
                Cheat("Instant Limit Break (Cloud)", "800F5E6A 00FF", enabled = false),
            ),
        ),
    )
}
