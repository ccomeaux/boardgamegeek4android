package com.boardgamegeek

fun Int.clamp(min: Int, max: Int) = Math.max(min, Math.min(max, this))

/**
 * Gets the ordinal (1st) for the given cardinal (1)
 */
fun Int.toOrdinal(): String {
    if (this < 0) {
        return "-th"
    }

    val c = toString()
    val onesPlace = c.substring(c.length - 1)
    val tensPlace = if (c.length > 1) {
        c.substring(c.length - 2, c.length - 1)
    } else {
        "0"
    }
    return when {
        tensPlace == "1" -> c + "th"
        onesPlace == "1" -> c + "st"
        onesPlace == "2" -> c + "nd"
        onesPlace == "3" -> c + "rd"
        else -> c + "th"
    }
}