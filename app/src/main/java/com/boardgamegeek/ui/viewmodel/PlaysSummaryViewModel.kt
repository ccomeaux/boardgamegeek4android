package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.util.PreferencesUtils
import java.util.concurrent.TimeUnit

class PlaysSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val syncTimestamp = MutableLiveData<Long>()

    init {
        refresh()
    }

    private val playRepository = PlayRepository(getApplication())

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = Transformations.switchMap(syncTimestamp) {
        playRepository.getPlays()
    }

    val playCount: LiveData<Int> = Transformations.map(plays) { list ->
        list?.data?.sumBy { it.quantity } ?: 0
    }

    val playsInProgress: LiveData<List<PlayEntity>> = Transformations.map(plays) { list ->
        list?.data?.filter { it.dirtyTimestamp > 0L }
    }

    val playsNotInProgress: LiveData<List<PlayEntity>> = Transformations.map(plays) { list ->
        list?.data?.filter { it.dirtyTimestamp == 0L }?.take(ITEMS_TO_DISPLAY)
    }

    val players: LiveData<List<PlayerEntity>> =
            Transformations.map(Transformations.switchMap(plays) {
                playRepository.loadPlayers(PlayDao.PlayerSortBy.PLAY_COUNT)
            }) { p ->
                p.filter { it.username != AccountUtils.getUsername(getApplication()) }.take(ITEMS_TO_DISPLAY)
            }

    val locations: LiveData<List<LocationEntity>> =
            Transformations.map(Transformations.switchMap(plays) {
                playRepository.loadLocations(PlayDao.LocationSortBy.PLAY_COUNT)
            }) { p ->
                p.filter { it.name.isNotBlank() }.take(ITEMS_TO_DISPLAY)
            }

    val colors: LiveData<List<PlayerColorEntity>>
        get() {
            val username = AccountUtils.getUsername(getApplication())
            return when (username) {
                null -> AbsentLiveData.create()
                else -> playRepository.loadUserColorsAsLiveData(username)
            }
        }

    val hIndex: LiveSharedPreference<Int> = LiveSharedPreference(getApplication(), PreferencesUtils.KEY_GAME_H_INDEX)

    val oldestSyncDate: LiveSharedPreference<Long> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME)
    val newestSyncDate: LiveSharedPreference<Long> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME)

    val syncPlays: LiveSharedPreference<Boolean> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    val syncPlaysTimestamp: LiveSharedPreference<Long> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP)

    fun refresh(): Boolean {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        val value = syncTimestamp.value
        return if (value == null || value.isOlderThan(1, TimeUnit.SECONDS)) {
            syncTimestamp.postValue(System.currentTimeMillis())
            true
        } else false
    }

    companion object {
        const val ITEMS_TO_DISPLAY = 5
    }
}