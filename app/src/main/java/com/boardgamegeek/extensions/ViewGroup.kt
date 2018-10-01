package com.boardgamegeek.extensions

import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

@Suppress("NOTHING_TO_INLINE")
inline fun ViewGroup.inflate(@LayoutRes layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

@Suppress("NOTHING_TO_INLINE")
inline fun ViewGroup.children() = object : Iterable<View> {
    override fun iterator() = object : Iterator<View> {
        var index = 0
        override fun hasNext() = index < childCount
        override fun next() = getChildAt(index++)
    }
}