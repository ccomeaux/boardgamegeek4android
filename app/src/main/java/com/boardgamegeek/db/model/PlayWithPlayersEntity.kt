package com.boardgamegeek.db.model

import androidx.room.*

data class PlayWithPlayersEntity(
    @Embedded
    val play: PlayEntity,
    @Relation(parentColumn = "_id", entityColumn = "_play_id")
    val players: List<PlayPlayerEntity>,
)
