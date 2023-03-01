package com.boardgamegeek.service

import android.content.SyncResult
import android.text.format.DateUtils
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.COLLECTION_STATUS_PLAYED
import com.boardgamegeek.extensions.hoursAgo
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Removes games that aren't in the collection and haven't been viewed in 72 hours.
 */
class SyncGamesRemove(application: BggApplication, syncResult: SyncResult, private val gameRepository: GameRepository) :
    SyncTask(application, syncResult) {
    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_collection_missing

    private val lastViewedAgeInHours = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_DELETE_VIEW_HOURS)

    override fun execute() {
        Timber.i("Removing games not in the collection")
        try {
            val hoursAgo = lastViewedAgeInHours.hoursAgo()
            val date = DateUtils.formatDateTime(context, hoursAgo, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_TIME)
            Timber.i("Fetching games that aren't in the collection and have not been viewed since $date")

            val games = runBlocking { gameRepository.loadDeletableGames(hoursAgo, prefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED)) }
            if (games.isNotEmpty()) {
                Timber.i("Found ${games.size} games to delete: ${games.map { "[${it.first}] ${it.second}" }}")
                updateProgressNotification(context.resources.getQuantityString(R.plurals.sync_notification_games_remove, games.size, games.size))

                var count = 0
                // NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
                for ((gameId, _) in games) {
                    Timber.i("Deleting game ID=${gameId}")
                    count += runBlocking { gameRepository.delete(gameId) }
                }
                syncResult.stats.numDeletes += count.toLong()
                Timber.i("Deleted $count games")
            } else {
                Timber.i("No games need deleting")
            }
        } finally {
            Timber.i("Game removal complete!")
        }
    }
}
