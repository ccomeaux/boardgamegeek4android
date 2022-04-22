package com.boardgamegeek.extensions

import android.util.SparseBooleanArray
import androidx.core.util.forEach

fun SparseBooleanArray.toggle(position: Int) {
    if (this[position, false]) {
        this.delete(position)
    } else {
        this.put(position, true)
    }
}

fun SparseBooleanArray.filterTrue(): List<Int> {
    val filteredList = mutableListOf<Int>()
    this.forEach { key, value ->
        if (value) filteredList += key
    }
    return filteredList
}
