package com.boardgamegeek

fun String?.replaceHtmlLineFeeds(): String {
    return if (this == null || this.isBlank()) "" else this.replace("&#10;", "\n")
}

fun String?.sortName(sortIndex: Int): String {
    if (this == null) return ""
    if (sortIndex <= 1 || sortIndex > length) return this
    val i = sortIndex - 1
    return "${substring(i)}, ${substring(0, i).trim()}"
}
