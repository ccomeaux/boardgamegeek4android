package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CategoryRepository

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    enum class CollectionSort {
        NAME, RATING
    }

    private val repository = CategoryRepository(getApplication())

    private val _category = MutableLiveData<Pair<Int, CollectionSort>>()

    fun setId(id: Int) {
        if (_category.value?.first != id) _category.value = id to (_category.value?.second ?: CollectionSort.RATING)
    }

    fun setSort(sortType: CollectionSort) {
        if (_category.value?.second != sortType)
            _category.value = (_category.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    val sort: LiveData<CollectionSort> = Transformations.map(_category) {
        it.second
    }

    val collection: LiveData<List<BriefGameEntity>> = Transformations.switchMap(_category) { c ->
        when (c.first) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> when (c.second) {
                CollectionSort.NAME -> repository.loadCollection(c.first, CollectionDao.SortType.NAME)
                CollectionSort.RATING -> repository.loadCollection(c.first, CollectionDao.SortType.RATING)
            }
        }
    }

    fun refresh() {
        _category.value?.let { _category.value = it }
    }
}
