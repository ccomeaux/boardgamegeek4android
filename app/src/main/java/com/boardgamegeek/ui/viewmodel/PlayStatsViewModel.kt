package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.PreferencesUtils

class PlayStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val playRepository = PlayRepository(getApplication())

    fun getPlays(): LiveData<PlayStatsEntity> {
        val ld = playRepository.loadForStats()
        return Transformations.map(ld) {
            val entity = PlayStatsEntity(it, PreferencesUtils.isStatusSetToSync(getApplication(), BggService.COLLECTION_QUERY_STATUS_OWN))
            playRepository.updateGameHIndex(entity.hIndex)
            return@map entity
        }
    }

    fun getPlayers(): LiveData<PlayerStatsEntity> {
        return Transformations.map(playRepository.loadPlayersForStats()) {
            val entity = PlayerStatsEntity(it)
            playRepository.updatePlayerHIndex(entity.hIndex)
            return@map entity
        }
    }
}