package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.karaoke.KaraokeLine
import io.mocha.music.lyrics.utils.SimpleXmlParser
import io.mocha.music.lyrics.utils.XmlElement
import io.mocha.music.lyrics.utils.parseAsTime

class TTMLParser : ILyricsParser {

    override fun parse(lines: List<String>): SyncedLyrics {
        val parsedLines = mutableListOf<KaraokeLine>()
        val ttmlContent = lines.joinToString("\n")
        val parser = SimpleXmlParser()
        val rootElement = parser.parse(ttmlContent)

        // 递归遍历 XML 元素，查找 <p> 标签
        fun traverse(element: XmlElement) {
            if (element.name == "p") {
                val beginAttribute = element.attributes.find { it.name == "begin" }
                val endAttribute = element.attributes.find { it.name == "end" }
                val begin = beginAttribute?.value
                val end = endAttribute?.value
                val text = element.text
                if (begin is String && end is String && text.isNotEmpty()) {
                    val currentLine = KaraokeLine(
                        TODO(),
                        text,
                        isAccompaniment = false, // 根据实际情况设置
                        alignment = null, // 根据实际情况设置
                        start = begin.parseAsTime(), // 根据实际情况设置
                        end = end.parseAsTime() // 根据实际情况设置
                    )
                    parsedLines.add(currentLine)
                }
            }
            element.children.forEach { traverse(it) }
        }

        traverse(rootElement)

        return SyncedLyrics(parsedLines)
    }

    fun parse(inputString: String): SyncedLyrics {
        return parse(listOf(inputString))
    }
}
