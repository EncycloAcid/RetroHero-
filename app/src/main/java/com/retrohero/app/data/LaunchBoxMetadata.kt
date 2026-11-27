package com.retrohero.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.zip.ZipInputStream

/** One classified artwork/screenshot asset from LaunchBox Games Database (`images.launchbox-app.com`). */
data class LaunchBoxImage(
    val url: String,
    val type: String,
    val region: String = "",
)

/** Parsed LaunchBox Games Database record (from either local `Metadata.zip` index or live `gamesdb.launchbox-app.com`). */
data class LaunchBoxGameRecord(
    val databaseId: String,
    val name: String,
    val platform: String,
    val overview: String = "",
    val developer: String = "",
    val publisher: String = "",
    val releaseDate: String = "",
    val releaseYear: String = "",
    val genres: List<String> = emptyList(),
    val esrb: String = "",
    val maxPlayers: Int? = null,
    val cooperative: Boolean = false,
    val communityRating: Float? = null,
    val wikipediaUrl: String = "",
    val videoUrl: String = "",
    val images: List<LaunchBoxImage> = emptyList(),
)

/** Storefront & 3D physical case/media URLs selected from a [LaunchBoxGameRecord]. */
data class LaunchBoxStorefrontMedia(
    val heroBannerUrl: String?,
    val boxArtUrl: String?,
    val clearLogoUrl: String?,
    val titleScreenUrl: String?,
    val screenshotUrls: List<String>,
    val boxBackUrl: String? = null,
    val boxSpineUrl: String? = null,
    val boxFullUrl: String? = null,
    val cartFrontUrl: String? = null,
    val cartBackUrl: String? = null,
    val discUrl: String? = null,
)

/** Status of the optional local offline LaunchBox `Metadata.zip` database index. */
data class LaunchBoxDbStatus(
    val synced: Boolean,
    val gameCount: Int = 0,
    val platformCount: Int = 0,
    val updatedAtMs: Long = 0L,
)

/**
 * LaunchBox Games Database metadata & storefront media provider.
 *
 * Supports two complementary, zero-API-key paths:
 * 1. **Local offline index** streamed from LaunchBox's official `https://gamesdb.launchbox-app.com/Metadata.zip`
 *    into compact per-platform JSONL files (instant <5 ms offline lookups).
 * 2. **Live web lookup** against `https://gamesdb.launchbox-app.com/games/results/...` and
 *    `/games/details/...` + `/games/images/...` so every Game Page gets authentic LaunchBox
 *    metadata and high-resolution storefront art immediately out of the box.
 */
object LaunchBoxMetadata {
    const val METADATA_ZIP_URL = "https://gamesdb.launchbox-app.com/Metadata.zip"
    const val IMAGES_BASE_URL = "https://images.launchbox-app.com/"
    private const val GAMESDB_BASE_URL = "https://gamesdb.launchbox-app.com"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) RetroHero/1.0"
    private const val CONNECT_TIMEOUT_MS = 9_000
    private const val READ_TIMEOUT_MS = 14_000

    /** Official LaunchBox platform names for every RetroHero system ID, primary first. */
    fun platformNamesFor(systemId: String): List<String> = when (systemId) {
        // Nintendo
        "nes" -> listOf("Nintendo Entertainment System", "Nintendo Famicom Disk System")
        "snes" -> listOf("Super Nintendo Entertainment System", "Nintendo Satellaview")
        "n64" -> listOf("Nintendo 64")
        "gb" -> listOf("Nintendo Game Boy")
        "gbc" -> listOf("Nintendo Game Boy Color", "Nintendo Game Boy")
        "gba" -> listOf("Nintendo Game Boy Advance")
        "nds" -> listOf("Nintendo DS")
        "3ds" -> listOf("Nintendo 3DS")
        "gc" -> listOf("Nintendo GameCube")
        "wii" -> listOf("Nintendo Wii")
        "wiiu" -> listOf("Nintendo Wii U")
        "switch" -> listOf("Nintendo Switch")
        "vb" -> listOf("Nintendo Virtual Boy")
        "pokemini" -> listOf("Nintendo Pokemon Mini")
        "gw" -> listOf("Nintendo Game & Watch")

        // Sega
        "genesis" -> listOf("Sega Genesis", "Sega Mega Drive")
        "sms" -> listOf("Sega Master System")
        "gg" -> listOf("Sega Game Gear")
        "sg1000" -> listOf("Sega SG-1000")
        "segacd" -> listOf("Sega CD")
        "32x" -> listOf("Sega 32X")
        "saturn" -> listOf("Sega Saturn")
        "dreamcast" -> listOf("Sega Dreamcast")
        "naomi" -> listOf("Sega Naomi", "Sammy Atomiswave")

        // Sony
        "psx" -> listOf("Sony Playstation", "Sony PlayStation")
        "ps2" -> listOf("Sony Playstation 2", "Sony PlayStation 2")
        "psp" -> listOf("Sony PSP")
        "psvita" -> listOf("Sony Playstation Vita", "Sony PlayStation Vita")

        // NEC
        "pce" -> listOf("NEC TurboGrafx-16", "PC Engine")
        "supergrafx" -> listOf("PC Engine SuperGrafx", "NEC TurboGrafx-16")
        "pcecd" -> listOf("NEC TurboGrafx-CD")
        "pcfx" -> listOf("NEC PC-FX")

        // SNK & Bandai
        "ngp" -> listOf("SNK Neo Geo Pocket Color", "SNK Neo Geo Pocket")
        "neogeo" -> listOf("SNK Neo Geo AES", "SNK Neo Geo MVS", "Arcade")
        "neocd" -> listOf("SNK Neo Geo CD")
        "ws" -> listOf("WonderSwan Color", "WonderSwan")

        // Atari
        "lynx" -> listOf("Atari Lynx")
        "atari2600" -> listOf("Atari 2600")
        "atari5200" -> listOf("Atari 5200")
        "atari7800" -> listOf("Atari 7800")
        "jaguar" -> listOf("Atari Jaguar")
        "atarist" -> listOf("Atari ST")

        // Classic Consoles, Home Computers & Arcade
        "coleco" -> listOf("ColecoVision")
        "intellivision" -> listOf("Mattel Intellivision")
        "vectrex" -> listOf("GCE Vectrex")
        "odyssey2" -> listOf("Magnavox Odyssey 2")
        "3do" -> listOf("3DO Interactive Multiplayer")
        "cdi" -> listOf("Philips CD-i")
        "supervision" -> listOf("Watara Supervision")
        "channelf" -> listOf("Fairchild Channel F")
        "msx" -> listOf("Microsoft MSX2", "Microsoft MSX")
        "c64" -> listOf("Commodore 64")
        "amiga" -> listOf("Commodore Amiga")
        "zxspectrum" -> listOf("Sinclair ZX Spectrum")
        "amstradcpc" -> listOf("Amstrad CPC")
        "x68000" -> listOf("Sharp X68000")
        "pc98" -> listOf("NEC PC-9801")
        "dos" -> listOf("MS-DOS")
        "scummvm" -> listOf("ScummVM", "MS-DOS")
        "pico8" -> listOf("PICO-8")
        "arcade" -> listOf("Arcade")
        else -> emptyList()
    }

    /** Set of all official LaunchBox platform names tracked by RetroHero (lowercase for fast XML filtering). */
    val supportedPlatformNamesLower: Set<String> by lazy {
        Systems.all.flatMap { platformNamesFor(it.id) }.map { it.lowercase(Locale.US) }.toSet()
    }

    fun imageUrl(fileName: String): String {
        val trimmed = fileName.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return IMAGES_BASE_URL + trimmed.removePrefix("/")
    }

    // ---- Storefront Media Selection -------------------------------------------------------------

    private val BOX_FRONT_PRIORITY = listOf(
        "Box - Front",
        "Box - Front - Reconstructed",
        "Fanart - Box - Front",
        "Box - 3D",
        "Steam Poster",
        "Amazon Poster",
    )

    private val BOX_BACK_PRIORITY = listOf(
        "Box - Back",
        "Box - Back - Reconstructed",
        "Fanart - Box - Back",
    )

    private val BOX_SPINE_PRIORITY = listOf(
        "Box - Spine",
    )

    private val BOX_FULL_PRIORITY = listOf(
        "Box - Full",
    )

    private val CART_FRONT_PRIORITY = listOf(
        "Cart - Front",
        "Cart - 3D",
    )

    private val CART_BACK_PRIORITY = listOf(
        "Cart - Back",
    )

    private val DISC_PRIORITY = listOf(
        "Disc",
        "Fanart - Disc",
    )

    private val HERO_BANNER_PRIORITY = listOf(
        "Fanart - Background",
        "Banner",
        "Steam Banner",
        "Arcade - Marquee",
        "Box - Front",
        "Fanart - Box - Front",
    )

    internal fun preferredRegionKeywords(romBaseName: String): List<String> {
        val lower = romBaseName.lowercase(Locale.US)
        // All region tags together, e.g. "(Japan, USA) (En)" -> "japan, usa,en".
        val tags = Regex("""\(([^)]*)\)""").findAll(lower).joinToString(",") { it.groupValues[1] }
        val regions = tags.split(',').map { it.trim() }.toSet()
        val northAmerica = listOf("north america", "united states", "world", "europe")
        return when {
            // A release that includes the USA (or is worldwide) gets North American art, even if Japan is listed first.
            regions.any { it == "usa" || it == "u" || it == "world" || it == "canada" } -> northAmerica
            regions.any { it == "europe" || it == "e" || it == "uk" || it in EUROPEAN_COUNTRIES } ->
                listOf("europe", "united kingdom", "world", "north america", "united states")
            regions.any { it == "japan" || it == "j" } -> listOf("japan", "world", "north america", "united states")
            else -> northAmerica
        }
    }

    private val EUROPEAN_COUNTRIES = setOf("germany", "france", "spain", "italy", "netherlands", "sweden", "australia")

    /** Picks the best storefront hero banner, 3D case faces (front/back/spine/full), cart/disc scans, and screenshots. */
    fun selectStorefrontMedia(images: List<LaunchBoxImage>, romBaseName: String = ""): LaunchBoxStorefrontMedia {
        if (images.isEmpty()) {
            return LaunchBoxStorefrontMedia(null, null, null, null, emptyList())
        }
        val regionOrder = preferredRegionKeywords(romBaseName)

        fun pickBest(types: List<String>): String? {
            for (targetType in types) {
                val matches = images.filter { it.type.equals(targetType, ignoreCase = true) }
                if (matches.isEmpty()) continue
                val best = matches.minByOrNull { img ->
                    val r = img.region.lowercase(Locale.US)
                    val idx = regionOrder.indexOfFirst { it in r }
                    if (idx >= 0) idx else if (r.isBlank()) regionOrder.size else regionOrder.size + 1
                }
                if (best != null) return best.url
            }
            return null
        }

        val boxArt = pickBest(BOX_FRONT_PRIORITY)
        val boxBack = pickBest(BOX_BACK_PRIORITY)
        val boxSpine = pickBest(BOX_SPINE_PRIORITY)
        val boxFull = pickBest(BOX_FULL_PRIORITY)
        val cartFront = pickBest(CART_FRONT_PRIORITY)
        val cartBack = pickBest(CART_BACK_PRIORITY)
        val disc = pickBest(DISC_PRIORITY)
        val heroBanner = pickBest(HERO_BANNER_PRIORITY) ?: boxArt
        val clearLogo = pickBest(listOf("Clear Logo"))
        val titleScreen = pickBest(listOf("Screenshot - Game Title"))

        val gameplay = images
            .filter { it.type.equals("Screenshot - Gameplay", ignoreCase = true) }
            .map { it.url }
            .distinct()
        val extraScreens = if (gameplay.size < 3) {
            images.filter {
                it.type.equals("Screenshot - Game Select", ignoreCase = true) ||
                    it.type.equals("Screenshot - High Scores", ignoreCase = true)
            }.map { it.url }.distinct()
        } else emptyList()

        return LaunchBoxStorefrontMedia(
            heroBannerUrl = heroBanner,
            boxArtUrl = boxArt,
            clearLogoUrl = clearLogo,
            titleScreenUrl = titleScreen,
            screenshotUrls = (gameplay + extraScreens).distinct().take(6),
            boxBackUrl = boxBack,
            boxSpineUrl = boxSpine,
            boxFullUrl = boxFull,
            cartFrontUrl = cartFront,
            cartBackUrl = cartBack,
            discUrl = disc,
        )
    }

    // ---- Convert LaunchBox Record -> RetroHero GameDossier --------------------------------------

    fun toGameDossier(game: Game, record: LaunchBoxGameRecord, fallback: GameDossier): GameDossier {
        val media = selectStorefrontMedia(record.images, game.baseName)
        val cleanOverview = cleanText(record.overview)
        val summary = if (cleanOverview.length >= 28) cleanOverview else fallback.summary
        val tagline = firstSentence(summary).takeIf { it.length >= 20 } ?: fallback.tagline

        val developer = cleanText(record.developer).ifBlank { fallback.developer }
        val publisher = cleanText(record.publisher).ifBlank { developer.ifBlank { fallback.publisher } }
        val formattedDate = formatLaunchBoxDate(record.releaseDate, record.releaseYear)
        val year = Regex("""\b(19[789]\d|20[012]\d)\b""").find(formattedDate)?.groupValues?.get(1)
            ?: record.releaseYear.takeIf { it.length == 4 }
            ?: fallback.releaseYear

        val genreStr = record.genres
            .map { cleanText(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
            .joinToString(" · ")
            .ifBlank { fallback.genre }

        val modesStr = buildModesString(record.maxPlayers, record.cooperative).ifBlank { fallback.modes }
        val esrbClean = cleanText(record.esrb)
            .takeIf { it.isNotBlank() && !it.equals("Not Rated", ignoreCase = true) }
            .orEmpty()

        val resolvedSeries = GameSeries.resolve(
            gameTitle = record.name.ifBlank { game.title },
            systemId = game.systemId,
            wikiSeries = "",
            wikiPrecededBy = "",
            wikiFollowedBy = "",
            wikiExtract = summary,
        )

        val sections = buildFeatureBlocks(record.name.ifBlank { game.title }, summary, fallback)

        return GameDossier(
            title = record.name.ifBlank { game.title },
            tagline = tagline,
            developer = developer,
            publisher = publisher,
            releaseDate = formattedDate.ifBlank { fallback.releaseDate },
            releaseYear = year,
            genre = genreStr,
            modes = modesStr,
            summary = summary,
            featureSections = sections,
            promoQuote = "${genreStr.split(" · ").firstOrNull()?.uppercase(Locale.US) ?: "CLASSIC"} · $year",
            radarAxes = GameDossier.computeGameDna(
                genre = genreStr,
                modes = modesStr,
                title = record.name.ifBlank { game.title },
                summary = summary,
                systemId = game.systemId,
            ),
            heroImageUrl = media.heroBannerUrl,
            sourceLabel = "LaunchBox",
            isLive = true,
            seriesName = resolvedSeries.seriesName,
            relatedGames = resolvedSeries.entries,
            boxArtUrl = media.boxArtUrl,
            boxBackUrl = media.boxBackUrl,
            boxSpineUrl = media.boxSpineUrl,
            boxFullUrl = media.boxFullUrl,
            cartFrontUrl = media.cartFrontUrl,
            cartBackUrl = media.cartBackUrl,
            discUrl = media.discUrl,
            clearLogoUrl = media.clearLogoUrl,
            titleScreenUrl = media.titleScreenUrl,
            screenshotUrls = media.screenshotUrls,
            esrbRating = esrbClean,
            communityRating = record.communityRating?.takeIf { it in 0.5f..5.0f },
        )
    }

    private fun buildModesString(maxPlayers: Int?, cooperative: Boolean): String = buildList {
        when {
            maxPlayers == null || maxPlayers <= 0 -> Unit
            maxPlayers == 1 -> add("Single-player")
            else -> add("1–$maxPlayers players")
        }
        if (cooperative) add("Co-op")
    }.joinToString(" · ")

    private fun buildFeatureBlocks(title: String, overview: String, fallback: GameDossier): List<GameDossier.FeatureBlock> {
        val paragraphs = overview
            .split(Regex("""\n\s*\n"""))
            .map { it.trim() }
            .filter { it.length >= 40 }
        if (paragraphs.size >= 2) {
            return paragraphs.drop(1).take(2).mapIndexed { i, p ->
                GameDossier.FeatureBlock(
                    headline = if (i == 0) "OVERVIEW" else "GAMEPLAY",
                    body = p,
                )
            }
        }
        return fallback.featureSections.map {
            it.copy(body = it.body.replace(fallback.title, title))
        }
    }

    private fun formatLaunchBoxDate(rawDate: String, rawYear: String): String {
        val trimmed = cleanText(rawDate)
        if (trimmed.isBlank()) return cleanText(rawYear)
        // ISO format from LaunchBox Metadata.xml: "2008-09-13T00:00:00-05:00" or "2008-09-13"
        val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(trimmed)
        if (iso != null) {
            val y = iso.groupValues[1]
            val m = iso.groupValues[2].toIntOrNull() ?: 1
            val d = iso.groupValues[3].toIntOrNull() ?: 1
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December",
            )
            val mName = monthNames.getOrNull(m - 1) ?: return y
            return "$mName $d, $y"
        }
        return trimmed
    }

    private fun firstSentence(text: String): String {
        val clean = text.replace(Regex("""\s+"""), " ").trim()
        val end = Regex("""[.!?](?:\s|$)""").find(clean)?.range?.first
        return if (end != null && end in 20..220) clean.substring(0, end + 1) else clean.take(180)
    }

    private fun cleanText(raw: String): String = raw
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("""\r\n?"""), "\n")
        .trim()

    // ---- Title Normalization & Strict Platform Matching -----------------------------------------

    private val NON_ALNUM = Regex("""[^a-z0-9\s]""")
    private val PAREN_TAG = Regex("""\([^)]*\)""")
    private val BRACKET_TAG = Regex("""\[[^\]]*\]""")
    private val ROMAN_MAP = mapOf(
        "ii" to "2", "iii" to "3", "iv" to "4", "vi" to "6",
        "vii" to "7", "viii" to "8", "ix" to "9", "xi" to "11", "xii" to "12",
    )

    fun normalizeTitle(raw: String): String {
        var s = raw
            .replace("é", "e", ignoreCase = true)
            .replace("É", "e")
            .replace("’", "'")
            .replace("&", " and ")
            .replace("+", " ")
        s = PAREN_TAG.replace(s, " ")
        s = BRACKET_TAG.replace(s, " ")
        s = s.replace(Regex("""(?i),\s*(the|a|an)(?=\s*[-:]|\s*$)"""), "")
        s = s.lowercase(Locale.US)
        s = s.replace(Regex("""^(the|a|an)\s+"""), "")
        s = NON_ALNUM.replace(s, " ")
        return s.trim()
            .split(Regex("""\s+"""))
            .filter { it.isNotEmpty() }
            .map { ROMAN_MAP[it] ?: it }
            .joinToString(" ")
    }

    private fun extractNumbers(norm: String): Set<String> =
        norm.split(' ').filter { it.isNotEmpty() && it.all(Char::isDigit) }.toSet()

    internal fun scoreTitleMatch(queryTitle: String, candidateTitle: String): Int {
        val target = normalizeTitle(queryTitle)
        val cand = normalizeTitle(candidateTitle)
        if (target.isEmpty() || cand.isEmpty()) return 0
        if (target == cand) return 120

        // Handle Pokémon "Version" suffix cleanly (e.g. "Pokemon Platinum Version" <-> "Pokemon Platinum")
        val targetNoVer = target.replace(Regex("""\bversion\b"""), "").replace(Regex("""\s+"""), " ").trim()
        val candNoVer = cand.replace(Regex("""\bversion\b"""), "").replace(Regex("""\s+"""), " ").trim()
        if (targetNoVer.isNotEmpty() && targetNoVer == candNoVer) return 112

        val tTokens = targetNoVer.split(' ').filter { it.isNotEmpty() }.toSet()
        val cTokens = candNoVer.split(' ').filter { it.isNotEmpty() }.toSet()
        val overlap = tTokens.intersect(cTokens).size
        if (overlap == 0) return 0
        val precision = overlap.toDouble() / cTokens.size.coerceAtLeast(1)
        val recall = overlap.toDouble() / tTokens.size.coerceAtLeast(1)
        val f1 = (2.0 * precision * recall) / (precision + recall)
        var score = (f1 * 95).toInt()
        if (extractNumbers(targetNoVer) != extractNumbers(candNoVer)) score -= 45
        return score.coerceAtLeast(0)
    }

    // ---- Main Unified Lookup (Local Offline DB -> Live LaunchBox GamesDB Web) -------------------

    fun lookup(game: Game, dbDir: File): LaunchBoxGameRecord? {
        val platforms = platformNamesFor(game.systemId)
        if (platforms.isEmpty()) return null

        // Path 1: Instant local lookup if the user synced LaunchBox Metadata.zip
        lookupLocalIndex(game.title, platforms, dbDir)?.let { return it }

        // Path 2: Live lookup against gamesdb.launchbox-app.com (no API key or sync required!)
        return lookupLiveWeb(game.title, platforms)
    }

    // ---- Path 1: Local LaunchBox Metadata.zip Streaming Indexer & Fast Reader -------------------

    fun dbStatus(dbDir: File): LaunchBoxDbStatus {
        val manifest = File(dbDir, "manifest.json")
        if (!manifest.isFile) return LaunchBoxDbStatus(synced = false)
        return runCatching {
            val o = JSONObject(manifest.readText())
            LaunchBoxDbStatus(
                synced = o.optInt("gameCount", 0) > 0,
                gameCount = o.optInt("gameCount", 0),
                platformCount = o.optInt("platformCount", 0),
                updatedAtMs = o.optLong("updatedAtMs", 0L),
            )
        }.getOrDefault(LaunchBoxDbStatus(synced = false))
    }

    private fun platformIndexFile(dbDir: File, platformName: String): File {
        val safe = platformName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return File(dbDir, "plat_$safe.jsonl")
    }

    /**
     * Downloads and streams LaunchBox's official `Metadata.zip` into compact per-platform JSONL indexes
     * containing only RetroHero-supported consoles (zero full-XML RAM buffering).
     */
    suspend fun syncOfflineDatabase(
        dbDir: File,
        onProgress: (String) -> Unit = {},
    ): LaunchBoxDbStatus = withContext(Dispatchers.IO) {
        onProgress("Downloading LaunchBox Metadata.zip…")
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(METADATA_ZIP_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("LaunchBox server returned HTTP ${conn.responseCode}")
            }
            val tmpZip = File(dbDir.parentFile ?: dbDir, "launchbox_metadata.tmp.zip")
            tmpZip.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                tmpZip.outputStream().use { out ->
                    val buf = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        total += n
                        if (total % (2 * 1024 * 1024) < 32 * 1024) {
                            onProgress("Downloading LaunchBox DB (${total / (1024 * 1024)} MB)…")
                        }
                    }
                }
            }
            onProgress("Indexing supported retro consoles…")
            val status = indexMetadataZipFile(tmpZip, dbDir, onProgress)
            tmpZip.delete()
            status
        } finally {
            conn?.disconnect()
        }
    }

    /** Streams a local LaunchBox `Metadata.zip` into compact per-platform JSONL files. */
    fun indexMetadataZipFile(
        zipFile: File,
        dbDir: File,
        onProgress: (String) -> Unit = {},
    ): LaunchBoxDbStatus {
        dbDir.mkdirs()
        // First pass: index <Game> entries for supported platforms and <GameAlternateName>
        val gamesById = HashMap<String, JSONObject>(32_768)
        val gamesByPlatform = HashMap<String, MutableList<String>>()
        val imagesById = HashMap<String, MutableList<JSONObject>>()

        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("Metadata.xml", ignoreCase = true)) {
                    parseMetadataXmlStream(
                        stream = zis,
                        allowedPlatformsLower = supportedPlatformNamesLower,
                        onGame = { id, platform, obj ->
                            gamesById[id] = obj
                            gamesByPlatform.getOrPut(platform) { ArrayList() }.add(id)
                            if (gamesById.size % 5000 == 0) {
                                onProgress("Indexed ${gamesById.size} games…")
                            }
                        },
                        onImage = { id, imgObj ->
                            if (id in gamesById) {
                                val list = imagesById.getOrPut(id) { ArrayList(10) }
                                if (list.size < 32) list += imgObj
                            }
                        },
                    )
                    break
                }
                entry = zis.nextEntry
            }
        }

        // Write compact per-platform JSONL files atomically
        var writtenGames = 0
        for ((platform, ids) in gamesByPlatform) {
            val outFile = platformIndexFile(dbDir, platform)
            val tmpFile = File(dbDir, outFile.name + ".tmp")
            tmpFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (id in ids) {
                    val obj = gamesById[id] ?: continue
                    val imgs = imagesById[id]
                    if (!imgs.isNullOrEmpty()) {
                        val arr = JSONArray()
                        imgs.forEach { arr.put(it) }
                        obj.put("images", arr)
                    }
                    writer.write(obj.toString())
                    writer.newLine()
                    writtenGames++
                }
            }
            if (!tmpFile.renameTo(outFile)) {
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }
        }

        val status = LaunchBoxDbStatus(
            synced = writtenGames > 0,
            gameCount = writtenGames,
            platformCount = gamesByPlatform.size,
            updatedAtMs = System.currentTimeMillis(),
        )
        writeAtomic(
            File(dbDir, "manifest.json"),
            JSONObject()
                .put("gameCount", status.gameCount)
                .put("platformCount", status.platformCount)
                .put("updatedAtMs", status.updatedAtMs)
                .toString(),
        )
        return status
    }

    /** Streaming pull-parser for LaunchBox `Metadata.xml` (<Game> and <GameImage> records). */
    internal fun parseMetadataXmlStream(
        stream: InputStream,
        allowedPlatformsLower: Set<String>,
        onGame: (databaseId: String, platform: String, json: JSONObject) -> Unit,
        onImage: (databaseId: String, imgJson: JSONObject) -> Unit,
    ) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var currentRecord: String? = null
        var currentTag: String? = null
        val fields = HashMap<String, String>(16)

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (currentRecord == null && (name == "Game" || name == "GameImage")) {
                        currentRecord = name
                        fields.clear()
                    } else if (currentRecord != null) {
                        currentTag = name
                    }
                }
                XmlPullParser.TEXT -> {
                    val tag = currentTag
                    if (currentRecord != null && tag != null) {
                        val txt = parser.text
                        if (!txt.isNullOrEmpty()) {
                            fields[tag] = (fields[tag] ?: "") + txt
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name == currentTag) {
                        currentTag = null
                    } else if (name == currentRecord) {
                        if (currentRecord == "Game") {
                            val id = fields["DatabaseID"]?.trim().orEmpty()
                            val platform = fields["Platform"]?.trim().orEmpty()
                            val gameName = fields["Name"]?.trim().orEmpty()
                            if (id.isNotEmpty() && gameName.isNotEmpty() &&
                                (allowedPlatformsLower.isEmpty() || platform.lowercase(Locale.US) in allowedPlatformsLower)
                            ) {
                                val obj = JSONObject()
                                    .put("id", id)
                                    .put("n", gameName)
                                    .put("norm", normalizeTitle(gameName))
                                    .put("p", platform)
                                    .put("o", fields["Overview"]?.trim().orEmpty())
                                    .put("d", fields["Developer"]?.trim().orEmpty())
                                    .put("pub", fields["Publisher"]?.trim().orEmpty())
                                    .put("rd", fields["ReleaseDate"]?.trim().orEmpty())
                                    .put("ry", fields["ReleaseYear"]?.trim().orEmpty())
                                    .put("g", fields["Genres"]?.trim().orEmpty())
                                    .put("e", fields["ESRB"]?.trim().orEmpty())
                                    .put("mp", fields["MaxPlayers"]?.trim()?.toIntOrNull() ?: 0)
                                    .put("coop", fields["Cooperative"]?.trim().equals("true", ignoreCase = true))
                                    .put("cr", fields["CommunityRating"]?.trim()?.toDoubleOrNull() ?: -1.0)
                                    .put("w", fields["WikipediaURL"]?.trim().orEmpty())
                                    .put("v", fields["VideoURL"]?.trim().orEmpty())
                                onGame(id, platform, obj)
                            }
                        } else if (currentRecord == "GameImage") {
                            val id = fields["DatabaseID"]?.trim().orEmpty()
                            val fileName = fields["FileName"]?.trim().orEmpty()
                            val type = fields["Type"]?.trim().orEmpty()
                            if (id.isNotEmpty() && fileName.isNotEmpty() && type.isNotEmpty() && isUsefulImageType(type)) {
                                onImage(
                                    id,
                                    JSONObject()
                                        .put("f", fileName)
                                        .put("t", type)
                                        .put("r", fields["Region"]?.trim().orEmpty()),
                                )
                            }
                        }
                        currentRecord = null
                        fields.clear()
                    }
                }
            }
            event = parser.next()
        }
    }

    private fun isUsefulImageType(type: String): Boolean =
        type in setOf(
            "Box - Front", "Box - Front - Reconstructed", "Fanart - Box - Front", "Box - 3D",
            "Box - Back", "Box - Back - Reconstructed", "Fanart - Box - Back",
            "Box - Spine", "Box - Full",
            "Cart - Front", "Cart - 3D", "Cart - Back",
            "Disc", "Fanart - Disc",
            "Fanart - Background", "Banner", "Clear Logo",
            "Screenshot - Gameplay", "Screenshot - Game Title",
            "Screenshot - Game Select", "Screenshot - High Scores",
            "Arcade - Marquee",
        )

    internal fun lookupLocalIndex(title: String, platforms: List<String>, dbDir: File): LaunchBoxGameRecord? {
        if (!dbDir.isDirectory) return null
        val targetNorm = normalizeTitle(title)
        if (targetNorm.isEmpty()) return null

        var bestScore = 0
        var bestJson: JSONObject? = null
        for (platform in platforms) {
            val file = platformIndexFile(dbDir, platform)
            if (!file.isFile) continue
            runCatching {
                file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    for (line in lines) {
                        if (line.isBlank()) continue
                        val obj = runCatching { JSONObject(line) }.getOrNull() ?: continue
                        val name = obj.optString("n")
                        val score = scoreTitleMatch(title, name)
                        if (score > bestScore) {
                            bestScore = score
                            bestJson = obj
                            if (score >= 120) break
                        }
                    }
                }
            }
            if (bestScore >= 120) break
        }
        val winner = bestJson ?: return null
        if (bestScore < 72) return null

        val imgs = mutableListOf<LaunchBoxImage>()
        val imgArr = winner.optJSONArray("images")
        if (imgArr != null) {
            for (i in 0 until imgArr.length()) {
                val o = imgArr.getJSONObject(i)
                val f = o.optString("f")
                val t = o.optString("t")
                if (f.isNotBlank() && t.isNotBlank()) {
                    imgs += LaunchBoxImage(url = imageUrl(f), type = t, region = o.optString("r"))
                }
            }
        }
        val cr = winner.optDouble("cr", -1.0).toFloat()
        val mp = winner.optInt("mp", 0)
        return LaunchBoxGameRecord(
            databaseId = winner.optString("id"),
            name = winner.optString("n"),
            platform = winner.optString("p"),
            overview = winner.optString("o"),
            developer = winner.optString("d"),
            publisher = winner.optString("pub"),
            releaseDate = winner.optString("rd"),
            releaseYear = winner.optString("ry"),
            genres = winner.optString("g").split(';', ',').map { it.trim() }.filter { it.isNotEmpty() },
            esrb = winner.optString("e"),
            maxPlayers = mp.takeIf { it > 0 },
            cooperative = winner.optBoolean("coop", false),
            communityRating = cr.takeIf { it > 0f },
            wikipediaUrl = winner.optString("w"),
            videoUrl = winner.optString("v"),
            images = imgs,
        )
    }

    // ---- Path 2: Live LaunchBox GamesDB Web Lookup (Zero Sync Required!) ------------------------

    internal data class SearchHit(val detailPath: String, val databaseId: String, val title: String, val platform: String)

    private fun lookupLiveWeb(title: String, platforms: List<String>): LaunchBoxGameRecord? {
        val cleanQuery = title
            .replace(PAREN_TAG, " ")
            .replace(BRACKET_TAG, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleanQuery.isBlank()) return null

        val encoded = URLEncoder.encode(cleanQuery, "UTF-8").replace("+", "%20")
        val searchHtml = fetchHtml("$GAMESDB_BASE_URL/games/results/$encoded") ?: return null
        val hits = parseSearchResultsHtml(searchHtml)
        val bestHit = pickBestSearchHit(cleanQuery, platforms, hits) ?: return null

        val detailHtml = fetchHtml("$GAMESDB_BASE_URL${bestHit.detailPath}") ?: return null
        var record = parseDetailsHtml(detailHtml, bestHit)
        // LaunchBox's /games/details/ tab usually shows only Box - Front + featured art;
        // fetch /games/images/ as well so 3D Box - Back, Box - Spine, Box - Full, Cart & Disc scans load!
        // Spine and full-wrap scans usually appear only on the Images tab, so read it unless the
        // details page already had one (a box back + cart there is not enough).
        val hasSpineArt = record.images.any {
            it.type.equals("Box - Spine", ignoreCase = true) || it.type.equals("Box - Full", ignoreCase = true)
        }
        if (!hasSpineArt) {
            val imagesPath = bestHit.detailPath.replace("/games/details/", "/games/images/")
            val imagesHtml = fetchHtml("$GAMESDB_BASE_URL$imagesPath")
            if (imagesHtml != null) {
                val extra = parseImagesFromHtml(imagesHtml)
                if (extra.isNotEmpty()) {
                    record = record.copy(images = (record.images + extra).distinctBy { it.url })
                }
            }
        }
        return record.takeIf { it.overview.isNotBlank() || it.images.isNotEmpty() || it.developer.isNotBlank() }
    }

    /** Parses LaunchBox GamesDB `/games/results/...` HTML into structured [SearchHit] candidates. */
    internal fun parseSearchResultsHtml(html: String): List<SearchHit> {
        val hits = ArrayList<SearchHit>()
        val anchorRegex = Regex(
            """<a\b[^>]*href=["'](/games/details/(\d+)(?:-[^"'#?]*)?)["'][^>]*>([\s\S]*?)</a>""",
            RegexOption.IGNORE_CASE,
        )
        val seen = HashSet<String>()
        for (m in anchorRegex.findAll(html)) {
            val path = m.groupValues[1]
            val id = m.groupValues[2]
            if (!seen.add(id)) continue
            val block = m.groupValues[3]
            val h3 = Regex("""<h3[^>]*>([\s\S]*?)</h3>""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.let(::stripTags).orEmpty()
            // The results card puts the platform in an <h4> (older pages) or a <p> under the <h3> title.
            val h4 = (
                Regex("""<h4[^>]*>([\s\S]*?)</h4>""", RegexOption.IGNORE_CASE).find(block)
                    ?: Regex("""</h3>\s*<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE).find(block)
                )?.groupValues?.get(1)?.let(::stripTags).orEmpty()
            val textLines = stripTags(block)
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val hitTitle = h3.ifBlank { textLines.firstOrNull().orEmpty() }
            val hitPlatform = h4.ifBlank { textLines.getOrNull(1).orEmpty() }
            if (hitTitle.isNotBlank()) {
                hits += SearchHit(path, id, hitTitle, hitPlatform)
            }
        }
        return hits
    }

    internal fun pickBestSearchHit(queryTitle: String, platforms: List<String>, hits: List<SearchHit>): SearchHit? {
        val allowedLower = platforms.map { it.lowercase(Locale.US) }.toSet()
        var best: SearchHit? = null
        var bestScore = 0
        for (hit in hits) {
            val platLower = hit.platform.lowercase(Locale.US)
            val platformMatches = platLower.isEmpty() || allowedLower.any { it == platLower || it in platLower || platLower in it }
            if (!platformMatches) continue
            val titleScore = scoreTitleMatch(queryTitle, hit.title)
            val total = titleScore + if (platLower in allowedLower) 20 else 0
            if (total > bestScore) {
                bestScore = total
                best = hit
            }
        }
        return if (bestScore >= 72) best else null
    }

    /** Parses a LaunchBox GamesDB `/games/details/<id>-<slug>` HTML page into a [LaunchBoxGameRecord]. */
    internal fun parseDetailsHtml(html: String, hit: SearchHit): LaunchBoxGameRecord {
        fun extractField(label: String): String {
            // Supports both table/row layouts and div/span label-value layouts used by gamesdb.launchbox-app.com
            val p1 = Regex(
                """(?is)<(?:td|th|div|span|dt|h\d)[^>]*>\s*${Regex.escape(label)}\s*</(?:td|th|div|span|dt|h\d)>\s*<(?:td|div|span|dd)[^>]*>([\s\S]*?)</(?:td|div|span|dd)>""",
            )
            val m1 = p1.find(html)?.groupValues?.get(1)?.let(::stripTags)?.trim().orEmpty()
            if (m1.isNotBlank() && !m1.equals("No information available", ignoreCase = true)) return m1

            val p2 = Regex("""(?is)\b${Regex.escape(label)}\s*</[^>]+>\s*<[^>]+>([^<]{2,160})<""")
            val m2 = p2.find(html)?.groupValues?.get(1)?.let(::cleanText)?.trim().orEmpty()
            return if (m2.equals("No information available", ignoreCase = true)) "" else m2
        }

        val overview = Regex(
            """(?is)<div[^>]*class=["'][^"']*\bview\b[^"']*["'][^>]*>([\s\S]*?)</div>""",
        ).find(html)?.groupValues?.get(1)?.let(::stripTags)?.takeIf { it.length >= 30 }
            ?: Regex("""(?is)<p[^>]*class=["'][^"']*overview[^"']*["'][^>]*>([\s\S]*?)</p>""")
                .find(html)?.groupValues?.get(1)?.let(::stripTags)
            ?: extractOverviewAfterHeading(html)

        val releaseDate = extractField("Release Date")
        val releaseYear = Regex("""\b(19[789]\d|20[012]\d)\b""").find(releaseDate)?.groupValues?.get(1).orEmpty()
        val developer = extractField("Developers").ifBlank { extractField("Developer") }
        val publisher = extractField("Publishers").ifBlank { extractField("Publisher") }
        val genreRaw = extractField("Genres").ifBlank { extractField("Genre") }
        val genres = genreRaw.split(',', ';', '/', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        val esrb = extractField("ESRB")
        val maxPlayers = Regex("""\d+""").find(extractField("Max Players"))?.value?.toIntOrNull()
        val coop = extractField("Cooperative").equals("Yes", ignoreCase = true)
        val rating = Regex("""\b([0-5](?:\.\d+)?)\b""").find(extractField("Community Rating"))
            ?.groupValues?.get(1)?.toFloatOrNull()
        val images = parseImagesFromHtml(html)

        return LaunchBoxGameRecord(
            databaseId = hit.databaseId,
            name = hit.title,
            platform = hit.platform.ifBlank { extractField("Platform") },
            overview = overview,
            developer = developer,
            publisher = publisher,
            releaseDate = releaseDate,
            releaseYear = releaseYear,
            genres = genres,
            esrb = esrb,
            maxPlayers = maxPlayers,
            cooperative = coop,
            communityRating = rating,
            images = images,
        )
    }

    private fun extractOverviewAfterHeading(html: String): String {
        val m = Regex("""(?is)<h[234][^>]*>\s*Overview\s*</h[234]>([\s\S]{30,3000}?)(?:<h[234]|<table|<section|Developers\s*</)""")
            .find(html) ?: return ""
        val paragraphs = Regex("""(?is)<p[^>]*>([\s\S]*?)</p>""")
            .findAll(m.groupValues[1])
            .map { stripTags(it.groupValues[1]).trim() }
            .filter { it.length >= 20 && !it.equals("Edit", ignoreCase = true) }
            .toList()
        return if (paragraphs.isNotEmpty()) paragraphs.joinToString("\n\n")
        else stripTags(m.groupValues[1]).replace(Regex("""^\s*Edit\s*""", RegexOption.IGNORE_CASE), "").trim()
    }

    /** Extracts classified `https://images.launchbox-app.com/...` assets from LaunchBox Details or Images HTML. */
    internal fun parseImagesFromHtml(html: String): List<LaunchBoxImage> {
        val results = ArrayList<LaunchBoxImage>()
        val seen = HashSet<String>()
        val coveredRanges = ArrayList<IntRange>()

        // Pass 1: Full-resolution <a href="https://images.launchbox-app.com/...">...</a> gallery cards
        val anchorMatches = Regex(
            """(?is)(<a\b[^>]+href=["'](https?://images\.launchbox-app\.com/[^"'\s)]+)["'][^>]*>)([\s\S]{0,600}?</a>)""",
        ).findAll(html).toList()

        for (i in anchorMatches.indices) {
            val m = anchorMatches[i]
            val fullUrl = m.groupValues[2].substringBefore('?')
            coveredRanges += m.range
            // Mark any nested thumbnail <img src="..."> inside this <a>...</a> as seen so it isn't duplicated
            Regex("""(?is)(?:src|data-src)=["'](https?://images\.launchbox-app\.com/[^"'\s)]+)["']""")
                .findAll(m.groupValues[3])
                .forEach { seen += it.groupValues[1].substringBefore('?') }
            if (!seen.add(fullUrl)) continue

            val attrLabels = Regex("""(?i)(?:alt|title|data-title|data-type|aria-label)=["']([^"']+)["']""")
                .findAll(m.value)
                .joinToString(" ") { it.groupValues[1] }
            val innerText = stripTags(m.groupValues[3])
            val prevEnd = if (i > 0) anchorMatches[i - 1].range.last + 1 else (m.range.first - 140).coerceAtLeast(0)
            val nextStart = if (i < anchorMatches.lastIndex) anchorMatches[i + 1].range.first else (m.range.last + 160).coerceAtMost(html.length)
            val localAfter = stripTags(html.substring((m.range.last + 1).coerceAtMost(html.length), nextStart.coerceAtMost(m.range.last + 160)))
            val localBefore = stripTags(html.substring(prevEnd.coerceAtLeast(m.range.first - 140), m.range.first))

            val detectedType = detectImageTypeOrNull(attrLabels)
                ?: detectImageTypeOrNull(innerText)
                ?: detectImageTypeOrNull("$localAfter $localBefore")
                ?: "Box - Front"
            val detectedRegion = detectRegion(attrLabels)
                .ifBlank { detectRegion(innerText) }
                .ifBlank { detectRegion("$localAfter $localBefore") }
            results += LaunchBoxImage(url = fullUrl, type = detectedType, region = detectedRegion)
        }

        // Pass 2: Standalone <img src="https://images.launchbox-app.com/..."> tags outside gallery anchors
        val imgMatches = Regex(
            """(?is)<img\b[^>]+(?:src|data-src)=["'](https?://images\.launchbox-app\.com/[^"'\s)]+)["'][^>]*>""",
        ).findAll(html).filter { m -> coveredRanges.none { m.range.first in it } }.toList()

        for (i in imgMatches.indices) {
            val m = imgMatches[i]
            val cleanUrl = m.groupValues[1].substringBefore('?')
            if (!seen.add(cleanUrl)) continue
            val tag = m.value
            val alt = Regex("""(?i)(?:alt|title|data-title|data-type)=["']([^"']+)["']""")
                .find(tag)?.groupValues?.get(1).orEmpty()

            val prevEnd = if (i > 0) imgMatches[i - 1].range.last + 1 else (m.range.first - 140).coerceAtLeast(0)
            val nextStart = if (i < imgMatches.lastIndex) imgMatches[i + 1].range.first else (m.range.last + 140).coerceAtMost(html.length)
            val localBefore = stripTags(html.substring(prevEnd.coerceAtLeast(m.range.first - 140), m.range.first))
            val localAfter = stripTags(html.substring((m.range.last + 1).coerceAtMost(html.length), nextStart.coerceAtMost(m.range.last + 140)))

            val detectedType = detectImageTypeOrNull(alt)
                ?: detectImageTypeOrNull("$localAfter $localBefore")
                ?: "Box - Front"
            val detectedRegion = detectRegion(alt).ifBlank { detectRegion("$localAfter $localBefore") }
            results += LaunchBoxImage(url = cleanUrl, type = detectedType, region = detectedRegion)
        }
        return results
    }

    private fun detectImageTypeOrNull(text: String): String? {
        if (text.isBlank()) return null
        val orderedTypes = listOf(
            "Fanart - Background",
            "Box - Front - Reconstructed",
            "Fanart - Box - Front",
            "Box - Back - Reconstructed",
            "Fanart - Box - Back",
            "Box - Front",
            "Box - Back",
            "Box - Spine",
            "Box - Full",
            "Box - 3D",
            "Clear Logo",
            "Screenshot - Game Title",
            "Screenshot - Gameplay",
            "Screenshot - Game Select",
            "Screenshot - High Scores",
            "Arcade - Marquee",
            "Banner",
            "Cart - Front",
            "Cart - Back",
            "Cart - 3D",
            "Fanart - Disc",
            "Disc",
        )
        return orderedTypes.firstOrNull { text.contains(it, ignoreCase = true) }
    }

    private fun detectRegion(text: String): String {
        val regions = listOf("North America", "United States", "Europe", "Japan", "World", "United Kingdom")
        return regions.firstOrNull { text.contains(it, ignoreCase = true) }.orEmpty()
    }

    private fun stripTags(html: String): String = cleanText(
        html
            .replace(Regex("""(?is)<br\s*/?>"""), "\n")
            .replace(Regex("""(?is)</p>|</div>|</li>"""), "\n")
            .replace(Regex("""(?is)<script[\s\S]*?</script>"""), " ")
            .replace(Regex("""(?is)<style[\s\S]*?</style>"""), " ")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n\s*\n+"""), "\n\n"),
    )

    private fun fetchHtml(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
