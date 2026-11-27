package com.retrohero.app.emulator

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrohero.app.data.Game
import java.net.URLEncoder

/** Pure-Kotlin URL & level-guide shortcut helper for the In-Game Browser. */
object InGameBrowserUrls {

    data class QuickLink(val label: String, val url: String)

    fun quickLinksFor(game: Game): List<QuickLink> {
        val title = game.title.trim()
        val sys = game.system?.shortName?.trim().orEmpty()
        val full = listOf(title, sys).filter { it.isNotBlank() }.joinToString(" ")
        return listOf(
            QuickLink("Walkthrough", searchUrl("$full walkthrough guide")),
            QuickLink("GameFAQs", "https://gamefaqs.gamespot.com/search?game=${enc(title)}"),
            QuickLink("Maps", "https://duckduckgo.com/?q=${enc("$full level map")}&iax=images&ia=images"),
            QuickLink("Boss Tips", searchUrl("$full boss guide tips")),
            QuickLink("StrategyWiki", "https://strategywiki.org/w/index.php?search=${enc(title)}"),
            QuickLink("YouTube", "https://m.youtube.com/results?search_query=${enc("$full walkthrough")}"),
        )
    }

    fun defaultUrlFor(game: Game): String = quickLinksFor(game).first().url

    fun searchUrl(query: String): String = "https://duckduckgo.com/?q=${enc(query.trim())}"

    /**
     * Resolves user input from the in-game browser address bar:
     * - Valid `http://` / `https://` URLs pass through directly.
     * - Bare domains like `strategywiki.org` or `gamefaqs.com/snes` become `https://...`.
     * - Natural-language level questions like `water temple key` become a search query.
     */
    fun resolveInput(raw: String, contextGameTitle: String = ""): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return if (contextGameTitle.isNotBlank()) searchUrl("$contextGameTitle walkthrough")
            else "https://duckduckgo.com/"
        }
        val lower = trimmed.lowercase()
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return trimmed
        }
        val looksLikeDomain = !trimmed.contains(' ') &&
            trimmed.contains('.') &&
            !trimmed.startsWith('.') &&
            !trimmed.endsWith('.') &&
            trimmed.substringAfterLast('.').substringBefore('/').length >= 2
        if (looksLikeDomain) {
            return "https://$trimmed"
        }
        return searchUrl(trimmed)
    }

    /** Strictly permits only safe HTTP(S) navigation inside the in-game WebView. */
    fun isAllowedScheme(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.trim().lowercase()
        return lower.startsWith("https://") || lower.startsWith("http://")
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

/**
 * Full-featured In-Game Walkthrough Browser overlay running directly inside EmulatorActivity.
 * Players can look up walkthroughs, maps, boss guides, or YouTube clips without ever leaving the app
 * or risking Android killing the emulator process.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InGameBrowserOverlay(
    game: Game,
    initialUrl: String,
    bookmarkedUrl: String?,
    onUrlChanged: (String) -> Unit,
    onBookmarkUrl: (String?) -> Unit,
    onWebViewReady: (WebView?) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val quickLinks = remember(game.id) { InGameBrowserUrls.quickLinksFor(game) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var addressText by remember { mutableStateOf(initialUrl) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("${game.title} Guide") }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var peekMode by remember { mutableStateOf(false) }

    val isBookmarked = !bookmarkedUrl.isNullOrBlank() && bookmarkedUrl == currentUrl

    fun navigateTo(raw: String) {
        val target = InGameBrowserUrls.resolveInput(raw, game.title)
        if (!InGameBrowserUrls.isAllowedScheme(target)) return
        addressText = target
        currentUrl = target
        onUrlChanged(target)
        webViewRef?.loadUrl(target)
        focusManager.clearFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            onWebViewReady(null)
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (peekMode) 0.25f else 0.62f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = if (peekMode) 0.dp else 0.dp,
                bottomEnd = if (peekMode) 0.dp else 0.dp,
            ),
            color = cs.surfaceContainer,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (peekMode) 0.56f else 0.94f)
                .clickable(interactionSource = null, indication = null) {},
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                // Top Browser Toolbar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            "Back",
                            tint = if (canGoBack) cs.onSurface else cs.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            "Forward",
                            tint = if (canGoForward) cs.onSurface else cs.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Pill Address / Search Input
                    Row(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(cs.surfaceContainerHighest)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        BasicTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = cs.onSurface, fontSize = 13.sp),
                            cursorBrush = SolidColor(cs.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { navigateTo(addressText) }),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, "Reload", tint = cs.onSurface, modifier = Modifier.size(19.dp))
                    }
                    IconButton(
                        onClick = {
                            if (isBookmarked) onBookmarkUrl(null) else onBookmarkUrl(currentUrl)
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            if (isBookmarked) "Remove pinned guide" else "Pin guide for this game",
                            tint = if (isBookmarked) cs.primary else cs.onSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    IconButton(
                        onClick = { peekMode = !peekMode },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            if (peekMode) Icons.Rounded.OpenInFull else Icons.Rounded.UnfoldLess,
                            if (peekMode) "Expand browser" else "Peek game above",
                            tint = cs.onSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Rounded.Close, "Return to game", tint = cs.onSurface, modifier = Modifier.size(20.dp))
                    }
                }

                // Current page title caption
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp),
                )

                // 1-Tap Level Help Shortcut Chips
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!bookmarkedUrl.isNullOrBlank()) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(cs.tertiaryContainer)
                                .clickable { navigateTo(bookmarkedUrl) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.Bookmark, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(13.dp))
                            Text(
                                "Pinned",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = cs.onTertiaryContainer,
                            )
                        }
                    }
                    quickLinks.forEach { link ->
                        val active = currentUrl == link.url
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(if (active) cs.primaryContainer else cs.surfaceContainerHigh)
                                .clickable { navigateTo(link.url) }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = link.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Live Page Load Progress Bar
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                    )
                } else {
                    Spacer(Modifier.height(2.5.dp))
                }

                // Embedded Android WebView
                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.setSupportZoom(true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString()
                                    return !InGameBrowserUrls.isAllowedScheme(url)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    if (!url.isNullOrBlank()) {
                                        currentUrl = url
                                        addressText = url
                                        onUrlChanged(url)
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!url.isNullOrBlank()) {
                                        currentUrl = url
                                        addressText = url
                                        onUrlChanged(url)
                                    }
                                    view?.title?.takeIf { it.isNotBlank() }?.let { pageTitle = it }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) pageTitle = title
                                }
                            }

                            webViewRef = this
                            onWebViewReady(this)
                            loadUrl(
                                if (InGameBrowserUrls.isAllowedScheme(initialUrl)) initialUrl
                                else InGameBrowserUrls.defaultUrlFor(game),
                            )
                        }
                    },
                )
            }
        }
    }
}
