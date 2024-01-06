package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    application: Application,
    private val repository: CategoryRepository,
) : AndroidViewModel(application) {
    enum class CollectionSort {
        NAME, RATING
    }

    private val _category = MutableLiveData<Pair<Int, CollectionSort>>()

    fun setId(id: Int) {
        if (_category.value?.first != id)
            _category.value = id to (_category.value?.second ?: CollectionSort.RATING)
    }

    fun setSort(sortType: CollectionSort) {
        if (_category.value?.second != sortType)
            _category.value = (_category.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    fun refresh() {
        _category.value?.let { _category.value = it }
    }

    val sort = _category.map {
        it.second
    }

    val collection = _category.switchMap { c ->
        liveData {
            val sortBy = when (c.second) {
                CollectionSort.NAME -> CategoryRepository.CollectionSortType.NAME
                CollectionSort.RATING -> CategoryRepository.CollectionSortType.RATING
            }
            emitSource(repository.loadCollection(c.first, sortBy))
        }
    }
}
