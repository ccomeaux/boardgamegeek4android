package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RateLimiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PlaysSummaryViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val playsRateLimiter = RateLimiter<Int>(10.minutes)
    private val syncTimestamp = MutableLiveData<Long>()
    private val h = LiveSharedPreference<Int>(getApplication(), PlayStatPrefs.KEY_GAME_H_INDEX)
    private val n = LiveSharedPreference<Int>(getApplication(), PlayStatPrefs.KEY_GAME_H_INDEX + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX)
    private val username = LiveSharedPreference<String>(getApplication(), AccountPreferences.KEY_USERNAME)

    init {
        reload()
    }

    val syncPlays = LiveSharedPreference<Boolean>(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    val syncPlaysTimestamp = LiveSharedPreference<Long>(getApplication(), PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP)
    val oldestSyncDate = LiveSharedPreference<Long>(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME)
    val newestSyncDate = LiveSharedPreference<Long>(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME)

    val plays = syncTimestamp.switchMap {
        liveData {
            emitSource(playRepository.loadPlaysFlow().asLiveData())
            attemptRefresh()
        }
    }

    fun reload(): Boolean {
        val value = syncTimestamp.value
        return if (value == null || value.isOlderThan(5.seconds)) {
            syncTimestamp.postValue(System.currentTimeMillis())
            true
        } else false
    }

    fun refresh(): Boolean {
        return attemptRefresh() // TODO - force?
    }

    private fun attemptRefresh(): Boolean {
        return if (syncPlays.value == true && playsRateLimiter.shouldProcess(0)) {
            viewModelScope.launch {
                try {
                    playRepository.refreshPlays()
                } catch (e: Exception) {
                    // TODO emit error message
                    playsRateLimiter.reset(0)
                }
            }
            true
        } else false
    }

    val playCount: LiveData<Int> = plays.map { list ->
        list.sumOf { it.quantity }
    }.distinctUntilChanged()

    val playsInProgress: LiveData<List<Play>> = plays.map { list ->
        list.filter { it.dirtyTimestamp > 0L }
    }.distinctUntilChanged()

    val playsNotInProgress: LiveData<List<Play>> = plays.map { list ->
        list.filter { it.dirtyTimestamp == 0L }.take(ITEMS_TO_DISPLAY)
    }.distinctUntilChanged()

    val players: LiveData<List<Player>> = plays.switchMap {
        liveData {
            emit(playRepository.loadPlayers())
        }
    }.map { p ->
        p.filter { it.username != username.value }.take(ITEMS_TO_DISPLAY)
    }

    val locations: LiveData<List<Location>> = plays.switchMap {
        liveData {
            emit(playRepository.loadLocations())
        }
    }.map { p ->
        p.filter { it.name.isNotBlank() }.take(ITEMS_TO_DISPLAY)
    }

    val colors: LiveData<List<PlayerColor>> = liveData {
        emit(
            if (username.value.isNullOrBlank()) emptyList()
            else playRepository.loadUserColors(username.value.orEmpty())
        )
    }

    val hIndex = MediatorLiveData<HIndex>().apply {
        addSource(h) {
            value = HIndex(it ?: 0, n.value ?: 0)
        }
        addSource(n) {
            value = HIndex(h.value ?: 0, it ?: 0)
        }
    }

    fun reset() {
        viewModelScope.launch {
            playRepository.resetPlays()
        }
    }

    companion object {
        const val ITEMS_TO_DISPLAY = 5
    }
}
