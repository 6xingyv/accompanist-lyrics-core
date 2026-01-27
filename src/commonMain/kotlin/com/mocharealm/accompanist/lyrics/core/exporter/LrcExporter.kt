package com.mocharealm.accompanist.lyrics.core.exporter

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.utils.toTimeFormattedString

/**
 * Exporter for the standard LRC format.
 *
 * It converts [SyncedLyrics] back into a string representation in the LRC format.
 * - Supports ID3 tags (`[ti:...]`, `[ar:...]`).
 * - Supports standard line timestamps `[mm:ss.xx]`.
 * - Supports translations as separate lines with the same timestamp.
 */
object LrcExporter : ILyricsExporter {
    /**
     * Exports the given [SyncedLyrics] to a LRC formatted string.
     *
     * @param lyrics The [SyncedLyrics] object to export.
     * @return A string containing the LRC formatted lyrics.
     */
    override fun export(lyrics: SyncedLyrics): String {
        if (lyrics.lines.isEmpty()) return ""

        val builder = StringBuilder()
        var lastEndTime = 0

        if (lyrics.title.isNotBlank()) {
            builder.appendLine("[ti:${lyrics.title}]")
        }
        if (lyrics.artists != null && lyrics.artists.isNotEmpty() && lyrics.artists.all { it.name.isNotBlank() }) {
            builder.appendLine(
                "[ar:${lyrics.artists.joinToString("/") { it.type + ":" + it.name }}]"
            )
        }

        lyrics.lines.forEach { line ->
            if (line.start - lastEndTime > 0) {
                builder.appendLine(lastEndTime.toTimeFormattedString())
            }

            val timeTag = line.start.toTimeFormattedString()

            when (line) {
                is SyncedLine -> {
                    builder.appendLine("$timeTag${line.content}")
                    line.translation?.let { builder.appendLine("$timeTag$it") }
                }

                is KaraokeLine -> {
                    val content = line.syllables.joinToString("") { it.content }.trim()
                    builder.appendLine("$timeTag$content")
                    line.translation?.let { builder.appendLine("$timeTag$it") }
                }
            }

            lastEndTime = line.end
        }

        return builder.toString()
    }
}