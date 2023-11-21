package com.boardgamegeek.db.model

import androidx.room.Embedded

data class GamePollResultsWithPoll(
    val totalVotes: Int?,
    @Embedded
    val results: GamePollResultsResultEntity,
)
