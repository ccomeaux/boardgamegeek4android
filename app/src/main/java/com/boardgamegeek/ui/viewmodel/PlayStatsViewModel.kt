package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
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
    val includeIncomplete = LiveSharedPreference<Boolean>(application, LOG_PLAY_STATS_INCOMPLETE)
    val includeExpansions = LiveSharedPreference<Boolean>(application, LOG_PLAY_STATS_EXPANSIONS)
    val includeAccessories = LiveSharedPreference<Boolean>(application, LOG_PLAY_STATS_ACCESSORIES)

    val plays: LiveData<PlayStatsEntity> =
        liveData {
            val data = playRepository.loadForStats(
                includeIncomplete.value ?: false,
                includeExpansions.value ?: false,
                includeAccessories.value ?: false
            )
            val entity = PlayStatsEntity(data, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN))
            playRepository.updateGameHIndex(entity.hIndex)
            emit(entity)
        }

    val players: LiveData<PlayerStatsEntity> =
        liveData {
            val x = playRepository.loadPlayersForStats(includeIncomplete.value ?: false)
            val entity = PlayerStatsEntity(x)
            emit(entity)
            playRepository.updatePlayerHIndex(entity.hIndex)
        }
}
