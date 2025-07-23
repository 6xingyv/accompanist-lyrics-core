package com.mocharealm.accompanist.lyrics.utils

import java.util.Stack

internal data class XmlAttribute(
    val name: String,
    val value: String
)

internal data class XmlElement(
    val name: String,
    val attributes: List<XmlAttribute>,
    val children: List<XmlElement>,
    val text: String
)

internal class SimpleXmlParser {
    fun parse(xml: String): XmlElement {
        val cleanXml = xml.replace(Regex("\\s+"), " ").trim()
        val stack = Stack<XmlElement>()
        var i = 0

        while (i < cleanXml.length) {
            when {
                cleanXml[i] == '<' -> {
                    if (cleanXml[i + 1] == '/') {
                        // 处理结束标签
                        val endIndex = cleanXml.indexOf('>', i + 1)
                        if (stack.size > 1) {
                            val currentElement = stack.pop()
                            val parent = stack.peek()
                            val newChildren = parent.children.toMutableList()
                            newChildren.add(currentElement)
                            stack.pop()
                            stack.push(parent.copy(children = newChildren))
                        }
                        i = endIndex + 1
                    } else {
                        // 处理开始标签
                        val endIndex = cleanXml.indexOf('>', i + 1)
                        val tagPart = cleanXml.substring(i + 1, endIndex)

                        // 检查是否为自闭合标签
                        val isSelfClosing = tagPart.endsWith("/")
                        val actualTagPart =
                            if (isSelfClosing) tagPart.dropLast(1).trim() else tagPart

                        val (tagName, attributes) = parseTagAndAttributes(actualTagPart)
                        val newElement = XmlElement(tagName, attributes, emptyList(), "")

                        if (isSelfClosing) {
                            // 自闭合标签直接添加到父元素
                            if (stack.isNotEmpty()) {
                                val parent = stack.peek()
                                val newChildren = parent.children.toMutableList()
                                newChildren.add(newElement)
                                stack.pop()
                                stack.push(parent.copy(children = newChildren))
                            } else {
                                stack.push(newElement)
                            }
                        } else {
                            stack.push(newElement)
                        }
                        i = endIndex + 1
                    }
                }

                else -> {
                    // 处理文本内容
                    val nextTagIndex = cleanXml.indexOf('<', i)
                    if (nextTagIndex == -1) { break } // End of document

                    val rawText = cleanXml.substring(i, nextTagIndex)

                    if (rawText.isNotEmpty() && stack.isNotEmpty()) {
                        val trimmedText = rawText.trim()

                        // 案例 1: 如果是有效字符，它属于当前栈顶元素的内部文本。
                        // 例如 `<span>Ya</span>` 中的 "Ya"
                        if (trimmedText.isNotEmpty()) {
                            val currentElement = stack.pop()
                            stack.push(currentElement.copy(text = currentElement.text + trimmedText))
                        }

                        // 案例 2: 如果原始文本不等于trim后的文本，说明存在纯空格。
                        // 这些空格应该作为一个独立的文本节点，成为父元素的子节点。
                        // 例如 `</span> <span` 中的 " "
                        val whitespace = rawText.replace(trimmedText, "")
                        if (whitespace.isNotEmpty()) {
                            // 在关闭标签后，栈顶就是父元素
                            val textNode = XmlElement(name = "#text", text = whitespace, attributes = emptyList(), children = emptyList())
                            val parent = stack.peek()
                            val newChildren = parent.children.toMutableList().apply { add(textNode) }
                            stack.pop() // pop and push to update the element with new children
                            stack.push(parent.copy(children = newChildren))
                        }
                    }
                    i = nextTagIndex
                }
            }
        }

        return if (stack.isNotEmpty()) stack.first() else XmlElement(
            "",
            emptyList(),
            emptyList(),
            ""
        )
    }

    private fun parseTagAndAttributes(tagPart: String): Pair<String, List<XmlAttribute>> {
        val parts = tagPart.split(" ")
        val tagName = parts[0]
        val attributes = mutableListOf<XmlAttribute>()

        var i = 1
        while (i < parts.size) {
            val part = parts[i]
            if (part.contains("=")) {
                val attrParts = part.split("=", limit = 2)
                if (attrParts.size == 2) {
                    val attrName = attrParts[0]
                    var attrValue = attrParts[1]

                    // 处理带引号的属性值
                    if (attrValue.startsWith("\"") && !attrValue.endsWith("\"")) {
                        // 属性值跨越多个空格分隔的部分
                        var j = i + 1
                        while (j < parts.size && !attrValue.endsWith("\"")) {
                            attrValue += " " + parts[j]
                            j++
                        }
                        i = j - 1
                    }

                    attrValue = attrValue.removeSurrounding("\"")
                    attributes.add(XmlAttribute(attrName, attrValue))
                }
            }
            i++
        }

        return tagName to attributes
    }
}
