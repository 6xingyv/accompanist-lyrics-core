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
class TTMLParser : ILyricsParser {
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

    override fun parse(lines: List<String>): SyncedLyrics {
        val parsedLines = mutableListOf<KaraokeLine>()
        val ttmlContent = lines.joinToString("\n")
        val parser = SimpleXmlParser()
        val rootElement = parser.parse(ttmlContent)

        val agentAlignments = parseMetadata(rootElement)

        fun findAllPElements(element: XmlElement): List<XmlElement> {
            val pElements = mutableListOf<XmlElement>()
            if (element.name == "p") {
                pElements.add(element)
            }
            element.children.forEach { child ->
                pElements.addAll(findAllPElements(child))
            }
            return pElements
        }

        val allPElements = findAllPElements(rootElement)
        
        allPElements.forEach { pElement ->
            val beginAttribute = pElement.attributes.find { it.name == "begin" }
            val endAttribute = pElement.attributes.find { it.name == "end" }

            val begin = beginAttribute?.value
            val end = endAttribute?.value

            if (begin != null && end != null) {
                // 获取当前p元素的alignment
                val currentAlignment = getAlignmentFromAgent(pElement, agentAlignments)

                // 获取p元素级别的翻译（排除背景音节内部的翻译）
                val pLevelTranslationSpan = pElement.children.find { child ->
                    child.attributes.any { it.name.endsWith(":role") && it.value == "x-translation" } &&
                    child.attributes.none { it.name.endsWith(":role") && it.value == "x-bg" }
                }

                // 处理普通歌词行
                val syllables = mutableListOf<KaraokeSyllable>()
                pElement.children.forEach { child ->
                    if (child.name == "span" && child.attributes.none { 
                        it.name.endsWith(":role") && (it.value == "x-bg" || it.value == "x-translation")
                    }) {
                        syllables.addAll(parseSpan(child))
                    }
                }

                if (syllables.isNotEmpty()) {
                    parsedLines.add(KaraokeLine(
                        syllables = syllables,
                        translation = pLevelTranslationSpan?.text,
                        isAccompaniment = false,
                        alignment = currentAlignment,
                        start = begin.parseAsTime(),
                        end = end.parseAsTime()
                    ))
                }

                // 处理背景音节
                pElement.children.forEach { child ->
                    if (child.name == "span" && child.attributes.any { 
                        it.name.endsWith(":role") && it.value == "x-bg" 
                    }) {
                        val bgSpanBegin = child.attributes.find { it.name == "begin" }?.value
                        val bgSpanEnd = child.attributes.find { it.name == "end" }?.value
                        
                        if (bgSpanBegin != null && bgSpanEnd != null) {
                            val accompanimentSyllables = parseSpan(child)
                            if (accompanimentSyllables.isNotEmpty()) {
                                // 查找背景音节内部的翻译
                                val bgTranslationSpan = child.children.find { bgChild ->
                                    bgChild.attributes.any { it.name.endsWith(":role") && it.value == "x-translation" }
                                }
                                
                                parsedLines.add(KaraokeLine(
                                    syllables = accompanimentSyllables,
                                    translation = bgTranslationSpan?.text,
                                    isAccompaniment = true,
                                    alignment = currentAlignment,
                                    start = bgSpanBegin.parseAsTime(),
                                    end = bgSpanEnd.parseAsTime()
                                ))
                            }
                        }
                    }
                }
            }
        }

        return SyncedLyrics(lines = parsedLines)
    }

    private fun parseSpan(element: XmlElement): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()

        if (element.name == "span") {
            val hasXBgRole = element.attributes.any { 
                it.name.endsWith(":role") && it.value == "x-bg" 
            }
            
            if (!hasXBgRole) {
                val spanBegin = element.attributes.find { it.name == "begin" }?.value
                val spanEnd = element.attributes.find { it.name == "end" }?.value
                if (spanBegin != null && spanEnd != null && element.text.isNotEmpty()) {
                    syllables.add(
                        KaraokeSyllable(
                            content = element.text,
                            start = spanBegin.parseAsTime(),
                            end = spanEnd.parseAsTime()
                        )
                    )
                }
            } else {
                // 处理 x-bg span 的子元素
                element.children.forEach { child ->
                    if (child.name == "span") {
                        val childSpanBegin = child.attributes.find { it.name == "begin" }?.value
                        val childSpanEnd = child.attributes.find { it.name == "end" }?.value
                        if (childSpanBegin != null && childSpanEnd != null && child.text.isNotEmpty()) {
                            syllables.add(
                                KaraokeSyllable(
                                    content = child.text,
                                    start = childSpanBegin.parseAsTime(),
                                    end = childSpanEnd.parseAsTime()
                                )
                            )
                        }
                    }
                }
            }
        }

        return syllables
    }
}
