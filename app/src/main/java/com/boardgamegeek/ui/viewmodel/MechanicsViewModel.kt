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
    private val _sortType = MutableLiveData<Mechanic.SortType>()
    val sortType: LiveData<Mechanic.SortType>
        get() = _sortType

    init {
        sort(Mechanic.SortType.ITEM_COUNT)
    }

    val mechanics = sortType.switchMap {
        liveData {
            sortType.value?.let {
                emitSource(repository.loadMechanicsFlow(it).distinctUntilChanged().asLiveData())
            }
        }
    }

    fun sort(sortType: Mechanic.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }
}
