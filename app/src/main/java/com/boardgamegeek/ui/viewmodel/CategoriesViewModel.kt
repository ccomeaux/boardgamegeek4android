package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.db.CategoryDao
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

    val categories = sort.switchMap {
        liveData {
            emitSource(repository.loadCategoriesAsLiveData(viewModelScope, it.sortBy))
        }
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> CategoriesSort.ByName()
            SortType.ITEM_COUNT -> CategoriesSort.ByItemCount()
        }
    }

    sealed class CategoriesSort {
        abstract val sortType: SortType
        abstract val sortBy: CategoryDao.SortType

        class ByName : CategoriesSort() {
            override val sortType = SortType.NAME
            override val sortBy = CategoryDao.SortType.NAME
        }

        class ByItemCount : CategoriesSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = CategoryDao.SortType.ITEM_COUNT
        }
    }
}
