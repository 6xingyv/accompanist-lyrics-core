package io.mocha.music.lyrics

import io.mocha.music.lyrics.parser.LrcParser
import io.mocha.music.lyrics.parser.LyricifySyllableParser
import kotlin.test.Test

class LyricsParserTest {
    @Test
    fun lrcWithoutTranslationTest () {
        val lrc ="""
            [ti:Apt 22]
            [ar:Joesef/Barney Lister]
            [al:Permanent Damage (Explicit)]
            [00:25.50]You're on my mind
            [00:28.25]
            [00:31.38]Sometimes I still wake up thinking you're by my side
            [00:35.36]It's like how it was before
            [00:39.85]
            [02:30.75]I can still feel you by my side
            [02:34.18]
            [02:41.06]I can still feel you by my side
            [02:44.27]
            [02:51.03]I can still feel you by my side
        """.trimIndent().split("\n")
        val data = LrcParser.parse(lrc)
        println(data.lines.toString())
    }

    @Test
    fun lrcWithTranslationTest () {
        val lrc ="""
            [00:39.96]I lean in and you move away
            [00:39.96]我靠在里面，你就搬走
            [00:42.90]
            [00:45.62]But you linger all the same
            [00:45.62]可你依然徘徊不去
            [00:48.19]
            [00:51.43]And I can't help myself but say
            [00:51.43]我情不自禁地说
            [00:54.48]
            [00:56.98]But I know a kiss won't make it right
            [00:56.98]但我知道一个吻无法弥补过错
            [01:02.31]
            [01:02.83]And there's no future left inside
            [01:02.83]心里已经没有未来
            [01:08.59]But I got no false hope in my mind
            [01:08.59]但我心中没有虚假的希望
            [01:14.17]Would you just come home with me tonight
            [01:14.17]今晚你能否与我回家
            [01:19.66]
            [01:20.96]Pieces of my soul
            [01:20.96]我支离破碎的灵魂
            [01:22.68]
            [01:26.40]Are burning in my throat
            [01:26.40]在我的喉咙里燃烧
            [01:28.35]
        """.trimIndent().split("\n")
        val data = LrcParser.parse(lrc)
        println(data.lines.toString())
    }

    @Test
    fun lyricifySyllableTest() {
        val lys = """
            [4]I (0,214)promise (214,345)that (559,185)you'll (744,154)never (898,334)find (1232,202)another (1434,470)like (1904,363)me(2267,658)
            [4]I (3476,185)know (3661,150)that (3811,161)I'm (3972,184)a (4156,155)handful, (4311,672)baby, (4983,672)uh(5655,401)
            [4]I (6113,213)know (6326,237)I (6563,165)never (6728,293)think (7021,339)before (7360,649)I (8009,113)jump(8122,563)
            [4]And (8771,100)you're (8871,115)the (8986,196)kind (9182,178)of (9360,185)guy (9545,345)the (9890,297)ladies (10187,685)want(10872,447)
            [7]And (11407,134)there's (11541,178)a (11719,125)lot (11844,100)of (11944,245)cool (12189,309)chicks (12498,399)out (12897,220)there(13117,758)
            [4]I (14064,156)know (14220,168)that (14388,165)I (14553,168)went (14721,134)psycho (14855,632)on (15487,317)the (15804,333)phone(16137,352)
        """.trimIndent().split("\n")
        val data = LyricifySyllableParser.parse(lys)
        println(data.lines.toString())
    }
}