package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CategoryEntity
import kotlinx.coroutines.CoroutineScope

class CategoryRepository(val application: BggApplication) {
    private val categoryDao = CategoryDao(application)

    suspend fun loadCategoriesAsLiveData(scope: CoroutineScope, sortBy: CategoryDao.SortType = CategoryDao.SortType.NAME): LiveData<List<CategoryEntity>> {
        return categoryDao.loadCategoriesAsLiveData(scope, sortBy)
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType): List<BriefGameEntity> {
        return categoryDao.loadCollection(id, sortBy)
    }
}
