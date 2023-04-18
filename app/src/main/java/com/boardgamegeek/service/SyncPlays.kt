package com.boardgamegeek.service

import android.content.SyncResult
import android.text.format.DateUtils
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class SyncPlays(
    application: BggApplication,
    syncResult: SyncResult,
    private val playRepository: PlayRepository,
) :
    SyncTask(application, syncResult) {

    private var startTime: Long = 0

    override val syncType = SyncService.FLAG_SYNC_PLAYS_DOWNLOAD

    override val notificationSummaryMessageId = R.string.sync_notification_plays

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_PLAYS_FETCH_PAUSE_MILLIS)

    override fun execute() {
        Timber.i("Starting syncing plays")
        try {
            if (prefs[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
                Timber.i("Plays not set to sync; aborting")
                return
            }

            startTime = System.currentTimeMillis()

            val newestSyncDate = syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L
            if (executeCall(newestSyncDate, 0L)) {
                cancel()
                return
            }
            if (newestSyncDate > 0L) {
                val deletedCount = runBlocking { playRepository.deleteUnupdatedPlaysSince(startTime, newestSyncDate) }
                syncResult.stats.numDeletes += deletedCount.toLong()
                Timber.i("Deleted $deletedCount unupdated plays since ${newestSyncDate.toDate()}")
            }

            val oldestDate = syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE
            if (oldestDate > 0) {
                if (executeCall(0L, oldestDate)) {
                    cancel()
                    return
                }
                if (oldestDate != Long.MAX_VALUE) {
                    val deletedCount = runBlocking { playRepository.deleteUnupdatedPlaysBefore(startTime, oldestDate) }
                    syncResult.stats.numDeletes += deletedCount.toLong()
                    Timber.i("Deleted $deletedCount unupdated plays before ${oldestDate.toDate()}")
                }
                syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = 0L
            }
            runBlocking { playRepository.calculatePlayStats() }
            Timber.i("Plays synced successfully ")
        } catch (e: Exception) {
            Timber.i("Plays sync ended with exception:\n$e")
        }
    }

    /**
     * Fetch the plays for the user in the specified date range. Plays are fetched 1 page of 50 at a time, most recent
     * first. Each page fetch shows a notification progress message. If successfully fetched, store the plays in the
     * database and update the sync timestamps. If there are more pages, pause then fetch another page .
     *
     * @return true if the sync operation should cancel
     */
    private fun executeCall(minDate: Long, maxDate: Long): Boolean {
        var page = 1
        do {
            if (isCancelled) {
                Timber.i("Play sync; aborting")
                return true
            }

            if (page != 1) if (wasSleepInterrupted(fetchPauseMillis.milliseconds)) return true

            val message = formatNotificationMessage(minDate, maxDate, page)
            updateProgressNotification(message)

            var shouldContinue: Boolean

            try {
                val (plays, canContinue) = runBlocking { playRepository.downloadPlays(minDate, maxDate, page) }
                if (plays.isNotEmpty()) {
                    runBlocking { playRepository.saveFromSync(plays, startTime) }
                    syncResult.stats.numEntries += plays.size.toLong()
                    Timber.i("Upserted ${plays.size} plays")
                } else {
                    Timber.i("No plays to update")
                }
                plays.maxOfOrNull { it.dateInMillis }?.let {
                    if (it > (syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L)) {
                        syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] = it
                    }
                }
                plays.minOfOrNull { it.dateInMillis }?.let {
                    if (it < (syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE)) {
                        syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] = it
                    }
                }
                shouldContinue = canContinue
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
        } while (shouldContinue)
        return false
    }

    private fun Long.toDate() = DateUtils.formatDateTime(context, this, DateUtils.FORMAT_SHOW_DATE)

    private fun formatNotificationMessage(minDate: Long, maxDate: Long, page: Int): String {
        val message = when {
            minDate == 0L && maxDate == 0L -> context.getString(R.string.sync_notification_plays_all)
            minDate == 0L -> context.getString(R.string.sync_notification_plays_old, maxDate.toDate())
            maxDate == 0L -> context.getString(R.string.sync_notification_plays_new, minDate.toDate())
            else -> context.getString(R.string.sync_notification_plays_between, minDate.toDate(), maxDate.toDate())
        }
        return when {
            page > 1 -> context.getString(R.string.sync_notification_page_suffix, message, page)
            else -> message
        }.also { Timber.i(it) }
    }
}
