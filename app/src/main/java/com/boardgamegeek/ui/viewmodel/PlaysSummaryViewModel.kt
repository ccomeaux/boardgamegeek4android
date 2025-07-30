package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaysSummaryViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val h: LiveData<Int?> = LiveSharedPreference(getApplication(), PlayStatPrefs.KEY_GAME_H_INDEX)
    private val n: LiveData<Int?> = LiveSharedPreference(getApplication(), PlayStatPrefs.KEY_GAME_H_INDEX + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX)
    private val username: LiveData<String?> = LiveSharedPreference(getApplication(), AccountPreferences.KEY_USERNAME)

    val syncPlays: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    val syncPlaysTimestamp: LiveData<Long?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP)
    val oldestSyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME)
    val newestSyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME)

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _isSyncing = MutableLiveData<Boolean>()
    val isSyncing: LiveData<Boolean>
        get() = _isSyncing

    private val plays = syncPlays.switchMap {
        liveData {
            if (it == true) {
                emitSource(playRepository.loadPlaysFlow().asLiveData())
                refresh()
            }
        }
    }

    fun enableSyncing(enable: Boolean) {
        if (enable)
            prefs[PREFERENCES_KEY_SYNC_PLAYS] = true
        else
            prefs[PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP] = System.currentTimeMillis()
    }

    fun refresh(): Boolean {
        return if (syncPlays.value == true && _isSyncing.value != true) {
            viewModelScope.launch {
                try {
                    _isSyncing.value = true
                    playRepository.refreshRecentPlays()?.let {
                        _errorMessage.setMessage(it)
                    }
                } finally {
                    _isSyncing.value = false
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
            emitSource(
                playRepository.loadPlayersFlow()
                    .map { list ->
                        list.filter { player -> player.username != username.value }
                            .take(ITEMS_TO_DISPLAY)
                    }
                    .distinctUntilChanged()
                    .asLiveData()
            )
        }
    }

    val locations: LiveData<List<Location>> = plays.switchMap {
        liveData {
            emitSource(
                playRepository.loadLocationsFlow()
                    .map { list ->
                        list.filter { location -> location.name.isNotBlank() }
                            .take(ITEMS_TO_DISPLAY)
                    }
                    .distinctUntilChanged()
                    .asLiveData()
            )
        }
    }

    val colors: LiveData<List<PlayerColor>> = username.switchMap {
        liveData {
            emitSource(playRepository.loadUserColorsAsFlow(it.orEmpty()).asLiveData())
        }
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
