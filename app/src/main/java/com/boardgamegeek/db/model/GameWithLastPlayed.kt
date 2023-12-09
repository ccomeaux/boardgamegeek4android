package com.boardgamegeek.db.model

import androidx.room.Embedded

data class GameWithLastPlayed(
    @Embedded
    val game: GameEntity?,
    val lastPlayedDate: String?,
)
