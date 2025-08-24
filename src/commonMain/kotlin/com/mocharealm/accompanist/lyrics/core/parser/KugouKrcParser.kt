package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.toSyncedLine


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
        // 翻译
        val languageLine = rawLines.firstOrNull { languageLineRegex.containsMatchIn(it.trim()) }
        val translations = parseTranslations(languageLine)

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

        val first = sylls.first().content
        if (first.endsWith("：") && first.length > 1) {
            // 触发切换逻辑
            val alignment = currentToggle
            // 下一次切换
            currentToggle = if (currentToggle == KaraokeAlignment.Start) {
                KaraokeAlignment.End
            } else {
                KaraokeAlignment.Start
            }
            return alignment to sylls
        }

        return KaraokeAlignment.Unspecified to sylls
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

    private fun parseTranslations(languageLine: String?): List<String>? {
        if (languageLine.isNullOrBlank()) return null
        val inside = languageLine.removePrefix("[language:").removeSuffix("]").trim()
        if (inside.isEmpty()) return null
        return try {
            val decoded = Base64.getDecoder().decode(inside)
            val jsonStr = String(decoded, Charsets.UTF_8)
            val root = JSONObject(jsonStr)
            val contentArray = root.optJSONArray("content") ?: return null
            val translations = mutableListOf<String>()
            for (i in 0 until contentArray.length()) {
                val obj = contentArray.getJSONObject(i)
                if (obj.optInt("type") == 1 && obj.optInt("language") == 0) {
                    val lyricContent = obj.optJSONArray("lyricContent") ?: continue
                    for (j in 0 until lyricContent.length()) {
                        val row = lyricContent.getJSONArray(j)
                        val sb = StringBuilder()
                        for (k in 0 until row.length()) sb.append(row.optString(k, ""))
                        translations.add(sb.toString())
                    }
                }
            }
            translations.takeIf { it.isNotEmpty() }
        } catch (_: Throwable) {
            null
        }
    }
}
