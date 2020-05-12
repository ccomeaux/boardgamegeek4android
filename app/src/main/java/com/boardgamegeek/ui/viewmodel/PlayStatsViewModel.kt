package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.extensions.COLLECTION_STATUS_OWN
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.repository.PlayRepository

class PlayStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val playRepository = PlayRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }
    val includeIncomplete: LiveSharedPreference<Boolean> = LiveSharedPreference(application, LOG_PLAY_STATS_INCOMPLETE)
    val includeExpansions: LiveSharedPreference<Boolean> = LiveSharedPreference(application, LOG_PLAY_STATS_EXPANSIONS)
    val includeAccessories: LiveSharedPreference<Boolean> = LiveSharedPreference(application, LOG_PLAY_STATS_ACCESSORIES)

    fun getPlays(): LiveData<PlayStatsEntity> {
        val ld = playRepository.loadForStatsAsLiveData(
                includeIncomplete.value ?: false,
                includeExpansions.value ?: false,
                includeAccessories.value ?: false)
        return Transformations.map(ld) {
            val entity = PlayStatsEntity(it, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN))
            playRepository.updateGameHIndex(entity.hIndex)
            return@map entity
        }
    }

    fun getPlayers(): LiveData<PlayerStatsEntity> {
        return Transformations.map(playRepository.loadPlayersForStatsAsLiveData(
                includeIncomplete.value ?: false)) {
            val entity = PlayerStatsEntity(it)
            playRepository.updatePlayerHIndex(entity.hIndex)
            return@map entity
        }
    }
}
