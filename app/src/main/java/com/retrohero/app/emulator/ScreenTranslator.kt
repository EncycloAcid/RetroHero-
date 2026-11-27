package com.retrohero.app.emulator

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.retrohero.app.ui.components.Ascend
import com.retrohero.app.ui.theme.Tone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Supported source languages for offline on-device translation into English. */
enum class TranslateSourceLang(val code: String, val label: String, val mlKitCode: String) {
    JAPANESE("ja", "JP", TranslateLanguage.JAPANESE),
    SPANISH("es", "ES", TranslateLanguage.SPANISH),
    FRENCH("fr", "FR", TranslateLanguage.FRENCH),
    GERMAN("de", "DE", TranslateLanguage.GERMAN),
    KOREAN("ko", "KO", TranslateLanguage.KOREAN),
    CHINESE("zh", "ZH", TranslateLanguage.CHINESE);

    companion object {
        fun fromCode(code: String?): TranslateSourceLang =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: JAPANESE
    }
}

/** Bounding box normalized into [0..1] × [0..1] relative to the captured game frame. */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    fun union(other: NormalizedRect): NormalizedRect = NormalizedRect(
        left = minOf(left, other.left),
        top = minOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom),
    )

    companion object {
        fun fromPixelBounds(left: Int, top: Int, right: Int, bottom: Int, imgW: Int, imgH: Int): NormalizedRect {
            val w = imgW.coerceAtLeast(1).toFloat()
            val h = imgH.coerceAtLeast(1).toFloat()
            return NormalizedRect(
                left = (left / w).coerceIn(0f, 1f),
                top = (top / h).coerceIn(0f, 1f),
                right = (right / w).coerceIn(0f, 1f),
                bottom = (bottom / h).coerceIn(0f, 1f),
            )
        }
    }
}

data class RecognizedBlock(val text: String, val bounds: NormalizedRect)

data class TranslatedBlock(
    val original: String,
    val translated: String,
    val bounds: NormalizedRect,
)

sealed interface TranslateState {
    data object Idle : TranslateState
    data class Working(val status: String) : TranslateState
    data class Ready(val blocks: List<TranslatedBlock>) : TranslateState
    data class Error(val message: String) : TranslateState
}

/**
 * Pure post-processing: merges closely-stacked OCR blocks (common in retro RPG dialogue boxes)
 * so the translator sees complete sentences instead of fragmented lines.
 */
fun mergeDialogueBlocks(blocks: List<RecognizedBlock>, maxVerticalGap: Float = 0.045f): List<RecognizedBlock> {
    if (blocks.size <= 1) return blocks
    val sorted = blocks.sortedWith(compareBy<RecognizedBlock> { it.bounds.top }.thenBy { it.bounds.left })
    val merged = mutableListOf<RecognizedBlock>()
    for (block in sorted) {
        val prev = merged.lastOrNull()
        val horizontallyClose = prev != null &&
            minOf(prev.bounds.right, block.bounds.right) - maxOf(prev.bounds.left, block.bounds.left) > -0.15f
        val verticallyClose = prev != null && (block.bounds.top - prev.bounds.bottom) <= maxVerticalGap
        if (prev != null && horizontallyClose && verticallyClose) {
            merged[merged.lastIndex] = RecognizedBlock(
                text = "${prev.text} ${block.text}".trim(),
                bounds = prev.bounds.union(block.bounds),
            )
        } else {
            merged += block
        }
    }
    return merged
}

/**
 * Ultra-fast (~0.1 ms) luminance grid signature for Live Overlay Translation.
 * Only triggers on-device OCR when the game's text area actually changes.
 */
fun frameDialogueSignature(rawFrame: IntArray?): Long {
    if (rawFrame == null || rawFrame.size < 3) return 0L
    val w = rawFrame[0]
    val h = rawFrame[1]
    if (w <= 0 || h <= 0 || rawFrame.size < 2 + w * h) return 0L
    var hash = -3750763034362895579L // FNV-1a 64-bit offset basis
    val cols = 32
    val rows = 16
    for (ry in 0 until rows) {
        val y = (ry * h) / rows
        val rowBase = 2 + y * w
        for (rx in 0 until cols) {
            val x = (rx * w) / cols
            val px = rawFrame[rowBase + x]
            val r = (px ushr 16) and 0xFF
            val g = (px ushr 8) and 0xFF
            val b = px and 0xFF
            // Quantize luma to 4 bits so tiny scanline/dither flicker never re-triggers OCR
            val lumaQ = ((r * 77 + g * 150 + b * 29) ushr 12) and 0x0F
            hash = (hash xor lumaQ.toLong()) * 1099511628211L
        }
    }
    return hash
}

/** Runs on-device ML Kit OCR + offline neural translation on raw libretro ARGB frames. */
object ScreenTranslator {
    suspend fun translateRawFrame(
        rawFrame: IntArray?,
        sourceLang: TranslateSourceLang,
        onStatus: (String) -> Unit,
    ): TranslateState = withContext(Dispatchers.Default) {
        if (rawFrame == null || rawFrame.size < 3) {
            return@withContext TranslateState.Error("No game frame available yet.")
        }
        val w = rawFrame[0]
        val h = rawFrame[1]
        if (w <= 0 || h <= 0 || rawFrame.size < 2 + w * h) {
            return@withContext TranslateState.Error("Invalid game frame.")
        }
        onStatus("Reading on-screen text…")
        val recognized = try {
            recognizeBlocks(rawFrame)
        } catch (t: Throwable) {
            return@withContext TranslateState.Error("OCR failed: ${t.message ?: "unknown error"}")
        }

        val merged = mergeDialogueBlocks(recognized)
        if (merged.isEmpty()) {
            return@withContext TranslateState.Ready(emptyList())
        }

        onStatus("Preparing offline ${sourceLang.label} → EN model…")
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang.mlKitCode)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        val translator = Translation.getClient(options)
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            onStatus("Translating ${merged.size} ${if (merged.size == 1) "block" else "blocks"}…")
            val out = merged.map { block ->
                val tr = translator.translate(block.text).awaitTask()
                TranslatedBlock(original = block.text, translated = tr, bounds = block.bounds)
            }
            TranslateState.Ready(out)
        } catch (t: Throwable) {
            TranslateState.Error("Translation failed: ${t.message ?: "check connection for first-time model download"}")
        } finally {
            translator.close()
        }
    }

    /**
     * OCR only: the text blocks on a raw libretro frame, with nearby lines merged into dialogue
     * boxes. Empty when the frame is invalid or has no text. Throws if ML Kit fails.
     */
    suspend fun recognizeRawFrame(rawFrame: IntArray?): List<RecognizedBlock> = withContext(Dispatchers.Default) {
        if (rawFrame == null || rawFrame.size < 3) return@withContext emptyList()
        val w = rawFrame[0]
        val h = rawFrame[1]
        if (w <= 0 || h <= 0 || rawFrame.size < 2 + w * h) return@withContext emptyList()
        mergeDialogueBlocks(recognizeBlocks(rawFrame))
    }

    private suspend fun recognizeBlocks(rawFrame: IntArray): List<RecognizedBlock> {
        val w = rawFrame[0]
        val h = rawFrame[1]
        val base = Bitmap.createBitmap(rawFrame, 2, w, w, h, Bitmap.Config.ARGB_8888)
        // Integer upscale 3× with nearest-neighbor so low-res pixel fonts are legible to ML Kit OCR.
        val scale = (900 / maxOf(w, h)).coerceIn(2, 4)
        val upscaled = Bitmap.createScaledBitmap(base, w * scale, h * scale, false)
        val imgW = upscaled.width
        val imgH = upscaled.height

        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        return try {
            val visionText = recognizer.process(InputImage.fromBitmap(upscaled, 0)).awaitTask()
            visionText.textBlocks.mapNotNull { b ->
                val cleaned = b.text.replace('\n', ' ').trim()
                val box: Rect = b.boundingBox ?: return@mapNotNull null
                if (cleaned.isEmpty()) return@mapNotNull null
                RecognizedBlock(
                    text = cleaned,
                    bounds = NormalizedRect.fromPixelBounds(box.left, box.top, box.right, box.bottom, imgW, imgH),
                )
            }
        } finally {
            recognizer.close()
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        addOnCanceledListener { if (cont.isActive) cont.cancel() }
    }
}

/**
 * Live in-place dialogue overlay rendered directly over the active game frame without pausing.
 * Each translated block is positioned over the exact normalized OCR bounding box so Japanese
 * RPG text boxes are cleanly replaced in place while touch inputs keep flowing smoothly.
 */
@Composable
fun LiveTranslateOverlay(
    blocks: List<TranslatedBlock>,
    sourceLang: TranslateSourceLang,
    scanning: Boolean,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        blocks.forEach { block ->
            val left = (w * block.bounds.left).coerceIn(4.dp, (w - 96.dp).coerceAtLeast(4.dp))
            val top = (h * block.bounds.top).coerceIn(4.dp, (h - 32.dp).coerceAtLeast(4.dp))
            val boxW = (w * block.bounds.width).coerceIn(96.dp, (w - left - 4.dp).coerceAtLeast(96.dp))
            Box(
                Modifier
                    .padding(start = left, top = top)
                    .widthIn(min = 96.dp, max = boxW)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE610131A))
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = block.translated,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (scanning && blocks.isEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC10131A))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "Live translate (${sourceLang.label} → EN) watching dialogue…",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Translucent Instrument-styled overlay showing translated on-screen dialogue over the paused game. */
@Composable
fun TranslateOverlay(
    state: TranslateState,
    sourceLang: TranslateSourceLang,
    liveActive: Boolean = false,
    onToggleLive: (Boolean) -> Unit = {},
    onSelectLang: (TranslateSourceLang) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showOriginal by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(Ascend.Radius.Card)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(Tone.Surface)
                .border(1.dp, Tone.Line, cardShape)
                .clickable(interactionSource = null, indication = null) {}
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Ascend.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Translate, null, tint = Ascend.onAccent, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Screen translate", style = MaterialTheme.typography.titleMedium, color = Tone.Ink)
                    Text("On-device OCR · ${sourceLang.label} → EN", style = MaterialTheme.typography.labelSmall, color = Tone.Muted)
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (liveActive) Ascend.accent else Tone.Raised)
                        .border(1.dp, Tone.Line, CircleShape)
                        .clickable { onToggleLive(!liveActive) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (liveActive) "LIVE ON" else "Live overlay",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liveActive) Ascend.onAccent else Tone.Ink,
                    )
                }
                IconButton(onClick = onRescan) {
                    Icon(Icons.Rounded.Refresh, "Re-scan", tint = Tone.Ink)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Resume game", tint = Tone.Ink)
                }
            }

            // Source language selector chips
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TranslateSourceLang.entries.forEach { lang ->
                    val selected = lang == sourceLang
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(Ascend.Radius.Control))
                            .background(if (selected) Tone.Ink else Tone.Raised)
                            .border(1.dp, Tone.Line, RoundedCornerShape(Ascend.Radius.Control))
                            .clickable { onSelectLang(lang) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            lang.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Tone.Surface else Tone.Ink,
                        )
                    }
                }
            }

            when (state) {
                TranslateState.Idle -> Unit
                is TranslateState.Working -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Ascend.accent)
                    Text(state.status, style = MaterialTheme.typography.bodyMedium, color = Tone.Ink)
                }
                is TranslateState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                is TranslateState.Ready -> {
                    if (state.blocks.isEmpty()) {
                        Text(
                            "No readable text was detected on this frame. Try pausing on a dialogue box or turn on Live overlay to auto-translate as you play.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Tone.Muted,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.blocks.forEach { block ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(Ascend.Radius.Control))
                                        .background(Tone.Raised)
                                        .border(1.dp, Tone.Line, RoundedCornerShape(Ascend.Radius.Control))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(block.translated, style = MaterialTheme.typography.bodyLarge, color = Tone.Ink)
                                    if (showOriginal) {
                                        Text(block.original, style = MaterialTheme.typography.bodySmall, color = Tone.Muted)
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (showOriginal) "Hide original text" else "Show original text",
                                style = MaterialTheme.typography.labelMedium,
                                color = Ascend.accent,
                                modifier = Modifier.clickable { showOriginal = !showOriginal }.padding(vertical = 4.dp),
                            )
                            Text("Tap outside to resume", style = MaterialTheme.typography.labelSmall, color = Tone.Muted)
                        }
                    }
                }
            }
        }
    }
}
