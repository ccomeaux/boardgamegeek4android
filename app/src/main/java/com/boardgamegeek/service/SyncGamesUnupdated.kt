package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig

/**
 * Syncs all games in the collection that have not been updated completely.
 */
class SyncGamesUnupdated(application: BggApplication, syncResult: SyncResult, private val gameRepository: GameRepository) :
    SyncGames(application, syncResult, gameRepository) {
    override val maxFetchCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX_UNUPDATED)
    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD
    override val introLogMessage = "Syncing $gamesPerFetch unupdated games in the collection..."
    override val exitLogMessage = "...no more unupdated games"
    override val notificationSummaryMessageId = R.string.sync_notification_games_unupdated
    override suspend fun getGames() = gameRepository.loadUnupdatedGames(gamesPerFetch)
}
