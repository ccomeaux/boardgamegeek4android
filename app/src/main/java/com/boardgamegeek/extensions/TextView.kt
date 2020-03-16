@file:JvmName("TextViewUtils")

package com.boardgamegeek.extensions

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.view.isVisible

fun TextView.setTextOrHide(text: CharSequence?) {
    this.text = text
    visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
}

fun TextView.setTextOrHide(@StringRes textResId: Int) {
    isVisible = if (textResId == 0) {
        false
    } else {
        this.setText(textResId)
        true
    }
}

fun TextView.setTextMaybeHtml(text: String?) {
    when {
        text == null -> this.text = ""
        text.isBlank() -> this.text = ""
        text.contains("<") && text.contains(">") || text.contains("&") && text.contains(";") -> {
            var html = text
            // Fix up problematic HTML
            // replace DIVs with BR
            html = html.replace("[<]div[^>]*[>]".toRegex(), "")
            html = html.replace("[<]/div[>]".toRegex(), "<br/>")
            // remove all P tags
            html = html.replace("[<](/)?p[>]".toRegex(), "")
            // remove trailing BRs
            html = html.replace("(<br\\s?/>)+$".toRegex(), "")
            // replace 3+ BRs with a double
            html = html.replace("(<br\\s?/>){3,}".toRegex(), "<br/><br/>")
            // use BRs instead of new line character
            html = html.replace("\n".toRegex(), "<br/>")
            html = fixInternalLinks(html)

            @Suppress("DEPRECATION")
            val spanned = Html.fromHtml(html)
            this.text = spanned
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
