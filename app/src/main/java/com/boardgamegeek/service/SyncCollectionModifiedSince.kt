package com.boardgamegeek.service

import android.accounts.Account
import android.content.SyncResult
import android.text.format.DateUtils
import androidx.collection.ArrayMap
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.getSyncStatusesOrDefault
import com.boardgamegeek.extensions.isCollectionSetToSync
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.pref.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.RemoteConfig
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Syncs the user's collection modified since the date stored in the sync service.
 */
class SyncCollectionModifiedSince(application: BggApplication, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(application, service, syncResult) {
    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS)
    private val statusesToSync = prefs.getSyncStatusesOrDefault()

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD
    override val notificationSummaryMessageId = R.string.sync_notification_collection_partial

    override fun execute() {
        try {
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            if (!prefs.isCollectionSetToSync()) {
                Timber.i("...collection not set to sync")
                return
            }

            if (syncPrefs.getCurrentCollectionSyncTimestamp() > 0) {
                Timber.i("Currently performing a full sync; skipping incremental sync")
                return
            }

            syncBySubtype()
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            if (wasSleepInterrupted(fetchPauseMillis)) return

            syncBySubtype(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY)
            if (isCancelled) {
                Timber.i("...cancelled")
                return
            }

            syncPrefs.setLastPartialCollectionTimestamp()
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun syncBySubtype(subtype: String = "") {
        val lastStatusSync = syncPrefs.getPartialCollectionSyncTimestamp(subtype)
        val lastPartialSync = syncPrefs.getLastPartialCollectionTimestamp()
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

        val dao = CollectionDao(application)
        val call = service.collection(account.name, options)
        try {
            val timestamp = System.currentTimeMillis()
            val response = call.execute()
            if (response.code() == 200) {
                val items = response.body()?.items
                if (items != null && items.size > 0) {
                    updateProgressNotification(context.getString(R.string.sync_notification_collection_since_saving, items.size, subtypeDescription, formattedDateTime))
                    val mapper = CollectionItemMapper()
                    var count = 0
                    for (item in items) {
                        val pair = mapper.map(item)
                        if (isItemStatusSetToSync(pair.first)) {
                            val collectionId = dao.saveItem(pair.first, pair.second, timestamp)
                            if (collectionId != BggContract.INVALID_ID) count++
                        } else {
                            Timber.i("Skipped collection item '${pair.first.gameName}' [ID=${pair.first.gameId}, collection ID=${pair.first.collectionId}] - collection status not synced")
                        }
                    }
                    syncResult.stats.numUpdates += count.toLong()
                    Timber.i("...saved %,d collection %s", count, subtypeDescription)
                } else {
                    Timber.i("...no new collection %s modifications", subtypeDescription)
                }
                syncPrefs.setPartialCollectionSyncTimestamp(subtype, timestamp)
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

    private fun isItemStatusSetToSync(item: CollectionItemEntity): Boolean {
        if (item.own && "own" in statusesToSync) return true
        if (item.previouslyOwned && "prevowned" in statusesToSync) return true
        if (item.forTrade && "fortrade" in statusesToSync) return true
        if (item.wantInTrade && "want" in statusesToSync) return true
        if (item.wantToPlay && "wanttoplay" in statusesToSync) return true
        if (item.wantToBuy && "wanttobuy" in statusesToSync) return true
        if (item.wishList && "wishlist" in statusesToSync) return true
        if (item.preOrdered && "preordered" in statusesToSync) return true
        return item.numberOfPlays > 0 && "played" in statusesToSync
    }
}
