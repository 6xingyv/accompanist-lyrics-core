package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import com.mocharealm.accompanist.lyrics.core.utils.LrcMetadataHelper
import com.mocharealm.accompanist.lyrics.core.utils.parseAsTime

/**
 * A parser for LRC (LyRiCs) files.
 *
 * It uses a regular expression to match the LRC line format: `[mm:ss.xx]Lyric content` or `[mm:ss.xxx]Lyric content`.
 */
object LrcParser : ILyricsParser {
    private val timestampRegex = Regex("\\[(?:(\\d{1,2}):)?(\\d{1,2}:\\d{1,2}\\.\\d{2,3})]")

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = lyricsLines
            .flatMap { line -> parseLine(line) }
            .combineRawWithTranslation()
            .rearrangeTime()
            .map { it.toSyncedLine() }
            .filter { it.content.isNotBlank() }
            .sortedBy { it.start }
        val attributes = LrcMetadataHelper.parse(lines)
        return SyncedLyrics(
            lines = data,
            title = attributes.title ?: "",
            artists = attributes.artist?.split("/")?.map { com.mocharealm.accompanist.lyrics.core.model.Artist("Main", it) } ?: emptyList()
        )
    }

    private fun parseLine(content: String): List<UncheckedSyncedLine> {
        val matches = timestampRegex.findAll(content).toList()
        if (matches.isEmpty()) return emptyList()

        val lastMatch = matches.last()
        val lyric = content.substring(lastMatch.range.last + 1).trim()

        return matches.map { matchResult ->
            val hour = matchResult.groupValues[1]
            val time = matchResult.groupValues[2]
            val fullTime = if (hour.isNotEmpty()) "$hour:$time" else time
            UncheckedSyncedLine(
                start = fullTime.parseAsTime(),
                end = 0,
                content = lyric,
                translation = null
            )
        }
    }

    private fun List<UncheckedSyncedLine>.combineRawWithTranslation(): List<UncheckedSyncedLine> {
        val list = mutableListOf<UncheckedSyncedLine>()
        var i = 0
        while (i < this.size) {
            val line = this[i]
            val nextLine = this.getOrNull(i + 1)
            if (nextLine != null && line.start == nextLine.start) {
                list.add(line.copy(translation = nextLine.content))
                i += 2  // 跳过下一行，因为它是翻译
            } else {
                list.add(line)
                i++
            }
        }
        return list
    }

    private fun List<UncheckedSyncedLine>.rearrangeTime(): List<UncheckedSyncedLine> =
        this.mapIndexed { index, line ->
            val end = this.getOrNull(index + 1)?.start ?: Int.MAX_VALUE
            line.copy(end = end)
        }

}



