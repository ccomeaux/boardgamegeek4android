package com.boardgamegeek.extensions

val Any.TAG: String
    get() = javaClass.simpleName.take(23)
