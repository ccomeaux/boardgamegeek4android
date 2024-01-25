package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.model.Category
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.Category.Companion.applySort
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.applySort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryRepository(
    val context: Context,
    private val categoryDao: CategoryDao,
    private val collectionDao: CollectionDao,
) {
    fun loadCategoriesAsLiveData(sortBy: Category.SortType): Flow<List<Category>> {
        return categoryDao.loadCategoriesAsLiveData()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadCollectionFlow(id: Int, sortBy: CollectionItem.SortType): Flow<List<CollectionItem>> {
        return collectionDao.loadForCategoryFlow(id)
            .map { it.map { entity -> entity.mapToModel() } }
            .map { list ->
                list.filter { it.deleteTimestamp == 0L }
                    .filter { it.filterBySyncedStatues(context) }
                    .applySort(sortBy)
            }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { categoryDao.deleteAll() }
}
