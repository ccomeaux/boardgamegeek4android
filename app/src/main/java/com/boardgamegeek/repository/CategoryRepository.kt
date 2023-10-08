package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.Category
import com.boardgamegeek.entities.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.mappers.mapToModel

class CategoryRepository(val context: Context) {
    private val categoryDao = CategoryDao(context)
    private val collectionDao = CollectionDao(context)

    suspend fun loadCategories(sortBy: CategoryDao.SortType = CategoryDao.SortType.NAME): List<Category> {
        return categoryDao.loadCategories(sortBy).map { it.mapToModel() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForCategory(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = categoryDao.delete()
}
