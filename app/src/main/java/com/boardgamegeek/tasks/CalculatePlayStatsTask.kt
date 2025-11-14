package com.boardgamegeek.tasks

import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayStatsEntity
import com.boardgamegeek.entities.PlayerStatsEntity
import com.boardgamegeek.extensions.COLLECTION_STATUS_OWN
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.extensions.launchTask
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository

class CalculatePlayStatsTask(private val application: BggApplication) {
    private val playRepository: PlayRepository = PlayRepository(application)

    fun execute() {
        launchTask(
            backgroundWork = {
                if (SyncPrefs.isPlaysSyncUpToDate(application)) {
                    val playStats = playRepository.loadForStats()
                    val playStatsEntity = PlayStatsEntity(playStats, application.isStatusSetToSync(COLLECTION_STATUS_OWN))
                    playRepository.updateGameHIndex(playStatsEntity.hIndex)

                    val playerStats = playRepository.loadPlayersForStats()
                    val playerStatsEntity = PlayerStatsEntity(playerStats)
                    playRepository.updatePlayerHIndex(playerStatsEntity.hIndex)
                }
            }
        )
    }
}
