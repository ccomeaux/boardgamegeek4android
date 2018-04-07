package com.boardgamegeek.service

import android.content.Context
import android.content.SyncResult
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.queryInts
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.RemoteConfig
import timber.log.Timber

/**
 * Removes games that aren't in the collection and haven't been viewed in 72 hours.
 */
class SyncGamesRemove(context: Context, service: BggService, syncResult: SyncResult) : SyncTask(context, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_collection_missing

    private val lastViewedAgeInHours = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_DELETE_VIEW_HOURS)

    /**
     * Get a list of games, sorted by least recently updated, that
     * 1. have no associated collection record
     * 2. haven't been viewed in 72 hours
     * 3. and have 0 plays (if plays are being synced
     */
    private fun fetchGameIds(): List<Int> {
        val hoursAgo = DateTimeUtils.hoursAgo(lastViewedAgeInHours)

        val date = DateUtils.formatDateTime(context, hoursAgo, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_TIME)
        Timber.i("...not viewed since $date")

        var selection = "collection.${Collection.GAME_ID} IS NULL AND games.${Games.LAST_VIEWED}<?"
        if (PreferencesUtils.isStatusSetToSync(context, BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
            selection += " AND games.${Games.NUM_PLAYS}=0"
        }
        return context.contentResolver.queryInts(
                Games.CONTENT_URI,
                Games.GAME_ID,
                selection,
                arrayOf(hoursAgo.toString()),
                "games.${Games.UPDATED}")
    }

    override fun execute() {
        Timber.i("Removing games not in the collection...")
        try {
            val gameIds = fetchGameIds()
            if (gameIds.isNotEmpty()) {
                Timber.i("...found ${gameIds.size} games to delete")
                updateProgressNotification(context.resources.getQuantityString(R.plurals.sync_notification_games, gameIds.size, gameIds.size))

                var count = 0
                // NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
                for (gameId in gameIds) {
                    Timber.i("...deleting game ID=$gameId")
                    count += context.contentResolver.delete(Games.buildGameUri(gameId), null, null)
                }
                syncResult.stats.numDeletes += count.toLong()
                Timber.i("...deleted $count games")
            } else {
                Timber.i("...no games need deleting")
            }
        } finally {
            Timber.i("...complete!")
        }
    }
}
