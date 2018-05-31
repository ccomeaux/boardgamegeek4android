package com.boardgamegeek.entities

data class GamePollResultEntity(
        val level: Int = 0,
        val value: String = "",
        val numberOfVotes: Int = 0
)
