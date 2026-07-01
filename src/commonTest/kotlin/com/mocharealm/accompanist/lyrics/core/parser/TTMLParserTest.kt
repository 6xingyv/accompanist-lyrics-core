package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.exporter.TTMLExporter
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TTMLParserTest {

    @Test
    fun testNoFallbackWhenPhoneticExists() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="00:00.000" end="00:01.000">
                        <span begin="00:00.000" end="00:01.000">Hello</span>
                        <span ttm:role="x-roman">Existing Phonetic</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val provider = object : PhoneticProvider {
            override val phoneticLevel: PhoneticLevel = PhoneticLevel.SYLLABLE
            override fun getPhonetic(content: String): String = "Fallback"
        }

        val result = TTMLParser(provider).parse(ttml)
        val line = result.lines[0] as KaraokeLine.MainKaraokeLine

        // Should keep existing line phonetic and NOT apply fallback to syllables
        assertEquals("Existing Phonetic", line.phonetic)
        assertEquals(null, line.syllables[0].phonetic)
    }

    @Test
    fun testNoFallbackWhenSyllablePhoneticExists() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
                <head>
                    <metadata>
                        <iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal">
                            <transliterations>
                                <transliteration>
                                    <text for="L1">
                                        <span begin="00:00.000" end="00:00.000">SyllablePhonetic</span>
                                    </text>
                                </transliteration>
                            </transliterations>
                        </iTunesMetadata>
                    </metadata>
                </head>
                <body><div>
                    <p begin="00:00.000" end="00:01.000" itunes:key="L1">
                        <span begin="00:00.000" end="00:01.000">Hello</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val provider = object : PhoneticProvider {
            override val phoneticLevel: PhoneticLevel = PhoneticLevel.LINE
            override fun getPhonetic(content: String): String = "Fallback"
        }

        val result = TTMLParser(provider).parse(ttml)
        val line = result.lines[0] as KaraokeLine.MainKaraokeLine

        // Should keep existing syllable phonetic and NOT apply fallback to line
        assertEquals("SyllablePhonetic", line.syllables[0].phonetic)
        assertEquals(null, line.phonetic)
    }

    @Test
    fun testBgPositioning() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="00:01.000" end="00:05.000">
                        <span ttm:role="x-bg" begin="00:01.000" end="00:02.000">
                            <span begin="00:01.000" end="00:02.000">(Before)</span>
                        </span>
                        <span begin="00:02.000" end="00:03.000">Main</span>
                        <span ttm:role="x-bg" begin="00:03.000" end="00:04.000">
                            <span begin="00:03.000" end="00:04.000">(After)</span>
                        </span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val result = TTMLParser().parse(ttml)
        assertEquals(1, result.lines.size)

        val line = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals("Main", line.syllables.joinToString("") { it.content }.trim())

        val bgs = line.accompanimentLines
        assertNotNull(bgs)
        assertEquals(2, bgs.size)
        // The parentheses that mark background vocals are stripped on parse.
        assertEquals("Before", bgs[0].syllables.joinToString("") { it.content }.trim())
        assertEquals("After", bgs[1].syllables.joinToString("") { it.content }.trim())
    }

    @Test
    fun testNestedTranslationAndSyllables() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="00:10.000" end="00:15.000">
                        <span begin="00:10.000" end="00:11.000">Hello</span>
                        <span ttm:role="x-bg" begin="00:11.000" end="00:12.000">
                            <span begin="00:11.000" end="00:12.000">World</span>
                            <span ttm:role="x-translation">世界</span>
                        </span>
                        <span ttm:role="x-translation">你好</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val result = TTMLParser().parse(ttml)
        val line = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals("你好", line.translation)

        val bg = line.accompanimentLines?.first()
        assertNotNull(bg)
        assertEquals("世界", bg.translation)
        assertEquals("World", bg.syllables.first().content.trim())
    }

    @Test
    fun testLinePhonetic() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:tts="http://www.w3.org/ns/ttml#styling" xmlns:amll="http://www.example.com/ns/amll" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="None">
                <head>
                    <metadata>
                        <ttm:agent type="person" xml:id="v1" />
                    </metadata>
                </head>
                <body dur="00:00.000">
                    <div begin="00:00.000" end="00:00.000">
                        <p begin="00:00.000" end="00:00.000" ttm:agent="v1" itunes:key="L1">
                            <span begin="00:00.000" end="00:00.000">Hello</span>
                            <span begin="00:00.000" end="00:00.000">World</span>
                            <span ttm:role="x-translation" xml:lang="zh-CN">你好世界</span>
                            <span ttm:role="x-roman">Halo Waludo</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val result = TTMLParser().parse(ttml)
        val line = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals("Halo Waludo", line.phonetic)
    }

    @Test
    fun testSyllablePhonetic() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:tts="http://www.w3.org/ns/ttml#styling" xmlns:amll="http://www.example.com/ns/amll" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="None">
                <head>
                    <metadata>
                        <ttm:agent type="person" xml:id="v1" />
                        <iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal">
                            <transliterations>
                                <transliteration>
                                    <text for="L1">
                                        <span begin="00:00.000" end="00:00.000">Halo</span>
                                        <span begin="00:00.000" end="00:00.000">waludo</span>
                                    </text>
                                </transliteration>
                            </transliterations>
                        </iTunesMetadata>
                    </metadata>
                </head>
                <body dur="00:00.000">
                    <div begin="00:00.000" end="00:00.000">
                        <p begin="00:00.000" end="00:00.000" ttm:agent="v1" itunes:key="L1">
                            <span begin="00:00.000" end="00:00.000">Hello</span>
                            <span begin="00:00.000" end="00:00.000">World</span>
                            <span ttm:role="x-translation" xml:lang="zh-CN">你好世界</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val result = TTMLParser().parse(ttml)
        val line = result.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals("Halo", line.syllables[0].phonetic)
        assertEquals("waludo", line.syllables[1].phonetic)
    }

    @Test
    fun testTTMLRoundTrip() {
        val originalTtml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" itunes:timing="Word">
              <head>
                <metadata>
                  <ttm:agent type="person" xml:id="v1"/>
                  <ttm:agent type="person" xml:id="v2"/>
                </metadata>
              </head>
              <body dur="00:05.000">
                <div begin="00:01.000" end="00:05.000">
                  <p begin="00:01.000" end="00:05.000" ttm:agent="v1">
                    <span begin="00:01.000" end="00:02.000">Main</span>
                    <span ttm:role="x-translation" xml:lang="zh-CN">主词</span>
                    <span ttm:role="x-bg" begin="00:02.000" end="00:03.000">
                      <span begin="00:02.000" end="00:03.000">BG</span>
                      <span ttm:role="x-translation" xml:lang="zh-CN">背景</span>
                    </span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsed = TTMLParser().parse(originalTtml)
        val exported = TTMLExporter.export(parsed)

        // 第二次解析确保数据一致性
        val reParsed = TTMLParser().parse(exported)
        assertEquals(parsed.lines.size, reParsed.lines.size)

        val p1 = parsed.lines[0] as KaraokeLine.MainKaraokeLine
        val p2 = reParsed.lines[0] as KaraokeLine.MainKaraokeLine

        assertEquals(p1.translation, p2.translation)
        assertEquals(p1.accompanimentLines?.size, p2.accompanimentLines?.size)
        assertEquals(
            p1.accompanimentLines?.first()?.translation,
            p2.accompanimentLines?.first()?.translation
        )
    }

    @Test
    fun testParseSyncedLineAndMixedLines() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="00:00.000" end="00:02.000">This is a regular synced line without syllables</p>
                    <p begin="00:02.000" end="00:05.000"><span begin="00:02.000" end="00:03.000">Ka</span><span begin="00:03.000" end="00:04.000">ra</span><span begin="00:04.000" end="00:05.000">oke</span></p>
                    <p begin="00:05.000" end="00:07.000">Another synced line<span ttm:role="x-translation">Translation here</span></p>
                </div></body>
            </tt>
        """.trimIndent()

        val result = TTMLParser().parse(ttml)
        val lines = result.lines

        assertEquals(3, lines.size)

        assertTrue(lines[0] is SyncedLine)
        assertEquals("This is a regular synced line without syllables", (lines[0] as SyncedLine).content.trim())
        assertEquals(0, lines[0].start)
        assertEquals(2000, lines[0].end)

        assertTrue(lines[1] is KaraokeLine.MainKaraokeLine)
        val karaokeLine = lines[1] as KaraokeLine.MainKaraokeLine
        assertEquals(3, karaokeLine.syllables.size)
        
        assertEquals("Ka", karaokeLine.syllables[0].content)
        assertEquals(2000, karaokeLine.syllables[0].start)
        
        assertEquals("ra", karaokeLine.syllables[1].content)
        assertEquals(3000, karaokeLine.syllables[1].start)
        
        assertEquals("oke", karaokeLine.syllables[2].content)
        assertEquals(4000, karaokeLine.syllables[2].start)

        assertTrue(lines[2] is SyncedLine)
        assertEquals("Another synced line", (lines[2] as SyncedLine).content.trim())
        assertEquals("Translation here", (lines[2] as SyncedLine).translation?.trim())
    }

    @Test
    fun testAlignmentFlipsOnPersonChangeWithGroupTransparent() {
        // Apple's model: person singers flip the side each time the person changes;
        // a "group" agent is always on the LEFT and is transparent to the flip (v2
        // still flips relative to v1, not relative to the group in between).
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1"/>
                    <ttm:agent type="person" xml:id="v2"/>
                    <ttm:agent type="person" xml:id="v3"/>
                    <ttm:agent type="group" xml:id="v4"/>
                </metadata></head>
                <body><div>
                    <p begin="00:00.000" end="00:01.000" ttm:agent="v1"><span begin="00:00.000" end="00:01.000">One</span></p>
                    <p begin="00:01.000" end="00:02.000" ttm:agent="v4"><span begin="00:01.000" end="00:02.000">Everyone</span></p>
                    <p begin="00:02.000" end="00:03.000" ttm:agent="v2"><span begin="00:02.000" end="00:03.000">Two</span></p>
                    <p begin="00:03.000" end="00:04.000" ttm:agent="v3"><span begin="00:03.000" end="00:04.000">Three</span></p>
                </div></body>
            </tt>
        """.trimIndent()

        val lines = TTMLParser().parse(ttml).lines
        assertEquals(4, lines.size)
        assertEquals(KaraokeAlignment.Start, (lines[0] as KaraokeLine).alignment) // v1 first -> left
        assertEquals(KaraokeAlignment.Start, (lines[1] as KaraokeLine).alignment) // group -> left, transparent
        assertEquals(KaraokeAlignment.End, (lines[2] as KaraokeLine).alignment)   // v2 flips vs v1 -> right
        assertEquals(KaraokeAlignment.Start, (lines[3] as KaraokeLine).alignment) // v3 flips vs v2 -> left
    }

    @Test
    fun testOtherAgentAlwaysRightAndTransparent() {
        // An "other" agent is always on the right and does not disturb the person
        // toggle: v1 stays on the left before and after the "other" line.
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1"/>
                    <ttm:agent type="other" xml:id="v2"/>
                </metadata></head>
                <body><div>
                    <p begin="00:00.000" end="00:01.000" ttm:agent="v1"><span begin="00:00.000" end="00:01.000">One</span></p>
                    <p begin="00:01.000" end="00:02.000" ttm:agent="v2"><span begin="00:01.000" end="00:02.000">Other</span></p>
                    <p begin="00:02.000" end="00:03.000" ttm:agent="v1"><span begin="00:02.000" end="00:03.000">One again</span></p>
                </div></body>
            </tt>
        """.trimIndent()

        val lines = TTMLParser().parse(ttml).lines
        assertEquals(3, lines.size)
        assertEquals(KaraokeAlignment.Start, (lines[0] as KaraokeLine).alignment) // v1 -> left
        assertEquals(KaraokeAlignment.End, (lines[1] as KaraokeLine).alignment)   // other -> right
        assertEquals(KaraokeAlignment.Start, (lines[2] as KaraokeLine).alignment) // v1 still left
    }

    @Test
    fun testAlignmentSeedsFromFirstLineAgent() {
        // Our one deviation from Apple: the first line's agent seeds the starting
        // side by its id. Opening with v2 (even) starts on the RIGHT, then flips.
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="00:00.000" end="00:01.000" ttm:agent="v2"><span begin="00:00.000" end="00:01.000">First</span></p>
                    <p begin="00:01.000" end="00:02.000" ttm:agent="v1"><span begin="00:01.000" end="00:02.000">Second</span></p>
                    <p begin="00:02.000" end="00:03.000" ttm:agent="v2"><span begin="00:02.000" end="00:03.000">Third</span></p>
                </div></body>
            </tt>
        """.trimIndent()

        val lines = TTMLParser().parse(ttml).lines
        assertEquals(3, lines.size)
        assertEquals(KaraokeAlignment.End, (lines[0] as KaraokeLine).alignment)   // v2 first -> right
        assertEquals(KaraokeAlignment.Start, (lines[1] as KaraokeLine).alignment) // v1 -> left
        assertEquals(KaraokeAlignment.End, (lines[2] as KaraokeLine).alignment)   // v2 keeps right
    }
}