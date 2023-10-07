package com.boardgamegeek.entities

data class GameForPlayStats(
    val id: Int,
    val name: String,
    val playCount: Int,
    val bggRank: Int = GameRankEntity.RANK_UNKNOWN,
    val isOwned: Boolean = false
)
