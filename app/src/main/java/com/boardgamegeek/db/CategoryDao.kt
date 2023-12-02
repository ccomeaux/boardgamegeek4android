package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun loadCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE category_id = :id")
    suspend fun loadCategory(id: Int): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(categoryEntity: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll(): Int
}