package com.retrohero.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.Game
import com.retrohero.app.data.PlayStat
import com.retrohero.app.data.PlayTime
import com.retrohero.app.ui.theme.Inter
import com.retrohero.app.ui.theme.Tone
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Real-world physical packaging families across retro console eras. */
enum class PackagingKind(
    val closedWidth: Dp,
    val closedHeight: Dp,
    val openScale: Float,
    val cornerRadius: Dp,
    val isDiscMedia: Boolean,
    val mediaNoun: String,
) {
    /** Square clear acrylic CD Jewel Case (PS1, Dreamcast, PCE-CD, Neo Geo CD, 3DO). */
    JewelCase(186.dp, 164.dp, 0.86f, 4.dp, true, "disc"),

    /** Tall molded polypropylene DVD Keep Case (PS2, Wii, Saturn, Sega CD). */
    KeepCaseDvd(158.dp, 224.dp, 0.96f, 6.dp, true, "disc"),

    /** GameCube charcoal Keep Case with 8cm Mini-DVD & bottom thumb wells. */
    KeepCaseMiniDisc(158.dp, 224.dp, 0.96f, 6.dp, true, "mini-disc"),

    /** Thick square-ish Nintendo DS / 3DS Keep Case with snapped Game Card. */
    KeepCaseDs(172.dp, 194.dp, 0.90f, 6.dp, false, "game card"),

    /** Slim translucent PSP case with Universal Media Disc (UMD) caddy. */
    PspUmdCase(142.dp, 228.dp, 1.0f, 9.dp, true, "UMD"),

    /** Pebbled black plastic Sega clamshell box (Genesis, 32X, Master System). */
    SegaClamshell(158.dp, 222.dp, 0.96f, 5.dp, false, "cartridge"),

    /** Wide horizontal retail cardboard box (SNES, Nintendo 64). */
    LandscapeCardboard(198.dp, 144.dp, 0.82f, 3.dp, false, "cartridge"),

    /** Vertical printed retail cardboard box (GB, GBC, GBA, NES, Atari, etc.). */
    PortraitCardboard(160.dp, 206.dp, 0.95f, 3.dp, false, "cartridge"),
}

fun packagingFor(systemId: String): PackagingKind = when (systemId) {
    "psx", "dc", "dreamcast", "pcecd", "neocd", "3do" -> PackagingKind.JewelCase
    "ps2", "wii", "saturn", "segacd" -> PackagingKind.KeepCaseDvd
    "gc" -> PackagingKind.KeepCaseMiniDisc
    "nds", "3ds" -> PackagingKind.KeepCaseDs
    "psp" -> PackagingKind.PspUmdCase
    "genesis", "32x", "sms", "sg1000" -> PackagingKind.SegaClamshell
    "snes", "n64" -> PackagingKind.LandscapeCardboard
    else -> PackagingKind.PortraitCardboard
}

fun caseShellColor(systemId: String): Color = when (systemId) {
    "ps2" -> Color(0xFF14244B)       // Iconic PS2 navy-blue polypropylene
    "wii", "3ds" -> Color(0xFFE8E8EE) // Gloss white Wii / 3DS plastic
    "gc" -> Color(0xFF23232B)        // Charcoal GameCube keepcase
    "nds" -> Color(0xFF25262C)       // Dark graphite DS keepcase
    "psp" -> Color(0xFFBCC4D0)       // Translucent clear-frost PSP shell
    "genesis", "sms", "32x" -> Color(0xFF141418) // Pebbled black Sega clamshell
    "dc", "dreamcast" -> Color(0xFF1E2229)
    else -> Color(0xFF1C1D24)
}

/**
 * Renders the authentic closed exterior of a game's physical retail case:
 * clear ribbed jewel-case hinges, molded plastic keepcase lips, console header bands,
 * ratings badges, cardboard tuck-flap seams, and clear-sleeve specular reflections.
 */
@Composable
fun RealisticCaseFront(
    game: Game,
    sheenOffset: Float = 0f,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val kind = packagingFor(game.systemId)
    val shell = caseShellColor(game.systemId)
    val shape = RoundedCornerShape(if (compact) 6.dp else kind.cornerRadius)

    when (kind) {
        PackagingKind.JewelCase -> JewelCaseExterior(game, sheenOffset, compact, shape, modifier)
        PackagingKind.KeepCaseDvd,
        PackagingKind.KeepCaseMiniDisc -> KeepCaseExterior(game, kind, shell, sheenOffset, compact, shape, modifier)
        PackagingKind.KeepCaseDs -> DsKeepCaseExterior(game, shell, sheenOffset, compact, shape, modifier)
        PackagingKind.PspUmdCase -> PspCaseExterior(game, sheenOffset, compact, shape, modifier)
        PackagingKind.SegaClamshell -> ClamshellExterior(game, sheenOffset, compact, shape, modifier)
        PackagingKind.LandscapeCardboard,
        PackagingKind.PortraitCardboard -> CardboardBoxExterior(game, kind, sheenOffset, compact, shape, modifier)
    }
}

@Composable
private fun JewelCaseExterior(
    game: Game,
    sheenOffset: Float,
    compact: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val hingeW = if (compact) 8.dp else 18.dp
    val brandStripH = if (compact) 12.dp else 22.dp
    val isDreamcast = game.systemId == "dc" || game.systemId == "dreamcast"

    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF14151A))
            .border(if (compact) 1.dp else 1.8.dp, Color.White.copy(alpha = 0.34f), shape),
    ) {
        Row(Modifier.fillMaxSize()) {
            // Iconic ribbed clear-over-charcoal CD Jewel Case left hinge spine
            Box(
                Modifier
                    .width(hingeW)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1C22)),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val step = if (compact) 3.dp.toPx() else 4.2.dp.toPx()
                    var y = 6f
                    while (y < size.height - 6f) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.22f),
                            start = Offset(1f, y),
                            end = Offset(size.width - 1f, y),
                            strokeWidth = 1.2f,
                        )
                        drawLine(
                            color = Color.Black.copy(alpha = 0.55f),
                            start = Offset(1f, y + 1.5f),
                            end = Offset(size.width - 1f, y + 1.5f),
                            strokeWidth = 1.2f,
                        )
                        y += step
                    }
                    // Vertical acrylic wall seam
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(size.width - 1f, 0f),
                        end = Offset(size.width - 1f, size.height),
                        strokeWidth = 1.5f,
                    )
                }
            }

            // Square front booklet under clear acrylic lid
            Box(Modifier.weight(1f).fillMaxHeight().padding(if (compact) 1.dp else 2.dp)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        CoverArt(game, Modifier.fillMaxSize())
                        if (!compact) EsrbBadge(Modifier.align(Alignment.BottomStart).padding(6.dp))
                    }
                    // Authentic PlayStation / Dreamcast bottom console branding bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(brandStripH)
                            .background(if (isDreamcast) Color(0xFFF2F2F5) else Color(0xFF0B0B0E))
                            .padding(horizontal = if (compact) 4.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier
                                    .size(if (compact) 5.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isDreamcast) Color(0xFFFF5A1F) else Color(0xFFE60012)),
                            )
                            Text(
                                text = if (isDreamcast) "DREAMCAST" else (game.system?.name ?: "PLAYSTATION").uppercase(),
                                style = TextStyle(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (compact) 6.sp else 8.5.sp,
                                    letterSpacing = 0.12.em,
                                ),
                                color = if (isDreamcast) Color(0xFF151518) else Color.White,
                                maxLines = 1,
                            )
                        }
                        if (!compact) {
                            Text(
                                text = "NTSC",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 7.sp),
                                color = if (isDreamcast) Color(0xFF55555E) else Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                }

                // Clear acrylic lid stopper tabs (top & bottom right) + sharp diagonal glass reflection
                AcrylicGlareOverlay(sheenOffset, crisp = true)
            }
        }
    }
}

@Composable
private fun KeepCaseExterior(
    game: Game,
    kind: PackagingKind,
    shell: Color,
    sheenOffset: Float,
    compact: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val lightShell = game.systemId == "wii"
    val lipPad = if (compact) 2.dp else 3.5.dp
    val headerH = if (compact) 13.dp else 22.dp

    // Outer molded polypropylene plastic lip visible around top, right, and bottom edges
    Box(
        modifier
            .clip(shape)
            .background(shell)
            .border(1.dp, Color.White.copy(alpha = if (lightShell) 0.55f else 0.18f), shape)
            .padding(top = lipPad, bottom = lipPad, end = lipPad, start = 0.dp),
    ) {
        // Paper insert tucked under clear outer vinyl sleeve
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(Color(0xFF0E0E12)),
        ) {
            // Console top banner (PS2 black/cyan bar, GameCube charcoal arch, Wii white bar)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(headerH)
                    .background(
                        when (game.systemId) {
                            "wii" -> Color(0xFFF5F5F8)
                            "ps2" -> Color(0xFF08090D)
                            else -> Color(0xFF14141A)
                        },
                    ),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = if (compact) 5.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = when (game.systemId) {
                            "ps2" -> "PlayStation 2"
                            "gc" -> "NINTENDO GAMECUBE"
                            "wii" -> "Wii"
                            else -> (game.system?.shortName ?: game.systemId).uppercase()
                        },
                        style = TextStyle(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compact) 6.5.sp else 8.5.sp,
                            letterSpacing = if (game.systemId == "ps2") 0.03.em else 0.09.em,
                        ),
                        color = if (lightShell) Color(0xFF6B6E75) else Color.White,
                        maxLines = 1,
                    )
                    if (!compact) {
                        Text(
                            text = if (kind == PackagingKind.KeepCaseMiniDisc) "ONLY FOR" else "NTSC",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 6.5.sp),
                            color = when (game.systemId) {
                                "ps2" -> Color(0xFF2884FF)
                                "gc" -> Color(0xFF8C7AE6)
                                else -> Color(0xFF888894)
                            },
                        )
                    }
                }
                // PS2 / GameCube signature colored underline rule
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(if (compact) 1.dp else 2.dp)
                        .background(
                            when (game.systemId) {
                                "ps2" -> Brush.horizontalGradient(listOf(Color(0xFF1E5BFF), Color(0xFF00D2FF)))
                                "gc" -> Brush.horizontalGradient(listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE)))
                                else -> Brush.horizontalGradient(listOf(Color(0xFF3498DB), Color(0xFF3498DB)))
                            },
                        ),
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                CoverArt(game, Modifier.fillMaxSize())
                if (!compact) EsrbBadge(Modifier.align(Alignment.BottomStart).padding(7.dp))
            }
        }

        // Left round keepcase spine crease + vinyl sleeve sheen
        SpineCreaseShadow(width = if (compact) 5.dp else 8.dp)
        AcrylicGlareOverlay(sheenOffset, crisp = false)
    }
}

@Composable
private fun DsKeepCaseExterior(
    game: Game,
    shell: Color,
    sheenOffset: Float,
    compact: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val is3ds = game.systemId == "3ds"
    val lip = if (compact) 2.dp else 3.5.dp
    val sideStripW = if (compact) 12.dp else 22.dp

    Box(
        modifier
            .clip(shape)
            .background(shell)
            .border(1.dp, Color.White.copy(alpha = if (is3ds) 0.6f else 0.18f), shape)
            .padding(top = lip, bottom = lip, end = lip),
    ) {
        Row(Modifier.fillMaxSize()) {
            // Iconic vertical left NINTENDO DS / 3DS branding bar
            Box(
                Modifier
                    .width(sideStripW)
                    .fillMaxHeight()
                    .background(if (is3ds) Color(0xFFF6F6F9) else Color(0xFF14151A)),
                contentAlignment = Alignment.Center,
            ) {
                if (!compact) {
                    Text(
                        text = if (is3ds) "NINTENDO 3DS" else "NINTENDO DS",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 8.sp, letterSpacing = 0.14.em),
                        color = if (is3ds) Color(0xFF18181C) else Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.requiredWidth(130.dp).graphicsLayer { rotationZ = -90f },
                    )
                }
                if (is3ds) {
                    Box(Modifier.align(Alignment.TopEnd).width(2.5.dp).fillMaxHeight().background(Color(0xFFE60012)))
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                CoverArt(game, Modifier.fillMaxSize())
                if (!compact) EsrbBadge(Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
        }
        SpineCreaseShadow(width = if (compact) 4.dp else 6.dp)
        AcrylicGlareOverlay(sheenOffset, crisp = false)
    }
}

@Composable
private fun PspCaseExterior(
    game: Game,
    sheenOffset: Float,
    compact: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val pad = if (compact) 2.dp else 4.dp
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFFDCE2EC).copy(alpha = 0.26f))
            .border(1.5.dp, Color.White.copy(alpha = 0.42f), shape)
            .padding(top = pad, bottom = pad, end = pad, start = 1.dp),
    ) {
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(if (compact) 12.dp else 20.dp)
                    .background(Color(0xFF101116))
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "PSP",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 7.sp else 9.5.sp, letterSpacing = 0.18.em),
                    color = Color.White,
                )
                if (!compact) {
                    Text("UMD", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 7.sp), color = Color(0xFFB5BCC8))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CoverArt(game, Modifier.fillMaxSize())
            }
        }
        AcrylicGlareOverlay(sheenOffset, crisp = true)
    }
}

@Composable
private fun ClamshellExterior(
    game: Game,
    sheenOffset: Float,
    compact: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val lip = if (compact) 2.5.dp else 5.dp
    val isSms = game.systemId == "sms"
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF141418))
            .border(1.5.dp, Color(0xFF2D2D36), shape)
            .padding(top = lip, bottom = lip, end = lip, start = 2.dp),
    ) {
        Row(Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp))) {
            // Classic Sega Genesis crimson / Master System white side accent band
            Box(
                Modifier
                    .width(if (compact) 8.dp else 16.dp)
                    .fillMaxHeight()
                    .background(
                        if (isSms) SolidColor(Color(0xFFECECF2))
                        else Brush.verticalGradient(listOf(Color(0xFFD61C24), Color(0xFF7D0D12))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!compact) {
                    Text(
                        text = if (isSms) "MASTER SYSTEM" else "GENESIS",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 7.5.sp, letterSpacing = 0.16.em),
                        color = if (isSms) Color(0xFF18181E) else Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.requiredWidth(120.dp).graphicsLayer { rotationZ = -90f },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                CoverArt(game, Modifier.fillMaxSize())
            }
        }
        SpineCreaseShadow(width = if (compact) 5.dp else 8.dp)
        AcrylicGlareOverlay(sheenOffset, crisp = false)
    }
}

@Composable
private fun CardboardBoxExterior(
    game: Game,
    kind: PackagingKind,
    sheenOffset: Float,
    compact: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val isHandheld = game.systemId in setOf("gb", "gbc", "gba", "gg", "lynx", "ngp", "ws")
    val isLandscape = kind == PackagingKind.LandscapeCardboard

    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF1D1C21))
            .border(1.dp, Color.White.copy(alpha = 0.20f), shape),
    ) {
        if (isLandscape) {
            // SNES / N64 horizontal retail box: artwork on left, vertical red/charcoal Nintendo band on right edge
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    CoverArt(game, Modifier.fillMaxSize())
                    if (!compact) NintendoSealOval(Modifier.align(Alignment.BottomEnd).padding(6.dp))
                }
                Box(
                    Modifier
                        .width(if (compact) 12.dp else 22.dp)
                        .fillMaxHeight()
                        .background(
                            if (game.systemId == "n64") Color(0xFFE60012) else Color(0xFF141418),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (game.systemId != "n64") {
                        Box(Modifier.align(Alignment.CenterStart).width(2.5.dp).fillMaxHeight().background(Color(0xFFE60012)))
                    }
                    if (!compact) {
                        Text(
                            text = if (game.systemId == "n64") "NINTENDO 64" else "SUPER NINTENDO",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 7.5.sp, letterSpacing = 0.14.em),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.requiredWidth(120.dp).graphicsLayer { rotationZ = -90f },
                        )
                    }
                }
            }
        } else if (isHandheld) {
            // Game Boy / GBC / GBA metallic foil left band + square cover
            Row(Modifier.fillMaxSize()) {
                val bandBrush = when (game.systemId) {
                    "gba" -> Brush.verticalGradient(listOf(Color(0xFF4F46B8), Color(0xFF2B2470), Color(0xFF4F46B8)))
                    "gbc" -> Brush.verticalGradient(listOf(Color(0xFF00A8CC), Color(0xFF7B2CBF), Color(0xFFFF006E)))
                    else -> Brush.verticalGradient(listOf(Color(0xFFC8CCD4), Color(0xFF8C929E), Color(0xFFD8DCE4)))
                }
                val darkInk = game.systemId == "gb"
                Box(
                    Modifier
                        .width(if (compact) 12.dp else 24.dp)
                        .fillMaxHeight()
                        .background(bandBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!compact) {
                        Text(
                            text = when (game.systemId) {
                                "gba" -> "GAME BOY ADVANCE"
                                "gbc" -> "GAME BOY COLOR"
                                "gb" -> "GAME BOY"
                                else -> (game.system?.shortName ?: game.systemId).uppercase()
                            },
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 7.5.sp, letterSpacing = 0.12.em),
                            color = if (darkInk) Color(0xFF1A1C22) else Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.requiredWidth(145.dp).graphicsLayer { rotationZ = -90f },
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    CoverArt(game, Modifier.fillMaxSize())
                    if (!compact) NintendoSealOval(Modifier.align(Alignment.BottomEnd).padding(6.dp))
                }
            }
        } else {
            // NES / Classic tall cardboard box with subtle top tuck-flap & bottom seal
            Box(Modifier.fillMaxSize()) {
                CoverArt(game, Modifier.fillMaxSize())
                if (!compact) NintendoSealOval(Modifier.align(Alignment.BottomEnd).padding(7.dp))
            }
        }

        // Authentic cardboard tuck-flap crease lines & subtle paperboard edge highlights
        Canvas(Modifier.fillMaxSize()) {
            // Top & bottom tuck-flap score creases
            val inset = if (compact) 3.dp.toPx() else 5.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, inset),
                end = Offset(size.width, inset),
                strokeWidth = 1f,
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.45f),
                start = Offset(0f, inset + 1.2f),
                end = Offset(size.width, inset + 1.2f),
                strokeWidth = 1f,
            )
            // Left fold score + subtle vintage paperboard corner sheen
            drawLine(
                color = Color.White.copy(alpha = 0.26f),
                start = Offset(2.5f, 0f),
                end = Offset(2.5f, size.height),
                strokeWidth = 1.5f,
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    start = Offset(-40f + sheenOffset, 0f),
                    end = Offset(size.width * 0.6f + sheenOffset, size.height * 0.7f),
                ),
            )
        }
    }
}

@Composable
private fun EsrbBadge(modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
            .border(0.8.dp, Color.Black, RoundedCornerShape(2.dp))
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ESRB", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 3.5.sp), color = Color.Black, maxLines = 1)
        Box(
            Modifier.fillMaxWidth().background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("T", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 8.sp, lineHeight = 8.sp), color = Color.Black)
        }
    }
}

@Composable
private fun NintendoSealOval(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 22.dp, height = 13.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.2.dp, Color(0xFFC9A227), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("SEAL", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 4.5.sp), color = Color(0xFF9C7A14))
    }
}

@Composable
private fun SpineCreaseShadow(width: Dp) {
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.28f),
                    ),
                ),
            ),
    )
}

@Composable
private fun AcrylicGlareOverlay(sheenOffset: Float, crisp: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (crisp) 0.20f else 0.12f),
                    Color.White.copy(alpha = if (crisp) 0.05f else 0.03f),
                    Color.Transparent,
                ),
                start = Offset(-50f + sheenOffset, 0f),
                end = Offset(size.width * 0.62f + sheenOffset, size.height * 0.68f),
            ),
        )
    }
}

// =================================================================================================
// REALISTIC OPEN CASE INTERIORS (Left Inside Lid + Right Molded Tray & Physical Media)
// =================================================================================================

@Composable
fun RealisticInsideLid(
    game: Game,
    stat: PlayStat?,
    modifier: Modifier = Modifier,
) {
    val kind = packagingFor(game.systemId)
    val shell = caseShellColor(game.systemId)
    val isCardboard = kind == PackagingKind.LandscapeCardboard || kind == PackagingKind.PortraitCardboard
    val isJewel = kind == PackagingKind.JewelCase
    val isKeep = kind == PackagingKind.KeepCaseDvd || kind == PackagingKind.KeepCaseMiniDisc
    val lightShell = game.systemId == "wii" || game.systemId == "3ds"

    val lidBg = when {
        isCardboard -> Color(0xFFDAD6CE) // Authentic unbleached grey-white paperboard inner wall!
        isJewel -> Color(0xFF1A1C24)     // Clear dark-tinted acrylic lid
        lightShell -> Color(0xFFE4E4EC)
        else -> shell
    }

    Box(
        modifier
            .clip(RoundedCornerShape(kind.cornerRadius))
            .background(lidBg)
            .border(
                width = 2.dp,
                color = if (isCardboard) Color(0xFFB5B0A4) else Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(kind.cornerRadius),
            )
            .padding(if (isCardboard) 8.dp else 10.dp),
    ) {
        // Printed instruction manual booklet tucked inside the left cover
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    top = if (isKeep) 28.dp else 2.dp, // Leave space above booklet for PS2/GC Memory Card slot!
                    end = 4.dp,
                )
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1C1C24))
                .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(4.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.48f),
            ) {
                CoverArt(game, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1C1C24)))),
                )
            }
            Column(
                Modifier
                    .weight(0.52f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "INSTRUCTION BOOKLET",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 6.5.sp, letterSpacing = 0.12.em),
                        color = Ascend.accent,
                        maxLines = 1,
                    )
                    Text(
                        text = game.title,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 12.5.sp),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (stat != null && stat.seconds > 0) PlayTime.format(stat.seconds) else (game.system?.shortName ?: game.systemId.uppercase()),
                    style = TextStyle(fontFamily = Inter, fontSize = 8.5.sp),
                    color = Tone.Muted,
                    maxLines = 1,
                )
            }
        }

        // Authentic PS2 / GameCube Memory Card Holder molded at top of left inside cover!
        if (isKeep) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 48.dp, height = 22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.36f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "MEM CARD",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 5.5.sp, letterSpacing = 0.08.em),
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
        }

        // Authentic retaining hardware (clear jewel dimples vs molded keepcase plastic clips)
        if (isJewel) {
            listOf(0.18f, 0.82f).forEach { frac ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (kind.closedHeight.value * frac - 12f).dp)
                        .size(width = 8.dp, height = 16.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .background(Color.White.copy(alpha = 0.28f)),
                )
            }
        } else if (!isCardboard) {
            listOf(0.34f, 0.74f).forEach { frac ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (kind.closedHeight.value * frac - 14f).dp)
                        .size(width = 16.dp, height = 24.dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                        .background(lidBg)
                        .border(1.2.dp, Color.Black.copy(alpha = 0.35f), RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)),
                )
            }
        }
    }
}

@Composable
fun RealisticMediaTray(
    game: Game,
    spinDegrees: Float,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = packagingFor(game.systemId)
    val shell = caseShellColor(game.systemId)
    val isCardboard = kind == PackagingKind.LandscapeCardboard || kind == PackagingKind.PortraitCardboard
    val lightShell = game.systemId == "wii" || game.systemId == "3ds"

    val trayBg = when {
        isCardboard -> Color(0xFFE6E3DC) // Authentic white/ivory die-cut folded cardboard inner tray!
        kind == PackagingKind.JewelCase -> Color(0xFF191B22)
        lightShell -> Color(0xFFEAEAF0)
        else -> shell
    }

    Box(
        modifier
            .clip(RoundedCornerShape(kind.cornerRadius))
            .background(trayBg)
            .border(
                width = 2.dp,
                color = if (isCardboard) Color(0xFFC2BDB2) else Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(kind.cornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            PackagingKind.JewelCase -> JewelCdTray(game, spinDegrees, ejectProgress, enabled, onEjectAndPlay)
            PackagingKind.KeepCaseDvd -> DvdKeepTray(game, spinDegrees, ejectProgress, enabled, onEjectAndPlay, miniDisc = false)
            PackagingKind.KeepCaseMiniDisc -> DvdKeepTray(game, spinDegrees, ejectProgress, enabled, onEjectAndPlay, miniDisc = true)
            PackagingKind.PspUmdCase -> PspUmdTray(game, spinDegrees, ejectProgress, enabled, onEjectAndPlay)
            PackagingKind.KeepCaseDs -> DsGameCardTray(game, ejectProgress, enabled, onEjectAndPlay)
            PackagingKind.SegaClamshell -> ClamshellCartridgeTray(game, ejectProgress, enabled, onEjectAndPlay)
            PackagingKind.LandscapeCardboard,
            PackagingKind.PortraitCardboard -> CardboardCartridgeTray(game, kind, ejectProgress, enabled, onEjectAndPlay)
        }
    }
}

@Composable
private fun JewelCdTray(
    game: Game,
    spinDegrees: Float,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Serrated jewel-case top/bottom tray rails + 4 finger cutouts + 12-tooth rosette hub
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension * 0.43f
            val center = Offset(w / 2f, h / 2f)
            drawCircle(color = Color(0xFF0E1015), radius = r, center = center)
            // 4 diagonal finger-release wells around the disc perimeter
            listOf(45f, 135f, 225f, 315f).forEach { deg ->
                val rad = Math.toRadians(deg.toDouble())
                val fx = center.x + (r * 0.92f) * cos(rad).toFloat()
                val fy = center.y + (r * 0.92f) * sin(rad).toFloat()
                drawCircle(color = Color(0xFF08090C), radius = 13.dp.toPx(), center = Offset(fx, fy))
            }
            drawCircle(color = Color.White.copy(alpha = 0.08f), radius = r, center = center, style = Stroke(width = 1.5f))
        }

        OpticalDiscWidget(
            game = game,
            discSize = 134.dp,
            isPlayStationBlackRim = game.systemId == "psx",
            spinDegrees = spinDegrees,
            ejectProgress = ejectProgress,
            enabled = enabled,
            onEjectAndPlay = onEjectAndPlay,
        )
    }
}

@Composable
private fun DvdKeepTray(
    game: Game,
    spinDegrees: Float,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
    miniDisc: Boolean,
) {
    val discDp = if (miniDisc) 96.dp else 128.dp
    val wellDp = if (miniDisc) 108.dp else 138.dp

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Molded circular disc well + top/bottom thumb-release indentations
        Canvas(Modifier.size(wellDp + 24.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val wr = wellDp.toPx() / 2f
            drawCircle(color = Color.Black.copy(alpha = 0.36f), radius = wr, center = c)
            drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 14.dp.toPx(), center = Offset(c.x, c.y - wr))
            drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 14.dp.toPx(), center = Offset(c.x, c.y + wr))
            drawCircle(color = Color.White.copy(alpha = 0.10f), radius = wr, center = c, style = Stroke(width = 1.5f))
        }

        OpticalDiscWidget(
            game = game,
            discSize = discDp,
            isPlayStationBlackRim = false,
            spinDegrees = spinDegrees,
            ejectProgress = ejectProgress,
            enabled = enabled,
            onEjectAndPlay = onEjectAndPlay,
        )
    }
}

@Composable
private fun OpticalDiscWidget(
    game: Game,
    discSize: Dp,
    isPlayStationBlackRim: Boolean,
    spinDegrees: Float,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    Box(
        Modifier
            .size(discSize)
            .offset { IntOffset(0, (-34f * ejectProgress).roundToInt()) }
            .graphicsLayer {
                val s = 1f + 0.12f * ejectProgress
                scaleX = s
                scaleY = s
                rotationZ = spinDegrees
            }
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier)
            .semantics { contentDescription = "Optical disc for ${game.title}" },
        contentAlignment = Alignment.Center,
    ) {
        CoverArt(game, Modifier.fillMaxSize())

        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Iridescent rainbow optical foil reflection
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color.White.copy(alpha = 0.25f),
                    0.16f to Color(0xFF5CE1E6).copy(alpha = 0.20f),
                    0.33f to Color.Transparent,
                    0.50f to Color(0xFFFF85C0).copy(alpha = 0.22f),
                    0.67f to Color(0xFFFFEA79).copy(alpha = 0.18f),
                    0.84f to Color.Transparent,
                    1.00f to Color.White.copy(alpha = 0.25f),
                ),
            )

            // Outer polycarbonate rim (PS1 iconic black CD rim vs silver mirror rim)
            drawCircle(
                color = if (isPlayStationBlackRim) Color(0xFF0A0A0E) else Color(0xFFE8ECF2).copy(alpha = 0.78f),
                radius = r - 1.5f,
                center = c,
                style = Stroke(width = 3.dp.toPx()),
            )

            // Translucent inner clamping ring + 12-tooth rosette center spindle hub!
            drawCircle(color = Color(0xFFDCE0E8).copy(alpha = 0.68f), radius = r * 0.28f, center = c)
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = r * 0.28f, center = c, style = Stroke(width = 1.2f))
            drawCircle(color = Color(0xFF16171E), radius = r * 0.15f, center = c)

            // 12 radial rosette teeth around the center spindle
            for (i in 0 until 12) {
                val a = Math.toRadians((i * 30).toDouble())
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(c.x + r * 0.08f * cos(a).toFloat(), c.y + r * 0.08f * sin(a).toFloat()),
                    end = Offset(c.x + r * 0.15f * cos(a).toFloat(), c.y + r * 0.15f * sin(a).toFloat()),
                    strokeWidth = 1.5f,
                )
            }
        }
    }
}

@Composable
private fun PspUmdTray(
    game: Game,
    spinDegrees: Float,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    // Authentic PSP UMD: rounded white/clear protective cartridge shell housing a miniature optical disc!
    Box(
        Modifier
            .size(width = 104.dp, height = 108.dp)
            .offset { IntOffset(0, (-32f * ejectProgress).roundToInt()) }
            .graphicsLayer {
                val s = 1f + 0.10f * ejectProgress
                scaleX = s
                scaleY = s
            }
            .shadow(10.dp, RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(Color(0xFFEBEef4))
            .border(2.dp, Color.White, RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .then(if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier)
            .semantics { contentDescription = "Optical disc for ${game.title}" }
            .padding(9.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Miniature spinning optical disc visible inside the UMD shell
        Box(
            Modifier
                .size(84.dp)
                .graphicsLayer { rotationZ = spinDegrees }
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CoverArt(game, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFD8DCE6), radius = size.minDimension * 0.22f)
                drawCircle(color = Color(0xFF1A1C22), radius = size.minDimension * 0.10f)
            }
        }
        Text(
            text = "PSP UMD",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 6.5.sp, letterSpacing = 0.12.em),
            color = Color(0xFF3A3D47),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEBEef4).copy(alpha = 0.92f))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun DsGameCardTray(
    game: Game,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    val is3ds = game.systemId == "3ds"
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Molded card socket with left/right retaining clips
        Box(
            Modifier
                .size(width = 86.dp, height = 94.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.34f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Tiny sculpted DS / 3DS Game Card (with 3DS right tab or charcoal DS body)
            Column(
                Modifier
                    .size(width = 66.dp, height = 74.dp)
                    .offset { IntOffset(0, (-30f * ejectProgress).roundToInt()) }
                    .graphicsLayer {
                        val s = 1f + 0.12f * ejectProgress
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(8.dp, RoundedCornerShape(5.dp))
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 5.dp, bottomEnd = 10.dp))
                    .background(if (is3ds) Color(0xFFEAEAEF) else Color(0xFF2D2E36))
                    .border(1.2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                    .then(if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier)
                    .semantics { contentDescription = "ROM cartridge for ${game.title}" }
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Downward insertion triangle embossed at top
                Text("▼", fontSize = 5.sp, color = if (is3ds) Color(0xFF888892) else Color.White.copy(alpha = 0.35f))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.6.dp, Color.Black.copy(alpha = 0.4f), RoundedCornerShape(3.dp)),
                ) {
                    CoverArt(game, Modifier.weight(1f).fillMaxWidth())
                    Box(
                        Modifier.fillMaxWidth().height(9.dp).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (is3ds) "CTR-P-NTR" else "NTR-005",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 5.sp),
                            color = Color(0xFF222228),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClamshellCartridgeTray(
    game: Game,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Molded black clamshell cradle walls
        Box(
            Modifier
                .size(width = 122.dp, height = 106.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0A0D))
                .border(1.5.dp, Color(0xFF282832), RoundedCornerShape(6.dp)),
        )

        // Authentic wide domed black Sega Genesis / Mega Drive Cartridge!
        Column(
            Modifier
                .size(width = 112.dp, height = 92.dp)
                .offset { IntOffset(0, (-32f * ejectProgress).roundToInt()) }
                .graphicsLayer {
                    val s = 1f + 0.10f * ejectProgress
                    scaleX = s
                    scaleY = s
                }
                .shadow(10.dp, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF282830), Color(0xFF141418))))
                .border(1.2.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .then(if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier)
                .semantics { contentDescription = "ROM cartridge for ${game.title}" }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .border(0.8.dp, Color(0xFFD61C24), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp)),
            ) {
                CoverArt(game, Modifier.fillMaxSize())
            }
            GoldPcbPins(width = 76.dp)
        }
    }
}

@Composable
private fun CardboardCartridgeTray(
    game: Game,
    kind: PackagingKind,
    ejectProgress: Float,
    enabled: Boolean,
    onEjectAndPlay: () -> Unit,
) {
    val isGba = game.systemId == "gba"
    val isGb = game.systemId == "gb" || game.systemId == "gbc"
    val isSnesOrN64 = kind == PackagingKind.LandscapeCardboard

    val cartW = when {
        isSnesOrN64 -> 134.dp
        isGba -> 116.dp
        isGb -> 98.dp
        else -> 104.dp // NES / Classic tall cart
    }
    val cartH = when {
        isSnesOrN64 -> 90.dp
        isGba -> 74.dp
        isGb -> 114.dp
        else -> 132.dp
    }
    val cartColorTop = when (game.systemId) {
        "gba" -> Color(0xFF32343E)
        "gbc" -> Color(0xFF453B6B) // Atomic purple GBC cart!
        "snes", "n64", "nes", "gb" -> Color(0xFFB6B6BE) // Classic Nintendo warm light-grey ABS plastic!
        else -> Color(0xFF2E2F38)
    }
    val cartColorBottom = when (game.systemId) {
        "gba" -> Color(0xFF22232B)
        "gbc" -> Color(0xFF2B2347)
        "snes", "n64", "nes", "gb" -> Color(0xFF96969F)
        else -> Color(0xFF1F2026)
    }

    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        // Die-cut folded white cardboard inner tray flaps + dark recessed cutout cavity!
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Die-cut fold crease lines on the white cardboard insert
            drawRect(
                color = Color(0xFFC5C0B4),
                topLeft = Offset(6f, 6f),
                size = Size(w - 12f, h - 12f),
                style = Stroke(width = 1.5f),
            )
            // Shadowed cutout cavity holding the cartridge
            val cw = (cartW.toPx() + 14f).coerceAtMost(w - 16f)
            val ch = (cartH.toPx() + 14f).coerceAtMost(h - 16f)
            drawRoundRect(
                color = Color(0xFF1A1A20),
                topLeft = Offset((w - cw) / 2f, (h - ch) / 2f),
                size = Size(cw, ch),
                cornerRadius = CornerRadius(14f, 14f),
            )
        }

        // Authentic Console-Specific Sculpted ROM Cartridge!
        val cartShape = when {
            game.systemId == "n64" -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 5.dp)
            isGb -> RoundedCornerShape(topStart = 6.dp, topEnd = 14.dp, bottomStart = 5.dp, bottomEnd = 5.dp) // GB top-right power notch!
            else -> RoundedCornerShape(6.dp)
        }

        Column(
            Modifier
                .size(cartW, cartH)
                .offset { IntOffset(0, (-32f * ejectProgress).roundToInt()) }
                .graphicsLayer {
                    val s = 1f + 0.10f * ejectProgress
                    scaleX = s
                    scaleY = s
                }
                .shadow(9.dp, cartShape)
                .clip(cartShape)
                .background(Brush.verticalGradient(listOf(cartColorTop, cartColorBottom)))
                .border(1.2.dp, Color.Black.copy(alpha = 0.28f), cartShape)
                .then(if (enabled) Modifier.clickable(onClick = onEjectAndPlay) else Modifier)
                .semantics { contentDescription = "ROM cartridge for ${game.title}" }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Embossed top grip / header detail per cartridge era
            if (isGba) {
                Text(
                    "GAME BOY ADVANCE",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 5.5.sp, letterSpacing = 0.10.em),
                    color = Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) {
                        Box(Modifier.size(width = 10.dp, height = 2.5.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.22f)))
                    }
                }
            }

            // Glossy recessed cartridge artwork sticker
            Box(
                Modifier
                    .fillMaxWidth(if (isSnesOrN64) 0.78f else 0.92f)
                    .weight(1f)
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(0.8.dp, Color.Black.copy(alpha = 0.38f), RoundedCornerShape(4.dp)),
            ) {
                CoverArt(game, Modifier.fillMaxSize())
            }

            GoldPcbPins(width = cartW * 0.62f)
        }
    }
}

@Composable
private fun GoldPcbPins(width: Dp) {
    Canvas(Modifier.width(width).height(5.dp)) {
        val pins = 11
        val step = size.width / pins
        for (i in 0 until pins) {
            drawRoundRect(
                color = Color(0xFFD4AF37),
                topLeft = Offset(i * step + 1.2f, 0f),
                size = Size((step - 2.4f).coerceAtLeast(1.5f), size.height),
                cornerRadius = CornerRadius(1.2f, 1.2f),
            )
        }
    }
}

/**
 * Renders a realistic standing side spine on the Vault 3D Bookshelf, tailored per case family
 * (ribbed clear jewel spine, curved keepcase spine with console header, black-and-red Sega clamshell,
 * or creased cardboard box spine).
 */
@Composable
fun RealisticCaseSpine(
    game: Game,
    height: Dp,
    onClick: () -> Unit,
) {
    val kind = packagingFor(game.systemId)
    val shell = caseShellColor(game.systemId)
    val accent = posterAccentColor(game)
    val isJewel = kind == PackagingKind.JewelCase
    val isCardboard = kind == PackagingKind.LandscapeCardboard || kind == PackagingKind.PortraitCardboard
    val lightSpine = game.systemId == "wii" || game.systemId == "3ds" || game.systemId == "gb"

    val spineWidth = when (kind) {
        PackagingKind.JewelCase -> 24.dp        // Slimmer CD jewel spine!
        PackagingKind.PspUmdCase -> 26.dp
        PackagingKind.SegaClamshell -> 38.dp    // Chunky black Sega clamshell spine!
        else -> 33.dp
    }
    val actualHeight = if (isJewel) height - 26.dp else height
    val shape = RoundedCornerShape(topStart = if (isCardboard) 2.dp else 5.dp, topEnd = if (isCardboard) 2.dp else 5.dp)

    Box(
        Modifier
            .width(spineWidth)
            .height(actualHeight)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(
                when {
                    lightSpine -> Brush.horizontalGradient(listOf(Color(0xFFD8D8E0), Color(0xFFF4F4F8), Color(0xFFD0D0DA)))
                    isJewel -> Brush.verticalGradient(listOf(Color(0xFF20222A), Color(0xFF121318)))
                    kind == PackagingKind.SegaClamshell -> Brush.verticalGradient(listOf(Color(0xFFB51820), Color(0xFF18181E), Color(0xFF121216)))
                    else -> Brush.verticalGradient(listOf(shell, accent.copy(alpha = 0.30f), Color(0xFF121218)))
                },
            )
            .border(
                width = 1.dp,
                color = if (isJewel) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.14f),
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        // Top console header cap on the spine
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(22.dp)
                .background(
                    when {
                        game.systemId == "ps2" -> Color(0xFF0A0C12)
                        game.systemId == "3ds" -> Color(0xFFE60012)
                        lightSpine -> Color(0xFF22222A)
                        else -> Color.Black.copy(alpha = 0.48f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (game.system?.shortName ?: game.systemId).take(4).uppercase(),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 7.sp),
                color = Color.White,
                maxLines = 1,
            )
        }

        // Vertical spine title
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 24.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = game.title.uppercase(),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isJewel) 8.sp else 9.sp,
                    letterSpacing = 0.05.em,
                ),
                color = if (lightSpine) Color(0xFF16161C) else Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .requiredWidth(actualHeight - 36.dp)
                    .graphicsLayer { rotationZ = -90f },
            )
        }

        // Cylindrical plastic sheen / jewel rib texture
        Canvas(Modifier.fillMaxSize()) {
            if (isJewel) {
                drawLine(Color.White.copy(alpha = 0.28f), Offset(2f, 0f), Offset(2f, size.height), 1.5f)
                drawLine(Color.White.copy(alpha = 0.28f), Offset(size.width - 2f, 0f), Offset(size.width - 2f, size.height), 1.5f)
            } else {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.32f),
                        ),
                    ),
                )
            }
        }
    }
}

