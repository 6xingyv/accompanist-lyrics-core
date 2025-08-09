package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics

interface ILyricsParser {
    /**
     * Parses a list of strings into SyncedLyrics.
     * Has a default implementation that joins the list and calls the String version of parse.
     *
     * @param lines The lines to parse.
     * @return The parsed SyncedLyrics.
     */
    fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("\n"))
    }

    /**
     * Parses a single string into SyncedLyrics.
     * Has a default implementation that splits the string by newlines and calls the List version of parse.
     *
     * @param content The string content to parse.
     * @return The parsed SyncedLyrics.
     */
    fun parse(content: String): SyncedLyrics {
        // 默认实现：将 String 按行分割成 List<String>，然后调用另一个 parse 方法。
        return parse(content.split('\n'))
    }
}