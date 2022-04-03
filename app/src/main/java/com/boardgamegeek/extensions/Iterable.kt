package com.boardgamegeek.extensions

fun <T> Iterable<T>.forDatabase(delimiter: String) = this.joinToString(delimiter, prefix = delimiter, postfix = delimiter)
