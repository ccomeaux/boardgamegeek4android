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
        NAME,
        ITEM_COUNT
    }

    private val _sort = MutableLiveData<SortType>()
    val sort: LiveData<SortType>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val categories = sort.switchMap {
        liveData {
            sort.value?.let {
                val sort = when (it) {
                    SortType.NAME -> CategoryRepository.SortType.NAME
                    SortType.ITEM_COUNT -> CategoryRepository.SortType.ITEM_COUNT
                }
                emitSource(repository.loadCategoriesAsLiveData(sort).distinctUntilChanged())
            }
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }
}
