package com.mocharealm.accompanist.lyrics.core.utils

class LyricsFormatGuesser {
    data class LyricsFormat(
        val name: String,
        val detector: (String) -> Boolean
    )

    private val registeredFormats = mutableListOf<LyricsFormat>()

    init {
        registerFormat(
            LyricsFormat("TTML") {
                // 注意：JS 端的 . 不匹配换行符，如果你需要匹配跨行标签，RegexOption.MULTILINE 是对的
                it.contains("""<tt.*xmlns.*=.*http://www.w3.org/ns/ttml.*>""".toRegex(RegexOption.MULTILINE))
            })

        // WARNING: DO NOT CHANGE THE LRC AND ENHANCED_LRC ORDER
        registerFormat(
            LyricsFormat("LRC") {
                it.contains("""\[\d{2}:\d{2}\.\d{2,3}\].+""".toRegex())
            })

        registerFormat(
            LyricsFormat("ENHANCED_LRC") {
                // 修复：将 ] 替换为 \]
                val hasVoiceTag = it.contains("""\]v[12]:""".toRegex())

                // 检查是否同时有行级和内联时间戳
                val hasLineTimestamp = it.contains("""\[\d{2}:\d{2}\.\d{2,3}\]""".toRegex())
                val hasInlineTimestamp = it.contains("""<\d{2}:\d{2}\.\d{2,3}>""".toRegex())
                val hasBothTimestamps = hasLineTimestamp && hasInlineTimestamp

                hasVoiceTag || hasBothTimestamps
            })

        registerFormat(
            LyricsFormat("LYRICIFY_SYLLABLE") {
                // 这里原本的 [a-zA-Z] 是字符集，不需要转义，但如果未来要匹配字面量 [，请务必转义
                it.contains("""[a-zA-Z]+\s*\(\d+,\d+\)""".toRegex())
            })

        registerFormat(
            LyricsFormat("KUGOU_KRC") {
                val lines = it.lines().map { it.trim() }.filter { it.isNotEmpty() }
                // 修复：确保所有字面量方括号都被转义
                val lineTimeRegex = """^\[\d+,\d+\]""".toRegex()
                val wordTimeRegex = """<\d+,\d+,\d+>.{1}""".toRegex()

                lines.any { line ->
                    lineTimeRegex.containsMatchIn(line) && wordTimeRegex.containsMatchIn(line)
                }
            })
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