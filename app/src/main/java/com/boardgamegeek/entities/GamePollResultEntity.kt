package com.boardgamegeek.entities

data class GamePollResultEntity(
        var level: Int = 0,
        var value: String = "",
        var numberOfVotes: Int = 0
)
