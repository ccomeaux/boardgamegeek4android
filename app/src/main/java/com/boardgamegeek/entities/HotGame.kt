package com.boardgamegeek.entities

data class HotGame(
    val rank: Int = 0,
    val id: Int = 0,
    val name: String = "",
    val thumbnailUrl: String = "",
    val yearPublished: Int = YEAR_UNKNOWN
) {
    companion object {
        const val YEAR_UNKNOWN = GameEntity.YEAR_UNKNOWN
    }
}
