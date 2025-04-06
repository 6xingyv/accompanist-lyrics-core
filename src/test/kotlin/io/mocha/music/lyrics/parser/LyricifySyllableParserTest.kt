package io.mocha.music.lyrics.parser

import kotlin.test.Test

class LyricifySyllableParserTest {
    @Test
    fun testParseLyricifySyllable() {
        val lys = """
            [4]I (0,214)promise (214,345)that (559,185)you'll (744,154)never (898,334)find (1232,202)another (1434,470)like (1904,363)me(2267,658)
            [4]I (3476,185)know (3661,150)that (3811,161)I'm (3972,184)a (4156,155)handful, (4311,672)baby, (4983,672)uh(5655,401)
        """.trimIndent().split("\n")
        val data = LyricifySyllableParser.parse(lys)
        println(data.lines.toString())
    }
}
