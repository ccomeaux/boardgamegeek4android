package com.boardgamegeek.entities

data class PlayUpsertResult(
    val play: PlayEntity,
    val status: Status = Status.NEW,
    val numberOfPlays: Int = 0,
) {
    enum class Status {
        NEW,
        UPDATE,
    }
}