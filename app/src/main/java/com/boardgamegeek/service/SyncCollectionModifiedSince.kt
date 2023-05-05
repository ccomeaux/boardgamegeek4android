package com.boardgamegeek.service

import android.content.SyncResult
import android.text.format.DateUtils
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getDescription
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.isCollectionSetToSync
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.getCurrentCollectionSyncTimestamp
import com.boardgamegeek.pref.getPartialCollectionSyncLastCompletedAt
import com.boardgamegeek.pref.setPartialCollectionSyncLastCompletedAt
import com.boardgamegeek.repository.CollectionItemRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Syncs the user's collection modified since the date stored in the sync service.
 */
class SyncCollectionModifiedSince(
    application: BggApplication,
    syncResult: SyncResult,
    private val collectionItemRepository: CollectionItemRepository,
) : SyncTask(application, syncResult) {
    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS)

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD
    override val notificationSummaryMessageId = R.string.sync_notification_collection_partial

    override fun execute() {
        Timber.i("Starting to sync recently modified collection items")
        try {
            if (isCancelled) {
                Timber.i("Sync task cancelled, aborting")
                return
            }

            if (!prefs.isCollectionSetToSync()) {
                Timber.i("Collection sync not set in preferences, aborting")
                return
            }

            if (syncPrefs.getCurrentCollectionSyncTimestamp() > 0) {
                Timber.i("Currently performing a full sync; skipping incremental sync")
                return
            }

            syncBySubtype()
            if (isCancelled) {
                Timber.i("Sync task cancelled, aborting")
                return
            }

            if (wasSleepInterrupted(fetchPauseMillis.milliseconds)) return

            syncBySubtype(BggService.ThingSubtype.BOARDGAME_ACCESSORY)
            if (isCancelled) {
                Timber.i("Sync task cancelled, aborting")
                return
            }

            syncPrefs.setPartialCollectionSyncLastCompletedAt()
            Timber.i("Syncing recently modified collection completed successfully")
        } catch (e: Exception) {
            Timber.i("Syncing recently modified collection ended with exception:\n$e")
        }
    }

    private fun syncBySubtype(subtype: BggService.ThingSubtype? = null) {
        Timber.i("Starting to sync subtype [${subtype?.code ?: "<none>"}]")
        val lastStatusSync = syncPrefs.getPartialCollectionSyncLastCompletedAt(subtype)
        val lastPartialSync = syncPrefs.getPartialCollectionSyncLastCompletedAt()
        if (lastStatusSync > lastPartialSync) {
            Timber.i("Subtype [${subtype?.code ?: "<none>"}] has been synced in the current sync request; aborting")
            return
        }

        val modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(lastStatusSync))
        val subtypeDescription = subtype.getDescription(context)

        val formattedDateTime = lastStatusSync.formatDateTime(context, flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

        updateProgressNotification(context.getString(R.string.sync_notification_collection_since_syncing, subtypeDescription, formattedDateTime))

        val options = mutableMapOf(
            BggService.COLLECTION_QUERY_KEY_STATS to "1",
            BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1",
            BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE to modifiedSince,
        )
        subtype?.let { options[BggService.COLLECTION_QUERY_KEY_SUBTYPE] = it.code }

        try {
            val timestamp = System.currentTimeMillis()
            val count = runBlocking { collectionItemRepository.refresh(options, timestamp) }
            syncResult.stats.numUpdates += count.toLong()
            Timber.i("Saved %,d recently modified collection %s", count, subtypeDescription)
            syncPrefs.setPartialCollectionSyncLastCompletedAt(subtype, timestamp)
        } catch (e: Exception) {
            if (e is HttpException) {
                showError(context.getString(R.string.sync_notification_collection_since, subtypeDescription, formattedDateTime), e.code())
            }else{
                showError(context.getString(R.string.sync_notification_collection_since, subtypeDescription, formattedDateTime), e)
            }
            syncResult.stats.numIoExceptions++
            cancel()
        }
    }
}
