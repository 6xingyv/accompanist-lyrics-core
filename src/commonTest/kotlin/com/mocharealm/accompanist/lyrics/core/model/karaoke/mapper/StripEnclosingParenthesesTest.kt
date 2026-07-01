package com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import kotlin.test.Test
import kotlin.test.assertEquals

class StripEnclosingParenthesesTest {

    /** Builds syllables whose contents concatenate to a line, with dummy sequential timing. */
    private fun syllables(vararg contents: String): List<KaraokeSyllable> =
        contents.mapIndexed { i, c -> KaraokeSyllable(content = c, start = i * 100, end = i * 100 + 100) }

    private fun List<KaraokeSyllable>.text() = joinToString("") { it.content }

    @Test
    fun stripsSingleSyllableWrapper() {
        assertEquals("Yeah", syllables("(Yeah)").stripEnclosingParentheses().text())
    }

    @Test
    fun stripsWrapperSpanningMultipleSyllables() {
        val result = syllables("(And ", "I ", "want ", "you, ", "baby)").stripEnclosingParentheses()
        assertEquals("And I want you, baby", result.text())
        assertEquals("And ", result.first().content)
        assertEquals("baby", result.last().content)
    }

    @Test
    fun stripsWrapperWhenParensSplitAcrossSyllables() {
        // Real me.lys shape: "(Ba" + "by)"
        val result = syllables("(Ba", "by)").stripEnclosingParentheses()
        assertEquals("Baby", result.text())
    }

    @Test
    fun preservesTimingAndSyllableCount() {
        val original = syllables("(Ba", "by)")
        val result = original.stripEnclosingParentheses()
        assertEquals(original.size, result.size)
        assertEquals(original.first().start, result.first().start)
        assertEquals(original.last().end, result.last().end)
    }

    @Test
    fun keepsInlineNonEnclosingParentheses() {
        // First pair closes before the end → not one outer wrapper → untouched.
        assertEquals("(ooh) yeah (ooh)", syllables("(ooh) yeah (ooh)").stripEnclosingParentheses().text())
        assertEquals("Hello (world)", syllables("Hello (world)").stripEnclosingParentheses().text())
        assertEquals("(a) (b)", syllables("(a) (b)").stripEnclosingParentheses().text())
    }

    @Test
    fun peelsNestedWrappers() {
        assertEquals("x", syllables("((x))").stripEnclosingParentheses().text())
        assertEquals("a (b) c", syllables("(a (b) c)").stripEnclosingParentheses().text())
    }

    @Test
    fun handlesFullWidthParentheses() {
        assertEquals("对唱", syllables("（对唱）").stripEnclosingParentheses().text())
    }

    @Test
    fun leavesUnparenthesizedTextUnchanged() {
        assertEquals("just words", syllables("just ", "words").stripEnclosingParentheses().text())
    }

    @Test
    fun toleratesSurroundingWhitespace() {
        assertEquals("  Yeah  ", syllables("  (Yeah)  ").stripEnclosingParentheses().text())
    }
}
