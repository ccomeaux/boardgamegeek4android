package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.pref.setBuddiesTimestamp
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Syncs the list of buddies. Only runs every few days.
 */
class SyncBuddiesList(application: BggApplication, service: BggService, syncResult: SyncResult, private val repository: UserRepository) :
    SyncTask(application, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId = R.string.sync_notification_buddies_list

    private val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_INTERVAL_DAYS)

    override fun execute() {
        Timber.i("Syncing list of buddies...")
        try {
            if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] != true) {
                Timber.i("...buddies not set to sync")
                return
            }

            val lastCompleteSync = syncPrefs.getBuddiesTimestamp()
            if (lastCompleteSync >= 0 && !lastCompleteSync.isOlderThan(fetchIntervalInDays, TimeUnit.DAYS)) {
                Timber.i("...skipping; we synced already within the last $fetchIntervalInDays days")
                return
            }

            runBlocking {
                val updateTimestamp = System.currentTimeMillis()

                val (upsertedCount, deletedCount) = repository.refreshBuddies(updateTimestamp)

                syncResult.stats.numEntries += upsertedCount
                Timber.i("Upserted %,d buddies", upsertedCount)

                syncResult.stats.numDeletes += deletedCount
                Timber.i("Pruned %,d users who are no longer buddies", deletedCount)

                syncPrefs.setBuddiesTimestamp(updateTimestamp)
            }
        } catch (e: Exception) {
            if (e is HttpException) {
                showError(context.getString(notificationSummaryMessageId), e.code())
            } else {
                showError(context.getString(notificationSummaryMessageId), e)
            }
            syncResult.stats.numIoExceptions++
            cancel()
        } finally {
            Timber.i("...complete!")
        }
    }
}
