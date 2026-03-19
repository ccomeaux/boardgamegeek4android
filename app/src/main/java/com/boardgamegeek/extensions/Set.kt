package com.boardgamegeek.extensions

fun <T> MutableSet<T>.toggle(element: T, add: Boolean = !this.contains(element)): Boolean {
    return if (add) {
        add(element)
        true
    } else {
        remove(element)
        false
    }
}