package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.model.Play
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RateLimiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class PlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val syncPlays = LiveSharedPreference<Boolean>(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    private val playsRateLimiter = RateLimiter<Int>(10.minutes)

    private data class PlayInfo(
        val mode: Mode,
        val name: String = "",
        val id: Int = BggContract.INVALID_ID,
    )

    enum class Mode {
        ALL, GAME, BUDDY, PLAYER, LOCATION
    }

    enum class FilterType {
        ALL, DIRTY, PENDING
    }

    enum class SortType {
        DATE, LOCATION, GAME, LENGTH
    }

    private val playInfo = MutableLiveData<PlayInfo>()

    private val _updateMessage = MutableLiveData<Event<String>>()
    val updateMessage: LiveData<Event<String>>
        get() = _updateMessage

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<Event<String>> = _errorMessage.map { Event(it) }

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
            val list = when (it.mode) {
                Mode.ALL -> playRepository.loadPlays()
                Mode.GAME -> playRepository.loadPlaysByGame(it.id)
                Mode.LOCATION -> playRepository.loadPlaysByLocation(it.name)
                Mode.BUDDY -> playRepository.loadPlaysByUsername(it.name)
                Mode.PLAYER -> playRepository.loadPlaysByPlayerName(it.name)
            }
            emit(list)
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

    val location: LiveData<String> = playInfo.map {
        if (it.mode == Mode.LOCATION) it.name else ""
    }

    fun setAll() {
        setFilter(FilterType.ALL)
        setSort(SortType.DATE)
        playInfo.value = PlayInfo(Mode.ALL)
    }

    fun setGame(gameId: Int) {
        playInfo.value = PlayInfo(Mode.GAME, id = gameId)
    }

    fun setLocation(locationName: String) {
        playInfo.value = PlayInfo(Mode.LOCATION, locationName)
    }

    fun setUsername(username: String) {
        playInfo.value = PlayInfo(Mode.BUDDY, username)
    }

    fun setPlayerName(playerName: String) {
        playInfo.value = PlayInfo(Mode.PLAYER, playerName)
    }

    fun setFilter(type: FilterType) {
        if (_filterType.value != type) {
            _filterType.value = type
        }
    }

    fun setSort(type: SortType) {
        if (sortType.value != type) {
            _sortType.value = type
        }
    }

    fun renameLocation(oldLocationName: String, newLocationName: String) {
        viewModelScope.launch {
            val internalIds = playRepository.renameLocation(oldLocationName, newLocationName)
            playRepository.enqueueUploadRequest(internalIds)
            _updateMessage.value = Event(
                getApplication<BggApplication>().resources.getQuantityString(
                    R.plurals.msg_play_location_change,
                    internalIds.size,
                    internalIds.size,
                    oldLocationName,
                    newLocationName
                )
            )
            setLocation(newLocationName)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _isRefreshing.postValue(true)
                val id = playInfo.value?.id ?: 0
                if (syncPlays.value == true && playsRateLimiter.shouldProcess(id)) {
                    when (playInfo.value?.mode) {
                        Mode.GAME -> playRepository.refreshPlaysForGame(id)
                        else -> playRepository.refreshPlays()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.localizedMessage ?: e.message ?: e.toString())
                playsRateLimiter.reset(0)
            } finally {
                playInfo.value.let { playInfo.value = it }
                _isRefreshing.postValue(false)
            }
        }
    }

    fun refreshPlaysByDate(timeInMillis: Long) {
        viewModelScope.launch {
            try {
                _isRefreshing.postValue(true)
                playRepository.refreshPlaysForDate(timeInMillis)
            } catch (e: Exception) {
                _errorMessage.postValue(e.localizedMessage ?: e.message ?: e.toString())
            } finally {
                playInfo.value.let { playInfo.value = it }
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
