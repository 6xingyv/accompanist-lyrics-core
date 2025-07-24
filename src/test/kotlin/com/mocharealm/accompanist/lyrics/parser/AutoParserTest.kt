package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.utils.LyricsFormatGuesser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutoParserTest {

    @Test
    fun testParseLrc() {
        val lrc = """
            [ti:Apt 22]
            [ar:Joesef/Barney Lister]
            [al:Permanent Damage (Explicit)]
            [00:25.50]You're on my mind
            [00:31.38]Sometimes I still wake up thinking you're by my side
        """.trimIndent()
        val result = AutoParser.parse(lrc)
        assertEquals(2, result.lines.size)
    }

    @Test
    fun testParseEnhancedLrc() {
        val enhancedLrc = listOf(
            "[00:29.299]v1:<00:29.299>Baby <00:29.508>we're <00:29.811>far <00:30.992>from <00:31.217>perfect<00:31.817>",
            "[00:29.299]<00:29.299>宝贝 我们并不完美<00:32.310>",
            "[00:32.313]v2:<00:32.313>Oh <00:32.521>I <00:32.704>know <00:32.914>all <00:33.080>about <00:33.265>you<00:33.670>",
            "[00:32.313]<00:32.313>我对你了如指掌<00:33.940>",
            "[bg: <00:33.940>Background <00:34.200>vocals<00:34.500>]",
            "[00:33.940]<00:33.940>背景音<00:34.500>"
        )

        val data = AutoParser.parse(enhancedLrc)

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
    fun testParseTtml() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="00:00.130" end="00:02.820">
                        <span begin="00:00.130" end="00:00.230">I</span> <span begin="00:00.230" end="00:00.450">promise</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val result = AutoParser.parse(ttml)
        assertEquals(1, result.lines.size)
        assertEquals(2, (result.lines[0] as KaraokeLine).syllables.size)
    }

    @Test
    fun testParseLyricifySyllable() {
        val lys =
            "[4]I (0,214)promise (214,345)that (559,185)you'll (744,154)never (898,334)find (1232,202)another (1434,470)like (1904,363)me(2267,658)"
        val result = AutoParser.parse(lys)
        assertEquals(1, result.lines.size)
        assertEquals(9, (result.lines[0] as KaraokeLine).syllables.size)
    }

    @Test
    fun testParseUnknownFormat() {
        val unknown = "just some random text"
        val result = AutoParser.parse(unknown)
        assertEquals(0, result.lines.size)
    }

    @Test
    fun testCustomFormatRegistration() {
        val customFormat = LyricsFormatGuesser.LyricsFormat(
            name = "CUSTOM",
            detector = { it.startsWith("CUSTOM_LYRICS:") }
        )

        AutoParser.register(customFormat, object : ILyricsParser {
            override fun parse(lines: List<String>): SyncedLyrics {
                return SyncedLyrics(
                    lines = listOf(
                        KaraokeLine(
                            syllables = listOf(),
                            translation = null,
                            isAccompaniment = false,
                            alignment = com.mocharealm.accompanist.lyrics.model.karaoke.KaraokeAlignment.Start,
                            start = 0,
                            end = 1000
                        )
                    )
                )
            }
        })

        // 3. Test parsing with the custom format
        val customLyrics = "CUSTOM_LYRICS: This is a test."
        val result = AutoParser.parse(customLyrics)

        assertEquals(1, result.lines.size)
        assertNotNull(result.lines.find { it is KaraokeLine })
    }
}