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
    private val voiceParser = Regex("^(v\\d+)\\s*:\\s*(.*)")
    private val tagRegex = Regex("""\\[(.*?)\\]""")

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
        val rawData = lyricsLines.flatMap { line -> parseLine(line) }
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

    private fun parseLine(string: String): List<KaraokeLine> {
        val matches = tagRegex.findAll(string).toList()
        if (matches.isEmpty()) return emptyList()

        var lastEnd = 0
        val leadingTags = mutableListOf<MatchResult>()
        for (match in matches) {
            val prefix = string.substring(lastEnd, match.range.first)
            if (prefix.isBlank()) {
                leadingTags.add(match)
                lastEnd = match.range.last + 1
            } else {
                break
            }
        }
        
        if (leadingTags.isEmpty()) return emptyList()
        
        val content = string.substring(lastEnd).trim()
        val results = mutableListOf<KaraokeLine>()
        
        val timestamps = mutableListOf<Int>()
        var bgTag: String? = null
        
        for (match in leadingTags) {
            val tagContentRaw = match.groupValues[1]
            if (tagContentRaw.trim().startsWith("bg:")) {
                bgTag = tagContentRaw.trim().substring(3).trim()
            } else if (isTimestamp(tagContentRaw.trim())) {
                timestamps.add(tagContentRaw.trim().parseAsTime())
            }
        }
        
        val bgSyllables = bgTag?.let { proceduralParseSyllables(it) } ?: emptyList()
        val mainSyllables = if (timestamps.isNotEmpty()) proceduralParseSyllables(content) else emptyList()
        val (alignment, textContent) = voiceParser.find(content)?.let {
            val align = when (it.groupValues[1]) {
                "v1" -> KaraokeAlignment.Start
                "v2" -> KaraokeAlignment.End
                else -> KaraokeAlignment.Unspecified
            }
            align to it.groupValues[2].trim()
        } ?: (KaraokeAlignment.Unspecified to content)

        if (timestamps.isNotEmpty()) {
            val firstTimestamp = timestamps.first()
            
            val relativeMainSyllables = if (mainSyllables.isNotEmpty() && mainSyllables.first().start >= firstTimestamp) {
                mainSyllables.map { it.copy(start = it.start - firstTimestamp, end = it.end - firstTimestamp) }
            } else {
                mainSyllables
            }

            val relativeBgSyllables = if (bgSyllables.isNotEmpty() && bgSyllables.first().start >= firstTimestamp) {
                bgSyllables.map { it.copy(start = it.start - firstTimestamp, end = it.end - firstTimestamp) }
            } else {
                bgSyllables
            }

            for (startTime in timestamps) {
                // Add Main Line
                if (relativeMainSyllables.isNotEmpty()) {
                    val shifted = relativeMainSyllables.map { it.copy(start = it.start + startTime, end = it.end + startTime) }
                    results.add(KaraokeLine.MainKaraokeLine(
                        syllables = shifted,
                        translation = null,
                        alignment = alignment,
                        start = shifted.first().start,
                        end = shifted.last().end
                    ))
                } else {
                    results.add(KaraokeLine.MainKaraokeLine(
                        syllables = listOf(KaraokeSyllable(textContent, startTime, startTime)),
                        translation = null,
                        alignment = alignment,
                        start = startTime,
                        end = startTime
                    ))
                }

                // Add Background Line (if present)
                if (relativeBgSyllables.isNotEmpty()) {
                    val shifted = relativeBgSyllables.map { it.copy(start = it.start + startTime, end = it.end + startTime) }
                    results.add(KaraokeLine.AccompanimentKaraokeLine(
                        syllables = shifted,
                        translation = null,
                        alignment = KaraokeAlignment.Unspecified,
                        start = shifted.first().start,
                        end = shifted.last().end
                    ))
                }
            }
        } else if (bgSyllables.isNotEmpty()) {
            results.add(KaraokeLine.AccompanimentKaraokeLine(
                syllables = bgSyllables,
                translation = null,
                alignment = KaraokeAlignment.Unspecified,
                start = bgSyllables.first().start,
                end = bgSyllables.last().end
            ))
        }

        return results
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
