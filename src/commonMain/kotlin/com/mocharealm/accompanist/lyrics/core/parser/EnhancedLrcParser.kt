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
    private val tagRegex = Regex("""\[(.*?)\]""")
    private val timestampPattern = Regex("""\d+([:.]\d+)+""")

    private fun isTimestamp(s: String): Boolean {
        return timestampPattern.matches(s.trim())
    }

    private fun proceduralParseSyllables(content: String): List<KaraokeSyllable> {
        if (content.isBlank()) return emptyList()

        val syllables = mutableListOf<KaraokeSyllable>()
        val syllableRegex = Regex("""<([^>]+)>([^<]*)""")
        val matches = syllableRegex.findAll(content).toList()

        for (match in matches) {
            val tsPart = match.groupValues[1].trim()
            val text = match.groupValues[2]

            if (isTimestamp(tsPart)) {
                val time = runCatching { tsPart.parseAsTime() }.getOrNull()
                if (time != null) {
                    syllables.add(KaraokeSyllable(text, time, time))
                }
            }
        }

        return if (syllables.isEmpty()) emptyList() else syllables.rearrangeTime()
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines).filter { it.isNotBlank() }

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
        if (string.isBlank()) return emptyList()

        val matches = tagRegex.findAll(string).toList()
        if (matches.isEmpty()) return emptyList()

        var lastEnd = 0
        val leadingTags = mutableListOf<MatchResult>()
        for (match in matches) {
            val prefix = string.substring(lastEnd, match.range.first)
            if (prefix.isBlank()) {
                leadingTags.add(match)
                lastEnd = match.range.last + 1
            } else break
        }

        if (leadingTags.isEmpty()) return emptyList()

        val content = if (lastEnd < string.length) string.substring(lastEnd).trim() else ""
        val results = mutableListOf<KaraokeLine>()
        val timestamps = mutableListOf<Int>()
        var bgTag: String? = null

        for (match in leadingTags) {
            val tagContentRaw = match.groupValues[1].trim()
            if (tagContentRaw.startsWith("bg:")) {
                bgTag = tagContentRaw.substring(3).trim()
            } else if (isTimestamp(tagContentRaw)) {
                runCatching { tagContentRaw.parseAsTime() }.getOrNull()?.let { timestamps.add(it) }
            }
        }

        val bgSyllables = bgTag?.let { proceduralParseSyllables(it) } ?: emptyList()
        val mainSyllables = if (timestamps.isNotEmpty() && content.isNotBlank()) {
            proceduralParseSyllables(content)
        } else emptyList()

        val voiceMatch = voiceParser.find(content)
        val alignment = when (voiceMatch?.groupValues?.get(1)) {
            "v1" -> KaraokeAlignment.Start
            "v2" -> KaraokeAlignment.End
            else -> KaraokeAlignment.Unspecified
        }
        val textContent = voiceMatch?.groupValues?.get(2)?.trim() ?: content

        val firstTimestamp = timestamps.firstOrNull() ?: 0
        val isRelative = mainSyllables.firstOrNull()?.start?.let { it < firstTimestamp } ?: false
        val bgIsRelative = bgSyllables.firstOrNull()?.start?.let { it < firstTimestamp } ?: false

        if (timestamps.isNotEmpty()) {
            for (startTime in timestamps) {
                if (mainSyllables.isNotEmpty()) {
                    val offset = if (isRelative) startTime else startTime - firstTimestamp
                    val shifted = mainSyllables.map { it.copy(start = it.start + offset, end = it.end + offset) }
                    results.add(KaraokeLine.MainKaraokeLine(
                        syllables = shifted,
                        translation = null,
                        alignment = alignment,
                        start = shifted.first().start,
                        end = shifted.last().end
                    ))
                } else if (textContent.isNotBlank()) {
                    results.add(KaraokeLine.MainKaraokeLine(
                        syllables = listOf(KaraokeSyllable(textContent, startTime, startTime)),
                        translation = null,
                        alignment = alignment,
                        start = startTime,
                        end = startTime
                    ))
                }

                if (bgSyllables.isNotEmpty()) {
                    val bgOffset = if (bgIsRelative) startTime else startTime - firstTimestamp
                    val shifted = bgSyllables.map { it.copy(start = it.start + bgOffset, end = it.end + bgOffset) }
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

    private fun List<KaraokeSyllable>.rearrangeTime(): List<KaraokeSyllable> {
        if (this.isEmpty()) return emptyList()
        val list = mutableListOf<KaraokeSyllable>()
        for (i in 0 until this.size - 1) {
            list.add(this[i].copy(end = this[i + 1].start))
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

                // 兼容逻辑：类型相同，或者当 AccompanimentLine 的翻译未带 bg 标签而被识别为 MainLine 时
                val isCompatibleType = (line::class == nextLine::class) ||
                        (line is KaraokeLine.AccompanimentKaraokeLine && nextLine is KaraokeLine.MainKaraokeLine)

                if (isCompatibleType && abs(line.start - nextLine.start) <= 150) {
                    val nextContent = nextLine.syllables.joinToString("") { it.content }.trim()
                    if (content != nextContent && content.isNotEmpty()) {
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