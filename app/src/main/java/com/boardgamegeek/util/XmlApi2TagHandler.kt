package com.boardgamegeek.util

import android.graphics.Color
import android.text.Editable
import android.text.Html.TagHandler
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import org.xml.sax.XMLReader

class XmlApi2TagHandler : TagHandler {
    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        when {
            tag.equals("details", ignoreCase = true) -> {
                if (opening)
                    start(output, Spoiler())
                else
                    end(output, Spoiler::class.java, BackgroundColorSpan(Color.BLACK))
            }
            tag.equals("summary", ignoreCase = true) -> {
                if (opening)
                    start(output, SpoilerHeader())
                else
                    end(output, SpoilerHeader::class.java, ForegroundColorSpan(Color.WHITE))
            }
            tag.equals("pre", ignoreCase = true) -> {
                if (opening)
                    start(output, Monospace())
                else
                    end(output, Monospace::class.java, TypefaceSpan("monospace"))
            }
        }
    }

    private class Spoiler
    private class SpoilerHeader
    private class Monospace

    companion object {
        private fun start(text: Editable, mark: Any) {
            val length = text.length
            text.setSpan(mark, length, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        private fun end(text: Editable, kind: Class<*>, spans: Any) {
            text.getSpans(0, text.length, kind).lastOrNull()?.let { setSpanFromMark(text, it, spans) }
        }

        private fun setSpanFromMark(text: Spannable, mark: Any, vararg spans: Any) {
            val where = text.getSpanStart(mark)
            text.removeSpan(mark)
            val len = text.length
            if (where != len) {
                for (span in spans) {
                    text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }
}