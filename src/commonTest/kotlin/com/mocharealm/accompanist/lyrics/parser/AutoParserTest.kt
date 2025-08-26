package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser
import com.mocharealm.accompanist.lyrics.core.utils.LyricsFormatGuesser
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
        val result = AutoParser.Builder().build().parse(lrc)
        assertTrue {
            result.lines.isNotEmpty() && result.lines.all { it is SyncedLine }
        }
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

        val data = AutoParser.Builder().build().parse(enhancedLrc)

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
        val result = AutoParser.Builder().build().parse(ttml)
        assertEquals(1, result.lines.size)
        assertEquals(2, (result.lines[0] as KaraokeLine).syllables.size)
    }

    @Test
    fun testParseLyricifySyllable() {
        val lys =
            "[4]I (0,214)promise (214,345)that (559,185)you'll (744,154)never (898,334)find (1232,202)another (1434,470)like (1904,363)me(2267,658)"
        val result = AutoParser.Builder().build().parse(lys)
        assertEquals(1, result.lines.size)
        assertEquals(9, (result.lines[0] as KaraokeLine).syllables.size)
    }

    @Test
    fun testParseUnknownFormat() {
        val unknown = "just some random text"
        val result = AutoParser.Builder().build().parse(unknown)
        assertEquals(0, result.lines.size)
    }

    @Test
    fun testCustomFormatRegistration() {
        val customFormat = LyricsFormatGuesser.LyricsFormat(
            name = "CUSTOM",
            detector = { it.startsWith("CUSTOM_LYRICS:") }
        )

        val parser = AutoParser.Builder().withFormat(customFormat, object : ILyricsParser {
            override fun parse(lines: List<String>): SyncedLyrics {
                return SyncedLyrics(
                    lines = listOf(
                        KaraokeLine(
                            syllables = listOf(),
                            translation = null,
                            isAccompaniment = false,
                            alignment = KaraokeAlignment.Start,
                            start = 0,
                            end = 1000
                        )
                    )
                )
            }
        }).build()

        // 3. Test parsing with the custom format
        val customLyrics = "CUSTOM_LYRICS: This is a commonTest."
        val result = parser.parse(customLyrics)

        assertEquals(1, result.lines.size)
        assertNotNull(result.lines.find { it is KaraokeLine })
    }

    @Test
    fun testParseKugouKrc(){
        val krc = """
[id:$00000000]
[ar:三Z-STUDIO/HOYO-MiX]
[ti:覆灭重生 Come Alive]
[by:]
[hash:07cec452facb62cf627d50928d32b9bb]
[al:绝区零-覆灭重生 Come Alive]
[sign:]
[qq:]
[total:0]
[offset:0]
[language:eyJjb250ZW50IjpbeyJsYW5ndWFnZSI6MCwibHlyaWNDb250ZW50IjpbWyIgIl0sWyIgIl0sWyIgIl0sWyIgIl0sWyIgIl0sWyIgIl0sWyIgIl0sWyIgIl0sWyJcdThGRDhcdTgwRkQiXSxbIlx1NjJCNVx1NjI5N1x1NTkxQVx1NEU0NVx1RkYxRiJdLFsiXHU3NzNDXHU3NzBCXHU0RTAwXHU1MjA3XHU1RDI5XHU1ODRDIl0sWyJcdTRFMDdcdTcyNjlcdTZEODhcdTkwMUQiXSxbIlx1OTZCRVx1OTA1M1x1OEZERVx1NUI1OFx1NTcyOFx1NzY4NFx1NzVENVx1OEZGOSJdLFsiXHU0RTVGXHU3RUM4XHU1RjUyXHU0RThFXHU4NjVBXHU2NUUwXHVGRjFGIl0sWyJcdThGRDhcdTg5ODEiXSxbIlx1NkM4OVx1NkNBNlx1NTkxQVx1NEU0NVx1RkYxRiJdLFsiXHU2MjREXHU0RjFBXHU1RTYxXHU3MTM2XHU5MTkyXHU2MDlGIl0sWyJcdTY2RkVcdTdFQ0ZcdTc2ODRcdTY4QTZcdTVERjJcdTZFMTBcdTg4NENcdTZFMTBcdThGREMiXSxbIlx1ODk4MVx1OTY3N1x1ODFGM1x1NEY1NVx1NzlDRFx1N0VERFx1NTg4MyJdLFsiXHU2MjREXHU4MEZEXHU2NzA5XHU3ODM0XHU5MURDXHU2Qzg5XHU4MjFGXHU3Njg0XHU1MkM3XHU2QzE0XHVGRjFGIl0sWyJcdThCRTVcdTRGNTVcdTUzQkJcdTRGNTVcdTRFQ0UiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIiAiXSxbIlx1N0VDOFx1N0E3NiJdLFsiXHU1RkMzXHU0RTJEXHU3Njg0XHU3MDZCXHU0RUNFXHU2NzJBXHU3MTg0XHU3MDZEIl0sWyJcdTg5ODZcdTcwNkRcdTkxQ0RcdTc1MUYiXSxbIlx1NjIxMVx1NzBCOVx1NzFDM1x1OTFDRVx1NzA2Qlx1NTFCMlx1NjU2M1x1OUVEMVx1NTkxQyJdLFsiXHU1NzI4XHU2NUUwXHU1QzNEXHU3Njg0XHU2NzJBXHU3N0U1XHU0RTJEXHU2MjdFXHU1QkZCXHU4MUVBXHU1REYxIl0sWyJcdThGRkRcdTkwMTBcdTc3NDBcdTY2MUZcdTVCQkZcdTU0OENcdTUxNDkiXSxbIlx1NzUyOFx1NUMzRFx1NTE2OFx1NTI5Qlx1NTk1NFx1OEREMVx1OEZGRFx1NUJGQiJdLFsiXHU5MEEzXHU4RkY3XHU1OTMxXHU1NzI4XHU3QTdBXHU2RDFFXHU5MUNDXHU3Njg0XHU3NzFGXHU3NDA2Il0sWyJcdTU0RUFcdTYwMTVcdTUyNERcdTkwMTRcdTgzNDZcdTY4RDhcdTZGMkJcdTZGMkIiXSxbIlx1NjIxMVx1NEVFQ1x1N0VDOFx1NUMwNlx1NTFCMlx1NzgzNFx1NEUwMFx1NTIwN1x1OTYzQlx1Nzg4RFx1MDBBMFx1NjIxMVx1NzY4NFx1NEYxOVx1NEYzNCJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiICJdLFsiXHU4OTg2XHU3MDZEXHU5MUNEXHU3NTFGIl0sWyJcdTYyMTFcdTcwQjlcdTcxQzNcdTkxQ0VcdTcwNkJcdTUxQjJcdTY1NjNcdTlFRDFcdTU5MUMiXSxbIlx1NTcyOFx1NjVFMFx1NUMzRFx1NzY4NFx1NjcyQVx1NzdFNVx1NEUyRFx1NjI3RVx1NUJGQlx1ODFFQVx1NURGMSJdLFsiXHU4RkZEXHU5MDEwXHU3NzQwXHU2NjFGXHU1QkJGXHU1NDhDXHU1MTQ5Il0sWyJcdTc1MjhcdTVDM0RcdTUxNjhcdTUyOUJcdTU5NTRcdThERDFcdThGRkRcdTVCRkIiXSxbIlx1OTBBM1x1OEZGN1x1NTkzMVx1NTcyOFx1N0E3QVx1NkQxRVx1OTFDQ1x1NzY4NFx1NzcxRlx1NzQwNiJdLFsiXHU1NEVBXHU2MDE1XHU1MjREXHU5MDE0XHU4MzQ2XHU2OEQ4XHU2RjJCXHU2RjJCIl0sWyJcdTYyMTFcdTRFRUNcdTdFQzhcdTVDMDZcdTUxQjJcdTc4MzRcdTRFMDBcdTUyMDdcdTk2M0JcdTc4OERcdTAwQTBcdTYyMTFcdTc2ODRcdTRGMTlcdTRGMzQiXSxbIlx1N0VFN1x1N0VFRFx1NTk1NFx1OEREMVx1MDBBMFx1N0E3Rlx1OEQ4QVx1OThDRVx1NjZCNFx1MDBBMFx1NTcyOFx1OThDRVx1NzczQ1x1NEU0Qlx1NEUyRFx1NjI3RVx1NTIzMFx1NUI4MVx1OTc1OSJdLFsiICJdLFsiXHU3RUU3XHU3RUVEXHU1OTU0XHU4REQxXHUwMEEwXHU3QTdGXHU4RDhBXHU5OENFXHU2NkI0XHUwMEEwXHU1NzI4XHU5OENFXHU3NzNDXHU0RTRCXHU0RTJEXHU2MjdFXHU1MjMwXHU1QjgxXHU5NzU5Il0sWyIgIl0sWyJcdTdFRTdcdTdFRURcdTU5NTRcdThERDFcdTAwQTBcdTdBN0ZcdThEOEFcdTk4Q0VcdTY2QjRcdTAwQTBcdTU3MjhcdTk4Q0VcdTc3M0NcdTRFNEJcdTRFMkRcdTYyN0VcdTUyMzBcdTVCODFcdTk3NTkiXSxbIiAiXSxbIlx1NEUwRFx1ODk4MVx1NTA1Q1x1NkI2Mlx1ODExQVx1NkI2NSJdLFsiICJdLFsiXHU2MjExXHU0RUVDXHU4OTg2XHU3MDZEXHU5MUNEXHU3NTFGIl0sWyJcdTU3MjhcdTY1RTBcdTU4RjBcdTY1RTBcdTVGNzFcdTc2ODRcdTlFRDFcdTU5MUMiXSxbIlx1NzBDOFx1NzA2Qlx1NzFDRVx1NTM5RiJdLFsiXHU0RTBEXHU1M0VGXHU5NjNCXHU2MzIxIl0sWyJcdTVDMzFcdTdCOTdcdTkwNkRcdTRFQkFcdThGN0JcdTg5QzYiXSxbIlx1NUMzMVx1N0I5N1x1NkJFQlx1NjVFMFx1ODBEQ1x1N0I5N1x1MDBBMFx1NEU1Rlx1ODk4MVx1N0VERFx1NTczMFx1NTNDRFx1NTFGQiJdLFsiXHU4OTg2XHU3MDZEXHU5MUNEXHU3NTFGIl0sWyJcdTYyMTFcdTcwQjlcdTcxQzNcdTkxQ0VcdTcwNkJcdTUxQjJcdTY1NjNcdTlFRDFcdTU5MUMiXSxbIlx1NTcyOFx1NjVFMFx1NUMzRFx1NzY4NFx1NjcyQVx1NzdFNVx1NEUyRFx1NjI3RVx1NUJGQlx1ODFFQVx1NURGMSJdLFsiXHU4RkZEXHU5MDEwXHU3NzQwXHU2NjFGXHU1QkJGXHU1NDhDXHU1MTQ5Il0sWyJcdTc1MjhcdTVDM0RcdTUxNjhcdTUyOUJcdTU5NTRcdThERDFcdThGRkRcdTVCRkIiXSxbIlx1OTBBM1x1OEZGN1x1NTkzMVx1NTcyOFx1N0E3QVx1NkQxRVx1OTFDQ1x1NzY4NFx1NzcxRlx1NzQwNiJdLFsiXHU1NEVBXHU2MDE1XHU1MjREXHU5MDE0XHU4MzQ2XHU2OEQ4XHU2RjJCXHU2RjJCIl0sWyJcdTU0RUFcdTYwMTVcdTVGNTJcdThERUZcdTVFMENcdTY3MUJcdTZFM0FcdTgzMkIiXSxbIlx1NTcyOFx1NTkzMVx1NUU4Rlx1NzY4NFx1N0E3QVx1NkQxRVx1OTFDQ1x1OTFDRFx1NjVCMFx1NUYwMFx1NTlDQiJdLFsiXHU2MjExXHU0RUVDXHU3RUM4XHU1QzA2XHU1MUIyXHU3ODM0XHU0RTAwXHU1MjA3XHU5NjNCXHU3ODhEXHUwMEEwXHU2MjExXHU3Njg0XHU0RjE5XHU0RjM0Il0sWyJcdTU5NTRcdThERDFcdTU0MjciXSxbIlx1NjIxMVx1NEVFQ1x1N0VDOFx1NUMwNlx1NTFCMlx1NzgzNFx1OTYzQlx1Nzg4RCJdLFsiXHU5MUNBXHU2NTNFXHU1NDI3Il0sWyJcdTYyMTFcdTRFRUNcdTdFQzhcdTVDMDZcdThENzBcdTUxRkFcdThGRjdcdTYwRDgiXV0sInR5cGUiOjF9XSwidmVyc2lvbiI6MX0=]
[0,728]<0,60,0>覆<60,60,0>灭<120,60,0>重<180,60,0>生<240,60,0> <300,60,0>Come <360,60,0>Alive - <420,60,0>三<480,60,0>Z-<540,60,0>STUDIO/<600,60,0>HOYO-<660,60,0>MiX
[729,364]<0,60,0>制<60,60,0>作<120,60,0>人<180,60,0>：<240,60,0>雷<300,60,0>声
[1094,424]<0,60,0>作<60,60,0>词<120,60,0>：<180,60,0>Philip <240,60,0>Strand/<300,60,0>雷<360,60,0>声
[1519,424]<0,60,0>作<60,60,0>曲<120,60,0>：<180,60,0>Philip <240,60,0>Strand/<300,60,0>雷<360,60,0>声
[1944,303]<0,60,0>编<60,60,0>曲<120,60,0>：<180,60,0>雷<240,60,0>声
[2248,485]<0,60,0>吉<60,60,0>他<120,60,0>贝<180,60,0>斯<240,60,0>鼓<300,60,0>：<360,60,0>因<420,60,0>可
[2734,364]<0,60,0>混<60,60,0>音<120,60,0>：<180,60,0>郑<240,60,0>仕<300,60,0>伟
[3098,364]<0,60,0>母<60,60,0>带<120,60,0>：<180,60,0>郑<240,60,0>仕<300,60,0>伟
[3463,720]<0,352,0>How <352,368,0>long
[4527,1352]<0,207,0>Can <207,225,0>we <432,328,0>stay <760,592,0>awake
[6111,1079]<0,184,0>Watch <184,167,0>it <351,345,0>all <696,383,0>fall
[7190,1737]<0,209,0>Yeah <209,167,0>we <376,161,0>watch <537,184,0>it <721,384,0>all <1105,632,0>break
[9398,841]<0,361,0>How <361,480,0>long
[10527,2659]<0,200,0>Till <200,208,0>we <408,290,0>all <698,760,0>fade <1458,1201,0>away
[15441,801]<0,320,0>How <320,481,0>deep
[16522,1496]<0,200,0>Are <200,208,0>we <408,367,0>gonna <775,721,0>sink
[18378,784]<0,168,0>Before <168,240,0>we <408,192,0>know <600,184,0>it
[19162,1847]<0,215,0>We've <215,201,0>been <416,321,0>losing <737,399,0>our <1136,711,0>dreams
[21394,1048]<0,400,0>How <400,648,0>deep
[22442,2736]<0,255,0>Before <255,218,0>we <473,376,0>all <849,687,0>fade <1536,1200,0>away
[26162,1087]<0,184,0>So <184,177,0>where <361,183,0>do <544,128,0>we <672,415,0>go
[27249,1250]<0,161,0>不<161,183,0>知<344,216,0>道<560,203,0>从<763,173,0>哪<936,118,0>天<1054,196,0>起
[28723,1625]<0,168,0>墙<168,192,0>上<360,184,0>的<544,184,0>电<728,220,0>视<948,176,0>机<1124,501,0>里
[30348,198]<0,33,0>(<33,33,0>And <66,35,0>where <101,32,0>is <133,32,0>your <165,33,0>heart)
[30546,2104]<0,34,0>播<34,106,0>报<140,144,0>有<284,184,0>人<468,183,0>迷<651,177,0>失<828,168,0>在<996,185,0>空<1181,207,0>洞<1388,215,0>陷<1603,501,0>阱
[32650,165]<0,33,0>(<33,32,0>Show <65,33,0>me <98,34,0>the <132,33,0>light)
[32815,963]<0,32,0>无<32,34,0>法<66,34,0>忍<100,343,0>受<443,152,0>的<595,175,0>沉<770,193,0>默
[33778,1519]<0,183,0>代<183,176,0>理<359,217,0>委<576,335,0>托<911,193,0>的<1104,207,0>生<1311,208,0>活
[35297,1763]<0,184,0>对<184,185,0>抗<369,199,0>堕<568,335,0>落<903,176,0>的<1079,184,0>困<1263,500,0>兽
[37060,132]<0,33,0>(<33,33,0>Over <66,35,0>and <101,31,0>over)
[37192,1367]<0,32,0>找<32,101,0>寻<133,136,0>逃<269,212,0>脱<481,208,0>的<689,177,0>绳<866,501,0>索
[38559,130]<0,31,0>(<31,33,0>Over <64,33,0>and <97,33,0>over)
[38689,2537]<0,33,0>向<33,33,0>着<66,33,0>吹<99,414,0>过<513,193,0>冷<706,182,0>冽<888,209,0>的<1097,192,0>风<1289,176,0>它<1465,176,0>带<1641,184,0>走<1825,184,0>我<2009,231,0>的<2240,297,0>manic
[41466,2036]<0,225,0>抛<225,166,0>去<391,184,0>狂<575,185,0>妄<760,184,0>自<944,160,0>大<1104,191,0>的<1295,177,0>偏<1472,227,0>执<1699,1,0>的<1699,176,0>想<1875,161,0>法
[43502,799]<0,136,0>和<136,160,0>现<296,160,0>实<456,151,0>对<607,192,0>垒
[44518,1954]<0,135,0>越<135,177,0>过<312,192,0>这<504,175,0>台<679,123,0>阶<802,143,0> <945,160,0>去<1105,167,0>寻<1272,185,0>找<1457,192,0>那<1649,160,0>规<1809,145,0>则
[46713,880]<0,215,0>It's <215,361,0>like <576,304,0>magic
[47881,1664]<0,192,0>举<192,129,0>起<321,143,0>火<464,136,0>把<600,160,0>点<760,152,0>燃<912,168,0>驱<1080,199,0>散<1279,193,0>晦<1472,192,0>涩
[49721,1279]<0,208,0>All <208,144,0>I <352,391,0>wanna <743,536,0>say
[51231,1889]<0,161,0>We <161,162,0>all <323,166,0>have <489,152,0>the <641,287,0>same <928,961,0>desire
[53120,2128]<0,199,0>I <199,441,0>come <640,1488,0>alive
[55576,3515]<0,168,0>Burn <168,306,0>in <474,224,0>the <698,384,0>night <1082,336,0>like <1418,2097,0>wildfire
[59378,1890]<0,175,0>Find <175,137,0>me <312,160,0>in <472,200,0>the <672,329,0>great <1001,889,0>unknown
[61268,2873]<0,232,0>I'm <232,992,0>following <1224,369,0>the <1593,1280,0>stars
[64990,3695]<0,279,0>Swear <279,368,0>I <647,424,0>will <1071,352,0>run <1423,175,0>until <1598,215,0>I <1813,201,0>run <2014,192,0>out <2206,192,0>of <2398,305,0>air <2703,209,0>in <2912,351,0>my <3263,432,0>lungs
[68988,3414]<0,384,0>Fighting <384,233,0>for <617,168,0>the <785,225,0>ones <1010,185,0>that <1195,198,0>are <1393,292,0>lost <1685,369,0>in <2054,231,0>the <2285,1129,0>hollow
[72402,4635]<0,193,0>I <193,543,0>know <736,239,0>it's <975,497,0>hard <1472,256,0>to <1728,449,0>see <2177,280,0>it <2457,2178,0>end
[78430,3818]<0,360,0>We're <360,744,0>gonna <1104,335,0>make <1439,193,0>it <1632,368,0>out <2000,391,0>my <2391,1427,0>friend
[82781,1128]<0,138,0>成<138,159,0>长<297,176,0>的<473,192,0>所<665,463,0>谓
[84206,1218]<0,168,0>擦<168,216,0>干<384,215,0>那<599,177,0>眼<776,442,0>泪
[85678,2304]<0,209,0>不<209,167,0>待<376,177,0>见<553,167,0>的<720,144,0>看<864,152,0>法<1016,159,0>只<1175,168,0>是<1343,169,0>生<1512,144,0>活<1656,144,0>的<1800,160,0>点<1960,344,0>缀
[87982,2048]<0,144,0>我<144,184,0>看<328,183,0>过<511,193,0>街<704,184,0>头<888,176,0>泛<1064,184,0>黄<1248,135,0>被<1383,153,0>涂<1536,152,0>鸦<1688,128,0>的<1816,120,0>墙<1936,112,0>上
[90030,683]<0,136,0>画<136,136,0>着<272,128,0>漫<400,26,0>天<426,143,0>星<569,114,0>斗
[90945,1609]<0,144,0>也<144,136,0>感<280,144,0>受<424,192,0>过<616,136,0>随<752,135,0>便<887,128,0>潦<1015,44,0>草<1059,118,0>几<1177,184,0>行<1361,135,0>的<1496,112,0>谎<1608,1,0>言
[92554,1417]<0,120,0>抛<120,112,0>出<232,103,0>就<335,105,0>可<440,144,0>以<584,128,0>变<712,112,0>成<824,128,0>锋<952,152,0>利<1104,98,0>匕<1202,215,0>首
[93971,2010]<0,161,0>历<161,208,0>经<369,216,0>着<585,183,0>生<768,160,0>离<928,127,0>死<1055,113,0>别<1168,120,0>存<1288,113,0>亡<1401,119,0>的<1520,128,0>困<1648,33,0>兽<1681,168,0>之<1849,161,0>斗
[96206,1137]<0,184,0>管<184,192,0>它<376,200,0>能<576,191,0>不<767,193,0>能<960,177,0>活
[97574,1566]<0,169,0>全<169,176,0>力<345,160,0>以<505,129,0>赴<634,1,0>地<903,152,0>支<1055,184,0>援<1239,152,0>朋<1391,175,0>友
[99285,1034]<0,152,0>哪<152,192,0>怕<344,192,0>它<536,175,0>侵<711,155,0>蚀<866,168,0>我
[100519,2272]<0,168,0>支<168,200,0>离<368,175,0>破<543,144,0>碎<687,153,0>的<840,136,0>身<976,184,0>体<1160,160,0>变<1320,192,0>得<1512,184,0>不<1696,191,0>能<1887,177,0>掌<2064,208,0>控
[102974,385]<0,195,0>哪<195,190,0>怕
[103543,2255]<0,168,0>面<168,184,0>目<352,176,0>全<528,136,0>非<664,152,0>的<816,152,0>身<968,184,0>体<1152,177,0>变<1329,191,0>得<1520,168,0>不<1688,176,0>能<1864,168,0>掌<2032,223,0>控
[105983,541]<0,168,0>管<168,192,0>他<360,181,0>的
[106524,1921]<0,335,0>我<335,362,0>也<697,391,0>绝<1088,256,0>不<1344,344,0>退<1688,233,0>让
[108445,2209]<0,329,0>I <329,448,0>come <777,1432,0>alive
[111062,3721]<0,231,0>Burn <231,305,0>in <536,216,0>the <752,360,0>night <1112,376,0>like <1488,2233,0>wildfire
[114783,2043]<0,192,0>Find <192,200,0>me <392,168,0>in <560,224,0>the <784,331,0>great <1115,928,0>unknown
[116826,3027]<0,201,0>I'm <201,1015,0>following <1216,360,0>the <1576,1451,0>stars
[120373,3848]<0,425,0>Swear <425,375,0>I <800,377,0>will <1177,335,0>run <1512,200,0>until <1712,208,0>I <1920,289,0>run <2209,119,0>out <2328,176,0>of <2504,384,0>air <2888,256,0>in <3144,232,0>my <3376,472,0>lungs
[124540,3367]<0,345,0>Fighting <345,208,0>for <553,192,0>the <745,240,0>ones <985,144,0>that <1129,208,0>are <1337,306,0>lost <1643,320,0>in <1963,240,0>the <2203,1164,0>hollow
[127907,4630]<0,224,0>I <224,496,0>know <720,224,0>it's <944,532,0>hard <1476,240,0>to <1716,472,0>see <2188,288,0>it <2476,2154,0>end
[133953,5476]<0,360,0>We're <360,693,0>gonna <1053,362,0>make <1415,366,0>it <1781,195,0>out <1976,397,0>my <2373,3103,0>friend
[142910,2716]<0,160,0>You <160,161,0>better <321,208,0>run <529,209,0>you <738,382,0>better <1120,230,0>hide <1350,377,0>inside <1727,206,0>the <1933,186,0>eye <2119,174,0>of <2293,193,0>the <2486,230,0>storm
[145626,3112]<0,200,0>哪<200,224,0>怕<424,225,0>我<649,191,0>身<840,184,0>体<1024,184,0>变<1208,176,0>得<1384,200,0>面<1584,192,0>目<1776,199,0>全<1975,209,0>非<2184,199,0>不<2383,185,0>能<2568,200,0>掌<2768,344,0>控
[148738,2995]<0,200,0>You <200,328,0>better <528,216,0>run <744,223,0>you <967,272,0>better <1239,249,0>hide <1488,352,0>inside <1840,216,0>the <2056,192,0>eye <2248,191,0>of <2439,208,0>the <2647,348,0>storm
[151733,2970]<0,160,0>哪<160,185,0>怕<345,207,0>我<552,208,0>身<760,226,0>体<986,191,0>变<1177,225,0>得<1402,184,0>面<1586,168,0>目<1754,160,0>全<1914,183,0>非<2097,186,0>不<2283,167,0>能<2450,200,0>掌<2650,320,0>控
[154894,3262]<0,154,0>You <154,239,0>better <393,207,0>run <600,201,0>you <801,336,0>better <1137,217,0>hide <1354,344,0>inside <1698,199,0>the <1897,192,0>eye <2089,183,0>of <2272,200,0>the <2472,790,0>storm
[163276,368]<0,368,0>Run
[168411,2510]<0,161,0>You <161,172,0>better <333,201,0>hide <534,184,0>you <718,400,0>better <1118,1392,0>run
[172553,1576]<0,1576,0>RUN
[174377,1890]<0,144,0>We <144,232,0>come <376,424,0>alive <800,344,0>come <1144,746,0>alive
[176643,1690]<0,168,0>In <168,248,0>the <416,179,0>dead <595,160,0>of <755,232,0>the <987,703,0>night
[178613,1434]<0,313,0>Spread <313,367,0>like <680,754,0>wildfire
[180047,1438]<0,367,0>Circle <367,224,0>you <591,847,0>out
[181909,2616]<0,608,0>Call <608,337,0>us <945,198,0>the <1143,1473,0>underdogs
[184757,1931]<0,184,0>Fighting <184,160,0>in <344,152,0>the <496,754,0>silence <1250,369,0>beat <1619,175,0>the <1794,137,0>odds
[186688,1826]<0,144,0>I <144,411,0>come <555,1271,0>alive
[188809,3584]<0,360,0>Burn <360,320,0>in <680,256,0>the <936,384,0>night <1320,376,0>like <1696,1888,0>wildfire
[192809,1879]<0,304,0>Find <304,128,0>me <432,176,0>in <608,168,0>the <776,383,0>great <1159,720,0>unknown
[194688,3307]<0,265,0>I'm <265,1127,0>following <1392,224,0>the <1616,1691,0>stars
[198403,3727]<0,391,0>Swear <391,346,0>I <737,341,0>will <1078,248,0>run <1326,323,0>until <1649,214,0>I <1863,132,0>run <1995,201,0>out <2196,234,0>of <2430,341,0>air <2771,217,0>in <2988,344,0>my <3332,395,0>lungs
[202514,3356]<0,368,0>Fighting <368,184,0>for <552,191,0>the <743,233,0>ones <976,152,0>that <1128,207,0>are <1335,388,0>lost <1723,328,0>in <2051,231,0>the <2282,1074,0>hollow
[205870,4570]<0,206,0>I <206,545,0>know <751,249,0>it's <1000,503,0>hard <1503,202,0>to <1705,406,0>see <2111,393,0>it <2504,2066,0>end
[211912,4447]<0,320,0>I <320,376,0>know <696,392,0>it's <1088,368,0>hard <1456,200,0>to <1656,320,0>see <1976,399,0>it <2375,2072,0>end
[218025,5306]<0,655,0>Inside <655,352,0>the <1007,527,0>hollow <1534,376,0>we <1910,3396,0>begain
[224003,3651]<0,303,0>We're <303,684,0>gonna <987,352,0>make <1339,208,0>it <1547,375,0>out <1922,392,0>my <2314,1337,0>friend
[228086,3008]<0,3008,0>RUN
[231574,2456]<0,199,0>We're <199,371,0>gonna <570,192,0>make <762,250,0>it <1012,346,0>out <1358,463,0>my <1821,323,0>friend
[234030,2612]<0,2612,0>RUN
[237242,3001]<0,209,0>We're <209,327,0>gonna <536,352,0>make <888,361,0>it <1249,448,0>out <1697,400,0>my <2097,592,0>friend
        """.trimIndent()
        val result = AutoParser.Builder().build().parse(krc)
        // 验证解析出的行数 (100行主歌词和翻译)
        assertEquals(100, result.lines.size)

        // 行按时间排序
        val nineLine = result.lines[8] as KaraokeLine
        val tenLine = result.lines[9] as KaraokeLine
        val jiuShiJiuLine = result.lines[99] as KaraokeLine

        // 验证第九行翻译
        assertEquals("还能", nineLine.translation?.trim())

        // 验证第十行翻译
        assertEquals("抵抗多久？", tenLine.translation?.trim())

        // 验证第九行是否解析正确
        assertEquals("How ",nineLine.syllables[0].content)

        // 验证第十行是否解析正确
        assertEquals("Can ",tenLine.syllables[0].content)

        // 验证九十九行翻译
        assertEquals("我们终将走出迷惘",jiuShiJiuLine.translation?.trim())

        //验证九十九行第三个
        assertEquals("make ",jiuShiJiuLine.syllables[2].content)
    }
}