package com.boardgamegeek.service

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.RemoteConfig
import com.boardgamegeek.util.StringUtils
import hugo.weaving.DebugLog
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.set

/**
 * Syncs the user's complete collection in brief mode, one collection status at a time, deleting all items from the local
 * database that weren't synced.
 */
class SyncCollectionComplete(context: Context, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(context, service, syncResult) {
    private val statusEntries = context.resources.getStringArray(R.array.pref_sync_status_entries)
    private val statusValues = context.resources.getStringArray(R.array.pref_sync_status_values)

    private val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_INTERVAL_DAYS)

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    private val syncableStatuses: List<String>
        get() {
            val statuses = ArrayList(PreferencesUtils.getSyncStatuses(context))
            // Played games should be synced first - they don't respect the "exclude" flag
            if (statuses.remove(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
                statuses.add(0, BggService.COLLECTION_QUERY_STATUS_PLAYED)
            }
            return statuses
        }

    override val notificationSummaryMessageId = R.string.sync_notification_collection_full

    @DebugLog
    override fun execute() {
        Timber.i("Syncing full collection list...")
        try {
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            if (!PreferencesUtils.isCollectionSetToSync(context)) {
                Timber.i("...collection not set to sync")
                return
            }

            val currentSyncTimestamp = SyncPrefs.getCurrentCollectionSyncTimestamp(context)
            if (currentSyncTimestamp == 0L) {
                val lastCompleteSync = SyncPrefs.getLastCompleteCollectionTimestamp(context)
                if (lastCompleteSync > 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < fetchIntervalInDays) {
                    Timber.i("Not currently syncing and it's been less than $fetchIntervalInDays days since we synced completely")
                    return
                }
                SyncPrefs.setCurrentCollectionSyncTimestamp(context)
            }

            val statuses = syncableStatuses
            for (i in statuses.indices) {
                if (i > 0) {
                    if (isCancelled) {
                        Timber.i("...cancelled")
                        return
                    }
                    if (wasSleepInterrupted(5, TimeUnit.SECONDS)) return
                }

                val excludedStatuses = (0 until i).map { statuses[it] }
                syncByStatus("", statuses[i], *excludedStatuses.toTypedArray())

                if (wasSleepInterrupted(5, TimeUnit.SECONDS)) return

                syncByStatus(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY, statuses[i], *excludedStatuses.toTypedArray())
            }

            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            deleteUnusedItems()
            updateTimestamps()
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

        val statusDescription = getStatusDescription(status)
        val subtypeDescription = context.getString(when (subtype) {
            BggService.THING_SUBTYPE_BOARDGAME -> R.string.games
            BggService.THING_SUBTYPE_BOARDGAME_EXPANSION -> R.string.expansions
            BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY -> R.string.accessories
            else -> R.string.items
        })

        val lastCompleteSync = SyncPrefs.getCurrentCollectionSyncTimestamp(context)
        val lastStatusSync = SyncPrefs.getCompleteCollectionSyncTimestamp(context, subtype, status)
        if (lastStatusSync > lastCompleteSync) {
            Timber.i("'$statusDescription' $subtypeDescription have been synced in the current sync request.")
            return
        }

        Timber.i("...syncing '$statusDescription' $subtypeDescription")
        Timber.i("...while excluding statuses [%s]", StringUtils.formatList(excludedStatuses))

        updateProgressNotification(context.getString(R.string.sync_notification_collection_downloading, statusDescription, subtypeDescription))

        val options = ArrayMap<String, String>()
        if (subtype.isNotEmpty()) options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = subtype
        options[BggService.COLLECTION_QUERY_KEY_STATS] = "1"
        options[BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE] = "1"
        options[status] = "1"
        for (excludedStatus in excludedStatuses) options[excludedStatus] = "0"

        val dao = CollectionDao(context)
        val call = service.collection(account.name, options)
        try {
            val timestamp = System.currentTimeMillis()
            val response = call.execute()
            if (response.code() == 200) {
                val items = response.body()?.items
                if (items != null && items.size > 0) {
                    updateProgressNotification(context.getString(R.string.sync_notification_collection_saving, items.size, statusDescription, subtypeDescription))
                    val mapper = CollectionItemMapper()
                    for (item in items) {
                        dao.saveItem(mapper.map(item), true, true, false, timestamp)
                    }
                    SyncPrefs.setCompleteCollectionSyncTimestamp(context, subtype, status, timestamp)
                    syncResult.stats.numUpdates += items.size.toLong()
                    Timber.i("...saved %,d collection $subtypeDescription", items.size)
                } else {
                    Timber.i("...no collection $subtypeDescription found for these games")
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
    private fun deleteUnusedItems() {
        Timber.i("...deleting old collection entries")
        val count = context.contentResolver.delete(
                Collection.CONTENT_URI,
                "${Collection.UPDATED_LIST}<?",
                arrayOf(SyncPrefs.getCurrentCollectionSyncTimestamp(context).toString()))
        Timber.i("...deleted $count old collection entries")
        // TODO: delete thumbnail images associated with this list (both collection and game)
    }

    @DebugLog
    private fun updateTimestamps() {
        SyncPrefs.setLastCompleteCollectionTimestamp(context, SyncPrefs.getCurrentCollectionSyncTimestamp(context))
        SyncPrefs.setLastPartialCollectionTimestamp(context, SyncPrefs.getCurrentCollectionSyncTimestamp(context))
        SyncPrefs.setCurrentCollectionSyncTimestamp(context, 0L)
    }
}
