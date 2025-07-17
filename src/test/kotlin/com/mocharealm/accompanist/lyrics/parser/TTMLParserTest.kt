package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import kotlin.test.Test
import kotlin.test.assertEquals

class TTMLParserTest {
    @Test
    fun testMeParseResult() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata xmlns="">
                    <ttm:agent type="singer" xml:id="v1"/>
                    <ttm:agent type="person" xml:id="v2"/>
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
                                <span ttm:role="x-translation" xml:lang="zh-CN">看见过</span>
                            </span>
                            <span ttm:role="x-translation" xml:lang="zh-CN">我从未</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val parser = TTMLParser()
        val result = parser.parse(listOf(ttml))

        // 验证解析出的行数
        assertEquals(3, result.lines.size)

        // 验证第一行的对齐方式(第一个agent v1)应该是左对齐
        assertEquals(KaraokeAlignment.Start, (result.lines[0] as KaraokeLine).alignment)

        // 验证第二行的对齐方式(第二个agent v2)应该是右对齐
        assertEquals(KaraokeAlignment.End, (result.lines[1] as KaraokeLine).alignment)

        // 验证背景音节的对齐方式应该继承自父元素的agent(v2 右对齐)
        assertEquals(KaraokeAlignment.End, (result.lines[2] as KaraokeLine).alignment)
        
        // 验证第一行翻译
        val firstLine = result.lines[0] as KaraokeLine
        assertEquals("确信我就是这世间的独一无二", firstLine.translation?.trim())
        
        // 验证第二行（主歌词）翻译
        val secondLine = result.lines[1] as KaraokeLine
        assertEquals("我从未", secondLine.translation?.trim())
        
        // 验证背景音节被正确解析，包含其独立的翻译
        val bgLine = result.lines[2] as KaraokeLine
        assertEquals(2, bgLine.syllables.size) // "wanna", "see"
        assertEquals("wanna see", bgLine.syllables.joinToString(" ") { it.content.trim() })
        assertEquals("看见过", bgLine.translation?.trim()) // 背景音节有自己的翻译
    }

    @Test
    fun testNestedTranslationStructure() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body>
                    <div>
                        <p begin="00:00.100" end="00:03.000">
                            <span begin="00:00.100" end="00:00.500">Main </span>
                            <span begin="00:00.500" end="00:01.000">vocals</span>
                            <span ttm:role="x-bg" begin="00:01.500" end="00:02.500">
                                <span begin="00:01.500" end="00:02.000">background</span>
                                <span begin="00:02.000" end="00:02.500">harmony</span>
                                <span ttm:role="x-translation" xml:lang="zh-CN">背景和声</span>
                            </span>
                            <span ttm:role="x-translation" xml:lang="zh-CN">主歌声</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val parser = TTMLParser()
        val result = parser.parse(listOf(ttml))

        assertEquals(2, result.lines.size)
        
        // 主歌词行应该有p级别的翻译
        val mainLine = result.lines[0] as KaraokeLine
        assertEquals("主歌声", mainLine.translation)
        assertEquals("Main vocals", mainLine.syllables.joinToString(" ") { it.content.trim() })
        
        // 背景音节行应该有自己的翻译
        val bgLine = result.lines[1] as KaraokeLine
        assertEquals("背景和声", bgLine.translation)
        assertEquals("background harmony", bgLine.syllables.joinToString(" ") { it.content.trim() })
    }
}
