package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.MechanicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MechanicViewModel @Inject constructor(
    application: Application,
    private val repository: MechanicRepository,
) : AndroidViewModel(application) {
    enum class CollectionSort {
        NAME, RATING
    }

    private val _mechanic = MutableLiveData<Pair<Int, CollectionSort>>()

    fun setId(id: Int) {
        if (_mechanic.value?.first != id)
            _mechanic.value = id to CollectionSort.RATING
    }

    fun setSort(sortType: CollectionSort) {
        if (_mechanic.value?.second != sortType)
            _mechanic.value = (_mechanic.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    fun refresh() {
        _mechanic.value?.let { _mechanic.value = it }
    }

    val sort = _mechanic.map {
        it.second
    }

    val collection = _mechanic.switchMap { m ->
        liveData {
            val sortBy = when (m.second) {
                CollectionSort.NAME -> MechanicRepository.CollectionSortType.NAME
                CollectionSort.RATING -> MechanicRepository.CollectionSortType.RATING
            }
            emitSource(repository.loadCollection(m.first, sortBy))
        }
    }
}
