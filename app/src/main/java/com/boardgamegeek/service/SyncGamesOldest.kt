package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.repository.GameRepository

/**
 * Syncs a number of games that haven't been updated in a long time.
 */
class SyncGamesOldest(application: BggApplication, service: BggService, syncResult: SyncResult, private val gameRepository: GameRepository) :
    SyncGames(application, service, syncResult, gameRepository) {
    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD
    override val introLogMessage = "Syncing $gamesPerFetch oldest games in the collection..."
    override val exitLogMessage = "...found no old games to update (this should only happen with empty collections)"
    override val notificationSummaryMessageId = R.string.sync_notification_games_oldest
    override suspend fun getGames() = gameRepository.loadOldestUpdatedGames(gamesPerFetch)
}
