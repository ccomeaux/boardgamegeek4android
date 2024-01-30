package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Category
import com.boardgamegeek.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    application: Application,
    private val repository: CategoryRepository,
) : AndroidViewModel(application) {
    private val _sort = MutableLiveData<Category.SortType>()
    val sort: LiveData<Category.SortType>
        get() = _sort

    init {
        sort(Category.SortType.ITEM_COUNT)
    }

    val categories = sort.switchMap {
        liveData {
            sort.value?.let {
                emitSource(repository.loadCategoriesFlow(it).distinctUntilChanged().asLiveData())
            }
        }
    }

    fun sort(sortType: Category.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun reload() {
        _sort.value?.let { _sort.value = it }
    }
}
