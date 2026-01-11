package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.repository.MechanicRepository

class MechanicsViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    private val repository = MechanicRepository(getApplication())

    private val _sort = MutableLiveData<MechanicsSort>()
    val sort: LiveData<MechanicsSort>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val mechanics: LiveData<List<MechanicEntity>> = sort.switchMap() {
        repository.loadMechanics(it.sortBy)
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> MechanicsSortByName()
            SortType.ITEM_COUNT -> MechanicsSortByItemCount()
        }
    }
}

sealed class MechanicsSort {
    abstract val sortType: MechanicsViewModel.SortType
    abstract val sortBy: MechanicDao.SortType
}

class MechanicsSortByName : MechanicsSort() {
    override val sortType = MechanicsViewModel.SortType.NAME
    override val sortBy = MechanicDao.SortType.NAME
}

class MechanicsSortByItemCount : MechanicsSort() {
    override val sortType = MechanicsViewModel.SortType.ITEM_COUNT
    override val sortBy = MechanicDao.SortType.ITEM_COUNT
}