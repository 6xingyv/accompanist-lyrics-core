package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.karaoke.KaraokeAlignment
import io.mocha.music.lyrics.model.karaoke.KaraokeLine
import io.mocha.music.lyrics.model.karaoke.KaraokeSyllable
import io.mocha.music.lyrics.utils.SimpleXmlParser
import io.mocha.music.lyrics.utils.XmlElement
import io.mocha.music.lyrics.utils.parseAsTime

class TTMLParser : ILyricsParser {

    private fun parseMetadata(element: XmlElement): Map<String, KaraokeAlignment> {
        val metadata = element.children.find { it.name == "metadata" } ?: return emptyMap()
        
        return metadata.children
            .filter { it.name == "ttm:agent" }
            .mapIndexed { index, agent ->
                val id = agent.attributes.find { it.name == "xml:id" }?.value ?: ""
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

        fun traverse(element: XmlElement) {
            if (element.name == "p") {
                val beginAttribute = element.attributes.find { it.name == "begin" }
                val endAttribute = element.attributes.find { it.name == "end" }
                val translationAttribute =
                    element.attributes.find { it.name == "ttm:role" && it.value == "x-translation" }

                val begin = beginAttribute?.value
                val end = endAttribute?.value
                val translation = translationAttribute?.value

                // 获取当前p元素的alignment，后续背景音节会用到
                val currentAlignment = getAlignmentFromAgent(element, agentAlignments)

                // 先处理普通歌词行
                val syllables = mutableListOf<KaraokeSyllable>()
                element.children.forEach { child ->
                    if (child.name != "span" || child.attributes.none { it.name == "ttm:role" && it.value == "x-bg" }) {
                        syllables.addAll(parseSpan(child))
                    }
                }

                if (begin is String && end is String && syllables.isNotEmpty()) {
                    val currentLine = KaraokeLine(
                        syllables = syllables,
                        translation = translation,
                        isAccompaniment = false,
                        alignment = currentAlignment,
                        start = begin.parseAsTime(),
                        end = end.parseAsTime()
                    )
                    parsedLines.add(currentLine)
                }

                // 后处理背景音节，使用相同的alignment
                element.children.forEach { child ->
                    if (child.name == "span" && child.attributes.any { it.name == "ttm:role" && it.value == "x-bg" }) {
                        val bgSpanBegin = child.attributes.find { it.name == "begin" }?.value
                        val bgSpanEnd = child.attributes.find { it.name == "end" }?.value
                        
                        if (bgSpanBegin != null && bgSpanEnd != null) {
                            val accompanimentSyllables = parseSpan(child)
                            if (accompanimentSyllables.isNotEmpty()) {
                                parsedLines.add(KaraokeLine(
                                    syllables = accompanimentSyllables,
                                    translation = null,
                                    isAccompaniment = true,
                                    alignment = currentAlignment,  // 使用缓存的alignment
                                    start = bgSpanBegin.parseAsTime(),
                                    end = bgSpanEnd.parseAsTime()
                                ))
                            }
                        }
                    }
                }
            }
            element.children.forEach { 
                if (element.name != "p") {
                    traverse(it)
                }
            }
        }

        traverse(rootElement)

        return SyncedLyrics(lines = parsedLines)
    }

    private fun parseSpan(element: XmlElement): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()

        if (element.name == "span") {
            val hasXBgRole = element.attributes.any { it.name == "ttm:role" && it.value == "x-bg" }
            if (!hasXBgRole) {
                val spanBegin = element.attributes.find { it.name == "begin" }?.value
                val spanEnd = element.attributes.find { it.name == "end" }?.value
                if (spanBegin is String && spanEnd is String && element.text.isNotEmpty()) {
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
                    val childSpanBegin = child.attributes.find { it.name == "begin" }?.value
                    val childSpanEnd = child.attributes.find { it.name == "end" }?.value
                    if (childSpanBegin is String && childSpanEnd is String && child.text.isNotEmpty()) {
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

        return syllables
    }
}
