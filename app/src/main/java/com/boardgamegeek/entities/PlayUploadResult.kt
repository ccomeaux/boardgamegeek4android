package com.boardgamegeek.entities

data class PlayUploadResult(
    val play: PlayEntity,
    val status: Status,
    val numberOfPlays: Int = 0,
    val errorMessage: String = ""

) {
    enum class Status {
        NEW,
        UPDATE,
        ERROR,
    }

    companion object {
        fun success(play: PlayEntity, playId:Int, numberOfPlays: Int): PlayUploadResult {
            val status = if (play.playId == playId) Status.UPDATE else Status.NEW
            return PlayUploadResult(play.copy(playId = playId), status, numberOfPlays = numberOfPlays)
        }

        fun error(play: PlayEntity, errorMessage: String): PlayUploadResult {
            return PlayUploadResult(play, Status.ERROR, errorMessage = errorMessage)
        }
    }
}