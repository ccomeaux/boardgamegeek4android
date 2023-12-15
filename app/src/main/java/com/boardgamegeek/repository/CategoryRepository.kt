package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract

class CategoryRepository(
    val context: Context,
    private val categoryDao: CategoryDao,
    private val collectionDao: CollectionDao,
) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    enum class CollectionSortType {
        NAME, RATING
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

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> {
        if (id == BggContract.INVALID_ID) return emptyList()
        return collectionDao.loadForCategory(id)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }
            .sortedWith(
                if (sortBy == CollectionSortType.RATING)
                    compareByDescending<CollectionItem> { it.rating }.thenByDescending { it.isFavorite }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                else
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
            )
    }

    suspend fun deleteAll() = categoryDao.deleteAll()
}
