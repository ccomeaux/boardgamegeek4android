package com.boardgamegeek.service

import android.accounts.Account
import android.content.SyncResult
import android.text.format.DateUtils
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.getSyncStatusesOrDefault
import com.boardgamegeek.extensions.isCollectionSetToSync
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntities
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Syncs the user's complete collection in brief mode, one collection status at a time, deleting all items from the local
 * database that weren't synced.
 */
class SyncCollectionComplete(application: BggApplication, service: BggService, syncResult: SyncResult, private val account: Account) :
    SyncTask(application, service, syncResult) {
    private val statusEntries = context.resources.getStringArray(R.array.pref_sync_status_entries)
    private val statusValues = context.resources.getStringArray(R.array.pref_sync_status_values)

    private val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_INTERVAL_DAYS)

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    private val syncableStatuses: List<String>
        get() {
            val statuses = prefs.getSyncStatusesOrDefault().toMutableList()
            // Played games should be synced first - they don't respect the "exclude" flag
            if (statuses.remove(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
                statuses.add(0, BggService.COLLECTION_QUERY_STATUS_PLAYED)
            }
            return statuses
        }

    override val notificationSummaryMessageId = R.string.sync_notification_collection_full

    override fun execute() {
        Timber.i("Syncing complete collection")
        try {
            if (isCancelled) {
                Timber.i("Complete collection sync task cancelled")
                return
            }

            if (!prefs.isCollectionSetToSync()) {
                Timber.i("Collection sync not set in preferences")
                return
            }

            if (syncPrefs.getCurrentCollectionSyncTimestamp() == 0L) {
                val lastCompleteSync = syncPrefs.getLastCompleteCollectionTimestamp()
                if (lastCompleteSync > 0 && !lastCompleteSync.isOlderThan(fetchIntervalInDays, TimeUnit.DAYS)) {
                    Timber.i("Not currently syncing and it's been less than $fetchIntervalInDays days since we synced completely")
                    return
                }
                syncPrefs.setCurrentCollectionSyncTimestamp()
            }

            val statuses = syncableStatuses
            for (i in statuses.indices) {
                val status = statuses[i]
                if (i > 0) {
                    if (isCancelled) {
                        Timber.i("Complete collection sync task cancelled before syncing $status")
                        return
                    }
                    if (wasSleepInterrupted(5, TimeUnit.SECONDS)) return
                }

                val excludedStatuses = (0 until i).map { statuses[it] }
                syncByStatus(null, status, *excludedStatuses.toTypedArray())

                if (wasSleepInterrupted(5, TimeUnit.SECONDS)) return

                syncByStatus(BggService.ThingSubtype.BOARDGAME_ACCESSORY, status, *excludedStatuses.toTypedArray())
            }

            if (isCancelled) {
                Timber.i("Complete collection sync task cancelled before item deletion")
                return
            }

            deleteUnusedItems()
            updateTimestamps()
        } finally {
            Timber.i("Complete collection sync task completed")
        }
    }

    private fun syncByStatus(subtype: BggService.ThingSubtype? = null, status: String, vararg excludedStatuses: String) {
        val statusDescription = getStatusDescription(status)
        val subtypeDescription = getSubtypeDescription(subtype)

        if (isCancelled) {
            Timber.i("Complete collection sync task cancelled before status $statusDescription, subtype $subtypeDescription")
            return
        }

        if (syncPrefs.getCompleteCollectionSyncTimestamp(subtype, status) > syncPrefs.getCurrentCollectionSyncTimestamp()) {
            Timber.i("Skipping $statusDescription collection $subtypeDescription that have already been synced in the current sync request.")
            return
        }

        Timber.i("Syncing $statusDescription collection $subtypeDescription while excluding statuses [${excludedStatuses.formatList()}]")

        updateProgressNotification(context.getString(R.string.sync_notification_collection_downloading, statusDescription, subtypeDescription))

        val options = mutableMapOf(
            BggService.COLLECTION_QUERY_KEY_STATS to "1",
            BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
            status to "1",
        )
        subtype?.let { options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = it.code }
        for (excludedStatus in excludedStatuses) options[excludedStatus] = "0"

        val dao = CollectionDao(application)
        val call = service.collection(account.name, options)
        try {
            val timestamp = System.currentTimeMillis()
            val response = call.execute()
            if (response.code() == 200) {
                val items = response.body()?.items
                if (items != null && items.size > 0) {
                    updateProgressNotification(context.getString(R.string.sync_notification_collection_saving, items.size, statusDescription, subtypeDescription))
                    for (item in items) {
                        val (collectionItem, game) = item.mapToEntities()
                        runBlocking {
                            dao.saveItem(collectionItem, game, timestamp)
                        }
                    }
                    syncPrefs.setCompleteCollectionSyncTimestamp(subtype, status, timestamp)
                    syncResult.stats.numUpdates += items.size.toLong()
                    Timber.i("Saved ${items.size} $statusDescription collection $subtypeDescription")
                } else {
                    Timber.i("No $statusDescription collection $subtypeDescription found")
                }
            } else {
                showError(context.getString(R.string.sync_notification_collection_detail, statusDescription, subtypeDescription), response.code())
                syncResult.stats.numIoExceptions++
                cancel()
            }
        } catch (e: IOException) {
            showError(context.getString(R.string.sync_notification_collection_detail, statusDescription, subtypeDescription), e)
            syncResult.stats.numIoExceptions++
            cancel()
        }
    }

    private fun getStatusDescription(status: String): String {
        for (i in statusEntries.indices) {
            if (statusValues[i].equals(status, ignoreCase = true)) {
                return statusEntries[i]
            }
        }
        return status
    }

    private fun getSubtypeDescription(subtype: BggService.ThingSubtype?): String {
        return context.getString(
            when (subtype) {
                BggService.ThingSubtype.BOARDGAME -> R.string.games
                BggService.ThingSubtype.BOARDGAME_EXPANSION -> R.string.expansions
                BggService.ThingSubtype.BOARDGAME_ACCESSORY -> R.string.accessories
                else -> R.string.items
            }
        )
    }

    private fun deleteUnusedItems() {
        val timestamp = syncPrefs.getCurrentCollectionSyncTimestamp()
        val formattedDateTime = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        Timber.i("Deleting collection items not updated since $formattedDateTime")
        val count = context.contentResolver.delete(
            Collection.CONTENT_URI,
            "${Collection.Columns.UPDATED_LIST}<?",
            arrayOf(timestamp.toString())
        )
        Timber.i("Deleted $count old collection items")
        // TODO: delete thumbnail images associated with this list (both collection and game)
    }

    private fun updateTimestamps() {
        syncPrefs.setLastCompleteCollectionTimestamp(syncPrefs.getCurrentCollectionSyncTimestamp())
        syncPrefs.setPartialCollectionSyncLastCompletedAt(syncPrefs.getCurrentCollectionSyncTimestamp())
        syncPrefs.setCurrentCollectionSyncTimestamp(0L)
    }
}
