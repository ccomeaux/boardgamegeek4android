package com.boardgamegeek.service

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import android.support.v4.util.ArrayMap
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.persister.CollectionPersister
import com.boardgamegeek.pref.SyncPrefs
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

            if (SyncPrefs.getCurrentCollectionSyncTimestamp(context) > 0) {
                Timber.i("Currently performing a full sync; skipping incremental sync")
                return
            }

            syncBySubtype()
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            if (wasSleepInterrupted(2000)) return

            syncBySubtype(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY)
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            SyncPrefs.setLastPartialCollectionTimestamp(context)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun syncBySubtype(subtype: String = "") {
        val lastStatusSync = SyncPrefs.getPartialCollectionSyncTimestamp(context, subtype)
        val lastPartialSync = SyncPrefs.getLastPartialCollectionTimestamp(context)
        if (lastStatusSync > lastPartialSync) {
            Timber.i("Subtype [$subtype] has been synced in the current sync request.")
            return
        }

        val modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(lastStatusSync))
        val subtypeDescription = context.getString(when (subtype) {
            BggService.THING_SUBTYPE_BOARDGAME -> R.string.games
            BggService.THING_SUBTYPE_BOARDGAME_EXPANSION -> R.string.expansions
            BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY -> R.string.accessories
            else -> R.string.items
        })

        val formattedDateTime = DateUtils.formatDateTime(context, lastStatusSync, DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

        updateProgressNotification(context.getString(R.string.sync_notification_collection_since_downloading, subtypeDescription, formattedDateTime))

        val options = ArrayMap<String, String>()
        options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
        options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
        options[BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE] = modifiedSince
        if (subtype.isNotEmpty()) options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = subtype

        persister.resetTimestamp()
        val call = service.collection(account.name, options)
        try {
            val response = call.execute()
            if (response.code() == 200) {
                val body = response.body()
                if (body != null && body.itemCount > 0) {
                    updateProgressNotification(context.getString(R.string.sync_notification_collection_since_saving, body.itemCount, subtypeDescription, formattedDateTime))
                    val count = persister.save(body.items).recordCount
                    syncResult.stats.numUpdates += body.itemCount.toLong()
                    Timber.i("...saved %,d records for %,d collection %s", count, body.itemCount, subtypeDescription)
                    SyncPrefs.setPartialCollectionSyncTimestamp(context, subtype, persister.initialTimestamp)
                } else {
                    Timber.i("...no new collection %s modifications", subtypeDescription)
                }
            } else {
                showError(context.getString(R.string.sync_notification_collection_since, subtypeDescription, formattedDateTime), response.code())
                syncResult.stats.numIoExceptions++
                cancel()
            }
        } catch (e: IOException) {
            showError(context.getString(R.string.sync_notification_collection_since, subtypeDescription, formattedDateTime), e)
            syncResult.stats.numIoExceptions++
            cancel()
        }
    }
}
