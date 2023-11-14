package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GameWithPublishers(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "publisher_id",
        associateBy = Junction(GamePublisherEntity::class)
    )
    val publishers: List<PublisherEntity>
)