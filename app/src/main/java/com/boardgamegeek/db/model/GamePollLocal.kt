package com.boardgamegeek.db.model

data class GamePollLocal(
    val internalId: Long,
    val gameId: Int,
    val pollName: String,
    val pollTitle: String,
    val pollTotalVotes: Int,
    val results: List<GamePollResultsLocal>,
)
