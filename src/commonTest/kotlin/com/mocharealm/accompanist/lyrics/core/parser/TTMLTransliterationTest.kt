package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticProvider
import com.mocharealm.accompanist.lyrics.core.utils.withPhonetics
import kotlin.test.*

class TTMLTransliterationTest {
    private val noFallback = object : PhoneticProvider {
        override val phoneticLevel = PhoneticLevel.SYLLABLE
        override fun getPhonetic(string: String): String = error("Supplied transliteration must take precedence")
    }
    private fun document(roman: String, body: String) = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xml:lang="ja"><head><metadata><transliterations><transliteration xml:lang="ja-Latn"><text for="L1">$roman</text></transliteration></transliterations></metadata></head><body><div><p begin="00:01.000" end="00:03.000" itunes:key="L1">$body</p></div></body></tt>"""

    @Test
    fun finerMoraFragmentsAttachToOriginalWordsAndSilentPunctuationKeepsItsTiming() {
        val roman = """<span begin="00:01.000" end="00:01.300">ha</span><span begin="00:01.300" end="00:01.600">na </span><span begin="00:01.700" end="00:02.000">sa</span><span begin="00:02.100" end="00:03.000">ku</span>"""
        val body = """<span begin="00:01.000" end="00:01.600">花</span><span begin="00:01.600" end="00:01.700">、</span><span begin="00:01.700" end="00:03.000">咲く</span><span ttm:role="x-translation">花开</span>"""
        val lyrics = TTMLParser(noFallback).parse(document(roman, body))
        val line = lyrics.lines.single() as KaraokeLine
        assertNull(line.phonetic)
        assertEquals(listOf("hana", null, "saku"), line.syllables.map { it.phonetic })
        assertEquals(listOf("", "", " "), line.syllables.map { it.phoneticSeparatorBefore })
        assertEquals(listOf("花", "、", "咲く"), line.syllables.map { it.content })
        assertEquals(listOf(1000 to 1600, 1600 to 1700, 1700 to 3000), line.syllables.map { it.start to it.end })
        assertEquals("花开", line.translation)
        assertEquals("ja", line.languageTag)
        assertEquals(lyrics, lyrics.withPhonetics(noFallback))
    }

    @Test
    fun oneToOneCaptionsIgnoreTheirMetadataTimesEntirely() {
        val body = """<span begin="00:01.000" end="00:02.000">気</span><span begin="00:02.000" end="00:03.000">持ち</span>"""
        for (timing in listOf("00:02.011", "00:02.500", "00:10.000")) {
            val roman = """<span begin="00:01.000" end="$timing">ki </span><span begin="$timing" end="00:30.000">mochi</span>"""
            val line = TTMLParser(noFallback).parse(document(roman, body)).lines.single() as KaraokeLine
            assertEquals(listOf("ki", "mochi"), line.syllables.map { it.phonetic })
            assertEquals(listOf(1000 to 2000, 2000 to 3000), line.syllables.map { it.start to it.end })
            assertNull(line.phonetic)
        }
    }

    @Test
    fun finerCaptionsWithElevenMillisecondDifferenceStillUseOriginalClock() {
        val roman = """<span begin="00:01.000" end="00:02.011">ki </span><span begin="00:02.011" end="00:02.500">mo</span><span begin="00:02.500" end="00:03.000">chi</span>"""
        val body = """<span begin="00:01.000" end="00:02.000">気</span><span begin="00:02.000" end="00:03.000">持ち</span>"""
        val line = TTMLParser(noFallback).parse(document(roman, body)).lines.single() as KaraokeLine
        assertEquals(listOf("ki", "mochi"), line.syllables.map { it.phonetic })
        assertEquals(listOf(1000 to 2000, 2000 to 3000), line.syllables.map { it.start to it.end })
        assertNull(line.phonetic)
    }

    @Test
    fun untimedLegacyMetadataPreservesSpacesAndExtraTextRemainsVisible() {
        val body = """<span begin="00:01.000" end="00:02.000">花</span><span begin="00:02.000" end="00:03.000">咲く</span>"""
        val roman = """<span>hana </span><span>saku</span>"""
        val line = TTMLParser(noFallback).parse(document(roman, body)).lines.single() as KaraokeLine
        assertEquals(listOf("hana", "saku"), line.syllables.map { it.phonetic })
        assertEquals(listOf("", " "), line.syllables.map { it.phoneticSeparatorBefore })
        val extra = TTMLParser(noFallback).parse(document("prefix $roman", body)).lines.single() as KaraokeLine
        assertEquals("prefix hana saku", extra.phonetic)
    }

    @Test
    fun plainMetadataSupportsSyncedLinesAndInlineRomanStillWins() {
        val synced = TTMLParser(noFallback).parse(document("hana saku", "花咲く")).lines.single() as SyncedLine
        assertEquals("hana saku", synced.phonetic)
        assertEquals("ja", synced.languageTag)
        val line = TTMLParser(noFallback).parse(document("metadata", """<span begin="00:01.000" end="00:03.000">花</span><span ttm:role="x-roman">inline</span>""")).lines.single() as KaraokeLine
        assertEquals("inline", line.phonetic)
    }
}
