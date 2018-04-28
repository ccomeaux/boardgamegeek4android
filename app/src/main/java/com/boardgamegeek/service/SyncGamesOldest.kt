package com.boardgamegeek.service

import android.content.Context
import android.content.SyncResult

import com.boardgamegeek.R
import com.boardgamegeek.io.BggService

/**
 * Syncs a number of games that haven't been updated in a long time.
 */
class SyncGamesOldest(context: Context, service: BggService, syncResult: SyncResult) : SyncGames(context, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val exitLogMessage = "...found no old games to update (this should only happen with empty collections)"

    override val notificationSummaryMessageId = R.string.sync_notification_games_oldest

    override fun getIntroLogMessage(gamesPerFetch: Int) = "Syncing $gamesPerFetch oldest games in the collection..."
}
