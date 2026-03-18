package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.utils.LrcMetadataHelper
import com.mocharealm.accompanist.lyrics.core.utils.parseAsTime
import kotlin.math.abs

/**
 * A parser for Enhanced LRC files.
 *
 * Enhanced LRC extends the standard LRC format with support for syllable-level timing (Karaoke),
 * multiple singers/voices, and background vocals.
 *
 * Example format:
 * ```
 * [00:12.34]<00:12.34>Hel<00:12.60>lo <00:12.90>World
 * [bg:<00:12.34>Back<00:12.60>ground<00:12.90>]
 * ```
 */
object EnhancedLrcParser : ILyricsParser {
    private val lineRegex = Regex("^\\[(.*?)](\\s*(.*))?$")
    private val voiceParser = Regex("^(v\\d+)\\s*:\\s*(.*)")

    private fun isTimestamp(s: String): Boolean {
        if (s.length < 5) return false
        var colonCount = 0
        var hasDot = false
        for (c in s) {
            if (c == ':') colonCount++
            else if (c == '.') hasDot = true
            else if (!c.isDigit()) return false
        }
        return colonCount >= 1 && hasDot
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val rawData = lyricsLines.asSequence()
            .mapNotNull { line -> parseLine(line) }
            .toList()
            .combineRawWithTranslation()
            .rearrangeAccompanimentAlignment()
        
        val data = mutableListOf<KaraokeLine>()
        rawData.forEach { line ->
            if (line is KaraokeLine.AccompanimentKaraokeLine && data.isNotEmpty()) {
                val last = data.last()
                if (last is KaraokeLine.MainKaraokeLine) {
                    val updated = last.copy(
                        accompanimentLines = (last.accompanimentLines ?: emptyList()) + line
                    )
                    data[data.size - 1] = updated
                } else {
                    data.add(line)
                }
            } else {
                data.add(line)
            }
        }
        val attributes = LrcMetadataHelper.parse(lines)
        return SyncedLyrics(
            lines = data,
            title = attributes.title ?: "",
            artists = attributes.artist?.let { artistStr ->
                artistStr.split("/").map { part ->
                    val segments = part.split(":", limit = 2)
                    if (segments.size == 2) com.mocharealm.accompanist.lyrics.core.model.Artist(segments[0], segments[1])
                    else com.mocharealm.accompanist.lyrics.core.model.Artist("Main", part)
                }
            } ?: emptyList()
        )
    }

    private fun parseLine(string: String): KaraokeLine? {
        val lineMatch = lineRegex.find(string) ?: return null

        val tagContentRaw = lineMatch.groupValues[1]
        val mainContent = lineMatch.groupValues[3] ?: ""

        // Case 1: Background line [bg:...]
        if (tagContentRaw.trim().startsWith("bg:")) {
            val bgTagBody = tagContentRaw.trim().substring(3).trim()
            
            // Background line MUST contain syllables within the tag body
            val syllables = proceduralParseSyllables(bgTagBody)
            if (syllables.isNotEmpty()) {
                 return KaraokeLine.AccompanimentKaraokeLine(
                    syllables = syllables,
                    translation = null,
                    alignment = KaraokeAlignment.Unspecified,
                    start = syllables.first().start,
                    end = syllables.last().end
                )
            }
            return null
        }
        
        // Case 2: Standard/Enhanced line [time]...
        val tagContent = tagContentRaw.trim()
        if (isTimestamp(tagContent)) {
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
                KaraokeLine.MainKaraokeLine(
                    syllables = syllables,
                    translation = null,
                    alignment = alignment,
                    start = syllables.first().start,
                    end = syllables.last().end
                )
            } else {
                KaraokeLine.MainKaraokeLine(
                    syllables = listOf(KaraokeSyllable(textContent, lineStartTime, lineStartTime)),
                    translation = null,
                    alignment = alignment,
                    start = lineStartTime,
                    end = lineStartTime
                )
            }
        }
        return null
    }

    private fun proceduralParseSyllables(content: String): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()
        var currentIndex = 0
        while (currentIndex < content.length) {
            val openTagStart = content.indexOf('<', currentIndex)
            if (openTagStart == -1) break

            val openTagEnd = content.indexOf('>', openTagStart)
            if (openTagEnd == -1) break

            val timestamp = content.substring(openTagStart + 1, openTagEnd).trim()

            if (!isTimestamp(timestamp)) {
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

    private fun List<KaraokeSyllable>.rearrangeTime(): List<KaraokeSyllable> {
        if (this.isEmpty()) return emptyList()
        val list = ArrayList<KaraokeSyllable>(this.size)
        for (i in 0 until this.size - 1) {
            val syllable = this[i]
            if (syllable.content.isNotEmpty()) {
                list.add(syllable.copy(end = this[i + 1].start))
            }
        }
        val last = this.last()
        if (last.content.isNotEmpty()) {
            list.add(last)
        }
        return list
    }

    private fun List<KaraokeLine>.combineRawWithTranslation(): List<KaraokeLine> {
        val list = ArrayList<KaraokeLine>()
        val usedIndices = mutableSetOf<Int>()

        for (i in this.indices) {
            if (i in usedIndices) continue
            val line = this[i]
            val content = line.syllables.joinToString("") { it.content }.trim()
            
            var translationFound = false
            for (j in i + 1 until this.size) {
                if (j in usedIndices) continue
                val nextLine = this[j]
                
                // Allow AccompanimentKaraokeLine to match MainKaraokeLine as translation if times are close
                val isCompatibleType = (line::class == nextLine::class) || 
                                       (line is KaraokeLine.AccompanimentKaraokeLine && nextLine is KaraokeLine.MainKaraokeLine)
                
                if (isCompatibleType && abs(line.start - nextLine.start) <= 100) {
                    val nextContent = nextLine.syllables.joinToString("") { it.content }.trim()
                    if (content != nextContent) {
                        val updated = when (line) {
                            is KaraokeLine.MainKaraokeLine -> line.copy(translation = nextContent)
                            is KaraokeLine.AccompanimentKaraokeLine -> line.copy(translation = nextContent)
                        }
                        list.add(updated)
                        usedIndices.add(i)
                        usedIndices.add(j)
                        translationFound = true
                        break
                    }
                }
            }
            
            if (!translationFound) {
                list.add(line)
                usedIndices.add(i)
            }
        }
        return list
    }

    private fun List<KaraokeLine>.rearrangeAccompanimentAlignment(): List<KaraokeLine> {
        var lastAlignment = KaraokeAlignment.Unspecified
        return this.map { line ->
            if (line is KaraokeLine.AccompanimentKaraokeLine) {
                if (line.alignment == lastAlignment) line else line.copy(alignment = lastAlignment)
            } else {
                lastAlignment = line.alignment
                line
            }
        }
    }
}
