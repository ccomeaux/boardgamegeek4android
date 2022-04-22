package com.boardgamegeek.extensions

fun <E> Array<E>?.formatList(): String {
    return if (this == null) "" else listOf(*this).formatList()
}
