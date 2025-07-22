package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LyricifySyllableParserTest {

    @Test
    fun testParseLyricifySyllable() {
        val lys = """
            [4]I (0,214)promise (214,345)that (559,185)you'll (744,154)never (898,334)find (1232,202)another (1434,470)like (1904,363)me(2267,658)
            [4]I (3476,185)know (3661,150)that (3811,161)I'm (3972,184)a (4156,155)handful, (4311,672)baby, (4983,672)uh(5655,401)
        """.trimIndent().split("\n")
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(2, data.lines.size)

        // 验证第一行
        val firstLine = data.lines[0] as KaraokeLine
        assertEquals(9, firstLine.syllables.size)
        assertEquals("I ", firstLine.syllables[0].content)
        assertEquals(0, firstLine.syllables[0].start)
        assertEquals(214, firstLine.syllables[0].end)
        assertEquals("promise ", firstLine.syllables[1].content)
        assertEquals(214, firstLine.syllables[1].start)
        assertEquals(559, firstLine.syllables[1].end)

        // 验证第二行
        val secondLine = data.lines[1] as KaraokeLine
        assertEquals(8, secondLine.syllables.size)
        assertEquals("I ", secondLine.syllables[0].content)
        assertEquals(3476, secondLine.syllables[0].start)
        assertEquals(3661, secondLine.syllables[0].end)
    }

    @Test
    fun testParseWithDifferentAttributes() {
        val lys = listOf(
            "[2]Start (0,100)aligned (100,200)line(200,300)",
            "[4]End (400,100)aligned (500,200)line(600,300)",
            "[8]Background (800,100)vocals(900,200)"
        )
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(3, data.lines.size)

        // 验证对齐方式
        assertEquals(KaraokeAlignment.End, (data.lines[0] as KaraokeLine).alignment)
        assertEquals(KaraokeAlignment.Start, (data.lines[1] as KaraokeLine).alignment)
        assertEquals(KaraokeAlignment.End, (data.lines[2] as KaraokeLine).alignment)

        // 验证伴唱标识
        assertFalse((data.lines[0] as KaraokeLine).isAccompaniment)
        assertFalse((data.lines[1] as KaraokeLine).isAccompaniment)
        assertTrue((data.lines[2] as KaraokeLine).isAccompaniment)
    }

    @Test
    fun testParseWithoutAttributes() {
        val lys = listOf("Hello (0,100)world(100,200)")
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(1, data.lines.size)
        val line = data.lines[0] as KaraokeLine
        assertEquals(2, line.syllables.size)
        assertEquals(KaraokeAlignment.Start, line.alignment)
        assertFalse(line.isAccompaniment)
    }

    @Test
    fun testParseEmptyLine() {
        val lys = listOf("")
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(0, data.lines.size)
    }

    @Test
    fun testParseWithSpecialCharacters() {
        val lys = listOf("[4]Hello, (0,100)world! (100,200)How (200,150)are (350,100)you?(450,200)")
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(1, data.lines.size)
        val line = data.lines[0] as KaraokeLine
        assertEquals(5, line.syllables.size)
        assertEquals("Hello, ", line.syllables[0].content)
        assertEquals("world! ", line.syllables[1].content)
        assertEquals("How ", line.syllables[2].content)
        assertEquals("are ", line.syllables[3].content)
        assertEquals("you?", line.syllables[4].content)
    }

    @Test
    fun testParseWithMultipleLines() {
        val lys = listOf(
            "[4]First (0,100)line(100,200)",
            "[2]Second (300,150)line(450,250)",
            "[5]Third (700,100)line(800,300)"
        )
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(3, data.lines.size)

        // 验证每行的时间范围
        assertEquals(0, data.lines[0].start)
        assertEquals(300, data.lines[0].end)
        assertEquals(300, data.lines[1].start)
        assertEquals(700, data.lines[1].end)
        assertEquals(700, data.lines[2].start)
        assertEquals(1100, data.lines[2].end)
    }

    @Test
    fun testParseWithAccompanimentAttributes() {
        val lys = listOf(
            "[6]Background (0,100)vocals(100,200)",
            "[7]Harmony (300,150)part(450,250)"
        )
        val data = LyricifySyllableParser.parse(lys)

        assertEquals(2, data.lines.size)
        assertTrue((data.lines[0] as KaraokeLine).isAccompaniment)
        assertTrue((data.lines[1] as KaraokeLine).isAccompaniment)
    }
}
