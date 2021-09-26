package com.boardgamegeek.util

import android.content.Context
import com.boardgamegeek.R
import java.util.regex.Pattern

/**
 * Converts XML returned from the BGG API into HTML.
 */
class XmlApi2MarkupConverter(private val context: Context) {
    private val replacers = mutableListOf<Replaceable>()

    init {
        replacers.add(SimpleReplacer("\\[o\\]", "<details><summary>${context.getString(R.string.spoiler)}</summary>"))
        replacers.add(SimpleReplacer("\\[/o\\]", "</details>"))
        createPair("heading", "h3")
        replacers.add(SimpleReplacer("\\[hr\\]", "<hr/>"))
    }

    private fun createPair(tag: String, replacementTag: String) {
        replacers.add(SimpleReplacer.createStart(tag, replacementTag))
        replacers.add(SimpleReplacer.createEnd(tag, replacementTag))
    }

    fun toHtml(text: String?): String {
        var html = text.orEmpty()
        if (html.isEmpty()) return ""
        for (replacer in replacers) {
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