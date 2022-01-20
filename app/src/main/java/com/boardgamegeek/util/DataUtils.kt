package com.boardgamegeek.util

object DataUtils {
    fun generatePollResultsKey(level: Int, value: String): String {
        return if (level <= 0) {
            generatePollResultsKey("", value)
        } else {
            generatePollResultsKey(level.toString(), value)
        }
    }

    fun generatePollResultsKey(level: String?, value: String): String {
        return if (level.isNullOrBlank()) {
            value.substringBefore(" ")
        } else {
            level
        }
    }
}
