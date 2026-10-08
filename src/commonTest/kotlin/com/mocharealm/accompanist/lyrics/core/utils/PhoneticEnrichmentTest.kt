package com.mocharealm.accompanist.lyrics.core.utils

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.toKaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.*
import com.mocharealm.accompanist.lyrics.core.model.synced.mapper.toSyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import kotlin.test.*

class PhoneticEnrichmentTest {
    private class Provider : PhoneticProvider {
        val requests = mutableListOf<PhoneticRequest>()
        var projections = listOf(PhoneticProjection("yin"), PhoneticProjection("hang", separatorBefore = " "), PhoneticProjection(null))
        override val phoneticLevel = PhoneticLevel.LINE
        override fun getPhonetic(string: String): String = error("Contextual path must use resolve")
        override fun resolve(request: PhoneticRequest): PhoneticResponse {
            requests.add(request)
            return PhoneticResponse("yin hang", if (request.projectionRanges.isEmpty()) emptyList() else projections)
        }
    }
    private fun line() = KaraokeLine.MainKaraokeLine(
        listOf(KaraokeSyllable("银", 10, 20, languageTag = "zh-CN"), KaraokeSyllable("行", 20, 30, languageTag = "zh-CN"), KaraokeSyllable("hola", 30, 40, languageTag = "es")),
        "translation", KaraokeAlignment.End, 10, 40,
    )
    @Test fun fullContextMergedHintsAndIntentionalOmissionPreserveMetadata() {
        val provider = Provider(); val source = line()
        val lyrics = SyncedLyrics(listOf(source), "title", "id")
        val result = lyrics.withPhonetics(provider)
        val request = provider.requests.single()
        assertEquals("银行hola", request.text)
        assertEquals(listOf(PhoneticLanguageHint(PhoneticTextRange(0, 2), "zh-CN"), PhoneticLanguageHint(PhoneticTextRange(2, 6), "es")), request.hints)
        val output = result.lines.single() as KaraokeLine
        assertNull(output.phonetic)
        assertEquals(listOf("yin", "hang", null), output.syllables.map { it.phonetic })
        assertEquals(listOf("", " ", ""), output.syllables.map { it.phoneticSeparatorBefore })
        assertEquals(source.syllables, output.syllables.map { it.copy(phonetic = null, phoneticSeparatorBefore = "") })
        assertEquals(result, result.withPhonetics(provider), "Existing captions and boundary metadata remain intact")
        assertEquals(lyrics.copy(lines = result.lines), result)
    }
    @Test fun unprovenAlignmentUsesLineCaptionAndKeepsSourceSyllables() {
        val provider = Provider().apply { projections = listOf(PhoneticProjection(null, aligned = false), PhoneticProjection("hang"), PhoneticProjection(null)) }
        val source = line(); val output = SyncedLyrics(listOf(source)).withPhonetics(provider).lines.single() as KaraokeLine
        assertEquals("yin hang", output.phonetic)
        assertEquals(source.syllables, output.syllables)
    }
    @Test fun syncedFieldsSurviveConversionsAndAutoParserEnrichesStandardLrc() {
        val source = SyncedLine("銀行", "translation", 100, 200, "yin hang", "zh-TW")
        assertEquals(source, source.toKaraokeLine().toSyncedLine())
        assertEquals(source, UncheckedSyncedLine(source.content, source.translation, 100, 200, source.phonetic, source.languageTag).toSyncedLine())
        val provider = Provider()
        val parsed = AutoParser(fallbackPhoneticProvider = provider).parse("[ti:title]\n[00:01.00]银行\n[00:03.00]银行")
        assertEquals("title", parsed.title)
        assertTrue(parsed.lines.all { it is SyncedLine && it.phonetic == "yin hang" })
        assertEquals(2, provider.requests.size)
        val already = SyncedLyrics(listOf(source))
        assertEquals(already, already.withPhonetics(provider))
        assertEquals(2, provider.requests.size)
    }
    @Test fun suppliedMainCaptionDoesNotSuppressMissingAccompanimentCaption() {
        val provider = Provider().apply { projections = listOf(PhoneticProjection("yin hang")) }
        val backing = KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("银行", 10, 20)), null, KaraokeAlignment.Start, 10, 20)
        val source = line().copy(phonetic = "supplied", accompanimentLines = listOf(backing))
        val output = SyncedLyrics(listOf(source)).withPhonetics(provider).lines.single() as KaraokeLine.MainKaraokeLine
        assertEquals("supplied", output.phonetic)
        assertEquals("yin hang", output.accompanimentLines!!.single().syllables.single().phonetic)
        assertEquals(1, provider.requests.size)
    }
}
