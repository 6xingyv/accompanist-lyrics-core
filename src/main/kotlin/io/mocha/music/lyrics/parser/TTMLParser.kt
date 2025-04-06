package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.karaoke.KaraokeAlignment
import io.mocha.music.lyrics.model.karaoke.KaraokeLine
import io.mocha.music.lyrics.model.karaoke.KaraokeSyllable
import io.mocha.music.lyrics.utils.SimpleXmlParser
import io.mocha.music.lyrics.utils.XmlElement
import io.mocha.music.lyrics.utils.parseAsTime

class TTMLParser : ILyricsParser {

    private data class TTMLMetadata(
        val agents: Map<String, AgentInfo> = emptyMap()
    )

    private data class AgentInfo(
        val type: String,
        val alignment: KaraokeAlignment
    )

    private fun parseMetadata(element: XmlElement): TTMLMetadata {
        val metadata = element.children.find { it.name == "metadata" } ?: return TTMLMetadata()

        val agents = metadata.children
            .filter { it.name == "ttm:agent" }
            .associate { agent ->
                val id = agent.attributes.find { it.name == "xml:id" }?.value ?: ""
                val type = agent.attributes.find { it.name == "type" }?.value ?: ""
                id to AgentInfo(
                    type = type,
                    alignment = when (type) {
                        "person" -> KaraokeAlignment.Start
                        "other" -> KaraokeAlignment.End
                        else -> KaraokeAlignment.Unspecified
                    }
                )
            }

        return TTMLMetadata(agents)
    }

    private fun getAlignmentFromAgent(
        element: XmlElement,
        metadata: TTMLMetadata
    ): KaraokeAlignment {
        val agentId = element.attributes.find { it.name == "ttm:agent" }?.value
        return metadata.agents[agentId]?.alignment ?: KaraokeAlignment.Unspecified
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        val parsedLines = mutableListOf<KaraokeLine>()
        val ttmlContent = lines.joinToString("\n")
        val parser = SimpleXmlParser()
        val rootElement = parser.parse(ttmlContent)

        val metadata = parseMetadata(rootElement)

        fun traverse(element: XmlElement) {
            if (element.name == "p") {
                val beginAttribute = element.attributes.find { it.name == "begin" }
                val endAttribute = element.attributes.find { it.name == "end" }
                val translationAttribute =
                    element.attributes.find { it.name == "ttm:role" && it.value == "x-translation" }

                val begin = beginAttribute?.value
                val end = endAttribute?.value
                val translation = translationAttribute?.value

                val syllables = mutableListOf<KaraokeSyllable>()

                element.children.forEach { child ->
                    syllables.addAll(parseSpan(child))
                }

                if (begin is String && end is String && syllables.isNotEmpty()) {
                    val currentLine = KaraokeLine(
                        syllables = syllables,
                        translation = translation,
                        isAccompaniment = false,
                        alignment = getAlignmentFromAgent(element, metadata),
                        start = begin.parseAsTime(),
                        end = end.parseAsTime()
                    )
                    parsedLines.add(currentLine)
                }
            }
            element.children.forEach { traverse(it) }
        }

        traverse(rootElement)

        return SyncedLyrics(lines = parsedLines)
    }

    private fun parseSpan(element: XmlElement): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()

        if (element.name == "span" && element.attributes.none { it.name == "ttm:role" }) {
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
        } else if (element.name == "span" && element.attributes.any { it.name == "ttm:role" && it.value == "x-bg" }) {
            element.children.forEach { child ->
                syllables.addAll(parseSpan(child))
            }
        }

        return syllables
    }

    fun parse(inputString: String): SyncedLyrics {
        return parse(listOf(inputString))
    }
}
