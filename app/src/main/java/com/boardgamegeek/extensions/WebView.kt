package com.boardgamegeek.extensions

import android.text.TextUtils
import android.webkit.WebView

fun WebView.setWebViewText(html: String) {
    this.loadDataWithBaseURL(null, fixInternalLinks(html), "text/html", "UTF-8", null)
}

private fun fixInternalLinks(text: String): String? {
    // ensure internal, path-only links are complete with the hostname
    if (TextUtils.isEmpty(text)) return ""
    var fixedText = text.replace("<a\\s+href=\"/".toRegex(), "<a href=\"https://www.boardgamegeek.com/")
    fixedText = fixedText.replace("<img\\s+src=\"//".toRegex(), "<img src=\"https://")
    return fixedText
}
