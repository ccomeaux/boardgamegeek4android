package com.boardgamegeek.db.model

data class PlayPlayerLocal(
    val internalId: Long,
    val internalPlayId: Long,
    val username: String?,
    val userId: Int?,
    val name: String?,
    val startingPosition: String?,
    val color: String?,
    val score: String?,
    val isNew: Boolean?,
    val rating: Double?,
    val isWin: Boolean?,
)
