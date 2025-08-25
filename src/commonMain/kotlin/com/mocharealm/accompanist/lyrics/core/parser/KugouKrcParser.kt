package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class KugouKrcParser(
) : ILyricsParser {
    private val krcLineRegex = Regex("""^\[(\d+),(\d+)](.*)$""")
    private val syllableRegex = Regex("""<(\d+),(\d+),\d+>""")
    private val bgLineRegex = Regex("""^\[bg:(.*)](.*)$""")
    private val languageLineRegex = Regex("""^\[language:(.*)]\s*$""")
    private var currentToggle = KaraokeAlignment.Start

    override fun parse(lines: List<String>): SyncedLyrics = parse(lines.joinToString("\n"))

    override fun parse(content: String): SyncedLyrics {
        val rawLines = content.lineSequence().toList()
        // 翻译 和注音
        val languageLine = rawLines.firstOrNull { languageLineRegex.containsMatchIn(it.trim()) }
        val (translations,prons) = parseTranslations(languageLine)?: Pair(emptyList(), emptyList())

        val out = mutableListOf<KaraokeLine>()
        var lyricLineIndex = 0
        var lastLineStart = -1

        for (raw in rawLines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (languageLineRegex.containsMatchIn(line)) continue

            // 背景行
            bgLineRegex.find(line)?.let { m ->
                val bgContent = m.groupValues[1]
                val sylls = parseSyllablesWithRoleMerge(bgContent, 0)
                if (sylls.isNotEmpty()) {
                    out.add(
                        KaraokeLine(
                            syllables = sylls,
                            translation = null,
                            isAccompaniment = true,
                            alignment = KaraokeAlignment.Unspecified,
                            start = sylls.first().start,
                            end = sylls.last().end
                        )
                    )
                }
                return@let
            } ?: run {
                // 正常歌词行
                val m = krcLineRegex.find(line) ?: return@run
                var lineStart = m.groupValues[1].toLong()
                val contentPart = m.groupValues[3]

                if (lastLineStart != -1 && lineStart <= lastLineStart) {
                    lineStart = (lastLineStart + 3).toLong()
                }
                lastLineStart = lineStart.toInt()

                val sylls = parseSyllablesWithRoleMerge(contentPart, lineStart.toInt())

                // 判断角色 → alignment
                val (alignment, finalSylls) = detectRoleAndAlignment(sylls)

                val translation = translations?.getOrNull(lyricLineIndex)?.takeIf { it.isNotBlank() }

                out.add(
                    KaraokeLine(
                        syllables = finalSylls,
                        translation = translation,
                        isAccompaniment = false,
                        alignment = alignment,
                        start = finalSylls.first().start,
                        end = finalSylls.last().end
                    )
                )
                lyricLineIndex++
            }
        }

        return SyncedLyrics(out)
    }

    /** 根据行首 token 动态识别角色和对齐 */
    private fun detectRoleAndAlignment(sylls: List<KaraokeSyllable>): Pair<KaraokeAlignment, List<KaraokeSyllable>> {
        if (sylls.isEmpty()) return KaraokeAlignment.Unspecified to sylls

        // 把整行拼起来
        val lineContent = sylls.joinToString(separator = "") { it.content }

        // 判断行首是否有角色标识
        if (lineContent.length > 1 && (lineContent.startsWith("：") || lineContent.startsWith(":") ||
                    lineContent.endsWith("：") || lineContent.endsWith(":"))) {
            // 切换逻辑
            currentToggle = if (currentToggle == KaraokeAlignment.Start) {
                KaraokeAlignment.End
            } else {
                KaraokeAlignment.Start
            }
            return currentToggle to sylls
        }

        return currentToggle to sylls
    }

    private fun parseSyllablesWithRoleMerge(content: String, lineStart: Int): List<KaraokeSyllable> {
        data class Tok(val offset: Int, val duration: Int, val text: String)
        val tokens = mutableListOf<Tok>()
        var cur = 0
        while (cur < content.length) {
            val m = syllableRegex.find(content, cur) ?: break
            val offset = m.groupValues[1].toIntOrNull() ?: 0
            val dur = m.groupValues[2].toIntOrNull() ?: 0
            val textStart = m.range.last + 1
            val next = syllableRegex.find(content, textStart)
            val textEnd = next?.range?.first ?: content.length
            val text = content.substring(textStart, textEnd)
            tokens.add(Tok(offset, dur, text))
            cur = textEnd
        }

        if (tokens.isEmpty()) return emptyList()

        val merged = mutableListOf<Tok>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val next = tokens.getOrNull(i + 1)
            if (next != null && t.text.length == 1 && next.text == "：") {
                merged.add(Tok(offset = next.offset, duration = next.duration, text = t.text + "："))
                i += 2
            } else {
                merged.add(t)
                i += 1
            }
        }

        return merged.map {
            val s = lineStart + it.offset
            val e = lineStart + it.offset + it.duration
            KaraokeSyllable(it.text, s, e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseTranslations(languageLine: String?): Pair<List<String>, List<String>>? {
        if (languageLine.isNullOrBlank()) return null
        val inside = languageLine.removePrefix("[language:").removeSuffix("]").trim()
        if (inside.isEmpty()) return null

        return try {
            val decoded = Base64.decode(inside) // Kotlin Multiplatform 的 Base64
            val jsonStr = decoded.decodeToString() // 替代 String(decoded, Charsets.UTF_8)

            val root = Json.parseToJsonElement(jsonStr).jsonObject
            val contentArray = root["content"]?.jsonArray ?: return null

            val lyricLines = mutableListOf<String>()
            val pronLines = mutableListOf<String>()

            for (obj in contentArray) {
                val jsonObj = obj.jsonObject
                val type = jsonObj["type"]?.jsonPrimitive?.intOrNull ?: continue
                val language = jsonObj["language"]?.jsonPrimitive?.intOrNull ?: continue

                if (type == 0 && language == 0) {
                    // 注音
                    val pronunciation = jsonObj["lyricContent"]?.jsonArray ?: continue
                    for (row in pronunciation) {
                        val pronRow = row.jsonArray
                        pronLines.add(pronRow.joinToString("") { it.jsonPrimitive.content })
                    }
                }

                if (type == 1 && language == 0) {
                    // 歌词
                    val lyricContent = jsonObj["lyricContent"]?.jsonArray ?: continue
                    for (row in lyricContent) {
                        val arr = row.jsonArray
                        lyricLines.add(arr.joinToString("") { it.jsonPrimitive.content })
                    }
                }
            }

            if (lyricLines.isNotEmpty()) Pair(lyricLines, pronLines) else null
        } catch (_: Throwable) {
            null
        }
    }

}
