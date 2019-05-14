package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.CategoryDao
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.repository.CategoryRepository

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    private val repository = CategoryRepository(getApplication())

    private val _sort = MutableLiveData<CategoriesSort>()
    val sort: LiveData<CategoriesSort>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val categories: LiveData<List<CategoryEntity>> = Transformations.switchMap(sort) {
        repository.loadCategories(it.sortBy)
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> CategoriesSortByName()
            SortType.ITEM_COUNT -> CategoriesSortByItemCount()
        }
    }
}

sealed class CategoriesSort {
    abstract val sortType: CategoriesViewModel.SortType
    abstract val sortBy: CategoryDao.SortType
}

class CategoriesSortByName : CategoriesSort() {
    override val sortType = CategoriesViewModel.SortType.NAME
    override val sortBy = CategoryDao.SortType.NAME
}

class CategoriesSortByItemCount : CategoriesSort() {
    override val sortType = CategoriesViewModel.SortType.ITEM_COUNT
    override val sortBy = CategoryDao.SortType.ITEM_COUNT
}