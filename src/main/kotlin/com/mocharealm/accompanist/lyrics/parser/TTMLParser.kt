package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.utils.SimpleXmlParser
import com.mocharealm.accompanist.lyrics.utils.XmlElement
import com.mocharealm.accompanist.lyrics.utils.parseAsTime

/**
 * A parser for lyrics in the TTML(Apple Syllable) format.
 *
 * More information about TTML(Apple Syllable) format can be found [here](https://help.apple.com/itc/videoaudioassetguide/#/itc0f14fecdd).
 */
object TTMLParser : ILyricsParser {
    override fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("") { it.trimIndent() })
    }

    // Fucking AMLL and other getter not obeying the rules
    private fun preformattingTTML(content: String): String =
        content
            .replace("  ","")
            .replace(" </span><span", "</span> <span")
            .replace(",</span><span", ",</span> <span")

    override fun parse(content: String): SyncedLyrics {
        val content = preformattingTTML(content)
        val parsedLines = mutableListOf<KaraokeLine>()
        val parser = SimpleXmlParser()
        val rootElement = parser.parse(content)

        val agentAlignments = parseMetadata(rootElement)

        val allPElements = findAllPElements(rootElement)

        allPElements.forEach { pElement ->
            val begin = pElement.attributes.find { it.name == "begin" }?.value
            val end = pElement.attributes.find { it.name == "end" }?.value

            if (begin != null && end != null) {
                val currentAlignment = getAlignmentFromAgent(pElement, agentAlignments)

                val pLevelTranslationSpan = pElement.children.find { child ->
                    child.attributes.any { it.name.endsWith(":role") && it.value == "x-translation" } &&
                            child.attributes.none { it.name.endsWith(":role") && it.value == "x-bg" }
                }

                // 处理普通歌词行
                val syllables = parseSyllablesFromChildren(pElement.children)

                if (syllables.isNotEmpty()) {
                    parsedLines.add(
                        KaraokeLine(
                            syllables = syllables,
                            translation = pLevelTranslationSpan?.text?.trim(),
                            isAccompaniment = false,
                            alignment = currentAlignment,
                            start = begin.parseAsTime(),
                            end = end.parseAsTime()
                        )
                    )
                }

                pElement.children.forEach { child ->
                    if (child.name == "span" && child.attributes.any {
                            it.name.endsWith(":role") && it.value == "x-bg"
                        }) {
                        val bgSpanBegin = child.attributes.find { it.name == "begin" }?.value
                        val bgSpanEnd = child.attributes.find { it.name == "end" }?.value


                        val accompanimentSyllables = parseSyllablesFromChildren(child.children)
                        if (accompanimentSyllables.isNotEmpty()) {
                            val bgTranslationSpan = child.children.find { bgChild ->
                                bgChild.attributes.any { it.name.endsWith(":role") && it.value == "x-translation" }
                            }

                            parsedLines.add(
                                KaraokeLine(
                                    syllables = accompanimentSyllables,
                                    translation = bgTranslationSpan?.text?.trim(),
                                    isAccompaniment = true,
                                    alignment = currentAlignment,
                                    start = bgSpanBegin?.parseAsTime()
                                        ?: accompanimentSyllables.first().start,
                                    end = bgSpanEnd?.parseAsTime()
                                        ?: accompanimentSyllables.last().end
                                )
                            )
                        }

                    }
                }
            }
        }

        return SyncedLyrics(lines = parsedLines.sortedBy { it.start })
    }

    /**
     * Parses a list of XmlElement children to extract KaraokeSyllables.
     * This function intelligently handles spacing by checking for `#text` nodes between `<span>` elements.
     */
    private fun parseSyllablesFromChildren(children: List<XmlElement>): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()
        for (i in children.indices) {
            val child = children[i]

            // We only care about <span> elements that are not for translation or background roles at this level.
            if (child.name == "span" && child.attributes.none {
                    it.name.endsWith(":role") && (it.value == "x-translation" || it.value == "x-bg")
                }) {
                val spanBegin = child.attributes.find { it.name == "begin" }?.value
                val spanEnd = child.attributes.find { it.name == "end" }?.value

                if (spanBegin != null && spanEnd != null && child.text.isNotEmpty()) {
                    var syllableContent = child.text

                    // Look ahead to the next sibling to see if it's a between tags text node.
                    // This indicates a space between words (syllables).
                    val nextSibling = children.getOrNull(i + 1)
                    if (nextSibling != null && nextSibling.name == "#text"){
                        syllableContent += nextSibling.text
                    }

                    syllables.add(
                        KaraokeSyllable(
                            content = syllableContent,
                            start = spanBegin.parseAsTime(),
                            end = spanEnd.parseAsTime()
                        )
                    )
                }
            }
        }

        // Trim the trailing space from the very last syllable of the line.
        if (syllables.isNotEmpty()) {
            val lastSyllable = syllables.last()
            syllables[syllables.lastIndex] =
                lastSyllable.copy(content = lastSyllable.content.trimEnd())
        }

        return syllables
    }

    private fun parseMetadata(element: XmlElement): Map<String, KaraokeAlignment> {
        fun findMetadata(elem: XmlElement): XmlElement? {
            if (elem.name == "metadata") return elem
            return elem.children.firstNotNullOfOrNull { findMetadata(it) }
        }

        val metadata = findMetadata(element) ?: return emptyMap()

        return metadata.children
            .filter { it.name.endsWith(":agent") || it.name == "agent" }
            .mapIndexed { index, agent ->
                val id = agent.attributes.find {
                    it.name == "xml:id" || it.name == "id"
                }?.value ?: ""
                id to if (index == 0) KaraokeAlignment.Start else KaraokeAlignment.End
            }.toMap()
    }

    private fun getAlignmentFromAgent(
        element: XmlElement,
        agentAlignments: Map<String, KaraokeAlignment>
    ): KaraokeAlignment {
        val agentId = element.attributes.find { it.name == "ttm:agent" }?.value
        return agentAlignments[agentId] ?: KaraokeAlignment.Start
    }

    private fun findAllPElements(element: XmlElement): List<XmlElement> {
        val pElements = mutableListOf<XmlElement>()
        if (element.name == "p") {
            pElements.add(element)
        }
        element.children.forEach { child ->
            pElements.addAll(findAllPElements(child))
        }
        return pElements
    }
}