package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CategoryEntity
import com.boardgamegeek.db.model.CategoryLocal
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.GameDetail

fun CategoryEntity.mapToModel() = Category(
    id = categoryId,
    name = categoryName,
)

fun CategoryLocal.mapToGameDetail() = GameDetail(
    id = id,
    name = name,
)
