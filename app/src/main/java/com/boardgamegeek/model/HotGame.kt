package com.boardgamegeek.model

data class HotGame(
    val rank: Int = 0,
    val id: Int = 0,
    val name: String = "",
    val thumbnailUrl: String = "",
    val yearPublished: Int = YEAR_UNKNOWN
) {
    companion object {
        const val YEAR_UNKNOWN = Game.YEAR_UNKNOWN
    }
}
