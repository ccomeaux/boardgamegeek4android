package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.repository.PlayRepository

class LocationsViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, PLAY_COUNT
    }

    private val playRepository = PlayRepository(getApplication())

    private val _sort = MutableLiveData<LocationsSort>()
    val sort: LiveData<LocationsSort>
        get() = _sort

    init {
        sort(SortType.NAME)
    }

    val locations: LiveData<List<LocationEntity>> = Transformations.switchMap(sort) {
        playRepository.loadLocations(it.sortBy)
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> LocationsSortByName()
            SortType.PLAY_COUNT -> LocationsSortByPlayCount()
        }
    }

    fun getSectionHeader(location: LocationEntity?): String {
        return sort.value?.getSectionHeader(location) ?: ""
    }
}

sealed class LocationsSort {
    abstract val sortType: LocationsViewModel.SortType
    abstract val sortBy: PlayDao.LocationSortBy
    abstract fun getSectionHeader(location: LocationEntity?): String
}

class LocationsSortByName : LocationsSort() {
    override val sortType = LocationsViewModel.SortType.NAME
    override val sortBy = PlayDao.LocationSortBy.NAME
    override fun getSectionHeader(location: LocationEntity?): String {
        return location?.name.firstChar()
    }
}

class LocationsSortByPlayCount : LocationsSort() {
    override val sortType = LocationsViewModel.SortType.PLAY_COUNT
    override val sortBy = PlayDao.LocationSortBy.PLAY_COUNT
    override fun getSectionHeader(location: LocationEntity?): String {
        return (location?.playCount ?: 0).orderOfMagnitude()
    }
}
