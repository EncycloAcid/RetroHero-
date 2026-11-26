package com.retrohero.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Rich game dossier fetched live from public encyclopedias (Wikipedia MediaWiki API)
 * and cached permanently on disk so every Game Page loads real credits, release date,
 * genre DNA radar values, editorial overview, and feature highlights.
 */
data class GameDossier(
    val title: String,
    val tagline: String,
    val developer: String,
    val publisher: String,
    val releaseDate: String,
    val releaseYear: String,
    val genre: String,
    val modes: String,
    val summary: String,
    val featureSections: List<FeatureBlock>,
    val promoQuote: String,
    val radarAxes: List<RadarAxis>,
    val heroImageUrl: String?,
    val sourceLabel: String,
    val isLive: Boolean,
) {
    data class FeatureBlock(val headline: String, val body: String)
    data class RadarAxis(val label: String, val value: Float)

    fun toJson(): JSONObject = JSONObject()
        .put("title", title)
        .put("tagline", tagline)
        .put("developer", developer)
        .put("publisher", publisher)
        .put("releaseDate", releaseDate)
        .put("releaseYear", releaseYear)
        .put("genre", genre)
        .put("modes", modes)
        .put("summary", summary)
        .put("promoQuote", promoQuote)
        .put("heroImageUrl", heroImageUrl ?: JSONObject.NULL)
        .put("sourceLabel", sourceLabel)
        .put("isLive", isLive)
        .put(
            "featureSections",
            JSONArray().apply {
                featureSections.forEach {
                    put(JSONObject().put("headline", it.headline).put("body", it.body))
                }
            },
        )
        .put(
            "radarAxes",
            JSONArray().apply {
                radarAxes.forEach {
                    put(JSONObject().put("label", it.label).put("value", it.value.toDouble()))
                }
            },
        )

    companion object {
        fun fromJson(o: JSONObject): GameDossier {
            val sections = mutableListOf<FeatureBlock>()
            val sArr = o.optJSONArray("featureSections")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    val item = sArr.getJSONObject(i)
                    sections += FeatureBlock(item.optString("headline"), item.optString("body"))
                }
            }
            val axes = mutableListOf<RadarAxis>()
            val rArr = o.optJSONArray("radarAxes")
            if (rArr != null) {
                for (i in 0 until rArr.length()) {
                    val item = rArr.getJSONObject(i)
                    axes += RadarAxis(item.optString("label"), item.optDouble("value", 0.5).toFloat())
                }
            }
            return GameDossier(
                title = o.optString("title"),
                tagline = o.optString("tagline"),
                developer = o.optString("developer"),
                publisher = o.optString("publisher"),
                releaseDate = o.optString("releaseDate"),
                releaseYear = o.optString("releaseYear"),
                genre = o.optString("genre"),
                modes = o.optString("modes"),
                summary = o.optString("summary"),
                featureSections = sections,
                promoQuote = o.optString("promoQuote"),
                radarAxes = axes.ifEmpty { defaultRadar("") },
                heroImageUrl = o.optString("heroImageUrl").takeIf { it.isNotBlank() && it != "null" },
                sourceLabel = o.optString("sourceLabel"),
                isLive = o.optBoolean("isLive", false),
            )
        }

        /** Instant local fallback dossier shown immediately while live Wikipedia metadata loads. */
        fun localFallback(game: Game): GameDossier {
            val sys = game.system
            val sysName = sys?.name ?: game.systemId.uppercase()
            val mfr = sys?.manufacturer ?: "Classic Hardware"
            val yearHint = Regex("""\b(19[789]\d|20[012]\d)\b""").find(game.fileName)?.groupValues?.get(1) ?: ""
            val region = Regex("""\((USA|Europe|Japan|World|USA, Europe)\)""", RegexOption.IGNORE_CASE)
                .find(game.fileName)?.groupValues?.get(1) ?: "Classic Edition"
            val guessedGenre = guessGenreFromTitle(game.title, game.systemId)
            return GameDossier(
                title = game.title,
                tagline = "Experience ${game.title} on $sysName — preserved with instant save states, crisp scaling, and modern controller support.",
                developer = mfr,
                publisher = mfr,
                releaseDate = if (yearHint.isNotEmpty()) yearHint else region,
                releaseYear = yearHint.ifEmpty { sys?.shortName ?: "RETRO" },
                genre = guessedGenre,
                modes = "Single-player · Controller Ready",
                summary = "${game.title} runs natively through RetroHero's libretro engine for $sysName. Enjoy frame-accurate emulation, instant auto-save recovery, customizable touch or Bluetooth gamepad controls, and gallery-ready screenshot captures.",
                featureSections = listOf(
                    FeatureBlock(
                        headline = "BUILT FOR $sysName".uppercase(),
                        body = "Originally engineered for $mfr's $sysName hardware, ${game.title} brings iconic retro craft directly to your pocket with zero-latency input and sharp pixel rendering.",
                    ),
                    FeatureBlock(
                        headline = "PLAY YOUR WAY",
                        body = "Pick up right where you left off with automatic save states, speed through grinding with fast-forward, or pair any Bluetooth gamepad for a full console experience.",
                    ),
                ),
                promoQuote = "A CLASSIC OF THE ${sys?.shortName?.uppercase() ?: "RETRO"} ERA",
                radarAxes = defaultRadar("$guessedGenre ${game.title} ${game.systemId}"),
                heroImageUrl = null,
                sourceLabel = "Local Library",
                isLive = false,
            )
        }

        internal fun guessGenreFromTitle(title: String, systemId: String): String {
            val t = title.lowercase()
            return when {
                listOf("mario", "sonic", "kirby", "donkey kong", "celeste", "rayman", "crash", "megaman", "mega man", "castlevania", "metroid").any { it in t } -> "Action · Platformer"
                listOf("zelda", "chrono", "final fantasy", "pokemon", "pokémon", "persona", "dragon quest", "earthbound", "mother", "golden sun", "fire emblem", "tactics").any { it in t } -> "Role-Playing · Adventure"
                listOf("kart", "racing", "gran turismo", "f-zero", "need for speed", "wipeout", "burnout", "ridge racer").any { it in t } -> "Racing · Arcade"
                listOf("street fighter", "mortal kombat", "tekken", "smash", "king of fighters", "marvel", "soulcalibur").any { it in t } -> "Fighting · Arcade"
                listOf("tetris", "puyo", "puzzle", "dr. mario", "lumines", "columns", "picross").any { it in t } -> "Puzzle · Strategy"
                listOf("star fox", "gradius", "contra", "metal slug", "doom", "quake", "galaga", "r-type", "ikaruga").any { it in t } -> "Shooter · Action"
                listOf("nba", "nfl", "fifa", "tony hawk", "golf", "tennis", "baseball", "wwe", "skate").any { it in t } -> "Sports · Arcade"
                systemId in setOf("arcade", "neogeo") -> "Arcade · Action"
                else -> "Classic Action · Adventure"
            }
        }

        internal fun defaultRadar(hints: String): List<RadarAxis> {
            val h = hints.lowercase()
            var action = 0.56f
            var adventure = 0.52f
            var rpg = 0.34f
            var skill = 0.62f
            var replay = 0.68f

            if (listOf("action", "shooter", "beat", "hack", "fight", "combat", "contra", "metal", "doom").any { it in h }) {
                action = 0.92f; skill = 0.84f
            }
            if (listOf("platform", "mario", "sonic", "kirby", "celeste", "donkey", "metroid", "castlevania").any { it in h }) {
                action = 0.85f; skill = 0.88f; replay = 0.86f; adventure = 0.68f
            }
            if (listOf("rpg", "role-playing", "chrono", "fantasy", "pokemon", "pokémon", "persona", "tactics", "emblem").any { it in h }) {
                rpg = 0.94f; adventure = 0.90f; replay = 0.80f; action = 0.48f
            }
            if (listOf("adventure", "zelda", "exploration", "metroidvania", "survival").any { it in h }) {
                adventure = 0.92f; action = 0.74f; rpg = 0.62f
            }
            if (listOf("puzzle", "strategy", "tactical", "tetris", "chess", "sim").any { it in h }) {
                skill = 0.90f; replay = 0.92f; rpg = 0.55f; action = 0.36f
            }
            if (listOf("racing", "kart", "sport", "arcade", "fighting", "smash").any { it in h }) {
                action = 0.88f; skill = 0.86f; replay = 0.94f; rpg = 0.24f; adventure = 0.32f
            }
            return listOf(
                RadarAxis("ADVENTURE", adventure.coerceIn(0.2f, 0.98f)),
                RadarAxis("ACTION", action.coerceIn(0.2f, 0.98f)),
                RadarAxis("SKILL", skill.coerceIn(0.2f, 0.98f)),
                RadarAxis("REPLAY", replay.coerceIn(0.2f, 0.98f)),
                RadarAxis("RPG", rpg.coerceIn(0.2f, 0.98f)),
            )
        }
    }
}

/**
 * Fetches and caches rich game dossiers from Wikipedia's free MediaWiki API (no API key required).
 */
class GameMetadataRepository(private val cacheDir: File) {
    private val memory = java.util.concurrent.ConcurrentHashMap<String, GameDossier>()

    private fun cacheFile(game: Game): File {
        val key = "${game.systemId}:${game.title.lowercase()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(24)
        return File(cacheDir, "$hash.json")
    }

    fun peekCached(game: Game): GameDossier? {
        memory[game.id]?.let { return it }
        val file = cacheFile(game)
        if (!file.exists()) return null
        return runCatching { GameDossier.fromJson(JSONObject(file.readText())) }
            .getOrNull()
            ?.also { memory[game.id] = it }
    }

    suspend fun load(game: Game, forceRefresh: Boolean = false): GameDossier = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            peekCached(game)?.let { return@withContext it }
        }
        val fallback = GameDossier.localFallback(game)
        val live = runCatching { fetchFromWikipedia(game, fallback) }.getOrNull()
        val result = live ?: fallback
        memory[game.id] = result
        if (live != null) {
            runCatching { writeAtomic(cacheFile(game), result.toJson().toString()) }
        }
        result
    }

    private fun fetchFromWikipedia(game: Game, fallback: GameDossier): GameDossier? {
        val sysName = game.system?.name.orEmpty()
        val searchQueries = listOf(
            "${game.title} video game $sysName".trim(),
            "${game.title} video game",
            game.title,
        )
        var pageTitle: String? = null
        for (q in searchQueries) {
            pageTitle = searchWikipediaTitle(q, game.title)
            if (pageTitle != null) break
        }
        if (pageTitle == null) return null

        val encTitle = URLEncoder.encode(pageTitle, "UTF-8")
        // Fetch plain-text extract, page thumbnail, and raw wikitext (for infobox credits) in one call!
        val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2" +
            "&prop=extracts|pageimages|revisions&exintro=false&explaintext=true&exchars=3200" +
            "&piprop=thumbnail&pithumbsize=960&rvprop=content&rvslots=main&rvsection=0" +
            "&redirects=1&titles=$encTitle"
        val json = httpGetJson(url) ?: return null
        val page = json.optJSONObject("query")?.optJSONArray("pages")?.optJSONObject(0) ?: return null
        if (page.optBoolean("missing", false)) return null

        val extract = page.optString("extract", "").trim()
        val thumbUrl = page.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
        val wikitext = page.optJSONArray("revisions")
            ?.optJSONObject(0)
            ?.optJSONObject("slots")
            ?.optJSONObject("main")
            ?.optString("content", "")
            .orEmpty()

        val developer = parseInfoboxField(wikitext, listOf("developer", "developers")).ifBlank { fallback.developer }
        val publisher = parseInfoboxField(wikitext, listOf("publisher", "publishers")).ifBlank { fallback.publisher }
        val releaseRaw = parseInfoboxField(wikitext, listOf("released", "release")).ifBlank { fallback.releaseDate }
        val releaseYear = Regex("""\b(19[789]\d|20[012]\d)\b""").find(releaseRaw)?.groupValues?.get(1)
            ?: Regex("""\b(19[789]\d|20[012]\d)\b""").find(extract)?.groupValues?.get(1)
            ?: fallback.releaseYear
        val releaseDate = cleanReleaseDate(releaseRaw, releaseYear)
        val genre = parseInfoboxField(wikitext, listOf("genre", "genres")).ifBlank { fallback.genre }
        val modes = parseInfoboxField(wikitext, listOf("modes", "mode")).ifBlank { fallback.modes }

        val (introSummary, sections) = parseExtractSections(extract, game.title, fallback)
        val tagline = introSummary.split(Regex("""(?<=[.!?])\s+""")).firstOrNull()?.take(180)?.trim()
            ?.takeIf { it.length > 24 } ?: fallback.tagline
        val quote = buildPromoQuote(game.title, releaseYear, genre, sysName)
        val radar = GameDossier.defaultRadar("$genre $modes $introSummary ${game.title}")

        return GameDossier(
            title = game.title,
            tagline = tagline,
            developer = developer,
            publisher = publisher,
            releaseDate = releaseDate,
            releaseYear = releaseYear,
            genre = genre,
            modes = modes,
            summary = introSummary,
            featureSections = sections,
            promoQuote = quote,
            radarAxes = radar,
            heroImageUrl = thumbUrl,
            sourceLabel = "Wikipedia",
            isLive = true,
        )
    }

    private fun searchWikipediaTitle(query: String, gameTitle: String): String? {
        val enc = URLEncoder.encode(query, "UTF-8")
        val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&list=search&srlimit=5&srsearch=$enc"
        val json = httpGetJson(url) ?: return null
        val results = json.optJSONObject("query")?.optJSONArray("search") ?: return null
        val normTarget = gameTitle.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        var bestTitle: String? = null
        var bestScore = -1
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            val t = item.optString("title")
            val snippet = item.optString("snippet").lowercase()
            val tNorm = t.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
            var score = 0
            if (tNorm == normTarget || tNorm == "$normTarget video game") score += 60
            else if (tNorm.startsWith(normTarget)) score += 35
            else if (normTarget.isNotEmpty() && normTarget in tNorm) score += 20
            if ("video game" in t.lowercase() || "video game" in snippet || "released" in snippet || "developed by" in snippet || "nintendo" in snippet || "playstation" in snippet || "sega" in snippet || "game boy" in snippet) {
                score += 25
            }
            if ("list of" in t.lowercase() || "soundtrack" in t.lowercase() || "film" in t.lowercase()) score -= 50
            if (score > bestScore && score >= 25) {
                bestScore = score
                bestTitle = t
            }
        }
        return bestTitle
    }

    private fun httpGetJson(urlStr: String): JSONObject? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5500
            readTimeout = 6500
            setRequestProperty("User-Agent", "RetroHero/0.2 (Android Retro Emulator; https://github.com/EncycloAcid/RetroHero-)")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) null
            else JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        internal fun parseInfoboxField(wikitext: String, keys: List<String>): String {
            if (wikitext.isBlank()) return ""
            for (key in keys) {
                val regex = Regex("""(?im)^\|\s*$key\s*=\s*(.+?)(?=\n\|\s*[a-zA-Z_ ]+\s*=|\n\}\}|\z)""", RegexOption.DOT_MATCHES_ALL)
                val raw = regex.find(wikitext)?.groupValues?.get(1) ?: continue
                val cleaned = cleanWikiMarkup(raw)
                if (cleaned.isNotBlank()) return cleaned.take(96)
            }
            return ""
        }

        internal fun cleanWikiMarkup(raw: String): String {
            var s = raw
            // Strip HTML comments and <ref>...</ref>
            s = s.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), " ")
            s = s.replace(Regex("""<ref[^>]*>.*?</ref>""", RegexOption.DOT_MATCHES_ALL), " ")
            s = s.replace(Regex("""<ref[^/]*/>"""), " ")
            s = s.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), ", ")
            // Unwrap {{vgrelease|JP|September 13, 1985|NA|October 1985}} or {{ubl|...}} or {{plainlist|...}}
            s = s.replace(Regex("""\{\{(?:vgrelease|video game release|vgr)\s*\|([^}]*)\}\}""", RegexOption.IGNORE_CASE)) { m ->
                m.groupValues[1].split('|').map { it.trim() }.filter { it.length > 3 && !it.contains('=') }.take(2).joinToString(" · ")
            }
            s = s.replace(Regex("""\{\{(?:ubl|unbulleted list|plainlist|flatlist|hlist|collapsible list)\s*\|([^}]*)\}\}""", RegexOption.IGNORE_CASE)) { m ->
                m.groupValues[1].split('|', '*').map { it.trim() }.filter { it.isNotBlank() && !it.contains('=') }.take(3).joinToString(", ")
            }
            // Replace [[Target|Display]] -> Display and [[Target]] -> Target
            s = s.replace(Regex("""\[\[[^\]|]+\|([^\]]+)\]\]"""), "$1")
            s = s.replace(Regex("""\[\[([^\]]+)\]\]"""), "$1")
            // Strip remaining templates {{...}}
            repeat(3) { s = s.replace(Regex("""\{\{[^{}]*\}\}"""), " ") }
            // Strip formatting quotes and HTML tags
            s = s.replace(Regex("""<[^>]+>"""), " ")
            s = s.replace("'''", "").replace("''", "")
            s = s.replace(Regex("""^\s*\*+\s*""", RegexOption.MULTILINE), "")
            s = s.replace('\n', ' ')
            s = s.replace(Regex("""\s*,\s*,\s*"""), ", ")
            s = s.replace(Regex("""\s{2,}"""), " ").trim().trim(',', ';', '·', ' ')
            return s
        }

        private fun cleanReleaseDate(raw: String, year: String): String {
            val cleaned = raw.trim()
            if (cleaned.length in 4..42 && year in cleaned) return cleaned
            val dateMatch = Regex("""([A-Z][a-z]+ \d{1,2},? \d{4}|\d{1,2} [A-Z][a-z]+ \d{4}|[A-Z][a-z]+ \d{4})""").find(cleaned)
            return dateMatch?.value ?: year.ifBlank { cleaned.take(36) }
        }

        internal fun parseExtractSections(
            extract: String,
            title: String,
            fallback: GameDossier,
        ): Pair<String, List<GameDossier.FeatureBlock>> {
            if (extract.isBlank()) return fallback.summary to fallback.featureSections
            val parts = extract.split(Regex("""\n==\s*([^=]+?)\s*==\n"""))
            val headings = Regex("""\n==\s*([^=]+?)\s*==\n""").findAll(extract).map { it.groupValues[1].trim() }.toList()
            val intro = parts.firstOrNull()?.replace(Regex("""\n+"""), "\n\n")?.trim().orEmpty()
                .ifBlank { fallback.summary }

            val blocks = mutableListOf<GameDossier.FeatureBlock>()
            headings.forEachIndexed { idx, heading ->
                val body = parts.getOrNull(idx + 1)
                    ?.replace(Regex("""===[^=]+==="""), "")
                    ?.replace(Regex("""\n+"""), "\n\n")
                    ?.trim()
                    .orEmpty()
                val lower = heading.lowercase()
                if (body.length >= 60 && lower !in setOf("references", "external links", "see also", "notes", "bibliography")) {
                    val trimmed = if (body.length > 340) body.take(340).substringBeforeLast(' ') + "…" else body
                    val label = when {
                        "gameplay" in lower -> "GAMEPLAY & MECHANICS"
                        "plot" in lower || "premise" in lower || "story" in lower || "synopsis" in lower -> "THE STORY UNFOLDS"
                        "reception" in lower || "legacy" in lower -> "CRITICAL ACCLAIM"
                        "development" in lower -> "BEHIND THE CRAFT"
                        else -> heading.uppercase()
                    }
                    blocks += GameDossier.FeatureBlock(label, trimmed)
                }
            }
            val finalBlocks = blocks.take(3).ifEmpty {
                // Split multi-paragraph intro into editorial highlight blocks
                val paras = intro.split("\n\n").filter { it.length > 50 }
                if (paras.size >= 2) {
                    listOf(
                        GameDossier.FeatureBlock("DISCOVER ${title.uppercase()}", paras[1].take(320)),
                    ) + fallback.featureSections.take(1)
                } else {
                    fallback.featureSections
                }
            }
            val shortIntro = if (intro.length > 420) intro.take(420).substringBeforeLast(' ') + "…" else intro
            return shortIntro to finalBlocks
        }

        private fun buildPromoQuote(title: String, year: String, genre: String, sysName: String): String {
            val primaryGenre = genre.split(',', '·', '/').firstOrNull()?.trim()?.uppercase().orEmpty()
            return when {
                year.length == 4 && primaryGenre.isNotEmpty() -> "$primaryGenre ICON · $year"
                sysName.isNotBlank() -> "ESSENTIAL ${sysName.uppercase()} CLASSIC"
                else -> "HALLMARK OF RETRO GAMING"
            }
        }
    }
}
