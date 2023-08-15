package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CategoryLocal
import com.boardgamegeek.entities.CategoryEntity

fun CategoryLocal.mapToEntity() = CategoryEntity(
    id = id,
    name = name,
    itemCount = itemCount,
)