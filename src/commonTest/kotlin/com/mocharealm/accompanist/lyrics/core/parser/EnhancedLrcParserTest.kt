package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.exporter.LrcExporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EnhancedLrcParserTest {

    @Test
    fun testParseBgWithTranslation() {
        val lrc = """
            [00:10.00]<00:10.00>Main <00:10.50>Lyrics<00:10.70>
            [00:10.00]主歌词翻译
            [bg: <00:10.00>Back<00:10.50>ground<00:11.00>]
            [bg: <00:10.00>背景音翻译<00:11.00>]
        """.trimIndent().split("\n")
        
        val data = EnhancedLrcParser.parse(lrc)
        
        assertEquals(1, data.lines.size)
        val line = data.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals("主歌词翻译", line.translation)
        
        val bg = line.accompanimentLines?.first()
        assertNotNull(bg)
        assertEquals("Background", bg.syllables.joinToString("") { it.content }.trim())
        assertEquals("背景音翻译", bg.translation)
    }

    @Test
    fun testELRCRoundTrip() {
        val lrc = """
            [ti:Test Title]
            [00:01.00]<00:01.00>Hello <00:02.00>World<00:02.50>
            [00:01.00]你好世界
            [bg: <00:01.50>Chorus<00:02.00>]
            [bg: <00:01.50>合唱<00:02.00>]
        """.trimIndent().split("\n")
        
        val parsed = EnhancedLrcParser.parse(lrc)
        val exported = LrcExporter.export(parsed)
        
        val reParsed = EnhancedLrcParser.parse(exported.split("\n"))
        
        assertEquals(parsed.lines.size, reParsed.lines.size)
        val p1 = parsed.lines[0] as KaraokeLine.MainKaraokeLine
        val p2 = reParsed.lines[0] as KaraokeLine.MainKaraokeLine
        
        assertEquals(p1.translation, p2.translation)
        assertEquals(p1.accompanimentLines?.size, p2.accompanimentLines?.size)
        assertEquals(p1.accompanimentLines?.first()?.translation, p2.accompanimentLines?.first()?.translation)
    }
}
