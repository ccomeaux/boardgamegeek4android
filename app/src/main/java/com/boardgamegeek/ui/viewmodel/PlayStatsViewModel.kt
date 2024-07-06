package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.model.PlayerStats
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayStatsViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    // TODO switch on these
    val includeIncomplete: LiveData<Boolean?> = LiveSharedPreference(application, LOG_PLAY_STATS_INCOMPLETE)
    val includeExpansions: LiveData<Boolean?> = LiveSharedPreference(application, LOG_PLAY_STATS_EXPANSIONS)
    val includeAccessories: LiveData<Boolean?> = LiveSharedPreference(application, LOG_PLAY_STATS_ACCESSORIES)

    val plays: LiveData<PlayStats> =
        liveData {
            val playStats = playRepository.calculatePlayStats()
            emit(playStats)
        }

    val players: LiveData<PlayerStats> =
        liveData {
            val playerStats = playRepository.calculatePlayerStats()
            emit(playerStats)
        }
}
