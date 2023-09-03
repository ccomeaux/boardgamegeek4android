package com.boardgamegeek.db.model

data class GamesExpansionLocal(
    val internalId: Long,
    val gameId: Int,
    val expansionId: Int,
    val expansionName: String,
    val inbound: Boolean?,
)
