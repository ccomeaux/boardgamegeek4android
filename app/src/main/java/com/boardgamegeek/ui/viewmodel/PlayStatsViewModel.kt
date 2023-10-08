package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.model.PlayerStats
import com.boardgamegeek.extensions.COLLECTION_STATUS_OWN
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayStatsViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }
    val includeIncomplete = LiveSharedPreference<Boolean>(application, LOG_PLAY_STATS_INCOMPLETE)
    val includeExpansions = LiveSharedPreference<Boolean>(application, LOG_PLAY_STATS_EXPANSIONS)
    val includeAccessories = LiveSharedPreference<Boolean>(application, LOG_PLAY_STATS_ACCESSORIES)

    val plays: LiveData<PlayStats> =
        liveData {
            val data = playRepository.loadForStats(
                includeIncomplete.value ?: false,
                includeExpansions.value ?: false,
                includeAccessories.value ?: false
            )
            val playStats = PlayStats(data, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN))
            playRepository.updateGameHIndex(playStats.hIndex)
            emit(playStats)
        }

    val players: LiveData<PlayerStats> =
        liveData {
            val players = playRepository.loadPlayersForStats(includeIncomplete.value ?: false)
            val playerStats = PlayerStats(players)
            emit(playerStats)
            playRepository.updatePlayerHIndex(playerStats.hIndex)
        }
}
