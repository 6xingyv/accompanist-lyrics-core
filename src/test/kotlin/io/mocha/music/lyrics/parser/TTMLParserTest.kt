package io.mocha.music.lyrics.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.mocha.music.lyrics.model.karaoke.KaraokeAlignment

class TTMLParserTest {
    @Test
    fun testMeParseResult() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata xmlns="">
                    <ttm:agent type="person" xml:id="v1"/>
                    <ttm:agent type="other" xml:id="v2"/>
                </metadata></head>
                <body>
                    <div xmlns="">
                        <p begin="00:00.130" end="00:02.820" ttm:agent="v1">
                            <span begin="00:00.130" end="00:00.230">I </span>
                            <span begin="00:00.230" end="00:00.450">promise </span>
                            <span begin="00:00.450" end="00:00.620">that </span>
                            <span ttm:role="x-translation" xml:lang="zh-CN">确信我就是这世间的独一无二</span>
                        </p>
                        <p begin="01:14.650" end="01:17.289" ttm:agent="v2">
                            <span begin="01:14.650" end="01:14.780">I </span>
                            <span begin="01:14.780" end="01:15.010">never </span>
                            <span ttm:role="x-bg" begin="01:15.010" end="01:15.420">
                                <span begin="01:15.010" end="01:15.250">wanna </span>
                                <span begin="01:15.250" end="01:15.420">see </span>
                            </span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val parser = TTMLParser()
        val result = parser.parse(listOf(ttml))

        // 验证解析出的行数
        assertEquals(2, result.lines.size)

        // 验证第一行的对齐方式(v1 = person)
        assertEquals(KaraokeAlignment.Start, result.lines[0].alignment)

        // 验证第二行的对齐方式(v2 = other)
        assertEquals(KaraokeAlignment.End, result.lines[1].alignment)

        // 验证背景音节被正确解析
        val secondLine = result.lines[1]
        assertEquals(4, secondLine.syllables.size) // "I", "never", "wanna", "see"
        assertEquals("I never wanna see", secondLine.syllables.joinToString(" ") { it.content.trim() })
    }
}
