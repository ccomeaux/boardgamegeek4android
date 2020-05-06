package com.boardgamegeek.service

import android.accounts.Account
import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.get
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.pref.getPlaysNewestTimestamp
import com.boardgamegeek.pref.getPlaysOldestTimestamp
import com.boardgamegeek.pref.setPlaysNewestTimestamp
import com.boardgamegeek.pref.setPlaysOldestTimestamp
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.util.RemoteConfig
import retrofit2.Response
import timber.log.Timber

class SyncPlays(application: BggApplication, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(application, service, syncResult) {
    private var startTime: Long = 0
    private val persister: PlayPersister = PlayPersister(context)
    private val playDao: PlayDao = PlayDao(application)

    override val syncType = SyncService.FLAG_SYNC_PLAYS_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_plays

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_PLAYS_FETCH_PAUSE_MILLIS)

    override fun execute() {
        Timber.i("Syncing plays...")
        try {
            if (prefs[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
                Timber.i("...plays not set to sync")
                return
            }

            startTime = System.currentTimeMillis()

            val newestSyncDate = syncPrefs.getPlaysNewestTimestamp()
            if (executeCall(account.name, newestSyncDate?.asDateForApi(), null)) {
                cancel()
                return
            }
            val deletedCount = playDao.deleteUnupdatedPlaysSince(startTime, newestSyncDate ?: 0L)
            syncResult.stats.numDeletes += deletedCount.toLong()
            Timber.i("...deleted $deletedCount unupdated plays")

            val oldestDate = syncPrefs.getPlaysOldestTimestamp()
            if (oldestDate > 0) {
                val date = oldestDate.asDateForApi()
                if (executeCall(account.name, null, date)) {
                    cancel()
                    return
                }
                val count = playDao.deleteUnupdatedPlaysBefore(startTime, newestSyncDate ?: 0L)
                syncResult.stats.numDeletes += count.toLong()
                Timber.i("...deleted $count unupdated plays")
                syncPrefs.setPlaysOldestTimestamp(0L)
            }
            CalculatePlayStatsTask(application).executeAsyncTask()
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

            if (page != 1) if (wasSleepInterrupted(fetchPauseMillis)) return true

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
            val mapper = PlayMapper()
            val plays = mapper.map(response?.plays)
            persist(plays)
            updateTimestamps(plays)
            page++
        } while (response != null && response.hasMorePages())
        return false
    }

    private fun formatNotificationMessage(minDate: String?, maxDate: String?, page: Int): String {
        val message = when {
            minDate.isNullOrBlank() && maxDate.isNullOrBlank() -> context.getString(R.string.sync_notification_plays_all)
            minDate.isNullOrBlank() -> context.getString(R.string.sync_notification_plays_old, maxDate)
            maxDate.isNullOrBlank() -> context.getString(R.string.sync_notification_plays_new, minDate)
            else -> context.getString(R.string.sync_notification_plays_between, minDate, maxDate)
        }
        return when {
            page > 1 -> context.getString(R.string.sync_notification_page_suffix, message, page)
            else -> message
        }
    }

    private fun persist(plays: List<Play>?) {
        if (plays != null && plays.isNotEmpty()) {
            persister.save(plays, startTime)
            syncResult.stats.numEntries += plays.size.toLong()
            Timber.i("...saved ${plays.size} plays")
        } else {
            Timber.i("...no plays to update")
        }
    }

    private fun updateTimestamps(plays: List<Play>?) {
        if (plays == null) return
        val newestDate = newestDate(plays)
        if (newestDate > syncPrefs.getPlaysNewestTimestamp() ?: 0L) {
            syncPrefs.setPlaysNewestTimestamp(newestDate)
        }
        val oldestDate = oldestDate(plays)
        if (oldestDate < syncPrefs.getPlaysOldestTimestamp()) {
            syncPrefs.setPlaysOldestTimestamp(oldestDate)
        }
    }

    private fun newestDate(plays: List<Play>) = plays.maxBy { it.dateInMillis }?.dateInMillis ?: 0L

    private fun oldestDate(plays: List<Play>) = plays.minBy { it.dateInMillis }?.dateInMillis ?: Long.MAX_VALUE
}
