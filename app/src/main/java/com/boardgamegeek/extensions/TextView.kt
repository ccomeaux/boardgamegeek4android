@file:JvmName("TextViewUtils")

package com.boardgamegeek.extensions

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible

fun TextView.setTextOrHide(text: CharSequence?) {
    this.text = text
    isVisible = !text.isNullOrBlank()
}

fun TextView.setTextOrHide(@StringRes textResId: Int) {
    isVisible = if (textResId == 0) {
        false
    } else {
        this.setText(textResId)
        true
    }
}

@JvmOverloads
fun TextView.setTextMaybeHtml(text: String?, fromHtmlFlags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY, useLinkMovementMethod: Boolean = true, tagHandler: Html.TagHandler? = null) {
    when {
        text == null -> this.text = ""
        text.isBlank() -> this.text = ""
        text.contains("<") && text.contains(">") || text.contains("&") && text.contains(";") -> {
            var html = text.trim()
            // Fix up problematic HTML
            // replace DIVs with BR
            html = html.replace("[<]div[^>]*[>]".toRegex(), "")
            html = html.replace("[<]/div[>]".toRegex(), "<br/>")
            // remove all P tags
            html = html.replace("[<](/)?p[>]".toRegex(), "")
            // remove trailing BRs
            html = html.replace("(<br\\s?/>)+$".toRegex(), "")
            // use BRs instead of &#10; (ASCII 10 = new line)
            html = html.replace("&#10;".toRegex(), "<br/>")
            // use BRs instead of new line character
            html = html.replace("\n".toRegex(), "<br/>")
            // replace 3+ BRs with a double
            html = html.replace("(<br\\s?/>){3,}".toRegex(), "<br/><br/>")
            html = fixInternalLinks(html)

            val spanned = HtmlCompat.fromHtml(html, fromHtmlFlags, null, tagHandler)
            this.text = spanned.trim()
            if (useLinkMovementMethod)
                this.movementMethod = LinkMovementMethod.getInstance()
        }
        else -> this.text = text
    }
}

fun TextView.setTextViewBackground(@ColorInt color: Int) {
    this.setViewBackground(color)
    this.setTextColor(color.getTextColor())
}

@ColorInt
fun View.setTextViewBackground(color: Int): Int {
    this.setViewBackground(color)
    return color.getTextColor()
}

private fun fixInternalLinks(html: String): String {
    // ensure internal, path-only links are complete with the hostname
    if (html.isBlank()) return ""
    var fixedText = html.replace("<a\\s+href=\"/".toRegex(), "<a href=\"https://www.boardgamegeek.com/")
    fixedText = fixedText.replace("<img\\s+src=\"//".toRegex(), "<img src=\"https://")
    return fixedText
}
