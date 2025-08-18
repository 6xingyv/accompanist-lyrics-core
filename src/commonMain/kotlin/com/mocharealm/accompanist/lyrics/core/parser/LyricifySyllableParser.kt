package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.utils.isDigitsOnly

/**
 * A parser for lyrics in the Lyricify Syllable format.
 *
 * More information about Lyricify Syllable format can be found [here](https://github.com/WXRIW/Lyricify-App/blob/main/docs/Lyricify%204/Lyrics.md#lyricify-syllable-%E6%A0%BC%E5%BC%8F%E8%A7%84%E8%8C%83).
 */
object LyricifySyllableParser: ILyricsParser {
    private val parser = Regex("(.*?)\\((\\d+),(\\d+)\\)")

    override fun parse(lines:List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = lyricsLines.mapNotNull { line->
            if (line.isBlank()) null else parseLine(line)
        }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(line:String): KaraokeLine {
        val real:String
        var isAccompaniment = false
        val alignment: KaraokeAlignment
        val attributes = mutableListOf<Int>()
        if (line.contains("]") and line.contains("[") and (line.indexOf("]")-line.indexOf("[")==2)) {
            real = line.substring(startIndex = (line.indexOf(']')+1))
            val regex = Regex("\\[(\\d+)]")
            val attribute = regex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            attributes.add(attribute)
            if(attribute !in 0..5) {
                isAccompaniment = true
            }
            alignment = if ( attribute==2 || attribute==5 || attribute==8) {
                KaraokeAlignment.End
            } else {
                KaraokeAlignment.Start
            }

        }
        else {
            real = line
            isAccompaniment = false
            alignment = KaraokeAlignment.Start
        }

        val data = parser.findAll(real)
        val syllables = data.map { matched->
            val result = matched.groupValues
            if (result.size == 4 && result[2].isDigitsOnly() && result[3].isDigitsOnly()) {
                KaraokeSyllable(
                    content =result[1],
                    start = result[2].toInt(),
                    end = result[2].toInt() +result[3].toInt(),
                )
            } else  {
                KaraokeSyllable(
                content ="Error",
                start = 0,
                end = 0,
            ) }
        }.toList()

        // 处理空音节列表的情况
        val startTime = if (syllables.isNotEmpty()) syllables[0].start else 0
        val endTime = if (syllables.isNotEmpty()) syllables.last().end else 0

        return KaraokeLine(syllables, null, isAccompaniment, alignment, startTime, endTime)
    }
}