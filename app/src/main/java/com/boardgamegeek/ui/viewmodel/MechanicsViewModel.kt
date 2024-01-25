package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.repository.MechanicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class MechanicsViewModel @Inject constructor(
    application: Application,
    private val repository: MechanicRepository,
) : AndroidViewModel(application) {
    private val _sort = MutableLiveData<Mechanic.SortType>()
    val sort: LiveData<Mechanic.SortType>
        get() = _sort

    init {
        sort(Mechanic.SortType.ITEM_COUNT)
    }

    val mechanics = sort.switchMap {
        liveData {
            sort.value?.let {
                emitSource(repository.loadMechanicsFlow(it).distinctUntilChanged().asLiveData())
            }
        }
    }

    fun sort(sortType: Mechanic.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun reload() {
        _sort.value?.let { _sort.value = it }
    }
}
