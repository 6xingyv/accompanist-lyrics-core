package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnhancedLrcParserTest {

    @Test
    fun testParseEnhancedLrcWithVoiceSeparation() {
        val lrc = """
            [00:26.503]v1:<00:26.503>This <00:26.711>won't <00:26.894>work <00:27.078>for <00:27.302>you<00:27.591>
            [00:26.503]<00:26.503>你对我无动于衷<00:27.590>
            [bg: <00:27.591>Mind <00:27.806>this <00:27.998>won't <00:28.182>work <00:28.358>for <00:28.543>you<00:28.703>]
            [00:27.591]<00:27.591>无动于衷<00:28.703>
        """.trimIndent().split("\n")
        
        val data = EnhancedLrcParser.parse(lrc)
        
        assertEquals(2, data.lines.size)
        // 第一行：主唱（合并翻译）
        val firstLine = data.lines[0] as KaraokeLine

        assertEquals(5, firstLine.syllables.size)
        assertEquals("This ", firstLine.syllables[0].content)
        assertEquals(26503, firstLine.syllables[0].start)
        assertEquals(26711, firstLine.syllables[0].end)
        assertEquals("you", firstLine.syllables[4].content)
        assertEquals(27302, firstLine.syllables[4].start)
        assertEquals(27591, firstLine.syllables[4].end)
        assertEquals("你对我无动于衷", firstLine.translation)
        assertFalse(firstLine.isAccompaniment)
        assertEquals(KaraokeAlignment.Start, firstLine.alignment)
        // 第二行：伴奏（合并翻译）
        val secondLine = data.lines[1] as KaraokeLine
        assertEquals(6, secondLine.syllables.size)
        assertEquals("Mind ", secondLine.syllables[0].content)
        assertEquals(27591, secondLine.syllables[0].start)
        assertEquals(27806, secondLine.syllables[0].end)
        assertEquals("you", secondLine.syllables[5].content)
        assertEquals(28543, secondLine.syllables[5].start)
        assertEquals(28703, secondLine.syllables[5].end)
        assertEquals("无动于衷", secondLine.translation)
        assertTrue(secondLine.isAccompaniment)
        assertEquals(KaraokeAlignment.Start, secondLine.alignment)
    }

    @Test
    fun testParseWithDifferentVoiceTypes() {
        val lrc = listOf(
            "[00:29.299]v1:<00:29.299>Baby <00:29.508>we're <00:29.811>far <00:30.992>from <00:31.217>perfect<00:31.817>",
            "[00:29.299]<00:29.299>宝贝 我们并不完美<00:32.310>",
            "[00:32.313]v2:<00:32.313>Oh <00:32.521>I <00:32.704>know <00:32.914>all <00:33.080>about <00:33.265>you<00:33.670>",
            "[00:32.313]<00:32.313>我对你了如指掌<00:33.940>",
            "[bg: <00:33.940>Background <00:34.200>vocals<00:34.500>]",
            "[00:33.940]<00:33.940>背景音<00:34.500>"
        )
        
        val data = EnhancedLrcParser.parse(lrc)
        
        assertEquals(3, data.lines.size)
        
        // 验证v1对齐方式
        val v1Line = data.lines[0] as KaraokeLine
        assertEquals(KaraokeAlignment.Start, v1Line.alignment)
        assertFalse(v1Line.isAccompaniment)
        assertEquals("宝贝 我们并不完美", v1Line.translation)
        
        // 验证v2对齐方式
        val v2Line = data.lines[1] as KaraokeLine
        assertEquals(KaraokeAlignment.End, v2Line.alignment)
        assertFalse(v2Line.isAccompaniment)
        assertEquals("我对你了如指掌", v2Line.translation)
        
        // 验证bg跟随v2的对齐方式
        val bgLine = data.lines[2] as KaraokeLine
        assertEquals(KaraokeAlignment.End, bgLine.alignment) // 应该跟随v2的End对齐
        assertTrue(bgLine.isAccompaniment)
        assertEquals("背景音", bgLine.translation)
    }

    @Test
    fun testParseWithoutInlineTimestamps() {
        val lrc = listOf(
            "[00:30.000]v1:Simple line without timestamps",
            "[00:30.000]简单的一行"
        )
        
        val data = EnhancedLrcParser.parse(lrc)
        
        assertEquals(1, data.lines.size)
        
        val firstLine = data.lines[0] as KaraokeLine
        assertEquals(1, firstLine.syllables.size)
        assertEquals("Simple line without timestamps", firstLine.syllables[0].content)
        assertEquals("简单的一行", firstLine.translation)
        assertFalse(firstLine.isAccompaniment)
    }

    @Test
    fun testParseMixedFormat() {
        val lrc = listOf(
            "[00:25.50]Regular LRC line",
            "[00:25.50]普通LRC行",
            "[00:30.000]v1:<00:30.000>Enhanced <00:30.500>line<00:31.000>",
            "[00:30.000]<00:30.000>增强格式行<00:31.000>",
            "[00:35.000]Another regular line"
        )
        
        val data = EnhancedLrcParser.parse(lrc)
        
        assertEquals(3, data.lines.size)
        
        // 第一行：普通LRC
        val firstLine = data.lines[0] as KaraokeLine
        assertEquals(1, firstLine.syllables.size)
        assertEquals("Regular LRC line", firstLine.syllables[0].content)
        assertEquals("普通LRC行", firstLine.translation)
        
        // 第二行：增强格式
        val secondLine = data.lines[1] as KaraokeLine
        assertEquals(2, secondLine.syllables.size)
        assertEquals("Enhanced ", secondLine.syllables[0].content)
        assertEquals("增强格式行", secondLine.translation)
        
        // 第三行：普通LRC（无翻译）
        val thirdLine = data.lines[2] as KaraokeLine
        assertEquals(1, thirdLine.syllables.size)
        assertEquals("Another regular line", thirdLine.syllables[0].content)
        assertEquals(null, thirdLine.translation)
    }
}
