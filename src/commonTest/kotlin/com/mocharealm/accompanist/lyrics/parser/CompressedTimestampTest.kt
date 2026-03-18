package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.parser.LrcParser
import com.mocharealm.accompanist.lyrics.core.parser.EnhancedLrcParser
import kotlin.test.Test
import kotlin.test.assertEquals

class CompressedTimestampTest {

    @Test
    fun testLrcCompressedTimestamps() {
        val lrc = """
            [00:12.50][01:30.20][02:15.00]这里是一句重复的副歌歌词
        """.trimIndent().split("\n")

        val result = LrcParser.parse(lrc)
        assertEquals(3, result.lines.size)
        
        assertEquals(12500, (result.lines[0] as SyncedLine).start)
        assertEquals("这里是一句重复的副歌歌词", (result.lines[0] as SyncedLine).content)
        
        assertEquals(90200, (result.lines[1] as SyncedLine).start)
        assertEquals("这里是一句重复的副歌歌词", (result.lines[1] as SyncedLine).content)
        
        assertEquals(135000, (result.lines[2] as SyncedLine).start)
        assertEquals("这里是一句重复的副歌歌词", (result.lines[2] as SyncedLine).content)
    }

    @Test
    fun testEnhancedLrcCompressedTimestamps() {
        val lrc = """
            [00:10.00][00:30.00]<00:00.00>Main <00:00.50>Lyrics<00:00.70>
        """.trimIndent().split("\n")
        
        val result = EnhancedLrcParser.parse(lrc)
        assertEquals(2, result.lines.size)
        
        val line1 = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals(10000, line1.start)
        assertEquals("Main Lyrics", line1.syllables.joinToString("") { it.content })
        assertEquals(10000, line1.syllables[0].start)
        assertEquals(10500, line1.syllables[1].start)
        
        val line2 = result.lines[1] as KaraokeLine.MainKaraokeLine
        assertEquals(30000, line2.start)
        assertEquals("Main Lyrics", line2.syllables.joinToString("") { it.content })
        assertEquals(30000, line2.syllables[0].start)
        assertEquals(30500, line2.syllables[1].start)
    }

    @Test
    fun testEnhancedLrcMixedAbsoluteCompressed() {
        // [00:10.00] with absolute <00:10.00>
        val lrc = """
            [00:10.00][00:30.00]<00:10.00>Main <00:10.50>Lyrics<00:10.70>
        """.trimIndent().split("\n")
        
        val result = EnhancedLrcParser.parse(lrc)
        assertEquals(2, result.lines.size)
        
        val line1 = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals(10000, line1.start)
        assertEquals(10000, line1.syllables[0].start)
        assertEquals(10500, line1.syllables[1].start)
        
        val line2 = result.lines[1] as KaraokeLine.MainKaraokeLine
        assertEquals(30000, line2.start)
        assertEquals(30000, line2.syllables[0].start)
        assertEquals(30500, line2.syllables[1].start)
    }

    @Test
    fun testEnhancedLrcCompressedWithBg() {
        val lrc = """
            [00:10.00][00:30.00][bg:<00:10.00>Back<00:10.50>]<00:00.00>Main <00:00.50>Lyrics<00:00.70>
        """.trimIndent().split("\n")

        val result = EnhancedLrcParser.parse(lrc)
        // Should have 2 main lines, each with the background line attached
        assertEquals(2, result.lines.size)

        val line1 = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals(1, line1.accompanimentLines?.size)
        assertEquals("Back", line1.accompanimentLines?.first()?.syllables?.first()?.content)

        val line2 = result.lines[1] as KaraokeLine.MainKaraokeLine
        assertEquals(1, line2.accompanimentLines?.size)
        assertEquals("Back", line2.accompanimentLines?.first()?.syllables?.first()?.content)
    }

}
