package com.boardgamegeek.extensions

import java.util.*

fun <E> Array<E>?.formatList(): String {
    return if (this == null) "" else Arrays.asList(*this).formatList<E>()
}
