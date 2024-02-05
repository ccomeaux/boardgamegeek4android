package com.boardgamegeek.model

data class Location(
    val name: String,
    val playCount: Int
) {
    enum class SortType {
        NAME, PLAY_COUNT
    }
}
