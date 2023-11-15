package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.boardgamegeek.db.model.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun loadCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE category_id = :id")
    suspend fun loadCategory(id: Int): CategoryEntity?

    @Upsert
    suspend fun upsert(categoryEntity: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll(): Int
}