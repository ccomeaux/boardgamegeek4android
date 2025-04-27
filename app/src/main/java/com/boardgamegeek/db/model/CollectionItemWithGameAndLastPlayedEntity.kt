package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class CollectionItemWithGameAndLastPlayedEntity (
    @Embedded
    val item: CollectionItemEntity,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "game_id",
    )
    val game: GameEntity,
    val lastPlayedDate: String?,
)
