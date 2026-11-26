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
 * with strict platform & hardware-era verification, plus curated dossiers for RetroHero's
 * built-in consoles (HeroBox-16 and CHIP-8). Cached permanently on disk by schema version.
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
    val seriesName: String = "",
    val relatedGames: List<RelatedGame> = emptyList(),
) {
    data class FeatureBlock(val headline: String, val body: String)
    data class RadarAxis(val label: String, val value: Float)

    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", CACHE_SCHEMA_VERSION)
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
        .put("seriesName", seriesName)
        .put(
            "relatedGames",
            JSONArray().apply { relatedGames.forEach { put(it.toJson()) } },
        )
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
        const val CACHE_SCHEMA_VERSION = 4

        fun fromJson(o: JSONObject): GameDossier? {
            if (o.optInt("schemaVersion", 1) < CACHE_SCHEMA_VERSION) return null
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
            val related = mutableListOf<RelatedGame>()
            val relArr = o.optJSONArray("relatedGames")
            if (relArr != null) {
                for (i in 0 until relArr.length()) {
                    related += RelatedGame.fromJson(relArr.getJSONObject(i))
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
                seriesName = o.optString("seriesName"),
                relatedGames = related,
            )
        }

        /** Bespoke dossiers for RetroHero's built-in HeroBox-16 and CHIP-8 launch cartridges. */
        fun builtInDossier(game: Game): GameDossier? {
            val t = game.title.lowercase()
            val resolvedSeries = GameSeries.resolve(game.title, game.systemId)
            val base = when {
                game.systemId == "herobox16" && "starlight vanguard" in t -> GameDossier(
                    title = "Starlight Vanguard",
                    tagline = "Vertical 16-bit arcade space shooter built natively for RetroHero's HeroBox-16 fantasy console.",
                    developer = "RetroHero Studio",
                    publisher = "RetroHero Originals",
                    releaseDate = "2026",
                    releaseYear = "2026",
                    genre = "Shoot 'em up · Arcade",
                    modes = "Single-player · 60 FPS Native",
                    summary = "Pilot the Starlight Vanguard through escalating alien armadas, glowing nebula starfields, and multi-phase bosses on HeroBox-16's custom 240×160 HB16-PPU and 4-channel 44.1 kHz chiptune synthesizer.",
                    featureSections = listOf(
                        FeatureBlock(
                            headline = "SPREAD CANNONS & COMBO CHAINS",
                            body = "Chain rapid interceptions to multiply your score, collect triple-spread plasma cores and energy shields, and survive escalating wave bosses with zero input lag.",
                        ),
                        FeatureBlock(
                            headline = "POWERED BY HEROBOX-16",
                            body = "Runs natively inside libretrohero.so on the custom 16-register HB16-CPU with full deterministic save states, instant rewind, CRT shaders, and Bluetooth gamepad support.",
                        ),
                    ),
                    promoQuote = "HEROBOX-16 LAUNCH CLASSIC · 60 FPS",
                    radarAxes = defaultRadar("shooter arcade action"),
                    heroImageUrl = null,
                    sourceLabel = "HeroBox-16 Original",
                    isLive = true,
                )
                game.systemId == "herobox16" && "neon breaker" in t -> GameDossier(
                    title = "Neon Breaker DX",
                    tagline = "Synthwave brick breaker with multiball chaos, laser paddles, and punchy 4-channel chiptune audio.",
                    developer = "RetroHero Studio",
                    publisher = "RetroHero Originals",
                    releaseDate = "2026",
                    releaseYear = "2026",
                    genre = "Arcade · Action Puzzle",
                    modes = "Single-player · 60 FPS Native",
                    summary = "Shatter neon synthwave brick fortresses across multi-stage layouts with responsive paddle physics, triple-ball splits, wide-paddle expansions, and rapid-fire laser bursts.",
                    featureSections = listOf(
                        FeatureBlock(
                            headline = "MULTIBALL & LASER OVERDRIVE",
                            body = "Catch falling neon power capsules to unleash simultaneous energy spheres and twin paddle lasers while racking up relentless combo streaks.",
                        ),
                        FeatureBlock(
                            headline = "POWERED BY HEROBOX-16",
                            body = "Built directly for HeroBox-16's 240×160 pixel renderer and synthesized 4-channel APU with instant save states and rewind.",
                        ),
                    ),
                    promoQuote = "SYNTHWAVE ARCADE ACTION · 60 FPS",
                    radarAxes = defaultRadar("arcade puzzle action"),
                    heroImageUrl = null,
                    sourceLabel = "HeroBox-16 Original",
                    isLive = true,
                )
                game.systemId == "herobox16" && "prism dash" in t -> GameDossier(
                    title = "Prism Dash",
                    tagline = "High-speed retro action platformer with double-jumps, air-dashes, and gem combo multipliers.",
                    developer = "RetroHero Studio",
                    publisher = "RetroHero Originals",
                    releaseDate = "2026",
                    releaseYear = "2026",
                    genre = "Platformer · Endless Runner",
                    modes = "Single-player · 60 FPS Native",
                    summary = "Sprint across neon floating skylines on HeroBox-16! Chain double-jumps and burst air-dashes to clear shifting chasms, snatch sparkling prism gems, and push your distance record ever higher.",
                    featureSections = listOf(
                        FeatureBlock(
                            headline = "DOUBLE JUMP & AIR DASH",
                            body = "Master tight 60 Hz jump arcs and mid-air dash bursts to thread impossible gaps and keep your gem multiplier climbing.",
                        ),
                        FeatureBlock(
                            headline = "POWERED BY HEROBOX-16",
                            body = "Native C++ HeroBox-16 launch title with crisp pixel art, chiptune soundtrack, instant save states, and touch/gamepad support.",
                        ),
                    ),
                    promoQuote = "PURE 16-BIT MOMENTUM · 60 FPS",
                    radarAxes = defaultRadar("platformer action arcade"),
                    heroImageUrl = null,
                    sourceLabel = "HeroBox-16 Original",
                    isLive = true,
                )
                game.systemId == "chip8" && "cosmic bounce" in t -> GameDossier(
                    title = "Cosmic Bounce",
                    tagline = "Classic 64×32 monochrome CHIP-8 bytecode demo running natively on RetroHero's built-in CHIP-8 core.",
                    developer = "RetroHero Studio",
                    publisher = "RetroHero Originals",
                    releaseDate = "2026",
                    releaseYear = "2026",
                    genre = "Arcade · Retro Bytecode",
                    modes = "Single-player · Native CHIP-8",
                    summary = "Experience pure 1970s micro-computing minimalism with authentic CHIP-8 opcodes, crisp 64×32 phosphor graphics, synthesized 44.1 kHz sound timer tones, and full save-state & rewind support.",
                    featureSections = listOf(
                        FeatureBlock(
                            headline = "ZERO-DOWNLOAD NATIVE CHIP-8",
                            body = "Compiled directly into libretrohero.so with full CHIP-8 & SUPER-CHIP-8 hi-res 128×64 support and automatic RetroPad key mapping.",
                        ),
                    ),
                    promoQuote = "PURE CHIP-8 BYTECODE · 1977 SPIRIT",
                    radarAxes = defaultRadar("arcade puzzle"),
                    heroImageUrl = null,
                    sourceLabel = "Built-in CHIP-8",
                    isLive = true,
                )
                else -> null
            }
            return base?.copy(
                seriesName = resolvedSeries.seriesName,
                relatedGames = resolvedSeries.entries,
            )
        }

        /** Instant local fallback dossier shown immediately while live Wikipedia metadata loads. */
        fun localFallback(game: Game): GameDossier {
            builtInDossier(game)?.let { return it }
            val sys = game.system
            val sysName = sys?.name ?: game.systemId.uppercase()
            val mfr = sys?.manufacturer ?: "Classic Hardware"
            val yearHint = Regex("""\b(19[789]\d|20[012]\d)\b""").find(game.fileName)?.groupValues?.get(1) ?: ""
            val region = Regex("""\((USA|Europe|Japan|World|USA, Europe)\)""", RegexOption.IGNORE_CASE)
                .find(game.fileName)?.groupValues?.get(1) ?: "Classic Edition"
            val guessedGenre = guessGenreFromTitle(game.title, game.systemId)
            val resolvedSeries = GameSeries.resolve(game.title, game.systemId)
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
                radarAxes = computeGameDna(
                    genre = guessedGenre,
                    modes = "Single-player",
                    title = game.title,
                    summary = "",
                    systemId = game.systemId,
                ),
                heroImageUrl = null,
                sourceLabel = "Local Library",
                isLive = false,
                seriesName = resolvedSeries.seriesName,
                relatedGames = resolvedSeries.entries,
            )
        }

        internal fun guessGenreFromTitle(title: String, systemId: String): String {
            val t = title.lowercase()
            return when {
                // Check specific sub-franchises BEFORE broad series names like "mario"
                listOf("mario kart", "kart", "f-zero", "gran turismo", "need for speed", "wipeout", "burnout", "ridge racer", "outrun", "daytona", "racing").any { it in t } -> "Racing · Arcade"
                listOf("dr. mario", "dr mario", "tetris", "puyo", "puzzle", "lumines", "columns", "picross", "bust-a-move", "arkanoid").any { it in t } -> "Puzzle · Strategy"
                listOf("mario rpg", "paper mario", "mario & luigi", "mario and luigi", "chrono", "final fantasy", "pokemon", "pokémon", "persona", "dragon quest", "earthbound", "mother", "golden sun", "fire emblem", "tactics", "phantasy star", "breath of fire", "xenogears", "suikoden").any { it in t } -> "Role-Playing"
                listOf("mario tennis", "mario golf", "mario strikers", "nba", "nfl", "fifa", "tony hawk", "golf", "tennis", "baseball", "wwe", "punch-out", "tecmo bowl").any { it in t } -> "Sports · Arcade"
                listOf("zelda", "metroid", "castlevania", "tomb raider", "resident evil", "silent hill", "metal gear", "okami", "banjo").any { it in t } -> "Action-Adventure"
                listOf("street fighter", "mortal kombat", "tekken", "smash", "king of fighters", "fatal fury", "samurai shodown", "marvel vs", "soulcalibur", "guilty gear").any { it in t } -> "Fighting · Arcade"
                listOf("star fox", "gradius", "contra", "metal slug", "doom", "quake", "galaga", "r-type", "ikaruga", "galaxian", "space invaders", "goldeneye").any { it in t } -> "Shooter · Action"
                listOf("mario", "sonic", "kirby", "donkey kong", "celeste", "rayman", "crash", "megaman", "mega man", "yoshi", "wario", "spyro", "klonoa", "shantae").any { it in t } -> "Platformer"
                systemId in setOf("arcade", "neogeo") -> "Arcade · Action"
                else -> "Action · Adventure"
            }
        }

        private data class DnaProfile(
            val action: Float,
            val adventure: Float,
            val skill: Float,
            val replay: Float,
            val rpg: Float,
        )

        private fun hasAnyWord(text: String, words: List<String>): Boolean =
            words.any { w -> Regex("""\b${Regex.escape(w)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) }

        /**
         * Computes accurate, weighted 5-axis Game DNA from authoritative genre + modes + title + summary.
         * Uses whole-word matching and multi-genre blending so accidental substrings in summaries
         * (e.g. "simple", "similar", "adventure", "arcade") never corrupt a game's true profile.
         */
        internal fun computeGameDna(
            genre: String,
            modes: String,
            title: String,
            summary: String,
            systemId: String = "",
        ): List<RadarAxis> {
            val g = genre.lowercase()
            val t = title.lowercase()
            val m = modes.lowercase()
            val s = summary.lowercase()

            val matched = mutableListOf<DnaProfile>()

            val isActionRpg = hasAnyWord(g, listOf("action role-playing", "action rpg", "arpg"))
            val isRpg = !isActionRpg && hasAnyWord(g, listOf("role-playing", "rpg", "jrpg", "tactical role-playing", "monster-taming", "roguelike", "dungeon crawl"))
            val isActionAdv = hasAnyWord(g, listOf("action-adventure", "metroidvania", "survival horror", "stealth"))
            val isPlatformer = hasAnyWord(g, listOf("platform", "platformer", "side-scrolling platform", "2d platform", "3d platform"))
            val isShooter = hasAnyWord(g, listOf("shooter", "shoot 'em up", "shoot em up", "first-person shooter", "third-person shooter", "run and gun", "light gun", "scrolling shooter", "bullet hell"))
            val isFighting = hasAnyWord(g, listOf("fighting", "beat 'em up", "beat em up", "brawler", "martial arts", "hack and slash"))
            val isRacing = hasAnyWord(g, listOf("racing", "kart racing", "driving", "vehicular combat", "motorsport"))
            val isSports = hasAnyWord(g, listOf("sports", "sport", "basketball", "football", "soccer", "baseball", "tennis", "golf", "skateboarding", "wrestling", "boxing"))
            val isPuzzle = hasAnyWord(g, listOf("puzzle", "falling block", "tile-matching", "rhythm", "music", "maze", "pinball"))
            val isStrategy = hasAnyWord(g, listOf("strategy", "real-time strategy", "turn-based strategy", "turn-based tactics", "tactical", "simulation", "city-building", "management"))
            val isPureAdv = !isActionAdv && hasAnyWord(g, listOf("graphic adventure", "point-and-click", "visual novel", "interactive fiction", "adventure"))
            val isArcadeAction = hasAnyWord(g, listOf("arcade", "action", "party", "minigame"))

            if (isActionRpg)  matched += DnaProfile(action = 0.80f, adventure = 0.90f, skill = 0.78f, replay = 0.82f, rpg = 0.92f)
            if (isRpg)        matched += DnaProfile(action = 0.34f, adventure = 0.90f, skill = 0.66f, replay = 0.84f, rpg = 0.96f)
            if (isActionAdv)  matched += DnaProfile(action = 0.78f, adventure = 0.94f, skill = 0.76f, replay = 0.74f, rpg = 0.48f)
            if (isPlatformer) matched += DnaProfile(action = 0.86f, adventure = 0.62f, skill = 0.88f, replay = 0.88f, rpg = 0.10f)
            if (isShooter)    matched += DnaProfile(action = 0.95f, adventure = 0.34f, skill = 0.92f, replay = 0.86f, rpg = 0.08f)
            if (isFighting)   matched += DnaProfile(action = 0.95f, adventure = 0.20f, skill = 0.95f, replay = 0.92f, rpg = 0.08f)
            if (isRacing)     matched += DnaProfile(action = 0.88f, adventure = 0.18f, skill = 0.86f, replay = 0.94f, rpg = 0.08f)
            if (isSports)     matched += DnaProfile(action = 0.82f, adventure = 0.14f, skill = 0.84f, replay = 0.92f, rpg = 0.12f)
            if (isPuzzle)     matched += DnaProfile(action = 0.36f, adventure = 0.12f, skill = 0.94f, replay = 0.95f, rpg = 0.08f)
            if (isStrategy)   matched += DnaProfile(action = 0.30f, adventure = 0.52f, skill = 0.92f, replay = 0.88f, rpg = 0.68f)
            if (isPureAdv)    matched += DnaProfile(action = 0.26f, adventure = 0.94f, skill = 0.54f, replay = 0.64f, rpg = 0.40f)
            // Only add generic Arcade/Action when no more specific action genre already matched
            if (isArcadeAction && matched.isEmpty()) {
                matched += DnaProfile(action = 0.88f, adventure = 0.30f, skill = 0.84f, replay = 0.90f, rpg = 0.10f)
            }

            // Fallback to title classification only if genre gave no matches
            if (matched.isEmpty()) {
                val inferred = guessGenreFromTitle(title, systemId)
                if (!inferred.equals(genre, ignoreCase = true)) {
                    return computeGameDna(inferred, modes, title, summary, systemId)
                }
                matched += DnaProfile(action = 0.72f, adventure = 0.58f, skill = 0.70f, replay = 0.74f, rpg = 0.20f)
            }

            // Average all matched primary genre archetypes
            var action = matched.map { it.action }.average().toFloat()
            var adventure = matched.map { it.adventure }.average().toFloat()
            var skill = matched.map { it.skill }.average().toFloat()
            var replay = matched.map { it.replay }.average().toFloat()
            var rpg = matched.map { it.rpg }.average().toFloat()

            // Careful, capped refinements from modes & summary (never overrides primary genre!)
            if (hasAnyWord(m, listOf("multiplayer", "co-op", "cooperative", "competitive", "two-player", "2-player", "4-player"))) {
                replay += 0.06f
            }
            if (hasAnyWord(s, listOf("open world", "dungeon", "dungeons", "quest", "quests", "exploration", "interconnected", "non-linear"))) {
                adventure += 0.08f
            }
            if (hasAnyWord(s, listOf("experience points", "level up", "leveling", "turn-based", "party members", "hit points", "inventory", "magic spells"))) {
                rpg += 0.16f
            }
            if (hasAnyWord(s, listOf("high score", "combo", "combos", "speedrun", "time limit", "bosses", "boss"))) {
                skill += 0.03f
                replay += 0.03f
            }
            // Specific iconic hybrid nuances (Metroidvania RPGs like Symphony of the Night / Aria of Sorrow)
            if (listOf("symphony of the night", "aria of sorrow", "dawn of sorrow", "circle of the moon", "harmony of dissonance", "order of ecclesia", "portrait of ruin").any { it in t }) {
                adventure = maxOf(adventure, 0.92f)
                rpg = maxOf(rpg, 0.84f)
            }

            return listOf(
                RadarAxis("ADVENTURE", adventure.coerceIn(0.08f, 0.98f)),
                RadarAxis("ACTION", action.coerceIn(0.08f, 0.98f)),
                RadarAxis("SKILL", skill.coerceIn(0.08f, 0.98f)),
                RadarAxis("REPLAY", replay.coerceIn(0.08f, 0.98f)),
                RadarAxis("RPG", rpg.coerceIn(0.06f, 0.98f)),
            )
        }

        internal fun defaultRadar(hints: String): List<RadarAxis> =
            computeGameDna(genre = hints, modes = "", title = hints, summary = "", systemId = "")
    }
}

/**
 * Fetches and caches verified game dossiers from Wikipedia's free MediaWiki API.
 * Uses multi-candidate infobox + platform + hardware-era scoring so e.g. "Super Mario Bros." (NES)
 * resolves strictly to the 1985 Nintendo R&D4 NES classic, never "Super Mario Bros. Wonder" (2023).
 */
class GameMetadataRepository(private val cacheDir: File) {
    private val memory = java.util.concurrent.ConcurrentHashMap<String, GameDossier>()

    private fun cacheFile(game: Game): File {
        val key = "v${GameDossier.CACHE_SCHEMA_VERSION}:${game.systemId}:${game.title.lowercase()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(24)
        return File(cacheDir, "$hash.json")
    }

    fun peekCached(game: Game): GameDossier? {
        GameDossier.builtInDossier(game)?.let { return it }
        memory[game.id]?.let { return it }
        val file = cacheFile(game)
        if (!file.exists()) return null
        return runCatching { GameDossier.fromJson(JSONObject(file.readText())) }
            .getOrNull()
            ?.also { memory[game.id] = it }
    }

    suspend fun load(game: Game, forceRefresh: Boolean = false): GameDossier = withContext(Dispatchers.IO) {
        GameDossier.builtInDossier(game)?.let { return@withContext it }
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

    internal data class WikiPageCandidate(
        val pageTitle: String,
        val extract: String,
        val wikitext: String,
        val thumbUrl: String?,
    )

    private fun fetchFromWikipedia(game: Game, fallback: GameDossier): GameDossier? {
        val sysName = game.system?.name.orEmpty()
        val cleanTitle = cleanGameSearchTitle(game.title)

        // 1. Collect candidate titles: exact title + "(video game)" + top search hits
        val candidateTitles = linkedSetOf<String>()
        candidateTitles += cleanTitle
        candidateTitles += "$cleanTitle (video game)"

        val searchQueries = listOf(
            "\"$cleanTitle\" $sysName video game".trim(),
            "$cleanTitle video game",
        )
        for (q in searchQueries) {
            for (hit in searchWikipediaCandidates(q, limit = 6)) {
                candidateTitles += hit
                if (candidateTitles.size >= 8) break
            }
            if (candidateTitles.size >= 8) break
        }

        // 2. Fetch infobox wikitext + full plain-text extract + thumbnail for top candidates in 1 batch!
        val pages = fetchWikiPagesBatch(candidateTitles.take(6))
        if (pages.isEmpty()) return null

        // 3. Pick the candidate with the highest platform- & era-verified score
        val best = pages
            .map { page -> page to scoreCandidatePage(page, cleanTitle, game.systemId) }
            .filter { it.second >= 40 }
            .maxByOrNull { it.second }
            ?.first ?: return null

        return buildDossierFromWikiPage(best, game, fallback)
    }

    private fun searchWikipediaCandidates(query: String, limit: Int): List<String> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&list=search&srlimit=$limit&srsearch=$enc"
        val json = httpGetJson(url) ?: return emptyList()
        val results = json.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return buildList {
            for (i in 0 until results.length()) {
                val t = results.getJSONObject(i).optString("title").trim()
                if (t.isNotEmpty()) add(t)
            }
        }
    }

    private fun fetchWikiPagesBatch(titles: List<String>): List<WikiPageCandidate> {
        if (titles.isEmpty()) return emptyList()
        val joined = URLEncoder.encode(titles.joinToString("|"), "UTF-8")
        // Note: omit `exintro` completely so MediaWiki returns full sections (Gameplay, Plot, etc.).
        val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2" +
            "&prop=extracts|pageimages|revisions&explaintext=1&exchars=3600" +
            "&piprop=thumbnail&pithumbsize=960&rvprop=content&rvslots=main&rvsection=0" +
            "&redirects=1&titles=$joined"
        val json = httpGetJson(url) ?: return emptyList()
        val pagesArr = json.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return buildList {
            for (i in 0 until pagesArr.length()) {
                val page = pagesArr.getJSONObject(i)
                if (page.optBoolean("missing", false)) continue
                val pageTitle = page.optString("title", "").trim()
                if (pageTitle.isEmpty()) continue
                val extract = page.optString("extract", "").trim()
                val thumbUrl = page.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
                val wikitext = page.optJSONArray("revisions")
                    ?.optJSONObject(0)
                    ?.optJSONObject("slots")
                    ?.optJSONObject("main")
                    ?.optString("content", "")
                    .orEmpty()
                if (extract.isNotEmpty() || wikitext.isNotEmpty()) {
                    add(WikiPageCandidate(pageTitle, extract, wikitext, thumbUrl))
                }
            }
        }
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
        internal fun cleanGameSearchTitle(raw: String): String {
            var t = raw.trim()
            // Normalize ", The" suffix -> "The ..." (e.g. "Legend of Zelda, The" -> "The Legend of Zelda")
            if (t.endsWith(", The", ignoreCase = true)) {
                t = "The " + t.dropLast(5).trim()
            }
            return t
        }

        internal fun normalizeForCompare(s: String): String =
            s.lowercase()
                .replace("&", " and ")
                .replace(Regex("""\([^)]*\)"""), " ")
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

        internal fun systemEraWindow(systemId: String): IntRange? = when (systemId) {
            "atari2600", "odyssey2", "channelf" -> 1976..1992
            "intellivision", "coleco", "vectrex", "atari5200", "sg1000" -> 1979..1990
            "nes", "fds", "sms", "atari7800", "msx", "zxspectrum", "c64", "amstrad" -> 1983..1996
            "pce", "pcecd", "supergrafx", "genesis", "segacd", "gb", "gg", "lynx", "neogeo", "amiga", "atarist", "x68000", "pc98" -> 1987..2001
            "snes", "satellaview", "32x", "jaguar", "3do", "cdi", "supervision", "virtualboy" -> 1990..2001
            "ps1", "saturn", "n64", "gbc", "ngp", "wonderswan", "dreamcast", "pokemini" -> 1994..2005
            "ps2", "gba", "gc", "nds", "psp", "wii", "xbox" -> 2000..2015
            "3ds", "vita", "wiiu", "ps3" -> 2010..2022
            "switch" -> 2017..2026
            else -> null
        }

        internal fun systemPlatformAliases(systemId: String): List<String> = when (systemId) {
            "nes", "fds" -> listOf("nintendo entertainment system", "famicom", "nes")
            "snes", "satellaview" -> listOf("super nintendo", "super famicom", "snes")
            "n64" -> listOf("nintendo 64", "n64")
            "gc" -> listOf("gamecube", "nintendo gamecube")
            "wii" -> listOf("wii")
            "wiiu" -> listOf("wii u")
            "switch" -> listOf("nintendo switch")
            "gb" -> listOf("game boy")
            "gbc" -> listOf("game boy color", "game boy")
            "gba" -> listOf("game boy advance", "gba")
            "nds" -> listOf("nintendo ds", "nds")
            "3ds" -> listOf("nintendo 3ds", "3ds")
            "virtualboy" -> listOf("virtual boy")
            "pokemini" -> listOf("pokémon mini", "pokemon mini")
            "ps1" -> listOf("playstation")
            "ps2" -> listOf("playstation 2", "ps2")
            "psp" -> listOf("playstation portable", "psp")
            "vita" -> listOf("playstation vita", "ps vita")
            "genesis" -> listOf("sega genesis", "mega drive", "genesis")
            "segacd" -> listOf("sega cd", "mega-cd", "mega cd")
            "32x" -> listOf("32x")
            "sms" -> listOf("master system", "sega mark iii")
            "gg" -> listOf("game gear")
            "saturn" -> listOf("sega saturn", "saturn")
            "dreamcast" -> listOf("dreamcast")
            "sg1000" -> listOf("sg-1000")
            "pce", "pcecd", "supergrafx" -> listOf("turbografx", "pc engine", "supergrafx")
            "neogeo" -> listOf("neo geo", "arcade")
            "ngp" -> listOf("neo geo pocket")
            "wonderswan" -> listOf("wonderswan")
            "atari2600" -> listOf("atari 2600", "atari vcs")
            "atari5200" -> listOf("atari 5200")
            "atari7800" -> listOf("atari 7800")
            "lynx" -> listOf("atari lynx", "lynx")
            "jaguar" -> listOf("atari jaguar", "jaguar")
            "arcade" -> listOf("arcade")
            "dos" -> listOf("ms-dos", "dos", "pc")
            "c64" -> listOf("commodore 64")
            "amiga" -> listOf("amiga")
            "msx" -> listOf("msx")
            else -> emptyList()
        }

        internal fun scoreCandidatePage(
            page: WikiPageCandidate,
            gameTitle: String,
            systemId: String,
        ): Int {
            val lowerTitle = page.pageTitle.lowercase()
            val lowerWiki = page.wikitext.lowercase()
            val lowerLead = page.extract.take(600).lowercase()

            // Hard reject non-game articles
            if ("may refer to:" in lowerLead || "{{disambiguation" in lowerWiki || "(disambiguation)" in lowerTitle) return -500
            if (listOf("(film)", "(movie)", "(tv series)", "(animated", "(soundtrack)", "(album)", "(character)", "list of ").any { it in lowerTitle }) {
                return -400
            }

            val isGameInfobox = "{{infobox video game" in lowerWiki && "{{infobox video game series" !in lowerWiki
            val isSeriesInfobox = "{{infobox video game series" in lowerWiki || "{{infobox media franchise" in lowerWiki
            val mentionsVideoGame = "video game" in lowerLead || "platform game" in lowerLead ||
                "role-playing game" in lowerLead || "action game" in lowerLead || "shooter" in lowerLead ||
                "arcade game" in lowerLead || "puzzle game" in lowerLead || "racing game" in lowerLead ||
                "fighting game" in lowerLead

            if (!isGameInfobox && !mentionsVideoGame) return -250

            var score = 0
            if (isGameInfobox) score += 70
            if (isSeriesInfobox) score -= 90

            val targetNorm = normalizeForCompare(gameTitle)
            val pageBaseNorm = normalizeForCompare(page.pageTitle)
            val targetWords = targetNorm.split(' ').filter { it.isNotEmpty() }
            val pageWords = pageBaseNorm.split(' ').filter { it.isNotEmpty() }

            when {
                pageBaseNorm == targetNorm -> score += 150
                pageBaseNorm.startsWith("$targetNorm ") -> {
                    // Extra words in the title (e.g. "Super Mario Bros. Wonder" when looking for "Super Mario Bros.")!
                    val extraWords = pageWords.drop(targetWords.size)
                    score -= 95 + (extraWords.size * 15)
                }
                targetNorm.isNotEmpty() && targetNorm in pageBaseNorm -> score -= 40
                else -> {
                    val overlap = targetWords.count { it in pageWords }
                    if (overlap == 0) return -300
                    score += (overlap * 25) - ((pageWords.size - overlap).coerceAtLeast(0) * 20)
                }
            }

            // Disambiguation suffix bonus when title matches e.g. "(1985 video game)" or "(video game)"
            val parenMatch = Regex("""\(([^)]+)\)""").find(page.pageTitle)?.groupValues?.get(1)?.lowercase()
            val era = systemEraWindow(systemId)
            if (parenMatch != null) {
                if ("video game" in parenMatch) score += 15
                val parenYear = Regex("""\b(19\d\d|20\d\d)\b""").find(parenMatch)?.groupValues?.get(1)?.toIntOrNull()
                if (parenYear != null && era != null) {
                    if (parenYear in era) score += 60 else score -= 90
                }
            }

            // Platform verification against infobox `| platforms =` and lead paragraph
            val aliases = systemPlatformAliases(systemId)
            val infoboxPlatforms = parseInfoboxField(page.wikitext, listOf("platforms", "platform")).lowercase()
            if (aliases.isNotEmpty()) {
                val platformMatchesInfobox = aliases.any { alias ->
                    Regex("""\b${Regex.escape(alias)}\b""").containsMatchIn(infoboxPlatforms)
                }
                val platformMatchesLead = aliases.any { alias ->
                    Regex("""\b${Regex.escape(alias)}\b""").containsMatchIn(lowerLead)
                }
                // Guard PS1 against matching "PlayStation 2/3/4/5"
                val ps1FalsePositive = systemId == "ps1" &&
                    !Regex("""\bplaystation\b(?!\s*[2345]|\s*portable|\s*vita)""").containsMatchIn("$infoboxPlatforms $lowerLead")

                when {
                    !ps1FalsePositive && (platformMatchesInfobox || platformMatchesLead) -> score += 95
                    infoboxPlatforms.isNotBlank() -> score -= 75
                }
            }

            // Hardware era window verification against earliest release year
            val releaseRaw = parseInfoboxField(page.wikitext, listOf("released", "release"))
            val earliestYear = extractEarliestYear(releaseRaw, page.extract.take(350))?.toIntOrNull()
            if (earliestYear != null && era != null) {
                when {
                    earliestYear in era -> score += 60
                    earliestYear in (era.first - 2)..(era.last + 3) -> score += 15
                    else -> score -= 95
                }
            }

            return score
        }

        internal fun buildDossierFromWikiPage(
            page: WikiPageCandidate,
            game: Game,
            fallback: GameDossier,
        ): GameDossier {
            val sysName = game.system?.name.orEmpty()
            val wikitext = page.wikitext
            val extract = page.extract.ifBlank { extractPlainLeadFromWikitext(wikitext) }

            val developer = parseInfoboxField(wikitext, listOf("developer", "developers")).ifBlank { fallback.developer }
            val publisher = parseInfoboxField(wikitext, listOf("publisher", "publishers")).ifBlank { fallback.publisher }
            val releaseRaw = parseInfoboxField(wikitext, listOf("released", "release"))
            val releaseYear = extractEarliestYear(releaseRaw, extract.take(400)) ?: fallback.releaseYear
            val releaseDate = cleanReleaseDate(releaseRaw, releaseYear).ifBlank { fallback.releaseDate }
            val genre = parseInfoboxField(wikitext, listOf("genre", "genres")).ifBlank { fallback.genre }
            val modes = parseInfoboxField(wikitext, listOf("modes", "mode")).ifBlank { fallback.modes }
            val wikiSeries = parseInfoboxField(wikitext, listOf("series"))
            val wikiPrecededBy = parseInfoboxField(wikitext, listOf("preceded_by", "preceded by", "prequel"))
            val wikiFollowedBy = parseInfoboxField(wikitext, listOf("followed_by", "followed by", "sequel"))

            val (introSummary, sections) = parseExtractSections(extract, game.title, fallback)
            val tagline = buildCleanTagline(introSummary, fallback.tagline)
            val quote = buildPromoQuote(game.title, releaseYear, genre, sysName)
            val radar = GameDossier.computeGameDna(
                genre = genre,
                modes = modes,
                title = game.title,
                summary = introSummary,
                systemId = game.systemId,
            )
            val resolvedSeries = GameSeries.resolve(
                gameTitle = game.title,
                systemId = game.systemId,
                wikiSeries = wikiSeries,
                wikiPrecededBy = wikiPrecededBy,
                wikiFollowedBy = wikiFollowedBy,
                wikiExtract = extract,
            )

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
                heroImageUrl = page.thumbUrl,
                sourceLabel = "Wikipedia",
                isLive = true,
                seriesName = resolvedSeries.seriesName,
                relatedGames = resolvedSeries.entries,
            )
        }

        internal fun extractEarliestYear(releaseRaw: String, leadText: String): String? {
            val yearRegex = Regex("""\b(19[789]\d|20[012]\d)\b""")
            val fromInfobox = yearRegex.findAll(releaseRaw).mapNotNull { it.groupValues[1].toIntOrNull() }.minOrNull()
            if (fromInfobox != null) return fromInfobox.toString()
            val fromLead = yearRegex.findAll(leadText).mapNotNull { it.groupValues[1].toIntOrNull() }.minOrNull()
            return fromLead?.toString()
        }

        internal fun buildCleanTagline(introSummary: String, fallbackTagline: String): String {
            if (introSummary.isBlank()) return fallbackTagline
            // Remove pronunciation / Japanese kana parentheticals like "(Japanese: スーパーマリオブラザーズ, ...)"
            val scrubbed = introSummary
                .replace(Regex("""\s*\([^)]*(?:Japanese|Hepburn|Korean|Chinese|IPA|listen|lit\.|stylized)[^)]*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\[[a-z0-9]+\]""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s{2,}"""), " ")
                .trim()
            val firstSentence = scrubbed.split(Regex("""(?<=[.!?])\s+""")).firstOrNull()?.trim().orEmpty()
            return when {
                firstSentence.length in 24..190 -> firstSentence
                firstSentence.length > 190 -> firstSentence.take(185).substringBeforeLast(' ') + "…"
                scrubbed.length >= 24 -> scrubbed.take(185).substringBeforeLast(' ') + "…"
                else -> fallbackTagline
            }
        }

        /** Extracts balanced `{{ ... }}` templates cleanly, handling nested templates. */
        private fun unwrapTemplates(input: String): String {
            val out = StringBuilder(input.length)
            var i = 0
            while (i < input.length) {
                if (i + 1 < input.length && input[i] == '{' && input[i + 1] == '{') {
                    var depth = 1
                    var j = i + 2
                    while (j < input.length && depth > 0) {
                        if (j + 1 < input.length && input[j] == '{' && input[j + 1] == '{') {
                            depth++; j += 2
                        } else if (j + 1 < input.length && input[j] == '}' && input[j + 1] == '}') {
                            depth--; j += 2
                        } else {
                            j++
                        }
                    }
                    val inner = if (depth == 0) input.substring(i + 2, j - 2) else input.substring(i + 2)
                    out.append(renderTemplate(inner))
                    i = j
                } else {
                    out.append(input[i])
                    i++
                }
            }
            return out.toString()
        }

        private fun splitTopLevelPipes(s: String): List<String> {
            val parts = mutableListOf<String>()
            var depthCurly = 0
            var depthSquare = 0
            var start = 0
            var i = 0
            while (i < s.length) {
                when {
                    i + 1 < s.length && s[i] == '{' && s[i + 1] == '{' -> { depthCurly++; i += 2; continue }
                    i + 1 < s.length && s[i] == '}' && s[i + 1] == '}' -> { if (depthCurly > 0) depthCurly--; i += 2; continue }
                    i + 1 < s.length && s[i] == '[' && s[i + 1] == '[' -> { depthSquare++; i += 2; continue }
                    i + 1 < s.length && s[i] == ']' && s[i + 1] == ']' -> { if (depthSquare > 0) depthSquare--; i += 2; continue }
                    s[i] == '|' && depthCurly == 0 && depthSquare == 0 -> {
                        parts += s.substring(start, i)
                        start = i + 1
                    }
                }
                i++
            }
            parts += s.substring(start)
            return parts
        }

        private fun renderTemplate(inner: String): String {
            val parts = splitTopLevelPipes(inner)
            val name = parts.firstOrNull()?.trim()?.lowercase().orEmpty()
            val args = parts.drop(1).map { unwrapTemplates(it).trim() }
            return when {
                name in setOf("vgrelease", "video game release", "vgr", "vgrelease new") -> {
                    val values = args.filter { it.isNotBlank() && !it.contains('=') }
                    // Pairs of (region, date) — extract the date values (containing a year or month)
                    val dates = values.filter { Regex("""\b(19\d\d|20\d\d)\b""").containsMatchIn(it) }
                    (if (dates.isNotEmpty()) dates else values).take(2).joinToString(" · ")
                }
                name in setOf("start date", "start date and age", "release date") -> {
                    val nums = args.filter { it.all(Char::isDigit) }
                    when (nums.size) {
                        in 3..Int.MAX_VALUE -> {
                            val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                            val m = nums[1].toIntOrNull()?.let { months.getOrNull(it - 1) }
                            if (m != null) "$m ${nums[2].toInt()}, ${nums[0]}" else nums[0]
                        }
                        2 -> {
                            val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                            val m = nums[1].toIntOrNull()?.let { months.getOrNull(it - 1) }
                            if (m != null) "$m ${nums[0]}" else nums[0]
                        }
                        1 -> nums[0]
                        else -> ""
                    }
                }
                name in setOf("ubl", "unbulleted list", "plainlist", "flatlist", "hlist", "collapsible list", "bulleted list") -> {
                    args.filter { it.isNotBlank() && !it.startsWith("title=") && !it.startsWith("class=") && !it.startsWith("style=") && !it.startsWith("framestyle=") && !it.startsWith("titlestyle=") }
                        .flatMap { it.split('\n') }
                        .map { it.trim().trimStart('*').trim() }
                        .filter { it.isNotBlank() && !it.contains('=') }
                        .take(3)
                        .joinToString(", ")
                }
                name in setOf("nowrap", "small", "lang", "nihongo") -> args.firstOrNull().orEmpty()
                else -> " "
            }
        }

        internal fun parseInfoboxField(wikitext: String, keys: List<String>): String {
            if (wikitext.isBlank()) return ""
            // Isolate the first {{Infobox ...}} block so we never accidentally read navboxes
            val boxStart = Regex("""\{\{\s*Infobox\b""", RegexOption.IGNORE_CASE).find(wikitext)?.range?.first ?: 0
            val scoped = wikitext.substring(boxStart)
            for (key in keys) {
                val regex = Regex("""(?im)^\s*\|\s*$key\s*=\s*(.+?)(?=\n\s*\|\s*[a-zA-Z0-9_ ]+\s*=|\n\s*\}\}|\z)""", RegexOption.DOT_MATCHES_ALL)
                val raw = regex.find(scoped)?.groupValues?.get(1) ?: continue
                val cleaned = cleanWikiMarkup(raw)
                if (cleaned.isNotBlank()) return cleaned.take(96)
            }
            return ""
        }

        internal fun cleanWikiMarkup(raw: String): String {
            var s = raw
            s = s.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), " ")
            s = s.replace(Regex("""<ref[^>]*>.*?</ref>""", RegexOption.DOT_MATCHES_ALL), " ")
            s = s.replace(Regex("""<ref[^/]*/>"""), " ")
            s = s.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), ", ")
            s = unwrapTemplates(s)
            // Replace [[Target|Display]] -> Display and [[Target]] -> Target
            s = s.replace(Regex("""\[\[[^\]|]+\|([^\]]+)\]\]"""), "$1")
            s = s.replace(Regex("""\[\[([^\]]+)\]\]"""), "$1")
            // Strip HTML tags and wiki formatting quotes
            s = s.replace(Regex("""<[^>]+>"""), " ")
            s = s.replace("'''", "").replace("''", "")
            s = s.replace(Regex("""^\s*\*+\s*""", RegexOption.MULTILINE), "")
            s = s.replace('\n', ' ')
            s = s.replace(Regex("""\s*,\s*,\s*"""), ", ")
            s = s.replace(Regex("""\s{2,}"""), " ").trim().trim(',', ';', '·', ' ')
            return s
        }

        private fun extractPlainLeadFromWikitext(wikitext: String): String {
            if (wikitext.isBlank()) return ""
            // Skip past top-level templates at the start of section 0
            var i = 0
            while (i < wikitext.length) {
                while (i < wikitext.length && wikitext[i].isWhitespace()) i++
                if (i + 1 < wikitext.length && wikitext[i] == '{' && wikitext[i + 1] == '{') {
                    var depth = 1
                    i += 2
                    while (i < wikitext.length && depth > 0) {
                        if (i + 1 < wikitext.length && wikitext[i] == '{' && wikitext[i + 1] == '{') { depth++; i += 2 }
                        else if (i + 1 < wikitext.length && wikitext[i] == '}' && wikitext[i + 1] == '}') { depth--; i += 2 }
                        else i++
                    }
                } else break
            }
            return cleanWikiMarkup(wikitext.substring(i).take(1200))
        }

        private fun cleanReleaseDate(raw: String, year: String): String {
            val cleaned = raw.trim()
            val dateMatches = Regex("""([A-Z][a-z]+ \d{1,2},? \d{4}|\d{1,2} [A-Z][a-z]+ \d{4}|[A-Z][a-z]+ \d{4})""")
                .findAll(cleaned)
                .map { it.value }
                .toList()
            val matchingYear = dateMatches.firstOrNull { year.isNotEmpty() && year in it }
            if (matchingYear != null) return matchingYear
            if (dateMatches.isNotEmpty()) return dateMatches.first()
            if (cleaned.length in 4..42 && year in cleaned) return cleaned
            return year.ifBlank { cleaned.take(36) }
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
                val paras = intro.split("\n\n").filter { it.length > 50 }
                if (paras.size >= 2) {
                    listOf(
                        GameDossier.FeatureBlock("DISCOVER ${title.uppercase()}", paras[1].take(320)),
                    ) + fallback.featureSections.take(1)
                } else {
                    fallback.featureSections
                }
            }
            val shortIntro = if (intro.length > 440) intro.take(440).substringBeforeLast(' ') + "…" else intro
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
