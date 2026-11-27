package com.retrohero.app.emulator

import androidx.compose.ui.graphics.Color

/**
 * Interactive reference sheets bundled inside the Spoiler-Free Strategy Guide drawer:
 * Pokémon type matchup charts, fighting-game move lists, Ocarina/melody songbooks,
 * and dungeon/overworld route legends.
 */
sealed interface PocketReference {
    val title: String

    data class PokemonTypeChart(
        override val title: String = "Interactive Type Matchup Chart",
    ) : PocketReference

    data class MoveList(
        override val title: String,
        val subtitle: String,
        val moves: List<MoveEntry>,
    ) : PocketReference

    data class SongBook(
        override val title: String,
        val subtitle: String,
        val songs: List<SongEntry>,
    ) : PocketReference

    data class MapLegend(
        override val title: String,
        val subtitle: String,
        val steps: List<RouteStep>,
    ) : PocketReference
}

data class MoveEntry(
    val group: String,
    val name: String,
    val input: String,
    val note: String = "",
)

data class SongEntry(
    val title: String,
    val notes: List<String>,
    val effect: String,
)

data class RouteStep(
    val label: String,
    val detail: String,
)

enum class PokemonType(val label: String, val color: Color) {
    NORMAL("Normal", Color(0xFFA8A77A)),
    FIRE("Fire", Color(0xFFEE8130)),
    WATER("Water", Color(0xFF6390F0)),
    ELECTRIC("Electric", Color(0xFFF7D02C)),
    GRASS("Grass", Color(0xFF7AC74C)),
    ICE("Ice", Color(0xFF96D9D6)),
    FIGHTING("Fighting", Color(0xFFC22E28)),
    POISON("Poison", Color(0xFFA33EA1)),
    GROUND("Ground", Color(0xFFE2BF65)),
    FLYING("Flying", Color(0xFFA98FF3)),
    PSYCHIC("Psychic", Color(0xFFF95587)),
    BUG("Bug", Color(0xFFA6B91A)),
    ROCK("Rock", Color(0xFFB6A136)),
    GHOST("Ghost", Color(0xFF735797)),
    DRAGON("Dragon", Color(0xFF6F35FC)),
    DARK("Dark", Color(0xFF705746)),
    STEEL("Steel", Color(0xFFB7B7CE)),
    FAIRY("Fairy", Color(0xFFD685AD)),
}

data class TypeMatchupSummary(
    val defender: PokemonType,
    val weakTo: List<PokemonType>,
    val resists: List<PokemonType>,
    val immuneTo: List<PokemonType>,
)

/**
 * Standard Pokémon defensive type effectiveness lookup so players can tap any opponent's
 * type mid-battle and immediately see 2× weaknesses, ½× resistances, and 0× immunities.
 */
object PokemonTypeChartLookup {

    private val table: Map<PokemonType, Triple<List<PokemonType>, List<PokemonType>, List<PokemonType>>> = mapOf(
        PokemonType.NORMAL to Triple(
            listOf(PokemonType.FIGHTING),
            emptyList(),
            listOf(PokemonType.GHOST),
        ),
        PokemonType.FIRE to Triple(
            listOf(PokemonType.WATER, PokemonType.GROUND, PokemonType.ROCK),
            listOf(PokemonType.FIRE, PokemonType.GRASS, PokemonType.ICE, PokemonType.BUG, PokemonType.STEEL, PokemonType.FAIRY),
            emptyList(),
        ),
        PokemonType.WATER to Triple(
            listOf(PokemonType.ELECTRIC, PokemonType.GRASS),
            listOf(PokemonType.FIRE, PokemonType.WATER, PokemonType.ICE, PokemonType.STEEL),
            emptyList(),
        ),
        PokemonType.ELECTRIC to Triple(
            listOf(PokemonType.GROUND),
            listOf(PokemonType.ELECTRIC, PokemonType.FLYING, PokemonType.STEEL),
            emptyList(),
        ),
        PokemonType.GRASS to Triple(
            listOf(PokemonType.FIRE, PokemonType.ICE, PokemonType.POISON, PokemonType.FLYING, PokemonType.BUG),
            listOf(PokemonType.WATER, PokemonType.ELECTRIC, PokemonType.GRASS, PokemonType.GROUND),
            emptyList(),
        ),
        PokemonType.ICE to Triple(
            listOf(PokemonType.FIRE, PokemonType.FIGHTING, PokemonType.ROCK, PokemonType.STEEL),
            listOf(PokemonType.ICE),
            emptyList(),
        ),
        PokemonType.FIGHTING to Triple(
            listOf(PokemonType.FLYING, PokemonType.PSYCHIC, PokemonType.FAIRY),
            listOf(PokemonType.BUG, PokemonType.ROCK, PokemonType.DARK),
            emptyList(),
        ),
        PokemonType.POISON to Triple(
            listOf(PokemonType.GROUND, PokemonType.PSYCHIC),
            listOf(PokemonType.GRASS, PokemonType.FIGHTING, PokemonType.POISON, PokemonType.BUG, PokemonType.FAIRY),
            emptyList(),
        ),
        PokemonType.GROUND to Triple(
            listOf(PokemonType.WATER, PokemonType.GRASS, PokemonType.ICE),
            listOf(PokemonType.POISON, PokemonType.ROCK),
            listOf(PokemonType.ELECTRIC),
        ),
        PokemonType.FLYING to Triple(
            listOf(PokemonType.ELECTRIC, PokemonType.ICE, PokemonType.ROCK),
            listOf(PokemonType.GRASS, PokemonType.FIGHTING, PokemonType.BUG),
            listOf(PokemonType.GROUND),
        ),
        PokemonType.PSYCHIC to Triple(
            listOf(PokemonType.BUG, PokemonType.GHOST, PokemonType.DARK),
            listOf(PokemonType.FIGHTING, PokemonType.PSYCHIC),
            emptyList(),
        ),
        PokemonType.BUG to Triple(
            listOf(PokemonType.FIRE, PokemonType.FLYING, PokemonType.ROCK),
            listOf(PokemonType.GRASS, PokemonType.FIGHTING, PokemonType.GROUND),
            emptyList(),
        ),
        PokemonType.ROCK to Triple(
            listOf(PokemonType.WATER, PokemonType.GRASS, PokemonType.FIGHTING, PokemonType.GROUND, PokemonType.STEEL),
            listOf(PokemonType.NORMAL, PokemonType.FIRE, PokemonType.POISON, PokemonType.FLYING),
            emptyList(),
        ),
        PokemonType.GHOST to Triple(
            listOf(PokemonType.GHOST, PokemonType.DARK),
            listOf(PokemonType.POISON, PokemonType.BUG),
            listOf(PokemonType.NORMAL, PokemonType.FIGHTING),
        ),
        PokemonType.DRAGON to Triple(
            listOf(PokemonType.ICE, PokemonType.DRAGON, PokemonType.FAIRY),
            listOf(PokemonType.FIRE, PokemonType.WATER, PokemonType.ELECTRIC, PokemonType.GRASS),
            emptyList(),
        ),
        PokemonType.DARK to Triple(
            listOf(PokemonType.FIGHTING, PokemonType.BUG, PokemonType.FAIRY),
            listOf(PokemonType.GHOST, PokemonType.DARK),
            listOf(PokemonType.PSYCHIC),
        ),
        PokemonType.STEEL to Triple(
            listOf(PokemonType.FIRE, PokemonType.FIGHTING, PokemonType.GROUND),
            listOf(
                PokemonType.NORMAL, PokemonType.GRASS, PokemonType.ICE, PokemonType.FLYING,
                PokemonType.PSYCHIC, PokemonType.BUG, PokemonType.ROCK, PokemonType.DRAGON,
                PokemonType.STEEL, PokemonType.FAIRY,
            ),
            listOf(PokemonType.POISON),
        ),
        PokemonType.FAIRY to Triple(
            listOf(PokemonType.POISON, PokemonType.STEEL),
            listOf(PokemonType.FIGHTING, PokemonType.BUG, PokemonType.DARK),
            listOf(PokemonType.DRAGON),
        ),
    )

    fun matchupsForDefender(defender: PokemonType): TypeMatchupSummary {
        val (weak, resists, immune) = table[defender] ?: Triple(emptyList(), emptyList(), emptyList())
        return TypeMatchupSummary(defender, weak, resists, immune)
    }
}

/**
 * Spoiler-safe hint search: only matches hint topics so typing a keyword never leaks
 * unrevealed clues or exact solutions.
 */
object PocketGuideSearch {
    fun filterHints(hints: List<PocketHint>, query: String): List<PocketHint> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return hints
        return hints.filter { hint -> q in hint.topic.lowercase() }
    }
}
