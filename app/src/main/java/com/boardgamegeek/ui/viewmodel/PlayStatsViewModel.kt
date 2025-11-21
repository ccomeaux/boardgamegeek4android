package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.extensions.COLLECTION_STATUS_OWN
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.repository.PlayRepository

class PlayStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val playRepository = PlayRepository(getApplication())

    fun getPlays(): LiveData<PlayStatsEntity> {
        val ld = playRepository.loadForStatsAsLiveData()
        return ld.map() {
            val entity = PlayStatsEntity(it, getApplication<BggApplication>().isStatusSetToSync(COLLECTION_STATUS_OWN))
            playRepository.updateGameHIndex(entity.hIndex)
            return@map entity
        }
    }

    fun getPlayers(): LiveData<PlayerStatsEntity> {
        return playRepository.loadPlayersForStatsAsLiveData().map() {
            val entity = PlayerStatsEntity(it)
            playRepository.updatePlayerHIndex(entity.hIndex)
            return@map entity
        }
    }
}
