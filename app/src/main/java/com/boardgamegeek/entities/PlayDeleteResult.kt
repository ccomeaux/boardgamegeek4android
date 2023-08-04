package com.boardgamegeek.entities

data class PlayDeleteResult(
    val play: PlayEntity,
    val errorMessage: String = "",
) {
    companion object {
        fun success(play: PlayEntity): PlayDeleteResult {
            return PlayDeleteResult(play)
        }

        fun error(play: PlayEntity, errorMessage: String): PlayDeleteResult {
            return PlayDeleteResult(play, errorMessage = errorMessage)
        }
    }
}