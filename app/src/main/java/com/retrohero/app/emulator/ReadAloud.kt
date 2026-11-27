package com.retrohero.app.emulator

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Decides which on-screen text is new enough to read aloud. Pure logic, so it is unit-tested.
 *
 *  - Text that was already on screen in the previous scan is HUD or a menu label: skipped.
 *  - Text with fewer than two letters (scores, timers, "x03") is skipped.
 *  - When a dialogue box grows ("Hello" → "Hello, traveler"), only the new part is read.
 *  - Recently read lines aren't repeated when the same box comes back after a menu.
 */
class SpokenTextFilter(private val memory: Int = 24) {
    private var previousScan: Set<String> = emptySet()
    private val recent = ArrayDeque<String>()

    fun newText(blocks: List<String>): List<String> {
        val scan = blocks.map(::normalize).filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        for (text in scan) {
            if (text in previousScan || text in recent) continue
            if (text.count { it.isLetter() } < 2) continue
            val grownFrom = recent.filter { text.startsWith(it) && text.length > it.length }.maxByOrNull { it.length }
            val speak = grownFrom?.let { text.substring(it.length).trimStart(' ', ',', '.', '…') } ?: text
            if (speak.count { it.isLetter() } >= 2) out += speak
            remember(text)
        }
        previousScan = scan.toSet()
        return out
    }

    fun reset() {
        previousScan = emptySet()
        recent.clear()
    }

    private fun remember(text: String) {
        recent.remove(text)
        recent.addLast(text)
        while (recent.size > memory) recent.removeFirst()
    }

    companion object {
        fun normalize(text: String): String = text.replace(Regex("\\s+"), " ").trim()

        /** Best-guess speech language for [text] from its script; null means the device default. */
        fun localeFor(text: String): Locale? = when {
            text.any { it in '぀'..'ヿ' } -> Locale.JAPANESE
            text.any { it in '가'..'힯' || it in 'ᄀ'..'ᇿ' } -> Locale.KOREAN
            text.any { it in '一'..'鿿' } -> Locale.CHINESE
            else -> null
        }
    }
}

/**
 * Reads game text aloud with the system text-to-speech engine. While [start]ed it watches the
 * frame and, once the picture has held still for a moment (dialogue finished typing), runs OCR
 * and speaks whatever is new. Live translation can feed it English text instead via [speakNew].
 */
class GameNarrator(context: Context, private val scope: CoroutineScope) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private var job: Job? = null
    private val filter = SpokenTextFilter()
    private var lastLocale: Locale? = null
    private val pending = mutableListOf<String>()

    /** False when the device has no working text-to-speech engine. */
    var available: Boolean = true
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            available = ready
            if (ready) {
                // Speech rate and voice come from the system's text-to-speech settings.
                synchronized(pending) {
                    pending.forEach { speakNow(it, flush = false) }
                    pending.clear()
                }
            }
        }
    }

    val running: Boolean get() = job?.isActive == true

    /**
     * Starts watching the screen. [grabFrame] returns a raw frame (width, height, ARGB pixels…),
     * [paused] says when not to read (menus open, game paused, translation feeding text instead).
     */
    fun start(grabFrame: () -> IntArray?, paused: () -> Boolean) {
        job?.cancel()
        filter.reset()
        job = scope.launch(Dispatchers.Default) {
            var lastSig = 0L
            var stableTicks = 0
            var lastReadSig = 0L
            while (isActive) {
                delay(POLL_MS)
                if (paused()) continue
                val raw = grabFrame() ?: continue
                val sig = frameDialogueSignature(raw)
                if (sig == 0L) continue
                if (sig != lastSig) {
                    lastSig = sig
                    stableTicks = 0
                    continue
                }
                stableTicks++
                if (stableTicks != STABLE_TICKS || sig == lastReadSig) continue
                lastReadSig = sig
                val blocks = runCatching { ScreenTranslator.recognizeRawFrame(raw) }.getOrNull() ?: continue
                speakNew(blocks.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left })).map { it.text })
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        tts?.stop()
    }

    /** Speaks the parts of [texts] that haven't been read yet. Safe from any thread. */
    fun speakNew(texts: List<String>) {
        val fresh = synchronized(filter) { filter.newText(texts) }
        fresh.forEachIndexed { i, t -> say(t, flush = i == 0) }
    }

    /** Speaks [text] right away, interrupting whatever is being read. */
    fun say(text: String, flush: Boolean = true) {
        if (!ready) {
            synchronized(pending) { pending += text }
            return
        }
        speakNow(text, flush)
    }

    private fun speakNow(text: String, flush: Boolean) {
        val engine = tts ?: return
        val locale = SpokenTextFilter.localeFor(text) ?: Locale.getDefault()
        if (locale != lastLocale) {
            val res = engine.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) engine.setLanguage(Locale.getDefault())
            lastLocale = locale
        }
        engine.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "rh-${text.hashCode()}")
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private companion object {
        const val POLL_MS = 350L
        /** Frames must match this many polls in a row (~0.7 s) before reading: text has finished typing. */
        const val STABLE_TICKS = 2
    }
}
