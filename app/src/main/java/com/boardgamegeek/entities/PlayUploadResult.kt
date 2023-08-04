package com.boardgamegeek.entities

data class PlayUploadResult private constructor(
    val play: PlayEntity,
    val status: Status = Status.NEW,
    val numberOfPlays: Int = 0,
) {
    enum class Status {
        NO_OP,
        NEW,
        UPDATE,
        DELETE,
    }

    companion object {
        fun noOp(play: PlayEntity) = PlayUploadResult(play, Status.NO_OP, 0)

        fun upsert(play: PlayEntity, playId: Int, numberOfPlays: Int = 0): PlayUploadResult {
            val status = if (play.playId == playId) Status.UPDATE else Status.NEW
            return PlayUploadResult(play.copy(playId = playId), status, numberOfPlays)
        }

        fun delete(play: PlayEntity) = PlayUploadResult(play, Status.DELETE, -1)
    }
}