package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GameWithDesigners(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "designer_id",
        associateBy = Junction(GameDesignerEntity::class)
    )
    val designers: List<DesignerEntity>
)