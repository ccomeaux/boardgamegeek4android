package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CategoryLocal
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.entities.GameDetailEntity

fun CategoryLocal.mapToEntity() = CategoryEntity(
    id = id,
    name = name,
    itemCount = itemCount,
)

fun CategoryLocal.mapToGameDetailEntity() = GameDetailEntity(
    id = id,
    name = name,
)
