package com.retrohero.app.emulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohero.app.data.QrCodeMatrix

/** Crisp, zero-dependency Compose Canvas QR Code renderer with quiet zone. */
@Composable
fun QrCodeCanvas(
    content: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(content) { QrCodeMatrix.encode(content) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val quiet = 2
            val total = matrix.size + quiet * 2
            val cell = size.width / total.toFloat()
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset((x + quiet) * cell, (y + quiet) * cell),
                            size = Size(cell + 0.5f, cell + 0.5f),
                        )
                    }
                }
            }
        }
    }
}

/** Compact floating HUD pill shown during active 3-Second Rollback Netplay. */
@Composable
fun NetplayHudPill(
    state: NetplayUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pingColor = when {
        state.pingMs <= 45 -> Color(0xFF8CFFAF)
        state.pingMs <= 95 -> Color(0xFFFFD54F)
        else -> Color(0xFFFF8A80)
    }
    val roleLabel = if (state.localPort == 0) "P1" else "P2"
    val label = when (state.phase) {
        NetplayPhase.CONNECTED -> "$roleLabel · ${state.pingMs}ms · RB ${state.rollbackCount}"
        NetplayPhase.HOSTING -> "Room ${state.invite?.formattedCode.orEmpty()} · Waiting…"
        NetplayPhase.JOINING -> "Joining ${state.invite?.formattedCode.orEmpty()}…"
        else -> return
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xD912151E))
            .border(1.dp, Color(0x4DFFFFFF), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Rounded.Wifi,
            null,
            tint = if (state.phase == NetplayPhase.CONNECTED) pingColor else Color(0xFF90CAF9),
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** In-game Material 3 Expressive 3-Second Mobile Rollback Netplay setup & status panel. */
@Composable
fun NetplayPanel(
    coreId: String,
    state: NetplayUiState,
    onHost: (inputDelay: Int) -> Unit,
    onJoin: (codeOrInvite: String, inputDelay: Int) -> Unit,
    onStop: () -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isNativeDeterministic = coreId in setOf("herones", "herobox16", "herochip8")
    var modeTab by remember { mutableIntStateOf(0) } // 0 = Host, 1 = Join
    var inputDelay by remember { mutableIntStateOf(state.inputDelay) }
    var joinInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Core determinism badge
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isNativeDeterministic) cs.tertiaryContainer else cs.secondaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = if (isNativeDeterministic) cs.onTertiaryContainer else cs.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isNativeDeterministic) "Native Deterministic Rollback Core"
                        else "Mobile Rollback Netplay Ready",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isNativeDeterministic) cs.onTertiaryContainer else cs.onSecondaryContainer,
                    )
                    Text(
                        text = "Zero port forwarding · STUN UDP hole-punching + instant Wi-Fi discovery · 16-frame GGPO rollback",
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isNativeDeterministic) cs.onTertiaryContainer else cs.onSecondaryContainer).copy(alpha = 0.84f),
                    )
                }
            }
        }

        if (state.phase == NetplayPhase.CONNECTED) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Connected as Player ${state.localPort + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        text = "Ping: ${state.pingMs} ms · Frame: ${state.frame} · Rollbacks: ${state.rollbackCount} (max ${state.maxRollbackDepth}f) · Delay: ${state.inputDelay}f",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onPrimaryContainer.copy(alpha = 0.88f),
                    )
                    Button(
                        onClick = onStop,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Disconnect Netplay") }
                }
            }
            return
        }

        // Input delay selector (0..2 frames)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Input delay", style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "0f (LAN)", 1 to "1f (Auto)", 2 to "2f (5G)").forEach { (d, label) ->
                    val sel = inputDelay == d
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (sel) cs.primaryContainer else cs.surfaceContainerHigh)
                            .clickable { inputDelay = d }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Host vs Join pill switcher
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Host Room (P1)", 1 to "Join Room (P2)").forEach { (idx, label) ->
                val active = modeTab == idx
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (active) cs.primaryContainer else cs.surfaceContainerHigh)
                        .clickable { modeTab = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    )
                }
            }
        }

        if (modeTab == 0) {
            val invite = state.invite
            if (state.phase == NetplayPhase.HOSTING && invite != null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = invite.formattedCode,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                            ),
                            color = cs.primary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        QrCodeCanvas(
                            content = invite.encode(),
                            modifier = Modifier.size(176.dp),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onCopyInvite(invite.encode()) },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                            Button(
                                onClick = { onShareInvite(invite.encode()) },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                        Text(
                            "Cancel room",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = cs.error,
                            modifier = Modifier.clickable(onClick = onStop).padding(6.dp),
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onHost(inputDelay) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
                ) { Text("Create 6-Digit Room & QR Code") }
            }
        } else {
            OutlinedTextField(
                value = joinInput,
                onValueChange = { joinInput = it },
                label = { Text("6-digit room code or RHNP invite") },
                placeholder = { Text("e.g. 482-917 or paste invite") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.statusMessage.isNotBlank()) {
                Text(
                    state.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.phase == NetplayPhase.ERROR) cs.error else cs.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onJoin(joinInput, inputDelay) },
                enabled = joinInput.isNotBlank() && state.phase != NetplayPhase.JOINING,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
            ) {
                Text(if (state.phase == NetplayPhase.JOINING) "Connecting…" else "Join & Sync Rollback")
            }
        }
    }
}
