package com.mocharealm.accompanist.lyrics.parser

import com.mocharealm.accompanist.lyrics.model.Attributes
import com.mocharealm.accompanist.lyrics.utils.isDigitsOnly

/**
 * Helper object for parsing and managing attributes from lyrics lines.
 * It uses a regular expression to identify and extract metadata tags and their values.
 *
 * Supported metadata tags include:
 * - `ar`: Artist
 * - `ti`: Title
 * - `al`: Album
 * - `offset`: Offset in milliseconds
 * - `length`: Duration in milliseconds
 */
@Suppress("RegExpRedundantEscape")
object AttributesHelper {
    private val metadataParser = Regex("\\[(\\D+):(.+)\\]")

    /**
     * Parses a list of lines to extract attributes.
     *
     * @param lines The list of strings, where each string is a line from the lyrics file.
     * @return An [Attributes] object containing the parsed metadata.
     *         If a metadata tag is not found or its value is invalid, the corresponding attribute will hold a default value.
     */
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