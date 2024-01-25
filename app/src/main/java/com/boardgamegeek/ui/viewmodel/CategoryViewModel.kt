package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    application: Application,
    private val repository: CategoryRepository,
) : AndroidViewModel(application) {
    private val _category = MutableLiveData<Pair<Int, CollectionItem.SortType>>()

    fun setId(id: Int) {
        if (_category.value?.first != id)
            _category.value = id to (_category.value?.second ?: CollectionItem.SortType.RATING)
    }

    fun setSort(sortType: CollectionItem.SortType) {
        if (_category.value?.second != sortType)
            _category.value = (_category.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    fun reload() {
        _category.value?.let { _category.value = it }
    }

    val sort = _category.map {
        it.second
    }

    val collection = _category.switchMap { c ->
        liveData {
            emitSource(repository.loadCollectionFlow(c.first, c.second).distinctUntilChanged().asLiveData())
        }
    }
}
