package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.SyncedLyrics
import io.mocha.music.lyrics.model.synced.SyncedLine
import io.mocha.music.lyrics.utils.parseAsTime

object LrcParser : ILyricsParser {
    private val parser = Regex("(\\[\\d{1,2}:\\d{1,2}\\.\\d{2,3}])+(.*)")
    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = AttributesHelper.removeAttributes(lines)
        val data = lyricsLines
            .map { line ->
                parseLine(line)
            }
            .rearrangeTime()
            .combineRawWithTranslation()
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(content: String): SyncedLine {
        val data = parser.findAll(content)
        return data.map { matched->
            val result = matched.groupValues
            var start =0
            if (result.size==2) {
                start=result[0].parseAsTime()
            }
            SyncedLine(
                start = start,
                end = 0,
                content = result[1],
                translation = null
            )
        }.first()
    }

    private fun List<SyncedLine>.combineRawWithTranslation(): List<SyncedLine> {
        val list = mutableListOf<SyncedLine>()

        for (index in indices) {
            val line = this[index]
            val nextLine = this[index+1]
            if (line.start == nextLine.start) {
                list.add(line.copy(translation = nextLine.content))
            }
        }

        return list
    }

    private fun List<SyncedLine>.rearrangeTime(): List<SyncedLine> {
        val list = this.toMutableList().mapIndexed { index, line ->
            if (index <= this.size-1) {
                val nextLine = this[index+1]
                line.copy(end = nextLine.start)
            } else line.copy(end = Int.MAX_VALUE)

        }
        val finalList = mutableListOf<SyncedLine>()

        for (index in list.indices) {
            if (index <= list.size-1) {
                val line = list[index]
                val nextLine = list[index+1]

                if (line.content.isBlank()) continue
                if (nextLine.content.isBlank()) {
                    finalList.add(line.copy(end = nextLine.start))
                }
            }
        }

        return finalList
    }
}


