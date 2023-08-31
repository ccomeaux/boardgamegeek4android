package com.boardgamegeek.db.model

data class GamePollResultsLocal(
    val internalId: Long,
    val pollId: Int,
    val pollResultsKey: String,
    val pollResultsPlayers: String?,
    val pollResultsSortIndex: Int,
    val pollResultsResult: List<GamePollResultsResultLocal>,
)

