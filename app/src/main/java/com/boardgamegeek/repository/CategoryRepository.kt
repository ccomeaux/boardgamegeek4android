package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CategoryEntity

class CategoryRepository(val application: BggApplication) {
    private val categoryDao = CategoryDao(application)

    fun loadCategories(sortBy: CategoryDao.SortType = CategoryDao.SortType.NAME): LiveData<List<CategoryEntity>> {
        return categoryDao.loadCategoriesAsLiveData(sortBy)
    }

    fun loadCollection(id: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>>? {
        return categoryDao.loadCollectionAsLiveData(id, sortBy)
    }
}
