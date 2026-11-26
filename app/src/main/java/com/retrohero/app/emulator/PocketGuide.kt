package com.retrohero.app.emulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.RaGameData

/**
 * Progressive 3-tier spoiler-free hint entry:
 * Tier 1 = Nudge (direction only), Tier 2 = Clue (mechanic/item), Tier 3 = Solution.
 */
data class PocketHint(
    val topic: String,
    val nudge: String,
    val clue: String,
    val solution: String,
)

/** In-game Pocket Manual, spoiler-free hint booklet, and quick-reference cards tailored to a game. */
data class PocketManual(
    val title: String,
    val badge: String,
    val overview: String,
    val controls: List<Pair<String, String>>,
    val quickTips: List<String>,
    val hints: List<PocketHint>,
    val references: List<PocketReference> = emptyList(),
)

/**
 * Built-in Spoiler-Free Smart Hint & Strategy Guide database for classic retro games, with an
 * automatic system-aware pocket manual fallback so EVERY game in the library has an in-emulator
 * guide without ever tab-switching out of RetroHero.
 */
object PocketGuides {

    fun forGame(game: Game): PocketManual {
        val norm = game.title.lowercase()
        val sysName = game.system?.shortName ?: game.systemId.uppercase()
        val curated = matchCurated(norm)
        if (curated != null) return curated
        return systemFallback(game.title, game.systemId, sysName)
    }

    private fun matchCurated(title: String): PocketManual? = when {
        "ocarina" in title -> PocketManual(
            title = "Ocarina Field Guide",
            badge = "Curated · Songbook + Hints",
            overview = "3D action-adventure centered on Z-targeting, time travel between Child and Adult Link, and musical melodies.",
            controls = listOf(
                "Z" to "Lock-On Target / Center Camera",
                "B" to "Sword (Hold for Spin Attack)",
                "A" to "Context Action / Roll / Talk",
                "C-Buttons" to "Equipped Items & Ocarina Notes",
            ),
            quickTips = listOf(
                "Backflipping (Z + Back + A) and Side-Hops give instant invulnerability frames against tough bosses.",
                "Playing Sun's Song freezes ReDeads and Gibdos in place and skips straight between day and night.",
                "Pin any Ocarina song below to keep the notes visible right at the top of the screen.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Water Temple — Missing Small Key",
                    nudge = "One key is hidden underneath something that floats upward when the water rises.",
                    clue = "Raise the water to the middle level inside the central tower, then look below the floating platform.",
                    solution = "Inside the central tower, raise the water to level 2, drop under the floating block to enter the submerged basement tunnel, and shoot the eye switch for the hidden chest.",
                ),
                PocketHint(
                    topic = "Entering Lord Jabu-Jabu's Belly",
                    nudge = "Jabu-Jabu only opens his mouth for his favorite snack.",
                    clue = "Scoop up something swimming in the shallow water around Zora's Domain with an Empty Bottle.",
                    solution = "Catch a Fish in an Empty Bottle in Zora's Domain, stand right in front of Jabu-Jabu's mouth in Zora's Fountain, and release it.",
                ),
                PocketHint(
                    topic = "Forest Temple — Twisted Corridor",
                    nudge = "The room itself can be straightened from the doorway above.",
                    clue = "Look above the hallway entrance for a familiar archery target.",
                    solution = "Shoot the Eye Switch above the twisted corridor doorway with the Fairy Bow to untwist the room and reach the Boss Key chest.",
                ),
                PocketHint(
                    topic = "Shadow Temple — Invisible Walls & Floors",
                    nudge = "You need an item from Kakariko's past before exploring deeply here.",
                    clue = "Learn the Song of Storms as Adult Link, then play it inside the Windmill as Child Link.",
                    solution = "Drain Kakariko Well as Child Link with Song of Storms, collect the Lens of Truth inside, and equip it to see hidden paths and false walls.",
                ),
            ),
            references = listOf(
                PocketReference.SongBook(
                    title = "Ocarina of Time Songbook",
                    subtitle = "Tap Pin on any song to keep the notes on-screen",
                    songs = listOf(
                        SongEntry("Zelda's Lullaby", listOf("C◄", "C▲", "C►", "C◄", "C▲", "C►"), "Royal Crest triggers & Fairy healing"),
                        SongEntry("Epona's Song", listOf("C▲", "C◄", "C►", "C▲", "C◄", "C►"), "Summon Epona & milk cows"),
                        SongEntry("Saria's Song", listOf("C▼", "C►", "C◄", "C▼", "C►", "C◄"), "Talk to Saria & cheer Darunia"),
                        SongEntry("Sun's Song", listOf("C►", "C▼", "C▲", "C►", "C▼", "C▲"), "Switch Day/Night & freeze ReDeads"),
                        SongEntry("Song of Time", listOf("C►", "A", "C▼", "C►", "A", "C▼"), "Move blue Time Blocks"),
                        SongEntry("Song of Storms", listOf("A", "C▼", "C▲", "A", "C▼", "C▲"), "Summon rain & drain the Well"),
                        SongEntry("Minuet of Forest", listOf("A", "C▲", "C◄", "C►", "C◄", "C►"), "Warp → Sacred Forest Meadow"),
                        SongEntry("Bolero of Fire", listOf("C▼", "A", "C▼", "A", "C►", "C▼", "C►", "C▼"), "Warp → Death Mountain Crater"),
                        SongEntry("Serenade of Water", listOf("A", "C▼", "C►", "C►", "C◄"), "Warp → Lake Hylia"),
                        SongEntry("Requiem of Spirit", listOf("A", "C▼", "A", "C►", "C▼", "A"), "Warp → Desert Colossus"),
                        SongEntry("Nocturne of Shadow", listOf("C◄", "C►", "C►", "A", "C◄", "C►", "C▼"), "Warp → Kakariko Graveyard"),
                        SongEntry("Prelude of Light", listOf("C▲", "C►", "C▲", "C►", "C◄", "C▲"), "Warp → Temple of Time"),
                    ),
                ),
            ),
        )

        "majora" in title -> PocketManual(
            title = "Termina 3-Day Guide",
            badge = "Curated · Songbook + Hints",
            overview = "Three-day time-loop adventure where masks grant new forms and secret songs bend the flow of time.",
            controls = listOf(
                "Z" to "Lock-On / Tatl Advice",
                "C-Buttons" to "Masks, Ocarina & Items",
                "B" to "Sword / Form Attack",
                "A" to "Action / Roll / Goron Curl",
            ),
            quickTips = listOf(
                "Play the Inverted Song of Time immediately at the start of every cycle to slow the clock by 50%!",
                "Deposit Rupees at the Clock Town Bank before resetting the 3-day cycle — your balance carries over.",
                "Owl Statues let you warp back to any region with the Song of Soaring.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Running Out of Time in Dungeons",
                    nudge = "Scarecrows know secret variations of the Song of Time.",
                    clue = "Try playing the Song of Time's six notes completely backwards.",
                    solution = "Play C▼, A, C►, C▼, A, C► (Inverted Song of Time) to halve the speed of the 3-day clock.",
                ),
                PocketHint(
                    topic = "Reaching Ikana Canyon / Stone Tower",
                    nudge = "You need to prove your link to the royal soldiers and ghosts.",
                    clue = "Bring the Garo's Mask (from Gorman Race) and the Hookshot to climb the canyon.",
                    solution = "Win Garo's Mask at Gorman Track, talk to the sitting figure at Ikana entrance while wearing it, then Hookshot up the trees.",
                ),
            ),
            references = listOf(
                PocketReference.SongBook(
                    title = "Majora's Mask Songbook",
                    subtitle = "Essential time & warp melodies",
                    songs = listOf(
                        SongEntry("Song of Time", listOf("C►", "A", "C▼", "C►", "A", "C▼"), "Reset to Dawn of the First Day"),
                        SongEntry("Inverted Song of Time", listOf("C▼", "A", "C►", "C▼", "A", "C►"), "Slow time to 50% speed!"),
                        SongEntry("Song of Double Time", listOf("C►", "C►", "A", "A", "C▼", "C▼"), "Jump forward 12 hours"),
                        SongEntry("Song of Healing", listOf("C◄", "C►", "C▼", "C◄", "C►", "C▼"), "Heal spirits & forge masks"),
                        SongEntry("Song of Soaring", listOf("C▼", "C◄", "C▲", "C▼", "C◄", "C▲"), "Warp to any activated Owl Statue"),
                        SongEntry("Elegy of Emptiness", listOf("C►", "C◄", "C►", "C▼", "C►", "C▲", "C◄"), "Create statue clones for switches"),
                    ),
                ),
            ),
        )

        "awakening" in title -> PocketManual(
            title = "Koholint Island Guide",
            badge = "Curated · Trading Chain + Hints",
            overview = "Compact, puzzle-dense Zelda adventure featuring flexible A/B item combos and an island-wide trading sequence.",
            controls = listOf(
                "A / B" to "Use either equipped item (combine Feather + Boots!)",
                "Select" to "Koholint Overworld Map",
                "Start" to "Inventory & Instruments",
            ),
            quickTips = listOf(
                "Equip Pegasus Boots + Roc's Feather together to leap across wide 4-tile chasms.",
                "Use the Telephone Booths around the island for Ulrira's built-in directional clues.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Eagle's Tower — Collapsing the 4 Pillars",
                    nudge = "There's a heavy black orb on the second floor that can break more than enemies.",
                    clue = "Carry the Iron Ball across rooms — you can throw it over pits or low walls to pick it up from the other side.",
                    solution = "Throw the Iron Ball into all four load-bearing pillars on 2F to drop the upper floor onto the lower floor and unlock the boss.",
                ),
                PocketHint(
                    topic = "Navigating the Wind Fish's Egg",
                    nudge = "A book in Mabe Village holds the maze directions.",
                    clue = "You need the final item from the island trading sequence to read the tiny print in the Library.",
                    solution = "Finish the Trading Sequence for the Magnifying Lens, read the bottom-right book in Mabe Library, and follow its unique 8-direction path inside the Egg.",
                ),
            ),
            references = listOf(
                PocketReference.MapLegend(
                    title = "Koholint Trading Sequence",
                    subtitle = "14-step chain to unlock the Magnifying Lens",
                    steps = listOf(
                        RouteStep("1. Yoshi Doll", "Win at Mabe Trendy Game → trade to Papahl's wife for Ribbon"),
                        RouteStep("2. Ribbon → Dog Food", "Give Ribbon to small BowWow in Madam MeowMeow's house"),
                        RouteStep("3. Dog Food → Bananas", "Give Dog Food to Sale (alligator) at Toronbo Shores"),
                        RouteStep("4. Bananas → Stick", "Give Bananas to Kiki the monkey at Kanalet Castle"),
                        RouteStep("5. Stick → Honeycomb", "Give Stick to Tarin near Ukuku Prairie beehive"),
                        RouteStep("6. Honeycomb → Pineapple", "Give Honeycomb to Chef Bear in Animal Village"),
                        RouteStep("7. Pineapple → Hibiscus", "Give Pineapple to Papahl on Tal Tal Heights"),
                        RouteStep("8. Hibiscus → Letter", "Give Hibiscus to Christine the goat in Animal Village"),
                        RouteStep("9. Letter → Broom", "Give Letter to Mr. Write north of Mysterious Forest"),
                        RouteStep("10. Broom → Fishing Hook", "Give Broom to Grandma Ulrira in Animal Village"),
                        RouteStep("11. Hook → Necklace", "Dive under the bridge in Martha's Bay for the Fisherman"),
                        RouteStep("12. Necklace → Scale", "Give Necklace to the Mermaid in Martha's Bay"),
                        RouteStep("13. Scale → Lens", "Place Scale on Mermaid Statue → enter cave for Magnifying Lens"),
                    ),
                ),
            ),
        )

        "link to the past" in title || ("zelda" in title && "ocarina" !in title && "awakening" !in title && "majora" !in title) -> PocketManual(
            title = "Hyrule Field Guide",
            badge = "Curated · Portals + Hints",
            overview = "Action-adventure classic built around item-gated exploration, cracked walls, and Light/Dark World spatial mirrors.",
            controls = listOf(
                "B" to "Sword slash (Hold for Spin Attack)",
                "Y" to "Use equipped sub-item",
                "A" to "Dash / Lift / Talk / Read",
                "X / Select" to "Overworld & Dungeon Map",
            ),
            quickTips = listOf(
                "Tap suspicious walls with your sword — a hollow metallic clink means a bombable secret.",
                "Always keep a Fairy inside a Bottle for automatic revival during boss fights.",
                "Fortune Tellers mark your next major objective on the map for a few Rupees.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Stuck in Eastern Palace / Early Dungeon",
                    nudge = "Look closely at statues and floor switches before assuming a door is locked.",
                    clue = "Bow arrows can hit eye statues and crystal switches from across gaps.",
                    solution = "Equip the Bow inside the Eastern Palace to shoot the Red Eyegore and Armos Knights (3 arrows each, or spin attacks on the final red knight).",
                ),
                PocketHint(
                    topic = "Crossing between Worlds & Reaching High Ledges",
                    nudge = "Compare the exact same tile coordinates between both worlds.",
                    clue = "Standing on clear ground in the Dark World lets the Magic Mirror warp you onto unreachable Light World cliffs.",
                    solution = "Use the Magic Mirror on an open tile directly matching the Light World cliff you want (like Spectacle Rock or Desert ledges).",
                ),
                PocketHint(
                    topic = "Finding the Master Sword in Lost Woods",
                    nudge = "Not every clearing in the fog is real.",
                    clue = "Collect all three Pendants first, then follow the northern mist clearings past the Thief hideout.",
                    solution = "Enter Lost Woods from Kakariko's northwest exit, head north through the clearing with two logs, and pull the sword with all 3 Pendants.",
                ),
            ),
            references = listOf(
                PocketReference.MapLegend(
                    title = "Light → Dark World Warp Portals",
                    subtitle = "Key permanent warp tiles across Hyrule",
                    steps = listOf(
                        RouteStep("Hyrule Castle Gate", "Central portal straight to the Pyramid of Power"),
                        RouteStep("East of Kakariko", "Hammer the stakes & lift the light rock → Village of Outcasts"),
                        RouteStep("South of Haunted Grove", "Lift the light rock → Swamp of Evil / Swamp Palace"),
                        RouteStep("Lake Hylia Island", "Swim to the center island & lift the heavy rock → Ice Palace"),
                        RouteStep("Death Mountain West", "Lift the heavy dark rock on the cliff → Turtle Rock"),
                        RouteStep("Desert Ledge (Flute #6)", "Fly to Mire area & lift dark rock → Misery Mire"),
                    ),
                ),
            ),
        )

        "street fighter" in title || "alpha" in title && "street" in title || "marvel vs" in title || "capcom" in title && "vs" in title -> PocketManual(
            title = "Arcade Fighter Command Sheet",
            badge = "Curated · Move List",
            overview = "6-button arcade fighter built on spacing (footsies), anti-airs, and canceling normal hits into special moves.",
            controls = listOf(
                "LP / MP / HP" to "Light / Medium / Heavy Punch",
                "LK / MK / HK" to "Light / Medium / Heavy Kick",
                "Back (Hold)" to "High Block · Down-Back = Low Block",
                "Close + Fwd + HP" to "Throw opponent",
            ),
            quickTips = listOf(
                "Cancel crouching Medium Kick into Hadouken (2-in-1 cancel) for safe pressure.",
                "For Charge moves (Guile / Chun-Li / Bison), hold Down-Back so you charge BOTH back and down while blocking!",
                "Tap Pin on any move below to keep the motion right at the top of the screen while practicing.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Landing Dragon Punch (Shoryuken) Consistently",
                    nudge = "You don't need a perfect Z-shape on a touchscreen D-pad.",
                    clue = "Tap Forward, then immediately roll a standard quarter-circle (Down → Down-Forward + Punch).",
                    solution = "Input → then ↓↘ + Punch (or tap ↘ twice in modern Street Fighter) to trigger Shoryuken cleanly every time.",
                ),
                PocketHint(
                    topic = "Beating Zangief / Grapplers",
                    nudge = "Never jump straight into their face.",
                    clue = "Keep them at max poke range with low kicks and fireballs, and jump vertically if they walk in close.",
                    solution = "Zone with crouching Medium Kick and fireballs; if knocked down, jump straight up on wake-up to dodge Spinning Piledriver and punish on landing.",
                ),
            ),
            references = listOf(
                PocketReference.MoveList(
                    title = "Classic Street Fighter Command List",
                    subtitle = "Motions shown facing right · tap Pin to keep on-screen",
                    moves = listOf(
                        MoveEntry("Ryu / Ken", "Hadouken", "↓ ↘ → + P", "Fireball projectile"),
                        MoveEntry("Ryu / Ken", "Shoryuken", "→ ↓ ↘ + P", "Invincible anti-air uppercut"),
                        MoveEntry("Ryu / Ken", "Tatsumaki", "↓ ↙ ← + K", "Hurricane Kick (passes fireballs)"),
                        MoveEntry("Chun-Li", "Lightning Legs", "Tap K rapidly", "Multi-hit pressure"),
                        MoveEntry("Chun-Li", "Spinning Bird Kick", "Hold ↓ (2s), ↑ + K", "Horizontal helicopter kick"),
                        MoveEntry("Guile", "Sonic Boom", "Hold ← (2s), → + P", "Fast-recovery projectile"),
                        MoveEntry("Guile", "Flash Kick", "Hold ↓ (2s), ↑ + K", "Anti-air somersault"),
                        MoveEntry("Zangief", "Spinning Piledriver", "360° + P", "Unblockable command grab"),
                        MoveEntry("Cammy", "Spiral Arrow", "↓ ↘ → + K", "Low drill rush"),
                        MoveEntry("Cammy", "Cannon Spike", "→ ↓ ↘ + K", "Anti-air rising kick"),
                        MoveEntry("Akuma", "Raging Demon (Super)", "LP, LP, →, LK, HP", "Unblockable teleport super"),
                    ),
                ),
            ),
        )

        "mortal kombat" in title -> PocketManual(
            title = "Kombat Kommands & Fatalities",
            badge = "Curated · Move List",
            overview = "Arcade fighter with dedicated Block button, dial-a-combos, and signature finishing Fatalities.",
            controls = listOf(
                "HP / LP" to "High / Low Punch",
                "HK / LK" to "High / Low Kick",
                "Block" to "Guard (does not use Back!)",
                "Down + HP" to "Uppercut (massive knockback)",
            ),
            quickTips = listOf(
                "Hold Block while inputting Up-based Fatalities so your fighter doesn't jump, then release Block before the final button!",
                "Sweeping (Back + LK) trips aggressive AI fighters reliably.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Pulling Off Fatalities Without Jumping",
                    nudge = "One button locks your feet to the ground during directional inputs.",
                    clue = "Hold Block while pressing Up directions, then let go of Block right before the final attack button.",
                    solution = "Stand at the exact distance (Close / Sweep / Jump), hold Block during any ↑ inputs, release Block, and press the finishing button.",
                ),
            ),
            references = listOf(
                PocketReference.MoveList(
                    title = "Classic MK Specials & Fatalities",
                    subtitle = "Distances: Close, Sweep (2 steps), Jump (half screen)",
                    moves = listOf(
                        MoveEntry("Scorpion", "Spear", "← ← + LP", "Get over here!"),
                        MoveEntry("Scorpion", "Teleport Punch", "↓ ↙ ← + HP", "Cross-up ambush"),
                        MoveEntry("Scorpion", "Toasty! (Fatality)", "Hold Blk: ↑ ↑ (Sweep)", "Release Blk after ↑ ↑ (MK1/MK2)"),
                        MoveEntry("Sub-Zero", "Ice Freeze", "↓ ↘ → + LP", "Freezes opponent for free hit"),
                        MoveEntry("Sub-Zero", "Ice Slide", "← + LP + LK + Blk", "Low sliding trip"),
                        MoveEntry("Sub-Zero", "Spine Rip (Fatality)", "→ ↓ → + HP (Close)", "Classic MK1 finisher"),
                        MoveEntry("Raiden", "Lightning Bolt", "↓ ↘ → + LP", "Fast horizontal zap"),
                        MoveEntry("Raiden", "Torpedo Fly", "← ← →", "Full-screen ram"),
                        MoveEntry("Liu Kang", "Fireball", "→ → + HP", "High projectile"),
                        MoveEntry("Liu Kang", "Flying Kick", "→ → + HK", "Fast punish kick"),
                    ),
                ),
            ),
        )

        "castlevania" in title || "symphony of the night" in title || "aria of sorrow" in title || "dawn of sorrow" in title -> PocketManual(
            title = "Dracula's Castle Compendium",
            badge = "Curated · Spells + True Ending",
            overview = "Gothic exploration RPG packed with secret breakable walls, hidden spell inputs, and multiple endings.",
            controls = listOf(
                "Square / B" to "Main Weapon Attack",
                "Cross / A" to "Jump (Double Jump / Bat)",
                "Triangle / L" to "Backdash (cancel recovery!)",
                "Up + Attack" to "Use Sub-Weapon",
            ),
            quickTips = listOf(
                "Backdashing immediately after swinging a sword cancels recovery so you attack twice as fast.",
                "Always check the edges of your map for unfinished blue borders — they mark secret breakable rooms.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Symphony of the Night — Unlocking the True Second Half",
                    nudge = "Defeating Richter right away gives the bad ending — something invisible is controlling him.",
                    clue = "Collect the Gold and Silver Rings to open the Clock Room floor and obtain a special pair of glasses from Maria.",
                    solution = "Equip both Gold Ring (Succubus) and Silver Ring (Royal Chapel) in the Marble Gallery clock room, get the Holy Glasses from Maria, equip them vs Richter, and attack the green orb above him.",
                ),
                PocketHint(
                    topic = "Aria of Sorrow — True Ending Soul Combination",
                    nudge = "Three specific ancient books hidden in the castle name the souls you must equip.",
                    clue = "Look for Red, Blue, and Yellow souls linked to a flame demon, a bat, and a succubus.",
                    solution = "Before fighting Graham, equip Flame Demon (Red), Giant Bat (Blue), and Succubus (Yellow) souls to unlock the Chaotic Realm true ending.",
                ),
            ),
            references = listOf(
                PocketReference.MoveList(
                    title = "Alucard Secret Spell Inputs (SotN)",
                    subtitle = "No need to buy the scrolls — these work from the start!",
                    moves = listOf(
                        MoveEntry("Alucard", "Dark Metamorphosis", "← ↖ ↑ ↗ → + Atk", "Heal HP from enemy blood"),
                        MoveEntry("Alucard", "Hellfire", "↑ ↓ ↘ → + Atk", "Teleport + 3 fireballs"),
                        MoveEntry("Alucard", "Tetra Spirit", "Hold ↑ (2s), ↗ → ↘ ↓ + Atk", "4 homing spirits"),
                        MoveEntry("Alucard", "Soul Steal", "← → ↘ ↓ ↙ ← → + Atk", "Screen-wide HP drain"),
                        MoveEntry("Alucard", "Wing Smash (Bat)", "Hold X: ↑ ↖ ← ↙ ↓ ↘ →, rel X", "High-speed bat charge"),
                    ),
                ),
            ),
        )

        "metal gear" in title -> PocketManual(
            title = "Shadow Moses Codec Dossier",
            badge = "Curated · Spoiler-Free",
            overview = "Tactical espionage stealth thriller where observation, Codec calls, and creative boss solutions beat raw firepower.",
            controls = listOf(
                "Cross / B" to "Crouch / Crawl into vents",
                "Circle / A" to "Punch-Punch-Kick / Choke",
                "Square / Y" to "Fire Equipped Weapon",
                "L2 / R2" to "Quick-swap Items & Weapons",
            ),
            quickTips = listOf(
                "Tap R2 twice quickly to instantly reload your SOCOM or FAMAS without waiting for the reload animation.",
                "Equipping the Cardboard Box inside a parked truck lets guards drive you directly to that zone!",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Finding Meryl's Codec Frequency",
                    nudge = "Campbell tells you to check the back of the CD case — and RetroHero actually has it!",
                    clue = "Flip the 3D Retail Box on the Game Page or check the exact 140.xx frequency.",
                    solution = "Tune your Codec to 140.15 to reach Meryl Silverburgh.",
                ),
                PocketHint(
                    topic = "Psycho Mantis Reading Your Mind",
                    nudge = "He predicts every button press coming from Controller Port 1.",
                    clue = "If you can't swap controller ports mid-fight, Campbell hints at another way after you die once and call him.",
                    solution = "Switch controller port to Port 2 (or call Campbell repeatedly after failing, then shoot the two covered statues in the upper corners of the room).",
                ),
                PocketHint(
                    topic = "PAL Key — Changing Its Color/Temperature",
                    nudge = "The shape-memory alloy reacts to extreme heat and cold.",
                    clue = "Walk into the Blast Furnace for Red and the Warehouse Freezer for Blue.",
                    solution = "Insert Yellow first; visit the Blast Furnace until it turns Red (insert #2); visit the Raven Freezer until it turns Blue (insert #3).",
                ),
            ),
            references = listOf(
                PocketReference.MapLegend(
                    title = "Essential Shadow Moses Codec Frequencies",
                    subtitle = "Pin any frequency to keep it handy",
                    steps = listOf(
                        RouteStep("140.85", "Colonel Roy Campbell (Mission control & boss tips)"),
                        RouteStep("140.96", "Mei Ling (Save game & proverbs)"),
                        RouteStep("140.15", "Meryl Silverburgh (Cell doors & sneaking help)"),
                        RouteStep("141.12", "Otacon (Hal Emmerich — tech & stealth clues)"),
                        RouteStep("141.52", "Nastasha Romanenko (Weapons & item intel)"),
                        RouteStep("141.80", "Master Miller (Survival & cold-weather tips)"),
                    ),
                ),
            ),
        )

        "mario 64" in title || "super mario 64" in title -> PocketManual(
            title = "Peach's Castle Star Guide",
            badge = "Curated · Move Tech + Hints",
            overview = "3D sandbox platformer where mastering acrobatic jump chains lets you reach Stars from creative angles.",
            controls = listOf(
                "A" to "Jump / Double / Triple Jump",
                "B" to "Punch / Dive / Grab",
                "Z" to "Crouch / Ground Pound (in air)",
                "C-Buttons" to "Camera angle & Zoom",
            ),
            quickTips = listOf(
                "Press Z + A while running for a fast Long Jump — it's faster than running and clears huge gaps.",
                "Standing still + Z + A performs a Backflip for maximum vertical height.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Unlocking the Wing Cap Switch",
                    nudge = "Look up when a bright beam of light shines onto the castle lobby rug.",
                    clue = "Stand on the sun carpet in the main foyer after collecting 10 Stars and look into the light with C-Up.",
                    solution = "With 10+ Stars, stand on the sun emblem in the center of the lobby, press C-Up, and look directly at the ceiling light to enter Tower of the Wing Cap.",
                ),
                PocketHint(
                    topic = "Unlocking the Vanish Cap (Blue Switch)",
                    nudge = "The castle moat outside can be drained.",
                    clue = "Ground-pound two pillars in the basement room with shallow water.",
                    solution = "Enter the basement water room, swim through the tunnel, ground-pound both pillars to drain the outside moat, then drop into the hole outside for Vanish Cap Under the Moat.",
                ),
            ),
            references = listOf(
                PocketReference.MoveList(
                    title = "Super Mario 64 Acrobatic Tech",
                    subtitle = "Master these jumps to reach high Stars",
                    moves = listOf(
                        MoveEntry("Movement", "Long Jump", "Run → Z + A", "Fastest horizontal gap clearer"),
                        MoveEntry("Movement", "Backflip", "Stand → Z + A", "High vertical jump backward"),
                        MoveEntry("Movement", "Side Somersault", "Run → Reverse Stick + A", "Highest single standing jump"),
                        MoveEntry("Movement", "Triple Jump", "Run → A, A, A on landing", "Huge height & momentum"),
                        MoveEntry("Movement", "Wall Kick", "Jump into wall → A on contact", "Climb narrow shafts"),
                        MoveEntry("Movement", "Dive Rollout", "Run → B, then A", "Fast forward lunge + recovery"),
                    ),
                ),
            ),
        )

        "super metroid" in title || "metroid" in title -> PocketManual(
            title = "Zebes Mission Briefing",
            badge = "Curated · Route + Hints",
            overview = "Non-linear sci-fi labyrinth where every dead end hides either an upgrade or a hidden breakable block.",
            controls = listOf(
                "B (Hold)" to "Sprint / Charge Speed Booster",
                "L / R" to "Aim diagonally up or down",
                "Select" to "Cycle Missiles, Super Missiles & Power Bombs",
                "Down × 2" to "Morph Ball (Lay bombs with Fire)",
            ),
            quickTips = listOf(
                "Use Smart Touch L/R Latch in RetroHero to hold diagonal aim effortlessly during boss fights.",
                "Save at Recharge/Map rooms often — pink doors take 5 Missiles or 1 Super Missile.",
                "The X-Ray Scope reveals hidden crumble blocks, false floors, and grapple points.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "The Infamous 'Noob Bridge' in Brinstar",
                    nudge = "Walking or jumping won't clear the crumbling floor fast enough.",
                    clue = "There is a dedicated Run button you may not have used yet.",
                    solution = "Hold the Run button (B by default) while sprinting right across the collapsing bridge.",
                ),
                PocketHint(
                    topic = "Accessing Maridia from Brinstar",
                    nudge = "Inspect the underwater glass tunnel connecting Brinstar and Norfair.",
                    clue = "Regular bombs only scratch the surface — you need a much stronger explosive.",
                    solution = "Drop a Power Bomb inside the glass tube in Brinstar to shatter it and open the entrance to Maridia.",
                ),
                PocketHint(
                    topic = "Escaping the Wall-Jump Pit with Etecoons",
                    nudge = "Watch the little green creatures carefully — they're teaching you a secret move.",
                    clue = "Spin-jump into the wall, tap the opposite direction, then immediately press Jump.",
                    solution = "Spin-jump against the wall; as Samus touches it, press away from the wall and hit Jump a split-second later to kick upward.",
                ),
            ),
            references = listOf(
                PocketReference.MapLegend(
                    title = "Zebes Golden Four Boss Route",
                    subtitle = "High-level spoiler-free progression order",
                    steps = listOf(
                        RouteStep("1. Crateria → Brinstar", "Morph Ball → Bombs → Charge Beam → Spazer"),
                        RouteStep("2. Kraid's Lair", "Defeat Kraid → Varia Suit (heat protection)"),
                        RouteStep("3. Upper Norfair", "Hi-Jump Boots → Speed Booster → Ice Beam → Grapple"),
                        RouteStep("4. Wrecked Ship", "Defeat Phantoon → Gravity Suit (underwater speed)"),
                        RouteStep("5. Maridia", "Shatter glass tube → Space Jump → Defeat Draygon"),
                        RouteStep("6. Lower Norfair", "Defeat Ridley → Screw Attack → Tourian"),
                    ),
                ),
            ),
        )

        "chrono trigger" in title -> PocketManual(
            title = "Epoch Time Traveler's Guide",
            badge = "Curated · Spoiler-Free",
            overview = "Active Time Battle RPG where enemy positioning matters for Dual & Triple Techs and actions across eras ripple forward.",
            controls = listOf(
                "A" to "Confirm / Talk / Interact",
                "B" to "Cancel / Hold to Sprint",
                "X" to "Party Menu & Tech Setup",
                "L + R" to "Flee from regular battles",
            ),
            quickTips = listOf(
                "Sealed black chests in 600 AD upgrade if you examine them in 600 AD, refuse to take them, then loot them in 1000 AD first!",
                "Keep Tech types diverse — fire, water, lightning, and shadow exploit specific boss shields.",
                "Talk to Spekkio at the End of Time whenever a new party member joins to unlock Magic.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Millennial Fair Trial Outcomes",
                    nudge = "Small polite habits at the Fair are remembered later.",
                    clue = "Wait for Marle before picking up the pendant, don't eat the old man's lunch, and return the lost cat.",
                    solution = "Check on Marle first (not the pendant), leave the lunch alone, return the girl's cat, and refuse Melchior's offer to buy the pendant for a unanimous Not Guilty verdict.",
                ),
                PocketHint(
                    topic = "2300 AD Password Console in Arris Dome",
                    nudge = "A tiny creature in the dome knows the security input.",
                    clue = "Catch the rat statue / scurrying rat inside the rafters to hear the sequence.",
                    solution = "Hold L and R simultaneously, then press A at the glowing terminal to open the secret bridge.",
                ),
            ),
        )

        "mother 3" in title || "earthbound" in title -> PocketManual(
            title = "PSI Traveler's Handbook",
            badge = "Curated · Spoiler-Free",
            overview = "Quirky modern RPG featuring rolling HP counters and rhythmic sound-battle combos.",
            controls = listOf(
                "A" to "Talk / Check / Rhythm Tap in battle",
                "B" to "Cancel / Dash (Mother 3)",
                "L" to "Quick Check / Talk (One-Handed RPG friendly!)",
                "Start" to "Party & PSI Menu",
            ),
            quickTips = listOf(
                "Enable RetroHero's Portrait One-Handed RPG Mode under Smart Touch to play effortlessly with one thumb!",
                "Because HP rolls down slowly, heal a mortally wounded hero before the odometer hits 0.",
                "In Mother 3, put enemies to sleep to hear their heartbeat rhythm clearly for 16-hit combos.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Stuck at a Waterfall or Password Door (EarthBound)",
                    nudge = "Sometimes doing nothing at all is the smartest answer.",
                    clue = "Mr. Saturn tells you the exact waiting time required.",
                    solution = "Stand directly in front of the Grapefruit Falls entrance and wait 3 full minutes without pressing any buttons.",
                ),
                PocketHint(
                    topic = "Tough Bosses Draining Your Party",
                    nudge = "Stat buffs and PSI Shields outperform pure attack spam.",
                    clue = "Defense Down + Offense Up stack up to 3 times on most bosses.",
                    solution = "Open with PSI Shield / Counter, debuff enemy Defense twice, and use bottle rockets or Bash combos while keeping HP rolling safely above zero.",
                ),
            ),
        )

        "pokemon" in title || "pokémon" in title -> PocketManual(
            title = "Trainer's Pocket Pokedex",
            badge = "Curated · Type Chart + Hints",
            overview = "Monster-collecting RPG where type matchups, status moves, and HM navigation unlock the region.",
            controls = listOf(
                "A" to "Interact / Confirm",
                "B" to "Cancel / Running Shoes (GBA+)",
                "Select" to "Use Registered Key Item (Bike / Rod)",
                "Start" to "Bag, Party & Save Menu",
            ),
            quickTips = listOf(
                "Open the Reference tab to tap any opponent's Type and see instant 2× weaknesses & immunities!",
                "Remember: in Gen 1–3, Physical vs Special attack depends on the move's TYPE, not the individual move.",
                "Paralyzing or putting wild Pokémon to sleep dramatically increases Poké Ball catch rates.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Thirsty Guard Blocking Saffron City (Kanto)",
                    nudge = "Check the top floor of the biggest store in Celadon.",
                    clue = "Vending machine drinks are way cheaper than you think.",
                    solution = "Buy Fresh Water (or Tea from the old lady in Celadon Mansion in FireRed/LeafGreen) and hand it to any gate guard.",
                ),
                PocketHint(
                    topic = "Waking the Sleeping Snorlax",
                    nudge = "You need an instrument from Lavender Town.",
                    clue = "Clear Pokémon Tower first to help Mr. Fuji.",
                    solution = "Rescue Mr. Fuji atop Pokémon Tower to receive the Poké Flute, then play it in front of Snorlax.",
                ),
                PocketHint(
                    topic = "Passing Sudowoodo Blocking Route 36 (Johto)",
                    nudge = "That wiggly tree really hates water.",
                    clue = "Visit the Flower Shop in Goldenrod City after earning the Plain Badge.",
                    solution = "Defeat Whitney for the Plain Badge, pick up the SquirtBottle from the Goldenrod Flower Shop, and use it on the tree.",
                ),
                PocketHint(
                    topic = "Finding HM04 Strength in Safari Zone (Kanto)",
                    nudge = "The Warden lost something valuable inside the Safari Zone.",
                    clue = "Explore Area 3 (West) to find the Secret House and a shiny item on the ground nearby.",
                    solution = "Pick up the Gold Teeth in Safari Zone Area 3 and return them to the Safari Warden in Fuchsia City for HM04 Strength.",
                ),
            ),
            references = listOf(
                PocketReference.PokemonTypeChart("Interactive Pokémon Type Chart"),
            ),
        )

        "final fantasy" in title -> PocketManual(
            title = "Moogle Field Manual",
            badge = "Curated · Blitz List + Hints",
            overview = "Classic JRPG blending Active Time Battle tactics, elemental weaknesses, and party customization.",
            controls = listOf(
                "A / Cross" to "Confirm / Inspect",
                "B / Circle" to "Cancel / Back Row toggle",
                "Y / Triangle" to "Party Menu",
                "L + R" to "Escape battle",
            ),
            quickTips = listOf(
                "Put mages and ranged fighters in the Back Row to halve incoming physical damage at no magic penalty.",
                "Undead bosses take massive damage from Phoenix Downs, Revive, and Cure spells!",
                "Use RetroHero's Live Screen Translate if you're playing an untranslated Japanese Super Famicom edition.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Sabin's Blitz Inputs (FF VI)",
                    nudge = "You don't need diagonal inputs for most Blitzes.",
                    clue = "Wait for the cursor to point at Sabin before entering the D-pad sequence, then press A.",
                    solution = "For diagonal steps (like Aura Bolt / Fire Dance), pressing either adjacent cardinal direction works on SNES/GBA!",
                ),
                PocketHint(
                    topic = "Floating Continent Countdown (FF VI)",
                    nudge = "Don't jump onto the airship the instant you reach the edge.",
                    clue = "Someone is still running behind you.",
                    solution = "Choose 'Wait' at the edge of the Floating Continent until 5 seconds remain so Shadow rejoins your party.",
                ),
            ),
            references = listOf(
                PocketReference.MoveList(
                    title = "Sabin's Blitz Inputs (FF VI)",
                    subtitle = "Cardinals substitute for diagonals on SNES/GBA!",
                    moves = listOf(
                        MoveEntry("Blitz", "Pummel", "← → ← + A", "Single-target heavy physical"),
                        MoveEntry("Blitz", "Aura Bolt", "↓ ↙ ← + A (or ↓ ↓ ←)", "Holy magic beam"),
                        MoveEntry("Blitz", "Suplex", "X, Y, ↓, ↑ + A", "Yes, works on the Phantom Train!"),
                        MoveEntry("Blitz", "Fire Dance", "← ↙ ↓ ↘ → + A (or ← ← ↓ → →)", "All-enemy Fire AoE"),
                        MoveEntry("Blitz", "Mantra", "R, L, R, L, X, Y + A", "Heals party based on Sabin's HP"),
                        MoveEntry("Blitz", "Bum Rush", "← ↖ ↑ ↗ → ↘ ↓ ↙ ← + A", "Ultimate single-target strike"),
                    ),
                ),
            ),
        )

        "mario" in title -> PocketManual(
            title = "Mushroom Kingdom Guide",
            badge = "Curated · Spoiler-Free",
            overview = "Precision platformer built around momentum jumps, hidden exits, and secret Star/Key paths.",
            controls = listOf(
                "B / A" to "Jump / Spin Jump",
                "Y (Hold)" to "Run faster & Carry shells",
                "Up + Jump" to "High jump / Enter doors",
                "Select" to "Drop reserve power-up (SMW)",
            ),
            quickTips = listOf(
                "Use RetroHero's Smart Touch Thumb-Roll Chord Zones to roll smoothly from Run (Y) into Jump (B)!",
                "In Super Mario World, RED dots on the overworld map always have TWO exits (a normal goal and a secret Keyhole).",
                "Spin Jumps (A) let you bounce safely off spiky enemies and Piranha Plants.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Finding Red-Dot Secret Exits (SMW)",
                    nudge = "Bring a Cape Feather or Yoshi into suspicious stages.",
                    clue = "Check ceilings above the normal goal post or hidden alcoves near P-Switches.",
                    solution = "In Donut Plains 1, fly with the Cape to the upper-right ledge above the goal to find the Key and Keyhole.",
                ),
            ),
        )

        "sonic" in title -> PocketManual(
            title = "Green Hill Speed Manual",
            badge = "Curated · Spoiler-Free",
            overview = "Momentum-driven platformer where upper routes are faster and Spin Dash turns slopes into launch ramps.",
            controls = listOf(
                "A / B / C" to "Jump / Spin Attack",
                "Down (while moving)" to "Roll into a fast ball",
                "Down + Jump (stopped)" to "Charge Spin Dash (Sonic 2+)",
            ),
            quickTips = listOf(
                "Switch RetroHero's Hardware Profile to 'Authentic' for CRT Composite waterfall transparency blending!",
                "Always keep at least 1 Ring — it protects you from every non-crush hit.",
                "Rolling downhill builds far more speed than running.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Carnival Night Zone Barrel (Sonic 3)",
                    nudge = "Jumping on the red-and-white barrel is a classic trap!",
                    clue = "Use the D-pad rhythmically instead of the Jump button.",
                    solution = "Stand on the barrel and press UP as it rises and DOWN as it falls until it swings low enough to walk under.",
                ),
            ),
        )

        else -> null
    }

    private fun systemFallback(title: String, systemId: String, sysName: String): PocketManual {
        val controls = when (systemId) {
            "nes" -> listOf("D-Pad" to "Move / Navigate menus", "B" to "Run / Cancel / Secondary", "A" to "Jump / Confirm / Action", "Start / Select" to "Pause / Mode Select")
            "snes" -> listOf("Y / B" to "Primary Action & Jump", "X / A" to "Secondary / Sub-weapon / Confirm", "L / R" to "Camera, Aim, or Page Turn", "Start / Select" to "Pause / Map or Mode")
            "gba" -> listOf("A / B" to "Confirm / Cancel & Action", "L / R" to "Shoulder abilities & shortcuts", "Start / Select" to "Pause / Inventory or Map")
            "gb", "gbc" -> listOf("A" to "Action / Confirm", "B" to "Cancel / Secondary", "Start / Select" to "Menu / Select")
            "genesis" -> listOf("A / B / C" to "Primary 3-button actions", "X / Y / Z" to "6-button fighting & extras", "Start" to "Pause / Menu")
            "psx" -> listOf("Cross / Circle" to "Confirm / Cancel", "Square / Triangle" to "Action / Menu", "L1–R2" to "Camera, Strafe & Lock-on")
            "n64" -> listOf("Analog Stick" to "360° Movement", "A / B" to "Primary / Secondary", "Z Trigger" to "Target / Crouch / Shoot", "C-Buttons" to "Camera & Equipped Items")
            "nds" -> listOf("Touch Screen" to "Direct Stylus Input", "A / B / X / Y" to "Face Actions", "L / R" to "Shoulder Triggers")
            else -> listOf("D-Pad / Stick" to "Movement", "Face Buttons" to "Primary & Secondary Actions", "Start" to "Pause / In-Game Menu")
        }
        return PocketManual(
            title = "$sysName Strategy Guide",
            badge = "$sysName · Spoiler-Free",
            overview = "In-emulator guide, quick reference, and personal scratchpad for $title. Swipe right anytime to jump straight back into your game.",
            controls = controls,
            quickTips = listOf(
                "Pin a personal note (like a dungeon password or code) in the Notes tab to keep it visible on-screen while playing.",
                "Use Live Screen Translate in the Pause Menu to translate untranslated Japanese dialogue boxes directly over the game.",
                "Switch between Authentic and Enhanced under Screen to toggle original LCD/CRT color matrices and overclock fixes.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Retro Puzzle & Exploration Checklist",
                    nudge = "When stuck in a classic RPG or adventure game, re-talk to every town NPC — dialogue often updates after key flags.",
                    clue = "Check your inventory for unequipped key items, inspect suspicious walls/floors, and check the system controls in the Notes tab.",
                    solution = "Try testing each sub-item on locked doors/statues, check for a hidden second button chord (like L+R or Select), and pin any discovered codes using the Pin button.",
                ),
            ),
        )
    }
}

/**
 * Ultra-subtle translucent right-edge pull tab that lets players swipe left (or tap) from the
 * screen edge to slide the Spoiler-Free Strategy Guide drawer right beside their paused game.
 */
@Composable
fun GuideEdgeHandle(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccum += dragAmount
                        if (dragAccum < -28f) {
                            dragAccum = 0f
                            onOpen()
                        }
                    },
                )
            }
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .background(Color.Black.copy(alpha = 0.56f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            )
            .clickable(onClick = onOpen)
            .padding(start = 7.dp, end = 5.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MenuBook,
            contentDescription = "Swipe or tap to open Strategy Guide",
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Compact translucent pinned-note reminder pill shown at the top of the game screen when the
 * player pins a password, Codec frequency, Ocarina song, or move input from the Strategy Guide.
 */
@Composable
fun PinnedNoteBanner(
    note: String,
    onOpenGuide: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.74f))
            .border(1.dp, cs.primary.copy(alpha = 0.45f), RoundedCornerShape(50))
            .clickable(onClick = onOpenGuide)
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Rounded.PushPin,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = note,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(
            Modifier
                .clip(CircleShape)
                .clickable(onClick = onUnpin)
                .padding(3.dp),
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Unpin note",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Swipe-in Spoiler-Free Smart Hint & Strategy Guide side drawer.
 * Features progressive 3-tier hints (Nudge → Clue → Solution), spoiler-safe topic search,
 * interactive Reference Sheets (Pokémon Type Chart, Move Lists, Ocarina Songbooks, Route Legends),
 * Pinned Notes, and RetroAchievements tracking — right beside the paused game.
 */
@Composable
fun PocketGuideDrawer(
    visible: Boolean,
    game: Game,
    manual: PocketManual,
    initialNotes: String,
    pinnedNote: String?,
    raData: RaGameData?,
    raHardcore: Boolean,
    onSaveNotes: (String) -> Unit,
    onPinNote: (String?) -> Unit,
    onOpenWebBrowser: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val hasRefs = manual.references.isNotEmpty()
    var selectedTab by remember(game.id) { mutableIntStateOf(0) } // 0 = Hints, 1 = Reference, 2 = Notes, 3 = Trophies
    var notes by remember(game.id) { mutableStateOf(initialNotes) }
    var hintQuery by remember(game.id) { mutableStateOf("") }
    val revealedTier = remember(game.id) { mutableStateMapOf<Int, Int>() }
    var trophyFilter by remember { mutableIntStateOf(0) }
    var swipeCloseAccum by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(interactionSource = null, indication = null) {
                    onSaveNotes(notes)
                    onDismiss()
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(tween(240)) { it },
                exit = slideOutHorizontally(tween(200)) { it },
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                    color = cs.surfaceContainer,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 310.dp, max = 430.dp)
                        .fillMaxWidth(0.90f)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipeCloseAccum = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (dragAmount > 0f) swipeCloseAccum += dragAmount
                                    if (swipeCloseAccum > 96f) {
                                        swipeCloseAccum = 0f
                                        onSaveNotes(notes)
                                        onDismiss()
                                    }
                                },
                            )
                        }
                        .clickable(interactionSource = null, indication = null) {},
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Header
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(cs.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.MenuBook, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = manual.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${game.title} · ${manual.badge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(cs.tertiaryContainer)
                                    .clickable {
                                        onSaveNotes(notes)
                                        onOpenWebBrowser()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    "Web",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = cs.onTertiaryContainer,
                                )
                            }
                            IconButton(onClick = {
                                onSaveNotes(notes)
                                onDismiss()
                            }) {
                                Icon(Icons.Rounded.Close, "Close guide", tint = cs.onSurface)
                            }
                        }

                        // Expressive Pill Tab Selector
                        val tabs = buildList {
                            add(Triple(0, "Hints", Icons.Rounded.AutoAwesome))
                            if (hasRefs) add(Triple(1, "Lists", Icons.Rounded.Map))
                            add(Triple(2, "Notes", Icons.Rounded.Gamepad))
                            add(Triple(3, "Trophies", Icons.Rounded.EmojiEvents))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tabs.forEach { (idx, label, icon) ->
                                val active = selectedTab == idx
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(if (active) cs.primaryContainer else cs.surfaceContainerHigh)
                                        .clickable { selectedTab = idx }
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        icon,
                                        null,
                                        tint = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        // Active Pinned Note indicator inside drawer
                        if (!pinnedNote.isNullOrBlank()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cs.secondaryContainer.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.PushPin, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Pinned: $pinnedNote",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = cs.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "Unpin",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = cs.primary,
                                    modifier = Modifier.clickable { onPinNote(null) }.padding(horizontal = 4.dp),
                                )
                            }
                        }

                        // Tab Body
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            when (selectedTab) {
                                0 -> HintsTabBody(
                                    hints = manual.hints,
                                    query = hintQuery,
                                    onQueryChange = { hintQuery = it },
                                    revealedTier = revealedTier,
                                    onPinNote = onPinNote,
                                )

                                1 -> ReferencesTabBody(
                                    references = manual.references,
                                    onPinNote = onPinNote,
                                )

                                2 -> NotesAndManualTabBody(
                                    manual = manual,
                                    notes = notes,
                                    onNotesChange = {
                                        notes = it
                                        onSaveNotes(it)
                                    },
                                    onPinNote = onPinNote,
                                )

                                3 -> TrophiesTabBody(
                                    raData = raData,
                                    raHardcore = raHardcore,
                                    trophyFilter = trophyFilter,
                                    onFilterChange = { trophyFilter = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HintsTabBody(
    hints: List<PocketHint>,
    query: String,
    onQueryChange: (String) -> Unit,
    revealedTier: MutableMap<Int, Int>,
    onPinNote: (String?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Spoiler-free: Nudge → Clue → Solution",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (revealedTier.values.any { it > 0 }) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHighest)
                    .clickable { revealedTier.clear() }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Rounded.VisibilityOff, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(13.dp))
                Text("Hide all", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }

    if (hints.size > 1) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Filter topics (spoiler-safe)…", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(17.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val filtered = remember(hints, query) { PocketGuideSearch.filterHints(hints, query) }
    if (filtered.isEmpty()) {
        Text(
            "No hint topics match \"$query\". Try another dungeon or boss name, or check the Web guide button above.",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }

    filtered.forEach { hint ->
        val idx = hints.indexOf(hint)
        val tier = revealedTier[idx] ?: 0
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        hint.topic,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    // 3-dot progress indicator showing spoiler tier
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (dot in 1..3) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (tier >= dot) cs.primary else cs.outlineVariant.copy(alpha = 0.5f)),
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = tier >= 1, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    HintTierBox("1 · NUDGE", hint.nudge, cs.secondaryContainer, cs.onSecondaryContainer)
                }
                AnimatedVisibility(visible = tier >= 2, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    HintTierBox("2 · CLUE", hint.clue, cs.tertiaryContainer, cs.onTertiaryContainer)
                }
                AnimatedVisibility(visible = tier >= 3, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    HintTierBox("3 · SOLUTION", hint.solution, cs.primaryContainer, cs.onPrimaryContainer)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (tier < 3) {
                        val nextLabel = when (tier) {
                            0 -> "Reveal gentle nudge"
                            1 -> "Reveal stronger clue"
                            else -> "Reveal exact solution"
                        }
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(cs.primary)
                                .clickable { revealedTier[idx] = tier + 1 }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Rounded.Visibility, null, tint = cs.onPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                nextLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = cs.onPrimary,
                            )
                        }
                    } else {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(cs.secondaryContainer)
                                .clickable { onPinNote("${hint.topic}: ${hint.solution}") }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.PushPin, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(13.dp))
                            Text(
                                "Pin to screen",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = cs.onSecondaryContainer,
                            )
                        }
                    }
                    if (tier > 0) {
                        Text(
                            "Hide",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = cs.primary,
                            modifier = Modifier.clickable { revealedTier[idx] = 0 }.padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferencesTabBody(
    references: List<PocketReference>,
    onPinNote: (String?) -> Unit,
) {
    references.forEach { ref ->
        when (ref) {
            is PocketReference.PokemonTypeChart -> PokemonTypeChartCard(ref, onPinNote)
            is PocketReference.MoveList -> MoveListCard(ref, onPinNote)
            is PocketReference.SongBook -> SongBookCard(ref, onPinNote)
            is PocketReference.MapLegend -> MapLegendCard(ref, onPinNote)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PokemonTypeChartCard(
    ref: PocketReference.PokemonTypeChart,
    onPinNote: (String?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var selectedType by remember { mutableStateOf(PokemonType.DRAGON) }
    val matchup = remember(selectedType) { PokemonTypeChartLookup.matchupsForDefender(selectedType) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ref.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = cs.onSurface)
                    Text("Tap opponent's type to see best counters", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(cs.secondaryContainer)
                        .clickable {
                            val weak = matchup.weakTo.joinToString("/") { it.label }
                            onPinNote("vs ${selectedType.label}: 2× $weak")
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text("Pin", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = cs.onSecondaryContainer)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PokemonType.entries.forEach { type ->
                    val active = type == selectedType
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) type.color else type.color.copy(alpha = 0.24f))
                            .border(
                                width = if (active) 1.5.dp else 0.dp,
                                color = if (active) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { selectedType = type }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    ) {
                        Text(
                            type.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color.Black.copy(alpha = 0.88f) else cs.onSurface,
                        )
                    }
                }
            }

            TypeEffectivenessRow("2× Super Effective (Use)", matchup.weakTo, Color(0xFF4CAF50))
            TypeEffectivenessRow("½× Not Very Effective", matchup.resists, Color(0xFFFF9800))
            if (matchup.immuneTo.isNotEmpty()) {
                TypeEffectivenessRow("0× Immune (No Effect)", matchup.immuneTo, Color(0xFFEF5350))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeEffectivenessRow(label: String, types: List<PokemonType>, accent: Color) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = accent,
        )
        if (types.isEmpty()) {
            Text("None", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                types.forEach { t ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(t.color)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            t.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                            color = Color.Black.copy(alpha = 0.88f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveListCard(
    ref: PocketReference.MoveList,
    onPinNote: (String?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val groups = remember(ref.moves) { ref.moves.map { it.group }.distinct() }
    var activeGroup by remember(ref.title) { mutableStateOf<String?>(null) }
    val shown = remember(ref.moves, activeGroup) {
        if (activeGroup == null) ref.moves else ref.moves.filter { it.group == activeGroup }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ref.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = cs.onSurface)
            Text(ref.subtitle, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)

            if (groups.size > 1) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val allActive = activeGroup == null
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (allActive) cs.primaryContainer else cs.surfaceContainerHighest)
                            .clickable { activeGroup = null }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("All", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (allActive) cs.onPrimaryContainer else cs.onSurfaceVariant)
                    }
                    groups.forEach { g ->
                        val sel = activeGroup == g
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(if (sel) cs.primaryContainer else cs.surfaceContainerHighest)
                                .clickable { activeGroup = g }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(g, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant)
                        }
                    }
                }
            }

            shown.forEach { m ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceContainer)
                        .clickable { onPinNote("${m.name}: ${m.input}") }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (groups.size > 1 && activeGroup == null) "${m.group} · ${m.name}" else m.name,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Rounded.PushPin, "Pin move", tint = cs.primary.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(cs.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            m.input,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = cs.onSecondaryContainer,
                        )
                    }
                    if (m.note.isNotBlank()) {
                        Text(m.note, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongBookCard(
    ref: PocketReference.SongBook,
    onPinNote: (String?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ref.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = cs.onSurface)
            Text(ref.subtitle, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)

            ref.songs.forEach { song ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceContainer)
                        .clickable { onPinNote("${song.title}: ${song.notes.joinToString(" ")}") }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(song.title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = cs.onSurface)
                        Icon(Icons.Rounded.PushPin, "Pin song", tint = cs.primary.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        song.notes.forEach { n ->
                            val isA = n == "A"
                            val bg = if (isA) Color(0xFF2979FF) else Color(0xFFFFD600)
                            val fg = if (isA) Color.White else Color.Black
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(bg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    n.removePrefix("C"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                                    color = fg,
                                )
                            }
                        }
                    }
                    Text(song.effect, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MapLegendCard(
    ref: PocketReference.MapLegend,
    onPinNote: (String?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ref.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = cs.onSurface)
            Text(ref.subtitle, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            ref.steps.forEach { step ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceContainer)
                        .clickable { onPinNote("${step.label} — ${step.detail}") }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(step.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = cs.primary)
                        Text(step.detail, style = MaterialTheme.typography.bodySmall, color = cs.onSurface)
                    }
                    Icon(Icons.Rounded.PushPin, "Pin step", tint = cs.primary.copy(alpha = 0.65f), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun NotesAndManualTabBody(
    manual: PocketManual,
    notes: String,
    onNotesChange: (String) -> Unit,
    onPinNote: (String?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Personal Pocket Notes",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.primary,
                )
                if (notes.isNotBlank()) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(cs.primaryContainer)
                            .clickable {
                                val firstLine = notes.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
                                if (!firstLine.isNullOrBlank()) onPinNote(firstLine)
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.PushPin, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(12.dp))
                        Text("Pin to screen", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = cs.onPrimaryContainer)
                    }
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                placeholder = { Text("Jot down door codes, passwords, or NPC clues…") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Overview",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = cs.primary,
            )
            Text(manual.overview, style = MaterialTheme.typography.bodySmall, color = cs.onSurface)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Original Controls Reference",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = cs.primary,
            )
            manual.controls.forEach { (btn, desc) ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(cs.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            btn,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = cs.onSecondaryContainer,
                        )
                    }
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (manual.quickTips.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Field Tips",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.primary,
                )
                manual.quickTips.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodySmall, color = cs.onSurface)
                }
            }
        }
    }
}

@Composable
private fun TrophiesTabBody(
    raData: RaGameData?,
    raHardcore: Boolean,
    trophyFilter: Int,
    onFilterChange: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    if (raData == null || raData.achievements.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "RetroAchievements Tracker",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = cs.onSurface,
                )
                Text(
                    "Sign into RetroAchievements in Settings to track live trophies, missables, and Hardcore unlocks right inside this drawer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    } else {
        val pct = if (raData.totalCount > 0) raData.unlockedCount.toFloat() / raData.totalCount else 0f
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${raData.unlockedCount} / ${raData.totalCount} Trophies",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        if (raHardcore) "HARDCORE" else "SOFTCORE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = cs.onPrimaryContainer,
                    )
                }
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Locked", "Unlocked").forEachIndexed { idx, label ->
                val active = trophyFilter == idx
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (active) cs.secondaryContainer else cs.surfaceContainerHigh)
                        .clickable { onFilterChange(idx) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
                    )
                }
            }
        }

        val filtered = raData.achievements.filter {
            when (trophyFilter) {
                1 -> !it.unlocked
                2 -> it.unlocked
                else -> true
            }
        }
        filtered.forEach { ach ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (ach.unlocked) cs.secondaryContainer.copy(alpha = 0.65f) else cs.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (ach.unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.EmojiEvents,
                        null,
                        tint = if (ach.unlocked) cs.primary else cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            ach.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            ach.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "${ach.points}p",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                        color = cs.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HintTierBox(label: String, body: String, bg: Color, fg: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp), color = fg.copy(alpha = 0.85f))
        Text(body, style = MaterialTheme.typography.bodySmall, color = fg)
    }
}
