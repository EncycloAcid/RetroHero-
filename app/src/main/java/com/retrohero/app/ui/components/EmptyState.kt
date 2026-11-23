package com.retrohero.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.retrohero.app.R

@Composable
fun ArchiveLandscape(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.archive_horizon), null, modifier, contentScale = ContentScale.Crop)
}

@Composable
fun EmptyState(title: String, body: String, action: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().emberBackground().verticalScroll(rememberScrollState())
        .padding(horizontal = Page.Gutter).padding(bottom = LocalNavSpace.current + 24.dp)) {
        PageHeader("RetroHero", null)
        ArchiveLandscape(Modifier.fillMaxWidth().padding(top = 16.dp).aspectRatio(1.8f).clip(RoundedCornerShape(8.dp)))
        ArchiveLabel("Getting started", Modifier.padding(top = 24.dp))
        Text(title, style = MaterialTheme.typography.headlineLargeEmphasized, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Ascend.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        action()
    }
}
