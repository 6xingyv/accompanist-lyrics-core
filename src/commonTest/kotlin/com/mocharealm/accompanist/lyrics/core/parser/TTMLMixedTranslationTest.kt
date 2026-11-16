package com.mocharealm.accompanist.lyrics.core.parser
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import kotlin.test.Test
import kotlin.test.assertEquals
class TTMLMixedTranslationTest {
 @Test fun preservesMixedTranslationTextAndSeparatesBacking() {
  val text="""<tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><head><metadata><itunes:translation xml:lang="fr"><itunes:text for="L1">Avant <span>milieu</span> après<span ttm:role="x-bg">(écho <span>ici</span>)</span> fin</itunes:text></itunes:translation></metadata></head><body><div><p begin="1.0" end="3.0" itunes:key="L1"><span begin="1.0" end="2.0">Original</span><span ttm:role="x-bg" begin="2.0" end="3.0"><span begin="2.0" end="3.0">(Echo)</span></span></p></div></body></tt>"""
  val line=TTMLParser().parse(text).lines.single() as KaraokeLine.MainKaraokeLine
  assertEquals("Avant milieu après fin",line.translation)
  assertEquals("écho ici",line.accompanimentLines!!.single().translation)
 }
}
