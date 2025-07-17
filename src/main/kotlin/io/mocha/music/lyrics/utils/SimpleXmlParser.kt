package io.mocha.music.lyrics.utils

import java.util.*

class SimpleXmlParser {
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
                        val actualTagPart = if (isSelfClosing) tagPart.dropLast(1).trim() else tagPart

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
                    if (nextTagIndex != -1) {
                        val text = cleanXml.substring(i, nextTagIndex).trim()
                        if (text.isNotEmpty() && stack.isNotEmpty()) {
                            val currentElement = stack.pop()
                            val newElement = currentElement.copy(text = currentElement.text + text)
                            stack.push(newElement)
                        }
                        i = nextTagIndex
                    } else {
                        break
                    }
                }
            }
        }

        return if (stack.isNotEmpty()) stack.first() else XmlElement("", emptyList(), emptyList(), "")
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

data class XmlAttribute(val name: String, val value: String)
data class XmlElement(val name: String, val attributes: List<XmlAttribute>, val children: List<XmlElement>, val text: String)
