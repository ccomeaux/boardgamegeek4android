package com.boardgamegeek.entities

data class GamePlayerPollResultsEntity(
        val totalVotes: Int = 0,
        val playerCount: String = "0",
        val recommendation: Int = 0
)
