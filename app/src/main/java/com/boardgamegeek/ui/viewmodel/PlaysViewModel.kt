package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class PlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val syncPlays: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS, defaultValue = null)

    private data class PlayInfo(
        val name: String = "",
        val id: Int = BggContract.INVALID_ID,
    )

    enum class FilterType {
        ALL, DIRTY, PENDING
    }

    enum class SortType {
        DATE, LOCATION, GAME, LENGTH
    }

    private val playInfo = MutableLiveData<PlayInfo>()

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean>
        get() = _isRefreshing

    private val _plays = MediatorLiveData<List<Play>>()
    val plays: LiveData<List<Play>>
        get() = _plays

    private val _filterType = MutableLiveData<FilterType>()
    val filterType: LiveData<FilterType>
        get() = _filterType

    private val _sortType = MutableLiveData<SortType>()
    val sortType: LiveData<SortType>
        get() = _sortType

    private val allPlays: LiveData<List<Play>> = playInfo.switchMap {
        liveData {
            emitSource(playRepository.loadPlaysFlow().distinctUntilChanged().asLiveData())
        }
    }

    init {
        _plays.addSource(allPlays) { list ->
            filterAndSortPlays(list, sortType.value, filterType.value)
        }
        _plays.addSource(sortType) {
            filterAndSortPlays(allPlays.value, it, filterType.value)
        }
        _plays.addSource(filterType) {
            filterAndSortPlays(allPlays.value, sortType.value, it)
        }
    }

    private fun filterAndSortPlays(
        list: List<Play>?,
        sortType: SortType?,
        filterType: FilterType?,
    ) {
        if (list == null) return
        val filteredList = when (filterType) {
            FilterType.ALL -> list.filter { it.deleteTimestamp == 0L }
            FilterType.DIRTY -> list.filter { it.dirtyTimestamp > 0L }
            FilterType.PENDING -> list.filter { it.updateTimestamp > 0L || it.deleteTimestamp > 0L }
            null -> list
        }
        val sortedList = when (sortType) {
            SortType.DATE -> filteredList.sortedByDescending { it.dateInMillis }
            SortType.LOCATION -> filteredList.sortedBy { it.location }
            SortType.GAME -> filteredList.sortedBy { it.gameName }
            SortType.LENGTH -> filteredList.sortedByDescending { it.length }
            null -> filteredList.sortedByDescending { it.dateInMillis }
        }
        _plays.postValue(sortedList)
    }

    fun setAll() {
        setFilter(FilterType.ALL)
        setSort(SortType.DATE)
    }

    fun setFilter(type: FilterType) {
        if (_filterType.value != type) _filterType.value = type
    }

    fun setSort(type: SortType) {
        if (sortType.value != type) _sortType.value = type
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                if (syncPlays.value == true && _isRefreshing.value != true) {
                    _isRefreshing.postValue(true)
                    playRepository.refreshRecentPlays()
                }
            } catch (e: Exception) {
                _errorMessage.postMessage(e)
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }

    fun refreshPlaysByDate(timeInMillis: Long) {
        viewModelScope.launch {
            try {
                if (syncPlays.value == true && _isRefreshing.value != true) {
                    _isRefreshing.postValue(true)
                    playRepository.refreshPlaysForDate(timeInMillis)?.let {
                        _errorMessage.postMessage(it)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postMessage(e)
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }

    fun send(plays: List<Play>) {
        viewModelScope.launch {
            val idsToSend = mutableListOf<Long>()
            plays.forEach {
                if (playRepository.markAsUpdated(it.internalId))
                    idsToSend += it.internalId
            }
            playRepository.enqueueUploadRequest(idsToSend)
        }
    }

    fun delete(plays: List<Play>) {
        viewModelScope.launch {
            val idsDeleted = mutableListOf<Long>()
            plays.forEach {
                if (playRepository.markAsDeleted(it.internalId))
                    idsDeleted += it.internalId
            }
            playRepository.enqueueUploadRequest(idsDeleted)
        }
    }
}
