package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.entities.CollectionItemEntity.Companion.filterBySyncedStatues
import com.boardgamegeek.mappers.mapToEntity

class CategoryRepository(val context: Context) {
    private val categoryDao = CategoryDao(context)
    private val collectionDao = CollectionDao(context)

    suspend fun loadCategories(sortBy: CategoryDao.SortType = CategoryDao.SortType.NAME): List<CategoryEntity> {
        return categoryDao.loadCategories(sortBy).map { it.mapToEntity() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForCategory(id, sortBy)
            .map { it.mapToEntity() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = categoryDao.delete()
}
