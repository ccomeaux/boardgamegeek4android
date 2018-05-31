package com.boardgamegeek.entities

data class GamePlayerPollEntity(
        val totalVotes: Int = 0,
        val playerCount: Int = 0,
        val recommendation: Int = 0
)
