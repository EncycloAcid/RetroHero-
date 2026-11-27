package com.retrohero.app.emulator

import com.retrohero.app.data.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketGuideTest {

    private fun fakeGame(title: String, systemId: String) = Game(
        id = "$systemId:$title",
        fileName = "$title (USA).rom",
        systemId = systemId,
        treeUri = "tree",
        parentDocId = "doc",
        size = 1024L,
    )

    @Test
    fun pokemonTypeChartReturnsAccurateWeaknessesResistancesAndImmunities() {
        val dragon = PokemonTypeChartLookup.matchupsForDefender(PokemonType.DRAGON)
        assertEquals(listOf(PokemonType.ICE, PokemonType.DRAGON, PokemonType.FAIRY), dragon.weakTo)
        assertTrue(PokemonType.FIRE in dragon.resists)
        assertTrue(dragon.immuneTo.isEmpty())

        val ground = PokemonTypeChartLookup.matchupsForDefender(PokemonType.GROUND)
        assertEquals(listOf(PokemonType.ELECTRIC), ground.immuneTo)
        assertTrue(PokemonType.WATER in ground.weakTo)

        val ghost = PokemonTypeChartLookup.matchupsForDefender(PokemonType.GHOST)
        assertEquals(listOf(PokemonType.NORMAL, PokemonType.FIGHTING), ghost.immuneTo)

        val steel = PokemonTypeChartLookup.matchupsForDefender(PokemonType.STEEL)
        assertEquals(listOf(PokemonType.POISON), steel.immuneTo)
        assertTrue(steel.resists.size >= 10)
    }

    @Test
    fun spoilerSafeSearchMatchesTopicsWithoutLeakingSolutions() {
        val hints = listOf(
            PocketHint(
                topic = "Water Temple — Missing Small Key",
                nudge = "Check under something floating.",
                clue = "Central tower middle water level.",
                solution = "Secret basement chest behind eye switch.",
            ),
            PocketHint(
                topic = "Forest Temple — Twisted Corridor",
                nudge = "Look above the doorway.",
                clue = "Archery target.",
                solution = "Shoot the eye switch with Fairy Bow.",
            ),
        )

        val waterHits = PocketGuideSearch.filterHints(hints, "water")
        assertEquals(1, waterHits.size)
        assertEquals("Water Temple — Missing Small Key", waterHits.first().topic)

        // Searching a word that only appears in the spoiler solution must NOT match!
        val spoilerQuery = PocketGuideSearch.filterHints(hints, "Fairy Bow")
        assertTrue(spoilerQuery.isEmpty())

        // Blank query returns all hints
        assertEquals(2, PocketGuideSearch.filterHints(hints, "   ").size)
    }

    @Test
    fun curatedGuidesProvideRichReferencesForClassicFranchises() {
        val oot = PocketGuides.forGame(fakeGame("The Legend of Zelda: Ocarina of Time", "n64"))
        val ootSongs = oot.references.filterIsInstance<PocketReference.SongBook>().firstOrNull()
        assertTrue(ootSongs != null && ootSongs.songs.size >= 12)
        assertTrue(oot.hints.any { "Water Temple" in it.topic })

        val mm = PocketGuides.forGame(fakeGame("The Legend of Zelda: Majora's Mask", "n64"))
        val mmSongs = mm.references.filterIsInstance<PocketReference.SongBook>().firstOrNull()
        assertTrue(mmSongs != null && mmSongs.songs.any { "Inverted Song of Time" in it.title })

        val la = PocketGuides.forGame(fakeGame("Legend of Zelda: Link's Awakening DX", "gbc"))
        val laLegend = la.references.filterIsInstance<PocketReference.MapLegend>().firstOrNull()
        assertTrue(laLegend != null && laLegend.steps.size >= 12)

        val sf = PocketGuides.forGame(fakeGame("Street Fighter II Turbo", "snes"))
        val sfMoves = sf.references.filterIsInstance<PocketReference.MoveList>().firstOrNull()
        assertTrue(sfMoves != null && sfMoves.moves.any { it.name == "Hadouken" } && sfMoves.moves.any { it.name == "Shoryuken" })

        val sotn = PocketGuides.forGame(fakeGame("Castlevania: Symphony of the Night", "psx"))
        assertTrue(sotn.references.filterIsInstance<PocketReference.MoveList>().first().moves.any { it.name == "Soul Steal" })

        val mgs = PocketGuides.forGame(fakeGame("Metal Gear Solid", "psx"))
        val mgsCodec = mgs.references.filterIsInstance<PocketReference.MapLegend>().first()
        assertTrue(mgsCodec.steps.any { it.label == "140.15" })

        val poke = PocketGuides.forGame(fakeGame("Pokémon Emerald", "gba"))
        assertTrue(poke.references.any { it is PocketReference.PokemonTypeChart })

        val ff = PocketGuides.forGame(fakeGame("Final Fantasy VI", "snes"))
        assertTrue(ff.references.filterIsInstance<PocketReference.MoveList>().first().moves.any { it.name == "Suplex" })

        val fallback = PocketGuides.forGame(fakeGame("Obscure Homebrew Adventure", "nds"))
        assertFalse(fallback.controls.isEmpty())
        assertFalse(fallback.hints.isEmpty())
    }

    @Test
    fun pokemonGuidesPrioritizeMatchingRegionHints() {
        val platinum = PocketGuides.forGame(fakeGame("Pokemon - Platinum Version", "nds"))
        assertEquals("Trainer's Guide", platinum.title)
        assertTrue(platinum.badge.startsWith("Sinnoh"))
        assertTrue(platinum.hints.first().topic.contains("Sinnoh"))
        assertTrue(platinum.hints.any { "Distortion World" in it.topic })
        assertTrue(platinum.hints.any { "Iron Island" in it.solution && "Solaceon Ruins" in it.solution })

        val emerald = PocketGuides.forGame(fakeGame("Pokemon - Emerald Version", "gba"))
        assertTrue(emerald.badge.startsWith("Hoenn"))
        assertTrue(emerald.hints.first().topic.contains("Hoenn"))

        val heartGold = PocketGuides.forGame(fakeGame("Pokemon - HeartGold Version", "nds"))
        assertTrue(heartGold.badge.startsWith("Johto"))
        assertTrue(heartGold.hints.first().topic.contains("Johto"))
    }
}
