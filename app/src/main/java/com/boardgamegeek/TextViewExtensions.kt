package com.boardgamegeek

import android.view.View
import android.widget.TextView

fun TextView.setTextOrHide(text: CharSequence?) {
    this.text = text
    visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
}
