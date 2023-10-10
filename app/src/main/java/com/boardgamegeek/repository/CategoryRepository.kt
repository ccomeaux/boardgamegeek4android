package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.mappers.mapToModel

class CategoryRepository(
    val context: Context,
    private val categoryDao: CategoryDao,
) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
    }

    suspend fun loadCategories(sortBy: SortType = SortType.NAME): List<Category> {
        return categoryDao.loadCategories()
            .sortedBy {
                when (sortBy) {
                    SortType.NAME -> it.categoryName
                    SortType.ITEM_COUNT -> 0 // TODO
                }.toString()
            }
            .map { it.mapToModel() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForCategory(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun deleteAll() = categoryDao.deleteAll()
}
