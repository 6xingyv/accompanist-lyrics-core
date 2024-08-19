package io.mocha.music.lyrics.parser

import io.mocha.music.lyrics.model.Attributes
import io.mocha.music.lyrics.utils.isDigitsOnly

object AttributesHelper {
    private val metadataParser = Regex("\\[(\\D+):(.+)\\]")
    fun parse(lines: List<String>): Attributes {
        var metadata = Attributes()
        for (line in lines) {
            val data = parseLine(line)
            if (data != null) {
                when(data.first) {
                    "ar"-> metadata = metadata.copy(artist = data.second)
                    "ti"-> metadata = metadata.copy(title = data.second)
                    "al"-> metadata = metadata.copy(album = data.second)
                    "offset"-> metadata = metadata.copy(offset = if (data.second.isDigitsOnly()) data.second.toInt() else 0)
                    "length"-> metadata = metadata.copy(duration = if (data.second.isDigitsOnly()) data.second.toInt() else 0)
                }
            }
        }
        return metadata
    }
    private fun parseLine(line: String): Pair<String, String>? {

        return when {
            metadataParser.matches(line) -> {
                // 匹配元数据行
                val matchResult = metadataParser.find(line)
                val (tag, value) = matchResult!!.destructured
               tag to value
            }

            else -> {null}
        }
    }
    fun removeAttributes(lines: List<String>): List<String> {
        return lines.filter { line->
            !(metadataParser.matches(line))
        }
    }
}