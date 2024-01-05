package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun loadCategoriesAsLiveData(sortBy: SortType = SortType.NAME): LiveData<List<Category>> = withContext(Dispatchers.Default) {
        categoryDao.loadCategoriesAsLiveData().map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Category> { it.itemCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> = withContext(Dispatchers.Default) {
        if (id == BggContract.INVALID_ID) emptyList()
        else withContext(Dispatchers.IO) { collectionDao.loadForCategory(id) }
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }
            .sortedWith(
                if (sortBy == CollectionSortType.RATING)
                    compareByDescending<CollectionItem> { it.rating }
                        .thenByDescending { it.isFavorite }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                else
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
            )
    }

    suspend fun deleteAll() = categoryDao.deleteAll()
}
