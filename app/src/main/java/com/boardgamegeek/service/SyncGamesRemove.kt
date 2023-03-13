package com.boardgamegeek.service

import android.content.SyncResult
import android.text.format.DateUtils
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Removes games that aren't in the collection and haven't been viewed in 72 hours.
 */
class SyncGamesRemove(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncTask(application, service, syncResult) {
    private val dao = GameDao(application)

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_collection_missing

    private val lastViewedAgeInHours = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_DELETE_VIEW_HOURS)

    override fun execute() {
        Timber.i("Removing games not in the collection...")
        try {
            val gameIds = fetchGameIds()
            if (gameIds.isNotEmpty()) {
                Timber.i("Found ${gameIds.size} games to delete: $gameIds")
                updateProgressNotification(context.resources.getQuantityString(R.plurals.sync_notification_games_remove, gameIds.size, gameIds.size))

                var count = 0
                // NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
                for (gameId in gameIds) {
                    Timber.i("Deleting game ID=$gameId")
                    count += runBlocking { dao.delete(gameId) }
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

    /**
     * Get a list of games, sorted by least recently updated, that
     * 1. have no associated collection record
     * 2. haven't been viewed in a configurable number of hours
     * 3. and have 0 plays (if plays are being synced
     */
    private fun fetchGameIds(): List<Int> {
        val hoursAgo = lastViewedAgeInHours.hoursAgo()

        val date = hoursAgo.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_TIME)
        Timber.i("Fetching games that aren't in the collection and have not been viewed since $date")

        var selection = "collection.${Collection.Columns.GAME_ID} IS NULL AND games.${Games.Columns.LAST_VIEWED}<?"
        if (prefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED)) {
            selection += " AND games.${Games.Columns.NUM_PLAYS}=0"
        }
        return context.contentResolver.queryInts(
                Games.CONTENT_URI,
                Games.Columns.GAME_ID,
                selection,
                arrayOf(hoursAgo.toString()),
                "games.${Games.Columns.UPDATED}")
    }
}
