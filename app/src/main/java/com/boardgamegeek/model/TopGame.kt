package com.boardgamegeek.model

data class TopGame(
    val id: Int,
    val name: String,
    val rank: Int,
    val yearPublished: Int,
    val thumbnailUrl: String,
) {
    companion object {
        const val YEAR_UNKNOWN = Game.YEAR_UNKNOWN
    }
}
