package com.boardgamegeek.extensions

import android.text.TextUtils

fun CharSequence.trimTrailingWhitespace(): CharSequence {
    var i = length
    do {
        --i
    } while (i >= 0 && Character.isWhitespace(this[i]))
    return subSequence(0, i + 1)
}

fun List<CharSequence>.concat(): CharSequence {
    return TextUtils.concat(*this.toTypedArray())
}

fun List<CharSequence>.joinTo(separator: CharSequence): CharSequence {
    val newList = mutableListOf<CharSequence>()
    this.forEachIndexed { index, charSequence ->
        if (index > 0) newList.add(separator)
        newList.add(charSequence)
    }
    return newList.concat()
}
