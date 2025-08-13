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
    private val _sortType = MutableLiveData<Category.SortType>()
    val sortType: LiveData<Category.SortType>
        get() = _sortType

    init {
        sort(Category.SortType.ITEM_COUNT)
    }

    val categories = sortType.switchMap {
        liveData {
            sortType.value?.let {
                emitSource(repository.loadCategoriesFlow(it).distinctUntilChanged().asLiveData())
            }
        }
    }

    fun sort(sortType: Category.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }
}
