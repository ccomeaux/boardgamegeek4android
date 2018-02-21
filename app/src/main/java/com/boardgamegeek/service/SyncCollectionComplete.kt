package com.boardgamegeek.service

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import android.support.annotation.IntegerRes
import android.support.annotation.StringRes
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.persister.CollectionPersister
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.StringUtils
import hugo.weaving.DebugLog
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Syncs the user's complete collection in brief mode, one collection status at a time, deleting all items from the local
 * database that weren't synced.
 */
class SyncCollectionComplete @DebugLog
constructor(context: Context, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(context, service, syncResult) {
    private val statusEntries = context.resources.getStringArray(R.array.pref_sync_status_entries)
    private val statusValues = context.resources.getStringArray(R.array.pref_sync_status_values)
    private val persister = CollectionPersister.Builder(context)
            .includePrivateInfo()
            .includeStats()
            .build()

    override val syncType: Int
        get() = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    private// Played games should be synced first - they don't respect the "exclude" flag
    val syncableStatuses: List<String>
        get() {
            val statuses = ArrayList(PreferencesUtils.getSyncStatuses(context))
            if (statuses.remove(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
                statuses.add(0, BggService.COLLECTION_QUERY_STATUS_PLAYED)
            }
            return statuses
        }

    override val notificationSummaryMessageId: Int
        get() = R.string.sync_notification_collection_full

    @DebugLog
    override fun execute() {
        Timber.i("Syncing full collection list...")
        try {
            persister.resetTimestamp()
            val statuses = syncableStatuses
            for (i in statuses.indices) {
                if (i > 0) {
                    if (isCancelled) {
                        Timber.i("...cancelled")
                        return
                    }
                    updateProgressNotification(context.getString(R.string.sync_notification_sleep))
                    if (wasSleepInterrupted(5000)) return
                }

                val excludedStatuses = (0 until i).map { statuses[it] }
                syncByStatus("", statuses[i], *excludedStatuses.toTypedArray())

                updateProgressNotification(context.getString(R.string.sync_notification_sleep))
                if (wasSleepInterrupted(5000)) return

                syncByStatus(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY, statuses[i], *excludedStatuses.toTypedArray())
            }

            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            deleteUnusedItems(persister.initialTimestamp)
            updateTimestamps(persister.initialTimestamp)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun syncByStatus(subtype: String = "", status: String, vararg excludedStatuses: String) {
        if (isCancelled) {
            Timber.i("...cancelled")
            return
        }

        if (TextUtils.isEmpty(status)) {
            Timber.i("...skipping blank status")
            return
        }
        Timber.i("...syncing subtype [$subtype] status [$status]")
        Timber.i("...while excluding statuses [%s]", StringUtils.formatList(excludedStatuses))

        val statusDescription = getStatusDescription(status)

        val options = ArrayMap<String, String>()
        options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = subtype
        options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
        options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
        options[status] = "1"
        for (excludedStatus in excludedStatuses) {
            options[excludedStatus] = "0"
        }

        @IntegerRes val typeResId = when (subtype) {
            BggService.THING_SUBTYPE_BOARDGAME -> R.string.games
            BggService.THING_SUBTYPE_BOARDGAME_EXPANSION -> R.string.expansions
            BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY -> R.string.accessories
            else -> R.string.items
        }
        fetchAndPersist(options, statusDescription, typeResId, excludedStatuses.isNotEmpty())
    }

    private fun fetchAndPersist(options: ArrayMap<String, String>, statusDescription: String, @StringRes typeResId: Int, hasExclusions: Boolean) {
        @StringRes val downloadingResId = when {
            hasExclusions -> R.string.sync_notification_collection_downloading_exclusions
            else -> R.string.sync_notification_collection_downloading
        }
        val type = context.getString(typeResId)
        updateProgressNotification(context.getString(downloadingResId, statusDescription, type))
        val call = service.collection(account.name, options)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.itemCount > 0) {
                    @StringRes val savingResId = when {
                        hasExclusions -> R.string.sync_notification_collection_saving_exclusions
                        else -> R.string.sync_notification_collection_saving
                    }
                    updateProgressNotification(context.getString(savingResId, body.itemCount, statusDescription, type))
                    val count = persister.save(body.items).recordCount
                    syncResult.stats.numUpdates += body.itemCount.toLong()
                    Timber.i("...saved %,d records for %,d collection %s", count, body.itemCount, type)
                } else {
                    Timber.i("...no collection %s found for these games", type)
                }
            } else {
                showError(context.getString(R.string.sync_notification_collection_detail, statusDescription, type), response.code())
                syncResult.stats.numIoExceptions++
                cancel()
            }
        } catch (e: IOException) {
            showError(context.getString(R.string.sync_notification_collection_detail, statusDescription, type), e)
            syncResult.stats.numIoExceptions++
            cancel()
        }

    }

    @DebugLog
    private fun getStatusDescription(status: String): String {
        for (i in statusEntries.indices) {
            if (statusValues[i].equals(status, ignoreCase = true)) {
                return statusEntries[i]
            }
        }
        return status
    }

    @DebugLog
    private fun deleteUnusedItems(initialTimestamp: Long) {
        Timber.i("...deleting old collection entries")
        val count = context.contentResolver.delete(
                Collection.CONTENT_URI,
                "${Collection.UPDATED_LIST}<?",
                arrayOf(initialTimestamp.toString()))
        Timber.i("...deleted $count old collection entries")
        // TODO: delete thumbnail images associated with this list (both collection and game)
    }

    @DebugLog
    private fun updateTimestamps(initialTimestamp: Long) {
        Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE, initialTimestamp)
        Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, initialTimestamp)
    }
}
