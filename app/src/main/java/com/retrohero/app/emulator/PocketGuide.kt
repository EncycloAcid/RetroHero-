package com.retrohero.app.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/** In-game Pocket Manual & spoiler-free hint booklet tailored to a game and its system. */
data class PocketManual(
    val title: String,
    val badge: String,
    val overview: String,
    val controls: List<Pair<String, String>>,
    val quickTips: List<String>,
    val hints: List<PocketHint>,
)

/**
 * Built-in Spoiler-Free Pocket Guide database for classic retro games, with an automatic
 * system-aware pocket manual fallback so EVERY game in the library has an in-emulator guide
 * without ever tab-switching out of RetroHero.
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
        "link to the past" in title || "zelda" in title && "ocarina" !in title && "awakening" !in title -> PocketManual(
            title = "Hyrule Field Guide",
            badge = "Curated · Spoiler-Free",
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
        )

        "super metroid" in title || "metroid" in title -> PocketManual(
            title = "Zebes Mission Briefing",
            badge = "Curated · Spoiler-Free",
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
            badge = "Curated · Spoiler-Free",
            overview = "Monster-collecting RPG where type matchups, status moves, and HM navigation unlock the region.",
            controls = listOf(
                "A" to "Interact / Confirm",
                "B" to "Cancel / Running Shoes (GBA+)",
                "Select" to "Use Registered Key Item (Bike / Rod)",
                "Start" to "Bag, Party & Save Menu",
            ),
            quickTips = listOf(
                "Pair RetroHero's Portrait One-Handed RPG Mode with Fast-Forward for breezy leveling.",
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
            ),
        )

        "final fantasy" in title -> PocketManual(
            title = "Moogle Field Manual",
            badge = "Curated · Spoiler-Free",
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
            title = "$sysName Pocket Manual",
            badge = "$sysName · No Tab-Switching",
            overview = "In-emulator reference card and personal scratchpad for $title. Keep notes, passwords, and RetroAchievements right here without leaving the game.",
            controls = controls,
            quickTips = listOf(
                "Use Live Screen Translate in the Pause Menu to translate untranslated Japanese dialogue boxes directly over the game.",
                "Use the Notes box below to save dungeon door codes, passwords, or NPC clues — saved automatically per game.",
                "Switch between Authentic and Enhanced under Screen to toggle original LCD/CRT color matrices and overclock fixes.",
            ),
            hints = listOf(
                PocketHint(
                    topic = "Retro Puzzle & Exploration Checklist",
                    nudge = "When stuck in a 1980s–1990s game, re-talk to every town NPC — dialogue often updates after key flags.",
                    clue = "Check your inventory for unequipped key items, inspect suspicious walls/floors, and check the system manual controls above.",
                    solution = "Try testing each sub-item on locked doors/statues, check for a hidden second button chord (like L+R or Select), and jot down any clues in your Pocket Notes tab.",
                ),
            ),
        )
    }
}

/**
 * Slide-over Spoiler-Free Pocket Guide, Manual, Notes, and RetroAchievements Tracker drawer.
 * Lives directly inside EmulatorActivity so the user NEVER leaves the app or loses their process.
 */
@Composable
fun PocketGuideDrawer(
    game: Game,
    manual: PocketManual,
    initialNotes: String,
    raData: RaGameData?,
    raHardcore: Boolean,
    onSaveNotes: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Hints, 1 = Manual & Notes, 2 = Trophies
    var notes by remember(game.id) { mutableStateOf(initialNotes) }
    val revealedTier = remember(game.id) { mutableStateMapOf<Int, Int>() }
    var trophyFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Locked, 2 = Unlocked

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
            color = cs.surfaceContainer,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 310.dp, max = 420.dp)
                .fillMaxWidth(0.88f)
                .clickable(interactionSource = null, indication = null) {},
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    IconButton(onClick = {
                        onSaveNotes(notes)
                        onDismiss()
                    }) {
                        Icon(Icons.Rounded.Close, "Close guide", tint = cs.onSurface)
                    }
                }

                // 3-tab Expressive Pill Selector
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tabs = listOf(
                        Triple(0, "Hints", Icons.Rounded.AutoAwesome),
                        Triple(1, "Manual", Icons.Rounded.Gamepad),
                        Triple(2, "Trophies", Icons.Rounded.EmojiEvents),
                    )
                    tabs.forEach { (idx, label, icon) ->
                        val active = selectedTab == idx
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (active) cs.primaryContainer else cs.surfaceContainerHigh)
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                icon,
                                null,
                                tint = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
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
                        0 -> {
                            Text(
                                "Spoiler-free progressive hints: tap to reveal one tier at a time (Nudge → Clue → Solution).",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                            manual.hints.forEachIndexed { idx, hint ->
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
                                        Text(
                                            hint.topic,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = cs.onSurface,
                                        )
                                        if (tier >= 1) {
                                            HintTierBox("NUDGE", hint.nudge, cs.secondaryContainer, cs.onSecondaryContainer)
                                        }
                                        if (tier >= 2) {
                                            HintTierBox("CLUE", hint.clue, cs.tertiaryContainer, cs.onTertiaryContainer)
                                        }
                                        if (tier >= 3) {
                                            HintTierBox("SOLUTION", hint.solution, cs.primaryContainer, cs.onPrimaryContainer)
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
                                                    else -> "Reveal full solution"
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
                                                Text(
                                                    "All 3 tiers revealed",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cs.onSurfaceVariant,
                                                )
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

                        1 -> {
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

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = cs.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Personal Pocket Notes (Codes & Passwords)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = cs.primary,
                                    )
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = {
                                            notes = it
                                            onSaveNotes(it)
                                        },
                                        placeholder = { Text("Jot down door codes, passwords, or NPC clues…") },
                                        minLines = 3,
                                        maxLines = 6,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        2 -> {
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
                                                .clickable { trophyFilter = idx }
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
                    }
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
