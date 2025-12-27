package com.mocharealm.accompanist.lyrics.core.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object KugouKrcMetadataDecoder {
    data class Metadata(
        val translations: List<String> = emptyList(),
        val phonetics: List<List<String>> = emptyList()
    )

    @OptIn(ExperimentalEncodingApi::class)
    fun decode(languageHeader: String?): Metadata {
        if (languageHeader.isNullOrBlank()) return Metadata()

        val contentBase64 = languageHeader
            .substringAfter("[language:")
            .substringBeforeLast("]")
            .trim()

        if (contentBase64.isEmpty()) return Metadata()

        return try {
            val decodedBytes = Base64.decode(contentBase64)
            val jsonStr = decodedBytes.decodeToString()
            parseJsonContent(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            Metadata()
        }
    }

    private fun parseJsonContent(jsonStr: String): Metadata {
        val root = Json.parseToJsonElement(jsonStr).jsonObject
        val contentArray = root["content"]?.jsonArray ?: return Metadata()

        val lyricLines = mutableListOf<String>()
        val pronLines = mutableListOf<List<String>>()

        for (element in contentArray) {
            val jsonObj = element.jsonObject
            val type = jsonObj["type"]?.jsonPrimitive?.intOrNull

            if (type == 0) {
                // 解析注音
                val rawArr = jsonObj["lyricContent"]?.jsonArray
                if (rawArr != null) {
                    val lineProns = rawArr.map { syllableNode ->
                        syllableNode.jsonArray.joinToString("") { it.jsonPrimitive.content }
                    }
                    pronLines.add(lineProns)
                }
            } else if (type == 1) {
                val rawArr = jsonObj["lyricContent"]?.jsonArray
                if (rawArr != null) {
                    val lineTrans = rawArr.map { syllableNode ->
                        syllableNode.jsonArray.joinToString("") { it.jsonPrimitive.content }
                    }
                    lyricLines.add(lineTrans.joinToString(""))
                }
            }
        }

        return Metadata(
            translations = lyricLines,
            phonetics = pronLines
        )
    }
}