package com.retrohero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.retrohero.app.data.AppPaths
import com.retrohero.app.data.Game
import com.retrohero.app.data.NotablePatchCatalog
import com.retrohero.app.data.PatchCompatStatus
import com.retrohero.app.data.PatchVerification
import com.retrohero.app.data.RomCache
import com.retrohero.app.data.RomFingerprint
import com.retrohero.app.data.RomPatch
import com.retrohero.app.data.RomPatchLab
import com.retrohero.app.data.RomPatchStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Guided 1-Tap ROM Patching Hub ("Patch Lab") dialog launched from any Game Page.
 * - Fingerprints the base ROM (CRC32 + SNES 512B copier / NES 16B iNES header detection)
 * - Dry-runs every attached `.ips`, `.bps`, `.ups`, or `.xdelta` patch in memory to verify
 *   compatibility BEFORE booting, with clear green/amber badges
 * - Surfaces curated community translation & QoL spotlights
 * - Guarantees non-destructive soft-patching (original ROM is never modified)
 */
@Composable
fun M3PatchLabDialog(
    game: Game,
    patches: List<RomPatch>,
    onAttachPatch: () -> Unit,
    onPlayOriginal: () -> Unit,
    onPlayWithPatch: (RomPatch) -> Unit,
    onTogglePatchSeparateSave: (RomPatch, Boolean) -> Unit,
    onDeletePatch: (RomPatch) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val spotlight = remember(game.id) { NotablePatchCatalog.forGame(game) }

    var scanning by remember(game.id, patches.size) { mutableStateOf(true) }
    var fingerprint by remember(game.id) { mutableStateOf<RomFingerprint?>(null) }
    var verifications by remember(game.id, patches) { mutableStateOf<Map<String, PatchVerification>>(emptyMap()) }
    var scanError by remember(game.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(game.id, patches) {
        scanning = true
        scanError = null
        withContext(Dispatchers.IO) {
            runCatching {
                val paths = AppPaths(ctx)
                val romFile = RomCache(ctx, paths).prepare(game)
                if (romFile.length() > RomPatchLab.MAX_VERIFY_ROM_BYTES) {
                    scanError = "Large ROM (${romFile.length() / (1024 * 1024)} MB) — verified on boot."
                } else {
                    val romBytes = romFile.readBytes()
                    fingerprint = RomPatchLab.inspectBaseRom(romBytes, game.systemId)
                    val store = RomPatchStore(paths)
                    val results = LinkedHashMap<String, PatchVerification>()
                    for (p in patches) {
                        val pFile = store.patchFile(game, p)
                        if (pFile.exists()) {
                            results[p.id] = RomPatchLab.verifyPatch(
                                sourceRom = romBytes,
                                patchBytes = pFile.readBytes(),
                                systemId = game.systemId,
                                fileName = p.fileName,
                            )
                        }
                    }
                    verifications = results
                }
            }.onFailure { err ->
                scanError = err.message ?: "Could not inspect base ROM."
            }
        }
        scanning = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceContainer,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cs.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ROM Patch Lab",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = cs.onSurface,
                        )
                        Text(
                            "IPS · BPS · UPS · xDelta · Non-destructive in-memory",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, "Close Patch Lab", tint = cs.onSurface)
                    }
                }

                // Base ROM Diagnostics Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Base ROM Fingerprint",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = cs.primary,
                            )
                            if (scanning) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (fingerprint != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Rounded.Verified, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                    Text(
                                        "CRC32 ${fingerprint!!.crc32Hex}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = cs.onSurface,
                                    )
                                }
                            }
                        }
                        val fp = fingerprint
                        if (fp != null) {
                            Text(
                                text = fp.headerStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurface,
                            )
                            Text(
                                text = "Original ROM stays untouched · 512B SNES / 16B NES headers auto-aligned on boot",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                            )
                        } else if (scanError != null) {
                            Text(
                                text = scanError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "Calculating CRC32 & checking copier header…",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Primary Attach + Play Original Row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAttachPatch,
                        modifier = Modifier.weight(1.3f),
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Attach Patch / ZIP")
                    }
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onPlayOriginal()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Original")
                    }
                }

                // Attached Patches with Live Verification Badges
                if (patches.isNotEmpty()) {
                    Text(
                        "Attached Patches (${patches.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.onSurface,
                    )
                    patches.forEach { patch ->
                        val ver = verifications[patch.id]
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cs.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = when (ver?.status) {
                                        PatchCompatStatus.COMPATIBLE,
                                        PatchCompatStatus.COMPATIBLE_WITH_HEADER_FIX -> Color(0xFF4CAF50).copy(alpha = 0.45f)
                                        PatchCompatStatus.INCOMPATIBLE -> Color(0xFFFF7043).copy(alpha = 0.55f)
                                        null -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                ),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cs.tertiaryContainer)
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            patch.format.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                            ),
                                            color = cs.onTertiaryContainer,
                                        )
                                    }
                                    Text(
                                        patch.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = cs.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { onDeletePatch(patch) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Rounded.Delete, "Remove patch", tint = cs.onSurfaceVariant, modifier = Modifier.size(17.dp))
                                    }
                                }

                                // Live Compatibility Status Pill
                                if (ver != null) {
                                    val isOk = ver.status != PatchCompatStatus.INCOMPATIBLE
                                    val tint = if (isOk) Color(0xFF4CAF50) else Color(0xFFFF7043)
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tint.copy(alpha = 0.14f))
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                            null,
                                            tint = tint,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = ver.message,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = cs.onSurface,
                                        )
                                    }
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Switch(
                                            checked = patch.separateSave,
                                            onCheckedChange = { onTogglePatchSeparateSave(patch, it) },
                                        )
                                        Text(
                                            "Separate save",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(cs.primary)
                                            .clickable {
                                                onDismiss()
                                                onPlayWithPatch(patch)
                                            }
                                            .padding(horizontal = 13.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = cs.onPrimary, modifier = Modifier.size(15.dp))
                                        Text(
                                            "Play Patched",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = cs.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Curated Translations & QoL Spotlight
                if (spotlight.isNotEmpty()) {
                    Text(
                        "Notable Translations & QoL Patches",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = cs.primary,
                    )
                    spotlight.forEach { info ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cs.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        info.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = cs.onSecondaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Box(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(cs.secondaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            info.category,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = cs.onSecondaryContainer,
                                        )
                                    }
                                }
                                Text(info.summary, style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer)
                                Text(
                                    info.baseRomTip,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = cs.onSecondaryContainer.copy(alpha = 0.82f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
