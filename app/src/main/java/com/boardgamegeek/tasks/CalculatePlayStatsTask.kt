package com.boardgamegeek.tasks

import android.os.AsyncTask
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.PreferencesUtils

class CalculatePlayStatsTask(private val application: BggApplication) : AsyncTask<Void, Void, Void?>() {
    private val playRepository: PlayRepository = PlayRepository(application)

    override fun doInBackground(vararg params: Void): Void? {
        if (SyncPrefs.isPlaysSyncUpToDate(application)) {
            val playStats = playRepository.loadForStats()
            val playStatsEntity = PlayStatsEntity(playStats, PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN))
            playRepository.updateGameHIndex(playStatsEntity.hIndex)

            val playerStats = playRepository.loadPlayersForStats()
            val playerStatsEntity = PlayerStatsEntity(playerStats)
            playRepository.updatePlayerHIndex(playerStatsEntity.hIndex)
        }
        return null
    }
}
