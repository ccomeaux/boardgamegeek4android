package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index("category_id", name = "index_categories_category_id", unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
)