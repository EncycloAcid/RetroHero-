package com.retrohero.app.data

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.security.MessageDigest
import java.util.Locale

/**
 * NFC "cartridges": a cheap NFC sticker or card that launches a game when tapped to the phone.
 *
 * A tag holds one link, `retrohero://play/<key>?t=<title>`. The key is a short hash of the system
 * and file name rather than the library id, so a tag keeps working after a rescan or a folder
 * change, and on a friend's phone that has the same game. The title is only for messages.
 * The same links work from QR codes or a browser.
 */
object NfcCartridge {
    const val SCHEME = "retrohero"
    const val HOST = "play"
    private const val MAX_TITLE = 40

    fun keyOf(game: Game): String = keyOf(game.systemId, game.fileName)

    fun keyOf(systemId: String, fileName: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$systemId/${fileName.lowercase(Locale.ROOT)}".toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    fun uriFor(game: Game): String =
        "$SCHEME://$HOST/${keyOf(game)}?t=${Uri.encode(game.title.take(MAX_TITLE))}"

    data class Link(val key: String, val title: String?)

    /** Parses a tag or QR link; null if it isn't a RetroHero play link. */
    fun parse(uri: String?): Link? {
        if (uri == null) return null
        val u = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
        if (!u.scheme.equals(SCHEME, ignoreCase = true) || !u.host.equals(HOST, ignoreCase = true)) return null
        val key = u.pathSegments.firstOrNull()?.lowercase(Locale.ROOT)?.takeIf { it.matches(Regex("[0-9a-f]{6,40}")) } ?: return null
        return Link(key, u.getQueryParameter("t")?.takeIf { it.isNotBlank() })
    }

    fun find(games: List<Game>, link: Link): Game? = games.firstOrNull { keyOf(it) == link.key }

    fun messageFor(game: Game): NdefMessage = NdefMessage(arrayOf(NdefRecord.createUri(uriFor(game))))

    /** Writes [message] to [tag], formatting a blank tag if needed. Blocking: call off the main thread. */
    fun write(tag: Tag, message: NdefMessage): Result<Unit> = runCatching {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                check(ndef.isWritable) { "This tag is locked and can't be written." }
                check(ndef.maxSize >= message.byteArrayLength) {
                    "This tag only holds ${ndef.maxSize} bytes. Use an NTAG213, 215 or 216 sticker."
                }
                ndef.writeNdefMessage(message)
            } finally {
                runCatching { ndef.close() }
            }
        } else {
            val formatable = NdefFormatable.get(tag)
                ?: error("This kind of tag isn't supported. NTAG213, 215 and 216 stickers work best.")
            formatable.connect()
            try {
                formatable.format(message)
            } finally {
                runCatching { formatable.close() }
            }
        }
    }
}
