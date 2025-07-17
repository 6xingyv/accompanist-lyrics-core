package com.mocharealm.accompanist.lyrics.parser

import kotlin.test.Test

class LrcParserTest {
    @Test
    fun testParseLrcWithoutTranslation() {
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
    fun testParseLrcWithTranslation() {
        val lrc ="""
            [00:39.96]I lean in and you move away
            [00:39.96]我靠在里面，你就离开
            [00:42.90]
            [00:45.62]But you linger all the same
            [00:45.62]可你依然徘徊不去
        """.trimIndent().split("\n")
        val data = LrcParser.parse(lrc)
        println(data.lines.toString())
    }
}
