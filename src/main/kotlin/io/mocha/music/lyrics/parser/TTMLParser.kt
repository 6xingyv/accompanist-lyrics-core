package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.ISyncedLine
import io.mocha.music.lyrics.model.karaoke.KaraokeAlignment
import io.mocha.music.lyrics.model.karaoke.KaraokeLine
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import io.mocha.music.lyrics.model.karaoke.KaraokeSyllable
import io.mocha.music.lyrics.utils.parseAsTime
import org.xml.sax.InputSource


object TTMLParser {
    fun parse(ttml: String): SyncedLyrics {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(ttml)))
        val divs = doc.getElementsByTagName("div")
        val head = doc.getElementsByTagName("head")


        val lines = mutableListOf<ISyncedLine>()
        for (i in 0 until divs.length) {
            val div = divs.item(i) as Element
            val paragraphs = div.getElementsByTagName("p")


            for (j in 0 until paragraphs.length) {
                val paragraph = paragraphs.item(j) as Element

                val spans = paragraph.getElementsByTagName("span")
                val start = (paragraph.getAttribute("begin")?: "0").parseAsTime()
                val end = (paragraph.getAttribute("end")?: "0").parseAsTime()
                var isAccompaniment = false
                val syllables = mutableListOf<KaraokeSyllable>()

                for (k in 0 until spans.length) {
                    val span = spans.item(k) as Element
                    val content = span.textContent.trim()
                    val spanStart = (span.getAttribute("begin")).parseAsTime()
                    val spanEnd = (span.getAttribute("end")).parseAsTime()
                    println("$spanStart,$spanEnd,$content")
                    syllables.add(KaraokeSyllable(content, start=spanStart, end=spanEnd))
                }

                lines.add(
                    KaraokeLine(
                        start = start,
                        end = end,
                        syllables = syllables,
                        translation = null,
                        isAccompaniment = isAccompaniment,
                        alignment = KaraokeAlignment.Start
                    )
                )
            }
        }

        return SyncedLyrics(lines)
    }
}