package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.CategoryEntity
import com.boardgamegeek.db.model.CategoryWithItemCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT categories.*, COUNT(game_id) AS itemCount FROM categories LEFT OUTER JOIN games_categories ON categories.category_id = games_categories.category_id GROUP BY games_categories.category_id")
    fun loadCategoriesAsLiveData(): Flow<List<CategoryWithItemCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(categoryEntity: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll(): Int
}
