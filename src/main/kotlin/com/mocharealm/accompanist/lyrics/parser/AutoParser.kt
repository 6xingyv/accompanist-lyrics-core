package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.utils.LyricsFormatGuesser

/**
 * A smart parser that automatically detects the lyrics format and uses the appropriate parser.
 *
 * This class combines the functionality of all individual parsers (`LrcParser`, `EnhancedLrcParser`,
 * `TTMLParser`, and `LyricifySyllableParser`) into a single, easy-to-use interface. It uses
 * `LyricsFormatGuesser` to determine the most likely format and then delegates the parsing task.
 *
 * This parser is extensible. You can register custom formats and their corresponding parsers.
 */
class AutoParser(private val guesser: LyricsFormatGuesser) : ILyricsParser {
    private val parsers = mutableMapOf<String, ILyricsParser>()

    init {
        registerParser("LRC", LrcParser)
        registerParser("ENHANCED_LRC", EnhancedLrcParser)
        registerParser("TTML", TTMLParser)
        registerParser("LYRICIFY_SYLLABLE", LyricifySyllableParser)
    }

    /**
     * Registers a custom parser for a specific lyrics format.
     *
     * @param formatName The name of the format (must match the name in the registered LyricsFormat).
     * @param parser The [ILyricsParser] instance for this format.
     */
    fun registerParser(formatName: String, parser: ILyricsParser) {
        parsers[formatName] = parser
    }

    /**
     * Registers a custom lyrics format and its parser.
     *
     * @param format The [LyricsFormatGuesser.LyricsFormat] to register.
     * @param parser The [ILyricsParser] for this format.
     */
    fun register(format: LyricsFormatGuesser.LyricsFormat, parser: ILyricsParser) {
        guesser.registerFormat(format)
        registerParser(format.name, parser)
    }


    /**
     * Parses lyrics from a list of strings by first guessing the format.
     *
     * @param lines The lyrics lines to parse.
     * @return A [com.mocharealm.accompanist.lyrics.model.SyncedLyrics] object, or an empty one if the format is unknown or parsing fails.
     */
    override fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("\n"))
    }

    /**
     * Parses lyrics from a single string by first guessing the format.
     *
     * @param content The lyrics content to parse.
     * @return A [SyncedLyrics] object, or an empty one if the format is unknown or parsing fails.
     */
    override fun parse(content: String): SyncedLyrics {
        val format = guesser.guessFormat(content)
        val parser = format?.name?.let { parsers[it] }

        return parser?.parse(content.split("\n"))
            ?: SyncedLyrics(lines = emptyList())
    }

    class Builder {
        private val guesser = LyricsFormatGuesser()
        private val customParsers = mutableMapOf<String, ILyricsParser>()

        fun withFormat(format: LyricsFormatGuesser.LyricsFormat, parser: ILyricsParser): Builder {
            guesser.registerFormat(format)
            customParsers[format.name] = parser
            return this
        }

        fun build(): AutoParser {
            val autoParser = AutoParser(guesser)
            customParsers.forEach { (name, parser) ->
                autoParser.registerParser(name, parser)
            }
            return autoParser
        }
    }
}