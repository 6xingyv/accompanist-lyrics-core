package com.mocharealm.accompanist.lyrics.core.utils

class LyricsFormatGuesser {
    data class LyricsFormat(
        val name: String,
        val detector: (String) -> Boolean
    )

    private val registeredFormats = mutableListOf<LyricsFormat>()

    init {
        registerFormat(
            LyricsFormat(
                "TTML"
            ) {
                it.contains("<tt.*xmlns.*=.*http://www.w3.org/ns/ttml.*>".toRegex(RegexOption.MULTILINE))
            })

        // WARNING: DO NOT CHANGE THE LRC AND ENHANCED_LRC ORDER
        registerFormat(
            LyricsFormat(
                "LRC"
            ) { it.contains("\\[\\d{2}:\\d{2}\\.\\d{2,3}].+".toRegex()) })
        registerFormat(
            _root_ide_package_.com.mocharealm.accompanist.lyrics.core.utils.LyricsFormatGuesser.LyricsFormat(
                "ENHANCED_LRC"
            ) {
                val hasVoiceTag = it.contains("\\]v[12]:".toRegex())

                // 检查是否同时有行级和内联时间戳
                val hasLineTimestamp = it.contains("\\[\\d{2}:\\d{2}\\.\\d{2,3}]".toRegex())
                val hasInlineTimestamp = it.contains("<\\d{2}:\\d{2}\\.\\d{2,3}>".toRegex())
                val hasBothTimestamps = hasLineTimestamp && hasInlineTimestamp

                // 满足任一条件即认为是Enhanced LRC
                hasVoiceTag || hasBothTimestamps
            })

        registerFormat(
            LyricsFormat(
                "LYRICIFY_SYLLABLE"
            ) { it.contains("[a-zA-Z]+\\s*\\(\\d+,\\d+\\)".toRegex()) })
    }

    /**
     * Registers a new lyrics format detector.
     *
     * @param format The [LyricsFormat] to add.
     */
    fun registerFormat(format: LyricsFormat) {
        registeredFormats.add(0, format) // Add to the front to prioritize custom formats
    }

    /**
     * Guesses the lyrics format from a list of lines.
     *
     * @param lines The lyrics lines to analyze.
     * @return The guessed [LyricsFormat], or null if no format matches.
     */
    fun guessFormat(lines: List<String>): LyricsFormat? {
        return guessFormat(lines.joinToString("\n"))
    }

    /**
     * Guesses the lyrics format from a single string.
     *
     * @param content The lyrics content to analyze.
     * @return The guessed [LyricsFormat], or null if no format matches.
     */
    fun guessFormat(content: String): LyricsFormat? {
        return registeredFormats.firstOrNull { it.detector(content) }
    }
}