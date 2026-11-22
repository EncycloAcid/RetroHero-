package com.retrohero.app.emulator

import java.io.File

/** Persists per-core option overrides as simple key=value lines. */
class CoreOptionsStore(private val file: File) {
    fun load(): Map<String, String> = if (!file.exists()) emptyMap() else
        file.readLines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()

    fun save(key: String, value: String) {
        val all = load().toMutableMap()
        all[key] = value
        file.writeText(all.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }
}
