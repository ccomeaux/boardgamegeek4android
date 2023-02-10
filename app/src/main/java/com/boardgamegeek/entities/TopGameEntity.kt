package com.boardgamegeek.entities

data class TopGameEntity(
    val id: Int,
    val name: String,
    val rank: Int,
    val yearPublished: Int,
    val thumbnailUrl: String,
) {
    companion object {
        const val YEAR_UNKNOWN = GameEntity.YEAR_UNKNOWN
    }
}
