package com.boardgamegeek.tasks

import android.content.SharedPreferences
import android.os.AsyncTask
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.extensions.COLLECTION_STATUS_OWN
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_INCOMPLETE
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.isPlaysSyncUpToDate
import com.boardgamegeek.repository.PlayRepository

class CalculatePlayStatsTask(private val application: BggApplication) : AsyncTask<Void, Void, Void?>() {
    private val playRepository: PlayRepository = PlayRepository(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    override fun doInBackground(vararg params: Void): Void? {
        if (SyncPrefs.getPrefs(application).isPlaysSyncUpToDate()) {
            val includeIncompletePlays = prefs[LOG_PLAY_STATS_INCOMPLETE, false] ?: false
            val includeExpansions = prefs[LOG_PLAY_STATS_EXPANSIONS, false] ?: false
            val includeAccessories = prefs[LOG_PLAY_STATS_ACCESSORIES, false] ?: false

            val playStats = playRepository.loadForStats(includeIncompletePlays, includeExpansions, includeAccessories)
            val playStatsEntity = PlayStatsEntity(playStats, prefs.isStatusSetToSync(COLLECTION_STATUS_OWN))
            playRepository.updateGameHIndex(playStatsEntity.hIndex)

            val playerStats = playRepository.loadPlayersForStats(includeIncompletePlays)
            val playerStatsEntity = PlayerStatsEntity(playerStats)
            playRepository.updatePlayerHIndex(playerStatsEntity.hIndex)
        }
        return null
    }
}
