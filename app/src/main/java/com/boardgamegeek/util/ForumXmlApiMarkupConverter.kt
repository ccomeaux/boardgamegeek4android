package com.boardgamegeek.util

import java.util.regex.Pattern

/**
 * Converts XML returned from the BGG API into HTML.
 */
class ForumXmlApiMarkupConverter(spoilerTag: String) {
    private val replacerList = mutableListOf<Replaceable>()

    init {
        replacerList.add(SimpleReplacer("\\[o\\]", "<details><summary>$spoilerTag</summary>"))
        replacerList.add(SimpleReplacer("\\[/o\\]", "</details>"))
        createPair("heading", "h3")
        replacerList.add(SimpleReplacer("\\[hr\\]", "<hr/>"))
    }

    @Suppress("SameParameterValue")
    private fun createPair(tag: String, replacementTag: String) {
        replacerList.add(SimpleReplacer.createStart(tag, replacementTag))
        replacerList.add(SimpleReplacer.createEnd(tag, replacementTag))
    }

    fun toHtml(text: String?): String {
        var html = text.orEmpty()
        if (html.isEmpty()) return ""
        for (replacer in replacerList) {
            html = replacer.replace(html)
        }
        return html
    }

    internal interface Replaceable {
        fun replace(text: String): String
        fun strip(text: String): String
    }

    private class SimpleReplacer(pattern: String, val replacement: String) : Replaceable {
        val pattern: Pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            return if (matcher.find()) {
                matcher.replaceAll(replacement)
            } else text
        }

        override fun strip(text: String): String {
            return pattern.matcher(text).replaceAll("")
        }

        companion object {
            fun createStart(tag: String, replacementTag: String): SimpleReplacer {
                return SimpleReplacer("\\[$tag\\]", "<$replacementTag>")
            }

            fun createEnd(tag: String, replacementTag: String): SimpleReplacer {
                return SimpleReplacer("\\[/$tag\\]", "</$replacementTag>")
            }
        }
    }
}
