package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import com.boardgamegeek.db.model.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun loadCategories(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun deleteAll(): Int
}