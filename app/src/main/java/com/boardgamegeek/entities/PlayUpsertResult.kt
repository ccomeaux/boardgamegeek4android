package com.boardgamegeek.entities

data class PlayUpsertResult(
    val play: PlayEntity,
    val status: Status,
    val numberOfPlays: Int = 0,
    val errorMessage: String = "",
) {
    enum class Status {
        NEW,
        UPDATE,
        ERROR,
    }

    companion object {
        fun success(play: PlayEntity, playId:Int = play.playId, numberOfPlays: Int = 0): PlayUpsertResult {
            val status = if (play.playId == playId) Status.UPDATE else Status.NEW
            return PlayUpsertResult(play.copy(playId = playId), status, numberOfPlays = numberOfPlays)
        }

        fun error(play: PlayEntity, errorMessage: String): PlayUpsertResult {
            return PlayUpsertResult(play, Status.ERROR, errorMessage = errorMessage)
        }
    }
}