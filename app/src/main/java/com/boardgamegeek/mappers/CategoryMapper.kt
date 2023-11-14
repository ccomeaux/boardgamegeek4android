package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CategoryEntity
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.GameDetail

fun CategoryEntity.mapToModel() = Category(
    id = categoryId,
    name = categoryName,
)

fun CategoryEntity.mapToGameDetail() = GameDetail(
    id = categoryId,
    name = categoryName,
)
