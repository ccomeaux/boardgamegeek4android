package com.boardgamegeek.service

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import android.support.annotation.StringRes
import android.support.v4.util.ArrayMap
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.persister.CollectionPersister
import com.boardgamegeek.pref.SyncPrefUtils
import com.boardgamegeek.util.PreferencesUtils
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Syncs the user's collection modified since the date stored in the sync service.
 */
class SyncCollectionModifiedSince(context: Context, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(context, service, syncResult) {
    private val persister = CollectionPersister.Builder(context)
            .includeStats()
            .includePrivateInfo()
            .validStatusesOnly()
            .build()

    override val syncType: Int
        get() = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val notificationSummaryMessageId: Int
        get() = R.string.sync_notification_collection_partial

    override fun execute() {
        try {
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            if (!PreferencesUtils.isCollectionSetToSync(context)) {
                Timber.i("...collection not set to sync")
                return
            }

            if (SyncPrefUtils.getCurrentCollectionSyncTimestamp(context) > 0) {
                Timber.i("Currently performing a full sync; skipping incremental sync")
                return
            }

            persister.resetTimestamp()
            val date = SyncPrefUtils.getLastPartialCollectionTimestamp(context)
            val modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(date))
            val formattedDateTime = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

            val options = ArrayMap<String, String>()
            options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
            options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
            options[BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE] = modifiedSince
            fetchAndPersist(context.getString(R.string.sync_notification_collection_items_since, formattedDateTime), options, R.string.items)

            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }
            if (wasSleepInterrupted(2000)) return

            options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY
            fetchAndPersist(context.getString(R.string.sync_notification_collection_accessories_since, formattedDateTime), options, R.string.accessories)

            SyncPrefUtils.setLastPartialCollectionTimestamp(context, persister.initialTimestamp)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun fetchAndPersist(detail: String, options: ArrayMap<String, String>, @StringRes typeResId: Int) {
        updateProgressNotification(detail)
        val call = service.collection(account.name, options)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.itemCount > 0) {
                    val count = persister.save(body.items).recordCount
                    syncResult.stats.numUpdates += body.itemCount.toLong()
                    Timber.i("...saved %,d records for %,d collection %s", count, body.itemCount, context.getString(typeResId))
                } else {
                    Timber.i("...no new collection %s modifications", context.getString(typeResId))
                }
            } else {
                showError(detail, response.code())
                syncResult.stats.numIoExceptions++
                cancel()
            }
        } catch (e: IOException) {
            showError(detail, e)
            syncResult.stats.numIoExceptions++
            cancel()
        }

    }
}
