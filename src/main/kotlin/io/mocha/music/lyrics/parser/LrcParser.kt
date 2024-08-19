package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.synced.SyncedLine

object LrcParser: ILyricsParser {
    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = AttributesHelper.removeAttributes(lines)
        val data = lyricsLines.map { line->
            parseLine(line)
        }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(line: String): SyncedLine {
        TODO()
    }
}