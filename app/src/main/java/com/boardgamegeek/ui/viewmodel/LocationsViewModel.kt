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
    private val _sortType = MutableLiveData<Location.SortType>()
    val sortType: LiveData<Location.SortType>
        get() = _sortType

    init {
        sort(Location.SortType.NAME)
    }

    val locations: LiveData<List<Location>> = sortType.switchMap {
        liveData {
            emit(playRepository.loadLocations(it))
        }
    }

    fun refresh() {
        _sortType.value?.let { _sortType.value = it }
    }

    fun sort(sortType: Location.SortType) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }

    fun getSectionHeader(location: Location?): String {
        return when(sortType.value) {
            Location.SortType.NAME -> location?.name.firstChar()
            Location.SortType.PLAY_COUNT -> (location?.playCount ?: 0).orderOfMagnitude()
            null -> ""
        }
    }
}
