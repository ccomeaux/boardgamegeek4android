package com.boardgamegeek.model

data class GameComment(
    val username: String,
    val rating: Double,
    val comment: String,
 ) {
    enum class SortType {
        Rating,
        Comment,
    }
}
