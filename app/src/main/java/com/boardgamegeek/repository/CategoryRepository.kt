package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.entities.MechanicEntity

class CategoryRepository (val application: BggApplication){
    private val categoryDao = CategoryDao(application)

    fun loadCategories(sortBy: CategoryDao.SortType = CategoryDao.SortType.NAME): LiveData<List<CategoryEntity>> {
        return categoryDao.loadCategoriesAsLiveData(sortBy)
    }
}