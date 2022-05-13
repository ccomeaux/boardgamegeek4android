package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.RateLimiter
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PlaysViewModel(application: Application) : AndroidViewModel(application) {
    private val syncPlays = LiveSharedPreference<Boolean>(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    private val playsRateLimiter = RateLimiter<Int>(10, TimeUnit.MINUTES)
    private val playRepository = PlayRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())

    private data class PlayInfo(
        val mode: Mode,
        val name: String = "",
        val id: Int = BggContract.INVALID_ID,
        val filter: FilterType = FilterType.ALL,
        val sort: SortType = SortType.DATE
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

    private val locationRenameCount = MutableLiveData<PlayRepository.RenameLocationResults>()
    val updateMessage: LiveData<Event<String>> = locationRenameCount.map { result ->
        setLocation(result.newLocationName)
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        Event(
            getApplication<BggApplication>().resources.getQuantityString(
                R.plurals.msg_play_location_change,
                result.count,
                result.count,
                result.oldLocationName,
                result.newLocationName
            )
        )
    }

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<Event<String>> = _errorMessage.map { Event(it) }

    private val _syncingStatus = MutableLiveData<Boolean>()
    val syncingStatus: LiveData<Boolean>
        get() = _syncingStatus

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = playInfo.switchMap {
        liveData {
            try {
                val list = loadPlays(it)
                val refreshedList = if (syncPlays.value == true && playsRateLimiter.shouldProcess(it.id)) {
                    emit(RefreshableResource.refreshing(list))
                    SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
                    when (it.mode) {
                        Mode.GAME -> gameRepository.refreshPlays(it.id)
                        else -> playRepository.refreshPlays()
                    }
                    loadPlays(it)
                } else list
                emit(RefreshableResource.success(refreshedList))
            } catch (e: Exception) {
                playsRateLimiter.reset(0)
                emit(RefreshableResource.error(e, getApplication()))
            }
        }
    }

    fun refresh() {
        playInfo.value.let { playInfo.value = it }
    }

    private suspend fun loadPlays(it: PlayInfo) = when (it.mode) {
        Mode.ALL -> {
            val sortType = when (it.sort) {
                SortType.DATE -> PlayRepository.SortBy.DATE
                SortType.LOCATION -> PlayRepository.SortBy.LOCATION
                SortType.GAME -> PlayRepository.SortBy.GAME
                SortType.LENGTH -> PlayRepository.SortBy.LENGTH
            }
            when (it.filter) {
                FilterType.ALL -> playRepository.getPlays(sortType)
                FilterType.DIRTY -> playRepository.getDraftPlays()
                FilterType.PENDING -> playRepository.getPendingPlays()
            }
        }
        Mode.GAME -> gameRepository.getPlays(it.id)
        Mode.LOCATION -> playRepository.loadPlaysByLocation(it.name)
        Mode.BUDDY -> playRepository.loadPlaysByUsername(it.name)
        Mode.PLAYER -> playRepository.loadPlaysByPlayerName(it.name)
    }

    val filterType: LiveData<FilterType> = playInfo.map {
        it.filter
    }

    val sortType: LiveData<SortType> = playInfo.map {
        it.sort
    }

    val location: LiveData<String> = playInfo.map {
        if (it.mode == Mode.LOCATION) it.name else ""
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
        if (playInfo.value?.filter != type) {
            playInfo.value = PlayInfo(Mode.ALL, filter = type, sort = playInfo.value?.sort ?: SortType.DATE)
        }
    }

    fun setSort(type: SortType) {
        if (playInfo.value?.sort != type) {
            playInfo.value = PlayInfo(Mode.ALL, filter = playInfo.value?.filter ?: FilterType.ALL, sort = type)
        }
    }

    fun renameLocation(oldLocationName: String, newLocationName: String) {
        viewModelScope.launch {
            val results = playRepository.renameLocation(oldLocationName, newLocationName)
            locationRenameCount.postValue(results)
        }
    }

    fun syncPlaysByDate(timeInMillis: Long) {
        viewModelScope.launch {
            try {
                _syncingStatus.postValue(true)
                playRepository.refreshPlays(timeInMillis)
                refresh()
            } catch (e: Exception) {
                _errorMessage.postValue(e.localizedMessage ?: e.message ?: e.toString())
            } finally {
                _syncingStatus.postValue(false)
            }
        }
    }

    fun send(plays: List<PlayEntity>) {
        viewModelScope.launch {
            plays.forEach {
                playRepository.markAsUpdated(it.internalId)
            }
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
    }

    fun delete(plays: List<PlayEntity>) {
        viewModelScope.launch {
            plays.forEach {
              playRepository.markAsDeleted(it.internalId)
            }
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        }
    }
}
