package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CategoryEntity
import com.boardgamegeek.db.model.CategoryWithItemCount
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.GameDetail

fun CategoryWithItemCount.mapToModel() = Category(
    id = category.categoryId,
    name = category.categoryName,
    itemCount = itemCount,
)

fun CategoryEntity.mapToGameDetail() = GameDetail(
    id = categoryId,
    name = categoryName,
)
