package com.boardgamegeek.service

import android.accounts.Account
import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE
import com.boardgamegeek.repository.PlayRepository

class SyncPlays(application: BggApplication, service: BggService, syncResult: SyncResult, private val account: Account) :
    SyncTask(application, service, syncResult) {
    private var startTime: Long = 0
    private val playRepository = PlayRepository(application)

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

            val newestSyncDate = syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L
            val minDate = if (newestSyncDate == 0L) null else newestSyncDate.asDateForApi()
            if (executeCall(account.name, minDate, null)) {
                cancel()
                return
            }
            if (minDate != null) {
                val deletedCount = runBlocking { playRepository.deleteUnupdatedPlaysSince(startTime, newestSyncDate) }
                syncResult.stats.numDeletes += deletedCount.toLong()
                Timber.i("...deleted $deletedCount unupdated plays")
            }

            val oldestDate = syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE
            if (oldestDate > 0) {
                val maxDate = oldestDate.asDateForApi()
                if (executeCall(account.name, null, maxDate)) {
                    cancel()
                    return
                }
                if (oldestDate != Long.MAX_VALUE) {
                    val count = runBlocking { playRepository.deleteUnupdatedPlaysBefore(startTime, oldestDate) }
                    syncResult.stats.numDeletes += count.toLong()
                    Timber.i("...deleted $count unupdated plays")
                }
                syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = 0L
            }
            runBlocking {
                playRepository.calculatePlayStats()
            }
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
        var page = 1
        do {
            if (isCancelled) {
                Timber.i("...cancelled early")
                return true
            }

            if (page != 1) if (wasSleepInterrupted(fetchPauseMillis)) return true

            val message = formatNotificationMessage(minDate, maxDate, page)
            updateProgressNotification(message)

            var response: PlaysResponse?

            try {
                response = runBlocking { service.plays(username, minDate, maxDate, page) }
                val plays = response.plays.mapToEntity(startTime)
                persist(plays)
                plays.maxOfOrNull { it.dateInMillis }?.let {
                    if (it > syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L) {
                        syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] = it
                    }
                }
                plays.minOfOrNull { it.dateInMillis }?.let {
                    if (it < syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE) {
                        syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = it
                    }
                }
            } catch (e: Exception) {
                if (e is HttpException) {
                    showError(message, e.code())
                } else {
                    showError(message, e)
                }
                syncResult.stats.numIoExceptions++
                return true
            }
            page++
        } while (response?.hasMorePages() == true)
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

    private fun persist(plays: List<PlayEntity>) {
        if (plays.isNotEmpty()) {
            runBlocking {
                playRepository.saveFromSync(plays, startTime)
            }
            syncResult.stats.numEntries += plays.size.toLong()
            Timber.i("...saved ${plays.size} plays")
        } else {
            Timber.i("...no plays to update")
        }
    }
}
