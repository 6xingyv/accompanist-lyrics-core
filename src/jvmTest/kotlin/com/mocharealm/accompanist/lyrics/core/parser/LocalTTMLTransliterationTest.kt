package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticProvider
import com.mocharealm.accompanist.lyrics.core.utils.parseAsTime
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Optional local validation; copyrighted input is neither committed nor downloaded by CI. */
class LocalTTMLTransliterationTest {
    @Test
    fun allSuppliedCaptionsAreRetained() {
        val path = System.getenv("LYRICS_VALIDATION_TTML") ?: return
        val file = File(path)
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val document = factory.newDocumentBuilder().parse(file)
        fun elements(tag: String) = document.getElementsByTagName(tag).let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
        val metadata = elements("text").filter { it.parentNode.nodeName == "transliteration" }
            .associate { it.getAttribute("for") to it.textContent.replace(Regex("\\s+"), " ").trim() }
        val paragraphs = elements("p").sortedBy { it.getAttribute("begin").parseAsTime() }
        val provider = object : PhoneticProvider {
            override val phoneticLevel = PhoneticLevel.SYLLABLE
            override fun getPhonetic(string: String): String = error("Supplied caption discarded")
        }
        val parsed = TTMLParser(provider).parse(file.readText()).lines
        assertEquals(paragraphs.size, parsed.size)
        var retained = 0
        var inline = 0
        parsed.zip(paragraphs).forEach { (source, p) ->
            val key = p.getAttribute("itunes:key")
            val expected = metadata[key] ?: return@forEach
            val line = source as KaraokeLine
            val actual = line.phonetic ?: buildString {
                line.syllables.forEach { syllable ->
                    syllable.phonetic?.let { caption ->
                        if (isNotEmpty()) append(syllable.phoneticSeparatorBefore)
                        append(caption)
                    }
                }
            }
            assertEquals(expected, actual, "Caption at $key")
            val sourceSpans = p.childNodes.let { nodes -> (0 until nodes.length).mapNotNull { nodes.item(it) as? Element } }
                .filter { it.tagName == "span" && it.hasAttribute("begin") && it.hasAttribute("end") }
            assertEquals(sourceSpans.map { it.getAttribute("begin").parseAsTime() to it.getAttribute("end").parseAsTime() }, line.syllables.map { it.start to it.end })
            assertEquals(p.getAttribute("begin").parseAsTime(), line.start)
            assertEquals(p.getAttribute("end").parseAsTime(), line.end)
            retained++
            if (line.phonetic == null) inline++
        }
        assertTrue(retained > 0)
        println("Local TTML: retained=$retained, attached-captions=$inline, generated=0; all timing comes from original syllables")
    }
}
