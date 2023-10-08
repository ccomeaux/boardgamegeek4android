package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CategoryLocal
import com.boardgamegeek.entities.Category
import com.boardgamegeek.entities.GameDetail

fun CategoryLocal.mapToModel() = Category(
    id = id,
    name = name,
    itemCount = itemCount,
)

fun CategoryLocal.mapToGameDetail() = GameDetail(
    id = id,
    name = name,
)
