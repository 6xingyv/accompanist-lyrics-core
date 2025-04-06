package io.mocha.music.lyrics.utils

import java.util.*

class SimpleXmlParser {
    fun parse(xml: String): XmlElement {
        val stack = Stack<XmlElement>()
        var currentElement: XmlElement? = null
        var i = 0
        while (i < xml.length) {
            when {
                xml[i] == '<' -> {
                    if (xml[i + 1] == '/') {
                        // 处理结束标签
                        val endIndex = xml.indexOf('>', i + 1)
                        val tagName = xml.substring(i + 2, endIndex)
                        currentElement = stack.pop()
                        if (stack.isNotEmpty()) {
                            val parent = stack.peek()
                            val newChildren = parent.children.toMutableList()
                            newChildren.add(currentElement)
                            stack.pop()
                            stack.push(parent.copy(children = newChildren))
                        }
                        i = endIndex + 1
                    } else {
                        // 处理开始标签
                        val endIndex = xml.indexOf('>', i + 1)
                        val tagPart = xml.substring(i + 1, endIndex)
                        val parts = tagPart.split(" ")
                        val tagName = parts[0]
                        val attributes = mutableListOf<XmlAttribute>()
                        for (j in 1 until parts.size) {
                            val attrParts = parts[j].split("=")
                            if (attrParts.size == 2) {
                                val attrName = attrParts[0]
                                val attrValue = attrParts[1].replace("\"", "")
                                attributes.add(XmlAttribute(attrName, attrValue))
                            }
                        }
                        val newElement = XmlElement(tagName, attributes, emptyList(), "")
                        stack.push(newElement)
                        i = endIndex + 1
                    }
                }
                else -> {
                    // 处理文本内容
                    val endIndex = xml.indexOf('<', i)
                    val text = xml.substring(i, endIndex).trim()
                    if (text.isNotEmpty()) {
                        currentElement = stack.pop()
                        val newElement = currentElement.copy(text = text)
                        stack.push(newElement)
                    }
                    i = endIndex
                }
            }
        }
        return stack.pop()
    }
}

data class XmlAttribute(val name: String, val value: String)
data class XmlElement(val name: String, val attributes: List<XmlAttribute>, val children: List<XmlElement>, val text: String)
