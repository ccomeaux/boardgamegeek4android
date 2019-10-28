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
import com.boardgamegeek.repository.MechanicRepository

class MechanicViewModel(application: Application) : AndroidViewModel(application) {
    enum class CollectionSort {
        NAME, RATING
    }

    private val repository = MechanicRepository(getApplication())

    private val _mechanic = MutableLiveData<Pair<Int, CollectionSort>>()

    fun setId(id: Int) {
        if (_mechanic.value?.first != id) _mechanic.value = id to CollectionSort.RATING
    }

    fun setSort(sortType: CollectionSort) {
        if (_mechanic.value?.second != sortType)
            _mechanic.value = (_mechanic.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    val sort: LiveData<CollectionSort> = Transformations.map(_mechanic) {
        it.second
    }

    val collection: LiveData<List<BriefGameEntity>> = Transformations.switchMap(_mechanic) { m ->
        when (m.first) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> when (m.second) {
                CollectionSort.NAME -> repository.loadCollection(m.first, CollectionDao.SortType.NAME)
                CollectionSort.RATING -> repository.loadCollection(m.first, CollectionDao.SortType.RATING)
            }
        }
    }

    fun refresh() {
        _mechanic.value?.let { _mechanic.value = it }
    }
}
