package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.repository.MechanicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MechanicsViewModel @Inject constructor(
    application: Application,
    private val repository: MechanicRepository,
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

    val mechanics = sort.switchMap {
        liveData {
            sort.value?.let {
                val sort = when (it) {
                    SortType.NAME -> MechanicRepository.SortType.NAME
                    SortType.ITEM_COUNT -> MechanicRepository.SortType.ITEM_COUNT
                }
                emitSource(repository.loadMechanicsAsLiveData(sort).distinctUntilChanged())
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
