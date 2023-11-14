package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GameWithCategories(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "game_id",
        entityColumn = "category_id",
        associateBy = Junction(GameCategoryEntity::class)
    )
    val categories: List<CategoryEntity>
)