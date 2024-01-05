package com.boardgamegeek.db.model

import androidx.room.Embedded

data class CategoryWithItemCount (
    @Embedded
    val category: CategoryEntity,
    val itemCount: Int,
)
