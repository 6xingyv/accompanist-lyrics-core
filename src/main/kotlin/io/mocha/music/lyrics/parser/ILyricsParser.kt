package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics

interface ILyricsParser {
    fun parse(lines:List<String>): SyncedLyrics
}