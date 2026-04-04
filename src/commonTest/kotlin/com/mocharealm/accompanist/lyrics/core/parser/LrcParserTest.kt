package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.exporter.EnhancedLrcExporter
import kotlin.test.Test
import kotlin.test.assertEquals

class LrcParserTest {

    @Test
    fun testStandardLrcWithTranslation() {
        val lrc = """
            [ti:Song Title]
            [ar:Artist Name]
            [00:01.00]Line 1
            [00:01.00]Translation 1
            [00:02.50]Line 2
        """.trimIndent().split("\n")

        val result = EnhancedLrcParser.parse(lrc)
        assertEquals("Song Title", result.title)
        assertEquals(2, result.lines.size)

        val line1 = result.lines[0] as SyncedLine
        assertEquals("Line 1", line1.content)
        assertEquals("Translation 1", line1.translation)
    }

    @Test
    fun testLRCRoundTrip() {
        val original = """
            [ti:Round Trip]
            [00:05.00]Hello
            [00:05.00]你好
            [00:10.00]World
        """.trimIndent()

        val parsed = EnhancedLrcParser.parse(original.split("\n"))
        val exported = EnhancedLrcExporter.export(parsed)

        val reParsed = EnhancedLrcParser.parse(exported.split("\n"))

        assertEquals(parsed.title, reParsed.title)
        assertEquals(parsed.lines.size, reParsed.lines.size)
        assertEquals((parsed.lines[0] as SyncedLine).translation, (reParsed.lines[0] as SyncedLine).translation)
    }
}