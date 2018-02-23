package com.boardgamegeek.service

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.PlaysResponse
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.pref.SyncPrefUtils
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.SelectionBuilder
import com.boardgamegeek.util.TaskUtils
import retrofit2.Response
import timber.log.Timber

class SyncPlays(context: Context, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(context, service, syncResult) {
    private var startTime: Long = 0
    private val persister: PlayPersister = PlayPersister(context)

    override val syncType: Int
        get() = SyncService.FLAG_SYNC_PLAYS_DOWNLOAD

    override val notificationSummaryMessageId: Int
        get() = R.string.sync_notification_plays

    override fun execute() {
        Timber.i("Syncing plays...")
        try {
            if (!PreferencesUtils.getSyncPlays(context)) {
                Timber.i("...plays not set to sync")
                return
            }

            startTime = System.currentTimeMillis()

            val newestSyncDate = SyncPrefUtils.getPlaysNewestTimestamp(context)
            if (newestSyncDate <= 0) {
                if (executeCall(account.name, null, null)) {
                    cancel()
                    return
                }
            } else {
                val date = DateTimeUtils.formatDateForApi(newestSyncDate)
                if (executeCall(account.name, date, null)) {
                    cancel()
                    return
                }
                deleteUnupdatedPlaysSince(newestSyncDate)
            }

            val oldestDate = SyncPrefUtils.getPlaysOldestTimestamp(context)
            if (oldestDate > 0) {
                val date = DateTimeUtils.formatDateForApi(oldestDate)
                if (executeCall(account.name, null, date)) {
                    cancel()
                    return
                }
                deleteUnupdatedPlaysBefore(oldestDate)
                SyncPrefUtils.setPlaysOldestTimestamp(context, 0L)
            }
            TaskUtils.executeAsyncTask(CalculatePlayStatsTask(context))
        } finally {
            Timber.i("...complete!")
        }
    }

    /**
     * Fetch the plays for the user in the specified date range. Plays are fetched 1 page of 50 at a time, most recent
     * first. Each page fetch shows a notification progress message. If successfully fetched, store the plays in the
     * database and update the sync timestamps. If there are more pages, pause then fetch another page .
     *
     * @return true if the sync operation should cancel
     */
    private fun executeCall(username: String, minDate: String?, maxDate: String?): Boolean {
        var response: PlaysResponse?
        var page = 1
        do {
            if (isCancelled) {
                Timber.i("...cancelled early")
                return true
            }

            if (page != 1) if (wasSleepInterrupted(3000)) return true

            val message = formatNotificationMessage(minDate, maxDate, page)
            updateProgressNotification(message)
            val call = service.plays(username, minDate, maxDate, page)
            val r: Response<PlaysResponse>
            try {
                r = call.execute()
                if (!r.isSuccessful) {
                    showError(message, r.code())
                    syncResult.stats.numIoExceptions++
                    return true
                }
            } catch (e: Exception) {
                showError(message, e)
                syncResult.stats.numIoExceptions++
                return true
            }

            response = r.body()
            persist(response)
            updateTimestamps(response)
            page++
        } while (response != null && response.hasMorePages())
        return false
    }

    private fun formatNotificationMessage(minDate: String?, maxDate: String?, page: Int): String {
        val message = if (TextUtils.isEmpty(minDate) && TextUtils.isEmpty(maxDate)) {
            context.getString(R.string.sync_notification_plays_all)
        } else if (TextUtils.isEmpty(minDate)) {
            context.getString(R.string.sync_notification_plays_old, maxDate)
        } else if (TextUtils.isEmpty(maxDate)) {
            context.getString(R.string.sync_notification_plays_new, minDate)
        } else {
            context.getString(R.string.sync_notification_plays_between, minDate, maxDate)
        }
        return when {
            page > 1 -> context.getString(R.string.sync_notification_page_suffix, message, page)
            else -> message
        }
    }

    private fun persist(response: PlaysResponse?) {
        if (response?.plays != null && response.plays.isNotEmpty()) {
            persister.save(response.plays, startTime)
            syncResult.stats.numEntries += response.plays.size.toLong()
            Timber.i("...saved ${response.plays.size} plays")
        } else {
            Timber.i("...no plays to update")
        }
    }

    private fun deleteUnupdatedPlaysSince(time: Long) {
        deleteUnupdatedPlays(time, ">=")
    }

    private fun deleteUnupdatedPlaysBefore(time: Long) {
        deleteUnupdatedPlays(time, "<=")
    }

    private fun deleteUnupdatedPlays(time: Long, dateComparator: String) {
        deletePlays(Plays.SYNC_TIMESTAMP + "<? AND " + Plays.DATE + dateComparator + "? AND " +
                SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
                SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
                SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
                arrayOf(startTime.toString(), DateTimeUtils.formatDateForApi(time)))
    }

    private fun deletePlays(selection: String, selectionArgs: Array<String>) {
        val count = context.contentResolver.delete(Plays.CONTENT_URI, selection, selectionArgs)
        syncResult.stats.numDeletes += count.toLong()
        Timber.i("...deleted $count unupdated plays")
    }

    private fun updateTimestamps(response: PlaysResponse?) {
        if (response == null) return
        if (response.newestDate > SyncPrefUtils.getPlaysNewestTimestamp(context)) {
            SyncPrefUtils.setPlaysNewestTimestamp(context, response.newestDate)
        }
        if (response.oldestDate < SyncPrefUtils.getPlaysOldestTimestamp(context)) {
            SyncPrefUtils.setPlaysOldestTimestamp(context, response.oldestDate)
        }
    }
}
