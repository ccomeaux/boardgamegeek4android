package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GameWithMechanics(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "mechanic_id",
        associateBy = Junction(GameMechanicEntity::class)
    )
    val mechanics: List<MechanicEntity>
)