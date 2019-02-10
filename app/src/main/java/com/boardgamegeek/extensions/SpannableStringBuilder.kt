package com.boardgamegeek.extensions

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan

fun SpannableStringBuilder.appendBold(boldText: String): SpannableStringBuilder {
    append(boldText)
    if (boldText.isNotEmpty()) {
        setSpan(StyleSpan(Typeface.BOLD), length - boldText.length, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}
