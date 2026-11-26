package com.retrohero.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Authenticated RetroAchievements account credentials (token-only; password is never persisted). */
data class RaProfile(
    val username: String,
    val token: String,
    val score: Int = 0,
    val softcoreScore: Int = 0,
) {
    val avatarUrl: String get() = "https://media.retroachievements.org/UserPic/$username.png"
}

/** A single RetroAchievements trophy from `dorequest.php?r=patch`. */
data class RaAchievement(
    val id: Int,
    val title: String,
    val description: String,
    val points: Int,
    val badgeName: String,
    val memAddr: String,
    val flags: Int = 3, // 3 = Official Core, 5 = Unofficial
    val type: String = "", // "progression", "win_condition", "missable", or ""
    val unlocked: Boolean = false,
    val unlockedHardcore: Boolean = false,
) {
    val isCore: Boolean get() = flags == 3
    val badgeUrl: String get() = "https://media.retroachievements.org/Badge/$badgeName.png"
    val lockedBadgeUrl: String get() = "https://media.retroachievements.org/Badge/${badgeName}_lock.png"
    val displayBadgeUrl: String get() = if (unlocked) badgeUrl else lockedBadgeUrl

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("description", description)
        .put("points", points)
        .put("badgeName", badgeName)
        .put("memAddr", memAddr)
        .put("flags", flags)
        .put("type", type)
        .put("unlocked", unlocked)
        .put("unlockedHardcore", unlockedHardcore)

    companion object {
        fun fromJson(o: JSONObject) = RaAchievement(
            id = o.optInt("id"),
            title = o.optString("title"),
            description = o.optString("description"),
            points = o.optInt("points"),
            badgeName = o.optString("badgeName"),
            memAddr = o.optString("memAddr"),
            flags = o.optInt("flags", 3),
            type = o.optString("type"),
            unlocked = o.optBoolean("unlocked", false),
            unlockedHardcore = o.optBoolean("unlockedHardcore", false),
        )
    }
}

/** Full RetroAchievements trophy set & status for a specific hashed ROM. */
data class RaGameData(
    val gameId: Int,
    val romHash: String,
    val title: String,
    val imageIconUrl: String?,
    val richPresencePatch: String,
    val achievements: List<RaAchievement>,
    val hardcoreEnabled: Boolean,
) {
    val coreAchievements: List<RaAchievement> get() = achievements.filter { it.isCore }
    val unlockedCount: Int get() = coreAchievements.count { it.unlocked }
    val totalCount: Int get() = coreAchievements.size
    val earnedPoints: Int get() = coreAchievements.filter { it.unlocked }.sumOf { it.points }
    val totalPoints: Int get() = coreAchievements.sumOf { it.points }
    val completionPct: Int get() = if (totalCount == 0) 0 else (unlockedCount * 100) / totalCount

    fun toJson(): JSONObject = JSONObject()
        .put("gameId", gameId)
        .put("romHash", romHash)
        .put("title", title)
        .put("imageIconUrl", imageIconUrl ?: JSONObject.NULL)
        .put("richPresencePatch", richPresencePatch)
        .put("hardcoreEnabled", hardcoreEnabled)
        .put(
            "achievements",
            JSONArray().apply { achievements.forEach { put(it.toJson()) } },
        )

    companion object {
        fun fromJson(o: JSONObject): RaGameData {
            val arr = o.optJSONArray("achievements") ?: JSONArray()
            val list = buildList {
                for (i in 0 until arr.length()) add(RaAchievement.fromJson(arr.getJSONObject(i)))
            }
            return RaGameData(
                gameId = o.optInt("gameId"),
                romHash = o.optString("romHash"),
                title = o.optString("title"),
                imageIconUrl = o.optString("imageIconUrl").takeIf { it.isNotBlank() && it != "null" },
                richPresencePatch = o.optString("richPresencePatch"),
                achievements = list,
                hardcoreEnabled = o.optBoolean("hardcoreEnabled", false),
            )
        }
    }
}

/**
 * Official RetroAchievements Connect API client (`https://retroachievements.org/dorequest.php`).
 */
object RetroAchievementsApi {
    private const val ENDPOINT = "https://retroachievements.org/dorequest.php"
    private const val USER_AGENT = "RetroHero/0.2.22 (Android Retro Emulator)"

    fun login(username: String, password: String): Result<RaProfile> {
        val u = username.trim()
        if (u.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter both username and password"))
        }
        val body = "r=login2&u=${enc(u)}&p=${enc(password)}"
        val json = postForm(body) ?: return Result.failure(IllegalStateException("Could not reach RetroAchievements.org"))
        if (!json.optBoolean("Success", false)) {
            val err = json.optString("Error").ifBlank { "Invalid username or password" }
            return Result.failure(IllegalStateException(err))
        }
        val token = json.optString("Token")
        if (token.isBlank()) return Result.failure(IllegalStateException("RetroAchievements returned an empty token"))
        return Result.success(
            RaProfile(
                username = json.optString("User").ifBlank { u },
                token = token,
                score = json.optInt("Score", 0),
                softcoreScore = json.optInt("SoftcoreScore", 0),
            ),
        )
    }

    fun lookupGameId(romHash: String): Int? {
        val json = getQuery("r=gameid&m=${enc(romHash)}") ?: return null
        if (!json.optBoolean("Success", false)) return null
        val id = json.optInt("GameID", 0)
        return id.takeIf { it > 0 }
    }

    fun fetchPatchAndUnlocks(
        profile: RaProfile,
        gameId: Int,
        romHash: String,
        hardcore: Boolean,
        includeUnofficial: Boolean,
    ): RaGameData? {
        val patchJson = getQuery("r=patch&u=${enc(profile.username)}&t=${enc(profile.token)}&g=$gameId")
            ?: return null
        if (!patchJson.optBoolean("Success", false)) return null
        val patch = patchJson.optJSONObject("PatchData") ?: return null

        val softUnlocks = fetchUnlockSet(profile, gameId, hardcore = false)
        val hardUnlocks = fetchUnlockSet(profile, gameId, hardcore = true)

        val arr = patch.optJSONArray("Achievements") ?: JSONArray()
        val achievements = buildList {
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                val flags = a.optInt("Flags", 3)
                if (flags != 3 && (!includeUnofficial || flags != 5)) continue
                val id = a.optInt("ID", 0)
                if (id <= 0) continue
                val inHard = id in hardUnlocks
                val inSoft = id in softUnlocks
                val unlocked = if (hardcore) inHard else (inSoft || inHard)
                add(
                    RaAchievement(
                        id = id,
                        title = a.optString("Title"),
                        description = a.optString("Description"),
                        points = a.optInt("Points", 0),
                        badgeName = a.optString("BadgeName"),
                        memAddr = a.optString("MemAddr"),
                        flags = flags,
                        type = a.optString("Type"),
                        unlocked = unlocked,
                        unlockedHardcore = inHard,
                    ),
                )
            }
        }

        val iconUrl = patch.optString("ImageIconURL").takeIf { it.isNotBlank() }
            ?: patch.optString("ImageIcon").takeIf { it.isNotBlank() }?.let { "https://media.retroachievements.org$it" }

        return RaGameData(
            gameId = gameId,
            romHash = romHash,
            title = patch.optString("Title"),
            imageIconUrl = iconUrl,
            richPresencePatch = patch.optString("RichPresencePatch"),
            achievements = achievements,
            hardcoreEnabled = hardcore,
        )
    }

    fun fetchUnlockSet(profile: RaProfile, gameId: Int, hardcore: Boolean): Set<Int> {
        val h = if (hardcore) 1 else 0
        val json = getQuery("r=unlocks&u=${enc(profile.username)}&t=${enc(profile.token)}&g=$gameId&h=$h")
            ?: return emptySet()
        if (!json.optBoolean("Success", false)) return emptySet()
        val arr = json.optJSONArray("UserUnlocks") ?: return emptySet()
        return buildSet {
            for (i in 0 until arr.length()) add(arr.optInt(i))
        }
    }

    fun startSession(profile: RaProfile, gameId: Int, hardcore: Boolean, romHash: String): Boolean {
        val h = if (hardcore) 1 else 0
        val json = getQuery("r=startsession&u=${enc(profile.username)}&t=${enc(profile.token)}&g=$gameId&h=$h&m=${enc(romHash)}")
        return json?.optBoolean("Success", false) == true
    }

    fun ping(profile: RaProfile, gameId: Int, richPresence: String = ""): Boolean {
        val body = buildString {
            append("r=ping&u=").append(enc(profile.username))
            append("&t=").append(enc(profile.token))
            append("&g=").append(gameId)
            if (richPresence.isNotBlank()) append("&m=").append(enc(richPresence))
        }
        return postForm(body)?.optBoolean("Success", false) == true
    }

    /**
     * Permanently awards [achievementId] on RetroAchievements.org.
     * Returns updated (hardcoreScore, softcoreScore) on success.
     */
    fun awardAchievement(
        profile: RaProfile,
        achievementId: Int,
        hardcore: Boolean,
        romHash: String,
    ): Result<Pair<Int, Int>> {
        val h = if (hardcore) 1 else 0
        // Official rcheevos validation hash: MD5("<id><username><hardcore>")
        val validation = md5Hex("$achievementId${profile.username}$h")
        val q = "r=awardachievement&u=${enc(profile.username)}&t=${enc(profile.token)}" +
            "&a=$achievementId&h=$h&m=${enc(romHash)}&v=$validation"
        val json = getQuery(q) ?: return Result.failure(IllegalStateException("Network unavailable"))
        if (!json.optBoolean("Success", false)) {
            val err = json.optString("Error")
            // "User already has this achievement unlocked" is still a successful state
            if ("already" in err.lowercase()) {
                return Result.success(profile.score to profile.softcoreScore)
            }
            return Result.failure(IllegalStateException(err.ifBlank { "Award failed" }))
        }
        val score = json.optInt("Score", profile.score)
        val soft = json.optInt("SoftcoreScore", profile.softcoreScore)
        return Result.success(score to soft)
    }

    internal fun md5Hex(input: String): String =
        MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    private fun getQuery(query: String): JSONObject? {
        val conn = (URL("$ENDPOINT?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 7500
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return readJson(conn)
    }

    private fun postForm(formBody: String): JSONObject? {
        val bytes = formBody.toByteArray(Charsets.UTF_8)
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 6000
            readTimeout = 7500
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
            setFixedLengthStreamingMode(bytes.size)
        }
        return try {
            conn.outputStream.use { it.write(bytes) }
            readJson(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun readJson(conn: HttpURLConnection): JSONObject? = try {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        stream?.bufferedReader()?.use { JSONObject(it.readText()) }
    } catch (_: Exception) {
        null
    } finally {
        conn.disconnect()
    }
}

/**
 * Caches RetroAchievements per-game trophy sets & hashes on disk for instant UI rendering.
 */
class RetroAchievementsRepository(
    private val context: Context,
    private val cacheDir: File,
) {
    private val memory = ConcurrentHashMap<String, RaGameData>()
    private val hashCacheFile = File(cacheDir, "hashes.json")

    private fun gameCacheFile(game: Game): File {
        val hash = RetroAchievementsApi.md5Hex("${game.systemId}:${game.id}").take(24)
        return File(cacheDir, "ra_$hash.json")
    }

    fun peekCached(game: Game): RaGameData? {
        memory[game.id]?.let { return it }
        val f = gameCacheFile(game)
        if (!f.exists()) return null
        return runCatching { RaGameData.fromJson(JSONObject(f.readText())) }
            .getOrNull()
            ?.also { memory[game.id] = it }
    }

    suspend fun loadForGame(
        game: Game,
        profile: RaProfile?,
        hardcore: Boolean,
        includeUnofficial: Boolean,
        forceRefresh: Boolean = false,
    ): RaGameData? = withContext(Dispatchers.IO) {
        if (profile == null) return@withContext null
        if (!forceRefresh) {
            peekCached(game)?.takeIf { it.hardcoreEnabled == hardcore }?.let { return@withContext it }
        }
        val hash = RetroAchievementsHasher.hashGame(context, game, hashCacheFile) ?: return@withContext null
        val gameId = RetroAchievementsApi.lookupGameId(hash) ?: return@withContext null
        val fresh = RetroAchievementsApi.fetchPatchAndUnlocks(profile, gameId, hash, hardcore, includeUnofficial)
            ?: return@withContext peekCached(game)
        memory[game.id] = fresh
        runCatching { writeAtomic(gameCacheFile(game), fresh.toJson().toString()) }
        fresh
    }

    fun markUnlockedLocally(game: Game, achievementId: Int, hardcore: Boolean) {
        val current = peekCached(game) ?: return
        val updated = current.copy(
            achievements = current.achievements.map { a ->
                if (a.id == achievementId) a.copy(unlocked = true, unlockedHardcore = a.unlockedHardcore || hardcore)
                else a
            },
        )
        memory[game.id] = updated
        runCatching { writeAtomic(gameCacheFile(game), updated.toJson().toString()) }
    }

    fun clearMemoryCache() {
        memory.clear()
    }
}
