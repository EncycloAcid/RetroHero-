package com.retrohero.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.retrohero.app.ui.components.ArchiveLandscape
import com.retrohero.app.ui.components.ArchiveLabel
import com.retrohero.app.ui.components.Ascend
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.retrohero.app.ui.components.press3D
import com.retrohero.app.ui.theme.Salmon

/**
 * First-run setup: welcome, add games, BIOS (optional), done. Every step can be skipped and
 * everything here is also reachable later from Home and Settings.
 */
@Composable
fun OnboardingScreen(
    hasGames: Boolean,
    onAddFolder: () -> Unit,
    onDownloadLink: () -> Unit,
    onHomebrew: () -> Unit,
    onImportBios: () -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = 4
    Box(
        Modifier
            .fillMaxSize()
            .background(com.retrohero.app.ui.components.Page.Bg),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).navigationBarsPadding()) {
            // Skip lives top-right on every page but the last.
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ArchiveLabel("00 — Setup", Modifier.padding(start = 8.dp))
                Spacer(Modifier.weight(1f))
                ArchiveLabel("RetroHero")
                if (page < pages - 1) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = com.retrohero.app.ui.theme.Tone.Muted,
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onFinish).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 380f)) { it / 4 * dir } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(180)) { -it / 4 * dir } + fadeOut(tween(150)))
                },
                label = "onboardingPage",
                modifier = Modifier.weight(1f),
            ) { p ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.Center) {
                    when (p) {
                        0 -> {
                            ArchiveLandscape(Modifier.fillMaxWidth().aspectRatio(1.55f).clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Hero)))
                            ArchiveLabel("The personal game archive", Modifier.padding(top = 22.dp))
                            Title("Your games. Your shelves.")
                            Body("Bring your classic games together. Build shelves, revisit favourites, and pick up where you left off.")
                        }
                        1 -> {
                            Title("Add your games")
                            Body("Point RetroHero at the folder where you keep your game files, or bring some in another way. You can add more any time from the + on Home.")
                            Spacer(Modifier.height(20.dp))
                            Option(Icons.Rounded.CreateNewFolder, "Choose a game folder", "Games already on this phone or SD card", onAddFolder)
                            Option(Icons.Rounded.Download, "Download from a link", "Your own backups on Drive, Dropbox, or your PC/NAS", onDownloadLink)
                            Option(Icons.Rounded.CardGiftcard, "Get free homebrew", "Openly licensed games, one tap each", onHomebrew)
                        }
                        2 -> {
                            Title("BIOS files")
                            Body("A few systems (PlayStation, Sega CD, PC Engine CD, Lynx) need BIOS files from your own consoles. Most games work without them, and you can add them later in Settings.")
                            Spacer(Modifier.height(20.dp))
                            Option(Icons.Rounded.Memory, "Import BIOS files", "Checked by checksum and named correctly for you", onImportBios)
                        }
                        else -> {
                            Icon(Icons.Rounded.SportsEsports, null, tint = com.retrohero.app.ui.theme.Tone.Ink, modifier = Modifier.size(64.dp))
                            Title(if (hasGames) "You're all set." else "Ready when you are.")
                            Body(
                                if (hasGames) "Your games are in the library. Use Shuffle on Home for a random pick, or open a system to browse."
                                else "Add games any time from the + on Home. Free homebrew is a good place to start.",
                            )
                        }
                    }
                }
            }
            ArchiveLabel("Step ${page + 1} of $pages", Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp), Ascend.Muted)
            Row(
                Modifier
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(Ascend.accent)
                    .clickable { if (page < pages - 1) page++ else onFinish() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when (page) {
                        0 -> "Get started"
                        pages - 1 -> "Start playing"
                        else -> "Continue"
                    },
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = Ascend.onAccent,
                )
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.displaySmallEmphasized, color = com.retrohero.app.ui.theme.Tone.Ink, modifier = Modifier.padding(top = 24.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = com.retrohero.app.ui.theme.Tone.Muted, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Option(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .press3D()
            .clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
            .background(com.retrohero.app.ui.theme.Tone.Surface)
            .border(1.dp, com.retrohero.app.ui.theme.Tone.Line, RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Card))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(com.retrohero.app.ui.components.Ascend.Radius.Control)).background(com.retrohero.app.ui.theme.Tone.Raised), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = com.retrohero.app.ui.theme.Tone.Ink)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = com.retrohero.app.ui.theme.Tone.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = com.retrohero.app.ui.theme.Tone.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = com.retrohero.app.ui.theme.Tone.Muted)
    }
}
