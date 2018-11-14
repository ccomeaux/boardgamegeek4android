package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
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
        list?.data?.filter { it.dirtyTimestamp == 0L }?.take(5)
    }

    val players: LiveData<List<PlayerEntity>> =
            Transformations.map(playRepository.loadPlayers(PlayDao.PlayerSortBy.PLAY_COUNT)) { p ->
                p.filter { it.username != AccountUtils.getUsername(getApplication()) }.take(5)
            }

    val locations: LiveData<List<LocationEntity>> =
            Transformations.map(playRepository.loadLocations(PlayDao.LocationSortBy.PLAY_COUNT)) { l ->
                l.filter { it.name.isNotBlank() }.take(5)
            }

    val colors: LiveData<List<PlayerColorEntity>>
        get() {
            val username = AccountUtils.getUsername(getApplication())
            return when (username) {
                null -> AbsentLiveData.create()
                else -> playRepository.loadUserColors(username)
            }
        }

    fun refresh(): Boolean {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        val value = syncTimestamp.value
        return if (value == null || value.isOlderThan(5, TimeUnit.MINUTES)) {
            syncTimestamp.postValue(System.currentTimeMillis())
            true
        } else false
    }
}