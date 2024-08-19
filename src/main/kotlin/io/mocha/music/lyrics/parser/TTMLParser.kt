package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.ISyncedLine
import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.karaoke.KaraokeAlignment
import io.mocha.music.lyrics.model.karaoke.KaraokeLine
import io.mocha.music.lyrics.model.karaoke.KaraokeSyllable
import io.mocha.music.lyrics.utils.parseAsTime
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {
    fun parse(ttml: String): SyncedLyrics {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(InputSource(StringReader(ttml)))
        val divs = doc.getElementsByTagName("div")
        val head = doc.getElementsByTagName("head").item(0) as Element

        val singerAlignments = parseSingerAlignments(head)

        val lines = mutableListOf<ISyncedLine>()
        for (i in 0 until divs.length) {
            val div = divs.item(i) as Element
            val paragraphs = div.getElementsByTagName("p")

            for (j in 0 until paragraphs.length) {
                val paragraph = paragraphs.item(j) as Element
                val start = parseTime(paragraph, "begin")
                val end = parseTime(paragraph, "end")

                val (mainSyllables, accompanimentSyllables) = parseSyllables(paragraph)

                val speakerId = paragraph.getAttribute("ttm:agent")
                val alignment = singerAlignments[speakerId] ?: KaraokeAlignment.Unspecified

                if (mainSyllables.isNotEmpty()) {
                    lines.add(KaraokeLine(mainSyllables, null, false, alignment, start, end))
                }

                if (accompanimentSyllables.isNotEmpty()) {
                    lines.add(KaraokeLine(accompanimentSyllables, null, true, alignment, start, end))
                }
            }
        }

        return SyncedLyrics(lines)
    }

    private fun parseSingerAlignments(head: Element): Map<String, KaraokeAlignment> {
        val agents = head.getElementsByTagName("ttm:agent")
        return (0 until agents.length).associate { i ->
            val agent = agents.item(i) as Element
            val agentId = agent.getAttribute("xml:id")
            val agentType = agent.getAttribute("type")

            val alignment = when (agentType) {
                "person", "group", "other" -> if (i == 0) KaraokeAlignment.Start else KaraokeAlignment.End
                else -> KaraokeAlignment.Unspecified
            }
            agentId to alignment
        }
    }

    private fun parseTime(element: Element, attributeName: String): Int {
        return (element.getAttribute(attributeName) ?: "0").parseAsTime()
    }

    private fun parseSyllables(paragraph: Element): Pair<List<KaraokeSyllable>, List<KaraokeSyllable>> {
        val mainSyllables = mutableListOf<KaraokeSyllable>()
        val accompanimentSyllables = mutableListOf<KaraokeSyllable>()

        val spans = paragraph.getElementsByTagName("span")

        var previousEnd = -1

        for (k in 0 until spans.length) {
            val span = spans.item(k) as Element
            val content = span.textContent
            val spanStart = parseTime(span, "begin")
            val spanEnd = parseTime(span, "end")

            val syllableContent = extractSyllableContent(span, content)

            val syllable = KaraokeSyllable(syllableContent, spanStart, spanEnd)

            if (span.hasAttribute("ttm:role") && span.getAttribute("ttm:role") == "x-bg") {
                accompanimentSyllables.add(syllable)
            } else {
                if (previousEnd != -1 && spanStart > previousEnd) {
                    mainSyllables.add(KaraokeSyllable(" ", previousEnd, spanStart))
                }
                mainSyllables.add(syllable)
            }
            previousEnd = spanEnd
        }

        // Combine syllables and include spaces for accompaniment syllables
        val combinedAccompanimentSyllables = combineSyllablesWithSpaces(accompanimentSyllables)

        return mainSyllables to combinedAccompanimentSyllables
    }

    private fun extractSyllableContent(span: Element, content: String): String {
        return if (span.hasAttribute("ttm:role") && span.getAttribute("ttm:role") == "x-bg") {
            if (content.startsWith("(") && content.endsWith(")")) {
                content.removeSurrounding("(", ")")
            } else content
        } else {
            content
        }
    }

    private fun combineSyllablesWithSpaces(syllables: List<KaraokeSyllable>): List<KaraokeSyllable> {
        val combined = mutableListOf<KaraokeSyllable>()
        var currentStart = -1
        var currentEnd = -1
        var combinedContent = ""

        for (syllable in syllables) {
            if (currentStart == -1) {
                currentStart = syllable.start
                currentEnd = syllable.end
                combinedContent = syllable.content
            } else if (syllable.start == currentEnd) {
                currentEnd = syllable.end
                combinedContent += " " + syllable.content
            } else {
                combined.add(KaraokeSyllable(combinedContent, currentStart, currentEnd))
                currentStart = syllable.start
                currentEnd = syllable.end
                combinedContent = syllable.content
            }
        }

        if (currentStart != -1) {
            combined.add(KaraokeSyllable(combinedContent, currentStart, currentEnd))
        }

        return combined
    }
}
