package com.retrohero.app.data

import org.json.JSONObject

/** A chronological prequel, sequel, or companion entry in a game's franchise. */
data class RelatedGame(
    val title: String,
    /** "Prequel", "Sequel", or "Series" */
    val relation: String,
    val year: String = "",
    val systemHint: String = "",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("title", title)
        .put("relation", relation)
        .put("year", year)
        .put("systemHint", systemHint)

    companion object {
        fun fromJson(o: JSONObject) = RelatedGame(
            title = o.optString("title"),
            relation = o.optString("relation", "Series"),
            year = o.optString("year"),
            systemHint = o.optString("systemHint"),
        )
    }
}

data class SeriesResolution(
    val seriesName: String,
    val entries: List<RelatedGame>,
)

/**
 * Resolves chronological prequels & sequels for retro games by combining:
 * 1. Curated chronological timelines for 33 iconic gaming franchises
 * 2. Wikipedia infobox (`series`, `preceded_by`, `followed_by`) and lead-sentence mentions
 * 3. Smart library cross-matching (linking directly to owned prequels/sequels in the user's ROMs)
 */
object GameSeries {

    private data class TimelineItem(
        val title: String,
        val year: String,
        val sys: String,
        val aliases: List<String> = emptyList(),
    )

    private data class FranchiseTimeline(
        val name: String,
        val items: List<TimelineItem>,
    )

    private val franchises: List<FranchiseTimeline> = listOf(
        FranchiseTimeline(
            "Super Mario",
            listOf(
                TimelineItem("Mario Bros.", "1983", "NES"),
                TimelineItem("Super Mario Bros.", "1985", "NES"),
                TimelineItem("Super Mario Bros.: The Lost Levels", "1986", "FDS", listOf("super mario bros 2 japan")),
                TimelineItem("Super Mario Bros. 2", "1988", "NES"),
                TimelineItem("Super Mario Bros. 3", "1988", "NES"),
                TimelineItem("Super Mario Land", "1989", "GB"),
                TimelineItem("Super Mario World", "1990", "SNES"),
                TimelineItem("Super Mario Land 2: 6 Golden Coins", "1992", "GB", listOf("super mario land 2")),
                TimelineItem("Super Mario World 2: Yoshi's Island", "1995", "SNES", listOf("yoshis island")),
                TimelineItem("Super Mario 64", "1996", "N64"),
                TimelineItem("Super Mario Sunshine", "2002", "GC"),
                TimelineItem("New Super Mario Bros.", "2006", "NDS"),
                TimelineItem("Super Mario Galaxy", "2007", "Wii"),
                TimelineItem("Super Mario Galaxy 2", "2010", "Wii"),
                TimelineItem("Super Mario 3D Land", "2011", "3DS"),
                TimelineItem("Super Mario 3D World", "2013", "Wii U"),
                TimelineItem("Super Mario Odyssey", "2017", "Switch"),
                TimelineItem("Super Mario Bros. Wonder", "2023", "Switch"),
            ),
        ),
        FranchiseTimeline(
            "Mario Kart",
            listOf(
                TimelineItem("Super Mario Kart", "1992", "SNES"),
                TimelineItem("Mario Kart 64", "1996", "N64"),
                TimelineItem("Mario Kart: Super Circuit", "2001", "GBA"),
                TimelineItem("Mario Kart: Double Dash!!", "2003", "GC", listOf("mario kart double dash")),
                TimelineItem("Mario Kart DS", "2005", "NDS"),
                TimelineItem("Mario Kart Wii", "2008", "Wii"),
                TimelineItem("Mario Kart 7", "2011", "3DS"),
                TimelineItem("Mario Kart 8", "2014", "Wii U"),
            ),
        ),
        FranchiseTimeline(
            "The Legend of Zelda",
            listOf(
                TimelineItem("The Legend of Zelda", "1986", "NES", listOf("legend of zelda")),
                TimelineItem("Zelda II: The Adventure of Link", "1987", "NES", listOf("zelda 2", "adventure of link")),
                TimelineItem("The Legend of Zelda: A Link to the Past", "1991", "SNES", listOf("a link to the past")),
                TimelineItem("The Legend of Zelda: Link's Awakening", "1993", "GB", listOf("links awakening", "links awakening dx")),
                TimelineItem("The Legend of Zelda: Ocarina of Time", "1998", "N64", listOf("ocarina of time")),
                TimelineItem("The Legend of Zelda: Majora's Mask", "2000", "N64", listOf("majoras mask")),
                TimelineItem("The Legend of Zelda: Oracle of Seasons & Ages", "2001", "GBC", listOf("oracle of seasons", "oracle of ages")),
                TimelineItem("The Legend of Zelda: The Wind Waker", "2002", "GC", listOf("the wind waker", "wind waker")),
                TimelineItem("The Legend of Zelda: The Minish Cap", "2004", "GBA", listOf("the minish cap", "minish cap")),
                TimelineItem("The Legend of Zelda: Twilight Princess", "2006", "GC", listOf("twilight princess")),
                TimelineItem("The Legend of Zelda: Phantom Hourglass", "2007", "NDS", listOf("phantom hourglass")),
                TimelineItem("The Legend of Zelda: Spirit Tracks", "2009", "NDS", listOf("spirit tracks")),
                TimelineItem("The Legend of Zelda: A Link Between Worlds", "2013", "3DS", listOf("a link between worlds")),
            ),
        ),
        FranchiseTimeline(
            "Sonic the Hedgehog",
            listOf(
                TimelineItem("Sonic the Hedgehog", "1991", "Genesis"),
                TimelineItem("Sonic the Hedgehog 2", "1992", "Genesis", listOf("sonic 2")),
                TimelineItem("Sonic CD", "1993", "Sega CD", listOf("sonic the hedgehog cd")),
                TimelineItem("Sonic the Hedgehog 3", "1994", "Genesis", listOf("sonic 3")),
                TimelineItem("Sonic & Knuckles", "1994", "Genesis", listOf("sonic and knuckles", "sonic 3 and knuckles")),
                TimelineItem("Sonic Adventure", "1998", "Dreamcast"),
                TimelineItem("Sonic Adventure 2", "2001", "Dreamcast"),
                TimelineItem("Sonic Advance", "2001", "GBA"),
                TimelineItem("Sonic Advance 2", "2002", "GBA"),
                TimelineItem("Sonic Heroes", "2003", "GC"),
                TimelineItem("Sonic Advance 3", "2004", "GBA"),
                TimelineItem("Sonic Rush", "2005", "NDS"),
            ),
        ),
        FranchiseTimeline(
            "Metroid",
            listOf(
                TimelineItem("Metroid", "1986", "NES"),
                TimelineItem("Metroid II: Return of Samus", "1991", "GB", listOf("metroid 2")),
                TimelineItem("Super Metroid", "1994", "SNES"),
                TimelineItem("Metroid Fusion", "2002", "GBA"),
                TimelineItem("Metroid Prime", "2002", "GC"),
                TimelineItem("Metroid: Zero Mission", "2004", "GBA"),
                TimelineItem("Metroid Prime 2: Echoes", "2004", "GC", listOf("metroid prime 2")),
                TimelineItem("Metroid Prime 3: Corruption", "2007", "Wii", listOf("metroid prime 3")),
                TimelineItem("Metroid: Samus Returns", "2017", "3DS"),
            ),
        ),
        FranchiseTimeline(
            "Mega Man",
            listOf(
                TimelineItem("Mega Man", "1987", "NES", listOf("rockman")),
                TimelineItem("Mega Man 2", "1988", "NES", listOf("mega man ii", "rockman 2")),
                TimelineItem("Mega Man 3", "1990", "NES", listOf("mega man iii", "rockman 3")),
                TimelineItem("Mega Man 4", "1991", "NES", listOf("mega man iv", "rockman 4")),
                TimelineItem("Mega Man 5", "1992", "NES", listOf("mega man v", "rockman 5")),
                TimelineItem("Mega Man 6", "1993", "NES", listOf("mega man vi", "rockman 6")),
                TimelineItem("Mega Man 7", "1995", "SNES", listOf("mega man vii")),
                TimelineItem("Mega Man 8", "1996", "PS1", listOf("mega man viii")),
            ),
        ),
        FranchiseTimeline(
            "Mega Man X",
            listOf(
                TimelineItem("Mega Man X", "1993", "SNES", listOf("rockman x")),
                TimelineItem("Mega Man X2", "1994", "SNES"),
                TimelineItem("Mega Man X3", "1995", "SNES"),
                TimelineItem("Mega Man X4", "1997", "PS1"),
                TimelineItem("Mega Man X5", "2000", "PS1"),
                TimelineItem("Mega Man X6", "2001", "PS1"),
            ),
        ),
        FranchiseTimeline(
            "Pokémon",
            listOf(
                TimelineItem("Pokémon Red & Blue", "1996", "GB", listOf("pokemon red", "pokemon blue", "pokemon green")),
                TimelineItem("Pokémon Yellow", "1998", "GB", listOf("pokemon yellow version")),
                TimelineItem("Pokémon Gold & Silver", "1999", "GBC", listOf("pokemon gold", "pokemon silver")),
                TimelineItem("Pokémon Crystal", "2000", "GBC", listOf("pokemon crystal version")),
                TimelineItem("Pokémon Ruby & Sapphire", "2002", "GBA", listOf("pokemon ruby", "pokemon sapphire")),
                TimelineItem("Pokémon FireRed & LeafGreen", "2004", "GBA", listOf("pokemon firered", "pokemon fire red", "pokemon leafgreen", "pokemon leaf green")),
                TimelineItem("Pokémon Emerald", "2004", "GBA", listOf("pokemon emerald version")),
                TimelineItem("Pokémon Diamond & Pearl", "2006", "NDS", listOf("pokemon diamond", "pokemon pearl")),
                TimelineItem("Pokémon Platinum", "2008", "NDS"),
                TimelineItem("Pokémon HeartGold & SoulSilver", "2009", "NDS", listOf("pokemon heartgold", "pokemon soulsilver")),
                TimelineItem("Pokémon Black & White", "2010", "NDS", listOf("pokemon black", "pokemon white")),
                TimelineItem("Pokémon Black 2 & White 2", "2012", "NDS", listOf("pokemon black 2", "pokemon white 2")),
                TimelineItem("Pokémon X & Y", "2013", "3DS", listOf("pokemon x", "pokemon y")),
                TimelineItem("Pokémon Sun & Moon", "2016", "3DS", listOf("pokemon sun", "pokemon moon")),
            ),
        ),
        FranchiseTimeline(
            "Final Fantasy",
            listOf(
                TimelineItem("Final Fantasy", "1987", "NES"),
                TimelineItem("Final Fantasy II", "1988", "NES", listOf("final fantasy 2")),
                TimelineItem("Final Fantasy III", "1990", "NES", listOf("final fantasy 3")),
                TimelineItem("Final Fantasy IV", "1991", "SNES", listOf("final fantasy 4")),
                TimelineItem("Final Fantasy V", "1992", "SNES", listOf("final fantasy 5")),
                TimelineItem("Final Fantasy VI", "1994", "SNES", listOf("final fantasy 6")),
                TimelineItem("Final Fantasy VII", "1997", "PS1", listOf("final fantasy 7")),
                TimelineItem("Final Fantasy VIII", "1999", "PS1", listOf("final fantasy 8")),
                TimelineItem("Final Fantasy IX", "2000", "PS1", listOf("final fantasy 9")),
                TimelineItem("Final Fantasy X", "2001", "PS2", listOf("final fantasy 10")),
                TimelineItem("Final Fantasy XII", "2006", "PS2", listOf("final fantasy 12")),
            ),
        ),
        FranchiseTimeline(
            "Castlevania",
            listOf(
                TimelineItem("Castlevania", "1986", "NES"),
                TimelineItem("Castlevania II: Simon's Quest", "1987", "NES", listOf("castlevania 2")),
                TimelineItem("Castlevania III: Dracula's Curse", "1989", "NES", listOf("castlevania 3")),
                TimelineItem("Super Castlevania IV", "1991", "SNES", listOf("castlevania 4")),
                TimelineItem("Castlevania: Rondo of Blood", "1993", "PCE-CD"),
                TimelineItem("Castlevania: Bloodlines", "1994", "Genesis"),
                TimelineItem("Castlevania: Symphony of the Night", "1997", "PS1", listOf("symphony of the night")),
                TimelineItem("Castlevania: Circle of the Moon", "2001", "GBA"),
                TimelineItem("Castlevania: Harmony of Dissonance", "2002", "GBA"),
                TimelineItem("Castlevania: Aria of Sorrow", "2003", "GBA"),
                TimelineItem("Castlevania: Dawn of Sorrow", "2005", "NDS"),
                TimelineItem("Castlevania: Portrait of Ruin", "2006", "NDS"),
                TimelineItem("Castlevania: Order of Ecclesia", "2008", "NDS"),
            ),
        ),
        FranchiseTimeline(
            "Donkey Kong",
            listOf(
                TimelineItem("Donkey Kong", "1981", "NES"),
                TimelineItem("Donkey Kong Jr.", "1982", "NES"),
                TimelineItem("Donkey Kong Country", "1994", "SNES"),
                TimelineItem("Donkey Kong Country 2: Diddy's Kong Quest", "1995", "SNES", listOf("donkey kong country 2")),
                TimelineItem("Donkey Kong Country 3: Dixie Kong's Double Trouble!", "1996", "SNES", listOf("donkey kong country 3")),
                TimelineItem("Donkey Kong 64", "1999", "N64"),
                TimelineItem("Donkey Kong Country Returns", "2010", "Wii"),
            ),
        ),
        FranchiseTimeline(
            "Kirby",
            listOf(
                TimelineItem("Kirby's Dream Land", "1992", "GB"),
                TimelineItem("Kirby's Adventure", "1993", "NES"),
                TimelineItem("Kirby's Dream Land 2", "1995", "GB"),
                TimelineItem("Kirby Super Star", "1996", "SNES"),
                TimelineItem("Kirby's Dream Land 3", "1997", "SNES"),
                TimelineItem("Kirby 64: The Crystal Shards", "2000", "N64", listOf("kirby 64")),
                TimelineItem("Kirby: Nightmare in Dream Land", "2002", "GBA"),
                TimelineItem("Kirby & the Amazing Mirror", "2004", "GBA"),
                TimelineItem("Kirby: Squeak Squad", "2006", "NDS"),
                TimelineItem("Kirby Super Star Ultra", "2008", "NDS"),
            ),
        ),
        FranchiseTimeline(
            "Chrono",
            listOf(
                TimelineItem("Chrono Trigger", "1995", "SNES"),
                TimelineItem("Radical Dreamers", "1996", "SNES"),
                TimelineItem("Chrono Cross", "1999", "PS1"),
            ),
        ),
        FranchiseTimeline(
            "Mother / EarthBound",
            listOf(
                TimelineItem("EarthBound Beginnings (Mother)", "1989", "NES", listOf("mother", "earthbound beginnings", "earthbound zero")),
                TimelineItem("EarthBound (Mother 2)", "1994", "SNES", listOf("earthbound", "mother 2")),
                TimelineItem("Mother 3", "2006", "GBA"),
            ),
        ),
        FranchiseTimeline(
            "Street Fighter",
            listOf(
                TimelineItem("Street Fighter", "1987", "Arcade"),
                TimelineItem("Street Fighter II", "1991", "SNES", listOf("street fighter 2", "super street fighter ii", "street fighter ii turbo")),
                TimelineItem("Street Fighter Alpha", "1995", "PS1", listOf("street fighter zero")),
                TimelineItem("Street Fighter Alpha 2", "1996", "PS1", listOf("street fighter zero 2")),
                TimelineItem("Street Fighter III: 3rd Strike", "1999", "Dreamcast", listOf("street fighter 3", "street fighter iii")),
                TimelineItem("Street Fighter Alpha 3", "1998", "PS1", listOf("street fighter zero 3")),
            ),
        ),
        FranchiseTimeline(
            "Resident Evil",
            listOf(
                TimelineItem("Resident Evil", "1996", "PS1", listOf("biohazard")),
                TimelineItem("Resident Evil 2", "1998", "PS1"),
                TimelineItem("Resident Evil 3: Nemesis", "1999", "PS1", listOf("resident evil 3")),
                TimelineItem("Resident Evil – Code: Veronica", "2000", "Dreamcast", listOf("resident evil code veronica")),
                TimelineItem("Resident Evil Zero", "2002", "GC", listOf("resident evil 0")),
                TimelineItem("Resident Evil 4", "2005", "GC"),
            ),
        ),
        FranchiseTimeline(
            "Metal Gear",
            listOf(
                TimelineItem("Metal Gear", "1987", "NES"),
                TimelineItem("Metal Gear 2: Solid Snake", "1990", "MSX"),
                TimelineItem("Metal Gear Solid", "1998", "PS1"),
                TimelineItem("Metal Gear Solid 2: Sons of Liberty", "2001", "PS2", listOf("metal gear solid 2")),
                TimelineItem("Metal Gear Solid 3: Snake Eater", "2004", "PS2", listOf("metal gear solid 3")),
                TimelineItem("Metal Gear Solid: Peace Walker", "2010", "PSP"),
            ),
        ),
        FranchiseTimeline(
            "Crash Bandicoot",
            listOf(
                TimelineItem("Crash Bandicoot", "1996", "PS1"),
                TimelineItem("Crash Bandicoot 2: Cortex Strikes Back", "1997", "PS1", listOf("crash bandicoot 2")),
                TimelineItem("Crash Bandicoot: Warped", "1998", "PS1", listOf("crash bandicoot 3")),
                TimelineItem("Crash Team Racing", "1999", "PS1"),
                TimelineItem("Crash Bandicoot: The Wrath of Cortex", "2001", "PS2"),
            ),
        ),
        FranchiseTimeline(
            "Spyro",
            listOf(
                TimelineItem("Spyro the Dragon", "1998", "PS1"),
                TimelineItem("Spyro 2: Ripto's Rage!", "1999", "PS1", listOf("spyro 2", "spyro 2 gateway to glimmer")),
                TimelineItem("Spyro: Year of the Dragon", "2000", "PS1", listOf("spyro 3")),
            ),
        ),
        FranchiseTimeline(
            "Contra",
            listOf(
                TimelineItem("Contra", "1987", "NES", listOf("probotector")),
                TimelineItem("Super C", "1988", "NES", listOf("super contra")),
                TimelineItem("Contra III: The Alien Wars", "1992", "SNES", listOf("contra 3")),
                TimelineItem("Contra: Hard Corps", "1994", "Genesis"),
                TimelineItem("Contra 4", "2007", "NDS"),
            ),
        ),
        FranchiseTimeline(
            "Star Fox",
            listOf(
                TimelineItem("Star Fox", "1993", "SNES", listOf("starwing")),
                TimelineItem("Star Fox 64", "1997", "N64", listOf("lylat wars")),
                TimelineItem("Star Fox Adventures", "2002", "GC"),
                TimelineItem("Star Fox: Assault", "2005", "GC"),
            ),
        ),
        FranchiseTimeline(
            "Dragon Quest",
            listOf(
                TimelineItem("Dragon Quest", "1986", "NES", listOf("dragon warrior")),
                TimelineItem("Dragon Quest II", "1987", "NES", listOf("dragon warrior ii", "dragon quest 2")),
                TimelineItem("Dragon Quest III", "1988", "NES", listOf("dragon warrior iii", "dragon quest 3")),
                TimelineItem("Dragon Quest IV", "1990", "NES", listOf("dragon warrior iv", "dragon quest 4")),
                TimelineItem("Dragon Quest V", "1992", "SNES", listOf("dragon quest 5")),
                TimelineItem("Dragon Quest VI", "1995", "SNES", listOf("dragon quest 6")),
                TimelineItem("Dragon Quest VII", "2000", "PS1", listOf("dragon quest 7")),
                TimelineItem("Dragon Quest VIII", "2004", "PS2", listOf("dragon quest 8")),
                TimelineItem("Dragon Quest IX", "2009", "NDS", listOf("dragon quest 9")),
            ),
        ),
        FranchiseTimeline(
            "Fire Emblem",
            listOf(
                TimelineItem("Fire Emblem: Shadow Dragon", "1990", "NES"),
                TimelineItem("Fire Emblem: The Binding Blade", "2002", "GBA", listOf("fire emblem binding blade")),
                TimelineItem("Fire Emblem: The Blazing Blade", "2003", "GBA", listOf("fire emblem")),
                TimelineItem("Fire Emblem: The Sacred Stones", "2004", "GBA", listOf("fire emblem sacred stones")),
                TimelineItem("Fire Emblem: Path of Radiance", "2005", "GC"),
                TimelineItem("Fire Emblem: Radiant Dawn", "2007", "Wii"),
                TimelineItem("Fire Emblem Awakening", "2012", "3DS"),
            ),
        ),
        FranchiseTimeline(
            "Golden Sun",
            listOf(
                TimelineItem("Golden Sun", "2001", "GBA"),
                TimelineItem("Golden Sun: The Lost Age", "2002", "GBA"),
                TimelineItem("Golden Sun: Dark Dawn", "2010", "NDS"),
            ),
        ),
        FranchiseTimeline(
            "Persona",
            listOf(
                TimelineItem("Revelations: Persona", "1996", "PS1", listOf("persona", "shin megami tensei persona")),
                TimelineItem("Persona 2: Innocent Sin", "1999", "PS1"),
                TimelineItem("Persona 2: Eternal Punishment", "2000", "PS1"),
                TimelineItem("Persona 3", "2006", "PS2", listOf("persona 3 fes", "persona 3 portable")),
                TimelineItem("Persona 4", "2008", "PS2", listOf("persona 4 golden")),
            ),
        ),
        FranchiseTimeline(
            "Silent Hill",
            listOf(
                TimelineItem("Silent Hill", "1999", "PS1"),
                TimelineItem("Silent Hill 2", "2001", "PS2"),
                TimelineItem("Silent Hill 3", "2003", "PS2"),
                TimelineItem("Silent Hill 4: The Room", "2004", "PS2", listOf("silent hill 4")),
                TimelineItem("Silent Hill: Origins", "2007", "PSP"),
            ),
        ),
        FranchiseTimeline(
            "Kingdom Hearts",
            listOf(
                TimelineItem("Kingdom Hearts", "2002", "PS2"),
                TimelineItem("Kingdom Hearts: Chain of Memories", "2004", "GBA"),
                TimelineItem("Kingdom Hearts II", "2005", "PS2", listOf("kingdom hearts 2")),
                TimelineItem("Kingdom Hearts 358/2 Days", "2009", "NDS"),
                TimelineItem("Kingdom Hearts Birth by Sleep", "2010", "PSP"),
            ),
        ),
        FranchiseTimeline(
            "Gran Turismo",
            listOf(
                TimelineItem("Gran Turismo", "1997", "PS1"),
                TimelineItem("Gran Turismo 2", "1999", "PS1"),
                TimelineItem("Gran Turismo 3: A-Spec", "2001", "PS2", listOf("gran turismo 3")),
                TimelineItem("Gran Turismo 4", "2004", "PS2"),
            ),
        ),
        FranchiseTimeline(
            "God of War",
            listOf(
                TimelineItem("God of War", "2005", "PS2"),
                TimelineItem("God of War II", "2007", "PS2", listOf("god of war 2")),
                TimelineItem("God of War: Chains of Olympus", "2008", "PSP"),
                TimelineItem("God of War: Ghost of Sparta", "2010", "PSP"),
            ),
        ),
        FranchiseTimeline(
            "F-Zero",
            listOf(
                TimelineItem("F-Zero", "1990", "SNES"),
                TimelineItem("F-Zero X", "1998", "N64"),
                TimelineItem("F-Zero: Maximum Velocity", "2001", "GBA"),
                TimelineItem("F-Zero GX", "2003", "GC"),
                TimelineItem("F-Zero: GP Legend", "2003", "GBA"),
            ),
        ),
        FranchiseTimeline(
            "Streets of Rage",
            listOf(
                TimelineItem("Streets of Rage", "1991", "Genesis", listOf("bare knuckle")),
                TimelineItem("Streets of Rage 2", "1992", "Genesis", listOf("bare knuckle ii")),
                TimelineItem("Streets of Rage 3", "1994", "Genesis", listOf("bare knuckle iii")),
            ),
        ),
        FranchiseTimeline(
            "Mortal Kombat",
            listOf(
                TimelineItem("Mortal Kombat", "1992", "Genesis"),
                TimelineItem("Mortal Kombat II", "1993", "SNES", listOf("mortal kombat 2")),
                TimelineItem("Mortal Kombat 3", "1995", "SNES"),
                TimelineItem("Ultimate Mortal Kombat 3", "1995", "SNES"),
                TimelineItem("Mortal Kombat 4", "1997", "N64"),
            ),
        ),
        FranchiseTimeline(
            "Metal Slug",
            listOf(
                TimelineItem("Metal Slug", "1996", "Neo Geo"),
                TimelineItem("Metal Slug 2", "1998", "Neo Geo"),
                TimelineItem("Metal Slug X", "1999", "Neo Geo"),
                TimelineItem("Metal Slug 3", "2000", "Neo Geo"),
                TimelineItem("Metal Slug 4", "2002", "Neo Geo"),
                TimelineItem("Metal Slug 5", "2003", "Neo Geo"),
            ),
        ),
        FranchiseTimeline(
            "HeroBox-16 Originals",
            listOf(
                TimelineItem("Starlight Vanguard", "2026", "HB16"),
                TimelineItem("Neon Breaker DX", "2026", "HB16"),
                TimelineItem("Prism Dash", "2026", "HB16"),
            ),
        ),
    )

    internal fun norm(s: String): String {
        var t = GameMetadataRepository.cleanGameSearchTitle(s)
            .lowercase()
            .replace("é", "e")
            .replace("’", "'")
            .replace("'", "")
            .replace("&", " and ")
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        // Normalize trailing Roman numerals to Arabic for reliable matching
        t = t.replace(Regex("""\bii\b"""), "2")
            .replace(Regex("""\biii\b"""), "3")
            .replace(Regex("""\biv\b"""), "4")
            .replace(Regex("""\bvi\b"""), "6")
            .replace(Regex("""\bvii\b"""), "7")
            .replace(Regex("""\bviii\b"""), "8")
            .replace(Regex("""\bix\b"""), "9")
        return t
    }

    fun resolve(
        gameTitle: String,
        systemId: String = "",
        wikiSeries: String = "",
        wikiPrecededBy: String = "",
        wikiFollowedBy: String = "",
        wikiExtract: String = "",
    ): SeriesResolution {
        val target = norm(gameTitle)
        // 1. Check curated franchise timelines first (exact or alias match)
        for (franchise in franchises) {
            val matchIdx = franchise.items.indexOfFirst { item ->
                val itemNorm = norm(item.title)
                itemNorm == target || item.aliases.any { norm(it) == target }
            }
            if (matchIdx >= 0) {
                val results = mutableListOf<RelatedGame>()
                if (franchise.name == "HeroBox-16 Originals") {
                    franchise.items.forEachIndexed { idx, item ->
                        if (idx != matchIdx) {
                            results += RelatedGame(item.title, "Series", item.year, item.sys)
                        }
                    }
                } else {
                    // Up to 3 immediate prequels (chronological)
                    val preStart = (matchIdx - 3).coerceAtLeast(0)
                    for (i in preStart until matchIdx) {
                        val it = franchise.items[i]
                        results += RelatedGame(it.title, "Prequel", it.year, it.sys)
                    }
                    // Up to 4 immediate sequels (chronological)
                    val seqEnd = (matchIdx + 4).coerceAtMost(franchise.items.lastIndex)
                    for (i in (matchIdx + 1)..seqEnd) {
                        val it = franchise.items[i]
                        results += RelatedGame(it.title, "Sequel", it.year, it.sys)
                    }
                }
                return SeriesResolution(
                    seriesName = wikiSeries.ifBlank { franchise.name },
                    entries = results,
                )
            }
        }

        // 2. Fallback: extract from Wikipedia infobox fields & lead text
        val entries = mutableListOf<RelatedGame>()
        if (wikiPrecededBy.isNotBlank() && norm(wikiPrecededBy) != target) {
            entries += RelatedGame(wikiPrecededBy, "Prequel")
        }
        if (wikiFollowedBy.isNotBlank() && norm(wikiFollowedBy) != target) {
            entries += RelatedGame(wikiFollowedBy, "Sequel")
        }

        if (entries.isEmpty() && wikiExtract.isNotBlank()) {
            val lead = wikiExtract.take(900)
            Regex("""(?:sequel to|follow-up to|preceded by)\s+(?:the\s+\d{4}\s+(?:video\s+)?game\s+)?([A-Z][A-Za-z0-9:'\-& ]{2,42}?)(?=[.,;()]|\s+(?:and|which|for|in|on)\b)""")
                .find(lead)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.length >= 3 && norm(it) != target }
                ?.let { entries += RelatedGame(it, "Prequel") }

            Regex("""(?:followed by|spawned a sequel,?\s*|succeeded by)\s+(?:a\s+sequel,?\s*)?([A-Z][A-Za-z0-9:'\-& ]{2,42}?)(?=[.,;()]|\s+(?:and|which|for|in|on)\b)""")
                .find(lead)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.length >= 3 && norm(it) != target && entries.none { e -> norm(e.title) == norm(it) } }
                ?.let { entries += RelatedGame(it, "Sequel") }
        }

        return SeriesResolution(seriesName = wikiSeries, entries = entries)
    }

    /**
     * Enriches [baseEntries] by matching each entry against the user's owned [allGames] library,
     * and also discovers numbered sequels/prequels already in the user's library (e.g. "Advance Wars 2").
     */
    fun resolveWithLibrary(
        currentGame: Game,
        baseEntries: List<RelatedGame>,
        allGames: List<Game>,
    ): List<Pair<RelatedGame, Game?>> {
        val others = allGames.filter { it.id != currentGame.id }
        val paired = mutableListOf<Pair<RelatedGame, Game?>>()
        val seenNorms = mutableSetOf(norm(currentGame.title))

        for (entry in baseEntries) {
            val eNorm = norm(entry.title)
            if (eNorm in seenNorms) continue
            seenNorms += eNorm
            val owned = others.firstOrNull { g ->
                val gNorm = norm(g.title)
                gNorm == eNorm || (eNorm.length >= 8 && (gNorm.startsWith("$eNorm ") || eNorm.startsWith("$gNorm ")))
            }
            paired += entry to owned
        }

        // Auto-detect numbered prequels/sequels already owned in the user's library!
        val (curBase, curNum) = splitSeriesNumber(norm(currentGame.title))
        if (curBase.length >= 4) {
            for (g in others) {
                val gNorm = norm(g.title)
                if (gNorm in seenNorms) continue
                val (otherBase, otherNum) = splitSeriesNumber(gNorm)
                if (otherBase == curBase && otherNum != curNum) {
                    seenNorms += gNorm
                    val rel = if (otherNum < curNum) "Prequel" else "Sequel"
                    paired += RelatedGame(
                        title = g.title,
                        relation = rel,
                        year = "",
                        systemHint = g.system?.shortName ?: g.systemId.uppercase(),
                    ) to g
                }
            }
        }
        return paired
    }

    private fun splitSeriesNumber(normalized: String): Pair<String, Int> {
        val m = Regex("""^(.+?)\s+(\d{1,2})$""").find(normalized)
        return if (m != null) m.groupValues[1].trim() to (m.groupValues[2].toIntOrNull() ?: 1)
        else normalized to 1
    }
}
