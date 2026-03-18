package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.utils.SimpleXmlParser
import com.mocharealm.accompanist.lyrics.core.utils.XmlElement
import com.mocharealm.accompanist.lyrics.core.utils.parseAsTime
import kotlin.collections.get

/**
 * A parser for lyrics in the TTML(Apple Syllable) format.
 *
 * More information about TTML(Apple Syllable) format can be found [here](https://help.apple.com/itc/videoaudioassetguide/#/itc0f14fecdd).
 */
object TTMLParser : ILyricsParser {
    override fun canParse(content: String): Boolean =
        content.contains("http://www.w3.org/ns/ttml")

    override fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("") { it.trimIndent() })
    }

    // Workaround for AMLL and other tools not strictly following the spec
    private fun preformattingTTML(content: String): String =
        content
            .replace("  ","")
            .replace(" </span><span", "</span> <span")
            .replace(",</span><span", ",</span> <span")

    private fun decodeXmlEntities(text: String): String {
        if (!text.contains('&')) return text
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
    }

    private fun splitTranslationByBracket(text: String): Pair<String, String?> {
        if (!text.endsWith('）')) return Pair(text.trim(), null)
        val bracketStart = text.lastIndexOf('（')
        if (bracketStart == -1) return Pair(text.trim(), null)
        
        val outside = text.substring(0, bracketStart).trim()
        val inside = text.substring(bracketStart + 1, text.length - 1).trim()
        return Pair(outside, inside.ifEmpty { null })
    }

    override fun parse(content: String): SyncedLyrics {
        val preformattedContent = preformattingTTML(content)
        val parsedLines = mutableListOf<KaraokeLine>()
        val parser = SimpleXmlParser()
        val rootElement = parser.parse(preformattedContent)

        val agentAlignments = parseMetadata(rootElement)
        val iTunesTranslations = parseITunesTranslations(rootElement)
        val allPElements = findAllPElements(rootElement)

        allPElements.forEach { pElement ->
            var beginAttr: String? = null
            var endAttr: String? = null
            var agentId: String? = null
            var iTunesLineKey: String? = null

            for (attr in pElement.attributes) {
                when (attr.name) {
                    "begin" -> beginAttr = attr.value
                    "end" -> endAttr = attr.value
                    "ttm:agent" -> agentId = attr.value
                    "itunes:key", "key" -> iTunesLineKey = attr.value
                }
            }

            if (beginAttr != null && endAttr != null) {
                val currentAlignment = agentAlignments[agentId] ?: KaraokeAlignment.Start

                var pLevelTranslationSpan: XmlElement? = null
                for (child in pElement.children) {
                    if (child.name == "span") {
                        var isTranslation = false
                        var isBg = false
                        for (attr in child.attributes) {
                            if (attr.name.endsWith(":role")) {
                                if (attr.value == "x-translation") isTranslation = true
                                else if (attr.value == "x-bg") isBg = true
                            }
                        }
                        if (isTranslation && !isBg) {
                            pLevelTranslationSpan = child
                            break
                        }
                    }
                }

                val iTunesTranslationRaw = if (iTunesLineKey != null) iTunesTranslations[iTunesLineKey] else null
                val iTunesTranslationPair = if (iTunesTranslationRaw != null) splitTranslationByBracket(iTunesTranslationRaw) else null

                val syllables = parseSyllablesFromChildren(pElement.children)
                
                val accompanimentLines = mutableListOf<KaraokeLine.AccompanimentKaraokeLine>()
                for (child in pElement.children) {
                    if (child.name == "span") {
                        var isBg = false
                        for (attr in child.attributes) {
                            if (attr.name.endsWith(":role") && attr.value == "x-bg") {
                                isBg = true
                                break
                            }
                        }
                        
                        if (isBg) {
                            var bgSpanBegin: String? = null
                            var bgSpanEnd: String? = null
                            var bgITunesLineKey: String? = iTunesLineKey

                            for (attr in child.attributes) {
                                when (attr.name) {
                                    "begin" -> bgSpanBegin = attr.value
                                    "end" -> bgSpanEnd = attr.value
                                    "itunes:key", "key" -> bgITunesLineKey = attr.value
                                }
                            }

                            val accompanimentSyllables = parseSyllablesFromChildren(child.children)
                            if (accompanimentSyllables.isNotEmpty()) {
                                var bgTranslationSpan: XmlElement? = null
                                for (bgChild in child.children) {
                                    if (bgChild.name == "span") {
                                        for (attr in bgChild.attributes) {
                                            if (attr.name.endsWith(":role") && attr.value == "x-translation") {
                                                bgTranslationSpan = bgChild
                                                break
                                            }
                                        }
                                        if (bgTranslationSpan != null) break
                                    }
                                }

                                val bgITunesTranslationRaw = if (bgITunesLineKey != null) iTunesTranslations[bgITunesLineKey] else null
                                val bgITunesTranslationPair = if (bgITunesTranslationRaw != null) splitTranslationByBracket(bgITunesTranslationRaw) else null

                                accompanimentLines.add(
                                    KaraokeLine.AccompanimentKaraokeLine(
                                        syllables = accompanimentSyllables,
                                        translation = bgTranslationSpan?.text?.trim()
                                            ?: bgITunesTranslationPair?.second // Backing vocals from inside brackets
                                            ?: bgITunesTranslationPair?.first, // Fallback to outside if inside is empty
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

                if (syllables.isNotEmpty() || accompanimentLines.isNotEmpty()) {
                    parsedLines.add(
                        KaraokeLine.MainKaraokeLine(
                            syllables = if (syllables.isEmpty() && accompanimentLines.isNotEmpty()) {
                                // 如果只有和声没有主音节（这在某些格式中可能发生），使用和声的时间作为占位
                                listOf(KaraokeSyllable("", accompanimentLines.first().start, accompanimentLines.first().start))
                            } else syllables,
                            translation = pLevelTranslationSpan?.text?.trim()
                                ?: iTunesTranslationPair?.first,
                            alignment = currentAlignment,
                            start = beginAttr.parseAsTime(),
                            end = endAttr.parseAsTime(),
                            accompanimentLines = accompanimentLines.ifEmpty { null }
                        )
                    )
                }
            }
        }

        return SyncedLyrics(lines = parsedLines.sortedBy { it.start })
    }

    private fun parseITunesTranslations(element: XmlElement): Map<String, String> {
        val translations = mutableMapOf<String, String>()
        fun findTranslations(elem: XmlElement) {
            if (elem.name == "translation" || elem.name.endsWith(":translation")) {
                elem.children.forEach { textElem ->
                    if (textElem.name == "text") {
                        val key = textElem.attributes.find { it.name == "for" }?.value
                        val value = textElem.text
                        if (key != null && value.isNotBlank()) {
                            translations[key] = value.trim()
                        }
                    }
                }
            }
            elem.children.forEach { findTranslations(it) }
        }
        findTranslations(element)
        return translations
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

                    var syllableContent = decodeXmlEntities(child.text)

                    val nextSibling = children.getOrNull(i + 1)
                    if (nextSibling != null && nextSibling.name == "#text") {
                        syllableContent += decodeXmlEntities(nextSibling.text)
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
            val last = syllables.last()
            syllables[syllables.lastIndex] =
                last.copy(content = last.content.trimEnd())
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