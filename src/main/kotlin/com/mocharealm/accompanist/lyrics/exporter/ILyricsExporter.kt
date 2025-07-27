package com.mocharealm.accompanist.lyrics.exporter

import com.mocharealm.accompanist.lyrics.model.SyncedLyrics

interface ILyricsExporter {
    fun export(lyrics: SyncedLyrics): String
}