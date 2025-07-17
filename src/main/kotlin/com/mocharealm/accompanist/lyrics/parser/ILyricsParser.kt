package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.SyncedLyrics

interface ILyricsParser {
    fun parse(lines:List<String>): SyncedLyrics
}