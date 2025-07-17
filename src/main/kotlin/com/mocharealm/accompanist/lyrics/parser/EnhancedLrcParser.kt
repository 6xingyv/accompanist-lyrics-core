package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.utils.parseAsTime

object EnhancedLrcParser : ILyricsParser {
    private val lineRegex = Regex("^\\[(.*?)](\\s*(.*))?$")
    private val timeRegex = Regex("^\\d{1,2}:\\d{1,2}\\.\\d{1,3}$")
    private val voiceParser = Regex("^(v\\d+)\\s*:\\s*(.*)")

    override fun parse(lines: List<String>): SyncedLyrics {
        // FIX: Removed the call to AttributesHelper.removeAttributes(lines)
        // This was the root cause, as it was incorrectly filtering out [bg:...] lines.
        val lyricsLines = AttributesHelper.removeAttributes(lines)
        val data = lyricsLines
            .mapNotNull { line -> parseLine(line) }
            .combineRawWithTranslation()
            .rearrangeAccompanimentAlignment()
        return SyncedLyrics(
            lines = data
        )
    }

    private fun parseLine(string: String): KaraokeLine? {
        /**
         * A robust, procedural syllable parser that walks through the string
         * instead of using unreliable complex regex matching.
         */
        fun proceduralParseSyllables(content: String): List<KaraokeSyllable> {
            val syllables = mutableListOf<KaraokeSyllable>()
            var currentIndex = 0
            while (currentIndex < content.length) {
                val openTagStart = content.indexOf('<', currentIndex)
                if (openTagStart == -1) break

                val openTagEnd = content.indexOf('>', openTagStart)
                if (openTagEnd == -1) break

                val timestamp = content.substring(openTagStart + 1, openTagEnd)

                if (!timeRegex.matches(timestamp)) {
                    currentIndex = openTagEnd + 1
                    continue
                }

                val nextOpenTagStart = content.indexOf('<', openTagEnd)
                val textEnd = if (nextOpenTagStart == -1) content.length else nextOpenTagStart
                val text = content.substring(openTagEnd + 1, textEnd)

                syllables.add(KaraokeSyllable(text, timestamp.parseAsTime(), timestamp.parseAsTime()))
                currentIndex = textEnd
            }
            return syllables.rearrangeTime()
        }

        val lineMatch = lineRegex.find(string) ?: return null

        val tagContent = lineMatch.groupValues[1]
        val mainContent = lineMatch.groupValues[3]

        // Case 1: Background line [bg:...]
        if (tagContent.startsWith("bg:")) {
            val bgContent = tagContent.substringAfter("bg:").trim()
            val syllables = proceduralParseSyllables(bgContent)

            return if (syllables.isNotEmpty()) {
                KaraokeLine(
                    syllables = syllables,
                    translation = null,
                    isAccompaniment = true,
                    alignment = KaraokeAlignment.Unspecified,
                    start = syllables.first().start,
                    end = syllables.last().end
                )
            } else null
        }
        // Case 2: Standard/Enhanced line [time]...
        else if (timeRegex.matches(tagContent)) {
            val lineStartTime = tagContent.parseAsTime()
            val content = mainContent.trim()
            val syllables = proceduralParseSyllables(content)

            val (alignment, textContent) = voiceParser.find(content)?.let {
                val align = when (it.groupValues[1]) {
                    "v1" -> KaraokeAlignment.Start
                    "v2" -> KaraokeAlignment.End
                    else -> KaraokeAlignment.Unspecified
                }
                align to it.groupValues[2].trim()
            } ?: (KaraokeAlignment.Unspecified to content)

            return if (syllables.isNotEmpty()) {
                KaraokeLine(
                    syllables = syllables,
                    translation = null,
                    isAccompaniment = false,
                    alignment = alignment,
                    start = syllables.first().start,
                    end = syllables.last().end
                )
            } else {
                KaraokeLine(
                    syllables = listOf(KaraokeSyllable(textContent, lineStartTime, lineStartTime)),
                    translation = null,
                    isAccompaniment = false,
                    alignment = alignment,
                    start = lineStartTime,
                    end = lineStartTime
                )
            }
        }
        return null
    }

    private fun List<KaraokeSyllable>.rearrangeTime(): List<KaraokeSyllable> {
        val list = mutableListOf<KaraokeSyllable>()
        for (i in indices) {
            val syllable = this[i]
            val nextSyllable = this.getOrNull(i + 1)
            if (nextSyllable != null) {
                list.add(syllable.copy(end = nextSyllable.start))
            } else {
                // For the last syllable, we can estimate its end time or leave it as is.
                // Here, we'll just add it. A better approach might be to add a small duration.
                list.add(syllable)
            }
        }
        return list.filter { it.content.isNotBlank() }
    }

    private fun List<KaraokeLine>.combineRawWithTranslation(): List<KaraokeLine> {
        val list = mutableListOf<KaraokeLine>()
        var i = 0
        while (i < this.size) {
            val line = this[i]
            val nextLine = this.getOrNull(i + 1)
            // A translation line must have the same start time and must NOT be an accompaniment line.
            if (nextLine != null && !nextLine.isAccompaniment && line.start == nextLine.start) {
                val translationText = nextLine.syllables.joinToString("") { it.content }.trim()
                list.add(line.copy(translation = translationText))
                i += 2
            } else {
                list.add(line)
                i++
            }
        }
        return list
    }

    private fun List<KaraokeLine>.rearrangeAccompanimentAlignment(): List<KaraokeLine> {
        var lastAlignment = KaraokeAlignment.Unspecified
        return this.map { line ->
            if (line.isAccompaniment) {
                line.copy(alignment = lastAlignment)
            } else {
                lastAlignment = line.alignment
                line
            }
        }
    }
}