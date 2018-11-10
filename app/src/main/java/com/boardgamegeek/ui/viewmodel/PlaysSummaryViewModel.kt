package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.PlayRepository

class PlaysSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val playRepository = PlayRepository(getApplication())

    val players: LiveData<List<PlayerEntity>> =
            Transformations.map(playRepository.loadPlayers(PlayDao.PlayerSortBy.PLAY_COUNT)) { l ->
                l.filter { it.username != AccountUtils.getUsername(getApplication()) }.take(5)
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
}