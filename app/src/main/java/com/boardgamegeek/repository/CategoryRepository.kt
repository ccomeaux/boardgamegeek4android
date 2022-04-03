package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CategoryEntity

class CategoryRepository(val application: BggApplication) {
    private val categoryDao = CategoryDao(application)

    suspend fun loadCategories(sortBy: CategoryDao.SortType = CategoryDao.SortType.NAME): List<CategoryEntity> {
        return categoryDao.loadCategories(sortBy)
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType): List<BriefGameEntity> {
        return categoryDao.loadCollection(id, sortBy)
    }

    suspend fun delete() = categoryDao.delete()
}
