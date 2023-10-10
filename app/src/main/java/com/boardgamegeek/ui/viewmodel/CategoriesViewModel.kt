package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    application: Application,
    private val repository: CategoryRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    private val _sort = MutableLiveData<CategoriesSort>()
    val sort: LiveData<CategoriesSort>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val categories = sort.switchMap {
        liveData {
            emit(repository.loadCategories(it.sortBy))
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value?.sortType != sortType) {
            _sort.value = when (sortType) {
                SortType.NAME -> CategoriesSort.ByName()
                SortType.ITEM_COUNT -> CategoriesSort.ByItemCount()
            }
        }
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    sealed class CategoriesSort {
        abstract val sortType: SortType
        abstract val sortBy: CategoryRepository.SortType

        class ByName : CategoriesSort() {
            override val sortType = SortType.NAME
            override val sortBy = CategoryRepository.SortType.NAME
        }

        class ByItemCount : CategoriesSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = CategoryRepository.SortType.ITEM_COUNT
        }
    }
}
