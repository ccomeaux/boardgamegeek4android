package com.boardgamegeek.model

data class GameForPlayStats(
    val id: Int,
    val name: String,
    val playCount: Int,
    val bggRank: Int = GameRank.RANK_UNKNOWN,
    val isOwned: Boolean = false
)