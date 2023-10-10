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
        NAME, ITEM_COUNT
    }

    private val _sort = MutableLiveData<MechanicsSort>()
    val sort: LiveData<MechanicsSort>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val mechanics = sort.switchMap {
        liveData {
            emit(repository.loadMechanics(it.sortBy))
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value?.sortType != sortType) {
            _sort.value = when (sortType) {
                SortType.NAME -> MechanicsSort.ByName()
                SortType.ITEM_COUNT -> MechanicsSort.ByItemCount()
            }
        }
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    sealed class MechanicsSort {
        abstract val sortType: SortType
        abstract val sortBy: MechanicRepository.SortType

        class ByName : MechanicsSort() {
            override val sortType = SortType.NAME
            override val sortBy = MechanicRepository.SortType.NAME
        }

        class ByItemCount : MechanicsSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = MechanicRepository.SortType.ITEM_COUNT
        }
    }
}
