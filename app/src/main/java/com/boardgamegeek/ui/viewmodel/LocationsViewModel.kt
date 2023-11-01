package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Location
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        NAME, PLAY_COUNT
    }

    private val _sort = MutableLiveData<LocationsSort>()
    val sort: LiveData<LocationsSort>
        get() = _sort

    init {
        sort(SortType.NAME)
    }

    val locations: LiveData<List<Location>> = sort.switchMap {
        liveData {
            emit(playRepository.loadLocations(it.sortBy))
        }
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> LocationsSort.ByName()
            SortType.PLAY_COUNT -> LocationsSort.ByPlayCount()
        }
    }

    fun getSectionHeader(location: Location?): String {
        return sort.value?.getSectionHeader(location) ?: ""
    }

    sealed class LocationsSort {
        abstract val sortType: SortType
        abstract val sortBy: PlayRepository.LocationSortBy
        abstract fun getSectionHeader(location: Location?): String

        class ByName : LocationsSort() {
            override val sortType = SortType.NAME
            override val sortBy = PlayRepository.LocationSortBy.NAME
            override fun getSectionHeader(location: Location?) = location?.name.firstChar()
        }

        class ByPlayCount : LocationsSort() {
            override val sortType = SortType.PLAY_COUNT
            override val sortBy = PlayRepository.LocationSortBy.PLAY_COUNT
            override fun getSectionHeader(location: Location?) = (location?.playCount ?: 0).orderOfMagnitude()
        }
    }
}
