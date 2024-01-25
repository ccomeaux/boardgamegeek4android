package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.MechanicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class MechanicViewModel @Inject constructor(
    application: Application,
    private val repository: MechanicRepository,
) : AndroidViewModel(application) {
    private val _mechanic = MutableLiveData<Pair<Int, CollectionItem.SortType>>()

    fun setId(id: Int) {
        if (_mechanic.value?.first != id)
            _mechanic.value = id to (_mechanic.value?.second ?: CollectionItem.SortType.RATING)
    }

    fun setSort(sortType: CollectionItem.SortType) {
        if (_mechanic.value?.second != sortType)
            _mechanic.value = (_mechanic.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    fun reload() {
        _mechanic.value?.let { _mechanic.value = it }
    }

    val sort = _mechanic.map {
        it.second
    }

    val collection = _mechanic.switchMap { m ->
        liveData {
            emitSource(repository.loadCollection(m.first, m.second).distinctUntilChanged().asLiveData())
        }
    }
}
