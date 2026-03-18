package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics

/**
 * A smart parser that automatically detects the lyrics format and uses the appropriate parser.
 *
 * This class combines the functionality of all individual parsers (`LrcParser`, `EnhancedLrcParser`,
 * `TTMLParser`, and `LyricifySyllableParser`) into a single, easy-to-use interface. It uses
 * `LyricsFormatGuesser` to determine the most likely format and then delegates the parsing task.
 *
 * This parser is extensible. You can register custom formats and their corresponding parsers.
 */
class AutoParser(
    private val parsers: List<ILyricsParser> = listOf(
        TTMLParser,
        LyricifySyllableParser,
        EnhancedLrcParser,
        LrcParser,
        KugouKrcParser,
    )
) : ILyricsParser {

    override fun canParse(content: String): Boolean =
        parsers.any { it.canParse(content) }

    override fun parse(content: String): SyncedLyrics {
        val parser = parsers.firstOrNull { it.canParse(content) }
        return parser?.parse(content) ?: SyncedLyrics(emptyList())
    }
}